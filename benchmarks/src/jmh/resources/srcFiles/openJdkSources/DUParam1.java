/*
 * @test /nodynamiccopyright/
 * @bug 4533915
 * @summary javac should not analyze final parameters for definite assignment status
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=DUParam1.out -XDrawDiagnostics  DUParam1.java
 */

public class DUParam1 {
    public static void meth(final String[] args) {
        if (false) args = null;
    }
}
