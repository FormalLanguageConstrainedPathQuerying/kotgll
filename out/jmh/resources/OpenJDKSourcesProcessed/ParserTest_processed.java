/*
 * @test /nodynamiccopyright/
 * @bug 8177466 8189146
 * @compile/ref=ParserTest9.out -XDrawDiagnostics --release 9 ParserTest.java
 * @summary Add compiler support for local variable type-inference
 * @compile/fail/ref=ParserTest.out -XDrawDiagnostics ParserTest.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.function.Function;
import java.util.List;

class ParserTest<var extends AutoCloseable> {
    static class TestClass {
        static class var { } 
    }

    static class TestInterface {
        interface var { } 
    }

    static class TestEnum {
        enum var { } 
    }

    static class TestAnno {
        @interface var { } 
    }

    @Target(ElementType.TYPE_USE)
    @interface TA { }

    @interface DA { }

    static abstract class var extends RuntimeException implements AutoCloseable { } 

    var x = null; 

    void test() {
        var[] x1 = null; 
        var x2[] = null; 
        var[][] x3 = null; 
        var x4[][] = null; 
        var[] x5 = null; 
        var x6[] = null; 
        var@TA[]@TA[] x7 = null; 
        var x8@TA[]@TA[] = null; 
        var x9 = null, y = null; 
        final @DA var x10 = m(); 
        @DA final var x11 = m(); 
    }

    var m() { 
        return null;
    }

    void test2(var x) { 
        List<var> l1; 
        List<? extends var> l2; 
        List<? super var> l3; 
        try {
            Function<var, String> f = (var x2) -> ""; 
        } catch (var ex) { } 
    }

    void test3(Object o) {
        boolean b1 = o instanceof var; 
        Object o2 = (var)o; 
    }

    void test4() throws Exception {
        try (final var resource1 = null; 
             var resource2 = null) {     
        }
    }
}
