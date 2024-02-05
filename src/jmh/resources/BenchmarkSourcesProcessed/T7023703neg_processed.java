/* @test /nodynamiccopyright/
 * @bug 7023703
 * @summary Valid code doesn't compile
 * @compile/fail/ref=T7023703neg.out -XDrawDiagnostics T7023703neg.java
 */

class T7023703neg {

    void testForLoop(boolean cond) {
        final int bug;
        final int bug2;
        for (;cond;) {
            final int item = 0;
            bug2 = 1; 
        }
        bug = 0; 
    }

    void testForEachLoop(java.util.Collection<Integer> c) {
        final int bug;
        final int bug2;
        for (Integer i : c) {
            final int item = 0;
            bug2 = 1; 
        }
        bug = 0; 
    }

    void testWhileLoop(boolean cond) {
        final int bug;
        final int bug2;
        while (cond) {
            final int item = 0;
            bug2 = 1; 
        }
        bug = 0; 
    }

    void testDoWhileLoop(boolean cond) {
        final int bug;
        final int bug2;
        do {
            final int item = 0;
            bug2 = 1; 
        } while (cond);
        bug = 0; 
    }
}
