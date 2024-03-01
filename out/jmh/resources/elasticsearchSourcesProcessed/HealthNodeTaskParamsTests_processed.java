/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.health.node.selection;

import org.elasticsearch.TransportVersion;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.test.AbstractNamedWriteableTestCase;

import java.io.IOException;

public class HealthNodeTaskParamsTests extends AbstractNamedWriteableTestCase<HealthNodeTaskParams> {


    @Override
    protected HealthNodeTaskParams createTestInstance() {
        return new HealthNodeTaskParams();
    }

    @Override
    protected HealthNodeTaskParams mutateInstance(HealthNodeTaskParams instance) {
        return null;
    }

    @Override
    protected HealthNodeTaskParams copyInstance(HealthNodeTaskParams instance, TransportVersion version) throws IOException {
        return new HealthNodeTaskParams();
    }

    @Override
    protected NamedWriteableRegistry getNamedWriteableRegistry() {
        return new NamedWriteableRegistry(HealthNodeTaskExecutor.getNamedWriteables());
    }

    @Override
    protected Class<HealthNodeTaskParams> categoryClass() {
        return HealthNodeTaskParams.class;
    }
}
