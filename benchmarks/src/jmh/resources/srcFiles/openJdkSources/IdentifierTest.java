/*
 * @test   /nodynamiccopyright/
 * @bug    8007401 8007427 8061549
 * @author sogoel
 * @summary Test generation of warnings when '_' is used an identifier
 * @compile/fail/ref=IdentifierTest8.out --release 8 -Werror -XDrawDiagnostics -Xlint:-options IdentifierTest.java
 * @compile/fail/ref=IdentifierTest9.out --release 9 -XDrawDiagnostics IdentifierTest.java
 * @compile/fail/ref=IdentifierTest22.out -XDrawDiagnostics IdentifierTest.java
 */

import java.util.List;

/*
 * This test checks for the generation of warnings when '_' is used as an
 * identifier in following cases:
 * package name, class name, class member names including constructor
 * cass members access using class object or this
 * loops: for, enhanced-for, while, do-while
 * arrays,
 * switch,
 * annotations, element=value pair
 * try-catch,
 * enum
 * break + identifier
 * continue + identifier
 * type-bounds
 * Above cases for identifier occurrences have been identified from JLS v3.
 *
 */

public class IdentifierTest {
    class _UnderscorePrefix {}
    class Underscore_Infix {}
    class UnderscorePostfix_ {}
    class __ {}

    static final int _prefix = 10;
    List<String> postfix_;

    class _ {
        String in_fix;
        public _() {
            String _ = "_";
            in_fix = _;
        }

        public void testClassMembersAccess(String[] _args) {
            _ _ = new _();
            _.testTryCatch();
            _.in_fix = "__";
        }

        public void testTryCatch() {
            try {
                int _ = 30/0;
            } catch (ArithmeticException _) {
                System.out.println("Got Arithmentic exception " + _);
            }
        }
    }

    class TestMisc {
        int _;
        void _ () {
            this._ = 5;
        }

        public void testClassMemberAccess(String[] args) {
            TestMisc _ = new TestMisc();
            _._ = 10;
            _._();
        }
    }

    class TestTypeBounds {
        <_ extends Object> void test(_ t) {}
    }

    static class TestEnum {
        enum _ {
            _MONDAY, _TUESDAY, _WEDNESDAY, _THURSDAY, _FRIDAY,
            _SATURDAY, _SUNDAY;
        }

        void foo() {
            for(_ _day : _.values()) {
                switch(_day) {
                case _SATURDAY:
                case _SUNDAY:
                    System.out.println("Weekend is here!");
                    break;
                default:
                    System.out.println("Weekday is here!");
                    break;
                }
            }
        }
    }

    static class TestAnno {
        @interface _ {
            String _name();
            int _id();
        }
        @_(_name ="m",_id=1)
        public void m(int arg) {}

        @interface MyAnno {
            int _();
        }
        @MyAnno(_='1')
        public void m2() {}
    }

    public void testLoop() {
        for(int _ = 0; _ < 5; ++_) {
            System.out.println("_=" + _ + " ");
        }

        int _ = 0;
        while(_ <= 5) {
            _++;
        }

        do {
            --_;
        } while(_ > 0);
    }

    public void testArraysEnhancedForLoop() {
        String _[] = {"A","B","C","D"};

        for(String _s : _ ) {
            System.out.println("_s="+_s);
        }
    }

    public void testLabels() {
        int j = 0;
    _:
        for (int i = 0; i <= 5; i++) {
            while( j > 4 ) {
                j++;
                continue _;
            }
            break _;
        }
    }
}

interface _ {
    void mI();
}

