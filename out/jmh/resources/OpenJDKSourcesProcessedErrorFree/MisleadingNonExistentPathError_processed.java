/*
 * @test /nodynamiccopyright/
 * @bug 8264258
 * @summary Unknown lookups in the java package give misleading compilation errors
 * @compile/fail/ref=MisleadingNonExistentPathError.out -XDrawDiagnostics MisleadingNonExistentPathError.java
 */
package knownpkg;

public class MisleadingNonExistentPathError {

    void classNotFound() {
        Class<?> c1 = knownpkg.NotFound.class;

        Class<?> c2 = java.lang.NotFound.class;

        Class<?> c3 = unknownpkg.NotFound.class;

        Class<?> c4 = java.NotFound.class;

        Object c5 = new java.lang();
    }
}
