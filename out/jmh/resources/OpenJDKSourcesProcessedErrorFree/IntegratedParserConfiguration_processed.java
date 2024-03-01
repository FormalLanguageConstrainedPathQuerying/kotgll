/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

import com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl;
import com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl;
import com.sun.org.apache.xerces.internal.impl.dtd.XMLDTDValidator;
import com.sun.org.apache.xerces.internal.impl.dtd.XMLNSDTDValidator;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaValidator;
import com.sun.org.apache.xerces.internal.impl.xs.XSMessageFormatter;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentScanner;

/**
 * This is configuration uses a scanner that integrates both scanning of the document
 * and binding namespaces.
 *
 * If namespace feature is turned on, the pipeline is constructured with the
 * following components:
 * XMLNSDocumentScannerImpl -> XMLNSDTDValidator -> (optional) XMLSchemaValidator
 *
 * If the namespace feature is turned off the default document scanner implementation
 * is used (XMLDocumentScannerImpl).
 * <p>
 * In addition to the features and properties recognized by the base
 * parser configuration, this class recognizes these additional
 * features and properties:
 * <ul>
 * <li>Features
 *  <ul>
 *  <li>http:
 *  <li>http:
 *  <li>http:
 *  <li>http:
 *  </ul>
 * <li>Properties
 *  <ul>
 *   <li>http:
 *   <li>http:
 *   <li>http:
 *   <li>http:
 *   <li>http:
 *   <li>http:
 *   <li>http:
 *  </ul>
 * </ul>
 *
 * @author Elena Litani, IBM
 *
 */
public class IntegratedParserConfiguration
extends StandardParserConfiguration {



    /** Document scanner that does namespace binding. */
    protected XMLNSDocumentScannerImpl fNamespaceScanner;

    /** Default Xerces implementation of scanner */
    protected XMLDocumentScannerImpl fNonNSScanner;

    /** DTD Validator that does not bind namespaces */
    protected XMLDTDValidator fNonNSDTDValidator;


    /** Default constructor. */
    public IntegratedParserConfiguration() {
        this(null, null, null);
    } 

    /**
     * Constructs a parser configuration using the specified symbol table.
     *
     * @param symbolTable The symbol table to use.
     */
    public IntegratedParserConfiguration(SymbolTable symbolTable) {
        this(symbolTable, null, null);
    } 

    /**
     * Constructs a parser configuration using the specified symbol table and
     * grammar pool.
     * <p>
     * <strong>REVISIT:</strong>
     * Grammar pool will be updated when the new validation engine is
     * implemented.
     *
     * @param symbolTable The symbol table to use.
     * @param grammarPool The grammar pool to use.
     */
    public IntegratedParserConfiguration(SymbolTable symbolTable,
                                         XMLGrammarPool grammarPool) {
        this(symbolTable, grammarPool, null);
    } 

    /**
     * Constructs a parser configuration using the specified symbol table,
     * grammar pool, and parent settings.
     * <p>
     * <strong>REVISIT:</strong>
     * Grammar pool will be updated when the new validation engine is
     * implemented.
     *
     * @param symbolTable    The symbol table to use.
     * @param grammarPool    The grammar pool to use.
     * @param parentSettings The parent settings.
     */
    public IntegratedParserConfiguration(SymbolTable symbolTable,
                                         XMLGrammarPool grammarPool,
                                         XMLComponentManager parentSettings) {
        super(symbolTable, grammarPool, parentSettings);

        fNonNSScanner = new XMLDocumentScannerImpl();
        fNonNSDTDValidator = new XMLDTDValidator();

        addComponent((XMLComponent)fNonNSScanner);
        addComponent((XMLComponent)fNonNSDTDValidator);

    } 


    /** Configures the pipeline. */
        protected void configurePipeline() {

                setProperty(DATATYPE_VALIDATOR_FACTORY, fDatatypeValidatorFactory);

                configureDTDPipeline();

                if (fFeatures.get(NAMESPACES) == Boolean.TRUE) {
            fProperties.put(NAMESPACE_BINDER, fNamespaceBinder);
                        fScanner = fNamespaceScanner;
                        fProperties.put(DOCUMENT_SCANNER, fNamespaceScanner);
                        if (fDTDValidator != null) {
                                fProperties.put(DTD_VALIDATOR, fDTDValidator);
                                fNamespaceScanner.setDTDValidator(fDTDValidator);
                                fNamespaceScanner.setDocumentHandler(fDTDValidator);
                                fDTDValidator.setDocumentSource(fNamespaceScanner);
                                fDTDValidator.setDocumentHandler(fDocumentHandler);
                                if (fDocumentHandler != null) {
                                        fDocumentHandler.setDocumentSource(fDTDValidator);
                                }
                                fLastComponent = fDTDValidator;
                        }
                        else {
                                fNamespaceScanner.setDocumentHandler(fDocumentHandler);
                fNamespaceScanner.setDTDValidator(null);
                                if (fDocumentHandler != null) {
                                        fDocumentHandler.setDocumentSource(fNamespaceScanner);
                                }
                                fLastComponent = fNamespaceScanner;
                        }
                }
                else {
                        fScanner = fNonNSScanner;
                        fProperties.put(DOCUMENT_SCANNER, fNonNSScanner);
                        if (fNonNSDTDValidator != null) {
                                fProperties.put(DTD_VALIDATOR, fNonNSDTDValidator);
                                fNonNSScanner.setDocumentHandler(fNonNSDTDValidator);
                                fNonNSDTDValidator.setDocumentSource(fNonNSScanner);
                                fNonNSDTDValidator.setDocumentHandler(fDocumentHandler);
                                if (fDocumentHandler != null) {
                                        fDocumentHandler.setDocumentSource(fNonNSDTDValidator);
                                }
                                fLastComponent = fNonNSDTDValidator;
                        }
                        else {
                                fScanner.setDocumentHandler(fDocumentHandler);
                                if (fDocumentHandler != null) {
                                        fDocumentHandler.setDocumentSource(fScanner);
                                }
                                fLastComponent = fScanner;
                        }
                }

                if (fFeatures.get(XMLSCHEMA_VALIDATION) == Boolean.TRUE) {
                        if (fSchemaValidator == null) {
                                fSchemaValidator = new XMLSchemaValidator();

                                fProperties.put(SCHEMA_VALIDATOR, fSchemaValidator);
                                addComponent(fSchemaValidator);
                                if (fErrorReporter.getMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN) == null) {
                                        XSMessageFormatter xmft = new XSMessageFormatter();
                                        fErrorReporter.putMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN, xmft);
                                }

                        }

                        fLastComponent.setDocumentHandler(fSchemaValidator);
                        fSchemaValidator.setDocumentSource(fLastComponent);
                        fSchemaValidator.setDocumentHandler(fDocumentHandler);
                        if (fDocumentHandler != null) {
                                fDocumentHandler.setDocumentSource(fSchemaValidator);
                        }
                        fLastComponent = fSchemaValidator;
                }
        } 



    /** Create a document scanner: this scanner performs namespace binding
      */
    protected XMLDocumentScanner createDocumentScanner() {
        fNamespaceScanner = new XMLNSDocumentScannerImpl();
        return fNamespaceScanner;
    } 


    /** Create a DTD validator: this validator performs namespace binding.
      */
    protected XMLDTDValidator createDTDValidator() {
        return new XMLNSDTDValidator();
    } 

} 
