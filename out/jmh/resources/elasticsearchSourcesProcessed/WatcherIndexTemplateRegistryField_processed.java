/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.watcher.support;

public final class WatcherIndexTemplateRegistryField {
    public static final int INDEX_TEMPLATE_VERSION = 16;
    public static final String HISTORY_TEMPLATE_NAME = ".watch-history-" + INDEX_TEMPLATE_VERSION;
    public static final String HISTORY_TEMPLATE_NAME_NO_ILM = ".watch-history-no-ilm-" + INDEX_TEMPLATE_VERSION;
    public static final String[] TEMPLATE_NAMES = new String[] { HISTORY_TEMPLATE_NAME };
    public static final String[] TEMPLATE_NAMES_NO_ILM = new String[] { HISTORY_TEMPLATE_NAME_NO_ILM };

    private WatcherIndexTemplateRegistryField() {}
}
