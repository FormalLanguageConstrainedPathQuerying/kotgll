/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.sql.client;

import org.elasticsearch.test.ESTestCase;

import java.net.URI;
import java.util.Arrays;

import static org.elasticsearch.xpack.sql.client.UriUtils.CredentialsRedaction.REDACTION_CHAR;
import static org.elasticsearch.xpack.sql.client.UriUtils.CredentialsRedaction.redactCredentialsInConnectionString;
import static org.elasticsearch.xpack.sql.client.UriUtils.appendSegmentToPath;
import static org.elasticsearch.xpack.sql.client.UriUtils.parseURI;
import static org.elasticsearch.xpack.sql.client.UriUtils.removeQuery;

public class UriUtilsTests extends ESTestCase {

    public static URI DEFAULT_URI = URI.create("http:

    public void testHostAndPort() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testJustHost() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testHttpWithPort() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testHttpsWithPort() throws Exception {
        assertEquals(URI.create("https:
    }

    public void testHttpNoPort() throws Exception {
        assertEquals(URI.create("https:
    }

    public void testLocalhostV6() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testUserLocalhostV6() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testLocalhostV4() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testUserLocalhostV4() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testNoHost() {
        assertEquals(URI.create("http:
    }

    public void testEmpty() {
        assertEquals(URI.create("http:
    }

    public void testHttpsWithUser() throws Exception {
        assertEquals(URI.create("https:
    }

    public void testUserPassHost() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testHttpPath() throws Exception {
        assertEquals(URI.create("https:
    }

    public void testHttpQuery() throws Exception {
        assertEquals(URI.create("https:
    }

    public void testUnsupportedProtocol() throws Exception {
        assertEquals(
            "Invalid connection scheme [ftp] configuration: only http and https protocols are supported",
            expectThrows(IllegalArgumentException.class, () -> parseURI("ftp:
        );
    }

    public void testMalformedWhiteSpace() throws Exception {
        assertEquals(
            "Invalid connection configuration: Illegal character in authority at index 7: http:
            expectThrows(IllegalArgumentException.class, () -> parseURI(" ", DEFAULT_URI)).getMessage()
        );
    }

    public void testNoRedaction() {
        assertEquals(
            "Invalid connection configuration: Illegal character in fragment at index 16: HTTP:
            expectThrows(IllegalArgumentException.class, () -> parseURI("HTTP:
        );
    }

    public void testSimpleUriRedaction() {
        assertEquals(
            "http:
            redactCredentialsInConnectionString("http:
        );
    }

    public void testSimpleConnectionStringRedaction() {
        assertEquals(
            "*************@host:9200/path?user=****&password=****",
            redactCredentialsInConnectionString("user:password@host:9200/path?user=user&password=pass")
        );
    }

    public void testNoRedactionInvalidHost() {
        assertEquals("https:
    }

    public void testUriRedactionInvalidUserPart() {
        assertEquals(
            "http:
            redactCredentialsInConnectionString("http:
        );
    }

    public void testUriRedactionInvalidHost() {
        assertEquals(
            "http:
            redactCredentialsInConnectionString("http:
        );
    }

    public void testUriRedactionInvalidPort() {
        assertEquals(
            "http:
            redactCredentialsInConnectionString("http:
        );
    }

    public void testUriRedactionInvalidPath() {
        assertEquals(
            "http:
            redactCredentialsInConnectionString("http:
        );
    }

    public void testUriRedactionInvalidQuery() {
        assertEquals(
            "http:
            redactCredentialsInConnectionString("http:
        );
    }

    public void testUriRedactionInvalidFragment() {
        assertEquals(
            "https:
            redactCredentialsInConnectionString("https:
        );
    }

    public void testUriRedactionMisspelledUser() {
        assertEquals(
            "https:
            redactCredentialsInConnectionString("https:
        );
    }

    public void testUriRedactionMisspelledUserAndPassword() {
        assertEquals(
            "https:
            redactCredentialsInConnectionString("https:
        );
    }

    public void testUriRedactionNoScheme() {
        assertEquals("host:9200/path?usr=****&passwo=****", redactCredentialsInConnectionString("host:9200/path?usr=user&passwo=pass"));
    }

    public void testUriRedactionNoPort() {
        assertEquals("host/path?usr=****&passwo=****", redactCredentialsInConnectionString("host/path?usr=user&passwo=pass"));
    }

    public void testUriRedactionNoHost() {
        assertEquals("/path?usr=****&passwo=****", redactCredentialsInConnectionString("/path?usr=user&passwo=pass"));
    }

    public void testUriRedactionNoPath() {
        assertEquals("?usr=****&passwo=****", redactCredentialsInConnectionString("?usr=user&passwo=pass"));
    }

    public void testUriRandomRedact() {
        final String scheme = randomFrom("http:
        final String host = randomFrom("host", "host:" + randomIntBetween(1, 65535), StringUtils.EMPTY);
        final String path = randomFrom("", "/", "/path", StringUtils.EMPTY);
        final String userVal = randomAlphaOfLengthBetween(1, 2 + randomInt(50));
        final String user = "user=" + userVal;
        final String passVal = randomAlphaOfLengthBetween(1, 2 + randomInt(50));
        final String pass = "password=" + passVal;
        final String redactedUser = "user=" + String.valueOf(REDACTION_CHAR).repeat(userVal.length());
        final String redactedPass = "password=" + String.valueOf(REDACTION_CHAR).repeat(passVal.length());

        String connStr, expectRedact, expectParse, creds = StringUtils.EMPTY;
        if (randomBoolean() && host.length() > 0) {
            creds = userVal;
            if (randomBoolean()) {
                creds += ":" + passVal;
            }
            connStr = scheme + creds + "@" + host + path;
            expectRedact = scheme + String.valueOf(REDACTION_CHAR).repeat(creds.length()) + "@" + host + path;
        } else {
            connStr = scheme + host + path;
            expectRedact = scheme + host + path;
        }

        expectParse = scheme.length() > 0 ? scheme : "http:
        expectParse += creds + (creds.length() > 0 ? "@" : StringUtils.EMPTY);
        expectParse += host.length() > 0 ? host : "localhost";
        expectParse += (host.indexOf(':') > 0 ? StringUtils.EMPTY : ":" + 9200);
        expectParse += path.length() > 0 ? path : "/";

        Character sep = '?';
        if (randomBoolean()) {
            connStr += sep + user;
            expectRedact += sep + redactedUser;
            expectParse += sep + user;
            sep = '&';
        }
        if (randomBoolean()) {
            connStr += sep + pass;
            expectRedact += sep + redactedPass;
            expectParse += sep + pass;
        }

        assertEquals(expectRedact, redactCredentialsInConnectionString(connStr));
        if ((connStr.equals("http:
            assertEquals(URI.create(expectParse), parseURI(connStr, DEFAULT_URI));
        }
    }

    public void testUriRedactionMissingSeparatorBetweenUserAndPassword() {
        assertEquals(
            "https:
            redactCredentialsInConnectionString("https:
        );
    }

    public void testUriRedactionMissingSeparatorBeforePassword() {
        assertEquals(
            "https:
            redactCredentialsInConnectionString("https:
        );
    }

    public void testUriRedactionAllOptions() {
        StringBuilder cs = new StringBuilder("https:
        String[] options = {
            "timezone",
            "connect.timeout",
            "network.timeout",
            "page.timeout",
            "page.size",
            "query.timeout",
            "user",
            "password",
            "ssl",
            "ssl.keystore.location",
            "ssl.keystore.pass",
            "ssl.keystore.type",
            "ssl.truststore.location",
            "ssl.truststore.pass",
            "ssl.truststore.type",
            "ssl.protocol",
            "proxy.http",
            "proxy.socks",
            "field.multi.value.leniency",
            "index.include.frozen",
            "validate.properties" };
        Arrays.stream(options).forEach(e -> cs.append(e).append("=").append(e).append("&"));
        String connStr = cs.substring(0, cs.length() - 1);
        String expected = connStr.replace("user=user", "user=****");
        expected = expected.replace("password=password", "password=********");
        assertEquals(expected, redactCredentialsInConnectionString(connStr));
    }

    public void testUriRedactionBrokenHost() {
        assertEquals("ho^st", redactCredentialsInConnectionString("ho^st"));
    }

    public void testUriRedactionDisabled() {
        assertEquals(
            "HTTPS:
            redactCredentialsInConnectionString("HTTPS:
        );
    }

    public void testRemoveQuery() throws Exception {
        assertEquals(
            URI.create("http:
            removeQuery(URI.create("http:
        );
    }

    public void testRemoveQueryTrailingSlash() throws Exception {
        assertEquals(
            URI.create("http:
            removeQuery(URI.create("http:
        );
    }

    public void testRemoveQueryNoQuery() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testAppendEmptySegmentToPath() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testAppendNullSegmentToPath() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testAppendSegmentToNullPath() throws Exception {
        assertEquals(
            "URI must not be null",
            expectThrows(IllegalArgumentException.class, () -> appendSegmentToPath(null, "/_sql")).getMessage()
        );
    }

    public void testAppendSegmentToEmptyPath() throws Exception {
        assertEquals(URI.create("/_sql"), appendSegmentToPath(URI.create(""), "/_sql"));
    }

    public void testAppendSlashSegmentToPath() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testAppendSqlSegmentToPath() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testAppendSqlSegmentNoSlashToPath() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testAppendSegmentToPath() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testAppendSegmentNoSlashToPath() throws Exception {
        assertEquals(URI.create("http:
    }

    public void testAppendSegmentTwoSlashesToPath() throws Exception {
        assertEquals(
            URI.create("https:
            appendSegmentToPath(URI.create("https:
        );
    }
}
