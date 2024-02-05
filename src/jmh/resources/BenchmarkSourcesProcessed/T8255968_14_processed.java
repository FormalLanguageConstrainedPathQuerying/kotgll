/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_14.out -XDrawDiagnostics T8255968_14.java
 */

class T8255968_14 {
    T8255968_14_TestMethodReference c = T8255968_14_Test::new;
}

interface T8255968_14_TestMethodReference {
    T8255968_14_Test create(int x);
}

class T8255968_14_Test {
    private T8255968_14_Test(int x) {}  
    T8255968_14_Test(String x) {}  
    private T8255968_14_Test(int[] x) {}
}
