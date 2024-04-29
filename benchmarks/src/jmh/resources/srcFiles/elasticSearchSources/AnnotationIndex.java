/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.ml.annotations;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.TransportClusterHealthAction;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexAbstraction;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.Index;
import org.elasticsearch.xpack.core.ml.MlMetadata;
import org.elasticsearch.xpack.core.ml.job.persistence.ElasticsearchMappings;
import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;
import org.elasticsearch.xpack.core.ml.utils.MlIndexAndAlias;
import org.elasticsearch.xpack.core.template.TemplateUtils;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static java.lang.Thread.currentThread;
import static org.elasticsearch.ExceptionsHelper.formatStackTrace;
import static org.elasticsearch.core.Strings.format;
import static org.elasticsearch.xpack.core.ClientHelper.ML_ORIGIN;
import static org.elasticsearch.xpack.core.ClientHelper.executeAsyncWithOrigin;

public class AnnotationIndex {

    private static final Logger logger = LogManager.getLogger(AnnotationIndex.class);

    public static final String READ_ALIAS_NAME = ".ml-annotations-read";
    public static final String WRITE_ALIAS_NAME = ".ml-annotations-write";

    public static final String LATEST_INDEX_NAME = ".ml-annotations-000001";
    public static final List<String> OLD_INDEX_NAMES = List.of(".ml-annotations-6");

    private static final String MAPPINGS_VERSION_VARIABLE = "xpack.ml.version";
    public static final int ANNOTATION_INDEX_MAPPINGS_VERSION = 1;

    /**
     * Create the .ml-annotations-6 index with correct mappings if it does not already exist. This index is read and written by the UI
     * results views, so needs to exist when there might be ML results to view.  This method also waits for the index to be ready to search
     * before it returns.
     */
    public static void createAnnotationsIndexIfNecessaryAndWaitForYellow(
        Client client,
        ClusterState state,
        TimeValue masterNodeTimeout,
        final ActionListener<Boolean> finalListener
    ) {

        final ActionListener<Boolean> annotationsIndexCreatedListener = finalListener.delegateFailureAndWrap((delegate, success) -> {
            final ClusterHealthRequest request = new ClusterHealthRequest(READ_ALIAS_NAME).waitForYellowStatus()
                .masterNodeTimeout(masterNodeTimeout);
            executeAsyncWithOrigin(
                client,
                ML_ORIGIN,
                TransportClusterHealthAction.TYPE,
                request,
                delegate.delegateFailureAndWrap((l, r) -> l.onResponse(r.isTimedOut() == false))
            );
        });

        createAnnotationsIndexIfNecessary(client, state, masterNodeTimeout, annotationsIndexCreatedListener);
    }

    /**
     * Create the .ml-annotations-6 index with correct mappings if it does not already exist. This index is read and written by the UI
     * results views, so needs to exist when there might be ML results to view.
     */
    public static void createAnnotationsIndexIfNecessary(
        Client client,
        ClusterState state,
        TimeValue masterNodeTimeout,
        final ActionListener<Boolean> finalListener
    ) {

        final ActionListener<Boolean> checkMappingsListener = finalListener.delegateFailureAndWrap(
            (delegate, success) -> ElasticsearchMappings.addDocMappingIfMissing(
                WRITE_ALIAS_NAME,
                AnnotationIndex::annotationsMapping,
                client,
                state,
                masterNodeTimeout,
                delegate,
                ANNOTATION_INDEX_MAPPINGS_VERSION
            )
        );

        final ActionListener<String> createAliasListener = finalListener.delegateFailureAndWrap((finalDelegate, currentIndexName) -> {
            final IndicesAliasesRequestBuilder requestBuilder = client.admin()
                .indices()
                .prepareAliases()
                .addAliasAction(IndicesAliasesRequest.AliasActions.add().index(currentIndexName).alias(READ_ALIAS_NAME).isHidden(true))
                .addAliasAction(IndicesAliasesRequest.AliasActions.add().index(currentIndexName).alias(WRITE_ALIAS_NAME).isHidden(true));
            SortedMap<String, IndexAbstraction> lookup = state.getMetadata().getIndicesLookup();
            for (String oldIndexName : OLD_INDEX_NAMES) {
                IndexAbstraction oldIndexAbstraction = lookup.get(oldIndexName);
                if (oldIndexAbstraction != null) {
                    for (Index oldIndex : oldIndexAbstraction.getIndices()) {
                        requestBuilder.removeAlias(oldIndex.getName(), WRITE_ALIAS_NAME);
                    }
                }
            }
            executeAsyncWithOrigin(
                client.threadPool().getThreadContext(),
                ML_ORIGIN,
                requestBuilder.request(),
                finalDelegate.<IndicesAliasesResponse>delegateFailureAndWrap(
                    (l, r) -> checkMappingsListener.onResponse(r.isAcknowledged())
                ),
                client.admin().indices()::aliases
            );
        });

        MlMetadata mlMetadata = MlMetadata.getMlMetadata(state);
        SortedMap<String, IndexAbstraction> mlLookup = state.getMetadata().getIndicesLookup().tailMap(".ml");
        if (mlMetadata.isResetMode() == false
            && mlMetadata.isUpgradeMode() == false
            && mlLookup.isEmpty() == false
            && mlLookup.firstKey().startsWith(".ml")) {

            IndexAbstraction currentIndexAbstraction = mlLookup.get(LATEST_INDEX_NAME);
            if (currentIndexAbstraction == null) {
                logger.debug(
                    () -> format(
                        "Creating [%s] because [%s] exists; trace %s",
                        LATEST_INDEX_NAME,
                        mlLookup.firstKey(),
                        formatStackTrace(currentThread().getStackTrace())
                    )
                );

                CreateIndexRequest createIndexRequest = new CreateIndexRequest(LATEST_INDEX_NAME).mapping(annotationsMapping())
                    .settings(
                        Settings.builder()
                            .put(IndexMetadata.SETTING_AUTO_EXPAND_REPLICAS, "0-1")
                            .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, "1")
                            .put(IndexMetadata.SETTING_INDEX_HIDDEN, true)
                    );

                executeAsyncWithOrigin(
                    client.threadPool().getThreadContext(),
                    ML_ORIGIN,
                    createIndexRequest,
                    ActionListener.<CreateIndexResponse>wrap(r -> createAliasListener.onResponse(LATEST_INDEX_NAME), e -> {
                        if (ExceptionsHelper.unwrapCause(e) instanceof ResourceAlreadyExistsException) {
                            createAliasListener.onResponse(LATEST_INDEX_NAME);
                        } else {
                            finalListener.onFailure(e);
                        }
                    }),
                    client.admin().indices()::create
                );
                return;
            }

            String currentIndexName = currentIndexAbstraction.getIndices().get(0).getName();

            IndexAbstraction writeAliasAbstraction = mlLookup.get(WRITE_ALIAS_NAME);
            if (mlLookup.containsKey(READ_ALIAS_NAME) == false || writeAliasAbstraction == null) {
                createAliasListener.onResponse(currentIndexName);
                return;
            }

            List<Index> writeAliasIndices = writeAliasAbstraction.getIndices();
            if (writeAliasIndices.size() != 1 || currentIndexName.equals(writeAliasIndices.get(0).getName()) == false) {
                createAliasListener.onResponse(currentIndexName);
                return;
            }

            checkMappingsListener.onResponse(false);
            return;
        }

        finalListener.onResponse(false);
    }

    public static String annotationsMapping() {
        return TemplateUtils.loadTemplate(
            "/ml/annotations_index_mappings.json",
            MlIndexAndAlias.BWC_MAPPINGS_VERSION, 
            MAPPINGS_VERSION_VARIABLE,
            Map.of("xpack.ml.managed.index.version", Integer.toString(ANNOTATION_INDEX_MAPPINGS_VERSION))
        );
    }
}
