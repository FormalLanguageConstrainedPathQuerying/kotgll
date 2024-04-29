/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
/*
 * $Id: SignatureMethod.java,v 1.5 2005/05/10 16:03:46 mullan Exp $
 */
package javax.xml.crypto.dsig;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.spec.SignatureMethodParameterSpec;
import java.security.spec.AlgorithmParameterSpec;

/**
 * A representation of the XML <code>SignatureMethod</code> element
 * as defined in the <a href="https:
 * W3C Recommendation for XML-Signature Syntax and Processing</a>.
 * The XML Schema Definition is defined as:
 * <pre>
 *   &lt;element name="SignatureMethod" type="ds:SignatureMethodType"/&gt;
 *     &lt;complexType name="SignatureMethodType" mixed="true"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="HMACOutputLength" minOccurs="0" type="ds:HMACOutputLengthType"/&gt;
 *         &lt;any namespace="##any" minOccurs="0" maxOccurs="unbounded"/&gt;
 *           &lt;!-- (0,unbounded) elements from (1,1) namespace --&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="Algorithm" type="anyURI" use="required"/&gt;
 *     &lt;/complexType&gt;
 * </pre>
 *
 * A <code>SignatureMethod</code> instance may be created by invoking the
 * {@link XMLSignatureFactory#newSignatureMethod newSignatureMethod} method
 * of the {@link XMLSignatureFactory} class.
 * <p>
 * The signature method algorithm URIs defined in this class are specified
 * in the <a href="https:
 * W3C Recommendation for XML-Signature Syntax and Processing</a>
 * and <a href="https:
 * RFC 9231: Additional XML Security Uniform Resource Identifiers (URIs)</a>
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @since 1.6
 * @see XMLSignatureFactory#newSignatureMethod(String, SignatureMethodParameterSpec)
 */
public interface SignatureMethod extends XMLStructure, AlgorithmMethod {

    /**
     * The <a href="http:
     * (DSS) signature method algorithm URI.
     */
    String DSA_SHA1 =
        "http:

    /**
     * The <a href="http:
     * (DSS) signature method algorithm URI.
     *
     * @since 11
     */
    String DSA_SHA256 = "http:

    /**
     * The <a href="http:
     * (PKCS #1) signature method algorithm URI.
     */
    String RSA_SHA1 =
        "http:

    /**
     * The <a href="http:
     * RSA-SHA224</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String RSA_SHA224 = "http:

    /**
     * The <a href="http:
     * RSA-SHA256</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String RSA_SHA256 = "http:

    /**
     * The <a href="http:
     * RSA-SHA384</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String RSA_SHA384 = "http:

    /**
     * The <a href="http:
     * RSA-SHA512</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String RSA_SHA512 = "http:

    /**
     * The <a href="http:
     * SHA1-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String SHA1_RSA_MGF1 = "http:

    /**
     * The <a href="http:
     * SHA224-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String SHA224_RSA_MGF1 = "http:

    /**
     * The <a href="http:
     * SHA256-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String SHA256_RSA_MGF1 = "http:

    /**
     * The <a href="http:
     * SHA384-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String SHA384_RSA_MGF1 = "http:

    /**
     * The <a href="http:
     * SHA512-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String SHA512_RSA_MGF1 = "http:

    /**
     * The <a href="http:
     * ECDSA-SHA1</a> (FIPS 180-4) signature method algorithm URI.
     *
     * @since 11
     */
    String ECDSA_SHA1 = "http:

    /**
     * The <a href="http:
     * ECDSA-SHA224</a> (FIPS 180-4) signature method algorithm URI.
     *
     * @since 11
     */
    String ECDSA_SHA224 = "http:

    /**
     * The <a href="http:
     * ECDSA-SHA256</a> (FIPS 180-4) signature method algorithm URI.
     *
     * @since 11
     */
    String ECDSA_SHA256 = "http:

    /**
     * The <a href="http:
     * ECDSA-SHA384</a> (FIPS 180-4) signature method algorithm URI.
     *
     * @since 11
     */
    String ECDSA_SHA384 = "http:

    /**
     * The <a href="http:
     * ECDSA-SHA512</a> (FIPS 180-4) signature method algorithm URI.
     *
     * @since 11
     */
    String ECDSA_SHA512 = "http:

    /**
     * The <a href="http:
     * MAC signature method algorithm URI
     */
    String HMAC_SHA1 =
        "http:

    /**
     * The <a href="http:
     * HMAC-SHA224</a> MAC signature method algorithm URI.
     *
     * @since 11
     */
    String HMAC_SHA224 = "http:

    /**
     * The <a href="http:
     * HMAC-SHA256</a> MAC signature method algorithm URI.
     *
     * @since 11
     */
    String HMAC_SHA256 = "http:

    /**
     * The <a href="http:
     * HMAC-SHA384</a> MAC signature method algorithm URI.
     *
     * @since 11
     */
    String HMAC_SHA384 = "http:

    /**
     * The <a href="http:
     * HMAC-SHA512</a> MAC signature method algorithm URI.
     *
     * @since 11
     */
    String HMAC_SHA512 = "http:


    /**
     * The <a href="http:
     * RSASSA-PSS</a> signature method algorithm URI.
     * <p>
     * Calling {@link XMLSignatureFactory#newSignatureMethod
     * XMLSignatureFactory.newSignatureMethod(RSA_PSS, null)} returns a
     * {@code SignatureMethod} object that uses the default parameter as defined in
     * <a href="https:
     * which uses SHA-256 as the {@code DigestMethod}, MGF1 with SHA-256 as the
     * {@code MaskGenerationFunction}, 32 as {@code SaltLength}, and 1 as
     * {@code TrailerField}. This default parameter is represented as an
     * {@link javax.xml.crypto.dsig.spec.RSAPSSParameterSpec RSAPSSParameterSpec}
     * type and returned by the {@link #getParameterSpec()} method
     * of the {@code SignatureMethod} object.
     *
     * @since 17
     */
    String RSA_PSS = "http:

    /**
     * The <a href="http:
     * ED25519</a> signature method algorithm URI.
     *
     * @since 21
     */
    String ED25519 = "http:

    /**
     * The <a href="http:
     * ED448</a> signature method algorithm URI.
     *
     * @since 21
     */
    String ED448 = "http:

    /**
     * The <a href="http:
     * SHA3-224-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 22
     */
    String SHA3_224_RSA_MGF1 =
            "http:

    /**
     * The <a href="http:
     * SHA3-256-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 22
     */
    String SHA3_256_RSA_MGF1 =
            "http:

    /**
     * The <a href="http:
     * SHA3-384-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 22
     */
    String SHA3_384_RSA_MGF1 =
            "http:

    /**
     * The <a href="http:
     * SHA3-512-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 22
     */
    String SHA3_512_RSA_MGF1 =
            "http:


    /**
     * Returns the algorithm-specific input parameters of this
     * <code>SignatureMethod</code>.
     *
     * <p>The returned parameters can be typecast to a {@link
     * SignatureMethodParameterSpec} object.
     *
     * @return the algorithm-specific input parameters of this
     *    <code>SignatureMethod</code> (may be <code>null</code> if not
     *    specified)
     */
    AlgorithmParameterSpec getParameterSpec();
}
