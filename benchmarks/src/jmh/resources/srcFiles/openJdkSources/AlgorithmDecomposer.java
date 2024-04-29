/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * The class decomposes standard algorithms into sub-elements.
 */
public class AlgorithmDecomposer {

    private static final Pattern PATTERN =
            Pattern.compile("with|and|(?<!padd)in", Pattern.CASE_INSENSITIVE);

    private static final Map<String, String> DECOMPOSED_DIGEST_NAMES =
        Map.of("SHA-1", "SHA1", "SHA-224", "SHA224", "SHA-256", "SHA256",
               "SHA-384", "SHA384", "SHA-512", "SHA512", "SHA-512/224",
               "SHA512/224", "SHA-512/256", "SHA512/256");

    private static Set<String> decomposeImpl(String algorithm) {
        Set<String> elements = new HashSet<>();

        String[] transTokens = algorithm.split("/");

        for (String transToken : transTokens) {
            if (transToken.isEmpty()) {
                continue;
            }

            String[] tokens = PATTERN.split(transToken);

            for (String token : tokens) {
                if (token.isEmpty()) {
                    continue;
                }

                elements.add(token);
            }
        }
        return elements;
    }

    /**
     * Decompose the standard algorithm name into sub-elements.
     * <p>
     * For example, we need to decompose "SHA1WithRSA" into "SHA1" and "RSA"
     * so that we can check the "SHA1" and "RSA" algorithm constraints
     * separately.
     * <p>
     * Please override the method if you need to support more name pattern.
     */
    public Set<String> decompose(String algorithm) {
        if (algorithm == null || algorithm.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> elements = decomposeImpl(algorithm);


        if (!algorithm.contains("SHA")) {
            return elements;
        }

        for (Map.Entry<String, String> e : DECOMPOSED_DIGEST_NAMES.entrySet()) {
            if (elements.contains(e.getValue()) &&
                    !elements.contains(e.getKey())) {
                elements.add(e.getKey());
            } else if (elements.contains(e.getKey()) &&
                    !elements.contains(e.getValue())) {
                elements.add(e.getValue());
            }
        }

        return elements;
    }

    /**
     * Get aliases of the specified algorithm.
     *
     * May support more algorithms in the future.
     */
    public static Collection<String> getAliases(String algorithm) {
        String[] aliases;
        if (algorithm.equalsIgnoreCase("DH") ||
                algorithm.equalsIgnoreCase("DiffieHellman")) {
            aliases = new String[] {"DH", "DiffieHellman"};
        } else {
            aliases = new String[] {algorithm};
        }

        return Arrays.asList(aliases);
    }

    /**
     * Decomposes a standard algorithm name into sub-elements and uses a
     * consistent message digest algorithm name to avoid overly complicated
     * checking.
     */
    static Set<String> decomposeName(String algorithm) {
        if (algorithm == null || algorithm.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> elements = decomposeImpl(algorithm);

        if (!algorithm.contains("SHA")) {
            return elements;
        }

        for (Map.Entry<String, String> e : DECOMPOSED_DIGEST_NAMES.entrySet()) {
            if (elements.contains(e.getKey())) {
                elements.add(e.getValue());
                elements.remove(e.getKey());
            }
        }

        return elements;
    }

    /**
     * Decomposes a standard message digest algorithm name into a consistent
     * name for matching purposes.
     *
     * @param algorithm the name to be decomposed
     * @return the decomposed name, or the passed in algorithm name if
     *     it is not a digest algorithm or does not need to be decomposed
     */
    static String decomposeDigestName(String algorithm) {
        return DECOMPOSED_DIGEST_NAMES.getOrDefault(algorithm, algorithm);
    }
}
