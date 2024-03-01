/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.crypto.provider;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.*;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.IntrinsicCandidate;
import sun.security.util.DerValue;

/**
 * Implementation of the ChaCha20 cipher, as described in RFC 7539.
 *
 * @since 11
 */
abstract class ChaCha20Cipher extends CipherSpi {
    private static final int MODE_NONE = 0;
    private static final int MODE_AEAD = 1;

    private static final int STATE_CONST_0 = 0x61707865;
    private static final int STATE_CONST_1 = 0x3320646e;
    private static final int STATE_CONST_2 = 0x79622d32;
    private static final int STATE_CONST_3 = 0x6b206574;

    private static final int KS_MAX_LEN = 1024;
    private static final int KS_BLK_SIZE = 64;
    private static final int KS_SIZE_INTS = KS_BLK_SIZE / Integer.BYTES;

    private boolean initialized;

    protected int mode;

    private int direction;

    private boolean aadDone = false;

    private byte[] keyBytes;

    private byte[] nonce;

    private static final long MAX_UINT32 = 0x00000000FFFFFFFFL;
    private long finalCounterValue;
    private long initCounterValue;
    private long counter;

    private final int[] startState = new int[KS_SIZE_INTS];

    private final byte[] keyStream = new byte[KS_MAX_LEN];

    private int keyStrLimit;
    private int keyStrOffset;

    private static final int TAG_LENGTH = 16;
    private long aadLen;
    private long dataLen;

    private static final byte[] padBuf = new byte[TAG_LENGTH];

    private final byte[] lenBuf = new byte[TAG_LENGTH];

    protected String authAlgName;
    private Poly1305 authenticator;

    private ChaChaEngine engine;

    private static final VarHandle asIntLittleEndian =
            MethodHandles.byteArrayViewVarHandle(int[].class,
                    ByteOrder.LITTLE_ENDIAN);

    private static final VarHandle asLongLittleEndian =
            MethodHandles.byteArrayViewVarHandle(long[].class,
                    ByteOrder.LITTLE_ENDIAN);

    private static final VarHandle asLongView =
            MethodHandles.byteArrayViewVarHandle(long[].class,
                    ByteOrder.nativeOrder());

    /**
     * Default constructor.
     */
    protected ChaCha20Cipher() { }

    /**
     * Set the mode of operation.  Since this is a stream cipher, there
     * is no mode of operation in the block-cipher sense of things.  The
     * protected {@code mode} field will only accept a value of {@code None}
     * (case-insensitive).
     *
     * @param mode The mode value
     *
     * @throws NoSuchAlgorithmException if a mode of operation besides
     *      {@code None} is provided.
     */
    @Override
    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        if (mode.equalsIgnoreCase("None") == false) {
            throw new NoSuchAlgorithmException("Mode must be None");
        }
    }

    /**
     * Set the padding scheme.  Padding schemes do not make sense with stream
     * ciphers, but allow {@code NoPadding}.  See JCE spec.
     *
     * @param padding The padding type.  The only allowed value is
     *      {@code NoPadding} case insensitive.
     *
     * @throws NoSuchPaddingException if a padding scheme besides
     *      {@code NoPadding} is provided.
     */
    @Override
    protected void engineSetPadding(String padding)
            throws NoSuchPaddingException {
        if (padding.equalsIgnoreCase("NoPadding") == false) {
            throw new NoSuchPaddingException("Padding must be NoPadding");
        }
    }

    /**
     * Returns the block size.  For a stream cipher like ChaCha20, this
     * value will always be zero.
     *
     * @return This method always returns 0.  See the JCE Specification.
     */
    @Override
    protected int engineGetBlockSize() {
        return 0;
    }

    /**
     * Get the output size required to hold the result of the next update or
     * doFinal operation.  In simple stream-cipher
     * mode, the output size will equal the input size.  For ChaCha20-Poly1305
     * for encryption the output size will be the sum of the input length
     * and tag length.  For decryption, the output size will be  the input
     * length plus any previously unprocessed data minus the tag
     * length, minimum zero.
     *
     * @param inputLen the length in bytes of the input
     *
     * @return the output length in bytes.
     */
    @Override
    protected int engineGetOutputSize(int inputLen) {
        return engine.getOutputSize(inputLen, true);
    }

    /**
     * Get the nonce value used.
     *
     * @return the nonce bytes.  For ChaCha20 this will be a 12-byte value.
     */
    @Override
    protected byte[] engineGetIV() {
        return (nonce != null) ? nonce.clone() : null;
    }

    /**
     * Get the algorithm parameters for this cipher.  For the ChaCha20
     * cipher, this will always return {@code null} as there currently is
     * no {@code AlgorithmParameters} implementation for ChaCha20.  For
     * ChaCha20-Poly1305, a {@code ChaCha20Poly1305Parameters} object will be
     * created and initialized with the configured nonce value and returned
     * to the caller.
     *
     * @return a {@code null} value if the ChaCha20 cipher is used (mode is
     * MODE_NONE), or a {@code ChaCha20Poly1305Parameters} object containing
     * the nonce if the mode is MODE_AEAD.
     */
    @Override
    protected AlgorithmParameters engineGetParameters() {
        AlgorithmParameters params = null;
        if (mode == MODE_AEAD) {
            byte[] nonceData = (initialized || nonce != null) ? nonce :
                    createRandomNonce(null);
            try {
                params = AlgorithmParameters.getInstance("ChaCha20-Poly1305");
                params.init((new DerValue(
                        DerValue.tag_OctetString, nonceData).toByteArray()));
            } catch (NoSuchAlgorithmException | IOException exc) {
                throw new RuntimeException(exc);
            }
        }

        return params;
    }

    /**
     * Initialize the engine using a key and secure random implementation.  If
     * a SecureRandom object is provided it will be used to create a random
     * nonce value.  If the {@code random} parameter is null an internal
     * secure random source will be used to create the random nonce.
     * The counter value will be set to 1.
     *
     * @param opmode the type of operation to do.  This value may not be
     *      {@code Cipher.DECRYPT_MODE} or {@code Cipher.UNWRAP_MODE} mode
     *      because it must generate random parameters like the nonce.
     * @param key a 256-bit key suitable for ChaCha20
     * @param random a {@code SecureRandom} implementation used to create the
     *      random nonce.  If {@code null} is used for the random object,
     *      then an internal secure random source will be used to create the
     *      nonce.
     *
     * @throws UnsupportedOperationException if the mode of operation
     *      is {@code Cipher.WRAP_MODE} or {@code Cipher.UNWRAP_MODE}
     *      (currently unsupported).
     * @throws InvalidKeyException if the key is of the wrong type or is
     *      not 256-bits in length.  This will also be thrown if the opmode
     *      parameter is {@code Cipher.DECRYPT_MODE}.
     *      {@code Cipher.UNWRAP_MODE} would normally be disallowed in this
     *      context but it is preempted by the UOE case above.
     */
    @Override
    protected void engineInit(int opmode, Key key, SecureRandom random)
            throws InvalidKeyException {
        if (opmode != Cipher.DECRYPT_MODE) {
            byte[] newNonce = createRandomNonce(random);
            counter = 1;
            init(opmode, key, newNonce);
        } else {
            throw new InvalidKeyException("Default parameter generation " +
                "disallowed in DECRYPT and UNWRAP modes");
        }
    }

    /**
     * Initialize the engine using a key and secure random implementation.
     *
     * @param opmode the type of operation to do.  This value must be either
     *      {@code Cipher.ENCRYPT_MODE} or {@code Cipher.DECRYPT_MODE}
     * @param key a 256-bit key suitable for ChaCha20
     * @param params a {@code ChaCha20ParameterSpec} that will provide
     *      the nonce and initial block counter value.
     * @param random a {@code SecureRandom} implementation, this parameter
     *      is not used in this form of the initializer.
     *
     * @throws UnsupportedOperationException if the mode of operation
     *      is {@code Cipher.WRAP_MODE} or {@code Cipher.UNWRAP_MODE}
     *      (currently unsupported).
     * @throws InvalidKeyException if the key is of the wrong type or is
     *      not 256-bits in length.  This will also be thrown if the opmode
     *      parameter is not {@code Cipher.ENCRYPT_MODE} or
     *      {@code Cipher.DECRYPT_MODE} (excepting the UOE case above).
     * @throws InvalidAlgorithmParameterException if {@code params} is
     *      not a {@code ChaCha20ParameterSpec}
     * @throws NullPointerException if {@code params} is {@code null}
     */
    @Override
    protected void engineInit(int opmode, Key key,
            AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {

        if (params == null) {
            engineInit(opmode, key, random);
            return;
        }

        byte[] newNonce = null;
        switch (mode) {
            case MODE_NONE:
                if (!(params instanceof ChaCha20ParameterSpec)) {
                    throw new InvalidAlgorithmParameterException(
                        "ChaCha20 algorithm requires ChaCha20ParameterSpec");
                }
                ChaCha20ParameterSpec chaParams = (ChaCha20ParameterSpec)params;
                newNonce = chaParams.getNonce();
                initCounterValue = ((long)chaParams.getCounter()) &
                        0x00000000FFFFFFFFL;
                counter = initCounterValue;
                break;
            case MODE_AEAD:
                if (!(params instanceof IvParameterSpec)) {
                    throw new InvalidAlgorithmParameterException(
                        "ChaCha20-Poly1305 requires IvParameterSpec");
                }
                IvParameterSpec ivParams = (IvParameterSpec)params;
                newNonce = ivParams.getIV();
                if (newNonce.length != 12) {
                    throw new InvalidAlgorithmParameterException(
                        "ChaCha20-Poly1305 nonce must be 12 bytes in length");
                }
                break;
            default:
                throw new RuntimeException("ChaCha20 in unsupported mode");
        }
        init(opmode, key, newNonce);
    }

    /**
     * Initialize the engine using the {@code AlgorithmParameter} initialization
     * format.  This cipher does supports initialization with
     * {@code AlgorithmParameter} objects for ChaCha20-Poly1305 but not for
     * ChaCha20 as a simple stream cipher.  In the latter case, it will throw
     * an {@code InvalidAlgorithmParameterException} if the value is non-null.
     * If a null value is supplied for the {@code params} field
     * the cipher will be initialized with the counter value set to 1 and
     * a random nonce.  If {@code null} is used for the random object,
     * then an internal secure random source will be used to create the
     * nonce.
     *
     * @param opmode the type of operation to do.  This value must be either
     *      {@code Cipher.ENCRYPT_MODE} or {@code Cipher.DECRYPT_MODE}
     * @param key a 256-bit key suitable for ChaCha20
     * @param params a {@code null} value if the algorithm is ChaCha20, or
     *      the appropriate {@code AlgorithmParameters} object containing the
     *      nonce information if the algorithm is ChaCha20-Poly1305.
     * @param random a {@code SecureRandom} implementation, may be {@code null}.
     *
     * @throws UnsupportedOperationException if the mode of operation
     *      is {@code Cipher.WRAP_MODE} or {@code Cipher.UNWRAP_MODE}
     *      (currently unsupported).
     * @throws InvalidKeyException if the key is of the wrong type or is
     *      not 256-bits in length.  This will also be thrown if the opmode
     *      parameter is not {@code Cipher.ENCRYPT_MODE} or
     *      {@code Cipher.DECRYPT_MODE} (excepting the UOE case above).
     * @throws InvalidAlgorithmParameterException if {@code params} is
     *      non-null and the algorithm is ChaCha20.  This exception will be
     *      also thrown if the algorithm is ChaCha20-Poly1305 and an incorrect
     *      {@code AlgorithmParameters} object is supplied.
     */
    @Override
    protected void engineInit(int opmode, Key key,
            AlgorithmParameters params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {

        if (params == null) {
            engineInit(opmode, key, random);
            return;
        }

        byte[] newNonce;
        switch (mode) {
            case MODE_NONE:
                throw new InvalidAlgorithmParameterException(
                        "AlgorithmParameters not supported");
            case MODE_AEAD:
                String paramAlg = params.getAlgorithm();
                if (!paramAlg.equalsIgnoreCase("ChaCha20-Poly1305")) {
                    throw new InvalidAlgorithmParameterException(
                            "Invalid parameter type: " + paramAlg);
                }
                try {
                    DerValue dv = new DerValue(params.getEncoded());
                    newNonce = dv.getOctetString();
                    if (newNonce.length != 12) {
                        throw new InvalidAlgorithmParameterException(
                                "ChaCha20-Poly1305 nonce must be " +
                                "12 bytes in length");
                    }
                } catch (IOException ioe) {
                    throw new InvalidAlgorithmParameterException(ioe);
                }
                break;
            default:
                throw new RuntimeException("Invalid mode: " + mode);
        }

        init(opmode, key, newNonce);
    }

    /**
     * Update additional authenticated data (AAD).
     *
     * @param src the byte array containing the authentication data.
     * @param offset the starting offset in the buffer to update.
     * @param len the amount of authentication data to update.
     *
     * @throws IllegalStateException if the cipher has not been initialized,
     *      {@code engineUpdate} has been called, or the cipher is running
     *      in a non-AEAD mode of operation.  It will also throw this
     *      exception if the submitted AAD would overflow a 64-bit length
     *      counter.
     */
    @Override
    protected void engineUpdateAAD(byte[] src, int offset, int len) {
        if (!initialized) {
            throw new IllegalStateException(
                    "Attempted to update AAD on uninitialized Cipher");
        } else if (aadDone) {
            throw new IllegalStateException("Attempted to update AAD on " +
                    "Cipher after plaintext/ciphertext update");
        } else if (mode != MODE_AEAD) {
            throw new IllegalStateException(
                    "Cipher is running in non-AEAD mode");
        } else {
            try {
                aadLen = Math.addExact(aadLen, len);
                authUpdate(src, offset, len);
            } catch (ArithmeticException ae) {
                throw new IllegalStateException("AAD overflow", ae);
            }
        }
    }

    /**
     * Update additional authenticated data (AAD).
     *
     * @param src the ByteBuffer containing the authentication data.
     *
     * @throws IllegalStateException if the cipher has not been initialized,
     *      {@code engineUpdate} has been called, or the cipher is running
     *      in a non-AEAD mode of operation.  It will also throw this
     *      exception if the submitted AAD would overflow a 64-bit length
     *      counter.
     */
    @Override
    protected void engineUpdateAAD(ByteBuffer src) {
        if (!initialized) {
            throw new IllegalStateException(
                    "Attempted to update AAD on uninitialized Cipher");
        } else if (aadDone) {
            throw new IllegalStateException("Attempted to update AAD on " +
                    "Cipher after plaintext/ciphertext update");
        } else if (mode != MODE_AEAD) {
            throw new IllegalStateException(
                    "Cipher is running in non-AEAD mode");
        } else {
            try {
                aadLen = Math.addExact(aadLen, (src.limit() - src.position()));
                authenticator.engineUpdate(src);
            } catch (ArithmeticException ae) {
                throw new IllegalStateException("AAD overflow", ae);
            }
        }
    }

    /**
     * Create a random 12-byte nonce.
     *
     * @param random a {@code SecureRandom} object.  If {@code null} is
     * provided a new {@code SecureRandom} object will be instantiated.
     *
     * @return a 12-byte array containing the random nonce.
     */
    private static byte[] createRandomNonce(SecureRandom random) {
        byte[] newNonce = new byte[12];
        SecureRandom rand = (random != null) ? random : new SecureRandom();
        rand.nextBytes(newNonce);
        return newNonce;
    }

    /**
     * Perform additional initialization actions based on the key and operation
     * type.
     *
     * @param opmode the type of operation to do.  This value must be either
     *      {@code Cipher.ENCRYPT_MODE} or {@code Cipher.DECRYPT_MODE}
     * @param key a 256-bit key suitable for ChaCha20
     * @param newNonce the new nonce value for this initialization.
     *
     * @throws UnsupportedOperationException if the {@code opmode} parameter
     *      is {@code Cipher.WRAP_MODE} or {@code Cipher.UNWRAP_MODE}
     *      (currently unsupported).
     * @throws InvalidKeyException if the {@code opmode} parameter is not
     *      {@code Cipher.ENCRYPT_MODE} or {@code Cipher.DECRYPT_MODE}, or
     *      if the key format is not {@code RAW}.
     */
    private void init(int opmode, Key key, byte[] newNonce)
            throws InvalidKeyException {
        if ((opmode == Cipher.WRAP_MODE) || (opmode == Cipher.UNWRAP_MODE)) {
            throw new UnsupportedOperationException(
                    "WRAP_MODE and UNWRAP_MODE are not currently supported");
        }

        byte[] newKeyBytes = getEncodedKey(key);
        if (opmode == Cipher.ENCRYPT_MODE) {
            checkKeyAndNonce(newKeyBytes, newNonce);
        }
        if (this.keyBytes != null) {
            Arrays.fill(this.keyBytes, (byte)0);
        }
        this.keyBytes = newKeyBytes;
        nonce = newNonce;

        setInitialState();

        if (mode == MODE_NONE) {
            engine = new EngineStreamOnly();
        } else if (mode == MODE_AEAD) {
            if (opmode == Cipher.ENCRYPT_MODE) {
                engine = new EngineAEADEnc();
            } else if (opmode == Cipher.DECRYPT_MODE) {
                engine = new EngineAEADDec();
            } else {
                throw new InvalidKeyException("Not encrypt or decrypt mode");
            }
        }

        finalCounterValue = counter + MAX_UINT32;
        this.keyStrLimit = chaCha20Block(startState, counter, keyStream);
        this.keyStrOffset = 0;
        this.counter += (keyStrLimit / KS_BLK_SIZE);
        direction = opmode;
        aadDone = false;
        initialized = true;
    }

    /**
     * Check the key and nonce bytes to make sure that they do not repeat
     * across reinitialization.
     *
     * @param newKeyBytes the byte encoding for the newly provided key
     * @param newNonce the new nonce to be used with this initialization
     *
     * @throws InvalidKeyException if both the key and nonce match the
     *      previous initialization.
     *
     */
    private void checkKeyAndNonce(byte[] newKeyBytes, byte[] newNonce)
            throws InvalidKeyException {
        if (MessageDigest.isEqual(newKeyBytes, keyBytes) &&
                MessageDigest.isEqual(newNonce, nonce)) {
            throw new InvalidKeyException(
                    "Matching key and nonce from previous initialization");
        }
    }

    /**
     * Return the encoded key as a byte array
     *
     * @param key the {@code Key} object used for this {@code Cipher}
     *
     * @return the key bytes
     *
     * @throws InvalidKeyException if the key is of the wrong type or length,
     *      or if the key encoding format is not {@code RAW}.
     */
    private static byte[] getEncodedKey(Key key) throws InvalidKeyException {
        if ("RAW".equals(key.getFormat()) == false) {
            throw new InvalidKeyException("Key encoding format must be RAW");
        }
        byte[] encodedKey = key.getEncoded();
        if (encodedKey == null || encodedKey.length != 32) {
            if (encodedKey != null) {
                Arrays.fill(encodedKey, (byte)0);
            }
            throw new InvalidKeyException("Key length must be 256 bits");
        }
        return encodedKey;
    }

    /**
     * Update the currently running operation with additional data
     *
     * @param in the plaintext or ciphertext input bytes (depending on the
     *      operation type).
     * @param inOfs the offset into the input array
     * @param inLen the length of the data to use for the update operation.
     *
     * @return the resulting plaintext or ciphertext bytes (depending on
     *      the operation type)
     */
    @Override
    protected byte[] engineUpdate(byte[] in, int inOfs, int inLen) {
        byte[] out = new byte[engine.getOutputSize(inLen, false)];
        try {
            engine.doUpdate(in, inOfs, inLen, out, 0);
        } catch (ShortBufferException | KeyException exc) {
            throw new ProviderException(exc);
        }

        return out;
    }

    /**
     * Update the currently running operation with additional data
     *
     * @param in the plaintext or ciphertext input bytes (depending on the
     *      operation type).
     * @param inOfs the offset into the input array
     * @param inLen the length of the data to use for the update operation.
     * @param out the byte array that will hold the resulting data.  The array
     *      must be large enough to hold the resulting data.
     * @param outOfs the offset for the {@code out} buffer to begin writing
     *      the resulting data.
     *
     * @return the length in bytes of the data written into the {@code out}
     *      buffer.
     *
     * @throws ShortBufferException if the buffer {@code out} does not have
     *      enough space to hold the resulting data.
     */
    @Override
    protected int engineUpdate(byte[] in, int inOfs, int inLen,
            byte[] out, int outOfs) throws ShortBufferException {
        int bytesUpdated = 0;
        try {
            bytesUpdated = engine.doUpdate(in, inOfs, inLen, out, outOfs);
        } catch (KeyException ke) {
            throw new ProviderException(ke);
        }
        return bytesUpdated;
    }

    /**
     * Update the currently running operation with additional data
     *
     * @param input the plaintext or ciphertext ByteBuffer
     * @param output ByteBuffer that will hold the resulting data.  This
     *      must be large enough to hold the resulting data.
     *
     * @return the length in bytes of the data written into the {@code out}
     *      buffer.
     *
     * @throws ShortBufferException if the buffer {@code out} does not have
     *      enough space to hold the resulting data.
     */
    @Override
    protected int engineUpdate(ByteBuffer input, ByteBuffer output)
        throws ShortBufferException {
        try {
            return bufferCrypt(input, output, true);
        } catch (AEADBadTagException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Complete the currently running operation using any final
     * data provided by the caller.
     *
     * @param in the plaintext or ciphertext input bytes (depending on the
     *      operation type).
     * @param inOfs the offset into the input array
     * @param inLen the length of the data to use for the update operation.
     *
     * @return the resulting plaintext or ciphertext bytes (depending on
     *      the operation type)
     *
     * @throws AEADBadTagException if, during decryption, the provided tag
     *      does not match the calculated tag.
     */
    @Override
    protected byte[] engineDoFinal(byte[] in, int inOfs, int inLen)
            throws AEADBadTagException {
        byte[] output = new byte[engine.getOutputSize(inLen, true)];
        try {
            engine.doFinal(in, inOfs, inLen, output, 0);
        } catch (ShortBufferException | KeyException exc) {
            throw new RuntimeException(exc);
        } finally {
            resetStartState();
        }
        return output;
    }

    /**
     * Complete the currently running operation using any final
     * data provided by the caller.
     *
     * @param in the plaintext or ciphertext input bytes (depending on the
     *      operation type).
     * @param inOfs the offset into the input array
     * @param inLen the length of the data to use for the update operation.
     * @param out the byte array that will hold the resulting data.  The array
     *      must be large enough to hold the resulting data.
     * @param outOfs the offset for the {@code out} buffer to begin writing
     *      the resulting data.
     *
     * @return the length in bytes of the data written into the {@code out}
     *      buffer.
     *
     * @throws ShortBufferException if the buffer {@code out} does not have
     *      enough space to hold the resulting data.
     * @throws AEADBadTagException if, during decryption, the provided tag
     *      does not match the calculated tag.
     */
    @Override
    protected int engineDoFinal(byte[] in, int inOfs, int inLen, byte[] out,
            int outOfs) throws ShortBufferException, AEADBadTagException {

        int bytesUpdated = 0;
        try {
            bytesUpdated = engine.doFinal(in, inOfs, inLen, out, outOfs);
        } catch (KeyException ke) {
            throw new RuntimeException(ke);
        } finally {
            resetStartState();
        }
        return bytesUpdated;
    }

    /**
     * Complete the currently running operation using any final
     * data provided by the caller.
     *
     * @param input the plaintext or ciphertext input bytebuffer.
     * @param output ByteBuffer that will hold the resulting data.  This
     *      must be large enough to hold the resulting data.
     *
     * @return the resulting plaintext or ciphertext bytes.
     *
     * @throws AEADBadTagException if, during decryption, the provided tag
     *      does not match the calculated tag.
     */
    @Override
    protected int engineDoFinal(ByteBuffer input, ByteBuffer output)
        throws ShortBufferException, AEADBadTagException {
        return bufferCrypt(input, output, false);
    }

    /*
     * Optimized version of bufferCrypt from CipherSpi.java.  Direct
     * ByteBuffers send to the engine code.
     */
    private int bufferCrypt(ByteBuffer input, ByteBuffer output,
        boolean isUpdate) throws ShortBufferException, AEADBadTagException {
        if ((input == null) || (output == null)) {
            throw new NullPointerException
                ("Input and output buffers must not be null");
        }
        int inPos = input.position();
        int inLimit = input.limit();
        int inLen = inLimit - inPos;
        if (isUpdate && (inLen == 0)) {
            return 0;
        }
        int outLenNeeded = engine.getOutputSize(inLen, !isUpdate);

        if (output.remaining() < outLenNeeded) {
            throw new ShortBufferException("Need at least " + outLenNeeded
                + " bytes of space in output buffer");
        }

        int total = 0;

        if (input.hasArray()) {
            byte[] inArray = input.array();
            int inOfs = input.arrayOffset() + inPos;

            byte[] outArray;
            if (output.hasArray()) {
                outArray = output.array();
                int outPos = output.position();
                int outOfs = output.arrayOffset() + outPos;

                boolean useTempOut = false;
                if (inArray == outArray &&
                    ((inOfs < outOfs) && (outOfs < inOfs + inLen))) {
                    useTempOut = true;
                    outArray = new byte[outLenNeeded];
                    outOfs = 0;
                }
                try {
                    if (isUpdate) {
                        total = engine.doUpdate(inArray, inOfs, inLen, outArray,
                            outOfs);
                    } else {
                        total = engine.doFinal(inArray, inOfs, inLen, outArray,
                            outOfs);
                    }
                } catch (KeyException e) {
                    throw new ProviderException(e);
                }
                if (useTempOut) {
                    output.put(outArray, outOfs, total);
                } else {
                    output.position(outPos + total);
                }
            } else { 
                if (isUpdate) {
                    outArray = engineUpdate(inArray, inOfs, inLen);
                } else {
                    outArray = engineDoFinal(inArray, inOfs, inLen);
                }
                if (outArray != null && outArray.length != 0) {
                    output.put(outArray);
                    total = outArray.length;
                }
            }
            input.position(inLimit);
        } else {  
            try {
                if (isUpdate) {
                    return engine.doUpdate(input, output);
                }
                return engine.doFinal(input, output);
            } catch (KeyException e) {
                throw new ProviderException(e);
            }
        }

        return total;
    }



    /**
     * Wrap a {@code Key} using this Cipher's current encryption parameters.
     *
     * @param key the key to wrap.  The data that will be encrypted will
     *      be the provided {@code Key} in its encoded form.
     *
     * @return a byte array consisting of the wrapped key.
     *
     * @throws UnsupportedOperationException this will (currently) always
     *      be thrown, as this method is not currently supported.
     */
    @Override
    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException,
            InvalidKeyException {
        throw new UnsupportedOperationException(
                "Wrap operations are not supported");
    }

    /**
     * Unwrap a {@code Key} using this Cipher's current encryption parameters.
     *
     * @param wrappedKey the key to unwrap.
     * @param algorithm the algorithm associated with the wrapped key
     * @param type the type of the wrapped key. This is one of
     *      {@code SECRET_KEY}, {@code PRIVATE_KEY}, or {@code PUBLIC_KEY}.
     *
     * @return the unwrapped key as a {@code Key} object.
     *
     * @throws UnsupportedOperationException this will (currently) always
     *      be thrown, as this method is not currently supported.
     */
    @Override
    protected Key engineUnwrap(byte[] wrappedKey, String algorithm,
            int type) throws InvalidKeyException, NoSuchAlgorithmException {
        throw new UnsupportedOperationException(
                "Unwrap operations are not supported");
    }

    /**
     * Get the length of a provided key in bits.
     *
     * @param key the key to be evaluated
     *
     * @return the length of the key in bits
     *
     * @throws InvalidKeyException if the key is invalid or does not
     *      have an encoded form.
     */
    @Override
    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        byte[] encodedKey = getEncodedKey(key);
        Arrays.fill(encodedKey, (byte)0);
        return encodedKey.length << 3;
    }

    /**
     * Set the initial state.  This will populate the state array and put the
     * key and nonce into their proper locations.  The counter field is not
     * set here.
     *
     * @throws IllegalArgumentException if the key or nonce are not in
     *      their proper lengths (32 bytes for the key, 12 bytes for the
     *      nonce).
     * @throws InvalidKeyException if the key does not support an encoded form.
     */
    private void setInitialState() throws InvalidKeyException {
        startState[0] = STATE_CONST_0;
        startState[1] = STATE_CONST_1;
        startState[2] = STATE_CONST_2;
        startState[3] = STATE_CONST_3;

        for (int i = 0; i < 32; i += 4) {
            startState[(i / 4) + 4] = (keyBytes[i] & 0x000000FF) |
                ((keyBytes[i + 1] << 8) & 0x0000FF00) |
                ((keyBytes[i + 2] << 16) & 0x00FF0000) |
                ((keyBytes[i + 3] << 24) & 0xFF000000);
        }

        startState[12] = 0;

        for (int i = 0; i < 12; i += 4) {
            startState[(i / 4) + 13] = (nonce[i] & 0x000000FF) |
                ((nonce[i + 1] << 8) & 0x0000FF00) |
                ((nonce[i + 2] << 16) & 0x00FF0000) |
                ((nonce[i + 3] << 24) & 0xFF000000);
        }
    }

    @ForceInline
    private static int chaCha20Block(int[] initState, long counter,
            byte[] result) {
        if (initState.length != KS_SIZE_INTS || result.length != KS_MAX_LEN) {
            throw new IllegalArgumentException(
                    "Illegal state or keystream buffer length");
        }

        initState[12] = (int)counter;
        return implChaCha20Block(initState, result);
    }

    /**
     * Perform a full 20-round ChaCha20 transform on the initial state.
     *
     * @param initState the starting state using the current counter value.
     * @param result  the array that will hold the result of the ChaCha20
     *      block function.
     *
     * @return the number of keystream bytes generated.  In a pure Java method
     *      this will always be 64 bytes, but intrinsics that make use of
     *      AVX2 or AVX512 registers may generate multiple blocks of keystream
     *      in a single call and therefore may be a larger multiple of 64.
     */
    @IntrinsicCandidate
    private static int implChaCha20Block(int[] initState, byte[] result) {
        int ws00 = STATE_CONST_0;
        int ws01 = STATE_CONST_1;
        int ws02 = STATE_CONST_2;
        int ws03 = STATE_CONST_3;
        int ws04 = initState[4];
        int ws05 = initState[5];
        int ws06 = initState[6];
        int ws07 = initState[7];
        int ws08 = initState[8];
        int ws09 = initState[9];
        int ws10 = initState[10];
        int ws11 = initState[11];
        int ws12 = initState[12];
        int ws13 = initState[13];
        int ws14 = initState[14];
        int ws15 = initState[15];

        for (int round = 0; round < 10; round++) {
            ws00 += ws04;
            ws12 = Integer.rotateLeft(ws12 ^ ws00, 16);

            ws08 += ws12;
            ws04 = Integer.rotateLeft(ws04 ^ ws08, 12);

            ws00 += ws04;
            ws12 = Integer.rotateLeft(ws12 ^ ws00, 8);

            ws08 += ws12;
            ws04 = Integer.rotateLeft(ws04 ^ ws08, 7);

            ws01 += ws05;
            ws13 = Integer.rotateLeft(ws13 ^ ws01, 16);

            ws09 += ws13;
            ws05 = Integer.rotateLeft(ws05 ^ ws09, 12);

            ws01 += ws05;
            ws13 = Integer.rotateLeft(ws13 ^ ws01, 8);

            ws09 += ws13;
            ws05 = Integer.rotateLeft(ws05 ^ ws09, 7);

            ws02 += ws06;
            ws14 = Integer.rotateLeft(ws14 ^ ws02, 16);

            ws10 += ws14;
            ws06 = Integer.rotateLeft(ws06 ^ ws10, 12);

            ws02 += ws06;
            ws14 = Integer.rotateLeft(ws14 ^ ws02, 8);

            ws10 += ws14;
            ws06 = Integer.rotateLeft(ws06 ^ ws10, 7);

            ws03 += ws07;
            ws15 = Integer.rotateLeft(ws15 ^ ws03, 16);

            ws11 += ws15;
            ws07 = Integer.rotateLeft(ws07 ^ ws11, 12);

            ws03 += ws07;
            ws15 = Integer.rotateLeft(ws15 ^ ws03, 8);

            ws11 += ws15;
            ws07 = Integer.rotateLeft(ws07 ^ ws11, 7);

            ws00 += ws05;
            ws15 = Integer.rotateLeft(ws15 ^ ws00, 16);

            ws10 += ws15;
            ws05 = Integer.rotateLeft(ws05 ^ ws10, 12);

            ws00 += ws05;
            ws15 = Integer.rotateLeft(ws15 ^ ws00, 8);

            ws10 += ws15;
            ws05 = Integer.rotateLeft(ws05 ^ ws10, 7);

            ws01 += ws06;
            ws12 = Integer.rotateLeft(ws12 ^ ws01, 16);

            ws11 += ws12;
            ws06 = Integer.rotateLeft(ws06 ^ ws11, 12);

            ws01 += ws06;
            ws12 = Integer.rotateLeft(ws12 ^ ws01, 8);

            ws11 += ws12;
            ws06 = Integer.rotateLeft(ws06 ^ ws11, 7);

            ws02 += ws07;
            ws13 = Integer.rotateLeft(ws13 ^ ws02, 16);

            ws08 += ws13;
            ws07 = Integer.rotateLeft(ws07 ^ ws08, 12);

            ws02 += ws07;
            ws13 = Integer.rotateLeft(ws13 ^ ws02, 8);

            ws08 += ws13;
            ws07 = Integer.rotateLeft(ws07 ^ ws08, 7);

            ws03 += ws04;
            ws14 = Integer.rotateLeft(ws14 ^ ws03, 16);

            ws09 += ws14;
            ws04 = Integer.rotateLeft(ws04 ^ ws09, 12);

            ws03 += ws04;
            ws14 = Integer.rotateLeft(ws14 ^ ws03, 8);

            ws09 += ws14;
            ws04 = Integer.rotateLeft(ws04 ^ ws09, 7);
        }

        asIntLittleEndian.set(result, 0, ws00 + STATE_CONST_0);
        asIntLittleEndian.set(result, 4, ws01 + STATE_CONST_1);
        asIntLittleEndian.set(result, 8, ws02 + STATE_CONST_2);
        asIntLittleEndian.set(result, 12, ws03 + STATE_CONST_3);
        asIntLittleEndian.set(result, 16, ws04 + initState[4]);
        asIntLittleEndian.set(result, 20, ws05 + initState[5]);
        asIntLittleEndian.set(result, 24, ws06 + initState[6]);
        asIntLittleEndian.set(result, 28, ws07 + initState[7]);
        asIntLittleEndian.set(result, 32, ws08 + initState[8]);
        asIntLittleEndian.set(result, 36, ws09 + initState[9]);
        asIntLittleEndian.set(result, 40, ws10 + initState[10]);
        asIntLittleEndian.set(result, 44, ws11 + initState[11]);
        asIntLittleEndian.set(result, 48, ws12 + initState[12]);
        asIntLittleEndian.set(result, 52, ws13 + initState[13]);
        asIntLittleEndian.set(result, 56, ws14 + initState[14]);
        asIntLittleEndian.set(result, 60, ws15 + initState[15]);

        return KS_BLK_SIZE;
    }

    /**
     * Perform the ChaCha20 transform.
     *
     * @param in the array of bytes for the input
     * @param inOff the offset into the input array to start the transform
     * @param inLen the length of the data to perform the transform on.
     * @param out the output array.  It must be large enough to hold the
     *      resulting data
     * @param outOff the offset into the output array to place the resulting
     *      data.
     */
    private void chaCha20Transform(byte[] in, int inOff, int inLen,
            byte[] out, int outOff) throws KeyException {
        int remainingData = inLen;

        while (remainingData > 0) {
            int ksRemain = keyStrLimit - keyStrOffset;
            if (ksRemain <= 0) {
                if (counter <= finalCounterValue) {
                    keyStrLimit = chaCha20Block(startState, counter, keyStream);
                    counter += (keyStrLimit / KS_BLK_SIZE);
                    if (counter > finalCounterValue) {
                        keyStrLimit -= (int)(counter - finalCounterValue) * 64;
                    }
                    keyStrOffset = 0;
                    ksRemain = keyStrLimit;
                } else {
                    throw new KeyException("Counter exhausted.  " +
                            "Reinitialize with new key and/or nonce");
                }
            }

            int xformLen = Math.min(remainingData, ksRemain);
            xor(keyStream, keyStrOffset, in, inOff, out, outOff, xformLen);
            outOff += xformLen;
            inOff += xformLen;
            keyStrOffset += xformLen;
            remainingData -= xformLen;
        }
    }

    private static void xor(byte[] in1, int off1, byte[] in2, int off2,
            byte[] out, int outOff, int len) {
        while (len >= 8) {
            long v1 = (long) asLongView.get(in1, off1);
            long v2 = (long) asLongView.get(in2, off2);
            asLongView.set(out, outOff, v1 ^ v2);
            off1 += 8;
            off2 += 8;
            outOff += 8;
            len -= 8;
        }
        while (len > 0) {
            out[outOff] = (byte) (in1[off1] ^ in2[off2]);
            off1++;
            off2++;
            outOff++;
            len--;
        }
    }

    /**
     * Perform initialization steps for the authenticator
     *
     * @throws InvalidKeyException if the key is unusable for some reason
     *      (invalid length, etc.)
     */
    private void initAuthenticator() throws InvalidKeyException {
        authenticator = new Poly1305();

        byte[] serializedKey = new byte[KS_MAX_LEN];
        chaCha20Block(startState, 0L, serializedKey);

        authenticator.engineInit(new SecretKeySpec(serializedKey, 0, 32,
                authAlgName), null);
        aadLen = 0;
        dataLen = 0;
    }

    /**
     * Update the authenticator state with data.  This routine can be used
     * to add data to the authenticator, whether AAD or application data.
     *
     * @param data the data to stir into the authenticator.
     * @param offset the offset into the data.
     * @param length the length of data to add to the authenticator.
     *
     * @return the number of bytes processed by this method.
     */
    private int authUpdate(byte[] data, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, data.length);
        authenticator.engineUpdate(data, offset, length);
        return length;
    }

    /**
     * Finalize the data and return the tag.
     *
     * @param data an array containing any remaining data to process.
     * @param dataOff the offset into the data.
     * @param length the length of the data to process.
     * @param out the array to write the resulting tag into
     * @param outOff the offset to begin writing the data.
     *
     * @throws ShortBufferException if there is insufficient room to
     *      write the tag.
     */
    private void authFinalizeData(byte[] data, int dataOff, int length,
            byte[] out, int outOff) throws ShortBufferException {
        if (data != null) {
            dataLen += authUpdate(data, dataOff, length);
        }
        authPad16(dataLen);

        authWriteLengths(aadLen, dataLen, lenBuf);
        authenticator.engineUpdate(lenBuf, 0, lenBuf.length);
        byte[] tag = authenticator.engineDoFinal();
        Objects.checkFromIndexSize(outOff, tag.length, out.length);
        System.arraycopy(tag, 0, out, outOff, tag.length);
        aadLen = 0;
        dataLen = 0;
    }

    /**
     * Based on a given length of data, make the authenticator process
     * zero bytes that will pad the length out to a multiple of 16.
     *
     * @param dataLen the starting length to be padded.
     */
    private void authPad16(long dataLen) {
        authenticator.engineUpdate(padBuf, 0,
                (TAG_LENGTH - ((int)dataLen & 15)) & 15);
    }

    /**
     * Write the two 64-bit little-endian length fields into an array
     * for processing by the poly1305 authenticator.
     *
     * @param aLen the length of the AAD.
     * @param dLen the length of the application data.
     * @param buf the buffer to write the two lengths into.
     *
     * @note it is the caller's responsibility to provide an array large
     *      enough to hold the two longs.
     */
    private void authWriteLengths(long aLen, long dLen, byte[] buf) {
        asLongLittleEndian.set(buf, 0, aLen);
        asLongLittleEndian.set(buf, Long.BYTES, dLen);
    }

    /**
     * reset the Cipher's state to the values it had after
     * the initial init() call.
     *
     * Note: The cipher's internal "initialized" field is set differently
     * for ENCRYPT_MODE and DECRYPT_MODE in order to allow DECRYPT_MODE
     * ciphers to reuse the key/nonce/counter values.  This kind of reuse
     * is disallowed in ENCRYPT_MODE.
     */
    private void resetStartState() {
        keyStrLimit = 0;
        keyStrOffset = 0;
        counter = initCounterValue;
        aadDone = false;
        initialized = (direction == Cipher.DECRYPT_MODE);
    }

    /**
     * Interface for the underlying processing engines for ChaCha20
     */
    interface ChaChaEngine {
        /**
         * Size an output buffer based on the input and where applicable
         * the current state of the engine in a multipart operation.
         *
         * @param inLength the input length.
         * @param isFinal true if this is invoked from a doFinal call.
         *
         * @return the recommended size for the output buffer.
         */
        int getOutputSize(int inLength, boolean isFinal);

        /**
         * Perform a multi-part update for ChaCha20.
         *
         * @param in the input data.
         * @param inOff the offset into the input.
         * @param inLen the length of the data to process.
         * @param out the output buffer.
         * @param outOff the offset at which to write the output data.
         *
         * @return the number of output bytes written.
         *
         * @throws ShortBufferException if the output buffer does not
         *      provide enough space.
         * @throws KeyException if the counter value has been exhausted.
         */
        int doUpdate(byte[] in, int inOff, int inLen, byte[] out, int outOff)
                throws ShortBufferException, KeyException;

        /**
         * Finalize a multi-part or single-part ChaCha20 operation.
         *
         * @param in the input data.
         * @param inOff the offset into the input.
         * @param inLen the length of the data to process.
         * @param out the output buffer.
         * @param outOff the offset at which to write the output data.
         *
         * @return the number of output bytes written.
         *
         * @throws ShortBufferException if the output buffer does not
         *      provide enough space.
         * @throws AEADBadTagException if in decryption mode the provided
         *      tag and calculated tag do not match.
         * @throws KeyException if the counter value has been exhausted.
         */
        int doFinal(byte[] in, int inOff, int inLen, byte[] out, int outOff)
                throws ShortBufferException, AEADBadTagException, KeyException;

        int doUpdate(ByteBuffer input, ByteBuffer output) throws
            ShortBufferException, KeyException;
        int doFinal(ByteBuffer input, ByteBuffer output) throws
            ShortBufferException, KeyException, AEADBadTagException;
    }

    private final class EngineStreamOnly implements ChaChaEngine {

        private EngineStreamOnly () { }

        @Override
        public int getOutputSize(int inLength, boolean isFinal) {
            return inLength;
        }

        @Override
        public int doUpdate(byte[] in, int inOff, int inLen, byte[] out,
                int outOff) throws ShortBufferException, KeyException {
            if (initialized) {
               try {
                    if (out != null) {
                        Objects.checkFromIndexSize(outOff, inLen, out.length);
                    } else {
                        throw new ShortBufferException(
                                "Output buffer too small");
                    }
                } catch (IndexOutOfBoundsException iobe) {
                    throw new ShortBufferException("Output buffer too small");
                }
                if (in != null) {
                    Objects.checkFromIndexSize(inOff, inLen, in.length);
                    chaCha20Transform(in, inOff, inLen, out, outOff);
                }
                return inLen;
            } else {
                throw new IllegalStateException(
                        "Must use either a different key or iv.");
            }
        }

        @Override
        public int doFinal(byte[] in, int inOff, int inLen, byte[] out,
                int outOff) throws ShortBufferException, KeyException {
            return doUpdate(in, inOff, inLen, out, outOff);
        }

        @Override
        public int doUpdate(ByteBuffer input, ByteBuffer output) throws
            ShortBufferException, KeyException {
            byte[] data = new byte[input.remaining()];
            input.get(data);
            doUpdate(data, 0, data.length, data, 0);
            output.put(data);
            return data.length;
        }

        @Override
        public int doFinal(ByteBuffer input, ByteBuffer output)
            throws ShortBufferException, KeyException {
            return doUpdate(input, output);
        }
    }

    private final class EngineAEADEnc implements ChaChaEngine {

        @Override
        public int getOutputSize(int inLength, boolean isFinal) {
            return (isFinal ? Math.addExact(inLength, TAG_LENGTH) : inLength);
        }

        private EngineAEADEnc() throws InvalidKeyException {
            initAuthenticator();
            initCounterValue = 1;
            counter = initCounterValue;
        }

        @Override
        public int doUpdate(byte[] in, int inOff, int inLen, byte[] out,
                int outOff) throws ShortBufferException, KeyException {
            if (initialized) {
                if (!aadDone) {
                    authPad16(aadLen);
                    aadDone = true;
                }
                try {
                    if (out != null) {
                        Objects.checkFromIndexSize(outOff, inLen, out.length);
                    } else {
                        throw new ShortBufferException(
                                "Output buffer too small");
                    }
                } catch (IndexOutOfBoundsException iobe) {
                    throw new ShortBufferException("Output buffer too small");
                }
                if (in != null) {
                    Objects.checkFromIndexSize(inOff, inLen, in.length);
                    chaCha20Transform(in, inOff, inLen, out, outOff);
                    dataLen += authUpdate(out, outOff, inLen);
                }

                return inLen;
            } else {
                throw new IllegalStateException(
                        "Must use either a different key or iv.");
            }
        }

        @Override
        public int doFinal(byte[] in, int inOff, int inLen, byte[] out,
                int outOff) throws ShortBufferException, KeyException {
            if ((inLen + TAG_LENGTH) > (out.length - outOff)) {
                throw new ShortBufferException("Output buffer too small");
            }

            doUpdate(in, inOff, inLen, out, outOff);
            authFinalizeData(null, 0, 0, out, outOff + inLen);
            aadDone = false;
            return inLen + TAG_LENGTH;
        }

        @Override
        public int doUpdate(ByteBuffer input, ByteBuffer output) throws
            ShortBufferException, KeyException {
            byte[] data = new byte[input.remaining()];
            input.get(data);
            doUpdate(data, 0, data.length, data, 0);
            output.put(data);
            return data.length;
        }

        @Override
        public int doFinal(ByteBuffer input, ByteBuffer output) throws
            ShortBufferException, KeyException {
            int len = input.remaining();
            byte[] data = new byte[len + TAG_LENGTH];
            input.get(data, 0, len);
            doFinal(data, 0, len, data, 0);
            output.put(data);
            return data.length;
        }
    }

    private final class EngineAEADDec implements ChaChaEngine {

        private AEADBufferedStream cipherBuf = null;
        private final byte[] tag;

        @Override
        public int getOutputSize(int inLen, boolean isFinal) {
            return (isFinal ?
                    Integer.max(Math.addExact((inLen - TAG_LENGTH),
                            getBufferedLength()), 0) : 0);
        }

        private void initBuffer(int len) {
            if (cipherBuf == null) {
                cipherBuf = new AEADBufferedStream(len);
            }
        }

        private int getBufferedLength() {
            if (cipherBuf != null) {
                return cipherBuf.size();
            }
            return 0;
        }

        private EngineAEADDec() throws InvalidKeyException {
            initAuthenticator();
            initCounterValue = 1;
            counter = initCounterValue;
            tag = new byte[TAG_LENGTH];
        }

        @Override
        public int doUpdate(byte[] in, int inOff, int inLen, byte[] out,
            int outOff) {
            if (initialized) {
                if (!aadDone) {
                    authPad16(aadLen);
                    aadDone = true;
                }

                if (in != null) {
                    Objects.checkFromIndexSize(inOff, inLen, in.length);
                    initBuffer(inLen);
                    cipherBuf.write(in, inOff, inLen);
                }
            } else {
                throw new IllegalStateException(
                        "Must use either a different key or iv.");
            }

            return 0;
        }


        @Override
        public int doUpdate(ByteBuffer input, ByteBuffer output) {
            initBuffer(input.remaining());
            cipherBuf.write(input);
            return 0;
        }

        @Override
        public int doFinal(byte[] in, int inOff, int inLen, byte[] out,
                int outOff) throws ShortBufferException, AEADBadTagException,
                KeyException {

            byte[] ctPlusTag;
            int ctPlusTagLen;
            if (getBufferedLength() == 0) {
                doUpdate(null, inOff, inLen, out, outOff);
                ctPlusTag = in;
                ctPlusTagLen = inLen;
            } else {
                doUpdate(in, inOff, inLen, out, outOff);
                ctPlusTag = cipherBuf.getBuffer();
                inOff = 0;
                ctPlusTagLen = cipherBuf.size();
                cipherBuf.reset();
            }

            if (ctPlusTagLen < TAG_LENGTH) {
                throw new AEADBadTagException("Input too short - need tag");
            }
            int ctLen = ctPlusTagLen - TAG_LENGTH;

            try {
                Objects.checkFromIndexSize(outOff, ctLen, out.length);
            } catch (IndexOutOfBoundsException ioobe) {
                throw new ShortBufferException("Output buffer too small");
            }

            authFinalizeData(ctPlusTag, inOff, ctLen, tag, 0);
            long tagCompare = ((long)asLongView.get(ctPlusTag, ctLen + inOff) ^
                    (long)asLongView.get(tag, 0)) |
                    ((long)asLongView.get(ctPlusTag, ctLen + inOff + Long.BYTES) ^
                    (long)asLongView.get(tag, Long.BYTES));
            if (tagCompare != 0) {
                throw new AEADBadTagException("Tag mismatch");
            }
            chaCha20Transform(ctPlusTag, inOff, ctLen, out, outOff);
            aadDone = false;

            return ctLen;
        }

        @Override
        public int doFinal(ByteBuffer input, ByteBuffer output)
            throws ShortBufferException, AEADBadTagException, KeyException {
            int len;
            int inLen = input.remaining();
            byte[] ct = null, buf = null;
            int bufLen = 0;
            int ctLen = getBufferedLength() + inLen;

            if (ctLen < TAG_LENGTH) {
                throw new AEADBadTagException("Input too short - need tag");
            }

            if (inLen < TAG_LENGTH) {
                if (inLen > 0) {
                    doUpdate(input, output);
                }
                if (cipherBuf != null) {
                    ct = cipherBuf.getBuffer();
                }
                len = ctLen;
            } else {
                if (cipherBuf != null) {
                    buf = cipherBuf.getBuffer();
                    bufLen = cipherBuf.size();
                }
                ct = new byte[inLen];
                input.get(ct, 0, inLen);
                len = inLen;
            }
            doUpdate(null, 0, 0, null, 0);

            if (buf != null) {
                dataLen = authUpdate(buf, 0, bufLen);
            }
            len -= TAG_LENGTH;
            authFinalizeData(ct, 0, len, tag, 0);
            if ((((long) asLongView.get(ct, len) ^
                (long) asLongView.get(tag, 0)) |
                ((long) asLongView.get(ct, len + Long.BYTES) ^
                    (long) asLongView.get(tag, Long.BYTES))) != 0) {
                throw new AEADBadTagException("Tag mismatch");
            }

            if (buf != null) {
                chaCha20Transform(buf, 0, bufLen, buf, 0);
                output.put(buf, 0, bufLen);
            }
            chaCha20Transform(ct, 0, len, ct, 0);
            output.put(ct, 0, len);
            aadDone = false;
            return ctLen - TAG_LENGTH;
        }
    }

    public static final class ChaCha20Only extends ChaCha20Cipher {
        public ChaCha20Only() {
            mode = MODE_NONE;
        }
    }

    public static final class ChaCha20Poly1305 extends ChaCha20Cipher {
        public ChaCha20Poly1305() {
            mode = MODE_AEAD;
            authAlgName = "Poly1305";
        }
    }
}
