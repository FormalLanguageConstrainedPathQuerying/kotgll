#!/bin/bash

shopt -s nullglob     #ingore failed patterns

cd src/main/kotlin/org/srcgll/lexer

for lexer_name in *.jflex *.jlex *.lex *.flex *.x
do
    jflex $lexer_name
done