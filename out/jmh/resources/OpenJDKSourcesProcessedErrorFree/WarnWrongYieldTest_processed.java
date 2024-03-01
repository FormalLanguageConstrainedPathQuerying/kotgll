/*
 * @test /nodynamiccopyright/
 * @bug 8223305 8226522
 * @summary Verify correct warnings w.r.t. yield
 * @compile/ref=WarnWrongYieldTest.out --release 13 -XDrawDiagnostics -XDshould-stop.ifError=ATTR -XDshould-stop.ifNoError=ATTR WarnWrongYieldTest.java
 */

package t;

import t.WarnWrongYieldTest.yield;

public class WarnWrongYieldTest {

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

    void YieldTypedLocals(int i) {
        yield y1 = null;

        yield y2 = new yield();

        Object y3 = new yield();

        final yield y4 = new yield();

        WarnWrongYieldTest.yield y5 = new yield();
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
    }

    private void yieldLocalVar1(int i) {
        int yield = 0;

        yield++;
        yield--;

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
        public void m(yield o);
    }
}
