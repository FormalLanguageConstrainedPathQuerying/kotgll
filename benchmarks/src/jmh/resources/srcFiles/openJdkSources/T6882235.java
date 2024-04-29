/*
 * @test /nodynamiccopyright/
 * @bug 6882235
 * @summary invalid exponent causes silent javac crash
 *
 * @compile/fail/ref=T6882235.out -XDrawDiagnostics T6882235.java
 */

class T6882235 {
    int i = ;           
    float f = 0e*;      
    int j = ;           
}
