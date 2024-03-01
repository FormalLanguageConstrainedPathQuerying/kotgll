/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.xml.internal.stream;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.PropertyManager;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.util.URI;
import com.sun.org.apache.xerces.internal.util.XMLResourceIdentifierImpl;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import java.util.HashMap;
import java.util.Map;
import jdk.xml.internal.SecuritySupport;

/**
 *
 * @author K.Venugopal SUN Microsystems
 * @author Neeraj Bajaj SUN Microsystems
 * @author Andy Clark, IBM
 *
 */
public class XMLEntityStorage {

    /** Property identifier: error reporter. */
    protected static final String ERROR_REPORTER =
    Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Feature identifier: warn on duplicate EntityDef */
    protected static final String WARN_ON_DUPLICATE_ENTITYDEF =
    Constants.XERCES_FEATURE_PREFIX +Constants.WARN_ON_DUPLICATE_ENTITYDEF_FEATURE;

    /** warn on duplicate Entity declaration.
     *  http:
     */
    protected boolean fWarnDuplicateEntityDef;

    /** Entities. */
    protected Map<String, Entity> fEntities = new HashMap<>();

    protected Entity.ScannedEntity fCurrentEntity ;

    private XMLEntityManager fEntityManager;
    /**
     * Error reporter. This property identifier is:
     * http:
     */
    protected XMLErrorReporter fErrorReporter;
    protected PropertyManager fPropertyManager ;

    /* To keep track whether an entity is declared in external or internal subset*/
    protected boolean fInExternalSubset = false;

    /** Creates a new instance of XMLEntityStorage */
    public XMLEntityStorage(PropertyManager propertyManager) {
        fPropertyManager = propertyManager ;
    }

    /** Creates a new instance of XMLEntityStorage */
    /*public XMLEntityStorage(Entity.ScannedEntity currentEntity) {
        fCurrentEntity = currentEntity ;*/
    public XMLEntityStorage(XMLEntityManager entityManager) {
        fEntityManager = entityManager;
    }

    public void reset(PropertyManager propertyManager){

        fErrorReporter = (XMLErrorReporter)propertyManager.getProperty(Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY);
        fEntities.clear();
        fCurrentEntity = null;

    }

    public void reset(){
        fEntities.clear();
        fCurrentEntity = null;
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



        fWarnDuplicateEntityDef = componentManager.getFeature(WARN_ON_DUPLICATE_ENTITYDEF, false);

        fErrorReporter = (XMLErrorReporter)componentManager.getProperty(ERROR_REPORTER);

        fEntities.clear();
        fCurrentEntity = null;

    } 

    /**
     * Returns entity declaration.
     *
     * @param name The name of the entity.
     *
     * @see SymbolTable
     */
    public Entity getEntity(String name) {
        return fEntities.get(name);
    } 

    public Map<String, Entity> getEntities() {
        return fEntities;
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
        }
        else{
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
    String baseSystemId) {
        if (!fEntities.containsKey(name)) {
            if (baseSystemId == null) {
                /**
                 * int size = fEntityStack.size();
                 * if (size == 0 && fCurrentEntity != null && fCurrentEntity.entityLocation != null) {
                 * baseSystemId = fCurrentEntity.entityLocation.getExpandedSystemId();
                 * }
                 */

                if (fCurrentEntity != null && fCurrentEntity.entityLocation != null) {
                    baseSystemId = fCurrentEntity.entityLocation.getExpandedSystemId();
                }
                /**
                 * for (int i = size - 1; i >= 0 ; i--) {
                 * ScannedEntity externalEntity =
                 * (ScannedEntity)fEntityStack.elementAt(i);
                 * if (externalEntity.entityLocation != null && externalEntity.entityLocation.getExpandedSystemId() != null) {
                 * baseSystemId = externalEntity.entityLocation.getExpandedSystemId();
                 * break;
                 * }
                 * }
                 */
            }

            fCurrentEntity = fEntityManager.getCurrentEntity();
            Entity entity = new Entity.ExternalEntity(name,
            new XMLResourceIdentifierImpl(publicId, literalSystemId,
            baseSystemId, expandSystemId(literalSystemId, baseSystemId)),
            null, fInExternalSubset);
            fEntities.put(name, entity);
        }
        else{
            if(fWarnDuplicateEntityDef){
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                "MSG_DUPLICATE_ENTITY_DEFINITION",
                new Object[]{ name },
                XMLErrorReporter.SEVERITY_WARNING );
            }
        }

    } 

    /**
     * Checks whether an entity given by name is external.
     *
     * @param entityName The name of the entity to check.
     * @returns True if the entity is external, false otherwise
     *           (including when the entity is not declared).
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
     * @returns True if the entity was declared in the external subset, false otherwise
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

        fCurrentEntity = fEntityManager.getCurrentEntity();
        if (!fEntities.containsKey(name)) {
            Entity entity = new Entity.ExternalEntity(name,
                    new XMLResourceIdentifierImpl(publicId, systemId, baseSystemId, null),
                    notation, fInExternalSubset);
            fEntities.put(name, entity);
        }
        else{
            if(fWarnDuplicateEntityDef){
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                "MSG_DUPLICATE_ENTITY_DEFINITION",
                new Object[]{ name },
                XMLErrorReporter.SEVERITY_WARNING );
            }
        }
    } 

    /**
     * Checks whether an entity given by name is unparsed.
     *
     * @param entityName The name of the entity to check.
     * @returns True if the entity is unparsed, false otherwise
     *          (including when the entity is not declared).
     */
    public boolean isUnparsedEntity(String entityName) {

        Entity entity = fEntities.get(entityName);
        if (entity == null) {
            return false;
        }
        return entity.isUnparsed();
    }

    /**
     * Checks whether an entity given by name is declared.
     *
     * @param entityName The name of the entity to check.
     * @returns True if the entity is declared, false otherwise.
     */
    public boolean isDeclaredEntity(String entityName) {

        Entity entity = fEntities.get(entityName);
        return entity != null;
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
    private static String gEscapedUserDir;
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
    private static synchronized String getUserDir() {
        String userDir = "";
        try {
            userDir = SecuritySupport.getSystemProperty("user.dir");
        }
        catch (SecurityException se) {
        }

        if (userDir.length() == 0)
            return "";

        if (userDir.equals(gUserDir)) {
            return gEscapedUserDir;
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
                return userDir;
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

        gEscapedUserDir = buffer.toString();

        return gEscapedUserDir;
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
            new URI(systemId);
            return systemId;
        } catch (URI.MalformedURIException e) {
        }
        String id = fixURI(systemId);

        URI base = null;
        URI uri = null;
        try {
            if (baseSystemId == null || baseSystemId.length() == 0 ||
            baseSystemId.equals(systemId)) {
                String dir = getUserDir();
                base = new URI("file", "", dir, null, null);
            }
            else {
                try {
                    base = new URI(fixURI(baseSystemId));
                }
                catch (URI.MalformedURIException e) {
                    if (baseSystemId.indexOf(':') != -1) {
                        base = new URI("file", "", fixURI(baseSystemId), null, null);
                    }
                    else {
                        String dir = getUserDir();
                        dir = dir + fixURI(baseSystemId);
                        base = new URI("file", "", dir, null, null);
                    }
                }
            }
            uri = new URI(base, id);
        }
        catch (Exception e) {

        }

        if (uri == null) {
            return systemId;
        }
        return uri.toString();

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

        return str;

    } 

    public void startExternalSubset() {
        fInExternalSubset = true;
    }

    public void endExternalSubset() {
        fInExternalSubset = false;
    }
}
