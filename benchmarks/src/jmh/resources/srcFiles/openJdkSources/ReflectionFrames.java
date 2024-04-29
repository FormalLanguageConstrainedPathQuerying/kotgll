/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8173898
 * @summary Basic test for checking filtering of reflection frames
 * @run testng ReflectionFrames
 */

import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.lang.StackWalker.Option.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReflectionFrames {
    final static boolean verbose = false;
    final static Class<?> REFLECT_ACCESS = findClass("java.lang.reflect.ReflectAccess");
    final static Class<?> REFLECTION_FACTORY = findClass("jdk.internal.reflect.ReflectionFactory");

    private static Class<?> findClass(String cn) {
        try {
            return Class.forName(cn);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * This test invokes new StackInspector() directly from
     * the caller StackInspector.Caller.create method.
     * It checks that the caller is StackInspector.Caller.
     * It also checks the expected frames collected
     * by walking the stack from the default StackInspector()
     * constructor.
     * This is done twice, once using a default StackWalker
     * that hides reflection frames, once using a StackWalker
     * configured to show reflection frames.
     */
    @Test
    public static void testNewStackInspector() throws Exception {
        StackInspector.walker.set(StackInspector.walkerHide);

        System.out.println("testNewStackInspector: create");

        StackInspector obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("create", How.class)
                             .invoke(null, How.NEW));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             ReflectionFrames.class.getName()
                                 +"::testNewStackInspector"));
        assertEquals(obj.cls, StackInspector.Caller.class);
        assertEquals(obj.filtered, 0);

        System.out.println("testNewStackInspector: reflect");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("reflect", How.class)
                             .invoke(null, How.NEW));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             StackInspector.Caller.class.getName()
                                 +"::reflect",
                             ReflectionFrames.class.getName()
                                 +"::testNewStackInspector"));
        assertEquals(obj.cls, StackInspector.Caller.class);
        assertEquals(obj.filtered, 0);

        System.out.println("testNewStackInspector: handle");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("handle", How.class)
                             .invoke(null, How.NEW));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             StackInspector.Caller.class.getName()
                                 +"::handle",
                             ReflectionFrames.class.getName()
                                 +"::testNewStackInspector"));
        assertEquals(obj.cls, StackInspector.Caller.class);
        assertEquals(obj.filtered, 0);

        StackInspector.walker.set(StackInspector.walkerShow);

        System.out.println("testNewStackInspector: create: show reflect");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("create", How.class)
                             .invoke(null, How.NEW));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             Method.class.getName()
                                 +"::invoke",
                             ReflectionFrames.class.getName()
                                 +"::testNewStackInspector"));
        assertEquals(obj.cls, StackInspector.Caller.class);
        assertNotEquals(obj.filtered, 0);

        System.out.println("testNewStackInspector: reflect: show reflect");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("reflect", How.class)
                             .invoke(null, How.NEW));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             Method.class.getName()
                                 +"::invoke",
                             StackInspector.Caller.class.getName()
                                 +"::reflect",
                             Method.class.getName()
                                 +"::invoke",
                             ReflectionFrames.class.getName()
                                 +"::testNewStackInspector"));
        assertEquals(obj.cls, StackInspector.Caller.class);
        assertNotEquals(obj.filtered, 0);

        System.out.println("testNewStackInspector: handle: show reflect");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("handle", How.class)
                             .invoke(null, How.NEW));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             StackInspector.Caller.class.getName()
                                 +"::handle",
                             Method.class.getName()
                                 +"::invoke",
                             ReflectionFrames.class.getName()
                                 +"::testNewStackInspector"));
        assertEquals(obj.cls, StackInspector.Caller.class);
        assertNotEquals(obj.filtered, 0);
    }

   /**
     * This test invokes Constructor.newInstance() from
     * the caller StackInspector.Caller.create method.
     * It checks that the caller is StackInspector.Caller.
     * It also checks the expected frames collected
     * by walking the stack from the default StackInspector()
     * constructor.
     * This is done twice, once using a default StackWalker
     * that hides reflection frames, once using a StackWalker
     * configured to show reflection frames.
     */
    @Test
    public static void testConstructor() throws Exception {
        StackInspector.walker.set(StackInspector.walkerHide);

        System.out.println("testConstructor: create");

        StackInspector obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("create", How.class)
                             .invoke(null, How.CONSTRUCTOR));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             ReflectionFrames.class.getName()
                                 +"::testConstructor"));
        assertEquals(obj.cls, StackInspector.Caller.class);
        assertEquals(obj.filtered, 0);

        System.out.println("testConstructor: reflect");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("reflect", How.class)
                             .invoke(null, How.CONSTRUCTOR));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             StackInspector.Caller.class.getName()
                                 +"::reflect",
                             ReflectionFrames.class.getName()
                                 +"::testConstructor"));
        assertEquals(obj.cls, StackInspector.Caller.class);
        assertEquals(obj.filtered, 0);

        System.out.println("testConstructor: handle");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("handle", How.class)
                             .invoke(null, How.CONSTRUCTOR));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             StackInspector.Caller.class.getName()
                                 +"::handle",
                             ReflectionFrames.class.getName()
                                 +"::testConstructor"));
        assertEquals(obj.cls, StackInspector.Caller.class);
        assertEquals(obj.filtered, 0);

        StackInspector.walker.set(StackInspector.walkerShow);

        System.out.println("testConstructor: create: show reflect");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("create", How.class)
                             .invoke(null, How.CONSTRUCTOR));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             Constructor.class.getName()
                                 +"::newInstanceWithCaller",
                             Constructor.class.getName()
                                 +"::newInstance",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             Method.class.getName()
                                 +"::invoke",
                             ReflectionFrames.class.getName()
                                 +"::testConstructor"));
        assertEquals(obj.cls, StackInspector.Caller.class);
        assertNotEquals(obj.filtered, 0);

        System.out.println("testConstructor: reflect: show reflect");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("reflect", How.class)
                             .invoke(null, How.CONSTRUCTOR));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             Constructor.class.getName()
                                 +"::newInstanceWithCaller",
                             Constructor.class.getName()
                                 +"::newInstance",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             Method.class.getName()
                                 +"::invoke",
                             StackInspector.Caller.class.getName()
                                 +"::reflect",
                             Method.class.getName()
                                 +"::invoke",
                             ReflectionFrames.class.getName()
                                 +"::testConstructor"));
        assertEquals(obj.cls, StackInspector.Caller.class);
        assertNotEquals(obj.filtered, 0);

        System.out.println("testConstructor: handle: show reflect");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("handle", How.class)
                             .invoke(null, How.CONSTRUCTOR));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             Constructor.class.getName()
                                 +"::newInstanceWithCaller",
                             Constructor.class.getName()
                                 +"::newInstance",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             StackInspector.Caller.class.getName()
                                 +"::handle",
                             Method.class.getName()
                                 +"::invoke",
                             ReflectionFrames.class.getName()
                                 +"::testConstructor"));
        assertEquals(obj.cls, StackInspector.Caller.class);
        assertNotEquals(obj.filtered, 0);
    }

   /**
     * This test invokes StackInspector.class.newInstance() from
     * the caller StackInspector.Caller.create method. Because
     * Class.newInstance() is not considered as a
     * reflection frame, the caller returned by
     * getCallerClass() should appear to be java.lang.Class
     * and not StackInspector.Caller.
     * It also checks the expected frames collected
     * by walking the stack from the default StackInspector()
     * constructor.
     * This is done twice, once using a default StackWalker
     * that hides reflection frames, once using a StackWalker
     * configured to show reflection frames.
     */
    @Test
    public static void testNewInstance() throws Exception {
        StackInspector.walker.set(StackInspector.walkerHide);

        System.out.println("testNewInstance: create");

        StackInspector obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("create", How.class)
                             .invoke(null, How.CLASS));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             REFLECT_ACCESS.getName()
                                 +"::newInstance",
                             REFLECTION_FACTORY.getName()
                                 +"::newInstance",
                             Class.class.getName()
                                 +"::newInstance",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             ReflectionFrames.class.getName()
                                 +"::testNewInstance"));
        assertEquals(obj.cls, REFLECT_ACCESS);
        assertEquals(obj.filtered, 0);

        System.out.println("testNewInstance: reflect");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("reflect", How.class)
                             .invoke(null, How.CLASS));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             REFLECT_ACCESS.getName()
                                 +"::newInstance",
                             REFLECTION_FACTORY.getName()
                                 +"::newInstance",
                             Class.class.getName()
                                 +"::newInstance",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             StackInspector.Caller.class.getName()
                                 +"::reflect",
                             ReflectionFrames.class.getName()
                                 +"::testNewInstance"));

        assertEquals(obj.cls, REFLECT_ACCESS);
        assertEquals(obj.filtered, 0);

        System.out.println("testNewInstance: handle");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("handle", How.class)
                             .invoke(null, How.CLASS));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             REFLECT_ACCESS.getName()
                                 +"::newInstance",
                             REFLECTION_FACTORY.getName()
                                 +"::newInstance",
                             Class.class.getName()
                                 +"::newInstance",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             StackInspector.Caller.class.getName()
                                 +"::handle",
                             ReflectionFrames.class.getName()
                                 +"::testNewInstance"));

        assertEquals(obj.cls, REFLECT_ACCESS);
        assertEquals(obj.filtered, 0);

        StackInspector.walker.set(StackInspector.walkerShow);

        System.out.println("testNewInstance: create: show reflect");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("create", How.class)
                             .invoke(null, How.CLASS));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             Constructor.class.getName()
                                 +"::newInstanceWithCaller",
                             REFLECT_ACCESS.getName()
                                 +"::newInstance",
                             Class.class.getName()
                                 +"::newInstance",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             Method.class.getName()
                                 +"::invoke",
                             ReflectionFrames.class.getName()
                                 +"::testNewInstance"));
        assertEquals(obj.cls, REFLECT_ACCESS);
        assertNotEquals(obj.filtered, 0);

        System.out.println("testNewInstance: reflect: show reflect");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("reflect", How.class)
                             .invoke(null, How.CLASS));
        System.out.println(obj.collectedFrames);
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             Constructor.class.getName()
                                 +"::newInstanceWithCaller",
                             REFLECT_ACCESS.getName()
                                 +"::newInstance",
                             Class.class.getName()
                                 +"::newInstance",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             Method.class.getName()
                                 +"::invoke",
                             StackInspector.Caller.class.getName()
                                 +"::reflect",
                             Method.class.getName()
                                 +"::invoke",
                             ReflectionFrames.class.getName()
                                 +"::testNewInstance"));

        assertEquals(obj.cls, REFLECT_ACCESS);
        assertNotEquals(obj.filtered, 0);

        System.out.println("testNewInstance: handle: show reflect");

        obj = ((StackInspector)StackInspector.Caller.class
                             .getMethod("handle", How.class)
                             .invoke(null, How.CLASS));
        assertEquals(obj.collectedFrames,
                     List.of(StackInspector.class.getName()
                                 +"::<init>",
                             Constructor.class.getName()
                                 +"::newInstanceWithCaller",
                             REFLECT_ACCESS.getName()
                                 +"::newInstance",
                             Class.class.getName()
                                 +"::newInstance",
                             StackInspector.Caller.class.getName()
                                 +"::create",
                             StackInspector.Caller.class.getName()
                                 +"::handle",
                             Method.class.getName()
                                 +"::invoke",
                             ReflectionFrames.class.getName()
                                 +"::testNewInstance"));

        assertEquals(obj.cls, REFLECT_ACCESS);
        assertNotEquals(obj.filtered, 0);
    }

    @Test
    public static void testGetCaller() throws Exception {
        StackInspector.walker.set(StackInspector.walkerHide);

        assertEquals(StackInspector.getCaller(), ReflectionFrames.class);
        assertEquals(StackInspector.class.getMethod("getCaller").invoke(null),
                     ReflectionFrames.class);

        StackInspector.walker.set(StackInspector.walkerShow);

        assertEquals(StackInspector.getCaller(), ReflectionFrames.class);
        assertEquals(StackInspector.class.getMethod("getCaller").invoke(null),
                     ReflectionFrames.class);
    }

    @Test
    public static void testReflectCaller() throws Exception {
        StackInspector.walker.set(StackInspector.walkerHide);

        assertEquals(StackInspector.reflectCaller(), ReflectionFrames.class);
        assertEquals(StackInspector.class.getMethod("reflectCaller").invoke(null),
                     ReflectionFrames.class);

        StackInspector.walker.set(StackInspector.walkerShow);

        assertEquals(StackInspector.reflectCaller(), ReflectionFrames.class);
        assertEquals(StackInspector.class.getMethod("reflectCaller").invoke(null),
                     ReflectionFrames.class);
    }

    @Test
    public static void testSupplyCaller() throws Exception {
        StackInspector.walker.set(StackInspector.walkerHide);

        assertEquals(StackInspector.supplyCaller(), ReflectionFrames.class);
        assertEquals(StackInspector.class.getMethod("supplyCaller").invoke(null),
                     ReflectionFrames.class);

        StackInspector.walker.set(StackInspector.walkerShow);

        assertEquals(StackInspector.supplyCaller(), ReflectionFrames.class);
        assertEquals(StackInspector.class.getMethod("supplyCaller").invoke(null),
                     ReflectionFrames.class);
    }

    @Test
    public static void testHandleCaller() throws Exception {
        StackInspector.walker.set(StackInspector.walkerHide);

        assertEquals(StackInspector.handleCaller(), ReflectionFrames.class);
        assertEquals(StackInspector.class.getMethod("handleCaller").invoke(null),
                     ReflectionFrames.class);

        StackInspector.walker.set(StackInspector.walkerShow);

        assertEquals(StackInspector.handleCaller(), ReflectionFrames.class);
        assertEquals(StackInspector.class.getMethod("handleCaller").invoke(null),
                     ReflectionFrames.class);
    }

    static enum How { NEW, CONSTRUCTOR, CLASS};

    /**
     * An object that collect stack frames by walking the stack
     * (and calling getCallerClass()) from within its constructor.
     * For the purpose of this test, StackInspector objects are
     * always created from the nested StackInspector.Caller class,
     * which should therefore appear as the caller of the
     * StackInspector constructor.
     */
    static class StackInspector {
        static final StackWalker walkerHide =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        static final StackWalker walkerShow =
            StackWalker.getInstance(EnumSet.of(
                           StackWalker.Option.RETAIN_CLASS_REFERENCE,
                           StackWalker.Option.SHOW_REFLECT_FRAMES));
        final static ThreadLocal<StackWalker> walker = new ThreadLocal<>() {
             protected StackWalker initialValue() {
                 return walkerHide;
             }
        };

        List<String> collectedFrames;
        Class<?> cls = null;
        boolean stop;
        int filtered;
        final boolean filterImplFrames;

        public StackInspector() {
            stop = false;
            filterImplFrames = walker.get() == walkerShow;
            collectedFrames = walker.get().walk(this::parse);
            cls = walker.get().getCallerClass();
        }

        public List<String> collectedFrames() {
            return collectedFrames;
        }

        public boolean takeWhile(StackFrame f) {
            if (stop) return false;
            if (verbose) System.out.println("    " + f);
            stop = stop || f.getDeclaringClass() == ReflectionFrames.class;
            return true;
        }

        public boolean filter(StackFrame f) {
            if (filterImplFrames &&
                f.getClassName().startsWith("jdk.internal.reflect.")) {
                filtered++;
                return false;
            }
            if (!verbose) System.out.println("    " + f);
            return true;
        }

        public String frame(StackFrame f) {
            return f.getClassName() + "::" + f.getMethodName();
        }

        List<String> parse(Stream<StackFrame> s) {
            return s.takeWhile(this::takeWhile)
                    .filter(this::filter)
                    .map(this::frame)
                    .collect(Collectors.toList());
        }

        /**
         * The Caller class is used to create instances of
         * StackInspector, either direcltly, or throug reflection.
         */
        public static class Caller {
            public static StackInspector create(How how) throws Exception {
                switch(how) {
                    case NEW: return new StackInspector();
                    case CONSTRUCTOR: return StackInspector.class
                        .getConstructor().newInstance();
                    case CLASS: return StackInspector.class.newInstance();
                    default: throw new AssertionError(String.valueOf(how));
                }
            }
            public static StackInspector reflect(How how) throws Exception {
                return (StackInspector) Caller.class.getMethod("create", How.class)
                      .invoke(null, how);
            }
            public static StackInspector handle(How how) throws Exception {
                Lookup lookup = MethodHandles.lookup();
                MethodHandle mh = lookup.findStatic(Caller.class, "create",
                        MethodType.methodType(StackInspector.class, How.class));
                try {
                    return (StackInspector) mh.invoke(how);
                } catch (Error | Exception x) {
                    throw x;
                } catch(Throwable t) {
                    throw new AssertionError(t);
                }
            }
        }

        public static Class<?> getCaller() throws Exception {
            return walker.get().getCallerClass();
        }

        public static Class<?> reflectCaller() throws Exception {
            return (Class<?>)StackWalker.class.getMethod("getCallerClass")
                .invoke(walker.get());
        }

        public static Class<?> supplyCaller() throws Exception {
            return ((Supplier<Class<?>>)StackInspector.walker.get()::getCallerClass).get();
        }

        public static Class<?> handleCaller() throws Exception {
            Lookup lookup = MethodHandles.lookup();
            MethodHandle mh = lookup.findVirtual(StackWalker.class, "getCallerClass",
                    MethodType.methodType(Class.class));
            try {
                return (Class<?>) mh.invoke(walker.get());
            } catch (Error | Exception x) {
                throw x;
            } catch(Throwable t) {
                throw new AssertionError(t);
            }
        }
    }
}
