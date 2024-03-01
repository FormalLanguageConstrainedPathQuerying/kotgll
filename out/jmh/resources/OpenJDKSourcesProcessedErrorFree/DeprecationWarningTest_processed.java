/*
 * @test /nodynamiccopyright/
 * @bug 8293519
 * @summary deprecation warnings should be emitted for uses of annotation methods inside other annotations
 * @compile/fail/ref=DeprecationWarningTest.out -deprecation -Werror -XDrawDiagnostics DeprecationWarningTest.java
 */

@interface Anno {
    @Deprecated
    boolean b() default false;
}

@Anno(b = true)
class Foo {}
