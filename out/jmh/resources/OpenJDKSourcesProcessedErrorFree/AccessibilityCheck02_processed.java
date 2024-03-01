/*
 * @test /nodynamiccopyright/
 * @bug     7007615
 * @summary java_util/generics/phase2/NameClashTest02 fails since jdk7/pit/b123.
 * @author  dlsmith
 * @compile/fail/ref=AccessibilityCheck02.out -XDrawDiagnostics AccessibilityCheck02.java
 */

public class AccessibilityCheck02 extends p2.E {
  String m(Object o) { return "hi"; } 
  public int m(String s) { return 3; } 
}

