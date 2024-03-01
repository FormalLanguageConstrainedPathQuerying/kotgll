/*
 * Copyright (c) 2003, 2023, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.nio.ByteBuffer;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.MacSpi;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import jdk.internal.access.JavaNioAccess;
import jdk.internal.access.SharedSecrets;
import sun.nio.ch.DirectBuffer;

import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;
import static sun.security.pkcs11.wrapper.PKCS11Exception.RV.*;
import sun.security.util.PBEUtil;

/**
 * MAC implementation class. This class currently supports HMAC using
 * MD5, SHA-1, SHA-2 family (SHA-224, SHA-256, SHA-384, and SHA-512),
 * SHA-3 family (SHA3-224, SHA3-256, SHA3-384, and SHA3-512), and the
 * SSL3 MAC using MD5 and SHA-1.
 *
 * Note that unlike other classes (e.g. Signature), this does not
 * composite various operations if the token only supports part of the
 * required functionality. The MAC implementations in SunJCE already
 * do exactly that by implementing an MAC on top of MessageDigests. We
 * could not do any better than they.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class P11Mac extends MacSpi {

    private static final JavaNioAccess NIO_ACCESS = SharedSecrets.getJavaNioAccess();

    private final Token token;

    private final String algorithm;

    private final P11SecretKeyFactory.PBEKeyInfo svcPbeKi;

    private final CK_MECHANISM ckMechanism;

    private final int macLength;

    private P11Key p11Key;

    private Session session;

    private boolean initialized;

    private byte[] oneByte;

    P11Mac(Token token, String algorithm, long mechanism)
            throws PKCS11Exception {
        super();
        this.token = token;
        this.algorithm = algorithm;
        this.svcPbeKi = P11SecretKeyFactory.getPBEKeyInfo(algorithm);
        Long params = null;
        macLength = switch ((int) mechanism) {
            case (int) CKM_MD5_HMAC -> 16;
            case (int) CKM_SHA_1_HMAC -> 20;
            case (int) CKM_SHA224_HMAC, (int) CKM_SHA512_224_HMAC, (int) CKM_SHA3_224_HMAC -> 28;
            case (int) CKM_SHA256_HMAC, (int) CKM_SHA512_256_HMAC, (int) CKM_SHA3_256_HMAC -> 32;
            case (int) CKM_SHA384_HMAC, (int) CKM_SHA3_384_HMAC -> 48;
            case (int) CKM_SHA512_HMAC, (int) CKM_SHA3_512_HMAC -> 64;
            case (int) CKM_SSL3_MD5_MAC -> {
                params = Long.valueOf(16);
                yield 16;
            }
            case (int) CKM_SSL3_SHA1_MAC -> {
                params = Long.valueOf(20);
                yield 20;
            }
            default -> throw new ProviderException("Unknown mechanism: " + mechanism);
        };
        ckMechanism = new CK_MECHANISM(mechanism, params);
    }

    private void reset(boolean doCancel) {
        if (!initialized) {
            return;
        }
        initialized = false;

        try {
            if (session == null) {
                return;
            }

            if (doCancel && token.explicitCancel) {
                cancelOperation();
            }
        } finally {
            p11Key.releaseKeyID();
            session = token.releaseSession(session);
        }
    }

    private void cancelOperation() {
        token.ensureValid();

        if (P11Util.trySessionCancel(token, session, CKF_SIGN)) {
            return;
        }

        try {
            token.p11.C_SignFinal(session.id(), 0);
        } catch (PKCS11Exception e) {
            if (e.match(CKR_OPERATION_NOT_INITIALIZED)) {
                return;
            }
            throw new ProviderException("Cancel failed", e);
        }
    }

    private void ensureInitialized() throws PKCS11Exception {
        if (!initialized) {
            initialize();
        }
    }

    private void initialize() throws PKCS11Exception {
        if (p11Key == null) {
            throw new ProviderException(
                    "Operation cannot be performed without calling engineInit first");
        }
        token.ensureValid();
        long p11KeyID = p11Key.getKeyID();
        try {
            if (session == null) {
                session = token.getOpSession();
            }
            token.p11.C_SignInit(session.id(), ckMechanism, p11KeyID);
        } catch (PKCS11Exception e) {
            p11Key.releaseKeyID();
            session = token.releaseSession(session);
            throw e;
        }
        initialized = true;
    }

    protected int engineGetMacLength() {
        return macLength;
    }

    protected void engineReset() {
        reset(true);
    }

    protected void engineInit(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        reset(true);
        p11Key = null;
        if (svcPbeKi != null) {
            if (key instanceof P11Key) {
                PBEUtil.checkKeyAndParams(key, params, algorithm);
            } else {
                PBEKeySpec pbeKeySpec = PBEUtil.getPBAKeySpec(key, params);
                try {
                    P11Key.P11PBEKey p11PBEKey =
                            P11SecretKeyFactory.derivePBEKey(token,
                            pbeKeySpec, svcPbeKi);
                    p11PBEKey.clearPassword();
                    p11Key = p11PBEKey;
                } catch (InvalidKeySpecException e) {
                    throw new InvalidKeyException(e);
                } finally {
                    pbeKeySpec.clearPassword();
                }
            }
            if (params instanceof PBEParameterSpec pbeParams) {
                params = pbeParams.getParameterSpec();
            }
        }
        if (params != null) {
            throw new InvalidAlgorithmParameterException(
                    "Parameters not supported");
        }
        if (p11Key == null) {
            p11Key = P11SecretKeyFactory.convertKey(token, key, algorithm);
        }
        try {
            initialize();
        } catch (PKCS11Exception e) {
            throw new InvalidKeyException("init() failed", e);
        }
    }

    protected byte[] engineDoFinal() {
        try {
            ensureInitialized();
            return token.p11.C_SignFinal(session.id(), 0);
        } catch (PKCS11Exception e) {
            throw new ProviderException("doFinal() failed", e);
        } finally {
            reset(false);
        }
    }

    protected void engineUpdate(byte input) {
        if (oneByte == null) {
           oneByte = new byte[1];
        }
        oneByte[0] = input;
        engineUpdate(oneByte, 0, 1);
    }

    protected void engineUpdate(byte[] b, int ofs, int len) {
        try {
            ensureInitialized();
            token.p11.C_SignUpdate(session.id(), 0, b, ofs, len);
        } catch (PKCS11Exception e) {
            throw new ProviderException("update() failed", e);
        }
    }

    protected void engineUpdate(ByteBuffer byteBuffer) {
        try {
            ensureInitialized();
            int len = byteBuffer.remaining();
            if (len <= 0) {
                return;
            }
            if (!(byteBuffer instanceof DirectBuffer dByteBuffer)) {
                super.engineUpdate(byteBuffer);
                return;
            }
            int ofs = byteBuffer.position();
            NIO_ACCESS.acquireSession(byteBuffer);
            try  {
                token.p11.C_SignUpdate(session.id(), dByteBuffer.address() + ofs, null, 0, len);
            } finally {
                NIO_ACCESS.releaseSession(byteBuffer);
            }
            byteBuffer.position(ofs + len);
        } catch (PKCS11Exception e) {
            throw new ProviderException("update() failed", e);
        }
    }
}
