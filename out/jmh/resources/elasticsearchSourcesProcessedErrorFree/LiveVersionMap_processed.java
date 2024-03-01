/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.engine;

import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.util.concurrent.KeyedLock;
import org.elasticsearch.core.Releasable;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Maps _uid value to its version information. */
public final class LiveVersionMap implements ReferenceManager.RefreshListener, Accountable {

    private final KeyedLock<BytesRef> keyedLock = new KeyedLock<>();

    private final LiveVersionMapArchive archive;

    LiveVersionMap() {
        this(LiveVersionMapArchive.NOOP_ARCHIVE);
    }

    LiveVersionMap(LiveVersionMapArchive archive) {
        this.archive = archive;
    }

    public static final class VersionLookup {

        /** Tracks bytes used by current map, i.e. what is freed on refresh. For deletes, which are also added to tombstones,
         *  we only account for the CHM entry here, and account for BytesRef/VersionValue against the tombstones, since refresh would not
         *  clear this from RAM. */
        final AtomicLong ramBytesUsed = new AtomicLong();

        private static final VersionLookup EMPTY = new VersionLookup(Collections.emptyMap());
        private final Map<BytesRef, VersionValue> map;

        private boolean unsafe;

        private final AtomicLong minDeleteTimestamp = new AtomicLong(Long.MAX_VALUE);

        public void merge(VersionLookup versionLookup) {
            long existingEntriesSize = 0;
            for (var entry : versionLookup.map.entrySet()) {
                var existingValue = map.get(entry.getKey());
                existingEntriesSize += existingValue == null ? 0 : mapEntryBytesUsed(entry.getKey(), existingValue);
            }
            map.putAll(versionLookup.map);
            adjustRamUsage(versionLookup.ramBytesUsed() - existingEntriesSize);
            minDeleteTimestamp.accumulateAndGet(versionLookup.minDeleteTimestamp(), Math::min);
        }

        VersionLookup(Map<BytesRef, VersionValue> map) {
            this.map = map;
        }

        public VersionValue get(BytesRef key) {
            return map.get(key);
        }

        VersionValue put(BytesRef key, VersionValue value) {
            long ramAccounting = mapEntryBytesUsed(key, value);
            VersionValue previousValue = map.put(key, value);
            ramAccounting += previousValue == null ? 0 : -mapEntryBytesUsed(key, previousValue);
            adjustRamUsage(ramAccounting);
            return previousValue;
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        int size() {
            return map.size();
        }

        public boolean isUnsafe() {
            return unsafe;
        }

        void markAsUnsafe() {
            unsafe = true;
        }

        VersionValue remove(BytesRef uid) {
            VersionValue previousValue = map.remove(uid);
            if (previousValue != null) {
                adjustRamUsage(-mapEntryBytesUsed(uid, previousValue));
            }
            return previousValue;
        }

        public void updateMinDeletedTimestamp(DeleteVersionValue delete) {
            minDeleteTimestamp.accumulateAndGet(delete.time, Math::min);
        }

        public long minDeleteTimestamp() {
            return minDeleteTimestamp.get();
        }

        void adjustRamUsage(long value) {
            if (value != 0) {
                long v = ramBytesUsed.addAndGet(value);
                assert v >= 0 : "bytes=" + v;
            }
        }

        public long ramBytesUsed() {
            return ramBytesUsed.get();
        }

        public static long mapEntryBytesUsed(BytesRef key, VersionValue value) {
            return (BASE_BYTES_PER_BYTESREF + key.bytes.length) + (BASE_BYTES_PER_CHM_ENTRY + value.ramBytesUsed());
        }

        Map<BytesRef, VersionValue> getMap() {
            return map;
        }
    }

    private static final class Maps {

        final VersionLookup current;

        final VersionLookup old;

        boolean needsSafeAccess;
        final boolean previousMapsNeededSafeAccess;

        Maps(VersionLookup current, VersionLookup old, boolean previousMapsNeededSafeAccess) {
            this.current = current;
            this.old = old;
            this.previousMapsNeededSafeAccess = previousMapsNeededSafeAccess;
        }

        Maps() {
            this(new VersionLookup(ConcurrentCollections.newConcurrentMapWithAggressiveConcurrency()), VersionLookup.EMPTY, false);
        }

        boolean isSafeAccessMode() {
            return needsSafeAccess || previousMapsNeededSafeAccess;
        }

        boolean shouldInheritSafeAccess() {
            final boolean mapHasNotSeenAnyOperations = current.isEmpty() && current.isUnsafe() == false;
            return needsSafeAccess
                || (mapHasNotSeenAnyOperations && previousMapsNeededSafeAccess);
        }

        /**
         * Builds a new map for the refresh transition this should be called in beforeRefresh()
         */
        Maps buildTransitionMap() {
            return new Maps(
                new VersionLookup(ConcurrentCollections.newConcurrentMapWithAggressiveConcurrency(current.size())),
                current,
                shouldInheritSafeAccess()
            );
        }

        /**
         * similar to `invalidateOldMap` but used only for the `unsafeKeysMap` used for assertions
         */
        Maps invalidateOldMapForAssert() {
            return new Maps(current, VersionLookup.EMPTY, previousMapsNeededSafeAccess);
        }

        /**
         * builds a new map that invalidates the old map but maintains the current. This should be called in afterRefresh()
         */
        Maps invalidateOldMap(LiveVersionMapArchive archive) {
            archive.afterRefresh(old);
            return new Maps(current, VersionLookup.EMPTY, previousMapsNeededSafeAccess);
        }

        void put(BytesRef uid, VersionValue version) {
            current.put(uid, version);
        }

        void remove(BytesRef uid, DeleteVersionValue deleted) {
            current.remove(uid);
            current.updateMinDeletedTimestamp(deleted);
            if (old != VersionLookup.EMPTY) {
                old.remove(uid);
            }
        }

        long getMinDeleteTimestamp() {
            return Math.min(current.minDeleteTimestamp.get(), old.minDeleteTimestamp.get());
        }

        long ramBytesUsed() {
            return current.ramBytesUsed.get() + old.ramBytesUsed.get();
        }
    }

    private final Map<BytesRef, DeleteVersionValue> tombstones = ConcurrentCollections.newConcurrentMapWithAggressiveConcurrency();

    private volatile Maps maps = new Maps();
    private volatile Maps unsafeKeysMap = new Maps();

    /**
     * Bytes consumed for each BytesRef UID:
     * In this base value, we account for the {@link BytesRef} object itself as
     * well as the header of the byte[] array it holds, and some lost bytes due
     * to object alignment. So consumers of this constant just have to add the
     * length of the byte[] (assuming it is not shared between multiple
     * instances).
     */
    private static final long BASE_BYTES_PER_BYTESREF =
        RamUsageEstimator.shallowSizeOfInstance(BytesRef.class) +
            RamUsageEstimator.NUM_BYTES_ARRAY_HEADER +
            3;

    /**
     * Bytes used by having CHM point to a key/value.
     */
    private static final long BASE_BYTES_PER_CHM_ENTRY;

    static {
        Map<Integer, Integer> map = ConcurrentCollections.newConcurrentMapWithAggressiveConcurrency();
        map.put(0, 0);
        long chmEntryShallowSize = RamUsageEstimator.shallowSizeOf(map.entrySet().iterator().next());
        BASE_BYTES_PER_CHM_ENTRY = chmEntryShallowSize + 2 * RamUsageEstimator.NUM_BYTES_OBJECT_REF;
    }

    /**
     * Tracks bytes used by tombstones (deletes)
     */
    private final AtomicLong ramBytesUsedForTombstones = new AtomicLong();

    @Override
    public void beforeRefresh() throws IOException {
        maps = maps.buildTransitionMap();
        assert (unsafeKeysMap = unsafeKeysMap.buildTransitionMap()) != null;
    }

    @Override
    public void afterRefresh(boolean didRefresh) throws IOException {

        maps = maps.invalidateOldMap(archive);
        assert (unsafeKeysMap = unsafeKeysMap.invalidateOldMapForAssert()) != null;

    }

    /**
     * Returns the live version (add or delete) for this uid.
     */
    VersionValue getUnderLock(final BytesRef uid) {
        return getUnderLock(uid, maps);
    }

    private VersionValue getUnderLock(final BytesRef uid, Maps currentMaps) {
        assert assertKeyedLockHeldByCurrentThread(uid);
        VersionValue value = currentMaps.current.get(uid);
        if (value != null) {
            return value;
        }

        value = currentMaps.old.get(uid);
        if (value != null) {
            return value;
        }

        value = tombstones.get(uid);
        if (value != null) {
            return value;
        }

        return archive.get(uid);
    }

    VersionValue getVersionForAssert(final BytesRef uid) {
        VersionValue value = getUnderLock(uid, maps);
        if (value == null) {
            value = getUnderLock(uid, unsafeKeysMap);
        }
        return value;
    }

    boolean isUnsafe() {
        return maps.current.isUnsafe() || maps.old.isUnsafe() || archive.isUnsafe();
    }

    void enforceSafeAccess() {
        maps.needsSafeAccess = true;
    }

    boolean isSafeAccessRequired() {
        return maps.isSafeAccessMode();
    }

    /**
     * Adds this uid/version to the pending adds map iff the map needs safe access.
     */
    void maybePutIndexUnderLock(BytesRef uid, IndexVersionValue version) {
        assert assertKeyedLockHeldByCurrentThread(uid);
        Maps maps = this.maps;
        if (maps.isSafeAccessMode()) {
            putIndexUnderLock(uid, version);
        } else {
            removeTombstoneUnderLock(uid);
            maps.current.markAsUnsafe();
            assert putAssertionMap(uid, version);
        }
    }

    void putIndexUnderLock(BytesRef uid, IndexVersionValue version) {
        assert assertKeyedLockHeldByCurrentThread(uid);
        assert uid.bytes.length == uid.length : "Oversized _uid! UID length: " + uid.length + ", bytes length: " + uid.bytes.length;
        maps.put(uid, version);
        removeTombstoneUnderLock(uid);
    }

    private boolean putAssertionMap(BytesRef uid, IndexVersionValue version) {
        assert assertKeyedLockHeldByCurrentThread(uid);
        assert uid.bytes.length == uid.length : "Oversized _uid! UID length: " + uid.length + ", bytes length: " + uid.bytes.length;
        unsafeKeysMap.put(uid, version);
        return true;
    }

    void putDeleteUnderLock(BytesRef uid, DeleteVersionValue version) {
        assert assertKeyedLockHeldByCurrentThread(uid);
        assert uid.bytes.length == uid.length : "Oversized _uid! UID length: " + uid.length + ", bytes length: " + uid.bytes.length;
        putTombstone(uid, version);
        maps.remove(uid, version);
    }

    private void putTombstone(BytesRef uid, DeleteVersionValue version) {
        long uidRamBytesUsed = BASE_BYTES_PER_BYTESREF + uid.bytes.length;
        final VersionValue prevTombstone = tombstones.put(uid, version);
        long ramBytes = (BASE_BYTES_PER_CHM_ENTRY + version.ramBytesUsed() + uidRamBytesUsed);
        if (prevTombstone != null) {
            ramBytes -= (BASE_BYTES_PER_CHM_ENTRY + prevTombstone.ramBytesUsed() + uidRamBytesUsed);
        }
        if (ramBytes != 0) {
            long v = ramBytesUsedForTombstones.addAndGet(ramBytes);
            assert v >= 0 : "bytes=" + v;
        }
    }

    /**
     * Removes this uid from the pending deletes map.
     */
    void removeTombstoneUnderLock(BytesRef uid) {
        assert assertKeyedLockHeldByCurrentThread(uid);
        long uidRamBytesUsed = BASE_BYTES_PER_BYTESREF + uid.bytes.length;
        final VersionValue prev = tombstones.remove(uid);
        if (prev != null) {
            assert prev.isDelete();
            long v = ramBytesUsedForTombstones.addAndGet(-(BASE_BYTES_PER_CHM_ENTRY + prev.ramBytesUsed() + uidRamBytesUsed));
            assert v >= 0 : "bytes=" + v;
        }
    }

    private boolean canRemoveTombstone(long maxTimestampToPrune, long maxSeqNoToPrune, DeleteVersionValue versionValue) {
        final boolean isTooOld = versionValue.time < maxTimestampToPrune;
        final boolean isSafeToPrune = versionValue.seqNo <= maxSeqNoToPrune;
        final boolean isNotTrackedByCurrentMaps = versionValue.time < maps.getMinDeleteTimestamp();
        final boolean isNotTrackedByArchive = versionValue.time < archive.getMinDeleteTimestamp();
        return isTooOld && isSafeToPrune && isNotTrackedByCurrentMaps & isNotTrackedByArchive;
    }

    /**
     * Try to prune tombstones whose timestamp is less than maxTimestampToPrune and seqno at most the maxSeqNoToPrune.
     */
    void pruneTombstones(long maxTimestampToPrune, long maxSeqNoToPrune) {
        for (Map.Entry<BytesRef, DeleteVersionValue> entry : tombstones.entrySet()) {
            if (canRemoveTombstone(maxTimestampToPrune, maxSeqNoToPrune, entry.getValue())) {
                final BytesRef uid = entry.getKey();
                try (Releasable lock = keyedLock.tryAcquire(uid)) {
                    if (lock != null) { 
                        final DeleteVersionValue versionValue = tombstones.get(uid);
                        if (versionValue != null) {
                            if (canRemoveTombstone(maxTimestampToPrune, maxSeqNoToPrune, versionValue)) {
                                removeTombstoneUnderLock(uid);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Called when this index is closed.
     */
    synchronized void clear() {
        maps = new Maps();
        tombstones.clear();
    }

    @Override
    public long ramBytesUsed() {
        return maps.ramBytesUsed() + ramBytesUsedForTombstones.get() + ramBytesUsedForArchive();
    }

    /**
     * Returns how much RAM is used by refresh. This is the RAM usage of the current and old version maps, and the RAM usage of the
     * archive, if any.
     */
    long ramBytesUsedForRefresh() {
        return maps.ramBytesUsed() + archive.getRamBytesUsed();
    }

    /**
     * Returns how much RAM could be reclaimed from the version map.
     * <p>
     * In stateful, this is the RAM usage of the current version map, and could be reclaimed by refreshing. It doesn't include tombstones
     * since they don't get cleared on refresh, nor the old version map that is being reclaimed.
     * <p>
     * In stateless, this is the RAM usage of current and old version map plus the RAM usage of the parts of the archive that require
     * a new unpromotable refresh. To reclaim all three components we need to refresh AND flush.
     */
    long reclaimableRefreshRamBytes() {
        return archive == LiveVersionMapArchive.NOOP_ARCHIVE
            ? maps.current.ramBytesUsed.get()
            : maps.ramBytesUsed() + archive.getReclaimableRamBytes();
    }

    /**
     * Returns how much RAM would be freed up by cleaning out the LiveVersionMapArchive.
     */
    long ramBytesUsedForArchive() {
        return archive.getRamBytesUsed();
    }

    /**
     * Returns how much RAM is current being freed up by refreshing. In Stateful, this is the RAM usage of the previous version map
     * that needs to stay around until operations are safely recorded in the Lucene index. In Stateless, this is the RAM usage of a
     * fraction of the Archive entries that are kept around until an ongoing unpromotable refresh is finished.
     */
    long getRefreshingBytes() {
        return archive == LiveVersionMapArchive.NOOP_ARCHIVE ? maps.old.ramBytesUsed.get() : archive.getRefreshingRamBytes();
    }

    /**
     * Returns the current internal versions as a point in time snapshot
     */
    Map<BytesRef, VersionValue> getAllCurrent() {
        return maps.current.map;
    }

    /** Iterates over all deleted versions, including new ones (not yet exposed via reader) and old ones
     *  (exposed via reader but not yet GC'd). */
    Map<BytesRef, DeleteVersionValue> getAllTombstones() {
        return tombstones;
    }

    /**
     * Acquires a releaseable lock for the given uId. All *UnderLock methods require
     * this lock to be hold by the caller otherwise the visibility guarantees of this version
     * map are broken. We assert on this lock to be hold when calling these methods.
     * @see KeyedLock
     */
    Releasable acquireLock(BytesRef uid) {
        return keyedLock.acquire(uid);
    }

    boolean assertKeyedLockHeldByCurrentThread(BytesRef uid) {
        assert keyedLock.isHeldByCurrentThread(uid) : "Thread [" + Thread.currentThread().getName() + "], uid [" + uid.utf8ToString() + "]";
        return true;
    }

    LiveVersionMapArchive getArchive() {
        return archive;
    }
}
