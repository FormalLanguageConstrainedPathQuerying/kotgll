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

import com.sun.jndi.toolkit.ctx.Continuation;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.naming.*;
import javax.naming.directory.Attributes;
import javax.naming.ldap.Control;

/**
 * Basic enumeration for NameClassPair, Binding, and SearchResults.
 */

abstract class AbstractLdapNamingEnumeration<T extends NameClassPair>
        implements NamingEnumeration<T>, ReferralEnumeration<T> {

    protected Name listArg;

    private boolean cleaned = false;
    private LdapResult res;
    private LdapClient enumClnt;
    private Continuation cont;  
    private Vector<LdapEntry> entries = null;
    private int limit = 0;
    private int posn = 0;
    protected LdapCtx homeCtx;
    private LdapReferralException refEx = null;
    private NamingException errEx = null;

    /*
     * Record the next set of entries and/or referrals.
     */
    AbstractLdapNamingEnumeration(LdapCtx homeCtx, LdapResult answer, Name listArg,
        Continuation cont) throws NamingException {


            if ((answer.status != LdapClient.LDAP_SUCCESS) &&
                (answer.status != LdapClient.LDAP_SIZE_LIMIT_EXCEEDED) &&
                (answer.status != LdapClient.LDAP_TIME_LIMIT_EXCEEDED) &&
                (answer.status != LdapClient.LDAP_ADMIN_LIMIT_EXCEEDED) &&
                (answer.status != LdapClient.LDAP_REFERRAL) &&
                (answer.status != LdapClient.LDAP_PARTIAL_RESULTS)) {

                NamingException e = new NamingException(
                                    LdapClient.getErrorMessage(
                                    answer.status, answer.errorMessage));

                throw cont.fillInException(e);
            }


            res = answer;
            entries = answer.entries;
            limit = (entries == null) ? 0 : entries.size(); 
            this.listArg = listArg;
            this.cont = cont;

            if (answer.refEx != null) {
                refEx = answer.refEx;
            }

            this.homeCtx = homeCtx;
            homeCtx.incEnumCount();
            enumClnt = homeCtx.clnt; 
    }

    @Override
    public final T nextElement() {
        try {
            return next();
        } catch (NamingException e) {
            cleanup();
            return null;
        }
    }

    @Override
    public final boolean hasMoreElements() {
        try {
            return hasMore();
        } catch (NamingException e) {
            cleanup();
            return false;
        }
    }

    /*
     * Retrieve the next set of entries and/or referrals.
     */
    private void getNextBatch() throws NamingException {

        res = homeCtx.getSearchReply(enumClnt, res);
        if (res == null) {
            limit = posn = 0;
            return;
        }

        entries = res.entries;
        limit = (entries == null) ? 0 : entries.size(); 
        posn = 0; 

        if ((res.status != LdapClient.LDAP_SUCCESS) ||
            ((res.status == LdapClient.LDAP_SUCCESS) &&
                (res.referrals != null))) {

            try {
                homeCtx.processReturnCode(res, listArg);

            } catch (LimitExceededException | PartialResultException e) {
                setNamingException(e);

            }
        }

        if (res.refEx != null) {
            if (refEx == null) {
                refEx = res.refEx;
            } else {
                refEx = refEx.appendUnprocessedReferrals(res.refEx);
            }
            res.refEx = null; 
        }

        if (res.resControls != null) {
            homeCtx.respCtls = res.resControls;
        }
    }

    private boolean more = true;  
    private boolean hasMoreCalled = false;

    /*
     * Test if unprocessed entries or referrals exist.
     */
    @Override
    public final boolean hasMore() throws NamingException {

        if (hasMoreCalled) {
            return more;
        }

        hasMoreCalled = true;

        if (!more) {
            return false;
        } else {
            return (more = hasMoreImpl());
        }
    }

    /*
     * Retrieve the next entry.
     */
    @Override
    public final T next() throws NamingException {

        if (!hasMoreCalled) {
            hasMore();
        }
        hasMoreCalled = false;
        return nextImpl();
    }

    /*
     * Test if unprocessed entries or referrals exist.
     */
    private boolean hasMoreImpl() throws NamingException {

        if (posn == limit) {
            getNextBatch();
        }

        if (posn < limit) {
            return true;
        } else {

            try {
                return hasMoreReferrals();

            } catch (LdapReferralException |
                     LimitExceededException |
                     PartialResultException e) {
                cleanup();
                throw e;

            } catch (NamingException e) {
                cleanup();
                PartialResultException pre = new PartialResultException();
                pre.setRootCause(e);
                throw pre;
            }
        }
    }

    /*
     * Retrieve the next entry.
     */
    private T nextImpl() throws NamingException {
        try {
            return nextAux();
        } catch (NamingException e) {
            cleanup();
            throw cont.fillInException(e);
        }
    }

    private T nextAux() throws NamingException {
        if (posn == limit) {
            getNextBatch();  
        }

        if (posn >= limit) {
            cleanup();
            throw new NoSuchElementException("invalid enumeration handle");
        }

        LdapEntry result = entries.elementAt(posn++);

        return createItem(result.DN, result.attributes, result.respCtls);
    }

    protected final String getAtom(String dn) {
        try {
            Name parsed = new LdapName(dn);
            return parsed.get(parsed.size() - 1);
        } catch (NamingException e) {
            return dn;
        }
    }

    protected abstract T createItem(String dn, Attributes attrs,
        Vector<Control> respCtls) throws NamingException;

    /*
     * Append the supplied (chain of) referrals onto the
     * end of the current (chain of) referrals.
     */
    @Override
    public void appendUnprocessedReferrals(LdapReferralException ex) {
        if (refEx != null) {
            refEx = refEx.appendUnprocessedReferrals(ex);
        } else {
            refEx = ex.appendUnprocessedReferrals(refEx);
        }
    }

    final void setNamingException(NamingException e) {
        errEx = e;
    }

    protected abstract AbstractLdapNamingEnumeration<? extends NameClassPair> getReferredResults(
            LdapReferralContext refCtx) throws NamingException;

    /*
     * Iterate through the URLs of a referral. If successful then perform
     * a search operation and merge the received results with the current
     * results.
     */
    protected final boolean hasMoreReferrals() throws NamingException {

        if ((refEx != null) && !(errEx instanceof LimitExceededException) &&
            (refEx.hasMoreReferrals() || refEx.hasMoreReferralExceptions())) {

            if (homeCtx.handleReferrals == LdapClient.LDAP_REF_THROW) {
                throw (NamingException)(refEx.fillInStackTrace());
            }

            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)refEx.getReferralContext(
                    homeCtx.envprops, homeCtx.reqCtls);

                try {

                    update(getReferredResults(refCtx));
                    break;

                } catch (LdapReferralException re) {

                    var namingException = re.getNamingException();
                    if (namingException instanceof LimitExceededException) {
                        errEx = namingException;
                        break;
                    } else if (errEx == null) {
                        errEx = namingException;
                    }
                    refEx = re;
                    continue;

                } finally {
                    refCtx.close();
                }
            }
            return hasMoreImpl();

        } else {
            cleanup();

            if (errEx != null) {
                throw errEx;
            }
            return (false);
        }
    }

    /*
     * Merge the entries and/or referrals from the supplied enumeration
     * with those of the current enumeration.
     */
    protected void update(AbstractLdapNamingEnumeration<? extends NameClassPair> ne) {
        homeCtx.decEnumCount();

        homeCtx = ne.homeCtx;
        enumClnt = ne.enumClnt;

        ne.homeCtx = null;

        posn = ne.posn;
        limit = ne.limit;
        res = ne.res;
        entries = ne.entries;
        refEx = ne.refEx;
        listArg = ne.listArg;
        if (errEx == null || ne.errEx instanceof LimitExceededException) {
            errEx = ne.errEx;
        }
    }

    @SuppressWarnings("removal")
    protected final void finalize() {
        cleanup();
    }

    protected final void cleanup() {
        if (cleaned) return; 

        if(enumClnt != null) {
            enumClnt.clearSearchReply(res, homeCtx.reqCtls);
        }

        enumClnt = null;
        cleaned = true;
        if (homeCtx != null) {
            homeCtx.decEnumCount();
            homeCtx = null;
        }
    }

    @Override
    public final void close() {
        cleanup();
    }
}
