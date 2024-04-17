#!/bin/bash

shopt -s nullglob     #ingore failed patterns

cd ./benchmarks/src/jmh/kotlin/lexers

for lexer_name in *.jflex *.jlex *.lex *.flex *.x
do
    jflex $lexer_name
done