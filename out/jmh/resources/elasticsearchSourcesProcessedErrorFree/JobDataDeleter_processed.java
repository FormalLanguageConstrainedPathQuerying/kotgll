/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ml.job.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshAction;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.TransportMultiSearchAction;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.broadcast.BroadcastResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.core.CheckedConsumer;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.AbstractBulkByScrollRequest;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.BulkByScrollTask;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xpack.core.action.util.PageParams;
import org.elasticsearch.xpack.core.ml.action.GetModelSnapshotsAction;
import org.elasticsearch.xpack.core.ml.annotations.Annotation;
import org.elasticsearch.xpack.core.ml.annotations.AnnotationIndex;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedTimingStats;
import org.elasticsearch.xpack.core.ml.job.config.Job;
import org.elasticsearch.xpack.core.ml.job.persistence.AnomalyDetectorsIndex;
import org.elasticsearch.xpack.core.ml.job.persistence.AnomalyDetectorsIndexFields;
import org.elasticsearch.xpack.core.ml.job.persistence.ElasticsearchMappings;
import org.elasticsearch.xpack.core.ml.job.process.autodetect.state.CategorizerState;
import org.elasticsearch.xpack.core.ml.job.process.autodetect.state.ModelSnapshot;
import org.elasticsearch.xpack.core.ml.job.process.autodetect.state.Quantiles;
import org.elasticsearch.xpack.core.ml.job.results.AnomalyRecord;
import org.elasticsearch.xpack.core.ml.job.results.Bucket;
import org.elasticsearch.xpack.core.ml.job.results.BucketInfluencer;
import org.elasticsearch.xpack.core.ml.job.results.Influencer;
import org.elasticsearch.xpack.core.ml.job.results.ModelPlot;
import org.elasticsearch.xpack.core.ml.job.results.Result;
import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;
import org.elasticsearch.xpack.core.security.user.InternalUsers;
import org.elasticsearch.xpack.ml.utils.MlIndicesUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.elasticsearch.xpack.core.ClientHelper.ML_ORIGIN;
import static org.elasticsearch.xpack.core.ClientHelper.executeAsyncWithOrigin;

public class JobDataDeleter {

    private static final Logger logger = LogManager.getLogger(JobDataDeleter.class);

    private static final int MAX_SNAPSHOTS_TO_DELETE = 10000;

    private final Client client;
    private final String jobId;
    private final boolean deleteUserAnnotations;

    public JobDataDeleter(Client client, String jobId) {
        this(client, jobId, false);
    }

    public JobDataDeleter(Client client, String jobId, boolean deleteUserAnnotations) {
        this.client = Objects.requireNonNull(client);
        this.jobId = Objects.requireNonNull(jobId);
        this.deleteUserAnnotations = deleteUserAnnotations;
    }

    /**
     * Delete a list of model snapshots and their corresponding state documents.
     *
     * @param modelSnapshots the model snapshots to delete
     */
    public void deleteModelSnapshots(List<ModelSnapshot> modelSnapshots, ActionListener<BulkByScrollResponse> listener) {
        if (modelSnapshots.isEmpty()) {
            listener.onResponse(
                new BulkByScrollResponse(
                    TimeValue.ZERO,
                    new BulkByScrollTask.Status(Collections.emptyList(), null),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    false
                )
            );
            return;
        }

        String stateIndexName = AnomalyDetectorsIndex.jobStateIndexPattern();

        List<String> idsToDelete = new ArrayList<>();
        Set<String> indices = new HashSet<>();
        indices.add(stateIndexName);
        indices.add(AnnotationIndex.READ_ALIAS_NAME);
        for (ModelSnapshot modelSnapshot : modelSnapshots) {
            idsToDelete.addAll(modelSnapshot.stateDocumentIds());
            idsToDelete.add(ModelSnapshot.documentId(modelSnapshot));
            idsToDelete.add(ModelSnapshot.annotationDocumentId(modelSnapshot));
            indices.add(AnomalyDetectorsIndex.jobResultsAliasedName(modelSnapshot.getJobId()));
        }

        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(indices.toArray(new String[0])).setRefresh(true)
            .setIndicesOptions(IndicesOptions.lenientExpandOpen())
            .setQuery(QueryBuilders.idsQuery().addIds(idsToDelete.toArray(new String[0])));

        deleteByQueryRequest.getSearchRequest().source().sort(ElasticsearchMappings.ES_DOC);

        executeAsyncWithOrigin(client, ML_ORIGIN, DeleteByQueryAction.INSTANCE, deleteByQueryRequest, listener);
    }

    /**
     * Asynchronously delete the annotations
     * If the deleteUserAnnotations field is set to true then all
     * annotations - both auto-generated and user-added - are removed, else
     * only the auto-generated ones, (i.e. created by the _xpack user) are
     * removed.
     * @param listener Response listener
     */
    public void deleteAllAnnotations(ActionListener<Boolean> listener) {
        deleteAnnotations(null, null, null, listener);
    }

    /**
     * Asynchronously delete all the auto-generated (i.e. created by the _xpack user) annotations starting from {@code cutOffTime}
     *
     * @param fromEpochMs Only annotations at and after this time will be deleted. If {@code null}, no cutoff is applied
     * @param toEpochMs Only annotations before this time will be deleted. If {@code null}, no cutoff is applied
     * @param eventsToDelete Only annotations with one of the provided event types will be deleted.
     *                       If {@code null} or empty, no event-related filtering is applied
     * @param listener Response listener
     */
    public void deleteAnnotations(
        @Nullable Long fromEpochMs,
        @Nullable Long toEpochMs,
        @Nullable Set<String> eventsToDelete,
        ActionListener<Boolean> listener
    ) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().filter(QueryBuilders.termQuery(Job.ID.getPreferredName(), jobId));
        if (deleteUserAnnotations == false) {
            boolQuery.filter(QueryBuilders.termQuery(Annotation.CREATE_USERNAME.getPreferredName(), InternalUsers.XPACK_USER.principal()));
        }
        if (fromEpochMs != null || toEpochMs != null) {
            boolQuery.filter(QueryBuilders.rangeQuery(Annotation.TIMESTAMP.getPreferredName()).gte(fromEpochMs).lt(toEpochMs));
        }
        if (eventsToDelete != null && eventsToDelete.isEmpty() == false) {
            boolQuery.filter(QueryBuilders.termsQuery(Annotation.EVENT.getPreferredName(), eventsToDelete));
        }
        QueryBuilder query = QueryBuilders.constantScoreQuery(boolQuery);
        DeleteByQueryRequest dbqRequest = new DeleteByQueryRequest(AnnotationIndex.READ_ALIAS_NAME).setQuery(query)
            .setIndicesOptions(IndicesOptions.lenientExpandOpen())
            .setAbortOnVersionConflict(false)
            .setRefresh(true)
            .setSlices(AbstractBulkByScrollRequest.AUTO_SLICES);

        dbqRequest.getSearchRequest().source().sort(ElasticsearchMappings.ES_DOC);

        executeAsyncWithOrigin(
            client,
            ML_ORIGIN,
            DeleteByQueryAction.INSTANCE,
            dbqRequest,
            ActionListener.wrap(r -> listener.onResponse(true), listener::onFailure)
        );
    }

    /**
     * Asynchronously delete all result types (Buckets, Records, Influencers) from {@code cutOffTime}.
     * Forecasts are <em>not</em> deleted, as they will not be automatically regenerated after
     * restarting a datafeed following a model snapshot reversion.
     *
     * @param cutoffEpochMs Results at and after this time will be deleted
     * @param listener Response listener
     */
    public void deleteResultsFromTime(long cutoffEpochMs, ActionListener<Boolean> listener) {
        QueryBuilder query = QueryBuilders.boolQuery()
            .filter(
                QueryBuilders.termsQuery(
                    Result.RESULT_TYPE.getPreferredName(),
                    AnomalyRecord.RESULT_TYPE_VALUE,
                    Bucket.RESULT_TYPE_VALUE,
                    BucketInfluencer.RESULT_TYPE_VALUE,
                    Influencer.RESULT_TYPE_VALUE,
                    ModelPlot.RESULT_TYPE_VALUE
                )
            )
            .filter(QueryBuilders.rangeQuery(Result.TIMESTAMP.getPreferredName()).gte(cutoffEpochMs));
        DeleteByQueryRequest dbqRequest = new DeleteByQueryRequest(AnomalyDetectorsIndex.jobResultsAliasedName(jobId)).setQuery(query)
            .setIndicesOptions(IndicesOptions.lenientExpandOpen())
            .setAbortOnVersionConflict(false)
            .setRefresh(true)
            .setSlices(AbstractBulkByScrollRequest.AUTO_SLICES);

        dbqRequest.getSearchRequest().source().sort(ElasticsearchMappings.ES_DOC);

        executeAsyncWithOrigin(
            client,
            ML_ORIGIN,
            DeleteByQueryAction.INSTANCE,
            dbqRequest,
            ActionListener.wrap(r -> listener.onResponse(true), listener::onFailure)
        );
    }

    /**
     * Delete all results marked as interim
     */
    public void deleteInterimResults() {
        QueryBuilder query = QueryBuilders.constantScoreQuery(QueryBuilders.termQuery(Result.IS_INTERIM.getPreferredName(), true));
        DeleteByQueryRequest dbqRequest = new DeleteByQueryRequest(AnomalyDetectorsIndex.jobResultsAliasedName(jobId)).setQuery(query)
            .setIndicesOptions(IndicesOptions.lenientExpandOpen())
            .setAbortOnVersionConflict(false)
            .setRefresh(false)
            .setSlices(AbstractBulkByScrollRequest.AUTO_SLICES);

        dbqRequest.getSearchRequest().source().sort(ElasticsearchMappings.ES_DOC);

        try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashWithOrigin(ML_ORIGIN)) {
            client.execute(DeleteByQueryAction.INSTANCE, dbqRequest).get();
        } catch (Exception e) {
            logger.error("[" + jobId + "] An error occurred while deleting interim results", e);
        }
    }

    /**
     * Delete the datafeed timing stats document from all the job results indices
     *
     * @param listener Response listener
     */
    public void deleteDatafeedTimingStats(ActionListener<BulkByScrollResponse> listener) {
        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(AnomalyDetectorsIndex.jobResultsAliasedName(jobId)).setRefresh(
            true
        )
            .setIndicesOptions(IndicesOptions.lenientExpandOpen())
            .setQuery(QueryBuilders.idsQuery().addIds(DatafeedTimingStats.documentId(jobId)));

        deleteByQueryRequest.getSearchRequest().source().sort(ElasticsearchMappings.ES_DOC);

        executeAsyncWithOrigin(client, ML_ORIGIN, DeleteByQueryAction.INSTANCE, deleteByQueryRequest, listener);
    }

    /**
     * Deletes all documents associated with a job except user annotations and notifications
     */
    public void deleteJobDocuments(
        JobConfigProvider jobConfigProvider,
        IndexNameExpressionResolver indexNameExpressionResolver,
        ClusterState clusterState,
        CheckedConsumer<Boolean, Exception> finishedHandler,
        Consumer<Exception> failureHandler
    ) {

        AtomicReference<String[]> indexNames = new AtomicReference<>();

        final ActionListener<AcknowledgedResponse> completionHandler = ActionListener.wrap(
            response -> finishedHandler.accept(response.isAcknowledged()),
            failureHandler
        );

        ActionListener<BulkByScrollResponse> dbqHandler = ActionListener.wrap(bulkByScrollResponse -> {
            if (bulkByScrollResponse == null) { 
                completionHandler.onResponse(AcknowledgedResponse.TRUE);
            } else {
                if (bulkByScrollResponse.isTimedOut()) {
                    logger.warn("[{}] DeleteByQuery for indices [{}] timed out.", jobId, String.join(", ", indexNames.get()));
                }
                if (bulkByScrollResponse.getBulkFailures().isEmpty() == false) {
                    logger.warn(
                        "[{}] {} failures and {} conflicts encountered while running DeleteByQuery on indices [{}].",
                        jobId,
                        bulkByScrollResponse.getBulkFailures().size(),
                        bulkByScrollResponse.getVersionConflicts(),
                        String.join(", ", indexNames.get())
                    );
                    for (BulkItemResponse.Failure failure : bulkByScrollResponse.getBulkFailures()) {
                        logger.warn("DBQ failure: " + failure);
                    }
                }
                deleteAliases(jobId, completionHandler);
            }
        }, failureHandler);

        ActionListener<Boolean> deleteByQueryExecutor = ActionListener.wrap(response -> {
            if (response && indexNames.get().length > 0) {
                deleteResultsByQuery(jobId, indexNames.get(), dbqHandler);
            } else { 
                dbqHandler.onResponse(null);
            }
        }, failureHandler);

        ActionListener<MultiSearchResponse> customIndexSearchHandler = ActionListener.wrap(multiSearchResponse -> {
            if (multiSearchResponse == null) {
                deleteByQueryExecutor.onResponse(true); 
                return;
            }
            String defaultSharedIndex = AnomalyDetectorsIndexFields.RESULTS_INDEX_PREFIX
                + AnomalyDetectorsIndexFields.RESULTS_INDEX_DEFAULT;
            List<String> indicesToDelete = new ArrayList<>();
            boolean needToRunDBQTemp = false;
            assert multiSearchResponse.getResponses().length == indexNames.get().length;
            int i = 0;
            for (MultiSearchResponse.Item item : multiSearchResponse.getResponses()) {
                if (item.isFailure()) {
                    ++i;
                    if (ExceptionsHelper.unwrapCause(item.getFailure()) instanceof IndexNotFoundException) {
                        continue;
                    } else {
                        failureHandler.accept(item.getFailure());
                        return;
                    }
                }
                SearchResponse searchResponse = item.getResponse();
                if (searchResponse.getHits().getTotalHits().value > 0 || indexNames.get()[i].equals(defaultSharedIndex)) {
                    needToRunDBQTemp = true;
                } else {
                    indicesToDelete.add(indexNames.get()[i]);
                }
                ++i;
            }
            final boolean needToRunDBQ = needToRunDBQTemp;
            if (indicesToDelete.isEmpty()) {
                deleteByQueryExecutor.onResponse(needToRunDBQ);
                return;
            }
            logger.info("[{}] deleting the following indices directly {}", jobId, indicesToDelete);
            DeleteIndexRequest request = new DeleteIndexRequest(indicesToDelete.toArray(String[]::new));
            request.indicesOptions(IndicesOptions.lenientExpandOpenHidden());
            executeAsyncWithOrigin(
                client.threadPool().getThreadContext(),
                ML_ORIGIN,
                request,
                ActionListener.<AcknowledgedResponse>wrap(
                    response -> deleteByQueryExecutor.onResponse(needToRunDBQ), 
                    failureHandler
                ),
                client.admin().indices()::delete
            );
        }, failure -> {
            if (ExceptionsHelper.unwrapCause(failure) instanceof IndexNotFoundException) { 
                deleteByQueryExecutor.onResponse(false); 
            } else {
                failureHandler.accept(failure);
            }
        });

        ActionListener<Job.Builder> getJobHandler = ActionListener.wrap(builder -> {
            indexNames.set(
                indexNameExpressionResolver.concreteIndexNames(
                    clusterState,
                    IndicesOptions.lenientExpandOpen(),
                    AnomalyDetectorsIndex.jobResultsAliasedName(jobId)
                )
            );
            if (indexNames.get().length == 0) {
                customIndexSearchHandler.onResponse(null);
                return;
            }
            MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
            for (String indexName : indexNames.get()) {
                SearchSourceBuilder source = new SearchSourceBuilder().size(0)
                    .trackTotalHitsUpTo(1)
                    .query(
                        QueryBuilders.boolQuery()
                            .filter(QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery(Job.ID.getPreferredName(), jobId)))
                    );
                multiSearchRequest.add(new SearchRequest(indexName).source(source));
            }
            executeAsyncWithOrigin(client, ML_ORIGIN, TransportMultiSearchAction.TYPE, multiSearchRequest, customIndexSearchHandler);
        }, failureHandler);

        ActionListener<Boolean> deleteAnnotationsHandler = ActionListener.wrap(
            response -> jobConfigProvider.getJob(jobId, null, getJobHandler),
            failureHandler
        );

        ActionListener<Boolean> deleteCategorizerStateHandler = ActionListener.wrap(
            response -> deleteAllAnnotations(deleteAnnotationsHandler),
            failureHandler
        );

        ActionListener<Boolean> deleteQuantilesHandler = ActionListener.wrap(
            response -> deleteCategorizerState(jobId, 1, deleteCategorizerStateHandler),
            failureHandler
        );

        ActionListener<BulkByScrollResponse> deleteStateHandler = ActionListener.wrap(
            bulkResponse -> deleteQuantiles(jobId, deleteQuantilesHandler),
            failureHandler
        );

        deleteModelState(jobId, deleteStateHandler);
    }

    private void deleteResultsByQuery(
        @SuppressWarnings("HiddenField") String jobId,
        String[] indices,
        ActionListener<BulkByScrollResponse> listener
    ) {
        assert indices.length > 0;

        ActionListener<BroadcastResponse> refreshListener = ActionListener.wrap(refreshResponse -> {
            logger.info("[{}] running delete by query on [{}]", jobId, String.join(", ", indices));
            ConstantScoreQueryBuilder query = new ConstantScoreQueryBuilder(new TermQueryBuilder(Job.ID.getPreferredName(), jobId));
            DeleteByQueryRequest request = new DeleteByQueryRequest(indices).setQuery(query)
                .setIndicesOptions(MlIndicesUtils.addIgnoreUnavailable(IndicesOptions.lenientExpandOpenHidden()))
                .setSlices(AbstractBulkByScrollRequest.AUTO_SLICES)
                .setAbortOnVersionConflict(false)
                .setRefresh(true);

            executeAsyncWithOrigin(client, ML_ORIGIN, DeleteByQueryAction.INSTANCE, request, listener);
        }, listener::onFailure);

        RefreshRequest refreshRequest = new RefreshRequest(indices);
        refreshRequest.indicesOptions(MlIndicesUtils.addIgnoreUnavailable(IndicesOptions.lenientExpandOpenHidden()));
        executeAsyncWithOrigin(client, ML_ORIGIN, RefreshAction.INSTANCE, refreshRequest, refreshListener);
    }

    private void deleteAliases(@SuppressWarnings("HiddenField") String jobId, ActionListener<AcknowledgedResponse> finishedHandler) {
        final String readAliasName = AnomalyDetectorsIndex.jobResultsAliasedName(jobId);
        final String writeAliasName = AnomalyDetectorsIndex.resultsWriteAlias(jobId);

        GetAliasesRequest aliasesRequest = new GetAliasesRequest().aliases(readAliasName, writeAliasName)
            .indicesOptions(IndicesOptions.lenientExpandOpenHidden());
        executeAsyncWithOrigin(
            client.threadPool().getThreadContext(),
            ML_ORIGIN,
            aliasesRequest,
            ActionListener.<GetAliasesResponse>wrap(getAliasesResponse -> {
                IndicesAliasesRequest removeRequest = buildRemoveAliasesRequest(getAliasesResponse);
                if (removeRequest == null) {
                    finishedHandler.onResponse(AcknowledgedResponse.TRUE);
                    return;
                }
                executeAsyncWithOrigin(
                    client.threadPool().getThreadContext(),
                    ML_ORIGIN,
                    removeRequest,
                    finishedHandler,
                    client.admin().indices()::aliases
                );
            }, finishedHandler::onFailure),
            client.admin().indices()::getAliases
        );
    }

    private static IndicesAliasesRequest buildRemoveAliasesRequest(GetAliasesResponse getAliasesResponse) {
        Set<String> aliases = new HashSet<>();
        List<String> indices = new ArrayList<>();
        for (var entry : getAliasesResponse.getAliases().entrySet()) {
            if (entry.getValue().isEmpty() == false) {
                indices.add(entry.getKey());
                entry.getValue().forEach(metadata -> aliases.add(metadata.getAlias()));
            }
        }
        return aliases.isEmpty()
            ? null
            : new IndicesAliasesRequest().addAliasAction(
                IndicesAliasesRequest.AliasActions.remove().aliases(aliases.toArray(new String[0])).indices(indices.toArray(new String[0]))
            );
    }

    private void deleteQuantiles(@SuppressWarnings("HiddenField") String jobId, ActionListener<Boolean> finishedHandler) {
        IdsQueryBuilder query = new IdsQueryBuilder().addIds(Quantiles.documentId(jobId));
        DeleteByQueryRequest request = new DeleteByQueryRequest(AnomalyDetectorsIndex.jobStateIndexPattern()).setQuery(query)
            .setIndicesOptions(MlIndicesUtils.addIgnoreUnavailable(IndicesOptions.lenientExpandOpen()))
            .setAbortOnVersionConflict(false)
            .setRefresh(true);

        executeAsyncWithOrigin(
            client,
            ML_ORIGIN,
            DeleteByQueryAction.INSTANCE,
            request,
            ActionListener.wrap(response -> finishedHandler.onResponse(true), ignoreIndexNotFoundException(finishedHandler))
        );
    }

    private void deleteModelState(@SuppressWarnings("HiddenField") String jobId, ActionListener<BulkByScrollResponse> listener) {
        GetModelSnapshotsAction.Request request = new GetModelSnapshotsAction.Request(jobId, null);
        request.setPageParams(new PageParams(0, MAX_SNAPSHOTS_TO_DELETE));
        executeAsyncWithOrigin(client, ML_ORIGIN, GetModelSnapshotsAction.INSTANCE, request, ActionListener.wrap(response -> {
            List<ModelSnapshot> deleteCandidates = response.getPage().results();
            deleteModelSnapshots(deleteCandidates, listener);
        }, listener::onFailure));
    }

    private void deleteCategorizerState(
        @SuppressWarnings("HiddenField") String jobId,
        int docNum,
        ActionListener<Boolean> finishedHandler
    ) {
        IdsQueryBuilder query = new IdsQueryBuilder().addIds(CategorizerState.documentId(jobId, docNum));
        DeleteByQueryRequest request = new DeleteByQueryRequest(AnomalyDetectorsIndex.jobStateIndexPattern()).setQuery(query)
            .setIndicesOptions(MlIndicesUtils.addIgnoreUnavailable(IndicesOptions.lenientExpandOpen()))
            .setAbortOnVersionConflict(false)
            .setRefresh(true);

        executeAsyncWithOrigin(client, ML_ORIGIN, DeleteByQueryAction.INSTANCE, request, ActionListener.wrap(response -> {
            if (response.getDeleted() > 0) {
                deleteCategorizerState(jobId, docNum + 1, finishedHandler);
                return;
            }
            finishedHandler.onResponse(true);
        }, ignoreIndexNotFoundException(finishedHandler)));
    }

    private static Consumer<Exception> ignoreIndexNotFoundException(ActionListener<Boolean> finishedHandler) {
        return e -> {
            if (ExceptionsHelper.unwrapCause(e) instanceof IndexNotFoundException) {
                finishedHandler.onResponse(true);
            } else {
                finishedHandler.onFailure(e);
            }
        };
    }
}
