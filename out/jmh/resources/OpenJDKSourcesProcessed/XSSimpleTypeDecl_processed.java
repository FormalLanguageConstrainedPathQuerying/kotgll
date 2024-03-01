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

package com.sun.org.apache.xerces.internal.impl.dv.xs;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.dv.DatatypeException;
import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeFacetException;
import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.ValidatedInfo;
import com.sun.org.apache.xerces.internal.impl.dv.ValidationContext;
import com.sun.org.apache.xerces.internal.impl.dv.XSFacets;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.RegularExpression;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.util.ObjectListImpl;
import com.sun.org.apache.xerces.internal.impl.xs.util.ShortListImpl;
import com.sun.org.apache.xerces.internal.impl.xs.util.StringListImpl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xs.ShortList;
import com.sun.org.apache.xerces.internal.xs.StringList;
import com.sun.org.apache.xerces.internal.xs.XSAnnotation;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSFacet;
import com.sun.org.apache.xerces.internal.xs.XSMultiValueFacet;
import com.sun.org.apache.xerces.internal.xs.XSNamespaceItem;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.datatypes.ObjectList;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import org.w3c.dom.TypeInfo;

/**
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 * @author Neeraj Bajaj, Sun Microsystems, inc.
 *
 * @LastModified: Nov 2017
 */
public class XSSimpleTypeDecl implements XSSimpleType, TypeInfo {

    protected static final short DV_STRING        = PRIMITIVE_STRING;
    protected static final short DV_BOOLEAN       = PRIMITIVE_BOOLEAN;
    protected static final short DV_DECIMAL       = PRIMITIVE_DECIMAL;
    protected static final short DV_FLOAT         = PRIMITIVE_FLOAT;
    protected static final short DV_DOUBLE        = PRIMITIVE_DOUBLE;
    protected static final short DV_DURATION      = PRIMITIVE_DURATION;
    protected static final short DV_DATETIME      = PRIMITIVE_DATETIME;
    protected static final short DV_TIME          = PRIMITIVE_TIME;
    protected static final short DV_DATE          = PRIMITIVE_DATE;
    protected static final short DV_GYEARMONTH    = PRIMITIVE_GYEARMONTH;
    protected static final short DV_GYEAR         = PRIMITIVE_GYEAR;
    protected static final short DV_GMONTHDAY     = PRIMITIVE_GMONTHDAY;
    protected static final short DV_GDAY          = PRIMITIVE_GDAY;
    protected static final short DV_GMONTH        = PRIMITIVE_GMONTH;
    protected static final short DV_HEXBINARY     = PRIMITIVE_HEXBINARY;
    protected static final short DV_BASE64BINARY  = PRIMITIVE_BASE64BINARY;
    protected static final short DV_ANYURI        = PRIMITIVE_ANYURI;
    protected static final short DV_QNAME         = PRIMITIVE_QNAME;
    protected static final short DV_PRECISIONDECIMAL = PRIMITIVE_PRECISIONDECIMAL;
    protected static final short DV_NOTATION      = PRIMITIVE_NOTATION;

    protected static final short DV_ANYSIMPLETYPE = 0;
    protected static final short DV_ID            = DV_NOTATION + 1;
    protected static final short DV_IDREF         = DV_NOTATION + 2;
    protected static final short DV_ENTITY        = DV_NOTATION + 3;
    protected static final short DV_INTEGER       = DV_NOTATION + 4;
    protected static final short DV_LIST          = DV_NOTATION + 5;
    protected static final short DV_UNION         = DV_NOTATION + 6;
    protected static final short DV_YEARMONTHDURATION = DV_NOTATION + 7;
    protected static final short DV_DAYTIMEDURATION     = DV_NOTATION + 8;
    protected static final short DV_ANYATOMICTYPE = DV_NOTATION + 9;

    private static final TypeValidator[] gDVs = {
        new AnySimpleDV(),
        new StringDV(),
        new BooleanDV(),
        new DecimalDV(),
        new FloatDV(),
        new DoubleDV(),
        new DurationDV(),
        new DateTimeDV(),
        new TimeDV(),
        new DateDV(),
        new YearMonthDV(),
        new YearDV(),
        new MonthDayDV(),
        new DayDV(),
        new MonthDV(),
        new HexBinaryDV(),
        new Base64BinaryDV(),
        new AnyURIDV(),
        new QNameDV(),
        new PrecisionDecimalDV(), 
        new QNameDV(),   
        new IDDV(),
        new IDREFDV(),
        new EntityDV(),
        new IntegerDV(),
        new ListDV(),
        new UnionDV(),
        new YearMonthDurationDV(), 
        new DayTimeDurationDV(), 
        new AnyAtomicDV() 
    };

    static final short NORMALIZE_NONE = 0;
    static final short NORMALIZE_TRIM = 1;
    static final short NORMALIZE_FULL = 2;
    static final short[] fDVNormalizeType = {
        NORMALIZE_NONE, 
        NORMALIZE_FULL, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_NONE, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_FULL, 
        NORMALIZE_NONE, 
        NORMALIZE_TRIM, 
        NORMALIZE_TRIM, 
        NORMALIZE_NONE, 
    };

    static final short SPECIAL_PATTERN_NONE     = 0;
    static final short SPECIAL_PATTERN_NMTOKEN  = 1;
    static final short SPECIAL_PATTERN_NAME     = 2;
    static final short SPECIAL_PATTERN_NCNAME   = 3;

    static final String[] SPECIAL_PATTERN_STRING   = {
        "NONE", "NMTOKEN", "Name", "NCName"
    };

    static final String[] WS_FACET_STRING = {
        "preserve", "replace", "collapse"
    };

    static final String URI_SCHEMAFORSCHEMA = "http:
    static final String ANY_TYPE = "anyType";

    public static final short YEARMONTHDURATION_DT      = 46;
    public static final short DAYTIMEDURATION_DT        = 47;
    public static final short PRECISIONDECIMAL_DT       = 48;
    public static final short ANYATOMICTYPE_DT          = 49;

    static final int DERIVATION_ANY = 0;
    static final int DERIVATION_RESTRICTION = 1;
    static final int DERIVATION_EXTENSION = 2;
    static final int DERIVATION_UNION = 4;
    static final int DERIVATION_LIST = 8;

    static final ValidationContext fEmptyContext = new ValidationContext() {
        public boolean needFacetChecking() {
            return true;
        }
        public boolean needExtraChecking() {
            return false;
        }
        public boolean needToNormalize() {
            return true;
        }
        public boolean useNamespaces () {
            return true;
        }
        public boolean isEntityDeclared (String name) {
            return false;
        }
        public boolean isEntityUnparsed (String name) {
            return false;
        }
        public boolean isIdDeclared (String name) {
            return false;
        }
        public void addId(String name) {
        }
        public void addIdRef(String name) {
        }
        public String getSymbol (String symbol) {
            return symbol.intern();
        }
        public String getURI(String prefix) {
            return null;
        }
        public Locale getLocale() {
            return Locale.getDefault();
        }
    };

    protected static TypeValidator[] getGDVs() {
        return gDVs.clone();
    }
    private TypeValidator[] fDVs = gDVs;
    protected void setDVs(TypeValidator[] dvs) {
        fDVs = dvs;
    }

    private boolean fIsImmutable = false;

    private XSSimpleTypeDecl fItemType;
    private XSSimpleTypeDecl[] fMemberTypes;
    private short fBuiltInKind;

    private String fTypeName;
    private String fTargetNamespace;
    private short fFinalSet = 0;
    private XSSimpleTypeDecl fBase;
    private short fVariety = -1;
    private short fValidationDV = -1;

    private short fFacetsDefined = 0;
    private short fFixedFacet = 0;

    private short fWhiteSpace = 0;
    private int fLength = -1;
    private int fMinLength = -1;
    private int fMaxLength = -1;
    private int fTotalDigits = -1;
    private int fFractionDigits = -1;
    private List<RegularExpression> fPattern;
    private List<String> fPatternStr;
    private ValidatedInfo[] fEnumeration;
    private int fEnumerationSize;
    private ShortList fEnumerationTypeList;
    private ObjectList fEnumerationItemTypeList;
    private StringList fLexicalPattern;
    private StringList fLexicalEnumeration;
    private ObjectList fActualEnumeration;
    private Object fMaxInclusive;
    private Object fMaxExclusive;
    private Object fMinExclusive;
    private Object fMinInclusive;

    public XSAnnotation lengthAnnotation;
    public XSAnnotation minLengthAnnotation;
    public XSAnnotation maxLengthAnnotation;
    public XSAnnotation whiteSpaceAnnotation;
    public XSAnnotation totalDigitsAnnotation;
    public XSAnnotation fractionDigitsAnnotation;
    public XSObjectListImpl patternAnnotations;
    public XSObjectList enumerationAnnotations;
    public XSAnnotation maxInclusiveAnnotation;
    public XSAnnotation maxExclusiveAnnotation;
    public XSAnnotation minInclusiveAnnotation;
    public XSAnnotation minExclusiveAnnotation;

    private XSObjectListImpl fFacets;

    private XSObjectListImpl fMultiValueFacets;

    private XSObjectList fAnnotations = null;

    private short fPatternType = SPECIAL_PATTERN_NONE;

    private short fOrdered;
    private boolean fFinite;
    private boolean fBounded;
    private boolean fNumeric;

    private XSNamespaceItem fNamespaceItem = null;

    public XSSimpleTypeDecl(){}

    protected XSSimpleTypeDecl(XSSimpleTypeDecl base, String name, short validateDV,
            short ordered, boolean bounded, boolean finite,
            boolean numeric, boolean isImmutable, short builtInKind) {
        fIsImmutable = isImmutable;
        fBase = base;
        fTypeName = name;
        fTargetNamespace = URI_SCHEMAFORSCHEMA;
        fVariety = VARIETY_ATOMIC;
        fValidationDV = validateDV;
        fFacetsDefined = FACET_WHITESPACE;
        if (validateDV == DV_ANYSIMPLETYPE ||
            validateDV == DV_ANYATOMICTYPE ||
            validateDV == DV_STRING) {
            fWhiteSpace = WS_PRESERVE;
        }
        else {
            fWhiteSpace = WS_COLLAPSE;
            fFixedFacet = FACET_WHITESPACE;
        }
        this.fOrdered = ordered;
        this.fBounded = bounded;
        this.fFinite = finite;
        this.fNumeric = numeric;
        fAnnotations = null;

        fBuiltInKind = builtInKind;
    }

    protected XSSimpleTypeDecl(XSSimpleTypeDecl base, String name, String uri, short finalSet, boolean isImmutable,
            XSObjectList annotations, short builtInKind) {
        this(base, name, uri, finalSet, isImmutable, annotations);
        fBuiltInKind = builtInKind;
    }

    protected XSSimpleTypeDecl(XSSimpleTypeDecl base, String name, String uri, short finalSet, boolean isImmutable,
            XSObjectList annotations) {
        fBase = base;
        fTypeName = name;
        fTargetNamespace = uri;
        fFinalSet = finalSet;
        fAnnotations = annotations;

        fVariety = fBase.fVariety;
        fValidationDV = fBase.fValidationDV;
        switch (fVariety) {
            case VARIETY_ATOMIC:
                break;
            case VARIETY_LIST:
                fItemType = fBase.fItemType;
                break;
            case VARIETY_UNION:
                fMemberTypes = fBase.fMemberTypes;
                break;
        }

        fLength = fBase.fLength;
        fMinLength = fBase.fMinLength;
        fMaxLength = fBase.fMaxLength;
        fPattern = fBase.fPattern;
        fPatternStr = fBase.fPatternStr;
        fEnumeration = fBase.fEnumeration;
        fEnumerationSize = fBase.fEnumerationSize;
        fWhiteSpace = fBase.fWhiteSpace;
        fMaxExclusive = fBase.fMaxExclusive;
        fMaxInclusive = fBase.fMaxInclusive;
        fMinExclusive = fBase.fMinExclusive;
        fMinInclusive = fBase.fMinInclusive;
        fTotalDigits = fBase.fTotalDigits;
        fFractionDigits = fBase.fFractionDigits;
        fPatternType = fBase.fPatternType;
        fFixedFacet = fBase.fFixedFacet;
        fFacetsDefined = fBase.fFacetsDefined;

        lengthAnnotation = fBase.lengthAnnotation;
        minLengthAnnotation = fBase.minLengthAnnotation;
        maxLengthAnnotation = fBase.maxLengthAnnotation;
        patternAnnotations = fBase.patternAnnotations;
        enumerationAnnotations = fBase.enumerationAnnotations;
        whiteSpaceAnnotation = fBase.whiteSpaceAnnotation;
        maxExclusiveAnnotation = fBase.maxExclusiveAnnotation;
        maxInclusiveAnnotation = fBase.maxInclusiveAnnotation;
        minExclusiveAnnotation = fBase.minExclusiveAnnotation;
        minInclusiveAnnotation = fBase.minInclusiveAnnotation;
        totalDigitsAnnotation = fBase.totalDigitsAnnotation;
        fractionDigitsAnnotation = fBase.fractionDigitsAnnotation;

        calcFundamentalFacets();
        fIsImmutable = isImmutable;

        fBuiltInKind = base.fBuiltInKind;
    }

    protected XSSimpleTypeDecl(String name, String uri, short finalSet, XSSimpleTypeDecl itemType, boolean isImmutable,
            XSObjectList annotations) {
        fBase = fAnySimpleType;
        fTypeName = name;
        fTargetNamespace = uri;
        fFinalSet = finalSet;
        fAnnotations = annotations;

        fVariety = VARIETY_LIST;
        fItemType = itemType;
        fValidationDV = DV_LIST;
        fFacetsDefined = FACET_WHITESPACE;
        fFixedFacet = FACET_WHITESPACE;
        fWhiteSpace = WS_COLLAPSE;

        calcFundamentalFacets();
        fIsImmutable = isImmutable;

        fBuiltInKind = XSConstants.LIST_DT;
    }

    protected XSSimpleTypeDecl(String name, String uri, short finalSet, XSSimpleTypeDecl[] memberTypes,
            XSObjectList annotations) {
        fBase = fAnySimpleType;
        fTypeName = name;
        fTargetNamespace = uri;
        fFinalSet = finalSet;
        fAnnotations = annotations;

        fVariety = VARIETY_UNION;
        fMemberTypes = memberTypes;
        fValidationDV = DV_UNION;
        fFacetsDefined = FACET_WHITESPACE;
        fWhiteSpace = WS_COLLAPSE;

        calcFundamentalFacets();
        fIsImmutable = false;

        fBuiltInKind = XSConstants.UNAVAILABLE_DT;
    }

    protected XSSimpleTypeDecl setRestrictionValues(XSSimpleTypeDecl base, String name, String uri, short finalSet,
            XSObjectList annotations) {
        if(fIsImmutable) return null;
        fBase = base;
        fAnonymous = false;
        fTypeName = name;
        fTargetNamespace = uri;
        fFinalSet = finalSet;
        fAnnotations = annotations;

        fVariety = fBase.fVariety;
        fValidationDV = fBase.fValidationDV;
        switch (fVariety) {
            case VARIETY_ATOMIC:
                break;
            case VARIETY_LIST:
                fItemType = fBase.fItemType;
                break;
            case VARIETY_UNION:
                fMemberTypes = fBase.fMemberTypes;
                break;
        }

        fLength = fBase.fLength;
        fMinLength = fBase.fMinLength;
        fMaxLength = fBase.fMaxLength;
        fPattern = fBase.fPattern;
        fPatternStr = fBase.fPatternStr;
        fEnumeration = fBase.fEnumeration;
        fEnumerationSize = fBase.fEnumerationSize;
        fWhiteSpace = fBase.fWhiteSpace;
        fMaxExclusive = fBase.fMaxExclusive;
        fMaxInclusive = fBase.fMaxInclusive;
        fMinExclusive = fBase.fMinExclusive;
        fMinInclusive = fBase.fMinInclusive;
        fTotalDigits = fBase.fTotalDigits;
        fFractionDigits = fBase.fFractionDigits;
        fPatternType = fBase.fPatternType;
        fFixedFacet = fBase.fFixedFacet;
        fFacetsDefined = fBase.fFacetsDefined;

        calcFundamentalFacets();

        fBuiltInKind = base.fBuiltInKind;

        return this;
    }

    protected XSSimpleTypeDecl setListValues(String name, String uri, short finalSet, XSSimpleTypeDecl itemType,
            XSObjectList annotations) {
        if(fIsImmutable) return null;
        fBase = fAnySimpleType;
        fAnonymous = false;
        fTypeName = name;
        fTargetNamespace = uri;
        fFinalSet = finalSet;
        fAnnotations = annotations;

        fVariety = VARIETY_LIST;
        fItemType = itemType;
        fValidationDV = DV_LIST;
        fFacetsDefined = FACET_WHITESPACE;
        fFixedFacet = FACET_WHITESPACE;
        fWhiteSpace = WS_COLLAPSE;

        calcFundamentalFacets();

        fBuiltInKind = XSConstants.LIST_DT;

        return this;
    }

    protected XSSimpleTypeDecl setUnionValues(String name, String uri, short finalSet, XSSimpleTypeDecl[] memberTypes,
            XSObjectList annotations) {
        if(fIsImmutable) return null;
        fBase = fAnySimpleType;
        fAnonymous = false;
        fTypeName = name;
        fTargetNamespace = uri;
        fFinalSet = finalSet;
        fAnnotations = annotations;

        fVariety = VARIETY_UNION;
        fMemberTypes = memberTypes;
        fValidationDV = DV_UNION;
        fFacetsDefined = FACET_WHITESPACE;
        fWhiteSpace = WS_COLLAPSE;

        calcFundamentalFacets();

        fBuiltInKind = XSConstants.UNAVAILABLE_DT;

        return this;
    }

    public short getType () {
        return XSConstants.TYPE_DEFINITION;
    }

    public short getTypeCategory () {
        return SIMPLE_TYPE;
    }

    public String getName() {
        return getAnonymous()?null:fTypeName;
    }

    public String getTypeName() {
        return fTypeName;
    }

    public String getNamespace() {
        return fTargetNamespace;
    }

    public short getFinal(){
        return fFinalSet;
    }

    public boolean isFinal(short derivation) {
        return (fFinalSet & derivation) != 0;
    }

    public XSTypeDefinition getBaseType(){
        return fBase;
    }

    public boolean getAnonymous() {
        return fAnonymous || (fTypeName == null);
    }

    public short getVariety(){
        return fValidationDV == DV_ANYSIMPLETYPE ? VARIETY_ABSENT : fVariety;
    }

    public boolean isIDType(){
        switch (fVariety) {
            case VARIETY_ATOMIC:
                return fValidationDV == DV_ID;
            case VARIETY_LIST:
                return fItemType.isIDType();
            case VARIETY_UNION:
                for (int i = 0; i < fMemberTypes.length; i++) {
                    if (fMemberTypes[i].isIDType())
                        return true;
                }
        }
        return false;
    }

    public short getWhitespace() throws DatatypeException{
        if (fVariety == VARIETY_UNION) {
            throw new DatatypeException("dt-whitespace", new Object[]{fTypeName});
        }
        return fWhiteSpace;
    }

    public short getPrimitiveKind() {
        if (fVariety == VARIETY_ATOMIC && fValidationDV != DV_ANYSIMPLETYPE) {
            if (fValidationDV == DV_ID || fValidationDV == DV_IDREF || fValidationDV == DV_ENTITY) {
                return DV_STRING;
            }
            else if (fValidationDV == DV_INTEGER) {
                return DV_DECIMAL;
            }
            else if (Constants.SCHEMA_1_1_SUPPORT && (fValidationDV == DV_YEARMONTHDURATION || fValidationDV == DV_DAYTIMEDURATION)) {
                return DV_DURATION;
            }
            else {
                return fValidationDV;
            }
        }
        else {
            return (short)0;
        }
    }

    /**
     * Returns the closest built-in type category this type represents or
     * derived from. For example, if this simple type is a built-in derived
     * type integer the <code>INTEGER_DV</code> is returned.
     */
    public short getBuiltInKind() {
        return this.fBuiltInKind;
    }

    /**
     * If variety is <code>atomic</code> the primitive type definition (a
     * built-in primitive datatype definition or the simple ur-type
     * definition) is available, otherwise <code>null</code>.
     */
    public XSSimpleTypeDefinition getPrimitiveType() {
        if (fVariety == VARIETY_ATOMIC && fValidationDV != DV_ANYSIMPLETYPE) {
            XSSimpleTypeDecl pri = this;
            while (pri.fBase != fAnySimpleType)
                pri = pri.fBase;
            return pri;
        }
        else {
            return null;
        }
    }

    /**
     * If variety is <code>list</code> the item type definition (an atomic or
     * union simple type definition) is available, otherwise
     * <code>null</code>.
     */
    public XSSimpleTypeDefinition getItemType() {
        if (fVariety == VARIETY_LIST) {
            return fItemType;
        }
        else {
            return null;
        }
    }

    /**
     * If variety is <code>union</code> the list of member type definitions (a
     * non-empty sequence of simple type definitions) is available,
     * otherwise an empty <code>XSObjectList</code>.
     */
    public XSObjectList getMemberTypes() {
        if (fVariety == VARIETY_UNION) {
            return new XSObjectListImpl(fMemberTypes, fMemberTypes.length);
        }
        else {
            return XSObjectListImpl.EMPTY_LIST;
        }
    }

    /**
     * If <restriction> is chosen
     */
    public void applyFacets(XSFacets facets, short presentFacet, short fixedFacet, ValidationContext context)
    throws InvalidDatatypeFacetException {
        if (context == null) {
            context = fEmptyContext;
        }
        applyFacets(facets, presentFacet, fixedFacet, SPECIAL_PATTERN_NONE, context);
    }

    /**
     * built-in derived types by restriction
     */
    void applyFacets1(XSFacets facets, short presentFacet, short fixedFacet) {

        try {
            applyFacets(facets, presentFacet, fixedFacet, SPECIAL_PATTERN_NONE, fDummyContext);
        } catch (InvalidDatatypeFacetException e) {
            throw new RuntimeException("internal error");
        }
        fIsImmutable = true;
    }

    /**
     * built-in derived types by restriction
     */
    void applyFacets1(XSFacets facets, short presentFacet, short fixedFacet, short patternType) {

        try {
            applyFacets(facets, presentFacet, fixedFacet, patternType, fDummyContext);
        } catch (InvalidDatatypeFacetException e) {
            throw new RuntimeException("internal error");
        }
        fIsImmutable = true;
    }

    /**
     * If <restriction> is chosen, or built-in derived types by restriction
     */
    void applyFacets(XSFacets facets, short presentFacet, short fixedFacet, short patternType, ValidationContext context)
    throws InvalidDatatypeFacetException {

        if(fIsImmutable) return;
        ValidatedInfo tempInfo = new ValidatedInfo();


        fFacetsDefined = 0;
        fFixedFacet = 0;

        int result = 0 ;

        short allowedFacet = fDVs[fValidationDV].getAllowedFacets();

        if ((presentFacet & FACET_LENGTH) != 0) {
            if ((allowedFacet & FACET_LENGTH) == 0) {
                reportError("cos-applicable-facets", new Object[]{"length", fTypeName});
            } else {
                fLength = facets.length;
                lengthAnnotation = facets.lengthAnnotation;
                fFacetsDefined |= FACET_LENGTH;
                if ((fixedFacet & FACET_LENGTH) != 0)
                    fFixedFacet |= FACET_LENGTH;
            }
        }
        if ((presentFacet & FACET_MINLENGTH) != 0) {
            if ((allowedFacet & FACET_MINLENGTH) == 0) {
                reportError("cos-applicable-facets", new Object[]{"minLength", fTypeName});
            } else {
                fMinLength = facets.minLength;
                minLengthAnnotation = facets.minLengthAnnotation;
                fFacetsDefined |= FACET_MINLENGTH;
                if ((fixedFacet & FACET_MINLENGTH) != 0)
                    fFixedFacet |= FACET_MINLENGTH;
            }
        }
        if ((presentFacet & FACET_MAXLENGTH) != 0) {
            if ((allowedFacet & FACET_MAXLENGTH) == 0) {
                reportError("cos-applicable-facets", new Object[]{"maxLength", fTypeName});
            } else {
                fMaxLength = facets.maxLength;
                maxLengthAnnotation = facets.maxLengthAnnotation;
                fFacetsDefined |= FACET_MAXLENGTH;
                if ((fixedFacet & FACET_MAXLENGTH) != 0)
                    fFixedFacet |= FACET_MAXLENGTH;
            }
        }
        if ((presentFacet & FACET_PATTERN) != 0) {
            if ((allowedFacet & FACET_PATTERN) == 0) {
                reportError("cos-applicable-facets", new Object[]{"pattern", fTypeName});
            } else {
                patternAnnotations = facets.patternAnnotations;
                RegularExpression regex = null;
                try {
                    regex = new RegularExpression(facets.pattern, "X", context.getLocale());
                } catch (Exception e) {
                    reportError("InvalidRegex", new Object[]{facets.pattern, e.getLocalizedMessage()});
                }
                if (regex != null) {
                    fPattern = new ArrayList<>();
                    fPattern.add(regex);
                    fPatternStr = new ArrayList<>();
                    fPatternStr.add(facets.pattern);
                    fFacetsDefined |= FACET_PATTERN;
                    if ((fixedFacet & FACET_PATTERN) != 0)
                        fFixedFacet |= FACET_PATTERN;
                }
            }
        }

        if ((presentFacet & FACET_WHITESPACE) != 0) {
            if ((allowedFacet & FACET_WHITESPACE) == 0) {
                reportError("cos-applicable-facets", new Object[]{"whiteSpace", fTypeName});
            } else {
                fWhiteSpace = facets.whiteSpace;
                whiteSpaceAnnotation = facets.whiteSpaceAnnotation;
                fFacetsDefined |= FACET_WHITESPACE;
                if ((fixedFacet & FACET_WHITESPACE) != 0)
                    fFixedFacet |= FACET_WHITESPACE;
            }
        }
        if ((presentFacet & FACET_ENUMERATION) != 0) {
            if ((allowedFacet & FACET_ENUMERATION) == 0) {
                reportError("cos-applicable-facets", new Object[]{"enumeration", fTypeName});
            } else {
                List<String> enumVals = facets.enumeration;
                int size = enumVals.size();
                fEnumeration = new ValidatedInfo[size];
                List<NamespaceContext> enumNSDecls = facets.enumNSDecls;
                ValidationContextImpl ctx = new ValidationContextImpl(context);
                enumerationAnnotations = facets.enumAnnotations;
                fEnumerationSize = 0;
                for (int i = 0; i < size; i++) {
                    if (enumNSDecls != null)
                        ctx.setNSContext(enumNSDecls.get(i));
                    try {
                        ValidatedInfo info = getActualEnumValue(enumVals.get(i), ctx, null);
                        fEnumeration[fEnumerationSize++] = info;
                    } catch (InvalidDatatypeValueException ide) {
                        reportError("enumeration-valid-restriction", new Object[]{enumVals.get(i), this.getBaseType().getName()});
                    }
                }
                fFacetsDefined |= FACET_ENUMERATION;
                if ((fixedFacet & FACET_ENUMERATION) != 0)
                    fFixedFacet |= FACET_ENUMERATION;
            }
        }

        if ((presentFacet & FACET_MAXINCLUSIVE) != 0) {
            if ((allowedFacet & FACET_MAXINCLUSIVE) == 0) {
                reportError("cos-applicable-facets", new Object[]{"maxInclusive", fTypeName});
            } else {
                maxInclusiveAnnotation = facets.maxInclusiveAnnotation;
                try {
                    fMaxInclusive = fBase.getActualValue(facets.maxInclusive, context, tempInfo, true);
                    fFacetsDefined |= FACET_MAXINCLUSIVE;
                    if ((fixedFacet & FACET_MAXINCLUSIVE) != 0)
                        fFixedFacet |= FACET_MAXINCLUSIVE;
                } catch (InvalidDatatypeValueException ide) {
                    reportError(ide.getKey(), ide.getArgs());
                    reportError("FacetValueFromBase", new Object[]{fTypeName, facets.maxInclusive,
                            "maxInclusive", fBase.getName()});
                }

                if (((fBase.fFacetsDefined & FACET_MAXINCLUSIVE) != 0)) {
                    if ((fBase.fFixedFacet & FACET_MAXINCLUSIVE) != 0) {
                        if (fDVs[fValidationDV].compare(fMaxInclusive, fBase.fMaxInclusive) != 0)
                            reportError( "FixedFacetValue", new Object[]{"maxInclusive", fMaxInclusive, fBase.fMaxInclusive, fTypeName});
                    }
                }
                try {
                    fBase.validate(context, tempInfo);
                } catch (InvalidDatatypeValueException ide) {
                    reportError(ide.getKey(), ide.getArgs());
                    reportError("FacetValueFromBase", new Object[]{fTypeName, facets.maxInclusive,
                            "maxInclusive", fBase.getName()});
                }
            }
        }

        boolean needCheckBase = true;
        if ((presentFacet & FACET_MAXEXCLUSIVE) != 0) {
            if ((allowedFacet & FACET_MAXEXCLUSIVE) == 0) {
                reportError("cos-applicable-facets", new Object[]{"maxExclusive", fTypeName});
            } else {
                maxExclusiveAnnotation = facets.maxExclusiveAnnotation;
                try {
                    fMaxExclusive = fBase.getActualValue(facets.maxExclusive, context, tempInfo, true);
                    fFacetsDefined |= FACET_MAXEXCLUSIVE;
                    if ((fixedFacet & FACET_MAXEXCLUSIVE) != 0)
                        fFixedFacet |= FACET_MAXEXCLUSIVE;
                } catch (InvalidDatatypeValueException ide) {
                    reportError(ide.getKey(), ide.getArgs());
                    reportError("FacetValueFromBase", new Object[]{fTypeName, facets.maxExclusive,
                            "maxExclusive", fBase.getName()});
                }

                if (((fBase.fFacetsDefined & FACET_MAXEXCLUSIVE) != 0)) {
                    result = fDVs[fValidationDV].compare(fMaxExclusive, fBase.fMaxExclusive);
                    if ((fBase.fFixedFacet & FACET_MAXEXCLUSIVE) != 0 && result != 0) {
                        reportError( "FixedFacetValue", new Object[]{"maxExclusive", facets.maxExclusive, fBase.fMaxExclusive, fTypeName});
                    }
                    if (result == 0) {
                        needCheckBase = false;
                    }
                }
                if (needCheckBase) {
                    try {
                        fBase.validate(context, tempInfo);
                    } catch (InvalidDatatypeValueException ide) {
                        reportError(ide.getKey(), ide.getArgs());
                        reportError("FacetValueFromBase", new Object[]{fTypeName, facets.maxExclusive,
                                "maxExclusive", fBase.getName()});
                    }
                }
                else if (((fBase.fFacetsDefined & FACET_MAXINCLUSIVE) != 0)) {
                    if (fDVs[fValidationDV].compare(fMaxExclusive, fBase.fMaxInclusive) > 0) {
                        reportError( "maxExclusive-valid-restriction.2", new Object[]{facets.maxExclusive, fBase.fMaxInclusive});
                    }
                }
            }
        }
        needCheckBase = true;
        if ((presentFacet & FACET_MINEXCLUSIVE) != 0) {
            if ((allowedFacet & FACET_MINEXCLUSIVE) == 0) {
                reportError("cos-applicable-facets", new Object[]{"minExclusive", fTypeName});
            } else {
                minExclusiveAnnotation = facets.minExclusiveAnnotation;
                try {
                    fMinExclusive = fBase.getActualValue(facets.minExclusive, context, tempInfo, true);
                    fFacetsDefined |= FACET_MINEXCLUSIVE;
                    if ((fixedFacet & FACET_MINEXCLUSIVE) != 0)
                        fFixedFacet |= FACET_MINEXCLUSIVE;
                } catch (InvalidDatatypeValueException ide) {
                    reportError(ide.getKey(), ide.getArgs());
                    reportError("FacetValueFromBase", new Object[]{fTypeName, facets.minExclusive,
                            "minExclusive", fBase.getName()});
                }

                if (((fBase.fFacetsDefined & FACET_MINEXCLUSIVE) != 0)) {
                    result = fDVs[fValidationDV].compare(fMinExclusive, fBase.fMinExclusive);
                    if ((fBase.fFixedFacet & FACET_MINEXCLUSIVE) != 0 && result != 0) {
                        reportError( "FixedFacetValue", new Object[]{"minExclusive", facets.minExclusive, fBase.fMinExclusive, fTypeName});
                    }
                    if (result == 0) {
                        needCheckBase = false;
                    }
                }
                if (needCheckBase) {
                    try {
                        fBase.validate(context, tempInfo);
                    } catch (InvalidDatatypeValueException ide) {
                        reportError(ide.getKey(), ide.getArgs());
                        reportError("FacetValueFromBase", new Object[]{fTypeName, facets.minExclusive,
                                "minExclusive", fBase.getName()});
                    }
                }
                else if (((fBase.fFacetsDefined & FACET_MININCLUSIVE) != 0)) {
                    if (fDVs[fValidationDV].compare(fMinExclusive, fBase.fMinInclusive) < 0) {
                        reportError( "minExclusive-valid-restriction.3", new Object[]{facets.minExclusive, fBase.fMinInclusive});
                    }
                }
            }
        }
        if ((presentFacet & FACET_MININCLUSIVE) != 0) {
            if ((allowedFacet & FACET_MININCLUSIVE) == 0) {
                reportError("cos-applicable-facets", new Object[]{"minInclusive", fTypeName});
            } else {
                minInclusiveAnnotation = facets.minInclusiveAnnotation;
                try {
                    fMinInclusive = fBase.getActualValue(facets.minInclusive, context, tempInfo, true);
                    fFacetsDefined |= FACET_MININCLUSIVE;
                    if ((fixedFacet & FACET_MININCLUSIVE) != 0)
                        fFixedFacet |= FACET_MININCLUSIVE;
                } catch (InvalidDatatypeValueException ide) {
                    reportError(ide.getKey(), ide.getArgs());
                    reportError("FacetValueFromBase", new Object[]{fTypeName, facets.minInclusive,
                            "minInclusive", fBase.getName()});
                }

                if (((fBase.fFacetsDefined & FACET_MININCLUSIVE) != 0)) {
                    if ((fBase.fFixedFacet & FACET_MININCLUSIVE) != 0) {
                        if (fDVs[fValidationDV].compare(fMinInclusive, fBase.fMinInclusive) != 0)
                            reportError( "FixedFacetValue", new Object[]{"minInclusive", facets.minInclusive, fBase.fMinInclusive, fTypeName});
                    }
                }
                try {
                    fBase.validate(context, tempInfo);
                } catch (InvalidDatatypeValueException ide) {
                    reportError(ide.getKey(), ide.getArgs());
                    reportError("FacetValueFromBase", new Object[]{fTypeName, facets.minInclusive,
                            "minInclusive", fBase.getName()});
                }
            }
        }

        if ((presentFacet & FACET_TOTALDIGITS) != 0) {
            if ((allowedFacet & FACET_TOTALDIGITS) == 0) {
                reportError("cos-applicable-facets", new Object[]{"totalDigits", fTypeName});
            } else {
                totalDigitsAnnotation = facets.totalDigitsAnnotation;
                fTotalDigits = facets.totalDigits;
                fFacetsDefined |= FACET_TOTALDIGITS;
                if ((fixedFacet & FACET_TOTALDIGITS) != 0)
                    fFixedFacet |= FACET_TOTALDIGITS;
            }
        }
        if ((presentFacet & FACET_FRACTIONDIGITS) != 0) {
            if ((allowedFacet & FACET_FRACTIONDIGITS) == 0) {
                reportError("cos-applicable-facets", new Object[]{"fractionDigits", fTypeName});
            } else {
                fFractionDigits = facets.fractionDigits;
                fractionDigitsAnnotation = facets.fractionDigitsAnnotation;
                fFacetsDefined |= FACET_FRACTIONDIGITS;
                if ((fixedFacet & FACET_FRACTIONDIGITS) != 0)
                    fFixedFacet |= FACET_FRACTIONDIGITS;
            }
        }

        if (patternType != SPECIAL_PATTERN_NONE) {
            fPatternType = patternType;
        }

        if(fFacetsDefined != 0) {

            if(((fFacetsDefined & FACET_MINLENGTH ) != 0 ) && ((fFacetsDefined & FACET_MAXLENGTH) != 0))
            {
                if(fMinLength > fMaxLength)
                    reportError("minLength-less-than-equal-to-maxLength", new Object[]{Integer.toString(fMinLength), Integer.toString(fMaxLength), fTypeName});
            }

            if (((fFacetsDefined & FACET_MAXEXCLUSIVE) != 0) && ((fFacetsDefined & FACET_MAXINCLUSIVE) != 0)) {
                reportError( "maxInclusive-maxExclusive", new Object[]{fMaxInclusive, fMaxExclusive, fTypeName});
            }

            if (((fFacetsDefined & FACET_MINEXCLUSIVE) != 0) && ((fFacetsDefined & FACET_MININCLUSIVE) != 0)) {
                reportError("minInclusive-minExclusive", new Object[]{fMinInclusive, fMinExclusive, fTypeName});
            }

            if (((fFacetsDefined &  FACET_MAXINCLUSIVE) != 0) && ((fFacetsDefined & FACET_MININCLUSIVE) != 0)) {
                result = fDVs[fValidationDV].compare(fMinInclusive, fMaxInclusive);
                if (result != -1 && result != 0)
                    reportError("minInclusive-less-than-equal-to-maxInclusive", new Object[]{fMinInclusive, fMaxInclusive, fTypeName});
            }

            if (((fFacetsDefined & FACET_MAXEXCLUSIVE) != 0) && ((fFacetsDefined & FACET_MINEXCLUSIVE) != 0)) {
                result = fDVs[fValidationDV].compare(fMinExclusive, fMaxExclusive);
                if (result != -1 && result != 0)
                    reportError( "minExclusive-less-than-equal-to-maxExclusive", new Object[]{fMinExclusive, fMaxExclusive, fTypeName});
            }

            if (((fFacetsDefined & FACET_MAXINCLUSIVE) != 0) && ((fFacetsDefined & FACET_MINEXCLUSIVE) != 0)) {
                if (fDVs[fValidationDV].compare(fMinExclusive, fMaxInclusive) != -1)
                    reportError( "minExclusive-less-than-maxInclusive", new Object[]{fMinExclusive, fMaxInclusive, fTypeName});
            }

            if (((fFacetsDefined & FACET_MAXEXCLUSIVE) != 0) && ((fFacetsDefined & FACET_MININCLUSIVE) != 0)) {
                if (fDVs[fValidationDV].compare(fMinInclusive, fMaxExclusive) != -1)
                    reportError( "minInclusive-less-than-maxExclusive", new Object[]{fMinInclusive, fMaxExclusive, fTypeName});
            }

            if (((fFacetsDefined & FACET_FRACTIONDIGITS) != 0) &&
                    ((fFacetsDefined & FACET_TOTALDIGITS) != 0)) {
                if (fFractionDigits > fTotalDigits)
                    reportError( "fractionDigits-totalDigits", new Object[]{Integer.toString(fFractionDigits), Integer.toString(fTotalDigits), fTypeName});
            }

            if((fFacetsDefined & FACET_LENGTH) != 0 ){
                if ((fBase.fFacetsDefined & FACET_MINLENGTH) != 0 &&
                        fLength < fBase.fMinLength) {
                    reportError("length-minLength-maxLength.1.1", new Object[]{fTypeName, Integer.toString(fLength), Integer.toString(fBase.fMinLength)});
                }
                if ((fBase.fFacetsDefined & FACET_MAXLENGTH) != 0 &&
                        fLength > fBase.fMaxLength) {
                    reportError("length-minLength-maxLength.2.1", new Object[]{fTypeName, Integer.toString(fLength), Integer.toString(fBase.fMaxLength)});
                }
                if ( (fBase.fFacetsDefined & FACET_LENGTH) != 0 ) {
                    if ( fLength != fBase.fLength )
                        reportError( "length-valid-restriction", new Object[]{Integer.toString(fLength), Integer.toString(fBase.fLength), fTypeName});
                }
            }

            if((fBase.fFacetsDefined & FACET_LENGTH) != 0 || (fFacetsDefined & FACET_LENGTH) != 0){
                if ((fFacetsDefined & FACET_MINLENGTH) != 0){
                    if ((fBase.fFacetsDefined & FACET_LENGTH) != 0 && fBase.fLength < fMinLength) {
                        reportError("length-minLength-maxLength.1.1", new Object[]{fTypeName, Integer.toString(fBase.fLength), Integer.toString(fMinLength)});
                    }
                    if ((fFacetsDefined & FACET_LENGTH) != 0 && fLength < fMinLength) {
                        reportError("length-minLength-maxLength.1.1", new Object[]{fTypeName, Integer.toString(fLength), Integer.toString(fMinLength)});
                    }
                    if ((fBase.fFacetsDefined & FACET_MINLENGTH) == 0){
                        reportError("length-minLength-maxLength.1.2.a", new Object[]{fTypeName});
                    }
                    if (fMinLength != fBase.fMinLength){
                        reportError("length-minLength-maxLength.1.2.b", new Object[]{fTypeName, Integer.toString(fMinLength), Integer.toString(fBase.fMinLength)});
                    }
                }
                if ((fFacetsDefined & FACET_MAXLENGTH) != 0){
                    if ((fBase.fFacetsDefined & FACET_LENGTH) != 0 && fBase.fLength > fMaxLength) {
                        reportError("length-minLength-maxLength.2.1", new Object[]{fTypeName, Integer.toString(fBase.fLength), Integer.toString(fMaxLength)});
                    }
                    if ((fFacetsDefined & FACET_LENGTH) != 0 && fLength > fMaxLength) {
                        reportError("length-minLength-maxLength.2.1", new Object[]{fTypeName, Integer.toString(fLength), Integer.toString(fMaxLength)});
                    }
                    if ((fBase.fFacetsDefined & FACET_MAXLENGTH) == 0){
                        reportError("length-minLength-maxLength.2.2.a", new Object[]{fTypeName});
                    }
                    if (fMaxLength != fBase.fMaxLength){
                        reportError("length-minLength-maxLength.2.2.b", new Object[]{fTypeName, Integer.toString(fMaxLength), Integer.toString(fBase.fBase.fMaxLength)});
                    }
                }
            }

            if ( ((fFacetsDefined & FACET_MINLENGTH ) != 0 ) ) {
                if ( (fBase.fFacetsDefined & FACET_MAXLENGTH ) != 0 ) {
                    if ( fMinLength > fBase.fMaxLength ) {
                        reportError("minLength-less-than-equal-to-maxLength", new Object[]{Integer.toString(fMinLength), Integer.toString(fBase.fMaxLength), fTypeName});
                    }
                }
                else if ( (fBase.fFacetsDefined & FACET_MINLENGTH) != 0 ) {
                    if ( (fBase.fFixedFacet & FACET_MINLENGTH) != 0 && fMinLength != fBase.fMinLength ) {
                        reportError( "FixedFacetValue", new Object[]{"minLength", Integer.toString(fMinLength), Integer.toString(fBase.fMinLength), fTypeName});
                    }

                    if ( fMinLength < fBase.fMinLength ) {
                        reportError( "minLength-valid-restriction", new Object[]{Integer.toString(fMinLength), Integer.toString(fBase.fMinLength), fTypeName});
                    }
                }
            }


            if ( ((fFacetsDefined & FACET_MAXLENGTH ) != 0 ) && ((fBase.fFacetsDefined & FACET_MINLENGTH ) != 0 )) {
                if ( fMaxLength < fBase.fMinLength) {
                    reportError("minLength-less-than-equal-to-maxLength", new Object[]{Integer.toString(fBase.fMinLength), Integer.toString(fMaxLength)});
                }
            }

            if ( (fFacetsDefined & FACET_MAXLENGTH) != 0 ) {
                if ( (fBase.fFacetsDefined & FACET_MAXLENGTH) != 0 ){
                    if(( (fBase.fFixedFacet & FACET_MAXLENGTH) != 0 )&& fMaxLength != fBase.fMaxLength ) {
                        reportError( "FixedFacetValue", new Object[]{"maxLength", Integer.toString(fMaxLength), Integer.toString(fBase.fMaxLength), fTypeName});
                    }
                    if ( fMaxLength > fBase.fMaxLength ) {
                        reportError( "maxLength-valid-restriction", new Object[]{Integer.toString(fMaxLength), Integer.toString(fBase.fMaxLength), fTypeName});
                    }
                }
            }

            /*          

                             if (((fFacetsDefined & FACET_MAXINCLUSIVE) != 0)) {
                             if (((fBase.fFacetsDefined & FACET_MAXINCLUSIVE) != 0)) {
                             result = fDVs[fValidationDV].compare(fMaxInclusive, fBase.fMaxInclusive);
                             if ((fBase.fFixedFacet & FACET_MAXINCLUSIVE) != 0 && result != 0) {
                             reportError( "FixedFacetValue", new Object[]{"maxInclusive", fMaxInclusive, fBase.fMaxInclusive, fTypeName});
                             }
                             if (result != -1 && result != 0) {
                             reportError( "maxInclusive-valid-restriction.1", new Object[]{fMaxInclusive, fBase.fMaxInclusive, fTypeName});
                             }
                             }
                             if (((fBase.fFacetsDefined & FACET_MAXEXCLUSIVE) != 0) &&
                             fDVs[fValidationDV].compare(fMaxInclusive, fBase.fMaxExclusive) != -1){
                             reportError( "maxInclusive-valid-restriction.1", new Object[]{fMaxInclusive, fBase.fMaxExclusive, fTypeName});
                             }

                             if ((( fBase.fFacetsDefined & FACET_MININCLUSIVE) != 0)) {
                             result = fDVs[fValidationDV].compare(fMaxInclusive, fBase.fMinInclusive);
                             if (result != 1 && result != 0) {
                             reportError( "maxInclusive-valid-restriction.1", new Object[]{fMaxInclusive, fBase.fMinInclusive, fTypeName});
                             }
                             }

                             if ((( fBase.fFacetsDefined & FACET_MINEXCLUSIVE) != 0) &&
                             fDVs[fValidationDV].compare(fMaxInclusive, fBase.fMinExclusive ) != 1)
                             reportError( "maxInclusive-valid-restriction.1", new Object[]{fMaxInclusive, fBase.fMinExclusive, fTypeName});
                             }

                                  if (((fFacetsDefined & FACET_MAXEXCLUSIVE) != 0)) {
                                  if ((( fBase.fFacetsDefined & FACET_MAXEXCLUSIVE) != 0)) {
                                  result= fDVs[fValidationDV].compare(fMaxExclusive, fBase.fMaxExclusive);
                                  if ((fBase.fFixedFacet & FACET_MAXEXCLUSIVE) != 0 &&  result != 0) {
                                  reportError( "FixedFacetValue", new Object[]{"maxExclusive", fMaxExclusive, fBase.fMaxExclusive, fTypeName});
                                  }
                                  if (result != -1 && result != 0) {
                                  reportError( "maxExclusive-valid-restriction.1", new Object[]{fMaxExclusive, fBase.fMaxExclusive, fTypeName});
                                  }
                                  }

                                  if ((( fBase.fFacetsDefined & FACET_MAXINCLUSIVE) != 0)) {
                                  result= fDVs[fValidationDV].compare(fMaxExclusive, fBase.fMaxInclusive);
                                  if (result != -1 && result != 0) {
                                  reportError( "maxExclusive-valid-restriction.2", new Object[]{fMaxExclusive, fBase.fMaxInclusive, fTypeName});
                                  }
                                  }

                                  if ((( fBase.fFacetsDefined & FACET_MINEXCLUSIVE) != 0) &&
                                  fDVs[fValidationDV].compare(fMaxExclusive, fBase.fMinExclusive ) != 1)
                                  reportError( "maxExclusive-valid-restriction.3", new Object[]{fMaxExclusive, fBase.fMinExclusive, fTypeName});

                                  if ((( fBase.fFacetsDefined & FACET_MININCLUSIVE) != 0) &&
                                  fDVs[fValidationDV].compare(fMaxExclusive, fBase.fMinInclusive) != 1)
                                  reportError( "maxExclusive-valid-restriction.4", new Object[]{fMaxExclusive, fBase.fMinInclusive, fTypeName});
                                  }

                                       if (((fFacetsDefined & FACET_MINEXCLUSIVE) != 0)) {
                                       if ((( fBase.fFacetsDefined & FACET_MINEXCLUSIVE) != 0)) {
                                       result= fDVs[fValidationDV].compare(fMinExclusive, fBase.fMinExclusive);
                                       if ((fBase.fFixedFacet & FACET_MINEXCLUSIVE) != 0 && result != 0) {
                                       reportError( "FixedFacetValue", new Object[]{"minExclusive", fMinExclusive, fBase.fMinExclusive, fTypeName});
                                       }
                                       if (result != 1 && result != 0) {
                                       reportError( "minExclusive-valid-restriction.1", new Object[]{fMinExclusive, fBase.fMinExclusive, fTypeName});
                                       }
                                       }

                                       if ((( fBase.fFacetsDefined & FACET_MAXINCLUSIVE) != 0)) {
                                       result=fDVs[fValidationDV].compare(fMinExclusive, fBase.fMaxInclusive);

                                       if (result != -1 && result != 0) {
                                       reportError( "minExclusive-valid-restriction.2", new Object[]{fMinExclusive, fBase.fMaxInclusive, fTypeName});
                                       }
                                       }

                                       if ((( fBase.fFacetsDefined & FACET_MININCLUSIVE) != 0)) {
                                       result = fDVs[fValidationDV].compare(fMinExclusive, fBase.fMinInclusive);

                                       if (result != 1 && result != 0) {
                                       reportError( "minExclusive-valid-restriction.3", new Object[]{fMinExclusive, fBase.fMinInclusive, fTypeName});
                                       }
                                       }

                                       if ((( fBase.fFacetsDefined & FACET_MAXEXCLUSIVE) != 0) &&
                                       fDVs[fValidationDV].compare(fMinExclusive, fBase.fMaxExclusive) != -1)
                                       reportError( "minExclusive-valid-restriction.4", new Object[]{fMinExclusive, fBase.fMaxExclusive, fTypeName});
                                       }

                                            if (((fFacetsDefined & FACET_MININCLUSIVE) != 0)) {
                                            if (((fBase.fFacetsDefined & FACET_MININCLUSIVE) != 0)) {
                                            result = fDVs[fValidationDV].compare(fMinInclusive, fBase.fMinInclusive);

                                            if ((fBase.fFixedFacet & FACET_MININCLUSIVE) != 0 && result != 0) {
                                            reportError( "FixedFacetValue", new Object[]{"minInclusive", fMinInclusive, fBase.fMinInclusive, fTypeName});
                                            }
                                            if (result != 1 && result != 0) {
                                            reportError( "minInclusive-valid-restriction.1", new Object[]{fMinInclusive, fBase.fMinInclusive, fTypeName});
                                            }
                                            }
                                            if ((( fBase.fFacetsDefined & FACET_MAXINCLUSIVE) != 0)) {
                                            result=fDVs[fValidationDV].compare(fMinInclusive, fBase.fMaxInclusive);
                                            if (result != -1 && result != 0) {
                                            reportError( "minInclusive-valid-restriction.2", new Object[]{fMinInclusive, fBase.fMaxInclusive, fTypeName});
                                            }
                                            }
                                            if ((( fBase.fFacetsDefined & FACET_MINEXCLUSIVE) != 0) &&
                                            fDVs[fValidationDV].compare(fMinInclusive, fBase.fMinExclusive ) != 1)
                                            reportError( "minInclusive-valid-restriction.3", new Object[]{fMinInclusive, fBase.fMinExclusive, fTypeName});
                                            if ((( fBase.fFacetsDefined & FACET_MAXEXCLUSIVE) != 0) &&
                                            fDVs[fValidationDV].compare(fMinInclusive, fBase.fMaxExclusive) != -1)
                                            reportError( "minInclusive-valid-restriction.4", new Object[]{fMinInclusive, fBase.fMaxExclusive, fTypeName});
                                            }
             */
            if (((fFacetsDefined & FACET_TOTALDIGITS) != 0)) {
                if ((( fBase.fFacetsDefined & FACET_TOTALDIGITS) != 0)) {
                    if ((fBase.fFixedFacet & FACET_TOTALDIGITS) != 0 && fTotalDigits != fBase.fTotalDigits) {
                        reportError("FixedFacetValue", new Object[]{"totalDigits", Integer.toString(fTotalDigits), Integer.toString(fBase.fTotalDigits), fTypeName});
                    }
                    if (fTotalDigits > fBase.fTotalDigits) {
                        reportError( "totalDigits-valid-restriction", new Object[]{Integer.toString(fTotalDigits), Integer.toString(fBase.fTotalDigits), fTypeName});
                    }
                }
            }

            if ((fFacetsDefined & FACET_FRACTIONDIGITS) != 0) {
                if ((fBase.fFacetsDefined & FACET_TOTALDIGITS) != 0) {
                    if (fFractionDigits > fBase.fTotalDigits)
                        reportError( "fractionDigits-totalDigits", new Object[]{Integer.toString(fFractionDigits), Integer.toString(fTotalDigits), fTypeName});
                }
            }

            if (((fFacetsDefined & FACET_FRACTIONDIGITS) != 0)) {
                if ((( fBase.fFacetsDefined & FACET_FRACTIONDIGITS) != 0)) {
                    if (((fBase.fFixedFacet & FACET_FRACTIONDIGITS) != 0 && fFractionDigits != fBase.fFractionDigits) ||
                            (fValidationDV == DV_INTEGER && fFractionDigits != 0)) {
                        reportError("FixedFacetValue", new Object[]{"fractionDigits", Integer.toString(fFractionDigits), Integer.toString(fBase.fFractionDigits), fTypeName});
                    }
                    if (fFractionDigits > fBase.fFractionDigits) {
                        reportError( "fractionDigits-valid-restriction", new Object[]{Integer.toString(fFractionDigits), Integer.toString(fBase.fFractionDigits), fTypeName});
                    }
                }
                else if (fValidationDV == DV_INTEGER && fFractionDigits != 0) {
                    reportError("FixedFacetValue", new Object[]{"fractionDigits", Integer.toString(fFractionDigits), "0", fTypeName});
                }
            }


            if ( (fFacetsDefined & FACET_WHITESPACE) != 0 && (fBase.fFacetsDefined & FACET_WHITESPACE) != 0 ){
                if ( (fBase.fFixedFacet & FACET_WHITESPACE) != 0 &&  fWhiteSpace != fBase.fWhiteSpace ) {
                    reportError( "FixedFacetValue", new Object[]{"whiteSpace", whiteSpaceValue(fWhiteSpace), whiteSpaceValue(fBase.fWhiteSpace), fTypeName});
                }

                if ( fWhiteSpace == WS_PRESERVE &&  fBase.fWhiteSpace == WS_COLLAPSE ){
                    reportError( "whiteSpace-valid-restriction.1", new Object[]{fTypeName, "preserve"});
                }
                if ( fWhiteSpace == WS_REPLACE &&  fBase.fWhiteSpace == WS_COLLAPSE ){
                    reportError( "whiteSpace-valid-restriction.1", new Object[]{fTypeName, "replace"});
                }
                if ( fWhiteSpace == WS_PRESERVE &&  fBase.fWhiteSpace == WS_REPLACE ){
                    reportError( "whiteSpace-valid-restriction.2", new Object[]{fTypeName});
                }
            }
        }


        if ( (fFacetsDefined & FACET_LENGTH) == 0  && (fBase.fFacetsDefined & FACET_LENGTH) != 0 ) {
            fFacetsDefined |= FACET_LENGTH;
            fLength = fBase.fLength;
            lengthAnnotation = fBase.lengthAnnotation;
        }
        if ( (fFacetsDefined & FACET_MINLENGTH) == 0 && (fBase.fFacetsDefined & FACET_MINLENGTH) != 0 ) {
            fFacetsDefined |= FACET_MINLENGTH;
            fMinLength = fBase.fMinLength;
            minLengthAnnotation = fBase.minLengthAnnotation;
        }
        if ((fFacetsDefined & FACET_MAXLENGTH) == 0 &&  (fBase.fFacetsDefined & FACET_MAXLENGTH) != 0 ) {
            fFacetsDefined |= FACET_MAXLENGTH;
            fMaxLength = fBase.fMaxLength;
            maxLengthAnnotation = fBase.maxLengthAnnotation;
        }
        if ( (fBase.fFacetsDefined & FACET_PATTERN) != 0 ) {
            if ((fFacetsDefined & FACET_PATTERN) == 0) {
                fFacetsDefined |= FACET_PATTERN;
                fPattern = fBase.fPattern;
                fPatternStr = fBase.fPatternStr;
                patternAnnotations = fBase.patternAnnotations;
            }
            else {
                for (int i = fBase.fPattern.size()-1; i >= 0; --i) {
                    fPattern.add(fBase.fPattern.get(i));
                    fPatternStr.add(fBase.fPatternStr.get(i));
                }
                if (fBase.patternAnnotations != null) {
                    if (patternAnnotations != null) {
                        for (int i = fBase.patternAnnotations.getLength()-1; i >= 0; --i) {
                            patternAnnotations.addXSObject(fBase.patternAnnotations.item(i));
                        }
                    }
                    else {
                        patternAnnotations = fBase.patternAnnotations;
                    }
                }
            }
        }
        if ( (fFacetsDefined & FACET_WHITESPACE) == 0 &&  (fBase.fFacetsDefined & FACET_WHITESPACE) != 0 ) {
            fFacetsDefined |= FACET_WHITESPACE;
            fWhiteSpace = fBase.fWhiteSpace;
            whiteSpaceAnnotation = fBase.whiteSpaceAnnotation;
        }
        if ((fFacetsDefined & FACET_ENUMERATION) == 0 && (fBase.fFacetsDefined & FACET_ENUMERATION) != 0) {
            fFacetsDefined |= FACET_ENUMERATION;
            fEnumeration = fBase.fEnumeration;
            fEnumerationSize = fBase.fEnumerationSize;
            enumerationAnnotations = fBase.enumerationAnnotations;
        }
        if ((( fBase.fFacetsDefined & FACET_MAXEXCLUSIVE) != 0) &&
                !((fFacetsDefined & FACET_MAXEXCLUSIVE) != 0) && !((fFacetsDefined & FACET_MAXINCLUSIVE) != 0)) {
            fFacetsDefined |= FACET_MAXEXCLUSIVE;
            fMaxExclusive = fBase.fMaxExclusive;
            maxExclusiveAnnotation = fBase.maxExclusiveAnnotation;
        }
        if ((( fBase.fFacetsDefined & FACET_MAXINCLUSIVE) != 0) &&
                !((fFacetsDefined & FACET_MAXEXCLUSIVE) != 0) && !((fFacetsDefined & FACET_MAXINCLUSIVE) != 0)) {
            fFacetsDefined |= FACET_MAXINCLUSIVE;
            fMaxInclusive = fBase.fMaxInclusive;
            maxInclusiveAnnotation = fBase.maxInclusiveAnnotation;
        }
        if ((( fBase.fFacetsDefined & FACET_MINEXCLUSIVE) != 0) &&
                !((fFacetsDefined & FACET_MINEXCLUSIVE) != 0) && !((fFacetsDefined & FACET_MININCLUSIVE) != 0)) {
            fFacetsDefined |= FACET_MINEXCLUSIVE;
            fMinExclusive = fBase.fMinExclusive;
            minExclusiveAnnotation = fBase.minExclusiveAnnotation;
        }
        if ((( fBase.fFacetsDefined & FACET_MININCLUSIVE) != 0) &&
                !((fFacetsDefined & FACET_MINEXCLUSIVE) != 0) && !((fFacetsDefined & FACET_MININCLUSIVE) != 0)) {
            fFacetsDefined |= FACET_MININCLUSIVE;
            fMinInclusive = fBase.fMinInclusive;
            minInclusiveAnnotation = fBase.minInclusiveAnnotation;
        }
        if ((( fBase.fFacetsDefined & FACET_TOTALDIGITS) != 0) &&
                !((fFacetsDefined & FACET_TOTALDIGITS) != 0)) {
            fFacetsDefined |= FACET_TOTALDIGITS;
            fTotalDigits = fBase.fTotalDigits;
            totalDigitsAnnotation = fBase.totalDigitsAnnotation;
        }
        if ((( fBase.fFacetsDefined & FACET_FRACTIONDIGITS) != 0)
                && !((fFacetsDefined & FACET_FRACTIONDIGITS) != 0)) {
            fFacetsDefined |= FACET_FRACTIONDIGITS;
            fFractionDigits = fBase.fFractionDigits;
            fractionDigitsAnnotation = fBase.fractionDigitsAnnotation;
        }
        if ((fPatternType == SPECIAL_PATTERN_NONE ) && (fBase.fPatternType != SPECIAL_PATTERN_NONE)) {
            fPatternType = fBase.fPatternType ;
        }

        fFixedFacet |= fBase.fFixedFacet;

        calcFundamentalFacets();

    } 

    /**
     * validate a value, and return the compiled form
     */
    public Object validate(String content, ValidationContext context, ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {

        if (context == null)
            context = fEmptyContext;

        if (validatedInfo == null)
            validatedInfo = new ValidatedInfo();
        else
            validatedInfo.memberType = null;

        boolean needNormalize = context==null||context.needToNormalize();
        Object ob = getActualValue(content, context, validatedInfo, needNormalize);

        validate(context, validatedInfo);

        return ob;

    }

    protected ValidatedInfo getActualEnumValue(String lexical, ValidationContext ctx, ValidatedInfo info)
    throws InvalidDatatypeValueException {
        return fBase.validateWithInfo(lexical, ctx, info);
    }

    /**
     * validate a value, and return the compiled form
     */
    public ValidatedInfo validateWithInfo(String content, ValidationContext context, ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {

        if (context == null)
            context = fEmptyContext;

        if (validatedInfo == null)
            validatedInfo = new ValidatedInfo();
        else
            validatedInfo.memberType = null;

        boolean needNormalize = context==null||context.needToNormalize();
        getActualValue(content, context, validatedInfo, needNormalize);

        validate(context, validatedInfo);

        return validatedInfo;

    }

    /**
     * validate a value, and return the compiled form
     */
    public Object validate(Object content, ValidationContext context, ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {

        if (context == null)
            context = fEmptyContext;

        if (validatedInfo == null)
            validatedInfo = new ValidatedInfo();
        else
            validatedInfo.memberType = null;

        boolean needNormalize = context==null||context.needToNormalize();
        Object ob = getActualValue(content, context, validatedInfo, needNormalize);

        validate(context, validatedInfo);

        return ob;

    }

    /**
     * validate an actual value against this DV
     *
     * @param context       the validation context
     * @param validatedInfo used to provide the actual value and member types
     */
    public void validate(ValidationContext context, ValidatedInfo validatedInfo)
        throws InvalidDatatypeValueException {

        if (context == null)
            context = fEmptyContext;

        if (context.needFacetChecking() &&
                (fFacetsDefined != 0 && fFacetsDefined != FACET_WHITESPACE)) {
            checkFacets(validatedInfo);
        }

        if (context.needExtraChecking()) {
            checkExtraRules(context, validatedInfo);
        }

    }

    private void checkFacets(ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {

        Object ob = validatedInfo.actualValue;
        String content = validatedInfo.normalizedValue;
        short type = validatedInfo.actualValueType;
        ShortList itemType = validatedInfo.itemValueTypes;

        if (fValidationDV != DV_QNAME && fValidationDV != DV_NOTATION) {
            int length = fDVs[fValidationDV].getDataLength(ob);

            if ( (fFacetsDefined & FACET_MAXLENGTH) != 0 ) {
                if ( length > fMaxLength ) {
                    throw new InvalidDatatypeValueException("cvc-maxLength-valid",
                            new Object[]{content, Integer.toString(length), Integer.toString(fMaxLength), fTypeName});
                }
            }

            if ( (fFacetsDefined & FACET_MINLENGTH) != 0 ) {
                if ( length < fMinLength ) {
                    throw new InvalidDatatypeValueException("cvc-minLength-valid",
                            new Object[]{content, Integer.toString(length), Integer.toString(fMinLength), fTypeName});
                }
            }

            if ( (fFacetsDefined & FACET_LENGTH) != 0 ) {
                if ( length != fLength ) {
                    throw new InvalidDatatypeValueException("cvc-length-valid",
                            new Object[]{content, Integer.toString(length), Integer.toString(fLength), fTypeName});
                }
            }
        }

        if ( ((fFacetsDefined & FACET_ENUMERATION) != 0 ) ) {
            boolean present = false;
            final int enumSize = fEnumerationSize;
            final short primitiveType1 = convertToPrimitiveKind(type);
            for (int i = 0; i < enumSize; i++) {
                final short primitiveType2 = convertToPrimitiveKind(fEnumeration[i].actualValueType);
                if ((primitiveType1 == primitiveType2 ||
                        primitiveType1 == XSConstants.ANYSIMPLETYPE_DT && primitiveType2 == XSConstants.STRING_DT ||
                        primitiveType1 == XSConstants.STRING_DT && primitiveType2 == XSConstants.ANYSIMPLETYPE_DT)
                        && fEnumeration[i].actualValue.equals(ob)) {
                    if (primitiveType1 == XSConstants.LIST_DT || primitiveType1 == XSConstants.LISTOFUNION_DT) {
                        ShortList enumItemType = fEnumeration[i].itemValueTypes;
                        final int typeList1Length = itemType != null ? itemType.getLength() : 0;
                        final int typeList2Length = enumItemType != null ? enumItemType.getLength() : 0;
                        if (typeList1Length == typeList2Length) {
                            int j;
                            for (j = 0; j < typeList1Length; ++j) {
                                final short primitiveItem1 = convertToPrimitiveKind(itemType.item(j));
                                final short primitiveItem2 = convertToPrimitiveKind(enumItemType.item(j));
                                if (primitiveItem1 != primitiveItem2) {
                                    if (primitiveItem1 == XSConstants.ANYSIMPLETYPE_DT && primitiveItem2 == XSConstants.STRING_DT ||
                                            primitiveItem1 == XSConstants.STRING_DT && primitiveItem2 == XSConstants.ANYSIMPLETYPE_DT) {
                                        continue;
                                    }
                                    break;
                                }
                            }
                            if (j == typeList1Length) {
                                present = true;
                                break;
                            }
                        }
                    }
                    else {
                        present = true;
                        break;
                    }
                }
            }
            if(!present){
                StringBuffer sb = new StringBuffer();
                appendEnumString(sb);
                throw new InvalidDatatypeValueException("cvc-enumeration-valid",
                        new Object [] {content, sb.toString()});
            }
        }

        if ((fFacetsDefined & FACET_FRACTIONDIGITS) != 0) {
            int scale = fDVs[fValidationDV].getFractionDigits(ob);
            if (scale > fFractionDigits) {
                throw new InvalidDatatypeValueException("cvc-fractionDigits-valid",
                        new Object[] {content, Integer.toString(scale), Integer.toString(fFractionDigits)});
            }
        }

        if ((fFacetsDefined & FACET_TOTALDIGITS)!=0) {
            int totalDigits = fDVs[fValidationDV].getTotalDigits(ob);
            if (totalDigits > fTotalDigits) {
                throw new InvalidDatatypeValueException("cvc-totalDigits-valid",
                        new Object[] {content, Integer.toString(totalDigits), Integer.toString(fTotalDigits)});
            }
        }

        int compare;

        if ( (fFacetsDefined & FACET_MAXINCLUSIVE) != 0 ) {
            compare = fDVs[fValidationDV].compare(ob, fMaxInclusive);
            if (compare != -1 && compare != 0) {
                throw new InvalidDatatypeValueException("cvc-maxInclusive-valid",
                        new Object[] {content, fMaxInclusive, fTypeName});
            }
        }

        if ( (fFacetsDefined & FACET_MAXEXCLUSIVE) != 0 ) {
            compare = fDVs[fValidationDV].compare(ob, fMaxExclusive );
            if (compare != -1) {
                throw new InvalidDatatypeValueException("cvc-maxExclusive-valid",
                        new Object[] {content, fMaxExclusive, fTypeName});
            }
        }

        if ( (fFacetsDefined & FACET_MININCLUSIVE) != 0 ) {
            compare = fDVs[fValidationDV].compare(ob, fMinInclusive);
            if (compare != 1 && compare != 0) {
                throw new InvalidDatatypeValueException("cvc-minInclusive-valid",
                        new Object[] {content, fMinInclusive, fTypeName});
            }
        }

        if ( (fFacetsDefined & FACET_MINEXCLUSIVE) != 0 ) {
            compare = fDVs[fValidationDV].compare(ob, fMinExclusive);
            if (compare != 1) {
                throw new InvalidDatatypeValueException("cvc-minExclusive-valid",
                        new Object[] {content, fMinExclusive, fTypeName});
            }
        }

    }

    private void checkExtraRules(ValidationContext context, ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {

        Object ob = validatedInfo.actualValue;

        if (fVariety == VARIETY_ATOMIC) {

            fDVs[fValidationDV].checkExtraRules(ob, context);

        } else if (fVariety == VARIETY_LIST) {

            ListDV.ListData values = (ListDV.ListData)ob;
            XSSimpleType memberType = validatedInfo.memberType;
            int len = values.getLength();
            try {
                if (fItemType.fVariety == VARIETY_UNION) {
                    XSSimpleTypeDecl[] memberTypes = (XSSimpleTypeDecl[])validatedInfo.memberTypes;
                    for (int i = len-1; i >= 0; i--) {
                        validatedInfo.actualValue = values.item(i);
                        validatedInfo.memberType = memberTypes[i];
                        fItemType.checkExtraRules(context, validatedInfo);
                    }
                } else { 
                    for (int i = len-1; i >= 0; i--) {
                        validatedInfo.actualValue = values.item(i);
                        fItemType.checkExtraRules(context, validatedInfo);
                    }
                }
            }
            finally {
                validatedInfo.actualValue = values;
                validatedInfo.memberType = memberType;
            }

        } else { 

            ((XSSimpleTypeDecl)validatedInfo.memberType).checkExtraRules(context, validatedInfo);

        }

    }

    private Object getActualValue(Object content, ValidationContext context,
            ValidatedInfo validatedInfo, boolean needNormalize)
    throws InvalidDatatypeValueException{

        String nvalue;
        if (needNormalize) {
            nvalue = normalize(content, fWhiteSpace);
        } else {
            nvalue = content.toString();
        }
        if ( (fFacetsDefined & FACET_PATTERN ) != 0 ) {
            RegularExpression regex;
            for (int idx = fPattern.size()-1; idx >= 0; idx--) {
                regex = fPattern.get(idx);
                if (!regex.matches(nvalue)){
                    throw new InvalidDatatypeValueException("cvc-pattern-valid",
                            new Object[]{content,
                            fPatternStr.get(idx),

                            fTypeName});
                }
            }
        }

        if (fVariety == VARIETY_ATOMIC) {

            if (fPatternType != SPECIAL_PATTERN_NONE) {

                boolean seenErr = false;
                if (fPatternType == SPECIAL_PATTERN_NMTOKEN) {
                    seenErr = !XMLChar.isValidNmtoken(nvalue);
                }
                else if (fPatternType == SPECIAL_PATTERN_NAME) {
                    seenErr = !XMLChar.isValidName(nvalue);
                }
                else if (fPatternType == SPECIAL_PATTERN_NCNAME) {
                    seenErr = !XMLChar.isValidNCName(nvalue);
                }
                if (seenErr) {
                    throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1",
                            new Object[]{nvalue, SPECIAL_PATTERN_STRING[fPatternType]});
                }
            }

            validatedInfo.normalizedValue = nvalue;
            Object avalue = fDVs[fValidationDV].getActualValue(nvalue, context);
            validatedInfo.actualValue = avalue;
            validatedInfo.actualValueType = fBuiltInKind;
            validatedInfo.actualType = this;

            return avalue;

        } else if (fVariety == VARIETY_LIST) {

            StringTokenizer parsedList = new StringTokenizer(nvalue, " ");
            int countOfTokens = parsedList.countTokens() ;
            Object[] avalue = new Object[countOfTokens];
            boolean isUnion = fItemType.getVariety() == VARIETY_UNION;
            short[] itemTypes = new short[isUnion ? countOfTokens : 1];
            if (!isUnion)
                itemTypes[0] = fItemType.fBuiltInKind;
            XSSimpleTypeDecl[] memberTypes = new XSSimpleTypeDecl[countOfTokens];
            for(int i = 0 ; i < countOfTokens ; i ++){
                avalue[i] = fItemType.getActualValue(parsedList.nextToken(), context, validatedInfo, false);
                if (context.needFacetChecking() &&
                        (fItemType.fFacetsDefined != 0 && fItemType.fFacetsDefined != FACET_WHITESPACE)) {
                    fItemType.checkFacets(validatedInfo);
                }
                memberTypes[i] = (XSSimpleTypeDecl)validatedInfo.memberType;
                if (isUnion)
                    itemTypes[i] = memberTypes[i].fBuiltInKind;
            }

            ListDV.ListData v = new ListDV.ListData(avalue);
            validatedInfo.actualValue = v;
            validatedInfo.actualValueType = isUnion ? XSConstants.LISTOFUNION_DT : XSConstants.LIST_DT;
            validatedInfo.memberType = null;
            validatedInfo.memberTypes = memberTypes;
            validatedInfo.itemValueTypes = new ShortListImpl(itemTypes, itemTypes.length);
            validatedInfo.normalizedValue = nvalue;
            validatedInfo.actualType = this;

            return v;

        } else { 
            final Object _content = (fMemberTypes.length > 1 && content != null) ? content.toString() : content;
            for (int i = 0; i < fMemberTypes.length; i++) {
                try {
                    Object aValue = fMemberTypes[i].getActualValue(_content, context, validatedInfo, true);
                    if (context.needFacetChecking() &&
                            (fMemberTypes[i].fFacetsDefined != 0 && fMemberTypes[i].fFacetsDefined != FACET_WHITESPACE)) {
                        fMemberTypes[i].checkFacets(validatedInfo);
                    }
                    validatedInfo.memberType = fMemberTypes[i];
                    validatedInfo.actualType = this;
                    return aValue;
                } catch(InvalidDatatypeValueException invalidValue) {
                }
            }
            StringBuffer typesBuffer = new StringBuffer();
            XSSimpleTypeDecl decl;
            for(int i = 0;i < fMemberTypes.length; i++) {
                if(i != 0)
                    typesBuffer.append(" | ");
                decl = fMemberTypes[i];
                if(decl.fTargetNamespace != null) {
                    typesBuffer.append('{');
                    typesBuffer.append(decl.fTargetNamespace);
                    typesBuffer.append('}');
                }
                typesBuffer.append(decl.fTypeName);
                if(decl.fEnumeration != null) {
                    typesBuffer.append(" : ");
                    decl.appendEnumString(typesBuffer);
                }
            }
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3",
                    new Object[]{content, fTypeName, typesBuffer.toString()});
        }

    }

    public boolean isEqual(Object value1, Object value2) {
        if (value1 == null) {
            return false;
        }
        return value1.equals(value2);
    }

    public boolean isIdentical (Object value1, Object value2) {
        if (value1 == null) {
            return false;
        }
        return fDVs[fValidationDV].isIdentical(value1, value2);
    }

    public static String normalize(String content, short ws) {
        int len = content == null ? 0 : content.length();
        if (len == 0 || ws == WS_PRESERVE)
            return content;

        StringBuffer sb = new StringBuffer();
        if (ws == WS_REPLACE) {
            char ch;
            for (int i = 0; i < len; i++) {
                ch = content.charAt(i);
                if (ch != 0x9 && ch != 0xa && ch != 0xd)
                    sb.append(ch);
                else
                    sb.append((char)0x20);
            }
        } else {
            char ch;
            int i;
            boolean isLeading = true;
            for (i = 0; i < len; i++) {
                ch = content.charAt(i);
                if (ch != 0x9 && ch != 0xa && ch != 0xd && ch != 0x20) {
                    sb.append(ch);
                    isLeading = false;
                }
                else {
                    for (; i < len-1; i++) {
                        ch = content.charAt(i+1);
                        if (ch != 0x9 && ch != 0xa && ch != 0xd && ch != 0x20)
                            break;
                    }
                    if (i < len - 1 && !isLeading)
                        sb.append((char)0x20);
                }
            }
        }

        return sb.toString();
    }

    protected String normalize(Object content, short ws) {
        if (content == null)
            return null;

        if ( (fFacetsDefined & FACET_PATTERN ) == 0 ) {
            short norm_type = fDVNormalizeType[fValidationDV];
            if (norm_type == NORMALIZE_NONE) {
                return content.toString();
            }
            else if (norm_type == NORMALIZE_TRIM) {
                return XMLChar.trim(content.toString());
            }
        }

        if (!(content instanceof StringBuffer)) {
            String strContent = content.toString();
            return normalize(strContent, ws);
        }

        StringBuffer sb = (StringBuffer)content;
        int len = sb.length();
        if (len == 0)
            return "";
        if (ws == WS_PRESERVE)
            return sb.toString();

        if (ws == WS_REPLACE) {
            char ch;
            for (int i = 0; i < len; i++) {
                ch = sb.charAt(i);
                if (ch == 0x9 || ch == 0xa || ch == 0xd)
                    sb.setCharAt(i, (char)0x20);
            }
        } else {
            char ch;
            int i, j = 0;
            boolean isLeading = true;
            for (i = 0; i < len; i++) {
                ch = sb.charAt(i);
                if (ch != 0x9 && ch != 0xa && ch != 0xd && ch != 0x20) {
                    sb.setCharAt(j++, ch);
                    isLeading = false;
                }
                else {
                    for (; i < len-1; i++) {
                        ch = sb.charAt(i+1);
                        if (ch != 0x9 && ch != 0xa && ch != 0xd && ch != 0x20)
                            break;
                    }
                    if (i < len - 1 && !isLeading)
                        sb.setCharAt(j++, (char)0x20);
                }
            }
            sb.setLength(j);
        }

        return sb.toString();
    }

    void reportError(String key, Object[] args) throws InvalidDatatypeFacetException {
        throw new InvalidDatatypeFacetException(key, args);
    }


    private String whiteSpaceValue(short ws){
        return WS_FACET_STRING[ws];
    }

    /**
     *  Fundamental Facet: ordered.
     */
    public short getOrdered() {
        return fOrdered;
    }

    /**
     * Fundamental Facet: bounded.
     */
    public boolean getBounded(){
        return fBounded;
    }

    /**
     * Fundamental Facet: cardinality.
     */
    public boolean getFinite(){
        return fFinite;
    }

    /**
     * Fundamental Facet: numeric.
     */
    public boolean getNumeric(){
        return fNumeric;
    }

    /**
     * Convenience method. [Facets]: check whether a facet is defined on this
     * type.
     * @param facetName  The name of the facet.
     * @return  True if the facet is defined, false otherwise.
     */
    public boolean isDefinedFacet(short facetName) {
        if (fValidationDV == DV_ANYSIMPLETYPE ||
            fValidationDV == DV_ANYATOMICTYPE) {
            return false;
        }
        if ((fFacetsDefined & facetName) != 0) {
            return true;
        }
        if (fPatternType != SPECIAL_PATTERN_NONE) {
            return facetName == FACET_PATTERN;
        }
        if (fValidationDV == DV_INTEGER) {
            return facetName == FACET_PATTERN || facetName == FACET_FRACTIONDIGITS;
        }
        return false;
    }

    /**
     * [facets]: all facets defined on this type. The value is a bit
     * combination of FACET_XXX constants of all defined facets.
     */
    public short getDefinedFacets() {
        if (fValidationDV == DV_ANYSIMPLETYPE ||
            fValidationDV == DV_ANYATOMICTYPE) {
            return FACET_NONE;
        }
        if (fPatternType != SPECIAL_PATTERN_NONE) {
            return (short)(fFacetsDefined | FACET_PATTERN);
        }
        if (fValidationDV == DV_INTEGER) {
            return (short)(fFacetsDefined | FACET_PATTERN | FACET_FRACTIONDIGITS);
        }
        return fFacetsDefined;
    }

    /**
     * Convenience method. [Facets]: check whether a facet is defined and
     * fixed on this type.
     * @param facetName  The name of the facet.
     * @return  True if the facet is fixed, false otherwise.
     */
    public boolean isFixedFacet(short facetName) {
        if ((fFixedFacet & facetName) != 0)
            return true;
        if (fValidationDV == DV_INTEGER)
            return facetName == FACET_FRACTIONDIGITS;
        return false;
    }

    /**
     * [facets]: all defined facets for this type which are fixed.
     */
    public short getFixedFacets() {
        if (fValidationDV == DV_INTEGER)
            return (short)(fFixedFacet | FACET_FRACTIONDIGITS);
        return fFixedFacet;
    }

    /**
     * Convenience method. Returns a value of a single constraining facet for
     * this simple type definition. This method must not be used to retrieve
     * values for <code>enumeration</code> and <code>pattern</code> facets.
     * @param facetName The name of the facet, i.e.
     *   <code>FACET_LENGTH, FACET_TOTALDIGITS </code> (see
     *   <code>XSConstants</code>). To retrieve the value for a pattern or
     *   an enumeration, see <code>enumeration</code> and
     *   <code>pattern</code>.
     * @return A value of the facet specified in <code>facetName</code> for
     *   this simple type definition or <code>null</code>.
     */
    public String getLexicalFacetValue(short facetName) {
        switch (facetName) {
            case FACET_LENGTH:
                return (fLength == -1)?null:Integer.toString(fLength);
            case FACET_MINLENGTH:
                return (fMinLength == -1)?null:Integer.toString(fMinLength);
            case FACET_MAXLENGTH:
                return (fMaxLength == -1)?null:Integer.toString(fMaxLength);
            case FACET_WHITESPACE:
                if (fValidationDV == DV_ANYSIMPLETYPE ||
                    fValidationDV == DV_ANYATOMICTYPE) {
                    return null;
                }
                return WS_FACET_STRING[fWhiteSpace];
            case FACET_MAXINCLUSIVE:
                return (fMaxInclusive == null)?null:fMaxInclusive.toString();
            case FACET_MAXEXCLUSIVE:
                return (fMaxExclusive == null)?null:fMaxExclusive.toString();
            case FACET_MINEXCLUSIVE:
                return (fMinExclusive == null)?null:fMinExclusive.toString();
            case FACET_MININCLUSIVE:
                return (fMinInclusive == null)?null:fMinInclusive.toString();
            case FACET_TOTALDIGITS:
                return (fTotalDigits == -1)?null:Integer.toString(fTotalDigits);
            case FACET_FRACTIONDIGITS:
                if (fValidationDV == DV_INTEGER) {
                    return "0";
                }
                return (fFractionDigits == -1)?null:Integer.toString(fFractionDigits);
        }
        return null;
    }

    /**
     * A list of enumeration values if it exists, otherwise an empty
     * <code>StringList</code>.
     */
    public StringList getLexicalEnumeration() {
        if (fLexicalEnumeration == null){
            if (fEnumeration == null)
                return StringListImpl.EMPTY_LIST;
            int size = fEnumerationSize;
            String[] strs = new String[size];
            for (int i = 0; i < size; i++)
                strs[i] = fEnumeration[i].normalizedValue;
            fLexicalEnumeration = new StringListImpl(strs, size);
        }
        return fLexicalEnumeration;
    }

    /**
     * A list of actual enumeration values if it exists, otherwise an empty
     * <code>ObjectList</code>.
     */
    public ObjectList getActualEnumeration() {
        if (fActualEnumeration == null) {
            fActualEnumeration = new AbstractObjectList() {
                public int getLength() {
                    return (fEnumeration != null) ? fEnumerationSize : 0;
                }
                public boolean contains(Object item) {
                    if (fEnumeration == null) {
                        return false;
                    }
                    for (int i = 0; i < fEnumerationSize; i++) {
                        if (fEnumeration[i].getActualValue().equals(item)) {
                            return true;
                        }
                    }
                    return false;
                }
                public Object item(int index) {
                    if (index < 0 || index >= getLength()) {
                        return null;
                    }
                    return fEnumeration[index].getActualValue();
                }
            };
        }
        return fActualEnumeration;
    }

    /**
     * A list of enumeration type values (as a list of ShortList objects) if it exists, otherwise returns
     * null
     */
    public ObjectList getEnumerationItemTypeList() {
        if (fEnumerationItemTypeList == null) {
            if (fEnumeration == null) {
                return null;
            }
            fEnumerationItemTypeList = new AbstractObjectList() {
                public int getLength() {
                    return (fEnumeration != null) ? fEnumerationSize : 0;
                }
                public boolean contains(Object item) {
                    if (fEnumeration == null || !(item instanceof ShortList))
                        return false;
                    for (int i = 0;i < fEnumerationSize; i++)
                        if (fEnumeration[i].itemValueTypes == item)
                            return true;
                    return false;
                }
                public Object item(int index) {
                    if (index < 0 || index >= getLength()) {
                        return null;
                    }
                    return fEnumeration[index].itemValueTypes;
                }
            };
        }
        return fEnumerationItemTypeList;
    }

    public ShortList getEnumerationTypeList() {
        if (fEnumerationTypeList == null) {
            if (fEnumeration == null) {
                return ShortListImpl.EMPTY_LIST;
            }
            short[] list = new short[fEnumerationSize];
            for (int i = 0; i < fEnumerationSize; i++) {
                list[i] = fEnumeration[i].actualValueType;
            }
            fEnumerationTypeList = new ShortListImpl(list, fEnumerationSize);
        }
        return fEnumerationTypeList;
    }

    /**
     * A list of pattern values if it exists, otherwise an empty
     * <code>StringList</code>.
     */
    public StringList getLexicalPattern() {
        if (fPatternType == SPECIAL_PATTERN_NONE && fValidationDV != DV_INTEGER && fPatternStr == null)
            return StringListImpl.EMPTY_LIST;
        if (fLexicalPattern == null){
            int size = fPatternStr == null ? 0 : fPatternStr.size();
            String[] strs;
            if (fPatternType == SPECIAL_PATTERN_NMTOKEN) {
                strs = new String[size+1];
                strs[size] = "\\c+";
            }
            else if (fPatternType == SPECIAL_PATTERN_NAME) {
                strs = new String[size+1];
                strs[size] = "\\i\\c*";
            }
            else if (fPatternType == SPECIAL_PATTERN_NCNAME) {
                strs = new String[size+2];
                strs[size] = "\\i\\c*";
                strs[size+1] = "[\\i-[:]][\\c-[:]]*";
            }
            else if (fValidationDV == DV_INTEGER) {
                strs = new String[size+1];
                strs[size] = "[\\-+]?[0-9]+";
            }
            else {
                strs = new String[size];
            }
            for (int i = 0; i < size; i++)
                strs[i] = fPatternStr.get(i);
            fLexicalPattern = new StringListImpl(strs, strs.length);
        }
        return fLexicalPattern;
    }

    /**
     * [annotations]: a set of annotations for this simple type component if
     * it exists, otherwise an empty <code>XSObjectList</code>.
     */
    public XSObjectList getAnnotations() {
        return (fAnnotations != null) ? fAnnotations : XSObjectListImpl.EMPTY_LIST;
    }

    private void calcFundamentalFacets() {
        setOrdered();
        setNumeric();
        setBounded();
        setCardinality();
    }

    private void setOrdered(){

        if(fVariety == VARIETY_ATOMIC){
            this.fOrdered = fBase.fOrdered;
        }

        else if(fVariety == VARIETY_LIST){
            this.fOrdered = ORDERED_FALSE;
        }

        else if(fVariety == VARIETY_UNION){
            int length = fMemberTypes.length;
            if (length == 0) {
                this.fOrdered = ORDERED_PARTIAL;
                return;
            }
            short ancestorId = getPrimitiveDV(fMemberTypes[0].fValidationDV);
            boolean commonAnc = ancestorId != DV_ANYSIMPLETYPE;
            boolean allFalse = fMemberTypes[0].fOrdered == ORDERED_FALSE;
            for (int i = 1; i < fMemberTypes.length && (commonAnc || allFalse); i++) {
                if (commonAnc)
                    commonAnc = ancestorId == getPrimitiveDV(fMemberTypes[i].fValidationDV);
                if (allFalse)
                    allFalse = fMemberTypes[i].fOrdered == ORDERED_FALSE;
            }
            if (commonAnc) {
                this.fOrdered = fMemberTypes[0].fOrdered;
            } else if (allFalse) {
                this.fOrdered = ORDERED_FALSE;
            } else {
                this.fOrdered = ORDERED_PARTIAL;
            }
        }

    }

    private void setNumeric(){
        if(fVariety == VARIETY_ATOMIC){
            this.fNumeric = fBase.fNumeric;
        }
        else if(fVariety == VARIETY_LIST){
            this.fNumeric = false;
        }
        else if(fVariety == VARIETY_UNION){
            XSSimpleType[] memberTypes = fMemberTypes;
            for(int i = 0 ; i < memberTypes.length ; i++){
                if(!memberTypes[i].getNumeric() ){
                    this.fNumeric = false;
                    return;
                }
            }
            this.fNumeric = true;
        }

    }

    private void setBounded(){
        if(fVariety == VARIETY_ATOMIC){
            if( (((this.fFacetsDefined & FACET_MININCLUSIVE) != 0)  || ((this.fFacetsDefined & FACET_MINEXCLUSIVE) != 0))
                    &&  (((this.fFacetsDefined & FACET_MAXINCLUSIVE) != 0)  || ((this.fFacetsDefined & FACET_MAXEXCLUSIVE) != 0)) ){
                this.fBounded = true;
            }
            else{
                this.fBounded = false;
            }
        }
        else if(fVariety == VARIETY_LIST){
            if( ((this.fFacetsDefined & FACET_LENGTH) != 0 ) || ( ((this.fFacetsDefined & FACET_MINLENGTH) != 0 )
                    &&  ((this.fFacetsDefined & FACET_MAXLENGTH) != 0 )) ){
                this.fBounded = true;
            }
            else{
                this.fBounded = false;
            }

        }
        else if(fVariety == VARIETY_UNION){

            XSSimpleTypeDecl [] memberTypes = this.fMemberTypes;
            short ancestorId = 0 ;

            if(memberTypes.length > 0){
                ancestorId = getPrimitiveDV(memberTypes[0].fValidationDV);
            }

            for(int i = 0 ; i < memberTypes.length ; i++){
                if(!memberTypes[i].getBounded() || (ancestorId != getPrimitiveDV(memberTypes[i].fValidationDV)) ){
                    this.fBounded = false;
                    return;
                }
            }
            this.fBounded = true;
        }

    }

    private boolean specialCardinalityCheck(){
        if( (fBase.fValidationDV == XSSimpleTypeDecl.DV_DATE) || (fBase.fValidationDV == XSSimpleTypeDecl.DV_GYEARMONTH)
                || (fBase.fValidationDV == XSSimpleTypeDecl.DV_GYEAR) || (fBase.fValidationDV == XSSimpleTypeDecl.DV_GMONTHDAY)
                || (fBase.fValidationDV == XSSimpleTypeDecl.DV_GDAY) || (fBase.fValidationDV == XSSimpleTypeDecl.DV_GMONTH) ){
            return true;
        }
        return false;

    } 

    private void setCardinality(){
        if(fVariety == VARIETY_ATOMIC){
            if(fBase.fFinite){
                this.fFinite = true;
            }
            else {
                if ( ((this.fFacetsDefined & FACET_LENGTH) != 0 ) || ((this.fFacetsDefined & FACET_MAXLENGTH) != 0 )
                        || ((this.fFacetsDefined & FACET_TOTALDIGITS) != 0 ) ){
                    this.fFinite = true;
                }
                else if( (((this.fFacetsDefined & FACET_MININCLUSIVE) != 0 ) || ((this.fFacetsDefined & FACET_MINEXCLUSIVE) != 0 ))
                        && (((this.fFacetsDefined & FACET_MAXINCLUSIVE) != 0 ) || ((this.fFacetsDefined & FACET_MAXEXCLUSIVE) != 0 )) ){
                    if( ((this.fFacetsDefined & FACET_FRACTIONDIGITS) != 0 ) || specialCardinalityCheck()){
                        this.fFinite = true;
                    }
                    else{
                        this.fFinite = false;
                    }
                }
                else{
                    this.fFinite = false;
                }
            }
        }
        else if(fVariety == VARIETY_LIST){
            if( ((this.fFacetsDefined & FACET_LENGTH) != 0 ) || ( ((this.fFacetsDefined & FACET_MINLENGTH) != 0 )
                    && ((this.fFacetsDefined & FACET_MAXLENGTH) != 0 )) ){
                this.fFinite = true;
            }
            else{
                this.fFinite = false;
            }

        }
        else if(fVariety == VARIETY_UNION){
            XSSimpleType [] memberTypes = fMemberTypes;
            for(int i = 0 ; i < memberTypes.length ; i++){
                if(!(memberTypes[i].getFinite()) ){
                    this.fFinite = false;
                    return;
                }
            }
            this.fFinite = true;
        }

    }

    private short getPrimitiveDV(short validationDV){

        if (validationDV == DV_ID || validationDV == DV_IDREF || validationDV == DV_ENTITY){
            return DV_STRING;
        }
        else if (validationDV == DV_INTEGER) {
            return DV_DECIMAL;
        }
        else if (Constants.SCHEMA_1_1_SUPPORT && (validationDV == DV_YEARMONTHDURATION || validationDV == DV_DAYTIMEDURATION)) {
            return DV_DURATION;
        }
        else {
            return validationDV;
        }

    }

    public boolean derivedFromType(XSTypeDefinition ancestor, short derivation) {

        if (ancestor == null) {
            return false;
        }
        while (ancestor instanceof XSSimpleTypeDelegate) {
            ancestor = ((XSSimpleTypeDelegate) ancestor).type;
        }
        if (ancestor.getBaseType() == ancestor) {
            return true;
        }
        XSTypeDefinition type = this;
        while (type != ancestor &&                      
                type != fAnySimpleType) {  
            type = type.getBaseType();
        }
        return type == ancestor;
    }

    public boolean derivedFrom(String ancestorNS, String ancestorName, short derivation) {

        if (ancestorName == null)
            return false;
        if (URI_SCHEMAFORSCHEMA.equals(ancestorNS) &&
                ANY_TYPE.equals(ancestorName)) {
            return true;
        }

        XSTypeDefinition type = this;
        while (!(ancestorName.equals(type.getName()) &&
                ((ancestorNS == null && type.getNamespace() == null) ||
                        (ancestorNS != null && ancestorNS.equals(type.getNamespace())))) &&   
                        type != fAnySimpleType) {  
            type = type.getBaseType();
        }

        return type != fAnySimpleType;
    }

    /**
     * Checks if a type is derived from another by restriction, given the name
     * and namespace. See:
     * http:
     *
     * @param ancestorNS
     *            The namspace of the ancestor type declaration
     * @param ancestorName
     *            The name of the ancestor type declaration
     * @param derivationMethod
     *            The derivation method
     *
     * @return boolean True if the ancestor type is derived from the reference type by the specifiied derivation method.
     */
    public boolean isDOMDerivedFrom(String ancestorNS, String ancestorName, int derivationMethod) {

        if (ancestorName == null)
            return false;

        if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(ancestorNS)
                && SchemaSymbols.ATTVAL_ANYTYPE.equals(ancestorName)
                && (((derivationMethod  & DERIVATION_RESTRICTION) != 0)
                        || (derivationMethod  == DERIVATION_ANY))) {
            return true;
        }

        if ((derivationMethod & DERIVATION_RESTRICTION) != 0) {
            if (isDerivedByRestriction(ancestorNS, ancestorName, this)) {
                return true;
            }
        }

        if ((derivationMethod & DERIVATION_LIST) != 0) {
            if (isDerivedByList(ancestorNS, ancestorName, this)) {
                return true;
            }
        }

        if ((derivationMethod & DERIVATION_UNION) != 0) {
            if (isDerivedByUnion(ancestorNS, ancestorName, this)) {
                return true;
            }
        }

        if (((derivationMethod & DERIVATION_EXTENSION) != 0)
                && (((derivationMethod & DERIVATION_RESTRICTION) == 0)
                        && ((derivationMethod & DERIVATION_LIST) == 0)
                        && ((derivationMethod & DERIVATION_UNION) == 0))) {
            return false;
        }

        if (((derivationMethod & DERIVATION_EXTENSION) == 0)
                && (((derivationMethod & DERIVATION_RESTRICTION) == 0)
                        && ((derivationMethod & DERIVATION_LIST) == 0)
                        && ((derivationMethod & DERIVATION_UNION) == 0))) {
            return isDerivedByAny(ancestorNS, ancestorName, this);
        }

        return false;
    }


    /**
     * Checks if a type is derived from another by any combination of restriction, list ir union. See:
     * http:
     *
     * @param ancestorNS
     *            The namspace of the ancestor type declaration
     * @param ancestorName
     *            The name of the ancestor type declaration
     * @param type
     *            The reference type definition
     *
     * @return boolean True if the type is derived by restriciton for the reference type
     */
    private boolean isDerivedByAny(String ancestorNS, String ancestorName,
            XSTypeDefinition type) {

        boolean derivedFrom = false;
        XSTypeDefinition oldType = null;
        while (type != null && type != oldType)  {

            if ((ancestorName.equals(type.getName()))
                    && ((ancestorNS == null && type.getNamespace() == null)
                            || (ancestorNS != null && ancestorNS.equals(type.getNamespace())))) {
                derivedFrom = true;
                break;
            }

            if (isDerivedByRestriction(ancestorNS, ancestorName, type)) {
                return true;
            } else if (isDerivedByList(ancestorNS, ancestorName, type)) {
                return true;
            } else  if (isDerivedByUnion(ancestorNS, ancestorName, type)) {
                return true;
            }
            oldType = type;
            if (((XSSimpleTypeDecl) type).getVariety() == VARIETY_ABSENT
                    || ((XSSimpleTypeDecl) type).getVariety() == VARIETY_ATOMIC) {
                type = type.getBaseType();
            } else if (((XSSimpleTypeDecl) type).getVariety() == VARIETY_UNION) {
                for (int i = 0; i < ((XSSimpleTypeDecl) type).getMemberTypes().getLength(); i++) {
                    return isDerivedByAny(ancestorNS, ancestorName,
                            (XSTypeDefinition) ((XSSimpleTypeDecl) type)
                            .getMemberTypes().item(i));
                }
            } else if (((XSSimpleTypeDecl) type).getVariety() == VARIETY_LIST) {
                type = ((XSSimpleTypeDecl) type).getItemType();
            }
        }

        return derivedFrom;
    }

    /**
     * DOM Level 3
     * Checks if a type is derived from another by restriction. See:
     * http:
     *
     * @param ancestorNS
     *            The namspace of the ancestor type declaration
     * @param ancestorName
     *            The name of the ancestor type declaration
     * @param type
     *            The reference type definition
     *
     * @return boolean True if the type is derived by restriciton for the
     *         reference type
     */
    private boolean isDerivedByRestriction (String ancestorNS, String ancestorName, XSTypeDefinition type) {
        XSTypeDefinition oldType = null;
        while (type != null && type != oldType) {
            if ((ancestorName.equals(type.getName()))
                    && ((ancestorNS != null && ancestorNS.equals(type.getNamespace()))
                            || (type.getNamespace() == null && ancestorNS == null))) {

                return true;
            }
            oldType = type;
            type = type.getBaseType();
        }

        return false;
    }

    /**
     * Checks if a type is derived from another by list. See:
     * http:
     *
     * @param ancestorNS
     *            The namspace of the ancestor type declaration
     * @param ancestorName
     *            The name of the ancestor type declaration
     * @param type
     *            The reference type definition
     *
     * @return boolean True if the type is derived by list for the reference type
     */
    private boolean isDerivedByList (String ancestorNS, String ancestorName, XSTypeDefinition type) {
        if (type !=null && ((XSSimpleTypeDefinition)type).getVariety() == VARIETY_LIST) {

            XSTypeDefinition itemType = ((XSSimpleTypeDefinition)type).getItemType();

            if (itemType != null) {

                if (isDerivedByRestriction(ancestorNS, ancestorName, itemType)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a type is derived from another by union.  See:
     * http:
     *
     * @param ancestorNS
     *            The namspace of the ancestor type declaration
     * @param ancestorName
     *            The name of the ancestor type declaration
     * @param type
     *            The reference type definition
     *
     * @return boolean True if the type is derived by union for the reference type
     */
    private boolean isDerivedByUnion (String ancestorNS, String ancestorName, XSTypeDefinition type) {

        if (type !=null && ((XSSimpleTypeDefinition)type).getVariety() == VARIETY_UNION) {

            XSObjectList memberTypes = ((XSSimpleTypeDefinition)type).getMemberTypes();

            for (int i = 0; i < memberTypes.getLength(); i++) {
                if (memberTypes.item(i) != null) {
                    if (isDerivedByRestriction(ancestorNS, ancestorName,(XSSimpleTypeDefinition)memberTypes.item(i))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    static final XSSimpleTypeDecl fAnySimpleType = new XSSimpleTypeDecl(null, "anySimpleType", DV_ANYSIMPLETYPE, ORDERED_FALSE, false, true, false, true, XSConstants.ANYSIMPLETYPE_DT);

    static final XSSimpleTypeDecl fAnyAtomicType = new XSSimpleTypeDecl(fAnySimpleType, "anyAtomicType", DV_ANYATOMICTYPE, ORDERED_FALSE, false, true, false, true, XSSimpleTypeDecl.ANYATOMICTYPE_DT);

    /**
     * Validation context used to validate facet values.
     */
    static final ValidationContext fDummyContext = new ValidationContext() {
        public boolean needFacetChecking() {
            return true;
        }

        public boolean needExtraChecking() {
            return false;
        }
        public boolean needToNormalize() {
            return false;
        }
        public boolean useNamespaces() {
            return true;
        }

        public boolean isEntityDeclared(String name) {
            return false;
        }

        public boolean isEntityUnparsed(String name) {
            return false;
        }

        public boolean isIdDeclared(String name) {
            return false;
        }

        public void addId(String name) {
        }

        public void addIdRef(String name) {
        }

        public String getSymbol (String symbol) {
            return symbol.intern();
        }

        public String getURI(String prefix) {
            return null;
        }

        public Locale getLocale() {
            return Locale.getDefault();
        }
    };

    private boolean fAnonymous = false;

    /**
     * A wrapper of ValidationContext, to provide a way of switching to a
     * different Namespace declaration context.
     */
    static final class ValidationContextImpl implements ValidationContext {

        final ValidationContext fExternal;

        ValidationContextImpl(ValidationContext external) {
            fExternal = external;
        }

        NamespaceContext fNSContext;
        void setNSContext(NamespaceContext nsContext) {
            fNSContext = nsContext;
        }

        public boolean needFacetChecking() {
            return fExternal.needFacetChecking();
        }

        public boolean needExtraChecking() {
            return fExternal.needExtraChecking();
        }
        public boolean needToNormalize() {
            return fExternal.needToNormalize();
        }
        public boolean useNamespaces() {
            return true;
        }

        public boolean isEntityDeclared (String name) {
            return fExternal.isEntityDeclared(name);
        }

        public boolean isEntityUnparsed (String name) {
            return fExternal.isEntityUnparsed(name);
        }

        public boolean isIdDeclared (String name) {
            return fExternal.isIdDeclared(name);
        }

        public void addId(String name) {
            fExternal.addId(name);
        }

        public void addIdRef(String name) {
            fExternal.addIdRef(name);
        }

        public String getSymbol (String symbol) {
            return fExternal.getSymbol(symbol);
        }

        public String getURI(String prefix) {
            if (fNSContext == null) {
                return fExternal.getURI(prefix);
            }
            else {
                return fNSContext.getURI(prefix);
            }
        }

        public Locale getLocale() {
            return fExternal.getLocale();
        }
    }

    public void reset(){

        if (fIsImmutable) return;
        fItemType = null;
        fMemberTypes = null;

        fTypeName = null;
        fTargetNamespace = null;
        fFinalSet = 0;
        fBase = null;
        fVariety = -1;
        fValidationDV = -1;

        fFacetsDefined = 0;
        fFixedFacet = 0;

        fWhiteSpace = 0;
        fLength = -1;
        fMinLength = -1;
        fMaxLength = -1;
        fTotalDigits = -1;
        fFractionDigits = -1;
        fPattern = null;
        fPatternStr = null;
        fEnumeration = null;
        fLexicalPattern = null;
        fLexicalEnumeration = null;
        fActualEnumeration = null;
        fEnumerationTypeList = null;
        fEnumerationItemTypeList = null;
        fMaxInclusive = null;
        fMaxExclusive = null;
        fMinExclusive = null;
        fMinInclusive = null;
        lengthAnnotation = null;
        minLengthAnnotation = null;
        maxLengthAnnotation = null;
        whiteSpaceAnnotation = null;
        totalDigitsAnnotation = null;
        fractionDigitsAnnotation = null;
        patternAnnotations = null;
        enumerationAnnotations = null;
        maxInclusiveAnnotation = null;
        maxExclusiveAnnotation = null;
        minInclusiveAnnotation = null;
        minExclusiveAnnotation = null;

        fPatternType = SPECIAL_PATTERN_NONE;
        fAnnotations = null;
        fFacets = null;

    }

    /**
     * @see com.sun.org.apache.xerces.internal.xs.XSObject#getNamespaceItem()
     */
    public XSNamespaceItem getNamespaceItem() {
        return fNamespaceItem;
    }

    public void setNamespaceItem(XSNamespaceItem namespaceItem) {
        fNamespaceItem = namespaceItem;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return this.fTargetNamespace+"," +this.fTypeName;
    }

    /**
     *  A list of constraining facets if it exists, otherwise an empty
     * <code>XSObjectList</code>. Note: This method must not be used to
     * retrieve values for <code>enumeration</code> and <code>pattern</code>
     * facets.
     */
    public XSObjectList getFacets() {
        if (fFacets == null &&
                (fFacetsDefined != 0 || fValidationDV == DV_INTEGER)) {

            XSFacetImpl[] facets = new XSFacetImpl[10];
            int count = 0;
            if ((fFacetsDefined & FACET_WHITESPACE) != 0 &&
                fValidationDV != DV_ANYSIMPLETYPE &&
                fValidationDV != DV_ANYATOMICTYPE) {
                facets[count] =
                    new XSFacetImpl(
                            FACET_WHITESPACE,
                            WS_FACET_STRING[fWhiteSpace],
                            0,
                            null,
                            (fFixedFacet & FACET_WHITESPACE) != 0,
                            whiteSpaceAnnotation);
                count++;
            }
            if (fLength != -1) {
                facets[count] =
                    new XSFacetImpl(
                            FACET_LENGTH,
                            Integer.toString(fLength),
                            fLength,
                            null,
                            (fFixedFacet & FACET_LENGTH) != 0,
                            lengthAnnotation);
                count++;
            }
            if (fMinLength != -1) {
                facets[count] =
                    new XSFacetImpl(
                            FACET_MINLENGTH,
                            Integer.toString(fMinLength),
                            fMinLength,
                            null,
                            (fFixedFacet & FACET_MINLENGTH) != 0,
                            minLengthAnnotation);
                count++;
            }
            if (fMaxLength != -1) {
                facets[count] =
                    new XSFacetImpl(
                            FACET_MAXLENGTH,
                            Integer.toString(fMaxLength),
                            fMaxLength,
                            null,
                            (fFixedFacet & FACET_MAXLENGTH) != 0,
                            maxLengthAnnotation);
                count++;
            }
            if (fTotalDigits != -1) {
                facets[count] =
                    new XSFacetImpl(
                            FACET_TOTALDIGITS,
                            Integer.toString(fTotalDigits),
                            fTotalDigits,
                            null,
                            (fFixedFacet & FACET_TOTALDIGITS) != 0,
                            totalDigitsAnnotation);
                count++;
            }
            if (fValidationDV == DV_INTEGER) {
                facets[count] =
                    new XSFacetImpl(
                            FACET_FRACTIONDIGITS,
                            "0",
                            0,
                            null,
                            true,
                            fractionDigitsAnnotation);
                count++;
            }
            else if (fFractionDigits != -1) {
                facets[count] =
                    new XSFacetImpl(
                            FACET_FRACTIONDIGITS,
                            Integer.toString(fFractionDigits),
                            fFractionDigits,
                            null,
                            (fFixedFacet & FACET_FRACTIONDIGITS) != 0,
                            fractionDigitsAnnotation);
                count++;
            }
            if (fMaxInclusive != null) {
                facets[count] =
                    new XSFacetImpl(
                            FACET_MAXINCLUSIVE,
                            fMaxInclusive.toString(),
                            0,
                            fMaxInclusive,
                            (fFixedFacet & FACET_MAXINCLUSIVE) != 0,
                            maxInclusiveAnnotation);
                count++;
            }
            if (fMaxExclusive != null) {
                facets[count] =
                    new XSFacetImpl(
                            FACET_MAXEXCLUSIVE,
                            fMaxExclusive.toString(),
                            0,
                            fMaxExclusive,
                            (fFixedFacet & FACET_MAXEXCLUSIVE) != 0,
                            maxExclusiveAnnotation);
                count++;
            }
            if (fMinExclusive != null) {
                facets[count] =
                    new XSFacetImpl(
                            FACET_MINEXCLUSIVE,
                            fMinExclusive.toString(),
                            0,
                            fMinExclusive,
                            (fFixedFacet & FACET_MINEXCLUSIVE) != 0,
                            minExclusiveAnnotation);
                count++;
            }
            if (fMinInclusive != null) {
                facets[count] =
                    new XSFacetImpl(
                            FACET_MININCLUSIVE,
                            fMinInclusive.toString(),
                            0,
                            fMinInclusive,
                            (fFixedFacet & FACET_MININCLUSIVE) != 0,
                            minInclusiveAnnotation);
                count++;
            }
            fFacets = (count > 0) ? new XSObjectListImpl(facets, count) : XSObjectListImpl.EMPTY_LIST;
        }
        return (fFacets != null) ? fFacets : XSObjectListImpl.EMPTY_LIST;
    }

    public XSObject getFacet(int facetType) {
        if (facetType == FACET_ENUMERATION || facetType == FACET_PATTERN) {
            XSObjectList list = getMultiValueFacets();
            for (int i = 0; i < list.getLength(); i++) {
                XSMultiValueFacet f = (XSMultiValueFacet)list.item(i);
                if (f.getFacetKind() == facetType) {
                    return f;
                }
            }
        }
        else {
            XSObjectList list = getFacets();
            for (int i = 0; i < list.getLength(); i++) {
                XSFacet f = (XSFacet)list.item(i);
                if (f.getFacetKind() == facetType) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     *  A list of enumeration and pattern constraining facets if it exists,
     * otherwise an empty <code>XSObjectList</code>.
     */
    public XSObjectList getMultiValueFacets() {
        if (fMultiValueFacets == null &&
                ((fFacetsDefined & FACET_ENUMERATION) != 0 ||
                        (fFacetsDefined & FACET_PATTERN) != 0 ||
                        fPatternType != SPECIAL_PATTERN_NONE ||
                        fValidationDV == DV_INTEGER)) {

            XSMVFacetImpl[] facets = new XSMVFacetImpl[2];
            int count = 0;
            if ((fFacetsDefined & FACET_PATTERN) != 0 ||
                    fPatternType != SPECIAL_PATTERN_NONE ||
                    fValidationDV == DV_INTEGER) {
                facets[count] =
                    new XSMVFacetImpl(
                            FACET_PATTERN,
                            this.getLexicalPattern(),
                            null,
                            patternAnnotations);
                count++;
            }
            if (fEnumeration != null) {
                facets[count] =
                    new XSMVFacetImpl(
                            FACET_ENUMERATION,
                            this.getLexicalEnumeration(),
                            new ObjectListImpl(fEnumeration, fEnumerationSize),
                            enumerationAnnotations);
                count++;
            }
            fMultiValueFacets = new XSObjectListImpl(facets, count);
        }
        return (fMultiValueFacets != null) ?
                fMultiValueFacets : XSObjectListImpl.EMPTY_LIST;
    }

    public Object getMinInclusiveValue() {
        return fMinInclusive;
    }

    public Object getMinExclusiveValue() {
        return fMinExclusive;
    }

    public Object getMaxInclusiveValue() {
        return fMaxInclusive;
    }

    public Object getMaxExclusiveValue() {
        return fMaxExclusive;
    }

    public void setAnonymous(boolean anon) {
        fAnonymous = anon;
    }

    private static final class XSFacetImpl implements XSFacet {
        final short kind;
        final String svalue;
        final int ivalue;
        Object avalue;
        final boolean fixed;
        final XSObjectList annotations;

        public XSFacetImpl(short kind, String svalue, int ivalue, Object avalue, boolean fixed, XSAnnotation annotation) {
            this.kind = kind;
            this.svalue = svalue;
            this.ivalue = ivalue;
            this.avalue = avalue;
            this.fixed = fixed;

            if (annotation != null) {
                this.annotations = new XSObjectListImpl();
                ((XSObjectListImpl)this.annotations).addXSObject(annotation);
            }
            else {
                this.annotations =  XSObjectListImpl.EMPTY_LIST;
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see com.sun.org.apache.xerces.internal.xs.XSFacet#getAnnotation()
         */
        /**
         * Optional. Annotation.
         */
        public XSAnnotation getAnnotation() {
            return (XSAnnotation) annotations.item(0);
        }

        /*
         * (non-Javadoc)
         *
         * @see com.sun.org.apache.xerces.internal.xs.XSFacet#getAnnotations()
         */
        /**
         * Optional. Annotations.
         */
        public XSObjectList getAnnotations() {
            return annotations;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSFacet#getFacetKind()
         */
        public short getFacetKind() {
            return kind;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSFacet#getLexicalFacetValue()
         */
        public String getLexicalFacetValue() {
            return svalue;
        }

        public Object getActualFacetValue() {
            if (avalue == null) {
                if (kind == FACET_WHITESPACE) {
                    avalue = svalue;
                }
                else {
                    avalue = BigInteger.valueOf(ivalue);
                }
            }
            return avalue;
        }

        public int getIntFacetValue() {
            return ivalue;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSFacet#isFixed()
         */
        public boolean getFixed() {
            return fixed;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSObject#getName()
         */
        public String getName() {
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSObject#getNamespace()
         */
        public String getNamespace() {
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSObject#getNamespaceItem()
         */
        public XSNamespaceItem getNamespaceItem() {
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSObject#getType()
         */
        public short getType() {
            return XSConstants.FACET;
        }

    }

    private static final class XSMVFacetImpl implements XSMultiValueFacet {
        final short kind;
        final XSObjectList annotations;
        final StringList svalues;
        final ObjectList avalues;

        public XSMVFacetImpl(short kind, StringList svalues, ObjectList avalues, XSObjectList annotations) {
            this.kind = kind;
            this.svalues = svalues;
            this.avalues = avalues;
            this.annotations = (annotations != null) ? annotations : XSObjectListImpl.EMPTY_LIST;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSFacet#getFacetKind()
         */
        public short getFacetKind() {
            return kind;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSMultiValueFacet#getAnnotations()
         */
        public XSObjectList getAnnotations() {
            return annotations;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSMultiValueFacet#getLexicalFacetValues()
         */
        public StringList getLexicalFacetValues() {
            return svalues;
        }

        public ObjectList getEnumerationValues() {
            return avalues;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSObject#getName()
         */
        public String getName() {
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSObject#getNamespace()
         */
        public String getNamespace() {
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSObject#getNamespaceItem()
         */
        public XSNamespaceItem getNamespaceItem() {
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.org.apache.xerces.internal.xs.XSObject#getType()
         */
        public short getType() {
            return XSConstants.MULTIVALUE_FACET;
        }
    }

    private static abstract class AbstractObjectList extends AbstractList<Object> implements ObjectList {
        public Object get(int index) {
            if (index >= 0 && index < getLength()) {
                return item(index);
            }
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        public int size() {
            return getLength();
        }
    }

    public String getTypeNamespace() {
        return getNamespace();
    }

    public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg, int derivationMethod) {
        return isDOMDerivedFrom(typeNamespaceArg, typeNameArg, derivationMethod);
    }

    private short convertToPrimitiveKind(short valueType) {
        /** Primitive datatypes. */
        if (valueType <= XSConstants.NOTATION_DT) {
            return valueType;
        }
        /** Types derived from string. */
        if (valueType <= XSConstants.ENTITY_DT) {
            return XSConstants.STRING_DT;
        }
        /** Types derived from decimal. */
        if (valueType <= XSConstants.POSITIVEINTEGER_DT) {
            return XSConstants.DECIMAL_DT;
        }
        /** Other types. */
        return valueType;
    }

    private void appendEnumString(StringBuffer sb) {
        sb.append('[');
        for (int i = 0; i < fEnumerationSize; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(fEnumeration[i].actualValue);
        }
        sb.append(']');
    }
} 
