#!/usr/bin/perl -wT
# Author: Luong Minh Thang <luongmin@comp.nus.edu.sg>, generated at Tue, 21 Dec 2010 22:42:14


require 5.0;
use strict;
use Getopt::Long;

# I do not know a better solution to find a lib path in -T mode.
# So if you know a better solution, I'd be glad to hear.
# See this http://www.perlmonks.org/?node_id=585299 for why I used the below code
use FindBin;
FindBin::again(); # to get correct path in case 2 scripts in different directories use FindBin
my $path;
BEGIN {
  if ($FindBin::Bin =~ /(.*)/) {
    $path = $1;
  }
}
use lib "$path";
use Result;

### USER customizable section
$0 =~ /([^\/]+)$/; my $progname = $1;
my $outputVersion = "1.0";
### END user customizable section

sub License {
  print STDERR "# Copyright 2010 \251 by Luong Minh Thang\n";
}

### HELP Sub-procedure
sub Help {
  print STDERR "usage: $progname -h\t[invokes help]\n";
  print STDERR "       $progname -in inFile -out outFile\n";
  print STDERR "Options:\n";
  print STDERR "\t-q\tQuiet Mode (don't echo license)\n";
}
my $QUIET = 0;
my $HELP = 0;
my $inFile = undef;
my $outFile = undef;

$HELP = 1 unless GetOptions('in=s' => \$inFile,
			    'out=s' => \$outFile,
			    'h' => \$HELP,
			    'q' => \$QUIET);

if ($HELP || !defined $inFile || !defined $outFile) {
  Help();
  exit(0);
}

if (!$QUIET) {
  License();
}

### Untaint ###
$inFile = untaintPath($inFile);
$outFile = untaintPath($outFile);
untaintEnv("PATH");
untaintEnv("ENV");
untaintEnv("BASH_ENV");
### End untaint ###

processFile($inFile, $outFile);

sub processFile{
  my ($inFile, $outFile) = @_;
  
  #file I/O
  if(! (-e $inFile)){
    die "#File \"$inFile\" doesn't exist";
  }
  open(IF, "<:utf8", $inFile) || die "#Can't open file \"$inFile\"";
  open(OF, ">:utf8", $outFile) || die "#Can't open file \"$outFile\"";
 
  my $version = "110101";
  my $time = `date +%s`; chomp($time);
  my $date = `date`; chomp($date);

  print OF "<?xml version=\"1.0\" encoding='UTF-8'?>\n";
  print OF "<results user=\"Kairos\" time=\"$time\" date=\"$date\">\n";
  print OF "<papers>\n";

  #process input file
  my $isStartDoc = 0;
  my $result = undef;
  
  my ($title, $author, $proceeding, $beginDate, $endDate) = ("", "", "", "", "");
  while(<IF>){
    chomp;
   
    if(/<doc>/){
      $isStartDoc = 1;
      $result = new Result();
    }
    elsif(/<\/doc>/){
      # get year
      my $year = "";
      if($beginDate =~ /\b((19|20)\d\d)\b/){
        $year = $1; 
      }
      if($endDate =~ /\b((19|20)\d\d)\b/){
        $year = $1; 
      }
      $result->setYear($year);

      # get id
      my $cleanTitle = $title; $cleanTitle =~ s/[^\w]//g;
      my $cleanAuthor = $author; $cleanAuthor =~ s/[^\w]//g;
      my $cleanProceeding = $proceeding; $cleanProceeding =~ s/[^\w]//g;
      my $id = "$cleanTitle-$cleanAuthor-$cleanProceeding-$year";
      $result->setId($id);


      print OF $result->toXML();
      $isStartDoc = 0;
      $result = undef;
      ($title, $author, $proceeding, $beginDate, $endDate) = ("", "", "", "", "");
    }

    elsif(/<field name=\"(.+?)\">(.+)<\/field>/){ # field
      my $field = $1;
      my $value = $2;

      if($field eq "p_author"){
        $author = $value;
        $result->setAuthor($value);
      }
      elsif($field eq "p_title"){
        $title = $value;
        $result->setTitle($value);
      }
      elsif($field eq "p_affiliation"){
        $result->setAffiliation($value);
      }
      elsif($field eq "c_name"){
        $proceeding = $value;
        $result->setProceeding($value);
      }
      elsif($field eq "c_begin_date"){
        $beginDate = $value;
        $result->setBeginDate($value);
      }
      elsif($field eq "c_end_date"){
        $endDate = $value;
        $result->setEndDate($value);
      }
      elsif($field eq "c_place"){
        $result->setPlace($value);
      }
      elsif($field eq "c_url"){
        $result->setUrl($value);
      }
    }
  }
 
  print OF "</papers>\n</results>\n";
  close IF;
  close OF;
}


####################
## Helper methods ##
####################
sub untaintEnv {
  my ($name) = @_;

  my $envPath = $ENV{$name};
  if(defined $envPath){  
    $envPath = untaintPath($envPath);
    $ENV{$name} = $envPath;
  }
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
  my ($cmd, $header) = @_;

  if(defined $header){
    print STDERR "$header\n";
  }

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
