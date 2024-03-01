/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster.metadata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingClusterStateUpdateRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateAckListener;
import org.elasticsearch.cluster.ClusterStateTaskExecutor;
import org.elasticsearch.cluster.ClusterStateTaskListener;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.cluster.service.MasterServiceTaskQueue;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.core.IOUtils;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.mapper.MapperService.MergeReason;
import org.elasticsearch.index.mapper.Mapping;
import org.elasticsearch.indices.IndicesService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for submitting mapping changes
 */
public class MetadataMappingService {

    private static final Logger logger = LogManager.getLogger(MetadataMappingService.class);

    private final ClusterService clusterService;
    private final IndicesService indicesService;

    private final MasterServiceTaskQueue<PutMappingClusterStateUpdateTask> taskQueue;

    @Inject
    public MetadataMappingService(ClusterService clusterService, IndicesService indicesService) {
        this.clusterService = clusterService;
        this.indicesService = indicesService;
        taskQueue = clusterService.createTaskQueue("put-mapping", Priority.HIGH, new PutMappingExecutor());
    }

    record PutMappingClusterStateUpdateTask(PutMappingClusterStateUpdateRequest request, ActionListener<AcknowledgedResponse> listener)
        implements
            ClusterStateTaskListener,
            ClusterStateAckListener {

        @Override
        public void onFailure(Exception e) {
            listener.onFailure(e);
        }

        @Override
        public boolean mustAck(DiscoveryNode discoveryNode) {
            return true;
        }

        @Override
        public void onAllNodesAcked() {
            listener.onResponse(AcknowledgedResponse.of(true));
        }

        @Override
        public void onAckFailure(Exception e) {
            listener.onResponse(AcknowledgedResponse.of(false));
        }

        @Override
        public void onAckTimeout() {
            listener.onResponse(AcknowledgedResponse.FALSE);
        }

        @Override
        public TimeValue ackTimeout() {
            return request.ackTimeout();
        }
    }

    class PutMappingExecutor implements ClusterStateTaskExecutor<PutMappingClusterStateUpdateTask> {
        @Override
        public ClusterState execute(BatchExecutionContext<PutMappingClusterStateUpdateTask> batchExecutionContext) throws Exception {
            Map<Index, MapperService> indexMapperServices = new HashMap<>();
            try {
                var currentState = batchExecutionContext.initialState();
                for (final var taskContext : batchExecutionContext.taskContexts()) {
                    final var task = taskContext.getTask();
                    final PutMappingClusterStateUpdateRequest request = task.request;
                    try (var ignored = taskContext.captureResponseHeaders()) {
                        for (Index index : request.indices()) {
                            final IndexMetadata indexMetadata = currentState.metadata().getIndexSafe(index);
                            if (indexMapperServices.containsKey(indexMetadata.getIndex()) == false) {
                                MapperService mapperService = indicesService.createIndexMapperServiceForValidation(indexMetadata);
                                indexMapperServices.put(index, mapperService);
                                mapperService.merge(indexMetadata, MergeReason.MAPPING_RECOVERY);
                            }
                        }
                        currentState = applyRequest(currentState, request, indexMapperServices);
                        taskContext.success(task);
                    } catch (Exception e) {
                        taskContext.onFailure(e);
                    }
                }
                return currentState;
            } finally {
                IOUtils.close(indexMapperServices.values());
            }
        }

        private static ClusterState applyRequest(
            ClusterState currentState,
            PutMappingClusterStateUpdateRequest request,
            Map<Index, MapperService> indexMapperServices
        ) {

            final CompressedXContent mappingUpdateSource = request.source();
            final Metadata metadata = currentState.metadata();
            final List<IndexMetadata> updateList = new ArrayList<>();
            for (Index index : request.indices()) {
                MapperService mapperService = indexMapperServices.get(index);
                final IndexMetadata indexMetadata = currentState.getMetadata().getIndexSafe(index);
                DocumentMapper existingMapper = mapperService.documentMapper();
                if (existingMapper != null && existingMapper.mappingSource().equals(mappingUpdateSource)) {
                    continue;
                }
                updateList.add(indexMetadata);
                Mapping mapping = mapperService.parseMapping(MapperService.SINGLE_MAPPING_NAME, mappingUpdateSource);
                MapperService.mergeMappings(
                    mapperService.documentMapper(),
                    mapping,
                    request.autoUpdate() ? MergeReason.MAPPING_AUTO_UPDATE : MergeReason.MAPPING_UPDATE,
                    mapperService.getIndexSettings()
                );
            }
            Metadata.Builder builder = Metadata.builder(metadata);
            boolean updated = false;
            for (IndexMetadata indexMetadata : updateList) {
                boolean updatedMapping = false;
                final Index index = indexMetadata.getIndex();
                final MapperService mapperService = indexMapperServices.get(index);

                CompressedXContent existingSource = null;
                DocumentMapper existingMapper = mapperService.documentMapper();
                if (existingMapper != null) {
                    existingSource = existingMapper.mappingSource();
                }
                DocumentMapper mergedMapper = mapperService.merge(
                    MapperService.SINGLE_MAPPING_NAME,
                    mappingUpdateSource,
                    request.autoUpdate() ? MergeReason.MAPPING_AUTO_UPDATE : MergeReason.MAPPING_UPDATE
                );
                CompressedXContent updatedSource = mergedMapper.mappingSource();

                if (existingSource != null) {
                    if (existingSource.equals(updatedSource)) {
                    } else {
                        updatedMapping = true;
                        if (logger.isDebugEnabled()) {
                            logger.debug("{} update_mapping [{}] with source [{}]", index, mergedMapper.type(), updatedSource);
                        } else if (logger.isInfoEnabled()) {
                            logger.info("{} update_mapping [{}]", index, mergedMapper.type());
                        }

                    }
                } else {
                    updatedMapping = true;
                    if (logger.isDebugEnabled()) {
                        logger.debug("{} create_mapping with source [{}]", index, updatedSource);
                    } else if (logger.isInfoEnabled()) {
                        logger.info("{} create_mapping", index);
                    }
                }

                IndexMetadata.Builder indexMetadataBuilder = IndexMetadata.builder(indexMetadata);
                DocumentMapper mapper = mapperService.documentMapper();
                if (mapper != null) {
                    indexMetadataBuilder.putMapping(new MappingMetadata(mapper));
                }
                if (updatedMapping) {
                    indexMetadataBuilder.mappingVersion(1 + indexMetadataBuilder.mappingVersion());
                }
                /*
                 * This implicitly increments the index metadata version and builds the index metadata. This means that we need to have
                 * already incremented the mapping version if necessary. Therefore, the mapping version increment must remain before this
                 * statement.
                 */
                builder.put(indexMetadataBuilder);
                updated |= updatedMapping;
            }
            if (updated) {
                return ClusterState.builder(currentState).metadata(builder).build();
            } else {
                return currentState;
            }
        }

    }

    public void putMapping(final PutMappingClusterStateUpdateRequest request, final ActionListener<AcknowledgedResponse> listener) {
        final Metadata metadata = clusterService.state().metadata();
        boolean noop = true;
        for (Index index : request.indices()) {
            final IndexMetadata indexMetadata = metadata.index(index);
            if (indexMetadata == null) {
                noop = false;
                break;
            }
            final MappingMetadata mappingMetadata = indexMetadata.mapping();
            if (mappingMetadata == null) {
                noop = false;
                break;
            }
            if (request.source().equals(mappingMetadata.source()) == false) {
                noop = false;
                break;
            }
        }
        if (noop) {
            listener.onResponse(AcknowledgedResponse.TRUE);
            return;
        }

        taskQueue.submitTask(
            "put-mapping " + Strings.arrayToCommaDelimitedString(request.indices()),
            new PutMappingClusterStateUpdateTask(request, listener),
            request.masterNodeTimeout()
        );
    }
}
