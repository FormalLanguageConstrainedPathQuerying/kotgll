/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl.xs;

import com.sun.org.apache.xerces.internal.dom.DOMErrorImpl;
import com.sun.org.apache.xerces.internal.dom.DOMMessageFormatter;
import com.sun.org.apache.xerces.internal.dom.DOMStringListImpl;
import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.SchemaDVFactory;
import com.sun.org.apache.xerces.internal.impl.dv.xs.SchemaDVFactoryImpl;
import com.sun.org.apache.xerces.internal.impl.xs.models.CMBuilder;
import com.sun.org.apache.xerces.internal.impl.xs.models.CMNodeFactory;
import com.sun.org.apache.xerces.internal.impl.xs.traversers.XSDHandler;
import com.sun.org.apache.xerces.internal.util.DOMEntityResolverWrapper;
import com.sun.org.apache.xerces.internal.util.DOMErrorHandlerWrapper;
import com.sun.org.apache.xerces.internal.util.DefaultErrorHandler;
import com.sun.org.apache.xerces.internal.util.MessageFormatter;
import com.sun.org.apache.xerces.internal.util.ParserConfigurationSettings;
import com.sun.org.apache.xerces.internal.util.Status;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.URI.MalformedURIException;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarLoader;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.grammars.XSGrammar;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.org.apache.xerces.internal.xs.LSInputList;
import com.sun.org.apache.xerces.internal.xs.StringList;
import com.sun.org.apache.xerces.internal.xs.XSLoader;
import com.sun.org.apache.xerces.internal.xs.XSModel;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import javax.xml.XMLConstants;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.XMLSecurityManager;
import jdk.xml.internal.JdkXmlUtils;
import jdk.xml.internal.SecuritySupport;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;

/**
 * This class implements xni.grammars.XMLGrammarLoader.
 * It also serves as implementation of xs.XSLoader interface and DOMConfiguration interface.
 *
 * This class is designed to interact either with a proxy for a user application
 * which wants to preparse schemas, or with our own Schema validator.
 * It is hoped that none of these "external" classes will therefore need to communicate directly
 * with XSDHandler in future.
 * <p>This class only knows how to make XSDHandler do its thing.
 * The caller must ensure that all its properties (schemaLocation, JAXPSchemaSource
 * etc.) have been properly set.
 *
 * @xerces.internal
 *
 * @author Neil Graham, IBM
 * @LastModified: July 2023
 */

public class XMLSchemaLoader implements XMLGrammarLoader, XMLComponent, XSElementDeclHelper,
XSLoader, DOMConfiguration {


    /** Feature identifier: schema full checking*/
    protected static final String SCHEMA_FULL_CHECKING =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING;

    /** Feature identifier: continue after fatal error. */
    protected static final String CONTINUE_AFTER_FATAL_ERROR =
        Constants.XERCES_FEATURE_PREFIX + Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE;

    /** Feature identifier: allow java encodings to be recognized when parsing schema docs. */
    protected static final String ALLOW_JAVA_ENCODINGS =
        Constants.XERCES_FEATURE_PREFIX + Constants.ALLOW_JAVA_ENCODINGS_FEATURE;

    /** Feature identifier: standard uri conformant feature. */
    protected static final String STANDARD_URI_CONFORMANT_FEATURE =
        Constants.XERCES_FEATURE_PREFIX + Constants.STANDARD_URI_CONFORMANT_FEATURE;

    /** Feature identifier: validate annotations. */
    protected static final String VALIDATE_ANNOTATIONS =
        Constants.XERCES_FEATURE_PREFIX + Constants.VALIDATE_ANNOTATIONS_FEATURE;

    /** Feature: disallow doctype*/
    protected static final String DISALLOW_DOCTYPE =
        Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE;

    /** Feature: generate synthetic annotations */
    protected static final String GENERATE_SYNTHETIC_ANNOTATIONS =
        Constants.XERCES_FEATURE_PREFIX + Constants.GENERATE_SYNTHETIC_ANNOTATIONS_FEATURE;

    /** Feature identifier: honour all schemaLocations */
    protected static final String HONOUR_ALL_SCHEMALOCATIONS =
        Constants.XERCES_FEATURE_PREFIX + Constants.HONOUR_ALL_SCHEMALOCATIONS_FEATURE;

    protected static final String AUGMENT_PSVI =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_AUGMENT_PSVI;

    protected static final String PARSER_SETTINGS =
        Constants.XERCES_FEATURE_PREFIX + Constants.PARSER_SETTINGS;

    /** Feature identifier: namespace growth */
    protected static final String NAMESPACE_GROWTH =
        Constants.XERCES_FEATURE_PREFIX + Constants.NAMESPACE_GROWTH_FEATURE;

    /** Feature identifier: tolerate duplicates */
    protected static final String TOLERATE_DUPLICATES =
        Constants.XERCES_FEATURE_PREFIX + Constants.TOLERATE_DUPLICATES_FEATURE;

    /** Property identifier: Schema DV Factory */
    protected static final String SCHEMA_DV_FACTORY =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SCHEMA_DV_FACTORY_PROPERTY;

    protected static final String OVERRIDE_PARSER = JdkConstants.OVERRIDE_PARSER;

    private static final String[] RECOGNIZED_FEATURES = {
        SCHEMA_FULL_CHECKING,
        AUGMENT_PSVI,
        CONTINUE_AFTER_FATAL_ERROR,
        ALLOW_JAVA_ENCODINGS,
        STANDARD_URI_CONFORMANT_FEATURE,
        DISALLOW_DOCTYPE,
        GENERATE_SYNTHETIC_ANNOTATIONS,
        VALIDATE_ANNOTATIONS,
        HONOUR_ALL_SCHEMALOCATIONS,
        NAMESPACE_GROWTH,
        TOLERATE_DUPLICATES,
        OVERRIDE_PARSER,
        XMLConstants.USE_CATALOG
    };


    /** Property identifier: symbol table. */
    public static final String SYMBOL_TABLE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property identifier: error reporter. */
    public static final String ERROR_REPORTER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Property identifier: error handler. */
    public static final String ERROR_HANDLER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_HANDLER_PROPERTY;

    /** Property identifier: entity resolver. */
    public static final String ENTITY_RESOLVER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

    /** Property identifier: grammar pool. */
    public static final String XMLGRAMMAR_POOL =
        Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

    /** Property identifier: schema location. */
    protected static final String SCHEMA_LOCATION =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SCHEMA_LOCATION;

    /** Property identifier: no namespace schema location. */
    protected static final String SCHEMA_NONS_LOCATION =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SCHEMA_NONS_LOCATION;

    /** Property identifier: JAXP schema source. */
    protected static final String JAXP_SCHEMA_SOURCE =
        Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_SOURCE;

    protected static final String SECURITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY;

    /** Property identifier: locale. */
    protected static final String LOCALE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.LOCALE_PROPERTY;

    protected static final String ENTITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_MANAGER_PROPERTY;

    /** Property identifier: Security property manager. */
    private static final String XML_SECURITY_PROPERTY_MANAGER =
            JdkConstants.XML_SECURITY_PROPERTY_MANAGER;

    /** Property identifier: access to external dtd */
    public static final String ACCESS_EXTERNAL_DTD = XMLConstants.ACCESS_EXTERNAL_DTD;

    /** Property identifier: access to external schema */
    public static final String ACCESS_EXTERNAL_SCHEMA = XMLConstants.ACCESS_EXTERNAL_SCHEMA;

    private static final String [] RECOGNIZED_PROPERTIES = {
        ENTITY_MANAGER,
        SYMBOL_TABLE,
        ERROR_REPORTER,
        ERROR_HANDLER,
        ENTITY_RESOLVER,
        XMLGRAMMAR_POOL,
        SCHEMA_LOCATION,
        SCHEMA_NONS_LOCATION,
        JAXP_SCHEMA_SOURCE,
        SECURITY_MANAGER,
        LOCALE,
        SCHEMA_DV_FACTORY,
        XML_SECURITY_PROPERTY_MANAGER,
        JdkXmlUtils.CATALOG_DEFER,
        JdkXmlUtils.CATALOG_FILES,
        JdkXmlUtils.CATALOG_PREFER,
        JdkXmlUtils.CATALOG_RESOLVE,
        JdkConstants.CDATA_CHUNK_SIZE
    };


    private final ParserConfigurationSettings fLoaderConfig = new ParserConfigurationSettings();
    private XMLErrorReporter fErrorReporter = new XMLErrorReporter ();
    private XMLEntityManager fEntityManager = null;
    private XMLEntityResolver fUserEntityResolver = null;
    private XMLGrammarPool fGrammarPool = null;
    private String fExternalSchemas = null;
    private String fExternalNoNSSchema = null;
    private Object fJAXPSource = null;
    private boolean fIsCheckedFully = false;
    private boolean fJAXPProcessed = false;
    private boolean fSettingsChanged = true;

    private XSDHandler fSchemaHandler;
    private XSGrammarBucket fGrammarBucket;
    private XSDeclarationPool fDeclPool = null;
    private SubstitutionGroupHandler fSubGroupHandler;
    private final CMNodeFactory fNodeFactory = new CMNodeFactory(); 
    private CMBuilder fCMBuilder;
    private XSDDescription fXSDDescription = new XSDDescription();
    private String faccessExternalSchema = JdkConstants.EXTERNAL_ACCESS_DEFAULT;

    private WeakHashMap<Object, SchemaGrammar> fJAXPCache;
    private Locale fLocale = Locale.getDefault();

    private DOMStringList fRecognizedParameters = null;

    /** DOM L3 error handler */
    private DOMErrorHandlerWrapper fErrorHandler = null;

    /** DOM L3 resource resolver */
    private DOMEntityResolverWrapper fResourceResolver = null;

    public XMLSchemaLoader() {
        this( new SymbolTable(), null, new XMLEntityManager(), null, null, null);
    }

    public XMLSchemaLoader(SymbolTable symbolTable) {
        this( symbolTable, null, new XMLEntityManager(), null, null, null);
    }

    /**
     * This constractor is used by the XMLSchemaValidator. Additional properties, i.e. XMLEntityManager,
     * will be passed during reset(XMLComponentManager).
     * @param errorReporter
     * @param grammarBucket
     * @param sHandler
     * @param builder
     */
    XMLSchemaLoader(XMLErrorReporter errorReporter, XSGrammarBucket grammarBucket,
            SubstitutionGroupHandler sHandler, CMBuilder builder) {
        this(null, errorReporter, null, grammarBucket, sHandler, builder);
    }

    XMLSchemaLoader(SymbolTable symbolTable, XMLErrorReporter errorReporter,
            XMLEntityManager entityResolver, XSGrammarBucket grammarBucket,
            SubstitutionGroupHandler sHandler, CMBuilder builder) {

        fLoaderConfig.addRecognizedFeatures(RECOGNIZED_FEATURES);
        fLoaderConfig.addRecognizedProperties(RECOGNIZED_PROPERTIES);
        if (symbolTable != null){
            fLoaderConfig.setProperty(SYMBOL_TABLE, symbolTable);
        }

        if(errorReporter == null) {
            errorReporter = new XMLErrorReporter ();
            errorReporter.setLocale(fLocale);
            errorReporter.setProperty(ERROR_HANDLER, new DefaultErrorHandler());

        }
        fErrorReporter = errorReporter;
        if(fErrorReporter.getMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN) == null) {
            fErrorReporter.putMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN, new XSMessageFormatter());
        }
        fLoaderConfig.setProperty(ERROR_REPORTER, fErrorReporter);
        fEntityManager = entityResolver;
        if (fEntityManager != null){
            fLoaderConfig.setProperty(ENTITY_MANAGER, fEntityManager);
        }

        fLoaderConfig.setFeature(AUGMENT_PSVI, true);

        if(grammarBucket == null ) {
            grammarBucket = new XSGrammarBucket();
        }
        fGrammarBucket = grammarBucket;
        if (sHandler == null) {
            sHandler = new SubstitutionGroupHandler(this);
        }
        fSubGroupHandler = sHandler;

        if(builder == null) {
            builder = new CMBuilder(fNodeFactory);
        }
        fCMBuilder = builder;
        fSchemaHandler = new XSDHandler(fGrammarBucket);
        fJAXPCache = new WeakHashMap<>();

        fSettingsChanged = true;
    }

    /**
     * Returns a list of feature identifiers that are recognized by
     * this XMLGrammarLoader.  This method may return null if no features
     * are recognized.
     */
    public String[] getRecognizedFeatures() {
        return RECOGNIZED_FEATURES.clone();
    } 

    /**
     * Returns the state of a feature.
     *
     * @param featureId The feature identifier.
     *
     * @throws XMLConfigurationException Thrown on configuration error.
     */
    public boolean getFeature(String featureId)
    throws XMLConfigurationException {
        return fLoaderConfig.getFeature(featureId);
    } 

    /**
     * Sets the state of a feature.
     *
     * @param featureId The feature identifier.
     * @param state     The state of the feature.
     *
     * @throws XMLConfigurationException Thrown when a feature is not
     *                  recognized or cannot be set.
     */
    public void setFeature(String featureId,
            boolean state) throws XMLConfigurationException {
        fSettingsChanged = true;
        if(featureId.equals(CONTINUE_AFTER_FATAL_ERROR)) {
            fErrorReporter.setFeature(CONTINUE_AFTER_FATAL_ERROR, state);
        }
        else if(featureId.equals(GENERATE_SYNTHETIC_ANNOTATIONS)) {
            fSchemaHandler.setGenerateSyntheticAnnotations(state);
        }
        fLoaderConfig.setFeature(featureId, state);
    } 

    /**
     * Returns a list of property identifiers that are recognized by
     * this XMLGrammarLoader.  This method may return null if no properties
     * are recognized.
     */
    public String[] getRecognizedProperties() {
        return RECOGNIZED_PROPERTIES.clone();
    } 

    /**
     * Returns the state of a property.
     *
     * @param propertyId The property identifier.
     *
     * @throws XMLConfigurationException Thrown on configuration error.
     */
    public Object getProperty(String propertyId)
    throws XMLConfigurationException {
        return fLoaderConfig.getProperty(propertyId);
    } 

    /**
     * Sets the state of a property.
     *
     * @param propertyId The property identifier.
     * @param state     The state of the property.
     *
     * @throws XMLConfigurationException Thrown when a property is not
     *                  recognized or cannot be set.
     */
    public void setProperty(String propertyId,
            Object state) throws XMLConfigurationException {
        fSettingsChanged = true;
        fLoaderConfig.setProperty(propertyId, state);
        if (propertyId.equals(JAXP_SCHEMA_SOURCE)) {
            fJAXPSource = state;
            fJAXPProcessed = false;
        }
        else if (propertyId.equals(XMLGRAMMAR_POOL)) {
            fGrammarPool = (XMLGrammarPool)state;
        }
        else if (propertyId.equals(SCHEMA_LOCATION)) {
            fExternalSchemas = (String)state;
        }
        else if (propertyId.equals(SCHEMA_NONS_LOCATION)) {
            fExternalNoNSSchema = (String) state;
        }
        else if (propertyId.equals(LOCALE)) {
            setLocale((Locale) state);
        }
        else if (propertyId.equals(ENTITY_RESOLVER)) {
            fEntityManager.setProperty(ENTITY_RESOLVER, state);
        }
        else if (propertyId.equals(ERROR_REPORTER)) {
            fErrorReporter = (XMLErrorReporter)state;
            if (fErrorReporter.getMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN) == null) {
                fErrorReporter.putMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN, new XSMessageFormatter());
            }
        }
        else if (propertyId.equals(XML_SECURITY_PROPERTY_MANAGER)) {
            XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager)state;
            faccessExternalSchema = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_SCHEMA);
        }
    } 

    /**
     * Set the locale to use for messages.
     *
     * @param locale The locale object to use for localization of messages.
     *
     * @exception XNIException Thrown if the parser does not support the
     *                         specified locale.
     */
    public void setLocale(Locale locale) {
        fLocale = locale;
        fErrorReporter.setLocale(locale);
    } 

    /** Return the Locale the XMLGrammarLoader is using. */
    public Locale getLocale() {
        return fLocale;
    } 

    /**
     * Sets the error handler.
     *
     * @param errorHandler The error handler.
     */
    public void setErrorHandler(XMLErrorHandler errorHandler) {
        fErrorReporter.setProperty(ERROR_HANDLER, errorHandler);
    } 

    /** Returns the registered error handler.  */
    public XMLErrorHandler getErrorHandler() {
        return fErrorReporter.getErrorHandler();
    } 

    /**
     * Sets the entity resolver.
     *
     * @param entityResolver The new entity resolver.
     */
    public void setEntityResolver(XMLEntityResolver entityResolver) {
        fUserEntityResolver = entityResolver;
        fLoaderConfig.setProperty(ENTITY_RESOLVER, entityResolver);
        fEntityManager.setProperty(ENTITY_RESOLVER, entityResolver);
    } 

    /** Returns the registered entity resolver.  */
    public XMLEntityResolver getEntityResolver() {
        return fUserEntityResolver;
    } 

    /**
     * Returns a Grammar object by parsing the contents of the
     * entities pointed to by sources.
     *
     * @param source the locations of the entity which forms
     * the staring point of the grammars to be constructed
     * @throws IOException  when a problem is encounted reading the entity
     * @throws XNIException when a condition arises (such as a FatalError) that requires parsing
     *                          of the entity be terminated
     */
    public void loadGrammar(XMLInputSource source[])
    throws IOException, XNIException {
        int numSource = source.length;
        for (int i = 0; i < numSource; ++i) {
            loadGrammar(source[i]);
        }
    }

    /**
     * Returns a Grammar object by parsing the contents of the
     * entity pointed to by source.
     *
     * @param source        the location of the entity which forms
     *                          the starting point of the grammar to be constructed.
     * @throws IOException      When a problem is encountered reading the entity
     *          XNIException    When a condition arises (such as a FatalError) that requires parsing
     *                              of the entity be terminated.
     */
    public Grammar loadGrammar(XMLInputSource source)
    throws IOException, XNIException {


        reset(fLoaderConfig);
        fSettingsChanged = false;
        XSDDescription desc = new XSDDescription();
        desc.fContextType = XSDDescription.CONTEXT_PREPARSE;
        desc.setBaseSystemId(source.getBaseSystemId());
        desc.setLiteralSystemId( source.getSystemId());
        Map<String, LocationArray> locationPairs = new HashMap<>();
        processExternalHints(fExternalSchemas, fExternalNoNSSchema,
                locationPairs, fErrorReporter);
        SchemaGrammar grammar = loadSchema(desc, source, locationPairs);

        if(grammar != null && fGrammarPool != null) {
            fGrammarPool.cacheGrammars(XMLGrammarDescription.XML_SCHEMA, fGrammarBucket.getGrammars());
            if(fIsCheckedFully && fJAXPCache.get(grammar) != grammar) {
                XSConstraints.fullSchemaChecking(fGrammarBucket, fSubGroupHandler, fCMBuilder, fErrorReporter);
            }
        }
        return grammar;
    } 

    /**
     * This method is called either from XMLGrammarLoader.loadGrammar or from XMLSchemaValidator.
     * Note: in either case, the EntityManager (or EntityResolvers) are not going to be invoked
     * to resolve the location of the schema in XSDDescription
     * @param desc
     * @param source
     * @param locationPairs
     * @return An XML Schema grammar
     * @throws IOException
     * @throws XNIException
     */
    SchemaGrammar loadSchema(XSDDescription desc, XMLInputSource source,
            Map<String, LocationArray> locationPairs) throws IOException, XNIException {

        if(!fJAXPProcessed) {
            processJAXPSchemaSource(locationPairs);
        }

        if (desc.isExternal() && !source.isCreatedByResolver()) {
            String accessError = SecuritySupport.checkAccess(desc.getExpandedSystemId(), faccessExternalSchema, JdkConstants.ACCESS_EXTERNAL_ALL);
            if (accessError != null) {
                throw new XNIException(fErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN,
                        "schema_reference.access",
                        new Object[] { SecuritySupport.sanitizePath(desc.getExpandedSystemId()), accessError }, XMLErrorReporter.SEVERITY_ERROR));
            }
        }
        SchemaGrammar grammar = fSchemaHandler.parseSchema(source, desc, locationPairs);

        return grammar;
    } 

    /**
     * This method tries to resolve location of the given schema.
     * The loader stores the namespace/location pairs in a map (use "" as the
     * namespace of absent namespace). When resolving an entity, loader first tries
     * to find in the map whether there is a value for that namespace,
     * if so, pass that location value to the user-defined entity resolver.
     *
     * @param desc
     * @param locationPairs
     * @param entityResolver
     * @return the XMLInputSource
     * @throws IOException
     */
    public static XMLInputSource resolveDocument(XSDDescription desc,
            Map<String, LocationArray> locationPairs,
            XMLEntityResolver entityResolver) throws IOException {
        String loc = null;
        if (desc.getContextType() == XSDDescription.CONTEXT_IMPORT ||
                desc.fromInstance()) {
            String namespace = desc.getTargetNamespace();
            String ns = namespace == null ? XMLSymbols.EMPTY_STRING : namespace;
            LocationArray tempLA = locationPairs.get(ns);
            if(tempLA != null)
                loc = tempLA.getFirstLocation();
        }

        if (loc == null) {
            String[] hints = desc.getLocationHints();
            if (hints != null && hints.length > 0)
                loc = hints[0];
        }

        String expandedLoc = XMLEntityManager.expandSystemId(loc, desc.getBaseSystemId(), false);
        desc.setLiteralSystemId(loc);
        desc.setExpandedSystemId(expandedLoc);
        return entityResolver.resolveEntity(desc);
    }

    public static void processExternalHints(String sl, String nsl,
            Map<String, LocationArray> locations,
            XMLErrorReporter er) {
        if (sl != null) {
            try {
                XSAttributeDecl attrDecl = SchemaGrammar.SG_XSI.getGlobalAttributeDecl(SchemaSymbols.XSI_SCHEMALOCATION);
                attrDecl.fType.validate(sl, null, null);
                if (!tokenizeSchemaLocationStr(sl, locations, null)) {
                    er.reportError(XSMessageFormatter.SCHEMA_DOMAIN,
                            "SchemaLocation",
                            new Object[]{sl},
                            XMLErrorReporter.SEVERITY_WARNING);
                }
            }
            catch (InvalidDatatypeValueException ex) {
                er.reportError(XSMessageFormatter.SCHEMA_DOMAIN,
                        ex.getKey(), ex.getArgs(),
                        XMLErrorReporter.SEVERITY_WARNING);
            }
        }

        if (nsl != null) {
            try {
                XSAttributeDecl attrDecl = SchemaGrammar.SG_XSI.getGlobalAttributeDecl(
                        SchemaSymbols.XSI_NONAMESPACESCHEMALOCATION);
                attrDecl.fType.validate(nsl, null, null);
                LocationArray la = locations.get(XMLSymbols.EMPTY_STRING);
                if(la == null) {
                    la = new LocationArray();
                    locations.put(XMLSymbols.EMPTY_STRING, la);
                }
                la.addLocation(nsl);
            }
            catch (InvalidDatatypeValueException ex) {
                er.reportError(XSMessageFormatter.SCHEMA_DOMAIN,
                        ex.getKey(), ex.getArgs(),
                        XMLErrorReporter.SEVERITY_WARNING);
            }
        }
    }
    public static boolean tokenizeSchemaLocationStr(String schemaStr,
            Map<String, XMLSchemaLoader.LocationArray> locations, String base) {
        if (schemaStr!= null) {
            StringTokenizer t = new StringTokenizer(schemaStr, " \n\t\r");
            String namespace, location;
            while (t.hasMoreTokens()) {
                namespace = t.nextToken ();
                if (!t.hasMoreTokens()) {
                    return false; 
                }
                location = t.nextToken();
                LocationArray la = locations.get(namespace);
                if(la == null) {
                    la = new LocationArray();
                    locations.put(namespace, la);
                }
                if (base != null) {
                    try {
                        location = XMLEntityManager.expandSystemId(location, base, false);
                    } catch (MalformedURIException e) {
                    }
                }
                la.addLocation(location);
            }
        }
        return true;
    } 

    /**
     * Translate the various JAXP SchemaSource property types to XNI
     * XMLInputSource.  Valid types are: String, org.xml.sax.InputSource,
     * InputStream, File, or Object[] of any of previous types.
     * REVISIT:  the JAXP 1.2 spec is less than clear as to whether this property
     * should be available to imported schemas.  I have assumed
     * that it should.  - NG
     * Note: all JAXP schema files will be checked for full-schema validity if the feature was set up
     *
     */
    private void processJAXPSchemaSource(
            Map<String, LocationArray> locationPairs) throws IOException {
        fJAXPProcessed = true;
        if (fJAXPSource == null) {
            return;
        }

        Class<?> componentType = fJAXPSource.getClass().getComponentType();
        XMLInputSource xis = null;
        String sid = null;
        if (componentType == null) {
            if (fJAXPSource instanceof InputStream ||
                    fJAXPSource instanceof InputSource) {
                SchemaGrammar g = fJAXPCache.get(fJAXPSource);
                if (g != null) {
                    fGrammarBucket.putGrammar(g);
                    return;
                }
            }
            fXSDDescription.reset();
            xis = xsdToXMLInputSource(fJAXPSource);
            sid = xis.getSystemId();
            fXSDDescription.fContextType = XSDDescription.CONTEXT_PREPARSE;
            if (sid != null) {
                fXSDDescription.setBaseSystemId(xis.getBaseSystemId());
                fXSDDescription.setLiteralSystemId(sid);
                fXSDDescription.setExpandedSystemId(sid);
                fXSDDescription.fLocationHints = new String[]{sid};
            }
            SchemaGrammar g = loadSchema(fXSDDescription, xis, locationPairs);
            if (g != null) {
                if (fJAXPSource instanceof InputStream ||
                        fJAXPSource instanceof InputSource) {
                    fJAXPCache.put(fJAXPSource, g);
                    if (fIsCheckedFully) {
                        XSConstraints.fullSchemaChecking(fGrammarBucket, fSubGroupHandler, fCMBuilder, fErrorReporter);
                    }
                }
                fGrammarBucket.putGrammar(g);
            }
            return;
        }
        else if ( (componentType != Object.class) &&
                (componentType != String.class) &&
                !File.class.isAssignableFrom(componentType) &&
                !InputStream.class.isAssignableFrom(componentType) &&
                !InputSource.class.isAssignableFrom(componentType) &&
                !componentType.isInterface()
        ) {
            MessageFormatter mf = fErrorReporter.getMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN);
            throw new XMLConfigurationException(
                    Status.NOT_SUPPORTED,
                    mf.formatMessage(fErrorReporter.getLocale(), "jaxp12-schema-source-type.2",
                    new Object [] {componentType.getName()}));
        }

        Object[] objArr = (Object[]) fJAXPSource;
        ArrayList<String> jaxpSchemaSourceNamespaces = new ArrayList<>();
        for (int i = 0; i < objArr.length; i++) {
            if (objArr[i] instanceof InputStream ||
                    objArr[i] instanceof InputSource) {
                SchemaGrammar g = fJAXPCache.get(objArr[i]);
                if (g != null) {
                    fGrammarBucket.putGrammar(g);
                    continue;
                }
            }
            fXSDDescription.reset();
            xis = xsdToXMLInputSource(objArr[i]);
            sid = xis.getSystemId();
            fXSDDescription.fContextType = XSDDescription.CONTEXT_PREPARSE;
            if (sid != null) {
                fXSDDescription.setBaseSystemId(xis.getBaseSystemId());
                fXSDDescription.setLiteralSystemId(sid);
                fXSDDescription.setExpandedSystemId(sid);
                fXSDDescription.fLocationHints = new String[]{sid};
            }
            String targetNamespace = null ;
            SchemaGrammar grammar = fSchemaHandler.parseSchema(xis,fXSDDescription, locationPairs);

            if (fIsCheckedFully) {
                XSConstraints.fullSchemaChecking(fGrammarBucket, fSubGroupHandler, fCMBuilder, fErrorReporter);
            }
            if (grammar != null) {
                targetNamespace = grammar.getTargetNamespace();
                if (jaxpSchemaSourceNamespaces.contains(targetNamespace)) {
                    MessageFormatter mf = fErrorReporter.getMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN);
                    throw new java.lang.IllegalArgumentException(mf.formatMessage(fErrorReporter.getLocale(),
                            "jaxp12-schema-source-ns", null));
                }
                else {
                    jaxpSchemaSourceNamespaces.add(targetNamespace) ;
                }
                if(objArr[i] instanceof InputStream ||
                        objArr[i] instanceof InputSource) {
                    fJAXPCache.put(objArr[i], grammar);
                }
                fGrammarBucket.putGrammar(grammar);
            }
            else {
            }
        }
    }

    private XMLInputSource xsdToXMLInputSource(Object val) {
        if (val instanceof String) {
            String loc = (String) val;
            fXSDDescription.reset();
            fXSDDescription.setValues(null, loc, null, null);
            XMLInputSource xis = null;
            try {
                xis = fEntityManager.resolveEntity(fXSDDescription);
            }
            catch (IOException ex) {
                fErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN,
                        "schema_reference.4",
                        new Object[] { loc }, XMLErrorReporter.SEVERITY_ERROR);
            }
            if (xis == null) {
                return new XMLInputSource(null, loc, null, false);
            }
            return xis;
        }
        else if (val instanceof InputSource) {
            return saxToXMLInputSource((InputSource) val);
        }
        else if (val instanceof InputStream) {
            return new XMLInputSource(null, null, null,
                    (InputStream) val, null);
        }
        else if (val instanceof File) {
            File file = (File) val;
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(file));
            } catch (FileNotFoundException ex) {
                fErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN,
                        "schema_reference.4", new Object[] { file.toString() },
                        XMLErrorReporter.SEVERITY_ERROR);
            }
            return new XMLInputSource(null, file.toURI().toString(), null, is, null);
        }
        MessageFormatter mf = fErrorReporter.getMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN);
        throw new XMLConfigurationException(
                Status.NOT_SUPPORTED,
                mf.formatMessage(fErrorReporter.getLocale(), "jaxp12-schema-source-type.1",
                new Object [] {val != null ? val.getClass().getName() : "null"}));
    }



    private static XMLInputSource saxToXMLInputSource(InputSource sis) {
        String publicId = sis.getPublicId();
        String systemId = sis.getSystemId();

        Reader charStream = sis.getCharacterStream();
        if (charStream != null) {
            return new XMLInputSource(publicId, systemId, null, charStream,
                    null);
        }

        InputStream byteStream = sis.getByteStream();
        if (byteStream != null) {
            return new XMLInputSource(publicId, systemId, null, byteStream,
                    sis.getEncoding());
        }

        return new XMLInputSource(publicId, systemId, null, false);
    }

    public static class LocationArray{

        int length ;
        String [] locations = new String[2];

        public void resize(int oldLength , int newLength){
            String [] temp = new String[newLength] ;
            System.arraycopy(locations, 0, temp, 0, Math.min(oldLength, newLength));
            locations = temp ;
            length = Math.min(oldLength, newLength);
        }

        public void addLocation(String location){
            if(length >= locations.length ){
                resize(length, Math.max(1, length*2));
            }
            locations[length++] = location;
        }

        public String [] getLocationArray(){
            if(length < locations.length ){
                resize(locations.length, length);
            }
            return locations;
        }

        public String getFirstLocation(){
            return length > 0 ? locations[0] : null;
        }

        public int getLength(){
            return length ;
        }

    } 

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.parser.XMLComponent#getFeatureDefault(java.lang.String)
     */
    public Boolean getFeatureDefault(String featureId) {
        if (featureId.equals(AUGMENT_PSVI)){
            return Boolean.TRUE;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.parser.XMLComponent#getPropertyDefault(java.lang.String)
     */
    public Object getPropertyDefault(String propertyId) {
        return null;
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xni.parser.XMLComponent#reset(com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager)
     */
    public void reset(XMLComponentManager componentManager) throws XMLConfigurationException {

        XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager)componentManager.getProperty(XML_SECURITY_PROPERTY_MANAGER);
        if (spm == null) {
            spm = new XMLSecurityPropertyManager();
            setProperty(XML_SECURITY_PROPERTY_MANAGER, spm);
        }

        XMLSecurityManager sm = (XMLSecurityManager)componentManager.getProperty(SECURITY_MANAGER);
        if (sm == null)
            setProperty(SECURITY_MANAGER,new XMLSecurityManager(true));

        faccessExternalSchema = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_SCHEMA);

        fGrammarBucket.reset();

        fSubGroupHandler.reset();

        boolean parser_settings = true;
        if (componentManager != fLoaderConfig) {
            parser_settings = componentManager.getFeature(PARSER_SETTINGS, true);
        }

        if (!parser_settings || !fSettingsChanged){
            fJAXPProcessed = false;
            initGrammarBucket();
            if (fDeclPool != null) {
                fDeclPool.reset();
            }
            return;
        }

        fNodeFactory.reset(componentManager);

        fEntityManager = (XMLEntityManager)componentManager.getProperty(ENTITY_MANAGER);

        fErrorReporter = (XMLErrorReporter)componentManager.getProperty(ERROR_REPORTER);

        SchemaDVFactory dvFactory = null;
        dvFactory = fSchemaHandler.getDVFactory();
        if (dvFactory == null) {
            dvFactory = SchemaDVFactory.getInstance();
            fSchemaHandler.setDVFactory(dvFactory);
        }

        try {
            fExternalSchemas = (String) componentManager.getProperty(SCHEMA_LOCATION);
            fExternalNoNSSchema = (String) componentManager.getProperty(SCHEMA_NONS_LOCATION);
        } catch (XMLConfigurationException e) {
            fExternalSchemas = null;
            fExternalNoNSSchema = null;
        }

        fJAXPSource = componentManager.getProperty(JAXP_SCHEMA_SOURCE, null);
        fJAXPProcessed = false;

        fGrammarPool = (XMLGrammarPool) componentManager.getProperty(XMLGRAMMAR_POOL, null);
        initGrammarBucket();

        boolean psvi = componentManager.getFeature(AUGMENT_PSVI, false);

        if (!psvi && fGrammarPool == null && false) {
            if (fDeclPool != null) {
                fDeclPool.reset();
            }
            else {
                fDeclPool = new XSDeclarationPool();
            }
            fCMBuilder.setDeclPool(fDeclPool);
            fSchemaHandler.setDeclPool(fDeclPool);
            if (dvFactory instanceof SchemaDVFactoryImpl) {
                fDeclPool.setDVFactory((SchemaDVFactoryImpl)dvFactory);
                ((SchemaDVFactoryImpl)dvFactory).setDeclPool(fDeclPool);
            }
        } else {
            fCMBuilder.setDeclPool(null);
            fSchemaHandler.setDeclPool(null);
            if (dvFactory instanceof SchemaDVFactoryImpl) {
                ((SchemaDVFactoryImpl)dvFactory).setDeclPool(null);
            }
        }

        try {
            boolean fatalError = componentManager.getFeature(CONTINUE_AFTER_FATAL_ERROR, false);
            if (!fatalError) {
                fErrorReporter.setFeature(CONTINUE_AFTER_FATAL_ERROR, fatalError);
            }
        } catch (XMLConfigurationException e) {
        }
        fIsCheckedFully = componentManager.getFeature(SCHEMA_FULL_CHECKING, false);

        fSchemaHandler.setGenerateSyntheticAnnotations(componentManager.getFeature(GENERATE_SYNTHETIC_ANNOTATIONS, false));
        fSchemaHandler.reset(componentManager);
    }

    private void initGrammarBucket(){
        if(fGrammarPool != null) {
            Grammar [] initialGrammars = fGrammarPool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_SCHEMA);
            final int length = (initialGrammars != null) ? initialGrammars.length : 0;
            for (int i = 0; i < length; ++i) {
                if (!fGrammarBucket.putGrammar((SchemaGrammar)(initialGrammars[i]), true)) {
                    fErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN,
                            "GrammarConflict", null,
                            XMLErrorReporter.SEVERITY_WARNING);
                }
            }
        }
    }


    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xs.XSLoader#getConfig()
     */
    public DOMConfiguration getConfig() {
        return this;
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xs.XSLoader#load(org.w3c.dom.ls.LSInput)
     */
    public XSModel load(LSInput is) {
        try {
            Grammar g = loadGrammar(dom2xmlInputSource(is));
            return ((XSGrammar) g).toXSModel();
        } catch (Exception e) {
            reportDOMFatalError(e);
            return null;
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xs.XSLoader#loadInputList(com.sun.org.apache.xerces.internal.xs.LSInputList)
     */
    public XSModel loadInputList(LSInputList is) {
        int length = is.getLength();
        SchemaGrammar[] gs = new SchemaGrammar[length];
        for (int i = 0; i < length; i++) {
            try {
                gs[i] = (SchemaGrammar) loadGrammar(dom2xmlInputSource(is.item(i)));
            } catch (Exception e) {
                reportDOMFatalError(e);
                return null;
            }
        }
        return new XSModelImpl(gs);
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xs.XSLoader#loadURI(java.lang.String)
     */
    public XSModel loadURI(String uri) {
        try {
            Grammar g = loadGrammar(new XMLInputSource(null, uri, null, false));
            return ((XSGrammar)g).toXSModel();
        }
        catch (Exception e){
            reportDOMFatalError(e);
            return null;
        }
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xs.XSLoader#loadURIList(com.sun.org.apache.xerces.internal.xs.StringList)
     */
    public XSModel loadURIList(StringList uriList) {
        int length = uriList.getLength();
        SchemaGrammar[] gs = new SchemaGrammar[length];
        for (int i = 0; i < length; i++) {
            try {
                gs[i] =
                    (SchemaGrammar) loadGrammar(new XMLInputSource(null, uriList.item(i), null, false));
            } catch (Exception e) {
                reportDOMFatalError(e);
                return null;
            }
        }
        return new XSModelImpl(gs);
    }

    void reportDOMFatalError(Exception e) {
                if (fErrorHandler != null) {
                    DOMErrorImpl error = new DOMErrorImpl();
                    error.fException = e;
                    error.fMessage = e.getMessage();
                    error.fSeverity = DOMError.SEVERITY_FATAL_ERROR;
                    fErrorHandler.getErrorHandler().handleError(error);
                }
            }

    /* (non-Javadoc)
     * @see DOMConfiguration#canSetParameter(String, Object)
     */
    public boolean canSetParameter(String name, Object value) {
        if(value instanceof Boolean){
            if (name.equals(Constants.DOM_VALIDATE) ||
                name.equals(SCHEMA_FULL_CHECKING) ||
                name.equals(VALIDATE_ANNOTATIONS) ||
                name.equals(CONTINUE_AFTER_FATAL_ERROR) ||
                name.equals(ALLOW_JAVA_ENCODINGS) ||
                name.equals(STANDARD_URI_CONFORMANT_FEATURE) ||
                name.equals(GENERATE_SYNTHETIC_ANNOTATIONS) ||
                name.equals(HONOUR_ALL_SCHEMALOCATIONS) ||
                name.equals(NAMESPACE_GROWTH) ||
                name.equals(TOLERATE_DUPLICATES) ||
                name.equals(OVERRIDE_PARSER)) {
                return true;

            }
            return false;
        }
        if (name.equals(Constants.DOM_ERROR_HANDLER) ||
            name.equals(Constants.DOM_RESOURCE_RESOLVER) ||
            name.equals(SYMBOL_TABLE) ||
            name.equals(ERROR_REPORTER) ||
            name.equals(ERROR_HANDLER) ||
            name.equals(ENTITY_RESOLVER) ||
            name.equals(XMLGRAMMAR_POOL) ||
            name.equals(SCHEMA_LOCATION) ||
            name.equals(SCHEMA_NONS_LOCATION) ||
            name.equals(JAXP_SCHEMA_SOURCE) ||
            name.equals(SCHEMA_DV_FACTORY)) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see DOMConfiguration#getParameter(String)
     */
    public Object getParameter(String name) throws DOMException {

        if (name.equals(Constants.DOM_ERROR_HANDLER)){
            return (fErrorHandler != null) ? fErrorHandler.getErrorHandler() : null;
        }
        else if (name.equals(Constants.DOM_RESOURCE_RESOLVER)) {
            return (fResourceResolver != null) ? fResourceResolver.getEntityResolver() : null;
        }

        try {
            boolean feature = getFeature(name);
            return (feature) ? Boolean.TRUE : Boolean.FALSE;
        } catch (Exception e) {
            Object property;
            try {
                property = getProperty(name);
                return property;
            } catch (Exception ex) {
                String msg =
                    DOMMessageFormatter.formatMessage(
                            DOMMessageFormatter.DOM_DOMAIN,
                            "FEATURE_NOT_SUPPORTED",
                            new Object[] { name });
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
            }
        }
    }

    /* (non-Javadoc)
     * @see DOMConfiguration#getParameterNames()
     */
    public DOMStringList getParameterNames() {
        if (fRecognizedParameters == null){
            ArrayList<String> v = new ArrayList<>();
            v.add(Constants.DOM_VALIDATE);
            v.add(Constants.DOM_ERROR_HANDLER);
            v.add(Constants.DOM_RESOURCE_RESOLVER);
            v.add(SYMBOL_TABLE);
            v.add(ERROR_REPORTER);
            v.add(ERROR_HANDLER);
            v.add(ENTITY_RESOLVER);
            v.add(XMLGRAMMAR_POOL);
            v.add(SCHEMA_LOCATION);
            v.add(SCHEMA_NONS_LOCATION);
            v.add(JAXP_SCHEMA_SOURCE);
            v.add(SCHEMA_FULL_CHECKING);
            v.add(CONTINUE_AFTER_FATAL_ERROR);
            v.add(ALLOW_JAVA_ENCODINGS);
            v.add(STANDARD_URI_CONFORMANT_FEATURE);
            v.add(VALIDATE_ANNOTATIONS);
            v.add(GENERATE_SYNTHETIC_ANNOTATIONS);
            v.add(HONOUR_ALL_SCHEMALOCATIONS);
            v.add(NAMESPACE_GROWTH);
            v.add(TOLERATE_DUPLICATES);
            v.add(OVERRIDE_PARSER);
            fRecognizedParameters = new DOMStringListImpl(v);
        }
        return fRecognizedParameters;
    }

    /* (non-Javadoc)
     * @see DOMConfiguration#setParameter(String, Object)
     */
    public void setParameter(String name, Object value) throws DOMException {
        if (value instanceof Boolean) {
            boolean state = ((Boolean) value).booleanValue();
            if (name.equals("validate") && state) {
                return;
            }
            try {
                setFeature(name, state);
            } catch (Exception e) {
                String msg =
                    DOMMessageFormatter.formatMessage(
                            DOMMessageFormatter.DOM_DOMAIN,
                            "FEATURE_NOT_SUPPORTED",
                            new Object[] { name });
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
            }
            return;
        }
        if (name.equals(Constants.DOM_ERROR_HANDLER)) {
            if (value instanceof DOMErrorHandler) {
                try {
                    fErrorHandler = new DOMErrorHandlerWrapper((DOMErrorHandler) value);
                    setErrorHandler(fErrorHandler);
                } catch (XMLConfigurationException e) {
                }
            } else {
                String msg =
                    DOMMessageFormatter.formatMessage(
                            DOMMessageFormatter.DOM_DOMAIN,
                            "FEATURE_NOT_SUPPORTED",
                            new Object[] { name });
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
            }
            return;

        }
        if (name.equals(Constants.DOM_RESOURCE_RESOLVER)) {
            if (value instanceof LSResourceResolver) {
                try {
                    fResourceResolver = new DOMEntityResolverWrapper((LSResourceResolver) value);
                    setEntityResolver(fResourceResolver);
                }
                catch (XMLConfigurationException e) {}
            } else {
                String msg =
                    DOMMessageFormatter.formatMessage(
                            DOMMessageFormatter.DOM_DOMAIN,
                            "FEATURE_NOT_SUPPORTED",
                            new Object[] { name });
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
            }
            return;
        }

        try {
            setProperty(name, value);
        } catch (Exception ex) {

            String msg =
                DOMMessageFormatter.formatMessage(
                        DOMMessageFormatter.DOM_DOMAIN,
                        "FEATURE_NOT_SUPPORTED",
                        new Object[] { name });
            throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);

        }

    }

    XMLInputSource dom2xmlInputSource(LSInput is) {
        XMLInputSource xis = null;

        /**
         * An LSParser looks at inputs specified in LSInput in
         * the following order: characterStream, byteStream,
         * stringData, systemId, publicId. For consistency
         * have the same behaviour for XSLoader.
         */

        if (is.getCharacterStream() != null) {
            xis = new XMLInputSource(is.getPublicId(), is.getSystemId(),
                    is.getBaseURI(), is.getCharacterStream(),
            "UTF-16");
        }
        else if (is.getByteStream() != null) {
            xis = new XMLInputSource(is.getPublicId(), is.getSystemId(),
                    is.getBaseURI(), is.getByteStream(),
                    is.getEncoding());
        }
        else if (is.getStringData() != null && is.getStringData().length() != 0) {
            xis = new XMLInputSource(is.getPublicId(), is.getSystemId(),
                    is.getBaseURI(), new StringReader(is.getStringData()),
            "UTF-16");
        }
        else {
            xis = new XMLInputSource(is.getPublicId(), is.getSystemId(),
                    is.getBaseURI(), false);
        }

        return xis;
    }

    public XSElementDecl getGlobalElementDecl(QName element) {
        SchemaGrammar sGrammar = fGrammarBucket.getGrammar(element.uri);
        if (sGrammar != null) {
            return sGrammar.getGlobalElementDecl(element.localpart);
        }
        return null;
    }

} 
