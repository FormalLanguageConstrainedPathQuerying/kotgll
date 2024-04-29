/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Verify reachability in switch expressions.
 * @compile/fail/ref=ExpressionSwitchUnreachable.out -XDrawDiagnostics ExpressionSwitchUnreachable.java
 */

public class ExpressionSwitchUnreachable {

    public static void meth() {
        int z = 42;
        int i = switch (z) {
            case 0 -> {
                yield 42;
                System.out.println("Unreachable");  
            }
            default -> 0;
        };
        i = switch (z) {
            case 0 -> {
                yield 42;
                yield 42; 
            }
            default -> 0;
        };
        i = switch (z) {
            case 0:
                System.out.println("0");
                yield 42;
                System.out.println("1");    
            default : yield 42;
        };
        i = switch (z) {
            case 0 -> 42;
            default -> {
                yield 42;
                System.out.println("Unreachable"); 
            }
        };
        i = switch (z) {
            case 0: yield 42;
            default:
                System.out.println("0");
                yield 42;
                System.out.println("1");    
        };
        i = switch (z) {
            case 0:
            default:
                System.out.println("0");
                yield 42;
                System.out.println("1");    
        };
    }


}
