/*
 * @test /nodynamiccopyright/
 * @bug 6860973
 * @summary Project Coin: underscores in literals
 *
 * @compile/fail/ref=BadUnderscoreLiterals.7.out -XDrawDiagnostics BadUnderscoreLiterals.java
 */

public class BadUnderscoreLiterals {
    int valid = 1_1;            

    int z1 = _0;                
    int z2 = 0_;                

    int i1 = _1_2_3;            
    int i2 = 1_2_3_;            

    int b1 = 0b_0;              
    int b2 = 0b0_;              

    int x1 = 0x_0;              
    int x2 = 0x0_;              

    float f1 = 0_.1;            
    float f2 = 0._1;            
    float f3 = 0.1_;            
    float f4 = 0.1_e0;          
    float f5 = 0e_1;            
    float f6 = 0e1_;            

    float xf1 = 0x_0.1p0;       
    float xf2 = 0x0_.1p0;       
    float xf3 = 0x0._1p0;       
    float xf4 = 0x0.1_p0;       
    float xf5 = 0x0p_1;         
    float xf6 = 0x0p1_;         
}

