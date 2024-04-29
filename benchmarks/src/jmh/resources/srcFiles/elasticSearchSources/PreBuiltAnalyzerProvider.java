/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Analyzer;

public class PreBuiltAnalyzerProvider implements AnalyzerProvider<NamedAnalyzer> {

    private final NamedAnalyzer analyzer;

    public PreBuiltAnalyzerProvider(String name, AnalyzerScope scope, Analyzer analyzer) {
        this.analyzer = new NamedAnalyzer(name, scope, analyzer);
    }

    @Override
    public String name() {
        return analyzer.name();
    }

    @Override
    public AnalyzerScope scope() {
        return analyzer.scope();
    }

    @Override
    public NamedAnalyzer get() {
        return analyzer;
    }
}
