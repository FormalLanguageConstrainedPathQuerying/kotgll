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
package common.config;

import org.testng.annotations.DataProvider;

/**
 * Verifies the configuration file and precedence:
 *     settings in the configuration file are used as the default values of properties;
 *     any settings in a custom configuration file override those in the default
 * configuration.
 */
public class ConfigurationTest {
    static final String SP_CONFIG = "java.xml.config.file";
    static final String ISP_ENTITY_EXPANSION = "jdk.xml.entityExpansionLimit";
    static final String ISP_PARAMETER_ENTITY = "jdk.xml.maxParameterEntitySizeLimit";
    static final String ISP_ELEMENT_ATTRIBUTE = "jdk.xml.elementAttributeLimit";
    static final String ISP_NAME_LIMIT = "jdk.xml.maxXMLNameLimit";

    static final String ISF_EXTENSION_FUNCTIONS = "jdk.xml.enableExtensionFunctions";
    static final String CATALOG_RESOLVE = "javax.xml.catalog.resolve";
    static final String USE_CATALOG = "http:
    static final String SP_USE_CATALOG = "javax.xml.useCatalog";


    static final boolean IS_WINDOWS = System.getProperty("os.name").contains("Windows");
    static final String SRC_DIR;
    static final String TEST_SOURCE_DIR;
    static {
        String srcDir = System.getProperty("test.src", ".");
        if (IS_WINDOWS) {
            srcDir = srcDir.replace('\\', '/');
        }
        SRC_DIR = srcDir;
        TEST_SOURCE_DIR = srcDir + "/files/";
    }

    static enum PropertyType { FEATURE, PROPERTY };

   /*
     * DataProvider for testing the configuration file and system property.
     *
     * Fields:
     *     configuration file, property name, property type, property value
     */
    @DataProvider(name = "getProperty")
    public Object[][] getProperty() {
        /**
         * Test cases for verifying the configuration file
         */
        return new Object[][]{
            {null, ISP_PARAMETER_ENTITY, "1000000"},
            {null, ISP_NAME_LIMIT, "1000"},
            {null, ISP_ENTITY_EXPANSION, "64000"},
        };
    }

    @DataProvider(name = "getProperty0")
    public Object[][] getProperty0() {
        /**
         * Duplicate of getProperty to include the case that uses the system
         * property to set up a custom configuration file. This is to avoid
         * interfering with other test cases.
         */
        return new Object[][]{
            {"customJaxp.properties", ISP_ENTITY_EXPANSION, "1000"},
        };
    }


    static String getPath(String file) {
        String temp = TEST_SOURCE_DIR + file;
        if (IS_WINDOWS) {
            temp = "/" + temp;
        }
        return temp;
    }
}
