/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8266666 8275788 8276964
 * @summary Implementation for snippets
 * @library /tools/lib ../../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.* toolbox.ToolBox toolbox.ModuleBuilder builder.ClassBuilder
 * @run main TestSnippetTag
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;

import builder.ClassBuilder;
import builder.ClassBuilder.MethodBuilder;
import toolbox.ModuleBuilder;


/*
 * General notes
 * =============
 *
 * To simplify maintenance of this test suite, a test name uses a convention.
 * By convention, a test name is a concatenation of the following parts:
 *
 *    1. "test"
 *    2. ("Positive", "Negative")
 *    3. ("Inline", "External", "Hybrid")
 *    4. ("Tag", "Markup")
 *    5. <custom string>
 *
 * A test can be either positive or negative; it cannot be both or neither.
 * A test can exercise inline, external or hybrid variant or any combination
 * thereof, including none at all. A test can exercise tag syntax, markup syntax
 * or both.
 *
 * 1. Some of the below tests could benefit from using a combinatorics library
 * as they are otherwise very wordy.
 *
 * 2. One has to be careful when using JavadocTester.checkOutput with duplicating
 * strings. If JavadocTester.checkOutput(x) is true, then it will also be true
 * if x is passed to that method additionally N times: JavadocTester.checkOutput(x, x, ..., x).
 * This is because a single occurrence of x in the output will be matched N times.
 */
public class TestSnippetTag extends SnippetTester {

    public static void main(String... args) throws Exception {
        new TestSnippetTag().runTests();
    }

    /*
     * Make sure the "id" and "lang" attributes defined in JEP 413 are translated
     * to HTML. In particular, verify that the "lang" attribute is translated
     * to a value added to the "class" attribute as recommended by the HTML5 specification:
     * https:
     */
    @Test
    public void testPositiveInlineTag_IdAndLangAttributes(Path base) throws IOException {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");

        record SnippetAttributes(String content, String id, String lang) { }

        final var snippets = List.of(
                new SnippetAttributes("""
                    {@snippet id="foo1" :
                        Hello, Snippet!
                    }
                    """, "foo1", null),
                new SnippetAttributes("""
                    {@snippet id="foo2":
                        Hello, Snippet!
                    }
                    """, "foo2", null),
                new SnippetAttributes("""
                    {@snippet id='foo3' :
                        Hello, Snippet!
                    }
                    """, "foo3", null),
                new SnippetAttributes("""
                    {@snippet id='foo4':
                        Hello, Snippet!
                    }
                    """, "foo4", null),
                new SnippetAttributes("""
                    {@snippet id=foo5 :
                        Hello, Snippet!
                    }
                    """, "foo5", null),

                new SnippetAttributes("""
                    {@snippet id="" :
                        Hello, Snippet!
                    }
                    """, null, null),
                new SnippetAttributes("""
                    {@snippet id="":
                        Hello, Snippet!
                    }
                    """, null, null),
                new SnippetAttributes("""
                    {@snippet id='':
                        Hello, Snippet!
                    }
                    """, null, null),
                new SnippetAttributes("""
                    {@snippet id=:
                        Hello, Snippet!
                    }
                    """, null, null),
                new SnippetAttributes("""
                    {@snippet lang="java" :
                        Hello, Snippet!
                    }
                    """, null, "java"),
                new SnippetAttributes("""
                    {@snippet lang="java":
                        Hello, Snippet!
                    }
                    """, null, "java"),
                new SnippetAttributes("""
                    {@snippet lang='java' :
                        Hello, Snippet!
                    }
                    """, null, "java"),
                new SnippetAttributes("""
                    {@snippet lang='java':
                        Hello, Snippet!
                    }
                    """, null, "java"),
                new SnippetAttributes("""
                    {@snippet lang=java :
                        Hello, Snippet!
                    }
                    """, null, "java"),
                new SnippetAttributes("""
                    {@snippet lang="properties" :
                        Hello, Snippet!
                    }
                    """, null, "properties"),
                new SnippetAttributes("""
                    {@snippet lang="text" :
                        Hello, Snippet!
                    }
                    """, null, "text"),
                new SnippetAttributes("""
                    {@snippet lang="" :
                        Hello, Snippet!
                    }
                    """, null, null),
                new SnippetAttributes("""
                    {@snippet lang="foo" id="bar" :
                        Hello, Snippet!
                    }
                    """, "bar", "foo")
        );
        ClassBuilder classBuilder = new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class");
        forEachNumbered(snippets, (s, i) -> {
            classBuilder.addMembers(
                    MethodBuilder.parse("public void case%s() { }".formatted(i))
                            .setComments("A method.", s.content()));
        });
        classBuilder.write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.OK);
        checkLinks();
        for (int j = 0; j < snippets.size(); j++) {
            var attr = snippets.get(j);
            var snippetHtml = getSnippetHtmlRepresentation("pkg/A.html", "    Hello, Snippet!\n",
                    Optional.ofNullable(attr.lang()), Optional.ofNullable(attr.id()));
            checkOutput("pkg/A.html", true,
                        """
                        <span class="element-name">case%s</span>()</div>
                        <div class="block">A method.
                         \s
                        %s
                        """.formatted(j, snippetHtml));
        }
    }

    /*
     * If the "lang" attribute is absent in the snippet tag for an external snippet,
     * then the "class" attribute is derived from the snippet source file extension.
     *
     * If the "class" attribute can be derived both from the "lang" attribute and
     * the file extension, then it is derived from the "lang" attribute.
     */
    @Test
    public void testPositiveInlineExternalTagMarkup_ImplicitAttributes(Path base) throws IOException {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");

        ClassBuilder classBuilder = new ClassBuilder(tb, "com.example.Cls")
                .setModifiers("public", "class");
        classBuilder.setComments("""
                {@snippet class="Snippets" region="code" id="snippet1"}
                {@snippet file="Snippets.java" region="code" id="snippet2"}
                {@snippet class="Snippets" region="code" id="snippet3" lang="none"}
                {@snippet file="Snippets.java" region="code" id="snippet4" lang="none"}
                {@snippet class="Snippets" region="code" id="snippet5" lang=""}
                {@snippet file="Snippets.java" region="code" id="snippet6" lang=""}
                {@snippet file="user.properties" id="snippet7"}
                {@snippet file="user.properties" id="snippet8" lang="none"}
                {@snippet file="user.properties" id="snippet9" lang=""}
                """);
        addSnippetFile(srcDir, "com.example", "Snippets.java", """
                public class Snippets {
                    public static void printMessage(String msg) {
                        System.out.println(msg);
                    }
                }
                """);
        addSnippetFile(srcDir, "com.example", "user.properties", """
                user=jane
                home=/home/jane
                """);
        classBuilder.write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "com.example");
        checkExit(Exit.OK);
        checkLinks();
        final var javaContent = """
                System.out.println(msg);
                """;
        final var propertiesContent = """
                user=jane
                home=/home/jane
                """;
        checkOutput("com/example/Cls.html", true,
                getSnippetHtmlRepresentation("com/example/Cls.html", javaContent, Optional.of("java"), Optional.of("snippet1")),
                getSnippetHtmlRepresentation("com/example/Cls.html", javaContent, Optional.of("java"), Optional.of("snippet2")),
                getSnippetHtmlRepresentation("com/example/Cls.html", javaContent, Optional.of("none"), Optional.of("snippet3")),
                getSnippetHtmlRepresentation("com/example/Cls.html", javaContent, Optional.of("none"), Optional.of("snippet4")),
                getSnippetHtmlRepresentation("com/example/Cls.html", javaContent, Optional.empty(), Optional.of("snippet5")),
                getSnippetHtmlRepresentation("com/example/Cls.html", javaContent, Optional.empty(), Optional.of("snippet6")),
                getSnippetHtmlRepresentation("com/example/user.properties", propertiesContent, Optional.of("properties"), Optional.of("snippet7")),
                getSnippetHtmlRepresentation("com/example/user.properties", propertiesContent, Optional.of("none"), Optional.of("snippet8")),
                getSnippetHtmlRepresentation("com/example/user.properties", propertiesContent, Optional.empty(), Optional.of("snippet9")));
    }

    @Test
    public void testNegativeInlineTag_BadTagSyntax(Path base) throws IOException {

        class Capture {
            static final AtomicInteger counter = new AtomicInteger();

            record TestCase(String input, String expectedError) { }

            void testErrors(List<TestCase> testCases) throws IOException {
                List<String> inputs = testCases.stream().map(s -> s.input).toList();
                StringBuilder methods = new StringBuilder();
                forEachNumbered(inputs, (i, n) -> {
                    methods.append(
                        """

                        /**
                        %s*/
                        public void case%s() {}
                        """.formatted(i, n));
                });

                String classString =
                    """
                    public class A {
                    %s
                    }
                    """.formatted(methods.toString());

                String suffix = String.valueOf(counter.incrementAndGet());

                Path src = Files.createDirectories(base.resolve("src" + suffix));
                tb.writeJavaFiles(src, classString);

                javadoc("-d", base.resolve("out" + suffix).toString(),
                    "-sourcepath", src.toString(),
                    src.resolve("A.java").toString());
                checkExit(Exit.ERROR);
                checkOrder(Output.OUT, testCases.stream().map(TestCase::expectedError).toArray(String[]::new));
                checkNoCrashes();
            }
        }

        new Capture().testErrors(List.of(
            new Capture.TestCase(
                """
                {@snippet :}
                """,
                """
                error: unexpected content
                {@snippet :}
                          ^
                """),
            new Capture.TestCase(
                """
                {@snippet : }
                """,
                """
                error: unexpected content
                {@snippet : }
                          ^
                """),
            new Capture.TestCase(
                """
                {@snippet :a}
                """,
                """
                error: unexpected content
                {@snippet :a}
                          ^
                """),
            new Capture.TestCase(
                """
                {@snippet
                :}
                """,
                """
                error: unexpected content
                :}
                ^
                """),
            new Capture.TestCase(
                """
                {@snippet
                : }
                """,
                """
                error: unexpected content
                : }
                ^
                """),
            new Capture.TestCase(
                """
                {@snippet
                :a}
                """,
                """
                error: unexpected content
                :a}
                ^
                """),
            new Capture.TestCase(
                """
                {@snippet
                 :}
                """,
                """
                error: unexpected content
                 :}
                 ^
                """),
            new Capture.TestCase(
                """
                {@snippet
                 : }
                """,
                """
                error: unexpected content
                 : }
                 ^
                """),
            new Capture.TestCase(
                """
                {@snippet
                 :a}
                """,
                """
                error: unexpected content
                 :a}
                 ^
                """),
            new Capture.TestCase(
                """
                {@snippet file="}
                """,
                """
                error: no content
                {@snippet file="}
                                ^
                """),
            new Capture.TestCase(
                """
                {@snippet file="
                }
                """,
                """
                error: no content
                }
                ^
                """),
            new Capture.TestCase(
                """
                {@snippet file='}
                """,
                """
                error: no content
                {@snippet file='}
                                ^
                """),
            new Capture.TestCase(
                """
                {@snippet file='
                }
                """,
                """
                error: no content
                }
                ^
                """),
            new Capture.TestCase(
                """
                {@snippet file='
                    }
                """,
                """
                error: no content
                    }
                    ^
                """),
            new Capture.TestCase(
                """
                {@snippet
                file='
                    }
                """,
                """
                error: no content
                    }
                    ^
                """),
            new Capture.TestCase(
                """
                {@snippet
                file='}
                """,
                """
                error: no content
                file='}
                      ^
                """),
            new Capture.TestCase(
                """
                {@snippet file=}
                """,
                """
                error: illegal value for attribute "file": ""
                {@snippet file=}
                          ^
                """),
            new Capture.TestCase(
                """
                {@snippet file=:
                }
                """,
                """
                error: illegal value for attribute "file": ""
                {@snippet file=:
                          ^
                """)
        ));


        new Capture().testErrors(List.of(
            new Capture.TestCase(
                """
                {@snippet file=:}
                """,
                """
                error: unexpected content
                {@snippet file=:}
                               ^
                """),
            new Capture.TestCase(
                """
                {@snippet
                """,
                """
                error: no content
                {@snippet
                        ^
                """),
            new Capture.TestCase(
                """
                {@snippet file
                """,
                """
                error: no content
                {@snippet file
                             ^
                """),
            new Capture.TestCase(
                """
                {@snippet file=
                """,
                """
                error: no content
                {@snippet file=
                              ^
                """),
            new Capture.TestCase(
                """
                {@snippet file="
                """,
                """
                error: no content
                {@snippet file="
                               ^
                """),
            new Capture.TestCase(
                """
                {@snippet file='
                """,
                """
                error: no content
                {@snippet file='
                               ^
                """),
            new Capture.TestCase(
                """
                {@snippet :""",
                """
                error: no content
                {@snippet :*/
                          ^
                """),
            new Capture.TestCase(
                """
                {@snippet :
                    Hello, World!""",
                """
                error: unterminated inline tag
                    Hello, World!*/
                                ^
                """),
            new Capture.TestCase(
                """
                {@snippet file="gibberish" :\
                """,
                """
                error: no content
                {@snippet file="gibberish" :*/
                                           ^
                """),
            new Capture.TestCase(
                """
                {@snippet file="gibberish" :
                """,
                """
                error: unterminated inline tag
                {@snippet file="gibberish" :
                                           ^
                """)
        ));
    }

    /*
     * A colon that is not separated from a tag name by whitespace is considered
     * a part of that name. This behavior is historical. For more context see,
     * for example, JDK-4750173.
     */
    @Test
    public void testNegativeInlineTagUnknownTag(Path base) throws IOException {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        final var unknownTags = List.of(
                """
                {@snippet:}
                """,
                """
                {@snippet:
                }
                """
        );
        ClassBuilder classBuilder = new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class");
        forEachNumbered(unknownTags, (s, i) -> {
            classBuilder.addMembers(
                    MethodBuilder.parse("public void case%s() { }".formatted(i))
                            .setComments(s));
        });
        classBuilder.write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        long actual = Pattern.compile("error: unknown tag. Mistyped @snippet")
                .matcher(getOutput(Output.OUT)).results().count();
        checking("Number of errors");
        int expected = unknownTags.size();
        if (actual == expected) {
            passed("");
        } else {
            failed(actual + " vs " + expected);
        }
        checkNoCrashes();
    }

    @Test
    public void testPositiveInlineTag(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");

        record TestCase(String input, String expectedOutput) { }

        final var testCases = List.of(
                new TestCase("""
                             {@snippet :
                             }
                             """,
                             """
                             """),
                new TestCase("""
                             {@snippet
                             :
                             }
                             """,
                             """
                             """),
                new TestCase("""
                             {@snippet
                                       :
                             }
                             """,
                             """
                             """),
                new TestCase("""
                             {@snippet    \s
                             :
                             }
                             """,
                             """
                             """),
                new TestCase("""
                             {@snippet :
                                 Hello, Snippet!
                             }
                             """,
                             """
                                 Hello, Snippet!
                             """),
                new TestCase("""
                             {@snippet       :
                                 Hello, Snippet!
                             }
                             """,
                             """
                                 Hello, Snippet!
                             """),
                new TestCase("""
                             {@snippet :      \s
                                 Hello, Snippet!
                             }
                             """,
                             """
                                 Hello, Snippet!
                             """),
                new TestCase("""
                             {@snippet  attr1="val1"    :
                                 Hello, Snippet!
                             }
                             """,
                             """
                                 Hello, Snippet!
                             """),
                new TestCase("""
                             {@snippet :
                                 Hello
                                 ,
                                  Snippet!
                             }
                             """,
                             """
                                 Hello
                                 ,
                                  Snippet!
                             """),
                new TestCase("""
                             {@snippet :

                                 Hello
                                 ,
                                  Snippet!
                             }
                             """,
                             """

                                 Hello
                                 ,
                                  Snippet!
                             """),
                new TestCase("""
                             {@snippet :
                                 Hello
                                 ,
                                  Snippet!

                             }
                             """,
                             """
                                 Hello
                                 ,
                                  Snippet!

                             """),
                new TestCase("""
                             {@snippet :
                                 Hello
                                 ,
                                  Snippet!
                                 }
                             """,
                             """
                             Hello
                             ,
                              Snippet!
                             """
                ),
                new TestCase("""
                             {@snippet :
                                 Hello
                                 ,
                                  Snippet!}
                             """,
                             """
                             Hello
                             ,
                              Snippet!"""),
                new TestCase("""
                             {@snippet :
                                 Hello
                                 ,    \s
                                  Snippet!
                             }
                             """,
                             """
                                 Hello
                                 ,
                                  Snippet!
                             """),
                new TestCase("""
                             {@snippet :
                                 \\b\\t\\n\\f\\r\\"\\'\\\
                                 Hello\\
                                 ,\\s
                                  Snippet!
                             }
                             """,
                             """
                                 \\b\\t\\n\\f\\r\\"\\'\\    Hello\\
                                 ,\\s
                                  Snippet!
                             """),
                new TestCase("""
                             {@snippet :
                                 </pre>
                                     <!-- comment -->
                                 <b>&trade;</b> &#8230; " '
                             }
                             """,
                             """
                                 &lt;/pre&gt;
                                     &lt;!-- comment --&gt;
                                 &lt;b&gt;&amp;trade;&lt;/b&gt; &amp;#8230; " '
                             """)
        );
        ClassBuilder classBuilder = new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class");
        forEachNumbered(testCases, (t, id) -> {
            classBuilder
                    .addMembers(
                            MethodBuilder
                                    .parse("public void case%s() { }".formatted(id))
                                    .setComments(t.input()));
        });
        classBuilder.write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.OK);
        forEachNumbered(testCases, (t, id) -> {
            checkOutput("pkg/A.html", true,
                        """
                        <span class="element-name">case%s</span>()</div>
                        <div class="block">
                        %s""".formatted(id, getSnippetHtmlRepresentation("pkg/A.html", t.expectedOutput())));
        });
    }

    @Test
    public void testPositiveExternalTag_File(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");

        record TestCase(String input, Function<String, String> expectedTransformation) {

            TestCase(String input) {
                this(input, Function.identity());
            }
        }

        final var testCases = List.of(
                new TestCase("""
                             Hello, Snippet!
                             """),
                new TestCase("""
                                 Hello, Snippet!
                             """),
                new TestCase("""
                                 Hello
                                 ,
                                  Snippet!
                             """),
                new TestCase("""

                                 Hello
                                 ,
                                  Snippet!
                             """),
                new TestCase("""
                                 Hello
                                 ,
                                  Snippet!

                             """),
                new TestCase("""
                                 Hello
                                 ,        \s
                                  Snippet!
                             """,
                             String::stripIndent),
                new TestCase("""
                             Hello
                             ,
                              Snippet!"""),
                new TestCase("""
                                 \\b\\t\\n\\f\\r\\"\\'\\\
                                 Hello\\
                                 ,\\s
                                  Snippet!
                             """),
                new TestCase("""
                                 </pre>
                                     <!-- comment -->
                                 <b>&trade;</b> &#8230; " '
                             """,
                             s -> s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")),
                new TestCase("""
                                 &lt;/pre&gt;
                                     &lt;!-- comment --&gt;
                                 &lt;b&gt;&amp;trade;&lt;/b&gt; &amp;#8230; " '
                             """,
                             s -> s.replaceAll("&", "&amp;"))
        );
        ClassBuilder classBuilder = new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class");
        forEachNumbered(testCases, (t, id) -> {
            classBuilder
                    .addMembers(
                            MethodBuilder
                                    .parse("public void case%s() { }".formatted(id))
                                    .setComments("""
                                                 {@snippet file="%s.txt"}
                                                 """.formatted(id)));
            addSnippetFile(srcDir, "pkg", "%s.txt".formatted(id), t.input());
        });
        classBuilder.write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.OK);
        forEachNumbered(testCases, (testCase, index) -> {
            String expectedOutput = testCase.expectedTransformation().apply(testCase.input());
            checkOutput("pkg/A.html", true,
                        """
                        <span class="element-name">case%s</span>()</div>
                        <div class="block">
                        %s""".formatted(index, getSnippetHtmlRepresentation("pkg/A.html", expectedOutput)));
        });
    }

    @Test
    public void testPositiveInlineTag_InDocFiles(Path base) throws IOException {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void m() { }")
                                .setComments("<a href=\"doc-files/file.html\">A document</a>"))
                .write(srcDir);
        var content = """
                              Unlike Java files, HTML files don't mind hosting
                              the */ sequence in a @snippet tag
                      """;
        String html = """
                      <!DOCTYPE html>
                      <html lang="en">
                        <head>
                          <meta charset="utf-8">
                          <title>title</title>
                        </head>
                        <body>
                          <!-- yet another user entry point to the html file (not used by test): through an index page -->
                          {@index this A document}
                          {@snippet :
                              %s}
                        </body>
                      </html>
                      """.formatted(content);
        Path p = Files.createDirectories(srcDir.resolve("pkg").resolve("doc-files"));
        Files.writeString(p.resolve("file.html"), html, StandardOpenOption.CREATE_NEW);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.OK);
        checkOutput("pkg/doc-files/file.html", true, content);
    }

    @Test
    public void testPositiveExternalTag_InDocFiles(Path base) throws IOException {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void m() { }")
                                .setComments("<a href=\"doc-files/file.html\">A document</a>"))
                .write(srcDir);
        String html = """
                      <!DOCTYPE html>
                      <html lang="en">
                        <head>
                          <meta charset="utf-8">
                          <title>title</title>
                        </head>
                        <body>
                          <!-- yet another user entry point to the html file (not used by test): through an index page -->
                          {@index this A document}
                          {@snippet file="file.txt"}
                        </body>
                      </html>
                      """;
        Path p = Files.createDirectories(srcDir.resolve("pkg").resolve("doc-files"));
        Files.writeString(p.resolve("file.html"), html, StandardOpenOption.CREATE_NEW);
        String content = """
                            Unlike Java files, text files don't mind hosting
                            the */ sequence in a @snippet tag
                         """;
        addSnippetFile(srcDir, "pkg", "file.txt", content);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.OK);
        checkOutput("pkg/doc-files/file.html", true, content);
    }

    @Test
    public void testNegativeExternalTag_FileNotFound(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        var fileName = "text.txt";
        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void test() { }")
                                .setComments("""
                                             {@snippet file="%s"}
                                             """.formatted(fileName)))
                .write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:4: error: file not found on source path or snippet path: %s""".formatted(fileName));
        checkOutput("pkg/A.html", true, """
                        <details class="invalid-tag">
                        <summary>invalid @snippet</summary>
                        <pre>file not found on source path or snippet path: text.txt</pre>
                        </details>
                        """);
        checkNoCrashes();
    }

    @Test 
    public void testNegativeExternalTag_FileModuleSourcePath(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        var fileName = "snippet.txt";
        String MODULE_NAME = "mdl1";
        String PACKAGE_NAME = "pkg1.pkg2";
        Path moduleDir = new ModuleBuilder(tb, MODULE_NAME)
                .exports(PACKAGE_NAME)
                .write(srcDir);
        new ClassBuilder(tb, String.join(".", PACKAGE_NAME, "A"))
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void test() { }")
                                .setComments("""
                                             {@snippet file="%s"}
                                             """.formatted(fileName)))
                .write(moduleDir);
        addSnippetFile(moduleDir, PACKAGE_NAME, fileName, "content");
        javadoc("-d", outDir.toString(),
                "--module-source-path", srcDir.toString(),
                "--module", MODULE_NAME);
        checkExit(Exit.OK);
    }

    @Test 
    public void testNegativeExternalTag_FileNotFoundModuleSourcePath(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        var fileName = "text.txt";
        var MODULE_NAME = "mdl1";
        var PACKAGE_NAME = "pkg1.pkg2";
        Path moduleDir = new ModuleBuilder(tb, MODULE_NAME)
                .exports(PACKAGE_NAME)
                .write(srcDir);
        new ClassBuilder(tb, String.join(".", PACKAGE_NAME, "A"))
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void test() { }")
                                .setComments("""
                                             {@snippet file="%s"}
                                             """.formatted(fileName)))
                .write(moduleDir);
        javadoc("-d", outDir.toString(),
                "--module-source-path", srcDir.toString(),
                "--module", MODULE_NAME);
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:4: error: file not found on source path or snippet path: %s""".formatted(fileName));
        checkNoCrashes();
    }

    @Test
    public void testNegativeTag_NoContents(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        new ClassBuilder(tb, "pkg.A")
                .setComments("""
                             {@snippet}
                             """)
                .setModifiers("public", "class")
                .write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:3: error: @snippet does not specify contents""");
        checkOutput("pkg/A.html", true, """
                        <details class="invalid-tag">
                        <summary>invalid @snippet</summary>
                        <pre>@snippet does not specify contents</pre>
                        </details>
                        """);
        checkNoCrashes();
    }

    @Test
    public void testNegativeExternalTagMarkup(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        addSnippetFile(srcDir, "pkg", "file.txt", """
                                                  """
        );
        ClassBuilder classBuilder = new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void case0() { }")
                                .setComments("""
                                             {@snippet file="file.txt"}
                                             """));
        classBuilder.write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
"""
: error: snippet markup: missing attribute "region"
    ^""");
        checkNoCrashes();
    }

    @Test
    public void testNegativeInlineTag_AttributeConflict20(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        new ClassBuilder(tb, "pkg.A")
                .setComments("""
                             {@snippet file="" class="" :
                                 Hello, Snippet!
                             }
                             """)
                .setModifiers("public", "class")
                .write(srcDir);

        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:3: error: @snippet specifies multiple external contents, which is ambiguous""");
        checkOutput("pkg/A.html", true, """
                        <details class="invalid-tag">
                        <summary>invalid @snippet</summary>
                        <pre>@snippet specifies multiple external contents, which is ambiguous</pre>
                        </details>
                        """);
        checkNoCrashes();
    }

    @Test
    public void testNegativeInlineTag_AttributeConflict30(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        new ClassBuilder(tb, "pkg.A")
                .setComments("""
                             {@snippet class="" file="" :
                                 Hello, Snippet!
                             }
                             """)
                .setModifiers("public", "class")
                .write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutputEither(Output.OUT,
                          """
                          A.java:3: error: @snippet specifies multiple external contents, which is ambiguous""");
        checkNoCrashes();
    }

    @Test
    public void testNegativeInlineTag_AttributeConflict60(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        new ClassBuilder(tb, "pkg.A")
                .setComments("""
                             {@snippet file="" file=""}
                             """)
                .setModifiers("public", "class")
                .write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:3: error: repeated attribute: "file\"""");
        checkNoCrashes();
    }

    @Test
    public void testNegativeInlineTag_AttributeConflict70(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        new ClassBuilder(tb, "pkg.A")
                .setComments("""
                             {@snippet class="" class="" }
                             """)
                .setModifiers("public", "class")
                .write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:3: error: repeated attribute: "class\"""");
        checkNoCrashes();
    }

    @Test
    public void testNegativeInlineTag_AttributeConflict80(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        new ClassBuilder(tb, "pkg.A")
                .setComments("""
                             {@snippet class="" class="" :
                                 Hello, Snippet!
                             }
                             """)
                .setModifiers("public", "class")
                .write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutputEither(Output.OUT,
                          """
                          A.java:3: error: repeated attribute: "class\"""",
                          """
                          A.java:3: error: @snippet specifies external and inline contents, which is ambiguous""");
        checkNoCrashes();
    }

    @Test
    public void testNegativeInlineTag_AttributeConflict90(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        new ClassBuilder(tb, "pkg.A")
                .setComments("""
                             {@snippet file="" file="" :
                                 Hello, Snippet!
                             }
                             """)
                .setModifiers("public", "class")
                .write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutputEither(Output.OUT,
                          """
                          A.java:3: error: repeated attribute: "file\"""",
                          """
                          A.java:3: error: @snippet specifies external and inline contents, which is ambiguous""");
        checkNoCrashes();
    }

    @Test
    public void testNegativeTag_PositionResolution(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        new ClassBuilder(tb, "pkg.A")
                .setComments("""
                             {@snippet} {@snippet}
                             """)
                .setModifiers("public", "class")
                .write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:3: error: @snippet does not specify contents
                     * {@snippet} {@snippet}
                       ^
                    """,
                    """
                    A.java:3: error: @snippet does not specify contents
                     * {@snippet} {@snippet}
                                  ^
                    """);
        checkNoCrashes();
    }

    @Test
    public void testPositiveInlineTag_AttributeConflictRegion(Path base) throws Exception {
        record TestCase(Snippet snippet, String expectedOutput) { }
        final var testCases = List.of(
                new TestCase(newSnippetBuilder()
                                     .body("""
                                           Hello
                                           ,
                                            Snippet!
                                           """)
                                     .region("here")
                                     .build(),
                             """
                             Hello
                             ,
                              Snippet!
                             """
                )
                ,
                new TestCase(newSnippetBuilder()
                                     .body("""
                                               Hello
                                               ,
                                                Snippet!
                                               """)
                                     .region("here")
                                     .build(),
                             """
                                 Hello
                                 ,
                                  Snippet!
                             """)
                ,
                new TestCase(newSnippetBuilder()
                                     .body("""
                                               Hello
                                               ,
                                                Snippet!
                                           """)
                                     .region("here")
                                     .build(),
                             """
                             Hello
                             ,
                              Snippet!\
                             """
                )
                ,
                new TestCase(newSnippetBuilder()
                                     .body("""

                                               Hello
                                               ,
                                                Snippet!
                                                  """)
                                     .region("here")
                                     .build(),
                             """
                             Hello
                             ,
                              Snippet!
                             """
                )
                ,
                new TestCase(newSnippetBuilder()
                                     .body("""
                                               This is the only line you should see.
                                               You should NOT see this.
                                               """)
                                     .region("here")
                                     .build(),
                             """
                                 This is the only line you should see.
                             """
                )
                ,
                new TestCase(newSnippetBuilder()
                                     .body("""
                                               You should NOT see this.
                                               This is the only line you should see.
                                               """)
                                     .region("hereafter")
                                     .build(),
                             """
                                 This is the only line you should see.
                             """
                )
                ,
                new TestCase(newSnippetBuilder()
                                     .body("""
                                               You should NOT see this.
                                               This is the only line you should see.
                                               """)
                                     .region("before")
                                     .build(),
                             """
                                 This is the only line you should see.
                             """
                )
                ,
                new TestCase(newSnippetBuilder()
                                     .body("""
                                               This is the only line you should see.
                                               You should NOT see this.
                                               """)
                                     .region("beforehand")
                                     .build(),
                             """
                                 This is the only line you should see.
                             """
                )
        );
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        ClassBuilder classBuilder = new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class");
        forEachNumbered(testCases, (t, id) -> {
            var snippet = t.snippet();
            classBuilder
                    .addMembers(
                            MethodBuilder
                                    .parse("public void case%s() { }".formatted(id))
                                    .setComments("""
                                                 {@snippet region="%s" :
                                                 %s}
                                                 """.formatted(snippet.regionName(), snippet.body())));
        });
        classBuilder.write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.OK);
        forEachNumbered(testCases, (t, index) -> {
            checkOutput("pkg/A.html", true,
                        """
                        <span class="element-name">case%s</span>()</div>
                        <div class="block">
                        %s""".formatted(index, getSnippetHtmlRepresentation("pkg/A.html", t.expectedOutput())));
        });
    }

    private static Snippet.Builder newSnippetBuilder() {
        return new Snippet.Builder();
    }

    private record Snippet(String regionName, String body, String fileContent) {

        static class Builder {

            private String regionName;
            private String body;
            private String fileContent;

            Builder region(String name) {
                this.regionName = name;
                return this;
            }

            Builder body(String content) {
                this.body = content;
                return this;
            }

            Builder fileContent(String fileContent) {
                this.fileContent = fileContent;
                return this;
            }

            Snippet build() {
                return new Snippet(regionName, body, fileContent);
            }
        }
    }

    @Test
    public void testNegativeInlineTagMarkup_AttributeValueSyntaxUnquotedCurly(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        /*
         * The snippet region attribute's value is empty because the tag is
         * terminated by the first }
         *
         *    v                v
         *    {@snippet region=} :
         *        
         *    }
         */
        ClassBuilder classBuilder = new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void case0() { }")
                                .setComments("""
                                             {@snippet region=} :
                                             }
                                             """));
        classBuilder.write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:4: error: @snippet does not specify contents""");
        checkNoCrashes();
    }

    @Test
    public void testPositiveInlineTagMarkup_SyntaxCurly(Path base) throws Exception {
        /*
         * The snippet has to be external, otherwise its content would
         * interfere with the test: that internal closing curly would
         * terminate the @snippet tag:
         *
         *     v
         *     {@snippet region="}" :
         *         
         *                           ^
         *     }
         */
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        addSnippetFile(srcDir, "pkg", "file.txt", """
                                                  """
        );
        ClassBuilder classBuilder = new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void case0() { }")
                                .setComments("""
                                             {@snippet region="}" file="file.txt"}
                                             """))
                .addMembers(
                        MethodBuilder
                                .parse("public void case1() { }")
                                .setComments("""
                                             {@snippet region='}' file="file.txt"}
                                             """));
        classBuilder.write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.OK);
        checkOutput("pkg/A.html", true,
                    """
                    <span class="element-name">case0</span>()</div>
                    <div class="block">
                    """ + getSnippetHtmlRepresentation("pkg/A.html", ""));
        checkOutput("pkg/A.html", true,
                    """
                    <span class="element-name">case1</span>()</div>
                    <div class="block">
                    """ + getSnippetHtmlRepresentation("pkg/A.html", ""));
    }

    @Test 
    public void testPositiveExternalTagMarkup_AttributeValueSyntax(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        final var snippets = List.of(
                """
                {@snippet file=file region=region}
                """,
                """
                {@snippet file=file region= region}
                """,
                """
                {@snippet file=file region="region"}
                """,
                """
                {@snippet file=file region='region'}
                """,
                """
                {@snippet file= file region=region}
                """,
                """
                {@snippet file= file region= region}
                """,
                """
                {@snippet file= file region="region"}
                """,
                """
                {@snippet file= file region='region'}
                """,
                """
                {@snippet file="file" region=region}
                """,
                """
                {@snippet file="file" region= region}
                """,
                """
                {@snippet file="file" region="region"}
                """,
                """
                {@snippet file="file" region='region'}
                """,
                """
                {@snippet file='file' region=region}
                """,
                """
                {@snippet file='file' region= region}
                """,
                """
                {@snippet file='file' region="region"}
                """,
                """
                {@snippet file='file' region='region'}
                """,
                """
                {@snippet region=region file=file}
                """,
                """
                {@snippet region=region file="file"}
                """,
                """
                {@snippet region="region" file="file"}
                """,
                """
                {@snippet file="file"
                          region="region"}
                """,
                """
                {@snippet file="file"
                          region=region}
                """
        );
        addSnippetFile(srcDir, "pkg", "file", """
                                              1 
                                              2 
                                              3 
                                              """);
        ClassBuilder classBuilder = new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class");
        forEachNumbered(snippets, (s, i) -> {
            classBuilder.addMembers(
                    MethodBuilder.parse("public void case%s() { }".formatted(i)).setComments(s));
        });
        classBuilder.write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.OK);
        for (int j = 0; j < snippets.size(); j++) {
            checkOutput("pkg/A.html", true,
                        """
                        <span class="element-name">case%s</span>()</div>
                        <div class="block">
                        %s
                        """.formatted(j, getSnippetHtmlRepresentation("pkg/A.html", "2")));
        }
    }

    @Test
    public void testPositiveInlineTagMarkup_Comment(Path base) throws Exception {
        record TestCase(Snippet snippet, String expectedOutput) { }
        final var testCases = List.of(
                new TestCase(newSnippetBuilder()
                                     .body("""
                                           ,
                                            Snippet!""")
                                     .build(),
                             """
                             Hello
                             ,
                              Snippet!"""
                )
                ,
                new TestCase(newSnippetBuilder()
                                     .body("""
                                           ,
                                            Snippet!""")
                                     .build(),
                             """
                             Hello
                             ,
                              Snippet!"""
                )
                ,
                new TestCase(newSnippetBuilder()
                                     .body("""
                                           ,
                                            Snippet!""")
                                     .build(),
                             """
                              Hello
                             ,
                              Snippet!"""
                )
        );
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        ClassBuilder classBuilder = new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class");
        forEachNumbered(testCases, (t, id) -> {
            classBuilder
                    .addMembers(
                            MethodBuilder
                                    .parse("public void case%s() { }".formatted(id))
                                    .setComments("""
                                                 {@snippet :
                                                 %s}
                                                 """.formatted(t.snippet().body())));
        });
        classBuilder.write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.OK);
        forEachNumbered(testCases, (t, index) -> {
            checkOutput("pkg/A.html", true,
                        """
                        <span class="element-name">case%s</span>()</div>
                        <div class="block">
                        %s""".formatted(index, getSnippetHtmlRepresentation("pkg/A.html", t.expectedOutput())));
        });
    }

    @Test
    public void testNegativeHybridTag_FileNotFound(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        var fileName = "text.txt";
        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void test() { }")
                                .setComments("""
                                             {@snippet file="%s":
                                                 Hello, Snippet!}
                                             """.formatted(fileName)))
                .write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:4: error: file not found on source path or snippet path: %s""".formatted(fileName));
        checkNoCrashes();
    }

    @Test
    public void testNegativeTag_ValuelessAttributes(Path base) throws IOException {
        record TestCase(String input, String expectedError) { }
        var testCases = new ArrayList<TestCase>();
        for (String attrName : List.of("class", "file", "id", "lang", "region")) {
            TestCase t = new TestCase("""
                        {@snippet %s:
                            First line
                              Second line
                        }
                        """.formatted(attrName),
                        """
                        : error: missing value for attribute "%s"
                        {@snippet %s:
                                  ^""".formatted(attrName, attrName));
            testCases.add(t);
        }

        List<String> inputs = testCases.stream().map(s -> s.input).toList();
        StringBuilder methods = new StringBuilder();
        forEachNumbered(inputs, (i, n) -> {
            methods.append(
                    """

                    /**
                    %s*/
                    public void case%s() {}
                    """.formatted(i, n));
        });

        String classString =
                """
                public class A {
                %s
                }
                """.formatted(methods.toString());

        Path src = Files.createDirectories(base.resolve("src"));
        tb.writeJavaFiles(src, classString);

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("A.java").toString());
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true, testCases.stream().map(TestCase::expectedError).toArray(String[]::new));
        checkNoCrashes();
    }

    @Test
    public void testNegativeTag_BlankRegion(Path base) throws Exception {
        record TestCase(String input, String expectedError) { }

      var testCases = new ArrayList<TestCase>();
      for (String quote : List.of("", "'", "\""))
          for (String value : List.of("", " ")) {
              var t = new TestCase("""
                        {@snippet region=%s%s%s:
                            First line
                              Second line
                        }
                        """.formatted(quote, value, quote),
                        """
                        : error: illegal value for attribute "region": "%s"
                        {@snippet region=%s%s%s:
                                  ^
                        """.formatted(
                                quote.isEmpty() ? "" : value, 
                                quote, value, quote));
              testCases.add(t);
          }

        List<String> inputs = testCases.stream().map(s -> s.input).toList();
        StringBuilder methods = new StringBuilder();
        forEachNumbered(inputs, (i, n) -> {
            methods.append(
                    """

                    /**
                    %s*/
                    public void case%s() {}
                    """.formatted(i, n));
        });

        String classString =
                """
                public class A {
                %s
                }
                """.formatted(methods.toString());

        Path src = Files.createDirectories(base.resolve("src"));
        tb.writeJavaFiles(src, classString);

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("A.java").toString());
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true, testCases.stream().map(TestCase::expectedError).toArray(String[]::new));
        checkNoCrashes();
    }

    @Test
    public void testNegativeHybridTagMarkup_RegionNotFound(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        var fileName = "text.txt";
        var region = "here";
        var content =
                """
                Hello, Snippet!""";

        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void test() { }")
                                .setComments("""
                                             {@snippet region="%s" file="%s":
                                             %s}
                                             """.formatted(region, fileName, content)))
                .write(srcDir);
        addSnippetFile(srcDir, "pkg", fileName, content);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:4: error: region not found: "%s\"""".formatted(region));
        checkNoCrashes();
    }

    @Test
    public void testNegativeHybridTag_Mismatch(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        var fileName = "text.txt";
        var content =
                """
                Hello, Snippet!""";
        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void test() { }")
                                .setComments("""
                                             {@snippet file="%s":
                                             %s}
                                             """.formatted(fileName, content)))
                .write(srcDir);
        addSnippetFile(srcDir, "pkg", fileName, content + "...more");
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:4: error: contents mismatch""");
        checkOutput("pkg/A.html", true, """
                        <details class="invalid-tag">
                        <summary>invalid @snippet</summary>
                        <pre>contents mismatch:
                        ----------------- inline -------------------
                        Hello, Snippet!
                        ----------------- external -----------------
                        Hello, Snippet!...more
                        </pre>
                        </details>
                        """);
        checkNoCrashes();
    }

    @Test
    public void testNegativeHybridTagMarkup_RegionRegionMismatch(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        var fileName = "text.txt";
        var region = "here";
        var content =
                """
                Hello, Snippet!""";
        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void test() { }")
                                .setComments("""
                                             {@snippet region="%s" file="%s":
                                             Above the region.
                                             %s ...more
                                             Below the region}
                                             """.formatted(region, fileName, region, content)))
                .write(srcDir);
        addSnippetFile(srcDir, "pkg", fileName,
                       """
                       This line is above the region.
                       %s
                       This line is below the region.""".formatted(region, content));
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:4: error: contents mismatch""");
        checkOutput("pkg/A.html", true, """
                        <details class="invalid-tag">
                        <summary>invalid @snippet</summary>
                        <pre>contents mismatch:
                        ----------------- inline -------------------
                        Hello, Snippet! ...more

                        ----------------- external -----------------
                        Hello, Snippet!

                        </pre>
                        </details>
                        """);
        checkNoCrashes();
    }

    @Test
    public void testNegativeHybridTagMarkup_Region1Mismatch(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        var fileName = "text.txt";
        var region = "here";
        var content =
                """
                Hello, Snippet!""";
        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void test() { }")
                                .setComments("""
                                             {@snippet region="%s" file="%s":
                                             Above the region.
                                             %s ...more
                                             Below the region}
                                             """.formatted(region, fileName, region, content)))
                .write(srcDir);
        addSnippetFile(srcDir, "pkg", fileName, content);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:4: error: contents mismatch""");
        checkNoCrashes();
    }

    @Test
    public void testNegativeHybridTagMarkup_Region2Mismatch(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        var fileName = "text.txt";
        var region = "here";
        var content =
                """
                Hello, Snippet!""";
        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder
                                .parse("public void test() { }")
                                .setComments("""
                                             {@snippet region="%s" file="%s":
                                             %s}
                                             """.formatted(region, fileName, content)))
                .write(srcDir);
        addSnippetFile(srcDir, "pkg", fileName,
                       """
                       Above the region.
                       %s ...more
                       Below the region
                       """.formatted(region, content));
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true,
                    """
                    A.java:4: error: contents mismatch""");
        checkNoCrashes();
    }

    @Test
    public void testPositiveHybridTagMarkup(Path base) throws Exception {
        record TestCase(Snippet snippet, String expectedOutput) { }
        final var testCases = List.of(
                new TestCase(newSnippetBuilder()
                                     .body("""
                                           Hello
                                           ,
                                            Snippet!""")
                                     .fileContent(
                                             """
                                             Hello
                                             ,
                                              Snippet!""")
                                     .build(),
                             """
                             Hello
                             ,
                              Snippet!"""
                )
                ,
                new TestCase(newSnippetBuilder()
                                     .body("""
                                             Hello
                                             ,
                                              Snippet!
                                           """)
                                     .region("here")
                                     .fileContent(
                                             """
                                             Above the region.
                                               Hello
                                               ,
                                                Snippet!
                                             Below the region.
                                             """)
                                     .build(),
                             """
                               Hello
                               ,
                                Snippet!
                             """
                )
                ,
                new TestCase(newSnippetBuilder()
                                     .body("""
                                           Above the region.
                                             Hello
                                             ,
                                              Snippet!
                                           Below the region.
                                           """)
                                     .region("here")
                                     .fileContent(
                                             """
                                               Hello
                                               ,
                                                Snippet!
                                             """)
                                     .build(),
                             """
                               Hello
                               ,
                                Snippet!
                             """
                )
                ,
                new TestCase(newSnippetBuilder()
                                     .body("""
                                           Above the region.
                                             Hello
                                             ,
                                              Snippet!
                                           Below the region.
                                           """)
                                     .region("here")
                                     .fileContent(
                                             """
                                             Above the region.
                                               Hello
                                               ,
                                                Snippet!
                                             Below the region.
                                             """)
                                     .build(),
                             """
                               Hello
                               ,
                                Snippet!
                             """
                )
        );
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");
        ClassBuilder classBuilder = new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class");
        forEachNumbered(testCases, (t, id) -> {
            var snippet = t.snippet();
            final String r = snippet.regionName() == null ? "" : "region=\"" + snippet.regionName() + "\"";
            final String f = snippet.fileContent() == null ? "" : "file=\"%s.txt\"".formatted(id);
            classBuilder
                    .addMembers(
                            MethodBuilder
                                    .parse("public void case%s() { }".formatted(id))
                                    .setComments("""
                                                 {@snippet %s %s:
                                                 %s}
                                                 """.formatted(r, f, snippet.body())));
            addSnippetFile(srcDir, "pkg", "%s.txt".formatted(id), snippet.fileContent());
        });
        classBuilder.write(srcDir);
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.OK);
        forEachNumbered(testCases, (t, index) -> {
            checkOutput("pkg/A.html", true,
                        """
                        <span class="element-name">case%s</span>()</div>
                        <div class="block">
                        %s""".formatted(index, getSnippetHtmlRepresentation("pkg/A.html", t.expectedOutput())));
        });
    }

    @Test
    public void testNegativeInlineTagMarkup_InvalidRegexDiagnostics(Path base) throws Exception {

        record TestCase(String input, String expectedError) { }


        final var testCases = List.of(
                new TestCase("""
                    {@snippet :
                    hello there 
                    }""",
                             """
                    error: snippet markup: invalid regex
                    hello there 
                                                          \t ^
                    """),
                new TestCase("""
                    {@snippet :
                    hello there 
                    }""",
                        """
                    error: snippet markup: invalid regex
                    hello there 
                                                             ^
                    """),
                new TestCase("""
                    {@snippet :
                    hello there 
                    }""",
                             """
                    error: snippet markup: invalid regex
                    hello there 
                                                     \s\s\s\s   ^
                    """), 
                new TestCase("""
                    {@snippet :
                    hello there 
                    }""",
                        """
                    error: snippet markup: invalid regex
                    hello there 
                                                                          ^
                    """)
                );

        List<String> inputs = testCases.stream().map(s -> s.input).toList();
        StringBuilder methods = new StringBuilder();
        forEachNumbered(inputs, (i, n) -> {
            methods.append(
                    """

                    /**
                    %s*/
                    public void case%s() {}
                    """.formatted(i, n));
        });

        String classString =
                """
                public class A {
                %s
                }
                """.formatted(methods.toString());

        Path src = Files.createDirectories(base.resolve("src"));
        tb.writeJavaFiles(src, classString);

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("A.java").toString());
        checkExit(Exit.ERROR);
        checkOrder(Output.OUT, testCases.stream().map(TestCase::expectedError).toArray(String[]::new));
        checkOutput("A.html", true, """
                        <details class="invalid-tag">
                        <summary>invalid @snippet</summary>
                        <pre>invalid regex</pre>
                        </details>
                        """);
        checkNoCrashes();
    }

    @Test
    public void testNegativeInlineTagMarkup_ErrorMessages(Path base) throws Exception {

        record TestCase(String input, String expectedError) { }

        final var testCases = List.of(
                new TestCase("""
                        {@snippet :
                            hello 
                        }""",
                             """
                        error: snippet markup: missing attribute "target"
                            hello 
                                      ^
                             """),
                new TestCase("""
                        {@snippet :
                            hello 
                        }""",
                             """
                        error: snippet markup: missing attribute "region"
                            hello 
                                      ^
                             """),
                new TestCase("""
                        {@snippet :
                            hello 
                        }""",
                             """
                        error: snippet markup: missing attribute "replacement"
                            hello 
                                      ^
                             """),
                /* ---------------------- */
                new TestCase("""
                        {@snippet :
                            hello 
                        }""",
                        """
                        error: snippet markup: attributes "substring" and "regex" used simultaneously
                            hello 
                                                          ^
                        """),
                new TestCase("""
                        {@snippet :
                            hello 
                        }""",
                        """
                        error: snippet markup: unexpected attribute
                            hello 
                                                       ^
                        """),
                new TestCase("""
                        {@snippet :
                            hello 
                        }""",
                        """
                        error: snippet markup: invalid attribute value
                            hello 
                                                    ^
                        """),
                new TestCase("""
                        {@snippet :
                            hello 
                        }""",
                        """
                        error: snippet markup: invalid attribute value
                            hello 
                                                                         ^
                        """),
                /* ---------------------- */
                new TestCase("""
                        {@snippet :
                            hello
                            there 
                        }""",
                             """
                        error: snippet markup: unterminated attribute value
                            there 
                                                          ^
                             """),
                new TestCase("""
                        {@snippet :
                            hello 
                            world 
                            !     
                        }""",
                        """
                        error: snippet markup: duplicated region
                            world 
                                                    ^
                        """),
                new TestCase("""
                        {@snippet :
                            hello 
                        }""",
                        """
                        error: snippet markup: no region to end
                            hello 
                                      ^
                        """),
                new TestCase("""
                        {@snippet :
                            hello 
                        }""",
                        """
                        error: snippet markup: unpaired region
                            hello 
                                      ^
                        """),
                new TestCase("""
                        {@snippet :
                            hello 
                        }""",
                             """
                        error: snippet markup: tag refers to non-existent lines
                            hello 
                                      ^
                        """)
        );
        List<String> inputs = testCases.stream().map(s -> s.input).toList();
        StringBuilder methods = new StringBuilder();
        forEachNumbered(inputs, (i, n) -> {
            methods.append(
                    """

                    /**
                    %s*/
                    public void case%s() {}
                    """.formatted(i, n));
        });

        String classString =
                """
                public class A {
                %s
                }
                """.formatted(methods.toString());

        Path src = Files.createDirectories(base.resolve("src"));
        tb.writeJavaFiles(src, classString);

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                src.resolve("A.java").toString());
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true, testCases.stream().map(TestCase::expectedError).toArray(String[]::new));
        checkOutput("A.html", true, """
                        <details class="invalid-tag">
                        <summary>invalid @snippet</summary>
                        <pre>missing attribute "target"</pre>
                        </details>
                        """);
        checkNoCrashes();
    }
}
