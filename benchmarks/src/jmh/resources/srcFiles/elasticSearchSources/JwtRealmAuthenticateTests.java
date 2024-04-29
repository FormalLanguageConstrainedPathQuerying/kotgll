/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.security.authc.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;

import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.settings.MockSecureSettings;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.xpack.core.security.authc.AuthenticationResult;
import org.elasticsearch.xpack.core.security.authc.AuthenticationToken;
import org.elasticsearch.xpack.core.security.authc.Realm;
import org.elasticsearch.xpack.core.security.authc.RealmSettings;
import org.elasticsearch.xpack.core.security.authc.jwt.JwtAuthenticationToken;
import org.elasticsearch.xpack.core.security.authc.jwt.JwtRealmSettings;
import org.elasticsearch.xpack.core.security.user.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class JwtRealmAuthenticateTests extends JwtRealmTestCase {

    /**
     * Test with empty roles.
     * @throws Exception Unexpected test failure
     */
    public void testJwtAuthcRealmAuthcAuthzWithEmptyRoles() throws Exception {
        jwtIssuerAndRealms = generateJwtIssuerRealmPairs(
            randomIntBetween(1, 1), 
            randomIntBetween(0, 1), 
            randomIntBetween(1, JwtRealmSettings.SUPPORTED_SIGNATURE_ALGORITHMS.size()), 
            randomIntBetween(1, 3), 
            randomIntBetween(1, 3), 
            randomIntBetween(0, 0), 
            randomIntBetween(0, 1), 
            randomBoolean() 
        );
        final JwtIssuerAndRealm jwtIssuerAndRealm = randomJwtIssuerRealmPair();
        final User user = randomUser(jwtIssuerAndRealm.issuer());
        final SecureString jwt = randomJwt(jwtIssuerAndRealm, user);
        final SecureString clientSecret = JwtRealmInspector.getClientAuthenticationSharedSecret(jwtIssuerAndRealm.realm());
        final int jwtAuthcCount = randomIntBetween(2, 3);
        doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwt, clientSecret, jwtAuthcCount);
    }

    public void testJwtCache() throws Exception {
        jwtIssuerAndRealms = generateJwtIssuerRealmPairs(1, 1, 1, 1, 1, 1, 99, false);
        JwtRealm realm = jwtIssuerAndRealms.get(0).realm();
        realm.expireAll();
        assertThat(realm.getJwtCache().count(), is(0));
        final JwtIssuerAndRealm jwtIssuerAndRealm = randomJwtIssuerRealmPair();
        final SecureString clientSecret = JwtRealmInspector.getClientAuthenticationSharedSecret(jwtIssuerAndRealm.realm());
        for (int i = 1; i <= randomIntBetween(2, 10); i++) {
            User user = randomUser(jwtIssuerAndRealm.issuer());
            doMultipleAuthcAuthzAndVerifySuccess(
                jwtIssuerAndRealm.realm(),
                user,
                randomJwt(jwtIssuerAndRealm, user),
                clientSecret,
                randomIntBetween(2, 10)
            );
            assertThat(realm.getJwtCache().count(), is(i));
        }
    }

    /**
     * Test with no authz realms.
     * @throws Exception Unexpected test failure
     */
    public void testJwtAuthcRealmAuthcAuthzWithoutAuthzRealms() throws Exception {
        jwtIssuerAndRealms = generateJwtIssuerRealmPairs(
            randomIntBetween(1, 3), 
            randomIntBetween(0, 0), 
            randomIntBetween(1, JwtRealmSettings.SUPPORTED_SIGNATURE_ALGORITHMS.size()), 
            randomIntBetween(1, 3), 
            randomIntBetween(1, 3), 
            randomIntBetween(0, 3), 
            randomIntBetween(0, 1), 
            randomBoolean() 
        );
        final JwtIssuerAndRealm jwtIssuerAndRealm = randomJwtIssuerRealmPair();
        assertThat(jwtIssuerAndRealm.realm().delegatedAuthorizationSupport.hasDelegation(), is(false));

        final User user = randomUser(jwtIssuerAndRealm.issuer());
        final SecureString jwt = randomJwt(jwtIssuerAndRealm, user);
        final SecureString clientSecret = JwtRealmInspector.getClientAuthenticationSharedSecret(jwtIssuerAndRealm.realm());
        final int jwtAuthcCount = randomIntBetween(2, 3);
        doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwt, clientSecret, jwtAuthcCount);
    }

    /**
     * Test with updated/removed/restored JWKs.
     * @throws Exception Unexpected test failure
     */
    public void testJwkSetUpdates() throws Exception {
        jwtIssuerAndRealms = generateJwtIssuerRealmPairs(
            randomIntBetween(1, 3), 
            randomIntBetween(0, 0), 
            randomIntBetween(1, JwtRealmSettings.SUPPORTED_SIGNATURE_ALGORITHMS.size()), 
            randomIntBetween(1, 3), 
            randomIntBetween(1, 3), 
            randomIntBetween(0, 3), 
            randomIntBetween(0, 1), 
            randomBoolean() 
        );
        final JwtIssuerAndRealm jwtIssuerAndRealm = randomJwtIssuerRealmPair();
        assertThat(jwtIssuerAndRealm.realm().delegatedAuthorizationSupport.hasDelegation(), is(false));

        final User user = randomUser(jwtIssuerAndRealm.issuer());
        final SecureString jwtJwks1 = randomJwt(jwtIssuerAndRealm, user);
        final SecureString clientSecret = JwtRealmInspector.getClientAuthenticationSharedSecret(jwtIssuerAndRealm.realm());
        final int jwtAuthcCount = randomIntBetween(2, 3);
        doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwtJwks1, clientSecret, jwtAuthcCount);

        final String jwt1JwksAlg = SignedJWT.parse(jwtJwks1.toString()).getHeader().getAlgorithm().getName();
        final boolean isPkcJwtJwks1 = JwtRealmSettings.SUPPORTED_SIGNATURE_ALGORITHMS_PKC.contains(jwt1JwksAlg);
        logger.debug("JWT alg=[{}]", jwt1JwksAlg);

        final List<JwtIssuer.AlgJwkPair> jwtIssuerJwks1Backup = jwtIssuerAndRealm.issuer().algAndJwksAll;
        final boolean jwtIssuerJwks1OidcSafe = JwkValidateUtilTests.areJwkHmacOidcSafe(
            jwtIssuerJwks1Backup.stream().map(e -> e.jwk()).toList()
        );
        logger.debug("JWKs 1, algs=[{}]", String.join(",", jwtIssuerAndRealm.issuer().algorithmsAll));

        logger.debug("JWKs 1 backed up, algs=[{}]", String.join(",", jwtIssuerAndRealm.issuer().algorithmsAll));
        jwtIssuerAndRealm.issuer().setJwks(Collections.emptyList(), jwtIssuerJwks1OidcSafe);
        printJwtIssuer(jwtIssuerAndRealm.issuer());
        copyIssuerJwksToRealmConfig(jwtIssuerAndRealm);
        logger.debug("JWKs 1 emptied, algs=[{}]", String.join(",", jwtIssuerAndRealm.issuer().algorithmsAll));

        doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwtJwks1, clientSecret, jwtAuthcCount);
        logger.debug("JWT 1 still worked, because JWT realm has old JWKs cached in memory");

        jwtIssuerAndRealm.issuer().setJwks(jwtIssuerJwks1Backup, jwtIssuerJwks1OidcSafe);
        printJwtIssuer(jwtIssuerAndRealm.issuer());
        copyIssuerJwksToRealmConfig(jwtIssuerAndRealm);
        logger.debug("JWKs 1 restored, algs=[{}]", String.join(",", jwtIssuerAndRealm.issuer().algorithmsAll));

        doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwtJwks1, clientSecret, jwtAuthcCount);
        logger.debug("JWT 1 still worked, because JWT realm has old JWKs cached in memory");

        final List<JwtIssuer.AlgJwkPair> jwtIssuerJwks2Backup = JwtRealmTestCase.randomJwks(
            jwtIssuerJwks1Backup.stream().map(e -> e.alg()).toList(),
            jwtIssuerJwks1OidcSafe
        );
        jwtIssuerAndRealm.issuer().setJwks(jwtIssuerJwks2Backup, jwtIssuerJwks1OidcSafe);
        printJwtIssuer(jwtIssuerAndRealm.issuer());
        copyIssuerJwksToRealmConfig(jwtIssuerAndRealm);
        logger.debug("JWKs 2 created, algs=[{}]", String.join(",", jwtIssuerAndRealm.issuer().algorithmsAll));

        doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwtJwks1, clientSecret, jwtAuthcCount);
        logger.debug("JWT 1 still worked, because JWT realm has old JWKs cached in memory");

        final SecureString jwtJwks2 = randomJwt(jwtIssuerAndRealm, user);
        final String jwtJwks2Alg = SignedJWT.parse(jwtJwks2.toString()).getHeader().getAlgorithm().getName();
        final boolean isPkcJwtJwks2 = JwtRealmSettings.SUPPORTED_SIGNATURE_ALGORITHMS_PKC.contains(jwtJwks2Alg);
        logger.debug("Created JWT 2: oidcSafe=[{}], algs=[{}, {}]", jwtIssuerJwks1OidcSafe, jwt1JwksAlg, jwtJwks2Alg);

        if (isPkcJwtJwks2) {
            doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwtJwks2, clientSecret, jwtAuthcCount);
            logger.debug("PKC JWT 2 worked with JWKs 2");
        } else {
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtJwks2, clientSecret);
            logger.debug("HMAC JWT 2 failed with JWKs 1");
        }

        if (isPkcJwtJwks1 == false || isPkcJwtJwks2 == false) {
            doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwtJwks1, clientSecret, jwtAuthcCount);
        } else {
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtJwks1, clientSecret);
        }

        jwtIssuerAndRealm.issuer().setJwks(Collections.emptyList(), jwtIssuerJwks1OidcSafe);
        printJwtIssuer(jwtIssuerAndRealm.issuer());
        copyIssuerJwksToRealmConfig(jwtIssuerAndRealm);

        if (isPkcJwtJwks2) {
            doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwtJwks2, clientSecret, jwtAuthcCount);
        } else {
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtJwks2, clientSecret);
        }

        if (isPkcJwtJwks1 == false || isPkcJwtJwks2 == false) {
            doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwtJwks1, clientSecret, jwtAuthcCount);
        } else {
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtJwks1, clientSecret);
        }

        if (isPkcJwtJwks1 == false && isPkcJwtJwks2) {
            doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwtJwks2, clientSecret, jwtAuthcCount);
        } else {
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtJwks2, clientSecret);
        }

        jwtIssuerAndRealm.issuer().setJwks(jwtIssuerJwks2Backup, jwtIssuerJwks1OidcSafe);
        copyIssuerJwksToRealmConfig(jwtIssuerAndRealm);
        printJwtIssuer(jwtIssuerAndRealm.issuer());

        if (isPkcJwtJwks2) {
            doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwtJwks2, clientSecret, jwtAuthcCount);
        } else {
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtJwks2, clientSecret);
        }
        if (isPkcJwtJwks1 == false || isPkcJwtJwks2 == false) {
            doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwtJwks1, clientSecret, jwtAuthcCount);
        } else {
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtJwks1, clientSecret);
        }
    }

    /**
     * Test with authz realms.
     * @throws Exception Unexpected test failure
     */
    public void testJwtAuthcRealmAuthcAuthzWithAuthzRealms() throws Exception {
        jwtIssuerAndRealms = generateJwtIssuerRealmPairs(
            randomIntBetween(1, 3), 
            randomIntBetween(1, 3), 
            randomIntBetween(1, JwtRealmSettings.SUPPORTED_SIGNATURE_ALGORITHMS.size()), 
            randomIntBetween(1, 3), 
            randomIntBetween(1, 3), 
            randomIntBetween(0, 3), 
            randomIntBetween(0, 1), 
            randomBoolean() 
        );
        final JwtIssuerAndRealm jwtIssuerAndRealm = randomJwtIssuerRealmPair();
        assertThat(jwtIssuerAndRealm.realm().delegatedAuthorizationSupport.hasDelegation(), is(true));

        final User user = randomUser(jwtIssuerAndRealm.issuer());
        final SecureString jwt = randomJwt(jwtIssuerAndRealm, user);
        final SecureString clientSecret = JwtRealmInspector.getClientAuthenticationSharedSecret(jwtIssuerAndRealm.realm());
        final int jwtAuthcCount = randomIntBetween(2, 3);
        doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwt, clientSecret, jwtAuthcCount);

        {
            final String otherUsername = randomValueOtherThanMany(
                candidate -> jwtIssuerAndRealm.issuer().principals.containsKey(candidate),
                () -> randomAlphaOfLengthBetween(4, 12)
            );
            final User otherUser = new User(otherUsername);
            final SecureString otherJwt = randomJwt(jwtIssuerAndRealm, otherUser);

            final AuthenticationToken otherToken = jwtIssuerAndRealm.realm().token(createThreadContext(otherJwt, clientSecret));
            final PlainActionFuture<AuthenticationResult<User>> otherFuture = new PlainActionFuture<>();
            jwtIssuerAndRealm.realm().authenticate(otherToken, otherFuture);
            final AuthenticationResult<User> otherResult = otherFuture.actionGet();
            assertThat(otherResult.isAuthenticated(), is(false));
            assertThat(otherResult.getException(), nullValue());
            assertThat(
                otherResult.getMessage(),
                containsString("[" + otherUsername + "] was authenticated, but no user could be found in realms [")
            );
        }
    }

    /**
     * Verify that a JWT realm successfully connects to HTTPS server, and can handle an HTTP 404 Not Found response correctly.
     * @throws Exception Unexpected test failure
     */
    public void testPkcJwkSetUrlNotFound() throws Exception {
        final List<Realm> allRealms = new ArrayList<>(); 
        final boolean createHttpsServer = true; 
        final JwtIssuer jwtIssuer = createJwtIssuer(0, 12, 1, 1, 1, createHttpsServer);
        assertThat(jwtIssuer.httpsServer, notNullValue());
        try {
            final JwtRealmSettingsBuilder jwtRealmSettingsBuilder = createJwtRealmSettingsBuilder(jwtIssuer, 0, 0);
            final String configKey = RealmSettings.getFullSettingKey(jwtRealmSettingsBuilder.name(), JwtRealmSettings.PKC_JWKSET_PATH);
            final String configValue = jwtIssuer.httpsServer.url.replace("/valid/", "/invalid"); 
            jwtRealmSettingsBuilder.settingsBuilder().put(configKey, configValue);
            final Exception exception = expectThrows(
                SettingsException.class,
                () -> createJwtRealm(allRealms, jwtIssuer, jwtRealmSettingsBuilder)
            );
            assertThat(exception.getMessage(), equalTo("Can't get contents for setting [" + configKey + "] value [" + configValue + "]."));
            assertThat(exception.getCause().getMessage(), equalTo("Get [" + configValue + "] failed, status [404], reason [Not Found]."));
        } finally {
            jwtIssuer.close();
        }
    }

    /**
     * Test token parse failures and authentication failures.
     * @throws Exception Unexpected test failure
     */
    public void testJwtValidationFailures() throws Exception {
        jwtIssuerAndRealms = generateJwtIssuerRealmPairs(
            randomIntBetween(1, 1), 
            randomIntBetween(0, 0), 
            randomIntBetween(1, JwtRealmSettings.SUPPORTED_SIGNATURE_ALGORITHMS.size()), 
            randomIntBetween(1, 1), 
            randomIntBetween(1, 1), 
            randomIntBetween(1, 1), 
            randomIntBetween(0, 1), 
            randomBoolean() 
        );
        final JwtIssuerAndRealm jwtIssuerAndRealm = randomJwtIssuerRealmPair();
        final User user = randomUser(jwtIssuerAndRealm.issuer());
        final SecureString jwt = randomJwt(jwtIssuerAndRealm, user);
        final SecureString clientSecret = JwtRealmInspector.getClientAuthenticationSharedSecret(jwtIssuerAndRealm.realm());
        final int jwtAuthcCount = randomIntBetween(2, 3);

        doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwt, clientSecret, jwtAuthcCount);


        {   
            final ThreadContext requestThreadContext = createThreadContext(jwt, clientSecret);
            final JwtAuthenticationToken token = (JwtAuthenticationToken) jwtIssuerAndRealm.realm().token(requestThreadContext);
            final PlainActionFuture<AuthenticationResult<User>> plainActionFuture = new PlainActionFuture<>();
            jwtIssuerAndRealm.realm().authenticate(token, plainActionFuture);
            assertThat(plainActionFuture.get(), notNullValue());
            assertThat(plainActionFuture.get().isAuthenticated(), is(true));
        }


        final ThreadContext tc1 = createThreadContext(null, clientSecret);
        assertThat(jwtIssuerAndRealm.realm().token(tc1), nullValue());

        final ThreadContext tc2 = createThreadContext("", clientSecret);
        assertThat(jwtIssuerAndRealm.realm().token(tc2), nullValue());

        final ThreadContext tc3 = createThreadContext("  ", clientSecret);
        assertThat(jwtIssuerAndRealm.realm().token(tc3), nullValue());

        final ThreadContext tc4 = createThreadContext(jwt, "");
        final Exception e4 = expectThrows(IllegalArgumentException.class, () -> jwtIssuerAndRealm.realm().token(tc4));
        assertThat(e4.getMessage(), equalTo("Client shared secret must be non-empty"));

        final ThreadContext tc5 = createThreadContext(jwt, " ");
        final Exception e5 = expectThrows(IllegalArgumentException.class, () -> jwtIssuerAndRealm.realm().token(tc5));
        assertThat(e5.getMessage(), equalTo("Client shared secret must be non-empty"));

        final ThreadContext tc6 = createThreadContext("Head.Body.Sig", clientSecret);
        assertThat(jwtIssuerAndRealm.realm().token(tc6), nullValue());

        final SignedJWT parsedJwt = SignedJWT.parse(jwt.toString());
        final JWSHeader validHeader = parsedJwt.getHeader();
        final JWTClaimsSet validClaimsSet = parsedJwt.getJWTClaimsSet();
        final Base64URL validSignature = parsedJwt.getSignature();

        {   
            final SecureString unsignedJwt = new SecureString(new PlainJWT(validClaimsSet).serialize().toCharArray());
            final ThreadContext tc = createThreadContext(unsignedJwt, clientSecret);
            assertThat(jwtIssuerAndRealm.realm().token(tc), nullValue());
        }

        {   
            final String mixupAlg; 
            if (JwtRealmSettings.SUPPORTED_SIGNATURE_ALGORITHMS_HMAC.contains(validHeader.getAlgorithm().getName())) {
                if (JwtRealmInspector.getJwksAlgsPkc(jwtIssuerAndRealm.realm()).algs().isEmpty()) {
                    mixupAlg = null; 
                } else {
                    mixupAlg = randomFrom(JwtRealmInspector.getJwksAlgsPkc(jwtIssuerAndRealm.realm()).algs()); 
                }
            } else {
                if (JwtRealmInspector.getJwksAlgsHmac(jwtIssuerAndRealm.realm()).algs().isEmpty()) {
                    mixupAlg = null; 
                } else {
                    mixupAlg = randomFrom(JwtRealmInspector.getJwksAlgsHmac(jwtIssuerAndRealm.realm()).algs()); 
                }
            }
            if (Strings.hasText(mixupAlg)) {
                final JWSHeader tamperedHeader = new JWSHeader.Builder(JWSAlgorithm.parse(mixupAlg)).build();
                final SecureString jwtTamperedHeader = buildJwt(tamperedHeader, validClaimsSet, validSignature);
                verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtTamperedHeader, clientSecret);
            }
        }

        {   
            final JWTClaimsSet tamperedClaimsSet = new JWTClaimsSet.Builder(validClaimsSet).claim("gr0up", "superuser").build();
            final SecureString jwtTamperedClaimsSet = buildJwt(validHeader, tamperedClaimsSet, validSignature);
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtTamperedClaimsSet, clientSecret);
        }

        {   
            final SecureString jwtWithTruncatedSignature = new SecureString(jwt.toString().substring(0, jwt.length() - 1).toCharArray());
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtWithTruncatedSignature, clientSecret);
        }

        final JwtIssuer.AlgJwkPair algJwkPair = randomFrom(jwtIssuerAndRealm.issuer().algAndJwksAll);
        final JWSHeader jwtHeader = new JWSHeader.Builder(JWSAlgorithm.parse(algJwkPair.alg())).build();
        final Instant now = Instant.now();
        final Date past = Date.from(now.minusSeconds(86400));
        final Date future = Date.from(now.plusSeconds(86400));

        {   
            final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder(validClaimsSet).claim("auth_time", future).build();
            final SecureString jwtIatFuture = signJwt(algJwkPair.jwk(), new SignedJWT(jwtHeader, claimsSet));
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtIatFuture, clientSecret);
        }

        {   
            final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder(validClaimsSet).issueTime(future).build();
            final SecureString jwtIatFuture = signJwt(algJwkPair.jwk(), new SignedJWT(jwtHeader, claimsSet));
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtIatFuture, clientSecret);
        }

        {   
            final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder(validClaimsSet).notBeforeTime(future).build();
            final SecureString jwtIatFuture = signJwt(algJwkPair.jwk(), new SignedJWT(jwtHeader, claimsSet));
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtIatFuture, clientSecret);
        }

        {   
            final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder(validClaimsSet).expirationTime(past).build();
            final SecureString jwtExpPast = signJwt(algJwkPair.jwk(), new SignedJWT(jwtHeader, claimsSet));
            verifyAuthenticateFailureHelper(jwtIssuerAndRealm, jwtExpPast, clientSecret);
        }
    }

    /**
     * Configure two realms for same issuer. Use identical realm config, except different client secrets.
     * Generate a JWT which is valid for both realms, but verify authentication only succeeds for second realm with correct client secret.
     * @throws Exception Unexpected test failure
     */
    public void testSameIssuerTwoRealmsDifferentClientSecrets() throws Exception {
        final int realmsCount = 2;
        final List<Realm> allRealms = new ArrayList<>(realmsCount); 
        final JwtIssuer jwtIssuer = createJwtIssuer(0, 12, 1, 1, 1, false);
        printJwtIssuer(jwtIssuer);
        jwtIssuerAndRealms = new ArrayList<>(realmsCount);
        for (int i = 0; i < realmsCount; i++) {
            final String realmName = "realm_" + jwtIssuer.issuerClaimValue + "_" + i;
            final String clientSecret = "clientSecret_" + jwtIssuer.issuerClaimValue + "_" + i;

            final Settings.Builder authcSettings = Settings.builder()
                .put(globalSettings)
                .put(RealmSettings.getFullSettingKey(realmName, JwtRealmSettings.ALLOWED_ISSUER), jwtIssuer.issuerClaimValue)
                .put(
                    RealmSettings.getFullSettingKey(realmName, JwtRealmSettings.ALLOWED_SIGNATURE_ALGORITHMS),
                    String.join(",", jwtIssuer.algorithmsAll)
                )
                .put(RealmSettings.getFullSettingKey(realmName, JwtRealmSettings.ALLOWED_AUDIENCES), jwtIssuer.audiencesClaimValue.get(0))
                .put(RealmSettings.getFullSettingKey(realmName, JwtRealmSettings.CLAIMS_PRINCIPAL.getClaim()), jwtIssuer.principalClaimName)
                .put(
                    RealmSettings.getFullSettingKey(realmName, JwtRealmSettings.CLIENT_AUTHENTICATION_TYPE),
                    JwtRealmSettings.ClientAuthenticationType.SHARED_SECRET.value()
                );
            if (jwtIssuer.encodedJwkSetPkcPublic.isEmpty() == false) {
                authcSettings.put(
                    RealmSettings.getFullSettingKey(realmName, JwtRealmSettings.PKC_JWKSET_PATH),
                    saveToTempFile("jwkset.", ".json", jwtIssuer.encodedJwkSetPkcPublic)
                );
            }
            final MockSecureSettings secureSettings = new MockSecureSettings();
            if (jwtIssuer.algAndJwksHmac.isEmpty() == false) {
                secureSettings.setString(
                    RealmSettings.getFullSettingKey(realmName, JwtRealmSettings.HMAC_JWKSET),
                    jwtIssuer.encodedJwkSetHmac
                );
            }
            if (jwtIssuer.encodedKeyHmacOidc != null) {
                secureSettings.setString(
                    RealmSettings.getFullSettingKey(realmName, JwtRealmSettings.HMAC_KEY),
                    jwtIssuer.encodedKeyHmacOidc
                );
            }
            secureSettings.setString(
                RealmSettings.getFullSettingKey(realmName, JwtRealmSettings.CLIENT_AUTHENTICATION_SHARED_SECRET),
                clientSecret
            );
            authcSettings.setSecureSettings(secureSettings);
            final JwtRealmSettingsBuilder jwtRealmSettingsBuilder = new JwtRealmSettingsBuilder(realmName, authcSettings);
            final JwtRealm jwtRealm = createJwtRealm(allRealms, jwtIssuer, jwtRealmSettingsBuilder);
            jwtRealm.initialize(allRealms, licenseState);
            final JwtIssuerAndRealm jwtIssuerAndRealm = new JwtIssuerAndRealm(jwtIssuer, jwtRealm, jwtRealmSettingsBuilder);
            jwtIssuerAndRealms.add(jwtIssuerAndRealm); 
            printJwtRealm(jwtRealm);
        }

        final JwtIssuerAndRealm jwtIssuerAndRealm = jwtIssuerAndRealms.get(1);
        final User user = randomUser(jwtIssuerAndRealm.issuer());
        final SecureString jwt = randomJwt(jwtIssuerAndRealm, user);
        final SecureString clientSecret = JwtRealmInspector.getClientAuthenticationSharedSecret(jwtIssuerAndRealm.realm());
        final int jwtAuthcCount = randomIntBetween(2, 3);
        doMultipleAuthcAuthzAndVerifySuccess(jwtIssuerAndRealm.realm(), user, jwt, clientSecret, jwtAuthcCount);
    }

    public void testConcurrentPutAndInvalidateCacheWorks() throws Exception {
        jwtIssuerAndRealms = generateJwtIssuerRealmPairs(
            randomIntBetween(1, 1), 
            randomIntBetween(0, 0), 
            randomIntBetween(1, JwtRealmSettings.SUPPORTED_SIGNATURE_ALGORITHMS.size()), 
            randomIntBetween(1, 1), 
            randomIntBetween(1, 1), 
            randomIntBetween(1, 1), 
            randomIntBetween(1, 1), 
            false 
        );

        final JwtIssuerAndRealm jwtIssuerAndRealm = randomJwtIssuerRealmPair();
        final User user = randomUser(jwtIssuerAndRealm.issuer());
        final SecureString jwt = randomJwt(jwtIssuerAndRealm, user);
        final SignedJWT parsedJwt = SignedJWT.parse(jwt.toString());
        final JWTClaimsSet validClaimsSet = parsedJwt.getJWTClaimsSet();

        final int processors = Runtime.getRuntime().availableProcessors();
        final int numberOfThreads = Math.min(50, scaledRandomIntBetween((processors + 1) / 2, 4 * processors));  
        final Thread[] threads = new Thread[numberOfThreads];
        final CountDownLatch threadsCountDown = new CountDownLatch(numberOfThreads);
        final CountDownLatch racingCountDown = new CountDownLatch(1);
        final CountDownLatch completionCountDown = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            if (randomBoolean()) {
                threads[i] = new Thread(() -> {
                    threadsCountDown.countDown();
                    try {
                        if (racingCountDown.await(10, TimeUnit.SECONDS)) {
                            jwtIssuerAndRealm.realm().expireAll();
                            completionCountDown.countDown();
                        } else {
                            throw new AssertionError("racing is not ready within the given time period");
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                threads[i] = new Thread(() -> {
                    final BytesArray jwtCacheKey = new BytesArray(randomAlphaOfLength(10));
                    final PlainActionFuture<AuthenticationResult<User>> future = new PlainActionFuture<>();
                    threadsCountDown.countDown();
                    try {
                        if (racingCountDown.await(10, TimeUnit.SECONDS)) {
                            for (int j = 0; j < 10; j++) {
                                jwtIssuerAndRealm.realm().processValidatedJwt("token-principal", jwtCacheKey, validClaimsSet, future);
                                assertThat(future.actionGet().getValue().principal(), equalTo(user.principal()));
                            }
                            completionCountDown.countDown();
                        } else {
                            throw new AssertionError("Racing is not ready within the given time period");
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            threads[i].start();
        }

        if (threadsCountDown.await(10, TimeUnit.SECONDS)) {
            racingCountDown.countDown();
        } else {
            throw new AssertionError("Threads are not ready within the given time period");
        }

        if (false == completionCountDown.await(30, TimeUnit.SECONDS)) {
            throw new AssertionError("Test is not completed in time, check whether threads had deadlock");
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
