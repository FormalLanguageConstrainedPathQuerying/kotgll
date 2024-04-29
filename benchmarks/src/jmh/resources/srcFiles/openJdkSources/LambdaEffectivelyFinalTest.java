/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  Integrate efectively final check with DA/DU analysis
 * @compile/fail/ref=LambdaEffectivelyFinalTest.out -XDrawDiagnostics LambdaEffectivelyFinalTest.java
 */
class LambdaEffectivelyFinalTest {

    interface SAM {
        int m();
    }

    void foo(LambdaEffectivelyFinalTest.SAM s) { }

    void m1(int x) {
        int y = 1;
        foo(() -> x+y); 
    }

    void m2(int x) {
        int y;
        y = 1;
        foo(() -> x+y); 
    }

    void m3(int x, boolean cond) {
        int y;
        if (cond) y = 1;
        foo(() -> x+y); 
    }

    void m4(int x, boolean cond) {
        int y;
        if (cond) y = 1;
        else y = 2;
        foo(() -> x+y); 
    }

    void m5(int x, boolean cond) {
        int y;
        if (cond) y = 1;
        y = 2;
        foo(() -> x+y); 
    }

    void m6(int x) {
        foo(() -> x+1);
        x++; 
    }

    void m7(int x) {
        foo(() -> x=1); 
    }

    void m8() {
        int y;
        foo(() -> y=1); 
    }
}
