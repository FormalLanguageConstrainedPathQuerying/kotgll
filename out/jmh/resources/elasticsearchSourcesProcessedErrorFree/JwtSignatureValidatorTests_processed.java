/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.authc.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.core.security.authc.RealmConfig;
import org.elasticsearch.xpack.core.security.authc.jwt.JwtRealmSettings;
import org.junit.Before;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class JwtSignatureValidatorTests extends ESTestCase {

    private AtomicInteger validateSignatureAttemptCounter;
    private AtomicInteger finalSuccessCounter;
    private AtomicInteger finalFailureCounter;
    private AtomicInteger reloadAttemptCounter;
    private JwkSetLoader jwkSetLoader;
    private JwtSignatureValidator.PkcJwtSignatureValidator signatureValidator;
    private ActionListener<Void> primaryListener;
    private final SignedJWT signedJWT = mock(SignedJWT.class);
    private static final Logger logger = LogManager.getLogger(JwtSignatureValidatorTests.class);

    @Before
    public void setup() throws Exception {
        final Path tempDir = createTempDir();
        final Path path = tempDir.resolve("jwkset.json");
        Files.write(path, List.of("{\"keys\":[]}"), StandardCharsets.UTF_8);
        final RealmConfig realmConfig = mock(RealmConfig.class);
        when(realmConfig.getSetting(JwtRealmSettings.PKC_JWKSET_PATH)).thenReturn("jwkset.json");
        final Environment env = mock(Environment.class);
        when(env.configFile()).thenReturn(tempDir);
        when(realmConfig.env()).thenReturn(env);

        validateSignatureAttemptCounter = new AtomicInteger();
        reloadAttemptCounter = new AtomicInteger();
        finalSuccessCounter = new AtomicInteger();
        finalFailureCounter = new AtomicInteger();

        jwkSetLoader = spy(new JwkSetLoader(realmConfig, List.of(), null));

        final JwtSignatureValidator.PkcJwkSetReloadNotifier reloadNotifier = () -> {};
        signatureValidator = spy(new JwtSignatureValidator.PkcJwtSignatureValidator(jwkSetLoader, reloadNotifier));

        primaryListener = new ActionListener<>() {
            @Override
            public void onResponse(Void o) {
                finalSuccessCounter.getAndIncrement();
            }

            @Override
            public void onFailure(Exception e) {
                finalFailureCounter.getAndIncrement();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public void testWorkflowWithSuccess() throws Exception {
        Mockito.doAnswer(invocation -> {
            reloadAttemptCounter.getAndIncrement();
            return null;
        }).when(jwkSetLoader).reload(any(ActionListener.class));

        Mockito.doAnswer(invocation -> {
            validateSignatureAttemptCounter.getAndIncrement();
            return null;
        }).when(signatureValidator).validateSignature(any(SignedJWT.class), anyList());
        signatureValidator.validate(randomIdentifier(), signedJWT, primaryListener);
        assertThat(validateSignatureAttemptCounter.get(), is(1));
        assertThat(finalSuccessCounter.get(), is(1));
        assertThat(finalFailureCounter.get(), is(0));
        assertThat(reloadAttemptCounter.get(), is(0));
    }

    @SuppressWarnings("unchecked")
    public void testWorkflowWithFailure() throws Exception {
        Mockito.doAnswer(invocation -> {
            reloadAttemptCounter.getAndIncrement();
            ActionListener<Void> listener = invocation.getArgument(0);
            listener.onResponse(null);
            return null;
        }).when(jwkSetLoader).reload(any(ActionListener.class));

        Mockito.doAnswer(
            invocation -> new JwkSetLoader.ContentAndJwksAlgs(
                "myshavalue".getBytes(StandardCharsets.UTF_8),
                new JwkSetLoader.JwksAlgs(Collections.emptyList(), Collections.emptyList())
            )
        ).when(jwkSetLoader).getContentAndJwksAlgs();

        Mockito.doAnswer(invocation -> {
            validateSignatureAttemptCounter.getAndIncrement();
            throw new RuntimeException("boom");
        }).when(signatureValidator).validateSignature(any(SignedJWT.class), anyList());
        signatureValidator.validate(randomIdentifier(), signedJWT, primaryListener);
        assertThat(validateSignatureAttemptCounter.get(), is(1));
        assertThat(finalSuccessCounter.get(), is(0));
        assertThat(finalFailureCounter.get(), is(1));
        assertThat(reloadAttemptCounter.get(), is(1));
    }

    @SuppressWarnings("unchecked")
    public void testWorkflowWithFailureThenSuccess() throws Exception {
        Mockito.doAnswer(invocation -> {
            reloadAttemptCounter.getAndIncrement();
            ActionListener<Void> listener = invocation.getArgument(0);
            listener.onResponse(null);
            return null;
        }).when(jwkSetLoader).reload(any(ActionListener.class));

        Mockito.doAnswer(invocation -> {
            String version = "before";
            if (reloadAttemptCounter.get() > 0) {
                version = "after";
            }
            return new JwkSetLoader.ContentAndJwksAlgs(
                version.getBytes(StandardCharsets.UTF_8),
                new JwkSetLoader.JwksAlgs(Collections.emptyList(), Collections.emptyList())
            );
        }).when(jwkSetLoader).getContentAndJwksAlgs();

        Mockito.doAnswer(invocation -> {
            if (validateSignatureAttemptCounter.getAndIncrement() == 0) {
                throw new RuntimeException("boom");
            } else {
                return null;
            }
        }).when(signatureValidator).validateSignature(any(SignedJWT.class), anyList());
        signatureValidator.validate(randomIdentifier(), signedJWT, primaryListener);
        assertThat(validateSignatureAttemptCounter.get(), is(2));
        assertThat(finalSuccessCounter.get(), is(1));
        assertThat(finalFailureCounter.get(), is(0));
        assertThat(reloadAttemptCounter.get(), is(1));
    }

    @SuppressWarnings("unchecked")
    public void testConcurrentWorkflowWithFailureThenSuccess() throws Exception {
        final CyclicBarrier reloadBarrier = new CyclicBarrier(2);
        Mockito.doAnswer(invocation -> {
            reloadAttemptCounter.getAndIncrement();
            safeAwait(reloadBarrier); 
            ActionListener<Void> listener = invocation.getArgument(0);
            listener.onResponse(null);
            return null;
        }).when(jwkSetLoader).reload(any(ActionListener.class));

        Mockito.doAnswer(invocation -> {
            String version = "before";
            if (reloadAttemptCounter.get() > 1) {
                version = "after";
            }
            return new JwkSetLoader.ContentAndJwksAlgs(
                version.getBytes(StandardCharsets.UTF_8),
                new JwkSetLoader.JwksAlgs(Collections.emptyList(), Collections.emptyList())
            );
        }).when(jwkSetLoader).getContentAndJwksAlgs();

        Mockito.doAnswer(invocation -> {
            if (validateSignatureAttemptCounter.getAndIncrement() <= 1) {
                throw new RuntimeException("boom");
            } else {
                return null;
            }
        }).when(signatureValidator).validateSignature(any(SignedJWT.class), anyList());

        final CyclicBarrier barrier = new CyclicBarrier(3);

        Thread t1 = new Thread(() -> {
            safeAwait(barrier);
            signatureValidator.validate(randomIdentifier(), signedJWT, primaryListener);
        });

        Thread t2 = new Thread(() -> {
            safeAwait(barrier);
            signatureValidator.validate(randomIdentifier(), signedJWT, primaryListener);
        });

        t1.start();
        t2.start();
        safeAwait(barrier); 
        t1.join();
        t2.join();

        try {
            assertThat(validateSignatureAttemptCounter.get(), is(4));
            assertThat(finalSuccessCounter.get(), is(2));
            assertThat(finalFailureCounter.get(), is(0));
            assertThat(reloadAttemptCounter.get(), is(2));
        } catch (AssertionError ae) {
            logger.info("validateSignatureAttemptCounter = [{}]", validateSignatureAttemptCounter.get());
            logger.info("finalSuccessCounter = [{}]", finalSuccessCounter.get());
            logger.info("finalFailureCounter = [{}]", finalFailureCounter.get());
            logger.info("reloadAttemptCounter = [{}]", reloadAttemptCounter.get());
            throw ae;
        }
    }

    public void testJwtSignVerifyPassedForAllSupportedAlgorithms() {
        for (final String signatureAlgorithm : JwtRealmSettings.SUPPORTED_SIGNATURE_ALGORITHMS) {
            try {
                helpTestSignatureAlgorithm(signatureAlgorithm, false);
            } catch (Exception e) {
                fail("signature validation with algorithm [" + signatureAlgorithm + "] should have succeeded");
            }
        }
        final Exception exp1 = expectThrows(JOSEException.class, () -> helpTestSignatureAlgorithm(JWSAlgorithm.ES256K.getName(), false));
        final String msg1 = "Unsupported signature algorithm ["
            + JWSAlgorithm.ES256K
            + "]. Supported signature algorithms are "
            + JwtRealmSettings.SUPPORTED_SIGNATURE_ALGORITHMS
            + ".";
        assertThat(exp1.getMessage(), equalTo(msg1));
    }

    private void helpTestSignatureAlgorithm(final String signatureAlgorithm, final boolean requireOidcSafe) throws Exception {
        logger.trace("Testing signature algorithm " + signatureAlgorithm);
        final JWK jwk = JwtTestCase.randomJwk(signatureAlgorithm, requireOidcSafe);
        final SecureString serializedJWTOriginal = JwtTestCase.randomBespokeJwt(jwk, signatureAlgorithm);
        final SignedJWT parsedSignedJWT = SignedJWT.parse(serializedJWTOriginal.toString());
        JwtSignatureValidator jwtSignatureValidator = (tokenPrincipal, jwt, listener) -> {};
        jwtSignatureValidator.validateSignature(parsedSignedJWT, List.of(jwk));
    }

}
