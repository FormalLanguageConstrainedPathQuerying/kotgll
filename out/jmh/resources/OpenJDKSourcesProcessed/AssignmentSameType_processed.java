/*
 * @test /nodynamiccopyright/
 * @summary Test subtyping for wildcards with the same type bound.
 *
 * @compile/fail/ref=AssignmentSameType.out -XDrawDiagnostics AssignmentSameType.java
 */

public class AssignmentSameType {

    public static void meth() {
        Ref<B> exact = null;
        Ref<? extends B> ebound = null;
        Ref<? super B> sbound = null;
        Ref<?> unbound = null;

;       exact = exact;          

        ebound = exact;         
        ebound = ebound;        

        sbound = exact;         
        sbound = sbound;        

        unbound = exact;        
        unbound = ebound;       
        unbound = sbound;       
        unbound = unbound;      

        exact = ebound;         
        exact = sbound;         
        exact = unbound;        
        ebound = sbound;        
        ebound = unbound;       
        sbound = ebound;        
        sbound = unbound;       
    }
}

class Ref<A> {}
class B {}
