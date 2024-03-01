/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.sql.cli;

import org.elasticsearch.cli.UserException;
import org.elasticsearch.xpack.sql.client.ConnectionConfiguration;
import org.elasticsearch.xpack.sql.client.SslConfig;

import java.net.URI;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ConnectionBuilderTests extends SqlCliTestCase {

    public void testDefaultConnection() throws Exception {
        CliTerminal testTerminal = mock(CliTerminal.class);
        ConnectionBuilder connectionBuilder = new ConnectionBuilder(testTerminal);
        boolean binaryCommunication = random().nextBoolean();
        ConnectionConfiguration con = connectionBuilder.buildConnection(null, null, binaryCommunication);
        assertNull(con.authUser());
        assertNull(con.authPass());
        assertEquals("http:
        assertEquals(URI.create("http:
        assertEquals(30000, con.connectTimeout());
        assertEquals(60000, con.networkTimeout());
        assertEquals(45000, con.pageTimeout());
        assertEquals(90000, con.queryTimeout());
        assertEquals(1000, con.pageSize());
        assertEquals(binaryCommunication, con.binaryCommunication());
        verifyNoMoreInteractions(testTerminal);
    }

    public void testBasicConnection() throws Exception {
        CliTerminal testTerminal = mock(CliTerminal.class);
        ConnectionBuilder connectionBuilder = new ConnectionBuilder(testTerminal);
        ConnectionConfiguration con = buildConnection(connectionBuilder, "http:
        assertNull(con.authUser());
        assertNull(con.authPass());
        assertEquals("http:
        assertEquals(URI.create("http:
        verifyNoMoreInteractions(testTerminal);
    }

    public void testUserAndPasswordConnection() throws Exception {
        CliTerminal testTerminal = mock(CliTerminal.class);
        ConnectionBuilder connectionBuilder = new ConnectionBuilder(testTerminal);
        ConnectionConfiguration con = buildConnection(connectionBuilder, "http:
        assertEquals("user", con.authUser());
        assertEquals("pass", con.authPass());
        assertEquals("http:
        assertEquals(URI.create("http:
        verifyNoMoreInteractions(testTerminal);
    }

    public void testAskUserForPassword() throws Exception {
        CliTerminal testTerminal = mock(CliTerminal.class);
        when(testTerminal.readPassword("password: ")).thenReturn("password");
        ConnectionBuilder connectionBuilder = new ConnectionBuilder(testTerminal);
        ConnectionConfiguration con = buildConnection(connectionBuilder, "http:
        assertEquals("user", con.authUser());
        assertEquals("password", con.authPass());
        assertEquals("http:
        assertEquals(URI.create("http:
        verify(testTerminal, times(1)).readPassword(any());
        verifyNoMoreInteractions(testTerminal);
    }

    public void testAskUserForPasswordAndKeystorePassword() throws Exception {
        CliTerminal testTerminal = mock(CliTerminal.class);
        when(testTerminal.readPassword("keystore password: ")).thenReturn("keystore password");
        when(testTerminal.readPassword("password: ")).thenReturn("password");
        AtomicBoolean called = new AtomicBoolean(false);
        ConnectionBuilder connectionBuilder = new ConnectionBuilder(testTerminal) {
            @Override
            protected void checkIfExists(String name, Path p) {
            }

            @Override
            protected ConnectionConfiguration newConnectionConfiguration(URI uri, String connectionString, Properties properties) {
                assertEquals("true", properties.get(SslConfig.SSL));
                assertEquals("keystore_location", properties.get(SslConfig.SSL_KEYSTORE_LOCATION));
                assertEquals("keystore password", properties.get(SslConfig.SSL_KEYSTORE_PASS));
                assertEquals("keystore_location", properties.get(SslConfig.SSL_TRUSTSTORE_LOCATION));
                assertEquals("keystore password", properties.get(SslConfig.SSL_TRUSTSTORE_PASS));

                called.set(true);
                return null;
            }
        };
        assertNull(buildConnection(connectionBuilder, "https:
        assertTrue(called.get());
        verify(testTerminal, times(2)).readPassword(any());
        verifyNoMoreInteractions(testTerminal);
    }

    public void testUserGaveUpOnPassword() throws Exception {
        CliTerminal testTerminal = mock(CliTerminal.class);
        UserException ue = new UserException(random().nextInt(), randomAlphaOfLength(5));
        when(testTerminal.readPassword("password: ")).thenThrow(ue);
        ConnectionBuilder connectionBuilder = new ConnectionBuilder(testTerminal);
        UserException actual = expectThrows(
            UserException.class,
            () -> buildConnection(connectionBuilder, "http:
        );
        assertSame(actual, ue);
    }

    public void testUserGaveUpOnKeystorePassword() throws Exception {
        CliTerminal testTerminal = mock(CliTerminal.class);
        UserException ue = new UserException(random().nextInt(), randomAlphaOfLength(5));
        when(testTerminal.readPassword("keystore password: ")).thenThrow(ue);
        when(testTerminal.readPassword("password: ")).thenReturn("password");
        ConnectionBuilder connectionBuilder = new ConnectionBuilder(testTerminal) {
            @Override
            protected void checkIfExists(String name, Path p) {
            }
        };
        UserException actual = expectThrows(
            UserException.class,
            () -> buildConnection(connectionBuilder, "https:
        );
        assertSame(actual, ue);
    }

    private ConnectionConfiguration buildConnection(ConnectionBuilder builder, String connectionStringArg, String keystoreLocation)
        throws UserException {
        return builder.buildConnection(connectionStringArg, keystoreLocation, randomBoolean());
    }
}
