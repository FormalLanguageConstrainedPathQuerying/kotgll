/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.inference.services.cohere.embeddings;

import java.util.Locale;

/**
 * Defines the type of embedding that the cohere api should return for a request.
 *
 * <p>
 * <a href="https:
 * </p>
 */
public enum CohereEmbeddingType {
    /**
     * Use this when you want to get back the default float embeddings. Valid for all models.
     */
    FLOAT,
    /**
     * Use this when you want to get back signed int8 embeddings. Valid for only v3 models.
     */
    INT8;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static String toLowerCase(CohereEmbeddingType type) {
        return type.toString().toLowerCase(Locale.ROOT);
    }

    public static CohereEmbeddingType fromString(String name) {
        return valueOf(name.trim().toUpperCase(Locale.ROOT));
    }
}
