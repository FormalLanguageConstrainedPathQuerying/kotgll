/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import jdk.internal.access.JavaNioAccess;
import jdk.internal.access.SharedSecrets;
import sun.nio.ch.DirectBuffer;
import sun.security.jca.JCAUtil;
import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;
import static sun.security.pkcs11.wrapper.PKCS11Exception.RV.*;

/**
 * P11 AEAD Cipher implementation class. This class currently supports
 * AES cipher in GCM mode and CHACHA20-POLY1305 cipher.
 *
 * Note that AEAD modes do not use padding, so this class does not have
 * its own padding impl. In addition, some vendors such as NSS may not support
 * multi-part encryption/decryption for AEAD cipher algorithms, thus the
 * current impl uses PKCS#11 C_Encrypt/C_Decrypt calls and buffers data until
 * doFinal is called.
 *
 * @since   13
 */
final class P11AEADCipher extends CipherSpi {

    private static final JavaNioAccess NIO_ACCESS = SharedSecrets.getJavaNioAccess();

    private enum Transformation {
        AES_GCM("AES", "GCM", "NOPADDING", 16, 16),
        CHACHA20_POLY1305("CHACHA20", "NONE", "NOPADDING", 12, 16);

        final String keyAlgo;
        final String mode;
        final String padding;
        final int defIvLen; 
        final int defTagLen; 

        Transformation(String keyAlgo, String mode, String padding,
                int defIvLen, int defTagLen) {
            this.keyAlgo = keyAlgo;
            this.mode = mode;
            this.padding = padding;
            this.defIvLen = defIvLen;
            this.defTagLen = defTagLen;
        }
    }

    private final Token token;

    private final long mechanism;

    private final Transformation type;

    private final int fixedKeySize;

    private Session session = null;

    private P11Key p11Key = null;

    private boolean initialized = false;

    private boolean encrypt = true;

    private byte[] iv = null;
    private int tagLen = -1;
    private SecureRandom random = JCAUtil.getSecureRandom();

    private final ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();
    private final ByteArrayOutputStream aadBuffer = new ByteArrayOutputStream();
    private boolean updateCalled = false;

    private boolean requireReinit = false;
    private P11Key lastEncKey = null;
    private byte[] lastEncIv = null;

    P11AEADCipher(Token token, String algorithm, long mechanism)
            throws PKCS11Exception, NoSuchAlgorithmException {
        super();
        this.token = token;
        this.mechanism = mechanism;

        String[] algoParts = algorithm.split("/");
        if (algoParts[0].startsWith("AES")) {
            if (algoParts.length != 3) {
                throw new AssertionError("Invalid Transformation format: " +
                        algorithm);
            }
            int index = algoParts[0].indexOf('_');
            if (index != -1) {
                fixedKeySize = Integer.parseInt(algoParts[0].substring(index+1)) >> 3;
            } else {
                fixedKeySize = -1;
            }
            this.type = Transformation.AES_GCM;
            engineSetMode(algoParts[1]);
            try {
                engineSetPadding(algoParts[2]);
            } catch (NoSuchPaddingException e) {
                throw new NoSuchAlgorithmException(e);
            }
        } else if (algoParts[0].equals("ChaCha20-Poly1305")) {
            fixedKeySize = 32;
            this.type = Transformation.CHACHA20_POLY1305;
            if (algoParts.length > 3) {
                throw new AssertionError(
                        "Invalid Transformation format: " + algorithm);
            } else {
                if (algoParts.length > 1) {
                    engineSetMode(algoParts[1]);
                }
                try {
                    if (algoParts.length > 2) {
                        engineSetPadding(algoParts[2]);
                    }
                } catch (NoSuchPaddingException e) {
                    throw new NoSuchAlgorithmException();
                }
            }
        } else {
            throw new AssertionError("Unsupported transformation " + algorithm);
        }
    }

    @Override
    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        if (!mode.toUpperCase(Locale.ENGLISH).equals(type.mode)) {
            throw new NoSuchAlgorithmException("Unsupported mode " + mode);
        }
    }

    @Override
    protected void engineSetPadding(String padding)
            throws NoSuchPaddingException {
        if (!padding.toUpperCase(Locale.ENGLISH).equals(type.padding)) {
            throw new NoSuchPaddingException("Unsupported padding " + padding);
        }
    }

    @Override
    protected int engineGetBlockSize() {
        return switch (type) {
            case AES_GCM -> 16;
            case CHACHA20_POLY1305 -> 0;
            default -> throw new AssertionError("Unsupported type " + type);
        };
    }

    @Override
    protected int engineGetOutputSize(int inputLen) {
        return doFinalLength(inputLen);
    }

    @Override
    protected byte[] engineGetIV() {
        return (iv == null) ? null : iv.clone();
    }

    protected AlgorithmParameters engineGetParameters() {
        String apAlgo;
        AlgorithmParameterSpec spec = null;
        switch (type) {
            case AES_GCM -> {
                apAlgo = "GCM";
                if (encrypt && iv == null && tagLen == -1) {
                    iv = new byte[type.defIvLen];
                    tagLen = type.defTagLen;
                    random.nextBytes(iv);
                }
                if (iv != null) {
                    spec = new GCMParameterSpec(tagLen << 3, iv);
                }
            }
            case CHACHA20_POLY1305 -> {
                if (encrypt && iv == null) {
                    iv = new byte[type.defIvLen];
                    random.nextBytes(iv);
                }
                apAlgo = "ChaCha20-Poly1305";
                if (iv != null) {
                    spec = new IvParameterSpec(iv);
                }
            }
            default -> throw new AssertionError("Unsupported type " + type);
        }
        if (spec != null) {
            try {
                AlgorithmParameters params =
                    AlgorithmParameters.getInstance(apAlgo);
                params.init(spec);
                return params;
            } catch (GeneralSecurityException e) {
                throw new ProviderException("Could not encode parameters", e);
            }
        }
        return null;
    }

    protected void engineInit(int opmode, Key key, SecureRandom sr)
            throws InvalidKeyException {
        if (opmode == Cipher.DECRYPT_MODE) {
            throw new InvalidKeyException("Parameters required for decryption");
        }
        updateCalled = false;
        try {
            implInit(opmode, key, null, -1, sr);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException("init() failed", e);
        }
    }

    protected void engineInit(int opmode, Key key,
            AlgorithmParameterSpec params, SecureRandom sr)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (opmode == Cipher.DECRYPT_MODE && params == null) {
            throw new InvalidAlgorithmParameterException
                    ("Parameters required for decryption");
        }
        updateCalled = false;
        byte[] ivValue = null;
        int tagLen = -1;
        switch (type) {
            case AES_GCM:
                if (params != null) {
                    if (!(params instanceof GCMParameterSpec)) {
                        throw new InvalidAlgorithmParameterException
                                ("Only GCMParameterSpec is supported");
                    }
                    ivValue = ((GCMParameterSpec) params).getIV();
                    tagLen = ((GCMParameterSpec) params).getTLen() >> 3;
                }
            break;
            case CHACHA20_POLY1305:
                if (params != null) {
                    if (!(params instanceof IvParameterSpec)) {
                        throw new InvalidAlgorithmParameterException
                                ("Only IvParameterSpec is supported");
                    }
                    ivValue = ((IvParameterSpec) params).getIV();
                    tagLen = type.defTagLen;
                }
            break;
            default:
                throw new AssertionError("Unsupported type " + type);
        };
        implInit(opmode, key, ivValue, tagLen, sr);
    }

    protected void engineInit(int opmode, Key key, AlgorithmParameters params,
            SecureRandom sr)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (opmode == Cipher.DECRYPT_MODE && params == null) {
            throw new InvalidAlgorithmParameterException
                    ("Parameters required for decryption");
        }
        updateCalled = false;
        try {
            AlgorithmParameterSpec paramSpec = null;
            if (params != null) {
                paramSpec = switch (type) {
                    case AES_GCM -> params.getParameterSpec(GCMParameterSpec.class);
                    case CHACHA20_POLY1305 -> params.getParameterSpec(IvParameterSpec.class);
                };
            }
            engineInit(opmode, key, paramSpec, sr);
        } catch (InvalidParameterSpecException ex) {
            throw new InvalidAlgorithmParameterException(ex);
        }
    }

    private void implInit(int opmode, Key key, byte[] iv, int tagLen,
        SecureRandom sr)
        throws InvalidKeyException, InvalidAlgorithmParameterException {
        reset(true);
        if (fixedKeySize != -1 &&
                ((key instanceof P11Key) ? ((P11Key) key).length() >> 3 :
                            key.getEncoded().length) != fixedKeySize) {
            throw new InvalidKeyException("Key size is invalid");
        }
        P11Key newKey = P11SecretKeyFactory.convertKey(token, key,
                type.keyAlgo);
        switch (opmode) {
            case Cipher.ENCRYPT_MODE -> {
                encrypt = true;
                requireReinit = Arrays.equals(iv, lastEncIv) &&
                        (newKey == lastEncKey);
                if (requireReinit) {
                    throw new InvalidAlgorithmParameterException(
                            "Cannot reuse the same key and iv pair");
                }
            }
            case Cipher.DECRYPT_MODE -> {
                encrypt = false;
                requireReinit = false;
            }
            case Cipher.WRAP_MODE, Cipher.UNWRAP_MODE -> throw new UnsupportedOperationException
                    ("Unsupported mode: " + opmode);
            default ->
                    throw new AssertionError("Unknown mode: " + opmode);
        }

        if (sr != null) {
            this.random = sr;
        }

        if (iv == null && tagLen == -1) {
            iv = new byte[type.defIvLen];
            this.random.nextBytes(iv);
            tagLen = type.defTagLen;
        }
        this.iv = iv;
        this.tagLen = tagLen;
        this.p11Key = newKey;
        try {
            initialize();
        } catch (PKCS11Exception e) {
            if (e.match(CKR_MECHANISM_PARAM_INVALID)) {
                throw new InvalidAlgorithmParameterException("Bad params", e);
            }
            throw new InvalidKeyException("Could not initialize cipher", e);
        }
    }

    private void cancelOperation() {
        token.ensureValid();
        if (P11Util.trySessionCancel(token, session,
                (encrypt ? CKF_ENCRYPT : CKF_DECRYPT))) {
            return;
        }

        int bufLen = doFinalLength(0);
        byte[] buffer = new byte[bufLen];
        byte[] in = dataBuffer.toByteArray();
        int inLen = in.length;
        try {
            if (encrypt) {
                token.p11.C_Encrypt(session.id(), 0, in, 0, inLen,
                        0, buffer, 0, bufLen);
            } else {
                token.p11.C_Decrypt(session.id(), 0, in, 0, inLen,
                        0, buffer, 0, bufLen);
            }
        } catch (PKCS11Exception e) {
            if (e.match(CKR_OPERATION_NOT_INITIALIZED)) {
                return;
            }
            if (encrypt) {
                throw new ProviderException("Cancel failed", e);
            }
        }
    }

    private void ensureInitialized() throws PKCS11Exception {
        if (initialized && aadBuffer.size() > 0) {
            reset(true);
        }
        if (!initialized) {
            initialize();
        }
    }

    private void initialize() throws PKCS11Exception {
        if (p11Key == null) {
            throw new ProviderException(
                    "Operation cannot be performed without"
                    + " calling engineInit first");
        }
        if (requireReinit) {
            throw new IllegalStateException
                ("Must use either different key or iv");
        }

        token.ensureValid();

        byte[] aad = (aadBuffer.size() > 0 ? aadBuffer.toByteArray() : null);

        long p11KeyID = p11Key.getKeyID();
        try {
            CK_MECHANISM mechWithParams = switch (type) {
                case AES_GCM -> new CK_MECHANISM(mechanism,
                        new CK_GCM_PARAMS(tagLen << 3, iv, aad));
                case CHACHA20_POLY1305 -> new CK_MECHANISM(mechanism,
                        new CK_SALSA20_CHACHA20_POLY1305_PARAMS(iv, aad));
            };
            if (session == null) {
                session = token.getOpSession();
            }
            if (encrypt) {
                token.p11.C_EncryptInit(session.id(), mechWithParams,
                    p11KeyID);
            } else {
                token.p11.C_DecryptInit(session.id(), mechWithParams,
                    p11KeyID);
            }
        } catch (PKCS11Exception e) {
            p11Key.releaseKeyID();
            session = token.releaseSession(session);
            throw e;
        } finally {
            dataBuffer.reset();
            aadBuffer.reset();
        }
        initialized = true;
    }

    private int doFinalLength(int inLen) {
        if (inLen < 0) {
            throw new ProviderException("Invalid negative input length");
        }

        int result = inLen + dataBuffer.size();
        if (encrypt) {
            result += tagLen;
        } else {
            if (type == Transformation.CHACHA20_POLY1305) {
                result -= tagLen;
            }
        }
        return (Math.max(result, 0));
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
            dataBuffer.reset();
        }
    }

    protected byte[] engineUpdate(byte[] in, int inOfs, int inLen) {
        updateCalled = true;
        int n = implUpdate(in, inOfs, inLen);
        return new byte[0];
    }

    protected int engineUpdate(byte[] in, int inOfs, int inLen, byte[] out,
            int outOfs) throws ShortBufferException {
        updateCalled = true;
        implUpdate(in, inOfs, inLen);
        return 0;
    }

    @Override
    protected int engineUpdate(ByteBuffer inBuffer, ByteBuffer outBuffer)
            throws ShortBufferException {
        updateCalled = true;
        implUpdate(inBuffer);
        return 0;
    }

    @Override
    protected synchronized void engineUpdateAAD(byte[] src, int srcOfs, int srcLen)
            throws IllegalStateException {
        if ((src == null) || (srcOfs < 0) || (srcOfs + srcLen > src.length)) {
            throw new IllegalArgumentException("Invalid AAD");
        }
        if (requireReinit) {
            throw new IllegalStateException
                ("Must use either different key or iv for encryption");
        }
        if (p11Key == null) {
            throw new IllegalStateException("Need to initialize Cipher first");
        }
        if (updateCalled) {
            throw new IllegalStateException
                ("Update has been called; no more AAD data");
        }
        aadBuffer.write(src, srcOfs, srcLen);
    }

    @Override
    protected void engineUpdateAAD(ByteBuffer src)
            throws IllegalStateException {
        if (src == null) {
            throw new IllegalArgumentException("Invalid AAD");
        }
        byte[] srcBytes = new byte[src.remaining()];
        src.get(srcBytes);
        engineUpdateAAD(srcBytes, 0, srcBytes.length);
    }

    protected byte[] engineDoFinal(byte[] in, int inOfs, int inLen)
            throws IllegalBlockSizeException, BadPaddingException {
        int minOutLen = doFinalLength(inLen);
        try {
            byte[] out = new byte[minOutLen];
            int n = engineDoFinal(in, inOfs, inLen, out, 0);
            return P11Util.convert(out, 0, n);
        } catch (ShortBufferException e) {
            throw new ProviderException(e);
        } finally {
            updateCalled = false;
        }
    }
    protected int engineDoFinal(byte[] in, int inOfs, int inLen, byte[] out,
            int outOfs) throws ShortBufferException, IllegalBlockSizeException,
            BadPaddingException {
        try {
            return implDoFinal(in, inOfs, inLen, out, outOfs, out.length - outOfs);
        } finally {
            updateCalled = false;
        }
    }

    @Override
    protected int engineDoFinal(ByteBuffer inBuffer, ByteBuffer outBuffer)
            throws ShortBufferException, IllegalBlockSizeException,
            BadPaddingException {
        try {
            return implDoFinal(inBuffer, outBuffer);
        } finally {
            updateCalled = false;
        }
    }

    private int implUpdate(byte[] in, int inOfs, int inLen) {
        if (inLen > 0) {
            updateCalled = true;
            try {
                ensureInitialized();
            } catch (PKCS11Exception e) {
                reset(false);
                throw new ProviderException("update() failed", e);
            }
            dataBuffer.write(in, inOfs, inLen);
        }
        return 0;
    }

    private int implUpdate(ByteBuffer inBuf) {
        int inLen = inBuf.remaining();
        if (inLen > 0) {
            try {
                ensureInitialized();
            } catch (PKCS11Exception e) {
                reset(false);
                throw new ProviderException("update() failed", e);
            }
            byte[] data = new byte[inLen];
            inBuf.get(data);
            dataBuffer.write(data, 0, data.length);
        }
        return 0;
    }

    private int implDoFinal(byte[] in, int inOfs, int inLen,
            byte[] out, int outOfs, int outLen)
            throws ShortBufferException, IllegalBlockSizeException,
            BadPaddingException {
        int requiredOutLen = doFinalLength(inLen);
        if (outLen < requiredOutLen) {
            throw new ShortBufferException();
        }

        boolean doCancel = true;
        try {
            ensureInitialized();
            if (dataBuffer.size() > 0) {
                if (in != null && inOfs > 0 && inLen > 0 &&
                    inOfs < (in.length - inLen)) {
                    dataBuffer.write(in, inOfs, inLen);
                }
                in = dataBuffer.toByteArray();
                inOfs = 0;
                inLen = in.length;
            }
            int k = 0;
            if (encrypt) {
                k = token.p11.C_Encrypt(session.id(), 0, in, inOfs, inLen,
                        0, out, outOfs, outLen);
                doCancel = false;
            } else {
                if (inLen == 0) {
                    return 0;
                }
                k = token.p11.C_Decrypt(session.id(), 0, in, inOfs, inLen,
                        0, out, outOfs, outLen);
                doCancel = false;
            }
            return k;
        } catch (PKCS11Exception e) {
            doCancel = false;
            handleException(e);
            throw new ProviderException("doFinal() failed", e);
        } finally {
            if (encrypt) {
                lastEncKey = this.p11Key;
                lastEncIv = this.iv;
                requireReinit = true;
            }
            reset(doCancel);
        }
    }

    private int implDoFinal(ByteBuffer inBuffer, ByteBuffer outBuffer)
            throws ShortBufferException, IllegalBlockSizeException,
            BadPaddingException {
        int outLen = outBuffer.remaining();
        int inLen = inBuffer.remaining();

        int requiredOutLen = doFinalLength(inLen);
        if (outLen < requiredOutLen) {
            throw new ShortBufferException();
        }

        boolean doCancel = true;
        NIO_ACCESS.acquireSession(inBuffer);
        try {
            NIO_ACCESS.acquireSession(outBuffer);
            try {
                try {
                    ensureInitialized();

                    long inAddr = 0;
                    byte[] in = null;
                    int inOfs = 0;
                    if (dataBuffer.size() > 0) {
                        if (inLen > 0) {
                            byte[] temp = new byte[inLen];
                            inBuffer.get(temp);
                            dataBuffer.write(temp, 0, temp.length);
                        }
                        in = dataBuffer.toByteArray();
                        inOfs = 0;
                        inLen = in.length;
                    } else {
                        if (inBuffer instanceof DirectBuffer dInBuffer) {
                            inAddr = dInBuffer.address();
                            inOfs = inBuffer.position();
                        } else {
                            if (inBuffer.hasArray()) {
                                in = inBuffer.array();
                                inOfs = inBuffer.position() + inBuffer.arrayOffset();
                            } else {
                                in = new byte[inLen];
                                inBuffer.get(in);
                            }
                        }
                    }
                    long outAddr = 0;
                    byte[] outArray = null;
                    int outOfs = 0;
                    if (outBuffer instanceof DirectBuffer dOutBuffer) {
                        outAddr = dOutBuffer.address();
                        outOfs = outBuffer.position();
                    } else {
                        if (outBuffer.hasArray()) {
                            outArray = outBuffer.array();
                            outOfs = outBuffer.position() + outBuffer.arrayOffset();
                        } else {
                            outArray = new byte[outLen];
                        }
                    }

                    int k = 0;
                    if (encrypt) {
                        k = token.p11.C_Encrypt(session.id(), inAddr, in, inOfs, inLen,
                                outAddr, outArray, outOfs, outLen);
                        doCancel = false;
                    } else {
                        if (inLen == 0) {
                            return 0;
                        }
                        k = token.p11.C_Decrypt(session.id(), inAddr, in, inOfs, inLen,
                                outAddr, outArray, outOfs, outLen);
                        doCancel = false;
                    }
                    inBuffer.position(inBuffer.limit());
                    outBuffer.position(outBuffer.position() + k);
                    return k;
                } catch (PKCS11Exception e) {
                    doCancel = false;
                    handleException(e);
                    throw new ProviderException("doFinal() failed", e);
                } finally {
                    if (encrypt) {
                        lastEncKey = this.p11Key;
                        lastEncIv = this.iv;
                        requireReinit = true;
                    }
                    reset(doCancel);
                }
            } finally {
                NIO_ACCESS.releaseSession(outBuffer);
            }
        } finally {
            NIO_ACCESS.releaseSession(inBuffer);
        }
    }

    private void handleException(PKCS11Exception e)
            throws ShortBufferException, IllegalBlockSizeException,
            BadPaddingException {
        if (e.match(CKR_BUFFER_TOO_SMALL)) {
            throw (ShortBufferException)
                    (new ShortBufferException().initCause(e));
        } else if (e.match(CKR_DATA_LEN_RANGE) ||
                e.match(CKR_ENCRYPTED_DATA_LEN_RANGE)) {
            throw (IllegalBlockSizeException)
                    (new IllegalBlockSizeException(e.toString()).initCause(e));
        } else if (e.match(CKR_ENCRYPTED_DATA_INVALID) ||
                e.match(CKR_GENERAL_ERROR)) {
            throw (AEADBadTagException)
                    (new AEADBadTagException(e.toString()).initCause(e));
        }
    }

    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException,
            InvalidKeyException {
        throw new UnsupportedOperationException("engineWrap()");
    }

    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm,
            int wrappedKeyType)
            throws InvalidKeyException, NoSuchAlgorithmException {
        throw new UnsupportedOperationException("engineUnwrap()");
    }

    @Override
    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        int n = P11SecretKeyFactory.convertKey
                (token, key, type.keyAlgo).length();
        return n;
    }
}

