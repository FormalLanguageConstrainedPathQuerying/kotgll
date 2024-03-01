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

BooleanLiteral = "false" | "true"
NullLiteral = "null"

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
CommentContent       = ( [^*] | \*+ [^/*] )*

%%

"boolean"      { return JavaToken.BOOLEAN; }
"byte"         { return JavaToken.BYTE; }
"short"        { return JavaToken.SHORT; }
"int"          { return JavaToken.INT; }
"long"         { return JavaToken.LONG; }
"char"         { return JavaToken.CHAR; }
"float"        { return JavaToken.FLOAT; }
"double"       { return JavaToken.DOUBLE; }
"."            { return JavaToken.DOT; }
"["            { return JavaToken.BRACKETLEFT; }
"]"            { return JavaToken.BRACKETRIGHT; }
"("            { return JavaToken.PARENTHLEFT; }
")"            { return JavaToken.PARENTHRIGHT; }
"{"            { return JavaToken.CURLYLEFT; }
"}"            { return JavaToken.CURLYRIGHT; }
"extends"      { return JavaToken.EXTENDS; }
"&"            { return JavaToken.ANDBIT; }
"<"            { return JavaToken.LT; }
">"            { return JavaToken.GT; }
";"            { return JavaToken.SEMICOLON; }
":"            { return JavaToken.COLON; }
"::"           { return JavaToken.DOUBLECOLON; }
"..."          { return JavaToken.ELLIPSIS; }
","            { return JavaToken.COMMA; }
"?"            { return JavaToken.QUESTIONMARK; }
"super"        { return JavaToken.SUPER; }
"package"      { return JavaToken.PACKAGE; }
"import"       { return JavaToken.IMPORT; }
"static"       { return JavaToken.STATIC; }
"*"            { return JavaToken.STAR; }
"+"            { return JavaToken.PLUS; }
"-"            { return JavaToken.MINUS; }
"%"            { return JavaToken.PERCENT; }
"/"            { return JavaToken.SLASH; }
"++"           { return JavaToken.PLUSPLUS; }
"--"           { return JavaToken.MINUSMINUS; }
"~"            { return JavaToken.TILDA; }
"!"            { return JavaToken.EXCLAMATIONMARK; }
"class"        { return JavaToken.CLASS; }
"public"       { return JavaToken.PUBLIC; }
"protected"    { return JavaToken.PROTECTED; }
"private"      { return JavaToken.PRIVATE; }
"abstract"     { return JavaToken.ABSTRACT; }
"final"        { return JavaToken.FINAL; }
"const"        { return JavaToken.FINAL; }
"strictfp"     { return JavaToken.STRICTFP; }
"implements"   { return JavaToken.IMPLEMENTS; }
"transient"    { return JavaToken.TRANSIENT; }
"volatile"     { return JavaToken.VOLATILE; }
"="            { return JavaToken.ASSIGN; }
"*="           { return JavaToken.STARASSIGN; }
"/="           { return JavaToken.SLASHASSIGN; }
"+="           { return JavaToken.PLUSASSIGN; }
"-="           { return JavaToken.MINUSASSIGN; }
"%="           { return JavaToken.PERCENTASSIGN; }
"^="           { return JavaToken.XORASSIGN; }
"<<="          { return JavaToken.SHIFTLEFTASSIGN; }
">>="          { return JavaToken.SHIFTRIGHTASSIGN; }
">>>="         { return JavaToken.USRIGHTSHIFTASSIGN; }
"&="           { return JavaToken.ANDASSIGN; }
"|="           { return JavaToken.ORASSIGN; }
"||"           { return JavaToken.OR; }
"&&"           { return JavaToken.AND; }
"^"            { return JavaToken.XORBIT; }
"=="           { return JavaToken.EQ; }
"!="           { return JavaToken.NOTEQ; }
"<="           { return JavaToken.LESSEQ; }
">="           { return JavaToken.GREATEQ; }
"instanceof"   { return JavaToken.INSTANCEOF; }
"synchronized" { return JavaToken.SYNCHRONIZED; }
"native"       { return JavaToken.NATIVE; }
"void"         { return JavaToken.VOID; }
"this"         { return JavaToken.THIS; }
"throws"       { return JavaToken.THROWS; }
"enum"         { return JavaToken.ENUM; }
"interface"    { return JavaToken.INTERFACE; }
"@"            { return JavaToken.AT; }
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
"|"            { return JavaToken.ORBIT; }
"new"          { return JavaToken.NEW; }
"->"           { return JavaToken.ARROW; }

{LineTerminator}       {}
{Comment}              {}
{WhiteSpace}           {}
{CharacterLiteral}     { return JavaToken.CHARLIT; }
{NullLiteral}          { return JavaToken.NULLLIT; }
{StringLiteral}        { return JavaToken.STRINGLIT; }
{FloatingPointLiteral} { return JavaToken.FLOATINGLIT; }
{BooleanLiteral}       { return JavaToken.BOOLEANLIT; }
{IntegerLiteral}       { return JavaToken.INTEGERLIT; }
{Identifier}           { return JavaToken.ID; }
<<EOF>>                { return JavaToken.EOF; }