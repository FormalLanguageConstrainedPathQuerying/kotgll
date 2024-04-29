/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.mapper.vectors;

import org.apache.lucene.util.BytesRef;
import org.elasticsearch.index.IndexVersion;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.index.IndexVersionUtils;

import java.nio.ByteBuffer;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;

public class VectorEncoderDecoderTests extends ESTestCase {

    public void testVectorDecodingWithOffset() {
        float[] inputFloats = new float[] { 1f, 2f, 3f, 4f };
        float[] expected = new float[] { 2f, 3f, 4f };
        int dims = 3;
        for (IndexVersion version : List.of(
            IndexVersionUtils.randomVersionBetween(
                random(),
                DenseVectorFieldMapper.MAGNITUDE_STORED_INDEX_VERSION,
                IndexVersionUtils.getPreviousVersion(DenseVectorFieldMapper.LITTLE_ENDIAN_FLOAT_STORED_INDEX_VERSION)
            ),
            DenseVectorFieldMapper.LITTLE_ENDIAN_FLOAT_STORED_INDEX_VERSION
        )) {
            ByteBuffer byteBuffer = DenseVectorFieldMapper.ElementType.FLOAT.createByteBuffer(version, 20);
            double magnitude = 0.0;
            for (float f : inputFloats) {
                byteBuffer.putFloat(f);
                magnitude += f * f;
            }
            magnitude /= 4;
            byteBuffer.putFloat((float) magnitude);
            BytesRef floatBytes = new BytesRef(byteBuffer.array());
            floatBytes.length = 16;
            floatBytes.offset = 4;
            float[] outputFloats = new float[dims];
            VectorEncoderDecoder.decodeDenseVector(version, floatBytes, outputFloats);
            assertArrayEquals(outputFloats, expected, 0f);
            assertThat(VectorEncoderDecoder.decodeMagnitude(version, floatBytes), equalTo((float) magnitude));
        }
    }

}
