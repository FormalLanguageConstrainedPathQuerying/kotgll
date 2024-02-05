/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary class p1.c1 defined in an unnamed module tries to access p2.c2 defined in m2x.
 *          Access allowed, an unnamed module can read all modules and p2 in module m2x
 *          which is exported unqualifiedly.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile myloaders/MyDiffClassLoader.java
 * @compile p2/c2.java
 * @compile p1/c1.java
 * @run main/othervm -Xbootclasspath/a:. UmodDiffCL_ExpUnqual
 */

import static jdk.test.lib.Asserts.*;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import myloaders.MyDiffClassLoader;

public class UmodDiffCL_ExpUnqual {

    public void createLayerOnBoot() throws Throwable {

        ModuleDescriptor descriptor_m1x =
                ModuleDescriptor.newModule("m1x")
                        .requires("java.base")
                        .requires("m2x")
                        .build();

        ModuleDescriptor descriptor_m2x =
                ModuleDescriptor.newModule("m2x")
                        .requires("java.base")
                        .exports("p2")
                        .build();

        ModuleFinder finder = ModuleLibrary.of(descriptor_m1x, descriptor_m2x);

        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(finder, ModuleFinder.of(), Set.of("m1x"));

        Map<String, ClassLoader> map = new HashMap<>();
        map.put("m1x", MyDiffClassLoader.loader1);
        map.put("m2x", MyDiffClassLoader.loader2);

        ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

        assertTrue(layer.findLoader("m1x") == MyDiffClassLoader.loader1);
        assertTrue(layer.findLoader("m2x") == MyDiffClassLoader.loader2);
        assertTrue(layer.findLoader("java.base") == null);

        Class p1_c1_class = MyDiffClassLoader.loader1.loadClass("p1.c1");
        try {
            p1_c1_class.newInstance();
        } catch (IllegalAccessError e) {
            throw new RuntimeException("Test Failed, p1.c1 defined in unnamed module can access p2.c2 in module m2x");
        }
    }

    public static void main(String args[]) throws Throwable {
      UmodDiffCL_ExpUnqual test = new UmodDiffCL_ExpUnqual();
      test.createLayerOnBoot();
    }
}
