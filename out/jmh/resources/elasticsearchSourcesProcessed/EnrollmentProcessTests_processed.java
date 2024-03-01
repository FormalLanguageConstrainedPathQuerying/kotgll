/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.packaging.test;

import org.elasticsearch.common.Strings;
import org.elasticsearch.packaging.util.Archives;
import org.elasticsearch.packaging.util.Distribution;
import org.elasticsearch.packaging.util.Shell;
import org.elasticsearch.packaging.util.docker.Docker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.elasticsearch.packaging.util.Archives.installArchive;
import static org.elasticsearch.packaging.util.Archives.verifyArchiveInstallation;
import static org.elasticsearch.packaging.util.FileUtils.getCurrentVersion;
import static org.elasticsearch.packaging.util.docker.Docker.removeContainer;
import static org.elasticsearch.packaging.util.docker.Docker.runAdditionalContainer;
import static org.elasticsearch.packaging.util.docker.Docker.runContainer;
import static org.elasticsearch.packaging.util.docker.Docker.verifyContainerInstallation;
import static org.elasticsearch.packaging.util.docker.Docker.waitForElasticsearch;
import static org.elasticsearch.packaging.util.docker.DockerRun.builder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeTrue;

public class EnrollmentProcessTests extends PackagingTestCase {

    public void test10ArchiveAutoFormCluster() throws Exception {
        /* Windows issue awaits fix: https:
        assumeTrue("expect command isn't on Windows", distribution.platform != Distribution.Platform.WINDOWS);
        assumeTrue("only archives", distribution.isArchive());
        installation = installArchive(sh, distribution(), getRootTempDir().resolve("elasticsearch-node1"), getCurrentVersion(), true);
        verifyArchiveInstallation(installation, distribution());
        setFileSuperuser("test_superuser", "test_superuser_password");
        sh.getEnv().put("ES_JAVA_OPTS", "-Xms1g -Xmx1g");
        Shell.Result startFirstNode = awaitElasticsearchStartupWithResult(
            Archives.startElasticsearchWithTty(installation, sh, null, List.of(), null, false)
        );
        assertThat(startFirstNode.isSuccess(), is(true));
        verifySecurityAutoConfigured(installation);
        Shell.Result createTokenResult = installation.executables().createEnrollmentToken.run("-s node");
        assertThat(Strings.isNullOrEmpty(createTokenResult.stdout()), is(false));
        final String enrollmentToken = createTokenResult.stdout();
        installation = installArchive(sh, distribution(), getRootTempDir().resolve("elasticsearch-node2"), getCurrentVersion(), true);

        Shell.Result startSecondNodeWithInvalidToken = Archives.startElasticsearchWithTty(
            installation,
            sh,
            null,
            List.of("--enrollment-token", "some-invalid-token-here"),
            null,
            false
        );
        assertThat(
            startSecondNodeWithInvalidToken.stdout(),
            containsString("Failed to parse enrollment token : some-invalid-token-here . Error was: Illegal base64 character 2d")
        );
        verifySecurityNotAutoConfigured(installation);

        Shell.Result startSecondNode = awaitElasticsearchStartupWithResult(
            Archives.startElasticsearchWithTty(installation, sh, null, List.of("--enrollment-token", enrollmentToken), null, false)
        );
        waitForSecondNode();
        assertThat(startSecondNode.isSuccess(), is(true));
        verifySecurityAutoConfigured(installation);
        assertThat(makeRequest("https:
    }

    public void test20DockerAutoFormCluster() throws Exception {
        assumeTrue("only docker", distribution.isDocker());
        installation = runContainer(distribution(), builder().envVar("ELASTIC_PASSWORD", "password"));
        verifyContainerInstallation(installation);
        verifySecurityAutoConfigured(installation);
        waitForElasticsearch(installation);
        final String node1ContainerId = Docker.getContainerId();

        Docker.waitForNodeStarted(node1ContainerId);

        String enrollmentToken = getEnrollmentToken();

        installation = runAdditionalContainer(distribution(), builder().envVar("ENROLLMENT_TOKEN", enrollmentToken), 9201, 9301);

        waitForElasticsearch(installation);
        verifyContainerInstallation(installation);
        verifySecurityAutoConfigured(installation);

        assertBusy(
            () -> assertThat(
                makeRequestAsElastic("https:
                containsString("\"number_of_nodes\":2")
            ),
            20,
            TimeUnit.SECONDS
        );

        removeContainer(node1ContainerId);
    }

    private String getEnrollmentToken() throws Exception {
        final AtomicReference<String> enrollmentTokenHolder = new AtomicReference<>();

        assertBusy(() -> {
            final Shell.Result result = installation.executables().createEnrollmentToken.run("-s node", null, true);

            if (result.isSuccess() == false) {
                if (result.stdout().contains("Failed to determine the health of the cluster")) {
                    throw new AssertionError("Elasticsearch is not ready yet");
                }
                throw new Shell.ShellException(
                    "Command was not successful: [elasticsearch-create-enrollment-token -s node]\n   result: " + result
                );
            }

            final String tokenValue = result.stdout()
                .lines()
                .filter(line -> line.startsWith("WARNING:") == false)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Failed to find any non-warning output lines"));
            enrollmentTokenHolder.set(tokenValue);
        }, 30, TimeUnit.SECONDS);

        return enrollmentTokenHolder.get();
    }

    private void waitForSecondNode() {
        int retries = 60;
        while (retries > 0) {
            retries -= 1;
            try (Socket s = new Socket(InetAddress.getLoopbackAddress(), 9201)) {
                return;
            } catch (IOException e) {
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new RuntimeException("Elasticsearch second node did not start listening on 9201");
    }
}
