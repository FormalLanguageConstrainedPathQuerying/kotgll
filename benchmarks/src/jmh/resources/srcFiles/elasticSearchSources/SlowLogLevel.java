/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index;

import java.util.Locale;

/**
 * Legacy enum class for index settings, kept for 7.x BWC compatibility. Do not use.
 * TODO: Remove in 9.0
 */
@Deprecated
public enum SlowLogLevel {
    WARN(3), 
    INFO(2),
    DEBUG(1),
    TRACE(0); 

    private final int specificity;

    SlowLogLevel(int specificity) {
        this.specificity = specificity;
    }

    public static SlowLogLevel parse(String level) {
        return valueOf(level.toUpperCase(Locale.ROOT));
    }

    boolean isLevelEnabledFor(SlowLogLevel levelToBeUsed) {
        return this.specificity <= levelToBeUsed.specificity;
    }
}
