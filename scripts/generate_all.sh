#!/bin/bash
shopt -s nullglob     #ingore failed patterns
rootPrj=$(pwd)
parserDest="../benchmarks/src/jmh/kotlin/"
antlrSrc="benchmarks/src/jmh/java/org/antlr"
lexerSrc="examples/src/main/java/java8"

printf "\n\nINSTALL PACKAGES\n"
apt-get install jflex
apt-get install antlr4

printf "\n\nGENERATE FILES\n"

printf "\nGenerate ANTLR4 files"
cd $antlrSrc
antlr4 Java8.g4
cd $rootPrj

printf "\nGenerate lexers"
cd $lexerSrc
for lexer_name in *.jflex *.jlex *.lex *.flex *.x
do
    jflex $lexer_name
done
cd $rootPrj


printf "\nGenerate UCFS parser files at"
echo $parserDest
./gradlew :examples:run --args=$parserDest