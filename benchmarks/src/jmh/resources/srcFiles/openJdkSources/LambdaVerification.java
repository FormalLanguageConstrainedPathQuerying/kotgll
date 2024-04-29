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
 *
 */
import java.lang.StackWalker.StackFrame;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import jdk.test.whitebox.WhiteBox;

public class LambdaVerification {
    static void verifyCallerIsArchivedLambda(boolean isRuntime) {
        Class<?> c = getCallerClass(2);
        System.out.println("Lambda proxy class = " + c);
        String cn = c.getName();
        System.out.println(" cn = " + cn);
        String hiddenClassName = cn.replace('/', '+');
        String lambdaClassName = cn.substring(0, cn.lastIndexOf('/'));
        System.out.println(" lambda name = " + lambdaClassName);
        WhiteBox wb = WhiteBox.getWhiteBox();
        if (isRuntime) {
            if (wb.isSharedClass(c)) {
                System.out.println("As expected, " + c + " is in shared space.");
            } else {
                throw new java.lang.RuntimeException(c + " must be in shared space.");
            }
            try {
                Class.forName(hiddenClassName);
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace(System.out);
                System.out.println("As expected, loading of " + hiddenClassName + " should result in ClassNotFoundException.");
            } catch (Exception ex) {
                throw ex;
            }
            if (wb.isClassAlive(hiddenClassName)) {
                System.out.println("As expected, " + cn + " is alive.");
            } else {
                throw new java.lang.RuntimeException(cn + " should be alive.");
            }
        } else {
            if (wb.isSharedClass(c)) {
                throw new java.lang.RuntimeException(c + " must not be in shared space.");
            } else {
                System.out.println("As expected, " + c + " is not in shared space.");
            }
        }
        System.out.println("Succeeded");
    }

    static Class<?> getCallerClass(int depth) {
        StackWalker walker = StackWalker.getInstance(
            Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE,
                   StackWalker.Option.SHOW_HIDDEN_FRAMES));
        List<StackFrame> stack = walker.walk(s -> s.limit(depth+2).collect(Collectors.toList()));
        return stack.get(depth+1).getDeclaringClass();
    }
}
