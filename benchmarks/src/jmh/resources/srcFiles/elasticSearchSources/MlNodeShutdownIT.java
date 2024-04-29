/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.integration;

import org.apache.lucene.util.SetOnce;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.metadata.SingleNodeShutdownMetadata;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.xpack.core.action.util.QueryPage;
import org.elasticsearch.xpack.core.ml.action.CloseJobAction;
import org.elasticsearch.xpack.core.ml.action.GetJobsStatsAction;
import org.elasticsearch.xpack.core.ml.action.OpenJobAction;
import org.elasticsearch.xpack.core.ml.action.PutDatafeedAction;
import org.elasticsearch.xpack.core.ml.action.PutJobAction;
import org.elasticsearch.xpack.core.ml.action.StartDatafeedAction;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.core.ml.job.config.Job;
import org.elasticsearch.xpack.core.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.support.BaseMlIntegTestCase;
import org.elasticsearch.xpack.shutdown.PutShutdownNodeAction;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.cluster.metadata.SingleNodeShutdownMetadata.Type.SIGTERM;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class MlNodeShutdownIT extends BaseMlIntegTestCase {

    public void testJobsVacateShuttingDownNode() throws Exception {

        internalCluster().ensureAtLeastNumDataNodes(3);
        ensureStableCluster();

        createSourceData();

        setupJobAndDatafeed("shutdown-job-1", ByteSizeValue.ofMb(2));
        setupJobAndDatafeed("shutdown-job-2", ByteSizeValue.ofMb(2));
        setupJobAndDatafeed("shutdown-job-3", ByteSizeValue.ofMb(2));
        setupJobAndDatafeed("shutdown-job-4", ByteSizeValue.ofMb(2));
        setupJobAndDatafeed("shutdown-job-5", ByteSizeValue.ofMb(2));
        setupJobAndDatafeed("shutdown-job-6", ByteSizeValue.ofMb(2));

        String nodeNameToShutdown = rarely()
            ? internalCluster().getMasterName()
            : Arrays.stream(internalCluster().getNodeNames())
                .filter(nodeName -> internalCluster().getMasterName().equals(nodeName) == false)
                .findFirst()
                .get();
        SetOnce<String> nodeIdToShutdown = new SetOnce<>();

        assertBusy(() -> {
            GetJobsStatsAction.Response statsResponse = client().execute(
                GetJobsStatsAction.INSTANCE,
                new GetJobsStatsAction.Request(Metadata.ALL)
            ).actionGet();
            QueryPage<GetJobsStatsAction.Response.JobStats> jobStats = statsResponse.getResponse();
            assertThat(jobStats, notNullValue());
            long numJobsOnNodeToShutdown = jobStats.results()
                .stream()
                .filter(stats -> stats.getNode() != null && nodeNameToShutdown.equals(stats.getNode().getName()))
                .count();
            long numJobsOnOtherNodes = jobStats.results()
                .stream()
                .filter(stats -> stats.getNode() != null && nodeNameToShutdown.equals(stats.getNode().getName()) == false)
                .count();
            assertThat(numJobsOnNodeToShutdown, is(2L));
            assertThat(numJobsOnOtherNodes, is(4L));
            nodeIdToShutdown.set(
                jobStats.results()
                    .stream()
                    .filter(stats -> stats.getNode() != null && nodeNameToShutdown.equals(stats.getNode().getName()))
                    .map(stats -> stats.getNode().getId())
                    .findFirst()
                    .get()
            );
        });

        final SingleNodeShutdownMetadata.Type type = randomFrom(SingleNodeShutdownMetadata.Type.values());
        final String targetNodeName = type == SingleNodeShutdownMetadata.Type.REPLACE ? randomAlphaOfLengthBetween(10, 20) : null;
        final TimeValue grace = type == SIGTERM ? randomTimeValue() : null;
        client().execute(
            PutShutdownNodeAction.INSTANCE,
            new PutShutdownNodeAction.Request(nodeIdToShutdown.get(), type, "just testing", null, targetNodeName, grace)
        ).actionGet();

        assertBusy(() -> {
            GetJobsStatsAction.Response statsResponse = client().execute(
                GetJobsStatsAction.INSTANCE,
                new GetJobsStatsAction.Request(Metadata.ALL)
            ).actionGet();
            QueryPage<GetJobsStatsAction.Response.JobStats> jobStats = statsResponse.getResponse();
            assertThat(jobStats, notNullValue());
            long numJobsOnNodeToShutdown = jobStats.results()
                .stream()
                .filter(stats -> stats.getNode() != null && nodeNameToShutdown.equals(stats.getNode().getName()))
                .count();
            long numJobsOnOtherNodes = jobStats.results()
                .stream()
                .filter(stats -> stats.getNode() != null && nodeNameToShutdown.equals(stats.getNode().getName()) == false)
                .count();
            assertThat(numJobsOnNodeToShutdown, is(0L));
            assertThat(numJobsOnOtherNodes, is(6L));
        }, 30, TimeUnit.SECONDS);
    }

    public void testCloseJobVacatingShuttingDownNode() throws Exception {

        internalCluster().ensureAtLeastNumDataNodes(3);
        ensureStableCluster();

        createSourceData();

        setupJobAndDatafeed("shutdown-close-job-1", ByteSizeValue.ofMb(2));
        setupJobAndDatafeed("shutdown-close-job-2", ByteSizeValue.ofMb(2));
        setupJobAndDatafeed("shutdown-close-job-3", ByteSizeValue.ofMb(2));
        setupJobAndDatafeed("shutdown-close-job-4", ByteSizeValue.ofMb(2));
        setupJobAndDatafeed("shutdown-close-job-5", ByteSizeValue.ofMb(2));
        setupJobAndDatafeed("shutdown-close-job-6", ByteSizeValue.ofMb(2));

        String nodeNameToShutdown = rarely()
            ? internalCluster().getMasterName()
            : Arrays.stream(internalCluster().getNodeNames())
                .filter(nodeName -> internalCluster().getMasterName().equals(nodeName) == false)
                .findFirst()
                .get();
        SetOnce<String> nodeIdToShutdown = new SetOnce<>();
        SetOnce<String> jobIdToClose = new SetOnce<>();

        assertBusy(() -> {
            GetJobsStatsAction.Response statsResponse = client().execute(
                GetJobsStatsAction.INSTANCE,
                new GetJobsStatsAction.Request(Metadata.ALL)
            ).actionGet();
            QueryPage<GetJobsStatsAction.Response.JobStats> jobStats = statsResponse.getResponse();
            assertThat(jobStats, notNullValue());
            long numJobsOnNodeToShutdown = jobStats.results()
                .stream()
                .filter(stats -> stats.getNode() != null && nodeNameToShutdown.equals(stats.getNode().getName()))
                .count();
            long numJobsOnOtherNodes = jobStats.results()
                .stream()
                .filter(stats -> stats.getNode() != null && nodeNameToShutdown.equals(stats.getNode().getName()) == false)
                .count();
            assertThat(numJobsOnNodeToShutdown, is(2L));
            assertThat(numJobsOnOtherNodes, is(4L));
            nodeIdToShutdown.set(
                jobStats.results()
                    .stream()
                    .filter(stats -> stats.getNode() != null && nodeNameToShutdown.equals(stats.getNode().getName()))
                    .map(stats -> stats.getNode().getId())
                    .findFirst()
                    .get()
            );
            jobIdToClose.set(
                jobStats.results()
                    .stream()
                    .filter(stats -> stats.getNode() != null && nodeNameToShutdown.equals(stats.getNode().getName()))
                    .map(GetJobsStatsAction.Response.JobStats::getJobId)
                    .findAny()
                    .get()
            );
        });

        final SingleNodeShutdownMetadata.Type type = randomFrom(SingleNodeShutdownMetadata.Type.values());
        final String targetNodeName = type == SingleNodeShutdownMetadata.Type.REPLACE ? randomAlphaOfLengthBetween(10, 20) : null;
        final TimeValue grace = type == SIGTERM ? randomTimeValue() : null;
        client().execute(
            PutShutdownNodeAction.INSTANCE,
            new PutShutdownNodeAction.Request(nodeIdToShutdown.get(), type, "just testing", null, targetNodeName, grace)
        ).actionGet();

        if (randomBoolean()) {
            Thread.sleep(randomIntBetween(1, 10));
        }

        client().execute(CloseJobAction.INSTANCE, new CloseJobAction.Request(jobIdToClose.get())).actionGet();

        assertBusy(() -> {
            GetJobsStatsAction.Response statsResponse = client().execute(
                GetJobsStatsAction.INSTANCE,
                new GetJobsStatsAction.Request(Metadata.ALL)
            ).actionGet();
            QueryPage<GetJobsStatsAction.Response.JobStats> jobStats = statsResponse.getResponse();
            assertThat(jobStats, notNullValue());
            long numJobsOnNodeToShutdown = jobStats.results()
                .stream()
                .filter(stats -> stats.getNode() != null && nodeNameToShutdown.equals(stats.getNode().getName()))
                .count();
            long numJobsOnOtherNodes = jobStats.results()
                .stream()
                .filter(stats -> stats.getNode() != null && nodeNameToShutdown.equals(stats.getNode().getName()) == false)
                .count();
            assertThat(numJobsOnNodeToShutdown, is(0L));
            assertThat(numJobsOnOtherNodes, is(5L)); 
        }, 30, TimeUnit.SECONDS);
    }

    private void setupJobAndDatafeed(String jobId, ByteSizeValue modelMemoryLimit) throws Exception {
        Job.Builder job = createScheduledJob(jobId, modelMemoryLimit);
        PutJobAction.Request putJobRequest = new PutJobAction.Request(job);
        client().execute(PutJobAction.INSTANCE, putJobRequest).actionGet();

        String datafeedId = jobId;
        DatafeedConfig config = createDatafeed(datafeedId, job.getId(), Collections.singletonList("data"));
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

        StartDatafeedAction.Request startDatafeedRequest = new StartDatafeedAction.Request(config.getId(), 0L);
        client().execute(StartDatafeedAction.INSTANCE, startDatafeedRequest).get();
    }

    private void createSourceData() {
        client().admin().indices().prepareCreate("data").setMapping("time", "type=date").get();
        long numDocs = randomIntBetween(50, 100);
        long now = System.currentTimeMillis();
        long weekAgo = now - 604800000;
        long twoWeeksAgo = weekAgo - 604800000;
        indexDocs(logger, "data", numDocs, twoWeeksAgo, weekAgo);
    }
}
