package org.ucfs;
/*
 * Copyright (C) 1998-2018  Gerwin Klein <lsf@jflex.de>
 * SPDX-License-Identifier: GPL-2.0-only
 */

/* Java 1.2 language lexer specification */

/* Use together with unicode.flex for Unicode preprocesssing */
/* and java12.cup for a Java 1.2 parser                      */

/* Note that this lexer specification is not tuned for speed.
   It is in fact quite slow on integer and floating point literals,
   because the input is read twice and the methods used to parse
   the numbers are not very fast.
   For a production quality application (e.g. a Java compiler)
   this could be optimized */


%%

%public
%class Scanner

%unicode

%line
%column
%type JavaToken


%{

  /**
   * assumes correct representation of a long value for
   * specified radix in scanner buffer from <code>start</code>
   * to <code>end</code>
   */

%}

/* main character classes */
LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]

WhiteSpace = {LineTerminator} | [ \t\f]

/* comments */
Comment = {TraditionalComment} | {EndOfLineComment} |
          {DocumentationComment}

TraditionalComment = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment = "//" {InputCharacter}* {LineTerminator}?
DocumentationComment = "/*" "*"+ [^/*] ~"*/"

/* identifiers */
Identifier = [:jletter:][:jletterdigit:]*

/* integer literals */
DecIntegerLiteral = 0 | [1-9][0-9]*
DecLongLiteral    = {DecIntegerLiteral} [lL]

HexIntegerLiteral = 0 [xX] 0* {HexDigit} {1,8}
HexLongLiteral    = 0 [xX] 0* {HexDigit} {1,16} [lL]
HexDigit          = [0-9a-fA-F]

OctIntegerLiteral = 0+ [1-3]? {OctDigit} {1,15}
OctLongLiteral    = 0+ 1? {OctDigit} {1,21} [lL]
OctDigit          = [0-7]

/* floating point literals */
FloatLiteral  = ({FLit1}|{FLit2}|{FLit3}) {Exponent}? [fF]
DoubleLiteral = ({FLit1}|{FLit2}|{FLit3}) {Exponent}?

FLit1    = [0-9]+ \. [0-9]*
FLit2    = \. [0-9]+
FLit3    = [0-9]+
Exponent = [eE] [+-]? [0-9]+

/* string and character literals */
StringCharacter = [^\r\n\"\\]
SingleCharacter = [^\r\n\'\\]

%state STRING, CHARLITERAL

%%

<YYINITIAL> {

  /* keywords */
  "abstract"                     { return JavaToken.ABSTRACT; }
  "boolean"                      { return JavaToken.BOOLEAN; }
  "break"                        { return JavaToken.BREAK; }
  "byte"                         { return JavaToken.BYTE; }
  "case"                         { return JavaToken.CASE; }
  "catch"                        { return JavaToken.CATCH; }
  "char"                         { return JavaToken.CHAR; }
  "class"                        { return JavaToken.CLASS; }
  "const"                        { return JavaToken.CONST; }
  "continue"                     { return JavaToken.CONTINUE; }
  "do"                           { return JavaToken.DO; }
  "double"                       { return JavaToken.DOUBLE; }
  "else"                         { return JavaToken.ELSE; }
  "extends"                      { return JavaToken.EXTENDS; }
  "final"                        { return JavaToken.FINAL; }
  "finally"                      { return JavaToken.FINALLY; }
  "float"                        { return JavaToken.FLOAT; }
  "for"                          { return JavaToken.FOR; }
  "default"                      { return JavaToken.DEFAULT; }
  "implements"                   { return JavaToken.IMPLEMENTS; }
  "import"                       { return JavaToken.IMPORT; }
  "instanceof"                   { return JavaToken.INSTANCEOF; }
  "int"                          { return JavaToken.INT; }
  "interface"                    { return JavaToken.INTERFACE; }
  "long"                         { return JavaToken.LONG; }
  "native"                       { return JavaToken.NATIVE; }
  "new"                          { return JavaToken.NEW; }
  "goto"                         { return JavaToken.GOTO; }
  "if"                           { return JavaToken.IF; }
  "public"                       { return JavaToken.PUBLIC; }
  "short"                        { return JavaToken.SHORT; }
  "super"                        { return JavaToken.SUPER; }
  "switch"                       { return JavaToken.SWITCH; }
  "synchronized"                 { return JavaToken.SYNCHRONIZED; }
  "package"                      { return JavaToken.PACKAGE; }
  "private"                      { return JavaToken.PRIVATE; }
  "protected"                    { return JavaToken.PROTECTED; }
  "transient"                    { return JavaToken.TRANSIENT; }
  "return"                       { return JavaToken.RETURN; }
  "void"                         { return JavaToken.VOID; }
  "static"                       { return JavaToken.STATIC; }
  "while"                        { return JavaToken.WHILE; }
  "this"                         { return JavaToken.THIS; }
  "throw"                        { return JavaToken.THROW; }
  "throws"                       { return JavaToken.THROWS; }
  "try"                          { return JavaToken.TRY; }
  "volatile"                     { return JavaToken.VOLATILE; }
  "strictfp"                     { return JavaToken.STRICTFP; }
  "enum"                         { return JavaToken.ENUM; }
  "@"                            { return JavaToken.AT; }

  /* boolean literals */
  "true"                         { return JavaToken.BOOLEAN_LITERAL; }
  "false"                        { return JavaToken.BOOLEAN_LITERAL; }

  /* null literal */
  "null"                         { return JavaToken.NULL_LITERAL; }

  "..."                          { return JavaToken.ELLIPSIS; }


  /* separators */
  "("                            { return JavaToken.LPAREN; }
  ")"                            { return JavaToken.RPAREN; }
  "{"                            { return JavaToken.LBRACE; }
  "}"                            { return JavaToken.RBRACE; }
  "["                            { return JavaToken.LBRACK; }
  "]"                            { return JavaToken.RBRACK; }
  ";"                            { return JavaToken.SEMICOLON; }
  ","                            { return JavaToken.COMMA; }
  "."                            { return JavaToken.DOT; }

  /* operators */
  "="                            { return JavaToken.EQ; }
  ">"                            { return JavaToken.GT; }
  "<"                            { return JavaToken.LT; }
  "!"                            { return JavaToken.NOT; }
  "~"                            { return JavaToken.COMP; }
  "?"                            { return JavaToken.QUESTION; }
  "::"                            { return JavaToken.COLONCOLON; }
  ":"                            { return JavaToken.COLON; }
  "=="                           { return JavaToken.EQEQ; }
  "<="                           { return JavaToken.LTEQ; }
  ">="                           { return JavaToken.GTEQ; }
  "!="                           { return JavaToken.NOTEQ; }
  "&&"                           { return JavaToken.ANDAND; }
  "||"                           { return JavaToken.OROR; }
  "++"                           { return JavaToken.PLUSPLUS; }
  "--"                           { return JavaToken.MINUSMINUS; }
  "+"                            { return JavaToken.PLUS; }
  "-"                            { return JavaToken.MINUS; }
  "*"                            { return JavaToken.MULT; }
  "/"                            { return JavaToken.DIV; }
  "&"                            { return JavaToken.AND; }
  "|"                            { return JavaToken.OR; }
  "^"                            { return JavaToken.XOR; }
  "%"                            { return JavaToken.MOD; }
  "+="                           { return JavaToken.PLUSEQ; }
  "-="                           { return JavaToken.MINUSEQ; }
  "*="                           { return JavaToken.MULTEQ; }
  "/="                           { return JavaToken.DIVEQ; }
  "&="                           { return JavaToken.ANDEQ; }
  "|="                           { return JavaToken.OREQ; }
  "^="                           { return JavaToken.XOREQ; }
  "%="                           { return JavaToken.MODEQ; }
  "<<="                          { return JavaToken.LSHIFTEQ; }
  ">>="                          { return JavaToken.RSHIFTEQ; }
  ">>>="                         { return JavaToken.URSHIFTEQ; }

  /* string literal */
  \"                             { yybegin(STRING); }

  /* character literal */
  \'                             { yybegin(CHARLITERAL); }

  /* numeric literals */

  /* This is matched together with the minus, because the number is too big to
     be represented by a positive integer. */
  "-2147483648"                  { return JavaToken.INTEGER_LITERAL; }

  {DecIntegerLiteral}            { return JavaToken.INTEGER_LITERAL ; }
  {DecLongLiteral}               { return JavaToken.INTEGER_LITERAL ; }

  {HexIntegerLiteral}            { return JavaToken.INTEGER_LITERAL ; }
  {HexLongLiteral}               { return JavaToken.INTEGER_LITERAL; }

  {OctIntegerLiteral}            { return JavaToken.INTEGER_LITERAL; }
  {OctLongLiteral}               { return JavaToken.INTEGER_LITERAL; }

  {FloatLiteral}                 { return JavaToken.FLOATING_POINT_LITERAL; }
  {DoubleLiteral}                { return JavaToken.FLOATING_POINT_LITERAL; }
  {DoubleLiteral}[dD]            { return JavaToken.FLOATING_POINT_LITERAL; }

  /* comments */
  {Comment}                      { /* ignore */ }

  /* whitespace */
  {WhiteSpace}                   { /* ignore */ }

  /* identifiers */
  {Identifier}                   { return JavaToken.IDENTIFIER; }
}

<STRING> {
  \"                             { yybegin(YYINITIAL); return JavaToken.STRING_LITERAL; }

  {StringCharacter}+             {  }

  /* escape sequences */
  "\\b"                          {  }
  "\\t"                          {  }
  "\\n"                          { }
  "\\f"                          { }
  "\\r"                          {  }
  "\\u"                          {  }
  "\\u000C"                          {  }
  "\\\""                         {  }
  "\\'"                          {  }
  "\\\\"                         {  }
  \\[0-3]?{OctDigit}?{OctDigit}  {  }

  /* error cases */
  \\.                            { throw new RuntimeException("Illegal escape sequence \""+yytext()+"\""); }
  {LineTerminator}               { throw new RuntimeException("Unterminated string at end of line"); }
}

<CHARLITERAL> {
  {SingleCharacter}\'            { yybegin(YYINITIAL); return JavaToken.CHARACTER_LITERAL; }

  /* escape sequences */
  "\\b"\'                        { yybegin(YYINITIAL); return JavaToken.CHARACTER_LITERAL;}
  "\\t"\'                        { yybegin(YYINITIAL); return JavaToken.CHARACTER_LITERAL;}
  "\\n"\'                        { yybegin(YYINITIAL); return JavaToken.CHARACTER_LITERAL;}
  "\\f"\'                        { yybegin(YYINITIAL); return JavaToken.CHARACTER_LITERAL;}
  "\\r"\'                        { yybegin(YYINITIAL); return JavaToken.CHARACTER_LITERAL;}
  "\\\""\'                       { yybegin(YYINITIAL); return JavaToken.CHARACTER_LITERAL;}
  "\\'"\'                        { yybegin(YYINITIAL); return JavaToken.CHARACTER_LITERAL;}
  "\\\\"\'                       { yybegin(YYINITIAL); return JavaToken.CHARACTER_LITERAL; }
  \\[0-3]?{OctDigit}?{OctDigit}\' { yybegin(YYINITIAL);
			                            return JavaToken.CHARACTER_LITERAL; }

  /* error cases */
  \\.                            { throw new RuntimeException("Illegal escape sequence \""+yytext()+"\""); }
  {LineTerminator}               { throw new RuntimeException("Unterminated character literal at end of line"); }
}

/* error fallback */
[^]                              { throw new RuntimeException("Illegal character \""+yytext()+
                                                              "\" at line "+yyline+", column "+yycolumn); }
<<EOF>>                          { return JavaToken.EOF; }