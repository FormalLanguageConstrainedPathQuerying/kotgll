/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.StackWalker.StackFrame;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.lang.System.Logger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import sun.security.action.GetPropertyAction;
import sun.util.logging.PlatformLogger;
import sun.util.logging.PlatformLogger.ConfigurableBridge.LoggerConfiguration;

/**
 * A simple console logger to emulate the behavior of JUL loggers when
 * in the default configuration. SimpleConsoleLoggers are also used when
 * JUL is not present and no DefaultLoggerFinder is installed.
 */
public class SimpleConsoleLogger extends LoggerConfiguration
    implements Logger, PlatformLogger.Bridge, PlatformLogger.ConfigurableBridge {

    static final Level DEFAULT_LEVEL = getDefaultLevel();
    static final PlatformLogger.Level DEFAULT_PLATFORM_LEVEL =
            PlatformLogger.toPlatformLevel(DEFAULT_LEVEL);

    static Level getDefaultLevel() {
        String levelName = GetPropertyAction
                .privilegedGetProperty("jdk.system.logger.level", "INFO");
        try {
            return Level.valueOf(levelName);
        } catch (IllegalArgumentException iae) {
            return Level.INFO;
        }
    }

    final String name;
    volatile PlatformLogger.Level  level;
    final boolean usePlatformLevel;
    SimpleConsoleLogger(String name, boolean usePlatformLevel) {
        this.name = name;
        this.usePlatformLevel = usePlatformLevel;
    }

    String getSimpleFormatString() {
        return Formatting.SIMPLE_CONSOLE_LOGGER_FORMAT;
    }

    PlatformLogger.Level defaultPlatformLevel() {
        return DEFAULT_PLATFORM_LEVEL;
    }

    @Override
    public final String getName() {
        return name;
    }

    private Enum<?> logLevel(PlatformLogger.Level level) {
        return usePlatformLevel ? level : level.systemLevel();
    }

    private Enum<?> logLevel(Level level) {
        return usePlatformLevel ? PlatformLogger.toPlatformLevel(level) : level;
    }


    @Override
    public final boolean isLoggable(Level level) {
        return isLoggable(PlatformLogger.toPlatformLevel(level));
    }

    @Override
    public final void log(Level level, ResourceBundle bundle, String key, Throwable thrown) {
        if (isLoggable(level)) {
            if (bundle != null) {
                key = getString(bundle, key);
            }
            publish(getCallerInfo(), logLevel(level), key, thrown);
        }
    }

    @Override
    public final void log(Level level, ResourceBundle bundle, String format, Object... params) {
        if (isLoggable(level)) {
            if (bundle != null) {
                format = getString(bundle, format);
            }
            publish(getCallerInfo(), logLevel(level), format, params);
        }
    }


    @Override
    public final boolean isLoggable(PlatformLogger.Level level) {
        final PlatformLogger.Level effectiveLevel =  effectiveLevel();
        return level != PlatformLogger.Level.OFF
                && level.ordinal() >= effectiveLevel.ordinal();
    }

    @Override
    public final boolean isEnabled() {
        return level != PlatformLogger.Level.OFF;
    }

    @Override
    public final void log(PlatformLogger.Level level, String msg) {
        if (isLoggable(level)) {
            publish(getCallerInfo(), logLevel(level), msg);
        }
    }

    @Override
    public final void log(PlatformLogger.Level level, String msg, Throwable thrown) {
        if (isLoggable(level)) {
            publish(getCallerInfo(), logLevel(level), msg, thrown);
        }
    }

    @Override
    public final void log(PlatformLogger.Level level, String msg, Object... params) {
        if (isLoggable(level)) {
            publish(getCallerInfo(), logLevel(level), msg, params);
        }
    }

    private PlatformLogger.Level effectiveLevel() {
        if (level == null) return defaultPlatformLevel();
        return level;
    }

    @Override
    public final PlatformLogger.Level getPlatformLevel() {
        return level;
    }

    @Override
    public final void setPlatformLevel(PlatformLogger.Level newLevel) {
        level = newLevel;
    }

    @Override
    public final LoggerConfiguration getLoggerConfiguration() {
        return this;
    }

    /**
     * Default platform logging support - output messages to System.err -
     * equivalent to ConsoleHandler with SimpleFormatter.
     */
    static PrintStream outputStream() {
        return System.err;
    }

    private String getCallerInfo() {
        Optional<StackWalker.StackFrame> frame = new CallerFinder().get();
        if (frame.isPresent()) {
            return frame.get().getClassName() + " " + frame.get().getMethodName();
        } else {
            return name;
        }
    }

    /*
     * CallerFinder is a stateful predicate.
     */
    @SuppressWarnings("removal")
    static final class CallerFinder implements Predicate<StackWalker.StackFrame> {
        private static final StackWalker WALKER;
        static {
            final PrivilegedAction<StackWalker> action = new PrivilegedAction<>() {
                @Override
                public StackWalker run() {
                    return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
                }
            };
            WALKER = AccessController.doPrivileged(action);
        }

        /**
         * Returns StackFrame of the caller's frame.
         * @return StackFrame of the caller's frame.
         */
        Optional<StackWalker.StackFrame> get() {
            return WALKER.walk((s) -> s.filter(this).findFirst());
        }

        private boolean lookingForLogger = true;
        /**
         * Returns true if we have found the caller's frame, false if the frame
         * must be skipped.
         *
         * @param t The frame info.
         * @return true if we have found the caller's frame, false if the frame
         * must be skipped.
         */
        @Override
        public boolean test(StackWalker.StackFrame t) {
            final String cname = t.getClassName();
            if (lookingForLogger) {
                lookingForLogger = !isLoggerImplFrame(cname);
                return false;
            }
            return !Formatting.isFilteredFrame(t);
        }

        private boolean isLoggerImplFrame(String cname) {
            return (cname.equals("sun.util.logging.PlatformLogger") ||
                    cname.equals("jdk.internal.logger.SimpleConsoleLogger"));
        }
    }

    private String getCallerInfo(String sourceClassName, String sourceMethodName) {
        if (sourceClassName == null) return name;
        if (sourceMethodName == null) return sourceClassName;
        return sourceClassName + " " + sourceMethodName;
    }

    private String toString(Throwable thrown) {
        String throwable = "";
        if (thrown != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            thrown.printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }
        return throwable;
    }

    private synchronized String format(Enum<?> level,
            String msg, Throwable thrown, String callerInfo) {

        ZonedDateTime zdt = ZonedDateTime.now();
        String throwable = toString(thrown);

        return String.format(getSimpleFormatString(),
                         zdt,
                         callerInfo,
                         name,
                         level.name(),
                         msg,
                         throwable);
    }

    private void publish(String callerInfo, Enum<?> level, String msg) {
        outputStream().print(format(level, msg, null, callerInfo));
    }
    private void publish(String callerInfo, Enum<?> level, String msg, Throwable thrown) {
        outputStream().print(format(level, msg, thrown, callerInfo));
    }
    private void publish(String callerInfo, Enum<?> level, String msg, Object... params) {
        msg = params == null || params.length == 0 ? msg
                : Formatting.formatMessage(msg, params);
        outputStream().print(format(level, msg, null, callerInfo));
    }

    public static SimpleConsoleLogger makeSimpleLogger(String name) {
        return new SimpleConsoleLogger(name, false);
    }

    @Override
    public final void log(PlatformLogger.Level level, Supplier<String> msgSupplier) {
        if (isLoggable(level)) {
            publish(getCallerInfo(), logLevel(level), msgSupplier.get());
        }
    }

    @Override
    public final void log(PlatformLogger.Level level, Throwable thrown,
            Supplier<String> msgSupplier) {
        if (isLoggable(level)) {
            publish(getCallerInfo(), logLevel(level), msgSupplier.get(), thrown);
        }
    }

    @Override
    public final void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, String msg) {
        if (isLoggable(level)) {
            publish(getCallerInfo(sourceClass, sourceMethod), logLevel(level), msg);
        }
    }

    @Override
    public final void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, Supplier<String> msgSupplier) {
        if (isLoggable(level)) {
            publish(getCallerInfo(sourceClass, sourceMethod), logLevel(level), msgSupplier.get());
        }
    }

    @Override
    public final void logp(PlatformLogger.Level level, String sourceClass, String sourceMethod,
            String msg, Object... params) {
        if (isLoggable(level)) {
            publish(getCallerInfo(sourceClass, sourceMethod), logLevel(level), msg, params);
        }
    }

    @Override
    public final void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, String msg, Throwable thrown) {
        if (isLoggable(level)) {
            publish(getCallerInfo(sourceClass, sourceMethod), logLevel(level), msg, thrown);
        }
    }

    @Override
    public final void logp(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, Throwable thrown, Supplier<String> msgSupplier) {
        if (isLoggable(level)) {
            publish(getCallerInfo(sourceClass, sourceMethod), logLevel(level), msgSupplier.get(), thrown);
        }
    }

    @Override
    public final void logrb(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, ResourceBundle bundle, String key, Object... params) {
        if (isLoggable(level)) {
            String msg = bundle == null ? key : getString(bundle, key);
            publish(getCallerInfo(sourceClass, sourceMethod), logLevel(level), msg, params);
        }
    }

    @Override
    public final void logrb(PlatformLogger.Level level, String sourceClass,
            String sourceMethod, ResourceBundle bundle, String key, Throwable thrown) {
        if (isLoggable(level)) {
            String msg = bundle == null ? key : getString(bundle, key);
            publish(getCallerInfo(sourceClass, sourceMethod), logLevel(level), msg, thrown);
        }
    }

    @Override
    public final void logrb(PlatformLogger.Level level, ResourceBundle bundle,
            String key, Object... params) {
        if (isLoggable(level)) {
            String msg = bundle == null ? key : getString(bundle,key);
            publish(getCallerInfo(), logLevel(level), msg, params);
        }
    }

    @Override
    public final void logrb(PlatformLogger.Level level, ResourceBundle bundle,
            String key, Throwable thrown) {
        if (isLoggable(level)) {
            String msg = bundle == null ? key : getString(bundle,key);
            publish(getCallerInfo(), logLevel(level), msg, thrown);
        }
    }

    static String getString(ResourceBundle bundle, String key) {
        if (bundle == null || key == null) return key;
        try {
            return bundle.getString(key);
        } catch (MissingResourceException x) {
            return key;
        }
    }

    static final class Formatting {
        static final String DEFAULT_FORMAT =
            "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %2$s%n%4$s: %5$s%6$s%n";

        static final String DEFAULT_FORMAT_PROP_KEY =
            "jdk.system.logger.format";

        static final String JUL_FORMAT_PROP_KEY =
            "java.util.logging.SimpleFormatter.format";

        static final String SIMPLE_CONSOLE_LOGGER_FORMAT =
                getSimpleFormat(DEFAULT_FORMAT_PROP_KEY, null);

        private static final String[] skips;
        static {
            String additionalPkgs =
                    GetPropertyAction.privilegedGetProperty("jdk.logger.packages");
            skips = additionalPkgs == null ? new String[0] : additionalPkgs.split(",");
        }

        static boolean isFilteredFrame(StackFrame st) {
            if (System.Logger.class.isAssignableFrom(st.getDeclaringClass())) {
                return true;
            }

            final String cname = st.getClassName();
            char c = cname.length() < 12 ? 0 : cname.charAt(0);
            if (c == 's') {
                if (cname.startsWith("sun.util.logging."))   return true;
                if (cname.startsWith("sun.rmi.runtime.Log")) return true;
            } else if (c == 'j') {
                if (cname.startsWith("jdk.internal.logger.BootstrapLogger$LogEvent")) return false;
                if (cname.startsWith("jdk.internal.logger."))          return true;
                if (cname.startsWith("java.util.logging."))            return true;
                if (cname.startsWith("java.lang.invoke.MethodHandle")) return true;
                if (cname.startsWith("java.security.AccessController")) return true;
            }

            if (skips.length > 0) {
                for (int i=0; i<skips.length; i++) {
                    if (!skips[i].isEmpty() && cname.startsWith(skips[i])) {
                        return true;
                    }
                }
            }

            return false;
        }

        static String getSimpleFormat(String key, Function<String, String> defaultPropertyGetter) {
            if (!DEFAULT_FORMAT_PROP_KEY.equals(key)
                    && !JUL_FORMAT_PROP_KEY.equals(key)) {
                throw new IllegalArgumentException("Invalid property name: " + key);
            }

            String format = GetPropertyAction.privilegedGetProperty(key);

            if (format == null && defaultPropertyGetter != null) {
                format = defaultPropertyGetter.apply(key);
            }
            if (format != null) {
                try {
                    String.format(format, ZonedDateTime.now(), "", "", "", "", "");
                } catch (IllegalArgumentException e) {
                    format = DEFAULT_FORMAT;
                }
            } else {
                format = DEFAULT_FORMAT;
            }
            return format;
        }


        static String formatMessage(String format, Object... parameters) {
            try {
                if (parameters == null || parameters.length == 0) {
                    return format;
                }
                boolean isJavaTestFormat = false;
                final int len = format.length();
                for (int i=0; i<len-2; i++) {
                    final char c = format.charAt(i);
                    if (c == '{') {
                        final int d = format.charAt(i+1);
                        if (d >= '0' && d <= '9') {
                            isJavaTestFormat = true;
                            break;
                        }
                    }
                }
                if (isJavaTestFormat) {
                    return java.text.MessageFormat.format(format, parameters);
                }
                return format;
            } catch (Exception ex) {
                return format;
            }
        }
    }
}
