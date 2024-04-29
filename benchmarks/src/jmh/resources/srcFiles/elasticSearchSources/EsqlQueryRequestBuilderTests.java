/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.core.esql.action;

import org.elasticsearch.test.ESIntegTestCase;

import static org.hamcrest.core.IsEqual.equalTo;

public class EsqlQueryRequestBuilderTests extends ESIntegTestCase {

    public void testIllegalStateException() {
        var e = expectThrows(IllegalStateException.class, () -> EsqlQueryRequestBuilder.newRequestBuilder(client()));
        assertThat(e.getMessage(), equalTo("ESQL module not present or initialized"));
    }
}
