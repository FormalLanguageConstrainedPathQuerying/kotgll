/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package bcel;

import com.sun.org.apache.bcel.internal.classfile.Utility;
import java.util.Base64;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8256919
 * @modules java.xml/com.sun.org.apache.bcel.internal.classfile
 * @run testng bcel.UtilityTest
 * @summary Tests the Utility.
 */
public class UtilityTest {

    /*
     * @bug 8256919
     * Verifies the encode method.
     */
    @Test
    public void test() throws Exception {
        /**
         * public class Hello {
         *     public void hello(){
         *         System.out.println("Hello,world");
         *     }
         *     public static void main(String[] args) {
         *         Hello hello = new Hello();
         *         hello.hello();
         *     }
         * }
         * javac Hello.java
         * cat Hello.class |base64
         */
        String bytecodeBase64 = "cHVibGljIGNsYXNzIEhlbGxvIHsKICAgIHB1YmxpYyB2b2lkIGhlbGxvKCl7CiAgICAgICAgU3lzdGVtLm91dC5wcmludGxuKCJIZWxsbyx3b3JsZCIpOwogICAgfQogICAgcHVibGljIHN0YXRpYyB2b2lkIG1haW4oU3RyaW5nW10gYXJncykgewogICAgICAgIEhlbGxvIGhlbGxvID0gbmV3IEhlbGxvKCk7CiAgICAgICAgaGVsbG8uaGVsbG8oKTsKICAgIH0KfQo=";
        byte[] bytecode = Base64.getDecoder().decode(bytecodeBase64);
        String classname = Utility.encode(bytecode,true);
        /* attempting to decode would result in an exception:
         *     java.io.EOFException: Unexpected end of ZLIB input stream
         *if encode is not done properly
         */
        Utility.decode(classname, true);
    }

}
