/*
 * @test  /nodynamiccopyright/
 * @bug 4294065 4785453
 * @summary Verify that invalid access modifiers on interface members don't cause crash.
 * @author maddox
 *
 * @compile/fail/ref=InterfaceMemberClassModifiers.out --diags=layout=%b:%l:%_%m InterfaceMemberClassModifiers.java
 */

public interface InterfaceMemberClassModifiers {

    Object nullWriter = null;

    class SomeClass1 implements InterfaceMemberClassModifiers {                 
        public Object getOut() {
            return nullWriter;
        }
    }

    public class SomeClass2 implements InterfaceMemberClassModifiers {          
        public Object getOut() {
            return nullWriter;
        }
    }


    protected class SomeClass3 implements InterfaceMemberClassModifiers {       
        public Object getOut() {
            return nullWriter;
        }
    }

    private class SomeClass4 implements InterfaceMemberClassModifiers {         
        public Object getOut() {
            return nullWriter;
        }
    }

}
