/*
 * Copyright (c) 2005, 2023, Oracle and/or its affiliates. All rights reserved.
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
package sun.tools.attach;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.AttachNotSupportedException;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class AttachProviderImpl extends HotSpotAttachProvider {

    public AttachProviderImpl() {
    }

    public String name() {
        return "sun";
    }

    public String type() {
        return "windows";
    }

    public VirtualMachine attachVirtualMachine(String vmid)
        throws AttachNotSupportedException, IOException
    {
        checkAttachPermission();

        testAttachable(vmid);

        return new VirtualMachineImpl(this, vmid);
    }

    public List<VirtualMachineDescriptor> listVirtualMachines() {
        if (isTempPathSecure()) {
            return super.listVirtualMachines();
        } else {
            return listJavaProcesses();
        }
    }

    /**
     * Returns true if the temporary file system supports security
     */
    private static boolean isTempPathSecure() {
        if (!wasTempPathChecked) {
            synchronized (AttachProviderImpl.class) {
                if (!wasTempPathChecked) {
                    String temp = tempPath();
                    if ((temp != null) && (temp.length() >= 3) &&
                        (temp.charAt(1) == ':') && (temp.charAt(2) == '\\'))
                    {
                        long flags = volumeFlags(temp.substring(0, 3));
                        isTempPathSecure = ((flags & FS_PERSISTENT_ACLS) != 0);
                    }
                    wasTempPathChecked = true;
                }
            }
        }

        return isTempPathSecure;
    }

    private static final long FS_PERSISTENT_ACLS = 0x8L;

    private static volatile boolean wasTempPathChecked;

    private static boolean isTempPathSecure;

    private static native String tempPath();

    private static native long volumeFlags(String volume);


    /**
     * Returns a list of virtual machine descriptors derived from an enumeration
     * of the process list.
     */
    private List<VirtualMachineDescriptor> listJavaProcesses() {
        ArrayList<VirtualMachineDescriptor> list =
            new ArrayList<VirtualMachineDescriptor>();

        String host = "localhost";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
        }

        int processes[] = new int[1024];
        int count = enumProcesses(processes, processes.length);
        for (int i=0; i<count; i++) {
            if (isLibraryLoadedByProcess("jvm.dll", processes[i])) {
                String pid = Integer.toString(processes[i]);
                try {
                    new VirtualMachineImpl(this, pid).detach();

                    String name = pid + "@" + host;

                    list.add(new HotSpotVirtualMachineDescriptor(this, pid, name));
                } catch (AttachNotSupportedException x) {
                } catch (IOException ioe) {
                }
            }
        }

        return list;
    }

    private static native int enumProcesses(int[] processes, int max);

    private static native boolean isLibraryLoadedByProcess(String library,
                                                           int processId);


    static {
        System.loadLibrary("attach");
    }

}
