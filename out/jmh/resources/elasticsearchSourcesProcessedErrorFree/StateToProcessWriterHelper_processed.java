/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ml.process;

import org.elasticsearch.common.bytes.BytesReference;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A helper class for writing state to a native process
 */
public final class StateToProcessWriterHelper {

    private StateToProcessWriterHelper() {}

    public static void writeStateToStream(BytesReference source, OutputStream stream) throws IOException {
        int length = source.length();
        while (length > 0 && source.get(length - 1) == 0) {
            --length;
        }
        source.slice(0, length).writeTo(stream);
        stream.write(0);
    }
}
