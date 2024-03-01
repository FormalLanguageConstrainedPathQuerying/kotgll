/**
 * @test  /nodynamiccopyright/
 * @bug 4241231 4785453
 * @summary Make sure the compiler scans for deprecated tag in legal
 * docComment only
 * @author Jing Qian
 *
 * @compile DeprecatedDocComment2.java
 * @compile/fail/ref=DeprecatedDocComment.out -XDrawDiagnostics -Werror -deprecation DeprecatedDocComment.java
 */




public class DeprecatedDocComment {

    public static void meth() {
      DeprecatedDocComment2.deprecatedTest1();
      DeprecatedDocComment2.deprecatedTest2();
      DeprecatedDocComment2.deprecatedTest3();
      DeprecatedDocComment2.deprecatedTest4();
      DeprecatedDocComment2.deprecatedTest5();
      DeprecatedDocComment2.deprecatedTest6();
      DeprecatedDocComment2.deprecatedTest7();
      DeprecatedDocComment2.deprecatedTest8();
    }

}
