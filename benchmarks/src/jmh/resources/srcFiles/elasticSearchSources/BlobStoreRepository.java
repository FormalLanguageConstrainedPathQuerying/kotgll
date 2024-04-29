/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.repositories.blobstore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexFormatTooNewException;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RateLimiter;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SetOnce;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRunnable;
import org.elasticsearch.action.SingleResultDeduplicator;
import org.elasticsearch.action.support.GroupedActionListener;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.action.support.RefCountingListener;
import org.elasticsearch.action.support.RefCountingRunnable;
import org.elasticsearch.action.support.SubscribableListener;
import org.elasticsearch.action.support.ThreadedActionListener;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.cluster.RepositoryCleanupInProgress;
import org.elasticsearch.cluster.SnapshotDeletionsInProgress;
import org.elasticsearch.cluster.SnapshotsInProgress;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.metadata.RepositoriesMetadata;
import org.elasticsearch.cluster.metadata.RepositoryMetadata;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Numbers;
import org.elasticsearch.common.ReferenceDocs;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.blobstore.BlobContainer;
import org.elasticsearch.common.blobstore.BlobPath;
import org.elasticsearch.common.blobstore.BlobStore;
import org.elasticsearch.common.blobstore.DeleteResult;
import org.elasticsearch.common.blobstore.OperationPurpose;
import org.elasticsearch.common.blobstore.fs.FsBlobContainer;
import org.elasticsearch.common.blobstore.support.BlobMetadata;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Iterators;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.compress.NotXContentException;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.lucene.Lucene;
import org.elasticsearch.common.lucene.store.InputStreamIndexInput;
import org.elasticsearch.common.metrics.CounterMetric;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.util.concurrent.ListenableFuture;
import org.elasticsearch.common.util.concurrent.ThrottledIterator;
import org.elasticsearch.common.util.concurrent.ThrottledTaskRunner;
import org.elasticsearch.common.xcontent.ChunkedToXContent;
import org.elasticsearch.common.xcontent.LoggingDeprecationHandler;
import org.elasticsearch.core.AbstractRefCounted;
import org.elasticsearch.core.CheckedConsumer;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.RefCounted;
import org.elasticsearch.core.Releasable;
import org.elasticsearch.core.SuppressForbidden;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.index.IndexVersion;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.snapshots.IndexShardRestoreFailedException;
import org.elasticsearch.index.snapshots.IndexShardSnapshotFailedException;
import org.elasticsearch.index.snapshots.IndexShardSnapshotStatus;
import org.elasticsearch.index.snapshots.blobstore.BlobStoreIndexShardSnapshot;
import org.elasticsearch.index.snapshots.blobstore.BlobStoreIndexShardSnapshots;
import org.elasticsearch.index.snapshots.blobstore.RateLimitingInputStream;
import org.elasticsearch.index.snapshots.blobstore.SlicedInputStream;
import org.elasticsearch.index.snapshots.blobstore.SnapshotFiles;
import org.elasticsearch.index.store.Store;
import org.elasticsearch.index.store.StoreFileMetadata;
import org.elasticsearch.indices.recovery.RecoverySettings;
import org.elasticsearch.indices.recovery.RecoveryState;
import org.elasticsearch.repositories.FinalizeSnapshotContext;
import org.elasticsearch.repositories.IndexId;
import org.elasticsearch.repositories.IndexMetaDataGenerations;
import org.elasticsearch.repositories.RepositoriesService;
import org.elasticsearch.repositories.Repository;
import org.elasticsearch.repositories.RepositoryData;
import org.elasticsearch.repositories.RepositoryData.SnapshotDetails;
import org.elasticsearch.repositories.RepositoryException;
import org.elasticsearch.repositories.RepositoryOperation;
import org.elasticsearch.repositories.RepositoryShardId;
import org.elasticsearch.repositories.RepositoryStats;
import org.elasticsearch.repositories.RepositoryVerificationException;
import org.elasticsearch.repositories.ShardGeneration;
import org.elasticsearch.repositories.ShardGenerations;
import org.elasticsearch.repositories.ShardSnapshotResult;
import org.elasticsearch.repositories.SnapshotShardContext;
import org.elasticsearch.snapshots.AbortedSnapshotException;
import org.elasticsearch.snapshots.PausedSnapshotException;
import org.elasticsearch.snapshots.SnapshotDeleteListener;
import org.elasticsearch.snapshots.SnapshotException;
import org.elasticsearch.snapshots.SnapshotId;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.elasticsearch.snapshots.SnapshotMissingException;
import org.elasticsearch.snapshots.SnapshotsService;
import org.elasticsearch.tasks.TaskCancelledException;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.elasticsearch.core.Strings.format;
import static org.elasticsearch.index.snapshots.blobstore.BlobStoreIndexShardSnapshot.FileInfo;
import static org.elasticsearch.index.snapshots.blobstore.BlobStoreIndexShardSnapshot.FileInfo.canonicalName;
import static org.elasticsearch.indices.recovery.RecoverySettings.INDICES_RECOVERY_MAX_BYTES_PER_SEC_SETTING;

/**
 * BlobStore - based implementation of Snapshot Repository
 * <p>
 * This repository works with any {@link BlobStore} implementation. The blobStore could be (and is preferably) lazily initialized in
 * {@link #createBlobStore()}.
 * </p>
 * For in depth documentation on how exactly implementations of this class interact with the snapshot functionality please refer to the
 * documentation of the package {@link org.elasticsearch.repositories.blobstore}.
 */
public abstract class BlobStoreRepository extends AbstractLifecycleComponent implements Repository {
    private static final Logger logger = LogManager.getLogger(BlobStoreRepository.class);

    protected volatile RepositoryMetadata metadata;

    protected final ThreadPool threadPool;

    public static final String STATELESS_SHARD_READ_THREAD_NAME = "stateless_shard_read";
    public static final String STATELESS_TRANSLOG_THREAD_NAME = "stateless_translog";
    public static final String STATELESS_SHARD_WRITE_THREAD_NAME = "stateless_shard_write";
    public static final String STATELESS_CLUSTER_STATE_READ_WRITE_THREAD_NAME = "stateless_cluster_state";
    public static final String STATELESS_SHARD_PREWARMING_THREAD_NAME = "stateless_prewarm";

    public static final String SNAPSHOT_PREFIX = "snap-";

    public static final String INDEX_FILE_PREFIX = "index-";

    public static final String INDEX_LATEST_BLOB = "index.latest";

    private static final String TESTS_FILE = "tests-";

    public static final String METADATA_PREFIX = "meta-";

    public static final String METADATA_NAME_FORMAT = METADATA_PREFIX + "%s.dat";

    public static final String SNAPSHOT_NAME_FORMAT = SNAPSHOT_PREFIX + "%s.dat";

    private static final String SNAPSHOT_INDEX_PREFIX = "index-";

    public static final String SNAPSHOT_INDEX_NAME_FORMAT = SNAPSHOT_INDEX_PREFIX + "%s";

    public static final String UPLOADED_DATA_BLOB_PREFIX = "__";

    public static final String URL_REPOSITORY_TYPE = "url";

    /**
     * All {@link BlobStoreRepository} implementations can be made read-only by setting this key to {@code true} in their settings.
     */
    public static final String READONLY_SETTING_KEY = "readonly";

    /**
     * Prefix used for the identifiers of data blobs that were not actually written to the repository physically because their contents are
     * already stored in the metadata referencing them, i.e. in {@link BlobStoreIndexShardSnapshot} and
     * {@link BlobStoreIndexShardSnapshots}. This is the case for files for which {@link StoreFileMetadata#hashEqualsContents()} is
     * {@code true}.
     */
    private static final String VIRTUAL_DATA_BLOB_PREFIX = "v__";

    /**
     * When set to true metadata files are stored in compressed format. This setting doesn’t affect index
     * files that are already compressed by default. Changing the setting does not invalidate existing files since reads
     * do not observe the setting, instead they examine the file to see if it is compressed or not.
     */
    public static final Setting<Boolean> COMPRESS_SETTING = Setting.boolSetting("compress", true, Setting.Property.NodeScope);

    /**
     * Setting to disable caching of the latest repository data.
     */
    public static final Setting<Boolean> CACHE_REPOSITORY_DATA = Setting.boolSetting(
        "cache_repository_data",
        true,
        Setting.Property.DeprecatedWarning
    );

    /**
     * Size hint for the IO buffer size to use when reading from and writing to the repository.
     */
    public static final Setting<ByteSizeValue> BUFFER_SIZE_SETTING = Setting.byteSizeSetting(
        "io_buffer_size",
        ByteSizeValue.parseBytesSizeValue("128kb", "io_buffer_size"),
        ByteSizeValue.parseBytesSizeValue("8kb", "buffer_size"),
        ByteSizeValue.parseBytesSizeValue("16mb", "io_buffer_size"),
        Setting.Property.NodeScope
    );

    /**
     * Setting to disable writing the {@code index.latest} blob which enables the contents of this repository to be used with a
     * url-repository.
     */
    public static final Setting<Boolean> SUPPORT_URL_REPO = Setting.boolSetting("support_url_repo", true, Setting.Property.NodeScope);

    /**
     * Setting that defines the maximum number of snapshots to which the repository may grow. Trying to create a snapshot into the
     * repository that would move it above this size will throw an exception.
     */
    public static final Setting<Integer> MAX_SNAPSHOTS_SETTING = Setting.intSetting(
        "max_number_of_snapshots",
        Integer.MAX_VALUE,
        1,
        Setting.Property.NodeScope
    );

    /**
     * Setting that defines if the repository should be used to recover index files during peer recoveries.
     */
    public static final Setting<Boolean> USE_FOR_PEER_RECOVERY_SETTING = Setting.boolSetting("use_for_peer_recovery", false);

    protected final boolean supportURLRepo;

    private final boolean compress;

    private final boolean cacheRepositoryData;

    private volatile RateLimiter snapshotRateLimiter;

    private volatile RateLimiter restoreRateLimiter;

    private final CounterMetric snapshotRateLimitingTimeInNanos = new CounterMetric();

    private final CounterMetric restoreRateLimitingTimeInNanos = new CounterMetric();

    public static final ChecksumBlobStoreFormat<Metadata> GLOBAL_METADATA_FORMAT = new ChecksumBlobStoreFormat<>(
        "metadata",
        METADATA_NAME_FORMAT,
        (repoName, parser) -> Metadata.fromXContent(parser),
        ChunkedToXContent::wrapAsToXContent
    );

    public static final ChecksumBlobStoreFormat<IndexMetadata> INDEX_METADATA_FORMAT = new ChecksumBlobStoreFormat<>(
        "index-metadata",
        METADATA_NAME_FORMAT,
        (repoName, parser) -> IndexMetadata.Builder.legacyFromXContent(parser),
        (repoName, parser) -> IndexMetadata.fromXContent(parser),
        Function.identity()
    );

    private static final String SNAPSHOT_CODEC = "snapshot";

    public static final ChecksumBlobStoreFormat<SnapshotInfo> SNAPSHOT_FORMAT = new ChecksumBlobStoreFormat<>(
        SNAPSHOT_CODEC,
        SNAPSHOT_NAME_FORMAT,
        SnapshotInfo::fromXContentInternal,
        Function.identity()
    );

    public static final ChecksumBlobStoreFormat<BlobStoreIndexShardSnapshot> INDEX_SHARD_SNAPSHOT_FORMAT = new ChecksumBlobStoreFormat<>(
        SNAPSHOT_CODEC,
        SNAPSHOT_NAME_FORMAT,
        (repoName, parser) -> BlobStoreIndexShardSnapshot.fromXContent(parser),
        Function.identity()
    );

    public static final ChecksumBlobStoreFormat<BlobStoreIndexShardSnapshots> INDEX_SHARD_SNAPSHOTS_FORMAT = new ChecksumBlobStoreFormat<>(
        "snapshots",
        SNAPSHOT_INDEX_NAME_FORMAT,
        (repoName, parser) -> BlobStoreIndexShardSnapshots.fromXContent(parser),
        Function.identity()
    );

    public static final Setting<ByteSizeValue> MAX_SNAPSHOT_BYTES_PER_SEC = Setting.byteSizeSetting(
        "max_snapshot_bytes_per_sec",
        ByteSizeValue.ofMb(40), 
        Setting.Property.Dynamic,
        Setting.Property.NodeScope
    );

    public static final Setting<ByteSizeValue> MAX_RESTORE_BYTES_PER_SEC = Setting.byteSizeSetting(
        "max_restore_bytes_per_sec",
        ByteSizeValue.ZERO,
        Setting.Property.Dynamic,
        Setting.Property.NodeScope
    );

    /**
     * Repository settings that can be updated dynamically without having to create a new repository.
     */
    private static final Set<String> DYNAMIC_SETTING_NAMES = Set.of(
        MAX_SNAPSHOT_BYTES_PER_SEC.getKey(),
        MAX_RESTORE_BYTES_PER_SEC.getKey()
    );

    private final boolean readOnly;

    private final Object lock = new Object();

    private final SetOnce<BlobContainer> blobContainer = new SetOnce<>();

    private final SetOnce<BlobStore> blobStore = new SetOnce<>();

    private final BlobPath basePath;

    private final ClusterService clusterService;

    private final RecoverySettings recoverySettings;

    private final NamedXContentRegistry namedXContentRegistry;

    protected final BigArrays bigArrays;

    /**
     * Flag that is set to {@code true} if this instance is started with {@link #metadata} that has a higher value for
     * {@link RepositoryMetadata#pendingGeneration()} than for {@link RepositoryMetadata#generation()} indicating a full cluster restart
     * potentially accounting for the the last {@code index-N} write in the cluster state.
     * Note: While it is true that this value could also be set to {@code true} for an instance on a node that is just joining the cluster
     * during a new {@code index-N} write, this does not present a problem. The node will still load the correct {@link RepositoryData} in
     * all cases and simply do a redundant listing of the repository contents if it tries to load {@link RepositoryData} and falls back
     * to {@link #latestIndexBlobId()} to validate the value of {@link RepositoryMetadata#generation()}.
     */
    private boolean uncleanStart;

    /**
     * This flag indicates that the repository can not exclusively rely on the value stored in {@link #latestKnownRepoGen} to determine the
     * latest repository generation but must inspect its physical contents as well via {@link #latestIndexBlobId()}.
     * This flag is set in the following situations:
     * <ul>
     *     <li>All repositories that are read-only, i.e. for which {@link #isReadOnly()} returns {@code true} because there are no
     *     guarantees that another cluster is not writing to the repository at the same time</li>
     *     <li>The value of {@link RepositoryMetadata#generation()} for this repository is {@link RepositoryData#UNKNOWN_REPO_GEN}
     *     indicating that no consistent repository generation is tracked in the cluster state yet.</li>
     *     <li>The {@link #uncleanStart} flag is set to {@code true}</li>
     * </ul>
     */
    private volatile boolean bestEffortConsistency;

    /**
     * IO buffer size hint for reading and writing to the underlying blob store.
     */
    protected final int bufferSize;

    /**
     * Maximum number of snapshots that this repository can hold.
     */
    private final int maxSnapshotCount;

    private final ShardSnapshotTaskRunner shardSnapshotTaskRunner;

    private final ThrottledTaskRunner staleBlobDeleteRunner;

    /**
     * Constructs new BlobStoreRepository
     * @param metadata   The metadata for this repository including name and settings
     * @param clusterService ClusterService
     */
    @SuppressWarnings("this-escape")
    protected BlobStoreRepository(
        final RepositoryMetadata metadata,
        final NamedXContentRegistry namedXContentRegistry,
        final ClusterService clusterService,
        final BigArrays bigArrays,
        final RecoverySettings recoverySettings,
        final BlobPath basePath
    ) {
        this.metadata = metadata;
        this.threadPool = clusterService.getClusterApplierService().threadPool();
        this.clusterService = clusterService;
        this.bigArrays = bigArrays;
        this.recoverySettings = recoverySettings;
        this.compress = COMPRESS_SETTING.get(metadata.settings());
        this.supportURLRepo = SUPPORT_URL_REPO.get(metadata.settings());
        snapshotRateLimiter = getSnapshotRateLimiter();
        restoreRateLimiter = getRestoreRateLimiter();
        readOnly = metadata.settings().getAsBoolean(READONLY_SETTING_KEY, false);
        cacheRepositoryData = CACHE_REPOSITORY_DATA.get(metadata.settings());
        bufferSize = Math.toIntExact(BUFFER_SIZE_SETTING.get(metadata.settings()).getBytes());
        this.namedXContentRegistry = namedXContentRegistry;
        this.basePath = basePath;
        this.maxSnapshotCount = MAX_SNAPSHOTS_SETTING.get(metadata.settings());
        this.repoDataLoadDeduplicator = new SingleResultDeduplicator<>(
            threadPool.getThreadContext(),
            listener -> threadPool.executor(ThreadPool.Names.SNAPSHOT_META)
                .execute(ActionRunnable.wrap(listener, this::doGetRepositoryData))
        );
        shardSnapshotTaskRunner = new ShardSnapshotTaskRunner(
            threadPool.info(ThreadPool.Names.SNAPSHOT).getMax(),
            threadPool.executor(ThreadPool.Names.SNAPSHOT),
            this::doSnapshotShard,
            this::snapshotFile
        );
        staleBlobDeleteRunner = new ThrottledTaskRunner(
            "cleanupStaleBlobs",
            threadPool.info(ThreadPool.Names.SNAPSHOT).getMax(),
            threadPool.executor(ThreadPool.Names.SNAPSHOT)
        );
    }

    @Override
    protected void doStart() {
        uncleanStart = metadata.pendingGeneration() > RepositoryData.EMPTY_REPO_GEN
            && metadata.generation() != metadata.pendingGeneration();
        ByteSizeValue chunkSize = chunkSize();
        if (chunkSize != null && chunkSize.getBytes() <= 0) {
            throw new IllegalArgumentException("the chunk size cannot be negative: [" + chunkSize + "]");
        }
    }

    @Override
    protected void doStop() {}

    @Override
    protected void doClose() {
        activityRefs.decRef();
        BlobStore store;
        synchronized (lock) {
            store = blobStore.get();
        }
        if (store != null) {
            try {
                store.close();
            } catch (Exception t) {
                logger.warn("cannot close blob store", t);
            }
        }
    }

    private final SubscribableListener<Void> closedAndIdleListeners = new SubscribableListener<>();

    private final RefCounted activityRefs = AbstractRefCounted.of(() -> closedAndIdleListeners.onResponse(null));

    @Override
    public void awaitIdle() {
        assert lifecycle.closed();
        PlainActionFuture.<Void, RuntimeException>get(closedAndIdleListeners::addListener);
    }

    @SuppressForbidden(reason = "legacy usage of unbatched task") 
    private void submitUnbatchedTask(@SuppressWarnings("SameParameterValue") String source, ClusterStateUpdateTask task) {
        clusterService.submitUnbatchedStateUpdateTask(source, task);
    }

    @Override
    public void cloneShardSnapshot(
        SnapshotId source,
        SnapshotId target,
        RepositoryShardId shardId,
        @Nullable ShardGeneration shardGeneration,
        ActionListener<ShardSnapshotResult> listener
    ) {
        if (isReadOnly()) {
            listener.onFailure(new RepositoryException(metadata.name(), "cannot clone shard snapshot on a readonly repository"));
            return;
        }
        final IndexId index = shardId.index();
        final int shardNum = shardId.shardId();
        final Executor executor = threadPool.executor(ThreadPool.Names.SNAPSHOT);
        executor.execute(ActionRunnable.supply(listener, () -> {
            final long startTime = threadPool.absoluteTimeInMillis();
            final BlobContainer shardContainer = shardContainer(index, shardNum);
            final BlobStoreIndexShardSnapshots existingSnapshots;
            final ShardGeneration newGen;
            final ShardGeneration existingShardGen;
            if (shardGeneration == null) {
                Tuple<BlobStoreIndexShardSnapshots, Long> tuple = buildBlobStoreIndexShardSnapshots(
                    shardContainer.listBlobsByPrefix(OperationPurpose.SNAPSHOT_METADATA, INDEX_FILE_PREFIX).keySet(),
                    shardContainer
                );
                existingShardGen = new ShardGeneration(tuple.v2());
                newGen = new ShardGeneration(tuple.v2() + 1);
                existingSnapshots = tuple.v1();
            } else {
                newGen = ShardGeneration.newGeneration();
                existingSnapshots = buildBlobStoreIndexShardSnapshots(Collections.emptySet(), shardContainer, shardGeneration).v1();
                existingShardGen = shardGeneration;
            }
            SnapshotFiles existingTargetFiles = null;
            SnapshotFiles sourceFiles = null;
            for (SnapshotFiles existingSnapshot : existingSnapshots) {
                final String snapshotName = existingSnapshot.snapshot();
                if (snapshotName.equals(target.getName())) {
                    existingTargetFiles = existingSnapshot;
                } else if (snapshotName.equals(source.getName())) {
                    sourceFiles = existingSnapshot;
                }
                if (sourceFiles != null && existingTargetFiles != null) {
                    break;
                }
            }
            if (sourceFiles == null) {
                throw new RepositoryException(
                    metadata.name(),
                    "Can't create clone of ["
                        + shardId
                        + "] for snapshot ["
                        + target
                        + "]. The source snapshot ["
                        + source
                        + "] was not found in the shard metadata."
                );
            }
            if (existingTargetFiles != null) {
                if (existingTargetFiles.isSame(sourceFiles)) {
                    return new ShardSnapshotResult(
                        existingShardGen,
                        ByteSizeValue.ofBytes(existingTargetFiles.totalSize()),
                        getSegmentInfoFileCount(existingTargetFiles.indexFiles())
                    );
                }
                throw new RepositoryException(
                    metadata.name(),
                    "Can't create clone of ["
                        + shardId
                        + "] for snapshot ["
                        + target
                        + "]. A snapshot by that name already exists for this shard."
                );
            }
            final BlobStoreIndexShardSnapshot sourceMeta = loadShardSnapshot(shardContainer, source);
            logger.trace("[{}] [{}] writing shard snapshot file for clone", shardId, target);
            INDEX_SHARD_SNAPSHOT_FORMAT.write(
                sourceMeta.asClone(target.getName(), startTime, threadPool.absoluteTimeInMillis() - startTime),
                shardContainer,
                target.getUUID(),
                compress
            );
            INDEX_SHARD_SNAPSHOTS_FORMAT.write(
                existingSnapshots.withClone(source.getName(), target.getName()),
                shardContainer,
                newGen.toBlobNamePart(),
                compress
            );
            return new ShardSnapshotResult(
                newGen,
                ByteSizeValue.ofBytes(sourceMeta.totalSize()),
                getSegmentInfoFileCount(sourceMeta.indexFiles())
            );
        }));
    }

    private static int getSegmentInfoFileCount(List<BlobStoreIndexShardSnapshot.FileInfo> indexFiles) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, indexFiles.stream().filter(fi -> fi.physicalName().endsWith(".si")).count()));
    }

    @Override
    public boolean canUpdateInPlace(Settings updatedSettings, Set<String> ignoredSettings) {
        final Settings current = metadata.settings();
        if (current.equals(updatedSettings)) {
            return true;
        }
        final Set<String> changedSettingNames = new HashSet<>(current.keySet());
        changedSettingNames.addAll(updatedSettings.keySet());
        changedSettingNames.removeAll(ignoredSettings);
        changedSettingNames.removeIf(setting -> Objects.equals(current.get(setting), updatedSettings.get(setting)));
        changedSettingNames.removeAll(DYNAMIC_SETTING_NAMES);
        return changedSettingNames.isEmpty();
    }

    @Override
    public void updateState(ClusterState state) {
        final Settings previousSettings = metadata.settings();
        metadata = getRepoMetadata(state);
        final Settings updatedSettings = metadata.settings();
        if (updatedSettings.equals(previousSettings) == false) {
            snapshotRateLimiter = getSnapshotRateLimiter();
            restoreRateLimiter = getRestoreRateLimiter();
        }

        uncleanStart = uncleanStart && metadata.generation() != metadata.pendingGeneration();
        final boolean wasBestEffortConsistency = bestEffortConsistency;
        bestEffortConsistency = uncleanStart || isReadOnly() || metadata.generation() == RepositoryData.UNKNOWN_REPO_GEN;
        if (isReadOnly()) {
            return;
        }
        if (bestEffortConsistency) {
            long bestGenerationFromCS = bestGeneration(SnapshotsInProgress.get(state).forRepo(this.metadata.name()));
            if (bestGenerationFromCS == RepositoryData.EMPTY_REPO_GEN) {
                bestGenerationFromCS = bestGeneration(SnapshotDeletionsInProgress.get(state).getEntries());
            }
            if (bestGenerationFromCS == RepositoryData.EMPTY_REPO_GEN) {
                bestGenerationFromCS = bestGeneration(RepositoryCleanupInProgress.get(state).entries());
            }
            final long finalBestGen = Math.max(bestGenerationFromCS, metadata.generation());
            latestKnownRepoGen.accumulateAndGet(finalBestGen, Math::max);
        } else {
            final long previousBest = latestKnownRepoGen.getAndSet(metadata.generation());
            if (previousBest != metadata.generation()) {
                assert wasBestEffortConsistency
                    || metadata.generation() == RepositoryData.CORRUPTED_REPO_GEN
                    || previousBest < metadata.generation()
                    : "Illegal move from repository generation [" + previousBest + "] to generation [" + metadata.generation() + "]";
                logger.debug("Updated repository generation from [{}] to [{}]", previousBest, metadata.generation());
            }
        }
    }

    private long bestGeneration(Collection<? extends RepositoryOperation> operations) {
        final String repoName = metadata.name();
        return operations.stream()
            .filter(e -> e.repository().equals(repoName))
            .mapToLong(RepositoryOperation::repositoryStateId)
            .max()
            .orElse(RepositoryData.EMPTY_REPO_GEN);
    }

    public ThreadPool threadPool() {
        return threadPool;
    }

    BlobContainer getBlobContainer() {
        return blobContainer.get();
    }

    protected BlobStore getBlobStore() {
        return blobStore.get();
    }

    /**
     * maintains single lazy instance of {@link BlobContainer}
     */
    protected BlobContainer blobContainer() {
        assertSnapshotOrStatelessPermittedThreadPool();

        if (lifecycle.started() == false) {
            throw notStartedException();
        }

        BlobContainer blobContainer = this.blobContainer.get();
        if (blobContainer == null) {
            synchronized (lock) {
                blobContainer = this.blobContainer.get();
                if (blobContainer == null) {
                    blobContainer = blobStore().blobContainer(basePath());
                    this.blobContainer.set(blobContainer);
                }
            }
        }

        return blobContainer;
    }

    /**
     * Maintains single lazy instance of {@link BlobStore}.
     * Public for testing.
     */
    public BlobStore blobStore() {
        assertSnapshotOrStatelessPermittedThreadPool();

        BlobStore store = blobStore.get();
        if (store == null) {
            synchronized (lock) {
                store = blobStore.get();
                if (store == null) {
                    if (lifecycle.started() == false) {
                        throw notStartedException();
                    }
                    try {
                        store = createBlobStore();
                    } catch (RepositoryException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RepositoryException(metadata.name(), "cannot create blob store", e);
                    }
                    blobStore.set(store);
                }
            }
        }
        return store;
    }

    /**
     * Creates new BlobStore to read and write data.
     */
    protected abstract BlobStore createBlobStore() throws Exception;

    /**
     * Returns base path of the repository
     * Public for testing.
     */
    public BlobPath basePath() {
        return basePath;
    }

    /**
     * Returns true if metadata and snapshot files should be compressed
     *
     * @return true if compression is needed
     */
    protected final boolean isCompress() {
        return compress;
    }

    /**
     * Returns data file chunk size.
     * <p>
     * This method should return null if no chunking is needed.
     *
     * @return chunk size
     */
    protected ByteSizeValue chunkSize() {
        return null;
    }

    @Override
    public RepositoryMetadata getMetadata() {
        return metadata;
    }

    @Override
    public RepositoryStats stats() {
        final BlobStore store = blobStore.get();
        if (store == null) {
            return RepositoryStats.EMPTY_STATS;
        }
        return new RepositoryStats(store.stats());
    }

    /**
     * Loads {@link RepositoryData} ensuring that it is consistent with the given {@code rootBlobs} as well of the assumed generation.
     *
     * @param repositoryDataGeneration Expected repository generation
     * @param rootBlobs                Blobs at the repository root
     * @return RepositoryData
     */
    private RepositoryData safeRepositoryData(long repositoryDataGeneration, Map<String, BlobMetadata> rootBlobs) {
        final long generation = latestGeneration(rootBlobs.keySet());
        final long genToLoad;
        final RepositoryData cached;
        if (bestEffortConsistency) {
            genToLoad = latestKnownRepoGen.accumulateAndGet(repositoryDataGeneration, Math::max);
            cached = null;
        } else {
            genToLoad = latestKnownRepoGen.get();
            cached = latestKnownRepositoryData.get();
        }
        if (genToLoad > generation) {
            logger.debug(
                "Determined repository's generation from its contents to ["
                    + generation
                    + "] but "
                    + "current generation is at least ["
                    + genToLoad
                    + "]"
            );
        }
        if (genToLoad != repositoryDataGeneration) {
            throw new RepositoryException(
                metadata.name(),
                "concurrent modification of the index-N file, expected current generation ["
                    + repositoryDataGeneration
                    + "], actual current generation ["
                    + genToLoad
                    + "]"
            );
        }
        if (cached != null && cached.getGenId() == genToLoad) {
            return cached;
        }
        return getRepositoryData(genToLoad);
    }

    /**
     * Some repositories (i.e. S3) run at extra risk of corruption when using the pre-7.6.0 repository format, against which we try and
     * protect by adding some delays in between operations so that things have a chance to settle down. This method is the hook that allows
     * the delete process to add this protection when necessary.
     */
    protected SnapshotDeleteListener wrapWithWeakConsistencyProtection(SnapshotDeleteListener snapshotDeleteListener) {
        return snapshotDeleteListener;
    }

    @Override
    public void deleteSnapshots(
        Collection<SnapshotId> snapshotIds,
        long repositoryDataGeneration,
        IndexVersion minimumNodeVersion,
        SnapshotDeleteListener listener
    ) {
        createSnapshotsDeletion(snapshotIds, repositoryDataGeneration, minimumNodeVersion, new ActionListener<>() {
            @Override
            public void onResponse(SnapshotsDeletion snapshotsDeletion) {
                snapshotsDeletion.runDelete(listener);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Runs cleanup actions on the repository. Increments the repository state id by one before executing any modifications on the
     * repository.
     * TODO: Add shard level cleanups
     * TODO: Add unreferenced index metadata cleanup
     * <ul>
     *     <li>Deleting stale indices</li>
     *     <li>Deleting unreferenced root level blobs</li>
     * </ul>
     *
     * @param repositoryDataGeneration         Generation of {@link RepositoryData} at start of process
     * @param repositoryFormatIndexVersion     Repository format version
     * @param listener                         Listener to complete when done
     */
    public void cleanup(long repositoryDataGeneration, IndexVersion repositoryFormatIndexVersion, ActionListener<DeleteResult> listener) {
        createSnapshotsDeletion(
            List.of(),
            repositoryDataGeneration,
            repositoryFormatIndexVersion,
            listener.delegateFailureAndWrap((delegate, snapshotsDeletion) -> snapshotsDeletion.runCleanup(delegate))
        );
    }

    private void createSnapshotsDeletion(
        Collection<SnapshotId> snapshotIds,
        long repositoryDataGeneration,
        IndexVersion minimumNodeVersion,
        ActionListener<SnapshotsDeletion> listener
    ) {
        if (isReadOnly()) {
            listener.onFailure(new RepositoryException(metadata.name(), "repository is readonly"));
        } else {
            threadPool.executor(ThreadPool.Names.SNAPSHOT).execute(ActionRunnable.supply(listener, () -> {
                final var originalRootBlobs = blobContainer().listBlobs(OperationPurpose.SNAPSHOT_METADATA);

                final var originalRepositoryData = safeRepositoryData(repositoryDataGeneration, originalRootBlobs);

                return new SnapshotsDeletion(
                    snapshotIds,
                    repositoryDataGeneration,
                    SnapshotsService.minCompatibleVersion(minimumNodeVersion, originalRepositoryData, snapshotIds),
                    originalRootBlobs,
                    blobStore().blobContainer(indicesPath()).children(OperationPurpose.SNAPSHOT_DATA),
                    originalRepositoryData
                );
            }));
        }
    }

    /**
     * <p>
     *     Represents the process of deleting some collection of snapshots within this repository which since 7.6.0 looks like this:
     * </p>
     * <ul>
     *     <li>Write a new {@link BlobStoreIndexShardSnapshots} for each affected shard, and compute the blobs to delete.</li>
     *     <li>Update the {@link RepositoryData} to remove references to deleted snapshots/indices and point to the new
     *     {@link BlobStoreIndexShardSnapshots} files.</li>
     *     <li>Remove up any now-unreferenced blobs.</li>
     * </ul>
     * <p>
     *     Until the {@link RepositoryData} is updated there should be no other activities in the repository, and in particular the root
     *     blob must not change until it is updated by this deletion and {@link SnapshotDeleteListener#onRepositoryDataWritten} is called.
     * </p>
     */
    class SnapshotsDeletion {

        /**
         * The IDs of the snapshots to delete. This collection is empty if the deletion is a repository cleanup.
         */
        private final Collection<SnapshotId> snapshotIds;

        /**
         * The {@link RepositoryData} generation at the start of the process, to ensure that the {@link RepositoryData} does not change
         * while the new {@link BlobStoreIndexShardSnapshots} are being written.
         */
        private final long originalRepositoryDataGeneration;

        /**
         * The minimum {@link IndexVersion} of the nodes in the cluster and the snapshots remaining in the repository. The repository must
         * remain readable by all node versions which support this {@link IndexVersion}.
         */
        private final IndexVersion repositoryFormatIndexVersion;

        /**
         * Whether the {@link #repositoryFormatIndexVersion} is new enough to support naming {@link BlobStoreIndexShardSnapshots} blobs with
         * UUIDs (i.e. does not need to remain compatible with versions before v7.6.0). Older repositories use (unsafe) numeric indices for
         * these blobs instead.
         */
        private final boolean useShardGenerations;

        /**
         * All blobs in the repository root at the start of the operation, obtained by listing the repository contents. Note that this may
         * include some blobs which are no longer referenced by the current {@link RepositoryData}, but which have not yet been removed by
         * the cleanup that follows an earlier deletion. This cleanup may still be ongoing (we do not wait for it to complete before
         * starting the next repository operation) or it may have failed before completion (it could have been running on a different node,
         * which crashed for unrelated reasons) so we track all the blobs here and clean them up again at the end.
         */
        private final Map<String, BlobMetadata> originalRootBlobs;

        /**
         * All index containers at the start of the operation, obtained by listing the repository contents. Note that this may include some
         * containers which are no longer referenced by the current {@link RepositoryData}, but which have not yet been removed by
         * the cleanup that follows an earlier deletion. This cleanup may or may not still be ongoing (it could have been running on a
         * different node, which died before completing it) so we track all the blobs here and clean them up again at the end.
         */
        private final Map<String, BlobContainer> originalIndexContainers;

        /**
         * The {@link RepositoryData} at the start of the operation, obtained after verifying that {@link #originalRootBlobs} contains no
         * {@link RepositoryData} blob newer than the one identified by {@link #originalRepositoryDataGeneration}.
         */
        private final RepositoryData originalRepositoryData;

        /**
         * Executor to use for all repository interactions.
         */
        private final Executor snapshotExecutor = threadPool.executor(ThreadPool.Names.SNAPSHOT);

        /**
         * Accumulates the number of blobs deleted by this operation.
         */
        private final AtomicLong blobsDeleted = new AtomicLong();

        /**
         * Accumulates the total size of the blobs deleted by this operation.
         */
        private final AtomicLong bytesDeleted = new AtomicLong();

        SnapshotsDeletion(
            Collection<SnapshotId> snapshotIds,
            long originalRepositoryDataGeneration,
            IndexVersion repositoryFormatIndexVersion,
            Map<String, BlobMetadata> originalRootBlobs,
            Map<String, BlobContainer> originalIndexContainers,
            RepositoryData originalRepositoryData
        ) {
            this.snapshotIds = snapshotIds;
            this.originalRepositoryDataGeneration = originalRepositoryDataGeneration;
            this.repositoryFormatIndexVersion = repositoryFormatIndexVersion;
            this.useShardGenerations = SnapshotsService.useShardGenerations(repositoryFormatIndexVersion);
            this.originalRootBlobs = originalRootBlobs;
            this.originalIndexContainers = originalIndexContainers;
            this.originalRepositoryData = originalRepositoryData;
        }

        /**
         * The result of removing a snapshot from a shard folder in the repository.
         *
         * @param indexId       Index that the snapshot was removed from
         * @param shardId       Shard id that the snapshot was removed from
         * @param newGeneration Id of the new index-${uuid} blob that does not include the snapshot any more
         * @param blobsToDelete Blob names in the shard directory that have become unreferenced in the new shard generation
         */
        private record ShardSnapshotMetaDeleteResult(
            IndexId indexId,
            int shardId,
            ShardGeneration newGeneration,
            Collection<String> blobsToDelete
        ) {}

        /**
         * <p>
         *     Shard-level results, see {@link ShardSnapshotMetaDeleteResult}.
         * </p>
         * <p>
         *     Writes to this list are all synchronized (via {@link #addShardDeleteResult}), and happen-before it is read so the reads need
         *     no further synchronization
         * </p>
         */
        private final List<ShardSnapshotMetaDeleteResult> shardDeleteResults = new ArrayList<>();

        private synchronized void addShardDeleteResult(ShardSnapshotMetaDeleteResult shardDeleteResult) {
            shardDeleteResults.add(shardDeleteResult);
        }


        void runDelete(SnapshotDeleteListener listener) {
            if (useShardGenerations) {
                runWithUniqueShardMetadataNaming(listener);
            } else {
                runWithLegacyNumericShardMetadataNaming(wrapWithWeakConsistencyProtection(listener));
            }
        }

        private void runWithUniqueShardMetadataNaming(SnapshotDeleteListener listener) {
            SubscribableListener

                .newForked(this::writeUpdatedShardMetadataAndComputeDeletes)

                .<RepositoryData>andThen((l, ignored) -> {
                    final ShardGenerations.Builder builder = ShardGenerations.builder();
                    for (ShardSnapshotMetaDeleteResult newGen : shardDeleteResults) {
                        builder.put(newGen.indexId, newGen.shardId, newGen.newGeneration);
                    }
                    updateRepositoryData(originalRepositoryData.removeSnapshots(snapshotIds, builder.build()), l);
                })

                .addListener(
                    ActionListener.wrap(
                        newRepositoryData -> {
                            listener.onRepositoryDataWritten(newRepositoryData);
                            try (var refs = new RefCountingRunnable(listener::onDone)) {
                                cleanupUnlinkedRootAndIndicesBlobs(newRepositoryData, refs.acquireListener());
                                cleanupUnlinkedShardLevelBlobs(shardDeleteResults, refs.acquireListener());
                            }
                        },
                        listener::onFailure
                    )
                );
        }

        private void runWithLegacyNumericShardMetadataNaming(SnapshotDeleteListener listener) {
            updateRepositoryData(
                originalRepositoryData.removeSnapshots(snapshotIds, ShardGenerations.EMPTY),
                ActionListener.wrap(newRepositoryData -> {
                    try (var refs = new RefCountingRunnable(() -> {
                        listener.onRepositoryDataWritten(newRepositoryData);
                        listener.onDone();
                    })) {
                        cleanupUnlinkedRootAndIndicesBlobs(newRepositoryData, refs.acquireListener());

                        snapshotExecutor.execute(
                            ActionRunnable.wrap(
                                refs.acquireListener(),
                                l0 -> writeUpdatedShardMetadataAndComputeDeletes(
                                    l0.delegateFailure((l, ignored) -> cleanupUnlinkedShardLevelBlobs(shardDeleteResults, l))
                                )
                            )
                        );
                    }
                }, listener::onFailure)
            );
        }

        void runCleanup(ActionListener<DeleteResult> listener) {
            final Set<String> survivingIndexIds = originalRepositoryData.getIndices()
                .values()
                .stream()
                .map(IndexId::getId)
                .collect(Collectors.toSet());
            final List<String> staleRootBlobs = staleRootBlobs(originalRepositoryData, originalRootBlobs.keySet());
            if (survivingIndexIds.equals(originalIndexContainers.keySet()) && staleRootBlobs.isEmpty()) {
                listener.onResponse(DeleteResult.ZERO);
            } else {
                updateRepositoryData(
                    originalRepositoryData,
                    listener.delegateFailureAndWrap(
                        (l, newRepositoryData) -> cleanupUnlinkedRootAndIndicesBlobs(
                            originalRepositoryData,
                            l.map(ignored -> DeleteResult.of(blobsDeleted.get(), bytesDeleted.get()))
                        )
                    )
                );
            }
        }


        private void writeUpdatedShardMetadataAndComputeDeletes(ActionListener<Void> listener) {
            final var listeners = new RefCountingListener(listener);

            ThrottledIterator.run(
                originalRepositoryData.indicesToUpdateAfterRemovingSnapshot(snapshotIds),
                (ref, indexId) -> ActionListener.run(
                    ActionListener.releaseAfter(listeners.acquire(), ref),
                    l -> new IndexSnapshotsDeletion(indexId).run(l)
                ),
                threadPool.info(ThreadPool.Names.SNAPSHOT).getMax(),
                () -> {},
                listeners::close
            );
        }

        private class IndexSnapshotsDeletion {
            private final IndexId indexId;
            private final Set<SnapshotId> snapshotsWithIndex;
            private final BlobContainer indexContainer;

            IndexSnapshotsDeletion(IndexId indexId) {
                this.indexId = indexId;
                this.indexContainer = indexContainer(indexId);
                this.snapshotsWithIndex = Set.copyOf(originalRepositoryData.getSnapshots(indexId));
            }

            private int shardCount;

            private synchronized void updateShardCount(int newShardCount) {
                shardCount = Math.max(shardCount, newShardCount);
            }

            void run(ActionListener<Void> listener) {
                determineShardCount(listener.delegateFailureAndWrap((l, v) -> processShards(l)));
            }


            private void determineShardCount(ActionListener<Void> listener) {
                try (var listeners = new RefCountingListener(listener)) {
                    for (final var indexMetaGeneration : snapshotIds.stream()
                        .filter(snapshotsWithIndex::contains)
                        .map(id -> originalRepositoryData.indexMetaDataGenerations().indexMetaBlobId(id, indexId))
                        .collect(Collectors.toSet())) {
                        snapshotExecutor.execute(ActionRunnable.run(listeners.acquire(), () -> getOneShardCount(indexMetaGeneration)));
                    }
                }
            }

            private void getOneShardCount(String indexMetaGeneration) {
                try {
                    updateShardCount(
                        INDEX_METADATA_FORMAT.read(metadata.name(), indexContainer, indexMetaGeneration, namedXContentRegistry)
                            .getNumberOfShards()
                    );
                } catch (Exception ex) {
                    logger.warn(() -> format("[%s] [%s] failed to read metadata for index", indexMetaGeneration, indexId.getName()), ex);
                }
            }

            private void processShards(ActionListener<Void> listener) {
                final Set<SnapshotId> survivingSnapshots = snapshotsWithIndex.stream()
                    .filter(id -> snapshotIds.contains(id) == false)
                    .collect(Collectors.toSet());
                try (var listeners = new RefCountingListener(listener)) {
                    for (int shardId = 0; shardId < shardCount; shardId++) {
                        snapshotExecutor.execute(new ShardSnapshotsDeletion(shardId, survivingSnapshots, listeners.acquire()));
                    }
                }
            }


            private class ShardSnapshotsDeletion extends AbstractRunnable {

                private final int shardId;
                private final Set<SnapshotId> survivingSnapshots;
                private final ActionListener<Void> listener;

                private BlobContainer shardContainer;
                private Set<String> originalShardBlobs;

                ShardSnapshotsDeletion(int shardId, Set<SnapshotId> survivingSnapshots, ActionListener<Void> listener) {
                    this.shardId = shardId;
                    this.survivingSnapshots = survivingSnapshots;
                    this.listener = listener;
                }

                @Override
                protected void doRun() throws Exception {
                    shardContainer = shardContainer(indexId, shardId);
                    originalShardBlobs = shardContainer.listBlobs(OperationPurpose.SNAPSHOT_DATA).keySet();
                    final BlobStoreIndexShardSnapshots blobStoreIndexShardSnapshots;
                    final long newGen;
                    if (useShardGenerations) {
                        newGen = -1L;
                        blobStoreIndexShardSnapshots = buildBlobStoreIndexShardSnapshots(
                            originalShardBlobs,
                            shardContainer,
                            originalRepositoryData.shardGenerations().getShardGen(indexId, shardId)
                        ).v1();
                    } else {
                        Tuple<BlobStoreIndexShardSnapshots, Long> tuple = buildBlobStoreIndexShardSnapshots(
                            originalShardBlobs,
                            shardContainer
                        );
                        newGen = tuple.v2() + 1;
                        blobStoreIndexShardSnapshots = tuple.v1();
                    }
                    addShardDeleteResult(
                        deleteFromShardSnapshotMeta(blobStoreIndexShardSnapshots.withRetainedSnapshots(survivingSnapshots), newGen)
                    );
                }

                /**
                 * Delete snapshot from shard level metadata.
                 *
                 * @param indexGeneration generation to write the new shard level level metadata to. If negative a uuid id shard generation
                 *                        should be used
                 */
                private ShardSnapshotMetaDeleteResult deleteFromShardSnapshotMeta(
                    BlobStoreIndexShardSnapshots updatedSnapshots,
                    long indexGeneration
                ) {
                    ShardGeneration writtenGeneration = null;
                    try {
                        if (updatedSnapshots.snapshots().isEmpty()) {
                            return new ShardSnapshotMetaDeleteResult(
                                indexId,
                                shardId,
                                ShardGenerations.DELETED_SHARD_GEN,
                                originalShardBlobs
                            );
                        } else {
                            if (indexGeneration < 0L) {
                                writtenGeneration = ShardGeneration.newGeneration();
                                INDEX_SHARD_SNAPSHOTS_FORMAT.write(
                                    updatedSnapshots,
                                    shardContainer,
                                    writtenGeneration.toBlobNamePart(),
                                    compress
                                );
                            } else {
                                writtenGeneration = new ShardGeneration(indexGeneration);
                                writeShardIndexBlobAtomic(shardContainer, indexGeneration, updatedSnapshots, Collections.emptyMap());
                            }
                            final Set<String> survivingSnapshotUUIDs = survivingSnapshots.stream()
                                .map(SnapshotId::getUUID)
                                .collect(Collectors.toSet());
                            return new ShardSnapshotMetaDeleteResult(
                                indexId,
                                shardId,
                                writtenGeneration,
                                unusedBlobs(originalShardBlobs, survivingSnapshotUUIDs, updatedSnapshots)
                            );
                        }
                    } catch (IOException e) {
                        throw new RepositoryException(
                            metadata.name(),
                            "Failed to finalize snapshot deletion "
                                + snapshotIds
                                + " with shard index ["
                                + INDEX_SHARD_SNAPSHOTS_FORMAT.blobName(writtenGeneration.toBlobNamePart())
                                + "]",
                            e
                        );
                    }
                }

                private static List<String> unusedBlobs(
                    Set<String> originalShardBlobs,
                    Set<String> survivingSnapshotUUIDs,
                    BlobStoreIndexShardSnapshots updatedSnapshots
                ) {
                    return originalShardBlobs.stream()
                        .filter(
                            blob -> blob.startsWith(SNAPSHOT_INDEX_PREFIX)
                                || (blob.startsWith(SNAPSHOT_PREFIX)
                                    && blob.endsWith(".dat")
                                    && survivingSnapshotUUIDs.contains(
                                        blob.substring(SNAPSHOT_PREFIX.length(), blob.length() - ".dat".length())
                                    ) == false)
                                || (blob.startsWith(UPLOADED_DATA_BLOB_PREFIX)
                                    && updatedSnapshots.findNameFile(canonicalName(blob)) == null)
                                || FsBlobContainer.isTempBlobName(blob)
                        )
                        .toList();
                }

                @Override
                public void onFailure(Exception ex) {
                    logger.warn(
                        () -> format("%s failed to delete shard data for shard [%s][%s]", snapshotIds, indexId.getName(), shardId),
                        ex
                    );
                }

                @Override
                public void onAfter() {
                    listener.onResponse(null);
                }
            }
        }


        private void updateRepositoryData(RepositoryData newRepositoryData, ActionListener<RepositoryData> listener) {
            writeIndexGen(newRepositoryData, originalRepositoryDataGeneration, repositoryFormatIndexVersion, Function.identity(), listener);
        }


        private void cleanupUnlinkedShardLevelBlobs(
            Collection<ShardSnapshotMetaDeleteResult> shardDeleteResults,
            ActionListener<Void> listener
        ) {
            final Iterator<String> filesToDelete = resolveFilesToDelete(shardDeleteResults);
            if (filesToDelete.hasNext() == false) {
                listener.onResponse(null);
                return;
            }
            snapshotExecutor.execute(ActionRunnable.wrap(listener, l -> {
                try {
                    deleteFromContainer(OperationPurpose.SNAPSHOT_DATA, blobContainer(), filesToDelete);
                    l.onResponse(null);
                } catch (Exception e) {
                    logger.warn(() -> format("%s Failed to delete some blobs during snapshot delete", snapshotIds), e);
                    throw e;
                }
            }));
        }

        private Iterator<String> resolveFilesToDelete(Collection<ShardSnapshotMetaDeleteResult> deleteResults) {
            final String basePath = basePath().buildAsString();
            final int basePathLen = basePath.length();
            return Stream.concat(
                deleteResults.stream().flatMap(shardResult -> {
                    final String shardPath = shardPath(shardResult.indexId, shardResult.shardId).buildAsString();
                    return shardResult.blobsToDelete.stream().map(blob -> shardPath + blob);
                }),
                originalRepositoryData.indexMetaDataToRemoveAfterRemovingSnapshots(snapshotIds).entrySet().stream().flatMap(entry -> {
                    final String indexContainerPath = indexPath(entry.getKey()).buildAsString();
                    return entry.getValue().stream().map(id -> indexContainerPath + INDEX_METADATA_FORMAT.blobName(id));
                })
            ).map(absolutePath -> {
                assert absolutePath.startsWith(basePath);
                return absolutePath.substring(basePathLen);
            }).iterator();
        }

        /**
         * Cleans up stale blobs directly under the repository root as well as all indices paths that aren't referenced by any existing
         * snapshots. This method is only to be called directly after a new {@link RepositoryData} was written to the repository.
         *
         * @param newRepositoryData       new repository data that was just written
         * @param listener                listener to invoke when the deletion is complete
         */
        private void cleanupUnlinkedRootAndIndicesBlobs(RepositoryData newRepositoryData, ActionListener<Void> listener) {
            try (var listeners = new RefCountingListener(listener)) {
                final List<String> staleRootBlobs = staleRootBlobs(newRepositoryData, originalRootBlobs.keySet());
                if (staleRootBlobs.isEmpty() == false) {
                    staleBlobDeleteRunner.enqueueTask(listeners.acquire(ref -> {
                        try (ref) {
                            logStaleRootLevelBlobs(newRepositoryData.getGenId() - 1, snapshotIds, staleRootBlobs);
                            deleteFromContainer(OperationPurpose.SNAPSHOT_METADATA, blobContainer(), staleRootBlobs.iterator());
                            for (final var staleRootBlob : staleRootBlobs) {
                                bytesDeleted.addAndGet(originalRootBlobs.get(staleRootBlob).length());
                            }
                            blobsDeleted.addAndGet(staleRootBlobs.size());
                        } catch (Exception e) {
                            logger.warn(
                                () -> format(
                                    "[%s] The following blobs are no longer part of any snapshot [%s] but failed to remove them",
                                    metadata.name(),
                                    staleRootBlobs
                                ),
                                e
                            );
                        }
                    }));
                }

                final var survivingIndexIds = newRepositoryData.getIndices()
                    .values()
                    .stream()
                    .map(IndexId::getId)
                    .collect(Collectors.toSet());
                for (final var indexEntry : originalIndexContainers.entrySet()) {
                    final var indexId = indexEntry.getKey();
                    if (survivingIndexIds.contains(indexId)) {
                        continue;
                    }
                    staleBlobDeleteRunner.enqueueTask(listeners.acquire(ref -> {
                        try (ref) {
                            logger.debug("[{}] Found stale index [{}]. Cleaning it up", metadata.name(), indexId);
                            final var deleteResult = indexEntry.getValue().delete(OperationPurpose.SNAPSHOT_DATA);
                            blobsDeleted.addAndGet(deleteResult.blobsDeleted());
                            bytesDeleted.addAndGet(deleteResult.bytesDeleted());
                            logger.debug("[{}] Cleaned up stale index [{}]", metadata.name(), indexId);
                        } catch (IOException e) {
                            logger.warn(() -> format("""
                                [%s] index %s is no longer part of any snapshot in the repository, \
                                but failed to clean up its index folder""", metadata.name(), indexId), e);
                        }
                    }));
                }
            }


            staleBlobDeleteRunner.runSyncTasksEagerly(snapshotExecutor);
        }

        private static List<String> staleRootBlobs(RepositoryData newRepositoryData, Set<String> originalRootBlobNames) {
            final Set<String> allSnapshotIds = newRepositoryData.getSnapshotIds()
                .stream()
                .map(SnapshotId::getUUID)
                .collect(Collectors.toSet());
            return originalRootBlobNames.stream().filter(blob -> {
                if (FsBlobContainer.isTempBlobName(blob)) {
                    return true;
                }
                if (blob.endsWith(".dat")) {
                    final String foundUUID;
                    if (blob.startsWith(SNAPSHOT_PREFIX)) {
                        foundUUID = blob.substring(SNAPSHOT_PREFIX.length(), blob.length() - ".dat".length());
                        assert SNAPSHOT_FORMAT.blobName(foundUUID).equals(blob);
                    } else if (blob.startsWith(METADATA_PREFIX)) {
                        foundUUID = blob.substring(METADATA_PREFIX.length(), blob.length() - ".dat".length());
                        assert GLOBAL_METADATA_FORMAT.blobName(foundUUID).equals(blob);
                    } else {
                        return false;
                    }
                    return allSnapshotIds.contains(foundUUID) == false;
                } else if (blob.startsWith(INDEX_FILE_PREFIX)) {
                    try {
                        return newRepositoryData.getGenId() > Long.parseLong(blob.substring(INDEX_FILE_PREFIX.length()));
                    } catch (NumberFormatException nfe) {
                        return false;
                    }
                }
                return false;
            }).toList();
        }

        private void logStaleRootLevelBlobs(
            long newestStaleRepositoryDataGeneration,
            Collection<SnapshotId> snapshotIds,
            List<String> blobsToDelete
        ) {
            if (logger.isInfoEnabled()) {
                final Set<String> blobNamesToIgnore = snapshotIds.stream()
                    .flatMap(
                        snapshotId -> Stream.of(
                            GLOBAL_METADATA_FORMAT.blobName(snapshotId.getUUID()),
                            SNAPSHOT_FORMAT.blobName(snapshotId.getUUID()),
                            INDEX_FILE_PREFIX + newestStaleRepositoryDataGeneration
                        )
                    )
                    .collect(Collectors.toSet());
                final List<String> blobsToLog = blobsToDelete.stream().filter(b -> blobNamesToIgnore.contains(b) == false).toList();
                if (blobsToLog.isEmpty() == false) {
                    logger.info("[{}] Found stale root level blobs {}. Cleaning them up", metadata.name(), blobsToLog);
                }
            }
        }
    }

    @Override
    public void finalizeSnapshot(final FinalizeSnapshotContext finalizeSnapshotContext) {
        final long repositoryStateId = finalizeSnapshotContext.repositoryStateId();
        final ShardGenerations shardGenerations = finalizeSnapshotContext.updatedShardGenerations();
        final SnapshotInfo snapshotInfo = finalizeSnapshotContext.snapshotInfo();
        assert repositoryStateId > RepositoryData.UNKNOWN_REPO_GEN
            : "Must finalize based on a valid repository generation but received [" + repositoryStateId + "]";
        final Collection<IndexId> indices = shardGenerations.indices();
        final SnapshotId snapshotId = snapshotInfo.snapshotId();
        final IndexVersion repositoryMetaVersion = finalizeSnapshotContext.repositoryMetaVersion();
        final boolean writeShardGens = SnapshotsService.useShardGenerations(repositoryMetaVersion);

        final Executor executor = threadPool.executor(ThreadPool.Names.SNAPSHOT);

        final boolean writeIndexGens = SnapshotsService.useIndexGenerations(repositoryMetaVersion);

        record MetadataWriteResult(
            RepositoryData existingRepositoryData,
            Map<IndexId, String> indexMetas,
            Map<String, String> indexMetaIdentifiers
        ) {}

        record RootBlobUpdateResult(RepositoryData oldRepositoryData, RepositoryData newRepositoryData) {}

        SubscribableListener

            .<RepositoryData>newForked(
                listener -> getRepositoryData(
                    EsExecutors.DIRECT_EXECUTOR_SERVICE,
                    listener
                )
            )

            .<MetadataWriteResult>andThen((l, existingRepositoryData) -> {
                final int existingSnapshotCount = existingRepositoryData.getSnapshotIds().size();
                if (existingSnapshotCount >= maxSnapshotCount) {
                    throw new RepositoryException(
                        metadata.name(),
                        "Cannot add another snapshot to this repository as it already contains ["
                            + existingSnapshotCount
                            + "] snapshots and is configured to hold up to ["
                            + maxSnapshotCount
                            + "] snapshots only."
                    );
                }

                final MetadataWriteResult metadataWriteResult;
                if (writeIndexGens) {
                    metadataWriteResult = new MetadataWriteResult(
                        existingRepositoryData,
                        ConcurrentCollections.newConcurrentMap(),
                        ConcurrentCollections.newConcurrentMap()
                    );
                } else {
                    metadataWriteResult = new MetadataWriteResult(existingRepositoryData, null, null);
                }

                try (var allMetaListeners = new RefCountingListener(l.map(ignored -> metadataWriteResult))) {

                    final Metadata clusterMetadata = finalizeSnapshotContext.clusterMetadata();
                    executor.execute(
                        ActionRunnable.run(
                            allMetaListeners.acquire(),
                            () -> GLOBAL_METADATA_FORMAT.write(clusterMetadata, blobContainer(), snapshotId.getUUID(), compress)
                        )
                    );

                    for (IndexId index : indices) {
                        executor.execute(ActionRunnable.run(allMetaListeners.acquire(), () -> {
                            final IndexMetadata indexMetaData = clusterMetadata.index(index.getName());
                            if (writeIndexGens) {
                                final String identifiers = IndexMetaDataGenerations.buildUniqueIdentifier(indexMetaData);
                                String metaUUID = existingRepositoryData.indexMetaDataGenerations().getIndexMetaBlobId(identifiers);
                                if (metaUUID == null) {
                                    metaUUID = UUIDs.base64UUID();
                                    INDEX_METADATA_FORMAT.write(indexMetaData, indexContainer(index), metaUUID, compress);
                                    metadataWriteResult.indexMetaIdentifiers().put(identifiers, metaUUID);
                                } 
                                metadataWriteResult.indexMetas().put(index, identifiers);
                            } else {
                                INDEX_METADATA_FORMAT.write(
                                    clusterMetadata.index(index.getName()),
                                    indexContainer(index),
                                    snapshotId.getUUID(),
                                    compress
                                );
                            }
                        }));
                    }

                    executor.execute(
                        ActionRunnable.run(
                            allMetaListeners.acquire(),
                            () -> SNAPSHOT_FORMAT.write(snapshotInfo, blobContainer(), snapshotId.getUUID(), compress)
                        )
                    );

                }
            })

            .<RootBlobUpdateResult>andThen((l, metadataWriteResult) -> {
                final var snapshotDetails = SnapshotDetails.fromSnapshotInfo(snapshotInfo);
                final var existingRepositoryData = metadataWriteResult.existingRepositoryData();
                writeIndexGen(
                    existingRepositoryData.addSnapshot(
                        snapshotId,
                        snapshotDetails,
                        shardGenerations,
                        metadataWriteResult.indexMetas(),
                        metadataWriteResult.indexMetaIdentifiers()
                    ),
                    repositoryStateId,
                    repositoryMetaVersion,
                    new Function<>() {
                        @Override
                        public ClusterState apply(ClusterState state) {
                            return finalizeSnapshotContext.updatedClusterState(state);
                        }

                        @Override
                        public String toString() {
                            return "finalizing snapshot [" + metadata.name() + "][" + snapshotId + "]";
                        }
                    },
                    l.map(newRepositoryData -> new RootBlobUpdateResult(existingRepositoryData, newRepositoryData))
                );
            })

            .<RepositoryData>andThen((l, rootBlobUpdateResult) -> {
                l.onResponse(rootBlobUpdateResult.newRepositoryData());
                cleanupOldMetadata(
                    rootBlobUpdateResult.oldRepositoryData(),
                    rootBlobUpdateResult.newRepositoryData(),
                    finalizeSnapshotContext,
                    snapshotInfo,
                    writeShardGens
                );
            })

            .addListener(
                finalizeSnapshotContext.delegateResponse(
                    (l, e) -> l.onFailure(new SnapshotException(metadata.name(), snapshotId, "failed to update snapshot in repository", e))
                )
            );
    }

    private void cleanupOldMetadata(
        RepositoryData existingRepositoryData,
        RepositoryData updatedRepositoryData,
        FinalizeSnapshotContext finalizeSnapshotContext,
        SnapshotInfo snapshotInfo,
        boolean writeShardGenerations
    ) {
        final Set<String> toDelete = new HashSet<>();
        long newRepoGeneration = updatedRepositoryData.getGenId();
        for (long gen = Math.max(
            Math.max(existingRepositoryData.getGenId() - 1, 0),
            newRepoGeneration - 1000
        ); gen < newRepoGeneration; gen++) {
            toDelete.add(INDEX_FILE_PREFIX + gen);
        }
        if (writeShardGenerations) {
            final int prefixPathLen = basePath().buildAsString().length();
            updatedRepositoryData.shardGenerations()
                .obsoleteShardGenerations(existingRepositoryData.shardGenerations())
                .forEach(
                    (indexId, gens) -> gens.forEach(
                        (shardId, oldGen) -> toDelete.add(
                            shardPath(indexId, shardId).buildAsString().substring(prefixPathLen) + INDEX_FILE_PREFIX + oldGen
                                .toBlobNamePart()
                        )
                    )
                );
            for (Map.Entry<RepositoryShardId, Set<ShardGeneration>> obsoleteEntry : finalizeSnapshotContext.obsoleteShardGenerations()
                .entrySet()) {
                final String containerPath = shardPath(obsoleteEntry.getKey().index(), obsoleteEntry.getKey().shardId()).buildAsString()
                    .substring(prefixPathLen) + INDEX_FILE_PREFIX;
                for (ShardGeneration shardGeneration : obsoleteEntry.getValue()) {
                    toDelete.add(containerPath + shardGeneration);
                }
            }
        }

        if (toDelete.isEmpty() == false) {
            threadPool.executor(ThreadPool.Names.SNAPSHOT).execute(new AbstractRunnable() {
                @Override
                protected void doRun() throws Exception {
                    deleteFromContainer(OperationPurpose.SNAPSHOT_METADATA, blobContainer(), toDelete.iterator());
                }

                @Override
                public void onFailure(Exception e) {
                    logger.warn("Failed to clean up old metadata blobs", e);
                }

                @Override
                public void onAfter() {
                    finalizeSnapshotContext.onDone(snapshotInfo);
                }
            });
        } else {
            finalizeSnapshotContext.onDone(snapshotInfo);
        }
    }

    @Override
    public void getSnapshotInfo(
        Collection<SnapshotId> snapshotIds,
        boolean abortOnFailure,
        BooleanSupplier isCancelled,
        CheckedConsumer<SnapshotInfo, Exception> consumer,
        ActionListener<Void> listener
    ) {
        final var context = new GetSnapshotInfoContext(snapshotIds, abortOnFailure, isCancelled, (ctx, sni) -> {
            try {
                consumer.accept(sni);
            } catch (Exception e) {
                ctx.onFailure(e);
            }
        }, listener);
        final int workers = Math.min(threadPool.info(ThreadPool.Names.SNAPSHOT_META).getMax(), context.snapshotIds().size());
        final BlockingQueue<SnapshotId> queue = new LinkedBlockingQueue<>(context.snapshotIds());
        for (int i = 0; i < workers; i++) {
            getOneSnapshotInfo(queue, context);
        }
    }

    /**
     * Tries to poll a {@link SnapshotId} to load {@link SnapshotInfo} for from the given {@code queue}.
     */
    private void getOneSnapshotInfo(BlockingQueue<SnapshotId> queue, GetSnapshotInfoContext context) {
        final SnapshotId snapshotId = queue.poll();
        if (snapshotId == null) {
            return;
        }
        threadPool.executor(ThreadPool.Names.SNAPSHOT_META).execute(() -> {
            if (context.done()) {
                return;
            }
            if (context.isCancelled()) {
                queue.clear();
                context.onFailure(new TaskCancelledException("task cancelled"));
                return;
            }
            Exception failure = null;
            SnapshotInfo snapshotInfo = null;
            try {
                snapshotInfo = SNAPSHOT_FORMAT.read(metadata.name(), blobContainer(), snapshotId.getUUID(), namedXContentRegistry);
            } catch (NoSuchFileException ex) {
                failure = new SnapshotMissingException(metadata.name(), snapshotId, ex);
            } catch (IOException | NotXContentException ex) {
                failure = new SnapshotException(metadata.name(), snapshotId, "failed to get snapshot info" + snapshotId, ex);
            } catch (Exception e) {
                failure = e instanceof SnapshotException
                    ? e
                    : new SnapshotException(metadata.name(), snapshotId, "Snapshot could not be read", e);
            }
            if (failure != null) {
                if (context.abortOnFailure()) {
                    queue.clear();
                }
                context.onFailure(failure);
            } else {
                assert snapshotInfo != null;
                context.onResponse(snapshotInfo);
            }
            getOneSnapshotInfo(queue, context);
        });
    }

    @Override
    public Metadata getSnapshotGlobalMetadata(final SnapshotId snapshotId) {
        try {
            return GLOBAL_METADATA_FORMAT.read(metadata.name(), blobContainer(), snapshotId.getUUID(), namedXContentRegistry);
        } catch (NoSuchFileException ex) {
            throw new SnapshotMissingException(metadata.name(), snapshotId, ex);
        } catch (IOException ex) {
            throw new SnapshotException(metadata.name(), snapshotId, "failed to read global metadata", ex);
        }
    }

    @Override
    public IndexMetadata getSnapshotIndexMetaData(RepositoryData repositoryData, SnapshotId snapshotId, IndexId index) throws IOException {
        try {
            return INDEX_METADATA_FORMAT.read(
                metadata.name(),
                indexContainer(index),
                repositoryData.indexMetaDataGenerations().indexMetaBlobId(snapshotId, index),
                namedXContentRegistry
            );
        } catch (NoSuchFileException e) {
            throw new SnapshotMissingException(metadata.name(), snapshotId, e);
        }
    }

    private void deleteFromContainer(OperationPurpose purpose, BlobContainer container, Iterator<String> blobs) throws IOException {
        final Iterator<String> wrappedIterator;
        if (logger.isTraceEnabled()) {
            wrappedIterator = new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return blobs.hasNext();
                }

                @Override
                public String next() {
                    final String blobName = blobs.next();
                    logger.trace("[{}] Deleting [{}] from [{}]", metadata.name(), blobName, container.path());
                    return blobName;
                }
            };
        } else {
            wrappedIterator = blobs;
        }
        container.deleteBlobsIgnoringIfNotExists(purpose, wrappedIterator);
    }

    private BlobPath indicesPath() {
        return basePath().add("indices");
    }

    private BlobContainer indexContainer(IndexId indexId) {
        return blobStore().blobContainer(indexPath(indexId));
    }

    private BlobContainer shardContainer(IndexId indexId, ShardId shardId) {
        return shardContainer(indexId, shardId.getId());
    }

    private BlobPath indexPath(IndexId indexId) {
        return indicesPath().add(indexId.getId());
    }

    private BlobPath shardPath(IndexId indexId, int shardId) {
        return indexPath(indexId).add(Integer.toString(shardId));
    }

    public BlobContainer shardContainer(IndexId indexId, int shardId) {
        return blobStore().blobContainer(shardPath(indexId, shardId));
    }

    /**
     * Configures RateLimiter based on repository and global settings
     *
     * @param rateLimiter              the existing rate limiter to configure (or null if no throttling was previously needed)
     * @param maxConfiguredBytesPerSec the configured max bytes per sec from the settings
     * @param settingKey               setting used to configure the rate limiter
     * @param warnIfOverRecovery       log a warning if rate limit setting is over the effective recovery rate limit
     * @return the newly configured rate limiter or null if no throttling is needed
     */
    private RateLimiter getRateLimiter(
        RateLimiter rateLimiter,
        ByteSizeValue maxConfiguredBytesPerSec,
        String settingKey,
        boolean warnIfOverRecovery
    ) {
        if (maxConfiguredBytesPerSec.getBytes() <= 0) {
            return null;
        } else {
            ByteSizeValue effectiveRecoverySpeed = recoverySettings.getMaxBytesPerSec();
            if (warnIfOverRecovery && effectiveRecoverySpeed.getBytes() > 0) {
                if (maxConfiguredBytesPerSec.getBytes() > effectiveRecoverySpeed.getBytes()) {
                    logger.warn(
                        "repository [{}] has a rate limit [{}={}] per second which is above the effective recovery rate limit "
                            + "[{}={}] per second, thus the repository rate limit will be superseded by the recovery rate limit",
                        metadata.name(),
                        settingKey,
                        maxConfiguredBytesPerSec,
                        INDICES_RECOVERY_MAX_BYTES_PER_SEC_SETTING.getKey(),
                        effectiveRecoverySpeed
                    );
                }
            }

            if (rateLimiter != null) {
                rateLimiter.setMBPerSec(maxConfiguredBytesPerSec.getMbFrac());
                return rateLimiter;
            } else {
                return new RateLimiter.SimpleRateLimiter(maxConfiguredBytesPerSec.getMbFrac());
            }
        }
    }

    RateLimiter getSnapshotRateLimiter() {
        Settings repositorySettings = metadata.settings();
        ByteSizeValue maxConfiguredBytesPerSec = MAX_SNAPSHOT_BYTES_PER_SEC.get(repositorySettings);
        if (MAX_SNAPSHOT_BYTES_PER_SEC.exists(repositorySettings) == false && recoverySettings.nodeBandwidthSettingsExist()) {
            assert maxConfiguredBytesPerSec.getMb() == 40;
            maxConfiguredBytesPerSec = ByteSizeValue.ZERO;
        }
        return getRateLimiter(
            snapshotRateLimiter,
            maxConfiguredBytesPerSec,
            MAX_SNAPSHOT_BYTES_PER_SEC.getKey(),
            recoverySettings.nodeBandwidthSettingsExist()
        );
    }

    RateLimiter getRestoreRateLimiter() {
        return getRateLimiter(
            restoreRateLimiter,
            MAX_RESTORE_BYTES_PER_SEC.get(metadata.settings()),
            MAX_RESTORE_BYTES_PER_SEC.getKey(),
            true
        );
    }

    @Override
    public long getSnapshotThrottleTimeInNanos() {
        return snapshotRateLimitingTimeInNanos.count();
    }

    @Override
    public long getRestoreThrottleTimeInNanos() {
        return restoreRateLimitingTimeInNanos.count();
    }

    private void assertSnapshotOrStatelessPermittedThreadPool() {
        assert ThreadPool.assertCurrentThreadPool(
            ThreadPool.Names.SNAPSHOT,
            ThreadPool.Names.SNAPSHOT_META,
            ThreadPool.Names.GENERIC,
            STATELESS_SHARD_READ_THREAD_NAME,
            STATELESS_TRANSLOG_THREAD_NAME,
            STATELESS_SHARD_WRITE_THREAD_NAME,
            STATELESS_CLUSTER_STATE_READ_WRITE_THREAD_NAME,
            STATELESS_SHARD_PREWARMING_THREAD_NAME
        );
    }

    @Override
    public String startVerification() {
        try {
            if (isReadOnly()) {
                latestIndexBlobId();
                return "read-only";
            } else {
                String seed = UUIDs.randomBase64UUID();
                byte[] testBytes = Strings.toUTF8Bytes(seed);
                BlobContainer testContainer = blobStore().blobContainer(basePath().add(testBlobPrefix(seed)));
                testContainer.writeBlobAtomic(OperationPurpose.SNAPSHOT_METADATA, "master.dat", new BytesArray(testBytes), true);
                return seed;
            }
        } catch (Exception exp) {
            throw new RepositoryVerificationException(metadata.name(), "path " + basePath() + " is not accessible on master node", exp);
        }
    }

    @Override
    public void endVerification(String seed) {
        if (isReadOnly() == false) {
            try {
                final String testPrefix = testBlobPrefix(seed);
                blobStore().blobContainer(basePath().add(testPrefix)).delete(OperationPurpose.SNAPSHOT_METADATA);
            } catch (Exception exp) {
                throw new RepositoryVerificationException(metadata.name(), "cannot delete test data at " + basePath(), exp);
            }
        }
    }

    private final AtomicLong latestKnownRepoGen = new AtomicLong(RepositoryData.UNKNOWN_REPO_GEN);

    private final AtomicReference<RepositoryData> latestKnownRepositoryData = new AtomicReference<>(RepositoryData.EMPTY);

    @Override
    public void getRepositoryData(Executor responseExecutor, ActionListener<RepositoryData> listener) {
        assert clusterService.localNode().isMasterNode() : "should only load repository data on master nodes";

        while (true) {

            if (lifecycle.started() == false) {
                listener.onFailure(notStartedException());
                return;
            }

            if (latestKnownRepoGen.get() == RepositoryData.CORRUPTED_REPO_GEN) {
                listener.onFailure(corruptedStateException(null, null));
                return;
            }
            final RepositoryData cached = latestKnownRepositoryData.get();
            if (bestEffortConsistency == false && cached.getGenId() == latestKnownRepoGen.get()) {
                listener.onResponse(cached);
                return;
            }
            if (metadata.generation() == RepositoryData.UNKNOWN_REPO_GEN && isReadOnly() == false) {
                logger.debug("""
                    [{}] loading repository metadata for the first time, trying to determine correct generation and to store it in the \
                    cluster state""", metadata.name());
                if (initializeRepoGenerationTracking(responseExecutor, listener)) {
                    return;
                } 
            } else {
                logger.trace(
                    "[{}] loading un-cached repository data with best known repository generation [{}]",
                    metadata.name(),
                    latestKnownRepoGen
                );
                repoDataLoadDeduplicator.execute(new ThreadedActionListener<>(responseExecutor, listener));
                return;
            }
        }
    }

    private RepositoryException notStartedException() {
        return new RepositoryException(metadata.name(), "repository is not in started state");
    }

    @Nullable 
    private SubscribableListener<RepositoryData> repoDataInitialized;

    /**
     * Method used to set the current repository generation in the cluster state's {@link RepositoryMetadata} to the latest generation that
     * can be physically found in the repository before passing the latest {@link RepositoryData} to the given listener.
     * This ensures that operations using {@code SnapshotsService#executeConsistentStateUpdate} right after mounting a fresh repository will
     * have a consistent view of the {@link RepositoryData} before any data has been written to the repository.
     *
     * @param responseExecutor executor to use to complete the listener if not completing it on the calling thread
     * @param listener         listener to resolve with new repository data
     * @return {@code true} if this method at least started the initialization process successfully and will eventually complete the
     * listener, {@code false} if there was some concurrent state change which prevents us from starting repo generation tracking (typically
     * that some other node got there first) and the caller should check again and possibly retry or complete the listener in some other
     * way.
     */
    private boolean initializeRepoGenerationTracking(Executor responseExecutor, ActionListener<RepositoryData> listener) {
        final SubscribableListener<RepositoryData> listenerToSubscribe;
        final ActionListener<RepositoryData> listenerToComplete;

        synchronized (this) {
            if (repoDataInitialized == null) {
                if (metadata.generation() != RepositoryData.UNKNOWN_REPO_GEN) {
                    return false; 
                }
                logger.trace("[{}] initializing repository generation in cluster state", metadata.name());
                repoDataInitialized = listenerToSubscribe = new SubscribableListener<>();
                listenerToComplete = new ActionListener<>() {
                    private ActionListener<RepositoryData> acquireAndClearRepoDataInitialized() {
                        synchronized (BlobStoreRepository.this) {
                            assert repoDataInitialized == listenerToSubscribe;
                            repoDataInitialized = null;
                            return listenerToSubscribe;
                        }
                    }

                    @Override
                    public void onResponse(RepositoryData repositoryData) {
                        acquireAndClearRepoDataInitialized().onResponse(repositoryData);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        logger.warn(
                            () -> format("[%s] Exception when initializing repository generation in cluster state", metadata.name()),
                            e
                        );
                        acquireAndClearRepoDataInitialized().onFailure(e);
                    }
                };
            } else {
                logger.trace(
                    "[{}] waiting for existing initialization of repository metadata generation in cluster state",
                    metadata.name()
                );
                listenerToComplete = null;
                listenerToSubscribe = repoDataInitialized;
            }
        }

        if (listenerToComplete != null) {
            SubscribableListener
                .newForked(repoDataLoadDeduplicator::execute)
                .<RepositoryData>andThen(
                    (l, repoData) -> submitUnbatchedTask(
                        "set initial safe repository generation [" + metadata.name() + "][" + repoData.getGenId() + "]",
                        new ClusterStateUpdateTask() {
                            @Override
                            public ClusterState execute(ClusterState currentState) {
                                return getClusterStateWithUpdatedRepositoryGeneration(currentState, repoData);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                l.onFailure(e);
                            }

                            @Override
                            public void clusterStateProcessed(ClusterState oldState, ClusterState newState) {
                                l.onResponse(repoData);
                            }
                        }
                    )
                )
                .<RepositoryData>andThen((l, repoData) -> {
                    logger.trace("[{}] initialized repository generation in cluster state to [{}]", metadata.name(), repoData.getGenId());
                    threadPool.generic().execute(ActionRunnable.supply(ActionListener.runAfter(l, () -> {
                        logger.trace(
                            "[{}] called listeners after initializing repository to generation [{}]",
                            metadata.name(),
                            repoData.getGenId()
                        );
                    }), () -> repoData));
                })
                .addListener(listenerToComplete);
        }

        listenerToSubscribe.addListener(listener, responseExecutor, threadPool.getThreadContext());
        return true;
    }

    private ClusterState getClusterStateWithUpdatedRepositoryGeneration(ClusterState currentState, RepositoryData repoData) {
        final RepositoryMetadata repoMetadata = getRepoMetadata(currentState);
        if (repoMetadata.generation() != RepositoryData.UNKNOWN_REPO_GEN) {
            throw new RepositoryException(repoMetadata.name(), "Found unexpected initialized repo metadata [" + repoMetadata + "]");
        }
        return ClusterState.builder(currentState)
            .metadata(
                Metadata.builder(currentState.getMetadata())
                    .putCustom(
                        RepositoriesMetadata.TYPE,
                        RepositoriesMetadata.get(currentState)
                            .withUpdatedGeneration(repoMetadata.name(), repoData.getGenId(), repoData.getGenId())
                    )
            )
            .build();
    }

    /**
     * Deduplicator that deduplicates the physical loading of {@link RepositoryData} from the repositories' underlying storage.
     */
    private final SingleResultDeduplicator<RepositoryData> repoDataLoadDeduplicator;

    private void doGetRepositoryData(ActionListener<RepositoryData> listener) {

        long lastFailedGeneration = RepositoryData.UNKNOWN_REPO_GEN;
        while (true) {
            final long genToLoad;
            if (bestEffortConsistency) {
                final long generation;
                try {
                    generation = latestIndexBlobId();
                } catch (Exception e) {
                    listener.onFailure(
                        new RepositoryException(metadata.name(), "Could not determine repository generation from root blobs", e)
                    );
                    return;
                }
                genToLoad = latestKnownRepoGen.accumulateAndGet(generation, Math::max);
                if (genToLoad > generation) {
                    logger.info(
                        "Determined repository generation [{}] from repository contents but correct generation must be at " + "least [{}]",
                        generation,
                        genToLoad
                    );
                }
            } else {
                genToLoad = latestKnownRepoGen.get();
            }
            try {
                final RepositoryData cached = latestKnownRepositoryData.get();
                if (bestEffortConsistency == false && cached.getGenId() == genToLoad) {
                    listener.onResponse(cached);
                } else {
                    final RepositoryData loaded = getRepositoryData(genToLoad);
                    if (cached == null || cached.getGenId() < genToLoad) {
                        cacheRepositoryData(loaded, IndexVersion.current());
                    }
                    if (loaded.getUuid().equals(metadata.uuid())) {
                        listener.onResponse(loaded);
                    } else {
                        RepositoriesService.updateRepositoryUuidInMetadata(
                            clusterService,
                            metadata.name(),
                            loaded,
                            new ThreadedActionListener<>(threadPool.generic(), listener.map(v -> loaded))
                        );
                    }
                }
                return;
            } catch (RepositoryException e) {
                if (genToLoad != latestKnownRepoGen.get() && genToLoad != lastFailedGeneration) {
                    lastFailedGeneration = genToLoad;
                    logger.warn(
                        "Failed to load repository data generation ["
                            + genToLoad
                            + "] because a concurrent operation moved the current generation to ["
                            + latestKnownRepoGen.get()
                            + "]",
                        e
                    );
                    continue;
                }
                if (bestEffortConsistency == false && ExceptionsHelper.unwrap(e, NoSuchFileException.class) != null) {
                    Tuple<Long, String> previousWriterInformation = null;
                    try {
                        previousWriterInformation = readLastWriterInfo();
                    } catch (Exception ex) {
                        e.addSuppressed(ex);
                    }
                    final Tuple<Long, String> finalLastInfo = previousWriterInformation;
                    markRepoCorrupted(
                        genToLoad,
                        e,
                        listener.delegateFailureAndWrap((l, v) -> l.onFailure(corruptedStateException(e, finalLastInfo)))
                    );
                } else {
                    listener.onFailure(e);
                }
                return;
            } catch (Exception e) {
                listener.onFailure(new RepositoryException(metadata.name(), "Unexpected exception when loading repository data", e));
                return;
            }
        }
    }

    /**
     * Cache repository data if repository data caching is enabled.
     *
     * @param repositoryData repository data to cache
     * @param version        repository metadata version used when writing the data to the repository
     */
    private void cacheRepositoryData(RepositoryData repositoryData, IndexVersion version) {
        if (cacheRepositoryData == false) {
            return;
        }
        final RepositoryData toCache;
        if (SnapshotsService.useShardGenerations(version)) {
            toCache = repositoryData;
        } else {
            toCache = repositoryData.withoutShardGenerations();
            assert repositoryData.indexMetaDataGenerations().equals(IndexMetaDataGenerations.EMPTY)
                : "repository data should not contain index generations at version ["
                    + version.toReleaseVersion()
                    + "] but saw ["
                    + repositoryData.indexMetaDataGenerations()
                    + "]";
        }
        assert toCache.getGenId() >= 0 : "No need to cache abstract generations but attempted to cache [" + toCache.getGenId() + "]";
        latestKnownRepositoryData.updateAndGet(known -> {
            if (known.getGenId() > toCache.getGenId()) {
                return known;
            }
            return toCache;
        });
    }

    private RepositoryException corruptedStateException(@Nullable Exception cause, @Nullable Tuple<Long, String> previousWriterInfo) {
        return new RepositoryException(metadata.name(), Strings.format("""
            The repository has been disabled to prevent data corruption because its contents were found not to match its expected state. \
            This is either because something other than this cluster modified the repository contents, or because the repository's \
            underlying storage behaves incorrectly. To re-enable this repository, first ensure that this cluster has exclusive write \
            access to it, and then re-register the repository with this cluster. See %s for further information.\
            %s""", ReferenceDocs.CONCURRENT_REPOSITORY_WRITERS, previousWriterMessage(previousWriterInfo)), cause);
    }

    private static String previousWriterMessage(@Nullable Tuple<Long, String> previousWriterInfo) {
        return previousWriterInfo == null
            ? ""
            : " The last cluster to write to this repository was ["
                + previousWriterInfo.v2()
                + "] at generation ["
                + previousWriterInfo.v1()
                + "].";
    }

    /**
     * Marks the repository as corrupted. This puts the repository in a state where its tracked value for
     * {@link RepositoryMetadata#pendingGeneration()} is unchanged while its value for {@link RepositoryMetadata#generation()} is set to
     * {@link RepositoryData#CORRUPTED_REPO_GEN}. In this state, the repository can not be used any longer and must be removed and
     * recreated after the problem that lead to it being marked as corrupted has been fixed.
     *
     * @param corruptedGeneration generation that failed to load because the index file was not found but that should have loaded
     * @param originalException   exception that lead to the failing to load the {@code index-N} blob
     * @param listener            listener to invoke once done
     */
    private void markRepoCorrupted(long corruptedGeneration, Exception originalException, ActionListener<Void> listener) {
        assert corruptedGeneration != RepositoryData.UNKNOWN_REPO_GEN;
        assert bestEffortConsistency == false;
        logger.warn(() -> "Marking repository [" + metadata.name() + "] as corrupted", originalException);
        submitUnbatchedTask(
            "mark repository corrupted [" + metadata.name() + "][" + corruptedGeneration + "]",
            new ClusterStateUpdateTask() {
                @Override
                public ClusterState execute(ClusterState currentState) {
                    final RepositoriesMetadata state = RepositoriesMetadata.get(currentState);
                    final RepositoryMetadata repoState = state.repository(metadata.name());
                    if (repoState.generation() != corruptedGeneration) {
                        throw new IllegalStateException(
                            "Tried to mark repo generation ["
                                + corruptedGeneration
                                + "] as corrupted but its state concurrently changed to ["
                                + repoState
                                + "]"
                        );
                    }
                    return ClusterState.builder(currentState)
                        .metadata(
                            Metadata.builder(currentState.metadata())
                                .putCustom(
                                    RepositoriesMetadata.TYPE,
                                    state.withUpdatedGeneration(
                                        metadata.name(),
                                        RepositoryData.CORRUPTED_REPO_GEN,
                                        repoState.pendingGeneration()
                                    )
                                )
                                .build()
                        )
                        .build();
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(
                        new RepositoryException(
                            metadata.name(),
                            "Failed marking repository state as corrupted",
                            ExceptionsHelper.useOrSuppress(e, originalException)
                        )
                    );
                }

                @Override
                public void clusterStateProcessed(ClusterState oldState, ClusterState newState) {
                    listener.onResponse(null);
                }
            }
        );
    }

    private RepositoryData getRepositoryData(long indexGen) {
        if (indexGen == RepositoryData.EMPTY_REPO_GEN) {
            return RepositoryData.EMPTY;
        }
        try {
            final String snapshotsIndexBlobName = INDEX_FILE_PREFIX + Long.toString(indexGen);

            try (
                InputStream blob = blobContainer().readBlob(OperationPurpose.SNAPSHOT_METADATA, snapshotsIndexBlobName);
                XContentParser parser = XContentType.JSON.xContent()
                    .createParser(NamedXContentRegistry.EMPTY, LoggingDeprecationHandler.INSTANCE, blob)
            ) {
                return RepositoryData.snapshotsFromXContent(parser, indexGen, true);
            }
        } catch (IOException ioe) {
            if (bestEffortConsistency) {
                if (latestKnownRepoGen.compareAndSet(indexGen, RepositoryData.EMPTY_REPO_GEN)) {
                    logger.warn("Resetting repository generation tracker because we failed to read generation [" + indexGen + "]", ioe);
                }
            }
            throw new RepositoryException(metadata.name(), "could not read repository data from index blob", ioe);
        }
    }

    private static String testBlobPrefix(String seed) {
        return TESTS_FILE + seed;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Writing a new index generation (root) blob is a three-step process. Typically, it starts from a stable state where the pending
     * generation {@link RepositoryMetadata#pendingGeneration()} is equal to the safe generation {@link RepositoryMetadata#generation()},
     * but after a failure it may be that the pending generation starts out greater than the safe generation.
     * <ol>
     * <li>
     * We reserve ourselves a new root blob generation {@code G}, greater than {@link RepositoryMetadata#pendingGeneration()}, via a
     * cluster state update which edits the {@link RepositoryMetadata} entry for this repository, increasing its pending generation to
     * {@code G} without changing its safe generation.
     * <li>
     * We write the updated {@link RepositoryData} to a new root blob with generation {@code G}.
     * <li>
     * We mark the successful end of the update of the repository data with a cluster state update which edits the
     * {@link RepositoryMetadata} entry for this repository again, increasing its safe generation to equal to its pending generation
     * {@code G}.
     * </ol>
     * We use this process to protect against problems such as a master failover part-way through. If a new master is elected while we're
     * writing the root blob with generation {@code G} then we will fail to update the safe repository generation in the final step, and
     * meanwhile the new master will choose a generation greater than {@code G} for all subsequent root blobs so there is no risk that we
     * will clobber its writes. See the package level documentation for {@link org.elasticsearch.repositories.blobstore} for more details.
     * <p>
     * Note that a failure here does not imply that the process was unsuccessful or the repository is unchanged. Once we have written the
     * new root blob the repository is updated from the point of view of any other clusters reading from it, and if we performed a full
     * cluster restart at that point then we would also pick up the new root blob. Writing the root blob may succeed without us receiving
     * a successful response from the repository, leading us to report that the write operation failed. Updating the safe generation may
     * likewise succeed on a majority of master-eligible nodes which does not include this one, again leading to an apparent failure.
     * <p>
     * We therefore cannot safely clean up apparently-dangling blobs after a failure here. Instead, we defer any cleanup until after the
     * next successful root-blob write, which may happen on a different master node or possibly even in a different cluster.
     *
     * @param repositoryData RepositoryData to write
     * @param expectedGen    expected repository generation at the start of the operation
     * @param version        version of the repository metadata to write
     * @param stateFilter    filter for the last cluster state update executed by this method
     * @param listener       completion listener
     */
    protected void writeIndexGen(
        RepositoryData repositoryData,
        long expectedGen,
        IndexVersion version,
        Function<ClusterState, ClusterState> stateFilter,
        ActionListener<RepositoryData> listener
    ) {
        logger.trace("[{}] writing repository data on top of expected generation [{}]", metadata.name(), expectedGen);
        assert isReadOnly() == false; 
        final long currentGen = repositoryData.getGenId();
        if (currentGen != expectedGen) {
            listener.onFailure(
                new RepositoryException(
                    metadata.name(),
                    "concurrent modification of the index-N file, expected current generation ["
                        + expectedGen
                        + "], actual current generation ["
                        + currentGen
                        + "]"
                )
            );
            return;
        }

        final ListenableFuture<Long> setPendingStep = new ListenableFuture<>();
        final String setPendingGenerationSource = "set pending repository generation [" + metadata.name() + "][" + expectedGen + "]";
        submitUnbatchedTask(setPendingGenerationSource, new ClusterStateUpdateTask() {

            private long newGen;

            @Override
            public ClusterState execute(ClusterState currentState) {
                final RepositoryMetadata meta = getRepoMetadata(currentState);
                final String repoName = metadata.name();
                final long genInState = meta.generation();
                final boolean uninitializedMeta = meta.generation() == RepositoryData.UNKNOWN_REPO_GEN || bestEffortConsistency;
                if (uninitializedMeta == false && meta.pendingGeneration() != genInState) {
                    logger.info(
                        "Trying to write new repository data over unfinished write, repo [{}] is at "
                            + "safe generation [{}] and pending generation [{}]",
                        meta.name(),
                        genInState,
                        meta.pendingGeneration()
                    );
                }
                assert expectedGen == RepositoryData.EMPTY_REPO_GEN || uninitializedMeta || expectedGen == meta.generation()
                    : "Expected non-empty generation [" + expectedGen + "] does not match generation tracked in [" + meta + "]";
                final long safeGeneration = expectedGen == RepositoryData.EMPTY_REPO_GEN
                    ? RepositoryData.EMPTY_REPO_GEN
                    : (uninitializedMeta ? expectedGen : genInState);
                final long nextPendingGen = metadata.pendingGeneration() + 1;
                newGen = uninitializedMeta ? Math.max(expectedGen + 1, nextPendingGen) : nextPendingGen;
                assert newGen > latestKnownRepoGen.get()
                    : "Attempted new generation ["
                        + newGen
                        + "] must be larger than latest known generation ["
                        + latestKnownRepoGen.get()
                        + "]";
                return ClusterState.builder(currentState)
                    .metadata(
                        Metadata.builder(currentState.getMetadata())
                            .putCustom(
                                RepositoriesMetadata.TYPE,
                                RepositoriesMetadata.get(currentState).withUpdatedGeneration(repoName, safeGeneration, newGen)
                            )
                            .build()
                    )
                    .build();
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(
                    new RepositoryException(
                        metadata.name(),
                        "Failed to execute cluster state update [" + setPendingGenerationSource + "]",
                        e
                    )
                );
            }

            @Override
            public void clusterStateProcessed(ClusterState oldState, ClusterState newState) {
                logger.trace("[{}] successfully set pending repository generation to [{}]", metadata.name(), newGen);
                setPendingStep.onResponse(newGen);
            }

            @Override
            public String toString() {
                return Strings.format("start RepositoryData update from generation [%d], stateFilter=[%s]", expectedGen, stateFilter);
            }
        });

        final ListenableFuture<RepositoryData> filterRepositoryDataStep = new ListenableFuture<>();

        setPendingStep.addListener(
            listener.delegateFailureAndWrap(
                (delegate, newGen) -> threadPool().executor(ThreadPool.Names.SNAPSHOT).execute(ActionRunnable.wrap(delegate, l -> {
                    final List<SnapshotId> snapshotIdsWithMissingDetails = repositoryData.getSnapshotIds()
                        .stream()
                        .filter(repositoryData::hasMissingDetails)
                        .toList();
                    if (snapshotIdsWithMissingDetails.isEmpty() == false) {
                        final Map<SnapshotId, SnapshotDetails> extraDetailsMap = new ConcurrentHashMap<>();
                        getSnapshotInfo(
                            snapshotIdsWithMissingDetails,
                            false,
                            () -> false,
                            snapshotInfo -> extraDetailsMap.put(snapshotInfo.snapshotId(), SnapshotDetails.fromSnapshotInfo(snapshotInfo)),
                            ActionListener.runAfter(new ActionListener<>() {
                                @Override
                                public void onResponse(Void aVoid) {
                                    logger.info(
                                        "Successfully loaded all snapshots' detailed information for {} from snapshot metadata",
                                        AllocationService.firstListElementsToCommaDelimitedString(
                                            snapshotIdsWithMissingDetails,
                                            SnapshotId::toString,
                                            logger.isDebugEnabled()
                                        )
                                    );
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    logger.warn("Failure when trying to load missing details from snapshot metadata", e);
                                }
                            }, () -> filterRepositoryDataStep.onResponse(repositoryData.withExtraDetails(extraDetailsMap)))
                        );
                    } else {
                        filterRepositoryDataStep.onResponse(repositoryData);
                    }
                }))
            )
        );
        filterRepositoryDataStep.addListener(listener.delegateFailureAndWrap((delegate, filteredRepositoryData) -> {
            final long newGen = setPendingStep.result();
            final RepositoryData newRepositoryData = updateRepositoryData(filteredRepositoryData, version, newGen);
            if (latestKnownRepoGen.get() >= newGen) {
                throw new IllegalArgumentException(
                    "Tried writing generation ["
                        + newGen
                        + "] but repository is at least at generation ["
                        + latestKnownRepoGen.get()
                        + "] already"
                );
            }
            if (ensureSafeGenerationExists(expectedGen, delegate::onFailure) == false) {
                return;
            }
            final String indexBlob = INDEX_FILE_PREFIX + newGen;
            logger.debug("Repository [{}] writing new index generational blob [{}]", metadata.name(), indexBlob);
            writeAtomic(OperationPurpose.SNAPSHOT_METADATA, blobContainer(), indexBlob, out -> {
                try (XContentBuilder xContentBuilder = XContentFactory.jsonBuilder(org.elasticsearch.core.Streams.noCloseStream(out))) {
                    newRepositoryData.snapshotsToXContent(xContentBuilder, version);
                }
            }, true);
            maybeWriteIndexLatest(newGen);

            final String setSafeGenerationSource = "set safe repository generation [" + metadata.name() + "][" + newGen + "]";
            submitUnbatchedTask(setSafeGenerationSource, new ClusterStateUpdateTask() {
                @Override
                public ClusterState execute(ClusterState currentState) {
                    final RepositoryMetadata meta = getRepoMetadata(currentState);
                    if (meta.generation() != expectedGen) {
                        throw new IllegalStateException(
                            "Tried to update repo generation to [" + newGen + "] but saw unexpected generation in state [" + meta + "]"
                        );
                    }
                    if (meta.pendingGeneration() != newGen) {
                        throw new IllegalStateException(
                            "Tried to update from unexpected pending repo generation ["
                                + meta.pendingGeneration()
                                + "] after write to generation ["
                                + newGen
                                + "]"
                        );
                    }
                    final RepositoriesMetadata withGenerations = RepositoriesMetadata.get(currentState)
                        .withUpdatedGeneration(metadata.name(), newGen, newGen);
                    final RepositoriesMetadata withUuid = meta.uuid().equals(newRepositoryData.getUuid())
                        ? withGenerations
                        : withGenerations.withUuid(metadata.name(), newRepositoryData.getUuid());
                    final ClusterState newClusterState = stateFilter.apply(
                        ClusterState.builder(currentState)
                            .metadata(Metadata.builder(currentState.getMetadata()).putCustom(RepositoriesMetadata.TYPE, withUuid))
                            .build()
                    );
                    return updateRepositoryGenerationsIfNecessary(newClusterState, expectedGen, newGen);
                }

                @Override
                public void onFailure(Exception e) {
                    delegate.onFailure(
                        new RepositoryException(
                            metadata.name(),
                            "Failed to execute cluster state update [" + setSafeGenerationSource + "]",
                            e
                        )
                    );
                }

                @Override
                public void clusterStateProcessed(ClusterState oldState, ClusterState newState) {
                    logger.trace("[{}] successfully set safe repository generation to [{}]", metadata.name(), newGen);
                    cacheRepositoryData(newRepositoryData, version);
                    delegate.onResponse(newRepositoryData);
                }

                @Override
                public String toString() {
                    return Strings.format(
                        "complete RepositoryData update from generation [%d] to generation [%d], stateFilter=[%s]",
                        expectedGen,
                        newGen,
                        stateFilter
                    );
                }
            });
        }));
    }

    private RepositoryData updateRepositoryData(RepositoryData repositoryData, IndexVersion repositoryMetaversion, long newGen) {
        if (SnapshotsService.includesUUIDs(repositoryMetaversion)) {
            final String clusterUUID = clusterService.state().metadata().clusterUUID();
            if (repositoryData.getClusterUUID().equals(clusterUUID) == false) {
                repositoryData = repositoryData.withClusterUuid(clusterUUID);
            }
        }
        return repositoryData.withGenId(newGen);
    }

    /**
     * Write {@code index.latest} blob to support using this repository as the basis of a url repository.
     *
     * @param newGen new repository generation
     */
    private void maybeWriteIndexLatest(long newGen) {
        if (supportURLRepo) {
            logger.debug("Repository [{}] updating index.latest with generation [{}]", metadata.name(), newGen);
            try {
                writeAtomic(
                    OperationPurpose.SNAPSHOT_METADATA,
                    blobContainer(),
                    INDEX_LATEST_BLOB,
                    out -> out.write(Numbers.longToBytes(newGen)),
                    false
                );
            } catch (Exception e) {
                logger.warn(
                    () -> format(
                        "Failed to write index.latest blob. If you do not intend to use this "
                            + "repository as the basis for a URL repository you may turn off attempting to write the index.latest blob by "
                            + "setting repository setting [%s] to [false]",
                        SUPPORT_URL_REPO.getKey()
                    ),
                    e
                );
            }
        }
    }

    /**
     * Ensures that {@link RepositoryData} for the given {@code safeGeneration} actually physically exists in the repository.
     * This method is used by {@link #writeIndexGen} to make sure that no writes are executed on top of a concurrently modified repository.
     * This check is necessary because {@link RepositoryData} is mostly read from the cached value in {@link #latestKnownRepositoryData}
     * which could be stale in the broken situation of a concurrent write to the repository.
     *
     * @param safeGeneration generation to verify existence for
     * @param onFailure      callback to invoke with failure in case the repository generation is not physically found in the repository
     */
    private boolean ensureSafeGenerationExists(long safeGeneration, Consumer<Exception> onFailure) throws IOException {
        logger.debug("Ensure generation [{}] that is the basis for this write exists in [{}]", safeGeneration, metadata.name());
        if (safeGeneration != RepositoryData.EMPTY_REPO_GEN
            && blobContainer().blobExists(OperationPurpose.SNAPSHOT_METADATA, INDEX_FILE_PREFIX + safeGeneration) == false) {
            Tuple<Long, String> previousWriterInfo = null;
            Exception readRepoDataEx = null;
            try {
                previousWriterInfo = readLastWriterInfo();
            } catch (Exception ex) {
                readRepoDataEx = ex;
            }
            final Exception exception = new RepositoryException(
                metadata.name(),
                "concurrent modification of the index-N file, expected current generation ["
                    + safeGeneration
                    + "] but it was not found in the repository."
                    + previousWriterMessage(previousWriterInfo)
            );
            if (readRepoDataEx != null) {
                exception.addSuppressed(readRepoDataEx);
            }
            markRepoCorrupted(safeGeneration, exception, new ActionListener<>() {
                @Override
                public void onResponse(Void aVoid) {
                    onFailure.accept(exception);
                }

                @Override
                public void onFailure(Exception e) {
                    onFailure.accept(e);
                }
            });
            return false;
        }
        return true;
    }

    /**
     * Tries to find the latest cluster UUID that wrote to this repository on a best effort basis by listing out repository root contents
     * to find the latest repository generation and then reading the cluster UUID of the last writer from the {@link RepositoryData} found
     * at this generation.
     *
     * @return tuple of repository generation and cluster UUID of the last cluster to write to this repository
     */
    private Tuple<Long, String> readLastWriterInfo() throws IOException {
        assert bestEffortConsistency == false : "This should only be used for adding information to errors in consistent mode";
        final long latestGeneration = latestIndexBlobId();
        final RepositoryData actualRepositoryData = getRepositoryData(latestGeneration);
        return Tuple.tuple(latestGeneration, actualRepositoryData.getClusterUUID());
    }

    /**
     * Updates the repository generation that running deletes and snapshot finalizations will be based on for this repository if any such
     * operations are found in the cluster state while setting the safe repository generation.
     *
     * @param state  cluster state to update
     * @param oldGen previous safe repository generation
     * @param newGen new safe repository generation
     * @return updated cluster state
     */
    private ClusterState updateRepositoryGenerationsIfNecessary(ClusterState state, long oldGen, long newGen) {
        final String repoName = metadata.name();
        final SnapshotsInProgress updatedSnapshotsInProgress;
        boolean changedSnapshots = false;
        final List<SnapshotsInProgress.Entry> snapshotEntries = new ArrayList<>();
        final SnapshotsInProgress snapshotsInProgress = SnapshotsInProgress.get(state);
        for (SnapshotsInProgress.Entry entry : snapshotsInProgress.forRepo(repoName)) {
            if (entry.repositoryStateId() == oldGen) {
                snapshotEntries.add(entry.withRepoGen(newGen));
                changedSnapshots = true;
            } else {
                snapshotEntries.add(entry);
            }
        }
        updatedSnapshotsInProgress = changedSnapshots ? snapshotsInProgress.withUpdatedEntriesForRepo(repoName, snapshotEntries) : null;
        final SnapshotDeletionsInProgress updatedDeletionsInProgress;
        boolean changedDeletions = false;
        final List<SnapshotDeletionsInProgress.Entry> deletionEntries = new ArrayList<>();
        for (SnapshotDeletionsInProgress.Entry entry : SnapshotDeletionsInProgress.get(state).getEntries()) {
            if (entry.repository().equals(repoName) && entry.repositoryStateId() == oldGen) {
                deletionEntries.add(entry.withRepoGen(newGen));
                changedDeletions = true;
            } else {
                deletionEntries.add(entry);
            }
        }
        updatedDeletionsInProgress = changedDeletions ? SnapshotDeletionsInProgress.of(deletionEntries) : null;
        return SnapshotsService.updateWithSnapshots(state, updatedSnapshotsInProgress, updatedDeletionsInProgress);
    }

    private RepositoryMetadata getRepoMetadata(ClusterState state) {
        final RepositoryMetadata repositoryMetadata = RepositoriesMetadata.get(state).repository(metadata.name());
        assert repositoryMetadata != null || lifecycle.stoppedOrClosed()
            : "did not find metadata for repo [" + metadata.name() + "] in state [" + lifecycleState() + "]";
        return repositoryMetadata;
    }

    /**
     * Get the latest snapshot index blob id.  Snapshot index blobs are named index-N, where N is
     * the next version number from when the index blob was written.  Each individual index-N blob is
     * only written once and never overwritten.  The highest numbered index-N blob is the latest one
     * that contains the current snapshots in the repository.
     *
     * Package private for testing
     */
    long latestIndexBlobId() throws IOException {
        try {
            return listBlobsToGetLatestIndexId();
        } catch (UnsupportedOperationException e) {
            try {
                return readSnapshotIndexLatestBlob();
            } catch (NoSuchFileException nsfe) {
                return RepositoryData.EMPTY_REPO_GEN;
            }
        }
    }

    long readSnapshotIndexLatestBlob() throws IOException {
        final BytesReference content = Streams.readFully(
            Streams.limitStream(blobContainer().readBlob(OperationPurpose.SNAPSHOT_METADATA, INDEX_LATEST_BLOB), Long.BYTES + 1)
        );
        if (content.length() != Long.BYTES) {
            throw new RepositoryException(
                metadata.name(),
                "exception reading blob ["
                    + INDEX_LATEST_BLOB
                    + "]: expected 8 bytes but blob was "
                    + (content.length() < Long.BYTES ? content.length() + " bytes" : "longer")
            );
        }
        return Numbers.bytesToLong(content.toBytesRef());
    }

    private long listBlobsToGetLatestIndexId() throws IOException {
        return latestGeneration(blobContainer().listBlobsByPrefix(OperationPurpose.SNAPSHOT_METADATA, INDEX_FILE_PREFIX).keySet());
    }

    private long latestGeneration(Collection<String> rootBlobs) {
        long latest = RepositoryData.EMPTY_REPO_GEN;
        for (String blobName : rootBlobs) {
            if (blobName.startsWith(INDEX_FILE_PREFIX) == false) {
                continue;
            }
            try {
                final long curr = Long.parseLong(blobName.substring(INDEX_FILE_PREFIX.length()));
                latest = Math.max(latest, curr);
            } catch (NumberFormatException nfe) {
                logger.warn("[{}] Unknown blob in the repository: {}", metadata.name(), blobName);
            }
        }
        return latest;
    }

    private void writeAtomic(
        OperationPurpose purpose,
        BlobContainer container,
        final String blobName,
        CheckedConsumer<OutputStream, IOException> writer,
        boolean failIfAlreadyExists
    ) throws IOException {
        logger.trace(() -> format("[%s] Writing [%s] to %s atomically", metadata.name(), blobName, container.path()));
        container.writeMetadataBlob(purpose, blobName, failIfAlreadyExists, true, writer);
    }

    @Override
    public void snapshotShard(SnapshotShardContext context) {
        shardSnapshotTaskRunner.enqueueShardSnapshot(context);
    }

    private void doSnapshotShard(SnapshotShardContext context) {
        if (isReadOnly()) {
            context.onFailure(new RepositoryException(metadata.name(), "cannot snapshot shard on a readonly repository"));
            return;
        }
        final Store store = context.store();
        final ShardId shardId = store.shardId();
        final SnapshotId snapshotId = context.snapshotId();
        final IndexShardSnapshotStatus snapshotStatus = context.status();
        final long startTime = threadPool.absoluteTimeInMillis();
        try {
            final ShardGeneration generation = snapshotStatus.generation();
            final BlobContainer shardContainer = shardContainer(context.indexId(), shardId);
            logger.debug("[{}][{}] snapshot to [{}][{}][{}] ...", shardId, snapshotId, metadata.name(), context.indexId(), generation);
            final Set<String> blobs;
            if (generation == null) {
                snapshotStatus.ensureNotAborted();
                try {
                    blobs = shardContainer.listBlobsByPrefix(OperationPurpose.SNAPSHOT_METADATA, INDEX_FILE_PREFIX).keySet();
                } catch (IOException e) {
                    throw new IndexShardSnapshotFailedException(shardId, "failed to list blobs", e);
                }
            } else {
                blobs = Collections.singleton(INDEX_FILE_PREFIX + generation);
            }

            snapshotStatus.ensureNotAborted();
            Tuple<BlobStoreIndexShardSnapshots, ShardGeneration> tuple = buildBlobStoreIndexShardSnapshots(
                blobs,
                shardContainer,
                generation
            );
            BlobStoreIndexShardSnapshots snapshots = tuple.v1();
            ShardGeneration fileListGeneration = tuple.v2();

            if (snapshots.snapshots().stream().anyMatch(sf -> sf.snapshot().equals(snapshotId.getName()))) {
                throw new IndexShardSnapshotFailedException(
                    shardId,
                    "Duplicate snapshot name [" + snapshotId.getName() + "] detected, aborting"
                );
            }
            final List<BlobStoreIndexShardSnapshot.FileInfo> filesFromSegmentInfos = Optional.ofNullable(context.stateIdentifier())
                .map(id -> {
                    for (SnapshotFiles snapshotFileSet : snapshots.snapshots()) {
                        if (id.equals(snapshotFileSet.shardStateIdentifier())) {
                            return snapshotFileSet.indexFiles();
                        }
                    }
                    return null;
                })
                .orElse(null);

            final List<BlobStoreIndexShardSnapshot.FileInfo> indexCommitPointFiles;
            int indexIncrementalFileCount = 0;
            int indexTotalNumberOfFiles = 0;
            long indexIncrementalSize = 0;
            long indexTotalFileSize = 0;
            final BlockingQueue<BlobStoreIndexShardSnapshot.FileInfo> filesToSnapshot = new LinkedBlockingQueue<>();
            int filesInShardMetadataCount = 0;
            long filesInShardMetadataSize = 0;

            if (store.indexSettings().getIndexMetadata().isSearchableSnapshot()) {
                indexCommitPointFiles = Collections.emptyList();
            } else if (filesFromSegmentInfos == null) {
                indexCommitPointFiles = new ArrayList<>();
                final Collection<String> fileNames;
                final Store.MetadataSnapshot metadataFromStore;
                try (Releasable ignored = context.withCommitRef()) {
                    try {
                        final IndexCommit snapshotIndexCommit = context.indexCommit();
                        logger.trace("[{}] [{}] Loading store metadata using index commit [{}]", shardId, snapshotId, snapshotIndexCommit);
                        metadataFromStore = store.getMetadata(snapshotIndexCommit);
                        fileNames = snapshotIndexCommit.getFileNames();
                    } catch (IOException e) {
                        throw new IndexShardSnapshotFailedException(shardId, "Failed to get store file metadata", e);
                    }
                }
                for (String fileName : fileNames) {
                    ensureNotAborted(shardId, snapshotId, snapshotStatus, fileName);

                    logger.trace("[{}] [{}] Processing [{}]", shardId, snapshotId, fileName);
                    final StoreFileMetadata md = metadataFromStore.get(fileName);
                    BlobStoreIndexShardSnapshot.FileInfo existingFileInfo = snapshots.findPhysicalIndexFile(md);

                    final boolean needsWrite = md.hashEqualsContents() == false;
                    indexTotalFileSize += md.length();
                    indexTotalNumberOfFiles++;

                    if (existingFileInfo == null) {
                        indexIncrementalFileCount++;
                        indexIncrementalSize += md.length();
                        BlobStoreIndexShardSnapshot.FileInfo snapshotFileInfo = new BlobStoreIndexShardSnapshot.FileInfo(
                            (needsWrite ? UPLOADED_DATA_BLOB_PREFIX : VIRTUAL_DATA_BLOB_PREFIX) + UUIDs.randomBase64UUID(),
                            md,
                            chunkSize()
                        );
                        indexCommitPointFiles.add(snapshotFileInfo);
                        if (needsWrite) {
                            filesToSnapshot.add(snapshotFileInfo);
                        } else {
                            assert assertFileContentsMatchHash(snapshotStatus, snapshotFileInfo, store);
                            filesInShardMetadataCount += 1;
                            filesInShardMetadataSize += md.length();
                        }
                    } else {
                        indexCommitPointFiles.add(existingFileInfo);
                    }
                }
            } else {
                for (BlobStoreIndexShardSnapshot.FileInfo fileInfo : filesFromSegmentInfos) {
                    indexTotalNumberOfFiles++;
                    indexTotalFileSize += fileInfo.length();
                }
                indexCommitPointFiles = filesFromSegmentInfos;
            }

            snapshotStatus.moveToStarted(
                startTime,
                indexIncrementalFileCount,
                indexTotalNumberOfFiles,
                indexIncrementalSize,
                indexTotalFileSize
            );

            final ShardGeneration indexGeneration;
            final boolean writeShardGens = SnapshotsService.useShardGenerations(context.getRepositoryMetaVersion());
            final boolean writeFileInfoWriterUUID = SnapshotsService.includeFileInfoWriterUUID(context.getRepositoryMetaVersion());
            final BlobStoreIndexShardSnapshots updatedBlobStoreIndexShardSnapshots = snapshots.withAddedSnapshot(
                new SnapshotFiles(snapshotId.getName(), indexCommitPointFiles, context.stateIdentifier())
            );
            final Runnable afterWriteSnapBlob;
            if (writeShardGens) {
                indexGeneration = ShardGeneration.newGeneration();
                try {
                    final Map<String, String> serializationParams = Collections.singletonMap(
                        BlobStoreIndexShardSnapshot.FileInfo.SERIALIZE_WRITER_UUID,
                        Boolean.toString(writeFileInfoWriterUUID)
                    );
                    INDEX_SHARD_SNAPSHOTS_FORMAT.write(
                        updatedBlobStoreIndexShardSnapshots,
                        shardContainer,
                        indexGeneration.toBlobNamePart(),
                        compress,
                        serializationParams
                    );
                    snapshotStatus.addProcessedFiles(filesInShardMetadataCount, filesInShardMetadataSize);
                } catch (IOException e) {
                    throw new IndexShardSnapshotFailedException(
                        shardId,
                        "Failed to write shard level snapshot metadata for ["
                            + snapshotId
                            + "] to ["
                            + INDEX_SHARD_SNAPSHOTS_FORMAT.blobName(indexGeneration.toBlobNamePart())
                            + "]",
                        e
                    );
                }
                afterWriteSnapBlob = () -> {};
            } else {
                final long newGen = Long.parseLong(fileListGeneration.toBlobNamePart()) + 1;
                indexGeneration = new ShardGeneration(newGen);
                final List<String> blobsToDelete = blobs.stream().filter(blob -> blob.startsWith(SNAPSHOT_INDEX_PREFIX)).toList();
                assert blobsToDelete.stream()
                    .mapToLong(b -> Long.parseLong(b.replaceFirst(SNAPSHOT_INDEX_PREFIX, "")))
                    .max()
                    .orElse(-1L) < Long.parseLong(indexGeneration.toString())
                    : "Tried to delete an index-N blob newer than the current generation ["
                        + indexGeneration
                        + "] when deleting index-N blobs "
                        + blobsToDelete;
                final var finalFilesInShardMetadataCount = filesInShardMetadataCount;
                final var finalFilesInShardMetadataSize = filesInShardMetadataSize;

                afterWriteSnapBlob = () -> {
                    try {
                        final Map<String, String> serializationParams = Collections.singletonMap(
                            BlobStoreIndexShardSnapshot.FileInfo.SERIALIZE_WRITER_UUID,
                            Boolean.toString(writeFileInfoWriterUUID)
                        );
                        writeShardIndexBlobAtomic(shardContainer, newGen, updatedBlobStoreIndexShardSnapshots, serializationParams);
                    } catch (IOException e) {
                        throw new IndexShardSnapshotFailedException(
                            shardId,
                            "Failed to finalize snapshot creation ["
                                + snapshotId
                                + "] with shard index ["
                                + INDEX_SHARD_SNAPSHOTS_FORMAT.blobName(indexGeneration.toBlobNamePart())
                                + "]",
                            e
                        );
                    }
                    snapshotStatus.addProcessedFiles(finalFilesInShardMetadataCount, finalFilesInShardMetadataSize);
                    try {
                        deleteFromContainer(OperationPurpose.SNAPSHOT_METADATA, shardContainer, blobsToDelete.iterator());
                    } catch (IOException e) {
                        logger.warn(
                            () -> format("[%s][%s] failed to delete old index-N blobs during finalization", snapshotId, shardId),
                            e
                        );
                    }
                };
            }

            final AtomicReference<List<FileInfo>> fileToCleanUp = new AtomicReference<>(List.copyOf(filesToSnapshot));
            final ActionListener<Collection<Void>> allFilesUploadedListener = ActionListener.assertOnce(ActionListener.wrap(ignore -> {
                final IndexShardSnapshotStatus.Copy lastSnapshotStatus = snapshotStatus.moveToFinalize();

                logger.trace("[{}] [{}] writing shard snapshot file", shardId, snapshotId);
                final BlobStoreIndexShardSnapshot blobStoreIndexShardSnapshot = new BlobStoreIndexShardSnapshot(
                    snapshotId.getName(),
                    indexCommitPointFiles,
                    lastSnapshotStatus.getStartTime(),
                    threadPool.absoluteTimeInMillis() - lastSnapshotStatus.getStartTime(),
                    lastSnapshotStatus.getIncrementalFileCount(),
                    lastSnapshotStatus.getIncrementalSize()
                );
                fileToCleanUp.set(List.of());
                try {
                    final String snapshotUUID = snapshotId.getUUID();
                    final Map<String, String> serializationParams = Collections.singletonMap(
                        BlobStoreIndexShardSnapshot.FileInfo.SERIALIZE_WRITER_UUID,
                        Boolean.toString(writeFileInfoWriterUUID)
                    );
                    INDEX_SHARD_SNAPSHOT_FORMAT.write(
                        blobStoreIndexShardSnapshot,
                        shardContainer,
                        snapshotUUID,
                        compress,
                        serializationParams
                    );
                } catch (IOException e) {
                    throw new IndexShardSnapshotFailedException(shardId, "Failed to write commit point", e);
                }
                afterWriteSnapBlob.run();
                final ShardSnapshotResult shardSnapshotResult = new ShardSnapshotResult(
                    indexGeneration,
                    ByteSizeValue.ofBytes(blobStoreIndexShardSnapshot.totalSize()),
                    getSegmentInfoFileCount(blobStoreIndexShardSnapshot.indexFiles())
                );
                snapshotStatus.moveToDone(threadPool.absoluteTimeInMillis(), shardSnapshotResult);
                context.onResponse(shardSnapshotResult);
            }, e -> {
                try {
                    shardContainer.deleteBlobsIgnoringIfNotExists(
                        OperationPurpose.SNAPSHOT_DATA,
                        Iterators.flatMap(fileToCleanUp.get().iterator(), f -> Iterators.forRange(0, f.numberOfParts(), f::partName))
                    );
                } catch (Exception innerException) {
                    e.addSuppressed(innerException);
                }
                context.onFailure(e);
            }));

            if (indexIncrementalFileCount == 0 || filesToSnapshot.isEmpty()) {
                allFilesUploadedListener.onResponse(Collections.emptyList());
                return;
            }
            snapshotFiles(context, filesToSnapshot, allFilesUploadedListener);
        } catch (Exception e) {
            context.onFailure(e);
        }
    }

    private static void ensureNotAborted(ShardId shardId, SnapshotId snapshotId, IndexShardSnapshotStatus snapshotStatus, String fileName) {
        try {
            snapshotStatus.ensureNotAborted();
        } catch (Exception e) {
            logger.debug("[{}] [{}] {} on the file [{}], exiting", shardId, snapshotId, e.getMessage(), fileName);
            assert e instanceof AbortedSnapshotException || e instanceof PausedSnapshotException : e;
            throw e;
        }
    }

    protected void snapshotFiles(
        SnapshotShardContext context,
        BlockingQueue<FileInfo> filesToSnapshot,
        ActionListener<Collection<Void>> allFilesUploadedListener
    ) {
        final int noOfFilesToSnapshot = filesToSnapshot.size();
        final ActionListener<Void> filesListener = fileQueueListener(filesToSnapshot, noOfFilesToSnapshot, allFilesUploadedListener);
        for (int i = 0; i < noOfFilesToSnapshot; i++) {
            shardSnapshotTaskRunner.enqueueFileSnapshot(context, filesToSnapshot::poll, filesListener);
        }
    }

    private static boolean assertFileContentsMatchHash(
        IndexShardSnapshotStatus snapshotStatus,
        BlobStoreIndexShardSnapshot.FileInfo fileInfo,
        Store store
    ) {
        if (store.tryIncRef()) {
            try (IndexInput indexInput = store.openVerifyingInput(fileInfo.physicalName(), IOContext.READONCE, fileInfo.metadata())) {
                final byte[] tmp = new byte[Math.toIntExact(fileInfo.metadata().length())];
                indexInput.readBytes(tmp, 0, tmp.length);
                assert fileInfo.metadata().hash().bytesEquals(new BytesRef(tmp));
            } catch (IOException e) {
                throw new AssertionError(e);
            } finally {
                store.decRef();
            }
        } else {
            try {
                snapshotStatus.ensureNotAborted();
                assert false : "if the store is already closed we must have been aborted";
            } catch (Exception e) {
                assert e instanceof AbortedSnapshotException : e;
            }
        }
        return true;
    }

    @Override
    public void restoreShard(
        Store store,
        SnapshotId snapshotId,
        IndexId indexId,
        ShardId snapshotShardId,
        RecoveryState recoveryState,
        ActionListener<Void> listener
    ) {
        final ShardId shardId = store.shardId();
        final ActionListener<Void> restoreListener = listener.delegateResponse(
            (l, e) -> l.onFailure(new IndexShardRestoreFailedException(shardId, "failed to restore snapshot [" + snapshotId + "]", e))
        );
        final Executor executor = threadPool.executor(ThreadPool.Names.SNAPSHOT);
        final BlobContainer container = shardContainer(indexId, snapshotShardId);
        if (store.isClosing()) {
            restoreListener.onFailure(new AlreadyClosedException("store is closing"));
            return;
        }
        if (lifecycle.started() == false) {
            restoreListener.onFailure(new AlreadyClosedException("repository [" + metadata.name() + "] closed"));
            return;
        }
        if (activityRefs.tryIncRef() == false) {
            restoreListener.onFailure(new AlreadyClosedException("repository [" + metadata.name() + "] closing"));
            return;
        }
        executor.execute(ActionRunnable.wrap(ActionListener.runBefore(restoreListener, activityRefs::decRef), l -> {
            final BlobStoreIndexShardSnapshot snapshot = loadShardSnapshot(container, snapshotId);
            final SnapshotFiles snapshotFiles = new SnapshotFiles(snapshot.snapshot(), snapshot.indexFiles(), null);
            new FileRestoreContext(metadata.name(), shardId, snapshotId, recoveryState) {
                @Override
                protected void restoreFiles(
                    List<BlobStoreIndexShardSnapshot.FileInfo> filesToRecover,
                    Store store,
                    ActionListener<Void> listener
                ) {
                    if (filesToRecover.isEmpty()) {
                        listener.onResponse(null);
                    } else {
                        final int workers = Math.min(
                            threadPool.info(ThreadPool.Names.SNAPSHOT).getMax(),
                            snapshotFiles.indexFiles().size()
                        );
                        final BlockingQueue<BlobStoreIndexShardSnapshot.FileInfo> files = new LinkedBlockingQueue<>(filesToRecover);
                        final ActionListener<Void> allFilesListener = fileQueueListener(files, workers, listener.map(v -> null));
                        for (int i = 0; i < workers; ++i) {
                            try {
                                executeOneFileRestore(files, allFilesListener);
                            } catch (Exception e) {
                                allFilesListener.onFailure(e);
                            }
                        }
                    }
                }

                private void executeOneFileRestore(
                    BlockingQueue<BlobStoreIndexShardSnapshot.FileInfo> files,
                    ActionListener<Void> allFilesListener
                ) throws InterruptedException {
                    final BlobStoreIndexShardSnapshot.FileInfo fileToRecover = files.poll(0L, TimeUnit.MILLISECONDS);
                    if (fileToRecover == null) {
                        allFilesListener.onResponse(null);
                    } else {
                        executor.execute(ActionRunnable.wrap(allFilesListener, filesListener -> {
                            store.incRef();
                            try {
                                restoreFile(fileToRecover, store);
                            } finally {
                                store.decRef();
                            }
                            executeOneFileRestore(files, filesListener);
                        }));
                    }
                }

                private void restoreFile(BlobStoreIndexShardSnapshot.FileInfo fileInfo, Store store) throws IOException {
                    ensureNotClosing(store);
                    logger.trace(() -> format("[%s] restoring [%s] to [%s]", metadata.name(), fileInfo, store));
                    boolean success = false;
                    try (
                        IndexOutput indexOutput = store.createVerifyingOutput(
                            fileInfo.physicalName(),
                            fileInfo.metadata(),
                            IOContext.DEFAULT
                        )
                    ) {
                        if (fileInfo.name().startsWith(VIRTUAL_DATA_BLOB_PREFIX)) {
                            final BytesRef hash = fileInfo.metadata().hash();
                            indexOutput.writeBytes(hash.bytes, hash.offset, hash.length);
                            recoveryState.getIndex().addRecoveredBytesToFile(fileInfo.physicalName(), hash.length);
                        } else {
                            try (InputStream stream = maybeRateLimitRestores(new SlicedInputStream(fileInfo.numberOfParts()) {
                                @Override
                                protected InputStream openSlice(int slice) throws IOException {
                                    ensureNotClosing(store);
                                    return container.readBlob(OperationPurpose.SNAPSHOT_DATA, fileInfo.partName(slice));
                                }
                            })) {
                                final byte[] buffer = new byte[Math.toIntExact(Math.min(bufferSize, fileInfo.length()))];
                                int length;
                                while ((length = stream.read(buffer)) > 0) {
                                    ensureNotClosing(store);
                                    indexOutput.writeBytes(buffer, 0, length);
                                    recoveryState.getIndex().addRecoveredBytesToFile(fileInfo.physicalName(), length);
                                }
                            }
                        }
                        Store.verify(indexOutput);
                        indexOutput.close();
                        store.directory().sync(Collections.singleton(fileInfo.physicalName()));
                        success = true;
                    } catch (CorruptIndexException | IndexFormatTooOldException | IndexFormatTooNewException ex) {
                        try {
                            store.markStoreCorrupted(ex);
                        } catch (IOException e) {
                            logger.warn("store cannot be marked as corrupted", e);
                        }
                        throw ex;
                    } finally {
                        if (success == false) {
                            store.deleteQuiet(fileInfo.physicalName());
                        }
                    }
                }

                void ensureNotClosing(final Store store) throws AlreadyClosedException {
                    assert store.refCount() > 0;
                    if (store.isClosing()) {
                        throw new AlreadyClosedException("store is closing");
                    }
                    if (lifecycle.started() == false) {
                        throw new AlreadyClosedException("repository [" + metadata.name() + "] closed");
                    }
                }

            }.restore(snapshotFiles, store, l);
        }));
    }

    private static ActionListener<Void> fileQueueListener(
        BlockingQueue<BlobStoreIndexShardSnapshot.FileInfo> files,
        int numberOfFiles,
        ActionListener<Collection<Void>> listener
    ) {
        return new GroupedActionListener<>(numberOfFiles, listener).delegateResponse((l, e) -> {
            files.clear(); 
            l.onFailure(e);
        });
    }

    private static InputStream maybeRateLimit(
        InputStream stream,
        Supplier<RateLimiter> rateLimiterSupplier,
        RateLimitingInputStream.Listener throttleListener
    ) {
        return new RateLimitingInputStream(stream, rateLimiterSupplier, throttleListener);
    }

    /**
     * Wrap the restore rate limiter (controlled by the repository setting `max_restore_bytes_per_sec` and the cluster setting
     * `indices.recovery.max_bytes_per_sec`) around the given stream. Any throttling is reported to the given listener and not otherwise
     * recorded in the value returned by {@link BlobStoreRepository#getRestoreThrottleTimeInNanos}.
     */
    public InputStream maybeRateLimitRestores(InputStream stream) {
        return maybeRateLimitRestores(stream, restoreRateLimitingTimeInNanos::inc);
    }

    /**
     * Wrap the restore rate limiter (controlled by the repository setting `max_restore_bytes_per_sec` and the cluster setting
     * `indices.recovery.max_bytes_per_sec`) around the given stream. Any throttling is recorded in the value returned by {@link
     * BlobStoreRepository#getRestoreThrottleTimeInNanos}.
     */
    public InputStream maybeRateLimitRestores(InputStream stream, RateLimitingInputStream.Listener throttleListener) {
        return maybeRateLimit(
            maybeRateLimit(stream, () -> restoreRateLimiter, throttleListener),
            recoverySettings::rateLimiter,
            throttleListener
        );
    }

    /**
     * Wrap the snapshot rate limiter around the given stream. Any throttling is recorded in the value returned by
     * {@link BlobStoreRepository#getSnapshotThrottleTimeInNanos()}. Note that speed is throttled by the repository setting
     * `max_snapshot_bytes_per_sec` and, if recovery node bandwidth settings have been set, additionally by the
     * `indices.recovery.max_bytes_per_sec` speed.
     */
    public InputStream maybeRateLimitSnapshots(InputStream stream) {
        return maybeRateLimitSnapshots(stream, snapshotRateLimitingTimeInNanos::inc);
    }

    /**
     * Wrap the snapshot rate limiter around the given stream. Any throttling is recorded in the value returned by
     * {@link BlobStoreRepository#getSnapshotThrottleTimeInNanos()}. Note that speed is throttled by the repository setting
     * `max_snapshot_bytes_per_sec` and, if recovery node bandwidth settings have been set, additionally by the
     * `indices.recovery.max_bytes_per_sec` speed.
     */
    public InputStream maybeRateLimitSnapshots(InputStream stream, RateLimitingInputStream.Listener throttleListener) {
        InputStream rateLimitStream = maybeRateLimit(stream, () -> snapshotRateLimiter, throttleListener);
        if (recoverySettings.nodeBandwidthSettingsExist()) {
            rateLimitStream = maybeRateLimit(rateLimitStream, recoverySettings::rateLimiter, throttleListener);
        }
        return rateLimitStream;
    }

    @Override
    public IndexShardSnapshotStatus.Copy getShardSnapshotStatus(SnapshotId snapshotId, IndexId indexId, ShardId shardId) {
        BlobStoreIndexShardSnapshot snapshot = loadShardSnapshot(shardContainer(indexId, shardId), snapshotId);
        return IndexShardSnapshotStatus.newDone(
            snapshot.startTime(),
            snapshot.time(),
            snapshot.incrementalFileCount(),
            snapshot.totalFileCount(),
            snapshot.incrementalSize(),
            snapshot.totalSize(),
            null 
        );
    }

    @Override
    public void verify(String seed, DiscoveryNode localNode) {
        assertSnapshotOrStatelessPermittedThreadPool();
        if (isReadOnly()) {
            try {
                latestIndexBlobId();
            } catch (Exception e) {
                throw new RepositoryVerificationException(
                    metadata.name(),
                    "path " + basePath() + " is not accessible on node " + localNode,
                    e
                );
            }
        } else {
            BlobContainer testBlobContainer = blobStore().blobContainer(basePath().add(testBlobPrefix(seed)));
            try {
                testBlobContainer.writeBlob(
                    OperationPurpose.SNAPSHOT_METADATA,
                    "data-" + localNode.getId() + ".dat",
                    new BytesArray(seed),
                    true
                );
            } catch (Exception exp) {
                throw new RepositoryVerificationException(
                    metadata.name(),
                    "store location [" + blobStore() + "] is not accessible on the node [" + localNode + "]",
                    exp
                );
            }
            try (InputStream masterDat = testBlobContainer.readBlob(OperationPurpose.SNAPSHOT_METADATA, "master.dat")) {
                final String seedRead = Streams.readFully(masterDat).utf8ToString();
                if (seedRead.equals(seed) == false) {
                    throw new RepositoryVerificationException(
                        metadata.name(),
                        "Seed read from master.dat was [" + seedRead + "] but expected seed [" + seed + "]"
                    );
                }
            } catch (NoSuchFileException e) {
                throw new RepositoryVerificationException(
                    metadata.name(),
                    "a file written by master to the store ["
                        + blobStore()
                        + "] cannot be accessed on the node ["
                        + localNode
                        + "]. "
                        + "This might indicate that the store ["
                        + blobStore()
                        + "] is not shared between this node and the master node or "
                        + "that permissions on the store don't allow reading files written by the master node",
                    e
                );
            } catch (Exception e) {
                throw new RepositoryVerificationException(metadata.name(), "Failed to verify repository", e);
            }
        }
    }

    @Override
    public String toString() {
        return "BlobStoreRepository[" + "[" + metadata.name() + "], [" + blobStore.get() + ']' + ']';
    }

    /**
     * Utility for atomically writing shard level metadata to a numeric shard generation. This is only required for writing
     * numeric shard generations where atomic writes with fail-if-already-exists checks are useful in preventing repository corruption.
     */
    private void writeShardIndexBlobAtomic(
        BlobContainer shardContainer,
        long indexGeneration,
        BlobStoreIndexShardSnapshots updatedSnapshots,
        Map<String, String> serializationParams
    ) throws IOException {
        assert indexGeneration >= 0 : "Shard generation must not be negative but saw [" + indexGeneration + "]";
        logger.trace(() -> format("[%s] Writing shard index [%s] to [%s]", metadata.name(), indexGeneration, shardContainer.path()));
        final String blobName = INDEX_SHARD_SNAPSHOTS_FORMAT.blobName(String.valueOf(indexGeneration));
        writeAtomic(
            OperationPurpose.SNAPSHOT_METADATA,
            shardContainer,
            blobName,
            out -> INDEX_SHARD_SNAPSHOTS_FORMAT.serialize(updatedSnapshots, blobName, compress, serializationParams, out),
            true
        );
    }

    /**
     * Loads information about shard snapshot
     */
    public BlobStoreIndexShardSnapshot loadShardSnapshot(BlobContainer shardContainer, SnapshotId snapshotId) {
        try {
            return INDEX_SHARD_SNAPSHOT_FORMAT.read(metadata.name(), shardContainer, snapshotId.getUUID(), namedXContentRegistry);
        } catch (NoSuchFileException ex) {
            throw new SnapshotMissingException(metadata.name(), snapshotId, ex);
        } catch (IOException ex) {
            throw new SnapshotException(
                metadata.name(),
                snapshotId,
                "failed to read shard snapshot file for [" + shardContainer.path() + ']',
                ex
            );
        }
    }

    /**
     * Loads all available snapshots in the repository using the given {@code generation} for a shard. When {@code shardGen}
     * is null it tries to load it using the BwC mode, listing the available index- blobs in the shard container.
     */
    public BlobStoreIndexShardSnapshots getBlobStoreIndexShardSnapshots(IndexId indexId, int shardId, @Nullable ShardGeneration shardGen)
        throws IOException {
        final BlobContainer shardContainer = shardContainer(indexId, shardId);

        Set<String> blobs = Collections.emptySet();
        if (shardGen == null) {
            blobs = shardContainer.listBlobsByPrefix(OperationPurpose.SNAPSHOT_METADATA, INDEX_FILE_PREFIX).keySet();
        }

        return buildBlobStoreIndexShardSnapshots(blobs, shardContainer, shardGen).v1();
    }

    /**
     * Loads all available snapshots in the repository using the given {@code generation} or falling back to trying to determine it from
     * the given list of blobs in the shard container.
     *
     * @param blobs      list of blobs in repository
     * @param generation shard generation or {@code null} in case there was no shard generation tracked in the {@link RepositoryData} for
     *                   this shard because its snapshot was created in a version older than
     *                   {@link SnapshotsService#SHARD_GEN_IN_REPO_DATA_VERSION}.
     * @return tuple of BlobStoreIndexShardSnapshots and the last snapshot index generation
     */
    private Tuple<BlobStoreIndexShardSnapshots, ShardGeneration> buildBlobStoreIndexShardSnapshots(
        Set<String> blobs,
        BlobContainer shardContainer,
        @Nullable ShardGeneration generation
    ) throws IOException {
        if (generation != null) {
            if (generation.equals(ShardGenerations.NEW_SHARD_GEN)) {
                return new Tuple<>(BlobStoreIndexShardSnapshots.EMPTY, ShardGenerations.NEW_SHARD_GEN);
            }
            return new Tuple<>(
                INDEX_SHARD_SNAPSHOTS_FORMAT.read(metadata.name(), shardContainer, generation.toBlobNamePart(), namedXContentRegistry),
                generation
            );
        }
        final Tuple<BlobStoreIndexShardSnapshots, Long> legacyIndex = buildBlobStoreIndexShardSnapshots(blobs, shardContainer);
        return new Tuple<>(legacyIndex.v1(), new ShardGeneration(legacyIndex.v2()));
    }

    /**
     * Loads all available snapshots in the repository
     *
     * @param blobs list of blobs in repository
     * @return tuple of BlobStoreIndexShardSnapshots and the last snapshot index generation
     */
    private Tuple<BlobStoreIndexShardSnapshots, Long> buildBlobStoreIndexShardSnapshots(Set<String> blobs, BlobContainer shardContainer)
        throws IOException {
        long latest = latestGeneration(blobs);
        if (latest >= 0) {
            final BlobStoreIndexShardSnapshots shardSnapshots = INDEX_SHARD_SNAPSHOTS_FORMAT.read(
                metadata.name(),
                shardContainer,
                Long.toString(latest),
                namedXContentRegistry
            );
            return new Tuple<>(shardSnapshots, latest);
        } else if (blobs.stream()
            .anyMatch(b -> b.startsWith(SNAPSHOT_PREFIX) || b.startsWith(INDEX_FILE_PREFIX) || b.startsWith(UPLOADED_DATA_BLOB_PREFIX))) {
                logger.warn(
                    "Could not find a readable index-N file in a non-empty shard snapshot directory [" + shardContainer.path() + "]"
                );
            }
        return new Tuple<>(BlobStoreIndexShardSnapshots.EMPTY, latest);
    }

    /**
     * Snapshot individual file
     * @param fileInfo file to snapshot
     */
    protected void snapshotFile(SnapshotShardContext context, FileInfo fileInfo) throws IOException {
        final IndexId indexId = context.indexId();
        final Store store = context.store();
        final ShardId shardId = store.shardId();
        final IndexShardSnapshotStatus snapshotStatus = context.status();
        final SnapshotId snapshotId = context.snapshotId();
        final BlobContainer shardContainer = shardContainer(indexId, shardId);
        final String file = fileInfo.physicalName();
        try (
            Releasable ignored = context.withCommitRef();
            IndexInput indexInput = store.openVerifyingInput(file, IOContext.READONCE, fileInfo.metadata())
        ) {
            for (int i = 0; i < fileInfo.numberOfParts(); i++) {
                final long partBytes = fileInfo.partBytes(i);

                final InputStream inputStream = new FilterInputStream(
                    maybeRateLimitSnapshots(new InputStreamIndexInput(indexInput, partBytes))
                ) {
                    @Override
                    public int read() throws IOException {
                        checkAborted();
                        return super.read();
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        checkAborted();
                        return super.read(b, off, len);
                    }

                    private void checkAborted() {
                        ensureNotAborted(shardId, snapshotId, snapshotStatus, fileInfo.physicalName());
                    }
                };
                final String partName = fileInfo.partName(i);
                logger.trace("[{}] Writing [{}] to [{}]", metadata.name(), partName, shardContainer.path());
                final long startMS = threadPool.relativeTimeInMillis();
                shardContainer.writeBlob(OperationPurpose.SNAPSHOT_DATA, partName, inputStream, partBytes, false);
                logger.trace(
                    "[{}] Writing [{}] of size [{}b] to [{}] took [{}ms]",
                    metadata.name(),
                    partName,
                    partBytes,
                    shardContainer.path(),
                    threadPool.relativeTimeInMillis() - startMS
                );
            }
            Store.verify(indexInput);
            snapshotStatus.addProcessedFile(fileInfo.length());
        } catch (Exception t) {
            failStoreIfCorrupted(store, t);
            snapshotStatus.addProcessedFile(0);
            throw t;
        }
    }

    private static void failStoreIfCorrupted(Store store, Exception e) {
        if (Lucene.isCorruptionException(e)) {
            try {
                store.markStoreCorrupted((IOException) e);
            } catch (IOException inner) {
                inner.addSuppressed(e);
                logger.warn("store cannot be marked as corrupted", inner);
            }
        }
    }

    public boolean supportURLRepo() {
        return supportURLRepo;
    }

    /**
     * @return whether this repository performs overwrites atomically. In practice we only overwrite the `index.latest` blob so this
     * is not very important, but the repository analyzer does test that overwrites happen atomically. It will skip those tests if the
     * repository overrides this method to indicate that it does not support atomic overwrites.
     */
    public boolean hasAtomicOverwrites() {
        return true;
    }

    public int getReadBufferSizeInBytes() {
        return bufferSize;
    }
}
