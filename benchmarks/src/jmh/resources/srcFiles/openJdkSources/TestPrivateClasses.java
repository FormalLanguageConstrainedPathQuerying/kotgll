/*
 * Copyright (c) 2003, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4780441 4874845 4978816 8014017 8016328 8025633 8026567 8175200 8182765
 * @summary  Make sure that when the -private flag is not used, members
 *           inherited from package private class are documented in the child.
 *
 *           Make sure that when a method inherits documentation from a method
 *           in a non-public class/interface, the non-public class/interface
 *           is not mentioned anywhere (not even in the signature or tree).
 *
 *           Make sure that when a private interface method with generic parameters
 *           is implemented, the comments can be inherited properly.
 *
 *           Make sure when no modifier appear in the class signature, the
 *           signature is displayed correctly without extra space at the beginning.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestPrivateClasses
 */
import javadoc.tester.JavadocTester;

public class TestPrivateClasses extends JavadocTester {

    public static void main(String... args) throws Exception {
        var tester = new TestPrivateClasses();
        tester.runTests();
    }

    @Test
    public void testDefault() {
        javadoc("-d", "out-default",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "pkg", "pkg2");
        checkExit(Exit.OK);

        checkOutput("pkg/PublicChild.html", true,
                """
                    <a href="#fieldInheritedFromParent" class="member-name-link">fieldInheritedFromParent</a>""",
                """
                    <a href="#methodInheritedFromParent(int)" class="member-name-link">methodInheritedFromParent</a>""",
                """
                    <div class="inheritance" title="Inheritance Tree">java.lang.Object
                    <div class="inheritance">pkg.PublicChild</div>
                    </div>""",
                """
                    <div class="member-signature"><span class="modifiers">public</span>&nbsp;<span c\
                    lass="return-type">void</span>&nbsp;<span class="element-name">methodInheritedFr\
                    omParent</span><wbr><span class="parameters">(int&nbsp;p1)</span>
                                                   throws <span class="exceptions">java.lang.Exception</span></div>""",
                """
                    <dl class="notes">
                    <dt>All Implemented Interfaces:</dt>
                    <dd><code><a href="PublicInterface.html" title="interface in pkg">PublicInterface</a></code></dd>
                    </dl>""");

        checkOutput("pkg/PublicChild.html", false,
                """
                    <span class="overrideSpecifyLabel">Overrides:</span>""",
                """
                    <span class="overrideSpecifyLabel">Specified by:</span>""",
                "Description copied from",
                "PrivateParent",
                "PrivateInterface");

        checkOutput("pkg/PublicChild.html", false,
                """
                    <td class="col-last"><code><span class="member-name-link"><a href="#methodInterf\
                    ace(int)">methodInterface</a></span><wbr>(int&nbsp;p1)</code>
                    <div class="block">Comment from interface.</div>
                    </td>""",
                """
                    <td class="col-last"><code><span class="member-name-link"><a href="#methodInterf\
                    ace2(int)">methodInterface2</a></span><wbr>(int&nbsp;p1)</code>
                    <div class="block">Comment from interface.</div>
                    </td>"""
        );

        checkOutput("pkg/PublicInterface.html", true,
                """
                    <a href="#fieldInheritedFromInterface" class="member-name-link">fieldInheritedFromInterface</a>""",
                """
                    <a href="#methodInterface(int)" class="member-name-link">methodInterface</a>""",
                """
                    <dl class="notes">
                    <dt>All Known Implementing Classes:</dt>
                    <dd><code><a href="PublicChild.html" title="class in pkg">PublicChild</a></code></dd>
                    </dl>""");

        checkOutput("pkg/PublicInterface.html", false,
                """
                    <span class="overrideSpecifyLabel">Specified by:</span>""",
                "Description copied from",
                "PrivateInterface",
                "All Superinterfaces");

        checkOutput("pkg2/C.html", false,
                "This comment should get copied to the implementing class");

        checkOutput("pkg2/C.html", false,
                """
                    <span class="member-name-link"><a href="I.html#hello(T)">hello</a></span>""");

        checkOutput("constant-values.html", false,
                "PrivateInterface");
    }

    @Test
    public void testPrivate() {
        javadoc("-d", "out-private",
                "-sourcepath", testSrc,
                "--no-platform-links",
                "-private",
                "pkg", "pkg2");
        checkExit(Exit.OK);

        checkOutput("pkg/PublicChild.html", true,
                """
                    Fields inherited from class&nbsp;pkg.<a href="PrivateParent.html" title="class in pkg">PrivateParent</a>""",
                """
                    <a href="PrivateParent.html#fieldInheritedFromParent">fieldInheritedFromParent</a>""",
                """
                    Methods inherited from class&nbsp;pkg.<a href="PrivateParent.html" title="class in pkg">PrivateParent</a>""",
                """
                    <a href="PrivateParent.html#methodInheritedFromParent(int)">methodInheritedFromParent</a>""",
                """
                    <dt>Overrides:</dt>
                    <dd><code><a href="PrivateParent.html#methodOverriddenFromParent(char%5B%5D,int,\
                    T,V,java.util.List)">methodOverriddenFromParent</a></code>&nbsp;in class&nbsp;<c\
                    ode><a href="PrivateParent.html" title="class in pkg">PrivateParent</a></code></\
                    dd>""",
                """
                    <dt>Specified by:</dt>
                    <dd><code><a href="PrivateInterface.html#methodInterface(int)">methodInterface</\
                    a></code>&nbsp;in interface&nbsp;<code><a href="PrivateInterface.html" title="in\
                    terface in pkg">PrivateInterface</a></code></dd>""",
                "Description copied from",
                "extends",
                """
                    <dl class="notes">
                    <dt>All Implemented Interfaces:</dt>
                    <dd><code><a href="PrivateInterface.html" title="interface in pkg">PrivateInterf\
                    ace</a></code>, <code><a href="PublicInterface.html" title="interface in pkg">Pu\
                    blicInterface</a></code></dd>
                    </dl>""",
                """
                    <div class="type-signature"><span class="modifiers">public class </span><span cl\
                    ass="element-name type-name-label">PublicChild</span>""");

        checkOutput("pkg/PublicInterface.html", true,
                """
                    Fields inherited from interface&nbsp;pkg.<a href="PrivateInterface.html" title="interface in pkg">PrivateInterface</a>""",
                """
                    <a href="PrivateInterface.html#fieldInheritedFromInterface">fieldInheritedFromInterface</a>""",
                """
                    Methods inherited from interface&nbsp;pkg.<a href="PrivateInterface.html" title="interface in pkg">PrivateInterface</a>""",
                "extends",
                "All Superinterfaces",
                """
                    <dl class="notes">
                    <dt>All Known Implementing Classes:</dt>
                    <dd><code><a href="PrivateParent.html" title="class in pkg">PrivateParent</a></c\
                    ode>, <code><a href="PublicChild.html" title="class in pkg">PublicChild</a></cod\
                    e></dd>
                    </dl>""");

        checkOutput("pkg/PrivateInterface.html", true,
                """
                    <a href="#methodInterface(int)" class="member-name-link">methodInterface</a>"""
        );

        checkOutput("pkg2/C.html", true,
                """
                    <span class="description-from-type-label">Description copied from interface:&nbsp;<code><\
                    a href="I.html#hello(T)">I</a></code></span>""",
                """
                    <dt>Specified by:</dt>
                    <dd><code><a href="I.html#hello(T)">hello</a></code>&nbsp;in interface&nbsp;<cod\
                    e><a href="I.html" title="interface in pkg2">I</a>&lt;java.lang.String&gt;</code\
                    ></dd>""");

        checkOutput("pkg/PrivateParent.html", true,
                """
                    <div class="type-signature"><span class="modifiers">class </span><span class="el\
                    ement-name type-name-label">PrivateParent</span>""");

        checkOutput("pkg/PrivateParent.html", false,
                """
                    <div class="type-signature"><span class="modifiers"> class </span><span class="el\
                    ement-name type-name-label">PrivateParent</span>""");
    }
}
