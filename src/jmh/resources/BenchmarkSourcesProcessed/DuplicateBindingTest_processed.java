/*
 * @test /nodynamiccopyright/
 * @bug 8231827
 * @summary Basic pattern bindings scope test
 * @compile/fail/ref=DuplicateBindingTest.out -XDrawDiagnostics DuplicateBindingTest.java
 */

public class DuplicateBindingTest {

    int f;

    public static boolean main(String[] args) {
        Object o1 = "";
        Object o2 = "";

        if (args != null) {
            int s;
            if (o1 instanceof String s) { 
            }
            if (o1 instanceof String f) { 
            }
        }


        if (o1 instanceof String s && o2 instanceof String s) {} 
        if (o1 instanceof String s && !(o2 instanceof String s)) {} 
        if (!(o1 instanceof String s) && !(o2 instanceof String s)) {} 
        if (!(o1 instanceof String s) && (o2 instanceof String s)) {} 

        if (!(o1 instanceof String s) || o2 instanceof String s) {} 
        if (!(o1 instanceof String s) || !(o2 instanceof String s)) {} 
        if (o1 instanceof String s || o2 instanceof String s) {} 
        if (o1 instanceof String s || !(o2 instanceof String s)) {} 

        if (o1 instanceof String s ? o2 instanceof String s : true) {} 
        if (o1 instanceof String s ? true : o2 instanceof String s) {} 
        if (!(o1 instanceof String s) ? o2 instanceof String s : true) {} 
        if (args.length == 1 ? o2 instanceof String s : o2 instanceof String s) {} 
        if (o1 instanceof String s ? true : !(o2 instanceof String s)) {} 
        if (!(o1 instanceof String s) ? !(o2 instanceof String s) : true) {} 
        if (args.length == 1 ? !(o2 instanceof String s) : !(o2 instanceof String s)) {} 

        boolean b = o1 instanceof String s && o2 instanceof String s;
        b = o1 instanceof String s && o2 instanceof String s;
        b &= o1 instanceof String s && o2 instanceof String s;
        assert o1 instanceof String s && o2 instanceof String s;
        testMethod(o1 instanceof String s && o2 instanceof String s, false);
        b = switch (args.length) { default -> o1 instanceof String s && o2 instanceof String s; };
        b = switch (args.length) { default -> { yield o1 instanceof String s && o2 instanceof String s; } };
        if (true) return o1 instanceof String s && o2 instanceof String s;

        b = ((VoidPredicate) () -> o1 instanceof String s).get() && s.isEmpty();
        testMethod(o1 instanceof String s, s.isEmpty());
    }

    static void testMethod(boolean b1, boolean b2) {}
    interface VoidPredicate {
        public boolean get();
    }
}
