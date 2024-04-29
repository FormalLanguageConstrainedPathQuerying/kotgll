/*
 * @test /nodynamiccopyright/
 * @bug 8177466
 * @summary Add compiler support for local variable type-inference
 * @compile/fail/ref=FoldingTest.out -XDrawDiagnostics FoldingTest.java
 */
class FoldingTest {

        void testReachability() {
        for(var i = 0; i < 3; i++) {
        }
            System.out.println("foo");   
        }

    void testCase(String s) {
        var c = "";
        final String c2 = "" + c;
        switch (s) {
            case c: break; 
            case c2: break; 
        }
    }

    void testAnno() {
        @Anno1(s1) 
        var s1 = "";
        @Anno2(s2) 
        var s2 = "";
        @Anno3(s3) 
        var s3 = "";
    }

    @interface Anno1 {
        String value();
    }
    @interface Anno2 {
        Class<?> value();
    }
    @interface Anno3 {
        Foo value();
    }

    enum Foo {
        A, B;
    }
}
