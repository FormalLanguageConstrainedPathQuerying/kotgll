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

package com.sun.org.apache.xerces.internal.impl.xs;

import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.ValidatedInfo;
import com.sun.org.apache.xerces.internal.impl.dv.ValidationContext;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.xs.models.CMBuilder;
import com.sun.org.apache.xerces.internal.impl.xs.models.XSCMValidator;
import com.sun.org.apache.xerces.internal.impl.xs.util.SimpleLocator;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.util.SymbolHash;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Constaints shared by traversers and validator
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 * @LastModified: Nov 2017
 */
public class XSConstraints {


    static final int OCCURRENCE_UNKNOWN = SchemaSymbols.OCCURRENCE_UNBOUNDED-1;
    static final XSSimpleType STRING_TYPE =
            (XSSimpleType)SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(SchemaSymbols.ATTVAL_STRING);

    private static XSParticleDecl fEmptyParticle = null;
    public static XSParticleDecl getEmptySequence() {
        if (fEmptyParticle == null) {
            XSModelGroupImpl group = new XSModelGroupImpl();
            group.fCompositor = XSModelGroupImpl.MODELGROUP_SEQUENCE;
            group.fParticleCount = 0;
            group.fParticles = null;
            group.fAnnotations = XSObjectListImpl.EMPTY_LIST;
            XSParticleDecl particle = new XSParticleDecl();
            particle.fType = XSParticleDecl.PARTICLE_MODELGROUP;
            particle.fValue = group;
            particle.fAnnotations = XSObjectListImpl.EMPTY_LIST;
            fEmptyParticle = particle;
        }
        return fEmptyParticle;
    }

    private static final Comparator<XSParticleDecl> ELEMENT_PARTICLE_COMPARATOR =
            new Comparator<XSParticleDecl>() {

        public int compare(XSParticleDecl o1, XSParticleDecl o2) {
            XSParticleDecl pDecl1 = o1;
            XSParticleDecl pDecl2 = o2;
            XSElementDecl decl1 = (XSElementDecl) pDecl1.fValue;
            XSElementDecl decl2 = (XSElementDecl) pDecl2.fValue;

            String namespace1 = decl1.getNamespace();
            String namespace2 = decl2.getNamespace();
            String name1 = decl1.getName();
            String name2 = decl2.getName();

            boolean sameNamespace = (namespace1 == namespace2);
            int namespaceComparison = 0;

            if (!sameNamespace) {
                if (namespace1 != null) {
                    if (namespace2 != null){
                        namespaceComparison = namespace1.compareTo(namespace2);
                    }
                    else {
                        namespaceComparison = 1;
                    }
                }
                else {
                    namespaceComparison = -1;
                }
            }
            return namespaceComparison != 0 ? namespaceComparison : name1.compareTo(name2);
        }
    };

    /**
     * check whether derived is valid derived from base, given a subset
     * of {restriction, extension}.B
     */
    public static boolean checkTypeDerivationOk(XSTypeDefinition derived, XSTypeDefinition base, short block) {
        if (derived == SchemaGrammar.fAnyType)
            return derived == base;
        if (derived == SchemaGrammar.fAnySimpleType) {
            return (base == SchemaGrammar.fAnyType ||
                    base == SchemaGrammar.fAnySimpleType);
        }

        if (derived.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
            if (base.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                if (base == SchemaGrammar.fAnyType)
                    base = SchemaGrammar.fAnySimpleType;
                else
                    return false;
            }
            return checkSimpleDerivation((XSSimpleType)derived,
                    (XSSimpleType)base, block);
        }
        else {
            return checkComplexDerivation((XSComplexTypeDecl)derived, base, block);
        }
    }

    /**
     * check whether simple type derived is valid derived from base,
     * given a subset of {restriction, extension}.
     */
    public static boolean checkSimpleDerivationOk(XSSimpleType derived, XSTypeDefinition base, short block) {
        if (derived == SchemaGrammar.fAnySimpleType) {
            return (base == SchemaGrammar.fAnyType ||
                    base == SchemaGrammar.fAnySimpleType);
        }

        if (base.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
            if (base == SchemaGrammar.fAnyType)
                base = SchemaGrammar.fAnySimpleType;
            else
                return false;
        }
        return checkSimpleDerivation(derived, (XSSimpleType)base, block);
    }

    /**
     * check whether complex type derived is valid derived from base,
     * given a subset of {restriction, extension}.
     */
    public static boolean checkComplexDerivationOk(XSComplexTypeDecl derived, XSTypeDefinition base, short block) {
        if (derived == SchemaGrammar.fAnyType)
            return derived == base;
        return checkComplexDerivation(derived, base, block);
    }

    /**
     * Note: this will be a private method, and it assumes that derived is not
     *       anySimpleType, and base is not anyType. Another method will be
     *       introduced for public use, which will call this method.
     */
    private static boolean checkSimpleDerivation(XSSimpleType derived, XSSimpleType base, short block) {
        if (derived == base)
            return true;

        if ((block & XSConstants.DERIVATION_RESTRICTION) != 0 ||
                (derived.getBaseType().getFinal() & XSConstants.DERIVATION_RESTRICTION) != 0) {
            return false;
        }

        XSSimpleType directBase = (XSSimpleType)derived.getBaseType();
        if (directBase == base)
            return true;

        if (directBase != SchemaGrammar.fAnySimpleType &&
                checkSimpleDerivation(directBase, base, block)) {
            return true;
        }

        if ((derived.getVariety() == XSSimpleType.VARIETY_LIST ||
                derived.getVariety() == XSSimpleType.VARIETY_UNION) &&
                base == SchemaGrammar.fAnySimpleType) {
            return true;
        }

        if (base.getVariety() == XSSimpleType.VARIETY_UNION) {
            XSObjectList subUnionMemberDV = base.getMemberTypes();
            int subUnionSize = subUnionMemberDV.getLength();
            for (int i=0; i<subUnionSize; i++) {
                base = (XSSimpleType)subUnionMemberDV.item(i);
                if (checkSimpleDerivation(derived, base, block))
                    return true;
            }
        }

        return false;
    }

    /**
     * Note: this will be a private method, and it assumes that derived is not
     *       anyType. Another method will be introduced for public use,
     *       which will call this method.
     */
    private static boolean checkComplexDerivation(XSComplexTypeDecl derived, XSTypeDefinition base, short block) {
        if (derived == base)
            return true;

        if ((derived.fDerivedBy & block) != 0)
            return false;

        XSTypeDefinition directBase = derived.fBaseType;
        if (directBase == base)
            return true;

        if (directBase == SchemaGrammar.fAnyType ||
                directBase == SchemaGrammar.fAnySimpleType) {
            return false;
        }

        if (directBase.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE)
            return checkComplexDerivation((XSComplexTypeDecl)directBase, base, block);

        if (directBase.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
            if (base.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                if (base == SchemaGrammar.fAnyType)
                    base = SchemaGrammar.fAnySimpleType;
                else
                    return false;
            }
            return checkSimpleDerivation((XSSimpleType)directBase,
                    (XSSimpleType)base, block);
        }

        return false;
    }

    /**
     * check whether a value is a valid default for some type
     * returns the compiled form of the value
     * The parameter value could be either a String or a ValidatedInfo object
     */
    public static Object ElementDefaultValidImmediate(XSTypeDefinition type, String value, ValidationContext context, ValidatedInfo vinfo) {

        XSSimpleType dv = null;

        if (type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
            dv = (XSSimpleType)type;
        }

        else {
            XSComplexTypeDecl ctype = (XSComplexTypeDecl)type;
            if (ctype.fContentType == XSComplexTypeDecl.CONTENTTYPE_SIMPLE) {
                dv = ctype.fXSSimpleType;
            }
            else if (ctype.fContentType == XSComplexTypeDecl.CONTENTTYPE_MIXED) {
                if (!((XSParticleDecl)ctype.getParticle()).emptiable())
                    return null;
            }
            else {
                return null;
            }
        }

        Object actualValue = null;
        if (dv == null) {
            dv = STRING_TYPE;
        }
        try {
            actualValue = dv.validate(value, context, vinfo);
            if (vinfo != null)
                actualValue = dv.validate(vinfo.stringValue(), context, vinfo);
        } catch (InvalidDatatypeValueException ide) {
            return null;
        }

        return actualValue;
    }

    static void reportSchemaError(XMLErrorReporter errorReporter,
            SimpleLocator loc,
            String key, Object[] args) {
        if (loc != null) {
            errorReporter.reportError(loc, XSMessageFormatter.SCHEMA_DOMAIN,
                    key, args, XMLErrorReporter.SEVERITY_ERROR);
        }
        else {
            errorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN,
                    key, args, XMLErrorReporter.SEVERITY_ERROR);
        }
    }

    /**
     * used to check the 3 constraints against each complex type
     * (should be each model group):
     * Unique Particle Attribution, Particle Derivation (Restriction),
     * Element Declrations Consistent.
     */
    public static void fullSchemaChecking(XSGrammarBucket grammarBucket,
            SubstitutionGroupHandler SGHandler,
            CMBuilder cmBuilder,
            XMLErrorReporter errorReporter) {
        SchemaGrammar[] grammars = grammarBucket.getGrammars();
        for (int i = grammars.length-1; i >= 0; i--) {
            SGHandler.addSubstitutionGroup(grammars[i].getSubstitutionGroups());
        }

        XSParticleDecl fakeDerived = new XSParticleDecl();
        XSParticleDecl fakeBase = new XSParticleDecl();
        fakeDerived.fType = XSParticleDecl.PARTICLE_MODELGROUP;
        fakeBase.fType = XSParticleDecl.PARTICLE_MODELGROUP;
        for (int g = grammars.length-1; g >= 0; g--) {
            XSGroupDecl [] redefinedGroups = grammars[g].getRedefinedGroupDecls();
            SimpleLocator [] rgLocators = grammars[g].getRGLocators();
            for(int i=0; i<redefinedGroups.length; ) {
                XSGroupDecl derivedGrp = redefinedGroups[i++];
                XSModelGroupImpl derivedMG = derivedGrp.fModelGroup;
                XSGroupDecl baseGrp = redefinedGroups[i++];
                XSModelGroupImpl baseMG = baseGrp.fModelGroup;
                fakeDerived.fValue = derivedMG;
                fakeBase.fValue = baseMG;
                if(baseMG == null) {
                    if(derivedMG != null) { 
                        reportSchemaError(errorReporter, rgLocators[i/2-1],
                                "src-redefine.6.2.2",
                                new Object[]{derivedGrp.fName, "rcase-Recurse.2"});
                    }
                } else if (derivedMG == null) {
                    if (!fakeBase.emptiable()) {
                        reportSchemaError(errorReporter, rgLocators[i/2-1],
                                "src-redefine.6.2.2",
                                new Object[]{derivedGrp.fName, "rcase-Recurse.2"});
                    }
                } else {
                    try {
                        particleValidRestriction(fakeDerived, SGHandler, fakeBase, SGHandler);
                    } catch (XMLSchemaException e) {
                        String key = e.getKey();
                        reportSchemaError(errorReporter, rgLocators[i/2-1],
                                key,
                                e.getArgs());
                        reportSchemaError(errorReporter, rgLocators[i/2-1],
                                "src-redefine.6.2.2",
                                new Object[]{derivedGrp.fName, key});
                    }
                }
            }
        }

        XSComplexTypeDecl[] types;
        SimpleLocator [] ctLocators;
        boolean further, fullChecked;
        int keepType;
        SymbolHash elemTable = new SymbolHash();
        for (int i = grammars.length-1, j; i >= 0; i--) {
            keepType = 0;
            fullChecked = grammars[i].fFullChecked;
            types = grammars[i].getUncheckedComplexTypeDecls();
            ctLocators = grammars[i].getUncheckedCTLocators();
            for (j = 0; j < types.length; j++) {
                if (!fullChecked) {
                    if (types[j].fParticle!=null) {
                        elemTable.clear();
                        try {
                            checkElementDeclsConsistent(types[j], types[j].fParticle,
                                    elemTable, SGHandler);
                        }
                        catch (XMLSchemaException e) {
                            reportSchemaError(errorReporter, ctLocators[j],
                                    e.getKey(),
                                    e.getArgs());
                        }
                    }
                }


                if (types[j].fBaseType != null &&
                        types[j].fBaseType != SchemaGrammar.fAnyType &&
                        types[j].fDerivedBy == XSConstants.DERIVATION_RESTRICTION &&
                        (types[j].fBaseType instanceof XSComplexTypeDecl)) {

                    XSParticleDecl derivedParticle=types[j].fParticle;
                    XSParticleDecl baseParticle=
                        ((XSComplexTypeDecl)(types[j].fBaseType)).fParticle;
                    if (derivedParticle==null) {
                        if (baseParticle!=null && !baseParticle.emptiable()) {
                            reportSchemaError(errorReporter,ctLocators[j],
                                    "derivation-ok-restriction.5.3.2",
                                    new Object[]{types[j].fName, types[j].fBaseType.getName()});
                        }
                    }
                    else if (baseParticle!=null) {
                        try {
                            particleValidRestriction(types[j].fParticle,
                                    SGHandler,
                                    ((XSComplexTypeDecl)(types[j].fBaseType)).fParticle,
                                    SGHandler);
                        } catch (XMLSchemaException e) {
                            reportSchemaError(errorReporter, ctLocators[j],
                                    e.getKey(),
                                    e.getArgs());
                            reportSchemaError(errorReporter, ctLocators[j],
                                    "derivation-ok-restriction.5.4.2",
                                    new Object[]{types[j].fName});
                        }
                    }
                    else {
                        reportSchemaError(errorReporter, ctLocators[j],
                                "derivation-ok-restriction.5.4.2",
                                new Object[]{types[j].fName});
                    }
                }
                XSCMValidator cm = types[j].getContentModel(cmBuilder, true);
                further = false;
                if (cm != null) {
                    try {
                        further = cm.checkUniqueParticleAttribution(SGHandler);
                    } catch (XMLSchemaException e) {
                        reportSchemaError(errorReporter, ctLocators[j],
                                e.getKey(),
                                e.getArgs());
                    }
                }
                /*for (k = errors.getErrorCodeNum()-1; k >= 0; k--) {
                    reportSchemaError(errorReporter, ctLocators[j],
                                      errors.getErrorCode(k),
                                      errors.getArgs(k));
                }*/

                if (!fullChecked && further)
                    types[keepType++] = types[j];

            }
            if (!fullChecked) {
                grammars[i].setUncheckedTypeNum(keepType);
                grammars[i].fFullChecked = true;
            }
        }
    }

    /*
       Check that a given particle is a valid restriction of a base particle.
     */

    public static void checkElementDeclsConsistent(XSComplexTypeDecl type,
            XSParticleDecl particle,
            SymbolHash elemDeclHash,
            SubstitutionGroupHandler sgHandler)
        throws XMLSchemaException {


        int pType = particle.fType;

        if (pType == XSParticleDecl.PARTICLE_WILDCARD)
            return;

        if (pType == XSParticleDecl.PARTICLE_ELEMENT) {
            XSElementDecl elem = (XSElementDecl)(particle.fValue);
            findElemInTable(type, elem, elemDeclHash);

            if (elem.fScope == XSConstants.SCOPE_GLOBAL) {
                XSElementDecl[] subGroup = sgHandler.getSubstitutionGroup(elem);
                for (int i = 0; i < subGroup.length; i++) {
                    findElemInTable(type, subGroup[i], elemDeclHash);
                }
            }
            return;
        }

        XSModelGroupImpl group = (XSModelGroupImpl)particle.fValue;
        for (int i = 0; i < group.fParticleCount; i++)
            checkElementDeclsConsistent(type, group.fParticles[i], elemDeclHash, sgHandler);
    }

    public static void findElemInTable(XSComplexTypeDecl type, XSElementDecl elem,
            SymbolHash elemDeclHash)
        throws XMLSchemaException {

        String name = elem.fName + "," + elem.fTargetNamespace;

        XSElementDecl existingElem = null;
        if ((existingElem = (XSElementDecl)(elemDeclHash.get(name))) == null) {
            elemDeclHash.put(name, elem);
        }
        else {
            if (elem == existingElem)
                return;

            if (elem.fType != existingElem.fType) {
                throw new XMLSchemaException("cos-element-consistent",
                        new Object[] {type.fName, elem.fName});

            }
        }
    }

    private static boolean particleValidRestriction(XSParticleDecl dParticle,
            SubstitutionGroupHandler dSGHandler,
            XSParticleDecl bParticle,
            SubstitutionGroupHandler bSGHandler)
        throws XMLSchemaException {
        return particleValidRestriction(dParticle, dSGHandler, bParticle, bSGHandler, true);
    }

    private static boolean particleValidRestriction(XSParticleDecl dParticle,
            SubstitutionGroupHandler dSGHandler,
            XSParticleDecl bParticle,
            SubstitutionGroupHandler bSGHandler,
            boolean checkWCOccurrence)
        throws XMLSchemaException {

        List<XSParticleDecl> dChildren = null;
        List<XSParticleDecl> bChildren = null;
        int dMinEffectiveTotalRange=OCCURRENCE_UNKNOWN;
        int dMaxEffectiveTotalRange=OCCURRENCE_UNKNOWN;

        boolean bExpansionHappened = false;

        if (dParticle.isEmpty() && !bParticle.emptiable()) {
            throw new XMLSchemaException("cos-particle-restrict.a", null);
        }
        else if (!dParticle.isEmpty() && bParticle.isEmpty()) {
            throw new XMLSchemaException("cos-particle-restrict.b", null);
        }


        short dType = dParticle.fType;
        if (dType == XSParticleDecl.PARTICLE_MODELGROUP) {
            dType = ((XSModelGroupImpl)dParticle.fValue).fCompositor;

            XSParticleDecl dtmp = getNonUnaryGroup(dParticle);
            if (dtmp != dParticle) {
                dParticle = dtmp;
                dType = dParticle.fType;
                if (dType == XSParticleDecl.PARTICLE_MODELGROUP)
                    dType = ((XSModelGroupImpl)dParticle.fValue).fCompositor;
            }

            dChildren = removePointlessChildren(dParticle);
        }

        int dMinOccurs = dParticle.fMinOccurs;
        int dMaxOccurs = dParticle.fMaxOccurs;

        if (dSGHandler != null && dType == XSParticleDecl.PARTICLE_ELEMENT) {
            XSElementDecl dElement = (XSElementDecl)dParticle.fValue;

            if (dElement.fScope == XSConstants.SCOPE_GLOBAL) {
                XSElementDecl[] subGroup = dSGHandler.getSubstitutionGroup(dElement);
                if (subGroup.length >0 ) {
                    dType = XSModelGroupImpl.MODELGROUP_CHOICE;
                    dMinEffectiveTotalRange = dMinOccurs;
                    dMaxEffectiveTotalRange = dMaxOccurs;

                    dChildren = new ArrayList<>(subGroup.length+1);
                    for (int i = 0; i < subGroup.length; i++) {
                        addElementToParticleVector(dChildren, subGroup[i]);
                    }
                    addElementToParticleVector(dChildren, dElement);
                    Collections.sort(dChildren, ELEMENT_PARTICLE_COMPARATOR);

                    dSGHandler = null;
                }
            }
        }

        short bType = bParticle.fType;
        if (bType == XSParticleDecl.PARTICLE_MODELGROUP) {
            bType = ((XSModelGroupImpl)bParticle.fValue).fCompositor;

            XSParticleDecl btmp = getNonUnaryGroup(bParticle);
            if (btmp != bParticle) {
                bParticle = btmp;
                bType = bParticle.fType;
                if (bType == XSParticleDecl.PARTICLE_MODELGROUP)
                    bType = ((XSModelGroupImpl)bParticle.fValue).fCompositor;
            }

            bChildren = removePointlessChildren(bParticle);
        }

        int bMinOccurs = bParticle.fMinOccurs;
        int bMaxOccurs = bParticle.fMaxOccurs;

        if (bSGHandler != null && bType == XSParticleDecl.PARTICLE_ELEMENT) {
            XSElementDecl bElement = (XSElementDecl)bParticle.fValue;

            if (bElement.fScope == XSConstants.SCOPE_GLOBAL) {
                XSElementDecl[] bsubGroup = bSGHandler.getSubstitutionGroup(bElement);
                if (bsubGroup.length >0 ) {
                    bType = XSModelGroupImpl.MODELGROUP_CHOICE;

                    bChildren = new ArrayList<>(bsubGroup.length+1);
                    for (int i = 0; i < bsubGroup.length; i++) {
                        addElementToParticleVector(bChildren, bsubGroup[i]);
                    }
                    addElementToParticleVector(bChildren, bElement);
                    Collections.sort(bChildren, ELEMENT_PARTICLE_COMPARATOR);
                    bSGHandler = null;

                    bExpansionHappened = true;
                }
            }
        }

        switch (dType) {
            case XSParticleDecl.PARTICLE_ELEMENT:
            {
                switch (bType) {

                    case XSParticleDecl.PARTICLE_ELEMENT:
                    {
                        checkNameAndTypeOK((XSElementDecl)dParticle.fValue,dMinOccurs,dMaxOccurs,
                                (XSElementDecl)bParticle.fValue,bMinOccurs,bMaxOccurs);
                        return bExpansionHappened;
                    }

                    case XSParticleDecl.PARTICLE_WILDCARD:
                    {
                        checkNSCompat((XSElementDecl)dParticle.fValue,dMinOccurs,dMaxOccurs,
                                (XSWildcardDecl)bParticle.fValue,bMinOccurs,bMaxOccurs,
                                checkWCOccurrence);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_CHOICE:
                    {
                        dChildren = new ArrayList<>();
                        dChildren.add(dParticle);

                        checkRecurseLax(dChildren, 1, 1, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }
                    case XSModelGroupImpl.MODELGROUP_SEQUENCE:
                    case XSModelGroupImpl.MODELGROUP_ALL:
                    {
                        dChildren = new ArrayList<>();
                        dChildren.add(dParticle);

                        checkRecurse(dChildren, 1, 1, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }

                    default:
                    {
                        throw new XMLSchemaException("Internal-Error",
                                new Object[]{"in particleValidRestriction"});
                    }
                }
            }

            case XSParticleDecl.PARTICLE_WILDCARD:
            {
                switch (bType) {

                    case XSParticleDecl.PARTICLE_WILDCARD:
                    {
                        checkNSSubset((XSWildcardDecl)dParticle.fValue, dMinOccurs, dMaxOccurs,
                                (XSWildcardDecl)bParticle.fValue, bMinOccurs, bMaxOccurs);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_CHOICE:
                    case XSModelGroupImpl.MODELGROUP_SEQUENCE:
                    case XSModelGroupImpl.MODELGROUP_ALL:
                    case XSParticleDecl.PARTICLE_ELEMENT:
                    {
                        throw new XMLSchemaException("cos-particle-restrict.2",
                                new Object[]{"any:choice,sequence,all,elt"});
                    }

                    default:
                    {
                        throw new XMLSchemaException("Internal-Error",
                                new Object[]{"in particleValidRestriction"});
                    }
                }
            }

            case XSModelGroupImpl.MODELGROUP_ALL:
            {
                switch (bType) {

                    case XSParticleDecl.PARTICLE_WILDCARD:
                    {
                        if (dMinEffectiveTotalRange == OCCURRENCE_UNKNOWN)
                            dMinEffectiveTotalRange = dParticle.minEffectiveTotalRange();
                        if (dMaxEffectiveTotalRange == OCCURRENCE_UNKNOWN)
                            dMaxEffectiveTotalRange = dParticle.maxEffectiveTotalRange();

                        checkNSRecurseCheckCardinality(dChildren, dMinEffectiveTotalRange,
                                dMaxEffectiveTotalRange,
                                dSGHandler,
                                bParticle,bMinOccurs,bMaxOccurs,
                                checkWCOccurrence);

                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_ALL:
                    {
                        checkRecurse(dChildren, dMinOccurs, dMaxOccurs, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_CHOICE:
                    case XSModelGroupImpl.MODELGROUP_SEQUENCE:
                    case XSParticleDecl.PARTICLE_ELEMENT:
                    {
                        throw new XMLSchemaException("cos-particle-restrict.2",
                                new Object[]{"all:choice,sequence,elt"});
                    }

                    default:
                    {
                        throw new XMLSchemaException("Internal-Error",
                                new Object[]{"in particleValidRestriction"});
                    }
                }
            }

            case XSModelGroupImpl.MODELGROUP_CHOICE:
            {
                switch (bType) {

                    case XSParticleDecl.PARTICLE_WILDCARD:
                    {
                        if (dMinEffectiveTotalRange == OCCURRENCE_UNKNOWN)
                            dMinEffectiveTotalRange = dParticle.minEffectiveTotalRange();
                        if (dMaxEffectiveTotalRange == OCCURRENCE_UNKNOWN)
                            dMaxEffectiveTotalRange = dParticle.maxEffectiveTotalRange();

                        checkNSRecurseCheckCardinality(dChildren, dMinEffectiveTotalRange,
                                dMaxEffectiveTotalRange,
                                dSGHandler,
                                bParticle,bMinOccurs,bMaxOccurs,
                                checkWCOccurrence);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_CHOICE:
                    {
                        checkRecurseLax(dChildren, dMinOccurs, dMaxOccurs, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_ALL:
                    case XSModelGroupImpl.MODELGROUP_SEQUENCE:
                    case XSParticleDecl.PARTICLE_ELEMENT:
                    {
                        throw new XMLSchemaException("cos-particle-restrict.2",
                                new Object[]{"choice:all,sequence,elt"});
                    }

                    default:
                    {
                        throw new XMLSchemaException("Internal-Error",
                                new Object[]{"in particleValidRestriction"});
                    }
                }
            }


            case XSModelGroupImpl.MODELGROUP_SEQUENCE:
            {
                switch (bType) {

                    case XSParticleDecl.PARTICLE_WILDCARD:
                    {
                        if (dMinEffectiveTotalRange == OCCURRENCE_UNKNOWN)
                            dMinEffectiveTotalRange = dParticle.minEffectiveTotalRange();
                        if (dMaxEffectiveTotalRange == OCCURRENCE_UNKNOWN)
                            dMaxEffectiveTotalRange = dParticle.maxEffectiveTotalRange();

                        checkNSRecurseCheckCardinality(dChildren, dMinEffectiveTotalRange,
                                dMaxEffectiveTotalRange,
                                dSGHandler,
                                bParticle,bMinOccurs,bMaxOccurs,
                                checkWCOccurrence);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_ALL:
                    {
                        checkRecurseUnordered(dChildren, dMinOccurs, dMaxOccurs, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_SEQUENCE:
                    {
                        checkRecurse(dChildren, dMinOccurs, dMaxOccurs, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_CHOICE:
                    {
                        int min1 = dMinOccurs * dChildren.size();
                        int max1 = (dMaxOccurs == SchemaSymbols.OCCURRENCE_UNBOUNDED)?
                                dMaxOccurs : dMaxOccurs * dChildren.size();
                        checkMapAndSum(dChildren, min1, max1, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }

                    case XSParticleDecl.PARTICLE_ELEMENT:
                    {
                        throw new XMLSchemaException("cos-particle-restrict.2",
                                new Object[]{"seq:elt"});
                    }

                    default:
                    {
                        throw new XMLSchemaException("Internal-Error",
                                new Object[]{"in particleValidRestriction"});
                    }
                }
            }

        }

        return bExpansionHappened;
    }

    private static void addElementToParticleVector (List<XSParticleDecl> v, XSElementDecl d)  {

        XSParticleDecl p = new XSParticleDecl();
        p.fValue = d;
        p.fType = XSParticleDecl.PARTICLE_ELEMENT;
        v.add(p);

    }

    private static XSParticleDecl getNonUnaryGroup(XSParticleDecl p) {

        if (p.fType == XSParticleDecl.PARTICLE_ELEMENT ||
                p.fType == XSParticleDecl.PARTICLE_WILDCARD)
            return p;

        if (p.fMinOccurs==1 && p.fMaxOccurs==1 &&
                p.fValue!=null && ((XSModelGroupImpl)p.fValue).fParticleCount == 1)
            return getNonUnaryGroup(((XSModelGroupImpl)p.fValue).fParticles[0]);
        else
            return p;
    }

    private static List<XSParticleDecl> removePointlessChildren(XSParticleDecl p)  {

        if (p.fType == XSParticleDecl.PARTICLE_ELEMENT ||
                p.fType == XSParticleDecl.PARTICLE_WILDCARD)
            return null;

        List<XSParticleDecl> children = new ArrayList<>();

        XSModelGroupImpl group = (XSModelGroupImpl)p.fValue;
        for (int i = 0; i < group.fParticleCount; i++)
            gatherChildren(group.fCompositor, group.fParticles[i], children);

        return children;
    }


    private static void gatherChildren(int parentType, XSParticleDecl p, List<XSParticleDecl> children) {

        int min = p.fMinOccurs;
        int max = p.fMaxOccurs;
        int type = p.fType;
        if (type == XSParticleDecl.PARTICLE_MODELGROUP)
            type = ((XSModelGroupImpl)p.fValue).fCompositor;

        if (type == XSParticleDecl.PARTICLE_ELEMENT ||
                type== XSParticleDecl.PARTICLE_WILDCARD) {
            children.add(p);
            return;
        }

        if (! (min==1 && max==1)) {
            children.add(p);
        }
        else if (parentType == type) {
            XSModelGroupImpl group = (XSModelGroupImpl)p.fValue;
            for (int i = 0; i < group.fParticleCount; i++)
                gatherChildren(type, group.fParticles[i], children);
        }
        else if (!p.isEmpty()) {
            children.add(p);
        }

    }

    private static void checkNameAndTypeOK(XSElementDecl dElement, int dMin, int dMax,
            XSElementDecl bElement, int bMin, int bMax)
        throws XMLSchemaException {


        if (dElement.fName != bElement.fName ||
                dElement.fTargetNamespace != bElement.fTargetNamespace) {
            throw new XMLSchemaException(
                    "rcase-NameAndTypeOK.1",new Object[]{dElement.fName,
                            dElement.fTargetNamespace, bElement.fName, bElement.fTargetNamespace});
        }

        if (!bElement.getNillable() && dElement.getNillable()) {
            throw new XMLSchemaException("rcase-NameAndTypeOK.2",
                    new Object[]{dElement.fName});
        }

        if (!checkOccurrenceRange(dMin, dMax, bMin, bMax)) {
            throw new XMLSchemaException("rcase-NameAndTypeOK.3",
                    new Object[]{
                    dElement.fName,
                    Integer.toString(dMin),
                    dMax==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(dMax),
                            Integer.toString(bMin),
                            bMax==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(bMax)});
        }

        if (bElement.getConstraintType() == XSConstants.VC_FIXED) {
            if (dElement.getConstraintType() != XSConstants.VC_FIXED) {
                throw new XMLSchemaException("rcase-NameAndTypeOK.4.a",
                        new Object[]{dElement.fName, bElement.fDefault.stringValue()});
            }

            boolean isSimple = false;
            if (dElement.fType.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE ||
                    ((XSComplexTypeDecl)dElement.fType).fContentType == XSComplexTypeDecl.CONTENTTYPE_SIMPLE) {
                isSimple = true;
            }

            if (!isSimple && !bElement.fDefault.normalizedValue.equals(dElement.fDefault.normalizedValue) ||
                    isSimple && !bElement.fDefault.actualValue.equals(dElement.fDefault.actualValue)) {
                throw new XMLSchemaException("rcase-NameAndTypeOK.4.b",
                        new Object[]{dElement.fName,
                        dElement.fDefault.stringValue(),
                        bElement.fDefault.stringValue()});
            }
        }

        checkIDConstraintRestriction(dElement, bElement);

        int blockSet1 = dElement.fBlock;
        int blockSet2 = bElement.fBlock;
        if (((blockSet1 & blockSet2)!=blockSet2) ||
                (blockSet1==XSConstants.DERIVATION_NONE && blockSet2!=XSConstants.DERIVATION_NONE))
            throw new XMLSchemaException("rcase-NameAndTypeOK.6",
                    new Object[]{dElement.fName});


        if (!checkTypeDerivationOk(dElement.fType, bElement.fType,
                (short)(XSConstants.DERIVATION_EXTENSION|XSConstants.DERIVATION_LIST|XSConstants.DERIVATION_UNION))) {
            throw new XMLSchemaException("rcase-NameAndTypeOK.7",
                    new Object[]{dElement.fName, dElement.fType.getName(), bElement.fType.getName()});
        }

    }


    private static void checkIDConstraintRestriction(XSElementDecl derivedElemDecl,
            XSElementDecl baseElemDecl)
        throws XMLSchemaException {
    } 


    private static boolean checkOccurrenceRange(int min1, int max1, int min2, int max2) {

        if ((min1 >= min2) &&
                ((max2==SchemaSymbols.OCCURRENCE_UNBOUNDED) ||
                        (max1!=SchemaSymbols.OCCURRENCE_UNBOUNDED && max1<=max2)))
            return true;
        else
            return false;
    }

    private static void checkNSCompat(XSElementDecl elem, int min1, int max1,
            XSWildcardDecl wildcard, int min2, int max2,
            boolean checkWCOccurrence)
        throws XMLSchemaException {

        if (checkWCOccurrence && !checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-NSCompat.2",
                    new Object[]{
                    elem.fName,
                    Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                            Integer.toString(min2),
                            max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        if (!wildcard.allowNamespace(elem.fTargetNamespace))  {
            throw new XMLSchemaException("rcase-NSCompat.1",
                    new Object[]{elem.fName,elem.fTargetNamespace});
        }

    }

    private static void checkNSSubset(XSWildcardDecl dWildcard, int min1, int max1,
            XSWildcardDecl bWildcard, int min2, int max2)
        throws XMLSchemaException {

        if (!checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-NSSubset.2", new Object[]{
                    Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                            Integer.toString(min2),
                            max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        if (!dWildcard.isSubsetOf(bWildcard)) {
            throw new XMLSchemaException("rcase-NSSubset.1", null);
        }

        if (dWildcard.weakerProcessContents(bWildcard)) {
            throw new XMLSchemaException("rcase-NSSubset.3",
                    new Object[]{dWildcard.getProcessContentsAsString(),
                    bWildcard.getProcessContentsAsString()});
        }

    }


    private static void checkNSRecurseCheckCardinality(List<XSParticleDecl> children, int min1, int max1,
            SubstitutionGroupHandler dSGHandler,
            XSParticleDecl wildcard, int min2, int max2,
            boolean checkWCOccurrence)
        throws XMLSchemaException {


        if (checkWCOccurrence && !checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-NSRecurseCheckCardinality.2", new Object[]{
                    Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                            Integer.toString(min2),
                            max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        int count = children.size();
        try {
            for (int i = 0; i < count; i++) {
                XSParticleDecl particle1 = children.get(i);
                particleValidRestriction(particle1, dSGHandler, wildcard, null, false);

            }
        }
        catch (XMLSchemaException e) {
            throw new XMLSchemaException("rcase-NSRecurseCheckCardinality.1", null);
        }

    }

    private static void checkRecurse(List<XSParticleDecl> dChildren, int min1, int max1,
            SubstitutionGroupHandler dSGHandler,
            List<XSParticleDecl> bChildren, int min2, int max2,
            SubstitutionGroupHandler bSGHandler)
        throws XMLSchemaException {

        if (!checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-Recurse.1", new Object[]{
                    Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                    Integer.toString(min2),
                    max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        int count1= dChildren.size();
        int count2= bChildren.size();

        int current = 0;
        label: for (int i = 0; i<count1; i++) {

            XSParticleDecl particle1 = dChildren.get(i);
            for (int j = current; j<count2; j++) {
                XSParticleDecl particle2 = bChildren.get(j);
                current +=1;
                try {
                    particleValidRestriction(particle1, dSGHandler, particle2, bSGHandler);
                    continue label;
                }
                catch (XMLSchemaException e) {
                    if (!particle2.emptiable())
                        throw new XMLSchemaException("rcase-Recurse.2", null);
                }
            }
            throw new XMLSchemaException("rcase-Recurse.2", null);
        }

        for (int j=current; j < count2; j++) {
            XSParticleDecl particle2 = bChildren.get(j);
            if (!particle2.emptiable()) {
                throw new XMLSchemaException("rcase-Recurse.2", null);
            }
        }

    }

    private static void checkRecurseUnordered(List<XSParticleDecl> dChildren, int min1, int max1,
            SubstitutionGroupHandler dSGHandler,
            List<XSParticleDecl> bChildren, int min2, int max2,
            SubstitutionGroupHandler bSGHandler)
        throws XMLSchemaException {


        if (!checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-RecurseUnordered.1", new Object[]{
                    Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                    Integer.toString(min2),
                    max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        int count1= dChildren.size();
        int count2 = bChildren.size();

        boolean foundIt[] = new boolean[count2];

        label: for (int i = 0; i<count1; i++) {
            XSParticleDecl particle1 = dChildren.get(i);

            for (int j = 0; j<count2; j++) {
                XSParticleDecl particle2 = bChildren.get(j);
                try {
                    particleValidRestriction(particle1, dSGHandler, particle2, bSGHandler);
                    if (foundIt[j])
                        throw new XMLSchemaException("rcase-RecurseUnordered.2", null);
                    else
                        foundIt[j]=true;

                    continue label;
                }
                catch (XMLSchemaException e) {
                }
            }
            throw new XMLSchemaException("rcase-RecurseUnordered.2", null);
        }

        for (int j=0; j < count2; j++) {
            XSParticleDecl particle2 = bChildren.get(j);
            if (!foundIt[j] && !particle2.emptiable()) {
                throw new XMLSchemaException("rcase-RecurseUnordered.2", null);
            }
        }

    }

    private static void checkRecurseLax(List<XSParticleDecl> dChildren, int min1, int max1,
            SubstitutionGroupHandler dSGHandler,
            List<XSParticleDecl> bChildren, int min2, int max2,
            SubstitutionGroupHandler  bSGHandler)
        throws XMLSchemaException {

        if (!checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-RecurseLax.1", new Object[]{
                    Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                            Integer.toString(min2),
                            max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        int count1= dChildren.size();
        int count2 = bChildren.size();

        int current = 0;
        label: for (int i = 0; i<count1; i++) {

            XSParticleDecl particle1 = dChildren.get(i);
            for (int j = current; j<count2; j++) {
                XSParticleDecl particle2 = bChildren.get(j);
                current +=1;
                try {
                    if (particleValidRestriction(particle1, dSGHandler, particle2, bSGHandler))
                        current--;
                    continue label;
                }
                catch (XMLSchemaException e) {
                }
            }
            throw new XMLSchemaException("rcase-RecurseLax.2", null);

        }

    }

    private static void checkMapAndSum(List<XSParticleDecl> dChildren, int min1, int max1,
            SubstitutionGroupHandler dSGHandler,
            List<XSParticleDecl> bChildren, int min2, int max2,
            SubstitutionGroupHandler bSGHandler)
        throws XMLSchemaException {



        if (!checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-MapAndSum.2",
                    new Object[]{Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                            Integer.toString(min2),
                            max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        int count1 = dChildren.size();
        int count2 = bChildren.size();

        label: for (int i = 0; i<count1; i++) {

            XSParticleDecl particle1 = dChildren.get(i);
            for (int j = 0; j<count2; j++) {
                XSParticleDecl particle2 = bChildren.get(j);
                try {
                    particleValidRestriction(particle1, dSGHandler, particle2, bSGHandler);
                    continue label;
                }
                catch (XMLSchemaException e) {
                }
            }
            throw new XMLSchemaException("rcase-MapAndSum.1", null);
        }
    }
    public static boolean overlapUPA(XSElementDecl element1,
            XSElementDecl element2,
            SubstitutionGroupHandler sgHandler) {
        if (element1.fName == element2.fName &&
                element1.fTargetNamespace == element2.fTargetNamespace) {
            return true;
        }

        XSElementDecl[] subGroup = sgHandler.getSubstitutionGroup(element1);
        for (int i = subGroup.length-1; i >= 0; i--) {
            if (subGroup[i].fName == element2.fName &&
                    subGroup[i].fTargetNamespace == element2.fTargetNamespace) {
                return true;
            }
        }

        subGroup = sgHandler.getSubstitutionGroup(element2);
        for (int i = subGroup.length-1; i >= 0; i--) {
            if (subGroup[i].fName == element1.fName &&
                    subGroup[i].fTargetNamespace == element1.fTargetNamespace) {
                return true;
            }
        }

        return false;
    }

    public static boolean overlapUPA(XSElementDecl element,
            XSWildcardDecl wildcard,
            SubstitutionGroupHandler sgHandler) {
        if (wildcard.allowNamespace(element.fTargetNamespace))
            return true;

        XSElementDecl[] subGroup = sgHandler.getSubstitutionGroup(element);
        for (int i = subGroup.length-1; i >= 0; i--) {
            if (wildcard.allowNamespace(subGroup[i].fTargetNamespace))
                return true;
        }

        return false;
    }

    public static boolean overlapUPA(XSWildcardDecl wildcard1,
            XSWildcardDecl wildcard2) {
        XSWildcardDecl intersect = wildcard1.performIntersectionWith(wildcard2, wildcard1.fProcessContents);
        if (intersect == null ||
                intersect.fType != XSWildcardDecl.NSCONSTRAINT_LIST ||
                intersect.fNamespaceList.length != 0) {
            return true;
        }

        return false;
    }

    public static boolean overlapUPA(Object decl1, Object decl2,
            SubstitutionGroupHandler sgHandler) {
        if (decl1 instanceof XSElementDecl) {
            if (decl2 instanceof XSElementDecl) {
                return overlapUPA((XSElementDecl)decl1,
                        (XSElementDecl)decl2,
                        sgHandler);
            }
            else {
                return overlapUPA((XSElementDecl)decl1,
                        (XSWildcardDecl)decl2,
                        sgHandler);
            }
        }
        else {
            if (decl2 instanceof XSElementDecl) {
                return overlapUPA((XSElementDecl)decl2,
                        (XSWildcardDecl)decl1,
                        sgHandler);
            }
            else {
                return overlapUPA((XSWildcardDecl)decl1,
                        (XSWildcardDecl)decl2);
            }
        }
    }

} 
