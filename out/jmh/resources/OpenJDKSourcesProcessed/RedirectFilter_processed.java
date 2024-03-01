/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.util.concurrent.locks.ReentrantLock;

import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Utils;

class RedirectFilter implements HeaderFilter {

    private final ReentrantLock stateLock = new ReentrantLock();
    HttpRequestImpl request;
    HttpClientImpl client;
    HttpClient.Redirect policy;
    String method;
    MultiExchange<?> exchange;
    static final int DEFAULT_MAX_REDIRECTS = 5;
    URI uri;

    /*
     * NOT_MODIFIED status code results from a conditional GET where
     * the server does not (must not) return a response body because
     * the condition specified in the request disallows it
     */
    static final int HTTP_NOT_MODIFIED = 304;

    static final int max_redirects = Utils.getIntegerNetProperty(
            "jdk.httpclient.redirects.retrylimit", DEFAULT_MAX_REDIRECTS
    );

    public RedirectFilter() {}

    @Override
    public void request(HttpRequestImpl r, MultiExchange<?> e) throws IOException {
        stateLock.lock();
        try {
            this.request = r;
            this.client = e.client();
            this.policy = client.followRedirects();

            this.method = r.method();
            this.uri = r.uri();
            this.exchange = e;
        } finally {
            stateLock.unlock();
        }
    }

    @Override
    public HttpRequestImpl response(Response r) throws IOException {
        stateLock.lock();
        try {
            return handleResponse(r);
        } finally {
            stateLock.unlock();
        }
    }

    private static String redirectedMethod(int statusCode, String orig) {
        return switch (statusCode) {
            case 301, 302   -> orig.equals("POST") ? "GET" : orig;
            case 303        -> "GET";
            case 307, 308   -> orig;

            default -> orig; 
        };
    }

    private static boolean isRedirecting(int statusCode) {
        if (statusCode < 300) return false;
        if (statusCode > 308) return false;

        return switch (statusCode) {
            case 300, 304, 305, 306 -> false;
            default -> true;
        };
    }

    /**
     * Checks to see if a new request is needed and returns it.
     * Null means response is ok to return to user.
     */
    private HttpRequestImpl handleResponse(Response r) {
        int rcode = r.statusCode();
        if (rcode == 200 || policy == HttpClient.Redirect.NEVER) {
            return null;
        }

        if (rcode == HTTP_NOT_MODIFIED)
            return null;

        if (isRedirecting(rcode)) {
            URI redir = getRedirectedURI(r.headers());
            String newMethod = redirectedMethod(rcode, method);
            Log.logTrace("response code: {0}, redirected URI: {1}", rcode, redir);
            if (canRedirect(redir) && ++exchange.numberOfRedirects < max_redirects) {
                Log.logTrace("redirect to: {0} with method: {1}", redir, newMethod);
                return HttpRequestImpl.newInstanceForRedirection(redir, newMethod, request, rcode != 303);
            } else {
                Log.logTrace("not redirecting");
                return null;
            }
        }
        return null;
    }

    private URI getRedirectedURI(HttpHeaders headers) {
        URI redirectedURI;
        redirectedURI = headers.firstValue("Location")
                .map(URI::create)
                .orElseThrow(() -> new UncheckedIOException(
                        new IOException("Invalid redirection")));

        redirectedURI = uri.resolve(redirectedURI);
        return redirectedURI;
    }

    private boolean canRedirect(URI redir) {
        String newScheme = redir.getScheme();
        String oldScheme = uri.getScheme();
        return switch (policy) {
            case ALWAYS -> true;
            case NEVER -> false;
            case NORMAL -> newScheme.equalsIgnoreCase(oldScheme)
                    || newScheme.equalsIgnoreCase("https");
        };
    }
}
