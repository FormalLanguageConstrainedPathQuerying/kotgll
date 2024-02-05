/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   This is negative test for wrong parameter/return type in method references
 * @compile/fail/ref=MethodRef_neg.out -XDrawDiagnostics MethodRef_neg.java
 */

public class MethodRef_neg {

    static interface A {void m(Integer i);}

    static interface B {void m(String s);}

    static interface C {Integer m();}

    static interface D {String m();}


    static void bar(int x) { }

    int foo() {
        return 5;
    }

    static void make() { }

    void method() {
        A a = MethodRef_neg::bar; 
        B b = MethodRef_neg::bar; 
        C c = this::foo; 
        D d = this::foo; 
        a = MethodRef_neg::make; 
        c = MethodRef_neg::make; 
    }
}
