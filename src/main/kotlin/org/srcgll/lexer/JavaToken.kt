package org.srcgll.lexer

import org.srcgll.rsm.symbol.ITerminal

enum class JavaToken : ITerminal {
    ID, EOF, INTEGERLIT, FLOATINGLIT, BOOLEANLIT, CHARLIT, STRINGLIT, NULLLIT,
    BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE, DOT, BRACKETLEFT, BRACKETRIGHT,
    PARENTHLEFT, PARENTHRIGHT, CURLYLEFT, CURLYRIGHT, EXTENDS, ANDBIT, DIAMONDLEFT, DIAMONDRIGHT,
    DIAMOND, SEMICOLON, COLON, DOUBLECOLON, ELLIPSIS, COMMA, QUESTIONMARK, SUPER, PACKAGE,
    IMPORT, STATIC, STAR, PLUS, MINUS, PERCENT, SLASH, PLUSPLUS, MINUSMINUS, TILDA, EXCLAMATIONMARK,
    CLASS, PUBLIC, PROTECTED, PRIVATE, FINAL, STRICTFP, IMPLEMENTS, TRANSIENT, VOLATILE, ASSIGN,
    STARASSIGN, SLASHASSIGN, PLUSASSIGN, MINUSASSIGN, PERCENTASSIGN, XORASSIGN, SHIFTLEFTASSIGN,
    SHIFTRIGHTASSIGN, USRIGHTSHIFTASSIGN, ANDASSIGN, ORASSIGN, OR, AND, XORBIT, EQ, NOTEQ, LESSEQ,
    GREATEQ, INSTANCEOF, RIGHTSHIT, LEFTSHIFT, USRIGHTSHIFT, SYNCHRONIZED, NATIVE, VOID,
    THIS, THROWS, ENUM, INTERFACE, ABSTRACT, AT, DEFAULT, ASSERT, SWITCH, CASE, WHILE, FOR, IF,
    ELSE, DO, BREAK, CONTINUE, RETURN, THROW, TRY, CATCH, FINALLY, ORBIT, NEW, ARROW
}