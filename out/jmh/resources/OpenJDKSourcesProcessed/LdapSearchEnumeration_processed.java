/*
 * Copyright (c) 1999, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Vector;
import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;
import javax.naming.ldap.LdapName;

import com.sun.jndi.toolkit.ctx.Continuation;
import com.sun.naming.internal.NamingManagerHelper;
import com.sun.naming.internal.ObjectFactoriesFilter;

final class LdapSearchEnumeration
        extends AbstractLdapNamingEnumeration<SearchResult> {

    private Name startName;             
    private LdapCtx.SearchArgs searchArgs = null;

    @SuppressWarnings("removal")
    private final AccessControlContext acc = AccessController.getContext();

    LdapSearchEnumeration(LdapCtx homeCtx, LdapResult search_results,
        String starter, LdapCtx.SearchArgs args, Continuation cont)
        throws NamingException {

        super(homeCtx, search_results,
              args.name, /* listArg */
              cont);

        startName = new LdapName(starter);
        searchArgs = args;
    }

    @SuppressWarnings("removal")
    @Override
    protected SearchResult createItem(String dn, Attributes attrs,
                                      Vector<Control> respCtls)
            throws NamingException {

        Object obj = null;

        String relStart;         
        String relHome;          
        boolean relative = true; 


        try {
            Name parsed = new LdapName(dn);

            if (startName != null && parsed.startsWith(startName)) {
                relStart = parsed.getSuffix(startName.size()).toString();
                relHome = parsed.getSuffix(homeCtx.currentParsedDN.size()).toString();
            } else {
                relative = false;
                relHome = relStart =
                    LdapURL.toUrlString(homeCtx.hostname, homeCtx.port_number,
                    dn, homeCtx.hasLdapsScheme);
            }
        } catch (NamingException e) {
            relative = false;
            relHome = relStart =
                LdapURL.toUrlString(homeCtx.hostname, homeCtx.port_number,
                dn, homeCtx.hasLdapsScheme);
        }

        CompositeName cn = new CompositeName();
        if (!relStart.isEmpty()) {
            cn.add(relStart);
        }

        CompositeName rcn = new CompositeName();
        if (!relHome.isEmpty()) {
            rcn.add(relHome);
        }

        homeCtx.setParents(attrs, rcn);

        if (searchArgs.cons.getReturningObjFlag()) {

            if (attrs.get(Obj.JAVA_ATTRIBUTES[Obj.CLASSNAME]) != null) {
                try {
                    PrivilegedExceptionAction<Object> pea = () -> Obj.decodeObject(attrs);
                    obj = AccessController.doPrivileged(pea, acc);
                } catch (PrivilegedActionException e) {
                    throw (NamingException)e.getException();
                }
            }
            if (obj == null) {
                obj = new LdapCtx(homeCtx, dn);
            }

            try {
                obj = NamingManagerHelper.getDirObjectInstance(
                    obj, rcn, (relative ? homeCtx : null),
                    homeCtx.envprops, attrs, ObjectFactoriesFilter::checkLdapFilter);
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) {
                NamingException ne =
                    new NamingException(
                            "problem generating object using object factory");
                ne.setRootCause(e);
                throw ne;
            }


            String[] reqAttrs;
            if ((reqAttrs = searchArgs.reqAttrs) != null) {
                Attributes rattrs = new BasicAttributes(true); 
                for (int i = 0; i < reqAttrs.length; i++) {
                    rattrs.put(reqAttrs[i], null);
                }
                for (int i = 0; i < Obj.JAVA_ATTRIBUTES.length; i++) {
                    if (rattrs.get(Obj.JAVA_ATTRIBUTES[i]) == null) {
                        attrs.remove(Obj.JAVA_ATTRIBUTES[i]);
                    }
                }
            }

        }

        /*
         * name in search result is either the stringified composite name
         * relative to the search context that can be passed directly to
         * methods of the search context, or the fully qualified DN
         * which can be used with the initial context.
         */
        SearchResult sr;
        if (respCtls != null) {
            sr = new SearchResultWithControls(
                (relative ? cn.toString() : relStart), obj, attrs,
                relative, homeCtx.convertControls(respCtls));
        } else {
            sr = new SearchResult(
                (relative ? cn.toString() : relStart),
                obj, attrs, relative);
        }
        sr.setNameInNamespace(dn);
        return sr;
    }

    @Override
    public void appendUnprocessedReferrals(LdapReferralException ex) {

        startName = null;
        super.appendUnprocessedReferrals(ex);
    }

    @Override
    protected AbstractLdapNamingEnumeration<? extends NameClassPair> getReferredResults(
            LdapReferralContext refCtx) throws NamingException {
        return (AbstractLdapNamingEnumeration<? extends NameClassPair>)refCtx.search(
                searchArgs.name, searchArgs.filter, searchArgs.cons);
    }

    @Override
    protected void update(AbstractLdapNamingEnumeration<? extends NameClassPair> ne) {
        super.update(ne);

        LdapSearchEnumeration se = (LdapSearchEnumeration)ne;
        startName = se.startName;
    }

    void setStartName(Name nm) {
        startName = nm;
    }
}
