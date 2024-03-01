/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal.precommit;

import org.elasticsearch.gradle.internal.conventions.precommit.PrecommitPlugin;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CheckstylePrecommitPlugin extends PrecommitPlugin {
    @Override
    public TaskProvider<? extends Task> createTask(Project project) {
        URL checkstyleConfUrl = CheckstylePrecommitPlugin.class.getResource("/checkstyle.xml");
        URL checkstyleSuppressionsUrl = CheckstylePrecommitPlugin.class.getResource("/checkstyle_suppressions.xml");
        File checkstyleDir = new File(project.getBuildDir(), "checkstyle");
        File checkstyleSuppressions = new File(checkstyleDir, "checkstyle_suppressions.xml");
        File checkstyleConf = new File(checkstyleDir, "checkstyle.xml");
        TaskProvider<Task> copyCheckstyleConf = project.getTasks().register("copyCheckstyleConf");

        copyCheckstyleConf.configure(t -> t.getOutputs().files(checkstyleSuppressions, checkstyleConf));
        if ("jar".equals(checkstyleConfUrl.getProtocol())) {
            try {
                JarURLConnection jarURLConnection = (JarURLConnection) checkstyleConfUrl.openConnection();
                copyCheckstyleConf.configure(t -> t.getInputs().file(jarURLConnection.getJarFileURL()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else if ("file".equals(checkstyleConfUrl.getProtocol())) {
            copyCheckstyleConf.configure(t -> t.getInputs().files(checkstyleConfUrl.getFile(), checkstyleSuppressionsUrl.getFile()));
        }

        copyCheckstyleConf.configure(t -> t.doLast(new Action<Task>() {
            @Override
            public void execute(Task task) {
                checkstyleDir.mkdirs();
                try (InputStream stream = checkstyleConfUrl.openStream()) {
                    Files.copy(stream, checkstyleConf.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                try (InputStream stream = checkstyleSuppressionsUrl.openStream()) {
                    Files.copy(stream, checkstyleSuppressions.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }));

        TaskProvider<Task> checkstyleTask = project.getTasks().register("checkstyle");
        checkstyleTask.configure(t -> t.dependsOn(project.getTasks().withType(Checkstyle.class)));

        project.getPluginManager().apply("checkstyle");
        CheckstyleExtension checkstyle = project.getExtensions().getByType(CheckstyleExtension.class);
        checkstyle.getConfigDirectory().set(checkstyleDir);

        DependencyHandler dependencies = project.getDependencies();
        Provider<String> conventionsDependencyProvider = project.provider(
            () -> "org.elasticsearch:build-conventions:" + project.getVersion()
        );
        dependencies.addProvider("checkstyle", project.provider(() -> {
            var versionCatalog = project.getExtensions().getByType(VersionCatalogsExtension.class).named("buildLibs");
            return versionCatalog.findLibrary("checkstyle").get().get();
        }));
        dependencies.addProvider("checkstyle", conventionsDependencyProvider, dep -> dep.setTransitive(false));

        project.getTasks().withType(Checkstyle.class).configureEach(t -> {
            t.dependsOn(copyCheckstyleConf);
            t.getMaxHeapSize().set("1g");
            t.reports(r -> r.getHtml().getRequired().set(false));
        });

        project.getPlugins()
            .withType(
                JavaBasePlugin.class,
                javaBasePlugin -> project.getExtensions()
                    .getByType(SourceSetContainer.class)
                    .all(
                        sourceSet -> project.getTasks()
                            .withType(Checkstyle.class)
                            .named(sourceSet.getTaskName("checkstyle", null))
                            .configure(t -> t.setClasspath(project.getObjects().fileCollection()))
                    )
            );

        return checkstyleTask;
    }
}
