/*
 * Copyright (c) 2008, 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6730926
 * @summary Check behaviour of MBeanServer when postRegister and postDeregister
 *          throw exceptions.
 * @author Daniel Fuchs
 *
 * @run main PostExceptionTest
 */

import javax.management.*;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.EnumSet;

public class PostExceptionTest {

    /**
     * A test case where we instantiate an ExceptionalWombatMBean (or a
     * subclass of it) which will throw the exception {@code t} from within
     * the methods indicated by {@code where}
     */
    public static class Case {
        public final Throwable t;
        public final EnumSet<WHERE> where;
        public Case(Throwable t,EnumSet<WHERE> where) {
            this.t=t; this.where=where;
        }
    }


    public static Case caze(Throwable t, WHERE w) {
        return new Case(t,EnumSet.of(w));
    }
    public static Case caze(Throwable t, EnumSet<WHERE> where) {
        return new Case(t,where);
    }
    public static Case caze(Throwable t, WHERE w, WHERE... rest) {
        return new Case(t,EnumSet.of(w,rest));
    }

    /**
     * Here is the list of our test cases:
     */
    public static Case[] cases ={
        caze(new RuntimeException(),WHERE.PREREGISTER),
        caze(new RuntimeException(),WHERE.POSTREGISTER),
        caze(new RuntimeException(),WHERE.POSTREGISTER, WHERE.PREDEREGISTER),
        caze(new RuntimeException(),WHERE.POSTREGISTER, WHERE.POSTDEREGISTER),
        caze(new Exception(),WHERE.PREREGISTER),
        caze(new Exception(),WHERE.POSTREGISTER),
        caze(new Exception(),WHERE.POSTREGISTER, WHERE.PREDEREGISTER),
        caze(new Exception(),WHERE.POSTREGISTER, WHERE.POSTDEREGISTER),
        caze(new Error(),WHERE.PREREGISTER),
        caze(new Error(),WHERE.POSTREGISTER),
        caze(new Error(),WHERE.POSTREGISTER, WHERE.PREDEREGISTER),
        caze(new Error(),WHERE.POSTREGISTER, WHERE.POSTDEREGISTER),
        caze(new RuntimeException(),EnumSet.allOf(WHERE.class)),
        caze(new Exception(),EnumSet.allOf(WHERE.class)),
        caze(new Error(),EnumSet.allOf(WHERE.class)),
    };

    public static void main(String[] args) throws Exception {
        System.out.println("Test behaviour of MBeanServer when postRegister " +
                "or postDeregister throw exceptions");
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        int failures = 0;
        final ObjectName n = new ObjectName("test:type=Wombat");

        for (Case caze:cases) {
            for (CREATE how : CREATE.values()) {
                failures+=test(mbs,n,how,caze.t,caze.where);
            }
        }
        if (failures == 0)
            System.out.println("Test passed");
        else {
            System.out.println("TEST FAILED: " + failures + " failure(s)");
            System.exit(1);
        }
    }

    private static int test(MBeanServer mbs, ObjectName name, CREATE how,
            Throwable t, EnumSet<WHERE> where)
            throws Exception {
        System.out.println("-------<"+how+"> / <"+t+"> / "+ where + "-------");

        int failures = 0;
        ObjectInstance oi = null;
        Exception reg = null;    
        Exception unreg = null;  
        try {
            oi = how.create(t, where, mbs, name);
        } catch (Exception xx) {
            reg=xx;
        }
        final ObjectName n = (oi==null)?name:oi.getObjectName();
        final boolean isRegistered = mbs.isRegistered(n);
        try {
            if (isRegistered) mbs.unregisterMBean(n);
        } catch (Exception xxx) {
            unreg=xxx;
        }
        final boolean isUnregistered = !mbs.isRegistered(n);
        if (!isUnregistered) {
            JMX.newMBeanProxy(mbs, n, ExceptionalWombatMBean.class).end();
            mbs.unregisterMBean(n);
        }

        if (where.isEmpty() && reg!=null) {
            System.out.println("Unexpected registration exception: "+
                    reg);
            throw new RuntimeException("Unexpected registration exception: "+
                    reg,reg);
        }

        if (where.isEmpty() && unreg!=null) {
            System.out.println("Unexpected unregistration exception: "+
                    unreg);
            throw new RuntimeException("Unexpected unregistration exception: "+
                    unreg,unreg);
        }

        if ((where.contains(WHERE.PREREGISTER)
            || where.contains(WHERE.POSTREGISTER))&& reg==null) {
            System.out.println("Expected registration exception not " +
                    "thrown by "+where);
            throw new RuntimeException("Expected registration exception not " +
                    "thrown by "+where);
        }

        if ((where.contains(WHERE.PREDEREGISTER)
            || where.contains(WHERE.POSTDEREGISTER))&& unreg==null
            && !where.contains(WHERE.PREREGISTER)) {
            System.out.println("Expected unregistration exception not " +
                    "thrown by "+where);
            throw new RuntimeException("Expected unregistration exception not " +
                    "thrown by "+where);
        }

        if (where.contains(WHERE.PREREGISTER)) {
            if (isRegistered) {
                System.out.println("MBean is still registered [" +
                        where+
                        "]: "+name+" / "+reg);
                throw new RuntimeException("MBean is still registered [" +
                        where+
                        "]: "+name+" / "+reg,reg);
            }
        }

        if (where.contains(WHERE.POSTREGISTER) &&
                !where.contains(WHERE.PREREGISTER)) {
            if (!isRegistered) {
                System.out.println("MBean is already unregistered [" +
                        where+
                        "]: "+name+" / "+reg);
                throw new RuntimeException("MBean is already unregistered [" +
                        where+
                        "]: "+name+" / "+reg,reg);
            }
        }

        if (where.contains(WHERE.PREREGISTER)) {
            WHERE.PREREGISTER.check(reg, t);
        } else if (where.contains(WHERE.POSTREGISTER)) {
            WHERE.POSTREGISTER.check(reg, t);
        }

        if (!isRegistered) return failures;


        if (where.contains(WHERE.PREDEREGISTER)) {
            if (isUnregistered) {
                System.out.println("MBean is already unregistered [" +
                        where+
                        "]: "+name+" / "+unreg);
                throw new RuntimeException("MBean is already unregistered [" +
                        where+
                        "]: "+name+" / "+unreg,unreg);
            }
        }

        if (where.contains(WHERE.POSTDEREGISTER) &&
                !where.contains(WHERE.PREDEREGISTER)) {
            if (!isUnregistered) {
                System.out.println("MBean is not unregistered [" +
                        where+
                        "]: "+name+" / "+unreg);
                throw new RuntimeException("MBean is not unregistered [" +
                        where+
                        "]: "+name+" / "+unreg,unreg);
            }
        }

        if (where.contains(WHERE.PREDEREGISTER)) {
            WHERE.PREDEREGISTER.check(unreg, t);
        } else if (where.contains(WHERE.POSTDEREGISTER)) {
            WHERE.POSTDEREGISTER.check(unreg, t);
        }
        return failures;
    }

    /**
     * This enum lists the 4 methods in MBeanRegistration.
     */
    public static enum WHERE {

        PREREGISTER, POSTREGISTER, PREDEREGISTER, POSTDEREGISTER;

        public void check(Exception thrown, Throwable t)
                throws Exception {
           if (t instanceof RuntimeException) {
               if (!(thrown instanceof RuntimeMBeanException)) {
                   System.out.println("Expected RuntimeMBeanException, got "+
                           thrown);
                   throw new Exception("Expected RuntimeMBeanException, got "+
                           thrown);
               }
           } else if (t instanceof Error) {
               if (!(thrown instanceof RuntimeErrorException)) {
                   System.out.println("Expected RuntimeErrorException, got "+
                           thrown);
                   throw new Exception("Expected RuntimeErrorException, got "+
                           thrown);
               }
           } else if (t instanceof Exception) {
               if (EnumSet.of(POSTDEREGISTER,POSTREGISTER).contains(this)) {
                   if (!(thrown instanceof RuntimeMBeanException)) {
                       System.out.println("Expected RuntimeMBeanException, got "+
                           thrown);
                       throw new Exception("Expected RuntimeMBeanException, got "+
                           thrown);
                   }
                   if (! (thrown.getCause() instanceof RuntimeException)) {
                       System.out.println("Bad cause: " +
                               "expected RuntimeException, " +
                           "got <"+thrown.getCause()+">");
                       throw new Exception("Bad cause: " +
                               "expected RuntimeException, " +
                           "got <"+thrown.getCause()+">");
                   }
               }
               if (EnumSet.of(PREDEREGISTER,PREREGISTER).contains(this)) {
                   if (!(thrown instanceof MBeanRegistrationException)) {
                       System.out.println("Expected " +
                               "MBeanRegistrationException, got "+
                           thrown);
                       throw new Exception("Expected " +
                               "MBeanRegistrationException, got "+
                           thrown);
                   }
                   if (! (thrown.getCause() instanceof Exception)) {
                       System.out.println("Bad cause: " +
                               "expected Exception, " +
                           "got <"+thrown.getCause()+">");
                       throw new Exception("Bad cause: " +
                               "expected Exception, " +
                           "got <"+thrown.getCause()+">");
                   }
               }
           }

        }
    }

    /**
     * This enum lists the 5 methods to create and register an
     * ExceptionalWombat MBean
     */
    public static enum CREATE {

        CREATE1() {
            public ObjectInstance create(Throwable t, EnumSet<WHERE> where,
                    MBeanServer server, ObjectName name) throws Exception {
                ExceptionallyHackyWombat.t = t;
                ExceptionallyHackyWombat.w = where;
                return server.createMBean(
                        ExceptionallyHackyWombat.class.getName(),
                        name);
            }
        },
        CREATE2() {
            public ObjectInstance create(Throwable t, EnumSet<WHERE> where,
                    MBeanServer server, ObjectName name) throws Exception {
                ExceptionallyHackyWombat.t = t;
                ExceptionallyHackyWombat.w = where;
                final ObjectName loaderName = registerMB(server);
                return server.createMBean(
                        ExceptionallyHackyWombat.class.getName(),
                        name, loaderName);
            }
        },
        CREATE3() {
            public ObjectInstance create(Throwable t, EnumSet<WHERE> where,
                    MBeanServer server, ObjectName name) throws Exception {
                final Object[] params = {t, where};
                final String[] signature = {Throwable.class.getName(),
                    EnumSet.class.getName()
                };
                return server.createMBean(
                        ExceptionalWombat.class.getName(), name,
                        params, signature);
            }
        },
        CREATE4() {
            public ObjectInstance create(Throwable t, EnumSet<WHERE> where,
                    MBeanServer server, ObjectName name) throws Exception {
                final Object[] params = {t, where};
                final String[] signature = {Throwable.class.getName(),
                    EnumSet.class.getName()
                };
                return server.createMBean(
                        ExceptionalWombat.class.getName(), name,
                        registerMB(server), params, signature);
            }
        },
        REGISTER() {
            public ObjectInstance create(Throwable t, EnumSet<WHERE> where,
                    MBeanServer server, ObjectName name) throws Exception {
                final ExceptionalWombat wombat =
                        new ExceptionalWombat(t, where);
                return server.registerMBean(wombat, name);
            }
        };

        public abstract ObjectInstance create(Throwable t, EnumSet<WHERE> where,
                MBeanServer server, ObjectName name) throws Exception;

        public ObjectName registerMB(MBeanServer server) throws Exception {
            final ObjectName name = new ObjectName("test:type=TestMBean");
            if (server.isRegistered(name)) {
                return name;
            }
            final TestMBean mbean = new Test();
            return server.registerMBean(mbean, name).getObjectName();
        }
    }

    /**
     * A Wombat MBean that can throw exceptions or errors in any of the
     * MBeanRegistration methods.
     */
    public static interface ExceptionalWombatMBean {
        public void end();
    }

    /**
     *A Wombat MBean that can throw exceptions or errors in any of the
     * MBeanRegistration methods.
     */
    public static class ExceptionalWombat
            implements ExceptionalWombatMBean, MBeanRegistration {

        private final Throwable throwable;
        private final EnumSet<WHERE> where;
        private volatile boolean end=false;

        public ExceptionalWombat(Throwable t, EnumSet<WHERE> where) {
            this.throwable=t; this.where=where;
        }
        private Exception doThrow() {
            if (throwable instanceof Error)
                throw (Error)throwable;
            if (throwable instanceof RuntimeException)
                throw (RuntimeException)throwable;
            return (Exception)throwable;
        }
        public ObjectName preRegister(MBeanServer server, ObjectName name)
                throws Exception {
            if (!end && where.contains(WHERE.PREREGISTER))
                throw doThrow();
            return name;
        }

        public void postRegister(Boolean registrationDone) {
            if (!end && where.contains(WHERE.POSTREGISTER))
                throw new RuntimeException(doThrow());
        }

        public void preDeregister() throws Exception {
            if (!end && where.contains(WHERE.PREDEREGISTER))
                throw doThrow();
        }

        public void postDeregister() {
            if (!end && where.contains(WHERE.POSTREGISTER))
                throw new RuntimeException(doThrow());
        }

        public void end() {
            this.end=true;
        }
    }

    /**
     * This is a big ugly hack to call createMBean form #1 and #2 - where
     * the empty constructor is used. Since we still want to supply parameters
     * to the ExceptionalWombat super class, we temporarily store these
     * parameter value in a static volatile before calling create MBean.
     * Of course this only works because our test is sequential and single
     * threaded, and nobody but our test uses this ExceptionallyHackyWombat.
     */
    public static class ExceptionallyHackyWombat extends ExceptionalWombat {
        public static volatile Throwable  t;
        public static volatile EnumSet<WHERE> w;
        public ExceptionallyHackyWombat() {
            super(t,w);
        }
    }

    public static interface TestMBean {
    }

    public static class Test extends URLClassLoader implements TestMBean {
        public Test() {
            super(new URL[0], ClassLoader.getSystemClassLoader());
        }
    }

}
