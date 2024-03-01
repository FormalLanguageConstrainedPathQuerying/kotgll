/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The Document interface represents the entire HTML or XML document.
 * Conceptually, it is the root of the document tree, and provides the
 * primary access to the document's data.
 * <P>
 * Since elements, text nodes, comments, processing instructions,
 * etc. cannot exist outside the context of a Document, the Document
 * interface also contains the factory methods needed to create these
 * objects. The Node objects created have a ownerDocument attribute
 * which associates them with the Document within whose context they
 * were created.
 *
 * @xerces.internal
 *
 * @since  PR-DOM-Level-1-19980818.
 * @LastModified: May 2019
 */
public class DeferredDocumentImpl
    extends DocumentImpl
    implements DeferredNode {


    /** Serialization version. */
    static final long serialVersionUID = 5186323580749626857L;


    /** To include code for printing the ref count tables. */
    private static final boolean DEBUG_PRINT_REF_COUNTS = false;

    /** To include code for printing the internal tables. */
    private static final boolean DEBUG_PRINT_TABLES = false;

    /** To debug identifiers set to true and recompile. */
    private static final boolean DEBUG_IDS = false;


    /** Chunk shift. */
    protected static final int CHUNK_SHIFT = 8;           

    /** Chunk size. */
    protected static final int CHUNK_SIZE = (1 << CHUNK_SHIFT);

    /** Chunk mask. */
    protected static final int CHUNK_MASK = CHUNK_SIZE - 1;

    /** Initial chunk size. */
    protected static final int INITIAL_CHUNK_COUNT = (1 << (13 - CHUNK_SHIFT));   



    /** Node count. */
    protected transient int fNodeCount = 0;

    /** Node types. */
    protected transient int fNodeType[][];

    /** Node names. */
    protected transient Object fNodeName[][];

    /** Node values. */
    protected transient Object fNodeValue[][];

    /** Node parents. */
    protected transient int fNodeParent[][];

    /** Node first children. */
    protected transient int fNodeLastChild[][];

    /** Node prev siblings. */
    protected transient int fNodePrevSib[][];

    /** Node namespace URI. */
    protected transient Object fNodeURI[][];

    /** Extra data. */
    protected transient int fNodeExtra[][];

    /** Identifier count. */
    protected transient int fIdCount;

    /** Identifier name indexes. */
    protected transient String fIdName[];

    /** Identifier element indexes. */
    protected transient int fIdElement[];

    /** DOM2: For namespace support in the deferred case.
     */
    protected boolean fNamespacesEnabled = false;

    private transient final StringBuilder fBufferStr = new StringBuilder();
    private transient final List<String> fStrChunks = new ArrayList<>();


    /**
     * NON-DOM: Actually creating a Document is outside the DOM's spec,
     * since it has to operate in terms of a particular implementation.
     */
    public DeferredDocumentImpl() {
        this(false);
    } 

    /**
     * NON-DOM: Actually creating a Document is outside the DOM's spec,
     * since it has to operate in terms of a particular implementation.
     */
    public DeferredDocumentImpl(boolean namespacesEnabled) {
        this(namespacesEnabled, false);
    } 

    /** Experimental constructor. */
    public DeferredDocumentImpl(boolean namespaces, boolean grammarAccess) {
        super(grammarAccess);

        needsSyncData(true);
        needsSyncChildren(true);

        fNamespacesEnabled = namespaces;

    } 


    /**
     * Retrieve information describing the abilities of this particular
     * DOM implementation. Intended to support applications that may be
     * using DOMs retrieved from several different sources, potentially
     * with different underlying representations.
     */
    public DOMImplementation getImplementation() {
        return DeferredDOMImplementationImpl.getDOMImplementation();
    }

    /** Returns the cached parser.getNamespaces() value.*/
    boolean getNamespacesEnabled() {
        return fNamespacesEnabled;
    }

    void setNamespacesEnabled(boolean enable) {
        fNamespacesEnabled = enable;
    }


    /** Creates a document node in the table. */
    public int createDeferredDocument() {
        int nodeIndex = createNode(Node.DOCUMENT_NODE);
        return nodeIndex;
    }

    /** Creates a doctype. */
    public int createDeferredDocumentType(String rootElementName,
                                          String publicId, String systemId) {

        int nodeIndex = createNode(Node.DOCUMENT_TYPE_NODE);
        int chunk     = nodeIndex >> CHUNK_SHIFT;
        int index     = nodeIndex & CHUNK_MASK;

        setChunkValue(fNodeName, rootElementName, chunk, index);
        setChunkValue(fNodeValue, publicId, chunk, index);
        setChunkValue(fNodeURI, systemId, chunk, index);

        return nodeIndex;

    } 

    public void setInternalSubset(int doctypeIndex, String subset) {
        int chunk     = doctypeIndex >> CHUNK_SHIFT;
        int index     = doctypeIndex & CHUNK_MASK;

        int extraDataIndex = createNode(Node.DOCUMENT_TYPE_NODE);
        int echunk = extraDataIndex >> CHUNK_SHIFT;
        int eindex = extraDataIndex & CHUNK_MASK;
        setChunkIndex(fNodeExtra, extraDataIndex, chunk, index);
        setChunkValue(fNodeValue, subset, echunk, eindex);
    }

    /** Creates a notation in the table. */
    public int createDeferredNotation(String notationName,
                                      String publicId, String systemId, String baseURI) {

        int nodeIndex = createNode(Node.NOTATION_NODE);
        int chunk     = nodeIndex >> CHUNK_SHIFT;
        int index     = nodeIndex & CHUNK_MASK;


        int extraDataIndex = createNode(Node.NOTATION_NODE);
        int echunk = extraDataIndex >> CHUNK_SHIFT;
        int eindex = extraDataIndex & CHUNK_MASK;

        setChunkValue(fNodeName, notationName, chunk, index);
        setChunkValue(fNodeValue, publicId, chunk, index);
        setChunkValue(fNodeURI, systemId, chunk, index);

        setChunkIndex(fNodeExtra, extraDataIndex, chunk, index);
        setChunkValue(fNodeName, baseURI, echunk, eindex);

        return nodeIndex;

    } 

    /** Creates an entity in the table. */
    public int createDeferredEntity(String entityName, String publicId,
                                    String systemId, String notationName,
                                    String baseURI) {
        int nodeIndex = createNode(Node.ENTITY_NODE);
        int chunk     = nodeIndex >> CHUNK_SHIFT;
        int index     = nodeIndex & CHUNK_MASK;

        int extraDataIndex = createNode(Node.ENTITY_NODE);
        int echunk = extraDataIndex >> CHUNK_SHIFT;
        int eindex = extraDataIndex & CHUNK_MASK;

        setChunkValue(fNodeName, entityName, chunk, index);
        setChunkValue(fNodeValue, publicId, chunk, index);
        setChunkValue(fNodeURI, systemId, chunk, index);
        setChunkIndex(fNodeExtra, extraDataIndex, chunk, index);
        setChunkValue(fNodeName, notationName, echunk, eindex);
        setChunkValue(fNodeValue, null, echunk, eindex);
        setChunkValue(fNodeURI, null, echunk, eindex);


        int extraDataIndex2 = createNode(Node.ENTITY_NODE);
        int echunk2 = extraDataIndex2 >> CHUNK_SHIFT;
        int eindex2 = extraDataIndex2 & CHUNK_MASK;

        setChunkIndex(fNodeExtra, extraDataIndex2, echunk, eindex);

        setChunkValue(fNodeName, baseURI, echunk2, eindex2);

        return nodeIndex;

    } 

    public String getDeferredEntityBaseURI (int entityIndex){
        if (entityIndex != -1) {
            int extraDataIndex = getNodeExtra(entityIndex, false);
            extraDataIndex = getNodeExtra(extraDataIndex, false);
            return getNodeName (extraDataIndex, false);
        }
        return null;
    }

    public void setEntityInfo(int currentEntityDecl,
                              String version, String encoding){
        int eNodeIndex = getNodeExtra(currentEntityDecl, false);
        if (eNodeIndex !=-1) {
            int echunk = eNodeIndex >> CHUNK_SHIFT;
            int eindex = eNodeIndex & CHUNK_MASK;
            setChunkValue(fNodeValue, version, echunk, eindex);
            setChunkValue(fNodeURI, encoding, echunk, eindex);
        }
    }

    public void setTypeInfo(int elementNodeIndex, Object type) {
        int elementChunk     = elementNodeIndex >> CHUNK_SHIFT;
        int elementIndex     = elementNodeIndex & CHUNK_MASK;
        setChunkValue(fNodeValue, type, elementChunk, elementIndex);
    }

    /**
     * DOM Internal
     *
     * An attribute specifying the actual encoding of this document. This is
     * <code>null</code> otherwise.
     * <br> This attribute represents the property [character encoding scheme]
     * defined in .
     */
    public void setInputEncoding(int currentEntityDecl, String value){
        int nodeIndex = getNodeExtra(currentEntityDecl, false);
        int extraDataIndex = getNodeExtra(nodeIndex, false);

        int echunk = extraDataIndex >> CHUNK_SHIFT;
        int eindex = extraDataIndex & CHUNK_MASK;

        setChunkValue(fNodeValue, value, echunk, eindex);

    }

    /** Creates an entity reference node in the table. */
    public int createDeferredEntityReference(String name, String baseURI) {

        int nodeIndex = createNode(Node.ENTITY_REFERENCE_NODE);
        int chunk     = nodeIndex >> CHUNK_SHIFT;
        int index     = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeName, name, chunk, index);
        setChunkValue(fNodeValue, baseURI, chunk, index);

        return nodeIndex;

    } 


    /**
     * Creates an element node with a URI in the table and type information.
     * @deprecated
     */
    @Deprecated
    public int createDeferredElement(String elementURI, String elementName,
                                      Object type) {

        int elementNodeIndex = createNode(Node.ELEMENT_NODE);
        int elementChunk     = elementNodeIndex >> CHUNK_SHIFT;
        int elementIndex     = elementNodeIndex & CHUNK_MASK;
        setChunkValue(fNodeName, elementName, elementChunk, elementIndex);
        setChunkValue(fNodeURI, elementURI, elementChunk, elementIndex);
        setChunkValue(fNodeValue, type, elementChunk, elementIndex);

        return elementNodeIndex;

    } 

    /**
     * Creates an element node in the table.
     * @deprecated
     */
    @Deprecated
    public int createDeferredElement(String elementName) {
        return createDeferredElement(null, elementName);
    }

    /**
     * Creates an element node with a URI in the table.
     */
    public int createDeferredElement(String elementURI, String elementName) {

        int elementNodeIndex = createNode(Node.ELEMENT_NODE);
        int elementChunk     = elementNodeIndex >> CHUNK_SHIFT;
        int elementIndex     = elementNodeIndex & CHUNK_MASK;
        setChunkValue(fNodeName, elementName, elementChunk, elementIndex);
        setChunkValue(fNodeURI, elementURI, elementChunk, elementIndex);

        return elementNodeIndex;

    } 


        /**
         * This method is used by the DOMParser to create attributes.
         * @param elementNodeIndex
         * @param attrName
         * @param attrURI
         * @param attrValue
         * @param specified
         * @param id
         * @param type
         * @return int
         */
        public int setDeferredAttribute(int elementNodeIndex,
                                        String attrName,
                                        String attrURI,
                                        String attrValue,
                                        boolean specified,
                                        boolean id,
                                        Object type) {

                int attrNodeIndex = createDeferredAttribute(attrName, attrURI, attrValue, specified);
                int attrChunk = attrNodeIndex >> CHUNK_SHIFT;
                int attrIndex = attrNodeIndex & CHUNK_MASK;
                setChunkIndex(fNodeParent, elementNodeIndex, attrChunk, attrIndex);

                int elementChunk = elementNodeIndex >> CHUNK_SHIFT;
                int elementIndex = elementNodeIndex & CHUNK_MASK;

                int lastAttrNodeIndex = getChunkIndex(fNodeExtra, elementChunk, elementIndex);
                if (lastAttrNodeIndex != 0) {
                        setChunkIndex(fNodePrevSib, lastAttrNodeIndex, attrChunk, attrIndex);
                }
                setChunkIndex(fNodeExtra, attrNodeIndex, elementChunk, elementIndex);

                int extra = getChunkIndex(fNodeExtra, attrChunk, attrIndex);
                if (id) {
                        extra = extra | ID;
                        setChunkIndex(fNodeExtra, extra, attrChunk, attrIndex);
                        String value = getChunkValue(fNodeValue, attrChunk, attrIndex);
                        putIdentifier(value, elementNodeIndex);
                }
                if (type != null) {
                        int extraDataIndex = createNode(DeferredNode.TYPE_NODE);
                        int echunk = extraDataIndex >> CHUNK_SHIFT;
                        int eindex = extraDataIndex & CHUNK_MASK;

                        setChunkIndex(fNodeLastChild, extraDataIndex, attrChunk, attrIndex);
                        setChunkValue(fNodeValue, type, echunk, eindex);
                }

                return attrNodeIndex;
        }

    /** Creates an attribute in the table. */
    public int createDeferredAttribute(String attrName, String attrValue,
                                       boolean specified) {
        return createDeferredAttribute(attrName, null, attrValue, specified);
    }

    /** Creates an attribute with a URI in the table. */
    public int createDeferredAttribute(String attrName, String attrURI,
                                       String attrValue, boolean specified) {

        int nodeIndex = createNode(NodeImpl.ATTRIBUTE_NODE);
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeName, attrName, chunk, index);
        setChunkValue(fNodeURI, attrURI, chunk, index);
        setChunkValue(fNodeValue, attrValue, chunk, index);
        int extra = specified ? SPECIFIED : 0;
        setChunkIndex(fNodeExtra, extra, chunk, index);

        return nodeIndex;

    } 

    /** Creates an element definition in the table.*/
    public int createDeferredElementDefinition(String elementName) {

        int nodeIndex = createNode(NodeImpl.ELEMENT_DEFINITION_NODE);
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeName, elementName, chunk, index);

        return nodeIndex;

    } 

    /** Creates a text node in the table. */
    public int createDeferredTextNode(String data,
                                      boolean ignorableWhitespace) {

        int nodeIndex = createNode(Node.TEXT_NODE);
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeValue, data, chunk, index);
        setChunkIndex(fNodeExtra, ignorableWhitespace ?  1 : 0, chunk, index);

        return nodeIndex;

    } 

    /** Creates a CDATA section node in the table. */
    public int createDeferredCDATASection(String data) {

        int nodeIndex = createNode(Node.CDATA_SECTION_NODE);
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeValue, data, chunk, index);

        return nodeIndex;

    } 

    /** Creates a processing instruction node in the table. */
    public int createDeferredProcessingInstruction(String target,
                                                   String data) {
        int nodeIndex = createNode(Node.PROCESSING_INSTRUCTION_NODE);
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeName, target, chunk, index);
        setChunkValue(fNodeValue, data, chunk, index);
        return nodeIndex;

    } 

    /** Creates a comment node in the table. */
    public int createDeferredComment(String data) {

        int nodeIndex = createNode(Node.COMMENT_NODE);
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeValue, data, chunk, index);

        return nodeIndex;

    } 

    /** Creates a clone of the specified node. */
    public int cloneNode(int nodeIndex, boolean deep) {


        int nchunk = nodeIndex >> CHUNK_SHIFT;
        int nindex = nodeIndex & CHUNK_MASK;
        int nodeType = fNodeType[nchunk][nindex];
        int cloneIndex = createNode((short)nodeType);
        int cchunk = cloneIndex >> CHUNK_SHIFT;
        int cindex = cloneIndex & CHUNK_MASK;
        setChunkValue(fNodeName, fNodeName[nchunk][nindex], cchunk, cindex);
        setChunkValue(fNodeValue, fNodeValue[nchunk][nindex], cchunk, cindex);
        setChunkValue(fNodeURI, fNodeURI[nchunk][nindex], cchunk, cindex);
        int extraIndex = fNodeExtra[nchunk][nindex];
        if (extraIndex != -1) {
            if (nodeType != Node.ATTRIBUTE_NODE && nodeType != Node.TEXT_NODE) {
                extraIndex = cloneNode(extraIndex, false);
            }
            setChunkIndex(fNodeExtra, extraIndex, cchunk, cindex);
        }

        if (deep) {
            int prevIndex = -1;
            int childIndex = getLastChild(nodeIndex, false);
            while (childIndex != -1) {
                int clonedChildIndex = cloneNode(childIndex, deep);
                insertBefore(cloneIndex, clonedChildIndex, prevIndex);
                prevIndex = clonedChildIndex;
                childIndex = getRealPrevSibling(childIndex, false);
            }


        }

        return cloneIndex;

    } 

    /** Appends a child to the specified parent in the table. */
    public void appendChild(int parentIndex, int childIndex) {

        int pchunk = parentIndex >> CHUNK_SHIFT;
        int pindex = parentIndex & CHUNK_MASK;
        int cchunk = childIndex >> CHUNK_SHIFT;
        int cindex = childIndex & CHUNK_MASK;
        setChunkIndex(fNodeParent, parentIndex, cchunk, cindex);

        int olast = getChunkIndex(fNodeLastChild, pchunk, pindex);
        setChunkIndex(fNodePrevSib, olast, cchunk, cindex);

        setChunkIndex(fNodeLastChild, childIndex, pchunk, pindex);

    } 

    /** Adds an attribute node to the specified element. */
    public int setAttributeNode(int elemIndex, int attrIndex) {

        int echunk = elemIndex >> CHUNK_SHIFT;
        int eindex = elemIndex & CHUNK_MASK;
        int achunk = attrIndex >> CHUNK_SHIFT;
        int aindex = attrIndex & CHUNK_MASK;

        String attrName = getChunkValue(fNodeName, achunk, aindex);
        int oldAttrIndex = getChunkIndex(fNodeExtra, echunk, eindex);
        int nextIndex = -1;
        int oachunk = -1;
        int oaindex = -1;
        while (oldAttrIndex != -1) {
            oachunk = oldAttrIndex >> CHUNK_SHIFT;
            oaindex = oldAttrIndex & CHUNK_MASK;
            String oldAttrName = getChunkValue(fNodeName, oachunk, oaindex);
            if (oldAttrName.equals(attrName)) {
                break;
            }
            nextIndex = oldAttrIndex;
            oldAttrIndex = getChunkIndex(fNodePrevSib, oachunk, oaindex);
        }

        if (oldAttrIndex != -1) {

            int prevIndex = getChunkIndex(fNodePrevSib, oachunk, oaindex);
            if (nextIndex == -1) {
                setChunkIndex(fNodeExtra, prevIndex, echunk, eindex);
            }
            else {
                int pchunk = nextIndex >> CHUNK_SHIFT;
                int pindex = nextIndex & CHUNK_MASK;
                setChunkIndex(fNodePrevSib, prevIndex, pchunk, pindex);
            }

            clearChunkIndex(fNodeType, oachunk, oaindex);
            clearChunkValue(fNodeName, oachunk, oaindex);
            clearChunkValue(fNodeValue, oachunk, oaindex);
            clearChunkIndex(fNodeParent, oachunk, oaindex);
            clearChunkIndex(fNodePrevSib, oachunk, oaindex);
            int attrTextIndex =
                clearChunkIndex(fNodeLastChild, oachunk, oaindex);
            int atchunk = attrTextIndex >> CHUNK_SHIFT;
            int atindex = attrTextIndex & CHUNK_MASK;
            clearChunkIndex(fNodeType, atchunk, atindex);
            clearChunkValue(fNodeValue, atchunk, atindex);
            clearChunkIndex(fNodeParent, atchunk, atindex);
            clearChunkIndex(fNodeLastChild, atchunk, atindex);
        }

        int prevIndex = getChunkIndex(fNodeExtra, echunk, eindex);
        setChunkIndex(fNodeExtra, attrIndex, echunk, eindex);
        setChunkIndex(fNodePrevSib, prevIndex, achunk, aindex);

        return oldAttrIndex;

    } 


    /** Adds an attribute node to the specified element. */
    public void setIdAttributeNode(int elemIndex, int attrIndex) {

        int chunk = attrIndex >> CHUNK_SHIFT;
        int index = attrIndex & CHUNK_MASK;
        int extra = getChunkIndex(fNodeExtra, chunk, index);
        extra = extra | ID;
        setChunkIndex(fNodeExtra, extra, chunk, index);

        String value = getChunkValue(fNodeValue, chunk, index);
        putIdentifier(value, elemIndex);
    }


    /** Sets type of attribute */
    public void setIdAttribute(int attrIndex) {

        int chunk = attrIndex >> CHUNK_SHIFT;
        int index = attrIndex & CHUNK_MASK;
        int extra = getChunkIndex(fNodeExtra, chunk, index);
        extra = extra | ID;
        setChunkIndex(fNodeExtra, extra, chunk, index);
    }

    /** Inserts a child before the specified node in the table. */
    public int insertBefore(int parentIndex, int newChildIndex, int refChildIndex) {

        if (refChildIndex == -1) {
            appendChild(parentIndex, newChildIndex);
            return newChildIndex;
        }

        int nchunk = newChildIndex >> CHUNK_SHIFT;
        int nindex = newChildIndex & CHUNK_MASK;
        int rchunk = refChildIndex >> CHUNK_SHIFT;
        int rindex = refChildIndex & CHUNK_MASK;
        int previousIndex = getChunkIndex(fNodePrevSib, rchunk, rindex);
        setChunkIndex(fNodePrevSib, newChildIndex, rchunk, rindex);
        setChunkIndex(fNodePrevSib, previousIndex, nchunk, nindex);

        return newChildIndex;

    } 

    /** Sets the last child of the parentIndex to childIndex. */
    public void setAsLastChild(int parentIndex, int childIndex) {
        int pchunk = parentIndex >> CHUNK_SHIFT;
        int pindex = parentIndex & CHUNK_MASK;
        setChunkIndex(fNodeLastChild, childIndex, pchunk, pindex);
    } 

    /**
     * Returns the parent node of the given node.
     * <em>Calling this method does not free the parent index.</em>
     */
    public int getParentNode(int nodeIndex) {
        return getParentNode(nodeIndex, false);
    }

    /**
     * Returns the parent node of the given node.
     * @param free True to free parent node.
     */
    public int getParentNode(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return -1;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkIndex(fNodeParent, chunk, index)
                    : getChunkIndex(fNodeParent, chunk, index);

    } 

    /** Returns the last child of the given node. */
    public int getLastChild(int nodeIndex) {
        return getLastChild(nodeIndex, true);
    }

    /**
     * Returns the last child of the given node.
     * @param free True to free child index.
     */
    public int getLastChild(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return -1;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkIndex(fNodeLastChild, chunk, index)
                    : getChunkIndex(fNodeLastChild, chunk, index);

    } 

    /**
     * Returns the prev sibling of the given node.
     * This is post-normalization of Text Nodes.
     */
    public int getPrevSibling(int nodeIndex) {
        return getPrevSibling(nodeIndex, true);
    }

    /**
     * Returns the prev sibling of the given node.
     * @param free True to free sibling index.
     */
    public int getPrevSibling(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return -1;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        int type = getChunkIndex(fNodeType, chunk, index);
        if (type == Node.TEXT_NODE) {
            do {
                nodeIndex = getChunkIndex(fNodePrevSib, chunk, index);
                if (nodeIndex == -1) {
                    break;
                }
                chunk = nodeIndex >> CHUNK_SHIFT;
                index = nodeIndex & CHUNK_MASK;
                type = getChunkIndex(fNodeType, chunk, index);
            } while (type == Node.TEXT_NODE);
        }
        else {
            nodeIndex = getChunkIndex(fNodePrevSib, chunk, index);
        }

        return nodeIndex;

    } 

    /**
     * Returns the <i>real</i> prev sibling of the given node,
     * directly from the data structures. Used by TextImpl#getNodeValue()
     * to normalize values.
     */
    public int getRealPrevSibling(int nodeIndex) {
        return getRealPrevSibling(nodeIndex, true);
    }

    /**
     * Returns the <i>real</i> prev sibling of the given node.
     * @param free True to free sibling index.
     */
    public int getRealPrevSibling(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return -1;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkIndex(fNodePrevSib, chunk, index)
                    : getChunkIndex(fNodePrevSib, chunk, index);

    } 

    /**
     * Returns the index of the element definition in the table
     * with the specified name index, or -1 if no such definition
     * exists.
     */
    public int lookupElementDefinition(String elementName) {

        if (fNodeCount > 1) {

            int docTypeIndex = -1;
            int nchunk = 0;
            int nindex = 0;
            for (int index = getChunkIndex(fNodeLastChild, nchunk, nindex);
                 index != -1;
                 index = getChunkIndex(fNodePrevSib, nchunk, nindex)) {

                nchunk = index >> CHUNK_SHIFT;
                nindex = index  & CHUNK_MASK;
                if (getChunkIndex(fNodeType, nchunk, nindex) == Node.DOCUMENT_TYPE_NODE) {
                    docTypeIndex = index;
                    break;
                }
            }

            if (docTypeIndex == -1) {
                return -1;
            }
            nchunk = docTypeIndex >> CHUNK_SHIFT;
            nindex = docTypeIndex & CHUNK_MASK;
            for (int index = getChunkIndex(fNodeLastChild, nchunk, nindex);
                 index != -1;
                 index = getChunkIndex(fNodePrevSib, nchunk, nindex)) {

                nchunk = index >> CHUNK_SHIFT;
                nindex = index & CHUNK_MASK;
                if (getChunkIndex(fNodeType, nchunk, nindex) ==
                                           NodeImpl.ELEMENT_DEFINITION_NODE
                 && getChunkValue(fNodeName, nchunk, nindex) == elementName) {
                    return index;
                }
            }
        }

        return -1;

    } 

    /** Instantiates the requested node object. */
    public DeferredNode getNodeObject(int nodeIndex) {

        if (nodeIndex == -1) {
            return null;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        int type = getChunkIndex(fNodeType, chunk, index);
        if (type != Node.TEXT_NODE && type != Node.CDATA_SECTION_NODE) {
            clearChunkIndex(fNodeType, chunk, index);
        }

        DeferredNode node = null;
        switch (type) {


            case Node.ATTRIBUTE_NODE: {
                if (fNamespacesEnabled) {
                    node = new DeferredAttrNSImpl(this, nodeIndex);
                } else {
                    node = new DeferredAttrImpl(this, nodeIndex);
                }
                break;
            }

            case Node.CDATA_SECTION_NODE: {
                node = new DeferredCDATASectionImpl(this, nodeIndex);
                break;
            }

            case Node.COMMENT_NODE: {
                node = new DeferredCommentImpl(this, nodeIndex);
                break;
            }

            case Node.DOCUMENT_NODE: {
                node = this;
                break;
            }

            case Node.DOCUMENT_TYPE_NODE: {
                node = new DeferredDocumentTypeImpl(this, nodeIndex);
                docType = (DocumentTypeImpl)node;
                break;
            }

            case Node.ELEMENT_NODE: {

                if (DEBUG_IDS) {
                    System.out.println("getNodeObject(ELEMENT_NODE): "+nodeIndex);
                }

                if (fNamespacesEnabled) {
                    node = new DeferredElementNSImpl(this, nodeIndex);
                } else {
                    node = new DeferredElementImpl(this, nodeIndex);
                }

                if (fIdElement != null) {
                    int idIndex = binarySearch(fIdElement, 0,
                                               fIdCount-1, nodeIndex);
                    while (idIndex != -1) {

                        if (DEBUG_IDS) {
                            System.out.println("  id index: "+idIndex);
                            System.out.println("  fIdName["+idIndex+
                                               "]: "+fIdName[idIndex]);
                        }

                        String name = fIdName[idIndex];
                        if (name != null) {
                            if (DEBUG_IDS) {
                                System.out.println("  name: "+name);
                                System.out.print("getNodeObject()#");
                            }
                            putIdentifier0(name, (Element)node);
                            fIdName[idIndex] = null;
                        }

                        if (idIndex + 1 < fIdCount &&
                            fIdElement[idIndex + 1] == nodeIndex) {
                            idIndex++;
                        }
                        else {
                            idIndex = -1;
                        }
                    }
                }
                break;
            }

            case Node.ENTITY_NODE: {
                node = new DeferredEntityImpl(this, nodeIndex);
                break;
            }

            case Node.ENTITY_REFERENCE_NODE: {
                node = new DeferredEntityReferenceImpl(this, nodeIndex);
                break;
            }

            case Node.NOTATION_NODE: {
                node = new DeferredNotationImpl(this, nodeIndex);
                break;
            }

            case Node.PROCESSING_INSTRUCTION_NODE: {
                node = new DeferredProcessingInstructionImpl(this, nodeIndex);
                break;
            }

            case Node.TEXT_NODE: {
                node = new DeferredTextImpl(this, nodeIndex);
                break;
            }


            case NodeImpl.ELEMENT_DEFINITION_NODE: {
                node = new DeferredElementDefinitionImpl(this, nodeIndex);
                break;
            }

            default: {
                throw new IllegalArgumentException("type: "+type);
            }

        } 

        if (node != null) {
            return node;
        }

        throw new IllegalArgumentException();

    } 

    /** Returns the name of the given node. */
    public String getNodeName(int nodeIndex) {
        return getNodeName(nodeIndex, true);
    } 

    /**
     * Returns the name of the given node.
     * @param free True to free the string index.
     */
    public String getNodeName(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return null;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkValue(fNodeName, chunk, index)
                    : getChunkValue(fNodeName, chunk, index);

    } 

    /** Returns the real value of the given node. */
    public String getNodeValueString(int nodeIndex) {
        return getNodeValueString(nodeIndex, true);
    } 

    /**
     * Returns the real value of the given node.
     * @param free True to free the string index.
     */
    public String getNodeValueString(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return null;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        String value = free ? clearChunkValue(fNodeValue, chunk, index)
                            : getChunkValue(fNodeValue, chunk, index);
        if (value == null) {
            return null;
        }

        int type  = getChunkIndex(fNodeType, chunk, index);
        if (type == Node.TEXT_NODE) {
            int prevSib = getRealPrevSibling(nodeIndex);
            if (prevSib != -1 &&
                getNodeType(prevSib, false) == Node.TEXT_NODE) {
                fStrChunks.add(value);
                do {
                    chunk = prevSib >> CHUNK_SHIFT;
                    index = prevSib & CHUNK_MASK;
                    value = getChunkValue(fNodeValue, chunk, index);
                    fStrChunks.add(value);
                    prevSib = getChunkIndex(fNodePrevSib, chunk, index);
                    if (prevSib == -1) {
                        break;
                    }
                } while (getNodeType(prevSib, false) == Node.TEXT_NODE);

                int chunkCount = fStrChunks.size();

                for (int i = chunkCount - 1; i >= 0; i--) {
                    fBufferStr.append(fStrChunks.get(i));
                }

                value = fBufferStr.toString();
                fStrChunks.clear();
                fBufferStr.setLength(0);
                return value;
            }
        }
        else if (type == Node.CDATA_SECTION_NODE) {
            int child = getLastChild(nodeIndex, false);
            if (child !=-1) {
                fBufferStr.append(value);
                while (child !=-1) {
                   chunk = child >> CHUNK_SHIFT;
                    index = child & CHUNK_MASK;
                    value = getChunkValue(fNodeValue, chunk, index);
                    fStrChunks.add(value);
                    child = getChunkIndex(fNodePrevSib, chunk, index);
                }
                for (int i=fStrChunks.size()-1; i>=0; i--) {
                     fBufferStr.append(fStrChunks.get(i));
                }

                value = fBufferStr.toString();
                fStrChunks.clear();
                fBufferStr.setLength(0);
                return value;
            }
        }

        return value;

    } 

    /**
     * Returns the value of the given node.
     */
    public String getNodeValue(int nodeIndex) {
        return getNodeValue(nodeIndex, true);
    }

        /**
         * Clears the type info that is stored in the fNodeValue array
         * @param nodeIndex
         * @return Object - type information for the attribute/element node
         */
    public Object getTypeInfo(int nodeIndex) {
        if (nodeIndex == -1) {
            return null;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;


        Object value = fNodeValue[chunk] != null ? fNodeValue[chunk][index] : null;
        if (value != null) {
            fNodeValue[chunk][index] = null;
            RefCount c = (RefCount) fNodeValue[chunk][CHUNK_SIZE];
            c.fCount--;
            if (c.fCount == 0) {
                fNodeValue[chunk] = null;
            }
        }
        return value;
    }

    /**
     * Returns the value of the given node.
     * @param free True to free the value index.
     */
    public String getNodeValue(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return null;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkValue(fNodeValue, chunk, index)
                    : getChunkValue(fNodeValue, chunk, index);

    } 

    /**
     * Returns the extra info of the given node.
     * Used by AttrImpl to store specified value (1 == true).
     */
    public int getNodeExtra(int nodeIndex) {
        return getNodeExtra(nodeIndex, true);
    }

    /**
     * Returns the extra info of the given node.
     * @param free True to free the value index.
     */
    public int getNodeExtra(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return -1;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkIndex(fNodeExtra, chunk, index)
                    : getChunkIndex(fNodeExtra, chunk, index);

    } 

    /** Returns the type of the given node. */
    public short getNodeType(int nodeIndex) {
        return getNodeType(nodeIndex, true);
    }

    /**
     * Returns the type of the given node.
     * @param free True to free type index.
     */
    public short getNodeType(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return -1;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? (short)clearChunkIndex(fNodeType, chunk, index)
                    : (short)getChunkIndex(fNodeType, chunk, index);

    } 

    /** Returns the attribute value of the given name. */
    public String getAttribute(int elemIndex, String name) {
        if (elemIndex == -1 || name == null) {
            return null;
        }
        int echunk = elemIndex >> CHUNK_SHIFT;
        int eindex = elemIndex & CHUNK_MASK;
        int attrIndex = getChunkIndex(fNodeExtra, echunk, eindex);
        while (attrIndex != -1) {
            int achunk = attrIndex >> CHUNK_SHIFT;
            int aindex = attrIndex & CHUNK_MASK;
            if (getChunkValue(fNodeName, achunk, aindex) == name) {
                return getChunkValue(fNodeValue, achunk, aindex);
            }
            attrIndex = getChunkIndex(fNodePrevSib, achunk, aindex);
        }
        return null;
    }

    /** Returns the URI of the given node. */
    public String getNodeURI(int nodeIndex) {
        return getNodeURI(nodeIndex, true);
    }

    /**
     * Returns the URI of the given node.
     * @param free True to free URI index.
     */
    public String getNodeURI(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return null;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkValue(fNodeURI, chunk, index)
                    : getChunkValue(fNodeURI, chunk, index);

    } 


    /** Registers an identifier name with a specified element node. */
    public void putIdentifier(String name, int elementNodeIndex) {

        if (DEBUG_IDS) {
            System.out.println("putIdentifier(" + name + ", "
                               + elementNodeIndex + ')' + " 
                               getChunkValue(fNodeName,
                                             elementNodeIndex >> CHUNK_SHIFT,
                                             elementNodeIndex & CHUNK_MASK));
        }

        if (fIdName == null) {
            fIdName    = new String[64];
            fIdElement = new int[64];
        }

        if (fIdCount == fIdName.length) {
            String idName[] = new String[fIdCount * 2];
            System.arraycopy(fIdName, 0, idName, 0, fIdCount);
            fIdName = idName;

            int idElement[] = new int[idName.length];
            System.arraycopy(fIdElement, 0, idElement, 0, fIdCount);
            fIdElement = idElement;
        }

        fIdName[fIdCount] = name;
        fIdElement[fIdCount] = elementNodeIndex;
        fIdCount++;

    } 


    /** Prints out the tables. */
    public void print() {

        if (DEBUG_PRINT_REF_COUNTS) {
            System.out.print("num\t");
            System.out.print("type\t");
            System.out.print("name\t");
            System.out.print("val\t");
            System.out.print("par\t");
            System.out.print("lch\t");
            System.out.print("psib");
            System.out.println();
            for (int i = 0; i < fNodeType.length; i++) {
                if (fNodeType[i] != null) {
                    System.out.print("--------");
                    System.out.print("--------");
                    System.out.print("--------");
                    System.out.print("--------");
                    System.out.print("--------");
                    System.out.print("--------");
                    System.out.print("--------");
                    System.out.println();

                    System.out.print(i);
                    System.out.print('\t');
                    switch (fNodeType[i][CHUNK_SIZE]) {
                        case DocumentImpl.ELEMENT_DEFINITION_NODE: { System.out.print("EDef"); break; }
                        case Node.DOCUMENT_NODE: { System.out.print("Doc"); break; }
                        case Node.DOCUMENT_TYPE_NODE: { System.out.print("DType"); break; }
                        case Node.COMMENT_NODE: { System.out.print("Com"); break; }
                        case Node.PROCESSING_INSTRUCTION_NODE: { System.out.print("PI"); break; }
                        case Node.ELEMENT_NODE: { System.out.print("Elem"); break; }
                        case Node.ENTITY_NODE: { System.out.print("Ent"); break; }
                        case Node.ENTITY_REFERENCE_NODE: { System.out.print("ERef"); break; }
                        case Node.TEXT_NODE: { System.out.print("Text"); break; }
                        case Node.ATTRIBUTE_NODE: { System.out.print("Attr"); break; }
                        case DeferredNode.TYPE_NODE: { System.out.print("TypeInfo"); break; }
                        default: { System.out.print("?"+fNodeType[i][CHUNK_SIZE]); }
                    }
                    System.out.print('\t');
                    System.out.print(fNodeName[i][CHUNK_SIZE]);
                    System.out.print('\t');
                    System.out.print(fNodeValue[i][CHUNK_SIZE]);
                    System.out.print('\t');
                    System.out.print(fNodeURI[i][CHUNK_SIZE]);
                    System.out.print('\t');
                    System.out.print(fNodeParent[i][CHUNK_SIZE]);
                    System.out.print('\t');
                    System.out.print(fNodeLastChild[i][CHUNK_SIZE]);
                    System.out.print('\t');
                    System.out.print(fNodePrevSib[i][CHUNK_SIZE]);
                    System.out.print('\t');
                    System.out.print(fNodeExtra[i][CHUNK_SIZE]);
                    System.out.println();
                }
            }
        }

        if (DEBUG_PRINT_TABLES) {
            System.out.println("# start table");
            for (int i = 0; i < fNodeCount; i++) {
                int chunk = i >> CHUNK_SHIFT;
                int index = i & CHUNK_MASK;
                if (i % 10 == 0) {
                    System.out.print("num\t");
                    System.out.print("type\t");
                    System.out.print("name\t");
                    System.out.print("val\t");
                    System.out.print("uri\t");
                    System.out.print("par\t");
                    System.out.print("lch\t");
                    System.out.print("psib\t");
                    System.out.print("xtra");
                    System.out.println();
                }
                System.out.print(i);
                System.out.print('\t');
                switch (getChunkIndex(fNodeType, chunk, index)) {
                    case DocumentImpl.ELEMENT_DEFINITION_NODE: { System.out.print("EDef"); break; }
                    case Node.DOCUMENT_NODE: { System.out.print("Doc"); break; }
                    case Node.DOCUMENT_TYPE_NODE: { System.out.print("DType"); break; }
                    case Node.COMMENT_NODE: { System.out.print("Com"); break; }
                    case Node.PROCESSING_INSTRUCTION_NODE: { System.out.print("PI"); break; }
                    case Node.ELEMENT_NODE: { System.out.print("Elem"); break; }
                    case Node.ENTITY_NODE: { System.out.print("Ent"); break; }
                    case Node.ENTITY_REFERENCE_NODE: { System.out.print("ERef"); break; }
                    case Node.TEXT_NODE: { System.out.print("Text"); break; }
                    case Node.ATTRIBUTE_NODE: { System.out.print("Attr"); break; }
                    case DeferredNode.TYPE_NODE: { System.out.print("TypeInfo"); break; }
                    default: { System.out.print("?"+getChunkIndex(fNodeType, chunk, index)); }
                }
                System.out.print('\t');
                System.out.print(getChunkValue(fNodeName, chunk, index));
                System.out.print('\t');
                System.out.print(getNodeValue(chunk, index));
                System.out.print('\t');
                System.out.print(getChunkValue(fNodeURI, chunk, index));
                System.out.print('\t');
                System.out.print(getChunkIndex(fNodeParent, chunk, index));
                System.out.print('\t');
                System.out.print(getChunkIndex(fNodeLastChild, chunk, index));
                System.out.print('\t');
                System.out.print(getChunkIndex(fNodePrevSib, chunk, index));
                System.out.print('\t');
                System.out.print(getChunkIndex(fNodeExtra, chunk, index));
                System.out.println();
            }
            System.out.println("# end table");
        }

    } 


    /** Returns the node index. */
    public int getNodeIndex() {
        return 0;
    }


    /** Synchronizes the node's data. */
    protected void synchronizeData() {

        needsSyncData(false);

        if (fIdElement != null) {


            IntVector path = new IntVector();
            for (int i = 0; i < fIdCount; i++) {

                int elementNodeIndex = fIdElement[i];
                String idName      = fIdName[i];
                if (idName == null) {
                    continue;
                }

                path.removeAllElements();
                int index = elementNodeIndex;
                do {
                    path.addElement(index);
                    int pchunk = index >> CHUNK_SHIFT;
                    int pindex = index & CHUNK_MASK;
                    index = getChunkIndex(fNodeParent, pchunk, pindex);
                } while (index != -1);

                Node place = this;
                for (int j = path.size() - 2; j >= 0; j--) {
                    index = path.elementAt(j);
                    Node child = place.getLastChild();
                    while (child != null) {
                        if (child instanceof DeferredNode) {
                            int nodeIndex =
                                ((DeferredNode)child).getNodeIndex();
                            if (nodeIndex == index) {
                                place = child;
                                break;
                            }
                        }
                        child = child.getPreviousSibling();
                    }
                }

                Element element = (Element)place;
                putIdentifier0(idName, element);
                fIdName[i] = null;

                while (i + 1 < fIdCount &&
                    fIdElement[i + 1] == elementNodeIndex) {
                    idName = fIdName[++i];
                    if (idName == null) {
                        continue;
                    }
                    putIdentifier0(idName, element);
                }
            }

        } 

    } 

    /**
     * Synchronizes the node's children with the internal structure.
     * Fluffing the children at once solves a lot of work to keep
     * the two structures in sync. The problem gets worse when
     * editing the tree -- this makes it a lot easier.
     */
    protected void synchronizeChildren() {

        if (needsSyncData()) {
            synchronizeData();
            /*
             * when we have elements with IDs this method is being recursively
             * called from synchronizeData, in which case we've already gone
             * through the following and we can now simply stop here.
             */
            if (!needsSyncChildren()) {
                return;
            }
        }

        boolean orig = mutationEvents;
        mutationEvents = false;

        needsSyncChildren(false);

        getNodeType(0);

        ChildNode first = null;
        ChildNode last = null;
        for (int index = getLastChild(0);
             index != -1;
             index = getPrevSibling(index)) {

            ChildNode node = (ChildNode)getNodeObject(index);
            if (last == null) {
                last = node;
            }
            else {
                first.previousSibling = node;
            }
            node.ownerNode = this;
            node.isOwned(true);
            node.nextSibling = first;
            first = node;

            int type = node.getNodeType();
            if (type == Node.ELEMENT_NODE) {
                docElement = (ElementImpl)node;
            }
            else if (type == Node.DOCUMENT_TYPE_NODE) {
                docType = (DocumentTypeImpl)node;
            }
        }

        if (first != null) {
            firstChild = first;
            first.isFirstChild(true);
            lastChild(last);
        }

        mutationEvents = orig;

    } 

    /**
     * Synchronizes the node's children with the internal structure.
     * Fluffing the children at once solves a lot of work to keep
     * the two structures in sync. The problem gets worse when
     * editing the tree -- this makes it a lot easier.
     * This is not directly used in this class but this method is
     * here so that it can be shared by all deferred subclasses of AttrImpl.
     */
    protected final void synchronizeChildren(AttrImpl a, int nodeIndex) {

        boolean orig = getMutationEvents();
        setMutationEvents(false);

        a.needsSyncChildren(false);

        int last = getLastChild(nodeIndex);
        int prev = getPrevSibling(last);
        if (prev == -1) {
            a.value = getNodeValueString(nodeIndex);
            a.hasStringValue(true);
        }
        else {
            ChildNode firstNode = null;
            ChildNode lastNode = null;
            for (int index = last; index != -1;
                 index = getPrevSibling(index)) {

                ChildNode node = (ChildNode) getNodeObject(index);
                if (lastNode == null) {
                    lastNode = node;
                }
                else {
                    firstNode.previousSibling = node;
                }
                node.ownerNode = a;
                node.isOwned(true);
                node.nextSibling = firstNode;
                firstNode = node;
            }
            if (lastNode != null) {
                a.value = firstNode; 
                firstNode.isFirstChild(true);
                a.lastChild(lastNode);
            }
            a.hasStringValue(false);
        }

        setMutationEvents(orig);

    } 


    /**
     * Synchronizes the node's children with the internal structure.
     * Fluffing the children at once solves a lot of work to keep
     * the two structures in sync. The problem gets worse when
     * editing the tree -- this makes it a lot easier.
     * This is not directly used in this class but this method is
     * here so that it can be shared by all deferred subclasses of ParentNode.
     */
    protected final void synchronizeChildren(ParentNode p, int nodeIndex) {

        boolean orig = getMutationEvents();
        setMutationEvents(false);

        p.needsSyncChildren(false);

        ChildNode firstNode = null;
        ChildNode lastNode = null;
        for (int index = getLastChild(nodeIndex);
             index != -1;
             index = getPrevSibling(index)) {

            ChildNode node = (ChildNode) getNodeObject(index);
            if (lastNode == null) {
                lastNode = node;
            }
            else {
                firstNode.previousSibling = node;
            }
            node.ownerNode = p;
            node.isOwned(true);
            node.nextSibling = firstNode;
            firstNode = node;
        }
        if (lastNode != null) {
            p.firstChild = firstNode;
            firstNode.isFirstChild(true);
            p.lastChild(lastNode);
        }

        setMutationEvents(orig);

    } 


    /** Ensures that the internal tables are large enough. */
    protected void ensureCapacity(int chunk) {
        if (fNodeType == null) {
            fNodeType       = new int[INITIAL_CHUNK_COUNT][];
            fNodeName       = new Object[INITIAL_CHUNK_COUNT][];
            fNodeValue      = new Object[INITIAL_CHUNK_COUNT][];
            fNodeParent     = new int[INITIAL_CHUNK_COUNT][];
            fNodeLastChild  = new int[INITIAL_CHUNK_COUNT][];
            fNodePrevSib    = new int[INITIAL_CHUNK_COUNT][];
            fNodeURI        = new Object[INITIAL_CHUNK_COUNT][];
            fNodeExtra      = new int[INITIAL_CHUNK_COUNT][];
        }
        else if (fNodeType.length <= chunk) {
            int newsize = chunk * 2;

            int[][] newArray = new int[newsize][];
            System.arraycopy(fNodeType, 0, newArray, 0, chunk);
            fNodeType = newArray;

            Object[][] newStrArray = new Object[newsize][];
            System.arraycopy(fNodeName, 0, newStrArray, 0, chunk);
            fNodeName = newStrArray;

            newStrArray = new Object[newsize][];
            System.arraycopy(fNodeValue, 0, newStrArray, 0, chunk);
            fNodeValue = newStrArray;

            newArray = new int[newsize][];
            System.arraycopy(fNodeParent, 0, newArray, 0, chunk);
            fNodeParent = newArray;

            newArray = new int[newsize][];
            System.arraycopy(fNodeLastChild, 0, newArray, 0, chunk);
            fNodeLastChild = newArray;

            newArray = new int[newsize][];
            System.arraycopy(fNodePrevSib, 0, newArray, 0, chunk);
            fNodePrevSib = newArray;

            newStrArray = new Object[newsize][];
            System.arraycopy(fNodeURI, 0, newStrArray, 0, chunk);
            fNodeURI = newStrArray;

            newArray = new int[newsize][];
            System.arraycopy(fNodeExtra, 0, newArray, 0, chunk);
            fNodeExtra = newArray;
        }
        else if (fNodeType[chunk] != null) {
            return;
        }

        createChunk(fNodeType, chunk);
        createChunk(fNodeName, chunk);
        createChunk(fNodeValue, chunk);
        createChunk(fNodeParent, chunk);
        createChunk(fNodeLastChild, chunk);
        createChunk(fNodePrevSib, chunk);
        createChunk(fNodeURI, chunk);
        createChunk(fNodeExtra, chunk);

        return;

    } 

    /** Creates a node of the specified type. */
    protected int createNode(short nodeType) {
        int chunk = fNodeCount >> CHUNK_SHIFT;
        int index = fNodeCount & CHUNK_MASK;
        ensureCapacity(chunk);

        setChunkIndex(fNodeType, nodeType, chunk, index);

        return fNodeCount++;

    } 

    /**
     * Performs a binary search for a target value in an array of
     * values. The array of values must be in ascending sorted order
     * before calling this method and all array values must be
     * non-negative.
     *
     * @param values  The array of values to search.
     * @param start   The starting offset of the search.
     * @param end     The ending offset of the search.
     * @param target  The target value.
     *
     * @return This function will return the <i>first</i> occurrence
     *         of the target value, or -1 if the target value cannot
     *         be found.
     */
    protected static int binarySearch(final int values[],
                                      int start, int end, int target) {

        if (DEBUG_IDS) {
            System.out.println("binarySearch(), target: "+target);
        }

        while (start <= end) {

            int middle = (start + end) >>> 1;
            int value  = values[middle];
            if (DEBUG_IDS) {
                System.out.print("  value: "+value+", target: "+target+" 
                print(values, start, end, middle, target);
            }
            if (value == target) {
                while (middle > 0 && values[middle - 1] == target) {
                    middle--;
                }
                if (DEBUG_IDS) {
                    System.out.println("FOUND AT "+middle);
                }
                return middle;
            }

            if (value > target) {
                end = middle - 1;
            }
            else {
                start = middle + 1;
            }

        } 

        if (DEBUG_IDS) {
            System.out.println("NOT FOUND!");
        }
        return -1;

    } 

    private static final int[] INIT_ARRAY = new int[CHUNK_SIZE + 1];
    static {
        for (int i = 0; i < CHUNK_SIZE; i++) {
            INIT_ARRAY[i] = -1;
        }
    }
    /** Creates the specified chunk in the given array of chunks. */
    private final void createChunk(int data[][], int chunk) {
        data[chunk] = new int[CHUNK_SIZE + 1];
        System.arraycopy(INIT_ARRAY, 0, data[chunk], 0, CHUNK_SIZE);
    }

    static final class RefCount {
        int fCount;
    }

    private final void createChunk(Object data[][], int chunk) {
        data[chunk] = new Object[CHUNK_SIZE + 1];
        data[chunk][CHUNK_SIZE] = new RefCount();
    }

    /**
     * Sets the specified value in the given of data at the chunk and index.
     *
     * @return Returns the old value.
     */
    private final int setChunkIndex(int data[][], int value,
                                    int chunk, int index) {
        if (value == -1) {
            return clearChunkIndex(data, chunk, index);
        }
        int [] dataChunk = data[chunk];
        if (dataChunk == null) {
            createChunk(data, chunk);
            dataChunk = data[chunk];
        }
        int ovalue = dataChunk[index];
        if (ovalue == -1) {
            dataChunk[CHUNK_SIZE]++;
        }
        dataChunk[index] = value;
        return ovalue;
    }
    private final String setChunkValue(Object data[][], Object value,
                                       int chunk, int index) {
        if (value == null) {
            return clearChunkValue(data, chunk, index);
        }
        Object [] dataChunk = data[chunk];
        if (dataChunk == null) {
            createChunk(data, chunk);
            dataChunk = data[chunk];
        }
        String ovalue = (String) dataChunk[index];
        if (ovalue == null) {
            RefCount c = (RefCount) dataChunk[CHUNK_SIZE];
            c.fCount++;
        }
        dataChunk[index] = value;
        return ovalue;
    }

    /**
     * Returns the specified value in the given data at the chunk and index.
     */
    private final int getChunkIndex(int data[][], int chunk, int index) {
        return data[chunk] != null ? data[chunk][index] : -1;
    }
    private final String getChunkValue(Object data[][], int chunk, int index) {
        return data[chunk] != null ? (String) data[chunk][index] : null;
    }
    private final String getNodeValue(int chunk, int index) {
        Object data = fNodeValue[chunk][index];
        if (data == null){
            return null;
        }
        else if (data instanceof String){
            return (String)data;
        }
        else {
            return data.toString();
        }
    }


    /**
     * Clears the specified value in the given data at the chunk and index.
     * Note that this method will clear the given chunk if the reference
     * count becomes zero.
     *
     * @return Returns the old value.
     */
    private final int clearChunkIndex(int data[][], int chunk, int index) {
        int value = data[chunk] != null ? data[chunk][index] : -1;
        if (value != -1) {
            data[chunk][CHUNK_SIZE]--;
            data[chunk][index] = -1;
            if (data[chunk][CHUNK_SIZE] == 0) {
                data[chunk] = null;
            }
        }
        return value;
    }
    private final String clearChunkValue(Object data[][],
                                         int chunk, int index) {
        String value = data[chunk] != null ? (String)data[chunk][index] : null;
        if (value != null) {
            data[chunk][index] = null;
            RefCount c = (RefCount) data[chunk][CHUNK_SIZE];
            c.fCount--;
            if (c.fCount == 0) {
                data[chunk] = null;
            }
        }
        return value;
    }

    /**
     * This version of putIdentifier is needed to avoid fluffing
     * all of the paths to ID attributes when a node object is
     * created that contains an ID attribute.
     */
    private final void putIdentifier0(String idName, Element element) {

        if (DEBUG_IDS) {
            System.out.println("putIdentifier0("+
                               idName+", "+
                               element+')');
        }

        if (identifiers == null) {
            identifiers = new HashMap<>();
        }

        identifiers.put(idName, element);

    } 

    /** Prints the ID array. */
    private static void print(int values[], int start, int end,
                              int middle, int target) {

        if (DEBUG_IDS) {
            System.out.print(start);
            System.out.print(" [");
            for (int i = start; i < end; i++) {
                if (middle == i) {
                    System.out.print("!");
                }
                System.out.print(values[i]);
                if (values[i] == target) {
                    System.out.print("*");
                }
                if (i < end - 1) {
                    System.out.print(" ");
                }
            }
            System.out.println("] "+end);
        }

    } 


    /**
     * A simple integer vector.
     */
    static final class IntVector {


        /** Data. */
        private int data[];

        /** Size. */
        private int size;


        /** Returns the length of this vector. */
        public int size() {
            return size;
        }

        /** Returns the element at the specified index. */
        public int elementAt(int index) {
            return data[index];
        }

        /** Appends an element to the end of the vector. */
        public void addElement(int element) {
            ensureCapacity(size + 1);
            data[size++] = element;
        }

        /** Clears the vector. */
        public void removeAllElements() {
            size = 0;
        }


        /** Makes sure that there is enough storage. */
        private void ensureCapacity(int newsize) {

            if (data == null) {
                data = new int[newsize + 15];
            }
            else if (newsize > data.length) {
                int newdata[] = new int[newsize + 15];
                System.arraycopy(data, 0, newdata, 0, data.length);
                data = newdata;
            }

        } 

    } 

} 
