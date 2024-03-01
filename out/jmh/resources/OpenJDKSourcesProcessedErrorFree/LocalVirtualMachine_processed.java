/*
 * Copyright (c) 2005, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole;

import java.util.*;
import java.io.IOException;
import java.io.File;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.AttachNotSupportedException;

import jdk.internal.agent.ConnectorAddressLink;
import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.VmIdentifier;

public class LocalVirtualMachine {
    private String address;
    private String commandLine;
    private String displayName;
    private int vmid;
    private boolean isAttachSupported;

    public LocalVirtualMachine(int vmid, String commandLine, boolean canAttach, String connectorAddress) {
        this.vmid = vmid;
        this.commandLine = commandLine;
        this.address = connectorAddress;
        this.isAttachSupported = canAttach;
        this.displayName = getDisplayName(commandLine);
    }

    private static String getDisplayName(String commandLine) {
        String[] res = commandLine.split(" ", 2);
        if (res[0].endsWith(".jar")) {
           File jarfile = new File(res[0]);
           String displayName = jarfile.getName();
           if (res.length == 2) {
               displayName += " " + res[1];
           }
           return displayName;
        }
        return commandLine;
    }

    public int vmid() {
        return vmid;
    }

    public boolean isManageable() {
        return (address != null);
    }

    public boolean isAttachable() {
        return isAttachSupported;
    }

    public void startManagementAgent() throws IOException {
        if (address != null) {
            return;
        }

        if (!isAttachable()) {
            throw new IOException("This virtual machine \"" + vmid +
                "\" does not support dynamic attach.");
        }

        loadManagementAgent();
        if (address == null) {
            throw new IOException("Fails to find connector address");
        }
    }

    public String connectorAddress() {
        return address;
    }

    public String displayName() {
        return displayName;
    }

    public String toString() {
        return commandLine;
    }

    public static Map<Integer, LocalVirtualMachine> getAllVirtualMachines() {
        Map<Integer, LocalVirtualMachine> map =
            new HashMap<Integer, LocalVirtualMachine>();
        getMonitoredVMs(map);
        getAttachableVMs(map);
        return map;
    }

    private static void getMonitoredVMs(Map<Integer, LocalVirtualMachine> map) {
        MonitoredHost host;
        Set<Integer> vms;
        try {
            host = MonitoredHost.getMonitoredHost(new HostIdentifier((String)null));
            vms = host.activeVms();
        } catch (java.net.URISyntaxException | MonitorException x) {
            throw new InternalError(x.getMessage(), x);
        }
        for (Object vmid: vms) {
            if (vmid instanceof Integer) {
                int pid = ((Integer) vmid).intValue();
                String name = vmid.toString(); 
                boolean attachable = false;
                String address = null;
                try {
                     MonitoredVm mvm = host.getMonitoredVm(new VmIdentifier(name));
                     name =  MonitoredVmUtil.commandLine(mvm);
                     attachable = MonitoredVmUtil.isAttachable(mvm);
                     address = ConnectorAddressLink.importFrom(pid);
                     mvm.detach();
                } catch (Exception x) {
                }
                map.put((Integer) vmid,
                        new LocalVirtualMachine(pid, name, attachable, address));
            }
        }
    }

    private static final String LOCAL_CONNECTOR_ADDRESS_PROP =
        "com.sun.management.jmxremote.localConnectorAddress";

    private static void getAttachableVMs(Map<Integer, LocalVirtualMachine> map) {
        List<VirtualMachineDescriptor> vms = VirtualMachine.list();
        for (VirtualMachineDescriptor vmd : vms) {
            try {
                Integer vmid = Integer.valueOf(vmd.id());
                if (!map.containsKey(vmid)) {
                    boolean attachable = false;
                    String address = null;
                    try {
                        VirtualMachine vm = VirtualMachine.attach(vmd);
                        attachable = true;
                        Properties agentProps = vm.getAgentProperties();
                        address = (String) agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);
                        vm.detach();
                    } catch (AttachNotSupportedException x) {
                    } catch (IOException x) {
                    }
                    map.put(vmid, new LocalVirtualMachine(vmid.intValue(),
                                                          vmd.displayName(),
                                                          attachable,
                                                          address));
                }
            } catch (NumberFormatException e) {
            }
        }
    }

    public static LocalVirtualMachine getLocalVirtualMachine(int vmid) {
        Map<Integer, LocalVirtualMachine> map = getAllVirtualMachines();
        LocalVirtualMachine lvm = map.get(vmid);
        if (lvm == null) {
            boolean attachable = false;
            String address = null;
            String name = String.valueOf(vmid); 
            try {
                VirtualMachine vm = VirtualMachine.attach(name);
                attachable = true;
                Properties agentProps = vm.getAgentProperties();
                address = (String) agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);
                vm.detach();
                lvm = new LocalVirtualMachine(vmid, name, attachable, address);
            } catch (AttachNotSupportedException x) {
                if (JConsole.isDebug()) {
                    x.printStackTrace();
                }
            } catch (IOException x) {
                if (JConsole.isDebug()) {
                    x.printStackTrace();
                }
            }
        }
        return lvm;
    }

    private void loadManagementAgent() throws IOException {
        VirtualMachine vm = null;
        String name = String.valueOf(vmid);
        try {
            vm = VirtualMachine.attach(name);
        } catch (AttachNotSupportedException x) {
            IOException ioe = new IOException(x.getMessage());
            ioe.initCause(x);
            throw ioe;
        }

        vm.startLocalManagementAgent();

        Properties agentProps = vm.getAgentProperties();
        address = (String) agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);

        vm.detach();
    }
}
