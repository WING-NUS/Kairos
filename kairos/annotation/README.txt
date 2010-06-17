# CVS: $Id: README.txt,v 1.5 2009/01/06 16:49:16 rpnlpir Exp $

This software is copyrighted 2008,2009 by Min-Yen Kan. This program and all
its source code is distributed are under the terms of the GNU General
Public License (or the Lesser GPL).


This is a HTML annotation tool.

It works by processing a input HTML file or a URL.  The output is the
original file but adds extra javascript and alters <A HREF>s tags so
that the text can be annotated.  A user can then annotate this file by
using a javascript-enabled browser by simply highlighting spans
(starting on a word and ending on a word) and selecting an appropriate
annotation from the annotation pane.  The user can also annotate
images with the same tags by clicking on them directly.

The annotation pane's palette is controlled by an external .YAML file,
by default "coloring.yaml" but this can be altered by the -y command
line switch.

- (v081107) You may resume a previous annotation session by including the
annotation .YAML file using the '-r <annotation>.yaml' switch.  

- (v090106) You can use the recolor.rb script to recreate the original
functional HTML page but with the annotations inlined as additional
spans and modified image tags.  This does not yet handle creating the
proper parent annotation for multiple consecutive tags.
----------------------------------------
LIMITATIONS / CAVEATS: 

- The system only caters to text and images that are present in the
HTML at fetch time.  The final DOM tree that is present in the browser
may not reflect that actual one loaded in memory in a browser due to
extra javascript commands that are found in the original web page.

- It does not handle overlapping annotations (e.g.,
<tag1><tag2>X</tag2></tag1>)

- It does not handle text that is dynamically written (e.g., by a
script or included from a include file).

- To have the system save, the Firefox browser must be used.  ActiveX
controls are not (yet) supported).  A proper save directory path must
also be input by the -d command line

----------------------------------------
Specifically, the system adds the following to the input page:

Javascript event handlers for onload, onmousedown and onmouseup for
text spans.
- These are to handle the creation and movement of the annotation pane

Javascript event handlers for onclick for images.
- As above, these handle the creation and movement of the annotation pane

An extra (initially hidden) annotation pane (as <DIV> tag)
- This is to give the user the palette of annotation options

Javascript code functionality for tracking text spans' and image
annotations, background color and undo buffers 
- This is to hold the user's annotation and support simple undo and
refresh

SPAN tags around each text line to support annotation

modifies IMG tags to remove usemaps and add onclick handlers.  
- The latter to support annotation

----------------------------------------
TODO:

- Better A HREF removal
- Better span reconstruction
----------------------------------------
LOG:

$Log: README.txt,v $
Revision 1.5  2009/01/06 16:49:16  rpnlpir
Fixed annotation pane movement.

Revision 1.4  2009/01/06 04:54:06  rpnlpir
Added recolor.rb

Revision 1.3  2008/12/15 16:08:45  rpnlpir
Adds resume support.

Revision 1.2  2008/11/07 13:38:01  rpnlpir
Updated for image version.

Revision 1.1  2008/11/07 03:35:22  rpnlpir
Initial commit

