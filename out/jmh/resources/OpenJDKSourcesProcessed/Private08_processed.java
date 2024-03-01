/* @test   /nodynamiccopyright/
 * @bug    8071453
 * @author sadayapalam
 * @summary Test various JLS changes made for supporting private interface methods.
 * @compile/fail/ref=Private08.out -XDrawDiagnostics Private08.java
 */
class Private08 {
    interface I {
        private void poo() {}
        private int foo() { return 0; }
        int goo();
        default int doo() { return foo(); }
        private public int bad(); 
        private abstract int verybad(); 
        private default int alsobad() { return foo(); } 
        protected void blah();
        private void missingBody(); 
    }
}

class Private08_01 {
    int y = ((Private08.I) null).foo();   
    interface J extends Private08.I {
        default void foo() { 
            super.foo();  
        }
        private int doo() { return 0; } 
    };

    Private08.I i = new Private08.I () {
        public void foo() { 
            super.foo();  
        }
        private int doo() { return 0; } 
    }; 
}

