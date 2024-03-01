/*
 * @test   /nodynamiccopyright/
 * @bug    8071453
 * @author sadayapalam
 * @summary Various tests for private methods in interfaces.
 * @compile/fail/ref=Private03.out -XDrawDiagnostics Private03.java
 */


public class Private03 {
    interface I {
        private void foo(int x) {}
        private void goo(int x) {}
    }

    interface J extends I {
        void foo(int x);
        default void goo(int x) {}
    }

    interface K extends J {
        private void foo(int x) {} 
        private void goo(int x) {} 
    }
}
