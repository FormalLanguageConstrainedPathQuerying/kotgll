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
 * @summary Test access to private static fields between nestmates and nest-host
 *          using different flavours of named nested types
 * @run main TestPrivateStaticField
 */

public class TestPrivateStaticField {

    private static int priv_field;

    public TestPrivateStaticField() {}



    void access_priv(TestPrivateStaticField o) {
        priv_field = o.priv_field++;
    }
    void access_priv(StaticNested o) {
        priv_field = o.priv_field++;
    }


    static interface StaticIface {


        default void access_priv(TestPrivateStaticField o) {
            int priv_field = o.priv_field++;
        }
        default void access_priv(StaticNested o) {
            int priv_field = o.priv_field++;
        }
    }

    static class StaticNested {

        private static int priv_field;

        public StaticNested() {}


        void access_priv(TestPrivateStaticField o) {
            priv_field = o.priv_field++;
        }
        void access_priv(StaticNested o) {
            priv_field = o.priv_field++;
        }
    }

    class InnerNested {

        public InnerNested() {}

        void access_priv(TestPrivateStaticField o) {
            int priv_field = o.priv_field++;
        }
        void access_priv(StaticNested o) {
            int priv_field = o.priv_field++;
        }
    }

    public static void main(String[] args) {
        TestPrivateStaticField o = new TestPrivateStaticField();
        StaticNested s = new StaticNested();
        InnerNested i = o.new InnerNested();
        StaticIface intf = new StaticIface() {};

        o.access_priv(new TestPrivateStaticField());
        o.access_priv(s);

        s.access_priv(o);
        s.access_priv(new StaticNested());

        i.access_priv(o);
        i.access_priv(s);

        intf.access_priv(o);
        intf.access_priv(s);
    }
}
