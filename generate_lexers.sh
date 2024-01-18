#!/bin/bash
sudo apt-get install -y jflex

cd src/main/kotlin/org/srcgll/lexer

jflex while.x
jflex Java.x

