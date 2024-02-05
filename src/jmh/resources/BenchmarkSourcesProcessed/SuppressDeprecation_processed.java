/**
 * @test  /nodynamiccopyright/
 * @bug 4216683 4346296 4656556 4785453 8164073
 * @summary New rules for when deprecation messages are suppressed
 * @author gafter
 *
 * @compile/ref=SuppressDeprecation.out -Xlint:deprecation -XDrawDiagnostics SuppressDeprecation.java
 * @compile/ref=SuppressDeprecation8.out --release 8 -Xlint:deprecation,-options -XDrawDiagnostics SuppressDeprecation.java
 */

/* Test for the contexts in which deprecations warnings should
 * (and should not) be given.  They should be given when
 * o  invoking a deprecated method from a non-deprecated one.
 * o  new X() using a deprecated constructor
 * o  super() to a deprecated constructor
 * o  extending a deprecated class.
 * But deprecation messages are suppressed as follows:
 * o  Never complain about code in the same outermost class as
 *    the deprecated entity.
 * o  Extending a deprecated class with a deprecated one is OK.
 * o  Overriding a deprecated method with a deprecated one is OK.
 * o  Code appearing in a deprecated class is OK.
 *
 */

class T {
    /** var.
     *  @deprecated . */
    int var;

    /** f.
     *  @deprecated . */
    void f() {
    }

    /** g.
     *  @deprecated . */
    void g() {
        f();
    }

    void h() {
        f();
    }

    /** T.
     *  @deprecated . */
    T() {
    }

    /** T.
     *  @deprecated . */
    T(int i) {
        this();
    }

    T(float f) {
        this();
    }

    void xyzzy() {
        new T();
        new T(1.4f);
    }
    /** plugh.
     *  @deprecated . */
    void plugh() {
        new T();
        new T(1.45f);
    }

    /** calcx..
     *  @deprecated . */
    int calcx() { return 0; }
}

class U extends T {
    /** f.
     * @deprecated . */
    void f() {
    }

    void g() { 
        super.g(); 
        var = 12; 
    }

    U() {} 

    U(int i) {
        super(i); 
    }

    U(float f) {
        super(1.3f);
    }
}

class V extends T {} 

/** W.
 * @deprecated . */
class W extends T { 
    /** W.
     * @deprecated . */
    static {
        new T(1.3f).g(); 
    }

    /** W.
     * @deprecated . */
    {
        new T(1.3f).g(); 
    }

    {
        new T(1.3f).g(); 
    }

    int x = calcx(); 

    /** y.
     * @deprecated . */
    int y = calcx();
}

/** X.
 * @deprecated . */
class X {}

class Y extends X {} 

/** Z.
 * @deprecated . */
class Z extends X {}
