/*
 * @test /nodynamiccopyright/
 * @bug 8231827
 * @summary Basic pattern bindings scope test
 * @compile/fail/ref=MatchBindingScopeTest.out -XDrawDiagnostics MatchBindingScopeTest.java
 */
public class MatchBindingScopeTest {

    static Integer i = 42;
    static String s = "Hello";
    static Object o1 = s;
    static Object o2 = i;

    public static void meth() {

        if (o1 instanceof String j && j.length() == 5) { 
            System.out.println(j); 
        } else {
            System.out.println(j); 
        }

        if (o1 instanceof String j && o2 instanceof Integer j) {
        }

        if (o1 instanceof String j && j.length() == 5 && o2 instanceof Integer k && k == 42) { 
            System.out.println(j); 
            System.out.println(k); 
        } else {
            System.out.println(j); 
            System.out.println(k); 
        }

        if (o1 instanceof String j || j.length() == 5) { 
            System.out.println(j); 
        }

        if (o1 instanceof String j || o2 instanceof Integer j) { 
            System.out.println(j);
        } else {
            System.out.println(j); 
        }

        while (o1 instanceof String j && j.length() == 5) { 
            System.out.println(j); 
        }

        while (o1 instanceof String j || true) {
            System.out.println(j); 
        }

        for (; o1 instanceof String j; j.length()) { 
            System.out.println(j); 
        }

        for (; o1 instanceof String j || true; j.length()) { 
            System.out.println(j); 
        }

        int x = o1 instanceof String j ?
                      j.length() : 
                      j.length();  

        x = !(o1 instanceof String j) ?
                      j.length() : 
                      j.length();  
    }
}
