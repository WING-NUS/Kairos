package org.apache.nutch.wikicfp;

// JDK imports
import java.util.Enumeration;
import java.util.Properties;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.util.LogUtil;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.NodeList;

/**
 * This is the source code for the WikiCFP Information Extraction Wrapper. It
 * tries to grab the contents of the small table on each event side from WikiCFP
 * with XPath.
 * 
 * @author Markus Haense
 * 
 */
public class WikicfpInformationExtractionWrapper {

	// The event meta data attributes
	private static final String META_EVENT_DESCRIPTION = "Description";

	private static final String META_EVENT_TITLE = "Title";

	private static final String META_EVENT_NAME = "Name";

	private static final String META_EVENT_WHEN = "When";

	private static final String META_EVENT_WHERE = "Where";

	private static final String META_EVENT_PAPER_REGISTRATION_DUE = "Paper Registration Due";

	private static final String META_EVENT_SUBMISSION_DEADLINE = "Submission Deadline";

	private static final String META_EVENT_NOTIFICATION_DUE = "Notification Due";

	private static final String META_EVENT_FINAL_VERSION_DUE = "Final Version Due";

	private static final String META_EVENT_LINK = "Link";

	// XPATH Queries
	private static final String XPATH_EXPRESSION_EVENT_TITLE = "/HTML/HEAD/TITLE";

	private static final String XPATH_EXPRESSION_EVENT_NAME = "//H2/text()";

	private static final String XPATH_EXPRESSION_EVENT_LINK = "//TD[normalize-space(text()) = 'link:']/A/text()";

	private static final String EVENT_URL_PATTERN = "http://www.wikicfp.com/cfp/servlet/event.showcfp?eventid=";

	/**
	 * Logger
	 */
	private static final Log LOG = LogFactory
			.getLog(WikicfpHtmlParseFilter.class.getName());

	/**
	 * 
	 * @param content
	 * @param parse
	 * @param metaTags
	 * @param doc
	 * @return
	 */
	public boolean wrapIt(Content content, Parse parse, HTMLMetaTags metaTags,
			DocumentFragment doc) {

		// Get the base URL
		String contentBaseUrl = content.getUrl();

		// We only want to parse events
		if (contentBaseUrl.startsWith(EVENT_URL_PATTERN) == true) {

			// Get the meta data
			Metadata metaData = parse.getData().getContentMeta();

			// LOG.info("BEGINN XPATH");
			// Utils.printDOMTree(doc, "");
			// LOG.info("END XPATH");

			// Print an info to the log that the current url was an event
			LOG.info("Found conference metadata: [" + contentBaseUrl + "]");

			// Print an info to the log that we
			// start with the information extraction now
			LOG.info("Start Information Extraction");

			// XPath
			XPathFactory factory = XPathFactory.newInstance();
			XPath xPath = factory.newXPath();

			try {
				// XPath expression for the event title
				XPathExpression xPathExpressionEventTitle = xPath
						.compile(XPATH_EXPRESSION_EVENT_TITLE);

				// XPath expression for the event name
				XPathExpression xPathExpressionEventName = xPath
						.compile(XPATH_EXPRESSION_EVENT_NAME);

				// XPath expression for the event link
				XPathExpression xPathExpressionEventLink = xPath
						.compile(XPATH_EXPRESSION_EVENT_LINK);

				// XPath expression for the event dates
				NodeList nodes = (NodeList) XPathFactory
						.newInstance()
						.newXPath()
						.evaluate(
								"//TR[@bgcolor]/TH/text() | //TR[@bgcolor]/TD/text()",
								doc, XPathConstants.NODESET);

				/*
				 * Utils.printDOMTree(o.item(curNode), ""); }
				 */

				// Print an info to the log that we process the meta tags now
				LOG.info("Processing meta tags [" + contentBaseUrl + "]");

				// Check if there is a meta tag with description already
				Properties generalMetaTags = metaTags.getGeneralTags();

				for (Enumeration<?> tagNames = generalMetaTags.propertyNames(); tagNames
						.hasMoreElements();) {
					if (tagNames.nextElement().equals("description")) {
						String description = generalMetaTags
								.getProperty("description");

						// Add description
						metaData
								.set(META_EVENT_DESCRIPTION, description.trim());
					}
				}

				LOG.info("ADDING META DATA FOR [" + contentBaseUrl + "]");

				// Set the event title
				String eventTitle = xPathExpressionEventTitle.evaluate(doc)
						.trim();
				metaData.set(META_EVENT_TITLE, eventTitle);
				LOG.info("ADDING META DATA [" + eventTitle + "]");

				// Set the event name
				String eventName = xPathExpressionEventName.evaluate(doc)
						.trim();
				metaData.set(META_EVENT_NAME, eventName);
				LOG.info("ADDING META DATA [" + eventName + "]");

				// Set the event link
				String eventLink = xPathExpressionEventLink.evaluate(doc)
						.trim();

				metaData.set(META_EVENT_LINK, eventLink);
				LOG.info("ADDING META DATA [" + eventLink + "]");

				// Set the event dates
				for (int curNode = 0; curNode < nodes.getLength(); curNode++) {

					String currentDateField = nodes.item(curNode)
							.getNodeValue().trim();
					String currentDateValue = nodes.item(++curNode)
							.getNodeValue().trim();

					// if & else, because Java did not support switch on Strings
					// =(
					// Hopefully in Java 7 it is implemented =)
					if (currentDateField.equals("When:")) {
						// Set the event when date
						metaData.set(META_EVENT_WHEN, currentDateValue);
						LOG.info("ADDING META DATA [" + currentDateValue + "]");
					} else if (currentDateField.equals("Where:")) {
						// Set the event where date
						metaData.set(META_EVENT_WHERE, currentDateValue);
						LOG.info("ADDING META DATA [" + currentDateValue + "]");
					} else if (currentDateField
							.equals("Paper Registration Due:")) {
						// Set the event paper registration due date
						metaData.set(META_EVENT_PAPER_REGISTRATION_DUE,
								currentDateValue);
						LOG.info("ADDING META DATA [" + currentDateValue + "]");
					} else if (currentDateField.equals("Submission Deadline:")) {
						// Set the event submission deadline date
						metaData.set(META_EVENT_SUBMISSION_DEADLINE,
								currentDateValue);
						LOG.info("ADDING META DATA [" + currentDateValue + "]");
					} else if (currentDateField.equals("Notification Due:")) {
						// Set the event notification date
						metaData.set(META_EVENT_NOTIFICATION_DUE,
								currentDateValue);
					} else if (currentDateField.equals("Final Version Due:")) {
						// Set the event notification date
						metaData.set(META_EVENT_FINAL_VERSION_DUE,
								currentDateValue);
						LOG.info("ADDING META DATA [" + currentDateValue + "]");
					} else {
						LOG.warn("NO META DATA AVAILABLE!");
					}
				}

							
				/*
				// Get the outlinks of the current page
				Outlink[] outlinks = parse.getData().getOutlinks();

				// For each outlink
				for (int i = 0; i < outlinks.length; i++) {
					// Get the current outlink
					String currentOutlink = outlinks[i].getToUrl();

					// If it is not a WikiCFP link
					if (!currentOutlink.equals("www.wikicfp.com")) {
						try {
							// Create a new feature for the Maximum Entropy
							// Classifier
							MaximumEntropyFeatureExtractor createFeature = new MaximumEntropyFeatureExtractor();

							// Create the predicates for the
							// Maximum Entropy Classifier
							String predicates = createFeature
									.createMaximumEntropyFeatureLineForURL(outlinks[i]
											.getToUrl());

							// Get the Maximum Entropy Classifier
							MaximumEntropyClassifier maximumEntropyConferenceURLClassifier = MaximumEntropyClassifier
									.getInstance();

							// Predict the probability of the predicates
							double predicted = maximumEntropyConferenceURLClassifier
									.predict(predicates);

							// If the predicates are higher or equal than 75%
							if (predicted >= 0.75) {
								// Set the outlinks in the meta data
								metaData.set("Outlink" + i, currentOutlink);
								LOG.info("ADD OUTLINK [" + contentBaseUrl
										+ "] [" + currentOutlink
										+ "] (P +conference)=" + predicted);
							} else {
								// If the probability is less than 74.9%
								// => discard the outlink
								LOG.info("DISCARD OUTLINK [" + currentOutlink
										+ "] (P +conference)=" + predicted);
							}
						} catch (Exception e) {
							LOG.error("Exception Maxent: " + e.getMessage());
						}
					}
				}
				*/

				// Set an flag that we found an event
				metaData.set("EVENT", "1");

				return true;
			} catch (XPathExpressionException ex) {
				ex.printStackTrace(LogUtil.getFatalStream(LOG)); 
				return false;
			}
		} else {
			// Print an info to the log that the current url was not an event
			LOG.info("NO EVENT [" + contentBaseUrl + "]");
			return false;
		}
	}
}