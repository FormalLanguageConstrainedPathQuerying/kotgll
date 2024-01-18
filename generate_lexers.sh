#!/bin/bash

cd src/main/kotlin/org/srcgll/lexer

for lexer_name in while.x Java.x
do
    jflex $lexer_name
done
