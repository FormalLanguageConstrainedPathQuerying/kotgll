
/*
 * @test /nodynamiccopyright/
 * @bug 8272374
 * @summary doclint should report missing "body" comments
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-missing EmptyDescriptionTest.java
 * @run main DocLintTester -Xmsgs:missing -ref EmptyDescriptionTest.out EmptyDescriptionTest.java
 */

/** . */
public class EmptyDescriptionTest {

    public int f1;

    /** */
    public int f2;

    /**
     * @since 1.0
     */
    public int f3;

    /**
     * @deprecated do not use
     */
    public int f4;

    public int m1() { return 0; }

    /** */
    public int m2() { return 0; }

    /**
     * @return 0
     */
    public int m3() { return 0; }

    /**
     * @deprecated do not use
     * @return 0
     */
    public int m4() { return 0; };

    /**
     * A class containing overriding methods.
     * Overriding methods with missing/empty comments do not generate messages
     * since they are presumed to inherit descriptions as needed.
     */
    public static class Nested extends EmptyDescriptionTest {
        /** . */ Nested() { }

        @Override
        public int m1() { return 1; }

        /** */
        @Override
        public int m2() { return 1; }

        /**
         * @return 1
         */
        @Override
        public int m3() { return 1; }

    }
}
