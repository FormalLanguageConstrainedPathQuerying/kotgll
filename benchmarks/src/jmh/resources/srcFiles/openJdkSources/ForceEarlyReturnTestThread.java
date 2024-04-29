/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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


package nsk.share.jpda;

import java.net.*;
import nsk.share.*;

public class ForceEarlyReturnTestThread
extends Thread
{
    void VoidMethod()
    {
        try
        {
            log("in void method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();
    }

    boolean BooleanMethod()
    {
        try
        {
            log("in boolean method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedBooleanValue;
    }

    byte ByteMethod()
    {
        try
        {
            log("in byte method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedByteValue;
    }

    short ShortMethod()
    {
        try
        {
            log("in short method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedShortValue;
    }

    char CharMethod()
    {
        try
        {
            log("in char method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedCharValue;
    }

    int IntMethod()
    {
        try
        {
            log("in int method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedIntValue;
    }

    long LongMethod()
    {
        try
        {
            log("in long method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedLongValue;
    }

    float FloatMethod()
    {
        try
        {
            log("in float method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedFloatValue;
    }

    double DoubleMethod()
    {
        try
        {
            log("in double method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedDoubleValue;
    }

    Object[] ObjectArrayMethod()
    {
        try
        {
            log("in object array method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedObjectArrayValue;
    }

    String StringMethod()
    {
        try
        {
            log("in string method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedStringValue;
    }

    Thread ThreadMethod()
    {
        try
        {
            log("in thread method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedThreadValue;
    }

    ThreadGroup ThreadGroupMethod()
    {
        try
        {
            log("in thread group method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedThreadGroupValue;
    }

    Class<?> ClassObjectMethod()
    {
        try
        {
            log("in class object method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedClassObjectValue;
    }

    ClassLoader ClassLoaderMethod()
    {
        try
        {
            log("in class loader method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedClassLoaderValue;
    }

    Object ObjectMethod()
    {
        try
        {
            log("in object method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedObjectValue;
    }

    Boolean BooleanWrapperMethod()
    {
        try
        {
            log("in boolean wrapper method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedBooleanWrapperValue;
    }

    Byte ByteWrapperMethod()
    {
        try
        {
            log("in byte wrapper method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedByteWrapperValue;
    }

    Short ShortWrapperMethod()
    {
        try
        {
            log("in short wrapper method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedShortWrapperValue;
    }

    Character CharWrapperMethod()
    {
        try
        {
            log("in char wrapper method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedCharWrapperValue;
    }

    Integer IntWrapperMethod()
    {
        try
        {
            log("in int wrapper method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedIntWrapperValue;
    }

    Long LongWrapperMethod()
    {
        try
        {
            log("in long wrapper method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedLongWrapperValue;
    }

    Float FloatWrapperMethod()
    {
        try
        {
            log("in float wrapper method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedFloatWrapperValue;
    }

    Double DoubleWrapperMethod()
    {
        try
        {
            log("in double wrapper method"); 
        }
        finally
        {
            unexpectedMethod();
        }

        unexpectedMethod();

        return unexpectedDoubleWrapperValue;
    }

    private void log(String message)
    {
        log.display(currentThread().getName() + ": " + message);
    }

    private void logError(String message)
    {
        log.complain(currentThread().getName() + ": " + message);
    }


    public static boolean expectedBooleanValue = Boolean.TRUE;
    public static byte expectedByteValue = Byte.MAX_VALUE;
    public static char expectedCharValue = Character.MAX_VALUE;
    public static short expectedShortValue = Short.MAX_VALUE;
    public static int expectedIntValue = Integer.MAX_VALUE;
    public static long expectedLongValue = Long.MAX_VALUE;
    public static float expectedFloatValue = Float.MAX_VALUE;
    public static double expectedDoubleValue = Double.MAX_VALUE;
    public static Object[] expectedObjectArrayValue = new Object[1000];
    public static Thread expectedThreadValue = new Thread();
    public static ThreadGroup expectedThreadGroupValue = new ThreadGroup("Expected thread group");
    public static Class<?> expectedClassObjectValue = ForceEarlyReturnTestThread.class;
    public static ClassLoader expectedClassLoaderValue = new URLClassLoader(new URL[]{});
    public static String expectedStringValue = "EXPECTED STRING";
    public static Object expectedObjectValue = new Object();
    public static Boolean expectedBooleanWrapperValue = Boolean.valueOf(Boolean.TRUE);
    public static Byte expectedByteWrapperValue = Byte.valueOf(Byte.MAX_VALUE);
    public static Character expectedCharWrapperValue = Character.valueOf(Character.MAX_VALUE);
    public static Short expectedShortWrapperValue = Short.valueOf(Short.MAX_VALUE);
    public static Integer expectedIntWrapperValue = Integer.valueOf(Integer.MAX_VALUE);
    public static Long expectedLongWrapperValue = Long.valueOf(Long.MAX_VALUE);
    public static Float expectedFloatWrapperValue = Float.valueOf(Float.MAX_VALUE);
    public static Double expectedDoubleWrapperValue = Double.valueOf(Double.MAX_VALUE);


    public static boolean unexpectedBooleanValue = Boolean.FALSE;
    public static byte unexpectedByteValue = 0;
    public static char unexpectedCharValue = 0;
    public static short unexpectedShortValue = 0;
    public static int unexpectedIntValue = 0;
    public static long unexpectedLongValue = 0;
    public static float unexpectedFloatValue = 0;
    public static double unexpectedDoubleValue = 0;
    public static Object[] unexpectedObjectArrayValue = new Object[1000];
    public static String unexpectedStringValue = "UNEXPECTED STRING";
    public static Thread unexpectedThreadValue = new Thread();
    public static ThreadGroup unexpectedThreadGroupValue = new ThreadGroup("Unexpected thread group");
    public static Class<?> unexpectedClassObjectValue = Object.class;
    public static ClassLoader unexpectedClassLoaderValue = new URLClassLoader(new URL[]{});
    public static Object unexpectedObjectValue = new Object();
    public static Boolean unexpectedBooleanWrapperValue = Boolean.valueOf(Boolean.FALSE);
    public static Byte unexpectedByteWrapperValue = Byte.valueOf((byte)0);
    public static Character unexpectedCharWrapperValue = Character.valueOf((char)0);
    public static Short unexpectedShortWrapperValue = Short.valueOf((short)0);
    public static Integer unexpectedIntWrapperValue = Integer.valueOf(0);
    public static Long unexpectedLongWrapperValue = Long.valueOf(0);
    public static Float unexpectedFloatWrapperValue = Float.valueOf(0);
    public static Double unexpectedDoubleWrapperValue = Double.valueOf(0);

    public static int[] breakpointLines = {
        49,
        63,
        79,
        95,
        111,
        127,
        143,
        159,
        175,
        191,
        207,
        223,
        239,
        255,
        271,
        287,
        303,
        319,
        335,
        351,
        367,
        383,
        399,
        415};

    /* Invalid data for ForceEarlyReturn, needed to check is ForceEarlyReturn complies with following part of specification:
     * Object values must be assignment compatible with the method return type
     * (This implies that the method return type must be loaded through the enclosing class's class loader).
     * Primitive values must be either assignment compatible with the method return type or must
     * be convertible to the variable type without loss of information.
     */
    public static boolean invalidVoidValue = Boolean.TRUE;
    public static boolean invalidObjectValue = Boolean.TRUE;
    public static byte invalidBooleanValue = Byte.MAX_VALUE;
    public static short invalidByteValue = Short.MAX_VALUE;
    public static char invalidShortValue = Character.MAX_VALUE;
    public static int invalidCharValue = Integer.MAX_VALUE;
    public static long invalidIntValue = Long.MAX_VALUE;
    public static float invalidLongValue = Float.MAX_VALUE;
    public static double invalidFloatValue = Double.MAX_VALUE;
    public static Object[] invalidDoubleValue = new Object[1000];
    public static String invalidObjectArrayValue = "EXPECTED STRING";
    public static Thread invalidStringValue = new Thread("Invalid thread");
    public static ThreadGroup invalidThreadValue = new ThreadGroup("Invalid thread group");
    public static Class<?> invalidThreadGroupValue = ForceEarlyReturnTestThread.class;
    public static ClassLoader invalidClassObjectValue = new URLClassLoader(new URL[]{});
    public static Object invalidClassLoaderValue = new Object();
    public static Byte invalidBooleanWrapperValue = Byte.valueOf(Byte.MAX_VALUE);
    public static Short invalidByteWrapperValue = Short.valueOf(Short.MAX_VALUE);
    public static Character invalidShortWrapperValue = Character.valueOf(Character.MAX_VALUE);
    public static Integer invalidCharWrapperValue = Integer.valueOf(Integer.MAX_VALUE);
    public static Long invalidIntWrapperValue = Long.valueOf(Long.MAX_VALUE);
    public static Float invalidLongWrapperValue = Float.valueOf(Float.MAX_VALUE);
    public static Double invalidFloatWrapperValue = Double.valueOf(Double.MAX_VALUE);
    public static Object[] invalidDoubleWrapperValue = new Object[1000];

    public static String testedTypesNames[] =
    {
        "Void",
        "Boolean",
        "Byte",
        "Short",
        "Char",
        "Int",
        "Long",
        "Float",
        "Double",
        "ObjectArray",
        "String",
        "Thread",
        "ThreadGroup",
        "ClassObject",
        "ClassLoader",
        "Object",
        "BooleanWrapper",
        "ByteWrapper",
        "ShortWrapper",
        "CharWrapper",
        "IntWrapper",
        "LongWrapper",
        "FloatWrapper",
        "DoubleWrapper",
        };

    private Log log;

    private boolean isTestThread;

    private int iterationsNumber = 1;

    private Wicket startExecutionWicket = new Wicket();

    private boolean success = true;

    public ForceEarlyReturnTestThread(Log log, boolean isTestThread, int iterationNumber)
    {
        this.log = log;
        this.isTestThread = isTestThread;

        this.iterationsNumber = iterationNumber;
    }

    private volatile boolean stopExecution;

    public void stopExecution()
    {
        stopExecution = true;
    }

    public void startExecuion()
    {
        startExecutionWicket.unlockAll();
    }

    public void run()
    {
        startExecutionWicket.waitFor();

        int iterationCount = 0;

        while(!stopExecution && (!isTestThread || ((iterationsNumber == 0) || (iterationCount++ < iterationsNumber))))
        {
            for(int i = 0; (i < testedTypesNames.length) && !stopExecution; i++)
            {
                executeMethod(testedTypesNames[i] + "Method");

                /*
                 * Small delay was inserted because of if test starts several ForceEarlyReturnTestThreads
                 * with parameter isTestThread = false, these threads may consume too many CPU time and test
                 * execution will be very slow
                 */
                if (!isTestThread) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        logError("Unexpected exception: " + e);
                        e.printStackTrace(log.getOutStream());
                        success = false;
                    }
                }
            }
        }

        log("Test thread exit");
    }

    private void executeMethod(String methodName)
    {
        if(methodName.equals("VoidMethod"))
        {
            VoidMethod();
        }
        if(methodName.equals("BooleanMethod"))
        {
            boolean result = BooleanMethod();


            boolean expectedResult;

            expectedResult = isTestThread ? expectedBooleanValue : unexpectedBooleanValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ByteMethod"))
        {
            byte result = ByteMethod();


            byte expectedResult;

            expectedResult = isTestThread ? expectedByteValue : unexpectedByteValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("CharMethod"))
        {
            char result = CharMethod();


            char expectedResult;

            expectedResult = isTestThread ? expectedCharValue : unexpectedCharValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ShortMethod"))
        {
            short result = ShortMethod();


            short expectedResult;

            expectedResult = isTestThread ? expectedShortValue : unexpectedShortValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("IntMethod"))
        {
            int result = IntMethod();


            int expectedResult;

            expectedResult = isTestThread ? expectedIntValue : unexpectedIntValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("LongMethod"))
        {
            long result = LongMethod();


            long expectedResult;

            expectedResult = isTestThread ? expectedLongValue : unexpectedLongValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("FloatMethod"))
        {
            float result = FloatMethod();


            float expectedResult;

            expectedResult = isTestThread ? expectedFloatValue : unexpectedFloatValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("DoubleMethod"))
        {
            double result = DoubleMethod();


            double expectedResult;

            expectedResult = isTestThread ? expectedDoubleValue : unexpectedDoubleValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("StringMethod"))
        {
            String result = StringMethod();


            String expectedResult;

            expectedResult = isTestThread ? expectedStringValue : unexpectedStringValue;

            if(!result.equals(expectedResult))
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ObjectMethod"))
        {
            Object result = ObjectMethod();


            Object expectedResult;

            expectedResult = isTestThread ? expectedObjectValue : unexpectedObjectValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ObjectArrayMethod"))
        {
            Object[] result = ObjectArrayMethod();


            Object[] expectedResult;

            expectedResult = isTestThread ? expectedObjectArrayValue : unexpectedObjectArrayValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ThreadMethod"))
        {
            Thread result = ThreadMethod();


            Thread expectedResult;

            expectedResult = isTestThread ? expectedThreadValue : unexpectedThreadValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ThreadGroupMethod"))
        {
            ThreadGroup result = ThreadGroupMethod();


            ThreadGroup expectedResult;

            expectedResult = isTestThread ? expectedThreadGroupValue : unexpectedThreadGroupValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ClassObjectMethod"))
        {
            Class<?> result = ClassObjectMethod();


            Class<?> expectedResult;

            expectedResult = isTestThread ? expectedClassObjectValue : unexpectedClassObjectValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ClassLoaderMethod"))
        {
            ClassLoader result = ClassLoaderMethod();


            ClassLoader expectedResult;

            expectedResult = isTestThread ? expectedClassLoaderValue : unexpectedClassLoaderValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("BooleanWrapperMethod"))
        {
            Boolean result = BooleanWrapperMethod();


            Boolean expectedResult;

            expectedResult = isTestThread ? expectedBooleanWrapperValue : unexpectedBooleanWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ByteWrapperMethod"))
        {
            Byte result = ByteWrapperMethod();


            Byte expectedResult;

            expectedResult = isTestThread ? expectedByteWrapperValue : unexpectedByteWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("ShortWrapperMethod"))
        {
            Short result = ShortWrapperMethod();


            Short expectedResult;

            expectedResult = isTestThread ? expectedShortWrapperValue : unexpectedShortWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("CharWrapperMethod"))
        {
            Character result = CharWrapperMethod();


            Character expectedResult;

            expectedResult = isTestThread ? expectedCharWrapperValue : unexpectedCharWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("IntWrapperMethod"))
        {
            Integer result = IntWrapperMethod();


            Integer expectedResult;

            expectedResult = isTestThread ? expectedIntWrapperValue : unexpectedIntWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("LongWrapperMethod"))
        {
            Long result = LongWrapperMethod();


            Long expectedResult;

            expectedResult = isTestThread ? expectedLongWrapperValue : unexpectedLongWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("FloatWrapperMethod"))
        {
            Float result = FloatWrapperMethod();


            Float expectedResult;

            expectedResult = isTestThread ? expectedFloatWrapperValue : unexpectedFloatWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
        if(methodName.equals("DoubleWrapperMethod"))
        {
            Double result = DoubleWrapperMethod();


            Double expectedResult;

            expectedResult = isTestThread ? expectedDoubleWrapperValue : unexpectedDoubleWrapperValue;

            if(result != expectedResult)
            {
                logError("unexpected result of "  + methodName + ": " + result + ", expected is: " + expectedResult);
                success = false;
            }
        }
    }

    void unexpectedMethod()
    {
        if(isTestThread)
        {
            success = false;
            logError("unexpected code is executed after forceEarlyReturn");
        }
    }

    public boolean getSuccess()
    {
        return success;
    }
}
