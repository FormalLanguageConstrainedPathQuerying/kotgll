/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.packaging.test;

import org.apache.http.client.fluent.Request;
import org.elasticsearch.packaging.util.FileUtils;
import org.elasticsearch.packaging.util.Platforms;
import org.elasticsearch.packaging.util.ServerUtils;
import org.junit.Before;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.nio.file.attribute.PosixFilePermissions.fromString;
import static org.elasticsearch.packaging.util.FileUtils.append;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assume.assumeFalse;

public class ConfigurationTests extends PackagingTestCase {

    @Before
    public void filterDistros() {
        assumeFalse("no docker", distribution.isDocker());
    }

    public void test10Install() throws Exception {
        install();
        setFileSuperuser("test_superuser", "test_superuser_password");
    }

    public void test20HostnameSubstitution() throws Exception {
        String hostnameKey = Platforms.WINDOWS ? "COMPUTERNAME" : "HOSTNAME";
        sh.getEnv().put(hostnameKey, "mytesthost");
        withCustomConfig(confPath -> {
            FileUtils.append(confPath.resolve("elasticsearch.yml"), "node.name: ${HOSTNAME}");
            if (distribution.isPackage()) {
                append(installation.envFile, "HOSTNAME=mytesthost");
                ServerUtils.removeSettingFromExistingConfiguration(confPath, "cluster.initial_master_nodes");
                ServerUtils.addSettingToExistingConfiguration(confPath, "cluster.initial_master_nodes", "[\"${HOSTNAME}\"]");
            }
            Platforms.onWindows(() -> sh.chown(confPath, installation.getOwner()));
            assertWhileRunning(() -> {
                final String nameResponse = ServerUtils.makeRequest(
                    Request.Get("https:
                    "test_superuser",
                    "test_superuser_password",
                    ServerUtils.getCaCert(confPath)
                ).strip();
                assertThat(nameResponse, equalTo("mytesthost"));
            });
            Platforms.onWindows(() -> sh.chown(confPath));
        });
    }

    public void test30SymlinkedDataPath() throws Exception {
        Path data = createTempDir("temp-data");
        Platforms.onLinux(() -> Files.setPosixFilePermissions(data, fromString("rwxrwxrwx")));
        Path symlinkedData = createTempDir("symlink-data");
        Files.delete(symlinkedData); 
        Files.createSymbolicLink(symlinkedData, data);

        withCustomConfig(confPath -> {
            ServerUtils.addSettingToExistingConfiguration(confPath, "path.data", symlinkedData.toString());
            Platforms.onWindows(() -> sh.chown(confPath, installation.getOwner()));
            assertWhileRunning(() -> {
                try (Stream<Path> entries = Files.list(data)) {
                    assertTrue(entries.findFirst().isPresent());
                }
            });
        });
    }

}
