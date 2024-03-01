/**
 * @test  /nodynamiccopyright/
 * @bug 4094658 4785453
 * @summary Test enforcement of JLS 6.6.1 and 6.6.2 rules requiring that
 * the type to which a component member belongs be accessible in qualified
 * names.
 *
 * @compile/fail/ref=QualifiedAccess_3.out -XDrawDiagnostics QualifiedAccess_3.java
 */

import pack1.P1;

class CMain {

    class Foo {
        class Bar {}
    }

    static class Baz {
        private static class Quux {
            static class Quem {}
        }
    }

    CMain z = new CMain();
    Foo x = z.new Foo();
    Foo.Bar y = x.new Bar();

    void test() {
        P1 p1 = new P1();

        /*------------------------------------*
        Baz.Quux z = null;
        Baz.Quux.Quem y = null;
        *------------------------------------*/

        P1.Foo.Bar x = null;            
        int i = p1.a.length;            
        p1.p2.privatei = 3;             
        System.out.println (p1.p2.privatei);    
    }

}
