#!/bin/sh

# Author: Luong Minh Thang <luongmin@comp.nus.edu.sg>, generated at Sun, 01 Jun 2008 15:21:09

date

if [ $# != "1" ] 
then
  date=`date`
  msg="$USER@$HOSTNAME $date"
else
  msg=$1
fi

echo "git add ."
git add .

echo "git commit -a -m \"$msg\""
git commit -a -m "$msg"

echo "git pull origin"
git pull origin

echo "git push origin"
git push origin
