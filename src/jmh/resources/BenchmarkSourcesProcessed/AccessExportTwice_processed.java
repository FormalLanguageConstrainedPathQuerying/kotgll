/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Class p1.c1 in an unnamed module cannot read p2.c2 in module second_mod,
 *          even after p2 is exported to all unnamed. Ensures constant
 *          access check answers when not accessible due to exportedness.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.module
 * @compile myloaders/MySameClassLoader.java
 * @compile p2/c2.java
 * @compile p1/c1.java
 * @run main/othervm AccessExportTwice
 */

import static jdk.test.lib.Asserts.*;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import myloaders.MySameClassLoader;


public class AccessExportTwice {

    public void createLayerOnBoot() throws Throwable {

        ModuleDescriptor descriptor_first_mod =
                ModuleDescriptor.newModule("first_mod")
                        .requires("java.base")
                        .requires("second_mod")
                        .build();

        ModuleDescriptor descriptor_second_mod =
                ModuleDescriptor.newModule("second_mod")
                        .requires("java.base")
                        .exports("p2", Set.of("first_mod"))
                        .build();

        ModuleFinder finder = ModuleLibrary.of(descriptor_first_mod, descriptor_second_mod);

        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(finder, ModuleFinder.of(), Set.of("first_mod"));

        Map<String, ClassLoader> map = new HashMap<>();
        map.put("first_mod", MySameClassLoader.loader1);
        map.put("second_mod", MySameClassLoader.loader1);

        ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

        assertTrue(layer.findLoader("first_mod") == MySameClassLoader.loader1);
        assertTrue(layer.findLoader("second_mod") == MySameClassLoader.loader1);
        assertTrue(layer.findLoader("java.base") == null);

        Class p2_c2_class = MySameClassLoader.loader1.loadClass("p2.c2");
        Class p1_c1_class = MySameClassLoader.loader1.loadClass("p1.c1");
        try {
            p1_c1_class.newInstance();
            throw new RuntimeException("Test Failed, the unnamed module should not have access to public type p2.c2");
        } catch (IllegalAccessError e) {
            String message = e.getMessage();
            if (!(message.contains("cannot access") &&
                  message.contains("because module second_mod does not export p2 to unnamed module"))) {
                throw new RuntimeException("Wrong message: " + message);
            } else {
                System.out.println("Test Succeeded at attempt #1");
            }
        }

        Module second_mod = p2_c2_class.getModule();
        jdk.internal.module.Modules.addExportsToAllUnnamed(second_mod, "p2");

        try {
            p1_c1_class.newInstance();
            throw new RuntimeException("Test Failed, access should have been cached above");
        } catch (IllegalAccessError e) {
            String message = e.getMessage();
            if (!(message.contains("cannot access") &&
                  message.contains("because module second_mod does not export p2 to unnamed module"))) {
                throw new RuntimeException("Wrong message: " + message);
            } else {
                System.out.println("Test Succeeded at attempt #2");
            }
        }
    }

    public static void main(String args[]) throws Throwable {
      AccessExportTwice test = new AccessExportTwice();
      test.createLayerOnBoot();
    }
}
