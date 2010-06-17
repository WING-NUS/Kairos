package org.apache.nutch.kairos.maxent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.util.LogUtil;

public class MaximumEntropySearchEngineWrapperInputFileCreator {
	public static final Log LOG = LogFactory
			.getLog(MaximumEntropySearchEngineWrapperInputFileCreator.class);

	public static void main(String argv[]) {
		// WikiCFP index directory path
		Path index = new Path("crawl.dirs/crawl.wikicfp/index");

		// Check if the annotated author directory file already exists
		boolean wikicfpIndexExists = (new File(index.toString())).exists();

		// If the annotated author directory file exists
		if (wikicfpIndexExists == true) {

			// Index reader object
			IndexReader ir = null;

			// Index reader object
			try {
				ir = IndexReader.open(index.toString());
			} catch (CorruptIndexException e) {
				LOG.info("fuck1");
				e.printStackTrace(LogUtil.getErrorStream(LOG));
			} catch (IOException e) {
				LOG.info("fuck2");
				e.printStackTrace(LogUtil.getErrorStream(LOG));
			}

			LOG.info("yeah1");
			// Path where to save the input file format
			// for the Search Engine Wrapper
			Path searchEngineWrapperInputFile = new Path(
					"maxent/searchEngineWrapperInputFile.txt");

			// Writes the input file for the Search Engine Wrapper
			BufferedWriter out = null;

			try {
				out = new BufferedWriter(new FileWriter(
						searchEngineWrapperInputFile.toString()));
			} catch (IOException e) {
				LOG.info("fuck3");
				e.printStackTrace(LogUtil.getErrorStream(LOG));
			}

			// For each document in the index
			for (int i = 0; i < ir.maxDoc(); i++) {

				Document doc = null;

				try {
					if (!ir.isDeleted(i)) {
						// Get the current doc
						doc = ir.document(i);
					}
				} catch (CorruptIndexException e) {
					LOG.info("fuck4");
					e.printStackTrace(LogUtil.getErrorStream(LOG));
				} catch (IOException e) {
					LOG.info("fuck5");
					e.printStackTrace(LogUtil.getErrorStream(LOG));
				}

				// If we have a valid document from the index
				if (doc != null) {
					// Get the tokenized Outlink from the document
					String link = doc.get("Link");

					try {
						if (link != null) {
							String[] segments = Utils.segmentURL(link);

							URL urlObject = Utils.convertStringToURLObject(
									link, false);

							if (urlObject != null) {
								String host = urlObject.getHost();

								LOG.info(java.util.Arrays.toString(segments));
								LOG.info(host);

								// Write the input file for the Search Engine
								// Wrapper
								out.write("hehe: [inurl:"
										+ Utils.arrayToString(segments, " ")
										+ "] filetype:html -site:" + host
										+ System.getProperty("line.separator"));
								out.flush();
							}
						}
					} catch (IOException e) {
						e.printStackTrace(LogUtil.getErrorStream(LOG));
					}
				} else {
					LOG.info("Document is null");
				}
			}

			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					LOG.info("fuck666");
					e.printStackTrace(LogUtil.getErrorStream(LOG));
				}
			}

			if (ir != null) {
				try {
					ir.close();
				} catch (IOException e) {
					LOG.info("fuck777");
					e.printStackTrace(LogUtil.getErrorStream(LOG));
				}
			}
		} else {
			LOG.info("INDEX DOES NOT EXIST");
		}
	}
}