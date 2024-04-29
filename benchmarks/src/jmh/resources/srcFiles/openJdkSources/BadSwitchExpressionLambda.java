/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Adding switch expressions
 * @compile/fail/ref=BadSwitchExpressionLambda.out -XDrawDiagnostics BadSwitchExpressionLambda.java
 */

class BadSwitchExpressionLambda {

    interface SAM {
        void invoke();
    }

    public static void m() {}
    public static void r(SAM sam) {}

    void test(int i) {
        SAM sam1 = () -> m(); 
        SAM sam2 = () -> switch (i) { case 0 -> m(); default -> m(); }; 
        r(() -> m()); 
        r(() -> switch (i) { case 0 -> m(); default -> m(); }); 
        return switch (i) { case 0 -> m(); default -> m(); }; 
    }
}
