/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8071693
 * @summary Verify that the Introspector finds default methods inherited
 *          from interfaces
 */

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultMethodBeanPropertyTest {


    public interface A1 {
        default int getValue() {
            return 0;
        }
        default Object getObj() {
            return null;
        }

        public static int getStaticValue() {
            return 0;
        }
    }

    public interface B1 extends A1 {
    }

    public interface C1 extends A1 {
        Number getFoo();
    }

    public class D1 implements C1 {
        @Override
        public Integer getFoo() {
            return null;
        }
        @Override
        public Float getObj() {
            return null;
        }
    }

    public static void testScenario1() {
        verifyProperties(D1.class,
            "getClass",     
            "getValue",     
            "getFoo",       
            "getObj"        
        );
    }


    public interface A2 {
        default Object getFoo() {
            return null;
        }
    }

    public interface B2 extends A2 {
    }

    public interface C2 extends A2 {
    }

    public class D2 implements B2, C2 {
    }

    public static void testScenario2() {
        verifyProperties(D2.class,
            "getClass",
            "getFoo"
        );
    }


    public interface A3 {
        default Object getFoo() {
            return null;
        }
    }

    public interface B3 extends A3 {
        @Override
        Set<?> getFoo();
    }

    public interface C3 extends A3 {
        @Override
        Collection<?> getFoo();
    }

    public class D3 implements B3, C3 {
        @Override
        public NavigableSet<?> getFoo() {
            return null;
        }
    }

    public static void testScenario3() {
        verifyProperties(D3.class,
            "getClass",
            "getFoo"
        );
    }


    public static void verifyProperties(Class<?> type, String... getterNames) {

        final HashSet<PropertyDescriptor> expected = new HashSet<>();
        for (String methodName : getterNames) {
            final String suffix = methodName.substring(3);
            final String propName = Introspector.decapitalize(suffix);
            final Method getter;
            try {
                getter = type.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                throw new Error("unexpected error", e);
            }
            final PropertyDescriptor propDesc;
            try {
                propDesc = new PropertyDescriptor(propName, getter, null);
            } catch (IntrospectionException e) {
                throw new Error("unexpected error", e);
            }
            expected.add(propDesc);
        }

        expected.stream()
                .map(PropertyDescriptor::getName)
                .filter(name -> BeanUtils.getPropertyDescriptor(type, name) == null)
                .findFirst()
                .ifPresent(name -> {
                    throw new Error("property \"" + name + "\" not found in " + type);
                });

        final Set<PropertyDescriptor> actual =
                Set.of(BeanUtils.getPropertyDescriptors(type));

        if (!actual.equals(expected)) {
            throw new Error("mismatch: " + type
              + "\nACTUAL:\n  "
              + actual.stream()
                      .map(Object::toString)
                      .collect(Collectors.joining("\n  "))
              + "\nEXPECTED:\n  "
              + expected.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining("\n  ")));
        }
    }


    public static void main(String[] args) throws Exception {
        testScenario1();
        testScenario2();
        testScenario3();
    }
}
