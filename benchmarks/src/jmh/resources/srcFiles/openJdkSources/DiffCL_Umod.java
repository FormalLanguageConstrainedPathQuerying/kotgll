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
 * @summary class p1.c1 defined in m1x tries to access p2.c2 defined in unnamed module.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.module
 * @compile myloaders/MyDiffClassLoader.java
 * @compile p2/c2.java
 * @compile p1/c1.java
 * @compile p1/c1ReadEdgeDiffLoader.java
 * @compile p1/c1Loose.java
 * @run main/othervm -Xbootclasspath/a:. DiffCL_Umod
 */

import static jdk.test.lib.Asserts.*;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import myloaders.MyDiffClassLoader;

public class DiffCL_Umod {


 public void test_strictModuleLayer() throws Throwable {

     ModuleDescriptor descriptor_m1x =
             ModuleDescriptor.newModule("m1x")
                     .requires("java.base")
                     .exports("p1")
                     .build();

     ModuleFinder finder = ModuleLibrary.of(descriptor_m1x);

     Configuration cf = ModuleLayer.boot()
             .configuration()
             .resolve(finder, ModuleFinder.of(), Set.of("m1x"));

     MyDiffClassLoader.loader1 = new MyDiffClassLoader();
     MyDiffClassLoader.loader2 = new MyDiffClassLoader();

     Map<String, ClassLoader> map = new HashMap<>();
     map.put("m1x", MyDiffClassLoader.loader1);

     ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

     assertTrue(layer.findLoader("m1x") == MyDiffClassLoader.loader1);
     assertTrue(layer.findLoader("java.base") == null);

     Class p1_c1_class = MyDiffClassLoader.loader1.loadClass("p1.c1");

     try {
         p1_c1_class.newInstance();
         throw new RuntimeException("Test Failed, strict module m1x should not be able " +
                                    "to access public type p2.c2 defined in unnamed module");
     } catch (IllegalAccessError e) {
     }
}

 public void test_strictModuleUnnamedReadableLayer() throws Throwable {

     ModuleDescriptor descriptor_m1x =
             ModuleDescriptor.newModule("m1x")
                     .requires("java.base")
                     .exports("p1")
                     .build();

     ModuleFinder finder = ModuleLibrary.of(descriptor_m1x);

     Configuration cf = ModuleLayer.boot()
             .configuration()
             .resolve(finder, ModuleFinder.of(), Set.of("m1x"));

     MyDiffClassLoader.loader1 = new MyDiffClassLoader();
     MyDiffClassLoader.loader2 = new MyDiffClassLoader();

     Map<String, ClassLoader> map = new HashMap<>();
     map.put("m1x", MyDiffClassLoader.loader1);

     ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

     assertTrue(layer.findLoader("m1x") == MyDiffClassLoader.loader1);
     assertTrue(layer.findLoader("java.base") == null);

     Class p1_c1_class = MyDiffClassLoader.loader1.loadClass("p1.c1ReadEdgeDiffLoader");

     try {
        p1_c1_class.newInstance();
     } catch (IllegalAccessError e) {
         throw new RuntimeException("Test Failed, module m1x has established readability to p2/c2 loader's " +
                                    "unnamed module, access should be allowed: " + e.getMessage());
     }
 }

 public void test_looseModuleLayer() throws Throwable {

     ModuleDescriptor descriptor_m1x =
             ModuleDescriptor.newModule("m1x")
                     .requires("java.base")
                     .exports("p1")
                     .build();

     ModuleFinder finder = ModuleLibrary.of(descriptor_m1x);

     Configuration cf = ModuleLayer.boot()
             .configuration()
             .resolve(finder, ModuleFinder.of(), Set.of("m1x"));

     MyDiffClassLoader.loader1 = new MyDiffClassLoader();
     MyDiffClassLoader.loader2 = new MyDiffClassLoader();

     Map<String, ClassLoader> map = new HashMap<>();
     map.put("m1x", MyDiffClassLoader.loader1);

     ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

     assertTrue(layer.findLoader("m1x") == MyDiffClassLoader.loader1);
     assertTrue(layer.findLoader("java.base") == null);

     Class p1_c1_class = MyDiffClassLoader.loader1.loadClass("p1.c1Loose");

     Module m1x = layer.findModule("m1x").get();
     jdk.internal.module.Modules.addReadsAllUnnamed(m1x);

     try {
         p1_c1_class.newInstance();
     } catch (IllegalAccessError e) {
         throw new RuntimeException("Test Failed, loose module m1x should be able to access " +
                                    "public type p2.c2 defined in unnamed module: " + e.getMessage());
     }
 }

 public static void main(String args[]) throws Throwable {
   DiffCL_Umod test = new DiffCL_Umod();
   test.test_strictModuleLayer();                
   test.test_strictModuleUnnamedReadableLayer(); 
   test.test_looseModuleLayer();                 
 }
}
