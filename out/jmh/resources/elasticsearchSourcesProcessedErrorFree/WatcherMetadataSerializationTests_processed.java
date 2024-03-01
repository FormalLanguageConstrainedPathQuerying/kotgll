/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.watcher;

import org.elasticsearch.cluster.ClusterModule;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.metadata.RepositoriesMetadata;
import org.elasticsearch.cluster.metadata.RepositoryMetadata;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ChunkedToXContent;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xpack.core.XPackClientPlugin;
import org.elasticsearch.xpack.core.watcher.WatcherMetadata;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class WatcherMetadataSerializationTests extends ESTestCase {
    public void testXContentSerializationOneSignedWatcher() throws Exception {
        boolean manuallyStopped = randomBoolean();
        WatcherMetadata watcherMetadata = new WatcherMetadata(manuallyStopped);
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        builder.startObject("watcher");
        ChunkedToXContent.wrapAsToXContent(watcherMetadata).toXContent(builder, ToXContent.EMPTY_PARAMS);
        builder.endObject();
        builder.endObject();
        WatcherMetadata watchersMetadataFromXContent = getWatcherMetadataFromXContent(createParser(builder));
        assertThat(watchersMetadataFromXContent.manuallyStopped(), equalTo(manuallyStopped));
    }

    public void testWatcherMetadataParsingDoesNotSwallowOtherMetadata() throws Exception {
        Settings settings = Settings.builder().put("path.home", createTempDir()).build();
        new Watcher(settings);  
        boolean manuallyStopped = randomBoolean();
        WatcherMetadata watcherMetadata = new WatcherMetadata(manuallyStopped);
        RepositoryMetadata repositoryMetadata = new RepositoryMetadata("repo", "fs", Settings.EMPTY);
        RepositoriesMetadata repositoriesMetadata = new RepositoriesMetadata(Collections.singletonList(repositoryMetadata));
        final Metadata.Builder metadataBuilder = Metadata.builder();
        if (randomBoolean()) { 
            metadataBuilder.putCustom(watcherMetadata.getWriteableName(), watcherMetadata);
            metadataBuilder.putCustom(repositoriesMetadata.getWriteableName(), repositoriesMetadata);
        } else {
            metadataBuilder.putCustom(repositoriesMetadata.getWriteableName(), repositoriesMetadata);
            metadataBuilder.putCustom(watcherMetadata.getWriteableName(), watcherMetadata);
        }
        XContentBuilder builder = XContentFactory.jsonBuilder();
        ToXContent.Params params = new ToXContent.MapParams(
            Collections.singletonMap(Metadata.CONTEXT_MODE_PARAM, Metadata.CONTEXT_MODE_GATEWAY)
        );
        builder.startObject();
        builder = ChunkedToXContent.wrapAsToXContent(metadataBuilder.build()).toXContent(builder, params);
        builder.endObject();
        Metadata metadata = Metadata.Builder.fromXContent(createParser(builder));
        assertThat(metadata.custom(watcherMetadata.getWriteableName()), notNullValue());
        assertThat(metadata.custom(repositoriesMetadata.getWriteableName()), notNullValue());
    }

    private static WatcherMetadata getWatcherMetadataFromXContent(XContentParser parser) throws Exception {
        parser.nextToken(); 
        parser.nextToken(); 
        WatcherMetadata watcherMetadataFromXContent = (WatcherMetadata) WatcherMetadata.fromXContent(parser);
        parser.nextToken(); 
        assertThat(parser.nextToken(), nullValue());
        return watcherMetadataFromXContent;
    }

    @Override
    protected NamedXContentRegistry xContentRegistry() {
        return new NamedXContentRegistry(
            Stream.concat(new XPackClientPlugin().getNamedXContent().stream(), ClusterModule.getNamedXWriteables().stream())
                .collect(Collectors.toList())
        );
    }

}
