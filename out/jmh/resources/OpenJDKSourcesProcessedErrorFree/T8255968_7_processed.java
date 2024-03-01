/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_7.out -XDrawDiagnostics T8255968_7.java
 */

class T8255968_7 {
    T8255968_7_Test c = new T8255968_7_Test(0);
}

class T8255968_7_Test {
    T8255968_7_Test(String x) {}  
    private T8255968_7_Test(int[] x) {}
    private T8255968_7_Test(int x) {}  
}
