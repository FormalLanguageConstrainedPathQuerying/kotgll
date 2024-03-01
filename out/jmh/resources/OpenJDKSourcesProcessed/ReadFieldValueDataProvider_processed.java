/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.vm.ci.hotspot.test;

import jdk.vm.ci.meta.JavaConstant;
import org.testng.annotations.DataProvider;

import java.util.LinkedList;

import static jdk.vm.ci.hotspot.test.TestHelper.ARRAYS_MAP;
import static jdk.vm.ci.hotspot.test.TestHelper.ARRAY_ARRAYS_MAP;
import static jdk.vm.ci.hotspot.test.TestHelper.CONSTANT_REFLECTION_PROVIDER;
import static jdk.vm.ci.hotspot.test.TestHelper.DUMMY_CLASS_CONSTANT;
import static jdk.vm.ci.hotspot.test.TestHelper.DUMMY_CLASS_INSTANCE;
import static jdk.vm.ci.hotspot.test.TestHelper.INSTANCE_FIELDS_MAP;
import static jdk.vm.ci.hotspot.test.TestHelper.INSTANCE_STABLE_FIELDS_MAP;
import static jdk.vm.ci.hotspot.test.TestHelper.STABLE_ARRAYS_MAP;
import static jdk.vm.ci.hotspot.test.TestHelper.STABLE_ARRAY_ARRAYS_MAP;
import static jdk.vm.ci.hotspot.test.TestHelper.STATIC_FIELDS_MAP;
import static jdk.vm.ci.hotspot.test.TestHelper.STATIC_STABLE_FIELDS_MAP;

public class ReadFieldValueDataProvider {

    @DataProvider(name = "readFieldValueDataProvider")
    public static Object[][] readFieldValueDataProvider() {
        LinkedList<Object[]> cfgSet = new LinkedList<>();
        INSTANCE_FIELDS_MAP.entrySet().stream().forEach((instanceField) -> {
            cfgSet.add(new Object[]{instanceField.getKey(),
                            DUMMY_CLASS_CONSTANT,
                            instanceField.getValue()});
        });
        STATIC_FIELDS_MAP.entrySet().stream().forEach((staticField) -> {
            cfgSet.add(new Object[]{staticField.getKey(), null, staticField.getValue()});
        });
        STATIC_FIELDS_MAP.entrySet().stream().forEach((staticField) -> {
            cfgSet.add(new Object[]{staticField.getKey(),
                            JavaConstant.NULL_POINTER,
                            staticField.getValue()});
        });
        INSTANCE_STABLE_FIELDS_MAP.entrySet().stream().forEach((instanceField) -> {
            cfgSet.add(new Object[]{instanceField.getKey(),
                            DUMMY_CLASS_CONSTANT,
                            instanceField.getValue()});
        });
        STATIC_STABLE_FIELDS_MAP.entrySet().stream().forEach((staticField) -> {
            cfgSet.add(new Object[]{staticField.getKey(), null, staticField.getValue()});
        });
        STATIC_STABLE_FIELDS_MAP.entrySet().stream().forEach((staticField) -> {
            cfgSet.add(new Object[]{staticField.getKey(),
                            JavaConstant.NULL_POINTER,
                            staticField.getValue()});
        });
        ARRAYS_MAP.entrySet().stream().forEach((instanceField) -> {
            cfgSet.add(new Object[]{instanceField.getKey(),
                            DUMMY_CLASS_CONSTANT,
                            instanceField.getValue()});
        });
        STABLE_ARRAYS_MAP.entrySet().stream().forEach((instanceField) -> {
            cfgSet.add(new Object[]{instanceField.getKey(),
                            DUMMY_CLASS_CONSTANT,
                            instanceField.getValue()});
        });
        ARRAY_ARRAYS_MAP.entrySet().stream().forEach((instanceField) -> {
            cfgSet.add(new Object[]{instanceField.getKey(),
                            DUMMY_CLASS_CONSTANT,
                            instanceField.getValue()});
        });
        STABLE_ARRAY_ARRAYS_MAP.entrySet().stream().forEach((instanceField) -> {
            cfgSet.add(new Object[]{instanceField.getKey(),
                            DUMMY_CLASS_CONSTANT,
                            instanceField.getValue()});
        });
        INSTANCE_FIELDS_MAP.entrySet().stream().forEach((instanceField) -> {
            cfgSet.add(new Object[]{instanceField.getKey(), JavaConstant.NULL_POINTER, null});
        });
        INSTANCE_FIELDS_MAP.entrySet().stream().forEach((instanceField) -> {
            cfgSet.add(new Object[]{instanceField.getKey(),
                            CONSTANT_REFLECTION_PROVIDER.forObject(DUMMY_CLASS_INSTANCE.objectField),
                            null});
        });
        return cfgSet.toArray(new Object[0][0]);
    }

    @DataProvider(name = "readFieldValueNegativeDataProvider")
    public static Object[][] readFieldValueNegativeDataProvider() {
        LinkedList<Object[]> cfgSet = new LinkedList<>();
        INSTANCE_FIELDS_MAP.keySet().stream().forEach((instanceField) -> {
            cfgSet.add(new Object[]{instanceField, null});
        });
        cfgSet.add(new Object[]{null, null});
        return cfgSet.toArray(new Object[0][0]);
    }
}
