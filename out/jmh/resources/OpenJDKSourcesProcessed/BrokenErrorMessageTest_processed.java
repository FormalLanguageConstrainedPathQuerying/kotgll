/*
 * @test  /nodynamiccopyright/
 * @bug 8194998
 * @summary broken error message for subclass of interface with private method
 * @compile/fail/ref=BrokenErrorMessageTest.out -XDrawDiagnostics BrokenErrorMessageTest.java
 */

class BrokenErrorMessageTest {
    void foo() {
        Runnable test1 = ((I)(new I() {}))::test;
        Runnable test2 = ((new I() {}))::test;
    }

    interface I {
        private void test() {}
    }
}
