/*
 * Copyright (c) 2001, 2008, Oracle and/or its affiliates. All rights reserved.
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


public final class KeepAliveDuringCall_Stub
    extends java.rmi.server.RemoteStub
    implements ShutdownMonitor
{
    private static final long serialVersionUID = 2;

    private static java.lang.reflect.Method $method_declareStillAlive_0;
    private static java.lang.reflect.Method $method_submitShutdown_1;

    static {
        try {
            $method_declareStillAlive_0 = ShutdownMonitor.class.getMethod("declareStillAlive", new java.lang.Class[] {});
            $method_submitShutdown_1 = ShutdownMonitor.class.getMethod("submitShutdown", new java.lang.Class[] {Shutdown.class});
        } catch (java.lang.NoSuchMethodException e) {
            throw new java.lang.NoSuchMethodError(
                "stub class initialization failed");
        }
    }

    public KeepAliveDuringCall_Stub(java.rmi.server.RemoteRef ref) {
        super(ref);
    }


    public void declareStillAlive()
        throws java.rmi.RemoteException
    {
        try {
            ref.invoke(this, $method_declareStillAlive_0, null, -1562228924246272634L);
        } catch (java.lang.RuntimeException e) {
            throw e;
        } catch (java.rmi.RemoteException e) {
            throw e;
        } catch (java.lang.Exception e) {
            throw new java.rmi.UnexpectedException("undeclared checked exception", e);
        }
    }

    public void submitShutdown(Shutdown $param_Shutdown_1)
        throws java.rmi.RemoteException
    {
        try {
            ref.invoke(this, $method_submitShutdown_1, new java.lang.Object[] {$param_Shutdown_1}, 7574258166120515108L);
        } catch (java.lang.RuntimeException e) {
            throw e;
        } catch (java.rmi.RemoteException e) {
            throw e;
        } catch (java.lang.Exception e) {
            throw new java.rmi.UnexpectedException("undeclared checked exception", e);
        }
    }
}
