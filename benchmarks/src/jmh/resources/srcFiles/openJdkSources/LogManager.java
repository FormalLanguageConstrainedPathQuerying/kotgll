/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates. All rights reserved.
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

package java.util.logging;

import java.io.*;
import java.util.*;
import java.security.*;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Paths;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.internal.access.JavaAWTAccess;
import jdk.internal.access.SharedSecrets;
import sun.util.logging.internal.LoggingProviderImpl;
import static jdk.internal.logger.DefaultLoggerFinder.isSystem;

/**
 * There is a single global LogManager object that is used to
 * maintain a set of shared state about Loggers and log services.
 * <p>
 * This LogManager object:
 * <ul>
 * <li> Manages a hierarchical namespace of Logger objects.  All
 *      named Loggers are stored in this namespace.
 * <li> Manages a set of logging control properties.  These are
 *      simple key-value pairs that can be used by Handlers and
 *      other logging objects to configure themselves.
 * </ul>
 * <p>
 * The global LogManager object can be retrieved using LogManager.getLogManager().
 * The LogManager object is created during class initialization and
 * cannot subsequently be changed.
 * <p>
 * At startup the LogManager class is located using the
 * java.util.logging.manager system property.
 *
 * <h2>LogManager Configuration</h2>
 *
 * A LogManager initializes the logging configuration via
 * the {@link #readConfiguration()} method during LogManager initialization.
 * By default, LogManager default configuration is used.
 * The logging configuration read by LogManager must be in the
 * {@linkplain Properties properties file} format.
 * <p>
 * The LogManager defines two optional system properties that allow control over
 * the initial configuration, as specified in the {@link #readConfiguration()}
 * method:
 * <ul>
 * <li>{@systemProperty java.util.logging.config.class}
 * <li>{@systemProperty java.util.logging.config.file}
 * </ul>
 * <p>
 * These two system properties may be specified on the command line to the "java"
 * command, or as system property definitions passed to JNI_CreateJavaVM.
 * <p>
 * The {@linkplain Properties properties} for loggers and Handlers will have
 * names starting with the dot-separated name for the handler or logger.<br>
 * The global logging properties may include:
 * <ul>
 * <li>A property "handlers".  This defines a whitespace or comma separated
 * list of class names for handler classes to load and register as
 * handlers on the root Logger (the Logger named "").  Each class
 * name must be for a Handler class which has a default constructor.
 * Note that these Handlers may be created lazily, when they are
 * first used.
 *
 * <li>A property "&lt;logger&gt;.handlers". This defines a whitespace or
 * comma separated list of class names for handlers classes to
 * load and register as handlers to the specified logger. Each class
 * name must be for a Handler class which has a default constructor.
 * Note that these Handlers may be created lazily, when they are
 * first used.
 *
 * <li>A property "&lt;logger&gt;.handlers.ensureCloseOnReset". This defines a
 * a boolean value. If "&lt;logger&gt;.handlers" is not defined or is empty,
 * this property is ignored. Otherwise it defaults to {@code true}. When the
 * value is {@code true}, the handlers associated with the logger are guaranteed
 * to be closed on {@linkplain #reset} and shutdown. This can be turned off
 * by explicitly setting "&lt;logger&gt;.handlers.ensureCloseOnReset=false" in
 * the configuration. Note that turning this property off causes the risk of
 * introducing a resource leak, as the logger may get garbage collected before
 * {@code reset()} is called, thus preventing its handlers from being closed
 * on {@code reset()}. In that case it is the responsibility of the application
 * to ensure that the handlers are closed before the logger is garbage
 * collected.
 *
 * <li>A property "&lt;logger&gt;.useParentHandlers". This defines a boolean
 * value. By default every logger calls its parent in addition to
 * handling the logging message itself, this often result in messages
 * being handled by the root logger as well. When setting this property
 * to false a Handler needs to be configured for this logger otherwise
 * no logging messages are delivered.
 *
 * <li>A property "config".  This property is intended to allow
 * arbitrary configuration code to be run.  The property defines a
 * whitespace or comma separated list of class names.  A new instance will be
 * created for each named class.  The default constructor of each class
 * may execute arbitrary code to update the logging configuration, such as
 * setting logger levels, adding handlers, adding filters, etc.
 * </ul>
 * <p>
 * Note that all classes loaded during LogManager configuration are
 * first searched on the system class path before any user class path.
 * That includes the LogManager class, any config classes, and any
 * handler classes.
 * <p>
 * Loggers are organized into a naming hierarchy based on their
 * dot separated names.  Thus "a.b.c" is a child of "a.b", but
 * "a.b1" and a.b2" are peers.
 * <p>
 * All properties whose names end with ".level" are assumed to define
 * log levels for Loggers.  Thus "foo.level" defines a log level for
 * the logger called "foo" and (recursively) for any of its children
 * in the naming hierarchy.  Log Levels are applied in the order they
 * are defined in the properties file.  Thus level settings for child
 * nodes in the tree should come after settings for their parents.
 * The property name ".level" can be used to set the level for the
 * root of the tree.
 * <p>
 * All methods on the LogManager object are multi-thread safe.
 *
 * @since 1.4
*/

public class LogManager {

    private volatile Properties props = new Properties();
    private static final Level defaultLevel = Level.INFO;

    private final LoggerContext systemContext = new SystemLoggerContext();
    private final LoggerContext userContext = new LoggerContext();
    private volatile Logger rootLogger;
    private volatile boolean readPrimordialConfiguration;
    private static final int
            STATE_INITIALIZED = 0, 
            STATE_INITIALIZING = 1,
            STATE_READING_CONFIG = 2,
            STATE_UNINITIALIZED = 3,
            STATE_SHUTDOWN = 4;    
    private volatile int globalHandlersState; 
    private final ReentrantLock configurationLock = new ReentrantLock();

    private static final class CloseOnReset {
        private final Logger logger;
        private CloseOnReset(Logger ref) {
            this.logger = Objects.requireNonNull(ref);
        }
        @Override
        public boolean equals(Object other) {
            return (other instanceof CloseOnReset) && ((CloseOnReset)other).logger == logger;
        }
        @Override
        public int hashCode() {
            return System.identityHashCode(logger);
        }
        public Logger get() {
            return logger;
        }
        public static CloseOnReset create(Logger logger) {
            return new CloseOnReset(logger);
        }
    }
    private final CopyOnWriteArrayList<CloseOnReset> closeOnResetLoggers =
            new CopyOnWriteArrayList<>();


    private final Map<Object, Runnable> listeners =
            Collections.synchronizedMap(new IdentityHashMap<>());

    @SuppressWarnings("removal")
    private static final LogManager manager = AccessController.doPrivileged(
            new PrivilegedAction<LogManager>() {
                @Override
                public LogManager run() {
                    LogManager mgr = null;
                    String cname = null;
                    try {
                        cname = System.getProperty("java.util.logging.manager");
                        if (cname != null) {
                            try {
                                @SuppressWarnings("deprecation")
                                Object tmp = ClassLoader.getSystemClassLoader()
                                        .loadClass(cname).newInstance();
                                mgr = (LogManager) tmp;
                            } catch (ClassNotFoundException ex) {
                                @SuppressWarnings("deprecation")
                                Object tmp = Thread.currentThread()
                                        .getContextClassLoader().loadClass(cname).newInstance();
                                mgr = (LogManager) tmp;
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("Could not load Logmanager \"" + cname + "\"");
                        ex.printStackTrace();
                    }
                    if (mgr == null) {
                        mgr = new LogManager();
                    }
                    return mgr;

                }
            });

    private class Cleaner extends Thread {

        private Cleaner() {
            super(null, null, "Logging-Cleaner", 0, false);
            /* Set context class loader to null in order to avoid
             * keeping a strong reference to an application classloader.
             */
            this.setContextClassLoader(null);
        }

        @Override
        public void run() {
            LogManager mgr = manager;

            configurationLock.lock();
            globalHandlersState = STATE_SHUTDOWN;
            configurationLock.unlock();

            reset();
        }
    }


    /**
     * Protected constructor.  This is protected so that container applications
     * (such as J2EE containers) can subclass the object.  It is non-public as
     * it is intended that there only be one LogManager object, whose value is
     * retrieved by calling LogManager.getLogManager.
     */
    protected LogManager() {
        this(checkSubclassPermissions());
    }

    private LogManager(Void checked) {

        try {
            Runtime.getRuntime().addShutdownHook(new Cleaner());
        } catch (IllegalStateException e) {
        }
    }

    private static Void checkSubclassPermissions() {
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("shutdownHooks"));
            sm.checkPermission(new RuntimePermission("setContextClassLoader"));
        }
        return null;
    }

    /**
     * Lazy initialization: if this instance of manager is the global
     * manager then this method will read the initial configuration and
     * add the root logger and global logger by calling addLogger().
     *
     * Note that it is subtly different from what we do in LoggerContext.
     * In LoggerContext we're patching up the logger context tree in order to add
     * the root and global logger *to the context tree*.
     *
     * For this to work, addLogger() must have already have been called
     * once on the LogManager instance for the default logger being
     * added.
     *
     * This is why ensureLogManagerInitialized() needs to be called before
     * any logger is added to any logger context.
     *
     */
    private boolean initializedCalled = false;
    private volatile boolean initializationDone = false;
    @SuppressWarnings("removal")
    final void ensureLogManagerInitialized() {
        final LogManager owner = this;
        if (initializationDone || owner != manager) {
            return;
        }

        configurationLock.lock();
        try {
            final boolean isRecursiveInitialization = (initializedCalled == true);

            assert initializedCalled || !initializationDone
                    : "Initialization can't be done if initialized has not been called!";

            if (isRecursiveInitialization || initializationDone) {
                return;
            }
            initializedCalled = true;
            try {
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        assert rootLogger == null;
                        assert initializedCalled && !initializationDone;

                        final Logger root = owner.rootLogger = owner.new RootLogger();

                        owner.readPrimordialConfiguration();

                        owner.addLogger(root);

                        if (!root.isLevelInitialized()) {
                            root.setLevel(defaultLevel);
                        }

                        @SuppressWarnings("deprecation")
                        final Logger global = Logger.global;

                        owner.addLogger(global);
                        return null;
                    }
                });
            } finally {
                initializationDone = true;
            }
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Returns the global LogManager object.
     * @return the global LogManager object
     */
    public static LogManager getLogManager() {
        if (manager != null) {
            manager.ensureLogManagerInitialized();
        }
        return manager;
    }

    private void readPrimordialConfiguration() { 
        if (!readPrimordialConfiguration) {
            if (System.out == null) {
                return;
            }
            readPrimordialConfiguration = true;
            try {
                readConfiguration();

                jdk.internal.logger.BootstrapLogger.redirectTemporaryLoggers();

            } catch (Exception ex) {
                assert false : "Exception raised while reading logging configuration: " + ex;
            }
        }
    }

    private WeakHashMap<Object, LoggerContext> contextsMap = null;

    private LoggerContext getUserContext() {
        LoggerContext context = null;

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        JavaAWTAccess javaAwtAccess = SharedSecrets.getJavaAWTAccess();
        if (sm != null && javaAwtAccess != null) {
            final Object ecx = javaAwtAccess.getAppletContext();
            if (ecx != null) {
                synchronized (javaAwtAccess) {
                    if (contextsMap == null) {
                        contextsMap = new WeakHashMap<>();
                    }
                    context = contextsMap.get(ecx);
                    if (context == null) {
                        context = new LoggerContext();
                        contextsMap.put(ecx, context);
                    }
                }
            }
        }
        return context != null ? context : userContext;
    }

    final LoggerContext getSystemContext() {
        return systemContext;
    }

    private List<LoggerContext> contexts() {
        List<LoggerContext> cxs = new ArrayList<>();
        cxs.add(getSystemContext());
        cxs.add(getUserContext());
        return cxs;
    }

    Logger demandLogger(String name, String resourceBundleName, Class<?> caller) {
        final Module module = caller == null ? null : caller.getModule();
        return demandLogger(name, resourceBundleName, module);
    }

    Logger demandLogger(String name, String resourceBundleName, Module module) {
        Logger result = getLogger(name);
        if (result == null) {
            Logger newLogger = new Logger(name, resourceBundleName,
                                          module, this, false);
            do {
                if (addLogger(newLogger)) {
                    return newLogger;
                }

                result = getLogger(name);
            } while (result == null);
        }
        return result;
    }

    Logger demandSystemLogger(String name, String resourceBundleName, Class<?> caller) {
        final Module module = caller == null ? null : caller.getModule();
        return demandSystemLogger(name, resourceBundleName, module);
    }

    @SuppressWarnings("removal")
    Logger demandSystemLogger(String name, String resourceBundleName, Module module) {
        final Logger sysLogger = getSystemContext()
                .demandLogger(name, resourceBundleName, module);

        Logger logger;
        do {
            if (addLogger(sysLogger)) {
                logger = sysLogger;
            } else {
                logger = getLogger(name);
            }
        } while (logger == null);

        if (logger != sysLogger) {
            final Logger l = logger;
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    l.mergeWithSystemLogger(sysLogger);
                    return null;
                }
            });
        }
        return sysLogger;
    }

    class LoggerContext {
        private final ConcurrentHashMap<String,LoggerWeakRef> namedLoggers =
                new ConcurrentHashMap<>();
        private final LogNode root;
        private LoggerContext() {
            this.root = new LogNode(null, this);
        }


        final boolean requiresDefaultLoggers() {
            final boolean requiresDefaultLoggers = (getOwner() == manager);
            if (requiresDefaultLoggers) {
                getOwner().ensureLogManagerInitialized();
            }
            return requiresDefaultLoggers;
        }

        final LogManager getOwner() {
            return LogManager.this;
        }

        final Logger getRootLogger() {
            return getOwner().rootLogger;
        }

        final Logger getGlobalLogger() {
            @SuppressWarnings("deprecation") 
            final Logger global = Logger.global;
            return global;
        }

        Logger demandLogger(String name, String resourceBundleName, Module module) {
            final LogManager owner = getOwner();
            return owner.demandLogger(name, resourceBundleName, module);
        }


        private void ensureInitialized() {
            if (requiresDefaultLoggers()) {
                ensureDefaultLogger(getRootLogger());
                ensureDefaultLogger(getGlobalLogger());
            }
        }


        Logger findLogger(String name) {
            LoggerWeakRef ref = namedLoggers.get(name);
            Logger logger = ref == null ? null : ref.get();

            if (logger != null || (ref == null && !name.isEmpty()
                    && !name.equals(Logger.GLOBAL_LOGGER_NAME))) {
                return logger;
            }

            synchronized(this) {
                ensureInitialized();
                ref = namedLoggers.get(name);
                if (ref == null) {
                    return null;
                }
                logger = ref.get();
                if (logger == null) {
                    ref.dispose();
                }
                return logger;
            }
        }

        private void ensureAllDefaultLoggers(Logger logger) {
            if (requiresDefaultLoggers()) {
                final String name = logger.getName();
                if (!name.isEmpty()) {
                    ensureDefaultLogger(getRootLogger());
                    if (!Logger.GLOBAL_LOGGER_NAME.equals(name)) {
                        ensureDefaultLogger(getGlobalLogger());
                    }
                }
            }
        }

        private void ensureDefaultLogger(Logger logger) {

            if (!requiresDefaultLoggers() || logger == null
                    || logger != getGlobalLogger() && logger != LogManager.this.rootLogger ) {

                assert logger == null;

                return;
            }

            if (!namedLoggers.containsKey(logger.getName())) {
                addLocalLogger(logger, false);
            }
        }

        boolean addLocalLogger(Logger logger) {
            return addLocalLogger(logger, requiresDefaultLoggers());
        }

        synchronized boolean addLocalLogger(Logger logger, boolean addDefaultLoggersIfNeeded) {
            if (addDefaultLoggersIfNeeded) {
                ensureAllDefaultLoggers(logger);
            }

            final String name = logger.getName();
            if (name == null) {
                throw new NullPointerException();
            }
            LoggerWeakRef ref = namedLoggers.get(name);
            if (ref != null) {
                if (ref.refersTo(null)) {
                    ref.dispose();
                } else {
                    return false;
                }
            }

            final LogManager owner = getOwner();
            logger.setLogManager(owner);
            ref = owner.new LoggerWeakRef(logger);

            Level level = owner.getLevelProperty(name + ".level", null);
            if (level != null && !logger.isLevelInitialized()) {
                doSetLevel(logger, level);
            }

            processParentHandlers(logger, name, VisitedLoggers.NEVER);

            LogNode node = getNode(name);
            node.loggerRef = ref;
            Logger parent = null;
            LogNode nodep = node.parent;
            while (nodep != null) {
                LoggerWeakRef nodeRef = nodep.loggerRef;
                if (nodeRef != null) {
                    parent = nodeRef.get();
                    if (parent != null) {
                        break;
                    }
                }
                nodep = nodep.parent;
            }

            if (parent != null) {
                doSetParent(logger, parent);
            }
            node.walkAndSetParent(logger);
            ref.setNode(node);

            namedLoggers.put(name, ref);
            return true;
        }

        void removeLoggerRef(String name, LoggerWeakRef ref) {
            namedLoggers.remove(name, ref);
        }

        synchronized Enumeration<String> getLoggerNames() {
            ensureInitialized();
            return Collections.enumeration(namedLoggers.keySet());
        }

        @SuppressWarnings("removal")
        private void processParentHandlers(final Logger logger, final String name,
               Predicate<Logger> visited) {
            final LogManager owner = getOwner();
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    if (logger != owner.rootLogger) {
                        boolean useParent = owner.getBooleanProperty(name + ".useParentHandlers", true);
                        if (!useParent) {
                            logger.setUseParentHandlers(false);
                        }
                    }
                    return null;
                }
            });

            int ix = 1;
            for (;;) {
                int ix2 = name.indexOf('.', ix);
                if (ix2 < 0) {
                    break;
                }
                String pname = name.substring(0, ix2);
                if (owner.getProperty(pname + ".level") != null ||
                    owner.getProperty(pname + ".handlers") != null) {
                    if (visited.test(demandLogger(pname, null, null))) {
                        break;
                    }
                }
                ix = ix2+1;
            }
        }

        LogNode getNode(String name) {
            if (name == null || name.isEmpty()) {
                return root;
            }
            LogNode node = root;
            while (name.length() > 0) {
                int ix = name.indexOf('.');
                String head;
                if (ix > 0) {
                    head = name.substring(0, ix);
                    name = name.substring(ix + 1);
                } else {
                    head = name;
                    name = "";
                }
                if (node.children == null) {
                    node.children = new HashMap<>();
                }
                LogNode child = node.children.get(head);
                if (child == null) {
                    child = new LogNode(node, this);
                    node.children.put(head, child);
                }
                node = child;
            }
            return node;
        }
    }

    final class SystemLoggerContext extends LoggerContext {
        @Override
        Logger demandLogger(String name, String resourceBundleName,
                            Module module) {
            Logger result = findLogger(name);
            if (result == null) {
                Logger newLogger = new Logger(name, resourceBundleName,
                                              module, getOwner(), true);
                do {
                    if (addLocalLogger(newLogger)) {
                        result = newLogger;
                    } else {
                        result = findLogger(name);
                    }
                } while (result == null);
            }
            return result;
        }
    }

    @SuppressWarnings("removal")
    private void loadLoggerHandlers(final Logger logger, final String name,
                                    final String handlersPropertyName)
    {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                setLoggerHandlers(logger, name, handlersPropertyName,
                    createLoggerHandlers(name, handlersPropertyName));
                return null;
            }
        });
    }

    private void setLoggerHandlers(final Logger logger, final String name,
                                   final String handlersPropertyName,
                                   List<Handler> handlers)
    {
        final boolean ensureCloseOnReset = ! handlers.isEmpty()
                    && getBooleanProperty(handlersPropertyName + ".ensureCloseOnReset",true);
        int count = 0;
        for (Handler hdl : handlers) {
            logger.addHandler(hdl);
            if (++count == 1 && ensureCloseOnReset) {
                closeOnResetLoggers.addIfAbsent(CloseOnReset.create(logger));
            }
        }
    }

    private List<Handler> createLoggerHandlers(final String name,
                                               final String handlersPropertyName)
    {
        String names[] = parseClassNames(handlersPropertyName);
        List<Handler> handlers = new ArrayList<>(names.length);
        for (String type : names) {
            try {
                @SuppressWarnings("deprecation")
                Object o = ClassLoader.getSystemClassLoader().loadClass(type).newInstance();
                Handler hdl = (Handler) o;
                String levs = getProperty(type + ".level");
                if (levs != null) {
                    Level l = Level.findLevel(levs);
                    if (l != null) {
                        hdl.setLevel(l);
                    } else {
                        System.err.println("Can't set level for " + type);
                    }
                }
                handlers.add(hdl);
            } catch (Exception ex) {
                System.err.println("Can't load log handler \"" + type + "\"");
                System.err.println("" + ex);
                ex.printStackTrace();
            }
        }

        return handlers;
    }


    private final ReferenceQueue<Logger> loggerRefQueue
        = new ReferenceQueue<>();

    final class LoggerWeakRef extends WeakReference<Logger> {
        private String                name;       
        private LogNode               node;       
        private WeakReference<Logger> parentRef;  
        private boolean disposed = false;         

        LoggerWeakRef(Logger logger) {
            super(logger, loggerRefQueue);

            name = logger.getName();  
        }

        void dispose() {
            synchronized(this) {
                if (disposed) return;
                disposed = true;
            }

            final LogNode n = node;
            if (n != null) {
                synchronized (n.context) {
                    n.context.removeLoggerRef(name, this);
                    name = null;  

                    if (n.loggerRef == this) {
                        n.loggerRef = null;  
                    }
                    node = null;            
                }
            }

            if (parentRef != null) {
                Logger parent = parentRef.get();
                if (parent != null) {
                    parent.removeChildLogger(this);
                }
                parentRef = null;  
            }
        }

        void setNode(LogNode node) {
            this.node = node;
        }

        void setParentRef(WeakReference<Logger> parentRef) {
            this.parentRef = parentRef;
        }
    }

    private static final int MAX_ITERATIONS = 400;
    final void drainLoggerRefQueueBounded() {
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            if (loggerRefQueue == null) {
                break;
            }

            LoggerWeakRef ref = (LoggerWeakRef) loggerRefQueue.poll();
            if (ref == null) {
                break;
            }
            ref.dispose();
        }
    }

    /**
     * Add a named logger.  This does nothing and returns false if a logger
     * with the same name is already registered.
     * <p>
     * The Logger factory methods call this method to register each
     * newly created Logger.
     * <p>
     * The application should retain its own reference to the Logger
     * object to avoid it being garbage collected.  The LogManager
     * may only retain a weak reference.
     *
     * @param   logger the new logger.
     * @return  true if the argument logger was registered successfully,
     *          false if a logger of that name already exists.
     * @throws NullPointerException if the logger name is null.
     */
    public boolean addLogger(Logger logger) {
        final String name = logger.getName();
        if (name == null) {
            throw new NullPointerException();
        }
        drainLoggerRefQueueBounded();
        LoggerContext cx = getUserContext();
        if (cx.addLocalLogger(logger) || forceLoadHandlers(logger)) {
            loadLoggerHandlers(logger, name, name + ".handlers");
            return true;
        } else {
            return false;
        }
    }


    @SuppressWarnings("deprecation")
    private boolean forceLoadHandlers(Logger logger) {
        return (logger == rootLogger ||  logger == Logger.global)
                && !initializationDone
                && initializedCalled
                && configurationLock.isHeldByCurrentThread();
    }

    @SuppressWarnings("removal")
    private static void doSetLevel(final Logger logger, final Level level) {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            logger.setLevel(level);
            return;
        }
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                logger.setLevel(level);
                return null;
            }});
    }

    @SuppressWarnings("removal")
    private static void doSetParent(final Logger logger, final Logger parent) {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            logger.setParent(parent);
            return;
        }
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                logger.setParent(parent);
                return null;
            }});
    }

    /**
     * Method to find a named logger.
     * <p>
     * Note that since untrusted code may create loggers with
     * arbitrary names this method should not be relied on to
     * find Loggers for security sensitive logging.
     * It is also important to note that the Logger associated with the
     * String {@code name} may be garbage collected at any time if there
     * is no strong reference to the Logger. The caller of this method
     * must check the return value for null in order to properly handle
     * the case where the Logger has been garbage collected.
     *
     * @param name name of the logger
     * @return  matching logger or null if none is found
     */
    public Logger getLogger(String name) {
        return getUserContext().findLogger(name);
    }

    /**
     * Get an enumeration of known logger names.
     * <p>
     * Note:  Loggers may be added dynamically as new classes are loaded.
     * This method only reports on the loggers that are currently registered.
     * It is also important to note that this method only returns the name
     * of a Logger, not a strong reference to the Logger itself.
     * The returned String does nothing to prevent the Logger from being
     * garbage collected. In particular, if the returned name is passed
     * to {@code LogManager.getLogger()}, then the caller must check the
     * return value from {@code LogManager.getLogger()} for null to properly
     * handle the case where the Logger has been garbage collected in the
     * time since its name was returned by this method.
     *
     * @return  enumeration of logger name strings
     */
    public Enumeration<String> getLoggerNames() {
        return getUserContext().getLoggerNames();
    }

    /**
     * Reads and initializes the logging configuration.
     * <p>
     * If the "java.util.logging.config.class" system property is set, then the
     * property value is treated as a class name.  The given class will be
     * loaded, an object will be instantiated, and that object's constructor
     * is responsible for reading in the initial configuration.  (That object
     * may use other system properties to control its configuration.)  The
     * alternate configuration class can use {@code readConfiguration(InputStream)}
     * to define properties in the LogManager.
     * <p>
     * If "java.util.logging.config.class" system property is <b>not</b> set,
     * then this method will read the initial configuration from a properties
     * file and calls the {@link #readConfiguration(InputStream)} method to initialize
     * the configuration. The "java.util.logging.config.file" system property can be used
     * to specify the properties file that will be read as the initial configuration;
     * if not set, then the LogManager default configuration is used.
     * The default configuration is typically loaded from the
     * properties file "{@code conf/logging.properties}" in the Java installation
     * directory.
     *
     * <p>
     * Any {@linkplain #addConfigurationListener registered configuration
     * listener} will be invoked after the properties are read.
     *
     * @apiNote This {@code readConfiguration} method should only be used for
     * initializing the configuration during LogManager initialization or
     * used with the "java.util.logging.config.class" property.
     * When this method is called after loggers have been created, and
     * the "java.util.logging.config.class" system property is not set, all
     * existing loggers will be {@linkplain #reset() reset}. Then any
     * existing loggers that have a level property specified in the new
     * configuration stream will be {@linkplain
     * Logger#setLevel(java.util.logging.Level) set} to the specified log level.
     * <p>
     * To properly update the logging configuration, use the
     * {@link #updateConfiguration(java.util.function.Function)} or
     * {@link #updateConfiguration(java.io.InputStream, java.util.function.Function)}
     * methods instead.
     *
     * @throws   SecurityException  if a security manager exists and if
     *              the caller does not have LoggingPermission("control").
     * @throws   IOException if there are IO problems reading the configuration.
     */
    public void readConfiguration() throws IOException, SecurityException {
        checkPermission();

        String cname = System.getProperty("java.util.logging.config.class");
        if (cname != null) {
            try {
                try {
                    Class<?> clz = ClassLoader.getSystemClassLoader().loadClass(cname);
                    @SuppressWarnings("deprecation")
                    Object witness = clz.newInstance();
                    return;
                } catch (ClassNotFoundException ex) {
                    Class<?> clz = Thread.currentThread().getContextClassLoader().loadClass(cname);
                    @SuppressWarnings("deprecation")
                    Object witness = clz.newInstance();
                    return;
                }
            } catch (Exception ex) {
                System.err.println("Logging configuration class \"" + cname + "\" failed");
                System.err.println("" + ex);
            }
        }

        String fname = getConfigurationFileName();
        try (final InputStream in = new FileInputStream(fname)) {
            readConfiguration(in);
        }
    }

    String getConfigurationFileName() throws IOException {
        String fname = System.getProperty("java.util.logging.config.file");
        if (fname == null) {
            fname = System.getProperty("java.home");
            if (fname == null) {
                throw new Error("Can't find java.home ??");
            }
            fname = Paths.get(fname, "conf", "logging.properties")
                    .toAbsolutePath().normalize().toString();
        }
        return fname;
    }

    /**
     * Reset the logging configuration.
     * <p>
     * For all named loggers, the reset operation removes and closes
     * all Handlers and (except for the root logger) sets the level
     * to {@code null}. The root logger's level is set to {@code Level.INFO}.
     *
     * @apiNote Calling this method also clears the LogManager {@linkplain
     * #getProperty(java.lang.String) properties}. The {@link
     * #updateConfiguration(java.util.function.Function)
     * updateConfiguration(Function)} or
     * {@link #updateConfiguration(java.io.InputStream, java.util.function.Function)
     * updateConfiguration(InputStream, Function)} method can be used to
     * properly update to a new configuration.
     *
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */

    public void reset() throws SecurityException {
        checkPermission();

        List<CloseOnReset> persistent;

        configurationLock.lock();
        try {
            props = new Properties();
            persistent = new ArrayList<>(closeOnResetLoggers);
            closeOnResetLoggers.clear();

            if (globalHandlersState != STATE_SHUTDOWN &&
                globalHandlersState != STATE_READING_CONFIG) {
                globalHandlersState = STATE_INITIALIZED;
            }

            for (LoggerContext cx : contexts()) {
                resetLoggerContext(cx);
            }

            persistent.clear();
        } finally {
            configurationLock.unlock();
        }
    }

    private void resetLoggerContext(LoggerContext cx) {
        Enumeration<String> enum_ = cx.getLoggerNames();
        while (enum_.hasMoreElements()) {
            String name = enum_.nextElement();
            Logger logger = cx.findLogger(name);
            if (logger != null) {
                resetLogger(logger);
            }
        }
    }

    private void closeHandlers(Logger logger) {
        Handler[] targets = logger.getHandlers();
        for (Handler h : targets) {
            logger.removeHandler(h);
            try {
                h.close();
            } catch (Exception ex) {
            } catch (Error e) {
                if (globalHandlersState != STATE_SHUTDOWN) {
                    throw e;
                }
            }
        }
    }

    private void resetLogger(Logger logger) {
        closeHandlers(logger);

        String name = logger.getName();
        if (name != null && name.isEmpty()) {
            logger.setLevel(defaultLevel);
        } else {
            logger.setLevel(null);
        }
    }

    private String[] parseClassNames(String propertyName) {
        String hands = getProperty(propertyName);
        if (hands == null) {
            return new String[0];
        }
        hands = hands.trim();
        int ix = 0;
        final List<String> result = new ArrayList<>();
        while (ix < hands.length()) {
            int end = ix;
            while (end < hands.length()) {
                if (Character.isWhitespace(hands.charAt(end))) {
                    break;
                }
                if (hands.charAt(end) == ',') {
                    break;
                }
                end++;
            }
            String word = hands.substring(ix, end);
            ix = end+1;
            word = word.trim();
            if (word.length() == 0) {
                continue;
            }
            result.add(word);
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * Reads and initializes the logging configuration from the given input stream.
     *
     * <p>
     * Any {@linkplain #addConfigurationListener registered configuration
     * listener} will be invoked after the properties are read.
     *
     * @apiNote This {@code readConfiguration} method should only be used for
     * initializing the configuration during LogManager initialization or
     * used with the "java.util.logging.config.class" property.
     * When this method is called after loggers have been created, all
     * existing loggers will be {@linkplain #reset() reset}. Then any
     * existing loggers that have a level property specified in the
     * given input stream will be {@linkplain
     * Logger#setLevel(java.util.logging.Level) set} to the specified log level.
     * <p>
     * To properly update the logging configuration, use the
     * {@link #updateConfiguration(java.util.function.Function)} or
     * {@link #updateConfiguration(java.io.InputStream, java.util.function.Function)}
     * method instead.
     *
     * @param ins  stream to read properties from
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     * @throws  IOException if there are problems reading from the stream,
     *             or the given stream is not in the
     *             {@linkplain java.util.Properties properties file} format.
     */
    public void readConfiguration(InputStream ins) throws IOException, SecurityException {
        checkPermission();

        configurationLock.lock();
        try {
            if (globalHandlersState == STATE_SHUTDOWN) {
                return;
            }

            globalHandlersState = STATE_READING_CONFIG;
            try {
                reset();

                try {
                    props.load(ins);
                } catch (IllegalArgumentException x) {
                    throw new IOException(x.getMessage(), x);
                }

                String names[] = parseClassNames("config");

                for (String word : names) {
                    try {
                        Class<?> clz = ClassLoader.getSystemClassLoader().loadClass(word);
                        @SuppressWarnings("deprecation")
                        Object witness = clz.newInstance();
                    } catch (Exception ex) {
                        System.err.println("Can't load config class \"" + word + "\"");
                        System.err.println("" + ex);
                    }
                }

                setLevelsOnExistingLoggers();

                globalHandlersState = STATE_UNINITIALIZED;
            } catch (Throwable t) {
                globalHandlersState = STATE_INITIALIZED;
                throw t;
            }
        } finally {
            configurationLock.unlock();
        }

        invokeConfigurationListeners();
    }

    static enum ConfigProperty {
        LEVEL(".level"), HANDLERS(".handlers"), USEPARENT(".useParentHandlers");
        final String suffix;
        final int length;
        private ConfigProperty(String suffix) {
            this.suffix = Objects.requireNonNull(suffix);
            length = suffix.length();
        }

        public boolean handleKey(String key) {
            if (this == HANDLERS && suffix.substring(1).equals(key)) return true;
            if (this == HANDLERS && suffix.equals(key)) return false;
            return key.endsWith(suffix);
        }
        String key(String loggerName) {
            if (this == HANDLERS && (loggerName == null || loggerName.isEmpty())) {
                return suffix.substring(1);
            }
            return loggerName + suffix;
        }
        String loggerName(String key) {
            assert key.equals(suffix.substring(1)) && this == HANDLERS || key.endsWith(suffix);
            if (this == HANDLERS && suffix.substring(1).equals(key)) return "";
            return key.substring(0, key.length() - length);
        }

        /**
         * If the property is one that should be updated on existing loggers by
         * updateConfiguration, returns the name of the logger for which the
         * property is configured. Otherwise, returns null.
         * @param property a property key in 'props'
         * @return the name of the logger on which the property is to be set,
         *         if the property is one that should be updated on existing
         *         loggers, {@code null} otherwise.
         */
        static String getLoggerName(String property) {
            for (ConfigProperty p : ConfigProperty.ALL) {
                if (p.handleKey(property)) {
                    return p.loggerName(property);
                }
            }
            return null; 
        }

        /**
         * Find the ConfigProperty corresponding to the given
         * property key (may find none).
         * @param property a property key in 'props'
         * @return An optional containing a ConfigProperty object,
         *         if the property is one that should be updated on existing
         *         loggers, empty otherwise.
         */
        static Optional<ConfigProperty> find(String property) {
            return ConfigProperty.ALL.stream()
                    .filter(p -> p.handleKey(property))
                    .findFirst();
         }

        /**
         * Returns true if the given property is one that should be updated
         * on existing loggers.
         * Used to filter property name streams.
         * @param property a property key from the configuration.
         * @return true if this property is of interest for updateConfiguration.
         */
        static boolean matches(String property) {
            return find(property).isPresent();
        }

        /**
         * Returns true if the new property value is different from the old,
         * and therefore needs to be updated on existing loggers.
         * @param k a property key in the configuration
         * @param previous the old configuration
         * @param next the new configuration
         * @return true if the property is changing value between the two
         *         configurations.
         */
        static boolean needsUpdating(String k, Properties previous, Properties next) {
            final String p = trim(previous.getProperty(k, null));
            final String n = trim(next.getProperty(k, null));
            return ! Objects.equals(p,n);
        }

        /**
         * Applies the mapping function for the given key to the next
         * configuration.
         * If the mapping function is null then this method does nothing.
         * Otherwise, it calls the mapping function to compute the value
         * that should be associated with {@code key} in the resulting
         * configuration, and applies it to {@code next}.
         * If the mapping function returns {@code null} the key is removed
         * from {@code next}.
         *
         * @param k a property key in the configuration
         * @param previous the old configuration
         * @param next the new configuration (modified by this function)
         * @param mappingFunction the mapping function.
         */
        static void merge(String k, Properties previous, Properties next,
                          BiFunction<String, String, String> mappingFunction) {
            String p = trim(previous.getProperty(k, null));
            String n = trim(next.getProperty(k, null));
            String mapped = trim(mappingFunction.apply(p,n));
            if (!Objects.equals(n, mapped)) {
                if (mapped == null) {
                    next.remove(k);
                } else {
                    next.setProperty(k, mapped);
                }
            }
        }

        private static final EnumSet<ConfigProperty> ALL =
                EnumSet.allOf(ConfigProperty.class);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * An object that keep track of loggers we have already visited.
     * Used when updating configuration, to avoid processing the same logger
     * twice.
     */
    static final class VisitedLoggers implements Predicate<Logger> {
        final IdentityHashMap<Logger,Boolean> visited;
        private VisitedLoggers(IdentityHashMap<Logger,Boolean> visited) {
            this.visited = visited;
        }
        VisitedLoggers() {
            this(new IdentityHashMap<>());
        }
        @Override
        public boolean test(Logger logger) {
            return visited != null && visited.put(logger, Boolean.TRUE) != null;
        }
        public void clear() {
            if (visited != null) visited.clear();
        }

        static final VisitedLoggers NEVER = new VisitedLoggers(null);
    }


    /**
     * Type of the modification for a given property. One of SAME, ADDED, CHANGED,
     * or REMOVED.
     */
    static enum ModType {
        SAME,    
        ADDED,   
        CHANGED, 
        REMOVED; 
        static ModType of(String previous, String next) {
            if (previous == null && next != null) {
                return ADDED;
            }
            if (next == null && previous != null) {
                return REMOVED;
            }
            if (!Objects.equals(trim(previous), trim(next))) {
                return CHANGED;
            }
            return SAME;
        }
    }

    /**
     * Updates the logging configuration.
     * <p>
     * If the "java.util.logging.config.file" system property is set,
     * then the property value specifies the properties file to be read
     * as the new configuration. Otherwise, the LogManager default
     * configuration is used.
     * <br>The default configuration is typically loaded from the
     * properties file "{@code conf/logging.properties}" in the
     * Java installation directory.
     * <p>
     * This method reads the new configuration and calls the {@link
     * #updateConfiguration(java.io.InputStream, java.util.function.Function)
     * updateConfiguration(ins, mapper)} method to
     * update the configuration.
     *
     * @apiNote
     * This method updates the logging configuration from reading
     * a properties file and ignores the "java.util.logging.config.class"
     * system property.  The "java.util.logging.config.class" property is
     * only used by the {@link #readConfiguration()}  method to load a custom
     * configuration class as an initial configuration.
     *
     * @param mapper a functional interface that takes a configuration
     *   key <i>k</i> and returns a function <i>f(o,n)</i> whose returned
     *   value will be applied to the resulting configuration. The
     *   function <i>f</i> may return {@code null} to indicate that the property
     *   <i>k</i> will not be added to the resulting configuration.
     *   <br>
     *   If {@code mapper} is {@code null} then {@code (k) -> ((o, n) -> n)} is
     *   assumed.
     *   <br>
     *   For each <i>k</i>, the mapped function <i>f</i> will
     *   be invoked with the value associated with <i>k</i> in the old
     *   configuration (i.e <i>o</i>) and the value associated with
     *   <i>k</i> in the new configuration (i.e. <i>n</i>).
     *   <br>A {@code null} value for <i>o</i> or <i>n</i> indicates that no
     *   value was present for <i>k</i> in the corresponding configuration.
     *
     * @throws  SecurityException  if a security manager exists and if
     *          the caller does not have LoggingPermission("control"), or
     *          does not have the permissions required to set up the
     *          configuration (e.g. open file specified for FileHandlers
     *          etc...)
     *
     * @throws  NullPointerException  if {@code mapper} returns a {@code null}
     *         function when invoked.
     *
     * @throws  IOException if there are problems reading from the
     *          logging configuration file.
     *
     * @see #updateConfiguration(java.io.InputStream, java.util.function.Function)
     * @since 9
     */
    public void updateConfiguration(Function<String, BiFunction<String,String,String>> mapper)
            throws IOException {
        checkPermission();
        ensureLogManagerInitialized();
        drainLoggerRefQueueBounded();

        String fname = getConfigurationFileName();
        try (final InputStream in = new FileInputStream(fname)) {
            updateConfiguration(in, mapper);
        }
    }

    /**
     * Updates the logging configuration.
     * <p>
     * For each configuration key in the {@linkplain
     * #getProperty(java.lang.String) existing configuration} and
     * the given input stream configuration, the given {@code mapper} function
     * is invoked to map from the configuration key to a function,
     * <i>f(o,n)</i>, that takes the old value and new value and returns
     * the resulting value to be applied in the resulting configuration,
     * as specified in the table below.
     * <p>Let <i>k</i> be a configuration key in the old or new configuration,
     * <i>o</i> be the old value (i.e. the value associated
     * with <i>k</i> in the old configuration), <i>n</i> be the
     * new value (i.e. the value associated with <i>k</i> in the new
     * configuration), and <i>f</i> be the function returned
     * by {@code mapper.apply(}<i>k</i>{@code )}: then <i>v = f(o,n)</i> is the
     * resulting value. If <i>v</i> is not {@code null}, then a property
     * <i>k</i> with value <i>v</i> will be added to the resulting configuration.
     * Otherwise, it will be omitted.
     * <br>A {@code null} value may be passed to function
     * <i>f</i> to indicate that the corresponding configuration has no
     * configuration key <i>k</i>.
     * The function <i>f</i> may return {@code null} to indicate that
     * there will be no value associated with <i>k</i> in the resulting
     * configuration.
     * <p>
     * If {@code mapper} is {@code null}, then <i>v</i> will be set to
     * <i>n</i>.
     * <p>
     * LogManager {@linkplain #getProperty(java.lang.String) properties} are
     * updated with the resulting value in the resulting configuration.
     * <p>
     * The registered {@linkplain #addConfigurationListener configuration
     * listeners} will be invoked after the configuration is successfully updated.
     * <br><br>
     * <table class="striped">
     * <caption style="display:none">Updating configuration properties</caption>
     * <thead>
     * <tr>
     * <th scope="col">Property</th>
     * <th scope="col">Resulting Behavior</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <th scope="row" style="vertical-align:top">{@code <logger>.level}</th>
     * <td>
     * <ul>
     *   <li>If the resulting configuration defines a level for a logger and
     *       if the resulting level is different than the level specified in the
     *       the old configuration, or not specified in
     *       the old configuration, then if the logger exists or if children for
     *       that logger exist, the level for that logger will be updated,
     *       and the change propagated to any existing logger children.
     *       This may cause the logger to be created, if necessary.
     *   </li>
     *   <li>If the old configuration defined a level for a logger, and the
     *       resulting configuration doesn't, then this change will not be
     *       propagated to existing loggers, if any.
     *       To completely replace a configuration - the caller should therefore
     *       call {@link #reset() reset} to empty the current configuration,
     *       before calling {@code updateConfiguration}.
     *   </li>
     * </ul>
     * </td>
     * <tr>
     * <th scope="row" style="vertical-align:top">{@code <logger>.useParentHandlers}</th>
     * <td>
     * <ul>
     *   <li>If either the resulting or the old value for the useParentHandlers
     *       property is not null, then if the logger exists or if children for
     *       that logger exist, that logger will be updated to the resulting
     *       value.
     *       The value of the useParentHandlers property is the value specified
     *       in the configuration; if not specified, the default is true.
     *   </li>
     * </ul>
     * </td>
     * </tr>
     * <tr>
     * <th scope="row" style="vertical-align:top">{@code <logger>.handlers}</th>
     * <td>
     * <ul>
     *   <li>If the resulting configuration defines a list of handlers for a
     *       logger, and if the resulting list is different than the list
     *       specified in the old configuration for that logger (that could be
     *       empty), then if the logger exists or its children exist, the
     *       handlers associated with that logger are closed and removed and
     *       the new handlers will be created per the resulting configuration
     *       and added to that logger, creating that logger if necessary.
     *   </li>
     *   <li>If the old configuration defined some handlers for a logger, and
     *       the resulting configuration doesn't, if that logger exists,
     *       its handlers will be removed and closed.
     *   </li>
     *   <li>Changing the list of handlers on an existing logger will cause all
     *       its previous handlers to be removed and closed, regardless of whether
     *       they had been created from the configuration or programmatically.
     *       The old handlers will be replaced by new handlers, if any.
     *   </li>
     * </ul>
     * </td>
     * </tr>
     * <tr>
     * <th scope="row" style="vertical-align:top">{@code <handler-name>.*}</th>
     * <td>
     * <ul>
     *   <li>Properties configured/changed on handler classes will only affect
     *       newly created handlers. If a node is configured with the same list
     *       of handlers in the old and the resulting configuration, then these
     *       handlers will remain unchanged.
     *   </li>
     * </ul>
     * </td>
     * </tr>
     * <tr>
     * <th scope="row" style="vertical-align:top">{@code config} and any other property</th>
     * <td>
     * <ul>
     *   <li>The resulting value for these property will be stored in the
     *   LogManager properties, but {@code updateConfiguration} will not parse
     *   or process their values.
     *   </li>
     * </ul>
     * </td>
     * </tr>
     * </tbody>
     * </table>
     * <p>
     * <em>Example mapper functions:</em>
     * <br><br>
     * <ul>
     * <li>Replace all logging properties with the new configuration:
     * <br><br>{@code     (k) -> ((o, n) -> n)}:
     * <br><br>this is equivalent to passing a null {@code mapper} parameter.
     * </li>
     * <li>Merge the new configuration and old configuration and use the
     * new value if <i>k</i> exists in the new configuration:
     * <br><br>{@code     (k) -> ((o, n) -> n == null ? o : n)}:
     * <br><br>as if merging two collections as follows:
     * {@code result.putAll(oldc); result.putAll(newc)}.<br></li>
     * <li>Merge the new configuration and old configuration and use the old
     * value if <i>k</i> exists in the old configuration:
     * <br><br>{@code     (k) -> ((o, n) -> o == null ? n : o)}:
     * <br><br>as if merging two collections as follows:
     * {@code result.putAll(newc); result.putAll(oldc)}.<br></li>
     * <li>Replace all properties with the new configuration except the handler
     * property to configure Logger's handler that is not root logger:
     * <br>
     * <pre>{@code (k) -> k.endsWith(".handlers")}
     *      {@code     ? ((o, n) -> (o == null ? n : o))}
     *      {@code     : ((o, n) -> n)}</pre>
     * </li>
     * </ul>
     * <p>
     * To completely reinitialize a configuration, an application can first call
     * {@link #reset() reset} to fully remove the old configuration, followed by
     * {@code updateConfiguration} to initialize the new configuration.
     *
     * @param ins    a stream to read properties from
     * @param mapper a functional interface that takes a configuration
     *   key <i>k</i> and returns a function <i>f(o,n)</i> whose returned
     *   value will be applied to the resulting configuration. The
     *   function <i>f</i> may return {@code null} to indicate that the property
     *   <i>k</i> will not be added to the resulting configuration.
     *   <br>
     *   If {@code mapper} is {@code null} then {@code (k) -> ((o, n) -> n)} is
     *   assumed.
     *   <br>
     *   For each <i>k</i>, the mapped function <i>f</i> will
     *   be invoked with the value associated with <i>k</i> in the old
     *   configuration (i.e <i>o</i>) and the value associated with
     *   <i>k</i> in the new configuration (i.e. <i>n</i>).
     *   <br>A {@code null} value for <i>o</i> or <i>n</i> indicates that no
     *   value was present for <i>k</i> in the corresponding configuration.
     *
     * @throws  SecurityException if a security manager exists and if
     *          the caller does not have LoggingPermission("control"), or
     *          does not have the permissions required to set up the
     *          configuration (e.g. open files specified for FileHandlers)
     *
     * @throws  NullPointerException if {@code ins} is null or if
     *          {@code mapper} returns a null function when invoked.
     *
     * @throws  IOException if there are problems reading from the stream,
     *          or the given stream is not in the
     *          {@linkplain java.util.Properties properties file} format.
     * @since 9
     */
    public void updateConfiguration(InputStream ins,
            Function<String, BiFunction<String,String,String>> mapper)
            throws IOException {
        checkPermission();
        ensureLogManagerInitialized();
        drainLoggerRefQueueBounded();

        final Properties previous;
        final Set<String> updatePropertyNames;
        List<LoggerContext> cxs = Collections.emptyList();
        final VisitedLoggers visited = new VisitedLoggers();
        final Properties next = new Properties();

        try {
            next.load(ins);
        } catch (IllegalArgumentException x) {
            throw new IOException(x.getMessage(), x);
        }

        if (globalHandlersState == STATE_SHUTDOWN) return;

        configurationLock.lock();
        try {
            if (globalHandlersState == STATE_SHUTDOWN) return;
            previous = props;

            updatePropertyNames =
                    Stream.concat(previous.stringPropertyNames().stream(),
                                  next.stringPropertyNames().stream())
                        .collect(Collectors.toCollection(TreeSet::new));

            if (mapper != null) {
                updatePropertyNames.stream()
                        .forEachOrdered(k -> ConfigProperty
                                .merge(k, previous, next,
                                       Objects.requireNonNull(mapper.apply(k))));
            }

            props = next;

            final Stream<String> allKeys = updatePropertyNames.stream()
                    .filter(ConfigProperty::matches)
                    .filter(k -> ConfigProperty.needsUpdating(k, previous, next));

            final Map<String, TreeSet<String>> loggerConfigs =
                    allKeys.collect(Collectors.groupingBy(ConfigProperty::getLoggerName,
                                    TreeMap::new,
                                    Collectors.toCollection(TreeSet::new)));

            if (!loggerConfigs.isEmpty()) {
                cxs = contexts();
            }
            final List<Logger> loggers = cxs.isEmpty()
                    ? Collections.emptyList() : new ArrayList<>(cxs.size());
            for (Map.Entry<String, TreeSet<String>> e : loggerConfigs.entrySet()) {
                final String name = e.getKey();
                final Set<String> properties = e.getValue();
                loggers.clear();
                for (LoggerContext cx : cxs) {
                    Logger l = cx.findLogger(name);
                    if (l != null && !visited.test(l)) {
                        loggers.add(l);
                    }
                }
                if (loggers.isEmpty()) continue;
                for (String pk : properties) {
                    ConfigProperty cp = ConfigProperty.find(pk).get();
                    String p = previous.getProperty(pk, null);
                    String n = next.getProperty(pk, null);

                    ModType mod = ModType.of(p, n);

                    if (mod == ModType.SAME) continue;

                    switch (cp) {
                        case LEVEL:
                            if (mod == ModType.REMOVED) continue;
                            Level level = Level.findLevel(trim(n));
                            if (level != null) {
                                if (name.isEmpty()) {
                                    rootLogger.setLevel(level);
                                }
                                for (Logger l : loggers) {
                                    if (!name.isEmpty() || l != rootLogger) {
                                        l.setLevel(level);
                                    }
                                }
                            }
                            break;
                        case USEPARENT:
                            if (!name.isEmpty()) {
                                boolean useParent = getBooleanProperty(pk, true);
                                if (n != null || p != null) {
                                    for (Logger l : loggers) {
                                        l.setUseParentHandlers(useParent);
                                    }
                                }
                            }
                            break;
                        case HANDLERS:
                            List<Handler> hdls = null;
                            if (name.isEmpty()) {
                                globalHandlersState = STATE_READING_CONFIG;
                                try {
                                    closeHandlers(rootLogger);
                                    globalHandlersState = STATE_UNINITIALIZED;
                                } catch (Throwable t) {
                                    globalHandlersState = STATE_INITIALIZED;
                                    throw t;
                                }
                            }
                            for (Logger l : loggers) {
                                if (l == rootLogger) continue;
                                closeHandlers(l);
                                if (mod == ModType.REMOVED) {
                                    closeOnResetLoggers.removeIf(c -> c.logger == l);
                                    continue;
                                }
                                if (hdls == null) {
                                    hdls = name.isEmpty()
                                            ? Arrays.asList(rootLogger.getHandlers())
                                            : createLoggerHandlers(name, pk);
                                }
                                setLoggerHandlers(l, name, pk, hdls);
                            }
                            break;
                        default: break;
                    }
                }
            }
        } finally {
            configurationLock.unlock();
            visited.clear();
        }

        drainLoggerRefQueueBounded();
        for (LoggerContext cx : cxs) {
            for (Enumeration<String> names = cx.getLoggerNames() ; names.hasMoreElements();) {
                String name = names.nextElement();
                if (name.isEmpty()) continue;  
                Logger l = cx.findLogger(name);
                if (l != null && !visited.test(l)) {
                    cx.processParentHandlers(l, name, visited);
                }
            }
        }

        invokeConfigurationListeners();
    }

    /**
     * Get the value of a logging property.
     * The method returns null if the property is not found.
     * @param name      property name
     * @return          property value
     */
    public String getProperty(String name) {
        return props.getProperty(name);
    }

    String getStringProperty(String name, String defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        return val.trim();
    }

    int getIntProperty(String name, int defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    long getLongProperty(String name, long defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(val.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    boolean getBooleanProperty(String name, boolean defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        val = val.toLowerCase();
        if (val.equals("true") || val.equals("1")) {
            return true;
        } else if (val.equals("false") || val.equals("0")) {
            return false;
        }
        return defaultValue;
    }

    Level getLevelProperty(String name, Level defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        Level l = Level.findLevel(val.trim());
        return l != null ? l : defaultValue;
    }

    Filter getFilterProperty(String name, Filter defaultValue) {
        String val = getProperty(name);
        try {
            if (val != null) {
                @SuppressWarnings("deprecation")
                Object o = ClassLoader.getSystemClassLoader().loadClass(val).newInstance();
                return (Filter) o;
            }
        } catch (Exception ex) {
        }
        return defaultValue;
    }


    Formatter getFormatterProperty(String name, Formatter defaultValue) {
        String val = getProperty(name);
        try {
            if (val != null) {
                @SuppressWarnings("deprecation")
                Object o = ClassLoader.getSystemClassLoader().loadClass(val).newInstance();
                return (Formatter) o;
            }
        } catch (Exception ex) {
        }
        return defaultValue;
    }

    private void initializeGlobalHandlers() {
        int state = globalHandlersState;
        if (state == STATE_INITIALIZED ||
            state == STATE_SHUTDOWN) {
            return;
        }

        configurationLock.lock();
        try {
            if (globalHandlersState != STATE_UNINITIALIZED) {
                return; 
            }
            globalHandlersState = STATE_INITIALIZING;
            try {
                loadLoggerHandlers(rootLogger, null, "handlers");
            } finally {
                globalHandlersState = STATE_INITIALIZED;
            }
        } finally {
            configurationLock.unlock();
        }
    }

    static final Permission controlPermission =
            new LoggingPermission("control", null);

    void checkPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(controlPermission);
    }

    /**
     * Check that the current context is trusted to modify the logging
     * configuration.  This requires LoggingPermission("control").
     * <p>
     * If the check fails we throw a SecurityException, otherwise
     * we return normally.
     *
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     * @deprecated This method is only useful in conjunction with
     *       {@linkplain SecurityManager the Security Manager}, which is
     *       deprecated and subject to removal in a future release.
     *       Consequently, this method is also deprecated and subject to
     *       removal. There is no replacement for the Security Manager or this
     *       method.
     */
    @Deprecated(since="17", forRemoval=true)
    public void checkAccess() throws SecurityException {
        checkPermission();
    }

    private static class LogNode {
        HashMap<String,LogNode> children;
        LoggerWeakRef loggerRef;
        LogNode parent;
        final LoggerContext context;

        LogNode(LogNode parent, LoggerContext context) {
            this.parent = parent;
            this.context = context;
        }

        void walkAndSetParent(Logger parent) {
            if (children == null) {
                return;
            }
            for (LogNode node : children.values()) {
                LoggerWeakRef ref = node.loggerRef;
                Logger logger = (ref == null) ? null : ref.get();
                if (logger == null) {
                    node.walkAndSetParent(parent);
                } else {
                    doSetParent(logger, parent);
                }
            }
        }
    }

    private final class RootLogger extends Logger {
        private RootLogger() {
            super("", null, null, LogManager.this, true);
        }

        @Override
        public void log(LogRecord record) {
            initializeGlobalHandlers();
            super.log(record);
        }

        @Override
        public void addHandler(Handler h) {
            initializeGlobalHandlers();
            super.addHandler(h);
        }

        @Override
        public void removeHandler(Handler h) {
            initializeGlobalHandlers();
            super.removeHandler(h);
        }

        @Override
        Handler[] accessCheckedHandlers() {
            initializeGlobalHandlers();
            return super.accessCheckedHandlers();
        }
    }


    private void setLevelsOnExistingLoggers() {
        Enumeration<?> enum_ = props.propertyNames();
        while (enum_.hasMoreElements()) {
            String key = (String)enum_.nextElement();
            if (!key.endsWith(".level")) {
                continue;
            }
            int ix = key.length() - 6;
            String name = key.substring(0, ix);
            Level level = getLevelProperty(key, null);
            if (level == null) {
                System.err.println("Bad level value for property: " + key);
                continue;
            }
            for (LoggerContext cx : contexts()) {
                Logger l = cx.findLogger(name);
                if (l == null) {
                    continue;
                }
                l.setLevel(level);
            }
        }
    }

    /**
     * String representation of the
     * {@link java.management/javax.management.ObjectName} for the management interface
     * for the logging facility.
     *
     * @see java.management/java.lang.management.PlatformLoggingMXBean
     *
     * @since 1.5
     */
    @SuppressWarnings("doclint:reference")
    public static final String LOGGING_MXBEAN_NAME
        = "java.util.logging:type=Logging";

    /**
     * Returns {@code LoggingMXBean} for managing loggers.
     *
     * @return a {@link LoggingMXBean} object.
     *
     * @deprecated {@code java.util.logging.LoggingMXBean} is deprecated and
     *      replaced with {@code java.lang.management.PlatformLoggingMXBean}. Use
     *      {@link java.management/java.lang.management.ManagementFactory#getPlatformMXBean(Class)
     *      ManagementFactory.getPlatformMXBean}(PlatformLoggingMXBean.class)
     *      instead.
     *
     * @see java.management/java.lang.management.PlatformLoggingMXBean
     * @since 1.5
     */
    @Deprecated(since="9")
    @SuppressWarnings("doclint:reference")
    public static synchronized LoggingMXBean getLoggingMXBean() {
        return Logging.getInstance();
    }

    /**
     * Adds a configuration listener to be invoked each time the logging
     * configuration is read.
     * If the listener is already registered the method does nothing.
     * <p>
     * The listener is invoked with privileges that are restricted by the
     * calling context of this method.
     * The order in which the listeners are invoked is unspecified.
     * <p>
     * It is recommended that listeners do not throw errors or exceptions.
     *
     * If a listener terminates with an uncaught error or exception then
     * the first exception will be propagated to the caller of
     * {@link #readConfiguration()} (or {@link #readConfiguration(java.io.InputStream)})
     * after all listeners have been invoked.
     *
     * @implNote If more than one listener terminates with an uncaught error or
     * exception, an implementation may record the additional errors or
     * exceptions as {@linkplain Throwable#addSuppressed(java.lang.Throwable)
     * suppressed exceptions}.
     *
     * @param listener A configuration listener that will be invoked after the
     *        configuration changed.
     * @return This LogManager.
     * @throws SecurityException if a security manager exists and if the
     * caller does not have LoggingPermission("control").
     * @throws NullPointerException if the listener is null.
     *
     * @since 9
     */
    public LogManager addConfigurationListener(Runnable listener) {
        final Runnable r = Objects.requireNonNull(listener);
        checkPermission();
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        @SuppressWarnings("removal")
        final AccessControlContext acc =
                sm == null ? null : AccessController.getContext();
        final PrivilegedAction<Void> pa =
                acc == null ? null : () -> { r.run() ; return null; };
        @SuppressWarnings("removal")
        final Runnable pr =
                acc == null ? r : () -> AccessController.doPrivileged(pa, acc);
        listeners.putIfAbsent(r, pr);
        return this;
    }

    /**
     * Removes a previously registered configuration listener.
     *
     * Returns silently if the listener is not found.
     *
     * @param listener the configuration listener to remove.
     * @throws NullPointerException if the listener is null.
     * @throws SecurityException if a security manager exists and if the
     * caller does not have LoggingPermission("control").
     *
     * @since 9
     */
    public void removeConfigurationListener(Runnable listener) {
        final Runnable key = Objects.requireNonNull(listener);
        checkPermission();
        listeners.remove(key);
    }

    private void invokeConfigurationListeners() {
        Throwable t = null;

        for (Runnable c : listeners.values().toArray(new Runnable[0])) {
            try {
                c.run();
            } catch (Error | RuntimeException x) {
                if (t == null) t = x;
                else t.addSuppressed(x);
            }
        }
        if (t instanceof Error) throw (Error)t;
        if (t instanceof RuntimeException) throw (RuntimeException)t;
    }

    /**
     * This class allows the {@link LoggingProviderImpl} to demand loggers on
     * behalf of system and application classes.
     */
    private static final class LoggingProviderAccess
        implements LoggingProviderImpl.LogManagerAccess,
                   PrivilegedAction<Void> {

        private LoggingProviderAccess() {
        }

        /**
         * Demands a logger on behalf of the given {@code module}.
         * <p>
         * If a named logger suitable for the given module is found
         * returns it.
         * Otherwise, creates a new logger suitable for the given module.
         *
         * @param name   The logger name.
         * @param module The module on which behalf the logger is created/retrieved.
         * @return A logger for the given {@code module}.
         *
         * @throws NullPointerException if {@code name} is {@code null}
         *         or {@code module} is {@code null}.
         * @throws IllegalArgumentException if {@code manager} is not the default
         *         LogManager.
         * @throws SecurityException if a security manager is present and the
         *         calling code doesn't have the
         *        {@link LoggingPermission LoggingPermission("demandLogger", null)}.
         */
        @Override
        public Logger demandLoggerFor(LogManager manager, String name, Module module) {
            if (manager != getLogManager()) {
                throw new IllegalArgumentException("manager");
            }
            Objects.requireNonNull(name);
            Objects.requireNonNull(module);
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(controlPermission);
            }
            if (isSystem(module)) {
                return manager.demandSystemLogger(name,
                    Logger.SYSTEM_LOGGER_RB_NAME, module);
            } else {
                return manager.demandLogger(name, null, module);
            }
        }

        @Override
        public Void run() {
            LoggingProviderImpl.setLogManagerAccess(INSTANCE);
            return null;
        }

        static final LoggingProviderAccess INSTANCE = new LoggingProviderAccess();
    }

    static {
        initStatic();
    }

    @SuppressWarnings("removal")
    private static void initStatic() {
        AccessController.doPrivileged(LoggingProviderAccess.INSTANCE, null,
                                      controlPermission);
    }

}
