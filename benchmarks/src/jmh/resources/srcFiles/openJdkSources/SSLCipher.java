/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import sun.security.ssl.Authenticator.MAC;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static sun.security.ssl.CipherType.AEAD_CIPHER;
import static sun.security.ssl.CipherType.BLOCK_CIPHER;
import static sun.security.ssl.CipherType.NULL_CIPHER;
import static sun.security.ssl.CipherType.STREAM_CIPHER;
import static sun.security.ssl.JsseJce.CIPHER_3DES;
import static sun.security.ssl.JsseJce.CIPHER_AES;
import static sun.security.ssl.JsseJce.CIPHER_AES_GCM;
import static sun.security.ssl.JsseJce.CIPHER_CHACHA20_POLY1305;
import static sun.security.ssl.JsseJce.CIPHER_DES;
import static sun.security.ssl.JsseJce.CIPHER_RC4;

enum SSLCipher {
    @SuppressWarnings({"unchecked", "rawtypes"})
    B_NULL("NULL", NULL_CIPHER, 0, 0, 0, 0, true, true,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new NullReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_NONE
            ),
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new NullReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_13
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new NullWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_NONE
            ),
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new NullWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_13
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_RC4_40(CIPHER_RC4, STREAM_CIPHER, 5, 16, 0, 0, true, true,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new StreamReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new StreamWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_RC2_40("RC2", BLOCK_CIPHER, 5, 16, 8, 0, false, true,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new StreamReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new StreamWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_DES_40(CIPHER_DES,  BLOCK_CIPHER, 5, 8, 8, 0, true, true,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T10BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T10BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_RC4_128(CIPHER_RC4, STREAM_CIPHER, 16, 16, 0, 0, true, false,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new StreamReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_12
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new StreamWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_12
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_DES(CIPHER_DES, BLOCK_CIPHER, 8, 8, 8, 0, true, false,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T10BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T11BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_11
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T10BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T11BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_11
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_3DES(CIPHER_3DES, BLOCK_CIPHER, 24, 24, 8, 0, true, false,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T10BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T11BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_11_12
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T10BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T11BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_11_12
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_IDEA("IDEA", BLOCK_CIPHER, 16, 16, 8, 0, false, false,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                null,
                ProtocolVersion.PROTOCOLS_TO_12
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                null,
                ProtocolVersion.PROTOCOLS_TO_12
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_AES_128(CIPHER_AES, BLOCK_CIPHER, 16, 16, 16, 0, true, false,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T10BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T11BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_11_12
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T10BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T11BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_11_12
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_AES_256(CIPHER_AES, BLOCK_CIPHER, 32, 32, 16, 0, true, false,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T10BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T11BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_11_12
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T10BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T11BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_11_12
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_AES_128_GCM(CIPHER_AES_GCM, AEAD_CIPHER, 16, 16, 12, 4, true, false,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T12GcmReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_12
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T12GcmWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_12
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_AES_256_GCM(CIPHER_AES_GCM, AEAD_CIPHER, 32, 32, 12, 4, true, false,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T12GcmReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_12
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T12GcmWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_12
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_AES_128_GCM_IV(CIPHER_AES_GCM, AEAD_CIPHER, 16, 16, 12, 0, true, false,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T13GcmReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_13
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T13GcmWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_13
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_AES_256_GCM_IV(CIPHER_AES_GCM, AEAD_CIPHER, 32, 32, 12, 0, true, false,
        new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T13GcmReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_13
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T13GcmWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_13
            )
        }),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_CC20_P1305(CIPHER_CHACHA20_POLY1305, AEAD_CIPHER, 32, 32, 12,
        12, true, false, new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T12CC20P1305ReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_12
            ),
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T13CC20P1305ReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_13
            )
        }, new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T12CC20P1305WriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_12
            ),
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T13CC20P1305WriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_13
            )
        });

    final String description;

    final String transformation;

    final String algorithm;

    final boolean allowed;

    final int keySize;

    final int expandedKeySize;

    final int ivSize;

    final int fixedIvSize;

    final boolean exportable;

    final CipherType cipherType;

    final int tagSize = 16;

    private final boolean isAvailable;

    private final Map.Entry<ReadCipherGenerator,
            ProtocolVersion[]>[] readCipherGenerators;
    private final Map.Entry<WriteCipherGenerator,
            ProtocolVersion[]>[] writeCipherGenerators;

    private static final HashMap<String, Long> cipherLimits = new HashMap<>();

    static final String[] tag = {"KEYUPDATE"};

    static  {
        final long max = 4611686018427387904L; 
        @SuppressWarnings("removal")
        String prop = AccessController.doPrivileged(
                new PrivilegedAction<String>() {
            @Override
            public String run() {
                return Security.getProperty("jdk.tls.keyLimits");
            }
        });

        if (prop != null) {
            String[] propvalue = prop.split(",");

            for (String entry : propvalue) {
                int index;
                String[] values =
                        entry.trim().toUpperCase(Locale.ENGLISH).split(" ");

                if (values[1].contains(tag[0])) {
                    index = 0;
                } else {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                        SSLLogger.fine("jdk.tls.keyLimits:  Unknown action:  " +
                                entry);
                    }
                    continue;
                }

                long size;
                int i = values[2].indexOf("^");
                try {
                    if (i >= 0) {
                        size = (long) Math.pow(2,
                                Integer.parseInt(values[2].substring(i + 1)));
                    } else {
                        size = Long.parseLong(values[2]);
                    }
                    if (size < 1 || size > max) {
                        throw new NumberFormatException(
                            "Length exceeded limits");
                    }
                } catch (NumberFormatException e) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                        SSLLogger.fine("jdk.tls.keyLimits:  " + e.getMessage() +
                                ":  " +  entry);
                    }
                    continue;
                }
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("jdk.tls.keyLimits:  entry = " + entry +
                            ". " + values[0] + ":" + tag[index] + " = " + size);
                }
                cipherLimits.put(values[0] + ":" + tag[index], size);
            }
        }
    }

    SSLCipher(String transformation,
              CipherType cipherType, int keySize,
              int expandedKeySize, int ivSize,
              int fixedIvSize, boolean allowed, boolean exportable,
              Map.Entry<ReadCipherGenerator,
                      ProtocolVersion[]>[] readCipherGenerators,
              Map.Entry<WriteCipherGenerator,
                      ProtocolVersion[]>[] writeCipherGenerators) {
        this.transformation = transformation;
        String[] splits = transformation.split("/");
        this.algorithm = splits[0];
        this.cipherType = cipherType;
        this.description = this.algorithm + "/" + (keySize << 3);
        this.keySize = keySize;
        this.ivSize = ivSize;
        this.fixedIvSize = fixedIvSize;
        this.allowed = allowed;

        this.expandedKeySize = expandedKeySize;
        this.exportable = exportable;

        this.isAvailable = allowed && isUnlimited(keySize, transformation) &&
                isTransformationAvailable(transformation);

        this.readCipherGenerators = readCipherGenerators;
        this.writeCipherGenerators = writeCipherGenerators;
    }

    private static boolean isTransformationAvailable(String transformation) {
        if (transformation.equals("NULL")) {
            return true;
        }
        try {
            Cipher.getInstance(transformation);
            return true;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.fine("Transformation " + transformation + " is" +
                        " not available.");
            }
        }
        return false;
    }

    SSLReadCipher createReadCipher(Authenticator authenticator,
            ProtocolVersion protocolVersion,
            SecretKey key, IvParameterSpec iv,
            SecureRandom random) throws GeneralSecurityException {
        if (readCipherGenerators.length == 0) {
            return null;
        }

        ReadCipherGenerator rcg = null;
        for (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]> me : readCipherGenerators) {
            for (ProtocolVersion pv : me.getValue()) {
                if (protocolVersion == pv) {
                    rcg = me.getKey();
                    break;
                }
            }
        }

        if (rcg != null) {
            return rcg.createCipher(this, authenticator,
                    protocolVersion, transformation, key, iv, random);
        }
        return null;
    }

    SSLWriteCipher createWriteCipher(Authenticator authenticator,
            ProtocolVersion protocolVersion,
            SecretKey key, IvParameterSpec iv,
            SecureRandom random) throws GeneralSecurityException {
        if (writeCipherGenerators.length == 0) {
            return null;
        }

        WriteCipherGenerator wcg = null;
        for (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]> me : writeCipherGenerators) {
            for (ProtocolVersion pv : me.getValue()) {
                if (protocolVersion == pv) {
                    wcg = me.getKey();
                    break;
                }
            }
        }

        if (wcg != null) {
            return wcg.createCipher(this, authenticator,
                    protocolVersion, transformation, key, iv, random);
        }
        return null;
    }

    /**
     * Test if this bulk cipher is available. For use by CipherSuite.
     */
    boolean isAvailable() {
        return this.isAvailable;
    }

    private static boolean isUnlimited(int keySize, String transformation) {
        int keySizeInBits = keySize * 8;
        if (keySizeInBits > 128) {    
            try {
                if (Cipher.getMaxAllowedKeyLength(
                        transformation) < keySizeInBits) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return description;
    }

    interface ReadCipherGenerator {
        SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException;
    }

    abstract static class SSLReadCipher {
        final Authenticator authenticator;
        final ProtocolVersion protocolVersion;
        boolean keyLimitEnabled = false;
        long keyLimitCountdown = 0;
        SecretKey baseSecret;

        SSLReadCipher(Authenticator authenticator,
                ProtocolVersion protocolVersion) {
            this.authenticator = authenticator;
            this.protocolVersion = protocolVersion;
        }

        static final SSLReadCipher nullTlsReadCipher() {
            try {
                return B_NULL.createReadCipher(
                        Authenticator.nullTlsMac(),
                        ProtocolVersion.NONE, null, null, null);
            } catch (GeneralSecurityException gse) {
                throw new RuntimeException("Cannot create NULL SSLCipher", gse);
            }
        }

        static final SSLReadCipher nullDTlsReadCipher() {
            try {
                return B_NULL.createReadCipher(
                        Authenticator.nullDtlsMac(),
                        ProtocolVersion.NONE, null, null, null);
            } catch (GeneralSecurityException gse) {
                throw new RuntimeException("Cannot create NULL SSLCipher", gse);
            }
        }

        abstract Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException;

        void dispose() {
        }

        abstract int estimateFragmentSize(int packetSize, int headerSize);

        boolean isNullCipher() {
            return false;
        }

        /**
         * Check if processed bytes have reached the key usage limit.
         * If key usage limit is not be monitored, return false.
         */
        public boolean atKeyLimit() {
            if (keyLimitCountdown >= 0) {
                return false;
            }

            keyLimitEnabled = false;
            return true;
        }
    }

    interface WriteCipherGenerator {
        SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException;
    }

    abstract static class SSLWriteCipher {
        final Authenticator authenticator;
        final ProtocolVersion protocolVersion;
        boolean keyLimitEnabled = false;
        long keyLimitCountdown = 0;
        SecretKey baseSecret;

        SSLWriteCipher(Authenticator authenticator,
                ProtocolVersion protocolVersion) {
            this.authenticator = authenticator;
            this.protocolVersion = protocolVersion;
        }

        abstract int encrypt(byte contentType, ByteBuffer bb);

        static final SSLWriteCipher nullTlsWriteCipher() {
            try {
                return B_NULL.createWriteCipher(
                        Authenticator.nullTlsMac(),
                        ProtocolVersion.NONE, null, null, null);
            } catch (GeneralSecurityException gse) {
                throw new RuntimeException(
                        "Cannot create NULL SSL write Cipher", gse);
            }
        }

        static final SSLWriteCipher nullDTlsWriteCipher() {
            try {
                return B_NULL.createWriteCipher(
                        Authenticator.nullDtlsMac(),
                        ProtocolVersion.NONE, null, null, null);
            } catch (GeneralSecurityException gse) {
                throw new RuntimeException(
                        "Cannot create NULL SSL write Cipher", gse);
            }
        }

        void dispose() {
        }

        abstract int getExplicitNonceSize();
        abstract int calculateFragmentSize(int packetLimit, int headerSize);
        abstract int calculatePacketSize(int fragmentSize, int headerSize);

        boolean isCBCMode() {
            return false;
        }

        boolean isNullCipher() {
            return false;
        }

        /**
         * Check if processed bytes have reached the key usage limit.
         * If key usage limit is not be monitored, return false.
         */
        public boolean atKeyLimit() {
            if (keyLimitCountdown >= 0) {
                return false;
            }

            keyLimitEnabled = false;
            return true;
        }
    }

    private static final
            class NullReadCipherGenerator implements ReadCipherGenerator {
        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new NullReadCipher(authenticator, protocolVersion);
        }

        static final class NullReadCipher extends SSLReadCipher {
            NullReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion) {
                super(authenticator, protocolVersion);
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                MAC signer = (MAC)authenticator;
                if (signer.macAlg().size != 0) {
                    checkStreamMac(signer, bb, contentType, sequence);
                } else {
                    authenticator.increaseSequenceNumber();
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                return packetSize - headerSize - macLen;
            }

            @Override
            boolean isNullCipher() {
                return true;
            }
        }
    }

    private static final
            class NullWriteCipherGenerator implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new NullWriteCipher(authenticator, protocolVersion);
        }

        static final class NullWriteCipher extends SSLWriteCipher {
            NullWriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion) {
                super(authenticator, protocolVersion);
            }

            @Override
            public int encrypt(byte contentType, ByteBuffer bb) {
                MAC signer = (MAC)authenticator;
                if (signer.macAlg().size != 0) {
                    addMac(signer, bb, contentType);
                } else {
                    authenticator.increaseSequenceNumber();
                }

                int len = bb.remaining();
                bb.position(bb.limit());
                return len;
            }


            @Override
            int getExplicitNonceSize() {
                return 0;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                return packetLimit - headerSize - macLen;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                return fragmentSize + headerSize + macLen;
            }

            @Override
            boolean isNullCipher() {
                return true;
            }
        }
    }

    private static final
            class StreamReadCipherGenerator implements ReadCipherGenerator {
        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new StreamReadCipher(authenticator, protocolVersion,
                    algorithm, key, params, random);
        }

        static final class StreamReadCipher extends SSLReadCipher {
            private final Cipher cipher;

            StreamReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.DECRYPT_MODE, key, params, random);
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                int len = bb.remaining();
                int pos;
                ByteBuffer pt;

                if (!bb.isReadOnly()) {
                    pt = bb.duplicate();
                    pos = bb.position();
                } else {
                    pt = ByteBuffer.allocate(bb.remaining());
                    pos = 0;
                }

                try {
                    if (len != cipher.update(bb, pt)) {
                        throw new RuntimeException(
                                "Unexpected number of plaintext bytes");
                    }
                } catch (ShortBufferException sbe) {
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }
                pt.position(pos);
                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext after DECRYPTION", pt.duplicate());
                }

                MAC signer = (MAC)authenticator;
                if (signer.macAlg().size != 0) {
                    checkStreamMac(signer, pt, contentType, sequence);
                } else {
                    authenticator.increaseSequenceNumber();
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, pt.slice());
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                return packetSize - headerSize - macLen;
            }
        }
    }

    private static final
            class StreamWriteCipherGenerator implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new StreamWriteCipher(authenticator,
                    protocolVersion, algorithm, key, params, random);
        }

        static final class StreamWriteCipher extends SSLWriteCipher {
            private final Cipher cipher;

            StreamWriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, key, params, random);
            }

            @Override
            public int encrypt(byte contentType, ByteBuffer bb) {
                MAC signer = (MAC)authenticator;
                if (signer.macAlg().size != 0) {
                    addMac(signer, bb, contentType);
                } else {
                    authenticator.increaseSequenceNumber();
                }

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.finest(
                        "Padded plaintext before ENCRYPTION", bb.duplicate());
                }

                int len = bb.remaining();
                ByteBuffer dup = bb.duplicate();
                try {
                    if (len != cipher.update(dup, bb)) {
                        throw new RuntimeException(
                                "Unexpected number of plaintext bytes");
                    }
                    if (bb.position() != dup.position()) {
                        throw new RuntimeException(
                                "Unexpected ByteBuffer position");
                    }
                } catch (ShortBufferException sbe) {
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }

                return len;
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            int getExplicitNonceSize() {
                return 0;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                return packetLimit - headerSize - macLen;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                return fragmentSize + headerSize + macLen;
            }
        }
    }

    private static final
            class T10BlockReadCipherGenerator implements ReadCipherGenerator {
        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new BlockReadCipher(authenticator,
                    protocolVersion, algorithm, key, params, random);
        }

        static final class BlockReadCipher extends SSLReadCipher {
            private final Cipher cipher;

            BlockReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.DECRYPT_MODE, key, params, random);
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                BadPaddingException reservedBPE = null;

                MAC signer = (MAC)authenticator;
                int cipheredLength = bb.remaining();
                int tagLen = signer.macAlg().size;
                if (tagLen != 0) {
                    if (!sanityCheck(tagLen, cipheredLength)) {
                        reservedBPE = new BadPaddingException(
                                "ciphertext sanity check failed");
                    }
                }
                ByteBuffer pt;
                int pos;

                if (!bb.isReadOnly()) {
                    pt = bb.duplicate();
                    pos = bb.position();
                } else {
                    pt = ByteBuffer.allocate(cipheredLength);
                    pos = 0;
                }

                try {
                    if (cipheredLength != cipher.update(bb, pt)) {
                        throw new RuntimeException(
                                "Unexpected number of plaintext bytes");
                    }
                } catch (ShortBufferException sbe) {
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Padded plaintext after DECRYPTION",
                            pt.duplicate().position(pos));
                }

                pt.position(pos);
                try {
                    removePadding(pt, tagLen, cipher.getBlockSize(),
                        protocolVersion);
                } catch (BadPaddingException bpe) {
                    if (reservedBPE == null) {
                        reservedBPE = bpe;
                    }
                }

                try {
                    if (tagLen != 0) {
                        checkCBCMac(signer, pt,
                                contentType, cipheredLength, sequence);
                    } else {
                        authenticator.increaseSequenceNumber();
                    }
                } catch (BadPaddingException bpe) {
                    if (reservedBPE == null) {
                        reservedBPE = bpe;
                    }
                }

                if (reservedBPE != null) {
                    throw reservedBPE;
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, pt.slice());
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;

                return packetSize - headerSize - macLen - 1;
            }

            /**
             * Sanity check the length of a fragment before decryption.
             *
             * In CBC mode, check that the fragment length is one or multiple
             * times of the block size of the cipher suite, and is at least
             * one (one is the smallest size of padding in CBC mode) bigger
             * than the tag size of the MAC algorithm except the explicit IV
             * size for TLS 1.1 or later.
             *
             * In non-CBC mode, check that the fragment length is not less than
             * the tag size of the MAC algorithm.
             *
             * @return true if the length of a fragment matches above
             *         requirements
             */
            private boolean sanityCheck(int tagLen, int fragmentLen) {
                int blockSize = cipher.getBlockSize();
                if ((fragmentLen % blockSize) == 0) {
                    int minimal = tagLen + 1;
                    minimal = Math.max(minimal, blockSize);

                    return (fragmentLen >= minimal);
                }

                return false;
            }
        }
    }

    private static final
            class T10BlockWriteCipherGenerator implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new BlockWriteCipher(authenticator,
                    protocolVersion, algorithm, key, params, random);
        }

        static final class BlockWriteCipher extends SSLWriteCipher {
            private final Cipher cipher;

            BlockWriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, key, params, random);
            }

            @Override
            public int encrypt(byte contentType, ByteBuffer bb) {
                int pos = bb.position();

                MAC signer = (MAC)authenticator;
                if (signer.macAlg().size != 0) {
                    addMac(signer, bb, contentType);
                } else {
                    authenticator.increaseSequenceNumber();
                }

                int blockSize = cipher.getBlockSize();
                int len = addPadding(bb, blockSize);
                bb.position(pos);

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Padded plaintext before ENCRYPTION",
                            bb.duplicate());
                }

                ByteBuffer dup = bb.duplicate();
                try {
                    if (len != cipher.update(dup, bb)) {
                        throw new RuntimeException(
                                "Unexpected number of plaintext bytes");
                    }

                    if (bb.position() != dup.position()) {
                        throw new RuntimeException(
                                "Unexpected ByteBuffer position");
                    }
                } catch (ShortBufferException sbe) {
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }

                return len;
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            int getExplicitNonceSize() {
                return 0;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                int blockSize = cipher.getBlockSize();
                int fragLen = packetLimit - headerSize;
                fragLen -= (fragLen % blockSize);   
                fragLen -= 1;       
                fragLen -= macLen;
                return fragLen;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                int blockSize = cipher.getBlockSize();
                int paddedLen = fragmentSize + macLen + 1;
                if ((paddedLen % blockSize)  != 0) {
                    paddedLen += blockSize - 1;
                    paddedLen -= paddedLen % blockSize;
                }

                return headerSize + paddedLen;
            }

            @Override
            boolean isCBCMode() {
                return true;
            }
        }
    }

    private static final
            class T11BlockReadCipherGenerator implements ReadCipherGenerator {
        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new BlockReadCipher(authenticator, protocolVersion,
                    sslCipher, algorithm, key, params, random);
        }

        static final class BlockReadCipher extends SSLReadCipher {
            private final Cipher cipher;

            BlockReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                if (params == null) {
                    params = new IvParameterSpec(new byte[sslCipher.ivSize]);
                }
                cipher.init(Cipher.DECRYPT_MODE, key, params, random);
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                BadPaddingException reservedBPE = null;

                MAC signer = (MAC)authenticator;
                int cipheredLength = bb.remaining();
                int tagLen = signer.macAlg().size;
                if (tagLen != 0) {
                    if (!sanityCheck(tagLen, cipheredLength)) {
                        reservedBPE = new BadPaddingException(
                                "ciphertext sanity check failed");
                    }
                }

                ByteBuffer pt;
                int pos;

                if (!bb.isReadOnly()) {
                    pt = bb.duplicate();
                    pos = bb.position();
                } else {
                    pt = ByteBuffer.allocate(cipheredLength);
                    pos = 0;
                }

                try {
                    if (cipheredLength != cipher.update(bb, pt)) {
                        throw new RuntimeException(
                                "Unexpected number of plaintext bytes");
                    }
                } catch (ShortBufferException sbe) {
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine("Padded plaintext after DECRYPTION",
                        pt.duplicate().position(pos));
                }

                int blockSize = cipher.getBlockSize();
                pos += blockSize;
                pt.position(pos);

                try {
                    removePadding(pt, tagLen, blockSize, protocolVersion);
                } catch (BadPaddingException bpe) {
                    if (reservedBPE == null) {
                        reservedBPE = bpe;
                    }
                }

                try {
                    if (tagLen != 0) {
                        checkCBCMac(signer, pt,
                                contentType, cipheredLength, sequence);
                    } else {
                        authenticator.increaseSequenceNumber();
                    }
                } catch (BadPaddingException bpe) {
                    if (reservedBPE == null) {
                        reservedBPE = bpe;
                    }
                }

                if (reservedBPE != null) {
                    throw reservedBPE;
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, pt.slice());
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;

                int nonceSize = cipher.getBlockSize();
                return packetSize - headerSize - nonceSize - macLen - 1;
            }

            /**
             * Sanity check the length of a fragment before decryption.
             *
             * In CBC mode, check that the fragment length is one or multiple
             * times of the block size of the cipher suite, and is at least
             * one (one is the smallest size of padding in CBC mode) bigger
             * than the tag size of the MAC algorithm except the explicit IV
             * size for TLS 1.1 or later.
             *
             * In non-CBC mode, check that the fragment length is not less than
             * the tag size of the MAC algorithm.
             *
             * @return true if the length of a fragment matches above
             *         requirements
             */
            private boolean sanityCheck(int tagLen, int fragmentLen) {
                int blockSize = cipher.getBlockSize();
                if ((fragmentLen % blockSize) == 0) {
                    int minimal = tagLen + 1;
                    minimal = Math.max(minimal, blockSize);
                    minimal += blockSize;

                    return (fragmentLen >= minimal);
                }

                return false;
            }
        }
    }

    private static final
            class T11BlockWriteCipherGenerator implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new BlockWriteCipher(authenticator, protocolVersion,
                    sslCipher, algorithm, key, params, random);
        }

        static final class BlockWriteCipher extends SSLWriteCipher {
            private final Cipher cipher;
            private final SecureRandom random;

            BlockWriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.random = random;
                if (params == null) {
                    params = new IvParameterSpec(new byte[sslCipher.ivSize]);
                }
                cipher.init(Cipher.ENCRYPT_MODE, key, params, random);
            }

            @Override
            public int encrypt(byte contentType, ByteBuffer bb) {
                int pos = bb.position();

                MAC signer = (MAC)authenticator;
                if (signer.macAlg().size != 0) {
                    addMac(signer, bb, contentType);
                } else {
                    authenticator.increaseSequenceNumber();
                }

                byte[] nonce = new byte[cipher.getBlockSize()];
                random.nextBytes(nonce);
                pos = pos - nonce.length;
                bb.position(pos);
                bb.put(nonce);
                bb.position(pos);

                int blockSize = cipher.getBlockSize();
                int len = addPadding(bb, blockSize);
                bb.position(pos);

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Padded plaintext before ENCRYPTION",
                            bb.duplicate());
                }

                ByteBuffer dup = bb.duplicate();
                try {
                    if (len != cipher.update(dup, bb)) {
                        throw new RuntimeException(
                                "Unexpected number of plaintext bytes");
                    }

                    if (bb.position() != dup.position()) {
                        throw new RuntimeException(
                                "Unexpected ByteBuffer position");
                    }
                } catch (ShortBufferException sbe) {
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }

                return len;
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            int getExplicitNonceSize() {
                return cipher.getBlockSize();
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                int blockSize = cipher.getBlockSize();
                int fragLen = packetLimit - headerSize - blockSize;
                fragLen -= (fragLen % blockSize);   
                fragLen -= 1;       
                fragLen -= macLen;
                return fragLen;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                int blockSize = cipher.getBlockSize();
                int paddedLen = fragmentSize + macLen + 1;
                if ((paddedLen % blockSize)  != 0) {
                    paddedLen += blockSize - 1;
                    paddedLen -= paddedLen % blockSize;
                }

                return headerSize + blockSize + paddedLen;
            }

            @Override
            boolean isCBCMode() {
                return true;
            }
        }
    }

    private static final
            class T12GcmReadCipherGenerator implements ReadCipherGenerator {
        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new GcmReadCipher(authenticator, protocolVersion, sslCipher,
                    algorithm, key, params, random);
        }

        static final class GcmReadCipher extends SSLReadCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] fixedIv;
            private final int recordIvSize;
            private final SecureRandom random;

            GcmReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.fixedIv = ((IvParameterSpec)params).getIV();
                this.recordIvSize = sslCipher.ivSize - sslCipher.fixedIvSize;
                this.random = random;

            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                if (bb.remaining() < (recordIvSize + tagSize)) {
                    throw new BadPaddingException(
                        "Insufficient buffer remaining for AEAD cipher " +
                        "fragment (" + bb.remaining() + "). Needs to be " +
                        "more than or equal to IV size (" + recordIvSize +
                         ") + tag size (" + tagSize + ")");
                }

                byte[] iv = Arrays.copyOf(fixedIv,
                                    fixedIv.length + recordIvSize);
                bb.get(iv, fixedIv.length, recordIvSize);
                GCMParameterSpec spec = new GCMParameterSpec(tagSize * 8, iv);
                try {
                    cipher.init(Cipher.DECRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    throw new RuntimeException(
                                "invalid key or spec in GCM mode", ikae);
                }

                byte[] aad = authenticator.acquireAuthenticationBytes(
                        contentType, bb.remaining() - tagSize,
                        sequence);
                cipher.updateAAD(aad);

                ByteBuffer pt;
                int len, pos;

                if (!bb.isReadOnly()) {
                    pt = bb.duplicate();
                    pos = bb.position();
                } else {
                    pt = ByteBuffer.allocate(bb.remaining());
                    pos = 0;
                }

                try {
                    len = cipher.doFinal(bb, pt);
                } catch (IllegalBlockSizeException ibse) {
                    throw new RuntimeException(
                        "Cipher error in AEAD mode \"" + ibse.getMessage() +
                        " \"in JCE provider " + cipher.getProvider().getName());
                } catch (ShortBufferException sbe) {
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }
                pt.position(pos);
                pt.limit(pos + len);

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext after DECRYPTION", pt.duplicate());
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, pt.slice());
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                return packetSize - headerSize - recordIvSize - tagSize;
            }
        }
    }

    private static final
            class T12GcmWriteCipherGenerator implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new GcmWriteCipher(authenticator, protocolVersion, sslCipher,
                    algorithm, key, params, random);
        }

        private static final class GcmWriteCipher extends SSLWriteCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] fixedIv;
            private final int recordIvSize;
            private final SecureRandom random;

            GcmWriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.fixedIv = ((IvParameterSpec)params).getIV();
                this.recordIvSize = sslCipher.ivSize - sslCipher.fixedIvSize;
                this.random = random;

            }

            @Override
            public int encrypt(byte contentType,
                    ByteBuffer bb) {
                byte[] nonce = authenticator.sequenceNumber();

                byte[] iv = Arrays.copyOf(fixedIv,
                                            fixedIv.length + nonce.length);
                System.arraycopy(nonce, 0, iv, fixedIv.length, nonce.length);

                GCMParameterSpec spec = new GCMParameterSpec(tagSize * 8, iv);
                try {
                    cipher.init(Cipher.ENCRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    throw new RuntimeException(
                                "invalid key or spec in GCM mode", ikae);
                }

                byte[] aad = authenticator.acquireAuthenticationBytes(
                                        contentType, bb.remaining(), null);
                cipher.updateAAD(aad);

                bb.position(bb.position() - nonce.length);
                bb.put(nonce);

                int len, pos = bb.position();
                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext before ENCRYPTION",
                            bb.duplicate());
                }

                ByteBuffer dup = bb.duplicate();
                int outputSize = cipher.getOutputSize(dup.remaining());
                if (outputSize > bb.remaining()) {
                    bb.limit(pos + outputSize);
                }

                try {
                    len = cipher.doFinal(dup, bb);
                } catch (IllegalBlockSizeException |
                            BadPaddingException | ShortBufferException ibse) {
                    throw new RuntimeException(
                            "Cipher error in AEAD mode in JCE provider " +
                            cipher.getProvider().getName(), ibse);
                }

                if (len != outputSize) {
                    throw new RuntimeException(
                            "Cipher buffering error in JCE provider " +
                            cipher.getProvider().getName());
                }

                return len + nonce.length;
            }

            @Override
            int getExplicitNonceSize() {
                return recordIvSize;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                return packetLimit - headerSize - recordIvSize - tagSize;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                return fragmentSize + headerSize + recordIvSize + tagSize;
            }
        }
    }

    private static final
            class T13GcmReadCipherGenerator implements ReadCipherGenerator {

        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new GcmReadCipher(authenticator, protocolVersion, sslCipher,
                    algorithm, key, params, random);
        }

        static final class GcmReadCipher extends SSLReadCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] iv;
            private final SecureRandom random;

            GcmReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.iv = ((IvParameterSpec)params).getIV();
                this.random = random;

                keyLimitCountdown = cipherLimits.getOrDefault(
                    algorithm.toUpperCase(Locale.ENGLISH) + ":" + tag[0], 0L);
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("KeyLimit read side: algorithm = " +
                            algorithm + ":" + tag[0] +
                            "\ncountdown value = " + keyLimitCountdown);
                }
                if (keyLimitCountdown > 0) {
                    keyLimitEnabled = true;
                }
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                if (contentType == ContentType.CHANGE_CIPHER_SPEC.id) {
                    return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
                }

                if (bb.remaining() <= tagSize) {
                    throw new BadPaddingException(
                        "Insufficient buffer remaining for AEAD cipher " +
                        "fragment (" + bb.remaining() + "). Needs to be " +
                        "more than tag size (" + tagSize + ")");
                }

                byte[] sn = sequence;
                if (sn == null) {
                    sn = authenticator.sequenceNumber();
                }
                byte[] nonce = iv.clone();
                int offset = nonce.length - sn.length;
                for (int i = 0; i < sn.length; i++) {
                    nonce[offset + i] ^= sn[i];
                }

                GCMParameterSpec spec =
                        new GCMParameterSpec(tagSize * 8, nonce);
                try {
                    cipher.init(Cipher.DECRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    throw new RuntimeException(
                                "invalid key or spec in GCM mode", ikae);
                }
                byte[] aad = authenticator.acquireAuthenticationBytes(
                                        contentType, bb.remaining(), sn);
                cipher.updateAAD(aad);

                ByteBuffer pt;
                int len, pos;

                if (!bb.isReadOnly()) {
                    pt = bb.duplicate();
                    pos = bb.position();
                } else {
                    pt = ByteBuffer.allocate(bb.remaining());
                    pos = 0;
                }

                try {
                    len = cipher.doFinal(bb, pt);
                } catch (IllegalBlockSizeException ibse) {
                    throw new RuntimeException(
                        "Cipher error in AEAD mode \"" + ibse.getMessage() +
                        " \"in JCE provider " + cipher.getProvider().getName());
                } catch (ShortBufferException sbe) {
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }
                pt.position(pos);
                pt.limit(pos + len);

                int i = pt.limit() - 1;
                for (; i > 0 && pt.get(i) == 0; i--);

                if (i < (pos + 1)) {
                    throw new BadPaddingException(
                            "Incorrect inner plaintext: no content type");
                }
                contentType = pt.get(i);
                pt.limit(i);

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext after DECRYPTION", pt.duplicate());
                }
                if (keyLimitEnabled) {
                    keyLimitCountdown -= len;
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, pt.slice());
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                return packetSize - headerSize - tagSize;
            }
        }
    }

    private static final
            class T13GcmWriteCipherGenerator implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new GcmWriteCipher(authenticator, protocolVersion, sslCipher,
                    algorithm, key, params, random);
        }

        private static final class GcmWriteCipher extends SSLWriteCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] iv;
            private final SecureRandom random;

            GcmWriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.iv = ((IvParameterSpec)params).getIV();
                this.random = random;

                keyLimitCountdown = cipherLimits.getOrDefault(
                    algorithm.toUpperCase(Locale.ENGLISH) + ":" + tag[0], 0L);
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("KeyLimit write side: algorithm = "
                            + algorithm + ":" + tag[0] +
                            "\ncountdown value = " + keyLimitCountdown);
                }
                if (keyLimitCountdown > 0) {
                    keyLimitEnabled = true;
                }

            }

            @Override
            public int encrypt(byte contentType,
                    ByteBuffer bb) {
                byte[] sn = authenticator.sequenceNumber();
                byte[] nonce = iv.clone();
                int offset = nonce.length - sn.length;
                for (int i = 0; i < sn.length; i++) {
                    nonce[offset + i] ^= sn[i];
                }

                GCMParameterSpec spec =
                        new GCMParameterSpec(tagSize * 8, nonce);
                try {
                    cipher.init(Cipher.ENCRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    throw new RuntimeException(
                                "invalid key or spec in GCM mode", ikae);
                }

                int outputSize = cipher.getOutputSize(bb.remaining());
                byte[] aad = authenticator.acquireAuthenticationBytes(
                                        contentType, outputSize, sn);
                cipher.updateAAD(aad);

                int len, pos = bb.position();
                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext before ENCRYPTION",
                            bb.duplicate());
                }

                ByteBuffer dup = bb.duplicate();
                if (outputSize > bb.remaining()) {
                    bb.limit(pos + outputSize);
                }

                try {
                    len = cipher.doFinal(dup, bb);
                } catch (IllegalBlockSizeException |
                            BadPaddingException | ShortBufferException ibse) {
                    throw new RuntimeException(
                            "Cipher error in AEAD mode in JCE provider " +
                            cipher.getProvider().getName(), ibse);
                }

                if (len != outputSize) {
                    throw new RuntimeException(
                            "Cipher buffering error in JCE provider " +
                            cipher.getProvider().getName());
                }

                if (keyLimitEnabled) {
                    keyLimitCountdown -= len;
                }
                return len;
            }

            @Override
            int getExplicitNonceSize() {
                return 0;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                return packetLimit - headerSize - tagSize;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                return fragmentSize + headerSize + tagSize;
            }
        }
    }

    private static final class T12CC20P1305ReadCipherGenerator
            implements ReadCipherGenerator {

        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new CC20P1305ReadCipher(authenticator, protocolVersion,
                    sslCipher, algorithm, key, params, random);
        }

        static final class CC20P1305ReadCipher extends SSLReadCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] iv;
            private final SecureRandom random;

            CC20P1305ReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.iv = ((IvParameterSpec)params).getIV();
                this.random = random;

            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                if (bb.remaining() <= tagSize) {
                    throw new BadPaddingException(
                        "Insufficient buffer remaining for AEAD cipher " +
                        "fragment (" + bb.remaining() + "). Needs to be " +
                        "more than tag size (" + tagSize + ")");
                }

                byte[] sn = sequence;
                if (sn == null) {
                    sn = authenticator.sequenceNumber();
                }
                byte[] nonce = new byte[iv.length];
                System.arraycopy(sn, 0, nonce, nonce.length - sn.length,
                        sn.length);
                for (int i = 0; i < nonce.length; i++) {
                    nonce[i] ^= iv[i];
                }

                AlgorithmParameterSpec spec = new IvParameterSpec(nonce);
                try {
                    cipher.init(Cipher.DECRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    throw new RuntimeException(
                                "invalid key or spec in AEAD mode", ikae);
                }

                byte[] aad = authenticator.acquireAuthenticationBytes(
                        contentType, bb.remaining() - tagSize, sequence);
                cipher.updateAAD(aad);

                ByteBuffer pt;
                int len, pos;

                if (!bb.isReadOnly()) {
                    pt = bb.duplicate();
                    pos = bb.position();
                } else {
                    pt = ByteBuffer.allocate(bb.remaining());
                    pos = 0;
                }

                try {
                    len = cipher.doFinal(bb, pt);
                } catch (IllegalBlockSizeException ibse) {
                    throw new RuntimeException(
                        "Cipher error in AEAD mode \"" + ibse.getMessage() +
                        " \"in JCE provider " + cipher.getProvider().getName());
                } catch (ShortBufferException sbe) {
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }
                pt.position(pos);
                pt.limit(pos + len);

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext after DECRYPTION", pt.duplicate());
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, pt.slice());
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                return packetSize - headerSize - tagSize;
            }
        }
    }

    private static final class T12CC20P1305WriteCipherGenerator
            implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new CC20P1305WriteCipher(authenticator, protocolVersion,
                    sslCipher, algorithm, key, params, random);
        }

        private static final class CC20P1305WriteCipher extends SSLWriteCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] iv;
            private final SecureRandom random;

            CC20P1305WriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.iv = ((IvParameterSpec)params).getIV();
                this.random = random;

                keyLimitCountdown = cipherLimits.getOrDefault(
                    algorithm.toUpperCase(Locale.ENGLISH) + ":" + tag[0], 0L);
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("algorithm = " + algorithm +
                            ":" + tag[0] + "\ncountdown value = " +
                            keyLimitCountdown);
                }
                if (keyLimitCountdown > 0) {
                    keyLimitEnabled = true;
                }

            }

            @Override
            public int encrypt(byte contentType,
                    ByteBuffer bb) {
                byte[] sn = authenticator.sequenceNumber();
                byte[] nonce = new byte[iv.length];
                System.arraycopy(sn, 0, nonce, nonce.length - sn.length,
                        sn.length);
                for (int i = 0; i < nonce.length; i++) {
                    nonce[i] ^= iv[i];
                }

                AlgorithmParameterSpec spec = new IvParameterSpec(nonce);
                try {
                    cipher.init(Cipher.ENCRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    throw new RuntimeException(
                                "invalid key or spec in AEAD mode", ikae);
                }

                byte[] aad = authenticator.acquireAuthenticationBytes(
                                        contentType, bb.remaining(), null);
                cipher.updateAAD(aad);

                int pos = bb.position();
                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext before ENCRYPTION",
                            bb.duplicate());
                }

                ByteBuffer dup = bb.duplicate();
                int outputSize = cipher.getOutputSize(dup.remaining());
                if (outputSize > bb.remaining()) {
                    bb.limit(pos + outputSize);
                }

                int len;
                try {
                    len = cipher.doFinal(dup, bb);
                } catch (IllegalBlockSizeException |
                            BadPaddingException | ShortBufferException ibse) {
                    throw new RuntimeException(
                            "Cipher error in AEAD mode in JCE provider " +
                            cipher.getProvider().getName(), ibse);
                }

                if (len != outputSize) {
                    throw new RuntimeException(
                            "Cipher buffering error in JCE provider " +
                            cipher.getProvider().getName());
                }

                return len;
            }

            @Override
            int getExplicitNonceSize() {
                return 0;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                return packetLimit - headerSize - tagSize;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                return fragmentSize + headerSize + tagSize;
            }
        }
    }

    private static final class T13CC20P1305ReadCipherGenerator
            implements ReadCipherGenerator {

        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new CC20P1305ReadCipher(authenticator, protocolVersion,
                    sslCipher, algorithm, key, params, random);
        }

        static final class CC20P1305ReadCipher extends SSLReadCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] iv;
            private final SecureRandom random;

            CC20P1305ReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.iv = ((IvParameterSpec)params).getIV();
                this.random = random;

            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                if (contentType == ContentType.CHANGE_CIPHER_SPEC.id) {
                    return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
                }

                if (bb.remaining() <= tagSize) {
                    throw new BadPaddingException(
                        "Insufficient buffer remaining for AEAD cipher " +
                        "fragment (" + bb.remaining() + "). Needs to be " +
                        "more than tag size (" + tagSize + ")");
                }

                byte[] sn = sequence;
                if (sn == null) {
                    sn = authenticator.sequenceNumber();
                }
                byte[] nonce = new byte[iv.length];
                System.arraycopy(sn, 0, nonce, nonce.length - sn.length,
                        sn.length);
                for (int i = 0; i < nonce.length; i++) {
                    nonce[i] ^= iv[i];
                }

                AlgorithmParameterSpec spec = new IvParameterSpec(nonce);
                try {
                    cipher.init(Cipher.DECRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    throw new RuntimeException(
                                "invalid key or spec in AEAD mode", ikae);
                }

                byte[] aad = authenticator.acquireAuthenticationBytes(
                                        contentType, bb.remaining(), sn);
                cipher.updateAAD(aad);

                ByteBuffer pt;
                int len, pos;

                if (!bb.isReadOnly()) {
                    pt = bb.duplicate();
                    pos = bb.position();
                } else {
                    pt = ByteBuffer.allocate(bb.remaining());
                    pos = 0;
                }

                try {
                    len = cipher.doFinal(bb, pt);
                } catch (IllegalBlockSizeException ibse) {
                    throw new RuntimeException(
                        "Cipher error in AEAD mode \"" + ibse.getMessage() +
                        " \"in JCE provider " + cipher.getProvider().getName());
                } catch (ShortBufferException sbe) {
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }
                pt.position(pos);
                pt.limit(pos + len);

                int i = pt.limit() - 1;
                for (; i > 0 && pt.get(i) == 0; i--) {
                }
                if (i < (pos + 1)) {
                    throw new BadPaddingException(
                            "Incorrect inner plaintext: no content type");
                }
                contentType = pt.get(i);
                pt.limit(i);

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext after DECRYPTION", pt.duplicate());
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, pt.slice());
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                return packetSize - headerSize - tagSize;
            }
        }
    }

    private static final class T13CC20P1305WriteCipherGenerator
            implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new CC20P1305WriteCipher(authenticator, protocolVersion,
                    sslCipher, algorithm, key, params, random);
        }

        private static final class CC20P1305WriteCipher extends SSLWriteCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] iv;
            private final SecureRandom random;

            CC20P1305WriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.iv = ((IvParameterSpec)params).getIV();
                this.random = random;

                keyLimitCountdown = cipherLimits.getOrDefault(
                    algorithm.toUpperCase(Locale.ENGLISH) + ":" + tag[0], 0L);
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("algorithm = " + algorithm +
                            ":" + tag[0] + "\ncountdown value = " +
                            keyLimitCountdown);
                }
                if (keyLimitCountdown > 0) {
                    keyLimitEnabled = true;
                }

            }

            @Override
            public int encrypt(byte contentType,
                    ByteBuffer bb) {
                byte[] sn = authenticator.sequenceNumber();
                byte[] nonce = new byte[iv.length];
                System.arraycopy(sn, 0, nonce, nonce.length - sn.length,
                        sn.length);
                for (int i = 0; i < nonce.length; i++) {
                    nonce[i] ^= iv[i];
                }

                AlgorithmParameterSpec spec = new IvParameterSpec(nonce);
                try {
                    cipher.init(Cipher.ENCRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    throw new RuntimeException(
                                "invalid key or spec in AEAD mode", ikae);
                }

                int outputSize = cipher.getOutputSize(bb.remaining());
                byte[] aad = authenticator.acquireAuthenticationBytes(
                                        contentType, outputSize, sn);
                cipher.updateAAD(aad);

                int pos = bb.position();
                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext before ENCRYPTION",
                            bb.duplicate());
                }

                ByteBuffer dup = bb.duplicate();
                if (outputSize > bb.remaining()) {
                    bb.limit(pos + outputSize);
                }

                int len;
                try {
                    len = cipher.doFinal(dup, bb);
                } catch (IllegalBlockSizeException |
                            BadPaddingException | ShortBufferException ibse) {
                    throw new RuntimeException(
                            "Cipher error in AEAD mode in JCE provider " +
                            cipher.getProvider().getName(), ibse);
                }

                if (len != outputSize) {
                    throw new RuntimeException(
                            "Cipher buffering error in JCE provider " +
                            cipher.getProvider().getName());
                }

                if (keyLimitEnabled) {
                    keyLimitCountdown -= len;
                }
                return len;
            }

            @Override
            int getExplicitNonceSize() {
                return 0;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                return packetLimit - headerSize - tagSize;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                return fragmentSize + headerSize + tagSize;
            }
        }
    }

    private static void addMac(MAC signer,
            ByteBuffer destination, byte contentType) {
        if (signer.macAlg().size != 0) {
            int dstContent = destination.position();
            byte[] hash = signer.compute(contentType, destination, false);

            /*
             * position was advanced to limit in MAC compute above.
             *
             * Mark next area as writable (above layers should have
             * established that we have plenty of room), then write
             * out the hash.
             */
            destination.limit(destination.limit() + hash.length);
            destination.put(hash);

            destination.position(dstContent);
        }
    }

    private static void checkStreamMac(MAC signer, ByteBuffer bb,
            byte contentType,  byte[] sequence) throws BadPaddingException {
        int tagLen = signer.macAlg().size;

        if (tagLen != 0) {
            int contentLen = bb.remaining() - tagLen;
            if (contentLen < 0) {
                throw new BadPaddingException("bad record");
            }

            if (checkMacTags(contentType, bb, signer, sequence, false)) {
                throw new BadPaddingException("bad record MAC");
            }
        }
    }

    private static void checkCBCMac(MAC signer, ByteBuffer bb,
            byte contentType, int cipheredLength,
            byte[] sequence) throws BadPaddingException {
        BadPaddingException reservedBPE = null;
        int tagLen = signer.macAlg().size;
        int pos = bb.position();

        if (tagLen != 0) {
            int contentLen = bb.remaining() - tagLen;
            if (contentLen < 0) {
                reservedBPE = new BadPaddingException("bad record");

                contentLen = cipheredLength - tagLen;
                bb.limit(pos + cipheredLength);
            }

            if (checkMacTags(contentType, bb, signer, sequence, false)) {
                if (reservedBPE == null) {
                    reservedBPE =
                            new BadPaddingException("bad record MAC");
                }
            }

            int remainingLen = calculateRemainingLen(
                    signer, cipheredLength, contentLen);

            remainingLen += signer.macAlg().size;
            ByteBuffer temporary = ByteBuffer.allocate(remainingLen);

            checkMacTags(contentType, temporary, signer, sequence, true);
        }

        if (reservedBPE != null) {
            throw reservedBPE;
        }
    }

    /*
     * Run MAC computation and comparison
     */
    private static boolean checkMacTags(byte contentType, ByteBuffer bb,
            MAC signer, byte[] sequence, boolean isSimulated) {
        int tagLen = signer.macAlg().size;
        int position = bb.position();
        int lim = bb.limit();
        int macOffset = lim - tagLen;

        bb.limit(macOffset);
        byte[] hash = signer.compute(contentType, bb, sequence, isSimulated);
        if (hash == null || tagLen != hash.length) {
            throw new RuntimeException("Internal MAC error");
        }

        bb.position(macOffset);
        bb.limit(lim);
        try {
            int[] results = compareMacTags(bb, hash);
            return (results[0] != 0);
        } finally {
            bb.position(position);
            bb.limit(macOffset);
        }
    }

    /*
     * A constant-time comparison of the MAC tags.
     *
     * Please DON'T change the content of the ByteBuffer parameter!
     */
    private static int[] compareMacTags(ByteBuffer bb, byte[] tag) {
        int[] results = {0, 0};     

        for (byte t : tag) {
            if (bb.get() != t) {
                results[0]++;       
            } else {
                results[1]++;       
            }
        }

        return results;
    }

    /*
     * Calculate the length of a dummy buffer to run MAC computation
     * and comparison on the remainder.
     *
     * The caller MUST ensure that the fullLen is not less than usedLen.
     */
    private static int calculateRemainingLen(
            MAC signer, int fullLen, int usedLen) {

        int blockLen = signer.macAlg().hashBlockSize;
        int minimalPaddingLen = signer.macAlg().minimalPaddingSize;

        fullLen += 13 - (blockLen - minimalPaddingLen);
        usedLen += 13 - (blockLen - minimalPaddingLen);

        return 0x01 + (int)(Math.ceil(fullLen/(1.0d * blockLen)) -
                Math.ceil(usedLen/(1.0d * blockLen))) * blockLen;
    }

    private static int addPadding(ByteBuffer bb, int blockSize) {

        int     len = bb.remaining();
        int     offset = bb.position();

        int     newlen = len + 1;
        byte    pad;
        int     i;

        if ((newlen % blockSize) != 0) {
            newlen += blockSize - 1;
            newlen -= newlen % blockSize;
        }
        pad = (byte) (newlen - len);

        /*
         * Update the limit to what will be padded.
         */
        bb.limit(newlen + offset);

        /*
         * TLS version of the padding works for both SSLv3 and TLSv1
         */
        for (i = 0, offset += len; i < pad; i++) {
            bb.put(offset++, (byte) (pad - 1));
        }

        bb.position(offset);
        bb.limit(offset);

        return newlen;
    }

    private static int removePadding(ByteBuffer bb,
            int tagLen, int blockSize,
            ProtocolVersion protocolVersion) throws BadPaddingException {
        int len = bb.remaining();
        int offset = bb.position();

        int padOffset = offset + len - 1;
        int padLen = bb.get(padOffset) & 0xFF;

        int newLen = len - (padLen + 1);
        if ((newLen - tagLen) < 0) {
            checkPadding(bb.duplicate(), (byte)(padLen & 0xFF));

            throw new BadPaddingException("Invalid Padding length: " + padLen);
        }

        int[] results = checkPadding(
                bb.duplicate().position(offset + newLen),
                (byte)(padLen & 0xFF));
        if (protocolVersion.useTLS10PlusSpec()) {
            if (results[0] != 0) {          
                throw new BadPaddingException("Invalid TLS padding data");
            }
        } else { 
            if (padLen > blockSize) {
                throw new BadPaddingException("Padding length (" +
                padLen + ") of SSLv3 message should not be bigger " +
                "than the block size (" + blockSize + ")");
            }
        }

        bb.limit(offset + newLen);

        return newLen;
    }

    /*
     * A constant-time check of the padding.
     *
     * NOTE that we are checking both the padding and the padLen bytes here.
     *
     * The caller MUST ensure that the bb parameter has remaining.
     */
    private static int[] checkPadding(ByteBuffer bb, byte pad) {
        if (!bb.hasRemaining()) {
            throw new RuntimeException("hasRemaining() must be positive");
        }

        int[] results = {0, 0};    
        bb.mark();
        for (int i = 0; i <= 256; bb.reset()) {
            for (; bb.hasRemaining() && i <= 256; i++) {
                if (bb.get() != pad) {
                    results[0]++;       
                } else {
                    results[1]++;       
                }
            }
        }

        return results;
    }
}
