/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.watcher.transform.script;

import org.elasticsearch.core.TimeValue;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.xpack.core.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.core.watcher.watch.Payload;
import org.elasticsearch.xpack.watcher.support.Variables;

import java.util.Map;

/**
 * A script to transform the results of a watch execution.
 */
public abstract class WatcherTransformScript {

    public static final String[] PARAMETERS = {};

    private final Map<String, Object> params;
    private final Map<String, Object> ctx;

    public WatcherTransformScript(Map<String, Object> params, WatchExecutionContext watcherContext, Payload payload) {
        this.params = params;
        this.ctx = Variables.createCtx(watcherContext, payload);
    }

    public abstract Object execute();

    public Map<String, Object> getParams() {
        return params;
    }

    public Map<String, Object> getCtx() {
        return ctx;
    }

    public interface Factory {
        WatcherTransformScript newInstance(Map<String, Object> params, WatchExecutionContext watcherContext, Payload payload);
    }

    public static ScriptContext<Factory> CONTEXT = new ScriptContext<>(
        "watcher_transform",
        Factory.class,
        200,
        TimeValue.timeValueMillis(0),
        false,
        true
    );
}
