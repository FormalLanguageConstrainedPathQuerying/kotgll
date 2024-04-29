/*
 * Copyright (c) 2009 SAP SE. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @bug 6880034
 * @summary SIGBUS during deoptimisation at a safepoint on 64bit-SPARC
 *
 * @run main/othervm -Xcomp -Xbatch
 *    -XX:+PrintCompilation
 *    -XX:CompileCommand=compileonly,compiler.c2.Test6880034::deopt_compiledframe_at_safepoint
 *    compiler.c2.Test6880034
 */

package compiler.c2;


public class Test6880034 {
  static class A {
    public int doSomething() {
      return 0;
    }
  }

  static class B extends A {
    public B() {}
    public int doSomething() {
      return 1;
    }
  }

  static class G {
    public static volatile A a = new A();

    public static void setAtoB() {
      try {
        a =  (A) ClassLoader.
                getSystemClassLoader().
                loadClass("B").
                getConstructor(new Class[] {}).
                newInstance(new Object[] {});
      }
      catch (Exception e) {
        System.out.println(e);
      }
    }
  }

  public static volatile boolean is_in_loop = false;
  public static volatile boolean stop_while_loop = false;

  public static double deopt_compiledframe_at_safepoint() {
    int i = G.a.doSomething();

    double local1 = 1;
    double local2 = 2;
    double local3 = 3;
    double local4 = 4;
    double local5 = 5;
    double local6 = 6;
    double local7 = 7;
    double local8 = 8;

    long k = 0;
    while (!stop_while_loop) {
      if (k ==  1) local1 += i;
      if (k ==  2) local2 += i;
      if (k ==  3) local3 += i;
      if (k ==  4) local4 += i;
      if (k ==  5) local5 += i;
      if (k ==  6) local6 += i;
      if (k ==  7) local7 += i;
      if (k ==  8) local8 += i;

      if (k++ == 20000) is_in_loop = true;
    }

    return
      local1 + local2 + local3 + local4 +
      local5 + local6 + local7 + local8 + i;
  }

  public static void main(String[] args) {

    G g = new G();

    new Thread() {
      public void run() {
        while (!is_in_loop) {
        }
        G.setAtoB();
        stop_while_loop = true;
      }
    }.start();

    double retVal = deopt_compiledframe_at_safepoint();

    System.out.println(retVal == 36 ? "OK" : "ERROR : " + retVal);
    if (retVal != 36) throw new RuntimeException();
  }
}
