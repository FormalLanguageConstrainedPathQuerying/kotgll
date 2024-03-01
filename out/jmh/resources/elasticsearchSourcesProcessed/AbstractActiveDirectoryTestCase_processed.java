/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.security.authc.ldap;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakFilters;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.ssl.SslVerificationMode;
import org.elasticsearch.core.Booleans;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.TestEnvironment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.fixtures.smb.SmbTestContainer;
import org.elasticsearch.test.fixtures.testcontainers.TestContainersThreadFilter;
import org.elasticsearch.xpack.core.security.authc.RealmConfig;
import org.elasticsearch.xpack.core.security.authc.ldap.ActiveDirectorySessionFactorySettings;
import org.elasticsearch.xpack.core.security.authc.ldap.support.LdapSearchScope;
import org.elasticsearch.xpack.core.security.authc.ldap.support.SessionFactorySettings;
import org.elasticsearch.xpack.core.ssl.SSLConfigurationSettings;
import org.elasticsearch.xpack.core.ssl.SSLService;
import org.junit.Before;
import org.junit.ClassRule;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.xpack.core.security.authc.RealmSettings.getFullSettingKey;

@ThreadLeakFilters(filters = { TestContainersThreadFilter.class })
public abstract class AbstractActiveDirectoryTestCase extends ESTestCase {

    @ClassRule
    public static final SmbTestContainer smbFixture = new SmbTestContainer();
    public static final Boolean FOLLOW_REFERRALS = Booleans.parseBoolean(getFromEnv("TESTS_AD_FOLLOW_REFERRALS", "false"));
    public static final String PASSWORD = "Passw0rd";
    public static final String AD_DOMAIN = "ad.test.elasticsearch.com";

    protected SSLService sslService;
    protected Settings globalSettings;
    protected List<String> certificatePaths;

    @Before
    public void initializeSslSocketFactory() throws Exception {
        certificatePaths = new ArrayList<>();
        Files.walkFileTree(getDataPath("../ldap/support"), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (fileName.endsWith(".crt")) {
                    certificatePaths.add(getDataPath("../ldap/support/" + fileName).toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        /*
         * Prior to each test we reinitialize the socket factory with a new SSLService so that we get a new SSLContext.
         * If we re-use an SSLContext, previously connected sessions can get re-established which breaks hostname
         * verification tests since a re-established connection does not perform hostname verification.
         */
        Settings.Builder builder = Settings.builder().put("path.home", createTempDir());

        builder.putList("xpack.security.authc.realms.active_directory.foo.ssl.certificate_authorities", certificatePaths);
        builder.put("xpack.security.authc.realms.active_directory.foo.ssl.verification_mode", SslVerificationMode.FULL);
        builder.putList("xpack.security.authc.realms.active_directory.bar.ssl.certificate_authorities", certificatePaths);
        builder.put("xpack.security.authc.realms.active_directory.bar.ssl.verification_mode", SslVerificationMode.CERTIFICATE);
        globalSettings = builder.build();
        Environment environment = TestEnvironment.newEnvironment(globalSettings);
        sslService = new SSLService(environment);
    }

    Settings buildAdSettings(
        RealmConfig.RealmIdentifier realmId,
        String ldapUrl,
        String adDomainName,
        String userSearchDN,
        LdapSearchScope scope,
        boolean hostnameVerification
    ) {
        final String realmName = realmId.getName();
        Settings.Builder builder = Settings.builder()
            .putList(getFullSettingKey(realmId, SessionFactorySettings.URLS_SETTING), ldapUrl)
            .put(getFullSettingKey(realmId, ActiveDirectorySessionFactorySettings.AD_DOMAIN_NAME_SETTING), adDomainName)
            .put(getFullSettingKey(realmName, ActiveDirectorySessionFactorySettings.AD_USER_SEARCH_BASEDN_SETTING), userSearchDN)
            .put(getFullSettingKey(realmName, ActiveDirectorySessionFactorySettings.AD_USER_SEARCH_SCOPE_SETTING), scope)
            .put(getFullSettingKey(realmId, SessionFactorySettings.FOLLOW_REFERRALS_SETTING), FOLLOW_REFERRALS)
            .putList(getFullSettingKey(realmId, SSLConfigurationSettings.CAPATH_SETTING_REALM), certificatePaths);
        if (randomBoolean()) {
            builder.put(
                getFullSettingKey(realmId, SSLConfigurationSettings.VERIFICATION_MODE_SETTING_REALM),
                hostnameVerification ? SslVerificationMode.FULL : SslVerificationMode.CERTIFICATE
            );
        } else {
            builder.put(getFullSettingKey(realmId, SessionFactorySettings.HOSTNAME_VERIFICATION_SETTING), hostnameVerification);
        }
        return builder.build();
    }

    protected static void assertConnectionCanReconnect(LDAPInterface conn) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    if (conn instanceof LDAPConnection) {
                        ((LDAPConnection) conn).reconnect();
                    } else if (conn instanceof LDAPConnectionPool) {
                        try (LDAPConnection c = ((LDAPConnectionPool) conn).getConnection()) {
                            c.reconnect();
                        }
                    }
                } catch (LDAPException e) {
                    fail(
                        "Connection is not valid. It will not work on follow referral flow."
                            + System.lineSeparator()
                            + ExceptionsHelper.stackTrace(e)
                    );
                }
                return null;
            }
        });
    }

    private static String getFromEnv(String envVar, String defaultValue) {
        final String value = System.getenv(envVar);
        return value == null ? defaultValue : value;
    }
}
