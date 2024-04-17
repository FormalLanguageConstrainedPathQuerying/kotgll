#!/bin/bash

shopt -s nullglob     #ingore failed patterns

cd src/jmh/kotlin/antlr4 || exit

antlr4 Java8.g4