/*
 * @test /nodynamiccopyright/
 * @bug 8202056
 * @compile/ref=SerialMethodMods.out -XDrawDiagnostics -Xlint:serial SerialMethodMods.java
 */

import java.io.*;

abstract class SerialMethodMods implements Serializable {
    private static final long serialVersionUID = 42;

    void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
    }

    public void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
    }

    private static void readObjectNoData() throws ObjectStreamException {}

    public abstract Object writeReplace() throws ObjectStreamException;

    public static Object readResolve() throws ObjectStreamException {
        return null;
    }
}
