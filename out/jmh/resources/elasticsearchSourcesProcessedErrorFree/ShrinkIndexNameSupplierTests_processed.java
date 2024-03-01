/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.core.ilm;

import org.elasticsearch.cluster.metadata.LifecycleExecutionState;
import org.elasticsearch.test.ESTestCase;

import static org.elasticsearch.xpack.core.ilm.ShrinkIndexNameSupplier.SHRUNKEN_INDEX_PREFIX;
import static org.elasticsearch.xpack.core.ilm.ShrinkIndexNameSupplier.getShrinkIndexName;
import static org.hamcrest.Matchers.is;

public class ShrinkIndexNameSupplierTests extends ESTestCase {

    public void testGetShrinkIndexName() {
        String sourceIndexName = "test-index";
        {
            String shrinkIndexName = "the-shrink-index";
            LifecycleExecutionState lifecycleExecutionState = LifecycleExecutionState.builder().setShrinkIndexName(shrinkIndexName).build();

            assertThat(getShrinkIndexName(sourceIndexName, lifecycleExecutionState), is(shrinkIndexName));
        }

        {
            assertThat(
                getShrinkIndexName(sourceIndexName, LifecycleExecutionState.builder().build()),
                is(SHRUNKEN_INDEX_PREFIX + sourceIndexName)
            );
        }
    }
}
