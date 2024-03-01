/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal.conventions.info;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GitInfo {
    private static final Pattern GIT_PATTERN = Pattern.compile("git@([^:]+):([^\\.]+)\\.git");

    private final String revision;
    private final String origin;

    private GitInfo(String revision, String origin) {
        this.revision = revision;
        this.origin = origin;
    }

    public String getRevision() {
        return revision;
    }

    public String getOrigin() {
        return origin;
    }

    public static GitInfo gitInfo(File rootDir) {
        try {
            /*
             * We want to avoid forking another process to run git rev-parse HEAD. Instead, we will read the refs manually. The
             * documentation for this follows from https:
             *
             * There are two cases to consider:
             *  - a plain repository with .git directory at the root of the working tree
             *  - a worktree with a plain text .git file at the root of the working tree
             *
             * In each case, our goal is to parse the HEAD file to get either a ref or a bare revision (in the case of being in detached
             * HEAD state).
             *
             * In the case of a plain repository, we can read the HEAD file directly, resolved directly from the .git directory.
             *
             * In the case of a worktree, we read the gitdir from the plain text .git file. This resolves to a directory from which we read
             * the HEAD file and resolve commondir to the plain git repository.
             */
            final Path dotGit = rootDir.toPath().resolve(".git");
            final String revision;
            if (Files.exists(dotGit) == false) {
                return new GitInfo("unknown", "unknown");
            }
            final Path head;
            final Path gitDir;
            if (Files.isDirectory(dotGit)) {
                head = dotGit.resolve("HEAD");
                gitDir = dotGit;
            } else {
                final Path reference = Paths.get(readFirstLine(dotGit).substring("gitdir:".length()).trim());
                if (reference.getParent().endsWith("modules")) {
                    gitDir = rootDir.toPath().resolve(reference);
                    head = gitDir.resolve("HEAD");
                } else {
                    if (Files.exists(reference) == false) {
                        return new GitInfo("unknown", "unknown");
                    }
                    head = reference.resolve("HEAD");
                    final Path commonDir = Paths.get(readFirstLine(reference.resolve("commondir")));
                    if (commonDir.isAbsolute()) {
                        gitDir = commonDir;
                    } else {
                        gitDir = reference.resolve(commonDir);
                    }
                }
            }
            final String ref = readFirstLine(head);
            if (ref.startsWith("ref:")) {
                String refName = ref.substring("ref:".length()).trim();
                Path refFile = gitDir.resolve(refName);
                if (Files.exists(refFile)) {
                    revision = readFirstLine(refFile);
                } else if (Files.exists(gitDir.resolve("packed-refs"))) {
                    Pattern p = Pattern.compile("^([a-f0-9]{40}) " + refName + "$");
                    try (Stream<String> lines = Files.lines(gitDir.resolve("packed-refs"))) {
                        revision = lines.map(p::matcher)
                                .filter(Matcher::matches)
                                .map(m -> m.group(1))
                                .findFirst()
                                .orElseThrow(() -> new IOException("Packed reference not found for refName " + refName));
                    }
                } else {
                    File refsDir = gitDir.resolve("refs").toFile();
                    if (refsDir.exists()) {
                        String foundRefs = Arrays.stream(refsDir.listFiles()).map(f -> f.getName()).collect(Collectors.joining("\n"));
                        Logging.getLogger(GitInfo.class).error("Found git refs\n" + foundRefs);
                    } else {
                        Logging.getLogger(GitInfo.class).error("No git refs dir found");
                    }
                    throw new GradleException("Can't find revision for refName " + refName);
                }
            } else {
                revision = ref;
            }
            return new GitInfo(revision, findOriginUrl(gitDir.resolve("config")));
        } catch (final IOException e) {
            throw new GradleException("unable to read the git revision", e);
        }
    }


    private static String findOriginUrl(final Path configFile) throws IOException {
        Map<String, String> props = new HashMap<>();

        try (Stream<String> stream = Files.lines(configFile, StandardCharsets.UTF_8)) {
            Iterator<String> lines = stream.iterator();
            boolean foundOrigin = false;
            while (lines.hasNext()) {
                String line = lines.next().trim();
                if (line.startsWith(";") || line.startsWith("#")) {
                    continue;
                }
                if (foundOrigin) {
                    if (line.startsWith("[")) {
                        break;
                    }
                    String[] pair = line.trim().split("=", 2);
                    props.put(pair[0].trim(), pair[1].trim());
                } else {
                    if (line.equals("[remote \"origin\"]")) {
                        foundOrigin = true;
                    }
                }
            }
        }

        String originUrl = props.get("url");
        return originUrl == null ? "unknown" : originUrl;
    }

    private static String readFirstLine(final Path path) throws IOException {
        String firstLine;
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            firstLine = lines.findFirst().orElseThrow(() -> new IOException("file [" + path + "] is empty"));
        }
        return firstLine;
    }

    /** Find the reponame. */
    public String urlFromOrigin() {
        if (origin == null) {
            return null; 
        }
        if (origin.startsWith("https")) {
            return origin;
        }
        Matcher matcher = GIT_PATTERN.matcher(origin);
        if (matcher.matches()) {
            return String.format("https:
        } else {
            return origin; 
        }
    }

}
