/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * These are essentially flake ids but we use 6 (not 8) bytes for timestamp, and use 3 (not 2) bytes for sequence number. We also reorder
 * bytes in a way that does not make ids sort in order anymore, but is more friendly to the way that the Lucene terms dictionary is
 * structured.
 * For more information about flake ids, check out
 * https:
 */

class TimeBasedUUIDGenerator implements UUIDGenerator {

    private final AtomicInteger sequenceNumber = new AtomicInteger(SecureRandomHolder.INSTANCE.nextInt());

    private final AtomicLong lastTimestamp = new AtomicLong(0);

    private static final byte[] SECURE_MUNGED_ADDRESS = MacAddressProvider.getSecureMungedAddress();

    static {
        assert SECURE_MUNGED_ADDRESS.length == 6;
    }

    private static final Base64.Encoder BASE_64_NO_PADDING = Base64.getUrlEncoder().withoutPadding();

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    protected byte[] macAddress() {
        return SECURE_MUNGED_ADDRESS;
    }

    @Override
    public String getBase64UUID() {
        final int sequenceId = sequenceNumber.incrementAndGet() & 0xffffff;

        long timestamp = this.lastTimestamp.accumulateAndGet(
            currentTimeMillis(),
            sequenceId == 0 ? (lastTimestamp, currentTimeMillis) -> Math.max(lastTimestamp, currentTimeMillis) + 1 : Math::max
        );

        final byte[] uuidBytes = new byte[15];
        int i = 0;



        uuidBytes[i++] = (byte) sequenceId;
        uuidBytes[i++] = (byte) (sequenceId >>> 16);

        uuidBytes[i++] = (byte) (timestamp >>> 16); 
        uuidBytes[i++] = (byte) (timestamp >>> 24); 
        uuidBytes[i++] = (byte) (timestamp >>> 32); 
        uuidBytes[i++] = (byte) (timestamp >>> 40); 
        byte[] macAddress = macAddress();
        assert macAddress.length == 6;
        System.arraycopy(macAddress, 0, uuidBytes, i, macAddress.length);
        i += macAddress.length;

        uuidBytes[i++] = (byte) (timestamp >>> 8);
        uuidBytes[i++] = (byte) (sequenceId >>> 8);
        uuidBytes[i++] = (byte) timestamp;

        assert i == uuidBytes.length;

        return BASE_64_NO_PADDING.encodeToString(uuidBytes);
    }
}
