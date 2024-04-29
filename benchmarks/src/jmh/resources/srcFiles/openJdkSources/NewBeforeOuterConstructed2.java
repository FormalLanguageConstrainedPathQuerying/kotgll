/*
 * @test /nodynamiccopyright/
 * @bug 4689058
 * @summary unverifiable code for implicit outer in super constructor call
 *
 * @compile/fail/ref=NewBeforeOuterConstructed2.out -XDrawDiagnostics  NewBeforeOuterConstructed2.java
 */

public class NewBeforeOuterConstructed2 {
    NewBeforeOuterConstructed2(Object o) {}
    class Middle extends NewBeforeOuterConstructed2 {
        Middle(int i) {
            super(null);
        }
        Middle() {
            super(/*Middle.this.*/new Middle(1));
        }
        class Inner {}
        void f() {
            System.out.println("ok");
        }
    }
}
