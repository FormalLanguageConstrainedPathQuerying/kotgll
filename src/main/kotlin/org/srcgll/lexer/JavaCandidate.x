package org.srcgll.lexer;

import java_cup.runtime.*;
import java.io.*;

%%

%public
%class Scanner
%type JavaSymbol
%unicode

%{
  StringBuilder string = new StringBuilder();

  private JavaSymbol symbol(JavaToken type) {
    return new JavaSymbol(type, null);
  }

  private JavaSymbol symbol(JavaToken type, Object value) {
    return new JavaSymbol(type, value.toString());
  }

  /**
   * assumes correct representation of a long value for
   * specified radix in scanner buffer from <code>start</code>
   * to <code>end</code>
   */
  private long parseLong(int start, int end, int radix) {
    long result = 0;
    long digit;

    for (int i = start; i < end; i++) {
      digit  = Character.digit(yycharat(i),radix);
      result*= radix;
      result+= digit;
    }

    return result;
  }
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
  "abstract"                     { return symbol(JavaToken.ABSTRACT); }
  "boolean"                      { return symbol(JavaToken.BOOLEAN); }
  "break"                        { return symbol(JavaToken.BREAK); }
  "byte"                         { return symbol(JavaToken.BYTE); }
  "case"                         { return symbol(JavaToken.CASE); }
  "catch"                        { return symbol(JavaToken.CATCH); }
  "char"                         { return symbol(JavaToken.CHAR); }
  "class"                        { return symbol(JavaToken.CLASS); }
  "continue"                     { return symbol(JavaToken.CONTINUE); }
  "do"                           { return symbol(JavaToken.DO); }
  "double"                       { return symbol(JavaToken.DOUBLE); }
  "else"                         { return symbol(JavaToken.ELSE); }
  "extends"                      { return symbol(JavaToken.EXTENDS); }
  "final"                        { return symbol(JavaToken.FINAL); }
  "finally"                      { return symbol(JavaToken.FINALLY); }
  "float"                        { return symbol(JavaToken.FLOAT); }
  "for"                          { return symbol(JavaToken.FOR); }
  "default"                      { return symbol(JavaToken.DEFAULT); }
  "implements"                   { return symbol(JavaToken.IMPLEMENTS); }
  "import"                       { return symbol(JavaToken.IMPORT); }
  "instanceof"                   { return symbol(JavaToken.INSTANCEOF); }
  "int"                          { return symbol(JavaToken.INT); }
  "interface"                    { return symbol(JavaToken.INTERFACE); }
  "long"                         { return symbol(JavaToken.LONG); }
  "native"                       { return symbol(JavaToken.NATIVE); }
  "new"                          { return symbol(JavaToken.NEW); }
  "if"                           { return symbol(JavaToken.IF); }
  "public"                       { return symbol(JavaToken.PUBLIC); }
  "short"                        { return symbol(JavaToken.SHORT); }
  "super"                        { return symbol(JavaToken.SUPER); }
  "switch"                       { return symbol(JavaToken.SWITCH); }
  "synchronized"                 { return symbol(JavaToken.SYNCHRONIZED); }
  "package"                      { return symbol(JavaToken.PACKAGE); }
  "private"                      { return symbol(JavaToken.PRIVATE); }
  "protected"                    { return symbol(JavaToken.PROTECTED); }
  "transient"                    { return symbol(JavaToken.TRANSIENT); }
  "return"                       { return symbol(JavaToken.RETURN); }
  "void"                         { return symbol(JavaToken.VOID); }
  "static"                       { return symbol(JavaToken.STATIC); }
  "while"                        { return symbol(JavaToken.WHILE); }
  "this"                         { return symbol(JavaToken.THIS); }
  "throw"                        { return symbol(JavaToken.THROW); }
  "throws"                       { return symbol(JavaToken.THROWS); }
  "try"                          { return symbol(JavaToken.TRY); }
  "volatile"                     { return symbol(JavaToken.VOLATILE); }
  "strictfp"                     { return symbol(JavaToken.STRICTFP); }

  /* boolean literals */
  "true"                         { return symbol(JavaToken.BOOLEAN_LITERAL, true); }
  "false"                        { return symbol(JavaToken.BOOLEAN_LITERAL, false); }

  /* null literal */
  "null"                         { return symbol(JavaToken.NULL_LITERAL); }


  /* separators */
  "("                            { return symbol(JavaToken.LPAREN); }
  ")"                            { return symbol(JavaToken.RPAREN); }
  "{"                            { return symbol(JavaToken.LBRACE); }
  "}"                            { return symbol(JavaToken.RBRACE); }
  "["                            { return symbol(JavaToken.LBRACK); }
  "]"                            { return symbol(JavaToken.RBRACK); }
  ";"                            { return symbol(JavaToken.SEMICOLON); }
  ","                            { return symbol(JavaToken.COMMA); }
  "."                            { return symbol(JavaToken.DOT); }

  /* operators */
  "="                            { return symbol(JavaToken.EQ); }
  ">"                            { return symbol(JavaToken.GT); }
  "<"                            { return symbol(JavaToken.LT); }
  "!"                            { return symbol(JavaToken.NOT); }
  "~"                            { return symbol(JavaToken.COMP); }
  "?"                            { return symbol(JavaToken.QUESTION); }
  ":"                            { return symbol(JavaToken.COLON); }
  "=="                           { return symbol(JavaToken.EQEQ); }
  "<="                           { return symbol(JavaToken.LTEQ); }
  ">="                           { return symbol(JavaToken.GTEQ); }
  "!="                           { return symbol(JavaToken.NOTEQ); }
  "&&"                           { return symbol(JavaToken.ANDAND); }
  "||"                           { return symbol(JavaToken.OROR); }
  "++"                           { return symbol(JavaToken.PLUSPLUS); }
  "--"                           { return symbol(JavaToken.MINUSMINUS); }
  "+"                            { return symbol(JavaToken.PLUS); }
  "-"                            { return symbol(JavaToken.MINUS); }
  "*"                            { return symbol(JavaToken.MULT); }
  "/"                            { return symbol(JavaToken.DIV); }
  "&"                            { return symbol(JavaToken.AND); }
  "|"                            { return symbol(JavaToken.OR); }
  "^"                            { return symbol(JavaToken.XOR); }
  "%"                            { return symbol(JavaToken.MOD); }
  "<<"                           { return symbol(JavaToken.LSHIFT); }
  ">>"                           { return symbol(JavaToken.RSHIFT); }
  ">>>"                          { return symbol(JavaToken.URSHIFT); }
  "+="                           { return symbol(JavaToken.PLUSEQ); }
  "-="                           { return symbol(JavaToken.MINUSEQ); }
  "*="                           { return symbol(JavaToken.MULTEQ); }
  "/="                           { return symbol(JavaToken.DIVEQ); }
  "&="                           { return symbol(JavaToken.ANDEQ); }
  "|="                           { return symbol(JavaToken.OREQ); }
  "^="                           { return symbol(JavaToken.XOREQ); }
  "%="                           { return symbol(JavaToken.MODEQ); }
  "<<="                          { return symbol(JavaToken.LSHIFTEQ); }
  ">>="                          { return symbol(JavaToken.RSHIFTEQ); }
  ">>>="                         { return symbol(JavaToken.URSHIFTEQ); }
  "@"                            { return symbol(JavaToken.AT); }

  /* string literal */
  \"                             { yybegin(STRING); string.setLength(0); }

  /* character literal */
  \'                             { yybegin(CHARLITERAL); }

  /* numeric literals */

  /* This is matched together with the minus, because the number is too big to
     be represented by a positive integer. */
  "-2147483648"                  { return symbol(JavaToken.INTEGER_LITERAL, Integer.valueOf(Integer.MIN_VALUE)); }

  {DecIntegerLiteral}            { return symbol(JavaToken.INTEGER_LITERAL, Integer.valueOf(yytext())); }
  {DecLongLiteral}               { return symbol(JavaToken.INTEGER_LITERAL, new Long(yytext().substring(0,yylength()-1))); }

  {HexIntegerLiteral}            { return symbol(JavaToken.INTEGER_LITERAL, Integer.valueOf((int) parseLong(2, yylength(), 16))); }
  {HexLongLiteral}               { return symbol(JavaToken.INTEGER_LITERAL, new Long(parseLong(2, yylength()-1, 16))); }

  {OctIntegerLiteral}            { return symbol(JavaToken.INTEGER_LITERAL, Integer.valueOf((int) parseLong(0, yylength(), 8))); }
  {OctLongLiteral}               { return symbol(JavaToken.INTEGER_LITERAL, new Long(parseLong(0, yylength()-1, 8))); }

  {FloatLiteral}                 { return symbol(JavaToken.FLOATING_POINT_LITERAL, new Float(yytext().substring(0,yylength()-1))); }
  {DoubleLiteral}                { return symbol(JavaToken.FLOATING_POINT_LITERAL, new Double(yytext())); }
  {DoubleLiteral}[dD]            { return symbol(JavaToken.FLOATING_POINT_LITERAL, new Double(yytext().substring(0,yylength()-1))); }

  /* comments */
  {Comment}                      { /* ignore */ }

  /* whitespace */
  {WhiteSpace}                   { /* ignore */ }

  /* identifiers */
  {Identifier}                   { return symbol(JavaToken.IDENTIFIER, yytext()); }
}

<STRING> {
  \"                             { yybegin(YYINITIAL); return symbol(JavaToken.STRING_LITERAL, string.toString()); }

  {StringCharacter}+             { string.append( yytext() ); }

  /* escape sequences */
  "\\b"                          { string.append( '\b' ); }
  "\\t"                          { string.append( '\t' ); }
  "\\n"                          { string.append( '\n' ); }
  "\\f"                          { string.append( '\f' ); }
  "\\r"                          { string.append( '\r' ); }
  "\\\""                         { string.append( '\"' ); }
  "\\'"                          { string.append( '\'' ); }
  "\\\\"                         { string.append( '\\' ); }
  \\[0-3]?{OctDigit}?{OctDigit}  { char val = (char) Integer.parseInt(yytext().substring(1),8);
                        				   string.append( val ); }

  /* error cases */
  \\.                            { throw new RuntimeException("Illegal escape sequence \""+yytext()+"\""); }
  {LineTerminator}               { throw new RuntimeException("Unterminated string at end of line"); }
}

<CHARLITERAL> {
  {SingleCharacter}\'            { yybegin(YYINITIAL); return symbol(JavaToken.CHARACTER_LITERAL, yytext().charAt(0)); }

  /* escape sequences */
  "\\b"\'                        { yybegin(YYINITIAL); return symbol(JavaToken.CHARACTER_LITERAL, '\b');}
  "\\t"\'                        { yybegin(YYINITIAL); return symbol(JavaToken.CHARACTER_LITERAL, '\t');}
  "\\n"\'                        { yybegin(YYINITIAL); return symbol(JavaToken.CHARACTER_LITERAL, '\n');}
  "\\f"\'                        { yybegin(YYINITIAL); return symbol(JavaToken.CHARACTER_LITERAL, '\f');}
  "\\r"\'                        { yybegin(YYINITIAL); return symbol(JavaToken.CHARACTER_LITERAL, '\r');}
  "\\\""\'                       { yybegin(YYINITIAL); return symbol(JavaToken.CHARACTER_LITERAL, '\"');}
  "\\'"\'                        { yybegin(YYINITIAL); return symbol(JavaToken.CHARACTER_LITERAL, '\'');}
  "\\\\"\'                       { yybegin(YYINITIAL); return symbol(JavaToken.CHARACTER_LITERAL, '\\'); }
  \\[0-3]?{OctDigit}?{OctDigit}\' { yybegin(YYINITIAL);
			                              int val = Integer.parseInt(yytext().substring(1,yylength()-1),8);
			                            return symbol(JavaToken.CHARACTER_LITERAL, (char)val); }

  /* error cases */
  \\.                            { throw new RuntimeException("Illegal escape sequence \""+yytext()+"\""); }
  {LineTerminator}               { throw new RuntimeException("Unterminated character literal at end of line"); }
}

/* error fallback */
[^]                              { throw new RuntimeException("Illegal character \""+yytext()+
                                                              "\" at line "+yyline+", column "+yycolumn); }
<<EOF>>                          { return symbol(JavaToken.EOF); }