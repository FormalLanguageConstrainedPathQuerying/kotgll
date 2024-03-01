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

package com.sun.org.apache.xerces.internal.impl.xs.traversers;

import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.ValidatedInfo;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSAnnotationImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSAttributeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSAttributeUseImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSComplexTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XInt;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import org.w3c.dom.Element;

/**
 * The attribute declaration schema component traverser.
 *
 * <attribute
 *   default = string
 *   fixed = string
 *   form = (qualified | unqualified)
 *   id = ID
 *   name = NCName
 *   ref = QName
 *   type = QName
 *   use = (optional | prohibited | required) : optional
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, (simpleType?))
 * </attribute>
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 * @author Neeraj Bajaj, Sun Microsystems, inc.
 */
class XSDAttributeTraverser extends XSDAbstractTraverser {

    public XSDAttributeTraverser (XSDHandler handler,
            XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }

    protected XSAttributeUseImpl traverseLocal(Element attrDecl,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar,
            XSComplexTypeDecl enclosingCT) {

        Object[] attrValues = fAttrChecker.checkAttributes(attrDecl, false, schemaDoc);

        String defaultAtt = (String) attrValues[XSAttributeChecker.ATTIDX_DEFAULT];
        String fixedAtt   = (String) attrValues[XSAttributeChecker.ATTIDX_FIXED];
        String nameAtt    = (String) attrValues[XSAttributeChecker.ATTIDX_NAME];
        QName  refAtt     = (QName)  attrValues[XSAttributeChecker.ATTIDX_REF];
        XInt   useAtt     = (XInt)   attrValues[XSAttributeChecker.ATTIDX_USE];

        XSAttributeDecl attribute = null;
        XSAnnotationImpl annotation = null;
        if (attrDecl.getAttributeNode(SchemaSymbols.ATT_REF) != null) {
            if (refAtt != null) {
                attribute = (XSAttributeDecl)fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.ATTRIBUTE_TYPE, refAtt, attrDecl);

                Element child = DOMUtil.getFirstChildElement(attrDecl);
                if (child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    annotation = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
                    child = DOMUtil.getNextSiblingElement(child);
                }
                else {
                    String text = DOMUtil.getSyntheticAnnotation(attrDecl);
                    if (text != null) {
                        annotation = traverseSyntheticAnnotation(attrDecl, text, attrValues, false, schemaDoc);
                    }
                }

                if (child != null) {
                    reportSchemaError("src-attribute.3.2", new Object[]{refAtt.rawname}, child);
                }
                nameAtt = refAtt.localpart;
            } else {
                attribute = null;
            }
        } else {
            attribute = traverseNamedAttr(attrDecl, attrValues, schemaDoc, grammar, false, enclosingCT);
        }

        short consType = XSConstants.VC_NONE;
        if (defaultAtt != null) {
            consType = XSConstants.VC_DEFAULT;
        } else if (fixedAtt != null) {
            consType = XSConstants.VC_FIXED;
            defaultAtt = fixedAtt;
            fixedAtt = null;
        }

        XSAttributeUseImpl attrUse = null;
        if (attribute != null) {
            if (fSchemaHandler.fDeclPool !=null) {
                attrUse = fSchemaHandler.fDeclPool.getAttributeUse();
            } else {
                attrUse = new XSAttributeUseImpl();
            }
            attrUse.fAttrDecl = attribute;
            attrUse.fUse = useAtt.shortValue();
            attrUse.fConstraintType = consType;
            if (defaultAtt != null) {
                attrUse.fDefault = new ValidatedInfo();
                attrUse.fDefault.normalizedValue = defaultAtt;
            }
            if (attrDecl.getAttributeNode(SchemaSymbols.ATT_REF) == null) {
                attrUse.fAnnotations = attribute.getAnnotations();
            } else {
                XSObjectList annotations;
                if (annotation != null) {
                    annotations = new XSObjectListImpl();
                    ((XSObjectListImpl) annotations).addXSObject(annotation);
                } else {
                    annotations = XSObjectListImpl.EMPTY_LIST;
                }
                attrUse.fAnnotations = annotations;
            }
        }


        if (defaultAtt != null && fixedAtt != null) {
            reportSchemaError("src-attribute.1", new Object[]{nameAtt}, attrDecl);
        }

        if (consType == XSConstants.VC_DEFAULT &&
                useAtt != null && useAtt.intValue() != SchemaSymbols.USE_OPTIONAL) {
            reportSchemaError("src-attribute.2", new Object[]{nameAtt}, attrDecl);
            attrUse.fUse = SchemaSymbols.USE_OPTIONAL;
        }


        if (defaultAtt != null && attrUse != null) {
            fValidationState.setNamespaceSupport(schemaDoc.fNamespaceSupport);
            try {
                checkDefaultValid(attrUse);
            }
            catch (InvalidDatatypeValueException ide) {
                reportSchemaError (ide.getKey(), ide.getArgs(), attrDecl);
                reportSchemaError ("a-props-correct.2", new Object[]{nameAtt, defaultAtt}, attrDecl);
                attrUse.fDefault = null;
                attrUse.fConstraintType = XSConstants.VC_NONE;
            }

            if (((XSSimpleType)attribute.getTypeDefinition()).isIDType() ) {
                reportSchemaError ("a-props-correct.3", new Object[]{nameAtt}, attrDecl);
                attrUse.fDefault = null;
                attrUse.fConstraintType = XSConstants.VC_NONE;
            }

            if (attrUse.fAttrDecl.getConstraintType() == XSConstants.VC_FIXED &&
                    attrUse.fConstraintType != XSConstants.VC_NONE) {
                if (attrUse.fConstraintType != XSConstants.VC_FIXED ||
                        !attrUse.fAttrDecl.getValInfo().actualValue.equals(attrUse.fDefault.actualValue)) {
                    reportSchemaError ("au-props-correct.2", new Object[]{nameAtt, attrUse.fAttrDecl.getValInfo().stringValue()}, attrDecl);
                    attrUse.fDefault = attrUse.fAttrDecl.getValInfo();
                    attrUse.fConstraintType = XSConstants.VC_FIXED;
                }
            }
        }

        fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return attrUse;
    }

    protected XSAttributeDecl traverseGlobal(Element attrDecl,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {

        Object[] attrValues = fAttrChecker.checkAttributes(attrDecl, true, schemaDoc);
        XSAttributeDecl attribute = traverseNamedAttr(attrDecl, attrValues, schemaDoc, grammar, true, null);
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return attribute;

    }

    /**
     * Traverse a globally declared attribute.
     *
     * @param  attrDecl
     * @param  attrValues
     * @param  schemaDoc
     * @param  grammar
     * @param  isGlobal
     * @return the attribute declaration index
     */
    XSAttributeDecl traverseNamedAttr(Element attrDecl,
            Object[] attrValues,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar,
            boolean isGlobal,
            XSComplexTypeDecl enclosingCT) {

        String  defaultAtt = (String) attrValues[XSAttributeChecker.ATTIDX_DEFAULT];
        String  fixedAtt   = (String) attrValues[XSAttributeChecker.ATTIDX_FIXED];
        XInt    formAtt    = (XInt)   attrValues[XSAttributeChecker.ATTIDX_FORM];
        String  nameAtt    = (String) attrValues[XSAttributeChecker.ATTIDX_NAME];
        QName   typeAtt    = (QName)  attrValues[XSAttributeChecker.ATTIDX_TYPE];

        XSAttributeDecl attribute = null;
        if (fSchemaHandler.fDeclPool !=null) {
            attribute = fSchemaHandler.fDeclPool.getAttributeDecl();
        } else {
            attribute = new XSAttributeDecl();
        }

        if (nameAtt != null)
            nameAtt = fSymbolTable.addSymbol(nameAtt);

        String tnsAtt = null;
        XSComplexTypeDecl enclCT = null;
        short scope = XSAttributeDecl.SCOPE_ABSENT;
        if (isGlobal) {
            tnsAtt = schemaDoc.fTargetNamespace;
            scope = XSAttributeDecl.SCOPE_GLOBAL;
        }
        else {
            if (enclosingCT != null) {
                enclCT = enclosingCT;
                scope = XSAttributeDecl.SCOPE_LOCAL;
            }
            if (formAtt != null) {
                if (formAtt.intValue() == SchemaSymbols.FORM_QUALIFIED)
                    tnsAtt = schemaDoc.fTargetNamespace;
            } else if (schemaDoc.fAreLocalAttributesQualified) {
                tnsAtt = schemaDoc.fTargetNamespace;
            }
        }
        ValidatedInfo attDefault = null;
        short constraintType = XSConstants.VC_NONE;
        if (isGlobal) {
            if (fixedAtt != null) {
                attDefault = new ValidatedInfo();
                attDefault.normalizedValue = fixedAtt;
                constraintType = XSConstants.VC_FIXED;
            } else if (defaultAtt != null) {
                attDefault = new ValidatedInfo();
                attDefault.normalizedValue = defaultAtt;
                constraintType = XSConstants.VC_DEFAULT;
            }
        }

        Element child = DOMUtil.getFirstChildElement(attrDecl);
        XSAnnotationImpl annotation = null;
        if (child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
            annotation = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
            child = DOMUtil.getNextSiblingElement(child);
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(attrDecl);
            if (text != null) {
                annotation = traverseSyntheticAnnotation(attrDecl, text, attrValues, false, schemaDoc);
            }
        }

        XSSimpleType attrType = null;
        boolean haveAnonType = false;

        if (child != null) {
            String childName = DOMUtil.getLocalName(child);

            if (childName.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                attrType = fSchemaHandler.fSimpleTypeTraverser.traverseLocal(child, schemaDoc, grammar);
                haveAnonType = true;
                child = DOMUtil.getNextSiblingElement(child);
            }
        }

        if (attrType == null && typeAtt != null) {
            XSTypeDefinition type = (XSTypeDefinition)fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.TYPEDECL_TYPE, typeAtt, attrDecl);
            if (type != null && type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
                attrType = (XSSimpleType)type;
            }
            else {
                reportSchemaError("src-resolve", new Object[]{typeAtt.rawname, "simpleType definition"}, attrDecl);
                if (type == null) {
                        attribute.fUnresolvedTypeName = typeAtt;
                }
            }
        }

        if (attrType == null) {
            attrType = SchemaGrammar.fAnySimpleType;
        }

        XSObjectList annotations;
        if (annotation != null) {
            annotations = new XSObjectListImpl();
            ((XSObjectListImpl)annotations).addXSObject(annotation);
        } else {
            annotations = XSObjectListImpl.EMPTY_LIST;
        }
        attribute.setValues(nameAtt, tnsAtt, attrType, constraintType, scope,
                attDefault, enclCT, annotations);


        if (nameAtt == null) {
            if (isGlobal)
                reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_ATTRIBUTE, SchemaSymbols.ATT_NAME}, attrDecl);
            else
                reportSchemaError("src-attribute.3.1", null, attrDecl);
            nameAtt = NO_NAME;
        }

        if (child != null) {
            reportSchemaError("s4s-elt-must-match.1", new Object[]{nameAtt, "(annotation?, (simpleType?))", DOMUtil.getLocalName(child)}, child);
        }



        if (defaultAtt != null && fixedAtt != null) {
            reportSchemaError("src-attribute.1", new Object[]{nameAtt}, attrDecl);
        }




        if (haveAnonType && (typeAtt != null)) {
            reportSchemaError( "src-attribute.4", new Object[]{nameAtt}, attrDecl);
        }

        checkNotationType(nameAtt, attrType, attrDecl);


        if (attDefault != null) {
            fValidationState.setNamespaceSupport(schemaDoc.fNamespaceSupport);
            try {
                checkDefaultValid(attribute);
            }
            catch (InvalidDatatypeValueException ide) {
                reportSchemaError (ide.getKey(), ide.getArgs(), attrDecl);
                reportSchemaError ("a-props-correct.2", new Object[]{nameAtt, attDefault.normalizedValue}, attrDecl);
                attDefault = null;
                constraintType = XSConstants.VC_NONE;
                attribute.setValues(nameAtt, tnsAtt, attrType, constraintType, scope,
                        attDefault, enclCT, annotations);
            }
        }

        if (attDefault != null) {
            if (attrType.isIDType() ) {
                reportSchemaError ("a-props-correct.3", new Object[]{nameAtt}, attrDecl);
                attDefault = null;
                constraintType = XSConstants.VC_NONE;
                attribute.setValues(nameAtt, tnsAtt, attrType, constraintType, scope,
                        attDefault, enclCT, annotations);
            }
        }


        if (nameAtt != null && nameAtt.equals(XMLSymbols.PREFIX_XMLNS)) {
            reportSchemaError("no-xmlns", null, attrDecl);
            return null;
        }


        if (tnsAtt != null && tnsAtt.equals(SchemaSymbols.URI_XSI)) {
            reportSchemaError("no-xsi", new Object[]{SchemaSymbols.URI_XSI}, attrDecl);
            return null;
        }

        if (nameAtt.equals(NO_NAME))
            return null;

        if (isGlobal) {
            if (grammar.getGlobalAttributeDecl(nameAtt) == null) {
                grammar.addGlobalAttributeDecl(attribute);
            }

            final String loc = fSchemaHandler.schemaDocument2SystemId(schemaDoc);
            final XSAttributeDecl attribute2 = grammar.getGlobalAttributeDecl(nameAtt, loc);
            if (attribute2  == null) {
                grammar.addGlobalAttributeDecl(attribute, loc);
            }

            if (fSchemaHandler.fTolerateDuplicates) {
                if (attribute2  != null) {
                    attribute = attribute2;
                }
                fSchemaHandler.addGlobalAttributeDecl(attribute);
            }
        }

        return attribute;
    }

    void checkDefaultValid(XSAttributeDecl attribute) throws InvalidDatatypeValueException {
        ((XSSimpleType)attribute.getTypeDefinition()).validate(attribute.getValInfo().normalizedValue, fValidationState, attribute.getValInfo());
        ((XSSimpleType)attribute.getTypeDefinition()).validate(attribute.getValInfo().stringValue(), fValidationState, attribute.getValInfo());
    }

    void checkDefaultValid(XSAttributeUseImpl attrUse) throws InvalidDatatypeValueException {
        ((XSSimpleType)attrUse.fAttrDecl.getTypeDefinition()).validate(attrUse.fDefault.normalizedValue, fValidationState, attrUse.fDefault);
        ((XSSimpleType)attrUse.fAttrDecl.getTypeDefinition()).validate(attrUse.fDefault.stringValue(), fValidationState, attrUse.fDefault);
    }

}
