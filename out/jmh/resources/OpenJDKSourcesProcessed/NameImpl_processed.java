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

package javax.naming;

import java.util.Locale;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Properties;
import java.util.NoSuchElementException;

/**
  * The implementation class for CompoundName and CompositeName.
  * This class is package private.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @author Aravindan Ranganathan
  * @since 1.3
  */

class NameImpl {
    private static final byte LEFT_TO_RIGHT = 1;
    private static final byte RIGHT_TO_LEFT = 2;
    private static final byte FLAT = 0;

    private Vector<String> components;

    private byte syntaxDirection = LEFT_TO_RIGHT;
    private String syntaxSeparator = "/";
    private String syntaxSeparator2 = null;
    private boolean syntaxCaseInsensitive = false;
    private boolean syntaxTrimBlanks = false;
    private String syntaxEscape = "\\";
    private String syntaxBeginQuote1 = "\"";
    private String syntaxEndQuote1 = "\"";
    private String syntaxBeginQuote2 = "'";
    private String syntaxEndQuote2 = "'";
    private String syntaxAvaSeparator = null;
    private String syntaxTypevalSeparator = null;

    private static final int STYLE_NONE = 0;
    private static final int STYLE_QUOTE1 = 1;
    private static final int STYLE_QUOTE2 = 2;
    private static final int STYLE_ESCAPE = 3;
    private int escapingStyle = STYLE_NONE;

    private final boolean isA(String n, int i, String match) {
        return (match != null && n.startsWith(match, i));
    }

    private final boolean isMeta(String n, int i) {
        return (isA(n, i, syntaxEscape) ||
                isA(n, i, syntaxBeginQuote1) ||
                isA(n, i, syntaxBeginQuote2) ||
                isSeparator(n, i));
    }

    private final boolean isSeparator(String n, int i) {
        return (isA(n, i, syntaxSeparator) ||
                isA(n, i, syntaxSeparator2));
    }

    private final int skipSeparator(String name, int i) {
        if (isA(name, i, syntaxSeparator)) {
            i += syntaxSeparator.length();
        } else if (isA(name, i, syntaxSeparator2)) {
            i += syntaxSeparator2.length();
        }
        return i;
    }

    private final int extractComp(String name, int i, int len, Vector<String> comps)
    throws InvalidNameException {
        String beginQuote;
        String endQuote;
        boolean start = true;
        boolean one = false;
        StringBuilder answer = new StringBuilder(len);

        while (i < len) {
            if (start && ((one = isA(name, i, syntaxBeginQuote1)) ||
                          isA(name, i, syntaxBeginQuote2))) {

                beginQuote = one ? syntaxBeginQuote1 : syntaxBeginQuote2;
                endQuote = one ? syntaxEndQuote1 : syntaxEndQuote2;
                if (escapingStyle == STYLE_NONE) {
                    escapingStyle = one ? STYLE_QUOTE1 : STYLE_QUOTE2;
                }

                for (i += beginQuote.length();
                     ((i < len) && !name.startsWith(endQuote, i));
                     i++) {
                    if (isA(name, i, syntaxEscape) &&
                        isA(name, i + syntaxEscape.length(), endQuote)) {
                        i += syntaxEscape.length();
                    }
                    answer.append(name.charAt(i));  
                }

                if (i >= len)
                    throw
                        new InvalidNameException(name + ": no close quote");

                i += endQuote.length();

                if (i == len || isSeparator(name, i)) {
                    break;
                }
                throw (new InvalidNameException(name +
                    ": close quote appears before end of component"));

            } else if (isSeparator(name, i)) {
                break;

            } else if (isA(name, i, syntaxEscape)) {
                if (isMeta(name, i + syntaxEscape.length())) {
                    i += syntaxEscape.length();
                    if (escapingStyle == STYLE_NONE) {
                        escapingStyle = STYLE_ESCAPE;
                    }
                } else if (i + syntaxEscape.length() >= len) {
                    throw (new InvalidNameException(name +
                        ": unescaped " + syntaxEscape + " at end of component"));
                }
            } else if (isA(name, i, syntaxTypevalSeparator) &&
        ((one = isA(name, i+syntaxTypevalSeparator.length(), syntaxBeginQuote1)) ||
            isA(name, i+syntaxTypevalSeparator.length(), syntaxBeginQuote2))) {
                beginQuote = one ? syntaxBeginQuote1 : syntaxBeginQuote2;
                endQuote = one ? syntaxEndQuote1 : syntaxEndQuote2;

                i += syntaxTypevalSeparator.length();
                answer.append(syntaxTypevalSeparator).append(beginQuote); 

                for (i += beginQuote.length();
                     ((i < len) && !name.startsWith(endQuote, i));
                     i++) {
                    if (isA(name, i, syntaxEscape) &&
                        isA(name, i + syntaxEscape.length(), endQuote)) {
                        i += syntaxEscape.length();
                    }
                    answer.append(name.charAt(i));  
                }

                if (i >= len)
                    throw
                        new InvalidNameException(name + ": typeval no close quote");

                i += endQuote.length();
                answer.append(endQuote); 

                if (i == len || isSeparator(name, i)) {
                    break;
                }
                throw (new InvalidNameException(name.substring(i) +
                    ": typeval close quote appears before end of component"));
            }

            answer.append(name.charAt(i++));
            start = false;
        }

        if (syntaxDirection == RIGHT_TO_LEFT)
            comps.insertElementAt(answer.toString(), 0);
        else
            comps.addElement(answer.toString());
        return i;
    }

    private static boolean getBoolean(Properties p, String name) {
        return toBoolean(p.getProperty(name));
    }

    private static boolean toBoolean(String name) {
        return ((name != null) &&
            name.toLowerCase(Locale.ENGLISH).equals("true"));
    }

    private final void recordNamingConvention(Properties p) {
        String syntaxDirectionStr =
            p.getProperty("jndi.syntax.direction", "flat");
        if (syntaxDirectionStr.equals("left_to_right")) {
            syntaxDirection = LEFT_TO_RIGHT;
        } else if (syntaxDirectionStr.equals("right_to_left")) {
            syntaxDirection = RIGHT_TO_LEFT;
        } else if (syntaxDirectionStr.equals("flat")) {
            syntaxDirection = FLAT;
        } else {
            throw new IllegalArgumentException(syntaxDirectionStr +
                " is not a valid value for the jndi.syntax.direction property");
        }

        if (syntaxDirection != FLAT) {
            syntaxSeparator = p.getProperty("jndi.syntax.separator");
            syntaxSeparator2 = p.getProperty("jndi.syntax.separator2");
            if (syntaxSeparator == null) {
                throw new IllegalArgumentException(
                    "jndi.syntax.separator property required for non-flat syntax");
            }
        } else {
            syntaxSeparator = null;
        }
        syntaxEscape = p.getProperty("jndi.syntax.escape");

        syntaxCaseInsensitive = getBoolean(p, "jndi.syntax.ignorecase");
        syntaxTrimBlanks = getBoolean(p, "jndi.syntax.trimblanks");

        syntaxBeginQuote1 = p.getProperty("jndi.syntax.beginquote");
        syntaxEndQuote1 = p.getProperty("jndi.syntax.endquote");
        if (syntaxEndQuote1 == null && syntaxBeginQuote1 != null)
            syntaxEndQuote1 = syntaxBeginQuote1;
        else if (syntaxBeginQuote1 == null && syntaxEndQuote1 != null)
            syntaxBeginQuote1 = syntaxEndQuote1;
        syntaxBeginQuote2 = p.getProperty("jndi.syntax.beginquote2");
        syntaxEndQuote2 = p.getProperty("jndi.syntax.endquote2");
        if (syntaxEndQuote2 == null && syntaxBeginQuote2 != null)
            syntaxEndQuote2 = syntaxBeginQuote2;
        else if (syntaxBeginQuote2 == null && syntaxEndQuote2 != null)
            syntaxBeginQuote2 = syntaxEndQuote2;

        syntaxAvaSeparator = p.getProperty("jndi.syntax.separator.ava");
        syntaxTypevalSeparator =
            p.getProperty("jndi.syntax.separator.typeval");
    }

    NameImpl(Properties syntax) {
        if (syntax != null) {
            recordNamingConvention(syntax);
        }
        components = new Vector<>();
    }

    NameImpl(Properties syntax, String n) throws InvalidNameException {
        this(syntax);

        boolean rToL = (syntaxDirection == RIGHT_TO_LEFT);
        boolean compsAllEmpty = true;
        int len = n.length();

        for (int i = 0; i < len; ) {
            i = extractComp(n, i, len, components);

            String comp = rToL
                ? components.firstElement()
                : components.lastElement();
            if (comp.length() >= 1) {
                compsAllEmpty = false;
            }

            if (i < len) {
                i = skipSeparator(n, i);
                if ((i == len) && !compsAllEmpty) {
                    if (rToL) {
                        components.insertElementAt("", 0);
                    } else {
                        components.addElement("");
                    }
                }
            }
        }
    }

    NameImpl(Properties syntax, Enumeration<String> comps) {
        this(syntax);

        while (comps.hasMoreElements())
            components.addElement(comps.nextElement());
    }
/*
    private final boolean escapingNeeded(String comp) {
        int len = comp.length();
        for (int i = 0; i < len; i++) {
            if (i == 0) {
                if (isA(comp, 0, syntaxBeginQuote1) ||
                    isA(comp, 0, syntaxBeginQuote2)) {
                    return (true);
                }
            }
            if (isSeparator(comp, i)) {
                return (true);
            }
            if (isA(comp, i, syntaxEscape)) {
                i += syntaxEscape.length();
                if (i >= len || isMeta(comp, i)) {
                    return (true);
                }
            }
        }
        return (false);
    }
*/
    private String stringifyComp(String comp) {
        int len = comp.length();
        boolean escapeSeparator = false, escapeSeparator2 = false;
        String beginQuote = null, endQuote = null;
        StringBuilder strbuf = new StringBuilder(len);

        if (syntaxSeparator != null &&
            comp.contains(syntaxSeparator)) {
            if (syntaxBeginQuote1 != null) {
                beginQuote = syntaxBeginQuote1;
                endQuote = syntaxEndQuote1;
            } else if (syntaxBeginQuote2 != null) {
                beginQuote = syntaxBeginQuote2;
                endQuote = syntaxEndQuote2;
            } else if (syntaxEscape != null)
                escapeSeparator = true;
        }
        if (syntaxSeparator2 != null &&
            comp.contains(syntaxSeparator2)) {
            if (syntaxBeginQuote1 != null) {
                if (beginQuote == null) {
                    beginQuote = syntaxBeginQuote1;
                    endQuote = syntaxEndQuote1;
                }
            } else if (syntaxBeginQuote2 != null) {
                if (beginQuote == null) {
                    beginQuote = syntaxBeginQuote2;
                    endQuote = syntaxEndQuote2;
                }
            } else if (syntaxEscape != null)
                escapeSeparator2 = true;
        }

        if (beginQuote != null) {

            strbuf = strbuf.append(beginQuote);

            for (int i = 0; i < len; ) {
                if (comp.startsWith(endQuote, i)) {
                    strbuf.append(syntaxEscape).append(endQuote);
                    i += endQuote.length();
                } else {
                    strbuf.append(comp.charAt(i++));
                }
            }

            strbuf.append(endQuote);

        } else {


            boolean start = true;
            for (int i = 0; i < len; ) {
                if (start && isA(comp, i, syntaxBeginQuote1)) {
                    strbuf.append(syntaxEscape).append(syntaxBeginQuote1);
                    i += syntaxBeginQuote1.length();
                } else if (start && isA(comp, i, syntaxBeginQuote2)) {
                    strbuf.append(syntaxEscape).append(syntaxBeginQuote2);
                    i += syntaxBeginQuote2.length();
                } else

                if (isA(comp, i, syntaxEscape)) {
                    if (i + syntaxEscape.length() >= len) {
                        strbuf.append(syntaxEscape);
                    } else if (isMeta(comp, i + syntaxEscape.length())) {
                        strbuf.append(syntaxEscape);
                    }
                    strbuf.append(syntaxEscape);
                    i += syntaxEscape.length();
                } else

                if (escapeSeparator && comp.startsWith(syntaxSeparator, i)) {
                    strbuf.append(syntaxEscape).append(syntaxSeparator);
                    i += syntaxSeparator.length();
                } else if (escapeSeparator2 &&
                           comp.startsWith(syntaxSeparator2, i)) {
                    strbuf.append(syntaxEscape).append(syntaxSeparator2);
                    i += syntaxSeparator2.length();
                } else {
                    strbuf.append(comp.charAt(i++));
                }
                start = false;
            }
        }
        return strbuf.toString();
    }

    public String toString() {
        StringBuilder answer = new StringBuilder();
        String comp;
        boolean compsAllEmpty = true;
        int size = components.size();

        for (int i = 0; i < size; i++) {
            if (syntaxDirection == RIGHT_TO_LEFT) {
                comp =
                    stringifyComp(components.elementAt(size - 1 - i));
            } else {
                comp = stringifyComp(components.elementAt(i));
            }
            if ((i != 0) && (syntaxSeparator != null))
                answer.append(syntaxSeparator);
            if (comp.length() >= 1)
                compsAllEmpty = false;
            answer = answer.append(comp);
        }
        if (compsAllEmpty && (size >= 1) && (syntaxSeparator != null))
            answer = answer.append(syntaxSeparator);
        return answer.toString();
    }

    public boolean equals(Object obj) {
        if (obj instanceof NameImpl target) {
            if (target.size() ==  this.size()) {
                Enumeration<String> mycomps = getAll();
                Enumeration<String> comps = target.getAll();
                while (mycomps.hasMoreElements()) {
                    String my = mycomps.nextElement();
                    String his = comps.nextElement();
                    if (syntaxTrimBlanks) {
                        my = my.trim();
                        his = his.trim();
                    }
                    if (syntaxCaseInsensitive) {
                        if (!(my.equalsIgnoreCase(his)))
                            return false;
                    } else {
                        if (!(my.equals(his)))
                            return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
      * Compares obj to this NameImpl to determine ordering.
      * Takes into account syntactic properties such as
      * elimination of blanks, case-ignore, etc, if relevant.
      *
      * Note: using syntax of this NameImpl and ignoring
      * that of comparison target.
      */
    public int compareTo(NameImpl obj) {
        if (this == obj) {
            return 0;
        }

        int len1 = size();
        int len2 = obj.size();
        int n = Math.min(len1, len2);

        int index1 = 0, index2 = 0;

        while (n-- != 0) {
            String comp1 = get(index1++);
            String comp2 = obj.get(index2++);

            if (syntaxTrimBlanks) {
                comp1 = comp1.trim();
                comp2 = comp2.trim();
            }

            int local;
            if (syntaxCaseInsensitive) {
                local = comp1.compareToIgnoreCase(comp2);
            } else {
                local = comp1.compareTo(comp2);
            }

            if (local != 0) {
                return local;
            }
        }

        return len1 - len2;
    }

    public int size() {
        return components.size();
    }

    public Enumeration<String> getAll() {
        return components.elements();
    }

    public String get(int posn) {
        return components.elementAt(posn);
    }

    public Enumeration<String> getPrefix(int posn) {
        if (posn < 0 || posn > size()) {
            throw new ArrayIndexOutOfBoundsException(posn);
        }
        return new NameImplEnumerator(components, 0, posn);
    }

    public Enumeration<String> getSuffix(int posn) {
        int cnt = size();
        if (posn < 0 || posn > cnt) {
            throw new ArrayIndexOutOfBoundsException(posn);
        }
        return new NameImplEnumerator(components, posn, cnt);
    }

    public boolean isEmpty() {
        return (components.isEmpty());
    }

    public boolean startsWith(int posn, Enumeration<String> prefix) {
        if (posn < 0 || posn > size()) {
            return false;
        }
        try {
            Enumeration<String> mycomps = getPrefix(posn);
            while (mycomps.hasMoreElements()) {
                String my = mycomps.nextElement();
                String his = prefix.nextElement();
                if (syntaxTrimBlanks) {
                    my = my.trim();
                    his = his.trim();
                }
                if (syntaxCaseInsensitive) {
                    if (!(my.equalsIgnoreCase(his)))
                        return false;
                } else {
                    if (!(my.equals(his)))
                        return false;
                }
            }
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    public boolean endsWith(int posn, Enumeration<String> suffix) {
        int startIndex = size() - posn;
        if (startIndex < 0 || startIndex > size()) {
            return false;
        }
        try {
            Enumeration<String> mycomps = getSuffix(startIndex);
            while (mycomps.hasMoreElements()) {
                String my = mycomps.nextElement();
                String his = suffix.nextElement();
                if (syntaxTrimBlanks) {
                    my = my.trim();
                    his = his.trim();
                }
                if (syntaxCaseInsensitive) {
                    if (!(my.equalsIgnoreCase(his)))
                        return false;
                } else {
                    if (!(my.equals(his)))
                        return false;
                }
            }
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    public boolean addAll(Enumeration<String> comps) throws InvalidNameException {
        boolean added = false;
        while (comps.hasMoreElements()) {
            try {
                String comp = comps.nextElement();
                if (size() > 0 && syntaxDirection == FLAT) {
                    throw new InvalidNameException(
                        "A flat name can only have a single component");
                }
                components.addElement(comp);
                added = true;
            } catch (NoSuchElementException e) {
                break;  
            }
        }
        return added;
    }

    public boolean addAll(int posn, Enumeration<String> comps)
    throws InvalidNameException {
        boolean added = false;
        for (int i = posn; comps.hasMoreElements(); i++) {
            try {
                String comp = comps.nextElement();
                if (size() > 0 && syntaxDirection == FLAT) {
                    throw new InvalidNameException(
                        "A flat name can only have a single component");
                }
                components.insertElementAt(comp, i);
                added = true;
            } catch (NoSuchElementException e) {
                break;  
            }
        }
        return added;
    }

    public void add(String comp) throws InvalidNameException {
        if (size() > 0 && syntaxDirection == FLAT) {
            throw new InvalidNameException(
                "A flat name can only have a single component");
        }
        components.addElement(comp);
    }

    public void add(int posn, String comp) throws InvalidNameException {
        if (size() > 0 && syntaxDirection == FLAT) {
            throw new InvalidNameException(
                "A flat name can only zero or one component");
        }
        components.insertElementAt(comp, posn);
    }

    public Object remove(int posn) {
        Object r = components.elementAt(posn);
        components.removeElementAt(posn);
        return r;
    }

    public int hashCode() {
        int hash = 0;
        for (Enumeration<String> e = getAll(); e.hasMoreElements();) {
            String comp = e.nextElement();
            if (syntaxTrimBlanks) {
                comp = comp.trim();
            }
            if (syntaxCaseInsensitive) {
                comp = comp.toLowerCase(Locale.ENGLISH);
            }

            hash += comp.hashCode();
        }
        return hash;
    }
}

final
class NameImplEnumerator implements Enumeration<String> {
    Vector<String> vector;
    int count;
    int limit;

    NameImplEnumerator(Vector<String> v, int start, int lim) {
        vector = v;
        count = start;
        limit = lim;
    }

    public boolean hasMoreElements() {
        return count < limit;
    }

    public String nextElement() {
        if (count < limit) {
            return vector.elementAt(count++);
        }
        throw new NoSuchElementException("NameImplEnumerator");
    }
}
