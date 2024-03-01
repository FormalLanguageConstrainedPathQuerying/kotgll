/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ml.integration;

import org.apache.lucene.util.SetOnce;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodeRole;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.core.CheckedRunnable;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.persistent.PersistentTaskResponse;
import org.elasticsearch.persistent.PersistentTasksClusterService;
import org.elasticsearch.persistent.PersistentTasksCustomMetadata;
import org.elasticsearch.persistent.UpdatePersistentTaskStatusAction;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentParserConfiguration;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xcontent.json.JsonXContent;
import org.elasticsearch.xpack.core.action.util.QueryPage;
import org.elasticsearch.xpack.core.ml.MlConfigVersion;
import org.elasticsearch.xpack.core.ml.MlTasks;
import org.elasticsearch.xpack.core.ml.action.CloseJobAction;
import org.elasticsearch.xpack.core.ml.action.GetDatafeedsStatsAction;
import org.elasticsearch.xpack.core.ml.action.GetDatafeedsStatsAction.Response.DatafeedStats;
import org.elasticsearch.xpack.core.ml.action.GetJobsStatsAction;
import org.elasticsearch.xpack.core.ml.action.GetJobsStatsAction.Response.JobStats;
import org.elasticsearch.xpack.core.ml.action.OpenJobAction;
import org.elasticsearch.xpack.core.ml.action.PostDataAction;
import org.elasticsearch.xpack.core.ml.action.PutDatafeedAction;
import org.elasticsearch.xpack.core.ml.action.PutJobAction;
import org.elasticsearch.xpack.core.ml.action.StartDatafeedAction;
import org.elasticsearch.xpack.core.ml.action.StopDatafeedAction;
import org.elasticsearch.xpack.core.ml.action.UpdateJobAction;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedState;
import org.elasticsearch.xpack.core.ml.job.config.Job;
import org.elasticsearch.xpack.core.ml.job.config.JobState;
import org.elasticsearch.xpack.core.ml.job.config.JobUpdate;
import org.elasticsearch.xpack.core.ml.job.persistence.AnomalyDetectorsIndex;
import org.elasticsearch.xpack.core.ml.job.process.autodetect.state.DataCounts;
import org.elasticsearch.xpack.core.ml.job.process.autodetect.state.ModelSizeStats;
import org.elasticsearch.xpack.core.ml.job.process.autodetect.state.ModelSnapshot;
import org.elasticsearch.xpack.core.ml.notifications.NotificationsIndex;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.ml.job.process.autodetect.BlackHoleAutodetectProcess;
import org.elasticsearch.xpack.ml.support.BaseMlIntegTestCase;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.elasticsearch.persistent.PersistentTasksClusterService.needsReassignment;
import static org.elasticsearch.test.NodeRoles.masterOnlyNode;
import static org.elasticsearch.test.NodeRoles.onlyRole;
import static org.elasticsearch.test.NodeRoles.onlyRoles;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertCheckedResponse;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertResponse;
import static org.elasticsearch.xpack.core.ml.MlTasks.DATAFEED_TASK_NAME;
import static org.elasticsearch.xpack.core.ml.MlTasks.JOB_TASK_NAME;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class MlDistributedFailureIT extends BaseMlIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal, Settings otherSettings) {
        return Settings.builder()
            .put(super.nodeSettings(nodeOrdinal, otherSettings))
            .put(MachineLearning.CONCURRENT_JOB_ALLOCATIONS.getKey(), 4)
            .build();
    }

    public void testFailOver() throws Exception {
        internalCluster().ensureAtLeastNumDataNodes(3);
        ensureStableCluster();
        run("fail-over-job", () -> {
            GetJobsStatsAction.Request request = new GetJobsStatsAction.Request("fail-over-job");
            GetJobsStatsAction.Response response = client().execute(GetJobsStatsAction.INSTANCE, request).actionGet();
            DiscoveryNode discoveryNode = response.getResponse().results().get(0).getNode();
            internalCluster().stopNode(discoveryNode.getName());
            ensureStableCluster();
        });
    }

    @Before
    public void setLogging() {
        updateClusterSettings(Settings.builder().put("logger.org.elasticsearch.xpack.ml.utils.persistence", "TRACE"));
    }

    @After
    public void unsetLogging() {
        updateClusterSettings(Settings.builder().putNull("logger.org.elasticsearch.xpack.ml.utils.persistence"));
    }

    public void testLoseDedicatedMasterNode() throws Exception {
        internalCluster().ensureAtMostNumDataNodes(0);
        logger.info("Starting dedicated master node...");
        internalCluster().startMasterOnlyNode();
        logger.info("Starting ml and data node...");
        String mlAndDataNode = internalCluster().startNode(onlyRoles(Set.of(DiscoveryNodeRole.DATA_ROLE, DiscoveryNodeRole.ML_ROLE)));
        ensureStableCluster();
        run("lose-dedicated-master-node-job", () -> {
            logger.info("Stopping dedicated master node");
            Settings masterDataPathSettings = internalCluster().dataPathSettings(internalCluster().getMasterName());
            internalCluster().stopCurrentMasterNode();
            assertBusy(() -> {
                ClusterState state = client(mlAndDataNode).admin().cluster().prepareState().setLocal(true).get().getState();
                assertNull(state.nodes().getMasterNodeId());
            });
            logger.info("Restarting dedicated master node");
            internalCluster().startNode(Settings.builder().put(masterDataPathSettings).put(masterOnlyNode()).build());
            ensureStableCluster();
        });
    }

    public void testFullClusterRestart() throws Exception {
        internalCluster().ensureAtLeastNumDataNodes(3);
        ensureStableCluster();
        run("full-cluster-restart-job", () -> {
            logger.info("Restarting all nodes");
            internalCluster().fullRestart();
            logger.info("Restarted all nodes");
            ensureStableCluster();
        });
    }

    public void testCloseUnassignedJobAndDatafeed() throws Exception {
        internalCluster().ensureAtMostNumDataNodes(0);
        logger.info("Starting data and master node...");
        internalCluster().startNode(onlyRoles(Set.of(DiscoveryNodeRole.DATA_ROLE, DiscoveryNodeRole.MASTER_ROLE)));
        logger.info("Starting ml and data node...");
        internalCluster().startNode(onlyRoles(Set.of(DiscoveryNodeRole.DATA_ROLE, DiscoveryNodeRole.ML_ROLE)));
        ensureStableCluster();

        client().admin().indices().prepareCreate("data").setMapping("time", "type=date").get();
        long numDocs1 = randomIntBetween(32, 2048);
        long now = System.currentTimeMillis();
        long weekAgo = now - 604800000;
        long twoWeeksAgo = weekAgo - 604800000;
        indexDocs(logger, "data", numDocs1, twoWeeksAgo, weekAgo);

        String jobId = "test-lose-ml-node";
        String datafeedId = jobId + "-datafeed";
        setupJobAndDatafeed(jobId, datafeedId, TimeValue.timeValueHours(1));
        waitForJobToHaveProcessedExactly(jobId, numDocs1);

        ensureGreen(); 
        internalCluster().stopRandomNonMasterNode();
        ensureStableCluster(1);

        GetJobsStatsAction.Request jobStatsRequest = new GetJobsStatsAction.Request(jobId);
        GetJobsStatsAction.Response jobStatsResponse = client().execute(GetJobsStatsAction.INSTANCE, jobStatsRequest).actionGet();
        assertEquals(JobState.OPENED, jobStatsResponse.getResponse().results().get(0).getState());

        GetDatafeedsStatsAction.Request datafeedStatsRequest = new GetDatafeedsStatsAction.Request(datafeedId);
        GetDatafeedsStatsAction.Response datafeedStatsResponse = client().execute(GetDatafeedsStatsAction.INSTANCE, datafeedStatsRequest)
            .actionGet();
        assertEquals(DatafeedState.STARTED, datafeedStatsResponse.getResponse().results().get(0).getDatafeedState());

        StopDatafeedAction.Request stopDatafeedRequest = new StopDatafeedAction.Request(datafeedId);
        stopDatafeedRequest.setForce(randomBoolean());
        StopDatafeedAction.Response stopDatafeedResponse = client().execute(StopDatafeedAction.INSTANCE, stopDatafeedRequest).actionGet();
        assertTrue(stopDatafeedResponse.isStopped());

        CloseJobAction.Request closeJobRequest = new CloseJobAction.Request(jobId);
        boolean closeWithForce = randomBoolean();
        closeJobRequest.setForce(closeWithForce);
        CloseJobAction.Response closeJobResponse = client().execute(CloseJobAction.INSTANCE, closeJobRequest).actionGet();
        assertTrue(closeJobResponse.isClosed());

        SearchRequest datafeedAuditSearchRequest = new SearchRequest(NotificationsIndex.NOTIFICATIONS_INDEX);
        datafeedAuditSearchRequest.source().query(new TermsQueryBuilder("message.raw", "Datafeed stopped"));
        assertBusy(() -> {
            assertTrue(indexExists(NotificationsIndex.NOTIFICATIONS_INDEX));
            assertResponse(client().search(datafeedAuditSearchRequest), searchResponse -> {
                assertThat(searchResponse.getHits(), notNullValue());
                assertThat(searchResponse.getHits().getHits(), arrayWithSize(1));
                assertThat(searchResponse.getHits().getHits()[0].getSourceAsMap().get("job_id"), is(jobId));
            });
        });

        String expectedAuditMessage = closeWithForce ? "Job is closing (forced)" : "Job is closing";
        SearchRequest jobAuditSearchRequest = new SearchRequest(NotificationsIndex.NOTIFICATIONS_INDEX);
        jobAuditSearchRequest.source().query(new TermsQueryBuilder("message.raw", expectedAuditMessage));
        assertBusy(() -> {
            assertTrue(indexExists(NotificationsIndex.NOTIFICATIONS_INDEX));
            assertResponse(client().search(jobAuditSearchRequest), searchResponse -> {
                assertThat(searchResponse.getHits(), notNullValue());
                assertThat(searchResponse.getHits().getHits(), arrayWithSize(1));
                assertThat(searchResponse.getHits().getHits()[0].getSourceAsMap().get("job_id"), is(jobId));
            });
        });
    }

    public void testCloseUnassignedFailedJobAndStopUnassignedStoppingDatafeed() throws Exception {
        internalCluster().ensureAtMostNumDataNodes(0);
        logger.info("Starting master/data nodes...");
        for (int count = 0; count < 3; ++count) {
            internalCluster().startNode(onlyRoles(Set.of(DiscoveryNodeRole.DATA_ROLE, DiscoveryNodeRole.MASTER_ROLE)));
        }
        logger.info("Starting dedicated ml node...");
        internalCluster().startNode(onlyRole(DiscoveryNodeRole.ML_ROLE));
        ensureStableCluster();

        client().admin().indices().prepareCreate("data").setMapping("time", "type=date").get();
        long numDocs1 = randomIntBetween(32, 2048);
        long now = System.currentTimeMillis();
        long weekAgo = now - 604800000;
        long twoWeeksAgo = weekAgo - 604800000;
        indexDocs(logger, "data", numDocs1, twoWeeksAgo, weekAgo);

        String jobId = "test-stop-unassigned-datafeed-for-failed-job";
        String datafeedId = jobId + "-datafeed";
        setupJobAndDatafeed(jobId, datafeedId, TimeValue.timeValueHours(1));
        waitForJobToHaveProcessedExactly(jobId, numDocs1);

        GetJobsStatsAction.Request jobStatsRequest = new GetJobsStatsAction.Request(jobId);
        GetJobsStatsAction.Response jobStatsResponse = client().execute(GetJobsStatsAction.INSTANCE, jobStatsRequest).actionGet();
        assertEquals(JobState.OPENED, jobStatsResponse.getResponse().results().get(0).getState());
        DiscoveryNode jobNode = jobStatsResponse.getResponse().results().get(0).getNode();

        PostDataAction.Request postDataRequest = new PostDataAction.Request(jobId);
        postDataRequest.setContent(
            new BytesArray("{ \"time\" : \"" + BlackHoleAutodetectProcess.MAGIC_FAILURE_VALUE_AS_DATE + "\" }"),
            XContentType.JSON
        );
        PostDataAction.Response postDataResponse = client().execute(PostDataAction.INSTANCE, postDataRequest).actionGet();
        assertEquals(1L, postDataResponse.getDataCounts().getInputRecordCount());

        assertBusy(() -> {
            GetJobsStatsAction.Request jobStatsRequest2 = new GetJobsStatsAction.Request(jobId);
            GetJobsStatsAction.Response jobStatsResponse2 = client().execute(GetJobsStatsAction.INSTANCE, jobStatsRequest2).actionGet();
            assertEquals(JobState.FAILED, jobStatsResponse2.getResponse().results().get(0).getState());
        });

        PersistentTasksCustomMetadata tasks = clusterService().state().getMetadata().custom(PersistentTasksCustomMetadata.TYPE);
        PersistentTasksCustomMetadata.PersistentTask<?> task = MlTasks.getDatafeedTask(datafeedId, tasks);

        if (task == null) {
            CloseJobAction.Request closeJobRequest = new CloseJobAction.Request(jobId);
            closeJobRequest.setForce(true);
            client().execute(CloseJobAction.INSTANCE, closeJobRequest).actionGet();
            assumeFalse(
                "The datafeed task is null most likely because the datafeed detected the job had failed. "
                    + "This is expected to happen extremely rarely but the test cannot continue in these circumstances.",
                task == null
            );
        }

        UpdatePersistentTaskStatusAction.Request updatePersistentTaskStatusRequest = new UpdatePersistentTaskStatusAction.Request(
            task.getId(),
            task.getAllocationId(),
            DatafeedState.STOPPING
        );
        PersistentTaskResponse updatePersistentTaskStatusResponse = client().execute(
            UpdatePersistentTaskStatusAction.INSTANCE,
            updatePersistentTaskStatusRequest
        ).actionGet();
        assertNotNull(updatePersistentTaskStatusResponse.getTask());

        assertBusy(() -> {
            GetDatafeedsStatsAction.Request datafeedStatsRequest = new GetDatafeedsStatsAction.Request(datafeedId);
            GetDatafeedsStatsAction.Response datafeedStatsResponse = client().execute(
                GetDatafeedsStatsAction.INSTANCE,
                datafeedStatsRequest
            ).actionGet();
            assertEquals(DatafeedState.STOPPING, datafeedStatsResponse.getResponse().results().get(0).getDatafeedState());
        });

        ensureGreen(); 
        internalCluster().stopNode(jobNode.getName());
        ensureStableCluster(3);

        StopDatafeedAction.Request stopDatafeedRequest = new StopDatafeedAction.Request(datafeedId);
        stopDatafeedRequest.setForce(true);
        StopDatafeedAction.Response stopDatafeedResponse = client().execute(StopDatafeedAction.INSTANCE, stopDatafeedRequest).actionGet();
        assertTrue(stopDatafeedResponse.isStopped());

        GetDatafeedsStatsAction.Request datafeedStatsRequest2 = new GetDatafeedsStatsAction.Request(datafeedId);
        GetDatafeedsStatsAction.Response datafeedStatsResponse2 = client().execute(GetDatafeedsStatsAction.INSTANCE, datafeedStatsRequest2)
            .actionGet();
        assertEquals(DatafeedState.STOPPED, datafeedStatsResponse2.getResponse().results().get(0).getDatafeedState());

        CloseJobAction.Request closeJobRequest = new CloseJobAction.Request(jobId);
        closeJobRequest.setForce(true);
        CloseJobAction.Response closeJobResponse = client().execute(CloseJobAction.INSTANCE, closeJobRequest).actionGet();
        assertTrue(closeJobResponse.isClosed());
    }

    public void testStopAndForceStopDatafeed() throws Exception {
        internalCluster().ensureAtMostNumDataNodes(0);
        logger.info("Starting dedicated master node...");
        internalCluster().startMasterOnlyNode();
        logger.info("Starting ml and data node...");
        internalCluster().startNode(onlyRoles(Set.of(DiscoveryNodeRole.DATA_ROLE, DiscoveryNodeRole.ML_ROLE)));
        ensureStableCluster();

        client().admin().indices().prepareCreate("data").setMapping("time", "type=date").get();
        long numDocs1 = randomIntBetween(32, 2048);
        long now = System.currentTimeMillis();
        long weekAgo = now - 604800000;
        long twoWeeksAgo = weekAgo - 604800000;
        indexDocs(logger, "data", numDocs1, twoWeeksAgo, weekAgo);

        String jobId = "test-stop-and-force-stop";
        String datafeedId = jobId + "-datafeed";
        setupJobAndDatafeed(jobId, datafeedId, TimeValue.timeValueHours(1));
        waitForJobToHaveProcessedExactly(jobId, numDocs1);

        GetDatafeedsStatsAction.Request datafeedStatsRequest = new GetDatafeedsStatsAction.Request(datafeedId);
        GetDatafeedsStatsAction.Response datafeedStatsResponse = client().execute(GetDatafeedsStatsAction.INSTANCE, datafeedStatsRequest)
            .actionGet();
        assertEquals(DatafeedState.STARTED, datafeedStatsResponse.getResponse().results().get(0).getDatafeedState());

        StopDatafeedAction.Request stopDatafeedRequest = new StopDatafeedAction.Request(datafeedId);
        ActionFuture<StopDatafeedAction.Response> normalStopActionFuture = client().execute(
            StopDatafeedAction.INSTANCE,
            stopDatafeedRequest
        );

        stopDatafeedRequest = new StopDatafeedAction.Request(datafeedId);
        stopDatafeedRequest.setForce(true);
        StopDatafeedAction.Response stopDatafeedResponse = client().execute(StopDatafeedAction.INSTANCE, stopDatafeedRequest).actionGet();
        assertTrue(stopDatafeedResponse.isStopped());

        stopDatafeedResponse = normalStopActionFuture.actionGet();
        assertTrue(stopDatafeedResponse.isStopped());

        CloseJobAction.Request closeJobRequest = new CloseJobAction.Request(jobId);
        CloseJobAction.Response closeJobResponse = client().execute(CloseJobAction.INSTANCE, closeJobRequest).actionGet();
        assertTrue(closeJobResponse.isClosed());
    }

    public void testJobRelocationIsMemoryAware() throws Exception {
        internalCluster().ensureAtLeastNumDataNodes(1);
        ensureStableCluster();


        setupJobWithoutDatafeed("small1", ByteSizeValue.ofMb(2));
        setupJobWithoutDatafeed("small2", ByteSizeValue.ofMb(2));
        setupJobWithoutDatafeed("small3", ByteSizeValue.ofMb(2));
        setupJobWithoutDatafeed("small4", ByteSizeValue.ofMb(2));


        internalCluster().ensureAtLeastNumDataNodes(3);
        ensureStableCluster();


        ensureGreen();


        setupJobWithoutDatafeed("big1", ByteSizeValue.ofMb(500));


        internalCluster().stopCurrentMasterNode();
        ensureStableCluster();

        PersistentTasksClusterService persistentTasksClusterService = internalCluster().getInstance(
            PersistentTasksClusterService.class,
            internalCluster().getMasterName()
        );
        persistentTasksClusterService.setRecheckInterval(TimeValue.timeValueMillis(200));


        assertBusy(() -> {
            GetJobsStatsAction.Response statsResponse = client().execute(
                GetJobsStatsAction.INSTANCE,
                new GetJobsStatsAction.Request(Metadata.ALL)
            ).actionGet();
            QueryPage<JobStats> jobStats = statsResponse.getResponse();
            assertNotNull(jobStats);
            List<String> smallJobNodes = jobStats.results()
                .stream()
                .filter(s -> s.getJobId().startsWith("small") && s.getNode() != null)
                .map(s -> s.getNode().getName())
                .collect(Collectors.toList());
            List<String> bigJobNodes = jobStats.results()
                .stream()
                .filter(s -> s.getJobId().startsWith("big") && s.getNode() != null)
                .map(s -> s.getNode().getName())
                .collect(Collectors.toList());
            logger.info("small job nodes: " + smallJobNodes + ", big job nodes: " + bigJobNodes);
            assertEquals(5, jobStats.count());
            assertEquals(4, smallJobNodes.size());
            assertEquals(1, bigJobNodes.size());
            assertEquals(1L, smallJobNodes.stream().distinct().count());
            assertEquals(1L, bigJobNodes.stream().distinct().count());
            assertNotEquals(smallJobNodes, bigJobNodes);
        });
    }

    public void testClusterWithTwoMlNodes_RunsDatafeed_GivenOriginalNodeGoesDown() throws Exception {
        internalCluster().ensureAtMostNumDataNodes(0);
        logger.info("Starting dedicated master node...");
        internalCluster().startMasterOnlyNode();
        logger.info("Starting ml and data node...");
        internalCluster().startNode(onlyRoles(Set.of(DiscoveryNodeRole.DATA_ROLE, DiscoveryNodeRole.ML_ROLE)));
        logger.info("Starting another ml and data node...");
        internalCluster().startNode(onlyRoles(Set.of(DiscoveryNodeRole.DATA_ROLE, DiscoveryNodeRole.ML_ROLE)));
        ensureStableCluster();

        client().admin().indices().prepareCreate("data").setMapping("time", "type=date").get();
        long numDocs = 80000;
        long now = System.currentTimeMillis();
        long weekAgo = now - 604800000;
        long twoWeeksAgo = weekAgo - 604800000;
        indexDocs(logger, "data", numDocs, twoWeeksAgo, weekAgo);

        String jobId = "test-node-goes-down-while-running-job";
        String datafeedId = jobId + "-datafeed";

        Job.Builder job = createScheduledJob(jobId);
        PutJobAction.Request putJobRequest = new PutJobAction.Request(job);
        client().execute(PutJobAction.INSTANCE, putJobRequest).actionGet();

        DatafeedConfig config = createDatafeed(datafeedId, job.getId(), Collections.singletonList("data"), TimeValue.timeValueHours(1));
        PutDatafeedAction.Request putDatafeedRequest = new PutDatafeedAction.Request(config);
        client().execute(PutDatafeedAction.INSTANCE, putDatafeedRequest).actionGet();

        client().execute(OpenJobAction.INSTANCE, new OpenJobAction.Request(job.getId()));

        assertBusy(() -> {
            GetJobsStatsAction.Response statsResponse = client().execute(
                GetJobsStatsAction.INSTANCE,
                new GetJobsStatsAction.Request(job.getId())
            ).actionGet();
            assertEquals(JobState.OPENED, statsResponse.getResponse().results().get(0).getState());
        }, 30, TimeUnit.SECONDS);

        DiscoveryNode nodeRunningJob = client().execute(GetJobsStatsAction.INSTANCE, new GetJobsStatsAction.Request(job.getId()))
            .actionGet()
            .getResponse()
            .results()
            .get(0)
            .getNode();

        setMlIndicesDelayedNodeLeftTimeoutToZero();

        StartDatafeedAction.Request startDatafeedRequest = new StartDatafeedAction.Request(config.getId(), 0L);
        client().execute(StartDatafeedAction.INSTANCE, startDatafeedRequest).get();

        waitForJobToHaveProcessedAtLeast(jobId, 1000);

        internalCluster().stopNode(nodeRunningJob.getName());

        assertBusy(() -> {
            assertThat(getJobStats(jobId).getNode(), is(not(nullValue())));
            assertThat(getDatafeedStats(datafeedId).getNode(), is(not(nullValue())));
        }, 30, TimeUnit.SECONDS);

        assertBusy(() -> {
            DataCounts dataCounts = getJobStats(jobId).getDataCounts();
            assertThat(dataCounts.getProcessedRecordCount(), equalTo(numDocs));
            assertThat(dataCounts.getOutOfOrderTimeStampCount(), equalTo(0L));
        });
    }

    @AwaitsFix(bugUrl = "https:
    public void testClusterWithTwoMlNodes_StopsDatafeed_GivenJobFailsOnReassign() throws Exception {
        internalCluster().ensureAtMostNumDataNodes(0);
        logger.info("Starting dedicated master node...");
        internalCluster().startMasterOnlyNode();
        logger.info("Starting ml and data node...");
        internalCluster().startNode(onlyRoles(Set.of(DiscoveryNodeRole.DATA_ROLE, DiscoveryNodeRole.ML_ROLE)));
        logger.info("Starting another ml and data node...");
        internalCluster().startNode(onlyRoles(Set.of(DiscoveryNodeRole.DATA_ROLE, DiscoveryNodeRole.ML_ROLE)));
        ensureStableCluster();

        client().admin().indices().prepareCreate("data").setMapping("time", "type=date").get();
        long numDocs = 80000;
        long now = System.currentTimeMillis();
        long weekAgo = now - 604800000;
        long twoWeeksAgo = weekAgo - 604800000;
        indexDocs(logger, "data", numDocs, twoWeeksAgo, weekAgo);

        String jobId = "test-node-goes-down-while-running-job";
        String datafeedId = jobId + "-datafeed";

        Job.Builder job = createScheduledJob(jobId);
        PutJobAction.Request putJobRequest = new PutJobAction.Request(job);
        client().execute(PutJobAction.INSTANCE, putJobRequest).actionGet();

        DatafeedConfig config = createDatafeed(datafeedId, job.getId(), Collections.singletonList("data"), TimeValue.timeValueHours(1));
        PutDatafeedAction.Request putDatafeedRequest = new PutDatafeedAction.Request(config);
        client().execute(PutDatafeedAction.INSTANCE, putDatafeedRequest).actionGet();

        client().execute(OpenJobAction.INSTANCE, new OpenJobAction.Request(job.getId()));

        assertBusy(() -> {
            GetJobsStatsAction.Response statsResponse = client().execute(
                GetJobsStatsAction.INSTANCE,
                new GetJobsStatsAction.Request(job.getId())
            ).actionGet();
            assertEquals(JobState.OPENED, statsResponse.getResponse().results().get(0).getState());
        }, 30, TimeUnit.SECONDS);

        DiscoveryNode nodeRunningJob = client().execute(GetJobsStatsAction.INSTANCE, new GetJobsStatsAction.Request(job.getId()))
            .actionGet()
            .getResponse()
            .results()
            .get(0)
            .getNode();

        setMlIndicesDelayedNodeLeftTimeoutToZero();

        StartDatafeedAction.Request startDatafeedRequest = new StartDatafeedAction.Request(config.getId(), 0L);
        client().execute(StartDatafeedAction.INSTANCE, startDatafeedRequest).get();

        waitForJobToHaveProcessedAtLeast(jobId, 1000);

        assertBusy(() -> {
            GetDatafeedsStatsAction.Response statsResponse = client().execute(
                GetDatafeedsStatsAction.INSTANCE,
                new GetDatafeedsStatsAction.Request(config.getId())
            ).actionGet();
            assertEquals(DatafeedState.STARTED, statsResponse.getResponse().results().get(0).getDatafeedState());
        }, 30, TimeUnit.SECONDS);

        String snapshotId = "123";
        ModelSnapshot modelSnapshot = new ModelSnapshot.Builder(jobId).setSnapshotId(snapshotId).setTimestamp(new Date()).build();
        JobResultsPersister jobResultsPersister = internalCluster().getInstance(
            JobResultsPersister.class,
            internalCluster().getMasterName()
        );
        jobResultsPersister.persistModelSnapshot(modelSnapshot, WriteRequest.RefreshPolicy.IMMEDIATE, () -> true);
        UpdateJobAction.Request updateJobRequest = UpdateJobAction.Request.internal(
            jobId,
            new JobUpdate.Builder(jobId).setModelSnapshotId(snapshotId).build()
        );
        client().execute(UpdateJobAction.INSTANCE, updateJobRequest).actionGet();
        refresh(AnomalyDetectorsIndex.resultsWriteAlias(jobId));

        internalCluster().stopNode(nodeRunningJob.getName());

        assertBusy(() -> {
            GetJobsStatsAction.Response statsResponse = client().execute(
                GetJobsStatsAction.INSTANCE,
                new GetJobsStatsAction.Request(job.getId())
            ).actionGet();
            assertEquals(JobState.FAILED, statsResponse.getResponse().results().get(0).getState());
        }, 30, TimeUnit.SECONDS);

        assertBusy(() -> {
            GetDatafeedsStatsAction.Response statsResponse = client().execute(
                GetDatafeedsStatsAction.INSTANCE,
                new GetDatafeedsStatsAction.Request(config.getId())
            ).actionGet();
            assertEquals(DatafeedState.STOPPED, statsResponse.getResponse().results().get(0).getDatafeedState());
        }, 30, TimeUnit.SECONDS);

        client().execute(CloseJobAction.INSTANCE, new CloseJobAction.Request(jobId).setForce(true)).actionGet();
    }

    private void setupJobWithoutDatafeed(String jobId, ByteSizeValue modelMemoryLimit) throws Exception {
        Job.Builder job = createFareQuoteJob(jobId, modelMemoryLimit);
        PutJobAction.Request putJobRequest = new PutJobAction.Request(job);
        client().execute(PutJobAction.INSTANCE, putJobRequest).actionGet();

        client().execute(OpenJobAction.INSTANCE, new OpenJobAction.Request(job.getId())).actionGet();
        assertBusy(() -> {
            GetJobsStatsAction.Response statsResponse = client().execute(
                GetJobsStatsAction.INSTANCE,
                new GetJobsStatsAction.Request(job.getId())
            ).actionGet();
            assertEquals(JobState.OPENED, statsResponse.getResponse().results().get(0).getState());
        });
    }

    private void setupJobAndDatafeed(String jobId, String datafeedId, TimeValue datafeedFrequency) throws Exception {
        Job.Builder job = createScheduledJob(jobId);
        PutJobAction.Request putJobRequest = new PutJobAction.Request(job);
        client().execute(PutJobAction.INSTANCE, putJobRequest).actionGet();

        DatafeedConfig config = createDatafeed(datafeedId, job.getId(), Collections.singletonList("data"), datafeedFrequency);
        PutDatafeedAction.Request putDatafeedRequest = new PutDatafeedAction.Request(config);
        client().execute(PutDatafeedAction.INSTANCE, putDatafeedRequest).actionGet();

        client().execute(OpenJobAction.INSTANCE, new OpenJobAction.Request(job.getId()));
        assertBusy(() -> {
            GetJobsStatsAction.Response statsResponse = client().execute(
                GetJobsStatsAction.INSTANCE,
                new GetJobsStatsAction.Request(job.getId())
            ).actionGet();
            assertEquals(JobState.OPENED, statsResponse.getResponse().results().get(0).getState());
        }, 30, TimeUnit.SECONDS);

        setMlIndicesDelayedNodeLeftTimeoutToZero();

        StartDatafeedAction.Request startDatafeedRequest = new StartDatafeedAction.Request(config.getId(), 0L);
        client().execute(StartDatafeedAction.INSTANCE, startDatafeedRequest).get();
    }

    private void run(String jobId, CheckedRunnable<Exception> disrupt) throws Exception {
        client().admin().indices().prepareCreate("data").setMapping("time", "type=date").get();
        long numDocs1 = randomIntBetween(32, 2048);
        long now = System.currentTimeMillis();
        long weekAgo = now - 604800000;
        long twoWeeksAgo = weekAgo - 604800000;
        indexDocs(logger, "data", numDocs1, twoWeeksAgo, weekAgo);

        setupJobAndDatafeed(jobId, "data_feed_id", TimeValue.timeValueSeconds(1));
        waitForJobToHaveProcessedExactly(jobId, numDocs1);

        indexModelSnapshotFromCurrentJobStats(jobId);

        client().admin().indices().prepareFlush().get();

        disrupt.run();

        PersistentTasksClusterService persistentTasksClusterService = internalCluster().getInstance(
            PersistentTasksClusterService.class,
            internalCluster().getMasterName()
        );
        persistentTasksClusterService.setRecheckInterval(TimeValue.timeValueMillis(200));

        awaitClusterState(state -> {
            List<PersistentTasksCustomMetadata.PersistentTask<?>> tasks = findTasks(state, Set.of(DATAFEED_TASK_NAME, JOB_TASK_NAME));
            if (tasks == null || tasks.size() != 2) {
                return false;
            }
            for (PersistentTasksCustomMetadata.PersistentTask<?> task : tasks) {
                if (needsReassignment(task.getAssignment(), state.nodes())) {
                    return false;
                }
            }
            return true;
        });
        assertBusy(() -> {
            GetJobsStatsAction.Request jobStatsRequest = new GetJobsStatsAction.Request(jobId);
            JobStats jobStats = client().execute(GetJobsStatsAction.INSTANCE, jobStatsRequest).actionGet().getResponse().results().get(0);
            assertEquals(JobState.OPENED, jobStats.getState());
            assertNotNull(jobStats.getNode());

            GetDatafeedsStatsAction.Request datafeedStatsRequest = new GetDatafeedsStatsAction.Request("data_feed_id");
            DatafeedStats datafeedStats = client().execute(GetDatafeedsStatsAction.INSTANCE, datafeedStatsRequest)
                .actionGet()
                .getResponse()
                .results()
                .get(0);
            assertEquals(DatafeedState.STARTED, datafeedStats.getDatafeedState());
            assertNotNull(datafeedStats.getNode());
        }, 20, TimeUnit.SECONDS);

        long numDocs2 = randomIntBetween(2, 64);
        long now2 = System.currentTimeMillis();
        indexDocs(logger, "data", numDocs2, now2 + 5000, now2 + 6000);
        waitForJobToHaveProcessedExactly(jobId, numDocs1 + numDocs2);
    }

    private static DataCounts getDataCountsFromIndex(String jobId) throws IOException {
        SetOnce<DataCounts> setOnce = new SetOnce<>();
        assertCheckedResponse(
            prepareSearch().setIndicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN_CLOSED_HIDDEN)
                .setQuery(QueryBuilders.idsQuery().addIds(DataCounts.documentId(jobId))),
            searchResponse -> {
                if (searchResponse.getHits().getTotalHits().value != 1) {
                    setOnce.set(new DataCounts(jobId));
                    return;
                }
                BytesReference source = searchResponse.getHits().getHits()[0].getSourceRef();
                try (XContentParser parser = XContentHelper.createParser(XContentParserConfiguration.EMPTY, source, XContentType.JSON)) {
                    setOnce.set(DataCounts.PARSER.apply(parser, null));
                }
            }
        );
        return setOnce.get();
    }

    private void waitForJobToHaveProcessedExactly(String jobId, long numDocs) throws Exception {
        assertBusy(() -> {
            DataCounts dataCounts = getDataCountsFromIndex(jobId);
            assertEquals(numDocs, dataCounts.getProcessedRecordCount());
            assertEquals(0L, dataCounts.getOutOfOrderTimeStampCount());
        }, 30, TimeUnit.SECONDS);
    }

    private void waitForJobToHaveProcessedAtLeast(String jobId, long numDocs) throws Exception {
        assertBusy(() -> {
            DataCounts dataCounts = getDataCountsFromIndex(jobId);
            assertThat(dataCounts.getProcessedRecordCount(), greaterThanOrEqualTo(numDocs));
            assertEquals(0L, dataCounts.getOutOfOrderTimeStampCount());
        }, 30, TimeUnit.SECONDS);
    }

    private void indexModelSnapshotFromCurrentJobStats(String jobId) throws IOException {
        JobStats jobStats = getJobStats(jobId);
        DataCounts dataCounts = jobStats.getDataCounts();

        ModelSnapshot modelSnapshot = new ModelSnapshot.Builder(jobId).setLatestResultTimeStamp(dataCounts.getLatestRecordTimeStamp())
            .setLatestRecordTimeStamp(dataCounts.getLatestRecordTimeStamp())
            .setMinVersion(MlConfigVersion.CURRENT)
            .setSnapshotId(jobId + "_mock_snapshot")
            .setTimestamp(new Date())
            .setModelSizeStats(new ModelSizeStats.Builder(jobId).build())
            .build();

        try (XContentBuilder xContentBuilder = JsonXContent.contentBuilder()) {
            modelSnapshot.toXContent(xContentBuilder, ToXContent.EMPTY_PARAMS);
            IndexRequest indexRequest = new IndexRequest(AnomalyDetectorsIndex.jobResultsAliasedName(jobId));
            indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            indexRequest.id(ModelSnapshot.documentId(modelSnapshot));
            indexRequest.source(xContentBuilder);
            client().index(indexRequest).actionGet();
        }

        JobUpdate jobUpdate = new JobUpdate.Builder(jobId).setModelSnapshotId(modelSnapshot.getSnapshotId()).build();
        client().execute(UpdateJobAction.INSTANCE, new UpdateJobAction.Request(jobId, jobUpdate)).actionGet();
    }
}
