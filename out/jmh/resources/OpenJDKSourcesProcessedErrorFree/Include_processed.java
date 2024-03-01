/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8029994 8177085 8177398
 * @summary Support "include" and "includedir" in krb5.conf
 * @modules java.security.jgss/sun.security.krb5
 * @compile -XDignore.symbol.file Include.java
 * @run main/othervm Include
 */
import sun.security.krb5.Config;
import sun.security.krb5.KrbException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Include {
    public static void main(String[] args) throws Exception {

        String krb5Conf = "[section]\nkey=";        

        Path conf = Paths.get("krb5.conf");         

        Path f = Paths.get("f");            
        Path f2 = Paths.get("f2");          
        Path d = Paths.get("d");            
        Path dd = Paths.get("d/dd");        
        Path ddf = Paths.get("d/dd/ddf");   
        Path df1 = Paths.get("d/f1");       
        Path df2 = Paths.get("d/f2");       
        Path df3 = Paths.get("d/f.3");      
        Path df4 = Paths.get("d/f4.conf");  
        Path df5 = Paths.get("d/.f5.conf"); 

        System.setProperty("java.security.krb5.conf", "no-such-file");
        tryReload(true);

        System.setProperty("java.security.krb5.conf", conf.toString());

        Files.write(conf,
                ("include " + f.toAbsolutePath() + "\n" +
                        "includedir " + d.toAbsolutePath() + "\n" +
                        krb5Conf + "base").getBytes()
        );

        tryReload(false);

        Files.createDirectory(d);
        tryReload(false);

        Files.write(f,
                ("include " + conf.toAbsolutePath() + "\n" +
                    krb5Conf + "f").getBytes());
        tryReload(false);

        Files.delete(d);
        Files.write(f, (krb5Conf + "f").getBytes());
        tryReload(false);

        Files.createDirectory(d);
        tryReload(true);   

        Files.write(f,
                ("include " + f2.toAbsolutePath() + "\n" +
                        krb5Conf + "f").getBytes());
        Files.write(f2, (krb5Conf + "f2").getBytes());
        Files.write(df1, (krb5Conf + "df1").getBytes());
        Files.write(df2, (krb5Conf + "df2").getBytes());
        Files.write(df3, (krb5Conf + "df3").getBytes());
        Files.createDirectory(dd);
        Files.write(ddf, (krb5Conf + "ddf").getBytes());
        Files.write(df4, (krb5Conf + "df4").getBytes());
        Files.write(df5, (krb5Conf + "df5").getBytes());

        tryReload(true);

        String[] v = Config.getInstance().getAll("section", "key") .split(" ");
        Arrays.sort(v, 2, 5);
        String longv = Arrays.toString(v);
        if (!longv.equals("[f2, f, df1, df2, df4, base]")) {
            throw new Exception(longv);
        }

        Files.write(conf,
                ("include " + f + "\n" +
                        "includedir " + d.toAbsolutePath() + "\n" +
                        krb5Conf + "base").getBytes()
        );
        tryReload(false);

        Files.write(conf,
                ("include " + f.toAbsolutePath() + "\n" +
                        "includedir " + d + "\n" +
                        krb5Conf + "base").getBytes()
        );
        tryReload(false);

        Files.write(conf,
                ("module /lib/lib.so\n" +
                        krb5Conf + "base").getBytes()
        );
        tryReload(true);
    }

    private static void tryReload(boolean expected) throws Exception {
        if (expected) {
            Config.refresh();
        } else {
            try {
                Config.refresh();
                throw new Exception("Should be illegal");
            } catch (KrbException ke) {
            }
        }
    }
}
