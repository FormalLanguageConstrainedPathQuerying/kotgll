/**
 * @test  /nodynamiccopyright/
 * @bug 4222327 4785453
 * @summary Interface names for classes in the same scope should not
 * cause the compiler to crash.
 *
 * @compile/fail/ref=NameCollision.out -XDrawDiagnostics NameCollision.java
 */


public class NameCollision {
    class Runnable implements Runnable { } 
}
