/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.lucene.search;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.ByteRunAutomaton;
import org.elasticsearch.test.ESTestCase;

import java.util.Locale;

public class AutomatonQueriesTests extends ESTestCase {

    public void testToCaseInsensitiveChar() {
        int codepoint = randomBoolean() ? randomInt(128) : randomUnicodeOfLength(1).codePointAt(0);
        Automaton automaton = AutomatonQueries.toCaseInsensitiveChar(codepoint);
        assertTrue(automaton.isDeterministic());
        ByteRunAutomaton runAutomaton = new ByteRunAutomaton(automaton);
        BytesRef br = new BytesRef(new String(Character.toChars(codepoint)));
        assertTrue(runAutomaton.run(br.bytes, br.offset, br.length));
        if (codepoint <= 128) {
            int altCase = Character.isLowerCase(codepoint) ? Character.toUpperCase(codepoint) : Character.toLowerCase(codepoint);
            br = new BytesRef(new String(Character.toChars(altCase)));
            assertTrue(runAutomaton.run(br.bytes, br.offset, br.length));
        }
    }

    public void testToCaseInsensitiveString() {
        String s = randomAlphaOfLengthBetween(10, 100);
        Automaton automaton = AutomatonQueries.toCaseInsensitiveString(s);
        assertTrue(automaton.isDeterministic());
        ByteRunAutomaton runAutomaton = new ByteRunAutomaton(automaton);
        BytesRef br = new BytesRef(s);
        assertTrue(runAutomaton.run(br.bytes, br.offset, br.length));
        br = new BytesRef(randomBoolean() ? s.toLowerCase(Locale.ROOT) : s.toUpperCase(Locale.ROOT));
        assertTrue(runAutomaton.run(br.bytes, br.offset, br.length));

        s = randomRealisticUnicodeOfLengthBetween(10, 100);
        automaton = AutomatonQueries.toCaseInsensitiveString(s);
        runAutomaton = new ByteRunAutomaton(automaton);
        br = new BytesRef(s);
        assertTrue(runAutomaton.run(br.bytes, br.offset, br.length));

        s = randomUnicodeOfLengthBetween(10, 100);
        automaton = AutomatonQueries.toCaseInsensitiveString(s);
        runAutomaton = new ByteRunAutomaton(automaton);
        br = new BytesRef(s);
        assertTrue(runAutomaton.run(br.bytes, br.offset, br.length));
    }

    public void testToCaseInsensitivePrefix() {
        String s = randomAlphaOfLengthBetween(10, 100);
        Automaton automaton = AutomatonQueries.caseInsensitivePrefix(s);
        assertTrue(automaton.isDeterministic());
        ByteRunAutomaton runAutomaton = new ByteRunAutomaton(automaton);
        BytesRef br = new BytesRef(s + randomRealisticUnicodeOfLengthBetween(10, 20));
        assertTrue(runAutomaton.run(br.bytes, br.offset, br.length));
        br = new BytesRef(
            (randomBoolean() ? s.toLowerCase(Locale.ROOT) : s.toUpperCase(Locale.ROOT)) + randomRealisticUnicodeOfLengthBetween(10, 20)
        );
        assertTrue(runAutomaton.run(br.bytes, br.offset, br.length));

        s = randomRealisticUnicodeOfLengthBetween(10, 100);
        automaton = AutomatonQueries.caseInsensitivePrefix(s);
        runAutomaton = new ByteRunAutomaton(automaton);
        br = new BytesRef(s + randomRealisticUnicodeOfLengthBetween(10, 20));
        assertTrue(runAutomaton.run(br.bytes, br.offset, br.length));

        s = randomUnicodeOfLengthBetween(10, 100);
        automaton = AutomatonQueries.caseInsensitivePrefix(s);
        runAutomaton = new ByteRunAutomaton(automaton);
        br = new BytesRef(s + randomRealisticUnicodeOfLengthBetween(10, 20));
        assertTrue(runAutomaton.run(br.bytes, br.offset, br.length));
    }
}
