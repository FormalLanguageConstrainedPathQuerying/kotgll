/*
 * Copyright (c) 2006, 2023, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.xinclude;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.io.MalformedByteSequenceException;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.parsers.XIncludeParserConfiguration;
import com.sun.org.apache.xerces.internal.parsers.XPointerParserConfiguration;
import com.sun.org.apache.xerces.internal.util.AugmentationsImpl;
import com.sun.org.apache.xerces.internal.util.HTTPInputSource;
import com.sun.org.apache.xerces.internal.util.IntStack;
import com.sun.org.apache.xerces.internal.util.ParserConfigurationSettings;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.URI.MalformedURIException;
import com.sun.org.apache.xerces.internal.util.URI;
import com.sun.org.apache.xerces.internal.util.XMLAttributesImpl;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLLocatorWrapper;
import com.sun.org.apache.xerces.internal.util.XMLResourceIdentifierImpl;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLDTDHandler;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDTDFilter;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDTDSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentFilter;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParserConfiguration;
import com.sun.org.apache.xerces.internal.xpointer.XPointerHandler;
import com.sun.org.apache.xerces.internal.xpointer.XPointerProcessor;
import java.io.CharConversionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;
import java.util.StringTokenizer;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import javax.xml.transform.Source;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkXmlUtils;
import jdk.xml.internal.XMLSecurityManager;
import org.xml.sax.InputSource;

/**
 * <p>
 * This is a pipeline component which performs XInclude handling, according to the
 * W3C specification for XML Inclusions.
 * </p>
 * <p>
 * This component analyzes each event in the pipeline, looking for &lt;include&gt;
 * elements. An &lt;include&gt; element is one which has a namespace of
 * <code>http:
 * When it finds an &lt;include&gt; element, it attempts to include the file specified
 * in the <code>href</code> attribute of the element.  If inclusion succeeds, all
 * children of the &lt;include&gt; element are ignored (with the exception of
 * checking for invalid children as outlined in the specification).  If the inclusion
 * fails, the &lt;fallback&gt; child of the &lt;include&gt; element is processed.
 * </p>
 * <p>
 * See the <a href="http:
 * more information on how XInclude is to be used.
 * </p>
 * <p>
 * This component requires the following features and properties from the
 * component manager that uses it:
 * <ul>
 *  <li>http:
 *  <li>http:
 *  <li>http:
 * </ul>
 * Optional property:
 * <ul>
 *  <li>http:
 * </ul>
 *
 * Furthermore, the <code>NamespaceContext</code> used in the pipeline is required
 * to be an instance of <code>XIncludeNamespaceSupport</code>.
 * </p>
 * <p>
 * Currently, this implementation has only partial support for the XInclude specification.
 * Specifically, it is missing support for XPointer document fragments.  Thus, only whole
 * documents can be included using this component in the pipeline.
 * </p>
 *
 * @author Peter McCracken, IBM
 * @author Michael Glavassevich, IBM
 *
 *
 * @see XIncludeNamespaceSupport
 * @LastModified: July 2023
 */
public class XIncludeHandler
    implements XMLComponent, XMLDocumentFilter, XMLDTDFilter {

    public final static String HTTP_ACCEPT = "Accept";
    public final static String HTTP_ACCEPT_LANGUAGE = "Accept-Language";
    public final static String XPOINTER = "xpointer";

    public final static String XINCLUDE_NS_URI =
        "http:
    public final static String XINCLUDE_INCLUDE = "include".intern();
    public final static String XINCLUDE_FALLBACK = "fallback".intern();

    public final static String XINCLUDE_PARSE_XML = "xml".intern();
    public final static String XINCLUDE_PARSE_TEXT = "text".intern();

    public final static String XINCLUDE_ATTR_HREF = "href".intern();
    public final static String XINCLUDE_ATTR_PARSE = "parse".intern();
    public final static String XINCLUDE_ATTR_ENCODING = "encoding".intern();
    public final static String XINCLUDE_ATTR_ACCEPT = "accept".intern();
    public final static String XINCLUDE_ATTR_ACCEPT_LANGUAGE = "accept-language".intern();

    public final static String XINCLUDE_INCLUDED = "[included]".intern();

    /** The identifier for the Augmentation that contains the current base URI */
    public final static String CURRENT_BASE_URI = "currentBaseURI";

    private final static String XINCLUDE_BASE = "base".intern();
    private final static QName XML_BASE_QNAME =
        new QName(
            XMLSymbols.PREFIX_XML,
            XINCLUDE_BASE,
            (XMLSymbols.PREFIX_XML + ":" + XINCLUDE_BASE).intern(),
            NamespaceContext.XML_URI);

    private final static String XINCLUDE_LANG = "lang".intern();
    private final static QName XML_LANG_QNAME =
        new QName(
            XMLSymbols.PREFIX_XML,
            XINCLUDE_LANG,
            (XMLSymbols.PREFIX_XML + ":" + XINCLUDE_LANG).intern(),
            NamespaceContext.XML_URI);

    private final static QName NEW_NS_ATTR_QNAME =
        new QName(
            XMLSymbols.PREFIX_XMLNS,
            "",
            XMLSymbols.PREFIX_XMLNS + ":",
            NamespaceContext.XMLNS_URI);

    private final static int STATE_NORMAL_PROCESSING = 1;
    private final static int STATE_IGNORE = 2;
    private final static int STATE_EXPECT_FALLBACK = 3;


    /** Feature identifier: validation. */
    protected static final String VALIDATION =
        Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

    /** Feature identifier: schema validation. */
    protected static final String SCHEMA_VALIDATION =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_VALIDATION_FEATURE;

    /** Feature identifier: dynamic validation. */
    protected static final String DYNAMIC_VALIDATION =
        Constants.XERCES_FEATURE_PREFIX + Constants.DYNAMIC_VALIDATION_FEATURE;

    /** Feature identifier: allow notation and unparsed entity events to be sent out of order. */
    protected static final String ALLOW_UE_AND_NOTATION_EVENTS =
        Constants.SAX_FEATURE_PREFIX
            + Constants.ALLOW_DTD_EVENTS_AFTER_ENDDTD_FEATURE;

    /** Feature identifier: fixup base URIs. */
    protected static final String XINCLUDE_FIXUP_BASE_URIS =
        Constants.XERCES_FEATURE_PREFIX + Constants.XINCLUDE_FIXUP_BASE_URIS_FEATURE;

    /** Feature identifier: fixup language. */
    protected static final String XINCLUDE_FIXUP_LANGUAGE =
        Constants.XERCES_FEATURE_PREFIX + Constants.XINCLUDE_FIXUP_LANGUAGE_FEATURE;

    /** Property identifier: JAXP schema language. */
    protected static final String JAXP_SCHEMA_LANGUAGE =
        Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_LANGUAGE;

    /** Property identifier: symbol table. */
    protected static final String SYMBOL_TABLE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property identifier: error reporter. */
    protected static final String ERROR_REPORTER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Property identifier: entity resolver. */
    protected static final String ENTITY_RESOLVER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

    /** property identifier: security manager. */
    protected static final String SECURITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY;

    /** property identifier: buffer size. */
    protected static final String BUFFER_SIZE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.BUFFER_SIZE_PROPERTY;

    protected static final String PARSER_SETTINGS =
        Constants.XERCES_FEATURE_PREFIX + Constants.PARSER_SETTINGS;

    /** property identifier: XML security property manager. */
    protected static final String XML_SECURITY_PROPERTY_MANAGER =
            JdkConstants.XML_SECURITY_PROPERTY_MANAGER;

    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES =
        { ALLOW_UE_AND_NOTATION_EVENTS, XINCLUDE_FIXUP_BASE_URIS, XINCLUDE_FIXUP_LANGUAGE };

    /** Feature defaults. */
    private static final Boolean[] FEATURE_DEFAULTS = { Boolean.TRUE, Boolean.TRUE, Boolean.TRUE };

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES =
        { ERROR_REPORTER, ENTITY_RESOLVER, SECURITY_MANAGER, BUFFER_SIZE };

    /** Property defaults. */
    private static final Object[] PROPERTY_DEFAULTS = { null, null, null, XMLEntityManager.DEFAULT_BUFFER_SIZE};


    protected XMLDocumentHandler fDocumentHandler;
    protected XMLDocumentSource fDocumentSource;

    protected XMLDTDHandler fDTDHandler;
    protected XMLDTDSource fDTDSource;

    protected XIncludeHandler fParentXIncludeHandler;

    protected int fBufferSize = XMLEntityManager.DEFAULT_BUFFER_SIZE;

    protected String fParentRelativeURI;

    protected XMLParserConfiguration fChildConfig;

    protected XMLParserConfiguration fXIncludeChildConfig;
    protected XMLParserConfiguration fXPointerChildConfig;

    protected XPointerProcessor fXPtrProcessor = null;

    protected XMLLocator fDocLocation;
    protected XMLLocatorWrapper fXIncludeLocator = new XMLLocatorWrapper();
    protected XIncludeMessageFormatter fXIncludeMessageFormatter = new XIncludeMessageFormatter();
    protected XIncludeNamespaceSupport fNamespaceContext;
    protected SymbolTable fSymbolTable;
    protected XMLErrorReporter fErrorReporter;
    protected XMLEntityResolver fEntityResolver;
    protected XMLSecurityManager fSecurityManager;
    protected XMLSecurityPropertyManager fSecurityPropertyMgr;

    protected XIncludeTextReader fXInclude10TextReader;
    protected XIncludeTextReader fXInclude11TextReader;

    protected final XMLResourceIdentifier fCurrentBaseURI;
    protected final IntStack fBaseURIScope;
    protected final Stack<String> fBaseURI;
    protected final Stack<String> fLiteralSystemID;
    protected final Stack<String> fExpandedSystemID;

    protected final IntStack fLanguageScope;
    protected final Stack<String> fLanguageStack;
    protected String fCurrentLanguage;

    protected String fHrefFromParent;

    protected ParserConfigurationSettings fSettings;

    private int fDepth;

    private int fResultDepth;

    private static final int INITIAL_SIZE = 8;

    private boolean[] fSawInclude = new boolean[INITIAL_SIZE];

    private boolean[] fSawFallback = new boolean[INITIAL_SIZE];

    private int[] fState = new int[INITIAL_SIZE];

    private final List<Notation> fNotations;
    private final List<UnparsedEntity> fUnparsedEntities;

    private boolean fFixupBaseURIs = true;
    private boolean fFixupLanguage = true;

    private boolean fSendUEAndNotationEvents;

    private boolean fIsXML11;

    private boolean fInDTD;

    boolean fHasIncludeReportedContent;

    private boolean fSeenRootElement;

    private boolean fNeedCopyFeatures = true;

    /** indicate whether Catalog should be used for resolving external resources */
    private boolean fUseCatalog = true;
    CatalogFeatures fCatalogFeatures;
    CatalogResolver fCatalogResolver;

    private String fCatalogFile;
    private String fDefer;
    private String fPrefer;
    private String fResolve;


    public XIncludeHandler() {
        fDepth = 0;

        fSawFallback[fDepth] = false;
        fSawInclude[fDepth] = false;
        fState[fDepth] = STATE_NORMAL_PROCESSING;
        fNotations = new ArrayList<>();
        fUnparsedEntities = new ArrayList<>();

        fBaseURIScope = new IntStack();
        fBaseURI = new Stack<>();
        fLiteralSystemID = new Stack<>();
        fExpandedSystemID = new Stack<>();
        fCurrentBaseURI = new XMLResourceIdentifierImpl();

        fLanguageScope = new IntStack();
        fLanguageStack = new Stack<>();
        fCurrentLanguage = null;
    }


    @Override
    public void reset(XMLComponentManager componentManager)
        throws XNIException {
        fNamespaceContext = null;
        fDepth = 0;
        fResultDepth = isRootDocument() ? 0 : fParentXIncludeHandler.getResultDepth();
        fNotations.clear();
        fUnparsedEntities.clear();
        fParentRelativeURI = null;
        fIsXML11 = false;
        fInDTD = false;
        fSeenRootElement = false;

        fBaseURIScope.clear();
        fBaseURI.clear();
        fLiteralSystemID.clear();
        fExpandedSystemID.clear();
        fLanguageScope.clear();
        fLanguageStack.clear();

        for (int i = 0; i < fState.length; ++i) {
            fState[i] = STATE_NORMAL_PROCESSING;
        }
        for (int i = 0; i < fSawFallback.length; ++i) {
            fSawFallback[i] = false;
        }
        for (int i = 0; i < fSawInclude.length; ++i) {
            fSawInclude[i] = false;
        }

        try {
            if (!componentManager.getFeature(PARSER_SETTINGS)) {
                return;
            }
        }
        catch (XMLConfigurationException e) {}

        fNeedCopyFeatures = true;

        try {
            fSendUEAndNotationEvents =
                componentManager.getFeature(ALLOW_UE_AND_NOTATION_EVENTS);
            if (fChildConfig != null) {
                fChildConfig.setFeature(
                    ALLOW_UE_AND_NOTATION_EVENTS,
                    fSendUEAndNotationEvents);
            }
        }
        catch (XMLConfigurationException e) {
        }

        try {
            fFixupBaseURIs =
                componentManager.getFeature(XINCLUDE_FIXUP_BASE_URIS);
            if (fChildConfig != null) {
                fChildConfig.setFeature(
                    XINCLUDE_FIXUP_BASE_URIS,
                    fFixupBaseURIs);
            }
        }
        catch (XMLConfigurationException e) {
            fFixupBaseURIs = true;
        }

        try {
            fFixupLanguage =
                componentManager.getFeature(XINCLUDE_FIXUP_LANGUAGE);
            if (fChildConfig != null) {
                fChildConfig.setFeature(
                    XINCLUDE_FIXUP_LANGUAGE,
                    fFixupLanguage);
            }
        }
        catch (XMLConfigurationException e) {
            fFixupLanguage = true;
        }

        try {
            SymbolTable value =
                (SymbolTable)componentManager.getProperty(SYMBOL_TABLE);
            if (value != null) {
                fSymbolTable = value;
                if (fChildConfig != null) {
                    fChildConfig.setProperty(SYMBOL_TABLE, value);
                }
            }
        }
        catch (XMLConfigurationException e) {
            fSymbolTable = null;
        }

        try {
            XMLErrorReporter value =
                (XMLErrorReporter)componentManager.getProperty(ERROR_REPORTER);
            if (value != null) {
                setErrorReporter(value);
                if (fChildConfig != null) {
                    fChildConfig.setProperty(ERROR_REPORTER, value);
                }
            }
        }
        catch (XMLConfigurationException e) {
            fErrorReporter = null;
        }

        try {
            XMLEntityResolver value =
                (XMLEntityResolver)componentManager.getProperty(
                    ENTITY_RESOLVER);

            if (value != null) {
                fEntityResolver = value;
                if (fChildConfig != null) {
                    fChildConfig.setProperty(ENTITY_RESOLVER, value);
                }
            }
        }
        catch (XMLConfigurationException e) {
            fEntityResolver = null;
        }

        try {
            XMLSecurityManager value =
                (XMLSecurityManager)componentManager.getProperty(
                    SECURITY_MANAGER);

            if (value != null) {
                fSecurityManager = value;
                if (fChildConfig != null) {
                    fChildConfig.setProperty(SECURITY_MANAGER, value);
                }
            }
        }
        catch (XMLConfigurationException e) {
            fSecurityManager = null;
        }

        fSecurityPropertyMgr = (XMLSecurityPropertyManager)
                componentManager.getProperty(JdkConstants.XML_SECURITY_PROPERTY_MANAGER);

        fUseCatalog = componentManager.getFeature(XMLConstants.USE_CATALOG);
        fCatalogFile = (String)componentManager.getProperty(CatalogFeatures.Feature.FILES.getPropertyName());
        fDefer = (String)componentManager.getProperty(CatalogFeatures.Feature.DEFER.getPropertyName());
        fPrefer = (String)componentManager.getProperty(CatalogFeatures.Feature.PREFER.getPropertyName());
        fResolve = (String)componentManager.getProperty(CatalogFeatures.Feature.RESOLVE.getPropertyName());

        try {
            Integer value =
                (Integer)componentManager.getProperty(
                    BUFFER_SIZE);

            if (value != null && value > 0) {
                fBufferSize = value;
                if (fChildConfig != null) {
                    fChildConfig.setProperty(BUFFER_SIZE, value);
                }
            }
            else {
                fBufferSize = ((Integer)getPropertyDefault(BUFFER_SIZE));
            }
        }
        catch (XMLConfigurationException e) {
            fBufferSize = ((Integer)getPropertyDefault(BUFFER_SIZE));
        }

        if (fXInclude10TextReader != null) {
                fXInclude10TextReader.setBufferSize(fBufferSize);
        }
        if (fXInclude11TextReader != null) {
            fXInclude11TextReader.setBufferSize(fBufferSize);
        }

        fSettings = new ParserConfigurationSettings();
        copyFeatures(componentManager, fSettings);

        try {
            if (componentManager.getFeature(SCHEMA_VALIDATION)) {
                fSettings.setFeature(SCHEMA_VALIDATION, false);
                if (Constants.NS_XMLSCHEMA.equals(componentManager.getProperty(JAXP_SCHEMA_LANGUAGE))) {
                    fSettings.setFeature(VALIDATION, false);
                }
                else if (componentManager.getFeature(VALIDATION)) {
                    fSettings.setFeature(DYNAMIC_VALIDATION, true);
                }
            }
        }
        catch (XMLConfigurationException e) {}

    } 

    /**
     * Returns a list of feature identifiers that are recognized by
     * this component. This method may return null if no features
     * are recognized by this component.
     */
    @Override
    public String[] getRecognizedFeatures() {
        return RECOGNIZED_FEATURES.clone();
    } 

    /**
     * Sets the state of a feature. This method is called by the component
     * manager any time after reset when a feature changes state.
     * <p>
     * <strong>Note:</strong> Components should silently ignore features
     * that do not affect the operation of the component.
     *
     * @param featureId The feature identifier.
     * @param state     The state of the feature.
     *
     * @throws SAXNotRecognizedException The component should not throw
     *                                   this exception.
     * @throws SAXNotSupportedException The component should not throw
     *                                  this exception.
     */
    @Override
    public void setFeature(String featureId, boolean state)
        throws XMLConfigurationException {
        if (featureId.equals(ALLOW_UE_AND_NOTATION_EVENTS)) {
            fSendUEAndNotationEvents = state;
        }
        if (fSettings != null) {
            fNeedCopyFeatures = true;
            fSettings.setFeature(featureId, state);
        }
    } 

    /**
     * Returns a list of property identifiers that are recognized by
     * this component. This method may return null if no properties
     * are recognized by this component.
     */
    @Override
    public String[] getRecognizedProperties() {
        return RECOGNIZED_PROPERTIES.clone();
    } 

    /**
     * Sets the value of a property. This method is called by the component
     * manager any time after reset when a property changes value.
     * <p>
     * <strong>Note:</strong> Components should silently ignore properties
     * that do not affect the operation of the component.
     *
     * @param propertyId The property identifier.
     * @param value      The value of the property.
     *
     * @throws SAXNotRecognizedException The component should not throw
     *                                   this exception.
     * @throws SAXNotSupportedException The component should not throw
     *                                  this exception.
     */
    @Override
    public void setProperty(String propertyId, Object value)
        throws XMLConfigurationException {
        if (propertyId.equals(SYMBOL_TABLE)) {
            fSymbolTable = (SymbolTable)value;
            if (fChildConfig != null) {
                fChildConfig.setProperty(propertyId, value);
            }
            return;
        }
        if (propertyId.equals(ERROR_REPORTER)) {
            setErrorReporter((XMLErrorReporter)value);
            if (fChildConfig != null) {
                fChildConfig.setProperty(propertyId, value);
            }
            return;
        }
        if (propertyId.equals(ENTITY_RESOLVER)) {
            fEntityResolver = (XMLEntityResolver)value;
            if (fChildConfig != null) {
                fChildConfig.setProperty(propertyId, value);
            }
            return;
        }
        if (propertyId.equals(SECURITY_MANAGER)) {
            fSecurityManager = (XMLSecurityManager)value;
            if (fChildConfig != null) {
                fChildConfig.setProperty(propertyId, value);
            }
            return;
        }
        if (propertyId.equals(XML_SECURITY_PROPERTY_MANAGER)) {
            fSecurityPropertyMgr = (XMLSecurityPropertyManager)value;

            if (fChildConfig != null) {
                fChildConfig.setProperty(XML_SECURITY_PROPERTY_MANAGER, value);
            }

            return;
        }

        if (propertyId.equals(BUFFER_SIZE)) {
            Integer bufferSize = (Integer) value;
            if (fChildConfig != null) {
                fChildConfig.setProperty(propertyId, value);
            }
            if (bufferSize != null && bufferSize.intValue() > 0) {
                fBufferSize = bufferSize.intValue();
                if (fXInclude10TextReader != null) {
                    fXInclude10TextReader.setBufferSize(fBufferSize);
                }
                if (fXInclude11TextReader != null) {
                    fXInclude11TextReader.setBufferSize(fBufferSize);
                }
            }
            return;
        }

    } 

    /**
     * Returns the default state for a feature, or null if this
     * component does not want to report a default value for this
     * feature.
     *
     * @param featureId The feature identifier.
     *
     * @since Xerces 2.2.0
     */
    @Override
    public Boolean getFeatureDefault(String featureId) {
        for (int i = 0; i < RECOGNIZED_FEATURES.length; i++) {
            if (RECOGNIZED_FEATURES[i].equals(featureId)) {
                return FEATURE_DEFAULTS[i];
            }
        }
        return null;
    } 

    /**
     * Returns the default state for a property, or null if this
     * component does not want to report a default value for this
     * property.
     *
     * @param propertyId The property identifier.
     *
     * @since Xerces 2.2.0
     */
    @Override
    public Object getPropertyDefault(String propertyId) {
        for (int i = 0; i < RECOGNIZED_PROPERTIES.length; i++) {
            if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
                return PROPERTY_DEFAULTS[i];
            }
        }
        return null;
    } 

    @Override
    public void setDocumentHandler(XMLDocumentHandler handler) {
        if (fDocumentHandler != handler) {
            fDocumentHandler = handler;
            if (fXIncludeChildConfig != null) {
                fXIncludeChildConfig.setDocumentHandler(handler);
            }
            if (fXPointerChildConfig != null) {
                fXPointerChildConfig.setDocumentHandler(handler);
            }
        }
    }

    @Override
    public XMLDocumentHandler getDocumentHandler() {
        return fDocumentHandler;
    }


    /**
     * Event sent at the start of the document.
     *
     * A fatal error will occur here, if it is detected that this document has been processed
     * before.
     *
     * This event is only passed on to the document handler if this is the root document.
     */
    @Override
    public void startDocument(
        XMLLocator locator,
        String encoding,
        NamespaceContext namespaceContext,
        Augmentations augs)
        throws XNIException {

        fErrorReporter.setDocumentLocator(locator);

        if (!(namespaceContext instanceof XIncludeNamespaceSupport)) {
            reportFatalError("IncompatibleNamespaceContext");
        }
        fNamespaceContext = (XIncludeNamespaceSupport)namespaceContext;
        fDocLocation = locator;
        fXIncludeLocator.setLocator(fDocLocation);

        setupCurrentBaseURI(locator);
        saveBaseURI();
        if (augs == null) {
            augs = new AugmentationsImpl();
        }
        augs.putItem(CURRENT_BASE_URI, fCurrentBaseURI);

        if (!isRootDocument()) {
            fParentXIncludeHandler.fHasIncludeReportedContent = true;
            if (fParentXIncludeHandler.searchForRecursiveIncludes(
                fCurrentBaseURI.getExpandedSystemId())) {
                reportFatalError(
                        "RecursiveInclude",
                        new Object[] { fCurrentBaseURI.getExpandedSystemId()});
            }
        }

        fCurrentLanguage = XMLSymbols.EMPTY_STRING;
        saveLanguage(fCurrentLanguage);

        if (isRootDocument() && fDocumentHandler != null) {
            fDocumentHandler.startDocument(
                fXIncludeLocator,
                encoding,
                namespaceContext,
                augs);
        }
    }

    @Override
    public void xmlDecl(
        String version,
        String encoding,
        String standalone,
        Augmentations augs)
        throws XNIException {
        fIsXML11 = "1.1".equals(version);
        if (isRootDocument() && fDocumentHandler != null) {
            fDocumentHandler.xmlDecl(version, encoding, standalone, augs);
        }
    }

    @Override
    public void doctypeDecl(
        String rootElement,
        String publicId,
        String systemId,
        Augmentations augs)
        throws XNIException {
        if (isRootDocument() && fDocumentHandler != null) {
            fDocumentHandler.doctypeDecl(rootElement, publicId, systemId, augs);
        }
    }

    @Override
    public void comment(XMLString text, Augmentations augs)
        throws XNIException {
        if (!fInDTD) {
            if (fDocumentHandler != null
                && getState() == STATE_NORMAL_PROCESSING) {
                fDepth++;
                augs = modifyAugmentations(augs);
                fDocumentHandler.comment(text, augs);
                fDepth--;
            }
        }
        else if (fDTDHandler != null) {
            fDTDHandler.comment(text, augs);
        }
    }

    @Override
    public void processingInstruction(
        String target,
        XMLString data,
        Augmentations augs)
        throws XNIException {
        if (!fInDTD) {
            if (fDocumentHandler != null
                && getState() == STATE_NORMAL_PROCESSING) {
                fDepth++;
                augs = modifyAugmentations(augs);
                fDocumentHandler.processingInstruction(target, data, augs);
                fDepth--;
            }
        }
        else if (fDTDHandler != null) {
            fDTDHandler.processingInstruction(target, data, augs);
        }
    }

    @Override
    public void startElement(
        QName element,
        XMLAttributes attributes,
        Augmentations augs)
        throws XNIException {
        fDepth++;
        int lastState = getState(fDepth - 1);
        if (lastState == STATE_EXPECT_FALLBACK && getState(fDepth - 2) == STATE_EXPECT_FALLBACK) {
            setState(STATE_IGNORE);
        }
        else {
            setState(lastState);
        }

        processXMLBaseAttributes(attributes);
        if (fFixupLanguage) {
            processXMLLangAttributes(attributes);
        }

        if (isIncludeElement(element)) {
            boolean success = this.handleIncludeElement(attributes);
            if (success) {
                setState(STATE_IGNORE);
            }
            else {
                setState(STATE_EXPECT_FALLBACK);
            }
        }
        else if (isFallbackElement(element)) {
            this.handleFallbackElement();
        }
        else if (hasXIncludeNamespace(element)) {
            if (getSawInclude(fDepth - 1)) {
                reportFatalError(
                    "IncludeChild",
                    new Object[] { element.rawname });
            }
            if (getSawFallback(fDepth - 1)) {
                reportFatalError(
                    "FallbackChild",
                    new Object[] { element.rawname });
            }
            if (getState() == STATE_NORMAL_PROCESSING) {
                if (fResultDepth++ == 0) {
                    checkMultipleRootElements();
                }
                if (fDocumentHandler != null) {
                    augs = modifyAugmentations(augs);
                    attributes = processAttributes(attributes);
                    fDocumentHandler.startElement(element, attributes, augs);
                }
            }
        }
        else if (getState() == STATE_NORMAL_PROCESSING) {
            if (fResultDepth++ == 0) {
                checkMultipleRootElements();
            }
            if (fDocumentHandler != null) {
                augs = modifyAugmentations(augs);
                attributes = processAttributes(attributes);
                fDocumentHandler.startElement(element, attributes, augs);
            }
        }
    }

    @Override
    public void emptyElement(
        QName element,
        XMLAttributes attributes,
        Augmentations augs)
        throws XNIException {
        fDepth++;
        int lastState = getState(fDepth - 1);
        if (lastState == STATE_EXPECT_FALLBACK && getState(fDepth - 2) == STATE_EXPECT_FALLBACK) {
            setState(STATE_IGNORE);
        }
        else {
            setState(lastState);
        }

        processXMLBaseAttributes(attributes);
        if (fFixupLanguage) {
            processXMLLangAttributes(attributes);
        }

        if (isIncludeElement(element)) {
            boolean success = this.handleIncludeElement(attributes);
            if (success) {
                setState(STATE_IGNORE);
            }
            else {
                reportFatalError("NoFallback",
                    new Object[] { attributes.getValue(null, "href") });
            }
        }
        else if (isFallbackElement(element)) {
            this.handleFallbackElement();
        }
        else if (hasXIncludeNamespace(element)) {
            if (getSawInclude(fDepth - 1)) {
                reportFatalError(
                    "IncludeChild",
                    new Object[] { element.rawname });
            }
            if (getSawFallback(fDepth - 1)) {
                reportFatalError(
                    "FallbackChild",
                    new Object[] { element.rawname });
            }
            if (getState() == STATE_NORMAL_PROCESSING) {
                if (fResultDepth == 0) {
                    checkMultipleRootElements();
                }
                if (fDocumentHandler != null) {
                    augs = modifyAugmentations(augs);
                    attributes = processAttributes(attributes);
                    fDocumentHandler.emptyElement(element, attributes, augs);
                }
            }
        }
        else if (getState() == STATE_NORMAL_PROCESSING) {
            if (fResultDepth == 0) {
                checkMultipleRootElements();
            }
            if (fDocumentHandler != null) {
                augs = modifyAugmentations(augs);
                attributes = processAttributes(attributes);
                fDocumentHandler.emptyElement(element, attributes, augs);
            }
        }
        setSawFallback(fDepth + 1, false);
        setSawInclude(fDepth, false);

        if (fBaseURIScope.size() > 0 && fDepth == fBaseURIScope.peek()) {
            restoreBaseURI();
        }
        fDepth--;
    }

    @Override
    public void endElement(QName element, Augmentations augs)
        throws XNIException {

        if (isIncludeElement(element)) {
            if (getState() == STATE_EXPECT_FALLBACK
                && !getSawFallback(fDepth + 1)) {
                reportFatalError("NoFallback",
                    new Object[] { "unknown" });
            }
        }
        if (isFallbackElement(element)) {
            if (getState() == STATE_NORMAL_PROCESSING) {
                setState(STATE_IGNORE);
            }
        }
        else if (getState() == STATE_NORMAL_PROCESSING) {
            --fResultDepth;
            if (fDocumentHandler != null) {
                fDocumentHandler.endElement(element, augs);
            }
        }

        setSawFallback(fDepth + 1, false);
        setSawInclude(fDepth, false);

        if (fBaseURIScope.size() > 0 && fDepth == fBaseURIScope.peek()) {
            restoreBaseURI();
        }

        if (fLanguageScope.size() > 0 && fDepth == fLanguageScope.peek()) {
            fCurrentLanguage = restoreLanguage();
        }

        fDepth--;
    }

    @Override
    public void startGeneralEntity(
        String name,
        XMLResourceIdentifier resId,
        String encoding,
        Augmentations augs)
        throws XNIException {
        if (getState() == STATE_NORMAL_PROCESSING) {
            if (fResultDepth == 0) {
                if (augs != null && Boolean.TRUE.equals(augs.getItem(Constants.ENTITY_SKIPPED))) {
                    reportFatalError("UnexpandedEntityReferenceIllegal");
                }
            }
            else if (fDocumentHandler != null) {
                fDocumentHandler.startGeneralEntity(name, resId, encoding, augs);
            }
        }
    }

    @Override
    public void textDecl(String version, String encoding, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null
            && getState() == STATE_NORMAL_PROCESSING) {
            fDocumentHandler.textDecl(version, encoding, augs);
        }
    }

    @Override
    public void endGeneralEntity(String name, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null
            && getState() == STATE_NORMAL_PROCESSING
            && fResultDepth != 0) {
            fDocumentHandler.endGeneralEntity(name, augs);
        }
    }

    @Override
    public void characters(XMLString text, Augmentations augs)
        throws XNIException {
        if (getState() == STATE_NORMAL_PROCESSING) {
            if (fResultDepth == 0) {
                checkWhitespace(text);
            }
            else if (fDocumentHandler != null) {
                fDepth++;
                augs = modifyAugmentations(augs);
                fDocumentHandler.characters(text, augs);
                fDepth--;
            }
        }
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null
            && getState() == STATE_NORMAL_PROCESSING
            && fResultDepth != 0) {
            fDocumentHandler.ignorableWhitespace(text, augs);
        }
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
        if (fDocumentHandler != null
            && getState() == STATE_NORMAL_PROCESSING
            && fResultDepth != 0) {
            fDocumentHandler.startCDATA(augs);
        }
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
        if (fDocumentHandler != null
            && getState() == STATE_NORMAL_PROCESSING
            && fResultDepth != 0) {
            fDocumentHandler.endCDATA(augs);
        }
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {
        if (isRootDocument()) {
            if (!fSeenRootElement) {
                reportFatalError("RootElementRequired");
            }
            if (fDocumentHandler != null) {
                fDocumentHandler.endDocument(augs);
            }
        }
    }

    @Override
    public void setDocumentSource(XMLDocumentSource source) {
        fDocumentSource = source;
    }

    @Override
    public XMLDocumentSource getDocumentSource() {
        return fDocumentSource;
    }


    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#attributeDecl(java.lang.String, java.lang.String, java.lang.String, java.lang.String[], java.lang.String, com.sun.org.apache.xerces.internal.xni.XMLString, com.sun.org.apache.xerces.internal.xni.XMLString, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void attributeDecl(
        String elementName,
        String attributeName,
        String type,
        String[] enumeration,
        String defaultType,
        XMLString defaultValue,
        XMLString nonNormalizedDefaultValue,
        Augmentations augmentations)
        throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.attributeDecl(
                elementName,
                attributeName,
                type,
                enumeration,
                defaultType,
                defaultValue,
                nonNormalizedDefaultValue,
                augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#elementDecl(java.lang.String, java.lang.String, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void elementDecl(
        String name,
        String contentModel,
        Augmentations augmentations)
        throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.elementDecl(name, contentModel, augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#endAttlist(com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void endAttlist(Augmentations augmentations) throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.endAttlist(augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#endConditional(com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void endConditional(Augmentations augmentations)
        throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.endConditional(augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#endDTD(com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void endDTD(Augmentations augmentations) throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.endDTD(augmentations);
        }
        fInDTD = false;
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#endExternalSubset(com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void endExternalSubset(Augmentations augmentations)
        throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.endExternalSubset(augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#endParameterEntity(java.lang.String, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void endParameterEntity(String name, Augmentations augmentations)
        throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.endParameterEntity(name, augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#externalEntityDecl(java.lang.String, com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void externalEntityDecl(
        String name,
        XMLResourceIdentifier identifier,
        Augmentations augmentations)
        throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.externalEntityDecl(name, identifier, augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#getDTDSource()
     */
    @Override
    public XMLDTDSource getDTDSource() {
        return fDTDSource;
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#ignoredCharacters(com.sun.org.apache.xerces.internal.xni.XMLString, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void ignoredCharacters(XMLString text, Augmentations augmentations)
        throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.ignoredCharacters(text, augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#internalEntityDecl(java.lang.String, com.sun.org.apache.xerces.internal.xni.XMLString, com.sun.org.apache.xerces.internal.xni.XMLString, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void internalEntityDecl(
        String name,
        XMLString text,
        XMLString nonNormalizedText,
        Augmentations augmentations)
        throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.internalEntityDecl(
                name,
                text,
                nonNormalizedText,
                augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#notationDecl(java.lang.String, com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void notationDecl(
        String name,
        XMLResourceIdentifier identifier,
        Augmentations augmentations)
        throws XNIException {
        this.addNotation(name, identifier, augmentations);
        if (fDTDHandler != null) {
            fDTDHandler.notationDecl(name, identifier, augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#setDTDSource(com.sun.org.apache.xerces.internal.xni.parser.XMLDTDSource)
     */
    @Override
    public void setDTDSource(XMLDTDSource source) {
        fDTDSource = source;
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#startAttlist(java.lang.String, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void startAttlist(String elementName, Augmentations augmentations)
        throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.startAttlist(elementName, augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#startConditional(short, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void startConditional(short type, Augmentations augmentations)
        throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.startConditional(type, augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#startDTD(com.sun.org.apache.xerces.internal.xni.XMLLocator, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void startDTD(XMLLocator locator, Augmentations augmentations)
        throws XNIException {
        fInDTD = true;
        if (fDTDHandler != null) {
            fDTDHandler.startDTD(locator, augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#startExternalSubset(com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void startExternalSubset(
        XMLResourceIdentifier identifier,
        Augmentations augmentations)
        throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.startExternalSubset(identifier, augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#startParameterEntity(java.lang.String, com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier, java.lang.String, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void startParameterEntity(
        String name,
        XMLResourceIdentifier identifier,
        String encoding,
        Augmentations augmentations)
        throws XNIException {
        if (fDTDHandler != null) {
            fDTDHandler.startParameterEntity(
                name,
                identifier,
                encoding,
                augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.XMLDTDHandler#unparsedEntityDecl(java.lang.String, com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier, java.lang.String, com.sun.org.apache.xerces.internal.xni.Augmentations)
     */
    @Override
    public void unparsedEntityDecl(
        String name,
        XMLResourceIdentifier identifier,
        String notation,
        Augmentations augmentations)
        throws XNIException {
        this.addUnparsedEntity(name, identifier, notation, augmentations);
        if (fDTDHandler != null) {
            fDTDHandler.unparsedEntityDecl(
                name,
                identifier,
                notation,
                augmentations);
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.parser.XMLDTDSource#getDTDHandler()
     */
    @Override
    public XMLDTDHandler getDTDHandler() {
        return fDTDHandler;
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.parser.XMLDTDSource#setDTDHandler(com.sun.org.apache.xerces.internal.xni.XMLDTDHandler)
     */
    @Override
    public void setDTDHandler(XMLDTDHandler handler) {
        fDTDHandler = handler;
    }


    private void setErrorReporter(XMLErrorReporter reporter) {
        fErrorReporter = reporter;
        if (fErrorReporter != null) {
            fErrorReporter.putMessageFormatter(
                XIncludeMessageFormatter.XINCLUDE_DOMAIN, fXIncludeMessageFormatter);
            if (fDocLocation != null) {
                fErrorReporter.setDocumentLocator(fDocLocation);
            }
        }
    }

    protected void handleFallbackElement() {
        if (!getSawInclude(fDepth - 1)) {
            if (getState() == STATE_IGNORE) {
                return;
            }
            reportFatalError("FallbackParent");
        }

        setSawInclude(fDepth, false);
        fNamespaceContext.setContextInvalid();

        if (getSawFallback(fDepth)) {
            reportFatalError("MultipleFallbacks");
        }
        else {
            setSawFallback(fDepth, true);
        }

        if (getState() == STATE_EXPECT_FALLBACK) {
            setState(STATE_NORMAL_PROCESSING);
        }
    }

    protected boolean handleIncludeElement(XMLAttributes attributes)
        throws XNIException {
        if (getSawInclude(fDepth - 1)) {
            reportFatalError("IncludeChild", new Object[] { XINCLUDE_INCLUDE });
        }
        if (getState() == STATE_IGNORE) {
            return true;
        }
        setSawInclude(fDepth, true);
        fNamespaceContext.setContextInvalid();


        String href = attributes.getValue(XINCLUDE_ATTR_HREF);
        String parse = attributes.getValue(XINCLUDE_ATTR_PARSE);
        String xpointer =  attributes.getValue(XPOINTER);
        String accept = attributes.getValue(XINCLUDE_ATTR_ACCEPT);
        String acceptLanguage = attributes.getValue(XINCLUDE_ATTR_ACCEPT_LANGUAGE);

        if (parse == null) {
            parse = XINCLUDE_PARSE_XML;
        }
        if (href == null) {
            href = XMLSymbols.EMPTY_STRING;
        }
        if (href.length() == 0 && XINCLUDE_PARSE_XML.equals(parse)) {
            if (xpointer == null) {
                reportFatalError("XpointerMissing");
            }
            else {
                Locale locale = (fErrorReporter != null) ? fErrorReporter.getLocale() : null;
                String reason = fXIncludeMessageFormatter.formatMessage(locale, "XPointerStreamability", null);
                reportResourceError("XMLResourceError", new Object[] { href, reason });
                return false;
            }
        }

        URI hrefURI = null;

        try {
            hrefURI = new URI(href, true);
            if (hrefURI.getFragment() != null) {
                reportFatalError("HrefFragmentIdentifierIllegal", new Object[] {href});
            }
        }
        catch (URI.MalformedURIException exc) {
            String newHref = escapeHref(href);
            if (href != newHref) {
                href = newHref;
                try {
                    hrefURI = new URI(href, true);
                    if (hrefURI.getFragment() != null) {
                        reportFatalError("HrefFragmentIdentifierIllegal", new Object[] {href});
                    }
                }
                catch (URI.MalformedURIException exc2) {
                    reportFatalError("HrefSyntacticallyInvalid", new Object[] {href});
                }
            }
            else {
                reportFatalError("HrefSyntacticallyInvalid", new Object[] {href});
            }
        }

        if (accept != null && !isValidInHTTPHeader(accept)) {
            reportFatalError("AcceptMalformed", null);
            accept = null;
        }
        if (acceptLanguage != null && !isValidInHTTPHeader(acceptLanguage)) {
            reportFatalError("AcceptLanguageMalformed", null);
            acceptLanguage = null;
        }

        XMLInputSource includedSource = null;
        try {
            if (fEntityResolver != null) {
                XMLResourceIdentifier resourceIdentifier =
                    new XMLResourceIdentifierImpl(
                        null,
                        href,
                        fCurrentBaseURI.getExpandedSystemId(),
                        XMLEntityManager.expandSystemId(
                            href,
                            fCurrentBaseURI.getExpandedSystemId(),
                            false));

                includedSource =
                    fEntityResolver.resolveEntity(resourceIdentifier);
            }
            if (includedSource == null && fUseCatalog) {
                if (fCatalogFeatures == null) {
                    fCatalogFeatures = JdkXmlUtils.getCatalogFeatures(fDefer, fCatalogFile, fPrefer, fResolve);
                }
                fCatalogFile = fCatalogFeatures.get(CatalogFeatures.Feature.FILES);
                if (fCatalogFile != null) {
                    /*
                       Although URI entry is preferred for resolving XInclude, system entry
                       is allowed as well.
                    */
                    Source source = null;
                    try {
                        if (fCatalogResolver == null) {
                            fCatalogResolver = CatalogManager.catalogResolver(fCatalogFeatures);
                        }
                        source = fCatalogResolver.resolve(href, fCurrentBaseURI.getExpandedSystemId());
                    } catch (CatalogException e) {}

                    if (source != null && !source.isEmpty()) {
                        includedSource = new XMLInputSource(null, source.getSystemId(),
                                fCurrentBaseURI.getExpandedSystemId(), true);
                    } else {
                        if (fCatalogResolver == null) {
                            fCatalogResolver = CatalogManager.catalogResolver(fCatalogFeatures);
                        }
                        InputSource is = fCatalogResolver.resolveEntity(href, href);
                        if (is != null && !is.isEmpty()) {
                            includedSource = new XMLInputSource(is, true);
                        }
                    }
                }
            }

            if (includedSource != null &&
                !(includedSource instanceof HTTPInputSource) &&
                (accept != null || acceptLanguage != null) &&
                includedSource.getCharacterStream() == null &&
                includedSource.getByteStream() == null) {

                includedSource = createInputSource(includedSource.getPublicId(), includedSource.getSystemId(),
                    includedSource.getBaseSystemId(), accept, acceptLanguage);
            }
        }
        catch (IOException | CatalogException e) {
            reportResourceError(
                "XMLResourceError",
                new Object[] { href, e.getMessage()}, e);
            return false;
        }

        if (includedSource == null) {
            if (accept != null || acceptLanguage != null) {
                includedSource = createInputSource(null, href, fCurrentBaseURI.getExpandedSystemId(), accept, acceptLanguage);
            }
            else {
                includedSource = new XMLInputSource(null, href, fCurrentBaseURI.getExpandedSystemId(), false);
            }
        }

        if (parse.equals(XINCLUDE_PARSE_XML)) {
            if ((xpointer != null && fXPointerChildConfig == null)
                        || (xpointer == null && fXIncludeChildConfig == null) ) {

                if (xpointer == null) {
                    fChildConfig = new XIncludeParserConfiguration();
                } else {
                    fChildConfig = new XPointerParserConfiguration();
                }

                if (fSymbolTable != null) fChildConfig.setProperty(SYMBOL_TABLE, fSymbolTable);
                if (fErrorReporter != null) fChildConfig.setProperty(ERROR_REPORTER, fErrorReporter);
                if (fEntityResolver != null) fChildConfig.setProperty(ENTITY_RESOLVER, fEntityResolver);
                fChildConfig.setProperty(SECURITY_MANAGER, fSecurityManager);
                fChildConfig.setProperty(XML_SECURITY_PROPERTY_MANAGER, fSecurityPropertyMgr);
                fChildConfig.setProperty(BUFFER_SIZE, fBufferSize);
                fChildConfig.setProperty(CatalogFeatures.Feature.FILES.getPropertyName(), fCatalogFile);
                fChildConfig.setProperty(CatalogFeatures.Feature.DEFER.getPropertyName(), fDefer);
                fChildConfig.setProperty(CatalogFeatures.Feature.PREFER.getPropertyName(), fPrefer);
                fChildConfig.setProperty(CatalogFeatures.Feature.RESOLVE.getPropertyName(), fResolve);
                fChildConfig.setFeature(XMLConstants.USE_CATALOG, fUseCatalog);

                fNeedCopyFeatures = true;

                fChildConfig.setProperty(
                    Constants.XERCES_PROPERTY_PREFIX
                        + Constants.NAMESPACE_CONTEXT_PROPERTY,
                    fNamespaceContext);

                fChildConfig.setFeature(
                            XINCLUDE_FIXUP_BASE_URIS,
                            fFixupBaseURIs);

                fChildConfig.setFeature(
                            XINCLUDE_FIXUP_LANGUAGE,
                            fFixupLanguage);


                if (xpointer != null ) {

                    XPointerHandler newHandler =
                        (XPointerHandler)fChildConfig.getProperty(
                            Constants.XERCES_PROPERTY_PREFIX
                                + Constants.XPOINTER_HANDLER_PROPERTY);

                        fXPtrProcessor = newHandler;

                        ((XPointerHandler)fXPtrProcessor).setProperty(
                            Constants.XERCES_PROPERTY_PREFIX
                            + Constants.NAMESPACE_CONTEXT_PROPERTY,
                        fNamespaceContext);

                    ((XPointerHandler)fXPtrProcessor).setProperty(XINCLUDE_FIXUP_BASE_URIS,
                            fFixupBaseURIs);

                    ((XPointerHandler)fXPtrProcessor).setProperty(
                            XINCLUDE_FIXUP_LANGUAGE, fFixupLanguage);

                    if (fErrorReporter != null)
                        ((XPointerHandler)fXPtrProcessor).setProperty(ERROR_REPORTER, fErrorReporter);

                    newHandler.setParent(this);
                    newHandler.setHref(href);
                    newHandler.setXIncludeLocator(fXIncludeLocator);
                    newHandler.setDocumentHandler(this.getDocumentHandler());
                    fXPointerChildConfig = fChildConfig;
                } else {
                    XIncludeHandler newHandler =
                        (XIncludeHandler)fChildConfig.getProperty(
                            Constants.XERCES_PROPERTY_PREFIX
                                + Constants.XINCLUDE_HANDLER_PROPERTY);

                    newHandler.setParent(this);
                    newHandler.setHref(href);
                    newHandler.setDocumentHandler(this.getDocumentHandler());
                    fXIncludeChildConfig = fChildConfig;
                }
            }

            if (xpointer != null ) {
                fChildConfig = fXPointerChildConfig;

                try {
                    fXPtrProcessor.parseXPointer(xpointer);

                } catch (XNIException ex) {
                    reportResourceError(
                            "XMLResourceError",
                            new Object[] { href, ex.getMessage()});
                        return false;
                }
            } else {
                fChildConfig = fXIncludeChildConfig;
            }

            if (fNeedCopyFeatures) {
                copyFeatures(fSettings, fChildConfig);
            }
            fNeedCopyFeatures = false;

            try {
                fHasIncludeReportedContent = false;
                fNamespaceContext.pushScope();

                fChildConfig.parse(includedSource);
                fXIncludeLocator.setLocator(fDocLocation);
                if (fErrorReporter != null) {
                    fErrorReporter.setDocumentLocator(fDocLocation);
                }

                if (xpointer != null ) {
                        if (!fXPtrProcessor.isXPointerResolved()) {
                        Locale locale = (fErrorReporter != null) ? fErrorReporter.getLocale() : null;
                        String reason = fXIncludeMessageFormatter.formatMessage(locale, "XPointerResolutionUnsuccessful", null);
                        reportResourceError("XMLResourceError", new Object[] {href, reason});
                                return false;
                        }
                }
            }
            catch (XNIException e) {
                fXIncludeLocator.setLocator(fDocLocation);
                if (fErrorReporter != null) {
                    fErrorReporter.setDocumentLocator(fDocLocation);
                }
                reportFatalError("XMLParseError", new Object[] { href, e.getMessage() });
            }
            catch (IOException e) {
                fXIncludeLocator.setLocator(fDocLocation);
                if (fErrorReporter != null) {
                    fErrorReporter.setDocumentLocator(fDocLocation);
                }
                if (fHasIncludeReportedContent) {
                    throw new XNIException(e);
                }
                reportResourceError(
                    "XMLResourceError",
                    new Object[] { href, e.getMessage()}, e);
                return false;
            }
            finally {
                fNamespaceContext.popScope();
            }
        }
        else if (parse.equals(XINCLUDE_PARSE_TEXT)) {
            String encoding = attributes.getValue(XINCLUDE_ATTR_ENCODING);
            includedSource.setEncoding(encoding);
            XIncludeTextReader textReader = null;

            try {
                fHasIncludeReportedContent = false;

                if (!fIsXML11) {
                    if (fXInclude10TextReader == null) {
                        fXInclude10TextReader = new XIncludeTextReader(includedSource, this, fBufferSize);
                    }
                    else {
                        fXInclude10TextReader.setInputSource(includedSource);
                    }
                    textReader = fXInclude10TextReader;
                }
                else {
                    if (fXInclude11TextReader == null) {
                        fXInclude11TextReader = new XInclude11TextReader(includedSource, this, fBufferSize);
                    }
                    else {
                        fXInclude11TextReader.setInputSource(includedSource);
                    }
                    textReader = fXInclude11TextReader;
                }
                textReader.setErrorReporter(fErrorReporter);
                textReader.parse();
            }
            catch (MalformedByteSequenceException ex) {
                fErrorReporter.reportError(ex.getDomain(), ex.getKey(),
                    ex.getArguments(), XMLErrorReporter.SEVERITY_FATAL_ERROR, ex);
            }
            catch (CharConversionException e) {
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                    "CharConversionFailure", null, XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
            }
            catch (IOException e) {
                if (fHasIncludeReportedContent) {
                    throw new XNIException(e);
                }
                reportResourceError(
                    "TextResourceError",
                    new Object[] { href, e.getMessage()}, e);
                return false;
            }
            finally {
                if (textReader != null) {
                    try {
                        textReader.close();
                    }
                    catch (IOException e) {
                        reportResourceError(
                            "TextResourceError",
                            new Object[] { href, e.getMessage()}, e);
                        return false;
                    }
                }
            }
        }
        else {
            reportFatalError("InvalidParseValue", new Object[] { parse });
        }
        return true;
    }

    /**
     * Returns true if the element has the namespace "http:
     * @param element the element to check
     * @return true if the element has the namespace "http:
     */
    protected boolean hasXIncludeNamespace(QName element) {
        return element.uri == XINCLUDE_NS_URI
            || fNamespaceContext.getURI(element.prefix) == XINCLUDE_NS_URI;
    }

    /**
     * Checks if the element is an &lt;include&gt; element.  The element must have
     * the XInclude namespace, and a local name of "include".
     *
     * @param element the element to check
     * @return true if the element is an &lt;include&gt; element
     * @see #hasXIncludeNamespace(QName)
     */
    protected boolean isIncludeElement(QName element) {
        return element.localpart.equals(XINCLUDE_INCLUDE) &&
            hasXIncludeNamespace(element);
    }

    /**
     * Checks if the element is an &lt;fallback&gt; element.  The element must have
     * the XInclude namespace, and a local name of "fallback".
     *
     * @param element the element to check
     * @return true if the element is an &lt;fallback; element
     * @see #hasXIncludeNamespace(QName)
     */
    protected boolean isFallbackElement(QName element) {
        return element.localpart.equals(XINCLUDE_FALLBACK) &&
            hasXIncludeNamespace(element);
    }

    /**
     * Returns true if the current [base URI] is the same as the [base URI] that
     * was in effect on the include parent.  This method should <em>only</em> be called
     * when the current element is a top level included element, i.e. the direct child
     * of a fallback element, or the root elements in an included document.
     * The "include parent" is the element which, in the result infoset, will be the
     * direct parent of the current element.
     * @return true if the [base URIs] are the same string
     */
    protected boolean sameBaseURIAsIncludeParent() {
        String parentBaseURI = getIncludeParentBaseURI();
        String baseURI = fCurrentBaseURI.getExpandedSystemId();
        return parentBaseURI != null && parentBaseURI.equals(baseURI);
    }

    /**
     * Returns true if the current [language] is equivalent to the [language] that
     * was in effect on the include parent, taking case-insensitivity into account
     * as per [RFC 3066].  This method should <em>only</em> be called when the
     * current element is a top level included element, i.e. the direct child
     * of a fallback element, or the root elements in an included document.
     * The "include parent" is the element which, in the result infoset, will be the
     * direct parent of the current element.
     *
     * @return true if the [language] properties have the same value
     * taking case-insensitivity into account as per [RFC 3066].
     */
    protected boolean sameLanguageAsIncludeParent() {
        String parentLanguage = getIncludeParentLanguage();
        return parentLanguage != null && parentLanguage.equalsIgnoreCase(fCurrentLanguage);
    }

    private void setupCurrentBaseURI(XMLLocator locator) {
        fCurrentBaseURI.setBaseSystemId(locator.getBaseSystemId());
        if (locator.getLiteralSystemId() != null) {
            fCurrentBaseURI.setLiteralSystemId(locator.getLiteralSystemId());
        }
        else {
            fCurrentBaseURI.setLiteralSystemId(fHrefFromParent);
        }

        String expandedSystemId = locator.getExpandedSystemId();
        if (expandedSystemId == null) {
            try {
                expandedSystemId =
                    XMLEntityManager.expandSystemId(
                        fCurrentBaseURI.getLiteralSystemId(),
                        fCurrentBaseURI.getBaseSystemId(),
                        false);
                if (expandedSystemId == null) {
                    expandedSystemId = fCurrentBaseURI.getLiteralSystemId();
                }
            }
            catch (MalformedURIException e) {
                reportFatalError("ExpandedSystemId");
            }
        }
        fCurrentBaseURI.setExpandedSystemId(expandedSystemId);
    }

    /**
     * Checks if the file indicated by the given system id has already been
     * included in the current stack.
     * @param includedSysId the system id to check for inclusion
     * @return true if the source has already been included
     */
    protected boolean searchForRecursiveIncludes(String includedSysId) {
        if (includedSysId.equals(fCurrentBaseURI.getExpandedSystemId())) {
            return true;
        }
        else if (fParentXIncludeHandler == null) {
            return false;
        }
        else {
            return fParentXIncludeHandler.searchForRecursiveIncludes(includedSysId);
        }
    }

    /**
     * Returns true if the current element is a top level included item.  This means
     * it's either the child of a fallback element, or the top level item in an
     * included document
     * @return true if the current element is a top level included item
     */
    protected boolean isTopLevelIncludedItem() {
        return isTopLevelIncludedItemViaInclude()
            || isTopLevelIncludedItemViaFallback();
    }

    protected boolean isTopLevelIncludedItemViaInclude() {
        return fDepth == 1 && !isRootDocument();
    }

    protected boolean isTopLevelIncludedItemViaFallback() {
        return getSawFallback(fDepth - 1);
    }

    /**
     * Processes the XMLAttributes object of startElement() calls.  Performs the following tasks:
     * <ul>
     * <li> If the element is a top level included item whose [base URI] is different from the
     * [base URI] of the include parent, then an xml:base attribute is added to specify the
     * true [base URI]
     * <li> For all namespace prefixes which are in-scope in an included item, but not in scope
     * in the include parent, a xmlns:prefix attribute is added
     * <li> For all attributes with a type of ENTITY, ENTITIES or NOTATIONS, the notations and
     * unparsed entities are processed as described in the spec, sections 4.5.1 and 4.5.2
     * </ul>
     * @param attributes
     * @return the processed XMLAttributes
     */
    protected XMLAttributes processAttributes(XMLAttributes attributes) {
        if (isTopLevelIncludedItem()) {
            if (fFixupBaseURIs && !sameBaseURIAsIncludeParent()) {
                if (attributes == null) {
                    attributes = new XMLAttributesImpl();
                }

                String uri = null;
                try {
                    uri = this.getRelativeBaseURI();
                }
                catch (MalformedURIException e) {
                    uri = fCurrentBaseURI.getExpandedSystemId();
                }
                int index =
                    attributes.addAttribute(
                        XML_BASE_QNAME,
                        XMLSymbols.fCDATASymbol,
                        uri);
                attributes.setSpecified(index, true);
            }

            if (fFixupLanguage && !sameLanguageAsIncludeParent()) {
                if (attributes == null) {
                    attributes = new XMLAttributesImpl();
                }
                int index =
                    attributes.addAttribute(
                        XML_LANG_QNAME,
                        XMLSymbols.fCDATASymbol,
                        fCurrentLanguage);
                attributes.setSpecified(index, true);
            }

            Enumeration<String> inscopeNS = fNamespaceContext.getAllPrefixes();
            while (inscopeNS.hasMoreElements()) {
                String prefix = inscopeNS.nextElement();
                String parentURI =
                    fNamespaceContext.getURIFromIncludeParent(prefix);
                String uri = fNamespaceContext.getURI(prefix);
                if (parentURI != uri && attributes != null) {
                    if (prefix == XMLSymbols.EMPTY_STRING) {
                        if (attributes
                            .getValue(
                                NamespaceContext.XMLNS_URI,
                                XMLSymbols.PREFIX_XMLNS)
                            == null) {
                            if (attributes == null) {
                                attributes = new XMLAttributesImpl();
                            }

                            QName ns = (QName)NEW_NS_ATTR_QNAME.clone();
                            ns.prefix = null;
                            ns.localpart = XMLSymbols.PREFIX_XMLNS;
                            ns.rawname = XMLSymbols.PREFIX_XMLNS;
                            int index =
                                attributes.addAttribute(
                                    ns,
                                    XMLSymbols.fCDATASymbol,
                                    uri != null ? uri : XMLSymbols.EMPTY_STRING);
                            attributes.setSpecified(index, true);
                            fNamespaceContext.declarePrefix(prefix, uri);
                        }
                    }
                    else if (
                        attributes.getValue(NamespaceContext.XMLNS_URI, prefix)
                            == null) {
                        if (attributes == null) {
                            attributes = new XMLAttributesImpl();
                        }

                        QName ns = (QName)NEW_NS_ATTR_QNAME.clone();
                        ns.localpart = prefix;
                        ns.rawname += prefix;
                        ns.rawname = (fSymbolTable != null) ?
                            fSymbolTable.addSymbol(ns.rawname) :
                            ns.rawname.intern();
                        int index =
                            attributes.addAttribute(
                                ns,
                                XMLSymbols.fCDATASymbol,
                                uri != null ? uri : XMLSymbols.EMPTY_STRING);
                        attributes.setSpecified(index, true);
                        fNamespaceContext.declarePrefix(prefix, uri);
                    }
                }
            }
        }

        if (attributes != null) {
            int length = attributes.getLength();
            for (int i = 0; i < length; i++) {
                String type = attributes.getType(i);
                String value = attributes.getValue(i);
                if (type == XMLSymbols.fENTITYSymbol) {
                    this.checkUnparsedEntity(value);
                }
                if (type == XMLSymbols.fENTITIESSymbol) {
                    StringTokenizer st = new StringTokenizer(value);
                    while (st.hasMoreTokens()) {
                        String entName = st.nextToken();
                        this.checkUnparsedEntity(entName);
                    }
                }
                else if (type == XMLSymbols.fNOTATIONSymbol) {
                    this.checkNotation(value);
                }
                /* We actually don't need to do anything for 4.5.3, because at this stage the
                 * value of the attribute is just a string. It will be taken care of later
                 * in the pipeline, when the IDREFs are actually resolved against IDs.
                 *
                 * if (type == XMLSymbols.fIDREFSymbol || type == XMLSymbols.fIDREFSSymbol) { }
                 */
            }
        }

        return attributes;
    }

    /**
     * Returns a URI, relative to the include parent's base URI, of the current
     * [base URI].  For instance, if the current [base URI] was "dir1/dir2/file.xml"
     * and the include parent's [base URI] was "dir/", this would return "dir2/file.xml".
     * @return the relative URI
     */
    protected String getRelativeBaseURI() throws MalformedURIException {
        int includeParentDepth = getIncludeParentDepth();
        String relativeURI = this.getRelativeURI(includeParentDepth);
        if (isRootDocument()) {
            return relativeURI;
        }
        else {
            if (relativeURI.length() == 0) {
                relativeURI = fCurrentBaseURI.getLiteralSystemId();
            }

            if (includeParentDepth == 0) {
                if (fParentRelativeURI == null) {
                    fParentRelativeURI =
                        fParentXIncludeHandler.getRelativeBaseURI();
                }
                if (fParentRelativeURI.length() == 0) {
                    return relativeURI;
                }

                URI base = new URI(fParentRelativeURI, true);
                URI uri = new URI(base, relativeURI);

                /** Check whether the scheme components are equal. */
                final String baseScheme = base.getScheme();
                final String literalScheme = uri.getScheme();
                if (!Objects.equals(baseScheme, literalScheme)) {
                    return relativeURI;
                }

                /** Check whether the authority components are equal. */
                final String baseAuthority = base.getAuthority();
                final String literalAuthority = uri.getAuthority();
                if (!Objects.equals(baseAuthority, literalAuthority)) {
                    return uri.getSchemeSpecificPart();
                }

                /**
                 * The scheme and authority components are equal,
                 * return the path and the possible query and/or
                 * fragment which follow.
                 */
                final String literalPath = uri.getPath();
                final String literalQuery = uri.getQueryString();
                final String literalFragment = uri.getFragment();
                if (literalQuery != null || literalFragment != null) {
                    final StringBuilder buffer = new StringBuilder();
                    if (literalPath != null) {
                        buffer.append(literalPath);
                    }
                    if (literalQuery != null) {
                        buffer.append('?');
                        buffer.append(literalQuery);
                    }
                    if (literalFragment != null) {
                        buffer.append('#');
                        buffer.append(literalFragment);
                    }
                    return buffer.toString();
                }
                return literalPath;
            }
            else {
                return relativeURI;
            }
        }
    }

    /**
     * Returns the [base URI] of the include parent.
     * @return the base URI of the include parent.
     */
    private String getIncludeParentBaseURI() {
        int depth = getIncludeParentDepth();
        if (!isRootDocument() && depth == 0) {
            return fParentXIncludeHandler.getIncludeParentBaseURI();
        }
        else {
            return this.getBaseURI(depth);
        }
    }

    /**
     * Returns the [language] of the include parent.
     *
     * @return the language property of the include parent.
     */
    private String getIncludeParentLanguage() {
        int depth = getIncludeParentDepth();
        if (!isRootDocument() && depth == 0) {
            return fParentXIncludeHandler.getIncludeParentLanguage();
        }
        else {
            return getLanguage(depth);
        }
    }

    /**
     * Returns the depth of the include parent.  Here, the include parent is
     * calculated as the last non-include or non-fallback element. It is assumed
     * this method is called when the current element is a top level included item.
     * Returning 0 indicates that the top level element in this document
     * was an include element.
     * @return the depth of the top level include element
     */
    private int getIncludeParentDepth() {
        for (int i = fDepth - 1; i >= 0; i--) {
            if (!getSawInclude(i) && !getSawFallback(i)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Returns the current element depth of the result infoset.
     */
    private int getResultDepth() {
        return fResultDepth;
    }

    /**
     * Modify the augmentations.  Add an [included] infoset item, if the current
     * element is a top level included item.
     * @param augs the Augmentations to modify.
     * @return the modified Augmentations
     */
    protected Augmentations modifyAugmentations(Augmentations augs) {
        return modifyAugmentations(augs, false);
    }

    /**
     * Modify the augmentations.  Add an [included] infoset item, if <code>force</code>
     * is true, or if the current element is a top level included item.
     * @param augs the Augmentations to modify.
     * @param force whether to force modification
     * @return the modified Augmentations
     */
    protected Augmentations modifyAugmentations(
        Augmentations augs,
        boolean force) {
        if (force || isTopLevelIncludedItem()) {
            if (augs == null) {
                augs = new AugmentationsImpl();
            }
            augs.putItem(XINCLUDE_INCLUDED, Boolean.TRUE);
        }
        return augs;
    }

    protected int getState(int depth) {
        return fState[depth];
    }

    protected int getState() {
        return fState[fDepth];
    }

    protected void setState(int state) {
        if (fDepth >= fState.length) {
            int[] newarray = new int[fDepth * 2];
            System.arraycopy(fState, 0, newarray, 0, fState.length);
            fState = newarray;
        }
        fState[fDepth] = state;
    }

    /**
     * Records that an &lt;fallback&gt; was encountered at the specified depth,
     * as an ancestor of the current element, or as a sibling of an ancestor of the
     * current element.
     *
     * @param depth
     * @param val
     */
    protected void setSawFallback(int depth, boolean val) {
        if (depth >= fSawFallback.length) {
            boolean[] newarray = new boolean[depth * 2];
            System.arraycopy(fSawFallback, 0, newarray, 0, fSawFallback.length);
            fSawFallback = newarray;
        }
        fSawFallback[depth] = val;
    }

    /**
     * Returns whether an &lt;fallback&gt; was encountered at the specified depth,
     * as an ancestor of the current element, or as a sibling of an ancestor of the
     * current element.
     *
     * @param depth
     */
    protected boolean getSawFallback(int depth) {
        if (depth >= fSawFallback.length) {
            return false;
        }
        return fSawFallback[depth];
    }

    /**
     * Records that an &lt;include&gt; was encountered at the specified depth,
     * as an ancestor of the current item.
     *
     * @param depth
     * @param val
     */
    protected void setSawInclude(int depth, boolean val) {
        if (depth >= fSawInclude.length) {
            boolean[] newarray = new boolean[depth * 2];
            System.arraycopy(fSawInclude, 0, newarray, 0, fSawInclude.length);
            fSawInclude = newarray;
        }
        fSawInclude[depth] = val;
    }

    /**
     * Return whether an &lt;include&gt; was encountered at the specified depth,
     * as an ancestor of the current item.
     *
     * @param depth
     * @return true if an include was seen at the given depth, false otherwise
     */
    protected boolean getSawInclude(int depth) {
        if (depth >= fSawInclude.length) {
            return false;
        }
        return fSawInclude[depth];
    }

    protected void reportResourceError(String key) {
        this.reportResourceError(key, null);
    }

    protected void reportResourceError(String key, Object[] args) {
        this.reportResourceError(key, args, null);
    }

    protected void reportResourceError(String key, Object[] args, Exception exception) {
        this.reportError(key, args, XMLErrorReporter.SEVERITY_WARNING, exception);
    }

    protected void reportFatalError(String key) {
        this.reportFatalError(key, null);
    }

    protected void reportFatalError(String key, Object[] args) {
        this.reportFatalError(key, args, null);
    }

    protected void reportFatalError(String key, Object[] args, Exception exception) {
        this.reportError(key, args, XMLErrorReporter.SEVERITY_FATAL_ERROR, exception);
    }

    private void reportError(String key, Object[] args, short severity, Exception exception) {
        if (fErrorReporter != null) {
            fErrorReporter.reportError(
                XIncludeMessageFormatter.XINCLUDE_DOMAIN,
                key,
                args,
                severity,
                exception);
        }
    }

    /**
     * Set the parent of this XIncludeHandler in the tree
     * @param parent
     */
    protected void setParent(XIncludeHandler parent) {
        fParentXIncludeHandler = parent;
    }

    protected void setHref(String href) {
       fHrefFromParent = href;
    }

    protected void setXIncludeLocator(XMLLocatorWrapper locator) {
        fXIncludeLocator = locator;
    }

    protected boolean isRootDocument() {
        return fParentXIncludeHandler == null;
    }

    /**
     * Caches an unparsed entity.
     * @param name the name of the unparsed entity
     * @param identifier the location of the unparsed entity
     * @param augmentations any Augmentations that were on the original unparsed entity declaration
     */
    protected void addUnparsedEntity(
        String name,
        XMLResourceIdentifier identifier,
        String notation,
        Augmentations augmentations) {
        UnparsedEntity ent = new UnparsedEntity();
        ent.name = name;
        ent.systemId = identifier.getLiteralSystemId();
        ent.publicId = identifier.getPublicId();
        ent.baseURI = identifier.getBaseSystemId();
        ent.expandedSystemId = identifier.getExpandedSystemId();
        ent.notation = notation;
        ent.augmentations = augmentations;
        fUnparsedEntities.add(ent);
    }

    /**
     * Caches a notation.
     * @param name the name of the notation
     * @param identifier the location of the notation
     * @param augmentations any Augmentations that were on the original notation declaration
     */
    protected void addNotation(
        String name,
        XMLResourceIdentifier identifier,
        Augmentations augmentations) {
        Notation not = new Notation();
        not.name = name;
        not.systemId = identifier.getLiteralSystemId();
        not.publicId = identifier.getPublicId();
        not.baseURI = identifier.getBaseSystemId();
        not.expandedSystemId = identifier.getExpandedSystemId();
        not.augmentations = augmentations;
        fNotations.add(not);
    }

    /**
     * Checks if an UnparsedEntity with the given name was declared in the DTD of the document
     * for the current pipeline.  If so, then the notation for the UnparsedEntity is checked.
     * If that turns out okay, then the UnparsedEntity is passed to the root pipeline to
     * be checked for conflicts, and sent to the root DTDHandler.
     *
     * @param entName the name of the UnparsedEntity to check
     */
    protected void checkUnparsedEntity(String entName) {
        UnparsedEntity ent = new UnparsedEntity();
        ent.name = entName;
        int index = fUnparsedEntities.indexOf(ent);
        if (index != -1) {
            ent = fUnparsedEntities.get(index);
            checkNotation(ent.notation);
            checkAndSendUnparsedEntity(ent);
        }
    }

    /**
     * Checks if a Notation with the given name was declared in the DTD of the document
     * for the current pipeline.  If so, that Notation is passed to the root pipeline to
     * be checked for conflicts, and sent to the root DTDHandler
     *
     * @param notName the name of the Notation to check
     */
    protected void checkNotation(String notName) {
        Notation not = new Notation();
        not.name = notName;
        int index = fNotations.indexOf(not);
        if (index != -1) {
            not = fNotations.get(index);
            checkAndSendNotation(not);
        }
    }

    /**
     * The purpose of this method is to check if an UnparsedEntity conflicts with a previously
     * declared entity in the current pipeline stack.  If there is no conflict, the
     * UnparsedEntity is sent by the root pipeline.
     *
     * @param ent the UnparsedEntity to check for conflicts
     */
    protected void checkAndSendUnparsedEntity(UnparsedEntity ent) {
        if (isRootDocument()) {
            int index = fUnparsedEntities.indexOf(ent);
            if (index == -1) {
                XMLResourceIdentifier id =
                    new XMLResourceIdentifierImpl(
                        ent.publicId,
                        ent.systemId,
                        ent.baseURI,
                        ent.expandedSystemId);
                addUnparsedEntity(
                    ent.name,
                    id,
                    ent.notation,
                    ent.augmentations);
                if (fSendUEAndNotationEvents && fDTDHandler != null) {
                    fDTDHandler.unparsedEntityDecl(
                        ent.name,
                        id,
                        ent.notation,
                        ent.augmentations);
                }
            }
            else {
                UnparsedEntity localEntity = fUnparsedEntities.get(index);
                if (!ent.isDuplicate(localEntity)) {
                    reportFatalError(
                        "NonDuplicateUnparsedEntity",
                        new Object[] { ent.name });
                }
            }
        }
        else {
            fParentXIncludeHandler.checkAndSendUnparsedEntity(ent);
        }
    }

    /**
     * The purpose of this method is to check if a Notation conflicts with a previously
     * declared notation in the current pipeline stack.  If there is no conflict, the
     * Notation is sent by the root pipeline.
     *
     * @param not the Notation to check for conflicts
     */
    protected void checkAndSendNotation(Notation not) {
        if (isRootDocument()) {
            int index = fNotations.indexOf(not);
            if (index == -1) {
                XMLResourceIdentifier id =
                    new XMLResourceIdentifierImpl(
                        not.publicId,
                        not.systemId,
                        not.baseURI,
                        not.expandedSystemId);
                addNotation(not.name, id, not.augmentations);
                if (fSendUEAndNotationEvents && fDTDHandler != null) {
                    fDTDHandler.notationDecl(not.name, id, not.augmentations);
                }
            }
            else {
                Notation localNotation = fNotations.get(index);
                if (!not.isDuplicate(localNotation)) {
                    reportFatalError(
                        "NonDuplicateNotation",
                        new Object[] { not.name });
                }
            }
        }
        else {
            fParentXIncludeHandler.checkAndSendNotation(not);
        }
    }

    /**
     * Checks whether the string only contains white space characters.
     *
     * @param value the text to check
     */
    private void checkWhitespace(XMLString value) {
        int end = value.offset + value.length;
        for (int i = value.offset; i < end; ++i) {
            if (!XMLChar.isSpace(value.ch[i])) {
                reportFatalError("ContentIllegalAtTopLevel");
                return;
            }
        }
    }

    /**
     * Checks whether the root element has already been processed.
     */
    private void checkMultipleRootElements() {
        if (getRootElementProcessed()) {
            reportFatalError("MultipleRootElements");
        }
        setRootElementProcessed(true);
    }

    /**
     * Sets whether the root element has been processed.
     */
    private void setRootElementProcessed(boolean seenRoot) {
        if (isRootDocument()) {
            fSeenRootElement = seenRoot;
            return;
        }
        fParentXIncludeHandler.setRootElementProcessed(seenRoot);
    }

    /**
     * Returns whether the root element has been processed.
     */
    private boolean getRootElementProcessed() {
        return isRootDocument() ? fSeenRootElement : fParentXIncludeHandler.getRootElementProcessed();
    }

    protected void copyFeatures(
        XMLComponentManager from,
        ParserConfigurationSettings to) {
        Enumeration<Object> features = Constants.getXercesFeatures();
        copyFeatures1(features, Constants.XERCES_FEATURE_PREFIX, from, to);
        features = Constants.getSAXFeatures();
        copyFeatures1(features, Constants.SAX_FEATURE_PREFIX, from, to);
    }

    protected void copyFeatures(
        XMLComponentManager from,
        XMLParserConfiguration to) {
        Enumeration<Object> features = Constants.getXercesFeatures();
        copyFeatures1(features, Constants.XERCES_FEATURE_PREFIX, from, to);
        features = Constants.getSAXFeatures();
        copyFeatures1(features, Constants.SAX_FEATURE_PREFIX, from, to);
    }

    private void copyFeatures1(
        Enumeration<Object> features,
        String featurePrefix,
        XMLComponentManager from,
        ParserConfigurationSettings to) {
        while (features.hasMoreElements()) {
            String featureId = featurePrefix + (String)features.nextElement();

            to.addRecognizedFeatures(new String[] { featureId });

            try {
                to.setFeature(featureId, from.getFeature(featureId));
            }
            catch (XMLConfigurationException e) {
            }
        }
    }

    private void copyFeatures1(
        Enumeration<Object> features,
        String featurePrefix,
        XMLComponentManager from,
        XMLParserConfiguration to) {
        while (features.hasMoreElements()) {
            String featureId = featurePrefix + (String)features.nextElement();
            boolean value = from.getFeature(featureId);

            try {
                to.setFeature(featureId, value);
            }
            catch (XMLConfigurationException e) {
            }
        }
    }

    protected static class Notation {
        public String name;
        public String systemId;
        public String baseURI;
        public String publicId;
        public String expandedSystemId;
        public Augmentations augmentations;

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof Notation
                    && Objects.equals(name, ((Notation)obj).name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }

        public boolean isDuplicate(Object obj) {
            if (obj != null && obj instanceof Notation) {
                Notation other = (Notation)obj;
                return Objects.equals(name, other.name)
                && Objects.equals(publicId, other.publicId)
                && Objects.equals(expandedSystemId, other.expandedSystemId);
            }
            return false;
        }
    }

    protected static class UnparsedEntity {
        public String name;
        public String systemId;
        public String baseURI;
        public String publicId;
        public String expandedSystemId;
        public String notation;
        public Augmentations augmentations;

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof UnparsedEntity
                    && Objects.equals(name, ((UnparsedEntity)obj).name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }

        public boolean isDuplicate(Object obj) {
            if (obj != null && obj instanceof UnparsedEntity) {
                UnparsedEntity other = (UnparsedEntity)obj;
                return Objects.equals(name, other.name)
                && Objects.equals(publicId, other.publicId)
                && Objects.equals(expandedSystemId, other.expandedSystemId)
                && Objects.equals(notation, other.notation);
            }
            return false;
        }
    }


    /**
     * Saves the current base URI to the top of the stack.
     */
    protected void saveBaseURI() {
        fBaseURIScope.push(fDepth);
        fBaseURI.push(fCurrentBaseURI.getBaseSystemId());
        fLiteralSystemID.push(fCurrentBaseURI.getLiteralSystemId());
        fExpandedSystemID.push(fCurrentBaseURI.getExpandedSystemId());
    }

    /**
     * Discards the URIs at the top of the stack, and restores the ones beneath it.
     */
    protected void restoreBaseURI() {
        fBaseURI.pop();
        fLiteralSystemID.pop();
        fExpandedSystemID.pop();
        fBaseURIScope.pop();
        fCurrentBaseURI.setBaseSystemId(fBaseURI.peek());
        fCurrentBaseURI.setLiteralSystemId(fLiteralSystemID.peek());
        fCurrentBaseURI.setExpandedSystemId(fExpandedSystemID.peek());
    }


    /**
     * Saves the given language on the top of the stack.
     *
     * @param language the language to push onto the stack.
     */
    protected void saveLanguage(String language) {
        fLanguageScope.push(fDepth);
        fLanguageStack.push(language);
    }

    /**
     * Discards the language at the top of the stack, and returns the one beneath it.
     */
    public String restoreLanguage() {
        fLanguageStack.pop();
        fLanguageScope.pop();
        return fLanguageStack.peek();
    }

    /**
     * Gets the base URI that was in use at that depth
     * @param depth
     * @return the base URI
     */
    public String getBaseURI(int depth) {
        int scope = scopeOfBaseURI(depth);
        return fExpandedSystemID.get(scope);
    }

    /**
     * Gets the language that was in use at that depth.
     * @param depth
     * @return the language
     */
    public String getLanguage(int depth) {
        int scope = scopeOfLanguage(depth);
        return fLanguageStack.get(scope);
    }

    /**
     * Returns a relative URI, which when resolved against the base URI at the
     * specified depth, will create the current base URI.
     * This is accomplished by merged the literal system IDs.
     * @param depth the depth at which to start creating the relative URI
     * @return a relative URI to convert the base URI at the given depth to the current
     *         base URI
     */
    public String getRelativeURI(int depth) throws MalformedURIException {
        int start = scopeOfBaseURI(depth) + 1;
        if (start == fBaseURIScope.size()) {
            return "";
        }
        URI uri = new URI("file", fLiteralSystemID.get(start));
        for (int i = start + 1; i < fBaseURIScope.size(); i++) {
            uri = new URI(uri, fLiteralSystemID.get(i));
        }
        return uri.getPath();
    }

    private int scopeOfBaseURI(int depth) {
        for (int i = fBaseURIScope.size() - 1; i >= 0; i--) {
            if (fBaseURIScope.elementAt(i) <= depth)
                return i;
        }
        return -1;
    }

    private int scopeOfLanguage(int depth) {
        for (int i = fLanguageScope.size() - 1; i >= 0; i--) {
            if (fLanguageScope.elementAt(i) <= depth)
                return i;
        }
        return -1;
    }

    /**
     * Search for a xml:base attribute, and if one is found, put the new base URI into
     * effect.
     */
    protected void processXMLBaseAttributes(XMLAttributes attributes) {
        String baseURIValue =
            attributes.getValue(NamespaceContext.XML_URI, "base");
        if (baseURIValue != null) {
            try {
                String expandedValue =
                    XMLEntityManager.expandSystemId(
                        baseURIValue,
                        fCurrentBaseURI.getExpandedSystemId(),
                        false);
                fCurrentBaseURI.setLiteralSystemId(baseURIValue);
                fCurrentBaseURI.setBaseSystemId(
                    fCurrentBaseURI.getExpandedSystemId());
                fCurrentBaseURI.setExpandedSystemId(expandedValue);

                saveBaseURI();
            }
            catch (MalformedURIException e) {
            }
        }
    }

    /**
     * Search for a xml:lang attribute, and if one is found, put the new
     * [language] into effect.
     */
    protected void processXMLLangAttributes(XMLAttributes attributes) {
        String language = attributes.getValue(NamespaceContext.XML_URI, "lang");
        if (language != null) {
            fCurrentLanguage = language;
            saveLanguage(fCurrentLanguage);
        }
    }

    /**
     * Returns <code>true</code> if the given string
     * would be valid in an HTTP header.
     *
     * @param value string to check
     * @return <code>true</code> if the given string
     * would be valid in an HTTP header
     */
    private boolean isValidInHTTPHeader (String value) {
        char ch;
        for (int i = value.length() - 1; i >= 0; --i) {
            ch = value.charAt(i);
            if (ch < 0x20 || ch > 0x7E) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a new <code>XMLInputSource</code> from the given parameters.
     */
    private XMLInputSource createInputSource(String publicId,
            String systemId, String baseSystemId,
            String accept, String acceptLanguage) {

        HTTPInputSource httpSource = new HTTPInputSource(publicId, systemId, baseSystemId);
        if (accept != null && accept.length() > 0) {
            httpSource.setHTTPRequestProperty(XIncludeHandler.HTTP_ACCEPT, accept);
        }
        if (acceptLanguage != null && acceptLanguage.length() > 0) {
            httpSource.setHTTPRequestProperty(XIncludeHandler.HTTP_ACCEPT_LANGUAGE, acceptLanguage);
        }
        return httpSource;
    }

    private static final boolean gNeedEscaping[] = new boolean[128];
    private static final char gAfterEscaping1[] = new char[128];
    private static final char gAfterEscaping2[] = new char[128];
    private static final char[] gHexChs = {'0', '1', '2', '3', '4', '5', '6', '7',
                                           '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    static {
        char[] escChs = {' ', '<', '>', '"', '{', '}', '|', '\\', '^', '`'};
        int len = escChs.length;
        char ch;
        for (int i = 0; i < len; i++) {
            ch = escChs[i];
            gNeedEscaping[ch] = true;
            gAfterEscaping1[ch] = gHexChs[ch >> 4];
            gAfterEscaping2[ch] = gHexChs[ch & 0xf];
        }
    }

    private String escapeHref(String href) {
        int len = href.length();
        int ch;
        final StringBuilder buffer = new StringBuilder(len*3);

        int i = 0;
        for (; i < len; i++) {
            ch = href.charAt(i);
            if (ch > 0x7E) {
                break;
            }
            if (ch < 0x20) {
                return href;
            }
            if (gNeedEscaping[ch]) {
                buffer.append('%');
                buffer.append(gAfterEscaping1[ch]);
                buffer.append(gAfterEscaping2[ch]);
            }
            else {
                buffer.append((char)ch);
            }
        }

        if (i < len) {
            for (int j = i; j < len; ++j) {
                ch = href.charAt(j);
                if ((ch >= 0x20 && ch <= 0x7E) ||
                    (ch >= 0xA0 && ch <= 0xD7FF) ||
                    (ch >= 0xF900 && ch <= 0xFDCF) ||
                    (ch >= 0xFDF0 && ch <= 0xFFEF)) {
                    continue;
                }
                if (XMLChar.isHighSurrogate(ch) && ++j < len) {
                    int ch2 = href.charAt(j);
                    if (XMLChar.isLowSurrogate(ch2)) {
                        ch2 = XMLChar.supplemental((char)ch, (char)ch2);
                        if (ch2 < 0xF0000 && (ch2 & 0xFFFF) <= 0xFFFD) {
                            continue;
                        }
                    }
                }
                return href;
            }

            byte[] bytes = null;
            byte b;
            try {
                bytes = href.substring(i).getBytes("UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                return href;
            }
            len = bytes.length;

            for (i = 0; i < len; i++) {
                b = bytes[i];
                if (b < 0) {
                    ch = b + 256;
                    buffer.append('%');
                    buffer.append(gHexChs[ch >> 4]);
                    buffer.append(gHexChs[ch & 0xf]);
                }
                else if (gNeedEscaping[b]) {
                    buffer.append('%');
                    buffer.append(gAfterEscaping1[b]);
                    buffer.append(gAfterEscaping2[b]);
                }
                else {
                    buffer.append((char)b);
                }
            }
        }

        if (buffer.length() != len) {
            return buffer.toString();
        }
        else {
            return href;
        }
    }
}
