package org.apache.nutch.kairos.crf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.util.StringUtil;

public class AffiliationDictionarySingleton {

	/**
	 * Log writer
	 */
	public static final Log LOG = LogFactory
			.getLog(AuthorDictionarySingleton.class);

	/**
	 * Instance of the Singleton.
	 */
	private static AffiliationDictionarySingleton _instance;

	/**
	 * Lock object to ensure we get an Singelton object when we work in a
	 * multithreaded environment.
	 */
	private static final Object _classLock = AffiliationDictionarySingleton.class;

	/**
	 * Path to the name dictionary file
	 */
	private static final String _affiliationDictionaryPath = "kairos/dictionaries/affiliations.txt";

	/**
	 * Hash set of the author names
	 */
	private static HashSet<String> _affiliationsDictionary = new HashSet<String>();

	private AffiliationDictionarySingleton() {
		// Singleton
	}

	/**
	 * Singleton
	 * 
	 * @return
	 * @throws Exception
	 */
	public static AffiliationDictionarySingleton getInstance() throws Exception {
		synchronized (_classLock) {
			if (_instance == null) {
				boolean authorDictionaryFileExists = (new File(
						_affiliationDictionaryPath)).exists();

				if (authorDictionaryFileExists == true) {
					try {
						// Read in the affiliation dictionary
						BufferedReader br = new BufferedReader(new FileReader(
								_affiliationDictionaryPath));

						// Contains the current line
						String line;

						// Read in the current line
						while ((line = br.readLine()) != null) {
							// If the current line has characters
							if (line.length() > 0) {
								// Remove leading and trailing whitespaces
								line = line.trim();

								for (String currentAffiliation : line
										.split(" ")) {
									currentAffiliation = currentAffiliation
											.trim();

									// If the current token has more or two
									// characters and the first character of the
									// current token is uppercased
									if (!StringUtil.isEmpty(currentAffiliation)) {
								
											// Add the current token to the hash
											// map
											_affiliationsDictionary
													.add(StringEscapeUtils
															.escapeHtml(currentAffiliation));
									
									}
								}
							}
						}
						// Close the stream
						br.close();

						_instance = new AffiliationDictionarySingleton();
					} catch (IOException e) {
						_instance = null;
					}
				} else {
					LOG.error("AFFILIATION DICTIONARY DOES NOT EXISTS");
				}
			}

			// Return the singleton
			return _instance;
		}
	}

	public Iterator<String> iterator() {
		return _affiliationsDictionary.iterator();
	}

	public boolean contains(String affiliationName) {
		// Unescape HTML
		affiliationName = StringEscapeUtils.escapeHtml(affiliationName);

		affiliationName = affiliationName.trim();

		// Check if the hash set contains the author name
		return _affiliationsDictionary.contains(affiliationName);
	}

	public static void main(String[] args) {
		boolean affiliationDictionaryFileExists = (new File(
				"kairos/dictionaries/affiliations.txt")).exists();

		if (affiliationDictionaryFileExists == true) {
			try {
				AffiliationDictionarySingleton a = AffiliationDictionarySingleton
						.getInstance();

				Iterator<String> it = a.iterator();

				try {
					if (it.hasNext()) {
						BufferedWriter out = new BufferedWriter(new FileWriter(
								"kairos/dictionaries/affiliations.txt"));

						while (it.hasNext()) {
							String currentName = it.next();
							out.write(currentName
									+ System.getProperty("line.separator"));
						}

						out.close();
					}
				} catch (IOException e) {
					LOG
							.error("AffiliationDictionarySingleton: IOException finalize()");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
