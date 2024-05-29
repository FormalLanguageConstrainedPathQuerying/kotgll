#!/bin/bash
shopt -s nullglob     #ingore failed patterns
rootPrj=$(pwd)
parserDest="../benchmarks/src/main/kotlin/"
antlrSrc="benchmarks/src/main/java/org/antlr"
lexerSrc="examples/src/main/java/java7"

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

printf "\n\nRUN BENCHMARKS\n"
mkdir benchmarks/logs
for dataset in /home/olga/gllgen/java7/junit #/home/olga/gllgen/dataset_black_box/too_little
do
  for tool in Recovery #Antlr Online Offline
  do
    echo "running $tool on $dataset, start at $(date)"
    ./gradlew benchmark -PtoolName=$tool -Pdataset=$dataset >> benchmarks/logs/stdout_$tool.txt 2>> benchmarks/logs/stderr_$tool.txt
  done
done

