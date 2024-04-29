/**
 * @test /nodynamiccopyright/
 * @bug     8295024
 * @summary Cyclic constructor error is non-deterministic and inconsistent
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;
import javax.tools.*;
public class T8295024 {

    private static final int NUM_RUNS = 10;
    private static final String EXPECTED_ERROR = """
        Cyclic.java:12:9: compiler.err.recursive.ctor.invocation
        1 error
        """;
    private static final String SOURCE = """
        public class Cyclic {
            public Cyclic(int x) {
                this((float)x);
            }
            public Cyclic(float x) {
                this((long)x);
            }
            public Cyclic(long x) {
                this((double)x);
            }
            public Cyclic(double x) {
                this((int)x);
            }
        }
        """;

    private static final JavaFileObject FILE = SimpleJavaFileObject.forSource(
            URI.create("string:

    public static void main(String[] args) throws Exception {

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final StringWriter output = new StringWriter();
        final Iterable<String> options = Collections.singleton("-XDrawDiagnostics");
        final Iterable<JavaFileObject> files = Collections.singleton(FILE);
        for (int i = 0; i < NUM_RUNS; i++)
            compiler.getTask(output, null, null, options, null, files).call();

        final String expected = IntStream.range(0, NUM_RUNS)
          .mapToObj(i -> EXPECTED_ERROR)
          .collect(Collectors.joining(""));
        final String actual = output.toString().replaceAll("\\r", "");
        assert expected.equals(actual) : "EXPECTED:\n" + expected + "ACTUAL:\n" + actual;
    }
}
