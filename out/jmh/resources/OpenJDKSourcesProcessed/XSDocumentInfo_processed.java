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

package com.sun.org.apache.xerces.internal.impl.xs.traversers;

import com.sun.org.apache.xerces.internal.impl.validation.ValidationState;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaNamespaceSupport;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaException;
import com.sun.org.apache.xerces.internal.impl.xs.util.XInt;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Objects of this class hold all information pecular to a
 * particular XML Schema document.  This is needed because
 * namespace bindings and other settings on the <schema/> element
 * affect the contents of that schema document alone.
 *
 * @xerces.internal
 *
 * @author Neil Graham, IBM
 * @LastModified: Oct 2017
 */
class XSDocumentInfo {

    protected SchemaNamespaceSupport fNamespaceSupport;
    protected SchemaNamespaceSupport fNamespaceSupportRoot;
    protected Stack<SchemaNamespaceSupport> SchemaNamespaceSupportStack = new Stack<>();

    protected boolean fAreLocalAttributesQualified;

    protected boolean fAreLocalElementsQualified;

    protected short fBlockDefault;
    protected short fFinalDefault;

    String fTargetNamespace;

    protected boolean fIsChameleonSchema;

    protected Element fSchemaElement;

    List<String> fImportedNS = new ArrayList<>();

    protected ValidationState fValidationContext = new ValidationState();

    SymbolTable fSymbolTable = null;

    protected XSAttributeChecker fAttrChecker;

    protected Object [] fSchemaAttrs;

    protected XSAnnotationInfo fAnnotations = null;

    XSDocumentInfo (Element schemaRoot, XSAttributeChecker attrChecker, SymbolTable symbolTable)
                    throws XMLSchemaException {
        fSchemaElement = schemaRoot;
        initNamespaceSupport(schemaRoot);
        fIsChameleonSchema = false;

        fSymbolTable = symbolTable;
        fAttrChecker = attrChecker;

        if (schemaRoot != null) {
            Element root = schemaRoot;
            fSchemaAttrs = attrChecker.checkAttributes(root, true, this);
            if (fSchemaAttrs == null) {
                throw new XMLSchemaException(null, null);
            }
            fAreLocalAttributesQualified =
                ((XInt)fSchemaAttrs[XSAttributeChecker.ATTIDX_AFORMDEFAULT]).intValue() == SchemaSymbols.FORM_QUALIFIED;
            fAreLocalElementsQualified =
                ((XInt)fSchemaAttrs[XSAttributeChecker.ATTIDX_EFORMDEFAULT]).intValue() == SchemaSymbols.FORM_QUALIFIED;
            fBlockDefault =
                ((XInt)fSchemaAttrs[XSAttributeChecker.ATTIDX_BLOCKDEFAULT]).shortValue();
            fFinalDefault =
                ((XInt)fSchemaAttrs[XSAttributeChecker.ATTIDX_FINALDEFAULT]).shortValue();
            fTargetNamespace =
                (String)fSchemaAttrs[XSAttributeChecker.ATTIDX_TARGETNAMESPACE];
            if (fTargetNamespace != null)
                fTargetNamespace = symbolTable.addSymbol(fTargetNamespace);

            fNamespaceSupportRoot = new SchemaNamespaceSupport(fNamespaceSupport);

            fValidationContext.setNamespaceSupport(fNamespaceSupport);
            fValidationContext.setSymbolTable(symbolTable);

        }
    }

    /**
     * Initialize namespace support by collecting all of the namespace
     * declarations in the root's ancestors. This is necessary to
     * support schemas fragments, i.e. schemas embedded in other
     * documents. See,
     *
     * https:
     *
     * Requires the DOM to be created with namespace support enabled.
     */
    private void initNamespaceSupport(Element schemaRoot) {
        fNamespaceSupport = new SchemaNamespaceSupport();
        fNamespaceSupport.reset();

        Node parent = schemaRoot.getParentNode();
        while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE
                && !parent.getNodeName().equals("DOCUMENT_NODE"))
        {
            Element eparent = (Element) parent;
            NamedNodeMap map = eparent.getAttributes();
            int length = (map != null) ? map.getLength() : 0;
            for (int i = 0; i < length; i++) {
                Attr attr = (Attr) map.item(i);
                String uri = attr.getNamespaceURI();

                if (uri != null && uri.equals("http:
                    String prefix = attr.getLocalName().intern();
                    if (prefix == "xmlns") prefix = "";
                    if (fNamespaceSupport.getURI(prefix) == null) {
                        fNamespaceSupport.declarePrefix(prefix,
                                attr.getValue().intern());
                    }
                }
            }
            parent = parent.getParentNode();
        }
    }

    void backupNSSupport(SchemaNamespaceSupport nsSupport) {
        SchemaNamespaceSupportStack.push(fNamespaceSupport);
        if (nsSupport == null)
            nsSupport = fNamespaceSupportRoot;
        fNamespaceSupport = new SchemaNamespaceSupport(nsSupport);

        fValidationContext.setNamespaceSupport(fNamespaceSupport);
    }

    void restoreNSSupport() {
        fNamespaceSupport = SchemaNamespaceSupportStack.pop();
        fValidationContext.setNamespaceSupport(fNamespaceSupport);
    }

    public String toString() {
        return fTargetNamespace == null?"no targetNamspace":"targetNamespace is " + fTargetNamespace;
    }

    public void addAllowedNS(String namespace) {
        fImportedNS.add(namespace == null ? "" : namespace);
    }

    public boolean isAllowedNS(String namespace) {
        return fImportedNS.contains(namespace == null ? "" : namespace);
    }

    private List<String> fReportedTNS = null;
    final boolean needReportTNSError(String uri) {
        if (fReportedTNS == null)
            fReportedTNS = new ArrayList<>();
        else if (fReportedTNS.contains(uri))
            return false;
        fReportedTNS.add(uri);
        return true;
    }

    Object [] getSchemaAttrs () {
        return fSchemaAttrs;
    }

    void returnSchemaAttrs () {
        fAttrChecker.returnAttrArray (fSchemaAttrs, null);
        fSchemaAttrs = null;
    }

    void addAnnotation(XSAnnotationInfo info) {
        info.next = fAnnotations;
        fAnnotations = info;
    }

    XSAnnotationInfo getAnnotations() {
        return fAnnotations;
    }

    void removeAnnotations() {
        fAnnotations = null;
    }

} 
