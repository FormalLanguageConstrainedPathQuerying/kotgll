#!/bin/bash

shopt -s nullglob     #ingore failed patterns
rootPrj=$(pwd)
echo "\nGenerate lexers"

cd ./benchmarks/src/main/java/org/ucfs

for lexer_name in *.jflex *.jlex *.lex *.flex *.x
do
    jflex $lexer_name
done

echo "\nGenerate ANTLR4 files"
cd $rootPrj
cd ./benchmarks/src/main/java/org/antlr

antlr4 Java8.g4