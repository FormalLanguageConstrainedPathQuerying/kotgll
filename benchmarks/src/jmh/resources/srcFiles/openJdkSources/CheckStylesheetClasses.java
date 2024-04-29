/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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


/*
 * @test
 * @bug 8267574
 * @summary check stylesheet names against HtmlStyle
 * @modules jdk.javadoc/jdk.javadoc.internal.doclets.formats.html.markup
 *          jdk.javadoc/jdk.javadoc.internal.doclets.formats.html.resources:open
 */

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;

/**
 * This test compares the set of CSS class names defined in HtmlStyle
 * and other files (such as search.js) against the set of CSS class names
 * defined in the main stylesheet.css provided by the doclet.
 *
 * The goal is to detect "unexpected" discrepancies between the two sets.
 * "Expected" discrepancies are taken into account, but may indicate a
 * need to resolve the discrepancy.
 *
 * The test does not take into direct account the recent introduction of
 * CSS constructs like section {@code [class$="-details"]}
 */
public class CheckStylesheetClasses {
    public static void main(String... args) throws Exception {
        CheckStylesheetClasses c = new CheckStylesheetClasses();
        c.run();
    }

    int errors = 0;

    void run() throws Exception {
        Set<String> htmlStyleNames = getHtmlStyleNames();
        Set<String> styleSheetNames = getStylesheetNames();

        System.err.println("found " + htmlStyleNames.size() + " names in HtmlStyle");
        System.err.println("found " + styleSheetNames.size() + " names in stylesheet");


        try (BufferedWriter out = Files.newBufferedWriter(Path.of("htmlStyleNames.txt"));
                PrintWriter pw = new PrintWriter(out)) {
            htmlStyleNames.forEach(pw::println);
        }

        try (BufferedWriter out = Files.newBufferedWriter(Path.of("styleSheetNames.txt"));
             PrintWriter pw = new PrintWriter(out)) {
            styleSheetNames.forEach(pw::println);
        }


        htmlStyleNames.removeIf(s -> s.endsWith("-page") && !styleSheetNames.contains(s));

        htmlStyleNames.removeIf(s -> s.endsWith("-description") && !styleSheetNames.contains(s));

        htmlStyleNames.removeIf(s -> s.startsWith("help-") && !styleSheetNames.contains(s));

        htmlStyleNames.removeIf(s -> s.endsWith("-details") && !styleSheetNames.contains(s));
        htmlStyleNames.removeIf(s -> s.endsWith("-summary") && !styleSheetNames.contains(s));

        removeAll(htmlStyleNames, "annotations", "element-name", "extends-implements",
                "modifiers", "permits", "return-type");

        removeAll(htmlStyleNames, "col-plain", "external-link", "header",
                "hierarchy", "index", "package-uses", "packages", "permits-note",
                "serialized-package-container", "source-container");


        removeAll(styleSheetNames, "css", "png", "w3", "org");

        removeAll(styleSheetNames, "borderless", "plain", "striped");

        removeAll(styleSheetNames, "result-highlight", "result-item", "anchor-link",
                "search-tag-desc-result", "search-tag-holder-result", "page-search-header",
                "ui-autocomplete", "ui-autocomplete-category", "ui-state-active", "ui-menu",
                "ui-menu-item-wrapper", "ui-static-link", "expanded", "search-result-link",
                "two-column-search-results", "sort-asc", "sort-desc", "visible");

        styleSheetNames.remove("module-graph");
        styleSheetNames.remove("sealed-graph");

        boolean ok = check(htmlStyleNames, "HtmlStyle", styleSheetNames, "stylesheet")
                    & check(styleSheetNames, "stylesheet", htmlStyleNames, "HtmlStyle");

        if (!ok) {
            throw new Exception("differences found");
        }

        if (errors > 0) {
            throw new Exception(errors + " errors found");
        }
    }

    boolean check(Set<String> s1, String l1, Set<String> s2, String l2) {
        boolean equal = true;
        for (String s : s1) {
            if (!s2.contains(s)) {
                System.err.println("In " + l1 + " but not " + l2 + ": " + s);
                equal = false;
            }
        }
        return equal;
    }

    /**
     * Remove all the names from the set, giving a message for any that were not found.
     */
    void removeAll(Set<String> set, String... names) {
        for (String name : names) {
            if (!set.remove(name)) {
                error("name not found in set: " + name);
            }
        }
    }

    void error(String message) {
        System.err.println("error: " + message);
        errors++;
    }

    Set<String> getHtmlStyleNames() {
        return Arrays.stream(HtmlStyle.values())
                .map(HtmlStyle::cssName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    Set<String> getStylesheetNames() throws IOException {
        Set<String> names = new TreeSet<>();
        String stylesheet = "/jdk/javadoc/internal/doclets/formats/html/resources/stylesheet.css";
        URL url = HtmlStyle.class.getResource(stylesheet);
        readStylesheet(url, names);
        return names;
    }

    private void readStylesheet(URL resource, Set<String> names) throws IOException {
        try (InputStream in = resource.openStream()) {
            if (in == null) {
                throw new AssertionError("Cannot find or access resource " + resource);
            }
            String s = new String(in.readAllBytes())
                    .replaceAll("(?s)/\\*.*?\\*/", ""); 
            Pattern p = Pattern.compile("(?i)\\.(?<name>[a-z][a-z0-9-]+)\\b");
            Matcher m = p.matcher(s);
            while (m.find()) {
                names.add(m.group("name"));
            }
        }
    }
}
