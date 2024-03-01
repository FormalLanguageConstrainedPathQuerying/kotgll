/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal;

import com.github.jengelman.gradle.plugins.shadow.ShadowBasePlugin;

import org.elasticsearch.gradle.OS;
import org.elasticsearch.gradle.internal.conventions.util.Util;
import org.elasticsearch.gradle.internal.info.BuildParams;
import org.elasticsearch.gradle.internal.info.GlobalBuildInfoPlugin;
import org.elasticsearch.gradle.internal.test.ErrorReportingTestListener;
import org.elasticsearch.gradle.internal.test.SimpleCommandLineArgumentProvider;
import org.elasticsearch.gradle.test.GradleTestPolicySetupPlugin;
import org.elasticsearch.gradle.test.SystemPropertyCommandLineArgumentProvider;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;

import java.io.File;
import java.util.Map;

import static org.elasticsearch.gradle.util.FileUtils.mkdirs;
import static org.elasticsearch.gradle.util.GradleUtils.maybeConfigure;

/**
 * Applies commonly used settings to all Test tasks in the project
 */
public class ElasticsearchTestBasePlugin implements Plugin<Project> {

    public static final String DUMP_OUTPUT_ON_FAILURE_PROP_NAME = "dumpOutputOnFailure";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(GradleTestPolicySetupPlugin.class);
        project.getRootProject().getPluginManager().apply(GlobalBuildInfoPlugin.class);
        maybeConfigure(project.getTasks(), "test", Test.class, task -> task.include("**/*Tests.class"));

        File heapdumpDir = new File(project.getBuildDir(), "heapdump");

        project.getTasks().withType(Test.class).configureEach(test -> {
            File testOutputDir = new File(test.getReports().getJunitXml().getOutputLocation().getAsFile().get(), "output");

            ErrorReportingTestListener listener = new ErrorReportingTestListener(test, testOutputDir);
            test.getExtensions().getExtraProperties().set(DUMP_OUTPUT_ON_FAILURE_PROP_NAME, true);
            test.getExtensions().add("errorReportingTestListener", listener);
            test.addTestOutputListener(listener);
            test.addTestListener(listener);

            /*
             * We use lazy-evaluated strings in order to configure system properties whose value will not be known until
             * execution time (e.g. cluster port numbers). Adding these via the normal DSL doesn't work as these get treated
             * as task inputs and therefore Gradle attempts to snapshot them before/after task execution. This fails due
             * to the GStrings containing references to non-serializable objects.
             *
             * We bypass this by instead passing this system properties vi a CommandLineArgumentProvider. This has the added
             * side-effect that these properties are NOT treated as inputs, therefore they don't influence things like the
             * build cache key or up to date checking.
             */
            SystemPropertyCommandLineArgumentProvider nonInputProperties = new SystemPropertyCommandLineArgumentProvider();

            test.doFirst(new Action<>() {
                @Override
                public void execute(Task t) {
                    mkdirs(testOutputDir);
                    mkdirs(heapdumpDir);
                    mkdirs(test.getWorkingDir());
                    mkdirs(test.getWorkingDir().toPath().resolve("temp").toFile());

                    test.systemProperty("java.locale.providers", "SPI,COMPAT");
                }
            });
            test.getJvmArgumentProviders().add(nonInputProperties);
            test.getExtensions().add("nonInputProperties", nonInputProperties);

            test.setWorkingDir(project.file(project.getBuildDir() + "/testrun/" + test.getName().replace("#", "_")));
            test.setMaxParallelForks(Integer.parseInt(System.getProperty("tests.jvms", BuildParams.getDefaultParallel().toString())));

            test.exclude("**/*$*.class");

            test.jvmArgs(
                "-Xmx" + System.getProperty("tests.heap.size", "512m"),
                "-Xms" + System.getProperty("tests.heap.size", "512m"),
                "-Djava.security.manager=allow",
                "--add-opens=java.base/java.security.cert=ALL-UNNAMED",
                "--add-opens=java.base/java.nio.channels=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/javax.net.ssl=ALL-UNNAMED",
                "--add-opens=java.base/java.nio.file=ALL-UNNAMED",
                "--add-opens=java.base/java.time=ALL-UNNAMED",
                "--add-opens=java.management/java.lang.management=ALL-UNNAMED",
                "-XX:+HeapDumpOnOutOfMemoryError"
            );

            test.getJvmArgumentProviders().add(new SimpleCommandLineArgumentProvider("-XX:HeapDumpPath=" + heapdumpDir));

            String argline = System.getProperty("tests.jvm.argline");
            if (argline != null) {
                test.jvmArgs((Object[]) argline.split(" "));
            }

            if (Util.getBooleanProperty("tests.asserts", true)) {
                test.jvmArgs("-ea", "-esa");
            }

            Map<String, String> sysprops = Map.of(
                "java.awt.headless",
                "true",
                "tests.artifact",
                project.getName(),
                "tests.security.manager",
                "true",
                "jna.nosys",
                "true"
            );
            test.systemProperties(sysprops);

            if (System.getProperty("ignore.tests.seed") != null) {
                nonInputProperties.systemProperty("tests.seed", BuildParams.getTestSeed());
            } else {
                test.systemProperty("tests.seed", BuildParams.getTestSeed());
            }

            File gradleUserHome = project.getGradle().getGradleUserHomeDir();
            nonInputProperties.systemProperty("gradle.user.home", gradleUserHome);
            nonInputProperties.systemProperty("java.io.tmpdir", test.getWorkingDir().toPath().resolve("temp"));

            test.systemProperty("tests.logger.level", "WARN");
            System.getProperties().entrySet().forEach(entry -> {
                if ((entry.getKey().toString().startsWith("tests.") || entry.getKey().toString().startsWith("es."))) {
                    test.systemProperty(entry.getKey().toString(), entry.getValue());
                }
            });

            test.systemProperty("es.scripting.update.ctx_in_params", "false");

            test.systemProperty("es.search.rewrite_sort", "true");

            test.systemProperty("es.transport.cname_in_publish_address", "true");

            test.systemProperty("io.netty.noUnsafe", "true");
            test.systemProperty("io.netty.noKeySetOptimization", "true");
            test.systemProperty("io.netty.recycler.maxCapacityPerThread", "0");

            test.testLogging(logging -> {
                logging.setShowExceptions(true);
                logging.setShowCauses(true);
                logging.setExceptionFormat("full");
            });

            if (OS.current().equals(OS.WINDOWS) && System.getProperty("tests.timeoutSuite") == null) {
                test.systemProperty("tests.timeoutSuite", "2400000!");
            }

            /*
             *  If this project builds a shadow JAR than any unit tests should test against that artifact instead of
             *  compiled class output and dependency jars. This better emulates the runtime environment of consumers.
             */
            project.getPluginManager().withPlugin("com.github.johnrengelman.shadow", p -> {
                if (test.getName().equals(JavaPlugin.TEST_TASK_NAME)) {
                    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
                    FileCollection mainRuntime = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getRuntimeClasspath();
                    Configuration shadowConfig = project.getConfigurations().getByName(ShadowBasePlugin.CONFIGURATION_NAME);
                    FileCollection shadowJar = project.files(project.getTasks().named("shadowJar"));
                    FileCollection testRuntime = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getRuntimeClasspath();
                    test.setClasspath(testRuntime.minus(mainRuntime).plus(shadowConfig).plus(shadowJar));
                }
            });
        });
    }
}
