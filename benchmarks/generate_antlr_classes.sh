#!/bin/bash

shopt -s nullglob     #ingore failed patterns

cd ./benchmarks/src/jmh/kotlin/antlr4

antlr4 Java8.g4