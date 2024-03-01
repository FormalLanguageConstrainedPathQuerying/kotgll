/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.packaging.test;

import org.elasticsearch.Version;
import org.elasticsearch.cli.ExitCodes;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.ssl.PemKeyConfig;
import org.elasticsearch.packaging.util.FileMatcher;
import org.elasticsearch.packaging.util.Installation;
import org.elasticsearch.packaging.util.Packages;
import org.elasticsearch.packaging.util.Shell;
import org.elasticsearch.test.http.MockResponse;
import org.elasticsearch.test.http.MockWebServer;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xpack.core.security.EnrollmentToken;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.elasticsearch.packaging.util.FileMatcher.Fileness.Directory;
import static org.elasticsearch.packaging.util.FileMatcher.Fileness.File;
import static org.elasticsearch.packaging.util.FileMatcher.p660;
import static org.elasticsearch.packaging.util.FileMatcher.p750;
import static org.elasticsearch.packaging.util.FileUtils.append;
import static org.elasticsearch.packaging.util.Packages.assertInstalled;
import static org.elasticsearch.packaging.util.Packages.assertRemoved;
import static org.elasticsearch.packaging.util.Packages.installPackage;
import static org.elasticsearch.packaging.util.Packages.verifyPackageInstallation;
import static org.elasticsearch.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeTrue;

public class PackagesSecurityAutoConfigurationTests extends PackagingTestCase {

    private static final String AUTOCONFIG_DIRNAME = "certs";

    @BeforeClass
    public static void filterDistros() {
        assumeTrue("rpm or deb", distribution.isPackage());
    }

    public void test10SecurityAutoConfiguredOnPackageInstall() throws Exception {
        assertRemoved(distribution());
        installation = installPackage(sh, distribution(), successfulAutoConfiguration());
        assertInstalled(distribution());
        verifyPackageInstallation(installation, distribution(), sh);
        verifySecurityAutoConfigured(installation);
        assertNotNull(installation.getElasticPassword());
    }

    public void test20SecurityNotAutoConfiguredOnReInstallation() throws Exception {
        byte[] transportKeystore = Files.readAllBytes(installation.config(AUTOCONFIG_DIRNAME).resolve("transport.p12"));
        installation = Packages.forceUpgradePackage(sh, distribution);
        assertInstalled(distribution);
        verifyPackageInstallation(installation, distribution, sh);
        verifySecurityAutoConfigured(installation);
        assertThat(transportKeystore, equalTo(Files.readAllBytes(installation.config(AUTOCONFIG_DIRNAME).resolve("transport.p12"))));
    }

    public void test30SecurityNotAutoConfiguredWhenExistingDataDir() throws Exception {
        final Path dataPath = installation.data;
        cleanup();
        Files.createDirectory(dataPath);
        append(dataPath.resolve("foo"), "some data");
        installation = installPackage(sh, distribution(), existingSecurityConfiguration());
        verifySecurityNotAutoConfigured(installation);
    }

    public void test40SecurityNotAutoConfiguredWhenExistingKeystoreUnknownPassword() throws Exception {
        final Installation.Executables bin = installation.executables();
        bin.keystoreTool.run("passwd", "some_password\nsome_password\n");
        final Path tempDir = createTempDir("existing-keystore-config");
        final Path confPath = installation.config;
        Files.copy(
            confPath.resolve("elasticsearch.keystore"),
            tempDir.resolve("elasticsearch.keystore"),
            StandardCopyOption.COPY_ATTRIBUTES
        );
        cleanup();
        Files.createDirectory(confPath);
        Files.copy(
            tempDir.resolve("elasticsearch.keystore"),
            confPath.resolve("elasticsearch.keystore"),
            StandardCopyOption.COPY_ATTRIBUTES
        );
        installation = installPackage(sh, distribution(), errorOutput());
        List<String> configLines = Files.readAllLines(installation.config("elasticsearch.yml"));
        assertThat(configLines, not(hasItem("# have been automatically generated in order to configure Security.               #")));
    }

    public void test50ReconfigureAndEnroll() throws Exception {
        cleanup();
        assertRemoved(distribution());
        installation = installPackage(sh, distribution(), successfulAutoConfiguration());
        assertInstalled(distribution());
        verifyPackageInstallation(installation, distribution(), sh);
        verifySecurityAutoConfigured(installation);
        assertNotNull(installation.getElasticPassword());
        Shell.Result result = installation.executables().nodeReconfigureTool.run("--enrollment-token thisisinvalid", "y", true);
        assertThat(result.exitCode(), equalTo(ExitCodes.DATA_ERROR)); 
        verifySecurityNotAutoConfigured(installation);
    }

    public void test60ReconfigureWithoutEnrollmentToken() throws Exception {
        cleanup();
        assertRemoved(distribution());
        installation = installPackage(sh, distribution(), successfulAutoConfiguration());
        assertInstalled(distribution());
        verifyPackageInstallation(installation, distribution(), sh);
        verifySecurityAutoConfigured(installation);
        assertNotNull(installation.getElasticPassword());
        Shell.Result result = installation.executables().nodeReconfigureTool.run("", null, true);
        assertThat(result.exitCode(), equalTo(ExitCodes.USAGE)); 
        verifySecurityAutoConfigured(installation);
    }


    public void test70ReconfigureFailsWhenTlsAutoConfDirMissing() throws Exception {
        cleanup();
        assertRemoved(distribution());
        installation = installPackage(sh, distribution(), successfulAutoConfiguration());
        assertInstalled(distribution());
        verifyPackageInstallation(installation, distribution(), sh);
        verifySecurityAutoConfigured(installation);
        assertNotNull(installation.getElasticPassword());

        Files.move(installation.config(AUTOCONFIG_DIRNAME), installation.config("temp-autoconf-dir"));
        Shell.Result result = installation.executables().nodeReconfigureTool.run("--enrollment-token a-token", "y", true);
        assertThat(result.exitCode(), equalTo(ExitCodes.USAGE)); 
    }

    public void test71ReconfigureFailsWhenKeyStorePasswordWrong() throws Exception {
        cleanup();
        assertRemoved(distribution());
        installation = installPackage(sh, distribution(), successfulAutoConfiguration());
        assertInstalled(distribution());
        verifyPackageInstallation(installation, distribution(), sh);
        verifySecurityAutoConfigured(installation);
        assertNotNull(installation.getElasticPassword());
        Shell.Result changePassword = installation.executables().keystoreTool.run("passwd", "some-password\nsome-password\n");
        assertThat(changePassword.exitCode(), equalTo(0));
        Shell.Result result = installation.executables().nodeReconfigureTool.run(
            "--enrollment-token a-token",
            "y" + "\n" + "some-wrong-password",
            true
        );
        assertThat(result.exitCode(), equalTo(ExitCodes.IO_ERROR)); 
        assertThat(result.stderr(), containsString("Error was: Provided keystore password was incorrect"));
    }

    public void test71ReconfigureFailsWhenKeyStoreDoesNotContainExpectedSettings() throws Exception {
        cleanup();
        assertRemoved(distribution());
        installation = installPackage(sh, distribution(), successfulAutoConfiguration());
        assertInstalled(distribution());
        verifyPackageInstallation(installation, distribution(), sh);
        verifySecurityAutoConfigured(installation);
        assertNotNull(installation.getElasticPassword());
        Shell.Result removeSetting = installation.executables().keystoreTool.run(
            "remove xpack.security.transport.ssl.keystore.secure_password"
        );
        assertThat(removeSetting.exitCode(), equalTo(0));
        Shell.Result result = installation.executables().nodeReconfigureTool.run("--enrollment-token a-token", "y", true);
        assertThat(result.exitCode(), equalTo(ExitCodes.IO_ERROR));
        assertThat(
            result.stderr(),
            containsString(
                "elasticsearch.keystore did not contain expected setting [xpack.security.transport.ssl.keystore.secure_password]."
            )
        );
    }

    public void test72ReconfigureFailsWhenConfigurationDoesNotContainSecurityAutoConfig() throws Exception {
        cleanup();
        assertRemoved(distribution());
        installation = installPackage(sh, distribution(), successfulAutoConfiguration());
        assertInstalled(distribution());
        verifyPackageInstallation(installation, distribution(), sh);
        verifySecurityAutoConfigured(installation);
        assertNotNull(installation.getElasticPassword());
        Path yml = installation.config("elasticsearch.yml");
        Files.write(yml, List.of(), TRUNCATE_EXISTING);

        Shell.Result result = installation.executables().nodeReconfigureTool.run("--enrollment-token a-token", "y", true);
        assertThat(result.exitCode(), equalTo(ExitCodes.USAGE)); 
        assertThat(result.stderr(), containsString("Expected configuration is missing from elasticsearch.yml."));
    }

    public void test72ReconfigureRetainsUserSettings() throws Exception {
        cleanup();
        assertRemoved(distribution());
        installation = installPackage(sh, distribution(), successfulAutoConfiguration());
        assertInstalled(distribution());
        verifyPackageInstallation(installation, distribution(), sh);
        verifySecurityAutoConfigured(installation);
        assertNotNull(installation.getElasticPassword());
        Path yml = installation.config("elasticsearch.yml");
        List<String> allLines = Files.readAllLines(yml);
        allLines.set(
            allLines.indexOf("# Enable encryption for HTTP API client connections, such as Kibana, Logstash, and Agents"),
            "cluster.name: testclustername"
        );
        allLines.add("node.name: testnodename");
        Files.write(yml, allLines, TRUNCATE_EXISTING);

        Shell.Result result = installation.executables().nodeReconfigureTool.run("--enrollment-token thisisinvalid", "y", true);
        assertThat(result.exitCode(), equalTo(ExitCodes.DATA_ERROR)); 
        verifySecurityNotAutoConfigured(installation);
        Path editedYml = installation.config("elasticsearch.yml");
        List<String> newConfigurationLines = Files.readAllLines(editedYml);
        assertThat(newConfigurationLines, hasItem("cluster.name: testclustername"));
        assertThat(newConfigurationLines, hasItem("node.name: testnodename"));
    }

    public void test73ReconfigureCreatesFilesWithCorrectPermissions() throws Exception {
        cleanup();
        assertRemoved(distribution());
        installation = installPackage(sh, distribution(), successfulAutoConfiguration());
        assertInstalled(distribution());
        verifyPackageInstallation(installation, distribution(), sh);
        verifySecurityAutoConfigured(installation);
        assertNotNull(installation.getElasticPassword());
        final PemKeyConfig keyConfig = new PemKeyConfig(
            Paths.get(getClass().getResource("http.crt").toURI()).toAbsolutePath().normalize().toString(),
            Paths.get(getClass().getResource("http.key").toURI()).toAbsolutePath().normalize().toString(),
            new char[0],
            Paths.get(getClass().getResource("http.crt").toURI()).getParent().toAbsolutePath().normalize()
        );
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[] { keyConfig.createKeyManager() }, new TrustManager[] {}, new SecureRandom());
        try (MockWebServer mockNode = new MockWebServer(sslContext, false)) {
            mockNode.start();
            final String httpCaCertPemString = Files.readAllLines(
                Paths.get(getClass().getResource("http_ca.crt").toURI()).toAbsolutePath().normalize()
            ).stream().filter(l -> l.contains("-----") == false).collect(Collectors.joining());
            final String httpCaKeyPemString = Files.readAllLines(
                Paths.get(getClass().getResource("http_ca.key").toURI()).toAbsolutePath().normalize()
            ).stream().filter(l -> l.contains("-----") == false).collect(Collectors.joining());
            final String transportCaCertPemString = Files.readAllLines(
                Paths.get(getClass().getResource("transport_ca.crt").toURI()).toAbsolutePath().normalize()
            ).stream().filter(l -> l.contains("-----") == false).collect(Collectors.joining());
            final String transportKeyPemString = Files.readAllLines(
                Paths.get(getClass().getResource("transport.key").toURI()).toAbsolutePath().normalize()
            ).stream().filter(l -> l.contains("-----") == false).collect(Collectors.joining());
            final String transportCertPemString = Files.readAllLines(
                Paths.get(getClass().getResource("transport.crt").toURI()).toAbsolutePath().normalize()
            ).stream().filter(l -> l.contains("-----") == false).collect(Collectors.joining());
            final XContentBuilder responseBuilder = jsonBuilder().startObject()
                .field("http_ca_key", httpCaKeyPemString)
                .field("http_ca_cert", httpCaCertPemString)
                .field("transport_ca_cert", transportCaCertPemString)
                .field("transport_key", transportKeyPemString)
                .field("transport_cert", transportCertPemString)
                .array("nodes_addresses", "192.168.1.23:9300") 
                .endObject();
            mockNode.enqueue(new MockResponse().setResponseCode(200).setBody(Strings.toString(responseBuilder)));
            final EnrollmentToken enrollmentToken = new EnrollmentToken(
                "some-api-key",
                "b0150fd8a29f9012207912de9a01aa1d1f0dd696c847d3a9353881f9045bf442", 
                Version.CURRENT.toString(),
                List.of(mockNode.getHostName() + ":" + mockNode.getPort())
            );
            Shell.Result result = installation.executables().nodeReconfigureTool.run(
                "-v --enrollment-token " + enrollmentToken.getEncoded(),
                "y",
                true
            );
            assertThat(result.exitCode(), CoreMatchers.equalTo(0));
            assertThat(installation.config(AUTOCONFIG_DIRNAME), FileMatcher.file(Directory, "root", "elasticsearch", p750));
            Stream.of("http.p12", "http_ca.crt", "transport.p12")
                .forEach(
                    file -> assertThat(
                        installation.config(AUTOCONFIG_DIRNAME).resolve(file),
                        FileMatcher.file(File, "root", "elasticsearch", p660)
                    )
                );
        }
    }

    private Predicate<String> successfulAutoConfiguration() {
        Predicate<String> p1 = output -> output.contains("Authentication and authorization are enabled.");
        Predicate<String> p2 = output -> output.contains("TLS for the transport and HTTP layers is enabled and configured.");
        Predicate<String> p3 = output -> output.contains("The generated password for the elastic built-in superuser is :");
        return p1.and(p2).and(p3);
    }

    private Predicate<String> existingSecurityConfiguration() {
        return output -> output.contains("Skipping auto-configuration because security features appear to be already configured.");
    }

    private Predicate<String> errorOutput() {
        Predicate<String> p1 = output -> output.contains("Failed to auto-configure security features.");
        Predicate<String> p2 = output -> output.contains("However, authentication and authorization are still enabled.");
        Predicate<String> p3 = output -> output.contains("You can reset the password of the elastic built-in superuser with");
        Predicate<String> p4 = output -> output.contains("/usr/share/elasticsearch/bin/elasticsearch-reset-password -u elastic");
        return p1.and(p2).and(p3).and(p4);
    }

}
