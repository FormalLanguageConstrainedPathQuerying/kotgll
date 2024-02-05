/*
 * @test /nodynamiccopyright/
 * @bug 8310835
 * @compile/ref=RecordExtern.out -XDrawDiagnostics -Xlint:serial RecordExtern.java
 */

import java.io.*;

record RecordExtern(int foo) implements Externalizable {

    @Override
    public void writeExternal(ObjectOutput oo) {
        ;
    }

    @Override
    public void readExternal(ObjectInput oi) {
        ;
    }

    public void writeExternal() {
        ;
    }

    public void readExternal() {
        ;
    }


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
}
