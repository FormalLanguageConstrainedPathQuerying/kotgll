/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.analysis.common;

import org.elasticsearch.test.ESTestCase;

public class CharMatcherTests extends ESTestCase {

    public void testLetter() {
        assertTrue(CharMatcher.Basic.LETTER.isTokenChar('a')); 
        assertTrue(CharMatcher.Basic.LETTER.isTokenChar('é')); 
        assertTrue(CharMatcher.Basic.LETTER.isTokenChar('A')); 
        assertTrue(CharMatcher.Basic.LETTER.isTokenChar('Å')); 
        assertTrue(CharMatcher.Basic.LETTER.isTokenChar('ʰ')); 
        assertTrue(CharMatcher.Basic.LETTER.isTokenChar('ª')); 
        assertTrue(CharMatcher.Basic.LETTER.isTokenChar('ǅ')); 
        assertFalse(CharMatcher.Basic.LETTER.isTokenChar(' '));
        assertFalse(CharMatcher.Basic.LETTER.isTokenChar('0'));
        assertFalse(CharMatcher.Basic.LETTER.isTokenChar('!'));
    }

    public void testSpace() {
        assertTrue(CharMatcher.Basic.WHITESPACE.isTokenChar(' '));
        assertTrue(CharMatcher.Basic.WHITESPACE.isTokenChar('\t'));
        assertFalse(CharMatcher.Basic.WHITESPACE.isTokenChar('\u00A0')); 
    }

    public void testNumber() {
        assertTrue(CharMatcher.Basic.DIGIT.isTokenChar('1'));
        assertTrue(CharMatcher.Basic.DIGIT.isTokenChar('١')); 
        assertFalse(CharMatcher.Basic.DIGIT.isTokenChar(','));
        assertFalse(CharMatcher.Basic.DIGIT.isTokenChar('a'));
    }

    public void testSymbol() {
        assertTrue(CharMatcher.Basic.SYMBOL.isTokenChar('$')); 
        assertTrue(CharMatcher.Basic.SYMBOL.isTokenChar('+')); 
        assertTrue(CharMatcher.Basic.SYMBOL.isTokenChar('`')); 
        assertTrue(CharMatcher.Basic.SYMBOL.isTokenChar('^')); 
        assertTrue(CharMatcher.Basic.SYMBOL.isTokenChar('¦')); 
        assertFalse(CharMatcher.Basic.SYMBOL.isTokenChar(' '));
    }

    public void testPunctuation() {
        assertTrue(CharMatcher.Basic.PUNCTUATION.isTokenChar('(')); 
        assertTrue(CharMatcher.Basic.PUNCTUATION.isTokenChar(')')); 
        assertTrue(CharMatcher.Basic.PUNCTUATION.isTokenChar('_')); 
        assertTrue(CharMatcher.Basic.PUNCTUATION.isTokenChar('!')); 
        assertTrue(CharMatcher.Basic.PUNCTUATION.isTokenChar('-')); 
        assertTrue(CharMatcher.Basic.PUNCTUATION.isTokenChar('«')); 
        assertTrue(CharMatcher.Basic.PUNCTUATION.isTokenChar('»')); 
        assertFalse(CharMatcher.Basic.PUNCTUATION.isTokenChar(' '));
    }
}
