/**
 * @test /nodynamiccopyright/
 * @bug     6717241
 * @summary some diagnostic argument is prematurely converted into a String object
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=T6717241b.out -XDrawDiagnostics T6717241b.java
 */

class T6717241b {
    void test() {
        Object o = v;
        m1(1, "");
        T6717241b.<Integer,Double>m2(1, "");
    }
}
