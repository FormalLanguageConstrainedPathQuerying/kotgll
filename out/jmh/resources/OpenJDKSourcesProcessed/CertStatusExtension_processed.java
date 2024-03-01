/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.security.cert.Extension;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLProtocolException;
import sun.security.provider.certpath.OCSPResponse;
import sun.security.provider.certpath.ResponderId;
import sun.security.ssl.SSLExtension.ExtensionConsumer;
import sun.security.ssl.SSLExtension.SSLExtensionSpec;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.util.HexDumpEncoder;

/**
 * Pack of "status_request" and "status_request_v2" extensions.
 */
final class CertStatusExtension {
    static final HandshakeProducer chNetworkProducer =
            new CHCertStatusReqProducer();
    static final ExtensionConsumer chOnLoadConsumer =
            new CHCertStatusReqConsumer();

    static final HandshakeProducer shNetworkProducer =
            new SHCertStatusReqProducer();
    static final ExtensionConsumer shOnLoadConsumer =
            new SHCertStatusReqConsumer();

    static final HandshakeProducer ctNetworkProducer =
            new CTCertStatusResponseProducer();
    static final ExtensionConsumer ctOnLoadConsumer =
            new CTCertStatusResponseConsumer();

    static final SSLStringizer certStatusReqStringizer =
            new CertStatusRequestStringizer();

    static final HandshakeProducer chV2NetworkProducer =
            new CHCertStatusReqV2Producer();
    static final ExtensionConsumer chV2OnLoadConsumer =
            new CHCertStatusReqV2Consumer();

    static final HandshakeProducer shV2NetworkProducer =
            new SHCertStatusReqV2Producer();
    static final ExtensionConsumer shV2OnLoadConsumer =
            new SHCertStatusReqV2Consumer();

    static final SSLStringizer certStatusReqV2Stringizer =
            new CertStatusRequestsStringizer();

    static final SSLStringizer certStatusRespStringizer =
            new CertStatusRespStringizer();

    /**
     * The "status_request" extension.
     *
     * RFC6066 defines the TLS extension,"status_request" (type 0x5),
     * which allows the client to request that the server perform OCSP
     * on the client's behalf.
     *
     * The "extension data" field of this extension contains a
     * "CertificateStatusRequest" structure:
     *
     *      struct {
     *          CertificateStatusType status_type;
     *          select (status_type) {
     *              case ocsp: OCSPStatusRequest;
     *          } request;
     *      } CertificateStatusRequest;
     *
     *      enum { ocsp(1), (255) } CertificateStatusType;
     *
     *      struct {
     *          ResponderID responder_id_list<0..2^16-1>;
     *          Extensions  request_extensions;
     *      } OCSPStatusRequest;
     *
     *      opaque ResponderID<1..2^16-1>;
     *      opaque Extensions<0..2^16-1>;
     */
    static final class CertStatusRequestSpec implements SSLExtensionSpec {
        static final CertStatusRequestSpec DEFAULT =
                new CertStatusRequestSpec(OCSPStatusRequest.EMPTY_OCSP);

        final CertStatusRequest statusRequest;

        private CertStatusRequestSpec(CertStatusRequest statusRequest) {
            this.statusRequest = statusRequest;
        }

        private CertStatusRequestSpec(HandshakeContext hc,
                ByteBuffer buffer) throws IOException {
            if (buffer.remaining() == 0) {
                this.statusRequest = null;
                return;
            }

            if (buffer.remaining() < 1) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid status_request extension: insufficient data"));
            }

            byte statusType = (byte)Record.getInt8(buffer);
            byte[] encoded = new byte[buffer.remaining()];
            if (encoded.length != 0) {
                buffer.get(encoded);
            }
            if (statusType == CertStatusRequestType.OCSP.id) {
                this.statusRequest = new OCSPStatusRequest(statusType, encoded);
            } else {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.info(
                        "Unknown certificate status request " +
                        "(status type: " + statusType + ")");
                }

                this.statusRequest = new CertStatusRequest(statusType, encoded);
            }
        }

        @Override
        public String toString() {
            return statusRequest == null ?
                        "<empty>" : statusRequest.toString();
        }
    }

    /**
     * Defines the CertificateStatus response structure as outlined in
     * RFC 6066.  This will contain a status response type, plus a single,
     * non-empty OCSP response in DER-encoded form.
     *
     * struct {
     *     CertificateStatusType status_type;
     *     select (status_type) {
     *         case ocsp: OCSPResponse;
     *     } response;
     * } CertificateStatus;
     */
    static final class CertStatusResponseSpec implements SSLExtensionSpec {
        final CertStatusResponse statusResponse;

        private CertStatusResponseSpec(CertStatusResponse resp) {
            this.statusResponse = resp;
        }

        private CertStatusResponseSpec(HandshakeContext hc,
                ByteBuffer buffer) throws IOException {
            if (buffer.remaining() < 2) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid status_request extension: insufficient data"));
            }

            byte type = (byte)Record.getInt8(buffer);
            byte[] respData = Record.getBytes24(buffer);

            if (type == CertStatusRequestType.OCSP.id) {
                this.statusResponse = new OCSPStatusResponse(type, respData);
            } else {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.info(
                        "Unknown certificate status response " +
                        "(status type: " + type + ")");
                }

                this.statusResponse = new CertStatusResponse(type, respData);
            }
        }

        @Override
        public String toString() {
            return statusResponse == null ?
                        "<empty>" : statusResponse.toString();
        }
    }

    private static final
            class CertStatusRequestStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new CertStatusRequestSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                return ioe.getMessage();
            }
        }
    }

    private static final
            class CertStatusRespStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new CertStatusResponseSpec(hc, buffer)).toString();
            } catch (IOException ioe) {
                return ioe.getMessage();
            }
        }
    }

    enum CertStatusRequestType {
        OCSP        ((byte)0x01,    "ocsp"),        
        OCSP_MULTI  ((byte)0x02,    "ocsp_multi");  

        final byte id;
        final String name;

        CertStatusRequestType(byte id, String name) {
            this.id = id;
            this.name = name;
        }

        /**
         * Returns the enum constant of the specified id (see RFC 6066).
         */
        static CertStatusRequestType valueOf(byte id) {
            for (CertStatusRequestType srt : CertStatusRequestType.values()) {
                if (srt.id == id) {
                    return srt;
                }
            }

            return null;
        }

        static String nameOf(byte id) {
            for (CertStatusRequestType srt : CertStatusRequestType.values()) {
                if (srt.id == id) {
                    return srt.name;
                }
            }

            return "UNDEFINED-CERT-STATUS-TYPE(" + id + ")";
        }
    }

    static class CertStatusRequest {
        final byte statusType;
        final byte[] encodedRequest;

        protected CertStatusRequest(byte statusType, byte[] encodedRequest) {
            this.statusType = statusType;
            this.encodedRequest = encodedRequest;
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """
                            "certificate status type": {0}
                            "encoded certificate status": '{'
                            {1}
                            '}'""",
                Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            String encoded = hexEncoder.encodeBuffer(encodedRequest);

            Object[] messageFields = {
                CertStatusRequestType.nameOf(statusType),
                Utilities.indent(encoded)
            };

            return messageFormat.format(messageFields);
        }
    }

    /*
     * RFC6066 defines the TLS extension,"status_request" (type 0x5),
     * which allows the client to request that the server perform OCSP
     * on the client's behalf.
     *
     * The RFC defines an OCSPStatusRequest structure:
     *
     *      struct {
     *          ResponderID responder_id_list<0..2^16-1>;
     *          Extensions  request_extensions;
     *      } OCSPStatusRequest;
     */
    static final class OCSPStatusRequest extends CertStatusRequest {
        static final OCSPStatusRequest EMPTY_OCSP;
        static final OCSPStatusRequest EMPTY_OCSP_MULTI;

        final List<ResponderId> responderIds;
        final List<Extension> extensions;

        static {
            OCSPStatusRequest ocspReq = null;
            OCSPStatusRequest multiReq = null;

            try {
                ocspReq = new OCSPStatusRequest(
                        CertStatusRequestType.OCSP.id,
                        new byte[] {0x00, 0x00, 0x00, 0x00});
                multiReq = new OCSPStatusRequest(
                    CertStatusRequestType.OCSP_MULTI.id,
                    new byte[] {0x00, 0x00, 0x00, 0x00});
            } catch (IOException ioe) {
            }

            EMPTY_OCSP = ocspReq;
            EMPTY_OCSP_MULTI = multiReq;
        }

        private OCSPStatusRequest(byte statusType,
                byte[] encoded) throws IOException {
            super(statusType, encoded);

            if (encoded == null || encoded.length < 4) {
                throw new SSLProtocolException(
                        "Invalid OCSP status request: insufficient data");
            }

            List<ResponderId> rids = new ArrayList<>();
            List<Extension> exts = new ArrayList<>();
            ByteBuffer m = ByteBuffer.wrap(encoded);

            int ridListLen = Record.getInt16(m);
            if (m.remaining() < (ridListLen + 2)) {
                throw new SSLProtocolException(
                        "Invalid OCSP status request: insufficient data");
            }

            int ridListBytesRemaining = ridListLen;
            while (ridListBytesRemaining >= 2) {    
                byte[] ridBytes = Record.getBytes16(m);
                try {
                    rids.add(new ResponderId(ridBytes));
                } catch (IOException ioe) {
                    throw new SSLProtocolException(
                        "Invalid OCSP status request: invalid responder ID");
                }
                ridListBytesRemaining -= ridBytes.length + 2;
            }

            if (ridListBytesRemaining != 0) {
                    throw new SSLProtocolException(
                        "Invalid OCSP status request: incomplete data");
            }

            byte[] extListBytes = Record.getBytes16(m);
            int extListLen = extListBytes.length;
            if (extListLen > 0) {
                try {
                    DerInputStream dis = new DerInputStream(extListBytes);
                    DerValue[] extSeqContents =
                            dis.getSequence(extListBytes.length);
                    for (DerValue extDerVal : extSeqContents) {
                        exts.add(new sun.security.x509.Extension(extDerVal));
                    }
                } catch (IOException ioe) {
                    throw new SSLProtocolException(
                        "Invalid OCSP status request: invalid extension");
                }
            }

            this.responderIds = rids;
            this.extensions = exts;
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """
                            "certificate status type": {0}
                            "OCSP status request": '{'
                            {1}
                            '}'""",
                Locale.ENGLISH);

            MessageFormat requestFormat = new MessageFormat(
                    """
                            "responder_id": {0}
                            "request extensions": '{'
                            {1}
                            '}'""",
                Locale.ENGLISH);

            String ridStr = "<empty>";
            if (!responderIds.isEmpty()) {
                ridStr = responderIds.toString();
            }

            String extsStr = "<empty>";
            if (!extensions.isEmpty()) {
                StringBuilder extBuilder = new StringBuilder(512);
                boolean isFirst = true;
                for (Extension ext : this.extensions) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        extBuilder.append(",\n");
                    }
                    extBuilder.append("{\n").
                            append(Utilities.indent(ext.toString())).
                            append("}");
                }

                extsStr = extBuilder.toString();
            }

            Object[] requestFields = {
                    ridStr,
                    Utilities.indent(extsStr)
                };
            String ocspStatusRequest = requestFormat.format(requestFields);

            Object[] messageFields = {
                    CertStatusRequestType.nameOf(statusType),
                    Utilities.indent(ocspStatusRequest)
                };

            return messageFormat.format(messageFields);
        }
    }

    static class CertStatusResponse {
        final byte statusType;
        final byte[] encodedResponse;

        protected CertStatusResponse(byte statusType, byte[] respDer) {
            this.statusType = statusType;
            this.encodedResponse = respDer;
        }

        byte[] toByteArray() throws IOException {
            byte[] outData = new byte[encodedResponse.length + 4];
            ByteBuffer buf = ByteBuffer.wrap(outData);
            Record.putInt8(buf, statusType);
            Record.putBytes24(buf, encodedResponse);
            return buf.array();
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """
                            "certificate status response type": {0}
                            "encoded certificate status": '{'
                            {1}
                            '}'""",
                Locale.ENGLISH);

            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            String encoded = hexEncoder.encodeBuffer(encodedResponse);

            Object[] messageFields = {
                CertStatusRequestType.nameOf(statusType),
                Utilities.indent(encoded)
            };

            return messageFormat.format(messageFields);
        }
    }

    static final class OCSPStatusResponse extends CertStatusResponse {
        final OCSPResponse ocspResponse;

        private OCSPStatusResponse(byte statusType,
                byte[] encoded) throws IOException {
            super(statusType, encoded);

            if (encoded == null || encoded.length < 1) {
                throw new SSLProtocolException(
                        "Invalid OCSP status response: insufficient data");
            }

            ocspResponse = new OCSPResponse(encoded);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    """
                            "certificate status response type": {0}
                            "OCSP status response": '{'
                            {1}
                            '}'""",
                Locale.ENGLISH);

            Object[] messageFields = {
                CertStatusRequestType.nameOf(statusType),
                Utilities.indent(ocspResponse.toString())
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * Network data producer of a "status_request" extension in the
     * ClientHello handshake message.
     */
    private static final
            class CHCertStatusReqProducer implements HandshakeProducer {
        private CHCertStatusReqProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            if (!chc.sslContext.isStaplingEnabled(true)) {
                return null;
            }

            if (!chc.sslConfig.isAvailable(SSLExtension.CH_STATUS_REQUEST)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Ignore unavailable extension: " +
                        SSLExtension.CH_STATUS_REQUEST.name);
                }
                return null;
            }

            byte[] extData = new byte[] {0x01, 0x00, 0x00, 0x00, 0x00};

            chc.handshakeExtensions.put(SSLExtension.CH_STATUS_REQUEST,
                    CertStatusRequestSpec.DEFAULT);

            return extData;
        }
    }

    /**
     * Network data consumer of a "status_request" extension in the
     * ClientHello handshake message.
     */
    private static final
            class CHCertStatusReqConsumer implements ExtensionConsumer {
        private CHCertStatusReqConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {

            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (!shc.sslConfig.isAvailable(SSLExtension.CH_STATUS_REQUEST)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine("Ignore unavailable extension: " +
                        SSLExtension.CH_STATUS_REQUEST.name);
                }
                return;     
            }

            CertStatusRequestSpec spec = new CertStatusRequestSpec(shc, buffer);

            shc.handshakeExtensions.put(SSLExtension.CH_STATUS_REQUEST, spec);
            if (!shc.isResumption &&
                    !shc.negotiatedProtocol.useTLS13PlusSpec()) {
                shc.handshakeProducers.put(SSLHandshake.CERTIFICATE_STATUS.id,
                    SSLHandshake.CERTIFICATE_STATUS);
            }   

        }
    }

    /**
     * Network data producer of a "status_request" extension in the
     * ServerHello handshake message.
     */
    private static final
            class SHCertStatusReqProducer implements HandshakeProducer {
        private SHCertStatusReqProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if ((shc.stapleParams == null) ||
                    (shc.stapleParams.statusRespExt !=
                    SSLExtension.CH_STATUS_REQUEST)) {
                return null;    
            }

            CertStatusRequestSpec spec = (CertStatusRequestSpec)
                    shc.handshakeExtensions.get(SSLExtension.CH_STATUS_REQUEST);
            if (spec == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest("Ignore unavailable extension: " +
                        SSLExtension.CH_STATUS_REQUEST.name);
                }

                return null;        
            }

            if (shc.isResumption) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "No status_request response for session resuming");
                }

                return null;        
            }

            byte[] extData = new byte[0];

            shc.handshakeExtensions.put(SSLExtension.SH_STATUS_REQUEST,
                    CertStatusRequestSpec.DEFAULT);

            return extData;
        }
    }

    /**
     * Network data consumer of a "status_request" extension in the
     * ServerHello handshake message.
     */
    private static final
            class SHCertStatusReqConsumer implements ExtensionConsumer {
        private SHCertStatusReqConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {

            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            CertStatusRequestSpec requestedCsr = (CertStatusRequestSpec)
                    chc.handshakeExtensions.get(SSLExtension.CH_STATUS_REQUEST);
            if (requestedCsr == null) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                    "Unexpected status_request extension in ServerHello");
            }

            if (buffer.hasRemaining()) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                  "Invalid status_request extension in ServerHello message: " +
                  "the extension data must be empty");
            }

            chc.handshakeExtensions.put(SSLExtension.SH_STATUS_REQUEST,
                    CertStatusRequestSpec.DEFAULT);

            chc.staplingActive = chc.sslContext.isStaplingEnabled(true);
            if (chc.staplingActive) {
                chc.handshakeConsumers.put(SSLHandshake.CERTIFICATE_STATUS.id,
                    SSLHandshake.CERTIFICATE_STATUS);
            }

        }
    }

    /**
     * The "status_request_v2" extension.
     *
     * RFC6961 defines the TLS extension,"status_request_v2" (type 0x5),
     * which allows the client to request that the server perform OCSP
     * on the client's behalf.
     *
     * The RFC defines an CertStatusReqItemV2 structure:
     *
     *      struct {
     *          CertificateStatusType status_type;
     *          uint16 request_length;
     *          select (status_type) {
     *              case ocsp: OCSPStatusRequest;
     *              case ocsp_multi: OCSPStatusRequest;
     *          } request;
     *      } CertificateStatusRequestItemV2;
     *
     *      enum { ocsp(1), ocsp_multi(2), (255) } CertificateStatusType;
     *      struct {
     *        ResponderID responder_id_list<0..2^16-1>;
     *        Extensions request_extensions;
     *      } OCSPStatusRequest;
     *
     *      opaque ResponderID<1..2^16-1>;
     *      opaque Extensions<0..2^16-1>;
     *
     *      struct {
     *        CertificateStatusRequestItemV2
     *                         certificate_status_req_list<1..2^16-1>;
     *      } CertificateStatusRequestListV2;
     */
    static final class CertStatusRequestV2Spec implements SSLExtensionSpec {
        static final CertStatusRequestV2Spec DEFAULT =
                new CertStatusRequestV2Spec(new CertStatusRequest[] {
                        OCSPStatusRequest.EMPTY_OCSP_MULTI});

        final CertStatusRequest[] certStatusRequests;

        private CertStatusRequestV2Spec(CertStatusRequest[] certStatusRequests) {
            this.certStatusRequests = certStatusRequests;
        }

        private CertStatusRequestV2Spec(HandshakeContext hc,
                ByteBuffer message) throws IOException {
            if (message.remaining() == 0) {
                this.certStatusRequests = new CertStatusRequest[0];
                return;
            }

            if (message.remaining() < 5) {  
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "Invalid status_request_v2 extension: insufficient data"));
            }

            int listLen = Record.getInt16(message);
            if (listLen <= 0) {
                throw hc.conContext.fatal(Alert.DECODE_ERROR,
                        new SSLProtocolException(
                    "certificate_status_req_list length must be positive " +
                    "(received length: " + listLen + ")"));
            }

            int remaining = listLen;
            List<CertStatusRequest> statusRequests = new ArrayList<>();
            while (remaining > 0) {
                byte statusType = (byte)Record.getInt8(message);
                int requestLen = Record.getInt16(message);

                if (message.remaining() < requestLen) {
                        throw hc.conContext.fatal(
                                Alert.DECODE_ERROR,
                                new SSLProtocolException(
                            "Invalid status_request_v2 extension: " +
                            "insufficient data (request_length=" + requestLen +
                            ", remaining=" + message.remaining() + ")"));
                }

                byte[] encoded = new byte[requestLen];
                if (encoded.length != 0) {
                    message.get(encoded);
                }
                remaining -= 3;     
                remaining -= requestLen;

                if (statusType == CertStatusRequestType.OCSP.id ||
                        statusType == CertStatusRequestType.OCSP_MULTI.id) {
                    if (encoded.length < 4) {
                        throw hc.conContext.fatal(
                                Alert.DECODE_ERROR,
                                new SSLProtocolException(
                            "Invalid status_request_v2 extension: " +
                            "insufficient data"));
                    }
                    statusRequests.add(
                            new OCSPStatusRequest(statusType, encoded));
                } else {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.info(
                                "Unknown certificate status request " +
                                "(status type: " + statusType + ")");
                    }
                    statusRequests.add(
                            new CertStatusRequest(statusType, encoded));
                }
            }

            certStatusRequests =
                    statusRequests.toArray(new CertStatusRequest[0]);
        }

        @Override
        public String toString() {
            if (certStatusRequests == null || certStatusRequests.length == 0) {
                return "<empty>";
            } else {
                MessageFormat messageFormat = new MessageFormat(
                    "\"cert status request\": '{'\n{0}\n'}'", Locale.ENGLISH);

                StringBuilder builder = new StringBuilder(512);
                boolean isFirst = true;
                for (CertStatusRequest csr : certStatusRequests) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        builder.append(", ");
                    }
                    Object[] messageFields = {
                            Utilities.indent(csr.toString())
                        };
                    builder.append(messageFormat.format(messageFields));
                }

                return builder.toString();
            }
        }
    }

    private static final
            class CertStatusRequestsStringizer implements SSLStringizer {
        @Override
        public String toString(HandshakeContext hc, ByteBuffer buffer) {
            try {
                return (new CertStatusRequestV2Spec(hc, buffer)).toString();
            } catch (IOException ioe) {
                return ioe.getMessage();
            }
        }
    }

    /**
     * Network data producer of a "status_request_v2" extension in the
     * ClientHello handshake message.
     */
    private static final
            class CHCertStatusReqV2Producer implements HandshakeProducer {
        private CHCertStatusReqV2Producer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            if (!chc.sslContext.isStaplingEnabled(true)) {
                return null;
            }

            if (!chc.sslConfig.isAvailable(SSLExtension.CH_STATUS_REQUEST_V2)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "Ignore unavailable status_request_v2 extension");
                }

                return null;
            }

            byte[] extData = new byte[] {
                0x00, 0x07, 0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00};

            chc.handshakeExtensions.put(SSLExtension.CH_STATUS_REQUEST_V2,
                    CertStatusRequestV2Spec.DEFAULT);

            return extData;
        }
    }

    /**
     * Network data consumer of a "status_request_v2" extension in the
     * ClientHello handshake message.
     */
    private static final
            class CHCertStatusReqV2Consumer implements ExtensionConsumer {
        private CHCertStatusReqV2Consumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {

            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            if (!shc.sslConfig.isAvailable(SSLExtension.CH_STATUS_REQUEST_V2)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "Ignore unavailable status_request_v2 extension");
                }

                return;     
            }

            CertStatusRequestV2Spec spec = new CertStatusRequestV2Spec(shc, buffer);

            shc.handshakeExtensions.put(SSLExtension.CH_STATUS_REQUEST_V2,
                    spec);
            if (!shc.isResumption) {
                shc.handshakeProducers.putIfAbsent(
                        SSLHandshake.CERTIFICATE_STATUS.id,
                        SSLHandshake.CERTIFICATE_STATUS);
            }

        }
    }

    /**
     * Network data producer of a "status_request_v2" extension in the
     * ServerHello handshake message.
     */
    private static final
            class SHCertStatusReqV2Producer implements HandshakeProducer {
        private SHCertStatusReqV2Producer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {

            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            if ((shc.stapleParams == null) ||
                    (shc.stapleParams.statusRespExt !=
                    SSLExtension.CH_STATUS_REQUEST_V2)) {
                return null;    
            }

            CertStatusRequestV2Spec spec = (CertStatusRequestV2Spec)
                shc.handshakeExtensions.get(SSLExtension.CH_STATUS_REQUEST_V2);
            if (spec == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "Ignore unavailable status_request_v2 extension");
                }

                return null;        
            }

            if (shc.isResumption) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "No status_request_v2 response for session resumption");
                }
                return null;        
            }

            byte[] extData = new byte[0];

            shc.handshakeExtensions.put(SSLExtension.SH_STATUS_REQUEST_V2,
                    CertStatusRequestV2Spec.DEFAULT);

            return extData;
        }
    }

    /**
     * Network data consumer of a "status_request_v2" extension in the
     * ServerHello handshake message.
     */
    private static final
            class SHCertStatusReqV2Consumer implements ExtensionConsumer {
        private SHCertStatusReqV2Consumer() {
        }

        @Override
        public void consume(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {

            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            CertStatusRequestV2Spec requestedCsr = (CertStatusRequestV2Spec)
                chc.handshakeExtensions.get(SSLExtension.CH_STATUS_REQUEST_V2);
            if (requestedCsr == null) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                    "Unexpected status_request_v2 extension in ServerHello");
            }

            if (buffer.hasRemaining()) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                  "Invalid status_request_v2 extension in ServerHello: " +
                  "the extension data must be empty");
            }

            chc.handshakeExtensions.put(SSLExtension.SH_STATUS_REQUEST_V2,
                    CertStatusRequestV2Spec.DEFAULT);

            chc.staplingActive = chc.sslContext.isStaplingEnabled(true);
            if (chc.staplingActive) {
                chc.handshakeConsumers.put(SSLHandshake.CERTIFICATE_STATUS.id,
                    SSLHandshake.CERTIFICATE_STATUS);
            }

        }
    }

    private static final
            class CTCertStatusResponseProducer implements HandshakeProducer {
        private CTCertStatusResponseProducer() {
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            byte[] producedData;

            if (shc.stapleParams == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest(
                        "Stapling is disabled for this connection");
                }
                return null;
            }

            if (shc.currentCertEntry == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest("Found null CertificateEntry in context");
                }
                return null;
            }

            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate x509Cert =
                        (X509Certificate)cf.generateCertificate(
                                new ByteArrayInputStream(
                                        shc.currentCertEntry.encoded));
                byte[] respBytes = shc.stapleParams.responseMap.get(x509Cert);
                if (respBytes == null) {
                    if (SSLLogger.isOn &&
                            SSLLogger.isOn("ssl,handshake,verbose")) {
                        SSLLogger.finest("No status response found for " +
                                x509Cert.getSubjectX500Principal());
                    }
                    shc.currentCertEntry = null;
                    return null;
                }

                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake,verbose")) {
                    SSLLogger.finest("Found status response for " +
                            x509Cert.getSubjectX500Principal() +
                            ", response length: " + respBytes.length);
                }
                CertStatusResponse certResp = (shc.stapleParams.statReqType ==
                        CertStatusRequestType.OCSP) ?
                        new OCSPStatusResponse(shc.stapleParams.statReqType.id,
                                respBytes) :
                        new CertStatusResponse(shc.stapleParams.statReqType.id,
                                respBytes);
                producedData = certResp.toByteArray();
            } catch (CertificateException ce) {
                throw shc.conContext.fatal(Alert.BAD_CERTIFICATE,
                        "Failed to parse server certificates", ce);
            } catch (IOException ioe) {
                throw shc.conContext.fatal(Alert.BAD_CERT_STATUS_RESPONSE,
                        "Failed to parse certificate status response", ioe);
            }

            shc.currentCertEntry = null;
            return producedData;
        }
    }

    private static final
        class CTCertStatusResponseConsumer implements ExtensionConsumer {
        private CTCertStatusResponseConsumer() {
        }

        @Override
        public void consume(ConnectionContext context,
                HandshakeMessage message, ByteBuffer buffer) throws IOException {
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            CertStatusResponseSpec spec = new CertStatusResponseSpec(chc, buffer);

            if (chc.sslContext.isStaplingEnabled(true)) {
                chc.staplingActive = true;
            } else {
                return;
            }

            if ((chc.handshakeSession != null) && (!chc.isResumption)) {
                List<byte[]> respList = new ArrayList<>(
                        chc.handshakeSession.getStatusResponses());
                respList.add(spec.statusResponse.encodedResponse);
                chc.handshakeSession.setStatusResponses(respList);
            } else {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake,verbose")) {
                    SSLLogger.finest(
                            "Ignoring stapled data on resumed session");
                }
            }
        }
    }
}
