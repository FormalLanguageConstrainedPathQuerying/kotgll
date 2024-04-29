/*
 * @test  /nodynamiccopyright/
 * @bug 4336282 4785453
 * @summary Verify that extending an erray class does not crash the compiler.
 *
 * @compile/fail/ref=ExtendArray.out -XDrawDiagnostics ExtendArray.java
 */


public class ExtendArray extends Object[] {}
