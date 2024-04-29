/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.security.authz.privilege;

import org.elasticsearch.action.search.TransportSearchShardsAction;
import org.elasticsearch.index.seqno.RetentionLeaseActions;
import org.elasticsearch.index.seqno.RetentionLeaseBackgroundSyncAction;
import org.elasticsearch.index.seqno.RetentionLeaseSyncAction;
import org.elasticsearch.persistent.CompletionPersistentTaskAction;
import org.elasticsearch.transport.TransportActionProxy;
import org.elasticsearch.xpack.core.security.action.ActionTypes;
import org.elasticsearch.xpack.core.security.support.StringMatcher;

import java.util.Collections;
import java.util.function.Predicate;

public final class SystemPrivilege extends Privilege {

    public static SystemPrivilege INSTANCE = new SystemPrivilege();

    private static final Predicate<String> ALLOWED_ACTIONS = StringMatcher.of(
        "internal:*",
        "indices:monitor/*", 
        "cluster:monitor/*",  
        "cluster:admin/bootstrap/*", 
        "cluster:admin/reroute", 
        "indices:admin/mapping/put", 
        "indices:admin/mapping/auto_put", 
        "indices:admin/template/put", 
        "indices:admin/template/delete", 
        "indices:admin/seq_no/global_checkpoint_sync*", 
        RetentionLeaseSyncAction.ACTION_NAME + "*", 
        RetentionLeaseBackgroundSyncAction.ACTION_NAME + "*", 
        RetentionLeaseActions.ADD.name() + "*", 
        RetentionLeaseActions.REMOVE.name() + "*", 
        RetentionLeaseActions.RENEW.name() + "*", 
        "indices:admin/settings/update", 
        CompletionPersistentTaskAction.NAME, 
        "indices:data/write/*", 
        "indices:data/read/*", 
        "indices:admin/refresh", 
        "indices:admin/aliases", 
        TransportSearchShardsAction.TYPE.name(), 
        ActionTypes.RELOAD_REMOTE_CLUSTER_CREDENTIALS_ACTION.name() 
    );

    private static final Predicate<String> PREDICATE = (action) -> {
        if (TransportActionProxy.isProxyAction(action)) {
            return ALLOWED_ACTIONS.test(TransportActionProxy.unwrapAction(action));
        } else {
            return ALLOWED_ACTIONS.test(action);
        }
    };

    private SystemPrivilege() {
        super(Collections.singleton("internal"));
    }

    @Override
    public Predicate<String> predicate() {
        return PREDICATE;
    }
}
