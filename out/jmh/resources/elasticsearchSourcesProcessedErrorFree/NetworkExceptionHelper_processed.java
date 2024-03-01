/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.transport;

import org.apache.logging.log4j.Level;

import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;

public class NetworkExceptionHelper {

    public static boolean isConnectException(Throwable e) {
        return e instanceof ConnectException;
    }

    /**
     * @return a log level indicating an approximate severity of the exception, or {@link Level#OFF} if the exception doesn't look to be
     *         network-related.
     */
    public static Level getCloseConnectionExceptionLevel(Throwable e, boolean rstOnClose) {
        if (e instanceof ClosedChannelException) {
            return Level.DEBUG;
        }

        final String message = e.getMessage();
        if (message != null) {


            if (message.contains("Connection timed out")) {
                return Level.INFO;
            }
            if (message.contains("Connection reset")) {
                return rstOnClose ? Level.DEBUG : Level.INFO;
            }
            if (message.contains("Broken pipe")) {
                return Level.INFO;
            }


            if (message.contains("connection was aborted") || message.contains("forcibly closed")) {
                return Level.INFO;
            }


            if (message.equals("Socket is closed") || message.equals("Socket closed")) {
                return Level.DEBUG;
            }


            if (message.equals("SSLEngine closed already")) {
                return Level.DEBUG;
            }
        }
        return Level.OFF;
    }
}
