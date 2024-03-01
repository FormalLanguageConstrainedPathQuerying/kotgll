/*
 * @test  /nodynamiccopyright/
 * @bug 6911256 6964740
 * @author Joseph D. Darcy
 * @summary Verify bad TWRs don't compile
 * @compile/fail/ref=BadTwr.out -XDrawDiagnostics BadTwr.java
 */

public class BadTwr implements AutoCloseable {
    public static void meth(String... args) {
        try(BadTwr r1 = new BadTwr(); BadTwr r1 = new BadTwr()) {
            System.out.println(r1.toString());
        }

        try(BadTwr args = new BadTwr()) {
            System.out.println(args.toString());
            final BadTwr thatsIt = new BadTwr();
            thatsIt = null;
        }

        try(BadTwr name = new BadTwr()) {
            try(BadTwr name = new BadTwr()) {
                System.out.println(name.toString());
            }
        }

    }

    public void close() {
        ;
    }
}
