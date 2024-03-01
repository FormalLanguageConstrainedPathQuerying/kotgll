/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   Test invalid lambda expressions
 * @compile/fail/ref=InvalidExpression6.out -XDrawDiagnostics InvalidExpression6.java
 */

public class InvalidExpression6 {

    interface SAM {
        void m(int i);
    }

    void test() {
        SAM s = (int n) -> { break; }; 
        s = (int n) -> { continue; }; 
    }
}
