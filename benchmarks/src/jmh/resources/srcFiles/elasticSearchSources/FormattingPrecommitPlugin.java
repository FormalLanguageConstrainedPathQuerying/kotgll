/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal.conventions.precommit;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.gradle.spotless.SpotlessPlugin;

import org.elasticsearch.gradle.internal.conventions.util.Util;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;

/**
 * This plugin configures formatting for Java source using Spotless
 * for Gradle. Since the act of formatting existing source can interfere
 * with developers' workflows, we don't automatically format all code
 * (yet). Instead, we maintain a list of projects that are excluded from
 * formatting, until we reach a point where we can comfortably format them
 * in one go without too much disruption.
 *
 * <p>Any new sub-projects must not be added to the exclusions list!
 *
 * <p>To perform a reformat, run:
 *
 * <pre>    ./gradlew spotlessApply</pre>
 *
 * <p>To check the current format, run:
 *
 * <pre>    ./gradlew spotlessJavaCheck</pre>
 *
 * <p>This is also carried out by the `precommit` task.
 *
 * <p>See also the <a href="https:
 * >Spotless project page</a>.
 */
public class FormattingPrecommitPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().withPlugin("java-base", javaBasePlugin -> {
            project.getPlugins().apply(PrecommitTaskPlugin.class);
            project.getPlugins().apply(SpotlessPlugin.class);

            project.getRepositories().mavenCentral();

            project.getExtensions().getByType(SpotlessExtension.class).java(java -> {
                File elasticsearchWorkspace = Util.locateElasticsearchWorkspace(project.getGradle());
                String importOrderPath = "build-conventions/elastic.importorder";
                String formatterConfigPath = "build-conventions/formatterConfig.xml";

                java.target("src/**/*.java");
                java.removeUnusedImports();

                java.importOrderFile(new File(elasticsearchWorkspace, importOrderPath));

                java.eclipse().configFile(new File(elasticsearchWorkspace, formatterConfigPath));

                java.trimTrailingWhitespace();

                if(Boolean.getBoolean("BUILD_PERFORMANCE_TEST") && project.getPath().equals(":server")) {
                    java.targetExclude("src/main/java/org/elasticsearch/bootstrap/BootstrapInfo.java");
                }
            });

            project.getTasks().named("precommit").configure(precommitTask -> precommitTask.dependsOn("spotlessJavaCheck"));
        });
    }
}
