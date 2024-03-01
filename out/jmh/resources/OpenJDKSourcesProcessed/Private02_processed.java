/*
 * @test   /nodynamiccopyright/
 * @bug    8071453
 * @author sadayapalam
 * @summary Various tests for private methods in interfaces.
 * @compile/fail/ref=Private02.out -XDrawDiagnostics Private02.java
 */


public class Private02 {
    interface I {
        private void foo(String s); 
        private abstract void foo(int i, int j); 
        void foo(int x); 
        private I foo() { return null; } 
        private void foo(int x) {} 
    }
    interface J extends I {
        private J foo() { return null; } 
    }
    interface K extends J {
        void foo(); 
    }
}
