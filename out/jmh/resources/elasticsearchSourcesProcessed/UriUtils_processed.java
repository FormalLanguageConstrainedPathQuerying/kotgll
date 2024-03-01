/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.sql.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.elasticsearch.xpack.sql.client.StringUtils.repeatString;

public final class UriUtils {
    private UriUtils() {

    }

    static final String HTTP_SCHEME = "http";
    static final String HTTPS_SCHEME = "https";
    static final String HTTP_PREFIX = HTTP_SCHEME + ":
    static final String HTTPS_PREFIX = HTTPS_SCHEME + ":

    /**
     * Parses the URL provided by the user and substitutes any missing parts in it with defaults read from the defaultURI.
     * The result is returned as an URI object.
     * In case of a parsing exception, the credentials are redacted from the URISyntaxException message.
     */
    public static URI parseURI(String connectionString, URI defaultURI) {
        final URI uri = parseMaybeWithScheme(connectionString, defaultURI.getScheme() + ":
        final String scheme = uri.getScheme() != null ? uri.getScheme() : defaultURI.getScheme();
        final String host = uri.getHost() != null ? uri.getHost() : defaultURI.getHost();
        final String path = "".equals(uri.getPath()) ? defaultURI.getPath() : uri.getPath();
        final String rawQuery = uri.getQuery() == null ? defaultURI.getRawQuery() : uri.getRawQuery();
        final String rawFragment = uri.getFragment() == null ? defaultURI.getRawFragment() : uri.getRawFragment();
        final int port = uri.getPort() < 0 ? defaultURI.getPort() : uri.getPort();
        try {
            String connStr = new URI(scheme, uri.getUserInfo(), host, port, path, null, null).toString();
            if (StringUtils.hasLength(rawQuery)) {
                connStr += "?" + rawQuery;
            }
            if (StringUtils.hasLength(rawFragment)) {
                connStr += "#" + rawFragment;
            }
            return new URI(connStr);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid connection configuration [" + connectionString + "]: " + e.getMessage(), e);
        }
    }

    private static URI parseMaybeWithScheme(String connectionString, String defaultPrefix) {
        URI uri;
        String c = connectionString.toLowerCase(Locale.ROOT);
        boolean hasAnHttpPrefix = c.startsWith(HTTP_PREFIX) || c.startsWith(HTTPS_PREFIX);
        try {
            uri = new URI(connectionString);
        } catch (URISyntaxException e) {
            if (hasAnHttpPrefix == false) {
                return parseMaybeWithScheme(defaultPrefix + connectionString, null);
            }
            URISyntaxException s = CredentialsRedaction.redactedURISyntaxException(e);
            throw new IllegalArgumentException("Invalid connection configuration: " + s.getMessage(), s);
        }

        if (hasAnHttpPrefix == false) {
            if (uri.getHost() != null) { 
                throw new IllegalArgumentException(
                    "Invalid connection scheme ["
                        + uri.getScheme()
                        + "] configuration: only "
                        + HTTP_SCHEME
                        + " and "
                        + HTTPS_SCHEME
                        + " protocols are supported"
                );
            }
            if (connectionString.length() > 0) { 
                return parseMaybeWithScheme(defaultPrefix + connectionString, null);
            }
        }

        return uri;
    }

    public static class CredentialsRedaction {
        public static final Character REDACTION_CHAR = '*';
        private static final String USER_ATTR_NAME = "user";
        private static final String PASS_ATTR_NAME = "password";

        private static String redactAttributeInString(String string, String attrName, Character replacement) {
            String needle = attrName + "=";
            int attrIdx = string.toLowerCase(Locale.ROOT).indexOf(needle); 
            if (attrIdx >= 0) { 
                int attrEndIdx = attrIdx + needle.length();
                return string.substring(0, attrEndIdx) + repeatString(String.valueOf(replacement), string.length() - attrEndIdx);
            }
            return string;
        }

        private static void redactValueForSimilarKey(
            String key,
            List<String> options,
            List<Map.Entry<String, String>> attrs,
            Character replacement
        ) {
            List<String> similar = StringUtils.findSimilar(key, options);
            for (String k : similar) {
                for (Map.Entry<String, String> e : attrs) {
                    if (e.getKey().equals(k)) {
                        e.setValue(repeatString(String.valueOf(replacement), e.getValue().length()));
                    }
                }
            }
        }

        public static String redactCredentialsInRawUriQuery(String rawQuery, Character replacement) {
            List<Map.Entry<String, String>> attrs = new ArrayList<>();
            List<String> options = new ArrayList<>();

            String key, value;
            for (String param : StringUtils.tokenize(rawQuery, "&")) {
                int eqIdx = param.indexOf('=');
                if (eqIdx <= 0) { 
                    value = eqIdx < 0 ? null : StringUtils.EMPTY;
                    key = redactAttributeInString(param, USER_ATTR_NAME, replacement);
                    key = redactAttributeInString(key, PASS_ATTR_NAME, replacement);
                } else {
                    key = param.substring(0, eqIdx);
                    value = param.substring(eqIdx + 1);
                    if (value.indexOf('=') >= 0) { 
                        value = redactAttributeInString(value, USER_ATTR_NAME, replacement);
                        value = redactAttributeInString(value, PASS_ATTR_NAME, replacement);
                    }
                    options.add(key);
                }
                attrs.add(new AbstractMap.SimpleEntry<>(key, value));
            }

            redactValueForSimilarKey(USER_ATTR_NAME, options, attrs, replacement);
            redactValueForSimilarKey(PASS_ATTR_NAME, options, attrs, replacement);

            StringBuilder sb = new StringBuilder(rawQuery.length());
            for (Map.Entry<String, String> a : attrs) {
                sb.append("&");
                sb.append(a.getKey());
                if (a.getValue() != null) {
                    sb.append("=");
                    sb.append(a.getValue());
                }
            }
            return sb.substring(1);
        }

        private static String editURI(URI uri, List<Map.Entry<Integer, Character>> faults, boolean hasPort) {
            StringBuilder sb = new StringBuilder();
            if (uri.getScheme() != null) {
                sb.append(uri.getScheme());
                sb.append(":
            }
            if (uri.getRawUserInfo() != null) {
                sb.append(repeatString("\0", uri.getRawUserInfo().length()));
                if (uri.getHost() != null) {
                    sb.append('@');
                }
            }
            if (uri.getHost() != null) {
                sb.append(uri.getHost());
            }
            if (hasPort || uri.getPort() > 0) {
                sb.append(':');
            }
            if (uri.getPort() > 0) {
                sb.append(uri.getPort());
            }
            if (uri.getRawPath() != null) {
                sb.append(uri.getRawPath());
            }
            if (uri.getQuery() != null) {
                sb.append('?');
                sb.append(redactCredentialsInRawUriQuery(uri.getRawQuery(), '\0'));
            }
            if (uri.getRawFragment() != null) {
                sb.append('#');
                sb.append(uri.getRawFragment());
            }

            Collections.reverse(faults);
            for (Map.Entry<Integer, Character> e : faults) {
                int idx = e.getKey();
                if (idx >= sb.length()) {
                    sb.append(e.getValue());
                } else {
                    sb.insert(
                        idx,
                        (sb.charAt(idx) == '\0' && (idx + 1 >= sb.length() || sb.charAt(idx + 1) == '\0')) ? '\0' : e.getValue()
                    );
                }
            }

            StringBuilder ret = new StringBuilder();
            sb.chars().forEach(x -> ret.append(x == '\0' ? REDACTION_CHAR : (char) x));

            return ret.toString();
        }

        private static String redactCredentialsInURLString(String urlString) {
            List<Map.Entry<Integer, Character>> faults = new ArrayList<>();

            boolean hasPort = false;
            for (StringBuilder sb = new StringBuilder(urlString); sb.length() > 0;) {
                try {
                    URI uri = new URI(sb.toString()).parseServerAuthority();
                    return editURI(uri, faults, hasPort);
                } catch (URISyntaxException use) {
                    int idx = use.getIndex();
                    if (idx < 0 || idx >= sb.length()) {
                        break; 
                    }
                    if (use.getReason().equals("Illegal character in port number")) {
                        hasPort = true;
                    }
                    faults.add(new AbstractMap.SimpleImmutableEntry<>(use.getIndex(), sb.charAt(idx)));
                    sb.deleteCharAt(idx);
                }
            }
            return null;
        }

        public static String redactCredentialsInConnectionString(String connectionString) {
            if (connectionString.startsWith(HTTP_PREFIX.toUpperCase(Locale.ROOT))
                || connectionString.startsWith(HTTPS_PREFIX.toUpperCase(Locale.ROOT))
                || connectionString.length() < "_:_@_".length()
                || (connectionString.indexOf('@') < 0 && connectionString.indexOf('?') < 0)) {
                return connectionString;
            }

            String cs = connectionString.toLowerCase(Locale.ROOT);
            boolean prefixed = cs.startsWith(HTTP_PREFIX) || cs.startsWith(HTTPS_PREFIX);
            String redacted = redactCredentialsInURLString((prefixed ? StringUtils.EMPTY : HTTP_PREFIX) + connectionString);
            if (redacted == null) {
                return "<REDACTED> ; a capitalized scheme (HTTP|HTTPS) disables the redaction";
            }
            return prefixed ? redacted : redacted.substring(HTTP_PREFIX.length());
        }

        public static URISyntaxException redactedURISyntaxException(URISyntaxException e) {
            return new URISyntaxException(redactCredentialsInConnectionString(e.getInput()), e.getReason(), e.getIndex());
        }

    }

    /**
     * Removes the query part of the URI
     */
    public static URI removeQuery(URI uri, String connectionString, URI defaultURI) {
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), null, defaultURI.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid connection configuration [" + connectionString + "]: " + e.getMessage(), e);
        }
    }

    public static URI appendSegmentToPath(URI uri, String segment) {
        if (uri == null) {
            throw new IllegalArgumentException("URI must not be null");
        }
        if (segment == null || segment.isEmpty() || "/".equals(segment)) {
            return uri;
        }

        String path = uri.getPath();
        String concatenatedPath = "";
        String cleanSegment = segment.startsWith("/") ? segment.substring(1) : segment;

        if (path == null || path.isEmpty()) {
            path = "/";
        }

        if (path.charAt(path.length() - 1) == '/') {
            concatenatedPath = path + cleanSegment;
        } else {
            concatenatedPath = path + "/" + cleanSegment;
        }
        try {
            return new URI(
                uri.getScheme(),
                uri.getUserInfo(),
                uri.getHost(),
                uri.getPort(),
                concatenatedPath,
                uri.getQuery(),
                uri.getFragment()
            );
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid segment [" + segment + "] for URI [" + uri + "]: " + e.getMessage(), e);
        }
    }
}
