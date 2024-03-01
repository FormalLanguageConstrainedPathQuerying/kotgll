/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.jvm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import jdk.jfr.Event;
import jdk.jfr.Registered;
/**
 * @test Tests that reflective access works as (normally) expected
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 *
 * @run main/othervm jdk.jfr.jvm.TestGetEventWriterReflection
 */
public class TestGetEventWriterReflection {

    @Registered(false)
    static class InitializeEvent extends Event {
    }

    public static void main(String... args) throws Throwable {
        testReflectionGetConstructor();
        testReflectionGetDeclaredConstructor();
        testReflectionGetDeclaredConstructorSetAccessible();
        testReflectionGetDeclaredFieldSetAccessible();
    }

    private static void testReflectionGetConstructor() throws Exception {
        try {
            Class<?> c = Class.forName("jdk.jfr.internal.event.EventWriter");
            Constructor<?> constructor = c.getConstructor(new Class[0]);
            throw new RuntimeException("Should not reach here " + constructor);
        } catch (NoSuchMethodException nsme) {
        }
    }

    private static void testReflectionGetDeclaredConstructor() throws Exception {
        try {
            Class<?> c = Class.forName("jdk.jfr.internal.event.EventWriter");
            Constructor<?> constructor = c.getDeclaredConstructor(new Class[0]);
            constructor.newInstance();
            throw new RuntimeException("Should not reach here " + constructor);
        } catch (IllegalAccessException iae) {
            if (iae.getMessage().contains("""
                cannot access a member of class jdk.jfr.internal.event.EventWriter
                (in module jdk.jfr) with modifiers \"private\"
                                         """)) {
            }
        }
    }

    private static void testReflectionGetDeclaredConstructorSetAccessible() throws Exception {
        try {
            Class<?> c = Class.forName("jdk.jfr.internal.event.EventWriter");
            Constructor<?> constructor = c.getDeclaredConstructor(new Class[0]);
            constructor.setAccessible(true);
            throw new RuntimeException("Should not reach here " + constructor);
        } catch (InaccessibleObjectException ioe) {
            if (ioe.getMessage().contains("module jdk.jfr does not \"opens jdk.jfr.internal.event")) {
            }
        }
    }

    private static void testReflectionGetDeclaredFieldSetAccessible() throws Exception {
        try {
            Class<?> c = Class.forName("jdk.jfr.internal.event.EventWriter");
            Field field = c.getDeclaredField("unsafe");
            field.setAccessible(true);
            throw new RuntimeException("Should not reach here " + field);
        } catch (InaccessibleObjectException ioe) {
            if (ioe.getMessage().contains("module jdk.jfr does not \"opens jdk.jfr.internal.event")) {
            }
        }
    }
}
