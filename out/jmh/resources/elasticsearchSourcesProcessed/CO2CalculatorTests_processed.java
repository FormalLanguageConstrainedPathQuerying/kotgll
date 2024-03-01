/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.profiling;

import org.elasticsearch.test.ESTestCase;

import java.util.Map;

public class CO2CalculatorTests extends ESTestCase {
    private static final String HOST_ID_A = "1110256254710195391";
    private static final String HOST_ID_B = "2220256254710195392";
    private static final String HOST_ID_C = "3330256254710195393";
    private static final String HOST_ID_D = "4440256254710195394";

    public void testCreateFromRegularSource() {
        Map<String, HostMetadata> hostsTable = Map.ofEntries(
            Map.entry(HOST_ID_A,
                new HostMetadata(HOST_ID_A,
                    new InstanceType(
                        "aws",
                        "eu-west-1",
                        "c5n.xlarge"
                    ),
                    "", 
                    null
                )
            ),
            Map.entry(HOST_ID_B,
                new HostMetadata(HOST_ID_B,
                    new InstanceType(
                        "gcp",
                        "europe-west1",
                        null 
                    ),
                    "x86_64",
                    null
                )
            ),
            Map.entry(HOST_ID_C,
                new HostMetadata(HOST_ID_C,
                    new InstanceType(
                        "azure",
                        "northcentralus",
                        null 
                    ),
                    "aarch64",
                    null
                )
            ),
            Map.entry(HOST_ID_D,
                new HostMetadata(HOST_ID_D,
                    new InstanceType(
                        "on-prem-provider",
                        "on-prem-region",
                        null 
                    ),
                    "aarch64",
                    null
                )
            )
        );

        double samplingDurationInSeconds = 1_800.0d; 
        long samples = 100_000L; 
        double annualCoreHours = CostCalculator.annualCoreHours(samplingDurationInSeconds, samples, 20.0d);
        CO2Calculator co2Calculator = new CO2Calculator(hostsTable, samplingDurationInSeconds, null, null, null, null);

        checkCO2Calculation(co2Calculator.getAnnualCO2Tons(HOST_ID_A, samples), annualCoreHours, 1.135d, 0.0002786d, 7.0d);
        checkCO2Calculation(co2Calculator.getAnnualCO2Tons(HOST_ID_B, samples), annualCoreHours, 1.1d, 0.0000198d, 7.0d);
        checkCO2Calculation(co2Calculator.getAnnualCO2Tons(HOST_ID_C, samples), annualCoreHours, 1.185d, 0.000410608d, 2.8d);
        checkCO2Calculation(co2Calculator.getAnnualCO2Tons(HOST_ID_D, samples), annualCoreHours, 1.7d, 0.000379069d, 2.8d);
    }

    private void checkCO2Calculation(
        double calculatedAnnualCO2Tons,
        double annualCoreHours,
        double datacenterPUE,
        double co2TonsPerKWH,
        double wattsPerCore
    ) {
        double kiloWattsPerCore = wattsPerCore / 1000.0d;
        double expectedAnnualCO2Tons = annualCoreHours * datacenterPUE * co2TonsPerKWH * kiloWattsPerCore;
        assertEquals(expectedAnnualCO2Tons, calculatedAnnualCO2Tons, 0.000000000001d);
    }
}
