/*
 * @test /nodynamiccopyright/
 * @summary Test subtyping for wildcards with related type bounds.
 *
 * @compile/fail/ref=AssignmentDifferentTypes.out -XDrawDiagnostics AssignmentDifferentTypes.java
 */

public class AssignmentDifferentTypes {

    public static void meth() {
        Ref<Der> derexact = null;
        Ref<Base> baseexact = null;
        Ref<? extends Der> derext = null;
        Ref<? extends Base> baseext = null;
        Ref<? super Der> dersuper = null;
        Ref<? super Base> basesuper = null;

        baseext = derext;       
        baseext = derexact;     
        dersuper = basesuper;   
        dersuper = baseexact;   

        derexact = baseexact;   
        baseexact = derexact;   
        derext = baseext;       
        derext = baseexact;     
        derext = basesuper;     
        baseext = dersuper;     
        basesuper = dersuper;   
        basesuper = derexact;   
    }
}

class Ref<T> {}
class Base {}
class Der extends Base {}
