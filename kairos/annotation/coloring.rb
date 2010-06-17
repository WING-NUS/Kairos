#!/usr/bin/env ruby
# -*- ruby -*-
#
# Coloring.rb
#
# CVS: $Id: coloring.rb,v 1.9 2009/01/06 16:48:23 rpnlpir Exp $
#
# Author:: Min-Yen Kan (mailto:kanmy@comp.nus.edu.sg)
# Copyright:: 2006,2007,2008,2009 by Min-Yen Kan, National University of Singapore
# License::Proprietary
# 
# CVS: $Log: coloring.rb,v $
# CVS: Revision 1.9  2009/01/06 16:48:23  rpnlpir
# CVS: Fixed minor undo not saving annotations bug.
# CVS: Fixed annotation pane move problem.
# CVS:
# CVS: Revision 1.8  2008/12/15 16:09:11  rpnlpir
# CVS: with resume capability for images
# CVS:
# CVS: Revision 1.7  2008/12/15 15:14:13  rpnlpir
# CVS: with resume capability for spans
# CVS:
# CVS: Revision 1.6  2008/11/07 13:31:21  rpnlpir
# CVS: add support for image annotation
# CVS:
# CVS: Revision 1.5  2008/11/07 03:17:22  rpnlpir
# CVS: Fixed range coloring bug.
# CVS: Add version info in save file.
# CVS: Add array-based YAML file.
# CVS:
# CVS: Revision 1.4  2008/11/06 06:54:18  rpnlpir
# CVS: Faster file creation.
# CVS:
# CVS: Revision 1.3  2008/11/03 10:38:45  rpnlpir
# CVS: minor problem with spaces in hrefs corrected
# CVS:
# CVS: Revision 1.2  2008/11/03 09:31:48  rpnlpir
# CVS: keyboard shortcuts and undo added
# CVS:
# @@BASE_DIR = "/Users/NUS/coloring"
# $:.unshift("#{@@BASE_DIR}/lib/")
require 'optparse'
require 'time'
require 'yaml'
require 'tempfile'
require 'uri'
require 'net/http'

# defaults
@@VERSION = [1,6]
@@INTERVAL = 100
@@PROG_NAME = File.basename($0)
@@DEFAULT_YAML_FILE = "coloring.yaml"
@@yaml_file = ""
############################################################
# EXCEPTION HANDLING
int_handler = proc {
  # clean up code goes here
  STDERR.puts "\n# #{@@PROG_NAME} fatal\t\tReceived a 'SIGINT'\n# #{@@PROG_NAME}\t\texiting cleanly"
  exit -1
}
trap "SIGINT", int_handler

############################################################
class Coloring
  @coloring = Hash.new          # annotation colors and descriptions
  @base_href = nil              # base href for the file to save
  @base_dir = nil               # directory where the results should be
                                # written to, when viewed from the local host
  @annote_yaml_file = nil       # partial annotations from a previous 
                                # coloring round
  @annotations = nil
  @debug = true                # debug variable

  # constructor
  def initialize(yaml, base_href=nil, base_dir=nil, annote_yaml_file=nil)
    @coloring = YAML.load_file(yaml) # load annote guidelines into hash table
    @base_href = base_href      
    @base_dir = base_dir
    @annote_yaml_file = annote_yaml_file
    if @annote_yaml_file != nil 
      @annotations = YAML.load_file(annote_yaml_file) # load annote guidelines into hash table
    end
  end

  # process an input file from a filename.  Calls process_core()
  # for shared functionality with process_url_string
  def process_file(f,filename)
    buf = ""
    while !f.eof do
      buf += f.gets
    end
    f.close
    process_core(buf,filename)
  end

  # segments the string into head, body and processes them to create the
  # response
  def process_core(buf,filename)
    filename = filename.gsub(/.\w+$/i, ".yaml") # set the save file name
    if !filename.match(/\./) 
      filename += ".yaml"
    end
    # do work to split the head from the body of the html file
    # N.B. p_h_b does some work to insert js code in the head
    (head,body) = parse_head_body(buf,"#{@base_dir}#{filename}")

    # construct the response
    retval = ""
    retval += (head != nil) ? head : ""
    retval += process_body(body)# create necessary spans around text
    return(retval)
  end

  # parses the macro parts of the input document, processing the head portion
  def parse_head_body(buf,filename) 
    return_head = return_body = ""
    pre_head = pre_body = post_body = head_misc = body_misc = nil

    if match = /(<head)([^>]*>)/i.match(buf)
      pre_head = match.pre_match
      head_misc = match[2]
      post_head = match.post_match
      if match = /(<body)([^>]*>)/i.match(post_head)
        pre_body = match.pre_match
        body_misc = match[2]
        post_body = match.post_match

        return_head += pre_head
        return_head += "<head" + head_misc
        return_head += dump_script(filename)
        if (@base_href != nil) 
          return_head += "<base href=\"" + @base_href + "\"/>"
        end
        return_head += pre_body
        return_head += "<body onload=\"on_load()\" onkeyup=\"on_key_up(event)\"" 
        return_head += body_misc
        return_head += dump_hidden_div
        return_body += post_body
        
        return (Array.[](return_head,return_body))
      else
        STDERR.print "no matches"      # BUG big problem!
        return ""
      end
    else
      STDERR.print "no matches"        # BUG big problem!
      return ""
    end
    return ""
  end

  def process_body(body)
    index = 0                   # coloring span index
    mode = "normal"
	flag = false
    span_start_index = 0
    bufs = Array.new;         # for intermediate results
    if body != nil 
      # disable <a hrefs>, change them to spans
      body.gsub!(/<[aA]\s/, "<span onclick=\"return false\" style=\"text-decoration:underline; color:blue\" ")
      body.gsub!(/<\/[aA]\b/, "</span")
      # disable usemap, delete them
      body.gsub!(/usemap=\"[^\"]+/i,"")
      body.gsub!(/usemap=[^ ]+/i,"")
      # img tags
      # body.gsub!(/<img /i,"<img onmousedown=\"imd(this)\" onmouseup=\"imu(this)\" onmouseover=\"imv(this)\" ")
      body.gsub!(/<img /i,"<img onmousedown=\"imd(this)\" ")
      letters = body.split(//)
    
      STDERR.print "[total: #{letters.length}]"
	  letters.each_with_index do |l,i|
        if (i % 1000 == 0) 
          STDERR.print "[#{i}]"
        end
        
        if (mode == "normal" && l == '<' && letters[i+1..i+3].join("") == '!--')
          mode = "in_comment"
          if @debug then 
			bufs.push("[comment]") 
		  end
        elsif (mode == "normal" && l == '<' && letters[i+1..i+6].join("") == "script")
          mode = "in_script"
          if @debug then 
			bufs.push("[script]") 
		  end
        elsif (mode == "normal" && l == '<')
		  # modification ##########
		  if(flag == false)
			bufs.push("</span>")
			flag = true
		  end
		  # modification ##########
		  
          mode = "in_tag"
        elsif (l == '>') 
		  if (mode == "in_script" && letters[i-8..i-1].join("") == "</script")
            mode = "normal"
            if @debug then bufs.push("[/script]") end
          elsif (mode == "in_comment" && letters[i-2..i-1].join("") == "--")
            mode = "normal"
            if @debug then bufs.push("[/comment]") end
          elsif (mode == "in_tag")
            mode = "normal"
          end
        elsif mode == "normal"
		    if /\S/.match(l) 
				if /[\s>]/.match(letters[i-1]) 
					if @debug then 
						bufs.push("^") 
					end
					
					bufs.push(letters[span_start_index..i-1]) # flush buffer to intermediate array
					span_start_index = i # re-set span_start_index
					
					# modification ##########
					if(flag == true)
						index += 1
						# modified -> OPEN QUOTE IS EXPECTED FOR ATTRIBUTE "ATTR" ASSOCIATED WITH AN ELEMENT TYPE "ELEMENT".
						# http://mobiforge.com/testing/story/fixit-valid-markup#open_quote_expected
						bufs.push("<span class=\"coloringspan\" id=\"#{index}\" onmousedown=\"md(this)\" onmouseup=\"mu(this)\" >")
						flag = false
					end
					#########################
					
					
				end

				if /[<\s]/.match(letters[i+1])
					bufs.push(letters[span_start_index..i]) # flush buffer to intermediate array
					span_start_index = i+1
				
					if @debug then
						buf += "$" 
					end
              
					#bufs.push("</span>")
				end
				else 
					# a space
				end
			else
				# any other mode
			end
		end
    end
    STDERR.print "\n"
    return bufs.join("")
  end

  # dumps a here document that describes the code for the annotation layer  -> modified alert("off")...
  def dump_hidden_div
    div = <<DUMP_DIV_HEADER
<!-- BEGIN COLORING -->

<div id="theLayer" style="position:fixed;width:300;left:100;top:100;visibility:hidden;z-index:1000">
<table border="0" width="250" bgcolor="#424242" cellspacing="0" cellpadding="5">
<tr>
<td width="100%">
  <table border="0" width="100%" cellspacing="0" cellpadding="0" height="36">
  <tr>
  <td id="titleBar" style="cursor:move" width="100%" onmousedown="tbMouseDown(event)" onmouseup="alert('off'); ddEnabled=false">
<font face="Arial" color="#FFFFFF">Annotate selection as...</font>
  </td><td valign="top"><font color="#ffffff" size="2" face="arial" style="text-decoration:none">(<a href="#" onClick="cancel_annotation();return false" style="color: white">X</a>)</font>
  </td></tr>
  <tr><td width="100%"><font color="#ffffff" face="arial">(u)ndo (r)efresh e(x)it (1-4)colors</font></td></tr>
  <tr><td width="100%" bgcolor="#FFFFFF" style="padding:4px" colspan="2">
DUMP_DIV_HEADER
    @coloring.each_with_index do |h,index| 
      h.each_pair do |key,value|
        # use gsub to get back rgb colors correctly
        div += "<span class=\"palette\" onclick=\"done('#{key}','#{value}')\" style=\"background-color: #{value.gsub(/\\#/,"#")}\"> (#{index+1}) #{key}</span><br/>\n"
      end
    end
    div += "</td></tr></table></td></tr></table>\n</div>"
    div += "<!-- END COLORING -->"
    return div
  end
  
  # returns color from yaml file given annotation
  def lookup_color(annotation) 
    @coloring.each do |i|
      i.each_pair do |key,value|
        if key == annotation
          return value
        end
      end
    end
  end

  # dumps script code for the annotation and the save functionality
  def dump_script(savefile)
    keypressBuf = ""
    index = 49
    @coloring.each_with_index do |h,i| 
      keypressBuf += ((index+i) == 49) ? "if " : "    } else if "
      keypressBuf += "(e.keyCode == #{index+i}) {\n"
      h.each_pair do |key,value|
        keypressBuf += "      done(\"#{key}\",\"#{value}\")\n"
      end
    end
    keypressBuf += "  }"

    lookupColorBuf = ""
    @coloring.each_with_index do |h,i| 
      h.each_pair do |key,value|
        lookupColorBuf += (i == 0) ? "  if " : "    } else if "
        lookupColorBuf += "(annotation == \'#{key}\') {\n"
        lookupColorBuf += "      return (\'#{value}\');\n"
      end
    end
    lookupColorBuf += "  }"

    # check if loaded previous annotations; to be emitted in on_load()
    prevAnnoteBuf = "";
    if @annotations != nil 
      prevAnnoteBuf = "  // Start previous annotation store\n";
      @annotations["Spans"].each_index do |i|
        if @annotations["Spans"][i] != "na"
          annotation = @annotations["Spans"][i]
          color = lookup_color(annotation);
          prevAnnoteBuf += "  spanAnnotations[#{i}] = \"#{annotation}\";\n" 
          prevAnnoteBuf += "  backgrounds[#{i}] = \"#{color}\";\n"
        end
      end
      @annotations["Images"].each_index do |i|
        if @annotations["Images"][i] != "na"
          annotation = @annotations["Images"][i]
          color = lookup_color(annotation);
          prevAnnoteBuf += "  imageAnnotations[#{i}] = \"#{annotation}\";\n" 
        end
      end
      prevAnnoteBuf += "  // End previous annotation store\n";
    end

    time = Time.new
    script = <<DUMP_SCRIPT
<!-- BEGIN COLORING -->
<script type="text/javascript">
isIE = document.all;
topDivTag = isIE ? "body" : "html";
hotObject = "";
undoObject = "";
undoImage = "";
cantSave = false;

var savefile="#{savefile}";
var annotationLayer = null;
var span_start_index; // where the mouse starts
var span_end_index; // where the mouse ends
var spanAnnotations = new Array(); // annotations to be saved
var spans = new Array(); // shortcut variable
var undo_span_bg = new Array(); // undo bg buffer
var undo_span_annot = new Array(); // undo annotation buffer
var undo_span_start_index;
var undo_span_end_index;
var backgrounds = new Array(); // original background color

var pivot_tmp = new Array(); // for swapping

var image_start_index; // where the mouse starts
var image_end_index; // where the mouse ends
// assumes image borders are uniform on all four sides
var hot_image_border = ""; // for current hot image
var hot_image_border_color = "";
var hot_image_border_style = "";
var undo_image_annot = new Array(); // undo annotation buffer
var undo_image_borders = new Array(); // undo border width buffer
var undo_image_border_style = new Array(); // undo border style
var undo_image_border_color = new Array(); // undo border color
var imageAnnotations = new Array(); // annotations to be saved

function tbMouseDown(e){
  annotationLayer=isIE ? document.all.theLayer : document.getElementById("theLayer");
  hotDiv=isIE ? event.srcElement : e.target;
  while (hotDiv != null && hotDiv.id!="titleBar" && hotDiv.tagName != topDivTag){
    hotDiv=isIE ? hotDiv.parentElement : hotDiv.parentNode;
  }
  if (hotDiv.id=="titleBar"){
    offsetx=isIE ? event.clientX : e.clientX;
    offsety=isIE ? event.clientY : e.clientY;
    nowX=parseInt(annotationLayer.style.left);
    nowY=parseInt(annotationLayer.style.top);
    if (isNaN(nowX)) { nowX = 0; }
    if (isNaN(nowY)) { nowY = 0; }
    ddEnabled=true;
    document.onmousemove=dd;
    document.onmouseup=Function("ddEnabled=false");
  }
}

function dd(e){
  if (!ddEnabled) return;
  var left=isIE ? "" + (nowX+event.clientX-offsetx) + "px" : "" + (nowX+e.clientX-offsetx) + "px";
  var top = isIE ? "" + (nowY+event.clientY-offsety) + "px" : "" + (nowY+e.clientY-offsety) + "px";
  annotationLayer.style.left = left;
  annotationLayer.style.top = top;
  return false;
}

function cancel_annotation(){
  if (hotObject == "span") {
    for (i = span_start_index; i <= span_end_index; i++) {
      spans[i].style.backgroundColor = backgrounds[i];
      hotObject = "null"
    }
  } else if (hotObject == "image") {
    image = document.images[image_end_index];
    image.border = hot_image_border;
    image.style.borderColor = hot_image_border_color;
    image.style.borderStyle = hot_image_border_style;
    hotObject = "null"
    image_end_index = null;
  }
  hide_annotation_pane();
}

function hide_annotation_pane(){
  span_start_index = null;
  annotationLayer.style.visibility="hidden";
}

function show_annotation_pane(){
  annotationLayer.style.visibility="visible";
}

function on_load() {
  annotationLayer=isIE ? document.all.theLayer : document.getElementById("theLayer");
  var els = document.getElementsByTagName("span");
  var elsLen = els.length;
  var pattern = new RegExp("(^|\\s)coloringspan(\\s|$)");
  for (i = 0, j = 0; i < elsLen; i++) {
    if (pattern.test(els[i].className) ) {
      j = els[i].id;
      spans[j] = els[i];
      backgrounds[j] = els[i].style.background;
      spanAnnotations[j] = "na";
    }
  }
  if (document.images) {
    // populate undo data - simpler than spans as only one at a time
    for (i = 0; i < document.images.length; i++) {
      var image = document.images[i];
      imageAnnotations[i] = "na";
      undo_image_borders[i] = image.border;
      undo_image_border_style[i] = image.style.borderStyle;
      undo_image_border_color[i] = image.style.borderColor;
    }
  }
#{prevAnnoteBuf}
  refresh();
}

function on_key_up(e) {
  if (annotationLayer == null) {
    ;
  } else if (annotationLayer.style.visibility == "visible") {
    annot_pane_key_press(e); // pane visible
  } else {
    general_key_press(e) // pane invisible
  } 
}

function annot_pane_key_press(e) {
  if (e.keyCode >= 48 && e.keyCode <= 57) {
    #{keypressBuf}
  } else if (e.keyCode == 88) { // "x" close
    cancel_annotation();
  } else if (e.keyCode == 82) { // "r" refresh
    refresh();
    alert("annotations all refreshed!");
  } else if (e.keyCode == 85) { // "u" undo
    undo();
    save_annotations();
    cancel_annotation();
  }
}

function general_key_press(e) {
  if (e.keyCode == 85) { // "u" undo
    undo();
    save_annotations();
  } else if (e.keyCode == 82) { // "r" refresh
    alert("annotations all refreshed!");
    refresh();
  }
}

function lookupColor(annotation) {
#{lookupColorBuf}
}

function refresh() {
  for (i = 0; i <= backgrounds.length; i++) {
    if (spans[i] == null || spans[i] == 'na') { ; }
    else { spans[i].style.backgroundColor = backgrounds[i]; }
  }
  for (i = 0; i <= document.images.length; i++) {
    if (imageAnnotations[i] == null || imageAnnotations[i] == 'na') { ; }
    else { 
      var bg_color = lookupColor(imageAnnotations[i]);
      var image = document.images[i];
      image.style.borderColor = bg_color;
      image.style.borderStyle = 'solid';
      image.border = 5;
    }
  }
}

function undo() {
  if (undoObject == "span") {
    for (i = undo_span_start_index; i <= undo_span_end_index; i++) {
      pivot_tmp[i] = backgrounds[i];
      backgrounds[i] = undo_span_bg[i];
      undo_span_bg[i] = pivot_tmp[i];

      pivot_tmp[i] = spanAnnotations[i];
      spanAnnotations[i] = undo_span_annot[i];
      undo_span_annot[i] = pivot_tmp[i];

      spans[i].style.backgroundColor = backgrounds[i];
    }
  } else if (undoObject == "image") {
    var image = document.images[undoImage];
    var tmp = imageAnnotations[i];
    imageAnnotations[i] = undo_image_annot[i];
    undo_image_annot[i] = tmp;

    tmp = image.border;
    image.border = undo_image_borders[undoImage];
    undo_image_borders[undoImage] = tmp;

    tmp = image.style.borderColor;
    image.style.borderColor = undo_image_border_color[undoImage];
    undo_image_border_color[undoImage] = tmp;

    tmp = image.style.borderStyle;
    image.style.borderStyle = undo_image_border_style[undoImage];
    undo_image_border_style[undoImage] = tmp;
  }
}

function save_annotations() {
  try {
    netscape.security.PrivilegeManager.enablePrivilege("UniversalXPConnect");
    var file = Components.classes["@mozilla.org/file/local;1"].createInstance(Components.interfaces.nsILocalFile);
    file.initWithPath( savefile );
    if ( file.exists() == false ) { 
      try {
        file.create( Components.interfaces.nsIFile.NORMAL_FILE_TYPE, 420 ); 
      } catch (e) {
        alert("Permission to save file was denied.");
      }
    }
    var outputStream = Components.classes["@mozilla.org/network/file-output-stream;1"].createInstance( Components.interfaces.nsIFileOutputStream );
    outputStream.init( file, 0x04 | 0x08 | 0x20, 420, 0 );
    var buf = "\# Coloring annotation file for #{savefile} created on #{time} \\n\# Coloring version #{@@VERSION.join(".")}\\n"
    outputStream.write(buf, buf.length);
    var spansBuf = spanAnnotations.join("\\n  - ");
    var imagesBuf = imageAnnotations.join("\\n  - ");
    var output = "";
    if (spansBuf != "") { output += "Spans:\\n  - " + spansBuf + "\\n"; }
    if (imagesBuf != "") { output += "Images:\\n  - " + imagesBuf + "\\n"; }
    outputStream.write(output, output.length);
    outputStream.close();
  } catch (e) {
    if (!cantSave) {
      alert("Saving annotations will not work. The save path was specified incorrectly.  Proceed at your own risk.");
    }
    cantSave = true
  }
}

function md(obj) { // mouse_down (for spans)
  span_start_index = parseInt(obj.id);
}

function mu(obj) { // mouse up (for spans)
  // save stuff out
  hotObject = "span";
  span_end_index = parseInt(obj.id);
  if (span_end_index < span_start_index) {
    span_end_index = span_start_index;
    span_start_index = obj.id
  }
  for (i = span_start_index; i <= span_end_index; i++) {
    spans[i].style.backgroundColor = "cyan";
  }
  show_annotation_pane();
}

function imd(obj) { // mouse down (for images)
  var image = null
  if (document.images) { 
    if (document.images[image_end_index] == obj) { 
      // already highlighted, turn off 
      cancel_annotation();
    } else {
      // to be annotated
      hotObject = "image";
      for (i=0; i < document.images.length; i++) {
        if (document.images[i] == obj) {
          image_end_index = i;
          image = document.images[i];
          break;
        }
      } 
      hot_image_border = image.border;
      hot_image_border_color = image.style.borderColor;
      hot_image_border_style = image.style.borderStyle;
      image.border = 5;
      image.style.borderColor = 'cyan';
      image.style.borderStyle = 'dotted';
      show_annotation_pane();
    }
  }
}

function imu(obj) { ; }
function imv(obj) { ; } 

function done(annotation,bg_color) {
  if (hotObject == "span") { // annotate span
    for (i = span_start_index; i <= span_end_index; i++) {
      undo_span_bg[i] = backgrounds[i];
      undo_span_annot[i] = spanAnnotations[i];
      spanAnnotations[i] = annotation;
      backgrounds[i] = bg_color;
      spans[i].style.backgroundColor = bg_color;
    }
    undo_span_start_index = span_start_index;
    undo_span_end_index = span_end_index;
    undoObject = "span";
  } else if (hotObject == "image") { // annotate image
    var i = image_end_index;
    var image = document.images[i];
    undo_image_borders[i] = hot_image_border;
    undo_image_border_color[i] = hot_image_border_color;
    undo_image_border_style[i] = hot_image_border_style;
    imageAnnotations[i] = annotation;
    image.style.borderStyle = 'solid';
    image.style.borderColor = bg_color;
    image.border = 2;
    image_end_index = null;
    undoObject = "image";
    undoImage = i;
  }
  // reset
  hide_annotation_pane();
  save_annotations();
  hotObject = null;
}
</script>
<!-- END COLORING -->
DUMP_SCRIPT
    return(script)
  end
end # end of Coloring class 

############################################################

@@yaml_file = @@DEFAULT_YAML_FILE
@@base_href = nil
@@base_dir = nil
@@annote_yaml_file = nil

# set up options
OptionParser.new do |opts|
  opts.banner = "usage: #{@@PROG_NAME} [options] file_name"

  opts.separator ""
  opts.on_tail("-b", "--base-href HREF", "Set the basename for hrefs") do |href|
    @@base_href = href
  end
  opts.on_tail("-d", "--base-dir DIR", "Set the base directory to save annotations to (e.g., 'e:\\coloring\\')") do |dir|
    @@base_dir = dir
  end
  opts.on_tail("-h", "--help", "Show this message") do STDERR.puts opts; exit end
  opts.on_tail("-v", "--version", "Show version") do STDERR.puts "#{@@PROG_NAME} " + @@VERSION.join('.'); exit end
  opts.on_tail("-y", "--yaml-file YAML_FILE", "Use the YAML_FILE as the annotation description and coloring file") do |yaml| 
    @@yaml_file = yaml
  end
  opts.on_tail("-r", "--resume-yaml-file YAML_FILE", "Construct the output using partial annotations from the annotation yaml file YAML_FILE") do |yaml| 
    @@annote_yaml_file = yaml
  end
end.parse!

# determine files
ifs = Array.new                 # ifs = input file s
if (ARGV.size == 0) 
  ifs.push(STDIN)
else
  ifs = ARGV
end

# run main class
ifs.each do |fn|                # loop through each file
  # is each fn a url or a file?
  begin                         # is a url
    url_array = URI.extract(fn)
    f = Net::HTTP.get_response(URI.parse(url_array[0]))
    base_href = url_array[0]
    base_href.gsub!(/[^\/]+$/,"")
    url_fn = url_array[0].match(/[^\/]*$/)[0]
    if url_fn == "" then url_fn = "index.html" end # fix filename if empty
    c = Coloring.new(@@yaml_file, base_href, @@base_dir, @@annote_yaml_file)
    retval = c.process_core(f.body,url_fn)
  rescue                        # is a file
    f = (fn == STDIN or fn == "-") ? STDIN : File.open(fn)
    fn = (fn == STDIN or fn == "-") ? "stdin" : fn
    fn = fn.gsub(/.*\//,"");
    c = Coloring.new(@@yaml_file, @@base_href, @@base_dir, @@annote_yaml_file)
    retval = c.process_file(f,fn)
  end

  # print output
  if (retval != nil) then print retval end
end
