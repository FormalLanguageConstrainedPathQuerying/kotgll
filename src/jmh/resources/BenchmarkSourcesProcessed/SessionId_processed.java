/*
 * Copyright (c) 1996, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.net.ssl.SSLProtocolException;

/**
 * Encapsulates an SSL session ID.
 *
 * @author Satish Dharmaraj
 * @author David Brownell
 */
final class SessionId {
    static final int MAX_LENGTH = 32;
    private final byte[] sessionId;          

    SessionId(boolean isRejoinable, SecureRandom generator) {
        if (isRejoinable && (generator != null)) {
            sessionId = new RandomCookie(generator).randomBytes;
        } else {
            sessionId = new byte[0];
        }
    }

    SessionId(byte[] sessionId) {
        this.sessionId = sessionId.clone();
    }

    int length() {
        return sessionId.length;
    }

    byte[] getId() {
        return sessionId.clone();
    }

    @Override
    public String toString() {
        if (sessionId.length == 0) {
            return "";
        }

        return Utilities.toHexString(sessionId);
    }


    @Override
    public int hashCode() {
        return Arrays.hashCode(sessionId);
    }

    @Override
    public boolean equals (Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof SessionId that) {
            return MessageDigest.isEqual(this.sessionId, that.sessionId);
        }

        return false;
    }

    /**
     * Checks the length of the session ID to make sure it sits within
     * the range called out in the specification
     */
    void checkLength(int protocolVersion) throws SSLProtocolException {
        if (sessionId.length > MAX_LENGTH) {
            throw new SSLProtocolException("Invalid session ID length (" +
                    sessionId.length + " bytes)");
        }
    }
}
