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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.elasticsearch.Build;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.TriFunction;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.LoggingDeprecationHandler;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.IndexVersion;
import org.elasticsearch.index.analysis.AnalyzerScope;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.elasticsearch.index.mapper.MapperRegistry;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.similarity.SimilarityService;
import org.elasticsearch.script.ScriptCompiler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentParserConfiguration;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.core.Strings.format;

/**
 * This service is responsible for verifying index metadata when an index is introduced
 * to the cluster, for example when restarting nodes, importing dangling indices, or restoring
 * an index from a snapshot repository.
 *
 * It performs the following:
 *   - Verifies the index version is not too old.
 *   - Tries to parse the mappings to catch compatibility bugs early.
 *   - Identifies unknown and invalid settings and archives them.
 */
public class IndexMetadataVerifier {

    private static final Logger logger = LogManager.getLogger(IndexMetadataVerifier.class);

    private final Settings settings;
    private final ClusterService clusterService;
    private final XContentParserConfiguration parserConfiguration;
    private final MapperRegistry mapperRegistry;
    private final IndexScopedSettings indexScopedSettings;
    private final ScriptCompiler scriptService;

    public IndexMetadataVerifier(
        Settings settings,
        ClusterService clusterService,
        NamedXContentRegistry xContentRegistry,
        MapperRegistry mapperRegistry,
        IndexScopedSettings indexScopedSettings,
        ScriptCompiler scriptCompiler
    ) {
        this.settings = settings;
        this.clusterService = clusterService;
        this.parserConfiguration = XContentParserConfiguration.EMPTY.withRegistry(xContentRegistry)
            .withDeprecationHandler(LoggingDeprecationHandler.INSTANCE);
        this.mapperRegistry = mapperRegistry;
        this.indexScopedSettings = indexScopedSettings;
        this.scriptService = scriptCompiler;
    }

    /**
     * Checks that the index can be upgraded to the current version of the master node.
     *
     * <p>
     * If the index does not need upgrade it returns the index metadata unchanged, otherwise it returns a modified index metadata. If index
     * cannot be updated the method throws an exception.
     */
    public IndexMetadata verifyIndexMetadata(IndexMetadata indexMetadata, IndexVersion minimumIndexCompatibilityVersion) {
        checkSupportedVersion(indexMetadata, minimumIndexCompatibilityVersion);

        IndexMetadata newMetadata = convertSharedCacheTierPreference(indexMetadata);
        newMetadata = removeTierFiltering(newMetadata);
        newMetadata = archiveOrDeleteBrokenIndexSettings(newMetadata);
        checkMappingsCompatibility(newMetadata);
        return newMetadata;
    }

    /**
     * Check that the index version is compatible. Elasticsearch does not support indices created before the
     * previous major version.
     */
    private static void checkSupportedVersion(IndexMetadata indexMetadata, IndexVersion minimumIndexCompatibilityVersion) {
        boolean isSupportedVersion = indexMetadata.getCompatibilityVersion().onOrAfter(minimumIndexCompatibilityVersion);
        if (isSupportedVersion == false) {
            throw new IllegalStateException(
                "The index "
                    + indexMetadata.getIndex()
                    + " has current compatibility version ["
                    + indexMetadata.getCompatibilityVersion().toReleaseVersion()
                    + "] but the minimum compatible version is ["
                    + minimumIndexCompatibilityVersion.toReleaseVersion()
                    + "]. It should be re-indexed in Elasticsearch "
                    + (Version.CURRENT.major - 1)
                    + ".x before upgrading to "
                    + Build.current().version()
                    + "."
            );
        }
    }

    /**
     * Check that we can parse the mappings.
     *
     * This is not strictly necessary, since we parse the mappings later when loading the index and will
     * catch issues then. But it lets us fail very quickly and clearly: if there is a mapping incompatibility,
     * the node refuses to start instead of starting but having unallocated shards.
     *
     * Note that we don't expect users to encounter mapping incompatibilities, since our index compatibility
     * policy guarantees we can read mappings from previous compatible index versions. A failure here would
     * indicate a compatibility bug (which are unfortunately not that uncommon).
     */
    private void checkMappingsCompatibility(IndexMetadata indexMetadata) {
        try {


            IndexSettings indexSettings = new IndexSettings(indexMetadata, this.settings);

            final Map<String, TriFunction<Settings, IndexVersion, ScriptService, Similarity>> similarityMap = new AbstractMap<>() {
                @Override
                public boolean containsKey(Object key) {
                    return true;
                }

                @Override
                public TriFunction<Settings, IndexVersion, ScriptService, Similarity> get(Object key) {
                    assert key instanceof String : "key must be a string but was: " + key.getClass();
                    return (settings, version, scriptService) -> new BM25Similarity();
                }

                @Override
                public Set<Entry<String, TriFunction<Settings, IndexVersion, ScriptService, Similarity>>> entrySet() {
                    return Collections.emptySet();
                }
            };
            SimilarityService similarityService = new SimilarityService(indexSettings, null, similarityMap);
            final NamedAnalyzer fakeDefault = new NamedAnalyzer("default", AnalyzerScope.INDEX, new Analyzer() {
                @Override
                protected TokenStreamComponents createComponents(String fieldName) {
                    throw new UnsupportedOperationException("shouldn't be here");
                }
            });

            try (
                MapperService mapperService = new MapperService(
                    clusterService,
                    indexSettings,
                    (type, name) -> new NamedAnalyzer(name, AnalyzerScope.INDEX, fakeDefault.analyzer()),
                    parserConfiguration,
                    similarityService,
                    mapperRegistry,
                    () -> null,
                    indexSettings.getMode().idFieldMapperWithoutFieldData(),
                    scriptService
                )
            ) {
                mapperService.merge(indexMetadata, MapperService.MergeReason.MAPPING_RECOVERY);
            }

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse mappings for index [" + indexMetadata.getIndex() + "]", ex);
        }
    }

    /**
     * Identify invalid or unknown index settings and archive them. This leniency allows Elasticsearch to load
     * indices even if they contain old settings that are no longer valid.
     *
     * When we find an invalid setting on a system index, we simply remove it instead of archiving. System indices
     * are managed by Elasticsearch and manual modification of settings is limited and sometimes impossible.
     */
    IndexMetadata archiveOrDeleteBrokenIndexSettings(IndexMetadata indexMetadata) {
        final Settings settings = indexMetadata.getSettings();
        final Settings newSettings;

        if (indexMetadata.isSystem()) {
            newSettings = indexScopedSettings.deleteUnknownOrInvalidSettings(
                settings,
                e -> logger.warn(
                    "{} deleting unknown system index setting: [{}] with value [{}]",
                    indexMetadata.getIndex(),
                    e.getKey(),
                    e.getValue()
                ),
                (e, ex) -> logger.warn(
                    () -> format(
                        "%s deleting invalid system index setting: [%s] with value [%s]",
                        indexMetadata.getIndex(),
                        e.getKey(),
                        e.getValue()
                    ),
                    ex
                )
            );
        } else {
            newSettings = indexScopedSettings.archiveUnknownOrInvalidSettings(
                settings,
                e -> logger.warn(
                    "{} ignoring unknown index setting: [{}] with value [{}]; archiving",
                    indexMetadata.getIndex(),
                    e.getKey(),
                    e.getValue()
                ),
                (e, ex) -> logger.warn(
                    () -> format(
                        "%s ignoring invalid index setting: [%s] with value [%s]; archiving",
                        indexMetadata.getIndex(),
                        e.getKey(),
                        e.getValue()
                    ),
                    ex
                )
            );
        }

        if (newSettings != settings) {
            return IndexMetadata.builder(indexMetadata).settings(newSettings).build();
        } else {
            return indexMetadata;
        }
    }

    /**
     * Convert shared_cache searchable snapshot indices to only specify
     * _tier_preference: data_frozen, removing any pre-existing tier allocation rules.
     */
    static IndexMetadata convertSharedCacheTierPreference(IndexMetadata indexMetadata) {
        if (indexMetadata.isPartialSearchableSnapshot()) {
            final Settings settings = indexMetadata.getSettings();
            final Settings.Builder settingsBuilder = Settings.builder().put(settings);
            settingsBuilder.remove("index.routing.allocation.include._tier");
            settingsBuilder.remove("index.routing.allocation.exclude._tier");
            settingsBuilder.remove("index.routing.allocation.require._tier");
            settingsBuilder.put("index.routing.allocation.include._tier_preference", "data_frozen");
            final Settings newSettings = settingsBuilder.build();
            if (settings.equals(newSettings)) {
                return indexMetadata;
            } else {
                return IndexMetadata.builder(indexMetadata).settings(newSettings).build();
            }
        } else {
            return indexMetadata;
        }
    }

    /**
     * Removes index level ._tier allocation filters, if they exist
     */
    static IndexMetadata removeTierFiltering(IndexMetadata indexMetadata) {
        final Settings settings = indexMetadata.getSettings();
        final Settings.Builder settingsBuilder = Settings.builder().put(settings);
        settingsBuilder.remove("index.routing.allocation.include._tier");
        settingsBuilder.remove("index.routing.allocation.exclude._tier");
        settingsBuilder.remove("index.routing.allocation.require._tier");
        final Settings newSettings = settingsBuilder.build();
        if (settings.equals(newSettings)) {
            return indexMetadata;
        } else {
            return IndexMetadata.builder(indexMetadata).settings(newSettings).build();
        }
    }
}
