/*
 * @test /nodynamiccopyright/
 * @bug 8043893
 * @summary Inference doesn't report error on incompatible upper bounds
 *
 *
 * @compile/fail/ref=T8043893.out -XDrawDiagnostics T8043893.java
 */

class T8043893<X> {

    interface S1<U> { }

    interface S2<U> { }

    interface T0 { }

    interface T1 extends S1<Number>, S2<Number> { }

    interface T2 extends S1<Integer>, S2<Integer> { }

    interface T3 extends S1<Number>, S2<Integer> { }

    interface T4 extends S1<Number> { }

    interface T5 extends S1<Integer> { }

    <Z extends T1> void m_intersection(T8043893<? super Z> a) { }

    <Z extends T4> void m_class(T8043893<? super Z> a) { }

    void test() {
        m_intersection(new T8043893<T1>()); 
        m_intersection(new T8043893<T2>()); 
        m_intersection(new T8043893<T3>()); 
        m_intersection(new T8043893<T0>()); 
        m_class(new T8043893<T4>()); 
        m_class(new T8043893<T5>()); 
        m_class(new T8043893<T0>()); 
    }
}
