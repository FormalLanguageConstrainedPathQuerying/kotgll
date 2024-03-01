/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8046171 8211065
 * @summary Setup nestmate calls to private methods then use
 *          modified jcod classes to introduce errors. Test with
 *          and without verification enabled
 * @compile TestInvokeErrors.java
 * @compile MissingMethod.jcod
 *          MissingMethodWithSuper.jcod
 *          MissingNestHost.jcod
 * @run main TestInvokeErrors true
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:-BytecodeVerificationRemote -XX:-BytecodeVerificationLocal TestInvokeErrors false
 */

public class TestInvokeErrors {

    static class Nested {
        private void priv_invoke() {
            System.out.println("Nested::priv_invoke");
        }
    }

    static class MissingMethod {
        private void priv_invoke() {
            System.out.println("MissingMethod::priv_invoke");
        }
    }

    static class MissingMethodWithSuper extends Nested {
        private void priv_invoke() {
            System.out.println("MissingMethodWithSuper::priv_invoke");
        }
    }

    static class MissingNestHost {
        private void priv_invoke() {
            System.out.println("MissingNestHost::priv_invoke");
        }
    }

    static class Helper {
        static void doTest() {
            try {
                MissingNestHost m = new MissingNestHost();
                m.priv_invoke();
                throw new Error("Unexpected success invoking MissingNestHost.priv_invoke");
            }
            catch (IllegalAccessError iae) {
                if (iae.getMessage().contains("java.lang.NoClassDefFoundError: NoSuchClass")) {
                    System.out.println("Got expected exception:" + iae);
                } else {
                    throw new Error("Unexpected exception", iae);
                }
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        boolean verifying = Boolean.parseBoolean(args[0]);
        System.out.println("Verification is " +
                           (verifying ? "enabled" : "disabled"));

        try {
            MissingMethod m = new MissingMethod();
            m.priv_invoke();
            throw new Error("Unexpected success invoking MissingMethod.priv_invoke");
        }
        catch (NoSuchMethodError nsme) {
            System.out.println("Got expected exception:" + nsme);
        }

        MissingMethodWithSuper m = new MissingMethodWithSuper();
        m.priv_invoke();

        try {
            Helper.doTest();
        }
        catch (IllegalAccessError iae) {
            if (verifying)
                System.out.println("Got expected exception:" + iae);
            else
                throw new Error("Unexpected error loading Helper class with verification disabled", iae);
        }
    }
}
