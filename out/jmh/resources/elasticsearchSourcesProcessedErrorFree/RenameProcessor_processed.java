/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.ingest.common;

import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.TemplateScript;

import java.util.Map;

/**
 * Processor that allows to rename existing fields. Will throw exception if the field is not present.
 */
public final class RenameProcessor extends AbstractProcessor {

    public static final String TYPE = "rename";
    private final TemplateScript.Factory field;
    private final TemplateScript.Factory targetField;
    private final boolean ignoreMissing;
    private final boolean overrideEnabled;

    RenameProcessor(
        String tag,
        String description,
        TemplateScript.Factory field,
        TemplateScript.Factory targetField,
        boolean ignoreMissing,
        boolean overrideEnabled
    ) {
        super(tag, description);
        this.field = field;
        this.targetField = targetField;
        this.ignoreMissing = ignoreMissing;
        this.overrideEnabled = overrideEnabled;
    }

    TemplateScript.Factory getField() {
        return field;
    }

    TemplateScript.Factory getTargetField() {
        return targetField;
    }

    boolean isIgnoreMissing() {
        return ignoreMissing;
    }

    public boolean isOverrideEnabled() {
        return overrideEnabled;
    }

    @Override
    public IngestDocument execute(IngestDocument document) {
        String path = document.renderTemplate(field);
        if (path.isEmpty() || document.hasField(path, true) == false) {
            if (ignoreMissing) {
                return document;
            } else {
                throw new IllegalArgumentException("field [" + path + "] doesn't exist");
            }
        }

        String target = document.renderTemplate(targetField);
        if (document.hasField(target, true) && overrideEnabled == false) {
            throw new IllegalArgumentException("field [" + target + "] already exists");
        }

        Object value = document.getFieldValue(path, Object.class);
        document.removeField(path);
        try {
            document.setFieldValue(target, value);
        } catch (Exception e) {
            document.setFieldValue(path, value);
            throw e;
        }
        return document;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public static final class Factory implements Processor.Factory {

        private final ScriptService scriptService;

        public Factory(ScriptService scriptService) {
            this.scriptService = scriptService;
        }

        @Override
        public RenameProcessor create(
            Map<String, Processor.Factory> registry,
            String processorTag,
            String description,
            Map<String, Object> config
        ) throws Exception {
            String field = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "field");
            TemplateScript.Factory fieldTemplate = ConfigurationUtils.compileTemplate(TYPE, processorTag, "field", field, scriptService);
            String targetField = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "target_field");
            TemplateScript.Factory targetFieldTemplate = ConfigurationUtils.compileTemplate(
                TYPE,
                processorTag,
                "target_field",
                targetField,
                scriptService
            );
            boolean ignoreMissing = ConfigurationUtils.readBooleanProperty(TYPE, processorTag, config, "ignore_missing", false);
            boolean overrideEnabled = ConfigurationUtils.readBooleanProperty(TYPE, processorTag, config, "override", false);
            return new RenameProcessor(processorTag, description, fieldTemplate, targetFieldTemplate, ignoreMissing, overrideEnabled);
        }
    }
}
