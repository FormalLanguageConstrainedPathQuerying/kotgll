#!/bin/bash
for lexer_name in while.x Java.x
do 
    jflex $lexer_name
done