/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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


package vm.mlvm.indy.func.java.rawRetypes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

 public class INDIFY_Test6998541 {
     private static final int N = 100000;


     public static void main(String[] args) throws Throwable {
         doboolean ();
         dobyte ();
         dochar ();
         doshort ();
         doint ();
         dolong ();
         dofloat ();
         dodouble ();
     }

     private static void doboolean () throws Throwable {
         System.out.println("doboolean");

         for (int i = 0; i < N; i++ ) {
             boolean2prim(false);
             boolean2prim(true);

         }
     }
     private static void dobyte () throws Throwable {
         System.out.println("dobyte");

         byte x = Byte.MIN_VALUE;
         byte D = Byte.MAX_VALUE / (N / 2);
         for (int i = 0; i < N; i++, x += D) {
             byte2prim(x);


         }
     }
     private static void dochar () throws Throwable {
         System.out.println("dochar");

         char x = Character.MIN_VALUE;
         char D = Character.MAX_VALUE / (N / 2);
         for (int i = 0; i < N; i++, x += D) {
             char2prim(x);


         }
     }
     private static void doshort () throws Throwable {
         System.out.println("doshort");

         short x = Short.MIN_VALUE;
         short D = Short.MAX_VALUE / (N / 2);
         for (int i = 0; i < N; i++, x += D) {
             short2prim(x);


         }
     }
     private static void doint () throws Throwable {
         System.out.println("doint");

         int x = Integer.MIN_VALUE;
         int D = Integer.MAX_VALUE / (N / 2);
         for (int i = 0; i < N; i++, x += D) {
             int2prim(x);

             void2prim(x);
             prim2void(x);
             prim2prim(x);

         }
     }
     private static void dolong () throws Throwable {
         System.out.println("dolong");

         long x = Long.MIN_VALUE;
         long D = Long.MAX_VALUE / (N / 2);
         for (int i = 0; i < N; i++, x += D) {
             long2prim(x);


         }
     }
     private static void dofloat () throws Throwable {
         System.out.println("dofloat");

         float x = Float.MIN_VALUE;
         float D = Float.MAX_VALUE / (N / 2);
         for (int i = 0; i < N; i++, x += D) {
             float2prim(x);


         }
     }
     private static void dodouble () throws Throwable {
         System.out.println("dodouble");

         double x = Double.MIN_VALUE;
         double D = Double.MAX_VALUE / (N / 2);
         for (int i = 0; i < N; i++, x += D) {
             double2prim(x);


         }
     }

     private static void void2prim(int i) throws Throwable {
         assertEquals(        false, (boolean) INDY_boolean_foo_void().invokeExact());  
         assertEquals(        0, (byte) INDY_byte_foo_void().invokeExact());  
         assertEquals(        0, (char) INDY_char_foo_void().invokeExact());  
         assertEquals(        0, (short) INDY_short_foo_void().invokeExact());  
         assertEquals(        0, (int) INDY_int_foo_void().invokeExact());  
         assertEquals(        0L, (long) INDY_long_foo_void().invokeExact());  
         assertEquals(        0f, (float) INDY_float_foo_void().invokeExact());  
         assertEquals(        0d, (double) INDY_double_foo_void().invokeExact());  
     }

     private static void boolean2prim(boolean x) throws Throwable {
         int i = x ? 1 : 0;
             boolean z = x;
         assertEquals(z, (boolean) INDY_boolean_foo_boolean().invokeExact(x));  
         assertEquals((byte) i, (byte) INDY_byte_foo_boolean().invokeExact(x));  
         assertEquals((char) i, (char) INDY_char_foo_boolean().invokeExact(x));  
         assertEquals((short) i, (short) INDY_short_foo_boolean().invokeExact(x));  
         assertEquals((int) i, (int) INDY_int_foo_boolean().invokeExact(x));  
         assertEquals((long) i, (long) INDY_long_foo_boolean().invokeExact(x));  
         assertEquals((float) i, (float) INDY_float_foo_boolean().invokeExact(x));  
         assertEquals((double) i, (double) INDY_double_foo_boolean().invokeExact(x));  
     }
     private static void byte2prim(byte x) throws Throwable {
         byte i = x;
             boolean z = (x != 0);
         assertEquals(z, (boolean) INDY_boolean_foo_byte().invokeExact(x));  
         assertEquals((byte) i, (byte) INDY_byte_foo_byte().invokeExact(x));  
         assertEquals((char) i, (char) INDY_char_foo_byte().invokeExact(x));  
         assertEquals((short) i, (short) INDY_short_foo_byte().invokeExact(x));  
         assertEquals((int) i, (int) INDY_int_foo_byte().invokeExact(x));  
         assertEquals((long) i, (long) INDY_long_foo_byte().invokeExact(x));  
         assertEquals((float) i, (float) INDY_float_foo_byte().invokeExact(x));  
         assertEquals((double) i, (double) INDY_double_foo_byte().invokeExact(x));  
     }
     private static void char2prim(char x) throws Throwable {
         char i = x;
             boolean z = (x != 0);
         assertEquals(z, (boolean) INDY_boolean_foo_char().invokeExact(x));  
         assertEquals((byte) i, (byte) INDY_byte_foo_char().invokeExact(x));  
         assertEquals((char) i, (char) INDY_char_foo_char().invokeExact(x));  
         assertEquals((short) i, (short) INDY_short_foo_char().invokeExact(x));  
         assertEquals((int) i, (int) INDY_int_foo_char().invokeExact(x));  
         assertEquals((long) i, (long) INDY_long_foo_char().invokeExact(x));  
         assertEquals((float) i, (float) INDY_float_foo_char().invokeExact(x));  
         assertEquals((double) i, (double) INDY_double_foo_char().invokeExact(x));  
     }
     private static void short2prim(short x) throws Throwable {
         short i = x;
             boolean z = (x != 0);
         assertEquals(z, (boolean) INDY_boolean_foo_short().invokeExact(x));  
         assertEquals((byte) i, (byte) INDY_byte_foo_short().invokeExact(x));  
         assertEquals((char) i, (char) INDY_char_foo_short().invokeExact(x));  
         assertEquals((short) i, (short) INDY_short_foo_short().invokeExact(x));  
         assertEquals((int) i, (int) INDY_int_foo_short().invokeExact(x));  
         assertEquals((long) i, (long) INDY_long_foo_short().invokeExact(x));  
         assertEquals((float) i, (float) INDY_float_foo_short().invokeExact(x));  
         assertEquals((double) i, (double) INDY_double_foo_short().invokeExact(x));  
     }
     private static void int2prim(int x) throws Throwable {
         int i = x;
             boolean z = (x != 0);
         assertEquals(z, (boolean) INDY_boolean_foo_int().invokeExact(x));  
         assertEquals((byte) i, (byte) INDY_byte_foo_int().invokeExact(x));  
         assertEquals((char) i, (char) INDY_char_foo_int().invokeExact(x));  
         assertEquals((short) i, (short) INDY_short_foo_int().invokeExact(x));  
         assertEquals((int) i, (int) INDY_int_foo_int().invokeExact(x));  
         assertEquals((long) i, (long) INDY_long_foo_int().invokeExact(x));  
         assertEquals((float) i, (float) INDY_float_foo_int().invokeExact(x));  
         assertEquals((double) i, (double) INDY_double_foo_int().invokeExact(x));  
     }
     private static void long2prim(long x) throws Throwable {
         long i = x;
             boolean z = (x != 0);
         assertEquals(z, (boolean) INDY_boolean_foo_long().invokeExact(x));  
         assertEquals((byte) i, (byte) INDY_byte_foo_long().invokeExact(x));  
         assertEquals((char) i, (char) INDY_char_foo_long().invokeExact(x));  
         assertEquals((short) i, (short) INDY_short_foo_long().invokeExact(x));  
         assertEquals((int) i, (int) INDY_int_foo_long().invokeExact(x));  
         assertEquals((long) i, (long) INDY_long_foo_long().invokeExact(x));  
         assertEquals((float) i, (float) INDY_float_foo_long().invokeExact(x));  
         assertEquals((double) i, (double) INDY_double_foo_long().invokeExact(x));  
     }
     private static void float2prim(float x) throws Throwable {
         float i = x;
             boolean z = (x != 0);
         assertEquals(z, (boolean) INDY_boolean_foo_float().invokeExact(x));  
         assertEquals((byte) i, (byte) INDY_byte_foo_float().invokeExact(x));  
         assertEquals((char) i, (char) INDY_char_foo_float().invokeExact(x));  
         assertEquals((short) i, (short) INDY_short_foo_float().invokeExact(x));  
         assertEquals((int) i, (int) INDY_int_foo_float().invokeExact(x));  
         assertEquals((long) i, (long) INDY_long_foo_float().invokeExact(x));  
         assertEquals((float) i, (float) INDY_float_foo_float().invokeExact(x));  
         assertEquals((double) i, (double) INDY_double_foo_float().invokeExact(x));  
     }
     private static void double2prim(double x) throws Throwable {
         double i = x;
             boolean z = (x != 0);
         assertEquals(z, (boolean) INDY_boolean_foo_double().invokeExact(x));  
         assertEquals((byte) i, (byte) INDY_byte_foo_double().invokeExact(x));  
         assertEquals((char) i, (char) INDY_char_foo_double().invokeExact(x));  
         assertEquals((short) i, (short) INDY_short_foo_double().invokeExact(x));  
         assertEquals((int) i, (int) INDY_int_foo_double().invokeExact(x));  
         assertEquals((long) i, (long) INDY_long_foo_double().invokeExact(x));  
         assertEquals((float) i, (float) INDY_float_foo_double().invokeExact(x));  
         assertEquals((double) i, (double) INDY_double_foo_double().invokeExact(x));  
     }

     private static void prim2void(int x) throws Throwable {
             boolean z = (x != 0);
              INDY_void_foo_boolean().invokeExact(z);  
              INDY_void_foo_byte().invokeExact((byte) x);  
              INDY_void_foo_char().invokeExact((char) x);  
              INDY_void_foo_short().invokeExact((short) x);  
              INDY_void_foo_int().invokeExact((int) x);  
              INDY_void_foo_long().invokeExact((long) x);  
              INDY_void_foo_float().invokeExact((float) x);  
              INDY_void_foo_double().invokeExact((double) x);  
     }

     private static void void2void() throws Throwable {
         INDY_void_foo_void().invokeExact();  
     }

     private static void prim2prim(int x) throws Throwable {
         boolean z = (x != 0);
         assertEquals(         z, (boolean) INDY_boolean_spread_boolean().invokeExact(z));  
         assertEquals((byte) x, (byte) INDY_byte_spread_byte().invokeExact((byte) x));  
         assertEquals((char) x, (char) INDY_char_spread_char().invokeExact((char) x));  
         assertEquals((short) x, (short) INDY_short_spread_short().invokeExact((short) x));  
         assertEquals((int) x, (int) INDY_int_spread_int().invokeExact((int) x));  
         assertEquals((long) x, (long) INDY_long_spread_long().invokeExact((long) x));  
         assertEquals((float) x, (float) INDY_float_spread_float().invokeExact((float) x));  
         assertEquals((double) x, (double) INDY_double_spread_double().invokeExact((double) x));  
     }

     private static void assertEquals(Object o, Object o2) {
         if (!o.equals(o2))
             throw new AssertionError("expected: " + o + ", found: " + o2);
     }


    private static MethodHandle INDY_void_foo_void;
    private static MethodHandle INDY_void_foo_void () throws Throwable {
        if ( INDY_void_foo_void != null ) return INDY_void_foo_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(void .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_spread_void;
    private static MethodHandle INDY_void_spread_void () throws Throwable {
        if ( INDY_void_spread_void != null ) return INDY_void_spread_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(void .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_foo_boolean;
    private static MethodHandle INDY_void_foo_boolean () throws Throwable {
        if ( INDY_void_foo_boolean != null ) return INDY_void_foo_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(void .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_spread_boolean;
    private static MethodHandle INDY_void_spread_boolean () throws Throwable {
        if ( INDY_void_spread_boolean != null ) return INDY_void_spread_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(void .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_foo_byte;
    private static MethodHandle INDY_void_foo_byte () throws Throwable {
        if ( INDY_void_foo_byte != null ) return INDY_void_foo_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(void .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_spread_byte;
    private static MethodHandle INDY_void_spread_byte () throws Throwable {
        if ( INDY_void_spread_byte != null ) return INDY_void_spread_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(void .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_foo_char;
    private static MethodHandle INDY_void_foo_char () throws Throwable {
        if ( INDY_void_foo_char != null ) return INDY_void_foo_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(void .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_spread_char;
    private static MethodHandle INDY_void_spread_char () throws Throwable {
        if ( INDY_void_spread_char != null ) return INDY_void_spread_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(void .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_foo_short;
    private static MethodHandle INDY_void_foo_short () throws Throwable {
        if ( INDY_void_foo_short != null ) return INDY_void_foo_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(void .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_spread_short;
    private static MethodHandle INDY_void_spread_short () throws Throwable {
        if ( INDY_void_spread_short != null ) return INDY_void_spread_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(void .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_foo_int;
    private static MethodHandle INDY_void_foo_int () throws Throwable {
        if ( INDY_void_foo_int != null ) return INDY_void_foo_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(void .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_spread_int;
    private static MethodHandle INDY_void_spread_int () throws Throwable {
        if ( INDY_void_spread_int != null ) return INDY_void_spread_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(void .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_foo_long;
    private static MethodHandle INDY_void_foo_long () throws Throwable {
        if ( INDY_void_foo_long != null ) return INDY_void_foo_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(void .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_spread_long;
    private static MethodHandle INDY_void_spread_long () throws Throwable {
        if ( INDY_void_spread_long != null ) return INDY_void_spread_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(void .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_foo_float;
    private static MethodHandle INDY_void_foo_float () throws Throwable {
        if ( INDY_void_foo_float != null ) return INDY_void_foo_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(void .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_spread_float;
    private static MethodHandle INDY_void_spread_float () throws Throwable {
        if ( INDY_void_spread_float != null ) return INDY_void_spread_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(void .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_foo_double;
    private static MethodHandle INDY_void_foo_double () throws Throwable {
        if ( INDY_void_foo_double != null ) return INDY_void_foo_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(void .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_void_spread_double;
    private static MethodHandle INDY_void_spread_double () throws Throwable {
        if ( INDY_void_spread_double != null ) return INDY_void_spread_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(void .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_foo_void;
    private static MethodHandle INDY_boolean_foo_void () throws Throwable {
        if ( INDY_boolean_foo_void != null ) return INDY_boolean_foo_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(boolean .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_spread_void;
    private static MethodHandle INDY_boolean_spread_void () throws Throwable {
        if ( INDY_boolean_spread_void != null ) return INDY_boolean_spread_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(boolean .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_foo_boolean;
    private static MethodHandle INDY_boolean_foo_boolean () throws Throwable {
        if ( INDY_boolean_foo_boolean != null ) return INDY_boolean_foo_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(boolean .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_spread_boolean;
    private static MethodHandle INDY_boolean_spread_boolean () throws Throwable {
        if ( INDY_boolean_spread_boolean != null ) return INDY_boolean_spread_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(boolean .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_foo_byte;
    private static MethodHandle INDY_boolean_foo_byte () throws Throwable {
        if ( INDY_boolean_foo_byte != null ) return INDY_boolean_foo_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(boolean .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_spread_byte;
    private static MethodHandle INDY_boolean_spread_byte () throws Throwable {
        if ( INDY_boolean_spread_byte != null ) return INDY_boolean_spread_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(boolean .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_foo_char;
    private static MethodHandle INDY_boolean_foo_char () throws Throwable {
        if ( INDY_boolean_foo_char != null ) return INDY_boolean_foo_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(boolean .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_spread_char;
    private static MethodHandle INDY_boolean_spread_char () throws Throwable {
        if ( INDY_boolean_spread_char != null ) return INDY_boolean_spread_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(boolean .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_foo_short;
    private static MethodHandle INDY_boolean_foo_short () throws Throwable {
        if ( INDY_boolean_foo_short != null ) return INDY_boolean_foo_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(boolean .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_spread_short;
    private static MethodHandle INDY_boolean_spread_short () throws Throwable {
        if ( INDY_boolean_spread_short != null ) return INDY_boolean_spread_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(boolean .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_foo_int;
    private static MethodHandle INDY_boolean_foo_int () throws Throwable {
        if ( INDY_boolean_foo_int != null ) return INDY_boolean_foo_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(boolean .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_spread_int;
    private static MethodHandle INDY_boolean_spread_int () throws Throwable {
        if ( INDY_boolean_spread_int != null ) return INDY_boolean_spread_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(boolean .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_foo_long;
    private static MethodHandle INDY_boolean_foo_long () throws Throwable {
        if ( INDY_boolean_foo_long != null ) return INDY_boolean_foo_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(boolean .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_spread_long;
    private static MethodHandle INDY_boolean_spread_long () throws Throwable {
        if ( INDY_boolean_spread_long != null ) return INDY_boolean_spread_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(boolean .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_foo_float;
    private static MethodHandle INDY_boolean_foo_float () throws Throwable {
        if ( INDY_boolean_foo_float != null ) return INDY_boolean_foo_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(boolean .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_spread_float;
    private static MethodHandle INDY_boolean_spread_float () throws Throwable {
        if ( INDY_boolean_spread_float != null ) return INDY_boolean_spread_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(boolean .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_foo_double;
    private static MethodHandle INDY_boolean_foo_double () throws Throwable {
        if ( INDY_boolean_foo_double != null ) return INDY_boolean_foo_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(boolean .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_boolean_spread_double;
    private static MethodHandle INDY_boolean_spread_double () throws Throwable {
        if ( INDY_boolean_spread_double != null ) return INDY_boolean_spread_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(boolean .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_foo_void;
    private static MethodHandle INDY_byte_foo_void () throws Throwable {
        if ( INDY_byte_foo_void != null ) return INDY_byte_foo_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(byte .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_spread_void;
    private static MethodHandle INDY_byte_spread_void () throws Throwable {
        if ( INDY_byte_spread_void != null ) return INDY_byte_spread_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(byte .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_foo_boolean;
    private static MethodHandle INDY_byte_foo_boolean () throws Throwable {
        if ( INDY_byte_foo_boolean != null ) return INDY_byte_foo_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(byte .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_spread_boolean;
    private static MethodHandle INDY_byte_spread_boolean () throws Throwable {
        if ( INDY_byte_spread_boolean != null ) return INDY_byte_spread_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(byte .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_foo_byte;
    private static MethodHandle INDY_byte_foo_byte () throws Throwable {
        if ( INDY_byte_foo_byte != null ) return INDY_byte_foo_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(byte .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_spread_byte;
    private static MethodHandle INDY_byte_spread_byte () throws Throwable {
        if ( INDY_byte_spread_byte != null ) return INDY_byte_spread_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(byte .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_foo_char;
    private static MethodHandle INDY_byte_foo_char () throws Throwable {
        if ( INDY_byte_foo_char != null ) return INDY_byte_foo_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(byte .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_spread_char;
    private static MethodHandle INDY_byte_spread_char () throws Throwable {
        if ( INDY_byte_spread_char != null ) return INDY_byte_spread_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(byte .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_foo_short;
    private static MethodHandle INDY_byte_foo_short () throws Throwable {
        if ( INDY_byte_foo_short != null ) return INDY_byte_foo_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(byte .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_spread_short;
    private static MethodHandle INDY_byte_spread_short () throws Throwable {
        if ( INDY_byte_spread_short != null ) return INDY_byte_spread_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(byte .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_foo_int;
    private static MethodHandle INDY_byte_foo_int () throws Throwable {
        if ( INDY_byte_foo_int != null ) return INDY_byte_foo_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(byte .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_spread_int;
    private static MethodHandle INDY_byte_spread_int () throws Throwable {
        if ( INDY_byte_spread_int != null ) return INDY_byte_spread_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(byte .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_foo_long;
    private static MethodHandle INDY_byte_foo_long () throws Throwable {
        if ( INDY_byte_foo_long != null ) return INDY_byte_foo_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(byte .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_spread_long;
    private static MethodHandle INDY_byte_spread_long () throws Throwable {
        if ( INDY_byte_spread_long != null ) return INDY_byte_spread_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(byte .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_foo_float;
    private static MethodHandle INDY_byte_foo_float () throws Throwable {
        if ( INDY_byte_foo_float != null ) return INDY_byte_foo_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(byte .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_spread_float;
    private static MethodHandle INDY_byte_spread_float () throws Throwable {
        if ( INDY_byte_spread_float != null ) return INDY_byte_spread_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(byte .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_foo_double;
    private static MethodHandle INDY_byte_foo_double () throws Throwable {
        if ( INDY_byte_foo_double != null ) return INDY_byte_foo_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(byte .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_byte_spread_double;
    private static MethodHandle INDY_byte_spread_double () throws Throwable {
        if ( INDY_byte_spread_double != null ) return INDY_byte_spread_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(byte .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_foo_void;
    private static MethodHandle INDY_char_foo_void () throws Throwable {
        if ( INDY_char_foo_void != null ) return INDY_char_foo_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(char .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_spread_void;
    private static MethodHandle INDY_char_spread_void () throws Throwable {
        if ( INDY_char_spread_void != null ) return INDY_char_spread_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(char .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_foo_boolean;
    private static MethodHandle INDY_char_foo_boolean () throws Throwable {
        if ( INDY_char_foo_boolean != null ) return INDY_char_foo_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(char .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_spread_boolean;
    private static MethodHandle INDY_char_spread_boolean () throws Throwable {
        if ( INDY_char_spread_boolean != null ) return INDY_char_spread_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(char .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_foo_byte;
    private static MethodHandle INDY_char_foo_byte () throws Throwable {
        if ( INDY_char_foo_byte != null ) return INDY_char_foo_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(char .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_spread_byte;
    private static MethodHandle INDY_char_spread_byte () throws Throwable {
        if ( INDY_char_spread_byte != null ) return INDY_char_spread_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(char .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_foo_char;
    private static MethodHandle INDY_char_foo_char () throws Throwable {
        if ( INDY_char_foo_char != null ) return INDY_char_foo_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(char .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_spread_char;
    private static MethodHandle INDY_char_spread_char () throws Throwable {
        if ( INDY_char_spread_char != null ) return INDY_char_spread_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(char .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_foo_short;
    private static MethodHandle INDY_char_foo_short () throws Throwable {
        if ( INDY_char_foo_short != null ) return INDY_char_foo_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(char .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_spread_short;
    private static MethodHandle INDY_char_spread_short () throws Throwable {
        if ( INDY_char_spread_short != null ) return INDY_char_spread_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(char .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_foo_int;
    private static MethodHandle INDY_char_foo_int () throws Throwable {
        if ( INDY_char_foo_int != null ) return INDY_char_foo_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(char .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_spread_int;
    private static MethodHandle INDY_char_spread_int () throws Throwable {
        if ( INDY_char_spread_int != null ) return INDY_char_spread_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(char .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_foo_long;
    private static MethodHandle INDY_char_foo_long () throws Throwable {
        if ( INDY_char_foo_long != null ) return INDY_char_foo_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(char .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_spread_long;
    private static MethodHandle INDY_char_spread_long () throws Throwable {
        if ( INDY_char_spread_long != null ) return INDY_char_spread_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(char .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_foo_float;
    private static MethodHandle INDY_char_foo_float () throws Throwable {
        if ( INDY_char_foo_float != null ) return INDY_char_foo_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(char .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_spread_float;
    private static MethodHandle INDY_char_spread_float () throws Throwable {
        if ( INDY_char_spread_float != null ) return INDY_char_spread_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(char .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_foo_double;
    private static MethodHandle INDY_char_foo_double () throws Throwable {
        if ( INDY_char_foo_double != null ) return INDY_char_foo_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(char .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_char_spread_double;
    private static MethodHandle INDY_char_spread_double () throws Throwable {
        if ( INDY_char_spread_double != null ) return INDY_char_spread_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(char .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_foo_void;
    private static MethodHandle INDY_short_foo_void () throws Throwable {
        if ( INDY_short_foo_void != null ) return INDY_short_foo_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(short .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_spread_void;
    private static MethodHandle INDY_short_spread_void () throws Throwable {
        if ( INDY_short_spread_void != null ) return INDY_short_spread_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(short .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_foo_boolean;
    private static MethodHandle INDY_short_foo_boolean () throws Throwable {
        if ( INDY_short_foo_boolean != null ) return INDY_short_foo_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(short .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_spread_boolean;
    private static MethodHandle INDY_short_spread_boolean () throws Throwable {
        if ( INDY_short_spread_boolean != null ) return INDY_short_spread_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(short .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_foo_byte;
    private static MethodHandle INDY_short_foo_byte () throws Throwable {
        if ( INDY_short_foo_byte != null ) return INDY_short_foo_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(short .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_spread_byte;
    private static MethodHandle INDY_short_spread_byte () throws Throwable {
        if ( INDY_short_spread_byte != null ) return INDY_short_spread_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(short .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_foo_char;
    private static MethodHandle INDY_short_foo_char () throws Throwable {
        if ( INDY_short_foo_char != null ) return INDY_short_foo_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(short .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_spread_char;
    private static MethodHandle INDY_short_spread_char () throws Throwable {
        if ( INDY_short_spread_char != null ) return INDY_short_spread_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(short .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_foo_short;
    private static MethodHandle INDY_short_foo_short () throws Throwable {
        if ( INDY_short_foo_short != null ) return INDY_short_foo_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(short .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_spread_short;
    private static MethodHandle INDY_short_spread_short () throws Throwable {
        if ( INDY_short_spread_short != null ) return INDY_short_spread_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(short .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_foo_int;
    private static MethodHandle INDY_short_foo_int () throws Throwable {
        if ( INDY_short_foo_int != null ) return INDY_short_foo_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(short .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_spread_int;
    private static MethodHandle INDY_short_spread_int () throws Throwable {
        if ( INDY_short_spread_int != null ) return INDY_short_spread_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(short .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_foo_long;
    private static MethodHandle INDY_short_foo_long () throws Throwable {
        if ( INDY_short_foo_long != null ) return INDY_short_foo_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(short .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_spread_long;
    private static MethodHandle INDY_short_spread_long () throws Throwable {
        if ( INDY_short_spread_long != null ) return INDY_short_spread_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(short .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_foo_float;
    private static MethodHandle INDY_short_foo_float () throws Throwable {
        if ( INDY_short_foo_float != null ) return INDY_short_foo_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(short .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_spread_float;
    private static MethodHandle INDY_short_spread_float () throws Throwable {
        if ( INDY_short_spread_float != null ) return INDY_short_spread_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(short .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_foo_double;
    private static MethodHandle INDY_short_foo_double () throws Throwable {
        if ( INDY_short_foo_double != null ) return INDY_short_foo_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(short .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_short_spread_double;
    private static MethodHandle INDY_short_spread_double () throws Throwable {
        if ( INDY_short_spread_double != null ) return INDY_short_spread_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(short .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_foo_void;
    private static MethodHandle INDY_int_foo_void () throws Throwable {
        if ( INDY_int_foo_void != null ) return INDY_int_foo_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(int .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_spread_void;
    private static MethodHandle INDY_int_spread_void () throws Throwable {
        if ( INDY_int_spread_void != null ) return INDY_int_spread_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(int .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_foo_boolean;
    private static MethodHandle INDY_int_foo_boolean () throws Throwable {
        if ( INDY_int_foo_boolean != null ) return INDY_int_foo_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(int .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_spread_boolean;
    private static MethodHandle INDY_int_spread_boolean () throws Throwable {
        if ( INDY_int_spread_boolean != null ) return INDY_int_spread_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(int .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_foo_byte;
    private static MethodHandle INDY_int_foo_byte () throws Throwable {
        if ( INDY_int_foo_byte != null ) return INDY_int_foo_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(int .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_spread_byte;
    private static MethodHandle INDY_int_spread_byte () throws Throwable {
        if ( INDY_int_spread_byte != null ) return INDY_int_spread_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(int .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_foo_char;
    private static MethodHandle INDY_int_foo_char () throws Throwable {
        if ( INDY_int_foo_char != null ) return INDY_int_foo_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(int .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_spread_char;
    private static MethodHandle INDY_int_spread_char () throws Throwable {
        if ( INDY_int_spread_char != null ) return INDY_int_spread_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(int .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_foo_short;
    private static MethodHandle INDY_int_foo_short () throws Throwable {
        if ( INDY_int_foo_short != null ) return INDY_int_foo_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(int .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_spread_short;
    private static MethodHandle INDY_int_spread_short () throws Throwable {
        if ( INDY_int_spread_short != null ) return INDY_int_spread_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(int .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_foo_int;
    private static MethodHandle INDY_int_foo_int () throws Throwable {
        if ( INDY_int_foo_int != null ) return INDY_int_foo_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(int .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_spread_int;
    private static MethodHandle INDY_int_spread_int () throws Throwable {
        if ( INDY_int_spread_int != null ) return INDY_int_spread_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(int .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_foo_long;
    private static MethodHandle INDY_int_foo_long () throws Throwable {
        if ( INDY_int_foo_long != null ) return INDY_int_foo_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(int .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_spread_long;
    private static MethodHandle INDY_int_spread_long () throws Throwable {
        if ( INDY_int_spread_long != null ) return INDY_int_spread_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(int .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_foo_float;
    private static MethodHandle INDY_int_foo_float () throws Throwable {
        if ( INDY_int_foo_float != null ) return INDY_int_foo_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(int .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_spread_float;
    private static MethodHandle INDY_int_spread_float () throws Throwable {
        if ( INDY_int_spread_float != null ) return INDY_int_spread_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(int .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_foo_double;
    private static MethodHandle INDY_int_foo_double () throws Throwable {
        if ( INDY_int_foo_double != null ) return INDY_int_foo_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(int .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_int_spread_double;
    private static MethodHandle INDY_int_spread_double () throws Throwable {
        if ( INDY_int_spread_double != null ) return INDY_int_spread_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(int .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_foo_void;
    private static MethodHandle INDY_long_foo_void () throws Throwable {
        if ( INDY_long_foo_void != null ) return INDY_long_foo_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(long .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_spread_void;
    private static MethodHandle INDY_long_spread_void () throws Throwable {
        if ( INDY_long_spread_void != null ) return INDY_long_spread_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(long .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_foo_boolean;
    private static MethodHandle INDY_long_foo_boolean () throws Throwable {
        if ( INDY_long_foo_boolean != null ) return INDY_long_foo_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(long .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_spread_boolean;
    private static MethodHandle INDY_long_spread_boolean () throws Throwable {
        if ( INDY_long_spread_boolean != null ) return INDY_long_spread_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(long .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_foo_byte;
    private static MethodHandle INDY_long_foo_byte () throws Throwable {
        if ( INDY_long_foo_byte != null ) return INDY_long_foo_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(long .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_spread_byte;
    private static MethodHandle INDY_long_spread_byte () throws Throwable {
        if ( INDY_long_spread_byte != null ) return INDY_long_spread_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(long .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_foo_char;
    private static MethodHandle INDY_long_foo_char () throws Throwable {
        if ( INDY_long_foo_char != null ) return INDY_long_foo_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(long .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_spread_char;
    private static MethodHandle INDY_long_spread_char () throws Throwable {
        if ( INDY_long_spread_char != null ) return INDY_long_spread_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(long .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_foo_short;
    private static MethodHandle INDY_long_foo_short () throws Throwable {
        if ( INDY_long_foo_short != null ) return INDY_long_foo_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(long .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_spread_short;
    private static MethodHandle INDY_long_spread_short () throws Throwable {
        if ( INDY_long_spread_short != null ) return INDY_long_spread_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(long .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_foo_int;
    private static MethodHandle INDY_long_foo_int () throws Throwable {
        if ( INDY_long_foo_int != null ) return INDY_long_foo_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(long .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_spread_int;
    private static MethodHandle INDY_long_spread_int () throws Throwable {
        if ( INDY_long_spread_int != null ) return INDY_long_spread_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(long .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_foo_long;
    private static MethodHandle INDY_long_foo_long () throws Throwable {
        if ( INDY_long_foo_long != null ) return INDY_long_foo_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(long .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_spread_long;
    private static MethodHandle INDY_long_spread_long () throws Throwable {
        if ( INDY_long_spread_long != null ) return INDY_long_spread_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(long .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_foo_float;
    private static MethodHandle INDY_long_foo_float () throws Throwable {
        if ( INDY_long_foo_float != null ) return INDY_long_foo_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(long .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_spread_float;
    private static MethodHandle INDY_long_spread_float () throws Throwable {
        if ( INDY_long_spread_float != null ) return INDY_long_spread_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(long .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_foo_double;
    private static MethodHandle INDY_long_foo_double () throws Throwable {
        if ( INDY_long_foo_double != null ) return INDY_long_foo_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(long .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_long_spread_double;
    private static MethodHandle INDY_long_spread_double () throws Throwable {
        if ( INDY_long_spread_double != null ) return INDY_long_spread_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(long .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_foo_void;
    private static MethodHandle INDY_float_foo_void () throws Throwable {
        if ( INDY_float_foo_void != null ) return INDY_float_foo_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(float .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_spread_void;
    private static MethodHandle INDY_float_spread_void () throws Throwable {
        if ( INDY_float_spread_void != null ) return INDY_float_spread_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(float .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_foo_boolean;
    private static MethodHandle INDY_float_foo_boolean () throws Throwable {
        if ( INDY_float_foo_boolean != null ) return INDY_float_foo_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(float .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_spread_boolean;
    private static MethodHandle INDY_float_spread_boolean () throws Throwable {
        if ( INDY_float_spread_boolean != null ) return INDY_float_spread_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(float .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_foo_byte;
    private static MethodHandle INDY_float_foo_byte () throws Throwable {
        if ( INDY_float_foo_byte != null ) return INDY_float_foo_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(float .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_spread_byte;
    private static MethodHandle INDY_float_spread_byte () throws Throwable {
        if ( INDY_float_spread_byte != null ) return INDY_float_spread_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(float .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_foo_char;
    private static MethodHandle INDY_float_foo_char () throws Throwable {
        if ( INDY_float_foo_char != null ) return INDY_float_foo_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(float .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_spread_char;
    private static MethodHandle INDY_float_spread_char () throws Throwable {
        if ( INDY_float_spread_char != null ) return INDY_float_spread_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(float .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_foo_short;
    private static MethodHandle INDY_float_foo_short () throws Throwable {
        if ( INDY_float_foo_short != null ) return INDY_float_foo_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(float .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_spread_short;
    private static MethodHandle INDY_float_spread_short () throws Throwable {
        if ( INDY_float_spread_short != null ) return INDY_float_spread_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(float .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_foo_int;
    private static MethodHandle INDY_float_foo_int () throws Throwable {
        if ( INDY_float_foo_int != null ) return INDY_float_foo_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(float .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_spread_int;
    private static MethodHandle INDY_float_spread_int () throws Throwable {
        if ( INDY_float_spread_int != null ) return INDY_float_spread_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(float .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_foo_long;
    private static MethodHandle INDY_float_foo_long () throws Throwable {
        if ( INDY_float_foo_long != null ) return INDY_float_foo_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(float .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_spread_long;
    private static MethodHandle INDY_float_spread_long () throws Throwable {
        if ( INDY_float_spread_long != null ) return INDY_float_spread_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(float .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_foo_float;
    private static MethodHandle INDY_float_foo_float () throws Throwable {
        if ( INDY_float_foo_float != null ) return INDY_float_foo_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(float .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_spread_float;
    private static MethodHandle INDY_float_spread_float () throws Throwable {
        if ( INDY_float_spread_float != null ) return INDY_float_spread_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(float .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_foo_double;
    private static MethodHandle INDY_float_foo_double () throws Throwable {
        if ( INDY_float_foo_double != null ) return INDY_float_foo_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(float .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_float_spread_double;
    private static MethodHandle INDY_float_spread_double () throws Throwable {
        if ( INDY_float_spread_double != null ) return INDY_float_spread_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(float .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_foo_void;
    private static MethodHandle INDY_double_foo_void () throws Throwable {
        if ( INDY_double_foo_void != null ) return INDY_double_foo_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(double .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_spread_void;
    private static MethodHandle INDY_double_spread_void () throws Throwable {
        if ( INDY_double_spread_void != null ) return INDY_double_spread_void;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(double .class ))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_foo_boolean;
    private static MethodHandle INDY_double_foo_boolean () throws Throwable {
        if ( INDY_double_foo_boolean != null ) return INDY_double_foo_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(double .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_spread_boolean;
    private static MethodHandle INDY_double_spread_boolean () throws Throwable {
        if ( INDY_double_spread_boolean != null ) return INDY_double_spread_boolean;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(double .class , boolean.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_foo_byte;
    private static MethodHandle INDY_double_foo_byte () throws Throwable {
        if ( INDY_double_foo_byte != null ) return INDY_double_foo_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(double .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_spread_byte;
    private static MethodHandle INDY_double_spread_byte () throws Throwable {
        if ( INDY_double_spread_byte != null ) return INDY_double_spread_byte;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(double .class , byte.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_foo_char;
    private static MethodHandle INDY_double_foo_char () throws Throwable {
        if ( INDY_double_foo_char != null ) return INDY_double_foo_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(double .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_spread_char;
    private static MethodHandle INDY_double_spread_char () throws Throwable {
        if ( INDY_double_spread_char != null ) return INDY_double_spread_char;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(double .class , char.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_foo_short;
    private static MethodHandle INDY_double_foo_short () throws Throwable {
        if ( INDY_double_foo_short != null ) return INDY_double_foo_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(double .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_spread_short;
    private static MethodHandle INDY_double_spread_short () throws Throwable {
        if ( INDY_double_spread_short != null ) return INDY_double_spread_short;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(double .class , short.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_foo_int;
    private static MethodHandle INDY_double_foo_int () throws Throwable {
        if ( INDY_double_foo_int != null ) return INDY_double_foo_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(double .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_spread_int;
    private static MethodHandle INDY_double_spread_int () throws Throwable {
        if ( INDY_double_spread_int != null ) return INDY_double_spread_int;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(double .class , int.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_foo_long;
    private static MethodHandle INDY_double_foo_long () throws Throwable {
        if ( INDY_double_foo_long != null ) return INDY_double_foo_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(double .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_spread_long;
    private static MethodHandle INDY_double_spread_long () throws Throwable {
        if ( INDY_double_spread_long != null ) return INDY_double_spread_long;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(double .class , long.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_foo_float;
    private static MethodHandle INDY_double_foo_float () throws Throwable {
        if ( INDY_double_foo_float != null ) return INDY_double_foo_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(double .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_spread_float;
    private static MethodHandle INDY_double_spread_float () throws Throwable {
        if ( INDY_double_spread_float != null ) return INDY_double_spread_float;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(double .class , float.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_foo_double;
    private static MethodHandle INDY_double_foo_double () throws Throwable {
        if ( INDY_double_foo_double != null ) return INDY_double_foo_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "foo",
                    MethodType.methodType(double .class , double.class))).dynamicInvoker();
    }


    private static MethodHandle INDY_double_spread_double;
    private static MethodHandle INDY_double_spread_double () throws Throwable {
        if ( INDY_double_spread_double != null ) return INDY_double_spread_double;
        return ((CallSite) MH_bootstrap().invokeWithArguments(
                    MethodHandles.lookup(),
                    "spread",
                    MethodType.methodType(double .class , double.class))).dynamicInvoker();
    }



    private static MethodType MT_bootstrap () { return MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class); }

    private static MethodHandle MH_bootstrap () throws Throwable {
        return MethodHandles.lookup().findStatic(INDIFY_Test6998541.class, "bootstrap", MT_bootstrap());
    }

     private static CallSite bootstrap(MethodHandles.Lookup declaring, String name, MethodType methodType) throws Throwable {
         MethodHandles.Lookup lookup = MethodHandles.lookup();
         MethodHandle mh;
         if (methodType.parameterCount() == 0) {
             mh = lookup.findStatic(INDIFY_Test6998541.class, "identity", MethodType.methodType(void.class));
         } else {
             Class<?> type = methodType.parameterType(0);
             mh = lookup.findStatic(INDIFY_Test6998541.class, "identity", MethodType.methodType(type, type));

             if ("spread".equals(name)) {
                 int paramCount = mh.type().parameterCount();
                 mh = mh.asSpreader(Object[].class, paramCount).asCollector(Object[].class, paramCount);
             }
         }
         mh = mh.asType(methodType);
         return new ConstantCallSite(mh);
     }

     private static boolean identity(boolean v) { return v; }
     private static byte identity(byte v) { return v; }
     private static char identity(char v) { return v; }
     private static short identity(short v) { return v; }
     private static int identity(int v) { return v; }
     private static long identity(long v) { return v; }
     private static float identity(float v) { return v; }
     private static double identity(double v) { return v; }
     private static void identity() {}
}
