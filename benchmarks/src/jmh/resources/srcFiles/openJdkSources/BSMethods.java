/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4098239 4107540 4080736 4261102 4274710 4305272
 *      4979017 4979028 4979031 5030267 6222207 8040806
 * @summary Test the operation of the methods of BitSet class
 * @author Mike McCloskey, Martin Buchholz
 * @run main/othervm BSMethods
 * @key randomness
 */

import java.util.*;

/**
 * This is a simple test class created to run tests on the BitSet class.
 *
 */
public class BSMethods {

    private static Random generator = new Random();
    private static boolean failure = false;

    private static void fail(String diagnostic) {
        new Error(diagnostic).printStackTrace();
        failure = true;
    }

    private static void check(boolean condition) {
        check(condition, "something's fishy");
    }

    private static void check(boolean condition, String diagnostic) {
        if (! condition)
            fail(diagnostic);
    }

    private static void checkEmpty(BitSet s) {
        check(s.isEmpty(), "isEmpty");
        check(s.length() == 0, "length");
        check(s.cardinality() == 0, "cardinality");
        check(s.equals(new BitSet())   , "equals");
        check(s.equals(new BitSet(0))  , "equals");
        check(s.equals(new BitSet(127)), "equals");
        check(s.equals(new BitSet(128)), "equals");
        check(s.nextSetBit(0)   == -1, "nextSetBit");
        check(s.nextSetBit(127) == -1, "nextSetBit");
        check(s.nextSetBit(128) == -1, "nextSetBit");
        check(s.nextClearBit(0)   == 0,   "nextClearBit");
        check(s.nextClearBit(127) == 127, "nextClearBit");
        check(s.nextClearBit(128) == 128, "nextClearBit");
        check(s.toString().equals("{}"), "toString");
        check(! s.get(0), "get");
    }

    private static BitSet makeSet(int... elts) {
        BitSet s = new BitSet();
        for (int elt : elts)
            s.set(elt);
        return s;
    }

    private static void checkEquality(BitSet s, BitSet t) {
        checkSanity(s, t);
        check(s.equals(t), "equals");
        check(s.toString().equals(t.toString()), "equal strings");
        check(s.length() == t.length(), "equal lengths");
        check(s.cardinality() == t.cardinality(), "equal cardinalities");
    }

    private static void checkSanity(BitSet... sets) {
        for (BitSet s : sets) {
            int len = s.length();
            int cardinality1 = s.cardinality();
            int cardinality2 = 0;
            for (int i = s.nextSetBit(0); i >= 0; i = s.nextSetBit(i+1)) {
                check(s.get(i));
                cardinality2++;
            }
            check(s.nextSetBit(len) == -1, "last set bit");
            check(s.nextClearBit(len) == len, "last set bit");
            check(s.isEmpty() == (len == 0), "emptiness");
            check(cardinality1 == cardinality2, "cardinalities");
            check(len <= s.size(), "length <= size");
            check(len >= 0, "length >= 0");
            check(cardinality1 >= 0, "cardinality >= 0");
        }
    }

    public static void main(String[] args) {


        testSetGetClearFlip();

        testClear();

        testFlip();
        testSet();
        testGet();

        testAndNot();
        testAnd();
        testOr();
        testXor();

        testLength();
        testEquals();
        testNextSetBit();
        testNextClearBit();
        testIntersects();
        testCardinality();
        testEmpty();
        testEmpty2();
        testToString();
        testLogicalIdentities();

        if (failure)
            throw new RuntimeException("One or more BitSet failures.");
    }

    private static void report(String testName, int failCount) {
        System.err.println(testName+": " +
                           (failCount==0 ? "Passed":"Failed("+failCount+")"));
        if (failCount > 0)
            failure = true;
    }

    private static void testFlipTime() {
        BitSet b1 = new BitSet();
        b1.set(1000);
        long startTime = System.currentTimeMillis();
        for(int x=0; x<100000; x++) {
            b1.flip(100, 900);
        }
        long endTime = System.currentTimeMillis();
        long total = endTime - startTime;
        System.out.println("Multiple word flip Time "+total);

        startTime = System.currentTimeMillis();
        for(int x=0; x<100000; x++) {
            b1.flip(2, 44);
        }
        endTime = System.currentTimeMillis();
        total = endTime - startTime;
        System.out.println("Single word flip Time "+total);
    }

    private static void testNextSetBit() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            int numberOfSetBits = generator.nextInt(100) + 1;
            BitSet testSet = new BitSet();
            int[] history = new int[numberOfSetBits];

            int nextBitToSet = 0;
            for (int x=0; x<numberOfSetBits; x++) {
                nextBitToSet += generator.nextInt(30)+1;
                history[x] = nextBitToSet;
                testSet.set(nextBitToSet);
            }

            int historyIndex = 0;
            for(int x=testSet.nextSetBit(0); x>=0; x=testSet.nextSetBit(x+1)) {
                if (x != history[historyIndex++])
                    failCount++;
            }

            checkSanity(testSet);
        }

        report("NextSetBit                  ", failCount);
    }

    private static void testNextClearBit() {
        int failCount = 0;

        for (int i=0; i<1000; i++) {
            BitSet b = new BitSet(256);
            int[] history = new int[10];

            for (int x=0; x<256; x++)
                b.set(x);

            int nextBitToClear = 0;
            for (int x=0; x<10; x++) {
                nextBitToClear += generator.nextInt(24)+1;
                history[x] = nextBitToClear;
                b.clear(nextBitToClear);
            }

            int historyIndex = 0;
            for(int x=b.nextClearBit(0); x<256; x=b.nextClearBit(x+1)) {
                if (x != history[historyIndex++])
                    failCount++;
            }

            checkSanity(b);
        }

        BitSet bs  = new BitSet();
        if (bs.nextClearBit(0) != 0)
                failCount++;
        for (int i = 0; i < 64; i++) {
            bs.set(i);
            if (bs.nextClearBit(0) != i+1)
                failCount++;
        }

        checkSanity(bs);

        report("NextClearBit                ", failCount);
    }

    private static void testSetGetClearFlip() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet testSet = new BitSet();
            HashSet<Integer> history = new HashSet<Integer>();

            int nextBitToSet = 0;
            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;
            for (int x=0; x<numberOfSetBits; x++) {
                nextBitToSet = generator.nextInt(highestPossibleSetBit);
                history.add(new Integer(nextBitToSet));
                testSet.set(nextBitToSet);
            }

            for (int x=0; x<highestPossibleSetBit; x++) {
                if (testSet.get(x) != history.contains(new Integer(x)))
                    failCount++;
            }

            Iterator<Integer> setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = setBitIterator.next();
                testSet.clear(setBit.intValue());
            }

            for (int x=0; x<highestPossibleSetBit; x++)
                if (testSet.get(x))
                    failCount++;
            if(testSet.length() != 0)
                failCount++;

            setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = setBitIterator.next();
                testSet.set(setBit.intValue(), true);
            }

            for (int x=0; x<highestPossibleSetBit; x++) {
                if (testSet.get(x) != history.contains(new Integer(x)))
                    failCount++;
            }

            setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = (Integer)setBitIterator.next();
                testSet.set(setBit.intValue(), false);
            }

            for (int x=0; x<highestPossibleSetBit; x++)
                if (testSet.get(x))
                    failCount++;
            if(testSet.length() != 0)
                failCount++;

            setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = (Integer)setBitIterator.next();
                testSet.flip(setBit.intValue());
            }

            for (int x=0; x<highestPossibleSetBit; x++) {
                if (testSet.get(x) != history.contains(new Integer(x)))
                    failCount++;
            }

            setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = (Integer)setBitIterator.next();
                testSet.flip(setBit.intValue());
            }

            for (int x=0; x<highestPossibleSetBit; x++)
                if (testSet.get(x))
                    failCount++;
            if(testSet.length() != 0)
                failCount++;

            checkSanity(testSet);
        }

        report("SetGetClearFlip             ", failCount);
    }

    private static void testAndNot() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            BitSet b2 = new BitSet(256);

            int nextBitToSet = 0;
            for (int x=0; x<10; x++)
                b1.set(generator.nextInt(255));

            for (int x=10; x<20; x++)
                b2.set(generator.nextInt(255));

            BitSet b3 = (BitSet)b1.clone();
            b3.andNot(b2);

            for(int x=0; x<256; x++) {
                boolean bit1 = b1.get(x);
                boolean bit2 = b2.get(x);
                boolean bit3 = b3.get(x);
                if (!(bit3 == (bit1 & (!bit2))))
                    failCount++;
            }
            checkSanity(b1, b2, b3);
        }

        report("AndNot                      ", failCount);
    }

    private static void testAnd() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            BitSet b2 = new BitSet(256);

            int nextBitToSet = 0;
            for (int x=0; x<10; x++)
                b1.set(generator.nextInt(255));

            for (int x=10; x<20; x++)
                b2.set(generator.nextInt(255));

            BitSet b3 = (BitSet)b1.clone();
            b3.and(b2);

            for(int x=0; x<256; x++) {
                boolean bit1 = b1.get(x);
                boolean bit2 = b2.get(x);
                boolean bit3 = b3.get(x);
                if (!(bit3 == (bit1 & bit2)))
                    failCount++;
            }
            checkSanity(b1, b2, b3);
        }

        BitSet b4 = makeSet(2, 127);
        b4.and(makeSet(2, 64));
        checkSanity(b4);
        if (!(b4.equals(makeSet(2))))
            failCount++;

        report("And                         ", failCount);
    }

    private static void testOr() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            BitSet b2 = new BitSet(256);
            int[] history = new int[20];

            int nextBitToSet = 0;
            for (int x=0; x<10; x++) {
                nextBitToSet = generator.nextInt(255);
                history[x] = nextBitToSet;
                b1.set(nextBitToSet);
            }

            for (int x=10; x<20; x++) {
                nextBitToSet = generator.nextInt(255);
                history[x] = nextBitToSet;
                b2.set(nextBitToSet);
            }

            BitSet b3 = (BitSet)b1.clone();
            b3.or(b2);

            int historyIndex = 0;
            for(int x=0; x<20; x++) {
                if (!b3.get(history[x]))
                    failCount++;
            }

            for(int x=0; x<256; x++) {
                boolean bit1 = b1.get(x);
                boolean bit2 = b2.get(x);
                boolean bit3 = b3.get(x);
                if (!(bit3 == (bit1 | bit2)))
                    failCount++;
            }
            checkSanity(b1, b2, b3);
        }

        report("Or                          ", failCount);
    }

    private static void testXor() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            BitSet b2 = new BitSet(256);

            int nextBitToSet = 0;
            for (int x=0; x<10; x++)
                b1.set(generator.nextInt(255));

            for (int x=10; x<20; x++)
                b2.set(generator.nextInt(255));

            BitSet b3 = (BitSet)b1.clone();
            b3.xor(b2);

            for(int x=0; x<256; x++) {
                boolean bit1 = b1.get(x);
                boolean bit2 = b2.get(x);
                boolean bit3 = b3.get(x);
                if (!(bit3 == (bit1 ^ bit2)))
                    failCount++;
            }
            checkSanity(b1, b2, b3);
            b3.xor(b3); checkEmpty(b3);
        }

        BitSet b4 = makeSet(2, 64, 127);
        b4.xor(makeSet(64, 127));
        checkSanity(b4);
        if (!(b4.equals(makeSet(2))))
            failCount++;

        report("Xor                         ", failCount);
    }

    private static void testEquals() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(generator.nextInt(1000)+1);
            BitSet b2 = new BitSet(generator.nextInt(1000)+1);

            int nextBitToSet = 0;
            for (int x=0; x<10; x++) {
                nextBitToSet += generator.nextInt(50)+1;
                b1.set(nextBitToSet);
                b2.set(nextBitToSet);
            }

            if (!b1.equals(b2))
                failCount++;
            checkEquality(b1,b2);
        }

        report("Equals                      ", failCount);
    }

    private static void testLength() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            int highestSetBit = 0;

            for(int x=0; x<100; x++) {
                int nextBitToSet = generator.nextInt(255);
                if (nextBitToSet > highestSetBit)
                    highestSetBit = nextBitToSet;
                b1.set(nextBitToSet);
                if (b1.length() != highestSetBit + 1)
                    failCount++;
            }
            checkSanity(b1);
        }

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            for(int x=0; x<100; x++) {
                int rangeStart = generator.nextInt(100);
                int rangeEnd = rangeStart + generator.nextInt(100);
                b1.flip(rangeStart);
                b1.flip(rangeStart);
                if (b1.length() != 0)
                    failCount++;
                b1.flip(rangeStart, rangeEnd);
                b1.flip(rangeStart, rangeEnd);
                if (b1.length() != 0)
                    failCount++;
            }
            checkSanity(b1);
        }

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            BitSet b2 = new BitSet(256);
            int bit1 = generator.nextInt(100);
            int bit2 = generator.nextInt(100);
            int highestSetBit = (bit1 > bit2) ? bit1 : bit2;
            b1.set(bit1);
            b2.set(bit2);
            b1.or(b2);
            if (b1.length() != highestSetBit + 1)
                failCount++;
            checkSanity(b1, b2);
        }

        report("Length                      ", failCount);
    }

    private static void testClear() {
        int failCount = 0;

        for (int i=0; i<1000; i++) {
            BitSet b1 = new BitSet();

            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            BitSet b2 = (BitSet)b1.clone();

            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(100);

            b1.clear(rangeStart, rangeEnd);

            for (int x=rangeStart; x<rangeEnd; x++)
                b2.clear(x);

            if (!b1.equals(b2)) {
                System.out.println("rangeStart = " + rangeStart);
                System.out.println("rangeEnd = " + rangeEnd);
                System.out.println("b1 = " + b1);
                System.out.println("b2 = " + b2);
                failCount++;
            }
            checkEquality(b1,b2);
        }

        report("Clear                       ", failCount);
    }

    private static void testSet() {
        int failCount = 0;

        for (int i=0; i<1000; i++) {
            BitSet b1 = new BitSet();

            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            BitSet b2 = (BitSet)b1.clone();

            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(100);

            b1.set(rangeStart, rangeEnd);

            for (int x=rangeStart; x<rangeEnd; x++)
                b2.set(x);

            if (!b1.equals(b2)) {
                System.out.println("Set 1");
                System.out.println("rangeStart = " + rangeStart);
                System.out.println("rangeEnd = " + rangeEnd);
                System.out.println("b1 = " + b1);
                System.out.println("b2 = " + b2);
                failCount++;
            }
            checkEquality(b1,b2);
        }

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet();

            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            BitSet b2 = (BitSet)b1.clone();
            boolean setOrClear = generator.nextBoolean();

            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(100);

            b1.set(rangeStart, rangeEnd, setOrClear);

            for (int x=rangeStart; x<rangeEnd; x++)
                b2.set(x, setOrClear);

            if (!b1.equals(b2)) {
                System.out.println("Set 2");
                System.out.println("b1 = " + b1);
                System.out.println("b2 = " + b2);
                failCount++;
            }
            checkEquality(b1,b2);
        }

        report("Set                         ", failCount);
    }

    private static void testFlip() {
        int failCount = 0;

        for (int i=0; i<1000; i++) {
            BitSet b1 = new BitSet();

            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            BitSet b2 = (BitSet)b1.clone();

            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(100);

            b1.flip(rangeStart, rangeEnd);

            for (int x=rangeStart; x<rangeEnd; x++)
                b2.flip(x);

            if (!b1.equals(b2))
                failCount++;
            checkEquality(b1,b2);
        }

        report("Flip                        ", failCount);
    }

    private static void testGet() {
        int failCount = 0;

        for (int i=0; i<1000; i++) {
            BitSet b1 = new BitSet();

            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(100);

            BitSet b2 = b1.get(rangeStart, rangeEnd);

            BitSet b3 = new BitSet();
            for(int x=rangeStart; x<rangeEnd; x++)
                b3.set(x-rangeStart, b1.get(x));

            if (!b2.equals(b3)) {
                System.out.println("start="+rangeStart);
                System.out.println("end="+rangeEnd);
                System.out.println(b1);
                System.out.println(b2);
                System.out.println(b3);
                failCount++;
            }
            checkEquality(b2,b3);
        }

        report("Get                         ", failCount);
    }


    private static void testIntersects() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            BitSet b2 = new BitSet(256);

            int nextBitToSet = 0;
            for (int x=0; x<30; x++) {
                nextBitToSet = generator.nextInt(255);
                b1.set(nextBitToSet);
            }

            for (int x=0; x<30; x++) {
                nextBitToSet = generator.nextInt(255);
                b2.set(nextBitToSet);
            }

            nextBitToSet = generator.nextInt(255);
            b1.set(nextBitToSet);
            b2.set(nextBitToSet);

            if (!b1.intersects(b2))
                failCount++;

            b1.andNot(b2);

            if (b1.intersects(b2))
                failCount++;

            checkSanity(b1, b2);
        }

        report("Intersects                  ", failCount);
    }

    private static void testCardinality() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);

            int nextBitToSet = 0;
            int iterations = generator.nextInt(20)+1;
            for (int x=0; x<iterations; x++) {
                nextBitToSet += generator.nextInt(20)+1;
                b1.set(nextBitToSet);
            }

            if (b1.cardinality() != iterations) {
                System.out.println("Iterations is "+iterations);
                System.out.println("Cardinality is "+b1.cardinality());
                failCount++;
            }

            checkSanity(b1);
        }

        report("Cardinality                 ", failCount);
    }

    private static void testEmpty() {
        int failCount = 0;

        BitSet b1 = new BitSet();
        if (!b1.isEmpty())
            failCount++;

        int nextBitToSet = 0;
        int numberOfSetBits = generator.nextInt(100) + 1;
        int highestPossibleSetBit = generator.nextInt(1000) + 1;
        for (int x=0; x<numberOfSetBits; x++) {
            nextBitToSet = generator.nextInt(highestPossibleSetBit);
            b1.set(nextBitToSet);
            if (b1.isEmpty())
                failCount++;
            b1.clear(nextBitToSet);
            if (!b1.isEmpty())
                failCount++;
        }

        report("Empty                       ", failCount);
    }

    private static void testEmpty2() {
        {BitSet t = new BitSet(); t.set(100); t.clear(3,600); checkEmpty(t);}
        checkEmpty(new BitSet(0));
        checkEmpty(new BitSet(342));
        BitSet s = new BitSet(0);
        checkEmpty(s);
        s.clear(92);      checkEmpty(s);
        s.clear(127,127); checkEmpty(s);
        s.set(127,127);   checkEmpty(s);
        s.set(128,128);   checkEmpty(s);
        BitSet empty = new BitSet();
        {BitSet t = new BitSet(); t.and   (empty);     checkEmpty(t);}
        {BitSet t = new BitSet(); t.or    (empty);     checkEmpty(t);}
        {BitSet t = new BitSet(); t.xor   (empty);     checkEmpty(t);}
        {BitSet t = new BitSet(); t.andNot(empty);     checkEmpty(t);}
        {BitSet t = new BitSet(); t.and   (t);         checkEmpty(t);}
        {BitSet t = new BitSet(); t.or    (t);         checkEmpty(t);}
        {BitSet t = new BitSet(); t.xor   (t);         checkEmpty(t);}
        {BitSet t = new BitSet(); t.andNot(t);         checkEmpty(t);}
        {BitSet t = new BitSet(); t.and(makeSet(1));   checkEmpty(t);}
        {BitSet t = new BitSet(); t.and(makeSet(127)); checkEmpty(t);}
        {BitSet t = new BitSet(); t.and(makeSet(128)); checkEmpty(t);}
        {BitSet t = new BitSet(); t.flip(7);t.flip(7); checkEmpty(t);}
        {BitSet t = new BitSet(); checkEmpty(t.get(200,300));}
        {BitSet t = makeSet(2,5); check(t.get(2,6).equals(makeSet(0,3)),"");}
    }

    private static void testToString() {
        check(new BitSet().toString().equals("{}"));
        check(makeSet(2,3,42,43,234).toString().equals("{2, 3, 42, 43, 234}"));

        final long MB = 1024*1024;
        if (Runtime.getRuntime().maxMemory() >= 512*MB) {
            try {
                check(makeSet(Integer.MAX_VALUE-1).toString().equals(
                        "{" + (Integer.MAX_VALUE-1) + "}"));
                check(makeSet(Integer.MAX_VALUE).toString().equals(
                        "{" + Integer.MAX_VALUE + "}"));
                check(makeSet(0, 1, Integer.MAX_VALUE-1, Integer.MAX_VALUE).toString().equals(
                        "{0, 1, " + (Integer.MAX_VALUE-1) + ", " + Integer.MAX_VALUE + "}"));
            } catch (IndexOutOfBoundsException exc) {
                fail("toString() with indices near MAX_VALUE");
            }
        }
    }

    private static void testLogicalIdentities() {
        int failCount = 0;

        for (int i=0; i<50; i++) {
            BitSet b1 = new BitSet();
            BitSet b2 = new BitSet();

            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++) {
                b1.set(generator.nextInt(highestPossibleSetBit));
                b2.set(generator.nextInt(highestPossibleSetBit));
            }

            BitSet b3 = (BitSet) b1.clone();
            BitSet b4 = (BitSet) b2.clone();

            for (int x=0; x<highestPossibleSetBit; x++) {
                b1.flip(x);
                b2.flip(x);
            }
            b1.or(b2);
            b3.and(b4);
            for (int x=0; x<highestPossibleSetBit; x++)
                b3.flip(x);
            if (!b1.equals(b3))
                failCount++;
            checkSanity(b1, b2, b3, b4);
        }

        for (int i=0; i<50; i++) {
            BitSet b1 = new BitSet();
            BitSet b2 = new BitSet();

            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++) {
                b1.set(generator.nextInt(highestPossibleSetBit));
                b2.set(generator.nextInt(highestPossibleSetBit));
            }

            BitSet b3 = (BitSet) b1.clone();
            BitSet b4 = (BitSet) b2.clone();
            BitSet b5 = (BitSet) b1.clone();
            BitSet b6 = (BitSet) b2.clone();

            for (int x=0; x<highestPossibleSetBit; x++)
                b2.flip(x);
            b1.and(b2);
            for (int x=0; x<highestPossibleSetBit; x++)
                b3.flip(x);
            b3.and(b4);
            b1.or(b3);
            b5.xor(b6);
            if (!b1.equals(b5))
                failCount++;
            checkSanity(b1, b2, b3, b4, b5, b6);
        }
        report("Logical Identities          ", failCount);
    }

}
