/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6894072 8194486
 * @summary always refresh keytab
 * @library /test/lib
 * @compile -XDignore.symbol.file DynamicKeytab.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts DynamicKeytab
 */

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.ietf.jgss.GSSException;
import sun.security.jgss.GSSUtil;
import sun.security.krb5.KrbException;
import sun.security.krb5.internal.Krb5;

public class DynamicKeytab {

    Context c, s;
    public static void main(String[] args)
            throws Exception {
        new DynamicKeytab().go();
    }

    void go() throws Exception {
        OneKDC k = new OneKDC(null);
        k.writeJAASConf();

        Files.delete(Paths.get(OneKDC.KTAB));

        c = Context.fromJAAS("client");
        s = Context.fromJAAS("com.sun.security.jgss.krb5.accept");

        k.addPrincipal(OneKDC.SERVER, "pass1".toCharArray());
        k.writeKtab(OneKDC.KTAB);
        connect();

        k.addPrincipal(OneKDC.SERVER, "pass2".toCharArray());
        k.appendKtab(OneKDC.KTAB);
        connect();

        c = Context.fromJAAS("client");
        connect();

        c = Context.fromJAAS("client");
        k.addPrincipal(OneKDC.SERVER, "pass3".toCharArray());
        k.appendKtab(OneKDC.KTAB);
        k.addPrincipal(OneKDC.SERVER, "pass4".toCharArray());
        k.appendKtab(OneKDC.KTAB);
        k.addPrincipal(OneKDC.SERVER, "pass3".toCharArray());
        connect();

        try (FileOutputStream fos = new FileOutputStream(OneKDC.KTAB)) {
            fos.write("BADBADBAD".getBytes());
        }
        connect();

        Files.delete(Paths.get(OneKDC.KTAB));
        try {
            connect();
            throw new Exception("Should not success");
        } catch (GSSException gsse) {
            System.out.println(gsse);
            KrbException ke = (KrbException)gsse.getCause();
            if (ke.returnCode() != Krb5.API_INVALID_ARG) {
                throw new Exception("Not expected failure code: " +
                        ke.returnCode());
            }
        }

        k.addPrincipal(OneKDC.SERVER, "pass5".toCharArray());
        k.writeKtab(OneKDC.KTAB);   
        try {
            connect();
            throw new Exception("Should not success");
        } catch (GSSException gsse) {
            System.out.println(gsse);
        }

        KDC.create("EMPTY.REALM").writeKtab(OneKDC.KTAB);
        try {
            connect();
            throw new Exception("Should not success");
        } catch (GSSException gsse) {
            System.out.println(gsse);
            KrbException ke = (KrbException)gsse.getCause();
            if (ke.returnCode() != Krb5.API_INVALID_ARG) {
                throw new Exception("Not expected failure code: " +
                        ke.returnCode());
            }
        }
    }

    void connect() throws Exception {
        Thread.sleep(2000);     
        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);
        Context.handshake(c, s);
    }
}
