/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.logger;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jdk.internal.misc.InnocuousThread;
import jdk.internal.misc.VM;
import sun.util.logging.PlatformLogger;
import jdk.internal.logger.LazyLoggers.LazyLoggerAccessor;

/**
 * The BootstrapLogger class handles all the logic needed by Lazy Loggers
 * to delay the creation of System.Logger instances until the VM is booted.
 * By extension - it also contains the logic that will delay the creation
 * of JUL Loggers until the LogManager is initialized by the application, in
 * the common case where JUL is the default and there is no custom JUL
 * configuration.
 *
 * A BootstrapLogger instance is both a Logger and a
 * PlatformLogger.Bridge instance, which will put all Log messages in a queue
 * until the VM is booted.
 * Once the VM is booted, it obtain the real System.Logger instance from the
 * LoggerFinder and flushes the message to the queue.
 *
 * There are a few caveat:
 *  - the queue may not be flush until the next message is logged after
 *    the VM is booted
 *  - while the BootstrapLogger is active, the default implementation
 *    for all convenience methods is used
 *  - PlatformLogger.setLevel calls are ignored
 *
 *
 */
public final class BootstrapLogger implements Logger, PlatformLogger.Bridge,
        PlatformLogger.ConfigurableBridge {

    private static class BootstrapExecutors implements ThreadFactory {

        static final long KEEP_EXECUTOR_ALIVE_SECONDS = 30;

        private static class BootstrapMessageLoggerTask implements Runnable {
            ExecutorService owner;
            Runnable run;
            public BootstrapMessageLoggerTask(ExecutorService owner, Runnable r) {
                this.owner = owner;
                this.run = r;
            }
            @Override
            public void run() {
                try {
                    run.run();
                } finally {
                    owner = null; 
                }
            }
        }

        private static volatile WeakReference<ExecutorService> executorRef;
        private static ExecutorService getExecutor() {
            WeakReference<ExecutorService> ref = executorRef;
            ExecutorService executor = ref == null ? null : ref.get();
            if (executor != null) return executor;
            synchronized (BootstrapExecutors.class) {
                ref = executorRef;
                executor = ref == null ? null : ref.get();
                if (executor == null) {
                    executor = new ThreadPoolExecutor(0, 1,
                            KEEP_EXECUTOR_ALIVE_SECONDS, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(), new BootstrapExecutors());
                }
                executorRef = new WeakReference<>(executor);
                return executorRef.get();
            }
        }

        @Override
        public Thread newThread(Runnable r) {
            ExecutorService owner = getExecutor();
            @SuppressWarnings("removal")
            Thread thread = AccessController.doPrivileged(new PrivilegedAction<Thread>() {
                @Override
                public Thread run() {
                    Thread t = InnocuousThread.newThread(new BootstrapMessageLoggerTask(owner, r));
                    t.setName("BootstrapMessageLoggerTask-"+t.getName());
                    return t;
                }
            }, null, new RuntimePermission("enableContextClassLoaderOverride"));
            thread.setDaemon(true);
            return thread;
        }

        static void submit(Runnable r) {
            getExecutor().execute(r);
        }

        static void join(Runnable r) {
            try {
                getExecutor().submit(r).get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }

        static void awaitPendingTasks() {
            WeakReference<ExecutorService> ref = executorRef;
            ExecutorService executor = ref == null ? null : ref.get();
            if (ref == null) {
                synchronized(BootstrapExecutors.class) {
                    ref = executorRef;
                    executor = ref == null ? null : ref.get();
                }
            }
            if (executor != null) {
                join(()->{});
            }
        }

        static boolean isAlive() {
            WeakReference<ExecutorService> ref = executorRef;
            if (ref != null && !ref.refersTo(null)) return true;
            synchronized (BootstrapExecutors.class) {
                ref = executorRef;
                return ref != null && !ref.refersTo(null);
            }
        }

        static LogEvent head, tail;

        static void enqueue(LogEvent event) {
            if (event.next != null) return;
            synchronized (BootstrapExecutors.class) {
                if (event.next != null) return;
                event.next = event;
                if (tail == null) {
                    head = tail = event;
                } else {
                    tail.next = event;
                    tail = event;
                }
            }
        }

        static void flush() {
            LogEvent event;
            synchronized(BootstrapExecutors.class) {
                event = head;
                head = tail = null;
            }
            while(event != null) {
                LogEvent.log(event);
                synchronized(BootstrapExecutors.class) {
                    LogEvent prev = event;
                    event = (event.next == event ? null : event.next);
                    prev.next = null;
                }
            }
        }
    }

    final LazyLoggerAccessor holder;
    final BooleanSupplier isLoadingThread;

    boolean isLoadingThread() {
        return isLoadingThread != null && isLoadingThread.getAsBoolean();
    }

    BootstrapLogger(LazyLoggerAccessor holder, BooleanSupplier isLoadingThread) {
        this.holder = holder;
        this.isLoadingThread = isLoadingThread;
    }

    static final class LogEvent {
        final Level level;
        final PlatformLogger.Level platformLevel;
        final BootstrapLogger bootstrap;

        final ResourceBundle bundle;
        final String msg;
        final Throwable thrown;
        final Object[] params;
        final Supplier<String> msgSupplier;
        final String sourceClass;
        final String sourceMethod;
        final long timeMillis;
        final long nanoAdjustment;

        @SuppressWarnings("removal")
        final AccessControlContext acc;

        LogEvent next;

        @SuppressWarnings("removal")
        private LogEvent(BootstrapLogger bootstrap, Level level,
                ResourceBundle bundle, String msg,
                Throwable thrown, Object[] params) {
            this.acc = AccessController.getContext();
            this.timeMillis = System.currentTimeMillis();
            this.nanoAdjustment = VM.getNanoTimeAdjustment(timeMillis);
            this.level = level;
            this.platformLevel = null;
            this.bundle = bundle;
            this.msg = msg;
            this.msgSupplier = null;
            this.thrown = thrown;
            this.params = params;
            this.sourceClass = null;
            this.sourceMethod = null;
            this.bootstrap = bootstrap;
        }

        @SuppressWarnings("removal")
        private LogEvent(BootstrapLogger bootstrap, Level level,
                Supplier<String> msgSupplier,
                Throwable thrown, Object[] params) {
            this.acc = AccessController.getContext();
            this.timeMillis = System.currentTimeMillis();
            this.nanoAdjustment = VM.getNanoTimeAdjustment(timeMillis);
            this.level = level;
            this.platformLevel = null;
            this.bundle = null;
            this.msg = null;
            this.msgSupplier = msgSupplier;
            this.thrown = thrown;
            this.params = params;
            this.sourceClass = null;
            this.sourceMethod = null;
            this.bootstrap = bootstrap;
        }

        @SuppressWarnings("removal")
        private LogEvent(BootstrapLogger bootstrap,
                PlatformLogger.Level platformLevel,
                String sourceClass, String sourceMethod,
                ResourceBundle bundle, String msg,
                Throwable thrown, Object[] params) {
            this.acc = AccessController.getContext();
            this.timeMillis = System.currentTimeMillis();
            this.nanoAdjustment = VM.getNanoTimeAdjustment(timeMillis);
            this.level = null;
            this.platformLevel = platformLevel;
            this.bundle = bundle;
            this.msg = msg;
            this.msgSupplier = null;
            this.thrown = thrown;
            this.params = params;
            this.sourceClass = sourceClass;
            this.sourceMethod = sourceMethod;
            this.bootstrap = bootstrap;
        }

        @SuppressWarnings("removal")
        private LogEvent(BootstrapLogger bootstrap,
                PlatformLogger.Level platformLevel,
                String sourceClass, String sourceMethod,
                Supplier<String> msgSupplier,
                Throwable thrown, Object[] params) {
            this.acc = AccessController.getContext();
            this.timeMillis = System.currentTimeMillis();
            this.nanoAdjustment = VM.getNanoTimeAdjustment(timeMillis);
            this.level = null;
            this.platformLevel = platformLevel;
            this.bundle = null;
            this.msg = null;
            this.msgSupplier = msgSupplier;
            this.thrown = thrown;
            this.params = params;
            this.sourceClass = sourceClass;
            this.sourceMethod = sourceMethod;
            this.bootstrap = bootstrap;
        }

        private void log(Logger logger) {
            assert platformLevel == null && level != null;
            if (msgSupplier != null) {
                if (thrown != null) {
                    logger.log(level, msgSupplier, thrown);
                } else {
                    logger.log(level, msgSupplier);
                }
            } else {
                if (thrown != null) {
                    logger.log(level, bundle, msg, thrown);
                } else {
                    logger.log(level, bundle, msg, params);
                }
            }
        }

        private void log(PlatformLogger.Bridge logger) {
            assert platformLevel != null && level == null;
            if (sourceClass == null) {
                if (msgSupplier != null) {
                    if (thrown != null) {
                        logger.log(platformLevel, thrown, msgSupplier);
                    } else {
                        logger.log(platformLevel, msgSupplier);
                    }
                } else {
                    if (thrown != null) {
                        logger.logrb(platformLevel, bundle, msg, thrown);
                    } else {
                        logger.logrb(platformLevel, bundle, msg, params);
                    }
                }
            } else {
                if (msgSupplier != null) {
                    if (thrown != null) {
                        logger.logp(platformLevel, sourceClass, sourceMethod, thrown, msgSupplier);
                    } else {
                        logger.logp(platformLevel, sourceClass, sourceMethod, msgSupplier);
                    }
                } else {
                    if (thrown != null) {
                        logger.logrb(platformLevel, sourceClass, sourceMethod, bundle, msg, thrown);
                    } else {
                        logger.logrb(platformLevel, sourceClass, sourceMethod, bundle, msg, params);
                    }
                }
            }
        }

        static LogEvent valueOf(BootstrapLogger bootstrap, Level level,
                ResourceBundle bundle, String key, Throwable thrown) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), bundle, key,
                                thrown, null);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, Level level,
                ResourceBundle bundle, String format, Object[] params) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), bundle, format,
                                null, params);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, Level level,
                                Supplier<String> msgSupplier, Throwable thrown) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                    Objects.requireNonNull(level),
                    Objects.requireNonNull(msgSupplier), thrown, null);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, Level level,
                                Supplier<String> msgSupplier) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level),
                                Objects.requireNonNull(msgSupplier), null, null);
        }
        @SuppressWarnings("removal")
        static void log(LogEvent log, Logger logger) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm == null || log.acc == null) {
                BootstrapExecutors.submit(() -> log.log(logger));
            } else {
                BootstrapExecutors.submit(() ->
                    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                        log.log(logger); return null;
                    }, log.acc));
            }
        }

        static LogEvent valueOf(BootstrapLogger bootstrap,
                                PlatformLogger.Level level, String msg) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), null, null, null,
                                msg, null, null);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                                String msg, Throwable thrown) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                    Objects.requireNonNull(level), null, null, null, msg, thrown, null);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                                String msg, Object[] params) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                    Objects.requireNonNull(level), null, null, null, msg, null, params);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                                Supplier<String> msgSupplier) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                    Objects.requireNonNull(level), null, null, msgSupplier, null, null);
        }
        static LogEvent vaueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                               Supplier<String> msgSupplier,
                               Throwable thrown) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), null, null,
                                msgSupplier, thrown, null);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                                String sourceClass, String sourceMethod,
                                ResourceBundle bundle, String msg, Object[] params) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), sourceClass,
                                sourceMethod, bundle, msg, null, params);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                                String sourceClass, String sourceMethod,
                                ResourceBundle bundle, String msg, Throwable thrown) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), sourceClass,
                                sourceMethod, bundle, msg, thrown, null);
        }
        static LogEvent valueOf(BootstrapLogger bootstrap, PlatformLogger.Level level,
                                String sourceClass, String sourceMethod,
                                Supplier<String> msgSupplier, Throwable thrown) {
            return new LogEvent(Objects.requireNonNull(bootstrap),
                                Objects.requireNonNull(level), sourceClass,
                                sourceMethod, msgSupplier, thrown, null);
        }
        @SuppressWarnings("removal")
        static void log(LogEvent log, PlatformLogger.Bridge logger) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm == null || log.acc == null) {
                BootstrapExecutors.submit(() -> log.log(logger));
            } else {
                BootstrapExecutors.submit(() ->
                    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                        log.log(logger); return null;
                }, log.acc));
            }
        }

        static void log(LogEvent event) {
            event.bootstrap.flush(event);
        }

    }

    void push(LogEvent log) {
        BootstrapExecutors.enqueue(log);
        checkBootstrapping();
    }

    void flush(LogEvent event) {
        assert event.bootstrap == this;
        if (event.platformLevel != null) {
            PlatformLogger.Bridge concrete = holder.getConcretePlatformLogger(this);
            LogEvent.log(event, concrete);
        } else {
            Logger concrete = holder.getConcreteLogger(this);
            LogEvent.log(event, concrete);
        }
    }

    /**
     * The name of this logger. This is the name of the actual logger for which
     * this logger acts as a temporary proxy.
     * @return The logger name.
     */
    @Override
    public String getName() {
        return holder.name;
    }

    /**
     * Check whether the VM is still bootstrapping, and if not, arranges
     * for this logger's holder to create the real logger and flush the
     * pending event queue.
     * @return true if the VM is still bootstrapping.
     */
    boolean checkBootstrapping() {
        if (isBooted() && !isLoadingThread()) {
            BootstrapExecutors.flush();
            holder.getConcreteLogger(this);
            return false;
        }
        return true;
    }


    @Override
    public boolean isLoggable(Level level) {
        if (checkBootstrapping()) {
            return level.getSeverity() >= Level.INFO.getSeverity();
        } else {
            final Logger spi = holder.wrapped();
            return spi.isLoggable(level);
        }
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String key, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, bundle, key, thrown));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, bundle, key, thrown);
        }
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, bundle, format, params));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, bundle, format, params);
        }
    }

    @Override
    public void log(Level level, String msg, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, null, msg, thrown));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, msg, thrown);
        }
    }

    @Override
    public void log(Level level, String format, Object... params) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, null, format, params));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, format, params);
        }
    }

    @Override
    public void log(Level level, Supplier<String> msgSupplier) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, msgSupplier));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, msgSupplier);
        }
    }

    @Override
    public void log(Level level, Object obj) {
        if (checkBootstrapping()) {
            Logger.super.log(level, obj);
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, obj);
        }
    }

    @Override
    public void log(Level level, String msg) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, null, msg, (Object[])null));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, msg);
        }
    }

    @Override
    public void log(Level level, Supplier<String> msgSupplier, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, msgSupplier, thrown));
        } else {
            final Logger spi = holder.wrapped();
            spi.log(level, msgSupplier, thrown);
        }
    }


    @Override
    public boolean isLoggable(PlatformLogger.Level level) {
        if (checkBootstrapping()) {
            return level.intValue() >= PlatformLogger.Level.INFO.intValue();
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            return spi.isLoggable(level);
        }
    }

    @Override
    public boolean isEnabled() {
        if (checkBootstrapping()) {
            return true;
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            return spi.isEnabled();
        }
    }

    @Override
    public void log(PlatformLogger.Level level, String msg) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, msg));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.log(level, msg);
        }
    }

    @Override
    public void log(PlatformLogger.Level level, String msg, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, msg, thrown));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.log(level, msg, thrown);
        }
    }

    @Override
    public void log(PlatformLogger.Level level, String msg, Object... params) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, msg, params));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.log(level, msg, params);
        }
    }

    @Override
    public void log(PlatformLogger.Level level, Supplier<String> msgSupplier) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, msgSupplier));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.log(level, msgSupplier);
        }
    }

    @Override
    public void log(PlatformLogger.Level level, Throwable thrown,
            Supplier<String> msgSupplier) {
        if (checkBootstrapping()) {
            push(LogEvent.vaueOf(this, level, msgSupplier, thrown));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.log(level, thrown, msgSupplier);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, String msg) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, null,
                    msg, (Object[])null));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logp(level, sourceClass, sourceMethod, msg);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, Supplier<String> msgSupplier) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, msgSupplier, null));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logp(level, sourceClass, sourceMethod, msgSupplier);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, String msg, Object... params) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, null, msg, params));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logp(level, sourceClass, sourceMethod, msg, params);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, String msg, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, null, msg, thrown));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logp(level, sourceClass, sourceMethod, msg, thrown);
        }
    }

    @Override
    public void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, Throwable thrown, Supplier<String> msgSupplier) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, msgSupplier, thrown));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logp(level, sourceClass, sourceMethod, thrown, msgSupplier);
        }
    }

    @Override
    public void logrb(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, ResourceBundle bundle, String msg, Object... params) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, bundle, msg, params));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logrb(level, sourceClass, sourceMethod, bundle, msg, params);
        }
    }

    @Override
    public void logrb(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, ResourceBundle bundle, String msg, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, sourceClass, sourceMethod, bundle, msg, thrown));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logrb(level, sourceClass, sourceMethod, bundle, msg, thrown);
        }
    }

    @Override
    public void logrb(PlatformLogger.Level level, ResourceBundle bundle,
            String msg, Object... params) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, null, null, bundle, msg, params));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logrb(level, bundle, msg, params);
        }
    }

    @Override
    public void logrb(PlatformLogger.Level level, ResourceBundle bundle, String msg, Throwable thrown) {
        if (checkBootstrapping()) {
            push(LogEvent.valueOf(this, level, null, null, bundle, msg, thrown));
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            spi.logrb(level, bundle, msg, thrown);
        }
    }

    @Override
    public LoggerConfiguration getLoggerConfiguration() {
        if (checkBootstrapping()) {
            return PlatformLogger.ConfigurableBridge.super.getLoggerConfiguration();
        } else {
            final PlatformLogger.Bridge spi = holder.platform();
            return PlatformLogger.ConfigurableBridge.getLoggerConfiguration(spi);
        }
    }

    private static volatile BooleanSupplier isBooted;
    public static boolean isBooted() {
        if (isBooted != null) return isBooted.getAsBoolean();
        else return VM.isBooted();
    }

    private static enum LoggingBackend {
        NONE(true),

        JUL_DEFAULT(false),

        JUL_WITH_CONFIG(true),

        CUSTOM(true);

        final boolean useLoggerFinder;
        private LoggingBackend(boolean useLoggerFinder) {
            this.useLoggerFinder = useLoggerFinder;
        }
    };

    @SuppressWarnings("removal")
    private static final class DetectBackend {
        static final LoggingBackend detectedBackend;
        static {
            detectedBackend = AccessController.doPrivileged(new PrivilegedAction<LoggingBackend>() {
                    @Override
                    public LoggingBackend run() {
                        final Iterator<LoggerFinder> iterator =
                            ServiceLoader.load(LoggerFinder.class, ClassLoader.getSystemClassLoader())
                            .iterator();
                        if (iterator.hasNext()) {
                            return LoggingBackend.CUSTOM; 
                        }
                        final Iterator<DefaultLoggerFinder> iterator2 =
                            ServiceLoader.loadInstalled(DefaultLoggerFinder.class)
                            .iterator();
                        if (iterator2.hasNext()) {
                            String cname = System.getProperty("java.util.logging.config.class");
                            String fname = System.getProperty("java.util.logging.config.file");
                            return (cname != null || fname != null)
                                ? LoggingBackend.JUL_WITH_CONFIG
                                : LoggingBackend.JUL_DEFAULT;
                        } else {
                            return LoggingBackend.NONE;
                        }
                    }
                });

        }
    }

    private static  boolean useSurrogateLoggers() {
        if (!isBooted()) return true;
        return DetectBackend.detectedBackend == LoggingBackend.JUL_DEFAULT
                && !logManagerConfigured;
    }

    public static boolean useLazyLoggers() {
        if (!BootstrapLogger.isBooted() ||
                DetectBackend.detectedBackend == LoggingBackend.CUSTOM) {
            return true;
        }
        synchronized (BootstrapLogger.class) {
            return useSurrogateLoggers();
        }
    }

    static Logger getLogger(LazyLoggerAccessor accessor, BooleanSupplier isLoading) {
        if (!BootstrapLogger.isBooted() || isLoading != null && isLoading.getAsBoolean()) {
            return new BootstrapLogger(accessor, isLoading);
        } else {
            if (useSurrogateLoggers()) {
                synchronized(BootstrapLogger.class) {
                    if (useSurrogateLoggers()) {
                        return createSurrogateLogger(accessor);
                    }
                }
            }
            return accessor.createLogger();
        }
    }

    static void ensureBackendDetected() {
        assert VM.isBooted() : "VM is not booted";
        var backend = DetectBackend.detectedBackend;
    }

    static final class RedirectedLoggers implements
            Function<LazyLoggerAccessor, SurrogateLogger> {

        final Map<LazyLoggerAccessor, SurrogateLogger> redirectedLoggers =
                new HashMap<>();

        boolean cleared;

        @Override
        public SurrogateLogger apply(LazyLoggerAccessor t) {
            if (cleared) throw new IllegalStateException("LoggerFinder already initialized");
            return SurrogateLogger.makeSurrogateLogger(t.getLoggerName());
        }

        SurrogateLogger get(LazyLoggerAccessor a) {
            if (cleared) throw new IllegalStateException("LoggerFinder already initialized");
            return redirectedLoggers.computeIfAbsent(a, this);
        }

        Map<LazyLoggerAccessor, SurrogateLogger> drainLoggersMap() {
            if (redirectedLoggers.isEmpty()) return null;
            if (cleared) throw new IllegalStateException("LoggerFinder already initialized");
            final Map<LazyLoggerAccessor, SurrogateLogger> accessors = new HashMap<>(redirectedLoggers);
            redirectedLoggers.clear();
            cleared = true;
            return accessors;
        }

        static void replaceSurrogateLoggers(Map<LazyLoggerAccessor, SurrogateLogger> accessors) {
            final LoggingBackend detectedBackend = DetectBackend.detectedBackend;
            final boolean lazy = detectedBackend != LoggingBackend.JUL_DEFAULT
                    && detectedBackend != LoggingBackend.JUL_WITH_CONFIG;
            for (Map.Entry<LazyLoggerAccessor, SurrogateLogger> a : accessors.entrySet()) {
                a.getKey().release(a.getValue(), !lazy);
            }
        }

        static final RedirectedLoggers INSTANCE = new RedirectedLoggers();
    }

    static synchronized Logger createSurrogateLogger(LazyLoggerAccessor a) {
        return RedirectedLoggers.INSTANCE.get(a);
    }

    private static volatile boolean logManagerConfigured;

    private static synchronized Map<LazyLoggerAccessor, SurrogateLogger>
         releaseSurrogateLoggers() {
        final boolean releaseSurrogateLoggers = useSurrogateLoggers();

        logManagerConfigured = true;

        if (releaseSurrogateLoggers) {
            return RedirectedLoggers.INSTANCE.drainLoggersMap();
        } else {
            return null;
        }
    }

    public static void redirectTemporaryLoggers() {
        final Map<LazyLoggerAccessor, SurrogateLogger> accessors =
                releaseSurrogateLoggers();

        if (accessors != null) {
            RedirectedLoggers.replaceSurrogateLoggers(accessors);
        }

        BootstrapExecutors.flush();
    }

    static void awaitPendingTasks() {
        BootstrapExecutors.awaitPendingTasks();
    }
    static boolean isAlive() {
        return BootstrapExecutors.isAlive();
    }

}
