/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.packaging.test;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.elasticsearch.Version;
import org.elasticsearch.packaging.util.Distribution;
import org.elasticsearch.packaging.util.FileUtils;
import org.elasticsearch.packaging.util.Installation;
import org.elasticsearch.packaging.util.Packages;
import org.elasticsearch.packaging.util.ServerUtils;
import org.junit.BeforeClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.elasticsearch.packaging.util.Packages.assertInstalled;
import static org.elasticsearch.packaging.util.Packages.installPackage;
import static org.elasticsearch.packaging.util.Packages.verifyPackageInstallation;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assume.assumeTrue;

public class PackageUpgradeTests extends PackagingTestCase {

    protected static final Distribution bwcDistribution;
    static {
        bwcDistribution = new Distribution(Paths.get(System.getProperty("tests.bwc-distribution")));
    }

    @BeforeClass
    public static void filterVersions() {
        assumeTrue("only wire compatible versions", Version.fromString(bwcDistribution.baseVersion).isCompatible(Version.CURRENT));
    }

    public void test10InstallBwcVersion() throws Exception {
        installation = installPackage(sh, bwcDistribution);
        assertInstalled(bwcDistribution);
        if (Version.fromString(bwcDistribution.baseVersion).onOrAfter(Version.V_8_0_0)) {
            possiblyRemoveSecurityConfiguration(installation);
        }
    }

    public void test11ModifyKeystore() throws Exception {
        installation.executables().keystoreTool.run("remove keystore.seed");
        installation.executables().keystoreTool.run("add -x keystore.seed", "keystore_seed");
    }

    public void test12SetupBwcVersion() throws Exception {
        startElasticsearch();

        ServerUtils.makeRequest(
            Request.Put("http:
                .bodyString("{\"settings\":{\"index\":{\"number_of_replicas\":0}}}", ContentType.APPLICATION_JSON)
        );
        ServerUtils.makeRequest(
            Request.Put("http:
                .bodyString("{\"settings\":{\"index\":{\"number_of_replicas\":0}}}", ContentType.APPLICATION_JSON)
        );

        ServerUtils.makeRequest(
            Request.Post("http:
                .bodyString("{ \"title\": \"Elasticsearch - The Definitive Guide\"}", ContentType.APPLICATION_JSON)
        );
        ServerUtils.makeRequest(
            Request.Post("http:
                .bodyString("{ \"title\": \"Brave New World\"}", ContentType.APPLICATION_JSON)
        );
        ServerUtils.makeRequest(
            Request.Post("http:
                .bodyString("{ \"title\": \"The Left Hand of Darkness\"}", ContentType.APPLICATION_JSON)
        );

        assertDocsExist();

        stopElasticsearch();
    }

    public void test20InstallUpgradedVersion() throws Exception {
        if (bwcDistribution.path.equals(distribution.path)) {
            installation = Packages.forceUpgradePackage(sh, distribution);
        } else {
            installation = Packages.upgradePackage(sh, distribution);
        }

        assertInstalled(distribution);
        verifyPackageInstallation(installation, distribution, sh);
        verifySecurityNotAutoConfigured(installation);

        ServerUtils.addSettingToExistingConfiguration(installation, "xpack.security.enabled", "false");
    }

    public void test21CheckUpgradedVersion() throws Exception {
        assertWhileRunning(this::assertDocsExist);
    }

    private void assertDocsExist() throws Exception {
        String response1 = ServerUtils.makeRequest(Request.Get("http:
        assertThat(response1, containsString("Elasticsearch"));
        String response2 = ServerUtils.makeRequest(Request.Get("http:
        assertThat(response2, containsString("World"));
        String response3 = ServerUtils.makeRequest(Request.Get("http:
        assertThat(response3, containsString("Darkness"));
    }

    private void possiblyRemoveSecurityConfiguration(Installation es) throws IOException {
        ServerUtils.disableSecurityFeatures(es);
        if (Files.exists(es.config("certs"))) {
            FileUtils.rm(es.config("certs"));
        }
        for (String entry : List.of(
            "xpack.security.transport.ssl.keystore.secure_password",
            "xpack.security.transport.ssl.truststore.secure_password",
            "xpack.security.http.ssl.keystore.secure_password",
            "autoconfiguration.password_hash"
        )) {
            if (es.executables().keystoreTool.run("list").stdout().contains(entry)) {
                es.executables().keystoreTool.run("remove " + entry);
            }
        }
    }
}
