/*
 * @test /nodynamiccopyright/
 * @bug 8202056
 * @compile/ref=ImproperSerialPF.out -XDrawDiagnostics -Xlint:serial ImproperSerialPF.java
 */

import java.io.*;

class ImproperSerialPF implements Serializable {
    public /*instance*/ Object serialPersistentFields = Boolean.TRUE;

    private static final long serialVersionUID = 42;

    static class LiteralNullSPF implements Serializable {
        private static final ObjectStreamField[] serialPersistentFields = null;

        private static final long serialVersionUID = 42;
    }

    static class CastedNullSPF implements Serializable {
        private static final ObjectStreamField[] serialPersistentFields =
            (ObjectStreamField[])null;

        private static final long serialVersionUID = 42;
    }

    static class ConditionalNullSPF implements Serializable {
        private static final ObjectStreamField[] serialPersistentFields =
            (true ? null : null);

        private static final long serialVersionUID = 42;
    }
}
