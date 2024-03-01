/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.profiling;

import org.elasticsearch.test.ESTestCase;

import java.util.Map;

public class CostCalculatorTests extends ESTestCase {
    private static final String HOST_ID_AWS = "1110256254710195391";
    private static final int HOST_ID_A_NUM_CORES = 8;
    private static final String HOST_ID_AZURE = "2220256254710195392";
    private static final int HOST_ID_AZURE_NUM_CORES = 8;
    private static final String HOST_ID_UNKNOWN = "3330256254710195392";
    private static final Integer HOST_ID_UNKNOWN_NUM_CORES = null; 

    public void testCreateFromRegularSource() {
        Map<String, HostMetadata> hostsTable = Map.ofEntries(
            Map.entry(HOST_ID_AWS,
                new HostMetadata(HOST_ID_AWS,
                    new InstanceType(
                        "aws",
                        "eu-west-1",
                        "c5n.xlarge"
                    ),
                    "", 
                    HOST_ID_A_NUM_CORES 
                )
            ),
            Map.entry(HOST_ID_AZURE,
                new HostMetadata(HOST_ID_AZURE,
                    new InstanceType(
                        "azure",
                        "eastus2",
                        "Standard_D4s_v3"
                    ),
                    "", 
                    HOST_ID_AZURE_NUM_CORES 
                )
            ),
            Map.entry(HOST_ID_UNKNOWN,
                new HostMetadata(HOST_ID_UNKNOWN,
                    new InstanceType(
                        "on-prem-provider",
                        "on-prem-region",
                        "on-prem-instance-type"
                    ),
                    "", 
                    HOST_ID_UNKNOWN_NUM_CORES 
                )
            )
        );

        double samplingDurationInSeconds = 1_800.0d; 
        long samples = 100_000L; 
        double annualCoreHours = CostCalculator.annualCoreHours(samplingDurationInSeconds, samples, 20.0d);
        CostCalculator costCalculator = new CostCalculator(hostsTable, samplingDurationInSeconds, null, null, null);

        checkCostCalculation(costCalculator.annualCostsUSD(HOST_ID_AWS, samples), annualCoreHours, 0.244d, HOST_ID_A_NUM_CORES);

        checkCostCalculation(costCalculator.annualCostsUSD(HOST_ID_AZURE, samples), annualCoreHours, 0.192d, HOST_ID_A_NUM_CORES);

        checkCostCalculation(
            costCalculator.annualCostsUSD(HOST_ID_UNKNOWN, samples),
            annualCoreHours,
            CostCalculator.DEFAULT_COST_USD_PER_CORE_HOUR * HostMetadata.DEFAULT_PROFILING_NUM_CORES,
            HostMetadata.DEFAULT_PROFILING_NUM_CORES
        );
    }

    private void checkCostCalculation(double calculatedAnnualCostsUSD, double annualCoreHours, double usd_per_hour, int profilingNumCores) {
        double expectedAnnualCostsUSD = annualCoreHours * (usd_per_hour / profilingNumCores);
        assertEquals(expectedAnnualCostsUSD, calculatedAnnualCostsUSD, 0.00000001d);
    }
}
