/*
 * @test /nodynamiccopyright/
 * @bug 8304246
 * @summary Compiler Implementation for Unnamed patterns and variables
 * @compile -Xlint:try -XDrawDiagnostics TwrLintUnderscore.java
 */
class TwrLintUnderscore implements AutoCloseable {
    private static void test1() {
        try(TwrLintUnderscore _ = new TwrLintUnderscore()) {
        }
    }

    /**
     * The AutoCloseable method of a resource.
     */
    @Override
    public void close () {
        return;
    }
}
