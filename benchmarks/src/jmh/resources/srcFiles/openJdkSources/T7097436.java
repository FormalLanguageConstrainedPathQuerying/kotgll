/*
 * @test /nodynamiccopyright/
 * @bug     7097436
 * @summary  ClassCastException occurs in assignment expressions without any heap pollutions
 * @compile/fail/ref=T7097436.out -Xlint:varargs -Werror -XDrawDiagnostics T7097436.java
 */

import java.util.List;

class T7097436 {
    @SafeVarargs
    static void m(List<String>... ls) {
        Object o = ls; 
        Object[] oArr = ls; 
        String s = ls; 
        Integer[] iArr = ls; 
    }
}
