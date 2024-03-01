/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless;

public class PromotionTests extends ScriptTestCase {
    public void testBinaryPromotion() throws Exception {
        assertEquals((byte) 1 + (byte) 1, exec("byte x = 1; byte y = 1; return x+y;"));
        assertEquals((byte) 1 + (char) 1, exec("byte x = 1; char y = 1; return x+y;"));
        assertEquals((byte) 1 + (short) 1, exec("byte x = 1; short y = 1; return x+y;"));
        assertEquals((byte) 1 + 1, exec("byte x = 1; int y = 1; return x+y;"));
        assertEquals((byte) 1 + 1L, exec("byte x = 1; long y = 1; return x+y;"));
        assertEquals((byte) 1 + 1F, exec("byte x = 1; float y = 1; return x+y;"));
        assertEquals((byte) 1 + 1.0, exec("byte x = 1; double y = 1; return x+y;"));

        assertEquals((char) 1 + (byte) 1, exec("char x = 1; byte y = 1; return x+y;"));
        assertEquals((char) 1 + (char) 1, exec("char x = 1; char y = 1; return x+y;"));
        assertEquals((char) 1 + (short) 1, exec("char x = 1; short y = 1; return x+y;"));
        assertEquals((char) 1 + 1, exec("char x = 1; int y = 1; return x+y;"));
        assertEquals((char) 1 + 1L, exec("char x = 1; long y = 1; return x+y;"));
        assertEquals((char) 1 + 1F, exec("char x = 1; float y = 1; return x+y;"));
        assertEquals((char) 1 + 1.0, exec("char x = 1; double y = 1; return x+y;"));

        assertEquals((short) 1 + (byte) 1, exec("short x = 1; byte y = 1; return x+y;"));
        assertEquals((short) 1 + (char) 1, exec("short x = 1; char y = 1; return x+y;"));
        assertEquals((short) 1 + (short) 1, exec("short x = 1; short y = 1; return x+y;"));
        assertEquals((short) 1 + 1, exec("short x = 1; int y = 1; return x+y;"));
        assertEquals((short) 1 + 1L, exec("short x = 1; long y = 1; return x+y;"));
        assertEquals((short) 1 + 1F, exec("short x = 1; float y = 1; return x+y;"));
        assertEquals((short) 1 + 1.0, exec("short x = 1; double y = 1; return x+y;"));

        assertEquals(1 + (byte) 1, exec("int x = 1; byte y = 1; return x+y;"));
        assertEquals(1 + (char) 1, exec("int x = 1; char y = 1; return x+y;"));
        assertEquals(1 + (short) 1, exec("int x = 1; short y = 1; return x+y;"));
        assertEquals(1 + 1, exec("int x = 1; int y = 1; return x+y;"));
        assertEquals(1 + 1L, exec("int x = 1; long y = 1; return x+y;"));
        assertEquals(1 + 1F, exec("int x = 1; float y = 1; return x+y;"));
        assertEquals(1 + 1.0, exec("int x = 1; double y = 1; return x+y;"));

        assertEquals(1L + (byte) 1, exec("long x = 1; byte y = 1; return x+y;"));
        assertEquals(1L + (char) 1, exec("long x = 1; char y = 1; return x+y;"));
        assertEquals(1L + (short) 1, exec("long x = 1; short y = 1; return x+y;"));
        assertEquals(1L + 1, exec("long x = 1; int y = 1; return x+y;"));
        assertEquals(1L + 1L, exec("long x = 1; long y = 1; return x+y;"));
        assertEquals(1L + 1F, exec("long x = 1; float y = 1; return x+y;"));
        assertEquals(1L + 1.0, exec("long x = 1; double y = 1; return x+y;"));

        assertEquals(1F + (byte) 1, exec("float x = 1; byte y = 1; return x+y;"));
        assertEquals(1F + (char) 1, exec("float x = 1; char y = 1; return x+y;"));
        assertEquals(1F + (short) 1, exec("float x = 1; short y = 1; return x+y;"));
        assertEquals(1F + 1, exec("float x = 1; int y = 1; return x+y;"));
        assertEquals(1F + 1L, exec("float x = 1; long y = 1; return x+y;"));
        assertEquals(1F + 1F, exec("float x = 1; float y = 1; return x+y;"));
        assertEquals(1F + 1.0, exec("float x = 1; double y = 1; return x+y;"));

        assertEquals(1.0 + (byte) 1, exec("double x = 1; byte y = 1; return x+y;"));
        assertEquals(1.0 + (char) 1, exec("double x = 1; char y = 1; return x+y;"));
        assertEquals(1.0 + (short) 1, exec("double x = 1; short y = 1; return x+y;"));
        assertEquals(1.0 + 1, exec("double x = 1; int y = 1; return x+y;"));
        assertEquals(1.0 + 1L, exec("double x = 1; long y = 1; return x+y;"));
        assertEquals(1.0 + 1F, exec("double x = 1; float y = 1; return x+y;"));
        assertEquals(1.0 + 1.0, exec("double x = 1; double y = 1; return x+y;"));
    }

    public void testBinaryPromotionConst() throws Exception {
        assertEquals((byte) 1 + (byte) 1, exec("return (byte)1 + (byte)1;"));
        assertEquals((byte) 1 + (char) 1, exec("return (byte)1 + (char)1;"));
        assertEquals((byte) 1 + (short) 1, exec("return (byte)1 + (short)1;"));
        assertEquals((byte) 1 + 1, exec("return (byte)1 + 1;"));
        assertEquals((byte) 1 + 1L, exec("return (byte)1 + 1L;"));
        assertEquals((byte) 1 + 1F, exec("return (byte)1 + 1F;"));
        assertEquals((byte) 1 + 1.0, exec("return (byte)1 + 1.0;"));

        assertEquals((char) 1 + (byte) 1, exec("return (char)1 + (byte)1;"));
        assertEquals((char) 1 + (char) 1, exec("return (char)1 + (char)1;"));
        assertEquals((char) 1 + (short) 1, exec("return (char)1 + (short)1;"));
        assertEquals((char) 1 + 1, exec("return (char)1 + 1;"));
        assertEquals((char) 1 + 1L, exec("return (char)1 + 1L;"));
        assertEquals((char) 1 + 1F, exec("return (char)1 + 1F;"));
        assertEquals((char) 1 + 1.0, exec("return (char)1 + 1.0;"));

        assertEquals((short) 1 + (byte) 1, exec("return (short)1 + (byte)1;"));
        assertEquals((short) 1 + (char) 1, exec("return (short)1 + (char)1;"));
        assertEquals((short) 1 + (short) 1, exec("return (short)1 + (short)1;"));
        assertEquals((short) 1 + 1, exec("return (short)1 + 1;"));
        assertEquals((short) 1 + 1L, exec("return (short)1 + 1L;"));
        assertEquals((short) 1 + 1F, exec("return (short)1 + 1F;"));
        assertEquals((short) 1 + 1.0, exec("return (short)1 + 1.0;"));

        assertEquals(1 + (byte) 1, exec("return 1 + (byte)1;"));
        assertEquals(1 + (char) 1, exec("return 1 + (char)1;"));
        assertEquals(1 + (short) 1, exec("return 1 + (short)1;"));
        assertEquals(1 + 1, exec("return 1 + 1;"));
        assertEquals(1 + 1L, exec("return 1 + 1L;"));
        assertEquals(1 + 1F, exec("return 1 + 1F;"));
        assertEquals(1 + 1.0, exec("return 1 + 1.0;"));

        assertEquals(1L + (byte) 1, exec("return 1L + (byte)1;"));
        assertEquals(1L + (char) 1, exec("return 1L + (char)1;"));
        assertEquals(1L + (short) 1, exec("return 1L + (short)1;"));
        assertEquals(1L + 1, exec("return 1L + 1;"));
        assertEquals(1L + 1L, exec("return 1L + 1L;"));
        assertEquals(1L + 1F, exec("return 1L + 1F;"));
        assertEquals(1L + 1.0, exec("return 1L + 1.0;"));

        assertEquals(1F + (byte) 1, exec("return 1F + (byte)1;"));
        assertEquals(1F + (char) 1, exec("return 1F + (char)1;"));
        assertEquals(1F + (short) 1, exec("return 1F + (short)1;"));
        assertEquals(1F + 1, exec("return 1F + 1;"));
        assertEquals(1F + 1L, exec("return 1F + 1L;"));
        assertEquals(1F + 1F, exec("return 1F + 1F;"));
        assertEquals(1F + 1.0, exec("return 1F + 1.0;"));

        assertEquals(1.0 + (byte) 1, exec("return 1.0 + (byte)1;"));
        assertEquals(1.0 + (char) 1, exec("return 1.0 + (char)1;"));
        assertEquals(1.0 + (short) 1, exec("return 1.0 + (short)1;"));
        assertEquals(1.0 + 1, exec("return 1.0 + 1;"));
        assertEquals(1.0 + 1L, exec("return 1.0 + 1L;"));
        assertEquals(1.0 + 1F, exec("return 1.0 + 1F;"));
        assertEquals(1.0 + 1.0, exec("return 1.0 + 1.0;"));
    }
}
