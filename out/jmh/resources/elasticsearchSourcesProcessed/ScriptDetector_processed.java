/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 * This Java port of CLD3 was derived from Google's CLD3 project at https:
 */

package org.elasticsearch.xpack.core.ml.inference.preprocessing.customwordembedding;

import static java.lang.Character.UnicodeBlock.ARABIC;
import static java.lang.Character.UnicodeBlock.CYRILLIC;
import static java.lang.Character.UnicodeBlock.GREEK;
import static java.lang.Character.UnicodeBlock.HANGUL_JAMO;
import static java.lang.Character.UnicodeBlock.HEBREW;
import static java.lang.Character.UnicodeBlock.HIRAGANA;
import static java.lang.Character.UnicodeBlock.KATAKANA;

/**
 * Derived from https:
 *
 * We take advantage of Java codepoints to determine the specific script value we care about
 */
public final class ScriptDetector {

    private ScriptDetector() {}

    public enum Script {
        kScriptError(0),

        kScriptOtherUtf8OneByte(1),
        kScriptOtherUtf8TwoBytes(2),
        kScriptOtherUtf8ThreeBytes(3),
        kScriptOtherUtf8FourBytes(4),

        kScriptGreek(5),
        kScriptCyrillic(6),
        kScriptHebrew(7),
        kScriptArabic(8),
        kScriptHangulJamo(9),  
        kScriptHiragana(10),    
        kScriptKatakana(11);    

        private final int code;

        Script(int code) {
            this.code = code;
        }

        public int toInt() {
            return code;
        }

        public static Script fromCodePoint(int codePoint) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
            if (GREEK.equals(block)) {
                return kScriptGreek;
            }
            if (CYRILLIC.equals(block)) {
                return kScriptCyrillic;
            }
            if (ARABIC.equals(block)) {
                return kScriptArabic;
            }
            if (HEBREW.equals(block)) {
                return kScriptHebrew;
            }
            if (KATAKANA.equals(block)) {
                return kScriptKatakana;
            }
            if (HIRAGANA.equals(block)) {
                return kScriptHiragana;
            }
            if (HANGUL_JAMO.equals(block)) {
                return kScriptHangulJamo;
            }

            if (codePoint > 0) {
                if (codePoint < 128) {
                    return kScriptOtherUtf8OneByte;
                }
                if (codePoint < 2048) {
                    return kScriptOtherUtf8TwoBytes;
                }
                if (codePoint < 65536) {
                    return kScriptOtherUtf8ThreeBytes;
                }
                if (codePoint < 1114112) {
                    return kScriptOtherUtf8FourBytes;
                }
            }

            return kScriptError;
        }
    }
}
