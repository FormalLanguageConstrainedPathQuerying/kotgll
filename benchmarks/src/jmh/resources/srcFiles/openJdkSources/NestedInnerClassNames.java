/*
 * @test  /nodynamiccopyright/
 * @bug 4037746 4277279 4350658 4785453
 * @summary Verify that an inner class cannot have the same simple name as an enclosing one.
 * @author William Maddox (maddox)
 *
 * @compile/fail/ref=NestedInnerClassNames.out -XDrawDiagnostics NestedInnerClassNames.java
 */

/*
 * This program should compile with errors as marked.
 */

public class NestedInnerClassNames {

    class NestedInnerClassNames {}              

    void m1() {
        class NestedInnerClassNames {}          
    }

    class foo {
        class foo { }                           
    }

    void m2 () {
        class foo {
            class foo { }                       
        }
    }

    class bar {
        class foo { }
        class NestedInnerClassNames {}          
    }

    void m3() {
        class bar {
            class foo { }
            class NestedInnerClassNames {}      
        }
    }

    class baz {
        class baz {                             
            class baz { }                       
        }
    }

    void m4() {
        class baz {
            class baz {                         
                class baz { }                   
            }
        }
    }

    class foo$bar {
        class foo$bar {                         
            class foo { }
            class bar { }
        }
    }

    void m5() {
        class foo$bar {
            class foo$bar {                     
                class foo { }
                class bar { }
            }
        }
    }

    class $bar {
        class foo {
            class $bar { }                      
        }
    }

    void m6() {
        class $bar {
            class foo {
                class $bar { }                  
            }
        }
    }

    class bar$bar {
        class bar {
            class bar{ }                       
        }
    }

    void m7() {
        class bar$bar {
            class bar {
                class bar{ }                   
            }
        }
    }


    class foo$foo { }                           

}
