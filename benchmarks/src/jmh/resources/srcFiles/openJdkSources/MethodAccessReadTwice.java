/*
 Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8174954
 * @summary Test that invokedynamic instructions, that initially throw IAE exceptions
 *          because of a missing module read edge, behave correctly when executed
 *          after the module read edge is added.
 * @compile ModuleLibrary.java
 *          p2/c2.java
 *          p5/c5.jasm
 *          p7/c7.jasm
 * @run main/othervm MethodAccessReadTwice
 */

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.ModuleLayer;
import java.lang.Module;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class MethodAccessReadTwice {

    public void createLayerOnBoot() throws Throwable {

        ModuleDescriptor descriptor_first_mod =
                ModuleDescriptor.newModule("first_mod")
                        .requires("java.base")
                        .exports("p5")
                        .build();

        ModuleDescriptor descriptor_second_mod =
                ModuleDescriptor.newModule("second_mod")
                        .requires("java.base")
                        .exports("p2")
                        .build();

        ModuleDescriptor descriptor_third_mod =
                ModuleDescriptor.newModule("third_mod")
                        .requires("java.base")
                        .exports("p7")
                        .build();

        ModuleFinder finder = ModuleLibrary.of(descriptor_first_mod,
                                               descriptor_second_mod,
                                               descriptor_third_mod);

        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(finder, ModuleFinder.of(),
                         Set.of("first_mod", "second_mod", "third_mod"));

        Map<String, ClassLoader> map = new HashMap<>();
        ClassLoader loader = MethodAccessReadTwice.class.getClassLoader();
        map.put("first_mod", loader);
        map.put("second_mod", loader);
        map.put("third_mod", loader);

        ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

        Class p2_c2_class = loader.loadClass("p2.c2");
        Class p5_c5_class = loader.loadClass("p5.c5");
        Class p7_c7_class = loader.loadClass("p7.c7");

        Module first_mod = p5_c5_class.getModule();
        Module second_mod = p2_c2_class.getModule();
        Module third_mod = p7_c7_class.getModule();

        p5.c5 c5_obj = new p5.c5();
        p2.c2 c2_obj = new p2.c2();
        p7.c7 c7_obj = new p7.c7();


        try {
            c5_obj.method5(c2_obj);
            throw new RuntimeException("Test Failed, module first_mod should not have access to p2.c2");
        } catch (IllegalAccessError e) {
            String message = e.getMessage();
            if (!(message.contains("cannot access") &&
                  message.contains("because module first_mod does not read module second_mod"))) {
                throw new RuntimeException("Wrong message: " + message);
            } else {
                System.out.println("Test Succeeded at attempt #1");
            }
        }

        c5_obj.methodAddReadEdge(p2_c2_class.getModule());
        try {
            c5_obj.method5(c2_obj); 
            throw new RuntimeException("Test Failed, access should have been cached above");
        } catch (IllegalAccessError e) {
            String message = e.getMessage();
            if (!(message.contains("cannot access") &&
                  message.contains("because module first_mod does not read module second_mod"))) {
                throw new RuntimeException("Wrong message: " + message);
            } else {
                System.out.println("Test Succeeded at attempt #2");
            }
        }


        c7_obj.method7(c2_obj, second_mod); 
    }

    public static void main(String args[]) throws Throwable {
      MethodAccessReadTwice test = new MethodAccessReadTwice();
      test.createLayerOnBoot();
    }
}
