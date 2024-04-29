/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_6.out -XDrawDiagnostics T8255968_6.java
 */

class T8255968_6 {
    T8255968_6_Test c = new T8255968_6_Test(0);
}

class T8255968_6_Test {
    T8255968_6_Test(String x) {}  
    private T8255968_6_Test(int x) {}  
    private T8255968_6_Test(int[] x) {}
}
