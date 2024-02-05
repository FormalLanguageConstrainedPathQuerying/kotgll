/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary test invalid location of TypeUse
 * @author Mahmood Ali
 * @compile/fail/ref=Constructor.out -XDrawDiagnostics Constructor.java
 */

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

class Constructor {
  @A Constructor() { }

  @B Constructor(int x) { }


}

class Constructor2 {
  class Inner {
    @A Inner() { }
  }
}

@Target(ElementType.TYPE_USE)
@interface A { }

@Target(ElementType.TYPE_PARAMETER)
@interface B { }

