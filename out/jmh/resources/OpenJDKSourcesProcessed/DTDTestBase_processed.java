/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package common.dtd;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import common.util.TestBase;

/**
 * @bug 8306632
 * @summary tests the DTD property jdk.xml.dtd.support.
 * The DTD property controls how DTDs are processed.
 */
public class DTDTestBase extends TestBase {

    public void testDOM(String filename, Properties fsp, PropertyState state,
        Properties config, Properties[] sysProp, Properties[] apiProp,
        boolean expectError, String error) throws Exception {

        DocumentBuilderFactory dbf = getDBF(fsp, state, config, sysProp, apiProp);
        process(filename, dbf, expectError, error);
    }

    public void testSAX(String filename, Properties fsp, PropertyState state,
        Properties config, Properties[] sysProp, Properties[] apiProp,
        boolean expectError, String error) throws Exception {

        SAXParser parser = getSAXParser(fsp, state, config, sysProp, apiProp);
        process(filename, parser, expectError, error);
    }

    public void testStAX(String filename, Properties fsp, PropertyState state,
        Properties config, Properties[] sysProp, Properties[] apiProp,
        boolean expectError, String error) throws Exception {

        XMLInputFactory xif = getXMLInputFactory(state, config, sysProp, apiProp);
        process(filename, xif, expectError, error);
    }

    public void testSchema1(String filename, String xsd, Properties fsp, PropertyState state,
        Properties config, Properties[] sysProp, Properties[] apiProp,
        boolean expectError, String error) throws Exception {

        SchemaFactory sf = getSchemaFactory(fsp, state, config, sysProp, apiProp);
        process(filename, sf, expectError, error);
    }

    public void testSchema2(String filename, String xsd, Properties fsp, PropertyState state,
        Properties config, Properties[] sysProp, Properties[] apiProp,
        boolean expectError, String error) throws Exception {
        testSchema1(filename, xsd, fsp, state, config, sysProp, apiProp, expectError,  error);
    }

    public void testValidation(String filename, String xsd, Properties fsp, PropertyState state,
        Properties config, Properties[] sysProp, Properties[] apiProp,
        boolean expectError, String error) throws Exception {

        SchemaFactory sf = getSchemaFactory(fsp, state, config, sysProp, apiProp);
        validate(filename, sf, expectError, error);
    }

    public void testStylesheet(String filename, String xsl, Properties fsp, PropertyState state,
        Properties config, Properties[] sysProp, Properties[] apiProp,
        boolean expectError, String error) throws Exception {

        TransformerFactory tf = getTransformerFactory(fsp, state, config, sysProp, apiProp);
        process(filename, tf, expectError, error);
    }

    public void testTransform(String filename, String xsl, Properties fsp, PropertyState state,
        Properties config, Properties[] sysProp, Properties[] apiProp,
        boolean expectError, String error) throws Exception {

        TransformerFactory tf = getTransformerFactory(fsp, state, config, sysProp, apiProp);
        transform(filename, xsl, tf, expectError, error);
    }

    /*
     * DataProvider for testing configuring properties for parsers.
     *
     * Fields:
     *     file, FSP, state of setting, config file, system property, api property,
     *     Custom Catalog, error expected, error code or expected result
     */
    public Object[][] getConfigs(Processor processor) {
        String fileDTDNotInC = "properties1.xml";

        String errCode = "JAXP00010008";

        String errXerces = "disallow-doctype-decl";

        boolean isErrExpected = true;
        String expected1 = "invalid.site.com";

        String expected = "";

        switch (processor) {
            case SAX:
                break;
            case STAX:
                errCode = "JAXP00010008";
                isErrExpected = false;
                expected = ".*[\\w\\s]+(value1)[\\w\\s]+.*";
                expected1 = expected;
                break;
            default:
                break;
        }

        return new Object[][]{
            /**
             * Case 1-1: DTD=allow by default; no Config file;
             * Expect: error as the parser processes DTD and tries to access the invalid site
             * Error: JAXP00010008 java.net.UnknownHostException: invalid.site.com
             */
            {fileDTDNotInC, null, null, null, null, null, isErrExpected, expected},

            /**
             * Case 1-2: DTD=deny in config file
             * Expect: Exception since DTD is denied
             */
            {fileDTDNotInC, null, PropertyState.CONFIG_FILE, Properties.CONFIG_FILE_DTD2, null, null, true, errCode},

            /**
             * Case 1-3: DTD=allow with the System Property
             * Expect: error as Case 1-1
             */
            {fileDTDNotInC, null, PropertyState.CONFIG_FILE_SYSTEM, Properties.CONFIG_FILE_DTD2, new Properties[]{Properties.DTD0}, null, isErrExpected, expected1},

            /**
             * Case 1-4: DTD=deny with the API property
             * Expect: Exception as Case 1-2
             */
            {fileDTDNotInC, null, PropertyState.CONFIG_FILE_SYSTEM_API, Properties.CONFIG_FILE_DTD2, new Properties[]{Properties.DTD0}, new Properties[]{Properties.DTD2}, true, errCode},

            /**
             * Case 1-5: DTD=ignore with the API property
             * Expect: no error, DTD is ignored
             */
            {fileDTDNotInC, null, PropertyState.CONFIG_FILE_SYSTEM_API, Properties.CONFIG_FILE_DTD2, new Properties[]{Properties.DTD0}, new Properties[]{Properties.DTD1}, false, expected},

            {fileDTDNotInC, null, PropertyState.CONFIG_FILE_SYSTEM_API, Properties.CONFIG_FILE_DTD2, new Properties[]{Properties.DTD0}, new Properties[]{Properties.XERCES_ALLOW_DTD}, isErrExpected, expected1},
            {fileDTDNotInC, null, PropertyState.CONFIG_FILE_SYSTEM_API, Properties.CONFIG_FILE_DTD2, new Properties[]{Properties.DTD0}, new Properties[]{Properties.XERCES_DISALLOW_DTD}, true, errXerces},

            {fileDTDNotInC, null, PropertyState.CONFIG_FILE_SYSTEM_API, Properties.CONFIG_FILE_DTD2, new Properties[]{Properties.DTD0}, new Properties[]{Properties.SUPPORT_DTD}, isErrExpected, expected1},
            {fileDTDNotInC, null, PropertyState.CONFIG_FILE_SYSTEM_API, Properties.CONFIG_FILE_DTD2, new Properties[]{Properties.DTD0}, new Properties[]{Properties.SUPPORT_DTD_FALSE}, false, expected},
        };
    }

    /*
     * DataProvider for testing configuring properties for validation or transform.
     *
     * Fields:
     *     xml file, xsd or xsl file, FSP, state of setting, config file, system property,
     *     api property, Custom Catalog, error expected, error code or expected result
     */
    public Object[][] getConfig(String m) {
        String xmlFile = "XSDImport_company.xsd";
        String xsdOrXsl = null;
        String expected = "";
        boolean errOnIgnore = false;
        String ignoreExpected = "";
        String errCode = "JAXP00010008";

        switch (m) {
            case "SchemaTest2":
                xmlFile = "XSDInclude_company.xsd";
                break;
            case "Validation":
                xmlFile = "val_test.xml";
                errOnIgnore = true;
                ignoreExpected = "x1";
                break;
            case "Stylesheet":
                xmlFile = "XSLDTD.xsl";
                break;
            case "Transform":
                xmlFile = "XSLPI.xml";
                xsdOrXsl = "<?xml version='1.0'?>"
                + "<!DOCTYPE top SYSTEM 'test.dtd'"
                + "["
                + "<!ENTITY % pe \"x\">"
                + "<!ENTITY   x1 \"AAAAA\">"
                + "<!ENTITY   x2 \"bbb\">"
                +"]>"
                + "<?xml-stylesheet href=\""
                + TEST_SOURCE_DIR
                + "/XSLPI_target.xsl\" type=\"text/xml\"?>"
                + "<xsl:stylesheet "
                + "    xmlns:xsl='http:
                + "    version='1.0'>"
                + "</xsl:stylesheet> ";
                errCode = "JAXP00010008";
                break;
            default:
                break;
        }
        return new Object[][]{
            /**
             * Case 1-1: default setting, DTD=allow
             * Expect: pass without error
             */
            {xmlFile, xsdOrXsl, null, null, null, null, null, false, expected},

            /**
             * Case 1-2: DTD=deny in config file
             * Expect: Exception since DTD is denied
             */
            {xmlFile, xsdOrXsl, null, PropertyState.CONFIG_FILE, Properties.CONFIG_FILE_DTD2, null, null, true, errCode},

            /**
             * Case 1-3: DTD=allow with the System Property
             * Expect: error as Case 1-1
             */
            {xmlFile, xsdOrXsl, null, PropertyState.CONFIG_FILE_SYSTEM, Properties.CONFIG_FILE_DTD2, new Properties[]{Properties.DTD0}, null, false, expected},

            /**
             * Case 1-4: DTD=deny with the API property
             * Expect: Exception as Case 1-2
             */
            {xmlFile, xsdOrXsl, null, PropertyState.CONFIG_FILE_SYSTEM_API, Properties.CONFIG_FILE_DTD2, new Properties[]{Properties.DTD0}, new Properties[]{Properties.DTD2}, true, errCode},

            /**
             * Case 1-5: DTD=ignore with the API property
             * Expect: no error, DTD is ignored
             */
            {xmlFile, xsdOrXsl, null, PropertyState.CONFIG_FILE_SYSTEM_API, Properties.CONFIG_FILE_DTD2, new Properties[]{Properties.DTD0}, new Properties[]{Properties.DTD1}, errOnIgnore, ignoreExpected},

        };
    }

    static String getPath(String file) {
        String temp = TEST_SOURCE_DIR + file;
        if (IS_WINDOWS) {
            temp = "/" + temp;
        }
        return temp;
    }

    String filename; String xsd; String xsl; Properties fsp; PropertyState state;
    Properties config; Properties[] sysProp; Properties[] apiProp;
    boolean expectError; String error;

    public void paramMap(Processor processor, String method, String index) {
        int i = 0;
        Object[][] params;
        if (processor == Processor.VALIDATOR ||
                processor == Processor.TRANSFORMER) {
            params = getConfig(method);
            i = 1;
        } else {
            params = getConfigs(processor);
        }
        Object[] param = params[Integer.parseInt(index)];
        filename = (String)param[0];
        if (processor == Processor.VALIDATOR) {
            xsd = (String)param[i];
        } else if (processor == Processor.TRANSFORMER) {
            xsl = (String)param[i];
        }
        fsp = (Properties)param[i + 1];
        state = (PropertyState)param[i + 2];
        config = (Properties)param[i + 3];
        sysProp = (Properties[])param[i + 4];
        apiProp = (Properties[])param[i + 5];
        expectError = (boolean)param[i + 6];
        error = (String)param[i + 7];
    }
}
