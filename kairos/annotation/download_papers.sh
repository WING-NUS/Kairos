#!/bin/bash

###############################
# Download all the paper URLs #
###############################

cat papers.txt | 
while read line; do 
	DATE=`date +%Y-%m-%d__%H-%M-%S`	

	replvar=${line/:/+};
	replvar=${replvar//\/\//^^};
	replvar=${replvar//\//^};
	
	echo $line
	echo ${replvar}

	wget --output-document ${replvar}_${DATE}".html" $line
done