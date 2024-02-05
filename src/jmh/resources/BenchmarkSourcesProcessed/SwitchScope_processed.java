/*
 * @test /nodynamiccopyright/
 * @bug 4725650
 * @summary Restrict scope of local classes in switch-block-group
 * @author gafter
 *
 * @compile/fail/ref=SwitchScope.out -XDrawDiagnostics SwitchScope.java
 */

public class SwitchScope {
    public static void meth(String[] args) {
        switch (args.length) {
        case 0:
            final int k;
            k = 12;
            class Local {
                int j = k;
            }
        case 1:
            Object o = new Local();
        }
    }
}
