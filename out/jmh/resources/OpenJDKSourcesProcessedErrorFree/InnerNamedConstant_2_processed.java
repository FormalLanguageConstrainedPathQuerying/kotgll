/*
 * @test  /nodynamiccopyright/
 * @bug 4095568 4277286 4785453
 * @summary Verify rejection of illegal static variables in inner classes.
 * @author William Maddox (maddox)
 *
 * @compile/fail/ref=InnerNamedConstant_2_A.out -XDrawDiagnostics --release 15 InnerNamedConstant_2.java
 * @compile/fail/ref=InnerNamedConstant_2_B.out -XDrawDiagnostics InnerNamedConstant_2.java
 */

public class InnerNamedConstant_2 {

    static class Inner1 {
        static int x = 1;                  
        static final int y = x * 5;        
        static final String z;             
        static {
            z = "foobar";
        }
    }

    class Inner2 {
        static int x = 1;                  
        static final String z;             
        {
            z = "foobar";                  
        }
    }


    class Inner3 {
        static final int y = Inner1.x * 5; 
    }

}
