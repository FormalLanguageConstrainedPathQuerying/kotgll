/*
 * @test /nodynamiccopyright/
 * @bug 4916607
 * @summary Test casts (warning)
 * @author gafter
 *
 * @compile/ref=CastWarn.out -XDrawDiagnostics -Xlint:unchecked CastWarn.java
 */

import java.util.*;

class CastWarn {


    private interface DA<T> { }
    private interface DB<T> extends DA<T> { }
    private interface DC<T> extends DA<Integer> { }

    private <N extends Number, I extends Integer, R extends Runnable, S extends String> void disjointness() {
        Object o;

        o = (DA<? extends Runnable>) (DA<? extends Number>) null; 
        o = (DA<? super Integer>) (DA<? extends Number>) null; 
        o = (DA<? super String>) (DA<? super Number>) null; 

        o = (DA<? extends Integer>) (DA<N>) null; 
        o = (DA<I>) (DA<? extends Number>) null; 
        o = (DA<N>) (DA<? extends Integer>) null; 
        o = (DA<N>) (DA<? extends Runnable>) null; 

        o = (DA<N>) (DA<I>) null; 
        o = (DA<N>) (DA<R>) null; 

        o = (DA<Number>) (DB) null; 
        o = (DA<? extends Number>) (DB) null; 
        o = (DB<Number>) (DA) null; 
        o = (DB<? extends Number>) (DA) null; 
    }
}
