/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.io;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.security.ProtectionDomain;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.IllegalClassFormatException;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @summary Test that will instrument the same classes that JFR will also instrument.
 * @key jfr
 * @requires vm.hasJFR
 *
 * @library /test/lib /test/jdk
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 *          jdk.jfr
 *
 * @run main/othervm jdk.jfr.event.io.TestInstrumentation
 */

public class TestInstrumentation implements ClassFileTransformer {

    private static Instrumentation instrumentation = null;
    private static TestInstrumentation testTransformer = null;

    private static final String[] instrMethodKeys = {
        "java/io/RandomAccessFile::seek::(J)V",
        "java/io/RandomAccessFile::read::()I",
        "java/io/RandomAccessFile::read::([B)I",
        "java/io/RandomAccessFile::write::([B)V",
        "java/io/RandomAccessFile::write::(I)V",
        "java/io/RandomAccessFile::close::()V",
        "java/io/FileInputStream::read::([BII)I",
        "java/io/FileInputStream::read::([B)I",
        "java/io/FileInputStream::read::()I",
        "java/io/FileOutputStream::write::(I)V",
        "java/io/FileOutputStream::write::([B)V",
        "java/io/FileOutputStream::write::([BII)V",
        "java/net/Socket$SocketInputStream::read::()I",
        "java/net/Socket$SocketInputStream::read::([BII)I",
        "java/net/Socket$SocketInputStream::close::()V",
        "java/net/Socket$SocketOutputStream::write::(I)V",
        "java/net/Socket$SocketOutputStream::write::([BII)V",
        "java/net/Socket$SocketOutputStream::close::()V",
        "java/nio/channels/FileChannel::read::([Ljava/nio/ByteBuffer;)J",
        "java/nio/channels/FileChannel::write::([Ljava/nio/ByteBuffer;)J",
        "java/nio/channels/SocketChannel::open::()Ljava/nio/channels/SocketChannel;",
        "java/nio/channels/SocketChannel::open::(Ljava/net/SocketAddress;)Ljava/nio/channels/SocketChannel;",
        "java/nio/channels/SocketChannel::read::([Ljava/nio/ByteBuffer;)J",
        "java/nio/channels/SocketChannel::write::([Ljava/nio/ByteBuffer;)J",
        "sun/nio/ch/FileChannelImpl::read::(Ljava/nio/ByteBuffer;)I",
        "sun/nio/ch/FileChannelImpl::write::(Ljava/nio/ByteBuffer;)I",
    };

    private static String getInstrMethodKey(String className, String methodName, String signature) {
        return className + "::" + methodName + "::" + signature;
    }

    private static String getClassFromMethodKey(String methodKey) {
        return methodKey.split("::")[0];
    }

    private static Set<String> instrClassesTarget = null;

    private static Set<String> instrClassesDone = null;

    static {
        instrClassesTarget = new HashSet<String>();
        instrClassesDone = new HashSet<String>();
        for (String s : instrMethodKeys) {
            String className = getClassFromMethodKey(s);
            instrClassesTarget.add(className);
        }
    }

    private static void log(String msg) {
        System.out.println("TestTransformation: " + msg);
    }



    public static class TestMain {

        private enum TransformStatus { Transformed, Retransformed, Removed }

        public static void main(String[] args) throws Throwable {
            runAllTests(TransformStatus.Transformed);

            Set<Class<?>> classes = new HashSet<Class<?>>();
            for (String className : instrClassesTarget) {
                Class<?> clazz = Class.forName(className.replaceAll("/", "."));
                classes.add(clazz);
                log("Will retransform " + clazz.getName());
            }
            instrumentation.retransformClasses(classes.toArray(new Class<?>[0]));

            InstrumentationCallback.clear();
            runAllTests(TransformStatus.Retransformed);

            instrumentation.removeTransformer(testTransformer);
            instrumentation.retransformClasses(classes.toArray(new Class<?>[0]));
            InstrumentationCallback.clear();
            runAllTests(TransformStatus.Removed);
        }

        public static void runAllTests(TransformStatus status) throws Throwable {
            log("runAllTests, TransformStatus: " + status);
            try {
                String[] noArgs = new String[0];
                TestRandomAccessFileEvents.main(noArgs);
                TestSocketEvents.main(noArgs);
                TestSocketChannelEvents.main(noArgs);
                TestFileChannelEvents.main(noArgs);
                TestFileStreamEvents.main(noArgs);
                TestDisabledEvents.main(noArgs);

                Set<String> callbackKeys = InstrumentationCallback.getKeysCopy();
                for (String key : instrMethodKeys) {
                    boolean gotCallback = callbackKeys.contains(key);
                    boolean expectsCallback = isClassInstrumented(status, key);
                    String msg = String.format("key:%s, expects:%b", key, expectsCallback);
                    if (gotCallback != expectsCallback) {
                        throw new Exception("Wrong callback() for " + msg);
                    } else {
                        log("Correct callback() for " + msg);
                    }
                }
            } catch (Throwable t) {
                log("Test failed in phase " + status);
                t.printStackTrace();
                throw t;
            }
        }

        private static boolean isClassInstrumented(TransformStatus status, String key) throws Throwable {
            switch (status) {
            case Retransformed:
                return true;
            case Removed:
                return false;
            case Transformed:
                String className = getClassFromMethodKey(key);
                return instrClassesDone.contains(className);
            }
            throw new Exception("Test error: Unknown TransformStatus: " + status);
        }
    }



    public static void main(String[] args) throws Throwable {
        buildJar("TestInstrumentation", true);
        buildJar("InstrumentationCallback", false);
        launchTest();
    }

    private static void buildJar(String jarName, boolean withManifest) throws Throwable {
        final String slash = File.separator;
        final String packageName = "jdk/jfr/event/io".replace("/", slash);
        System.out.println("buildJar packageName: " + packageName);

        String testClasses = System.getProperty("test.classes", "?");
        String testSrc = System.getProperty("test.src", "?");
        String jarPath = testClasses + slash + jarName + ".jar";
        String manifestPath = testSrc + slash + jarName + ".mf";
        String className = packageName + slash + jarName + ".class";

        String[] args = null;
        if (withManifest) {
            args = new String[] {"-cfm", jarPath, manifestPath, "-C", testClasses, className};
        } else {
            args = new String[] {"-cf", jarPath, "-C", testClasses, className};
        }

        log("Running jar " + Arrays.toString(args));
        sun.tools.jar.Main jarTool = new sun.tools.jar.Main(System.out, System.err, "jar");
        if (!jarTool.run(args)) {
            throw new Exception("jar failed: args=" + Arrays.toString(args));
        }
    }

    private static void launchTest() throws Throwable {
        final String slash = File.separator;

        String classpath =
            System.getProperty("test.class.path", "") + File.pathSeparator +
            System.getProperty("test.jdk", ".") + slash + "lib" + slash + "tools.jar";
        String testClassDir = System.getProperty("test.classes", "") + slash;

        String[] args = {
            "-Xbootclasspath/a:" + testClassDir + "InstrumentationCallback.jar",
            "--add-exports", "java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
            "-classpath", classpath,
            "-javaagent:" + testClassDir + "TestInstrumentation.jar",
            "jdk.jfr.event.io.TestInstrumentation$TestMain" };
        OutputAnalyzer output = ProcessTools.executeTestJava(args);
        output.shouldHaveExitValue(0);
    }



    public static void premain(String args, Instrumentation inst) throws Exception {
        instrumentation = inst;
        testTransformer = new TestInstrumentation();
        inst.addTransformer(testTransformer, true);
    }

    public byte[] transform(
            ClassLoader classLoader, String className, Class<?> classBeingRedefined,
            ProtectionDomain pd, byte[] bytes) throws IllegalClassFormatException {
        if (!instrClassesTarget.contains(className)) {
            return null;
        }

        boolean isRedefinition = classBeingRedefined != null;
        log("instrument class(" + className + ") " + (isRedefinition ? "redef" : "load"));

        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(
                reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        CallbackClassVisitor classVisitor = new CallbackClassVisitor(writer);
        reader.accept(classVisitor, 0);
        instrClassesDone.add(className);
        return writer.toByteArray();
    }

    private static class CallbackClassVisitor extends ClassVisitor {
        private String className;

        public CallbackClassVisitor(ClassVisitor cv) {
            super(Opcodes.ASM7, cv);
        }

        @Override
        public void visit(
                int version, int access, String name, String signature,
                String superName, String[] interfaces) {
            cv.visit(version, access, name, signature, superName, interfaces);
            className = name;
        }

        @Override
        public MethodVisitor visitMethod(
                int access, String methodName, String desc, String signature, String[] exceptions) {
            String methodKey = getInstrMethodKey(className, methodName, desc);
            boolean isInstrumentedMethod = Arrays.asList(instrMethodKeys).contains(methodKey);
            MethodVisitor mv = cv.visitMethod(access, methodName, desc, signature, exceptions);
            if (isInstrumentedMethod && mv != null) {
                mv = new CallbackMethodVisitor(mv, methodKey);
                log("instrumented " + methodKey);
            }
            return mv;
        }
    }

    public static class CallbackMethodVisitor extends MethodVisitor {
        private String logMessage;

        public CallbackMethodVisitor(MethodVisitor mv, String logMessage) {
            super(Opcodes.ASM7, mv);
            this.logMessage = logMessage;
        }

        @Override
        public void visitCode() {
            mv.visitCode();
            String methodDescr = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class));
            String className = InstrumentationCallback.class.getName().replace('.', '/');
            mv.visitLdcInsn(logMessage);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "callback", methodDescr);
        }
    }

}
