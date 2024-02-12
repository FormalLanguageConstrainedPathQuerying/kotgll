package org.srcgll.lexer;

import java.io.*;

%%

%public
%class JavaLexer
%type  JavaToken
%unicode

Identifier = [:jletter:] [:jletterdigit:]*
IntegerLiteral = {DecimalIntegerLiteral} | {HexIntegerLiteral} | {OctalIntegerLiteral} | {BinaryIntegerLiteral}

DecimalIntegerLiteral = {DecimalNumeral} [lL]?
HexIntegerLiteral = {HexNumeral} [lL]?
OctalIntegerLiteral = {OctalNumeral} [lL]?
BinaryIntegerLiteral = {BinaryNumeral} [lL]?

DecimalNumeral = 0 | [1-9] {Digits}? | [1-9] "_"+ {Digits}
Digits = [0-9] | [0-9] (([0-9] | "_")+)? [0-9]

HexNumeral = "0x" {HexDigits} | "0X" {HexDigits}
HexDigits = [0-9a-fA-F] ((([0-9a-fA-F] | "_")+)? [0-9a-fA-F])?

OctalNumeral = 0 ("_"+)? {OctalDigits}
OctalDigits = [0-7] ((([0-7] | "_")+)? [0-7])?

BinaryNumeral = 0 [bB] {BinaryDigits}
BinaryDigits = [0-1] ((([0-1] | "_")+)? [0-1])?

FloatingPointLiteral = {DecimalFloatingPointLiteral} | {HexadecimalFloatingPointLiteral}
DecimalFloatingPointLiteral = [0-9] "." [0-9]? {ExponentPart}? [fFdD]? | "." [0-9] {ExponentPart}? [fFdD]? | [0-9] {ExponentPart} [fFdD] | [0-9] {ExponentPart}? [fFdD]
ExponentPart = [eE] [\+\-]? {Digits}
HexadecimalFloatingPointLiteral = {HexSignificand} {BinaryExponent} [fFdD]?
HexSignificand = {HexNumeral} "."? | 0 [xX] {HexDigits}? "." {HexDigits}
BinaryExponent = [pP] [\+\-]? {Digits}

InputCharacter = \\ "u"+ [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] | [^\r\n\"\\]

EscapeSequence = \\ [btnfr\"\'\\] | \\ ([0-7] | [0-7] [0-7] | [0-3][0-7][0-7])
LineTerminator = \r | \n | \r\n

CharacterLiteral = \' [^\r\n\'\\] \' | \' {EscapeSequence} \'

StringLiteral = \" {StringCharacter}* \"
StringCharacter = {InputCharacter} | {EscapeSequence}
WhiteSpace = {LineTerminator} | [\ \t\f]

Comment = {TraditionalComment} | {DocumentationComment}
TraditionalComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/"
DocumentationComment = "/**" {CommentContent} "*"+ "/"
CommentContent       = ( [^*] | \*+ [^/*] )

%%

"boolean"      { return JavaToken.BOOLEAN; }
"byte"         { return JavaToken.BYTE; }
"short"        { return JavaToken.SHORT; }
"int"          { return JavaToken.INT; }
"long"         { return JavaToken.LONG; }
"char"         { return JavaToken.CHAR; }
"float"        { return JavaToken.FLOAT; }
"double"       { return JavaToken.DOUBLE; }
"super"        { return JavaToken.SUPER; }
"package"      { return JavaToken.PACKAGE; }
"import"       { return JavaToken.IMPORT; }
"static"       { return JavaToken.STATIC; }
"extends"      { return JavaToken.EXTENDS; }
"class"        { return JavaToken.CLASS; }
"public"       { return JavaToken.PUBLIC; }
"protected"    { return JavaToken.PROTECTED; }
"private"      { return JavaToken.PRIVATE; }
"abstract"     { return JavaToken.ABSTRACT; }
"final"        { return JavaToken.FINAL; }
"strictfp"     { return JavaToken.STRICTFP; }
"implements"   { return JavaToken.IMPLEMENTS; }
"transient"    { return JavaToken.TRANSIENT; }
"volatile"     { return JavaToken.VOLATILE; }
"instanceof"   { return JavaToken.INSTANCEOF; }
"synchronized" { return JavaToken.SYNCHRONIZED; }
"native"       { return JavaToken.NATIVE; }
"void"         { return JavaToken.VOID; }
"this"         { return JavaToken.THIS; }
"throws"       { return JavaToken.THROWS; }
"enum"         { return JavaToken.ENUM; }
"interface"    { return JavaToken.INTERFACE; }
"default"      { return JavaToken.DEFAULT; }
"assert"       { return JavaToken.ASSERT; }
"switch"       { return JavaToken.SWITCH; }
"case"         { return JavaToken.CASE; }
"while"        { return JavaToken.WHILE; }
"for"          { return JavaToken.FOR; }
"if"           { return JavaToken.IF; }
"else"         { return JavaToken.ELSE; }
"do"           { return JavaToken.DO; }
"break"        { return JavaToken.BREAK; }
"continue"     { return JavaToken.CONTINUE; }
"return"       { return JavaToken.RETURN; }
"throw"        { return JavaToken.THROW; }
"try"          { return JavaToken.TRY; }
"catch"        { return JavaToken.CATCH; }
"finally"      { return JavaToken.FINALLY; }
"new"          { return JavaToken.NEW; }

"true"         { return JavaToken.BOOLEAN_LITERAL; }
"false"        { return JavaToken.BOOLEAN_LITERAL; }
"null"         { return JavaToken.NULL_LITERAL; }

"("            { return JavaToken.LPAREN; }
")"            { return JavaToken.RPAREN; }
"{"            { return JavaToken.LBRACE; }
"}"            { return JavaToken.RBRACE; }
"["            { return JavaToken.LBRACK; }
"]"            { return JavaToken.RBRACK; }
";"            { return JavaToken.SEMICOLON; }
","            { return JavaToken.COMMA; }
"."            { return JavaToken.DOT; }
"="            { return JavaToken.EQ; }
">"            { return JavaToken.GT; }
"<"            { return JavaToken.LT; }
"!"            { return JavaToken.NOT; }
"~"            { return JavaToken.COMP; }
"?"            { return JavaToken.QUESTION; }
":"            { return JavaToken.COLON; }
"=="           { return JavaToken.EQEQ; }
"<="           { return JavaToken.LTEQ; }
">="           { return JavaToken.GTEQ; }
"!="           { return JavaToken.NOTEQ; }
"&&"           { return JavaToken.ANDAND; }
"||"           { return JavaToken.OROR; }
"++"           { return JavaToken.PLUSPLUS; }
"--"           { return JavaToken.MINUSMINUS; }
"+"            { return JavaToken.PLUS; }
"-"            { return JavaToken.MINUS; }
"*"            { return JavaToken.MULT; }
"/"            { return JavaToken.DIV; }
"&"            { return JavaToken.AND; }
"|"            { return JavaToken.OR; }
"^"            { return JavaToken.XOR; }
"%"            { return JavaToken.MOD; }
"+="           { return JavaToken.PLUSEQ; }
"-="           { return JavaToken.MINUSEQ; }
"*="           { return JavaToken.MULTEQ; }
"/="           { return JavaToken.DIVEQ; }
"&="           { return JavaToken.ANDEQ; }
"|="           { return JavaToken.OREQ; }
"^="           { return JavaToken.XOREQ; }
"%="           { return JavaToken.MODEQ; }
"<<="          { return JavaToken.LSHIFTEQ; }
">>="          { return JavaToken.RSHIFTEQ; }
">>>="         { return JavaToken.URSHIFTEQ; }
"->"           { return JavaToken.ARROW; }

{LineTerminator}       {}
{Comment}              {}
{WhiteSpace}           {}
{FloatingPointLiteral} { return JavaToken.FLOATING_POINT_LITERAL; }
{IntegerLiteral}       { return JavaToken.INTEGER_LITERAL; }
{StringLiteral}        { return JavaToken.STRING_LITERAL; }
{CharacterLiteral}     { return JavaToken.CHARACTER_LITERAL; }
{Identifier}           { return JavaToken.IDENTIFIER; }
<<EOF>>                { return JavaToken.EOF; }