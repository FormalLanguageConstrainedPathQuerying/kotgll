/*
 * @test /nodynamiccopyright/
 * @bug 8164399
 * @summary inference of thrown variable does not work correctly
 * @compile/fail/ref=T8164399b.out -XDrawDiagnostics T8164399b.java
 */
class T8164399b<X extends Throwable> {
    <T extends Throwable> void m(Class<? super T> arg) throws T {}
    <T extends X> void g() throws T {}

    void test() {
        m(RuntimeException.class); 
        m(Exception.class); 
        m(Throwable.class); 
        m(java.io.Serializable.class); 
        m(Object.class); 
        m(Runnable.class); 
        T8164399b<? super Exception> x = null;
        x.g(); 
    }
}
