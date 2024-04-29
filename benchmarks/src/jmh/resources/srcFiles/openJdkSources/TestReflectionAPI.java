/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8046171
 * @summary Test the new nestmate reflection API
 * @compile TestReflectionAPI.java
 *          PackagedNestHost.java
 *          PackagedNestHost2.java
 *          SampleNest.java
 *          Hosts.java
 *          InvalidNestHost.java
 *
 * @compile MemberNoHost.jcod
 *          MemberMissingHost.jcod
 *          MemberNotInstanceHost.jcod
 *          MemberNotOurHost.jcod
 *          MemberMalformedHost.jcod
 *          MalformedHost.jcod
 *          PackagedNestHost.jcod
 *          PackagedNestHost2Member.jcod
 *          PackagedNestHostMember.jcod
 *          HostOfMemberNoHost.jcod
 *          HostOfMemberMissingHost.jcod
 *          HostOfMemberNotInstanceHost.jcod
 *          HostOfMemberNotOurHost.jcod
 *          HostOfMemberMalformedHost.jcod
 *          HostWithSelfMember.jcod
 *          HostWithDuplicateMembers.jcod
 *
 * @run main/othervm TestReflectionAPI
 * @run main/othervm/java.security.policy=empty.policy TestReflectionAPI
 */


import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

public class TestReflectionAPI {

    static class Member {}

    static class MemberNoHost {}

    static class MemberMissingHost {}

    static class MemberNotInstanceHost {
        Object[] oa; 
    }

    static class MemberNotOurHost {}

    static class MemberMalformedHost {}

    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < 2; i++) {
            test_getNestHost();
            test_isNestmateOf();
            test_getNestMembers();
        }
    }

    static void test_getNestHost() {
        Class<?> host = TestReflectionAPI.class;


        checkHost(host, host);
        checkHost(Member.class, host);
        Runnable r = new Runnable() { public void run() {}};
        checkHost(r.getClass(), host);

        Class<?>[] allClasses = host.getDeclaredClasses();
        for (Class<?> c : allClasses) {
            if (c == Member.class)
                continue;
            checkHost(c, c);
        }
        checkHost(P1.PackagedNestHost.Member.class,
                  P1.PackagedNestHost.Member.class);
        checkHost(P2.PackagedNestHost2.Member.class,
                  P2.PackagedNestHost2.Member.class);

        checkHost(int.class, int.class);                   
        checkHost(Object[].class, Object[].class);         
        checkHost(Thread.State.class, Thread.class);       
        checkHost(java.lang.annotation.Documented.class,   
                  java.lang.annotation.Documented.class);
    }

    static void test_isNestmateOf() {
        Class<?> host = TestReflectionAPI.class;
        checkNestmates(host, host, true);
        checkNestmates(Member.class, host, true);
        Runnable r = new Runnable() { public void run() {}};
        checkNestmates(r.getClass(), host, true);

        Class<?>[] allClasses = host.getDeclaredClasses();
        for (Class<?> c : allClasses) {
            if (c == Member.class)
                continue;
            checkNestmates(host, c, false);
        }

        checkNestmates(int.class, int.class, true);             
        checkNestmates(int.class, long.class, false);           
        checkNestmates(Object[].class, Object[].class, true);   
        checkNestmates(Object[].class, int[].class, false);     
        checkNestmates(Thread.State.class, Thread.class, true); 
        checkNestmates(java.lang.annotation.Documented.class,   
                       java.lang.annotation.Documented.class, true);
    }

    static void test_getNestMembers() {
        Class<?>[] good = { Object.class, Object[].class, int.class};
        checkSingletonNests(good);

        checkNest(SampleNest.class, SampleNest.nestedTypes(), false);

        checkNest(HostWithSelfMember.class,
                  new Class<?>[] { HostWithSelfMember.class,
                          HostWithSelfMember.Member.class },
                  true);
        checkNest(HostWithDuplicateMembers.class,
                  new Class<?>[] { HostWithDuplicateMembers.class,
                          HostWithDuplicateMembers.Member1.class,
                          HostWithDuplicateMembers.Member2.class },
                  true);

        Class<?>[] bad = {
            HostOfMemberNoHost.class,
            HostOfMemberMissingHost.class,
            HostOfMemberNotOurHost.class,
            HostOfMemberNotInstanceHost.class,
            HostOfMemberMalformedHost.class,
        };
        checkSingletonNests(bad);
    }

    static void checkHost(Class<?> target, Class<?> expected) {
        System.out.println("Checking nest host of " + target.getName());
        Class<?> host = target.getNestHost();
        if (host != expected)
            throw new Error("Class " + target.getName() +
                            " has nest host " + host.getName() +
                            " but expected " + expected.getName());
    }

    static void checkNestmates(Class<?> a, Class<?> b, boolean mates) {
        System.out.println("Checking if " + a.getName() +
                           " isNestmateOf " + b.getName());

        if (a.isNestmateOf(b) != mates)
            throw new Error("Class " + a.getName() + " is " +
                            (mates ? "not " : "") +
                            "a nestmate of " + b.getName() + " but should " +
                            (mates ? "" : "not ") + "be");
    }

    static Comparator<Class<?>> cmp = Comparator.comparing(Class::getName);

    static void checkNest(Class<?> host, Class<?>[] unsortedTypes, boolean expectDups) {
        Class<?>[] members = host.getNestMembers();
        Arrays.sort(members, cmp);
        Class<?>[] nestedTypes = unsortedTypes.clone();
        Arrays.sort(nestedTypes, cmp);
        printMembers(host, members);
        printDeclared(host, nestedTypes);
        if (!Arrays.equals(members, nestedTypes)) {
            if (!expectDups) {
                throw new Error("Class " + host.getName() + " has different members " +
                                "compared to declared classes");
            }
            else {
                Class<?>[] memberSet =
                    Arrays.stream(members).sorted(cmp).distinct().toArray(Class<?>[]::new);
                if (!Arrays.equals(memberSet, nestedTypes)) {
                    throw new Error("Class " + host.getName() + " has different members " +
                                "compared to declared classes, even after duplicate removal");
                }
            }
        }
        for (Class<?> a : members) {
            checkHost(a, host);
            checkNestmates(a, host, true);
            Class<?>[] aMembers = a.getNestMembers();
            if (aMembers[0] != host) {
                throw new Error("Class " + a.getName() + " getNestMembers()[0] = " +
                                aMembers[0].getName() + " not " + host.getName());

            }
            Arrays.sort(aMembers, cmp);
            if (!Arrays.equals(members, aMembers)) {
                throw new Error("Class " + a.getName() + " has different members " +
                                "compared to host " + host.getName());
            }
            for (Class<?> b : members) {
                checkNestmates(a, b, true);
            }
        }
    }

    static void checkSingletonNests(Class<?>[] classes) {
        for (Class<?> host : classes) {
            Class<?>[] members = host.getNestMembers();
            if (members.length != 1) {
                printMembers(host, members);
                throw new Error("Class " + host.getName() + " lists " + members.length
                                + " members instead of 1 (itself)");
            }
            if (members[0] != host) {
                printMembers(host, members);
                throw new Error("Class " + host.getName() + " lists " +
                                members[0].getName() + " as member instead of itself");
            }
        }
    }

    static void printMembers(Class<?> host, Class<?>[] members) {
        System.out.println("Class " + host.getName() + " has members: ");
        for (Class<?> c : members) {
            System.out.println(" - " + c.getName());
        }
    }

    static void printDeclared(Class<?> host, Class<?>[] declared) {
        System.out.println("Class " + host.getName() + " has declared types: ");
        for (Class<?> c : declared) {
            System.out.println(" - " + c.getName());
        }
    }

}
