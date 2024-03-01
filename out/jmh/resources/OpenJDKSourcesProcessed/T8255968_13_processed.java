/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_13.out -XDrawDiagnostics T8255968_13.java
 */

class T8255968_13 {
    T8255968_13_TestMethodReference c = T8255968_13_Test::new;
}

interface T8255968_13_TestMethodReference {
    T8255968_13_Test create(int x);
}

class T8255968_13_Test {
    T8255968_13_Test(String x) {}  
    private T8255968_13_Test(int x) {}  
}
