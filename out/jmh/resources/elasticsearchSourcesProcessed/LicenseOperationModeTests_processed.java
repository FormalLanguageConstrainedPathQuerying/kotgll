/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.license;

import org.elasticsearch.core.Strings;
import org.elasticsearch.test.ESTestCase;

import static org.elasticsearch.license.License.OperationMode;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests {@link License.OperationMode} for correctness.
 * <p>
 * If you change the behavior of these tests, then it means that licensing changes across the products!
 */
public class LicenseOperationModeTests extends ESTestCase {
    public void testResolveTrial() {
        assertResolve(OperationMode.TRIAL, "nONE", "DEv", "deveLopment");
        assertResolve(OperationMode.TRIAL, "tRiAl", "trial");
    }

    public void testResolveBasic() {
        assertResolve(OperationMode.BASIC, "bAsIc", "basic");
    }

    public void testResolveStandard() {
        assertResolve(OperationMode.STANDARD, "StAnDARd", "standard");
    }

    public void testResolveGold() {
        assertResolve(OperationMode.GOLD, "SiLvEr", "gOlD", "silver", "gold");
    }

    public void testResolvePlatinum() {
        assertResolve(OperationMode.PLATINUM, "iNtErNaL");
        assertResolve(OperationMode.PLATINUM, "PlAtINum", "platinum");
    }

    public void testResolveEnterprise() {
        assertResolve(OperationMode.ENTERPRISE, License.LicenseType.ENTERPRISE.getTypeName());
        assertResolve(OperationMode.ENTERPRISE, License.LicenseType.ENTERPRISE.name());
    }

    public void testResolveUnknown() {
        String[] types = { "unknown", "fake", "commercial" };

        for (String type : types) {
            try {
                final License.LicenseType licenseType = License.LicenseType.resolve(type);
                OperationMode.resolve(licenseType);

                fail(Strings.format("[%s] should not be recognized as an operation mode", type));
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage(), equalTo("unknown license type [" + type + "]"));
            }
        }
    }

    private static void assertResolve(OperationMode expected, String... types) {
        for (String type : types) {
            License.LicenseType licenseType = License.LicenseType.resolve(type);
            assertThat(OperationMode.resolve(licenseType), equalTo(expected));
        }
    }
}
