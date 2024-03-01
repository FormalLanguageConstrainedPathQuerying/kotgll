/*
 * @test /nodynamiccopyright/
 * @bug 8003280 8062373
 * @summary Add lambda tests
 *  compiler doesn't report accessibility problem due to inaccessible target
 * @compile/fail/ref=TargetType46.out -XDrawDiagnostics TargetType46.java
 */
import java.util.*;

class TargetType46Outer {

    private interface PI {
       void m();
    }

    void m(PI p) { }
    void m(List<PI> p) { }
}

class TargetType46 {
    void test(TargetType46Outer outer) {
        outer.m(()->{}); 
        outer.m(this::g); 
        outer.m(new ArrayList<>()); 
        outer.m(new ArrayList<>() {}); 
        outer.m(Collections.emptyList()); 
    }

    void g() { }
}
