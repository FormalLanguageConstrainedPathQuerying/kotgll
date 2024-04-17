#!/bin/bash

shopt -s nullglob     #ingore failed patterns

echo "\nCurrent directory: "
echo "$PWD"

cd ./src/jmh/kotlin/antlr4

antlr4 Java8.g4