/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import com.sun.net.httpserver.HttpHandler;
import jdk.internal.misc.Unsafe;
import jdk.test.lib.Asserts;
import java.io.InputStream;
import java.net.URL;


public class LoaderConstraintsApp {
    static void defineHttpExchangeWithAppLoader() throws Exception {
        Unsafe unsafe = Unsafe.getUnsafe();
        URL url = new URL("jrt:
        byte[] bytes;
        try (InputStream is = url.openStream()) {
            bytes = is.readAllBytes();
        }
        Class fakeClass = unsafe.defineClass("com/sun/net/httpserver/HttpExchange", bytes, 0, bytes.length,
                                             LoaderConstraintsApp.class.getClassLoader(),
                                             LoaderConstraintsApp.class.getProtectionDomain());
         System.out.println("fake HttpExchange          = " + fakeClass.hashCode());
         System.out.println("fake HttpExchange (loader) = " + fakeClass.getClassLoader());
    }

    static void resolveHttpExchangeInParentLoader(ClassLoader loader) throws Exception {
        Class realClass = Class.forName("com.sun.net.httpserver.HttpExchange", false, loader);
        System.out.println("real HttpExchange          = " + realClass.hashCode());
        System.out.println("real HttpExchange (loader) = " + realClass.getClassLoader());
    }

    static void doTest(int k) throws Exception {
        ClassLoader appLoader =  LoaderConstraintsApp.class.getClassLoader();
        ClassLoader platformLoader = appLoader.getParent();
        if (k == 1) {
           defineHttpExchangeWithAppLoader();
           resolveHttpExchangeInParentLoader(platformLoader);
           try {
               HttpHandler h1 = new MyHttpHandler();
               throw new RuntimeException("Load HttpExchange with platform loader did not fail as expected");
           } catch (LinkageError e) {
               System.out.println("Expected: " + e);
               Asserts.assertTrue(e.getMessage().contains("loader constraint violation in interface itable initialization for class MyHttpHandler"));
               e.printStackTrace(System.out);
           }
        } else if (k == 2) {
            resolveHttpExchangeInParentLoader(platformLoader);

            HttpHandler h2 = new MyHttpHandler();

            try {
                defineHttpExchangeWithAppLoader();
                throw new RuntimeException("defineHttpExchangeWithAppLoader() did not fail as expected");
            } catch (LinkageError e) {
                System.out.println("Expected: " + e);
                e.printStackTrace(System.out);
            }
        } else if (k == 3) {
            resolveHttpExchangeInParentLoader(platformLoader);
            defineHttpExchangeWithAppLoader();

            MyHttpHandlerB.touch();

            MyClassLoader loader = new MyClassLoader(platformLoader, appLoader);
            try {
                Class C = loader.loadClass("MyHttpHandlerC");
                System.out.println("MyHttpHandlerC          = " + C);
                System.out.println("MyHttpHandlerC (loader) = " + C.getClassLoader());

                HttpHandler handlerC = (HttpHandler)C.newInstance();
                try {
                    MyHttpHandlerB.test(handlerC);
                    throw new RuntimeException("MyHttpHandlerB.test() did not fail as expected");
                } catch (LinkageError e) {
                    System.out.println("Expected: " + e);
                    Asserts.assertTrue(e.getMessage().matches(".*constraint violation: when resolving interface method .*.HttpHandler.handle.*"));
                    e.printStackTrace(System.out);
                }
            } catch (Exception e) {
                throw new RuntimeException("Unexpected exception", e);
            }
        } else {
            throw new RuntimeException("Wrong option specified k = " + k);
        }
    }

    public static void main(String... args) throws Throwable {
        if (args.length < 1) {
            throw new RuntimeException("Wrong number of arguments");
        }

        if (args.length >= 2 && "loadClassOnly".equals(args[1])) {
            System.out.println("Loading: " + MyHttpHandler.class);
            System.out.println("Loading: " + MyHttpHandlerB.class);
            System.exit(0);
        }

        int k = Integer.valueOf(args[0]);
        if (k < 1 && k > 3) {
            throw new RuntimeException("Arg is out of range [1,3] k = " + k);
        }

        doTest(k);
    }
}
