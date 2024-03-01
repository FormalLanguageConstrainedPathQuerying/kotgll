/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl.dtd;

import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.XMLDTDScannerImpl;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;

import com.sun.org.apache.xerces.internal.util.Status;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.DefaultErrorHandler;

import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarLoader;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;


/**
 * The DTD loader. The loader knows how to build grammars from XMLInputSources.
 * It extends the DTD processor in order to do this; it's
 * a separate class because DTD processors don't need to know how
 * to talk to the outside world in their role as instance-document
 * helpers.
 * <p>
 * This component requires the following features and properties.  It
 * know ho to set them if no one else does:from the
 * <ul>
 *  <li>http:
 *  <li>http:
 *  <li>http:
 *  <li>http:
 *  <li>http:
 * </ul>
 *
 * @xerces.internal
 *
 * @author Neil Graham, IBM
 * @author Michael Glavassevich, IBM
 *
 * @LastModified: Nov 2017
 */
public class XMLDTDLoader
        extends XMLDTDProcessor
        implements XMLGrammarLoader {



    /** Feature identifier: standard uri conformant feature. */
    protected static final String STANDARD_URI_CONFORMANT_FEATURE =
        Constants.XERCES_FEATURE_PREFIX + Constants.STANDARD_URI_CONFORMANT_FEATURE;

    /** Feature identifier: balance syntax trees. */
    protected static final String BALANCE_SYNTAX_TREES =
        Constants.XERCES_FEATURE_PREFIX + Constants.BALANCE_SYNTAX_TREES;

    private static final String[] LOADER_RECOGNIZED_FEATURES = {
        VALIDATION,
        WARN_ON_DUPLICATE_ATTDEF,
        WARN_ON_UNDECLARED_ELEMDEF,
        NOTIFY_CHAR_REFS,
        STANDARD_URI_CONFORMANT_FEATURE,
        BALANCE_SYNTAX_TREES
    };


    /** Property identifier: error handler. */
    protected static final String ERROR_HANDLER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_HANDLER_PROPERTY;

    /** Property identifier: entity resolver. */
    public static final String ENTITY_RESOLVER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

    /** Property identifier: locale. */
    public static final String LOCALE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.LOCALE_PROPERTY;

    /** Recognized properties. */
    private static final String[] LOADER_RECOGNIZED_PROPERTIES = {
        SYMBOL_TABLE,
        ERROR_REPORTER,
        ERROR_HANDLER,
        ENTITY_RESOLVER,
        GRAMMAR_POOL,
        DTD_VALIDATOR,
        LOCALE
    };

    private boolean fStrictURI = false;

    /** Controls whether the DTD grammar produces balanced syntax trees. */
    private boolean fBalanceSyntaxTrees = false;

    /** Entity resolver . */
    protected XMLEntityResolver fEntityResolver;

    protected XMLDTDScannerImpl fDTDScanner;

    protected XMLEntityManager fEntityManager;

    protected Locale fLocale;


    /** Deny default construction; we need a SymtolTable! */
    public XMLDTDLoader() {
        this(new SymbolTable());
    } 

    public XMLDTDLoader(SymbolTable symbolTable) {
        this(symbolTable, null);
    } 

    public XMLDTDLoader(SymbolTable symbolTable,
                XMLGrammarPool grammarPool) {
        this(symbolTable, grammarPool, null, new XMLEntityManager());
    } 

    XMLDTDLoader(SymbolTable symbolTable,
                XMLGrammarPool grammarPool, XMLErrorReporter errorReporter,
                XMLEntityResolver entityResolver) {
        fSymbolTable = symbolTable;
        fGrammarPool = grammarPool;
        if(errorReporter == null) {
            errorReporter = new XMLErrorReporter();
            errorReporter.setProperty(ERROR_HANDLER, new DefaultErrorHandler());
        }
        fErrorReporter = errorReporter;
        if (fErrorReporter.getMessageFormatter(XMLMessageFormatter.XML_DOMAIN) == null) {
            XMLMessageFormatter xmft = new XMLMessageFormatter();
            fErrorReporter.putMessageFormatter(XMLMessageFormatter.XML_DOMAIN, xmft);
            fErrorReporter.putMessageFormatter(XMLMessageFormatter.XMLNS_DOMAIN, xmft);
        }
        fEntityResolver = entityResolver;
        if(fEntityResolver instanceof XMLEntityManager) {
            fEntityManager = (XMLEntityManager)fEntityResolver;
        } else {
            fEntityManager = new XMLEntityManager();
        }
        fEntityManager.setProperty(Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY, errorReporter);
        fDTDScanner = createDTDScanner(fSymbolTable, fErrorReporter, fEntityManager);
        fDTDScanner.setDTDHandler(this);
        fDTDScanner.setDTDContentModelHandler(this);
        reset();
    } 


    /**
     * Returns a list of feature identifiers that are recognized by
     * this component. This method may return null if no features
     * are recognized by this component.
     */
    public String[] getRecognizedFeatures() {
        return LOADER_RECOGNIZED_FEATURES.clone();
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
        if (featureId.equals(VALIDATION)) {
            fValidation = state;
        }
        else if (featureId.equals(WARN_ON_DUPLICATE_ATTDEF)) {
            fWarnDuplicateAttdef = state;
        }
        else if (featureId.equals(WARN_ON_UNDECLARED_ELEMDEF)) {
            fWarnOnUndeclaredElemdef = state;
        }
        else if (featureId.equals(NOTIFY_CHAR_REFS)) {
            fDTDScanner.setFeature(featureId, state);
        }
        else if (featureId.equals(STANDARD_URI_CONFORMANT_FEATURE)) {
            fStrictURI = state;
        }
        else if (featureId.equals(BALANCE_SYNTAX_TREES)) {
            fBalanceSyntaxTrees = state;
        }
        else {
            throw new XMLConfigurationException(Status.NOT_RECOGNIZED, featureId);
        }
    } 

    /**
     * Returns a list of property identifiers that are recognized by
     * this component. This method may return null if no properties
     * are recognized by this component.
     */
    public String[] getRecognizedProperties() {
        return LOADER_RECOGNIZED_PROPERTIES.clone();
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
        if (propertyId.equals(SYMBOL_TABLE)) {
            return fSymbolTable;
        }
        else if (propertyId.equals(ERROR_REPORTER)) {
            return fErrorReporter;
        }
        else if (propertyId.equals(ERROR_HANDLER)) {
            return fErrorReporter.getErrorHandler();
        }
        else if (propertyId.equals(ENTITY_RESOLVER)) {
            return fEntityResolver;
        }
        else if (propertyId.equals(LOCALE)) {
            return getLocale();
        }
        else if (propertyId.equals(GRAMMAR_POOL)) {
            return fGrammarPool;
        }
        else if (propertyId.equals(DTD_VALIDATOR)) {
            return fValidator;
        }
        throw new XMLConfigurationException(Status.NOT_RECOGNIZED, propertyId);
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
        if (propertyId.equals(SYMBOL_TABLE)) {
            fSymbolTable = (SymbolTable)value;
            fDTDScanner.setProperty(propertyId, value);
            fEntityManager.setProperty(propertyId, value);
        }
        else if(propertyId.equals(ERROR_REPORTER)) {
            fErrorReporter = (XMLErrorReporter)value;
            if (fErrorReporter.getMessageFormatter(XMLMessageFormatter.XML_DOMAIN) == null) {
                XMLMessageFormatter xmft = new XMLMessageFormatter();
                fErrorReporter.putMessageFormatter(XMLMessageFormatter.XML_DOMAIN, xmft);
                fErrorReporter.putMessageFormatter(XMLMessageFormatter.XMLNS_DOMAIN, xmft);
            }
            fDTDScanner.setProperty(propertyId, value);
            fEntityManager.setProperty(propertyId, value);
        }
        else if (propertyId.equals(ERROR_HANDLER)) {
            fErrorReporter.setProperty(propertyId, value);
        }
        else if (propertyId.equals(ENTITY_RESOLVER)) {
            fEntityResolver = (XMLEntityResolver)value;
            fEntityManager.setProperty(propertyId, value);
        }
        else if (propertyId.equals(LOCALE)) {
            setLocale((Locale) value);
        }
        else if(propertyId.equals(GRAMMAR_POOL)) {
            fGrammarPool = (XMLGrammarPool)value;
        }
        else {
            throw new XMLConfigurationException(Status.NOT_RECOGNIZED, propertyId);
        }
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
        if (featureId.equals(VALIDATION)) {
            return fValidation;
        }
        else if (featureId.equals(WARN_ON_DUPLICATE_ATTDEF)) {
            return fWarnDuplicateAttdef;
        }
        else if (featureId.equals(WARN_ON_UNDECLARED_ELEMDEF)) {
            return fWarnOnUndeclaredElemdef;
        }
        else if (featureId.equals(NOTIFY_CHAR_REFS)) {
            return fDTDScanner.getFeature(featureId);
        }
        else if (featureId.equals(STANDARD_URI_CONFORMANT_FEATURE)) {
            return fStrictURI;
        }
        else if (featureId.equals(BALANCE_SYNTAX_TREES)) {
            return fBalanceSyntaxTrees;
        }
        throw new XMLConfigurationException(Status.NOT_RECOGNIZED, featureId);
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
        fEntityResolver = entityResolver;
        fEntityManager.setProperty(ENTITY_RESOLVER, entityResolver);
    } 

    /** Returns the registered entity resolver.  */
    public XMLEntityResolver getEntityResolver() {
        return fEntityResolver;
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
        reset();
        String eid = XMLEntityManager.expandSystemId(source.getSystemId(), source.getBaseSystemId(), fStrictURI);
        XMLDTDDescription desc = new XMLDTDDescription(source.getPublicId(), source.getSystemId(), source.getBaseSystemId(), eid, null);
        if (!fBalanceSyntaxTrees) {
            fDTDGrammar = new DTDGrammar(fSymbolTable, desc);
        }
        else {
            fDTDGrammar = new BalancedDTDGrammar(fSymbolTable, desc);
        }
        fGrammarBucket = new DTDGrammarBucket();
        fGrammarBucket.setStandalone(false);
        fGrammarBucket.setActiveGrammar(fDTDGrammar);

        try {
            fDTDScanner.setInputSource(source);
            fDTDScanner.scanDTDExternalSubset(true);
        } catch (EOFException e) {
        }
        finally {
            fEntityManager.closeReaders();
        }
        if(fDTDGrammar != null && fGrammarPool != null) {
            fGrammarPool.cacheGrammars(XMLDTDDescription.XML_DTD, new Grammar[] {fDTDGrammar});
        }
        return fDTDGrammar;
    } 

    /**
     * Parse a DTD internal and/or external subset and insert the content
     * into the existing DTD grammar owned by the given DTDValidator.
     */
    public void loadGrammarWithContext(XMLDTDValidator validator, String rootName,
            String publicId, String systemId, String baseSystemId, String internalSubset)
        throws IOException, XNIException {
        final DTDGrammarBucket grammarBucket = validator.getGrammarBucket();
        final DTDGrammar activeGrammar = grammarBucket.getActiveGrammar();
        if (activeGrammar != null && !activeGrammar.isImmutable()) {
            fGrammarBucket = grammarBucket;
            fEntityManager.setScannerVersion(getScannerVersion());
            reset();
            try {
                if (internalSubset != null) {
                    StringBuffer buffer = new StringBuffer(internalSubset.length() + 2);
                    buffer.append(internalSubset).append("]>");
                    XMLInputSource is = new XMLInputSource(null, baseSystemId,
                            null, new StringReader(buffer.toString()), null);
                    fEntityManager.startDocumentEntity(is);
                    fDTDScanner.scanDTDInternalSubset(true, false, systemId != null);
                }
                if (systemId != null) {
                    XMLDTDDescription desc = new XMLDTDDescription(publicId, systemId, baseSystemId, null, rootName);
                    XMLInputSource source = fEntityManager.resolveEntity(desc);
                    fDTDScanner.setInputSource(source);
                    fDTDScanner.scanDTDExternalSubset(true);
                }
            }
            catch (EOFException e) {
            }
            finally {
                fEntityManager.closeReaders();
            }
        }
    } 

    protected void reset() {
        super.reset();
        fDTDScanner.reset();
        fEntityManager.reset();
        fErrorReporter.setDocumentLocator(fEntityManager.getEntityScanner());
    }

    protected XMLDTDScannerImpl createDTDScanner(SymbolTable symbolTable,
            XMLErrorReporter errorReporter, XMLEntityManager entityManager) {
        return new XMLDTDScannerImpl(symbolTable, errorReporter, entityManager);
    } 

    protected short getScannerVersion() {
        return Constants.XML_VERSION_1_0;
    } 

} 
