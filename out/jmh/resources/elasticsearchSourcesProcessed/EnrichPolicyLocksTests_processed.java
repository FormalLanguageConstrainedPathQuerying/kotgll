/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.enrich;

import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.enrich.EnrichPolicyLocks.EnrichPolicyLock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

public class EnrichPolicyLocksTests extends ESTestCase {

    public void testLockPolicy() {
        EnrichPolicyLocks policyLocks = new EnrichPolicyLocks();
        String policy1 = "policy1";
        String policy2 = "policy2";

        policyLocks.lockPolicy(policy1);

        EsRejectedExecutionException exception1 = expectThrows(EsRejectedExecutionException.class, () -> policyLocks.lockPolicy(policy1));
        assertThat(
            exception1.getMessage(),
            is(equalTo("Could not obtain lock because policy execution for [policy1]" + " is already in progress."))
        );

        policyLocks.lockPolicy(policy2);
        EsRejectedExecutionException exception2 = expectThrows(EsRejectedExecutionException.class, () -> policyLocks.lockPolicy(policy2));

        assertThat(
            exception2.getMessage(),
            is(equalTo("Could not obtain lock because policy execution for [policy2]" + " is already in progress."))
        );
    }

    public void testReleasePolicy() {
        EnrichPolicyLocks policyLocks = new EnrichPolicyLocks();
        String policy1 = "policy1";
        String policy2 = "policy2";

        EnrichPolicyLock lock1 = policyLocks.lockPolicy(policy1);
        EnrichPolicyLock lock2 = policyLocks.lockPolicy(policy2);

        lock1.close();
        lock2.close();

        policyLocks.lockPolicy(policy1);
        policyLocks.lockPolicy(policy2);
    }

    public void testNonRepeatableRelease() {
        EnrichPolicyLocks policyLocks = new EnrichPolicyLocks();
        String policy1 = "policy1";

        EnrichPolicyLock lock1 = policyLocks.lockPolicy(policy1);
        assertThat(policyLocks.lockedPolices().size(), equalTo(1));
        lock1.close();
        assertThat(policyLocks.lockedPolices().size(), equalTo(0));

        EnrichPolicyLock lock2 = policyLocks.lockPolicy(policy1);
        assertThat(policyLocks.lockedPolices().size(), equalTo(1));
        expectThrows(AssertionError.class, lock1::close);
        assertThat(policyLocks.lockedPolices().size(), equalTo(1));

        lock2.close();
        assertThat(policyLocks.lockedPolices().size(), equalTo(0));
    }
}
