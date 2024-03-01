/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search.fetch.subphase.highlight;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Query;
import org.elasticsearch.index.mapper.KeywordFieldMapper;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.TextFieldMapper;
import org.elasticsearch.search.fetch.FetchContext;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.fetch.FetchSubPhaseProcessor;
import org.elasticsearch.search.fetch.StoredFieldsSpec;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class HighlightPhase implements FetchSubPhase {

    private final Map<String, Highlighter> highlighters;

    public HighlightPhase(Map<String, Highlighter> highlighters) {
        this.highlighters = highlighters;
    }

    @Override
    public FetchSubPhaseProcessor getProcessor(FetchContext context) {
        if (context.highlight() == null) {
            return null;
        }

        return getProcessor(context, context.highlight(), context.parsedQuery().query());
    }

    public FetchSubPhaseProcessor getProcessor(FetchContext context, SearchHighlightContext highlightContext, Query query) {
        Map<String, Object> sharedCache = new HashMap<>();
        FieldContext fieldContext = contextBuilders(context, highlightContext, query, sharedCache);

        return new FetchSubPhaseProcessor() {
            @Override
            public void setNextReader(LeafReaderContext readerContext) {

            }

            @Override
            public StoredFieldsSpec storedFieldsSpec() {
                return fieldContext.storedFieldsSpec;
            }

            @Override
            public void process(HitContext hitContext) throws IOException {
                Map<String, HighlightField> highlightFields = new HashMap<>();
                Map<String, Function<HitContext, FieldHighlightContext>> contextBuilders = fieldContext.builders;
                for (String field : contextBuilders.keySet()) {
                    FieldHighlightContext fieldContext = contextBuilders.get(field).apply(hitContext);
                    Highlighter highlighter = getHighlighter(fieldContext.field);
                    HighlightField highlightField = highlighter.highlight(fieldContext);
                    if (highlightField != null) {
                        highlightFields.put(field, new HighlightField(field, highlightField.fragments()));
                    }
                }
                hitContext.hit().highlightFields(highlightFields);
            }
        };
    }

    private Highlighter getHighlighter(SearchHighlightContext.Field field) {
        String highlighterType = field.fieldOptions().highlighterType();
        if (highlighterType == null) {
            highlighterType = "unified";
        }
        Highlighter highlighter = highlighters.get(highlighterType);
        if (highlighter == null) {
            throw new IllegalArgumentException("unknown highlighter type [" + highlighterType + "] for the field [" + field.field() + "]");
        }
        return highlighter;
    }

    private record FieldContext(StoredFieldsSpec storedFieldsSpec, Map<String, Function<HitContext, FieldHighlightContext>> builders) {}

    private FieldContext contextBuilders(
        FetchContext context,
        SearchHighlightContext highlightContext,
        Query query,
        Map<String, Object> sharedCache
    ) {
        Map<String, Function<HitContext, FieldHighlightContext>> builders = new LinkedHashMap<>();
        StoredFieldsSpec storedFieldsSpec = StoredFieldsSpec.NO_REQUIREMENTS;
        for (SearchHighlightContext.Field field : highlightContext.fields()) {
            Highlighter highlighter = getHighlighter(field);

            Collection<String> fieldNamesToHighlight = context.getSearchExecutionContext().getMatchingFieldNames(field.field());

            boolean fieldNameContainsWildcards = field.field().contains("*");
            Set<String> storedFields = new HashSet<>();
            boolean sourceRequired = false;
            for (String fieldName : fieldNamesToHighlight) {
                MappedFieldType fieldType = context.getSearchExecutionContext().getFieldType(fieldName);

                if (fieldNameContainsWildcards) {
                    if (fieldType.typeName().equals(TextFieldMapper.CONTENT_TYPE) == false
                        && fieldType.typeName().equals(KeywordFieldMapper.CONTENT_TYPE) == false
                        && fieldType.typeName().equals("match_only_text") == false) {
                        continue;
                    }
                    if (highlighter.canHighlight(fieldType) == false) {
                        continue;
                    }
                }

                if (fieldType.isStored()) {
                    storedFields.add(fieldType.name());
                } else {
                    sourceRequired = true;
                }

                Query highlightQuery = field.fieldOptions().highlightQuery();

                builders.put(
                    fieldName,
                    hc -> new FieldHighlightContext(
                        fieldType.name(),
                        field,
                        fieldType,
                        context,
                        hc,
                        highlightQuery == null ? query : highlightQuery,
                        sharedCache
                    )
                );
            }
            storedFieldsSpec = storedFieldsSpec.merge(new StoredFieldsSpec(sourceRequired, false, storedFields));
        }
        return new FieldContext(storedFieldsSpec, builders);
    }
}
