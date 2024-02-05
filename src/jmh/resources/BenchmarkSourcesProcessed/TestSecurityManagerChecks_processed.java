/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that security checks occur for getNestHost/getNestMembers
 *
 * @library /test/lib
 * @build TestSecurityManagerChecks testPkg.Host testPkg.Singleton
 * @run driver jdk.test.lib.helpers.ClassFileInstaller testPkg.Host testPkg.Host$Member testPkg.Singleton
 * @run main/othervm -Xbootclasspath/a:. -Djava.security.manager=allow TestSecurityManagerChecks
 */

import java.security.Security;

public class TestSecurityManagerChecks {

    public static void main(String[] args) throws Throwable {

        Class<?> host = testPkg.Host.class;
        Class<?> member = testPkg.Host.Member.class;
        Class<?> memberArray = testPkg.Host.Member[].class;
        Class<?> singleton = testPkg.Singleton.class;

        Security.setProperty("package.access",
                             Security.getProperty("package.access") + ",testPkg.");

        SecurityManager sm = new SecurityManager();
        System.setSecurityManager(sm);

        getNestHost(int.class);   
        getNestHost(int[].class); 
        getNestHost(host);        
        getNestHost(memberArray); 
        getNestHost(singleton);   

        getNestMembers(int.class);   
        getNestMembers(int[].class); 
        getNestMembers(memberArray); 
        getNestMembers(singleton);   

        getNestHostThrows(member); 

        getNestMembersThrows(member); 
        getNestMembersThrows(host);   
    }

    static void getNestHost(Class<?> c) {
        Class<?> host = c.getNestHost();
        System.out.println("OK - getNestHost succeeded for " + c.getName());
    }

    static void getNestHostThrows(Class<?> c) throws SecurityException {
        try {
            Class<?> host = c.getNestHost();
            throw new Error("getNestHost succeeded for " + c.getName());
        } catch (SecurityException e) {
            System.out.println("OK - getNestHost for " + c.getName() +
                               " got expected exception: " + e);
        }
    }

    static void getNestMembers(Class<?> c) {
        Class<?>[] members = c.getNestMembers();
        System.out.println("OK - getNestMembers succeeded for " + c.getName());
    }

    static void getNestMembersThrows(Class<?> c) throws SecurityException {
        try {
            Class<?>[] members = c.getNestMembers();
            throw new Error("getNestMembers succeeded for " + c.getName());
        } catch (SecurityException e) {
            System.out.println("OK - getNestMembers for " + c.getName() +
                               " got expected exception: " + e);
        }
    }

}
