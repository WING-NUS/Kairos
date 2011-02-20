package Utility;
#
# This package contains utility methods
#
# Copyright 2010 @ Minh-Thang Luong
#
use strict;

#### List methods ###
# baseName
# checkFile, return 1 if the file exists, 0 otherwises
# checkDir, return 1 if the directory exists, 0 otherwises
# getNumLines
# getFilesInDir($inDir, $files, $pattern) 
# getHashRandom($n, $min, $max, $hashIndex)
# loadListHash
# loadListArray
# printHashKeyNum($hash, $outFile)
# printHashKeyAlpha($hash, $outFile)
# sequenceSim
# max
# getIdentityStr
# normalDistributionSet
# normalDistribution
#####################

#######################
### General methods ###
#######################
### Pattern replace for line(s) in a file using perl built-in command ###
sub perlReplace {
  my ($inFile, $findRegex, $replaceRegex) = @_;

  my $newFindRegex = escapeRegex($findRegex);
  my $newReplaceRegex = escapeRegex($replaceRegex);

  execute("perl -pi -e 's/$newFindRegex/$newReplaceRegex/g' $inFile");
}

sub escapeRegex {
  my ($regex) = @_;
  my $newRegex = $regex;
#  $newRegex =~ s/([^\\])\$/$1\\\$/g;
  $newRegex =~ s/([^\\])\//$1\\\//g;

  return $newRegex;
}

# baseName
sub baseName {
  my ($inFile) = @_;

  my $baseName = `basename $inFile`;
  chomp($baseName);
#  $baseName = untainPath($baseName);
  return $baseName;
}

# checkFile, return 1 if the directory exists, 0 otherwises
sub checkFile {
  my ($file) = @_;

  if(-f $file){
    print STDERR "#! File \"$file\" exists\n";
    return 1;
  } else {
    return 0;
  }
}

# checkDir, return 1 if the directory exists, 0 otherwises
sub checkDir {
  my ($outDir, $option) = @_;

  if(-d $outDir){
    print STDERR "#! Directory \"$outDir\" exists\n";
    return 1;
  } else {
    if(!defined $option || $option ne "quiet"){
      print STDERR "# Directory \"$outDir\" does not exist! Creating ...\n";
    }
    system("mkdir -p $outDir");
    return 0;
  }
}

# generate a hash of $n int random value btw [$min, $max]
sub getHashRandom {
  my ($n, $min, $max, $hashIndex) = @_;

  if($max < $min){
    die "Die: max $max < min $min\n";
  }

  if($n > ($max - $min + 1)){
    die "Die: can't generate $n distinct values in range [$min, $max]\n";
  }

  my $count = 0;
  my $upper = $min - 1;
  while($count < $n){
    my $num = $min + int(rand($max-$min+1)); #generate values in [0, $max-$min]
    if(!$hashIndex->{$num}){
      $hashIndex->{$num} = 1;
      $count++;
      
      if($num > $upper){
	$upper = $num;
      }
    }
  }

  return $upper;
}

# get the number of lines in a file
sub getNumLines {
  my ($inFile) = @_;

  ### Count & verify the totalLines ###
  chomp(my $tmp = `wc -l $inFile`);
  my @tokens = split(/ /, $tmp);
  return $tokens[0];
}

### Get a list of files in the provided directory, and sort them alphabetically###
sub getFilesInDir{
  my ($inDir, $files, $pattern) = @_;

  if(!-d $inDir) {
    die "Die: directory $inDir does not exist!\n";
  }
  
  if(!defined $pattern){
    $pattern = "";
  }

#  opendir DIR, $inDir or die "cannot open dir $inDir: $!";
#  my @files= grep { $_ ne '.' && $_ ne '..' && $_ !~ /~$/} readdir DIR;
#  closedir DIR;
  my $line = `find $inDir -type f`;
  my @tokens = split(/\s+/, $line);
  my @tmpFiles = ();
  foreach my $token (@tokens){
    if($token ne "" && $token !~ /^\.$/ && $token !~ /^\.\.$/ && $token !~ /~$/ && $token =~ /$pattern/){
      $token = untaintPath($token);
      push(@tmpFiles, $token);
    }
  }

  my @sorted_files = sort { $a cmp $b } @tmpFiles;
  @{$files} = @sorted_files;
}

sub loadListHash {
  my ($inFile, $hash) = @_;

  open(IF, "<:utf8", $inFile) || die "#Can't open file \"$inFile\"";

  print STDERR "# Loading $inFile ";
  my $count = 0;
  while(<IF>){ 
    chomp;
    
    my @tokens = split(/\s+/, $_);
    $hash->{$tokens[0]} = 1;
    $count++;
    if($count % 1000 == 0){
      print STDERR ".";
    }
  }
  print STDERR " - Done! Total lines read = $count\n";
  close IF;
}

sub loadListArray {
  my ($inFile, $array) = @_;

  open(IF, "<:utf8", $inFile) || die "#Can't open file \"$inFile\"";

  my $count = 0;
  while(<IF>){ 
    chomp;

    $array->[$count++] = $_;
  }

  close IF;
}

# print hash key numerical sort
sub printHashKeyNum {
  my ($hash, $outFile) = @_;

  if(defined $outFile){
    open(OF, ">:utf8", $outFile) || die "Can't open file \"$outFile\"\n";
  }

  my @keys = sort {$a <=> $b} keys (%{$hash});
  for my $key (@keys) {
    if(defined $outFile){
      print OF "$key\n";
    } else {
      print STDERR "$key\n";
    }
  }
}

# print hash key alphabetical sort
sub printHashKeyAlpha {
  my ($hash, $outFile) = @_;

  if(defined $outFile){
    open(OF, ">:utf8", $outFile) || die "Can't open file \"$outFile\"\n";
  }

  my @keys = sort {$a cmp $b} keys (%{$hash});
  for my $key (@keys) {
    if(defined $outFile){
      print OF "$key\n";
    } else {
      print STDERR "$key\n";
    }
  }
}

##
##$str = sort {$a <=> $b} keys (%sampleIndex);
# 84 for my $index (@indexes) {
#  85   print STDERR "$index\n";
#   86 }
#   , $str2) ##
# Compute similarity between 2 strings
# by computing the longest common subsequence
# of their two sequence of chars/words depending on split pattern (default is chars)
##
sub stringSim {
  my ($str1, $str2, $pattern) = @_;

  if(!defined $pattern){
    $pattern = "";
  }

  my @seq1 = split(/$pattern/, $str1);
  my @seq2 = split(/$pattern/, $str2);

  my $sim = sequenceSim(\@seq1, \@seq2);
  return $sim;
}

# Longest common subsequence
# compute similarity score between two sequences
# complexiy O(mn), m and n are the lengths of the two sequences
sub sequenceSim{
  my($seq1, $seq2) = @_;
  my $l1 = @{$seq1};

  my $l2 = @{$seq2};

  my @numMatches = ();

  # initialization
  for(my $i=0; $i<=$l1; $i++){
    push(@numMatches, []);
    for(my $j=0; $j<=$l2; $j++){
      $numMatches[$i]->[$j] = 0;
    }
  }

  # dynamic programming
  for(my $i=1; $i<=$l1; $i++){
    push(@numMatches, []);
    for(my $j=1; $j<=$l2; $j++){
      if($seq1->[$i-1] eq $seq2->[$j-1]){
	$numMatches[$i]->[$j] = 1 + $numMatches[$i-1]->[$j-1];
      }
      else {
	$numMatches[$i]->[$j] = $numMatches[$i-1]->[$j] > $numMatches[$i]->[$j-1] ?
	  $numMatches[$i-1]->[$j] : $numMatches[$i]->[$j-1];
      }
    }
  }
  
  my $sim = sprintf("%.3f", $numMatches[$l1]->[$l2]/max($l1, $l2));
  return $sim;
}

#2-argument
sub max {
  my ($a, $b) = @_;
  return ($a > $b) ? $a : $b;
}

sub getIdentityStr {
  my ($dir) = @_;

  if($dir eq ""){
    $dir = `pwd`;
    chomp($dir);
  }

  my $identityStr="";
  if($ENV{HOSTNAME} =~ /^(.+?)\./){
    $identityStr .= "$1 ";
  }
  my $date=`date`;
  $identityStr .= "$date\t$dir";

  return $identityStr;
}

sub normalDistributionSet {
  my ($numbers) = @_;

  my $count = scalar(@{$numbers});
  my $sum = 0;
  my $squareSum = 0;
  foreach(@{$numbers}){
    $sum += $_;
    $squareSum += ($_*$_);
  }
  return normalDistribution($sum, $squareSum, $count);
}

  
##
## normalDistribution($sum, $squareSum, $count) ##
#
# sum: the total sum of all element values
# squareSum: the total sum of squares of all element values
# count: num of elements
##
sub normalDistribution {
  my ($sum, $squareSum, $count) = @_;

  my $mean = $sum / $count;
  my $stddev = 0;
  if($count < 2) {
    $stddev = sqrt(($squareSum - $count*$mean*$mean)/ $count);
  } else {
    $stddev = sqrt(($squareSum - $count*$mean*$mean)/ ($count - 1));
  }

  return ($mean, $stddev);
}

sub untaintPath {
  my ($path) = @_;

  if ( $path =~ /^([\/\w\p{P}]*)$/ ) { #\p{C}\p{P}
    $path = $1;
  } else {
    die "Bad path $path\n";
  }

  return $path;
}

sub untaint {
  my ($s) = @_;
  if ($s =~ /^([\w \@\/\p{P}\p{S}\p{C}]*)$/) { #\p{C}\p{P}
    $s = $1;               # $data now untainted
  } else {
    die "Bad data in $s";  # log this somewhere
  }
  return $s;
}

sub execute {
  my ($cmd) = @_;
  print STDERR "# Executing: $cmd\n";
  $cmd = untaint($cmd);
  system($cmd);
}

sub newTmpFile {
  my $tmpFile = `date '+%Y%m%d-%H%M%S-$$'`;
  chomp($tmpFile);
  $tmpFile = untaintPath($tmpFile);
  return $tmpFile;
}

1;
