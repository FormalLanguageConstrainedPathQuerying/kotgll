/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996-1998 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.text;

import java.text.Normalizer;
import java.util.Vector;
import java.util.Locale;

/**
 * The {@code RuleBasedCollator} class is a concrete subclass of
 * {@code Collator} that provides a simple, data-driven, table
 * collator.  With this class you can create a customized table-based
 * {@code Collator}.  {@code RuleBasedCollator} maps
 * characters to sort keys.
 *
 * <p>
 * {@code RuleBasedCollator} has the following restrictions
 * for efficiency (other subclasses may be used for more complex languages) :
 * <ol>
 * <li>If a special collation rule controlled by a &lt;modifier&gt; is
 *     specified it applies to the whole collator object.
 * <li>All non-mentioned characters are at the end of the
 *     collation order.
 * </ol>
 *
 * <p>
 * The collation table is composed of a list of collation rules, where each
 * rule is of one of three forms:
 * <pre>
 *    &lt;modifier&gt;
 *    &lt;relation&gt; &lt;text-argument&gt;
 *    &lt;reset&gt; &lt;text-argument&gt;
 * </pre>
 * The definitions of the rule elements is as follows:
 * <UL>
 *    <LI><strong>Text-Argument</strong>: A text-argument is any sequence of
 *        characters, excluding special characters (that is, common
 *        whitespace characters [0009-000D, 0020] and rule syntax characters
 *        [0021-002F, 003A-0040, 005B-0060, 007B-007E]). If those
 *        characters are desired, you can put them in single quotes
 *        (e.g. ampersand =&gt; '&amp;'). Note that unquoted white space characters
 *        are ignored; e.g. {@code b c} is treated as {@code bc}.
 *    <LI><strong>Modifier</strong>: There are currently two modifiers that
 *        turn on special collation rules.
 *        <UL>
 *            <LI>'@' : Turns on backwards sorting of accents (secondary
 *                      differences), as in French.
 *            <LI>'!' : Turns on Thai/Lao vowel-consonant swapping.  If this
 *                      rule is in force when a Thai vowel of the range
 *                      &#92;U0E40-&#92;U0E44 precedes a Thai consonant of the range
 *                      &#92;U0E01-&#92;U0E2E OR a Lao vowel of the range &#92;U0EC0-&#92;U0EC4
 *                      precedes a Lao consonant of the range &#92;U0E81-&#92;U0EAE then
 *                      the vowel is placed after the consonant for collation
 *                      purposes.
 *        </UL>
 *        <p>'@' : Indicates that accents are sorted backwards, as in French.
 *    <LI><strong>Relation</strong>: The relations are the following:
 *        <UL>
 *            <LI>'&lt;' : Greater, as a letter difference (primary)
 *            <LI>';' : Greater, as an accent difference (secondary)
 *            <LI>',' : Greater, as a case difference (tertiary)
 *            <LI>'=' : Equal
 *        </UL>
 *    <LI><strong>Reset</strong>: There is a single reset
 *        which is used primarily for contractions and expansions, but which
 *        can also be used to add a modification at the end of a set of rules.
 *        <p>'&amp;' : Indicates that the next rule follows the position to where
 *            the reset text-argument would be sorted.
 * </UL>
 *
 * <p>
 * This sounds more complicated than it is in practice. For example, the
 * following are equivalent ways of expressing the same thing:
 * <blockquote>
 * <pre>
 * a &lt; b &lt; c
 * a &lt; b &amp; b &lt; c
 * a &lt; c &amp; a &lt; b
 * </pre>
 * </blockquote>
 * Notice that the order is important, as the subsequent item goes immediately
 * after the text-argument. The following are not equivalent:
 * <blockquote>
 * <pre>
 * a &lt; b &amp; a &lt; c
 * a &lt; c &amp; a &lt; b
 * </pre>
 * </blockquote>
 * Either the text-argument must already be present in the sequence, or some
 * initial substring of the text-argument must be present. (e.g. "a &lt; b &amp; ae &lt;
 * e" is valid since "a" is present in the sequence before "ae" is reset). In
 * this latter case, "ae" is not entered and treated as a single character;
 * instead, "e" is sorted as if it were expanded to two characters: "a"
 * followed by an "e". This difference appears in natural languages: in
 * traditional Spanish "ch" is treated as though it contracts to a single
 * character (expressed as "c &lt; ch &lt; d"), while in traditional German
 * a-umlaut is treated as though it expanded to two characters
 * (expressed as "a,A &lt; b,B ... &amp;ae;&#92;u00e3&amp;AE;&#92;u00c3").
 * [&#92;u00e3 and &#92;u00c3 are, of course, the escape sequences for a-umlaut.]
 * <p>
 * <strong>Ignorable Characters</strong>
 * <p>
 * For ignorable characters, the first rule must start with a relation (the
 * examples we have used above are really fragments; "a &lt; b" really should be
 * "&lt; a &lt; b"). If, however, the first relation is not "&lt;", then all the all
 * text-arguments up to the first "&lt;" are ignorable. For example, ", - &lt; a &lt; b"
 * makes "-" an ignorable character, as we saw earlier in the word
 * "black-birds". In the samples for different languages, you see that most
 * accents are ignorable.
 *
 * <p><strong>Normalization and Accents</strong>
 * <p>
 * {@code RuleBasedCollator} automatically processes its rule table to
 * include both pre-composed and combining-character versions of
 * accented characters.  Even if the provided rule string contains only
 * base characters and separate combining accent characters, the pre-composed
 * accented characters matching all canonical combinations of characters from
 * the rule string will be entered in the table.
 * <p>
 * This allows you to use a RuleBasedCollator to compare accented strings
 * even when the collator is set to NO_DECOMPOSITION.  There are two caveats,
 * however.  First, if the strings to be collated contain combining
 * sequences that may not be in canonical order, you should set the collator to
 * CANONICAL_DECOMPOSITION or FULL_DECOMPOSITION to enable sorting of
 * combining sequences.  Second, if the strings contain characters with
 * compatibility decompositions (such as full-width and half-width forms),
 * you must use FULL_DECOMPOSITION, since the rule tables only include
 * canonical mappings.
 *
 * <p><strong>Errors</strong>
 * <p>
 * The following are errors:
 * <UL>
 *     <LI>A text-argument contains unquoted punctuation symbols
 *        (e.g. "a &lt; b-c &lt; d").
 *     <LI>A relation or reset character not followed by a text-argument
 *        (e.g. "a &lt; ,b").
 *     <LI>A reset where the text-argument (or an initial substring of the
 *         text-argument) is not already in the sequence.
 *         (e.g. "a &lt; b &amp; e &lt; f")
 * </UL>
 * If you produce one of these errors, a {@code RuleBasedCollator} throws
 * a {@code ParseException}.
 *
 * <p><strong>Examples</strong>
 * <p>Simple:     "&lt; a &lt; b &lt; c &lt; d"
 * <p>Norwegian:  "&lt; a, A &lt; b, B &lt; c, C &lt; d, D &lt; e, E &lt; f, F
 *                 &lt; g, G &lt; h, H &lt; i, I &lt; j, J &lt; k, K &lt; l, L
 *                 &lt; m, M &lt; n, N &lt; o, O &lt; p, P &lt; q, Q &lt; r, R
 *                 &lt; s, S &lt; t, T &lt; u, U &lt; v, V &lt; w, W &lt; x, X
 *                 &lt; y, Y &lt; z, Z
 *                 &lt; &#92;u00E6, &#92;u00C6
 *                 &lt; &#92;u00F8, &#92;u00D8
 *                 &lt; &#92;u00E5 = a&#92;u030A, &#92;u00C5 = A&#92;u030A;
 *                      aa, AA"
 *
 * <p>
 * To create a {@code RuleBasedCollator} object with specialized
 * rules tailored to your needs, you construct the {@code RuleBasedCollator}
 * with the rules contained in a {@code String} object. For example:
 * <blockquote>
 * <pre>
 * String simple = "&lt; a&lt; b&lt; c&lt; d";
 * RuleBasedCollator mySimple = new RuleBasedCollator(simple);
 * </pre>
 * </blockquote>
 * Or:
 * <blockquote>
 * <pre>
 * String Norwegian = "&lt; a, A &lt; b, B &lt; c, C &lt; d, D &lt; e, E &lt; f, F &lt; g, G &lt; h, H &lt; i, I" +
 *                    "&lt; j, J &lt; k, K &lt; l, L &lt; m, M &lt; n, N &lt; o, O &lt; p, P &lt; q, Q &lt; r, R" +
 *                    "&lt; s, S &lt; t, T &lt; u, U &lt; v, V &lt; w, W &lt; x, X &lt; y, Y &lt; z, Z" +
 *                    "&lt; &#92;u00E6, &#92;u00C6" +     
 *                    "&lt; &#92;u00F8, &#92;u00D8" +     
 *                    "&lt; &#92;u00E5 = a&#92;u030A," +  
 *                    "  &#92;u00C5 = A&#92;u030A;" +  
 *                    "  aa, AA";
 * RuleBasedCollator myNorwegian = new RuleBasedCollator(Norwegian);
 * </pre>
 * </blockquote>
 *
 * <p>
 * A new collation rules string can be created by concatenating rules
 * strings. For example, the rules returned by {@link #getRules()} could
 * be concatenated to combine multiple {@code RuleBasedCollator}s.
 *
 * <p>
 * The following example demonstrates how to change the order of
 * non-spacing accents,
 * <blockquote>
 * <pre>
 * 
 * String oldRules = "=&#92;u0301;&#92;u0300;&#92;u0302;&#92;u0308"    
 *                 + ";&#92;u0327;&#92;u0303;&#92;u0304;&#92;u0305"    
 *                 + ";&#92;u0306;&#92;u0307;&#92;u0309;&#92;u030A"    
 *                 + ";&#92;u030B;&#92;u030C;&#92;u030D;&#92;u030E"    
 *                 + ";&#92;u030F;&#92;u0310;&#92;u0311;&#92;u0312"    
 *                 + "&lt; a , A ; ae, AE ; &#92;u00e6 , &#92;u00c6"
 *                 + "&lt; b , B &lt; c, C &lt; e, E &amp; C &lt; d, D";
 * 
 * String addOn = "&amp; &#92;u0300 ; &#92;u0308 ; &#92;u0302";
 * RuleBasedCollator myCollator = new RuleBasedCollator(oldRules + addOn);
 * </pre>
 * </blockquote>
 *
 * @see        Collator
 * @see        CollationElementIterator
 * @author     Helena Shih, Laura Werner, Richard Gillam
 * @since 1.1
 */
public class RuleBasedCollator extends Collator{

    /**
     * RuleBasedCollator constructor.  This takes the table rules and builds
     * a collation table out of them.  Please see RuleBasedCollator class
     * description for more details on the collation rule syntax.
     * @see java.util.Locale
     * @param rules the collation rules to build the collation table from.
     * @throws    ParseException A format exception
     * will be thrown if the build process of the rules fails. For
     * example, build rule "a &lt; ? &lt; d" will cause the constructor to
     * throw the ParseException because the '?' is not quoted.
     */
    public RuleBasedCollator(String rules) throws ParseException {
        this(rules, Collator.CANONICAL_DECOMPOSITION);
    }

    /**
     * RuleBasedCollator constructor.  This takes the table rules and builds
     * a collation table out of them.  Please see RuleBasedCollator class
     * description for more details on the collation rule syntax.
     * @see java.util.Locale
     * @param rules the collation rules to build the collation table from.
     * @param decomp the decomposition strength used to build the
     * collation table and to perform comparisons.
     * @throws    ParseException A format exception
     * will be thrown if the build process of the rules fails. For
     * example, build rule "a < ? < d" will cause the constructor to
     * throw the ParseException because the '?' is not quoted.
     */
    RuleBasedCollator(String rules, int decomp) throws ParseException {
        setStrength(Collator.TERTIARY);
        setDecomposition(decomp);
        tables = new RBCollationTables(rules, decomp);
    }

    /**
     * "Copy constructor."  Used in clone() for performance.
     */
    private RuleBasedCollator(RuleBasedCollator that) {
        setStrength(that.getStrength());
        setDecomposition(that.getDecomposition());
        tables = that.tables;
    }

    /**
     * Gets the table-based rules for the collation object.
     * @return returns the collation rules that the table collation object
     * was created from.
     */
    public String getRules()
    {
        return tables.getRules();
    }

    /**
     * Returns a CollationElementIterator for the given String.
     *
     * @param source the string to be collated
     * @return a {@code CollationElementIterator} object
     * @see java.text.CollationElementIterator
     */
    public CollationElementIterator getCollationElementIterator(String source) {
        return new CollationElementIterator( source, this );
    }

    /**
     * Returns a CollationElementIterator for the given CharacterIterator.
     *
     * @param source the character iterator to be collated
     * @return a {@code CollationElementIterator} object
     * @see java.text.CollationElementIterator
     * @since 1.2
     */
    public CollationElementIterator getCollationElementIterator(
                                                CharacterIterator source) {
        return new CollationElementIterator( source, this );
    }

    /**
     * Compares the character data stored in two different strings based on the
     * collation rules.  Returns information about whether a string is less
     * than, greater than or equal to another string in a language.
     * This can be overridden in a subclass.
     *
     * @throws    NullPointerException if {@code source} or {@code target} is null.
     */
    public synchronized int compare(String source, String target)
    {
        if (source == null || target == null) {
            throw new NullPointerException();
        }

        if (source.equals(target)) {
            return Collator.EQUAL;
        }


        int result = Collator.EQUAL;

        if (sourceCursor == null) {
            sourceCursor = getCollationElementIterator(source);
        } else {
            sourceCursor.setText(source);
        }
        if (targetCursor == null) {
            targetCursor = getCollationElementIterator(target);
        } else {
            targetCursor.setText(target);
        }

        int sOrder = 0, tOrder = 0;

        boolean initialCheckSecTer = getStrength() >= Collator.SECONDARY;
        boolean checkSecTer = initialCheckSecTer;
        boolean checkTertiary = getStrength() >= Collator.TERTIARY;

        boolean gets = true, gett = true;

        while(true) {
            if (gets) sOrder = sourceCursor.next(); else gets = true;
            if (gett) tOrder = targetCursor.next(); else gett = true;

            if ((sOrder == CollationElementIterator.NULLORDER)||
                (tOrder == CollationElementIterator.NULLORDER))
                break;

            int pSOrder = CollationElementIterator.primaryOrder(sOrder);
            int pTOrder = CollationElementIterator.primaryOrder(tOrder);

            if (sOrder == tOrder) {
                if (tables.isFrenchSec() && pSOrder != 0) {
                    if (!checkSecTer) {
                        checkSecTer = initialCheckSecTer;
                        checkTertiary = false;
                    }
                }
                continue;
            }

            if ( pSOrder != pTOrder )
            {
                if (sOrder == 0) {
                    gett = false;
                    continue;
                }
                if (tOrder == 0) {
                    gets = false;
                    continue;
                }


                if (pSOrder == 0)  
                {
                    if (checkSecTer) {
                        result = Collator.GREATER;  
                        checkSecTer = false;
                    }
                    gett = false;
                }
                else if (pTOrder == 0)
                {
                    if (checkSecTer) {
                        result = Collator.LESS;  
                        checkSecTer = false;
                    }
                    gets = false;
                } else {
                    if (pSOrder < pTOrder) {
                        return Collator.LESS;  
                    } else {
                        return Collator.GREATER;  
                    }
                }
            } else { 

                if (checkSecTer) {
                    short secSOrder = CollationElementIterator.secondaryOrder(sOrder);
                    short secTOrder = CollationElementIterator.secondaryOrder(tOrder);
                    if (secSOrder != secTOrder) {
                        result = (secSOrder < secTOrder) ? Collator.LESS : Collator.GREATER;
                        checkSecTer = false;
                    } else {
                        if (checkTertiary) {
                            short terSOrder = CollationElementIterator.tertiaryOrder(sOrder);
                            short terTOrder = CollationElementIterator.tertiaryOrder(tOrder);
                            if (terSOrder != terTOrder) {
                                result = (terSOrder < terTOrder) ? Collator.LESS : Collator.GREATER;
                                checkTertiary = false;
                            }
                        }
                    }
                } 

            }  
        } 

        if (sOrder != CollationElementIterator.NULLORDER) {
            do {
                if (CollationElementIterator.primaryOrder(sOrder) != 0) {
                    return Collator.GREATER; 
                }
                else if (CollationElementIterator.secondaryOrder(sOrder) != 0) {
                    if (checkSecTer) {
                        result = Collator.GREATER;  
                        checkSecTer = false;
                    }
                }
            } while ((sOrder = sourceCursor.next()) != CollationElementIterator.NULLORDER);
        }
        else if (tOrder != CollationElementIterator.NULLORDER) {
            do {
                if (CollationElementIterator.primaryOrder(tOrder) != 0)
                    return Collator.LESS; 
                else if (CollationElementIterator.secondaryOrder(tOrder) != 0) {
                    if (checkSecTer) {
                        result = Collator.LESS;  
                        checkSecTer = false;
                    }
                }
            } while ((tOrder = targetCursor.next()) != CollationElementIterator.NULLORDER);
        }

        if (result == 0 && getStrength() == IDENTICAL) {
            int mode = getDecomposition();
            Normalizer.Form form;
            if (mode == CANONICAL_DECOMPOSITION) {
                form = Normalizer.Form.NFD;
            } else if (mode == FULL_DECOMPOSITION) {
                form = Normalizer.Form.NFKD;
            } else {
                return source.compareTo(target);
            }

            String sourceDecomposition = Normalizer.normalize(source, form);
            String targetDecomposition = Normalizer.normalize(target, form);
            return sourceDecomposition.compareTo(targetDecomposition);
        }
        return result;
    }

    /**
     * Transforms the string into a series of characters that can be compared
     * with CollationKey.compareTo. This overrides java.text.Collator.getCollationKey.
     * It can be overridden in a subclass.
     */
    public synchronized CollationKey getCollationKey(String source)
    {
        if (source == null)
            return null;

        if (primResult == null) {
            primResult = new StringBuilder();
            secResult = new StringBuilder();
            terResult = new StringBuilder();
        } else {
            primResult.setLength(0);
            secResult.setLength(0);
            terResult.setLength(0);
        }
        int order = 0;
        boolean compareSec = (getStrength() >= Collator.SECONDARY);
        boolean compareTer = (getStrength() >= Collator.TERTIARY);
        int secOrder = CollationElementIterator.NULLORDER;
        int terOrder = CollationElementIterator.NULLORDER;
        int preSecIgnore = 0;

        if (sourceCursor == null) {
            sourceCursor = getCollationElementIterator(source);
        } else {
            sourceCursor.setText(source);
        }

        while ((order = sourceCursor.next()) !=
               CollationElementIterator.NULLORDER)
        {
            secOrder = CollationElementIterator.secondaryOrder(order);
            terOrder = CollationElementIterator.tertiaryOrder(order);
            if (!CollationElementIterator.isIgnorable(order))
            {
                primResult.append((char) (CollationElementIterator.primaryOrder(order)
                                    + COLLATIONKEYOFFSET));

                if (compareSec) {
                    if (tables.isFrenchSec() && preSecIgnore < secResult.length()) {
                        RBCollationTables.reverse(secResult, preSecIgnore, secResult.length());
                    }
                    secResult.append((char)(secOrder+ COLLATIONKEYOFFSET));
                    preSecIgnore = secResult.length();
                }
                if (compareTer) {
                    terResult.append((char)(terOrder+ COLLATIONKEYOFFSET));
                }
            }
            else
            {
                if (compareSec && secOrder != 0)
                    secResult.append((char)
                        (secOrder + tables.getMaxSecOrder() + COLLATIONKEYOFFSET));
                if (compareTer && terOrder != 0)
                    terResult.append((char)
                        (terOrder + tables.getMaxTerOrder() + COLLATIONKEYOFFSET));
            }
        }
        if (tables.isFrenchSec())
        {
            if (preSecIgnore < secResult.length()) {
                RBCollationTables.reverse(secResult, preSecIgnore, secResult.length());
            }
            RBCollationTables.reverse(secResult, 0, secResult.length());
        }
        primResult.append((char)0);
        secResult.append((char)0);
        secResult.append(terResult);
        primResult.append(secResult);

        if (getStrength() == IDENTICAL) {
            primResult.append((char)0);
            int mode = getDecomposition();
            if (mode == CANONICAL_DECOMPOSITION) {
                primResult.append(Normalizer.normalize(source, Normalizer.Form.NFD));
            } else if (mode == FULL_DECOMPOSITION) {
                primResult.append(Normalizer.normalize(source, Normalizer.Form.NFKD));
            } else {
                primResult.append(source);
            }
        }
        return new RuleBasedCollationKey(source, primResult.toString());
    }

    /**
     * Standard override; no change in semantics.
     */
    public Object clone() {
        if (getClass() == RuleBasedCollator.class) {
            return new RuleBasedCollator(this);
        }
        else {
            RuleBasedCollator result = (RuleBasedCollator) super.clone();
            result.primResult = null;
            result.secResult = null;
            result.terResult = null;
            result.sourceCursor = null;
            result.targetCursor = null;
            return result;
        }
    }

    /**
     * Compares the equality of two collation objects.
     * @param obj the table-based collation object to be compared with this.
     * @return true if the current table-based collation object is the same
     * as the table-based collation object obj; false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;  
        RuleBasedCollator other = (RuleBasedCollator) obj;
        return (getRules().equals(other.getRules()));
    }

    /**
     * Generates the hash code for the table-based collation object
     */
    @Override
    public int hashCode() {
        return getRules().hashCode();
    }

    /**
     * Allows CollationElementIterator access to the tables object
     */
    RBCollationTables getTables() {
        return tables;
    }


    static final int CHARINDEX = 0x70000000;  
    static final int EXPANDCHARINDEX = 0x7E000000; 
    static final int CONTRACTCHARINDEX = 0x7F000000;  
    static final int UNMAPPED = 0xFFFFFFFF;

    private static final int COLLATIONKEYOFFSET = 1;

    private RBCollationTables tables = null;

    private StringBuilder primResult = null;
    private StringBuilder secResult = null;
    private StringBuilder terResult = null;
    private CollationElementIterator sourceCursor = null;
    private CollationElementIterator targetCursor = null;
}
