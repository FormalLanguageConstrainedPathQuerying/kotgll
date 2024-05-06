#!/bin/bash
shopt -s expand_aliases
cd src/main/kotlin/org/srcgll/lexer

printf "\n\nGENERATE LEXER FILE\n"
jflex Java.x

cd ../../../../../..

cd src/main/kotlin/org/antlr/

printf "\n\nGENERATE ANTLR4 FILES\n"
antlr4 Java8.g4

cd ../../../../..

printf "\n\nBUILD PROJECT BY GRADLEW\n"
./gradlew JmhJar
