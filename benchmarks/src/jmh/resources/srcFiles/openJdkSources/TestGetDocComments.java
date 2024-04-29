/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8307184
 * @summary Test basic operation of Elements.getDocComments
 * @library /tools/lib /tools/javac/lib
 * @build   toolbox.ToolBox JavacTestingAbstractProcessor TestGetDocComments
 * @compile -processor TestGetDocComments -proc:only TestGetDocComments.java
 */

import java.io.Writer;
import java.util.*;
import java.util.function.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import toolbox.ToolBox;

/**
 * Test basic workings of Elements.getDocComments
 */
public class TestGetDocComments extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            boolean elementSeen = false;

            for (TypeElement typeRoot : ElementFilter.typesIn(roundEnv.getRootElements()) ) {
                for (Element element : typeRoot.getEnclosedElements()) {
                    ExpectedComment expectedComment = element.getAnnotation(ExpectedComment.class);
                    if (expectedComment != null ) {
                        elementSeen = true;
                        String expectedCommentStr = expectedComment.value();
                        String actualComment = elements.getDocComment(element);

                        if (!expectedCommentStr.equals(actualComment)) {
                            messager.printError("Unexpected doc comment found", element);
                            (new ToolBox()).checkEqual(expectedCommentStr.lines().toList(),
                                                       actualComment.lines().toList());
                        }
                    }
                }

                if (!elementSeen) {
                    throw new RuntimeException("No elements seen.");
                }
            }
        }
        return true;
    }

    @interface ExpectedComment {
        String value();
    }

    /**
     *Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
     *eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
     *enim ad minim veniam, quis nostrud exercitation ullamco laboris
     *nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor
     *in reprehenderit in voluptate velit esse cillum dolore eu
     *fugiat nulla pariatur. Excepteur sint occaecat cupidatat non
     *proident, sunt in culpa qui officia deserunt mollit anim id est
     *laborum.
     */
    @ExpectedComment("""
     Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
     eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
     enim ad minim veniam, quis nostrud exercitation ullamco laboris
     nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor
     in reprehenderit in voluptate velit esse cillum dolore eu
     fugiat nulla pariatur. Excepteur sint occaecat cupidatat non
     proident, sunt in culpa qui officia deserunt mollit anim id est
     laborum.
      """)
    @SuppressWarnings("") 
    /* Traditional comment */
    private void foo() {return ;}


    /**
*Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
**eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
***enim ad minim veniam, quis nostrud exercitation ullamco laboris
*****nisi ut aliquip ex ea commodo consequat.
 \u0009*Duis aute irure dolor in reprehenderit in voluptate velit esse
 **cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat
  ***cupidatat non proident, sunt in culpa qui officia deserunt mollit
                                            *anim id est laborum.
     */
    @ExpectedComment("""
       Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
       eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
       enim ad minim veniam, quis nostrud exercitation ullamco laboris
       nisi ut aliquip ex ea commodo consequat.
       Duis aute irure dolor in reprehenderit in voluptate velit esse
       cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat
       cupidatat non proident, sunt in culpa qui officia deserunt mollit
       anim id est laborum.
       """)
    @SuppressWarnings("") 
    /*
     * Traditional comment over multiple lines.
     */
    private void bar() {return ;}

    /**
     * Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
     * eiusmod tempor incididunt ut labore et dolore magna aliqua.
     */
    @ExpectedComment( 
     " Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do\n" +
     " eiusmod tempor incididunt ut labore et dolore magna aliqua.\n")
    private void baz() {return ;}

    /**   Totality */
    @ExpectedComment("Totality ") 
    private void quux() {return ;}

    /** Totality\u0020
     */
    @ExpectedComment("Totality \n")
    private void corge() {return ;}

    /**
     * Totality */
    @ExpectedComment(" Totality ") 
    private void grault() {return ;}

    /** \u0009\u0020
     * Totality
     */
    @ExpectedComment(" Totality\n")
    private void wombat() {return ;}

    /**
     */
    @ExpectedComment("") 
    private void empty() {return ;}

    /**
     * tail */
    @ExpectedComment(" tail ") 
    private void tail() {return ;}

    /**
   ****/
    @ExpectedComment("") 
    private void tail2() {return ;}

    /**
     * Lorem\u000A\u000D\u000D\u000Aipsum
     */
    @ExpectedComment(" Lorem\n\n\nipsum\n")
    private void wombat2() {return ;}
}
