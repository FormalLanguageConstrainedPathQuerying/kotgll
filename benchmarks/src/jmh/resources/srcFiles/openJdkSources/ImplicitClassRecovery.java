/**
 * @test /nodynamiccopyright/
 * @compile/fail/ref=ImplicitClassRecovery.out -XDrawDiagnostics --enable-preview --source ${jdk.version} ImplicitClassRecovery.java
 */
public void main() {
    System.err.println("Hello!")
}
