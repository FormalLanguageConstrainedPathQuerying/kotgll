/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import resources.ListBundle;

/**
 * @test
 * @bug 8013839
 * @summary tests Logger.logrb(..., ResourceBundle);
 * @build TestLogrbResourceBundle resources.ListBundle resources.ListBundle_fr
 * @run main TestLogrbResourceBundle
 * @author danielfuchs
 */
public class TestLogrbResourceBundle {

    static final String LIST_BUNDLE_NAME = "resources.ListBundle";
    static final String PROPERTY_BUNDLE_NAME = "resources.PropertyBundle";

    /**
     * A dummy handler class that we can use to check the bundle/bundle name
     * that was present in the last LogRecord instance published.
     */
    static final class TestHandler extends Handler {
        ResourceBundle lastBundle = null;
        String lastBundleName = null;
        Object[] lastParams = null;
        Throwable lastThrown = null;
        String lastMessage = null;
        @Override
        public void publish(LogRecord record) {
            lastBundle = record.getResourceBundle();
            lastBundleName = record.getResourceBundleName();
            lastParams = record.getParameters();
            lastThrown = record.getThrown();
            lastMessage = record.getMessage();
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    /**
     * We're going to do the same test with each of the different new logrb
     * forms.
     * <ul>
     *    <li> LOGRB_NO_ARGS: calling logrb with no message argument.
     *    <li> LOGRB_SINGLE_ARG: calling logrb with a single message argument.
     *    <li> LOGRB_ARG_ARRAY: calling logrb with an array of message arguments.
     *    <li> LOGRB_VARARGS: calling logrb with a variable list of message arguments.
     *    <li> LOGRB_THROWABLE: calling logrb with an exception.
     * </ul>
     */
    private static enum TestCase {
        LOGRB_NO_ARGS, LOGRB_SINGLE_ARG, LOGRB_ARG_ARRAY, LOGRB_VARARGS, LOGRB_THROWABLE;

        public void logrb(Logger logger, ResourceBundle bundle) {
            switch(this) {
                case LOGRB_NO_ARGS:
                    logger.logrb(Level.CONFIG,
                            TestLogrbResourceBundle.class.getName(),
                            "main", bundle, "dummy");
                    break;
                case LOGRB_SINGLE_ARG:
                    logger.logrb(Level.CONFIG,
                            TestLogrbResourceBundle.class.getName(),
                            "main", bundle, "dummy", "bar");
                    break;
                case LOGRB_ARG_ARRAY:
                    logger.logrb(Level.CONFIG,
                            TestLogrbResourceBundle.class.getName(),
                            "main", bundle, "dummy",
                            new Object[] { "bar", "baz"} );
                    break;
                case LOGRB_VARARGS:
                    logger.logrb(Level.CONFIG,
                            TestLogrbResourceBundle.class.getName(),
                            "main", bundle, "dummy",
                            "bar", "baz" );
                    break;
                case LOGRB_THROWABLE:
                    logger.logrb(Level.CONFIG,
                            TestLogrbResourceBundle.class.getName(),
                            "main", bundle, "dummy",
                            new Exception("dummy exception") );
                    break;
                default:
            }
        }

        /**
         * Checks that the last published logged record had the expected data.
         * @param handler the TestHandler through which the record was published.
         */
        public void checkLogged(TestHandler handler) {
            checkLogged(handler.lastMessage, handler.lastParams, handler.lastThrown);
        }

        private void checkLogged(String message, Object[] parameters, Throwable thrown) {
            switch(this) {
                case LOGRB_NO_ARGS:
                    if ("dummy".equals(message) && thrown == null
                            && (parameters == null || parameters.length == 0)) {
                        return; 
                    }
                    break;
                case LOGRB_SINGLE_ARG:
                    if ("dummy".equals(message) && thrown == null
                            && parameters != null
                            && parameters.length == 1
                            && "bar".equals(parameters[0])) {
                        return; 
                    }
                    break;
                case LOGRB_VARARGS:
                case LOGRB_ARG_ARRAY:
                    if ("dummy".equals(message) && thrown == null
                            && parameters != null
                            && parameters.length > 1
                            && Arrays.deepEquals(new Object[] { "bar", "baz"},
                                    parameters)) {
                        return; 
                    }
                    break;
                case LOGRB_THROWABLE:
                    if ("dummy".equals(message) && thrown != null
                            && thrown.getClass() == Exception.class
                            && "dummy exception".equals(thrown.getMessage())) {
                        return; 
                    }
                    break;
                default:
            }

            throw new RuntimeException(this + ": "
                    + "Unexpected content in last published log record: "
                    + "\n\tmessage=\"" + message + "\""
                    + "\n\tparameters=" + Arrays.toString(parameters)
                    + "\n\tthrown=" + thrown);
        }
    }

    static String getBaseName(ResourceBundle bundle) {
        return bundle == null ? null : bundle.getBaseBundleName();
    }

    public static void main(String... args) throws Exception {

        Locale defaultLocale = Locale.getDefault();

        final ResourceBundle bundle = ResourceBundle.getBundle(LIST_BUNDLE_NAME);
        final ResourceBundle bundle_fr =
                    ResourceBundle.getBundle(LIST_BUNDLE_NAME, Locale.FRENCH);
        final ResourceBundle propertyBundle = ResourceBundle.getBundle(PROPERTY_BUNDLE_NAME);
        final ResourceBundle propertyBundle_fr =
                    ResourceBundle.getBundle(PROPERTY_BUNDLE_NAME, Locale.FRENCH);
        Logger foobar = Logger.getLogger("foo.bar");
        final TestHandler handler = new TestHandler();
        foobar.addHandler(handler);
        foobar.setLevel(Level.CONFIG);

        final ResourceBundle anonBundle = new ListBundle();
        try {

            for (TestCase test : TestCase.values()) {
                for (ResourceBundle b : new ResourceBundle[] {
                    anonBundle, bundle, bundle_fr, propertyBundle,
                    anonBundle, null, propertyBundle_fr,
                }) {
                    final String baseName = getBaseName(b);
                    System.out.println("Testing " + test + " with " + baseName);

                    test.logrb(foobar, b);

                    if (handler.lastBundle != b) {
                        throw new RuntimeException("Unexpected bundle: "
                                + handler.lastBundle);
                    }

                    if (!Objects.equals(handler.lastBundleName, baseName)) {
                        throw new RuntimeException("Unexpected bundle name: "
                                + handler.lastBundleName);
                    }

                    if (foobar.getResourceBundle() != null) {
                        throw new RuntimeException("Unexpected bundle: "
                            + foobar.getResourceBundle());
                    }
                    if (foobar.getResourceBundleName() != null) {
                        throw new RuntimeException("Unexpected bundle: "
                            + foobar.getResourceBundleName());
                    }

                    test.checkLogged(handler);
                }
            }


            for (ResourceBundle propBundle : new ResourceBundle[] {
                propertyBundle, propertyBundle_fr,
            }) {

                foobar.setResourceBundle(propBundle);

                if (!propBundle.equals(foobar.getResourceBundle())) {
                    throw new RuntimeException("Unexpected bundle: "
                            + foobar.getResourceBundle());
                }
                if (!Objects.equals(getBaseName(propBundle), foobar.getResourceBundleName())) {
                    throw new RuntimeException("Unexpected bundle name: "
                            + foobar.getResourceBundleName());
                }

                System.out.println("Configuring " + foobar.getName() + " with "
                        + propBundle);

                for (TestCase test : TestCase.values()) {

                    for (ResourceBundle b : new ResourceBundle[] {
                        anonBundle, bundle, null, bundle_fr, propertyBundle,
                        anonBundle, propertyBundle_fr,
                    }) {

                        final String baseName = getBaseName(b);
                        System.out.println("Testing " + test + " with " + baseName);

                        test.logrb(foobar, b);

                        if (handler.lastBundle != b) {
                            throw new RuntimeException("Unexpected bundle: "
                                    + handler.lastBundle);
                        }
                        if (!Objects.equals(handler.lastBundleName, baseName)) {
                            throw new RuntimeException("Unexpected bundle name: "
                                    + handler.lastBundleName);
                        }

                        if (foobar.getResourceBundle() != propBundle) {
                            throw new RuntimeException("Unexpected bundle: "
                                + foobar.getResourceBundle());
                        }
                        if (!Objects.equals(getBaseName(propBundle),
                                foobar.getResourceBundleName())) {
                            throw new RuntimeException("Unexpected bundle name: "
                                + foobar.getResourceBundleName());
                        }

                        test.checkLogged(handler);
                    }
                }
            }

            Logger foobaz = Logger.getLogger("foo.bar.baz");

            Reference.reachabilityFence(foobar);

            if (foobaz.getResourceBundle() != null) {
                throw new RuntimeException("Unexpected bundle: "
                        + foobaz.getResourceBundle());
            }
            if (foobaz.getResourceBundleName() != null) {
                throw new RuntimeException("Unexpected bundle: "
                        + foobaz.getResourceBundle());
            }

            Locale.setDefault(Locale.GERMAN); 

            for (TestCase test : TestCase.values()) {

                for (ResourceBundle b : new ResourceBundle[] {
                    anonBundle, bundle, bundle_fr, propertyBundle, null,
                     anonBundle, propertyBundle_fr,
                }) {
                    final String baseName = getBaseName(b);
                    System.out.println("Testing " + test + " with "
                            + foobaz.getName() + " and "
                            + baseName);

                    test.logrb(foobaz, b);

                    if (handler.lastBundle != b) {
                        throw new RuntimeException("Unexpected bundle: "
                                + handler.lastBundle);
                    }
                    if (!Objects.equals(handler.lastBundleName, baseName)) {
                        throw new RuntimeException("Unexpected bundle name: "
                                + handler.lastBundleName);
                    }

                    if (foobaz.getResourceBundle() != null) {
                        throw new RuntimeException("Unexpected bundle: "
                            + foobaz.getResourceBundle());
                    }
                    if (foobaz.getResourceBundleName() != null) {
                        throw new RuntimeException("Unexpected bundle: "
                            + foobaz.getResourceBundleName());
                    }

                    test.checkLogged(handler);
                }
            }

        } finally {
            Locale.setDefault(defaultLocale);
        }

    }
}
