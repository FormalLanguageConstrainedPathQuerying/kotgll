/*
 * @test /nodynamiccopyright/
 * @bug 8177466
 * @summary Add compiler support for local variable type-inference
 * @compile/fail/ref=SelfRefTest.out -XDrawDiagnostics SelfRefTest.java
 */

import java.util.function.Function;

class SelfRefTest {

        int q() { return 42; }
    int m(int t) { return t; }

    void test(boolean cond) {
       var x = cond ? x : x; 
       var y = (Function<Integer, Integer>)(Integer y) -> y; 
       var z = (Runnable)() -> { int z2 = m(z); }; 
       var w = new Object() { int w = 42; void test() { int w2 = w; } }; 
       int u = u; 
       int q = q(); 
    }
}
