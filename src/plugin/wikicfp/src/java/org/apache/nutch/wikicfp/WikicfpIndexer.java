package org.apache.nutch.wikicfp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.indexer.lucene.LuceneWriter;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Parse;

/**
 * The following is the code for the Indexing Filter extension. If the document
 * being indexed had a event flag this extension adds a Lucene text fields to
 * the index with the content of the meta data (small event table on WikiCFP).
 * 
 * @author Markus Haense
 */
public class WikicfpIndexer implements IndexingFilter {

	/**
	 * Logger
	 */
	public static final Log LOG = LogFactory.getLog(WikicfpIndexer.class
			.getName());

	/**
	 * Configuration
	 */
	private Configuration conf;

	public NutchDocument filter(NutchDocument doc, Parse parse,
			org.apache.hadoop.io.Text url, CrawlDatum datum, Inlinks inlinks)
			throws IndexingException {
		// Add the WikiCFP URL
		doc.add("url", url.toString());

		// Get the meta data
		Metadata metaData = parse.getData().getContentMeta();

		// Get the event flag that indicates if we are dealing with an event
		String isEventFlag = parse.getData().getMeta("EVENT");

		// If it is an event
		if (isEventFlag != null && isEventFlag.equals("1")) {
			// Iterate over all meta data entries
			for (String currentName : metaData.names()) {
				// Get the current value associated to the current name
				String currentValue = metaData.get(currentName);

				// If the current value from the meta tag is not null
				// and is a description, title, name, when etc.
				if (currentValue != null
						&& currentValue.length() > 0
						&& (currentName.equals("Description")
								|| currentName.equals("Title") || currentName
								.equals("Name")) || currentName.equals("When")
						|| currentName.equals("Where")
						|| currentName.equals("Notification Due")
						|| currentName.equals("Final Version Due")
						|| currentName.equals("Link")) {

					// Add the meta data to the index
					doc.add(currentName, Utils
							.getCleanTextFromHTML(currentValue));

					LOG.info("Indexing [name: " + currentName + "] [value: "
							+ currentValue + "]");
				}
			}

			return doc;
		} else {
			LOG.info("No event for indexing");
			return null;
		}
	}

	public void addIndexBackendOptions(org.apache.hadoop.conf.Configuration conf) {
		LuceneWriter.addFieldOptions("url", LuceneWriter.STORE.YES,
				LuceneWriter.INDEX.UNTOKENIZED, conf);

		LuceneWriter.addFieldOptions("Description", LuceneWriter.STORE.YES,
				LuceneWriter.INDEX.UNTOKENIZED, conf);

		LuceneWriter.addFieldOptions("Title", LuceneWriter.STORE.YES,
				LuceneWriter.INDEX.UNTOKENIZED, conf);

		LuceneWriter.addFieldOptions("Name", LuceneWriter.STORE.YES,
				LuceneWriter.INDEX.UNTOKENIZED, conf);

		LuceneWriter.addFieldOptions("When", LuceneWriter.STORE.YES,
				LuceneWriter.INDEX.UNTOKENIZED, conf);

		LuceneWriter.addFieldOptions("Where", LuceneWriter.STORE.YES,
				LuceneWriter.INDEX.UNTOKENIZED, conf);

		LuceneWriter.addFieldOptions("Paper Registration Due",
				LuceneWriter.STORE.YES, LuceneWriter.INDEX.UNTOKENIZED, conf);

		LuceneWriter.addFieldOptions("Submission Deadline",
				LuceneWriter.STORE.YES, LuceneWriter.INDEX.UNTOKENIZED, conf);

		LuceneWriter.addFieldOptions("Notification Due",
				LuceneWriter.STORE.YES, LuceneWriter.INDEX.UNTOKENIZED, conf);

		LuceneWriter.addFieldOptions("Final Version Due",
				LuceneWriter.STORE.YES, LuceneWriter.INDEX.UNTOKENIZED, conf);

		LuceneWriter.addFieldOptions("Link", LuceneWriter.STORE.YES,
				LuceneWriter.INDEX.TOKENIZED, conf);	
	}

	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	public Configuration getConf() {
		return this.conf;
	}
}