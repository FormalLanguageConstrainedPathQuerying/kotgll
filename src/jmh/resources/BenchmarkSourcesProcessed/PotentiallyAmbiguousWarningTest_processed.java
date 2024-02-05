/*
 * @test /nodynamiccopyright/
 * @bug 8024947 8026369
 * @summary javac should issue the potentially ambiguous overload warning only
 * where the problem appears
 * @compile/fail/ref=PotentiallyAmbiguousWarningTest.out -XDrawDiagnostics -Werror -Xlint:overloads PotentiallyAmbiguousWarningTest.java
 */

import java.util.function.*;

public interface PotentiallyAmbiguousWarningTest {

    interface I1 {
        void foo(Consumer<Integer> c);
        void foo(IntConsumer c);
    }

    class C1 {
        void foo(Consumer<Integer> c) { }
        void foo(IntConsumer c) { }
    }

    interface I2 {
        void foo(Consumer<Integer> c);
    }

    interface J1 extends I2 {
        void foo(IntConsumer c);
    }

    interface I3 extends I1 {}

    interface I4 extends I1 {
        void foo(IntConsumer c);
    }

    class C2 {
        void foo(Consumer<Integer> c) { }
    }

    class D1 extends C2 {
        void foo(IntConsumer c) { }
    }

    class C3 implements I2 {
        public void foo(Consumer<Integer> c) { }
        public void foo(IntConsumer c) { }
    }

    class C4 extends C1 {}

    class C5 extends C1 {
        void foo(IntConsumer c) {}
    }

    interface I5<T> {
        void foo(T c);
    }

    interface J2 extends I5<IntConsumer> {
        void foo(Consumer<Integer> c);
    }


    interface I6 {
        void foo(Consumer<Integer> c);
    }

    interface I7 {
        void foo(IntConsumer c);
    }

    interface I8 extends I6, I7 { }

    interface I9 extends I8 { }

    interface I10<T> {
        void foo(Consumer<Integer> c);
        void foo(T c);
    }

    interface I11 extends I10<IntConsumer> { }

    interface I12<T> extends Consumer<T>, IntSupplier {
        interface OfInt extends I12<Integer>, IntConsumer {
            @Override
            void accept(int value);
            default void accept(Integer i) { }
        }
        @Override
        default int getAsInt() { return 0; }
    }

    abstract static class C6<T> implements I12.OfInt { }

    default <U> Object foo() {
        return new C6<U>() {
            @Override
            public void accept(int value) { }
        };
    }

    interface I13 extends I8 {
        @Override
        void foo(Consumer<Integer> c);
        @Override
        void foo(IntConsumer c);
    }
    interface I14 extends I8 {
        @Override
        void foo(IntConsumer c);
    }

    @SuppressWarnings("overloads")
    interface I15 extends I8 { }        

    interface I16 extends I2 {
        @SuppressWarnings("overloads")
        void foo(IntConsumer c);        
    }
}
