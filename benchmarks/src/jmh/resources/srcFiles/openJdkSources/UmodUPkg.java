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
 * @summary class p3.c3 defined in module m1x tries to access c4 defined in unnamed module.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile myloaders/MySameClassLoader.java
 * @compile c4.java
 * @compile p3/c3.jcod
 * @compile p3/c3ReadEdge.jcod
 * @run main/othervm -Xbootclasspath/a:. UmodUPkg
 */

import static jdk.test.lib.Asserts.*;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import myloaders.MySameClassLoader;

public class UmodUPkg {


 public void test_strictModuleLayer() throws Throwable {

     ModuleDescriptor descriptor_m1x =
             ModuleDescriptor.newModule("m1x")
                     .requires("java.base")
                     .exports("p3")
                     .build();

     ModuleFinder finder = ModuleLibrary.of(descriptor_m1x);

     Configuration cf = ModuleLayer.boot()
             .configuration()
             .resolve(finder, ModuleFinder.of(), Set.of("m1x"));

     MySameClassLoader loader = new MySameClassLoader();
     Map<String, ClassLoader> map = new HashMap<>();
     map.put("m1x", loader);

     ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

     assertTrue(layer.findLoader("m1x") == loader);
     assertTrue(layer.findLoader("java.base") == null);

     Class p3_c3_class = loader.loadClass("p3.c3");

     try {
         p3_c3_class.newInstance();
         throw new RuntimeException("Test Failed, strict module m1x, type p3.c3, should not be able to access " +
                                    "public type c4 defined in unnamed module");
     } catch (IllegalAccessError e) {
     }
 }

 public void test_strictModuleUnnamedReadableLayer() throws Throwable {

     ModuleDescriptor descriptor_m1x =
             ModuleDescriptor.newModule("m1x")
                     .requires("java.base")
                     .exports("p3")
                     .build();

     ModuleFinder finder = ModuleLibrary.of(descriptor_m1x);

     Configuration cf = ModuleLayer.boot()
             .configuration()
             .resolve(finder, ModuleFinder.of(), Set.of("m1x"));

     MySameClassLoader loader = new MySameClassLoader();
     Map<String, ClassLoader> map = new HashMap<>();
     map.put("m1x", loader);

     ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

     assertTrue(layer.findLoader("m1x") == loader);
     assertTrue(layer.findLoader("java.base") == null);

     Class p3_c3_class = loader.loadClass("p3.c3ReadEdge");

     try {
        p3_c3_class.newInstance();
     } catch (IllegalAccessError e) {
         throw new RuntimeException("Test Failed, module m1x, type p3.c3ReadEdge, has established readability to " +
                                    "c4 loader's unnamed module, access should be allowed: " + e.getMessage());
     }
 }

 public static void main(String args[]) throws Throwable {
   UmodUPkg test = new UmodUPkg();
   test.test_strictModuleLayer();                
   test.test_strictModuleUnnamedReadableLayer(); 
 }
}
