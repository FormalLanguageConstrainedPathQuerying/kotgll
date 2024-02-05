/*
 * @test /nodynamiccopyright/
 * @bug 8202056 8310861
 * @compile/ref=RecordSerial.out -XDrawDiagnostics -Xlint:serial RecordSerial.java
 */

import java.io.*;

record RecordSerial(int foo) implements Serializable {

    private static final long serialVersionUID = 42;

    private static final ObjectStreamField[] serialPersistentFields = {};

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
    }

    private Object writeReplace() throws ObjectStreamException {
        return null;
    }

    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
    }

    private void readObjectNoData() throws ObjectStreamException {
        return;
    }

    private Object readResolve() throws ObjectStreamException {
        return null;
    }

    public void writeExternal(ObjectOutput oo) {
        ;
    }

    public void readExternal(ObjectInput oi) {
        ;
    }
}
