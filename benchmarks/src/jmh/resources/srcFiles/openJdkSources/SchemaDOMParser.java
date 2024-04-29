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

package com.sun.org.apache.xerces.internal.impl.xs.opti;

import java.io.IOException;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSMessageFormatter;
import com.sun.org.apache.xerces.internal.util.XMLAttributesImpl;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParserConfiguration;
import org.w3c.dom.Document;

/**
 * @xerces.internal
 *
 * @author Rahul Srivastava, Sun Microsystems Inc.
 * @author Sandy Gao, IBM
 *
 */
public class SchemaDOMParser extends DefaultXMLDocumentHandler {


    /** Property identifier: error reporter. */
    public static final String ERROR_REPORTER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Feature identifier: generate synthetic annotations. */
    public static final String GENERATE_SYNTHETIC_ANNOTATION =
        Constants.XERCES_FEATURE_PREFIX + Constants.GENERATE_SYNTHETIC_ANNOTATIONS_FEATURE;

    protected XMLLocator   fLocator;

    protected NamespaceContext fNamespaceContext = null;

    SchemaDOM schemaDOM;

    XMLParserConfiguration config;


    /** Default constructor. */
    public SchemaDOMParser(XMLParserConfiguration config) {
        this.config = config;
        config.setDocumentHandler(this);
        config.setDTDHandler(this);
        config.setDTDContentModelHandler(this);
    }

    private ElementImpl fCurrentAnnotationElement;
    private int fAnnotationDepth = -1;
    private int fInnerAnnotationDepth = -1;
    private int fDepth = -1;
    XMLErrorReporter fErrorReporter;

    private boolean fGenerateSyntheticAnnotation = false;
    private BooleanStack fHasNonSchemaAttributes = new BooleanStack();
    private BooleanStack fSawAnnotation = new BooleanStack();
    private XMLAttributes fEmptyAttr = new XMLAttributesImpl();


    public void startDocument(XMLLocator locator, String encoding,
            NamespaceContext namespaceContext, Augmentations augs)
    throws XNIException {
        fErrorReporter = (XMLErrorReporter)config.getProperty(ERROR_REPORTER);
        fGenerateSyntheticAnnotation = config.getFeature(GENERATE_SYNTHETIC_ANNOTATION);
        fHasNonSchemaAttributes.clear();
        fSawAnnotation.clear();
        schemaDOM = new SchemaDOM();
        fCurrentAnnotationElement = null;
        fAnnotationDepth = -1;
        fInnerAnnotationDepth = -1;
        fDepth = -1;
        fLocator = locator;
        fNamespaceContext = namespaceContext;
        schemaDOM.setDocumentURI(locator.getExpandedSystemId());
    } 

    /**
     * The end of the document.
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endDocument(Augmentations augs) throws XNIException {
    } 


    /**
     * A comment.
     *
     * @param text   The text in the comment.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by application to signal an error.
     */
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        if(fAnnotationDepth > -1) {
            schemaDOM.comment(text);
        }
    }

    /**
     * A processing instruction. Processing instructions consist of a
     * target name and, optionally, text data. The data is only meaningful
     * to the application.
     * <p>
     * Typically, a processing instruction's data will contain a series
     * of pseudo-attributes. These pseudo-attributes follow the form of
     * element attributes but are <strong>not</strong> parsed or presented
     * to the application as anything other than text. The application is
     * responsible for parsing the data.
     *
     * @param target The target.
     * @param data   The data or null if none specified.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void processingInstruction(String target, XMLString data, Augmentations augs)
    throws XNIException {
        if (fAnnotationDepth > -1) {
            schemaDOM.processingInstruction(target, data);
        }
    }

    /**
     * Character content.
     *
     * @param text   The content.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        if (fInnerAnnotationDepth == -1 ) {
            for (int i=text.offset; i<text.offset+text.length; i++) {
                if (!XMLChar.isSpace(text.ch[i])) {
                    String txt = new String(text.ch, i, text.length+text.offset-i);
                    fErrorReporter.reportError(fLocator,
                            XSMessageFormatter.SCHEMA_DOMAIN,
                            "s4s-elt-character",
                            new Object[]{txt},
                            XMLErrorReporter.SEVERITY_ERROR);
                    break;
                }
            }
        }
        else {
            schemaDOM.characters(text);
        }

    }


    /**
     * The start of an element.
     *
     * @param element    The name of the element.
     * @param attributes The element attributes.
     * @param augs       Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs)
    throws XNIException {

        fDepth++;
        if (fAnnotationDepth == -1) {
            if (element.uri == SchemaSymbols.URI_SCHEMAFORSCHEMA &&
                    element.localpart == SchemaSymbols.ELT_ANNOTATION) {
                if (fGenerateSyntheticAnnotation) {
                    if (fSawAnnotation.size() > 0) {
                        fSawAnnotation.pop();
                    }
                    fSawAnnotation.push(true);
                }
                fAnnotationDepth = fDepth;
                schemaDOM.startAnnotation(element, attributes, fNamespaceContext);
                fCurrentAnnotationElement = schemaDOM.startElement(element, attributes,
                        fLocator.getLineNumber(),
                        fLocator.getColumnNumber(),
                        fLocator.getCharacterOffset());
                return;
            }
            else if (element.uri == SchemaSymbols.URI_SCHEMAFORSCHEMA && fGenerateSyntheticAnnotation) {
                fSawAnnotation.push(false);
                fHasNonSchemaAttributes.push(hasNonSchemaAttributes(element, attributes));
            }
        }
        else if (fDepth == fAnnotationDepth + 1) {
            fInnerAnnotationDepth = fDepth;
            schemaDOM.startAnnotationElement(element, attributes);
        }
        else {
            schemaDOM.startAnnotationElement(element, attributes);
            return;
        }
        schemaDOM.startElement(element, attributes,
                fLocator.getLineNumber(),
                fLocator.getColumnNumber(),
                fLocator.getCharacterOffset());

    }


    /**
     * An empty element.
     *
     * @param element    The name of the element.
     * @param attributes The element attributes.
     * @param augs       Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs)
    throws XNIException {

        if (fGenerateSyntheticAnnotation && fAnnotationDepth == -1 &&
                element.uri == SchemaSymbols.URI_SCHEMAFORSCHEMA && element.localpart != SchemaSymbols.ELT_ANNOTATION && hasNonSchemaAttributes(element, attributes)) {

            schemaDOM.startElement(element, attributes,
                    fLocator.getLineNumber(),
                    fLocator.getColumnNumber(),
                    fLocator.getCharacterOffset());

            attributes.removeAllAttributes();
            String schemaPrefix = fNamespaceContext.getPrefix(SchemaSymbols.URI_SCHEMAFORSCHEMA);
            final String annRawName = (schemaPrefix.length() == 0) ? SchemaSymbols.ELT_ANNOTATION : (schemaPrefix + ':' + SchemaSymbols.ELT_ANNOTATION);
            schemaDOM.startAnnotation(annRawName, attributes, fNamespaceContext);
            final String elemRawName = (schemaPrefix.length() == 0) ? SchemaSymbols.ELT_DOCUMENTATION : (schemaPrefix + ':' + SchemaSymbols.ELT_DOCUMENTATION);
            schemaDOM.startAnnotationElement(elemRawName, attributes);
            schemaDOM.charactersRaw("SYNTHETIC_ANNOTATION");
            schemaDOM.endSyntheticAnnotationElement(elemRawName, false);
            schemaDOM.endSyntheticAnnotationElement(annRawName, true);

            schemaDOM.endElement();

            return;
        }
        if (fAnnotationDepth == -1) {
            if (element.uri == SchemaSymbols.URI_SCHEMAFORSCHEMA &&
                    element.localpart == SchemaSymbols.ELT_ANNOTATION) {
                schemaDOM.startAnnotation(element, attributes, fNamespaceContext);
            }
        }
        else {
            schemaDOM.startAnnotationElement(element, attributes);
        }

        ElementImpl newElem = schemaDOM.emptyElement(element, attributes,
                fLocator.getLineNumber(),
                fLocator.getColumnNumber(),
                fLocator.getCharacterOffset());

        if (fAnnotationDepth == -1) {
            if (element.uri == SchemaSymbols.URI_SCHEMAFORSCHEMA &&
                    element.localpart == SchemaSymbols.ELT_ANNOTATION) {
                schemaDOM.endAnnotation(element, newElem);
            }
        }
        else {
            schemaDOM.endAnnotationElement(element);
        }
    }


    /**
     * The end of an element.
     *
     * @param element The name of the element.
     * @param augs    Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void endElement(QName element, Augmentations augs) throws XNIException {

        if(fAnnotationDepth > -1) {
            if (fInnerAnnotationDepth == fDepth) {
                fInnerAnnotationDepth = -1;
                schemaDOM.endAnnotationElement(element);
                schemaDOM.endElement();
            } else if (fAnnotationDepth == fDepth) {
                fAnnotationDepth = -1;
                schemaDOM.endAnnotation(element, fCurrentAnnotationElement);
                schemaDOM.endElement();
            } else { 
                schemaDOM.endAnnotationElement(element);
            }
        } else { 
            if(element.uri == SchemaSymbols.URI_SCHEMAFORSCHEMA && fGenerateSyntheticAnnotation) {
                boolean value = fHasNonSchemaAttributes.pop();
                boolean sawann = fSawAnnotation.pop();
                if (value && !sawann) {
                    String schemaPrefix = fNamespaceContext.getPrefix(SchemaSymbols.URI_SCHEMAFORSCHEMA);
                    final String annRawName = (schemaPrefix.length() == 0) ? SchemaSymbols.ELT_ANNOTATION : (schemaPrefix + ':' + SchemaSymbols.ELT_ANNOTATION);
                    schemaDOM.startAnnotation(annRawName, fEmptyAttr, fNamespaceContext);
                    final String elemRawName = (schemaPrefix.length() == 0) ? SchemaSymbols.ELT_DOCUMENTATION : (schemaPrefix + ':' + SchemaSymbols.ELT_DOCUMENTATION);
                    schemaDOM.startAnnotationElement(elemRawName, fEmptyAttr);
                    schemaDOM.charactersRaw("SYNTHETIC_ANNOTATION");
                    schemaDOM.endSyntheticAnnotationElement(elemRawName, false);
                    schemaDOM.endSyntheticAnnotationElement(annRawName, true);
                }
            }
            schemaDOM.endElement();
        }
        fDepth--;

    }

    /**
     * @param attributes
     * @return
     */
    private boolean hasNonSchemaAttributes(QName element, XMLAttributes attributes) {
        final int length = attributes.getLength();
        for (int i = 0; i < length; ++i) {
            String uri = attributes.getURI(i);
            if (uri != null && uri != SchemaSymbols.URI_SCHEMAFORSCHEMA &&
                    uri != NamespaceContext.XMLNS_URI &&
                    !(uri == NamespaceContext.XML_URI &&
                            attributes.getQName(i) == SchemaSymbols.ATT_XML_LANG && element.localpart == SchemaSymbols.ELT_SCHEMA)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ignorable whitespace. For this method to be called, the document
     * source must have some way of determining that the text containing
     * only whitespace characters should be considered ignorable. For
     * example, the validator can determine if a length of whitespace
     * characters in the document are ignorable based on the element
     * content model.
     *
     * @param text   The ignorable whitespace.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        if (fAnnotationDepth != -1 ) {
            schemaDOM.characters(text);
        }
    }

    /**
     * The start of a CDATA section.
     *
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void startCDATA(Augmentations augs) throws XNIException {
        if (fAnnotationDepth != -1) {
            schemaDOM.startAnnotationCDATA();
        }
    }

    /**
     * The end of a CDATA section.
     *
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void endCDATA(Augmentations augs) throws XNIException {
        if (fAnnotationDepth != -1) {
            schemaDOM.endAnnotationCDATA();
        }
    }



    /**
     * Returns the DOM document object.
     */
    public Document getDocument() {
        return schemaDOM;
    }

    /**
     * Delegates to SchemaParsingConfig.setFeature
     * @param featureId
     * @param state
     */
    public void setFeature(String featureId, boolean state){
        config.setFeature(featureId, state);
    }

    /**
     * Delegates to SchemaParsingConfig.getFeature
     * @param featureId
     * @return boolean
     */
    public boolean getFeature(String featureId){
        return config.getFeature(featureId);
    }

    /**
     * Delegates to SchemaParsingConfig.setProperty.
     * @param propertyId
     * @param value
     */
    public void setProperty(String propertyId, Object value){
        config.setProperty(propertyId, value);
    }

    /**
     * Delegates to SchemaParsingConfig.getProperty.
     * @param propertyId
     * @return Object
     */
    public Object getProperty(String propertyId){
        return config.getProperty(propertyId);
    }

    /**
     * Delegates to SchemaParsingConfig.setEntityResolver.
     * @param er XMLEntityResolver
     */
    public void setEntityResolver(XMLEntityResolver er) {
        config.setEntityResolver(er);
    }

    /**
     * Delegates parsing to SchemaParsingConfig
     *
     * @param inputSource
     * @throws IOException
     */
    public void parse(XMLInputSource inputSource) throws IOException {
        config.parse(inputSource);
    }

    /**
     * Reset SchemaParsingConfig
     */
    public void reset() {
        ((SchemaParsingConfig)config).reset();
    }

    /**
     * ResetNodePool on SchemaParsingConfig
     */
    public void resetNodePool() {
        ((SchemaParsingConfig)config).resetNodePool();
    }

    /**
     * A simple boolean based stack.
     *
     * @xerces.internal
     */
    private static final class BooleanStack {


        /** Stack depth. */
        private int fDepth;

        /** Stack data. */
        private boolean[] fData;


        public BooleanStack () {}


        /** Returns the size of the stack. */
        public int size() {
            return fDepth;
        }

        /** Pushes a value onto the stack. */
        public void push(boolean value) {
            ensureCapacity(fDepth + 1);
            fData[fDepth++] = value;
        }

        /** Pops a value off of the stack. */
        public boolean pop() {
            return fData[--fDepth];
        }

        /** Clears the stack. */
        public void clear() {
            fDepth = 0;
        }


        /** Ensures capacity. */
        private void ensureCapacity(int size) {
            if (fData == null) {
                fData = new boolean[32];
            }
            else if (fData.length <= size) {
                boolean[] newdata = new boolean[fData.length * 2];
                System.arraycopy(fData, 0, newdata, 0, fData.length);
                fData = newdata;
            }
        }
    }
}
