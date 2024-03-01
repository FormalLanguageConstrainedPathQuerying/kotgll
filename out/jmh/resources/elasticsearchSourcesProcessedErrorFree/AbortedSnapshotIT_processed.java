/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.snapshots;

import org.elasticsearch.cluster.SnapshotsInProgress;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.snapshots.IndexShardSnapshotStatus;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.threadpool.ThreadPool;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 0)
public class AbortedSnapshotIT extends AbstractSnapshotIntegTestCase {

    public void testQueuedSnapshotReleasesCommitOnAbort() throws Exception {
        internalCluster().startMasterOnlyNode();
        final String dataNode = internalCluster().startDataOnlyNode(Settings.builder().put("thread_pool.snapshot.max", 1).build());
        final String indexName = "test-index";
        createIndexWithContent(indexName);

        final var indicesService = internalCluster().getInstance(IndicesService.class, dataNode);
        final var clusterService = indicesService.clusterService();
        final var index = clusterService.state().metadata().index(indexName).getIndex();
        final var store = indicesService.indexServiceSafe(index).getShard(0).store();
        assertTrue(store.hasReferences());

        final String repoName = "test-repo";
        createRepository(repoName, "fs");

        final var snapshotExecutor = internalCluster().getInstance(ThreadPool.class, dataNode).executor(ThreadPool.Names.SNAPSHOT);

        final CyclicBarrier barrier = new CyclicBarrier(2);
        final AtomicBoolean stopBlocking = new AtomicBoolean();
        class BlockingTask implements Runnable {
            @Override
            public void run() {
                safeAwait(barrier);
                safeAwait(barrier);
                if (stopBlocking.get() == false) {
                    snapshotExecutor.execute(BlockingTask.this);
                }
            }
        }
        snapshotExecutor.execute(new BlockingTask());
        safeAwait(barrier); 

        clusterAdmin().prepareCreateSnapshot(repoName, "snapshot-1").setWaitForCompletion(false).setPartial(true).get();

        final var snapshot = SnapshotsInProgress.get(clusterService.state()).forRepo(repoName).get(0).snapshot();
        final var snapshotShardsService = internalCluster().getInstance(SnapshotShardsService.class, dataNode);


        final var steps = between(0, 3);
        for (int i = 0; i < steps; i++) {
            safeAwait(barrier); 
            safeAwait(barrier); 

            final var shardStatuses = snapshotShardsService.currentSnapshotShards(snapshot);
            assertEquals(1, shardStatuses.size());
            final var shardStatus = shardStatuses.get(new ShardId(index, 0));
            logger.info("--> {}", shardStatus);

            if (i == 0) {
                assertEquals(IndexShardSnapshotStatus.Stage.INIT, shardStatus.getStage());
                assertEquals(0, shardStatus.getProcessedFileCount());
                assertEquals(0, shardStatus.getTotalFileCount());
            } else {
                assertEquals(IndexShardSnapshotStatus.Stage.STARTED, shardStatus.getStage());
                assertThat(shardStatus.getProcessedFileCount(), greaterThan(0));
                assertThat(shardStatus.getProcessedFileCount(), lessThan(shardStatus.getTotalFileCount()));
            }
        }

        assertTrue(store.hasReferences());
        assertAcked(indicesAdmin().prepareDelete(indexName).get());

        assertBusy(() -> assertFalse(store.hasReferences()));

        stopBlocking.set(true);
        safeAwait(barrier); 

        assertBusy(() -> assertTrue(SnapshotsInProgress.get(clusterService.state()).isEmpty()));
    }

}
