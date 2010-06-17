package org.apache.nutch.kairos.crf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.util.LogUtil;
import org.apache.nutch.util.StringUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

/**
 * This class reads in the annotated/colored HTML/YAML files from the coloring
 * tool. It then creates the feature lines with the associated annotated labels.
 * The feature lines are the input for the Conditional Random Field (CRF).
 * 
 * @author Markus Haense
 */
public class AnnotationTool {

	/**
	 * Logger
	 */
	public static final Log LOG = LogFactory.getLog(AnnotationTool.class);

	/**
	 * Linked list of the extracted HTML segments
	 */
	private static LinkedList<Segment> _segmentList = new LinkedList<Segment>();

	/**
	 * Extract the HTML segments from a DOM document
	 * 
	 * @param doc
	 * @return the HTML segments from the DOM document
	 */
	public static LinkedList<Segment> createSegmentsFromDOMDocument(Document doc) {

		// XPath
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();

		// HTML Segments
		String[] htmlSegments = new String[] { "//tr[not(descendant::tr)]",
				"//p[not(descendant::p)]", "//li[not(descendant::li)]" };

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

					// LOG.info("parseDOMTree START: " + currentHtmlSegment
					// + " ===============");
					// Parse the DOM tree
					parseDOMTree(node, segment, "");

					// Add a new segment to the list of HTML segments
					_segmentList.add(segment);
					// LOG.info("parseDOMTree END: " + currentHtmlSegment
					// + " ===============");
					// LOG.info("");
				}

			} catch (XPathExpressionException e) {
				// Error
				e.printStackTrace();
				e.printStackTrace(LogUtil.getErrorStream(LOG));
				return null;
			}
		}

		return _segmentList;
	}

	/**
	 * Parse the DOM tree and create HTML segments
	 * 
	 * @param node
	 * @param sIndent
	 */
	private static void parseDOMTree(Node node, Segment segment, String domPath) {
		// Get the node name
		String nodeName = node.getNodeName();

		// Construct the DOM path
		domPath += nodeName + "-";

		// Set the DOM path for the segment
		segment.setSegmentDOMPath(domPath);

		// Get the node value
		String nodeValue = node.getNodeValue();

		// LOG.info("parseDOMTree: " + nodeName + " = " + nodeValue);

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
				if (item.getName().equals("href")) {
					String href = item.getValue();

					// Set the url
					segment.setURL(href);
					break;
				}
			}
		}

		// If we encounter a br node
		if (nodeName != null && nodeName.equals("br")) {
			segment.addLineBreak();
		}

		// If we encounter a hr node
		if (nodeName != null && nodeName.equals("hr")) {
			segment.addHorizontalLine();
		}

		// If we encounter a text node and it has a value
		if (nodeName != null && nodeValue != null && nodeName.equals("#text")) {
			segment.addSegmentText(nodeValue);
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

	public static void createAnnotatedAuthorDictionary(String author) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(
					"kairos/dictionaries/annotated_authors.txt", true));

			author = author.trim();

			if (!StringUtil.isEmpty(author)) {
				out.write(author + Utils.NEWLINE);
			}
			out.close();
		} catch (IOException e) {
			// Error
			e.printStackTrace();
			e.printStackTrace(LogUtil.getErrorStream(LOG));
		}
	}

	public static void createAnnotatedAffiliationDictionary(String affiliation) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(
					"kairos/dictionaries/annotated_affiliations.txt", true));

			affiliation = affiliation.trim();

			if (!StringUtil.isEmpty(affiliation)) {
				out.write(affiliation + Utils.NEWLINE);
			}
			out.close();
		} catch (IOException e) {
			// Error
			e.printStackTrace();
			e.printStackTrace(LogUtil.getErrorStream(LOG));
		}
	}

	public static void main(String[] args) {
		System.out.println("=START= CREATING CRF TRAINING FILE");

		// Check if the training file already exists
		// If it exists then delete it
		Utils.fileExists(new File("kairos/train.data.txt"), true);

		// Check if the annotated author directory file already exists
		// If it exists then delete it
		// Utils.fileExists(new
		// File("kairos/dictionaries/annotated_authors.txt"),
		// true);

		// Golden standard input file
		/*
		 * File goldenStandard = new File(
		 * "kairos/golden-standard-input/chunk_tagged_scholarly_paper_metadata.xml"
		 * ); BufferedWriter goldenStandardWriter = null; try {
		 * goldenStandardWriter = new BufferedWriter(new FileWriter(
		 * goldenStandard)); } catch (IOException e) { // Error
		 * e.printStackTrace(); e.printStackTrace(LogUtil.getErrorStream(LOG));
		 * }
		 */

		// File writer for the feature lines /
		// training data
		Writer bwTrainingFile;
		try {
			bwTrainingFile = new BufferedWriter(new FileWriter(
					"kairos/train.data.txt", true));

			// Path to the repository directory
			File repositoryDir = new File("kairos/annotation/repository");

			// Path to the annotated repository directory
			File annotatedRepositoryDir = new File(
					"kairos/annotation/annotated_repository");

			// Filename filter for YAML files
			FilenameFilter yamlFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".yaml");
				}
			};

			// Filename filter for HTML files
			FilenameFilter htmlFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".html");
				}
			};

			// Get the annotated YAML files
			File[] yamlFiles = annotatedRepositoryDir.listFiles(yamlFilter);

			// Get the annotated HTML files
			File[] annotatedHtmlFiles = annotatedRepositoryDir
					.listFiles(htmlFilter);

			// Cleaning HTML
			Tidy tidy = new Tidy();

			// Output extensible HTML
			tidy.setXHTML(true);

			// User specified doctype
			tidy.setDocType("strict");

			// Remove presentational clutter
			tidy.setMakeClean(true);

			// Quiet - no 'Parsing X', guessed DTD or summary
			tidy.setQuiet(false);

			// Indent content of appropriate tags
			tidy.setIndentContent(true);

			// Does text/block level content effect indentation
			tidy.setSmartIndent(true);

			// Newline + indent before each attribute
			tidy.setIndentAttributes(true);

			// Draconian cleaning for Word2000
			tidy.setWord2000(true);

			// Show warnings
			tidy.setShowWarnings(false);

			// This option specifies if Tidy should
			// output tag names in upper case
			tidy.setUpperCaseTags(true);

			// This option specifies if Tidy should
			// output attribute names in upper case
			tidy.setUpperCaseAttrs(true);

			// Redirect errors to a temporary file
			// instead to print it out on the console
			try {
				File tmp = File
						.createTempFile("tidy", ".tmp", new File("tmp/"));
				tidy.setErrout(new PrintWriter(tmp));
				tmp.deleteOnExit();
			} catch (IOException e) {
				// Error
				e.printStackTrace();
				e.printStackTrace(LogUtil.getErrorStream(LOG));
			}

			// Process the YAML files
			for (int i = 0; i < yamlFiles.length; i++) {
				try {
					System.out.println("Annotated File: "
							+ annotatedHtmlFiles[i].getAbsolutePath());

					// Get the YAML file name
					String filename = yamlFiles[i].getName();

					int index = filename.lastIndexOf(".");

					if (index != -1) {
						filename = filename.substring(0, index) + ".html";
					}

					// File reader for the annotated YAML file
					BufferedReader brYamlFile = new BufferedReader(
							new FileReader(yamlFiles[i]));

					// File input stream for the annotated HTML file
					FileInputStream fisAnnotatedHtmlFile = new FileInputStream(
							annotatedRepositoryDir.getAbsolutePath() + "/"
									+ filename);

					// Create a temporary XML file for the annotated HTML file
					File tempAnnotatedXMLFile = File.createTempFile(
							"annotatedHTML", ".xml", new File("tmp/"));

					// File output stream for the annotated XML file
					FileOutputStream fosAnnotatedXMLFile = new FileOutputStream(
							tempAnnotatedXMLFile);

					// File input stream for the original HTML file
					FileInputStream fisOrginalHtmlFile = new FileInputStream(
							repositoryDir.getAbsolutePath() + "/"
									+ annotatedHtmlFiles[i].getName());

					// Create a temporary XML file for the original HTML file
					File tempOrginalXMLFile = File.createTempFile(
							"orginalHTML", ".xml", new File("tmp/"));

					// File output stream for the original XML file
					FileOutputStream fosOriginalXMLFile = new FileOutputStream(
							tempOrginalXMLFile);

					// DOM document for the annotated HTML file
					Document annotatedHtmlDOMdocument = tidy.parseDOM(
							fisAnnotatedHtmlFile, fosAnnotatedXMLFile);
					fisAnnotatedHtmlFile.close();
					fosAnnotatedXMLFile.close();
					tempAnnotatedXMLFile.delete();

					// DOM document for the original HTML file
					Document originalHtmlDOMdocument = tidy.parseDOM(
							fisOrginalHtmlFile, fosOriginalXMLFile);
					fisOrginalHtmlFile.close();
					fosOriginalXMLFile.close();
					tempOrginalXMLFile.delete();

					// <Text, annotated label>
					HashMap<String, String> segmentHashMap = new HashMap<String, String>();

					// The current line in the YAML file
					String lineInYamlFile;
					int count = 0;

					// Read in the YAML file till EOF
					while ((lineInYamlFile = brYamlFile.readLine()) != null) {
						// Skip comments
						if (!lineInYamlFile.startsWith("#")) {
							// Omit leading and trailing whitespace
							lineInYamlFile = lineInYamlFile.trim();

							// Process spans
							if (lineInYamlFile.startsWith("-")) {
								// Split the current line in the YAML file
								String[] split = lineInYamlFile.split(" ");

								// If we have a span and a label
								if (split.length == 2) {
									// Increment count
									count++;

									// Get the label
									String label = split[1];
									label = label.trim();

									// Skip "na" labels
									if (!label.equals("na")
											&& label.length() > 0) {
										// XPath
										XPathFactory factory = XPathFactory
												.newInstance();
										XPath xPath = factory.newXPath();

										try {
											// Find the spans with the
											// corresponding id
											XPathExpression xPathExpression = xPath
													.compile("//span[@id='"
															+ count
															+ "']//text()");

											// Get the text of the span
											String text = xPathExpression
													.evaluate(
															annotatedHtmlDOMdocument)
													.trim();

											// Print out some info
											System.out
													.println((count + 1)
															+ " ["
															+ annotatedHtmlFiles[i]
																	.getName()
															+ "] Text: "
															+ StringEscapeUtils
																	.escapeHtml(text)
															+ " [label: "
															+ label + "]");

											/*
											 * // Create golden standard input
											 * if (goldenStandardWriter != null)
											 * { if (!StringUtil.isEmpty(label)
											 * && !StringUtil .isEmpty(text) &&
											 * text.length() > 1) { String
											 * textNew = text.trim();
											 * 
											 * goldenStandardWriter .write("<" +
											 * label + ">" + StringEscapeUtils
											 * .escapeXml(StringEscapeUtils
											 * .escapeHtml(textNew)) + "</" +
											 * label + ">" + Utils.NEWLINE);
											 * goldenStandardWriter.flush(); } }
											 */

											// Create a new segment
											// and add the span text
											Segment segment = new Segment();
											segment.addSegmentText(text);

											if (label.equals("affiliation")) {
												label = "";
												// createAnnotatedAffiliationDictionary(text);
											}

											if (label.equals("author")) {
												// createAnnotatedAuthorDictionary(text);
											}

											if (label
													.equals("authorWithAffiliation")) {
												label = "author";
											}

											if (segment.getSegmentTextSize() > 0) {
												text = segment
														.getSegmentText(0);

												// Put the segment in a hash map
												// with associated annotated
												// label
												segmentHashMap.put(text, label);
											}
										} catch (XPathExpressionException e) {
											// Error
											e.printStackTrace();
											e.printStackTrace(LogUtil
													.getErrorStream(LOG));
										}
									}
								}
							}
						}
					} // end of while read line in YAML

					// Close the file reader for the YAML file
					brYamlFile.close();

					// goldenStandardWriter.write(Utils.NEWLINE);
					// goldenStandardWriter.flush();

					// Create a conditional random field
					ConditionalRandomFieldSingleton crf = ConditionalRandomFieldSingleton
							.getInstance();

					// Find the segments in the original HTML file
					// System.out.println();
					// System.out.println("== FIND SEGMENTS ==");
					LinkedList<Segment> segments = createSegmentsFromDOMDocument(originalHtmlDOMdocument);

					// System.out.println();
					// System.out.println("== SEGMENTS SIZE: " + segments.size()
					// + " ==");

					// For each segment
					for (Segment currentSegment : segments) {
						// For each text in the segment
						for (int k = 0; k < currentSegment.getSegmentTextSize(); k++) {
							// Get the current segment text
							String currentSegmentText = currentSegment
									.getSegmentText(k);

							// If it is a HTML break line tag
							// or a HTML horizontal line tag
							if (currentSegmentText.equals("\\\\")) {
								// do nothing
							} else {
								// If the hash map contains an annotated text
								if (segmentHashMap
										.containsKey(currentSegmentText)) {

									// Get the annotated label
									String label = segmentHashMap
											.get(currentSegmentText);

									// Set the annotated label
									currentSegment
											.setSegmentTextLabel(k, label);

									// System.out.println("++++++++ Text: "
									// + currentSegmentText + " [LABEL: "
									// + label + "]");
								} else {
									// System.out.println("++++++++ Text: "
									// + currentSegmentText + " [NO LABEL]");
								}
							}
						}

						// Get the feature lines for CRF++
						List<CRFContext> context = crf
								.generateCRFContextFromAnnotation(currentSegment);

						// System.out.println("CONTEXT SIZE " + context.size());

						Set<String> set = new HashSet<String>();

						try {
							for (CRFContext crfContext : context) {
								// Write the feature lines
								String contextLine = Utils.listToString(
										crfContext.getContext(), Utils.NEWLINE);

								if (!StringUtil.isEmpty(contextLine)) {
									if (!set.contains(contextLine)) {
										set.add(contextLine);
										// Write the features to
										// the training file
										bwTrainingFile.write(contextLine);
										bwTrainingFile.write(Utils.NEWLINE
												+ Utils.NEWLINE);
										bwTrainingFile.flush();

										/*
										System.out.println("\n\nCONTEXT \n"
												+ contextLine + "\n\n");
										Scanner in = new Scanner(System.in);

										// Get the answer from the command line
										String answer = in.nextLine();
										*/
								
									}
									else {
										System.out.println("COOOONTAINS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
									}
								}
							}

						} catch (IOException e) {
							// Error
							e.printStackTrace();
							e.printStackTrace(LogUtil.getErrorStream(LOG));
						}
					} // end for each segment
				} catch (IOException e) {
					// Error
					e.printStackTrace();
					e.printStackTrace(LogUtil.getErrorStream(LOG));
				}
			} // end for each YAML file

			// Close the file writer
			// for the training file
			bwTrainingFile.close();

		} catch (IOException e) {
			// training data write
			e.printStackTrace();
			e.printStackTrace(LogUtil.getErrorStream(LOG));
		}

		/*
		 * try { goldenStandardWriter.close(); } catch (IOException e) { //
		 * Error e.printStackTrace(LogUtil.getErrorStream(LOG));
		 * e.printStackTrace(); }
		 */

		System.out.println("=DONE= CREATING CRF TRAINING FILE");
	}
}