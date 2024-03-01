/*
 * @test /nodynamiccopyright/
 * @bug 8008540 8008539 8008538
 * @summary Constructor reference to non-reifiable array should be rejected
 * @compile/fail/ref=MethodReference64.out -XDrawDiagnostics MethodReference64.java
 */
class MethodReference64 {
    interface ClassFactory {
        Object m();
    }

    interface ArrayFactory {
        Object m(int i);
    }

    @interface Anno { }

    enum E { }

    interface I { }

    static class Foo<X> { }

    void m(ClassFactory cf) { }
    void m(ArrayFactory cf) { }

    void testAssign() {
        ClassFactory c1 = Anno::new; 
        ClassFactory c2 = E::new; 
        ClassFactory c3 = I::new; 
        ClassFactory c4 = Foo<?>::new; 
        ClassFactory c5 = 1::new; 
        ArrayFactory a1 = Foo<?>[]::new; 
        ArrayFactory a2 = Foo<? extends String>[]::new; 
    }

    void testMethod() {
        m(Anno::new); 
        m(E::new); 
        m(I::new); 
        m(Foo<?>::new); 
        m(1::new); 
        m(Foo<?>[]::new); 
        m(Foo<? extends String>[]::new); 
    }
}
