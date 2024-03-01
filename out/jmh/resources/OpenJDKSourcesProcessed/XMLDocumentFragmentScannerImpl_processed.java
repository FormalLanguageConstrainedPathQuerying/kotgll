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

import com.sun.org.apache.xerces.internal.impl.io.MalformedByteSequenceException;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.util.AugmentationsImpl;
import com.sun.org.apache.xerces.internal.util.XMLAttributesIteratorImpl;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLStringBuffer;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentScanner;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.xml.internal.stream.XMLBufferListener;
import com.sun.xml.internal.stream.XMLEntityStorage;
import com.sun.xml.internal.stream.dtd.DTDGrammarUtil;
import java.io.CharConversionException;
import java.io.EOFException;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkXmlUtils;
import jdk.xml.internal.SecuritySupport;
import jdk.xml.internal.XMLSecurityManager;
import jdk.xml.internal.XMLSecurityManager.Limit;

/**
 *
 * This class is responsible for scanning the structure and content
 * of document fragments.
 *
 * This class has been modified as per the new design which is more suited to
 * efficiently build pull parser. Lot of improvements have been done and
 * the code has been added to support stax functionality/features.
 *
 * @author Neeraj Bajaj SUN Microsystems
 * @author K.Venugopal SUN Microsystems
 * @author Glenn Marcy, IBM
 * @author Andy Clark, IBM
 * @author Arnaud  Le Hors, IBM
 * @author Eric Ye, IBM
 * @author Sunitha Reddy, SUN Microsystems
 *
 * @LastModified: Nov 2023
 */
public class XMLDocumentFragmentScannerImpl
        extends XMLScanner
        implements XMLDocumentScanner, XMLComponent, XMLEntityHandler, XMLBufferListener {


    protected int fElementAttributeLimit, fXMLNameLimit;

    /** External subset resolver. **/
    protected ExternalSubsetResolver fExternalSubsetResolver;


    /** Scanner state: start of markup. */
    protected static final int SCANNER_STATE_START_OF_MARKUP = 21;

    /** Scanner state: content. */
    protected static final int SCANNER_STATE_CONTENT = 22;

    /** Scanner state: processing instruction. */
    protected static final int SCANNER_STATE_PI = 23;

    /** Scanner state: DOCTYPE. */
    protected static final int SCANNER_STATE_DOCTYPE = 24;

    /** Scanner state: XML Declaration */
    protected static final int SCANNER_STATE_XML_DECL = 25;

    /** Scanner state: root element. */
    protected static final int SCANNER_STATE_ROOT_ELEMENT = 26;

    /** Scanner state: comment. */
    protected static final int SCANNER_STATE_COMMENT = 27;

    /** Scanner state: reference. */
    protected static final int SCANNER_STATE_REFERENCE = 28;

    protected static final int SCANNER_STATE_ATTRIBUTE = 29;

    protected static final int SCANNER_STATE_ATTRIBUTE_VALUE = 30;

    /** Scanner state: trailing misc. USED BY DOCUMENT_SCANNER_IMPL*/

    /** Scanner state: end of input. */
    protected static final int SCANNER_STATE_END_OF_INPUT = 33;

    /** Scanner state: terminated. */
    protected static final int SCANNER_STATE_TERMINATED = 34;

    /** Scanner state: CDATA section. */
    protected static final int SCANNER_STATE_CDATA = 35;

    /** Scanner state: Text declaration. */
    protected static final int SCANNER_STATE_TEXT_DECL = 36;

    /** Scanner state: Text declaration. */
    protected static final int SCANNER_STATE_CHARACTER_DATA = 37;

    protected static final int SCANNER_STATE_START_ELEMENT_TAG = 38;

    protected static final int SCANNER_STATE_END_ELEMENT_TAG = 39;

    protected static final int SCANNER_STATE_CHAR_REFERENCE = 40;
    protected static final int SCANNER_STATE_BUILT_IN_REFS = 41;



    /** Feature identifier: notify built-in refereces. */
    protected static final String NOTIFY_BUILTIN_REFS =
            Constants.XERCES_FEATURE_PREFIX + Constants.NOTIFY_BUILTIN_REFS_FEATURE;

    /** Property identifier: entity resolver. */
    protected static final String ENTITY_RESOLVER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

    /** Feature identifier: standard uri conformant */
    protected static final String STANDARD_URI_CONFORMANT =
            Constants.XERCES_FEATURE_PREFIX +Constants.STANDARD_URI_CONFORMANT_FEATURE;

    /** Feature id: create entity ref nodes. */
    protected static final String CREATE_ENTITY_REF_NODES =
            Constants.XERCES_FEATURE_PREFIX + Constants.CREATE_ENTITY_REF_NODES_FEATURE;

    /** Property identifier: Security property manager. */
    private static final String XML_SECURITY_PROPERTY_MANAGER =
            JdkConstants.XML_SECURITY_PROPERTY_MANAGER;

    /** access external dtd: file protocol
     *  For DOM/SAX, the secure feature is set to true by default
     */
    final static String EXTERNAL_ACCESS_DEFAULT = JdkConstants.EXTERNAL_ACCESS_DEFAULT;


    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
                NAMESPACES,
                VALIDATION,
                NOTIFY_BUILTIN_REFS,
                NOTIFY_CHAR_REFS,
                Constants.STAX_REPORT_CDATA_EVENT,
                XMLConstants.USE_CATALOG
    };

    /** Feature defaults. */
    private static final Boolean[] FEATURE_DEFAULTS = {
                Boolean.TRUE,
                null,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                JdkXmlUtils.USE_CATALOG_DEFAULT
    };

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
                SYMBOL_TABLE,
                ERROR_REPORTER,
                ENTITY_MANAGER,
                XML_SECURITY_PROPERTY_MANAGER,
                JdkXmlUtils.CATALOG_DEFER,
                JdkXmlUtils.CATALOG_FILES,
                JdkXmlUtils.CATALOG_PREFER,
                JdkXmlUtils.CATALOG_RESOLVE,
                JdkConstants.CDATA_CHUNK_SIZE
    };

    /** Property defaults. */
    private static final Object[] PROPERTY_DEFAULTS = {
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                JdkConstants.CDATA_CHUNK_SIZE_DEFAULT
    };


    private static final char [] CDATA = {'[','C','D','A','T','A','['};
    static final char [] XMLDECL = {'<','?','x','m','l'};

    /** Debug scanner state. */
    private static final boolean DEBUG_SCANNER_STATE = false;

    /** Debug driver. */
    private static final boolean DEBUG_DISPATCHER = false;

    /** Debug content driver scanning. */
    protected static final boolean DEBUG_START_END_ELEMENT = false;

    /** Debug driver next */
    protected static final boolean DEBUG = false;



    /** Document handler. */
    protected XMLDocumentHandler fDocumentHandler;
    protected int fScannerLastState ;

    /** Entity Storage */
    protected XMLEntityStorage fEntityStore;

    /** Entity stack. */
    protected int[] fEntityStack = new int[4];

    /** Markup depth. */
    protected int fMarkupDepth;

    protected boolean fEmptyElement ;

    protected boolean fReadingAttributes = false;

    /** Scanner state. */
    protected int fScannerState;

    /** SubScanner state: inside scanContent method. */
    protected boolean fInScanContent = false;
    protected boolean fLastSectionWasCData = false;
    protected boolean fCDataStart = false;
    protected boolean fInCData = false;
    protected boolean fCDataEnd = false;
    protected boolean fLastSectionWasEntityReference = false;
    protected boolean fLastSectionWasCharacterData = false;

    /** has external dtd */
    protected boolean fHasExternalDTD;

    /** Standalone. */
    protected boolean fStandaloneSet;
    protected boolean fStandalone;
    protected String fVersion;


    /** Current element. */
    protected QName fCurrentElement;

    /** Element stack. */
    protected ElementStack fElementStack = new ElementStack();
    protected ElementStack2 fElementStack2 = new ElementStack2();


    /** Document system identifier.
     * REVISIT:  So what's this used for?  - NG
     * protected String fDocumentSystemId;
     ******/

    protected String fPITarget ;

    protected XMLString fPIData  = new XMLString();



    /** Notify built-in references. */
    protected boolean fNotifyBuiltInRefs = false;

    protected boolean fSupportDTD = true;
    protected boolean fReplaceEntityReferences = true;
    protected boolean fSupportExternalEntities = false;
    protected boolean fReportCdataEvent = false ;
    protected boolean fIsCoalesce = false ;
    protected String fDeclaredEncoding =  null;
    /** Xerces Feature: Disallow doctype declaration. */
    protected boolean fDisallowDoctype = false;
    protected String fDTDErrorCode = null;

    /** Create entity reference nodes. */
    protected boolean fCreateEntityRefNodes = false;

    /**
     * CDATA chunk size limit
     */
    private int fChunkSize;

    /**
     * comma-delimited list of protocols that are allowed for the purpose
     * of accessing external dtd or entity references
     */
    protected String fAccessExternalDTD = EXTERNAL_ACCESS_DEFAULT;

    /**
     * Properties to determine whether to use a user-specified Catalog:
     * Feature USE_CATALOG, Resolve and Catalog File
     */
    protected boolean fUseCatalog = true;
    protected String fCatalogFile;

    /**
     * standard uri conformant (strict uri).
     * http:
     */
    protected boolean fStrictURI;


    /** Active driver. */
    protected Driver fDriver;

    /** Content driver. */
    protected Driver fContentDriver = createContentDriver();


    /** Element QName. */
    protected QName fElementQName = new QName();

    /** Attribute QName. */
    protected QName fAttributeQName = new QName();

    /**
     * CHANGED: Using XMLAttributesIteratorImpl instead of XMLAttributesImpl. This class
     * implements Iterator interface so we can directly give Attributes in the form of
     * iterator.
     */
    protected XMLAttributesIteratorImpl fAttributes = new XMLAttributesIteratorImpl();


    /** String. */
    protected XMLString fTempString = new XMLString();

    /** String. */
    protected XMLString fTempString2 = new XMLString();

    /** Array of 3 strings. */
    private final String[] fStrings = new String[3];

    /** Making the buffer accessible to derived class -- String buffer. */
    protected XMLStringBuffer fStringBuffer = new XMLStringBuffer();

    /** Making the buffer accessible to derived class -- String buffer. */
    protected XMLStringBuffer fStringBuffer2 = new XMLStringBuffer();

    /** stores character data. */
    /** Making the buffer accessible to derived class -- stores PI data */
    protected XMLStringBuffer fContentBuffer = new XMLStringBuffer();

    /** Single character array. */
    private final char[] fSingleChar = new char[1];
    private String fCurrentEntityName = null;

    protected boolean fScanToEnd = false;

    protected DTDGrammarUtil dtdGrammarUtil= null;

    protected boolean fAddDefaultAttr = false;

    protected boolean foundBuiltInRefs = false;

    /** Built-in reference character event */
    protected boolean builtInRefCharacterHandled = false;

    static final short MAX_DEPTH_LIMIT = 5 ;
    static final short ELEMENT_ARRAY_LENGTH = 200 ;
    static final short MAX_POINTER_AT_A_DEPTH = 4 ;
    static final boolean DEBUG_SKIP_ALGORITHM = false;
    String [] fElementArray = new String[ELEMENT_ARRAY_LENGTH] ;
    short fLastPointerLocation = 0 ;
    short fElementPointer = 0 ;
    short [] [] fPointerInfo = new short[MAX_DEPTH_LIMIT] [MAX_POINTER_AT_A_DEPTH] ;
    protected String fElementRawname ;
    protected boolean fShouldSkip = false;
    protected boolean fAdd = false ;
    protected boolean fSkip = false;

    /** Reusable Augmentations. */
    private Augmentations fTempAugmentations = null;

    /** Default constructor. */
    public XMLDocumentFragmentScannerImpl() {
    } 


    /**
     * Sets the input source.
     *
     * @param inputSource The input source.
     *
     * @throws IOException Thrown on i/o error.
     */
    public void setInputSource(XMLInputSource inputSource) throws IOException {
        fEntityManager.setEntityHandler(this);
        fEntityManager.startEntity(false, "$fragment$", inputSource, false, true);
    } 

    /**
     * Scans a document.
     *
     * @param complete True if the scanner should scan the document
     *                 completely, pushing all events to the registered
     *                 document handler. A value of false indicates that
     *                 that the scanner should only scan the next portion
     *                 of the document and return. A scanner instance is
     *                 permitted to completely scan a document if it does
     *                 not support this "pull" scanning model.
     *
     * @return True if there is more to scan, false otherwise.
     */
    public boolean scanDocument(boolean complete)
    throws IOException, XNIException {

        fEntityManager.setEntityHandler(this);

        int event = next();
        do {
            switch (event) {
                case XMLStreamConstants.START_DOCUMENT :
                    break;
                case XMLStreamConstants.START_ELEMENT :
                    break;
                case XMLStreamConstants.CHARACTERS :
                    fEntityScanner.checkNodeCount(fEntityScanner.fCurrentEntity);
                    fDocumentHandler.characters(getCharacterData(),null);
                    break;
                case XMLStreamConstants.SPACE:
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE :
                    fEntityScanner.checkNodeCount(fEntityScanner.fCurrentEntity);
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION :
                    fEntityScanner.checkNodeCount(fEntityScanner.fCurrentEntity);
                    fDocumentHandler.processingInstruction(getPITarget(),getPIData(),null);
                    break;
                case XMLStreamConstants.COMMENT :
                    fEntityScanner.checkNodeCount(fEntityScanner.fCurrentEntity);
                    fDocumentHandler.comment(getCharacterData(),null);
                    break;
                case XMLStreamConstants.DTD :
                    break;
                case XMLStreamConstants.CDATA:
                   fEntityScanner.checkNodeCount(fEntityScanner.fCurrentEntity);
                    if (fCDataStart) {
                        fDocumentHandler.startCDATA(null);
                        fCDataStart = false;
                        fInCData = true;
                    }

                    fDocumentHandler.characters(getCharacterData(),null);
                    if (fCDataEnd) {
                        fDocumentHandler.endCDATA(null);
                        fCDataEnd = false;
                    }
                    break;
                case XMLStreamConstants.NOTATION_DECLARATION :
                    break;
                case XMLStreamConstants.ENTITY_DECLARATION :
                    break;
                case XMLStreamConstants.NAMESPACE :
                    break;
                case XMLStreamConstants.ATTRIBUTE :
                    break;
                case XMLStreamConstants.END_ELEMENT :
                    break;
                default :
                    return false;

            }
            event = next();
        } while (event!=XMLStreamConstants.END_DOCUMENT && complete);

        if(event == XMLStreamConstants.END_DOCUMENT) {
            fDocumentHandler.endDocument(null);
            return false;
        }

        return true;

    } 



    public com.sun.org.apache.xerces.internal.xni.QName getElementQName(){
        if(fScannerLastState == XMLEvent.END_ELEMENT){
            fElementQName.setValues(fElementStack.getLastPoppedElement());
        }
        return fElementQName ;
    }

    /** return the next state on the input
     * @return int
     */

    public int next() throws IOException, XNIException {
        return fDriver.next();
    }


    /**
     * Resets the component. The component can query the component manager
     * about any features and properties that affect the operation of the
     * component.
     *
     * @param componentManager The component manager.
     *
     * @throws SAXException Thrown by component on initialization error.
     *                      For example, if a feature or property is
     *                      required for the operation of the component, the
     *                      component manager may throw a
     *                      SAXNotRecognizedException or a
     *                      SAXNotSupportedException.
     */

    public void reset(XMLComponentManager componentManager)
    throws XMLConfigurationException {

        super.reset(componentManager);



        fReportCdataEvent = componentManager.getFeature(Constants.STAX_REPORT_CDATA_EVENT, true);
        fSecurityManager = (XMLSecurityManager)componentManager.getProperty(Constants.SECURITY_MANAGER, null);
        fNotifyBuiltInRefs = componentManager.getFeature(NOTIFY_BUILTIN_REFS, false);

        fCreateEntityRefNodes = componentManager.getFeature(CREATE_ENTITY_REF_NODES, fCreateEntityRefNodes);

        Object resolver = componentManager.getProperty(ENTITY_RESOLVER, null);
        fExternalSubsetResolver = (resolver instanceof ExternalSubsetResolver) ?
                (ExternalSubsetResolver) resolver : null;

        fReadingAttributes = false;
        fSupportExternalEntities = true;
        fReplaceEntityReferences = true;
        fIsCoalesce = false;

        setScannerState(SCANNER_STATE_CONTENT);
        setDriver(fContentDriver);

        XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager)
                componentManager.getProperty(XML_SECURITY_PROPERTY_MANAGER, null);
        fAccessExternalDTD = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);

        fStrictURI = componentManager.getFeature(STANDARD_URI_CONFORMANT, false);
        fChunkSize = JdkXmlUtils.getValue(componentManager.getProperty(JdkConstants.CDATA_CHUNK_SIZE),
                JdkConstants.CDATA_CHUNK_SIZE_DEFAULT);

        resetCommon();
    } 


    public void reset(PropertyManager propertyManager){

        super.reset(propertyManager);

        fNamespaces = ((Boolean)propertyManager.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE));
        fNotifyBuiltInRefs = false ;

        Boolean bo = (Boolean)propertyManager.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES);
        fReplaceEntityReferences = bo;
        bo = (Boolean)propertyManager.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES);
        fSupportExternalEntities = bo;
        Boolean cdata = (Boolean)propertyManager.getProperty(
                Constants.ZEPHYR_PROPERTY_PREFIX + Constants.STAX_REPORT_CDATA_EVENT) ;
        if(cdata != null)
            fReportCdataEvent = cdata ;
        Boolean coalesce = (Boolean)propertyManager.getProperty(XMLInputFactory.IS_COALESCING) ;
        if(coalesce != null)
            fIsCoalesce = coalesce;
        fReportCdataEvent = fIsCoalesce ? false : (fReportCdataEvent && true) ;
        fReplaceEntityReferences = fIsCoalesce ? true : fReplaceEntityReferences;

        XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager)
                propertyManager.getProperty(XML_SECURITY_PROPERTY_MANAGER);
        fAccessExternalDTD = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);

        fSecurityManager = (XMLSecurityManager)propertyManager.getProperty(Constants.SECURITY_MANAGER);
        fChunkSize = JdkXmlUtils.getValue(propertyManager.getProperty(JdkConstants.CDATA_CHUNK_SIZE),
                JdkConstants.CDATA_CHUNK_SIZE_DEFAULT);
        resetCommon();
    } 

    void resetCommon() {
        fMarkupDepth = 0;
        fCurrentElement = null;
        fElementStack.clear();
        fHasExternalDTD = false;
        fStandaloneSet = false;
        fStandalone = false;
        fInScanContent = false;
        fShouldSkip = false;
        fAdd = false;
        fSkip = false;

        fEntityStore = fEntityManager.getEntityStore();
        dtdGrammarUtil = null;

        if (fSecurityManager != null) {
            fElementAttributeLimit = fSecurityManager.getLimit(XMLSecurityManager.Limit.ELEMENT_ATTRIBUTE_LIMIT);
            fXMLNameLimit = fSecurityManager.getLimit(XMLSecurityManager.Limit.MAX_NAME_LIMIT);
        } else {
            fElementAttributeLimit = 0;
            fXMLNameLimit = XMLSecurityManager.Limit.MAX_NAME_LIMIT.defaultValue();
        }
        fLimitAnalyzer = fEntityManager.fLimitAnalyzer;
    }

    /**
     * Returns a list of feature identifiers that are recognized by
     * this component. This method may return null if no features
     * are recognized by this component.
     */
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
    public void setFeature(String featureId, boolean state)
    throws XMLConfigurationException {

        super.setFeature(featureId, state);

        if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
            String feature = featureId.substring(Constants.XERCES_FEATURE_PREFIX.length());
            if (feature.equals(Constants.NOTIFY_BUILTIN_REFS_FEATURE)) {
                fNotifyBuiltInRefs = state;
            }
        }

    } 

    /**
     * Returns a list of property identifiers that are recognized by
     * this component. This method may return null if no properties
     * are recognized by this component.
     */
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
    public void setProperty(String propertyId, Object value)
    throws XMLConfigurationException {

        super.setProperty(propertyId, value);

        if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
            final int suffixLength = propertyId.length() - Constants.XERCES_PROPERTY_PREFIX.length();
            if (suffixLength == Constants.ENTITY_MANAGER_PROPERTY.length() &&
                    propertyId.endsWith(Constants.ENTITY_MANAGER_PROPERTY)) {
                fEntityManager = (XMLEntityManager)value;
                return;
            }
            if (suffixLength == Constants.ENTITY_RESOLVER_PROPERTY.length() &&
                    propertyId.endsWith(Constants.ENTITY_RESOLVER_PROPERTY)) {
                fExternalSubsetResolver = (value instanceof ExternalSubsetResolver) ?
                    (ExternalSubsetResolver) value : null;
                return;
            }
        }


        if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
            String property = propertyId.substring(Constants.XERCES_PROPERTY_PREFIX.length());
            if (property.equals(Constants.ENTITY_MANAGER_PROPERTY)) {
                fEntityManager = (XMLEntityManager)value;
            }
            return;
        }

        if (propertyId.equals(XML_SECURITY_PROPERTY_MANAGER))
        {
            XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager)value;
            fAccessExternalDTD = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);
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
    public Object getPropertyDefault(String propertyId) {
        for (int i = 0; i < RECOGNIZED_PROPERTIES.length; i++) {
            if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
                return PROPERTY_DEFAULTS[i];
            }
        }
        return null;
    } 


    /**
     * setDocumentHandler
     *
     * @param documentHandler
     */
    public void setDocumentHandler(XMLDocumentHandler documentHandler) {
        fDocumentHandler = documentHandler;
    } 


    /** Returns the document handler */
    public XMLDocumentHandler getDocumentHandler(){
        return fDocumentHandler;
    }


    /**
     * This method notifies of the start of an entity. The DTD has the
     * pseudo-name of "[dtd]" parameter entity names start with '%'; and
     * general entities are just specified by their name.
     *
     * @param name     The name of the entity.
     * @param identifier The resource identifier.
     * @param encoding The auto-detected IANA encoding name of the entity
     *                 stream. This value will be null in those situations
     *                 where the entity encoding is not auto-detected (e.g.
     *                 internal entities or a document entity that is
     *                 parsed from a java.io.Reader).
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startEntity(String name,
            XMLResourceIdentifier identifier,
            String encoding, Augmentations augs) throws XNIException {

        if (fEntityDepth == fEntityStack.length) {
            int[] entityarray = new int[fEntityStack.length * 2];
            System.arraycopy(fEntityStack, 0, entityarray, 0, fEntityStack.length);
            fEntityStack = entityarray;
        }
        fEntityStack[fEntityDepth] = fMarkupDepth;

        super.startEntity(name, identifier, encoding, augs);

        if(fStandalone && fEntityStore.isEntityDeclInExternalSubset(name)) {
            reportFatalError("MSG_REFERENCE_TO_EXTERNALLY_DECLARED_ENTITY_WHEN_STANDALONE",
                    new Object[]{name});
        }

        /** we are not calling the handlers yet.. */
        if (fDocumentHandler != null && !fScanningAttribute) {
            if (!name.equals("[xml]")) {
                fDocumentHandler.startGeneralEntity(name, identifier, encoding, augs);
            }
        }

    } 

    /**
     * This method notifies the end of an entity. The DTD has the pseudo-name
     * of "[dtd]" parameter entity names start with '%'; and general entities
     * are just specified by their name.
     *
     * @param name The name of the entity.
     * @param augs Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endEntity(String name, Augmentations augs) throws IOException, XNIException {

        /**
         * 
         * if (fInScanContent && fStringBuffer.length != 0
         * && fDocumentHandler != null) {
         * fDocumentHandler.characters(fStringBuffer, null);
         * fStringBuffer.length = 0; 
         * }
         */
        super.endEntity(name, augs);

        if (fMarkupDepth != fEntityStack[fEntityDepth]) {
            reportFatalError("MarkupEntityMismatch", null);
        }

        /**/
        if (fDocumentHandler != null && !fScanningAttribute) {
            if (!name.equals("[xml]")) {
                fDocumentHandler.endGeneralEntity(name, augs);
            }
        }


    } 



    /** Creates a content Driver. */
    protected Driver createContentDriver() {
        return new FragmentContentDriver();
    } 


    /**
     * Scans an XML or text declaration.
     * <p>
     * <pre>
     * [23] XMLDecl ::= '&lt;?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'
     * [24] VersionInfo ::= S 'version' Eq (' VersionNum ' | " VersionNum ")
     * [80] EncodingDecl ::= S 'encoding' Eq ('"' EncName '"' |  "'" EncName "'" )
     * [81] EncName ::= [A-Za-z] ([A-Za-z0-9._] | '-')*
     * [32] SDDecl ::= S 'standalone' Eq (("'" ('yes' | 'no') "'")
     *                 | ('"' ('yes' | 'no') '"'))
     *
     * [77] TextDecl ::= '&lt;?xml' VersionInfo? EncodingDecl S? '?>'
     * </pre>
     *
     * @param scanningTextDecl True if a text declaration is to
     *                         be scanned instead of an XML
     *                         declaration.
     */
    protected void scanXMLDeclOrTextDecl(boolean scanningTextDecl)
    throws IOException, XNIException {

        super.scanXMLDeclOrTextDecl(scanningTextDecl, fStrings);
        fMarkupDepth--;

        String version = fStrings[0];
        String encoding = fStrings[1];
        String standalone = fStrings[2];
        fDeclaredEncoding = encoding;
        fStandaloneSet = standalone != null;
        fStandalone = fStandaloneSet && standalone.equals("yes");
        fEntityManager.setStandalone(fStandalone);


        if (fDocumentHandler != null) {
            if (scanningTextDecl) {
                fDocumentHandler.textDecl(version, encoding, null);
            } else {
                fDocumentHandler.xmlDecl(version, encoding, standalone, null);
            }
        }

        if(version != null){
            fEntityScanner.setVersion(version);
            fEntityScanner.setXMLVersion(version);
        }
        if (encoding != null && !fEntityScanner.getCurrentEntity().isEncodingExternallySpecified()) {
             fEntityScanner.setEncoding(encoding);
        }

    } 

    public String getPITarget(){
        return fPITarget ;
    }

    public XMLStringBuffer getPIData(){
        return fContentBuffer ;
    }

    public XMLString getCharacterData(){
        if(fUsebuffer){
            return fContentBuffer ;
        }else{
            return fTempString;
        }

    }


    /**
     * Scans a processing data. This is needed to handle the situation
     * where a document starts with a processing instruction whose
     * target name <em>starts with</em> "xml". (e.g. xmlfoo)
     *
     * @param target The PI target
     * @param data The XMLStringBuffer to fill in with the data
     */
    protected void scanPIData(String target, XMLStringBuffer data)
    throws IOException, XNIException {

        super.scanPIData(target, data);

        fPITarget = target ;

        fMarkupDepth--;

    } 

    /**
     * Scans a comment.
     * <p>
     * <pre>
     * [15] Comment ::= '&lt!--' ((Char - '-') | ('-' (Char - '-')))* '-->'
     * </pre>
     * <p>
     * <strong>Note:</strong> Called after scanning past '&lt;!--'
     */
    protected void scanComment() throws IOException, XNIException {
        fContentBuffer.clear();
        scanComment(fContentBuffer);
        fUsebuffer = true;
        fMarkupDepth--;

    } 

    public String getComment(){
        return fContentBuffer.toString();
    }

    void addElement(String rawname){
        if(fElementPointer < ELEMENT_ARRAY_LENGTH){
            fElementArray[fElementPointer] = rawname ;

            if(DEBUG_SKIP_ALGORITHM){
                StringBuffer sb = new StringBuffer() ;
                sb.append(" Storing element information ") ;
                sb.append(" fElementPointer = " + fElementPointer) ;
                sb.append(" fElementRawname = " + fElementQName.rawname) ;
                sb.append(" fElementStack.fDepth = " + fElementStack.fDepth);
                System.out.println(sb.toString()) ;
            }

            if(fElementStack.fDepth < MAX_DEPTH_LIMIT){
                short column = storePointerForADepth(fElementPointer);
                if(column > 0){
                    short pointer = getElementPointer((short)fElementStack.fDepth, (short)(column - 1) );
                    if(rawname == fElementArray[pointer]){
                        fShouldSkip = true ;
                        fLastPointerLocation = pointer ;
                        resetPointer((short)fElementStack.fDepth , column) ;
                        fElementArray[fElementPointer] = null ;
                        return ;
                    }else{
                        fShouldSkip = false ;
                    }
                }
            }
            fElementPointer++ ;
        }
    }


    void resetPointer(short depth, short column){
        fPointerInfo[depth] [column] = (short)0;
    }

    short storePointerForADepth(short elementPointer){
        short depth = (short) fElementStack.fDepth ;

        for(short i = 0 ; i < MAX_POINTER_AT_A_DEPTH ; i++){

            if(canStore(depth, i)){
                fPointerInfo[depth][i] = elementPointer ;
                if(DEBUG_SKIP_ALGORITHM){
                    StringBuffer sb = new StringBuffer() ;
                    sb.append(" Pointer information ") ;
                    sb.append(" fElementPointer = " + fElementPointer) ;
                    sb.append(" fElementStack.fDepth = " + fElementStack.fDepth);
                    sb.append(" column = " + i ) ;
                    System.out.println(sb.toString()) ;
                }
                return i;
            }
        }
        return -1 ;
    }

    boolean canStore(short depth, short column){
        return fPointerInfo[depth][column] == 0 ? true : false ;
    }


    short getElementPointer(short depth, short column){
        return fPointerInfo[depth][column] ;
    }

    boolean skipFromTheBuffer(String rawname) throws IOException{
        if(fEntityScanner.skipString(rawname)){
            char c = (char)fEntityScanner.peekChar() ;
            if( c == ' ' || c == '/' || c == '>'){
                fElementRawname = rawname ;
                return true ;
            } else{
                return false;
            }
        } else
            return false ;
    }

    boolean skipQElement(String rawname) throws IOException{

        final int c = fEntityScanner.getChar(rawname.length());
        if(XMLChar.isName(c)){
            return false;
        }else{
            return fEntityScanner.skipString(rawname);
        }
    }

    protected boolean skipElement() throws IOException {

        if(!fShouldSkip) return false ;

        if(fLastPointerLocation != 0){
            String rawname = fElementArray[fLastPointerLocation + 1] ;
            if(rawname != null && skipFromTheBuffer(rawname)){
                fLastPointerLocation++ ;
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("Element " + fElementRawname +
                            " was SKIPPED at pointer location = " + fLastPointerLocation);
                }
                return true ;
            } else{
                fLastPointerLocation = 0 ;

            }
        }
        return fShouldSkip && skipElement((short)0);

    }

    boolean skipElement(short column) throws IOException {
        short depth = (short)fElementStack.fDepth ;

        if(depth > MAX_DEPTH_LIMIT){
            return fShouldSkip = false ;
        }
        for(short i = column ; i < MAX_POINTER_AT_A_DEPTH ; i++){
            short pointer = getElementPointer(depth , i ) ;

            if(pointer == 0){
                return fShouldSkip = false ;
            }

            if(fElementArray[pointer] != null && skipFromTheBuffer(fElementArray[pointer])){
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println();
                    System.out.println("Element " + fElementRawname + " was SKIPPED at depth = " +
                            fElementStack.fDepth + " column = " + column );
                    System.out.println();
                }
                fLastPointerLocation = pointer ;
                return fShouldSkip = true ;
            }
        }
        return fShouldSkip = false ;
    }

    /**
     * Scans a start element. This method will handle the binding of
     * namespace information and notifying the handler of the start
     * of the element.
     * <p>
     * <pre>
     * [44] EmptyElemTag ::= '&lt;' Name (S Attribute)* S? '/>'
     * [40] STag ::= '&lt;' Name (S Attribute)* S? '>'
     * </pre>
     * <p>
     * <strong>Note:</strong> This method assumes that the leading
     * '&lt;' character has been consumed.
     * <p>
     * <strong>Note:</strong> This method uses the fElementQName and
     * fAttributes variables. The contents of these variables will be
     * destroyed. The caller should copy important information out of
     * these variables before calling this method.
     * NB: Content in fAttributes is valid only till the state of the parser is XMLEvent.START_ELEMENT
     *
     * @return True if element is empty. (i.e. It matches
     *          production [44].
     */
    protected boolean scanStartElement()
    throws IOException, XNIException {

        if (DEBUG_START_END_ELEMENT) System.out.println( this.getClass().toString() + ">>> scanStartElement()");
        if(fSkip && !fAdd){

            QName name = fElementStack.getNext();

            if(DEBUG_SKIP_ALGORITHM){
                System.out.println("Trying to skip String = " + name.rawname);
            }

            fSkip = fEntityScanner.skipString(name.rawname);

            if(fSkip){
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("Element SUCESSFULLY skipped = " + name.rawname);
                }
                fElementStack.push();
                fElementQName = name;
            }else{
                fElementStack.reposition();
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("Element was NOT skipped, REPOSITIONING stack" );
                }
            }
        }

        if(!fSkip || fAdd){
            fElementQName = fElementStack.nextElement();
            if (fNamespaces) {
                fEntityScanner.scanQName(fElementQName, NameType.ELEMENTSTART);
            } else {
                String name = fEntityScanner.scanName(NameType.ELEMENTSTART);
                fElementQName.setValues(null, name, name, null);
            }

            if(DEBUG)System.out.println("Element scanned in start element is " + fElementQName.toString());
            if(DEBUG_SKIP_ALGORITHM){
                if(fAdd){
                    System.out.println("Elements are being ADDED -- elemet added is = " +
                            fElementQName.rawname + " at count = " + fElementStack.fCount);
                }
            }

        }

        if(fAdd){
            fElementStack.matchElement(fElementQName);
        }


        fCurrentElement = fElementQName;

        String rawname = fElementQName.rawname;

        fEmptyElement = false;

        fAttributes.removeAllAttributes();

        checkDepth(rawname);
        if(!seekCloseOfStartTag()){
            fReadingAttributes = true;
            fAttributeCacheUsedCount =0;
            fStringBufferIndex =0;
            fAddDefaultAttr = true;
            do {
                scanAttribute(fAttributes);
                if (fSecurityManager != null && !fSecurityManager.isNoLimit(fElementAttributeLimit) &&
                        fAttributes.getLength() > fElementAttributeLimit){
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                 "ElementAttributeLimit",
                                                 new Object[]{rawname, fElementAttributeLimit },
                                                 XMLErrorReporter.SEVERITY_FATAL_ERROR );
                }

            } while (!seekCloseOfStartTag());
            fReadingAttributes=false;
        }

        if (fEmptyElement) {
            fMarkupDepth--;

            if (fMarkupDepth < fEntityStack[fEntityDepth - 1]) {
                reportFatalError("ElementEntityMismatch",
                        new Object[]{fCurrentElement.rawname});
            }
            if (fDocumentHandler != null) {
                fDocumentHandler.emptyElement(fElementQName, fAttributes, null);
            }


            fElementStack.popElement();

        } else {

            if(dtdGrammarUtil != null)
                dtdGrammarUtil.startElement(fElementQName, fAttributes);
            if(fDocumentHandler != null){
                fDocumentHandler.startElement(fElementQName, fAttributes, null);
            }
        }


        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +
                "<<< scanStartElement(): "+fEmptyElement);
        return fEmptyElement;

    } 

    /**
     * Looks for the close of start tag, i.e. if it finds '>' or '/>'
     * Characters are consumed.
     */
    protected boolean seekCloseOfStartTag() throws IOException, XNIException {
        boolean sawSpace = fEntityScanner.skipSpaces();

        final int c = fEntityScanner.peekChar();
        if (c == '>') {
            fEntityScanner.scanChar(null);
            return true;
        } else if (c == '/') {
            fEntityScanner.scanChar(null);
            if (!fEntityScanner.skipChar('>', NameType.ELEMENTEND)) {
                reportFatalError("ElementUnterminated",
                        new Object[]{fElementQName.rawname});
            }
            fEmptyElement = true;
            return true;
        } else if (!isValidNameStartChar(c) || !sawSpace) {
            if (!isValidNameStartHighSurrogate(c) || !sawSpace) {
                reportFatalError("ElementUnterminated",
                        new Object[]{fElementQName.rawname});
            }
        }

        return false;
    }

    public boolean hasAttributes(){
        return fAttributes.getLength() > 0;
    }

    /** return the attribute iterator implementation */
    public XMLAttributesIteratorImpl getAttributeIterator(){
        if(dtdGrammarUtil != null && fAddDefaultAttr){
            dtdGrammarUtil.addDTDDefaultAttrs(fElementQName,fAttributes);
            fAddDefaultAttr = false;
        }
        return fAttributes;
    }

    /** return if standalone is set */
    public boolean standaloneSet(){
        return fStandaloneSet;
    }
    /** return if the doucment is standalone */
    public boolean isStandAlone(){
        return fStandalone ;
    }
    /**
     * Scans an attribute name value pair.
     * <p>
     * <pre>
     * [41] Attribute ::= Name Eq AttValue
     * </pre>
     * <p>
     * <strong>Note:</strong> This method assumes that the next
     * character on the stream is the first character of the attribute
     * name.
     * <p>
     * <strong>Note:</strong> This method uses the fAttributeQName and
     * fQName variables. The contents of these variables will be
     * destroyed.
     *
     * @param attributes The attributes list for the scanned attribute.
     */

    protected void scanAttribute(XMLAttributes attributes)
    throws IOException, XNIException {
        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +">>> scanAttribute()");

        if (fNamespaces) {
            fEntityScanner.scanQName(fAttributeQName, NameType.ATTRIBUTENAME);
        } else {
            String name = fEntityScanner.scanName(NameType.ATTRIBUTENAME);
            fAttributeQName.setValues(null, name, name, null);
        }

        fEntityScanner.skipSpaces();
        if (!fEntityScanner.skipChar('=', NameType.ATTRIBUTE)) {
            reportFatalError("EqRequiredInAttribute",
                new Object[] {fCurrentElement.rawname, fAttributeQName.rawname});
        }
        fEntityScanner.skipSpaces();

        int attIndex = 0 ;
        boolean isVC =  fHasExternalDTD && !fStandalone;

        XMLString tmpStr = getString();

        scanAttributeValue(tmpStr, fTempString2, fAttributeQName.rawname, attributes,
                attIndex, isVC, fCurrentElement.rawname, false);

        int oldLen = attributes.getLength();
        attIndex = attributes.addAttribute(fAttributeQName, XMLSymbols.fCDATASymbol, null);

        if (oldLen == attributes.getLength()) {
            reportFatalError("AttributeNotUnique",
                    new Object[]{fCurrentElement.rawname,
                            fAttributeQName.rawname});
        }

        attributes.setValue(attIndex, null, tmpStr);

        attributes.setSpecified(attIndex, true);

        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +"<<< scanAttribute()");

    } 

    /**
     * Scans element content.
     *
     * @return Returns the next character on the stream.
     */
    protected int scanContent(XMLStringBuffer content) throws IOException, XNIException {
        fTempString.length = 0;
        int c = fEntityScanner.scanContent(fTempString);
        content.append(fTempString);
        fTempString.length = 0;
        if (c == '\r') {
            fEntityScanner.scanChar(null);
            content.append((char)c);
            c = -1;
        } else if (c == ']') {
            content.append((char)fEntityScanner.scanChar(null));
            fInScanContent = true;
            if (fEntityScanner.skipChar(']', null)) {
                content.append(']');
                while (fEntityScanner.skipChar(']', null)) {
                    content.append(']');
                }
                if (fEntityScanner.skipChar('>', null)) {
                    reportFatalError("CDEndInContent", null);
                }
            }
            fInScanContent = false;
            c = -1;
        }
        if (fDocumentHandler != null && content.length > 0) {
        }
        return c;

    } 


    /**
     * Scans a CDATA section.
     * <p>
     * <strong>Note:</strong> This method uses the fTempString and
     * fStringBuffer variables.
     *
     * @param complete True if the CDATA section is to be scanned
     *                 completely.
     *
     * @return True if CDATA is completely scanned.
     */
    protected boolean scanCDATASection(XMLStringBuffer contentBuffer, boolean complete)
    throws IOException, XNIException {

        if (fDocumentHandler != null) {
        }

        while (true) {
            if (!fEntityScanner.scanData("]]>", contentBuffer, fChunkSize)) {
                fInCData = false;
                fCDataEnd = true;
                fMarkupDepth--;
                break ;
            } else {
                int c = fEntityScanner.peekChar();
                if (c != -1 && isInvalidLiteral(c)) {
                    if (XMLChar.isHighSurrogate(c)) {
                        scanSurrogates(contentBuffer);
                    } else {
                        reportFatalError("InvalidCharInCDSect",
                                new Object[]{Integer.toString(c,16)});
                                fEntityScanner.scanChar(null);
                    }
                } else {
                    fInCData = true;
                    fCDataEnd = false;
                    break;
                }
                if (fDocumentHandler != null) {
                }
            }
        }

        return true;

    } 

    /**
     * Scans an end element.
     * <p>
     * <pre>
     * [42] ETag ::= '&lt;/' Name S? '>'
     * </pre>
     * <p>
     * <strong>Note:</strong> This method uses the fElementQName variable.
     * The contents of this variable will be destroyed. The caller should
     * copy the needed information out of this variable before calling
     * this method.
     *
     * @return The element depth.
     */
    protected int scanEndElement() throws IOException, XNIException {
        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +">>> scanEndElement()");

        QName endElementName = fElementStack.popElement();

        String rawname = endElementName.rawname;
        if(DEBUG)System.out.println("endElementName = " + endElementName.toString());



        if (!fEntityScanner.skipString(endElementName.rawname)) {
             reportFatalError("ETagRequired", new Object[]{rawname});
        }

        fEntityScanner.skipSpaces();
        if (!fEntityScanner.skipChar('>', NameType.ELEMENTEND)) {
            reportFatalError("ETagUnterminated",
                    new Object[]{rawname});
        }
        fMarkupDepth--;

        fMarkupDepth--;

        if (fMarkupDepth < fEntityStack[fEntityDepth - 1]) {
            reportFatalError("ElementEntityMismatch",
                    new Object[]{rawname});
        }



        if (fDocumentHandler != null ) {

            fDocumentHandler.endElement(endElementName, null);
        }
        if(dtdGrammarUtil != null)
            dtdGrammarUtil.endElement(endElementName);

        return fMarkupDepth;

    } 

    /**
     * Scans a character reference.
     * <p>
     * <pre>
     * [66] CharRef ::= '&#' [0-9]+ ';' | '&#x' [0-9a-fA-F]+ ';'
     * </pre>
     */
    protected void scanCharReference()
    throws IOException, XNIException {

        fStringBuffer2.clear();
        int ch = scanCharReferenceValue(fStringBuffer2, null);
        fMarkupDepth--;
        if (ch != -1) {

            if (fDocumentHandler != null) {
                if (fNotifyCharRefs) {
                    fDocumentHandler.startGeneralEntity(fCharRefLiteral, null, null, null);
                }
                Augmentations augs = null;
                if (fValidation && ch <= 0x20) {
                    if (fTempAugmentations != null) {
                        fTempAugmentations.removeAllItems();
                    }
                    else {
                        fTempAugmentations = new AugmentationsImpl();
                    }
                    augs = fTempAugmentations;
                    augs.putItem(Constants.CHAR_REF_PROBABLE_WS, Boolean.TRUE);
                }
                if (fNotifyCharRefs) {
                    fDocumentHandler.endGeneralEntity(fCharRefLiteral, null);
                }
            }
        }

    } 


    /**
     * Scans an entity reference.
     *
     * @return returns true if the new entity is started. If it was built-in entity
     *         'false' is returned.
     * @throws IOException  Thrown if i/o error occurs.
     * @throws XNIException Thrown if handler throws exception upon
     *                      notification.
     */
    protected void scanEntityReference(XMLStringBuffer content) throws IOException, XNIException {
        String name = fEntityScanner.scanName(NameType.REFERENCE);
        if (name == null) {
            reportFatalError("NameRequiredInReference", null);
            return;
        }
        if (!fEntityScanner.skipChar(';', NameType.REFERENCE)) {
            reportFatalError("SemicolonRequiredInReference", new Object []{name});
        }
        if (fEntityStore.isUnparsedEntity(name)) {
            reportFatalError("ReferenceToUnparsedEntity", new Object[]{name});
        }
        fMarkupDepth--;
        fCurrentEntityName = name;

        if (name == fAmpSymbol) {
            handleCharacter('&', fAmpSymbol, content);
            fScannerState = SCANNER_STATE_BUILT_IN_REFS;
            return ;
        } else if (name == fLtSymbol) {
            handleCharacter('<', fLtSymbol, content);
            fScannerState = SCANNER_STATE_BUILT_IN_REFS;
            return ;
        } else if (name == fGtSymbol) {
            handleCharacter('>', fGtSymbol, content);
            fScannerState = SCANNER_STATE_BUILT_IN_REFS;
            return ;
        } else if (name == fQuotSymbol) {
            handleCharacter('"', fQuotSymbol, content);
            fScannerState = SCANNER_STATE_BUILT_IN_REFS;
            return ;
        } else if (name == fAposSymbol) {
            handleCharacter('\'', fAposSymbol, content);
            fScannerState = SCANNER_STATE_BUILT_IN_REFS;
            return ;
        }

        boolean isEE = fEntityStore.isExternalEntity(name);
        if((isEE && !fSupportExternalEntities) || (!isEE && !fReplaceEntityReferences) || foundBuiltInRefs){
            fScannerState = SCANNER_STATE_REFERENCE;
            return ;
        }
        if (!fEntityStore.isDeclaredEntity(name)) {
            if (!fSupportDTD && fReplaceEntityReferences) {
                reportFatalError("EntityNotDeclared", new Object[]{name});
                return;
            }
            if ( fHasExternalDTD && !fStandalone) {
                if (fValidation)
                    fErrorReporter.reportError(fEntityScanner, XMLMessageFormatter.XML_DOMAIN,"EntityNotDeclared",
                            new Object[]{name}, XMLErrorReporter.SEVERITY_ERROR);
            } else
                reportFatalError("EntityNotDeclared", new Object[]{name});
        }

        if (fCreateEntityRefNodes) {
            fDocumentHandler.startGeneralEntity(name, null, null, null);
        } else {
            fEntityManager.startEntity(true, name, false);
        }
    } 


    /**
     * Check if the depth exceeds the maxElementDepth limit
     * @param elementName name of the current element
     */
    void checkDepth(String elementName) {
        fLimitAnalyzer.addValue(Limit.MAX_ELEMENT_DEPTH_LIMIT, elementName, fElementStack.fDepth);
        if (fSecurityManager.isOverLimit(Limit.MAX_ELEMENT_DEPTH_LIMIT,fLimitAnalyzer)) {
            fSecurityManager.debugPrint(fLimitAnalyzer);
            reportFatalError("MaxElementDepthLimit", new Object[]{elementName,
                fLimitAnalyzer.getTotalValue(Limit.MAX_ELEMENT_DEPTH_LIMIT),
                fSecurityManager.getLimit(Limit.MAX_ELEMENT_DEPTH_LIMIT),
                "maxElementDepth"});
        }
    }

    /**
     * Calls document handler with a single character resulting from
     * built-in entity resolution.
     *
     * @param c
     * @param entity built-in name
     * @param XMLStringBuffer append the character to buffer
     *
     * we really dont need to call this function -- this function is only required when
     * we integrate with rest of Xerces2. SO maintaining the current behavior and still
     * calling this function to hanlde built-in entity reference.
     *
     */
    private void handleCharacter(char c, String entity, XMLStringBuffer content) throws XNIException {
        foundBuiltInRefs = true;
        checkEntityLimit(false, fEntityScanner.fCurrentEntity.name, 1);
        content.append(c);
        if (fDocumentHandler != null) {
            fSingleChar[0] = c;
            if (fNotifyBuiltInRefs) {
                fDocumentHandler.startGeneralEntity(entity, null, null, null);
            }
            fTempString.setValues(fSingleChar, 0, 1);
            if(!fIsCoalesce){
                fDocumentHandler.characters(fTempString, null);
                builtInRefCharacterHandled = true;
            }

            if (fNotifyBuiltInRefs) {
                fDocumentHandler.endGeneralEntity(entity, null);
            }
        }
    } 


    /**
     * Sets the scanner state.
     *
     * @param state The new scanner state.
     */
    protected final void setScannerState(int state) {

        fScannerState = state;
        if (DEBUG_SCANNER_STATE) {
            System.out.print("### setScannerState: ");
            System.out.print(getScannerStateName(state));
            System.out.println();
        }

    } 


    /**
     * Sets the Driver.
     *
     * @param Driver The new Driver.
     */
    protected final void setDriver(Driver driver) {
        fDriver = driver;
        if (DEBUG_DISPATCHER) {
            System.out.print("%%% setDriver: ");
            System.out.print(getDriverName(driver));
            System.out.println();
        }
    }


    /** Returns the scanner state name. */
    protected String getScannerStateName(int state) {

        switch (state) {
            case SCANNER_STATE_DOCTYPE: return "SCANNER_STATE_DOCTYPE";
            case SCANNER_STATE_ROOT_ELEMENT: return "SCANNER_STATE_ROOT_ELEMENT";
            case SCANNER_STATE_START_OF_MARKUP: return "SCANNER_STATE_START_OF_MARKUP";
            case SCANNER_STATE_COMMENT: return "SCANNER_STATE_COMMENT";
            case SCANNER_STATE_PI: return "SCANNER_STATE_PI";
            case SCANNER_STATE_CONTENT: return "SCANNER_STATE_CONTENT";
            case SCANNER_STATE_REFERENCE: return "SCANNER_STATE_REFERENCE";
            case SCANNER_STATE_END_OF_INPUT: return "SCANNER_STATE_END_OF_INPUT";
            case SCANNER_STATE_TERMINATED: return "SCANNER_STATE_TERMINATED";
            case SCANNER_STATE_CDATA: return "SCANNER_STATE_CDATA";
            case SCANNER_STATE_TEXT_DECL: return "SCANNER_STATE_TEXT_DECL";
            case SCANNER_STATE_ATTRIBUTE: return "SCANNER_STATE_ATTRIBUTE";
            case SCANNER_STATE_ATTRIBUTE_VALUE: return "SCANNER_STATE_ATTRIBUTE_VALUE";
            case SCANNER_STATE_START_ELEMENT_TAG: return "SCANNER_STATE_START_ELEMENT_TAG";
            case SCANNER_STATE_END_ELEMENT_TAG: return "SCANNER_STATE_END_ELEMENT_TAG";
            case SCANNER_STATE_CHARACTER_DATA: return "SCANNER_STATE_CHARACTER_DATA" ;
        }

        return "??? ("+state+')';

    } 
    public String getEntityName(){
        return fCurrentEntityName;
    }

    /** Returns the driver name. */
    public String getDriverName(Driver driver) {

        if (DEBUG_DISPATCHER) {
            if (driver != null) {
                String name = driver.getClass().getName();
                int index = name.lastIndexOf('.');
                if (index != -1) {
                    name = name.substring(index + 1);
                    index = name.lastIndexOf('$');
                    if (index != -1) {
                        name = name.substring(index + 1);
                    }
                }
                return name;
            }
        }
        return "null";

    } 

    /**
     * Check the protocol used in the systemId against allowed protocols
     *
     * @param systemId the Id of the URI
     * @param allowedProtocols a list of allowed protocols separated by comma
     * @return the name of the protocol if rejected, null otherwise
     */
    String checkAccess(String systemId, String allowedProtocols) throws IOException {
        String baseSystemId = fEntityScanner.getBaseSystemId();
        String expandedSystemId = XMLEntityManager.expandSystemId(systemId, baseSystemId, fStrictURI);
        return SecuritySupport.checkAccess(expandedSystemId, allowedProtocols, JdkConstants.ACCESS_EXTERNAL_ALL);
    }


    /**
     * @author Neeraj Bajaj, Sun Microsystems.
     */
    protected static final class Element {


        /** Symbol. */
        public QName qname;

        public char[] fRawname;

        /** The next Element entry. */
        public Element next;


        /**
         * Constructs a new Element from the given QName and next Element
         * reference.
         */
        public Element(QName qname, Element next) {
            this.qname.setValues(qname);
            this.fRawname = qname.rawname.toCharArray();
            this.next = next;
        }

    } 

    /**
     * Element stack.
     *
     * @author Neeraj Bajaj, Sun Microsystems.
     */
    protected class ElementStack2 {


        /** The stack data. */
        protected QName [] fQName = new QName[20];

        protected int fDepth;
        protected int fCount;
        protected int fPosition;
        protected int fMark;

        protected int fLastDepth ;


        /** Default constructor. */
        public ElementStack2() {
            for (int i = 0; i < fQName.length; i++) {
                fQName[i] = new QName();
            }
            fMark = fPosition = 1;
        } 

        public void resize(){
            /**
             * int length = fElements.length;
             * Element [] temp = new Element[length * 2];
             * System.arraycopy(fElements, 0, temp, 0, length);
             * fElements = temp;
             */
            int oldLength = fQName.length;
            QName [] tmp = new QName[oldLength * 2];
            System.arraycopy(fQName, 0, tmp, 0, oldLength);
            fQName = tmp;

            for (int i = oldLength; i < fQName.length; i++) {
                fQName[i] = new QName();
            }

        }



        /** Check if the element scanned during the start element
         *matches the stored element.
         *
         *@return true if the match suceeds.
         */
        public boolean matchElement(QName element) {
            if(DEBUG_SKIP_ALGORITHM){
                System.out.println("fLastDepth = " + fLastDepth);
                System.out.println("fDepth = " + fDepth);
            }
            boolean match = false;
            if(fLastDepth > fDepth && fDepth <= 2){
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("Checking if the elements match " + element.rawname + " , " + fQName[fDepth].rawname);
                }
                if(element.rawname == fQName[fDepth].rawname){
                    fAdd = false;
                    fMark = fDepth - 1;
                    fPosition = fMark + 1 ;
                    match = true;
                    --fCount;
                    if(DEBUG_SKIP_ALGORITHM){
                        System.out.println("fAdd FALSE -- NOW ELEMENT SHOULD NOT BE ADDED");
                        System.out.println("fMark = " + fMark);
                        System.out.println("fPosition = " + fPosition);
                        System.out.println("fDepth = " + fDepth);
                        System.out.println("fCount = " + fCount);
                    }
                }else{
                    fAdd = true;
                    if(DEBUG_SKIP_ALGORITHM)System.out.println("fAdd is " + fAdd);
                }
            }
            fLastDepth = fDepth++;
            return match;
        } 

        /**
         * This function doesn't increase depth. The function in this function is
         *broken down into two functions for efficiency. <@see>matchElement</see>.
         * This function just returns the pointer to the object and its values are set.
         *
         *@return QName reference to the next element in the list
         */
        public QName nextElement() {

            if (fCount == fQName.length) {
                fShouldSkip = false;
                fAdd = false;
                if(DEBUG_SKIP_ALGORITHM)System.out.println("SKIPPING STOPPED, fShouldSkip = " + fShouldSkip);
                return fQName[--fCount];
            }
            if(DEBUG_SKIP_ALGORITHM){
                System.out.println("fCount = " + fCount);
            }
            return fQName[fCount++];

        }

        /** Note that this function is considerably different than nextElement()
         * This function just returns the previously stored elements
         */
        public QName getNext(){
            if(fPosition == fCount){
                fPosition = fMark;
            }
            return fQName[fPosition++];
        }

        /** returns the current depth
         */
        public int popElement(){
            return fDepth--;
        }


        /** Clears the stack without throwing away existing QName objects. */
        public void clear() {
            fLastDepth = 0;
            fDepth = 0;
            fCount = 0 ;
            fPosition = fMark = 1;
        } 

    } 

    /**
     * Element stack. This stack operates without synchronization, error
     * checking, and it re-uses objects instead of throwing popped items
     * away.
     *
     * @author Andy Clark, IBM
     */
    protected class ElementStack {


        /** The stack data. */
        protected QName[] fElements;
        protected int []  fInt = new int[20];


        protected int fDepth;
        protected int fCount;
        protected int fPosition;
        protected int fMark;

        protected int fLastDepth ;


        /** Default constructor. */
        public ElementStack() {
            fElements = new QName[20];
            for (int i = 0; i < fElements.length; i++) {
                fElements[i] = new QName();
            }
        } 


        /**
         * Pushes an element on the stack.
         * <p>
         * <strong>Note:</strong> The QName values are copied into the
         * stack. In other words, the caller does <em>not</em> orphan
         * the element to the stack. Also, the QName object returned
         * is <em>not</em> orphaned to the caller. It should be
         * considered read-only.
         *
         * @param element The element to push onto the stack.
         *
         * @return Returns the actual QName object that stores the
         */
        public QName pushElement(QName element) {
            if (fDepth == fElements.length) {
                QName[] array = new QName[fElements.length * 2];
                System.arraycopy(fElements, 0, array, 0, fDepth);
                fElements = array;
                for (int i = fDepth; i < fElements.length; i++) {
                    fElements[i] = new QName();
                }
            }
            fElements[fDepth].setValues(element);
            return fElements[fDepth++];
        } 


        /** Note that this function is considerably different than nextElement()
         * This function just returns the previously stored elements
         */
        public QName getNext(){
            if(fPosition == fCount){
                fPosition = fMark;
            }
            if(DEBUG_SKIP_ALGORITHM){
                System.out.println("Element at fPosition = " + fPosition + " is " + fElements[fPosition].rawname);
            }
            return fElements[fPosition];
        }

        /** This function should be called only when element was skipped sucessfully.
         * 1. Increase the depth - because element was sucessfully skipped.
         *2. Store the position of the element token in array  "last opened tag" at depth.
         *3. increase the position counter so as to point to the next element in the array
         */
        public void push(){

            fInt[++fDepth] = fPosition++;
        }

        /** Check if the element scanned during the start element
         *matches the stored element.
         *
         *@return true if the match suceeds.
         */
        public boolean matchElement(QName element) {
            boolean match = false;
            if(fLastDepth > fDepth && fDepth <= 3){
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("----------ENTERED THE LOOP WHERE WE CHECK FOR MATCHING OF ELMENT-----");
                    System.out.println("Depth = " + fDepth + " Checking if INCOMING element " + element.rawname + " match STORED ELEMENT " + fElements[fDepth - 1].rawname);
                }
                if(element.rawname == fElements[fDepth - 1].rawname){
                    fAdd = false;
                    fMark = fDepth - 1;
                    fPosition = fMark;
                    match = true;
                    --fCount;
                    if(DEBUG_SKIP_ALGORITHM){
                        System.out.println("NOW ELEMENT SHOULD NOT BE ADDED, fAdd is set to false");
                        System.out.println("fMark = " + fMark);
                        System.out.println("fPosition = " + fPosition);
                        System.out.println("fDepth = " + fDepth);
                        System.out.println("fCount = " + fCount);
                        System.out.println("---------MATCH SUCEEDED-----------------");
                        System.out.println("");
                    }
                }else{
                    fAdd = true;
                    if(DEBUG_SKIP_ALGORITHM)System.out.println("fAdd is " + fAdd);
                }
            }
            if(match){
                fInt[fDepth] = fPosition++;
            } else{
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("At depth = " + fDepth + "array position is = " + (fCount - 1));
                }
                fInt[fDepth] = fCount - 1;
            }

            if (fCount == fElements.length) {
                fSkip = false;
                fAdd = false;
                reposition();
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("ALL THE ELMENTS IN ARRAY HAVE BEEN FILLED");
                    System.out.println("REPOSITIONING THE STACK");
                    System.out.println("-----------SKIPPING STOPPED----------");
                    System.out.println("");
                }
                return false;
            }
            if(DEBUG_SKIP_ALGORITHM){
                if(match){
                    System.out.println("Storing fPosition = " + fInt[fDepth] + " at fDepth = " + fDepth);
                }else{
                    System.out.println("Storing fCount = " + fInt[fDepth] + " at fDepth = " + fDepth);
                }
            }
            fLastDepth = fDepth;
            return match;
        } 


        /**
         * Returns the next element on the stack.
         *
         * @return Returns the actual QName object. Callee should
         * use this object to store the details of next element encountered.
         */
        public QName nextElement() {
            if(fSkip){
                fDepth++;
                return fElements[fCount++];
            } else if (fDepth == fElements.length) {
                QName[] array = new QName[fElements.length * 2];
                System.arraycopy(fElements, 0, array, 0, fDepth);
                fElements = array;
                for (int i = fDepth; i < fElements.length; i++) {
                    fElements[i] = new QName();
                }
            }

            return fElements[fDepth++];

        } 


        /**
         * Pops an element off of the stack by setting the values of
         * the specified QName.
         * <p>
         * <strong>Note:</strong> The object returned is <em>not</em>
         * orphaned to the caller. Therefore, the caller should consider
         * the object to be read-only.
         */
        public QName popElement() {
            if(fSkip || fAdd ){
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("POPPING Element, at position " + fInt[fDepth] + " element at that count is = " + fElements[fInt[fDepth]].rawname);
                    System.out.println("");
                }
                return fElements[fInt[fDepth--]];
            } else{
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("Retrieveing element at depth = " + fDepth + " is " + fElements[fDepth].rawname );
                }
                return fElements[--fDepth] ;
            }
        } 

        /** Reposition the stack. fInt [] contains all the opened tags at particular depth.
         * Transfer all the opened tags starting from depth '2' to the current depth and reposition them
         *as per the depth.
         */
        public void reposition(){
            for( int i = 2 ; i <= fDepth ; i++){
                fElements[i-1] = fElements[fInt[i]];
            }
            if(DEBUG_SKIP_ALGORITHM){
                for( int i = 0 ; i < fDepth ; i++){
                    System.out.println("fElements[" + i + "]" + " = " + fElements[i].rawname);
                }
            }
        }

        /** Clears the stack without throwing away existing QName objects. */
        public void clear() {
            fDepth = 0;
            fLastDepth = 0;
            fCount = 0 ;
            fPosition = fMark = 1;

        } 

        /**
         * This function is as a result of optimization done for endElement --
         * we dont need to set the value for every end element encouterd.
         * For Well formedness checks we can have the same QName object that was pushed.
         * the values will be set only if application need to know about the endElement
         */

        public QName getLastPoppedElement(){
            return fElements[fDepth];
        }
    } 

    /**
     * Drives the parser to the next state/event on the input. Parser is guaranteed
     * to stop at the next state/event.
     *
     * Internally XML document is divided into several states. Each state represents
     * a sections of XML document. When this functions returns normally, it has read
     * the section of XML document and returns the state corresponding to section of
     * document which has been read. For optimizations, a particular driver
     * can read ahead of the section of document (state returned) just read and
     * can maintain a different internal state.
     *
     *
     * @author Neeraj Bajaj, Sun Microsystems
     */
    protected interface Driver {


        /**
         * Drives the parser to the next state/event on the input. Parser is guaranteed
         * to stop at the next state/event.
         *
         * Internally XML document is divided into several states. Each state represents
         * a sections of XML document. When this functions returns normally, it has read
         * the section of XML document and returns the state corresponding to section of
         * document which has been read. For optimizations, a particular driver
         * can read ahead of the section of document (state returned) just read and
         * can maintain a different internal state.
         *
         * @return state representing the section of document just read.
         *
         * @throws IOException  Thrown on i/o error.
         * @throws XNIException Thrown on parse error.
         */

        public int next() throws IOException, XNIException;

    } 

    /**
     * Driver to handle content scanning. This driver is capable of reading
     * the fragment of XML document. When it has finished reading fragment
     * of XML documents, it can pass the job of reading to another driver.
     *
     * This class has been modified as per the new design which is more suited to
     * efficiently build pull parser. Lot of performance improvements have been done and
     * the code has been added to support stax functionality/features.
     *
     * @author Neeraj Bajaj, Sun Microsystems
     *
     *
     * @author Andy Clark, IBM
     * @author Eric Ye, IBM
     */
    protected class FragmentContentDriver
            implements Driver {


        /**
         *  decides the appropriate state of the parser
         */
        private void startOfMarkup() throws IOException {
            fMarkupDepth++;
            final int ch = fEntityScanner.peekChar();
            if (isValidNameStartChar(ch) || isValidNameStartHighSurrogate(ch)) {
                setScannerState(SCANNER_STATE_START_ELEMENT_TAG);
            } else {
                switch(ch){
                    case '?' :{
                        setScannerState(SCANNER_STATE_PI);
                        fEntityScanner.skipChar(ch, null);
                        break;
                    }
                    case '!' :{
                        fEntityScanner.skipChar(ch, null);
                        if (fEntityScanner.skipChar('-', null)) {
                            if (!fEntityScanner.skipChar('-', NameType.COMMENT)) {
                                reportFatalError("InvalidCommentStart",
                                        null);
                            }
                            setScannerState(SCANNER_STATE_COMMENT);
                        } else if (fEntityScanner.skipString(CDATA)) {
                            fCDataStart = true;
                            setScannerState(SCANNER_STATE_CDATA );
                        } else if (!scanForDoctypeHook()) {
                            reportFatalError("MarkupNotRecognizedInContent",
                                    null);
                        }
                        break;
                    }
                    case '/' :{
                        setScannerState(SCANNER_STATE_END_ELEMENT_TAG);
                        fEntityScanner.skipChar(ch, NameType.ELEMENTEND);
                        break;
                    }
                    default :{
                        reportFatalError("MarkupNotRecognizedInContent", null);
                    }
                }
            }

        }

        private void startOfContent() throws IOException {
            if (fEntityScanner.skipChar('<', null)) {
                setScannerState(SCANNER_STATE_START_OF_MARKUP);
            } else if (fEntityScanner.skipChar('&', NameType.REFERENCE)) {
                setScannerState(SCANNER_STATE_REFERENCE) ; 
            } else {
                setScannerState(SCANNER_STATE_CHARACTER_DATA);
            }
        }


        /**
         *
         * SCANNER_STATE_CONTENT and SCANNER_STATE_START_OF_MARKUP are two super states of the parser.
         * At any point of time when in doubt over the current state of the parser, the state should be
         * set to SCANNER_STATE_CONTENT. Parser will automatically revive itself and will set state of
         * the parser to one of its sub state.
         * sub states are defined in the parser on the basis of different XML component like
         * SCANNER_STATE_ENTITY_REFERENCE , SCANNER_STATE_START_ELEMENT, SCANNER_STATE_CDATA etc..
         * These sub states help the parser to have fine control over the parsing. These are the
         * different milepost, parser stops at each sub state (milepost). Based on this state it is
         * decided if paresr needs to stop at next milepost ??
         *
         */
        public void decideSubState() throws IOException {
            while( fScannerState == SCANNER_STATE_CONTENT || fScannerState == SCANNER_STATE_START_OF_MARKUP){

                switch (fScannerState) {

                    case SCANNER_STATE_CONTENT: {
                        startOfContent() ;
                        break;
                    }

                    case SCANNER_STATE_START_OF_MARKUP: {
                        startOfMarkup() ;
                        break;
                    }
                }
            }
        }

        /**
         * Drives the parser to the next state/event on the input. Parser is guaranteed
         * to stop at the next state/event. Internally XML document
         * is divided into several states. Each state represents a sections of XML
         * document. When this functions returns normally, it has read the section
         * of XML document and returns the state corresponding to section of
         * document which has been read. For optimizations, a particular driver
         * can read ahead of the section of document (state returned) just read and
         * can maintain a different internal state.
         *
         * State returned corresponds to Stax states.
         *
         * @return state representing the section of document just read.
         *
         * @throws IOException  Thrown on i/o error.
         * @throws XNIException Thrown on parse error.
         */

        public int next() throws IOException, XNIException {
            while (true) {
            try {


                if (fScannerState == SCANNER_STATE_CONTENT) {
                    final int ch = fEntityScanner.peekChar();
                    if (ch == '<') {
                        fEntityScanner.scanChar(null);
                        setScannerState(SCANNER_STATE_START_OF_MARKUP);
                    } else if (ch == '&') {
                        fEntityScanner.scanChar(NameType.REFERENCE);
                        setScannerState(SCANNER_STATE_REFERENCE) ;
                    } else {
                        setScannerState(SCANNER_STATE_CHARACTER_DATA);
                    }
                }

                if (fScannerState == SCANNER_STATE_START_OF_MARKUP) {
                    startOfMarkup();
                }


                if (fIsCoalesce) {
                    fUsebuffer = true ;
                    if (fLastSectionWasCharacterData) {

                        if ((fScannerState != SCANNER_STATE_CDATA)
                                && (fScannerState != SCANNER_STATE_REFERENCE)
                                && (fScannerState != SCANNER_STATE_CHARACTER_DATA)) {
                            fLastSectionWasCharacterData = false;
                            return XMLEvent.CHARACTERS;
                        }
                    }
                    else if ((fLastSectionWasCData || fLastSectionWasEntityReference)) {
                        if ((fScannerState != SCANNER_STATE_CDATA)
                                && (fScannerState != SCANNER_STATE_REFERENCE)
                                && (fScannerState != SCANNER_STATE_CHARACTER_DATA)){

                            fLastSectionWasCData = false;
                            fLastSectionWasEntityReference = false;
                            return XMLEvent.CHARACTERS;
                        }
                    }
                }

                switch(fScannerState){

                    case XMLEvent.START_DOCUMENT :
                        return XMLEvent.START_DOCUMENT;

                    case SCANNER_STATE_START_ELEMENT_TAG :{

                        fEmptyElement = scanStartElement() ;
                        if(fEmptyElement){
                            setScannerState(SCANNER_STATE_END_ELEMENT_TAG);
                        }else{
                            setScannerState(SCANNER_STATE_CONTENT);
                        }
                        return XMLEvent.START_ELEMENT ;
                    }

                    case SCANNER_STATE_CHARACTER_DATA: {

                        fUsebuffer = fLastSectionWasEntityReference || fLastSectionWasCData
                                || fLastSectionWasCharacterData ;

                        if( fIsCoalesce && (fLastSectionWasEntityReference ||
                                fLastSectionWasCData || fLastSectionWasCharacterData) ){
                            fLastSectionWasEntityReference = false;
                            fLastSectionWasCData = false;
                            fLastSectionWasCharacterData = true ;
                            fUsebuffer = true;
                        }else{
                            fContentBuffer.clear();
                        }

                        fTempString.length = 0;
                        int c = fEntityScanner.scanContent(fTempString);

                        if(fEntityScanner.skipChar('<', null)){
                            if(fEntityScanner.skipChar('/', NameType.ELEMENTEND)){
                                fMarkupDepth++;
                                fLastSectionWasCharacterData = false;
                                setScannerState(SCANNER_STATE_END_ELEMENT_TAG);
                            }else if(XMLChar.isNameStart(fEntityScanner.peekChar())){
                                fMarkupDepth++;
                                fLastSectionWasCharacterData = false;
                                setScannerState(SCANNER_STATE_START_ELEMENT_TAG);
                            }else{
                                setScannerState(SCANNER_STATE_START_OF_MARKUP);
                                if(fIsCoalesce){
                                    fLastSectionWasCharacterData = true;
                                    bufferContent();
                                    continue;
                                }
                            }
                            if(fUsebuffer){
                                bufferContent();
                            }

                            if(dtdGrammarUtil!= null && dtdGrammarUtil.isIgnorableWhiteSpace(fContentBuffer)){
                                if(DEBUG)System.out.println("Return SPACE EVENT");
                                return XMLEvent.SPACE;
                            }else
                                return XMLEvent.CHARACTERS;

                        } else{
                            bufferContent();
                        }
                        if (c == '\r') {
                            if(DEBUG){
                                System.out.println("'\r' character found");
                            }
                            fEntityScanner.scanChar(null);
                            fUsebuffer = true;
                            fContentBuffer.append((char)c);
                            c = -1 ;
                        } else if (c == ']') {
                            fUsebuffer = true;
                            fContentBuffer.append((char)fEntityScanner.scanChar(null));
                            fInScanContent = true;

                            if (fEntityScanner.skipChar(']', null)) {
                                fContentBuffer.append(']');
                                while (fEntityScanner.skipChar(']', null)) {
                                    fContentBuffer.append(']');
                                }
                                if (fEntityScanner.skipChar('>', null)) {
                                    reportFatalError("CDEndInContent", null);
                                }
                            }
                            c = -1 ;
                            fInScanContent = false;
                        }

                        do{

                            if (c == '<') {
                                fEntityScanner.scanChar(null);
                                setScannerState(SCANNER_STATE_START_OF_MARKUP);
                                break;
                            }
                            else if (c == '&') {
                                fEntityScanner.scanChar(NameType.REFERENCE);
                                setScannerState(SCANNER_STATE_REFERENCE);
                                break;
                            }
                            else if (c != -1 && isInvalidLiteral(c)) {
                                if (XMLChar.isHighSurrogate(c)) {
                                    scanSurrogates(fContentBuffer) ;
                                    setScannerState(SCANNER_STATE_CONTENT);
                                } else {
                                    reportFatalError("InvalidCharInContent",
                                            new Object[] {
                                        Integer.toString(c, 16)});
                                        fEntityScanner.scanChar(null);
                                }
                                break;
                            }
                            c = scanContent(fContentBuffer) ;

                            if(!fIsCoalesce){
                                setScannerState(SCANNER_STATE_CONTENT);
                                break;
                            }

                        }while(true);

                        if(DEBUG)System.out.println("USING THE BUFFER, STRING START=" + fContentBuffer.toString() +"=END");
                        if(fIsCoalesce){
                            fLastSectionWasCharacterData = true ;
                            continue;
                        }else{
                            if(dtdGrammarUtil!= null && dtdGrammarUtil.isIgnorableWhiteSpace(fContentBuffer)){
                                if(DEBUG)System.out.println("Return SPACE EVENT");
                                return XMLEvent.SPACE;
                            } else
                                return XMLEvent.CHARACTERS ;
                        }
                    }

                    case SCANNER_STATE_END_ELEMENT_TAG :{
                        if(fEmptyElement){
                            fEmptyElement = false;
                            setScannerState(SCANNER_STATE_CONTENT);
                            return (fMarkupDepth == 0 && elementDepthIsZeroHook() ) ?
                                    XMLEvent.END_ELEMENT : XMLEvent.END_ELEMENT ;

                        } else if(scanEndElement() == 0) {
                            if (elementDepthIsZeroHook()) {
                                return XMLEvent.END_ELEMENT ;
                            }

                        }
                        setScannerState(SCANNER_STATE_CONTENT);
                        return XMLEvent.END_ELEMENT ;
                    }

                    case SCANNER_STATE_COMMENT: { 
                        scanComment();
                        setScannerState(SCANNER_STATE_CONTENT);
                        return XMLEvent.COMMENT;
                    }
                    case SCANNER_STATE_PI:{ 
                        fContentBuffer.clear() ;
                        scanPI(fContentBuffer);
                        setScannerState(SCANNER_STATE_CONTENT);
                        return XMLEvent.PROCESSING_INSTRUCTION;
                    }
                    case SCANNER_STATE_CDATA :{ 

                        if(fIsCoalesce && ( fLastSectionWasEntityReference ||
                                fLastSectionWasCData || fLastSectionWasCharacterData)){
                            fLastSectionWasCData = true ;
                            fLastSectionWasEntityReference = false;
                            fLastSectionWasCharacterData = false;
                        }
                        else{
                            fContentBuffer.clear();
                        }
                        fUsebuffer = true;
                        scanCDATASection(fContentBuffer , true);
                        if (!fCDataEnd) {
                            setScannerState(SCANNER_STATE_CDATA);
                        } else {
                            setScannerState(SCANNER_STATE_CONTENT);
                        }
                        if (fIsCoalesce) {
                            fLastSectionWasCData = true ;
                            continue;
                        } else if(fReportCdataEvent) {
                            return XMLEvent.CDATA;
                        } else {
                            return XMLEvent.CHARACTERS;
                        }
                    }

                    case SCANNER_STATE_REFERENCE :{
                        fMarkupDepth++;
                        foundBuiltInRefs = false;

                        if(fIsCoalesce && ( fLastSectionWasEntityReference ||
                                fLastSectionWasCData || fLastSectionWasCharacterData)){
                            fLastSectionWasEntityReference = true ;
                            fLastSectionWasCData = false;
                            fLastSectionWasCharacterData = false;
                        }
                        else{
                            fContentBuffer.clear();
                        }
                        fUsebuffer = true ;
                        if (fEntityScanner.skipChar('#', NameType.REFERENCE)) {
                            scanCharReferenceValue(fContentBuffer, null);
                            fMarkupDepth--;
                            if(!fIsCoalesce){
                                setScannerState(SCANNER_STATE_CONTENT);
                                return XMLEvent.CHARACTERS;
                            }
                        } else {
                            scanEntityReference(fContentBuffer);
                            if(fScannerState == SCANNER_STATE_BUILT_IN_REFS && !fIsCoalesce){
                                setScannerState(SCANNER_STATE_CONTENT);
                                if (builtInRefCharacterHandled) {
                                    builtInRefCharacterHandled = false;
                                    return XMLEvent.ENTITY_REFERENCE;
                                } else {
                                    return XMLEvent.CHARACTERS;
                                }
                            }

                            if(fScannerState == SCANNER_STATE_TEXT_DECL){
                                fLastSectionWasEntityReference = true ;
                                continue;
                            }

                            if(fScannerState == SCANNER_STATE_REFERENCE){
                                setScannerState(SCANNER_STATE_CONTENT);
                                if (fReplaceEntityReferences &&
                                        fEntityStore.isDeclaredEntity(fCurrentEntityName)) {
                                    continue;
                                }
                                return XMLEvent.ENTITY_REFERENCE;
                            }
                        }
                        setScannerState(SCANNER_STATE_CONTENT);
                        fLastSectionWasEntityReference = true ;
                        continue;
                    }

                    case SCANNER_STATE_TEXT_DECL: {
                        if (fEntityScanner.skipString("<?xml")) {
                            fMarkupDepth++;
                            if (isValidNameChar(fEntityScanner.peekChar())) {
                                fStringBuffer.clear();
                                fStringBuffer.append("xml");

                                if (fNamespaces) {
                                    while (isValidNCName(fEntityScanner.peekChar())) {
                                        fStringBuffer.append((char)fEntityScanner.scanChar(null));
                                    }
                                } else {
                                    while (isValidNameChar(fEntityScanner.peekChar())) {
                                        fStringBuffer.append((char)fEntityScanner.scanChar(null));
                                    }
                                }
                                String target = fSymbolTable.addSymbol(fStringBuffer.ch,
                                        fStringBuffer.offset, fStringBuffer.length);
                                fContentBuffer.clear();
                                scanPIData(target, fContentBuffer);
                            }

                            else {
                                scanXMLDeclOrTextDecl(true);
                            }
                        }
                        fEntityManager.fCurrentEntity.mayReadChunks = true;
                        setScannerState(SCANNER_STATE_CONTENT);
                        continue;
                    }


                    case SCANNER_STATE_ROOT_ELEMENT: {
                        if (scanRootElementHook()) {
                            fEmptyElement = true;
                            return XMLEvent.START_ELEMENT;
                        }
                        setScannerState(SCANNER_STATE_CONTENT);
                        return XMLEvent.START_ELEMENT ;
                    }
                    case SCANNER_STATE_CHAR_REFERENCE : {
                        fContentBuffer.clear();
                        scanCharReferenceValue(fContentBuffer, null);
                        fMarkupDepth--;
                        setScannerState(SCANNER_STATE_CONTENT);
                        return XMLEvent.CHARACTERS;
                    }
                    default:
                        throw new XNIException("Scanner State " + fScannerState + " not Recognized ");

                }
            }
             catch (MalformedByteSequenceException e) {
                 fErrorReporter.reportError(e.getDomain(), e.getKey(),
                    e.getArguments(), XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
                 return -1;
             }
             catch (CharConversionException e) {
                fErrorReporter.reportError(
                        XMLMessageFormatter.XML_DOMAIN,
                        "CharConversionFailure",
                        null,
                        XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
                 return -1;
             }
            catch (EOFException e) {
                endOfFileHook(e);
                return -1;
            }
            } 
        }




        /**
         * Scan for DOCTYPE hook. This method is a hook for subclasses
         * to add code to handle scanning for a the "DOCTYPE" string
         * after the string "<!" has been scanned.
         *
         * @return True if the "DOCTYPE" was scanned; false if "DOCTYPE"
         *          was not scanned.
         */
        protected boolean scanForDoctypeHook()
        throws IOException, XNIException {
            return false;
        } 

        /**
         * Element depth iz zero. This methos is a hook for subclasses
         * to add code to handle when the element depth hits zero. When
         * scanning a document fragment, an element depth of zero is
         * normal. However, when scanning a full XML document, the
         * scanner must handle the trailing miscellanous section of
         * the document after the end of the document's root element.
         *
         * @return True if the caller should stop and return true which
         *          allows the scanner to switch to a new scanning
         *          driver. A return value of false indicates that
         *          the content driver should continue as normal.
         */
        protected boolean elementDepthIsZeroHook()
        throws IOException, XNIException {
            return false;
        } 

        /**
         * Scan for root element hook. This method is a hook for
         * subclasses to add code that handles scanning for the root
         * element. When scanning a document fragment, there is no
         * "root" element. However, when scanning a full XML document,
         * the scanner must handle the root element specially.
         *
         * @return True if the caller should stop and return true which
         *          allows the scanner to switch to a new scanning
         *          driver. A return value of false indicates that
         *          the content driver should continue as normal.
         */
        protected boolean scanRootElementHook()
        throws IOException, XNIException {
            return false;
        } 

        /**
         * End of file hook. This method is a hook for subclasses to
         * add code that handles the end of file. The end of file in
         * a document fragment is OK if the markup depth is zero.
         * However, when scanning a full XML document, an end of file
         * is always premature.
         */
        protected void endOfFileHook(EOFException e)
        throws IOException, XNIException {

            if (fMarkupDepth != 0) {
                reportFatalError("PrematureEOF", null);
            }

        } 

    } 

    static void pr(String str) {
        System.out.println(str) ;
    }

    protected boolean fUsebuffer ;

    /** this function gets an XMLString (which is used to store the attribute value) from the special pool
     *  maintained for attributes.
     *  fAttributeCacheUsedCount tracks the number of attributes that has been consumed from the pool.
     *  if all the attributes has been consumed, it adds a new XMLString inthe pool and returns the same
     *  XMLString.
     *
     * @return XMLString XMLString used to store an attribute value.
     */

    protected XMLString getString(){
        if(fAttributeCacheUsedCount < initialCacheCount ||
                fAttributeCacheUsedCount < attributeValueCache.size()){
            return attributeValueCache.get(fAttributeCacheUsedCount++);
        } else{
            XMLString str = new XMLString();
            fAttributeCacheUsedCount++;
            attributeValueCache.add(str);
            return str;
        }
    }

    /**
     * Implements XMLBufferListener interface.
     */

    public void refresh(){
        refresh(0);
    }

    /**
     * receives callbacks from {@link XMLEntityReader } when buffer
     * is being changed.
     * @param refreshPosition
     */
    public void refresh(int refreshPosition){
        if(fReadingAttributes){
            fAttributes.refresh();
        }
        if(fScannerState == SCANNER_STATE_CHARACTER_DATA){
            bufferContent();
        }
    }

    /**
     * Since 'TempString' shares the buffer (a char array) with the CurrentEntity,
     * when the cursor position reaches the end, that is, before the buffer is
     * being loaded with new data, the content in the TempString needs to be
     * copied into the ContentBuffer.
     */
    private void bufferContent() {
        fContentBuffer.append(fTempString);
        fTempString.length = 0;
        fUsebuffer = true;
    }
} 
