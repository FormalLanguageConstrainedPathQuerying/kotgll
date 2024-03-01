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
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.sun.org.apache.xerces.internal.impl;

import java.io.IOException;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.impl.dtd.XMLDTDValidatorFilter;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.util.XMLAttributesImpl;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentSource;
import javax.xml.stream.events.XMLEvent;
import jdk.xml.internal.XMLSecurityManager;

/**
 * This class adds the functionality of namespace processing.
 *
 * This class has been modified as per the new design which is more suited to
 * efficiently build pull parser. Lot of improvements have been done and
 * the code has been added to support stax functionality/features.
 *
 *
 * This class scans an XML document, checks if document has a DTD, and if
 * DTD is not found the scanner will remove the DTD Validator from the pipeline and perform
 * namespace binding.
 *
 *
 * @author Neeraj Bajaj, Sun Microsystems
 * @author Venugopal Rao K, Sun Microsystems
 * @author Elena Litani, IBM
 *
 * @LastModified: July 2023
 */
public class XMLNSDocumentScannerImpl
        extends XMLDocumentScannerImpl {

    /**
     * If is true, the dtd validator is no longer in the pipeline
     * and the scanner should bind namespaces
     */
    protected boolean fBindNamespaces;

    /** If validating parser, make sure we report an error in the
     *   scanner if DTD grammar is missing.*/
    protected boolean fPerformValidation;


    /** Default value of this feature is false, when in Stax mode this should be true */
    protected boolean fNotAddNSDeclAsAttribute = false;

    /** DTD validator */
     private XMLDTDValidatorFilter fDTDValidator;

     /** xmlns, default Namespace, declared */
     private boolean fXmlnsDeclared = false;

    /** Resets the fields of this scanner.
     */
    public void reset(PropertyManager propertyManager) {
        setPropertyManager(propertyManager);
        super.reset(propertyManager);
        fBindNamespaces = false;
        fNotAddNSDeclAsAttribute = !((Boolean)propertyManager.getProperty(Constants.ADD_NAMESPACE_DECL_AS_ATTRIBUTE)).booleanValue();
    }

    public void reset(XMLComponentManager componentManager)
    throws XMLConfigurationException {
        super.reset(componentManager);
        fNotAddNSDeclAsAttribute = false ;
        fPerformValidation = false;
        fBindNamespaces = false;
    }

    /** return the next state on the input
     *
     * @return int
     */

    public int next() throws IOException, XNIException {

        if((fScannerLastState == XMLEvent.END_ELEMENT) && fBindNamespaces){
            fScannerLastState = -1;
            fNamespaceContext.popContext();
        }

        return fScannerLastState = super.next();
    }

    /**
     * The scanner is responsible for removing DTD validator
     * from the pipeline if it is not needed.
     *
     * @param previous The filter component before DTDValidator
     * @param dtdValidator
     *                 The DTDValidator
     * @param next     The documentHandler after the DTDValidator
     */
    public void setDTDValidator(XMLDTDValidatorFilter dtd){
        fDTDValidator = dtd;
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
     *
     * @return True if element is empty. (i.e. It matches
     *          production [44].
     */
    protected boolean scanStartElement()
    throws IOException, XNIException {

        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +">>> scanStartElement()");
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
                    System.out.println("Elements are being ADDED -- elemet added is = " + fElementQName.rawname + " at count = " + fElementStack.fCount);
                }
            }

        }

        if(fAdd){
            fElementStack.matchElement(fElementQName);
        }

        fCurrentElement = fElementQName;

        String rawname = fElementQName.rawname;
        checkDepth(rawname);
        if (fBindNamespaces) {
            fNamespaceContext.pushContext();
            if (fScannerState == SCANNER_STATE_ROOT_ELEMENT) {
                if (fPerformValidation) {
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                            "MSG_GRAMMAR_NOT_FOUND",
                            new Object[]{ rawname},
                            XMLErrorReporter.SEVERITY_ERROR);

                            if (fDoctypeName == null || !fDoctypeName.equals(rawname)) {
                                fErrorReporter.reportError( XMLMessageFormatter.XML_DOMAIN,
                                        "RootElementTypeMustMatchDoctypedecl",
                                        new Object[]{fDoctypeName, rawname},
                                        XMLErrorReporter.SEVERITY_ERROR);
                            }
                }
            }
        }


        fEmptyElement = false;
        fAttributes.removeAllAttributes();

        if(!seekCloseOfStartTag()){
            fReadingAttributes = true;
            fAttributeCacheUsedCount =0;
            fStringBufferIndex =0;
            fAddDefaultAttr = true;
            fXmlnsDeclared = false;

            do {
                scanAttribute(fAttributes);
                if (fSecurityManager != null && (!fSecurityManager.isNoLimit(fElementAttributeLimit)) &&
                        fAttributes.getLength() > fElementAttributeLimit){
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                 "ElementAttributeLimit",
                                                 new Object[]{rawname, fElementAttributeLimit },
                                                 XMLErrorReporter.SEVERITY_FATAL_ERROR );
                }

            } while (!seekCloseOfStartTag());
            fReadingAttributes=false;
        }

        if (fBindNamespaces) {
            if (fElementQName.prefix == XMLSymbols.PREFIX_XMLNS) {
                fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                        "ElementXMLNSPrefix",
                        new Object[]{fElementQName.rawname},
                        XMLErrorReporter.SEVERITY_FATAL_ERROR);
            }

            String prefix = fElementQName.prefix != null
                    ? fElementQName.prefix : XMLSymbols.EMPTY_STRING;
            fElementQName.uri = fNamespaceContext.getURI(prefix);
            fCurrentElement.uri = fElementQName.uri;

            if (fElementQName.prefix == null && fElementQName.uri != null) {
                fElementQName.prefix = XMLSymbols.EMPTY_STRING;
            }
            if (fElementQName.prefix != null && fElementQName.uri == null) {
                fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                        "ElementPrefixUnbound",
                        new Object[]{fElementQName.prefix, fElementQName.rawname},
                        XMLErrorReporter.SEVERITY_FATAL_ERROR);
            }

            int length = fAttributes.getLength();
            for (int i = 0; i < length; i++) {
                fAttributes.getName(i, fAttributeQName);

                String aprefix = fAttributeQName.prefix != null
                        ? fAttributeQName.prefix : XMLSymbols.EMPTY_STRING;
                String uri = fNamespaceContext.getURI(aprefix);
                if (fAttributeQName.uri != null && fAttributeQName.uri == uri) {
                    continue;
                }
                if (aprefix != XMLSymbols.EMPTY_STRING) {
                    fAttributeQName.uri = uri;
                    if (uri == null) {
                        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                                "AttributePrefixUnbound",
                                new Object[]{fElementQName.rawname,fAttributeQName.rawname,aprefix},
                                XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    }
                    fAttributes.setURI(i, uri);
                }
            }

            if (length > 1) {
                QName name = fAttributes.checkDuplicatesNS();
                if (name != null) {
                    if (name.uri != null) {
                        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                                "AttributeNSNotUnique",
                                new Object[]{fElementQName.rawname, name.localpart, name.uri},
                                XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    } else {
                        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                                "AttributeNotUnique",
                                new Object[]{fElementQName.rawname, name.rawname},
                                XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    }
                }
            }
        }


        if (fEmptyElement) {
            fMarkupDepth--;

            if (fMarkupDepth < fEntityStack[fEntityDepth - 1]) {
                reportFatalError("ElementEntityMismatch",
                        new Object[]{fCurrentElement.rawname});
            }
            if (fDocumentHandler != null) {
                if(DEBUG)
                    System.out.println("emptyElement = " + fElementQName);

                fDocumentHandler.emptyElement(fElementQName, fAttributes, null);
            }

            fScanEndElement = true;

            fElementStack.popElement();

        } else {

            if(dtdGrammarUtil != null)
                dtdGrammarUtil.startElement(fElementQName,fAttributes);
            if(fDocumentHandler != null){
                if(DEBUG)
                    System.out.println("startElement = " + fElementQName);
                fDocumentHandler.startElement(fElementQName, fAttributes, null);
            }
        }


        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +"<<< scanStartElement(): "+fEmptyElement);
        return fEmptyElement;

    } 



    /**
     * Scans an attribute.
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
    protected void scanAttribute(XMLAttributesImpl attributes)
    throws IOException, XNIException {
        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +">>> scanAttribute()");

        fEntityScanner.scanQName(fAttributeQName, NameType.ATTRIBUTENAME);

        fEntityScanner.skipSpaces();
        if (!fEntityScanner.skipChar('=', NameType.ATTRIBUTE)) {
            reportFatalError("EqRequiredInAttribute",
                    new Object[]{fCurrentElement.rawname,fAttributeQName.rawname});
        }
        fEntityScanner.skipSpaces();

        int attrIndex = 0 ;


        boolean isVC =  fHasExternalDTD && !fStandalone;


        XMLString tmpStr = getString();

        /**
         * Determine whether this is a namespace declaration that will be subject
         * to the name limit check in the scanAttributeValue operation.
         * Namespace declaration format: xmlns="..." or xmlns:prefix="..."
         * Note that prefix:xmlns="..." isn't a namespace.
         */
        String localpart = fAttributeQName.localpart;
        String prefix = fAttributeQName.prefix != null
                ? fAttributeQName.prefix : XMLSymbols.EMPTY_STRING;
        boolean isNSDecl = fBindNamespaces & (prefix == XMLSymbols.PREFIX_XMLNS ||
                    prefix == XMLSymbols.EMPTY_STRING && localpart == XMLSymbols.PREFIX_XMLNS);

        scanAttributeValue(tmpStr, fTempString2, fAttributeQName.rawname, attributes,
                attrIndex, isVC, fCurrentElement.rawname, isNSDecl);

        String value = null;

        if (fBindNamespaces) {
            if (isNSDecl) {
                if (fXMLNameLimit > 0 && tmpStr.length > fXMLNameLimit) {
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                            "MaxXMLNameLimit",
                            new Object[]{new String(tmpStr.ch,tmpStr.offset,tmpStr.length),
                            tmpStr.length, fXMLNameLimit,
                            fSecurityManager.getStateLiteral(XMLSecurityManager.Limit.MAX_NAME_LIMIT)},
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }
                String uri = fSymbolTable.addSymbol(tmpStr.ch,tmpStr.offset,tmpStr.length);
                value = uri;
                if (prefix == XMLSymbols.PREFIX_XMLNS && localpart == XMLSymbols.PREFIX_XMLNS) {
                    fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                            "CantBindXMLNS",
                            new Object[]{fAttributeQName},
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }

                if (uri == NamespaceContext.XMLNS_URI) {
                    fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                            "CantBindXMLNS",
                            new Object[]{fAttributeQName},
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }

                if (localpart == XMLSymbols.PREFIX_XML) {
                    if (uri != NamespaceContext.XML_URI) {
                        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                                "CantBindXML",
                                new Object[]{fAttributeQName},
                                XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    }
                }
                else {
                    if (uri ==NamespaceContext.XML_URI) {
                        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                                "CantBindXML",
                                new Object[]{fAttributeQName},
                                XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    }
                }
                prefix = localpart != XMLSymbols.PREFIX_XMLNS ? localpart : XMLSymbols.EMPTY_STRING;
                if(prefix == XMLSymbols.EMPTY_STRING && localpart == XMLSymbols.PREFIX_XMLNS){
                    fAttributeQName.prefix = XMLSymbols.PREFIX_XMLNS;
                }
                if (uri == XMLSymbols.EMPTY_STRING && localpart != XMLSymbols.PREFIX_XMLNS) {
                    fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                            "EmptyPrefixedAttName",
                            new Object[]{fAttributeQName},
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }

                if (((com.sun.org.apache.xerces.internal.util.NamespaceSupport) fNamespaceContext).containsPrefixInCurrentContext(prefix)) {
                    reportFatalError("AttributeNotUnique",
                            new Object[]{fCurrentElement.rawname,
                            fAttributeQName.rawname});
                }

                boolean declared = fNamespaceContext.declarePrefix(prefix, uri.length() != 0 ? uri : null);

                if (!declared) { 
                    if (fXmlnsDeclared) {
                        reportFatalError("AttributeNotUnique",
                                new Object[]{fCurrentElement.rawname,
                                fAttributeQName.rawname});
                    }

                    fXmlnsDeclared = true;
                }


                if(fNotAddNSDeclAsAttribute){
                    return ;
                }
            }
        }

        if (fBindNamespaces) {
            attrIndex = attributes.getLength();
            attributes.addAttributeNS(fAttributeQName, XMLSymbols.fCDATASymbol, null);
        } else {
            int oldLen = attributes.getLength();
            attrIndex = attributes.addAttribute(fAttributeQName, XMLSymbols.fCDATASymbol, null);

            if (oldLen == attributes.getLength()) {
                reportFatalError("AttributeNotUnique",
                        new Object[]{fCurrentElement.rawname,
                                fAttributeQName.rawname});
            }
        }

        attributes.setValue(attrIndex, value,tmpStr);
        attributes.setSpecified(attrIndex, true);

        if (fAttributeQName.prefix != null) {
            attributes.setURI(attrIndex, fNamespaceContext.getURI(fAttributeQName.prefix));
        }

        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +"<<< scanAttribute()");
    } 





    /** Creates a content driver. */
    protected Driver createContentDriver() {
        return new NSContentDriver();
    } 

    /**
     * Driver to handle content scanning.
     */
    protected final class NSContentDriver
            extends ContentDriver {
        /**
         * Scan for root element hook. This method is a hook for
         * subclasses to add code that handles scanning for the root
         * element. This method will also attempt to remove DTD validator
         * from the pipeline, if there is no DTD grammar. If DTD validator
         * is no longer in the pipeline bind namespaces in the scanner.
         *
         *
         * @return True if the caller should stop and return true which
         *          allows the scanner to switch to a new scanning
         *          driver. A return value of false indicates that
         *          the content driver should continue as normal.
         */
        protected boolean scanRootElementHook()
        throws IOException, XNIException {

            reconfigurePipeline();
            if (scanStartElement()) {
                setScannerState(SCANNER_STATE_TRAILING_MISC);
                setDriver(fTrailingMiscDriver);
                return true;
            }
            return false;

        } 

        /**
         * Re-configures pipeline by removing the DTD validator
         * if no DTD grammar exists. If no validator exists in the
         * pipeline or there is no DTD grammar, namespace binding
         * is performed by the scanner in the enclosing class.
         */
        private void reconfigurePipeline() {
            if (fNamespaces && fDTDValidator == null) {
                fBindNamespaces = true;
            }
            else if (fNamespaces && !fDTDValidator.hasGrammar() ) {
                fBindNamespaces = true;
                fPerformValidation = fDTDValidator.validate();
                XMLDocumentSource source = fDTDValidator.getDocumentSource();
                XMLDocumentHandler handler = fDTDValidator.getDocumentHandler();
                source.setDocumentHandler(handler);
                if (handler != null)
                    handler.setDocumentSource(source);
                fDTDValidator.setDocumentSource(null);
                fDTDValidator.setDocumentHandler(null);
            }
        } 
    }

} 
