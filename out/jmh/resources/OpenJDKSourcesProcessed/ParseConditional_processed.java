/*
 * @test /nodynamiccopyright/
 * @bug 4092958
 * @summary The compiler was too permissive in its parsing of conditional
 *          expressions.
 * @author turnidge
 *
 * @compile/fail/ref=ParseConditional.out -XDrawDiagnostics ParseConditional.java
 */

public class ParseConditional {
    public static void meth() {
        boolean condition = true;
        int a = 1;
        int b = 2;
        int c = 3;
        int d = 4;
        a = condition ? b = c : c = d;
    }
}
