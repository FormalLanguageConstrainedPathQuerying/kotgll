#!/bin/bash

# Check if the correct number of arguments are provided
if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <destination-folder> <search-path>"
  exit 1
fi

# Assign arguments to variables
destination=$1
search_path=$2

# Check if the destination directory exists, if not, create it
if [ ! -d "$destination" ]; then
  mkdir -p "$destination"
fi

# Initialize counters
file_count=0
line_count=0
char_count=0
size=0

# Find, copy all .java files, and calculate the stats
while IFS= read -r -d '' file; do
  cp "$file" "$destination"
  ((file_count++))
  line_count=$(($line_count + $(wc -l < "$file")))
  char_count=$(($char_count + $(wc -m < "$file")))
  size=$(($size + $(stat --format=%s "$file")))
done < <(find "$search_path" -name "*.java" -print0)

echo "$file_count .java files have been copied to the $destination folder."
echo "Total lines: $line_count"
echo "Total characters: $char_count"
echo "Total size on disk: $size bytes"

# Convert size from bytes to megabytes
size_mb=$(awk "BEGIN {printf \"%.2f\", $size/1024/1024}")


echo "#Files,#Chars,SLOC,Size (on disk)"
echo "$file_count,$char_count,$line_count,$size_mb"