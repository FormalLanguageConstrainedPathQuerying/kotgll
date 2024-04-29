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

package com.sun.org.apache.xerces.internal.xpointer;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xs.AttributePSVI;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;

/**
 * <p>
 * Implements the XPointerPart interface and handles processing of
 * ShortHand Pointers.  It identifies at most one element in the
 * resource's information set; specifically, the first one (if any)
 * in document order that has a matching NCName as an identifier.
 * </p>
 *
 */
final class ShortHandPointer implements XPointerPart {

    private String fShortHandPointer;

    private boolean fIsFragmentResolved = false;

    private SymbolTable fSymbolTable;

    public ShortHandPointer() {
    }

    public ShortHandPointer(SymbolTable symbolTable) {
        fSymbolTable = symbolTable;
    }

    /**
     * The XPointerProcessor takes care of this.  Simply set the ShortHand Pointer here.
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#parseXPointer(java.lang.String)
     */
    public void parseXPointer(String part) throws XNIException {
        fShortHandPointer = part;
        fIsFragmentResolved = false;
    }

    /**
     * Resolves the XPointer ShortHand pointer based on the rules defined in
     * Section 3.2 of the XPointer Framework Recommendation.
     * Note that in the current implementation only supports DTD determined ID's.
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#resolveXPointer(com.sun.org.apache.xerces.internal.xni.QName, com.sun.org.apache.xerces.internal.xni.XMLAttributes, com.sun.org.apache.xerces.internal.xni.Augmentations, int event)
     */
    int fMatchingChildCount = 0;
    public boolean resolveXPointer(QName element, XMLAttributes attributes,
            Augmentations augs, int event) throws XNIException {

        if (fMatchingChildCount == 0) {
            fIsFragmentResolved = false;
        }

        if (event == XPointerPart.EVENT_ELEMENT_START) {
            if (fMatchingChildCount == 0) {
                fIsFragmentResolved = hasMatchingIdentifier(element, attributes, augs,
                    event);
            }
            if (fIsFragmentResolved) {
               fMatchingChildCount++;
            }
        } else if (event == XPointerPart.EVENT_ELEMENT_EMPTY) {
            if (fMatchingChildCount == 0) {
                fIsFragmentResolved = hasMatchingIdentifier(element, attributes, augs,
                    event);
            }
        }
        else {
            if (fIsFragmentResolved) {
                fMatchingChildCount--;
            }
        }

        return fIsFragmentResolved ;
    }

    /**
     *
     * @param element
     * @param attributes
     * @param augs
     * @param event
     * @return
     * @throws XNIException
     */
    private boolean hasMatchingIdentifier(QName element,
            XMLAttributes attributes, Augmentations augs, int event)
    throws XNIException {
        String normalizedValue = null;


        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {

                normalizedValue = getSchemaDeterminedID(attributes, i);
                if (normalizedValue != null) {
                    break;
                }

                normalizedValue = getChildrenSchemaDeterminedID(attributes, i);
                if (normalizedValue != null) {
                    break;
                }

                normalizedValue = getDTDDeterminedID(attributes, i);
                if (normalizedValue != null) {
                    break;
                }
            }
        }

        if (normalizedValue != null
                && normalizedValue.equals(fShortHandPointer)) {
            return true;
        }

        return false;
    }

    /**
     * Rerturns the DTD determine-ID
     *
     * @param attributes
     * @param index
     * @return String
     * @throws XNIException
     */
    public String getDTDDeterminedID(XMLAttributes attributes, int index)
    throws XNIException {

        if (attributes.getType(index).equals("ID")) {
            return attributes.getValue(index);
        }
        return null;
    }

    /**
     * Returns the schema-determined-ID.
     *
     *
     * @param attributes
     * @param index
     * @return A String containing the schema-determined ID.
     * @throws XNIException
     */
    public String getSchemaDeterminedID(XMLAttributes attributes, int index)
    throws XNIException {
        Augmentations augs = attributes.getAugmentations(index);
        AttributePSVI attrPSVI = (AttributePSVI) augs
        .getItem(Constants.ATTRIBUTE_PSVI);

        if (attrPSVI != null) {




            XSTypeDefinition typeDef = attrPSVI.getMemberTypeDefinition();
            if (typeDef != null) {
                typeDef = attrPSVI.getTypeDefinition();
            }

            if (typeDef != null && ((XSSimpleType) typeDef).isIDType()) {
                return attrPSVI.getSchemaValue().getNormalizedValue();
            }

        }

        return null;
    }

    /**
     * Not quite sure how this can be correctly implemented.
     *
     * @param attributes
     * @param index
     * @return String - We return null since we currenly do not supprt this.
     * @throws XNIException
     */
    public String getChildrenSchemaDeterminedID(XMLAttributes attributes,
            int index) throws XNIException {
        return null;
    }

    /**
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#isFragmentResolved()
     */
    public boolean isFragmentResolved() {
        return fIsFragmentResolved;
    }

    /**
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#isChildFragmentResolved()
     */
    public boolean isChildFragmentResolved() {
        return fIsFragmentResolved && ( fMatchingChildCount >  0);
    }

    /**
     * Returns the name of the ShortHand pointer
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#getSchemeName()
     */
    public String getSchemeName() {
        return fShortHandPointer;
    }

    /**
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#getSchemeData()
     */
    public String getSchemeData() {
        return null;
    }

    /**
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#setSchemeName(java.lang.String)
     */
    public void setSchemeName(String schemeName) {
        fShortHandPointer = schemeName;
    }

    /**
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#setSchemeData(java.lang.String)
     */
    public void setSchemeData(String schemeData) {
    }
}
