/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package com.sun.tools.example.debug.tty;

import com.sun.jdi.*;
import com.sun.jdi.request.*;

import java.util.ArrayList;
import java.util.List;

class BreakpointSpec extends EventRequestSpec {
    String methodId;
    List<String> methodArgs;
    int lineNumber;
    ThreadReference threadFilter; /* Thread to break in. null if global breakpoint. */
    public static final String locationTokenDelimiter = ":( \t\n\r";

    BreakpointSpec(ReferenceTypeSpec refSpec, int lineNumber, ThreadReference threadFilter) {
        super(refSpec);
        this.methodId = null;
        this.methodArgs = null;
        this.lineNumber = lineNumber;
        this.threadFilter = threadFilter;
    }

    BreakpointSpec(ReferenceTypeSpec refSpec, String methodId, ThreadReference threadFilter,
                   List<String> methodArgs) throws MalformedMemberNameException {
        super(refSpec);
        this.methodId = methodId;
        this.methodArgs = methodArgs;
        this.lineNumber = 0;
        this.threadFilter = threadFilter;
        if (!isValidMethodName(methodId)) {
            throw new MalformedMemberNameException(methodId);
        }
    }

    /**
     * The 'refType' is known to match, return the EventRequest.
     */
    @Override
    EventRequest resolveEventRequest(ReferenceType refType)
                           throws AmbiguousMethodException,
                                  AbsentInformationException,
                                  InvalidTypeException,
                                  NoSuchMethodException,
                                  LineNotFoundException {
        Location location = location(refType);
        if (location == null) {
            throw new InvalidTypeException();
        }
        EventRequestManager em = refType.virtualMachine().eventRequestManager();
        BreakpointRequest bp = em.createBreakpointRequest(location);
        bp.setSuspendPolicy(suspendPolicy);
        if (threadFilter != null) {
            bp.addThreadFilter(threadFilter);
        }
        bp.enable();
        return bp;
    }

    String methodName() {
        return methodId;
    }

    int lineNumber() {
        return lineNumber;
    }

    List<String> methodArgs() {
        return methodArgs;
    }

    boolean isMethodBreakpoint() {
        return (methodId != null);
    }

    @Override
    public int hashCode() {
        return refSpec.hashCode() + lineNumber +
            ((methodId != null) ? methodId.hashCode() : 0) +
            ((methodArgs != null) ? methodArgs.hashCode() : 0) +
            ((threadFilter != null) ? threadFilter.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BreakpointSpec) {
            BreakpointSpec breakpoint = (BreakpointSpec)obj;

            return ((methodId != null) ?
                        methodId.equals(breakpoint.methodId)
                      : methodId == breakpoint.methodId) &&
                   ((methodArgs != null) ?
                        methodArgs.equals(breakpoint.methodArgs)
                      : methodArgs == breakpoint.methodArgs) &&
                   ((threadFilter != null) ?
                        threadFilter.equals(breakpoint.threadFilter)
                      : threadFilter == breakpoint.threadFilter) &&
                   refSpec.equals(breakpoint.refSpec) &&
                   (lineNumber == breakpoint.lineNumber);
        } else {
            return false;
        }
    }

    @Override
    String errorMessageFor(Exception e) {
        if (e instanceof AmbiguousMethodException) {
            return (MessageOutput.format("Method is overloaded; specify arguments",
                                         methodName()));
            /*
             * TO DO: list the methods here
             */
        } else if (e instanceof NoSuchMethodException) {
            return (MessageOutput.format("No method in",
                                         new Object [] {methodName(),
                                                        refSpec.toString()}));
        } else if (e instanceof AbsentInformationException) {
            return (MessageOutput.format("No linenumber information for",
                                         refSpec.toString()));
        } else if (e instanceof LineNotFoundException) {
            return (MessageOutput.format("No code at line",
                                         new Object [] {Long.valueOf(lineNumber()),
                                                 refSpec.toString()}));
        } else if (e instanceof InvalidTypeException) {
            return (MessageOutput.format("Breakpoints can be located only in classes.",
                                         refSpec.toString()));
        } else {
            return super.errorMessageFor( e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(refSpec.toString());
        if (isMethodBreakpoint()) {
            sb.append('.');
            sb.append(methodId);
            if (methodArgs != null) {
                boolean first = true;
                sb.append('(');
                for (String arg : methodArgs) {
                    if (!first) {
                        sb.append(',');
                    }
                    sb.append(arg);
                    first = false;
                }
                sb.append(")");
            }
        } else {
            sb.append(':');
            sb.append(lineNumber);
        }
        return MessageOutput.format("breakpoint", sb.toString());
    }

    private Location location(ReferenceType refType) throws
                                    AmbiguousMethodException,
                                    AbsentInformationException,
                                    NoSuchMethodException,
                                    LineNotFoundException {
        Location location = null;
        if (isMethodBreakpoint()) {
            Method method = findMatchingMethod(refType);
            location = method.location();
        } else {
            List<Location> locs = refType.locationsOfLine(lineNumber());
            if (locs.size() == 0) {
                throw new LineNotFoundException();
            }
            location = locs.get(0);
            if (location.method() == null) {
                throw new LineNotFoundException();
            }
        }
        return location;
    }

    private boolean isValidMethodName(String s) {
        return isJavaIdentifier(s) ||
               s.equals("<init>") ||
               s.equals("<clinit>");
    }

    /*
     * Compare a method's argument types with a Vector of type names.
     * Return true if each argument type has a name identical to the
     * corresponding string in the vector (allowing for varars)
     * and if the number of arguments in the method matches the
     * number of names passed
     */
    private boolean compareArgTypes(Method method, List<String> nameList) {
        List<String> argTypeNames = method.argumentTypeNames();

        if (argTypeNames.size() != nameList.size()) {
            return false;
        }

        int nTypes = argTypeNames.size();
        for (int i = 0; i < nTypes; ++i) {
            String comp1 = argTypeNames.get(i);
            String comp2 = nameList.get(i);
            if (! comp1.equals(comp2)) {
                /*
                 * We have to handle varargs.  EG, the
                 * method's last arg type is xxx[]
                 * while the nameList contains xxx...
                 * Note that the nameList can also contain
                 * xxx[] in which case we don't get here.
                 */
                if (i != nTypes - 1 ||
                    !method.isVarArgs()  ||
                    !comp2.endsWith("...")) {
                    return false;
                }
                /*
                 * The last types differ, it is a varargs
                 * method and the nameList item is varargs.
                 * We just have to compare the type names, eg,
                 * make sure we don't have xxx[] for the method
                 * arg type and yyy... for the nameList item.
                 */
                int comp1Length = comp1.length();
                if (comp1Length + 1 != comp2.length()) {
                    return false;
                }
                if (!comp1.regionMatches(0, comp2, 0, comp1Length - 2)) {
                    return false;
                }
                return true;
            }
        }

        return true;
    }


    /*
     * Remove unneeded spaces and expand class names to fully
     * qualified names, if necessary and possible.
     */
    private String normalizeArgTypeName(String name) {
        /*
         * Separate the type name from any array modifiers,
         * stripping whitespace after the name ends
         */
        int i = 0;
        StringBuilder typePart = new StringBuilder();
        StringBuilder arrayPart = new StringBuilder();
        name = name.trim();
        int nameLength = name.length();
        /*
         * For varargs, there can be spaces before the ... but not
         * within the ...  So, we will just ignore the ...
         * while stripping blanks.
         */
        boolean isVarArgs = name.endsWith("...");
        if (isVarArgs) {
            nameLength -= 3;
        }
        while (i < nameLength) {
            char c = name.charAt(i);
            if (Character.isWhitespace(c) || c == '[') {
                break;      
            }
            typePart.append(c);
            i++;
        }
        while (i < nameLength) {
            char c = name.charAt(i);
            if ( (c == '[') || (c == ']')) {
                arrayPart.append(c);
            } else if (!Character.isWhitespace(c)) {
                throw new IllegalArgumentException
                    (MessageOutput.format("Invalid argument type name"));
            }
            i++;
        }
        name = typePart.toString();

        /*
         * When there's no sign of a package name already, try to expand the
         * the name to a fully qualified class name
         */
        if ((name.indexOf('.') == -1) || name.startsWith("*.")) {
            try {
                ReferenceType argClass = Env.getReferenceTypeFromToken(name);
                if (argClass != null) {
                    name = argClass.name();
                }
            } catch (IllegalArgumentException e) {
            }
        }
        name += arrayPart.toString();
        if (isVarArgs) {
            name += "...";
        }
        return name;
    }

    /*
     * Attempt an unambiguous match of the method name and
     * argument specification to a method. If no arguments
     * are specified, the method must not be overloaded.
     * Otherwise, the argument types much match exactly
     */
    private Method findMatchingMethod(ReferenceType refType)
                                        throws AmbiguousMethodException,
                                               NoSuchMethodException {

        List<String> argTypeNames = null;
        if (methodArgs() != null) {
            argTypeNames = new ArrayList<String>(methodArgs().size());
            for (String name : methodArgs()) {
                name = normalizeArgTypeName(name);
                argTypeNames.add(name);
            }
        }

        Method firstMatch = null;  
        Method exactMatch = null;  
        int matchCount = 0;        
        for (Method candidate : refType.methods()) {
            if (candidate.name().equals(methodName())) {
                matchCount++;

                if (matchCount == 1) {
                    firstMatch = candidate;
                }

                if ((argTypeNames != null)
                        && compareArgTypes(candidate, argTypeNames) == true) {
                    exactMatch = candidate;
                    break;
                }
            }
        }

        Method method = null;
        if (exactMatch != null) {
            method = exactMatch;
        } else if ((argTypeNames == null) && (matchCount > 0)) {
            if (matchCount == 1) {
                method = firstMatch;       
            } else {
                throw new AmbiguousMethodException();
            }
        } else {
            throw new NoSuchMethodException(methodName());
        }
        return method;
    }
}
