/*
 * Copyright (c) 2020 SAP SE. All rights reserved.
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

/**
 * @test
 * @bug 8227745
 * @summary Collection of test cases that check if optimizations based on escape analysis are reverted just before non-escaping objects escape through JVMTI.
 * @author Richard Reingruber richard DOT reingruber AT sap DOT com
 *
 * @requires ((vm.compMode == "Xmixed") & vm.compiler2.enabled)
 * @library /test/lib /test/hotspot/jtreg
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run compile -g EATests.java
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                 -XX:LockingMode=1
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -XX:-EliminateLocks -XX:+EliminateNestedLocks
 *                 -XX:LockingMode=1
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:+DoEscapeAnalysis -XX:-EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                 -XX:LockingMode=1
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:-DoEscapeAnalysis -XX:-EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                 -XX:LockingMode=1
 *
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                 -XX:LockingMode=2
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -XX:-EliminateLocks -XX:+EliminateNestedLocks
 *                 -XX:LockingMode=2
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:+DoEscapeAnalysis -XX:-EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                 -XX:LockingMode=2
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:-DoEscapeAnalysis -XX:-EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                 -XX:LockingMode=2
 *
 * @comment Excercise -XX:+DeoptimizeObjectsALot. Mostly to prevent bit-rot because the option is meant to stress object deoptimization
 *          with non-synthetic workloads.
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:-DoEscapeAnalysis -XX:-EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                 -XX:+IgnoreUnrecognizedVMOptions -XX:+DeoptimizeObjectsALot
 *
 * @bug 8324881
 * @comment Regression test for using the wrong thread when logging during re-locking from deoptimization.
 *
 * @comment DiagnoseSyncOnValueBasedClasses=2 will cause logging when locking on \@ValueBased objects.
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                 -XX:LockingMode=1
 *                 -XX:DiagnoseSyncOnValueBasedClasses=2
 *
 * @comment Re-lock may inflate monitors when re-locking, which cause monitorinflation trace logging.
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                 -XX:LockingMode=2
 *                 -Xlog:monitorinflation=trace:file=monitorinflation.log
 *
 * @comment Re-lock may race with deflation.
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                 -XX:LockingMode=0
 *                 -XX:GuaranteedAsyncDeflationInterval=1000
 */

/**
 * @test
 * @bug 8227745
 *
 * @summary This is another configuration of EATests.java to test Graal. Some testcases are expected
 *          to fail because Graal does not provide all information about non-escaping objects in
 *          scope. These are skipped.
 *
 * @author Richard Reingruber richard DOT reingruber AT sap DOT com
 *
 * @requires ((vm.compMode == "Xmixed") & vm.graal.enabled)
 *
 * @library /test/lib /test/hotspot/jtreg
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run compile -g EATests.java
 *
 * @comment Test with Graal. Some testcases are expected to fail because Graal does not provide all information about non-escaping
 *          objects in scope. These are skipped.
 * @run driver EATests
 *                 -XX:+UnlockDiagnosticVMOptions
 *                 -Xms256m -Xmx256m
 *                 -Xbootclasspath/a:.
 *                 -XX:CompileCommand=dontinline,*::dontinline_*
 *                 -XX:+WhiteBoxAPI
 *                 -Xbatch
 *                 -XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler
 */

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import compiler.testlibrary.CompilerUtils;
import compiler.whitebox.CompilerWhiteBoxTest;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jdk.test.lib.Asserts;
import jdk.test.whitebox.WhiteBox;
import jdk.test.whitebox.gc.GC;






class EATestCaseBaseShared {
    public static final boolean INTERACTIVE =
            System.getProperty("EATests.interactive") != null &&
            System.getProperty("EATests.interactive").equals("true");

    public static final String RUN_ONLY_TEST_CASE_PROPERTY = "EATests.onlytestcase";
    public static final String RUN_ONLY_TEST_CASE = System.getProperty(RUN_ONLY_TEST_CASE_PROPERTY);

    public final String testCaseName;

    public EATestCaseBaseShared() {
        String clName = getClass().getName();
        int tidx = clName.lastIndexOf("Target");
        testCaseName = tidx > 0 ? clName.substring(0, tidx) : clName;
    }

    public boolean shouldSkip() {
        return EATestCaseBaseShared.RUN_ONLY_TEST_CASE != null &&
               EATestCaseBaseShared.RUN_ONLY_TEST_CASE.length() > 0 &&
               !testCaseName.equals(EATestCaseBaseShared.RUN_ONLY_TEST_CASE);
    }
}


class EATestsTarget {

    public static void main(String[] args) {
        EATestCaseBaseTarget.staticSetUp();
        EATestCaseBaseTarget.staticSetUpDone();

        new EAMaterializeLocalVariableUponGetTarget()                                       .run();
        new EAGetWithoutMaterializeTarget()                                                 .run();
        new EAMaterializeLocalAtObjectReturnTarget()                                        .run();
        new EAMaterializeLocalAtObjectPollReturnReturnTarget()                              .run();
        new EAMaterializeIntArrayTarget()                                                   .run();
        new EAMaterializeLongArrayTarget()                                                  .run();
        new EAMaterializeFloatArrayTarget()                                                 .run();
        new EAMaterializeDoubleArrayTarget()                                                .run();
        new EAMaterializeObjectArrayTarget()                                                .run();
        new EAMaterializeObjectWithConstantAndNotConstantValuesTarget()                     .run();
        new EAMaterializeObjReferencedBy2LocalsTarget()                                     .run();
        new EAMaterializeObjReferencedBy2LocalsAndModifyTarget()                            .run();
        new EAMaterializeObjReferencedBy2LocalsInDifferentVirtFramesTarget()                .run();
        new EAMaterializeObjReferencedBy2LocalsInDifferentVirtFramesAndModifyTarget()       .run();
        new EAMaterializeObjReferencedFromOperandStackTarget()                              .run();
        new EAMaterializeLocalVariableUponGetAfterSetIntegerTarget()                        .run();

        new EARelockingSimpleTarget()                                                       .run();
        new EARelockingSimpleWithAccessInOtherThreadTarget()                                .run();
        new EARelockingRecursiveTarget()                                                    .run();
        new EARelockingNestedInflatedTarget()                                               .run();
        new EARelockingNestedInflated_02Target()                                            .run();
        new EARelockingNestedInflated_03Target()                                            .run();
        new EARelockingArgEscapeLWLockedInCalleeFrameTarget()                               .run();
        new EARelockingArgEscapeLWLockedInCalleeFrame_2Target()                             .run();
        new EARelockingArgEscapeLWLockedInCalleeFrameNoRecursiveTarget()                    .run();
        new EAGetOwnedMonitorsTarget()                                                      .run();
        new EAEntryCountTarget()                                                            .run();
        new EARelockingObjectCurrentlyWaitingOnTarget()                                     .run();
        new EARelockingValueBasedTarget()                                                   .run();

        new EADeoptFrameAfterReadLocalObject_01Target()                                     .run();
        new EADeoptFrameAfterReadLocalObject_01BTarget()                                    .run();
        new EADeoptFrameAfterReadLocalObject_02Target()                                     .run();
        new EADeoptFrameAfterReadLocalObject_02BTarget()                                    .run();
        new EADeoptFrameAfterReadLocalObject_02CTarget()                                    .run();
        new EADeoptFrameAfterReadLocalObject_03Target()                                     .run();

        new EAPopFrameNotInlinedTarget()                                                    .run();
        new EAPopFrameNotInlinedReallocFailureTarget()                                      .run();
        new EAPopInlinedMethodWithScalarReplacedObjectsReallocFailureTarget()               .run();

        new EAForceEarlyReturnNotInlinedTarget()                                            .run();
        new EAForceEarlyReturnOfInlinedMethodWithScalarReplacedObjectsTarget()              .run();
        new EAForceEarlyReturnOfInlinedMethodWithScalarReplacedObjectsReallocFailureTarget().run();

        new EAGetInstancesOfReferenceTypeTarget()                                           .run();
    }
}


public class EATests extends TestScaffold {

    public TargetVMOptions targetVMOptions;
    public ThreadReference targetMainThread;

    EATests(String args[]) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        if (EATestCaseBaseShared.RUN_ONLY_TEST_CASE != null) {
            args = Arrays.copyOf(args, args.length + 1);
            args[args.length - 1] = "-D" + EATestCaseBaseShared.RUN_ONLY_TEST_CASE_PROPERTY + "=" + EATestCaseBaseShared.RUN_ONLY_TEST_CASE;
        }
        new EATests(args).startTests();
    }

    public static class TargetVMOptions {

        public final boolean UseJVMCICompiler;
        public final boolean EliminateAllocations;
        public final boolean DeoptimizeObjectsALot;
        public final boolean DoEscapeAnalysis;
        public final boolean ZGCIsSelected;
        public final boolean ShenandoahGCIsSelected;
        public final boolean StressReflectiveCode;

        public TargetVMOptions(EATests env, ClassType testCaseBaseTargetClass) {
            Value val;
            val = testCaseBaseTargetClass.getValue(testCaseBaseTargetClass.fieldByName("DoEscapeAnalysis"));
            DoEscapeAnalysis = ((PrimitiveValue) val).booleanValue();
            val = testCaseBaseTargetClass.getValue(testCaseBaseTargetClass.fieldByName("EliminateAllocations"));
            EliminateAllocations = DoEscapeAnalysis && ((PrimitiveValue) val).booleanValue();
            val = testCaseBaseTargetClass.getValue(testCaseBaseTargetClass.fieldByName("DeoptimizeObjectsALot"));
            DeoptimizeObjectsALot = ((PrimitiveValue) val).booleanValue();
            val = testCaseBaseTargetClass.getValue(testCaseBaseTargetClass.fieldByName("UseJVMCICompiler"));
            UseJVMCICompiler = ((PrimitiveValue) val).booleanValue();
            val = testCaseBaseTargetClass.getValue(testCaseBaseTargetClass.fieldByName("ZGCIsSelected"));
            ZGCIsSelected = ((PrimitiveValue) val).booleanValue();
            val = testCaseBaseTargetClass.getValue(testCaseBaseTargetClass.fieldByName("ShenandoahGCIsSelected"));
            ShenandoahGCIsSelected = ((PrimitiveValue) val).booleanValue();
            val = testCaseBaseTargetClass.getValue(testCaseBaseTargetClass.fieldByName("StressReflectiveCode"));
            StressReflectiveCode = ((PrimitiveValue) val).booleanValue();
        }

    }

    protected void runTests() throws Exception {
        String targetProgName = EATestsTarget.class.getName();
        msg("starting to main method in class " +  targetProgName);
        startToMain(targetProgName);
        msg("resuming to EATestCaseBaseTarget.staticSetUpDone()V");
        targetMainThread = resumeTo("EATestCaseBaseTarget", "staticSetUpDone", "()V").thread();
        Location loc = targetMainThread.frame(0).location();
        Asserts.assertEQ("staticSetUpDone", loc.method().name());

        targetVMOptions = new TargetVMOptions(this, (ClassType) loc.declaringType());

        new EAMaterializeLocalVariableUponGet()                                       .run(this);
        new EAGetWithoutMaterialize()                                                 .run(this);
        new EAMaterializeLocalAtObjectReturn()                                        .run(this);
        new EAMaterializeLocalAtObjectPollReturnReturn()                              .run(this);
        new EAMaterializeIntArray()                                                   .run(this);
        new EAMaterializeLongArray()                                                  .run(this);
        new EAMaterializeFloatArray()                                                 .run(this);
        new EAMaterializeDoubleArray()                                                .run(this);
        new EAMaterializeObjectArray()                                                .run(this);
        new EAMaterializeObjectWithConstantAndNotConstantValues()                     .run(this);
        new EAMaterializeObjReferencedBy2Locals()                                     .run(this);
        new EAMaterializeObjReferencedBy2LocalsAndModify()                            .run(this);
        new EAMaterializeObjReferencedBy2LocalsInDifferentVirtFrames()                .run(this);
        new EAMaterializeObjReferencedBy2LocalsInDifferentVirtFramesAndModify()       .run(this);
        new EAMaterializeObjReferencedFromOperandStack()                              .run(this);
        new EAMaterializeLocalVariableUponGetAfterSetInteger()                        .run(this);

        new EARelockingSimple()                                                       .run(this);
        new EARelockingSimpleWithAccessInOtherThread()                                .run(this);
        new EARelockingRecursive()                                                    .run(this);
        new EARelockingNestedInflated()                                               .run(this);
        new EARelockingNestedInflated_02()                                            .run(this);
        new EARelockingNestedInflated_03()                                            .run(this);
        new EARelockingArgEscapeLWLockedInCalleeFrame()                               .run(this);
        new EARelockingArgEscapeLWLockedInCalleeFrame_2()                             .run(this);
        new EARelockingArgEscapeLWLockedInCalleeFrameNoRecursive()                    .run(this);
        new EAGetOwnedMonitors()                                                      .run(this);
        new EAEntryCount()                                                            .run(this);
        new EARelockingObjectCurrentlyWaitingOn()                                     .run(this);
        new EARelockingValueBased()                                                   .run(this);

        new EADeoptFrameAfterReadLocalObject_01()                                     .run(this);
        new EADeoptFrameAfterReadLocalObject_01B()                                    .run(this);
        new EADeoptFrameAfterReadLocalObject_02()                                     .run(this);
        new EADeoptFrameAfterReadLocalObject_02B()                                    .run(this);
        new EADeoptFrameAfterReadLocalObject_02C()                                    .run(this);
        new EADeoptFrameAfterReadLocalObject_03()                                     .run(this);

        new EAPopFrameNotInlined()                                                    .run(this);
        new EAPopFrameNotInlinedReallocFailure()                                      .run(this);
        new EAPopInlinedMethodWithScalarReplacedObjectsReallocFailure()               .run(this);

        new EAForceEarlyReturnNotInlined()                                            .run(this);
        new EAForceEarlyReturnOfInlinedMethodWithScalarReplacedObjects()              .run(this);
        new EAForceEarlyReturnOfInlinedMethodWithScalarReplacedObjectsReallocFailure().run(this);

        new EAGetInstancesOfReferenceType()                                           .run(this);

        listenUntilVMDisconnect();
    }

    public void msg(String m) {
        System.out.println();
        System.out.println("###(Debugger) " + m);
        System.out.println();
    }

    public void msgHL(String m) {
        System.out.println();
        System.out.println();
        System.out.println("##########################################################");
        System.out.println("### " + m);
        System.out.println("### ");
        System.out.println();
        System.out.println();
    }
}


abstract class EATestCaseBaseDebugger  extends EATestCaseBaseShared {

    protected EATests env;

    public ObjectReference testCase;

    public static final String TARGET_TESTCASE_BASE_NAME = EATestCaseBaseTarget.class.getName();

    public static final String XYVAL_NAME = XYVal.class.getName();

    public abstract void runTestCase() throws Exception;

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() || env.targetVMOptions.StressReflectiveCode;
    }

    public void run(EATests env) {
        this.env = env;
        if (shouldSkip()) {
            msg("skipping " + testCaseName);
            return;
        }
        try {
            msgHL("Executing test case " + getClass().getName());
            env.testFailed = false;

            if (INTERACTIVE)
                env.waitForInput();

            resumeToWarmupDone();
            runTestCase();
            Asserts.assertTrue(env.targetMainThread.isSuspended(), "must be suspended after the testcase");
            resumeToTestCaseDone();
            checkPostConditions();
        } catch (Exception e) {
            Asserts.fail("Unexpected exception in test case " + getClass().getName(), e);
        }
    }

    /**
     * Set a breakpoint in the given method and resume all threads. The
     * breakpoint is configured to suspend just the thread that reaches it
     * instead of all threads. This is important when running with graal.
     */
    public BreakpointEvent resumeTo(String clsName, String methodName, String signature) {
        boolean suspendThreadOnly = true;
        return env.resumeTo(clsName, methodName, signature, suspendThreadOnly);
    }

    public void resumeToWarmupDone() throws Exception {
        msg("resuming to " + TARGET_TESTCASE_BASE_NAME + ".warmupDone()V");
        resumeTo(TARGET_TESTCASE_BASE_NAME, "warmupDone", "()V");
        testCase = env.targetMainThread.frame(0).thisObject();
    }

    public void resumeToTestCaseDone() {
        msg("resuming to " + TARGET_TESTCASE_BASE_NAME + ".testCaseDone()V");
        resumeTo(TARGET_TESTCASE_BASE_NAME, "testCaseDone", "()V");
    }

    public void checkPostConditions() throws Exception {
        Asserts.assertFalse(env.getExceptionCaught(), "Uncaught exception in Debuggee");

        String testName = getClass().getName();
        if (!env.testFailed) {
            env.println(testName  + ": passed");
        } else {
            throw new Exception(testName + ": failed");
        }
    }

    public void printStack(ThreadReference thread) throws Exception {
        msg("Debuggee Stack:");
        List<StackFrame> stack_frames = thread.frames();
        int i = 0;
        for (StackFrame ff : stack_frames) {
            System.out.println("frame[" + i++ +"]: " + ff.location().method() + " (bci:" + ff.location().codeIndex() + ")");
        }
    }

    public void msg(String m) {
        env.msg(m);
    }

    public void msgHL(String m) {
        env.msgHL(m);
    }

    enum FD {
        I, 
        J, 
        F, 
        D, 
    }

    public static final Map<FD, String> FD2JDIArrType = Map.of(FD.I, "int[]", FD.J, "long[]", FD.F, "float[]", FD.D, "double[]");

    public static final Function<PrimitiveValue, Integer> v2I = PrimitiveValue::intValue;
    public static final Function<PrimitiveValue, Long>    v2J = PrimitiveValue::longValue;
    public static final Function<PrimitiveValue, Float>   v2F = PrimitiveValue::floatValue;
    public static final Function<PrimitiveValue, Double>  v2D = PrimitiveValue::doubleValue;
    Map<FD, Function<PrimitiveValue, ?>> FD2getter = Map.of(FD.I, v2I, FD.J, v2J, FD.F, v2F, FD.D, v2D);

    /**
     * Retrieve array of primitive values referenced by a local variable in target and compare with
     * an array of expected values.
     * @param frame Frame in the target holding the local variable
     * @param lName Name of the local variable referencing the array to be retrieved
     * @param desc Array element type given as field descriptor.
     * @param expVals Array of expected values.
     * @throws Exception
     */
    protected void checkLocalPrimitiveArray(StackFrame frame, String lName, FD desc, Object expVals) throws Exception {
        String lType = FD2JDIArrType.get(desc);
        Asserts.assertNotNull(lType, "jdi type not found");
        Asserts.assertEQ(EATestCaseBaseTarget.TESTMETHOD_DEFAULT_NAME, frame .location().method().name());
        List<LocalVariable> localVars = frame.visibleVariables();
        msg("Check if the local array variable '" + lName  + "' in " + EATestCaseBaseTarget.TESTMETHOD_DEFAULT_NAME + " has the expected elements: ");
        boolean found = false;
        for (LocalVariable lv : localVars) {
            if (lv.name().equals(lName)) {
                found  = true;
                Value lVal = frame.getValue(lv);
                Asserts.assertNotNull(lVal);
                Asserts.assertEQ(lVal.type().name(), lType);
                ArrayReference aRef = (ArrayReference) lVal;
                Asserts.assertEQ(3, aRef.length());
                for (int i = 0; i < aRef.length(); i++) {
                    Object actVal = FD2getter.get(desc).apply((PrimitiveValue)aRef.getValue(i));
                    Object expVal = Array.get(expVals, i);
                    Asserts.assertEQ(expVal, actVal, "checking element at index " + i);
                }
            }
        }
        Asserts.assertTrue(found);
        msg("OK.");
    }

    /**
     * Retrieve array of objects referenced by a local variable in target and compare with an array
     * of expected values.
     * @param frame Frame in the target holding the local variable
     * @param lName Name of the local variable referencing the array to be retrieved
     * @param lType Local type, e.g. java.lang.Long[]
     * @param expVals Array of expected values.
     * @throws Exception
     */
    protected void checkLocalObjectArray(StackFrame frame, String lName, String lType, ObjectReference[] expVals) throws Exception {
        Asserts.assertEQ(EATestCaseBaseTarget.TESTMETHOD_DEFAULT_NAME, frame .location().method().name());
        List<LocalVariable> localVars = frame.visibleVariables();
        msg("Check if the local array variable '" + lName  + "' in " + EATestCaseBaseTarget.TESTMETHOD_DEFAULT_NAME + " has the expected elements: ");
        boolean found = false;
        for (LocalVariable lv : localVars) {
            if (lv.name().equals(lName)) {
                found  = true;
                Value lVal = frame.getValue(lv);
                Asserts.assertNotNull(lVal);
                Asserts.assertEQ(lType, lVal.type().name());
                ArrayReference aRef = (ArrayReference) lVal;
                Asserts.assertEQ(3, aRef.length());
                for (int i = 0; i < aRef.length(); i++) {
                    ObjectReference actVal = (ObjectReference)aRef.getValue(i);
                    Asserts.assertSame(expVals[i], actVal, "checking element at index " + i);
                }
            }
        }
        Asserts.assertTrue(found);
        msg("OK.");
    }

    /**
     * Retrieve a reference held by a local variable in the given frame. Check if the frame's method
     * is the expected method if the retrieved local value has the expected type and is not null.
     * @param frame The frame to retrieve the local variable value from.
     * @param expectedMethodName The name of the frames method should match the expectedMethodName.
     * @param lName The name of the local variable which is read.
     * @param expectedType Is the expected type of the object referenced by the local variable.
     * @return
     * @throws Exception
     */
    protected ObjectReference getLocalRef(StackFrame frame, String expectedMethodName, String lName, String expectedType) throws Exception {
        Asserts.assertEQ(expectedMethodName, frame.location().method().name());
        List<LocalVariable> localVars = frame.visibleVariables();
        msg("Get and check local variable '" + lName + "' in " + expectedMethodName);
        ObjectReference lRef = null;
        for (LocalVariable lv : localVars) {
            if (lv.name().equals(lName)) {
                Value lVal = frame.getValue(lv);
                Asserts.assertNotNull(lVal);
                Asserts.assertEQ(expectedType, lVal.type().name());
                lRef = (ObjectReference) lVal;
                break;
            }
        }
        Asserts.assertNotNull(lRef, "Local variable '" + lName + "' not found");
        msg("OK.");
        return lRef;
    }

    /**
     * Retrieve a reference held by a local variable in the given frame. Check if the frame's method
     * matches {@link EATestCaseBaseTarget#TESTMETHOD_DEFAULT_NAME} if the retrieved local value has
     * the expected type and is not null.
     * @param frame The frame to retrieve the local variable value from.
     * @param expectedMethodName The name of the frames method should match the expectedMethodName.
     * @param lName The name of the local variable which is read.
     * @param expectedType Is the expected type of the object referenced by the local variable.
     * @return
     * @throws Exception
     */
    protected ObjectReference getLocalRef(StackFrame frame, String lType, String lName) throws Exception {
        return getLocalRef(frame, EATestCaseBaseTarget.TESTMETHOD_DEFAULT_NAME, lName, lType);
    }

    /**
     * Set the value of a local variable in the given frame. Check if the frame's method is the expected method.
     * @param frame The frame holding the local variable.
     * @param expectedMethodName The expected name of the frame's method.
     * @param lName The name of the local variable to change.
     * @param val The new value of the local variable.
     * @throws Exception
     */
    public void setLocal(StackFrame frame, String expectedMethodName, String lName, Value val) throws Exception {
        Asserts.assertEQ(expectedMethodName, frame.location().method().name());
        List<LocalVariable> localVars = frame.visibleVariables();
        msg("Set local variable '" + lName + "' = " + val + " in " + expectedMethodName);
        for (LocalVariable lv : localVars) {
            if (lv.name().equals(lName)) {
                frame.setValue(lv, val);
                break;
            }
        }
        msg("OK.");
    }

    /**
     * Set the value of a local variable in the given frame. Check if the frame's method matches
     * {@link EATestCaseBaseTarget#TESTMETHOD_DEFAULT_NAME}.
     * @param frame The frame holding the local variable.
     * @param expectedMethodName The expected name of the frame's method.
     * @param lName The name of the local variable to change.
     * @param val The new value of the local variable.
     * @throws Exception
     */
    public void setLocal(StackFrame frame, String lName, Value val) throws Exception {
        setLocal(frame, EATestCaseBaseTarget.TESTMETHOD_DEFAULT_NAME, lName, val);
    }

    /**
     * Check if a field has the expected primitive value.
     * @param o Object holding the field.
     * @param desc Field descriptor.
     * @param fName Field name
     * @param expVal Expected primitive value
     * @throws Exception
     */
    protected void checkPrimitiveField(ObjectReference o, FD desc, String fName, Object expVal) throws Exception {
        msg("check field " + fName);
        ReferenceType rt = o.referenceType();
        Field fld = rt.fieldByName(fName);
        Value val = o.getValue(fld);
        Object actVal = FD2getter.get(desc).apply((PrimitiveValue) val);
        Asserts.assertEQ(expVal, actVal, "field '" + fName + "' has unexpected value.");
        msg("ok");
    }

    /**
     * Check if a field references the expected object.
     * @param obj Object holding the field.
     * @param fName Field name
     * @param expVal Object expected to be referenced by the field
     * @throws Exception
     */
    protected void checkObjField(ObjectReference obj, String fName, ObjectReference expVal) throws Exception {
        msg("check field " + fName);
        ReferenceType rt = obj.referenceType();
        Field fld = rt.fieldByName(fName);
        Value actVal = obj.getValue(fld);
        Asserts.assertEQ(expVal, actVal, "field '" + fName + "' has unexpected value.");
        msg("ok");
    }

    protected void setField(ObjectReference obj, String fName, Value val) throws Exception {
        msg("set field " + fName + " = " + val);
        ReferenceType rt = obj.referenceType();
        Field fld = rt.fieldByName(fName);
        obj.setValue(fld, val);
        msg("ok");
    }

    protected Value getField(ObjectReference obj, String fName) throws Exception {
        msg("get field " + fName);
        ReferenceType rt = obj.referenceType();
        Field fld = rt.fieldByName(fName);
        Value val = obj.getValue(fld);
        msg("result : " + val);
        return val;
    }

    /**
     * Free the memory consumed in the target by {@link EATestCaseBaseTarget#consumedMemory}
     * @throws Exception
     */
    public void freeAllMemory() throws Exception {
        msg("free consumed memory");
        setField(testCase, "consumedMemory", null);
    }

    /**
     * @return The value of {@link EATestCaseBaseTarget#targetIsInLoop}. The target must set that field to true as soon as it
     *         enters the endless loop.
     * @throws Exception
     */
    public boolean targetHasEnteredEndlessLoop() throws Exception {
        Value v = getField(testCase, "targetIsInLoop");
        return ((PrimitiveValue) v).booleanValue();
    }

    /**
     * Poll {@link EATestCaseBaseTarget#targetIsInLoop} and return if it is found to be true.
     * @throws Exception
     */
    public void waitUntilTargetHasEnteredEndlessLoop() throws Exception {
        while(!targetHasEnteredEndlessLoop()) {
            msg("Target has not yet entered the loop. Sleep 200ms.");
            try { Thread.sleep(200); } catch (InterruptedException e) { /*ignore */ }
        }
    }

    /**
     * Set {@link EATestCaseBaseTarget#doLoop} to <code>false</code>. This will allow the target to
     * leave the endless loop.
     * @throws Exception
     */
    public void terminateEndlessLoop() throws Exception {
        msg("terminate loop");
        setField(testCase, "doLoop", env.vm().mirrorOf(false));
    }
}


abstract class EATestCaseBaseTarget extends EATestCaseBaseShared implements Runnable {

    /**
     * The target must set that field to true as soon as it enters the endless loop.
     */
    public volatile boolean targetIsInLoop;

    /**
     * Used for busy loops. See {@link #dontinline_endlessLoop()}.
     */
    public volatile long loopCount;

    /**
     * Used in {@link EATestCaseBaseDebugger#terminateEndlessLoop()} to signal target to leave the endless loop.
     */
    public volatile boolean doLoop;

    public long checkSum;

    public static final String TESTMETHOD_DEFAULT_NAME = "dontinline_testMethod";

    public static final WhiteBox WB = WhiteBox.getWhiteBox();

    public static boolean unbox(Boolean value, boolean dflt) {
        return value == null ? dflt : value;
    }

    public static final boolean UseJVMCICompiler = unbox(WB.getBooleanVMFlag("UseJVMCICompiler"), false);
    public static final boolean DoEscapeAnalysis = unbox(WB.getBooleanVMFlag("DoEscapeAnalysis"), UseJVMCICompiler);
    public static final boolean EliminateAllocations = unbox(WB.getBooleanVMFlag("EliminateAllocations"), UseJVMCICompiler);
    public static final boolean DeoptimizeObjectsALot = WB.getBooleanVMFlag("DeoptimizeObjectsALot");
    public static final boolean ZGCIsSelected = GC.Z.isSelected();
    public static final boolean ShenandoahGCIsSelected = GC.Shenandoah.isSelected();
    public static final boolean StressReflectiveCode = unbox(WB.getBooleanVMFlag("StressReflectiveCode"), false);

    public String testMethodName;
    public int testMethodDepth;

    public int  iResult;
    public long lResult;
    public float  fResult;
    public double dResult;


    public boolean warmupDone;

    public static XYVal inflatedLock;
    public static Thread  inflatorThread;
    public static boolean inflatedLockIsPermanentlyInflated;

    public static int    NOT_CONST_1I = 1;
    public static long   NOT_CONST_1L = 1L;
    public static float  NOT_CONST_1F = 1.1F;
    public static double NOT_CONST_1D = 1.1D;

    public static          Long NOT_CONST_1_OBJ = Long.valueOf(1);


    public static final    Long CONST_2_OBJ     = Long.valueOf(2);
    public static final    Long CONST_3_OBJ     = Long.valueOf(3);

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() || StressReflectiveCode;
    }

    /**
     * Main driver of a test case.
     * <ul>
     * <li> Skips test case if not selected (see {@link EATestCaseBaseShared#RUN_ONLY_TEST_CASE}
     * <li> Call {@link #setUp()}
     * <li> warm-up and compile {@link #dontinline_testMethod()} (see {@link #compileTestMethod()}
     * <li> calling {@link #dontinline_testMethod()}
     * <li> checking the result (see {@link #checkResult()}
     * <ul>
     */
    public void run() {
        try {
            if (shouldSkip()) {
                msg("skipping " + testCaseName);
                return;
            }
            setUp();
            msg(testCaseName + " is up and running.");
            compileTestMethod();
            msg(testCaseName + " warmup done.");
            warmupDone();
            checkCompLevel();
            dontinline_testMethod();
            checkResult();
            msg(testCaseName + " done.");
            testCaseDone();
        } catch (Exception e) {
            Asserts.fail("Caught unexpected exception", e);
        }
    }

    public static void staticSetUp() {
        inflatedLock = new XYVal(1, 1);
        synchronized (inflatedLock) {
            inflatorThread = DebuggeeWrapper.newThread(() -> {
                synchronized (inflatedLock) {
                    inflatedLockIsPermanentlyInflated = true;
                    inflatedLock.notify(); 
                    while (true) {
                        try {
                            inflatedLock.wait();
                        } catch (InterruptedException e) { /* ignored */ }
                    }
                }
                }, "Lock Inflator (test thread)");
            inflatorThread.setDaemon(true);
            inflatorThread.start();

            while(!inflatedLockIsPermanentlyInflated) {
                try {
                    inflatedLock.wait(); 
                } catch (InterruptedException e1) { /* ignored */ }
            }
        }
    }

    public static void staticSetUpDone() {
    }

    public void setUp() {
        testMethodDepth = 1;
        testMethodName = TESTMETHOD_DEFAULT_NAME;
    }

    public abstract void dontinline_testMethod() throws Exception;

    public int dontinline_brkpt_iret() {
        dontinline_brkpt();
        return 42;
    }

    /**
     * It is a common protocol to have the debugger set a breakpoint in this method and have {@link
     * #dontinline_testMethod()} call it and then perform some test actions on debugger side.
     * After that it is checked if a frame of {@link #dontinline_testMethod()} is found at the
     * expected depth on stack and if it is (not) marked for deoptimization as expected.
     */
    public void dontinline_brkpt() {
        if (warmupDone) {
            StackTraceElement[] frames = Thread.currentThread().getStackTrace();
            int stackTraceDepth = testMethodDepth + 1; 
            Asserts.assertEQ(testMethodName, frames[stackTraceDepth].getMethodName(),
                    testCaseName + ": test method not found at depth " + testMethodDepth);
            if (!DeoptimizeObjectsALot) {
                if (testFrameShouldBeDeoptimized()) {
                    Asserts.assertTrue(WB.isFrameDeoptimized(testMethodDepth+1),
                            testCaseName + ": expected test method frame at depth " + testMethodDepth + " to be deoptimized");
                } else {
                    Asserts.assertFalse(WB.isFrameDeoptimized(testMethodDepth+1),
                            testCaseName + ": expected test method frame at depth " + testMethodDepth + " not to be deoptimized");
                }
            }
        }
    }

    /**
     * Some test cases run busy endless loops by initializing {@link #loopCount}
     * to {@link Long#MAX_VALUE} after warm-up and then counting down to 0 in their main test method.
     * During warm-up {@link #loopCount} is initialized to a small value.
     */
    public long dontinline_endlessLoop() {
        long cs = checkSum;
        doLoop = true;
        while (loopCount-- > 0 && doLoop) {
            targetIsInLoop = true;
            checkSum += checkSum % ++cs;
        }
        loopCount = 3;
        targetIsInLoop = false;
        return checkSum;
    }

    public boolean testFrameShouldBeDeoptimized() {
        return DoEscapeAnalysis;
    }

    public void warmupDone() {
        warmupDone = true;
    }

    public void testCaseDone() {
    }

    public void compileTestMethod() throws Exception {
        int callCount = CompilerWhiteBoxTest.THRESHOLD;
        while (callCount-- > 0) {
            dontinline_testMethod();
        }
    }

    public void checkCompLevel() {
        java.lang.reflect.Method m = null;
        try {
            m = getClass().getMethod(TESTMETHOD_DEFAULT_NAME);
        } catch (NoSuchMethodException | SecurityException e) {
            Asserts.fail("could not check compilation level of", e);
        }
        int highestLevel = CompilerUtils.getMaxCompilationLevel();
        int compLevel = WB.getMethodCompilationLevel(m);
        if (!UseJVMCICompiler) {
            Asserts.assertEQ(highestLevel, compLevel,
                             m + " not on expected compilation level");
        } else {
            while (compLevel != highestLevel) {
                msg(TESTMETHOD_DEFAULT_NAME + " is compiled on level " + compLevel +
                    ". Wait until highes level (" + highestLevel + ") is reached.");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) { /* ignored */ }
                compLevel = WB.getMethodCompilationLevel(m);
            }
        }
    }

    public int getExpectedIResult() {
        return 0;
    }

    public long getExpectedLResult() {
        return 0;
    }

    public float getExpectedFResult() {
        return 0f;
    }

    public double getExpectedDResult() {
        return 0d;
    }

    private void checkResult() {
        Asserts.assertEQ(getExpectedIResult(), iResult, "checking iResult");
        Asserts.assertEQ(getExpectedLResult(), lResult, "checking lResult");
        Asserts.assertEQ(getExpectedFResult(), fResult, "checking fResult");
        Asserts.assertEQ(getExpectedDResult(), dResult, "checking dResult");
    }

    public void msg(String m) {
        System.out.println();
        System.out.println("###(Target) " + m);
        System.out.println();
    }

    public final void dontinline_make_arg_escape(XYVal xy) {
    }

    /**
     * Call a method indirectly using reflection. The indirection is a limit for escape
     * analysis in the sense that the VM need not search beyond for frames that might have
     * an object being read by an JVMTI agent as ArgEscape.
     * @param receiver The receiver object of the call.
     * @param methodName The name of the method to be called.
     */
    public final void dontinline_call_with_entry_frame(Object receiver, String methodName) {
        Asserts.assertTrue(warmupDone, "We want to take the slow path through jni, so don't call in warmup");

        Class<?> cls = receiver.getClass();
        Class<?>[] none = {};

        java.lang.reflect.Method m;
        try {
            m = cls.getDeclaredMethod(methodName, none);
            m.invoke(receiver);
        } catch (Exception e) {
            Asserts.fail("Call through reflection failed", e);
        }
    }

    static class LinkedList {
        LinkedList l;
        public long[] array;
        public LinkedList(LinkedList l, int size) {
            this.array = size > 0 ? new long[size] : null;
            this.l = l;
        }
    }

    public LinkedList consumedMemory;

    public void consumeAllMemory() {
        msg("consume all memory");
        int size = 128 * 1024 * 1024;
        while(true) {
            try {
                while(true) {
                    consumedMemory = new LinkedList(consumedMemory, size);
                }
            } catch(OutOfMemoryError oom) {
                if (size == 0) break;
            }
            size = size / 2;
        }
    }
}


class EAGetWithoutMaterializeTarget extends EATestCaseBaseTarget {

    public XYVal getAway;

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(4, 2);
        getAway = xy;  
        dontinline_brkpt();
        iResult = xy.x + xy.y;
    }

    @Override
    public int getExpectedIResult() {
        return 4 + 2;
    }

    @Override
    public boolean testFrameShouldBeDeoptimized() {
        return false;
    }
}

class EAGetWithoutMaterialize extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        ObjectReference o = getLocalRef(bpe.thread().frame(1), XYVAL_NAME, "xy");
        checkPrimitiveField(o, FD.I, "x", 4);
        checkPrimitiveField(o, FD.I, "y", 2);
    }
}


class EAMaterializeLocalVariableUponGet extends EATestCaseBaseDebugger {

    private ObjectReference o;

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        o = getLocalRef(bpe.thread().frame(1), XYVAL_NAME, "xy");
        o.disableCollection();
        checkPrimitiveField(o, FD.I, "x", 4);
        checkPrimitiveField(o, FD.I, "y", 2);
    }

    @Override
    public void checkPostConditions() throws Exception {
        super.checkPostConditions();
        checkPrimitiveField(o, FD.I, "x", 5);
    }
}

class EAMaterializeLocalVariableUponGetTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(4, 2);
        dontinline_brkpt();       
        xy.x += 1;                
        iResult = xy.x + xy.y;
    }

    @Override
    public int getExpectedIResult() {
        return 4 + 2 + 1;
    }
}


class EAMaterializeLocalAtObjectReturn extends EATestCaseBaseDebugger {
    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        ObjectReference o = getLocalRef(bpe.thread().frame(2), XYVAL_NAME, "xy");
        checkPrimitiveField(o, FD.I, "x", 4);
        checkPrimitiveField(o, FD.I, "y", 2);
    }
}

class EAMaterializeLocalAtObjectReturnTarget extends EATestCaseBaseTarget {
    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(4, 2);
        Integer io =                 
                dontinline_brkpt_return_Integer();
        iResult = xy.x + xy.y + io;
    }

    public Integer dontinline_brkpt_return_Integer() {
        dontinline_brkpt();
        return Integer.valueOf(23);
    }

    @Override
    public int getExpectedIResult() {
        return 4 + 2 + 23;
    }
}


class EAMaterializeLocalAtObjectPollReturnReturn extends EATestCaseBaseDebugger {
    public void runTestCase() throws Exception {
        msg("Resume " + env.targetMainThread);
        env.vm().resume();
        waitUntilTargetHasEnteredEndlessLoop();
        ObjectReference o = null;
        int retryCount = 0;
        do {
            env.targetMainThread.suspend();
            printStack(env.targetMainThread);
            try {
                o = getLocalRef(env.targetMainThread.frame(0), XYVAL_NAME, "xy");
            } catch (Exception e) {
                ++retryCount;
                msg("The local variable xy is out of scope because we suspended at the wrong bci. Resume and try again! (" + retryCount + ")");
                env.targetMainThread.resume();
                if ((retryCount % 10) == 0) {
                    Thread.sleep(200);
                }
            }
        } while (o == null);
        checkPrimitiveField(o, FD.I, "x", 4);
        checkPrimitiveField(o, FD.I, "y", 2);
        terminateEndlessLoop();
    }
}

class EAMaterializeLocalAtObjectPollReturnReturnTarget extends EATestCaseBaseTarget {
    @Override
    public void setUp() {
        super.setUp();
        loopCount = 3;
        doLoop = true;
    }

    public void warmupDone() {
        super.warmupDone();
        msg("enter 'endless' loop by setting loopCount = Long.MAX_VALUE");
        loopCount = Long.MAX_VALUE; 
    }

    public void dontinline_testMethod() {
        long result = 0;
        while (doLoop && loopCount-- > 0) {
            targetIsInLoop = true;
            XYVal xy = new XYVal(4, 2);
            Integer io =           
                    dontinline_brkpt_return_Integer();
            result += xy.x + xy.y + io;
        }  
        targetIsInLoop = false;
        lResult = result;
    }

    public Integer dontinline_brkpt_return_Integer() {
        return Integer.valueOf(23);
    }

    @Override
    public long getExpectedLResult() {
        return (Long.MAX_VALUE - loopCount) * (4+2+23);
    }
}


class EAMaterializeIntArrayTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        int nums[] = {NOT_CONST_1I , 2, 3};
        dontinline_brkpt();
        iResult = nums[0] + nums[1] + nums[2];
    }

    @Override
    public int getExpectedIResult() {
        return NOT_CONST_1I + 2 + 3;
    }
}

class EAMaterializeIntArray extends EATestCaseBaseDebugger {
    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        int[] expectedVals = {1, 2, 3};
        checkLocalPrimitiveArray(bpe.thread().frame(1), "nums", FD.I, expectedVals);
    }
}


class EAMaterializeLongArrayTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        long nums[] = {NOT_CONST_1L , 2, 3};
        dontinline_brkpt();
        lResult = nums[0] + nums[1] + nums[2];
    }

    @Override
    public long getExpectedLResult() {
        return NOT_CONST_1L + 2 + 3;
    }
}

class EAMaterializeLongArray extends EATestCaseBaseDebugger {
    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        long[] expectedVals = {1, 2, 3};
        checkLocalPrimitiveArray(bpe.thread().frame(1), "nums", FD.J, expectedVals);
    }
}


class EAMaterializeFloatArrayTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        float nums[] = {NOT_CONST_1F , 2.2f, 3.3f};
        dontinline_brkpt();
        fResult = nums[0] + nums[1] + nums[2];
    }

    @Override
    public float getExpectedFResult() {
        return NOT_CONST_1F + 2.2f + 3.3f;
    }
}

class EAMaterializeFloatArray extends EATestCaseBaseDebugger {
    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        float[] expectedVals = {1.1f, 2.2f, 3.3f};
        checkLocalPrimitiveArray(bpe.thread().frame(1), "nums", FD.F, expectedVals);
    }
}


class EAMaterializeDoubleArrayTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        double nums[] = {NOT_CONST_1D , 2.2d, 3.3d};
        dontinline_brkpt();
        dResult = nums[0] + nums[1] + nums[2];
    }

    @Override
    public double getExpectedDResult() {
        return NOT_CONST_1D + 2.2d + 3.3d;
    }
}

class EAMaterializeDoubleArray extends EATestCaseBaseDebugger {
    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        double[] expectedVals = {1.1d, 2.2d, 3.3d};
        checkLocalPrimitiveArray(bpe.thread().frame(1), "nums", FD.D, expectedVals);
    }
}


class EAMaterializeObjectArrayTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        Long nums[] = {NOT_CONST_1_OBJ , CONST_2_OBJ, CONST_3_OBJ};
        dontinline_brkpt();
        lResult = nums[0] + nums[1] + nums[2];
    }

    @Override
    public long getExpectedLResult() {
        return 1 + 2 + 3;
    }
}

class EAMaterializeObjectArray extends EATestCaseBaseDebugger {
    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        ReferenceType clazz = bpe.thread().frame(0).location().declaringType();
        ObjectReference[] expectedVals = {
                (ObjectReference) clazz.getValue(clazz.fieldByName("NOT_CONST_1_OBJ")),
                (ObjectReference) clazz.getValue(clazz.fieldByName("CONST_2_OBJ")),
                (ObjectReference) clazz.getValue(clazz.fieldByName("CONST_3_OBJ"))
        };
        checkLocalObjectArray(bpe.thread().frame(1), "nums", "java.lang.Long[]", expectedVals);
    }
}


class EAMaterializeObjectWithConstantAndNotConstantValuesTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        ILFDO o = new ILFDO(NOT_CONST_1I, 2,
                            NOT_CONST_1L, 2L,
                            NOT_CONST_1F, 2.1F,
                            NOT_CONST_1D, 2.1D,
                            NOT_CONST_1_OBJ, CONST_2_OBJ
                            );
        dontinline_brkpt();
        dResult =
            o.i + o.i2 + o.l + o.l2 + o.f + o.f2 + o.d + o.d2 + o.o + o.o2;
    }

    @Override
    public double getExpectedDResult() {
        return NOT_CONST_1I + 2 + NOT_CONST_1L + 2L + NOT_CONST_1F + 2.1F + NOT_CONST_1D + 2.1D + NOT_CONST_1_OBJ + CONST_2_OBJ;
    }
}

class EAMaterializeObjectWithConstantAndNotConstantValues extends EATestCaseBaseDebugger {
    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        ObjectReference o = getLocalRef(bpe.thread().frame(1), "ILFDO", "o");
        checkPrimitiveField(o, FD.I, "i", 1);
        checkPrimitiveField(o, FD.I, "i2", 2);
        checkPrimitiveField(o, FD.J, "l", 1L);
        checkPrimitiveField(o, FD.J, "l2", 2L);
        checkPrimitiveField(o, FD.F, "f", 1.1f);
        checkPrimitiveField(o, FD.F, "f2", 2.1f);
        checkPrimitiveField(o, FD.D, "d", 1.1d);
        checkPrimitiveField(o, FD.D, "d2", 2.1d);
        ReferenceType clazz = bpe.thread().frame(1).location().declaringType();
        ObjectReference[] expVals = {
                (ObjectReference) clazz.getValue(clazz.fieldByName("NOT_CONST_1_OBJ")),
                (ObjectReference) clazz.getValue(clazz.fieldByName("CONST_2_OBJ")),
        };
        checkObjField(o, "o", expVals[0]);
        checkObjField(o, "o2", expVals[1]);
    }
}


class EAMaterializeObjReferencedBy2LocalsTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(2, 3);
        XYVal alias = xy;
        dontinline_brkpt();
        iResult = xy.x + alias.x;
    }

    @Override
    public int getExpectedIResult() {
        return 2 + 2;
    }
}

class EAMaterializeObjReferencedBy2Locals extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        ObjectReference xy = getLocalRef(bpe.thread().frame(1), XYVAL_NAME, "xy");
        ObjectReference alias = getLocalRef(bpe.thread().frame(1), XYVAL_NAME, "alias");
        Asserts.assertSame(xy, alias, "xy and alias are expected to reference the same object");
    }

}


class EAMaterializeObjReferencedBy2LocalsAndModifyTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(2, 3);
        XYVal alias = xy;
        dontinline_brkpt(); 
        iResult = xy.x + alias.x;
    }

    @Override
    public int getExpectedIResult() {
        return 42 + 42;
    }
}

class EAMaterializeObjReferencedBy2LocalsAndModify extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        ObjectReference alias = getLocalRef(bpe.thread().frame(1), XYVAL_NAME, "alias");
        setField(alias, "x", env.vm().mirrorOf(42));
    }
}


class EAMaterializeObjReferencedBy2LocalsInDifferentVirtFramesTarget extends EATestCaseBaseTarget {

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(2, 3);
        testMethod_inlined(xy);
        iResult += xy.x;
    }

    public void testMethod_inlined(XYVal xy) {
        XYVal alias = xy;
        dontinline_brkpt();
        iResult = alias.x;
    }

    @Override
    public int getExpectedIResult() {
        return 2 + 2;
    }
}

class EAMaterializeObjReferencedBy2LocalsInDifferentVirtFrames extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        ObjectReference xy = getLocalRef(bpe.thread().frame(2), XYVAL_NAME, "xy");
        ObjectReference alias = getLocalRef(bpe.thread().frame(1), "testMethod_inlined", "alias", XYVAL_NAME);
        Asserts.assertSame(xy, alias, "xy and alias are expected to reference the same object");
    }

}


class EAMaterializeObjReferencedBy2LocalsInDifferentVirtFramesAndModifyTarget extends EATestCaseBaseTarget {

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(2, 3);
        testMethod_inlined(xy);   
        iResult += xy.x;
    }

    public void testMethod_inlined(XYVal xy) {
        XYVal alias = xy;
        dontinline_brkpt();
        iResult = alias.x;
    }

    @Override
    public int getExpectedIResult() {
        return 42 + 42;
    }
}

class EAMaterializeObjReferencedBy2LocalsInDifferentVirtFramesAndModify extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        ObjectReference alias = getLocalRef(bpe.thread().frame(1), "testMethod_inlined", "alias", XYVAL_NAME);
        setField(alias, "x", env.vm().mirrorOf(42));
    }

}


class EAMaterializeObjReferencedFromOperandStackTarget extends EATestCaseBaseTarget {

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    public void dontinline_testMethod() {
        @SuppressWarnings("unused")
        XYVal xy1 = new XYVal(2, 3);
        iResult = testMethodInlined(new XYVal(4, 2), dontinline_brkpt_ret_100());
    }

    public int testMethodInlined(XYVal xy2, int dontinline_brkpt_ret_100) {
        return xy2.x + dontinline_brkpt_ret_100;
    }

    public int dontinline_brkpt_ret_100() {
        dontinline_brkpt();
        return 100;
    }

    @Override
    public int getExpectedIResult() {
        return 4 + 100;
    }
}

class EAMaterializeObjReferencedFromOperandStack extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        ObjectReference xy1 = getLocalRef(bpe.thread().frame(2), XYVAL_NAME, "xy1");
        checkPrimitiveField(xy1, FD.I, "x", 2);
        checkPrimitiveField(xy1, FD.I, "y", 3);
    }

}


/**
 * Tests a regression in the implementation by setting the value of a local int which triggers the
 * creation of a deferred update and then getting the reference to a scalar replaced object.  The
 * issue was that the scalar replaced object was not reallocated. Because of the deferred update it
 * was assumed that the reallocation already happened.
 */
class EAMaterializeLocalVariableUponGetAfterSetInteger extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        setLocal(bpe.thread().frame(1), "i", env.vm().mirrorOf(43));
        ObjectReference o = getLocalRef(bpe.thread().frame(1), XYVAL_NAME, "xy");
        checkPrimitiveField(o, FD.I, "x", 4);
        checkPrimitiveField(o, FD.I, "y", 2);
    }
}

class EAMaterializeLocalVariableUponGetAfterSetIntegerTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(4, 2);
        int i = 42;
        dontinline_brkpt();
        iResult = xy.x + xy.y + i;
    }

    @Override
    public int getExpectedIResult() {
        return 4 + 2 + 43;
    }

    @Override
    public boolean testFrameShouldBeDeoptimized() {
        return true; 
    }
}


class EARelockingSimple extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference o = getLocalRef(bpe.thread().frame(1), XYVAL_NAME, "l1");
    }
}

class EARelockingSimpleTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        XYVal l1 = new XYVal(4, 2);
        synchronized (l1) {
            dontinline_brkpt();
        }
    }
}


class EARelockingSimpleWithAccessInOtherThread extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        String l1ClassName = EARelockingSimpleWithAccessInOtherThreadTarget.SyncCounter.class.getName();
        ObjectReference ctr = getLocalRef(bpe.thread().frame(1), l1ClassName, "l1");
        setField(testCase, "sharedCounter", ctr);
        terminateEndlessLoop();
    }
}

class EARelockingSimpleWithAccessInOtherThreadTarget extends EATestCaseBaseTarget {

    public static class SyncCounter {
        private int val;
        public synchronized int inc() { return val++; }
    }

    public volatile SyncCounter sharedCounter;

    @Override
    public void setUp() {
        super.setUp();
        doLoop = true;
        Thread.ofPlatform().daemon().start(() -> {
                while (doLoop) {
                    SyncCounter ctr = sharedCounter;
                    if (ctr != null) {
                        ctr.inc();
                    }
                }
            });
    }

    public void dontinline_testMethod() {
        SyncCounter l1 = new SyncCounter();
        synchronized (l1) {      
            l1.inc();
            dontinline_brkpt();  
            iResult = l1.inc();  
        }
    }

    @Override
    public int getExpectedIResult() {
        return 1;
    }
}


class EARelockingRecursiveTarget extends EATestCaseBaseTarget {

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    public void dontinline_testMethod() {
        XYVal l1 = new XYVal(4, 2);
        synchronized (l1) {
            testMethod_inlined(l1);
        }
    }

    public void testMethod_inlined(XYVal l2) {
        synchronized (l2) {
            dontinline_brkpt();
        }
    }
}

class EARelockingRecursive extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference o = getLocalRef(bpe.thread().frame(2), XYVAL_NAME, "l1");
    }
}


class EARelockingNestedInflatedTarget extends EATestCaseBaseTarget {

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    @Override
    public boolean testFrameShouldBeDeoptimized() {
        return false;
    }

    public void dontinline_testMethod() {
        XYVal l1 = inflatedLock;
        synchronized (l1) {
            testMethod_inlined(l1);
        }
    }

    public void testMethod_inlined(XYVal l2) {
        synchronized (l2) {                 
            dontinline_brkpt();
        }
    }
}

class EARelockingNestedInflated extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference o = getLocalRef(bpe.thread().frame(2), XYVAL_NAME, "l1");
    }
}


/**
 * Like {@link EARelockingNestedInflated} with the difference that there is
 * a scalar replaced object in the scope from which the object with eliminated nested locking
 * is read. This triggers materialization and relocking.
 */
class EARelockingNestedInflated_02 extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference o = getLocalRef(bpe.thread().frame(2), XYVAL_NAME, "l1");
    }
}

class EARelockingNestedInflated_02Target extends EATestCaseBaseTarget {

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    public void dontinline_testMethod() {
        @SuppressWarnings("unused")
        XYVal xy = new XYVal(1, 1);     
        XYVal l1 = inflatedLock;          
        synchronized (l1) {
            testMethod_inlined(l1);
        }
    }

    public void testMethod_inlined(XYVal l2) {
        synchronized (l2) {                 
            dontinline_brkpt();
        }
    }
}


/**
 * Like {@link EARelockingNestedInflated_02} with the difference that the
 * inflation of the lock happens because of contention.
 */
class EARelockingNestedInflated_03 extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference o = getLocalRef(bpe.thread().frame(2), XYVAL_NAME, "l1");
    }
}

class EARelockingNestedInflated_03Target extends EATestCaseBaseTarget {

    public XYVal lockInflatedByContention;
    public boolean doLockNow;
    public EATestCaseBaseTarget testCase;

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
        lockInflatedByContention = new XYVal(1, 1);
        testCase = this;
    }

    @Override
    public void warmupDone() {
        super.warmupDone();
        lockInflatedByContention = new XYVal(1, 1);
        DebuggeeWrapper.newThread(() -> {
            while (true) {
                synchronized (testCase) {
                    try {
                        if (doLockNow) {
                            doLockNow = false; 
                            testCase.notify();
                            break;
                        }
                        testCase.wait();
                    } catch (InterruptedException e) { /* ignored */ }
                }
            }
            synchronized (lockInflatedByContention) { 
                msg(Thread.currentThread().getName() + ": acquired lockInflatedByContention");
            }
            }, testCaseName + ": Lock Contender (test thread)").start();
    }

    public void dontinline_testMethod() {
        @SuppressWarnings("unused")
        XYVal xy = new XYVal(1, 1);            
        XYVal l1 = lockInflatedByContention;   
        synchronized (l1) {
            testMethod_inlined(l1);
        }
    }

    public void testMethod_inlined(XYVal l2) {
        synchronized (l2) {                 
            dontinline_notifyOtherThread();
            dontinline_brkpt();
        }
    }

    public void dontinline_notifyOtherThread() {
        if (!warmupDone) {
            return;
        }
        synchronized (testCase) {
            doLockNow = true;
            testCase.notify();
            while (doLockNow) {
                try {
                    testCase.wait();
                } catch (InterruptedException e) { /* ignored */ }
            }
        }
    }
}


/**
 * Checks if an eliminated lock of an ArgEscape object l1 can be relocked if
 * l1 is locked in a callee frame.
 */
class EARelockingArgEscapeLWLockedInCalleeFrame extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference o = getLocalRef(bpe.thread().frame(2), XYVAL_NAME, "l1");
    }
}

class EARelockingArgEscapeLWLockedInCalleeFrameTarget extends EATestCaseBaseTarget {

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    public void dontinline_testMethod() {
        XYVal l1 = new XYVal(1, 1);       
        synchronized (l1) {                   
            l1.dontinline_sync_method(this);  
        }
    }

    @Override
    public boolean testFrameShouldBeDeoptimized() {
        return !UseJVMCICompiler && super.testFrameShouldBeDeoptimized();
    }
}


/**
 * Similar to {@link EARelockingArgEscapeLWLockedInCalleeFrame}. In addition
 * the test method has got a scalar replaced object with eliminated locking.
 * This pattern matches a regression in the implementation.
 */
class EARelockingArgEscapeLWLockedInCalleeFrame_2 extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference o = getLocalRef(bpe.thread().frame(2), XYVAL_NAME, "l1");
    }
}

class EARelockingArgEscapeLWLockedInCalleeFrame_2Target extends EATestCaseBaseTarget {

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    public void dontinline_testMethod() {
        XYVal l1 = new XYVal(1, 1);       
        XYVal l2 = new XYVal(4, 2);       
        synchronized (l1) {                   
            synchronized (l2) {               
                l1.dontinline_sync_method(this);  
            }
        }
        iResult = l2.x + l2.y;
    }

    @Override
    public int getExpectedIResult() {
        return 6;
    }
}


/**
 * Similar to {@link EARelockingArgEscapeLWLockedInCalleeFrame_2Target}. It does
 * not use recursive locking and exposed a bug in the lightweight-locking implementation.
 */
class EARelockingArgEscapeLWLockedInCalleeFrameNoRecursive extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference o = getLocalRef(bpe.thread().frame(2), XYVAL_NAME, "l1");
    }
}

class EARelockingArgEscapeLWLockedInCalleeFrameNoRecursiveTarget extends EATestCaseBaseTarget {

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    public void dontinline_testMethod() {
        XYVal l1 = new XYVal(1, 1);       
        XYVal l2 = new XYVal(4, 2);       
        XYVal l3 = new XYVal(5, 3);       
        synchronized (l1) {                   
            synchronized (l2) {               
                l3.dontinline_sync_method(this);  
            }
        }
        iResult = l2.x + l2.y;
    }

    @Override
    public int getExpectedIResult() {
        return 6;
    }
}


/**
 * Test relocking eliminated (nested) locks of an object on which the
 * target thread currently waits.
 */
class EARelockingObjectCurrentlyWaitingOn extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        env.vm().resume();
        boolean inWait = false;
        do {
            Thread.sleep(100);
            env.targetMainThread.suspend();
            printStack(env.targetMainThread);
            inWait = env.targetMainThread.frame(0).location().method().name().equals("wait0");
            if (!inWait) {
                msg("Target not yet in java.lang.Object.wait(long).");
                env.targetMainThread.resume();
            }
        } while(!inWait);
        StackFrame testMethodFrame = env.targetMainThread.frame(5);
        ObjectReference l0 = getLocalRef(testMethodFrame, EARelockingObjectCurrentlyWaitingOnTarget.ForLocking.class.getName(), "l0");
        Asserts.assertEQ(l0.entryCount(), 1, "wrong entry count");
        ObjectReference l1 = getLocalRef(testMethodFrame, EARelockingObjectCurrentlyWaitingOnTarget.ForLocking.class.getName(), "l1");
        Asserts.assertEQ(l1.entryCount(), 0, "wrong entry count");
        setField(testCase, "objToNotifyOn", l1);
    }
}

class EARelockingObjectCurrentlyWaitingOnTarget extends EATestCaseBaseTarget {

    public static class ForLocking {
    }

    public volatile Object objToNotifyOn; 

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    @Override
    public void warmupDone() {
        super.warmupDone();
        Thread t = new Thread(() -> doNotify());
        t.start();
    }

    public void doNotify() {
        while (objToNotifyOn == null) {
            try {
                msg("objToNotifyOn is still null");
                Thread.sleep(100);
            } catch (InterruptedException e) { /* ignored */ }
        }
        synchronized (objToNotifyOn) {
            msg("calling objToNotifyOn.notifyAll()");
            objToNotifyOn.notifyAll();
        }
    }

    @Override
    public boolean testFrameShouldBeDeoptimized() {
        return false;
    }

    @Override
    public void dontinline_testMethod() throws Exception {
        ForLocking l0 = new ForLocking(); 
        ForLocking l1 = new ForLocking();
        synchronized (l0) {
            synchronized (l1) {
                testMethod_inlined(l1);
            }
        }
    }

    public void testMethod_inlined(ForLocking l2) throws Exception {
        synchronized (l2) {                 
            dontinline_waitWhenWarmupDone(l2);
        }
    }

    public void dontinline_waitWhenWarmupDone(ForLocking l2) throws Exception {
        if (warmupDone) {
            l2.wait();
        }
    }
}



/**
 * Test relocking eliminated @ValueBased object.
 */
class EARelockingValueBased extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference o = getLocalRef(bpe.thread().frame(1), Integer.class.getName(), "l1");
    }
}

class EARelockingValueBasedTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        Integer l1 = new Integer(255);
        synchronized (l1) {
            dontinline_brkpt();
        }
    }
}


/**
 * Let xy be NoEscape whose allocation cannot be eliminated (simulated by
 * -XX:-EliminateAllocations). The holding compiled frame has to be deoptimized when debugger
 * accesses xy because afterwards locking on xy is omitted.
 * Note: there are no EA based optimizations at the escape point.
 */
class EADeoptFrameAfterReadLocalObject_01 extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference xy = getLocalRef(bpe.thread().frame(1), XYVAL_NAME, "xy");
    }
}

class EADeoptFrameAfterReadLocalObject_01Target extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(1, 1);
        dontinline_brkpt();              
        synchronized (xy) {              
            xy.x++;
            xy.y++;
        }
    }
}


/**
 * Similar to {@link EADeoptFrameAfterReadLocalObject_01} with the difference that the debugger
 * reads xy from an inlined callee. So xy is NoEscape instead of ArgEscape.
 */
class EADeoptFrameAfterReadLocalObject_01BTarget extends EATestCaseBaseTarget {

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    public void dontinline_testMethod() {
        XYVal xy  = new XYVal(1, 1);
        callee(xy);                 
        synchronized (xy) {         
            xy.x++;
            xy.y++;
        }
    }

    public void callee(XYVal xy) {
        dontinline_brkpt();              
    }
}

class EADeoptFrameAfterReadLocalObject_01B extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference xy = getLocalRef(bpe.thread().frame(1), "callee", "xy", XYVAL_NAME);
    }
}


/**
 * Let xy be ArgEscape. The frame dontinline_testMethod() has to be deoptimized when debugger
 * acquires xy from dontinline_callee() because afterwards locking on xy is omitted.
 * Note: there are no EA based optimizations at the escape point.
 */
class EADeoptFrameAfterReadLocalObject_02 extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference xy = getLocalRef(bpe.thread().frame(1), "dontinline_callee", "xy", XYVAL_NAME);
    }
}

class EADeoptFrameAfterReadLocalObject_02Target extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        XYVal xy  = new XYVal(1, 1);
        dontinline_callee(xy);      
        synchronized (xy) {         
            xy.x++;
            xy.y++;
        }
    }

    public void dontinline_callee(XYVal xy) {
        dontinline_brkpt();              
    }

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    @Override
    public boolean testFrameShouldBeDeoptimized() {
        return !UseJVMCICompiler && super.testFrameShouldBeDeoptimized();
    }
}


/**
 * Similar to {@link EADeoptFrameAfterReadLocalObject_02} there is an ArgEscape object xy, but in
 * contrast it is not in the parameter list of a call when the debugger reads an object.
 * Therefore the frame of the test method should not be deoptimized
 */
class EADeoptFrameAfterReadLocalObject_02B extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference xy = getLocalRef(bpe.thread().frame(1), "dontinline_callee", "xy", XYVAL_NAME);
    }
}

class EADeoptFrameAfterReadLocalObject_02BTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        XYVal xy  = new XYVal(1, 1);
        dontinline_make_arg_escape(xy);  
        dontinline_callee();             
        synchronized (xy) {              
            xy.x++;
            xy.y++;
        }
    }

    public void dontinline_callee() {
        @SuppressWarnings("unused")
        XYVal xy  = new XYVal(2, 2);
        dontinline_brkpt();              
    }

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    @Override
    public boolean testFrameShouldBeDeoptimized() {
        return false;
    }
}


/**
 * Similar to {@link EADeoptFrameAfterReadLocalObject_02} there is an ArgEscape object xy in
 * dontinline_testMethod() which is being passed as parameter when the debugger accesses a local object.
 * Nevertheless dontinline_testMethod must not be deoptimized because there is an entry frame
 * between it and the frame accessed by the debugger.
 */
class EADeoptFrameAfterReadLocalObject_02C extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        @SuppressWarnings("unused")
        ObjectReference xy = getLocalRef(bpe.thread().frame(1), "dontinline_callee_accessed_by_debugger", "xy", XYVAL_NAME);
    }
}

class EADeoptFrameAfterReadLocalObject_02CTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        XYVal xy  = new XYVal(1, 1);
        dontinline_callee(xy);           
        synchronized (xy) {              
            xy.x++;
            xy.y++;
        }
    }

    public void dontinline_callee(XYVal xy) {
        if (warmupDone) {
            dontinline_call_with_entry_frame(this, "dontinline_callee_accessed_by_debugger");
        }
    }

    public void dontinline_callee_accessed_by_debugger() {
        @SuppressWarnings("unused")
        XYVal xy  = new XYVal(2, 2);
        dontinline_brkpt();              
    }

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 11-5;
    }

    @Override
    public boolean testFrameShouldBeDeoptimized() {
        return false;
    }
}


/**
 * Let xy be NoEscape whose allocation cannot be eliminated (e.g. because of
 * -XX:-EliminateAllocations).  The holding compiled frame has to be deoptimized when debugger
 * accesses xy because the following field accesses get eliminated.  Note: there are no EA based
 * optimizations at the escape point.
 */
class EADeoptFrameAfterReadLocalObject_03 extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        ObjectReference xy = getLocalRef(bpe.thread().frame(1), XYVAL_NAME, "xy");
        setField(xy, "x", env.vm().mirrorOf(1));
    }
}

class EADeoptFrameAfterReadLocalObject_03Target extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(0, 1);
        dontinline_brkpt();              
        iResult = xy.x + xy.y;           
    }

    @Override
    public int getExpectedIResult() {
        return 1 + 1;
    }
}


class EAGetOwnedMonitorsTarget extends EATestCaseBaseTarget {

    public long checkSum;

    public void dontinline_testMethod() {
        XYVal l1 = new XYVal(4, 2);
        synchronized (l1) {
            dontinline_endlessLoop();
        }
    }

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
        loopCount = 3;
    }

    public void warmupDone() {
        super.warmupDone();
        msg("enter 'endless' loop by setting loopCount = Long.MAX_VALUE");
        loopCount = Long.MAX_VALUE; 
    }
}

class EAGetOwnedMonitors extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        msg("resume");
        env.vm().resume();
        waitUntilTargetHasEnteredEndlessLoop();
        msg("suspend target");
        env.targetMainThread.suspend();
        msg("Get owned monitors");
        List<ObjectReference> monitors = env.targetMainThread.ownedMonitors();
        Asserts.assertEQ(monitors.size(), 1, "unexpected number of owned monitors");
        terminateEndlessLoop();
    }
}


class EAEntryCountTarget extends EATestCaseBaseTarget {

    public long checkSum;

    public void dontinline_testMethod() {
        XYVal l1 = new XYVal(4, 2);
        synchronized (l1) {
            inline_testMethod2(l1);
        }
    }

    public void inline_testMethod2(XYVal l1) {
        synchronized (l1) {
            dontinline_endlessLoop();
        }
    }

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
        loopCount = 3;
    }

    public void warmupDone() {
        super.warmupDone();
        msg("enter 'endless' loop by setting loopCount = Long.MAX_VALUE");
        loopCount = Long.MAX_VALUE; 
    }
}

class EAEntryCount extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        msg("resume");
        env.vm().resume();
        waitUntilTargetHasEnteredEndlessLoop();
        msg("suspend target");
        env.targetMainThread.suspend();
        msg("Get owned monitors");
        List<ObjectReference> monitors = env.targetMainThread.ownedMonitors();
        Asserts.assertEQ(monitors.size(), 1, "unexpected number of owned monitors");
        msg("Get entry count");
        int entryCount = monitors.get(0).entryCount();
        Asserts.assertEQ(entryCount, 2, "wrong entry count");
        terminateEndlessLoop();
    }
}


/**
 * PopFrame into caller frame with scalar replaced objects.
 */
class EAPopFrameNotInlined extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        printStack(bpe.thread());
        msg("PopFrame");
        bpe.thread().popFrames(bpe.thread().frame(0));
        msg("PopFrame DONE");
    }

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() || env.targetVMOptions.UseJVMCICompiler;
    }
}

class EAPopFrameNotInlinedTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(4, 2);
        dontinline_brkpt();
        iResult = xy.x + xy.y;
    }

    @Override
    public boolean testFrameShouldBeDeoptimized() {
        return false;
    }

    @Override
    public int getExpectedIResult() {
        return 4 + 2;
    }

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() || UseJVMCICompiler;
    }
}


/**
 * Pop frames into {@link EAPopFrameNotInlinedReallocFailureTarget#dontinline_testMethod()} which
 * holds scalar replaced objects. In preparation of the pop frame operations the vm eagerly
 * reallocates scalar replaced objects to avoid failures when actually popping the frames. We provoke
 * a reallocation failures and expect {@link VMOutOfMemoryException}.
 */
class EAPopFrameNotInlinedReallocFailure extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        ThreadReference thread = bpe.thread();
        printStack(thread);
        msg("PopFrame");
        boolean coughtOom = false;
        try {
            thread.popFrames(thread.frame(1));
        } catch (VMOutOfMemoryException oom) {
            msg("cought OOM");
            coughtOom  = true;
        }
        freeAllMemory();
        Asserts.assertTrue(coughtOom, "PopFrame should have triggered an OOM exception in target");
        String expectedTopFrame = "dontinline_consume_all_memory_brkpt";
        Asserts.assertEQ(expectedTopFrame, thread.frame(0).location().method().name());
        printStack(thread);
    }

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() ||
                !env.targetVMOptions.EliminateAllocations ||
                env.targetVMOptions.ZGCIsSelected ||
                env.targetVMOptions.ShenandoahGCIsSelected ||
                env.targetVMOptions.DeoptimizeObjectsALot ||
                env.targetVMOptions.UseJVMCICompiler;
    }
}

class EAPopFrameNotInlinedReallocFailureTarget extends EATestCaseBaseTarget {

    public boolean doneAlready;

    public void dontinline_testMethod() {
        long a[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};                
        Vector10 v = new Vector10(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);  
        dontinline_consume_all_memory_brkpt();
        lResult = a[0] + a[1] + a[2] + a[3] + a[4] + a[5] + a[6] + a[7] + a[8] + a[9]
               + v.i0 + v.i1 + v.i2 + v.i3 + v.i4 + v.i5 + v.i6 + v.i7 + v.i8 + v.i9;
    }

    public void dontinline_consume_all_memory_brkpt() {
        if (warmupDone && !doneAlready) {
            doneAlready = true;
            consumeAllMemory(); 
            dontinline_brkpt();
        }
    }

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    @Override
    public long getExpectedLResult() {
        long n = 10;
        return 2*n*(n+1)/2;
    }

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() ||
                !EliminateAllocations ||
                ZGCIsSelected ||
                ShenandoahGCIsSelected ||
                DeoptimizeObjectsALot ||
                UseJVMCICompiler;
    }
}


/**
 * Pop inlined top frame dropping into method with scalar replaced opjects.
 */
class EAPopInlinedMethodWithScalarReplacedObjectsReallocFailure extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        ThreadReference thread = env.targetMainThread;
        env.vm().resume();
        waitUntilTargetHasEnteredEndlessLoop();

        thread.suspend();
        printStack(thread);

        msg("Pop Frames");
        boolean coughtOom = false;
        try {
            thread.popFrames(thread.frame(0));    
        } catch (VMOutOfMemoryException oom) {
            msg("cought OOM");
            coughtOom = true;
        }
        printStack(thread);

        freeAllMemory();
        setField(testCase, "loopCount", env.vm().mirrorOf(0)); 
        Asserts.assertTrue(coughtOom, "PopFrame should have triggered an OOM exception in target");
        String expectedTopFrame = "inlinedCallForcedToReturn";
        Asserts.assertEQ(expectedTopFrame, thread.frame(0).location().method().name());
    }

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() ||
                !env.targetVMOptions.EliminateAllocations ||
                env.targetVMOptions.ZGCIsSelected ||
                env.targetVMOptions.ShenandoahGCIsSelected ||
                env.targetVMOptions.DeoptimizeObjectsALot ||
                env.targetVMOptions.UseJVMCICompiler;
    }
}

class EAPopInlinedMethodWithScalarReplacedObjectsReallocFailureTarget extends EATestCaseBaseTarget {

    public long checkSum;

    public void dontinline_testMethod() {
        long a[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};                
        Vector10 v = new Vector10(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);  
        long l = inlinedCallForcedToReturn();
        lResult = a[0] + a[1] + a[2] + a[3] + a[4] + a[5] + a[6] + a[7] + a[8] + a[9]
               + v.i0 + v.i1 + v.i2 + v.i3 + v.i4 + v.i5 + v.i6 + v.i7 + v.i8 + v.i9;
    }

    public long inlinedCallForcedToReturn() {
        long cs = checkSum;
        dontinline_consumeAllMemory();
        while (loopCount-- > 0) {
            targetIsInLoop = true;
            checkSum += checkSum % ++cs;
        }
        loopCount = 3;
        targetIsInLoop = false;
        return checkSum;
    }

    public void dontinline_consumeAllMemory() {
        if (warmupDone && (loopCount > 3)) {
            consumeAllMemory();
        }
    }

    @Override
    public long getExpectedLResult() {
        long n = 10;
        return 2*n*(n+1)/2;
    }

    @Override
    public void setUp() {
        super.setUp();
        loopCount = 3;
    }

    public void warmupDone() {
        super.warmupDone();
        msg("enter 'endless' loop by setting loopCount = Long.MAX_VALUE");
        loopCount = Long.MAX_VALUE; 
    }

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() ||
                !EliminateAllocations ||
                ZGCIsSelected ||
                ShenandoahGCIsSelected ||
                DeoptimizeObjectsALot ||
                UseJVMCICompiler;
    }
}


/**
 * ForceEarlyReturn into caller frame with scalar replaced objects.
 */
class EAForceEarlyReturnNotInlined extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        BreakpointEvent bpe = resumeTo(TARGET_TESTCASE_BASE_NAME, "dontinline_brkpt", "()V");
        ThreadReference thread = bpe.thread();
        printStack(thread);

        msg("Step out");
        env.stepOut(thread);                               
        printStack(thread);
        msg("ForceEarlyReturn");
        thread.forceEarlyReturn(env.vm().mirrorOf(43));    
        msg("Step over line");
        env.stepOverLine(thread);                          
        printStack(thread);
        msg("ForceEarlyReturn DONE");
    }

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() || env.targetVMOptions.UseJVMCICompiler;
    }
}

class EAForceEarlyReturnNotInlinedTarget extends EATestCaseBaseTarget {

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(4, 2);
        int i = dontinline_brkpt_iret();
        iResult = xy.x + xy.y + i;
    }

    @Override
    public int getExpectedIResult() {
        return 4 + 2 + 43;
    }

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
    }

    public boolean testFrameShouldBeDeoptimized() {
        return true; 
    }

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() || UseJVMCICompiler;
    }
}


/**
 * ForceEarlyReturn at safepoint in frame with scalar replaced objects.
 */
class EAForceEarlyReturnOfInlinedMethodWithScalarReplacedObjects extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        ThreadReference thread = env.targetMainThread;
        env.vm().resume();
        waitUntilTargetHasEnteredEndlessLoop();

        thread.suspend();
        printStack(thread);

        msg("ForceEarlyReturn");
        thread.forceEarlyReturn(env.vm().mirrorOf(43));    
        msg("Step over instruction to do the forced return");
        env.stepOverInstruction(thread);
        printStack(thread);
        msg("ForceEarlyReturn DONE");
    }

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() || env.targetVMOptions.UseJVMCICompiler;
    }
}

class EAForceEarlyReturnOfInlinedMethodWithScalarReplacedObjectsTarget extends EATestCaseBaseTarget {

    public int checkSum;

    public void dontinline_testMethod() {
        XYVal xy = new XYVal(4, 2);
        int i = inlinedCallForcedToReturn();
        iResult = xy.x + xy.y + i;
    }

    public int inlinedCallForcedToReturn() {               
        int i = checkSum;
        while (loopCount-- > 0) {
            targetIsInLoop = true;
            checkSum += checkSum % ++i;
        }
        loopCount = 3;
        targetIsInLoop = false;
        return checkSum;
    }

    @Override
    public int getExpectedIResult() {
        return 4 + 2 + 43;
    }

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
        loopCount = 3;
    }

    public void warmupDone() {
        super.warmupDone();
        msg("enter 'endless' loop by setting loopCount = Long.MAX_VALUE");
        loopCount = Long.MAX_VALUE; 
    }

    public boolean testFrameShouldBeDeoptimized() {
        return true; 
    }

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() || UseJVMCICompiler;
    }
}


/**
 * ForceEarlyReturn with reallocation failure.
 */
class EAForceEarlyReturnOfInlinedMethodWithScalarReplacedObjectsReallocFailure extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        ThreadReference thread = env.targetMainThread;
        env.vm().resume();
        waitUntilTargetHasEnteredEndlessLoop();

        thread.suspend();
        printStack(thread);

        msg("ForceEarlyReturn");
        boolean coughtOom = false;
        try {
            thread.forceEarlyReturn(env.vm().mirrorOf(43));    
        } catch (VMOutOfMemoryException oom) {
            msg("cought OOM");
            coughtOom   = true;
        }
        freeAllMemory();
        Asserts.assertTrue(coughtOom, "ForceEarlyReturn should have triggered an OOM exception in target");
        printStack(thread);
        msg("ForceEarlyReturn(2)");
        thread.forceEarlyReturn(env.vm().mirrorOf(43));
        msg("Step over instruction to do the forced return");
        env.stepOverInstruction(thread);
        printStack(thread);
        msg("ForceEarlyReturn DONE");
    }

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() ||
                !env.targetVMOptions.EliminateAllocations ||
                env.targetVMOptions.ZGCIsSelected ||
                env.targetVMOptions.ShenandoahGCIsSelected ||
                env.targetVMOptions.DeoptimizeObjectsALot ||
                env.targetVMOptions.UseJVMCICompiler;
    }
}

class EAForceEarlyReturnOfInlinedMethodWithScalarReplacedObjectsReallocFailureTarget extends EATestCaseBaseTarget {

    public int checkSum;

    public void dontinline_testMethod() {
        long a[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};                
        Vector10 v = new Vector10(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);  
        long l = inlinedCallForcedToReturn();
        lResult = a[0] + a[1] + a[2] + a[3] + a[4] + a[5] + a[6] + a[7] + a[8] + a[9]
               + v.i0 + v.i1 + v.i2 + v.i3 + v.i4 + v.i5 + v.i6 + v.i7 + v.i8 + v.i9 + l;
    }

    public long inlinedCallForcedToReturn() {                      
        long cs = checkSum;
        dontinline_consumeAllMemory();
        while (loopCount-- > 0) {
            targetIsInLoop = true;
            checkSum += checkSum % ++cs;
        }
        loopCount = 3;
        targetIsInLoop = false;
        return checkSum;
    }

    public void dontinline_consumeAllMemory() {
        if (warmupDone) {
            consumeAllMemory();
        }
    }

    @Override
    public long getExpectedLResult() {
        long n = 10;
        return 2*n*(n+1)/2 + 43;
    }

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
        loopCount = 3;
    }

    public void warmupDone() {
        super.warmupDone();
        msg("enter 'endless' loop by setting loopCount = Long.MAX_VALUE");
        loopCount = Long.MAX_VALUE; 
    }

    @Override
    public boolean shouldSkip() {
        return super.shouldSkip() ||
                !EliminateAllocations ||
                ZGCIsSelected ||
                ShenandoahGCIsSelected ||
                DeoptimizeObjectsALot ||
                UseJVMCICompiler;
    }
}


/**
 * Check if instances of a type are found even if they are scalar replaced.  To stress the
 * implementation a little more, the instances should be retrieved while the target is running.
 */
class EAGetInstancesOfReferenceType extends EATestCaseBaseDebugger {

    public void runTestCase() throws Exception {
        printStack(env.targetMainThread);
        ReferenceType cls = ((ClassObjectReference)getField(testCase, "cls")).reflectedType();
        msg("reflected type is " + cls);
        msg("resume");
        env.vm().resume();
        waitUntilTargetHasEnteredEndlessLoop();
        msg("Retrieve instances of " + cls.name());
        List<ObjectReference> instances = cls.instances(10);
        Asserts.assertEQ(instances.size(), 3, "unexpected number of instances of " + cls.name());
        msg("suspend");
        env.targetMainThread.suspend();
        terminateEndlessLoop();
    }
}

class EAGetInstancesOfReferenceTypeTarget extends EATestCaseBaseTarget {

    public long checkSum;

    public static Class<LocalXYVal> cls = LocalXYVal.class;

    public static class LocalXYVal {
        public int x, y;

        public LocalXYVal(int x, int y) {
            this.x = x; this.y = y;
        }
    }

    @Override
    public void dontinline_testMethod() {
        LocalXYVal p1 = new LocalXYVal(4, 2);
        LocalXYVal p2 = new LocalXYVal(5, 3);
        LocalXYVal p3 = new LocalXYVal(6, 4);
        dontinline_endlessLoop();
        iResult = p1.x+p1.y + p2.x+p2.y + p3.x+p3.y;
    }

    @Override
    public int getExpectedIResult() {
        return 6+8+10;
    }

    @Override
    public void setUp() {
        super.setUp();
        testMethodDepth = 2;
        loopCount = 3;
    }

    public void warmupDone() {
        super.warmupDone();
        msg("enter 'endless' loop by setting loopCount = Long.MAX_VALUE");
        loopCount = Long.MAX_VALUE; 
    }
}



class XYVal {

    public int x, y;

    public XYVal(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Note that we don't use a sync block here because javac would generate an synthetic exception
     * handler for the synchronized block that catches Throwable E, unlocks and throws E
     * again. The throw bytecode causes the BCEscapeAnalyzer to set the escape state to GlobalEscape
     * (see comment on exception handlers in BCEscapeAnalyzer::iterate_blocks())
     */
    public synchronized void dontinline_sync_method(EATestCaseBaseTarget target) {
        target.dontinline_brkpt();
    }

    /**
     * Just like {@link #dontinline_sync_method(EATestCaseBaseTarget)} but without the call to
     * {@link EATestCaseBaseTarget#dontinline_brkpt()}.
     */
    public synchronized void dontinline_sync_method_no_brkpt(EATestCaseBaseTarget target) {
    }
}

class Vector10 {
    int i0, i1, i2, i3, i4, i5, i6, i7, i8, i9;
    public Vector10(int j0, int j1, int j2, int j3, int j4, int j5, int j6, int j7, int j8, int j9) {
        i0=j0; i1=j1; i2=j2; i3=j3; i4=j4; i5=j5; i6=j6; i7=j7; i8=j8; i9=j9;
    }
}

class ILFDO {

    public int i;
    public int i2;
    public long l;
    public long l2;
    public float f;
    public float f2;
    public double d;
    public double d2;
    public Long o;
    public Long o2;

    public ILFDO(int i,
                 int i2,
                 long l,
                 long l2,
                 float f,
                 float f2,
                 double d,
                 double d2,
                 Long o,
                 Long o2) {
        this.i = i;
        this.i2 = i2;
        this.l = l;
        this.l2 = l2;
        this.f = f;
        this.f2 = f2;
        this.d = d;
        this.d2 = d2;
        this.o = o;
        this.o2 = o2;
    }

}
