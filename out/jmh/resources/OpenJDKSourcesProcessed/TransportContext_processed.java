/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
import java.net.SocketException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

/**
 * SSL/(D)TLS transportation context.
 */
final class TransportContext implements ConnectionContext {
    final SSLTransport              transport;

    final Map<Byte, SSLConsumer>    consumers;
    @SuppressWarnings("removal")
    final AccessControlContext      acc;

    final SSLContextImpl            sslContext;
    final SSLConfiguration          sslConfig;
    final InputRecord               inputRecord;
    final OutputRecord              outputRecord;

    boolean                         isUnsureMode;
    boolean                         isNegotiated = false;
    boolean                         isBroken = false;
    boolean                         isInputCloseNotified = false;
    boolean                         peerUserCanceled = false;
    Exception                       closeReason = null;
    Exception                       delegatedThrown = null;

    boolean                         needHandshakeFinishedStatus = false;
    boolean                         hasDelegatedFinished = false;

    SSLSessionImpl                  conSession;
    ProtocolVersion                 protocolVersion;
    String                          applicationProtocol= null;

    HandshakeContext                handshakeContext = null;

    boolean                         secureRenegotiation = false;
    byte[]                          clientVerifyData;
    byte[]                          serverVerifyData;

    List<NamedGroup>                serverRequestedNamedGroups;

    CipherSuite cipherSuite;
    private static final byte[] emptyByteArray = new byte[0];

    TransportContext(SSLContextImpl sslContext, SSLTransport transport,
            InputRecord inputRecord, OutputRecord outputRecord) {
        this(sslContext, transport, new SSLConfiguration(sslContext, false),
                inputRecord, outputRecord, true);
    }

    TransportContext(SSLContextImpl sslContext, SSLTransport transport,
            InputRecord inputRecord, OutputRecord outputRecord,
            boolean isClientMode) {
        this(sslContext, transport,
                new SSLConfiguration(sslContext, isClientMode),
                inputRecord, outputRecord, false);
    }

    TransportContext(SSLContextImpl sslContext, SSLTransport transport,
            SSLConfiguration sslConfig,
            InputRecord inputRecord, OutputRecord outputRecord) {
        this(sslContext, transport, (SSLConfiguration)sslConfig.clone(),
                inputRecord, outputRecord, false);
    }

    @SuppressWarnings("removal")
    private TransportContext(SSLContextImpl sslContext, SSLTransport transport,
            SSLConfiguration sslConfig, InputRecord inputRecord,
            OutputRecord outputRecord, boolean isUnsureMode) {
        this.transport = transport;
        this.sslContext = sslContext;
        this.inputRecord = inputRecord;
        this.outputRecord = outputRecord;
        this.sslConfig = sslConfig;
        if (this.sslConfig.maximumPacketSize == 0) {
            this.sslConfig.maximumPacketSize = outputRecord.getMaxPacketSize();
        }
        this.isUnsureMode = isUnsureMode;

        this.conSession = new SSLSessionImpl();
        this.protocolVersion = this.sslConfig.maximumProtocolVersion;
        this.clientVerifyData = emptyByteArray;
        this.serverVerifyData = emptyByteArray;

        this.acc = AccessController.getContext();
        this.consumers = new HashMap<>();
    }

    void dispatch(Plaintext plaintext) throws IOException {
        if (plaintext == null) {
            return;
        }

        ContentType ct = ContentType.valueOf(plaintext.contentType);
        if (ct == null) {
            throw fatal(Alert.UNEXPECTED_MESSAGE,
                "Unknown content type: " + plaintext.contentType);
        }

        switch (ct) {
            case HANDSHAKE:
                byte type = HandshakeContext.getHandshakeType(this,
                        plaintext);
                if (handshakeContext == null) {
                    if (type == SSLHandshake.KEY_UPDATE.id ||
                            type == SSLHandshake.NEW_SESSION_TICKET.id) {
                        if (!isNegotiated) {
                            throw fatal(Alert.UNEXPECTED_MESSAGE,
                                    "Unexpected unnegotiated post-handshake" +
                                            " message: " +
                                            SSLHandshake.nameOf(type));
                        }

                        if (!PostHandshakeContext.isConsumable(this, type)) {
                            throw fatal(Alert.UNEXPECTED_MESSAGE,
                                    "Unexpected post-handshake message: " +
                                    SSLHandshake.nameOf(type));
                        }

                        handshakeContext = new PostHandshakeContext(this);
                    } else {
                        handshakeContext = sslConfig.isClientMode ?
                                new ClientHandshakeContext(sslContext, this) :
                                new ServerHandshakeContext(sslContext, this);
                        outputRecord.initHandshaker();
                    }
                }
                handshakeContext.dispatch(type, plaintext);
                break;
            case ALERT:
                Alert.alertConsumer.consume(this, plaintext.fragment);
                break;
            default:
                SSLConsumer consumer = consumers.get(plaintext.contentType);
                if (consumer != null) {
                    consumer.consume(this, plaintext.fragment);
                } else {
                    throw fatal(Alert.UNEXPECTED_MESSAGE,
                        "Unexpected content: " + plaintext.contentType);
                }
        }
    }

    void kickstart() throws IOException {
        if (isUnsureMode) {
            throw new IllegalStateException("Client/Server mode not yet set.");
        }

        boolean isNotUsable = outputRecord.writeCipher.atKeyLimit() ?
            (outputRecord.isClosed() || isBroken) :
            (outputRecord.isClosed() || inputRecord.isClosed() || isBroken);
        if (isNotUsable) {
            if (closeReason != null) {
                throw new SSLException(
                        "Cannot kickstart, the connection is broken or closed",
                        closeReason);
            } else {
                throw new SSLException(
                        "Cannot kickstart, the connection is broken or closed");
            }
        }

        if (handshakeContext == null) {
            if (isNegotiated && protocolVersion.useTLS13PlusSpec()) {
                handshakeContext = new PostHandshakeContext(this);
            } else {
                handshakeContext = sslConfig.isClientMode ?
                        new ClientHandshakeContext(sslContext, this) :
                        new ServerHandshakeContext(sslContext, this);
                outputRecord.initHandshaker();
            }
        }

        if (isNegotiated || sslConfig.isClientMode) {
           handshakeContext.kickstart();
        }
    }

    boolean isPostHandshakeContext() {
        return handshakeContext != null &&
                (handshakeContext instanceof PostHandshakeContext);
    }

    void warning(Alert alert) {
        if (isNegotiated || handshakeContext != null) {
            try {
                outputRecord.encodeAlert(Alert.Level.WARNING.level, alert.id);
            } catch (IOException ioe) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.warning(
                        "Warning: failed to send warning alert " + alert, ioe);
                }
            }
        }
    }

    void closeNotify(boolean isUserCanceled) throws IOException {
        if (transport instanceof SSLSocketImpl) {
            ((SSLSocketImpl)transport).closeNotify(isUserCanceled);
        } else {
            outputRecord.recordLock.lock();
            try {
                try {
                    if (isUserCanceled) {
                        warning(Alert.USER_CANCELED);
                    }

                    warning(Alert.CLOSE_NOTIFY);
                } finally {
                    outputRecord.close();
                }
            } finally {
                outputRecord.recordLock.unlock();
            }
        }
    }

    SSLException fatal(Alert alert,
            String diagnostic) throws SSLException {
        return fatal(alert, diagnostic, null);
    }

    SSLException fatal(Alert alert, Throwable cause) throws SSLException {
        return fatal(alert, null, cause);
    }

    SSLException fatal(Alert alert,
            String diagnostic, Throwable cause) throws SSLException {
        return fatal(alert, diagnostic, false, cause);
    }

    SSLException fatal(Alert alert, String diagnostic,
            boolean recvFatalAlert, Throwable cause) throws SSLException {
        if (closeReason != null) {
            if (cause == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.warning(
                            "Closed transport, general or untracked problem");
                }
                throw alert.createSSLException(
                        "Closed transport, general or untracked problem");
            }

            if (cause instanceof SSLException) {
                throw (SSLException)cause;
            } else {    
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.warning(
                            "Closed transport, unexpected rethrowing", cause);
                }
                throw alert.createSSLException("Unexpected rethrowing", cause);
            }
        }

        if (diagnostic == null) {
            if (cause == null) {
                diagnostic = "General/Untracked problem";
            } else {
                diagnostic = cause.getMessage();
            }
        }

        if (cause == null) {
            cause = alert.createSSLException(diagnostic);
        }

        if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
            SSLLogger.severe("Fatal (" + alert + "): " + diagnostic, cause);
        }

        if (cause instanceof SSLException) {
            closeReason = (SSLException)cause;
        } else {
            closeReason = alert.createSSLException(diagnostic, cause);
        }

        try {
            inputRecord.close();
        } catch (IOException ioe) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.warning("Fatal: input record closure failed", ioe);
            }

            closeReason.addSuppressed(ioe);
        }

        if (conSession != null) {
            if (!(cause instanceof SocketException)) {
                conSession.invalidate();
            }
        }

        if (handshakeContext != null &&
                handshakeContext.handshakeSession != null) {
            handshakeContext.handshakeSession.invalidate();
        }

        if (!recvFatalAlert && !isOutboundClosed() && !isBroken &&
                (isNegotiated || handshakeContext != null)) {
            try {
                outputRecord.encodeAlert(Alert.Level.FATAL.level, alert.id);
            } catch (IOException ioe) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.warning(
                        "Fatal: failed to send fatal alert " + alert, ioe);
                }

                closeReason.addSuppressed(ioe);
            }
        }

        try {
            outputRecord.close();
        } catch (IOException ioe) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.warning("Fatal: output record closure failed", ioe);
            }

            closeReason.addSuppressed(ioe);
        }

        if (handshakeContext != null) {
            handshakeContext = null;
        }

        try {
            transport.shutdown();
        } catch (IOException ioe) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.warning("Fatal: transport closure failed", ioe);
            }

            closeReason.addSuppressed(ioe);
        } finally {
            isBroken = true;
        }

        if (closeReason instanceof SSLException) {
            throw (SSLException)closeReason;
        } else {
            throw (RuntimeException)closeReason;
        }
    }

    void setUseClientMode(boolean useClientMode) {
        if (handshakeContext != null || isNegotiated) {
            throw new IllegalArgumentException(
                    "Cannot change mode after SSL traffic has started");
        }

        /*
         * If we need to change the client mode and the enabled
         * protocols and cipher suites haven't specifically been
         * set by the user, change them to the corresponding
         * default ones.
         */
        if (sslConfig.isClientMode != useClientMode) {
            if (sslContext.isDefaultProtocolVesions(
                    sslConfig.enabledProtocols)) {
                sslConfig.enabledProtocols =
                        sslContext.getDefaultProtocolVersions(!useClientMode);
            }

            if (sslContext.isDefaultCipherSuiteList(
                    sslConfig.enabledCipherSuites)) {
                sslConfig.enabledCipherSuites =
                        sslContext.getDefaultCipherSuites(!useClientMode);
            }

            sslConfig.toggleClientMode();
        }

        isUnsureMode = false;
    }

    boolean isOutboundDone() {
        return outputRecord.isClosed() && outputRecord.isEmpty();
    }

    boolean isOutboundClosed() {
        return outputRecord.isClosed();
    }

    boolean isInboundClosed() {
        return inputRecord.isClosed();
    }

    void closeInbound() {
        if (isInboundClosed()) {
            return;
        }

        try {
            if (!isInputCloseNotified) {
                initiateInboundClose();
            } else {
                passiveInboundClose();
            }
        } catch (IOException ioe) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.warning("inbound closure failed", ioe);
            }
        }
    }

    private void passiveInboundClose() throws IOException {
        if (!isInboundClosed()) {
            inputRecord.close();
        }

        if (!isOutboundClosed()) {
            boolean needCloseNotify = SSLConfiguration.acknowledgeCloseNotify;
            if (!needCloseNotify) {
                if (isNegotiated) {
                    if (!protocolVersion.useTLS13PlusSpec()) {
                        needCloseNotify = true;
                    }
                } else if (handshakeContext != null) {  
                    ProtocolVersion pv = handshakeContext.negotiatedProtocol;
                    if (pv == null || (!pv.useTLS13PlusSpec())) {
                        needCloseNotify = true;
                    }
                }
            }

            if (needCloseNotify) {
                closeNotify(false);
            }
        }
    }

    private void initiateInboundClose() throws IOException {
        if (!isInboundClosed()) {
            inputRecord.close();
        }
    }

    void closeOutbound() {
        if (isOutboundClosed()) {
            return;
        }

        try {
             initiateOutboundClose();
        } catch (IOException ioe) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.warning("outbound closure failed", ioe);
            }
        }
    }

    private void initiateOutboundClose() throws IOException {
        boolean useUserCanceled = !isNegotiated &&
                (handshakeContext != null) && !peerUserCanceled;

        closeNotify(useUserCanceled);
    }

    HandshakeStatus getHandshakeStatus() {
        if (!outputRecord.isEmpty()) {
            return HandshakeStatus.NEED_WRAP;
        } else if (isOutboundClosed() && isInboundClosed()) {
            return HandshakeStatus.NOT_HANDSHAKING;
        } else if (handshakeContext != null) {
            if (!handshakeContext.delegatedActions.isEmpty()) {
                return HandshakeStatus.NEED_TASK;
            } else if (!isInboundClosed()) {
                if (sslContext.isDTLS() && !inputRecord.isEmpty()) {
                    return HandshakeStatus.NEED_UNWRAP_AGAIN;
                } else {
                    return HandshakeStatus.NEED_UNWRAP;
                }
            } else if (!isOutboundClosed()) {
                return HandshakeStatus.NEED_WRAP;
            }   
        } else if (needHandshakeFinishedStatus) {
            return HandshakeStatus.NEED_WRAP;
        }

        return HandshakeStatus.NOT_HANDSHAKING;
    }

    HandshakeStatus finishHandshake() {
        if (protocolVersion.useTLS13PlusSpec()) {
            outputRecord.tc = this;
            inputRecord.tc = this;
            cipherSuite = handshakeContext.negotiatedCipherSuite;
            inputRecord.readCipher.baseSecret =
                    handshakeContext.baseReadSecret;
            outputRecord.writeCipher.baseSecret =
                    handshakeContext.baseWriteSecret;
        }

        handshakeContext = null;
        outputRecord.handshakeHash.finish();
        inputRecord.finishHandshake();
        outputRecord.finishHandshake();
        isNegotiated = true;

        if (transport instanceof SSLSocket &&
                sslConfig.handshakeListeners != null &&
                !sslConfig.handshakeListeners.isEmpty()) {
            HandshakeCompletedEvent hce =
                new HandshakeCompletedEvent((SSLSocket)transport, conSession);
            Thread thread = new Thread(
                null,
                new NotifyHandshake(sslConfig.handshakeListeners, hce),
                "HandshakeCompletedNotify-Thread",
                0,
                false);
            thread.start();
        }

        return HandshakeStatus.FINISHED;
    }

    HandshakeStatus finishPostHandshake() {
        handshakeContext = null;


        return HandshakeStatus.FINISHED;
    }

    private static class NotifyHandshake implements Runnable {
        @SuppressWarnings("removal")
        private final Set<Map.Entry<HandshakeCompletedListener,
                AccessControlContext>> targets;         
        private final HandshakeCompletedEvent event;    

        NotifyHandshake(
                @SuppressWarnings("removal")
                Map<HandshakeCompletedListener,AccessControlContext> listeners,
                HandshakeCompletedEvent event) {
            this.targets = new HashSet<>(listeners.entrySet());     
            this.event = event;
        }

        @SuppressWarnings("removal")
        @Override
        public void run() {
            for (Map.Entry<HandshakeCompletedListener,
                    AccessControlContext> entry : targets) {
                final HandshakeCompletedListener listener = entry.getKey();
                AccessControlContext acc = entry.getValue();
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        listener.handshakeCompleted(event);
                        return null;
                    }
                }, acc);
            }
        }
    }
}
