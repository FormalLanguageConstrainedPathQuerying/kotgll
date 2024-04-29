/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.ilm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.TransportVersions;
import org.elasticsearch.action.admin.indices.shrink.ResizeNumberOfShardsCalculator;
import org.elasticsearch.action.admin.indices.stats.IndexShardStats;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.metadata.IndexAbstraction;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.xcontent.ConstructingObjectParser;
import org.elasticsearch.xcontent.ObjectParser;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xpack.core.ilm.Step.StepKey;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.elasticsearch.xpack.core.ilm.ShrinkIndexNameSupplier.SHRUNKEN_INDEX_PREFIX;

/**
 * A {@link LifecycleAction} which shrinks the index.
 */
public class ShrinkAction implements LifecycleAction {
    private static final Logger logger = LogManager.getLogger(ShrinkAction.class);

    public static final String NAME = "shrink";
    public static final ParseField NUMBER_OF_SHARDS_FIELD = new ParseField("number_of_shards");
    public static final ParseField MAX_PRIMARY_SHARD_SIZE = new ParseField("max_primary_shard_size");
    public static final ParseField ALLOW_WRITE_AFTER_SHRINK = new ParseField("allow_write_after_shrink");
    public static final String CONDITIONAL_SKIP_SHRINK_STEP = BranchingStep.NAME + "-check-prerequisites";
    public static final String CONDITIONAL_DATASTREAM_CHECK_KEY = BranchingStep.NAME + "-on-datastream-check";

    private static final ConstructingObjectParser<ShrinkAction, Void> PARSER = new ConstructingObjectParser<>(
        NAME,
        a -> new ShrinkAction((Integer) a[0], (ByteSizeValue) a[1], (a[2] != null && (Boolean) a[2]))
    );

    static {
        PARSER.declareInt(ConstructingObjectParser.optionalConstructorArg(), NUMBER_OF_SHARDS_FIELD);
        PARSER.declareField(
            ConstructingObjectParser.optionalConstructorArg(),
            (p, c) -> ByteSizeValue.parseBytesSizeValue(p.text(), MAX_PRIMARY_SHARD_SIZE.getPreferredName()),
            MAX_PRIMARY_SHARD_SIZE,
            ObjectParser.ValueType.STRING
        );
        PARSER.declareBoolean(ConstructingObjectParser.optionalConstructorArg(), ALLOW_WRITE_AFTER_SHRINK);
    }

    public static final Settings CLEAR_WRITE_BLOCK_SETTINGS = Settings.builder()
        .put(IndexMetadata.INDEX_BLOCKS_WRITE_SETTING.getKey(), (String) null)
        .build();

    private Integer numberOfShards;
    private ByteSizeValue maxPrimaryShardSize;
    private boolean allowWriteAfterShrink;

    public static ShrinkAction parse(XContentParser parser) throws IOException {
        return PARSER.parse(parser, null);
    }

    public ShrinkAction(@Nullable Integer numberOfShards, @Nullable ByteSizeValue maxPrimaryShardSize, boolean allowWriteAfterShrink) {
        if (numberOfShards != null && maxPrimaryShardSize != null) {
            throw new IllegalArgumentException("Cannot set both [number_of_shards] and [max_primary_shard_size]");
        }
        if (numberOfShards == null && maxPrimaryShardSize == null) {
            throw new IllegalArgumentException("Either [number_of_shards] or [max_primary_shard_size] must be set");
        }
        if (maxPrimaryShardSize != null) {
            if (maxPrimaryShardSize.getBytes() <= 0) {
                throw new IllegalArgumentException("[max_primary_shard_size] must be greater than 0");
            }
            this.maxPrimaryShardSize = maxPrimaryShardSize;
        } else {
            if (numberOfShards <= 0) {
                throw new IllegalArgumentException("[" + NUMBER_OF_SHARDS_FIELD.getPreferredName() + "] must be greater than 0");
            }
            this.numberOfShards = numberOfShards;
        }
        this.allowWriteAfterShrink = allowWriteAfterShrink;
    }

    public ShrinkAction(StreamInput in) throws IOException {
        if (in.readBoolean()) {
            this.numberOfShards = in.readVInt();
            this.maxPrimaryShardSize = null;
        } else {
            this.numberOfShards = null;
            this.maxPrimaryShardSize = ByteSizeValue.readFrom(in);
        }
        this.allowWriteAfterShrink = in.getTransportVersion().onOrAfter(TransportVersions.ILM_SHRINK_ENABLE_WRITE) && in.readBoolean();
    }

    public Integer getNumberOfShards() {
        return numberOfShards;
    }

    public ByteSizeValue getMaxPrimaryShardSize() {
        return maxPrimaryShardSize;
    }

    public boolean getAllowWriteAfterShrink() {
        return allowWriteAfterShrink;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        boolean hasNumberOfShards = numberOfShards != null;
        out.writeBoolean(hasNumberOfShards);
        if (hasNumberOfShards) {
            out.writeVInt(numberOfShards);
        } else {
            maxPrimaryShardSize.writeTo(out);
        }
        if (out.getTransportVersion().onOrAfter(TransportVersions.ILM_SHRINK_ENABLE_WRITE)) {
            out.writeBoolean(this.allowWriteAfterShrink);
        }
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (numberOfShards != null) {
            builder.field(NUMBER_OF_SHARDS_FIELD.getPreferredName(), numberOfShards);
        }
        if (maxPrimaryShardSize != null) {
            builder.field(MAX_PRIMARY_SHARD_SIZE.getPreferredName(), maxPrimaryShardSize);
        }
        builder.field(ALLOW_WRITE_AFTER_SHRINK.getPreferredName(), allowWriteAfterShrink);
        builder.endObject();
        return builder;
    }

    @Override
    public boolean isSafeAction() {
        return false;
    }

    @Override
    public List<Step> toSteps(Client client, String phase, Step.StepKey nextStepKey) {
        StepKey preShrinkBranchingKey = new StepKey(phase, NAME, CONDITIONAL_SKIP_SHRINK_STEP);
        StepKey checkNotWriteIndex = new StepKey(phase, NAME, CheckNotDataStreamWriteIndexStep.NAME);
        StepKey waitForNoFollowerStepKey = new StepKey(phase, NAME, WaitForNoFollowersStep.NAME);
        StepKey waitTimeSeriesEndTimePassesKey = new StepKey(phase, NAME, WaitUntilTimeSeriesEndTimePassesStep.NAME);
        StepKey readOnlyKey = new StepKey(phase, NAME, ReadOnlyAction.NAME);
        StepKey checkTargetShardsCountKey = new StepKey(phase, NAME, CheckTargetShardsCountStep.NAME);
        StepKey cleanupShrinkIndexKey = new StepKey(phase, NAME, CleanupShrinkIndexStep.NAME);
        StepKey generateShrinkIndexNameKey = new StepKey(phase, NAME, GenerateUniqueIndexNameStep.NAME);
        StepKey setSingleNodeKey = new StepKey(phase, NAME, SetSingleNodeAllocateStep.NAME);
        StepKey allocationRoutedKey = new StepKey(phase, NAME, CheckShrinkReadyStep.NAME);
        StepKey shrinkKey = new StepKey(phase, NAME, ShrinkStep.NAME);
        StepKey enoughShardsKey = new StepKey(phase, NAME, ShrunkShardsAllocatedStep.NAME);
        StepKey copyMetadataKey = new StepKey(phase, NAME, CopyExecutionStateStep.NAME);
        StepKey dataStreamCheckBranchingKey = new StepKey(phase, NAME, CONDITIONAL_DATASTREAM_CHECK_KEY);
        StepKey aliasKey = new StepKey(phase, NAME, ShrinkSetAliasStep.NAME);
        StepKey isShrunkIndexKey = new StepKey(phase, NAME, ShrunkenIndexCheckStep.NAME);
        StepKey replaceDataStreamIndexKey = new StepKey(phase, NAME, ReplaceDataStreamBackingIndexStep.NAME);
        StepKey deleteIndexKey = new StepKey(phase, NAME, DeleteStep.NAME);
        StepKey allowWriteKey = new StepKey(phase, NAME, UpdateSettingsStep.NAME);
        StepKey lastOrNextStep = allowWriteAfterShrink ? allowWriteKey : nextStepKey;

        AsyncBranchingStep conditionalSkipShrinkStep = new AsyncBranchingStep(
            preShrinkBranchingKey,
            checkNotWriteIndex,
            lastOrNextStep,
            (indexMetadata, clusterState, listener) -> {
                if (indexMetadata.getSettings().get(LifecycleSettings.SNAPSHOT_INDEX_NAME) != null) {
                    logger.warn(
                        "[{}] action is configured for index [{}] in policy [{}] which is mounted as searchable snapshot. "
                            + "Skipping this action",
                        ShrinkAction.NAME,
                        indexMetadata.getIndex().getName(),
                        indexMetadata.getLifecyclePolicyName()
                    );
                    listener.onResponse(true);
                    return;
                }
                String indexName = indexMetadata.getIndex().getName();
                client.admin()
                    .indices()
                    .prepareStats(indexName)
                    .clear()
                    .setDocs(true)
                    .setStore(true)
                    .execute(listener.delegateFailure((delegateListener, indicesStatsResponse) -> {
                        int targetNumberOfShards = new ResizeNumberOfShardsCalculator.ShrinkShardsCalculator(
                            indicesStatsResponse.getPrimaries().store,
                            i -> {
                                IndexShardStats shard = indicesStatsResponse.getIndex(indexName).getIndexShards().get(i);
                                return shard == null ? null : shard.getPrimary().getDocs();
                            }
                        ).calculate(numberOfShards, maxPrimaryShardSize, indexMetadata);
                        delegateListener.onResponse(indexMetadata.getNumberOfShards() == targetNumberOfShards);
                    }));
            },
            client
        );
        CheckNotDataStreamWriteIndexStep checkNotWriteIndexStep = new CheckNotDataStreamWriteIndexStep(
            checkNotWriteIndex,
            waitForNoFollowerStepKey
        );
        WaitForNoFollowersStep waitForNoFollowersStep = new WaitForNoFollowersStep(
            waitForNoFollowerStepKey,
            waitTimeSeriesEndTimePassesKey,
            client
        );
        WaitUntilTimeSeriesEndTimePassesStep waitUntilTimeSeriesEndTimeStep = new WaitUntilTimeSeriesEndTimePassesStep(
            waitTimeSeriesEndTimePassesKey,
            readOnlyKey,
            Instant::now,
            client
        );
        ReadOnlyStep readOnlyStep = new ReadOnlyStep(readOnlyKey, checkTargetShardsCountKey, client);
        CheckTargetShardsCountStep checkTargetShardsCountStep = new CheckTargetShardsCountStep(
            checkTargetShardsCountKey,
            cleanupShrinkIndexKey,
            numberOfShards
        );
        CleanupShrinkIndexStep cleanupShrinkIndexStep = new CleanupShrinkIndexStep(
            cleanupShrinkIndexKey,
            generateShrinkIndexNameKey,
            client
        );
        GenerateUniqueIndexNameStep generateUniqueIndexNameStep = new GenerateUniqueIndexNameStep(
            generateShrinkIndexNameKey,
            setSingleNodeKey,
            SHRUNKEN_INDEX_PREFIX,
            (generatedIndexName, lifecycleStateBuilder) -> lifecycleStateBuilder.setShrinkIndexName(generatedIndexName)
        );
        SetSingleNodeAllocateStep setSingleNodeStep = new SetSingleNodeAllocateStep(setSingleNodeKey, allocationRoutedKey, client);

        ClusterStateWaitUntilThresholdStep checkShrinkReadyStep = new ClusterStateWaitUntilThresholdStep(
            new CheckShrinkReadyStep(allocationRoutedKey, shrinkKey),
            setSingleNodeKey
        );
        ShrinkStep shrink = new ShrinkStep(shrinkKey, enoughShardsKey, client, numberOfShards, maxPrimaryShardSize);
        ClusterStateWaitUntilThresholdStep allocated = new ClusterStateWaitUntilThresholdStep(
            new ShrunkShardsAllocatedStep(enoughShardsKey, copyMetadataKey),
            cleanupShrinkIndexKey
        );
        CopyExecutionStateStep copyMetadata = new CopyExecutionStateStep(
            copyMetadataKey,
            dataStreamCheckBranchingKey,
            ShrinkIndexNameSupplier::getShrinkIndexName,
            isShrunkIndexKey
        );
        BranchingStep isDataStreamBranchingStep = new BranchingStep(
            dataStreamCheckBranchingKey,
            aliasKey,
            replaceDataStreamIndexKey,
            (index, clusterState) -> {
                IndexAbstraction indexAbstraction = clusterState.metadata().getIndicesLookup().get(index.getName());
                assert indexAbstraction != null : "invalid cluster metadata. index [" + index.getName() + "] was not found";
                return indexAbstraction.getParentDataStream() != null;
            }
        );
        ShrinkSetAliasStep aliasSwapAndDelete = new ShrinkSetAliasStep(aliasKey, isShrunkIndexKey, client);
        ReplaceDataStreamBackingIndexStep replaceDataStreamBackingIndex = new ReplaceDataStreamBackingIndexStep(
            replaceDataStreamIndexKey,
            deleteIndexKey,
            ShrinkIndexNameSupplier::getShrinkIndexName
        );
        DeleteStep deleteSourceIndexStep = new DeleteStep(deleteIndexKey, isShrunkIndexKey, client);
        ShrunkenIndexCheckStep waitOnShrinkTakeover = new ShrunkenIndexCheckStep(isShrunkIndexKey, lastOrNextStep);
        UpdateSettingsStep allowWriteAfterShrinkStep = allowWriteAfterShrink
            ? new UpdateSettingsStep(allowWriteKey, nextStepKey, client, CLEAR_WRITE_BLOCK_SETTINGS)
            : null;

        Stream<Step> steps = Stream.of(
            conditionalSkipShrinkStep,
            checkNotWriteIndexStep,
            waitForNoFollowersStep,
            waitUntilTimeSeriesEndTimeStep,
            readOnlyStep,
            checkTargetShardsCountStep,
            cleanupShrinkIndexStep,
            generateUniqueIndexNameStep,
            setSingleNodeStep,
            checkShrinkReadyStep,
            shrink,
            allocated,
            copyMetadata,
            isDataStreamBranchingStep,
            aliasSwapAndDelete,
            waitOnShrinkTakeover,
            replaceDataStreamBackingIndex,
            deleteSourceIndexStep,
            allowWriteAfterShrinkStep
        );

        return steps.filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShrinkAction that = (ShrinkAction) o;
        return Objects.equals(numberOfShards, that.numberOfShards)
            && Objects.equals(maxPrimaryShardSize, that.maxPrimaryShardSize)
            && Objects.equals(allowWriteAfterShrink, that.allowWriteAfterShrink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfShards, maxPrimaryShardSize, allowWriteAfterShrink);
    }

    @Override
    public String toString() {
        return Strings.toString(this);
    }
}
