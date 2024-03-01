/*
 * @test /nodynamiccopyright/
 * @bug     7007615
 * @summary java_util/generics/phase2/NameClashTest02 fails since jdk7/pit/b123.
 * @author  dlsmith
 * @compile AccessibilityCheck01.java
 */

public class AccessibilityCheck01 extends p2.E {
  String m(Object o) { return "hi"; } 
  int m(String s) { return 3; } 
}
