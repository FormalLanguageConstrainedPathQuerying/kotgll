/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.krb5;

import org.ietf.jgss.*;
import sun.security.jgss.GSSCaller;
import sun.security.jgss.spi.*;

import java.io.IOException;

import sun.security.krb5.Credentials;
import sun.security.krb5.KrbException;

import javax.security.auth.kerberos.KerberosTicket;

/**
 * Implements the krb5 proxy credential element used in constrained
 * delegation. It is used in both impersonation (where there is no Kerberos 5
 * communication between the middle server and the client) and normal
 * constrained delegation (where there is, but client has not called
 * requestCredDeleg(true)).
 * @since 1.8
 */

final class Krb5ProxyCredential
    implements Krb5CredElement {

    public final Krb5InitCredential self;   
    private final Krb5NameElement user;     

    public final Credentials userCreds;

    Krb5ProxyCredential(Krb5InitCredential self, Krb5NameElement user,
            Credentials userCreds) {
        this.self = self;
        this.userCreds = userCreds;
        this.user = user;
    }

    @Override
    public final Krb5NameElement getName() throws GSSException {
        return user;
    }

    @Override
    public int getInitLifetime() throws GSSException {
        return self.getInitLifetime();
    }

    @Override
    public int getAcceptLifetime() throws GSSException {
        return 0;
    }

    @Override
    public boolean isInitiatorCredential() throws GSSException {
        return true;
    }

    @Override
    public boolean isAcceptorCredential() throws GSSException {
        return false;
    }

    @Override
    public final Oid getMechanism() {
        return Krb5MechFactory.GSS_KRB5_MECH_OID;
    }

    @Override
    public final java.security.Provider getProvider() {
        return Krb5MechFactory.PROVIDER;
    }

    @Override
    public void dispose() throws GSSException {
        try {
            self.destroy();
        } catch (javax.security.auth.DestroyFailedException e) {
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1,
                 "Could not destroy credentials - " + e.getMessage());
            gssException.initCause(e);
        }
    }

    @Override
    public GSSCredentialSpi impersonate(GSSNameSpi name) throws GSSException {
        throw new GSSException(GSSException.FAILURE, -1,
                "Only an initiate credentials can impersonate");
    }

    static Krb5CredElement tryImpersonation(GSSCaller caller,
            Krb5InitCredential initiator) throws GSSException {

        try {
            KerberosTicket proxy = initiator.proxyTicket;
            if (proxy != null) {
                Credentials proxyCreds = Krb5Util.ticketToCreds(proxy);
                return new Krb5ProxyCredential(initiator,
                        Krb5NameElement.getInstance(proxyCreds.getClient()),
                        proxyCreds);
            } else {
                return initiator;
            }
        } catch (KrbException | IOException e) {
            throw new GSSException(GSSException.DEFECTIVE_CREDENTIAL, -1,
                    "Cannot create proxy credential");
        }
    }
}
