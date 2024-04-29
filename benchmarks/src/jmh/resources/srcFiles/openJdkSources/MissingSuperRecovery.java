/*
 * @test  /nodynamiccopyright/
 * @bug 4332631 4785453
 * @summary Check for proper error recovery when superclass of extended
 * class is no longer available during a subsequent compilation.
 * @author maddox
 * @build impl
 * @compile/fail/ref=MissingSuperRecovery.out -XDrawDiagnostics MissingSuperRecovery.java
 */


public class MissingSuperRecovery extends impl {
  private String workdir="";
}
