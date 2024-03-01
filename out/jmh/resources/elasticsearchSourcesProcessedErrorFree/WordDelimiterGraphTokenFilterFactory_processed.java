/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.analysis.common;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterIterator;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.elasticsearch.index.analysis.Analysis;
import org.elasticsearch.index.analysis.TokenFilterFactory;

import java.util.List;
import java.util.Set;

import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.CATENATE_ALL;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.CATENATE_NUMBERS;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.CATENATE_WORDS;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.GENERATE_NUMBER_PARTS;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.GENERATE_WORD_PARTS;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.IGNORE_KEYWORDS;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.PRESERVE_ORIGINAL;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.SPLIT_ON_CASE_CHANGE;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.SPLIT_ON_NUMERICS;
import static org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter.STEM_ENGLISH_POSSESSIVE;
import static org.elasticsearch.analysis.common.WordDelimiterTokenFilterFactory.parseTypes;

public class WordDelimiterGraphTokenFilterFactory extends AbstractTokenFilterFactory {

    private final byte[] charTypeTable;
    private final int flags;
    private final CharArraySet protoWords;
    private final boolean adjustOffsets;

    @SuppressWarnings("HiddenField")
    public WordDelimiterGraphTokenFilterFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        super(name, settings);

        List<String> charTypeTableValues = Analysis.getWordList(env, settings, "type_table");
        if (charTypeTableValues == null) {
            this.charTypeTable = WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE;
        } else {
            this.charTypeTable = parseTypes(charTypeTableValues);
        }
        int flags = 0;
        flags |= getFlag(GENERATE_WORD_PARTS, settings, "generate_word_parts", true);
        flags |= getFlag(GENERATE_NUMBER_PARTS, settings, "generate_number_parts", true);
        flags |= getFlag(CATENATE_WORDS, settings, "catenate_words", false);
        flags |= getFlag(CATENATE_NUMBERS, settings, "catenate_numbers", false);
        flags |= getFlag(CATENATE_ALL, settings, "catenate_all", false);
        flags |= getFlag(SPLIT_ON_CASE_CHANGE, settings, "split_on_case_change", true);
        flags |= getFlag(PRESERVE_ORIGINAL, settings, "preserve_original", false);
        flags |= getFlag(SPLIT_ON_NUMERICS, settings, "split_on_numerics", true);
        flags |= getFlag(STEM_ENGLISH_POSSESSIVE, settings, "stem_english_possessive", true);
        flags |= getFlag(IGNORE_KEYWORDS, settings, "ignore_keywords", false);
        Set<?> protectedWords = Analysis.getWordSet(env, settings, "protected_words");
        this.protoWords = protectedWords == null ? null : CharArraySet.copy(protectedWords);
        this.flags = flags;
        this.adjustOffsets = settings.getAsBoolean("adjust_offsets", true);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new WordDelimiterGraphFilter(tokenStream, adjustOffsets, charTypeTable, flags, protoWords);
    }

    @Override
    public TokenFilterFactory getSynonymFilter() {
        throw new IllegalArgumentException("Token filter [" + name() + "] cannot be used to parse synonyms");
    }

    private static int getFlag(int flag, Settings settings, String key, boolean defaultValue) {
        if (settings.getAsBoolean(key, defaultValue)) {
            return flag;
        }
        return 0;
    }
}
