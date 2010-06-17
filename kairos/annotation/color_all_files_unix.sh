#!/bin/bash

PWD="`pwd`"

for currentfile in ${PWD}/repository/*.html
do
./coloring.rb -d "${PWD}/annotated_repository/" "repository/${currentfile##*/}" > "annotated_repository/${currentfile##*/}"
done
