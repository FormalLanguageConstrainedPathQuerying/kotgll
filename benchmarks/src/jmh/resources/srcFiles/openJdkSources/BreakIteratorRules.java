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

/*
 */

/*
 * Licensed Materials - Property of IBM
 *
 * (C) Copyright IBM Corp. 1999 All Rights Reserved.
 * (C) IBM Corp. 1997-1998.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */

package sun.text.resources;

import java.util.ListResourceBundle;

/**
 * Default break-iterator rules.  These rules are more or less general for
 * all locales, although there are probably a few we're missing.  The
 * behavior currently mimics the behavior of BreakIterator in JDK 1.2.
 * There are known deficiencies in this behavior, including the fact that
 * the logic for handling CJK characters works for Japanese but not for
 * Chinese, and that we don't currently have an appropriate locale for
 * Thai.  The resources will eventually be updated to fix these problems.
 */

 /* Modified for Hindi 3/1/99. */

/*
 * Since JDK 1.5.0, this file no longer goes to runtime and is used at J2SE
 * build phase in order to create [Character|Word|Line|Sentence]BreakIteratorData
 * files which are used on runtime instead.
 */

public class BreakIteratorRules extends ListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "WordBreakRules",
              "<ignore>=[:Cf:];"

              + "<enclosing>=[:Mn::Me:];"

              + "<danda>=[\u0964\u0965];"
              + "<kanji>=[\u3005\u4e00-\u9fa5\uf900-\ufa2d];"
              + "<kata>=[\u30a1-\u30fa\u30fd\u30fe];"
              + "<hira>=[\u3041-\u3094\u309d\u309e];"
              + "<cjk-diacrit>=[\u3099-\u309c\u30fb\u30fc];"
              + "<letter-base>=[:L::Mc:^[<kanji><kata><hira><cjk-diacrit>]];"
              + "<let>=(<letter-base><enclosing>*);"
              + "<digit-base>=[:N:];"
              + "<dgt>=(<digit-base><enclosing>*);"

              + "<mid-word>=[:Pd::Pc:\u00ad\u2027\\\"\\\'\\.];"

              + "<mid-num>=[\\\"\\\'\\,\u066b\\.];"

              + "<pre-num>=[:Sc:\\#\\.^\u00a2];"

              + "<post-num>=[\\%\\&\u00a2\u066a\u2030\u2031];"

              + "<ls>=[\n\u000c\u2028\u2029];"

              + "<ws-base>=[:Zs:\t];"
              + "<ws>=(<ws-base><enclosing>*);"

              + "<word>=((<let><let>*(<mid-word><let><let>*)*){<danda>});"

              + "<number>=(<dgt><dgt>*(<mid-num><dgt><dgt>*)*);"

              + ".;"

              + "{<word>}(<number><word>)*{<number>{<post-num>}};"

              + "<pre-num>(<number><word>)*{<number>{<post-num>}};"

              + "<ws>*{\r}{<ls>};"

              + "[<kata><cjk-diacrit>]*;"

              + "[<hira><cjk-diacrit>]*;"

              + "<kanji>*;"

              + "<base>=[^<enclosing>^[:Cc::Cf::Zl::Zp:]];"
              + "<base><enclosing><enclosing>*;"
            },

            { "LineBreakRules",
              "<break>=[\u0003\t\n\f\u2028\u2029];"

              + "<ignore>=[:Cf:[:Cc:^[<break>\r]]];"

              + "<enclosing>=[:Mn::Me:];"

              + "<danda>=[\u0964\u0965];"

              + "<glue>=[\u00a0\u0f0c\u2007\u2011\u202f\ufeff];"

              + "<space>=[:Zs::Cc:^[<glue><break>\r]];"

              + "<dash>=[:Pd:\u00ad^<glue>];"

              + "<pre-word>=[:Sc::Ps::Pi:^[\u00a2]\\\"\\\'];"

              + "<post-word>=[\\\":Pe::Pf:\\!\\%\\.\\,\\:\\;\\?\u00a2\u00b0\u066a\u2030-\u2034\u2103"
              + "\u2105\u2109\u3001\u3002\u3005\u3041\u3043\u3045\u3047\u3049\u3063"
              + "\u3083\u3085\u3087\u308e\u3099-\u309e\u30a1\u30a3\u30a5\u30a7\u30a9"
              + "\u30c3\u30e3\u30e5\u30e7\u30ee\u30f5\u30f6\u30fc-\u30fe\uff01\uff05"
              + "\uff0c\uff0e\uff1a\uff1b\uff1f];"

              + "<kanji>=[\u4e00-\u9fa5\uac00-\ud7a3\uf900-\ufa2d\ufa30-\ufa6a\u3041-\u3094\u30a1-\u30fa^[<post-word><ignore>]];"

              + "<digit>=[:Nd::No:];"

              + "<mid-num>=[\\.\\,];"

              + "<char>=[^[<break><space><dash><kanji><glue><ignore><pre-word><post-word><mid-num>\r<danda>]];"

              + "<number>=([<pre-word><dash>]*<digit><digit>*(<mid-num><digit><digit>*)*);"

              + "<word-core>=(<char>*|<kanji>|<number>);"

              + "<word-suffix>=((<dash><dash>*|<post-word>*));"

              + "<word>=(<pre-word>*<word-core><word-suffix>);"

              + "<hack1>=[\\(];"
              + "<hack2>=[\\)];"
              + "<hack3>=[\\$\\'];"

              + "<word>(((<space>*<glue><glue>*{<space>})|<hack3>)<word>)*<space>*{<enclosing>*}{<hack1><hack2><post-word>*}{<enclosing>*}{\r}{<break>};"
              + "\r<break>;"
            },

            { "SentenceBreakRules",
              "<ignore>=[:Mn::Me::Cf:];"

              + "<letter>=[:L:];"

              + "<lc>=[:Ll:];"

              + "<uc>=[:Lu:];"

              + "<notlc>=[<letter>^<lc>];"

              + "<space>=[\t\r\f\n\u2028:Zs:];"

              + "<start-punctuation>=[:Ps::Pi:\\\"\\\'];"

              + "<end>=[:Pe::Pf:\\\"\\\'];"

              + "<digit>=[:N:];"

              + "<term>=[\\!\\?\u3002\uff01\uff1f];"

              + "<period>=[\\.\uff0e];"

              + "<comma>=[\\,];"

              + "<sent-start>=[^[:L:<space><start-punctuation><end><digit><term><period><comma>\u2029<ignore>]];"

              + "<danda>=[\u0964\u0965];"

              + ".*?{\u2029};"

              + ".*?<danda><space>*;"


              + ".*?<period>[<period><end>]*<space><space>*/<notlc>;"
              + ".*?<period>[<period><end>]*<space>*/[<start-punctuation><sent-start>][<start-punctuation><sent-start>]*<letter>;"

              + ".*?<term>[<term><period><end>]*<space>*{\u2029};"


              + "!<sent-start><start-punctuation>*<space>*<end>*<period>;"

              + "![<sent-start><lc><digit>]<start-punctuation>*<space>*<end>*<term>;"
            }
        };
    }
}
