/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.indices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.store.AlreadyClosedException;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.index.shard.IndexShardState;
import org.elasticsearch.index.shard.IndexingOperationListener;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.threadpool.Scheduler.Cancellable;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class IndexingMemoryController implements IndexingOperationListener, Closeable {

    private static final Logger logger = LogManager.getLogger(IndexingMemoryController.class);

    /** How much heap (% or bytes) we will share across all actively indexing shards on this node (default: 10%). */
    public static final Setting<ByteSizeValue> INDEX_BUFFER_SIZE_SETTING = Setting.memorySizeSetting(
        "indices.memory.index_buffer_size",
        "10%",
        Property.NodeScope
    );

    /** Only applies when <code>indices.memory.index_buffer_size</code> is a %,
     * to set a floor on the actual size in bytes (default: 48 MB). */
    public static final Setting<ByteSizeValue> MIN_INDEX_BUFFER_SIZE_SETTING = Setting.byteSizeSetting(
        "indices.memory.min_index_buffer_size",
        new ByteSizeValue(48, ByteSizeUnit.MB),
        ByteSizeValue.ZERO,
        ByteSizeValue.ofBytes(Long.MAX_VALUE),
        Property.NodeScope
    );

    /** Only applies when <code>indices.memory.index_buffer_size</code> is a %,
     * to set a ceiling on the actual size in bytes (default: not set). */
    public static final Setting<ByteSizeValue> MAX_INDEX_BUFFER_SIZE_SETTING = Setting.byteSizeSetting(
        "indices.memory.max_index_buffer_size",
        ByteSizeValue.MINUS_ONE,
        ByteSizeValue.MINUS_ONE,
        ByteSizeValue.ofBytes(Long.MAX_VALUE),
        Property.NodeScope
    );

    /** If we see no indexing operations after this much time for a given shard,
     * we consider that shard inactive (default: 5 minutes). */
    public static final Setting<TimeValue> SHARD_INACTIVE_TIME_SETTING = Setting.positiveTimeSetting(
        "indices.memory.shard_inactive_time",
        TimeValue.timeValueMinutes(5),
        Property.NodeScope
    );

    /** How frequently we check indexing memory usage (default: 5 seconds). */
    public static final Setting<TimeValue> SHARD_MEMORY_INTERVAL_TIME_SETTING = Setting.positiveTimeSetting(
        "indices.memory.interval",
        TimeValue.timeValueSeconds(5),
        Property.NodeScope
    );

    private final ThreadPool threadPool;

    private final Iterable<IndexShard> indexShards;

    private final ByteSizeValue indexingBuffer;

    private final TimeValue inactiveTime;
    private final TimeValue interval;

    /** Contains shards currently being throttled because we can't write segments quickly enough */
    private final Set<IndexShard> throttled = new HashSet<>();

    private final Cancellable scheduler;

    private static final EnumSet<IndexShardState> CAN_WRITE_INDEX_BUFFER_STATES = EnumSet.of(
        IndexShardState.RECOVERING,
        IndexShardState.POST_RECOVERY,
        IndexShardState.STARTED
    );

    private final ShardsIndicesStatusChecker statusChecker;

    private final Set<IndexShard> pendingWriteIndexingBufferSet = ConcurrentCollections.newConcurrentSet();
    private final Deque<IndexShard> pendingWriteIndexingBufferQueue = new ConcurrentLinkedDeque<>();

    IndexingMemoryController(Settings settings, ThreadPool threadPool, Iterable<IndexShard> indexServices) {
        this.indexShards = indexServices;

        ByteSizeValue indexingBuffer = INDEX_BUFFER_SIZE_SETTING.get(settings);

        String indexingBufferSetting = settings.get(INDEX_BUFFER_SIZE_SETTING.getKey());
        if (indexingBufferSetting == null || indexingBufferSetting.endsWith("%")) {
            ByteSizeValue minIndexingBuffer = MIN_INDEX_BUFFER_SIZE_SETTING.get(settings);
            ByteSizeValue maxIndexingBuffer = MAX_INDEX_BUFFER_SIZE_SETTING.get(settings);
            if (indexingBuffer.getBytes() < minIndexingBuffer.getBytes()) {
                indexingBuffer = minIndexingBuffer;
            }
            if (maxIndexingBuffer.getBytes() != -1 && indexingBuffer.getBytes() > maxIndexingBuffer.getBytes()) {
                indexingBuffer = maxIndexingBuffer;
            }
        }
        this.indexingBuffer = indexingBuffer;

        this.inactiveTime = SHARD_INACTIVE_TIME_SETTING.get(settings);
        this.interval = SHARD_MEMORY_INTERVAL_TIME_SETTING.get(settings);

        this.statusChecker = new ShardsIndicesStatusChecker();

        logger.debug(
            "using indexing buffer size [{}] with {} [{}], {} [{}]",
            this.indexingBuffer,
            SHARD_INACTIVE_TIME_SETTING.getKey(),
            this.inactiveTime,
            SHARD_MEMORY_INTERVAL_TIME_SETTING.getKey(),
            this.interval
        );
        this.scheduler = scheduleTask(threadPool);

        this.threadPool = threadPool;
    }

    protected Cancellable scheduleTask(ThreadPool threadPool) {
        return threadPool.scheduleWithFixedDelay(statusChecker, interval, EsExecutors.DIRECT_EXECUTOR_SERVICE);
    }

    @Override
    public void close() {
        scheduler.cancel();
    }

    /**
     * returns the current budget for the total amount of indexing buffers of
     * active shards on this node
     */
    ByteSizeValue indexingBufferSize() {
        return indexingBuffer;
    }

    protected List<IndexShard> availableShards() {
        List<IndexShard> availableShards = new ArrayList<>();
        for (IndexShard shard : indexShards) {
            if (CAN_WRITE_INDEX_BUFFER_STATES.contains(shard.state())) {
                availableShards.add(shard);
            }
        }
        return availableShards;
    }

    /** returns how much heap this shard is using for its indexing buffer */
    protected long getIndexBufferRAMBytesUsed(IndexShard shard) {
        return shard.getIndexBufferRAMBytesUsed();
    }

    /** returns how many bytes this shard is currently writing to disk */
    protected long getShardWritingBytes(IndexShard shard) {
        return shard.getWritingBytes();
    }

    /** Record that the given shard needs to write its indexing buffer. */
    protected void enqueueWriteIndexingBuffer(IndexShard shard) {
        if (pendingWriteIndexingBufferSet.add(shard)) {
            pendingWriteIndexingBufferQueue.addLast(shard);
        }
    }

    /**
     * Write pending indexing buffers. This should run on indexing threads in order to naturally apply back pressure on indexing. Lucene has
     * similar logic in DocumentsWriter#postUpdate.
     */
    private boolean writePendingIndexingBuffers() {
        boolean wrotePendingIndexingBuffer = false;
        for (IndexShard shard = pendingWriteIndexingBufferQueue.pollFirst(); shard != null; shard = pendingWriteIndexingBufferQueue
            .pollFirst()) {
            pendingWriteIndexingBufferSet.remove(shard);
            shard.writeIndexingBuffer();
            wrotePendingIndexingBuffer = true;
        }
        return wrotePendingIndexingBuffer;
    }

    private void writePendingIndexingBuffersAsync() {
        for (IndexShard shard = pendingWriteIndexingBufferQueue.pollFirst(); shard != null; shard = pendingWriteIndexingBufferQueue
            .pollFirst()) {
            final IndexShard finalShard = shard;
            threadPool.executor(ThreadPool.Names.REFRESH).execute(() -> {
                pendingWriteIndexingBufferSet.remove(finalShard);
                finalShard.writeIndexingBuffer();
            });
        }
    }

    /** force checker to run now */
    void forceCheck() {
        statusChecker.run();
    }

    /** Asks this shard to throttle indexing to one thread */
    protected void activateThrottling(IndexShard shard) {
        shard.activateThrottling();
    }

    /** Asks this shard to stop throttling indexing to one thread */
    protected void deactivateThrottling(IndexShard shard) {
        shard.deactivateThrottling();
    }

    @Override
    public void postIndex(ShardId shardId, Engine.Index index, Engine.IndexResult result) {
        postOperation(shardId, index, result);
    }

    @Override
    public void postDelete(ShardId shardId, Engine.Delete delete, Engine.DeleteResult result) {
        postOperation(shardId, delete, result);
    }

    private void postOperation(ShardId shardId, Engine.Operation operation, Engine.Result result) {
        recordOperationBytes(operation, result);
        while (writePendingIndexingBuffers()) {
            if (statusChecker.tryRun() == false) {
                break;
            }
        }
    }

    /** called by IndexShard to record estimated bytes written to translog for the operation */
    private void recordOperationBytes(Engine.Operation operation, Engine.Result result) {
        if (result.getResultType() == Engine.Result.Type.SUCCESS) {
            statusChecker.bytesWritten(operation.estimatedSizeInBytes());
        }
    }

    private static final class ShardAndBytesUsed {
        final long bytesUsed;
        final IndexShard shard;

        ShardAndBytesUsed(long bytesUsed, IndexShard shard) {
            this.bytesUsed = bytesUsed;
            this.shard = shard;
        }

    }

    /** not static because we need access to many fields/methods from our containing class (IMC): */
    final class ShardsIndicesStatusChecker implements Runnable {

        final AtomicLong bytesWrittenSinceCheck = new AtomicLong();
        final ReentrantLock runLock = new ReentrantLock();
        private ShardId lastShardId = null;

        /** Shard calls this on each indexing/delete op */
        public void bytesWritten(int bytes) {
            long totalBytes = bytesWrittenSinceCheck.addAndGet(bytes);
            assert totalBytes >= 0;
            while (totalBytes > indexingBuffer.getBytes() / 128) {

                if (runLock.tryLock()) {
                    try {
                        totalBytes = bytesWrittenSinceCheck.get();
                        if (totalBytes > indexingBuffer.getBytes() / 128) {
                            bytesWrittenSinceCheck.addAndGet(-totalBytes);
                            runUnlocked();
                        }
                    } finally {
                        runLock.unlock();
                    }

                    totalBytes = bytesWrittenSinceCheck.get();
                } else {
                    break;
                }
            }
        }

        public boolean tryRun() {
            if (runLock.tryLock()) {
                try {
                    runUnlocked();
                } finally {
                    runLock.unlock();
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void run() {
            writePendingIndexingBuffersAsync();
            runLock.lock();
            try {
                runUnlocked();
            } finally {
                runLock.unlock();
            }
        }

        private void runUnlocked() {
            assert runLock.isHeldByCurrentThread() : "ShardsIndicesStatusChecker#runUnlocked must always run under the run lock";

            long totalBytesUsed = 0;
            long totalBytesWriting = 0;
            for (IndexShard shard : availableShards()) {

                checkIdle(shard, inactiveTime.nanos());

                long shardWritingBytes = getShardWritingBytes(shard);

                long shardBytesUsed = getIndexBufferRAMBytesUsed(shard);

                shardBytesUsed -= shardWritingBytes;
                totalBytesWriting += shardWritingBytes;

                if (shardBytesUsed < 0) {
                    continue;
                }

                totalBytesUsed += shardBytesUsed;
            }

            if (logger.isTraceEnabled()) {
                logger.trace(
                    "total indexing heap bytes used [{}] vs {} [{}], currently writing bytes [{}]",
                    ByteSizeValue.ofBytes(totalBytesUsed),
                    INDEX_BUFFER_SIZE_SETTING.getKey(),
                    indexingBuffer,
                    ByteSizeValue.ofBytes(totalBytesWriting)
                );
            }

            boolean doThrottle = (totalBytesWriting + totalBytesUsed) > 1.5 * indexingBuffer.getBytes();

            if (totalBytesUsed > indexingBuffer.getBytes()) {
                List<ShardAndBytesUsed> queue = new ArrayList<>();

                for (IndexShard shard : availableShards()) {
                    long shardWritingBytes = getShardWritingBytes(shard);

                    long shardBytesUsed = getIndexBufferRAMBytesUsed(shard);

                    shardBytesUsed -= shardWritingBytes;

                    if (shardBytesUsed < 0) {
                        continue;
                    }

                    if (shardBytesUsed > 0) {
                        if (logger.isTraceEnabled()) {
                            if (shardWritingBytes != 0) {
                                logger.trace(
                                    "shard [{}] is using [{}] heap, writing [{}] heap",
                                    shard.shardId(),
                                    shardBytesUsed,
                                    shardWritingBytes
                                );
                            } else {
                                logger.trace("shard [{}] is using [{}] heap, not writing any bytes", shard.shardId(), shardBytesUsed);
                            }
                        }
                        queue.add(new ShardAndBytesUsed(shardBytesUsed, shard));
                    }
                }

                logger.debug(
                    "now write some indexing buffers: total indexing heap bytes used [{}] vs {} [{}], "
                        + "currently writing bytes [{}], [{}] shards with non-zero indexing buffer",
                    ByteSizeValue.ofBytes(totalBytesUsed),
                    INDEX_BUFFER_SIZE_SETTING.getKey(),
                    indexingBuffer,
                    ByteSizeValue.ofBytes(totalBytesWriting),
                    queue.size()
                );


                queue.sort(Comparator.comparing(shardAndBytes -> shardAndBytes.shard.shardId()));
                if (lastShardId != null) {
                    int nextShardIdIndex = 0;
                    for (ShardAndBytesUsed shardAndBytes : queue) {
                        if (shardAndBytes.shard.shardId().compareTo(lastShardId) > 0) {
                            break;
                        }
                        nextShardIdIndex++;
                    }
                    Collections.rotate(queue, -nextShardIdIndex);
                }

                for (ShardAndBytesUsed shardAndBytesUsed : queue) {
                    logger.debug(
                        "write indexing buffer to disk for shard [{}] to free up its [{}] indexing buffer",
                        shardAndBytesUsed.shard.shardId(),
                        ByteSizeValue.ofBytes(shardAndBytesUsed.bytesUsed)
                    );
                    enqueueWriteIndexingBuffer(shardAndBytesUsed.shard);
                    totalBytesUsed -= shardAndBytesUsed.bytesUsed;
                    lastShardId = shardAndBytesUsed.shard.shardId();
                    if (doThrottle && throttled.contains(shardAndBytesUsed.shard) == false) {
                        logger.debug(
                            "now throttling indexing for shard [{}]: segment writing can't keep up",
                            shardAndBytesUsed.shard.shardId()
                        );
                        throttled.add(shardAndBytesUsed.shard);
                        activateThrottling(shardAndBytesUsed.shard);
                    }
                    if (totalBytesUsed <= indexingBuffer.getBytes()) {
                        break;
                    }
                }

            }

            if (doThrottle == false) {
                for (IndexShard shard : throttled) {
                    logger.info("stop throttling indexing for shard [{}]", shard.shardId());
                    deactivateThrottling(shard);
                }
                throttled.clear();
            }
        }
    }

    /**
     * ask this shard to check now whether it is inactive, and reduces its indexing buffer if so.
     */
    protected void checkIdle(IndexShard shard, long inactiveTimeNS) {
        try {
            shard.flushOnIdle(inactiveTimeNS);
        } catch (AlreadyClosedException e) {
            logger.trace(() -> "ignore exception while checking if shard " + shard.shardId() + " is inactive", e);
        }
    }
}
