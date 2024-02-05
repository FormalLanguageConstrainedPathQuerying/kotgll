/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6953455 7045655
 * @summary CookieStore.add() cannot handle null URI parameter
 *     and An empty InMemoryCookieStore should not return true for removeAll
 */

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class NullUriCookieTest {
    static boolean fail = false;

    public static void main(String[] args) throws Exception {
        checkCookieNullUri();
    }

    static void checkCookieNullUri() throws Exception {
        CookieStore cookieStore = (new CookieManager()).getCookieStore();
        if (cookieStore.removeAll()) {
            fail = true;
        }
        checkFail("removeAll on empty store should return false");
        HttpCookie cookie = new HttpCookie("MY_COOKIE", "MY_COOKIE_VALUE");
        cookie.setDomain("foo.com");
        cookieStore.add(null, cookie);

        URI uri = new URI("http:
        List<HttpCookie> addedCookieList = cookieStore.get(uri);

        if (addedCookieList.size() != 1) {
           fail = true;
        }
        checkFail("Abnormal size of cookie jar");

        for (HttpCookie chip : addedCookieList) {
            if (!chip.equals(cookie)) {
                 fail = true;
            }
        }
        checkFail("Cookie not retrieved from Cookie Jar");
        boolean ret = cookieStore.remove(null,cookie);
        if (!ret) {
            fail = true;
        }
        checkFail("Abnormal removal behaviour from Cookie Jar");
    }

    static void checkFail(String exp) {
        if (fail) {
            throw new RuntimeException(exp);
        }
    }
}

