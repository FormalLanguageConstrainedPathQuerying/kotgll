#!/bin/bash
cd build/libs
java -cp srcgll-jmh.jar -Djmh.ignoreLock=true org.openjdk.jmh.Main -i 15 -bs 1 -r 0 -to "10s" -wi 5 -w 0 -bm ss -gc true -foe false -f 1 -rff "../results.csv" -tu ns -jvmArgs "-Xmx4096m -Xss4m -XX:+UseG1GC"
