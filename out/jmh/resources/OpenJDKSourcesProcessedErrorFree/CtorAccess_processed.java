/*
 * @test /nodynamiccopyright/
 * @bug 8202056
 * @compile/ref=CtorAccess.out -XDrawDiagnostics -Xlint:serial CtorAccess.java
 * @compile/ref=empty.out      -XDrawDiagnostics               CtorAccess.java
 */

import java.io.*;

class CtorAccess {
    public CtorAccess(int i) {}

    private CtorAccess(){}

    static class SerialSubclass
        extends CtorAccess
        implements Serializable {
        private static final long serialVersionUID = 42;
        SerialSubclass() {
            super(42);
        }
    }

    class MemberSuper {
        public MemberSuper() {}
    }

    class SerialMemberSub
        extends MemberSuper
        implements Serializable {

        SerialMemberSub(){super();}
        private static final long serialVersionUID = 42;
    }
}
