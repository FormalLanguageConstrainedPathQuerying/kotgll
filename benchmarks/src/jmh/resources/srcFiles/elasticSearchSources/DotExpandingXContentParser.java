/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.mapper;

import org.elasticsearch.core.CheckedFunction;
import org.elasticsearch.xcontent.FilterXContentParser;
import org.elasticsearch.xcontent.FilterXContentParserWrapper;
import org.elasticsearch.xcontent.XContentLocation;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentSubParser;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * An XContentParser that reinterprets field names containing dots as an object structure.
 *
 * A field name named {@code "foo.bar.baz":...} will be parsed instead as {@code 'foo':{'bar':{'baz':...}}}.
 * The token location is preserved so that error messages refer to the original content being parsed.
 * This parser can output duplicate keys, but that is fine given that it's used for document parsing. The mapping
 * lookups will return the same mapper/field type, and we never load incoming documents in a map where duplicate
 * keys would end up overriding each other.
 */
class DotExpandingXContentParser extends FilterXContentParserWrapper {

    private static final class WrappingParser extends FilterXContentParser {

        private final ContentPath contentPath;
        final Deque<XContentParser> parsers = new ArrayDeque<>();

        WrappingParser(XContentParser in, ContentPath contentPath) throws IOException {
            this.contentPath = contentPath;
            parsers.push(in);
            if (in.currentToken() == Token.FIELD_NAME) {
                expandDots(in);
            }
        }

        @Override
        public Token nextToken() throws IOException {
            Token token;
            XContentParser delegate;
            while ((token = (delegate = parsers.peek()).nextToken()) == null) {
                parsers.pop();
                if (parsers.isEmpty()) {
                    return null;
                }
            }
            if (token != Token.FIELD_NAME) {
                return token;
            }
            expandDots(delegate);
            return Token.FIELD_NAME;
        }

        private void expandDots(XContentParser delegate) throws IOException {
            if (contentPath.isWithinLeafObject()) {
                return;
            }
            String field = delegate.currentName();
            int length = field.length();
            if (length == 0) {
                throw new IllegalArgumentException("field name cannot be an empty string");
            }
            final int dotCount = FieldTypeLookup.dotCount(field);
            if (dotCount == 0) {
                return;
            }
            doExpandDots(delegate, field, dotCount);
        }

        private void doExpandDots(XContentParser delegate, String field, int dotCount) throws IOException {
            int next;
            int offset = 0;
            String[] list = new String[dotCount + 1];
            int listIndex = 0;
            for (int i = 0; i < dotCount; i++) {
                next = field.indexOf('.', offset);
                list[listIndex++] = field.substring(offset, next);
                offset = next + 1;
            }

            list[listIndex] = field.substring(offset);

            int resultSize = list.length;
            while (resultSize > 0 && list[resultSize - 1].isEmpty()) {
                resultSize--;
            }
            if (resultSize == 0) {
                throw new IllegalArgumentException("field name cannot contain only dots");
            }
            final String[] subpaths;
            if (resultSize == list.length) {
                for (String part : list) {
                    if (part.isBlank()) {
                        throwOnBlankOrEmptyPart(field, part);
                    }
                }
                subpaths = list;
            } else {
                if (resultSize == 1 && field.endsWith(".") == false) {
                    return;
                }
                subpaths = extractAndValidateResults(field, list, resultSize);
            }
            pushSubParser(delegate, subpaths);
        }

        private void pushSubParser(XContentParser delegate, String[] subpaths) throws IOException {
            XContentLocation location = delegate.getTokenLocation();
            Token token = delegate.nextToken();
            final XContentParser subParser;
            if (token == Token.START_OBJECT || token == Token.START_ARRAY) {
                subParser = new XContentSubParser(delegate);
            } else {
                if (token == Token.END_OBJECT || token == Token.END_ARRAY) {
                    throwExpectedOpen(token);
                }
                subParser = new SingletonValueXContentParser(delegate);
            }
            parsers.push(new DotExpandingXContentParser(subParser, subpaths, location, contentPath));
        }

        private static void throwExpectedOpen(Token token) {
            throw new IllegalStateException("Expecting START_OBJECT or START_ARRAY or VALUE but got [" + token + "]");
        }

        private static String[] extractAndValidateResults(String field, String[] list, int resultSize) {
            final String[] subpaths = new String[resultSize];
            for (int i = 0; i < resultSize; i++) {
                String part = list[i];
                if (part.isBlank()) {
                    throwOnBlankOrEmptyPart(field, part);
                }
                subpaths[i] = part;
            }
            return subpaths;
        }

        private static void throwOnBlankOrEmptyPart(String field, String part) {
            if (part.isEmpty()) {
                throw new IllegalArgumentException("field name cannot contain only whitespace: ['" + field + "']");
            }
            throw new IllegalArgumentException(
                "field name starting or ending with a [.] makes object resolution ambiguous: [" + field + "]"
            );
        }

        @Override
        protected XContentParser delegate() {
            return parsers.peek();
        }

        /*
        The following methods (map* and list*) are known not be called by DocumentParser when parsing documents, but we support indexing
        percolator queries which are also parsed through DocumentParser, and their parsing code is completely up to each query, which are
        also pluggable. That means that this parser needs to fully support parsing arbitrary content, when dots expansion is turned off.
        We do throw UnsupportedOperationException when dots expansion is enabled as we don't expect such methods to be ever called in
        those circumstances.
         */

        @Override
        public Map<String, Object> map() throws IOException {
            if (contentPath.isWithinLeafObject()) {
                return super.map();
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, Object> mapOrdered() throws IOException {
            if (contentPath.isWithinLeafObject()) {
                return super.mapOrdered();
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, String> mapStrings() throws IOException {
            if (contentPath.isWithinLeafObject()) {
                return super.mapStrings();
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Map<String, T> map(Supplier<Map<String, T>> mapFactory, CheckedFunction<XContentParser, T, IOException> mapValueParser)
            throws IOException {
            if (contentPath.isWithinLeafObject()) {
                return super.map(mapFactory, mapValueParser);
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Object> list() throws IOException {
            if (contentPath.isWithinLeafObject()) {
                return super.list();
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Object> listOrderedMap() throws IOException {
            if (contentPath.isWithinLeafObject()) {
                return super.listOrderedMap();
            }
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Wraps an XContentParser such that it re-interprets dots in field names as an object structure
     * @param in    the parser to wrap
     * @return  the wrapped XContentParser
     */
    static XContentParser expandDots(XContentParser in, ContentPath contentPath) throws IOException {
        return new WrappingParser(in, contentPath);
    }

    private enum State {
        EXPANDING_START_OBJECT,
        PARSING_ORIGINAL_CONTENT,
        ENDING_EXPANDED_OBJECT
    }

    private final ContentPath contentPath;

    private String[] subPaths;
    private XContentLocation currentLocation;
    private int expandedTokens = 0;
    private int innerLevel = -1;
    private State state = State.EXPANDING_START_OBJECT;

    private DotExpandingXContentParser(
        XContentParser subparser,
        String[] subPaths,
        XContentLocation startLocation,
        ContentPath contentPath
    ) {
        super(subparser);
        this.subPaths = subPaths;
        this.currentLocation = startLocation;
        this.contentPath = contentPath;
    }

    @Override
    public Token nextToken() throws IOException {
        if (state == State.EXPANDING_START_OBJECT) {
            expandedTokens++;
            assert expandedTokens < subPaths.length * 2;
            if (expandedTokens == subPaths.length * 2 - 1) {
                state = State.PARSING_ORIGINAL_CONTENT;
                Token token = delegate().currentToken();
                if (token == Token.START_OBJECT || token == Token.START_ARRAY) {
                    innerLevel++;
                }
                return token;
            }
            if (expandedTokens % 2 == 0) {
                int currentIndex = expandedTokens / 2;
                if (currentIndex < subPaths.length - 1 && contentPath.isWithinLeafObject()) {
                    String[] newSubPaths = new String[currentIndex + 1];
                    StringBuilder collapsedPath = new StringBuilder();
                    for (int i = 0; i < subPaths.length; i++) {
                        if (i < currentIndex) {
                            newSubPaths[i] = subPaths[i];
                        } else {
                            collapsedPath.append(subPaths[i]);
                            if (i < subPaths.length - 1) {
                                collapsedPath.append(".");
                            }
                        }
                    }
                    newSubPaths[currentIndex] = collapsedPath.toString();
                    subPaths = newSubPaths;
                }
                return Token.FIELD_NAME;
            }
            return Token.START_OBJECT;
        }
        if (state == State.PARSING_ORIGINAL_CONTENT) {
            Token token = delegate().nextToken();
            if (token == Token.START_OBJECT || token == Token.START_ARRAY) {
                innerLevel++;
            }
            if (token == Token.END_OBJECT || token == Token.END_ARRAY) {
                innerLevel--;
            }
            if (token != null) {
                return token;
            }
            currentLocation = getTokenLocation();
            state = State.ENDING_EXPANDED_OBJECT;
        }
        assert expandedTokens % 2 == 1;
        expandedTokens -= 2;
        return expandedTokens < 0 ? null : Token.END_OBJECT;
    }

    @Override
    public XContentLocation getTokenLocation() {
        if (state == State.PARSING_ORIGINAL_CONTENT) {
            return super.getTokenLocation();
        }
        return currentLocation;
    }

    @Override
    public Token currentToken() {
        return switch (state) {
            case EXPANDING_START_OBJECT -> expandedTokens % 2 == 1 ? Token.START_OBJECT : Token.FIELD_NAME;
            case ENDING_EXPANDED_OBJECT -> Token.END_OBJECT;
            case PARSING_ORIGINAL_CONTENT -> delegate().currentToken();
        };
    }

    @Override
    public String currentName() throws IOException {
        if (state == State.PARSING_ORIGINAL_CONTENT) {
            assert expandedTokens == subPaths.length * 2 - 1;
            if (innerLevel > 0) {
                return delegate().currentName();
            }
            Token token = currentToken();
            if (innerLevel == 0 && token != Token.START_OBJECT && token != Token.START_ARRAY) {
                return delegate().currentName();
            }
        }
        return subPaths[expandedTokens / 2];
    }

    @Override
    public void skipChildren() throws IOException {
        if (state == State.EXPANDING_START_OBJECT) {
            delegate().skipChildren();
            state = State.ENDING_EXPANDED_OBJECT;
        }
        if (state == State.PARSING_ORIGINAL_CONTENT) {
            delegate().skipChildren();
        }
    }

    @Override
    public String textOrNull() throws IOException {
        if (state == State.EXPANDING_START_OBJECT) {
            throw new IllegalStateException("Can't get text on a " + currentToken() + " at " + getTokenLocation());
        }
        return super.textOrNull();
    }

    @Override
    public Number numberValue() throws IOException {
        if (state == State.EXPANDING_START_OBJECT) {
            throw new IllegalStateException("Can't get numeric value on a " + currentToken() + " at " + getTokenLocation());
        }
        return super.numberValue();
    }

    @Override
    public boolean booleanValue() throws IOException {
        if (state == State.EXPANDING_START_OBJECT) {
            throw new IllegalStateException("Can't get boolean value on a " + currentToken() + " at " + getTokenLocation());
        }
        return super.booleanValue();
    }

    private static class SingletonValueXContentParser extends FilterXContentParserWrapper {

        protected SingletonValueXContentParser(XContentParser in) {
            super(in);
        }

        @Override
        public Token nextToken() throws IOException {
            return null;
        }
    }
}
