/*
 * Copyright (c) 2009, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl ;

import com.sun.org.apache.xerces.internal.impl.io.ASCIIReader;
import com.sun.org.apache.xerces.internal.impl.io.UCSReader;
import com.sun.org.apache.xerces.internal.impl.io.UTF16Reader;
import com.sun.org.apache.xerces.internal.impl.io.UTF8Reader;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.impl.validation.ValidationManager;
import com.sun.org.apache.xerces.internal.util.*;
import com.sun.org.apache.xerces.internal.util.URI;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.*;
import com.sun.xml.internal.stream.Entity;
import com.sun.xml.internal.stream.StaxEntityResolverWrapper;
import com.sun.xml.internal.stream.StaxXMLInputSource;
import com.sun.xml.internal.stream.XMLEntityStorage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogFeatures.Feature;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Source;
import jdk.xml.internal.JdkCatalog;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkProperty;
import jdk.xml.internal.JdkXmlUtils;
import jdk.xml.internal.SecuritySupport;
import jdk.xml.internal.XMLLimitAnalyzer;
import jdk.xml.internal.XMLSecurityManager;
import jdk.xml.internal.XMLSecurityManager.Limit;
import org.xml.sax.InputSource;


/**
 * Will keep track of current entity.
 *
 * The entity manager handles the registration of general and parameter
 * entities; resolves entities; and starts entities. The entity manager
 * is a central component in a standard parser configuration and this
 * class works directly with the entity scanner to manage the underlying
 * xni.
 * <p>
 * This component requires the following features and properties from the
 * component manager that uses it:
 * <ul>
 *  <li>http:
 *  <li>http:
 *  <li>http:
 *  <li>http:
 *  <li>http:
 *  <li>http:
 *  <li>http:
 * </ul>
 *
 *
 * @author Andy Clark, IBM
 * @author Arnaud  Le Hors, IBM
 * @author K.Venugopal SUN Microsystems
 * @author Neeraj Bajaj SUN Microsystems
 * @author Sunitha Reddy SUN Microsystems
 * @LastModified: Feb 2024
 */
public class XMLEntityManager implements XMLComponent, XMLEntityResolver {


    /** Default buffer size (2048). */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /** Default buffer size before we've finished with the XMLDecl:  */
    public static final int DEFAULT_XMLDECL_BUFFER_SIZE = 64;

    /** Default internal entity buffer size (1024). */
    public static final int DEFAULT_INTERNAL_BUFFER_SIZE = 1024;


    /** Feature identifier: validation. */
    protected static final String VALIDATION =
            Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

    /**
     * standard uri conformant (strict uri).
     * http:
     */
    protected boolean fStrictURI;


    /** Feature identifier: external general entities. */
    protected static final String EXTERNAL_GENERAL_ENTITIES =
            Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE;

    /** Feature identifier: external parameter entities. */
    protected static final String EXTERNAL_PARAMETER_ENTITIES =
            Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE;

    /** Feature identifier: allow Java encodings. */
    protected static final String ALLOW_JAVA_ENCODINGS =
            Constants.XERCES_FEATURE_PREFIX + Constants.ALLOW_JAVA_ENCODINGS_FEATURE;

    /** Feature identifier: warn on duplicate EntityDef */
    protected static final String WARN_ON_DUPLICATE_ENTITYDEF =
            Constants.XERCES_FEATURE_PREFIX +Constants.WARN_ON_DUPLICATE_ENTITYDEF_FEATURE;

    /** Feature identifier: load external DTD. */
    protected static final String LOAD_EXTERNAL_DTD =
            Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE;


    /** Property identifier: symbol table. */
    protected static final String SYMBOL_TABLE =
            Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property identifier: error reporter. */
    protected static final String ERROR_REPORTER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Feature identifier: standard uri conformant */
    protected static final String STANDARD_URI_CONFORMANT =
            Constants.XERCES_FEATURE_PREFIX +Constants.STANDARD_URI_CONFORMANT_FEATURE;

    /** Property identifier: entity resolver. */
    protected static final String ENTITY_RESOLVER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

    protected static final String STAX_ENTITY_RESOLVER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.STAX_ENTITY_RESOLVER_PROPERTY;

    protected static final String VALIDATION_MANAGER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.VALIDATION_MANAGER_PROPERTY;

    /** property identifier: buffer size. */
    protected static final String BUFFER_SIZE =
            Constants.XERCES_PROPERTY_PREFIX + Constants.BUFFER_SIZE_PROPERTY;

    /** property identifier: security manager. */
    protected static final String SECURITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY;

    protected static final String PARSER_SETTINGS =
        Constants.XERCES_FEATURE_PREFIX + Constants.PARSER_SETTINGS;

    /** Property identifier: Security property manager. */
    private static final String XML_SECURITY_PROPERTY_MANAGER =
            JdkConstants.XML_SECURITY_PROPERTY_MANAGER;

    /** access external dtd: file protocol */
    static final String EXTERNAL_ACCESS_DEFAULT = JdkConstants.EXTERNAL_ACCESS_DEFAULT;


    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
                VALIDATION,
                EXTERNAL_GENERAL_ENTITIES,
                EXTERNAL_PARAMETER_ENTITIES,
                ALLOW_JAVA_ENCODINGS,
                WARN_ON_DUPLICATE_ENTITYDEF,
                STANDARD_URI_CONFORMANT,
                XMLConstants.USE_CATALOG
    };

    /** Feature defaults. */
    private static final Boolean[] FEATURE_DEFAULTS = {
                null,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                JdkXmlUtils.USE_CATALOG_DEFAULT
    };

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
                SYMBOL_TABLE,
                ERROR_REPORTER,
                ENTITY_RESOLVER,
                VALIDATION_MANAGER,
                BUFFER_SIZE,
                SECURITY_MANAGER,
                XML_SECURITY_PROPERTY_MANAGER,
                JdkXmlUtils.CATALOG_DEFER,
                JdkXmlUtils.CATALOG_FILES,
                JdkXmlUtils.CATALOG_PREFER,
                JdkXmlUtils.CATALOG_RESOLVE,
                JdkConstants.CDATA_CHUNK_SIZE
    };

    /** Property defaults. */
    private static final Object[] PROPERTY_DEFAULTS = {
                null,
                null,
                null,
                null,
                DEFAULT_BUFFER_SIZE,
                null,
                null,
                null,
                null,
                null,
                null,
                JdkConstants.CDATA_CHUNK_SIZE_DEFAULT
    };

    private static final String XMLEntity = "[xml]".intern();
    private static final String DTDEntity = "[dtd]".intern();


    /**
     * Debug printing of buffer. This debugging flag works best when you
     * resize the DEFAULT_BUFFER_SIZE down to something reasonable like
     * 64 characters.
     */
    private static final boolean DEBUG_BUFFER = false;

    /** warn on duplicate Entity declaration.
     *  http:
     */
    protected boolean fWarnDuplicateEntityDef;

    /** Debug some basic entities. */
    private static final boolean DEBUG_ENTITIES = false;

    /** Debug switching readers for encodings. */
    private static final boolean DEBUG_ENCODINGS = false;



    /**
     * Validation. This feature identifier is:
     * http:
     */
    protected boolean fValidation;

    /**
     * External general entities. This feature identifier is:
     * http:
     */
    protected boolean fExternalGeneralEntities;

    /**
     * External parameter entities. This feature identifier is:
     * http:
     */
    protected boolean fExternalParameterEntities;

    /**
     * Allow Java encoding names. This feature identifier is:
     * http:
     */
    protected boolean fAllowJavaEncodings = true ;

    /** Load external DTD. */
    protected boolean fLoadExternalDTD = true;


    /**
     * Symbol table. This property identifier is:
     * http:
     */
    protected SymbolTable fSymbolTable;

    /**
     * Error reporter. This property identifier is:
     * http:
     */
    protected XMLErrorReporter fErrorReporter;

    /**
     * Entity resolver. This property identifier is:
     * http:
     */
    protected XMLEntityResolver fEntityResolver;

    /** Stax Entity Resolver. This property identifier is XMLInputFactory.ENTITY_RESOLVER */

    protected StaxEntityResolverWrapper fStaxEntityResolver;

    /** Property Manager. This is used from Stax */
    protected PropertyManager fPropertyManager ;

    /** StAX properties */
    boolean fSupportDTD = true;
    boolean fReplaceEntityReferences = true;
    boolean fSupportExternalEntities = true;

    /** used to restrict external access */
    protected String fAccessExternalDTD = EXTERNAL_ACCESS_DEFAULT;


    /**
     * Validation manager. This property identifier is:
     * http:
     */
    protected ValidationManager fValidationManager;


    /**
     * Buffer size. We get this value from a property. The default size
     * is used if the input buffer size property is not specified.
     * REVISIT: do we need a property for internal entity buffer size?
     */
    protected int fBufferSize = DEFAULT_BUFFER_SIZE;

    /** Security Manager */
    protected XMLSecurityManager fSecurityManager = null;
    XMLSecurityPropertyManager fSecurityPropertyMgr;

    protected XMLLimitAnalyzer fLimitAnalyzer = null;

    protected int entityExpansionIndex;

    /**
     * True if the document entity is standalone. This should really
     * only be set by the document source (e.g. XMLDocumentScanner).
     */
    protected boolean fStandalone;

    protected boolean fInExternalSubset = false;


    /** Entity handler. */
    protected XMLEntityHandler fEntityHandler;

    /** Current entity scanner */
    protected XMLEntityScanner fEntityScanner ;

    /** XML 1.0 entity scanner. */
    protected XMLEntityScanner fXML10EntityScanner;

    /** XML 1.1 entity scanner. */
    protected XMLEntityScanner fXML11EntityScanner;

    /** count of entities expanded: */
    protected int fEntityExpansionCount = 0;


    /** Entities. */
    protected Map<String, Entity> fEntities = new HashMap<>();

    /** Entity stack. */
    protected Stack<Entity> fEntityStack = new Stack<>();

    /** Current entity. */
    protected Entity.ScannedEntity fCurrentEntity = null;

    /** identify if the InputSource is created by a resolver */
    boolean fISCreatedByResolver = false;


    protected XMLEntityStorage fEntityStorage ;

    protected final Object [] defaultEncoding = new Object[]{"UTF-8", null};



    /** Resource identifer. */
    private final XMLResourceIdentifierImpl fResourceIdentifier = new XMLResourceIdentifierImpl();

    /** Augmentations for entities. */
    private final Augmentations fEntityAugs = new AugmentationsImpl();

    /** indicate whether Catalog should be used for resolving external resources */
    private boolean fUseCatalog = true;
    CatalogFeatures fCatalogFeatures;
    CatalogResolver fCatalogResolver;
    CatalogResolver fDefCR;

    private String fCatalogFile;
    private String fDefer;
    private String fPrefer;
    private String fResolve;


    /**
     * If this constructor is used to create the object, reset() should be invoked on this object
     */
    public XMLEntityManager() {
        this(null, new XMLSecurityManager(true));
    }

    public XMLEntityManager(XMLSecurityPropertyManager securityPropertyMgr, XMLSecurityManager securityManager) {
        fSecurityManager = securityManager;
        fSecurityPropertyMgr = securityPropertyMgr;
        fEntityStorage = new XMLEntityStorage(this) ;
        setScannerVersion(Constants.XML_VERSION_1_0);
    }

    /** Default constructor. */
    public XMLEntityManager(PropertyManager propertyManager) {
        fPropertyManager = propertyManager ;
        fEntityStorage = new XMLEntityStorage(this) ;
        fEntityScanner = new XMLEntityScanner(propertyManager, this) ;
        reset(propertyManager);
    } 

    /**
     * Adds an internal entity declaration.
     * <p>
     * <strong>Note:</strong> This method ignores subsequent entity
     * declarations.
     * <p>
     * <strong>Note:</strong> The name should be a unique symbol. The
     * SymbolTable can be used for this purpose.
     *
     * @param name The name of the entity.
     * @param text The text of the entity.
     *
     * @see SymbolTable
     */
    public void addInternalEntity(String name, String text) {
        if (!fEntities.containsKey(name)) {
            Entity entity = new Entity.InternalEntity(name, text, fInExternalSubset);
            fEntities.put(name, entity);
        } else{
            if(fWarnDuplicateEntityDef){
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                        "MSG_DUPLICATE_ENTITY_DEFINITION",
                        new Object[]{ name },
                        XMLErrorReporter.SEVERITY_WARNING );
            }
        }

    } 

    /**
     * Adds an external entity declaration.
     * <p>
     * <strong>Note:</strong> This method ignores subsequent entity
     * declarations.
     * <p>
     * <strong>Note:</strong> The name should be a unique symbol. The
     * SymbolTable can be used for this purpose.
     *
     * @param name         The name of the entity.
     * @param publicId     The public identifier of the entity.
     * @param literalSystemId     The system identifier of the entity.
     * @param baseSystemId The base system identifier of the entity.
     *                     This is the system identifier of the entity
     *                     where <em>the entity being added</em> and
     *                     is used to expand the system identifier when
     *                     the system identifier is a relative URI.
     *                     When null the system identifier of the first
     *                     external entity on the stack is used instead.
     *
     * @see SymbolTable
     */
    public void addExternalEntity(String name,
            String publicId, String literalSystemId,
            String baseSystemId) throws IOException {
        if (!fEntities.containsKey(name)) {
            if (baseSystemId == null) {
                int size = fEntityStack.size();
                if (size == 0 && fCurrentEntity != null && fCurrentEntity.entityLocation != null) {
                    baseSystemId = fCurrentEntity.entityLocation.getExpandedSystemId();
                }
                for (int i = size - 1; i >= 0 ; i--) {
                    Entity.ScannedEntity externalEntity =
                            (Entity.ScannedEntity)fEntityStack.get(i);
                    if (externalEntity.entityLocation != null && externalEntity.entityLocation.getExpandedSystemId() != null) {
                        baseSystemId = externalEntity.entityLocation.getExpandedSystemId();
                        break;
                    }
                }
            }
            Entity entity = new Entity.ExternalEntity(name,
                    new XMLEntityDescriptionImpl(name, publicId, literalSystemId, baseSystemId,
                    expandSystemId(literalSystemId, baseSystemId, false)), null, fInExternalSubset);
            fEntities.put(name, entity);
        } else{
            if(fWarnDuplicateEntityDef){
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                        "MSG_DUPLICATE_ENTITY_DEFINITION",
                        new Object[]{ name },
                        XMLErrorReporter.SEVERITY_WARNING );
            }
        }

    } 


    /**
     * Adds an unparsed entity declaration.
     * <p>
     * <strong>Note:</strong> This method ignores subsequent entity
     * declarations.
     * <p>
     * <strong>Note:</strong> The name should be a unique symbol. The
     * SymbolTable can be used for this purpose.
     *
     * @param name     The name of the entity.
     * @param publicId The public identifier of the entity.
     * @param systemId The system identifier of the entity.
     * @param notation The name of the notation.
     *
     * @see SymbolTable
     */
    public void addUnparsedEntity(String name,
            String publicId, String systemId,
            String baseSystemId, String notation) {
        if (!fEntities.containsKey(name)) {
            Entity.ExternalEntity entity = new Entity.ExternalEntity(name,
                    new XMLEntityDescriptionImpl(name, publicId, systemId, baseSystemId, null),
                    notation, fInExternalSubset);
            fEntities.put(name, entity);
        } else{
            if(fWarnDuplicateEntityDef){
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                        "MSG_DUPLICATE_ENTITY_DEFINITION",
                        new Object[]{ name },
                        XMLErrorReporter.SEVERITY_WARNING );
            }
        }
    } 


    /** get the entity storage object from entity manager */
    public XMLEntityStorage getEntityStore(){
        return fEntityStorage ;
    }

    /** return the entity responsible for reading the entity */
    public XMLEntityScanner getEntityScanner(){
        if(fEntityScanner == null) {
            if(fXML10EntityScanner == null) {
                fXML10EntityScanner = new XMLEntityScanner();
            }
            fXML10EntityScanner.reset(fSymbolTable, this, fErrorReporter);
            fEntityScanner = fXML10EntityScanner;
        }
        return fEntityScanner;

    }

    public void setScannerVersion(short version) {

        if(version == Constants.XML_VERSION_1_0) {
            if(fXML10EntityScanner == null) {
                fXML10EntityScanner = new XMLEntityScanner();
            }
            fXML10EntityScanner.reset(fSymbolTable, this, fErrorReporter);
            fEntityScanner = fXML10EntityScanner;
            fEntityScanner.setCurrentEntity(fCurrentEntity);
        } else {
            if(fXML11EntityScanner == null) {
                fXML11EntityScanner = new XML11EntityScanner();
            }
            fXML11EntityScanner.reset(fSymbolTable, this, fErrorReporter);
            fEntityScanner = fXML11EntityScanner;
            fEntityScanner.setCurrentEntity(fCurrentEntity);
        }

    }

    /**
     * This method uses the passed-in XMLInputSource to make
     * fCurrentEntity usable for reading.
     *
     * @param reference flag to indicate whether the entity is an Entity Reference.
     * @param name  name of the entity (XML is it's the document entity)
     * @param xmlInputSource    the input source, with sufficient information
     *      to begin scanning characters.
     * @param literal        True if this entity is started within a
     *                       literal value.
     * @param isExternal    whether this entity should be treated as an internal or external entity.
     * @throws IOException  if anything can't be read
     *  XNIException    If any parser-specific goes wrong.
     * @return the encoding of the new entity or null if a character stream was employed
     */
    public String setupCurrentEntity(boolean reference, String name, XMLInputSource xmlInputSource,
            boolean literal, boolean isExternal)
            throws IOException, XNIException {

        final String publicId = xmlInputSource.getPublicId();
        String literalSystemId = xmlInputSource.getSystemId();
        String baseSystemId = xmlInputSource.getBaseSystemId();
        String encoding = xmlInputSource.getEncoding();
        final boolean encodingExternallySpecified = (encoding != null);
        Boolean isBigEndian = null;

        InputStream stream = null;
        Reader reader = xmlInputSource.getCharacterStream();

        String expandedSystemId = expandSystemId(literalSystemId, baseSystemId, fStrictURI);
        if (baseSystemId == null) {
            baseSystemId = expandedSystemId;
        }
        if (reader == null) {
            stream = xmlInputSource.getByteStream();
            if (stream == null) {
                @SuppressWarnings("deprecation")
                URL location = new URL(expandedSystemId);
                URLConnection connect = location.openConnection();
                if (!(connect instanceof HttpURLConnection)) {
                    if (expandedSystemId.startsWith("jrt:/java.xml")) {
                        stream = SecuritySupport.getInputStream(connect);
                    } else {
                        stream = connect.getInputStream();
                    }
                }
                else {
                    boolean followRedirects = true;

                    if (xmlInputSource instanceof HTTPInputSource) {
                        final HttpURLConnection urlConnection = (HttpURLConnection) connect;
                        final HTTPInputSource httpInputSource = (HTTPInputSource) xmlInputSource;

                        Iterator<Map.Entry<String, String>> propIter = httpInputSource.getHTTPRequestProperties();
                        while (propIter.hasNext()) {
                            Map.Entry<String, String> entry = propIter.next();
                            urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
                        }

                        followRedirects = httpInputSource.getFollowHTTPRedirects();
                        if (!followRedirects) {
                            urlConnection.setInstanceFollowRedirects(followRedirects);
                        }
                    }

                    stream = connect.getInputStream();


                    if (followRedirects) {
                        String redirect = connect.getURL().toString();
                        if (!redirect.equals(expandedSystemId)) {
                            literalSystemId = redirect;
                            expandedSystemId = redirect;
                        }
                    }
                }
            }

            RewindableInputStream rewindableStream = new RewindableInputStream(stream);
            stream = rewindableStream;

            if (encoding == null) {
                final byte[] b4 = new byte[4];
                int count = 0;
                for (; count<4; count++ ) {
                    b4[count] = (byte)rewindableStream.readAndBuffer();
                }
                if (count == 4) {
                    final EncodingInfo info = getEncodingInfo(b4, count);
                    encoding = info.autoDetectedEncoding;
                    final String readerEncoding = info.readerEncoding;
                    isBigEndian = info.isBigEndian;
                    stream.reset();
                    if (info.hasBOM) {
                        if (EncodingInfo.STR_UTF8.equals(readerEncoding)) {
                            stream.skip(3);
                        }
                        else if (EncodingInfo.STR_UTF16.equals(readerEncoding)) {
                            stream.skip(2);
                        }
                    }
                    reader = createReader(stream, readerEncoding, isBigEndian);
                } else {
                    reader = createReader(stream, encoding, isBigEndian);
                }
            }

            else {
                encoding = encoding.toUpperCase(Locale.ENGLISH);

                if (EncodingInfo.STR_UTF8.equals(encoding)) {
                    final int[] b3 = new int[3];
                    int count = 0;
                    for (; count < 3; ++count) {
                        b3[count] = rewindableStream.readAndBuffer();
                        if (b3[count] == -1)
                            break;
                    }
                    if (count == 3) {
                        if (b3[0] != 0xEF || b3[1] != 0xBB || b3[2] != 0xBF) {
                            stream.reset();
                        }
                    } else {
                        stream.reset();
                    }
                }
                else if (EncodingInfo.STR_UTF16.equals(encoding)) {
                    final int[] b4 = new int[4];
                    int count = 0;
                    for (; count < 4; ++count) {
                        b4[count] = rewindableStream.readAndBuffer();
                        if (b4[count] == -1)
                            break;
                    }
                    stream.reset();
                    if (count >= 2) {
                        final int b0 = b4[0];
                        final int b1 = b4[1];
                        if (b0 == 0xFE && b1 == 0xFF) {
                            isBigEndian = Boolean.TRUE;
                            stream.skip(2);
                        }
                        else if (b0 == 0xFF && b1 == 0xFE) {
                            isBigEndian = Boolean.FALSE;
                            stream.skip(2);
                        }
                        else if (count == 4) {
                            final int b2 = b4[2];
                            final int b3 = b4[3];
                            if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x3F) {
                                isBigEndian = Boolean.TRUE;
                            }
                            if (b0 == 0x3C && b1 == 0x00 && b2 == 0x3F && b3 == 0x00) {
                                isBigEndian = Boolean.FALSE;
                            }
                        }
                    }
                }
                else if (EncodingInfo.STR_UCS4.equals(encoding)) {
                    final int[] b4 = new int[4];
                    int count = 0;
                    for (; count < 4; ++count) {
                        b4[count] = rewindableStream.readAndBuffer();
                        if (b4[count] == -1)
                            break;
                    }
                    stream.reset();

                    if (count == 4) {
                        if (b4[0] == 0x00 && b4[1] == 0x00 && b4[2] == 0x00 && b4[3] == 0x3C) {
                            isBigEndian = Boolean.TRUE;
                        }
                        else if (b4[0] == 0x3C && b4[1] == 0x00 && b4[2] == 0x00 && b4[3] == 0x00) {
                            isBigEndian = Boolean.FALSE;
                        }
                    }
                }
                else if (EncodingInfo.STR_UCS2.equals(encoding)) {
                    final int[] b4 = new int[4];
                    int count = 0;
                    for (; count < 4; ++count) {
                        b4[count] = rewindableStream.readAndBuffer();
                        if (b4[count] == -1)
                            break;
                    }
                    stream.reset();

                    if (count == 4) {
                        if (b4[0] == 0x00 && b4[1] == 0x3C && b4[2] == 0x00 && b4[3] == 0x3F) {
                            isBigEndian = Boolean.TRUE;
                        }
                        else if (b4[0] == 0x3C && b4[1] == 0x00 && b4[2] == 0x3F && b4[3] == 0x00) {
                            isBigEndian = Boolean.FALSE;
                        }
                    }
                }

                reader = createReader(stream, encoding, isBigEndian);
            }

            if (DEBUG_ENCODINGS) {
                System.out.println("$$$ no longer wrapping reader in OneCharReader");
            }
        }

        fReaderStack.push(reader);

        if (fCurrentEntity != null) {
            fEntityStack.push(fCurrentEntity);
        }

        /* if encoding is specified externally, 'encoding' information present
         * in the prolog of the XML document is not considered. Hence, prolog can
         * be read in Chunks of data instead of byte by byte.
         */
        fCurrentEntity = new Entity.ScannedEntity(reference, name,
                new XMLResourceIdentifierImpl(publicId, literalSystemId, baseSystemId, expandedSystemId),
                stream, reader, encoding, literal, encodingExternallySpecified, isExternal);
        fCurrentEntity.setEncodingExternallySpecified(encodingExternallySpecified);
        fEntityScanner.setCurrentEntity(fCurrentEntity);
        fResourceIdentifier.setValues(publicId, literalSystemId, baseSystemId, expandedSystemId);
        if (fLimitAnalyzer != null) {
            fLimitAnalyzer.startEntity(name);
        }
        return encoding;
    } 


    /**
     * Checks whether an entity given by name is external.
     *
     * @param entityName The name of the entity to check.
     * @return True if the entity is external, false otherwise
     * (including when the entity is not declared).
     */
    public boolean isExternalEntity(String entityName) {

        Entity entity = fEntities.get(entityName);
        if (entity == null) {
            return false;
        }
        return entity.isExternal();
    }

    /**
     * Checks whether the declaration of an entity given by name is
     * 
     *
     * @param entityName The name of the entity to check.
     * @return True if the entity was declared in the external subset, false otherwise
     *           (including when the entity is not declared).
     */
    public boolean isEntityDeclInExternalSubset(String entityName) {

        Entity entity = fEntities.get(entityName);
        if (entity == null) {
            return false;
        }
        return entity.isEntityDeclInExternalSubset();
    }




    /**
     * Sets whether the document entity is standalone.
     *
     * @param standalone True if document entity is standalone.
     */
    public void setStandalone(boolean standalone) {
        fStandalone = standalone;
    }

    /** Returns true if the document entity is standalone. */
    public boolean isStandalone() {
        return fStandalone;
    }  

    public boolean isDeclaredEntity(String entityName) {

        Entity entity = fEntities.get(entityName);
        return entity != null;
    }

    public boolean isUnparsedEntity(String entityName) {

        Entity entity = fEntities.get(entityName);
        if (entity == null) {
            return false;
        }
        return entity.isUnparsed();
    }



    public XMLResourceIdentifier getCurrentResourceIdentifier() {
        return fResourceIdentifier;
    }

    /**
     * Sets the entity handler. When an entity starts and ends, the
     * entity handler is notified of the change.
     *
     * @param entityHandler The new entity handler.
     */

    public void setEntityHandler(com.sun.org.apache.xerces.internal.impl.XMLEntityHandler entityHandler) {
        fEntityHandler = entityHandler;
    } 

    public StaxXMLInputSource resolveEntityAsPerStax(XMLResourceIdentifier resourceIdentifier) throws java.io.IOException{

        if(resourceIdentifier == null ) return null;

        String publicId = resourceIdentifier.getPublicId();
        String literalSystemId = resourceIdentifier.getLiteralSystemId();
        String baseSystemId = resourceIdentifier.getBaseSystemId();
        String expandedSystemId = resourceIdentifier.getExpandedSystemId();
        boolean needExpand = (expandedSystemId == null);
        if (baseSystemId == null && fCurrentEntity != null && fCurrentEntity.entityLocation != null) {
            baseSystemId = fCurrentEntity.entityLocation.getExpandedSystemId();
            if (baseSystemId != null)
                needExpand = true;
        }
        if (needExpand)
            expandedSystemId = expandSystemId(literalSystemId, baseSystemId,false);

        StaxXMLInputSource staxInputSource = null;
        XMLInputSource xmlInputSource = null;

        XMLResourceIdentifierImpl ri = null;

        if (resourceIdentifier instanceof XMLResourceIdentifierImpl) {
            ri = (XMLResourceIdentifierImpl)resourceIdentifier;
        } else {
            fResourceIdentifier.clear();
            ri = fResourceIdentifier;
        }
        ri.setValues(publicId, literalSystemId, baseSystemId, expandedSystemId);

        fISCreatedByResolver = false;
        if (fStaxEntityResolver != null) {
            staxInputSource = fStaxEntityResolver.resolveEntity(ri);
        } else if (fEntityResolver != null) {
            xmlInputSource = fEntityResolver.resolveEntity(ri);
            if (xmlInputSource != null) {
                fISCreatedByResolver = true;
                staxInputSource = new StaxXMLInputSource(xmlInputSource, fISCreatedByResolver);
            }
        }

        if (staxInputSource == null
                && (publicId != null || literalSystemId != null)
                && (fUseCatalog && fCatalogFile != null)) {
            if (fCatalogResolver == null) {
                fCatalogFeatures = JdkXmlUtils.getCatalogFeatures(fDefer, fCatalogFile, fPrefer, fResolve);
                fCatalogResolver = CatalogManager.catalogResolver(fCatalogFeatures);
            }

            staxInputSource = resolveWithCatalogStAX(fCatalogResolver, fCatalogFile, publicId, literalSystemId);
        }

        if (staxInputSource == null
                && (publicId != null || literalSystemId != null)
                && JdkXmlUtils.isResolveContinue(fCatalogFeatures)) {
            initJdkCatalogResolver();

            staxInputSource = resolveWithCatalogStAX(fDefCR, JdkCatalog.JDKCATALOG, publicId, literalSystemId);
        }

        if (staxInputSource != null) {
            fISCreatedByResolver = true;
        } else if ((publicId == null && literalSystemId == null)
                || (JdkXmlUtils.isResolveContinue(fCatalogFeatures)
                && fSecurityManager.is(Limit.JDKCATALOG_RESOLVE, JdkConstants.CONTINUE))) {
            staxInputSource = new StaxXMLInputSource(
                    new XMLInputSource(publicId, literalSystemId, baseSystemId, true), false);
        }

        return staxInputSource;

    }

    private void initJdkCatalogResolver() {
        if (fDefCR == null) {
            fDefCR = fSecurityManager.getJDKCatalogResolver();
        }
    }

    /**
     * Resolves the external resource using the Catalog specified and returns
     * a StaxXMLInputSource.
     */
    private StaxXMLInputSource resolveWithCatalogStAX(CatalogResolver cr, String cFile,
            String publicId, String systemId) {
        InputSource is = resolveWithCatalog(cr, cFile, publicId, systemId);
        if (is != null) {
            return new StaxXMLInputSource(new XMLInputSource(is, true), true);
        }
        return null;
    }

    /**
     * Resolves the external resource using the Catalog specified and returns
     * a InputSource.
     */
    private InputSource resolveWithCatalog(CatalogResolver cr, String cFile,
            String publicId, String systemId) {
        if (cr != null) {
            try {
                return cr.resolveEntity(publicId, systemId);
            } catch (CatalogException e) {
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,"CatalogException",
                        new Object[]{SecuritySupport.sanitizePath(cFile)},
                        XMLErrorReporter.SEVERITY_FATAL_ERROR, e );
            }
        }
        return null;
    }

    /**
     * Resolves the external resource using the Catalog specified and returns
     * a XMLInputSource. Since the Resolve method can be called from various processors,
     * this method attempts to resolve the resource as an EntityResolver first
     * and then URIResolver if no match is found.
     */
    private XMLInputSource resolveEntityOrURI(String catalogName, CatalogResolver cr,
            String publicId, String systemId, String base) {
        XMLInputSource xis = resolveEntity(catalogName, cr, publicId, systemId, base);

        if (xis != null) {
            return xis;
        } else if (systemId != null) {
            Source source = null;
            try {
                source = cr.resolve(systemId, base);
            } catch (CatalogException e) {
                throw new XNIException(e);
            }
            if (source != null && !source.isEmpty()) {
                return new XMLInputSource(publicId, source.getSystemId(), base, true);
            }
        }
        return null;
    }

    private XMLInputSource resolveEntity(String catalogName, CatalogResolver cr,
            String publicId, String systemId, String base) {
        InputSource is = null;
        try {
            if (publicId != null || systemId != null) {
                is = cr.resolveEntity(publicId, systemId);
            }
        } catch (CatalogException e) {
            if (fErrorReporter != null) {
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,"CatalogException",
                    new Object[]{SecuritySupport.sanitizePath(catalogName)},
                    XMLErrorReporter.SEVERITY_FATAL_ERROR, e );
            }
        }

        if (is != null && !is.isEmpty()) {
            return new XMLInputSource(is, true);
        }
        return null;
    }

    /**
     * Resolves the specified public and system identifiers. This
     * method first attempts to resolve the entity based on the
     * EntityResolver registered by the application. If no entity
     * resolver is registered or if the registered entity handler
     * is unable to resolve the entity, then default entity
     * resolution will occur.
     *
     * @param publicId     The public identifier of the entity.
     * @param systemId     The system identifier of the entity.
     * @param baseSystemId The base system identifier of the entity.
     *                     This is the system identifier of the current
     *                     entity and is used to expand the system
     *                     identifier when the system identifier is a
     *                     relative URI.
     *
     * @return Returns an input source that wraps the resolved entity.
     *         This method will never return null.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown by entity resolver to signal an error.
     */
    public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws IOException, XNIException {
        if(resourceIdentifier == null ) return null;
        String publicId = resourceIdentifier.getPublicId();
        String literalSystemId = resourceIdentifier.getLiteralSystemId();
        String baseSystemId = resourceIdentifier.getBaseSystemId();
        String expandedSystemId = resourceIdentifier.getExpandedSystemId();

        boolean needExpand = (expandedSystemId == null);
        if (baseSystemId == null && fCurrentEntity != null && fCurrentEntity.entityLocation != null) {
            baseSystemId = fCurrentEntity.entityLocation.getExpandedSystemId();
            if (baseSystemId != null)
                needExpand = true;
        }
        if (needExpand)
            expandedSystemId = expandSystemId(literalSystemId, baseSystemId,false);

        XMLInputSource xmlInputSource = null;

        if (fEntityResolver != null) {
            resourceIdentifier.setBaseSystemId(baseSystemId);
            resourceIdentifier.setExpandedSystemId(expandedSystemId);
            xmlInputSource = fEntityResolver.resolveEntity(resourceIdentifier);
        }

        if (xmlInputSource == null
                && (publicId != null || literalSystemId != null || resourceIdentifier.getNamespace() !=null)
                && (fUseCatalog && fCatalogFile != null)) {
            if (fCatalogResolver == null) {
                fCatalogFeatures = JdkXmlUtils.getCatalogFeatures(fDefer, fCatalogFile, fPrefer, fResolve);
                fCatalogResolver = CatalogManager.catalogResolver(fCatalogFeatures);
            }
            String pid = (publicId != null? publicId : resourceIdentifier.getNamespace());
            xmlInputSource = resolveEntityOrURI(fCatalogFile, fCatalogResolver, pid, literalSystemId, baseSystemId);
        }

        if (xmlInputSource == null
                && (publicId != null || literalSystemId != null)
                && JdkXmlUtils.isResolveContinue(fCatalogFeatures)) {
            initJdkCatalogResolver();
            xmlInputSource = resolveEntity("JDKCatalog", fDefCR, publicId, literalSystemId, baseSystemId);
        }

        if (xmlInputSource == null) {
            if ((publicId == null && literalSystemId == null) ||
                    (JdkXmlUtils.isResolveContinue(fCatalogFeatures) &&
                    fSecurityManager.is(Limit.JDKCATALOG_RESOLVE, JdkConstants.CONTINUE))) {
                xmlInputSource = new XMLInputSource(publicId, literalSystemId, baseSystemId, false);
            }
        }

        return xmlInputSource;

    } 

    /**
     * Starts a named entity.
     *
     * @param isGE flag to indicate whether the entity is a General Entity
     * @param entityName The name of the entity to start.
     * @param literal    True if this entity is started within a literal
     *                   value.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown by entity handler to signal an error.
     */
    public void startEntity(boolean isGE, String entityName, boolean literal)
    throws IOException, XNIException {

        Entity entity = fEntityStorage.getEntity(entityName);
        if (entity == null) {
            if (fEntityHandler != null) {
                String encoding = null;
                fResourceIdentifier.clear();
                fEntityAugs.removeAllItems();
                fEntityAugs.putItem(Constants.ENTITY_SKIPPED, Boolean.TRUE);
                fEntityHandler.startEntity(entityName, fResourceIdentifier, encoding, fEntityAugs);
                fEntityAugs.removeAllItems();
                fEntityAugs.putItem(Constants.ENTITY_SKIPPED, Boolean.TRUE);
                fEntityHandler.endEntity(entityName, fEntityAugs);
            }
            return;
        }

        boolean external = entity.isExternal();
        Entity.ExternalEntity externalEntity = null;
        String extLitSysId = null, extBaseSysId = null, expandedSystemId = null;
        if (external) {
            externalEntity = (Entity.ExternalEntity)entity;
            extLitSysId = (externalEntity.entityLocation != null ? externalEntity.entityLocation.getLiteralSystemId() : null);
            extBaseSysId = (externalEntity.entityLocation != null ? externalEntity.entityLocation.getBaseSystemId() : null);
            expandedSystemId = expandSystemId(extLitSysId, extBaseSysId, fStrictURI);
            boolean unparsed = entity.isUnparsed();
            boolean parameter = entityName.startsWith("%");
            boolean general = !parameter;
            if (unparsed || (general && !fExternalGeneralEntities) ||
                    (parameter && !fExternalParameterEntities) ||
                    !fSupportDTD || !fSupportExternalEntities) {

                if (fEntityHandler != null) {
                    fResourceIdentifier.clear();
                    final String encoding = null;
                    fResourceIdentifier.setValues(
                            (externalEntity.entityLocation != null ? externalEntity.entityLocation.getPublicId() : null),
                            extLitSysId, extBaseSysId, expandedSystemId);
                    fEntityAugs.removeAllItems();
                    fEntityAugs.putItem(Constants.ENTITY_SKIPPED, Boolean.TRUE);
                    fEntityHandler.startEntity(entityName, fResourceIdentifier, encoding, fEntityAugs);
                    fEntityAugs.removeAllItems();
                    fEntityAugs.putItem(Constants.ENTITY_SKIPPED, Boolean.TRUE);
                    fEntityHandler.endEntity(entityName, fEntityAugs);
                }
                return;
            }
        }

        int size = fEntityStack.size();
        for (int i = size; i >= 0; i--) {
            Entity activeEntity = i == size
                    ? fCurrentEntity
                    : fEntityStack.get(i);
            if (activeEntity.name == entityName) {
                String path = entityName;
                for (int j = i + 1; j < size; j++) {
                    activeEntity = fEntityStack.get(j);
                    path = path + " -> " + activeEntity.name;
                }
                path = path + " -> " + fCurrentEntity.name;
                path = path + " -> " + entityName;
                fErrorReporter.reportError(this.getEntityScanner(),XMLMessageFormatter.XML_DOMAIN,
                        "RecursiveReference",
                        new Object[] { entityName, path },
                        XMLErrorReporter.SEVERITY_FATAL_ERROR);

                        if (fEntityHandler != null) {
                            fResourceIdentifier.clear();
                            final String encoding = null;
                            if (external) {
                                fResourceIdentifier.setValues(
                                        (externalEntity.entityLocation != null ? externalEntity.entityLocation.getPublicId() : null),
                                        extLitSysId, extBaseSysId, expandedSystemId);
                            }
                            fEntityAugs.removeAllItems();
                            fEntityAugs.putItem(Constants.ENTITY_SKIPPED, Boolean.TRUE);
                            fEntityHandler.startEntity(entityName, fResourceIdentifier, encoding, fEntityAugs);
                            fEntityAugs.removeAllItems();
                            fEntityAugs.putItem(Constants.ENTITY_SKIPPED, Boolean.TRUE);
                            fEntityHandler.endEntity(entityName, fEntityAugs);
                        }

                        return;
            }
        }

        StaxXMLInputSource staxInputSource = null;
        XMLInputSource xmlInputSource = null ;

        if (external) {
            staxInputSource = resolveEntityAsPerStax(externalEntity.entityLocation);
            /** xxx:  Waiting from the EG
             * 
             * 
             * if(staxInputSource.hasXMLStreamOrXMLEventReader()) return ;
             */
            xmlInputSource = staxInputSource.getXMLInputSource() ;
            if (!fISCreatedByResolver) {
                String accessError = SecuritySupport.checkAccess(expandedSystemId,
                        fAccessExternalDTD, JdkConstants.ACCESS_EXTERNAL_ALL);
                if (accessError != null) {
                    fErrorReporter.reportError(this.getEntityScanner(),XMLMessageFormatter.XML_DOMAIN,
                            "AccessExternalEntity",
                            new Object[] { SecuritySupport.sanitizePath(expandedSystemId), accessError },
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }
            }
        }
        else {
            Entity.InternalEntity internalEntity = (Entity.InternalEntity)entity;
            Reader reader = new StringReader(internalEntity.text);
            xmlInputSource = new XMLInputSource(null, null, null, reader, null);
        }

        startEntity(isGE, entityName, xmlInputSource, literal, external);

    } 

    /**
     * Starts the document entity. The document entity has the "[xml]"
     * pseudo-name.
     *
     * @param xmlInputSource The input source of the document entity.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown by entity handler to signal an error.
     */
    public void startDocumentEntity(XMLInputSource xmlInputSource)
    throws IOException, XNIException {
        startEntity(false, XMLEntity, xmlInputSource, false, true);
    } 

    /**
     * Starts the DTD entity. The DTD entity has the "[dtd]"
     * pseudo-name.
     *
     * @param xmlInputSource The input source of the DTD entity.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown by entity handler to signal an error.
     */
    public void startDTDEntity(XMLInputSource xmlInputSource)
    throws IOException, XNIException {
        startEntity(false, DTDEntity, xmlInputSource, false, true);
    } 

    public void startExternalSubset() {
        fInExternalSubset = true;
    }

    public void endExternalSubset() {
        fInExternalSubset = false;
    }

    /**
     * Starts an entity.
     * <p>
     * This method can be used to insert an application defined XML
     * entity stream into the parsing stream.
     *
     * @param isGE flag to indicate whether the entity is a General Entity
     * @param name           The name of the entity.
     * @param xmlInputSource The input source of the entity.
     * @param literal        True if this entity is started within a
     *                       literal value.
     * @param isExternal    whether this entity should be treated as an internal or external entity.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown by entity handler to signal an error.
     */
    public void startEntity(boolean isGE, String name,
            XMLInputSource xmlInputSource,
            boolean literal, boolean isExternal)
            throws IOException, XNIException {

        String encoding = setupCurrentEntity(isGE, name, xmlInputSource, literal, isExternal);

        fEntityExpansionCount++;
        if(fLimitAnalyzer != null) {
           fLimitAnalyzer.addValue(entityExpansionIndex, name, 1);
        }
        if( fSecurityManager != null && fSecurityManager.isOverLimit(entityExpansionIndex, fLimitAnalyzer)){
            fSecurityManager.debugPrint(fLimitAnalyzer);
            fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,"EntityExpansionLimit",
                    new Object[]{fSecurityManager.getLimitValueByIndex(entityExpansionIndex)},
                    XMLErrorReporter.SEVERITY_FATAL_ERROR );
            fEntityExpansionCount = 0;
        }

        if (fEntityHandler != null) {
            fEntityHandler.startEntity(name, fResourceIdentifier, encoding, null);
        }

    } 

    /**
     * Return the current entity being scanned. Current entity is SET using startEntity function.
     * @return Entity.ScannedEntity
     */

    public Entity.ScannedEntity getCurrentEntity(){
        return fCurrentEntity ;
    }

    /**
     * Return the top level entity handled by this manager, or null
     * if no entity was added.
     */
    public Entity.ScannedEntity getTopLevelEntity() {
        return (Entity.ScannedEntity)
            (fEntityStack.empty() ? null : fEntityStack.get(0));
    }

    protected Stack<Reader> fReaderStack = new Stack<>();

    /**
     * Close all opened InputStreams and Readers opened by this parser.
     */
    public void closeReaders() {
        while (!fReaderStack.isEmpty()) {
            try {
                (fReaderStack.pop()).close();
            } catch (IOException e) {
            }
        }
    }

    public void endEntity() throws IOException, XNIException {

        if (DEBUG_BUFFER) {
            System.out.print("(endEntity: ");
            print();
            System.out.println();
        }
        Entity.ScannedEntity entity = fEntityStack.size() > 0 ? (Entity.ScannedEntity)fEntityStack.pop() : null ;

        /** need to close the reader first since the program can end
         *  prematurely (e.g. fEntityHandler.endEntity may throw exception)
         *  leaving the reader open
         */
        if(fCurrentEntity != null){
            try{
                if (fLimitAnalyzer != null) {
                    fLimitAnalyzer.endEntity(XMLSecurityManager.Limit.GENERAL_ENTITY_SIZE_LIMIT, fCurrentEntity.name);
                    if (fCurrentEntity.name.equals("[xml]")) {
                        fSecurityManager.debugPrint(fLimitAnalyzer);
                    }
                }
                fCurrentEntity.close();
            }catch(IOException ex){
                throw new XNIException(ex);
            }
        }

        if (!fReaderStack.isEmpty()) {
            fReaderStack.pop();
        }

        if (fEntityHandler != null) {
            if(entity == null){
                fEntityAugs.removeAllItems();
                fEntityAugs.putItem(Constants.LAST_ENTITY, Boolean.TRUE);
                fEntityHandler.endEntity(fCurrentEntity.name, fEntityAugs);
                fEntityAugs.removeAllItems();
            }else{
                fEntityHandler.endEntity(fCurrentEntity.name, null);
            }
        }
        boolean documentEntity = fCurrentEntity.name == XMLEntity;

        fCurrentEntity = entity;
        fEntityScanner.setCurrentEntity(fCurrentEntity);


        if(fCurrentEntity == null & !documentEntity){
            throw new EOFException() ;
        }

        if (DEBUG_BUFFER) {
            System.out.print(")endEntity: ");
            print();
            System.out.println();
        }

    } 


    public void reset(PropertyManager propertyManager){
        fSymbolTable = (SymbolTable)propertyManager.getProperty(Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY);
        fErrorReporter = (XMLErrorReporter)propertyManager.getProperty(Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY);
        try {
            fStaxEntityResolver = (StaxEntityResolverWrapper)propertyManager.getProperty(STAX_ENTITY_RESOLVER);
        } catch (XMLConfigurationException e) {
            fStaxEntityResolver = null;
        }

        fReplaceEntityReferences = ((Boolean)propertyManager.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES));
        fSupportExternalEntities = ((Boolean)propertyManager.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES));

        fLoadExternalDTD = !((Boolean)propertyManager.getProperty(Constants.ZEPHYR_PROPERTY_PREFIX + Constants.IGNORE_EXTERNAL_DTD));

        fUseCatalog = (Boolean)propertyManager.getProperty(XMLConstants.USE_CATALOG);
        fCatalogFile = (String)propertyManager.getProperty(JdkXmlUtils.CATALOG_FILES);
        fDefer = (String)propertyManager.getProperty(JdkXmlUtils.CATALOG_DEFER);
        fPrefer = (String)propertyManager.getProperty(JdkXmlUtils.CATALOG_PREFER);
        fResolve = (String)propertyManager.getProperty(JdkXmlUtils.CATALOG_RESOLVE);

        XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager) propertyManager.getProperty(XML_SECURITY_PROPERTY_MANAGER);
        fAccessExternalDTD = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);

        fSecurityManager = (XMLSecurityManager)propertyManager.getProperty(SECURITY_MANAGER);
        checkSupportDTD();

        fLimitAnalyzer = new XMLLimitAnalyzer();
        fEntityStorage.reset(propertyManager);
        fEntityScanner.reset(propertyManager);

        fEntities.clear();
        fEntityStack.removeAllElements();
        fCurrentEntity = null;
        fValidation = false;
        fExternalGeneralEntities = true;
        fExternalParameterEntities = true;
        fAllowJavaEncodings = true ;
    }

    /**
     * Resets the component. The component can query the component manager
     * about any features and properties that affect the operation of the
     * component.
     *
     * @param componentManager The component manager.
     *
     * @throws SAXException Thrown by component on initialization error.
     *                      For example, if a feature or property is
     *                      required for the operation of the component, the
     *                      component manager may throw a
     *                      SAXNotRecognizedException or a
     *                      SAXNotSupportedException.
     */
    public void reset(XMLComponentManager componentManager)
    throws XMLConfigurationException {

        boolean parser_settings = componentManager.getFeature(PARSER_SETTINGS, true);

        if (!parser_settings) {
            reset();
            if(fEntityScanner != null){
                fEntityScanner.reset(componentManager);
            }
            if(fEntityStorage != null){
                fEntityStorage.reset(componentManager);
            }
            return;
        }

        fValidation = componentManager.getFeature(VALIDATION, false);
        fExternalGeneralEntities = componentManager.getFeature(EXTERNAL_GENERAL_ENTITIES, true);
        fExternalParameterEntities = componentManager.getFeature(EXTERNAL_PARAMETER_ENTITIES, true);

        fAllowJavaEncodings = componentManager.getFeature(ALLOW_JAVA_ENCODINGS, false);
        fWarnDuplicateEntityDef = componentManager.getFeature(WARN_ON_DUPLICATE_ENTITYDEF, false);
        fStrictURI = componentManager.getFeature(STANDARD_URI_CONFORMANT, false);
        fLoadExternalDTD = componentManager.getFeature(LOAD_EXTERNAL_DTD, true);

        fSymbolTable = (SymbolTable)componentManager.getProperty(SYMBOL_TABLE);
        fErrorReporter = (XMLErrorReporter)componentManager.getProperty(ERROR_REPORTER);
        fEntityResolver = (XMLEntityResolver)componentManager.getProperty(ENTITY_RESOLVER, null);
        fStaxEntityResolver = (StaxEntityResolverWrapper)componentManager.getProperty(STAX_ENTITY_RESOLVER, null);
        fValidationManager = (ValidationManager)componentManager.getProperty(VALIDATION_MANAGER, null);
        fSecurityManager = (XMLSecurityManager)componentManager.getProperty(SECURITY_MANAGER, null);
        entityExpansionIndex = fSecurityManager.getIndex(JdkConstants.SP_ENTITY_EXPANSION_LIMIT);

        checkSupportDTD();
        fReplaceEntityReferences = true;
        fSupportExternalEntities = true;

        XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager) componentManager.getProperty(XML_SECURITY_PROPERTY_MANAGER, null);
        if (spm == null) {
            spm = new XMLSecurityPropertyManager();
        }
        fAccessExternalDTD = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);

        fUseCatalog = componentManager.getFeature(XMLConstants.USE_CATALOG, true);
        fCatalogFile = (String)componentManager.getProperty(JdkXmlUtils.CATALOG_FILES);
        fDefer = (String)componentManager.getProperty(JdkXmlUtils.CATALOG_DEFER);
        fPrefer = (String)componentManager.getProperty(JdkXmlUtils.CATALOG_PREFER);
        fResolve = (String)componentManager.getProperty(JdkXmlUtils.CATALOG_RESOLVE);

        reset();

        fEntityScanner.reset(componentManager);
        fEntityStorage.reset(componentManager);

    } 

    /**
     * Checks the supportDTD setting. Use the StAX supportDTD property if it is
     * set, otherwise the jdk.xml.dtd.support. Refer to the module-summary for
     * more details.
     */
    private void checkSupportDTD() {
        fSupportDTD = !fSecurityManager.is(Limit.DTD, JdkConstants.IGNORE);
        if (fSecurityManager.getState(Limit.STAX_SUPPORT_DTD) == JdkProperty.State.APIPROPERTY
                || fSecurityManager.getState(Limit.STAX_SUPPORT_DTD) == JdkProperty.State.LEGACY_APIPROPERTY) {
            fSupportDTD = fSecurityManager.is(Limit.STAX_SUPPORT_DTD);
        }
    }

    public void reset() {
        fLimitAnalyzer = new XMLLimitAnalyzer();
        fStandalone = false;
        fEntities.clear();
        fEntityStack.removeAllElements();
        fEntityExpansionCount = 0;

        fCurrentEntity = null;
        if(fXML10EntityScanner != null){
            fXML10EntityScanner.reset(fSymbolTable, this, fErrorReporter);
        }
        if(fXML11EntityScanner != null) {
            fXML11EntityScanner.reset(fSymbolTable, this, fErrorReporter);
        }

        if (DEBUG_ENTITIES) {
            addInternalEntity("text", "Hello, World.");
            addInternalEntity("empty-element", "<foo/>");
            addInternalEntity("balanced-element", "<foo></foo>");
            addInternalEntity("balanced-element-with-text", "<foo>Hello, World</foo>");
            addInternalEntity("balanced-element-with-entity", "<foo>&text;</foo>");
            addInternalEntity("unbalanced-entity", "<foo>");
            addInternalEntity("recursive-entity", "<foo>&recursive-entity2;</foo>");
            addInternalEntity("recursive-entity2", "<bar>&recursive-entity3;</bar>");
            addInternalEntity("recursive-entity3", "<baz>&recursive-entity;</baz>");
            try {
                addExternalEntity("external-text", null, "external-text.ent", "test/external-text.xml");
                addExternalEntity("external-balanced-element", null, "external-balanced-element.ent", "test/external-balanced-element.xml");
                addExternalEntity("one", null, "ent/one.ent", "test/external-entity.xml");
                addExternalEntity("two", null, "ent/two.ent", "test/ent/one.xml");
            }
            catch (IOException ex) {
            }
        }

        fEntityHandler = null;


    }
    /**
     * Returns a list of feature identifiers that are recognized by
     * this component. This method may return null if no features
     * are recognized by this component.
     */
    public String[] getRecognizedFeatures() {
        return RECOGNIZED_FEATURES.clone();
    } 

    /**
     * Sets the state of a feature. This method is called by the component
     * manager any time after reset when a feature changes state.
     * <p>
     * <strong>Note:</strong> Components should silently ignore features
     * that do not affect the operation of the component.
     *
     * @param featureId The feature identifier.
     * @param state     The state of the feature.
     *
     * @throws SAXNotRecognizedException The component should not throw
     *                                   this exception.
     * @throws SAXNotSupportedException The component should not throw
     *                                  this exception.
     */
    public void setFeature(String featureId, boolean state)
    throws XMLConfigurationException {

        if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
            final int suffixLength = featureId.length() - Constants.XERCES_FEATURE_PREFIX.length();
            if (suffixLength == Constants.ALLOW_JAVA_ENCODINGS_FEATURE.length() &&
                featureId.endsWith(Constants.ALLOW_JAVA_ENCODINGS_FEATURE)) {
                fAllowJavaEncodings = state;
            }
            if (suffixLength == Constants.LOAD_EXTERNAL_DTD_FEATURE.length() &&
                featureId.endsWith(Constants.LOAD_EXTERNAL_DTD_FEATURE)) {
                fLoadExternalDTD = state;
                return;
            }
        } else if (featureId.equals(XMLConstants.USE_CATALOG)) {
            fUseCatalog = state;
        }

    } 

    /**
     * Sets the value of a property. This method is called by the component
     * manager any time after reset when a property changes value.
     * <p>
     * <strong>Note:</strong> Components should silently ignore properties
     * that do not affect the operation of the component.
     *
     * @param propertyId The property identifier.
     * @param value      The value of the property.
     *
     * @throws SAXNotRecognizedException The component should not throw
     *                                   this exception.
     * @throws SAXNotSupportedException The component should not throw
     *                                  this exception.
     */
    public void setProperty(String propertyId, Object value){
        if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
            final int suffixLength = propertyId.length() - Constants.XERCES_PROPERTY_PREFIX.length();

            if (suffixLength == Constants.SYMBOL_TABLE_PROPERTY.length() &&
                propertyId.endsWith(Constants.SYMBOL_TABLE_PROPERTY)) {
                fSymbolTable = (SymbolTable)value;
                return;
            }
            if (suffixLength == Constants.ERROR_REPORTER_PROPERTY.length() &&
                propertyId.endsWith(Constants.ERROR_REPORTER_PROPERTY)) {
                fErrorReporter = (XMLErrorReporter)value;
                return;
            }
            if (suffixLength == Constants.ENTITY_RESOLVER_PROPERTY.length() &&
                propertyId.endsWith(Constants.ENTITY_RESOLVER_PROPERTY)) {
                fEntityResolver = (XMLEntityResolver)value;
                return;
            }
            if (suffixLength == Constants.BUFFER_SIZE_PROPERTY.length() &&
                propertyId.endsWith(Constants.BUFFER_SIZE_PROPERTY)) {
                Integer bufferSize = (Integer)value;
                if (bufferSize != null &&
                    bufferSize.intValue() > DEFAULT_XMLDECL_BUFFER_SIZE) {
                    fBufferSize = bufferSize.intValue();
                    fEntityScanner.setBufferSize(fBufferSize);
                }
            }
            if (suffixLength == Constants.SECURITY_MANAGER_PROPERTY.length() &&
                propertyId.endsWith(Constants.SECURITY_MANAGER_PROPERTY)) {
                fSecurityManager = (XMLSecurityManager)value;
            }
        }

        if (propertyId.equals(XML_SECURITY_PROPERTY_MANAGER))
        {
            XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager)value;
            fAccessExternalDTD = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);
            return;
        }

        if (propertyId.equals(JdkXmlUtils.CATALOG_FILES)) {
            fCatalogFile = (String)value;
        } else if (propertyId.equals(JdkXmlUtils.CATALOG_DEFER)) {
            fDefer = (String)value;
        } else if (propertyId.equals(JdkXmlUtils.CATALOG_PREFER)) {
            fPrefer = (String)value;
        } else if (propertyId.equals(JdkXmlUtils.CATALOG_RESOLVE)) {
            fResolve = (String)value;
        }
    }

    public void setLimitAnalyzer(XMLLimitAnalyzer fLimitAnalyzer) {
        this.fLimitAnalyzer = fLimitAnalyzer;
    }

    /**
     * Returns a list of property identifiers that are recognized by
     * this component. This method may return null if no properties
     * are recognized by this component.
     */
    public String[] getRecognizedProperties() {
        return RECOGNIZED_PROPERTIES.clone();
    } 
    /**
     * Returns the default state for a feature, or null if this
     * component does not want to report a default value for this
     * feature.
     *
     * @param featureId The feature identifier.
     *
     * @since Xerces 2.2.0
     */
    public Boolean getFeatureDefault(String featureId) {
        for (int i = 0; i < RECOGNIZED_FEATURES.length; i++) {
            if (RECOGNIZED_FEATURES[i].equals(featureId)) {
                return FEATURE_DEFAULTS[i];
            }
        }
        return null;
    } 

    /**
     * Returns the default state for a property, or null if this
     * component does not want to report a default value for this
     * property.
     *
     * @param propertyId The property identifier.
     *
     * @since Xerces 2.2.0
     */
    public Object getPropertyDefault(String propertyId) {
        for (int i = 0; i < RECOGNIZED_PROPERTIES.length; i++) {
            if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
                return PROPERTY_DEFAULTS[i];
            }
        }
        return null;
    } 


    /**
     * Expands a system id and returns the system id as a URI, if
     * it can be expanded. A return value of null means that the
     * identifier is already expanded. An exception thrown
     * indicates a failure to expand the id.
     *
     * @param systemId The systemId to be expanded.
     *
     * @return Returns the URI string representing the expanded system
     *         identifier. A null value indicates that the given
     *         system identifier is already expanded.
     *
     */
    public static String expandSystemId(String systemId) {
        return expandSystemId(systemId, null);
    } 


    private static String gUserDir;
    private static URI gUserDirURI;
    private static boolean gNeedEscaping[] = new boolean[128];
    private static char gAfterEscaping1[] = new char[128];
    private static char gAfterEscaping2[] = new char[128];
    private static char[] gHexChs = {'0', '1', '2', '3', '4', '5', '6', '7',
                                     '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    static {
        for (int i = 0; i <= 0x1f; i++) {
            gNeedEscaping[i] = true;
            gAfterEscaping1[i] = gHexChs[i >> 4];
            gAfterEscaping2[i] = gHexChs[i & 0xf];
        }
        gNeedEscaping[0x7f] = true;
        gAfterEscaping1[0x7f] = '7';
        gAfterEscaping2[0x7f] = 'F';
        char[] escChs = {' ', '<', '>', '#', '%', '"', '{', '}',
                         '|', '\\', '^', '~', '[', ']', '`'};
        int len = escChs.length;
        char ch;
        for (int i = 0; i < len; i++) {
            ch = escChs[i];
            gNeedEscaping[ch] = true;
            gAfterEscaping1[ch] = gHexChs[ch >> 4];
            gAfterEscaping2[ch] = gHexChs[ch & 0xf];
        }
    }

    private static synchronized URI getUserDir() throws URI.MalformedURIException {
        String userDir = "";
        try {
            userDir = SecuritySupport.getSystemProperty("user.dir");
        }
        catch (SecurityException se) {
        }

        if (userDir.length() == 0)
            return new URI("file", "", "", null, null);
        if (gUserDirURI != null && userDir.equals(gUserDir)) {
            return gUserDirURI;
        }

        gUserDir = userDir;

        char separator = java.io.File.separatorChar;
        userDir = userDir.replace(separator, '/');

        int len = userDir.length(), ch;
        StringBuilder buffer = new StringBuilder(len*3);
        if (len >= 2 && userDir.charAt(1) == ':') {
            ch = Character.toUpperCase(userDir.charAt(0));
            if (ch >= 'A' && ch <= 'Z') {
                buffer.append('/');
            }
        }

        int i = 0;
        for (; i < len; i++) {
            ch = userDir.charAt(i);
            if (ch >= 128)
                break;
            if (gNeedEscaping[ch]) {
                buffer.append('%');
                buffer.append(gAfterEscaping1[ch]);
                buffer.append(gAfterEscaping2[ch]);
            }
            else {
                buffer.append((char)ch);
            }
        }

        if (i < len) {
            byte[] bytes = null;
            byte b;
            try {
                bytes = userDir.substring(i).getBytes("UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                return new URI("file", "", userDir, null, null);
            }
            len = bytes.length;

            for (i = 0; i < len; i++) {
                b = bytes[i];
                if (b < 0) {
                    ch = b + 256;
                    buffer.append('%');
                    buffer.append(gHexChs[ch >> 4]);
                    buffer.append(gHexChs[ch & 0xf]);
                }
                else if (gNeedEscaping[b]) {
                    buffer.append('%');
                    buffer.append(gAfterEscaping1[b]);
                    buffer.append(gAfterEscaping2[b]);
                }
                else {
                    buffer.append((char)b);
                }
            }
        }

        if (!userDir.endsWith("/"))
            buffer.append('/');

        gUserDirURI = new URI("file", "", buffer.toString(), null, null);

        return gUserDirURI;
    }

    public static OutputStream createOutputStream(String uri) throws IOException {
        final String expanded = XMLEntityManager.expandSystemId(uri, null, true);
        @SuppressWarnings("deprecation")
        final URL url = new URL(expanded != null ? expanded : uri);
        OutputStream out = null;
        String protocol = url.getProtocol();
        String host = url.getHost();
        if (protocol.equals("file")
                && (host == null || host.length() == 0 || host.equals("localhost"))) {
            File file = new File(getPathWithoutEscapes(url.getPath()));
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
            }
            out = new FileOutputStream(file);
        }
        else {
            URLConnection urlCon = url.openConnection();
            urlCon.setDoInput(false);
            urlCon.setDoOutput(true);
            urlCon.setUseCaches(false); 
            if (urlCon instanceof HttpURLConnection) {
                HttpURLConnection httpCon = (HttpURLConnection) urlCon;
                httpCon.setRequestMethod("PUT");
            }
            out = urlCon.getOutputStream();
        }
        return out;
    }

    private static String getPathWithoutEscapes(String origPath) {
        if (origPath != null && origPath.length() != 0 && origPath.indexOf('%') != -1) {
            StringTokenizer tokenizer = new StringTokenizer(origPath, "%");
            StringBuilder result = new StringBuilder(origPath.length());
            int size = tokenizer.countTokens();
            result.append(tokenizer.nextToken());
            for(int i = 1; i < size; ++i) {
                String token = tokenizer.nextToken();
                result.append((char)Integer.valueOf(token.substring(0, 2), 16).intValue());
                result.append(token.substring(2));
            }
            return result.toString();
        }
        return origPath;
    }

    /**
     * Absolutizes a URI using the current value
     * of the "user.dir" property as the base URI. If
     * the URI is already absolute, this is a no-op.
     *
     * @param uri the URI to absolutize
     */
    public static void absolutizeAgainstUserDir(URI uri)
        throws URI.MalformedURIException {
        uri.absolutize(getUserDir());
    }

    /**
     * Expands a system id and returns the system id as a URI, if
     * it can be expanded. A return value of null means that the
     * identifier is already expanded. An exception thrown
     * indicates a failure to expand the id.
     *
     * @param systemId The systemId to be expanded.
     *
     * @return Returns the URI string representing the expanded system
     *         identifier. A null value indicates that the given
     *         system identifier is already expanded.
     *
     */
    public static String expandSystemId(String systemId, String baseSystemId) {

        if (systemId == null || systemId.length() == 0) {
            return systemId;
        }
        try {
            URI uri = new URI(systemId);
            if (uri != null) {
                return systemId;
            }
        } catch (URI.MalformedURIException e) {
        }
        String id = fixURI(systemId);

        URI base = null;
        URI uri = null;
        try {
            if (baseSystemId == null || baseSystemId.length() == 0 ||
                    baseSystemId.equals(systemId)) {
                String dir = getUserDir().toString();
                base = new URI("file", "", dir, null, null);
            } else {
                try {
                    base = new URI(fixURI(baseSystemId));
                } catch (URI.MalformedURIException e) {
                    if (baseSystemId.indexOf(':') != -1) {
                        base = new URI("file", "", fixURI(baseSystemId), null, null);
                    } else {
                        String dir = getUserDir().toString();
                        dir = dir + fixURI(baseSystemId);
                        base = new URI("file", "", dir, null, null);
                    }
                }
            }
            uri = new URI(base, id);
        } catch (Exception e) {

        }

        if (uri == null) {
            return systemId;
        }
        return uri.toString();

    } 

    /**
     * Expands a system id and returns the system id as a URI, if
     * it can be expanded. A return value of null means that the
     * identifier is already expanded. An exception thrown
     * indicates a failure to expand the id.
     *
     * @param systemId The systemId to be expanded.
     *
     * @return Returns the URI string representing the expanded system
     *         identifier. A null value indicates that the given
     *         system identifier is already expanded.
     *
     */
    public static String expandSystemId(String systemId, String baseSystemId,
                                        boolean strict)
            throws URI.MalformedURIException {

        if (systemId == null) {
            return null;
        }

        if (strict) {
            try {
                new URI(systemId);
                return systemId;
            }
            catch (URI.MalformedURIException ex) {
            }
            URI base = null;
            if (baseSystemId == null || baseSystemId.length() == 0) {
                base = new URI("file", "", getUserDir().toString(), null, null);
            }
            else {
                try {
                    base = new URI(baseSystemId);
                }
                catch (URI.MalformedURIException e) {
                    String dir = getUserDir().toString();
                    dir = dir + baseSystemId;
                    base = new URI("file", "", dir, null, null);
                }
            }
            URI uri = new URI(base, systemId);
            return uri.toString();

        }

        try {
             return expandSystemIdStrictOff(systemId, baseSystemId);
        }
        catch (URI.MalformedURIException e) {
            /** Xerces URI rejects unicode, try java.net.URI
             * this is not ideal solution, but it covers known cases which either
             * Xerces URI or java.net.URI can handle alone
             * will file bug against java.net.URI
             */
            try {
                return expandSystemIdStrictOff1(systemId, baseSystemId);
            } catch (URISyntaxException ex) {
            }
        }
        if (systemId.length() == 0) {
            return systemId;
        }

        String id = fixURI(systemId);

        URI base = null;
        URI uri = null;
        try {
            if (baseSystemId == null || baseSystemId.length() == 0 ||
                baseSystemId.equals(systemId)) {
                base = getUserDir();
            }
            else {
                try {
                    base = new URI(fixURI(baseSystemId).trim());
                }
                catch (URI.MalformedURIException e) {
                    if (baseSystemId.indexOf(':') != -1) {
                        base = new URI("file", "", fixURI(baseSystemId).trim(), null, null);
                    }
                    else {
                        base = new URI(getUserDir(), fixURI(baseSystemId));
                    }
                }
             }
             uri = new URI(base, id.trim());
        }
        catch (Exception e) {

        }

        if (uri == null) {
            return systemId;
        }
        return uri.toString();

    } 

    /**
     * Helper method for expandSystemId(String,String,boolean):String
     */
    private static String expandSystemIdStrictOn(String systemId, String baseSystemId)
        throws URI.MalformedURIException {

        URI systemURI = new URI(systemId, true);
        if (systemURI.isAbsoluteURI()) {
            return systemId;
        }

        URI baseURI = null;
        if (baseSystemId == null || baseSystemId.length() == 0) {
            baseURI = getUserDir();
        }
        else {
            baseURI = new URI(baseSystemId, true);
            if (!baseURI.isAbsoluteURI()) {
                baseURI.absolutize(getUserDir());
            }
        }

        systemURI.absolutize(baseURI);

        return systemURI.toString();


    } 

    /**
     * Helper method for expandSystemId(String,String,boolean):String
     */
    private static String expandSystemIdStrictOff(String systemId, String baseSystemId)
        throws URI.MalformedURIException {

        URI systemURI = new URI(systemId, true);
        if (systemURI.isAbsoluteURI()) {
            if (systemURI.getScheme().length() > 1) {
                return systemId;
            }
            /**
             * If the scheme's length is only one character,
             * it's likely that this was intended as a file
             * path. Fixing this up in expandSystemId to
             * maintain backwards compatibility.
             */
            throw new URI.MalformedURIException();
        }

        URI baseURI = null;
        if (baseSystemId == null || baseSystemId.length() == 0) {
            baseURI = getUserDir();
        }
        else {
            baseURI = new URI(baseSystemId, true);
            if (!baseURI.isAbsoluteURI()) {
                baseURI.absolutize(getUserDir());
            }
        }

        systemURI.absolutize(baseURI);

        return systemURI.toString();


    } 

    private static String expandSystemIdStrictOff1(String systemId, String baseSystemId)
        throws URISyntaxException, URI.MalformedURIException {

            java.net.URI systemURI = new java.net.URI(systemId);
        if (systemURI.isAbsolute()) {
            if (systemURI.getScheme().length() > 1) {
                return systemId;
            }
            /**
             * If the scheme's length is only one character,
             * it's likely that this was intended as a file
             * path. Fixing this up in expandSystemId to
             * maintain backwards compatibility.
             */
            throw new URISyntaxException(systemId, "the scheme's length is only one character");
        }

        URI baseURI = null;
        if (baseSystemId == null || baseSystemId.length() == 0) {
            baseURI = getUserDir();
        }
        else {
            baseURI = new URI(baseSystemId, true);
            if (!baseURI.isAbsoluteURI()) {
                baseURI.absolutize(getUserDir());
            }
        }

        systemURI = (new java.net.URI(baseURI.toString())).resolve(systemURI);

        return systemURI.toString();


    } 



    /**
     * Returns the IANA encoding name that is auto-detected from
     * the bytes specified, with the endian-ness of that encoding where appropriate.
     *
     * @param b4    The first four bytes of the input.
     * @param count The number of bytes actually read.
     * @return an instance of EncodingInfo which represents the auto-detected encoding.
     */
    protected EncodingInfo getEncodingInfo(byte[] b4, int count) {

        if (count < 2) {
            return EncodingInfo.UTF_8;
        }

        int b0 = b4[0] & 0xFF;
        int b1 = b4[1] & 0xFF;
        if (b0 == 0xFE && b1 == 0xFF) {
            return EncodingInfo.UTF_16_BIG_ENDIAN_WITH_BOM;
        }
        if (b0 == 0xFF && b1 == 0xFE) {
            return EncodingInfo.UTF_16_LITTLE_ENDIAN_WITH_BOM;
        }

        if (count < 3) {
            return EncodingInfo.UTF_8;
        }

        int b2 = b4[2] & 0xFF;
        if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) {
            return EncodingInfo.UTF_8_WITH_BOM;
        }

        if (count < 4) {
            return EncodingInfo.UTF_8;
        }

        int b3 = b4[3] & 0xFF;
        if (b0 == 0x00 && b1 == 0x00 && b2 == 0x00 && b3 == 0x3C) {
            return EncodingInfo.UCS_4_BIG_ENDIAN;
        }
        if (b0 == 0x3C && b1 == 0x00 && b2 == 0x00 && b3 == 0x00) {
            return EncodingInfo.UCS_4_LITTLE_ENDIAN;
        }
        if (b0 == 0x00 && b1 == 0x00 && b2 == 0x3C && b3 == 0x00) {
            return EncodingInfo.UCS_4_UNUSUAL_BYTE_ORDER;
        }
        if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x00) {
            return EncodingInfo.UCS_4_UNUSUAL_BYTE_ORDER;
        }
        if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x3F) {
            return EncodingInfo.UTF_16_BIG_ENDIAN;
        }
        if (b0 == 0x3C && b1 == 0x00 && b2 == 0x3F && b3 == 0x00) {
            return EncodingInfo.UTF_16_LITTLE_ENDIAN;
        }
        if (b0 == 0x4C && b1 == 0x6F && b2 == 0xA7 && b3 == 0x94) {
            return EncodingInfo.EBCDIC;
        }

        return EncodingInfo.UTF_8;

    } 

    /**
     * Creates a reader capable of reading the given input stream in
     * the specified encoding.
     *
     * @param inputStream  The input stream.
     * @param encoding     The encoding name that the input stream is
     *                     encoded using. If the user has specified that
     *                     Java encoding names are allowed, then the
     *                     encoding name may be a Java encoding name;
     *                     otherwise, it is an ianaEncoding name.
     * @param isBigEndian   For encodings (like uCS-4), whose names cannot
     *                      specify a byte order, this tells whether the order
     *                      is bigEndian.  null if unknown or irrelevant.
     *
     * @return Returns a reader.
     */
    protected Reader createReader(InputStream inputStream, String encoding, Boolean isBigEndian)
        throws IOException {

        String enc = (encoding != null) ? encoding : EncodingInfo.STR_UTF8;
        enc = enc.toUpperCase(Locale.ENGLISH);
        MessageFormatter f = fErrorReporter.getMessageFormatter(XMLMessageFormatter.XML_DOMAIN);
        Locale l = fErrorReporter.getLocale();
        switch (enc) {
            case EncodingInfo.STR_UTF8:
                return new UTF8Reader(inputStream, fBufferSize, f, l);
            case EncodingInfo.STR_UTF16:
                if (isBigEndian != null) {
                    return new UTF16Reader(inputStream, fBufferSize, isBigEndian, f, l);
                }
                break;
            case EncodingInfo.STR_UTF16BE:
                return new UTF16Reader(inputStream, fBufferSize, true, f, l);
            case EncodingInfo.STR_UTF16LE:
                return new UTF16Reader(inputStream, fBufferSize, false, f, l);
            case EncodingInfo.STR_UCS4:
                if(isBigEndian != null) {
                    if(isBigEndian) {
                        return new UCSReader(inputStream, UCSReader.UCS4BE);
                    } else {
                        return new UCSReader(inputStream, UCSReader.UCS4LE);
                    }
                } else {
                    fErrorReporter.reportError(this.getEntityScanner(),
                            XMLMessageFormatter.XML_DOMAIN,
                            "EncodingByteOrderUnsupported",
                            new Object[] { encoding },
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }
                break;
            case EncodingInfo.STR_UCS2:
                if(isBigEndian != null) {
                    if(isBigEndian) {
                        return new UCSReader(inputStream, UCSReader.UCS2BE);
                    } else {
                        return new UCSReader(inputStream, UCSReader.UCS2LE);
                    }
                } else {
                    fErrorReporter.reportError(this.getEntityScanner(),
                            XMLMessageFormatter.XML_DOMAIN,
                            "EncodingByteOrderUnsupported",
                            new Object[] { encoding },
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }
                break;
        }

        boolean validIANA = XMLChar.isValidIANAEncoding(encoding);
        boolean validJava = XMLChar.isValidJavaEncoding(encoding);
        if (!validIANA || (fAllowJavaEncodings && !validJava)) {
            fErrorReporter.reportError(this.getEntityScanner(),
                    XMLMessageFormatter.XML_DOMAIN,
                    "EncodingDeclInvalid",
                    new Object[] { encoding },
                    XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    encoding = "ISO-8859-1";
        }

        String javaEncoding = EncodingMap.getIANA2JavaMapping(enc);
        if (javaEncoding == null) {
            if (fAllowJavaEncodings) {
                javaEncoding = encoding;
            } else {
                fErrorReporter.reportError(this.getEntityScanner(),
                        XMLMessageFormatter.XML_DOMAIN,
                        "EncodingDeclInvalid",
                        new Object[] { encoding },
                        XMLErrorReporter.SEVERITY_FATAL_ERROR);
                javaEncoding = "ISO8859_1";
            }
        }
        if (DEBUG_ENCODINGS) {
            System.out.print("$$$ creating Java InputStreamReader: encoding="+javaEncoding);
            if (javaEncoding == encoding) {
                System.out.print(" (IANA encoding)");
            }
            System.out.println();
        }
        return new BufferedReader( new InputStreamReader(inputStream, javaEncoding));

    } 


    /**
     * Return the public identifier for the current document event.
     * <p>
     * The return value is the public identifier of the document
     * entity or of the external parsed entity in which the markup
     * triggering the event appears.
     *
     * @return A string containing the public identifier, or
     *         null if none is available.
     */
    public String getPublicId() {
        return (fCurrentEntity != null && fCurrentEntity.entityLocation != null) ? fCurrentEntity.entityLocation.getPublicId() : null;
    } 

    /**
     * Return the expanded system identifier for the current document event.
     * <p>
     * The return value is the expanded system identifier of the document
     * entity or of the external parsed entity in which the markup
     * triggering the event appears.
     * <p>
     * If the system identifier is a URL, the parser must resolve it
     * fully before passing it to the application.
     *
     * @return A string containing the expanded system identifier, or null
     *         if none is available.
     */
    public String getExpandedSystemId() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.entityLocation != null &&
                    fCurrentEntity.entityLocation.getExpandedSystemId() != null ) {
                return fCurrentEntity.entityLocation.getExpandedSystemId();
            } else {
                int size = fEntityStack.size();
                for (int i = size - 1; i >= 0 ; i--) {
                    Entity.ScannedEntity externalEntity =
                            (Entity.ScannedEntity)fEntityStack.get(i);

                    if (externalEntity.entityLocation != null &&
                            externalEntity.entityLocation.getExpandedSystemId() != null) {
                        return externalEntity.entityLocation.getExpandedSystemId();
                    }
                }
            }
        }
        return null;
    } 

    /**
     * Return the literal system identifier for the current document event.
     * <p>
     * The return value is the literal system identifier of the document
     * entity or of the external parsed entity in which the markup
     * triggering the event appears.
     * <p>
     * @return A string containing the literal system identifier, or null
     *         if none is available.
     */
    public String getLiteralSystemId() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.entityLocation != null &&
                    fCurrentEntity.entityLocation.getLiteralSystemId() != null ) {
                return fCurrentEntity.entityLocation.getLiteralSystemId();
            } else {
                int size = fEntityStack.size();
                for (int i = size - 1; i >= 0 ; i--) {
                    Entity.ScannedEntity externalEntity =
                            (Entity.ScannedEntity)fEntityStack.get(i);

                    if (externalEntity.entityLocation != null &&
                            externalEntity.entityLocation.getLiteralSystemId() != null) {
                        return externalEntity.entityLocation.getLiteralSystemId();
                    }
                }
            }
        }
        return null;
    } 

    /**
     * Return the line number where the current document event ends.
     * <p>
     * <strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.
     * <p>
     * The return value is an approximation of the line number
     * in the document entity or external parsed entity where the
     * markup triggering the event appears.
     * <p>
     * If possible, the SAX driver should provide the line position
     * of the first character after the text associated with the document
     * event.  The first line in the document is line 1.
     *
     * @return The line number, or -1 if none is available.
     */
    public int getLineNumber() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.isExternal()) {
                return fCurrentEntity.lineNumber;
            } else {
                int size = fEntityStack.size();
                for (int i=size-1; i>0 ; i--) {
                    Entity.ScannedEntity firstExternalEntity = (Entity.ScannedEntity)fEntityStack.get(i);
                    if (firstExternalEntity.isExternal()) {
                        return firstExternalEntity.lineNumber;
                    }
                }
            }
        }

        return -1;

    } 

    /**
     * Return the column number where the current document event ends.
     * <p>
     * <strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.
     * <p>
     * The return value is an approximation of the column number
     * in the document entity or external parsed entity where the
     * markup triggering the event appears.
     * <p>
     * If possible, the SAX driver should provide the line position
     * of the first character after the text associated with the document
     * event.
     * <p>
     * If possible, the SAX driver should provide the line position
     * of the first character after the text associated with the document
     * event.  The first column in each line is column 1.
     *
     * @return The column number, or -1 if none is available.
     */
    public int getColumnNumber() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.isExternal()) {
                return fCurrentEntity.columnNumber;
            } else {
                int size = fEntityStack.size();
                for (int i=size-1; i>0 ; i--) {
                    Entity.ScannedEntity firstExternalEntity = (Entity.ScannedEntity)fEntityStack.get(i);
                    if (firstExternalEntity.isExternal()) {
                        return firstExternalEntity.columnNumber;
                    }
                }
            }
        }

        return -1;
    } 



    /**
     * Fixes a platform dependent filename to standard URI form.
     *
     * @param str The string to fix.
     *
     * @return Returns the fixed URI string.
     */
    protected static String fixURI(String str) {

        str = str.replace(java.io.File.separatorChar, '/');

        if (str.length() >= 2) {
            char ch1 = str.charAt(1);
            if (ch1 == ':') {
                char ch0 = Character.toUpperCase(str.charAt(0));
                if (ch0 >= 'A' && ch0 <= 'Z') {
                    str = "/" + str;
                }
            }
            else if (ch1 == '/' && str.charAt(0) == '/') {
                str = "file:" + str;
            }
        }

        int pos = str.indexOf(' ');
        if (pos >= 0) {
            StringBuilder sb = new StringBuilder(str.length());
            for (int i = 0; i < pos; i++)
                sb.append(str.charAt(i));
            sb.append("%20");
            for (int i = pos+1; i < str.length(); i++) {
                if (str.charAt(i) == ' ')
                    sb.append("%20");
                else
                    sb.append(str.charAt(i));
            }
            str = sb.toString();
        }

        return str;

    } 


    /** Prints the contents of the buffer. */
    final void print() {
        if (DEBUG_BUFFER) {
            if (fCurrentEntity != null) {
                System.out.print('[');
                System.out.print(fCurrentEntity.count);
                System.out.print(' ');
                System.out.print(fCurrentEntity.position);
                if (fCurrentEntity.count > 0) {
                    System.out.print(" \"");
                    for (int i = 0; i < fCurrentEntity.count; i++) {
                        if (i == fCurrentEntity.position) {
                            System.out.print('^');
                        }
                        char c = fCurrentEntity.ch[i];
                        switch (c) {
                            case '\n': {
                                System.out.print("\\n");
                                break;
                            }
                            case '\r': {
                                System.out.print("\\r");
                                break;
                            }
                            case '\t': {
                                System.out.print("\\t");
                                break;
                            }
                            case '\\': {
                                System.out.print("\\\\");
                                break;
                            }
                            default: {
                                System.out.print(c);
                            }
                        }
                    }
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        System.out.print('^');
                    }
                    System.out.print('"');
                }
                System.out.print(']');
                System.out.print(" @ ");
                System.out.print(fCurrentEntity.lineNumber);
                System.out.print(',');
                System.out.print(fCurrentEntity.columnNumber);
            } else {
                System.out.print("*NO CURRENT ENTITY*");
            }
        }
    } 

    /**
     * Information about auto-detectable encodings.
     *
     * @xerces.internal
     *
     * @author Michael Glavassevich, IBM
     */
    private static class EncodingInfo {
        public static final String STR_UTF8 = "UTF-8";
        public static final String STR_UTF16 = "UTF-16";
        public static final String STR_UTF16BE = "UTF-16BE";
        public static final String STR_UTF16LE = "UTF-16LE";
        public static final String STR_UCS4 = "ISO-10646-UCS-4";
        public static final String STR_UCS2 = "ISO-10646-UCS-2";
        public static final String STR_CP037 = "CP037";

        /** UTF-8 **/
        public static final EncodingInfo UTF_8 =
                new EncodingInfo(STR_UTF8, null, false);

        /** UTF-8, with BOM **/
        public static final EncodingInfo UTF_8_WITH_BOM =
                new EncodingInfo(STR_UTF8, null, true);

        /** UTF-16, big-endian **/
        public static final EncodingInfo UTF_16_BIG_ENDIAN =
                new EncodingInfo(STR_UTF16BE, STR_UTF16, Boolean.TRUE, false);

        /** UTF-16, big-endian with BOM **/
        public static final EncodingInfo UTF_16_BIG_ENDIAN_WITH_BOM =
                new EncodingInfo(STR_UTF16BE, STR_UTF16, Boolean.TRUE, true);

        /** UTF-16, little-endian **/
        public static final EncodingInfo UTF_16_LITTLE_ENDIAN =
                new EncodingInfo(STR_UTF16LE, STR_UTF16, Boolean.FALSE, false);

        /** UTF-16, little-endian with BOM **/
        public static final EncodingInfo UTF_16_LITTLE_ENDIAN_WITH_BOM =
                new EncodingInfo(STR_UTF16LE, STR_UTF16, Boolean.FALSE, true);

        /** UCS-4, big-endian **/
        public static final EncodingInfo UCS_4_BIG_ENDIAN =
                new EncodingInfo(STR_UCS4, Boolean.TRUE, false);

        /** UCS-4, little-endian **/
        public static final EncodingInfo UCS_4_LITTLE_ENDIAN =
                new EncodingInfo(STR_UCS4, Boolean.FALSE, false);

        /** UCS-4, unusual byte-order (2143) or (3412) **/
        public static final EncodingInfo UCS_4_UNUSUAL_BYTE_ORDER =
                new EncodingInfo(STR_UCS4, null, false);

        /** EBCDIC **/
        public static final EncodingInfo EBCDIC = new EncodingInfo(STR_CP037, null, false);

        public final String autoDetectedEncoding;
        public final String readerEncoding;
        public final Boolean isBigEndian;
        public final boolean hasBOM;

        private EncodingInfo(String autoDetectedEncoding, Boolean isBigEndian, boolean hasBOM) {
            this(autoDetectedEncoding, autoDetectedEncoding, isBigEndian, hasBOM);
        } 

        private EncodingInfo(String autoDetectedEncoding, String readerEncoding,
                Boolean isBigEndian, boolean hasBOM) {
            this.autoDetectedEncoding = autoDetectedEncoding;
            this.readerEncoding = readerEncoding;
            this.isBigEndian = isBigEndian;
            this.hasBOM = hasBOM;
        } 

    } 

    /**
    * This class wraps the byte inputstreams we're presented with.
    * We need it because java.io.InputStreams don't provide
    * functionality to reread processed bytes, and they have a habit
    * of reading more than one character when you call their read()
    * methods.  This means that, once we discover the true (declared)
    * encoding of a document, we can neither backtrack to read the
    * whole doc again nor start reading where we are with a new
    * reader.
    *
    * This class allows rewinding an inputStream by allowing a mark
    * to be set, and the stream reset to that position.  <strong>The
    * class assumes that it needs to read one character per
    * invocation when it's read() method is inovked, but uses the
    * underlying InputStream's read(char[], offset length) method--it
    * won't buffer data read this way!</strong>
    *
    * @xerces.internal
    *
    * @author Neil Graham, IBM
    * @author Glenn Marcy, IBM
    */

    protected final class RewindableInputStream extends InputStream {

        private InputStream fInputStream;
        private byte[] fData;
        private int fStartOffset;
        private int fEndOffset;
        private int fOffset;
        private int fLength;
        private int fMark;

        public RewindableInputStream(InputStream is) {
            fData = new byte[DEFAULT_XMLDECL_BUFFER_SIZE];
            fInputStream = is;
            fStartOffset = 0;
            fEndOffset = -1;
            fOffset = 0;
            fLength = 0;
            fMark = 0;
        }

        public void setStartOffset(int offset) {
            fStartOffset = offset;
        }

        public void rewind() {
            fOffset = fStartOffset;
        }

        public int readAndBuffer() throws IOException {
            if (fOffset == fData.length) {
                byte[] newData = new byte[fOffset << 1];
                System.arraycopy(fData, 0, newData, 0, fOffset);
                fData = newData;
            }
            final int b = fInputStream.read();
            if (b == -1) {
                fEndOffset = fOffset;
                return -1;
            }
            fData[fLength++] = (byte)b;
            fOffset++;
            return b & 0xff;
        }

        public int read() throws IOException {
            if (fOffset < fLength) {
                return fData[fOffset++] & 0xff;
            }
            if (fOffset == fEndOffset) {
                return -1;
            }
            if (fCurrentEntity.mayReadChunks) {
                return fInputStream.read();
            }
            return readAndBuffer();
        }

        public int read(byte[] b, int off, int len) throws IOException {
            final int bytesLeft = fLength - fOffset;
            if (bytesLeft == 0) {
                if (fOffset == fEndOffset) {
                    return -1;
                }

                if(fCurrentEntity.mayReadChunks || !fCurrentEntity.xmlDeclChunkRead) {

                    if (!fCurrentEntity.xmlDeclChunkRead)
                    {
                        fCurrentEntity.xmlDeclChunkRead = true;
                        len = Entity.ScannedEntity.DEFAULT_XMLDECL_BUFFER_SIZE;
                    }
                    return fInputStream.read(b, off, len);
                }
                int returnedVal = readAndBuffer();
                if (returnedVal == -1) {
                    fEndOffset = fOffset;
                    return -1;
                }
                b[off] = (byte)returnedVal;
                return 1;
            }
            if (len < bytesLeft) {
                if (len <= 0) {
                    return 0;
                }
            } else {
                len = bytesLeft;
            }
            if (b != null) {
                System.arraycopy(fData, fOffset, b, off, len);
            }
            fOffset += len;
            return len;
        }

        public long skip(long n) throws IOException {
            int bytesLeft;
            if (n <= 0) {
                return 0;
            }
            bytesLeft = fLength - fOffset;
            if (bytesLeft == 0) {
                if (fOffset == fEndOffset) {
                    return 0;
                }
                return fInputStream.skip(n);
            }
            if (n <= bytesLeft) {
                fOffset += n;
                return n;
            }
            fOffset += bytesLeft;
            if (fOffset == fEndOffset) {
                return bytesLeft;
            }
            n -= bytesLeft;
           /*
            * In a manner of speaking, when this class isn't permitting more
            * than one byte at a time to be read, it is "blocking".  The
            * available() method should indicate how much can be read without
            * blocking, so while we're in this mode, it should only indicate
            * that bytes in its buffer are available; otherwise, the result of
            * available() on the underlying InputStream is appropriate.
            */
            return fInputStream.skip(n) + bytesLeft;
        }

        public int available() throws IOException {
            final int bytesLeft = fLength - fOffset;
            if (bytesLeft == 0) {
                if (fOffset == fEndOffset) {
                    return -1;
                }
                return fCurrentEntity.mayReadChunks ? fInputStream.available()
                                                    : 0;
            }
            return bytesLeft;
        }

        public void mark(int howMuch) {
            fMark = fOffset;
        }

        public void reset() {
            fOffset = fMark;
        }

        public boolean markSupported() {
            return true;
        }

        public void close() throws IOException {
            if (fInputStream != null) {
                fInputStream.close();
                fInputStream = null;
            }
        }
    } 

    public void test(){
        fEntityStorage.addExternalEntity("entityUsecase1",null,
                "/space/home/stax/sun/6thJan2004/zephyr/data/test.txt",
                "/space/home/stax/sun/6thJan2004/zephyr/data/entity.xml");

        fEntityStorage.addInternalEntity("entityUsecase2","<Test>value</Test>");
        fEntityStorage.addInternalEntity("entityUsecase3","value3");
        fEntityStorage.addInternalEntity("text", "Hello World.");
        fEntityStorage.addInternalEntity("empty-element", "<foo/>");
        fEntityStorage.addInternalEntity("balanced-element", "<foo></foo>");
        fEntityStorage.addInternalEntity("balanced-element-with-text", "<foo>Hello, World</foo>");
        fEntityStorage.addInternalEntity("balanced-element-with-entity", "<foo>&text;</foo>");
        fEntityStorage.addInternalEntity("unbalanced-entity", "<foo>");
        fEntityStorage.addInternalEntity("recursive-entity", "<foo>&recursive-entity2;</foo>");
        fEntityStorage.addInternalEntity("recursive-entity2", "<bar>&recursive-entity3;</bar>");
        fEntityStorage.addInternalEntity("recursive-entity3", "<baz>&recursive-entity;</baz>");
        fEntityStorage.addInternalEntity("ch","&#x00A9;");
        fEntityStorage.addInternalEntity("ch1","&#84;");
        fEntityStorage.addInternalEntity("% ch2","param");
    }

} 
