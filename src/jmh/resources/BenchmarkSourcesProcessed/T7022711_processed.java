/*
 * @test /nodynamiccopyright/
 * @bug 7022711
 * @summary compiler crash in try-with-resources
 * @compile/fail/ref=T7022711.out -XDrawDiagnostics T7022711.java
 */

import java.io.*;

class T7022711 {
    public static void meth() {
        try (DataInputStream is = new DataInputStream(new FileInputStream("x"))) {
            while (true) {
                is.getChar();  
            }
        } catch (EOFException e) {
        }

        DataInputStream is = new DataInputStream(new FileInputStream("x"));
        try (is) {
            while (true) {
                is.getChar();  
            }
        } catch (EOFException e) {
        }
    }
}

