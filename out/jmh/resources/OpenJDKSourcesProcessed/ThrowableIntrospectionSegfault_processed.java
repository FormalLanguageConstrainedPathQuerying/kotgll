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

/*
 * @test
 * @bug 8033735
 * @summary check backtrace field introspection
 * @modules java.base/jdk.internal.misc:open
 * @modules java.base/java.lang:open
 * @run main ThrowableIntrospectionSegfault
 */

import java.lang.reflect.*;

public class ThrowableIntrospectionSegfault {
    public static void main(java.lang.String[] unused) {
        Throwable throwable = new Throwable();
        throwable.fillInStackTrace();

        Class class1 = throwable.getClass();
        Field field;
        try {
            field = class1.getDeclaredField("backtrace");
        }
        catch (NoSuchFieldException e) {
            System.err.println("Can't retrieve field handle Throwable.backtrace: " + e.toString());
            return;
        }
        field.setAccessible(true);

        Object backtrace;
        try {
            backtrace = field.get(throwable);
        }
        catch (IllegalAccessException e) {
            System.err.println( "Can't retrieve field value for Throwable.backtrace: " + e.toString());
            return;
        }

        try {

            Class class2 = ((Object[]) ((Object[]) backtrace)[2])[0].getClass();

            String class2Name = class2.getName();

            System.err.println("class2Name=" + class2Name);
            return;  
        } catch (ClassCastException e) {
            System.out.println("Catch exception " + e);
            return;  
        }
    }
}
