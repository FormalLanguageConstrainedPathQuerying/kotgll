/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.security.authc.ldap.support;

import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class LDAPServersTests extends ESTestCase {

    public void testConfigure1ldaps() {
        String[] urls = new String[] { "ldaps:

        SessionFactory.LDAPServers servers = new SessionFactory.LDAPServers(urls);
        assertThat(servers.addresses().length, is(equalTo(1)));
        assertThat(servers.addresses()[0], is(equalTo("example.com")));
        assertThat(servers.ports().length, is(equalTo(1)));
        assertThat(servers.ports()[0], is(equalTo(636)));
        assertThat(servers.ssl(), is(equalTo(true)));
    }

    public void testConfigure2ldaps() {
        String[] urls = new String[] { "ldaps:

        SessionFactory.LDAPServers servers = new SessionFactory.LDAPServers(urls);
        assertThat(servers.addresses().length, is(equalTo(2)));
        assertThat(servers.addresses()[0], is(equalTo("primary.example.com")));
        assertThat(servers.addresses()[1], is(equalTo("secondary.example.com")));
        assertThat(servers.ports().length, is(equalTo(2)));
        assertThat(servers.ports()[0], is(equalTo(636)));
        assertThat(servers.ports()[1], is(equalTo(10636)));
        assertThat(servers.ssl(), is(equalTo(true)));
    }

    public void testConfigure2ldap() {
        String[] urls = new String[] { "ldap:

        SessionFactory.LDAPServers servers = new SessionFactory.LDAPServers(urls);
        assertThat(servers.addresses().length, is(equalTo(2)));
        assertThat(servers.addresses()[0], is(equalTo("primary.example.com")));
        assertThat(servers.addresses()[1], is(equalTo("secondary.example.com")));
        assertThat(servers.ports().length, is(equalTo(2)));
        assertThat(servers.ports()[0], is(equalTo(392)));
        assertThat(servers.ports()[1], is(equalTo(10392)));
        assertThat(servers.ssl(), is(equalTo(false)));
    }

    public void testConfigure1ldaps1ldap() {
        String[] urls = new String[] { "LDAPS:

        try {
            new SessionFactory.LDAPServers(urls);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("configured LDAP protocols are not all equal"));
        }
    }

    public void testConfigure1ldap1ldaps() {
        String[] urls = new String[] { "ldap:

        try {
            new SessionFactory.LDAPServers(urls);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("configured LDAP protocols are not all equal"));
        }
    }
}
