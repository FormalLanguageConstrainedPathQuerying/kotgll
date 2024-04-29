/*
 * @test /nodynamiccopyright/
 * @bug 8312163
 * @summary Crash in dominance check when compiling unnamed patterns
 * @compile/fail/ref=T8312163.out -XDrawDiagnostics T8312163.java
 */

public class T8312163 {
    sealed interface A permits B {}
    record B() implements A {}

    record Rec(A a, A b) {}

    public void test(Rec r)
    {
        switch (r) {
            case Rec(_, _): break;
            case Rec(_, B()):   
        }

        switch (r) {
            case Rec(_, B()): break;
            case Rec(_, _):
        }

        switch (r) {
            case Rec(_, _): break;
            case Rec(_, A a):   
        }

        switch (r) {
            case Rec(_, A a): break;
            case Rec(_, _): 
        }

        switch (r) {
            case Rec(var _, var _): break;
            case Rec(var _, B()):   
        }

        switch (r) {
            case Rec(var _, B()): break;
            case Rec(var _, var _):
        }

        switch (r) {
            case Rec(var _, var _): break;
            case Rec(var _, A a):   
        }

        switch (r) {
            case Rec(var _, A a): break;
            case Rec(var _, var _): 
        }
    }
}
