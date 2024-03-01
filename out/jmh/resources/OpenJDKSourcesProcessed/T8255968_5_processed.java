/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_5.out -XDrawDiagnostics T8255968_5.java
 */

class T8255968_5 {
    T8255968_5_Test c = new T8255968_5_Test(0);
}

class T8255968_5_Test {
    private T8255968_5_Test(int x) {}  
    T8255968_5_Test(String x) {}  
    private T8255968_5_Test(int[] x) {}
}
