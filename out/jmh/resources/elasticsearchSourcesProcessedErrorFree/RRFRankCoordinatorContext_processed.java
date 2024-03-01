/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.rank.rrf;

import org.apache.lucene.util.PriorityQueue;
import org.elasticsearch.action.search.SearchPhaseController.SortedTopDocs;
import org.elasticsearch.action.search.SearchPhaseController.TopDocsStats;
import org.elasticsearch.common.util.Maps;
import org.elasticsearch.search.query.QuerySearchResult;
import org.elasticsearch.search.rank.RankCoordinatorContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.xpack.rank.rrf.RRFRankDoc.NO_RANK;

/**
 * Ranks and decorates search hits for RRF results on the coordinator.
 */
public class RRFRankCoordinatorContext extends RankCoordinatorContext {

    private final int rankConstant;

    public RRFRankCoordinatorContext(int size, int from, int windowSize, int rankConstant) {
        super(size, from, windowSize);
        this.rankConstant = rankConstant;
    }

    protected record RankKey(int doc, int shardIndex) {

    }

    @Override
    public SortedTopDocs rank(List<QuerySearchResult> querySearchResults, TopDocsStats topDocsStats) {
        int queryCount = -1;
        List<PriorityQueue<RRFRankDoc>> queues = new ArrayList<>();
        for (QuerySearchResult querySearchResult : querySearchResults) {
            if (querySearchResult.searchTimedOut()) {
                topDocsStats.timedOut = true;
                continue;
            }
            if (querySearchResult.terminatedEarly() != null && querySearchResult.terminatedEarly()) {
                topDocsStats.terminatedEarly = true;
            }
            assert querySearchResult.getRankShardResult() instanceof RRFRankShardResult;
            RRFRankShardResult rrfRankShardResult = (RRFRankShardResult) querySearchResult.getRankShardResult();

            if (queryCount == -1) {
                queryCount = rrfRankShardResult.queryCount;

                for (int qi = 0; qi < queryCount; ++qi) {
                    final int fqi = qi;
                    queues.add(new PriorityQueue<>(windowSize + from) {
                        @Override
                        protected boolean lessThan(RRFRankDoc a, RRFRankDoc b) {
                            float score1 = a.scores[fqi];
                            float score2 = b.scores[fqi];
                            if (score1 != score2) {
                                return score1 < score2;
                            }
                            if (a.shardIndex != b.shardIndex) {
                                return a.shardIndex > b.shardIndex;
                            }
                            return a.doc > b.doc;
                        }
                    });
                }
            }
            assert queryCount == rrfRankShardResult.queryCount;

            for (RRFRankDoc rrfRankDoc : rrfRankShardResult.rrfRankDocs) {
                assert rrfRankDoc.shardIndex == -1;
                rrfRankDoc.shardIndex = querySearchResult.getShardIndex();
                for (int qi = 0; qi < queryCount; ++qi) {
                    if (rrfRankDoc.positions[qi] != NO_RANK) {
                        queues.get(qi).insertWithOverflow(rrfRankDoc);
                    }
                }
            }
        }

        if (queues.isEmpty()) {
            return SortedTopDocs.EMPTY;
        }

        Map<RankKey, RRFRankDoc> results = Maps.newMapWithExpectedSize(queryCount * windowSize);
        final int fqc = queryCount;
        for (int qi = 0; qi < queryCount; ++qi) {
            PriorityQueue<RRFRankDoc> queue = queues.get(qi);
            final int fqi = qi;
            for (int rank = queue.size(); rank > 0; --rank) {
                RRFRankDoc rrfRankDoc = queue.pop();
                final int frank = rank;
                results.compute(new RankKey(rrfRankDoc.doc, rrfRankDoc.shardIndex), (key, value) -> {
                    if (value == null) {
                        value = new RRFRankDoc(rrfRankDoc.doc, rrfRankDoc.shardIndex, fqc);
                    }

                    value.score += 1.0f / (rankConstant + frank);
                    value.positions[fqi] = frank - 1;
                    value.scores[fqi] = rrfRankDoc.scores[fqi];

                    return value;
                });
            }
        }

        RRFRankDoc[] sortedResults = results.values().toArray(RRFRankDoc[]::new);
        Arrays.sort(sortedResults, (RRFRankDoc rrf1, RRFRankDoc rrf2) -> {
            if (rrf1.score != rrf2.score) {
                return rrf1.score < rrf2.score ? 1 : -1;
            }
            assert rrf1.positions.length == rrf2.positions.length;
            for (int qi = 0; qi < rrf1.positions.length; ++qi) {
                if (rrf1.positions[qi] != NO_RANK && rrf2.positions[qi] != NO_RANK) {
                    if (rrf1.scores[qi] != rrf2.scores[qi]) {
                        return rrf1.scores[qi] < rrf2.scores[qi] ? 1 : -1;
                    }
                } else if (rrf1.positions[qi] != NO_RANK) {
                    return -1;
                } else if (rrf2.positions[qi] != NO_RANK) {
                    return 1;
                }
            }
            if (rrf1.shardIndex != rrf2.shardIndex) {
                return rrf1.shardIndex < rrf2.shardIndex ? -1 : 1;
            }
            return rrf1.doc < rrf2.doc ? -1 : 1;
        });
        RRFRankDoc[] topResults = new RRFRankDoc[Math.min(size, sortedResults.length - from)];
        for (int rank = 0; rank < topResults.length; ++rank) {
            topResults[rank] = sortedResults[rank];
            topResults[rank].rank = rank + 1 + from;
        }
        assert topDocsStats.fetchHits == 0;
        topDocsStats.fetchHits = topResults.length;

        return new SortedTopDocs(topResults, false, null, null, null, 0);
    }
}
