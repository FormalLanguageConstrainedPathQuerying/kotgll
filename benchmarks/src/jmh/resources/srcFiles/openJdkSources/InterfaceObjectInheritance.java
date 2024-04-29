/*
 * @test /nodynamiccopyright/
 * @bug 4526026
 * @summary javac allows access to interface members inherited protected from Object
 * @author gafter
 *
 * @compile/fail/ref=InterfaceObjectInheritance.out -XDrawDiagnostics  InterfaceObjectInheritance.java
 */

interface InterfaceObjectInheritance {
    class Inner {
        static void bar(InterfaceObjectInheritance i) {
            try {
                i.finalize();
            } catch (Throwable t) {
            }
        }
    }
}
