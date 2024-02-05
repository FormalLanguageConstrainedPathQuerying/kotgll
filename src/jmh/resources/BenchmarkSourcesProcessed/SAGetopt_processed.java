/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot;

import java.util.Arrays;
import java.util.List;

public class SAGetopt {

    private String[] _argv;

    private int _optind;    
    private int _optopt;    
    private String _optarg; 
    private boolean _optreset; 

    public SAGetopt(String[] args) {
        _argv  = args.clone();
        _optind   = 0;
        _optopt   = 1;
        _optarg   = null;
        _optreset = true;
    }

    public String getOptarg() {
        return _optarg;
    }

    public int getOptind() {
        return _optind;
    }

    private void extractOptarg(String opt) {
        if (_optind > _argv.length) {
            throw new SAGetoptException("Not enough arguments for '" + opt + "'");
        }

        if (! _argv[_optind].isEmpty() && _argv[_optind].charAt(0) == '-') {
            throw new SAGetoptException("Successor argument without leading - is expected for '" + opt +
                                        "' but we got '" + _argv[_optind] + "'");
        }

        _optarg = _argv[_optind];
        _optind += 1;
    }

    private String processLongOptions(String carg, String[] longOptStr) {
        List<String> los = Arrays.asList(longOptStr);
        String[] ca = carg.split("=", 2);

        if (los.contains(ca[0])) {
            if (ca.length > 1) {
                throw new SAGetoptException("Argument is not expected for '" + ca[0] + "'");
            }
            return carg;
        }

        if (los.contains(ca[0] + "=")) {
            if (ca.length > 1) {
                _optarg = ca[1];
            }
            else {
                try {
                    extractOptarg(ca[0]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new SAGetoptException("Argument is expected for '" + ca[0] + "'");
                }
            }

            return ca[0];
        }

        throw new SAGetoptException("Invalid option '" + ca[0] + "'");
    }

    public String next(String optStr, String[] longOptStr) {

        if (_optind >= _argv.length || _argv[_optind] == null) {
            return null;
        }

        String carg = _argv[_optind];
        _optarg = null;

        if (_optreset) {

            if (carg.isEmpty() || carg.charAt(0) != '-' || carg.equals("--")) {
                return null;
            }

            if (carg.startsWith("--")) {
                if (longOptStr == null || longOptStr.length == 0) {
                    return null;
                }
                ++ _optind;

                carg = carg.substring(2);
                return processLongOptions(carg, longOptStr);
            }

            if (optStr == null || optStr.length() == 0) {
                return null;
            }

            _optreset = false;
            _optopt = 1;
        }

        char ch = carg.charAt(_optopt);

        _optopt += 1;


        int chIndex = optStr.indexOf(ch);
        if (chIndex == -1) {
            throw new SAGetoptException("Invalid option '" + ch + "'");
        }

        if (_optopt >= carg.length()) {
            _optind += 1;
            _optreset = true;
        }

        if (chIndex < optStr.length()-1 && optStr.charAt(chIndex+1) == ':') {
            extractOptarg(String.valueOf(ch));
        }

        return String.valueOf(ch);
    }
}
