package org.apache.nutch.kairos.plugin;

import java.util.LinkedList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.kairos.crf.Segment;
import org.apache.nutch.util.LogUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Markus Haense
 */
public class HTMLSegmentCreator {

	/**
	 * Logger
	 */
	private static final Log LOG = LogFactory.getLog(HTMLSegmentCreator.class
			.getName());

	public List<Segment> createHTMLSegments(DocumentFragment doc) {

		List<Segment> segmentList = new LinkedList<Segment>();

		// XPath
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();

		String[] htmlSegments = new String[] { "//TR[not(descendant::TR)]",
				"//P[not(descendant::P)]", "//LI[not(descendant::LI)]" };

		// For each HTML Segment
		for (int x = 0; x < htmlSegments.length; x++) {
			// Get the current HTML segment
			String currentHtmlSegment = htmlSegments[x];

			try {
				// Nodes of the HTML segments
				NodeList nodeList = (NodeList) xPath.evaluate(
						currentHtmlSegment, doc, XPathConstants.NODESET);

				// For each node in the nodes list
				for (int i = 0; i < nodeList.getLength(); i++) {
					// Get the current node from the node list
					Node node = nodeList.item(i);

					// Save the root node of the segment
					Segment segment = new Segment();

					// Parse the DOM tree
					parseDOMTree(node, segment, "");

					// Add a new segment to the list of HTML segments
					segmentList.add(segment);
				}

			} catch (XPathExpressionException e) {
				e.printStackTrace(LogUtil.getErrorStream(LOG));
				return null;
			}
		}

		return segmentList;
	}

	/**
	 * Parse the DOM tree and create HTML segments
	 * 
	 * @param node
	 * @param sIndent
	 */
	private void parseDOMTree(Node node, Segment segment, String domPath) {
		// Get the node name
		String nodeName = node.getNodeName();

		// Construct the DOM path
		domPath += nodeName + "-";

		// Set the DOM path for the segment
		segment.setSegmentDOMPath(domPath);

		// Get the node value
		String nodeValue = node.getNodeValue();

		// Access to all attributes as a map
		NamedNodeMap attrributes = node.getAttributes();

		// If we have attributes
		if (attrributes != null) {
			// Get the attribute count
			int attributeCount = attrributes.getLength();

			for (int i = 0; i < attributeCount; i++) {
				// Get the current attribute
				Attr item = (Attr) attrributes.item(i);

				// If we have an href attribute save the URL
				if (item.getName().equals("HREF")) {
					String href = item.getValue();

					// Set the url
					segment.setURL(href);
					break;
				}
			}
		}

		// If we encounter a br node
		if (nodeName != null && nodeName.equals("BR")) {
			segment.addLineBreak();
		}

		// If we encounter a hr node
		if (nodeName != null && nodeName.equals("HR")) {
			segment.addHorizontalLine();
		}

		// If we encounter a text node and it has a value
		if (nodeName != null && nodeValue != null && nodeName.equals("#text")) {
			segment.addSegmentText(nodeValue + " , "); // Thang v110101: add "," separator
		}

		// Get the next child node
		Node child = node.getFirstChild();

		// If we have a child node
		while (child != null) {
			// Recursive
			parseDOMTree(child, segment, domPath);
			child = child.getNextSibling();
		}
	}
}
