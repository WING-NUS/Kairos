package org.apache.nutch.kairos.crf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TitleDictionarySingleton {

	/**
	 * Log writer
	 */
	public static final Log LOG = LogFactory
			.getLog(TitleDictionarySingleton.class);

	/**
	 * Instance of the Singleton.
	 */
	private static TitleDictionarySingleton _instance;

	/**
	 * Lock object to ensure we get an Singelton object when we work in a
	 * multithreaded environment.
	 */
	private static final Object _classLock = TitleDictionarySingleton.class;

	/**
	 * Path to the name dictionary file
	 */
	private static final String _titleWordFrequencyDictionaryPath = "kairos/dictionaries/titleWordFrequency.txt";

	/**
	 * Hash map of the author names
	 */
	private static HashMap<String, Integer> _titleDictionary2 = new HashMap<String, Integer>();

	private TitleDictionarySingleton() {
		// Singleton
	}

	/**
	 * Singleton
	 * 
	 * @return
	 * @throws Exception
	 */
	public static TitleDictionarySingleton getInstance() throws Exception {
		synchronized (_classLock) {
			if (_instance == null) {
				boolean titleWordFrequencyFileExists = (new File(
						_titleWordFrequencyDictionaryPath)).exists();

				if (titleWordFrequencyFileExists == true) {
					try {
						// Read in the title word frequency dicitionary
						BufferedReader br = new BufferedReader(new FileReader(
								_titleWordFrequencyDictionaryPath));

						// Contains the current line
						String line;

						// Read in the current line
						while ((line = br.readLine()) != null) {
							// If the current line has characthers
							if (line.length() > 0) {
								// Remove leading and trailing whitespaces
								line = line.trim();

								if (line.length() > 0) {
									String[] splitted = line.split(" ");

									if (splitted.length > 1) {
										int frequency = 0;

										try {
											// Get the marginal Probability
											frequency = Integer
													.parseInt(splitted[0]);
										} catch (NumberFormatException e) {
											LOG
													.error("ConditionalRandomFieldSingelton: testing() NumberFormatException [marginal probability]: "
															+ e.getMessage());
										}

										String title = splitted[1].trim();

										if (title.length() > 1
												&& frequency > 1000) {

											try {

												Integer.parseInt(title);
											} catch (NumberFormatException e) {
												// Add the current token to the
												// hash map

												_titleDictionary2.put(
														StringEscapeUtils
																.escapeHtml(
																		title)
																.toLowerCase(),
														frequency);
											}
										}
									}
								}
							}
						}
						// Close the stream
						br.close();

						_instance = new TitleDictionarySingleton();
					} catch (IOException e) {
						_instance = null;
					}
				} else {
					LOG
							.error("TITLE WORD FREQUENCY DICTIONARY DOES NOT EXISTS");
				}
			}

			// Return the singleton
			return _instance;
		}
	}

	public Set<String> iterator() {
		return _titleDictionary2.keySet();
	}

	public int get(String key) {
		return _titleDictionary2.get(key);
	}

	public boolean contains(String titleWord) {
		// Unescape HTML
		titleWord = StringEscapeUtils.escapeHtml(titleWord);

		// Lowercase and trim the author name
		titleWord = titleWord.trim().toLowerCase();

		// Check if the hash set contains the author name
		return _titleDictionary2.containsKey(titleWord);
	}

	public static void main(String[] args) {
		boolean titleWordFrequencyFileExists = (new File(
				"kairos/dictionaries/titleWordFrequency.txt")).exists();

		if (titleWordFrequencyFileExists == true) {
			try {
				TitleDictionarySingleton a = TitleDictionarySingleton
						.getInstance();

				Set<String> keySet = a.iterator();

				try {
					if (keySet.size() > 0) {
						BufferedWriter out = new BufferedWriter(new FileWriter(
								"kairos/dictionaries/titleWordFrequency.txt"));

						for (String string : keySet) {
							out.write(a.get(string) + " " + string
									+ System.getProperty("line.separator"));
						}

						out.close();
					}
				} catch (IOException e) {
					LOG
							.error("TitleDictionarySingleton: IOException finalize()");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}