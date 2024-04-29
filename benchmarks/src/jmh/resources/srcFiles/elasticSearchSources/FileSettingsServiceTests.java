/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.reservedstate.service;

import org.apache.lucene.tests.util.LuceneTestCase.AwaitsFix;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.NodeConnectionsService;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodeUtils;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.RerouteService;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.reservedstate.action.ReservedClusterSettingsAction;
import org.elasticsearch.tasks.TaskManager;
import org.elasticsearch.test.ClusterServiceUtils;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xcontent.XContentParser;
import org.junit.After;
import org.junit.Before;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.elasticsearch.node.Node.NODE_NAME_SETTING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@AwaitsFix(bugUrl = "https:
public class FileSettingsServiceTests extends ESTestCase {
    private Environment env;
    private ClusterService clusterService;
    private FileSettingsService fileSettingsService;
    private ReservedClusterStateService controller;
    private ThreadPool threadpool;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        threadpool = new TestThreadPool("file_settings_service_tests");

        clusterService = spy(
            new ClusterService(
                Settings.builder().put(NODE_NAME_SETTING.getKey(), "test").build(),
                new ClusterSettings(Settings.EMPTY, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS),
                threadpool,
                new TaskManager(Settings.EMPTY, threadpool, Set.of())
            )
        );

        final DiscoveryNode localNode = DiscoveryNodeUtils.create("node");
        final ClusterState clusterState = ClusterState.builder(ClusterName.DEFAULT)
            .nodes(DiscoveryNodes.builder().add(localNode).localNodeId(localNode.getId()).masterNodeId(localNode.getId()))
            .build();
        doAnswer((Answer<ClusterState>) invocation -> clusterState).when(clusterService).state();

        clusterService.setNodeConnectionsService(mock(NodeConnectionsService.class));
        clusterService.getClusterApplierService().setInitialState(clusterState);
        clusterService.getMasterService().setClusterStatePublisher((e, pl, al) -> {
            ClusterServiceUtils.setAllElapsedMillis(e);
            al.onCommit(TimeValue.ZERO);
            for (DiscoveryNode node : e.getNewState().nodes()) {
                al.onNodeAck(node, null);
            }
            pl.onResponse(null);
        });
        clusterService.getMasterService().setClusterStateSupplier(() -> clusterState);
        env = newEnvironment(Settings.EMPTY);

        Files.createDirectories(env.configFile());

        ClusterSettings clusterSettings = new ClusterSettings(Settings.EMPTY, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);

        controller = new ReservedClusterStateService(
            clusterService,
            mock(RerouteService.class),
            List.of(new ReservedClusterSettingsAction(clusterSettings))
        );
        fileSettingsService = spy(new FileSettingsService(clusterService, controller, env));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        clusterService.close();
        threadpool.shutdownNow();
    }

    public void testStartStop() {
        fileSettingsService.start();
        assertFalse(fileSettingsService.watching());
        fileSettingsService.clusterChanged(new ClusterChangedEvent("test", clusterService.state(), ClusterState.EMPTY_STATE));
        assertTrue(fileSettingsService.watching());
        fileSettingsService.stop();
        assertFalse(fileSettingsService.watching());
        fileSettingsService.close();
    }

    public void testOperatorDirName() {
        Path operatorPath = fileSettingsService.watchedFileDir();
        assertTrue(operatorPath.startsWith(env.configFile()));
        assertTrue(operatorPath.endsWith("operator"));

        Path operatorSettingsFile = fileSettingsService.watchedFile();
        assertTrue(operatorSettingsFile.startsWith(operatorPath));
        assertTrue(operatorSettingsFile.endsWith("settings.json"));
    }

    @SuppressWarnings("unchecked")
    public void testInitialFileError() throws Exception {
        ReservedClusterStateService stateService = mock(ReservedClusterStateService.class);

        doAnswer((Answer<Void>) invocation -> {
            ((Consumer<Exception>) invocation.getArgument(2)).accept(new IllegalStateException("Some exception"));
            return null;
        }).when(stateService).process(any(), (XContentParser) any(), any());

        AtomicBoolean settingsChanged = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        final FileSettingsService service = spy(new FileSettingsService(clusterService, stateService, env));

        service.addFileChangedListener(() -> settingsChanged.set(true));

        doAnswer((Answer<Void>) invocation -> {
            try {
                invocation.callRealMethod();
            } finally {
                latch.countDown();
            }
            return null;
        }).when(service).processFileChanges();

        Files.createDirectories(service.watchedFileDir());
        writeTestFile(service.watchedFile(), "{}");

        service.start();
        service.clusterChanged(new ClusterChangedEvent("test", clusterService.state(), ClusterState.EMPTY_STATE));

        assertTrue(latch.await(20, TimeUnit.SECONDS));

        verify(service, times(1)).processFileChanges();
        assertFalse(settingsChanged.get());

        service.stop();
        service.close();
    }

    @SuppressWarnings("unchecked")
    public void testInitialFileWorks() throws Exception {
        ReservedClusterStateService stateService = mock(ReservedClusterStateService.class);

        doAnswer((Answer<Void>) invocation -> {
            ((Consumer<Exception>) invocation.getArgument(2)).accept(null);
            return null;
        }).when(stateService).process(any(), (XContentParser) any(), any());

        CountDownLatch latch = new CountDownLatch(1);

        final FileSettingsService service = spy(new FileSettingsService(clusterService, stateService, env));

        service.addFileChangedListener(latch::countDown);

        Files.createDirectories(service.watchedFileDir());
        writeTestFile(service.watchedFile(), "{}");

        service.start();
        service.clusterChanged(new ClusterChangedEvent("test", clusterService.state(), ClusterState.EMPTY_STATE));

        assertTrue(latch.await(20, TimeUnit.SECONDS));

        verify(service, times(1)).processFileChanges();

        service.stop();
        service.close();
    }

    @SuppressWarnings("unchecked")
    public void testStopWorksInMiddleOfProcessing() throws Exception {
        var spiedController = spy(controller);
        var fsService = new FileSettingsService(clusterService, spiedController, env);
        FileSettingsService service = spy(fsService);

        CountDownLatch processFileLatch = new CountDownLatch(1);
        CountDownLatch deadThreadLatch = new CountDownLatch(1);

        doAnswer((Answer<ReservedStateChunk>) invocation -> {
            processFileLatch.countDown();
            new Thread(() -> {
                try {
                    deadThreadLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            return new ReservedStateChunk(Collections.emptyMap(), new ReservedStateVersion(1L, Version.CURRENT));
        }).when(spiedController).parse(any(String.class), any());

        service.start();
        service.clusterChanged(new ClusterChangedEvent("test", clusterService.state(), ClusterState.EMPTY_STATE));
        assertTrue(service.watching());

        Files.createDirectories(service.watchedFileDir());

        writeTestFile(service.watchedFile(), "{}");

        assertTrue(processFileLatch.await(30, TimeUnit.SECONDS));

        service.stop();
        assertFalse(service.watching());
        service.close();
        deadThreadLatch.countDown();
    }

    public void testStopWorksIfProcessingDidntReturnYet() throws Exception {
        var spiedController = spy(controller);
        var service = new FileSettingsService(clusterService, spiedController, env);

        CountDownLatch processFileLatch = new CountDownLatch(1);
        CountDownLatch deadThreadLatch = new CountDownLatch(1);

        doAnswer((Answer<ReservedStateChunk>) invocation -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            processFileLatch.countDown();
            new Thread(() -> {
                try {
                    deadThreadLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            return new ReservedStateChunk(Collections.emptyMap(), new ReservedStateVersion(1L, Version.CURRENT));
        }).when(spiedController).parse(any(String.class), any());

        service.start();
        service.clusterChanged(new ClusterChangedEvent("test", clusterService.state(), ClusterState.EMPTY_STATE));
        assertTrue(service.watching());

        Files.createDirectories(service.watchedFileDir());

        writeTestFile(service.watchedFile(), "{}");

        assertTrue(processFileLatch.await(30, TimeUnit.SECONDS));

        service.stop();
        assertFalse(service.watching());
        service.close();
        deadThreadLatch.countDown();
    }

    private void writeTestFile(Path path, String contents) throws IOException {
        Path tempFilePath = createTempFile();

        Files.write(tempFilePath, contents.getBytes(StandardCharsets.UTF_8));
        Files.move(tempFilePath, path, StandardCopyOption.ATOMIC_MOVE);
    }
}
