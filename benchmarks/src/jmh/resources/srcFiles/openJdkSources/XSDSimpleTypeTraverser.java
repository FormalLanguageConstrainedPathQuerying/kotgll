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

import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeFacetException;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.dv.xs.XSSimpleTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSAnnotationImpl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XInt;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;

/**
 * The simple type definition schema component traverser.
 *
 * <simpleType
 *   final = (#all | (list | union | restriction))
 *   id = ID
 *   name = NCName
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, (restriction | list | union))
 * </simpleType>
 *
 * <restriction
 *   base = QName
 *   id = ID
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, (simpleType?, (minExclusive | minInclusive | maxExclusive | maxInclusive | totalDigits | fractionDigits | length | minLength | maxLength | enumeration | whiteSpace | pattern)*))
 * </restriction>
 *
 * <list
 *   id = ID
 *   itemType = QName
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, (simpleType?))
 * </list>
 *
 * <union
 *   id = ID
 *   memberTypes = List of QName
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, (simpleType*))
 * </union>
 *
 * @xerces.internal
 *
 * @author Elena Litani, IBM
 * @author Neeraj Bajaj, Sun Microsystems, Inc.
 * @author Sandy Gao, IBM
 *
 * @LastModified: Nov 2017
 */
class XSDSimpleTypeTraverser extends XSDAbstractTraverser {

    private boolean fIsBuiltIn = false;

    XSDSimpleTypeTraverser (XSDHandler handler,
            XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }

    XSSimpleType traverseGlobal(Element elmNode,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {

        Object[] attrValues = fAttrChecker.checkAttributes(elmNode, true, schemaDoc);
        String nameAtt = (String)attrValues[XSAttributeChecker.ATTIDX_NAME];
        if (nameAtt == null) {
            attrValues[XSAttributeChecker.ATTIDX_NAME] = NO_NAME;
        }
        XSSimpleType type = traverseSimpleTypeDecl(elmNode, attrValues, schemaDoc, grammar);
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);

        if (nameAtt == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_SIMPLETYPE, SchemaSymbols.ATT_NAME}, elmNode);
            type = null;
        }

        if (type != null) {
            if (grammar.getGlobalTypeDecl(type.getName()) == null) {
                grammar.addGlobalSimpleTypeDecl(type);
            }

            final String loc = fSchemaHandler.schemaDocument2SystemId(schemaDoc);
            final XSTypeDefinition type2 = grammar.getGlobalTypeDecl(type.getName(), loc);
            if (type2 == null) {
                grammar.addGlobalSimpleTypeDecl(type, loc);
            }

            if (fSchemaHandler.fTolerateDuplicates) {
                if (type2 != null) {
                    if (type2 instanceof XSSimpleType) {
                        type = (XSSimpleType) type2;
                    }
                }
                fSchemaHandler.addGlobalTypeDecl(type);
            }
        }

        return type;
    }

    XSSimpleType traverseLocal(Element elmNode,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {

        Object[] attrValues = fAttrChecker.checkAttributes(elmNode, false, schemaDoc);
        String name = genAnonTypeName(elmNode);
        XSSimpleType type = getSimpleType (name, elmNode, attrValues, schemaDoc, grammar);
        if (type instanceof XSSimpleTypeDecl) {
            ((XSSimpleTypeDecl)type).setAnonymous(true);
        }
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);

        return type;
    }

    private XSSimpleType traverseSimpleTypeDecl(Element simpleTypeDecl,
            Object[] attrValues,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {

        String name = (String)attrValues[XSAttributeChecker.ATTIDX_NAME];
        return getSimpleType(name, simpleTypeDecl, attrValues, schemaDoc, grammar);
    }

    /*
     * Generate a name for an anonymous type
     */
    private String genAnonTypeName(Element simpleTypeDecl) {

        StringBuffer typeName = new StringBuffer("#AnonType_");
        Element node = DOMUtil.getParent(simpleTypeDecl);
        while (node != null && (node != DOMUtil.getRoot(DOMUtil.getDocument(node)))) {
            typeName.append(node.getAttribute(SchemaSymbols.ATT_NAME));
            node = DOMUtil.getParent(node);
        }
        return typeName.toString();
    }

    /**
     * @param name
     * @param simpleTypeDecl
     * @param attrValues
     * @param schemaDoc
     * @param grammar
     * @return
     */
    private XSSimpleType getSimpleType(String name, Element simpleTypeDecl, Object[] attrValues, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        XInt finalAttr = (XInt)attrValues[XSAttributeChecker.ATTIDX_FINAL];
        int finalProperty = finalAttr == null ? schemaDoc.fFinalDefault : finalAttr.intValue();
        Element child = DOMUtil.getFirstChildElement(simpleTypeDecl);
        XSAnnotationImpl [] annotations = null;
        if (child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
            XSAnnotationImpl annotation = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
            if (annotation != null)
                annotations = new XSAnnotationImpl [] {annotation};
            child = DOMUtil.getNextSiblingElement(child);
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(simpleTypeDecl);
            if (text != null) {
                XSAnnotationImpl annotation = traverseSyntheticAnnotation(simpleTypeDecl, text, attrValues, false, schemaDoc);
                annotations = new XSAnnotationImpl[] {annotation};
            }
        }
        if (child == null) {
            reportSchemaError("s4s-elt-must-match.2", new Object[]{SchemaSymbols.ELT_SIMPLETYPE, "(annotation?, (restriction | list | union))"}, simpleTypeDecl);
            return errorType(name, schemaDoc.fTargetNamespace, XSConstants.DERIVATION_RESTRICTION);
        }
        String varietyProperty = DOMUtil.getLocalName(child);
        short refType = XSConstants.DERIVATION_RESTRICTION;
        boolean restriction = false, list = false, union = false;
        if (varietyProperty.equals(SchemaSymbols.ELT_RESTRICTION)) {
            refType = XSConstants.DERIVATION_RESTRICTION;
            restriction = true;
        }
        else if (varietyProperty.equals(SchemaSymbols.ELT_LIST)) {
            refType = XSConstants.DERIVATION_LIST;
            list = true;
        }
        else if (varietyProperty.equals(SchemaSymbols.ELT_UNION)) {
            refType = XSConstants.DERIVATION_UNION;
            union = true;
        }
        else {
            reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_SIMPLETYPE, "(annotation?, (restriction | list | union))", varietyProperty}, simpleTypeDecl);
            return errorType(name, schemaDoc.fTargetNamespace, XSConstants.DERIVATION_RESTRICTION);
        }
        Element nextChild = DOMUtil.getNextSiblingElement(child);
        if (nextChild != null) {
            reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_SIMPLETYPE, "(annotation?, (restriction | list | union))", DOMUtil.getLocalName(nextChild)}, nextChild);
        }
        Object[] contentAttrs = fAttrChecker.checkAttributes(child, false, schemaDoc);
        QName baseTypeName = (QName)contentAttrs[restriction ?
                XSAttributeChecker.ATTIDX_BASE :
                    XSAttributeChecker.ATTIDX_ITEMTYPE];
        @SuppressWarnings("unchecked")
        List<QName> memberTypes = (ArrayList<QName>)contentAttrs[XSAttributeChecker.ATTIDX_MEMBERTYPES];
        Element content = DOMUtil.getFirstChildElement(child);
        if (content != null && DOMUtil.getLocalName(content).equals(SchemaSymbols.ELT_ANNOTATION)) {
            XSAnnotationImpl annotation = traverseAnnotationDecl(content, contentAttrs, false, schemaDoc);
            if (annotation != null ) {
                if(annotations == null) {
                    annotations = new XSAnnotationImpl [] {annotation};
                }
                else {
                    XSAnnotationImpl [] tempArray = new XSAnnotationImpl[2];
                    tempArray[0] = annotations[0];
                    annotations = tempArray;
                    annotations[1] = annotation;
                }
            }
            content = DOMUtil.getNextSiblingElement(content);
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(child);
            if (text != null) {
                XSAnnotationImpl annotation = traverseSyntheticAnnotation(child, text, contentAttrs, false, schemaDoc);
                if (annotations == null) {
                    annotations = new XSAnnotationImpl [] {annotation};
                }
                else {
                    XSAnnotationImpl [] tempArray = new XSAnnotationImpl[2];
                    tempArray[0] = annotations[0];
                    annotations = tempArray;
                    annotations[1] = annotation;
                }
            }
        }
        XSSimpleType baseValidator = null;
        if ((restriction || list) && baseTypeName != null) {
            baseValidator = findDTValidator(child, name, baseTypeName, refType, schemaDoc);
            if (baseValidator == null && fIsBuiltIn) {
                fIsBuiltIn = false;
                return null;
            }
        }
        List<XSObject> dTValidators = null;
        XSSimpleType dv = null;
        XSObjectList dvs;
        if (union && memberTypes != null && memberTypes.size() > 0) {
            int size = memberTypes.size();
            dTValidators = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                dv = findDTValidator(child, name, memberTypes.get(i),
                        XSConstants.DERIVATION_UNION, schemaDoc);
                if (dv != null) {
                    if (dv.getVariety() == XSSimpleType.VARIETY_UNION) {
                        dvs = dv.getMemberTypes();
                        for (int j = 0; j < dvs.getLength(); j++)
                            dTValidators.add(dvs.item(j));
                    } else {
                        dTValidators.add(dv);
                    }
                }
            }
        }

        if (content != null && DOMUtil.getLocalName(content).equals(SchemaSymbols.ELT_SIMPLETYPE)) {
            if (restriction || list) {
                if (baseTypeName != null) {
                    reportSchemaError(list ? "src-simple-type.3.a" : "src-simple-type.2.a", null, content);
                }
                if (baseValidator == null) {
                    baseValidator = traverseLocal(content, schemaDoc, grammar);
                }
                content = DOMUtil.getNextSiblingElement(content);
            }
            else if (union) {
                if (dTValidators == null) {
                    dTValidators = new ArrayList<>(2);
                }
                do {
                    dv = traverseLocal(content, schemaDoc, grammar);
                    if (dv != null) {
                        if (dv.getVariety() == XSSimpleType.VARIETY_UNION) {
                            dvs = dv.getMemberTypes();
                            for (int j = 0; j < dvs.getLength(); j++) {
                                dTValidators.add(dvs.item(j));
                            }
                        }
                        else {
                            dTValidators.add(dv);
                        }
                    }
                    content = DOMUtil.getNextSiblingElement(content);
                } while (content != null && DOMUtil.getLocalName(content).equals(SchemaSymbols.ELT_SIMPLETYPE));
            }
        }
        else if ((restriction || list) && baseTypeName == null) {
            reportSchemaError(list ? "src-simple-type.3.b" : "src-simple-type.2.b", null, child);
        }
        else if (union && (memberTypes == null || memberTypes.size() == 0)) {
            reportSchemaError("src-union-memberTypes-or-simpleTypes", null, child);
        }
        if ((restriction || list) && baseValidator == null) {
            fAttrChecker.returnAttrArray(contentAttrs, schemaDoc);
            return errorType(name, schemaDoc.fTargetNamespace,
                    restriction ? XSConstants.DERIVATION_RESTRICTION : XSConstants.DERIVATION_LIST);
        }
        if (union && (dTValidators == null || dTValidators.size() == 0)) {
            fAttrChecker.returnAttrArray(contentAttrs, schemaDoc);
            return errorType(name, schemaDoc.fTargetNamespace,
                    XSConstants.DERIVATION_UNION);
        }
        if (list && isListDatatype(baseValidator)) {
            reportSchemaError("cos-st-restricts.2.1", new Object[]{name, baseValidator.getName()}, child);
            fAttrChecker.returnAttrArray(contentAttrs, schemaDoc);
            return errorType(name, schemaDoc.fTargetNamespace,
                    XSConstants.DERIVATION_LIST);
        }
        XSSimpleType newDecl = null;
        if (restriction) {
            newDecl = fSchemaHandler.fDVFactory.createTypeRestriction(name, schemaDoc.fTargetNamespace, (short)finalProperty, baseValidator,
                    annotations == null? null : new XSObjectListImpl(annotations, annotations.length));
        }
        else if (list) {
            newDecl = fSchemaHandler.fDVFactory.createTypeList(name, schemaDoc.fTargetNamespace, (short)finalProperty, baseValidator,
                    annotations == null? null : new XSObjectListImpl(annotations, annotations.length));
        }
        else if (union) {
            XSSimpleType[] memberDecls = dTValidators.toArray(new XSSimpleType[dTValidators.size()]);
            newDecl = fSchemaHandler.fDVFactory.createTypeUnion(name, schemaDoc.fTargetNamespace, (short)finalProperty, memberDecls,
                    annotations == null? null : new XSObjectListImpl(annotations, annotations.length));
        }
        if (restriction && content != null) {
            FacetInfo fi = traverseFacets(content, newDecl, baseValidator, schemaDoc);
            content = fi.nodeAfterFacets;

            try {
                fValidationState.setNamespaceSupport(schemaDoc.fNamespaceSupport);
                newDecl.applyFacets(fi.facetdata, fi.fPresentFacets, fi.fFixedFacets, fValidationState);
            } catch (InvalidDatatypeFacetException ex) {
                reportSchemaError(ex.getKey(), ex.getArgs(), child);
                newDecl = fSchemaHandler.fDVFactory.createTypeRestriction(name, schemaDoc.fTargetNamespace, (short)finalProperty, baseValidator,
                        annotations == null? null : new XSObjectListImpl(annotations, annotations.length));
            }
        }
        if (content != null) {
            if (restriction) {
                reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_RESTRICTION, "(annotation?, (simpleType?, (minExclusive | minInclusive | maxExclusive | maxInclusive | totalDigits | fractionDigits | length | minLength | maxLength | enumeration | whiteSpace | pattern)*))", DOMUtil.getLocalName(content)}, content);
            }
            else if (list) {
                reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_LIST, "(annotation?, (simpleType?))", DOMUtil.getLocalName(content)}, content);
            }
            else if (union) {
                reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_UNION, "(annotation?, (simpleType*))", DOMUtil.getLocalName(content)}, content);
            }
        }
        fAttrChecker.returnAttrArray(contentAttrs, schemaDoc);
        return newDecl;
    }

    private XSSimpleType findDTValidator(Element elm, String refName,
            QName baseTypeStr, short baseRefContext,
            XSDocumentInfo schemaDoc) {
        if (baseTypeStr == null)
            return null;

        XSTypeDefinition baseType = (XSTypeDefinition)fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.TYPEDECL_TYPE, baseTypeStr, elm);
        if (baseType == null) {
            return null;
        }
        if (baseType.getTypeCategory() != XSTypeDefinition.SIMPLE_TYPE) {
            reportSchemaError("cos-st-restricts.1.1", new Object[]{baseTypeStr.rawname, refName}, elm);
            return null;
        }

        if (baseType == SchemaGrammar.fAnySimpleType &&
            baseRefContext == XSConstants.DERIVATION_RESTRICTION) {
            if (checkBuiltIn(refName, schemaDoc.fTargetNamespace)) {
                return null;
            }
            reportSchemaError("cos-st-restricts.1.1", new Object[]{baseTypeStr.rawname, refName}, elm);
            return null;
        }

        if ((baseType.getFinal() & baseRefContext) != 0) {
            if (baseRefContext == XSConstants.DERIVATION_RESTRICTION) {
                reportSchemaError("st-props-correct.3", new Object[]{refName, baseTypeStr.rawname}, elm);
            }
            else if (baseRefContext == XSConstants.DERIVATION_LIST) {
                reportSchemaError("cos-st-restricts.2.3.1.1", new Object[]{baseTypeStr.rawname, refName}, elm);
            }
            else if (baseRefContext == XSConstants.DERIVATION_UNION) {
                reportSchemaError("cos-st-restricts.3.3.1.1", new Object[]{baseTypeStr.rawname, refName}, elm);
            }
            return null;
        }

        return (XSSimpleType)baseType;
    }

    private final boolean checkBuiltIn(String name, String namespace) {
        if (namespace != SchemaSymbols.URI_SCHEMAFORSCHEMA)
            return false;
        if (SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(name) != null)
            fIsBuiltIn = true;
        return fIsBuiltIn;
    }

    private boolean isListDatatype(XSSimpleType validator) {
        if (validator.getVariety() == XSSimpleType.VARIETY_LIST)
            return true;

        if (validator.getVariety() == XSSimpleType.VARIETY_UNION) {
            XSObjectList temp = validator.getMemberTypes();
            for (int i = 0; i < temp.getLength(); i++) {
                if (((XSSimpleType)temp.item(i)).getVariety() == XSSimpleType.VARIETY_LIST) {
                    return true;
                }
            }
        }

        return false;
    }

    private XSSimpleType errorType(String name, String namespace, short refType) {
        XSSimpleType stringType = (XSSimpleType)SchemaGrammar.SG_SchemaNS.getTypeDefinition("string");
        switch (refType) {
        case XSConstants.DERIVATION_RESTRICTION:
            return fSchemaHandler.fDVFactory.createTypeRestriction(name, namespace, (short)0,
                    stringType, null);
        case XSConstants.DERIVATION_LIST:
            return fSchemaHandler.fDVFactory.createTypeList(name, namespace, (short)0,
                    stringType, null);
        case XSConstants.DERIVATION_UNION:
            return fSchemaHandler.fDVFactory.createTypeUnion(name, namespace, (short)0,
                    new XSSimpleType[]{stringType}, null);
        }

        return null;
    }

}
