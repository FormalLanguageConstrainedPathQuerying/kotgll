/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdb.hidden_class.hc001;

import java.io.File;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.nio.file.Files;
import java.nio.file.Paths;

import nsk.share.jdb.*;

/* Interface for tested hidden class to implement. */
interface HCInterf {
    void hcMethod();
}

/* Hidden class definition used to define tested hidden class
 * with lookup.defineHiddenClass. */
class HiddenClass implements HCInterf {
    static String hcField = null;

    private String getClassName() {
        return this.getClass().getName();
    }

    public void hcMethod() {
        hcField = getClassName();
        if (hcField.indexOf("HiddenClass") == -1) {
            throw new RuntimeException("Debuggee: Unexpected HiddenClass name: " + hcField);
        }
    }
}

/* This is debuggee aplication */
public class hc001a {
    static PrintStream log = null;
    static void logMsg(String msg) { log.println(msg); log.flush(); }

    static final String JAVA_CP = System.getProperty("java.class.path");
    static final String HC_NAME = HiddenClass.class.getName().replace(".", File.separator) + ".class";
    static final String HC_PATH = getClassPath(HiddenClass.class);
    static String hcName = null; 

    static String getClassPath(Class<?> klass) {
        String classPath = klass.getTypeName().replace(".", File.separator) + ".class";
        for (String path: JAVA_CP.split(File.pathSeparator)) {
            String fullClassPath = path + File.separator + classPath;
            if (new File(fullClassPath).exists()) {
                return fullClassPath;
            }
        }
        throw new RuntimeException("class path for " + klass.getName() + " not found");
    }

    public static void main(String args[]) throws Exception {
        log = new PrintStream("Debuggee.log");

        hc001a testApp = new hc001a();
        int status = testApp.runIt(args);
        System.exit(hc001.JCK_STATUS_BASE + status);
    }

    void emptyMethod() {}

    public int runIt(String args[]) throws Exception {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);

        logMsg("Debuggee: runIt: started");
        logMsg("Debuggee: JAVA_CP: " + JAVA_CP);
        logMsg("Debuggee: HC_NAME: " + HC_NAME);
        logMsg("Debuggee: HC_PATH: " + HC_PATH);

        Class<?> hc = defineHiddenClass(HC_PATH);

        hcName = hc.getName();
        logMsg("Debuggee: Defined HiddenClass: " + hcName);

        HCInterf hcObj = (HCInterf)hc.newInstance();
        logMsg("Debuggee: created an instance of a hidden class: " + hcName);

        logMsg("Debuggee: invoking emptyMethod to hit expected breakpoint");
        emptyMethod();

        logMsg("Debuggee: invoking a method of a hidden class: " + hcName);
        hcObj.hcMethod();

        logMsg("Debuggee: runIt finished");
        return hc001.PASSED;
    }

    static Class<?> defineHiddenClass(String classFileName) throws Exception {
        try {
            Lookup lookup = MethodHandles.lookup();
            byte[] bytes = Files.readAllBytes(Paths.get(classFileName));
            Class<?> hc = lookup.defineHiddenClass(bytes, false).lookupClass();
            return hc;
        } catch (Exception ex) {
            logMsg("Debuggee: defineHiddenClass: caught Exception " + ex.getMessage());
            ex.printStackTrace(log);
            log.flush();
            throw ex;
        }
    }
}
