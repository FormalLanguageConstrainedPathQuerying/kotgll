/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.profile;

import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.test.SecuritySingleNodeTestCase;
import org.elasticsearch.xpack.core.security.action.profile.Profile;
import org.elasticsearch.xpack.core.security.authc.Authentication;
import org.elasticsearch.xpack.core.security.user.User;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.xpack.core.security.authc.AuthenticationTestHelper.randomRealmRef;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class ProfileSingleNodeTests extends SecuritySingleNodeTestCase {

    public void testConcurrentActivateUpdates() throws InterruptedException {
        final Authentication.RealmRef realmRef = randomRealmRef(false);
        final User originalUser = new User(randomAlphaOfLengthBetween(5, 12));
        final Authentication originalAuthentication = Authentication.newRealmAuthentication(originalUser, realmRef);

        final ProfileService profileService = getInstanceFromNode(ProfileService.class);
        final PlainActionFuture<Profile> originalFuture = new PlainActionFuture<>();
        profileService.activateProfile(originalAuthentication, originalFuture);
        final Profile originalProfile = originalFuture.actionGet();

        final Thread[] threads = new Thread[randomIntBetween(5, 10)];
        final CountDownLatch readyLatch = new CountDownLatch(threads.length);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final Set<Profile> updatedProfiles = ConcurrentHashMap.newKeySet();
        final Authentication updatedAuthentication = Authentication.newRealmAuthentication(
            new User(originalUser.principal(), "foo"),
            realmRef
        );
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    final PlainActionFuture<Profile> future = new PlainActionFuture<>();
                    readyLatch.countDown();
                    startLatch.await();
                    profileService.activateProfile(updatedAuthentication, future);
                    final Profile updatedProfile = future.actionGet();
                    assertThat(updatedProfile.uid(), equalTo(originalProfile.uid()));
                    assertThat(updatedProfile.user().roles(), contains("foo"));
                    updatedProfiles.add(updatedProfile);
                } catch (Exception e) {
                    logger.error(e);
                    fail("caught error when activating existing profile: " + e);
                }
            });
            threads[i].start();
        }

        if (readyLatch.await(20, TimeUnit.SECONDS)) {
            startLatch.countDown();
            for (Thread thread : threads) {
                thread.join();
            }
            assertThat(updatedProfiles, not(emptyIterable()));
            final Profile updatedProfile = updatedProfiles.stream()
                .max(Comparator.comparingLong(p -> p.versionControl().seqNo()))
                .orElseThrow();
            final PlainActionFuture<Profile> future = new PlainActionFuture<>();
            profileService.activateProfile(updatedAuthentication, future);
            assertThat(future.actionGet(), equalTo(updatedProfile));
        } else {
            fail("Not all threads are ready after waiting");
        }
    }

}
