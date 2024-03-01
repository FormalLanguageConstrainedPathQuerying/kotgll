/*
 * @test /nodynamiccopyright/
 * @bug 8223305 8226522
 * @summary Ensure proper errors are returned for yields.
 * @compile/fail/ref=WrongYieldTest.out -XDrawDiagnostics -XDshould-stop.ifError=ATTR -XDshould-stop.ifNoError=ATTR WrongYieldTest.java
 */

package t;

import t.WrongYieldTest.yield;

public class WrongYieldTest {

    class yield { }

    String[] yield = null;

    yield y;

    String[] yield() {
        return null;
    }
    String[] yield(int i) {
        return null;
    }
    String[] yield(int i, int j) {
        return null;
    }

    void LocalDeclaration1() {
       int yield;
    }
    void LocalDeclaration2() {
        int yield = 42;
    }
    void LocalDeclaration3() {
        int yield = yield + 1;
    }

    void LocalDeclaration4(int i) {
        int local = switch (i) {
            default -> {
                int yield = yield + 1;
                yield 42;
            }
        };
    }
    void LocalDeclaration5(int i) {
        int yield = 42;
        int temp = switch (i) {
            default -> {
                yield yield;
            }
        };
    }

    void YieldTypedLocals(int i) {
        yield y1 = null;
        Object y1;
        yield y1 = null;

        yield y2 = new yield();

        Object y;
        Object o = switch (i) {
            default :
                yield y = null;
        };

        Object y2;
        Object o2 = switch (i) {
            default :
                yield y2 = new yield();
        };

        Object y3 = new yield();

        final yield y4 = new yield();

        WrongYieldTest.yield y5 = new yield();

    }

    void MethodInvocation(int i) {

        String[] x = this.yield;

        yield();
        this.yield();

        yield(2);
        this.yield(2);

        yield(2, 2); 
        this.yield(2, 2);

        yield().toString();
        this.yield().toString();

        yield(2).toString();
        this.yield(2).toString();

        yield(2, 2).toString();
        this.yield(2, 2).toString();

        String str = yield(2).toString();



        yield.toString();

        this.yield.toString();

        yield[0].toString(); 

        int j = switch (i) {
            default:
                yield(2);
        };

        x = switch (i) {
            default: {
                yield x = null;
                yield null;
            }
        };
    }

    private void yieldLocalVar1(int i) {
        int yield = 0;

        yield++;
        yield--;

        yield ++i;
        yield --i;

        yield = 3;

        for (int j = 0; j < 3; j++)
            yield += 1;

        yieldLocalVar1(yield);

        yieldLocalVar1(yield().length);
        yieldLocalVar1(yield.class.getModifiers());
    }

    private void yieldLocalVar2(int i) {
        int[] yield = new int[1];

        yield[0] = 5;
    }

    private void lambda() {
        SAM s = (yield y) -> {};
    }

    interface SAM {
        public void m(Object o);
    }
}
