package org.apache.nutch.kairos.plugin;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.kairos.crf.CRFContext;
import org.apache.nutch.kairos.crf.ConditionalRandomFieldResult;
import org.apache.nutch.kairos.crf.ConditionalRandomFieldResultLine;
import org.apache.nutch.kairos.crf.ConditionalRandomFieldSingleton;
import org.apache.nutch.kairos.crf.Segment;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.util.LogUtil;
import org.w3c.dom.DocumentFragment;

/**
 * This is the source code for the HTML Parser extension. It tries to identify
 * conference paper metadata such as author(s), title(s), with a Conditional
 * Random Field (CRF) model. It create segments from an HTML web page. Segments
 * are list, paragraph or table entries.
 * 
 * @author Markus Haense
 */
public class KairosHtmlParseFilter implements HtmlParseFilter {

	/**
	 * Logger
	 */
	private static final Log LOG = LogFactory
			.getLog(KairosHtmlParseFilter.class.getName());

	/**
	 * Configuration
	 */
	private Configuration conf;

	private static NegativeDictionarySingleton negativeDictionary = NegativeDictionarySingleton
			.getInstance();

	static {
		try {
			negativeDictionary.read("kairos/dictionaries/negative.txt");
		} catch (Exception e) {
			// Error
			e.printStackTrace(LogUtil.getErrorStream(LOG));
		}
	}

	/**
	 * Scan the HTML document for paper metadata
	 */
	public ParseResult filter(Content content, ParseResult parseResult,
			HTMLMetaTags metaTags, DocumentFragment doc) {

		Metadata metadata = content.getMetadata();
		metadata.set("Content-Type", "text/html");

		// Get the content base URL
		String contentUrl = content.getUrl();

		// Contains the metadata
		StringBuilder indexText = new StringBuilder();

		// Parse Data
		ParseData parseData = parseResult.get(contentUrl).getData();

		// New HTML Segment Creator
		HTMLSegmentCreator htmlSegmentCreator = new HTMLSegmentCreator();

		// List of HTML segments
		List<Segment> segmentList = htmlSegmentCreator.createHTMLSegments(doc);

		// LOG.info("SEGMENT SIZE [" + segmentList.size() + "]");

		// New Conditional Random Field
		ConditionalRandomFieldSingleton crf = ConditionalRandomFieldSingleton
				.getInstance();

		// For each segment in the segment list
		for (int j = 0; j < segmentList.size(); j++) {
			// Get the current segment
			Segment currentSegment = segmentList.get(j);

			// Get the context for CRF
			List<CRFContext> context = crf.generateCRFContext(currentSegment);

			// Predict label for feature lines
			List<ConditionalRandomFieldResult> conditionalRandomFieldResults = crf
					.testing(context, false);

			if (conditionalRandomFieldResults.size() != 0) {
				// For each result from the Conditional Random Field
				for (int x = 0; x < conditionalRandomFieldResults.size(); x++) {
					// Get the conditional output probability
					// for the current result
					double currentConditionalOutputProbability = conditionalRandomFieldResults
							.get(x).getConditionalOutputProbability();

					// If the conditional output probability
					// is higher than 85%
					if (currentConditionalOutputProbability >= 0.75) {
						// Get the Conditional Random Field Testing Lines
						List<ConditionalRandomFieldResultLine> conditionalRandomFieldTestingLines = conditionalRandomFieldResults
								.get(x).getConditionalRandomFieldResultLines();

						// Contains the title words / tokens
						List<String> title = new LinkedList<String>();

						// Contains the author words / tokens
						List<String> author = new LinkedList<String>();

						// Contains the affiliation words / tokens
						List<String> affiliation = new LinkedList<String>();

						// For each CRF testing line
						for (int y = 0; y < conditionalRandomFieldTestingLines
								.size(); y++) {
							// Get the current tag
							ConditionalRandomFieldResultLine currentTag = conditionalRandomFieldTestingLines
									.get(y);

							// Get the guessed label from CRF
							String guess = currentTag.guessedLabelFromCRF;

							// double marginalProbability =
							// currentTag.marginalProbability;

							String word = currentTag.word;

							boolean isNegativeWord = negativeDictionary
									.isNegativeWord(word);

							if (isNegativeWord == true) {
								continue;
							}

							// If the guessed label equals title + some
							// heuristics
							if (guess.equals("title")) {
								// Add the word / token to the title
								title.add(word);
							}

							// If the guessed label equals author + some
							// heuristics
							if (guess.equals("author")) {
								if (currentTag.tokens[5].equals("1")
										&& !currentTag.tokens[20].equals("1")) {
									// Add the word / token to the author
									author.add(word);
								}

								if (currentTag.tokens[20].equals("1")) {
									affiliation.add(word);
								}
							}

						}

						// If we have a title and an author
						if (title.size() >= 5 && author.size() >= 2) {
							// Author
							indexText.append(Utils.listToString(author, " "));
							indexText.append(Utils.NEWLINE);

							// Title
							indexText.append(Utils.listToString(title, " "));

							if (affiliation.size() > 0) {
								indexText.append(Utils.NEWLINE);

								// Affiliation
								indexText.append(Utils.listToString(
										affiliation, " "));
							}

							// Empty line
							indexText.append(Utils.NEWLINE);
							indexText.append(Utils.NEWLINE);

							/*
							 * LOG.info("ADDING METADATA: PROB: " +
							 * currentConditionalOutputProbability +
							 * " [segment:" + j + "| result:" + x + " size:" +
							 * currentSegment.getSegmentTextSize() + "] : (" +
							 * indexText.toString());
							 */
						}
					} else {
						/*
						 * LOG.info("IGNORE METADATA: PROB: " +
						 * currentConditionalOutputProbability + " [segment:" +
						 * j + "| result:" + x + " size:" +
						 * currentSegment.getSegmentTextSize() + "] : (" +
						 * currentSegment.getSegmentTextConcatenated() +
						 * ")  URL: " + contentUrl);
						 */
					}
				}
			}
		}

		return ParseResult.createParseResult(content.getUrl(), new ParseImpl(
				indexText.toString(), parseData));
	}

	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	public Configuration getConf() {
		return this.conf;
	}
}