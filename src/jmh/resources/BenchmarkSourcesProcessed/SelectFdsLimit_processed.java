/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8021820
 * @summary The total number of file descriptors is limited to
 * 1024(FDSET_SIZE) on MacOSX (the size of fd array passed to select()
 * call in java.net classes is limited to this value).
 * @run main/othervm SelectFdsLimit
 * @author aleksej.efimov@oracle.com
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;


/*
 * Test must be run in othervm mode to ensure that all files
 * opened by openFiles() are closed propertly.
*/
public class SelectFdsLimit {
    static final int FDTOOPEN = 1023;
    static final String TESTFILE = "testfile";
    static FileInputStream [] testFIS;

    static void prepareTestEnv() throws IOException {
            File fileToCreate = new File(TESTFILE);
            if (!fileToCreate.exists())
                if (!fileToCreate.createNewFile())
                    throw new RuntimeException("Can't create test file");
    }

    static void openFiles(int fn, File f) throws FileNotFoundException, IOException {
        testFIS = new FileInputStream[FDTOOPEN];
        for (;;) {
            if (0 == fn)
                break;
            FileInputStream fis = new FileInputStream(f);
            testFIS[--fn] = fis;
        }
    }

    public static void main(String [] args) throws IOException, FileNotFoundException {

        if (!System.getProperty("os.name").contains("OS X")) {
           return;
        }

        prepareTestEnv();

        openFiles(FDTOOPEN,new File(TESTFILE));

        ServerSocket socket = new ServerSocket(0);

        socket.setSoTimeout(1);

        try {
           socket.accept();
        } catch (SocketTimeoutException e) { }
    }
}
