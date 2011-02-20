package Result;
#
# Container object for GS result
#
# Minh-Thang Luong, 16 Sep 09
#

use strict;

sub new {
    my ($class) = @_;

    my %citeInfo = ();
    my $self = {
		'_id' => undef,
		'_title' => undef, # p_title
		'_author' => undef, # p_author
		'_proceeding' => undef, # c_name
                '_url' => undef, # c_url
		'_year' => undef, 

                '_affiliation' => undef, # p_affiliation
		'_beginDate' => undef, # c_begin_date
		'_endDate' => undef, # c_end_date
		'_place' => undef, # c_place 
	       };

    bless $self, $class;
    return $self;

} # new


##
# Looks for various combinations of data that could be used to
# uniquely identify a citation.  If too much data is missing,
# returns 0; otherwise, returns 1.
##
sub isValid {
    my ($self) = @_;
    my $title = $self->getTitle();

    if (defined $title) {
	return 1;
    }

    return 0;
} # isValid


##
# Utility for loading in a datum based on a tag from Tr2crfpp output.
##
sub loadDataItem {
    my ($self, $tag, $data) = @_;

    if ($tag eq "id") {
	$self->setId($data);
    }

    if ($tag eq "title") {
	$self->setTitle($data);
    }

    if ($tag eq "author") {
	$self->setAuthor($data);
    }

    if ($tag eq "proceeding") {
	$self->setProceeding($data);
    }

    if ($tag eq "url") {
	$self->setUrl($data);
    }

    if ($tag eq "year") {
	$self->setYear($data);
    }

    if ($tag eq "affiliation") {
	$self->setAffiliation($data);
    }

    if ($tag eq "beginDate") {
	$self->setBeginDate($data);
    }

    if ($tag eq "endDate") {
	$self->setEndDate($data);
    }

    if ($tag eq "place") {
	$self->setPlace($data);
    }
} # loadDataItem

##
# Returns a well-form XML for ForeCite ingestion
##
sub toXML {
  my ($self) = @_; #shift;
 
  my $id = $self->getId();
  my $title = $self->getTitle();
  my $authorStr = $self->getAuthor();
  my $proceeding = $self->getProceeding();
  my $year = $self->getYear();
  my $url = $self->getUrl();
  my $affiliation = $self->getAffiliation();
  my $beginDate = $self->getBeginDate();
  my $endDate = $self->getEndDate();
  my $place = $self->getPlace();
 
  my $prefix .= "  ";

  ### Construct xml
  my $xml = ""; # construct header at the end
  my $isComplete = "yes"; # 0 if one of the fields is incomplete
  if(defined $title){
    $xml .= $prefix . "  <title>".cleanText($title)."</title>\n";
  }

  # affiliation
  if(defined $affiliation && $affiliation ne "") { 
    $xml .= $prefix . "  <affiliation>".cleanURL($affiliation)."</affiliation>\n"; 
  }

  # beginDate
  if(defined $beginDate && $beginDate ne "") { 
    $xml .= $prefix . "  <beginDate>".cleanURL($beginDate)."</beginDate>\n"; 
  }

  # endDate
  if(defined $endDate && $endDate ne "") { 
    $xml .= $prefix . "  <endDate>".cleanURL($endDate)."</endDate>\n"; 
  }

  # place
  if(defined $place && $place ne "") { 
    $xml .= $prefix . "  <place>".cleanURL($place)."</place>\n"; 
  }

  # conference url
  if(defined $url && $url ne "") { 
    $xml .= $prefix . "  <c_url>".cleanURL($url)."</c_url>\n"; 
  }

  # author
  if(defined $authorStr) { 
    my @authors = ();
    my $tmpStr = "";

    getIndividualAuthors(cleanText($authorStr), \@authors);
    if(scalar(@authors) > 0){
      $xml .= $prefix . "  <authors>\n"; 
      for my $author (@authors){
        $xml .= $prefix . "    <author>".cleanText($author)."</author>\n";
      }
      $xml .= $prefix . "  </authors>\n"; 
    } else { # no author
      $isComplete = "no";
    }
  }

  if(defined $proceeding && $proceeding !~ /(\.com|\.edu|\.org)/ && $proceeding !~ /\w{2,4}\.\w{2,4}\.\w{2,4}/) { 
    my $proceedingType = "booktitle";
    if($proceeding =~ /journal/i){
      $proceedingType = "journal";
    }
    $xml .= $prefix . "  <$proceedingType>".cleanText($proceeding)."</$proceedingType>\n"; 
  }
  
  if(defined $year && $year =~ /\d\d/) { 
    $xml .= $prefix . "  <year>".cleanText($year)."</year>\n"; 
  }
     
  my $bibType = "misc";
  if(defined $proceeding && $proceeding =~ /(proceeding|conference)/i){
    $bibType = "inproceedings";
  }

  my $newXml = $prefix . "<$bibType key=\"$id\" complete=\"$isComplete\">\n".$xml;
  $newXml .= $prefix . "</$bibType>\n";

  return $newXml;
} # toXML

# Preprocess & break into multiple authors
sub getIndividualAuthors {
  my ($authorStr, $authorArray) = @_;

  # Preprocess & break into multiple authors
  my @authors = split(/\s*,\s*/, $authorStr);
  foreach my $author (@authors){
    if($author =~ /\.\.\./){ # discard incomplete author
      next;
    }

    my @tokens = split(/\s+/, $author);
    my $numTokens = scalar(@tokens);
    $author = join(" ", @tokens);
    push(@{$authorArray}, $author);
  }
}

sub cleanURL {
  my ($text) = @_;

  while($text =~ /&amp;/){
    $text =~ s/&amp;/&/g;
  }
  $text =~ s/&/&amp;/g;

  return $text;
}

sub cleanText {
  my ($text) = @_;

  # handle XML entities
  $text =~ s/&hellip;/&#8230;/g; # convert &hellip; to the xml entitity...
  $text =~ s/\x{0093}/&#8220;/g; # convert to the xml entitity of ldquo "
  $text =~ s/\x{0094}/&#8221;/g; # convert to the xml entitity of rdquo "
  
  $text =~ s/<.+?>//g; # remove tag
  $text =~ s/^\s+//; # strip leading spaces
  $text =~ s/\s+$//; # strip trailing spaces
  $text =~ s/  +/ /g; # remove double spaces 

  $text =~ s/>/&gt;/g;
  $text =~ s/</&lt;/g;
  $text =~ s/\"/&quot;/g;
  $text =~ s/\'/&apos;/g;

  $text =~ s/&(#|amp;|gt;|lt;|quot;|apos;)/&THANG$1/g;
  $text =~ s/&/&amp;/g;
  $text =~ s/&amp;THANG/&/g;
  return $text;
}

###### Getters and setters #######
sub getId {
    my ($self) = @_;
    return $self->{'_id'};
}

sub setId {
    my ($self, $id) = @_;
    $self->{'_id'} = $id;
}

sub getTitle {
    my ($self) = @_;
    return $self->{'_title'};
}

sub setTitle {
    my ($self, $title) = @_;
    $self->{'_title'} = $title;
}

sub getAuthor {
    my ($self) = @_;
    return $self->{'_author'};
}

sub setAuthor {
    my ($self, $author) = @_;
    $self->{'_author'} = $author;
}

sub getProceeding {
    my ($self) = @_;
    return $self->{'_proceeding'};
}

sub setProceeding {
    my ($self, $proceeding) = @_;
    $self->{'_proceeding'} = $proceeding;
}

sub getYear {
    my ($self) = @_;
    return $self->{'_year'};
}

sub setYear {
    my ($self, $year) = @_;
    $self->{'_year'} = $year;
}

sub getUrl {
    my ($self) = @_;
    return $self->{'_url'};
}

sub setUrl {
    my ($self, $url) = @_;
    $self->{'_url'} = $url;
}

sub getAffiliation {
    my ($self) = @_;
    return $self->{'_affiliation'};
}

sub setAffiliation {
    my ($self, $affiliation) = @_;
    $self->{'_affiliation'} = $affiliation;
}

sub getBeginDate {
    my ($self) = @_;
    return $self->{'_beginDate'};
}

sub setBeginDate {
    my ($self, $beginDate) = @_;
    $self->{'_beginDate'} = $beginDate;
}

sub getEndDate {
    my ($self) = @_;
    return $self->{'_endDate'};
}

sub setEndDate {
    my ($self, $endDate) = @_;
    $self->{'_endDate'} = $endDate;
}

sub getPlace {
    my ($self) = @_;
    return $self->{'_place'};
}

sub setPlace {
    my ($self, $place) = @_;
    $self->{'_place'} = $place;
}

1;
