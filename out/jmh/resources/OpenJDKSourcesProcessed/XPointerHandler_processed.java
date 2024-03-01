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

import java.util.ArrayList;
import java.util.HashMap;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xinclude.XIncludeHandler;
import com.sun.org.apache.xerces.internal.xinclude.XIncludeNamespaceSupport;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;

/**
 * <p>
 * This is a pipeline component which extends the XIncludeHandler to perform
 * XPointer specific processing specified in the W3C XPointerFramework and
 * element() Scheme Recommendations.
 * </p>
 *
 * <p>
 * This component analyzes each event in the pipeline, looking for an element
 * that matches a PointerPart in the parent XInclude element's xpointer attribute
 * value.  If the match succeeds, all children are passed by this component.
 * </p>
 *
 * <p>
 * See the <a href="http:
 * more information on the XPointer Framework and ShortHand Pointers.
 * See the <a href="http:
 * more information on the XPointer element() Scheme.
 * </p>
 *
 * @xerces.internal
 *
 */
public final class XPointerHandler extends XIncludeHandler implements
        XPointerProcessor {

    protected ArrayList<XPointerPart> fXPointerParts = null;

    protected XPointerPart fXPointerPart = null;

    protected boolean fFoundMatchingPtrPart = false;

    protected XMLErrorReporter fXPointerErrorReporter;

    protected XMLErrorHandler fErrorHandler;

    protected SymbolTable fSymbolTable = null;

    private final String ELEMENT_SCHEME_NAME = "element";

    protected boolean fIsXPointerResolved = false;

    protected boolean fFixupBase = false;
    protected boolean fFixupLang = false;


    /**
     *
     */
    public XPointerHandler() {
        super();

        fXPointerParts = new ArrayList<>();
        fSymbolTable = new SymbolTable();
    }

    public XPointerHandler(SymbolTable symbolTable,
            XMLErrorHandler errorHandler, XMLErrorReporter errorReporter) {
        super();

        fXPointerParts = new ArrayList<>();
        fSymbolTable = symbolTable;
        fErrorHandler = errorHandler;
        fXPointerErrorReporter = errorReporter;
    }

    public void setDocumentHandler(XMLDocumentHandler handler) {
        fDocumentHandler = handler;
    }


    /**
     * Parses the XPointer framework expression and delegates scheme specific parsing.
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerProcessor#parseXPointer(java.lang.String)
     */
    public void parseXPointer(String xpointer) throws XNIException {

        init();

        final Tokens tokens = new Tokens(fSymbolTable);

        Scanner scanner = new Scanner(fSymbolTable) {
            protected void addToken(Tokens tokens, int token)
                    throws XNIException {
                if (token == Tokens.XPTRTOKEN_OPEN_PAREN
                        || token == Tokens.XPTRTOKEN_CLOSE_PAREN
                        || token == Tokens.XPTRTOKEN_SCHEMENAME
                        || token == Tokens.XPTRTOKEN_SCHEMEDATA
                        || token == Tokens.XPTRTOKEN_SHORTHAND) {
                    super.addToken(tokens, token);
                    return;
                }
                reportError("InvalidXPointerToken", new Object[] { tokens
                        .getTokenString(token) });
            }
        };

        int length = xpointer.length();
        boolean success = scanner.scanExpr(fSymbolTable, tokens, xpointer, 0,
                length);

        if (!success)
            reportError("InvalidXPointerExpression", new Object[] { xpointer });

        while (tokens.hasMore()) {
            int token = tokens.nextToken();

            switch (token) {
            case Tokens.XPTRTOKEN_SHORTHAND: {

                token = tokens.nextToken();
                String shortHandPointerName = tokens.getTokenString(token);

                if (shortHandPointerName == null) {
                    reportError("InvalidXPointerExpression",
                            new Object[] { xpointer });
                }

                XPointerPart shortHandPointer = new ShortHandPointer(
                        fSymbolTable);
                shortHandPointer.setSchemeName(shortHandPointerName);
                fXPointerParts.add(shortHandPointer);
                break;
            }
            case Tokens.XPTRTOKEN_SCHEMENAME: {

                token = tokens.nextToken();
                String prefix = tokens.getTokenString(token);
                token = tokens.nextToken();
                String localName = tokens.getTokenString(token);

                String schemeName = prefix + localName;

                int openParenCount = 0;
                int closeParenCount = 0;

                token = tokens.nextToken();
                String openParen = tokens.getTokenString(token);
                if (openParen != "XPTRTOKEN_OPEN_PAREN") {

                    if (token == Tokens.XPTRTOKEN_SHORTHAND) {
                        reportError("MultipleShortHandPointers",
                                new Object[] { xpointer });
                    } else {
                        reportError("InvalidXPointerExpression",
                                new Object[] { xpointer });
                    }
                }
                openParenCount++;

                String schemeData = null;
                while (tokens.hasMore()) {
                    token = tokens.nextToken();
                    schemeData = tokens.getTokenString(token);
                    if (schemeData != "XPTRTOKEN_OPEN_PAREN") {
                        break;
                    }
                    openParenCount++;
                }
                token = tokens.nextToken();
                schemeData = tokens.getTokenString(token);

                token = tokens.nextToken();
                String closeParen = tokens.getTokenString(token);
                if (closeParen != "XPTRTOKEN_CLOSE_PAREN") {
                    reportError("SchemeDataNotFollowedByCloseParenthesis",
                            new Object[] { xpointer });
                }
                closeParenCount++;

                while (tokens.hasMore()) {
                    if (tokens.getTokenString(tokens.peekToken()) != "XPTRTOKEN_OPEN_PAREN") {
                        break;
                    }
                    closeParenCount++;
                }

                if (openParenCount != closeParenCount) {
                    reportError("UnbalancedParenthesisInXPointerExpression",
                            new Object[] { xpointer,
                                    openParenCount,
                                    closeParenCount });
                }

                if (schemeName.equals(ELEMENT_SCHEME_NAME)) {
                    XPointerPart elementSchemePointer = new ElementSchemePointer(
                            fSymbolTable, fErrorReporter);
                    elementSchemePointer.setSchemeName(schemeName);
                    elementSchemePointer.setSchemeData(schemeData);

                    try {
                        elementSchemePointer.parseXPointer(schemeData);
                        fXPointerParts.add(elementSchemePointer);
                    } catch (XNIException e) {
                        throw new XNIException (e);
                    }

                } else {
                    reportWarning("SchemeUnsupported",
                            new Object[] { schemeName });
                }

                break;
            }
            default:
                reportError("InvalidXPointerExpression",
                        new Object[] { xpointer });
            }
        }

    }

    /**
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerProcessor#resolveXPointer(com.sun.org.apache.xerces.internal.xni.QName, com.sun.org.apache.xerces.internal.xni.XMLAttributes, com.sun.org.apache.xerces.internal.xni.Augmentations, int event)
     */
    public boolean resolveXPointer(QName element, XMLAttributes attributes,
            Augmentations augs, int event) throws XNIException {
        boolean resolved = false;

        if (!fFoundMatchingPtrPart) {

            for (int i = 0; i < fXPointerParts.size(); i++) {

                fXPointerPart = fXPointerParts.get(i);

                if (fXPointerPart.resolveXPointer(element, attributes, augs,
                        event)) {
                    fFoundMatchingPtrPart = true;
                    resolved = true;
                }
            }
        } else {
            if (fXPointerPart.resolveXPointer(element, attributes, augs, event)) {
                resolved = true;
            }
        }

        if (!fIsXPointerResolved) {
            fIsXPointerResolved = resolved;
        }

        return resolved;
    }

    /**
     * Returns true if the Node fragment is resolved.
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerProcessor#isFragmentResolved()
     */
    public boolean isFragmentResolved() throws XNIException {
        boolean resolved = (fXPointerPart != null) ? fXPointerPart.isFragmentResolved()
                : false;

        if (!fIsXPointerResolved) {
            fIsXPointerResolved = resolved;
        }

        return resolved;
    }

    /**
     * Returns true if the XPointer expression resolves to a non-element child
     * of the current resource fragment.
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerPart#isChildFragmentResolved()
     *
     */
    public boolean isChildFragmentResolved() throws XNIException {
        boolean resolved = (fXPointerPart != null) ? fXPointerPart
                                .isChildFragmentResolved() : false;
                return resolved;
    }

    /**
     * Returns true if the XPointer successfully found a sub-resource .
     *
     * @see com.sun.org.apache.xerces.internal.xpointer.XPointerProcessor#isFragmentResolved()
     */
    public boolean isXPointerResolved() throws XNIException {
        return fIsXPointerResolved;
    }

    /**
     * Returns the pointer part used to resolve the document fragment.
     *
     * @return String - The pointer part used to resolve the document fragment.
     */
    public XPointerPart getXPointerPart() {
        return fXPointerPart;
    }

    /**
     * Reports XPointer Errors
     *
     */
    private void reportError(String key, Object[] arguments)
            throws XNIException {
        /*
        fXPointerErrorReporter.reportError(
                XPointerMessageFormatter.XPOINTER_DOMAIN, key, arguments,
                XMLErrorReporter.SEVERITY_ERROR);
        */
        throw new XNIException((fErrorReporter
                                .getMessageFormatter(XPointerMessageFormatter.XPOINTER_DOMAIN))
                                .formatMessage(fErrorReporter.getLocale(), key, arguments));
    }

    /**
     * Reports XPointer Warnings
     *
     */
    private void reportWarning(String key, Object[] arguments)
            throws XNIException {
        fXPointerErrorReporter.reportError(
                XPointerMessageFormatter.XPOINTER_DOMAIN, key, arguments,
                XMLErrorReporter.SEVERITY_WARNING);
    }

    /**
     * Initializes error handling objects
     *
     */
    protected void initErrorReporter() {
        if (fXPointerErrorReporter == null) {
            fXPointerErrorReporter = new XMLErrorReporter();
        }
        if (fErrorHandler == null) {
            fErrorHandler = new XPointerErrorHandler();
        }
        /*
         fXPointerErrorReporter.setProperty(Constants.XERCES_PROPERTY_PREFIX
         + Constants.ERROR_HANDLER_PROPERTY, fErrorHandler);
         */
        fXPointerErrorReporter.putMessageFormatter(
                XPointerMessageFormatter.XPOINTER_DOMAIN,
                new XPointerMessageFormatter());
    }

    /**
     * Initializes the XPointer Processor;
     */
    protected void init() {
        fXPointerParts.clear();
        fXPointerPart = null;
        fFoundMatchingPtrPart = false;
        fIsXPointerResolved = false;

        initErrorReporter();
    }

    /**
     * Returns an ArrayList of XPointerPart objects
     *
     * @return An ArrayList of XPointerPart objects.
     */
    public ArrayList<XPointerPart> getPointerParts() {
        return fXPointerParts;
    }

    /**
     * List of XPointer Framework tokens.
     *
     * @xerces.internal
     *
     */
    private final class Tokens {

        /**
         * XPointer Framework tokens
         * [1] Pointer     ::= Shorthand | SchemeBased
         * [2] Shorthand   ::= NCName
         * [3] SchemeBased ::= PointerPart (S? PointerPart)*
         * [4] PointerPart ::= SchemeName '(' SchemeData ')'
         * [5] SchemeName  ::= QName
         * [6] SchemeData  ::= EscapedData*
         * [7] EscapedData ::= NormalChar | '^(' | '^)' | '^^' | '(' SchemeData ')'
         * [8] NormalChar  ::= UnicodeChar - [()^]
         * [9] UnicodeChar ::= [#x0-#x10FFFF]
         *
         */
        private static final int XPTRTOKEN_OPEN_PAREN = 0,
                XPTRTOKEN_CLOSE_PAREN = 1, XPTRTOKEN_SHORTHAND = 2,
                XPTRTOKEN_SCHEMENAME = 3, XPTRTOKEN_SCHEMEDATA = 4;

        private final String[] fgTokenNames = { "XPTRTOKEN_OPEN_PAREN",
                "XPTRTOKEN_CLOSE_PAREN", "XPTRTOKEN_SHORTHAND",
                "XPTRTOKEN_SCHEMENAME", "XPTRTOKEN_SCHEMEDATA" };

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

            fTokenNames.put(XPTRTOKEN_OPEN_PAREN,
                    "XPTRTOKEN_OPEN_PAREN");
            fTokenNames.put(XPTRTOKEN_CLOSE_PAREN,
                    "XPTRTOKEN_CLOSE_PAREN");
            fTokenNames.put(XPTRTOKEN_SHORTHAND,
                    "XPTRTOKEN_SHORTHAND");
            fTokenNames.put(XPTRTOKEN_SCHEMENAME,
                    "XPTRTOKEN_SCHEMENAME");
            fTokenNames.put(XPTRTOKEN_SCHEMEDATA,
                    "XPTRTOKEN_SCHEMEDATA");
        }

        /**
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
         * throws If there's no such next token, this method throws
         * <tt>new XNIException("XPointerProcessingError");</tt>.
         */
        private int nextToken() throws XNIException {
            if (fCurrentTokenIndex == fTokenCount) {
                reportError("XPointerProcessingError", null);
            }
            return fTokens[fCurrentTokenIndex++];
        }

        /**
         * Obtains the token at the current position, without advancing
         * the current position.
         *
         * If there's no such next token, this method throws
         * <tt>new XNIException("XPointerProcessingError");</tt>.
         */
        private int peekToken() throws XNIException {
            if (fCurrentTokenIndex == fTokenCount) {
                reportError("XPointerProcessingError", null);
            }
            return fTokens[fCurrentTokenIndex];
        }

        /**
         * Obtains the token at the current position as a String.
         *
         * If there's no current token or if the current token
         * is not a string token, this method throws
         * If there's no such next token, this method throws
         * <tt>new XNIException("XPointerProcessingError");</tt>.
         */
        private String nextTokenAsString() throws XNIException {
            String tokenStrint = getTokenString(nextToken());
            if (tokenStrint == null) {
                reportError("XPointerProcessingError", null);
            }
            return tokenStrint;
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
                CHARTYPE_WHITESPACE = 2, 
                CHARTYPE_CARRET = 3, 
                CHARTYPE_OPEN_PAREN = 4, 
                CHARTYPE_CLOSE_PAREN = 5, 
                CHARTYPE_MINUS = 6, 
                CHARTYPE_PERIOD = 7, 
                CHARTYPE_SLASH = 8, 
                CHARTYPE_DIGIT = 9, 
                CHARTYPE_COLON = 10, 
                CHARTYPE_EQUAL = 11, 
                CHARTYPE_LETTER = 12, 
                CHARTYPE_UNDERSCORE = 13, 
                CHARTYPE_NONASCII = 14; 

        private final byte[] fASCIICharMap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2,
                0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                2, 1, 1, 1, 1, 1, 1, 1, 4, 5, 1, 1, 1, 6, 7, 8, 9, 9, 9, 9, 9,
                9, 9, 9, 9, 9, 10, 1, 1, 11, 1, 1, 1, 12, 12, 12, 12, 12, 12,
                12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
                12, 12, 12, 12, 1, 1, 1, 3, 13, 1, 12, 12, 12, 12, 12, 12, 12,
                12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
                12, 12, 12, 1, 1, 1, 1, 1 };

        /** Symbol table. */
        private SymbolTable fSymbolTable;

        /**
         * Constructs an XPointer Framework expression scanner.
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
            int openParen = 0;
            int closeParen = 0;
            int nameOffset, dataOffset;
            boolean isQName = false;
            String name = null;
            String prefix = null;
            String schemeData = null;
            StringBuffer schemeDataBuff = new StringBuffer();

            while (true) {

                if (currentOffset == endOffset) {
                    break;
                }
                ch = data.charAt(currentOffset);

                while (ch == ' ' || ch == 0x0A || ch == 0x09 || ch == 0x0D) {
                    if (++currentOffset == endOffset) {
                        break;
                    }
                    ch = data.charAt(currentOffset);
                }
                if (currentOffset == endOffset) {
                    break;
                }

                byte chartype = (ch >= 0x80) ? CHARTYPE_NONASCII
                        : fASCIICharMap[ch];

                switch (chartype) {

                case CHARTYPE_OPEN_PAREN: 
                    addToken(tokens, Tokens.XPTRTOKEN_OPEN_PAREN);
                    openParen++;
                    ++currentOffset;
                    break;

                case CHARTYPE_CLOSE_PAREN: 
                    addToken(tokens, Tokens.XPTRTOKEN_CLOSE_PAREN);
                    closeParen++;
                    ++currentOffset;
                    break;

                case CHARTYPE_CARRET:
                case CHARTYPE_COLON:
                case CHARTYPE_DIGIT:
                case CHARTYPE_EQUAL:
                case CHARTYPE_LETTER:
                case CHARTYPE_MINUS:
                case CHARTYPE_NONASCII:
                case CHARTYPE_OTHER:
                case CHARTYPE_PERIOD:
                case CHARTYPE_SLASH:
                case CHARTYPE_UNDERSCORE:
                case CHARTYPE_WHITESPACE:
                    if (openParen == 0) {
                        nameOffset = currentOffset;
                        currentOffset = scanNCName(data, endOffset,
                                currentOffset);

                        if (currentOffset == nameOffset) {
                            reportError("InvalidShortHandPointer",
                                    new Object[] { data });
                            return false;
                        }

                        if (currentOffset < endOffset) {
                            ch = data.charAt(currentOffset);
                        } else {
                            ch = -1;
                        }

                        name = symbolTable.addSymbol(data.substring(nameOffset,
                                currentOffset));
                        prefix = XMLSymbols.EMPTY_STRING;

                        if (ch == ':') {
                            if (++currentOffset == endOffset) {
                                return false;
                            }

                            ch = data.charAt(currentOffset);
                            prefix = name;
                            nameOffset = currentOffset;
                            currentOffset = scanNCName(data, endOffset,
                                    currentOffset);

                            if (currentOffset == nameOffset) {
                                return false;
                            }

                            if (currentOffset < endOffset) {
                                ch = data.charAt(currentOffset);
                            } else {
                                ch = -1;
                            }

                            isQName = true;
                            name = symbolTable.addSymbol(data.substring(
                                    nameOffset, currentOffset));
                        }

                        if (currentOffset != endOffset) {
                            addToken(tokens, Tokens.XPTRTOKEN_SCHEMENAME);
                            tokens.addToken(prefix);
                            tokens.addToken(name);
                            isQName = false;
                        } else if (currentOffset == endOffset) {
                            addToken(tokens, Tokens.XPTRTOKEN_SHORTHAND);
                            tokens.addToken(name);
                            isQName = false;
                        }

                        closeParen = 0;

                        break;

                    } else if (openParen > 0 && closeParen == 0 && name != null) {
                        dataOffset = currentOffset;
                        currentOffset = scanData(data, schemeDataBuff,
                                endOffset, currentOffset);

                        if (currentOffset == dataOffset) {
                            reportError("InvalidSchemeDataInXPointer",
                                    new Object[] { data });
                            return false;
                        }

                        if (currentOffset < endOffset) {
                            ch = data.charAt(currentOffset);
                        } else {
                            ch = -1;
                        }

                        schemeData = symbolTable.addSymbol(schemeDataBuff
                                .toString());
                        addToken(tokens, Tokens.XPTRTOKEN_SCHEMEDATA);
                        tokens.addToken(schemeData);

                        openParen = 0;
                        schemeDataBuff.delete(0, schemeDataBuff.length());

                    } else {
                        return false;
                    }
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
         * Scans the SchemeData.
         * [6]    SchemeData   ::=    EscapedData*
         * [7]    EscapedData  ::=    NormalChar | '^(' | '^)' | '^^' | '(' SchemeData ')'
         * [8]    NormalChar   ::=    UnicodeChar - [()^]
         * [9]    UnicodeChar  ::=    [#x0-#x10FFFF]
         *
         */
        private int scanData(String data, StringBuffer schemeData,
                int endOffset, int currentOffset) {
            while (true) {

                if (currentOffset == endOffset) {
                    break;
                }

                int ch = data.charAt(currentOffset);
                byte chartype = (ch >= 0x80) ? CHARTYPE_NONASCII
                        : fASCIICharMap[ch];

                if (chartype == CHARTYPE_OPEN_PAREN) {
                    schemeData.append(ch);
                    currentOffset = scanData(data, schemeData, endOffset,
                            ++currentOffset);
                    if (currentOffset == endOffset) {
                        return currentOffset;
                    }

                    ch = data.charAt(currentOffset);
                    chartype = (ch >= 0x80) ? CHARTYPE_NONASCII
                            : fASCIICharMap[ch];

                    if (chartype != CHARTYPE_CLOSE_PAREN) {
                        return endOffset;
                    }
                    schemeData.append((char) ch);
                    ++currentOffset;

                } else if (chartype == CHARTYPE_CLOSE_PAREN) {
                    return currentOffset;

                } else  if (chartype == CHARTYPE_CARRET) {
                    ch = data.charAt(++currentOffset);
                    chartype = (ch >= 0x80) ? CHARTYPE_NONASCII
                            : fASCIICharMap[ch];

                    if (chartype != CHARTYPE_CARRET
                            && chartype != CHARTYPE_OPEN_PAREN
                            && chartype != CHARTYPE_CLOSE_PAREN) {
                        break;
                    }
                    schemeData.append((char) ch);
                    ++currentOffset;

                } else {
                    schemeData.append((char) ch);
                    ++currentOffset;
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

    /**
     * If the comment is a child of a matched element, then pass else return.
     *
     * @param text   The text in the comment.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by application to signal an error.
     */
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        if (!isChildFragmentResolved()) {
            return;
        }
        super.comment(text, augs);
    }

    /**
     * A processing instruction. Processing instructions consist of a
     * target name and, optionally, text data. The data is only meaningful
     * to the application.
     * <p>
     * Typically, a processing instruction's data will contain a series
     * of pseudo-attributes. These pseudo-attributes follow the form of
     * element attributes but are <strong>not</strong> parsed or presented
     * to the application as anything other than text. The application is
     * responsible for parsing the data.
     *
     * @param target The target.
     * @param data   The data or null if none specified.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void processingInstruction(String target, XMLString data,
            Augmentations augs) throws XNIException {
        if (!isChildFragmentResolved()) {
            return;
        }
        super.processingInstruction(target, data, augs);
    }

    /**
     * The start of an element.
     *
     * @param element    The name of the element.
     * @param attributes The element attributes.
     * @param augs       Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void startElement(QName element, XMLAttributes attributes,
            Augmentations augs) throws XNIException {
        if (!resolveXPointer(element, attributes, augs,
                XPointerPart.EVENT_ELEMENT_START)) {

                if (fFixupBase) {
                processXMLBaseAttributes(attributes);
                }
            if (fFixupLang) {
                processXMLLangAttributes(attributes);
            }

            fNamespaceContext.setContextInvalid();

            return;
        }
        super.startElement(element, attributes, augs);
    }

    /**
     * An empty element.
     *
     * @param element    The name of the element.
     * @param attributes The element attributes.
     * @param augs       Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void emptyElement(QName element, XMLAttributes attributes,
            Augmentations augs) throws XNIException {
        if (!resolveXPointer(element, attributes, augs,
                XPointerPart.EVENT_ELEMENT_EMPTY)) {
                if (fFixupBase) {
                processXMLBaseAttributes(attributes);
                }
            if (fFixupLang) {
                processXMLLangAttributes(attributes);
            }

            fNamespaceContext.setContextInvalid();
            return;
        }
        super.emptyElement(element, attributes, augs);
    }

    /**
     * Character content.
     *
     * @param text   The content.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void characters(XMLString text, Augmentations augs)
            throws XNIException {
        if (!isChildFragmentResolved()) {
            return;
        }
        super.characters(text, augs);
    }

    /**
     * Ignorable whitespace. For this method to be called, the document
     * source must have some way of determining that the text containing
     * only whitespace characters should be considered ignorable. For
     * example, the validator can determine if a length of whitespace
     * characters in the document are ignorable based on the element
     * content model.
     *
     * @param text   The ignorable whitespace.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void ignorableWhitespace(XMLString text, Augmentations augs)
            throws XNIException {
        if (!isChildFragmentResolved()) {
            return;
        }
        super.ignorableWhitespace(text, augs);
    }

    /**
     * The end of an element.
     *
     * @param element The name of the element.
     * @param augs    Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void endElement(QName element, Augmentations augs)
            throws XNIException {
        if (!resolveXPointer(element, null, augs,
                XPointerPart.EVENT_ELEMENT_END)) {

            return;
        }
        super.endElement(element, augs);
    }

    /**
     * The start of a CDATA section.
     *
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void startCDATA(Augmentations augs) throws XNIException {
        if (!isChildFragmentResolved()) {
            return;
        }
        super.startCDATA(augs);
    }

    /**
     * The end of a CDATA section.
     *
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void endCDATA(Augmentations augs) throws XNIException {
        if (!isChildFragmentResolved()) {
            return;
        }
        super.endCDATA(augs);
    }

    /**
     * <p>
     * Sets the value of a property. This method is called by the component
     * manager any time after reset when a property changes value.
     * </p>
     * <strong>Note:</strong> Components should silently ignore properties
     * that do not affect the operation of the component.
     *
     * @param propertyId The property identifier.
     * @param value      The value of the property.
     *
     * @throws XMLConfigurationException Thrown for configuration error.
     *                                  In general, components should
     *                                  only throw this exception if
     *                                  it is <strong>really</strong>
     *                                  a critical error.
     */
    public void setProperty(String propertyId, Object value)
            throws XMLConfigurationException {

        if (propertyId == Constants.XERCES_PROPERTY_PREFIX
                + Constants.ERROR_REPORTER_PROPERTY) {
            if (value != null) {
                fXPointerErrorReporter = (XMLErrorReporter) value;
            } else {
                fXPointerErrorReporter = null;
            }
        }

        if (propertyId == Constants.XERCES_PROPERTY_PREFIX
                + Constants.ERROR_HANDLER_PROPERTY) {
            if (value != null) {
                fErrorHandler = (XMLErrorHandler) value;
            } else {
                fErrorHandler = null;
            }
        }

        if (propertyId == Constants.XERCES_FEATURE_PREFIX
                + Constants.XINCLUDE_FIXUP_LANGUAGE_FEATURE) {
            if (value != null) {
                fFixupLang = ((Boolean)value).booleanValue();
            } else {
                fFixupLang = false;
            }
        }

        if (propertyId == Constants.XERCES_FEATURE_PREFIX
                + Constants.XINCLUDE_FIXUP_BASE_URIS_FEATURE) {
            if (value != null) {
                fFixupBase = ((Boolean)value).booleanValue();
            } else {
                fFixupBase = false;
            }
        }

        if (propertyId == Constants.XERCES_PROPERTY_PREFIX
                + Constants.NAMESPACE_CONTEXT_PROPERTY) {
            fNamespaceContext = (XIncludeNamespaceSupport) value;
        }

        super.setProperty(propertyId, value);
    }

}
