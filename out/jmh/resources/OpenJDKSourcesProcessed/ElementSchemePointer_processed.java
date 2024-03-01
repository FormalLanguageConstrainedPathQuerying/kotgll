/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sun.org.apache.xerces.internal.xpointer;

import java.util.HashMap;

import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;

/**
 * <p>
 * Implements the XPointerPart interface for element() scheme specific processing.
 * </p>
 *
 * @xerces.internal
 *
 */
final class ElementSchemePointer implements XPointerPart {


    private String fSchemeName;

    private String fSchemeData;

    private String fShortHandPointerName;

    private boolean fIsResolveElement = false;

    private boolean fIsElementFound = false;

    private boolean fWasOnlyEmptyElementFound = false;

    boolean fIsShortHand = false;

    int fFoundDepth = 0;

    private int fChildSequence[];

    private int fCurrentChildPosition = 1;

    private int fCurrentChildDepth = 0;

    private int fCurrentChildSequence[];;

    private boolean fIsFragmentResolved = false;

    private ShortHandPointer fShortHandPointer;

    protected XMLErrorReporter fErrorReporter;

    protected XMLErrorHandler fErrorHandler;

    private SymbolTable fSymbolTable;

    public ElementSchemePointer() {
    }

    public ElementSchemePointer(SymbolTable symbolTable) {
        fSymbolTable = symbolTable;
    }

    public ElementSchemePointer(SymbolTable symbolTable,
            XMLErrorReporter errorReporter) {
        fSymbolTable = symbolTable;
        fErrorReporter = errorReporter;
    }


    /**
     * Parses the XPointer expression and tokenizes it into Strings
     * delimited by whitespace.
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerProcessor#parseXPointer(java.lang.String)
     */
    public void parseXPointer(String xpointer) throws XNIException {

        init();

        final Tokens tokens = new Tokens(fSymbolTable);

        Scanner scanner = new Scanner(fSymbolTable) {
            protected void addToken(Tokens tokens, int token)
                    throws XNIException {
                if (token == Tokens.XPTRTOKEN_ELEM_CHILD
                        || token == Tokens.XPTRTOKEN_ELEM_NCNAME) {
                    super.addToken(tokens, token);
                    return;
                }
                reportError("InvalidElementSchemeToken", new Object[] { tokens
                        .getTokenString(token) });
            }
        };

        int length = xpointer.length();
        boolean success = scanner.scanExpr(fSymbolTable, tokens, xpointer, 0,
                length);

        if (!success) {
            reportError("InvalidElementSchemeXPointer",
                    new Object[] { xpointer });
        }

        int tmpChildSequence[] = new int[tokens.getTokenCount() / 2 + 1];

        int i = 0;

        while (tokens.hasMore()) {
            int token = tokens.nextToken();

            switch (token) {
            case Tokens.XPTRTOKEN_ELEM_NCNAME: {

                token = tokens.nextToken();
                fShortHandPointerName = tokens.getTokenString(token);

                fShortHandPointer = new ShortHandPointer(fSymbolTable);
                fShortHandPointer.setSchemeName(fShortHandPointerName);

                break;
            }
            case Tokens.XPTRTOKEN_ELEM_CHILD: {
                tmpChildSequence[i] = tokens.nextToken();
                i++;

                break;
            }
            default:
                reportError("InvalidElementSchemeXPointer",
                        new Object[] { xpointer });
            }
        }

        fChildSequence = new int[i];
        fCurrentChildSequence = new int[i];
        System.arraycopy(tmpChildSequence, 0, fChildSequence, 0, i);

    }

    /**
     * Returns the scheme name i.e element
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#getSchemeName()
     */
    public String getSchemeName() {
        return fSchemeName;
    }

    /**
     * Returns the scheme data
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#getSchemeData()
     */
    public String getSchemeData() {
        return fSchemeData;
    }

    /**
     * Sets the scheme name
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#setSchemeName(java.lang.String)
     */
    public void setSchemeName(String schemeName) {
        fSchemeName = schemeName;

    }

    /**
     * Sets the scheme data
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#setSchemeData(java.lang.String)
     */
    public void setSchemeData(String schemeData) {
        fSchemeData = schemeData;
    }

    /**
     * Responsible for resolving the element() scheme XPointer.  If a ShortHand
     * Pointer is present and it is successfully resolved and if a child
     * sequence is present, the child sequence is resolved relative to it.
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerProcessor#resolveXPointer(com.sun.org.apache.xerces.internal.xni.QName, com.sun.org.apache.xerces.internal.xni.XMLAttributes, com.sun.org.apache.xerces.internal.xni.Augmentations, int event)
     */
    public boolean resolveXPointer(QName element, XMLAttributes attributes,
            Augmentations augs, int event) throws XNIException {

        boolean isShortHandPointerResolved = false;


        if (fShortHandPointerName != null) {
            isShortHandPointerResolved = fShortHandPointer.resolveXPointer(
                    element, attributes, augs, event);
            if (isShortHandPointerResolved) {
                fIsResolveElement = true;
                fIsShortHand = true;
            } else {
                fIsResolveElement = false;
            }
        } else {
            fIsResolveElement = true;
        }

        if (fChildSequence.length > 0) {
            fIsFragmentResolved = matchChildSequence(element, event);
        } else if (isShortHandPointerResolved && fChildSequence.length <= 0) {
            fIsFragmentResolved = isShortHandPointerResolved;
        } else {
            fIsFragmentResolved = false;
        }

        return fIsFragmentResolved;
    }

    /**
     * Matches the current element position in the document tree with the
     * element position specified in the element XPointer scheme.
     *
     * @param event
     * @return boolean - true if the current element position in the document
     * tree matches theelement position specified in the element XPointer
     * scheme.
     */
    protected boolean matchChildSequence(QName element, int event)
            throws XNIException {

        if (fCurrentChildDepth >= fCurrentChildSequence.length) {
            int tmpCurrentChildSequence[] = new int[fCurrentChildSequence.length];
            System.arraycopy(fCurrentChildSequence, 0, tmpCurrentChildSequence,
                    0, fCurrentChildSequence.length);

            fCurrentChildSequence = new int[fCurrentChildDepth * 2];
            System.arraycopy(tmpCurrentChildSequence, 0, fCurrentChildSequence,
                    0, tmpCurrentChildSequence.length);
        }

        if (fIsResolveElement) {
            fWasOnlyEmptyElementFound = false;
            if (event == XPointerPart.EVENT_ELEMENT_START) {
                fCurrentChildSequence[fCurrentChildDepth] = fCurrentChildPosition;
                fCurrentChildDepth++;

                fCurrentChildPosition = 1;

                if ((fCurrentChildDepth <= fFoundDepth) || (fFoundDepth == 0)) {
                    if (checkMatch()) {
                        fIsElementFound = true;
                        fFoundDepth = fCurrentChildDepth;
                    } else {
                        fIsElementFound = false;
                        fFoundDepth = 0;
                    }
                }

            } else if (event == XPointerPart.EVENT_ELEMENT_END) {
                if (fCurrentChildDepth == fFoundDepth) {
                    fIsElementFound = true;
                } else if (((fCurrentChildDepth < fFoundDepth) && (fFoundDepth != 0))
                        || ((fCurrentChildDepth > fFoundDepth) 
                        && (fFoundDepth == 0))) {
                    fIsElementFound = false;
                }

                fCurrentChildSequence[fCurrentChildDepth] = 0;

                fCurrentChildDepth--;
                fCurrentChildPosition = fCurrentChildSequence[fCurrentChildDepth] + 1;

            } else if (event == XPointerPart.EVENT_ELEMENT_EMPTY) {

                fCurrentChildSequence[fCurrentChildDepth] = fCurrentChildPosition;
                fCurrentChildPosition++;

                if (checkMatch()) {
                    if (!fIsElementFound) {
                        fWasOnlyEmptyElementFound = true;
                    } else {
                        fWasOnlyEmptyElementFound = false;
                    }
                    fIsElementFound = true;
                } else {
                    fIsElementFound = false;
                    fWasOnlyEmptyElementFound = false;
                }
            }
        }

        return fIsElementFound;
    }

    /**
     * Matches the current position of the element being visited by checking
     * its position and previous elements against the element XPointer expression.
     * If a match is found it return true else false.
     *
     * @return boolean
     */
    protected boolean checkMatch() {
        if (!fIsShortHand) {
            if (fChildSequence.length <= fCurrentChildDepth + 1) {

                for (int i = 0; i < fChildSequence.length; i++) {
                    if (fChildSequence[i] != fCurrentChildSequence[i]) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        } else {
            if (fChildSequence.length <= fCurrentChildDepth + 1) {

                for (int i = 0; i < fChildSequence.length; i++) {
                    if (fCurrentChildSequence.length < i + 2) {
                        return false;
                    }

                    if (fChildSequence[i] != fCurrentChildSequence[i + 1]) {
                        return false;
                    }
                }
            } else {
                return false;
            }

        }

        return true;
    }

    /**
     * Returns true if the node matches or is a child of a matching element()
     * scheme XPointer.
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerProcessor#isFragmentResolved()
     */
    public boolean isFragmentResolved() throws XNIException {
        return fIsFragmentResolved ;
    }

    /**
     * Returns true if the XPointer expression resolves to a non-element child
     * of the current resource fragment.
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#isChildFragmentResolved()
     *
     */
    public boolean isChildFragmentResolved() {
        if (fIsShortHand && fShortHandPointer != null && fChildSequence.length <= 0) {
                return fShortHandPointer.isChildFragmentResolved();
        } else {
                return fWasOnlyEmptyElementFound ? !fWasOnlyEmptyElementFound
                                : (fIsFragmentResolved && (fCurrentChildDepth >= fFoundDepth));
        }
    }

    /**
         * Reports an XPointer error
         */
    protected void reportError(String key, Object[] arguments)
            throws XNIException {
        /*fErrorReporter.reportError(XPointerMessageFormatter.XPOINTER_DOMAIN,
         key, arguments, XMLErrorReporter.SEVERITY_ERROR);
         */
        throw new XNIException((fErrorReporter
                        .getMessageFormatter(XPointerMessageFormatter.XPOINTER_DOMAIN))
                                .formatMessage(fErrorReporter.getLocale(), key, arguments));
    }

    /**
     * Initializes error handling objects
     */
    protected void initErrorReporter() {
        if (fErrorReporter == null) {
            fErrorReporter = new XMLErrorReporter();
        }
        if (fErrorHandler == null) {
            fErrorHandler = new XPointerErrorHandler();
        }
        fErrorReporter.putMessageFormatter(
                XPointerMessageFormatter.XPOINTER_DOMAIN,
                new XPointerMessageFormatter());
    }

    /**
     * Initializes the element scheme processor
     */
    protected void init() {
        fSchemeName = null;
        fSchemeData = null;
        fShortHandPointerName = null;
        fIsResolveElement = false;
        fIsElementFound = false;
        fWasOnlyEmptyElementFound = false;
        fFoundDepth = 0;
        fCurrentChildPosition = 1;
        fCurrentChildDepth = 0;
        fIsFragmentResolved = false;
        fShortHandPointer = null;

        initErrorReporter();
    }


    /**
     * List of XPointer Framework tokens.
     *
     * @xerces.internal
     *
     * @author Neil Delima, IBM
     *
     */
    private final class Tokens {

        /**
         * XPointer element() scheme
         * [1]    ElementSchemeData    ::=    (NCName ChildSequence?) | ChildSequence
         * [2]    ChildSequence    ::=    ('/' [1-9] [0-9]*)+
         */
        private static final int XPTRTOKEN_ELEM_NCNAME = 0;

        private static final int XPTRTOKEN_ELEM_CHILD = 1;

        private final String[] fgTokenNames = { "XPTRTOKEN_ELEM_NCNAME",
                "XPTRTOKEN_ELEM_CHILD" };

        private static final int INITIAL_TOKEN_COUNT = 1 << 8;

        private int[] fTokens = new int[INITIAL_TOKEN_COUNT];

        private int fTokenCount = 0;

        private int fCurrentTokenIndex;

        private SymbolTable fSymbolTable;

        private HashMap<Integer, String> fTokenNames = new HashMap<>();

        /**
         * Constructor
         *
         * @param symbolTable SymbolTable
         */
        private Tokens(SymbolTable symbolTable) {
            fSymbolTable = symbolTable;

            fTokenNames.put(XPTRTOKEN_ELEM_NCNAME,
                    "XPTRTOKEN_ELEM_NCNAME");
            fTokenNames.put(XPTRTOKEN_ELEM_CHILD,
                    "XPTRTOKEN_ELEM_CHILD");
        }

        /*
         * Returns the token String
         * @param token The index of the token
         * @return String The token string
         */
        private String getTokenString(int token) {
            return fTokenNames.get(token);
        }

        /**
         * Add the specified string as a token
         *
         * @param token The token string
         */
        private void addToken(String tokenStr) {
            String str = fTokenNames.get(tokenStr);
            Integer tokenInt = str == null ? null : Integer.parseInt(str);
            if (tokenInt == null) {
                tokenInt = fTokenNames.size();
                fTokenNames.put(tokenInt, tokenStr);
            }
            addToken(tokenInt.intValue());
        }

        /**
         * Add the specified int token
         *
         * @param token The int specifying the token
         */
        private void addToken(int token) {
            try {
                fTokens[fTokenCount] = token;
            } catch (ArrayIndexOutOfBoundsException ex) {
                int[] oldList = fTokens;
                fTokens = new int[fTokenCount << 1];
                System.arraycopy(oldList, 0, fTokens, 0, fTokenCount);
                fTokens[fTokenCount] = token;
            }
            fTokenCount++;
        }

        /**
         * Resets the current position to the head of the token list.
         */
        private void rewind() {
            fCurrentTokenIndex = 0;
        }

        /**
         * Returns true if the {@link #getNextToken()} method
         * returns a valid token.
         */
        private boolean hasMore() {
            return fCurrentTokenIndex < fTokenCount;
        }

        /**
         * Obtains the token at the current position, then advance
         * the current position by one.
         *
         * If there's no such next token, this method throws
         * <tt>new XNIException("InvalidXPointerExpression");</tt>.
         */
        private int nextToken() throws XNIException {
            if (fCurrentTokenIndex == fTokenCount)
                reportError("XPointerElementSchemeProcessingError", null);
            return fTokens[fCurrentTokenIndex++];
        }

        /**
         * Obtains the token at the current position, without advancing
         * the current position.
         *
         * If there's no such next token, this method throws
         * <tt>new XNIException("InvalidXPointerExpression");</tt>.
         */
        private int peekToken() throws XNIException {
            if (fCurrentTokenIndex == fTokenCount)
                reportError("XPointerElementSchemeProcessingError", null);
            return fTokens[fCurrentTokenIndex];
        }

        /**
         * Obtains the token at the current position as a String.
         *
         * If there's no current token or if the current token
         * is not a string token, this method throws
         * If there's no such next token, this method throws
         * <tt>new XNIException("InvalidXPointerExpression");</tt>.
         */
        private String nextTokenAsString() throws XNIException {
            String s = getTokenString(nextToken());
            if (s == null)
                reportError("XPointerElementSchemeProcessingError", null);
            return s;
        }

        /**
         * Returns the number of tokens.
         *
         */
        private int getTokenCount() {
            return fTokenCount;
        }
    }

    /**
     *
     * The XPointer expression scanner.  Scans the XPointer framework expression.
     *
     * @xerces.internal
     *
     */
    private class Scanner {

        /**
         * 7-bit ASCII subset
         *
         *  0   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F
         *  0,  0,  0,  0,  0,  0,  0,  0,  0, HT, LF,  0,  0, CR,  0,  0,  
         *  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  
         * SP,  !,  ",  #,  $,  %,  &,  ',  (,  ),  *,  +,  ,,  -,  .,  /,  
         *  0,  1,  2,  3,  4,  5,  6,  7,  8,  9,  :,  ;,  <,  =,  >,  ?,  
         *  @,  A,  B,  C,  D,  E,  F,  G,  H,  I,  J,  K,  L,  M,  N,  O,  
         *  P,  Q,  R,  S,  T,  U,  V,  W,  X,  Y,  Z,  [,  \,  ],  ^,  _,  
         *  `,  a,  b,  c,  d,  e,  f,  g,  h,  i,  j,  k,  l,  m,  n,  o,  
         *  p,  q,  r,  s,  t,  u,  v,  w,  x,  y,  z,  {,  |,  },  ~, DEL  
         */
        private static final byte CHARTYPE_INVALID = 0, 
                CHARTYPE_OTHER = 1, 
                CHARTYPE_MINUS = 2, 
                CHARTYPE_PERIOD = 3, 
                CHARTYPE_SLASH = 4, 
                CHARTYPE_DIGIT = 5, 
                CHARTYPE_LETTER = 6, 
                CHARTYPE_UNDERSCORE = 7, 
                CHARTYPE_NONASCII = 8; 

        private final byte[] fASCIICharMap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1,
                0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 4, 5, 5, 5, 5, 5,
                5, 5, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 6, 6, 6, 6, 6, 6, 6, 6, 6,
                6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 1, 1, 1, 1,
                7, 1, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
                6, 6, 6, 6, 6, 6, 6, 1, 1, 1, 1, 1 };

        /**
         * Symbol literals
         */

        /** Symbol table. */
        private SymbolTable fSymbolTable;


        /**
         * Constructs an XPath expression scanner.
         *
         * @param symbolTable SymbolTable
         */
        private Scanner(SymbolTable symbolTable) {
            fSymbolTable = symbolTable;

        } 

        /**
         * Scans the XPointer Expression
         *
         */
        private boolean scanExpr(SymbolTable symbolTable, Tokens tokens,
                String data, int currentOffset, int endOffset)
                throws XNIException {

            int ch;
            int nameOffset;
            String nameHandle = null;

            while (true) {
                if (currentOffset == endOffset) {
                    break;
                }

                ch = data.charAt(currentOffset);
                byte chartype = (ch >= 0x80) ? CHARTYPE_NONASCII
                        : fASCIICharMap[ch];


                switch (chartype) {

                case CHARTYPE_SLASH:
                    if (++currentOffset == endOffset) {
                        return false;
                    }

                    addToken(tokens, Tokens.XPTRTOKEN_ELEM_CHILD);
                    ch = data.charAt(currentOffset);

                    int child = 0;
                    while (ch >= '0' && ch <= '9') {
                        child = (child * 10) + (ch - '0');
                        if (++currentOffset == endOffset) {
                            break;
                        }
                        ch = data.charAt(currentOffset);
                    }

                    if (child == 0) {
                        reportError("InvalidChildSequenceCharacter",
                                new Object[] { (char) ch });
                        return false;
                    }

                    tokens.addToken(child);

                    break;

                case CHARTYPE_DIGIT:
                case CHARTYPE_LETTER:
                case CHARTYPE_MINUS:
                case CHARTYPE_NONASCII:
                case CHARTYPE_OTHER:
                case CHARTYPE_PERIOD:
                case CHARTYPE_UNDERSCORE:
                    nameOffset = currentOffset;
                    currentOffset = scanNCName(data, endOffset, currentOffset);

                    if (currentOffset == nameOffset) {
                        reportError("InvalidNCNameInElementSchemeData",
                                new Object[] { data });
                        return false;
                    }

                    if (currentOffset < endOffset) {
                        ch = data.charAt(currentOffset);
                    } else {
                        ch = -1;
                    }

                    nameHandle = symbolTable.addSymbol(data.substring(
                            nameOffset, currentOffset));
                    addToken(tokens, Tokens.XPTRTOKEN_ELEM_NCNAME);
                    tokens.addToken(nameHandle);

                    break;
                }
            }
            return true;
        }

        /**
         * Scans a NCName.
         * From Namespaces in XML
         * [5] NCName ::= (Letter | '_') (NCNameChar)*
         * [6] NCNameChar ::= Letter | Digit | '.' | '-' | '_' | CombiningChar | Extender
         *
         * @param data A String containing the XPointer expression
         * @param endOffset The int XPointer expression length
         * @param currentOffset An int representing the current position of the XPointer expression pointer
         */
        private int scanNCName(String data, int endOffset, int currentOffset) {
            int ch = data.charAt(currentOffset);
            if (ch >= 0x80) {
                if (!XMLChar.isNameStart(ch)) {
                    return currentOffset;
                }
            } else {
                byte chartype = fASCIICharMap[ch];
                if (chartype != CHARTYPE_LETTER
                        && chartype != CHARTYPE_UNDERSCORE) {
                    return currentOffset;
                }
            }
            while (++currentOffset < endOffset) {
                ch = data.charAt(currentOffset);
                if (ch >= 0x80) {
                    if (!XMLChar.isName(ch)) {
                        break;
                    }
                } else {
                    byte chartype = fASCIICharMap[ch];
                    if (chartype != CHARTYPE_LETTER
                            && chartype != CHARTYPE_DIGIT
                            && chartype != CHARTYPE_PERIOD
                            && chartype != CHARTYPE_MINUS
                            && chartype != CHARTYPE_UNDERSCORE) {
                        break;
                    }
                }
            }
            return currentOffset;
        }


        /**
         * This method adds the specified token to the token list. By
         * default, this method allows all tokens. However, subclasses
         * of the XPathExprScanner can override this method in order
         * to disallow certain tokens from being used in the scanned
         * XPath expression. This is a convenient way of allowing only
         * a subset of XPath.
         */
        protected void addToken(Tokens tokens, int token) throws XNIException {
            tokens.addToken(token);
        } 

    } 

}
