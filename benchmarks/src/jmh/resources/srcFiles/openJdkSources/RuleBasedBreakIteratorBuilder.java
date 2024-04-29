/*
 * Copyright (c) 2003, 2022, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.generatebreakiteratordata;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.CRC32;
import sun.text.CompactByteArray;

/**
 * This class has the job of constructing a RuleBasedBreakIterator from a
 * textual description. A Builder is constructed by GenerateBreakIteratorData,
 * which uses it to construct the iterator itself and then throws it away.
 * <p>The construction logic is separated out into its own class for two primary
 * reasons:
 * <ul>
 * <li>The construction logic is quite sophisticated and large. Separating
 * it out into its own class means the code must only be loaded into memory
 * while a RuleBasedBreakIterator is being constructed, and can be purged after
 * that.
 * <li>There is a fair amount of state that must be maintained throughout the
 * construction process that is not needed by the iterator after construction.
 * Separating this state out into another class prevents all of the functions
 * that construct the iterator from having to have really long parameter lists,
 * (hopefully) contributing to readability and maintainability.
 * </ul>
 * <p>
 * It'd be really nice if this could be an independent class rather than an
 * inner class, because that would shorten the source file considerably, but
 * making Builder an inner class of RuleBasedBreakIterator allows it direct
 * access to RuleBasedBreakIterator's private members, which saves us from
 * having to provide some kind of "back door" to the Builder class that could
 * then also be used by other classes.
 */
class RuleBasedBreakIteratorBuilder {

    /**
     * A token used as a character-category value to identify ignore characters
     */
    protected static final byte IGNORE = -1;

    /**
     * Tables that indexes from character values to character category numbers
     */
    private CompactByteArray charCategoryTable = null;
    private SupplementaryCharacterData supplementaryCharCategoryTable = null;

    /**
     * The table of state transitions used for forward iteration
     */
    private short[] stateTable = null;

    /**
     * The table of state transitions used to sync up the iterator with the
     * text in backwards and random-access iteration
     */
    private short[] backwardsStateTable = null;

    /**
     * A list of flags indicating which states in the state table are accepting
     * ("end") states
     */
    private boolean[] endStates = null;

    /**
     * A list of flags indicating which states in the state table are
     * lookahead states (states which turn lookahead on and off)
     */
    private boolean[] lookaheadStates = null;

    /**
     * A table for additional data. May be used by a subclass of
     * RuleBasedBreakIterator.
     */
    private byte[] additionalData = null;

    /**
     * The number of character categories (and, thus, the number of columns in
     * the state tables)
     */
    private int numCategories;

    /**
     * A temporary holding place used for calculating the character categories.
     * This object contains CharSet objects.
     */
    protected Vector<CharSet> categories = null;

    /**
     * A table used to map parts of regexp text to lists of character
     * categories, rather than having to figure them out from scratch each time
     */
    protected Hashtable<String, Object> expressions = null;

    /**
     * A temporary holding place for the list of ignore characters
     */
    protected CharSet ignoreChars = null;

    /**
     * A temporary holding place where the forward state table is built
     */
    protected Vector<short[]> tempStateTable = null;

    /**
     * A list of all the states that have to be filled in with transitions to
     * the next state that is created.  Used when building the state table from
     * the regular expressions.
     */
    protected Vector<Integer> decisionPointList = null;

    /**
     * A stack for holding decision point lists.  This is used to handle nested
     * parentheses and braces in regexps.
     */
    protected Stack<Vector<Integer>> decisionPointStack = null;

    /**
     * A list of states that loop back on themselves.  Used to handle .*?
     */
    protected Vector<Integer> loopingStates = null;

    /**
     * Looping states actually have to be backfilled later in the process
     * than everything else.  This is where the list of states to backfill
     * is accumulated.  This is also used to handle .*?
     */
    protected Vector<Integer> statesToBackfill = null;

    /**
     * A list mapping pairs of state numbers for states that are to be combined
     * to the state number of the state representing their combination.  Used
     * in the process of making the state table deterministic to prevent
     * infinite recursion.
     */
    protected Vector<int[]> mergeList = null;

    /**
     * A flag that is used to indicate when the list of looping states can
     * be reset.
     */
    protected boolean clearLoopingStates = false;

    /**
     * A bit mask used to indicate a bit in the table's flags column that marks
     * a state as an accepting state.
     */
    protected static final int END_STATE_FLAG = 0x8000;

    /**
     * A bit mask used to indicate a bit in the table's flags column that marks
     * a state as one the builder shouldn't loop to any looping states
     */
    protected static final int DONT_LOOP_FLAG = 0x4000;

    /**
     * A bit mask used to indicate a bit in the table's flags column that marks
     * a state as a lookahead state.
     */
    protected static final int LOOKAHEAD_STATE_FLAG = 0x2000;

    /**
     * A bit mask representing the union of the mask values listed above.
     * Used for clearing or masking off the flag bits.
     */
    protected static final int ALL_FLAGS = END_STATE_FLAG
                                         | LOOKAHEAD_STATE_FLAG
                                         | DONT_LOOP_FLAG;

    /**
     * This is the main function for setting up the BreakIterator's tables. It
     * just vectors different parts of the job off to other functions.
     */
    public RuleBasedBreakIteratorBuilder(String description) {
        Vector<String> tempRuleList = buildRuleList(description);
        buildCharCategories(tempRuleList);
        buildStateTable(tempRuleList);
        buildBackwardsStateTable(tempRuleList);
    }

    /**
     * Thus function has three main purposes:
     * <ul><li>Perform general syntax checking on the description, so the rest
     * of the build code can assume that it's parsing a legal description.
     * <li>Split the description into separate rules
     * <li>Perform variable-name substitutions (so that no one else sees
     * variable names)
     * </ul>
     */
    private Vector<String> buildRuleList(String description) {

        Vector<String> tempRuleList = new Vector<>();
        Stack<Character> parenStack = new Stack<>();

        int p = 0;
        int ruleStart = 0;
        int c = '\u0000';
        int lastC = '\u0000';
        int lastOpen = '\u0000';
        boolean haveEquals = false;
        boolean havePipe = false;
        boolean sawVarName = false;
        final String charsThatCantPrecedeAsterisk = "=/{(|}*;\u0000";

        if (description.length() != 0 &&
            description.codePointAt(description.length() - 1) != ';') {
            description = description + ";";
        }

        while (p < description.length()) {
            c = description.codePointAt(p);

            switch (c) {
                case '\\':
                    ++p;
                    break;

                case '{':
                case '<':
                case '[':
                case '(':
                    if (lastOpen == '<') {
                        error("Can't nest brackets inside <>", p, description);
                    }
                    if (lastOpen == '[' && c != '[') {
                        error("Can't nest anything in [] but []", p, description);
                    }

                    if (c == '<' && (haveEquals || havePipe)) {
                        error("Unknown variable name", p, description);
                    }

                    lastOpen = c;
                    parenStack.push(Character.valueOf((char)c));
                    if (c == '<') {
                        sawVarName = true;
                    }
                    break;

                case '}':
                case '>':
                case ']':
                case ')':
                    char expectedClose = '\u0000';
                    switch (lastOpen) {
                        case '{':
                            expectedClose = '}';
                            break;
                        case '[':
                            expectedClose = ']';
                            break;
                        case '(':
                            expectedClose = ')';
                            break;
                        case '<':
                            expectedClose = '>';
                            break;
                    }
                    if (c != expectedClose) {
                        error("Unbalanced parentheses", p, description);
                    }
                    if (lastC == lastOpen) {
                        error("Parens don't contain anything", p, description);
                    }
                    parenStack.pop();
                    if (!parenStack.empty()) {
                        lastOpen = parenStack.peek().charValue();
                    }
                    else {
                        lastOpen = '\u0000';
                    }

                    break;

                case '*':
                    if (charsThatCantPrecedeAsterisk.indexOf(lastC) != -1) {
                        error("Misplaced asterisk", p, description);
                    }
                    break;

                case '?':
                    if (lastC != '*') {
                        error("Misplaced ?", p, description);
                    }
                    break;

                case '=':
                    if (haveEquals || havePipe) {
                        error("More than one = or / in rule", p, description);
                    }
                    haveEquals = true;
                    break;

                case '/':
                    if (haveEquals || havePipe) {
                        error("More than one = or / in rule", p, description);
                    }
                    if (sawVarName) {
                        error("Unknown variable name", p, description);
                    }
                    havePipe = true;
                    break;

                case '!':
                    if (lastC != ';' && lastC != '\u0000') {
                        error("! can only occur at the beginning of a rule", p, description);
                    }
                    break;

                case '.':
                    break;

                case '^':
                case '-':
                case ':':
                    if (lastOpen != '[' && lastOpen != '<') {
                        error("Illegal character", p, description);
                    }
                    break;

                case ';':
                    if (lastC == ';' || lastC == '\u0000') {
                        error("Empty rule", p, description);
                    }
                    if (!parenStack.empty()) {
                        error("Unbalanced parenheses", p, description);
                    }

                    if (parenStack.empty()) {
                        if (haveEquals) {
                            description = processSubstitution(description.substring(ruleStart,
                                            p), description, p + 1);
                        }
                        else {
                            if (sawVarName) {
                                error("Unknown variable name", p, description);
                            }

                            tempRuleList.addElement(description.substring(ruleStart, p));
                        }

                        ruleStart = p + 1;
                        haveEquals = havePipe = sawVarName = false;
                    }
                    break;

                case '|':
                    if (lastC == '|') {
                        error("Empty alternative", p, description);
                    }
                    if (parenStack.empty() || lastOpen != '(') {
                        error("Misplaced |", p, description);
                    }
                    break;

                default:
                    if (c >= ' ' && c < '\u007f' && !Character.isLetter((char)c)
                        && !Character.isDigit((char)c)) {
                        error("Illegal character", p, description);
                    }
                    if (c >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                        ++p;
                    }
                    break;
            }
            lastC = c;
            ++p;
        }
        if (tempRuleList.size() == 0) {
            error("No valid rules in description", p, description);
        }
        return tempRuleList;
    }

    /**
     * This function performs variable-name substitutions.  First it does syntax
     * checking on the variable-name definition.  If it's syntactically valid, it
     * then goes through the remainder of the description and does a simple
     * find-and-replace of the variable name with its text.  (The variable text
     * must be enclosed in either [] or () for this to work.)
     */
    protected String processSubstitution(String substitutionRule, String description,
                    int startPos) {
        String replace;
        String replaceWith;
        int equalPos = substitutionRule.indexOf('=');
        replace = substitutionRule.substring(0, equalPos);
        replaceWith = substitutionRule.substring(equalPos + 1);

        handleSpecialSubstitution(replace, replaceWith, startPos, description);

        if (replaceWith.length() == 0) {
            error("Nothing on right-hand side of =", startPos, description);
        }
        if (replace.length() == 0) {
            error("Nothing on left-hand side of =", startPos, description);
        }
        if (replace.length() == 2 && replace.charAt(0) != '\\') {
            error("Illegal left-hand side for =", startPos, description);
        }
        if (replace.length() >= 3 && replace.charAt(0) != '<' &&
            replace.codePointBefore(equalPos) != '>') {
            error("Illegal left-hand side for =", startPos, description);
        }
        if (!(replaceWith.charAt(0) == '[' &&
              replaceWith.charAt(replaceWith.length() - 1) == ']') &&
            !(replaceWith.charAt(0) == '(' &&
              replaceWith.charAt(replaceWith.length() - 1) == ')')) {
            error("Illegal right-hand side for =", startPos, description);
        }

        StringBuffer result = new StringBuffer();
        result.append(description.substring(0, startPos));
        int lastPos = startPos;
        int pos = description.indexOf(replace, startPos);
        while (pos != -1) {
            result.append(description.substring(lastPos, pos));
            result.append(replaceWith);
            lastPos = pos + replace.length();
            pos = description.indexOf(replace, lastPos);
        }
        result.append(description.substring(lastPos));
        return result.toString();
    }

    /**
     * This function defines a protocol for handling substitution names that
     * are "special," i.e., that have some property beyond just being
     * substitutions.  At the RuleBasedBreakIterator level, we have one
     * special substitution name, "<ignore>".  Subclasses can override this
     * function to add more.  Any special processing that has to go on beyond
     * that which is done by the normal substitution-processing code is done
     * here.
     */
    protected void handleSpecialSubstitution(String replace, String replaceWith,
                int startPos, String description) {
        if (replace.equals("<ignore>")) {
            if (replaceWith.charAt(0) == '(') {
                error("Ignore group can't be enclosed in (", startPos, description);
            }
            ignoreChars = CharSet.parseString(replaceWith);
        }
    }

    /**
     * This function builds the character category table.  On entry,
     * tempRuleList is a vector of break rules that has had variable names substituted.
     * On exit, the charCategoryTable data member has been initialized to hold the
     * character category table, and tempRuleList's rules have been munged to contain
     * character category numbers everywhere a literal character or a [] expression
     * originally occurred.
     */
    @SuppressWarnings("fallthrough")
    protected void buildCharCategories(Vector<String> tempRuleList) {
        int bracketLevel = 0;
        int p = 0;
        int lineNum = 0;

        expressions = new Hashtable<>();
        while (lineNum < tempRuleList.size()) {
            String line = tempRuleList.elementAt(lineNum);
            p = 0;
            while (p < line.length()) {
                int c = line.codePointAt(p);
                switch (c) {
                    case '{': case '}': case '(': case ')': case '*': case '.':
                    case '/': case '|': case ';': case '?': case '!':
                        break;

                    case '[':
                        int q = p + 1;
                        ++bracketLevel;
                        while (q < line.length() && bracketLevel != 0) {
                            c = line.codePointAt(q);
                            switch (c) {
                            case '\\':
                                q++;
                                break;
                            case '[':
                                ++bracketLevel;
                                break;
                            case ']':
                                --bracketLevel;
                                break;
                            }
                            q = q + Character.charCount(c);
                        }
                        if (expressions.get(line.substring(p, q)) == null) {
                            expressions.put(line.substring(p, q), CharSet.parseString(line.substring(p, q)));
                        }
                        p = q - 1;
                        break;

                    case '\\':
                        ++p;
                        c = line.codePointAt(p);

                    default:
                        expressions.put(line.substring(p, p + 1), CharSet.parseString(line.substring(p, p + 1)));
                        break;
                }
                p += Character.charCount(line.codePointAt(p));
            }
            ++lineNum;
        }
        CharSet.releaseExpressionCache();

        categories = new Vector<>();
        if (ignoreChars != null) {
            categories.addElement(ignoreChars);
        }
        else {
            categories.addElement(new CharSet());
        }
        ignoreChars = null;

        mungeExpressionList(expressions);


        for (Enumeration<Object> iter = expressions.elements(); iter.hasMoreElements(); ) {
            CharSet e = (CharSet)iter.nextElement();

            for (int j = categories.size() - 1; !e.empty() && j > 0; j--) {

                CharSet that = categories.elementAt(j);
                if (!that.intersection(e).empty()) {

                    CharSet temp = that.difference(e);
                    if (!temp.empty()) {
                        categories.addElement(temp);
                    }

                    temp = e.intersection(that);
                    e = e.difference(that);
                    if (!temp.equals(that)) {
                        categories.setElementAt(temp, j);
                    }
                }
            }

            if (!e.empty()) {
                categories.addElement(e);
            }
        }

        CharSet allChars = new CharSet();
        for (int i = 1; i < categories.size(); i++) {
            allChars = allChars.union(categories.elementAt(i));
        }
        CharSet ignoreChars = categories.elementAt(0);
        ignoreChars = ignoreChars.difference(allChars);
        categories.setElementAt(ignoreChars, 0);


        for (Enumeration<String> iter = expressions.keys(); iter.hasMoreElements(); ) {
            String key = iter.nextElement();
            CharSet cs = (CharSet)expressions.get(key);
            StringBuffer cats = new StringBuffer();

            for (int j = 0; j < categories.size(); j++) {

                CharSet temp = cs.intersection(categories.elementAt(j));
                if (!temp.empty()) {

                    cats.append((char)(0x100 + j));
                    if (temp.equals(cs)) {
                        break;
                    }
                }
            }

            expressions.put(key, cats.toString());
        }

        charCategoryTable = new CompactByteArray((byte)0);
        supplementaryCharCategoryTable = new SupplementaryCharacterData((byte)0);

        for (int i = 0; i < categories.size(); i++) {
            CharSet chars = categories.elementAt(i);

            Enumeration<int[]> enum_ = chars.getChars();
            while (enum_.hasMoreElements()) {
                int[] range = enum_.nextElement();

                if (i != 0) {
                    if (range[0] < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                        if (range[1] < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                            charCategoryTable.setElementAt((char)range[0], (char)range[1], (byte)i);
                        } else {
                            charCategoryTable.setElementAt((char)range[0], (char)0xFFFF, (byte)i);
                            supplementaryCharCategoryTable.appendElement(Character.MIN_SUPPLEMENTARY_CODE_POINT, range[1], (byte)i);
                        }
                    } else {
                        supplementaryCharCategoryTable.appendElement(range[0], range[1], (byte)i);
                    }
                }

                else {
                    if (range[0] < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                        if (range[1] < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                            charCategoryTable.setElementAt((char)range[0], (char)range[1], IGNORE);
                        } else {
                            charCategoryTable.setElementAt((char)range[0], (char)0xFFFF, IGNORE);
                            supplementaryCharCategoryTable.appendElement(Character.MIN_SUPPLEMENTARY_CODE_POINT, range[1], IGNORE);
                        }
                    } else {
                        supplementaryCharCategoryTable.appendElement(range[0], range[1], IGNORE);
                    }
                }
            }
        }

        charCategoryTable.compact();

        supplementaryCharCategoryTable.complete();

        numCategories = categories.size();
    }

    protected void mungeExpressionList(Hashtable<String, Object> expressions) {
    }

    /**
     * This is the function that builds the forward state table.  Most of the real
     * work is done in parseRule(), which is called once for each rule in the
     * description.
     */
    private void buildStateTable(Vector<String> tempRuleList) {
        tempStateTable = new Vector<>();
        tempStateTable.addElement(new short[numCategories + 1]);
        tempStateTable.addElement(new short[numCategories + 1]);

        for (int i = 0; i < tempRuleList.size(); i++) {
            String rule = tempRuleList.elementAt(i);
            if (rule.charAt(0) != '!') {
                parseRule(rule, true);
            }
        }

        finishBuildingStateTable(true);
    }

    /**
     * This is where most of the work really happens.  This routine parses a single
     * rule in the rule description, adding and modifying states in the state
     * table according to the new expression.  The state table is kept deterministic
     * throughout the whole operation, although some ugly postprocessing is needed
     * to handle the *? token.
     */
    private void parseRule(String rule, boolean forward) {

        int p = 0;
        int currentState = 1;   
        int lastState = currentState;
        String pendingChars = "";

        decisionPointStack = new Stack<>();
        decisionPointList = new Vector<>();
        loopingStates = new Vector<>();
        statesToBackfill = new Vector<>();

        short[] state;
        boolean sawEarlyBreak = false;

        if (!forward) {
            loopingStates.addElement(Integer.valueOf(1));
        }

        decisionPointList.addElement(Integer.valueOf(currentState)); 
        currentState = tempStateTable.size() - 1;   
        while (p < rule.length()) {
            int c = rule.codePointAt(p);
            clearLoopingStates = false;

            if (c == '['
                || c == '\\'
                || Character.isLetter(c)
                || Character.isDigit(c)
                || c < ' '
                || c == '.'
                || c >= '\u007f') {

                if (c != '.') {
                    int q = p;

                    if (c == '\\') {
                        q = p + 2;
                        ++p;
                    }

                    else if (c == '[') {
                        int bracketLevel = 1;

                        q += Character.charCount(rule.codePointAt(q));
                        while (bracketLevel > 0) {
                            c = rule.codePointAt(q);
                            if (c == '[') {
                                ++bracketLevel;
                            }
                            else if (c == ']') {
                                --bracketLevel;
                            }
                            else if (c == '\\') {
                                c = rule.codePointAt(++q);
                            }
                            q += Character.charCount(c);
                        }
                    }

                    else {
                        q = p + Character.charCount(c);
                    }

                    pendingChars = (String)expressions.get(rule.substring(p, q));

                    p = q - Character.charCount(rule.codePointBefore(q));
                }

                else {
                    int rowNum = decisionPointList.lastElement().intValue();
                    state = tempStateTable.elementAt(rowNum);

                    if (p + 1 < rule.length() && rule.charAt(p + 1) == '*' && state[0] != 0) {
                        decisionPointList.addElement(Integer.valueOf(state[0]));
                        pendingChars = "";
                        ++p;
                    }

                    else {
                        StringBuffer temp = new StringBuffer();
                        for (int i = 0; i < numCategories; i++)
                            temp.append((char)(i + 0x100));
                        pendingChars = temp.toString();
                    }
                }

                if (pendingChars.length() != 0) {

                    if (p + 1 < rule.length() && rule.charAt(p + 1) == '*') {
                        @SuppressWarnings("unchecked")
                        Vector<Integer> clone = (Vector<Integer>)decisionPointList.clone();
                        decisionPointStack.push(clone);
                    }

                    int newState = tempStateTable.size();
                    if (loopingStates.size() != 0) {
                        statesToBackfill.addElement(Integer.valueOf(newState));
                    }
                    state = new short[numCategories + 1];
                    if (sawEarlyBreak) {
                        state[numCategories] = DONT_LOOP_FLAG;
                    }
                    tempStateTable.addElement(state);

                    updateStateTable(decisionPointList, pendingChars, (short)newState);
                    decisionPointList.removeAllElements();

                    lastState = currentState;
                    do {
                        ++currentState;
                        decisionPointList.addElement(Integer.valueOf(currentState));
                    } while (currentState + 1 < tempStateTable.size());
                }
            }

            else if (c == '{') {
                @SuppressWarnings("unchecked")
                Vector<Integer> clone = (Vector<Integer>)decisionPointList.clone();
                decisionPointStack.push(clone);
            }

            else if (c == '}' || c == '*') {
                if (c == '*') {
                    for (int i = lastState + 1; i < tempStateTable.size(); i++) {
                        Vector<Integer> temp = new Vector<>();
                        temp.addElement(Integer.valueOf(i));
                        updateStateTable(temp, pendingChars, (short)(lastState + 1));
                    }
                }

                Vector<Integer> temp = decisionPointStack.pop();
                for (int i = 0; i < decisionPointList.size(); i++)
                    temp.addElement(decisionPointList.elementAt(i));
                decisionPointList = temp;
            }

            else if (c == '?') {
                setLoopingStates(decisionPointList, decisionPointList);
            }

            else if (c == '(') {

                tempStateTable.addElement(new short[numCategories + 1]);

                lastState = currentState;
                ++currentState;

                decisionPointList.insertElementAt(Integer.valueOf(currentState), 0);

                @SuppressWarnings("unchecked")
                Vector<Integer> clone = (Vector<Integer>)decisionPointList.clone();
                decisionPointStack.push(clone);
                decisionPointStack.push(new Vector<Integer>());
            }

            else if (c == '|') {

                Vector<Integer> oneDown = decisionPointStack.pop();
                Vector<Integer> twoDown = decisionPointStack.peek();
                decisionPointStack.push(oneDown);

                for (int i = 0; i < decisionPointList.size(); i++)
                    oneDown.addElement(decisionPointList.elementAt(i));
                @SuppressWarnings("unchecked")
                Vector<Integer> clone = (Vector<Integer>)twoDown.clone();
                decisionPointList = clone;
            }

            else if (c == ')') {

                Vector<Integer> exitPoints = decisionPointStack.pop();
                for (int i = 0; i < decisionPointList.size(); i++)
                    exitPoints.addElement(decisionPointList.elementAt(i));
                decisionPointList = exitPoints;

                if (p + 1 >= rule.length() || rule.charAt(p + 1) != '*') {
                    decisionPointStack.pop();
                }

                else {

                    @SuppressWarnings("unchecked")
                    Vector<Integer> clone = (Vector<Integer>)decisionPointList.clone();
                    exitPoints = clone;

                    Vector<Integer> temp = decisionPointStack.pop();

                    int tempStateNum = temp.firstElement().intValue();
                    short[] tempState = tempStateTable.elementAt(tempStateNum);

                    for (int i = 0; i < decisionPointList.size(); i++)
                        temp.addElement(decisionPointList.elementAt(i));
                    decisionPointList = temp;

                    for (int i = 0; i < tempState.length; i++) {
                        if (tempState[i] > tempStateNum) {
                            updateStateTable(exitPoints,
                                             Character.valueOf((char)(i + 0x100)).toString(),
                                             tempState[i]);
                        }
                    }

                    lastState = currentState;
                    currentState = tempStateTable.size() - 1;
                    ++p;
                }
            }

            else if (c == '/') {
                sawEarlyBreak = true;
                for (int i = 0; i < decisionPointList.size(); i++) {
                    state = tempStateTable.elementAt(decisionPointList.
                                    elementAt(i).intValue());
                    state[numCategories] |= LOOKAHEAD_STATE_FLAG;
                }
            }


            if (clearLoopingStates) {
                setLoopingStates(null, decisionPointList);
            }

            p += Character.charCount(c);
        }

        setLoopingStates(null, decisionPointList);

        for (int i = 0; i < decisionPointList.size(); i++) {
            int rowNum = decisionPointList.elementAt(i).intValue();
            state = tempStateTable.elementAt(rowNum);
            state[numCategories] |= (short) END_STATE_FLAG;
            if (sawEarlyBreak) {
                state[numCategories] |= LOOKAHEAD_STATE_FLAG;
            }
        }
    }


    /**
     * Update entries in the state table, and merge states when necessary to keep
     * the table deterministic.
     * @param rows The list of rows that need updating (the decision point list)
     * @param pendingChars A character category list, encoded in a String.  This is the
     * list of the columns that need updating.
     * @param newValue Update the cells specified above to contain this value
     */
    private void updateStateTable(Vector<Integer> rows,
                                  String pendingChars,
                                  short newValue) {
        short[] newValues = new short[numCategories + 1];
        for (int i = 0; i < pendingChars.length(); i++)
            newValues[(int)(pendingChars.charAt(i)) - 0x100] = newValue;

        for (int i = 0; i < rows.size(); i++) {
            mergeStates(rows.elementAt(i).intValue(), newValues, rows);
        }
    }

    /**
     * The real work of making the state table deterministic happens here.  This function
     * merges a state in the state table (specified by rowNum) with a state that is
     * passed in (newValues).  The basic process is to copy the nonzero cells in newStates
     * into the state in the state table (we'll call that oldValues).  If there's a
     * collision (i.e., if the same cell has a nonzero value in both states, and it's
     * not the SAME value), then we have to reconcile the collision.  We do this by
     * creating a new state, adding it to the end of the state table, and using this
     * function recursively to merge the original two states into a single, combined
     * state.  This process may happen recursively (i.e., each successive level may
     * involve collisions).  To prevent infinite recursion, we keep a log of merge
     * operations.  Any time we're merging two states we've merged before, we can just
     * supply the row number for the result of that merge operation rather than creating
     * a new state just like it.
     * @param rowNum The row number in the state table of the state to be updated
     * @param newValues The state to merge it with.
     * @param rowsBeingUpdated A copy of the list of rows passed to updateStateTable()
     * (itself a copy of the decision point list from parseRule()).  Newly-created
     * states get added to the decision point list if their "parents" were on it.
     */
    private void mergeStates(int rowNum,
                             short[] newValues,
                             Vector<Integer> rowsBeingUpdated) {
        short[] oldValues = tempStateTable.elementAt(rowNum);
        boolean isLoopingState = loopingStates.contains(Integer.valueOf(rowNum));

        for (int i = 0; i < oldValues.length; i++) {

            if (oldValues[i] == newValues[i]) {
                continue;
            }

            else if (isLoopingState && loopingStates.contains(Integer.valueOf(oldValues[i]))) {
                if (newValues[i] != 0) {
                    if (oldValues[i] == 0) {
                        clearLoopingStates = true;
                    }
                    oldValues[i] = newValues[i];
                }
            }

            else if (oldValues[i] == 0) {
                oldValues[i] = newValues[i];
            }

            else if (i == numCategories) {
                oldValues[i] = (short)((newValues[i] & ALL_FLAGS) | oldValues[i]);
            }

            else if (oldValues[i] != 0 && newValues[i] != 0) {

                int combinedRowNum = searchMergeList(oldValues[i], newValues[i]);
                if (combinedRowNum != 0) {
                    oldValues[i] = (short)combinedRowNum;
                }

                else {
                    int oldRowNum = oldValues[i];
                    int newRowNum = newValues[i];
                    combinedRowNum = tempStateTable.size();

                    if (mergeList == null) {
                        mergeList = new Vector<>();
                    }
                    mergeList.addElement(new int[] { oldRowNum, newRowNum, combinedRowNum });

                    short[] newRow = new short[numCategories + 1];
                    short[] oldRow = tempStateTable.elementAt(oldRowNum);
                    System.arraycopy(oldRow, 0, newRow, 0, numCategories + 1);
                    tempStateTable.addElement(newRow);
                    oldValues[i] = (short)combinedRowNum;

                    if ((decisionPointList.contains(Integer.valueOf(oldRowNum))
                            || decisionPointList.contains(Integer.valueOf(newRowNum)))
                        && !decisionPointList.contains(Integer.valueOf(combinedRowNum))
                    ) {
                        decisionPointList.addElement(Integer.valueOf(combinedRowNum));
                    }

                    if ((rowsBeingUpdated.contains(Integer.valueOf(oldRowNum))
                            || rowsBeingUpdated.contains(Integer.valueOf(newRowNum)))
                        && !rowsBeingUpdated.contains(Integer.valueOf(combinedRowNum))
                    ) {
                        decisionPointList.addElement(Integer.valueOf(combinedRowNum));
                    }
                    for (int k = 0; k < decisionPointStack.size(); k++) {
                        Vector<Integer> dpl = decisionPointStack.elementAt(k);
                        if ((dpl.contains(Integer.valueOf(oldRowNum))
                                || dpl.contains(Integer.valueOf(newRowNum)))
                            && !dpl.contains(Integer.valueOf(combinedRowNum))
                        ) {
                            dpl.addElement(Integer.valueOf(combinedRowNum));
                        }
                    }

                    mergeStates(combinedRowNum, tempStateTable.elementAt(
                                    newValues[i]), rowsBeingUpdated);
                }
            }
        }
        return;
    }

    /**
     * The merge list is a list of pairs of rows that have been merged somewhere in
     * the process of building this state table, along with the row number of the
     * row containing the merged state.  This function looks up a pair of row numbers
     * and returns the row number of the row they combine into.  (It returns 0 if
     * this pair of rows isn't in the merge list.)
     */
    private int searchMergeList(int a, int b) {
        if (mergeList == null) {
            return 0;
        }

        else {
            int[] entry;
            for (int i = 0; i < mergeList.size(); i++) {
                entry = mergeList.elementAt(i);

                if ((entry[0] == a && entry[1] == b) || (entry[0] == b && entry[1] == a)) {
                    return entry[2];
                }

                if ((entry[2] == a && (entry[0] == b || entry[1] == b))) {
                    return entry[2];
                }
                if ((entry[2] == b && (entry[0] == a || entry[1] == a))) {
                    return entry[2];
                }
            }
            return 0;
        }
    }

    /**
     * This function is used to update the list of current loooping states (i.e.,
     * states that are controlled by a *? construct).  It backfills values from
     * the looping states into unpopulated cells of the states that are currently
     * marked for backfilling, and then updates the list of looping states to be
     * the new list
     * @param newLoopingStates The list of new looping states
     * @param endStates The list of states to treat as end states (states that
     * can exit the loop).
     */
    private void setLoopingStates(Vector<Integer> newLoopingStates,
                                  Vector<Integer> endStates) {

        if (!loopingStates.isEmpty()) {
            int loopingState = loopingStates.lastElement().intValue();
            int rowNum;

            for (int i = 0; i < endStates.size(); i++) {
                eliminateBackfillStates(endStates.elementAt(i).intValue());
            }

            for (int i = 0; i < statesToBackfill.size(); i++) {
                rowNum = statesToBackfill.elementAt(i).intValue();
                short[] state = tempStateTable.elementAt(rowNum);
                state[numCategories] =
                    (short)((state[numCategories] & ALL_FLAGS) | loopingState);
            }
            statesToBackfill.removeAllElements();
            loopingStates.removeAllElements();
        }

        if (newLoopingStates != null) {
            @SuppressWarnings("unchecked")
            Vector<Integer> clone = (Vector<Integer>)newLoopingStates.clone();
            loopingStates = clone;
        }
    }

    /**
     * This removes "ending states" and states reachable from them from the
     * list of states to backfill.
     * @param The row number of the state to remove from the backfill list
     */
    private void eliminateBackfillStates(int baseState) {

        if (statesToBackfill.contains(Integer.valueOf(baseState))) {

            statesToBackfill.removeElement(Integer.valueOf(baseState));

            short[] state = tempStateTable.elementAt(baseState);
            for (int i = 0; i < numCategories; i++) {
                if (state[i] != 0) {
                    eliminateBackfillStates(state[i]);
                }
            }
        }
    }

    /**
     * This function completes the backfilling process by actually doing the
     * backfilling on the states that are marked for it
     */
    private void backfillLoopingStates() {
        short[] state;
        short[] loopingState = null;
        int loopingStateRowNum = 0;
        int fromState;

        for (int i = 0; i < tempStateTable.size(); i++) {
            state = tempStateTable.elementAt(i);

            fromState = state[numCategories] & ~ALL_FLAGS;
            if (fromState > 0) {

                if (fromState != loopingStateRowNum) {
                    loopingStateRowNum = fromState;
                    loopingState = tempStateTable.elementAt(loopingStateRowNum);
                }

                state[numCategories] &= (short) ALL_FLAGS;

                for (int j = 0; j < state.length; j++) {
                    if (state[j] == 0) {
                        state[j] = loopingState[j];
                    }
                    else if (state[j] == DONT_LOOP_FLAG) {
                        state[j] = 0;
                    }
                }
            }
        }
    }

    /**
     * This function completes the state-table-building process by doing several
     * postprocessing steps and copying everything into its final resting place
     * in the iterator itself
     * @param forward True if we're working on the forward state table
     */
    private void finishBuildingStateTable(boolean forward) {
        backfillLoopingStates();

        int[] rowNumMap = new int[tempStateTable.size()];
        Stack<Integer> rowsToFollow = new Stack<>();
        rowsToFollow.push(Integer.valueOf(1));
        rowNumMap[1] = 1;

        while (rowsToFollow.size() != 0) {
            int rowNum = rowsToFollow.pop().intValue();
            short[] row = tempStateTable.elementAt(rowNum);

            for (int i = 0; i < numCategories; i++) {
                if (row[i] != 0) {
                    if (rowNumMap[row[i]] == 0) {
                        rowNumMap[row[i]] = row[i];
                        rowsToFollow.push(Integer.valueOf(row[i]));
                    }
                }
            }
        }

        boolean madeChange;
        int newRowNum;


        int[] stateClasses = new int[tempStateTable.size()];
        int nextClass = numCategories + 1;
        short[] state1, state2;
        for (int i = 1; i < stateClasses.length; i++) {
            if (rowNumMap[i] == 0) {
                continue;
            }
            state1 = tempStateTable.elementAt(i);
            for (int j = 0; j < numCategories; j++) {
                if (state1[j] != 0) {
                    ++stateClasses[i];
                }
            }
            if (stateClasses[i] == 0) {
                stateClasses[i] = nextClass;
            }
        }
        ++nextClass;

        int currentClass;
        int lastClass;
        boolean split;

        do {
            currentClass = 1;
            lastClass = nextClass;
            while (currentClass < nextClass) {
                split = false;
                state1 = state2 = null;
                for (int i = 0; i < stateClasses.length; i++) {
                    if (stateClasses[i] == currentClass) {
                        if (state1 == null) {
                            state1 = tempStateTable.elementAt(i);
                        }
                        else {
                            state2 = tempStateTable.elementAt(i);
                            for (int j = 0; j < state2.length; j++) {
                                if ((j == numCategories && state1[j] != state2[j] && forward)
                                        || (j != numCategories && stateClasses[state1[j]]
                                        != stateClasses[state2[j]])) {
                                    stateClasses[i] = nextClass;
                                    split = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (split) {
                    ++nextClass;
                }
                ++currentClass;
            }
        } while (lastClass != nextClass);

        int[] representatives = new int[nextClass];
        for (int i = 1; i < stateClasses.length; i++)
            if (representatives[stateClasses[i]] == 0) {
                representatives[stateClasses[i]] = i;
            }
            else {
                rowNumMap[i] = representatives[stateClasses[i]];
            }

        for (int i = 1; i < rowNumMap.length; i++) {
            if (rowNumMap[i] != i) {
                tempStateTable.setElementAt(null, i);
            }
        }

        newRowNum = 1;
        for (int i = 1; i < rowNumMap.length; i++) {
            if (tempStateTable.elementAt(i) != null) {
                rowNumMap[i] = newRowNum++;
            }
        }
        for (int i = 1; i < rowNumMap.length; i++) {
            if (tempStateTable.elementAt(i) == null) {
                rowNumMap[i] = rowNumMap[rowNumMap[i]];
            }
        }


        if (forward) {
            endStates = new boolean[newRowNum];
            lookaheadStates = new boolean[newRowNum];
            stateTable = new short[newRowNum * numCategories];
            int p = 0;
            int p2 = 0;
            for (int i = 0; i < tempStateTable.size(); i++) {
                short[] row = tempStateTable.elementAt(i);
                if (row == null) {
                    continue;
                }
                for (int j = 0; j < numCategories; j++) {
                    stateTable[p] = (short)(rowNumMap[row[j]]);
                    ++p;
                }
                endStates[p2] = ((row[numCategories] & END_STATE_FLAG) != 0);
                lookaheadStates[p2] = ((row[numCategories] & LOOKAHEAD_STATE_FLAG) != 0);
                ++p2;
            }
        }

        else {
            backwardsStateTable = new short[newRowNum * numCategories];
            int p = 0;
            for (int i = 0; i < tempStateTable.size(); i++) {
                short[] row = tempStateTable.elementAt(i);
                if (row == null) {
                    continue;
                }
                for (int j = 0; j < numCategories; j++) {
                    backwardsStateTable[p] = (short)(rowNumMap[row[j]]);
                    ++p;
                }
            }
        }
    }

    /**
     * This function builds the backward state table from the forward state
     * table and any additional rules (identified by the ! on the front)
     * supplied in the description
     */
    private void buildBackwardsStateTable(Vector<String> tempRuleList) {

        tempStateTable = new Vector<>();
        tempStateTable.addElement(new short[numCategories + 1]);
        tempStateTable.addElement(new short[numCategories + 1]);

        for (int i = 0; i < tempRuleList.size(); i++) {
            String rule = tempRuleList.elementAt(i);
            if (rule.charAt(0) == '!') {
                parseRule(rule.substring(1), false);
            }
        }
        backfillLoopingStates();


        int backTableOffset = tempStateTable.size();
        if (backTableOffset > 2) {
            ++backTableOffset;
        }

        for (int i = 0; i < numCategories + 1; i++)
            tempStateTable.addElement(new short[numCategories + 1]);

        short[] state = tempStateTable.elementAt(backTableOffset - 1);
        for (int i = 0; i < numCategories; i++)
            state[i] = (short)(i + backTableOffset);

        int numRows = stateTable.length / numCategories;
        for (int column = 0; column < numCategories; column++) {
            for (int row = 0; row < numRows; row++) {
                int nextRow = lookupState(row, column);
                if (nextRow != 0) {
                    for (int nextColumn = 0; nextColumn < numCategories; nextColumn++) {
                        int cellValue = lookupState(nextRow, nextColumn);
                        if (cellValue != 0) {
                            state = tempStateTable.elementAt(nextColumn +
                                            backTableOffset);
                            state[column] = (short)(column + backTableOffset);
                        }
                    }
                }
            }
        }

        if (backTableOffset > 1) {

            state = tempStateTable.elementAt(1);
            for (int i = backTableOffset - 1; i < tempStateTable.size(); i++) {
                short[] state2 = tempStateTable.elementAt(i);
                for (int j = 0; j < numCategories; j++) {
                    if (state[j] != 0 && state2[j] != 0) {
                        state2[j] = state[j];
                    }
                }
            }

            state = tempStateTable.elementAt(backTableOffset - 1);
            for (int i = 1; i < backTableOffset - 1; i++) {
                short[] state2 = tempStateTable.elementAt(i);
                if ((state2[numCategories] & END_STATE_FLAG) == 0) {
                    for (int j = 0; j < numCategories; j++) {
                        if (state2[j] == 0) {
                            state2[j] = state[j];
                        }
                    }
                }
            }
        }

        finishBuildingStateTable(false);
    }

    /**
     * Given a current state and a character category, looks up the
     * next state to transition to in the state table.
     */
    protected int lookupState(int state, int category) {
        return stateTable[state * numCategories + category];
    }

    /**
     * Throws an IllegalArgumentException representing a syntax error in the rule
     * description.  The exception's message contains some debugging information.
     * @param message A message describing the problem
     * @param position The position in the description where the problem was
     * discovered
     * @param context The string containing the error
     */
    protected void error(String message, int position, String context) {
        throw new IllegalArgumentException("Parse error at position (" + position + "): " + message + "\n" +
                context.substring(0, position) + " -here- " + context.substring(position));
    }

    void makeFile(String filename) {
        writeTables(filename);
    }

    /**
     * Magic number for the BreakIterator data file format.
     */
    private static final byte[] LABEL = {
        (byte)'B', (byte)'I', (byte)'d', (byte)'a', (byte)'t', (byte)'a',
        (byte)'\0'
    };

    /**
     * Version number of the dictionary that was read in.
     */
    private static final byte[] supportedVersion = { (byte)1 };

    /**
     * Header size in byte count
     */
     private static final int HEADER_LENGTH = 36;

    /**
     * Array length of indices for BMP characters
     */
     private static final int BMP_INDICES_LENGTH = 512;

    /**
     * Read datafile. The datafile's format is as follows:
     * <pre>
     *   BreakIteratorData {
     *       u1           magic[7];
     *       u1           version;
     *       u4           totalDataSize;
     *       header_info  header;
     *       body         value;
     *   }
     * </pre>
     * <code>totalDataSize</code> is the summation of the size of
     * <code>header_info</code> and <code>body</code> in byte count.
     * <p>
     * In <code>header</code>, each field except for checksum implies the
     * length of each field. Since <code>BMPdataLength</code> is a fixed-length
     *  data(512 entries), its length isn't included in <code>header</code>.
     * <code>checksum</code> is a CRC32 value of all in <code>body</code>.
     * <pre>
     *   header_info {
     *       u4           stateTableLength;
     *       u4           backwardsStateTableLength;
     *       u4           endStatesLength;
     *       u4           lookaheadStatesLength;
     *       u4           BMPdataLength;
     *       u4           nonBMPdataLength;
     *       u4           additionalDataLength;
     *       u8           checksum;
     *   }
     * </pre>
     * <p>
     *
     * Finally, <code>BMPindices</code> and <code>BMPdata</code> are set to
     * <code>charCategoryTable</code>. <code>nonBMPdata</code> is set to
     * <code>supplementaryCharCategoryTable</code>.
     * <pre>
     *   body {
     *       u2           stateTable[stateTableLength];
     *       u2           backwardsStateTable[backwardsStateTableLength];
     *       u1           endStates[endStatesLength];
     *       u1           lookaheadStates[lookaheadStatesLength];
     *       u2           BMPindices[512];
     *       u1           BMPdata[BMPdataLength];
     *       u4           nonBMPdata[numNonBMPdataLength];
     *       u1           additionalData[additionalDataLength];
     *   }
     * </pre>
     */
    protected void writeTables(String datafile) {
        final String filename;
        final String outputDir;
        String tmpbuf = GenerateBreakIteratorData.getOutputDirectory();

        if (tmpbuf.equals("")) {
            filename = datafile;
            outputDir = "";
        } else {
            char sep = File.separatorChar;
            if (sep == '/') {
                outputDir = tmpbuf;
            } else if (sep == '\\') {
                outputDir = tmpbuf.replaceAll("/", "\\\\");
            } else {
                outputDir = tmpbuf.replaceAll("/", String.valueOf(sep));
            }

            filename = outputDir + sep + datafile;
        }

        try {
            if (!outputDir.equals("")) {
                new File(outputDir).mkdirs();
            }
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));

            byte[] BMPdata = charCategoryTable.getStringArray();
            short[] BMPindices = charCategoryTable.getIndexArray();
            int[] nonBMPdata = supplementaryCharCategoryTable.getArray();

            if (BMPdata.length <= 0) {
                throw new InternalError("Wrong BMP data length(" + BMPdata.length + ")");
            }
            if (BMPindices.length != BMP_INDICES_LENGTH) {
                throw new InternalError("Wrong BMP indices length(" + BMPindices.length + ")");
            }
            if (nonBMPdata.length <= 0) {
                throw new InternalError("Wrong non-BMP data length(" + nonBMPdata.length + ")");
            }

            int len;

            /* Compute checksum */
            CRC32 crc32 = new CRC32();
            len = stateTable.length;
            for (int i = 0; i < len; i++) {
                crc32.update(stateTable[i]);
            }
            len = backwardsStateTable.length;
            for (int i = 0; i < len; i++) {
                crc32.update(backwardsStateTable[i]);
            }
            crc32.update(toByteArray(endStates));
            crc32.update(toByteArray(lookaheadStates));
            for (int i = 0; i < BMP_INDICES_LENGTH; i++) {
                crc32.update(BMPindices[i]);
            }
            crc32.update(BMPdata);
            len = nonBMPdata.length;
            for (int i = 0; i < len; i++) {
                crc32.update(nonBMPdata[i]);
            }
            if (additionalData != null) {
                len = additionalData.length;
                for (int i = 0; i < len; i++) {
                    crc32.update(additionalData[i]);
                }
            }

            /* First, write magic, version, and totalDataSize. */
            len = HEADER_LENGTH +
                  (stateTable.length + backwardsStateTable.length) * 2 +
                  endStates.length + lookaheadStates.length + 1024 +
                  BMPdata.length + nonBMPdata.length * 4 +
                  ((additionalData == null) ? 0 : additionalData.length);
            out.write(LABEL);
            out.write(supportedVersion);
            out.write(toByteArray(len));

            /* Write header_info. */
            out.write(toByteArray(stateTable.length));
            out.write(toByteArray(backwardsStateTable.length));
            out.write(toByteArray(endStates.length));
            out.write(toByteArray(lookaheadStates.length));
            out.write(toByteArray(BMPdata.length));
            out.write(toByteArray(nonBMPdata.length));
            if (additionalData == null) {
                out.write(toByteArray(0));
            } else {
                out.write(toByteArray(additionalData.length));
            }
            out.write(toByteArray(crc32.getValue()));

            /* Write stateTable[numCategories * numRows] */
            len = stateTable.length;
            for (int i = 0; i < len; i++) {
                out.write(toByteArray(stateTable[i]));
            }

            /* Write backwardsStateTable[numCategories * numRows] */
            len = backwardsStateTable.length;
            for (int i = 0; i < len; i++) {
                out.write(toByteArray(backwardsStateTable[i]));
            }

            /* Write endStates[numRows] */
            out.write(toByteArray(endStates));

            /* Write lookaheadStates[numRows] */
            out.write(toByteArray(lookaheadStates));

            for (int i = 0; i < BMP_INDICES_LENGTH; i++) {
                out.write(toByteArray(BMPindices[i]));
            }
            BMPindices = null;
            out.write(BMPdata);
            BMPdata = null;

            /* Write a category table for non-BMP characters. */
            len = nonBMPdata.length;
            for (int i = 0; i < len; i++) {
                out.write(toByteArray(nonBMPdata[i]));
            }
            nonBMPdata = null;

            /* Write additional data */
            if (additionalData != null) {
                out.write(additionalData);
            }

            out.close();
        }
        catch (Exception e) {
            throw new InternalError(e.toString());
        }
    }

    byte[] toByteArray(short val) {
        byte[] buf = new byte[2];
        buf[0] = (byte)((val>>>8) & 0xFF);
        buf[1] = (byte)(val & 0xFF);
        return buf;
    }

    byte[] toByteArray(int val) {
        byte[] buf = new byte[4];
        buf[0] = (byte)((val>>>24) & 0xFF);
        buf[1] = (byte)((val>>>16) & 0xFF);
        buf[2] = (byte)((val>>>8) & 0xFF);
        buf[3] = (byte)(val & 0xFF);
        return buf;
    }

    byte[] toByteArray(long val) {
        byte[] buf = new byte[8];
        buf[0] = (byte)((val>>>56) & 0xff);
        buf[1] = (byte)((val>>>48) & 0xff);
        buf[2] = (byte)((val>>>40) & 0xff);
        buf[3] = (byte)((val>>>32) & 0xff);
        buf[4] = (byte)((val>>>24) & 0xff);
        buf[5] = (byte)((val>>>16) & 0xff);
        buf[6] = (byte)((val>>>8) & 0xff);
        buf[7] = (byte)(val & 0xff);
        return buf;
    }

    byte[] toByteArray(boolean[] data) {
        byte[] buf = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            buf[i] = data[i] ? (byte)1 : (byte)0;
        }
        return buf;
    }

    void setAdditionalData(byte[] data) {
        additionalData = data;
    }
}
