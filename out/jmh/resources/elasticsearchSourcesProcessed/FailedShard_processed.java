/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster.routing.allocation;

import org.elasticsearch.cluster.routing.ShardRouting;

/**
 * A class representing a failed shard.
 */
public record FailedShard(
    ShardRouting routingEntry, 
    String message,            
    Exception failure,         
    boolean markAsStale        
) {
    public FailedShard {
        assert routingEntry.assignedToNode() : "only assigned shards can be failed " + routingEntry;
    }
}
