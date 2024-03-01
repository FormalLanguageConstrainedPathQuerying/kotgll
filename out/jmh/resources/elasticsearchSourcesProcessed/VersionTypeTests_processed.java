/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index;

import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.test.ESTestCase;

import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class VersionTypeTests extends ESTestCase {
    public void testInternalVersionConflict() throws Exception {
        assertFalse(VersionType.INTERNAL.isVersionConflictForWrites(10, Versions.MATCH_ANY, randomBoolean()));
        assertFalse(VersionType.INTERNAL.isVersionConflictForReads(10, Versions.MATCH_ANY));

        assertTrue(VersionType.INTERNAL.isVersionConflictForWrites(Versions.NOT_FOUND, 10, randomBoolean()));
        assertTrue(VersionType.INTERNAL.isVersionConflictForReads(Versions.NOT_FOUND, 10));
        assertFalse(VersionType.INTERNAL.isVersionConflictForWrites(Versions.NOT_FOUND, Versions.MATCH_ANY, randomBoolean()));
        assertFalse(VersionType.INTERNAL.isVersionConflictForReads(Versions.NOT_FOUND, Versions.MATCH_ANY));

        assertEquals("current version [1] is different than the one provided [2]", VersionType.INTERNAL.explainConflictForReads(1, 2));
        assertEquals("document does not exist (expected version [2])", VersionType.INTERNAL.explainConflictForReads(Versions.NOT_FOUND, 2));

        assertFalse(VersionType.INTERNAL.isVersionConflictForWrites(Versions.NOT_FOUND, Versions.MATCH_DELETED, true));
        assertFalse(VersionType.INTERNAL.isVersionConflictForWrites(10, Versions.MATCH_DELETED, true));

        assertFalse(VersionType.INTERNAL.isVersionConflictForWrites(10, 10, randomBoolean()));
        assertFalse(VersionType.INTERNAL.isVersionConflictForReads(10, 10));
        assertTrue(VersionType.INTERNAL.isVersionConflictForWrites(9, 10, randomBoolean()));
        assertTrue(VersionType.INTERNAL.isVersionConflictForReads(9, 10));
        assertTrue(VersionType.INTERNAL.isVersionConflictForWrites(10, 9, randomBoolean()));
        assertTrue(VersionType.INTERNAL.isVersionConflictForReads(10, 9));

    }

    public void testVersionValidation() {
        assertTrue(VersionType.EXTERNAL.validateVersionForWrites(randomIntBetween(1, Integer.MAX_VALUE)));
        assertFalse(VersionType.EXTERNAL.validateVersionForWrites(Versions.MATCH_ANY));
        assertFalse(VersionType.EXTERNAL.validateVersionForWrites(randomIntBetween(Integer.MIN_VALUE, 0)));
        assertTrue(VersionType.EXTERNAL.validateVersionForReads(Versions.MATCH_ANY));
        assertTrue(VersionType.EXTERNAL.validateVersionForReads(randomIntBetween(1, Integer.MAX_VALUE)));
        assertFalse(VersionType.EXTERNAL.validateVersionForReads(randomIntBetween(Integer.MIN_VALUE, -1)));

        assertEquals("current version [1] is different than the one provided [2]", VersionType.EXTERNAL.explainConflictForReads(1, 2));
        assertEquals("document does not exist (expected version [2])", VersionType.EXTERNAL.explainConflictForReads(Versions.NOT_FOUND, 2));

        assertTrue(VersionType.EXTERNAL_GTE.validateVersionForWrites(randomIntBetween(1, Integer.MAX_VALUE)));
        assertFalse(VersionType.EXTERNAL_GTE.validateVersionForWrites(Versions.MATCH_ANY));
        assertFalse(VersionType.EXTERNAL_GTE.validateVersionForWrites(randomIntBetween(Integer.MIN_VALUE, 0)));
        assertTrue(VersionType.EXTERNAL_GTE.validateVersionForReads(Versions.MATCH_ANY));
        assertTrue(VersionType.EXTERNAL_GTE.validateVersionForReads(randomIntBetween(1, Integer.MAX_VALUE)));
        assertFalse(VersionType.EXTERNAL_GTE.validateVersionForReads(randomIntBetween(Integer.MIN_VALUE, -1)));

        assertEquals("current version [1] is different than the one provided [2]", VersionType.EXTERNAL_GTE.explainConflictForReads(1, 2));
        assertEquals(
            "document does not exist (expected version [2])",
            VersionType.EXTERNAL_GTE.explainConflictForReads(Versions.NOT_FOUND, 2)
        );

        assertTrue(VersionType.INTERNAL.validateVersionForWrites(randomIntBetween(1, Integer.MAX_VALUE)));
        assertTrue(VersionType.INTERNAL.validateVersionForWrites(Versions.MATCH_ANY));
        assertFalse(VersionType.INTERNAL.validateVersionForWrites(randomIntBetween(Integer.MIN_VALUE, 0)));
        assertTrue(VersionType.INTERNAL.validateVersionForReads(Versions.MATCH_ANY));
        assertTrue(VersionType.INTERNAL.validateVersionForReads(randomIntBetween(1, Integer.MAX_VALUE)));
        assertFalse(VersionType.INTERNAL.validateVersionForReads(randomIntBetween(Integer.MIN_VALUE, -1)));
    }

    public void testExternalVersionConflict() throws Exception {
        assertFalse(VersionType.EXTERNAL.isVersionConflictForWrites(Versions.NOT_FOUND, 10, randomBoolean()));
        assertTrue(VersionType.EXTERNAL.isVersionConflictForWrites(10, Versions.MATCH_ANY, randomBoolean()));

        assertFalse(VersionType.EXTERNAL.isVersionConflictForWrites(Versions.NOT_FOUND, Versions.NOT_FOUND, randomBoolean()));
        assertFalse(VersionType.EXTERNAL.isVersionConflictForWrites(Versions.NOT_FOUND, 10, randomBoolean()));

        assertTrue(VersionType.EXTERNAL.isVersionConflictForReads(Versions.NOT_FOUND, Versions.NOT_FOUND));
        assertTrue(VersionType.EXTERNAL.isVersionConflictForReads(Versions.NOT_FOUND, 10));
        assertFalse(VersionType.EXTERNAL.isVersionConflictForReads(Versions.NOT_FOUND, Versions.MATCH_ANY));

        assertTrue(VersionType.EXTERNAL.isVersionConflictForWrites(10, 10, randomBoolean()));
        assertFalse(VersionType.EXTERNAL.isVersionConflictForWrites(9, 10, randomBoolean()));
        assertTrue(VersionType.EXTERNAL.isVersionConflictForWrites(10, 9, randomBoolean()));

        assertFalse(VersionType.EXTERNAL.isVersionConflictForReads(10, 10));
        assertTrue(VersionType.EXTERNAL.isVersionConflictForReads(9, 10));
        assertTrue(VersionType.EXTERNAL.isVersionConflictForReads(10, 9));
        assertFalse(VersionType.EXTERNAL.isVersionConflictForReads(10, Versions.MATCH_ANY));

    }

    public void testExternalGTEVersionConflict() throws Exception {
        assertFalse(VersionType.EXTERNAL_GTE.isVersionConflictForWrites(Versions.NOT_FOUND, 10, randomBoolean()));
        assertTrue(VersionType.EXTERNAL_GTE.isVersionConflictForWrites(10, Versions.MATCH_ANY, randomBoolean()));

        assertFalse(VersionType.EXTERNAL_GTE.isVersionConflictForWrites(Versions.NOT_FOUND, Versions.NOT_FOUND, randomBoolean()));
        assertFalse(VersionType.EXTERNAL_GTE.isVersionConflictForWrites(Versions.NOT_FOUND, 10, randomBoolean()));

        assertTrue(VersionType.EXTERNAL_GTE.isVersionConflictForReads(Versions.NOT_FOUND, Versions.NOT_FOUND));
        assertTrue(VersionType.EXTERNAL_GTE.isVersionConflictForReads(Versions.NOT_FOUND, 10));
        assertFalse(VersionType.EXTERNAL_GTE.isVersionConflictForReads(Versions.NOT_FOUND, Versions.MATCH_ANY));

        assertFalse(VersionType.EXTERNAL_GTE.isVersionConflictForWrites(10, 10, randomBoolean()));
        assertFalse(VersionType.EXTERNAL_GTE.isVersionConflictForWrites(9, 10, randomBoolean()));
        assertTrue(VersionType.EXTERNAL_GTE.isVersionConflictForWrites(10, 9, randomBoolean()));

        assertFalse(VersionType.EXTERNAL_GTE.isVersionConflictForReads(10, 10));
        assertTrue(VersionType.EXTERNAL_GTE.isVersionConflictForReads(9, 10));
        assertTrue(VersionType.EXTERNAL_GTE.isVersionConflictForReads(10, 9));
        assertFalse(VersionType.EXTERNAL_GTE.isVersionConflictForReads(10, Versions.MATCH_ANY));

    }

    public void testUpdateVersion() {
        assertThat(VersionType.INTERNAL.updateVersion(Versions.NOT_FOUND, 10), equalTo(1L));
        assertThat(VersionType.INTERNAL.updateVersion(1, 1), equalTo(2L));
        assertThat(VersionType.INTERNAL.updateVersion(2, Versions.MATCH_ANY), equalTo(3L));

        assertThat(VersionType.EXTERNAL.updateVersion(Versions.NOT_FOUND, 10), equalTo(10L));
        assertThat(VersionType.EXTERNAL.updateVersion(1, 10), equalTo(10L));

        assertThat(VersionType.EXTERNAL_GTE.updateVersion(Versions.NOT_FOUND, 10), equalTo(10L));
        assertThat(VersionType.EXTERNAL_GTE.updateVersion(1, 10), equalTo(10L));
        assertThat(VersionType.EXTERNAL_GTE.updateVersion(10, 10), equalTo(10L));

    }

    public void testStaticFromString() {
        assertThat(VersionType.fromString("internal"), equalTo(VersionType.INTERNAL));
        assertThat(VersionType.fromString("external"), equalTo(VersionType.EXTERNAL));
        assertThat(VersionType.fromString("external_gt"), equalTo(VersionType.EXTERNAL));
        assertThat(VersionType.fromString("external_gte"), equalTo(VersionType.EXTERNAL_GTE));

        IllegalArgumentException exception;
        exception = expectThrows(IllegalArgumentException.class, () -> VersionType.fromString("other"));
        assertThat(exception.toString(), containsString("No version type match [other]"));

        exception = expectThrows(IllegalArgumentException.class, () -> VersionType.fromString(null));
        assertThat(exception.toString(), containsString("No version type match [null]"));
    }

    public void testStaticToString() {
        assertThat(VersionType.toString(VersionType.INTERNAL), equalTo("internal"));
        assertThat(VersionType.toString(VersionType.EXTERNAL), equalTo("external"));
        assertThat(VersionType.toString(VersionType.EXTERNAL_GTE), equalTo("external_gte"));

        NullPointerException npe = expectThrows(NullPointerException.class, () -> VersionType.toString(null));
        assertThat(
            npe.toString(),
            containsString("Cannot invoke \"org.elasticsearch.index.VersionType.ordinal()\" because \"versionType\" is null")
        );

        for (VersionType type : VersionType.values()) {
            assertThat(VersionType.toString(type), equalTo(type.name().toLowerCase(Locale.ROOT)));
        }
    }
}
