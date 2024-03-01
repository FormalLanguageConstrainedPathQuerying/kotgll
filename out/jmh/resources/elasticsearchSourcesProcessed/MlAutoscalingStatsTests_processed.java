/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.core.ml.autoscaling;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.test.AbstractWireSerializingTestCase;

import java.io.IOException;

public class MlAutoscalingStatsTests extends AbstractWireSerializingTestCase<MlAutoscalingStats> {

    public static MlAutoscalingStats randomAutoscalingResources() {
        return new MlAutoscalingStats(
            randomIntBetween(0, 100), 
            randomNonNegativeLong(), 
            randomNonNegativeLong(), 
            randomIntBetween(0, 100), 
            randomIntBetween(0, 100), 
            randomNonNegativeLong(), 
            randomIntBetween(0, 100), 
            randomNonNegativeLong(), 
            randomIntBetween(0, 100), 
            randomNonNegativeLong(), 
            randomNonNegativeLong() 
        );
    }

    @Override
    protected Writeable.Reader<MlAutoscalingStats> instanceReader() {
        return MlAutoscalingStats::new;
    }

    @Override
    protected MlAutoscalingStats createTestInstance() {
        return randomAutoscalingResources();
    }

    @Override
    protected MlAutoscalingStats mutateInstance(MlAutoscalingStats instance) throws IOException {
        return null; 
    }
}
