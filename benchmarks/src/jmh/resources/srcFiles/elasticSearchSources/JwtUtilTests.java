/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.security.authc.jwt;

import org.elasticsearch.common.settings.RotatableSecret;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.xpack.core.security.authc.jwt.JwtRealmSettings;
import org.elasticsearch.xpack.core.security.authc.jwt.JwtUtil;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class JwtUtilTests extends JwtTestCase {

    public void testClientAuthenticationTypeValidation() {
        final String clientAuthenticationTypeKey = JwtRealmSettings.CLIENT_AUTHENTICATION_TYPE.getKey();
        final String clientAuthenticationSharedSecretKey = JwtRealmSettings.CLIENT_AUTHENTICATION_SHARED_SECRET.getKey();
        final SecureString sharedSecretNonEmpty = new SecureString(randomAlphaOfLengthBetween(1, 32).toCharArray());
        final SecureString sharedSecretNullOrEmpty = randomBoolean() ? new SecureString("".toCharArray()) : null;

        JwtUtil.validateClientAuthenticationSettings(
            clientAuthenticationTypeKey,
            JwtRealmSettings.ClientAuthenticationType.NONE,
            clientAuthenticationSharedSecretKey,
            new RotatableSecret(sharedSecretNullOrEmpty)
        );
        final Exception exception1 = expectThrows(
            SettingsException.class,
            () -> JwtUtil.validateClientAuthenticationSettings(
                clientAuthenticationTypeKey,
                JwtRealmSettings.ClientAuthenticationType.NONE,
                clientAuthenticationSharedSecretKey,
                new RotatableSecret(sharedSecretNonEmpty)
            )
        );
        assertThat(
            exception1.getMessage(),
            is(
                equalTo(
                    "Setting ["
                        + clientAuthenticationSharedSecretKey
                        + "] is not supported, because setting ["
                        + clientAuthenticationTypeKey
                        + "] is ["
                        + JwtRealmSettings.ClientAuthenticationType.NONE.value()
                        + "]"
                )
            )
        );

        JwtUtil.validateClientAuthenticationSettings(
            clientAuthenticationTypeKey,
            JwtRealmSettings.ClientAuthenticationType.SHARED_SECRET,
            clientAuthenticationSharedSecretKey,
            new RotatableSecret(sharedSecretNonEmpty)
        );
        final Exception exception2 = expectThrows(
            SettingsException.class,
            () -> JwtUtil.validateClientAuthenticationSettings(
                clientAuthenticationTypeKey,
                JwtRealmSettings.ClientAuthenticationType.SHARED_SECRET,
                clientAuthenticationSharedSecretKey,
                new RotatableSecret(sharedSecretNullOrEmpty)
            )
        );
        assertThat(
            exception2.getMessage(),
            is(
                equalTo(
                    "Missing setting for ["
                        + clientAuthenticationSharedSecretKey
                        + "]. It is required when setting ["
                        + clientAuthenticationTypeKey
                        + "] is ["
                        + JwtRealmSettings.ClientAuthenticationType.SHARED_SECRET.value()
                        + "]"
                )
            )
        );
    }

    public void testParseHttpsUriAllowsFilesPassThrough() {
        assertThat(JwtUtil.parseHttpsUri(null), nullValue());
        assertThat(JwtUtil.parseHttpsUri(""), nullValue());
        assertThat(JwtUtil.parseHttpsUri("C:"), nullValue());
        assertThat(JwtUtil.parseHttpsUri("C:/"), nullValue());
        assertThat(JwtUtil.parseHttpsUri("C:/jwkset.json"), nullValue());
        assertThat(JwtUtil.parseHttpsUri("/"), nullValue());
        assertThat(JwtUtil.parseHttpsUri("/tmp"), nullValue());
        assertThat(JwtUtil.parseHttpsUri("/tmp/"), nullValue());
        assertThat(JwtUtil.parseHttpsUri("/tmp/jwkset.json"), nullValue());
    }

    public void testParseHttpUriAllRejected() {
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http"));
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:"));
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:/"));
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("http:
    }

    public void testParseHttpsUriProblemsRejected() {
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("https"));
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("https:"));
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("https:/"));
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("https:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("https:
        expectThrows(SettingsException.class, () -> JwtUtil.parseHttpsUri("https:
    }

    public void testParseHttpsUriAccepted() {
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
        assertThat(JwtUtil.parseHttpsUri("https:
    }
}
