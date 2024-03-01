/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.net.URI;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple in-memory java.net.CookieStore implementation
 *
 * @author Edward Wang
 * @since 1.6
 */
class InMemoryCookieStore implements CookieStore {
    private List<HttpCookie> cookieJar = null;

    private Map<String, List<HttpCookie>> domainIndex = null;
    private Map<URI, List<HttpCookie>> uriIndex = null;

    private ReentrantLock lock = null;


    /**
     * The default ctor
     */
    public InMemoryCookieStore() {
        cookieJar = new ArrayList<>();
        domainIndex = new HashMap<>();
        uriIndex = new HashMap<>();

        lock = new ReentrantLock(false);
    }

    /**
     * Add one cookie into cookie store.
     */
    public void add(URI uri, HttpCookie cookie) {
        if (cookie == null) {
            throw new NullPointerException("cookie is null");
        }


        lock.lock();
        try {
            cookieJar.remove(cookie);

            if (cookie.getMaxAge() != 0) {
                cookieJar.add(cookie);
                if (cookie.getDomain() != null) {
                    addIndex(domainIndex, cookie.getDomain(), cookie);
                }
                if (uri != null) {
                    addIndex(uriIndex, getEffectiveURI(uri), cookie);
                }
            }
        } finally {
            lock.unlock();
        }
    }


    /**
     * Get all cookies, which:
     *  1) given uri domain-matches with, or, associated with
     *     given uri when added to the cookie store.
     *  3) not expired.
     * See RFC 2965 sec. 3.3.4 for more detail.
     */
    public List<HttpCookie> get(URI uri) {
        if (uri == null) {
            throw new NullPointerException("uri is null");
        }

        List<HttpCookie> cookies = new ArrayList<>();
        boolean secureLink = "https".equalsIgnoreCase(uri.getScheme());
        lock.lock();
        try {
            getInternal1(cookies, domainIndex, uri.getHost(), secureLink);
            getInternal2(cookies, uriIndex, getEffectiveURI(uri), secureLink);
        } finally {
            lock.unlock();
        }

        return cookies;
    }

    /**
     * Get all cookies in cookie store, except those have expired
     */
    public List<HttpCookie> getCookies() {
        List<HttpCookie> rt;

        lock.lock();
        try {
            Iterator<HttpCookie> it = cookieJar.iterator();
            while (it.hasNext()) {
                if (it.next().hasExpired()) {
                    it.remove();
                }
            }
        } finally {
            rt = Collections.unmodifiableList(cookieJar);
            lock.unlock();
        }

        return rt;
    }

    /**
     * Get all URIs, which are associated with at least one cookie
     * of this cookie store.
     */
    public List<URI> getURIs() {
        List<URI> uris = new ArrayList<>();

        lock.lock();
        try {
            Iterator<URI> it = uriIndex.keySet().iterator();
            while (it.hasNext()) {
                URI uri = it.next();
                List<HttpCookie> cookies = uriIndex.get(uri);
                if (cookies == null || cookies.size() == 0) {
                    it.remove();
                }
            }
        } finally {
            uris.addAll(uriIndex.keySet());
            lock.unlock();
        }

        return uris;
    }


    /**
     * Remove a cookie from store
     */
    public boolean remove(URI uri, HttpCookie ck) {
        if (ck == null) {
            throw new NullPointerException("cookie is null");
        }

        boolean modified = false;
        lock.lock();
        try {
            modified = cookieJar.remove(ck);
        } finally {
            lock.unlock();
        }

        return modified;
    }


    /**
     * Remove all cookies in this cookie store.
     */
    public boolean removeAll() {
        lock.lock();
        try {
            if (cookieJar.isEmpty()) {
                return false;
            }
            cookieJar.clear();
            domainIndex.clear();
            uriIndex.clear();
        } finally {
            lock.unlock();
        }

        return true;
    }


    /* ---------------- Private operations -------------- */


    /*
     * This is almost the same as HttpCookie.domainMatches except for
     * one difference: It won't reject cookies when the 'H' part of the
     * domain contains a dot ('.').
     * I.E.: RFC 2965 section 3.3.2 says that if host is x.y.domain.com
     * and the cookie domain is .domain.com, then it should be rejected.
     * However that's not how the real world works. Browsers don't reject and
     * some sites, like yahoo.com do actually expect these cookies to be
     * passed along.
     * And should be used for 'old' style cookies (aka Netscape type of cookies)
     */
    private boolean netscapeDomainMatches(String domain, String host)
    {
        if (domain == null || host == null) {
            return false;
        }

        boolean isLocalDomain = ".local".equalsIgnoreCase(domain);
        int embeddedDotInDomain = domain.indexOf('.');
        if (embeddedDotInDomain == 0) {
            embeddedDotInDomain = domain.indexOf('.', 1);
        }
        if (!isLocalDomain && (embeddedDotInDomain == -1 || embeddedDotInDomain == domain.length() - 1)) {
            return false;
        }

        int firstDotInHost = host.indexOf('.');
        if (firstDotInHost == -1 && isLocalDomain) {
            return true;
        }

        int domainLength = domain.length();
        int lengthDiff = host.length() - domainLength;
        if (lengthDiff == 0) {
            return host.equalsIgnoreCase(domain);
        } else if (lengthDiff > 0) {
            String H = host.substring(0, lengthDiff);
            String D = host.substring(lengthDiff);

            return (D.equalsIgnoreCase(domain));
        } else if (lengthDiff == -1) {
            return (domain.charAt(0) == '.' &&
                    host.equalsIgnoreCase(domain.substring(1)));
        }

        return false;
    }

    private void getInternal1(List<HttpCookie> cookies, Map<String, List<HttpCookie>> cookieIndex,
            String host, boolean secureLink) {
        ArrayList<HttpCookie> toRemove = new ArrayList<>();
        for (Map.Entry<String, List<HttpCookie>> entry : cookieIndex.entrySet()) {
            String domain = entry.getKey();
            List<HttpCookie> lst = entry.getValue();
            for (HttpCookie c : lst) {
                if ((c.getVersion() == 0 && netscapeDomainMatches(domain, host)) ||
                        (c.getVersion() == 1 && HttpCookie.domainMatches(domain, host))) {
                    if ((cookieJar.indexOf(c) != -1)) {
                        if (!c.hasExpired()) {
                            if ((secureLink || !c.getSecure()) &&
                                    !cookies.contains(c)) {
                                cookies.add(c);
                            }
                        } else {
                            toRemove.add(c);
                        }
                    } else {
                        toRemove.add(c);
                    }
                }
            }
            for (HttpCookie c : toRemove) {
                lst.remove(c);
                cookieJar.remove(c);

            }
            toRemove.clear();
        }
    }

    private <T> void getInternal2(List<HttpCookie> cookies,
                                Map<T, List<HttpCookie>> cookieIndex,
                                Comparable<T> comparator, boolean secureLink)
    {
        for (T index : cookieIndex.keySet()) {
            if (comparator.compareTo(index) == 0) {
                List<HttpCookie> indexedCookies = cookieIndex.get(index);
                if (indexedCookies != null) {
                    Iterator<HttpCookie> it = indexedCookies.iterator();
                    while (it.hasNext()) {
                        HttpCookie ck = it.next();
                        if (cookieJar.indexOf(ck) != -1) {
                            if (!ck.hasExpired()) {
                                if ((secureLink || !ck.getSecure()) &&
                                        !cookies.contains(ck))
                                    cookies.add(ck);
                            } else {
                                it.remove();
                                cookieJar.remove(ck);
                            }
                        } else {
                            it.remove();
                        }
                    }
                } 
            } 
        } 
    }

    private <T> void addIndex(Map<T, List<HttpCookie>> indexStore,
                              T index,
                              HttpCookie cookie)
    {
        if (index != null) {
            List<HttpCookie> cookies = indexStore.get(index);
            if (cookies != null) {
                cookies.remove(cookie);

                cookies.add(cookie);
            } else {
                cookies = new ArrayList<>();
                cookies.add(cookie);
                indexStore.put(index, cookies);
            }
        }
    }


    private URI getEffectiveURI(URI uri) {
        URI effectiveURI = null;
        try {
            effectiveURI = new URI("http",
                                   uri.getHost(),
                                   null,  
                                   null,  
                                   null   
                                  );
        } catch (URISyntaxException ignored) {
            effectiveURI = uri;
        }

        return effectiveURI;
    }
}
