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

package sun.security.jca;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;

import java.security.*;

import sun.security.util.PropertyExpander;

/**
 * Class representing a configured provider which encapsulates configuration
 * (provider name + optional argument), the provider loading logic, and
 * the loaded Provider object itself.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class ProviderConfig {

    private static final sun.security.util.Debug debug =
        sun.security.util.Debug.getInstance("jca", "ProviderConfig");

    private static final String P11_SOL_NAME = "SunPKCS11";

    private static final String P11_SOL_ARG  =
        "${java.home}/conf/security/sunpkcs11-solaris.cfg";

    private static final int MAX_LOAD_TRIES = 30;

    private final String provName;

    private final String argument;

    private int tries;

    private volatile Provider provider;

    private boolean isLoading;

    ProviderConfig(String provName, String argument) {
        if (provName.endsWith(P11_SOL_NAME) && argument.equals(P11_SOL_ARG)) {
            checkSunPKCS11Solaris();
        }
        this.provName = provName;
        this.argument = expand(argument);
    }

    ProviderConfig(String provName) {
        this(provName, "");
    }

    ProviderConfig(Provider provider) {
        this.provName = provider.getName();
        this.argument = "";
        this.provider = provider;
    }

    private void checkSunPKCS11Solaris() {
        @SuppressWarnings("removal")
        Boolean o = AccessController.doPrivileged(
                                new PrivilegedAction<Boolean>() {
            public Boolean run() {
                File file = new File("/usr/lib/libpkcs11.so");
                if (file.exists() == false) {
                    return Boolean.FALSE;
                }
                if ("false".equalsIgnoreCase(System.getProperty
                        ("sun.security.pkcs11.enable-solaris"))) {
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
        });
        if (o == Boolean.FALSE) {
            tries = MAX_LOAD_TRIES;
        }
    }

    private boolean hasArgument() {
        return !argument.isEmpty();
    }

    private boolean shouldLoad() {
        return (tries < MAX_LOAD_TRIES);
    }

    private void disableLoad() {
        tries = MAX_LOAD_TRIES;
    }

    boolean isLoaded() {
        return (provider != null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ProviderConfig other)) {
            return false;
        }
        return this.provName.equals(other.provName)
            && this.argument.equals(other.argument);

    }

    @Override
    public int hashCode() {
        return Objects.hash(provName, argument);
    }

    public String toString() {
        if (hasArgument()) {
            return provName + "('" + argument + "')";
        } else {
            return provName;
        }
    }

    /**
     * Get the provider object. Loads the provider if it is not already loaded.
     */
    @SuppressWarnings("deprecation")
    Provider getProvider() {
        Provider p = provider;
        if (p != null) {
            return p;
        }
        synchronized (this) {
            p = provider;
            if (p != null) {
                return p;
            }
            if (!shouldLoad()) {
                return null;
            }

            p = switch (provName) {
                case "SUN", "sun.security.provider.Sun" ->
                    new sun.security.provider.Sun();
                case "SunRsaSign", "sun.security.rsa.SunRsaSign" ->
                    new sun.security.rsa.SunRsaSign();
                case "SunJCE", "com.sun.crypto.provider.SunJCE" ->
                    new com.sun.crypto.provider.SunJCE();
                case "SunJSSE" -> new sun.security.ssl.SunJSSE();
                case "SunEC" -> new sun.security.ec.SunEC();
                case "Apple", "apple.security.AppleProvider" -> {
                    @SuppressWarnings("removal")
                    var tmp = AccessController.doPrivileged(
                        new PrivilegedAction<Provider>() {
                            public Provider run() {
                                try {
                                    Class<?> c = Class.forName(
                                        "apple.security.AppleProvider");
                                    if (Provider.class.isAssignableFrom(c)) {
                                        @SuppressWarnings("deprecation")
                                        Object tmp = c.newInstance();
                                        return (Provider) tmp;
                                    }
                                } catch (Exception ex) {
                                    if (debug != null) {
                                        debug.println("Error loading provider Apple");
                                        ex.printStackTrace();
                                    }
                                }
                                return null;
                            }
                        });
                    yield tmp;
                }
                default -> {
                    if (isLoading) {
                        if (debug != null) {
                            debug.println("Recursion loading provider: " + this);
                            new Exception("Call trace").printStackTrace();
                        }
                        yield null;
                    }
                    try {
                        isLoading = true;
                        tries++;
                        yield doLoadProvider();
                    } finally {
                        isLoading = false;
                    }
                }
            };
            provider = p;
        }
        return p;
    }

    /**
     * Load and instantiate the Provider described by this class.
     *
     * NOTE use of doPrivileged().
     *
     * @return null if the Provider could not be loaded
     *
     * @throws ProviderException if executing the Provider's constructor
     * throws a ProviderException. All other Exceptions are ignored.
     */
    @SuppressWarnings("removal")
    private Provider doLoadProvider() {
        return AccessController.doPrivileged(new PrivilegedAction<Provider>() {
            public Provider run() {
                if (debug != null) {
                    debug.println("Loading provider " + ProviderConfig.this);
                }
                try {
                    Provider p = ProviderLoader.INSTANCE.load(provName);
                    if (p != null) {
                        if (hasArgument()) {
                            p = p.configure(argument);
                        }
                        if (debug != null) {
                            debug.println("Loaded provider " + p.getName());
                        }
                    } else {
                        if (debug != null) {
                            debug.println("Error loading provider " +
                                ProviderConfig.this);
                        }
                        disableLoad();
                    }
                    return p;
                } catch (Exception e) {
                    if (e instanceof ProviderException) {
                        throw e;
                    } else {
                        if (debug != null) {
                            debug.println("Error loading provider " +
                                ProviderConfig.this);
                            e.printStackTrace();
                        }
                        disableLoad();
                        return null;
                    }
                } catch (ExceptionInInitializerError err) {
                    if (debug != null) {
                        debug.println("Error loading provider " + ProviderConfig.this);
                        err.printStackTrace();
                    }
                    disableLoad();
                    return null;
                }
            }
        });
    }

    /**
     * Perform property expansion of the provider value.
     *
     * NOTE use of doPrivileged().
     */
    @SuppressWarnings("removal")
    private static String expand(final String value) {
        if (value.contains("${") == false) {
            return value;
        }
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                try {
                    return PropertyExpander.expand(value);
                } catch (GeneralSecurityException e) {
                    throw new ProviderException(e);
                }
            }
        });
    }

    private static final class ProviderLoader {
        static final ProviderLoader INSTANCE = new ProviderLoader();

        private final ServiceLoader<Provider> services;

        private ProviderLoader() {
            services = ServiceLoader.load(java.security.Provider.class,
                                          ClassLoader.getSystemClassLoader());
        }

        /**
         * Loads the provider with the specified class name.
         *
         * @param pn the name of the provider
         * @return the Provider, or null if it cannot be found or loaded
         * @throws ProviderException all other exceptions are ignored
         */
        public Provider load(String pn) {
            if (debug != null) {
                debug.println("Attempt to load " + pn + " using SL");
            }
            Iterator<Provider> iter = services.iterator();
            while (iter.hasNext()) {
                try {
                    Provider p = iter.next();
                    String pName = p.getName();
                    if (debug != null) {
                        debug.println("Found SL Provider named " + pName);
                    }
                    if (pName.equals(pn)) {
                        return p;
                    }
                } catch (SecurityException | ServiceConfigurationError |
                         InvalidParameterException ex) {
                    if (debug != null) {
                        debug.println("Encountered " + ex +
                            " while iterating through SL, ignore and move on");
                            ex.printStackTrace();
                    }
                }
            }
            try {
                return legacyLoad(pn);
            } catch (ProviderException pe) {
                throw pe;
            } catch (Exception ex) {
                if (debug != null) {
                    debug.println("Encountered " + ex +
                        " during legacy load of " + pn);
                        ex.printStackTrace();
                }
                return null;
            }
        }

        private Provider legacyLoad(String classname) {

            if (debug != null) {
                debug.println("Loading legacy provider: " + classname);
            }

            try {
                Class<?> provClass =
                    ClassLoader.getSystemClassLoader().loadClass(classname);

                if (!Provider.class.isAssignableFrom(provClass)) {
                    if (debug != null) {
                        debug.println(classname + " is not a provider");
                    }
                    return null;
                }

                @SuppressWarnings("removal")
                Provider p = AccessController.doPrivileged
                    (new PrivilegedExceptionAction<Provider>() {
                    @SuppressWarnings("deprecation") 
                    public Provider run() throws Exception {
                        return (Provider) provClass.newInstance();
                    }
                });
                return p;
            } catch (Exception e) {
                Throwable t;
                if (e instanceof InvocationTargetException) {
                    t = e.getCause();
                } else {
                    t = e;
                }
                if (debug != null) {
                    debug.println("Error loading legacy provider " + classname);
                    t.printStackTrace();
                }
                if (t instanceof ProviderException) {
                    throw (ProviderException) t;
                }
                return null;
            } catch (ExceptionInInitializerError | NoClassDefFoundError err) {
                if (debug != null) {
                    debug.println("Error loading legacy provider " + classname);
                    err.printStackTrace();
                }
                return null;
            }
        }
    }
}
