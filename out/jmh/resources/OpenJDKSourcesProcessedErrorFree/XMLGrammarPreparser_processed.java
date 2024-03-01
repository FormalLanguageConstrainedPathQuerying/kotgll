/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.parsers;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.utils.ObjectFactory;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarLoader;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <p> This class provides an easy way for a user to preparse grammars
 * of various types.  By default, it knows how to preparse external
 * DTD's and schemas; it provides an easy way for user applications to
 * register classes that know how to parse additional grammar types.
 * By default, it does no grammar caching; but it provides ways for
 * user applications to do so.
 *
 * @author Neil Graham, IBM
 *
 * @LastModified: Oct 2017
 */
public class XMLGrammarPreparser {


    private final static String CONTINUE_AFTER_FATAL_ERROR =
        Constants.XERCES_FEATURE_PREFIX + Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE;

    /** Property identifier: symbol table. */
    protected static final String SYMBOL_TABLE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property identifier: error reporter. */
    protected static final String ERROR_REPORTER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Property identifier: error handler. */
    protected static final String ERROR_HANDLER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_HANDLER_PROPERTY;

    /** Property identifier: entity resolver. */
    protected static final String ENTITY_RESOLVER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

    /** Property identifier: grammar pool . */
    protected static final String GRAMMAR_POOL =
        Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

    private static final Map<String, String> KNOWN_LOADERS;

    static {
        Map<String, String> loaders = new HashMap<>();
        loaders.put(XMLGrammarDescription.XML_SCHEMA,
            "com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader");
        loaders.put(XMLGrammarDescription.XML_DTD,
            "com.sun.org.apache.xerces.internal.impl.dtd.XMLDTDLoader");
        KNOWN_LOADERS = Collections.unmodifiableMap(loaders);
    }

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
        SYMBOL_TABLE,
        ERROR_REPORTER,
        ERROR_HANDLER,
        ENTITY_RESOLVER,
        GRAMMAR_POOL,
    };

    protected SymbolTable fSymbolTable;
    protected XMLErrorReporter fErrorReporter;
    protected XMLEntityResolver fEntityResolver;
    protected XMLGrammarPool fGrammarPool;

    protected Locale fLocale;

    private Map<String, XMLGrammarLoader> fLoaders;


    /** Default constructor. */
    public XMLGrammarPreparser() {
        this(new SymbolTable());
    } 

    /**
     * Constructs a preparser using the specified symbol table.
     *
     * @param symbolTable The symbol table to use.
     */
    public XMLGrammarPreparser (SymbolTable symbolTable) {
        fSymbolTable = symbolTable;

        fLoaders = new HashMap<>();
        fErrorReporter = new XMLErrorReporter();
        setLocale(Locale.getDefault());
        fEntityResolver = new XMLEntityManager();
    } 


    /*
    * Register a type of grammar to make it preparsable.   If
    * the second parameter is null, the parser will use its  built-in
    * facilities for that grammar type.
    * This should be called by the application immediately
    * after creating this object and before initializing any properties/features.
    * @param type   URI identifying the type of the grammar
    * @param loader an object capable of preparsing that type; null if the ppreparser should use built-in knowledge.
    * @return true if successful; false if no built-in knowledge of
    *       the type or if unable to instantiate the string we know about
    */
    public boolean registerPreparser(String grammarType, XMLGrammarLoader loader) {
        if(loader == null) { 
            if(KNOWN_LOADERS.containsKey(grammarType)) {
                String loaderName = KNOWN_LOADERS.get(grammarType);
                try {
                    XMLGrammarLoader gl = (XMLGrammarLoader)(ObjectFactory.newInstance(loaderName, true));
                    fLoaders.put(grammarType, gl);
                } catch (Exception e) {
                    return false;
                }
                return true;
            }
            return false;
        }
        fLoaders.put(grammarType, loader);
        return true;
    } 

    /**
     * Parse a grammar from a location identified by an
     * XMLInputSource.
     * This method also adds this grammar to the XMLGrammarPool
     *
     * @param type The type of the grammar to be constructed
     * @param is The XMLInputSource containing this grammar's
     * information
     * <strong>If a URI is included in the systemId field, the parser will not expand this URI or make it
     * available to the EntityResolver</strong>
     * @return The newly created <code>Grammar</code>.
     * @exception XNIException thrown on an error in grammar
     * construction
     * @exception IOException thrown if an error is encountered
     * in reading the file
     */
    public Grammar preparseGrammar(String type, XMLInputSource
                is) throws XNIException, IOException {
        if(fLoaders.containsKey(type)) {
            XMLGrammarLoader gl = fLoaders.get(type);
            gl.setProperty(SYMBOL_TABLE, fSymbolTable);
            gl.setProperty(ENTITY_RESOLVER, fEntityResolver);
            gl.setProperty(ERROR_REPORTER, fErrorReporter);
            if(fGrammarPool != null) {
                try {
                    gl.setProperty(GRAMMAR_POOL, fGrammarPool);
                } catch(Exception e) {
                }
            }
            return gl.loadGrammar(is);
        }
        return null;
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
    } 

    /** Returns the registered entity resolver.  */
    public XMLEntityResolver getEntityResolver() {
        return fEntityResolver;
    } 

    /**
     * Sets the grammar pool.
     *
     * @param grammarPool The new grammar pool.
     */
    public void setGrammarPool(XMLGrammarPool grammarPool) {
        fGrammarPool = grammarPool;
    } 

    /** Returns the registered grammar pool.  */
    public XMLGrammarPool getGrammarPool() {
        return fGrammarPool;
    } 

    public XMLGrammarLoader getLoader(String type) {
        return fLoaders.get(type);
    } 

    public void setFeature(String featureId, boolean value) {
        for (Map.Entry<String, XMLGrammarLoader> entry : fLoaders.entrySet()) {
            try {
                XMLGrammarLoader gl = entry.getValue();
                gl.setFeature(featureId, value);
            } catch(Exception e) {
            }
        }
        if(featureId.equals(CONTINUE_AFTER_FATAL_ERROR)) {
            fErrorReporter.setFeature(CONTINUE_AFTER_FATAL_ERROR, value);
        }
    } 

    public void setProperty(String propId, Object value) {
        for (Map.Entry<String, XMLGrammarLoader> entry : fLoaders.entrySet()) {
            try {
                XMLGrammarLoader gl = entry.getValue();
                gl.setProperty(propId, value);
            } catch(Exception e) {
            }
        }
    } 

    public boolean getFeature(String type, String featureId) {
        XMLGrammarLoader gl = fLoaders.get(type);
        return gl.getFeature(featureId);
    } 

    public Object getProperty(String type, String propertyId) {
        XMLGrammarLoader gl = fLoaders.get(type);
        return gl.getProperty(propertyId);
    } 
} 
