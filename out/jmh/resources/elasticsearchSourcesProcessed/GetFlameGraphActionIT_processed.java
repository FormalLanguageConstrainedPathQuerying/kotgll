/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.profiling;

public class GetFlameGraphActionIT extends ProfilingTestCase {
    public void testGetStackTracesUnfiltered() throws Exception {
        GetStackTracesRequest request = new GetStackTracesRequest(1000, 600.0d, 1.0d, 1.0d, null, null, null, null, null, null, null, null);
        GetFlamegraphResponse response = client().execute(GetFlamegraphAction.INSTANCE, request).get();
        assertEquals(994, response.getSize());
        assertEquals(1.0d, response.getSamplingRate(), 0.001d);
        assertEquals(46, response.getSelfCPU());
        assertEquals(1903, response.getTotalCPU());
        assertEquals(46, response.getTotalSamples());

        assertEquals("", response.getFileIds().get(0));
        assertEquals(response.getSelfCPU(), response.getCountInclusive().get(0).longValue());
    }
}
