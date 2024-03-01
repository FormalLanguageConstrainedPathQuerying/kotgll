/*
 * @test /nodynamiccopyright/
 * @bug 8062977
 * @summary Inference: NullPointerException during bound incorporation
 *
 * @compile/fail/ref=T8062977.out -XDrawDiagnostics T8062977.java
 */

import java.util.List;

class T8062977 {
    <T extends B, B> T m(Class<B> cb) { return null; }

    void test1(Class<Iterable<?>> cb) {
        List<Integer>[] r1 = m(cb); 
        List<Integer> r2 = m(cb); 
    }

    void test2(Class<Iterable<?>[]> cb) {
        List<Integer>[] r1 = m(cb); 
        List<Integer> r2 = m(cb); 
    }

    void test3(Class<Iterable<?>[][]> cb) {
        List<Integer>[][] r1 = m(cb); 
        List<Integer>[] r2 = m(cb); 
        List<Integer> r3 = m(cb); 
    }
}
