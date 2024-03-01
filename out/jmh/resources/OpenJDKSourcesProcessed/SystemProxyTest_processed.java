/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

import sun.net.spi.DefaultProxySelector;

public class SystemProxyTest {

    public static void main(final String[] args) throws Exception {
        final ProxySelector ps = new DefaultProxySelector();
        final URI uri = new URI("http:
        final List<Proxy> proxies = ps.select(uri);
        if (proxies == null) {
            throw new AssertionError("DefaultProxySelector.select(URI) returned null for uri: "
                    + uri);
        }
        if (proxies.isEmpty()) {
            throw new AssertionError("DefaultProxySelector.select(URI) returned empty list" +
                    " for uri: " + uri);
        }
        System.out.println("returned proxies list: " + proxies);
        for (final Proxy p : proxies) {
            if (p == null) {
                throw new AssertionError("null proxy in proxies list for uri: " + uri);
            }
        }
    }
}
