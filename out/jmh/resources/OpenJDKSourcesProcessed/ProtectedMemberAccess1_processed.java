/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4319507
 * @summary Verify correct implementation of JLS2e 6.6.2.1
 * @author maddox
 *
 * @run compile ProtectedMemberAccess1.java
 */

import pkg.SuperClass;

class ProtectedMemberAccess1a extends SuperClass {


  int i = pi;
  int j = spi;

  int x = pm();
  int y = spm();

  pC  obj1;
  spC obj2;

  pI  obj3;
  spI obj4;

  Object o1 = (pC) null;
  Object o2 = (spC) null;

  Object o3 = (pI) null;
  Object o4 = (spI) null;

  class C1 extends pC {}
  class C2 extends spC {}

  interface I1 extends pI {}
  interface I2 extends spI {}

  static {

    spi = 2;

    int y = spm();

    pC  obj1;
    spC obj2;

    pI  obj3;
    spI obj4;

    Object o1 = (pC) null;
    Object o2 = (spC) null;

    Object o3 = (pI) null;
    Object o4 = (spI) null;

    class C2 extends spC {}


  }

  void m() {

    pi  = 1;
    spi = 2;

    int x = pm();
    int y = spm();

    pC  obj1;
    spC obj2;

    pI  obj3;
    spI obj4;

    Object o1 = (pC) null;
    Object o2 = (spC) null;

    Object o3 = (pI) null;
    Object o4 = (spI) null;

    class C1 extends pC {}
    class C2 extends spC {}


  }

  class Inner {

    int i = pi;
    int j = spi;

    int x = pm();
    int y = spm();

    pC  obj1;
    spC obj2;

    pI  obj3;
    spI obj4;

    Object o1 = (pC) null;
    Object o2 = (spC) null;

    Object o3 = (pI) null;
    Object o4 = (spI) null;

    class C1 extends pC {}
    class C2 extends spC {}



    void m() {

      pi  = 1;
      spi = 2;

      int x = pm();
      int y = spm();

      pC  obj1;
      spC obj2;

      pI  obj3;
      spI obj4;

      Object o1 = (pC) null;
      Object o2 = (spC) null;

      Object o3 = (pI) null;
      Object o4 = (spI) null;

      class C1 extends pC {}
      class C2 extends spC {}

    }
  }

}

class ProtectedMemberAccess2a extends pkg.SuperClass {


  ProtectedMemberAccess2a x =
        new ProtectedMemberAccess2a();

  static ProtectedMemberAccess2a sx =
        new ProtectedMemberAccess2a();

  int i = x.pi;
  int j = x.spi;

  int n = sx.pi;
  int m = sx.spi;

  static int sn = sx.pi;
  static int sm = sx.spi;

  int w = x.pm();
  int y = x.spm();

  int u = sx.pm();
  int v = sx.spm();

  ProtectedMemberAccess2a.pC  obj1;
  ProtectedMemberAccess2a.spC obj2;

  ProtectedMemberAccess2a.pI  obj3;
  ProtectedMemberAccess2a.spI obj4;

  Object o1 = (ProtectedMemberAccess2a.pC) null;
  Object o2 = (ProtectedMemberAccess2a.spC) null;

  Object o3 = (ProtectedMemberAccess2a.pI) null;
  Object o4 = (ProtectedMemberAccess2a.spI) null;

  class C1 extends ProtectedMemberAccess2a.pC {}
  class C2 extends ProtectedMemberAccess2a.spC {}

  interface I1 extends ProtectedMemberAccess2a.pI {}
  interface I2 extends ProtectedMemberAccess2a.spI {}

  static {

    ProtectedMemberAccess2a lx =
      new ProtectedMemberAccess2a();

    sx.pi  = 1;
    sx.spi = 2;

    lx.pi  = 1;
    lx.spi = 2;

    int n = sx.pi;
    int m = sx.spi;

    int k = lx.pi;
    int l = lx.spi;

    int u = sx.pm();
    int v = sx.spm();

    int w = lx.pm();
    int z = lx.spm();

    ProtectedMemberAccess2a.pC  obj1;
    ProtectedMemberAccess2a.spC obj2;

    ProtectedMemberAccess2a.pI  obj3;
    ProtectedMemberAccess2a.spI obj4;

    Object o1 = (ProtectedMemberAccess2a.pC) null;
    Object o2 = (ProtectedMemberAccess2a.spC) null;

    Object o3 = (ProtectedMemberAccess2a.pI) null;
    Object o4 = (ProtectedMemberAccess2a.spI) null;

    class C2 extends ProtectedMemberAccess2a.spC {}


  }

  void m() {

    ProtectedMemberAccess2a lx =
        new ProtectedMemberAccess2a();

    x.pi  = 1;
    x.spi = 2;

    sx.pi  = 1;
    sx.spi = 2;

    lx.pi  = 1;
    lx.spi = 2;

    int t = x.pm();
    int y = x.spm();

    int u = sx.pm();
    int v = sx.spm();

    int w = lx.pm();
    int z = lx.spm();

    int i = x.pi;
    int j = x.spi;

    int n = sx.pi;
    int m = sx.spi;

    int k = lx.pi;
    int l = lx.spi;

    ProtectedMemberAccess2a.pC  obj1;
    ProtectedMemberAccess2a.spC obj2;

    ProtectedMemberAccess2a.pI  obj3;
    ProtectedMemberAccess2a.spI obj4;

    Object o1 = (ProtectedMemberAccess2a.pC) null;
    Object o2 = (ProtectedMemberAccess2a.spC) null;

    Object o3 = (ProtectedMemberAccess2a.pI) null;
    Object o4 = (ProtectedMemberAccess2a.spI) null;

    class C1 extends ProtectedMemberAccess2a.pC {}
    class C2 extends ProtectedMemberAccess2a.spC {}


  }

  class Inner {

    int i = x.pi;
    int j = x.spi;

    int n = sx.pi;
    int m = sx.spi;


    int w = x.pm();
    int y = x.spm();

    int u = sx.pm();
    int v = sx.spm();

    ProtectedMemberAccess2a.pC  obj1;
    ProtectedMemberAccess2a.spC obj2;

    ProtectedMemberAccess2a.pI  obj3;
    ProtectedMemberAccess2a.spI obj4;

    Object o1 = (ProtectedMemberAccess2a.pC) null;
    Object o2 = (ProtectedMemberAccess2a.spC) null;

    Object o3 = (ProtectedMemberAccess2a.pI) null;
    Object o4 = (ProtectedMemberAccess2a.spI) null;

    class C1 extends ProtectedMemberAccess2a.pC {}
    class C2 extends ProtectedMemberAccess2a.spC {}



    void m() {

      ProtectedMemberAccess2a lx =
        new ProtectedMemberAccess2a();

      x.pi  = 1;
      x.spi = 2;

      sx.pi  = 1;
      sx.spi = 2;

      lx.pi  = 1;
      lx.spi = 2;

      int t = x.pm();
      int y = x.spm();

      int u = sx.pm();
      int v = sx.spm();

      int w = lx.pm();
      int z = lx.spm();

      int i = x.pi;
      int j = x.spi;

      int n = sx.pi;
      int m = sx.spi;

      int k = lx.pi;
      int l = lx.spi;

      ProtectedMemberAccess2a.pC  obj1;
      ProtectedMemberAccess2a.spC obj2;

      ProtectedMemberAccess2a.pI  obj3;
      ProtectedMemberAccess2a.spI obj4;

      Object o1 = (ProtectedMemberAccess2a.pC) null;
      Object o2 = (ProtectedMemberAccess2a.spC) null;

      Object o3 = (ProtectedMemberAccess2a.pI) null;
      Object o4 = (ProtectedMemberAccess2a.spI) null;

      class C1 extends ProtectedMemberAccess2a.pC {}
      class C2 extends ProtectedMemberAccess2a.spC {}


    }

  }

}


class SubClass extends ProtectedMemberAccess3a { }

class ProtectedMemberAccess3a extends pkg.SuperClass {


  SubClass x = new SubClass();

  static SubClass sx = new SubClass();

  int i = x.pi;
  int j = x.spi;

  int n = sx.pi;
  int m = sx.spi;

  static int sn = sx.pi;
  static int sm = sx.spi;

  int w = x.pm();
  int y = x.spm();

  int u = sx.pm();
  int v = sx.spm();

  SubClass.pC  obj1;
  SubClass.spC obj2;

  SubClass.pI  obj3;
  SubClass.spI obj4;

  Object o1 = (SubClass.pC) null;
  Object o2 = (SubClass.spC) null;

  Object o3 = (SubClass.pI) null;
  Object o4 = (SubClass.spI) null;

  class C1 extends SubClass.pC {}
  class C2 extends SubClass.spC {}

  interface I1 extends SubClass.pI {}
  interface I2 extends SubClass.spI {}

  static {

    SubClass lx = new SubClass();

    sx.pi  = 1;
    sx.spi = 2;

    lx.pi  = 1;
    lx.spi = 2;

    int n = sx.pi;
    int m = sx.spi;

    int k = lx.pi;
    int l = lx.spi;

    int u = sx.pm();
    int v = sx.spm();

    int w = lx.pm();
    int z = lx.spm();

    SubClass.pC  obj1;
    SubClass.spC obj2;

    SubClass.pI  obj3;
    SubClass.spI obj4;

    Object o1 = (SubClass.pC) null;
    Object o2 = (SubClass.spC) null;

    Object o3 = (SubClass.pI) null;
    Object o4 = (SubClass.spI) null;

    class C2 extends SubClass.spC {}


  }

  void m() {

    SubClass lx = new SubClass();

    x.pi  = 1;
    x.spi = 2;

    sx.pi  = 1;
    sx.spi = 2;

    lx.pi  = 1;
    lx.spi = 2;

    int t = x.pm();
    int y = x.spm();

    int u = sx.pm();
    int v = sx.spm();

    int w = lx.pm();
    int z = lx.spm();

    int i = x.pi;
    int j = x.spi;

    int n = sx.pi;
    int m = sx.spi;

    int k = lx.pi;
    int l = lx.spi;

    SubClass.pC  obj1;
    SubClass.spC obj2;

    SubClass.pI  obj3;
    SubClass.spI obj4;

    Object o1 = (SubClass.pC) null;
    Object o2 = (SubClass.spC) null;

    Object o3 = (SubClass.pI) null;
    Object o4 = (SubClass.spI) null;

    class C1 extends SubClass.pC {}
    class C2 extends SubClass.spC {}


  }

  class Inner {

    int i = x.pi;
    int j = x.spi;

    int n = sx.pi;
    int m = sx.spi;


    int w = x.pm();
    int y = x.spm();

    int u = sx.pm();
    int v = sx.spm();

    SubClass.pC  obj1;
    SubClass.spC obj2;

    SubClass.pI  obj3;
    SubClass.spI obj4;

    Object o1 = (SubClass.pC) null;
    Object o2 = (SubClass.spC) null;

    Object o3 = (SubClass.pI) null;
    Object o4 = (SubClass.spI) null;

    class C1 extends SubClass.pC {}
    class C2 extends SubClass.spC {}



    void m() {

      SubClass lx = new SubClass();

      x.pi  = 1;
      x.spi = 2;

      sx.pi  = 1;
      sx.spi = 2;

      lx.pi  = 1;
      lx.spi = 2;

      int t = x.pm();
      int y = x.spm();

      int u = sx.pm();
      int v = sx.spm();

      int w = lx.pm();
      int z = lx.spm();

      int i = x.pi;
      int j = x.spi;

      int n = sx.pi;
      int m = sx.spi;

      int k = lx.pi;
      int l = lx.spi;

      SubClass.pC  obj1;
      SubClass.spC obj2;

      SubClass.pI  obj3;
      SubClass.spI obj4;

      Object o1 = (SubClass.pC) null;
      Object o2 = (SubClass.spC) null;

      Object o3 = (SubClass.pI) null;
      Object o4 = (SubClass.spI) null;

      class C1 extends SubClass.pC {}
      class C2 extends SubClass.spC {}


    }

  }

}
