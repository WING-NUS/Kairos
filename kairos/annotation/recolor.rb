#!/usr/bin/env ruby
# -*- ruby -*- 
#
# Recolor.rb
# 
# - takes in yaml file
# - take in original html file
# - outputs final file
# 
# CVS: $Id: recolor.rb,v 1.1 2009/01/06 09:18:25 rpnlpir Exp $
#
# Author:: Min-Yen Kan (mailto:kanmy@comp.nus.edu.sg)
# Copyright:: 2006,2007,2008 by Min-Yen Kan, National University of Singapore
# License::Proprietary
#
# CVS: $Log: recolor.rb,v $
# CVS: Revision 1.1  2009/01/06 09:18:25  rpnlpir
# CVS: *** empty log message ***
# CVS: 
# @@BASE_DIR = "/Users/NUS/coloring"
# $:.unshift("#{@@BASE_DIR}/lib/")
require 'optparse'
require 'time'
require 'yaml'
require 'uri'
require 'net/http'

# defaults
@@VERSION = [1,0]
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
class Recoloring
  # constructor
  # taken from Coloring with small modification
  def initialize(yaml, annote_yaml_file, base_href=nil)
    @coloring = YAML.load_file(yaml) # load color map into hash table
    @base_href = base_href      
    @annotations = YAML.load_file(annote_yaml_file) # load annotations
  end

  # process an input file from a filename.  Calls process_core()
  # for shared functionality with process_url_string
  # taken from Coloring without modification
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
  # taken from Coloring with minor modification
  def process_core(buf,filename)
    filename = filename.gsub(/.\w+$/i, ".yaml") # set the save file name
    if !filename.match(/\./) 
      filename += ".yaml"
    end
    # do work to split the head from the body of the html file
    (head,body) = parse_head_body(buf,"#{@base_dir}#{filename}")

    # construct the response
    retval = ""
    retval += (head != nil) ? head : ""
    retval += process_body(body) # transform necessary spans around text
    return(retval)
  end

  # parses the macro parts of the input document, processing the head portion
  def parse_head_body(buf,filename) 
    return_head = return_body = ""
    pre_head = pre_body = head_misc = body_misc = nil

    if match = /(<head)([^>]*>)/i.match(buf)
      pre_head = match.pre_match
      head_misc = match[2]
      post_head = match.post_match
      if match = /(<body)([^>]*>)/i.match(post_head)
        pre_body = match.pre_match
        body_misc = match[2]
        return_body = match.post_match

        return_head += pre_head
        return_head += "<head" + head_misc
        if (@base_href != nil) 
          return_head += "<base href=\"" + @base_href + "\"/>"
        end
        return_head += pre_body
        return_head += "<body" + body_misc

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

  # BUG: need to fix
  def process_body(body)
    index = 0                   # coloring span index
    mode = "normal"
    span_start_index = 0
    bufs = Array.new;         # for intermediate results
    if body != nil 
      # disable <a hrefs>, change them to spans
      body.gsub!(/<[aA]\s/, "<span class=ahref ")
      body.gsub!(/<\/[aA]\b/, "</span class=ahref")
      # disable usemap, delete them
#      body.gsub!(/usemap=\"[^\"]+/i,"")
#      body.gsub!(/usemap=[^ ]+/i,"")

      # img tags
      # split/scan for equiv of perl's /g looped regex match
      images = Array.new
      imd = body.scan(/<img [^>]+>/i) 
      if imd != nil # only treat images if there are actually images in the document!
        imd.each_with_index { |m,i|
          if (@annotations["Images"][i] != "na") 
            ann = @annotations["Images"][i]
            color = lookup_color(ann)
            m = ("<img class=coloringimg coloringvalue=\"#{ann}\" style=\"border-color:#{color}; border-style:solid; border:4\"" + m[4..-1])
            m.sub!(/border=\"?\S+\"?/i,"") # delete any border information that was in the image before
          end
          images << m
        }
        segs = body.split(/<img [^>]+>/i)
        # reknit together the body
        body = segs[0]          # initial fencepost
        images.each_with_index { |m,i|
          body += m + segs[i+1]
        }
      end
        
      letters = body.split(//)
    
      STDERR.print "[total: #{letters.length}]"
      letters.each_with_index do |l,i|
        if (i % 1000 == 0) 
          STDERR.print "[#{i}]"
        end
        
        if (mode == "normal" && l == '<' && letters[i+1..i+3].join("") == '!--')
          mode = "in_comment"
          if @debug then bufs.push("[comment]") end
        elsif (mode == "normal" && l == '<' && letters[i+1..i+6].join("") == "script")
          mode = "in_script"
          if @debug then bufs.push("[script]") end
        elsif (mode == "normal" && l == '<')
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
              if @debug then bufs.push("^") end
              bufs.push(letters[span_start_index..i-1]) # flush buffer to intermediate array
              span_start_index = i # re-set span_start_index
              if (@annotations["Spans"][index] != "na")
                ann = @annotations["Spans"][index]
                color = lookup_color(ann)
                color.gsub!(/\\/,"")
                bufs.push("<span class=coloringspan coloringvalue=\"#{ann}\" style=\"background-color: #{color}\">")
              end
              index += 1
            end

            if /[<\s]/.match(letters[i+1])
              bufs.push(letters[span_start_index..i]) # flush buffer to intermediate array
              span_start_index = i+1
              if @debug then buf += "$" end
              if (@annotations["Spans"][index-1] != "na")
                bufs.push("</span>")
              end
            end
          else # a space
          end
        else # any other mode
        end
      end
    end
    STDERR.print "\n"
    body = bufs.join("")
    body.gsub!("<span class=ahref ", "<a ")
    body.gsub!("</span class=ahref", "</a")
    return body
  end

  # returns color from yaml file given annotation
  # taken from Coloring without modification
  def lookup_color(annotation) 
    @coloring.each do |i|
      i.each_pair do |key,value|
        if key == annotation
          return value
        end
      end
    end
  end

end # end of Recoloring class

############################################################
@@yaml_file = @@DEFAULT_YAML_FILE
@@base_href = nil
@@base_dir = nil

# set up options
OptionParser.new do |opts|
  opts.banner = "usage: #{@@PROG_NAME} [options] yaml_annotation_file file_name"

  opts.separator ""
  opts.on_tail("-b", "--base-href HREF", "Set the basename for hrefs") do |href|
    @@base_href = href
  end
  opts.on_tail("-h", "--help", "Show this message") do STDERR.puts opts; exit end
  opts.on_tail("-v", "--version", "Show version") do STDERR.puts "#{@@PROG_NAME} " + @@VERSION.join('.'); exit end
  opts.on_tail("-y", "--yaml-file YAML_FILE", "Use the YAML_FILE as the annotation description and coloring file") do |yaml| 
    @@yaml_file = yaml
  end
end.parse!

# determine files
ifs = Array.new                 # ifs = input file s
if (ARGV.size != 2) 
  STDERR.puts "# #{@@PROG_NAME} fatal\t\tNeed yaml annotation file and source html file/url as arguments, see usage -h"
  exit
else
  ifs = ARGV
end

# deal with file
begin                         # is a url
  url_array = URI.extract(ARGV[1])
  f = Net::HTTP.get_response(URI.parse(url_array[0]))
  base_href = url_array[0]
  base_href.gsub!(/[^\/]+$/,"")
  url_fn = url_array[0].match(/[^\/]*$/)[0]
  if url_fn == "" then url_fn = "index.html" end # fix filename if empty
  c = Recoloring.new(@@yaml_file, ARGV[0], base_href)
  retval = c.process_core(f.body,url_fn)
rescue
  f = (ARGV[1] == STDIN or ARGV[1] == "-") ? STDIN : File.open(ARGV[1])
  fn = (ARGV[1] == STDIN or ARGV[1] == "-") ? "stdin" : ARGV[1]
  fn = fn.gsub(/.*\//,"");
  c = Recoloring.new(@@yaml_file, ARGV[0], @@base_href)
  retval = c.process_file(f,fn)
end

# print output
if (retval != nil) then print retval end

