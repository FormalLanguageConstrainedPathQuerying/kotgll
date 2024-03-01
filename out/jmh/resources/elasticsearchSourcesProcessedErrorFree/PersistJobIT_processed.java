/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ml.integration;

import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.xpack.core.ml.action.FlushJobAction;
import org.elasticsearch.xpack.core.ml.action.PersistJobAction;
import org.elasticsearch.xpack.core.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.core.ml.job.config.DataDescription;
import org.elasticsearch.xpack.core.ml.job.config.Detector;
import org.elasticsearch.xpack.core.ml.job.config.Job;
import org.elasticsearch.xpack.core.ml.job.persistence.AnomalyDetectorsIndex;
import org.elasticsearch.xpack.core.ml.job.process.autodetect.state.ModelSnapshot;
import org.junit.After;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertResponse;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class PersistJobIT extends MlNativeAutodetectIntegTestCase {

    private static final long BUCKET_SPAN_SECONDS = 300;
    private static final TimeValue BUCKET_SPAN = TimeValue.timeValueSeconds(BUCKET_SPAN_SECONDS);

    @After
    public void cleanUpJobs() {
        cleanUp();
    }

    public void testPersistJob() throws Exception {
        String jobId = "persist-job-test";
        runJob(jobId);

        PersistJobAction.Response r = persistJob(jobId);
        assertTrue(r.isPersisted());

        assertBusy(() -> {
            List<ModelSnapshot> snapshots = getModelSnapshots(jobId);
            assertFalse(snapshots.isEmpty());
        });
    }

    public void testPersistJobOnGracefulShutdown_givenTimeAdvancedAfterNoNewData() throws Exception {
        String jobId = "time-advanced-after-no-new-data-test";

        runJob(jobId);
        FlushJobAction.Response flushResponse = flushJob(jobId, true);

        closeJob(jobId);
        long job1CloseTime = System.currentTimeMillis() / 1000;

        assertResponse(
            prepareSearch(AnomalyDetectorsIndex.jobStateIndexPattern()).setFetchSource(false).setTrackTotalHits(true).setSize(10000),
            stateDocsResponse1 -> {
                int numQuantileRecords = 0;
                int numStateRecords = 0;
                for (SearchHit hit : stateDocsResponse1.getHits().getHits()) {
                    logger.info("1: " + hit.getId());
                    if (hit.getId().contains("quantiles")) {
                        ++numQuantileRecords;
                    } else if (hit.getId().contains("model_state")) {
                        ++numStateRecords;
                    }
                }
                assertThat(stateDocsResponse1.getHits().getTotalHits().value, equalTo(2L));
                assertThat(numQuantileRecords, equalTo(1));
                assertThat(numStateRecords, equalTo(1));
            }
        );

        assertBusy(() -> {
            long timeNow = System.currentTimeMillis() / 1000;
            assertFalse(job1CloseTime >= timeNow);
        });

        openJob(jobId);

        long lastFinalizedBucketEnd = flushResponse.getLastFinalizedBucketEnd().toEpochMilli();
        FlushJobAction.Request advanceTimeRequest = new FlushJobAction.Request(jobId);
        advanceTimeRequest.setAdvanceTime(String.valueOf(lastFinalizedBucketEnd + BUCKET_SPAN_SECONDS * 1000));
        advanceTimeRequest.setCalcInterim(false);
        assertThat(client().execute(FlushJobAction.INSTANCE, advanceTimeRequest).actionGet().isFlushed(), is(true));

        closeJob(jobId);

        assertResponse(
            prepareSearch(AnomalyDetectorsIndex.jobStateIndexPattern()).setFetchSource(false).setTrackTotalHits(true).setSize(10000),
            stateDocsResponse2 -> {
                int numQuantileRecords = 0;
                int numStateRecords = 0;
                for (SearchHit hit : stateDocsResponse2.getHits().getHits()) {
                    logger.info("2: " + hit.getId());
                    if (hit.getId().contains("quantiles")) {
                        ++numQuantileRecords;
                    } else if (hit.getId().contains("model_state")) {
                        ++numStateRecords;
                    }
                }

                assertThat(stateDocsResponse2.getHits().getTotalHits().value, equalTo(3L));
                assertThat(numQuantileRecords, equalTo(1));
                assertThat(numStateRecords, equalTo(2));

            }
        );
        deleteJob(jobId);
    }

    public void testPersistJobOnGracefulShutdown_givenNoDataAndTimeAdvanced() throws Exception {
        String jobId = "no-data-and-time-advanced-test";

        createAndOpenJob(jobId);

        FlushJobAction.Request advanceTimeRequest = new FlushJobAction.Request(jobId);
        advanceTimeRequest.setAdvanceTime(String.valueOf(BUCKET_SPAN_SECONDS * 1000));
        advanceTimeRequest.setCalcInterim(false);
        assertThat(client().execute(FlushJobAction.INSTANCE, advanceTimeRequest).actionGet().isFlushed(), is(true));

        closeJob(jobId);

        assertResponse(
            prepareSearch(AnomalyDetectorsIndex.jobStateIndexPattern()).setFetchSource(false).setTrackTotalHits(true).setSize(10000),
            stateDocsResponse -> {
                int numQuantileRecords = 0;
                int numStateRecords = 0;
                for (SearchHit hit : stateDocsResponse.getHits().getHits()) {
                    logger.info(hit.getId());
                    if (hit.getId().contains("quantiles")) {
                        ++numQuantileRecords;
                    } else if (hit.getId().contains("model_state")) {
                        ++numStateRecords;
                    }
                }
                assertThat(stateDocsResponse.getHits().getTotalHits().value, equalTo(2L));
                assertThat(numQuantileRecords, equalTo(1));
                assertThat(numStateRecords, equalTo(1));
            }
        );

        AcknowledgedResponse ack = openJob(jobId);
        assertTrue(ack.isAcknowledged());

        closeJob(jobId);
        deleteJob(jobId);

        assertResponse(
            prepareSearch(AnomalyDetectorsIndex.jobStateIndexPattern()).setFetchSource(false).setTrackTotalHits(true).setSize(10000),
            stateDocsResponse -> {
                int numQuantileRecords = 0;
                int numStateRecords = 0;
                for (SearchHit hit : stateDocsResponse.getHits().getHits()) {
                    logger.info(hit.getId());
                    if (hit.getId().contains("quantiles")) {
                        ++numQuantileRecords;
                    } else if (hit.getId().contains("model_state")) {
                        ++numStateRecords;
                    }
                }
                assertThat(numQuantileRecords, equalTo(0));
                assertThat(numStateRecords, equalTo(0));
            }
        );
    }

    public void testPersistJobOnGracefulShutdown_givenNoDataAndNoTimeAdvance() throws Exception {
        String jobId = "no-data-and-no-time-advance-test";

        createAndOpenJob(jobId);

        closeJob(jobId);

        assertHitCount(prepareSearch(AnomalyDetectorsIndex.jobStateIndexPattern()), 0);

        assertHitCount(prepareSearch(AnomalyDetectorsIndex.jobResultsAliasedName(jobId)), 0);

        deleteJob(jobId);
    }

    private void createAndOpenJob(String jobId) throws Exception {
        Detector.Builder detector = new Detector.Builder("count", null);
        AnalysisConfig.Builder analysisConfig = new AnalysisConfig.Builder(Arrays.asList(detector.build()));
        analysisConfig.setBucketSpan(BUCKET_SPAN);
        Job.Builder job = new Job.Builder(jobId);
        job.setAnalysisConfig(analysisConfig);
        job.setDataDescription(new DataDescription.Builder());
        putJob(job);
        openJob(job.getId());
    }

    private void runJob(String jobId) throws Exception {
        createAndOpenJob(jobId);
        List<String> data = generateData(System.currentTimeMillis(), BUCKET_SPAN, 10, bucketIndex -> randomIntBetween(10, 20));
        postData(jobId, data.stream().collect(Collectors.joining()));
    }
}
