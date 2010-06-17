package org.apache.nutch.wikicfp;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.w3c.dom.DocumentFragment;

/**
 * This is the source code for the HTML Parser extension. It tries to extract
 * the contents of the small table on each event side from WikiCFP and add them
 * to the document being parsed.
 * 
 * @author Markus Haense
 */
public class WikicfpHtmlParseFilter implements HtmlParseFilter {

	/**
	 * Logger
	 */
	private static final Log LOG = LogFactory
			.getLog(WikicfpHtmlParseFilter.class.getName());

	/**
	 * Configuration
	 */
	private Configuration conf;

	/**
	 * Scan the HTML document looking for an event table
	 */
	public ParseResult filter(Content content, ParseResult parseResult,
			HTMLMetaTags metaTags, DocumentFragment doc) {

		// Get the content base URL
		String contentBaseUrl = content.getUrl();

		URL url = Utils.convertStringToURLObject(contentBaseUrl, false);

		if (url != null) {
			if (url.getHost().equals("www.wikicfp.com")) {
				// Get the parse result
				Parse parse = parseResult.get(contentBaseUrl);

				// Print an info to the log
				LOG.info("Start parsing [" + contentBaseUrl + "]");

				// Information Extraction Wrapper for WikiCFP
				WikicfpInformationExtractionWrapper wikicfpInformationExtractionWrapper = new WikicfpInformationExtractionWrapper();

				// Do not scrap it - wrap it ;)
				boolean success = wikicfpInformationExtractionWrapper.wrapIt(
						content, parse, metaTags, doc);

				if (success == true) {
					LOG.info("FOUND conference metadata [" + contentBaseUrl
							+ "]");
				} else {
					LOG.warn("No conference metadata [" + contentBaseUrl + "]");
				}
			} else {
				LOG.info("Cannot parse [" + contentBaseUrl
						+ "] - No WikiCFP site");
			}
		} else {
			LOG.info("URL is null [" + contentBaseUrl + "]");
		}
		
		return parseResult;
	}

	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	public Configuration getConf() {
		return this.conf;
	}
}