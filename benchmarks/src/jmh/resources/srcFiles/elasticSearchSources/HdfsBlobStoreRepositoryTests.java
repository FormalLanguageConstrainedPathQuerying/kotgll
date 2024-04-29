/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.repositories.hdfs;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakFilters;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.repositories.blobstore.ESBlobStoreRepositoryIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.fixtures.hdfs.HdfsClientThreadLeakFilter;

import java.util.Collection;
import java.util.Collections;

@ThreadLeakFilters(filters = HdfsClientThreadLeakFilter.class)
@ESIntegTestCase.ClusterScope(numDataNodes = 1, supportsDedicatedMasters = false)
public class HdfsBlobStoreRepositoryTests extends ESBlobStoreRepositoryIntegTestCase {

    @Override
    protected String repositoryType() {
        return "hdfs";
    }

    @Override
    protected Settings repositorySettings(String repoName) {
        return Settings.builder()
            .put("uri", "hdfs:
            .put("conf.fs.AbstractFileSystem.hdfs.impl", TestingFs.class.getName())
            .put("path", "foo")
            .put("chunk_size", randomIntBetween(100, 1000) + "k")
            .put("compress", randomBoolean())
            .build();
    }

    @Override
    public void testSnapshotAndRestore() throws Exception {
        testSnapshotAndRestore(false);
    }

    @Override
    public void testBlobStoreBulkDeletion() throws Exception {
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singletonList(HdfsPlugin.class);
    }
}
