/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.monitoring.exporter.local;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexTemplateMetadata;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.time.DateFormatter;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.gateway.GatewayService;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.license.LicenseStateListener;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.protocol.xpack.watcher.DeleteWatchRequest;
import org.elasticsearch.protocol.xpack.watcher.PutWatchRequest;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.core.XPackSettings;
import org.elasticsearch.xpack.core.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.core.monitoring.exporter.MonitoringTemplateUtils;
import org.elasticsearch.xpack.core.watcher.transport.actions.delete.DeleteWatchAction;
import org.elasticsearch.xpack.core.watcher.transport.actions.get.GetWatchAction;
import org.elasticsearch.xpack.core.watcher.transport.actions.get.GetWatchRequest;
import org.elasticsearch.xpack.core.watcher.transport.actions.get.GetWatchResponse;
import org.elasticsearch.xpack.core.watcher.transport.actions.put.PutWatchAction;
import org.elasticsearch.xpack.core.watcher.watch.Watch;
import org.elasticsearch.xpack.monitoring.Monitoring;
import org.elasticsearch.xpack.monitoring.MonitoringTemplateRegistry;
import org.elasticsearch.xpack.monitoring.cleaner.CleanerService;
import org.elasticsearch.xpack.monitoring.exporter.ClusterAlertsUtil;
import org.elasticsearch.xpack.monitoring.exporter.ExportBulk;
import org.elasticsearch.xpack.monitoring.exporter.Exporter;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringMigrationCoordinator;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.elasticsearch.common.Strings.collectionToCommaDelimitedString;
import static org.elasticsearch.core.Strings.format;
import static org.elasticsearch.xpack.core.ClientHelper.MONITORING_ORIGIN;
import static org.elasticsearch.xpack.core.ClientHelper.executeAsyncWithOrigin;

public final class LocalExporter extends Exporter implements ClusterStateListener, CleanerService.Listener, LicenseStateListener {

    private static final Logger logger = LogManager.getLogger(LocalExporter.class);

    public static final String TYPE = "local";

    /**
     * Time to wait for the master node to setup local exporter for monitoring.
     * After that, the non-master nodes will warn the user for possible missing configuration.
     */
    public static final Setting.AffixSetting<TimeValue> WAIT_MASTER_TIMEOUT_SETTING = Setting.affixKeySetting(
        "xpack.monitoring.exporters.",
        "wait_master.timeout",
        (key) -> Setting.timeSetting(key, TimeValue.timeValueSeconds(30), Property.Dynamic, Property.NodeScope, Property.DeprecatedWarning),
        TYPE_DEPENDENCY
    );

    private final Client client;
    private final ClusterService clusterService;
    private final XPackLicenseState licenseState;
    private final CleanerService cleanerService;
    private final DateFormatter dateTimeFormatter;
    private final List<String> clusterAlertBlacklist;
    private final boolean decommissionClusterAlerts;
    private final MonitoringMigrationCoordinator migrationCoordinator;

    private final AtomicReference<State> state = new AtomicReference<>(State.INITIALIZED);
    private final AtomicBoolean installingSomething = new AtomicBoolean(false);
    private final AtomicBoolean watcherSetup = new AtomicBoolean(false);
    private final AtomicBoolean stateInitialized = new AtomicBoolean(false);

    private long stateInitializedTime;

    public LocalExporter(
        Exporter.Config config,
        Client client,
        MonitoringMigrationCoordinator migrationCoordinator,
        CleanerService cleanerService
    ) {
        super(config);
        this.client = client;
        this.clusterService = config.clusterService();
        this.licenseState = config.licenseState();
        this.clusterAlertBlacklist = ClusterAlertsUtil.getClusterAlertsBlacklist(config);
        this.decommissionClusterAlerts = Monitoring.MIGRATION_DECOMMISSION_ALERTS.get(config.settings());
        this.migrationCoordinator = migrationCoordinator;
        this.cleanerService = cleanerService;
        this.dateTimeFormatter = dateTimeFormatter(config);
        clusterService.addListener(this);
        cleanerService.add(this);
        licenseState.addListener(this);
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        if (event.state().blocks().hasGlobalBlock(GatewayService.STATE_NOT_RECOVERED_BLOCK) == false) {
            if (stateInitialized.getAndSet(true) == false) {
                stateInitializedTime = client.threadPool().relativeTimeInMillis();
            }
        }
        if (state.get() == State.INITIALIZED && migrationCoordinator.canInstall()) {
            resolveBulk(event.state(), true);
        }
    }

    /**
     * When the license changes, we need to ensure that Watcher is setup properly.
     */
    @Override
    public void licenseStateChanged() {
        watcherSetup.set(false);
    }

    /**
     * Determine if this {@link LocalExporter} is ready to use.
     *
     * @return {@code true} if it is ready. {@code false} if not.
     */
    public boolean isExporterReady() {
        final boolean running = resolveBulk(clusterService.state(), false) != null;
        boolean alertsProcessed = canUseWatcher() == false || watcherSetup.get();

        return running && installingSomething.get() == false && alertsProcessed;
    }

    @Override
    public void removeAlerts(Consumer<ExporterResourceStatus> listener) {
        if (state.get() == State.TERMINATED) {
            throw new IllegalStateException("Cannot refresh alerts on terminated exporter");
        }

        ClusterState clusterState = clusterService.state();
        if (clusterState.nodes().isLocalNodeElectedMaster()) {
            if (clusterState.blocks().hasGlobalBlockWithLevel(ClusterBlockLevel.METADATA_WRITE)) {
                throw new ElasticsearchException("waiting until metadata writes are unblocked");
            }

            assert migrationCoordinator.canInstall() == false : "migration attempted while resources could be erroneously installed";

            final List<Runnable> asyncActions = new ArrayList<>();
            final AtomicInteger pendingResponses = new AtomicInteger(0);
            final List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

            removeClusterAlertsTasks(clusterState, listener, asyncActions, pendingResponses, errors);
            if (asyncActions.size() > 0) {
                if (installingSomething.compareAndSet(false, true)) {
                    pendingResponses.set(asyncActions.size());
                    try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashWithOrigin(MONITORING_ORIGIN)) {
                        asyncActions.forEach(Runnable::run);
                    }
                } else {
                    throw new ElasticsearchException("exporter is busy installing resources");
                }
            } else {
                if (errors.size() > 0) {
                    listener.accept(ExporterResourceStatus.determineReadiness(name(), TYPE, errors));
                } else {
                    listener.accept(ExporterResourceStatus.ready(name(), TYPE));
                }
            }
        } else {
            throw new ElasticsearchException("Cannot refresh alerts from nodes other than currently elected master.");
        }
    }

    @Override
    public void openBulk(final ActionListener<ExportBulk> listener) {
        if (state.get() != State.RUNNING) {
            final TimeValue masterTimeout = WAIT_MASTER_TIMEOUT_SETTING.getConcreteSettingForNamespace(config.name())
                .get(config.settings());
            TimeValue timeElapsed = TimeValue.timeValueMillis(client.threadPool().relativeTimeInMillis() - stateInitializedTime);
            if (timeElapsed.compareTo(masterTimeout) > 0) {
                logger.info(
                    "waiting for elected master node [{}] to setup local exporter [{}] (does it have x-pack installed?)",
                    clusterService.state().nodes().getMasterNode(),
                    config.name()
                );
            }
            listener.onResponse(null);
        } else {
            try {
                listener.onResponse(resolveBulk(clusterService.state(), false));
            } catch (Exception e) {
                listener.onFailure(e);
            }
        }
    }

    @Override
    public void doClose() {
        if (state.getAndSet(State.TERMINATED) != State.TERMINATED) {
            logger.trace("stopped");
            clusterService.removeListener(this);
            cleanerService.remove(this);
            licenseState.removeListener(this);
        }
    }

    LocalBulk resolveBulk(ClusterState clusterState, boolean clusterStateChange) {
        if (clusterService.localNode() == null || clusterState == null) {
            return null;
        }

        boolean setup = performSetup(clusterState, clusterStateChange);

        if (setup == false) {
            return null;
        }

        if (state.compareAndSet(State.INITIALIZED, State.RUNNING)) {
            logger.debug("started");

            clusterService.removeListener(this);
        }

        return new LocalBulk(name(), logger, client, dateTimeFormatter);
    }

    /**
     * Kickstarts the set up process for the local exporter. On non leader nodes, this method is completely synchronous. On
     * the leader node, this returns immediately with a boolean stating whether the setup tasks have started. Setup tasks are
     * asynchronous. To determine exactly the outcome of setup tasks, an action listener can be passed in to be called after
     * any asynchronous operations.
     * @return true if local resources are up to date, false if they are still in progress, true on master nodes if setup has started.
     */
    private boolean performSetup(ClusterState clusterState, boolean clusterStateChange) {
        boolean setup;
        if (clusterService.state().nodes().isLocalNodeElectedMaster()) {
            setup = setupIfElectedMaster(clusterState, clusterStateChange);
        } else {
            setup = setupIfNotElectedMaster(clusterState);
        }
        return setup;
    }

    /**
     * When not on the elected master, we require all resources (mapping types, templates) to be available before we
     * attempt to run the exporter. If those resources do not exist, then it means the elected master's exporter has not yet run, so the
     * monitoring cluster (this one, as the local exporter) is not setup yet.
     *
     * @param clusterState The current cluster state.
     * @return {@code true} indicates that all resources are available and the exporter can be used. {@code false} to stop and wait.
     */
    private boolean setupIfNotElectedMaster(final ClusterState clusterState) {
        for (final String template : MonitoringTemplateRegistry.TEMPLATE_NAMES) {
            if (hasTemplate(clusterState, template) == false) {
                logger.debug("monitoring index template [{}] does not exist, so service cannot start (waiting on master)", template);
                return false;
            }
        }

        logger.trace("monitoring index templates are installed, service can start");

        return true;
    }

    /**
     * When on the elected master, we setup all resources (mapping types, templates) before we attempt to run the exporter.
     * If those resources do not exist, then we will create them.
     *
     * @param clusterState The current cluster state.
     * @param clusterStateChange {@code true} if a cluster state change caused this call (don't block it!)
     * @return {@code true} indicates that all resources are "ready" and the exporter can be used. {@code false} to stop and wait.
     */
    private boolean setupIfElectedMaster(final ClusterState clusterState, final boolean clusterStateChange) {
        if (clusterState.blocks().hasGlobalBlockWithLevel(ClusterBlockLevel.METADATA_WRITE)) {
            logger.debug("waiting until metadata writes are unblocked");
            return false;
        }

        if (migrationCoordinator.canInstall() == false) {
            logger.debug("already installing something, waiting for migration to complete");
            return false;
        }

        if (installingSomething.get()) {
            logger.trace("already installing something, waiting for install to complete");
            return false;
        }

        final List<Runnable> asyncActions = new ArrayList<>();
        final AtomicInteger pendingResponses = new AtomicInteger(0);

        final List<String> missingTemplates = Arrays.stream(MonitoringTemplateRegistry.TEMPLATE_NAMES)
            .filter(name -> hasTemplate(clusterState, name) == false)
            .toList();

        boolean templatesInstalled = false;
        if (missingTemplates.isEmpty() == false) {
            logger.debug(
                () -> format(
                    "monitoring index templates [%s] do not exist, so service " + "cannot start (waiting on registered templates)",
                    missingTemplates
                )
            );
        } else {
            templatesInstalled = true;
        }

        setupClusterAlertsTasks(clusterState, clusterStateChange, asyncActions, pendingResponses);

        if (asyncActions.size() > 0) {
            if (installingSomething.compareAndSet(false, true)) {
                pendingResponses.set(asyncActions.size());
                try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashWithOrigin(MONITORING_ORIGIN)) {
                    asyncActions.forEach(Runnable::run);
                }
            } else {
                logger.trace("already installing something, waiting for install to complete");
                return false;
            }
        } else if (templatesInstalled) {
            logger.debug("monitoring index templates are installed on master node, service can start");
        }

        return templatesInstalled;
    }

    private void setupClusterAlertsTasks(
        ClusterState clusterState,
        boolean clusterStateChange,
        List<Runnable> asyncActions,
        AtomicInteger pendingResponses
    ) {
        boolean shouldSetUpWatcher = state.get() == State.RUNNING && clusterStateChange == false;
        if (canUseWatcher()) {
            if (shouldSetUpWatcher) {
                final IndexRoutingTable watches = clusterState.routingTable().index(Watch.INDEX);
                final boolean indexExists = watches != null && watches.allPrimaryShardsActive();

                if (watches != null && watches.allPrimaryShardsActive() == false) {
                    logger.trace("cannot manage cluster alerts because [.watches] index is not allocated");
                } else if ((watches == null || indexExists) && watcherSetup.compareAndSet(false, true)) {
                    logger.trace("installing monitoring watches");
                    getClusterAlertsInstallationAsyncActions(indexExists, asyncActions, pendingResponses);
                } else {
                    logger.trace(
                        "skipping installing monitoring watches, watches=[{}], indexExists=[{}], watcherSetup=[{}]",
                        watches,
                        indexExists,
                        watcherSetup.get()
                    );
                }
            } else {
                logger.trace("watches shouldn't be setup, because state=[{}] and clusterStateChange=[{}]", state.get(), clusterStateChange);
            }
        } else {
            logger.trace(
                "watches will not be installed because xpack.watcher.enabled=[{}] and "
                    + "xpack.monitoring.exporters._local.cluster_alerts.management.enabled=[{}]",
                XPackSettings.WATCHER_ENABLED.get(config.settings()),
                CLUSTER_ALERTS_MANAGEMENT_SETTING.getConcreteSettingForNamespace(config.name()).get(config.settings())
            );
        }
    }

    private void removeClusterAlertsTasks(
        ClusterState clusterState,
        Consumer<ExporterResourceStatus> setupListener,
        List<Runnable> asyncActions,
        AtomicInteger pendingResponses,
        List<Exception> errors
    ) {
        if (canUseWatcher()) {
            if (state.get() != State.TERMINATED) {
                final IndexRoutingTable watches = clusterState.routingTable().index(Watch.INDEX);
                final boolean indexExists = watches != null && watches.allPrimaryShardsActive();

                if (watches != null && watches.allPrimaryShardsActive() == false) {
                    errors.add(new ElasticsearchException("cannot manage cluster alerts because [.watches] index is not allocated"));
                    logger.trace("cannot manage cluster alerts because [.watches] index is not allocated");
                } else if ((watches == null || indexExists) && watcherSetup.compareAndSet(false, true)) {
                    addClusterAlertsRemovalAsyncActions(indexExists, asyncActions, pendingResponses, setupListener, errors);
                }
            } else {
                errors.add(new ElasticsearchException("cannot manage cluster alerts because exporter is terminated"));
            }
        } else {
            errors.add(new ElasticsearchException("cannot manage cluster alerts because alerting is disabled"));
        }
    }

    private void responseReceived(
        final AtomicInteger pendingResponses,
        final boolean success,
        final Runnable onComplete,
        final @Nullable AtomicBoolean setup
    ) {
        if (setup != null && success == false) {
            setup.set(false);
        }

        if (pendingResponses.decrementAndGet() <= 0) {
            logger.trace("all installation requests returned a response");
            if (installingSomething.compareAndSet(true, false) == false) {
                throw new IllegalStateException("could not reset installing flag to false");
            }
            onComplete.run();
        }
    }

    private static boolean hasTemplate(final ClusterState clusterState, final String templateName) {
        final IndexTemplateMetadata template = clusterState.getMetadata().getTemplates().get(templateName);

        return template != null && hasValidVersion(template.getVersion(), MonitoringTemplateRegistry.REGISTRY_VERSION);
    }

    /**
     * Determine if the {@code version} is defined and greater than or equal to the {@code minimumVersion}.
     *
     * @param version The version to check
     * @param minimumVersion The minimum version required to be a "valid" version
     * @return {@code true} if the version exists and it's &gt;= to the minimum version. {@code false} otherwise.
     */
    private static boolean hasValidVersion(final Object version, final long minimumVersion) {
        return version instanceof Number && ((Number) version).intValue() >= minimumVersion;
    }

    /**
     * Install Cluster Alerts (Watches) into the cluster
     *
     * @param asyncActions Asynchronous actions are added to for each Watch.
     * @param pendingResponses Pending response countdown we use to track completion.
     */
    private void getClusterAlertsInstallationAsyncActions(
        final boolean indexExists,
        final List<Runnable> asyncActions,
        final AtomicInteger pendingResponses
    ) {
        final boolean canAddWatches = Monitoring.MONITORING_CLUSTER_ALERTS_FEATURE.check(licenseState);

        for (final String watchId : ClusterAlertsUtil.WATCH_IDS) {
            final String uniqueWatchId = ClusterAlertsUtil.createUniqueWatchId(clusterService, watchId);
            final boolean addWatch = canAddWatches
                && clusterAlertBlacklist.contains(watchId) == false
                && decommissionClusterAlerts == false;

            if (indexExists) {
                if (addWatch) {
                    logger.trace("checking monitoring watch [{}]", uniqueWatchId);

                    asyncActions.add(
                        () -> client.execute(
                            GetWatchAction.INSTANCE,
                            new GetWatchRequest(uniqueWatchId),
                            new GetAndPutWatchResponseActionListener(client, watchId, uniqueWatchId, pendingResponses)
                        )
                    );
                } else {
                    logger.trace("pruning monitoring watch [{}]", uniqueWatchId);

                    asyncActions.add(
                        () -> client.execute(
                            DeleteWatchAction.INSTANCE,
                            new DeleteWatchRequest(uniqueWatchId),
                            new ResponseActionListener<>("watch", uniqueWatchId, pendingResponses)
                        )
                    );
                }
            } else if (addWatch) {
                logger.trace("adding monitoring watch [{}]", uniqueWatchId);
                asyncActions.add(() -> putWatch(client, watchId, uniqueWatchId, pendingResponses));
            }
        }
    }

    /**
     * Creates actions that remove cluster alerts (watches) from the cluster
     *
     * @param indexExists True for watch index existing, false otherwise.
     * @param asyncActions Asynchronous actions are added to for each Watch.
     * @param pendingResponses Pending response countdown we use to track completion.
     * @param setupListener The listener to call with the status of the watch if there are watches to remove.
     * @param errors A list to collect errors during the watch removal process.
     */
    private void addClusterAlertsRemovalAsyncActions(
        final boolean indexExists,
        final List<Runnable> asyncActions,
        final AtomicInteger pendingResponses,
        Consumer<ExporterResourceStatus> setupListener,
        final List<Exception> errors
    ) {
        for (final String watchId : ClusterAlertsUtil.WATCH_IDS) {
            final String uniqueWatchId = ClusterAlertsUtil.createUniqueWatchId(clusterService, watchId);
            if (indexExists) {
                logger.trace("pruning monitoring watch [{}]", uniqueWatchId);
                asyncActions.add(
                    () -> client.execute(
                        DeleteWatchAction.INSTANCE,
                        new DeleteWatchRequest(uniqueWatchId),
                        new ErrorCapturingResponseListener<>("watch", uniqueWatchId, pendingResponses, setupListener, errors, this.name())
                    )
                );
            }
        }
    }

    private void putWatch(
        final Client clientToUse,
        final String watchId,
        final String uniqueWatchId,
        final AtomicInteger pendingResponses
    ) {
        final String watch = ClusterAlertsUtil.loadWatch(clusterService, watchId);

        logger.trace("adding monitoring watch [{}]", uniqueWatchId);

        executeAsyncWithOrigin(
            clientToUse,
            MONITORING_ORIGIN,
            PutWatchAction.INSTANCE,
            new PutWatchRequest(uniqueWatchId, new BytesArray(watch), XContentType.JSON),
            new ResponseActionListener<>("watch", uniqueWatchId, pendingResponses, watcherSetup)
        );
    }

    /**
     * Determine if the cluster can use Watcher.
     *
     * @return {@code true} to use Cluster Alerts.
     */
    private boolean canUseWatcher() {
        return XPackSettings.WATCHER_ENABLED.get(config.settings())
            && CLUSTER_ALERTS_MANAGEMENT_SETTING.getConcreteSettingForNamespace(config.name()).get(config.settings());
    }

    @Override
    public void onCleanUpIndices(TimeValue retention) {
        if (stateInitialized.get() == false) {
            logger.debug("exporter not yet initialized");
            return;
        }
        ClusterState clusterState = clusterService.state();
        if (clusterService.localNode() == null
            || clusterState == null
            || clusterState.blocks().hasGlobalBlockWithLevel(ClusterBlockLevel.METADATA_WRITE)) {
            logger.debug("exporter not ready");
            return;
        }

        if (clusterState.nodes().isLocalNodeElectedMaster()) {
            ZonedDateTime expiration = ZonedDateTime.now(ZoneOffset.UTC).minus(retention.millis(), ChronoUnit.MILLIS);
            logger.debug("cleaning indices [expiration={}, retention={}]", expiration, retention);

            final long expirationTimeMillis = expiration.toInstant().toEpochMilli();
            final long currentTimeMillis = System.currentTimeMillis();

            final String[] indexPatterns = new String[] { ".monitoring-*" };

            final Set<String> currents = MonitoredSystem.allSystems()
                .map(s -> MonitoringTemplateUtils.indexName(dateTimeFormatter, s, currentTimeMillis))
                .collect(Collectors.toSet());

            currents.add(MonitoringTemplateRegistry.ALERTS_INDEX_TEMPLATE_NAME);

            Set<String> indices = new HashSet<>();
            for (var index : clusterState.getMetadata().indices().entrySet()) {
                String indexName = index.getKey();

                if (Regex.simpleMatch(indexPatterns, indexName)) {
                    if (currents.contains(indexName)) {
                        continue;
                    }

                    long creationDate = index.getValue().getCreationDate();
                    if (creationDate <= expirationTimeMillis) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(
                                "detected expired index [name={}, created={}, expired={}]",
                                indexName,
                                Instant.ofEpochMilli(creationDate).atZone(ZoneOffset.UTC),
                                expiration
                            );
                        }
                        indices.add(indexName);
                    }
                }
            }

            if (indices.isEmpty() == false) {
                logger.info("cleaning up [{}] old indices", indices.size());
                deleteIndices(indices);
            } else {
                logger.debug("no old indices found for clean up");
            }
        }
    }

    private void deleteIndices(Set<String> indices) {
        logger.trace("deleting {} indices: [{}]", indices.size(), collectionToCommaDelimitedString(indices));
        final DeleteIndexRequest request = new DeleteIndexRequest(indices.toArray(new String[indices.size()]));
        executeAsyncWithOrigin(
            client.threadPool().getThreadContext(),
            MONITORING_ORIGIN,
            request,
            new ActionListener<AcknowledgedResponse>() {
                @Override
                public void onResponse(AcknowledgedResponse response) {
                    if (response.isAcknowledged()) {
                        logger.debug("{} indices deleted", indices.size());
                    } else {
                        logger.warn("deletion of {} indices wasn't acknowledged", indices.size());
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    logger.error("failed to delete indices", e);
                }
            },
            client.admin().indices()::delete
        );
    }

    enum State {
        INITIALIZED,
        RUNNING,
        TERMINATED
    }

    /**
     * Acknowledge success / failure for any given creation attempt (e.g., templates).
     */
    private class ResponseActionListener<Response> implements ActionListener<Response> {

        protected final String type;
        protected final String name;
        private final AtomicInteger countDown;
        private final Runnable onComplete;
        private final AtomicBoolean setup;

        private ResponseActionListener(String type, String name, AtomicInteger countDown) {
            this(type, name, countDown, () -> {}, null);
        }

        private ResponseActionListener(String type, String name, AtomicInteger countDown, Runnable onComplete) {
            this(type, name, countDown, onComplete, null);
        }

        private ResponseActionListener(String type, String name, AtomicInteger countDown, @Nullable AtomicBoolean setup) {
            this(type, name, countDown, () -> {}, setup);
        }

        private ResponseActionListener(
            String type,
            String name,
            AtomicInteger countDown,
            Runnable onComplete,
            @Nullable AtomicBoolean setup
        ) {
            this.type = Objects.requireNonNull(type);
            this.name = Objects.requireNonNull(name);
            this.countDown = Objects.requireNonNull(countDown);
            this.onComplete = Objects.requireNonNull(onComplete);
            this.setup = setup;
        }

        @Override
        public void onResponse(Response response) {
            if (response instanceof AcknowledgedResponse) {
                if (((AcknowledgedResponse) response).isAcknowledged()) {
                    logger.trace("successfully set monitoring {} [{}]", type, name);
                } else {
                    logger.error("failed to set monitoring {} [{}]", type, name);
                }
            } else {
                logger.trace("successfully handled monitoring {} [{}]", type, name);
            }
            responseReceived(countDown, true, onComplete, setup);
        }

        @Override
        public void onFailure(Exception e) {
            responseReceived(countDown, false, onComplete, setup);
            logger.error(() -> format("failed to set monitoring %s [%s]", type, name), e);
        }
    }

    private class ErrorCapturingResponseListener<Response> extends ResponseActionListener<Response> {
        private final List<Exception> errors;

        ErrorCapturingResponseListener(
            String type,
            String name,
            AtomicInteger countDown,
            Consumer<ExporterResourceStatus> setupListener,
            List<Exception> errors,
            String configName
        ) {
            super(type, name, countDown, () -> {
                ExporterResourceStatus status = ExporterResourceStatus.determineReadiness(configName, TYPE, errors);
                setupListener.accept(status);
            });
            this.errors = errors;
        }

        @Override
        public void onResponse(Response response) {
            if (response instanceof AcknowledgedResponse && ((AcknowledgedResponse) response).isAcknowledged() == false) {
                errors.add(new ElasticsearchException("failed to set monitoring {} [{}]", type, name));
            }
            super.onResponse(response);
        }

        @Override
        public void onFailure(Exception e) {
            errors.add(new ElasticsearchException("failed to set monitoring {} [{}]", e, type, name));
            super.onFailure(e);
        }
    }

    private class GetAndPutWatchResponseActionListener implements ActionListener<GetWatchResponse> {

        private final Client client;
        private final String watchId;
        private final String uniqueWatchId;
        private final AtomicInteger countDown;

        private GetAndPutWatchResponseActionListener(
            final Client client,
            final String watchId,
            final String uniqueWatchId,
            final AtomicInteger countDown
        ) {
            this.client = Objects.requireNonNull(client);
            this.watchId = Objects.requireNonNull(watchId);
            this.uniqueWatchId = Objects.requireNonNull(uniqueWatchId);
            this.countDown = Objects.requireNonNull(countDown);
        }

        @Override
        public void onResponse(GetWatchResponse response) {
            if (response.isFound()
                && hasValidVersion(
                    response.getSource().getValue("metadata.xpack.version_created"),
                    ClusterAlertsUtil.LAST_UPDATED_VERSION
                )) {
                logger.trace("found monitoring watch [{}]", uniqueWatchId);
                responseReceived(countDown, true, () -> {}, watcherSetup);
            } else {
                putWatch(client, watchId, uniqueWatchId, countDown);
            }
        }

        @Override
        public void onFailure(Exception e) {
            responseReceived(countDown, false, () -> {}, watcherSetup);

            if ((e instanceof IndexNotFoundException) == false) {
                logger.error((Supplier<?>) () -> "failed to get monitoring watch [" + uniqueWatchId + "]", e);
            }
        }

    }

    public static List<Setting.AffixSetting<?>> getSettings() {
        return List.of(WAIT_MASTER_TIMEOUT_SETTING);
    }

}
