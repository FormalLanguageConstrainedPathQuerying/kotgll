/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch;

import org.elasticsearch.common.Strings;
import org.elasticsearch.core.UpdateForV9;
import org.elasticsearch.internal.BuildExtension;
import org.elasticsearch.plugins.ExtensionLoader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

public class ReleaseVersions {

    private static final boolean USES_VERSIONS;

    static {
        USES_VERSIONS = ExtensionLoader.loadSingleton(ServiceLoader.load(BuildExtension.class))
            .map(BuildExtension::hasReleaseVersioning)
            .orElse(true);
    }

    private static final Pattern VERSION_LINE = Pattern.compile("(\\d+\\.\\d+\\.\\d+),(\\d+)");

    public static IntFunction<String> generateVersionsLookup(Class<?> versionContainer) {
        if (USES_VERSIONS == false) return Integer::toString;

        try {
            String versionsFileName = versionContainer.getSimpleName() + ".csv";
            InputStream versionsFile = versionContainer.getResourceAsStream(versionsFileName);
            if (versionsFile == null) {
                throw new FileNotFoundException(Strings.format("Could not find versions file for class [%s]", versionContainer));
            }

            NavigableMap<Integer, List<Version>> versions = new TreeMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(versionsFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    var matcher = VERSION_LINE.matcher(line);
                    if (matcher.matches() == false) {
                        throw new IOException(Strings.format("Incorrect format for line [%s] in [%s]", line, versionsFileName));
                    }
                    try {
                        Integer id = Integer.valueOf(matcher.group(2));
                        Version version = Version.fromString(matcher.group(1));
                        versions.computeIfAbsent(id, k -> new ArrayList<>()).add(version);
                    } catch (IllegalArgumentException e) {
                        assert false : "Regex allowed non-integer id or incorrect version through: " + e;
                        throw new IOException(Strings.format("Incorrect format for line [%s] in [%s]", line, versionsFileName), e);
                    }
                }
            }

            versions.replaceAll((k, v) -> {
                if (v.size() == 1) {
                    return List.of(v.get(0));
                } else {
                    v.sort(Comparator.naturalOrder());
                    return List.of(v.get(0), v.get(v.size() - 1));
                }
            });

            return lookupFunction(versions);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static IntFunction<String> lookupFunction(NavigableMap<Integer, List<Version>> versions) {
        assert versions.values().stream().allMatch(vs -> vs.size() == 1 || vs.size() == 2)
            : "Version ranges have not been properly processed: " + versions;

        return id -> {
            List<Version> versionRange = versions.get(id);

            String lowerBound, upperBound;
            if (versionRange != null) {
                lowerBound = versionRange.get(0).toString();
                upperBound = lastItem(versionRange).toString();
            } else {
                var lowerRange = versions.lowerEntry(id);
                if (lowerRange != null) {
                    lowerBound = nextVersion(lastItem(lowerRange.getValue())).toString();
                } else {
                    @UpdateForV9
                    Version oldVersion = Version.fromId(id);
                    return oldVersion.toString();
                }

                var upperRange = versions.higherEntry(id);
                if (upperRange != null) {
                    upperBound = upperRange.getValue().get(0).toString();
                } else {
                    upperBound = "snapshot[" + id + "]";
                }
            }

            return lowerBound.equals(upperBound) ? lowerBound : lowerBound + "-" + upperBound;
        };
    }

    private static <T> T lastItem(List<T> list) {
        return list.get(list.size() - 1);
    }

    private static Version nextVersion(Version version) {
        return new Version(version.id + 100);   
    }
}
