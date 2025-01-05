#!/bin/bash
shopt -s nullglob     #ingore failed patterns
rootPrj=$(pwd)

parserDest="benchmarks/src/main/kotlin/org/ucfs"
antlrSrc="benchmarks/src/main/java/org/antlr"
fastAntlrSrc="benchmarks/src/main/java/org/antlr/fast"
antlrPackage="org.antlr"
antlrFastPackage="org.antlr.fast"

lexerSrc="benchmarks/src/main/java/org/ucfs/scanner"

printf "\n\nINSTALL PACKAGES\n"
apt-get install jflex
apt-get install antlr4

printf "\n\nGENERATE FILES\n"

printf "\nGenerate ANTLR4 files"

cd $antlrSrc
antlr4 -package $antlrPackage Java8Lexer.g4
antlr4 -package $antlrPackage Java8Parser.g4
cd $rootPrj

printf "\nGenerate fast ANTLR4 files"
cd $fastAntlrSrc
antlr4 -package $antlrFastPackage JavaLexer.g4
antlr4 -package $antlrFastPackage JavaParser.g4
cd $rootPrj

printf "\nGenerate lexers"
cd $lexerSrc
for lexer_name in *.jflex *.jlex *.lex *.flex *.x
do
    jflex $lexer_name
done
cd $rootPrj

printf  $(pwd)
printf "\nGenerate UCFS parser files at"
echo $parserDest