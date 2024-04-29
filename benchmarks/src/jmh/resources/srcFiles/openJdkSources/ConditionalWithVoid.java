/*
 * @test /nodynamiccopyright/
 * @bug 4974927 8064464
 * @summary The compiler was allowing void types in its parsing of conditional expressions.
 * @author tball
 *
 * @compile/fail/ref=ConditionalWithVoid.out -XDrawDiagnostics ConditionalWithVoid.java
 */
public class ConditionalWithVoid {
    public void test(Object o, String s) {
        System.out.println(o instanceof String ? o.hashCode() : o.wait());
        (o instanceof String ? o.hashCode() : o.wait()).toString();
        System.out.println(switch (s) {case "" -> o.hashCode(); default -> o.wait();});
        (switch (s) {case "" -> o.hashCode(); default -> o.wait();}).toString();
    }
}
