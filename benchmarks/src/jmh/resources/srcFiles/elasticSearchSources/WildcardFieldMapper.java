/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.wildcard.mapper;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FieldExistsQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.MultiTermQuery.RewriteMethod;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.MinimizationOperations;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.RegExp;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.io.stream.ByteArrayStreamInput;
import org.elasticsearch.common.lucene.BytesRefs;
import org.elasticsearch.common.lucene.Lucene;
import org.elasticsearch.common.lucene.search.AutomatonQueries;
import org.elasticsearch.common.time.DateMathParser;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.index.IndexVersion;
import org.elasticsearch.index.IndexVersions;
import org.elasticsearch.index.analysis.AnalyzerScope;
import org.elasticsearch.index.analysis.LowercaseNormalizer;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.elasticsearch.index.fielddata.FieldDataContext;
import org.elasticsearch.index.fielddata.IndexFieldData;
import org.elasticsearch.index.fielddata.plain.StringBinaryIndexFieldData;
import org.elasticsearch.index.mapper.BinaryFieldMapper.CustomBinaryDocValuesField;
import org.elasticsearch.index.mapper.BlockDocValuesReader;
import org.elasticsearch.index.mapper.BlockLoader;
import org.elasticsearch.index.mapper.DocumentParserContext;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.KeywordFieldMapper;
import org.elasticsearch.index.mapper.LuceneDocument;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.MapperBuilderContext;
import org.elasticsearch.index.mapper.SourceLoader;
import org.elasticsearch.index.mapper.SourceValueFetcher;
import org.elasticsearch.index.mapper.TextSearchInfo;
import org.elasticsearch.index.mapper.ValueFetcher;
import org.elasticsearch.index.query.SearchExecutionContext;
import org.elasticsearch.search.aggregations.support.CoreValuesSourceType;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xpack.wildcard.WildcardDocValuesField;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

/**
 * A {@link FieldMapper} for indexing fields with ngrams for efficient wildcard matching
 */
public class WildcardFieldMapper extends FieldMapper {

    public static final String CONTENT_TYPE = "wildcard";
    public static short MAX_CLAUSES_IN_APPROXIMATION_QUERY = 10;
    public static final int NGRAM_SIZE = 3;

    static final NamedAnalyzer WILDCARD_ANALYZER_7_10 = new NamedAnalyzer("_wildcard_7_10", AnalyzerScope.GLOBAL, new Analyzer() {
        @Override
        public TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer = new NGramTokenizer(NGRAM_SIZE, NGRAM_SIZE);
            TokenStream tok = new LowerCaseFilter(tokenizer);
            tok = new PunctuationFoldingFilter(tok);
            return new TokenStreamComponents(tokenizer::setReader, tok);
        }
    });

    @Deprecated
    static final NamedAnalyzer WILDCARD_ANALYZER_7_9 = new NamedAnalyzer("_wildcard", AnalyzerScope.GLOBAL, new Analyzer() {
        @Override
        public TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer = new NGramTokenizer(NGRAM_SIZE, NGRAM_SIZE);
            TokenStream tok = new LowerCaseFilter(tokenizer);
            return new TokenStreamComponents(tokenizer::setReader, tok);
        }
    });

    public static final class PunctuationFoldingFilter extends TokenFilter {
        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

        /**
         * Create a new PunctuationFoldingFilter, that normalizes token text such that even-numbered ascii values
         * are made odd and punctuation is replaced with /
         *
         * @param in TokenStream to filter
         */
        public PunctuationFoldingFilter(TokenStream in) {
            super(in);
        }

        @Override
        public boolean incrementToken() throws IOException {
            if (input.incrementToken()) {
                normalize(termAtt.buffer(), 0, termAtt.length());
                return true;
            } else return false;
        }

        public static String normalize(String s) {
            char[] chars = s.toCharArray();
            normalize(chars, 0, chars.length);
            return new String(chars);
        }

        /**
         * Normalizes a token
         */
        public static void normalize(final char[] buffer, final int offset, final int limit) {
            assert buffer.length >= limit;
            assert 0 <= offset && offset <= buffer.length;
            for (int i = offset; i < limit;) {
                int codepoint = Character.codePointAt(buffer, i, limit);
                i += Character.toChars(normalize(codepoint), buffer, i);
            }
        }

        private static int normalize(int codepoint) {
            if (codepoint == TOKEN_START_OR_END_CHAR) {
                return codepoint;
            }
            if (Character.isLetterOrDigit(codepoint) == false) {
                return 47;
            }
            if (codepoint > 48 && codepoint <= 128 && codepoint % 2 == 0) {
                return codepoint - 1;
            } else {
                return codepoint;
            }
        }

    }

    public static class Defaults {
        public static final FieldType FIELD_TYPE = new FieldType();
        static {
            FIELD_TYPE.setTokenized(false);
            FIELD_TYPE.setIndexOptions(IndexOptions.DOCS);
            FIELD_TYPE.setStoreTermVectorOffsets(false);
            FIELD_TYPE.setOmitNorms(true);
            FIELD_TYPE.freeze();
        }
        public static final TextSearchInfo TEXT_SEARCH_INFO = new TextSearchInfo(
            FIELD_TYPE,
            null,
            Lucene.KEYWORD_ANALYZER,
            Lucene.KEYWORD_ANALYZER
        );
        public static final int IGNORE_ABOVE = Integer.MAX_VALUE;
    }

    private static WildcardFieldMapper toType(FieldMapper in) {
        return (WildcardFieldMapper) in;
    }

    public static class Builder extends FieldMapper.Builder {

        final Parameter<Integer> ignoreAbove = Parameter.intParam("ignore_above", true, m -> toType(m).ignoreAbove, Defaults.IGNORE_ABOVE)
            .addValidator(v -> {
                if (v < 0) {
                    throw new IllegalArgumentException("[ignore_above] must be positive, got [" + v + "]");
                }
            });
        final Parameter<String> nullValue = Parameter.stringParam("null_value", false, m -> toType(m).nullValue, null).acceptsNull();

        final Parameter<Map<String, String>> meta = Parameter.metaParam();

        final IndexVersion indexVersionCreated;

        public Builder(String name, IndexVersion indexVersionCreated) {
            super(name);
            this.indexVersionCreated = indexVersionCreated;
        }

        @Override
        protected Parameter<?>[] getParameters() {
            return new Parameter<?>[] { ignoreAbove, nullValue, meta };
        }

        Builder ignoreAbove(int ignoreAbove) {
            this.ignoreAbove.setValue(ignoreAbove);
            return this;
        }

        Builder nullValue(String nullValue) {
            this.nullValue.setValue(nullValue);
            return this;
        }

        @Override
        public WildcardFieldMapper build(MapperBuilderContext context) {
            return new WildcardFieldMapper(
                name(),
                new WildcardFieldType(context.buildFullName(name()), nullValue.get(), ignoreAbove.get(), indexVersionCreated, meta.get()),
                ignoreAbove.get(),
                context.isSourceSynthetic(),
                multiFieldsBuilder.build(this, context),
                copyTo,
                nullValue.get(),
                indexVersionCreated
            );
        }
    }

    public static TypeParser PARSER = new TypeParser((n, c) -> new Builder(n, c.indexVersionCreated()));

    public static final char TOKEN_START_OR_END_CHAR = 0;
    public static final String TOKEN_START_STRING = Character.toString(TOKEN_START_OR_END_CHAR);
    public static final String TOKEN_END_STRING = TOKEN_START_STRING + TOKEN_START_STRING;

    public static final class WildcardFieldType extends MappedFieldType {

        static Analyzer lowercaseNormalizer = new LowercaseNormalizer();

        private final String nullValue;
        private final int ignoreAbove;
        private final NamedAnalyzer analyzer;

        private WildcardFieldType(String name, String nullValue, int ignoreAbove, IndexVersion version, Map<String, String> meta) {
            super(name, true, false, true, Defaults.TEXT_SEARCH_INFO, meta);
            if (version.onOrAfter(IndexVersions.V_7_10_0)) {
                this.analyzer = WILDCARD_ANALYZER_7_10;
            } else {
                this.analyzer = WILDCARD_ANALYZER_7_9;
            }
            this.nullValue = nullValue;
            this.ignoreAbove = ignoreAbove;
        }

        @Override
        public boolean mayExistInIndex(SearchExecutionContext context) {
            return context.fieldExistsInIndex(name());
        }

        @Override
        public Query normalizedWildcardQuery(String value, MultiTermQuery.RewriteMethod method, SearchExecutionContext context) {
            return wildcardQuery(value, method, false, context);
        }

        @Override
        public Query wildcardQuery(String wildcardPattern, RewriteMethod method, boolean caseInsensitive, SearchExecutionContext context) {

            String ngramIndexPattern = addLineEndChars(wildcardPattern);
            Set<String> tokens = new LinkedHashSet<>();
            StringBuilder sequence = new StringBuilder();
            int numWildcardChars = 0;
            int numWildcardStrings = 0;
            for (int i = 0; i < ngramIndexPattern.length();) {
                final int c = ngramIndexPattern.codePointAt(i);
                int length = Character.charCount(c);
                switch (c) {
                    case WildcardQuery.WILDCARD_STRING:
                        if (sequence.length() > 0) {
                            getNgramTokens(tokens, sequence.toString());
                            sequence = new StringBuilder();
                        }
                        numWildcardStrings++;
                        break;
                    case WildcardQuery.WILDCARD_CHAR:
                        if (sequence.length() > 0) {
                            getNgramTokens(tokens, sequence.toString());
                            sequence = new StringBuilder();
                        }
                        numWildcardChars++;
                        break;
                    case WildcardQuery.WILDCARD_ESCAPE:
                        if (i + length < ngramIndexPattern.length()) {
                            final int nextChar = ngramIndexPattern.codePointAt(i + length);
                            length += Character.charCount(nextChar);
                            sequence.append(Character.toChars(nextChar));
                        } else {
                            sequence.append(Character.toChars(c));
                        }
                        break;

                    default:
                        sequence.append(Character.toChars(c));
                }
                i += length;
            }

            if (sequence.length() > 0) {
                getNgramTokens(tokens, sequence.toString());
            }

            BooleanQuery.Builder rewritten = new BooleanQuery.Builder();
            int clauseCount = 0;
            for (String string : tokens) {
                if (clauseCount >= MAX_CLAUSES_IN_APPROXIMATION_QUERY) {
                    break;
                }
                addClause(string, rewritten, Occur.MUST);
                clauseCount++;
            }
            Automaton automaton = caseInsensitive
                ? AutomatonQueries.toCaseInsensitiveWildcardAutomaton(new Term(name(), wildcardPattern))
                : WildcardQuery.toAutomaton(new Term(name(), wildcardPattern));
            if (clauseCount > 0) {
                BooleanQuery approxQuery = rewritten.build();
                return new BinaryDvConfirmedAutomatonQuery(approxQuery, name(), wildcardPattern, automaton);
            } else if (numWildcardChars == 0 || numWildcardStrings > 0) {
                return new FieldExistsQuery(name());
            }
            return new BinaryDvConfirmedAutomatonQuery(new MatchAllDocsQuery(), name(), wildcardPattern, automaton);

        }

        @Override
        public Query regexpQuery(
            String value,
            int syntaxFlags,
            int matchFlags,
            int maxDeterminizedStates,
            RewriteMethod method,
            SearchExecutionContext context
        ) {
            if (value.length() == 0) {
                return new MatchNoDocsQuery();
            }

            RegExp regExp = new RegExp(value, syntaxFlags, matchFlags);
            Automaton a = regExp.toAutomaton();
            a = Operations.determinize(a, maxDeterminizedStates);
            a = MinimizationOperations.minimize(a, maxDeterminizedStates);
            if (Operations.isTotal(a)) { 
                return existsQuery(context);
            }

            RegExp ngramRegex = new RegExp(addLineEndChars(value), syntaxFlags, matchFlags);

            Query approxBooleanQuery = toApproximationQuery(ngramRegex);
            Query approxNgramQuery = rewriteBoolToNgramQuery(approxBooleanQuery);

            RegExp regex = new RegExp(value, syntaxFlags, matchFlags);
            Automaton automaton = regex.toAutomaton(maxDeterminizedStates);

            return new BinaryDvConfirmedAutomatonQuery(approxNgramQuery, name(), value, automaton);
        }

        public static Query toApproximationQuery(RegExp r) throws IllegalArgumentException {
            Query result = null;
            switch (r.kind) {
                case REGEXP_UNION:
                    result = createUnionQuery(r);
                    break;
                case REGEXP_CONCATENATION:
                    result = createConcatenationQuery(r);
                    break;
                case REGEXP_STRING:
                    String normalizedString = toLowerCase(r.s);
                    result = new TermQuery(new Term("", normalizedString));
                    break;
                case REGEXP_CHAR:
                    String cs = Character.toString(r.c);
                    String normalizedChar = toLowerCase(cs);
                    result = new TermQuery(new Term("", normalizedChar));
                    break;
                case REGEXP_REPEAT:
                    result = new MatchAllDocsQuery();
                    break;

                case REGEXP_REPEAT_MIN:
                case REGEXP_REPEAT_MINMAX:
                    if (r.min > 0) {
                        result = toApproximationQuery(r.exp1);
                        if (result instanceof TermQuery) {
                            BooleanQuery.Builder wrapper = new BooleanQuery.Builder();
                            wrapper.add(result, Occur.FILTER);
                            result = wrapper.build();
                        }
                    } else {
                        result = new MatchAllDocsQuery();
                    }
                    break;
                case REGEXP_ANYSTRING:
                    result = new MatchAllDocsQuery();
                    break;
                case REGEXP_OPTIONAL:
                case REGEXP_INTERSECTION:
                case REGEXP_COMPLEMENT:
                case REGEXP_CHAR_RANGE:
                case REGEXP_ANYCHAR:
                case REGEXP_INTERVAL:
                case REGEXP_EMPTY:
                case REGEXP_AUTOMATON:
                case REGEXP_PRE_CLASS:
                    result = new MatchAllDocsQuery();
                    break;
            }
            assert result != null; 
            return result;
        }

        private static Query createConcatenationQuery(RegExp r) {
            ArrayList<Query> queries = new ArrayList<>();
            findLeaves(r.exp1, org.apache.lucene.util.automaton.RegExp.Kind.REGEXP_CONCATENATION, queries);
            findLeaves(r.exp2, org.apache.lucene.util.automaton.RegExp.Kind.REGEXP_CONCATENATION, queries);
            BooleanQuery.Builder bAnd = new BooleanQuery.Builder();
            StringBuilder sequence = new StringBuilder();
            for (Query query : queries) {
                if (query instanceof TermQuery tq) {
                    sequence.append(tq.getTerm().text());
                } else {
                    if (sequence.length() > 0) {
                        bAnd.add(new TermQuery(new Term("", sequence.toString())), Occur.FILTER);
                        sequence = new StringBuilder();
                    }
                    bAnd.add(query, Occur.FILTER);
                }
            }
            if (sequence.length() > 0) {
                bAnd.add(new TermQuery(new Term("", sequence.toString())), Occur.FILTER);
            }
            BooleanQuery combined = bAnd.build();
            if (combined.clauses().size() > 0) {
                return combined;
            }
            return new MatchAllDocsQuery();

        }

        private static Query createUnionQuery(RegExp r) {
            ArrayList<Query> queries = new ArrayList<>();
            findLeaves(r.exp1, org.apache.lucene.util.automaton.RegExp.Kind.REGEXP_UNION, queries);
            findLeaves(r.exp2, org.apache.lucene.util.automaton.RegExp.Kind.REGEXP_UNION, queries);
            BooleanQuery.Builder bOr = new BooleanQuery.Builder();
            HashSet<Query> uniqueClauses = new HashSet<>();
            for (Query query : queries) {
                if (uniqueClauses.add(query)) {
                    bOr.add(query, Occur.SHOULD);
                }
            }
            if (uniqueClauses.size() > 0) {
                if (uniqueClauses.size() == 1) {
                    return uniqueClauses.iterator().next();
                } else {
                    return bOr.build();
                }
            }
            return new MatchAllDocsQuery();
        }

        private static void findLeaves(RegExp exp, org.apache.lucene.util.automaton.RegExp.Kind kind, List<Query> queries) {
            if (exp.kind == kind) {
                findLeaves(exp.exp1, kind, queries);
                findLeaves(exp.exp2, kind, queries);
            } else {
                queries.add(toApproximationQuery(exp));
            }
        }

        private static String toLowerCase(String string) {
            return lowercaseNormalizer.normalize(null, string).utf8ToString();
        }

        private Query rewriteBoolToNgramQuery(Query approxQuery) {
            if (approxQuery == null) {
                return null;
            }
            if (approxQuery instanceof BooleanQuery bq) {
                BooleanQuery.Builder rewritten = new BooleanQuery.Builder();
                int clauseCount = 0;
                for (BooleanClause clause : bq) {
                    Query q = rewriteBoolToNgramQuery(clause.getQuery());
                    if (q != null) {
                        if (clause.getOccur().equals(Occur.FILTER)) {
                            clauseCount++;
                            if (clauseCount >= MAX_CLAUSES_IN_APPROXIMATION_QUERY) {
                                break;
                            }
                        }
                        rewritten.add(q, clause.getOccur());
                    }
                }
                return rewritten.build();
            }
            if (approxQuery instanceof TermQuery tq) {

                String s = tq.getTerm().text();
                if (s.equals(WildcardFieldMapper.TOKEN_START_STRING) || s.equals(WildcardFieldMapper.TOKEN_END_STRING)) {
                    return new MatchAllDocsQuery();
                }

                Set<String> tokens = new LinkedHashSet<>();
                getNgramTokens(tokens, s);
                BooleanQuery.Builder rewritten = new BooleanQuery.Builder();
                for (String string : tokens) {
                    addClause(string, rewritten, Occur.FILTER);
                }
                return rewritten.build();
            }
            if (approxQuery instanceof MatchAllDocsQuery) {
                return approxQuery;
            }
            throw new IllegalStateException("Invalid query type found parsing regex query:" + approxQuery);
        }

        private void getNgramTokens(Set<String> tokens, String fragment) {
            if (fragment.equals(TOKEN_START_STRING) || fragment.equals(TOKEN_END_STRING)) {
                return;
            }
            TokenStream tokenizer = analyzer.tokenStream(name(), fragment);
            CharTermAttribute termAtt = tokenizer.addAttribute(CharTermAttribute.class);
            int foundTokens = 0;
            try {
                tokenizer.reset();
                while (tokenizer.incrementToken()) {
                    String tokenValue = termAtt.toString();
                    tokens.add(tokenValue);
                    foundTokens++;
                }
                tokenizer.end();
                tokenizer.close();
            } catch (IOException ioe) {
                throw new ElasticsearchParseException("Error parsing wildcard regex pattern fragment [" + fragment + "]");
            }

            if (foundTokens == 0 && fragment.length() > 0) {
                fragment = toLowerCase(fragment);
                if (analyzer == WILDCARD_ANALYZER_7_10) {
                    fragment = PunctuationFoldingFilter.normalize(fragment);
                }
                tokens.add(fragment);
            }
        }

        private void addClause(String token, BooleanQuery.Builder bqBuilder, Occur occur) {
            assert token.codePointCount(0, token.length()) <= NGRAM_SIZE;
            int tokenSize = token.codePointCount(0, token.length());
            if (tokenSize < 2 || token.equals(WildcardFieldMapper.TOKEN_END_STRING)) {
                bqBuilder.add(new BooleanClause(new MatchAllDocsQuery(), occur));
                return;
            }
            if (tokenSize == NGRAM_SIZE) {
                TermQuery tq = new TermQuery(new Term(name(), token));
                bqBuilder.add(new BooleanClause(tq, occur));
            } else {
                PrefixQuery wq = new PrefixQuery(new Term(name(), token), MultiTermQuery.CONSTANT_SCORE_REWRITE);
                bqBuilder.add(new BooleanClause(wq, occur));
            }
        }

        @Override
        public Query rangeQuery(
            Object lowerTerm,
            Object upperTerm,
            boolean includeLower,
            boolean includeUpper,
            ShapeRelation relation,
            ZoneId timeZone,
            DateMathParser parser,
            SearchExecutionContext context
        ) {
            BytesRef lower = lowerTerm == null ? null : BytesRefs.toBytesRef(lowerTerm);
            BytesRef upper = upperTerm == null ? null : BytesRefs.toBytesRef(upperTerm);
            Query accelerationQuery = null;
            if (lowerTerm != null && upperTerm != null) {
                StringBuilder commonPrefix = new StringBuilder();
                String lowerS = addLineEndChars(lower.utf8ToString());
                String upperS = addLineEndChars(upper.utf8ToString());
                for (int i = 0; i < Math.min(lowerS.length(), upperS.length());) {
                    final int cL = lowerS.codePointAt(i);
                    final int cU = upperS.codePointAt(i);
                    if (cL == cU) {
                        commonPrefix.append(Character.toChars(cL));
                    } else {
                        break;
                    }
                    int length = Character.charCount(cL);
                    i += length;
                }

                if (commonPrefix.length() > 0) {
                    Set<String> tokens = new HashSet<>();
                    getNgramTokens(tokens, commonPrefix.toString());
                    BooleanQuery.Builder bqBuilder = new BooleanQuery.Builder();
                    for (String token : tokens) {
                        int tokenSize = token.codePointCount(0, token.length());
                        if (tokenSize < 2 || token.equals(WildcardFieldMapper.TOKEN_END_STRING)) {
                            continue;
                        }

                        if (tokenSize == NGRAM_SIZE) {
                            TermQuery tq = new TermQuery(new Term(name(), token));
                            bqBuilder.add(new BooleanClause(tq, Occur.FILTER));
                        } else {
                            PrefixQuery wq = new PrefixQuery(new Term(name(), token), MultiTermQuery.CONSTANT_SCORE_REWRITE);
                            bqBuilder.add(new BooleanClause(wq, Occur.FILTER));
                        }
                    }
                    BooleanQuery bq = bqBuilder.build();
                    if (bq.clauses().size() > 0) {
                        accelerationQuery = bq;
                    }
                }
            }
            Automaton automaton = TermRangeQuery.toAutomaton(lower, upper, includeLower, includeUpper);

            if (accelerationQuery == null) {
                return new BinaryDvConfirmedAutomatonQuery(new MatchAllDocsQuery(), name(), lower + "-" + upper, automaton);
            }
            return new BinaryDvConfirmedAutomatonQuery(accelerationQuery, name(), lower + "-" + upper, automaton);
        }

        @Override
        public Query fuzzyQuery(
            Object value,
            Fuzziness fuzziness,
            int prefixLength,
            int maxExpansions,
            boolean transpositions,
            SearchExecutionContext context,
            @Nullable MultiTermQuery.RewriteMethod rewriteMethod
        ) {
            String searchTerm = BytesRefs.toString(value);
            try {
                BooleanQuery.Builder approxBuilder = new BooleanQuery.Builder();

                String postPrefixString = searchTerm;

                if (prefixLength > 0) {
                    Set<String> prefixTokens = new LinkedHashSet<>();
                    postPrefixString = searchTerm.substring(prefixLength);
                    String prefixCandidate = TOKEN_START_OR_END_CHAR + searchTerm.substring(0, prefixLength);
                    getNgramTokens(prefixTokens, prefixCandidate);
                    for (String prefixToken : prefixTokens) {
                        addClause(prefixToken, approxBuilder, Occur.MUST);
                    }
                }
                TokenStream tokenizer = analyzer.tokenStream(name(), postPrefixString);
                CharTermAttribute termAtt = tokenizer.addAttribute(CharTermAttribute.class);
                ArrayList<String> postPrefixTokens = new ArrayList<>();
                String firstToken = null;
                tokenizer.reset();
                int tokenNumber = 0;
                while (tokenizer.incrementToken()) {
                    if (tokenNumber == 0) {
                        String token = termAtt.toString();
                        if (firstToken == null) {
                            firstToken = token;
                        }
                        postPrefixTokens.add(token);
                    }
                    tokenNumber++;
                    if (tokenNumber == 3) {
                        tokenNumber = 0;
                    }
                }
                tokenizer.end();
                tokenizer.close();

                BooleanQuery.Builder ngramBuilder = new BooleanQuery.Builder();
                int numClauses = 0;
                for (String token : postPrefixTokens) {
                    addClause(token, ngramBuilder, Occur.SHOULD);
                    numClauses++;
                }

                if (numClauses > fuzziness.asDistance(searchTerm)) {
                    ngramBuilder.setMinimumNumberShouldMatch(numClauses - fuzziness.asDistance(searchTerm));
                    approxBuilder.add(ngramBuilder.build(), Occur.MUST);
                }

                BooleanQuery ngramQ = approxBuilder.build();

                FuzzyQuery fq = rewriteMethod == null
                    ? new FuzzyQuery(
                        new Term(name(), searchTerm),
                        fuzziness.asDistance(searchTerm),
                        prefixLength,
                        maxExpansions,
                        transpositions
                    )
                    : new FuzzyQuery(
                        new Term(name(), searchTerm),
                        fuzziness.asDistance(searchTerm),
                        prefixLength,
                        maxExpansions,
                        transpositions,
                        rewriteMethod
                    );
                if (ngramQ.clauses().size() == 0) {
                    return new BinaryDvConfirmedAutomatonQuery(new MatchAllDocsQuery(), name(), searchTerm, fq.getAutomata().automaton);
                }

                return new BinaryDvConfirmedAutomatonQuery(ngramQ, name(), searchTerm, fq.getAutomata().automaton);
            } catch (IOException ioe) {
                throw new ElasticsearchParseException("Error parsing wildcard field fuzzy string [" + searchTerm + "]");
            }
        }

        @Override
        public String typeName() {
            return CONTENT_TYPE;
        }

        @Override
        public String familyTypeName() {
            return KeywordFieldMapper.CONTENT_TYPE;
        }

        @Override
        public Query termQuery(Object value, SearchExecutionContext context) {
            String searchTerm = BytesRefs.toString(value);
            return wildcardQuery(escapeWildcardSyntax(searchTerm), MultiTermQuery.CONSTANT_SCORE_REWRITE, false, context);
        }

        private static String escapeWildcardSyntax(String term) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < term.length();) {
                final int c = term.codePointAt(i);
                int length = Character.charCount(c);
                if (c == WildcardQuery.WILDCARD_STRING || c == WildcardQuery.WILDCARD_CHAR || c == WildcardQuery.WILDCARD_ESCAPE) {
                    result.append("\\");
                }
                result.appendCodePoint(c);
                i += length;
            }
            return result.toString();
        }

        @Override
        public Query termQueryCaseInsensitive(Object value, SearchExecutionContext context) {
            String searchTerm = BytesRefs.toString(value);
            return wildcardQuery(escapeWildcardSyntax(searchTerm), MultiTermQuery.CONSTANT_SCORE_REWRITE, true, context);
        }

        @Override
        public Query prefixQuery(
            String value,
            MultiTermQuery.RewriteMethod method,
            boolean caseInsensitive,
            SearchExecutionContext context
        ) {
            return wildcardQuery(escapeWildcardSyntax(value) + "*", method, caseInsensitive, context);
        }

        @Override
        public BlockLoader blockLoader(BlockLoaderContext blContext) {
            if (hasDocValues()) {
                return new BlockDocValuesReader.BytesRefsFromBinaryBlockLoader(name());
            }
            return null;
        }

        @Override
        public IndexFieldData.Builder fielddataBuilder(FieldDataContext fieldDataContext) {
            failIfNoDocValues();
            return (cache, breakerService) -> new StringBinaryIndexFieldData(
                name(),
                CoreValuesSourceType.KEYWORD,
                WildcardDocValuesField::new
            );
        }

        @Override
        public ValueFetcher valueFetcher(SearchExecutionContext context, String format) {
            if (format != null) {
                throw new IllegalArgumentException("Field [" + name() + "] of type [" + typeName() + "] doesn't support formats.");
            }

            return new SourceValueFetcher(name(), context, nullValue) {
                @Override
                protected String parseSourceValue(Object value) {
                    String keywordValue = value.toString();
                    if (keywordValue.length() > ignoreAbove) {
                        return null;
                    }
                    return keywordValue;
                }
            };
        }

    }

    private static final FieldType NGRAM_FIELD_TYPE;

    static {
        FieldType ft = new FieldType(Defaults.FIELD_TYPE);
        ft.setTokenized(true);
        NGRAM_FIELD_TYPE = freezeAndDeduplicateFieldType(ft);
        assert NGRAM_FIELD_TYPE.indexOptions() == IndexOptions.DOCS;
    }

    private final int ignoreAbove;
    private final String nullValue;
    private final IndexVersion indexVersionCreated;
    private final boolean storeIgnored;

    private WildcardFieldMapper(
        String simpleName,
        WildcardFieldType mappedFieldType,
        int ignoreAbove,
        boolean storeIgnored,
        MultiFields multiFields,
        CopyTo copyTo,
        String nullValue,
        IndexVersion indexVersionCreated
    ) {
        super(simpleName, mappedFieldType, multiFields, copyTo);
        this.nullValue = nullValue;
        this.ignoreAbove = ignoreAbove;
        this.storeIgnored = storeIgnored;
        this.indexVersionCreated = indexVersionCreated;
    }

    @Override
    public Map<String, NamedAnalyzer> indexAnalyzers() {
        return Map.of(mappedFieldType.name(), fieldType().analyzer);
    }

    /** Values that have more chars than the return value of this method will
     *  be skipped at parsing time. */
    int ignoreAbove() {
        return ignoreAbove;
    }

    @Override
    public WildcardFieldType fieldType() {
        return (WildcardFieldType) super.fieldType();
    }

    @Override
    protected void parseCreateField(DocumentParserContext context) throws IOException {
        final String value;
        XContentParser parser = context.parser();
        if (parser.currentToken() == XContentParser.Token.VALUE_NULL) {
            value = nullValue;
        } else {
            value = parser.textOrNull();
        }
        LuceneDocument parseDoc = context.doc();

        List<IndexableField> fields = new ArrayList<>();
        if (value != null) {
            if (value.length() <= ignoreAbove) {
                createFields(value, parseDoc, fields);
            } else {
                context.addIgnoredField(name());
                if (storeIgnored) {
                    parseDoc.add(new StoredField(originalName(), new BytesRef(value)));
                }
            }
        }
        parseDoc.addAll(fields);
    }

    private String originalName() {
        return name() + "._original";
    }

    void createFields(String value, LuceneDocument parseDoc, List<IndexableField> fields) {
        String ngramValue = addLineEndChars(value);
        Field ngramField = new Field(fieldType().name(), ngramValue, NGRAM_FIELD_TYPE);
        fields.add(ngramField);

        CustomBinaryDocValuesField dvField = (CustomBinaryDocValuesField) parseDoc.getByKey(fieldType().name());
        if (dvField == null) {
            dvField = new CustomBinaryDocValuesField(fieldType().name(), value.getBytes(StandardCharsets.UTF_8));
            parseDoc.addWithKey(fieldType().name(), dvField);
        } else {
            dvField.add(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    static String addLineEndChars(String value) {
        return TOKEN_START_OR_END_CHAR + value + TOKEN_START_OR_END_CHAR + TOKEN_START_OR_END_CHAR;
    }

    @Override
    protected String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public FieldMapper.Builder getMergeBuilder() {
        return new Builder(simpleName(), indexVersionCreated).init(this);
    }

    @Override
    public SourceLoader.SyntheticFieldLoader syntheticFieldLoader() {
        if (copyTo.copyToFields().isEmpty() != true) {
            throw new IllegalArgumentException(
                "field [" + name() + "] of type [" + typeName() + "] doesn't support synthetic source because it declares copy_to"
            );
        }
        return new WildcardSyntheticFieldLoader();
    }

    private class WildcardSyntheticFieldLoader implements SourceLoader.SyntheticFieldLoader {
        private final ByteArrayStreamInput docValuesStream = new ByteArrayStreamInput();
        private int docValueCount;
        private BytesRef docValueBytes;

        private List<Object> storedValues = emptyList();

        @Override
        public Stream<Map.Entry<String, StoredFieldLoader>> storedFieldLoaders() {
            if (ignoreAbove != Defaults.IGNORE_ABOVE) {
                return Stream.of(Map.entry(originalName(), storedValues -> this.storedValues = storedValues));
            }
            return Stream.empty();
        }

        @Override
        public DocValuesLoader docValuesLoader(LeafReader leafReader, int[] docIdsInLeaf) throws IOException {
            BinaryDocValues values = leafReader.getBinaryDocValues(name());
            if (values == null) {
                docValueCount = 0;
                return null;
            }

            return docId -> {
                if (values.advanceExact(docId) == false) {
                    docValueCount = 0;
                    return hasValue();
                }
                docValueBytes = values.binaryValue();
                docValuesStream.reset(docValueBytes.bytes);
                docValuesStream.setPosition(docValueBytes.offset);
                docValueCount = docValuesStream.readVInt();
                return hasValue();
            };
        }

        @Override
        public boolean hasValue() {
            return docValueCount > 0 || storedValues.isEmpty() == false;
        }

        @Override
        public void write(XContentBuilder b) throws IOException {
            switch (docValueCount + storedValues.size()) {
                case 0:
                    return;
                case 1:
                    b.field(simpleName());
                    break;
                default:
                    b.startArray(simpleName());
            }
            for (int i = 0; i < docValueCount; i++) {
                int length = docValuesStream.readVInt();
                b.utf8Value(docValueBytes.bytes, docValuesStream.getPosition(), length);
                docValuesStream.skipBytes(length);
            }
            for (Object o : storedValues) {
                BytesRef r = (BytesRef) o;
                b.utf8Value(r.bytes, r.offset, r.length);
            }
            if (docValueCount + storedValues.size() > 1) {
                b.endArray();
            }
            storedValues = emptyList();
        }
    }
}
