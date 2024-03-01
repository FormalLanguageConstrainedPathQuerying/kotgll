/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.jdeps;

import static com.sun.tools.jdeps.JdepsFilter.DEFAULT_FILTER;
import static com.sun.tools.jdeps.Module.trace;
import static com.sun.tools.jdeps.Graph.*;

import java.lang.module.ModuleDescriptor.Requires;
import java.io.IOException;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Inverse transitive dependency analysis (compile-time view)
 */
public class InverseDepsAnalyzer extends DepsAnalyzer {
    private final Map<Archive, Set<Archive>> endPoints = new HashMap<>();
    private final Set<Archive> targets = new HashSet<>();

    public InverseDepsAnalyzer(JdepsConfiguration config,
                               JdepsFilter filter,
                               JdepsWriter writer,
                               Analyzer.Type verbose,
                               boolean apiOnly) {
        super(config, filter, writer, verbose, apiOnly);
    }

    public boolean run() throws IOException {
        try {
            if (apiOnly) {
                finder.parseExportedAPIs(rootArchives.stream());
            } else {
                finder.parse(rootArchives.stream());
            }
            archives.addAll(rootArchives);

            Set<Archive> archives = archives();


            if (filter.requiresFilter().isEmpty()) {
                targets.addAll(archives);
            } else {
                filter.requiresFilter().stream()
                      .map(configuration::findModule)
                      .flatMap(Optional::stream)
                      .forEach(targets::add);
            }

            if (filter.requiresFilter().isEmpty()) {
                Map<Archive, Set<Archive>> dependences = finder.dependences();
                targets.forEach(source -> endPoints.put(source, dependences.get(source)));
            } else {
                targets.forEach(t -> endPoints.put(t, Collections.emptySet()));
            }

            analyzer.run(archives, finder.locationToArchive());

            if (writer != null) {
                writer.generateOutput(archives, analyzer);
            }

        } finally {
            finder.shutdown();
        }
        return true;
    }

    /**
     * Returns the target archives determined from the dependency analysis.
     *
     * Inverse transitive dependency will find all nodes that depend
     * upon the returned set of archives directly and indirectly.
     */
    public Set<Archive> targets() {
        return Collections.unmodifiableSet(targets);
    }

    /**
     * Finds all inverse transitive dependencies using the given requires set
     * as the targets, if non-empty.  If the given requires set is empty,
     * use the archives depending the packages specified in -regex or -p options.
     */
    public Set<Deque<Archive>> inverseDependences() throws IOException {
        DependencyFinder dependencyFinder = new DependencyFinder(configuration, DEFAULT_FILTER);
        try {
            Stream<Archive> archives =
                Stream.concat(configuration.initialArchives().stream(),
                              configuration.classPathArchives().stream());
            if (apiOnly) {
                dependencyFinder.parseExportedAPIs(archives);
            } else {
                dependencyFinder.parse(archives);
            }

            Graph.Builder<Archive> builder = new Graph.Builder<>();
            targets().forEach(builder::addNode);

            configuration.getModules().values()
                .forEach(m -> {
                    builder.addNode(m);
                    m.descriptor().requires().stream()
                        .filter(req -> !req.modifiers().contains(Requires.Modifier.STATIC)
                            || configuration.findModule(req.name()).isPresent())
                            .map(Requires::name)
                            .map(configuration::findModule)  
                            .forEach(v -> builder.addEdge(v.get(), m));
                });

            Map<Archive, Set<Archive>> dependences = dependencyFinder.dependences();
            dependences.entrySet()
                .forEach(e -> {
                    Archive u = e.getKey();
                    builder.addNode(u);
                    e.getValue().forEach(v -> builder.addEdge(v, u));
                });

            Graph<Archive> graph = builder.build();
            trace("targets: %s%n", targets());

            return targets().stream()
                .map(t -> findPaths(graph, t))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        } finally {
            dependencyFinder.shutdown();
        }
    }

    /**
     * Returns all paths reachable from the given targets.
     */
    private Set<Deque<Archive>> findPaths(Graph<Archive> graph, Archive target) {

        Deque<Archive> path = new LinkedList<>();
        path.push(target);

        Set<Edge<Archive>> visited = new HashSet<>();

        Deque<Edge<Archive>> deque = new LinkedList<>();
        deque.addAll(graph.edgesFrom(target));
        if (deque.isEmpty()) {
            return makePaths(path).collect(Collectors.toSet());
        }

        Set<Deque<Archive>> allPaths = new HashSet<>();
        while (!deque.isEmpty()) {
            Edge<Archive> edge = deque.pop();

            if (visited.contains(edge))
                continue;

            Archive node = edge.v;
            path.addLast(node);
            visited.add(edge);

            Set<Edge<Archive>> unvisitedDeps = graph.edgesFrom(node)
                    .stream()
                    .filter(e -> !visited.contains(e))
                    .collect(Collectors.toSet());

            trace("visiting %s %s (%s)%n", edge, path, unvisitedDeps);
            if (unvisitedDeps.isEmpty()) {
                makePaths(path).forEach(allPaths::add);
                path.removeLast();
            }

            unvisitedDeps.forEach(deque::push);


            while (!path.isEmpty()) {
                if (visited.containsAll(graph.edgesFrom(path.peekLast())))
                    path.removeLast();
                else
                    break;
            }
        }

       return allPaths;
    }

    /**
     * Prepend end point to the path
     */
    private Stream<Deque<Archive>> makePaths(Deque<Archive> path) {
        Set<Archive> nodes = endPoints.get(path.peekFirst());
        if (nodes == null || nodes.isEmpty()) {
            return Stream.of(new LinkedList<>(path));
        } else {
            return nodes.stream().map(n -> {
                Deque<Archive> newPath = new LinkedList<>();
                newPath.addFirst(n);
                newPath.addAll(path);
                return newPath;
            });
        }
    }
}
