/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5;

import sun.security.krb5.internal.*;
import sun.security.krb5.internal.crypto.Nonce;
import sun.security.krb5.internal.crypto.KeyUsage;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;

/**
 * This class encapsulates the KRB-AS-REQ message that the client
 * sends to the KDC.
 */
public class KrbAsReq extends KrbKdcReq {
    private ASReq asReqMessg;

    private boolean DEBUG = Krb5.DEBUG;

    /**
     * Constructs an AS-REQ message.
     */
    public KrbAsReq(EncryptionKey pakey,        
                      KDCOptions options,       
                      PrincipalName cname,      
                      PrincipalName sname,      
                      KerberosTime from,        
                      KerberosTime till,        
                      KerberosTime rtime,       
                      int[] eTypes,             
                      HostAddresses addresses,  
                      PAData[] extraPAs         
                      )
            throws KrbException, IOException {

        if (options == null) {
            options = new KDCOptions();
        }
        if (options.get(KDCOptions.FORWARDED) ||
            options.get(KDCOptions.PROXY) ||
            options.get(KDCOptions.ENC_TKT_IN_SKEY) ||
            options.get(KDCOptions.RENEW) ||
            options.get(KDCOptions.VALIDATE)) {
            throw new KrbException(Krb5.KRB_AP_ERR_REQ_OPTIONS);
        }
        if (options.get(KDCOptions.POSTDATED)) {
        } else {
            if (from != null)  from = null;
        }

        PAData[] paData = null;
        if (pakey != null) {
            PAEncTSEnc ts = new PAEncTSEnc();
            byte[] temp = ts.asn1Encode();
            EncryptedData encTs = new EncryptedData(pakey, temp,
                KeyUsage.KU_PA_ENC_TS);
            paData = new PAData[1];
            paData[0] = new PAData( Krb5.PA_ENC_TIMESTAMP,
                                    encTs.asn1Encode());
        }
        if (extraPAs != null && extraPAs.length > 0) {
            if (paData == null) {
                paData = new PAData[extraPAs.length];
            } else {
                paData = Arrays.copyOf(paData, paData.length + extraPAs.length);
            }
            System.arraycopy(extraPAs, 0, paData,
                    paData.length - extraPAs.length, extraPAs.length);
        }

        if (cname.getRealm() == null) {
            throw new RealmException(Krb5.REALM_NULL,
                                     "default realm not specified ");
        }

        if (DEBUG) {
            System.out.println(">>> KrbAsReq creating message");
        }

        Config cfg = Config.getInstance();

        if (addresses == null && cfg.useAddresses()) {
            addresses = HostAddresses.getLocalAddresses();
        }

        if (sname == null) {
            String realm = cname.getRealmAsString();
            sname = PrincipalName.tgsService(realm, realm);
        }

        if (till == null) {
            String d = cfg.get("libdefaults", "ticket_lifetime");
            if (d != null) {
                till = new KerberosTime(Instant.now().plusSeconds(Config.duration(d)));
            } else {
                till = new KerberosTime(0); 
            }
        }

        if (rtime == null) {
            String d = cfg.get("libdefaults", "renew_lifetime");
            if (d != null) {
                rtime = new KerberosTime(Instant.now().plusSeconds(Config.duration(d)));
            }
        }

        if (rtime != null) {
            options.set(KDCOptions.RENEWABLE, true);
            if (till.greaterThan(rtime)) {
                rtime = till;
            }
        }

        KDCReqBody kdc_req_body = new KDCReqBody(options,
                                                 cname,
                                                 sname,
                                                 from,
                                                 till,
                                                 rtime,
                                                 Nonce.value(),
                                                 eTypes,
                                                 addresses,
                                                 null,
                                                 null);

        asReqMessg = new ASReq(
                         paData,
                         kdc_req_body);
        obuf = asReqMessg.asn1Encode();
    }

    ASReq getMessage() {
        return asReqMessg;
    }
}
