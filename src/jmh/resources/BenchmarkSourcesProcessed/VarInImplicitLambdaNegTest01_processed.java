/*
 * @test /nodynamiccopyright/
 * @bug 8198512 8199327
 * @summary compiler support for local-variable syntax for lambda parameters
 * @compile/fail/ref=VarInImplicitLambdaNegTest01.out -XDrawDiagnostics VarInImplicitLambdaNegTest01.java
 * @compile/fail/ref=VarInImplicitLambdaNegTest01_source10.out -source 10 -XDrawDiagnostics VarInImplicitLambdaNegTest01.java
 */

import java.util.function.*;

class VarInImplicitLambdaNegTest01 {
    IntBinaryOperator f1 = (x, var y) -> x + y;                              
    IntBinaryOperator f2 = (var x, y) -> x + y;                              
    IntBinaryOperator f3 = (int x, var y) -> x + y;                          
    IntBinaryOperator f4 = (int x, y) -> x + y;                              

    BiFunction<String[], String, String> f5 = (var s1[], var s2) -> s2;      

    IntBinaryOperator f6 = (var x, var y) -> x + y;                          
    BiFunction<Function<String, String>, String, String> f = (Function<String, String> s1, String s2) -> s2; 
}
