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

import java.net.URLPermission;
import java.io.*;

/**
 * @test
 * @bug 8010464 8027570 8027687 8029354 8114860 8071660 8161291 8294378
 * @run main URLPermissionTest
 * @run main/othervm -Duser.language=tr URLPermissionTest
 */

public class URLPermissionTest {

    abstract static class Test {
        boolean expected;
        abstract boolean execute();
    };

    static class CreateTest extends Test {
        String arg;
        CreateTest(String arg) {
            this.arg = arg;
        }

        @Override
        boolean execute() {
            try {
                URLPermission p = new URLPermission(arg);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    };

    static CreateTest createtest(String arg) {
        return new CreateTest(arg);
    }


    static class ExTest extends Test {
        String arg;
        ExTest(String arg) {
            this.arg = arg;
        }

        @Override
        boolean execute() {
            try {
                URLPermission p = new URLPermission(arg);
                return false;
            } catch (IllegalArgumentException e) {
                return true;
            }
        }
    };

    static ExTest extest(String arg) {
        return new ExTest(arg);
    }

    static class URLImpliesTest extends Test {
        String arg1, arg2;

        URLImpliesTest(String arg1, String arg2, boolean expected) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.expected = expected;
        }

          boolean execute() {
            URLPermission p1 = new URLPermission (arg1, "GET:*");
            URLPermission p2 = new URLPermission (arg2, "GET:*");
            boolean result = p1.implies(p2);
            if (result != expected) {
                System.out.println("p1 = " + p1);
                System.out.println("p2 = " + p2);
            }
            return result == expected;
        }
    };

    static URLImpliesTest imtest(String arg1, String arg2, boolean expected) {
        return new URLImpliesTest(arg1, arg2, expected);
    }

    static class ActionImpliesTest extends Test {
        String arg1, arg2;
        String url1 = "http:
        String url2 = "http:

        ActionImpliesTest(String arg1, String arg2, boolean expected) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.expected = expected;
        }

        ActionImpliesTest(String ur11, String url2, String arg1, String arg2,
            boolean expected) {
            this.url1 = ur11;
            this.url2 = url2;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.expected = expected;
        }

        @Override
          boolean execute() {
            URLPermission p1 = new URLPermission(url1, arg1);
            URLPermission p2 = new URLPermission(url2, arg2);
            boolean result = p1.implies(p2);

            return result == expected;
        }
    }

    static ActionsStringTest actionstest(String arg, String expectedActions) {
        return new ActionsStringTest(arg, expectedActions);
    }

    static class ActionsStringTest extends Test {

        String expectedActions;
        String arg;

        public ActionsStringTest(String arg, String expectedActions) {
            this.arg = arg;
            this.expectedActions = expectedActions;
        }

        @Override
        boolean execute() {
            String url = "http:
            URLPermission urlp = new URLPermission(url, arg);
            return (expectedActions.equals(urlp.getActions()));
        }
    }

    static ActionImpliesTest actest(String arg1, String arg2, boolean expected) {
        return new ActionImpliesTest(arg1, arg2, expected);
    }

    static ActionImpliesTest actest(String url1, String url2, String arg1,
        String arg2, boolean expected) {
        return new ActionImpliesTest(url1, url2, arg1, arg2, expected);
    }

    static class HashCodeTest extends Test {
        String arg1, arg2;
        int hash;

        HashCodeTest(String arg1, String arg2, int hash) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.hash = hash;
        }

        @Override
        boolean execute() {
            URLPermission p = new URLPermission(arg1, arg2);
            int h = p.hashCode();
            return h == hash;
        }
    }

    static HashCodeTest hashtest(String arg1, String arg2, int expected) {
        return new HashCodeTest(arg1, arg2, expected);
    }

    static class URLEqualityTest extends Test {
        String arg1, arg2;

        URLEqualityTest(String arg1, String arg2, boolean expected) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.expected = expected;
        }

        @Override
          boolean execute() {
            URLPermission p1 = new URLPermission(arg1);
            URLPermission p2 = new URLPermission(arg2);
            boolean result = p1.equals(p2);

            return result == expected;
        }
    }

    static URLEqualityTest eqtest(String arg1, String arg2, boolean expected) {
        return new URLEqualityTest(arg1, arg2, expected);
    }

    static Test[] pathImplies = {
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("https:
        imtest("http:
        imtest("http:
        imtest("http:*", "https:
        imtest("http:*", "http:
        imtest("http:*", "http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
    };


    static Test[] exceptionTests = {
        extest("http:
        extest("http:
        extest("http:
        extest("http:
        extest("http:\\www.foo.com"),
        extest("http:
        extest("http:
        extest("http:
        extest("http:")
    };

    static Test[] hashTests = {
        hashtest("http:
        hashtest("http:*", "*:*", 3255810)
    };

    static Test[] pathImplies2 = {
        imtest("http:

        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:*", "http:

        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:*", "http:

        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:*", "http:

        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("http:
        imtest("https:
        imtest("https:
        imtest("https:
        imtest("https:
        imtest("http:
        imtest("http:

        imtest("https:*", "http:
        imtest("https:*", "http:*", false)
    };

    static final String FOO_URL = "http:
    static final String BAR_URL = "http:

    static Test[] actionImplies = {
        actest("GET", "GET", true),
        actest("GET", "POST", false),
        actest("GET:", "PUT", false),
        actest("GET:", "GET", true),
        actest("GET,POST", "GET", true),
        actest("GET,POST:", "GET", true),
        actest("GET:X-Foo", "GET:x-foo", true),
        actest("GET:X-Foo,X-bar", "GET:x-foo", true),
        actest("GET:X-Foo", "GET:x-boo", false),
        actest("GET:X-Foo,X-Bar", "GET:x-bar,x-foo", true),
        actest("GET:X-Bar,X-Foo,X-Bar,Y-Foo", "GET:x-bar,x-foo", true),
        actest("GET:*", "GET:x-bar,x-foo", true),
        actest("*:*", "GET:x-bar,x-foo", true),
        actest("", "GET:x-bar,x-foo", false),
        actest("GET:x-bar,x-foo", "", true),
        actest("", "", true),
        actest("GET,DELETE", "GET,DELETE:x-foo", false),
        actest(FOO_URL, BAR_URL, "", "GET:x-bar,x-foo", false),
        actest(FOO_URL, BAR_URL, "GET:x-bar,x-foo", "", false),
        actest(FOO_URL, BAR_URL, "", "", false)
    };

    static Test[] actionsStringTest = {
        actionstest("", ":"),
        actionstest(":", ":"),
        actionstest(":X-Bar", ":X-Bar"),
        actionstest("GET", "GET:"),
        actionstest("get", "GET:"),
        actionstest("GET,POST", "GET,POST:"),
        actionstest("GET,post", "GET,POST:"),
        actionstest("get,post", "GET,POST:"),
        actionstest("get,post,DELETE", "DELETE,GET,POST:"),
        actionstest("GET,POST:", "GET,POST:"),
        actionstest("GET:X-Foo,X-bar", "GET:X-Bar,X-Foo"),
        actionstest("GET,POST,DELETE:X-Bar,X-Foo,X-Bar,Y-Foo", "DELETE,GET,POST:X-Bar,X-Bar,X-Foo,Y-Foo")
    };

    static Test[] equalityTests = {
        eqtest("http:
        eqtest("http:
        eqtest("HTTP:
        eqtest("HTTP:
        eqtest("http:*", "http:*", true),
        eqtest("http:
        eqtest("http:
        eqtest("http:
        eqtest("http:
        eqtest("http:
        eqtest("https:
        eqtest("https:
        eqtest("http:
        eqtest("http:
        eqtest("http:
        eqtest("http:
        eqtest("http:
        eqtest("HTTPI:
    };

    static Test[] createTests = {
        createtest("http:
        createtest("http:
        createtest("http:
        createtest("http:
        createtest("http:
    };

    static boolean failed = false;

    public static void main(String args[]) throws Exception {
        for (int i=0; i<pathImplies.length ; i++) {
            URLImpliesTest test = (URLImpliesTest)pathImplies[i];
            Exception caught = null;
            boolean result = false;
            try {
                result = test.execute();
            } catch (Exception e) {
                caught = e;
                e.printStackTrace();
            }
            if (!result) {
                failed = true;
                System.out.printf("path test %d failed: %s : %s\n", i, test.arg1,
                        test.arg2);
            } else {
                System.out.println ("path test " + i + " OK");
            }

        }


        for (int i=0; i<pathImplies2.length ; i++) {
            URLImpliesTest test = (URLImpliesTest)pathImplies2[i];
            Exception caught = null;
            boolean result = false;
            try {
                result = test.execute();
            } catch (Exception e) {
                caught = e;
                e.printStackTrace();
            }
            if (!result) {
                failed = true;
                System.out.printf("path2 test %d failed: %s : %s\n", i, test.arg1,
                        test.arg2);
            } else {
                System.out.println ("path2 test " + i + " OK");
            }

        }

        for (int i=0; i<equalityTests.length ; i++) {
            URLEqualityTest test = (URLEqualityTest)equalityTests[i];
            Exception caught = null;
            boolean result = false;
            try {
                result = test.execute();
            } catch (Exception e) {
                caught = e;
                e.printStackTrace();
            }
            if (!result) {
                failed = true;
                System.out.printf("equality test %d failed: %s : %s\n", i, test.arg1,
                        test.arg2);
            } else {
                System.out.println ("equality test " + i + " OK");
            }

        }

        for (int i=0; i<hashTests.length; i++) {
            HashCodeTest test = (HashCodeTest)hashTests[i];
            boolean result = test.execute();
            if (!result) {
                System.out.printf ("test failed: %s %s %d\n", test.arg1, test.arg2, test.hash);
                failed = true;
            } else {
                System.out.println ("hash test " + i + " OK");
            }
        }

        for (int i=0; i<exceptionTests.length; i++) {
            ExTest test = (ExTest)exceptionTests[i];
            boolean result = test.execute();
            if (!result) {
                System.out.println ("test failed: " + test.arg);
                failed = true;
            } else {
                System.out.println ("exception test " + i + " OK");
            }
        }

        for (int i=0; i<createTests.length; i++) {
            CreateTest test = (CreateTest)createTests[i];
            boolean result = test.execute();
            if (!result) {
                System.out.println ("test failed: " + test.arg);
                failed = true;
            } else {
                System.out.println ("create test " + i + " OK");
            }
        }

        for (int i=0; i<actionImplies.length ; i++) {
            ActionImpliesTest test = (ActionImpliesTest)actionImplies[i];
            Exception caught = null;
            boolean result = false;
            try {
                result = test.execute();
            } catch (Exception e) {
                caught = e;
                e.printStackTrace();
            }
            if (!result) {
                failed = true;
                System.out.println ("test failed: " + test.arg1 + ": " +
                        test.arg2 + " Exception: " + caught);
            }
            System.out.println ("action test " + i + " OK");
        }

        for (int i = 0; i < actionsStringTest.length; i++) {
            ActionsStringTest test = (ActionsStringTest) actionsStringTest[i];
            Exception caught = null;
            boolean result = false;
            try {
                result = test.execute();
            } catch (Exception e) {
                caught = e;
            }
            if (!result) {
                failed = true;
                System.out.println("test failed: " + test.arg + ": "
                        + test.expectedActions + " Exception: " + caught);
            }
            System.out.println("Actions String test " + i + " OK");
        }

        serializationTest("http:
        serializationTest("https:
        serializationTest("https:*", "*:*");
        serializationTest("http:
        serializationTest("http:

        if (failed) {
            throw new RuntimeException("some tests failed");
        }

    }

    static void serializationTest(String name, String actions)
        throws Exception {

        URLPermission out = new URLPermission(name, actions);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(baos);
        o.writeObject(out);
        ByteArrayInputStream bain = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream i = new ObjectInputStream(bain);
        URLPermission in = (URLPermission)i.readObject();
        if (!in.equals(out)) {
            System.out.println ("FAIL");
            System.out.println ("in = " + in);
            System.out.println ("out = " + out);
            failed = true;
        }
    }
}
