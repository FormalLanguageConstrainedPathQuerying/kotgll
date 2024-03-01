/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8215790 8219389
 * @summary Verify exception
 * @library /test/lib
 * @modules java.base/sun.security.util
 * @run main/othervm ClientHelloBufferUnderflowException
 */

import javax.net.ssl.SSLHandshakeException;

import jdk.test.lib.hexdump.HexPrinter;

public class ClientHelloBufferUnderflowException extends ClientHelloInterOp {
    /*
     * Main entry point for this test.
     */
    public static void main(String args[]) throws Exception {
        try {
            (new ClientHelloBufferUnderflowException()).run();
        } catch (SSLHandshakeException e) {
            System.out.println("Correct exception thrown: " + e);
            return;
        } catch (Exception e) {
            System.out.println("Failed: Exception not SSLHandShakeException");
            System.out.println(e.getMessage());
            throw e;
        }

        throw new Exception("No expected exception");
    }

    @Override
    protected byte[] createClientHelloMessage() {
        byte[] bytes = {
            0x16, 0x03, 0x01, 0x00, 0x05, 0x01, 0x00, 0x00, 0x01, 0x03};

        System.out.println("The ClientHello message used");
        try {
            HexPrinter.simple().format(bytes);
        } catch (Exception e) {
        }

        return bytes;
    }
}
