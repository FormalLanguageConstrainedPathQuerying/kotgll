/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary static field access isn't a valid location
 * @author Mahmood Ali
 * @compile/fail/ref=StaticFields.out -XDrawDiagnostics StaticFields.java
 */
import java.lang.annotation.*;

class C {
  static int f;
  static {
    @A C.f = 1;
  }
  int a = @A C.f;
  static int f() { return @A C.f; }
  public static void meth() {
    int a = @A C.f;
  }
}

@Target(ElementType.TYPE_USE)
@interface A { }
