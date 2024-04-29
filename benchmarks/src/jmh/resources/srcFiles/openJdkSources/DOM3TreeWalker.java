/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xml.internal.serializer.dom3;

import com.sun.org.apache.xerces.internal.util.XML11Char;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import com.sun.org.apache.xml.internal.serializer.utils.MsgKey;
import com.sun.org.apache.xml.internal.serializer.utils.Utils;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import jdk.xml.internal.JdkXmlUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.ls.LSSerializerFilter;
import org.w3c.dom.traversal.NodeFilter;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.LocatorImpl;

/**
 * Built on org.apache.xml.serializer.TreeWalker and adds functionality to
 * traverse and serialize a DOM Node (Level 2 or Level 3) as specified in
 * the DOM Level 3 LS Recommedation by evaluating and applying DOMConfiguration
 * parameters and filters if any during serialization.
 *
 * @xsl.usage internal
 * @LastModified: July 2021
 */
final class DOM3TreeWalker {

    /**
     * The SerializationHandler, it extends ContentHandler and when
     * this class is instantiated via the constructor provided, a
     * SerializationHandler object is passed to it.
     */
    private SerializationHandler fSerializer = null;

    /** We do not need DOM2Helper since DOM Level 3 LS applies to DOM Level 2 or newer */

    /** Locator object for this TreeWalker          */
    private LocatorImpl fLocator = new LocatorImpl();

    /** ErrorHandler */
    private DOMErrorHandler fErrorHandler = null;

    /** LSSerializerFilter */
    private LSSerializerFilter fFilter = null;

    /** If the serializer is an instance of a LexicalHandler */
    private LexicalHandler fLexicalHandler = null;

    private int fWhatToShowFilter;

    /** New Line character to use in serialization */
    private String fNewLine = null;

    /** DOMConfiguration Properties */
    private Properties fDOMConfigProperties = null;

    /** Keeps track if we are in an entity reference when entities=true */
    private boolean fInEntityRef = false;

    /** Stores the version of the XML document to be serialize */
    private String fXMLVersion = null;

    /** XML Version, default 1.0 */
    private boolean fIsXMLVersion11 = false;

    /** Is the Node a Level 3 DOM node */
    private boolean fIsLevel3DOM = false;

    /** DOM Configuration Parameters */
    private int fFeatures = 0;

    /** Flag indicating whether following text to be processed is raw text          */
    boolean fNextIsRaw = false;

    private static final String XMLNS_URI = "http:

    private static final String XMLNS_PREFIX = "xmlns";

    private static final String XML_URI = "http:

    private static final String XML_PREFIX = "xml";

    /** stores namespaces in scope */
    protected NamespaceSupport fNSBinder;

    /** stores all namespace bindings on the current element */
    protected NamespaceSupport fLocalNSBinder;

    /** stores the current element depth */
    private int fElementDepth = 0;

    private final static int CANONICAL = 0x1 << 0;

    private final static int CDATA = 0x1 << 1;

    private final static int CHARNORMALIZE = 0x1 << 2;

    private final static int COMMENTS = 0x1 << 3;

    private final static int DTNORMALIZE = 0x1 << 4;

    private final static int ELEM_CONTENT_WHITESPACE = 0x1 << 5;

    private final static int ENTITIES = 0x1 << 6;

    private final static int INFOSET = 0x1 << 7;

    private final static int NAMESPACES = 0x1 << 8;

    private final static int NAMESPACEDECLS = 0x1 << 9;

    private final static int NORMALIZECHARS = 0x1 << 10;

    private final static int SPLITCDATA = 0x1 << 11;

    private final static int VALIDATE = 0x1 << 12;

    private final static int SCHEMAVALIDATE = 0x1 << 13;

    private final static int WELLFORMED = 0x1 << 14;

    private final static int DISCARDDEFAULT = 0x1 << 15;

    private final static int PRETTY_PRINT = 0x1 << 16;

    private final static int IGNORE_CHAR_DENORMALIZE = 0x1 << 17;

    private final static int XMLDECL = 0x1 << 18;

    /**
     * Constructor.
     * @param   contentHandler serialHandler The implemention of the SerializationHandler interface
     */
    DOM3TreeWalker(
        SerializationHandler serialHandler,
        DOMErrorHandler errHandler,
        LSSerializerFilter filter,
        String newLine) {
        fSerializer = serialHandler;
        fErrorHandler = errHandler;
        fFilter = filter;
        fLexicalHandler = null;
        fNewLine = newLine;

        fNSBinder = new NamespaceSupport();
        fLocalNSBinder = new NamespaceSupport();

        fDOMConfigProperties = fSerializer.getOutputFormat();
        fSerializer.setDocumentLocator(fLocator);
        initProperties(fDOMConfigProperties);
    }

    /**
     * Perform a pre-order traversal non-recursive style.
     *
     * Note that TreeWalker assumes that the subtree is intended to represent
     * a complete (though not necessarily well-formed) document and, during a
     * traversal, startDocument and endDocument will always be issued to the
     * SAX listener.
     *
     * @param pos Node in the tree where to start traversal
     *
     * @throws TransformerException
     */
    public void traverse(Node pos) throws org.xml.sax.SAXException {
        this.fSerializer.startDocument();

        if (pos.getNodeType() != Node.DOCUMENT_NODE) {
            Document ownerDoc = pos.getOwnerDocument();
            if (ownerDoc != null
                && ownerDoc.getImplementation().hasFeature("Core", "3.0")) {
                fIsLevel3DOM = true;
            }
        } else {
            if (((Document) pos)
                .getImplementation()
                .hasFeature("Core", "3.0")) {
                fIsLevel3DOM = true;
            }
        }

        if (fSerializer instanceof LexicalHandler) {
            fLexicalHandler = ((LexicalHandler) this.fSerializer);
        }

        if (fFilter != null)
            fWhatToShowFilter = fFilter.getWhatToShow();

        Node top = pos;

        while (null != pos) {
            startNode(pos);

            Node nextNode = null;

            nextNode = pos.getFirstChild();

            while (null == nextNode) {
                endNode(pos);

                if (top.equals(pos))
                    break;

                nextNode = pos.getNextSibling();

                if (null == nextNode) {
                    pos = pos.getParentNode();

                    if ((null == pos) || (top.equals(pos))) {
                        if (null != pos)
                            endNode(pos);

                        nextNode = null;

                        break;
                    }
                }
            }

            pos = nextNode;
        }
        this.fSerializer.endDocument();
    }

    /**
     * Perform a pre-order traversal non-recursive style.

     * Note that TreeWalker assumes that the subtree is intended to represent
     * a complete (though not necessarily well-formed) document and, during a
     * traversal, startDocument and endDocument will always be issued to the
     * SAX listener.
     *
     * @param pos Node in the tree where to start traversal
     * @param top Node in the tree where to end traversal
     *
     * @throws TransformerException
     */
    public void traverse(Node pos, Node top) throws org.xml.sax.SAXException {

        this.fSerializer.startDocument();

        if (pos.getNodeType() != Node.DOCUMENT_NODE) {
            Document ownerDoc = pos.getOwnerDocument();
            if (ownerDoc != null
                && ownerDoc.getImplementation().hasFeature("Core", "3.0")) {
                fIsLevel3DOM = true;
            }
        } else {
            if (((Document) pos)
                .getImplementation()
                .hasFeature("Core", "3.0")) {
                fIsLevel3DOM = true;
            }
        }

        if (fSerializer instanceof LexicalHandler) {
            fLexicalHandler = ((LexicalHandler) this.fSerializer);
        }

        if (fFilter != null)
            fWhatToShowFilter = fFilter.getWhatToShow();

        while (null != pos) {
            startNode(pos);

            Node nextNode = null;

            nextNode = pos.getFirstChild();

            while (null == nextNode) {
                endNode(pos);

                if ((null != top) && top.equals(pos))
                    break;

                nextNode = pos.getNextSibling();

                if (null == nextNode) {
                    pos = pos.getParentNode();

                    if ((null == pos) || ((null != top) && top.equals(pos))) {
                        nextNode = null;

                        break;
                    }
                }
            }

            pos = nextNode;
        }
        this.fSerializer.endDocument();
    }

    /**
     * Optimized dispatch of characters.
     */
    private final void dispatachChars(Node node)
        throws org.xml.sax.SAXException {
        if (fSerializer != null) {
            String data = ((Text) node).getData();
            this.fSerializer.characters(data.toCharArray(), 0, data.length());
        }
    }

    /**
     * Start processing given node
     *
     * @param node Node to process
     *
     * @throws org.xml.sax.SAXException
     */
    protected void startNode(Node node) throws org.xml.sax.SAXException {
        if (node instanceof Locator) {
            Locator loc = (Locator) node;
            fLocator.setColumnNumber(loc.getColumnNumber());
            fLocator.setLineNumber(loc.getLineNumber());
            fLocator.setPublicId(loc.getPublicId());
            fLocator.setSystemId(loc.getSystemId());
        } else {
            fLocator.setColumnNumber(0);
            fLocator.setLineNumber(0);
        }

        switch (node.getNodeType()) {
            case Node.DOCUMENT_TYPE_NODE :
                serializeDocType((DocumentType) node, true);
                break;
            case Node.COMMENT_NODE :
                serializeComment((Comment) node);
                break;
            case Node.DOCUMENT_FRAGMENT_NODE :
                break;
            case Node.DOCUMENT_NODE :
                break;
            case Node.ELEMENT_NODE :
                serializeElement((Element) node, true);
                break;
            case Node.PROCESSING_INSTRUCTION_NODE :
                serializePI((ProcessingInstruction) node);
                break;
            case Node.CDATA_SECTION_NODE :
                serializeCDATASection((CDATASection) node);
                break;
            case Node.TEXT_NODE :
                serializeText((Text) node);
                break;
            case Node.ENTITY_REFERENCE_NODE :
                serializeEntityReference((EntityReference) node, true);
                break;
            default :
                }
    }

    /**
     * End processing of given node
     *
     *
     * @param node Node we just finished processing
     *
     * @throws org.xml.sax.SAXException
     */
    protected void endNode(Node node) throws org.xml.sax.SAXException {

        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE :
                break;
            case Node.DOCUMENT_TYPE_NODE :
                serializeDocType((DocumentType) node, false);
                break;
            case Node.ELEMENT_NODE :
                serializeElement((Element) node, false);
                break;
            case Node.CDATA_SECTION_NODE :
                break;
            case Node.ENTITY_REFERENCE_NODE :
                serializeEntityReference((EntityReference) node, false);
                break;
            default :
                }
    }

    /**
     * Applies a filter on the node to serialize
     *
     * @param node The Node to serialize
     * @return True if the node is to be serialized else false if the node
     *         is to be rejected or skipped.
     */
    protected boolean applyFilter(Node node, int nodeType) {
        if (fFilter != null && (fWhatToShowFilter & nodeType) != 0) {

            short code = fFilter.acceptNode(node);
            switch (code) {
                case NodeFilter.FILTER_REJECT :
                case NodeFilter.FILTER_SKIP :
                    return false; 
                default : 
            }
        }
        return true;
    }

    /**
     * Serializes a Document Type Node.
     *
     * @param node The Docuemnt Type Node to serialize
     * @param bStart Invoked at the start or end of node.  Default true.
     */
    protected void serializeDocType(DocumentType node, boolean bStart)
        throws SAXException {
        String docTypeName = node.getNodeName();
        String publicId = node.getPublicId();
        String systemId = node.getSystemId();
        String internalSubset = node.getInternalSubset();


        if (internalSubset != null && !"".equals(internalSubset)) {

            if (bStart) {
                try {
                    Writer writer = fSerializer.getWriter();
                    StringBuilder dtd = new StringBuilder();

                    dtd.append("<!DOCTYPE ");
                    dtd.append(docTypeName);
                    dtd.append(JdkXmlUtils.getDTDExternalDecl(publicId, systemId));
                    dtd.append(" [ ");

                    dtd.append(fNewLine);
                    dtd.append(internalSubset);
                    dtd.append("]>");
                    dtd.append(fNewLine);

                    writer.write(dtd.toString());
                    writer.flush();

                } catch (IOException e) {
                    throw new SAXException(Utils.messages.createMessage(
                            MsgKey.ER_WRITING_INTERNAL_SUBSET, null), e);
                }
            } 

        } else {

            if (bStart) {
                if (fLexicalHandler != null) {
                    fLexicalHandler.startDTD(docTypeName, publicId, systemId);
                }
            } else {
                if (fLexicalHandler != null) {
                    fLexicalHandler.endDTD();
                }
            }
        }
    }

    /**
     * Serializes a Comment Node.
     *
     * @param node The Comment Node to serialize
     */
    protected void serializeComment(Comment node) throws SAXException {
        if ((fFeatures & COMMENTS) != 0) {
            String data = node.getData();

            if ((fFeatures & WELLFORMED) != 0) {
                isCommentWellFormed(data);
            }

            if (fLexicalHandler != null) {
                if (!applyFilter(node, NodeFilter.SHOW_COMMENT)) {
                    return;
                }

                fLexicalHandler.comment(data.toCharArray(), 0, data.length());
            }
        }
    }

    /**
     * Serializes an Element Node.
     *
     * @param node The Element Node to serialize
     * @param bStart Invoked at the start or end of node.
     */
    protected void serializeElement(Element node, boolean bStart)
        throws SAXException {
        if (bStart) {
            fElementDepth++;


            if ((fFeatures & WELLFORMED) != 0) {
                isElementWellFormed(node);
            }

            if (!applyFilter(node, NodeFilter.SHOW_ELEMENT)) {
                return;
            }

            if ((fFeatures & NAMESPACES) != 0) {
                fNSBinder.pushContext();
                fLocalNSBinder.reset();

                recordLocalNSDecl(node);
                fixupElementNS(node);
            }

            fSerializer.startElement(
                        node.getNamespaceURI(),
                    node.getLocalName(),
                    node.getNodeName());

            serializeAttList(node);

        } else {
                fElementDepth--;

            if (!applyFilter(node, NodeFilter.SHOW_ELEMENT)) {
                return;
            }

            this.fSerializer.endElement(
                node.getNamespaceURI(),
                node.getLocalName(),
                node.getNodeName());

            if ((fFeatures & NAMESPACES) != 0 ) {
                    fNSBinder.popContext();
            }

        }
    }

    /**
     * Serializes the Attr Nodes of an Element.
     *
     * @param node The OwnerElement whose Attr Nodes are to be serialized.
     */
    protected void serializeAttList(Element node) throws SAXException {
        NamedNodeMap atts = node.getAttributes();
        int nAttrs = atts.getLength();

        for (int i = 0; i < nAttrs; i++) {
            Node attr = atts.item(i);

            String localName = attr.getLocalName();
            String attrName = attr.getNodeName();
            String attrPrefix = attr.getPrefix() == null ? "" : attr.getPrefix();
            String attrValue = attr.getNodeValue();

            String type = null;
            if (fIsLevel3DOM) {
                type = ((Attr) attr).getSchemaTypeInfo().getTypeName();
            }
            type = type == null ? "CDATA" : type;

            String attrNS = attr.getNamespaceURI();
            if (attrNS !=null && attrNS.length() == 0) {
                attrNS=null;
                attrName=attr.getLocalName();
            }

            boolean isSpecified = ((Attr) attr).getSpecified();
            boolean addAttr = true;
            boolean applyFilter = false;
            boolean xmlnsAttr =
                attrName.equals("xmlns") || attrName.startsWith("xmlns:");

            if ((fFeatures & WELLFORMED) != 0) {
                isAttributeWellFormed(attr);
            }

            if ((fFeatures & NAMESPACES) != 0 && !xmlnsAttr) {

                        if (attrNS != null) {
                                attrPrefix = attrPrefix == null ? "" : attrPrefix;

                                String declAttrPrefix = fNSBinder.getPrefix(attrNS);
                                String declAttrNS = fNSBinder.getURI(attrPrefix);

                                if ("".equals(attrPrefix) || "".equals(declAttrPrefix)
                                                || !attrPrefix.equals(declAttrPrefix)) {

                                        if (declAttrPrefix != null && !"".equals(declAttrPrefix)) {
                                                attrPrefix = declAttrPrefix;

                                                if (declAttrPrefix.length() > 0 ) {
                                                        attrName = declAttrPrefix + ":" + localName;
                                                } else {
                                                        attrName = localName;
                                                }
                                        } else {
                                                if (attrPrefix != null && !"".equals(attrPrefix)
                                                                && declAttrNS == null) {
                                                        if ((fFeatures & NAMESPACEDECLS) != 0) {
                                                                fSerializer.addAttribute(XMLNS_URI, attrPrefix,
                                                                                XMLNS_PREFIX + ":" + attrPrefix, "CDATA",
                                                                                attrNS);
                                                                fNSBinder.declarePrefix(attrPrefix, attrNS);
                                                                fLocalNSBinder.declarePrefix(attrPrefix, attrNS);
                                                        }
                                                } else {
                                                        int counter = 1;
                                                        attrPrefix = "NS" + counter++;

                                                        while (fLocalNSBinder.getURI(attrPrefix) != null) {
                                                                attrPrefix = "NS" + counter++;
                                                        }
                                                        attrName = attrPrefix + ":" + localName;

                                                        if ((fFeatures & NAMESPACEDECLS) != 0) {

                                                                fSerializer.addAttribute(XMLNS_URI, attrPrefix,
                                                                                XMLNS_PREFIX + ":" + attrPrefix, "CDATA",
                                                                                attrNS);
                                                        fNSBinder.declarePrefix(attrPrefix, attrNS);
                                                        fLocalNSBinder.declarePrefix(attrPrefix, attrNS);
                                                        }
                                                }
                                        }
                                }

                        } else { 
                                if (localName == null) {
                                        String msg = Utils.messages.createMessage(
                                                        MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
                                                        new Object[] { attrName });

                                        if (fErrorHandler != null) {
                                                fErrorHandler
                                                                .handleError(new DOMErrorImpl(
                                                                                DOMError.SEVERITY_ERROR, msg,
                                                                                MsgKey.ER_NULL_LOCAL_ELEMENT_NAME, null,
                                                                                null, null));
                                        }

                                } else { 
                                }
                        }

            }


            if ((((fFeatures & DISCARDDEFAULT) != 0) && isSpecified)
                || ((fFeatures & DISCARDDEFAULT) == 0)) {
                applyFilter = true;
            } else {
                addAttr = false;
            }

            if (applyFilter) {
                if (fFilter != null
                    && (fFilter.getWhatToShow() & NodeFilter.SHOW_ATTRIBUTE)
                        != 0) {

                    if (!xmlnsAttr) {
                        short code = fFilter.acceptNode(attr);
                        switch (code) {
                            case NodeFilter.FILTER_REJECT :
                            case NodeFilter.FILTER_SKIP :
                                addAttr = false;
                                break;
                            default : 
                        }
                    }
                }
            }

            if (addAttr && xmlnsAttr) {
                if ((fFeatures & NAMESPACEDECLS) != 0) {
                        if (localName != null && !"".equals(localName)) {
                                fSerializer.addAttribute(attrNS, localName, attrName, type, attrValue);
                        }
                }
            } else if (
                addAttr && !xmlnsAttr) { 
                if (((fFeatures & NAMESPACEDECLS) != 0) && (attrNS != null)) {
                    fSerializer.addAttribute(
                        attrNS,
                        localName,
                        attrName,
                        type,
                        attrValue);
                } else {
                    fSerializer.addAttribute(
                        "",
                        localName,
                        attrName,
                        type,
                        attrValue);
                }
            }

            if (xmlnsAttr && ((fFeatures & NAMESPACEDECLS) != 0)) {
                int index;
                String prefix =
                    (index = attrName.indexOf(":")) < 0
                        ? ""
                        : attrName.substring(index + 1);

                if (!"".equals(prefix)) {
                    fSerializer.namespaceAfterStartElement(prefix, attrValue);
                }
            }
        }

    }

    /**
     * Serializes an ProcessingInstruction Node.
     *
     * @param node The ProcessingInstruction Node to serialize
     */
    protected void serializePI(ProcessingInstruction node)
        throws SAXException {
        ProcessingInstruction pi = node;
        String name = pi.getNodeName();

        if ((fFeatures & WELLFORMED) != 0) {
            isPIWellFormed(node);
        }

        if (!applyFilter(node, NodeFilter.SHOW_PROCESSING_INSTRUCTION)) {
            return;
        }

        if (name.equals("xslt-next-is-raw")) {
            fNextIsRaw = true;
        } else {
            this.fSerializer.processingInstruction(name, pi.getData());
        }
    }

    /**
     * Serializes an CDATASection Node.
     *
     * @param node The CDATASection Node to serialize
     */
    protected void serializeCDATASection(CDATASection node)
        throws SAXException {
        if ((fFeatures & WELLFORMED) != 0) {
            isCDATASectionWellFormed(node);
        }

        if ((fFeatures & CDATA) != 0) {

            String nodeValue = node.getNodeValue();
            int endIndex = nodeValue.indexOf("]]>");
            if ((fFeatures & SPLITCDATA) != 0) {
                if (endIndex >= 0) {
                    String relatedData = nodeValue.substring(0, endIndex + 2);

                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_CDATA_SECTIONS_SPLIT,
                            null);

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_WARNING,
                                msg,
                                MsgKey.ER_CDATA_SECTIONS_SPLIT,
                                null,
                                relatedData,
                                null));
                    }
                }
            } else {
                if (endIndex >= 0) {
                    String relatedData = nodeValue.substring(0, endIndex + 2);

                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_CDATA_SECTIONS_SPLIT,
                            null);

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_ERROR,
                                msg,
                                MsgKey.ER_CDATA_SECTIONS_SPLIT));
                    }
                    return;
                }
            }

            if (!applyFilter(node, NodeFilter.SHOW_CDATA_SECTION)) {
                return;
            }

            if (fLexicalHandler != null) {
                fLexicalHandler.startCDATA();
            }
            dispatachChars(node);
            if (fLexicalHandler != null) {
                fLexicalHandler.endCDATA();
            }
        } else {
            dispatachChars(node);
        }
    }

    /**
     * Serializes an Text Node.
     *
     * @param node The Text Node to serialize
     */
    protected void serializeText(Text node) throws SAXException {
        if (fNextIsRaw) {
            fNextIsRaw = false;
            fSerializer.processingInstruction(
                javax.xml.transform.Result.PI_DISABLE_OUTPUT_ESCAPING,
                "");
            dispatachChars(node);
            fSerializer.processingInstruction(
                javax.xml.transform.Result.PI_ENABLE_OUTPUT_ESCAPING,
                "");
        } else {
            boolean bDispatch = false;

            if ((fFeatures & WELLFORMED) != 0) {
                isTextWellFormed(node);
            }

            boolean isElementContentWhitespace = false;
            if (fIsLevel3DOM) {
                isElementContentWhitespace =
                       node.isElementContentWhitespace();
            }

            if (isElementContentWhitespace) {
                if ((fFeatures & ELEM_CONTENT_WHITESPACE) != 0) {
                    bDispatch = true;
                }
            } else {
                bDispatch = true;
            }

            if (!applyFilter(node, NodeFilter.SHOW_TEXT)) {
                return;
            }

            if (bDispatch
                    && (!fSerializer.getIndent() || !node.getData().replace('\n', ' ').trim().isEmpty())) {
                dispatachChars(node);
            }
        }
    }

    /**
     * Serializes an EntityReference Node.
     *
     * @param node The EntityReference Node to serialize
     * @param bStart Inicates if called from start or endNode
     */
    protected void serializeEntityReference(
        EntityReference node,
        boolean bStart)
        throws SAXException {
        if (bStart) {
            EntityReference eref = node;
            if ((fFeatures & ENTITIES) != 0) {


                if ((fFeatures & WELLFORMED) != 0) {
                    isEntityReferneceWellFormed(node);
                }

                if ((fFeatures & NAMESPACES) != 0) {
                    checkUnboundPrefixInEntRef(node);
                }

            }

            if (fLexicalHandler != null && ((fFeatures & ENTITIES) != 0 || !node.hasChildNodes())) {

                fLexicalHandler.startEntity(eref.getNodeName());
            }

        } else {
            EntityReference eref = node;
            if (fLexicalHandler != null) {
                fLexicalHandler.endEntity(eref.getNodeName());
            }
        }
    }


    /**
     * Taken from org.apache.xerces.dom.CoreDocumentImpl
     *
     * Check the string against XML's definition of acceptable names for
     * elements and attributes and so on using the XMLCharacterProperties
     * utility class
     */
    protected boolean isXMLName(String s, boolean xml11Version) {

        if (s == null) {
            return false;
        }
        if (!xml11Version)
            return XMLChar.isValidName(s);
        else
            return XML11Char.isXML11ValidName(s);
    }

    /**
     * Taken from org.apache.xerces.dom.CoreDocumentImpl
     *
     * Checks if the given qualified name is legal with respect
     * to the version of XML to which this document must conform.
     *
     * @param prefix prefix of qualified name
     * @param local local part of qualified name
     */
    protected boolean isValidQName(
        String prefix,
        String local,
        boolean xml11Version) {

        if (local == null)
            return false;
        boolean validNCName = false;

        if (!xml11Version) {
            validNCName =
                (prefix == null || XMLChar.isValidNCName(prefix))
                    && XMLChar.isValidNCName(local);
        } else {
            validNCName =
                (prefix == null || XML11Char.isXML11ValidNCName(prefix))
                    && XML11Char.isXML11ValidNCName(local);
        }

        return validNCName;
    }

    /**
     * Checks if a XML character is well-formed
     *
     * @param characters A String of characters to be checked for Well-Formedness
     * @param refInvalidChar A reference to the character to be returned that was determined invalid.
     */
    protected boolean isWFXMLChar(String chardata, Character refInvalidChar) {
        if (chardata == null || (chardata.length() == 0)) {
            return true;
        }

        char[] dataarray = chardata.toCharArray();
        int datalength = dataarray.length;

        if (fIsXMLVersion11) {
            int i = 0;
            while (i < datalength) {
                if (XML11Char.isXML11Invalid(dataarray[i++])) {
                    char ch = dataarray[i - 1];
                    if (XMLChar.isHighSurrogate(ch) && i < datalength) {
                        char ch2 = dataarray[i++];
                        if (XMLChar.isLowSurrogate(ch2)
                            && XMLChar.isSupplemental(
                                XMLChar.supplemental(ch, ch2))) {
                            continue;
                        }
                    }
                    refInvalidChar = ch;
                    return false;
                }
            }
        } 
        else {
            int i = 0;
            while (i < datalength) {
                if (XMLChar.isInvalid(dataarray[i++])) {
                    char ch = dataarray[i - 1];
                    if (XMLChar.isHighSurrogate(ch) && i < datalength) {
                        char ch2 = dataarray[i++];
                        if (XMLChar.isLowSurrogate(ch2)
                            && XMLChar.isSupplemental(
                                XMLChar.supplemental(ch, ch2))) {
                            continue;
                        }
                    }
                    refInvalidChar = ch;
                    return false;
                }
            }
        } 

        return true;
    } 

    /**
     * Checks if a XML character is well-formed.  If there is a problem with
     * the character a non-null Character is returned else null is returned.
     *
     * @param characters A String of characters to be checked for Well-Formedness
     * @return Character A reference to the character to be returned that was determined invalid.
     */
    protected Character isWFXMLChar(String chardata) {
        Character refInvalidChar;
        if (chardata == null || (chardata.length() == 0)) {
            return null;
        }

        char[] dataarray = chardata.toCharArray();
        int datalength = dataarray.length;

        if (fIsXMLVersion11) {
            int i = 0;
            while (i < datalength) {
                if (XML11Char.isXML11Invalid(dataarray[i++])) {
                    char ch = dataarray[i - 1];
                    if (XMLChar.isHighSurrogate(ch) && i < datalength) {
                        char ch2 = dataarray[i++];
                        if (XMLChar.isLowSurrogate(ch2)
                            && XMLChar.isSupplemental(
                                XMLChar.supplemental(ch, ch2))) {
                            continue;
                        }
                    }
                    refInvalidChar = ch;
                    return refInvalidChar;
                }
            }
        } 
        else {
            int i = 0;
            while (i < datalength) {
                if (XMLChar.isInvalid(dataarray[i++])) {
                    char ch = dataarray[i - 1];
                    if (XMLChar.isHighSurrogate(ch) && i < datalength) {
                        char ch2 = dataarray[i++];
                        if (XMLChar.isLowSurrogate(ch2)
                            && XMLChar.isSupplemental(
                                XMLChar.supplemental(ch, ch2))) {
                            continue;
                        }
                    }
                    refInvalidChar = ch;
                    return refInvalidChar;
                }
            }
        } 

        return null;
    } 

    /**
     * Checks if a comment node is well-formed
     *
     * @param data The contents of the comment node
     * @return a boolean indiacating if the comment is well-formed or not.
     */
    protected void isCommentWellFormed(String data) {
        if (data == null || (data.length() == 0)) {
            return;
        }

        char[] dataarray = data.toCharArray();
        int datalength = dataarray.length;

        if (fIsXMLVersion11) {
            int i = 0;
            while (i < datalength) {
                char c = dataarray[i++];
                if (XML11Char.isXML11Invalid(c)) {
                    if (XMLChar.isHighSurrogate(c) && i < datalength) {
                        char c2 = dataarray[i++];
                        if (XMLChar.isLowSurrogate(c2)
                            && XMLChar.isSupplemental(
                                XMLChar.supplemental(c, c2))) {
                            continue;
                        }
                    }
                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT,
                            new Object[] { c});

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_FATAL_ERROR,
                                msg,
                                MsgKey.ER_WF_INVALID_CHARACTER,
                                null,
                                null,
                                null));
                    }
                } else if (c == '-' && i < datalength && dataarray[i] == '-') {
                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_WF_DASH_IN_COMMENT,
                            null);

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_FATAL_ERROR,
                                msg,
                                MsgKey.ER_WF_INVALID_CHARACTER,
                                null,
                                null,
                                null));
                    }
                }
            }
        } 
        else {
            int i = 0;
            while (i < datalength) {
                char c = dataarray[i++];
                if (XMLChar.isInvalid(c)) {
                    if (XMLChar.isHighSurrogate(c) && i < datalength) {
                        char c2 = dataarray[i++];
                        if (XMLChar.isLowSurrogate(c2)
                            && XMLChar.isSupplemental(
                                XMLChar.supplemental(c, c2))) {
                            continue;
                        }
                    }
                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT,
                            new Object[] { c});

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_FATAL_ERROR,
                                msg,
                                MsgKey.ER_WF_INVALID_CHARACTER,
                                null,
                                null,
                                null));
                    }
                } else if (c == '-' && i < datalength && dataarray[i] == '-') {
                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_WF_DASH_IN_COMMENT,
                            null);

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_FATAL_ERROR,
                                msg,
                                MsgKey.ER_WF_INVALID_CHARACTER,
                                null,
                                null,
                                null));
                    }
                }
            }
        }
        return;
    }

    /**
     * Checks if an element node is well-formed, by checking its Name for well-formedness.
     *
     * @param data The contents of the comment node
     * @return a boolean indiacating if the comment is well-formed or not.
     */
    protected void isElementWellFormed(Node node) {
        boolean isNameWF = false;
        if ((fFeatures & NAMESPACES) != 0) {
            isNameWF =
                isValidQName(
                    node.getPrefix(),
                    node.getLocalName(),
                    fIsXMLVersion11);
        } else {
            isNameWF = isXMLName(node.getNodeName(), fIsXMLVersion11);
        }

        if (!isNameWF) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                    new Object[] { "Element", node.getNodeName()});

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                        null,
                        null,
                        null));
            }
        }
    }

    /**
     * Checks if an attr node is well-formed, by checking it's Name and value
     * for well-formedness.
     *
     * @param data The contents of the comment node
     * @return a boolean indiacating if the comment is well-formed or not.
     */
    protected void isAttributeWellFormed(Node node) {
        boolean isNameWF = false;
        if ((fFeatures & NAMESPACES) != 0) {
            isNameWF =
                isValidQName(
                    node.getPrefix(),
                    node.getLocalName(),
                    fIsXMLVersion11);
        } else {
            isNameWF = isXMLName(node.getNodeName(), fIsXMLVersion11);
        }

        if (!isNameWF) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                    new Object[] { "Attr", node.getNodeName()});

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                        null,
                        null,
                        null));
            }
        }

        String value = node.getNodeValue();
        if (value.indexOf('<') >= 0) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_LT_IN_ATTVAL,
                    new Object[] {
                        ((Attr) node).getOwnerElement().getNodeName(),
                        node.getNodeName()});

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_LT_IN_ATTVAL,
                        null,
                        null,
                        null));
            }
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child == null) {
                continue;
            }
            switch (child.getNodeType()) {
                case Node.TEXT_NODE :
                    isTextWellFormed((Text) child);
                    break;
                case Node.ENTITY_REFERENCE_NODE :
                    isEntityReferneceWellFormed((EntityReference) child);
                    break;
                default :
            }
        }


    }

    /**
     * Checks if a PI node is well-formed, by checking it's Name and data
     * for well-formedness.
     *
     * @param data The contents of the comment node
     */
    protected void isPIWellFormed(ProcessingInstruction node) {
        if (!isXMLName(node.getNodeName(), fIsXMLVersion11)) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                    new Object[] { "ProcessingInstruction", node.getTarget()});

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                        null,
                        null,
                        null));
            }
        }


        Character invalidChar = isWFXMLChar(node.getData());
        if (invalidChar != null) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_PI,
                    new Object[] { Integer.toHexString(Character.getNumericValue(invalidChar.charValue())) });

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER,
                        null,
                        null,
                        null));
            }
        }
    }

    /**
     * Checks if an CDATASection node is well-formed, by checking it's data
     * for well-formedness.  Note that the presence of a CDATA termination mark
     * in the contents of a CDATASection is handled by the parameter
     * spli-cdata-sections
     *
     * @param data The contents of the comment node
     */
    protected void isCDATASectionWellFormed(CDATASection node) {
        Character invalidChar = isWFXMLChar(node.getData());
        if (invalidChar != null) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_CDATA,
                    new Object[] { Integer.toHexString(Character.getNumericValue(invalidChar.charValue())) });

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER,
                        null,
                        null,
                        null));
            }
        }
    }

    /**
     * Checks if an Text node is well-formed, by checking if it contains invalid
     * XML characters.
     *
     * @param data The contents of the comment node
     */
    protected void isTextWellFormed(Text node) {
        Character invalidChar = isWFXMLChar(node.getData());
        if (invalidChar != null) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_TEXT,
                    new Object[] { Integer.toHexString(Character.getNumericValue(invalidChar.charValue())) });

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER,
                        null,
                        null,
                        null));
            }
        }
    }

    /**
     * Checks if an EntityRefernece node is well-formed, by checking it's node name.  Then depending
     * on whether it is referenced in Element content or in an Attr Node, checks if the EntityReference
     * references an unparsed entity or a external entity and if so throws raises the
     * appropriate well-formedness error.
     *
     * @param data The contents of the comment node
     * @parent The parent of the EntityReference Node
     */
    protected void isEntityReferneceWellFormed(EntityReference node) {
        if (!isXMLName(node.getNodeName(), fIsXMLVersion11)) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                    new Object[] { "EntityReference", node.getNodeName()});

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                        null,
                        null,
                        null));
            }
        }

        Node parent = node.getParentNode();

        DocumentType docType = node.getOwnerDocument().getDoctype();
        if (docType != null) {
            NamedNodeMap entities = docType.getEntities();
            for (int i = 0; i < entities.getLength(); i++) {
                Entity ent = (Entity) entities.item(i);

                String nodeName =
                    node.getNodeName() == null ? "" : node.getNodeName();
                String nodeNamespaceURI =
                    node.getNamespaceURI() == null
                        ? ""
                        : node.getNamespaceURI();
                String entName =
                    ent.getNodeName() == null ? "" : ent.getNodeName();
                String entNamespaceURI =
                    ent.getNamespaceURI() == null ? "" : ent.getNamespaceURI();
                if (parent.getNodeType() == Node.ELEMENT_NODE) {
                    if (entNamespaceURI.equals(nodeNamespaceURI)
                        && entName.equals(nodeName)) {

                        if (ent.getNotationName() != null) {
                            String msg =
                                Utils.messages.createMessage(
                                    MsgKey.ER_WF_REF_TO_UNPARSED_ENT,
                                    new Object[] { node.getNodeName()});

                            if (fErrorHandler != null) {
                                fErrorHandler.handleError(
                                    new DOMErrorImpl(
                                        DOMError.SEVERITY_FATAL_ERROR,
                                        msg,
                                        MsgKey.ER_WF_REF_TO_UNPARSED_ENT,
                                        null,
                                        null,
                                        null));
                            }
                        }
                    }
                } 

                if (parent.getNodeType() == Node.ATTRIBUTE_NODE) {
                    if (entNamespaceURI.equals(nodeNamespaceURI)
                        && entName.equals(nodeName)) {

                        if (ent.getPublicId() != null
                            || ent.getSystemId() != null
                            || ent.getNotationName() != null) {
                            String msg =
                                Utils.messages.createMessage(
                                    MsgKey.ER_WF_REF_TO_EXTERNAL_ENT,
                                    new Object[] { node.getNodeName()});

                            if (fErrorHandler != null) {
                                fErrorHandler.handleError(
                                    new DOMErrorImpl(
                                        DOMError.SEVERITY_FATAL_ERROR,
                                        msg,
                                        MsgKey.ER_WF_REF_TO_EXTERNAL_ENT,
                                        null,
                                        null,
                                        null));
                            }
                        }
                    }
                } 
            }
        }
    } 

    /**
     * If the configuration parameter "namespaces" is set to true, this methods
     * checks if an entity whose replacement text contains unbound namespace
     * prefixes is referenced in a location where there are no bindings for
     * the namespace prefixes and if so raises a LSException with the error-type
     * "unbound-prefix-in-entity-reference"
     *
     * @param Node, The EntityReference nodes whose children are to be checked
     */
    protected void checkUnboundPrefixInEntRef(Node node) {
        Node child, next;
        for (child = node.getFirstChild(); child != null; child = next) {
            next = child.getNextSibling();

            if (child.getNodeType() == Node.ELEMENT_NODE) {

                String prefix = child.getPrefix();
                if (prefix != null
                                && fNSBinder.getURI(prefix) == null) {
                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_ELEM_UNBOUND_PREFIX_IN_ENTREF,
                            new Object[] {
                                node.getNodeName(),
                                child.getNodeName(),
                                prefix });

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_FATAL_ERROR,
                                msg,
                                MsgKey.ER_ELEM_UNBOUND_PREFIX_IN_ENTREF,
                                null,
                                null,
                                null));
                    }
                }

                NamedNodeMap attrs = child.getAttributes();

                for (int i = 0; i < attrs.getLength(); i++) {
                    String attrPrefix = attrs.item(i).getPrefix();
                    if (attrPrefix != null
                                && fNSBinder.getURI(attrPrefix) == null) {
                        String msg =
                            Utils.messages.createMessage(
                                MsgKey.ER_ATTR_UNBOUND_PREFIX_IN_ENTREF,
                                new Object[] {
                                    node.getNodeName(),
                                    child.getNodeName(),
                                    attrs.item(i)});

                        if (fErrorHandler != null) {
                            fErrorHandler.handleError(
                                new DOMErrorImpl(
                                    DOMError.SEVERITY_FATAL_ERROR,
                                    msg,
                                    MsgKey.ER_ATTR_UNBOUND_PREFIX_IN_ENTREF,
                                    null,
                                    null,
                                    null));
                        }
                    }
                }
            }

            if (child.hasChildNodes()) {
                checkUnboundPrefixInEntRef(child);
            }
        }
    }

    /**
     * Records local namespace declarations, to be used for normalization later
     *
     * @param Node, The element node, whose namespace declarations are to be recorded
     */
    protected void recordLocalNSDecl(Node node) {
        NamedNodeMap atts = ((Element) node).getAttributes();
        int length = atts.getLength();

        for (int i = 0; i < length; i++) {
            Node attr = atts.item(i);

            String localName = attr.getLocalName();
            String attrPrefix = attr.getPrefix();
            String attrValue = attr.getNodeValue();
            String attrNS = attr.getNamespaceURI();

            localName =
                localName == null
                    || XMLNS_PREFIX.equals(localName) ? "" : localName;
            attrPrefix = attrPrefix == null ? "" : attrPrefix;
            attrValue = attrValue == null ? "" : attrValue;
            attrNS = attrNS == null ? "" : attrNS;

            if (XMLNS_URI.equals(attrNS)) {

                if (XMLNS_URI.equals(attrValue)) {
                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND,
                            new Object[] { attrPrefix, XMLNS_URI });

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_ERROR,
                                msg,
                                MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND,
                                null,
                                null,
                                null));
                    }
                } else {
                        if (XMLNS_PREFIX.equals(attrPrefix) ) {
                        if (attrValue.length() != 0) {
                            fNSBinder.declarePrefix(localName, attrValue);
                        } else {
                        }
                    } else { 
                        fNSBinder.declarePrefix("", attrValue);
                    }
                }

            }
        }
    }

    /**
     * Fixes an element's namespace
     *
     * @param Node, The element node, whose namespace is to be fixed
     */
    protected void fixupElementNS(Node node) throws SAXException {
        String namespaceURI = ((Element) node).getNamespaceURI();
        String prefix = ((Element) node).getPrefix();
        String localName = ((Element) node).getLocalName();

        if (namespaceURI != null) {
            prefix = prefix == null ? "" : prefix;
            String inScopeNamespaceURI = fNSBinder.getURI(prefix);

            if ((inScopeNamespaceURI != null
                && inScopeNamespaceURI.equals(namespaceURI))) {

            } else {

                if ((fFeatures & NAMESPACEDECLS) != 0) {
                    if ("".equals(prefix) || "".equals(namespaceURI)) {
                        ((Element)node).setAttributeNS(XMLNS_URI, XMLNS_PREFIX, namespaceURI);
                    } else {
                        ((Element)node).setAttributeNS(XMLNS_URI, XMLNS_PREFIX + ":" + prefix, namespaceURI);
                    }
                }
                fLocalNSBinder.declarePrefix(prefix, namespaceURI);
                fNSBinder.declarePrefix(prefix, namespaceURI);

            }
        } else {
            if (localName == null || "".equals(localName)) {
                String msg =
                    Utils.messages.createMessage(
                        MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
                        new Object[] { node.getNodeName()});

                if (fErrorHandler != null) {
                    fErrorHandler.handleError(
                        new DOMErrorImpl(
                            DOMError.SEVERITY_ERROR,
                            msg,
                            MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
                            null,
                            null,
                            null));
                }
            } else {
                namespaceURI = fNSBinder.getURI("");
                if (namespaceURI !=null && namespaceURI.length() > 0) {
                    ((Element)node).setAttributeNS(XMLNS_URI, XMLNS_PREFIX, "");
                        fLocalNSBinder.declarePrefix("", "");
                    fNSBinder.declarePrefix("", "");
                }
            }
        }
    }
    /**
     * This table is a quick lookup of a property key (String) to the integer that
     * is the bit to flip in the fFeatures field, so the integers should have
     * values 1,2,4,8,16...
     *
     */
    private static final Map<String, Integer> fFeatureMap;
    static {


        Map<String, Integer> featureMap = new HashMap<>();
        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_CDATA_SECTIONS,
            CDATA);

        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_COMMENTS,
            COMMENTS);

        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS
                + DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE,
            ELEM_CONTENT_WHITESPACE);

        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_ENTITIES,
            ENTITIES);

        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_NAMESPACES,
            NAMESPACES);

        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS
                + DOMConstants.DOM_NAMESPACE_DECLARATIONS,
            NAMESPACEDECLS);

        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_SPLIT_CDATA,
            SPLITCDATA);

        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_WELLFORMED,
            WELLFORMED);

        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS
                + DOMConstants.DOM_DISCARD_DEFAULT_CONTENT,
            DISCARDDEFAULT);

        fFeatureMap = Collections.unmodifiableMap(featureMap);
    }

    /**
     * Initializes fFeatures based on the DOMConfiguration Parameters set.
     *
     * @param properties DOMConfiguraiton properties that were set and which are
     * to be used while serializing the DOM.
     */
    protected void initProperties(Properties properties) {
        for(String key : properties.stringPropertyNames()) {




            final Integer bitFlag = fFeatureMap.get(key);
            if (bitFlag != null) {

                if ((properties.getProperty(key).endsWith("yes"))) {
                    fFeatures = fFeatures | bitFlag;
                } else {
                    fFeatures = fFeatures & ~bitFlag;
                }
            } else {
                /**
                 * Other properties that have a bit more complex value
                 * than the features in the above map.
                 */
                if ((DOMConstants.S_DOM3_PROPERTIES_NS
                    + DOMConstants.DOM_FORMAT_PRETTY_PRINT)
                    .equals(key)) {
                    if ((properties.getProperty(key).endsWith("yes"))) {
                        fSerializer.setIndent(true);
                        fSerializer.setIndentAmount(4);
                    } else {
                        fSerializer.setIndent(false);
                    }
                } else if ((DOMConstants.S_XSL_OUTPUT_OMIT_XML_DECL).equals(key)) {
                    if ((properties.getProperty(key).endsWith("yes"))) {
                        fSerializer.setOmitXMLDeclaration(true);
                    } else {
                        fSerializer.setOmitXMLDeclaration(false);
                    }
                } else if ((DOMConstants.S_XERCES_PROPERTIES_NS
                            + DOMConstants.S_XML_VERSION).equals(key)) {
                    String version = properties.getProperty(key);
                    if ("1.1".equals(version)) {
                        fIsXMLVersion11 = true;
                        fSerializer.setVersion(version);
                    } else {
                        fSerializer.setVersion("1.0");
                    }
                } else if ((DOMConstants.S_XSL_OUTPUT_ENCODING).equals(key)) {
                    String encoding = properties.getProperty(key);
                    if (encoding != null) {
                        fSerializer.setEncoding(encoding);
                    }
                } else if ((OutputPropertiesFactory.S_KEY_ENTITIES).equals(key)) {
                    String entities = properties.getProperty(key);
                    if (DOMConstants.S_XSL_VALUE_ENTITIES.equals(entities)) {
                        fSerializer.setDTDEntityExpansion(false);
                    }
                }
            }
        }
        if (fNewLine != null) {
            fSerializer.setOutputProperty(OutputPropertiesFactory.S_KEY_LINE_SEPARATOR, fNewLine);
        }
    }

} 
