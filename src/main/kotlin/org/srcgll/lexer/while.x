package org.srcgll.lexer;

import java.io.*;
import org.srcgll.lexer.SymbolCode;

%%

%public
%class GeneratedLexer
%type SymbolCode
%unicode

Space      = \ | \t | \n | \r | \r\n
Int        = [:digit:]+
Bool       = "true" | "false"
Id         = [:letter:]+
TextLimit  = "\""

%%

"if"        { return SymbolCode.IF; }
":="        { return SymbolCode.ASSIGN; }
"then"      { return SymbolCode.THEN; }
"else"      { return SymbolCode.ELSE; }
"skip"      { return SymbolCode.SKIP; }
"while"     { return SymbolCode.WHILE; }
"print"     { return SymbolCode.PRINT; }
"read"      { return SymbolCode.READ; }
"do"        { return SymbolCode.DO; }
"*"         { return SymbolCode.MULTIPLY; }
"/"         { return SymbolCode.DIVIDE; }
"+"         { return SymbolCode.PLUS; }
"-"         { return SymbolCode.MINUS; }
"not"       { return SymbolCode.NOT; }
"and"       { return SymbolCode.AND; }
"or"        { return SymbolCode.OR; }
"("         { return SymbolCode.LEFT; }
")"         { return SymbolCode.RIGHT; }
";"         { return SymbolCode.SEMICOLON; }
"{"         { return SymbolCode.LEFTCURLY; }
"}"         { return SymbolCode.RIGHTCURLY; }
"<"         { return SymbolCode.LESS; }
">"         { return SymbolCode.GREAT; }
"<="        { return SymbolCode.LESSOREQ; }
">="        { return SymbolCode.GREATOREQ; }
"="         { return SymbolCode.EQ; }
{Id}        { return SymbolCode.ID; }
{Int}       { return SymbolCode.INT; }
{Bool}      { return SymbolCode.BOOL; }
{TextLimit} { return SymbolCode.TEXTLIMIT; }
<<EOF>>     { return SymbolCode.EOF; }
{Space}     {}
