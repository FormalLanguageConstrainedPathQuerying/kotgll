/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Check behavior for invalid breaks.
 * @compile/fail/ref=ExpressionSwitchBreaks2.out -XDrawDiagnostics ExpressionSwitchBreaks2.java
 */

public class ExpressionSwitchBreaks2 {
    private String print(int i, int j) {
        LOOP: while (true) {
        OUTER: switch (i) {
            case 0:
                return switch (j) {
                    case 0:
                        yield "0-0";
                    case 1:
                        break ; 
                    case 2:
                        break OUTER; 
                    case 3: {
                        int x = -1;
                        x: switch (i + j) {
                            case 0: break x;
                        }
                        yield "X";
                    }
                    case 4: return "X"; 
                    case 5: continue;   
                    case 6: continue LOOP; 
                    case 7: continue UNKNOWN; 
                    default: {
                        String x = "X";
                        x: switch (i + j) {
                            case 0: yield ""; 
                        }
                        yield "X";
                    }
                };
            case 1:
                yield "1" + undef; 
        }
        }
        j: print(switch (i) {
            case 0: yield 0;
            default: break j;
        }, 0);
        j2: print(switch (i) {
            case 0: yield 0;
            default: break j2;
        }, 0);
        return null;
    }

}
