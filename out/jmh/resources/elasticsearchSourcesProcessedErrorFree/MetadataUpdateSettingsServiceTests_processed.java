/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster.metadata;

import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.IndexVersion;
import org.elasticsearch.index.codec.CodecService;
import org.elasticsearch.index.engine.EngineConfig;
import org.elasticsearch.index.translog.Translog;
import org.elasticsearch.test.ESTestCase;

import java.util.Set;

import static org.elasticsearch.index.IndexModule.Type.MMAPFS;
import static org.elasticsearch.index.IndexModule.Type.NIOFS;

public class MetadataUpdateSettingsServiceTests extends ESTestCase {
    private final Index index = new Index("test", UUIDs.randomBase64UUID());
    private final Settings metaSettings = Settings.builder()
        .put(IndexMetadata.SETTING_INDEX_UUID, index.getUUID())
        .put(IndexMetadata.INDEX_NUMBER_OF_SHARDS_SETTING.getKey(), 1)
        .put(IndexMetadata.SETTING_INDEX_VERSION_CREATED.getKey(), IndexVersion.current())
        .build();
    private final IndexScopedSettings indexScopedSettings = new IndexScopedSettings(
        Settings.EMPTY,
        IndexScopedSettings.BUILT_IN_INDEX_SETTINGS
    );

    /**
     * test update dynamic settings for open index, also test override existing settings if preserveExisting is false
     */
    public void testUpdateOpenIndexSettings() {
        Metadata metadata = mockMetadata(
            index,
            Settings.builder()
                .put(metaSettings)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 5)
                .put(IndexSettings.INDEX_REFRESH_INTERVAL_SETTING.getKey(), "5s")
                .build()
        );
        Metadata.Builder metadataBuilder = Metadata.builder(metadata);
        Settings settingToApply = Settings.builder()
            .put(IndexSettings.INDEX_REFRESH_INTERVAL_SETTING.getKey(), "2s")
            .put(IndexSettings.INDEX_TRANSLOG_DURABILITY_SETTING.getKey(), Translog.Durability.ASYNC)
            .build();
        MetadataUpdateSettingsService.updateIndexSettings(
            Set.of(index),
            metadataBuilder,
            (index, indexSettings) -> indexScopedSettings.updateDynamicSettings(
                settingToApply,
                indexSettings,
                Settings.builder(),
                index.getName()
            ),
            false,
            indexScopedSettings
        );
        assertEquals(
            Settings.builder()
                .put(metaSettings)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 5)
                .put(IndexSettings.INDEX_REFRESH_INTERVAL_SETTING.getKey(), "2s")
                .put(IndexSettings.INDEX_TRANSLOG_DURABILITY_SETTING.getKey(), Translog.Durability.ASYNC)
                .build(),
            metadataBuilder.get(index.getName()).getSettings()
        );
    }

    /**
     * test update dynamic and static settings for closed index, also test not override existing settings if preserveExisting is ture
     */
    public void testUpdateClosedIndexSettings() {
        Metadata metadata = mockMetadata(
            index,
            Settings.builder()
                .put(metaSettings)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 5)
                .put(IndexSettings.INDEX_REFRESH_INTERVAL_SETTING.getKey(), "5s")
                .put(EngineConfig.INDEX_CODEC_SETTING.getKey(), CodecService.BEST_COMPRESSION_CODEC)
                .put(IndexModule.INDEX_STORE_TYPE_SETTING.getKey(), NIOFS.getSettingsKey())
                .build()
        );
        Metadata.Builder metadataBuilder = Metadata.builder(metadata);
        Settings settingToApply = Settings.builder()
            .put(IndexSettings.INDEX_REFRESH_INTERVAL_SETTING.getKey(), "2s")
            .put(IndexSettings.INDEX_TRANSLOG_DURABILITY_SETTING.getKey(), Translog.Durability.ASYNC)
            .put(IndexModule.INDEX_STORE_TYPE_SETTING.getKey(), MMAPFS.getSettingsKey())
            .putList(IndexModule.INDEX_STORE_PRE_LOAD_SETTING.getKey(), "dvd", "tmp")
            .build();
        MetadataUpdateSettingsService.updateIndexSettings(
            Set.of(index),
            metadataBuilder,
            (index, indexSettings) -> indexScopedSettings.updateSettings(
                settingToApply,
                indexSettings,
                Settings.builder(),
                index.getName()
            ),
            true,
            indexScopedSettings
        );
        assertEquals(
            Settings.builder()
                .put(metaSettings)
                .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 5)
                .put(IndexSettings.INDEX_REFRESH_INTERVAL_SETTING.getKey(), "5s")
                .put(IndexSettings.INDEX_TRANSLOG_DURABILITY_SETTING.getKey(), Translog.Durability.ASYNC)
                .put(EngineConfig.INDEX_CODEC_SETTING.getKey(), CodecService.BEST_COMPRESSION_CODEC)
                .put(IndexModule.INDEX_STORE_TYPE_SETTING.getKey(), NIOFS.getSettingsKey())
                .putList(IndexModule.INDEX_STORE_PRE_LOAD_SETTING.getKey(), "dvd", "tmp")
                .build(),
            metadataBuilder.get(index.getName()).getSettings()
        );
    }

    private Metadata mockMetadata(Index index, Settings indexSettings) {
        return Metadata.builder()
            .put(IndexMetadata.builder(index.getName()).settings(Settings.builder().put(indexSettings)).build(), true)
            .build();
    }
}
