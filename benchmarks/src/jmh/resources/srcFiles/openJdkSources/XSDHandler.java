/*
 * Copyright (c) 2007, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.dv.SchemaDVFactory;
import com.sun.org.apache.xerces.internal.impl.dv.xs.XSSimpleTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaNamespaceSupport;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaException;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader;
import com.sun.org.apache.xerces.internal.impl.xs.XSAttributeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSAttributeGroupDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSComplexTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSDDescription;
import com.sun.org.apache.xerces.internal.impl.xs.XSDeclarationPool;
import com.sun.org.apache.xerces.internal.impl.xs.XSElementDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSGrammarBucket;
import com.sun.org.apache.xerces.internal.impl.xs.XSGroupDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSMessageFormatter;
import com.sun.org.apache.xerces.internal.impl.xs.XSModelGroupImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSNotationDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSParticleDecl;
import com.sun.org.apache.xerces.internal.impl.xs.identity.IdentityConstraint;
import com.sun.org.apache.xerces.internal.impl.xs.opti.ElementImpl;
import com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaDOMParser;
import com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaParsingConfig;
import com.sun.org.apache.xerces.internal.impl.xs.util.SimpleLocator;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSInputSource;
import com.sun.org.apache.xerces.internal.parsers.SAXParser;
import com.sun.org.apache.xerces.internal.parsers.XML11Configuration;
import com.sun.org.apache.xerces.internal.util.DOMInputSource;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import com.sun.org.apache.xerces.internal.util.DefaultErrorHandler;
import com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper;
import com.sun.org.apache.xerces.internal.util.SAXInputSource;
import com.sun.org.apache.xerces.internal.util.StAXInputSource;
import com.sun.org.apache.xerces.internal.util.StAXLocationWrapper;
import com.sun.org.apache.xerces.internal.util.SymbolHash;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.URI.MalformedURIException;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLSchemaDescription;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import com.sun.org.apache.xerces.internal.xs.StringList;
import com.sun.org.apache.xerces.internal.xs.XSAttributeDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSAttributeGroupDefinition;
import com.sun.org.apache.xerces.internal.xs.XSAttributeUse;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSElementDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSModelGroup;
import com.sun.org.apache.xerces.internal.xs.XSModelGroupDefinition;
import com.sun.org.apache.xerces.internal.xs.XSNamedMap;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSParticle;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSTerm;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.datatypes.ObjectList;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkXmlUtils;
import jdk.xml.internal.SecuritySupport;
import jdk.xml.internal.XMLSecurityManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * The purpose of this class is to co-ordinate the construction of a
 * grammar object corresponding to a schema.  To do this, it must be
 * prepared to parse several schema documents (for instance if the
 * schema document originally referred to contains <include> or
 * <redefined> information items).  If any of the schemas imports a
 * schema, other grammars may be constructed as a side-effect.
 *
 * @xerces.internal
 *
 * @author Neil Graham, IBM
 * @author Pavani Mukthipudi, Sun Microsystems
 *
 * @LastModified: July 2023
 */
@SuppressWarnings("deprecation") 
public class XSDHandler {

    /** Feature identifier: validation. */
    protected static final String VALIDATION =
        Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

    /** feature identifier: XML Schema validation */
    protected static final String XMLSCHEMA_VALIDATION =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_VALIDATION_FEATURE;

    /** Feature identifier:  allow java encodings */
    protected static final String ALLOW_JAVA_ENCODINGS =
        Constants.XERCES_FEATURE_PREFIX + Constants.ALLOW_JAVA_ENCODINGS_FEATURE;

    /** Feature identifier:  continue after fatal error */
    protected static final String CONTINUE_AFTER_FATAL_ERROR =
        Constants.XERCES_FEATURE_PREFIX + Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE;

    /** Feature identifier:  allow java encodings */
    protected static final String STANDARD_URI_CONFORMANT_FEATURE =
        Constants.XERCES_FEATURE_PREFIX + Constants.STANDARD_URI_CONFORMANT_FEATURE;

    /** Feature: disallow doctype*/
    protected static final String DISALLOW_DOCTYPE =
        Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE;

    /** Feature: generate synthetic annotations */
    protected static final String GENERATE_SYNTHETIC_ANNOTATIONS =
        Constants.XERCES_FEATURE_PREFIX + Constants.GENERATE_SYNTHETIC_ANNOTATIONS_FEATURE;

    /** Feature identifier: validate annotations. */
    protected static final String VALIDATE_ANNOTATIONS =
        Constants.XERCES_FEATURE_PREFIX + Constants.VALIDATE_ANNOTATIONS_FEATURE;

    /** Feature identifier: honour all schemaLocations */
    protected static final String HONOUR_ALL_SCHEMALOCATIONS =
      Constants.XERCES_FEATURE_PREFIX + Constants.HONOUR_ALL_SCHEMALOCATIONS_FEATURE;

    /** Feature identifier: namespace growth */
    protected static final String NAMESPACE_GROWTH =
      Constants.XERCES_FEATURE_PREFIX + Constants.NAMESPACE_GROWTH_FEATURE;

    /** Feature identifier: tolerate duplicates */
    protected static final String TOLERATE_DUPLICATES =
      Constants.XERCES_FEATURE_PREFIX + Constants.TOLERATE_DUPLICATES_FEATURE;

    /** Feature identifier: namespace prefixes. */
    private static final String NAMESPACE_PREFIXES =
        Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACE_PREFIXES_FEATURE;

    /** Feature identifier: string interning. */
    protected static final String STRING_INTERNING =
        Constants.SAX_FEATURE_PREFIX + Constants.STRING_INTERNING_FEATURE;

    /** Property identifier: error handler. */
    protected static final String ERROR_HANDLER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_HANDLER_PROPERTY;

    /** Property identifier: JAXP schema source. */
    protected static final String JAXP_SCHEMA_SOURCE =
        Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_SOURCE;

    /** Property identifier: entity resolver. */
    public static final String ENTITY_RESOLVER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

    /** Property identifier: entity manager. */
    protected static final String ENTITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_MANAGER_PROPERTY;

    /** Property identifier: error reporter. */
    public static final String ERROR_REPORTER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Property identifier: grammar pool. */
    public static final String XMLGRAMMAR_POOL =
        Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

    /** Property identifier: symbol table. */
    public static final String SYMBOL_TABLE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property identifier: security manager. */
    protected static final String SECURITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY;

    /** Property identifier: locale. */
    protected static final String LOCALE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.LOCALE_PROPERTY;

    /** Property identifier: Security property manager. */
    private static final String XML_SECURITY_PROPERTY_MANAGER =
        JdkConstants.XML_SECURITY_PROPERTY_MANAGER;

    protected static final boolean DEBUG_NODE_POOL = false;


    final static int ATTRIBUTE_TYPE          = 1;
    final static int ATTRIBUTEGROUP_TYPE     = 2;
    final static int ELEMENT_TYPE            = 3;
    final static int GROUP_TYPE              = 4;
    final static int IDENTITYCONSTRAINT_TYPE = 5;
    final static int NOTATION_TYPE           = 6;
    final static int TYPEDECL_TYPE           = 7;

    public final static String REDEF_IDENTIFIER = "_fn3dktizrknc9pi";


    protected XSDeclarationPool fDeclPool = null;

    protected XMLSecurityManager fSecurityManager = null;

    private String fAccessExternalSchema;
    private String fAccessExternalDTD;

    private boolean registryEmpty = true;
    private Map<String, Element> fUnparsedAttributeRegistry = new HashMap<>();
    private Map<String, Element> fUnparsedAttributeGroupRegistry =  new HashMap<>();
    private Map<String, Element> fUnparsedElementRegistry =  new HashMap<>();
    private Map<String, Element> fUnparsedGroupRegistry =  new HashMap<>();
    private Map<String, Element> fUnparsedIdentityConstraintRegistry =  new HashMap<>();
    private Map<String, Element> fUnparsedNotationRegistry =  new HashMap<>();
    private Map<String, Element> fUnparsedTypeRegistry =  new HashMap<>();
    private Map<String, XSDocumentInfo> fUnparsedAttributeRegistrySub =  new HashMap<>();
    private Map<String, XSDocumentInfo> fUnparsedAttributeGroupRegistrySub =  new HashMap<>();
    private Map<String, XSDocumentInfo> fUnparsedElementRegistrySub =  new HashMap<>();
    private Map<String, XSDocumentInfo> fUnparsedGroupRegistrySub =  new HashMap<>();
    private Map<String, XSDocumentInfo> fUnparsedIdentityConstraintRegistrySub =  new HashMap<>();
    private Map<String, XSDocumentInfo> fUnparsedNotationRegistrySub =  new HashMap<>();
    private Map<String, XSDocumentInfo> fUnparsedTypeRegistrySub =  new HashMap<>();

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map<String, XSDocumentInfo> fUnparsedRegistriesExt[] = new HashMap[] {
        null,
        null, 
        null, 
        null, 
        null, 
        null, 
        null, 
        null, 
    };

    private Map<XSDocumentInfo, List<XSDocumentInfo>> fDependencyMap = new HashMap<>();

    private Map<String, List<String>> fImportMap = new HashMap<> ();

    private List<String> fAllTNSs = new ArrayList<>();

    private Map<String, XMLSchemaLoader.LocationArray> fLocationPairs = null;

    Map<Node, String> fHiddenNodes = null;

    private String null2EmptyString(String ns) {
        return ns == null ? XMLSymbols.EMPTY_STRING : ns;
    }
    private String emptyString2Null(String ns) {
        return ns == XMLSymbols.EMPTY_STRING ? null : ns;
    }
    private String doc2SystemId(Element ele) {
        String documentURI = null;
        /**
         * REVISIT: Casting until DOM Level 3 interfaces are available. -- mrglavas
         */
        if(ele.getOwnerDocument() instanceof com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaDOM){
            documentURI = ((com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaDOM) ele.getOwnerDocument()).getDocumentURI();
        }
        return documentURI != null ? documentURI : fDoc2SystemId.get(ele);
    }

    private Map<XSDKey, Element> fTraversed = new HashMap<>();

    private Map<Element, String> fDoc2SystemId = new HashMap<>();

    private XSDocumentInfo fRoot = null;

    private Map<Element, XSDocumentInfo> fDoc2XSDocumentMap = new HashMap<>();

    private Map<Element, XSDocumentInfo> fRedefine2XSDMap = null;

    private Map<Element, SchemaNamespaceSupport> fRedefine2NSSupport = null;

    private final Map<String, String> fRedefinedRestrictedAttributeGroupRegistry = new HashMap<>();
    private final Map<String, String> fRedefinedRestrictedGroupRegistry = new HashMap<>();

    private boolean fLastSchemaWasDuplicate;

    private boolean fValidateAnnotations = false;

    private boolean fHonourAllSchemaLocations = false;

    boolean fNamespaceGrowth = false;

    boolean fTolerateDuplicates = false;

    private XMLErrorReporter fErrorReporter;

    private XMLErrorHandler fErrorHandler;

    private Locale fLocale;

    private XMLEntityManager fEntityManager;

    private XSAttributeChecker fAttributeChecker;

    private SymbolTable fSymbolTable;

    private XSGrammarBucket fGrammarBucket;

    private XSDDescription fSchemaGrammarDescription;

    private XMLGrammarPool fGrammarPool;

    private XMLSecurityPropertyManager fSecurityPropertyMgr = null;

    /** indicate whether Catalog should be used for resolving external resources */
    private boolean fUseCatalog = true;
    private String fCatalogFile;
    private String fDefer;
    private String fPrefer;
    private String fResolve;

    private boolean fOverrideDefaultParser;

    XSDAttributeGroupTraverser fAttributeGroupTraverser;
    XSDAttributeTraverser fAttributeTraverser;
    XSDComplexTypeTraverser fComplexTypeTraverser;
    XSDElementTraverser fElementTraverser;
    XSDGroupTraverser fGroupTraverser;
    XSDKeyrefTraverser fKeyrefTraverser;
    XSDNotationTraverser fNotationTraverser;
    XSDSimpleTypeTraverser fSimpleTypeTraverser;
    XSDUniqueOrKeyTraverser fUniqueOrKeyTraverser;
    XSDWildcardTraverser fWildCardTraverser;

    SchemaDVFactory fDVFactory;
    SchemaDOMParser fSchemaParser;
    SchemaContentHandler fXSContentHandler;
    StAXSchemaParser fStAXSchemaParser;
    XML11Configuration fAnnotationValidator;
    XSAnnotationGrammarPool fGrammarBucketAdapter;


    private static final int INIT_STACK_SIZE = 30;
    private static final int INC_STACK_SIZE  = 10;
    private int fLocalElemStackPos = 0;

    private XSParticleDecl[] fParticle = new XSParticleDecl[INIT_STACK_SIZE];
    private Element[] fLocalElementDecl = new Element[INIT_STACK_SIZE];
    private XSDocumentInfo[] fLocalElementDecl_schema = new XSDocumentInfo[INIT_STACK_SIZE]; 
    private int[] fAllContext = new int[INIT_STACK_SIZE];
    private XSObject[] fParent = new XSObject[INIT_STACK_SIZE];
    private String [][] fLocalElemNamespaceContext = new String [INIT_STACK_SIZE][1];


    private static final int INIT_KEYREF_STACK = 2;
    private static final int INC_KEYREF_STACK_AMOUNT = 2;
    private int fKeyrefStackPos = 0;

    private Element [] fKeyrefs = new Element[INIT_KEYREF_STACK];
    private XSDocumentInfo [] fKeyrefsMapXSDocumentInfo = new XSDocumentInfo[INIT_KEYREF_STACK];
    private XSElementDecl [] fKeyrefElems = new XSElementDecl [INIT_KEYREF_STACK];
    private String [][] fKeyrefNamespaceContext = new String[INIT_KEYREF_STACK][1];

    SymbolHash fGlobalAttrDecls = new SymbolHash(12);
    SymbolHash fGlobalAttrGrpDecls = new SymbolHash(5);
    SymbolHash fGlobalElemDecls = new SymbolHash(25);
    SymbolHash fGlobalGroupDecls = new SymbolHash(5);
    SymbolHash fGlobalNotationDecls = new SymbolHash(1);
    SymbolHash fGlobalIDConstraintDecls = new SymbolHash(3);
    SymbolHash fGlobalTypeDecls = new SymbolHash(25);

    public XSDHandler(){
        fHiddenNodes = new HashMap<>();
        fSchemaParser = new SchemaDOMParser(new SchemaParsingConfig());
    }

    public XSDHandler (XSGrammarBucket gBucket) {
        this();
        fGrammarBucket = gBucket;

        fSchemaGrammarDescription = new XSDDescription();
    } 

    /**
     * This method initiates the parse of a schema.  It will likely be
     * called from the Validator and it will make the
     * resulting grammar available; it returns a reference to this object just
     * in case.  A reset(XMLComponentManager) must be called before this methods is called.
     * @param is
     * @param desc
     * @param locationPairs
     * @return the SchemaGrammar
     * @throws IOException
     */
    public SchemaGrammar parseSchema(XMLInputSource is, XSDDescription desc,
            Map<String, XMLSchemaLoader.LocationArray> locationPairs)
    throws IOException {
        fLocationPairs = locationPairs;
        fSchemaParser.resetNodePool();
        SchemaGrammar grammar = null;
        String schemaNamespace  = null;
        short referType = desc.getContextType();

        if (referType != XSDDescription.CONTEXT_PREPARSE){
            if (fHonourAllSchemaLocations && referType == XSDDescription.CONTEXT_IMPORT && isExistingGrammar(desc, fNamespaceGrowth)) {
                grammar = fGrammarBucket.getGrammar(desc.getTargetNamespace());
            }
            else {
                grammar = findGrammar(desc, fNamespaceGrowth);
            }
            if (grammar != null) {
                if (!fNamespaceGrowth) {
                    return grammar;
                }
                else {
                    try {
                        if (grammar.getDocumentLocations().contains(XMLEntityManager.expandSystemId(is.getSystemId(), is.getBaseSystemId(), false))) {
                            return grammar;
                        }
                    }
                    catch (MalformedURIException e) {
                    }
                }
            }

            schemaNamespace = desc.getTargetNamespace();
            if (schemaNamespace != null) {
                schemaNamespace = fSymbolTable.addSymbol(schemaNamespace);
            }
        }

        prepareForParse();

        Element schemaRoot = null;
        if (is instanceof DOMInputSource) {
            schemaRoot = getSchemaDocument(schemaNamespace, (DOMInputSource) is,
                    referType == XSDDescription.CONTEXT_PREPARSE,
                    referType, null);
        } 
        else if (is instanceof SAXInputSource) {
                schemaRoot = getSchemaDocument(schemaNamespace, (SAXInputSource) is,
                    referType == XSDDescription.CONTEXT_PREPARSE,
                    referType, null);
        } 
        else if (is instanceof StAXInputSource) {
            schemaRoot = getSchemaDocument(schemaNamespace, (StAXInputSource) is,
                    referType == XSDDescription.CONTEXT_PREPARSE,
                    referType, null);
        } 
        else if (is instanceof XSInputSource) {
            schemaRoot = getSchemaDocument((XSInputSource) is, desc);
        } 
        else {
                schemaRoot = getSchemaDocument(schemaNamespace, is,
                  referType == XSDDescription.CONTEXT_PREPARSE,
                  referType, null);

        } 

        if (schemaRoot == null) {
            if (is instanceof XSInputSource) {
                XSInputSource xsinput = (XSInputSource)is;
                SchemaGrammar[] grammars = xsinput.getGrammars();
                if (grammars != null && grammars.length > 0) {
                    grammar = fGrammarBucket.getGrammar(grammars[0].getTargetNamespace());
                }
                else {
                    XSObject[] components = xsinput.getComponents();
                    if (components != null && components.length > 0) {
                        grammar = fGrammarBucket.getGrammar(components[0].getNamespace());
                    }
                }
            }
            return grammar;
        }

        if (referType == XSDDescription.CONTEXT_PREPARSE) {
                Element schemaElem = schemaRoot;
            schemaNamespace = DOMUtil.getAttrValue(schemaElem, SchemaSymbols.ATT_TARGETNAMESPACE);
            if(schemaNamespace != null && schemaNamespace.length() > 0) {
                schemaNamespace = fSymbolTable.addSymbol(schemaNamespace);
                desc.setTargetNamespace(schemaNamespace);
            }
            else {
                schemaNamespace = null;
            }
            grammar = findGrammar(desc, fNamespaceGrowth);
            String schemaId = XMLEntityManager.expandSystemId(is.getSystemId(), is.getBaseSystemId(), false);
            if (grammar != null) {
                if (!fNamespaceGrowth || (schemaId != null && grammar.getDocumentLocations().contains(schemaId))) {
                    return grammar;
                }
            }

            XSDKey key = new XSDKey(schemaId, referType, schemaNamespace);
            fTraversed.put(key, schemaRoot);
            if (schemaId != null) {
                fDoc2SystemId.put(schemaRoot, schemaId);
            }
        }

        prepareForTraverse();

        fRoot = constructTrees(schemaRoot, is.getSystemId(), desc, grammar != null);
        if (fRoot == null) {
            return null;
        }

        buildGlobalNameRegistries();

        List<Object> annotationInfo = fValidateAnnotations ? new ArrayList<>() : null;
        traverseSchemas(annotationInfo);

        traverseLocalElements();

        resolveKeyRefs();


        for (int i = fAllTNSs.size() - 1; i >= 0; i--) {
            String tns = fAllTNSs.get(i);
            List<String> ins = fImportMap.get(tns);
            List<SchemaGrammar> grammars = new ArrayList<>();
            SchemaGrammar sg = fGrammarBucket.getGrammar(emptyString2Null(tns));
            if (sg == null)
                continue;
            SchemaGrammar isg;
            int count = 0;
            for (int j = 0; j < ins.size(); j++) {
                isg = fGrammarBucket.getGrammar(ins.get(j));
                if (isg != null) {
                    grammars.add(isg);
                }
            }
            sg.setImportedGrammars(grammars);
        }

        /** validate annotations **/
        if (fValidateAnnotations && annotationInfo.size() > 0) {
            validateAnnotations(annotationInfo);
        }

        return fGrammarBucket.getGrammar(fRoot.fTargetNamespace);
    } 

    private void validateAnnotations(List<Object> annotationInfo) {
        if (fAnnotationValidator == null) {
            createAnnotationValidator();
        }
        final int size = annotationInfo.size();
        final XMLInputSource src = new XMLInputSource(null, null, null, false);
        fGrammarBucketAdapter.refreshGrammars(fGrammarBucket);
        for (int i = 0; i < size; i += 2) {
            src.setSystemId((String) annotationInfo.get(i));
            XSAnnotationInfo annotation = (XSAnnotationInfo) annotationInfo.get(i+1);
            while (annotation != null) {
                src.setCharacterStream(new StringReader(annotation.fAnnotation));
                try {
                    fAnnotationValidator.parse(src);
                }
                catch (IOException exc) {}
                annotation = annotation.next;
            }
        }
    }

    private void createAnnotationValidator() {
        fAnnotationValidator = new XML11Configuration();
        fGrammarBucketAdapter = new XSAnnotationGrammarPool();
        fAnnotationValidator.setFeature(VALIDATION, true);
        fAnnotationValidator.setFeature(XMLSCHEMA_VALIDATION, true);
        fAnnotationValidator.setProperty(XMLGRAMMAR_POOL, fGrammarBucketAdapter);
        /** set security manager and XML Security Property Manager **/
        fAnnotationValidator.setProperty(SECURITY_MANAGER, (fSecurityManager != null) ? fSecurityManager : new XMLSecurityManager(true));
        fAnnotationValidator.setProperty(XML_SECURITY_PROPERTY_MANAGER, fSecurityPropertyMgr);
        /** Set error handler. **/
        fAnnotationValidator.setProperty(ERROR_HANDLER, (fErrorHandler != null) ? fErrorHandler : new DefaultErrorHandler());
        /** Set locale. **/
        fAnnotationValidator.setProperty(LOCALE, fLocale);

        fAnnotationValidator.setFeature(XMLConstants.USE_CATALOG, fUseCatalog);
        fAnnotationValidator.setProperty(JdkXmlUtils.CATALOG_FILES, fCatalogFile);
        fAnnotationValidator.setProperty(JdkXmlUtils.CATALOG_DEFER, fDefer);
        fAnnotationValidator.setProperty(JdkXmlUtils.CATALOG_PREFER, fPrefer);
        fAnnotationValidator.setProperty(JdkXmlUtils.CATALOG_RESOLVE, fResolve);
    }

    /**
     * Pull the grammar out of the bucket simply using
     * its TNS as a key
     */
    SchemaGrammar getGrammar(String tns) {
        return fGrammarBucket.getGrammar(tns);
    }

    /**
     * First try to find a grammar in the bucket, if failed, consult the
     * grammar pool. If a grammar is found in the pool, then add it (and all
     * imported ones) into the bucket.
     */
    protected SchemaGrammar findGrammar(XSDDescription desc, boolean ignoreConflict) {
        SchemaGrammar sg = fGrammarBucket.getGrammar(desc.getTargetNamespace());
        if (sg == null) {
            if (fGrammarPool != null) {
                sg = (SchemaGrammar)fGrammarPool.retrieveGrammar(desc);
                if (sg != null) {
                    if (!fGrammarBucket.putGrammar(sg, true, ignoreConflict)) {
                        reportSchemaWarning("GrammarConflict", null, null);
                        sg = null;
                    }
                }
            }
        }
        return sg;
    }


    private static final String[][] NS_ERROR_CODES = {
            {"src-include.2.1", "src-include.2.1"},
            {"src-redefine.3.1", "src-redefine.3.1"},
            {"src-import.3.1", "src-import.3.2"},
            null,
            {"TargetNamespace.1", "TargetNamespace.2"},
            {"TargetNamespace.1", "TargetNamespace.2"},
            {"TargetNamespace.1", "TargetNamespace.2"},
            {"TargetNamespace.1", "TargetNamespace.2"}
    };

    private static final String[] ELE_ERROR_CODES = {
            "src-include.1", "src-redefine.2", "src-import.2", "schema_reference.4",
            "schema_reference.4", "schema_reference.4", "schema_reference.4", "schema_reference.4"
    };

    protected XSDocumentInfo constructTrees(Element schemaRoot, String locationHint, XSDDescription desc, boolean nsCollision) {
        if (schemaRoot == null) return null;
        String callerTNS = desc.getTargetNamespace();
        short referType = desc.getContextType();

        XSDocumentInfo currSchemaInfo = null;
        try {
            currSchemaInfo = new XSDocumentInfo(schemaRoot, fAttributeChecker, fSymbolTable);
        } catch (XMLSchemaException se) {
            reportSchemaError(ELE_ERROR_CODES[referType],
                    new Object[]{locationHint},
                                          schemaRoot);
            return null;
        }
        if (currSchemaInfo.fTargetNamespace != null &&
                currSchemaInfo.fTargetNamespace.length() == 0) {
            reportSchemaWarning("EmptyTargetNamespace",
                    new Object[]{locationHint},
                                        schemaRoot);
            currSchemaInfo.fTargetNamespace = null;
        }

        if (callerTNS != null) {
            int secondIdx = 0;
            if (referType == XSDDescription.CONTEXT_INCLUDE ||
                    referType == XSDDescription.CONTEXT_REDEFINE) {
                if (currSchemaInfo.fTargetNamespace == null) {
                    currSchemaInfo.fTargetNamespace = callerTNS;
                    currSchemaInfo.fIsChameleonSchema = true;
                }
                else if (callerTNS != currSchemaInfo.fTargetNamespace) {
                    reportSchemaError(NS_ERROR_CODES[referType][secondIdx],
                            new Object [] {callerTNS, currSchemaInfo.fTargetNamespace},
                                                        schemaRoot);
                    return null;
                }
            }
            else if (referType != XSDDescription.CONTEXT_PREPARSE && callerTNS != currSchemaInfo.fTargetNamespace) {
                reportSchemaError(NS_ERROR_CODES[referType][secondIdx],
                        new Object [] {callerTNS, currSchemaInfo.fTargetNamespace},
                                                schemaRoot);
                return null;
            }
        }
        else if (currSchemaInfo.fTargetNamespace != null) {
            if (referType == XSDDescription.CONTEXT_PREPARSE) {
                desc.setTargetNamespace(currSchemaInfo.fTargetNamespace);
                callerTNS = currSchemaInfo.fTargetNamespace;
            }
            else {
                int secondIdx = 1;
                reportSchemaError(NS_ERROR_CODES[referType][secondIdx],
                        new Object [] {callerTNS, currSchemaInfo.fTargetNamespace},
                                                schemaRoot);
                return null;
            }
        }

        currSchemaInfo.addAllowedNS(currSchemaInfo.fTargetNamespace);

        SchemaGrammar sg = null;

        if (nsCollision) {
            SchemaGrammar sg2 = fGrammarBucket.getGrammar(currSchemaInfo.fTargetNamespace);
            if (sg2.isImmutable()) {
                sg = new SchemaGrammar(sg2);
                fGrammarBucket.putGrammar(sg);
                updateImportListWith(sg);
            }
            else {
                sg = sg2;
            }

            updateImportListFor(sg);
        }
        else if (referType == XSDDescription.CONTEXT_INCLUDE ||
                referType == XSDDescription.CONTEXT_REDEFINE) {
            sg = fGrammarBucket.getGrammar(currSchemaInfo.fTargetNamespace);
        }
        else if(fHonourAllSchemaLocations && referType == XSDDescription.CONTEXT_IMPORT) {
            sg = findGrammar(desc, false);
            if(sg == null) {
                sg = new SchemaGrammar(currSchemaInfo.fTargetNamespace, desc.makeClone(), fSymbolTable);
                fGrammarBucket.putGrammar(sg);
            }
        }
        else {
            sg = new SchemaGrammar(currSchemaInfo.fTargetNamespace, desc.makeClone(), fSymbolTable);
            fGrammarBucket.putGrammar(sg);
        }

        sg.addDocument(null, fDoc2SystemId.get(currSchemaInfo.fSchemaElement));

        fDoc2XSDocumentMap.put(schemaRoot, currSchemaInfo);
        List<XSDocumentInfo> dependencies = new ArrayList<>();
        Element rootNode = schemaRoot;

        Element newSchemaRoot = null;
        for (Element child = DOMUtil.getFirstChildElement(rootNode);
        child != null;
        child = DOMUtil.getNextSiblingElement(child)) {
            String schemaNamespace=null;
            String schemaHint=null;
            String localName = DOMUtil.getLocalName(child);

            short refType = -1;
            boolean importCollision = false;

            if (localName.equals(SchemaSymbols.ELT_ANNOTATION))
                continue;
            else if (localName.equals(SchemaSymbols.ELT_IMPORT)) {
                refType = XSDDescription.CONTEXT_IMPORT;
                Object[] importAttrs = fAttributeChecker.checkAttributes(child, true, currSchemaInfo);
                schemaHint = (String)importAttrs[XSAttributeChecker.ATTIDX_SCHEMALOCATION];
                schemaNamespace = (String)importAttrs[XSAttributeChecker.ATTIDX_NAMESPACE];
                if (schemaNamespace != null)
                    schemaNamespace = fSymbolTable.addSymbol(schemaNamespace);

                Element importChild = DOMUtil.getFirstChildElement(child);
                if(importChild != null ) {
                    String importComponentType = DOMUtil.getLocalName(importChild);
                    if (importComponentType.equals(SchemaSymbols.ELT_ANNOTATION)) {
                        sg.addAnnotation(
                                fElementTraverser.traverseAnnotationDecl(importChild, importAttrs, true, currSchemaInfo));
                    } else {
                        reportSchemaError("s4s-elt-must-match.1", new Object [] {localName, "annotation?", importComponentType}, child);
                    }
                    if(DOMUtil.getNextSiblingElement(importChild) != null) {
                        reportSchemaError("s4s-elt-must-match.1", new Object [] {localName, "annotation?", DOMUtil.getLocalName(DOMUtil.getNextSiblingElement(importChild))}, child);
                    }
                }
                else {
                    String text = DOMUtil.getSyntheticAnnotation(child);
                    if (text != null) {
                        sg.addAnnotation(fElementTraverser.traverseSyntheticAnnotation(child, text, importAttrs, true, currSchemaInfo));
                    }
                }
                fAttributeChecker.returnAttrArray(importAttrs, currSchemaInfo);

                if (schemaNamespace == currSchemaInfo.fTargetNamespace) {
                    reportSchemaError(schemaNamespace != null ?
                            "src-import.1.1" : "src-import.1.2", new Object [] {schemaNamespace}, child);
                    continue;
                }

                if(currSchemaInfo.isAllowedNS(schemaNamespace)) {
                    if(!fHonourAllSchemaLocations && !fNamespaceGrowth)
                        continue;
                }
                else  {
                    currSchemaInfo.addAllowedNS(schemaNamespace);
                }
                String tns = null2EmptyString(currSchemaInfo.fTargetNamespace);
               List<String> ins = fImportMap.get(tns);
                if (ins == null) {
                    fAllTNSs.add(tns);
                    ins = new ArrayList<>();
                    fImportMap.put(tns, ins);
                    ins.add(schemaNamespace);
                }
                else if (!ins.contains(schemaNamespace)){
                    ins.add(schemaNamespace);
                }

                fSchemaGrammarDescription.reset();
                fSchemaGrammarDescription.setContextType(XSDDescription.CONTEXT_IMPORT);
                fSchemaGrammarDescription.setBaseSystemId(doc2SystemId(schemaRoot));
                fSchemaGrammarDescription.setLiteralSystemId(schemaHint);
                fSchemaGrammarDescription.setLocationHints(new String[]{schemaHint});
                fSchemaGrammarDescription.setTargetNamespace(schemaNamespace);

                SchemaGrammar isg = findGrammar(fSchemaGrammarDescription, fNamespaceGrowth);
                if (isg != null) {
                    if (fNamespaceGrowth) {
                        try {
                            if (isg.getDocumentLocations().contains(XMLEntityManager.expandSystemId(schemaHint, fSchemaGrammarDescription.getBaseSystemId(), false))) {
                                continue;
                            }
                            else {
                                importCollision = true;
                            }
                        }
                        catch (MalformedURIException e) {
                        }
                    }
                    else if (!fHonourAllSchemaLocations || isExistingGrammar(fSchemaGrammarDescription, false)) {
                        continue;
                    }
                }

                newSchemaRoot = resolveSchema(fSchemaGrammarDescription, false, child, isg == null);
            }
            else if ((localName.equals(SchemaSymbols.ELT_INCLUDE)) ||
                    (localName.equals(SchemaSymbols.ELT_REDEFINE))) {
                Object[] includeAttrs = fAttributeChecker.checkAttributes(child, true, currSchemaInfo);
                schemaHint = (String)includeAttrs[XSAttributeChecker.ATTIDX_SCHEMALOCATION];
                if (localName.equals(SchemaSymbols.ELT_REDEFINE)) {
                    if (fRedefine2NSSupport == null) fRedefine2NSSupport = new HashMap<>();
                    fRedefine2NSSupport.put(child, new SchemaNamespaceSupport(currSchemaInfo.fNamespaceSupport));
                }

                if(localName.equals(SchemaSymbols.ELT_INCLUDE)) {
                    Element includeChild = DOMUtil.getFirstChildElement(child);
                    if(includeChild != null ) {
                        String includeComponentType = DOMUtil.getLocalName(includeChild);
                        if (includeComponentType.equals(SchemaSymbols.ELT_ANNOTATION)) {
                            sg.addAnnotation(
                                    fElementTraverser.traverseAnnotationDecl(includeChild, includeAttrs, true, currSchemaInfo));
                        } else {
                            reportSchemaError("s4s-elt-must-match.1",
                                    new Object [] {localName, "annotation?", includeComponentType}, child);
                        }
                        if(DOMUtil.getNextSiblingElement(includeChild) != null) {
                            reportSchemaError("s4s-elt-must-match.1",
                                    new Object [] {localName, "annotation?",
                                        DOMUtil.getLocalName(DOMUtil.getNextSiblingElement(includeChild))}, child);
                        }
                    }
                    else {
                        String text = DOMUtil.getSyntheticAnnotation(child);
                        if (text != null) {
                            sg.addAnnotation(fElementTraverser.traverseSyntheticAnnotation(child, text, includeAttrs, true, currSchemaInfo));
                        }
                    }
                }
                else {
                    for (Element redefinedChild = DOMUtil.getFirstChildElement(child);
                    redefinedChild != null;
                    redefinedChild = DOMUtil.getNextSiblingElement(redefinedChild)) {
                        String redefinedComponentType = DOMUtil.getLocalName(redefinedChild);
                        if (redefinedComponentType.equals(SchemaSymbols.ELT_ANNOTATION)) {
                            sg.addAnnotation(
                                    fElementTraverser.traverseAnnotationDecl(redefinedChild, includeAttrs, true, currSchemaInfo));
                            DOMUtil.setHidden(redefinedChild, fHiddenNodes);
                        }
                        else {
                            String text = DOMUtil.getSyntheticAnnotation(child);
                            if (text != null) {
                                sg.addAnnotation(fElementTraverser.traverseSyntheticAnnotation(child, text, includeAttrs, true, currSchemaInfo));
                            }
                        }
                    }
                }
                fAttributeChecker.returnAttrArray(includeAttrs, currSchemaInfo);
                if (schemaHint == null) {
                    reportSchemaError("s4s-att-must-appear", new Object [] {
                            "<include> or <redefine>", "schemaLocation"},
                            child);
                }
                boolean mustResolve = false;
                refType = XSDDescription.CONTEXT_INCLUDE;
                if(localName.equals(SchemaSymbols.ELT_REDEFINE)) {
                    mustResolve = nonAnnotationContent(child);
                    refType = XSDDescription.CONTEXT_REDEFINE;
                }
                fSchemaGrammarDescription.reset();
                fSchemaGrammarDescription.setContextType(refType);
                fSchemaGrammarDescription.setBaseSystemId(doc2SystemId(schemaRoot));
                fSchemaGrammarDescription.setLocationHints(new String[]{schemaHint});
                fSchemaGrammarDescription.setTargetNamespace(callerTNS);

                boolean alreadyTraversed = false;
                XMLInputSource schemaSource =
                        resolveSchemaSource(fSchemaGrammarDescription, mustResolve, child, true);
                if (fNamespaceGrowth && refType == XSDDescription.CONTEXT_INCLUDE) {
                    try {
                        final String schemaId = XMLEntityManager.expandSystemId(
                                schemaSource.getSystemId(), schemaSource.getBaseSystemId(), false);
                        alreadyTraversed = sg.getDocumentLocations().contains(schemaId);
                    }
                    catch(MalformedURIException e) {

                    }
                }

                if (!alreadyTraversed) {
                    newSchemaRoot = resolveSchema(schemaSource, fSchemaGrammarDescription, mustResolve, child);
                    schemaNamespace = currSchemaInfo.fTargetNamespace;
                }
                else {
                    fLastSchemaWasDuplicate = true;
                }
            }
            else {
                break;
            }

            XSDocumentInfo newSchemaInfo = null;
            if (fLastSchemaWasDuplicate) {
                newSchemaInfo = newSchemaRoot == null ? null : fDoc2XSDocumentMap.get(newSchemaRoot);
            }
            else {
                newSchemaInfo = constructTrees(newSchemaRoot, schemaHint,
                        fSchemaGrammarDescription, importCollision);
            }

            if (localName.equals(SchemaSymbols.ELT_REDEFINE) &&
                    newSchemaInfo != null) {
                if (fRedefine2XSDMap == null) fRedefine2XSDMap = new HashMap<>();
                fRedefine2XSDMap.put(child, newSchemaInfo);
            }
            if (newSchemaRoot != null) {
                if (newSchemaInfo != null)
                    dependencies.add(newSchemaInfo);
                newSchemaRoot = null;
            }
        }

        fDependencyMap.put(currSchemaInfo, dependencies);
        return currSchemaInfo;
    } 

    private boolean isExistingGrammar(XSDDescription desc, boolean ignoreConflict) {
        SchemaGrammar sg = fGrammarBucket.getGrammar(desc.getTargetNamespace());
        if (sg == null) {
            return findGrammar(desc, ignoreConflict) != null;
        }
        else if (sg.isImmutable()) {
            return true;
        }
        else {
            try {
                return sg.getDocumentLocations().contains(XMLEntityManager.expandSystemId(desc.getLiteralSystemId(), desc.getBaseSystemId(), false));
            }
            catch (MalformedURIException e) {
                return false;
            }
        }
    }

    /**
     * Namespace growth
     *
     * Go through the import list of a given grammar and for each imported
     * grammar, check to see if the grammar bucket has a newer version.
     * If a new instance is found, we update the import list with the
     * newer version.
     */
    private void updateImportListFor(SchemaGrammar grammar) {
       List<SchemaGrammar> importedGrammars = grammar.getImportedGrammars();
        if (importedGrammars != null) {
            for (int i=0; i<importedGrammars.size(); i++) {
                SchemaGrammar isg1 = importedGrammars.get(i);
                SchemaGrammar isg2 = fGrammarBucket.getGrammar(isg1.getTargetNamespace());
                if (isg2 != null && isg1 != isg2) {
                    importedGrammars.set(i, isg2);
                }
            }
        }
    }

    /**
     * Namespace growth
     *
     * Go throuth the grammar bucket, and for each grammar in the bucket
     * check the import list. If there exists a grammar in import list
     * that has the same namespace as newGrammar, but a different instance,
     * then update the import list and replace the old grammar instance with
     * the new one
     */
    private void updateImportListWith(SchemaGrammar newGrammar) {
        SchemaGrammar[] schemaGrammars = fGrammarBucket.getGrammars();
        for (int i = 0; i < schemaGrammars.length; ++i) {
            SchemaGrammar sg = schemaGrammars[i];
            if (sg != newGrammar) {
                List<SchemaGrammar> importedGrammars = sg.getImportedGrammars();
                if (importedGrammars != null) {
                    for (int j=0; j<importedGrammars.size(); j++) {
                        SchemaGrammar isg = importedGrammars.get(j);
                        if (null2EmptyString(isg.getTargetNamespace()).equals(
                                null2EmptyString(newGrammar.getTargetNamespace()))) {
                            if (isg != newGrammar) {
                                importedGrammars.set(j, newGrammar);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    protected void buildGlobalNameRegistries() {

        registryEmpty = false;

        Stack<XSDocumentInfo> schemasToProcess = new Stack<>();
        schemasToProcess.push(fRoot);

        while (!schemasToProcess.empty()) {
            XSDocumentInfo currSchemaDoc = schemasToProcess.pop();
            Element currDoc = currSchemaDoc.fSchemaElement;
            if(DOMUtil.isHidden(currDoc, fHiddenNodes)){
                continue;
            }

            Element currRoot = currDoc;
            boolean dependenciesCanOccur = true;
            for (Element globalComp =
                DOMUtil.getFirstChildElement(currRoot);
            globalComp != null;
            globalComp = DOMUtil.getNextSiblingElement(globalComp)) {
                if (DOMUtil.getLocalName(globalComp).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    continue;
                }
                else if (DOMUtil.getLocalName(globalComp).equals(SchemaSymbols.ELT_INCLUDE) ||
                        DOMUtil.getLocalName(globalComp).equals(SchemaSymbols.ELT_IMPORT)) {
                    if (!dependenciesCanOccur) {
                        reportSchemaError("s4s-elt-invalid-content.3", new Object [] {DOMUtil.getLocalName(globalComp)}, globalComp);
                    }
                    DOMUtil.setHidden(globalComp, fHiddenNodes);
                }
                else if (DOMUtil.getLocalName(globalComp).equals(SchemaSymbols.ELT_REDEFINE)) {
                    if (!dependenciesCanOccur) {
                        reportSchemaError("s4s-elt-invalid-content.3", new Object [] {DOMUtil.getLocalName(globalComp)}, globalComp);
                    }
                    for (Element redefineComp = DOMUtil.getFirstChildElement(globalComp);
                    redefineComp != null;
                    redefineComp = DOMUtil.getNextSiblingElement(redefineComp)) {
                        String lName = DOMUtil.getAttrValue(redefineComp, SchemaSymbols.ATT_NAME);
                        if (lName.length() == 0) 
                            continue;
                        String qName = currSchemaDoc.fTargetNamespace == null ?
                                ","+lName:
                                    currSchemaDoc.fTargetNamespace +","+lName;
                        qName = XMLChar.trim(qName);
                        String componentType = DOMUtil.getLocalName(redefineComp);
                        if (componentType.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
                            checkForDuplicateNames(qName, ATTRIBUTEGROUP_TYPE, fUnparsedAttributeGroupRegistry, fUnparsedAttributeGroupRegistrySub, redefineComp, currSchemaDoc);
                            String targetLName = DOMUtil.getAttrValue(redefineComp, SchemaSymbols.ATT_NAME)+REDEF_IDENTIFIER;
                            renameRedefiningComponents(currSchemaDoc, redefineComp, SchemaSymbols.ELT_ATTRIBUTEGROUP,
                                    lName, targetLName);
                        }
                        else if ((componentType.equals(SchemaSymbols.ELT_COMPLEXTYPE)) ||
                                (componentType.equals(SchemaSymbols.ELT_SIMPLETYPE))) {
                            checkForDuplicateNames(qName, TYPEDECL_TYPE, fUnparsedTypeRegistry, fUnparsedTypeRegistrySub, redefineComp, currSchemaDoc);
                            String targetLName = DOMUtil.getAttrValue(redefineComp, SchemaSymbols.ATT_NAME) + REDEF_IDENTIFIER;
                            if (componentType.equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
                                renameRedefiningComponents(currSchemaDoc, redefineComp, SchemaSymbols.ELT_COMPLEXTYPE,
                                        lName, targetLName);
                            }
                            else { 
                                renameRedefiningComponents(currSchemaDoc, redefineComp, SchemaSymbols.ELT_SIMPLETYPE,
                                        lName, targetLName);
                            }
                        }
                        else if (componentType.equals(SchemaSymbols.ELT_GROUP)) {
                            checkForDuplicateNames(qName, GROUP_TYPE, fUnparsedGroupRegistry, fUnparsedGroupRegistrySub, redefineComp, currSchemaDoc);
                            String targetLName = DOMUtil.getAttrValue(redefineComp, SchemaSymbols.ATT_NAME)+REDEF_IDENTIFIER;
                            renameRedefiningComponents(currSchemaDoc, redefineComp, SchemaSymbols.ELT_GROUP,
                                    lName, targetLName);
                        }
                    } 
                }
                else {
                    dependenciesCanOccur = false;
                    String lName = DOMUtil.getAttrValue(globalComp, SchemaSymbols.ATT_NAME);
                    if (lName.length() == 0) 
                        continue;
                    String qName = currSchemaDoc.fTargetNamespace == null?
                            ","+lName:
                                currSchemaDoc.fTargetNamespace +","+lName;
                    qName = XMLChar.trim(qName);
                    String componentType = DOMUtil.getLocalName(globalComp);

                    if (componentType.equals(SchemaSymbols.ELT_ATTRIBUTE)) {
                        checkForDuplicateNames(qName, ATTRIBUTE_TYPE, fUnparsedAttributeRegistry, fUnparsedAttributeRegistrySub, globalComp, currSchemaDoc);
                    }
                    else if (componentType.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
                        checkForDuplicateNames(qName, ATTRIBUTEGROUP_TYPE, fUnparsedAttributeGroupRegistry, fUnparsedAttributeGroupRegistrySub, globalComp, currSchemaDoc);
                    }
                    else if ((componentType.equals(SchemaSymbols.ELT_COMPLEXTYPE)) ||
                            (componentType.equals(SchemaSymbols.ELT_SIMPLETYPE))) {
                        checkForDuplicateNames(qName, TYPEDECL_TYPE, fUnparsedTypeRegistry, fUnparsedTypeRegistrySub, globalComp, currSchemaDoc);
                    }
                    else if (componentType.equals(SchemaSymbols.ELT_ELEMENT)) {
                        checkForDuplicateNames(qName, ELEMENT_TYPE, fUnparsedElementRegistry, fUnparsedElementRegistrySub, globalComp, currSchemaDoc);
                    }
                    else if (componentType.equals(SchemaSymbols.ELT_GROUP)) {
                        checkForDuplicateNames(qName, GROUP_TYPE, fUnparsedGroupRegistry, fUnparsedGroupRegistrySub, globalComp, currSchemaDoc);
                    }
                    else if (componentType.equals(SchemaSymbols.ELT_NOTATION)) {
                        checkForDuplicateNames(qName, NOTATION_TYPE, fUnparsedNotationRegistry, fUnparsedNotationRegistrySub, globalComp, currSchemaDoc);
                    }
                }
            } 

            DOMUtil.setHidden(currDoc, fHiddenNodes);
            List<XSDocumentInfo> currSchemaDepends = fDependencyMap.get(currSchemaDoc);
            for (XSDocumentInfo currSchemaDepend : currSchemaDepends) {
                schemasToProcess.push(currSchemaDepend);
            }
        } 

    } 

    protected void traverseSchemas(List<Object> annotationInfo) {
        setSchemasVisible(fRoot);
        Stack<XSDocumentInfo> schemasToProcess = new Stack<>();
        schemasToProcess.push(fRoot);
        while (!schemasToProcess.empty()) {
            XSDocumentInfo currSchemaDoc = schemasToProcess.pop();
            Element currDoc = currSchemaDoc.fSchemaElement;

            SchemaGrammar currSG = fGrammarBucket.getGrammar(currSchemaDoc.fTargetNamespace);

            if(DOMUtil.isHidden(currDoc, fHiddenNodes)) {
                continue;
            }
            Element currRoot = currDoc;
            boolean sawAnnotation = false;
            for (Element globalComp =
                DOMUtil.getFirstVisibleChildElement(currRoot, fHiddenNodes);
            globalComp != null;
            globalComp = DOMUtil.getNextVisibleSiblingElement(globalComp, fHiddenNodes)) {
                DOMUtil.setHidden(globalComp, fHiddenNodes);
                String componentType = DOMUtil.getLocalName(globalComp);
                if (DOMUtil.getLocalName(globalComp).equals(SchemaSymbols.ELT_REDEFINE)) {
                    currSchemaDoc.backupNSSupport((fRedefine2NSSupport!=null) ?
                            fRedefine2NSSupport.get(globalComp) : null);
                    for (Element redefinedComp = DOMUtil.getFirstVisibleChildElement(globalComp, fHiddenNodes);
                    redefinedComp != null;
                    redefinedComp = DOMUtil.getNextVisibleSiblingElement(redefinedComp, fHiddenNodes)) {
                        String redefinedComponentType = DOMUtil.getLocalName(redefinedComp);
                        DOMUtil.setHidden(redefinedComp, fHiddenNodes);
                        if (redefinedComponentType.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
                            fAttributeGroupTraverser.traverseGlobal(redefinedComp, currSchemaDoc, currSG);
                        }
                        else if (redefinedComponentType.equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
                            fComplexTypeTraverser.traverseGlobal(redefinedComp, currSchemaDoc, currSG);
                        }
                        else if (redefinedComponentType.equals(SchemaSymbols.ELT_GROUP)) {
                            fGroupTraverser.traverseGlobal(redefinedComp, currSchemaDoc, currSG);
                        }
                        else if (redefinedComponentType.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                            fSimpleTypeTraverser.traverseGlobal(redefinedComp, currSchemaDoc, currSG);
                        }
                        else {
                            reportSchemaError("s4s-elt-must-match.1", new Object [] {DOMUtil.getLocalName(globalComp), "(annotation | (simpleType | complexType | group | attributeGroup))*", redefinedComponentType}, redefinedComp);
                        }
                    } 
                    currSchemaDoc.restoreNSSupport();
                }
                else if (componentType.equals(SchemaSymbols.ELT_ATTRIBUTE)) {
                    fAttributeTraverser.traverseGlobal(globalComp, currSchemaDoc, currSG);
                }
                else if (componentType.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
                    fAttributeGroupTraverser.traverseGlobal(globalComp, currSchemaDoc, currSG);
                }
                else if (componentType.equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
                    fComplexTypeTraverser.traverseGlobal(globalComp, currSchemaDoc, currSG);
                }
                else if (componentType.equals(SchemaSymbols.ELT_ELEMENT)) {
                    fElementTraverser.traverseGlobal(globalComp, currSchemaDoc, currSG);
                }
                else if (componentType.equals(SchemaSymbols.ELT_GROUP)) {
                    fGroupTraverser.traverseGlobal(globalComp, currSchemaDoc, currSG);
                }
                else if (componentType.equals(SchemaSymbols.ELT_NOTATION)) {
                    fNotationTraverser.traverse(globalComp, currSchemaDoc, currSG);
                }
                else if (componentType.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                    fSimpleTypeTraverser.traverseGlobal(globalComp, currSchemaDoc, currSG);
                }
                else if (componentType.equals(SchemaSymbols.ELT_ANNOTATION)) {
                    currSG.addAnnotation(fElementTraverser.traverseAnnotationDecl(globalComp, currSchemaDoc.getSchemaAttrs(), true, currSchemaDoc));
                    sawAnnotation = true;
                }
                else {
                    reportSchemaError("s4s-elt-invalid-content.1", new Object [] {SchemaSymbols.ELT_SCHEMA, DOMUtil.getLocalName(globalComp)}, globalComp);
                }
            } 

            if (!sawAnnotation) {
                String text = DOMUtil.getSyntheticAnnotation(currRoot);
                if (text != null) {
                    currSG.addAnnotation(fElementTraverser.traverseSyntheticAnnotation(currRoot, text, currSchemaDoc.getSchemaAttrs(), true, currSchemaDoc));
                }
            }

            /** Collect annotation information for validation. **/
            if (annotationInfo != null) {
                XSAnnotationInfo info = currSchemaDoc.getAnnotations();
                /** Only add annotations to the list if there were any in this document. **/
                if (info != null) {
                    annotationInfo.add(doc2SystemId(currDoc));
                    annotationInfo.add(info);
                }
            }
            currSchemaDoc.returnSchemaAttrs();
            DOMUtil.setHidden(currDoc, fHiddenNodes);

            List<XSDocumentInfo> currSchemaDepends = fDependencyMap.get(currSchemaDoc);
            for (XSDocumentInfo currSchemaDepend : currSchemaDepends) {
                schemasToProcess.push(currSchemaDepend);
            }
        } 
    } 

    private List<String> fReportedTNS = null;
    private final boolean needReportTNSError(String uri) {
        if (fReportedTNS == null)
            fReportedTNS = new ArrayList<>();
        else if (fReportedTNS.contains(uri))
            return false;
        fReportedTNS.add(uri);
        return true;
    }

    private static final String[] COMP_TYPE = {
            null,               
            "attribute declaration",
            "attribute group",
            "element declaration",
            "group",
            "identity constraint",
            "notation",
            "type definition",
    };

    private static final String[] CIRCULAR_CODES = {
            "Internal-Error",
            "Internal-Error",
            "src-attribute_group.3",
            "e-props-correct.6",
            "mg-props-correct.2",
            "Internal-Error",
            "Internal-Error",
            "st-props-correct.2",       
    };

    void addGlobalAttributeDecl(XSAttributeDecl decl) {
        final String namespace = decl.getNamespace();
        final String declKey = (namespace == null || namespace.length() == 0)
            ? "," + decl.getName() : namespace + "," + decl.getName();

        if (fGlobalAttrDecls.get(declKey) == null) {
            fGlobalAttrDecls.put(declKey, decl);
        }
    }

    void addGlobalAttributeGroupDecl(XSAttributeGroupDecl decl) {
        final String namespace = decl.getNamespace();
        final String declKey = (namespace == null || namespace.length() == 0)
            ? "," + decl.getName() : namespace + "," + decl.getName();

        if (fGlobalAttrGrpDecls.get(declKey) == null) {
            fGlobalAttrGrpDecls.put(declKey, decl);
        }
    }

    void addGlobalElementDecl(XSElementDecl decl) {
        final String namespace = decl.getNamespace();
        final String declKey = (namespace == null || namespace.length() == 0)
            ? "," + decl.getName() : namespace + "," + decl.getName();

        if (fGlobalElemDecls.get(declKey) == null) {
            fGlobalElemDecls.put(declKey, decl);
        }
    }

    void addGlobalGroupDecl(XSGroupDecl decl) {
        final String namespace = decl.getNamespace();
        final String declKey = (namespace == null || namespace.length() == 0)
            ? "," + decl.getName() : namespace + "," + decl.getName();

        if (fGlobalGroupDecls.get(declKey) == null) {
            fGlobalGroupDecls.put(declKey, decl);
        }
    }

    void addGlobalNotationDecl(XSNotationDecl decl) {
        final String namespace = decl.getNamespace();
        final String declKey = (namespace == null || namespace.length() == 0)
            ? "," + decl.getName() : namespace + "," + decl.getName();

        if (fGlobalNotationDecls.get(declKey) == null) {
            fGlobalNotationDecls.put(declKey, decl);
        }
    }

    void addGlobalTypeDecl(XSTypeDefinition decl) {
        final String namespace = decl.getNamespace();
        final String declKey = (namespace == null || namespace.length() == 0)
            ? "," + decl.getName() : namespace + "," + decl.getName();

        if (fGlobalTypeDecls.get(declKey) == null) {
            fGlobalTypeDecls.put(declKey, decl);
        }
    }

    void addIDConstraintDecl(IdentityConstraint decl) {
        final String namespace = decl.getNamespace();
        final String declKey = (namespace == null || namespace.length() == 0)
            ? "," + decl.getIdentityConstraintName() : namespace + "," + decl.getIdentityConstraintName();

        if (fGlobalIDConstraintDecls.get(declKey) == null) {
            fGlobalIDConstraintDecls.put(declKey, decl);
        }
    }

    private XSAttributeDecl getGlobalAttributeDecl(String declKey) {
        return (XSAttributeDecl)fGlobalAttrDecls.get(declKey);
    }

    private XSAttributeGroupDecl getGlobalAttributeGroupDecl(String declKey) {
        return (XSAttributeGroupDecl)fGlobalAttrGrpDecls.get(declKey);
    }

    private XSElementDecl getGlobalElementDecl(String declKey) {
        return (XSElementDecl)fGlobalElemDecls.get(declKey);
    }

    private XSGroupDecl getGlobalGroupDecl(String declKey) {
        return (XSGroupDecl)fGlobalGroupDecls.get(declKey);
    }

    private XSNotationDecl getGlobalNotationDecl(String declKey) {
        return (XSNotationDecl)fGlobalNotationDecls.get(declKey);
    }

    private XSTypeDefinition getGlobalTypeDecl(String declKey) {
        return (XSTypeDefinition)fGlobalTypeDecls.get(declKey);
    }

    private IdentityConstraint getIDConstraintDecl(String declKey) {
        return (IdentityConstraint)fGlobalIDConstraintDecls.get(declKey);
    }

    protected Object getGlobalDecl(XSDocumentInfo currSchema,
            int declType,
            QName declToTraverse,
            Element elmNode) {

        if (DEBUG_NODE_POOL) {
            System.out.println("TRAVERSE_GL: "+declToTraverse.toString());
        }

        if (declToTraverse.uri != null &&
                declToTraverse.uri == SchemaSymbols.URI_SCHEMAFORSCHEMA) {
            if (declType == TYPEDECL_TYPE) {
                Object retObj = SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(declToTraverse.localpart);
                if (retObj != null)
                    return retObj;
            }
        }

        if (!currSchema.isAllowedNS(declToTraverse.uri)) {
            if (currSchema.needReportTNSError(declToTraverse.uri)) {
                String code = declToTraverse.uri == null ? "src-resolve.4.1" : "src-resolve.4.2";
                reportSchemaError(code, new Object[]{fDoc2SystemId.get(currSchema.fSchemaElement), declToTraverse.uri, declToTraverse.rawname}, elmNode);
            }
        }

        SchemaGrammar sGrammar = fGrammarBucket.getGrammar(declToTraverse.uri);
        if (sGrammar == null) {
            if (needReportTNSError(declToTraverse.uri))
                reportSchemaError("src-resolve", new Object[]{declToTraverse.rawname, COMP_TYPE[declType]}, elmNode);
            return null;
        }

        Object retObj = getGlobalDeclFromGrammar(sGrammar, declType, declToTraverse.localpart);
        String declKey = declToTraverse.uri == null? ","+declToTraverse.localpart:
            declToTraverse.uri+","+declToTraverse.localpart;

        if (!fTolerateDuplicates) {
            if (retObj != null) {
                return retObj;
            }
        }
        else {
            Object retObj2 = getGlobalDecl(declKey, declType);
            if (retObj2 != null) {
                return retObj2;
            }
        }

        XSDocumentInfo schemaWithDecl = null;
        Element decl = null;
        XSDocumentInfo declDoc = null;

        switch (declType) {
        case ATTRIBUTE_TYPE :
            decl = getElementFromMap(fUnparsedAttributeRegistry, declKey);
            declDoc = getDocInfoFromMap(fUnparsedAttributeRegistrySub, declKey);
            break;
        case ATTRIBUTEGROUP_TYPE :
            decl = getElementFromMap(fUnparsedAttributeGroupRegistry, declKey);
            declDoc = getDocInfoFromMap(fUnparsedAttributeGroupRegistrySub, declKey);
            break;
        case ELEMENT_TYPE :
            decl = getElementFromMap(fUnparsedElementRegistry, declKey);
            declDoc = getDocInfoFromMap(fUnparsedElementRegistrySub, declKey);
            break;
        case GROUP_TYPE :
            decl = getElementFromMap(fUnparsedGroupRegistry, declKey);
            declDoc = getDocInfoFromMap(fUnparsedGroupRegistrySub, declKey);
            break;
        case IDENTITYCONSTRAINT_TYPE :
            decl = getElementFromMap(fUnparsedIdentityConstraintRegistry, declKey);
            declDoc = getDocInfoFromMap(fUnparsedIdentityConstraintRegistrySub, declKey);
            break;
        case NOTATION_TYPE :
            decl = getElementFromMap(fUnparsedNotationRegistry, declKey);
            declDoc = getDocInfoFromMap(fUnparsedNotationRegistrySub, declKey);
            break;
        case TYPEDECL_TYPE :
            decl = getElementFromMap(fUnparsedTypeRegistry, declKey);
            declDoc = getDocInfoFromMap(fUnparsedTypeRegistrySub, declKey);
            break;
        default:
            reportSchemaError("Internal-Error", new Object [] {"XSDHandler asked to locate component of type " + declType + "; it does not recognize this type!"}, elmNode);
        }

        if (decl == null) {
            if (retObj == null) {
                reportSchemaError("src-resolve", new Object[]{declToTraverse.rawname, COMP_TYPE[declType]}, elmNode);
            }
            return retObj;
        }

        schemaWithDecl = findXSDocumentForDecl(currSchema, decl, declDoc);
        if (schemaWithDecl == null) {
            if (retObj == null) {
                String code = declToTraverse.uri == null ? "src-resolve.4.1" : "src-resolve.4.2";
                reportSchemaError(code, new Object[]{fDoc2SystemId.get(currSchema.fSchemaElement), declToTraverse.uri, declToTraverse.rawname}, elmNode);
            }
            return retObj;
        }

        if (DOMUtil.isHidden(decl, fHiddenNodes)) {
            if (retObj == null) {
                String code = CIRCULAR_CODES[declType];
                if (declType == TYPEDECL_TYPE) {
                    if (SchemaSymbols.ELT_COMPLEXTYPE.equals(DOMUtil.getLocalName(decl))) {
                        code = "ct-props-correct.3";
                    }
                }
                reportSchemaError(code, new Object [] {declToTraverse.prefix+":"+declToTraverse.localpart}, elmNode);
            }
            return retObj;
        }

        return traverseGlobalDecl(declType, decl, schemaWithDecl, sGrammar);
    } 

    protected Object getGlobalDecl(String declKey, int declType) {
        Object retObj = null;

        switch (declType) {
        case ATTRIBUTE_TYPE :
            retObj = getGlobalAttributeDecl(declKey);
            break;
        case ATTRIBUTEGROUP_TYPE :
            retObj = getGlobalAttributeGroupDecl(declKey);
            break;
        case ELEMENT_TYPE :
            retObj = getGlobalElementDecl(declKey);
            break;
        case GROUP_TYPE :
            retObj = getGlobalGroupDecl(declKey);
            break;
        case IDENTITYCONSTRAINT_TYPE :
            retObj = getIDConstraintDecl(declKey);
            break;
        case NOTATION_TYPE :
            retObj = getGlobalNotationDecl(declKey);
            break;
        case TYPEDECL_TYPE :
            retObj = getGlobalTypeDecl(declKey);
            break;
        }

        return retObj;
    }

    protected Object getGlobalDeclFromGrammar(SchemaGrammar sGrammar, int declType, String localpart) {
        Object retObj = null;

        switch (declType) {
        case ATTRIBUTE_TYPE :
            retObj = sGrammar.getGlobalAttributeDecl(localpart);
            break;
        case ATTRIBUTEGROUP_TYPE :
            retObj = sGrammar.getGlobalAttributeGroupDecl(localpart);
            break;
        case ELEMENT_TYPE :
            retObj = sGrammar.getGlobalElementDecl(localpart);
            break;
        case GROUP_TYPE :
            retObj = sGrammar.getGlobalGroupDecl(localpart);
            break;
        case IDENTITYCONSTRAINT_TYPE :
            retObj = sGrammar.getIDConstraintDecl(localpart);
            break;
        case NOTATION_TYPE :
            retObj = sGrammar.getGlobalNotationDecl(localpart);
            break;
        case TYPEDECL_TYPE :
            retObj = sGrammar.getGlobalTypeDecl(localpart);
            break;
        }

        return retObj;
    }

    protected Object getGlobalDeclFromGrammar(SchemaGrammar sGrammar, int declType, String localpart, String schemaLoc) {
        Object retObj = null;

        switch (declType) {
        case ATTRIBUTE_TYPE :
            retObj = sGrammar.getGlobalAttributeDecl(localpart, schemaLoc);
            break;
        case ATTRIBUTEGROUP_TYPE :
            retObj = sGrammar.getGlobalAttributeGroupDecl(localpart, schemaLoc);
            break;
        case ELEMENT_TYPE :
            retObj = sGrammar.getGlobalElementDecl(localpart, schemaLoc);
            break;
        case GROUP_TYPE :
            retObj = sGrammar.getGlobalGroupDecl(localpart, schemaLoc);
            break;
        case IDENTITYCONSTRAINT_TYPE :
            retObj = sGrammar.getIDConstraintDecl(localpart, schemaLoc);
            break;
        case NOTATION_TYPE :
            retObj = sGrammar.getGlobalNotationDecl(localpart, schemaLoc);
            break;
        case TYPEDECL_TYPE :
            retObj = sGrammar.getGlobalTypeDecl(localpart, schemaLoc);
            break;
        }

        return retObj;
    }

    protected Object traverseGlobalDecl(int declType, Element decl,
            XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        Object retObj = null;

        DOMUtil.setHidden(decl, fHiddenNodes);
        SchemaNamespaceSupport nsSupport = null;
        Element parent = DOMUtil.getParent(decl);
        if (DOMUtil.getLocalName(parent).equals(SchemaSymbols.ELT_REDEFINE))
            nsSupport = (fRedefine2NSSupport!=null) ? fRedefine2NSSupport.get(parent) : null;
        schemaDoc.backupNSSupport(nsSupport);

        switch (declType) {
        case TYPEDECL_TYPE :
            if (DOMUtil.getLocalName(decl).equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
                retObj = fComplexTypeTraverser.traverseGlobal(decl, schemaDoc, grammar);
            }
            else {
                retObj = fSimpleTypeTraverser.traverseGlobal(decl, schemaDoc, grammar);
            }
            break;
        case ATTRIBUTE_TYPE :
            retObj = fAttributeTraverser.traverseGlobal(decl, schemaDoc, grammar);
            break;
        case ELEMENT_TYPE :
            retObj = fElementTraverser.traverseGlobal(decl, schemaDoc, grammar);
            break;
        case ATTRIBUTEGROUP_TYPE :
            retObj = fAttributeGroupTraverser.traverseGlobal(decl, schemaDoc, grammar);
            break;
        case GROUP_TYPE :
            retObj = fGroupTraverser.traverseGlobal(decl, schemaDoc, grammar);
            break;
        case NOTATION_TYPE :
            retObj = fNotationTraverser.traverse(decl, schemaDoc, grammar);
            break;
        case IDENTITYCONSTRAINT_TYPE :
            break;
        }

        schemaDoc.restoreNSSupport();

        return retObj;
    }

    public String schemaDocument2SystemId(XSDocumentInfo schemaDoc) {
        return fDoc2SystemId.get(schemaDoc.fSchemaElement);
    }

    Object getGrpOrAttrGrpRedefinedByRestriction(int type, QName name, XSDocumentInfo currSchema, Element elmNode) {
        String realName = name.uri != null?name.uri+","+name.localpart:
            ","+name.localpart;
        String nameToFind = null;
        switch (type) {
        case ATTRIBUTEGROUP_TYPE:
            nameToFind = fRedefinedRestrictedAttributeGroupRegistry.get(realName);
            break;
        case GROUP_TYPE:
            nameToFind = fRedefinedRestrictedGroupRegistry.get(realName);
            break;
        default:
            return null;
        }
        if (nameToFind == null) return null;
        int commaPos = nameToFind.indexOf(",");
        QName qNameToFind = new QName(XMLSymbols.EMPTY_STRING, nameToFind.substring(commaPos+1),
                nameToFind.substring(commaPos), (commaPos == 0)? null : nameToFind.substring(0, commaPos));
        Object retObj = getGlobalDecl(currSchema, type, qNameToFind, elmNode);
        if(retObj == null) {
            switch (type) {
            case ATTRIBUTEGROUP_TYPE:
                reportSchemaError("src-redefine.7.2.1", new Object []{name.localpart}, elmNode);
                break;
            case GROUP_TYPE:
                reportSchemaError("src-redefine.6.2.1", new Object []{name.localpart}, elmNode);
                break;
            }
            return null;
        }
        return retObj;
    } 

    protected void resolveKeyRefs() {
        for (int i=0; i<fKeyrefStackPos; i++) {
            XSDocumentInfo keyrefSchemaDoc = fKeyrefsMapXSDocumentInfo[i];
            keyrefSchemaDoc.fNamespaceSupport.makeGlobal();
            keyrefSchemaDoc.fNamespaceSupport.setEffectiveContext( fKeyrefNamespaceContext[i] );
            SchemaGrammar keyrefGrammar = fGrammarBucket.getGrammar(keyrefSchemaDoc.fTargetNamespace);
                DOMUtil.setHidden(fKeyrefs[i], fHiddenNodes);
            fKeyrefTraverser.traverse(fKeyrefs[i], fKeyrefElems[i], keyrefSchemaDoc, keyrefGrammar);
        }
    } 

    protected Map<String, Element> getIDRegistry() {
        return fUnparsedIdentityConstraintRegistry;
    }
    protected Map<String, XSDocumentInfo> getIDRegistry_sub() {
        return fUnparsedIdentityConstraintRegistrySub;
    }



    protected void storeKeyRef (Element keyrefToStore, XSDocumentInfo schemaDoc,
            XSElementDecl currElemDecl) {
        String keyrefName = DOMUtil.getAttrValue(keyrefToStore, SchemaSymbols.ATT_NAME);
        if (keyrefName.length() != 0) {
            String keyrefQName = schemaDoc.fTargetNamespace == null?
                    "," + keyrefName: schemaDoc.fTargetNamespace+","+keyrefName;
            checkForDuplicateNames(keyrefQName, IDENTITYCONSTRAINT_TYPE, fUnparsedIdentityConstraintRegistry, fUnparsedIdentityConstraintRegistrySub, keyrefToStore, schemaDoc);
        }

        if (fKeyrefStackPos == fKeyrefs.length) {
            Element [] elemArray = new Element [fKeyrefStackPos + INC_KEYREF_STACK_AMOUNT];
            System.arraycopy(fKeyrefs, 0, elemArray, 0, fKeyrefStackPos);
            fKeyrefs = elemArray;
            XSElementDecl [] declArray = new XSElementDecl [fKeyrefStackPos + INC_KEYREF_STACK_AMOUNT];
            System.arraycopy(fKeyrefElems, 0, declArray, 0, fKeyrefStackPos);
            fKeyrefElems = declArray;
            String[][] stringArray = new String [fKeyrefStackPos + INC_KEYREF_STACK_AMOUNT][];
            System.arraycopy(fKeyrefNamespaceContext, 0, stringArray, 0, fKeyrefStackPos);
            fKeyrefNamespaceContext = stringArray;

            XSDocumentInfo [] xsDocumentInfo = new XSDocumentInfo [fKeyrefStackPos + INC_KEYREF_STACK_AMOUNT];
            System.arraycopy(fKeyrefsMapXSDocumentInfo, 0, xsDocumentInfo, 0, fKeyrefStackPos);
            fKeyrefsMapXSDocumentInfo = xsDocumentInfo;

        }
        fKeyrefs[fKeyrefStackPos] = keyrefToStore;
        fKeyrefElems[fKeyrefStackPos] = currElemDecl;
        fKeyrefNamespaceContext[fKeyrefStackPos] = schemaDoc.fNamespaceSupport.getEffectiveLocalContext();

        fKeyrefsMapXSDocumentInfo[fKeyrefStackPos++] = schemaDoc;
    } 


    /**
     * resolveSchema method is responsible for resolving location of the schema (using XMLEntityResolver),
     * and if it was successfully resolved getting the schema Document.
     * @param desc
     * @param mustResolve
     * @param referElement
     * @return A schema Element or null.
     */
    private Element resolveSchema(XSDDescription desc, boolean mustResolve,
                                  Element referElement, boolean usePairs) {
        XMLInputSource schemaSource = null;
        try {
            Map<String, XMLSchemaLoader.LocationArray> pairs = usePairs ? fLocationPairs : Collections.emptyMap();
            schemaSource = XMLSchemaLoader.resolveDocument(desc, pairs, fEntityManager);
        }
        catch (IOException ex) {
            if (mustResolve) {
                reportSchemaError("schema_reference.4",
                        new Object[]{desc.getLocationHints()[0]},
                        referElement);
            }
            else {
                reportSchemaWarning("schema_reference.4",
                        new Object[]{desc.getLocationHints()[0]},
                        referElement);
            }
        }
        if (schemaSource instanceof DOMInputSource) {
            return getSchemaDocument(desc.getTargetNamespace(), (DOMInputSource) schemaSource, mustResolve, desc.getContextType(), referElement);
        } 
        else if (schemaSource instanceof SAXInputSource) {
            return getSchemaDocument(desc.getTargetNamespace(), (SAXInputSource) schemaSource, mustResolve, desc.getContextType(), referElement);
        } 
        else if (schemaSource instanceof StAXInputSource) {
            return getSchemaDocument(desc.getTargetNamespace(), (StAXInputSource) schemaSource, mustResolve, desc.getContextType(), referElement);
        } 
        else if (schemaSource instanceof XSInputSource) {
            return getSchemaDocument((XSInputSource) schemaSource, desc);
        } 
        return getSchemaDocument(desc.getTargetNamespace(), schemaSource, mustResolve, desc.getContextType(), referElement);
    } 

    private Element resolveSchema(XMLInputSource schemaSource, XSDDescription desc,
            boolean mustResolve, Element referElement) {

        if (schemaSource instanceof DOMInputSource) {
            return getSchemaDocument(desc.getTargetNamespace(), (DOMInputSource) schemaSource, mustResolve, desc.getContextType(), referElement);
        } 
        else if (schemaSource instanceof SAXInputSource) {
            return getSchemaDocument(desc.getTargetNamespace(), (SAXInputSource) schemaSource, mustResolve, desc.getContextType(), referElement);
        } 
        else if (schemaSource instanceof StAXInputSource) {
            return getSchemaDocument(desc.getTargetNamespace(), (StAXInputSource) schemaSource, mustResolve, desc.getContextType(), referElement);
        } 
        else if (schemaSource instanceof XSInputSource) {
            return getSchemaDocument((XSInputSource) schemaSource, desc);
        } 
        return getSchemaDocument(desc.getTargetNamespace(), schemaSource, mustResolve, desc.getContextType(), referElement);
    }

    private XMLInputSource resolveSchemaSource(XSDDescription desc, boolean mustResolve,
            Element referElement, boolean usePairs) {

        XMLInputSource schemaSource = null;
        try {
            Map<String, XMLSchemaLoader.LocationArray> pairs = usePairs ? fLocationPairs : Collections.emptyMap();
            schemaSource = XMLSchemaLoader.resolveDocument(desc, pairs, fEntityManager);
        }
        catch (IOException ex) {
            if (mustResolve) {
                reportSchemaError("schema_reference.4",
                        new Object[]{desc.getLocationHints()[0]},
                        referElement);
            }
            else {
                reportSchemaWarning("schema_reference.4",
                        new Object[]{desc.getLocationHints()[0]},
                        referElement);
            }
        }

        return schemaSource;
    }

    /**
     * getSchemaDocument method uses XMLInputSource to parse a schema document.
     * @param schemaNamespace
     * @param schemaSource
     * @param mustResolve
     * @param referType
     * @param referElement
     * @return A schema Element.
     */
    private Element getSchemaDocument(String schemaNamespace, XMLInputSource schemaSource,
            boolean mustResolve, short referType, Element referElement) {

        boolean hasInput = true;
        IOException exception = null;
        Element schemaElement = null;
        try {
            if (schemaSource != null &&
                    (schemaSource.getSystemId() != null ||
                            schemaSource.getByteStream() != null ||
                            schemaSource.getCharacterStream() != null)) {

                XSDKey key = null;
                String schemaId = null;
                if (referType != XSDDescription.CONTEXT_PREPARSE){
                    schemaId = XMLEntityManager.expandSystemId(schemaSource.getSystemId(), schemaSource.getBaseSystemId(), false);
                    key = new XSDKey(schemaId, referType, schemaNamespace);
                    if((schemaElement = fTraversed.get(key)) != null) {
                        fLastSchemaWasDuplicate = true;
                        return schemaElement;
                    }
                    if ((!schemaSource.isCreatedByResolver()) &&
                            (referType == XSDDescription.CONTEXT_IMPORT || referType == XSDDescription.CONTEXT_INCLUDE
                            || referType == XSDDescription.CONTEXT_REDEFINE)) {
                        String accessError = SecuritySupport.checkAccess(schemaId, fAccessExternalSchema, JdkConstants.ACCESS_EXTERNAL_ALL);
                        if (accessError != null) {
                            reportSchemaFatalError("schema_reference.access",
                                    new Object[] { SecuritySupport.sanitizePath(schemaId), accessError },
                                    referElement);
                        }
                    }
                }

                fSchemaParser.parse(schemaSource);
                Document schemaDocument = fSchemaParser.getDocument();
                schemaElement = schemaDocument != null ? DOMUtil.getRoot(schemaDocument) : null;
                return getSchemaDocument0(key, schemaId, schemaElement);
            }
            else {
                hasInput = false;
            }
        }
        catch (IOException ex) {
            exception = ex;
        }
        return getSchemaDocument1(mustResolve, hasInput, schemaSource, referElement, exception);
    } 

    /**
     * getSchemaDocument method uses SAXInputSource to parse a schema document.
     * @param schemaNamespace
     * @param schemaSource
     * @param mustResolve
     * @param referType
     * @param referElement
     * @return A schema Element.
     */
    private Element getSchemaDocument(String schemaNamespace, SAXInputSource schemaSource,
            boolean mustResolve, short referType, Element referElement) {
        XMLReader parser = schemaSource.getXMLReader();
        InputSource inputSource = schemaSource.getInputSource();
        boolean hasInput = true;
        IOException exception = null;
        Element schemaElement = null;
        try {
            if (inputSource != null &&
                    (inputSource.getSystemId() != null ||
                     inputSource.getByteStream() != null ||
                     inputSource.getCharacterStream() != null)) {

                XSDKey key = null;
                String schemaId = null;
                if (referType != XSDDescription.CONTEXT_PREPARSE) {
                    schemaId = XMLEntityManager.expandSystemId(inputSource.getSystemId(),
                            schemaSource.getBaseSystemId(), false);
                    key = new XSDKey(schemaId, referType, schemaNamespace);
                    if ((schemaElement = fTraversed.get(key)) != null) {
                        fLastSchemaWasDuplicate = true;
                        return schemaElement;
                    }
                }

                boolean namespacePrefixes = false;
                if (parser != null) {
                    try {
                        namespacePrefixes = parser.getFeature(NAMESPACE_PREFIXES);
                    }
                    catch (SAXException se) {}
                }
                else {
                    parser = JdkXmlUtils.getXMLReader(fSecurityManager,
                            fOverrideDefaultParser, fSecurityManager.isSecureProcessing(),
                            fUseCatalog,
                            JdkXmlUtils.getCatalogFeatures(fDefer, fCatalogFile, fPrefer, fResolve));

                    try {
                        parser.setFeature(NAMESPACE_PREFIXES, true);
                        namespacePrefixes = true;
                        if (parser instanceof SAXParser) {
                            if (fSecurityManager != null) {
                                parser.setProperty(SECURITY_MANAGER, fSecurityManager);
                            }
                        }
                    }
                    catch (SAXException se) {}

                    try {
                        parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, fAccessExternalDTD);
                    } catch (SAXNotRecognizedException exc) {
                        XMLSecurityManager.printWarning(parser.getClass().getName(),
                                XMLConstants.ACCESS_EXTERNAL_DTD, exc);
                    }
                }
                boolean stringsInternalized = false;
                try {
                    stringsInternalized = parser.getFeature(STRING_INTERNING);
                }
                catch (SAXException exc) {
                }
                if (fXSContentHandler == null) {
                    fXSContentHandler = new SchemaContentHandler();
                }
                fXSContentHandler.reset(fSchemaParser, fSymbolTable,
                        namespacePrefixes, stringsInternalized);
                parser.setContentHandler(fXSContentHandler);
                parser.setErrorHandler(fErrorReporter.getSAXErrorHandler());

                parser.parse(inputSource);
                try {
                    parser.setContentHandler(null);
                    parser.setErrorHandler(null);
                }
                catch (Exception e) {}

                Document schemaDocument = fXSContentHandler.getDocument();
                schemaElement = schemaDocument != null ? DOMUtil.getRoot(schemaDocument) : null;
                return getSchemaDocument0(key, schemaId, schemaElement);
            }
            else {
                hasInput = false;
            }
        }
        catch (SAXParseException spe) {
            throw SAX2XNIUtil.createXMLParseException0(spe);
        }
        catch (SAXException se) {
            throw SAX2XNIUtil.createXNIException0(se);
        }
        catch (IOException ioe) {
            exception = ioe;
        }
        return getSchemaDocument1(mustResolve, hasInput, schemaSource, referElement, exception);
    } 

    /**
     * getSchemaDocument method uses DOMInputSource to parse a schema document.
     * @param schemaNamespace
     * @param schemaSource
     * @param mustResolve
     * @param referType
     * @param referElement
     * @return A schema Element.
     */
    private Element getSchemaDocument(String schemaNamespace, DOMInputSource schemaSource,
            boolean mustResolve, short referType, Element referElement) {
        boolean hasInput = true;
        IOException exception = null;
        Element schemaElement = null;
        Element schemaRootElement = null;

        final Node node = schemaSource.getNode();
        short nodeType = -1;
        if (node != null) {
            nodeType = node.getNodeType();
            if (nodeType == Node.DOCUMENT_NODE) {
                schemaRootElement = DOMUtil.getRoot((Document) node);
            }
            else if (nodeType == Node.ELEMENT_NODE) {
                schemaRootElement = (Element) node;
            }
        }

        try {
            if (schemaRootElement != null) {
                XSDKey key = null;
                String schemaId = null;
                if (referType != XSDDescription.CONTEXT_PREPARSE) {
                    schemaId = XMLEntityManager.expandSystemId(schemaSource.getSystemId(), schemaSource.getBaseSystemId(), false);
                    boolean isDocument = (nodeType == Node.DOCUMENT_NODE);
                    if (!isDocument) {
                        Node parent = schemaRootElement.getParentNode();
                        if (parent != null) {
                            isDocument = (parent.getNodeType() == Node.DOCUMENT_NODE);
                        }
                    }
                    if (isDocument) {
                        key = new XSDKey(schemaId, referType, schemaNamespace);
                        if ((schemaElement = fTraversed.get(key)) != null) {
                            fLastSchemaWasDuplicate = true;
                            return schemaElement;
                        }
                    }
                }

                schemaElement = schemaRootElement;
                return getSchemaDocument0(key, schemaId, schemaElement);
            }
            else {
                hasInput = false;
            }
        }
        catch (IOException ioe) {
            exception = ioe;
        }
        return getSchemaDocument1(mustResolve, hasInput, schemaSource, referElement, exception);
    } 

    /**
     * getSchemaDocument method uses StAXInputSource to parse a schema document.
     * @param schemaNamespace
     * @param schemaSource
     * @param mustResolve
     * @param referType
     * @param referElement
     * @return A schema Element.
     */
    private Element getSchemaDocument(String schemaNamespace, StAXInputSource schemaSource,
            boolean mustResolve, short referType, Element referElement) {
        IOException exception = null;
        Element schemaElement = null;
        try {
            final boolean consumeRemainingContent = schemaSource.shouldConsumeRemainingContent();
            final XMLStreamReader streamReader = schemaSource.getXMLStreamReader();
            final XMLEventReader eventReader = schemaSource.getXMLEventReader();

            XSDKey key = null;
            String schemaId = null;
            if (referType != XSDDescription.CONTEXT_PREPARSE) {
                schemaId = XMLEntityManager.expandSystemId(schemaSource.getSystemId(), schemaSource.getBaseSystemId(), false);
                boolean isDocument = consumeRemainingContent;
                if (!isDocument) {
                    if (streamReader != null) {
                        isDocument = (streamReader.getEventType() == XMLStreamReader.START_DOCUMENT);
                    }
                    else {
                        isDocument = eventReader.peek().isStartDocument();
                    }
                }
                if (isDocument) {
                    key = new XSDKey(schemaId, referType, schemaNamespace);
                    if ((schemaElement = fTraversed.get(key)) != null) {
                        fLastSchemaWasDuplicate = true;
                        return schemaElement;
                    }
                }
            }

            if (fStAXSchemaParser == null) {
                fStAXSchemaParser = new StAXSchemaParser();
            }
            fStAXSchemaParser.reset(fSchemaParser, fSymbolTable);

            if (streamReader != null) {
                fStAXSchemaParser.parse(streamReader);
                if (consumeRemainingContent) {
                    while (streamReader.hasNext()) {
                        streamReader.next();
                    }
                }
            }
            else {
                fStAXSchemaParser.parse(eventReader);
                if (consumeRemainingContent) {
                    while (eventReader.hasNext()) {
                        eventReader.nextEvent();
                    }
                }
            }
            Document schemaDocument = fStAXSchemaParser.getDocument();
            schemaElement = schemaDocument != null ? DOMUtil.getRoot(schemaDocument) : null;
            return getSchemaDocument0(key, schemaId, schemaElement);
        }
        catch (XMLStreamException e) {
            Throwable t = e.getNestedException();
            if (t instanceof IOException) {
                exception = (IOException) t;
            }
            else {
                StAXLocationWrapper slw = new StAXLocationWrapper();
                slw.setLocation(e.getLocation());
                throw new XMLParseException(slw, e.getMessage(), e);
            }
        }
        catch (IOException e) {
            exception = e;
        }
        return getSchemaDocument1(mustResolve, true, schemaSource, referElement, exception);
    } 

    /**
     * Code shared between the various getSchemaDocument() methods which
     * stores mapping information for the document.
     */
    private Element getSchemaDocument0(XSDKey key, String schemaId, Element schemaElement) {
        if (key != null) {
            fTraversed.put(key, schemaElement);
        }
        if (schemaId != null) {
            fDoc2SystemId.put(schemaElement, schemaId);
        }
        fLastSchemaWasDuplicate = false;
        return schemaElement;
    } 

    /**
     * Error handling code shared between the various getSchemaDocument() methods.
     */
    private Element getSchemaDocument1(boolean mustResolve, boolean hasInput,
            XMLInputSource schemaSource, Element referElement, IOException ioe) {
        if (mustResolve) {
            if (hasInput) {
                reportSchemaError("schema_reference.4",
                        new Object[]{schemaSource.getSystemId()},
                        referElement, ioe);
            }
            else {
                reportSchemaError("schema_reference.4",
                        new Object[]{schemaSource == null ? "" : schemaSource.getSystemId()},
                        referElement, ioe);
            }
        }
        else if (hasInput) {
            reportSchemaWarning("schema_reference.4",
                    new Object[]{schemaSource.getSystemId()},
                    referElement, ioe);
        }

        fLastSchemaWasDuplicate = false;
        return null;
    } 

    /**
     * getSchemaDocument method uses XMLInputSource to parse a schema document.
     * @param schemaNamespace
     * @param schemaSource
     * @param mustResolve
     * @param referType
     * @param referElement
     * @return A schema Element.
     */
    private Element getSchemaDocument(XSInputSource schemaSource, XSDDescription desc) {

        SchemaGrammar[] grammars = schemaSource.getGrammars();
        short referType = desc.getContextType();

        if (grammars != null && grammars.length > 0) {
           List<SchemaGrammar> expandedGrammars = expandGrammars(grammars);
            if (fNamespaceGrowth || !existingGrammars(expandedGrammars)) {
                addGrammars(expandedGrammars);
                if (referType == XSDDescription.CONTEXT_PREPARSE) {
                    desc.setTargetNamespace(grammars[0].getTargetNamespace());
                }
            }
        }
        else {
            XSObject[] components = schemaSource.getComponents();
            if (components != null && components.length > 0) {
                Map<String, List<String>> importDependencies = new HashMap<>();
                List<XSObject> expandedComponents = expandComponents(components, importDependencies);
                if (fNamespaceGrowth || canAddComponents(expandedComponents)) {
                    addGlobalComponents(expandedComponents, importDependencies);
                    if (referType == XSDDescription.CONTEXT_PREPARSE) {
                        desc.setTargetNamespace(components[0].getNamespace());
                    }
                }
            }
        }
        return null;
    } 

    private List<SchemaGrammar> expandGrammars(SchemaGrammar[] grammars) {
        List<SchemaGrammar> currGrammars = new ArrayList<>();

        for (int i=0; i<grammars.length; i++) {
            if (!currGrammars.contains(grammars[i])) {
                currGrammars.add(grammars[i]);
            }
        }

        SchemaGrammar sg1, sg2;
        List<SchemaGrammar> gs;
        for (int i = 0; i < currGrammars.size(); i++) {
            sg1 = currGrammars.get(i);
            gs = sg1.getImportedGrammars();
            if (gs == null) {
                continue;
            }

            for (int j = gs.size() - 1; j >= 0; j--) {
                sg2 = gs.get(j);
                if (!currGrammars.contains(sg2)) {
                    currGrammars.add(sg2);
                }
            }
        }

        return currGrammars;
    }

    private boolean existingGrammars(List<SchemaGrammar> grammars) {
        int length = grammars.size();
        final XSDDescription desc = new XSDDescription();

        for (int i=0; i < length; i++) {
            final SchemaGrammar sg1 = grammars.get(i);
            desc.setNamespace(sg1.getTargetNamespace());

            final SchemaGrammar sg2 = findGrammar(desc, false);
            if (sg2 != null) {
                return true;
            }
        }

        return false;
    }

    private boolean canAddComponents(List<XSObject> components) {
        final int size = components.size();
        final XSDDescription desc = new XSDDescription();
        for (int i=0; i<size; i++) {
            XSObject component = components.get(i);
            if (!canAddComponent(component, desc)) {
                return false;
            }
        }
        return true;
    }

    private boolean canAddComponent(XSObject component, XSDDescription desc) {
        desc.setNamespace(component.getNamespace());

        final SchemaGrammar sg = findGrammar(desc, false);
        if (sg == null) {
            return true;
        }
        else if (sg.isImmutable()) {
            return false;
        }

        short componentType = component.getType();
        final String name = component.getName();

        switch (componentType) {
        case XSConstants.TYPE_DEFINITION :
            if (sg.getGlobalTypeDecl(name) == component) {
                return true;
            }
            break;
        case XSConstants.ATTRIBUTE_DECLARATION :
            if (sg.getGlobalAttributeDecl(name) == component) {
                return true;
            }
            break;
        case XSConstants.ATTRIBUTE_GROUP :
            if (sg.getGlobalAttributeDecl(name) == component) {
                return true;
            }
            break;
        case XSConstants.ELEMENT_DECLARATION :
            if (sg.getGlobalElementDecl(name) == component) {
                return true;
            }
            break;
        case XSConstants.MODEL_GROUP_DEFINITION :
            if (sg.getGlobalGroupDecl(name) == component) {
                return true;
            }
            break;
        case XSConstants.NOTATION_DECLARATION :
            if (sg.getGlobalNotationDecl(name) == component) {
                return true;
            }
            break;
        case XSConstants.IDENTITY_CONSTRAINT :
        case XSConstants.ATTRIBUTE_USE :
        default :
            return true;
        }
        return false;
    }

    private void addGrammars(List<SchemaGrammar> grammars) {
        int length = grammars.size();
        XSDDescription desc = new XSDDescription();

        for (int i=0; i < length; i++) {
            final SchemaGrammar sg1 = grammars.get(i);
            desc.setNamespace(sg1.getTargetNamespace());

            final SchemaGrammar sg2 = findGrammar(desc, fNamespaceGrowth);
            if (sg1 != sg2) {
                addGrammarComponents(sg1, sg2);
            }
        }
    }

    private void addGrammarComponents(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        if (dstGrammar == null) {
            createGrammarFrom(srcGrammar);
            return;
        }

        SchemaGrammar tmpGrammar = dstGrammar;
        if (tmpGrammar.isImmutable()) {
            tmpGrammar = createGrammarFrom(dstGrammar);
        }

        addNewGrammarLocations(srcGrammar, tmpGrammar);

        addNewImportedGrammars(srcGrammar, tmpGrammar);

        addNewGrammarComponents(srcGrammar, tmpGrammar);
    }

    private SchemaGrammar createGrammarFrom(SchemaGrammar grammar) {
        SchemaGrammar newGrammar = new SchemaGrammar(grammar);
        fGrammarBucket.putGrammar(newGrammar);
        updateImportListWith(newGrammar);
        updateImportListFor(newGrammar);
        return newGrammar;
    }

    private void addNewGrammarLocations(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        final StringList locations = srcGrammar.getDocumentLocations();
        final int locSize = locations.size();
        final StringList locations2 = dstGrammar.getDocumentLocations();

        for (int i=0; i<locSize; i++) {
            String loc = locations.item(i);
            if (!locations2.contains(loc)) {
                dstGrammar.addDocument(null, loc);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addNewImportedGrammars(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        final ArrayList<SchemaGrammar> src = (ArrayList<SchemaGrammar>)srcGrammar.getImportedGrammars();
        if (src != null) {
            ArrayList<SchemaGrammar> dst = (ArrayList<SchemaGrammar>)dstGrammar.getImportedGrammars();
            if (dst == null) {
                dst = new ArrayList<>();
                dstGrammar.setImportedGrammars(dst);
            }
            for (SchemaGrammar sg :src) {
                SchemaGrammar sg1 = fGrammarBucket.getGrammar(sg.getTargetNamespace());
                if (sg1 != null) {
                    sg = sg1;
                }
                if (!containedImportedGrammar(dst, sg)) {
                    dst.add(sg);
                }
            }
        }
    }

    private void updateImportList(List<SchemaGrammar> importedSrc, List<SchemaGrammar> importedDst)
    {
        final int size = importedSrc.size();

        for (int i=0; i<size; i++) {
            final SchemaGrammar sg =  importedSrc.get(i);
            if (!containedImportedGrammar(importedDst, sg)) {
                importedDst.add(sg);
            }
        }
    }

    private void addNewGrammarComponents(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        dstGrammar.resetComponents();
        addGlobalElementDecls(srcGrammar, dstGrammar);
        addGlobalAttributeDecls(srcGrammar, dstGrammar);
        addGlobalAttributeGroupDecls(srcGrammar, dstGrammar);
        addGlobalGroupDecls(srcGrammar, dstGrammar);
        addGlobalTypeDecls(srcGrammar, dstGrammar);
        addGlobalNotationDecls(srcGrammar, dstGrammar);
    }

    private void addGlobalElementDecls(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        XSNamedMap components = srcGrammar.getComponents(XSConstants.ELEMENT_DECLARATION);
        int len = components.getLength();
        XSElementDecl srcDecl, dstDecl;

        for (int i=0; i<len; i++) {
            srcDecl = (XSElementDecl) components.item(i);
            dstDecl = dstGrammar.getGlobalElementDecl(srcDecl.getName());
            if (dstDecl == null) {
                dstGrammar.addGlobalElementDecl(srcDecl);
            }
            else if (dstDecl != srcDecl){
            }
        }

        ObjectList componentsExt = srcGrammar.getComponentsExt(XSConstants.ELEMENT_DECLARATION);
        len = componentsExt.getLength();

        for (int i=0; i<len; i+= 2) {
            final String key = (String) componentsExt.item(i);
            final int index = key.indexOf(',');
            final String location = key.substring(0, index);
            final String name = key.substring(index + 1, key.length());

            srcDecl = (XSElementDecl)componentsExt.item(i+1);
            dstDecl = dstGrammar.getGlobalElementDecl(name, location);
            if ( dstDecl == null) {
                dstGrammar.addGlobalElementDecl(srcDecl, location);
            }
            else if (dstDecl != srcDecl){
            }
        }
    }

    private void addGlobalAttributeDecls(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        XSNamedMap components = srcGrammar.getComponents(XSConstants.ATTRIBUTE_DECLARATION);
        int len = components.getLength();
        XSAttributeDecl srcDecl, dstDecl;

        for (int i=0; i<len; i++) {
            srcDecl = (XSAttributeDecl) components.item(i);
            dstDecl = dstGrammar.getGlobalAttributeDecl(srcDecl.getName());
            if (dstDecl == null) {
                dstGrammar.addGlobalAttributeDecl(srcDecl);
            }
            else if (dstDecl != srcDecl && !fTolerateDuplicates) {
                reportSharingError(srcDecl.getNamespace(), srcDecl.getName());
            }
        }

        ObjectList componentsExt = srcGrammar.getComponentsExt(XSConstants.ATTRIBUTE_DECLARATION);
        len = componentsExt.getLength();

        for (int i=0; i<len; i+= 2) {
            final String key = (String) componentsExt.item(i);
            final int index = key.indexOf(',');
            final String location = key.substring(0, index);
            final String name = key.substring(index + 1, key.length());

            srcDecl = (XSAttributeDecl)componentsExt.item(i+1);
            dstDecl = dstGrammar.getGlobalAttributeDecl(name, location);
            if (dstDecl == null) {
                dstGrammar.addGlobalAttributeDecl(srcDecl, location);
            }
            else if (dstDecl != srcDecl) {
            }
        }
    }

    private void addGlobalAttributeGroupDecls(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        XSNamedMap components = srcGrammar.getComponents(XSConstants.ATTRIBUTE_GROUP);
        int len = components.getLength();
        XSAttributeGroupDecl srcDecl, dstDecl;

        for (int i=0; i<len; i++) {
            srcDecl = (XSAttributeGroupDecl) components.item(i);
            dstDecl = dstGrammar.getGlobalAttributeGroupDecl(srcDecl.getName());
            if (dstDecl == null) {
                dstGrammar.addGlobalAttributeGroupDecl(srcDecl);
            }
            else if (dstDecl != srcDecl && !fTolerateDuplicates) {
                reportSharingError(srcDecl.getNamespace(), srcDecl.getName());
            }
        }

        ObjectList componentsExt = srcGrammar.getComponentsExt(XSConstants.ATTRIBUTE_GROUP);
        len = componentsExt.getLength();

        for (int i=0; i<len; i+= 2) {
            final String key = (String) componentsExt.item(i);
            final int index = key.indexOf(',');
            final String location = key.substring(0, index);
            final String name = key.substring(index + 1, key.length());

            srcDecl = (XSAttributeGroupDecl)componentsExt.item(i+1);
            dstDecl = dstGrammar.getGlobalAttributeGroupDecl(name, location);
            if (dstDecl == null) {
                dstGrammar.addGlobalAttributeGroupDecl(srcDecl, location);
            }
            else if (dstDecl != srcDecl) {
            }
        }
    }

    private void addGlobalNotationDecls(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        XSNamedMap components = srcGrammar.getComponents(XSConstants.NOTATION_DECLARATION);
        int len = components.getLength();
        XSNotationDecl srcDecl, dstDecl;

        for (int i=0; i<len; i++) {
            srcDecl = (XSNotationDecl) components.item(i);
            dstDecl = dstGrammar.getGlobalNotationDecl(srcDecl.getName());
            if (dstDecl == null) {
                dstGrammar.addGlobalNotationDecl(srcDecl);
            }
            else if (dstDecl != srcDecl && !fTolerateDuplicates) {
                reportSharingError(srcDecl.getNamespace(), srcDecl.getName());
            }
        }

        ObjectList componentsExt = srcGrammar.getComponentsExt(XSConstants.NOTATION_DECLARATION);
        len = componentsExt.getLength();

        for (int i=0; i<len; i+= 2) {
            final String key = (String) componentsExt.item(i);
            final int index = key.indexOf(',');
            final String location = key.substring(0, index);
            final String name = key.substring(index + 1, key.length());

            srcDecl = (XSNotationDecl)componentsExt.item(i+1);
            dstDecl = dstGrammar.getGlobalNotationDecl(name, location);
            if (dstDecl == null) {
                dstGrammar.addGlobalNotationDecl(srcDecl, location);
            }
            else if (dstDecl != srcDecl) {
            }
        }
    }

    private void addGlobalGroupDecls(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        XSNamedMap components = srcGrammar.getComponents(XSConstants.MODEL_GROUP_DEFINITION);
        int len = components.getLength();
        XSGroupDecl srcDecl, dstDecl;

        for (int i=0; i<len; i++) {
            srcDecl = (XSGroupDecl) components.item(i);
            dstDecl = dstGrammar.getGlobalGroupDecl(srcDecl.getName());
            if (dstDecl == null) {
                dstGrammar.addGlobalGroupDecl(srcDecl);
            }
            else if (srcDecl != dstDecl && !fTolerateDuplicates) {
                reportSharingError(srcDecl.getNamespace(), srcDecl.getName());
            }
        }

        ObjectList componentsExt = srcGrammar.getComponentsExt(XSConstants.MODEL_GROUP_DEFINITION);
        len = componentsExt.getLength();

        for (int i=0; i<len; i+= 2) {
            final String key = (String) componentsExt.item(i);
            final int index = key.indexOf(',');
            final String location = key.substring(0, index);
            final String name = key.substring(index + 1, key.length());

            srcDecl = (XSGroupDecl)componentsExt.item(i+1);
            dstDecl = dstGrammar.getGlobalGroupDecl(name, location);
            if (dstDecl == null) {
                dstGrammar.addGlobalGroupDecl(srcDecl, location);
            }
            else if (dstDecl != srcDecl) {
            }
        }
    }

    private void addGlobalTypeDecls(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        XSNamedMap components = srcGrammar.getComponents(XSConstants.TYPE_DEFINITION);
        int len = components.getLength();
        XSTypeDefinition srcDecl, dstDecl;

        for (int i=0; i<len; i++) {
            srcDecl = (XSTypeDefinition) components.item(i);
            dstDecl = dstGrammar.getGlobalTypeDecl(srcDecl.getName());
            if (dstDecl == null) {
                dstGrammar.addGlobalTypeDecl(srcDecl);
            }
            else if (dstDecl != srcDecl && !fTolerateDuplicates) {
                reportSharingError(srcDecl.getNamespace(), srcDecl.getName());
            }
        }

        ObjectList componentsExt = srcGrammar.getComponentsExt(XSConstants.TYPE_DEFINITION);
        len = componentsExt.getLength();

        for (int i=0; i<len; i+= 2) {
            final String key = (String) componentsExt.item(i);
            final int index = key.indexOf(',');
            final String location = key.substring(0, index);
            final String name = key.substring(index + 1, key.length());

            srcDecl = (XSTypeDefinition)componentsExt.item(i+1);
            dstDecl = dstGrammar.getGlobalTypeDecl(name, location);
            if (dstDecl == null) {
                dstGrammar.addGlobalTypeDecl(srcDecl, location);
            }
            else if (dstDecl != srcDecl) {
            }
        }
    }

    private List<XSObject> expandComponents(XSObject[] components, Map<String, List<String>> dependencies) {
        List<XSObject> newComponents = new ArrayList<>();

        for (int i=0; i<components.length; i++) {
            if (!newComponents.contains(components[i])) {
                newComponents.add(components[i]);
            }
        }

        for (int i=0; i<newComponents.size(); i++) {
            final XSObject component = newComponents.get(i);
            expandRelatedComponents(component, newComponents, dependencies);
        }

        return newComponents;
    }

    @SuppressWarnings("fallthrough")
    private void expandRelatedComponents(XSObject component,List<XSObject>componentList, Map<String, List<String>> dependencies) {
        short componentType = component.getType();
        switch (componentType) {
        case XSConstants.TYPE_DEFINITION :
            expandRelatedTypeComponents((XSTypeDefinition) component, componentList, component.getNamespace(), dependencies);
            break;
        case XSConstants.ATTRIBUTE_DECLARATION :
            expandRelatedAttributeComponents((XSAttributeDeclaration) component, componentList, component.getNamespace(), dependencies);
            break;
        case XSConstants.ATTRIBUTE_GROUP :
            expandRelatedAttributeGroupComponents((XSAttributeGroupDefinition) component, componentList, component.getNamespace(), dependencies);
        case XSConstants.ELEMENT_DECLARATION :
            expandRelatedElementComponents((XSElementDeclaration) component, componentList, component.getNamespace(), dependencies);
            break;
        case XSConstants.MODEL_GROUP_DEFINITION :
            expandRelatedModelGroupDefinitionComponents((XSModelGroupDefinition) component, componentList, component.getNamespace(), dependencies);
        case XSConstants.ATTRIBUTE_USE :
        case XSConstants.NOTATION_DECLARATION :
        case XSConstants.IDENTITY_CONSTRAINT :
        default :
            break;
        }
    }

    private void expandRelatedAttributeComponents(XSAttributeDeclaration decl, List<XSObject> componentList, String namespace, Map<String, List<String>> dependencies) {
        addRelatedType(decl.getTypeDefinition(), componentList, namespace, dependencies);

        /*final XSComplexTypeDefinition enclosingType = decl.getEnclosingCTDefinition();
        if (enclosingType != null) {
            addRelatedType(enclosingType, componentList, namespace, dependencies);
        }*/
    }

    private void expandRelatedElementComponents(XSElementDeclaration decl, List<XSObject> componentList, String namespace, Map<String, List<String>> dependencies) {
        addRelatedType(decl.getTypeDefinition(), componentList, namespace, dependencies);

        /*final XSTypeDefinition enclosingType = decl.getEnclosingCTDefinition();
        if (enclosingType != null) {
            addRelatedType(enclosingType, componentList, namespace, dependencies);
        }*/

        final XSElementDeclaration subElemDecl = decl.getSubstitutionGroupAffiliation();
        if (subElemDecl != null) {
            addRelatedElement(subElemDecl, componentList, namespace, dependencies);
        }
    }

    private void expandRelatedTypeComponents(XSTypeDefinition type, List<XSObject> componentList, String namespace, Map<String, List<String>> dependencies) {
        if (type instanceof XSComplexTypeDecl) {
            expandRelatedComplexTypeComponents((XSComplexTypeDecl) type, componentList, namespace, dependencies);
        }
        else if (type instanceof XSSimpleTypeDecl) {
            expandRelatedSimpleTypeComponents((XSSimpleTypeDefinition) type, componentList, namespace, dependencies);
        }
    }

    private void expandRelatedModelGroupDefinitionComponents(XSModelGroupDefinition modelGroupDef, List<XSObject>componentList,
            String namespace, Map<String, List<String>> dependencies) {
        expandRelatedModelGroupComponents(modelGroupDef.getModelGroup(), componentList, namespace, dependencies);
    }

    private void expandRelatedAttributeGroupComponents(XSAttributeGroupDefinition attrGroup, List<XSObject> componentList
            , String namespace, Map<String, List<String>> dependencies) {
        expandRelatedAttributeUsesComponents(attrGroup.getAttributeUses(), componentList, namespace, dependencies);
    }

    private void expandRelatedComplexTypeComponents(XSComplexTypeDecl type, List<XSObject> componentList, String namespace, Map<String, List<String>> dependencies) {
        addRelatedType(type.getBaseType(), componentList, namespace, dependencies);
        expandRelatedAttributeUsesComponents(type.getAttributeUses(), componentList, namespace, dependencies);
        final XSParticle particle = type.getParticle();
        if (particle != null) {
            expandRelatedParticleComponents(particle, componentList, namespace, dependencies);
        }
    }

    private void expandRelatedSimpleTypeComponents(XSSimpleTypeDefinition type, List<XSObject> componentList, String namespace, Map<String, List<String>> dependencies) {
        final XSTypeDefinition baseType = type.getBaseType();
        if (baseType != null) {
            addRelatedType(baseType, componentList, namespace, dependencies);
        }

        final XSTypeDefinition itemType = type.getItemType();
        if (itemType != null) {
            addRelatedType(itemType, componentList, namespace, dependencies);
        }

        final XSTypeDefinition primitiveType = type.getPrimitiveType();
        if (primitiveType != null) {
            addRelatedType(primitiveType, componentList, namespace, dependencies);
        }

        final XSObjectList memberTypes = type.getMemberTypes();
        if (memberTypes.size() > 0) {
            for (int i=0; i<memberTypes.size(); i++) {
                addRelatedType((XSTypeDefinition)memberTypes.item(i), componentList, namespace, dependencies);
            }
        }
    }

    private void expandRelatedAttributeUsesComponents(XSObjectList attrUses, List<XSObject> componentList,
            String namespace, Map<String, List<String>> dependencies) {
        final int attrUseSize = (attrUses == null) ? 0 : attrUses.size();
        for (int i=0; i<attrUseSize; i++) {
            expandRelatedAttributeUseComponents((XSAttributeUse)attrUses.item(i), componentList, namespace, dependencies);
        }
    }

    private void expandRelatedAttributeUseComponents(XSAttributeUse component, List<XSObject> componentList,
            String namespace, Map<String, List<String>> dependencies) {
        addRelatedAttribute(component.getAttrDeclaration(), componentList, namespace, dependencies);
    }

    private void expandRelatedParticleComponents(XSParticle component, List<XSObject> componentList,
            String namespace, Map<String, List<String>> dependencies) {
        XSTerm term = component.getTerm();
        switch (term.getType()) {
        case XSConstants.ELEMENT_DECLARATION :
            addRelatedElement((XSElementDeclaration) term, componentList, namespace, dependencies);
            break;
        case XSConstants.MODEL_GROUP :
            expandRelatedModelGroupComponents((XSModelGroup) term, componentList, namespace, dependencies);
            break;
        default:
            break;
        }
    }

    private void expandRelatedModelGroupComponents(XSModelGroup modelGroup, List<XSObject> componentList,
            String namespace, Map<String, List<String>> dependencies) {
        XSObjectList particles = modelGroup.getParticles();
        final int length = (particles == null) ? 0 : particles.getLength();
        for (int i=0; i<length; i++) {
            expandRelatedParticleComponents((XSParticle)particles.item(i), componentList, namespace, dependencies);
        }
    }

    private void addRelatedType(XSTypeDefinition type, List<XSObject> componentList, String namespace, Map<String, List<String>> dependencies) {
        if (!type.getAnonymous()) {
            if (!SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(type.getNamespace())) { 
                if (!componentList.contains(type)) {
                    final List<String> importedNamespaces = findDependentNamespaces(namespace, dependencies);
                    addNamespaceDependency(namespace, type.getNamespace(), importedNamespaces);
                    componentList.add(type);
                }
            }
        }
        else {
            expandRelatedTypeComponents(type, componentList, namespace, dependencies);
        }
    }

    private void addRelatedElement(XSElementDeclaration decl, List<XSObject> componentList, String namespace, Map<String, List<String>> dependencies) {
        if (decl.getScope() == XSConstants.SCOPE_GLOBAL) {
            if (!componentList.contains(decl)) {
               List<String> importedNamespaces = findDependentNamespaces(namespace, dependencies);
                addNamespaceDependency(namespace, decl.getNamespace(), importedNamespaces);
                componentList.add(decl);
            }
        }
        else {
            expandRelatedElementComponents(decl, componentList, namespace, dependencies);
        }
    }

    private void addRelatedAttribute(XSAttributeDeclaration decl, List<XSObject> componentList, String namespace, Map<String, List<String>> dependencies) {
        if (decl.getScope() == XSConstants.SCOPE_GLOBAL) {
            if (!componentList.contains(decl)) {
                List<String> importedNamespaces = findDependentNamespaces(namespace, dependencies);
                addNamespaceDependency(namespace, decl.getNamespace(), importedNamespaces);
                componentList.add(decl);
            }
        }
        else {
            expandRelatedAttributeComponents(decl, componentList, namespace, dependencies);
        }
    }

    private void addGlobalComponents(List<XSObject> components, Map<String, List<String>> importDependencies) {
        final XSDDescription desc = new XSDDescription();
        final int size = components.size();

        for (int i=0; i<size; i++) {
            addGlobalComponent(components.get(i), desc);
        }
        updateImportDependencies(importDependencies);
    }

    private void addGlobalComponent(XSObject component, XSDDescription desc) {
        final String namespace = component.getNamespace();

        desc.setNamespace(namespace);
        final SchemaGrammar sg = getSchemaGrammar(desc);

        short componentType = component.getType();
        final String name = component.getName();

        switch (componentType) {
        case XSConstants.TYPE_DEFINITION :
            if (!((XSTypeDefinition) component).getAnonymous()) {
                if (sg.getGlobalTypeDecl(name) == null) {
                    sg.addGlobalTypeDecl((XSTypeDefinition) component);
                }
                if (sg.getGlobalTypeDecl(name, "") == null) {
                    sg.addGlobalTypeDecl((XSTypeDefinition) component, "");
                }
            }
            break;
        case XSConstants.ATTRIBUTE_DECLARATION :
            if (((XSAttributeDecl) component).getScope() == XSAttributeDecl.SCOPE_GLOBAL) {
                if (sg.getGlobalAttributeDecl(name) == null) {
                    sg.addGlobalAttributeDecl((XSAttributeDecl) component);
                }
                if (sg.getGlobalAttributeDecl(name, "") == null) {
                    sg.addGlobalAttributeDecl((XSAttributeDecl) component, "");
                }
            }
            break;
        case XSConstants.ATTRIBUTE_GROUP :
            if (sg.getGlobalAttributeDecl(name) == null) {
                sg.addGlobalAttributeGroupDecl((XSAttributeGroupDecl) component);
            }
            if (sg.getGlobalAttributeDecl(name, "") == null) {
                sg.addGlobalAttributeGroupDecl((XSAttributeGroupDecl) component, "");
            }
            break;
        case XSConstants.ELEMENT_DECLARATION :
            if (((XSElementDecl) component).getScope() == XSElementDecl.SCOPE_GLOBAL) {
                sg.addGlobalElementDeclAll((XSElementDecl) component);

                if (sg.getGlobalElementDecl(name) == null) {
                    sg.addGlobalElementDecl((XSElementDecl) component);
                }
                if (sg.getGlobalElementDecl(name, "") == null) {
                    sg.addGlobalElementDecl((XSElementDecl) component, "");
                }
            }
            break;
        case XSConstants.MODEL_GROUP_DEFINITION :
            if (sg.getGlobalGroupDecl(name) == null) {
                sg.addGlobalGroupDecl((XSGroupDecl) component);
            }
            if (sg.getGlobalGroupDecl(name, "") == null) {
                sg.addGlobalGroupDecl((XSGroupDecl) component, "");
            }
            break;
        case XSConstants.NOTATION_DECLARATION :
            if (sg.getGlobalNotationDecl(name) == null) {
                sg.addGlobalNotationDecl((XSNotationDecl) component);
            }
            if (sg.getGlobalNotationDecl(name, "") == null) {
                sg.addGlobalNotationDecl((XSNotationDecl) component, "");
            }
            break;
        case XSConstants.IDENTITY_CONSTRAINT :
        case XSConstants.ATTRIBUTE_USE :
        default :
            break;
        }
    }

    private void updateImportDependencies(Map<String, List<String>> table) {
        if (table == null) return;
        String namespace;
        List<String> importList;

        for(Map.Entry<String, List<String>> entry : table.entrySet()){
            namespace = entry.getKey();
            importList = entry.getValue();
            if (importList.size() > 0) {
                expandImportList(namespace, importList);
            }
        }
    }

    private void expandImportList(String namespace, List<String> namespaceList) {
        SchemaGrammar sg = fGrammarBucket.getGrammar(namespace);
        if (sg != null) {
            List<SchemaGrammar> isgs = sg.getImportedGrammars();
            if (isgs == null) {
                isgs = new ArrayList<>();
                addImportList(sg, isgs, namespaceList);
                sg.setImportedGrammars(isgs);
            }
            else {
                updateImportList(sg, isgs, namespaceList);
            }
        }
    }

    private void addImportList(SchemaGrammar sg, List<SchemaGrammar> importedGrammars, List<String> namespaceList) {
        final int size = namespaceList.size();
        SchemaGrammar isg;

        for (int i=0; i<size; i++) {
            isg = fGrammarBucket.getGrammar(namespaceList.get(i));
            if (isg != null) {
                importedGrammars.add(isg);
            }
            else {
            }
        }
    }

    private void updateImportList(SchemaGrammar sg, List<SchemaGrammar> importedGrammars,
            List<String> namespaceList) {
        final int size = namespaceList.size();
        SchemaGrammar isg;

        for (int i=0; i<size; i++) {
            isg = fGrammarBucket.getGrammar(namespaceList.get(i));
            if (isg != null) {
                if (!containedImportedGrammar(importedGrammars, isg)) {
                    importedGrammars.add(isg);
                }
            }
            else {
            }
        }
    }

    private boolean containedImportedGrammar(List<SchemaGrammar> importedGrammar, SchemaGrammar grammar) {
        final int size = importedGrammar.size();
        SchemaGrammar sg;

        for (int i=0; i<size; i++) {
            sg =  importedGrammar.get(i);
            if (null2EmptyString(sg.getTargetNamespace()).equals(null2EmptyString(grammar.getTargetNamespace()))) {
                return true;
            }
        }
        return false;
    }

    private SchemaGrammar getSchemaGrammar(XSDDescription desc) {
        SchemaGrammar sg = findGrammar(desc, fNamespaceGrowth);

        if (sg == null) {
            sg = new SchemaGrammar(desc.getNamespace(), desc.makeClone(), fSymbolTable);
            fGrammarBucket.putGrammar(sg);
        }
        else if (sg.isImmutable()){
            sg = createGrammarFrom(sg);
        }

        return sg;
    }

    private List<String> findDependentNamespaces(String namespace,
            Map<String, List<String>> table) {
        final String ns = null2EmptyString(namespace);
        List<String> namespaceList = getFromMap(table, ns);

        if (namespaceList == null) {
            namespaceList = new ArrayList<>();
            table.put(ns, namespaceList);
        }

        return namespaceList;
    }

    private void addNamespaceDependency(String namespace1, String namespace2, List<String> list) {
        final String ns1 = null2EmptyString(namespace1);
        final String ns2 = null2EmptyString(namespace2);
        if (!ns1.equals(ns2)) {
            if (!list.contains(ns2)) {
                list.add(ns2);
            }
        }
    }

    private void reportSharingError(String namespace, String name) {
        final String qName = (namespace == null)
            ? "," + name : namespace + "," + name;

        reportSchemaError("sch-props-correct.2", new Object [] {qName}, null);
    }


    private void createTraversers() {
        fAttributeChecker = new XSAttributeChecker(this);
        fAttributeGroupTraverser = new XSDAttributeGroupTraverser(this, fAttributeChecker);
        fAttributeTraverser = new XSDAttributeTraverser(this, fAttributeChecker);
        fComplexTypeTraverser = new XSDComplexTypeTraverser(this, fAttributeChecker);
        fElementTraverser = new XSDElementTraverser(this, fAttributeChecker);
        fGroupTraverser = new XSDGroupTraverser(this, fAttributeChecker);
        fKeyrefTraverser = new XSDKeyrefTraverser(this, fAttributeChecker);
        fNotationTraverser = new XSDNotationTraverser(this, fAttributeChecker);
        fSimpleTypeTraverser = new XSDSimpleTypeTraverser(this, fAttributeChecker);
        fUniqueOrKeyTraverser = new XSDUniqueOrKeyTraverser(this, fAttributeChecker);
        fWildCardTraverser = new XSDWildcardTraverser(this, fAttributeChecker);
    } 

    void prepareForParse() {
        fTraversed.clear();
        fDoc2SystemId.clear();
        fHiddenNodes.clear();
        fLastSchemaWasDuplicate = false;
    }

    void prepareForTraverse() {
        if (!registryEmpty) {
        fUnparsedAttributeRegistry.clear();
        fUnparsedAttributeGroupRegistry.clear();
        fUnparsedElementRegistry.clear();
        fUnparsedGroupRegistry.clear();
        fUnparsedIdentityConstraintRegistry.clear();
        fUnparsedNotationRegistry.clear();
        fUnparsedTypeRegistry.clear();

        fUnparsedAttributeRegistrySub.clear();
        fUnparsedAttributeGroupRegistrySub.clear();
        fUnparsedElementRegistrySub.clear();
        fUnparsedGroupRegistrySub.clear();
        fUnparsedIdentityConstraintRegistrySub.clear();
        fUnparsedNotationRegistrySub.clear();
        fUnparsedTypeRegistrySub.clear();
        }

        for (int i=1; i<= TYPEDECL_TYPE; i++) {
            if (fUnparsedRegistriesExt[i] != null)
                fUnparsedRegistriesExt[i].clear();
        }

        fDependencyMap.clear();
        fDoc2XSDocumentMap.clear();
        if (fRedefine2XSDMap != null) fRedefine2XSDMap.clear();
        if (fRedefine2NSSupport != null) fRedefine2NSSupport.clear();
        fAllTNSs.clear();
        fImportMap.clear();
        fRoot = null;

        for (int i = 0; i < fLocalElemStackPos; i++) {
            fParticle[i] = null;
            fLocalElementDecl[i] = null;
            fLocalElementDecl_schema[i] = null;
            fLocalElemNamespaceContext[i] = null;
        }
        fLocalElemStackPos = 0;

        for (int i = 0; i < fKeyrefStackPos; i++) {
            fKeyrefs[i] = null;
            fKeyrefElems[i] = null;
            fKeyrefNamespaceContext[i] = null;
            fKeyrefsMapXSDocumentInfo[i] = null;
        }
        fKeyrefStackPos = 0;

        if (fAttributeChecker == null) {
            createTraversers();
        }

        Locale locale = fErrorReporter.getLocale();
        fAttributeChecker.reset(fSymbolTable);
        fAttributeGroupTraverser.reset(fSymbolTable, fValidateAnnotations, locale);
        fAttributeTraverser.reset(fSymbolTable, fValidateAnnotations, locale);
        fComplexTypeTraverser.reset(fSymbolTable, fValidateAnnotations, locale);
        fElementTraverser.reset(fSymbolTable, fValidateAnnotations, locale);
        fGroupTraverser.reset(fSymbolTable, fValidateAnnotations, locale);
        fKeyrefTraverser.reset(fSymbolTable, fValidateAnnotations, locale);
        fNotationTraverser.reset(fSymbolTable, fValidateAnnotations, locale);
        fSimpleTypeTraverser.reset(fSymbolTable, fValidateAnnotations, locale);
        fUniqueOrKeyTraverser.reset(fSymbolTable, fValidateAnnotations, locale);
        fWildCardTraverser.reset(fSymbolTable, fValidateAnnotations, locale);

        fRedefinedRestrictedAttributeGroupRegistry.clear();
        fRedefinedRestrictedGroupRegistry.clear();

        fGlobalAttrDecls.clear();
        fGlobalAttrGrpDecls.clear();
        fGlobalElemDecls.clear();
        fGlobalGroupDecls.clear();
        fGlobalNotationDecls.clear();
        fGlobalIDConstraintDecls.clear();
        fGlobalTypeDecls.clear();
    }
    public void setDeclPool (XSDeclarationPool declPool){
        fDeclPool = declPool;
    }
    public void setDVFactory(SchemaDVFactory dvFactory){
        fDVFactory = dvFactory;
    }
    public SchemaDVFactory getDVFactory(){
        return fDVFactory;
    }

    public void reset(XMLComponentManager componentManager) {

        fSymbolTable = (SymbolTable) componentManager.getProperty(SYMBOL_TABLE);

        fSecurityManager = (XMLSecurityManager) componentManager.getProperty(SECURITY_MANAGER, null);

        fEntityManager = (XMLEntityManager) componentManager.getProperty(ENTITY_MANAGER);

        XMLEntityResolver er = (XMLEntityResolver)componentManager.getProperty(ENTITY_RESOLVER);
        if (er != null)
            fSchemaParser.setEntityResolver(er);

        fErrorReporter = (XMLErrorReporter) componentManager.getProperty(ERROR_REPORTER);
        fErrorHandler = fErrorReporter.getErrorHandler();
        fLocale = fErrorReporter.getLocale();

        fValidateAnnotations = componentManager.getFeature(VALIDATE_ANNOTATIONS, false);
        fHonourAllSchemaLocations = componentManager.getFeature(HONOUR_ALL_SCHEMALOCATIONS, false);
        fNamespaceGrowth = componentManager.getFeature(NAMESPACE_GROWTH, false);
        fTolerateDuplicates = componentManager.getFeature(TOLERATE_DUPLICATES, false);

        try {
            if (fErrorHandler != fSchemaParser.getProperty(ERROR_HANDLER)) {
                fSchemaParser.setProperty(ERROR_HANDLER,
                        (fErrorHandler != null) ? fErrorHandler : new DefaultErrorHandler());
                if (fAnnotationValidator != null) {
                    fAnnotationValidator.setProperty(ERROR_HANDLER,
                            (fErrorHandler != null) ? fErrorHandler : new DefaultErrorHandler());
                }
            }
            if (fLocale != fSchemaParser.getProperty(LOCALE)) {
                fSchemaParser.setProperty(LOCALE, fLocale);
                if (fAnnotationValidator != null) {
                    fAnnotationValidator.setProperty(LOCALE, fLocale);
                }
            }
        }
        catch (XMLConfigurationException e) {}

        try {
            fSchemaParser.setFeature(CONTINUE_AFTER_FATAL_ERROR,
                    fErrorReporter.getFeature(CONTINUE_AFTER_FATAL_ERROR));
        } catch (XMLConfigurationException e) {}

        try {
            if (componentManager.getFeature(ALLOW_JAVA_ENCODINGS, false)) {
                fSchemaParser.setFeature(ALLOW_JAVA_ENCODINGS, true);
            }
        } catch (XMLConfigurationException e) {}

        try {
            if (componentManager.getFeature(STANDARD_URI_CONFORMANT_FEATURE, false)) {
                fSchemaParser.setFeature(STANDARD_URI_CONFORMANT_FEATURE, true);
            }
        } catch (XMLConfigurationException e) {}

        try {
            fGrammarPool = (XMLGrammarPool) componentManager.getProperty(XMLGRAMMAR_POOL);
        } catch (XMLConfigurationException e) {
            fGrammarPool = null;
        }

        try {
            if (componentManager.getFeature(DISALLOW_DOCTYPE, false)) {
                fSchemaParser.setFeature(DISALLOW_DOCTYPE, true);
            }
        } catch (XMLConfigurationException e) {}

        try {
            if (fSecurityManager != null) {
                fSchemaParser.setProperty(SECURITY_MANAGER, fSecurityManager);
            }
        } catch (XMLConfigurationException e) {}

        fSecurityPropertyMgr = (XMLSecurityPropertyManager)
                componentManager.getProperty(XML_SECURITY_PROPERTY_MANAGER);

        fSchemaParser.setProperty(XML_SECURITY_PROPERTY_MANAGER, fSecurityPropertyMgr);

        fAccessExternalDTD = fSecurityPropertyMgr.getValue(
                XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);
        fAccessExternalSchema = fSecurityPropertyMgr.getValue(
                XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_SCHEMA);

        fOverrideDefaultParser = componentManager.getFeature(JdkConstants.OVERRIDE_PARSER);
        fSchemaParser.setFeature(JdkConstants.OVERRIDE_PARSER, fOverrideDefaultParser);
        fEntityManager.setFeature(JdkConstants.OVERRIDE_PARSER, fOverrideDefaultParser);
        fUseCatalog = componentManager.getFeature(XMLConstants.USE_CATALOG);
        fSchemaParser.setFeature(XMLConstants.USE_CATALOG, fUseCatalog);
        fEntityManager.setFeature(XMLConstants.USE_CATALOG, fUseCatalog);

        fCatalogFile = (String)componentManager.getProperty(JdkXmlUtils.CATALOG_FILES);
        fDefer = (String)componentManager.getProperty(JdkXmlUtils.CATALOG_DEFER);
        fPrefer = (String)componentManager.getProperty(JdkXmlUtils.CATALOG_PREFER);
        fResolve = (String)componentManager.getProperty(JdkXmlUtils.CATALOG_RESOLVE);

        for( CatalogFeatures.Feature f : CatalogFeatures.Feature.values()) {
            fSchemaParser.setProperty(f.getPropertyName(),
                    componentManager.getProperty(f.getPropertyName()));
            fEntityManager.setProperty(f.getPropertyName(),
                    componentManager.getProperty(f.getPropertyName()));
        }

        fSchemaParser.setProperty(JdkConstants.CDATA_CHUNK_SIZE,
                componentManager.getProperty(JdkConstants.CDATA_CHUNK_SIZE));
        fEntityManager.setProperty(JdkConstants.CDATA_CHUNK_SIZE,
                componentManager.getProperty(JdkConstants.CDATA_CHUNK_SIZE));
    } 


    /**
     * Traverse all the deferred local elements. This method should be called
     * by traverseSchemas after we've done with all the global declarations.
     */
    void traverseLocalElements() {
        fElementTraverser.fDeferTraversingLocalElements = false;

        for (int i = 0; i < fLocalElemStackPos; i++) {
            Element currElem = fLocalElementDecl[i];
            XSDocumentInfo currSchema = fLocalElementDecl_schema[i];
            SchemaGrammar currGrammar = fGrammarBucket.getGrammar(currSchema.fTargetNamespace);
            fElementTraverser.traverseLocal (fParticle[i], currElem, currSchema,
                    currGrammar, fAllContext[i], fParent[i], fLocalElemNamespaceContext[i]);
            if (fParticle[i].fType == XSParticleDecl.PARTICLE_EMPTY) {
                XSModelGroupImpl group = null;
                if (fParent[i] instanceof XSComplexTypeDecl) {
                    XSParticle p = ((XSComplexTypeDecl)fParent[i]).getParticle();
                    if (p != null)
                        group = (XSModelGroupImpl)p.getTerm();
                }
                else {
                    group = ((XSGroupDecl)fParent[i]).fModelGroup;
                }
                if (group != null)
                    removeParticle(group, fParticle[i]);
            }
        }
    }

    private boolean removeParticle(XSModelGroupImpl group, XSParticleDecl particle) {
        XSParticleDecl member;
        for (int i = 0; i < group.fParticleCount; i++) {
            member = group.fParticles[i];
            if (member == particle) {
                for (int j = i; j < group.fParticleCount-1; j++)
                    group.fParticles[j] = group.fParticles[j+1];
                group.fParticleCount--;
                return true;
            }
            if (member.fType == XSParticleDecl.PARTICLE_MODELGROUP) {
                if (removeParticle((XSModelGroupImpl)member.fValue, particle))
                    return true;
            }
        }
        return false;
    }

    void fillInLocalElemInfo(Element elmDecl,
            XSDocumentInfo schemaDoc,
            int allContextFlags,
            XSObject parent,
            XSParticleDecl particle) {

        if (fParticle.length == fLocalElemStackPos) {
            XSParticleDecl[] newStackP = new XSParticleDecl[fLocalElemStackPos+INC_STACK_SIZE];
            System.arraycopy(fParticle, 0, newStackP, 0, fLocalElemStackPos);
            fParticle = newStackP;
            Element[] newStackE = new Element[fLocalElemStackPos+INC_STACK_SIZE];
            System.arraycopy(fLocalElementDecl, 0, newStackE, 0, fLocalElemStackPos);
            fLocalElementDecl = newStackE;
            XSDocumentInfo [] newStackE_schema = new XSDocumentInfo[fLocalElemStackPos+INC_STACK_SIZE];
            System.arraycopy(fLocalElementDecl_schema, 0, newStackE_schema, 0, fLocalElemStackPos);
            fLocalElementDecl_schema = newStackE_schema;
            int[] newStackI = new int[fLocalElemStackPos+INC_STACK_SIZE];
            System.arraycopy(fAllContext, 0, newStackI, 0, fLocalElemStackPos);
            fAllContext = newStackI;
            XSObject[] newStackC = new XSObject[fLocalElemStackPos+INC_STACK_SIZE];
            System.arraycopy(fParent, 0, newStackC, 0, fLocalElemStackPos);
            fParent = newStackC;
            String [][] newStackN = new String [fLocalElemStackPos+INC_STACK_SIZE][];
            System.arraycopy(fLocalElemNamespaceContext, 0, newStackN, 0, fLocalElemStackPos);
            fLocalElemNamespaceContext = newStackN;
        }

        fParticle[fLocalElemStackPos] = particle;
        fLocalElementDecl[fLocalElemStackPos] = elmDecl;
        fLocalElementDecl_schema[fLocalElemStackPos] = schemaDoc;
        fAllContext[fLocalElemStackPos] = allContextFlags;
        fParent[fLocalElemStackPos] = parent;
        fLocalElemNamespaceContext[fLocalElemStackPos++] = schemaDoc.fNamespaceSupport.getEffectiveLocalContext();
    } 

    /** This method makes sure that
     * if this component is being redefined that it lives in the
     * right schema.  It then renames the component correctly.  If it
     * detects a collision--a duplicate definition--then it complains.
     * Note that redefines must be handled carefully:  if there
     * is a collision, it may be because we're redefining something we know about
     * or because we've found the thing we're redefining.
     */
    void checkForDuplicateNames(String qName, int declType,
            Map<String,Element> registry, Map<String,XSDocumentInfo> registry_sub, Element currComp,
            XSDocumentInfo currSchema) {
        Object objElem = null;
        if ((objElem = registry.get(qName)) == null) {
            if (fNamespaceGrowth && !fTolerateDuplicates) {
                checkForDuplicateNames(qName, declType, currComp);
            }
            registry.put(qName, currComp);
            registry_sub.put(qName, currSchema);
        }
        else {
            Element collidingElem = (Element)objElem;
            XSDocumentInfo collidingElemSchema = registry_sub.get(qName);
            if (collidingElem == currComp) return;
            Element elemParent = null;
            XSDocumentInfo redefinedSchema = null;
            boolean collidedWithRedefine = true;
            if ((DOMUtil.getLocalName((elemParent = DOMUtil.getParent(collidingElem))).equals(SchemaSymbols.ELT_REDEFINE))) {
                redefinedSchema = (fRedefine2XSDMap != null)? fRedefine2XSDMap.get(elemParent) : null;
            }
            else if ((DOMUtil.getLocalName(DOMUtil.getParent(currComp)).equals(SchemaSymbols.ELT_REDEFINE))) {
                redefinedSchema = collidingElemSchema;
                collidedWithRedefine = false;
            }
            if (redefinedSchema != null) { 
                if(collidingElemSchema == currSchema){
                    reportSchemaError("sch-props-correct.2", new Object[]{qName}, currComp);
                    return;
                }

                String newName = qName.substring(qName.lastIndexOf(',')+1)+REDEF_IDENTIFIER;
                if (redefinedSchema == currSchema) { 
                    currComp.setAttribute(SchemaSymbols.ATT_NAME, newName);
                    if (currSchema.fTargetNamespace == null){
                        registry.put(","+newName, currComp);
                        registry_sub.put(","+newName, currSchema);
                    }
                    else{
                        registry.put(currSchema.fTargetNamespace+","+newName, currComp);
                        registry_sub.put(currSchema.fTargetNamespace+","+newName, currSchema);
                    }
                    if (currSchema.fTargetNamespace == null)
                        checkForDuplicateNames(","+newName, declType, registry, registry_sub, currComp, currSchema);
                    else
                        checkForDuplicateNames(currSchema.fTargetNamespace+","+newName, declType, registry, registry_sub, currComp, currSchema);
                }
                else { 
                    if (collidedWithRedefine) {
                        if (currSchema.fTargetNamespace == null)
                            checkForDuplicateNames(","+newName, declType, registry, registry_sub, currComp, currSchema);
                        else
                            checkForDuplicateNames(currSchema.fTargetNamespace+","+newName, declType, registry, registry_sub, currComp, currSchema);
                    }
                    else {
                        reportSchemaError("sch-props-correct.2", new Object [] {qName}, currComp);
                    }
                }
            }
            else {
                if (!fTolerateDuplicates) {
                    reportSchemaError("sch-props-correct.2", new Object []{qName}, currComp);
                } else if (fUnparsedRegistriesExt[declType] != null) {
                    if (fUnparsedRegistriesExt[declType].get(qName) == currSchema) {
                        reportSchemaError("sch-props-correct.2", new Object []{qName}, currComp);
                    }
                }
            }
        }

        if (fTolerateDuplicates) {
            if (fUnparsedRegistriesExt[declType] == null)
                fUnparsedRegistriesExt[declType] = new HashMap<>();
            fUnparsedRegistriesExt[declType].put(qName, currSchema);
        }

    } 

    void checkForDuplicateNames(String qName, int declType, Element currComp) {
        int namespaceEnd = qName.indexOf(',');
        String namespace = qName.substring(0, namespaceEnd);
        SchemaGrammar grammar = fGrammarBucket.getGrammar(emptyString2Null(namespace));

        if (grammar != null) {
            Object obj = getGlobalDeclFromGrammar(grammar, declType, qName.substring(namespaceEnd + 1));
            if (obj != null) {
                reportSchemaError("sch-props-correct.2", new Object []{qName}, currComp);
            }
        }
    }

    private void renameRedefiningComponents(XSDocumentInfo currSchema,
            Element child, String componentType,
            String oldName, String newName) {
        if (componentType.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
            Element grandKid = DOMUtil.getFirstChildElement(child);
            if (grandKid == null) {
                reportSchemaError("src-redefine.5.a.a", null, child);
            }
            else {
                String grandKidName = DOMUtil.getLocalName(grandKid);
                if (grandKidName.equals(SchemaSymbols.ELT_ANNOTATION)) {
                    grandKid = DOMUtil.getNextSiblingElement(grandKid);
                }
                if (grandKid == null) {
                    reportSchemaError("src-redefine.5.a.a", null, child);
                }
                else {
                    grandKidName = DOMUtil.getLocalName(grandKid);
                    if (!grandKidName.equals(SchemaSymbols.ELT_RESTRICTION)) {
                        reportSchemaError("src-redefine.5.a.b", new Object[]{grandKidName}, child);
                    }
                    else {
                        Object[] attrs = fAttributeChecker.checkAttributes(grandKid, false, currSchema);
                        QName derivedBase = (QName)attrs[XSAttributeChecker.ATTIDX_BASE];
                        if (derivedBase == null ||
                                derivedBase.uri != currSchema.fTargetNamespace ||
                                !derivedBase.localpart.equals(oldName)) {
                            reportSchemaError("src-redefine.5.a.c",
                                    new Object[]{grandKidName,
                                    (currSchema.fTargetNamespace==null?"":currSchema.fTargetNamespace)
                                    + "," + oldName},
                                    child);
                        }
                        else {
                            if (derivedBase.prefix != null && derivedBase.prefix.length() > 0)
                                grandKid.setAttribute( SchemaSymbols.ATT_BASE,
                                        derivedBase.prefix + ":" + newName );
                            else
                                grandKid.setAttribute( SchemaSymbols.ATT_BASE, newName );
                        }
                        fAttributeChecker.returnAttrArray(attrs, currSchema);
                    }
                }
            }
        }
        else if (componentType.equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
            Element grandKid = DOMUtil.getFirstChildElement(child);
            if (grandKid == null) {
                reportSchemaError("src-redefine.5.b.a", null, child);
            }
            else {
                if (DOMUtil.getLocalName(grandKid).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    grandKid = DOMUtil.getNextSiblingElement(grandKid);
                }
                if (grandKid == null) {
                    reportSchemaError("src-redefine.5.b.a", null, child);
                }
                else {
                    Element greatGrandKid = DOMUtil.getFirstChildElement(grandKid);
                    if (greatGrandKid == null) {
                        reportSchemaError("src-redefine.5.b.b", null, grandKid);
                    }
                    else {
                        String greatGrandKidName = DOMUtil.getLocalName(greatGrandKid);
                        if (greatGrandKidName.equals(SchemaSymbols.ELT_ANNOTATION)) {
                            greatGrandKid = DOMUtil.getNextSiblingElement(greatGrandKid);
                        }
                        if (greatGrandKid == null) {
                            reportSchemaError("src-redefine.5.b.b", null, grandKid);
                        }
                        else {
                            greatGrandKidName = DOMUtil.getLocalName(greatGrandKid);
                            if (!greatGrandKidName.equals(SchemaSymbols.ELT_RESTRICTION) &&
                                    !greatGrandKidName.equals(SchemaSymbols.ELT_EXTENSION)) {
                                reportSchemaError("src-redefine.5.b.c", new Object[]{greatGrandKidName}, greatGrandKid);
                            }
                            else {
                                Object[] attrs = fAttributeChecker.checkAttributes(greatGrandKid, false, currSchema);
                                QName derivedBase = (QName)attrs[XSAttributeChecker.ATTIDX_BASE];
                                if (derivedBase == null ||
                                        derivedBase.uri != currSchema.fTargetNamespace ||
                                        !derivedBase.localpart.equals(oldName)) {
                                    reportSchemaError("src-redefine.5.b.d",
                                            new Object[]{greatGrandKidName,
                                            (currSchema.fTargetNamespace==null?"":currSchema.fTargetNamespace)
                                            + "," + oldName},
                                            greatGrandKid);
                                }
                                else {
                                    if (derivedBase.prefix != null && derivedBase.prefix.length() > 0)
                                        greatGrandKid.setAttribute( SchemaSymbols.ATT_BASE,
                                                derivedBase.prefix + ":" + newName );
                                    else
                                        greatGrandKid.setAttribute( SchemaSymbols.ATT_BASE,
                                                newName );
                                }
                            }
                        }
                    }
                }
            }
        }
        else if (componentType.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
            String processedBaseName = (currSchema.fTargetNamespace == null)?
                    ","+oldName:currSchema.fTargetNamespace+","+oldName;
            int attGroupRefsCount = changeRedefineGroup(processedBaseName, componentType, newName, child, currSchema);
            if (attGroupRefsCount > 1) {
                reportSchemaError("src-redefine.7.1", new Object []{attGroupRefsCount}, child);
            }
            else if (attGroupRefsCount == 1) {
            }
            else
                if (currSchema.fTargetNamespace == null)
                    fRedefinedRestrictedAttributeGroupRegistry.put(processedBaseName, ","+newName);
                else
                    fRedefinedRestrictedAttributeGroupRegistry.put(processedBaseName, currSchema.fTargetNamespace+","+newName);
        }
        else if (componentType.equals(SchemaSymbols.ELT_GROUP)) {
            String processedBaseName = (currSchema.fTargetNamespace == null)?
                    ","+oldName:currSchema.fTargetNamespace+","+oldName;
            int groupRefsCount = changeRedefineGroup(processedBaseName, componentType, newName, child, currSchema);
            if (groupRefsCount > 1) {
                reportSchemaError("src-redefine.6.1.1", new Object []{groupRefsCount}, child);
            }
            else if (groupRefsCount == 1) {
            }
            else {
                if (currSchema.fTargetNamespace == null)
                    fRedefinedRestrictedGroupRegistry.put(processedBaseName, ","+newName);
                else
                    fRedefinedRestrictedGroupRegistry.put(processedBaseName, currSchema.fTargetNamespace+","+newName);
            }
        }
        else {
            reportSchemaError("Internal-Error", new Object [] {"could not handle this particular <redefine>; please submit your schemas and instance document in a bug report!"}, child);
        }
    } 

    private String findQName(String name, XSDocumentInfo schemaDoc) {
        SchemaNamespaceSupport currNSMap = schemaDoc.fNamespaceSupport;
        int colonPtr = name.indexOf(':');
        String prefix = XMLSymbols.EMPTY_STRING;
        if (colonPtr > 0)
            prefix = name.substring(0, colonPtr);
        String uri = currNSMap.getURI(fSymbolTable.addSymbol(prefix));
        String localpart = (colonPtr == 0)?name:name.substring(colonPtr+1);
        if (prefix == XMLSymbols.EMPTY_STRING && uri == null && schemaDoc.fIsChameleonSchema)
            uri = schemaDoc.fTargetNamespace;
        if (uri == null)
            return ","+localpart;
        return uri+","+localpart;
    } 

    private int changeRedefineGroup(String originalQName, String elementSought,
            String newName, Element curr, XSDocumentInfo schemaDoc) {
        int result = 0;
        for (Element child = DOMUtil.getFirstChildElement(curr);
        child != null; child = DOMUtil.getNextSiblingElement(child)) {
            String name = DOMUtil.getLocalName(child);
            if (!name.equals(elementSought))
                result += changeRedefineGroup(originalQName, elementSought, newName, child, schemaDoc);
            else {
                String ref = child.getAttribute( SchemaSymbols.ATT_REF );
                if (ref.length() != 0) {
                    String processedRef = findQName(ref, schemaDoc);
                    if (originalQName.equals(processedRef)) {
                        String prefix = XMLSymbols.EMPTY_STRING;
                        int colonptr = ref.indexOf(":");
                        if (colonptr > 0) {
                            prefix = ref.substring(0,colonptr);
                            child.setAttribute(SchemaSymbols.ATT_REF, prefix + ":" + newName);
                        }
                        else
                            child.setAttribute(SchemaSymbols.ATT_REF, newName);
                        result++;
                        if (elementSought.equals(SchemaSymbols.ELT_GROUP)) {
                            String minOccurs = child.getAttribute( SchemaSymbols.ATT_MINOCCURS );
                            String maxOccurs = child.getAttribute( SchemaSymbols.ATT_MAXOCCURS );
                            if (!((maxOccurs.length() == 0 || maxOccurs.equals("1"))
                                    && (minOccurs.length() == 0 || minOccurs.equals("1")))) {
                                reportSchemaError("src-redefine.6.1.2", new Object [] {ref}, child);
                            }
                        }
                    }
                } 
            }
        }
        return result;
    } 

    private XSDocumentInfo findXSDocumentForDecl(XSDocumentInfo currSchema,
            Element decl, XSDocumentInfo decl_Doc) {

        if (DEBUG_NODE_POOL) {
            System.out.println("DOCUMENT NS:" + currSchema.fTargetNamespace + " hashcode:" +
                    ((Object)currSchema.fSchemaElement).hashCode());
        }
        Object temp = decl_Doc;
        if (temp == null) {
            return null;
        }
        XSDocumentInfo declDocInfo = (XSDocumentInfo)temp;
        return declDocInfo;
        /*********
         Logic here is unnecessary after schema WG's recent decision to allow
         schema components from one document to refer to components of any other,
         so long as there's some include/import/redefine path amongst them.
         If they rver reverse this decision the code's right here though...  - neilg
          if((fDependencyMap.get(currSchema)).contains(declDocInfo)) {
          return declDocInfo;
          }
            return null;
            **********/
    } 

    private boolean nonAnnotationContent(Element elem) {
        for(Element child = DOMUtil.getFirstChildElement(elem); child != null;
                child = DOMUtil.getNextSiblingElement(child)) {
            if(!(DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION))) return true;
        }
        return false;
    } 

    private void setSchemasVisible(XSDocumentInfo startSchema) {
        if (DOMUtil.isHidden(startSchema.fSchemaElement, fHiddenNodes)) {
            DOMUtil.setVisible(startSchema.fSchemaElement, fHiddenNodes);
            List<XSDocumentInfo> dependingSchemas = fDependencyMap.get(startSchema);
            for (int i = 0; i < dependingSchemas.size(); i++) {
                setSchemasVisible(dependingSchemas.get(i));
            }
        }
    } 

    private SimpleLocator xl = new SimpleLocator();

    /**
     * Extract location information from an Element node, and create a
     * new SimpleLocator object from such information. Returning null means
     * no information can be retrieved from the element.
     */
    public SimpleLocator element2Locator(Element e) {
        if (!( e instanceof ElementImpl))
            return null;

        SimpleLocator l = new SimpleLocator();
        return element2Locator(e, l) ? l : null;
    }

    /**
     * Extract location information from an Element node, store such
     * information in the passed-in SimpleLocator object, then return
     * true. Returning false means can't extract or store such information.
     */
    public boolean element2Locator(Element e, SimpleLocator l) {
        if (l == null)
            return false;
        if (e instanceof ElementImpl) {
            ElementImpl ele = (ElementImpl)e;
            Document doc = ele.getOwnerDocument();
            String sid = fDoc2SystemId.get(DOMUtil.getRoot(doc));
            int line = ele.getLineNumber();
            int column = ele.getColumnNumber();
            l.setValues(sid, sid, line, column, ele.getCharacterOffset());
            return true;
        }
        return false;
    }

    private Element getElementFromMap(Map<String, Element> registry, String declKey) {
        if (registry == null) return null;
        return registry.get(declKey);
    }

    private XSDocumentInfo getDocInfoFromMap(Map<String, XSDocumentInfo> registry, String declKey) {
        if (registry == null) return null;
        return registry.get(declKey);
    }

    private List<String> getFromMap(Map<String, List<String>> registry, String key) {
        if (registry == null) return null;
        return registry.get(key);
    }

    void reportSchemaFatalError(String key, Object[] args, Element ele) {
        reportSchemaErr(key, args, ele, XMLErrorReporter.SEVERITY_FATAL_ERROR, null);
    }

    void reportSchemaError(String key, Object[] args, Element ele) {
        reportSchemaErr(key, args, ele, XMLErrorReporter.SEVERITY_ERROR, null);
    }

    void reportSchemaError(String key, Object[] args, Element ele, Exception exception) {
        reportSchemaErr(key, args, ele, XMLErrorReporter.SEVERITY_ERROR, exception);
    }

    void reportSchemaWarning(String key, Object[] args, Element ele) {
        reportSchemaErr(key, args, ele, XMLErrorReporter.SEVERITY_WARNING, null);
    }

    void reportSchemaWarning(String key, Object[] args, Element ele, Exception exception) {
        reportSchemaErr(key, args, ele, XMLErrorReporter.SEVERITY_WARNING, exception);
    }

    void reportSchemaErr(String key, Object[] args, Element ele, short type, Exception exception) {
        if (element2Locator(ele, xl)) {
            fErrorReporter.reportError(xl, XSMessageFormatter.SCHEMA_DOMAIN,
                    key, args, type, exception);
        }
        else {
            fErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN,
                    key, args, type, exception);
        }
    }

    /**
     * Grammar pool used for validating annotations. This will return all of the
     * grammars from the grammar bucket. It will also return an object for the
     * schema for schemas which will contain at least the relevant declarations
     * for annotations.
     */
    private static class XSAnnotationGrammarPool implements XMLGrammarPool {

        private XSGrammarBucket fGrammarBucket;
        private Grammar [] fInitialGrammarSet;

        public Grammar[] retrieveInitialGrammarSet(String grammarType) {
            if (grammarType == XMLGrammarDescription.XML_SCHEMA) {
                if (fInitialGrammarSet == null) {
                    if (fGrammarBucket == null) {
                        fInitialGrammarSet = new Grammar [] {SchemaGrammar.Schema4Annotations.INSTANCE};
                    }
                    else {
                        SchemaGrammar [] schemaGrammars = fGrammarBucket.getGrammars();
                        /**
                         * If the grammar bucket already contains the schema for schemas
                         * then we already have the definitions for the parts relevant
                         * to annotations.
                         */
                        for (int i = 0; i < schemaGrammars.length; ++i) {
                            if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(schemaGrammars[i].getTargetNamespace())) {
                                fInitialGrammarSet = schemaGrammars;
                                return fInitialGrammarSet;
                            }
                        }
                        Grammar [] grammars = new Grammar[schemaGrammars.length + 1];
                        System.arraycopy(schemaGrammars, 0, grammars, 0, schemaGrammars.length);
                        grammars[grammars.length - 1] = SchemaGrammar.Schema4Annotations.INSTANCE;
                        fInitialGrammarSet = grammars;
                    }
                }
                return fInitialGrammarSet;
            }
            return new Grammar[0];
        }

        public void cacheGrammars(String grammarType, Grammar[] grammars) {

        }

        public Grammar retrieveGrammar(XMLGrammarDescription desc) {
            if (desc.getGrammarType() == XMLGrammarDescription.XML_SCHEMA) {
                final String tns = ((XMLSchemaDescription) desc).getTargetNamespace();
                if (fGrammarBucket != null) {
                    Grammar grammar = fGrammarBucket.getGrammar(tns);
                    if (grammar != null) {
                        return grammar;
                    }
                }
                if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(tns)) {
                    return SchemaGrammar.Schema4Annotations.INSTANCE;
                }
            }
            return null;
        }

        public void refreshGrammars(XSGrammarBucket gBucket) {
            fGrammarBucket = gBucket;
            fInitialGrammarSet = null;
        }

        public void lockPool() {}

        public void unlockPool() {}

        public void clear() {}
    }

    /**
     * used to identify a reference to a schema document
     * if the same document is referenced twice with the same key, then
     * we only need to parse it once.
     *
     * When 2 XSDKey's are compared, the following table can be used to
     * determine whether they are equal:
     *      inc     red     imp     pre     ins
     * inc  N/L      ?      N/L     N/L     N/L
     * red   ?      N/L      ?       ?       ?
     * imp  N/L      ?      N/P     N/P     N/P
     * pre  N/L      ?      N/P     N/P     N/P
     * ins  N/L      ?      N/P     N/P     N/P
     *
     * Where: N/L: duplicate when they have the same namespace and location.
     *         ? : not clear from the spec.
     *             REVISIT: to simplify the process, also considering
     *             it's very rare, we treat them as not duplicate.
     *        N/P: not possible. imp/pre/ins are referenced by namespace.
     *             when the first time we encounter a schema document for a
     *             namespace, we create a grammar and store it in the grammar
     *             bucket. when we see another reference to the same namespace,
     *             we first check whether a grammar with the same namespace is
     *             already in the bucket, which is true in this case, so we
     *             won't create another XSDKey.
     *
     * Conclusion from the table: two XSDKey's are duplicate only when all of
     * the following are true:
     * 1. They are both "redefine", or neither is "redefine";
     * 2. They have the same namespace;
     * 3. They have the same non-null location.
     *
     * About 3: if neither has a non-null location, then it's the case where
     * 2 input streams are provided, but no system ID is provided. We can't tell
     * whether the 2 streams have the same content, so we treat them as not
     * duplicate.
     */
    private static class XSDKey {
        String systemId;
        short  referType;
        String referNS;

        XSDKey(String systemId, short referType, String referNS) {
            this.systemId = systemId;
            this.referType = referType;
            this.referNS = referNS;
        }

        public int hashCode() {
            return referNS == null ? 0 : referNS.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof XSDKey)) {
                return false;
            }
            XSDKey key = (XSDKey)obj;

            /** if (referType == XSDDescription.CONTEXT_REDEFINE ||
                    key.referType == XSDDescription.CONTEXT_REDEFINE) {
                if (referType != key.referType)
                    return false;
            }**/

            if (referNS != key.referNS)
                return false;

            if (systemId == null || !systemId.equals(key.systemId)) {
                return false;
            }

            return true;
        }
    }

    private static final class SAX2XNIUtil extends ErrorHandlerWrapper {
        public static XMLParseException createXMLParseException0(SAXParseException exception) {
            return createXMLParseException(exception);
        }
        public static XNIException createXNIException0(SAXException exception) {
            return createXNIException(exception);
        }
    }

    /**
     * @param state
     */
    public void setGenerateSyntheticAnnotations(boolean state) {
        fSchemaParser.setFeature(GENERATE_SYNTHETIC_ANNOTATIONS, state);
    }

} 
