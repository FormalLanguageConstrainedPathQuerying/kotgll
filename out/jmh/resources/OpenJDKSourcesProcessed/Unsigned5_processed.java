/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.utilities;

import java.io.PrintStream;

import sun.jvm.hotspot.debugger.*;

/**
 * Decompression algorithm from utilities/unsigned5.hpp.
 */
public class Unsigned5 {
  public static final int LogBitsPerByte = 3;
  public static final int BitsPerByte = 1 << 3;

  private static final int lg_H = 6;     
  private static final int H = 1<<lg_H;  
  private static final int X = 1;  
  private static final int MAX_b = (1<<BitsPerByte)-1;  
  private static final int L = (MAX_b+1)-X-H;  
  public static final int MAX_LENGTH = 5;  


  public interface GetByte<ARR> {
      short getByte(ARR array, int position);
  }
  public interface SetPosition<ARR> {
      void setPosition(ARR array, int position);
  }

  public static
  <ARR> long readUint(ARR base, int position,
                      GetByte<ARR> getByte,
                      SetPosition<ARR> setPosition) {
    int pos = position;
    int b_0 = getByte.getByte(base, pos);
    int sum = b_0 - X;
    if (sum < L) {  
      setPosition.setPosition(base, pos+1);
      return Integer.toUnsignedLong(sum);
    }
    int lg_H_i = lg_H;  
    for (int i = 1; ; i++) {  
      int b_i = getByte.getByte(base, pos + i);
      if (b_i < X) {  
        setPosition.setPosition(base, pos+i);  
        return Integer.toUnsignedLong(sum);  
      }
      sum += (b_i - X) << lg_H_i;  
      if (b_i < X+L || i == MAX_LENGTH-1) {
        setPosition.setPosition(base, pos+i+1);
        return Integer.toUnsignedLong(sum);
      }
      lg_H_i += lg_H;
    }
  }

  public static int encodeSign(int value) {
    return (value << 1) ^ (value >> 31);
  }

  public static int decodeSign(int value) {
    return (value >>> 1) ^ -(value & 1);
  }


  private final Address base;
  private final int limit;

  public Unsigned5(Address base) {
    this(base, 0);  
  }
  public Unsigned5(Address base, int limit) {
    this.base = base;
    this.limit = limit;
  }

  public Address base() { return base; }
  public short getByte(int pos) {
    return (short) base.getCIntegerAt(pos, 1, true);
  }

  public class Reader {
    private int position = 0;  
    public int position() { return position; }
    public void setPosition(int pos) { position = pos; }
    public long nextUint() {
        if (!hasNext())  return -1;
        return readUint(this, position, Reader::getByte, Reader::setPosition);
    }
    public boolean hasNext() { return Unsigned5.this.hasNext(position); }
    private short getByte(int pos) { return Unsigned5.this.getByte(pos); }
  }

  public long readUint(int pos) {
    if (!hasNext(pos))  return -1;
    return readUint(this, pos, Unsigned5::getByte, (a,i)->{});
  }
  private boolean hasNext(int pos) {
    return ((X == 0 || getByte(pos) >= X) &&
            (limit == 0 || pos < limit));
  }

  public void print() {
    printOn(System.out);
  }
  public void printOn(PrintStream tty) {
    tty.print("U5 " + readUint(0) + ", ");
  }

  public void dumpOn(PrintStream tty, int count) {
    Reader r = new Reader();
    int printed = 0;
    tty.print("U5: [");
    for (;;) {
      if (count >= 0 && printed >= count)  break;
      if (!r.hasNext()) {
        if ((r.position < limit || limit == 0) && getByte(r.position) == 0) {
          tty.print(" null");
          ++r.position;  
          ++printed;
          if (limit != 0)  continue;  
        }
        break;
      }
      int value = (int) r.nextUint();
      tty.print(" ");
      tty.print(value);
      ++printed;
    }
    tty.println(" ] (values=" + printed + "/length=" + r.position + ")");
  }
  public void dump(int count) {
    dumpOn(System.out, count);
  }
  public void dump() {
    dumpOn(System.out, -1);
  }
}
