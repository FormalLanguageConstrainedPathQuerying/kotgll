/*
 * @test /nodynamiccopyright/
 * @bug 8006775
 * @summary Ensure unresolved upper bound annotation is handled correctly
 * @author Werner Dietl
 * @compile/fail/ref=BrokenAnnotation.out -XDrawDiagnostics BrokenAnnotation.java
 */


class BrokenAnnotation<T extends @BrokenAnnotation.A Object> {
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @interface A { }
}

