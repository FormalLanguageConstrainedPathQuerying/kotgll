/*
 * @test /nodynamiccopyright/
 * @bug 4916607
 * @summary Test casts (errors)
 * @author gafter
 *
 * @compile/fail/ref=CastFail.out -XDrawDiagnostics CastFail.java
 */

import java.util.*;

class CastFail {


    private class AA<T> { }

    private class AB<T> extends AA<T> { }
    private class AC<T> extends AA<Vector<T>> { }
    private class AD<T> extends AA<Vector<? extends T>> { }
    private class AE<T> extends AA<Vector<? super T>> { }
    private class AF<T> extends AA<T[]> { }
    private class AG<T> extends AA<String> { }

    private void parameterTransfer() {
        Object o;

        o = (AB<String>) (AA<Number>) null; 
        o = (AC<String>) (AA<Vector<Number>>) null; 
        o = (AC<String>) (AA<Stack<String>>) null; 
        o = (AD<String>) (AA<Vector<? extends Number>>) null; 
        o = (AE<Number>) (AA<Vector<? super String>>) null; 
        o = (AF<String>) (AA<Number[]>) null; 
        o = (AG<?>) (AA<Number>) null; 
    }


    private class BA<T> { }
    private class BB<T, S> { }

    private class BC<T> extends BA<Integer> { }
    private class BD<T> extends BB<T, T> { }

    private void inconsistentMatches() {
        Object o;

        o = (BC<?>) (BA<String>) null; 
        o = (BD<String>) (BB<String, Number>) null; 
        o = (BD<String>) (BB<Number, String>) null; 
    }


    private interface CA<T> { }
    private interface CB<T> extends CA<T> { }
    private interface CC<T> extends CA<T> { }

    private class CD<T> implements CB<T> { }
    private interface CE<T> extends CC<T> { }

    private interface CF<S> { }
    private interface CG<T> { }
    private class CH<S, T> implements CF<S>, CG<T> { }
    private interface CI<S> extends CF<S> { }
    private interface CJ<T> extends CG<T> { }
    private interface CK<S, T> extends CI<S>, CJ<T> { }

    private void supertypeParameterTransfer() {
        Object o;
        CD<?> cd = (CE<?>) null; 
        CE<?> ce = (CD<?>) null; 
        o = (CE<Number>) (CD<String>) null; 

    }


    private interface DA<T> { }
    private interface DB<T> extends DA<T> { }
    private interface DC<T> extends DA<Integer> { }

    private <N extends Number, I extends Integer, R extends Runnable, S extends String> void disjointness() {
        Object o;

        o = (DA<Number>) (DA<Integer>) null; 
        o = (DA<? extends Integer>) (DA<Number>) null; 
        o = (DA<? super Number>) (DA<Integer>) null; 
        o = (DA<? extends Runnable>) (DA<? extends String>) null; 
        o = (DA<? super Number>) (DA<? extends Integer>) null; 

        o = (DA<? extends String>) (DA<I>) null; 
        o = (DA<S>) (DA<R>) null; 

        o = (DC<?>) (DA<? super String>) null; 
    }
}
