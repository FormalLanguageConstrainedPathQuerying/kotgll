/*
 * @test /nodynamiccopyright/
 * @bug 8202056
 * @compile/ref=InstanceField.out -XDrawDiagnostics -Xlint:serial InstanceField.java
 */

import java.io.*;

class IntanceField implements Serializable {
    private static final long serialVersionUID = 42;


    private Object foo;

    private Object[] foos;

    private Thread[][] ArrayOfArrayOfThreads;


    private static Object bar;

    private static Object[] bars;

    private int baz;

    private double[] quux;

    static class NestedInstance implements Serializable {
        private static final long serialVersionUID = 24;

        private static final ObjectStreamField[] serialPersistentFields = {};

        private Object foo;
    }
}
