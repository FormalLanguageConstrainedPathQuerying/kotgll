/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.security.sasl.gsskerb;

import java.util.Map;
import java.util.logging.Level;
import javax.security.sasl.*;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.security.auth.callback.CallbackHandler;

import sun.security.jgss.krb5.internal.TlsChannelBindingImpl;
import org.ietf.jgss.*;

/**
 * Implements the GSSAPI SASL client mechanism for Kerberos V5.
 * (<A HREF="http:
 * <a HREF="http:
 * It uses the Java Bindings for GSSAPI
 * (<A HREF="http:
 * for getting GSSAPI/Kerberos V5 support.
 *
 * The client/server interactions are:
 * C0: bind (GSSAPI, initial response)
 * S0: sasl-bind-in-progress, challenge 1 (output of accept_sec_context or [])
 * C1: bind (GSSAPI, response 1 (output of init_sec_context or []))
 * S1: sasl-bind-in-progress challenge 2 (security layer, server max recv size)
 * C2: bind (GSSAPI, response 2 (security layer, client max recv size, authzid))
 * S2: bind success response
 *
 * Expects the client's credentials to be supplied from the
 * javax.security.sasl.credentials property or from the thread's Subject.
 * Otherwise the underlying KRB5 mech will attempt to acquire Kerberos creds
 * by logging into Kerberos (via default TextCallbackHandler).
 * These creds will be used for exchange with server.
 *
 * Required callbacks: none.
 *
 * Environment properties that affect behavior of implementation:
 *
 * javax.security.sasl.qop
 * - quality of protection; list of auth, auth-int, auth-conf; default is "auth"
 * javax.security.sasl.maxbuf
 * - max receive buffer size; default is 65536
 * javax.security.sasl.sendmaxbuffer
 * - max send buffer size; default is 65536; (min with server max recv size)
 *
 * javax.security.sasl.server.authentication
 * - "true" means require mutual authentication; default is "false"
 *
 * javax.security.sasl.credentials
 * - an {@link org.ietf.jgss.GSSCredential} used for delegated authentication.
 *
 * @author Rosanna Lee
 */

final class GssKrb5Client extends GssKrb5Base implements SaslClient {
    private static final String MY_CLASS_NAME = GssKrb5Client.class.getName();

    private boolean finalHandshake = false;
    private byte[] authzID;

    /**
     * Creates a SASL mechanism with client credentials that it needs
     * to participate in GSS-API/Kerberos v5 authentication exchange
     * with the server.
     */
    GssKrb5Client(String authzID, String protocol, String serverName,
        Map<String, ?> props, CallbackHandler cbh) throws SaslException {

        super(props, MY_CLASS_NAME);

        String service = protocol + "@" + serverName;
        logger.log(Level.FINE, "KRB5CLNT01:Requesting service name: {0}",
            service);

        try {
            GSSManager mgr = GSSManager.getInstance();

            GSSName acceptorName = mgr.createName(service,
                GSSName.NT_HOSTBASED_SERVICE, KRB5_OID);

            GSSCredential credentials = null;
            if (props != null) {
                Object prop = props.get(Sasl.CREDENTIALS);
                if (prop instanceof GSSCredential c) {
                    credentials = c;
                    logger.log(Level.FINE,
                        "KRB5CLNT01:Using the credentials supplied in " +
                        "javax.security.sasl.credentials");
                }
            }

            secCtx = mgr.createContext(acceptorName,
                KRB5_OID,   /* mechanism */
                credentials, /* credentials */
                GSSContext.INDEFINITE_LIFETIME);

            if (credentials != null) {
                secCtx.requestCredDeleg(true);
            }

            boolean mutual;
            if ((allQop & INTEGRITY_ONLY_PROTECTION) != 0
                    || (allQop & PRIVACY_PROTECTION) != 0) {
                mutual = true;
                secCtx.requestSequenceDet(true);
            } else {
                mutual = false;
            }

            if (props != null) {
                String prop = (String)props.get(Sasl.SERVER_AUTH);
                if (prop != null) {
                    mutual = "true".equalsIgnoreCase(prop);
                }
            }
            secCtx.requestMutualAuth(mutual);

            if (props != null) {
                byte[] tlsCB = (byte[])props.get("jdk.internal.sasl.tlschannelbinding");
                if (tlsCB != null) {
                    secCtx.setChannelBinding(new TlsChannelBindingImpl(tlsCB));
                }
            }

            secCtx.requestConf(true);
            secCtx.requestInteg(true);

        } catch (GSSException e) {
            throw new SaslException("Failure to initialize security context", e);
        }

        if (authzID != null && authzID.length() > 0) {
            this.authzID = authzID.getBytes(UTF_8);
        }
    }

    public boolean hasInitialResponse() {
        return true;
    }

    /**
     * Processes the challenge data.
     *
     * The server sends a challenge data using which the client must
     * process using GSS_Init_sec_context.
     * As per RFC 2222, when GSS_S_COMPLETE is returned, we do
     * an extra handshake to determine the negotiated security protection
     * and buffer sizes.
     *
     * @param challengeData A non-null byte array containing the
     * challenge data from the server.
     * @return A non-null byte array containing the response to be
     * sent to the server.
     */
    public byte[] evaluateChallenge(byte[] challengeData) throws SaslException {
        if (completed) {
            throw new IllegalStateException(
                "GSSAPI authentication already complete");
        }

        if (finalHandshake) {
            return doFinalHandshake(challengeData);
        } else {


            try {
                byte[] gssOutToken = secCtx.initSecContext(challengeData,
                    0, challengeData.length);
                if (logger.isLoggable(Level.FINER)) {
                    traceOutput(MY_CLASS_NAME, "evaluteChallenge",
                        "KRB5CLNT02:Challenge: [raw]", challengeData);
                    traceOutput(MY_CLASS_NAME, "evaluateChallenge",
                        "KRB5CLNT03:Response: [after initSecCtx]", gssOutToken);
                }

                if (secCtx.isEstablished()) {
                    finalHandshake = true;
                    if (gssOutToken == null) {
                        return EMPTY;
                    }
                }

                return gssOutToken;
            } catch (GSSException e) {
                throw new SaslException("GSS initiate failed", e);
            }
        }
    }

    private byte[] doFinalHandshake(byte[] challengeData) throws SaslException {
        try {

            if (logger.isLoggable(Level.FINER)) {
                traceOutput(MY_CLASS_NAME, "doFinalHandshake",
                    "KRB5CLNT04:Challenge [raw]:", challengeData);
            }

            if (challengeData.length == 0) {
                return EMPTY;
            }


            MessageProp msgProp = new MessageProp(false);
            byte[] gssOutToken = secCtx.unwrap(challengeData, 0,
                challengeData.length, msgProp);
            checkMessageProp("Handshake failure: ", msgProp);

            if (logger.isLoggable(Level.FINE)) {
                if (logger.isLoggable(Level.FINER)) {
                    traceOutput(MY_CLASS_NAME, "doFinalHandshake",
                        "KRB5CLNT05:Challenge [unwrapped]:", gssOutToken);
                }
                logger.log(Level.FINE, "KRB5CLNT06:Server protections: {0}",
                    gssOutToken[0]);
            }

            byte selectedQop = findPreferredMask(gssOutToken[0], qop);
            if (selectedQop == 0) {
                throw new SaslException(
                    "No common protection layer between client and server");
            }

            if ((selectedQop&PRIVACY_PROTECTION) != 0) {
                privacy = true;
                integrity = true;
            } else if ((selectedQop&INTEGRITY_ONLY_PROTECTION) != 0) {
                integrity = true;
            }

            int srvMaxBufSize = networkByteOrderToInt(gssOutToken, 1, 3);

            sendMaxBufSize = (sendMaxBufSize == 0) ? srvMaxBufSize :
                Math.min(sendMaxBufSize, srvMaxBufSize);

            rawSendSize = secCtx.getWrapSizeLimit(JGSS_QOP, privacy,
                sendMaxBufSize);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
"KRB5CLNT07:Client max recv size: {0}; server max recv size: {1}; rawSendSize: {2}",
                    new Object[] {recvMaxBufSize,
                                  srvMaxBufSize,
                                  rawSendSize});
            }

            int len = 4;
            if (authzID != null) {
                len += authzID.length;
            }

            byte[] gssInToken = new byte[len];
            gssInToken[0] = selectedQop;

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
            "KRB5CLNT08:Selected protection: {0}; privacy: {1}; integrity: {2}",
                    new Object[]{selectedQop,
                                 Boolean.valueOf(privacy),
                                 Boolean.valueOf(integrity)});
            }

            if (privacy || integrity) {
                intToNetworkByteOrder(recvMaxBufSize, gssInToken, 1, 3);
            }
            if (authzID != null) {
                System.arraycopy(authzID, 0, gssInToken, 4, authzID.length);
                logger.log(Level.FINE, "KRB5CLNT09:Authzid: {0}", authzID);
            }

            if (logger.isLoggable(Level.FINER)) {
                traceOutput(MY_CLASS_NAME, "doFinalHandshake",
                    "KRB5CLNT10:Response [raw]", gssInToken);
            }

            gssOutToken = secCtx.wrap(gssInToken,
                0, gssInToken.length,
                new MessageProp(0 /* qop */, false /* privacy */));

            if (logger.isLoggable(Level.FINER)) {
                traceOutput(MY_CLASS_NAME, "doFinalHandshake",
                    "KRB5CLNT11:Response [after wrap]", gssOutToken);
            }

            completed = true;  

            return gssOutToken;
        } catch (GSSException e) {
            throw new SaslException("Final handshake failed", e);
        }
    }
}
