/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.test.mockito;

import org.mockito.plugins.MockMaker;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.DomainCombiner;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.function.Supplier;

class SecureMockUtil {

    private static final AccessControlContext context = getContext();

    private static AccessControlContext getContext() {
        ProtectionDomain[] pda = new ProtectionDomain[] { wrap(MockMaker.class::getProtectionDomain) };
        DomainCombiner combiner = (current, assigned) -> pda;
        AccessControlContext acc = new AccessControlContext(AccessController.getContext(), combiner);
        return AccessController.doPrivileged((PrivilegedAction<AccessControlContext>) AccessController::getContext, acc);
    }

    public static void init() {}

    static <T> T wrap(Supplier<T> call) {
        return AccessController.doPrivileged((PrivilegedAction<T>) call::get, context);
    }

    private SecureMockUtil() {}
}
