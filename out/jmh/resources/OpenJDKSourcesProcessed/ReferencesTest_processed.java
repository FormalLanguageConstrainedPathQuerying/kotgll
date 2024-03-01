/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
/*
 * @test
 * @bug 4405807
 * @modules java.base/java.util:open
 * @run main/othervm -Xms10m ReferencesTest
 * @summary Verify that references from ResourceBundle cache don't prevent
 * class loader reclamation.
 */

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This test relies on the current behavior of the garbage collector and is
 * therefore no clear indicator of whether the fix for 4405807 works.
 * If the test fails, it might indicate a regression, or it might just mean
 * that a less aggressive garbage collector is used.
 */
public class ReferencesTest {

    private static final int CLASS_LOADER_COUNT = 20;

    private static ClassLoader[] loaders = new ClassLoader[CLASS_LOADER_COUNT];
    private static WeakReference[] weakLoaders = new WeakReference[CLASS_LOADER_COUNT];

    public static void main(String[] args) throws Exception {

        URL testDirectory = new File(System.getProperty("test.classes", ".")).toURL();

        for (int i = 0; i < loaders.length; i++) {
            URL[] urls = { testDirectory };
            loaders[i] = new URLClassLoader(urls);
            weakLoaders[i] = new WeakReference(loaders[i]);
        }

        loadBundles(0, CLASS_LOADER_COUNT / 2);

        report("After loading resource bundles for first half of class loaders: ");

        for (int i = 0; i < CLASS_LOADER_COUNT / 2; i++) {
            loaders[i] = null;
        }

        System.gc();

        report("After releasing first half of class loaders: ");

        loadBundles(CLASS_LOADER_COUNT / 2, CLASS_LOADER_COUNT);

        report("After loading resource bundles for second half of class loaders: ");

        for (int i = CLASS_LOADER_COUNT / 2; i < CLASS_LOADER_COUNT; i++) {
            loaders[i] = null;
        }

        System.gc();

        report("After releasing second half of class loaders: ");

        if (countLoaders(0, CLASS_LOADER_COUNT / 2) > 0) {
            throw new RuntimeException("Too many class loaders not reclaimed yet.");
        }
    }

    private static void report(String when) throws Exception {
        int first = countLoaders(0, CLASS_LOADER_COUNT / 2);
        int second = countLoaders(CLASS_LOADER_COUNT / 2, CLASS_LOADER_COUNT);

        Class clazz = ResourceBundle.class;
        Field cacheList = clazz.getDeclaredField("cacheList");
        cacheList.setAccessible(true);
        int cacheSize = ((Map)cacheList.get(clazz)).size();

        System.out.println(when);
        System.out.println("    " + first + " loaders alive in first half");
        System.out.println("    " + second + " loaders alive in second half");
        System.out.println("    " + cacheSize + " entries in resource bundle cache");
    }

    private static void loadBundles(int start, int end) throws Exception {
        for (int i = start; i < end; i++) {
            try {
                ResourceBundle.getBundle("NonExistantBundle", Locale.US, loaders[i]);
            } catch (MissingResourceException e) {
            }
            ResourceBundle.getBundle("ReferencesTestBundle", Locale.US, loaders[i]);
        }
    }

    private static int countLoaders(int start, int end) {
        int count = 0;
        for (int i = start; i < end; i++) {
            if (weakLoaders[i].get() != null) {
                count++;
            }
        }
        return count;
    }
}
