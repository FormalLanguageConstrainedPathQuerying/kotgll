/*
 * Copyright (c) 2003, 2023, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl;

import com.sun.org.apache.xerces.internal.util.Status;
import com.sun.xml.internal.stream.XMLEntityStorage;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.stream.events.XMLEvent;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLResourceIdentifierImpl;
import com.sun.org.apache.xerces.internal.util.XMLStringBuffer;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.xml.internal.stream.Entity;
import jdk.xml.internal.XMLLimitAnalyzer;
import jdk.xml.internal.XMLSecurityManager;


/**
 * This class is responsible for holding scanning methods common to
 * scanning the XML document structure and content as well as the DTD
 * structure and content. Both XMLDocumentScanner and XMLDTDScanner inherit
 * from this base class.
 *
 * <p>
 * This component requires the following features and properties from the
 * component manager that uses it:
 * <ul>
 *  <li>http:
 *  <li>http:
 *  <li>http:
 *  <li>http:
 *  <li>http:
 * </ul>
 *
 * @author Andy Clark, IBM
 * @author Arnaud  Le Hors, IBM
 * @author Eric Ye, IBM
 * @author K.Venugopal SUN Microsystems
 * @author Sunitha Reddy, SUN Microsystems
 * @LastModified: July 2023
 */
public abstract class XMLScanner
        implements XMLComponent {



    /** Feature identifier: namespaces. */
    protected static final String NAMESPACES =
            Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE;

    /** Feature identifier: validation. */
    protected static final String VALIDATION =
            Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

    /** Feature identifier: notify character references. */
    protected static final String NOTIFY_CHAR_REFS =
            Constants.XERCES_FEATURE_PREFIX + Constants.NOTIFY_CHAR_REFS_FEATURE;


    protected static final String PARSER_SETTINGS =
                                Constants.XERCES_FEATURE_PREFIX + Constants.PARSER_SETTINGS;
    /** Property identifier: symbol table. */
    protected static final String SYMBOL_TABLE =
            Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property identifier: error reporter. */
    protected static final String ERROR_REPORTER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Property identifier: entity manager. */
    protected static final String ENTITY_MANAGER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_MANAGER_PROPERTY;

    /** Property identifier: Security manager. */
    private static final String SECURITY_MANAGER = Constants.SECURITY_MANAGER;


    /** Debug attribute normalization. */
    protected static final boolean DEBUG_ATTR_NORMALIZATION = false;

    /**
     * Type of names
     */
    public static enum NameType {
        ATTRIBUTE("attribute"),
        ATTRIBUTENAME("attribute name"),
        COMMENT("comment"),
        DOCTYPE("doctype"),
        ELEMENTSTART("startelement"),
        ELEMENTEND("endelement"),
        ENTITY("entity"),
        NOTATION("notation"),
        PI("pi"),
        REFERENCE("reference");

        final String literal;
        NameType(String literal) {
            this.literal = literal;
        }

        String literal() {
            return literal;
        }
    }

    private boolean fNeedNonNormalizedValue = false;

    protected ArrayList<XMLString> attributeValueCache = new ArrayList<>();
    protected ArrayList<XMLStringBuffer> stringBufferCache = new ArrayList<>();
    protected int fStringBufferIndex = 0;
    protected boolean fAttributeCacheInitDone = false;
    protected int fAttributeCacheUsedCount = 0;



    /**
     * Validation. This feature identifier is:
     * http:
     */
    protected boolean fValidation = false;

    /** Namespaces. */
    protected boolean fNamespaces;

    /** Character references notification. */
    protected boolean fNotifyCharRefs = false;

    /** Internal parser-settings feature */
    protected boolean fParserSettings = true;


    protected PropertyManager fPropertyManager = null ;
    /** Symbol table. */
    protected SymbolTable fSymbolTable;

    /** Error reporter. */
    protected XMLErrorReporter fErrorReporter;

    /** Entity manager. */
    protected XMLEntityManager fEntityManager = null ;

    /** xxx this should be available from EntityManager Entity storage */
    protected XMLEntityStorage fEntityStore = null ;

    /** Security manager. */
    protected XMLSecurityManager fSecurityManager = null;

    /** Limit analyzer. */
    protected XMLLimitAnalyzer fLimitAnalyzer = null;


    /** event type */
    protected XMLEvent fEvent ;

    /** Entity scanner, this always works on last entity that was opened. */
    protected XMLEntityScanner fEntityScanner = null;

    /** Entity depth. */
    protected int fEntityDepth;

    /** Literal value of the last character reference scanned. */
    protected String fCharRefLiteral = null;

    /** Scanning attribute. */
    protected boolean fScanningAttribute;

    /** Report entity boundary. */
    protected boolean fReportEntity;


    /** Symbol: "version". */
    protected final static String fVersionSymbol = "version".intern();

    /** Symbol: "encoding". */
    protected final static String fEncodingSymbol = "encoding".intern();

    /** Symbol: "standalone". */
    protected final static String fStandaloneSymbol = "standalone".intern();

    /** Symbol: "amp". */
    protected final static String fAmpSymbol = "amp".intern();

    /** Symbol: "lt". */
    protected final static String fLtSymbol = "lt".intern();

    /** Symbol: "gt". */
    protected final static String fGtSymbol = "gt".intern();

    /** Symbol: "quot". */
    protected final static String fQuotSymbol = "quot".intern();

    /** Symbol: "apos". */
    protected final static String fAposSymbol = "apos".intern();



    /** String. */
    private XMLString fString = new XMLString();

    /** String buffer. */
    private XMLStringBuffer fStringBuffer = new XMLStringBuffer();

    /** String buffer. */
    private XMLStringBuffer fStringBuffer2 = new XMLStringBuffer();

    /** String buffer. */
    private XMLStringBuffer fStringBuffer3 = new XMLStringBuffer();

    protected XMLResourceIdentifierImpl fResourceIdentifier = new XMLResourceIdentifierImpl();
    int initialCacheCount = 6;

    /**
     *
     *
     * @param componentManager The component manager.
     *
     * @throws SAXException Throws exception if required features and
     *                      properties cannot be found.
     */
    public void reset(XMLComponentManager componentManager)
    throws XMLConfigurationException {

                fParserSettings = componentManager.getFeature(PARSER_SETTINGS, true);

                if (!fParserSettings) {
                        init();
                        return;
                }


        fSymbolTable = (SymbolTable)componentManager.getProperty(SYMBOL_TABLE);
        fErrorReporter = (XMLErrorReporter)componentManager.getProperty(ERROR_REPORTER);
        fEntityManager = (XMLEntityManager)componentManager.getProperty(ENTITY_MANAGER);
        fSecurityManager = (XMLSecurityManager)componentManager.getProperty(SECURITY_MANAGER);

        fEntityStore = fEntityManager.getEntityStore() ;

        fValidation = componentManager.getFeature(VALIDATION, false);
        fNamespaces = componentManager.getFeature(NAMESPACES, true);
        fNotifyCharRefs = componentManager.getFeature(NOTIFY_CHAR_REFS, false);

        init();
    } 

    protected void setPropertyManager(PropertyManager propertyManager){
        fPropertyManager = propertyManager ;
    }

    /**
     * Sets the value of a property during parsing.
     *
     * @param propertyId
     * @param value
     */
    public void setProperty(String propertyId, Object value)
    throws XMLConfigurationException {

        if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
            String property =
                    propertyId.substring(Constants.XERCES_PROPERTY_PREFIX.length());
            if (property.equals(Constants.SYMBOL_TABLE_PROPERTY)) {
                fSymbolTable = (SymbolTable)value;
            } else if (property.equals(Constants.ERROR_REPORTER_PROPERTY)) {
                fErrorReporter = (XMLErrorReporter)value;
            } else if (property.equals(Constants.ENTITY_MANAGER_PROPERTY)) {
                fEntityManager = (XMLEntityManager)value;
            }
        }

        if (propertyId.equals(SECURITY_MANAGER)) {
            fSecurityManager = (XMLSecurityManager)value;
        }
                /*else if(propertyId.equals(Constants.STAX_PROPERTIES)){
            fStaxProperties = (HashMap)value;
        }*/

    } 

    /*
     * Sets the feature of the scanner.
     */
    public void setFeature(String featureId, boolean value)
    throws XMLConfigurationException {

        if (VALIDATION.equals(featureId)) {
            fValidation = value;
        } else if (NOTIFY_CHAR_REFS.equals(featureId)) {
            fNotifyCharRefs = value;
        }
    }

    /*
     * Gets the state of the feature of the scanner.
     */
    public boolean getFeature(String featureId)
    throws XMLConfigurationException {

        if (VALIDATION.equals(featureId)) {
            return fValidation;
        } else if (NOTIFY_CHAR_REFS.equals(featureId)) {
            return fNotifyCharRefs;
        }
        throw new XMLConfigurationException(Status.NOT_RECOGNIZED, featureId);
    }


    protected void reset() {
        init();

        fValidation = true;
        fNotifyCharRefs = false;

    }

    public void reset(PropertyManager propertyManager) {
        init();
        fSymbolTable = (SymbolTable)propertyManager.getProperty(Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY);

        fErrorReporter = (XMLErrorReporter)propertyManager.getProperty(Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY);

        fEntityManager = (XMLEntityManager)propertyManager.getProperty(ENTITY_MANAGER);
        fEntityStore = fEntityManager.getEntityStore() ;
        fEntityScanner = fEntityManager.getEntityScanner() ;
        fSecurityManager = (XMLSecurityManager)propertyManager.getProperty(SECURITY_MANAGER);

        fValidation = false;
        fNotifyCharRefs = false;

    }

    /**
     * Scans an XML or text declaration.
     * <p>
     * <pre>
     * [23] XMLDecl ::= '<?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'
     * [24] VersionInfo ::= S 'version' Eq (' VersionNum ' | " VersionNum ")
     * [80] EncodingDecl ::= S 'encoding' Eq ('"' EncName '"' |  "'" EncName "'" )
     * [81] EncName ::= [A-Za-z] ([A-Za-z0-9._] | '-')*
     * [32] SDDecl ::= S 'standalone' Eq (("'" ('yes' | 'no') "'")
     *                 | ('"' ('yes' | 'no') '"'))
     *
     * [77] TextDecl ::= '<?xml' VersionInfo? EncodingDecl S? '?>'
     * </pre>
     *
     * @param scanningTextDecl True if a text declaration is to
     *                         be scanned instead of an XML
     *                         declaration.
     * @param pseudoAttributeValues An array of size 3 to return the version,
     *                         encoding and standalone pseudo attribute values
     *                         (in that order).
     *
     * <strong>Note:</strong> This method uses fString, anything in it
     * at the time of calling is lost.
     */
    protected void scanXMLDeclOrTextDecl(boolean scanningTextDecl,
            String[] pseudoAttributeValues)
            throws IOException, XNIException {

        String version = null;
        String encoding = null;
        String standalone = null;

        final int STATE_VERSION = 0;
        final int STATE_ENCODING = 1;
        final int STATE_STANDALONE = 2;
        final int STATE_DONE = 3;
        int state = STATE_VERSION;

        boolean dataFoundForTarget = false;
        boolean sawSpace = fEntityScanner.skipSpaces();
        Entity.ScannedEntity currEnt = fEntityManager.getCurrentEntity();
        boolean currLiteral = currEnt.literal;
        currEnt.literal = false;
        while (fEntityScanner.peekChar() != '?') {
            dataFoundForTarget = true;
            String name = scanPseudoAttribute(scanningTextDecl, fString);
            switch (state) {
                case STATE_VERSION: {
                    if (name.equals(fVersionSymbol)) {
                        if (!sawSpace) {
                            reportFatalError(scanningTextDecl
                                    ? "SpaceRequiredBeforeVersionInTextDecl"
                                    : "SpaceRequiredBeforeVersionInXMLDecl",
                                    null);
                        }
                        version = fString.toString();
                        state = STATE_ENCODING;
                        if (!versionSupported(version)) {
                            reportFatalError("VersionNotSupported",
                                    new Object[]{version});
                        }

                        if (version.equals("1.1")) {
                            Entity.ScannedEntity top = fEntityManager.getTopLevelEntity();
                            if (top != null && (top.version == null || top.version.equals("1.0"))) {
                                reportFatalError("VersionMismatch", null);
                            }
                            fEntityManager.setScannerVersion(Constants.XML_VERSION_1_1);
                        }

                    } else if (name.equals(fEncodingSymbol)) {
                        if (!scanningTextDecl) {
                            reportFatalError("VersionInfoRequired", null);
                        }
                        if (!sawSpace) {
                            reportFatalError(scanningTextDecl
                                    ? "SpaceRequiredBeforeEncodingInTextDecl"
                                    : "SpaceRequiredBeforeEncodingInXMLDecl",
                                    null);
                        }
                        encoding = fString.toString();
                        state = scanningTextDecl ? STATE_DONE : STATE_STANDALONE;
                    } else {
                        if (scanningTextDecl) {
                            reportFatalError("EncodingDeclRequired", null);
                        } else {
                            reportFatalError("VersionInfoRequired", null);
                        }
                    }
                    break;
                }
                case STATE_ENCODING: {
                    if (name.equals(fEncodingSymbol)) {
                        if (!sawSpace) {
                            reportFatalError(scanningTextDecl
                                    ? "SpaceRequiredBeforeEncodingInTextDecl"
                                    : "SpaceRequiredBeforeEncodingInXMLDecl",
                                    null);
                        }
                        encoding = fString.toString();
                        state = scanningTextDecl ? STATE_DONE : STATE_STANDALONE;
                    } else if (!scanningTextDecl && name.equals(fStandaloneSymbol)) {
                        if (!sawSpace) {
                            reportFatalError("SpaceRequiredBeforeStandalone",
                                    null);
                        }
                        standalone = fString.toString();
                        state = STATE_DONE;
                        if (!standalone.equals("yes") && !standalone.equals("no")) {
                            reportFatalError("SDDeclInvalid", new Object[] {standalone});
                        }
                    } else {
                        reportFatalError("EncodingDeclRequired", null);
                    }
                    break;
                }
                case STATE_STANDALONE: {
                    if (name.equals(fStandaloneSymbol)) {
                        if (!sawSpace) {
                            reportFatalError("SpaceRequiredBeforeStandalone",
                                    null);
                        }
                        standalone = fString.toString();
                        state = STATE_DONE;
                        if (!standalone.equals("yes") && !standalone.equals("no")) {
                            reportFatalError("SDDeclInvalid",  new Object[] {standalone});
                        }
                    } else {
                        reportFatalError("SDDeclNameInvalid", null);
                    }
                    break;
                }
                default: {
                    reportFatalError("NoMorePseudoAttributes", null);
                }
            }
            sawSpace = fEntityScanner.skipSpaces();
        }
        if(currLiteral) {
            currEnt.literal = true;
        }
        if (scanningTextDecl && state != STATE_DONE) {
            reportFatalError("MorePseudoAttributes", null);
        }

        if (scanningTextDecl) {
            if (!dataFoundForTarget && encoding == null) {
                reportFatalError("EncodingDeclRequired", null);
            }
        } else {
            if (!dataFoundForTarget && version == null) {
                reportFatalError("VersionInfoRequired", null);
            }
        }

        if (!fEntityScanner.skipChar('?', null)) {
            reportFatalError("XMLDeclUnterminated", null);
        }
        if (!fEntityScanner.skipChar('>', null)) {
            reportFatalError("XMLDeclUnterminated", null);

        }

        pseudoAttributeValues[0] = version;
        pseudoAttributeValues[1] = encoding;
        pseudoAttributeValues[2] = standalone;

    } 

    /**
     * Scans a pseudo attribute.
     *
     * @param scanningTextDecl True if scanning this pseudo-attribute for a
     *                         TextDecl; false if scanning XMLDecl. This
     *                         flag is needed to report the correct type of
     *                         error.
     * @param value            The string to fill in with the attribute
     *                         value.
     *
     * @return The name of the attribute
     *
     * <strong>Note:</strong> This method uses fStringBuffer2, anything in it
     * at the time of calling is lost.
     */
    protected String scanPseudoAttribute(boolean scanningTextDecl,
            XMLString value)
            throws IOException, XNIException {

        String name = scanPseudoAttributeName();

        if (name == null) {
            reportFatalError("PseudoAttrNameExpected", null);
        }
        fEntityScanner.skipSpaces();
        if (!fEntityScanner.skipChar('=', null)) {
            reportFatalError(scanningTextDecl ? "EqRequiredInTextDecl"
                    : "EqRequiredInXMLDecl", new Object[]{name});
        }
        fEntityScanner.skipSpaces();
        int quote = fEntityScanner.peekChar();
        if (quote != '\'' && quote != '"') {
            reportFatalError(scanningTextDecl ? "QuoteRequiredInTextDecl"
                    : "QuoteRequiredInXMLDecl" , new Object[]{name});
        }
        fEntityScanner.scanChar(NameType.ATTRIBUTE);
        int c = fEntityScanner.scanLiteral(quote, value, false);
        if (c != quote) {
            fStringBuffer2.clear();
            do {
                fStringBuffer2.append(value);
                if (c != -1) {
                    if (c == '&' || c == '%' || c == '<' || c == ']') {
                        fStringBuffer2.append((char)fEntityScanner.scanChar(NameType.ATTRIBUTE));
                    } else if (XMLChar.isHighSurrogate(c)) {
                        scanSurrogates(fStringBuffer2);
                    } else if (isInvalidLiteral(c)) {
                        String key = scanningTextDecl
                                ? "InvalidCharInTextDecl" : "InvalidCharInXMLDecl";
                        reportFatalError(key,
                                new Object[] {Integer.toString(c, 16)});
                                fEntityScanner.scanChar(null);
                    }
                }
                c = fEntityScanner.scanLiteral(quote, value, false);
            } while (c != quote);
            fStringBuffer2.append(value);
            value.setValues(fStringBuffer2);
        }
        if (!fEntityScanner.skipChar(quote, null)) {
            reportFatalError(scanningTextDecl ? "CloseQuoteMissingInTextDecl"
                    : "CloseQuoteMissingInXMLDecl",
                    new Object[]{name});
        }

        return name;

    } 

    /**
     * Scans the name of a pseudo attribute. The only legal names
     * in XML 1.0/1.1 documents are 'version', 'encoding' and 'standalone'.
     *
     * @return the name of the pseudo attribute or <code>null</code>
     * if a legal pseudo attribute name could not be scanned.
     */
    private String scanPseudoAttributeName() throws IOException, XNIException {
        final int ch = fEntityScanner.peekChar();
        switch (ch) {
            case 'v':
                if (fEntityScanner.skipString(fVersionSymbol)) {
                    return fVersionSymbol;
                }
                break;
            case 'e':
                if (fEntityScanner.skipString(fEncodingSymbol)) {
                    return fEncodingSymbol;
                }
                break;
            case 's':
                if (fEntityScanner.skipString(fStandaloneSymbol)) {
                    return fStandaloneSymbol;
                }
                break;
        }
        return null;
    } 

    /**
     * Scans a processing instruction.
     * <p>
     * <pre>
     * [16] PI ::= '&lt;?' PITarget (S (Char* - (Char* '?>' Char*)))? '?>'
     * [17] PITarget ::= Name - (('X' | 'x') ('M' | 'm') ('L' | 'l'))
     * </pre>
     */

    protected void scanPI(XMLStringBuffer data) throws IOException, XNIException {

        fReportEntity = false;
        String target = fEntityScanner.scanName(NameType.PI);
        if (target == null) {
            reportFatalError("PITargetRequired", null);
        }

        scanPIData(target, data);
        fReportEntity = true;

    } 

    /**
     * Scans a processing data. This is needed to handle the situation
     * where a document starts with a processing instruction whose
     * target name <em>starts with</em> "xml". (e.g. xmlfoo)
     *
     * This method would always read the whole data. We have while loop and data is buffered
     * until delimeter is encountered.
     *
     * @param target The PI target
     * @param data The string to fill in with the data
     */


    protected void scanPIData(String target, XMLStringBuffer data)
    throws IOException, XNIException {

        if (target.length() == 3) {
            char c0 = Character.toLowerCase(target.charAt(0));
            char c1 = Character.toLowerCase(target.charAt(1));
            char c2 = Character.toLowerCase(target.charAt(2));
            if (c0 == 'x' && c1 == 'm' && c2 == 'l') {
                reportFatalError("ReservedPITarget", null);
            }
        }

        if (!fEntityScanner.skipSpaces()) {
            if (fEntityScanner.skipString("?>")) {
                return;
            } else {
                reportFatalError("SpaceRequiredInPI", null);
            }
        }

        if (fEntityScanner.scanData("?>", data, 0)) {
            do {
                int c = fEntityScanner.peekChar();
                if (c != -1) {
                    if (XMLChar.isHighSurrogate(c)) {
                        scanSurrogates(data);
                    } else if (isInvalidLiteral(c)) {
                        reportFatalError("InvalidCharInPI",
                                new Object[]{Integer.toHexString(c)});
                                fEntityScanner.scanChar(null);
                    }
                }
            } while (fEntityScanner.scanData("?>", data, 0));
        }

    } 

    /**
     * Scans a comment.
     * <p>
     * <pre>
     * [15] Comment ::= '&lt!--' ((Char - '-') | ('-' (Char - '-')))* '-->'
     * </pre>
     * <p>
     * <strong>Note:</strong> Called after scanning past '&lt;!--'
     * <strong>Note:</strong> This method uses fString, anything in it
     * at the time of calling is lost.
     *
     * @param text The buffer to fill in with the text.
     */
    protected void scanComment(XMLStringBuffer text)
    throws IOException, XNIException {

        text.clear();
        while (fEntityScanner.scanData("--", text, 0)) {
            int c = fEntityScanner.peekChar();


            if (c != -1) {
                if (XMLChar.isHighSurrogate(c)) {
                    scanSurrogates(text);
                }
                else if (isInvalidLiteral(c)) {
                    reportFatalError("InvalidCharInComment",
                            new Object[] { Integer.toHexString(c) });
                            fEntityScanner.scanChar(NameType.COMMENT);
                }
            }
        }
        if (!fEntityScanner.skipChar('>', NameType.COMMENT)) {
            reportFatalError("DashDashInComment", null);
        }

    } 

    /**
     * Scans an attribute value and normalizes whitespace converting all
     * whitespace characters to space characters.
     *
     * [10] AttValue ::= '"' ([^<&"] | Reference)* '"' | "'" ([^<&'] | Reference)* "'"
     *
     * @param value The XMLString to fill in with the value.
     * @param nonNormalizedValue The XMLString to fill in with the
     *                           non-normalized value.
     * @param atName The name of the attribute being parsed (for error msgs).
     * @param attributes The attributes list for the scanned attribute.
     * @param attrIndex The index of the attribute to use from the list.
     * @param checkEntities true if undeclared entities should be reported as VC violation,
     *                      false if undeclared entities should be reported as WFC violation.
     * @param eleName The name of element to which this attribute belongs.
     * @param isNSURI a flag indicating whether the content is a Namespace URI
     *
     * <strong>Note:</strong> This method uses fStringBuffer2, anything in it
     * at the time of calling is lost.
     **/
    protected void scanAttributeValue(XMLString value, XMLString nonNormalizedValue,
            String atName, XMLAttributes attributes, int attrIndex, boolean checkEntities,
            String eleName, boolean isNSURI)
            throws IOException, XNIException {
        XMLStringBuffer stringBuffer = null;
        int quote = fEntityScanner.peekChar();
        if (quote != '\'' && quote != '"') {
            reportFatalError("OpenQuoteExpected", new Object[]{eleName, atName});
        }

        fEntityScanner.scanChar(NameType.ATTRIBUTE);
        int entityDepth = fEntityDepth;

        int c = fEntityScanner.scanLiteral(quote, value, isNSURI);
        if (DEBUG_ATTR_NORMALIZATION) {
            System.out.println("** scanLiteral -> \""
                    + value.toString() + "\"");
        }
        if(fNeedNonNormalizedValue){
            fStringBuffer2.clear();
            fStringBuffer2.append(value);
        }
        if(fEntityScanner.whiteSpaceLen > 0)
            normalizeWhitespace(value);
        if (DEBUG_ATTR_NORMALIZATION) {
            System.out.println("** normalizeWhitespace -> \""
                    + value.toString() + "\"");
        }
        if (c != quote) {
            fScanningAttribute = true;
            stringBuffer = getStringBuffer();
            stringBuffer.clear();
            do {
                stringBuffer.append(value);
                if (DEBUG_ATTR_NORMALIZATION) {
                    System.out.println("** value2: \""
                            + stringBuffer.toString() + "\"");
                }
                if (c == '&') {
                    fEntityScanner.skipChar('&', NameType.REFERENCE);
                    if (entityDepth == fEntityDepth && fNeedNonNormalizedValue ) {
                        fStringBuffer2.append('&');
                    }
                    if (fEntityScanner.skipChar('#', NameType.REFERENCE)) {
                        if (entityDepth == fEntityDepth && fNeedNonNormalizedValue ) {
                            fStringBuffer2.append('#');
                        }
                        int ch ;
                        if (fNeedNonNormalizedValue)
                            ch = scanCharReferenceValue(stringBuffer, fStringBuffer2);
                        else
                            ch = scanCharReferenceValue(stringBuffer, null);

                        if (ch != -1) {
                            if (DEBUG_ATTR_NORMALIZATION) {
                                System.out.println("** value3: \""
                                        + stringBuffer.toString()
                                        + "\"");
                            }
                        }
                    } else {
                        String entityName = fEntityScanner.scanName(NameType.ENTITY);
                        if (entityName == null) {
                            reportFatalError("NameRequiredInReference", null);
                        } else if (entityDepth == fEntityDepth && fNeedNonNormalizedValue) {
                            fStringBuffer2.append(entityName);
                        }
                        if (!fEntityScanner.skipChar(';', NameType.REFERENCE)) {
                            reportFatalError("SemicolonRequiredInReference",
                                    new Object []{entityName});
                        } else if (entityDepth == fEntityDepth && fNeedNonNormalizedValue) {
                            fStringBuffer2.append(';');
                        }
                        if (resolveCharacter(entityName, stringBuffer)) {
                            checkEntityLimit(false, fEntityScanner.fCurrentEntity.name, 1);
                        } else {
                            if (fEntityStore.isExternalEntity(entityName)) {
                                reportFatalError("ReferenceToExternalEntity",
                                        new Object[] { entityName });
                            } else {
                                if (!fEntityStore.isDeclaredEntity(entityName)) {
                                    if (checkEntities) {
                                        if (fValidation) {
                                            fErrorReporter.reportError(fEntityScanner,XMLMessageFormatter.XML_DOMAIN,
                                                    "EntityNotDeclared",
                                                    new Object[]{entityName},
                                                    XMLErrorReporter.SEVERITY_ERROR);
                                        }
                                    } else {
                                        reportFatalError("EntityNotDeclared",
                                                new Object[]{entityName});
                                    }
                                }
                                fEntityManager.startEntity(true, entityName, true);
                            }
                        }
                    }
                } else if (c == '<') {
                    reportFatalError("LessthanInAttValue",
                            new Object[] { eleName, atName });
                            fEntityScanner.scanChar(null);
                            if (entityDepth == fEntityDepth && fNeedNonNormalizedValue) {
                                fStringBuffer2.append((char)c);
                            }
                } else if (c == '%' || c == ']') {
                    fEntityScanner.scanChar(null);
                    stringBuffer.append((char)c);
                    if (entityDepth == fEntityDepth && fNeedNonNormalizedValue) {
                        fStringBuffer2.append((char)c);
                    }
                    if (DEBUG_ATTR_NORMALIZATION) {
                        System.out.println("** valueF: \""
                                + stringBuffer.toString() + "\"");
                    }
                } else if (c != -1 && XMLChar.isHighSurrogate(c)) {
                    fStringBuffer3.clear();
                    if (scanSurrogates(fStringBuffer3)) {
                        stringBuffer.append(fStringBuffer3);
                        if (entityDepth == fEntityDepth && fNeedNonNormalizedValue) {
                            fStringBuffer2.append(fStringBuffer3);
                        }
                        if (DEBUG_ATTR_NORMALIZATION) {
                            System.out.println("** valueI: \""
                                    + stringBuffer.toString()
                                    + "\"");
                        }
                    }
                } else if (c != -1 && isInvalidLiteral(c)) {
                    reportFatalError("InvalidCharInAttValue",
                            new Object[] {eleName, atName, Integer.toString(c, 16)});
                            fEntityScanner.scanChar(null);
                            if (entityDepth == fEntityDepth && fNeedNonNormalizedValue) {
                                fStringBuffer2.append((char)c);
                            }
                }
                c = fEntityScanner.scanLiteral(quote, value, isNSURI);
                if (entityDepth == fEntityDepth && fNeedNonNormalizedValue) {
                    fStringBuffer2.append(value);
                }
                if(fEntityScanner.whiteSpaceLen > 0)
                    normalizeWhitespace(value);
            } while (c != quote || entityDepth != fEntityDepth);
            stringBuffer.append(value);
            if (DEBUG_ATTR_NORMALIZATION) {
                System.out.println("** valueN: \""
                        + stringBuffer.toString() + "\"");
            }
            value.setValues(stringBuffer);
            fScanningAttribute = false;
        }
        if(fNeedNonNormalizedValue)
            nonNormalizedValue.setValues(fStringBuffer2);

        int cquote = fEntityScanner.scanChar(NameType.ATTRIBUTE);
        if (cquote != quote) {
            reportFatalError("CloseQuoteExpected", new Object[]{eleName, atName});
        }
    } 


    /**
     * Resolves character entity references.
     * @param entityName the name of the entity
     * @param stringBuffer the current XMLStringBuffer to append the character to.
     * @return true if resolved, false otherwise
     */
    protected boolean resolveCharacter(String entityName, XMLStringBuffer stringBuffer) {
        /**
         * entityNames (symbols) are interned. The equals method would do the same,
         * but I'm leaving it as comparisons by references are common in the impl
         * and it made it explicit to others who read this code.
         */
        if (entityName == fAmpSymbol) {
            stringBuffer.append('&');
            return true;
        } else if (entityName == fAposSymbol) {
            stringBuffer.append('\'');
            return true;
        } else if (entityName == fLtSymbol) {
            stringBuffer.append('<');
            return true;
        } else if (entityName == fGtSymbol) {
            checkEntityLimit(false, fEntityScanner.fCurrentEntity.name, 1);
            stringBuffer.append('>');
            return true;
        } else if (entityName == fQuotSymbol) {
            checkEntityLimit(false, fEntityScanner.fCurrentEntity.name, 1);
            stringBuffer.append('"');
            return true;
        }
        return false;
    }

    /**
     * Scans External ID and return the public and system IDs.
     *
     * @param identifiers An array of size 2 to return the system id,
     *                    and public id (in that order).
     * @param optionalSystemId Specifies whether the system id is optional.
     *
     * <strong>Note:</strong> This method uses fString and fStringBuffer,
     * anything in them at the time of calling is lost.
     */
    protected void scanExternalID(String[] identifiers,
            boolean optionalSystemId)
            throws IOException, XNIException {

        String systemId = null;
        String publicId = null;
        if (fEntityScanner.skipString("PUBLIC")) {
            if (!fEntityScanner.skipSpaces()) {
                reportFatalError("SpaceRequiredAfterPUBLIC", null);
            }
            scanPubidLiteral(fString);
            publicId = fString.toString();

            if (!fEntityScanner.skipSpaces() && !optionalSystemId) {
                reportFatalError("SpaceRequiredBetweenPublicAndSystem", null);
            }
        }

        if (publicId != null || fEntityScanner.skipString("SYSTEM")) {
            if (publicId == null && !fEntityScanner.skipSpaces()) {
                reportFatalError("SpaceRequiredAfterSYSTEM", null);
            }
            int quote = fEntityScanner.peekChar();
            if (quote != '\'' && quote != '"') {
                if (publicId != null && optionalSystemId) {
                    identifiers[0] = null;
                    identifiers[1] = publicId;
                    return;
                }
                reportFatalError("QuoteRequiredInSystemID", null);
            }
            fEntityScanner.scanChar(null);
            XMLString ident = fString;
            if (fEntityScanner.scanLiteral(quote, ident, false) != quote) {
                fStringBuffer.clear();
                do {
                    fStringBuffer.append(ident);
                    int c = fEntityScanner.peekChar();
                    if (XMLChar.isMarkup(c) || c == ']') {
                        fStringBuffer.append((char)fEntityScanner.scanChar(null));
                    } else if (c != -1 && isInvalidLiteral(c)) {
                        reportFatalError("InvalidCharInSystemID",
                            new Object[] {Integer.toString(c, 16)});
                    }
                } while (fEntityScanner.scanLiteral(quote, ident, false) != quote);
                fStringBuffer.append(ident);
                ident = fStringBuffer;
            }
            systemId = ident.toString();
            if (!fEntityScanner.skipChar(quote, null)) {
                reportFatalError("SystemIDUnterminated", null);
            }
        }

        identifiers[0] = systemId;
        identifiers[1] = publicId;
    }


    /**
     * Scans public ID literal.
     *
     * [12] PubidLiteral ::= '"' PubidChar* '"' | "'" (PubidChar - "'")* "'"
     * [13] PubidChar::= #x20 | #xD | #xA | [a-zA-Z0-9] | [-'()+,./:=?;!*#@$_%]
     *
     * The returned string is normalized according to the following rule,
     * from http:
     *
     * Before a match is attempted, all strings of white space in the public
     * identifier must be normalized to single space characters (#x20), and
     * leading and trailing white space must be removed.
     *
     * @param literal The string to fill in with the public ID literal.
     * @return True on success.
     *
     * <strong>Note:</strong> This method uses fStringBuffer, anything in it at
     * the time of calling is lost.
     */
    protected boolean scanPubidLiteral(XMLString literal)
    throws IOException, XNIException {
        int quote = fEntityScanner.scanChar(null);
        if (quote != '\'' && quote != '"') {
            reportFatalError("QuoteRequiredInPublicID", null);
            return false;
        }

        fStringBuffer.clear();
        boolean skipSpace = true;
        boolean dataok = true;
        while (true) {
            int c = fEntityScanner.scanChar(null);
            if (c == ' ' || c == '\n' || c == '\r') {
                if (!skipSpace) {
                    fStringBuffer.append(' ');
                    skipSpace = true;
                }
            } else if (c == quote) {
                if (skipSpace) {
                    fStringBuffer.length--;
                }
                literal.setValues(fStringBuffer);
                break;
            } else if (XMLChar.isPubid(c)) {
                fStringBuffer.append((char)c);
                skipSpace = false;
            } else if (c == -1) {
                reportFatalError("PublicIDUnterminated", null);
                return false;
            } else {
                dataok = false;
                reportFatalError("InvalidCharInPublicID",
                        new Object[]{Integer.toHexString(c)});
            }
        }
        return dataok;
    }


    /**
     * Normalize whitespace in an XMLString converting all whitespace
     * characters to space characters.
     */
    protected void normalizeWhitespace(XMLString value) {
        int i=0;
        int j=0;
        int [] buff = fEntityScanner.whiteSpaceLookup;
        int buffLen = fEntityScanner.whiteSpaceLen;
        int end = value.offset + value.length;
        while(i < buffLen){
            j = buff[i];
            if(j < end ){
                value.ch[j] = ' ';
            }
            i++;
        }
    }


    /**
     * This method notifies of the start of an entity. The document entity
     * has the pseudo-name of "[xml]" the DTD has the pseudo-name of "[dtd]"
     * parameter entity names start with '%'; and general entities are just
     * specified by their name.
     *
     * @param name     The name of the entity.
     * @param identifier The resource identifier.
     * @param encoding The auto-detected IANA encoding name of the entity
     *                 stream. This value will be null in those situations
     *                 where the entity encoding is not auto-detected (e.g.
     *                 internal entities or a document entity that is
     *                 parsed from a java.io.Reader).
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startEntity(String name,
            XMLResourceIdentifier identifier,
            String encoding, Augmentations augs) throws XNIException {

        fEntityDepth++;
        fEntityScanner = fEntityManager.getEntityScanner();
        fEntityStore = fEntityManager.getEntityStore() ;
    } 

    /**
     * This method notifies the end of an entity. The document entity has
     * the pseudo-name of "[xml]" the DTD has the pseudo-name of "[dtd]"
     * parameter entity names start with '%'; and general entities are just
     * specified by their name.
     *
     * @param name The name of the entity.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endEntity(String name, Augmentations augs) throws IOException, XNIException {
        if (fEntityDepth > 0) {
            fEntityDepth--;
        }
    } 

    /**
     * Scans a character reference and append the corresponding chars to the
     * specified buffer.
     *
     * <p>
     * <pre>
     * [66] CharRef ::= '&#' [0-9]+ ';' | '&#x' [0-9a-fA-F]+ ';'
     * </pre>
     *
     * <strong>Note:</strong> This method uses fStringBuffer, anything in it
     * at the time of calling is lost.
     *
     * @param buf the character buffer to append chars to
     * @param buf2 the character buffer to append non-normalized chars to
     *
     * @return the character value or (-1) on conversion failure
     */
    protected int scanCharReferenceValue(XMLStringBuffer buf, XMLStringBuffer buf2)
    throws IOException, XNIException {
        int initLen = buf.length;
        boolean hex = false;
        if (fEntityScanner.skipChar('x', NameType.REFERENCE)) {
            if (buf2 != null) { buf2.append('x'); }
            hex = true;
            fStringBuffer3.clear();
            boolean digit = true;

            int c = fEntityScanner.peekChar();
            digit = (c >= '0' && c <= '9') ||
                    (c >= 'a' && c <= 'f') ||
                    (c >= 'A' && c <= 'F');
            if (digit) {
                if (buf2 != null) { buf2.append((char)c); }
                fEntityScanner.scanChar(NameType.REFERENCE);
                fStringBuffer3.append((char)c);

                do {
                    c = fEntityScanner.peekChar();
                    digit = (c >= '0' && c <= '9') ||
                            (c >= 'a' && c <= 'f') ||
                            (c >= 'A' && c <= 'F');
                    if (digit) {
                        if (buf2 != null) { buf2.append((char)c); }
                        fEntityScanner.scanChar(NameType.REFERENCE);
                        fStringBuffer3.append((char)c);
                    }
                } while (digit);
            } else {
                reportFatalError("HexdigitRequiredInCharRef", null);
            }
        }

        else {
            fStringBuffer3.clear();
            boolean digit = true;

            int c = fEntityScanner.peekChar();
            digit = c >= '0' && c <= '9';
            if (digit) {
                if (buf2 != null) { buf2.append((char)c); }
                fEntityScanner.scanChar(NameType.REFERENCE);
                fStringBuffer3.append((char)c);

                do {
                    c = fEntityScanner.peekChar();
                    digit = c >= '0' && c <= '9';
                    if (digit) {
                        if (buf2 != null) { buf2.append((char)c); }
                        fEntityScanner.scanChar(NameType.REFERENCE);
                        fStringBuffer3.append((char)c);
                    }
                } while (digit);
            } else {
                reportFatalError("DigitRequiredInCharRef", null);
            }
        }

        if (!fEntityScanner.skipChar(';', NameType.REFERENCE)) {
            reportFatalError("SemicolonRequiredInCharRef", null);
        }
        if (buf2 != null) { buf2.append(';'); }

        int value = -1;
        try {
            value = Integer.parseInt(fStringBuffer3.toString(),
                    hex ? 16 : 10);

            if (isInvalid(value)) {
                StringBuffer errorBuf = new StringBuffer(fStringBuffer3.length + 1);
                if (hex) errorBuf.append('x');
                errorBuf.append(fStringBuffer3.ch, fStringBuffer3.offset, fStringBuffer3.length);
                reportFatalError("InvalidCharRef",
                        new Object[]{errorBuf.toString()});
            }
        } catch (NumberFormatException e) {
            StringBuffer errorBuf = new StringBuffer(fStringBuffer3.length + 1);
            if (hex) errorBuf.append('x');
            errorBuf.append(fStringBuffer3.ch, fStringBuffer3.offset, fStringBuffer3.length);
            reportFatalError("InvalidCharRef",
                    new Object[]{errorBuf.toString()});
        }

        if (!XMLChar.isSupplemental(value)) {
            buf.append((char) value);
        } else {
            buf.append(XMLChar.highSurrogate(value));
            buf.append(XMLChar.lowSurrogate(value));
        }

        if (fNotifyCharRefs && value != -1) {
            String literal = "#" + (hex ? "x" : "") + fStringBuffer3.toString();
            if (!fScanningAttribute) {
                fCharRefLiteral = literal;
            }
        }

        if (fEntityScanner.fCurrentEntity.isGE) {
            checkEntityLimit(false, fEntityScanner.fCurrentEntity.name, buf.length - initLen);
        }
        return value;
    }
    protected boolean isInvalid(int value) {
        return (XMLChar.isInvalid(value));
    } 

    protected boolean isInvalidLiteral(int value) {
        return (XMLChar.isInvalid(value));
    } 

    protected boolean isValidNameChar(int value) {
        return (XMLChar.isName(value));
    } 

    protected boolean isValidNCName(int value) {
        return (XMLChar.isNCName(value));
    } 

    protected boolean isValidNameStartChar(int value) {
        return (XMLChar.isNameStart(value));
    } 

    protected boolean isValidNameStartHighSurrogate(int value) {
        return false;
    } 

    protected boolean versionSupported(String version ) {
        return version.equals("1.0") || version.equals("1.1");
    } 

    /**
     * Scans surrogates and append them to the specified buffer.
     * <p>
     * <strong>Note:</strong> This assumes the current char has already been
     * identified as a high surrogate.
     *
     * @param buf The StringBuffer to append the read surrogates to.
     * @return True if it succeeded.
     */
    protected boolean scanSurrogates(XMLStringBuffer buf)
    throws IOException, XNIException {

        int high = fEntityScanner.scanChar(null);
        int low = fEntityScanner.peekChar();
        if (!XMLChar.isLowSurrogate(low)) {
            reportFatalError("InvalidCharInContent",
                    new Object[] {Integer.toString(high, 16)});
                    return false;
        }
        fEntityScanner.scanChar(null);

        int c = XMLChar.supplemental((char)high, (char)low);

        if (isInvalid(c)) {
            reportFatalError("InvalidCharInContent",
                    new Object[]{Integer.toString(c, 16)});
                    return false;
        }

        buf.append((char)high);
        buf.append((char)low);

        return true;

    } 


    /**
     * Convenience function used in all XML scanners.
     */
    protected void reportFatalError(String msgId, Object[] args)
    throws XNIException {
        fErrorReporter.reportError(fEntityScanner, XMLMessageFormatter.XML_DOMAIN,
                msgId, args,
                XMLErrorReporter.SEVERITY_FATAL_ERROR);
    }

    private void init() {
        fEntityScanner = null;
        fEntityDepth = 0;
        fReportEntity = true;
        fResourceIdentifier.clear();

        if(!fAttributeCacheInitDone){
            for(int i = 0; i < initialCacheCount; i++){
                attributeValueCache.add(new XMLString());
                stringBufferCache.add(new XMLStringBuffer());
            }
            fAttributeCacheInitDone = true;
        }
        fStringBufferIndex = 0;
        fAttributeCacheUsedCount = 0;

    }

    XMLStringBuffer getStringBuffer(){
        if((fStringBufferIndex < initialCacheCount )|| (fStringBufferIndex < stringBufferCache.size())){
            return stringBufferCache.get(fStringBufferIndex++);
        }else{
            XMLStringBuffer tmpObj = new XMLStringBuffer();
            fStringBufferIndex++;
            stringBufferCache.add(tmpObj);
            return tmpObj;
        }
    }

    /**
     * Add the count of the content buffer and check if the accumulated
     * value exceeds the limit
     * @param isPEDecl a flag to indicate whether the entity is parameter
     * @param entityName entity name
     * @param buffer content buffer
     */
    void checkEntityLimit(boolean isPEDecl, String entityName, XMLString buffer) {
        checkEntityLimit(isPEDecl, entityName, buffer.length);
    }

    /**
     * Add the count and check limit
     * @param isPEDecl a flag to indicate whether the entity is parameter
     * @param entityName entity name
     * @param len length of the buffer
     */
    void checkEntityLimit(boolean isPEDecl, String entityName, int len) {
        if (fLimitAnalyzer == null) {
            fLimitAnalyzer = fEntityManager.fLimitAnalyzer;
        }
        if (isPEDecl) {
            fLimitAnalyzer.addValue(XMLSecurityManager.Limit.PARAMETER_ENTITY_SIZE_LIMIT, "%" + entityName, len);
            if (fSecurityManager.isOverLimit(XMLSecurityManager.Limit.PARAMETER_ENTITY_SIZE_LIMIT, fLimitAnalyzer)) {
                        fSecurityManager.debugPrint(fLimitAnalyzer);
                reportFatalError("MaxEntitySizeLimit", new Object[]{"%" + entityName,
                    fLimitAnalyzer.getValue(XMLSecurityManager.Limit.PARAMETER_ENTITY_SIZE_LIMIT),
                    fSecurityManager.getLimit(XMLSecurityManager.Limit.PARAMETER_ENTITY_SIZE_LIMIT),
                    fSecurityManager.getStateLiteral(XMLSecurityManager.Limit.PARAMETER_ENTITY_SIZE_LIMIT)});
            }
        } else {
            fLimitAnalyzer.addValue(XMLSecurityManager.Limit.GENERAL_ENTITY_SIZE_LIMIT, entityName, len);
            if (fSecurityManager.isOverLimit(XMLSecurityManager.Limit.GENERAL_ENTITY_SIZE_LIMIT, fLimitAnalyzer)) {
                        fSecurityManager.debugPrint(fLimitAnalyzer);
                reportFatalError("MaxEntitySizeLimit", new Object[]{entityName,
                    fLimitAnalyzer.getValue(XMLSecurityManager.Limit.GENERAL_ENTITY_SIZE_LIMIT),
                    fSecurityManager.getLimit(XMLSecurityManager.Limit.GENERAL_ENTITY_SIZE_LIMIT),
                    fSecurityManager.getStateLiteral(XMLSecurityManager.Limit.GENERAL_ENTITY_SIZE_LIMIT)});
            }
        }
        if (fSecurityManager.isOverLimit(XMLSecurityManager.Limit.TOTAL_ENTITY_SIZE_LIMIT, fLimitAnalyzer)) {
            fSecurityManager.debugPrint(fLimitAnalyzer);
            reportFatalError("TotalEntitySizeLimit",
                new Object[]{fLimitAnalyzer.getTotalValue(XMLSecurityManager.Limit.TOTAL_ENTITY_SIZE_LIMIT),
                fSecurityManager.getLimit(XMLSecurityManager.Limit.TOTAL_ENTITY_SIZE_LIMIT),
                fSecurityManager.getStateLiteral(XMLSecurityManager.Limit.TOTAL_ENTITY_SIZE_LIMIT)});
        }
    }
} 
