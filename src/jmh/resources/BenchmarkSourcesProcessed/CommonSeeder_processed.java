/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.security.DrbgParameters;
import java.security.SecureRandom;
import java.security.Security;

import sun.security.provider.SeedGenerator;
/**
 * @test
 * @bug 8051408 8202608
 * @modules java.base/sun.security.provider
 * @build java.base/sun.security.provider.SeedGenerator
 * @run main/othervm CommonSeeder
 * @summary check entropy reading of DRBGs
 */
public class CommonSeeder {

    public static void main(String[] args) throws Exception {

        byte[] result = new byte[10];


        SeedGenerator.checkUsage(0);

        SecureRandom sr;
        sr = SecureRandom.getInstance("DRBG");

        SeedGenerator.checkUsage(0);

        sr.nextInt();
        SeedGenerator.checkUsage(1);

        for (String mech : new String[]{"Hash_DRBG", "HMAC_DRBG", "CTR_DRBG"}) {
            System.out.println("Testing " + mech + "...");


            Security.setProperty("securerandom.drbg.config", mech);
            sr = SecureRandom.getInstance("DRBG");
            sr.nextInt();
            sr.reseed();
            SeedGenerator.checkUsage(0);


            Security.setProperty("securerandom.drbg.config",
                    mech + ",pr_and_reseed");
            sr = SecureRandom.getInstance("DRBG");

            sr.nextInt();
            SeedGenerator.checkUsage(2); 
            sr.nextInt();
            SeedGenerator.checkUsage(1); 
            sr.reseed();
            SeedGenerator.checkUsage(1); 
            sr.nextBytes(result, DrbgParameters.nextBytes(-1, false, null));
            SeedGenerator.checkUsage(0); 
            sr.nextBytes(result, DrbgParameters.nextBytes(-1, true, null));
            SeedGenerator.checkUsage(1); 
            sr.reseed(DrbgParameters.reseed(true, null));
            SeedGenerator.checkUsage(1); 
            sr.reseed(DrbgParameters.reseed(false, null));
            SeedGenerator.checkUsage(0); 
        }
    }
}
