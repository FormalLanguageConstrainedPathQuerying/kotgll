/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal;

import org.elasticsearch.gradle.VersionProperties;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepositoriesSetupPlugin implements Plugin<Project> {

    private static final Pattern LUCENE_SNAPSHOT_REGEX = Pattern.compile("\\w+-snapshot-([a-z0-9]+)");

    @Override
    public void apply(Project project) {
        configureRepositories(project);
    }

    /**
     * Adds repositories used by ES projects and dependencies
     */
    public static void configureRepositories(Project project) {
        RepositoryHandler repos = project.getRepositories();
        if (System.getProperty("repos.mavenLocal") != null) {
            repos.mavenLocal();
        }
        repos.mavenCentral();

        String luceneVersion = VersionProperties.getLucene();
        if (luceneVersion.contains("-snapshot")) {
            Matcher matcher = LUCENE_SNAPSHOT_REGEX.matcher(luceneVersion);
            if (matcher.find() == false) {
                throw new GradleException("Malformed lucene snapshot version: " + luceneVersion);
            }
            String revision = matcher.group(1);
            MavenArtifactRepository luceneRepo = repos.maven(repo -> {
                repo.setName("lucene-snapshots");
                repo.setUrl("https:
            });
            repos.exclusiveContent(exclusiveRepo -> {
                exclusiveRepo.filter(
                    descriptor -> descriptor.includeVersionByRegex("org\\.apache\\.lucene", ".*", ".*-snapshot-" + revision)
                );
                exclusiveRepo.forRepositories(luceneRepo);
            });
        }
    }
}
