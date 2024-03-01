/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @modules java.base/jdk.internal.org.objectweb.asm:+open java.base/jdk.internal.org.objectweb.asm.util:+open
 * @library /vmTestbase /test/lib
 *
 * @comment build retransform.jar in current dir
 * @run driver vm.runtime.defmeth.shared.BuildJar
 *
 * @run driver jdk.test.lib.FileInstaller . .
 * @run main/othervm/native
 *      -agentlib:redefineClasses
 *      -javaagent:retransform.jar
 *      vm.runtime.defmeth.PrivateMethodsTest
 */
package vm.runtime.defmeth;

import java.util.Set;

import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.annotation.NotApplicableFor;
import vm.runtime.defmeth.shared.builder.TestBuilder;
import vm.runtime.defmeth.shared.data.*;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SYNCHRONIZED;
import static vm.runtime.defmeth.shared.data.method.body.CallMethod.Invoke.*;
import static vm.runtime.defmeth.shared.data.method.body.CallMethod.IndexbyteOp.*;
import static vm.runtime.defmeth.shared.ExecutionMode.*;

/**
 * Scenarios on private methods in interfaces.
 */
public class PrivateMethodsTest extends DefMethTest {

    public static void main(String[] args) {
        DefMethTest.runTest(PrivateMethodsTest.class,
                /* majorVer */ Set.of(MIN_MAJOR_VER, MAX_MAJOR_VER),
                /* flags    */ Set.of(0, ACC_SYNCHRONIZED),
                /* redefine */ Set.of(false, true),
                /* execMode */ Set.of(DIRECT, REFLECTION, INVOKE_EXACT, INVOKE_GENERIC, INVOKE_WITH_ARGS, INDY));
    }




    /*
     * testPrivateInvokeVirtual
     *
     * interface I {
     *           private int privateM() { return 1; }
     *   default public  int m()        { return (I)this.privateM(); } 
     * }
     * class C implements I {}
     *
     * TEST: I o = new C(); o.m()I throws VerifyError
     * TEST: C o = new C(); o.m()I throws VerifyError
     */
    @NotApplicableFor(modes = { REDEFINITION }) 
    public void testPrivateInvokeVirtual(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("privateM", "()I")
                    .private_().returns(1).build()

                .defaultMethod("m", "()I")
                    .invoke(VIRTUAL, b.intfByName("I"), null, "privateM", "()I", INTERFACEMETHODREF).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().callSite(I, C, "m", "()I").throws_(VerifyError.class).done()
         .test().callSite(C, C, "m", "()I").throws_(VerifyError.class).done();
    }

    /*
     * testPrivateInvokeIntf
     *
     * interface I {
     *           private int privateM() { return 1; }
     *   default public  int m()        { return (I)this.privateM(); } 
     * }
     * class C implements I {}
     *
     * TEST: I o = new C(); o.m()I returns 1
     * TEST: C o = new C(); o.m()I returns 1
     */
    public void testPrivateInvokeIntf(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("privateM", "()I")
                    .private_().returns(1).build()
                .defaultMethod("m", "()I")
                    .invoke(INTERFACE, b.intfByName("I"), null, "privateM", "()I", CALLSITE).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().callSite(I, C, "m", "()I").returns(1).done()
         .test().callSite(C, C, "m", "()I").returns(1).done();
    }

    /*
     * testPrivateInvokeStatic
     *
     * interface I {
     *           private int privateM() { return 1; }
     *   default public  int m()        { return I.privateM(); } 
     * }
     * class C implements I {}
     *
     * TEST: I o = new C(); o.m()I throws IncompatibleClassChangeError
     * TEST: C o = new C(); o.m()I throws IncompatibleClassChangeError
     */
    public void testPrivateInvokeStatic(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("privateM", "()I")
                    .private_().returns(1).build()
                .defaultMethod("m", "()I")
                    .invoke(STATIC, b.intfByName("I"), null, "privateM", "()I", CALLSITE).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().callSite(I, C, "m", "()I").throws_(IncompatibleClassChangeError.class).done()
         .test().callSite(C, C, "m", "()I").throws_(IncompatibleClassChangeError.class).done();
    }

    /*
     * testPrivateCallSameClass
     *
     * interface I {
     *           private privateM()I { return 1; }
     *   default public int m() { return I.super.privateM(); } 
     * }
     * class C implements I {}
     *
     * TEST: { I o = new C(); o.m()I  == 1; }
     * TEST: { C o = new C(); o.m()I  == 1; }
     */
    public void testPrivateCallSameClass(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("privateM", "()I")
                    .private_().returns(1).build()
                .defaultMethod("m", "()I")
                    .invokeSpecial(b.intfByName("I"), "privateM", "()I").build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().callSite(I, C, "m", "()I").returns(1).done()
         .test().callSite(C, C, "m", "()I").returns(1).done();
    }

    /*
     * testPrivateCallSubIntf
     *
     * Attempt to call from subinterface fails

     * interface I {
     *   private privateM()I { return 1; }
     * }
     * J, K, L use invokespecial
     * interface J extends I {
     *   default public int m() { return I.super.privateM(); }
     * }
     * interface K extends I {
     *   default public int m() { return K.super.privateM(); }
     * }
     * interface L extends J {
     *   default public int m() { return I.super.privateM(); }
     * }
     * class C implements J {}
     * class D implements K {}
     * class E implements L {}
     *
     * TEST: { J o = new C(); o.m()I throws IAE; }
     * TEST: { C o = new C(); o.m()I throws IAE; }
     * TEST: { K o = new D(); o.m()I throws NSME; } 
     * TEST: { D o = new D(); o.m()I throws NSME; }
     * TEST: { L o = new E(); o.m()I throws VerifyError; } 
     * TEST: { E o = new E(); o.m()I throws VerifyError; }
     */
    @NotApplicableFor(modes = { REDEFINITION }) 
    public void testPrivateCallSubIntf(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("privateM", "()I")
                    .private_().returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I)
                .defaultMethod("m", "()I")
                    .invokeSpecial(I, "privateM", "()I").build()
            .build();

        Interface K = b.intf("K").extend(J)
                .defaultMethod("m", "()I")
                    .invokeSpecial(b.intfByName("K"), "privateM", "()I").build()
            .build();

        Interface L = b.intf("L").extend(J)
                .defaultMethod("m", "()I")
                    .invokeSpecial(I, "privateM", "()I").build()
            .build();

        ConcreteClass C = b.clazz("C").implement(J).build();

        ConcreteClass D = b.clazz("D").implement(K).build();

        ConcreteClass E = b.clazz("E").implement(L).build();

        b.test().callSite(J, C, "m", "()I").throws_(IllegalAccessError.class).done()
         .test().callSite(C, C, "m", "()I").throws_(IllegalAccessError.class).done()

         .test().callSite(K, D, "m", "()I").throws_(NoSuchMethodError.class).done()
         .test().callSite(D, D, "m", "()I").throws_(NoSuchMethodError.class).done()

         .test().callSite(L, E, "m", "()I").throws_(VerifyError.class).done()
         .test().callSite(E, E, "m", "()I").throws_(VerifyError.class).done();
    }

    /*
     * Attempt to call from subclass fails
     *
     * interface I {
     *   private privateM()I { return 1; }
     * }
     * class C implements I {
     *   public int m() { return I.super.privateM(); }
     * }
     * class D extends C {
     *   public int m() { return I.super.privateM(); }
     * }
     * class E extends C {
     *   public int m() { return C.super.privateM(); }
     * }
     *
     * TEST: { C o = new C(); o.m()I throws IllegalAccessError (or VerifyError) }
     * TEST: { D o = new D(); o.m()I throws VerifyError }
     * TEST: { E o = new E(); o.m()I throws NoSuchMethodError (or VerifyError); }
     */
    @NotApplicableFor(modes = { REDEFINITION }) 
    public void testPrivateCallImplClass(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("privateM", "()I")
                    .private_().returns(1).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I)
                .concreteMethod("m", "()I")
                    .invokeSpecial(I, "privateM", "()I").build()
            .build();

        ConcreteClass D = b.clazz("D").extend(C)
                .concreteMethod("m", "()I")
                    .invokeSpecial(I, "privateM", "()I").build()
            .build();

        ConcreteClass E = b.clazz("E").extend(C)
                .concreteMethod("m", "()I")
                    .invokeSpecial(C, "privateM", "()I").build()
            .build();

        Class eeExpectedClass;
        Class ccExpectedClass;
        if (factory.getVer() >= 52) {
            eeExpectedClass = NoSuchMethodError.class;
            ccExpectedClass = IllegalAccessError.class;
        } else {
            eeExpectedClass = VerifyError.class;
            ccExpectedClass = VerifyError.class;
        }
        b.test().callSite(C, C, "m", "()I").throws_(ccExpectedClass).done()
         .test().callSite(D, D, "m", "()I").throws_(VerifyError.class).done()
         .test().callSite(E, E, "m", "()I").throws_(eeExpectedClass).done();
    }


    /*
     * testPrivate
     *
     * interface I {
     *   private int m() { return 1; }
     * }
     * class C implements I {}
     *
     * TEST: { I o = new C(); o.m()I throws IllegalAccessError; }
     * TEST: { C o = new C(); o.m()I throws NoSuchMethodError; }
     */
    public void testPrivate(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I")
                    .private_().returns(1).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().privateCallSite(I, C, "m", "()I").throws_(IllegalAccessError.class).done()
         .test().       callSite(C, C, "m", "()I").throws_(NoSuchMethodError.class).done();
    }

    /*
     * testPrivateVsConcrete
     *
     * interface I {
     *   private int m() { return 1; }
     * }
     * class C implements I {
     *   public int m() { return 2; }
     * }
     *
     * TEST: { I o = new C(); o.m()I  == IllegalAccessError; }
     * TEST: { C o = new C(); o.m()I  == 2; }
     */
    public void testPrivateVsConcrete(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I")
                    .private_().returns(1).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I)
                .concreteMethod("m", "()I").returns(2).build()
            .build();

        b.test().privateCallSite(I, C, "m", "()I").throws_(IllegalAccessError.class).done()
         .test().       callSite(C, C, "m", "()I").returns(2).done();
    }

    /*
     * testPublicOverridePrivate
     *
     * interface I {
     *   private int m() { return 1; }
     * }
     * interface J extends I {
     *   default public int m() { return 2; }
     * }
     * class C implements J {}
     *
     * TEST: { I o = new C(); o.m()I throws IllegalAccessError; }
     * TEST: { J o = new C(); o.m()I  == 2; }
     * TEST: { C o = new C(); o.m()I  == 2; }
     */
    public void testPublicOverridePrivate(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I")
                    .private_().returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I)
                .defaultMethod("m", "()I")
                    .returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(J).build();

        b.test().privateCallSite(I, C, "m", "()I").throws_(IllegalAccessError.class).done()
         .test().       callSite(J, C, "m", "()I").returns(2).done()
         .test().       callSite(C, C, "m", "()I").returns(2).done();
    }

    /*
     * testPrivateOverrideDefault
     *
     * interface I {
     *   default public int m() { return 1; }
     * }
     * interface J extends I {
     *   private int m() { return 2; }
     * }
     * class C implements J {}
     *
     * TEST: { I o = new C(); o.m()I  == 1; }
     * TEST: { J o = new C(); o.m()I  == IllegalAccessError; } II J.m priv
     * TEST: { C o = new C(); o.m()I  == 1; }
     */
    public void testPrivateOverrideDefault(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I")
                    .returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I)
                .defaultMethod("m", "()I")
                    .private_().returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(J).build();

        b.test().callSite(I, C, "m", "()I").returns(1).done()
         .test().privateCallSite(J, C, "m", "()I").throws_(IllegalAccessError.class).done()
         .test().callSite(C, C, "m", "()I").returns(1).done();
    }

    /*
     * testPrivateReabstract
     *
     * interface I {
     *   private int m() { return 1; }
     * }
     * interface J extends I {
     *   abstract public int m();
     * }
     * class C implements J {}
     *
     * TEST: { I o = new C(); o.m()I throws IllegalAccessError; } II I.m
     * TEST: { J o = new C(); o.m()I throws java/lang/AbstractMethodError; }
     * TEST: { C o = new C(); o.m()I throws java/lang/AbstractMethodError; }
     */
    public void testPrivateReabstract(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I")
                    .private_().returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I)
                .abstractMethod("m", "()I").build()
            .build();

        ConcreteClass C = b.clazz("C").implement(J).build();

        b.test().privateCallSite(I, C, "m", "()I").throws_(IllegalAccessError.class).done()
         .test().       callSite(J, C, "m", "()I").throws_(AbstractMethodError.class).done()
         .test().       callSite(C, C, "m", "()I").throws_(AbstractMethodError.class).done();
    }

    /*
     * testPrivateOverrideAbstract
     *
     * interface I {
     *   abstract public int m();
     * }
     * interface J extends I {
     *   private int m() { return 1; }
     * }
     * class C implements J {}
     *
     * TEST: { I o = new C(); o.m()I throws AbstractMethodError }
     * TEST: { J o = new C(); o.m()I throws IllegalAccessError }
     * TEST: { C o = new C(); o.m()I throws AbstractMethodError }
     */
    public void testPrivateOverrideAbstract(TestBuilder b) {
        Interface I = b.intf("I")
                .abstractMethod("m", "()I").build()
            .build();

        Interface J = b.intf("J").extend(I)
                .defaultMethod("m", "()I")
                    .private_().returns(1).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(J).build();

        b.test().callSite(I, C, "m", "()I").throws_(AbstractMethodError.class).done()
         .test().privateCallSite(J, C, "m", "()I").throws_(IllegalAccessError.class).done()
         .test().callSite(C, C, "m", "()I").throws_(AbstractMethodError.class).done();
    }

    /*
     * testPrivateInherited
     *
     * interface I {
     *   private int m() { return 1; }
     * }
     * class B implements I {}
     * class C extends B {}
     *
     * TEST: { I o = new C(); o.m()I throws IllegalAccessError } II I.m
     * TEST: { B o = new C(); o.m()I throws NoSuchMethodError }
     * TEST: { C o = new C(); o.m()I throws NoSuchMethodError }
     */
    public void testPrivateInherited(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I")
                    .private_().returns(1).build()
            .build();

        ConcreteClass B = b.clazz("B").implement(I).build();
        ConcreteClass C = b.clazz("C").extend(B).build();

        b.test().privateCallSite(I, C, "m","()I").throws_(IllegalAccessError.class).done()
         .test().       callSite(B, C, "m","()I").throws_(NoSuchMethodError.class).done()
         .test().       callSite(C, C, "m","()I").throws_(NoSuchMethodError.class).done();
    }

    /*
     * testPrivateVsConcreteInherited
     *
     * interface I {
     *    private int m() { return 1; }
     * }
     * class B {
     *   public int m() { return 2; }
     * }
     * class C extends B implements I {}
     *
     * TEST: { I o = new C(); o.m()I  == throws IllegalAccessError; }
     * TEST: { B o = new C(); o.m()I  == 2; }
     * TEST: { C o = new C(); o.m()I  == 2; }
     */
    public void testPrivateVsConcreteInherited(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I")
                    .private_().returns(1).build()
            .build();

        ConcreteClass B = b.clazz("B")
                .concreteMethod("m", "()I").returns(2).build()
                .build();

        ConcreteClass C = b.clazz("C").extend(B).implement(I).build();

        b.test().privateCallSite(I, C, "m","()I").throws_(IllegalAccessError.class).done()
         .test().       callSite(B, C, "m","()I").returns(2).done()
         .test().       callSite(C, C, "m","()I").returns(2).done();
    }

    /*
     * testPrivateConflict
     *
     * Conflicting methods
     *
     * interface I {
     *   private int m() { return 1; }
     * }
     * interface J {
     *   default public int m() { return 2; }
     * }
     * class C implements I, J {}
     *
     * TEST: { I o = new C(); o.m()I throws IllegalAccessError; }
     * TEST: { J o = new C(); o.m()I  == 2; }
     * TEST: { C o = new C(); o.m()I  == 2; }
     */
    public void testPrivateConflict(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").private_().returns(1).build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I,J).build();

        b.test().privateCallSite(I, C, "m", "()I").throws_(IllegalAccessError.class).done()
         .test().       callSite(J, C, "m", "()I").returns(2).done()
         .test().       callSite(C, C, "m", "()I").returns(2).done();
    }
    /*
     * testPrivateSuperClassMethodNoDefaultMethod
     *
     * interface I {
     *  public int m();
     * }
     *
     * public class A {
     *  private int m() { return 1; }
     * }
     *
     * public class B extends A implements I {}
     *
     * public class C extends B {
     *  public int m() { return 2; }
     * }
     *
     * TEST: { B b = new C(); b.m()I throws IllegalAccessError; }
     */
    public void testPrivateSuperClassMethodNoDefaultMethod(TestBuilder b) {
        ConcreteClass A = b.clazz("A")
                .concreteMethod("m", "()I").private_().returns(1).build()
                .build();

        Interface I = b.intf("I")
                .abstractMethod("m", "()I").public_().build()
                .build();

        ConcreteClass B = b.clazz("B").extend(A).implement(I).build();

        ConcreteClass C = b.clazz("C").extend(B)
                .concreteMethod("m", "()I").public_().returns(2).build()
                .build();

        b.test().privateCallSite(B, C, "m", "()I").throws_(IllegalAccessError.class).done();
    }

    /*
     * testPrivateSuperClassMethodDefaultMethod
     *
     * interface I {
     *  public default int m() { return 3; }
     * }
     *
     * public class A {
     *  private int m() { return 1; }
     * }
     *
     * public class B extends A implements I {}
     *
     * public class C extends B {
     *  public int m() { return 2; }
     * }
     *
     * TEST: { B b = new C(); b.m()I throws IllegalAccessError; }
     */
    public void testPrivateSuperClassMethodDefaultMethod(TestBuilder b) {
        ConcreteClass A = b.clazz("A")
                .concreteMethod("m", "()I").private_().returns(1).build()
                .build();

        Interface I = b.intf("I")
                .defaultMethod("m", "()I").public_().returns(3).build()
                .build();

        ConcreteClass B = b.clazz("B").extend(A).implement(I).build();

        ConcreteClass C = b.clazz("C").extend(B)
                .concreteMethod("m", "()I").public_().returns(2).build()
                .build();

        b.test().privateCallSite(B, C, "m", "()I").throws_(IllegalAccessError.class).done();
    }

    /*
     * testPrivateSuperClassMethodDefaultMethodNoOverride
     *
     * interface I {
     *  public default int m() { return 3; }
     * }
     *
     * public class A {
     *  private int m() { return 1; }
     * }
     *
     * public class B extends A implements I {}
     *
     * public class C extends B { }
     *
     * TEST: { B b = new C(); b.m()I throws IllegalAccessError; }
     */
    public void testPrivateSuperClassMethodDefaultMethodNoOverride(TestBuilder b) {
        ConcreteClass A = b.clazz("A")
                .concreteMethod("m", "()I").private_().returns(1).build()
                .build();

        Interface I = b.intf("I")
                .defaultMethod("m", "()I").public_().returns(3).build()
                .build();

        ConcreteClass B = b.clazz("B").extend(A).implement(I).build();

        ConcreteClass C = b.clazz("C").extend(B).build();

        b.test().privateCallSite(B, C, "m", "()I").throws_(IllegalAccessError.class).done();
    }
}
