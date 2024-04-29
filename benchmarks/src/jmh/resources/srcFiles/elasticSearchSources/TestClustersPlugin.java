/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.gradle.testclusters;

import org.elasticsearch.gradle.DistributionDownloadPlugin;
import org.elasticsearch.gradle.ReaperPlugin;
import org.elasticsearch.gradle.ReaperService;
import org.elasticsearch.gradle.Version;
import org.elasticsearch.gradle.transform.UnzipTransform;
import org.elasticsearch.gradle.util.GradleUtils;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.build.event.BuildEventsListenerRegistry;
import org.gradle.internal.jvm.Jvm;
import org.gradle.process.ExecOperations;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;
import org.gradle.tooling.events.task.TaskFailureResult;
import org.gradle.tooling.events.task.TaskFinishEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;

import static org.elasticsearch.gradle.util.GradleUtils.noop;

public class TestClustersPlugin implements Plugin<Project> {

    public static final Attribute<Boolean> BUNDLE_ATTRIBUTE = Attribute.of("bundle", Boolean.class);

    public static final String EXTENSION_NAME = "testClusters";
    public static final String THROTTLE_SERVICE_NAME = "testClustersThrottle";

    private static final String LIST_TASK_NAME = "listTestClusters";
    public static final String REGISTRY_SERVICE_NAME = "testClustersRegistry";
    private static final Logger logger = Logging.getLogger(TestClustersPlugin.class);
    public static final String TEST_CLUSTER_TASKS_SERVICE = "testClusterTasksService";
    private final ProviderFactory providerFactory;
    private Provider<File> runtimeJavaProvider;
    private Function<Version, Boolean> isReleasedVersion = v -> true;

    @Inject
    protected FileSystemOperations getFileSystemOperations() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected ArchiveOperations getArchiveOperations() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected ExecOperations getExecOperations() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected FileOperations getFileOperations() {
        throw new UnsupportedOperationException();
    }

    @Inject
    public TestClustersPlugin(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    public void setRuntimeJava(Provider<File> runtimeJava) {
        this.runtimeJavaProvider = runtimeJava;
    }

    public void setIsReleasedVersion(Function<Version, Boolean> isReleasedVersion) {
        this.isReleasedVersion = isReleasedVersion;
    }

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(DistributionDownloadPlugin.class);
        project.getRootProject().getPluginManager().apply(ReaperPlugin.class);
        Provider<ReaperService> reaperServiceProvider = GradleUtils.getBuildService(
            project.getGradle().getSharedServices(),
            ReaperPlugin.REAPER_SERVICE_NAME
        );
        runtimeJavaProvider = providerFactory.provider(
            () -> System.getenv("RUNTIME_JAVA_HOME") == null ? Jvm.current().getJavaHome() : new File(System.getenv("RUNTIME_JAVA_HOME"))
        );
        NamedDomainObjectContainer<ElasticsearchCluster> container = createTestClustersContainerExtension(project, reaperServiceProvider);

        createListClustersTask(project, container);

        project.getGradle().getSharedServices().registerIfAbsent(REGISTRY_SERVICE_NAME, TestClustersRegistry.class, noop());

        Provider<TestClustersThrottle> testClustersThrottleProvider = project.getGradle()
            .getSharedServices()
            .registerIfAbsent(
                THROTTLE_SERVICE_NAME,
                TestClustersThrottle.class,
                spec -> spec.getMaxParallelUsages().set(Math.max(1, project.getGradle().getStartParameter().getMaxWorkerCount() / 2))
            );

        project.getTasks().withType(TestClustersAware.class).configureEach(task -> { task.usesService(testClustersThrottleProvider); });
        project.getRootProject().getPluginManager().apply(TestClustersHookPlugin.class);
        configureArtifactTransforms(project);
    }

    private void configureArtifactTransforms(Project project) {
        project.getDependencies().getAttributesSchema().attribute(BUNDLE_ATTRIBUTE);
        project.getDependencies().getArtifactTypes().maybeCreate(ArtifactTypeDefinition.ZIP_TYPE);
        project.getDependencies().registerTransform(UnzipTransform.class, transformSpec -> {
            transformSpec.getFrom()
                .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.ZIP_TYPE)
                .attribute(BUNDLE_ATTRIBUTE, true);
            transformSpec.getTo()
                .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
                .attribute(BUNDLE_ATTRIBUTE, true);
            transformSpec.getParameters().setAsFiletreeOutput(true);
        });
    }

    private NamedDomainObjectContainer<ElasticsearchCluster> createTestClustersContainerExtension(
        Project project,
        Provider<ReaperService> reaper
    ) {
        NamedDomainObjectContainer<ElasticsearchCluster> container = project.container(
            ElasticsearchCluster.class,
            name -> new ElasticsearchCluster(
                project.getPath(),
                name,
                project,
                reaper,
                getFileSystemOperations(),
                getArchiveOperations(),
                getExecOperations(),
                getFileOperations(),
                new File(project.getBuildDir(), "testclusters"),
                runtimeJavaProvider,
                isReleasedVersion
            )
        );
        project.getExtensions().add(EXTENSION_NAME, container);
        container.configureEach(cluster -> cluster.systemProperty("ingest.geoip.downloader.enabled.default", "false"));
        return container;
    }

    private void createListClustersTask(Project project, NamedDomainObjectContainer<ElasticsearchCluster> container) {
        project.getTasks().register(LIST_TASK_NAME, task -> {
            task.setGroup("ES cluster formation");
            task.setDescription("Lists all ES clusters configured for this project");
            task.doLast(
                (Task t) -> container.forEach(cluster -> logger.lifecycle("   * {}: {}", cluster.getName(), cluster.getNumberOfNodes()))
            );
        });
    }

    static abstract class TestClustersHookPlugin implements Plugin<Project> {
        @Inject
        public abstract BuildEventsListenerRegistry getEventsListenerRegistry();

        @SuppressWarnings("checkstyle:RedundantModifier")
        @Inject
        public TestClustersHookPlugin() {}

        public void apply(Project project) {
            if (project != project.getRootProject()) {
                throw new IllegalStateException(this.getClass().getName() + " can only be applied to the root project.");
            }
            Provider<TestClustersRegistry> registryProvider = GradleUtils.getBuildService(
                project.getGradle().getSharedServices(),
                REGISTRY_SERVICE_NAME
            );

            Provider<TaskEventsService> testClusterTasksService = project.getGradle()
                .getSharedServices()
                .registerIfAbsent(TEST_CLUSTER_TASKS_SERVICE, TaskEventsService.class, spec -> {});

            TestClustersRegistry registry = registryProvider.get();
            configureClaimClustersHook(project.getGradle(), registry);

            configureStartClustersHook(project.getGradle(), registry, testClusterTasksService);

            getEventsListenerRegistry().onTaskCompletion(testClusterTasksService);
        }

        private static void configureClaimClustersHook(Gradle gradle, TestClustersRegistry registry) {
            gradle.getTaskGraph().whenReady(taskExecutionGraph -> {
                taskExecutionGraph.getAllTasks()
                    .stream()
                    .filter(task -> task instanceof TestClustersAware)
                    .map(task -> (TestClustersAware) task)
                    .flatMap(task -> task.getClusters().stream())
                    .forEach(registry::claimCluster);
            });
        }

        private void configureStartClustersHook(
            Gradle gradle,
            TestClustersRegistry registry,
            Provider<TaskEventsService> testClusterTasksService
        ) {
            testClusterTasksService.get().registry(registry);
            gradle.getTaskGraph().whenReady(taskExecutionGraph -> {
                taskExecutionGraph.getAllTasks()
                    .stream()
                    .filter(task -> task instanceof TestClustersAware)
                    .map(task -> (TestClustersAware) task)
                    .forEach(awareTask -> {
                        awareTask.doFirst(task -> {
                            awareTask.beforeStart();
                            awareTask.getClusters().forEach(awareTask.getRegistery().get()::maybeStartCluster);
                        });
                    });
            });
        }
    }

    static public abstract class TaskEventsService implements BuildService<BuildServiceParameters.None>, OperationCompletionListener {

        Map<String, TestClustersAware> tasksMap = new HashMap<>();
        private TestClustersRegistry registryProvider;

        public void register(TestClustersAware task) {
            tasksMap.put(task.getPath(), task);
        }

        public void registry(TestClustersRegistry registry) {
            this.registryProvider = registry;
        }

        @Override
        public void onFinish(FinishEvent finishEvent) {
            if (finishEvent instanceof TaskFinishEvent taskFinishEvent) {
                String taskPath = taskFinishEvent.getDescriptor().getTaskPath();
                if (tasksMap.containsKey(taskPath)) {
                    TestClustersAware task = tasksMap.get(taskPath);
                    if (task.getDidWork()) {
                        task.getClusters()
                            .forEach(
                                cluster -> registryProvider.stopCluster(cluster, taskFinishEvent.getResult() instanceof TaskFailureResult)
                            );
                    }
                }
            }
        }
    }
}
