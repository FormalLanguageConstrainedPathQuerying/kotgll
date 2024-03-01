/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.security.authz;

import java.util.Arrays;
import java.util.List;

public final class IndicesAndAliasesResolverField {
    public static final String NO_INDEX_PLACEHOLDER = "-*";
    public static final String[] NO_INDICES_OR_ALIASES_ARRAY = new String[] { "*", "-*" };
    public static final List<String> NO_INDICES_OR_ALIASES_LIST = Arrays.asList(NO_INDICES_OR_ALIASES_ARRAY);

    private IndicesAndAliasesResolverField() {}

}
