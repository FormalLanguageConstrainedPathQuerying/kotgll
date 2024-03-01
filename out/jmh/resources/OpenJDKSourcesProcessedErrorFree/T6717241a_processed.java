/**
 * @test /nodynamiccopyright/
 * @bug     6717241
 * @summary some diagnostic argument is prematurely converted into a String object
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=T6717241a.out -XDrawDiagnostics T6717241a.java
 */

class T6717241a<X extends Object & java.io.Serializable> {
    X x;
    void test() {
        Object o = x.v;
        x.m1(1, "");
        x.<Integer,Double>m2(1, "");
    }
}
