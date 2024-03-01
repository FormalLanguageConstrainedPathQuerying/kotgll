/*
 * @test /nodynamiccopyright/
 * @bug 8231827
 * @summary Basic tests for bindings from instanceof - tests for merging pattern variables
 * @compile/fail/ref=BindingsTest1Merging.out -XDrawDiagnostics BindingsTest1Merging.java
 */

public class BindingsTest1Merging {
    public static boolean Ktrue() { return true; }
    public static void meth() {
        Object o1 = "hello";
        Integer i = 42;
        Object o2 = i;
        Object o3 = "there";

        if (!(o1 instanceof String s) && !(o1 instanceof String s)) {

        } else {
            s.length();
        }

        if (o1 instanceof String s || o3 instanceof String s){
            System.out.println(s); 
        }

        if (Ktrue() ? o2 instanceof Integer x : o2 instanceof Integer x) {
            x.intValue();
        }

        if (o1 instanceof String s ? true : o1 instanceof String s) {
            s.length();
        }

        if (!(o1 instanceof String s) ? (o1 instanceof String s) : true) {
            s.length();
        }

        if (Ktrue() ? !(o2 instanceof Integer x) : !(o2 instanceof Integer x)){
        } else {
            x.intValue();
        }

        if (o1 instanceof String s ? true : !(o1 instanceof String s)){
        } else {
            s.length();
        }

        if (!(o1 instanceof String s) ? !(o1 instanceof String s) : true){
        } else {
            s.length();
        }

        L3: {
            if ((o1 instanceof String s) || (o3 instanceof String s)) {
                s.length();
            } else {
                break L3;
            }
            s.length();
        }

        System.out.println("BindingsTest1Merging complete");
    }
}
