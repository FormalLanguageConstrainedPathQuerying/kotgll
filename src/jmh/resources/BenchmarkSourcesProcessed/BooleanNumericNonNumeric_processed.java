/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Verify the type of a conditional expression with nested switch expression is computed properly
 * @compile/fail/ref=BooleanNumericNonNumeric.out -XDrawDiagnostics BooleanNumericNonNumeric.java
 */

public class BooleanNumericNonNumeric {

    private void test(boolean b, int i) {
        int r1 = 1 + (b ? switch (i) { 
            default -> true;
        } : false);
        int r2 = 1 + (b ? switch (i) { 
            default -> 0;
        } : 1);
        (b ? switch (i) { 
            default -> 0;
        } : 1).toString();
        (b ? switch (i) { 
            case 0 -> true;
            default -> 0;
        } : 1).toString();
    }

}
