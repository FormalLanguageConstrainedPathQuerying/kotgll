/*
 * @test /nodynamiccopyright/
 * @bug 6356530 8191637
 * @summary -Xlint:serial does not flag abstract classes with persisent fields
 * @compile/fail/ref=SerializableAbstractClassTest.out -XDrawDiagnostics -Werror -Xlint:serial SerializableAbstractClassTest.java
 */

abstract class SerializableAbstractClassTest implements java.io.Serializable {
    abstract void m2();

    static abstract class AWithUID implements java.io.Serializable {
        private static final long serialVersionUID = 0;
        void m(){}
    }

    interface I extends java.io.Serializable {
    }

    interface IDefault extends java.io.Serializable {
        default int m() { return 1; }
    }

    interface IUID extends java.io.Serializable {
        static final long serialVersionUID = 0;
    }
}
