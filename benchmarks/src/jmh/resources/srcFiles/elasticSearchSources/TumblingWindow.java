/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.eql.execution.sequence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xpack.eql.execution.assembler.BoxedQueryRequest;
import org.elasticsearch.xpack.eql.execution.assembler.Executable;
import org.elasticsearch.xpack.eql.execution.assembler.SequenceCriterion;
import org.elasticsearch.xpack.eql.execution.search.HitReference;
import org.elasticsearch.xpack.eql.execution.search.Ordinal;
import org.elasticsearch.xpack.eql.execution.search.QueryClient;
import org.elasticsearch.xpack.eql.execution.search.RuntimeUtils;
import org.elasticsearch.xpack.eql.execution.search.Timestamp;
import org.elasticsearch.xpack.eql.session.EmptyPayload;
import org.elasticsearch.xpack.eql.session.Payload;
import org.elasticsearch.xpack.eql.session.Payload.Type;
import org.elasticsearch.xpack.eql.util.ReversedIterator;
import org.elasticsearch.xpack.ql.expression.Attribute;
import org.elasticsearch.xpack.ql.util.ActionListeners;
import org.elasticsearch.xpack.ql.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.elasticsearch.action.ActionListener.runAfter;
import static org.elasticsearch.xpack.eql.execution.ExecutionUtils.copySource;
import static org.elasticsearch.xpack.eql.execution.search.RuntimeUtils.combineFilters;
import static org.elasticsearch.xpack.eql.util.SearchHitUtils.qualifiedIndex;

/**
 * Time-based window encapsulating query creation and advancement.
 * Since queries can return different number of results, to avoid creating incorrect sequences,
 * all searches are 'boxed' to a base query.
 *
 * The window always moves ASC (sorted on timestamp/tiebreaker ordinal) since events in a sequence occur
 * one after the other. The window starts at the base (the first query) - when no results are found,
 * the next query gets promoted. This allows the window to find any follow-up results even if they are
 * found outside the initial window of a base query.
 *
 * TAIL/DESC sequences are handled somewhat differently. The first/base query moves DESC and the tumbling
 * window keeps moving ASC but using the second query as its base. When the tumbling window finishes instead
 * of bailing out, the DESC query keeps advancing.
 */
public class TumblingWindow implements Executable {

    private static final int CACHE_MAX_SIZE = 64;

    /**
     * Missing events are checked using multi-queries.
     * This is the max number of sequences that are checked with a single multi-query.
     * If more sequences have to be checked, then multiple multi-queries are executed.
     */
    private static final int MISSING_EVENTS_SEQUENCES_CHECK_BATCH_SIZE = 1000;

    private static final Logger log = LogManager.getLogger(TumblingWindow.class);

    /**
     * Simple cache for removing duplicate strings (such as index name or common keys).
     * Designed to be low-effort, non-concurrent (not needed) and thus optimistic in nature.
     * Thus it has a small, upper limit so that it doesn't require any cleaning up.
     */
    private final Map<String, String> stringCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return this.size() >= CACHE_MAX_SIZE;
        }
    };

    private final QueryClient client;
    private final List<SequenceCriterion> criteria;
    private final SequenceCriterion until;
    private final SequenceMatcher matcher;
    private final int maxStages;
    private final int windowSize;

    private final boolean hasKeys;
    private final List<List<Attribute>> listOfKeys;

    private boolean restartWindowFromTailQuery;

    private long startTime;

    private static class WindowInfo {
        private final int baseStage;
        private final Ordinal begin;
        private final Ordinal end;

        WindowInfo(int baseStage, Ordinal begin, Ordinal end) {
            this.baseStage = baseStage;
            this.begin = begin;
            this.end = end;
        }
    }

    public TumblingWindow(
        QueryClient client,
        List<SequenceCriterion> criteria,
        SequenceCriterion until,
        SequenceMatcher matcher,
        List<List<Attribute>> listOfKeys
    ) {
        this.client = client;

        this.until = until;
        this.criteria = criteria;
        this.maxStages = criteria.size();
        this.matcher = matcher;

        SequenceCriterion baseRequest = criteria.get(matcher.firstPositiveStage);
        this.windowSize = baseRequest.queryRequest().searchSource().size();
        this.hasKeys = baseRequest.keySize() > 0;
        this.restartWindowFromTailQuery = baseRequest.descending();
        this.listOfKeys = listOfKeys;
    }

    @Override
    public void execute(ActionListener<Payload> listener) {
        log.trace("Starting sequence window w/ fetch size [{}]", windowSize);
        startTime = System.currentTimeMillis();
        tumbleWindow(matcher.firstPositiveStage, runAfter(listener, () -> {
            matcher.clear();
            client.close(listener.delegateFailure((l, r) -> {}));
        }));
    }

    /**
     * Move the window while preserving the same base.
     */
    private void tumbleWindow(int currentStage, ActionListener<Payload> listener) {
        if (currentStage > matcher.firstPositiveStage && matcher.hasCandidates() == false) {
            if (restartWindowFromTailQuery) {
                currentStage = matcher.firstPositiveStage;
            } else {
                checkMissingEvents(() -> doPayload(listener), listener);
                return;
            }
        }

        log.trace("Tumbling window...");
        if (restartWindowFromTailQuery) {
            if (currentStage == matcher.firstPositiveStage) {
                matcher.trim(null);
            }
        } else {
            Ordinal marker = criteria.get(currentStage).queryRequest().after();
            if (marker != null) {
                matcher.trim(marker);
            }
        }

        int c = currentStage;
        checkMissingEvents(() -> advance(c, listener), listener);
    }

    /**
     * Move the window while advancing the query base.
     */
    private void rebaseWindow(int nextStage, ActionListener<Payload> listener) {
        log.trace("Rebasing window...");
        checkMissingEvents(() -> advance(nextStage, listener), listener);
    }

    public void checkMissingEvents(Runnable next, ActionListener<Payload> listener) {
        Set<Sequence> sequencesToCheck = matcher.toCheckForMissing();
        if (sequencesToCheck.isEmpty()) {
            if (matcher.limitReached()) {
                doPayload(listener);
                return;
            }
            next.run();
        } else {
            Iterator<Sequence> iterator = sequencesToCheck.iterator();
            List<Sequence> batchToCheck = new ArrayList<>();

            for (int i = 0; i < MISSING_EVENTS_SEQUENCES_CHECK_BATCH_SIZE && iterator.hasNext(); i++) {
                batchToCheck.add(iterator.next());
                iterator.remove();
            }

            List<SearchRequest> queries = prepareQueryForMissingEvents(batchToCheck);
            client.multiQuery(queries, listener.delegateFailureAndWrap((l, p) -> doCheckMissingEvents(batchToCheck, p, l, next)));
        }
    }

    private void doCheckMissingEvents(List<Sequence> batchToCheck, MultiSearchResponse p, ActionListener<Payload> listener, Runnable next) {
        MultiSearchResponse.Item[] responses = p.getResponses();
        int nextResponse = 0;
        for (Sequence sequence : batchToCheck) {
            boolean leading = true;
            boolean discarded = false;
            Timestamp lastLeading = null;
            Timestamp firstTrailing = null;
            for (int i = 0; i < criteria.size(); i++) {
                SequenceCriterion criterion = criteria.get(i);
                if (criterion.missing()) {
                    SearchResponse response = responses[nextResponse++].getResponse();
                    if (discarded) {
                        continue; 
                    }
                    SearchHit[] hits = response.getHits().getHits();
                    if (leading) {
                        if (hits.length == 0) {
                            continue;
                        }
                        Timestamp hitTimestamp = criterion.timestamp(hits[0]);
                        lastLeading = lastLeading == null || lastLeading.instant().compareTo(hitTimestamp.instant()) < 0
                            ? hitTimestamp
                            : lastLeading;
                    } else if (trailing(i)) {
                        if (hits.length == 0) {
                            continue;
                        }
                        Timestamp hitTimestamp = criterion.timestamp(hits[0]);
                        firstTrailing = firstTrailing == null || firstTrailing.instant().compareTo(hitTimestamp.instant()) > 0
                            ? hitTimestamp
                            : firstTrailing;
                    } else {
                        if (hits.length > 0) {
                            discarded = true;
                        }
                    }
                } else {
                    leading = false;
                }
            }
            if (discarded == false) {
                int lastStage = criteria.size() - 1;
                if ((firstTrailing == null && lastLeading == null)
                    || (lastLeading == null && matcher.isMissingEvent(0))
                    || (firstTrailing == null && matcher.isMissingEvent(lastStage))
                    || (matcher.isMissingEvent(0) && matcher.isMissingEvent(lastStage) && biggerThanMaxSpan(lastLeading, firstTrailing))
                    || (matcher.isMissingEvent(0)
                        && matcher.isMissingEvent(lastStage) == false
                        && biggerThanMaxSpan(lastLeading, sequence.ordinal().timestamp()))
                    || (matcher.isMissingEvent(0) == false
                        && matcher.isMissingEvent(lastStage)
                        && biggerThanMaxSpan(sequence.startOrdinal().timestamp(), firstTrailing))

                ) {
                    matcher.addToCompleted(sequence);
                }
            }
        }
        checkMissingEvents(next, listener);
    }

    private boolean biggerThanMaxSpan(Timestamp from, Timestamp to) {
        if (from == null || to == null) {
            return true;
        }
        return matcher.exceedsMaxSpan(from, to);
    }

    private List<SearchRequest> prepareQueryForMissingEvents(List<Sequence> toCheck) {
        List<SearchRequest> result = new ArrayList<>();
        for (Sequence sequence : toCheck) {
            boolean leading = true;
            for (int i = 0; i < criteria.size(); i++) {
                SequenceCriterion criterion = criteria.get(i);
                if (criterion.missing()) {
                    BoxedQueryRequest r = criterion.queryRequest();
                    RangeQueryBuilder range = r.timestampRangeQuery();
                    SearchSourceBuilder builder = copySource(r.searchSource());
                    if (leading) {
                        builder.sorts().clear();
                        builder.sort(r.timestampField(), SortOrder.DESC);
                        range.lt(sequence.startOrdinal().timestamp().instant().toEpochMilli());
                    } else if (trailing(i)) {
                        builder.sorts().clear();
                        builder.sort(r.timestampField(), SortOrder.ASC);
                        range.gt(sequence.ordinal().timestamp().instant().toEpochMilli());
                    } else {
                        range.lt(sequence.matchAt(matcher.nextPositiveStage(i)).ordinal().timestamp().instant().toEpochMilli());
                        range.gt(sequence.matchAt(matcher.previousPositiveStage(i)).ordinal().timestamp().instant().toEpochMilli());
                        builder.sort(r.timestampField(), SortOrder.ASC);
                    }
                    addKeyFilter(i, sequence, builder);
                    RuntimeUtils.combineFilters(builder, range);
                    result.add(RuntimeUtils.prepareRequest(builder.size(1).trackTotalHits(false), false, Strings.EMPTY_ARRAY));
                } else {
                    leading = false;
                }
            }
        }
        return result;
    }

    private void addKeyFilter(int stage, Sequence sequence, SearchSourceBuilder builder) {
        List<Attribute> keys = listOfKeys.get(stage);
        if (keys.isEmpty()) {
            return;
        }
        for (int i = 0; i < keys.size(); i++) {
            Attribute k = keys.get(i);
            combineFilters(builder, new TermQueryBuilder(k.qualifiedName(), sequence.key().asList().get(i)));
        }
    }

    private boolean trailing(int i) {
        return matcher.nextPositiveStage(i - 1) < 0;
    }

    private void advance(int stage, ActionListener<Payload> listener) {
        SequenceCriterion base = criteria.get(stage);
        base.queryRequest().to(null);

        if (hasKeys) {
            addKeyConstraints(matcher.previousPositiveStage(stage), base.queryRequest());
        }

        log.trace("{}", matcher);
        log.trace("Querying base stage [{}] {}", stage, base.queryRequest());

        client.query(base.queryRequest(), listener.delegateFailureAndWrap((l, p) -> baseCriterion(stage, p, l)));
    }

    /**
     * Execute the base query.
     */
    private void baseCriterion(int baseStage, SearchResponse r, ActionListener<Payload> listener) {
        SequenceCriterion base = criteria.get(baseStage);
        SearchHits hits = r.getHits();

        log.trace("Found [{}] hits", hits.getHits().length);

        Ordinal begin = null, end = null;
        WindowInfo info;

        if (hits.getHits().length > 0) {
            var hitsAsList = Arrays.asList(hits.getHits());
            begin = headOrdinal(hitsAsList, base);
            end = tailOrdinal(hitsAsList, base);
            info = new WindowInfo(baseStage, begin, end);

            log.trace("Found {}base [{}] window {}->{}", base.descending() ? "tail " : "", base.stage(), begin, end);

            base.queryRequest().nextAfter(end);

            if (until != null && baseStage > 0) {
                hits.incRef();
                untilCriterion(info, listener, () -> {
                    try {
                        completeBaseCriterion(baseStage, hits, info, listener);
                    } finally {
                        hits.decRef();
                    }
                });
                return;
            }
        } else {
            info = null;
            if (baseStage == matcher.firstPositiveStage && baseStage == matcher.lastPositiveStage) {
                payload(listener);
                return;
            }
        }
        completeBaseCriterion(baseStage, hits, info, listener);
    }

    private void completeBaseCriterion(int baseStage, SearchHits hits, WindowInfo info, ActionListener<Payload> listener) {
        SequenceCriterion base = criteria.get(baseStage);

        if (matcher.match(baseStage, wrapValues(base, Arrays.asList(hits.getHits()))) == false) {
            payload(listener);
            return;
        }

        int nextStage = nextPositiveStage(baseStage);
        boolean windowCompleted = hits.getHits().length < windowSize;

        if (nextStage > 0) { 
            boolean descendingQuery = base.descending();
            Runnable next = null;

            if (info != null) {
                if (descendingQuery) {
                    setupWindowFromTail(info.end);
                } else {
                    boxQuery(info, criteria.get(nextStage));
                }
            }

            if (windowCompleted) {
                boolean shouldTerminate = false;

                if (descendingQuery) {
                    if (info != null) {
                        restartWindowFromTailQuery = false;
                        final int stage = nextPositiveStage(matcher.firstPositiveStage);
                        next = () -> checkMissingEvents(() -> advance(stage, listener), listener);
                    }
                    else {
                        shouldTerminate = true;
                    }
                }
                else {
                    if (matcher.hasFollowingCandidates(matcher.previousPositiveStage(nextStage))) {
                        next = () -> rebaseWindow(nextStage, listener);
                    }
                    else {
                        if (restartWindowFromTailQuery == false) {
                            shouldTerminate = true;
                        } else {
                            next = () -> tumbleWindow(matcher.firstPositiveStage, listener);
                        }
                    }
                }
                if (shouldTerminate) {
                    payload(listener);
                    return;
                }
            }
            else {
                if (descendingQuery) {
                    next = () -> advance(nextPositiveStage(matcher.firstPositiveStage), listener);
                }
                else {
                    next = () -> secondaryCriterion(info, nextStage, listener);
                }
            }

            if (until != null && info != null && info.baseStage == matcher.firstPositiveStage) {
                untilCriterion(info, listener, next);
            } else {
                next.run();
            }
        }
        else {
            if (windowCompleted) {
                if (restartWindowFromTailQuery) {
                    tumbleWindow(matcher.firstPositiveStage, listener);
                } else {
                    payload(listener);
                }
            }
            else {
                tumbleWindow(baseStage, listener);
            }
        }
    }

    private int nextPositiveStage(int current) {
        return matcher.nextPositiveStage(current);
    }

    private void untilCriterion(WindowInfo window, ActionListener<Payload> listener, Runnable next) {
        BoxedQueryRequest request = until.queryRequest();
        boxQuery(window, until);

        if (request.after().after(window.end)) {
            log.trace("Skipping until stage {}", request);
            next.run();
            return;
        }

        log.trace("Querying until stage {}", request);

        client.query(request, listener.delegateFailureAndWrap((delegate, r) -> {
            List<SearchHit> hits = Arrays.asList(r.getHits().getHits());

            log.trace("Found [{}] hits", hits.size());
            if (hits.isEmpty() == false) {
                request.nextAfter(tailOrdinal(hits, until));
                matcher.until(wrapUntilValues(wrapValues(until, hits)));
            }

            if (hits.size() == windowSize && request.after().before(window.end)) {
                untilCriterion(window, delegate, next);
            }
            else {
                next.run();
            }
        }));
    }

    private void secondaryCriterion(WindowInfo window, int currentStage, ActionListener<Payload> listener) {
        SequenceCriterion criterion = criteria.get(currentStage);
        BoxedQueryRequest request = criterion.queryRequest();

        boxQuery(window, criterion);

        log.trace("Querying (secondary) stage [{}] {}", criterion.stage(), request);

        client.query(request, listener.delegateFailureAndWrap((delegate, r) -> {
            List<SearchHit> hits = Arrays.asList(r.getHits().getHits());


            hits = trim(hits, criterion, window.end);

            log.trace("Found [{}] hits", hits.size());

            int nextPositiveStage = nextPositiveStage(currentStage);

            if (hits.isEmpty() == false) {
                Ordinal tailOrdinal = tailOrdinal(hits, criterion);
                Ordinal headOrdinal = headOrdinal(hits, criterion);

                log.trace("Found range [{}] -> [{}]", headOrdinal, tailOrdinal);

                if (tailOrdinal.after(window.end)) {
                    tailOrdinal = window.end;
                }
                request.nextAfter(tailOrdinal);

                if (matcher.match(criterion.stage(), wrapValues(criterion, hits)) == false) {
                    payload(delegate);
                    return;
                }

                if (nextPositiveStage > 0) {
                    BoxedQueryRequest nextRequest = criteria.get(nextPositiveStage).queryRequest();
                    if (nextRequest.from() == null || nextRequest.after() == null) {
                        nextRequest.from(headOrdinal);
                        nextRequest.nextAfter(headOrdinal);
                    }
                }
            }

            if (hits.size() == windowSize && request.after().before(window.end)) {
                secondaryCriterion(window, currentStage, delegate);
            }
            else {
                if (nextPositiveStage > 0 && matcher.hasFollowingCandidates(criterion.stage())) {
                    secondaryCriterion(window, nextPositiveStage, delegate);
                } else {
                    tumbleWindow(window.baseStage, delegate);
                }
            }
        }));
    }

    /**
     * Trim hits outside the (upper) limit.
     */
    private static List<SearchHit> trim(List<SearchHit> searchHits, SequenceCriterion criterion, Ordinal boundary) {
        int offset = 0;

        for (int i = searchHits.size() - 1; i >= 0; i--) {
            Ordinal ordinal = criterion.ordinal(searchHits.get(i));
            if (ordinal.after(boundary)) {
                offset++;
            } else {
                break;
            }
        }
        return offset == 0 ? searchHits : searchHits.subList(0, searchHits.size() - offset);
    }

    /**
     * Box the query for the given (ASC) criterion based on the window information.
     */
    private void boxQuery(WindowInfo window, SequenceCriterion criterion) {
        BoxedQueryRequest request = criterion.queryRequest();
        if (window.end.equals(request.to()) == false) {
            request.to(window.end);
        }

        if (request.from() == null) {
            request.from(window.begin);
            request.nextAfter(window.begin);
        }

        if (hasKeys) {
            int stage = criterion == until ? Integer.MIN_VALUE : window.baseStage;
            addKeyConstraints(stage, request);
        }
    }

    /**
     * Used by TAIL sequences. Sets the starting point of the (ASC) window.
     * It does that by initializing the from of the stage 1 (the window base)
     * and resets "from" from the other sub-queries so they can initialized accordingly
     * (based on the results of their predecessors).
     */
    private void setupWindowFromTail(Ordinal from) {
        int secondPositiveStage = nextPositiveStage(matcher.firstPositiveStage);
        BoxedQueryRequest request = criteria.get(secondPositiveStage).queryRequest();

        if (from.equals(request.from()) == false) {
            request.from(from).nextAfter(from);

            if (until != null) {
                until.queryRequest().from(from).nextAfter(from);
            }
            for (int i = secondPositiveStage + 1; i < maxStages; i++) {
                BoxedQueryRequest subRequest = criteria.get(i).queryRequest();
                subRequest.from(null);
            }
        }
    }

    private void addKeyConstraints(int keyStage, BoxedQueryRequest request) {
        if (keyStage >= 0 || keyStage == Integer.MIN_VALUE) {
            Set<SequenceKey> keys = keyStage == Integer.MIN_VALUE ? matcher.keys() : matcher.keys(keyStage);
            int size = keys.size();
            if (size > 0) {
                request.keys(keys.stream().map(SequenceKey::asList).collect(toList()));
            } else {
                request.keys(null);
            }
        }
        else {
            request.keys(null);
        }
    }

    private void payload(ActionListener<Payload> listener) {
        checkMissingEvents(() -> doPayload(listener), listener);
    }

    private void doPayload(ActionListener<Payload> listener) {
        List<Sequence> completed = matcher.completed();

        log.trace("Sending payload for [{}] sequences", completed.size());

        if (completed.isEmpty()) {
            listener.onResponse(new EmptyPayload(Type.SEQUENCE, timeTook()));
            return;
        }

        client.fetchHits(hits(completed), ActionListeners.map(listener, listOfHits -> {
            if (criteria.get(matcher.firstPositiveStage).descending()) {
                Collections.reverse(completed);
            }
            return new SequencePayload(completed, addMissingEventPlaceholders(listOfHits), false, timeTook());
        }));
    }

    private List<List<SearchHit>> addMissingEventPlaceholders(List<List<SearchHit>> hitLists) {
        List<List<SearchHit>> result = new ArrayList<>();

        for (List<SearchHit> hits : hitLists) {
            List<SearchHit> filled = new ArrayList<>();
            result.add(filled);
            int nextHit = 0;
            for (int i = 0; i < criteria.size(); i++) {
                if (matcher.isMissingEvent(i)) {
                    filled.add(null);
                } else {
                    filled.add(hits.get(nextHit++));
                }
            }
        }
        return result;
    }

    private TimeValue timeTook() {
        return new TimeValue(System.currentTimeMillis() - startTime);
    }

    private String cache(String string) {
        String value = stringCache.putIfAbsent(string, string);
        return value == null ? string : value;
    }

    private SequenceKey key(Object[] keys) {
        SequenceKey key;
        if (keys == null) {
            key = SequenceKey.NONE;
        } else {
            for (int i = 0; i < keys.length; i++) {
                Object o = keys[i];
                if (o instanceof String s) {
                    keys[i] = cache(s);
                }
            }
            key = new SequenceKey(keys);
        }

        return key;
    }

    private static Ordinal headOrdinal(List<SearchHit> hits, SequenceCriterion criterion) {
        return criterion.ordinal(hits.get(0));
    }

    private static Ordinal tailOrdinal(List<SearchHit> hits, SequenceCriterion criterion) {
        return criterion.ordinal(hits.get(hits.size() - 1));
    }

    Iterable<List<HitReference>> hits(List<Sequence> sequences) {
        return () -> {
            Iterator<Sequence> delegate = criteria.get(matcher.firstPositiveStage).descending()
                ? new ReversedIterator<>(sequences)
                : sequences.iterator();

            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public List<HitReference> next() {
                    List<HitReference> result = new ArrayList<>();
                    List<HitReference> originalHits = delegate.next().hits();
                    for (HitReference hit : originalHits) {
                        if (hit != null) {
                            result.add(hit);
                        }
                    }
                    return result;
                }
            };
        };
    }

    Iterable<Tuple<KeyAndOrdinal, HitReference>> wrapValues(SequenceCriterion criterion, List<SearchHit> hits) {
        return () -> {
            Iterator<SearchHit> delegate = criterion.descending() ? new ReversedIterator<>(hits) : hits.iterator();

            return new Iterator<>() {

                SearchHit lastFetchedHit = delegate.hasNext() ? delegate.next() : null;
                List<Object[]> remainingHitJoinKeys = lastFetchedHit == null ? Collections.emptyList() : extractJoinKeys(lastFetchedHit);

                /**
                 * extract the join key from a hit. If there are multivalues, the result is the cartesian product.
                 * eg.
                 * - if the key is ['a', 'b'], the result is a list containing ['a', 'b']
                 * - if the key is ['a', ['b', 'c]], the result is a list containing ['a', 'b'] and ['a', 'c']
                 */
                private List<Object[]> extractJoinKeys(SearchHit hit) {
                    if (hit == null) {
                        return null;
                    }
                    Object[] originalKeys = criterion.key(hit);

                    List<Object[]> partial = new ArrayList<>();
                    if (originalKeys == null) {
                        partial.add(null);
                    } else {
                        int keySize = originalKeys.length;
                        partial.add(new Object[keySize]);
                        for (int i = 0; i < keySize; i++) {
                            if (originalKeys[i] instanceof List<?> possibleValues) {
                                List<Object[]> newPartial = new ArrayList<>(possibleValues.size() * partial.size());
                                for (Object possibleValue : possibleValues) {
                                    for (Object[] partialKey : partial) {
                                        Object[] newKey = new Object[keySize];
                                        if (i > 0) {
                                            System.arraycopy(partialKey, 0, newKey, 0, i);
                                        }
                                        newKey[i] = possibleValue;
                                        newPartial.add(newKey);
                                    }
                                }
                                partial = newPartial;
                            } else {
                                for (Object[] key : partial) {
                                    key[i] = originalKeys[i];
                                }
                            }
                        }
                    }
                    return partial;
                }

                @Override
                public boolean hasNext() {
                    return CollectionUtils.isEmpty(remainingHitJoinKeys) == false || delegate.hasNext();
                }

                @Override
                public Tuple<KeyAndOrdinal, HitReference> next() {
                    if (remainingHitJoinKeys.isEmpty()) {
                        lastFetchedHit = delegate.next();
                        remainingHitJoinKeys = extractJoinKeys(lastFetchedHit);
                    }
                    Object[] joinKeys = remainingHitJoinKeys.remove(0);

                    SequenceKey k = key(joinKeys);
                    Ordinal o = criterion.ordinal(lastFetchedHit);
                    return new Tuple<>(
                        new KeyAndOrdinal(k, o),
                        new HitReference(cache(qualifiedIndex(lastFetchedHit)), lastFetchedHit.getId())
                    );
                }
            };
        };
    }

    <E> Iterable<KeyAndOrdinal> wrapUntilValues(Iterable<Tuple<KeyAndOrdinal, E>> iterable) {
        return () -> {
            Iterator<Tuple<KeyAndOrdinal, E>> delegate = iterable.iterator();

            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public KeyAndOrdinal next() {
                    return delegate.next().v1();
                }
            };
        };
    }
}
