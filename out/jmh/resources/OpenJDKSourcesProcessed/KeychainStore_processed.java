/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

package apple.security;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.*;
import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.*;
import javax.security.auth.x500.*;

import sun.security.pkcs.*;
import sun.security.pkcs.EncryptedPrivateKeyInfo;
import sun.security.util.*;
import sun.security.x509.*;

/**
 * This class provides the keystore implementation referred to as "KeychainStore".
 * It uses the current user's keychain as its backing storage, and does NOT support
 * a file-based implementation.
 */

public final class KeychainStore extends KeyStoreSpi {

    static class KeyEntry {
        Date date; 
        byte[] protectedPrivKey;
        char[] password;
        long keyRef;  
        Certificate chain[];
        long chainRefs[];  
    };

    static class TrustedCertEntry {
        Date date; 

        Certificate cert;
        long certRef;  


        List<Map<String, String>> trustSettings;

        String trustedKeyUsageValue;
    };

    /**
     * Entries that have been deleted.  When something calls engineStore we'll
     * remove them from the keychain.
     */
    private Hashtable<String, Object> deletedEntries = new Hashtable<>();

    /**
     * Entries that have been added.  When something calls engineStore we'll
     * add them to the keychain.
     */
    private Hashtable<String, Object> addedEntries = new Hashtable<>();

    /**
     * Private keys and certificates are stored in a hashtable.
     * Hash entries are keyed by alias names.
     */
    private Hashtable<String, Object> entries = new Hashtable<>();

    /**
     * Algorithm identifiers and corresponding OIDs for the contents of the
     * PKCS12 bag we get from the Keychain.
     */
    private static ObjectIdentifier PKCS8ShroudedKeyBag_OID =
            ObjectIdentifier.of(KnownOIDs.PKCS8ShroudedKeyBag);
    private static ObjectIdentifier pbeWithSHAAnd3KeyTripleDESCBC_OID =
            ObjectIdentifier.of(KnownOIDs.PBEWithSHA1AndDESede);

    /**
     * Constnats used in PBE decryption.
     */
    private static final int iterationCount = 1024;
    private static final int SALT_LEN = 20;

    private static final Debug debug = Debug.getInstance("keystore");

    static {
        jdk.internal.loader.BootLoader.loadLibrary("osxsecurity");
    }

    private static void permissionCheck() {
        @SuppressWarnings("removal")
        SecurityManager sec = System.getSecurityManager();

        if (sec != null) {
            sec.checkPermission(new RuntimePermission("useKeychainStore"));
        }
    }


    /**
     * Verify the Apple provider in the constructor.
     *
     * @exception SecurityException if fails to verify
     * its own integrity
     */
    public KeychainStore() { }

    /**
     * Returns the key associated with the given alias, using the given
     * password to recover it.
     *
     * @param alias the alias name
     * @param password the password for recovering the key. This password is
     *        used internally as the key is exported in a PKCS12 format.
     *
     * @return the requested key, or null if the given alias does not exist
     * or does not identify a <i>key entry</i>.
     *
     * @exception NoSuchAlgorithmException if the algorithm for recovering the
     * key cannot be found
     * @exception UnrecoverableKeyException if the key cannot be recovered
     * (e.g., the given password is wrong).
     */
    public Key engineGetKey(String alias, char[] password)
        throws NoSuchAlgorithmException, UnrecoverableKeyException
    {
        permissionCheck();

        if (password == null || password.length == 0) {
            if (random == null) {
                random = new SecureRandom();
            }
            password = Long.toString(random.nextLong()).toCharArray();
        }

        Object entry = entries.get(alias.toLowerCase(Locale.ROOT));

        if (!(entry instanceof KeyEntry keyEntry)) {
            return null;
        }

        byte[] exportedKeyInfo = _getEncodedKeyData(keyEntry.keyRef, password);
        if (exportedKeyInfo == null) {
            return null;
        }

        PrivateKey returnValue = null;

        try {
            byte[] pkcs8KeyData = fetchPrivateKeyFromBag(exportedKeyInfo);
            byte[] encryptedKey;
            AlgorithmParameters algParams;
            ObjectIdentifier algOid;
            try {
                EncryptedPrivateKeyInfo encrInfo = new EncryptedPrivateKeyInfo(pkcs8KeyData);
                encryptedKey = encrInfo.getEncryptedData();

                DerValue val = new DerValue(encrInfo.getAlgorithm().encode());
                DerInputStream in = val.toDerInputStream();
                algOid = in.getOID();
                algParams = parseAlgParameters(in);

            } catch (IOException ioe) {
                UnrecoverableKeyException uke =
                new UnrecoverableKeyException("Private key not stored as "
                                              + "PKCS#8 EncryptedPrivateKeyInfo: " + ioe);
                uke.initCause(ioe);
                throw uke;
            }

            SecretKey skey = getPBEKey(password);
            Cipher cipher = Cipher.getInstance(algOid.toString());
            cipher.init(Cipher.DECRYPT_MODE, skey, algParams);
            byte[] decryptedPrivateKey = cipher.doFinal(encryptedKey);
            PKCS8EncodedKeySpec kspec = new PKCS8EncodedKeySpec(decryptedPrivateKey);

            DerValue val = new DerValue(decryptedPrivateKey);
            DerInputStream in = val.toDerInputStream();

            int i = in.getInteger();

            DerValue[] value = in.getSequence(2);
            if (value.length < 1 || value.length > 2) {
                throw new IOException("Invalid length for AlgorithmIdentifier");
            }
            AlgorithmId algId = new AlgorithmId(value[0].getOID());
            String algName = algId.getName();

            KeyFactory kfac = KeyFactory.getInstance(algName);
            returnValue = kfac.generatePrivate(kspec);
        } catch (Exception e) {
            UnrecoverableKeyException uke =
            new UnrecoverableKeyException("Get Key failed: " +
                                          e.getMessage());
            uke.initCause(e);
            throw uke;
        }

        return returnValue;
    }

    private native byte[] _getEncodedKeyData(long secKeyRef, char[] password);

    /**
     * Returns the certificate chain associated with the given alias.
     *
     * @param alias the alias name
     *
     * @return the certificate chain (ordered with the user's certificate first
     * and the root certificate authority last), or null if the given alias
     * does not exist or does not contain a certificate chain (i.e., the given
     * alias identifies either a <i>trusted certificate entry</i> or a
     * <i>key entry</i> without a certificate chain).
     */
    public Certificate[] engineGetCertificateChain(String alias) {
        permissionCheck();

        Object entry = entries.get(alias.toLowerCase(Locale.ROOT));

        if (entry instanceof KeyEntry keyEntry) {
            if (keyEntry.chain == null) {
                return null;
            } else {
                return keyEntry.chain.clone();
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the certificate associated with the given alias.
     *
     * <p>If the given alias name identifies a
     * <i>trusted certificate entry</i>, the certificate associated with that
     * entry is returned. If the given alias name identifies a
     * <i>key entry</i>, the first element of the certificate chain of that
     * entry is returned, or null if that entry does not have a certificate
     * chain.
     *
     * @param alias the alias name
     *
     * @return the certificate, or null if the given alias does not exist or
     * does not contain a certificate.
     */
    public Certificate engineGetCertificate(String alias) {
        permissionCheck();

        Object entry = entries.get(alias.toLowerCase(Locale.ROOT));

        if (entry != null) {
            if (entry instanceof TrustedCertEntry) {
                return ((TrustedCertEntry)entry).cert;
            } else {
                KeyEntry ke = (KeyEntry)entry;
                if (ke.chain == null || ke.chain.length == 0) {
                    return null;
                }
                return ke.chain[0];
            }
        } else {
            return null;
        }
    }

    private record LocalAttr(String name, String value)
            implements KeyStore.Entry.Attribute {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    @Override
    public KeyStore.Entry engineGetEntry(String alias, KeyStore.ProtectionParameter protParam)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException {
        if (engineIsCertificateEntry(alias)) {
            Object entry = entries.get(alias.toLowerCase(Locale.ROOT));
            if (entry instanceof TrustedCertEntry tEntry) {
                return new KeyStore.TrustedCertificateEntry(
                        tEntry.cert, Set.of(
                                new LocalAttr(KnownOIDs.ORACLE_TrustedKeyUsage.value(), tEntry.trustedKeyUsageValue),
                                new LocalAttr("trustSettings", tEntry.trustSettings.toString())));
            }
        }
        return super.engineGetEntry(alias, protParam);
    }

    /**
     * Returns the creation date of the entry identified by the given alias.
     *
     * @param alias the alias name
     *
     * @return the creation date of this entry, or null if the given alias does
     * not exist
     */
    public Date engineGetCreationDate(String alias) {
        permissionCheck();

        Object entry = entries.get(alias.toLowerCase(Locale.ROOT));

        if (entry != null) {
            if (entry instanceof TrustedCertEntry) {
                return new Date(((TrustedCertEntry)entry).date.getTime());
            } else {
                return new Date(((KeyEntry)entry).date.getTime());
            }
        } else {
            return null;
        }
    }

    /**
     * Assigns the given key to the given alias, protecting it with the given
     * password.
     *
     * <p>If the given key is of type <code>java.security.PrivateKey</code>,
     * it must be accompanied by a certificate chain certifying the
     * corresponding public key.
     *
     * <p>If the given alias already exists, the keystore information
     * associated with it is overridden by the given key (and possibly
     * certificate chain).
     *
     * @param alias the alias name
     * @param key the key to be associated with the alias
     * @param password the password to protect the key
     * @param chain the certificate chain for the corresponding public
     * key (only required if the given key is of type
     * <code>java.security.PrivateKey</code>).
     *
     * @exception KeyStoreException if the given key cannot be protected, or
     * this operation fails for some other reason
     */
    public void engineSetKeyEntry(String alias, Key key, char[] password,
                                  Certificate[] chain)
        throws KeyStoreException
    {
        permissionCheck();

        synchronized(entries) {
            try {
                KeyEntry entry = new KeyEntry();
                entry.date = new Date();

                if (key instanceof PrivateKey) {
                    if ((key.getFormat().equals("PKCS#8")) ||
                        (key.getFormat().equals("PKCS8"))) {
                        entry.protectedPrivKey = encryptPrivateKey(key.getEncoded(), password);
                        entry.password = password.clone();
                    } else {
                        throw new KeyStoreException("Private key is not encoded as PKCS#8");
                    }
                } else {
                    throw new KeyStoreException("Key is not a PrivateKey");
                }

                if (chain != null) {
                    if ((chain.length > 1) && !validateChain(chain)) {
                        throw new KeyStoreException("Certificate chain does not validate");
                    }

                    entry.chain = chain.clone();
                    entry.chainRefs = new long[entry.chain.length];
                }

                String lowerAlias = alias.toLowerCase(Locale.ROOT);
                if (entries.get(lowerAlias) != null) {
                    deletedEntries.put(lowerAlias, entries.get(lowerAlias));
                }

                entries.put(lowerAlias, entry);
                addedEntries.put(lowerAlias, entry);
            } catch (Exception nsae) {
                KeyStoreException ke = new KeyStoreException("Key protection algorithm not found: " + nsae);
                ke.initCause(nsae);
                throw ke;
            }
        }
    }

    /**
     * Assigns the given key (that has already been protected) to the given
     * alias.
     *
     * <p>If the protected key is of type
     * <code>java.security.PrivateKey</code>, it must be accompanied by a
     * certificate chain certifying the corresponding public key. If the
     * underlying keystore implementation is of type <code>jks</code>,
     * <code>key</code> must be encoded as an
     * <code>EncryptedPrivateKeyInfo</code> as defined in the PKCS #8 standard.
     *
     * <p>If the given alias already exists, the keystore information
     * associated with it is overridden by the given key (and possibly
     * certificate chain).
     *
     * @param alias the alias name
     * @param key the key (in protected format) to be associated with the alias
     * @param chain the certificate chain for the corresponding public
     * key (only useful if the protected key is of type
     * <code>java.security.PrivateKey</code>).
     *
     * @exception KeyStoreException if this operation fails.
     */
    public void engineSetKeyEntry(String alias, byte[] key,
                                  Certificate[] chain)
        throws KeyStoreException
    {
        permissionCheck();

        synchronized(entries) {
            KeyEntry entry = new KeyEntry();
            try {
                EncryptedPrivateKeyInfo privateKey = new EncryptedPrivateKeyInfo(key);
                entry.protectedPrivKey = privateKey.getEncoded();
            } catch (IOException ioe) {
                throw new KeyStoreException("key is not encoded as "
                                            + "EncryptedPrivateKeyInfo");
            }

            entry.date = new Date();

            if ((chain != null) &&
                (chain.length != 0)) {
                entry.chain = chain.clone();
                entry.chainRefs = new long[entry.chain.length];
            }

            String lowerAlias = alias.toLowerCase(Locale.ROOT);
            if (entries.get(lowerAlias) != null) {
                deletedEntries.put(lowerAlias, entries.get(alias));
            }
            entries.put(lowerAlias, entry);
            addedEntries.put(lowerAlias, entry);
        }
    }

    /**
     * Adding trusted certificate entry is not supported.
     */
    public void engineSetCertificateEntry(String alias, Certificate cert)
            throws KeyStoreException {
        throw new KeyStoreException("Cannot set trusted certificate entry." +
                " Use the macOS \"security add-trusted-cert\" command instead.");
    }

    /**
     * Deletes the entry identified by the given alias from this keystore.
     *
     * @param alias the alias name
     *
     * @exception KeyStoreException if the entry cannot be removed.
     */
    public void engineDeleteEntry(String alias)
        throws KeyStoreException
    {
        permissionCheck();

        String lowerAlias = alias.toLowerCase(Locale.ROOT);
        synchronized(entries) {
            Object entry = entries.remove(lowerAlias);
            deletedEntries.put(lowerAlias, entry);
        }
    }

    /**
     * Lists all the alias names of this keystore.
     *
     * @return enumeration of the alias names
     */
    public Enumeration<String> engineAliases() {
        permissionCheck();
        return entries.keys();
    }

    /**
     * Checks if the given alias exists in this keystore.
     *
     * @param alias the alias name
     *
     * @return true if the alias exists, false otherwise
     */
    public boolean engineContainsAlias(String alias) {
        permissionCheck();
        return entries.containsKey(alias.toLowerCase(Locale.ROOT));
    }

    /**
     * Retrieves the number of entries in this keystore.
     *
     * @return the number of entries in this keystore
     */
    public int engineSize() {
        permissionCheck();
        return entries.size();
    }

    /**
     * Returns true if the entry identified by the given alias is a
     * <i>key entry</i>, and false otherwise.
     *
     * @return true if the entry identified by the given alias is a
     * <i>key entry</i>, false otherwise.
     */
    public boolean engineIsKeyEntry(String alias) {
        permissionCheck();
        Object entry = entries.get(alias.toLowerCase(Locale.ROOT));
        return entry instanceof KeyEntry;
    }

    /**
     * Returns true if the entry identified by the given alias is a
     * <i>trusted certificate entry</i>, and false otherwise.
     *
     * @return true if the entry identified by the given alias is a
     * <i>trusted certificate entry</i>, false otherwise.
     */
    public boolean engineIsCertificateEntry(String alias) {
        permissionCheck();
        Object entry = entries.get(alias.toLowerCase(Locale.ROOT));
        return entry instanceof TrustedCertEntry;
    }

    /**
     * Returns the (alias) name of the first keystore entry whose certificate
     * matches the given certificate.
     *
     * <p>This method attempts to match the given certificate with each
     * keystore entry. If the entry being considered
     * is a <i>trusted certificate entry</i>, the given certificate is
     * compared to that entry's certificate. If the entry being considered is
     * a <i>key entry</i>, the given certificate is compared to the first
     * element of that entry's certificate chain (if a chain exists).
     *
     * @param cert the certificate to match with.
     *
     * @return the (alias) name of the first entry with matching certificate,
     * or null if no such entry exists in this keystore.
     */
    public String engineGetCertificateAlias(Certificate cert) {
        permissionCheck();
        Certificate certElem;

        for (Enumeration<String> e = entries.keys(); e.hasMoreElements(); ) {
            String alias = e.nextElement();
            Object entry = entries.get(alias);
            if (entry instanceof TrustedCertEntry) {
                certElem = ((TrustedCertEntry)entry).cert;
            } else {
                KeyEntry ke = (KeyEntry)entry;
                if (ke.chain == null || ke.chain.length == 0) {
                    continue;
                }
                certElem = ke.chain[0];
            }
            if (certElem.equals(cert)) {
                return alias;
            }
        }
        return null;
    }

    /**
     * Stores this keystore to the given output stream, and protects its
     * integrity with the given password.
     *
     * @param stream Ignored. the output stream to which this keystore is written.
     * @param password the password to generate the keystore integrity check
     *
     * @exception IOException if there was an I/O problem with data
     * @exception NoSuchAlgorithmException if the appropriate data integrity
     * algorithm could not be found
     * @exception CertificateException if any of the certificates included in
     * the keystore data could not be stored
     */
    public void engineStore(OutputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        permissionCheck();

        for (Enumeration<String> e = deletedEntries.keys(); e.hasMoreElements(); ) {
            String alias = e.nextElement();
            Object entry = deletedEntries.get(alias);
            if (entry instanceof TrustedCertEntry) {
                if (((TrustedCertEntry)entry).certRef != 0) {
                    _removeItemFromKeychain(((TrustedCertEntry)entry).certRef);
                    _releaseKeychainItemRef(((TrustedCertEntry)entry).certRef);
                }
            } else {
                KeyEntry keyEntry = (KeyEntry)entry;

                if (keyEntry.chain != null) {
                    for (int i = 0; i < keyEntry.chain.length; i++) {
                        if (keyEntry.chainRefs[i] != 0) {
                            _removeItemFromKeychain(keyEntry.chainRefs[i]);
                            _releaseKeychainItemRef(keyEntry.chainRefs[i]);
                        }
                    }

                    if (keyEntry.keyRef != 0) {
                        _removeItemFromKeychain(keyEntry.keyRef);
                        _releaseKeychainItemRef(keyEntry.keyRef);
                    }
                }
            }
        }

        for (Enumeration<String> e = addedEntries.keys(); e.hasMoreElements(); ) {
            String alias = e.nextElement();
            Object entry = addedEntries.get(alias);
            if (entry instanceof TrustedCertEntry) {
            } else {
                KeyEntry keyEntry = (KeyEntry)entry;

                if (keyEntry.chain != null) {
                    for (int i = 0; i < keyEntry.chain.length; i++) {
                        keyEntry.chainRefs[i] = addCertificateToKeychain(alias, keyEntry.chain[i]);
                    }

                    keyEntry.keyRef = _addItemToKeychain(alias, false, keyEntry.protectedPrivKey, keyEntry.password);
                }
            }
        }

        deletedEntries.clear();
        addedEntries.clear();
    }

    private long addCertificateToKeychain(String alias, Certificate cert) {
        byte[] certblob = null;
        long returnValue = 0;

        try {
            certblob = cert.getEncoded();
            returnValue = _addItemToKeychain(alias, true, certblob, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnValue;
    }

    private native long _addItemToKeychain(String alias, boolean isCertificate, byte[] datablob, char[] password);
    private native int _removeItemFromKeychain(long certRef);
    private native void _releaseKeychainItemRef(long keychainItemRef);

    /**
     * Loads the keystore from the Keychain.
     *
     * @param stream Ignored - here for API compatibility.
     * @param password Ignored - if user needs to unlock keychain Security
     * framework will post any dialogs.
     *
     * @exception IOException if there is an I/O or format problem with the
     * keystore data
     * @exception NoSuchAlgorithmException if the algorithm used to check
     * the integrity of the keystore cannot be found
     * @exception CertificateException if any of the certificates in the
     * keystore could not be loaded
     */
    public void engineLoad(InputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        permissionCheck();

        synchronized(entries) {
            for (Enumeration<String> e = entries.keys(); e.hasMoreElements(); ) {
                String alias = e.nextElement();
                Object entry = entries.get(alias);
                if (entry instanceof TrustedCertEntry) {
                    if (((TrustedCertEntry)entry).certRef != 0) {
                        _releaseKeychainItemRef(((TrustedCertEntry)entry).certRef);
                    }
                } else {
                    KeyEntry keyEntry = (KeyEntry)entry;

                    if (keyEntry.chain != null) {
                        for (int i = 0; i < keyEntry.chain.length; i++) {
                            if (keyEntry.chainRefs[i] != 0) {
                                _releaseKeychainItemRef(keyEntry.chainRefs[i]);
                            }
                        }

                        if (keyEntry.keyRef != 0) {
                            _releaseKeychainItemRef(keyEntry.keyRef);
                        }
                    }
                }
            }

            entries.clear();
            _scanKeychain();
            if (debug != null) {
                debug.println("KeychainStore load entry count: " +
                        entries.size());
            }
        }
    }

    private native void _scanKeychain();

    /**
     * Callback method from _scanKeychain.  If a trusted certificate is found,
     * this method will be called.
     *
     * inputTrust is a list of strings in groups. Each group contains key/value
     * pairs for one trust setting and ends with a null. Thus the size of the
     * whole list is (2 * s_1 + 1) + (2 * s_2 + 1) + ... + (2 * s_n + 1),
     * where s_i is the size of mapping for the i'th trust setting,
     * and n is the number of trust settings. Ex:
     *
     * key1 for trust1
     * value1 for trust1
     * ..
     * null (end of trust1)
     * key1 for trust2
     * value1 for trust2
     * ...
     * null (end of trust2)
     * ...
     * null (end if trust_n)
     */
    private void createTrustedCertEntry(String alias, List<String> inputTrust,
            long keychainItemRef, long creationDate, byte[] derStream) {
        TrustedCertEntry tce = new TrustedCertEntry();

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream input = new ByteArrayInputStream(derStream);
            X509Certificate cert = (X509Certificate) cf.generateCertificate(input);
            input.close();
            tce.cert = cert;
            tce.certRef = keychainItemRef;

            if (entries.contains(alias.toLowerCase(Locale.ROOT))) {
                int uniqueVal = 1;
                String originalAlias = alias;
                var co = entries.get(alias.toLowerCase(Locale.ROOT));
                while (co != null) {
                    if (co instanceof TrustedCertEntry tco) {
                        if (tco.cert.equals(tce.cert)) {
                            return;
                        }
                    }
                    alias = originalAlias + " " + uniqueVal++;
                    co = entries.get(alias.toLowerCase(Locale.ROOT));
                }
            }

            tce.trustSettings = new ArrayList<>();
            Map<String, String> tmpMap = new LinkedHashMap<>();
            for (int i = 0; i < inputTrust.size(); i++) {
                if (inputTrust.get(i) == null) {
                    tce.trustSettings.add(tmpMap);
                    if (i < inputTrust.size() - 1) {
                        tmpMap = new LinkedHashMap<>();
                    }
                } else {
                    tmpMap.put(inputTrust.get(i), inputTrust.get(i+1));
                    i++;
                }
            }

            boolean isSelfSigned;
            try {
                cert.verify(cert.getPublicKey());
                isSelfSigned = true;
            } catch (Exception e) {
                isSelfSigned = false;
            }

            if (tce.trustSettings.isEmpty()) {
                if (isSelfSigned) {
                    tce.trustedKeyUsageValue = KnownOIDs.anyExtendedKeyUsage.value();
                } else {
                    return;
                }
            } else {
                List<String> values = new ArrayList<>();
                for (var oneTrust : tce.trustSettings) {
                    var result = oneTrust.get("kSecTrustSettingsResult");

                    if ("3".equals(result)) {
                        return;
                    }

                    if ((result == null && isSelfSigned)
                            || "1".equals(result) || "2".equals(result)) {
                        String oid = oneTrust.getOrDefault("SecPolicyOid",
                                KnownOIDs.anyExtendedKeyUsage.value());
                        if (!values.contains(oid)) {
                            values.add(oid);
                        }
                    }
                }
                if (values.isEmpty()) {
                    return;
                }
                if (values.size() == 1) {
                    tce.trustedKeyUsageValue = values.get(0);
                } else {
                    tce.trustedKeyUsageValue = values.toString();
                }
            }

            if (creationDate != 0)
                tce.date = new Date(creationDate);
            else
                tce.date = new Date();

            entries.put(alias.toLowerCase(Locale.ROOT), tce);
        } catch (Exception e) {
            System.err.println("KeychainStore Ignored Exception: " + e);
        }
    }

    /**
     * Callback method from _scanKeychain.  If an identity is found, this method will be called to create Java certificate
     * and private key objects from the keychain data.
     */
    private void createKeyEntry(String alias, long creationDate, long secKeyRef,
                                long[] secCertificateRefs, byte[][] rawCertData) {
        KeyEntry ke = new KeyEntry();

        ke.protectedPrivKey = null;
        ke.keyRef = secKeyRef;

        if (creationDate != 0)
            ke.date = new Date(creationDate);
        else
            ke.date = new Date();

        List<CertKeychainItemPair> createdCerts = new ArrayList<>();

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            for (int i = 0; i < rawCertData.length; i++) {
                try {
                    InputStream input = new ByteArrayInputStream(rawCertData[i]);
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(input);
                    input.close();

                    createdCerts.add(new CertKeychainItemPair(secCertificateRefs[i], cert));
                } catch (CertificateException e) {
                    System.err.println("KeychainStore Ignored Exception: " + e);
                }
            }
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();  
        }

        CertKeychainItemPair[] objArray = createdCerts.toArray(new CertKeychainItemPair[0]);
        Certificate[] certArray = new Certificate[objArray.length];
        long[] certRefArray = new long[objArray.length];

        for (int i = 0; i < objArray.length; i++) {
            CertKeychainItemPair addedItem = objArray[i];
            certArray[i] = addedItem.mCert;
            certRefArray[i] = addedItem.mCertificateRef;
        }

        ke.chain = certArray;
        ke.chainRefs = certRefArray;

        int uniqueVal = 1;
        String originalAlias = alias;

        while (entries.containsKey(alias.toLowerCase(Locale.ROOT))) {
            alias = originalAlias + " " + uniqueVal;
            uniqueVal++;
        }

        entries.put(alias.toLowerCase(Locale.ROOT), ke);
    }

    private static class CertKeychainItemPair {
        long mCertificateRef;
        Certificate mCert;

        CertKeychainItemPair(long inCertRef, Certificate cert) {
            mCertificateRef = inCertRef;
            mCert = cert;
        }
    }

    /*
     * Validate Certificate Chain
     */
    private boolean validateChain(Certificate[] certChain)
    {
        for (int i = 0; i < certChain.length-1; i++) {
            X500Principal issuerDN =
            ((X509Certificate)certChain[i]).getIssuerX500Principal();
            X500Principal subjectDN =
                ((X509Certificate)certChain[i+1]).getSubjectX500Principal();
            if (!(issuerDN.equals(subjectDN)))
                return false;
        }
        return true;
    }

    private byte[] fetchPrivateKeyFromBag(byte[] privateKeyInfo) throws IOException, NoSuchAlgorithmException, CertificateException
    {
        byte[] returnValue = null;
        DerValue val = new DerValue(new ByteArrayInputStream(privateKeyInfo));
        DerInputStream s = val.toDerInputStream();
        int version = s.getInteger();

        if (version != 3) {
            throw new IOException("PKCS12 keystore not in version 3 format");
        }

        /*
         * Read the authSafe.
         */
        byte[] authSafeData;
        ContentInfo authSafe = new ContentInfo(s);
        ObjectIdentifier contentType = authSafe.getContentType();

        if (contentType.equals(ContentInfo.DATA_OID)) {
            authSafeData = authSafe.getData();
        } else /* signed data */ {
            throw new IOException("public key protected PKCS12 not supported");
        }

        DerInputStream as = new DerInputStream(authSafeData);
        DerValue[] safeContentsArray = as.getSequence(2);
        int count = safeContentsArray.length;

        /*
         * Spin over the ContentInfos.
         */
        for (int i = 0; i < count; i++) {
            byte[] safeContentsData;
            ContentInfo safeContents;
            DerInputStream sci;

            sci = new DerInputStream(safeContentsArray[i].toByteArray());
            safeContents = new ContentInfo(sci);
            contentType = safeContents.getContentType();
            safeContentsData = null;

            if (contentType.equals(ContentInfo.DATA_OID)) {
                safeContentsData = safeContents.getData();
            } else if (contentType.equals(ContentInfo.ENCRYPTED_DATA_OID)) {
                continue;
            } else {
                throw new IOException("public key protected PKCS12" +
                                      " not supported");
            }
            DerInputStream sc = new DerInputStream(safeContentsData);
            returnValue = extractKeyData(sc);
        }

        return returnValue;
    }

    private byte[] extractKeyData(DerInputStream stream)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        byte[] returnValue = null;
        DerValue[] safeBags = stream.getSequence(2);
        int count = safeBags.length;

        /*
         * Spin over the SafeBags.
         */
        for (int i = 0; i < count; i++) {
            ObjectIdentifier bagId;
            DerInputStream sbi;
            DerValue bagValue;

            sbi = safeBags[i].toDerInputStream();
            bagId = sbi.getOID();
            bagValue = sbi.getDerValue();
            if (!bagValue.isContextSpecific((byte)0)) {
                throw new IOException("unsupported PKCS12 bag value type "
                                      + bagValue.tag);
            }
            bagValue = bagValue.data.getDerValue();
            if (bagId.equals(PKCS8ShroudedKeyBag_OID)) {
                returnValue = bagValue.toByteArray();
            } else {
                System.out.println("Unsupported bag type '" + bagId + "'");
            }
        }

        return returnValue;
    }

    /*
     * Generate PBE Algorithm Parameters
     */
    private AlgorithmParameters getAlgorithmParameters(String algorithm)
        throws IOException
    {
        AlgorithmParameters algParams = null;

        PBEParameterSpec paramSpec =
            new PBEParameterSpec(getSalt(), iterationCount);
        try {
            algParams = AlgorithmParameters.getInstance(algorithm);
            algParams.init(paramSpec);
        } catch (Exception e) {
            IOException ioe =
            new IOException("getAlgorithmParameters failed: " +
                            e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
        return algParams;
    }

    private SecureRandom random;

    /*
     * Generate random salt
     */
    private byte[] getSalt()
    {
        byte[] salt = new byte[SALT_LEN];
        if (random == null) {
            random = new SecureRandom();
        }
        random.nextBytes(salt);
        return salt;
    }

    /*
     * parse Algorithm Parameters
     */
    private AlgorithmParameters parseAlgParameters(DerInputStream in)
        throws IOException
    {
        AlgorithmParameters algParams = null;
        try {
            DerValue params;
            if (in.available() == 0) {
                params = null;
            } else {
                params = in.getDerValue();
                if (params.tag == DerValue.tag_Null) {
                    params = null;
                }
            }
            if (params != null) {
                algParams = AlgorithmParameters.getInstance("PBE");
                algParams.init(params.toByteArray());
            }
        } catch (Exception e) {
            IOException ioe =
            new IOException("parseAlgParameters failed: " +
                            e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
        return algParams;
    }

    /*
     * Generate PBE key
     */
    private SecretKey getPBEKey(char[] password) throws IOException
    {
        SecretKey skey = null;

        try {
            PBEKeySpec keySpec = new PBEKeySpec(password);
            SecretKeyFactory skFac = SecretKeyFactory.getInstance("PBE");
            skey = skFac.generateSecret(keySpec);
        } catch (Exception e) {
            IOException ioe = new IOException("getSecretKey failed: " +
                                              e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
        return skey;
    }

    /*
     * Encrypt private key using Password-based encryption (PBE)
     * as defined in PKCS#5.
     *
     * NOTE: Currently pbeWithSHAAnd3-KeyTripleDES-CBC algorithmID is
     *       used to derive the key and IV.
     *
     * @return encrypted private key encoded as EncryptedPrivateKeyInfo
     */
    private byte[] encryptPrivateKey(byte[] data, char[] password)
        throws IOException, NoSuchAlgorithmException, UnrecoverableKeyException
    {
        byte[] key = null;

        try {
            AlgorithmParameters algParams =
            getAlgorithmParameters("PBEWithSHA1AndDESede");

            SecretKey skey = getPBEKey(password);
            Cipher cipher = Cipher.getInstance("PBEWithSHA1AndDESede");
            cipher.init(Cipher.ENCRYPT_MODE, skey, algParams);
            byte[] encryptedKey = cipher.doFinal(data);

            AlgorithmId algid =
                new AlgorithmId(pbeWithSHAAnd3KeyTripleDESCBC_OID, algParams);
            EncryptedPrivateKeyInfo encrInfo =
                new EncryptedPrivateKeyInfo(algid, encryptedKey);
            key = encrInfo.getEncoded();
        } catch (Exception e) {
            UnrecoverableKeyException uke =
            new UnrecoverableKeyException("Encrypt Private Key failed: "
                                          + e.getMessage());
            uke.initCause(e);
            throw uke;
        }

        return key;
    }


}

