#!/bin/bash
shopt -s nullglob     #ingore failed patterns
rootPrj=$(pwd)

printf "\n\nRUN BENCHMARKS\n"
mkdir -p benchmarks/logs
for dataset in #/home/olga/gllgen/java7/junit #/home/olga/gllgen/dataset_black_box/too_little
do
  for tool in Recovery #Antlr Online Offline
  do
    echo "running $tool on $dataset, start at $(date)"
    ./gradlew benchmark -PtoolName=$tool -Pdataset=$dataset >> benchmarks/logs/stdout_$tool.txt 2>> benchmarks/logs/stderr_$tool.txt
  done
done
