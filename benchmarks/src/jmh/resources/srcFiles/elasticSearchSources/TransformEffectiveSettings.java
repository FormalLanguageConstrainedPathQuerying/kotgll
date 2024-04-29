/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.core.transform.transforms;

import org.elasticsearch.xpack.core.transform.TransformConfigVersion;

public final class TransformEffectiveSettings {

    private TransformEffectiveSettings() {}

    /**
     * Determines if the transform should write dates as epoch millis based on settings and version.
     *
     * @param settings transform's settings
     * @return whether or not the transform is unattended
     */
    public static boolean writeDatesAsEpochMillis(SettingsConfig settings, TransformConfigVersion version) {
        return settings.getDatesAsEpochMillis() != null
            ? settings.getDatesAsEpochMillis()
            : version.before(TransformConfigVersion.V_7_11_0);
    }

    /**
     * Determines if aligning checkpoints is disabled for this transform based on settings.
     *
     * @param settings transform's settings
     * @return whether or not aligning checkpoints is disabled for this transform
     */
    public static boolean isAlignCheckpointsDisabled(SettingsConfig settings) {
        return Boolean.FALSE.equals(settings.getAlignCheckpoints());
    }

    /**
     * Determines if pit is disabled for this transform based on settings.
     *
     * @param settings transform's settings
     * @return whether or not pit is disabled for this transform
     */
    public static boolean isPitDisabled(SettingsConfig settings) {
        return Boolean.FALSE.equals(settings.getUsePit());
    }

    /**
     * Determines if mappings deduction is disabled for this transform based on settings.
     *
     * @param settings transform's settings
     * @return whether or not mappings deduction is disabled for this transform
     */
    public static boolean isDeduceMappingsDisabled(SettingsConfig settings) {
        return Boolean.FALSE.equals(settings.getDeduceMappings());
    }

    /**
     * Determines the appropriate number of retries.
     * <p>
     * The number of retries are read from the config or if not read from the context which is based on a cluster wide default.
     * If the transform runs in unattended mode, the number of retries is always indefinite.
     *
     * @param settings transform's settings
     * @return the number of retries or -1 if retries are indefinite
     */
    public static int getNumFailureRetries(SettingsConfig settings, int defaultNumFailureRetries) {
        return isUnattended(settings) ? -1
            : settings.getNumFailureRetries() != null ? settings.getNumFailureRetries()
            : defaultNumFailureRetries;
    }

    /**
     * Determines if the transform is unattended based on settings.
     *
     * @param settings transform's settings
     * @return whether or not the transform is unattended
     */
    public static boolean isUnattended(SettingsConfig settings) {
        return Boolean.TRUE.equals(settings.getUnattended());
    }
}
