/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6603011
 * @summary long/int division by constant
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 *
 * @run main/othervm/timeout=480 -Xcomp -Xbatch -XX:-Inline compiler.c2.Test6603011
 */


package compiler.c2;

import jdk.test.lib.Utils;

public class Test6603011 implements Runnable {
  static class s {
    static int  divi(int  dividend, int  divisor) { return dividend / divisor; }
    static int  modi(int  dividend, int  divisor) { return dividend % divisor; }
    static long divl(long dividend, long divisor) { return dividend / divisor; }
    static long modl(long dividend, long divisor) { return dividend % divisor; }
  }
  static final boolean VERBOSE = true;

  static final int DIVISOR;
  static {
    int value = 0;
    try {
      value = Integer.decode(System.getProperty("divisor"));
    } catch (Throwable e) {
    }
    DIVISOR = value;
  }

  public int divbyI (int dividend)   { return dividend / DIVISOR; }
  public int modbyI (int dividend)   { return dividend % DIVISOR; }
  public long divbyL (long dividend) { return dividend / DIVISOR; }
  public long modbyL (long dividend) { return dividend % DIVISOR; }

  public int divisor() { return DIVISOR; }

  public boolean checkI (int dividend) {
    int quo = divbyI(dividend);
    int rem = modbyI(dividend);
    int quo0 = s.divi(dividend, divisor());
    int rem0 = s.modi(dividend, divisor());

    if (quo != quo0 || rem != rem0) {
      if (VERBOSE) {
        System.out.println("Computed: " + dividend + " / " + divisor() + " = " +
                           quo  + ", " + dividend + " % " + divisor() + " = " + rem );
        System.out.println("expected: " + dividend + " / " + divisor() + " = " +
                           quo0 + ", " + dividend + " % " + divisor() + " = " + rem0);
        if (rem != 0 && (rem ^ dividend) < 0) {
          System.out.println("  rem & dividend have different signs");
        }
        if (java.lang.Math.abs(rem) >= java.lang.Math.abs(divisor())) {
          System.out.println("  remainder out of range");
        }
        if ((quo * divisor()) + rem != dividend) {
          System.out.println("  quotien/remainder invariant broken");
        }
      }
      return false;
    }
    return true;
  }

  public boolean checkL (long dividend) {
    long quo = divbyL(dividend);
    long rem = modbyL(dividend);
    long quo0 = s.divl(dividend, divisor());
    long rem0 = s.modl(dividend, divisor());

    if (quo != quo0 || rem != rem0) {
      if (VERBOSE) {
        System.out.println("Computed: " + dividend + " / " + divisor() + " = " +
                           quo  + ", " + dividend + " % " + divisor() + " = " + rem );
        System.out.println("expected: " + dividend + " / " + divisor() + " = " +
                           quo0 + ", " + dividend + " % " + divisor() + " = " + rem0);
        if (rem != 0 && (rem ^ dividend) < 0) {
          System.out.println("  rem & dividend have different signs");
        }
        if (java.lang.Math.abs(rem) >= java.lang.Math.abs(divisor())) {
          System.out.println("  remainder out of range");
        }
        if ((quo * divisor()) + rem != dividend) {
          System.out.println(" (" + quo + " * " + divisor() + ") + " + rem + " != "
                             + dividend);
        }
      }
      return false;
    }
    return true;
  }

  public void run() {
    if (divisor() == 0) return;

    int start = -1024;
    int end = 1024;

    int wrong = 0;
    int total = 0;

    outerloop:
    for (int i = start; i <= end; i++) {
      for (int s = 0; s < 32; s += 4) {
        total++;
        int dividend = i << s;
        if (!checkI(dividend)) {
          wrong++;
        }
      }
    }
    if (wrong > 0) {
      System.out.println("divisor " + divisor() + ": " +
                         wrong + "/" + total + " wrong int divisions");
    }

    wrong = 0;
    total = 0;

    outerloop:
    for (int i = start; i <= end; i++) {
      for (int s = 0; s < 64; s += 4) {
        total++;
        long dividend = ((long)i) << s;
        if (!checkL(dividend)) {
          wrong++;
        }
      }
    }
    if (wrong > 0) {
      System.out.println("divisor " + divisor() + ": " +
                         wrong + "/" + total + " wrong long divisions");
    }

  }

  public static void test_divisor(int divisor,
                                  ClassLoader apploader) throws Exception {
    System.setProperty("divisor", "" + divisor);
    ClassLoader loader
                = Utils.getTestClassPathURLClassLoader(apploader.getParent());
    Class c = loader.loadClass(Test6603011.class.getName());
    Runnable r = (Runnable)c.newInstance();
    r.run();
  }

  public static void main(String[] args) throws Exception {
    Class cl = Test6603011.class;
    ClassLoader apploader = cl.getClassLoader();


    for (int i = -100; i <= 100; i++) {
      test_divisor(i, apploader);
    }

    test_divisor(101, apploader);
    test_divisor(400, apploader);
    test_divisor(1000, apploader);
    test_divisor(3600, apploader);
    test_divisor(9973, apploader);
    test_divisor(86400, apploader);
    test_divisor(1000000, apploader);
  }

}
