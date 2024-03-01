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

package com.sun.org.apache.xerces.internal.impl.xs;

import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSElementDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * To store and validate information about substitutionGroup
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 * @LastModified: July 2019
 */
public class SubstitutionGroupHandler {

    private static final XSElementDecl[] EMPTY_GROUP = new XSElementDecl[0];

    private final XSElementDeclHelper fXSElementDeclHelper;

    /**
     * Default constructor
     */
    public SubstitutionGroupHandler(XSElementDeclHelper elementDeclHelper) {
        fXSElementDeclHelper = elementDeclHelper;
    }

    public XSElementDecl getMatchingElemDecl(QName element, XSElementDecl exemplar) {
        if (Objects.equals(element.localpart, exemplar.fName) &&
            Objects.equals(element.uri, exemplar.fTargetNamespace)) {
            return exemplar;
        }

        if (exemplar.fScope != XSConstants.SCOPE_GLOBAL) {
            return null;
        }

        if ((exemplar.fBlock & XSConstants.DERIVATION_SUBSTITUTION) != 0) {
            return null;
        }

        XSElementDecl eDecl = fXSElementDeclHelper.getGlobalElementDecl(element);
        if (eDecl == null) {
            return null;
        }

        if (substitutionGroupOK(eDecl, exemplar, exemplar.fBlock)) {
            return eDecl;
        }

        return null;
    }

    protected boolean substitutionGroupOK(XSElementDecl element, XSElementDecl exemplar, short blockingConstraint) {
        if (element == exemplar) {
            return true;
        }

        if ((blockingConstraint & XSConstants.DERIVATION_SUBSTITUTION) != 0) {
            return false;
        }

        XSElementDecl subGroup = element.fSubGroup;
        while (subGroup != null && subGroup != exemplar) {
            subGroup = subGroup.fSubGroup;
        }

        if (subGroup == null) {
            return false;
        }

        return typeDerivationOK(element.fType, exemplar.fType, blockingConstraint);
    }

    private boolean typeDerivationOK(XSTypeDefinition derived, XSTypeDefinition base, short blockingConstraint) {

        short devMethod = 0, blockConstraint = blockingConstraint;

        XSTypeDefinition type = derived;
        while (type != base && type != SchemaGrammar.fAnyType) {
            if (type.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                devMethod |= ((XSComplexTypeDecl)type).fDerivedBy;
            }
            else {
                devMethod |= XSConstants.DERIVATION_RESTRICTION;
            }
            type = type.getBaseType();
            if (type == null) {
                type = SchemaGrammar.fAnyType;
            }
            if (type.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                blockConstraint |= ((XSComplexTypeDecl)type).fBlock;
            }
        }
        if (type != base) {
            if (base.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
                XSSimpleTypeDefinition st = (XSSimpleTypeDefinition) base;
                if (st.getVariety() ==  XSSimpleTypeDefinition.VARIETY_UNION) {
                    XSObjectList memberTypes = st.getMemberTypes();
                    final int length = memberTypes.getLength();
                    for (int i = 0; i < length; ++i) {
                        if (typeDerivationOK(derived, (XSTypeDefinition) memberTypes.item(i), blockingConstraint)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        if ((devMethod & blockConstraint) != 0) {
            return false;
        }
        return true;
    }

    public boolean inSubstitutionGroup(XSElementDecl element, XSElementDecl exemplar) {
        return substitutionGroupOK(element, exemplar, exemplar.fBlock);
    }

    Map<XSElementDecl, Object> fSubGroupsB = new HashMap<>();
    private static final OneSubGroup[] EMPTY_VECTOR = new OneSubGroup[0];
    Map<XSElementDecl, XSElementDecl[]> fSubGroups = new HashMap<>();

    /**
     * clear the internal registry of substitutionGroup information
     */
    public void reset() {
        fSubGroupsB.clear();
        fSubGroups.clear();
    }

    /**
     * add a list of substitution group information.
     */
    @SuppressWarnings("unchecked")
    public void addSubstitutionGroup(XSElementDecl[] elements) {
        XSElementDecl subHead, element;
        List<XSElementDecl> subGroup;
        for (int i = elements.length-1; i >= 0; i--) {
            element = elements[i];
            subHead = element.fSubGroup;
            subGroup = (List<XSElementDecl>)fSubGroupsB.get(subHead);
            if (subGroup == null) {
                subGroup = new ArrayList<>();
                fSubGroupsB.put(subHead, subGroup);
            }
            subGroup.add(element);
        }
    }

    /**
     * get all elements that can substitute the given element,
     * according to the spec, we shouldn't consider the {block} constraints.
     *
     * from the spec, substitution group of a given element decl also contains
     * the element itself. but the array returned from this method doesn't
     * containt this element.
     */
    public XSElementDecl[] getSubstitutionGroup(XSElementDecl element) {
        XSElementDecl[] subGroup = fSubGroups.get(element);
        if (subGroup != null)
            return subGroup;

        if ((element.fBlock & XSConstants.DERIVATION_SUBSTITUTION) != 0) {
            fSubGroups.put(element, EMPTY_GROUP);
            return EMPTY_GROUP;
        }

        OneSubGroup[] groupB = getSubGroupB(element, new OneSubGroup());
        int len = groupB.length, rlen = 0;
        XSElementDecl[] ret = new XSElementDecl[len];
        for (int i = 0 ; i < len; i++) {
            if ((element.fBlock & groupB[i].dMethod) == 0)
                ret[rlen++] = groupB[i].sub;
        }
        if (rlen < len) {
            XSElementDecl[] ret1 = new XSElementDecl[rlen];
            System.arraycopy(ret, 0, ret1, 0, rlen);
            ret = ret1;
        }
        fSubGroups.put(element, ret);

        return ret;
    }

    private OneSubGroup[] getSubGroupB(XSElementDecl element, OneSubGroup methods) {
        Object subGroup = fSubGroupsB.get(element);

        if (subGroup == null) {
            fSubGroupsB.put(element, EMPTY_VECTOR);
            return EMPTY_VECTOR;
        }

        if (subGroup instanceof OneSubGroup[])
            return (OneSubGroup[])subGroup;

        @SuppressWarnings("unchecked")
        List<XSElementDecl> group = (ArrayList<XSElementDecl>)subGroup;
        List<OneSubGroup> newGroup = new ArrayList<>();
        OneSubGroup[] group1;
        short dMethod, bMethod, dSubMethod, bSubMethod;
        for (int i = group.size()-1, j; i >= 0; i--) {
            XSElementDecl sub = group.get(i);
            if (!getDBMethods(sub.fType, element.fType, methods))
                continue;
            dMethod = methods.dMethod;
            bMethod = methods.bMethod;
            newGroup.add(new OneSubGroup(sub, methods.dMethod, methods.bMethod));
            group1 = getSubGroupB(sub, methods);
            for (j = group1.length-1; j >= 0; j--) {
                dSubMethod = (short)(dMethod | group1[j].dMethod);
                bSubMethod = (short)(bMethod | group1[j].bMethod);
                if ((dSubMethod & bSubMethod) != 0)
                    continue;
                newGroup.add(new OneSubGroup(group1[j].sub, dSubMethod, bSubMethod));
            }
        }
        OneSubGroup[] ret = new OneSubGroup[newGroup.size()];
        for (int i = newGroup.size()-1; i >= 0; i--) {
            ret[i] = newGroup.get(i);
        }
        fSubGroupsB.put(element, ret);

        return ret;
    }

    private boolean getDBMethods(XSTypeDefinition typed, XSTypeDefinition typeb,
                                 OneSubGroup methods) {
        short dMethod = 0, bMethod = 0;
        while (typed != typeb && typed != SchemaGrammar.fAnyType) {
            if (typed.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE)
                dMethod |= ((XSComplexTypeDecl)typed).fDerivedBy;
            else
                dMethod |= XSConstants.DERIVATION_RESTRICTION;
            typed = typed.getBaseType();
            if (typed == null)
                typed = SchemaGrammar.fAnyType;
            if (typed.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE)
                bMethod |= ((XSComplexTypeDecl)typed).fBlock;
        }
        if (typed != typeb || (dMethod & bMethod) != 0)
            return false;

        methods.dMethod = dMethod;
        methods.bMethod = bMethod;
        return true;
    }

    private static final class OneSubGroup {
        OneSubGroup() {}
        OneSubGroup(XSElementDecl sub, short dMethod, short bMethod) {
            this.sub = sub;
            this.dMethod = dMethod;
            this.bMethod = bMethod;
        }
        XSElementDecl sub;
        short dMethod;
        short bMethod;
    }
} 
