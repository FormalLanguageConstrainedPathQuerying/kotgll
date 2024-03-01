/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jdi;

import java.util.*;
import com.sun.jdi.*;
import nsk.share.TestBug;

/*
 * Debugger class used in tests for heapwalking(tests for VirtualMachine.instanceCounts, ReferenceType.instances, ObjectReference.referrers)
 * Contains several common checking and auxiliary methods.
 */
public class HeapwalkingDebugger extends TestDebuggerType2 {
    protected boolean strictCheck(String className) {
        if (className.equals("java.lang.String"))
            return false;

        if (className.equals("char[]"))
            return false;

        if (className.equals("byte[]"))
            return false;

        if (className.equals("boolean[]"))
            return false;

        if (className.equals("float[]"))
            return false;

        if (className.equals("long[]"))
            return false;

        if (className.equals("int[]"))
            return false;

        if (className.equals("double[]"))
            return false;

        if (className.equals("java.lang.Thread")) {
            return !isJFRActive();
        }

        return true;
    }

    protected boolean isJFRActive() {
       ReferenceType referenceType = debuggee.classByName("nsk.share.jdi.HeapwalkingDebuggee");
       if (referenceType == null)
           throw new RuntimeException("Debugeee is not initialized yet");

        Field isJFRActiveFld = referenceType.fieldByName("isJFRActive");
        boolean isJFRActive = ((BooleanValue)referenceType.getValue(isJFRActiveFld)).value();
        return isJFRActive;
    }

    public long getInstanceCount(String className) {
        List<ReferenceType> list = vm.classesByName(className);

        if (list.size() == 0)
            return 0;

        if (list.size() > 1) {
            setSuccess(false);
            log.complain("Unexpected collection size returned by VirtualMachine.classesByName(" + className + "): " + list.size()
                    + ", only 1 entry was expected.");
        }

        long result[] = (vm.instanceCounts(list));

        return result[0];
    }

    protected void testInstanceCounts() {
        try {
            vm.instanceCounts(vm.allClasses());
        } catch (Throwable t) {
            setSuccess(false);
            log.complain("Unexpected exception: " + t);
            t.printStackTrace(log.getOutStream());
        }

    }

    protected void checkDebugeeAnswer_instances(String className, int expectedCount) {
        ReferenceType referenceType = debuggee.classByName(className);

        int instanceCounts = referenceType.instances(0).size();

        if (strictCheck(className)) {
            if (referenceType.instances(0).size() != expectedCount) {
                setSuccess(false);
                log.complain("Unexpected size of referenceType.instances(" + className + "): " + instanceCounts + ", expected: " + expectedCount);
            }
        } else {
            if (referenceType.instances(0).size() < expectedCount) {
                setSuccess(false);
                log.complain("Unexpected size of referenceType.instances(" + className + "): " + instanceCounts + ", expected: >= " + expectedCount);
            }
        }
    }

    public void checkDebugeeAnswer_instanceCounts(String className, int expectedValue) {
        if (!isDebuggeeReady())
            return;

        try {
            long instanceCounts = getInstanceCount(className);

            if (strictCheck(className)) {
                if (instanceCounts != expectedValue) {
                    setSuccess(false);
                    log.complain("Wrong value was returned  by VirtualMachine.instanceCounts(" + className + "): " + instanceCounts + ", expected: "
                            + expectedValue);
                }
            } else {
                if (instanceCounts < expectedValue) {
                    setSuccess(false);
                    log.complain("Wrong value was returned  by VirtualMachine.instanceCounts(" + className + "): " + instanceCounts
                            + ", expected: >= " + expectedValue);
                }
            }
        } catch (Throwable e) {
            setSuccess(false);

            log.complain("Unexpected exception when getting instance count info:");
            e.printStackTrace(log.getOutStream());
        }
    }

    public void checkDebugeeAnswer_instanceCounts(String className, int countCreated, List<ObjectReference> oldReferences) {
        if (strictCheck(className)) {
            int countAll = countCreated + oldReferences.size();
            checkDebugeeAnswer_instanceCounts(className, countAll);
            checkDebugeeAnswer_instances(className, countAll);
        } else {
            if (!isDebuggeeReady()) {
                return;
            }
        }

        int countFoundCreated = countNewInstances(className, oldReferences);
        if (countFoundCreated < countCreated) {
            setSuccess(false);
            log.complain("Too few new instances(" + className + "). Expected >= " + countCreated + ", found " + countFoundCreated);
        }
    }

    private int countNewInstances(String className, List<ObjectReference> oldReferences) {
        List<ObjectReference> newReferences = new ArrayList<ObjectReference>(getObjectReferences(className, vm));
        newReferences.removeAll(oldReferences);
        return newReferences.size();
    }

    protected void checkDebugeeAnswer_InterfaceType_instances(InterfaceType interfaceType, int expectedInstances) {
        int instanceCounts = interfaceType.instances(0).size();

        if (instanceCounts != expectedInstances) {
            setSuccess(false);
            log.complain("List with wrong size was returned by InterfaceType.instances(" + interfaceType.name() + "): " + instanceCounts
                         + ", expected: " + expectedInstances);
        }
    }

    static public List<ObjectReference> filterObjectReferrence(List<ObjectReference> objectsToFilter, List<ObjectReference> sourceList) {
        List<ObjectReference> result = new ArrayList<ObjectReference>();

        for (ObjectReference object : sourceList) {
            if (!objectsToFilter.contains(object))
                result.add(object);
        }

        return result;
    }

    static public List<ObjectReference> getObjectReferences(String className, VirtualMachine vm) {
        List<ReferenceType> referenceTypes = vm.classesByName(className);

        List<ObjectReference> objectReferences;

        if (referenceTypes.size() == 0)
            objectReferences = new ArrayList<ObjectReference>();
        else if (referenceTypes.size() == 1)
            objectReferences = referenceTypes.get(0).instances(0);
        else {
            throw new TestBug("Unexpected collection size returned by VirtualMachine.classesByName: " + referenceTypes.size()
                    + ", only 1 entry was expected.");
        }

        return objectReferences;
    }

}
