/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.snapshots;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.AssociatedIndexDescriptor;
import org.elasticsearch.indices.SystemIndexDescriptor;
import org.elasticsearch.indices.SystemIndexDescriptorUtils;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SystemIndexPlugin;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESIntegTestCase;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 0)
public class SystemIndicesSnapshotIT extends AbstractSnapshotIntegTestCase {

    public static final String REPO_NAME = "test-repo";

    private List<String> dataNodes = null;

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        List<Class<? extends Plugin>> plugins = new ArrayList<>(super.nodePlugins());
        plugins.add(SystemIndexTestPlugin.class);
        plugins.add(AnotherSystemIndexTestPlugin.class);
        plugins.add(AssociatedIndicesTestPlugin.class);
        return plugins;
    }

    @Before
    public void setup() {
        internalCluster().startMasterOnlyNodes(2);
        dataNodes = internalCluster().startDataOnlyNodes(2);
    }

    /**
     * Test that if a snapshot includes system indices and we restore global state,
     * with no reference to feature state, the system indices are restored too.
     */
    public void testRestoreSystemIndicesAsGlobalState() {
        createRepository(REPO_NAME, "fs");
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        createFullSnapshot(REPO_NAME, "test-snap");

        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "2", "purpose", "post-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        assertThat(getDocCount(SystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(2L));

        RestoreSnapshotResponse restoreSnapshotResponse = clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setRestoreGlobalState(true)
            .get();
        assertThat(restoreSnapshotResponse.getRestoreInfo().totalShards(), greaterThan(0));

        assertThat(getDocCount(SystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(1L));
    }

    /**
     * If we take a snapshot with includeGlobalState set to false, are system indices included?
     */
    public void testSnapshotWithoutGlobalState() {
        createRepository(REPO_NAME, "fs");
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "system index doc");
        indexDoc("not-a-system-index", "1", "purpose", "non system index doc");

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(false)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        clusterAdmin().prepareGetRepositories(REPO_NAME).get();
        Set<String> snapshottedIndices = clusterAdmin().prepareGetSnapshots(REPO_NAME)
            .get()
            .getSnapshots()
            .stream()
            .map(SnapshotInfo::indices)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        assertThat("not-a-system-index", in(snapshottedIndices));
        assertThat(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, not(in(snapshottedIndices)));
    }

    /**
     * Test that we can snapshot feature states by name.
     */
    public void testSnapshotByFeature() {
        createRepository(REPO_NAME, "fs");
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        indexDoc(AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setIncludeGlobalState(true)
            .setWaitForCompletion(true)
            .setFeatureStates(SystemIndexTestPlugin.class.getSimpleName(), AnotherSystemIndexTestPlugin.class.getSimpleName())
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "2", "purpose", "post-snapshot doc");
        indexDoc(AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME, "2", "purpose", "post-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        assertThat(getDocCount(SystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(2L));
        assertThat(getDocCount(AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(2L));

        RestoreSnapshotResponse restoreSnapshotResponse = clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setRestoreGlobalState(true)
            .get();
        assertThat(restoreSnapshotResponse.getRestoreInfo().totalShards(), greaterThan(0));

        assertThat(getDocCount(SystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(1L));
        assertThat(getDocCount(SystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(1L));
    }

    /**
     * Take a snapshot with global state but don't restore system indexes. By
     * default, snapshot restorations ignore global state and don't include system indices.
     *
     * This means that we should be able to take a snapshot with a system index in it and restore it without specifying indices, even if
     * the cluster already has a system index with the same name (because the system index from the snapshot won't be restored).
     */
    public void testDefaultRestoreOnlyRegularIndices() {
        createRepository(REPO_NAME, "fs");
        final String regularIndex = "test-idx";

        indexDoc(regularIndex, "1", "purpose", "create an index that can be restored");
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        refresh(regularIndex, SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(true)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        assertAcked(cluster().client().admin().indices().prepareDelete(regularIndex));

        RestoreSnapshotResponse restoreResponse = clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .get();
        assertThat(restoreResponse.getRestoreInfo().totalShards(), greaterThan(0));
        assertThat(
            restoreResponse.getRestoreInfo().indices(),
            allOf(hasItem(regularIndex), not(hasItem(SystemIndexTestPlugin.SYSTEM_INDEX_NAME)))
        );
    }

    /**
     * Take a snapshot with global state but restore features by feature state.
     */
    public void testRestoreByFeature() {
        createRepository(REPO_NAME, "fs");
        final String regularIndex = "test-idx";

        indexDoc(regularIndex, "1", "purpose", "create an index that can be restored");
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        indexDoc(AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        refresh(regularIndex, SystemIndexTestPlugin.SYSTEM_INDEX_NAME, AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(true)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "2", "purpose", "post-snapshot doc");
        indexDoc(AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME, "2", "purpose", "post-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        assertThat(getDocCount(SystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(2L));
        assertThat(getDocCount(AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(2L));

        assertAcked(cluster().client().admin().indices().prepareDelete(regularIndex));

        RestoreSnapshotResponse restoreSnapshotResponse = clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setFeatureStates("SystemIndexTestPlugin")
            .get();
        assertThat(restoreSnapshotResponse.getRestoreInfo().totalShards(), greaterThan(0));

        assertThat(getDocCount(SystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(1L));

        assertThat(getDocCount(AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(2L));
    }

    /**
     * Test that if a feature state has associated indices, they are included in the snapshot
     * when that feature state is selected.
     */
    public void testSnapshotAndRestoreAssociatedIndices() {
        createRepository(REPO_NAME, "fs");
        final String regularIndex = "regular-idx";

        indexDoc(regularIndex, "1", "purpose", "pre-snapshot doc");
        indexDoc(AssociatedIndicesTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        indexDoc(AssociatedIndicesTestPlugin.ASSOCIATED_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        refresh(regularIndex, AssociatedIndicesTestPlugin.SYSTEM_INDEX_NAME, AssociatedIndicesTestPlugin.ASSOCIATED_INDEX_NAME);

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setFeatureStates(AssociatedIndicesTestPlugin.class.getSimpleName())
            .setWaitForCompletion(true)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        Set<String> snapshottedIndices = clusterAdmin().prepareGetSnapshots(REPO_NAME)
            .get()
            .getSnapshots()
            .stream()
            .map(SnapshotInfo::indices)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        assertThat(snapshottedIndices, hasItem(AssociatedIndicesTestPlugin.SYSTEM_INDEX_NAME));
        assertThat(snapshottedIndices, hasItem(AssociatedIndicesTestPlugin.ASSOCIATED_INDEX_NAME));

        indexDoc(regularIndex, "2", "purpose", "post-snapshot doc");
        indexDoc(AssociatedIndicesTestPlugin.SYSTEM_INDEX_NAME, "2", "purpose", "post-snapshot doc");
        refresh(regularIndex, AssociatedIndicesTestPlugin.SYSTEM_INDEX_NAME);

        assertThat(getDocCount(regularIndex), equalTo(2L));
        assertThat(getDocCount(AssociatedIndicesTestPlugin.SYSTEM_INDEX_NAME), equalTo(2L));

        assertAcked(indicesAdmin().prepareDelete(AssociatedIndicesTestPlugin.ASSOCIATED_INDEX_NAME).get());

        RestoreSnapshotResponse restoreSnapshotResponse = clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
            .setIndices(AssociatedIndicesTestPlugin.ASSOCIATED_INDEX_NAME)
            .setWaitForCompletion(true)
            .setFeatureStates(AssociatedIndicesTestPlugin.class.getSimpleName())
            .get();
        assertThat(restoreSnapshotResponse.getRestoreInfo().totalShards(), greaterThan(0));

        assertThat(getDocCount(AssociatedIndicesTestPlugin.SYSTEM_INDEX_NAME), equalTo(1L));
        assertThat(getDocCount(AssociatedIndicesTestPlugin.ASSOCIATED_INDEX_NAME), equalTo(1L));
    }

    /**
     * Check that if we request a feature not in the snapshot, we get an error.
     */
    public void testRestoreFeatureNotInSnapshot() {
        createRepository(REPO_NAME, "fs");
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(true)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        final String fakeFeatureStateName = "NonExistentTestPlugin";
        SnapshotRestoreException exception = expectThrows(
            SnapshotRestoreException.class,
            clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
                .setWaitForCompletion(true)
                .setFeatureStates("SystemIndexTestPlugin", fakeFeatureStateName)
        );

        assertThat(
            exception.getMessage(),
            containsString("requested feature states [[" + fakeFeatureStateName + "]] are not present in snapshot")
        );
    }

    public void testSnapshottingSystemIndexByNameIsRejected() throws Exception {
        createRepository(REPO_NAME, "fs");
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        IllegalArgumentException error = expectThrows(
            IllegalArgumentException.class,
            clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
                .setIndices(SystemIndexTestPlugin.SYSTEM_INDEX_NAME)
                .setWaitForCompletion(true)
                .setIncludeGlobalState(randomBoolean())
        );
        assertThat(
            error.getMessage(),
            equalTo(
                "the [indices] parameter includes system indices [.test-system-idx]; to include or exclude system indices from a snapshot, "
                    + "use the [include_global_state] or [feature_states] parameters"
            )
        );

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(true)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);
    }

    /**
     * Check that directly requesting a system index in a restore request throws an Exception.
     */
    public void testRestoringSystemIndexByNameIsRejected() throws IllegalAccessException {
        createRepository(REPO_NAME, "fs");
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(true)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "2", "purpose", "post-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        IllegalArgumentException ex = expectThrows(
            IllegalArgumentException.class,
            clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
                .setWaitForCompletion(true)
                .setIndices(SystemIndexTestPlugin.SYSTEM_INDEX_NAME)
        );
        assertThat(
            ex.getMessage(),
            equalTo("requested system indices [.test-system-idx], but system indices can only be restored as part of a feature state")
        );

        assertThat(getDocCount(SystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(2L));
    }

    /**
     * Check that if a system index matches a rename pattern in a restore request, it's not renamed
     */
    public void testSystemIndicesCannotBeRenamed() {
        createRepository(REPO_NAME, "fs");
        final String nonSystemIndex = ".test-non-system-index";
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        indexDoc(nonSystemIndex, "1", "purpose", "pre-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(true)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        assertAcked(indicesAdmin().prepareDelete(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, nonSystemIndex).get());

        clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setRestoreGlobalState(true)
            .setRenamePattern(".test-(.+)")
            .setRenameReplacement(".test-restored-$1")
            .get();

        assertTrue("System index not renamed", indexExists(SystemIndexTestPlugin.SYSTEM_INDEX_NAME));
        assertTrue("Non-system index was renamed", indexExists(".test-restored-non-system-index"));

        assertFalse("Renamed system index doesn't exist", indexExists(".test-restored-system-index"));
        assertFalse("Original non-system index doesn't exist", indexExists(nonSystemIndex));
    }

    /**
     * If the list of feature states to restore is left unspecified and we are restoring global state,
     * all feature states should be restored.
     */
    public void testRestoreSystemIndicesAsGlobalStateWithDefaultFeatureStateList() {
        createRepository(REPO_NAME, "fs");
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(true)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "2", "purpose", "post-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        assertThat(getDocCount(SystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(2L));

        RestoreSnapshotResponse restoreSnapshotResponse = clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setRestoreGlobalState(true)
            .get();
        assertThat(restoreSnapshotResponse.getRestoreInfo().totalShards(), greaterThan(0));

        assertThat(getDocCount(SystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(1L));
    }

    /**
     * If the list of feature states to restore contains only "none" and we are restoring global state,
     * no feature states should be restored.
     */
    public void testRestoreSystemIndicesAsGlobalStateWithNoFeatureStates() {
        createRepository(REPO_NAME, "fs");
        String regularIndex = "my-index";
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        indexDoc(regularIndex, "1", "purpose", "pre-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, regularIndex);

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(true)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "2", "purpose", "post-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        assertAcked(indicesAdmin().prepareDelete(regularIndex).get());
        assertThat(getDocCount(SystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(2L));

        RestoreSnapshotResponse restoreSnapshotResponse = clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setRestoreGlobalState(true)
            .setFeatureStates(new String[] { randomFrom("none", "NONE") })
            .get();
        assertThat(restoreSnapshotResponse.getRestoreInfo().totalShards(), greaterThan(0));

        assertThat(getDocCount(SystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(2L));
        assertThat(getDocCount(regularIndex), equalTo(1L));
    }

    /**
     * When a feature state is restored, all indices that are part of that feature state should be deleted, then the indices in
     * the snapshot should be restored.
     *
     * However, other feature states should be unaffected.
     */
    public void testAllSystemIndicesAreRemovedWhenThatFeatureStateIsRestored() {
        createRepository(REPO_NAME, "fs");
        final String systemIndexInSnapshot = SystemIndexTestPlugin.SYSTEM_INDEX_NAME + "-1";
        indexDoc(systemIndexInSnapshot, "1", "purpose", "pre-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME + "*");

        indexDoc(AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");

        final String regularIndex = "regular-index";
        indexDoc(regularIndex, "1", "purpose", "pre-snapshot doc");

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(true)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        final String systemIndexNotInSnapshot = SystemIndexTestPlugin.SYSTEM_INDEX_NAME + "-2";
        indexDoc(systemIndexInSnapshot, "2", "purpose", "post-snapshot doc");
        indexDoc(systemIndexNotInSnapshot, "1", "purpose", "post-snapshot doc");

        indexDoc(AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME, "2", "purpose", "post-snapshot doc");
        refresh(systemIndexInSnapshot, systemIndexNotInSnapshot, AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        assertAcked(cluster().client().admin().indices().prepareDelete(regularIndex));

        RestoreSnapshotResponse restoreSnapshotResponse = clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
            .setFeatureStates("SystemIndexTestPlugin")
            .setWaitForCompletion(true)
            .setRestoreGlobalState(true)
            .get();
        assertThat(restoreSnapshotResponse.getRestoreInfo().totalShards(), greaterThan(0));

        assertFalse(indexExists(systemIndexNotInSnapshot));
        assertThat(getDocCount(systemIndexInSnapshot), equalTo(1L));
        assertThat(getDocCount(AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME), equalTo(2L));
    }

    public void testSystemIndexAliasesAreAlwaysRestored() {
        createRepository(REPO_NAME, "fs");
        final String systemIndexName = SystemIndexTestPlugin.SYSTEM_INDEX_NAME + "-1";
        indexDoc(systemIndexName, "1", "purpose", "pre-snapshot doc");

        final String regularIndex = "regular-index";
        final String regularAlias = "regular-alias";
        indexDoc(regularIndex, "1", "purpose", "pre-snapshot doc");

        final String systemIndexAlias = SystemIndexTestPlugin.SYSTEM_INDEX_NAME + "-alias";
        assertAcked(indicesAdmin().prepareAliases().addAlias(systemIndexName, systemIndexAlias).addAlias(regularIndex, regularAlias).get());

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(true)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        assertAcked(cluster().client().admin().indices().prepareDelete(regularIndex, systemIndexName));

        RestoreSnapshotResponse restoreSnapshotResponse = clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
            .setFeatureStates("SystemIndexTestPlugin")
            .setWaitForCompletion(true)
            .setRestoreGlobalState(false)
            .setIncludeAliases(false)
            .get();
        assertThat(restoreSnapshotResponse.getRestoreInfo().totalShards(), greaterThan(0));

        assertTrue(indexExists(regularIndex));
        assertFalse(indexExists(regularAlias));
        assertTrue(indexExists(systemIndexName));
        assertTrue(indexExists(systemIndexAlias));
        assertThat(getDocCount(systemIndexAlias), equalTo(1L));

    }

    /**
     * Tests that the special "none" feature state name cannot be combined with other
     * feature state names, and an error occurs if it's tried.
     */
    public void testNoneFeatureStateMustBeAlone() {
        createRepository(REPO_NAME, "fs");
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        refresh(SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        IllegalArgumentException createEx = expectThrows(
            IllegalArgumentException.class,
            clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
                .setWaitForCompletion(true)
                .setIncludeGlobalState(randomBoolean())
                .setFeatureStates("SystemIndexTestPlugin", "none", "AnotherSystemIndexTestPlugin")
        );
        assertThat(
            createEx.getMessage(),
            equalTo(
                "the feature_states value [none] indicates that no feature states should be "
                    + "snapshotted, but other feature states were requested: [SystemIndexTestPlugin, none, AnotherSystemIndexTestPlugin]"
            )
        );

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(true)
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        SnapshotRestoreException restoreEx = expectThrows(
            SnapshotRestoreException.class,
            clusterAdmin().prepareRestoreSnapshot(REPO_NAME, "test-snap")
                .setWaitForCompletion(true)
                .setRestoreGlobalState(randomBoolean())
                .setFeatureStates("SystemIndexTestPlugin", "none")
        );
        assertThat(
            restoreEx.getMessage(),
            allOf(
                containsString(
                    "the feature_states value [none] indicates that no feature states should be restored, but other feature states were "
                        + "requested:"
                ),
                containsString("SystemIndexTestPlugin")
            )
        );
    }

    /**
     * Tests that using the special "none" feature state value creates a snapshot with no feature states included
     */
    public void testNoneFeatureStateOnCreation() {
        createRepository(REPO_NAME, "fs");
        final String regularIndex = "test-idx";

        indexDoc(regularIndex, "1", "purpose", "create an index that can be restored");
        indexDoc(SystemIndexTestPlugin.SYSTEM_INDEX_NAME, "1", "purpose", "pre-snapshot doc");
        refresh(regularIndex, SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, "test-snap")
            .setWaitForCompletion(true)
            .setIncludeGlobalState(true)
            .setFeatureStates(randomFrom("none", "NONE"))
            .get();
        assertSnapshotSuccess(createSnapshotResponse);

        Set<String> snapshottedIndices = clusterAdmin().prepareGetSnapshots(REPO_NAME)
            .get()
            .getSnapshots()
            .stream()
            .map(SnapshotInfo::indices)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        assertThat(snapshottedIndices, allOf(hasItem(regularIndex), not(hasItem(SystemIndexTestPlugin.SYSTEM_INDEX_NAME))));
    }

    /**
     * Ensures that if we can only capture a partial snapshot of a system index, then the feature state associated with that index is
     * not included in the snapshot, because it would not be safe to restore that feature state.
     */
    public void testPartialSnapshotsOfSystemIndexRemovesFeatureState() throws Exception {
        final String partialIndexName = SystemIndexTestPlugin.SYSTEM_INDEX_NAME;
        final String fullIndexName = AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME;

        createRepositoryNoVerify(REPO_NAME, "mock");

        assertAcked(prepareCreate(partialIndexName, 0, indexSettingsNoReplicas(6)));
        indexDoc(partialIndexName, "1", "purpose", "pre-snapshot doc");
        indexDoc(fullIndexName, "1", "purpose", "pre-snapshot doc");
        ensureGreen();

        internalCluster().stopRandomDataNode();
        assertBusy(() -> assertEquals(ClusterHealthStatus.RED, clusterAdmin().prepareHealth().get().getStatus()), 30, TimeUnit.SECONDS);

        blockMasterFromFinalizingSnapshotOnIndexFile(REPO_NAME);

        final String partialSnapName = "test-partial-snap";
        CreateSnapshotResponse createSnapshotResponse = clusterAdmin().prepareCreateSnapshot(REPO_NAME, partialSnapName)
            .setIncludeGlobalState(true)
            .setWaitForCompletion(false)
            .setPartial(true)
            .get();
        assertThat(createSnapshotResponse.status(), equalTo(RestStatus.ACCEPTED));
        waitForBlock(internalCluster().getMasterName(), REPO_NAME);
        internalCluster().stopCurrentMasterNode();

        assertBusy(() -> {
            GetSnapshotsResponse snapshotsStatusResponse = clusterAdmin().prepareGetSnapshots(REPO_NAME)
                .setSnapshots(partialSnapName)
                .get();
            SnapshotInfo snapshotInfo = snapshotsStatusResponse.getSnapshots().get(0);
            assertNotNull(snapshotInfo);
            assertThat(snapshotInfo.failedShards(), lessThan(snapshotInfo.totalShards()));
            List<String> statesInSnapshot = snapshotInfo.featureStates().stream().map(SnapshotFeatureInfo::getPluginName).toList();
            assertThat(statesInSnapshot, not(hasItem((new SystemIndexTestPlugin()).getFeatureName())));
            assertThat(statesInSnapshot, hasItem((new AnotherSystemIndexTestPlugin()).getFeatureName()));
        });
    }

    public void testParallelIndexDeleteRemovesFeatureState() throws Exception {
        final String indexToBeDeleted = SystemIndexTestPlugin.SYSTEM_INDEX_NAME;
        final String fullIndexName = AnotherSystemIndexTestPlugin.SYSTEM_INDEX_NAME;
        final String nonsystemIndex = "nonsystem-idx";

        final int nodesInCluster = internalCluster().size();
        internalCluster().stopNode(dataNodes.get(1));
        dataNodes.remove(1);
        ensureStableCluster(nodesInCluster - 1);

        createRepositoryNoVerify(REPO_NAME, "mock");

        assertAcked(prepareCreate(indexToBeDeleted, 0, indexSettingsNoReplicas(6)));
        indexDoc(indexToBeDeleted, "1", "purpose", "pre-snapshot doc");
        indexDoc(fullIndexName, "1", "purpose", "pre-snapshot doc");

        dataNodes.add(internalCluster().startDataOnlyNode());
        createIndexWithContent(
            nonsystemIndex,
            indexSettingsNoReplicas(2).put("index.routing.allocation.require._name", dataNodes.get(1)).build()
        );
        refresh();
        ensureGreen();

        logger.info("--> Created indices, blocking repo on new data node...");
        blockDataNode(REPO_NAME, dataNodes.get(1));

        logger.info("--> Blocked repo, starting snapshot...");
        final String partialSnapName = "test-partial-snap";
        ActionFuture<CreateSnapshotResponse> createSnapshotFuture = clusterAdmin().prepareCreateSnapshot(REPO_NAME, partialSnapName)
            .setIncludeGlobalState(true)
            .setWaitForCompletion(true)
            .setPartial(true)
            .execute();

        logger.info("--> Started snapshot, waiting for block...");
        waitForBlock(dataNodes.get(1), REPO_NAME);

        logger.info("--> Repo hit block, deleting the index...");
        assertAcked(cluster().client().admin().indices().prepareDelete(indexToBeDeleted));

        logger.info("--> Index deleted, unblocking repo...");
        unblockNode(REPO_NAME, dataNodes.get(1));

        logger.info("--> Repo unblocked, checking that snapshot finished...");
        CreateSnapshotResponse createSnapshotResponse = createSnapshotFuture.get();
        logger.info(createSnapshotResponse.toString());
        assertThat(createSnapshotResponse.status(), equalTo(RestStatus.OK));

        logger.info("--> All operations complete, running assertions");
        SnapshotInfo snapshotInfo = createSnapshotResponse.getSnapshotInfo();
        assertNotNull(snapshotInfo);
        assertThat(snapshotInfo.indices(), not(hasItem(indexToBeDeleted)));
        List<String> statesInSnapshot = snapshotInfo.featureStates().stream().map(SnapshotFeatureInfo::getPluginName).toList();
        assertThat(statesInSnapshot, not(hasItem((new SystemIndexTestPlugin()).getFeatureName())));
        assertThat(statesInSnapshot, hasItem((new AnotherSystemIndexTestPlugin()).getFeatureName()));
    }

    private void assertSnapshotSuccess(CreateSnapshotResponse createSnapshotResponse) {
        assertThat(createSnapshotResponse.getSnapshotInfo().successfulShards(), greaterThan(0));
        assertThat(
            createSnapshotResponse.getSnapshotInfo().successfulShards(),
            equalTo(createSnapshotResponse.getSnapshotInfo().totalShards())
        );
    }

    private long getDocCount(String indexName) {
        return indicesAdmin().prepareStats(indexName).get().getPrimaries().getDocs().getCount();
    }

    public static class SystemIndexTestPlugin extends Plugin implements SystemIndexPlugin {

        public static final String SYSTEM_INDEX_NAME = ".test-system-idx";

        @Override
        public Collection<SystemIndexDescriptor> getSystemIndexDescriptors(Settings settings) {
            return Collections.singletonList(
                SystemIndexDescriptorUtils.createUnmanaged(SYSTEM_INDEX_NAME + "*", "System indices for tests")
            );
        }

        @Override
        public String getFeatureName() {
            return SystemIndexTestPlugin.class.getSimpleName();
        }

        @Override
        public String getFeatureDescription() {
            return "A simple test plugin";
        }
    }

    public static class AnotherSystemIndexTestPlugin extends Plugin implements SystemIndexPlugin {

        public static final String SYSTEM_INDEX_NAME = ".another-test-system-idx";

        @Override
        public Collection<SystemIndexDescriptor> getSystemIndexDescriptors(Settings settings) {
            return Collections.singletonList(
                SystemIndexDescriptorUtils.createUnmanaged(SYSTEM_INDEX_NAME + "*", "System indices for tests")
            );
        }

        @Override
        public String getFeatureName() {
            return AnotherSystemIndexTestPlugin.class.getSimpleName();
        }

        @Override
        public String getFeatureDescription() {
            return "Another simple test plugin";
        }
    }

    public static class AssociatedIndicesTestPlugin extends Plugin implements SystemIndexPlugin {

        public static final String SYSTEM_INDEX_NAME = ".third-test-system-idx";
        public static final String ASSOCIATED_INDEX_NAME = ".associated-idx";

        @Override
        public Collection<SystemIndexDescriptor> getSystemIndexDescriptors(Settings settings) {
            return Collections.singletonList(
                SystemIndexDescriptorUtils.createUnmanaged(SYSTEM_INDEX_NAME + "*", "System & associated indices for tests")
            );
        }

        @Override
        public Collection<AssociatedIndexDescriptor> getAssociatedIndexDescriptors() {
            return Collections.singletonList(new AssociatedIndexDescriptor(ASSOCIATED_INDEX_NAME, "Associated indices"));
        }

        @Override
        public String getFeatureName() {
            return AssociatedIndicesTestPlugin.class.getSimpleName();
        }

        @Override
        public String getFeatureDescription() {
            return "Another simple test plugin";
        }
    }
}
