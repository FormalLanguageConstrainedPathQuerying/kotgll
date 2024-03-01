/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster.metadata;

import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.test.AbstractXContentSerializingTestCase;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.json.JsonXContent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.cluster.metadata.DataStreamAlias.DATA_STREAMS_FIELD;
import static org.elasticsearch.cluster.metadata.DataStreamAlias.OLD_FILTER_FIELD;
import static org.elasticsearch.cluster.metadata.DataStreamAlias.WRITE_DATA_STREAM_FIELD;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

public class DataStreamAliasTests extends AbstractXContentSerializingTestCase<DataStreamAlias> {

    @Override
    protected DataStreamAlias doParseInstance(XContentParser parser) throws IOException {
        parser.nextToken();
        parser.nextToken();
        return DataStreamAlias.fromXContent(parser);
    }

    @Override
    protected ToXContent.Params getToXContentParams() {
        return randomBoolean()
            ? new ToXContent.MapParams(Map.of("binary", randomBoolean() ? "true" : "false"))
            : super.getToXContentParams();
    }

    @Override
    protected Writeable.Reader<DataStreamAlias> instanceReader() {
        return DataStreamAlias::new;
    }

    @Override
    protected DataStreamAlias createTestInstance() {
        return DataStreamTestHelper.randomAliasInstance();
    }

    @Override
    protected DataStreamAlias mutateInstance(DataStreamAlias instance) {
        return null;
    }

    public void testUpdate() {
        DataStreamAlias alias = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), null, null);
        DataStreamAlias result = alias.update("ds-3", null, null);
        assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-2", "ds-3"));
        assertThat(result.getWriteDataStream(), nullValue());
        result = alias.update("ds-3", true, null);
        assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-2", "ds-3"));
        assertThat(result.getWriteDataStream(), equalTo("ds-3"));
        result = alias.update("ds-2", null, null);
        assertThat(result, sameInstance(alias));
        result = alias.update("ds-2", false, null);
        assertThat(result, sameInstance(alias));
        result = alias.update("ds-2", true, null);
        assertThat(result, not(sameInstance(alias)));
        assertThat(result.getWriteDataStream(), equalTo("ds-2"));
        alias = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), "ds-2", null);
        result = alias.update("ds-2", false, null);
        assertThat(result, not(sameInstance(alias)));
        assertThat(result.getWriteDataStream(), nullValue());
    }

    public void testUpdateFilter() {
        {
            DataStreamAlias alias = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), null, null);
            DataStreamAlias result = alias.update("ds-2", null, Map.of("term", Map.of("field", "value")));
            assertThat(result, not(sameInstance(alias)));
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-2"));
            assertThat(result.getWriteDataStream(), nullValue());
            assertThat(result.getFilter("ds-1"), nullValue());
            assertThat(result.getFilter("ds-2"), notNullValue());
            assertThat(result.getFilter("ds-2").string(), equalTo("""
                {"term":{"field":"value"}}"""));
        }
        {
            DataStreamAlias alias = new DataStreamAlias(
                "my-alias",
                List.of("ds-1", "ds-2"),
                null,
                Map.of("ds-2", Map.of("term", Map.of("field", "value")))
            );
            DataStreamAlias result = alias.update("ds-2", null, Map.of("term", Map.of("field", "value")));
            assertThat(result, sameInstance(alias));
        }
        {
            DataStreamAlias alias = new DataStreamAlias(
                "my-alias",
                List.of("ds-1", "ds-2"),
                null,
                Map.of("ds-2", Map.of("term", Map.of("field", "value")))
            );
            DataStreamAlias result = alias.update("ds-2", null, Map.of("term", Map.of("field", "value1")));
            assertThat(result, not(sameInstance(alias)));
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-2"));
            assertThat(result.getWriteDataStream(), nullValue());
            assertThat(result.getFilter("ds-1"), nullValue());
            assertThat(result.getFilter("ds-2"), notNullValue());
            assertThat(result.getFilter("ds-2").string(), equalTo("""
                {"term":{"field":"value1"}}"""));
        }
        {
            DataStreamAlias alias = new DataStreamAlias(
                "my-alias",
                List.of("ds-1", "ds-2"),
                null,
                Map.of("ds-2", Map.of("term", Map.of("field", "value")))
            );
            DataStreamAlias result = alias.update("ds-2", null, null);
            assertThat(result, sameInstance(alias));
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-2"));
            assertThat(result.getWriteDataStream(), nullValue());
            assertThat(result.getFilter("ds-1"), nullValue());
            assertThat(result.getFilter("ds-2").string(), equalTo("""
                {"term":{"field":"value"}}"""));
        }
    }

    public void testRemoveDataStream() {
        DataStreamAlias alias = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), null, null);
        DataStreamAlias result = alias.removeDataStream("ds-2");
        assertThat(result, not(sameInstance(alias)));
        assertThat(result.getDataStreams(), containsInAnyOrder("ds-1"));
        assertThat(result.getWriteDataStream(), nullValue());
        alias = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), "ds-2", null);
        result = alias.removeDataStream("ds-2");
        assertThat(result, not(sameInstance(alias)));
        assertThat(result.getDataStreams(), containsInAnyOrder("ds-1"));
        assertThat(result.getWriteDataStream(), nullValue());
        alias = new DataStreamAlias("my-alias", List.of("ds-1"), null, null);
        result = alias.removeDataStream("ds-1");
        assertThat(result, nullValue());
        alias = new DataStreamAlias("my-alias", List.of("ds-1"), null, null);
        result = alias.removeDataStream("ds-2");
        assertThat(result, sameInstance(alias));
    }

    public void testIntersect() {
        {
            DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), null, null);
            DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-2", "ds-3"), null, null);
            DataStreamAlias result = alias1.intersect(s -> alias2.getDataStreams().contains(s));
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-2"));
            assertThat(result.getWriteDataStream(), nullValue());
        }
        {
            DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), "ds-2", null);
            DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-2", "ds-3"), null, null);
            DataStreamAlias result = alias1.intersect(s -> alias2.getDataStreams().contains(s));
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-2"));
            assertThat(result.getWriteDataStream(), equalTo("ds-2"));
        }
        {
            DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), null, null);
            DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-2", "ds-3"), "ds-3", null);
            DataStreamAlias result = alias1.intersect(s -> alias2.getDataStreams().contains(s));
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-2"));
            assertThat(result.getWriteDataStream(), nullValue());
        }
        {
            DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2", "ds-3"), "ds-3", null);
            DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-2", "ds-3"), "ds-2", null);
            DataStreamAlias result = alias1.intersect(s -> alias2.getDataStreams().contains(s));
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-2", "ds-3"));
            assertThat(result.getWriteDataStream(), equalTo("ds-3"));
        }
    }

    public void testRestore() {
        {
            DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), null, null);
            DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-2", "ds-3"), null, null);
            DataStreamAlias result = alias1.restore(alias2, null, null);
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-2", "ds-3"));
            assertThat(result.getWriteDataStream(), nullValue());
        }
        {
            DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), "ds-2", null);
            DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-2", "ds-3"), null, null);
            DataStreamAlias result = alias1.restore(alias2, null, null);
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-2", "ds-3"));
            assertThat(result.getWriteDataStream(), equalTo("ds-2"));
        }
        {
            DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), "ds-2", null);
            DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-2", "ds-3"), "ds-3", null);
            var e = expectThrows(IllegalArgumentException.class, () -> alias1.restore(alias2, null, null));
            assertThat(
                e.getMessage(),
                equalTo(
                    "cannot merge alias [my-alias], write data stream of this [ds-2] and write data stream of other [ds-3] are different"
                )
            );
        }
        {
            DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), "ds-2", null);
            DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-2", "ds-3"), "ds-2", null);
            DataStreamAlias result = alias1.restore(alias2, null, null);
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-2", "ds-3"));
            assertThat(result.getWriteDataStream(), equalTo("ds-2"));
        }
        {
            DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), null, null);
            DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-2", "ds-3"), "ds-3", null);
            DataStreamAlias result = alias1.restore(alias2, null, null);
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-2", "ds-3"));
            assertThat(result.getWriteDataStream(), equalTo("ds-3"));
        }
    }

    public void testRestoreWithRename() {
        {
            DataStreamAlias alias = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), "ds-2", null);
            DataStreamAlias result = alias.restore(null, "ds-2", "ds-3");
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-3"));
            assertThat(result.getWriteDataStream(), equalTo("ds-3"));
        }
        {
            DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), "ds-2", null);
            DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-2", "ds-4", "ds-5"), "ds-2", null);
            DataStreamAlias result = alias1.restore(alias2, "ds-2", "ds-3");
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-2", "ds-3", "ds-4", "ds-5"));
            assertThat(result.getWriteDataStream(), equalTo("ds-3"));
        }
        {
            DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), "ds-2", null);
            DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-3", "ds-4", "ds-5"), "ds-3", null);
            DataStreamAlias result = alias1.restore(alias2, "ds-2", "ds-3");
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-3", "ds-4", "ds-5"));
            assertThat(result.getWriteDataStream(), equalTo("ds-3"));
        }
        {
            DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2", "ds-3"), "ds-3", null);
            DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-2", "ds-4", "ds-5"), "ds-2", null);
            DataStreamAlias result = alias1.restore(alias2, "ds-2", "ds-3");
            assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-2", "ds-3", "ds-4", "ds-5"));
            assertThat(result.getWriteDataStream(), equalTo("ds-3"));
        }
    }

    public void testRestoreDataStreamWithWriteDataStreamThatDoesNotExistInOriginalAlias() {
        DataStreamAlias alias1 = new DataStreamAlias("my-alias", List.of("ds-1", "ds-2"), null, null);
        DataStreamAlias alias2 = new DataStreamAlias("my-alias", List.of("ds-2", "ds-3"), "ds-3", null);
        DataStreamAlias result = alias1.restore(alias2, "ds-3", null);
        assertThat(result.getDataStreams(), containsInAnyOrder("ds-1", "ds-2", "ds-3"));
        assertThat(result.getWriteDataStream(), equalTo("ds-3"));
    }

    public void testSupportsXContentWithSingleFilter() throws IOException {
        /*
         * Before 8.7.0, DataStreamAlias only supported a single filter shared by all DataStreams. As of 8.7.0 the "filter" XContent field
         * is no longer written, but this tests that we can still read it (needed when the cluster state is read on a cluster upgrade).
         */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XContentBuilder builder = new XContentBuilder(JsonXContent.jsonXContent, baos);
        builder.startObject();
        String aliasName = randomAlphaOfLength(20);
        builder.startObject(aliasName);
        List<String> dataStreams = randomList(10, () -> randomAlphaOfLength(20));
        builder.stringListField(DATA_STREAMS_FIELD.getPreferredName(), dataStreams);
        String writeDataStream = dataStreams.isEmpty() ? null : randomFrom(dataStreams);
        if (writeDataStream != null) {
            builder.field(WRITE_DATA_STREAM_FIELD.getPreferredName(), writeDataStream);
        }
        boolean binary = randomBoolean();
        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put(randomAlphaOfLength(10), randomAlphaOfLength(20));
        CompressedXContent filter = randomBoolean() ? null : new CompressedXContent(filterMap);
        if (filter != null) {
            if (binary) {
                builder.field(OLD_FILTER_FIELD.getPreferredName(), filter.compressed());
            } else {
                builder.field(OLD_FILTER_FIELD.getPreferredName(), XContentHelper.convertToMap(filter.uncompressed(), true).v2());
            }
        }
        builder.endObject();
        builder.endObject();
        builder.close();
        try (XContentParser parser = createParser(JsonXContent.jsonXContent, baos.toByteArray())) {
            parser.nextToken();
            parser.nextToken();
            DataStreamAlias outputAlias = DataStreamAlias.fromXContent(parser);
            assertNotNull(outputAlias);
            assertThat(outputAlias.getName(), equalTo(aliasName));
            assertThat(outputAlias.getDataStreams(), equalTo(dataStreams));
            assertThat(outputAlias.getWriteDataStream(), equalTo(writeDataStream));
            if (filter == null) {
                assertTrue(outputAlias.dataStreamToFilterMap.isEmpty());
            } else {
                assertThat(outputAlias.dataStreamToFilterMap.size(), equalTo(dataStreams.size()));
                for (String dataStreamName : dataStreams) {
                    assertThat(outputAlias.getFilter(dataStreamName), equalTo(filter));
                }
            }
        }
    }
}
