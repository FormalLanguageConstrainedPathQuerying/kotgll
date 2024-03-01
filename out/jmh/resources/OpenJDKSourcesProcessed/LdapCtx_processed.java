/*
 * Copyright (c) 1999, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.event.*;
import javax.naming.ldap.*;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.jndi.toolkit.ctx.*;
import com.sun.jndi.toolkit.dir.HierMemDirCtx;
import com.sun.jndi.toolkit.dir.SearchFilter;
import com.sun.jndi.ldap.ext.StartTlsResponseImpl;
import com.sun.naming.internal.NamingManagerHelper;
import com.sun.naming.internal.ObjectFactoriesFilter;

/**
 * The LDAP context implementation.
 *
 * Implementation is not thread-safe. Caller must sync as per JNDI spec.
 * Members that are used directly or indirectly by internal worker threads
 * (Connection, EventQueue, NamingEventNotifier) must be thread-safe.
 * Connection - calls LdapClient.processUnsolicited(), which in turn calls
 *   LdapCtx.convertControls() and LdapCtx.fireUnsolicited().
 *   convertControls() - no sync; reads envprops and 'this'
 *   fireUnsolicited() - sync on eventSupport for all references to 'unsolicited'
 *      (even those in other methods);  don't sync on LdapCtx in case caller
 *      is already sync'ing on it - this would prevent Unsol events from firing
 *      and the Connection thread to block (thus preventing any other data
 *      from being read from the connection)
 *      References to 'eventSupport' need not be sync'ed because these
 *      methods can only be called after eventSupport has been set first
 *      (via addNamingListener()).
 * EventQueue - no direct or indirect calls to LdapCtx
 * NamingEventNotifier - calls newInstance() to get instance for run() to use;
 *      no sync needed for methods invoked on new instance;
 *
 * LdapAttribute links to LdapCtx in order to process getAttributeDefinition()
 * and getAttributeSyntaxDefinition() calls. It invokes LdapCtx.getSchema(),
 * which uses schemaTrees (a Hashtable - already sync). Potential conflict
 * of duplicating construction of tree for same subschemasubentry
 * but no inconsistency problems.
 *
 * NamingEnumerations link to LdapCtx for the following:
 * 1. increment/decrement enum count so that ctx doesn't close the
 *    underlying connection
 * 2. LdapClient handle to get next batch of results
 * 3. Sets LdapCtx's response controls
 * 4. Process return code
 * 5. For narrowing response controls (using ctx's factories)
 * Since processing of NamingEnumeration by client is treated the same as method
 * invocation on LdapCtx, caller is responsible for locking.
 *
 * @author Vincent Ryan
 * @author Rosanna Lee
 */

public final class LdapCtx extends ComponentDirContext
    implements EventDirContext, LdapContext {

    /*
     * Used to store arguments to the search method.
     */
    static final class SearchArgs {
        Name name;
        String filter;
        SearchControls cons;
        String[] reqAttrs; 

        SearchArgs(Name name, String filter, SearchControls cons, String[] ra) {
            this.name = name;
            this.filter = filter;
            this.cons = cons;
            this.reqAttrs = ra;
        }
    }

    private static final boolean debug = false;

    private static final boolean HARD_CLOSE = true;
    private static final boolean SOFT_CLOSE = false;


    public static final int DEFAULT_PORT = 389;
    public static final int DEFAULT_SSL_PORT = 636;
    public static final String DEFAULT_HOST = "localhost";

    private static final boolean DEFAULT_DELETE_RDN = true;
    private static final boolean DEFAULT_TYPES_ONLY = false;
    private static final int DEFAULT_DEREF_ALIASES = 3; 
    private static final int DEFAULT_LDAP_VERSION = LdapClient.LDAP_VERSION3_VERSION2;
    private static final int DEFAULT_BATCH_SIZE = 1;
    private static final int DEFAULT_REFERRAL_MODE = LdapClient.LDAP_REF_IGNORE;
    private static final char DEFAULT_REF_SEPARATOR = '#';

    static final String DEFAULT_SSL_FACTORY =
        "javax.net.ssl.SSLSocketFactory";       
    private static final int DEFAULT_REFERRAL_LIMIT = 10;
    private static final String STARTTLS_REQ_OID = "1.3.6.1.4.1.1466.20037";

    private static final String[] SCHEMA_ATTRIBUTES =
        { "objectClasses", "attributeTypes", "matchingRules", "ldapSyntaxes" };


    private static final String VERSION = "java.naming.ldap.version";

    private static final String BINARY_ATTRIBUTES =
                                        "java.naming.ldap.attributes.binary";

    private static final String DELETE_RDN = "java.naming.ldap.deleteRDN";

    private static final String DEREF_ALIASES = "java.naming.ldap.derefAliases";

    private static final String TYPES_ONLY = "java.naming.ldap.typesOnly";

    private static final String REF_SEPARATOR = "java.naming.ldap.ref.separator";

    private static final String SOCKET_FACTORY = "java.naming.ldap.factory.socket";

    static final String BIND_CONTROLS = "java.naming.ldap.control.connect";

    private static final String REFERRAL_LIMIT =
        "java.naming.ldap.referral.limit";

    private static final String TRACE_BER = "com.sun.jndi.ldap.trace.ber";

    private static final String NETSCAPE_SCHEMA_BUG =
        "com.sun.jndi.ldap.netscape.schemaBugs";
    private static final String OLD_NETSCAPE_SCHEMA_BUG =
        "com.sun.naming.netscape.schemaBugs";   

    private static final String CONNECT_TIMEOUT =
        "com.sun.jndi.ldap.connect.timeout";

    private static final String READ_TIMEOUT =
        "com.sun.jndi.ldap.read.timeout";

    private static final String ENABLE_POOL = "com.sun.jndi.ldap.connect.pool";

    private static final String DOMAIN_NAME = "com.sun.jndi.ldap.domainname";

    private static final String WAIT_FOR_REPLY =
        "com.sun.jndi.ldap.search.waitForReply";

    private static final String REPLY_QUEUE_SIZE =
        "com.sun.jndi.ldap.search.replyQueueSize";

    private static final String ALLOWED_MECHS_SP =
            "jdk.jndi.ldap.mechsAllowedToSendCredentials";

    private static final String ALLOWED_MECHS_SP_VALUE =
            getMechsAllowedToSendCredentials();

    private static final Set<String> MECHS_ALLOWED_BY_SP =
            getMechsFromPropertyValue(ALLOWED_MECHS_SP_VALUE);

    private static final String UNSECURED_CRED_TRANSMIT_MSG =
                "Transmission of credentials over unsecured connection is not allowed";

    private static final NameParser parser = new LdapNameParser();

    private static final ControlFactory myResponseControlFactory =
        new DefaultResponseControlFactory();
    private static final Control manageReferralControl =
        new ManageReferralControl(false);

    private static final HierMemDirCtx EMPTY_SCHEMA = new HierMemDirCtx();
    static {
        EMPTY_SCHEMA.setReadOnly(
            new SchemaViolationException("Cannot update schema object"));
    }



    int port_number;                    
    String hostname = null;             
    LdapClient clnt = null;             
    Hashtable<String, java.lang.Object> envprops = null; 
    int handleReferrals = DEFAULT_REFERRAL_MODE; 
    boolean hasLdapsScheme = false;     


    String currentDN;                   
    Name currentParsedDN;               
    Vector<Control> respCtls = null;    
    Control[] reqCtls = null;           
    volatile boolean contextSeenStartTlsEnabled;



    private OutputStream trace = null;  
    private boolean netscapeSchemaBug = false;       
    private Control[] bindCtls = null;  
    private int referralHopLimit = DEFAULT_REFERRAL_LIMIT;  
    private Hashtable<String, DirContext> schemaTrees = null; 
    private int batchSize = DEFAULT_BATCH_SIZE;      
    private boolean deleteRDN = DEFAULT_DELETE_RDN;  
    private boolean typesOnly = DEFAULT_TYPES_ONLY;  
    private int derefAliases = DEFAULT_DEREF_ALIASES;
    private char addrEncodingSeparator = DEFAULT_REF_SEPARATOR;  

    private Hashtable<String, Boolean> binaryAttrs = null; 
    private int connectTimeout = -1;         
    private int readTimeout = -1;            
    private boolean waitForReply = true;     
    private int replyQueueSize  = -1;        
    private boolean useSsl = false;          
    private boolean useDefaultPortNumber = false; 


    private boolean parentIsLdapCtx = false; 

    private int hopCount = 1;                
    private String url = null;               
    private EventSupport eventSupport;       
    private boolean unsolicited = false;     
    private boolean sharable = true;         
    private final ReentrantLock lock = new ReentrantLock(); 


    @SuppressWarnings("unchecked")
    public LdapCtx(String dn, String host, int port_number,
            Hashtable<?,?> props,
            boolean useSsl) throws NamingException {

        this.useSsl = this.hasLdapsScheme = useSsl;

        if (props != null) {
            envprops = (Hashtable<String, java.lang.Object>) props.clone();

            if ("ssl".equals(envprops.get(Context.SECURITY_PROTOCOL))) {
                this.useSsl = true;
            }

            trace = (OutputStream)envprops.get(TRACE_BER);

            if (props.get(NETSCAPE_SCHEMA_BUG) != null ||
                props.get(OLD_NETSCAPE_SCHEMA_BUG) != null) {
                netscapeSchemaBug = true;
            }
        }

        currentDN = (dn != null) ? dn : "";
        currentParsedDN = parser.parse(currentDN);

        hostname = (host != null && host.length() > 0) ? host : DEFAULT_HOST;
        if (hostname.charAt(0) == '[') {
            hostname = hostname.substring(1, hostname.length() - 1);
        }

        if (port_number > 0) {
            this.port_number = port_number;
        } else {
            this.port_number = this.useSsl ? DEFAULT_SSL_PORT : DEFAULT_PORT;
            this.useDefaultPortNumber = true;
        }

        schemaTrees = new Hashtable<>(11, 0.75f);
        initEnv();
        try {
            connect(false);
        } catch (NamingException e) {
            try {
                close();
            } catch (Exception e2) {
            }
            throw e;
        }
    }

    LdapCtx(LdapCtx existing, String newDN) throws NamingException {
        useSsl = existing.useSsl;
        hasLdapsScheme = existing.hasLdapsScheme;
        useDefaultPortNumber = existing.useDefaultPortNumber;

        hostname = existing.hostname;
        port_number = existing.port_number;
        currentDN = newDN;
        if (existing.currentDN == currentDN) {
            currentParsedDN = existing.currentParsedDN;
        } else {
            currentParsedDN = parser.parse(currentDN);
        }

        envprops = existing.envprops;
        schemaTrees = existing.schemaTrees;

        clnt = existing.clnt;
        clnt.incRefCount();

        parentIsLdapCtx = ((newDN == null || newDN.equals(existing.currentDN))
                           ? existing.parentIsLdapCtx
                           : true);

        trace = existing.trace;
        netscapeSchemaBug = existing.netscapeSchemaBug;

        initEnv();
    }

    public LdapContext newInstance(Control[] reqCtls) throws NamingException {

        LdapContext clone = new LdapCtx(this, currentDN);


        clone.setRequestControls(reqCtls);
        return clone;
    }


    protected void c_bind(Name name, Object obj, Continuation cont)
            throws NamingException {
        c_bind(name, obj, null, cont);
    }

    /*
     * attrs == null
     *      if obj is DirContext, attrs = obj.getAttributes()
     * if attrs == null && obj == null
     *      disallow (cannot determine objectclass to use)
     * if obj == null
     *      just create entry using attrs
     * else
     *      objAttrs = create attributes for representing obj
     *      attrs += objAttrs
     *      create entry using attrs
     */
    protected void c_bind(Name name, Object obj, Attributes attrs,
                          Continuation cont)
            throws NamingException {

        cont.setError(this, name);

        Attributes inputAttrs = attrs; 
        try {
            ensureOpen();

            if (obj == null) {
                if (attrs == null) {
                    throw new IllegalArgumentException(
                        "cannot bind null object with no attributes");
                }
            } else {
                attrs = Obj.determineBindAttrs(addrEncodingSeparator, obj, attrs,
                    false, name, this, envprops); 
            }

            String newDN = fullyQualifiedName(name);
            attrs = addRdnAttributes(newDN, attrs, inputAttrs != attrs);
            LdapEntry entry = new LdapEntry(newDN, attrs);

            LdapResult answer = clnt.add(entry, reqCtls);
            respCtls = answer.resControls; 

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                try {

                    refCtx.bind(name, obj, inputAttrs);
                    return;

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected void c_rebind(Name name, Object obj, Continuation cont)
            throws NamingException {
        c_rebind(name, obj, null, cont);
    }


    /*
     * attrs == null
     *    if obj is DirContext, attrs = obj.getAttributes().
     * if attrs == null
     *    leave any existing attributes alone
     *    (set attrs = {objectclass=top} if object doesn't exist)
     * else
     *    replace all existing attributes with attrs
     * if obj == null
     *      just create entry using attrs
     * else
     *      objAttrs = create attributes for representing obj
     *      attrs += objAttrs
     *      create entry using attrs
     */
    protected void c_rebind(Name name, Object obj, Attributes attrs,
        Continuation cont) throws NamingException {

        cont.setError(this, name);

        Attributes inputAttrs = attrs;

        try {
            Attributes origAttrs = null;

            try {
                origAttrs = c_getAttributes(name, null, cont);
            } catch (NameNotFoundException e) {}

            if (origAttrs == null) {
                c_bind(name, obj, attrs, cont);
                return;
            }


            if (attrs == null && obj instanceof DirContext) {
                attrs = ((DirContext)obj).getAttributes("");
            }
            Attributes keepAttrs = (Attributes)origAttrs.clone();

            if (attrs == null) {

                Attribute origObjectClass =
                    origAttrs.get(Obj.JAVA_ATTRIBUTES[Obj.OBJECT_CLASS]);

                if (origObjectClass != null) {
                    origObjectClass = (Attribute)origObjectClass.clone();
                    for (int i = 0; i < Obj.JAVA_OBJECT_CLASSES.length; i++) {
                        origObjectClass.remove(Obj.JAVA_OBJECT_CLASSES_LOWER[i]);
                        origObjectClass.remove(Obj.JAVA_OBJECT_CLASSES[i]);
                    }
                    origAttrs.put(origObjectClass);
                }

                for (int i = 1; i < Obj.JAVA_ATTRIBUTES.length; i++) {
                    origAttrs.remove(Obj.JAVA_ATTRIBUTES[i]);
                }

                attrs = origAttrs;
            }
            if (obj != null) {
                attrs =
                    Obj.determineBindAttrs(addrEncodingSeparator, obj, attrs,
                        inputAttrs != attrs, name, this, envprops);
            }

            String newDN = fullyQualifiedName(name);
            LdapResult answer = clnt.delete(newDN, reqCtls);
            respCtls = answer.resControls; 

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
                return;
            }

            Exception addEx = null;
            try {
                attrs = addRdnAttributes(newDN, attrs, inputAttrs != attrs);

                LdapEntry entry = new LdapEntry(newDN, attrs);
                answer = clnt.add(entry, reqCtls);
                if (answer.resControls != null) {
                    respCtls = appendVector(respCtls, answer.resControls);
                }
            } catch (NamingException | IOException ae) {
                addEx = ae;
            }

            if ((addEx != null && !(addEx instanceof LdapReferralException)) ||
                answer.status != LdapClient.LDAP_SUCCESS) {
                LdapResult answer2 =
                    clnt.add(new LdapEntry(newDN, keepAttrs), reqCtls);
                if (answer2.resControls != null) {
                    respCtls = appendVector(respCtls, answer2.resControls);
                }

                if (addEx == null) {
                    processReturnCode(answer, name);
                }
            }

            if (addEx instanceof NamingException) {
                throw (NamingException)addEx;
            } else if (addEx instanceof IOException) {
                throw (IOException)addEx;
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                try {

                    refCtx.rebind(name, obj, inputAttrs);
                    return;

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected void c_unbind(Name name, Continuation cont)
            throws NamingException {
        cont.setError(this, name);

        try {
            ensureOpen();

            String fname = fullyQualifiedName(name);
            LdapResult answer = clnt.delete(fname, reqCtls);
            respCtls = answer.resControls; 

            adjustDeleteStatus(fname, answer);

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                try {

                    refCtx.unbind(name);
                    return;

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected void c_rename(Name oldName, Name newName, Continuation cont)
            throws NamingException
    {
        Name oldParsed, newParsed;
        Name oldParent, newParent;
        String newRDN = null;
        String newSuperior = null;


        cont.setError(this, oldName);

        try {
            ensureOpen();

            if (oldName.isEmpty()) {
                oldParent = parser.parse("");
            } else {
                oldParsed = parser.parse(oldName.get(0)); 
                oldParent = oldParsed.getPrefix(oldParsed.size() - 1);
            }

            if (newName instanceof CompositeName) {
                newParsed = parser.parse(newName.get(0)); 
            } else {
                newParsed = newName; 
            }
            newParent = newParsed.getPrefix(newParsed.size() - 1);

            if(!oldParent.equals(newParent)) {
                if (!clnt.isLdapv3) {
                    throw new InvalidNameException(
                                  "LDAPv2 doesn't support changing " +
                                  "the parent as a result of a rename");
                } else {
                    newSuperior = fullyQualifiedName(newParent.toString());
                }
            }

            newRDN = newParsed.get(newParsed.size() - 1);

            LdapResult answer = clnt.moddn(fullyQualifiedName(oldName),
                                    newRDN,
                                    deleteRDN,
                                    newSuperior,
                                    reqCtls);
            respCtls = answer.resControls; 

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, oldName);
            }

        } catch (LdapReferralException e) {

            e.setNewRdn(newRDN);

            if (newSuperior != null) {
                PartialResultException pre = new PartialResultException(
                    "Cannot continue referral processing when newSuperior is " +
                    "nonempty: " + newSuperior);
                pre.setRootCause(cont.fillInException(e));
                throw cont.fillInException(pre);
            }

            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                try {

                    refCtx.rename(oldName, newName);
                    return;

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected Context c_createSubcontext(Name name, Continuation cont)
            throws NamingException {
        return c_createSubcontext(name, null, cont);
    }

    protected DirContext c_createSubcontext(Name name, Attributes attrs,
                                            Continuation cont)
            throws NamingException {
        cont.setError(this, name);

        Attributes inputAttrs = attrs;
        try {
            ensureOpen();
            if (attrs == null) {
                  Attribute oc = new BasicAttribute(
                      Obj.JAVA_ATTRIBUTES[Obj.OBJECT_CLASS],
                      Obj.JAVA_OBJECT_CLASSES[Obj.STRUCTURAL]);
                  oc.add("top");
                  attrs = new BasicAttributes(true); 
                  attrs.put(oc);
            }
            String newDN = fullyQualifiedName(name);
            attrs = addRdnAttributes(newDN, attrs, inputAttrs != attrs);

            LdapEntry entry = new LdapEntry(newDN, attrs);

            LdapResult answer = clnt.add(entry, reqCtls);
            respCtls = answer.resControls; 

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
                return null;
            }

            return new LdapCtx(this, newDN);

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                try {

                    return refCtx.createSubcontext(name, inputAttrs);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected void c_destroySubcontext(Name name, Continuation cont)
        throws NamingException {
        cont.setError(this, name);

        try {
            ensureOpen();

            String fname = fullyQualifiedName(name);
            LdapResult answer = clnt.delete(fname, reqCtls);
            respCtls = answer.resControls; 

            adjustDeleteStatus(fname, answer);

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                try {

                    refCtx.destroySubcontext(name);
                    return;
                } catch (LdapReferralException re) {
                    e = re;
                    continue;
                } finally {
                    refCtx.close();
                }
            }
        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);
        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    /**
     * Adds attributes from RDN to attrs if not already present.
     * Note that if attrs already contains an attribute by the same name,
     * or if the distinguished name is empty, then leave attrs unchanged.
     *
     * @param dn The non-null DN of the entry to add
     * @param attrs The non-null attributes of entry to add
     * @param directUpdate Whether attrs can be updated directly
     * @return Non-null attributes with attributes from the RDN added
     */
    private static Attributes addRdnAttributes(String dn, Attributes attrs,
        boolean directUpdate) throws NamingException {

            if (dn.isEmpty()) {
                return attrs;
            }

            List<Rdn> rdnList = (new LdapName(dn)).getRdns();

            Rdn rdn = rdnList.get(rdnList.size() - 1);
            Attributes nameAttrs = rdn.toAttributes();

            NamingEnumeration<? extends Attribute> enum_ = nameAttrs.getAll();
            Attribute nameAttr;
            while (enum_.hasMore()) {
                nameAttr = enum_.next();

                if (attrs.get(nameAttr.getID()) ==  null) {

                    /**
                     * When attrs.isCaseIgnored() is false, attrs.get() will
                     * return null when the case mis-matches for otherwise
                     * equal attrIDs.
                     * As the attrIDs' case is irrelevant for LDAP, ignore
                     * the case of attrIDs even when attrs.isCaseIgnored() is
                     * false. This is done by explicitly comparing the elements in
                     * the enumeration of IDs with their case ignored.
                     */
                    if (!attrs.isCaseIgnored() &&
                            containsIgnoreCase(attrs.getIDs(), nameAttr.getID())) {
                        continue;
                    }

                    if (!directUpdate) {
                        attrs = (Attributes)attrs.clone();
                        directUpdate = true;
                    }
                    attrs.put(nameAttr);
                }
            }

            return attrs;
    }


    private static boolean containsIgnoreCase(NamingEnumeration<String> enumStr,
                                String str) throws NamingException {
        String strEntry;

        while (enumStr.hasMore()) {
             strEntry = enumStr.next();
             if (strEntry.equalsIgnoreCase(str)) {
                return true;
             }
        }
        return false;
    }


    private void adjustDeleteStatus(String fname, LdapResult answer) {
        if (answer.status == LdapClient.LDAP_NO_SUCH_OBJECT &&
            answer.matchedDN != null) {
            try {

                Name orig = parser.parse(fname);
                Name matched = parser.parse(answer.matchedDN);
                if ((orig.size() - matched.size()) == 1)
                    answer.status = LdapClient.LDAP_SUCCESS;
            } catch (NamingException e) {}
        }
    }

    /*
     * Append the second Vector onto the first Vector
     * (v2 must be non-null)
     */
    private static <T> Vector<T> appendVector(Vector<T> v1, Vector<T> v2) {
        if (v1 == null) {
            v1 = v2;
        } else {
            for (int i = 0; i < v2.size(); i++) {
                v1.addElement(v2.elementAt(i));
            }
        }
        return v1;
    }


    protected Object c_lookupLink(Name name, Continuation cont)
            throws NamingException {
        return c_lookup(name, cont);
    }

    protected Object c_lookup(Name name, Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        Object obj = null;
        Attributes attrs;

        try {
            SearchControls cons = new SearchControls();
            cons.setSearchScope(SearchControls.OBJECT_SCOPE);
            cons.setReturningAttributes(null); 
            cons.setReturningObjFlag(true); 

            LdapResult answer = doSearchOnce(name, "(objectClass=*)", cons, true);
            respCtls = answer.resControls; 


            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
            }

            if (answer.entries == null || answer.entries.size() != 1) {
                attrs = new BasicAttributes(LdapClient.caseIgnore);
            } else {
                LdapEntry entry = answer.entries.elementAt(0);
                attrs = entry.attributes;

                Vector<Control> entryCtls = entry.respCtls; 
                if (entryCtls != null) {
                    appendVector(respCtls, entryCtls); 
                }
            }

            if (attrs.get(Obj.JAVA_ATTRIBUTES[Obj.CLASSNAME]) != null) {
                obj = Obj.decodeObject(attrs);
            }
            if (obj == null) {
                obj = new LdapCtx(this, fullyQualifiedName(name));
            }
        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);
                try {

                    return refCtx.lookup(name);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }

        try {
            return NamingManagerHelper.getDirObjectInstance(obj, name, this,
                    envprops, attrs, ObjectFactoriesFilter::checkLdapFilter);

        } catch (NamingException e) {
            throw cont.fillInException(e);

        } catch (Exception e) {
            NamingException e2 = new NamingException(
                    "problem generating object using object factory");
            e2.setRootCause(e);
            throw cont.fillInException(e2);
        }
    }

    protected NamingEnumeration<NameClassPair> c_list(Name name, Continuation cont)
            throws NamingException {
        SearchControls cons = new SearchControls();
        String[] classAttrs = new String[2];

        classAttrs[0] = Obj.JAVA_ATTRIBUTES[Obj.OBJECT_CLASS];
        classAttrs[1] = Obj.JAVA_ATTRIBUTES[Obj.CLASSNAME];
        cons.setReturningAttributes(classAttrs);

        cons.setReturningObjFlag(true);

        cont.setError(this, name);

        LdapResult answer = null;

        try {
            answer = doSearch(name, "(objectClass=*)", cons, true, true);

            if ((answer.status != LdapClient.LDAP_SUCCESS) ||
                (answer.referrals != null)) {
                processReturnCode(answer, name);
            }

            return new LdapNamingEnumeration(this, answer, name, cont);

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                try {

                    return refCtx.list(name);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }

        } catch (LimitExceededException e) {
            LdapNamingEnumeration res =
                new LdapNamingEnumeration(this, answer, name, cont);

            res.setNamingException(
                    (LimitExceededException)cont.fillInException(e));
            return res;

        } catch (PartialResultException e) {
            LdapNamingEnumeration res =
                new LdapNamingEnumeration(this, answer, name, cont);

            res.setNamingException(
                    (PartialResultException)cont.fillInException(e));
            return res;

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected NamingEnumeration<Binding> c_listBindings(Name name, Continuation cont)
            throws NamingException {

        SearchControls cons = new SearchControls();
        cons.setReturningAttributes(null); 
        cons.setReturningObjFlag(true); 

        cont.setError(this, name);

        LdapResult answer = null;

        try {
            answer = doSearch(name, "(objectClass=*)", cons, true, true);

            if ((answer.status != LdapClient.LDAP_SUCCESS) ||
                (answer.referrals != null)) {
                processReturnCode(answer, name);
            }

            return new LdapBindingEnumeration(this, answer, name, cont);

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {
                @SuppressWarnings("unchecked")
                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                try {

                    return refCtx.listBindings(name);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }
        } catch (LimitExceededException e) {
            LdapBindingEnumeration res =
                new LdapBindingEnumeration(this, answer, name, cont);

            res.setNamingException(cont.fillInException(e));
            return res;

        } catch (PartialResultException e) {
            LdapBindingEnumeration res =
                new LdapBindingEnumeration(this, answer, name, cont);

            res.setNamingException(cont.fillInException(e));
            return res;

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }


    protected NameParser c_getNameParser(Name name, Continuation cont)
            throws NamingException
    {
        cont.setSuccess();
        return parser;
    }

    public String getNameInNamespace() {
        return currentDN;
    }

    public Name composeName(Name name, Name prefix)
        throws NamingException
    {
        Name result;

        if ((name instanceof LdapName) && (prefix instanceof LdapName)) {
            result = (Name)(prefix.clone());
            result.addAll(name);
            return new CompositeName().add(result.toString());
        }
        if (!(name instanceof CompositeName)) {
            name = new CompositeName().add(name.toString());
        }
        if (!(prefix instanceof CompositeName)) {
            prefix = new CompositeName().add(prefix.toString());
        }

        int prefixLast = prefix.size() - 1;

        if (name.isEmpty() || prefix.isEmpty() ||
                name.get(0).isEmpty() || prefix.get(prefixLast).isEmpty()) {
            return super.composeName(name, prefix);
        }

        result = (Name)(prefix.clone());
        result.addAll(name);

        if (parentIsLdapCtx) {
            String ldapComp = concatNames(result.get(prefixLast + 1),
                                          result.get(prefixLast));
            result.remove(prefixLast + 1);
            result.remove(prefixLast);
            result.add(prefixLast, ldapComp);
        }
        return result;
    }

    private String fullyQualifiedName(Name rel) {
        return rel.isEmpty()
                ? currentDN
                : fullyQualifiedName(rel.get(0));
    }

    private String fullyQualifiedName(String rel) {
        return (concatNames(rel, currentDN));
    }

    private static String concatNames(String lesser, String greater) {
        if (lesser == null || lesser.isEmpty()) {
            return greater;
        } else if (greater == null || greater.isEmpty()) {
            return lesser;
        } else {
            return (lesser + "," + greater);
        }
    }


    protected Attributes c_getAttributes(Name name, String[] attrIds,
                                      Continuation cont)
            throws NamingException {
        cont.setError(this, name);

        SearchControls cons = new SearchControls();
        cons.setSearchScope(SearchControls.OBJECT_SCOPE);
        cons.setReturningAttributes(attrIds);

        try {
            LdapResult answer =
                doSearchOnce(name, "(objectClass=*)", cons, true);
            respCtls = answer.resControls; 

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
            }

            if (answer.entries == null || answer.entries.size() != 1) {
                return new BasicAttributes(LdapClient.caseIgnore);
            }

            LdapEntry entry = answer.entries.elementAt(0);

            Vector<Control> entryCtls = entry.respCtls; 
            if (entryCtls != null) {
                appendVector(respCtls, entryCtls); 
            }

            setParents(entry.attributes, (Name) name.clone());

            return (entry.attributes);

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                try {

                    return refCtx.getAttributes(name, attrIds);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected void c_modifyAttributes(Name name, int mod_op, Attributes attrs,
                                      Continuation cont)
            throws NamingException {

        cont.setError(this, name);

        try {
            ensureOpen();

            if (attrs == null || attrs.size() == 0) {
                return; 
            }
            String newDN = fullyQualifiedName(name);
            int jmod_op = convertToLdapModCode(mod_op);

            int[] jmods = new int[attrs.size()];
            Attribute[] jattrs = new Attribute[attrs.size()];

            NamingEnumeration<? extends Attribute> ae = attrs.getAll();
            for(int i = 0; i < jmods.length && ae.hasMore(); i++) {
                jmods[i] = jmod_op;
                jattrs[i] = ae.next();
            }

            LdapResult answer = clnt.modify(newDN, jmods, jattrs, reqCtls);
            respCtls = answer.resControls; 

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
                return;
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                try {

                    refCtx.modifyAttributes(name, mod_op, attrs);
                    return;

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected void c_modifyAttributes(Name name, ModificationItem[] mods,
                                      Continuation cont)
            throws NamingException {
        cont.setError(this, name);

        try {
            ensureOpen();

            if (mods == null || mods.length == 0) {
                return; 
            }
            String newDN = fullyQualifiedName(name);

            int[] jmods = new int[mods.length];
            Attribute[] jattrs = new Attribute[mods.length];
            ModificationItem mod;
            for (int i = 0; i < jmods.length; i++) {
                mod = mods[i];
                jmods[i] = convertToLdapModCode(mod.getModificationOp());
                jattrs[i] = mod.getAttribute();
            }

            LdapResult answer = clnt.modify(newDN, jmods, jattrs, reqCtls);
            respCtls = answer.resControls; 

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                try {

                    refCtx.modifyAttributes(name, mods);
                    return;

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    private static int convertToLdapModCode(int mod_op) {
        switch (mod_op) {
        case DirContext.ADD_ATTRIBUTE:
            return(LdapClient.ADD);

        case DirContext.REPLACE_ATTRIBUTE:
            return (LdapClient.REPLACE);

        case DirContext.REMOVE_ATTRIBUTE:
            return (LdapClient.DELETE);

        default:
            throw new IllegalArgumentException("Invalid modification code");
        }
    }


    protected DirContext c_getSchema(Name name, Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        try {
            return getSchemaTree(name);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected DirContext c_getSchemaClassDefinition(Name name,
                                                    Continuation cont)
            throws NamingException {
        cont.setError(this, name);

        try {
            Attribute objectClassAttr = c_getAttributes(name,
                new String[]{"objectclass"}, cont).get("objectclass");
            if (objectClassAttr == null || objectClassAttr.size() == 0) {
                return EMPTY_SCHEMA;
            }

            Context ocSchema = (Context) c_getSchema(name, cont).lookup(
                LdapSchemaParser.OBJECTCLASS_DEFINITION_NAME);

            HierMemDirCtx objectClassCtx = new HierMemDirCtx();
            DirContext objectClassDef;
            String objectClassName;
            for (Enumeration<?> objectClasses = objectClassAttr.getAll();
                objectClasses.hasMoreElements(); ) {
                objectClassName = (String)objectClasses.nextElement();
                objectClassDef = (DirContext)ocSchema.lookup(objectClassName);
                objectClassCtx.bind(objectClassName, objectClassDef);
            }

            objectClassCtx.setReadOnly(
                new SchemaViolationException("Cannot update schema object"));
            return (DirContext)objectClassCtx;

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    /*
     * getSchemaTree first looks to see if we have already built a
     * schema tree for the given entry. If not, it builds a new one and
     * stores it in our private hash table
     */
    private DirContext getSchemaTree(Name name) throws NamingException {
        String subschemasubentry = getSchemaEntry(name, true);

        DirContext schemaTree = schemaTrees.get(subschemasubentry);

        if(schemaTree==null) {
            if(debug){System.err.println("LdapCtx: building new schema tree " + this);}
            schemaTree = buildSchemaTree(subschemasubentry);
            schemaTrees.put(subschemasubentry, schemaTree);
        }

        return schemaTree;
    }

    /*
     * buildSchemaTree builds the schema tree corresponding to the
     * given subschemasubentree
     */
    private DirContext buildSchemaTree(String subschemasubentry)
        throws NamingException {

        SearchControls constraints = new
            SearchControls(SearchControls.OBJECT_SCOPE,
                0, 0, /* count and time limits */
                SCHEMA_ATTRIBUTES /* return schema attrs */,
                true /* return obj */,
                false /*deref link */ );

        Name sse = (new CompositeName()).add(subschemasubentry);
        NamingEnumeration<SearchResult> results =
            searchAux(sse, "(objectClass=subschema)", constraints,
            false, true, new Continuation());

        if(!results.hasMore()) {
            throw new OperationNotSupportedException(
                "Cannot get read subschemasubentry: " + subschemasubentry);
        }
        SearchResult result = results.next();
        results.close();

        Object obj = result.getObject();
        if(!(obj instanceof LdapCtx)) {
            throw new NamingException(
                "Cannot get schema object as DirContext: " + subschemasubentry);
        }

        return LdapSchemaCtx.createSchemaTree(envprops, subschemasubentry,
            (LdapCtx)obj /* schema entry */,
            result.getAttributes() /* schema attributes */,
            netscapeSchemaBug);
   }

    /*
     * getSchemaEntree returns the DN of the subschemasubentree for the
     * given entree. It first looks to see if the given entry has
     * a subschema different from that of the root DIT (by looking for
     * a "subschemasubentry" attribute). If it doesn't find one, it returns
     * the one for the root of the DIT (by looking for the root's
     * "subschemasubentry" attribute).
     *
     * This function is called regardless of the server's version, since
     * an administrator may have setup the server to support client schema
     * queries. If this function tries a search on a v2 server that
     * doesn't support schema, one of these two things will happen:
     * 1) It will get an exception when querying the root DSE
     * 2) It will not find a subschemasubentry on the root DSE
     * If either of these things occur and the server is not v3, we
     * throw OperationNotSupported.
     *
     * the relative flag tells whether the given name is relative to this
     * context.
     */
    private String getSchemaEntry(Name name, boolean relative)
        throws NamingException {

        SearchControls constraints = new SearchControls(SearchControls.OBJECT_SCOPE,
            0, 0, /* count and time limits */
            new String[]{"subschemasubentry"} /* attr to return */,
            false /* returning obj */,
            false /* deref link */);

        NamingEnumeration<SearchResult> results;
        try {
            results = searchAux(name, "objectclass=*", constraints, relative,
                true, new Continuation());

        } catch (NamingException ne) {
            if (!clnt.isLdapv3 && currentDN.length() == 0 && name.isEmpty()) {
                throw new OperationNotSupportedException(
                    "Cannot get schema information from server");
            } else {
                throw ne;
            }
        }

        if (!results.hasMoreElements()) {
            throw new ConfigurationException(
                "Requesting schema of nonexistent entry: " + name);
        }

        SearchResult result = results.next();
        results.close();

        Attribute schemaEntryAttr =
            result.getAttributes().get("subschemasubentry");

        if (schemaEntryAttr == null || schemaEntryAttr.size() < 0) {
            if (currentDN.length() == 0 && name.isEmpty()) {
                throw new OperationNotSupportedException(
                    "Cannot read subschemasubentry of root DSE");
            } else {
                return getSchemaEntry(new CompositeName(), false);
            }
        }

        return (String)(schemaEntryAttr.get()); 
    }

    void setParents(Attributes attrs, Name name) throws NamingException {
        NamingEnumeration<? extends Attribute> ae = attrs.getAll();
        while(ae.hasMore()) {
            ((LdapAttribute) ae.next()).setParent(this, name);
        }
    }

    /*
     * Returns the URL associated with this context; used by LdapAttribute
     * after deserialization to get pointer to this context.
     */
    String getURL() {
        if (url == null) {
            url = LdapURL.toUrlString(hostname, port_number, currentDN,
                hasLdapsScheme);
        }

        return url;
    }

    protected NamingEnumeration<SearchResult> c_search(Name name,
                                         Attributes matchingAttributes,
                                         Continuation cont)
            throws NamingException {
        return c_search(name, matchingAttributes, null, cont);
    }

    protected NamingEnumeration<SearchResult> c_search(Name name,
                                         Attributes matchingAttributes,
                                         String[] attributesToReturn,
                                         Continuation cont)
            throws NamingException {
        SearchControls cons = new SearchControls();
        cons.setReturningAttributes(attributesToReturn);
        String filter;
        try {
            filter = SearchFilter.format(matchingAttributes);
        } catch (NamingException e) {
            cont.setError(this, name);
            throw cont.fillInException(e);
        }
        return c_search(name, filter, cons, cont);
    }

    protected NamingEnumeration<SearchResult> c_search(Name name,
                                         String filter,
                                         SearchControls cons,
                                         Continuation cont)
            throws NamingException {
        return searchAux(name, filter, cloneSearchControls(cons), true,
                 waitForReply, cont);
    }

    protected NamingEnumeration<SearchResult> c_search(Name name,
                                         String filterExpr,
                                         Object[] filterArgs,
                                         SearchControls cons,
                                         Continuation cont)
            throws NamingException {
        String strfilter;
        try {
            strfilter = SearchFilter.format(filterExpr, filterArgs);
        } catch (NamingException e) {
            cont.setError(this, name);
            throw cont.fillInException(e);
        }
        return c_search(name, strfilter, cons, cont);
    }

    NamingEnumeration<SearchResult> searchAux(Name name,
        String filter,
        SearchControls cons,
        boolean relative,
        boolean waitForReply, Continuation cont) throws NamingException {

        LdapResult answer = null;
        String[] tokens = new String[2];    
        String[] reqAttrs;                  

        if (cons == null) {
            cons = new SearchControls();
        }
        reqAttrs = cons.getReturningAttributes();

        if (cons.getReturningObjFlag()) {
            if (reqAttrs != null) {

                boolean hasWildcard = false;
                for (int i = reqAttrs.length - 1; i >= 0; i--) {
                    if (reqAttrs[i].equals("*")) {
                        hasWildcard = true;
                        break;
                    }
                }
                if (! hasWildcard) {
                    String[] totalAttrs =
                        new String[reqAttrs.length +Obj.JAVA_ATTRIBUTES.length];
                    System.arraycopy(reqAttrs, 0, totalAttrs, 0,
                        reqAttrs.length);
                    System.arraycopy(Obj.JAVA_ATTRIBUTES, 0, totalAttrs,
                        reqAttrs.length, Obj.JAVA_ATTRIBUTES.length);

                    cons.setReturningAttributes(totalAttrs);
                }
            }
        }

        LdapCtx.SearchArgs args =
            new LdapCtx.SearchArgs(name, filter, cons, reqAttrs);

        cont.setError(this, name);
        try {
            if (searchToCompare(filter, cons, tokens)){
                answer = compare(name, tokens[0], tokens[1]);
                if (! (answer.compareToSearchResult(fullyQualifiedName(name)))){
                    processReturnCode(answer, name);
                }
            } else {
                answer = doSearch(name, filter, cons, relative, waitForReply);
                processReturnCode(answer, name);
            }
            return new LdapSearchEnumeration(this, answer,
                                             fullyQualifiedName(name),
                                             args, cont);

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            while (true) {

                @SuppressWarnings("unchecked")
                LdapReferralContext refCtx = (LdapReferralContext)
                        e.getReferralContext(envprops, bindCtls);

                try {

                    return refCtx.search(name, filter, cons);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }

        } catch (LimitExceededException e) {
            LdapSearchEnumeration res =
                new LdapSearchEnumeration(this, answer, fullyQualifiedName(name),
                                          args, cont);
            res.setNamingException(e);
            return res;

        } catch (PartialResultException e) {
            LdapSearchEnumeration res =
                new LdapSearchEnumeration(this, answer, fullyQualifiedName(name),
                                          args, cont);

            res.setNamingException(e);
            return res;

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }


    LdapResult getSearchReply(LdapClient eClnt, LdapResult res)
            throws NamingException {

        if (clnt != eClnt) {
            throw new CommunicationException(
                "Context's connection changed; unable to continue enumeration");
        }

        try {
            return eClnt.getSearchReply(batchSize, res, binaryAttrs);
        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw e2;
        }
    }

    private LdapResult doSearchOnce(Name name, String filter,
        SearchControls cons, boolean relative) throws NamingException {

        int savedBatchSize = batchSize;
        batchSize = 2; 

        LdapResult answer = doSearch(name, filter, cons, relative, true);

        batchSize = savedBatchSize;
        return answer;
    }

    private LdapResult doSearch(Name name, String filter, SearchControls cons,
        boolean relative, boolean waitForReply) throws NamingException {
            ensureOpen();
            try {
                int scope;

                switch (cons.getSearchScope()) {
                case SearchControls.OBJECT_SCOPE:
                    scope = LdapClient.SCOPE_BASE_OBJECT;
                    break;
                default:
                case SearchControls.ONELEVEL_SCOPE:
                    scope = LdapClient.SCOPE_ONE_LEVEL;
                    break;
                case SearchControls.SUBTREE_SCOPE:
                    scope = LdapClient.SCOPE_SUBTREE;
                    break;
                }


                String[] retattrs = cons.getReturningAttributes();
                if (retattrs != null && retattrs.length == 0) {
                    retattrs = new String[1];
                    retattrs[0] = "1.1";
                }

                String nm = (relative
                             ? fullyQualifiedName(name)
                             : (name.isEmpty()
                                ? ""
                                : name.get(0)));

                int msecLimit = cons.getTimeLimit();
                int secLimit = 0;

                if (msecLimit > 0) {
                    secLimit = (msecLimit / 1000) + 1;
                }

                LdapResult answer =
                    clnt.search(nm,
                        scope,
                        derefAliases,
                        (int)cons.getCountLimit(),
                        secLimit,
                        cons.getReturningObjFlag() ? false : typesOnly,
                        retattrs,
                        filter,
                        batchSize,
                        reqCtls,
                        binaryAttrs,
                        waitForReply,
                        replyQueueSize);
                respCtls = answer.resControls; 
                return answer;

            } catch (IOException e) {
                NamingException e2 = new CommunicationException(e.getMessage());
                e2.setRootCause(e);
                throw e2;
            }
    }


    /*
     * Certain simple JNDI searches are automatically converted to
     * LDAP compare operations by the LDAP service provider. A search
     * is converted to a compare iff:
     *
     *    - the scope is set to OBJECT_SCOPE
     *    - the filter string contains a simple assertion: "<type>=<value>"
     *    - the returning attributes list is present but empty
     */


    private static boolean searchToCompare(
                                    String filter,
                                    SearchControls cons,
                                    String tokens[]) {

        if (cons.getSearchScope() != SearchControls.OBJECT_SCOPE) {
            return false;
        }

        String[] attrs = cons.getReturningAttributes();
        if (attrs == null || attrs.length != 0) {
            return false;
        }

        if (! filterToAssertion(filter, tokens)) {
            return false;
        }

        return true;
    }


    private static boolean filterToAssertion(String filter, String tokens[]) {

        StringTokenizer assertionTokenizer = new StringTokenizer(filter, "=");

        if (assertionTokenizer.countTokens() != 2) {
            return false;
        }

        tokens[0] = assertionTokenizer.nextToken();
        tokens[1] = assertionTokenizer.nextToken();

        if (tokens[1].indexOf('*') != -1) {
            return false;
        }

        boolean hasParens = false;
        int len = tokens[1].length();

        if ((tokens[0].charAt(0) == '(') &&
            (tokens[1].charAt(len - 1) == ')')) {
            hasParens = true;

        } else if ((tokens[0].charAt(0) == '(') ||
            (tokens[1].charAt(len - 1) == ')')) {
            return false; 
        }

        StringTokenizer illegalCharsTokenizer =
            new StringTokenizer(tokens[0], "()&|!=~><*", true);

        if (illegalCharsTokenizer.countTokens() != (hasParens ? 2 : 1)) {
            return false;
        }

        illegalCharsTokenizer =
            new StringTokenizer(tokens[1], "()&|!=~><*", true);

        if (illegalCharsTokenizer.countTokens() != (hasParens ? 2 : 1)) {
            return false;
        }

        if (hasParens) {
            tokens[0] = tokens[0].substring(1);
            tokens[1] = tokens[1].substring(0, len - 1);
        }

        return true;
    }

    private LdapResult compare(Name name, String type, String value)
        throws IOException, NamingException {

        ensureOpen();
        String nm = fullyQualifiedName(name);

        LdapResult answer = clnt.compare(nm, type, value, reqCtls);
        respCtls = answer.resControls; 

        return answer;
    }

    private static SearchControls cloneSearchControls(SearchControls cons) {
        if (cons == null) {
            return null;
        }
        String[] retAttrs = cons.getReturningAttributes();
        if (retAttrs != null) {
            String[] attrs = new String[retAttrs.length];
            System.arraycopy(retAttrs, 0, attrs, 0, retAttrs.length);
            retAttrs = attrs;
        }
        return new SearchControls(cons.getSearchScope(),
                                  cons.getCountLimit(),
                                  cons.getTimeLimit(),
                                  retAttrs,
                                  cons.getReturningObjFlag(),
                                  cons.getDerefLinkFlag());
    }


    /**
     * Override with noncloning version.
     */
    protected Hashtable<String, Object> p_getEnvironment() {
        return envprops;
    }

    @SuppressWarnings("unchecked") 
    public Hashtable<String, Object> getEnvironment() throws NamingException {
        return (envprops == null
                ? new Hashtable<String, Object>(5, 0.75f)
                : (Hashtable<String, Object>)envprops.clone());
    }

    @SuppressWarnings("unchecked") 
    public Object removeFromEnvironment(String propName)
        throws NamingException {

        if (envprops == null || envprops.get(propName) == null) {
            return null;
        }
        switch (propName) {
            case REF_SEPARATOR:
                addrEncodingSeparator = DEFAULT_REF_SEPARATOR;
                break;
            case TYPES_ONLY:
                typesOnly = DEFAULT_TYPES_ONLY;
                break;
            case DELETE_RDN:
                deleteRDN = DEFAULT_DELETE_RDN;
                break;
            case DEREF_ALIASES:
                derefAliases = DEFAULT_DEREF_ALIASES;
                break;
            case Context.BATCHSIZE:
                batchSize = DEFAULT_BATCH_SIZE;
                break;
            case REFERRAL_LIMIT:
                referralHopLimit = DEFAULT_REFERRAL_LIMIT;
                break;
            case Context.REFERRAL:
                setReferralMode(null, true);
                break;
            case BINARY_ATTRIBUTES:
                setBinaryAttributes(null);
                break;
            case CONNECT_TIMEOUT:
                connectTimeout = -1;
                break;
            case READ_TIMEOUT:
                readTimeout = -1;
                break;
            case WAIT_FOR_REPLY:
                waitForReply = true;
                break;
            case REPLY_QUEUE_SIZE:
                replyQueueSize = -1;
                break;


            case Context.SECURITY_PROTOCOL:
                closeConnection(SOFT_CLOSE);
                if (useSsl && !hasLdapsScheme) {
                    useSsl = false;
                    url = null;
                    if (useDefaultPortNumber) {
                        port_number = DEFAULT_PORT;
                    }
                }
                break;
            case VERSION:
            case SOCKET_FACTORY:
                closeConnection(SOFT_CLOSE);
                break;
            case Context.SECURITY_AUTHENTICATION:
            case Context.SECURITY_PRINCIPAL:
            case Context.SECURITY_CREDENTIALS:
                sharable = false;
                break;
        }

        envprops = (Hashtable<String, Object>)envprops.clone();
        return envprops.remove(propName);
    }

    @SuppressWarnings("unchecked") 
    public Object addToEnvironment(String propName, Object propVal)
        throws NamingException {

            if (propVal == null) {
                return removeFromEnvironment(propName);
            }
            switch (propName) {
                case REF_SEPARATOR:
                    setRefSeparator((String)propVal);
                    break;
                case TYPES_ONLY:
                    setTypesOnly((String)propVal);
                    break;
                case DELETE_RDN:
                    setDeleteRDN((String)propVal);
                    break;
                case DEREF_ALIASES:
                    setDerefAliases((String)propVal);
                    break;
                case Context.BATCHSIZE:
                    setBatchSize((String)propVal);
                    break;
                case REFERRAL_LIMIT:
                    setReferralLimit((String)propVal);
                    break;
                case Context.REFERRAL:
                    setReferralMode((String)propVal, true);
                    break;
                case BINARY_ATTRIBUTES:
                    setBinaryAttributes((String)propVal);
                    break;
                case CONNECT_TIMEOUT:
                    setConnectTimeout((String)propVal);
                    break;
                case READ_TIMEOUT:
                    setReadTimeout((String)propVal);
                    break;
                case WAIT_FOR_REPLY:
                    setWaitForReply((String)propVal);
                    break;
                case REPLY_QUEUE_SIZE:
                    setReplyQueueSize((String)propVal);
                    break;


                case Context.SECURITY_PROTOCOL:
                    closeConnection(SOFT_CLOSE);
                    if ("ssl".equals(propVal)) {
                        useSsl = true;
                        url = null;
                        if (useDefaultPortNumber) {
                            port_number = DEFAULT_SSL_PORT;
                        }
                    }
                    break;
                case VERSION:
                case SOCKET_FACTORY:
                    closeConnection(SOFT_CLOSE);
                    break;
                case Context.SECURITY_AUTHENTICATION:
                case Context.SECURITY_PRINCIPAL:
                case Context.SECURITY_CREDENTIALS:
                    sharable = false;
                    break;
            }

            envprops = (envprops == null
                ? new Hashtable<String, Object>(5, 0.75f)
                : (Hashtable<String, Object>)envprops.clone());
            return envprops.put(propName, propVal);
    }

    /**
     * Sets the URL that created the context in the java.naming.provider.url
     * property.
     */
    void setProviderUrl(String providerUrl) { 
        if (envprops != null) {
            envprops.put(Context.PROVIDER_URL, providerUrl);
        }
    }

    /**
     * Sets the domain name for the context in the com.sun.jndi.ldap.domainname
     * property.
     * Used for hostname verification by Start TLS
     */
    void setDomainName(String domainName) { 
        if (envprops != null) {
            envprops.put(DOMAIN_NAME, domainName);
        }
    }

    private void initEnv() throws NamingException {
        if (envprops == null) {
            setReferralMode(null, false);
            return;
        }

        setBatchSize((String)envprops.get(Context.BATCHSIZE));

        setRefSeparator((String)envprops.get(REF_SEPARATOR));

        setDeleteRDN((String)envprops.get(DELETE_RDN));

        setTypesOnly((String)envprops.get(TYPES_ONLY));

        setDerefAliases((String)envprops.get(DEREF_ALIASES));

        setReferralLimit((String)envprops.get(REFERRAL_LIMIT));

        setBinaryAttributes((String)envprops.get(BINARY_ATTRIBUTES));

        bindCtls = cloneControls((Control[]) envprops.get(BIND_CONTROLS));

        setReferralMode((String)envprops.get(Context.REFERRAL), false);

        setConnectTimeout((String)envprops.get(CONNECT_TIMEOUT));

        setReadTimeout((String)envprops.get(READ_TIMEOUT));

        setWaitForReply((String)envprops.get(WAIT_FOR_REPLY));

        setReplyQueueSize((String)envprops.get(REPLY_QUEUE_SIZE));

    }

    private void setDeleteRDN(String deleteRDNProp) {
        if ((deleteRDNProp != null) &&
            (deleteRDNProp.equalsIgnoreCase("false"))) {
            deleteRDN = false;
        } else {
            deleteRDN = DEFAULT_DELETE_RDN;
        }
    }

    private void setTypesOnly(String typesOnlyProp) {
        if ((typesOnlyProp != null) &&
            (typesOnlyProp.equalsIgnoreCase("true"))) {
            typesOnly = true;
        } else {
            typesOnly = DEFAULT_TYPES_ONLY;
        }
    }

    /**
     * Sets the batch size of this context;
     */
    private void setBatchSize(String batchSizeProp) {
        if (batchSizeProp != null) {
            batchSize = Integer.parseInt(batchSizeProp);
        } else {
            batchSize = DEFAULT_BATCH_SIZE;
        }
    }

    /**
     * Sets the referral mode of this context to 'follow', 'throw' or 'ignore'.
     * If referral mode is 'ignore' then activate the manageReferral control.
     */
    private void setReferralMode(String ref, boolean update) {
        if (ref != null) {
            switch (ref) {
                case "follow-scheme":
                    handleReferrals = LdapClient.LDAP_REF_FOLLOW_SCHEME;
                    break;
                case "follow":
                    handleReferrals = LdapClient.LDAP_REF_FOLLOW;
                    break;
                case "throw":
                    handleReferrals = LdapClient.LDAP_REF_THROW;
                    break;
                case "ignore":
                    handleReferrals = LdapClient.LDAP_REF_IGNORE;
                    break;
                default:
                    throw new IllegalArgumentException(
                        "Illegal value for " + Context.REFERRAL + " property.");
            }
        } else {
            handleReferrals = DEFAULT_REFERRAL_MODE;
        }

        if (handleReferrals == LdapClient.LDAP_REF_IGNORE) {
            reqCtls = addControl(reqCtls, manageReferralControl);

        } else if (update) {

            reqCtls = removeControl(reqCtls, manageReferralControl);

        } 
    }

    /**
     * Set whether aliases are dereferenced during resolution and searches.
     */
    private void setDerefAliases(String deref) {
        if (deref != null) {
            switch (deref) {
                case "never":
                    derefAliases = 0; 
                    break;
                case "searching":
                    derefAliases = 1; 
                    break;
                case "finding":
                    derefAliases = 2; 
                    break;
                case "always":
                    derefAliases = 3; 
                    break;
                default:
                    throw new IllegalArgumentException("Illegal value for " +
                        DEREF_ALIASES + " property.");
            }
        } else {
            derefAliases = DEFAULT_DEREF_ALIASES;
        }
    }

    private void setRefSeparator(String sepStr) throws NamingException {
        if (sepStr != null && sepStr.length() > 0) {
            addrEncodingSeparator = sepStr.charAt(0);
        } else {
            addrEncodingSeparator = DEFAULT_REF_SEPARATOR;
        }
    }

    /**
     * Sets the limit on referral chains
     */
    private void setReferralLimit(String referralLimitProp) {
        if (referralLimitProp != null) {
            referralHopLimit = Integer.parseInt(referralLimitProp);

            if (referralHopLimit == 0)
                referralHopLimit = Integer.MAX_VALUE;
        } else {
            referralHopLimit = DEFAULT_REFERRAL_LIMIT;
        }
    }

    void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    /**
     * Sets the connect timeout value
     */
    private void setConnectTimeout(String connectTimeoutProp) {
        if (connectTimeoutProp != null) {
            connectTimeout = Integer.parseInt(connectTimeoutProp);
        } else {
            connectTimeout = -1;
        }
    }

    /**
     * Sets the size of the queue of unprocessed search replies
     */
    private void setReplyQueueSize(String replyQueueSizeProp) {
        if (replyQueueSizeProp != null) {
           replyQueueSize = Integer.parseInt(replyQueueSizeProp);
            if (replyQueueSize <= 0) {
                replyQueueSize = -1;    
            }
        } else {
            replyQueueSize = -1;        
        }
    }

    /**
     * Sets the flag that controls whether to block until the first search
     * reply is received
     */
    private void setWaitForReply(String waitForReplyProp) {
        if (waitForReplyProp != null &&
            (waitForReplyProp.equalsIgnoreCase("false"))) {
            waitForReply = false;
        } else {
            waitForReply = true;
        }
    }

    /**
     * Sets the read timeout value
     */
    private void setReadTimeout(String readTimeoutProp) {
        if (readTimeoutProp != null) {
           readTimeout = Integer.parseInt(readTimeoutProp);
        } else {
            readTimeout = -1;
        }
    }

    /*
     * Extract URLs from a string. The format of the string is:
     *
     *     <urlstring > ::= "Referral:" <ldapurls>
     *     <ldapurls>   ::= <separator> <ldapurl> | <ldapurls>
     *     <separator>  ::= ASCII linefeed character (0x0a)
     *     <ldapurl>    ::= LDAP URL format (RFC 1959)
     *
     * Returns a Vector of single-String Vectors.
     */
    private static Vector<Vector<String>> extractURLs(String refString) {

        int separator = 0;
        int urlCount = 0;

        while ((separator = refString.indexOf('\n', separator)) >= 0) {
            separator++;
            urlCount++;
        }

        Vector<Vector<String>> referrals = new Vector<>(urlCount);
        int iURL;

        separator = refString.indexOf('\n');
        iURL = separator + 1;
        while ((separator = refString.indexOf('\n', iURL)) >= 0) {
            Vector<String> referral = new Vector<>(1);
            referral.addElement(refString.substring(iURL, separator));
            referrals.addElement(referral);
            iURL = separator + 1;
        }
        Vector<String> referral = new Vector<>(1);
        referral.addElement(refString.substring(iURL));
        referrals.addElement(referral);

        return referrals;
    }

    /*
     * Argument is a space-separated list of attribute IDs
     * Converts attribute IDs to lowercase before adding to built-in list.
     */
    private void setBinaryAttributes(String attrIds) {
        if (attrIds == null) {
            binaryAttrs = null;
        } else {
            binaryAttrs = new Hashtable<>(11, 0.75f);
            StringTokenizer tokens =
                new StringTokenizer(attrIds.toLowerCase(Locale.ENGLISH), " ");

            while (tokens.hasMoreTokens()) {
                binaryAttrs.put(tokens.nextToken(), Boolean.TRUE);
            }
        }
    }


    @SuppressWarnings("removal")
    protected void finalize() {
        try {
            close();
        } catch (NamingException e) {
        }
    }

    public void close() throws NamingException {
        lock.lock();
        try {
            if (debug) {
                System.err.println("LdapCtx: close() called " + this);
                (new Throwable()).printStackTrace();
            }

            if (eventSupport != null) {
                eventSupport.cleanup(); 
                removeUnsolicited();
            }

            if (enumCount > 0) {
                if (debug)
                    System.err.println("LdapCtx: close deferred");
                closeRequested = true;
                return;
            }
            closeConnection(SOFT_CLOSE);
        } finally {
            lock.unlock();
        }

/*
        schemaTrees = null;
        envprops = null;
*/
    }

    @SuppressWarnings("unchecked") 
    public void reconnect(Control[] connCtls) throws NamingException {
        envprops = (envprops == null
                ? new Hashtable<String, Object>(5, 0.75f)
                : (Hashtable<String, Object>)envprops.clone());

        if (connCtls == null) {
            envprops.remove(BIND_CONTROLS);
            bindCtls = null;
        } else {
            envprops.put(BIND_CONTROLS, bindCtls = cloneControls(connCtls));
        }

        sharable = false;  
        ensureOpen();      
    }

    @SuppressWarnings("removal")
    private static String getMechsAllowedToSendCredentials() {
        PrivilegedAction<String> pa = () -> System.getProperty(ALLOWED_MECHS_SP);
        return System.getSecurityManager() == null ? pa.run() : AccessController.doPrivileged(pa);
    }

    private static Set<String> getMechsFromPropertyValue(String propValue) {
        if (propValue == null || propValue.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(propValue.split(","))
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isConnectionEncrypted() {
        return hasLdapsScheme || clnt.isUpgradedToStartTls();
    }

    private void ensureCanTransmitCredentials(String authMechanism) throws NamingException {

        if ("none".equalsIgnoreCase(authMechanism) || "anonymous".equalsIgnoreCase(authMechanism)) {
            return;
        }

        String allowedMechanismsOrTrue = (String) envprops.get(ALLOWED_MECHS_SP);
        boolean useSpMechsCache = false;
        boolean anyPropertyIsSet = ALLOWED_MECHS_SP_VALUE != null || allowedMechanismsOrTrue != null;

        if (!isConnectionEncrypted() && (contextSeenStartTlsEnabled || anyPropertyIsSet)) {
            if ("simple".equalsIgnoreCase(authMechanism) && !envprops.containsKey(SECURITY_PRINCIPAL)) {
                return;
            }

            if (allowedMechanismsOrTrue == null) {
                useSpMechsCache = true;
                allowedMechanismsOrTrue = ALLOWED_MECHS_SP_VALUE;
            }

            if ("all".equalsIgnoreCase(allowedMechanismsOrTrue)) {
                return;
            }

            Set<String> allowedAuthMechs = useSpMechsCache ?
                    MECHS_ALLOWED_BY_SP : getMechsFromPropertyValue(allowedMechanismsOrTrue);
            if (!allowedAuthMechs.contains(authMechanism)) {
                throw new NamingException(UNSECURED_CRED_TRANSMIT_MSG);
            }
        }
    }

    private void ensureOpen() throws NamingException {
        ensureOpen(false);
    }

    private void ensureOpen(boolean startTLS) throws NamingException {

        try {
            if (clnt == null) {
                if (debug) {
                    System.err.println("LdapCtx: Reconnecting " + this);
                }

                schemaTrees = new Hashtable<>(11, 0.75f);
                connect(startTLS);

            } else if (!sharable || startTLS) {

                ReentrantLock clientLock = clnt.lock;
                clientLock.lock();
                try {
                    if (!clnt.isLdapv3
                        || clnt.referenceCount > 1
                        || clnt.usingSaslStreams()
                        || !clnt.conn.useable) {
                        closeConnection(SOFT_CLOSE);
                    }
                } finally {
                    clientLock.unlock();
                }
                schemaTrees = new Hashtable<>(11, 0.75f);
                connect(startTLS);
            }

        } finally {
            sharable = true;   
        }
    }

    private void connect(boolean startTLS) throws NamingException {
        if (debug) { System.err.println("LdapCtx: Connecting " + this); }

        String user = null;             
        Object passwd = null;           
        String secProtocol = null;      
        String socketFactory = null;    
        String authMechanism = null;    
        String ver = null;
        int ldapVersion;                
        boolean usePool = false;        

        if (envprops != null) {
            user = (String)envprops.get(Context.SECURITY_PRINCIPAL);
            passwd = envprops.get(Context.SECURITY_CREDENTIALS);
            ver = (String)envprops.get(VERSION);
            secProtocol =
               useSsl ? "ssl" : (String)envprops.get(Context.SECURITY_PROTOCOL);
            socketFactory = (String)envprops.get(SOCKET_FACTORY);
            authMechanism =
                (String)envprops.get(Context.SECURITY_AUTHENTICATION);

            usePool = "true".equalsIgnoreCase((String)envprops.get(ENABLE_POOL));
        }

        if (socketFactory == null) {
            socketFactory =
                "ssl".equals(secProtocol) ? DEFAULT_SSL_FACTORY : null;
        }

        if (authMechanism == null) {
            authMechanism = (user == null) ? "none" : "simple";
        }

        try {
            boolean initial = (clnt == null);

            if (initial) {
                ldapVersion = (ver != null) ? Integer.parseInt(ver) :
                    DEFAULT_LDAP_VERSION;

                clnt = LdapClient.getInstance(
                    usePool, 

                    hostname,
                    port_number,
                    socketFactory,
                    connectTimeout,
                    readTimeout,
                    trace,

                    ldapVersion,
                    authMechanism,
                    bindCtls,
                    secProtocol,

                    user,
                    passwd,

                    envprops);

                contextSeenStartTlsEnabled |= clnt.isUpgradedToStartTls();

                /**
                 * Pooled connections are preauthenticated;
                 * newly created ones are not.
                 */
                if (clnt.authenticateCalled()) {
                    return;
                }

            } else if (sharable && startTLS) {
                return; 

            } else {
                ldapVersion = LdapClient.LDAP_VERSION3;
            }

            LdapResult answer;
            ReentrantLock startTlsLock = clnt.conn.startTlsLock;
            startTlsLock.lock();
            try {
                ensureCanTransmitCredentials(authMechanism);
                answer = clnt.authenticate(initial, user, passwd, ldapVersion,
                        authMechanism, bindCtls, envprops);
            } finally {
                startTlsLock.unlock();
            }

            respCtls = answer.resControls; 

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                if (initial) {
                    closeConnection(HARD_CLOSE);  
                }
                processReturnCode(answer);
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw e;

            String referral;
            LdapURL url;
            NamingException saved_ex = null;

            while (true) {

                if ((referral = e.getNextReferral()) == null) {

                    if (saved_ex != null) {
                        throw (NamingException)(saved_ex.fillInStackTrace());
                    } else {
                        throw new NamingException(
                        "Internal error processing referral during connection");
                    }
                }

                url = new LdapURL(referral);
                hostname = url.getHost();
                if ((hostname != null) && (hostname.charAt(0) == '[')) {
                    hostname = hostname.substring(1, hostname.length() - 1);
                }
                port_number = url.getPort();

                try {
                    connect(startTLS);
                    break;

                } catch (NamingException ne) {
                    saved_ex = ne;
                    continue; 
                }
            }
        }
    }

    private void closeConnection(boolean hardclose) {
        removeUnsolicited();            

        if (clnt != null) {
            if (debug) {
                System.err.println("LdapCtx: calling clnt.close() " + this);
            }
            clnt.close(reqCtls, hardclose);
            clnt = null;
        }
    }

    private int enumCount = 0;
    private boolean closeRequested = false;

    void incEnumCount() {
        lock.lock();
        try {
            ++enumCount;
            if (debug) System.err.println("LdapCtx: " + this + " enum inc: " + enumCount);
        } finally {
            lock.unlock();
        }
    }

    void decEnumCount() {
        lock.lock();
        try {
            --enumCount;
            if (debug) System.err.println("LdapCtx: " + this + " enum dec: " + enumCount);

            if (enumCount == 0 && closeRequested) {
                try {
                    close();
                } catch (NamingException e) {
                }
            }
        } finally {
            lock.unlock();
        }
    }



    protected void processReturnCode(LdapResult answer) throws NamingException {
        processReturnCode(answer, null, this, null, envprops, null);
    }

    void processReturnCode(LdapResult answer, Name remainName)
    throws NamingException {
        processReturnCode(answer,
                          (new CompositeName()).add(currentDN),
                          this,
                          remainName,
                          envprops,
                          fullyQualifiedName(remainName));
    }

    protected void processReturnCode(LdapResult res, Name resolvedName,
        Object resolvedObj, Name remainName, Hashtable<?,?> envprops, String fullDN)
    throws NamingException {

        String msg = LdapClient.getErrorMessage(res.status, res.errorMessage);
        NamingException e;
        LdapReferralException r = null;

        switch (res.status) {

        case LdapClient.LDAP_SUCCESS:

            if (res.referrals != null) {

                msg = "Unprocessed Continuation Reference(s)";

                if (handleReferrals == LdapClient.LDAP_REF_IGNORE) {
                    e = new PartialResultException(msg);
                    break;
                }

                int contRefCount = res.referrals.size();
                LdapReferralException head = null;
                LdapReferralException ptr = null;

                msg = "Continuation Reference";

                for (int i = 0; i < contRefCount; i++) {

                    r = new LdapReferralException(resolvedName, resolvedObj,
                        remainName, msg, envprops, fullDN, handleReferrals,
                        reqCtls);
                    r.setReferralInfo(res.referrals.elementAt(i), true);

                    if (hopCount > 1) {
                        r.setHopCount(hopCount);
                    }

                    if (head == null) {
                        head = ptr = r;
                    } else {
                        ptr.nextReferralEx = r; 
                        ptr = r;
                    }
                }
                res.referrals = null;  

                if (res.refEx == null) {
                    res.refEx = head;

                } else {
                    ptr = res.refEx;

                    while (ptr.nextReferralEx != null) {
                        ptr = ptr.nextReferralEx;
                    }
                    ptr.nextReferralEx = head;
                }

                if (hopCount > referralHopLimit) {
                    NamingException lee =
                        new LimitExceededException("Referral limit exceeded");
                    lee.setRootCause(r);
                    throw lee;
                }
            }
            return;

        case LdapClient.LDAP_REFERRAL:

            if (handleReferrals == LdapClient.LDAP_REF_IGNORE) {
                e = new PartialResultException(msg);
                break;
            }

            r = new LdapReferralException(resolvedName, resolvedObj, remainName,
                msg, envprops, fullDN, handleReferrals, reqCtls);
            Vector<String> refs;
            if (res.referrals == null) {
                refs = null;
            } else if (handleReferrals == LdapClient.LDAP_REF_FOLLOW_SCHEME) {
                refs = new Vector<>();
                for (String s : res.referrals.elementAt(0)) {
                    if (s.startsWith("ldap:")) {
                        refs.add(s);
                    }
                }
                if (refs.isEmpty()) {
                    refs = null;
                }
            } else {
                refs = res.referrals.elementAt(0);
            }
            r.setReferralInfo(refs, false);

            if (hopCount > 1) {
                r.setHopCount(hopCount);
            }

            if (hopCount > referralHopLimit) {
                NamingException lee =
                    new LimitExceededException("Referral limit exceeded");
                lee.setRootCause(r);
                e = lee;

            } else {
                e = r;
            }
            break;

        /*
         * Handle SLAPD-style referrals.
         *
         * Referrals received during name resolution should be followed
         * until one succeeds - the target entry is located. An exception
         * is thrown now to handle these.
         *
         * Referrals received during a search operation point to unexplored
         * parts of the directory and each should be followed. An exception
         * is thrown later (during results enumeration) to handle these.
         */

        case LdapClient.LDAP_PARTIAL_RESULTS:

            if (handleReferrals == LdapClient.LDAP_REF_IGNORE) {
                e = new PartialResultException(msg);
                break;
            }

            if ((res.errorMessage != null) && (!res.errorMessage.isEmpty())) {
                res.referrals = extractURLs(res.errorMessage);
            } else {
                e = new PartialResultException(msg);
                break;
            }

            r = new LdapReferralException(resolvedName,
                resolvedObj,
                remainName,
                msg,
                envprops,
                fullDN,
                handleReferrals,
                reqCtls);

            if (hopCount > 1) {
                r.setHopCount(hopCount);
            }
            /*
             * %%%
             * SLAPD-style referrals received during name resolution
             * cannot be distinguished from those received during a
             * search operation. Since both must be handled differently
             * the following rule is applied:
             *
             *     If 1 referral and 0 entries is received then
             *     assume name resolution has not yet completed.
             */
            if (((res.entries == null) || (res.entries.isEmpty())) &&
                ((res.referrals != null) && (res.referrals.size() == 1))) {

                r.setReferralInfo(res.referrals, false);

                if (hopCount > referralHopLimit) {
                    NamingException lee =
                        new LimitExceededException("Referral limit exceeded");
                    lee.setRootCause(r);
                    e = lee;

                } else {
                    e = r;
                }

            } else {
                r.setReferralInfo(res.referrals, true);
                res.refEx = r;
                return;
            }
            break;

        case LdapClient.LDAP_INVALID_DN_SYNTAX:
        case LdapClient.LDAP_NAMING_VIOLATION:

            if (remainName != null) {
                e = new
                    InvalidNameException(remainName.toString() + ": " + msg);
            } else {
                e = new InvalidNameException(msg);
            }
            break;

        default:
            e = mapErrorCode(res.status, res.errorMessage);
            break;
        }
        e.setResolvedName(resolvedName);
        e.setResolvedObj(resolvedObj);
        e.setRemainingName(remainName);
        throw e;
    }

    /**
     * Maps an LDAP error code to an appropriate NamingException.
     * %%% public; used by controls
     *
     * @param errorCode numeric LDAP error code
     * @param errorMessage textual description of the LDAP error. May be null.
     *
     * @return A NamingException or null if the error code indicates success.
     */
    public static NamingException mapErrorCode(int errorCode,
        String errorMessage) {

        if (errorCode == LdapClient.LDAP_SUCCESS)
            return null;

        NamingException e = null;
        String message = LdapClient.getErrorMessage(errorCode, errorMessage);

        switch (errorCode) {

        case LdapClient.LDAP_ALIAS_DEREFERENCING_PROBLEM:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_ALIAS_PROBLEM:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_ATTRIBUTE_OR_VALUE_EXISTS:
            e = new AttributeInUseException(message);
            break;

        case LdapClient.LDAP_AUTH_METHOD_NOT_SUPPORTED:
        case LdapClient.LDAP_CONFIDENTIALITY_REQUIRED:
        case LdapClient.LDAP_STRONG_AUTH_REQUIRED:
        case LdapClient.LDAP_INAPPROPRIATE_AUTHENTICATION:
            e = new AuthenticationNotSupportedException(message);
            break;

        case LdapClient.LDAP_ENTRY_ALREADY_EXISTS:
            e = new NameAlreadyBoundException(message);
            break;

        case LdapClient.LDAP_INVALID_CREDENTIALS:
        case LdapClient.LDAP_SASL_BIND_IN_PROGRESS:
            e = new AuthenticationException(message);
            break;

        case LdapClient.LDAP_INAPPROPRIATE_MATCHING:
            e = new InvalidSearchFilterException(message);
            break;

        case LdapClient.LDAP_INSUFFICIENT_ACCESS_RIGHTS:
            e = new NoPermissionException(message);
            break;

        case LdapClient.LDAP_INVALID_ATTRIBUTE_SYNTAX:
        case LdapClient.LDAP_CONSTRAINT_VIOLATION:
            e =  new InvalidAttributeValueException(message);
            break;

        case LdapClient.LDAP_LOOP_DETECT:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_NO_SUCH_ATTRIBUTE:
            e = new NoSuchAttributeException(message);
            break;

        case LdapClient.LDAP_NO_SUCH_OBJECT:
            e = new NameNotFoundException(message);
            break;

        case LdapClient.LDAP_OBJECT_CLASS_MODS_PROHIBITED:
        case LdapClient.LDAP_OBJECT_CLASS_VIOLATION:
        case LdapClient.LDAP_NOT_ALLOWED_ON_RDN:
            e = new SchemaViolationException(message);
            break;

        case LdapClient.LDAP_NOT_ALLOWED_ON_NON_LEAF:
            e = new ContextNotEmptyException(message);
            break;

        case LdapClient.LDAP_OPERATIONS_ERROR:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_OTHER:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_PROTOCOL_ERROR:
            e = new CommunicationException(message);
            break;

        case LdapClient.LDAP_SIZE_LIMIT_EXCEEDED:
            e = new SizeLimitExceededException(message);
            break;

        case LdapClient.LDAP_TIME_LIMIT_EXCEEDED:
            e = new TimeLimitExceededException(message);
            break;

        case LdapClient.LDAP_UNAVAILABLE_CRITICAL_EXTENSION:
            e = new OperationNotSupportedException(message);
            break;

        case LdapClient.LDAP_UNAVAILABLE:
        case LdapClient.LDAP_BUSY:
            e = new ServiceUnavailableException(message);
            break;

        case LdapClient.LDAP_UNDEFINED_ATTRIBUTE_TYPE:
            e = new InvalidAttributeIdentifierException(message);
            break;

        case LdapClient.LDAP_UNWILLING_TO_PERFORM:
            e = new OperationNotSupportedException(message);
            break;

        case LdapClient.LDAP_COMPARE_FALSE:
        case LdapClient.LDAP_COMPARE_TRUE:
        case LdapClient.LDAP_IS_LEAF:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_ADMIN_LIMIT_EXCEEDED:
            e = new LimitExceededException(message);
            break;

        case LdapClient.LDAP_REFERRAL:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_PARTIAL_RESULTS:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_INVALID_DN_SYNTAX:
        case LdapClient.LDAP_NAMING_VIOLATION:
            e = new InvalidNameException(message);
            break;

        default:
            e = new NamingException(message);
            break;
        }

        return e;
    }


    public ExtendedResponse extendedOperation(ExtendedRequest request)
        throws NamingException {

        boolean startTLS = (request.getID().equals(STARTTLS_REQ_OID));
        ensureOpen(startTLS);

        try {

            LdapResult answer =
                clnt.extendedOp(request.getID(), request.getEncodedValue(),
                                reqCtls, startTLS);
            respCtls = answer.resControls; 

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, new CompositeName());
            }

            int len = (answer.extensionValue == null) ?
                        0 :
                        answer.extensionValue.length;

            ExtendedResponse er =
                request.createExtendedResponse(answer.extensionId,
                    answer.extensionValue, 0, len);

            if (er instanceof StartTlsResponseImpl) {
                String domainName = (String)
                    (envprops != null ? envprops.get(DOMAIN_NAME) : null);
                ((StartTlsResponseImpl)er).setConnection(clnt.conn, domainName);
                contextSeenStartTlsEnabled |= startTLS;
            }
            return er;

        } catch (LdapReferralException e) {

            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw e;

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                try {

                    return refCtx.extendedOperation(request);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw e2;
        }
    }

    public void setRequestControls(Control[] reqCtls) throws NamingException {
        if (handleReferrals == LdapClient.LDAP_REF_IGNORE) {
            this.reqCtls = addControl(reqCtls, manageReferralControl);
        } else {
            this.reqCtls = cloneControls(reqCtls);
        }
    }

    public Control[] getRequestControls() throws NamingException {
        return cloneControls(reqCtls);
    }

    public Control[] getConnectControls() throws NamingException {
        return cloneControls(bindCtls);
    }

    public Control[] getResponseControls() throws NamingException {
        return (respCtls != null)? convertControls(respCtls) : null;
    }

    /**
     * Narrow controls using own default factory and ControlFactory.
     * @param ctls A non-null Vector<Control>
     */
    Control[] convertControls(Vector<Control> ctls) throws NamingException {
        int count = ctls.size();

        if (count == 0) {
            return null;
        }

        Control[] controls = new Control[count];

        for (int i = 0; i < count; i++) {
            controls[i] = myResponseControlFactory.getControlInstance(
                ctls.elementAt(i));

            if (controls[i] == null) {
                controls[i] = ControlFactory.getControlInstance(
                ctls.elementAt(i), this, envprops);
            }
        }
        return controls;
    }

    private static Control[] addControl(Control[] prevCtls, Control addition) {
        if (prevCtls == null) {
            return new Control[]{addition};
        }

        int found = findControl(prevCtls, addition);
        if (found != -1) {
            return prevCtls;  
        }

        Control[] newCtls = new Control[prevCtls.length+1];
        System.arraycopy(prevCtls, 0, newCtls, 0, prevCtls.length);
        newCtls[prevCtls.length] = addition;
        return newCtls;
    }

    private static int findControl(Control[] ctls, Control target) {
        for (int i = 0; i < ctls.length; i++) {
            if (ctls[i] == target) {
                return i;
            }
        }
        return -1;
    }

    private static Control[] removeControl(Control[] prevCtls, Control target) {
        if (prevCtls == null) {
            return null;
        }

        int found = findControl(prevCtls, target);
        if (found == -1) {
            return prevCtls;  
        }

        Control[] newCtls = new Control[prevCtls.length-1];
        System.arraycopy(prevCtls, 0, newCtls, 0, found);
        System.arraycopy(prevCtls, found+1, newCtls, found,
            prevCtls.length-found-1);
        return newCtls;
    }

    private static Control[] cloneControls(Control[] ctls) {
        if (ctls == null) {
            return null;
        }
        Control[] copiedCtls = new Control[ctls.length];
        System.arraycopy(ctls, 0, copiedCtls, 0, ctls.length);
        return copiedCtls;
    }

    /*
     * Access to eventSupport need not be synchronized even though the
     * Connection thread can access it asynchronously. It is
     * impossible for a race condition to occur because
     * eventSupport.addNamingListener() must have been called before
     * the Connection thread can call back to this ctx.
     */
    public void addNamingListener(Name nm, int scope, NamingListener l)
        throws NamingException {
            addNamingListener(getTargetName(nm), scope, l);
    }

    public void addNamingListener(String nm, int scope, NamingListener l)
        throws NamingException {
            if (eventSupport == null)
                eventSupport = new EventSupport(this);
            eventSupport.addNamingListener(getTargetName(new CompositeName(nm)),
                scope, l);

            if (l instanceof UnsolicitedNotificationListener && !unsolicited) {
                addUnsolicited();
            }
    }

    public void removeNamingListener(NamingListener l) throws NamingException {
        if (eventSupport == null)
            return; 

        eventSupport.removeNamingListener(l);

        if (l instanceof UnsolicitedNotificationListener &&
            !eventSupport.hasUnsolicited()) {
            removeUnsolicited();
        }
    }

    public void addNamingListener(String nm, String filter,
                                  SearchControls ctls, NamingListener l)
            throws NamingException {

        if (eventSupport == null)
            eventSupport = new EventSupport(this);

        eventSupport.addNamingListener(getTargetName(new CompositeName(nm)),
                filter, cloneSearchControls(ctls), l);

        if (l instanceof UnsolicitedNotificationListener && !unsolicited) {
            addUnsolicited();
        }
    }

    public void addNamingListener(Name nm, String filter, SearchControls ctls,
        NamingListener l) throws NamingException {
            addNamingListener(getTargetName(nm), filter, ctls, l);
    }

    public void addNamingListener(Name nm, String filter, Object[] filterArgs,
        SearchControls ctls, NamingListener l) throws NamingException {
            addNamingListener(getTargetName(nm), filter, filterArgs, ctls, l);
    }

    public void addNamingListener(String nm, String filterExpr, Object[] filterArgs,
        SearchControls ctls, NamingListener l) throws NamingException {
        String strfilter = SearchFilter.format(filterExpr, filterArgs);
        addNamingListener(getTargetName(new CompositeName(nm)), strfilter, ctls, l);
    }

    public boolean targetMustExist() {
        return true;
    }

    /**
     * Retrieves the target name for which the listener is registering.
     * If nm is a CompositeName, use its first and only component. It
     * cannot have more than one components because a target be outside of
     * this namespace. If nm is not a CompositeName, then treat it as a
     * compound name.
     * @param nm The non-null target name.
     */
    private static String getTargetName(Name nm) throws NamingException {
        if (nm instanceof CompositeName) {
            if (nm.size() > 1) {
                throw new InvalidNameException(
                    "Target cannot span multiple namespaces: " + nm);
            } else if (nm.isEmpty()) {
                return "";
            } else {
                return nm.get(0);
            }
        } else {
            return nm.toString();
        }
    }


    /**
     * Registers this context with the underlying LdapClient.
     * When the underlying LdapClient receives an unsolicited notification,
     * it will invoke LdapCtx.fireUnsolicited() so that this context
     * can (using EventSupport) notified any registered listeners.
     * This method is called by EventSupport when an unsolicited listener
     * first registers with this context (should be called just once).
     * @see #removeUnsolicited
     * @see #fireUnsolicited
     */
    private void addUnsolicited() throws NamingException {
        if (debug) {
            System.out.println("LdapCtx.addUnsolicited: " + this);
        }

        ensureOpen();
        ReentrantLock eventSupportLock = eventSupport.lock;
        eventSupportLock.lock();
        try {
            clnt.addUnsolicited(this);
            unsolicited = true;
        } finally {
            eventSupportLock.unlock();
        }
    }

    /**
     * Removes this context from registering interest in unsolicited
     * notifications from the underlying LdapClient. This method is called
     * under any one of the following conditions:
     * <ul>
     * <li>All unsolicited listeners have been removed. (see removingNamingListener)
     * <li>This context is closed.
     * <li>This context's underlying LdapClient changes.
     *</ul>
     * After this method has been called, this context will not pass
     * on any events related to unsolicited notifications to EventSupport and
     * and its listeners.
     */

    private void removeUnsolicited() {
        if (debug) {
            System.out.println("LdapCtx.removeUnsolicited: " + unsolicited);
        }
        if (eventSupport == null) {
            return;
        }

        ReentrantLock eventSupportLock = eventSupport.lock;
        eventSupportLock.lock();
        try {
            if (unsolicited && clnt != null) {
                clnt.removeUnsolicited(this);
            }
            unsolicited = false;
        } finally {
            eventSupportLock.unlock();
        }
    }

    /**
     * Uses EventSupport to fire an event related to an unsolicited notification.
     * Called by LdapClient when LdapClient receives an unsolicited notification.
     */
    void fireUnsolicited(Object obj) {
        if (debug) {
            System.out.println("LdapCtx.fireUnsolicited: " + obj);
        }
        ReentrantLock eventSupportLock = eventSupport.lock;
        eventSupportLock.lock();
        try {
            if (unsolicited) {
                eventSupport.fireUnsolicited(obj);

                if (obj instanceof NamingException) {
                    unsolicited = false;
                }
            }
        } finally {
            eventSupportLock.unlock();
        }
    }
}
