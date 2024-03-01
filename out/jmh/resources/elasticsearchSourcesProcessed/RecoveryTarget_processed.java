/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.indices.recovery;

import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexFormatTooNewException;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.bytes.ReleasableBytesReference;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.lucene.Lucene;
import org.elasticsearch.common.util.CancellableThreads;
import org.elasticsearch.core.AbstractRefCounted;
import org.elasticsearch.core.Assertions;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.Releasable;
import org.elasticsearch.core.Releasables;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.mapper.MapperException;
import org.elasticsearch.index.seqno.ReplicationTracker;
import org.elasticsearch.index.seqno.RetentionLeases;
import org.elasticsearch.index.seqno.SequenceNumbers;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.index.shard.IndexShardNotRecoveringException;
import org.elasticsearch.index.shard.IndexShardState;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.snapshots.blobstore.BlobStoreIndexShardSnapshot;
import org.elasticsearch.index.store.Store;
import org.elasticsearch.index.store.StoreFileMetadata;
import org.elasticsearch.index.translog.Translog;
import org.elasticsearch.repositories.IndexId;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.elasticsearch.core.Strings.format;

/**
 * Represents a recovery where the current node is the target node of the recovery. To track recoveries in a central place, instances of
 * this class are created through {@link RecoveriesCollection}.
 */
public class RecoveryTarget extends AbstractRefCounted implements RecoveryTargetHandler {

    private final Logger logger;

    private static final AtomicLong idGenerator = new AtomicLong();

    private static final String RECOVERY_PREFIX = "recovery.";

    private final ShardId shardId;
    private final long recoveryId;
    private final IndexShard indexShard;
    private final DiscoveryNode sourceNode;
    private final long clusterStateVersion;
    private final SnapshotFilesProvider snapshotFilesProvider;
    private volatile MultiFileWriter multiFileWriter;
    private final RecoveryRequestTracker requestTracker = new RecoveryRequestTracker();
    private final Store store;
    private final PeerRecoveryTargetService.RecoveryListener listener;

    private final AtomicBoolean finished = new AtomicBoolean();

    private final CancellableThreads cancellableThreads;

    private volatile long lastAccessTime = System.nanoTime();

    private final AtomicInteger recoveryMonitorBlocks = new AtomicInteger();

    @Nullable 
    private volatile Releasable snapshotFileDownloadsPermit;

    private final CountDownLatch closedLatch = new CountDownLatch(1);

    /**
     * Creates a new recovery target object that represents a recovery to the provided shard.
     *
     * @param indexShard                  local shard where we want to recover to
     * @param sourceNode                  source node of the recovery where we recover from
     * @param clusterStateVersion         version of the cluster state that initiated the recovery
     * @param snapshotFileDownloadsPermit a permit that allows to download files from a snapshot,
     *                                    limiting the concurrent snapshot file downloads per node
     *                                    preventing the exhaustion of repository resources.
     * @param listener                    called when recovery is completed/failed
     */
    @SuppressWarnings("this-escape")
    public RecoveryTarget(
        IndexShard indexShard,
        DiscoveryNode sourceNode,
        long clusterStateVersion,
        SnapshotFilesProvider snapshotFilesProvider,
        @Nullable Releasable snapshotFileDownloadsPermit,
        PeerRecoveryTargetService.RecoveryListener listener
    ) {
        this.cancellableThreads = new CancellableThreads();
        this.recoveryId = idGenerator.incrementAndGet();
        this.listener = listener;
        this.logger = Loggers.getLogger(getClass(), indexShard.shardId());
        this.indexShard = indexShard;
        this.sourceNode = sourceNode;
        this.clusterStateVersion = clusterStateVersion;
        this.snapshotFilesProvider = snapshotFilesProvider;
        this.snapshotFileDownloadsPermit = snapshotFileDownloadsPermit;
        this.shardId = indexShard.shardId();
        this.store = indexShard.store();
        this.multiFileWriter = createMultiFileWriter();
        store.incRef();
        indexShard.recoveryStats().incCurrentAsTarget();
    }

    private void recreateMultiFileWriter() {
        multiFileWriter.close();
        this.multiFileWriter = createMultiFileWriter();
    }

    private MultiFileWriter createMultiFileWriter() {
        final String tempFilePrefix = RECOVERY_PREFIX + UUIDs.randomBase64UUID() + ".";
        return new MultiFileWriter(indexShard.store(), indexShard.recoveryState().getIndex(), tempFilePrefix, logger, this::ensureRefCount);
    }

    /**
     * Returns a fresh recovery target to retry recovery from the same source node onto the same shard and using the same listener.
     *
     * @return a copy of this recovery target
     */
    public RecoveryTarget retryCopy() {
        Releasable snapshotFileDownloadsPermitCopy = snapshotFileDownloadsPermit;
        snapshotFileDownloadsPermit = null;
        return new RecoveryTarget(
            indexShard,
            sourceNode,
            clusterStateVersion,
            snapshotFilesProvider,
            snapshotFileDownloadsPermitCopy,
            listener
        );
    }

    @Nullable
    public ActionListener<Void> markRequestReceivedAndCreateListener(long requestSeqNo, ActionListener<Void> listener) {
        return requestTracker.markReceivedAndCreateListener(requestSeqNo, listener);
    }

    public long recoveryId() {
        return recoveryId;
    }

    public ShardId shardId() {
        return shardId;
    }

    public IndexShard indexShard() {
        ensureRefCount();
        return indexShard;
    }

    public DiscoveryNode sourceNode() {
        return this.sourceNode;
    }

    public long clusterStateVersion() {
        return clusterStateVersion;
    }

    public RecoveryState state() {
        return indexShard.recoveryState();
    }

    public CancellableThreads cancellableThreads() {
        return cancellableThreads;
    }

    public boolean hasPermitToDownloadSnapshotFiles() {
        return snapshotFileDownloadsPermit != null;
    }

    /** return the last time this RecoveryStatus was used (based on System.nanoTime() */
    public long lastAccessTime() {
        if (recoveryMonitorBlocks.get() == 0) {
            return lastAccessTime;
        }
        return System.nanoTime();
    }

    /** sets the lasAccessTime flag to now */
    public void setLastAccessTime() {
        lastAccessTime = System.nanoTime();
    }

    /**
     * Set flag to signal to {@link org.elasticsearch.indices.recovery.RecoveriesCollection.RecoveryMonitor} that it must not cancel this
     * recovery temporarily. This is used by the recovery clean files step to avoid recovery failure in case a long running condition was
     * added to the shard via {@link IndexShard#addCleanFilesDependency()}.
     *
     * @return releasable that once closed will re-enable liveness checks by the recovery monitor
     */
    public Releasable disableRecoveryMonitor() {
        recoveryMonitorBlocks.incrementAndGet();
        return Releasables.releaseOnce(() -> {
            setLastAccessTime();
            recoveryMonitorBlocks.decrementAndGet();
        });
    }

    public Store store() {
        ensureRefCount();
        return store;
    }

    /**
     * Closes the current recovery target and waits up to a certain timeout for resources to be freed.
     * Returns true if resetting the recovery was successful, false if the recovery target is already cancelled / failed or marked as done.
     */
    boolean resetRecovery(CancellableThreads newTargetCancellableThreads) throws IOException {
        if (finished.compareAndSet(false, true)) {
            try {
                logger.debug("reset of recovery with shard {} and id [{}]", shardId, recoveryId);
            } finally {
                decRef();
            }
            try {
                newTargetCancellableThreads.execute(closedLatch::await);
            } catch (CancellableThreads.ExecutionCancelledException e) {
                logger.trace(
                    "new recovery target cancelled for shard {} while waiting on old recovery target with id [{}] to close",
                    shardId,
                    recoveryId
                );
                return false;
            }
            RecoveryState.Stage stage = indexShard.recoveryState().getStage();
            if (indexShard.recoveryState().getPrimary() && (stage == RecoveryState.Stage.FINALIZE || stage == RecoveryState.Stage.DONE)) {
                assert stage != RecoveryState.Stage.DONE : "recovery should not have completed when it's being reset";
                throw new IllegalStateException("cannot reset recovery as previous attempt made it past finalization step");
            }
            indexShard.performRecoveryRestart();
            return true;
        }
        return false;
    }

    /**
     * cancel the recovery. calling this method will clean temporary files and release the store
     * unless this object is in use (in which case it will be cleaned once all ongoing users call
     * {@link #decRef()}
     * <p>
     * if {@link #cancellableThreads()} was used, the threads will be interrupted.
     */
    public void cancel(String reason) {
        if (finished.compareAndSet(false, true)) {
            try {
                logger.debug("recovery canceled (reason: [{}])", reason);
                cancellableThreads.cancel(reason);
            } finally {
                decRef();
            }
        }
    }

    /**
     * fail the recovery and call listener
     *
     * @param e                exception that encapsulating the failure
     * @param sendShardFailure indicates whether to notify the master of the shard failure
     */
    public void fail(RecoveryFailedException e, boolean sendShardFailure) {
        if (finished.compareAndSet(false, true)) {
            try {
                notifyListener(e, sendShardFailure);
            } finally {
                try {
                    cancellableThreads.cancel("failed recovery [" + ExceptionsHelper.stackTrace(e) + "]");
                } finally {
                    decRef();
                }
            }
        }
    }

    public void notifyListener(RecoveryFailedException e, boolean sendShardFailure) {
        listener.onRecoveryFailure(e, sendShardFailure);
    }

    /** mark the current recovery as done */
    public void markAsDone() {
        if (finished.compareAndSet(false, true)) {
            assert multiFileWriter.tempFileNames.isEmpty() : "not all temporary files are renamed";
            indexShard.postRecovery("peer recovery done", ActionListener.runBefore(new ActionListener<>() {
                @Override
                public void onResponse(Void unused) {
                    listener.onRecoveryDone(state(), indexShard.getTimestampRange());
                }

                @Override
                public void onFailure(Exception e) {
                    logger.debug("recovery failed after being marked as done", e);
                    notifyListener(new RecoveryFailedException(state(), "Recovery failed on post recovery step", e), true);
                }
            }, this::decRef));
        }
    }

    @Override
    protected void closeInternal() {
        assert recoveryMonitorBlocks.get() == 0;
        try {
            multiFileWriter.close();
        } finally {
            store.decRef();
            indexShard.recoveryStats().decCurrentAsTarget();
            closedLatch.countDown();
            releaseSnapshotFileDownloadsPermit();
        }
    }

    private void releaseSnapshotFileDownloadsPermit() {
        if (snapshotFileDownloadsPermit != null) {
            snapshotFileDownloadsPermit.close();
        }
    }

    @Override
    public String toString() {
        return shardId + " [" + recoveryId + "]";
    }

    private void ensureRefCount() {
        if (refCount() <= 0) {
            throw new ElasticsearchException(
                "RecoveryStatus is used but it's refcount is 0. Probably a mismatch between incRef/decRef " + "calls"
            );
        }
    }

    /*** Implementation of {@link RecoveryTargetHandler } */

    @Override
    public void prepareForTranslogOperations(int totalTranslogOps, ActionListener<Void> listener) {
        ActionListener.completeWith(listener, () -> {
            state().getIndex().setFileDetailsComplete(); 
            state().getTranslog().totalOperations(totalTranslogOps);
            indexShard().openEngineAndSkipTranslogRecovery();
            return null;
        });
    }

    @Override
    public void finalizeRecovery(final long globalCheckpoint, final long trimAboveSeqNo, ActionListener<Void> listener) {
        ActionListener.completeWith(listener, () -> {
            indexShard.updateGlobalCheckpointOnReplica(globalCheckpoint, "finalizing recovery");
            indexShard.sync();
            indexShard.persistRetentionLeases();
            if (trimAboveSeqNo != SequenceNumbers.UNASSIGNED_SEQ_NO) {
                indexShard.rollTranslogGeneration();
                indexShard.afterWriteOperation();
                indexShard.trimOperationOfPreviousPrimaryTerms(trimAboveSeqNo);
            }
            if (hasUncommittedOperations()) {
                indexShard.flush(new FlushRequest().force(true).waitIfOngoing(true));
            }
            indexShard.finalizeRecovery();
            return null;
        });
    }

    private boolean hasUncommittedOperations() throws IOException {
        long localCheckpointOfCommit = Long.parseLong(indexShard.commitStats().getUserData().get(SequenceNumbers.LOCAL_CHECKPOINT_KEY));
        return indexShard.countChanges("peer-recovery", localCheckpointOfCommit + 1, Long.MAX_VALUE) > 0;
    }

    @Override
    public void handoffPrimaryContext(final ReplicationTracker.PrimaryContext primaryContext, ActionListener<Void> listener) {
        ActionListener.completeWith(listener, () -> {
            indexShard.activateWithPrimaryContext(primaryContext);
            return null;
        });
    }

    @Override
    public void indexTranslogOperations(
        final List<Translog.Operation> operations,
        final int totalTranslogOps,
        final long maxSeenAutoIdTimestampOnPrimary,
        final long maxSeqNoOfDeletesOrUpdatesOnPrimary,
        final RetentionLeases retentionLeases,
        final long mappingVersionOnPrimary,
        final ActionListener<Long> listener
    ) {
        ActionListener.completeWith(listener, () -> {
            final RecoveryState.Translog translog = state().getTranslog();
            translog.totalOperations(totalTranslogOps);
            assert indexShard().recoveryState() == state();
            if (indexShard().state() != IndexShardState.RECOVERING) {
                throw new IndexShardNotRecoveringException(shardId, indexShard().state());
            }
            /*
             * The maxSeenAutoIdTimestampOnPrimary received from the primary is at least the highest auto_id_timestamp from any operation
             * will be replayed. Bootstrapping this timestamp here will disable the optimization for original append-only requests
             * (source of these operations) replicated via replication. Without this step, we may have duplicate documents if we
             * replay these operations first (without timestamp), then optimize append-only requests (with timestamp).
             */
            indexShard().updateMaxUnsafeAutoIdTimestamp(maxSeenAutoIdTimestampOnPrimary);
            /*
             * Bootstrap the max_seq_no_of_updates from the primary to make sure that the max_seq_no_of_updates on this replica when
             * replaying any of these operations will be at least the max_seq_no_of_updates on the primary when that op was executed on.
             */
            indexShard().advanceMaxSeqNoOfUpdatesOrDeletes(maxSeqNoOfDeletesOrUpdatesOnPrimary);
            /*
             * We have to update the retention leases before we start applying translog operations to ensure we are retaining according to
             * the policy.
             */
            indexShard().updateRetentionLeasesOnReplica(retentionLeases);
            for (Translog.Operation operation : operations) {
                Engine.Result result = indexShard().applyTranslogOperation(operation, Engine.Operation.Origin.PEER_RECOVERY);
                if (result.getResultType() == Engine.Result.Type.MAPPING_UPDATE_REQUIRED) {
                    throw new MapperException("mapping updates are not allowed [" + operation + "]");
                }
                if (result.getFailure() != null) {
                    if (Assertions.ENABLED && result.getFailure() instanceof MapperException == false) {
                        throw new AssertionError("unexpected failure while replicating translog entry", result.getFailure());
                    }
                    ExceptionsHelper.reThrowIfNotNull(result.getFailure());
                }
            }
            translog.incrementRecoveredOperations(operations.size());
            indexShard().sync();
            indexShard().afterWriteOperation();
            return indexShard().getLocalCheckpoint();
        });
    }

    @Override
    public void receiveFileInfo(
        List<String> phase1FileNames,
        List<Long> phase1FileSizes,
        List<String> phase1ExistingFileNames,
        List<Long> phase1ExistingFileSizes,
        int totalTranslogOps,
        ActionListener<Void> listener
    ) {
        ActionListener.completeWith(listener, () -> {
            indexShard.resetRecoveryStage();
            indexShard.prepareForIndexRecovery();
            recreateMultiFileWriter();
            final RecoveryState.Index index = state().getIndex();
            for (int i = 0; i < phase1ExistingFileNames.size(); i++) {
                index.addFileDetail(phase1ExistingFileNames.get(i), phase1ExistingFileSizes.get(i), true);
            }
            for (int i = 0; i < phase1FileNames.size(); i++) {
                index.addFileDetail(phase1FileNames.get(i), phase1FileSizes.get(i), false);
            }
            index.setFileDetailsComplete();
            state().getTranslog().totalOperations(totalTranslogOps);
            state().getTranslog().totalOperationsOnStart(totalTranslogOps);
            return null;
        });
    }

    @Override
    public void cleanFiles(
        int totalTranslogOps,
        long globalCheckpoint,
        Store.MetadataSnapshot sourceMetadata,
        ActionListener<Void> listener
    ) {
        ActionListener.completeWith(listener, () -> {
            state().getTranslog().totalOperations(totalTranslogOps);
            multiFileWriter.renameAllTempFiles();
            final Store store = store();
            store.incRef();
            try {
                if (indexShard.routingEntry().isPromotableToPrimary()) {
                    store.cleanupAndVerify("recovery CleanFilesRequestHandler", sourceMetadata);
                    final String translogUUID = Translog.createEmptyTranslog(
                        indexShard.shardPath().resolveTranslog(),
                        globalCheckpoint,
                        shardId,
                        indexShard.getPendingPrimaryTerm()
                    );
                    store.associateIndexWithNewTranslog(translogUUID);
                } else {
                    indexShard.setGlobalCheckpointIfUnpromotable(globalCheckpoint);
                }
                if (indexShard.getRetentionLeases().leases().isEmpty()) {
                    indexShard.persistRetentionLeases();
                    assert indexShard.loadRetentionLeases().leases().isEmpty();
                } else {
                    assert indexShard.assertRetentionLeasesPersisted();
                }
                indexShard.maybeCheckIndex();
                state().setRemoteTranslogStage();
            } catch (CorruptIndexException | IndexFormatTooNewException | IndexFormatTooOldException ex) {
                try {
                    try {
                        store.removeCorruptionMarker();
                    } finally {
                        Lucene.cleanLuceneIndex(store.directory()); 
                    }
                } catch (Exception e) {
                    logger.debug("Failed to clean lucene index", e);
                    ex.addSuppressed(e);
                }
                RecoveryFailedException rfe = new RecoveryFailedException(state(), "failed to clean after recovery", ex);
                fail(rfe, true);
                throw rfe;
            } catch (Exception ex) {
                RecoveryFailedException rfe = new RecoveryFailedException(state(), "failed to clean after recovery", ex);
                fail(rfe, true);
                throw rfe;
            } finally {
                store.decRef();
            }
            return null;
        });
    }

    @Override
    public void writeFileChunk(
        StoreFileMetadata fileMetadata,
        long position,
        ReleasableBytesReference content,
        boolean lastChunk,
        int totalTranslogOps,
        ActionListener<Void> listener
    ) {
        try {
            state().getTranslog().totalOperations(totalTranslogOps);
            multiFileWriter.writeFileChunk(fileMetadata, position, content, lastChunk);
            listener.onResponse(null);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    @Override
    public void restoreFileFromSnapshot(
        String repository,
        IndexId indexId,
        BlobStoreIndexShardSnapshot.FileInfo fileInfo,
        ActionListener<Void> listener
    ) {
        assert hasPermitToDownloadSnapshotFiles();

        try (
            InputStream inputStream = snapshotFilesProvider.getInputStreamForSnapshotFile(
                repository,
                indexId,
                shardId,
                fileInfo,
                this::registerThrottleTime
            )
        ) {
            StoreFileMetadata metadata = fileInfo.metadata();
            int readSnapshotFileBufferSize = snapshotFilesProvider.getReadSnapshotFileBufferSizeForRepo(repository);
            multiFileWriter.writeFile(metadata, readSnapshotFileBufferSize, new FilterInputStream(inputStream) {
                @Override
                public int read() throws IOException {
                    cancellableThreads.checkForCancel();
                    return super.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    cancellableThreads.checkForCancel();
                    return super.read(b, off, len);
                }
            });
            listener.onResponse(null);
        } catch (Exception e) {
            logger.debug(() -> format("Unable to recover snapshot file %s from repository %s", fileInfo, repository), e);
            listener.onFailure(e);
        }
    }

    private void registerThrottleTime(long throttleTimeInNanos) {
        state().getIndex().addTargetThrottling(throttleTimeInNanos);
        indexShard.recoveryStats().addThrottleTime(throttleTimeInNanos);
    }

    /** Get a temporary name for the provided file name. */
    public String getTempNameForFile(String origFile) {
        return multiFileWriter.getTempNameForFile(origFile);
    }

    Path translogLocation() {
        return indexShard().shardPath().resolveTranslog();
    }
}
