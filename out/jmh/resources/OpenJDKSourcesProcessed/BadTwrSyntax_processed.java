/*
 * @test  /nodynamiccopyright/
 * @bug 6911256 6964740
 * @author Joseph D. Darcy
 * @summary Verify bad TWRs don't compile
 * @compile/fail/ref=BadTwrSyntax.out -XDrawDiagnostics BadTwrSyntax.java
 */

import java.io.IOException;
public class BadTwrSyntax implements AutoCloseable {
    public static void meth() {
        try(BadTwr twrflow = new BadTwr();;) {
            System.out.println(twrflow.toString());
        }

        try(BadTwr twrflow = new BadTwr();) {
            System.out.println(twrflow.toString());
        }
    }

    public void close() {
        ;
    }
}
