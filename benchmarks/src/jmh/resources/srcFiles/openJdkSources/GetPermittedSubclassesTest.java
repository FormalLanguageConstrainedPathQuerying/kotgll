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

/*
 * @test
 * @bug 8225056
 * @compile GetPermittedSubclasses.jcod
 * @compile noSubclass/BaseC.java noSubclass/BaseI.java noSubclass/Impl1.java
 * @compile noSubclass/Impl2.java
 * @run main GetPermittedSubclassesTest
 */

import java.util.ArrayList;

public class GetPermittedSubclassesTest {

    sealed class Sealed1 permits Sub1 {}

    final class Sub1 extends Sealed1 implements SealedI1 {}

    sealed interface SealedI1 permits NotSealed, Sub1, Extender {}

    non-sealed interface Extender extends SealedI1 { }

    final class FinalC implements Extender {}

    final class NotSealed implements SealedI1 {}

    final class Final4 {}

    public static void testSealedInfo(Class<?> c, String[] expected, boolean isSealed) {
        var permitted = c.getPermittedSubclasses();

        if (isSealed) {
            if (permitted.length != expected.length) {
                throw new RuntimeException(
                    "Unexpected number of permitted subclasses for: " + c.toString() +
                    "(" + java.util.Arrays.asList(permitted));
            }

            if (!c.isSealed()) {
                throw new RuntimeException("Expected sealed class: " + c.toString());
            }

            ArrayList<String> permittedNames = new ArrayList<String>();
            for (int i = 0; i < permitted.length; i++) {
                permittedNames.add(permitted[i].getName());
            }

            if (permittedNames.size() != expected.length) {
                throw new RuntimeException(
                    "Unexpected number of permitted names for: " + c.toString());
            }

            for (int i = 0; i < expected.length; i++) {
                if (!permittedNames.contains(expected[i])) {
                    throw new RuntimeException(
                         "Expected class not found in permitted subclases list, super class: " +
                         c.getName() + ", expected class: " + expected[i]);
                }
            }
        } else {
            if (c.isSealed() || permitted != null) {
                throw new RuntimeException("Unexpected sealed class: " + c.toString());
            }
        }
    }

    public static void testBadSealedClass(String className,
                                          Class<?> expectedException,
                                          String expectedCFEMessage) throws Throwable {
        try {
            Class.forName(className);
            throw new RuntimeException("Expected ClassFormatError exception not thrown for " + className);
        } catch (ClassFormatError cfe) {
            if (ClassFormatError.class != expectedException) {
                throw new RuntimeException(
                    "Class " + className + " got unexpected exception: " + cfe.getMessage());
            }
            if (!cfe.getMessage().contains(expectedCFEMessage)) {
                throw new RuntimeException(
                    "Class " + className + " got unexpected ClassFormatError exception: " + cfe.getMessage());
            }
        } catch (IncompatibleClassChangeError icce) {
            if (IncompatibleClassChangeError.class != expectedException) {
                throw new RuntimeException(
                    "Class " + className + " got unexpected exception: " + icce.getMessage());
            }
            if (!icce.getMessage().contains(expectedCFEMessage)) {
                throw new RuntimeException(
                    "Class " + className + " got unexpected IncompatibleClassChangeError exception: " + icce.getMessage());
            }
        }
    }

    public static void main(String... args) throws Throwable {
        testSealedInfo(SealedI1.class, new String[] {"GetPermittedSubclassesTest$NotSealed",
                                                     "GetPermittedSubclassesTest$Sub1",
                                                     "GetPermittedSubclassesTest$Extender"},
                                                     true);

        testSealedInfo(Sealed1.class, new String[] {"GetPermittedSubclassesTest$Sub1"}, true);
        testSealedInfo(Final4.class, null, false);
        testSealedInfo(NotSealed.class, null, false);

        testSealedInfo(OldClassFile.class, null, false);

        testSealedInfo(NoSubclasses.class, new String[]{}, true);

        testBadSealedClass("SubClass", IncompatibleClassChangeError.class,
                           "SubClass cannot inherit from sealed class NoSubclasses");

        testSealedInfo(NoLoadSubclasses.class, new String[]{"ExistingClassFile" }, true);

        testBadSealedClass("BadPermittedAttr", ClassFormatError.class,
                           "Permitted subclass class_info_index 15 has bad constant type");

        testBadSealedClass("SealedButFinal", ClassFormatError.class,
                           "PermittedSubclasses attribute in final class");

        testBadSealedClass("BadPermittedSubclassEntry", ClassFormatError.class,
                           "Illegal class name \"iDont;;Exist\" in class file");

        testBadSealedClass("EmptyPermittedSubclassEntry", ClassFormatError.class,
                           "Illegal class name \"\" in class file");

        testSealedInfo(noSubclass.BaseC.class, new String[] {"noSubclass.ImplCIntermediate"}, true);
        testSealedInfo(noSubclass.BaseI.class, new String[] {"noSubclass.ImplIIntermediateI", "noSubclass.ImplIIntermediateC"}, true);
    }
}
