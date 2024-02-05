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
package common.catalog;

/**
 * @test @bug 8306632
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @modules java.xml/jdk.xml.internal
 * @run driver common.catalog.TransformTest Stylesheet 0 
 * @run driver common.catalog.TransformTest Stylesheet 1 
 * @run driver common.catalog.TransformTest Stylesheet 2 
 * @run driver common.catalog.TransformTest Stylesheet 3 
 * @run driver common.catalog.TransformTest Stylesheet 4 
 * @run driver common.catalog.TransformTest Transform 0 
 * @run driver common.catalog.TransformTest Transform 1 
 * @run driver common.catalog.TransformTest Transform 2 
 * @run driver common.catalog.TransformTest Transform 3 
 * @run driver common.catalog.TransformTest Transform 4 
 * @summary verifies Transform's support of the property jdk.xml.dtd.support.
 */
public class TransformTest extends CatalogTestBase {

    public static void main(String args[]) throws Exception {
        new TransformTest().run(args[0], args[1]);
    }

    public void run(String method, String index) throws Exception {
        paramMap(Processor.TRANSFORMER, method, index);
        switch (method) {
            case "Stylesheet":
                super.testStylesheet(filename, xsl, fsp, state, config, sysProp, apiProp, cc, expectError, error);
                break;
            case "Transform":
                super.testTransform(filename, xsl, fsp, state, config, sysProp, apiProp, cc, expectError, error);
                break;
        }
    }
}
