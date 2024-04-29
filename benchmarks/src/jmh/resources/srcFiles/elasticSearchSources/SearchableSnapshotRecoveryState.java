/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.searchablesnapshots.recovery;

import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.indices.recovery.RecoveryState;

import java.util.HashSet;
import java.util.Set;

public final class SearchableSnapshotRecoveryState extends RecoveryState {
    private boolean preWarmComplete;
    private boolean remoteTranslogSet;

    public SearchableSnapshotRecoveryState(ShardRouting shardRouting, DiscoveryNode targetNode, @Nullable DiscoveryNode sourceNode) {
        super(shardRouting, targetNode, sourceNode, new Index());
    }

    @Override
    public synchronized RecoveryState setStage(Stage stage) {
        if (getStage() == Stage.DONE || stage == Stage.FINALIZE && remoteTranslogSet) {
            return this;
        }

        if (preWarmComplete == false && stage == Stage.DONE) {
            validateCurrentStage(Stage.FINALIZE);
            return this;
        }

        if (stage == Stage.INIT) {
            remoteTranslogSet = false;
        }

        return super.setStage(stage);
    }

    @Override
    public synchronized RecoveryState setRemoteTranslogStage() {
        remoteTranslogSet = true;
        super.setStage(Stage.TRANSLOG);
        return super.setStage(Stage.FINALIZE);
    }

    @Override
    public synchronized RecoveryState reset() {
        setStage(Stage.INIT);
        return this;
    }

    @Override
    public synchronized void validateCurrentStage(Stage expected) {
        if (remoteTranslogSet == false) {
            super.validateCurrentStage(expected);
        } else {
            final Stage stage = getStage();
            if (stage != Stage.FINALIZE && stage != Stage.DONE) {
                assert false : "expected stage [" + Stage.FINALIZE + " || " + Stage.DONE + "]; but current stage is [" + stage + "]";
                throw new IllegalStateException(
                    "expected stage [" + Stage.FINALIZE + " || " + Stage.DONE + "]; " + "but current stage is [" + stage + "]"
                );
            }
        }
    }

    boolean isRemoteTranslogSet() {
        return remoteTranslogSet;
    }

    public synchronized void setPreWarmComplete() {
        if (getStage() == Stage.FINALIZE) {
            super.setStage(Stage.DONE);
        }

        SearchableSnapshotRecoveryState.Index index = (Index) getIndex();
        index.stopTimer();
        preWarmComplete = true;
    }

    public synchronized boolean isPreWarmComplete() {
        return preWarmComplete;
    }

    public synchronized void ignoreFile(String name) {
        SearchableSnapshotRecoveryState.Index index = (Index) getIndex();
        index.addFileToIgnore(name);
    }

    public synchronized void markIndexFileAsReused(String name) {
        SearchableSnapshotRecoveryState.Index index = (Index) getIndex();
        index.markFileAsReused(name);
    }

    private static final class Index extends RecoveryState.Index {
        private final Set<String> filesToIgnore = new HashSet<>();

        private Index() {
            super(new SearchableSnapshotRecoveryFilesDetails());
            super.start();
        }

        private synchronized void addFileToIgnore(String name) {
            filesToIgnore.add(name);
        }

        @Override
        public synchronized void addFileDetail(String name, long length, boolean reused) {
            if (filesToIgnore.contains(name)) {
                return;
            }

            super.addFileDetail(name, length, reused);
        }

        private synchronized void markFileAsReused(String name) {
            ((SearchableSnapshotRecoveryFilesDetails) fileDetails).markFileAsReused(name);
        }

        @Override
        public synchronized void start() {}

        @Override
        public synchronized void stop() {}

        @Override
        public synchronized void reset() {}

        private synchronized void stopTimer() {
            super.stop();
        }
    }

    private static class SearchableSnapshotRecoveryFilesDetails extends RecoveryFilesDetails {
        @Override
        public void addFileDetails(String name, long length, boolean reused) {
            FileDetail fileDetail = fileDetails.computeIfAbsent(name, n -> new FileDetail(name, length, reused));
            assert fileDetail == null || fileDetail.name().equals(name) && fileDetail.length() == length
                : "The file "
                    + name
                    + " was reported multiple times with different lengths: ["
                    + fileDetail.length()
                    + "] and ["
                    + length
                    + "]";
        }

        void markFileAsReused(String name) {
            final FileDetail fileDetail = fileDetails.get(name);
            assert fileDetail != null;
            fileDetails.put(name, new FileDetail(fileDetail.name(), fileDetail.length(), true));
        }

        @Override
        public void clear() {
            complete = false;
        }
    }
}
