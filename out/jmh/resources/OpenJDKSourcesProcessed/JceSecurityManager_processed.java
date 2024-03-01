/*
 * Copyright (c) 1999, 2023, Oracle and/or its affiliates. All rights reserved.
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

package javax.crypto;

import java.security.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.lang.StackWalker.*;

/**
 * The JCE security manager.
 *
 * <p>The JCE security manager is responsible for determining the maximum
 * allowable cryptographic strength for a given applet/application, for a given
 * algorithm, by consulting the configured jurisdiction policy files and
 * the cryptographic permissions bundled with the applet/application.
 *
 * @author Jan Luehe
 *
 * @since 1.4
 */
final class JceSecurityManager {

    private static final CryptoPermissions defaultPolicy;
    private static final CryptoPermissions exemptPolicy;
    private static final CryptoAllPermission allPerm;
    private static final Vector<Class<?>> TrustedCallersCache =
            new Vector<>(2);
    private static final ConcurrentMap<URL,CryptoPermissions> exemptCache =
            new ConcurrentHashMap<>();
    private static final CryptoPermissions CACHE_NULL_MARK =
            new CryptoPermissions();

    static final JceSecurityManager INSTANCE;
    static final StackWalker WALKER;

    static {
        defaultPolicy = JceSecurity.getDefaultPolicy();
        exemptPolicy = JceSecurity.getExemptPolicy();
        allPerm = CryptoAllPermission.INSTANCE;

        PrivilegedAction<JceSecurityManager> paSM = JceSecurityManager::new;
        @SuppressWarnings("removal")
        JceSecurityManager dummySecurityManager =
                AccessController.doPrivileged(paSM);
        INSTANCE = dummySecurityManager;

        PrivilegedAction<StackWalker> paWalker =
                () -> StackWalker.getInstance(Set.of(Option.DROP_METHOD_INFO, Option.RETAIN_CLASS_REFERENCE));
        @SuppressWarnings("removal")
        StackWalker dummyWalker = AccessController.doPrivileged(paWalker);

        WALKER = dummyWalker;
    }

    private JceSecurityManager() {
    }

    /**
     * Returns the maximum allowable crypto strength for the given
     * applet/application, for the given algorithm.
     */
    CryptoPermission getCryptoPermission(String theAlg) {

        final String alg = theAlg.toUpperCase(Locale.ENGLISH);

        CryptoPermission defaultPerm = getDefaultPermission(alg);
        if (defaultPerm == CryptoAllPermission.INSTANCE) {
            return defaultPerm;
        }

        return WALKER.walk(s -> s.map(StackFrame::getDeclaringClass)
                .filter(c -> !c.getPackageName().equals("javax.crypto"))
                .map(cls -> {
                    URL callerCodeBase = JceSecurity.getCodeBase(cls);
                    return (callerCodeBase != null) ?
                            getCryptoPermissionFromURL(callerCodeBase,
                                    alg, defaultPerm) : defaultPerm;})
                .findFirst().get()         
        );
    }

    CryptoPermission getCryptoPermissionFromURL(URL callerCodeBase,
            String alg, CryptoPermission defaultPerm) {
        CryptoPermissions appPerms = exemptCache.get(callerCodeBase);
        if (appPerms == null) {
            synchronized (this.getClass()) {
                appPerms = exemptCache.get(callerCodeBase);
                if (appPerms == null) {
                    appPerms = getAppPermissions(callerCodeBase);
                    exemptCache.putIfAbsent(callerCodeBase,
                        (appPerms == null? CACHE_NULL_MARK:appPerms));
                }
            }
        }
        if (appPerms == null || appPerms == CACHE_NULL_MARK) {
            return defaultPerm;
        }

        if (appPerms.implies(allPerm)) {
            return allPerm;
        }

        PermissionCollection appPc = appPerms.getPermissionCollection(alg);
        if (appPc == null) {
            return defaultPerm;
        }
        Enumeration<Permission> enum_ = appPc.elements();
        while (enum_.hasMoreElements()) {
            CryptoPermission cp = (CryptoPermission)enum_.nextElement();
            if (cp.getExemptionMechanism() == null) {
                return cp;
            }
        }

        PermissionCollection exemptPc =
            exemptPolicy.getPermissionCollection(alg);
        if (exemptPc == null) {
            return defaultPerm;
        }

        enum_ = exemptPc.elements();
        while (enum_.hasMoreElements()) {
            CryptoPermission cp = (CryptoPermission)enum_.nextElement();
            try {
                ExemptionMechanism.getInstance(cp.getExemptionMechanism());
                if (cp.getAlgorithm().equals(
                                      CryptoPermission.ALG_NAME_WILDCARD)) {
                    CryptoPermission newCp;
                    if (cp.getCheckParam()) {
                        newCp = new CryptoPermission(
                                alg, cp.getMaxKeySize(),
                                cp.getAlgorithmParameterSpec(),
                                cp.getExemptionMechanism());
                    } else {
                        newCp = new CryptoPermission(
                                alg, cp.getMaxKeySize(),
                                cp.getExemptionMechanism());
                    }
                    if (appPerms.implies(newCp)) {
                        return newCp;
                    }
                }

                if (appPerms.implies(cp)) {
                    return cp;
                }
            } catch (Exception e) {
            }
        }
        return defaultPerm;
    }

    private static CryptoPermissions getAppPermissions(URL callerCodeBase) {
        try {
            return JceSecurity.verifyExemptJar(callerCodeBase);
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * Returns the default permission for the given algorithm.
     */
    private CryptoPermission getDefaultPermission(String alg) {
        Enumeration<Permission> enum_ =
            defaultPolicy.getPermissionCollection(alg).elements();
        return (CryptoPermission)enum_.nextElement();
    }

    boolean isCallerTrusted(Class<?> caller, Provider provider) {
        if (caller != null) {
            URL callerCodeBase = JceSecurity.getCodeBase(caller);
            if (callerCodeBase == null) {
                return true;
            }
            if (TrustedCallersCache.contains(caller)) {
                return true;
            }

            Class<?> pCls = provider.getClass();
            Module pMod = pCls.getModule();
            boolean sameOrigin = (pMod.isNamed()?
                caller.getModule().equals(pMod) :
                callerCodeBase.equals(JceSecurity.getCodeBase(pCls)));
            if (sameOrigin) {
                if (ProviderVerifier.isTrustedCryptoProvider(provider)) {
                    TrustedCallersCache.addElement(caller);
                    return true;
                }
            } else {
                provider = null;
            }

            try {
                JceSecurity.verifyProvider(callerCodeBase, provider);
            } catch (Exception e2) {
                return false;
            }
            TrustedCallersCache.addElement(caller);
            return true;
        }
        return false;
    }
}
