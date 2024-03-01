/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.common.util;

import org.elasticsearch.test.ESTestCase;

import java.net.URI;

public class URIPatternTests extends ESTestCase {
    public void testURIPattern() throws Exception {
        assertTrue(new URIPattern("http:
        assertFalse(new URIPattern("http:
        assertTrue(new URIPattern("http:
        assertFalse(new URIPattern("http:
        assertTrue(new URIPattern("http:
        assertTrue(new URIPattern("http:
        assertTrue(new URIPattern("http:
        assertFalse(new URIPattern("http:
        assertFalse(new URIPattern("http:
        assertFalse(new URIPattern("http:
        assertFalse(new URIPattern("http:
        assertFalse(new URIPattern("http:
        assertTrue(new URIPattern("http:
        assertTrue(new URIPattern("http:
        assertFalse(new URIPattern("http:
        assertTrue(new URIPattern("http:
        assertFalse(new URIPattern("http:
        assertTrue(new URIPattern("http:
        assertFalse(new URIPattern("http:
        assertTrue(new URIPattern("http:
        assertTrue(new URIPattern("http:
    }
}
