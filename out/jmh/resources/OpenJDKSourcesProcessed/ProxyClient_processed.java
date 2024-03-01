/*
 * Copyright (c) 2004, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.tools.jconsole.JConsoleContext;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.lang.management.*;
import static java.lang.management.ManagementFactory.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.rmi.*;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.swing.event.SwingPropertyChangeSupport;
import sun.rmi.server.UnicastRef2;
import sun.rmi.transport.LiveRef;

public class ProxyClient implements JConsoleContext {

    private ConnectionState connectionState = ConnectionState.DISCONNECTED;

    private SwingPropertyChangeSupport propertyChangeSupport =
                                new SwingPropertyChangeSupport(this, true);

    private static Map<String, ProxyClient> cache =
        Collections.synchronizedMap(new HashMap<String, ProxyClient>());

    private volatile boolean isDead = true;
    private String hostName = null;
    private int port = 0;
    private String userName = null;
    private String password = null;
    private boolean hasPlatformMXBeans = false;
    private boolean hasHotSpotDiagnosticMXBean= false;
    private boolean hasCompilationMXBean = false;
    private boolean supportsLockUsage = false;


    private LocalVirtualMachine lvm;

    private String advancedUrl = null;

    private JMXServiceURL jmxUrl = null;
    private MBeanServerConnection mbsc = null;
    private SnapshotMBeanServerConnection server = null;
    private JMXConnector jmxc = null;
    private RMIServer stub = null;
    private static final SslRMIClientSocketFactory sslRMIClientSocketFactory =
            new SslRMIClientSocketFactory();
    private String registryHostName = null;
    private int registryPort = 0;
    private boolean vmConnector = false;
    private boolean sslRegistry = false;
    private boolean sslStub = false;
    private final String connectionName;
    private final String displayName;

    private ClassLoadingMXBean    classLoadingMBean = null;
    private CompilationMXBean     compilationMBean = null;
    private MemoryMXBean          memoryMBean = null;
    private OperatingSystemMXBean operatingSystemMBean = null;
    private RuntimeMXBean         runtimeMBean = null;
    private ThreadMXBean          threadMBean = null;

    private com.sun.management.OperatingSystemMXBean sunOperatingSystemMXBean = null;
    private HotSpotDiagnosticMXBean                  hotspotDiagnosticMXBean = null;

    private List<MemoryPoolProxy>           memoryPoolProxies = null;
    private List<GarbageCollectorMXBean>    garbageCollectorMBeans = null;

    private static final String HOTSPOT_DIAGNOSTIC_MXBEAN_NAME =
        "com.sun.management:type=HotSpotDiagnostic";

    private ProxyClient(String hostName, int port,
                        String userName, String password) throws IOException {
        this.connectionName = getConnectionName(hostName, port, userName);
        this.displayName = connectionName;
        if (hostName.equals("localhost") && port == 0) {
            this.hostName = hostName;
            this.port = port;
        } else {
            final String urlPath = "/jndi/rmi:
                                   "/jmxrmi";
            JMXServiceURL url = new JMXServiceURL("rmi", "", 0, urlPath);
            setParameters(url, userName, password);
            vmConnector = true;
            registryHostName = hostName;
            registryPort = port;
            checkSslConfig();
        }
    }

    private ProxyClient(String url,
                        String userName, String password) throws IOException {
        this.advancedUrl = url;
        this.connectionName = getConnectionName(url, userName);
        this.displayName = connectionName;
        setParameters(new JMXServiceURL(url), userName, password);
    }

    private ProxyClient(LocalVirtualMachine lvm) throws IOException {
        this.lvm = lvm;
        this.connectionName = getConnectionName(lvm);
        this.displayName = "pid: " + lvm.vmid() + " " + lvm.displayName();
    }

    private void setParameters(JMXServiceURL url, String userName, String password) {
        this.jmxUrl = url;
        this.hostName = jmxUrl.getHost();
        this.port = jmxUrl.getPort();
        this.userName = userName;
        this.password = password;
    }

    private static void checkStub(Remote stub,
                                  Class<? extends Remote> stubClass) {
        if (stub.getClass() != stubClass) {
            if (!Proxy.isProxyClass(stub.getClass())) {
                throw new SecurityException(
                    "Expecting a " + stubClass.getName() + " stub!");
            } else {
                InvocationHandler handler = Proxy.getInvocationHandler(stub);
                if (handler.getClass() != RemoteObjectInvocationHandler.class) {
                    throw new SecurityException(
                        "Expecting a dynamic proxy instance with a " +
                        RemoteObjectInvocationHandler.class.getName() +
                        " invocation handler!");
                } else {
                    stub = (Remote) handler;
                }
            }
        }
        RemoteRef ref = ((RemoteObject)stub).getRef();
        if (ref.getClass() != UnicastRef2.class) {
            throw new SecurityException(
                "Expecting a " + UnicastRef2.class.getName() +
                " remote reference in stub!");
        }
        LiveRef liveRef = ((UnicastRef2)ref).getLiveRef();
        RMIClientSocketFactory csf = liveRef.getClientSocketFactory();
        if (csf == null || csf.getClass() != SslRMIClientSocketFactory.class) {
            throw new SecurityException(
                "Expecting a " + SslRMIClientSocketFactory.class.getName() +
                " RMI client socket factory in stub!");
        }
    }

    private static final String rmiServerImplStubClassName =
        "javax.management.remote.rmi.RMIServerImpl_Stub";
    private static final Class<? extends Remote> rmiServerImplStubClass;

    static {
        Class<? extends Remote> serverStubClass = null;
        try {
            serverStubClass = Class.forName(rmiServerImplStubClassName).asSubclass(Remote.class);
        } catch (ClassNotFoundException e) {
            throw new InternalError(e.getMessage(), e);
        }
        rmiServerImplStubClass = serverStubClass;
    }

    private void checkSslConfig() throws IOException {
        Registry registry;
        try {
            registry =
                LocateRegistry.getRegistry(registryHostName, registryPort,
                                           sslRMIClientSocketFactory);
            try {
                stub = (RMIServer) registry.lookup("jmxrmi");
            } catch (NotBoundException nbe) {
                throw (IOException)
                    new IOException(nbe.getMessage()).initCause(nbe);
            }
            sslRegistry = true;
        } catch (IOException e) {
            registry =
                LocateRegistry.getRegistry(registryHostName, registryPort);
            try {
                stub = (RMIServer) registry.lookup("jmxrmi");
            } catch (NotBoundException nbe) {
                throw (IOException)
                    new IOException(nbe.getMessage()).initCause(nbe);
            }
            sslRegistry = false;
        }
        try {
            checkStub(stub, rmiServerImplStubClass);
            sslStub = true;
        } catch (SecurityException e) {
            sslStub = false;
        }
    }

    /**
     * Returns true if the underlying RMI registry is SSL-protected.
     *
     * @exception UnsupportedOperationException If this {@code ProxyClient}
     * does not denote a JMX connector for a JMX VM agent.
     */
    public boolean isSslRmiRegistry() {
        if (!isVmConnector()) {
            throw new UnsupportedOperationException(
                "ProxyClient.isSslRmiRegistry() is only supported if this " +
                "ProxyClient is a JMX connector for a JMX VM agent");
        }
        return sslRegistry;
    }

    /**
     * Returns true if the retrieved RMI stub is SSL-protected.
     *
     * @exception UnsupportedOperationException If this {@code ProxyClient}
     * does not denote a JMX connector for a JMX VM agent.
     */
    public boolean isSslRmiStub() {
        if (!isVmConnector()) {
            throw new UnsupportedOperationException(
                "ProxyClient.isSslRmiStub() is only supported if this " +
                "ProxyClient is a JMX connector for a JMX VM agent");
        }
        return sslStub;
    }

    /**
     * Returns true if this {@code ProxyClient} denotes
     * a JMX connector for a JMX VM agent.
     */
    public boolean isVmConnector() {
        return vmConnector;
    }

    private void setConnectionState(ConnectionState state) {
        ConnectionState oldState = this.connectionState;
        this.connectionState = state;
        propertyChangeSupport.firePropertyChange(CONNECTION_STATE_PROPERTY,
                                                 oldState, state);
    }

    public ConnectionState getConnectionState() {
        return this.connectionState;
    }

    void flush() {
        if (server != null) {
            server.flush();
        }
    }

    void connect(boolean requireSSL) {
        setConnectionState(ConnectionState.CONNECTING);
        try {
            tryConnect(requireSSL);
            setConnectionState(ConnectionState.CONNECTED);
        } catch (Exception e) {
            if (JConsole.isDebug()) {
                e.printStackTrace();
            }
            setConnectionState(ConnectionState.DISCONNECTED);
        }
    }

    private void tryConnect(boolean requireRemoteSSL) throws IOException {
        if (jmxUrl == null && "localhost".equals(hostName) && port == 0) {
            this.jmxc = null;
            this.mbsc = ManagementFactory.getPlatformMBeanServer();
            this.server = Snapshot.newSnapshot(mbsc);
        } else {
            if (lvm != null) {
                if (!lvm.isManageable()) {
                    lvm.startManagementAgent();
                    if (!lvm.isManageable()) {
                        throw new IOException(lvm + "not manageable");
                    }
                }
                if (this.jmxUrl == null) {
                    this.jmxUrl = new JMXServiceURL(lvm.connectorAddress());
                }
            }
            Map<String, Object> env = new HashMap<String, Object>();
            if (requireRemoteSSL) {
                env.put("jmx.remote.x.check.stub", "true");
            }
            if (userName == null && password == null) {
                if (isVmConnector()) {
                    if (stub == null) {
                        checkSslConfig();
                    }
                    this.jmxc = new RMIConnector(stub, null);
                    jmxc.connect(env);
                } else {
                    this.jmxc = JMXConnectorFactory.connect(jmxUrl, env);
                }
            } else {
                env.put(JMXConnector.CREDENTIALS,
                        new String[] {userName, password});
                if (isVmConnector()) {
                    if (stub == null) {
                        checkSslConfig();
                    }
                    this.jmxc = new RMIConnector(stub, null);
                    jmxc.connect(env);
                } else {
                    this.jmxc = JMXConnectorFactory.connect(jmxUrl, env);
                }
            }
            this.mbsc = jmxc.getMBeanServerConnection();
            this.server = Snapshot.newSnapshot(mbsc);
        }
        this.isDead = false;

        try {
            ObjectName on = new ObjectName(THREAD_MXBEAN_NAME);
            this.hasPlatformMXBeans = server.isRegistered(on);
            this.hasHotSpotDiagnosticMXBean =
                server.isRegistered(new ObjectName(HOTSPOT_DIAGNOSTIC_MXBEAN_NAME));
            if (this.hasPlatformMXBeans) {
                MBeanOperationInfo[] mopis = server.getMBeanInfo(on).getOperations();
                for (MBeanOperationInfo op : mopis) {
                    if (op.getName().equals("findDeadlockedThreads")) {
                        this.supportsLockUsage = true;
                        break;
                    }
                }

                on = new ObjectName(COMPILATION_MXBEAN_NAME);
                this.hasCompilationMXBean = server.isRegistered(on);
            }
        } catch (MalformedObjectNameException e) {
            throw new InternalError(e.getMessage());
        } catch (IntrospectionException |
                 InstanceNotFoundException |
                 ReflectionException e) {
            throw new InternalError(e.getMessage(), e);
        }

        if (hasPlatformMXBeans) {
            getRuntimeMXBean();
        }
    }

    /**
     * Gets a proxy client for a given local virtual machine.
     */
    public static ProxyClient getProxyClient(LocalVirtualMachine lvm)
        throws IOException {
        final String key = getCacheKey(lvm);
        ProxyClient proxyClient = cache.get(key);
        if (proxyClient == null) {
            proxyClient = new ProxyClient(lvm);
            cache.put(key, proxyClient);
        }
        return proxyClient;
    }

    public static String getConnectionName(LocalVirtualMachine lvm) {
        return Integer.toString(lvm.vmid());
    }

    private static String getCacheKey(LocalVirtualMachine lvm) {
        return Integer.toString(lvm.vmid());
    }

    /**
     * Gets a proxy client for a given JMXServiceURL.
     */
    public static ProxyClient getProxyClient(String url,
                                             String userName, String password)
        throws IOException {
        final String key = getCacheKey(url, userName, password);
        ProxyClient proxyClient = cache.get(key);
        if (proxyClient == null) {
            proxyClient = new ProxyClient(url, userName, password);
            cache.put(key, proxyClient);
        }
        return proxyClient;
    }

    public static String getConnectionName(String url,
                                           String userName) {
        if (userName != null && userName.length() > 0) {
            return userName + "@" + url;
        } else {
            return url;
        }
    }

    private static String getCacheKey(String url,
                                      String userName, String password) {
        return (url == null ? "" : url) + ":" +
               (userName == null ? "" : userName) + ":" +
               (password == null ? "" : password);
    }

    /**
     * Gets a proxy client for a given "hostname:port".
     */
    public static ProxyClient getProxyClient(String hostName, int port,
                                             String userName, String password)
        throws IOException {
        final String key = getCacheKey(hostName, port, userName, password);
        ProxyClient proxyClient = cache.get(key);
        if (proxyClient == null) {
            proxyClient = new ProxyClient(hostName, port, userName, password);
            cache.put(key, proxyClient);
        }
        return proxyClient;
    }

    public static String getConnectionName(String hostName, int port,
                                           String userName) {
        String name = hostName + ":" + port;
        if (userName != null && userName.length() > 0) {
            return userName + "@" + name;
        } else {
            return name;
        }
    }

    private static String getCacheKey(String hostName, int port,
                                      String userName, String password) {
        return (hostName == null ? "" : hostName) + ":" +
               port + ":" +
               (userName == null ? "" : userName) + ":" +
               (password == null ? "" : password);
    }

    public String connectionName() {
        return connectionName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String toString() {
        if (!isConnected()) {
            return Resources.format(Messages.CONNECTION_NAME__DISCONNECTED_, displayName);
        } else {
            return displayName;
        }
    }

   public MBeanServerConnection getMBeanServerConnection() {
       return mbsc;
   }

    public SnapshotMBeanServerConnection getSnapshotMBeanServerConnection() {
        return server;
    }

    public String getUrl() {
        return advancedUrl;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public int getVmid() {
        return (lvm != null) ? lvm.vmid() : 0;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public void disconnect() {
        stub = null;
        if (jmxc != null) {
            try {
                jmxc.close();
            } catch (IOException e) {
            }
        }
        classLoadingMBean = null;
        compilationMBean = null;
        memoryMBean = null;
        operatingSystemMBean = null;
        runtimeMBean = null;
        threadMBean = null;
        sunOperatingSystemMXBean = null;
        garbageCollectorMBeans = null;
        if (!isDead) {
            isDead = true;
            setConnectionState(ConnectionState.DISCONNECTED);
        }
    }

    /**
     * Returns the list of domains in which any MBean is
     * currently registered.
     */
    public String[] getDomains() throws IOException {
        return server.getDomains();
    }

    /**
     * Returns a map of MBeans with ObjectName as the key and MBeanInfo value
     * of a given domain.  If domain is {@code null}, all MBeans
     * are returned.  If no MBean found, an empty map is returned.
     *
     */
    public Map<ObjectName, MBeanInfo> getMBeans(String domain)
        throws IOException {

        ObjectName name = null;
        if (domain != null) {
            try {
                name = new ObjectName(domain + ":*");
            } catch (MalformedObjectNameException e) {
                assert(false);
            }
        }
        Set<ObjectName> mbeans = server.queryNames(name, null);
        Map<ObjectName,MBeanInfo> result =
            new HashMap<ObjectName,MBeanInfo>(mbeans.size());
        Iterator<ObjectName> iterator = mbeans.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (object instanceof ObjectName) {
                ObjectName o = (ObjectName)object;
                try {
                    MBeanInfo info = server.getMBeanInfo(o);
                    result.put(o, info);
                } catch (IntrospectionException | ReflectionException | InstanceNotFoundException e) {
                }
            }
        }
        return result;
    }

    /**
     * Returns a list of attributes of a named MBean.
     *
     */
    public AttributeList getAttributes(ObjectName name, String[] attributes)
        throws IOException {
        AttributeList list = null;
        try {
            list = server.getAttributes(name, attributes);
        } catch (InstanceNotFoundException e) {
        } catch (ReflectionException e) {
        }
        return list;
    }

    /**
     * Set the value of a specific attribute of a named MBean.
     */
    public void setAttribute(ObjectName name, Attribute attribute)
        throws InvalidAttributeValueException,
               MBeanException,
               IOException {
        try {
            server.setAttribute(name, attribute);
        } catch (InstanceNotFoundException e) {
        } catch (AttributeNotFoundException e) {
            assert(false);
        } catch (ReflectionException e) {
        }
    }

    /**
     * Invokes an operation of a named MBean.
     *
     * @throws MBeanException Wraps an exception thrown by
     *      the MBean's invoked method.
     */
    public Object invoke(ObjectName name, String operationName,
                         Object[] params, String[] signature)
        throws IOException, MBeanException {
        Object result = null;
        try {
            result = server.invoke(name, operationName, params, signature);
        } catch (InstanceNotFoundException e) {
        } catch (ReflectionException e) {
        }
        return result;
    }

    public synchronized ClassLoadingMXBean getClassLoadingMXBean() throws IOException {
        if (hasPlatformMXBeans && classLoadingMBean == null) {
            classLoadingMBean =
                newPlatformMXBeanProxy(server, CLASS_LOADING_MXBEAN_NAME,
                                       ClassLoadingMXBean.class);
        }
        return classLoadingMBean;
    }

    public synchronized CompilationMXBean getCompilationMXBean() throws IOException {
        if (hasCompilationMXBean && compilationMBean == null) {
            compilationMBean =
                newPlatformMXBeanProxy(server, COMPILATION_MXBEAN_NAME,
                                       CompilationMXBean.class);
        }
        return compilationMBean;
    }

    public Collection<MemoryPoolProxy> getMemoryPoolProxies()
        throws IOException {

        if (memoryPoolProxies == null) {
            ObjectName poolName = null;
            try {
                poolName = new ObjectName(MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",*");
            } catch (MalformedObjectNameException e) {
                assert(false);
            }
            Set<ObjectName> mbeans = server.queryNames(poolName, null);
            if (mbeans != null) {
                memoryPoolProxies = new ArrayList<MemoryPoolProxy>();
                Iterator<ObjectName> iterator = mbeans.iterator();
                while (iterator.hasNext()) {
                    ObjectName objName = iterator.next();
                    MemoryPoolProxy p = new MemoryPoolProxy(this, objName);
                    memoryPoolProxies.add(p);
                }
            }
        }
        return memoryPoolProxies;
    }

    public synchronized Collection<GarbageCollectorMXBean> getGarbageCollectorMXBeans()
        throws IOException {

        if (garbageCollectorMBeans == null) {
            ObjectName gcName = null;
            try {
                gcName = new ObjectName(GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*");
            } catch (MalformedObjectNameException e) {
                assert(false);
            }
            Set<ObjectName> mbeans = server.queryNames(gcName, null);
            if (mbeans != null) {
                garbageCollectorMBeans = new ArrayList<GarbageCollectorMXBean>();
                Iterator<ObjectName> iterator = mbeans.iterator();
                while (iterator.hasNext()) {
                    ObjectName on = iterator.next();
                    String name = GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE +
                        ",name=" + on.getKeyProperty("name");

                    GarbageCollectorMXBean mBean =
                        newPlatformMXBeanProxy(server, name,
                                               GarbageCollectorMXBean.class);
                        garbageCollectorMBeans.add(mBean);
                }
            }
        }
        return garbageCollectorMBeans;
    }

    public synchronized MemoryMXBean getMemoryMXBean() throws IOException {
        if (hasPlatformMXBeans && memoryMBean == null) {
            memoryMBean =
                newPlatformMXBeanProxy(server, MEMORY_MXBEAN_NAME,
                                       MemoryMXBean.class);
        }
        return memoryMBean;
    }

    public synchronized RuntimeMXBean getRuntimeMXBean() throws IOException {
        if (hasPlatformMXBeans && runtimeMBean == null) {
            runtimeMBean =
                newPlatformMXBeanProxy(server, RUNTIME_MXBEAN_NAME,
                                       RuntimeMXBean.class);
        }
        return runtimeMBean;
    }


    public synchronized ThreadMXBean getThreadMXBean() throws IOException {
        if (hasPlatformMXBeans && threadMBean == null) {
            threadMBean =
                newPlatformMXBeanProxy(server, THREAD_MXBEAN_NAME,
                                       ThreadMXBean.class);
        }
        return threadMBean;
    }

    public synchronized OperatingSystemMXBean getOperatingSystemMXBean() throws IOException {
        if (hasPlatformMXBeans && operatingSystemMBean == null) {
            operatingSystemMBean =
                newPlatformMXBeanProxy(server, OPERATING_SYSTEM_MXBEAN_NAME,
                                       OperatingSystemMXBean.class);
        }
        return operatingSystemMBean;
    }

    public synchronized com.sun.management.OperatingSystemMXBean
        getSunOperatingSystemMXBean() throws IOException {

        try {
            ObjectName on = new ObjectName(OPERATING_SYSTEM_MXBEAN_NAME);
            if (sunOperatingSystemMXBean == null) {
                if (server.isInstanceOf(on,
                        "com.sun.management.OperatingSystemMXBean")) {
                    sunOperatingSystemMXBean =
                        newPlatformMXBeanProxy(server,
                            OPERATING_SYSTEM_MXBEAN_NAME,
                            com.sun.management.OperatingSystemMXBean.class);
                }
            }
        } catch (InstanceNotFoundException e) {
             return null;
        } catch (MalformedObjectNameException e) {
             return null; 
        }
        return sunOperatingSystemMXBean;
    }

    public synchronized HotSpotDiagnosticMXBean getHotSpotDiagnosticMXBean() throws IOException {
        if (hasHotSpotDiagnosticMXBean && hotspotDiagnosticMXBean == null) {
            hotspotDiagnosticMXBean =
                newPlatformMXBeanProxy(server, HOTSPOT_DIAGNOSTIC_MXBEAN_NAME,
                                       HotSpotDiagnosticMXBean.class);
        }
        return hotspotDiagnosticMXBean;
    }

    public <T> T getMXBean(ObjectName objName, Class<T> interfaceClass)
        throws IOException {
        return newPlatformMXBeanProxy(server,
                                      objName.toString(),
                                      interfaceClass);

    }

    public long[] findDeadlockedThreads() throws IOException {
        ThreadMXBean tm = getThreadMXBean();
        if (supportsLockUsage && tm.isSynchronizerUsageSupported()) {
            return tm.findDeadlockedThreads();
        } else {
            return tm.findMonitorDeadlockedThreads();
        }
    }

    public synchronized void markAsDead() {
        disconnect();
    }

    public boolean isDead() {
        return isDead;
    }

    boolean isConnected() {
        return !isDead();
    }

    boolean hasPlatformMXBeans() {
        return this.hasPlatformMXBeans;
    }

    boolean hasHotSpotDiagnosticMXBean() {
        return this.hasHotSpotDiagnosticMXBean;
    }

    boolean isLockUsageSupported() {
        return supportsLockUsage;
    }

    public boolean isRegistered(ObjectName name) throws IOException {
        return server.isRegistered(name);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addWeakPropertyChangeListener(PropertyChangeListener listener) {
        if (!(listener instanceof WeakPCL)) {
            listener = new WeakPCL(listener);
        }
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (!(listener instanceof WeakPCL)) {
            for (PropertyChangeListener pcl : propertyChangeSupport.getPropertyChangeListeners()) {
                if (pcl instanceof WeakPCL && ((WeakPCL)pcl).get() == listener) {
                    listener = pcl;
                    break;
                }
            }
        }
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * The PropertyChangeListener is handled via a WeakReference
     * so as not to pin down the listener.
     */
    private class WeakPCL extends WeakReference<PropertyChangeListener>
                          implements PropertyChangeListener {
        WeakPCL(PropertyChangeListener referent) {
            super(referent);
        }

        public void propertyChange(PropertyChangeEvent pce) {
            PropertyChangeListener pcl = get();

            if (pcl == null) {
                dispose();
            } else {
                pcl.propertyChange(pce);
            }
        }

        private void dispose() {
            removePropertyChangeListener(this);
        }
    }


    public interface SnapshotMBeanServerConnection
            extends MBeanServerConnection {
        /**
         * Flush all cached values of attributes.
         */
        public void flush();
    }

    public static class Snapshot {
        private Snapshot() {
        }
        public static SnapshotMBeanServerConnection
                newSnapshot(MBeanServerConnection mbsc) {
            final InvocationHandler ih = new SnapshotInvocationHandler(mbsc);
            return (SnapshotMBeanServerConnection) Proxy.newProxyInstance(
                    Snapshot.class.getClassLoader(),
                    new Class<?>[] {SnapshotMBeanServerConnection.class},
                    ih);
        }
    }

    static class SnapshotInvocationHandler implements InvocationHandler {

        private final MBeanServerConnection conn;
        private Map<ObjectName, NameValueMap> cachedValues = newMap();
        private Map<ObjectName, Set<String>> cachedNames = newMap();

        @SuppressWarnings("serial")
        private static final class NameValueMap
                extends HashMap<String, Object> {}

        SnapshotInvocationHandler(MBeanServerConnection conn) {
            this.conn = conn;
        }

        synchronized void flush() {
            cachedValues = newMap();
        }

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            final String methodName = method.getName();
            if (methodName.equals("getAttribute")) {
                return getAttribute((ObjectName) args[0], (String) args[1]);
            } else if (methodName.equals("getAttributes")) {
                return getAttributes((ObjectName) args[0], (String[]) args[1]);
            } else if (methodName.equals("flush")) {
                flush();
                return null;
            } else {
                try {
                    return method.invoke(conn, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        private Object getAttribute(ObjectName objName, String attrName)
                throws MBeanException, InstanceNotFoundException,
                AttributeNotFoundException, ReflectionException, IOException {
            final NameValueMap values = getCachedAttributes(
                    objName, Collections.singleton(attrName));
            Object value = values.get(attrName);
            if (value != null || values.containsKey(attrName)) {
                return value;
            }
            return conn.getAttribute(objName, attrName);
        }

        private AttributeList getAttributes(
                ObjectName objName, String[] attrNames) throws
                InstanceNotFoundException, ReflectionException, IOException {
            final NameValueMap values = getCachedAttributes(
                    objName,
                    new TreeSet<String>(Arrays.asList(attrNames)));
            final AttributeList list = new AttributeList();
            for (String attrName : attrNames) {
                final Object value = values.get(attrName);
                if (value != null || values.containsKey(attrName)) {
                    list.add(new Attribute(attrName, value));
                }
            }
            return list;
        }

        private synchronized NameValueMap getCachedAttributes(
                ObjectName objName, Set<String> attrNames) throws
                InstanceNotFoundException, ReflectionException, IOException {
            NameValueMap values = cachedValues.get(objName);
            if (values != null && values.keySet().containsAll(attrNames)) {
                return values;
            }
            attrNames = new TreeSet<String>(attrNames);
            Set<String> oldNames = cachedNames.get(objName);
            if (oldNames != null) {
                attrNames.addAll(oldNames);
            }
            values = new NameValueMap();
            final AttributeList attrs = conn.getAttributes(
                    objName,
                    attrNames.toArray(new String[attrNames.size()]));
            for (Attribute attr : attrs.asList()) {
                values.put(attr.getName(), attr.getValue());
            }
            cachedValues.put(objName, values);
            cachedNames.put(objName, attrNames);
            return values;
        }

        private static <K, V> Map<K, V> newMap() {
            return new HashMap<K, V>();
        }
    }
}
