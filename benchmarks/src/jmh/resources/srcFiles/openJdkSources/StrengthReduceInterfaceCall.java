/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @requires !vm.graal.enabled & vm.opt.final.UseVtableBasedCHA == true
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 *          java.base/jdk.internal.vm.annotation
 * @library /test/lib /
 * @compile Utils.java
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+PrintCompilation -XX:+PrintInlining -Xlog:dependencies=debug -verbose:class -XX:CompileCommand=quiet
 *                   -XX:CompileCommand=compileonly,*::m
 *                   -XX:CompileCommand=compileonly,*::test -XX:CompileCommand=dontinline,*::test
 *                   -XX:CompileCommand=compileonly,*::testHelper -XX:CompileCommand=inline,*::testHelper
 *                   -Xbatch -Xmixed -XX:+WhiteBoxAPI
 *                   -XX:-TieredCompilation
 *                      compiler.cha.StrengthReduceInterfaceCall
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+PrintCompilation -XX:+PrintInlining -Xlog:dependencies=debug -verbose:class -XX:CompileCommand=quiet
 *                   -XX:CompileCommand=compileonly,*::m
 *                   -XX:CompileCommand=compileonly,*::test -XX:CompileCommand=dontinline,*::test
 *                   -XX:CompileCommand=compileonly,*::testHelper -XX:CompileCommand=inline,*::testHelper
 *                   -Xbatch -Xmixed -XX:+WhiteBoxAPI
 *                   -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                      compiler.cha.StrengthReduceInterfaceCall
 */
package compiler.cha;

import jdk.internal.vm.annotation.DontInline;

import static compiler.cha.Utils.*;

public class StrengthReduceInterfaceCall {
    public static void main(String[] args) {
        run(ObjectToString.class);
        run(ObjectHashCode.class);
        run(TwoLevelHierarchyLinear.class);
        run(ThreeLevelHierarchyLinear.class);
        run(ThreeLevelHierarchyAbstractVsDefault.class);
        run(ThreeLevelDefaultHierarchy.class);
        run(ThreeLevelDefaultHierarchy1.class);
        System.out.println("TEST PASSED");
    }

    public static class ObjectToString extends ATest<ObjectToString.I> {
        public ObjectToString() { super(I.class, C.class); }

        interface J           { String toString(); }
        interface I extends J {}

        static class C implements I {}

        interface K1 extends I {}
        interface K2 extends I { String toString(); } 

        static class D implements I { public String toString() { return "D"; }}

        static class DJ1 implements J {}
        static class DJ2 implements J { public String toString() { return "DJ2"; }}

        @Override
        public Object test(I i) { return ObjectToStringHelper.testHelper(i); /* invokeinterface I.toString() */ }

        @TestCase
        public void testMono() {
            compile(monomophic()); 
            assertCompiled();


            call(new C() { public String toString() { return "Cn"; }}); 
            assertCompiled();
        }

        @TestCase
        public void testBi() {
            compile(bimorphic()); 
            assertCompiled();


            call(new C() { public String toString() { return "Cn"; }}); 
            assertCompiled();
        }

        @TestCase
        public void testMega() {
            compile(megamorphic()); 
            assertCompiled();


            repeat(100, () -> call(new C(){})); 
            assertCompiled();

            initialize(K1.class,   
                       K2.class,   
                       DJ1.class,  
                       DJ2.class); 
            assertCompiled();

            initialize(D.class); 
            assertCompiled();

            call(new C() { public String toString() { return "Cn"; }}); 
            assertCompiled();
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); 
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J() {}); 
                test(j);
            });
            assertCompiled();
        }
    }

    public static class ObjectHashCode extends ATest<ObjectHashCode.I> {
        public ObjectHashCode() { super(I.class, C.class); }

        interface J {}
        interface I extends J {}

        static class C implements I {}

        interface K1 extends I {}
        interface K2 extends I { int hashCode(); } 

        static class D implements I { public int hashCode() { return super.hashCode(); }}

        static class DJ1 implements J {}
        static class DJ2 implements J { public int hashCode() { return super.hashCode(); }}

        @Override
        public Object test(I i) {
            return ObjectHashCodeHelper.testHelper(i); /* invokeinterface I.hashCode() */
        }

        @TestCase
        public void testMono() {
            compile(monomophic()); 
            assertCompiled();


            call(new C() { public int hashCode() { return super.hashCode(); }}); 
            assertCompiled();
        }

        @TestCase
        public void testBi() {
            compile(bimorphic()); 
            assertCompiled();


            call(new C() { public int hashCode() { return super.hashCode(); }}); 
            assertCompiled();
        }

        @TestCase
        public void testMega() {
            compile(megamorphic()); 
            assertCompiled();


            repeat(100, () -> call(new C(){})); 
            assertCompiled();

            initialize(K1.class,   
                       K2.class,   
                       DJ1.class,  
                       DJ2.class); 
            assertCompiled();

            initialize(D.class); 
            assertCompiled();

            call(new C() { public int hashCode() { return super.hashCode(); }}); 
            assertCompiled();
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); 
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J() {}); 
                test(j);
            });
            assertCompiled();
        }
    }

    public static class TwoLevelHierarchyLinear extends ATest<TwoLevelHierarchyLinear.I> {
        public TwoLevelHierarchyLinear() { super(I.class, C.class); }

        interface J { default Object m() { return WRONG; } }

        interface I extends J { Object m(); }
        static class C implements I { public Object m() { return CORRECT; }}

        interface K1 extends I {}
        interface K2 extends I { Object m(); }
        interface K3 extends I { default Object m() { return WRONG;   }}
        interface K4 extends I { default Object m() { return CORRECT; }}

        static class D implements I { public Object m() { return WRONG;   }}

        static class DJ1 implements J {}
        static class DJ2 implements J { public Object m() { return WRONG; }}

        @DontInline
        public Object test(I i) {
            return i.m();
        }

        @TestCase
        public void testMega1() {
            compile(megamorphic()); 
            assertCompiled();


            checkInvalidReceiver(); 

            repeat(100, () -> call(new C(){})); 
            assertCompiled();

            initialize(K1.class,   
                       K2.class,   
                       DJ1.class,  
                       DJ2.class); 
            assertCompiled();

            initialize(D.class); 
            assertNotCompiled();

            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; }}); 
            assertCompiled();

            checkInvalidReceiver(); 
        }

        @TestCase
        public void testMega2() {
            compile(megamorphic()); 
            assertCompiled();


            checkInvalidReceiver(); 

            initialize(K3.class); 
            assertCompiled();

            call(new K3() { public Object m() { return CORRECT; }}); 
            assertNotCompiled();

            compile(megamorphic());
            call(new K3() { public Object m() { return CORRECT; }}); 
            assertCompiled();

            checkInvalidReceiver(); 
        }

        @TestCase
        public void testMega3() {
            compile(megamorphic()); 
            assertCompiled();


            checkInvalidReceiver(); 

            initialize(K4.class); 
            assertCompiled();

            call(new K4() { /* default method K4.m */ }); 
            assertNotCompiled();

            compile(megamorphic());
            call(new K4() { /* default method K4.m */  }); 
            assertCompiled();

            checkInvalidReceiver(); 
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); 
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J() {}); 
                test(j);
            });
            assertCompiled();
        }
    }

    public static class ThreeLevelHierarchyLinear extends ATest<ThreeLevelHierarchyLinear.I> {
        public ThreeLevelHierarchyLinear() { super(I.class, C.class); }

        interface J           { Object m(); }
        interface I extends J {}

        interface K1 extends I {}
        interface K2 extends I { Object m(); }
        interface K3 extends I { default Object m() { return WRONG;   }}
        interface K4 extends I { default Object m() { return CORRECT; }}

        static class C  implements I { public Object m() { return CORRECT; }}

        static class DI implements I { public Object m() { return WRONG;   }}
        static class DJ implements J { public Object m() { return WRONG;   }}

        @DontInline
        public Object test(I i) {
            return i.m(); 
        }

        @TestCase
        public void testMega1() {
            compile(megamorphic()); 
            assertCompiled();


            checkInvalidReceiver(); 

            repeat(100, () -> call(new C(){})); 
            assertCompiled(); 

            initialize(DJ.class,  
                       K1.class,  
                       K2.class); 
            assertCompiled();

            initialize(DI.class); 
            assertNotCompiled();

            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; }}); 
            assertCompiled(); 

            checkInvalidReceiver(); 
        }

        @TestCase
        public void testMega2() {
            compile(megamorphic()); 
            assertCompiled();


            checkInvalidReceiver(); 

            initialize(K3.class); 
            assertCompiled();


            checkInvalidReceiver(); 

            call(new K3() { public Object m() { return CORRECT; }}); 
            assertNotCompiled();

            compile(megamorphic());
            checkInvalidReceiver(); 
            call(new C() { public Object m() { return CORRECT; }}); 
            assertCompiled();
        }

        @TestCase
        public void testMega3() {
            compile(megamorphic()); 
            assertCompiled();


            checkInvalidReceiver(); 

            initialize(K4.class); 
            assertCompiled();


            checkInvalidReceiver(); 

            call(new K4() { /* default method K4.m */ }); 
            assertNotCompiled();

            compile(megamorphic());
            checkInvalidReceiver(); 
            call(new C() { public Object m() { return CORRECT; }}); 
            assertCompiled();
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); 
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J() { public Object m() { return WRONG; }}); 
                test(j);
            });
            assertCompiled();
        }
    }

    public static class ThreeLevelHierarchyAbstractVsDefault extends ATest<ThreeLevelHierarchyAbstractVsDefault.I> {
        public ThreeLevelHierarchyAbstractVsDefault() { super(I.class, C.class); }

        interface J1                { default Object m() { return WRONG; } } 
        interface J2 extends J1     { Object m(); }                          
        interface I  extends J1, J2 {}                                       

        static class C  implements I { public Object m() { return CORRECT; }}

        @DontInline
        public Object test(I i) {
            return i.m(); 
        }

        static class DI implements I { public Object m() { return WRONG;   }}

        static class DJ11 implements J1 {}
        static class DJ12 implements J1 { public Object m() { return WRONG; }}

        static class DJ2 implements J2 { public Object m() { return WRONG;   }}

        interface K11 extends J1 {}
        interface K12 extends J1 { Object m(); }
        interface K13 extends J1 { default Object m() { return WRONG; }}
        interface K21 extends J2 {}
        interface K22 extends J2 { Object m(); }
        interface K23 extends J2 { default Object m() { return WRONG; }}


        public void testMega1() {
            compile(megamorphic()); 
            assertCompiled();


            checkInvalidReceiver(); 

            repeat(100, () -> call(new C(){})); 
            assertCompiled();

            initialize(K11.class, K12.class, K13.class,
                       K21.class, K22.class, K23.class);

            call(new C() { public Object m() { return CORRECT; }}); 
            assertNotCompiled();

            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; }});
            assertCompiled(); 

            checkInvalidReceiver(); 
        }

        public void testMega2() {
            compile(megamorphic());
            assertCompiled();


            checkInvalidReceiver(); 

            initialize(DJ11.class,
                       DJ12.class,
                       DJ2.class);
            assertCompiled();

            initialize(DI.class);
            assertNotCompiled();

            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; }});
            assertCompiled(); 

            checkInvalidReceiver(); 
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); 
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J1() {}); 
                test(j);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J2() { public Object m() { return WRONG; }}); 
                test(j);
            });
            assertCompiled();
        }
    }

    public static class ThreeLevelDefaultHierarchy extends ATest<ThreeLevelDefaultHierarchy.I> {
        public ThreeLevelDefaultHierarchy() { super(I.class, C.class); }

        interface J           { default Object m() { return WRONG; }}
        interface I extends J {}

        static class C  implements I { public Object m() { return CORRECT; }}

        interface K1 extends I {}
        interface K2 extends I { Object m(); }
        interface K3 extends J { default Object m() { return WRONG; }}

        static class DI implements I { public Object m() { return WRONG; }}
        static class DJ implements J { public Object m() { return WRONG; }}
        static class DK3 implements K3 {}

        @DontInline
        public Object test(I i) {
            return i.m();
        }

        @TestCase
        public void testMega() {
            compile(megamorphic()); 
            assertCompiled();


            checkInvalidReceiver(); 

            repeat(100, () -> call(new C() {}));
            assertCompiled();

            initialize(DJ.class,    
                       K1.class,   
                       K2.class,   
                       DK3.class); 
            assertCompiled();

            initialize(DI.class); 
            assertNotCompiled();

            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; }});
            assertCompiled(); 
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); 
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J() {}); 
                test(j);
            });
            assertCompiled();
        }
    }

    public static class ThreeLevelDefaultHierarchy1 extends ATest<ThreeLevelDefaultHierarchy1.I> {
        public ThreeLevelDefaultHierarchy1() { super(I.class, C.class); }

        interface J1                { Object m();}
        interface J2 extends J1     { default Object m() { return WRONG; }  }
        interface I  extends J1, J2 {}

        static class C  implements I { public Object m() { return CORRECT; }}

        interface K1 extends I {}
        interface K2 extends I { Object m(); }
        interface K3 extends I { default Object m() { return WRONG; }}

        static class DI implements I { public Object m() { return WRONG; }}
        static class DJ1 implements J1 { public Object m() { return WRONG; }}
        static class DJ2 implements J2 { public Object m() { return WRONG; }}

        @DontInline
        public Object test(I i) {
            return i.m();
        }

        @TestCase
        public void testMega() {
            compile(megamorphic());
            assertCompiled();


            checkInvalidReceiver(); 

            repeat(100, () -> call(new C() {}));
            assertCompiled();

            initialize(DJ1.class,
                       DJ2.class,
                       K1.class,
                       K2.class,
                       K3.class);
            assertCompiled();

            initialize(DI.class); 
            assertNotCompiled();

            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; }});
            assertCompiled(); 
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); 
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J1() { public Object m() { return WRONG; } }); 
                test(j);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J2() {}); 
                test(j);
            });
            assertCompiled();
        }
    }
}
