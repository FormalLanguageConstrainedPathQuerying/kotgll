/*
 * @test /nodynamiccopyright/
 * @bug 4901280
 * @summary name lookup scope for annotations
 * @author gafter
 *
 * @compile/fail/ref=Scope.out -XDrawDiagnostics  Scope.java
 */

package annotation.scope;

@A(red) 
enum E {
    red, green, blue;
}

@interface A {
    E value();
}
