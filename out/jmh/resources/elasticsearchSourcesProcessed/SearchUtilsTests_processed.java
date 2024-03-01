/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search;

import org.elasticsearch.test.ESTestCase;

public class SearchUtilsTests extends ESTestCase {

    public void testConfigureMaxClauses() {

        assertEquals(16368, SearchUtils.calculateMaxClauseValue(4, 1023));

        assertEquals(1024, SearchUtils.calculateMaxClauseValue(-1, 1024));

        assertEquals(1024, SearchUtils.calculateMaxClauseValue(1024, 1024));

        assertEquals(5041, SearchUtils.calculateMaxClauseValue(13, 1024));

        assertEquals(26932, SearchUtils.calculateMaxClauseValue(73, 30 * 1024));
    }

}
