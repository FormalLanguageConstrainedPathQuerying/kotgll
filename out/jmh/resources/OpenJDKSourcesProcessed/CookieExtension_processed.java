/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Locale;
import javax.net.ssl.SSLProtocolException;

import sun.security.ssl.ClientHello.ClientHelloMessage;
import sun.security.ssl.SSLExtension.ExtensionConsumer;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.SSLExtension.SSLExtensionSpec;
import sun.security.ssl.ServerHello.ServerHelloMessage;
import sun.security.util.HexDumpEncoder;

public class CookieExtension {
    static final HandshakeProducer chNetworkProducer =
            new CHCookieProducer();
    static final ExtensionConsumer chOnLoadConsumer =
            new CHCookieConsumer();
    static final HandshakeConsumer chOnTradeConsumer =
            new CHCookieUpdate();

    static final HandshakeProducer hrrNetworkProducer =
            new HRRCookieProducer();
    static final ExtensionConsumer hrrOnLoadConsumer =
            new HRRCookieConsumer();

    static final HandshakeProducer hrrNetworkReproducer =
            new HRRCookieReproducer();

    static final CookieStringizer cookieStringizer =
            new CookieStringizer();

    /**
     * The "cookie" extension.
     */
    static class CookieSpec implements SSLExtensionSpec {
        final byte[] cookie;

        private CookieSpec(HandshakeContext hc,
                ByteBuffer m) throws IOException {
            if (m.remaining() < 3) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid cookie extension: insufficient data"));
            }

            this.cookie = Record.getBytes16(m);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """
                            "cookie": '{'
                            {0}
                            '}',""", Locale.ENGLISH);
            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            Object[] messageFields = {
                Utilities.indent(hexEncoder.encode(cookie))
            };

            return messageFormat.format(messageFields);
        }
    }

    private static final class CookieStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new CookieSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                return ioe.getMessage();
            }
        }
    }

    private static final
            class CHCookieProducer implements HandshakeProducer {
        private CHCookieProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext) context;

            if (!chc.sslConfig.isAvailable(SSLExtension.CH_COOKIE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable cookie extension");
                }
                return null;
            }

            CookieSpec spec = (CookieSpec)chc.handshakeExtensions.get(
                    SSLExtension.HRR_COOKIE);

            if (spec != null && spec.cookie.length != 0) {
                byte[] extData = new byte[spec.cookie.length + 2];
                ByteBuffer m = ByteBuffer.wrap(extData);
                Record.putBytes16(m, spec.cookie);
                return extData;
            }

            return null;
        }
    }

    private static final
            class CHCookieConsumer implements ExtensionConsumer {
        private CHCookieConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (!shc.sslConfig.isAvailable(SSLExtension.CH_COOKIE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable cookie extension");
                }
                return;     
            }

            CookieSpec spec = new CookieSpec(shc, buffer);
            shc.handshakeExtensions.put(SSLExtension.CH_COOKIE, spec);

        }
    }

    private static final
            class CHCookieUpdate implements HandshakeConsumer {
        private CHCookieUpdate() {
        }

        @Override
        public void consume(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            ClientHelloMessage clientHello = (ClientHelloMessage)message;

            CookieSpec spec = (CookieSpec)
                    shc.handshakeExtensions.get(SSLExtension.CH_COOKIE);
            if (spec == null) {
                return;
            }

            HelloCookieManager hcm =
                shc.sslContext.getHelloCookieManager(shc.negotiatedProtocol);
            if (!hcm.isCookieValid(shc, clientHello, spec.cookie)) {
                throw shc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        "unrecognized cookie");
            }
        }
    }

    private static final
            class HRRCookieProducer implements HandshakeProducer {
        private HRRCookieProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            ServerHelloMessage hrrm = (ServerHelloMessage)message;

            if (!shc.sslConfig.isAvailable(SSLExtension.HRR_COOKIE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable cookie extension");
                }
                return null;
            }

            HelloCookieManager hcm =
                shc.sslContext.getHelloCookieManager(shc.negotiatedProtocol);

            byte[] cookie = hcm.createCookie(shc, hrrm.clientHello);

            byte[] extData = new byte[cookie.length + 2];
            ByteBuffer m = ByteBuffer.wrap(extData);
            Record.putBytes16(m, cookie);

            return extData;
        }
    }

    private static final
            class HRRCookieConsumer implements ExtensionConsumer {
        private HRRCookieConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            if (!chc.sslConfig.isAvailable(SSLExtension.HRR_COOKIE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable cookie extension");
                }
                return;     
            }

            CookieSpec spec = new CookieSpec(chc, buffer);
            chc.handshakeExtensions.put(SSLExtension.HRR_COOKIE, spec);
        }
    }

    private static final
            class HRRCookieReproducer implements HandshakeProducer {
        private HRRCookieReproducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext) context;

            if (!shc.sslConfig.isAvailable(SSLExtension.HRR_COOKIE)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                            "Ignore unavailable cookie extension");
                }
                return null;
            }

            CookieSpec spec = (CookieSpec)shc.handshakeExtensions.get(
                    SSLExtension.CH_COOKIE);

            if (spec != null && spec.cookie.length != 0) {
                byte[] extData = new byte[spec.cookie.length + 2];
                ByteBuffer m = ByteBuffer.wrap(extData);
                Record.putBytes16(m, spec.cookie);
                return extData;
            }

            return null;
        }
    }
}
