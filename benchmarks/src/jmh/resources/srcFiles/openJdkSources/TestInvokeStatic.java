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
 * @bug 8046171
 * @summary Test access to private static methods between nestmates and nest-host
 *          using different flavours of named nested types
 * @run main TestInvokeStatic
 */

public class TestInvokeStatic {

    private static void priv_static_invoke() {
        System.out.println("TestInvokeStatic::priv_static_invoke");
    }

    public TestInvokeStatic() {}



    void access_priv(TestInvokeStatic o) {
        o.priv_static_invoke();
    }
    void access_priv(StaticNested o) {
        o.priv_static_invoke();
    }
    void access_priv(StaticIface o) {
        StaticIface.priv_static_invoke();
    }


    static interface StaticIface {

        private static void priv_static_invoke() {
            System.out.println("StaticIface::priv_static_invoke");
        }


        default void access_priv(TestInvokeStatic o) {
            o.priv_static_invoke();
        }
        default void access_priv(StaticNested o) {
            o.priv_static_invoke();
        }
        default void access_priv(StaticIface o) {
            StaticIface.priv_static_invoke();
        }
    }

    static class StaticNested {

        private static void priv_static_invoke() {
            System.out.println("StaticNested::priv_static_invoke");
        }

        public StaticNested() {}


        void access_priv(TestInvokeStatic o) {
            o.priv_static_invoke();
        }
        void access_priv(StaticNested o) {
            o.priv_static_invoke();
        }
        void access_priv(StaticIface o) {
            StaticIface.priv_static_invoke();
        }
    }

    class InnerNested {

        public InnerNested() {}

        void access_priv(TestInvokeStatic o) {
            o.priv_static_invoke();
        }
        void access_priv(StaticNested o) {
            o.priv_static_invoke();
        }
        void access_priv(StaticIface o) {
            StaticIface.priv_static_invoke();
        }
    }

    public static void main(String[] args) {
        TestInvokeStatic o = new TestInvokeStatic();
        StaticNested s = new StaticNested();
        InnerNested i = o.new InnerNested();
        StaticIface intf = new StaticIface() {};

        o.access_priv(new TestInvokeStatic());
        o.access_priv(s);
        o.access_priv(intf);

        s.access_priv(o);
        s.access_priv(new StaticNested());
        s.access_priv(intf);

        i.access_priv(o);
        i.access_priv(s);
        i.access_priv(intf);

        intf.access_priv(o);
        intf.access_priv(s);
        intf.access_priv(new StaticIface(){});
    }
}
