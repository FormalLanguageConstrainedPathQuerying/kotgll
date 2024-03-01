/*
 * Copyright (c) 2003, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.instrument;

import java.lang.instrument.UnmodifiableModuleException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.AccessibleObject;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import jdk.internal.module.Modules;
import jdk.internal.vm.annotation.IntrinsicCandidate;

/*
 * Copyright 2003 Wily Technology, Inc.
 */

/**
 * The Java side of the JPLIS implementation. Works in concert with a native JVMTI agent
 * to implement the JPLIS API set. Provides both the Java API implementation of
 * the Instrumentation interface and utility Java routines to support the native code.
 * Keeps a pointer to the native data structure in a scalar field to allow native
 * processing behind native methods.
 */
public class InstrumentationImpl implements Instrumentation {
    private static final String TRACE_USAGE_PROP_NAME = "jdk.instrument.traceUsage";
    private static final boolean TRACE_USAGE;
    static {
        PrivilegedAction<String> pa = () -> System.getProperty(TRACE_USAGE_PROP_NAME);
        @SuppressWarnings("removal")
        String s = AccessController.doPrivileged(pa);
        TRACE_USAGE = (s != null) && (s.isEmpty() || Boolean.parseBoolean(s));
    }

    private final     TransformerManager      mTransformerManager;
    private           TransformerManager      mRetransfomableTransformerManager;
    private final     long                    mNativeAgent;
    private final     boolean                 mEnvironmentSupportsRedefineClasses;
    private volatile  boolean                 mEnvironmentSupportsRetransformClassesKnown;
    private volatile  boolean                 mEnvironmentSupportsRetransformClasses;
    private final     boolean                 mEnvironmentSupportsNativeMethodPrefix;

    private
    InstrumentationImpl(long    nativeAgent,
                        boolean environmentSupportsRedefineClasses,
                        boolean environmentSupportsNativeMethodPrefix,
                        boolean printWarning) {
        mTransformerManager                    = new TransformerManager(false);
        mRetransfomableTransformerManager      = null;
        mNativeAgent                           = nativeAgent;
        mEnvironmentSupportsRedefineClasses    = environmentSupportsRedefineClasses;
        mEnvironmentSupportsRetransformClassesKnown = false; 
        mEnvironmentSupportsRetransformClasses = false;      
        mEnvironmentSupportsNativeMethodPrefix = environmentSupportsNativeMethodPrefix;

        if (printWarning) {
            String source = jarFile(nativeAgent);
            try {
                Path path = Path.of(source);
                PrivilegedAction<Path> pa = path::toAbsolutePath;
                @SuppressWarnings("removal")
                Path absolutePath = AccessController.doPrivileged(pa);
                source = absolutePath.toString();
            } catch (InvalidPathException e) {
            }

            StringBuilder sb = new StringBuilder();
            sb.append("WARNING: A Java agent has been loaded dynamically (")
                    .append(source)
                    .append(")")
                    .append(System.lineSeparator());
            sb.append("WARNING: If a serviceability tool is in use, please run with"
                            + " -XX:+EnableDynamicAgentLoading to hide this warning")
                    .append(System.lineSeparator());
            if (!TRACE_USAGE) {
                sb.append("WARNING: If a serviceability tool is not in use, please run with"
                                + " -D" + TRACE_USAGE_PROP_NAME + " for more information")
                        .append(System.lineSeparator());
            }
            sb.append("WARNING: Dynamic loading of agents will be disallowed by default in a future release");
            String warningMessage = sb.toString();
            System.err.println(warningMessage);
        }
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
        addTransformer(transformer, false);
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        trace("addTransformer");
        if (transformer == null) {
            throw new NullPointerException("null passed as 'transformer' in addTransformer");
        }
        synchronized (this) {
            if (canRetransform) {
                if (!isRetransformClassesSupported()) {
                    throw new UnsupportedOperationException(
                        "adding retransformable transformers is not supported in this environment");
                }
                if (mRetransfomableTransformerManager == null) {
                    mRetransfomableTransformerManager = new TransformerManager(true);
                }
                mRetransfomableTransformerManager.addTransformer(transformer);
                if (mRetransfomableTransformerManager.getTransformerCount() == 1) {
                    setHasRetransformableTransformers(mNativeAgent, true);
                }
            } else {
                mTransformerManager.addTransformer(transformer);
                if (mTransformerManager.getTransformerCount() == 1) {
                    setHasTransformers(mNativeAgent, true);
                }
            }
        }
    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer) {
        trace("removeTransformer");
        if (transformer == null) {
            throw new NullPointerException("null passed as 'transformer' in removeTransformer");
        }
        synchronized (this) {
            TransformerManager mgr = findTransformerManager(transformer);
            if (mgr != null) {
                mgr.removeTransformer(transformer);
                if (mgr.getTransformerCount() == 0) {
                    if (mgr.isRetransformable()) {
                        setHasRetransformableTransformers(mNativeAgent, false);
                    } else {
                        setHasTransformers(mNativeAgent, false);
                    }
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean isModifiableClass(Class<?> theClass) {
        trace("isModifiableClass");
        if (theClass == null) {
            throw new NullPointerException(
                         "null passed as 'theClass' in isModifiableClass");
        }
        return isModifiableClass0(mNativeAgent, theClass);
    }

    @Override
    public boolean isModifiableModule(Module module) {
        trace("isModifiableModule");
        if (module == null) {
            throw new NullPointerException("'module' is null");
        }
        return true;
    }

    @Override
    public boolean isRetransformClassesSupported() {
        trace("isRetransformClassesSupported");
        if (!mEnvironmentSupportsRetransformClassesKnown) {
            mEnvironmentSupportsRetransformClasses = isRetransformClassesSupported0(mNativeAgent);
            mEnvironmentSupportsRetransformClassesKnown = true;
        }
        return mEnvironmentSupportsRetransformClasses;
    }

    @Override
    public void retransformClasses(Class<?>... classes) {
        trace("retransformClasses");
        if (!isRetransformClassesSupported()) {
            throw new UnsupportedOperationException(
              "retransformClasses is not supported in this environment");
        }
        if (classes.length == 0) {
            return; 
        }
        retransformClasses0(mNativeAgent, classes);
    }

    @Override
    public boolean isRedefineClassesSupported() {
        trace("isRedefineClassesSupported");
        return mEnvironmentSupportsRedefineClasses;
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException {
        trace("retransformClasses");
        if (!isRedefineClassesSupported()) {
            throw new UnsupportedOperationException("redefineClasses is not supported in this environment");
        }
        if (definitions == null) {
            throw new NullPointerException("null passed as 'definitions' in redefineClasses");
        }
        for (int i = 0; i < definitions.length; ++i) {
            if (definitions[i] == null) {
                throw new NullPointerException("element of 'definitions' is null in redefineClasses");
            }
        }
        if (definitions.length == 0) {
            return; 
        }
        redefineClasses0(mNativeAgent, definitions);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class[] getAllLoadedClasses() {
        trace("getAllLoadedClasses");
        return getAllLoadedClasses0(mNativeAgent);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class[] getInitiatedClasses(ClassLoader loader) {
        trace("getInitiatedClasses");
        return getInitiatedClasses0(mNativeAgent, loader);
    }

    @Override
    public long getObjectSize(Object objectToSize) {
        trace("getObjectSize");
        if (objectToSize == null) {
            throw new NullPointerException("null passed as 'objectToSize' in getObjectSize");
        }
        return getObjectSize0(mNativeAgent, objectToSize);
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
        trace("appendToBootstrapClassLoaderSearch");
        appendToClassLoaderSearch0(mNativeAgent, jarfile.getName(), true);
    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarfile) {
        trace("appendToSystemClassLoaderSearch");
        appendToClassLoaderSearch0(mNativeAgent, jarfile.getName(), false);
    }

    @Override
    public boolean isNativeMethodPrefixSupported() {
        trace("isNativeMethodPrefixSupported");
        return mEnvironmentSupportsNativeMethodPrefix;
    }

    @Override
    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
        trace("setNativeMethodPrefix");
        if (!isNativeMethodPrefixSupported()) {
            throw new UnsupportedOperationException(
                   "setNativeMethodPrefix is not supported in this environment");
        }
        if (transformer == null) {
            throw new NullPointerException(
                       "null passed as 'transformer' in setNativeMethodPrefix");
        }
        synchronized (this) {
            TransformerManager mgr = findTransformerManager(transformer);
            if (mgr == null) {
                throw new IllegalArgumentException(
                        "transformer not registered in setNativeMethodPrefix");
            }
            mgr.setNativeMethodPrefix(transformer, prefix);
            String[] prefixes = mgr.getNativeMethodPrefixes();
            setNativeMethodPrefixes(mNativeAgent, prefixes, mgr.isRetransformable());
        }
    }

    @Override
    public void redefineModule(Module module,
                               Set<Module> extraReads,
                               Map<String, Set<Module>> extraExports,
                               Map<String, Set<Module>> extraOpens,
                               Set<Class<?>> extraUses,
                               Map<Class<?>, List<Class<?>>> extraProvides)
    {
        trace("redefineModule");

        if (!module.isNamed())
            return;

        if (!isModifiableModule(module))
            throw new UnmodifiableModuleException(module.getName());

        extraReads = new HashSet<>(extraReads);
        if (extraReads.contains(null))
            throw new NullPointerException("'extraReads' contains null");

        extraExports = cloneAndCheckMap(module, extraExports);
        extraOpens = cloneAndCheckMap(module, extraOpens);

        extraUses = new HashSet<>(extraUses);
        if (extraUses.contains(null))
            throw new NullPointerException("'extraUses' contains null");

        Map<Class<?>, List<Class<?>>> tmpProvides = new HashMap<>();
        for (Map.Entry<Class<?>, List<Class<?>>> e : extraProvides.entrySet()) {
            Class<?> service = e.getKey();
            if (service == null)
                throw new NullPointerException("'extraProvides' contains null");
            List<Class<?>> providers = new ArrayList<>(e.getValue());
            if (providers.isEmpty())
                throw new IllegalArgumentException("list of providers is empty");
            providers.forEach(p -> {
                if (p.getModule() != module)
                    throw new IllegalArgumentException(p + " not in " + module);
                if (!service.isAssignableFrom(p))
                    throw new IllegalArgumentException(p + " is not a " + service);
            });
            tmpProvides.put(service, providers);
        }
        extraProvides = tmpProvides;

        extraReads.forEach(m -> Modules.addReads(module, m));

        for (Map.Entry<String, Set<Module>> e : extraExports.entrySet()) {
            String pkg = e.getKey();
            Set<Module> targets = e.getValue();
            targets.forEach(m -> Modules.addExports(module, pkg, m));
        }

        for (Map.Entry<String, Set<Module>> e : extraOpens.entrySet()) {
            String pkg = e.getKey();
            Set<Module> targets = e.getValue();
            targets.forEach(m -> Modules.addOpens(module, pkg, m));
        }

        extraUses.forEach(service -> Modules.addUses(module, service));

        for (Map.Entry<Class<?>, List<Class<?>>> e : extraProvides.entrySet()) {
            Class<?> service = e.getKey();
            List<Class<?>> providers = e.getValue();
            providers.forEach(p -> Modules.addProvides(module, service, p));
        }
    }

    private Map<String, Set<Module>>
        cloneAndCheckMap(Module module, Map<String, Set<Module>> map)
    {
        if (map.isEmpty())
            return Collections.emptyMap();

        Map<String, Set<Module>> result = new HashMap<>();
        Set<String> packages = module.getPackages();
        for (Map.Entry<String, Set<Module>> e : map.entrySet()) {
            String pkg = e.getKey();
            if (pkg == null)
                throw new NullPointerException("package cannot be null");
            if (!packages.contains(pkg))
                throw new IllegalArgumentException(pkg + " not in module");
            Set<Module> targets = new HashSet<>(e.getValue());
            if (targets.isEmpty())
                throw new IllegalArgumentException("set of targets is empty");
            if (targets.contains(null))
                throw new NullPointerException("set of targets cannot include null");
            result.put(pkg, targets);
        }
        return result;
    }


    private TransformerManager findTransformerManager(ClassFileTransformer transformer) {
        assert Thread.holdsLock(this);
        if (mTransformerManager.includesTransformer(transformer)) {
            return mTransformerManager;
        }
        if (mRetransfomableTransformerManager != null &&
                mRetransfomableTransformerManager.includesTransformer(transformer)) {
            return mRetransfomableTransformerManager;
        }
        return null;
    }


    /*
     *  Natives
     */
    private native
    String jarFile(long nativeAgent);

    private native boolean
    isModifiableClass0(long nativeAgent, Class<?> theClass);

    private native boolean
    isRetransformClassesSupported0(long nativeAgent);

    private native void
    setHasTransformers(long nativeAgent, boolean has);

    private native void
    setHasRetransformableTransformers(long nativeAgent, boolean has);

    private native void
    retransformClasses0(long nativeAgent, Class<?>[] classes);

    private native void
    redefineClasses0(long nativeAgent, ClassDefinition[]  definitions)
        throws  ClassNotFoundException;

    @SuppressWarnings("rawtypes")
    private native Class[]
    getAllLoadedClasses0(long nativeAgent);

    @SuppressWarnings("rawtypes")
    private native Class[]
    getInitiatedClasses0(long nativeAgent, ClassLoader loader);

    @IntrinsicCandidate
    private native long
    getObjectSize0(long nativeAgent, Object objectToSize);

    private native void
    appendToClassLoaderSearch0(long nativeAgent, String jarfile, boolean bootLoader);

    private native void
    setNativeMethodPrefixes(long nativeAgent, String[] prefixes, boolean isRetransformable);

    static {
        System.loadLibrary("instrument");
    }

    /*
     *  Internals
     */


    @SuppressWarnings("removal")
    private static void setAccessible(final AccessibleObject ao, final boolean accessible) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    ao.setAccessible(accessible);
                    return null;
                }});
    }

    private void
    loadClassAndStartAgent( String  classname,
                            String  methodname,
                            String  optionsString)
            throws Throwable {

        ClassLoader mainAppLoader   = ClassLoader.getSystemClassLoader();
        Class<?>    javaAgentClass  = mainAppLoader.loadClass(classname);

        Method m = null;
        NoSuchMethodException firstExc = null;
        boolean twoArgAgent = false;


        try {
            m = javaAgentClass.getDeclaredMethod( methodname,
                                 new Class<?>[] {
                                     String.class,
                                     java.lang.instrument.Instrumentation.class
                                 }
                               );
            twoArgAgent = true;
        } catch (NoSuchMethodException x) {
            firstExc = x;
        }

        if (m == null) {
            try {
                m = javaAgentClass.getDeclaredMethod(methodname,
                                                 new Class<?>[] { String.class });
            } catch (NoSuchMethodException x) {
                throw firstExc;
            }
        }

        if (!Modifier.isPublic(m.getModifiers())) {
            String msg = "method " + classname + "." +  methodname + " must be declared public";
            throw new IllegalAccessException(msg);
        }

        if (!Modifier.isPublic(javaAgentClass.getModifiers()) &&
            !javaAgentClass.getModule().isNamed()) {
            setAccessible(m, true);
        }

        if (twoArgAgent) {
            m.invoke(null, new Object[] { optionsString, this });
        } else {
            m.invoke(null, new Object[] { optionsString });
        }
    }

    private void
    loadClassAndCallPremain(    String  classname,
                                String  optionsString)
            throws Throwable {

        loadClassAndStartAgent( classname, "premain", optionsString );
    }


    private void
    loadClassAndCallAgentmain(  String  classname,
                                String  optionsString)
            throws Throwable {

        loadClassAndStartAgent( classname, "agentmain", optionsString );
    }

    private byte[]
    transform(  Module              module,
                ClassLoader         loader,
                String              classname,
                Class<?>            classBeingRedefined,
                ProtectionDomain    protectionDomain,
                byte[]              classfileBuffer,
                boolean             isRetransformer) {
        TransformerManager mgr = isRetransformer?
                                        mRetransfomableTransformerManager :
                                        mTransformerManager;
        if (module == null) {
            if (classBeingRedefined != null) {
                module = classBeingRedefined.getModule();
            } else {
                module = (loader == null) ? jdk.internal.loader.BootLoader.getUnnamedModule()
                                          : loader.getUnnamedModule();
            }
        }
        if (mgr == null) {
            return null; 
        } else {
            return mgr.transform(   module,
                                    loader,
                                    classname,
                                    classBeingRedefined,
                                    protectionDomain,
                                    classfileBuffer);
        }
    }


    /**
     * Invoked by the java launcher to load a java agent that is packaged with
     * the main application in an executable JAR file.
     */
    public static void loadAgent(String path) {
        loadAgent0(path);
    }

    private static native void loadAgent0(String path);

    /**
     * Prints a trace message and stack trace when tracing is enabled.
     */
    private void trace(String methodName) {
        if (!TRACE_USAGE) return;

        List<StackWalker.StackFrame> stack = HolderStackWalker.walker.walk(s ->
            s.dropWhile(f -> f.getDeclaringClass().getModule() == Instrumentation.class.getModule())
                .collect(Collectors.toList())
        );

        if (stack.size() > 0) {
            Class<?> callerClass = stack.get(0).getDeclaringClass();
            URL callerUrl = codeSource(callerClass);
            String source;
            if (callerUrl == null) {
                source = callerClass.getName();
            } else {
                source = callerClass.getName() + " (" + callerUrl + ")";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("java.lang.instrument.Instrumentation.")
                    .append(methodName)
                    .append(" has been called by ")
                    .append(source);
            stack.forEach(f -> sb.append(System.lineSeparator()).append("\tat " + f));
            String traceMessage = sb.toString();
            System.out.println(traceMessage);
        }
    }

    /**
     * Returns the possibly-bnull code source of the given class.
     */
    private static URL codeSource(Class<?> clazz) {
        PrivilegedAction<ProtectionDomain> pa = clazz::getProtectionDomain;
        @SuppressWarnings("removal")
        CodeSource cs = AccessController.doPrivileged(pa).getCodeSource();
        return (cs != null) ? cs.getLocation() : null;
    }

    /**
     * Holder for StackWalker object.
     */
    private static class HolderStackWalker {
        static final StackWalker walker;
        static {
            PrivilegedAction<StackWalker> pa = () ->
                    StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            @SuppressWarnings("removal")
            StackWalker w = AccessController.doPrivileged(pa);
            walker = w;
        }
    }
}
