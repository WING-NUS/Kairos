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

/**
 * DBLP Author Dictionary for feature selection. The class itself is implemented
 * with the so known save multithreaded Singleton Pattern.
 * 
 * @author Markus Haense
 */
public class AuthorDictionarySingleton {

	/**
	 * Log writer
	 */
	public static final Log LOG = LogFactory
			.getLog(AuthorDictionarySingleton.class);

	/**
	 * Instance of the Singleton.
	 */
	private static AuthorDictionarySingleton _instance;

	/**
	 * Lock object to ensure we get an Singelton object when we work in a
	 * multithreaded environment.
	 */
	private static final Object _classLock = AuthorDictionarySingleton.class;

	/**
	 * Path to the name dictionary file
	 */
	private static final String _authorDictionaryPath = "kairos/dictionaries/names.txt";

	/**
	 * Hash set of the author names
	 */
	private static HashSet<String> _authorDictionary = new HashSet<String>();

	private AuthorDictionarySingleton() {
		// Singleton
	}

	/**
	 * Singleton
	 * 
	 * @return
	 * @throws Exception
	 */
	public static AuthorDictionarySingleton getInstance() throws Exception {
		synchronized (_classLock) {
			if (_instance == null) {
				boolean authorDictionaryFileExists = (new File(
						_authorDictionaryPath)).exists();


				if (authorDictionaryFileExists == true) {
					try {
						// Read in the author dicitionary
						BufferedReader br = new BufferedReader(new FileReader(
								_authorDictionaryPath));

						// Contains the current line
						String line;

						// Read in the current line
						while ((line = br.readLine()) != null) {
							// If the current line has characthers
							if (line.length() > 0) {
								// Remove leading and trailing whitespaces
								line = line.trim();

								for (String currentName : line.split(" ")) {
									currentName = currentName.trim();

									// If the current token has more or two
									// characters and the first character of the
									// current token is uppercased
									if (currentName.length() >= 2) {
											// Add the current token to the hash
											// map
											_authorDictionary
													.add(StringEscapeUtils
															.escapeHtml(currentName));
									}
								}
							}
						}
						// Close the stream
						br.close();

						_instance = new AuthorDictionarySingleton();
					} catch (IOException e) {
						_instance = null;
					}
				} else {
					LOG.error("AUTHOR DICTIONARY DOES NOT EXISTS");
				}
			}

			// Return the singleton
			return _instance;
		}
	}

	public Iterator<String> iterator() {
		return _authorDictionary.iterator();
	}

	public boolean contains(String authorName) {
		// Unescape HTML
		authorName = StringEscapeUtils.escapeHtml(authorName);

		// Lowercase and trim the author name
		authorName = authorName.trim();

		// Check if the hash set contains the author name
		return _authorDictionary.contains(authorName);
	}

	public static void main(String[] args) {
		boolean authorDictionaryFileExists = (new File(
				"kairos/dictionaries/names.txt")).exists();

		if (authorDictionaryFileExists == true) {
			try {
				AuthorDictionarySingleton a = AuthorDictionarySingleton
						.getInstance();

				Iterator<String> it = a.iterator();

				try {
					if (it.hasNext()) {
						BufferedWriter out = new BufferedWriter(new FileWriter(
								"kairos/dictionaries/names.txt"));

						while (it.hasNext()) {
							String currentName = it.next();
							out.write(currentName
									+ System.getProperty("line.separator"));
						}

						out.close();
					}
				} catch (IOException e) {
					LOG
							.error("AuthorDictionarySingleton: IOException finalize()");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}