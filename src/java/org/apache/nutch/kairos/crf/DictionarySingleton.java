package org.apache.nutch.kairos.crf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.util.StringUtil;

public class DictionarySingleton {

	/**
	 * Log writer
	 */
	public static final Log LOG = LogFactory
			.getLog(AuthorDictionarySingleton.class);

	/**
	 * Instance of the Singleton.
	 */
	private static DictionarySingleton _instance;

	/**
	 * Lock object to ensure we get an Singelton object when we work in a
	 * multithreaded environment.
	 */
	private static final Object _classLock = DictionarySingleton.class;

	private HashMap<String, HashSet<String>> dictionaries = new HashMap<String, HashSet<String>>();

	private DictionarySingleton() {
		// Singleton
	}

	/**
	 * Singleton
	 * 
	 * @return
	 * @throws Exception
	 */
	public static DictionarySingleton getInstance() {
		synchronized (_classLock) {
			if (_instance == null) {

				_instance = new DictionarySingleton();

			}

			// Return the singleton
			return _instance;
		}
	}

	public void put(String dictionaryName, HashSet<String> values) {
		dictionaries.put(dictionaryName, values);
	}

	public HashSet<String> get(String dictionaryName) {
		return dictionaries.get(dictionaryName);
	}

	public static void main(String[] args) {
		DictionarySingleton dictionary = DictionarySingleton.getInstance();

		// Filename filter for TXT files
		FilenameFilter txtFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".txt");
			}
		};

		File dictionaryDirectory = new File(
				"/Users/myd/Downloads/dictionaries/");

		File[] dictionaryFiles = dictionaryDirectory.listFiles(txtFilter);

		for (File currentDictionaryFile : dictionaryFiles) {
			// Contains each line of the current directory
			List<String> dictionaryEntries = Utils
					.readFile(currentDictionaryFile);

			HashSet<String> dictionaryValues = new HashSet<String>();

			for (String currentDictionaryEntry : dictionaryEntries) {
				currentDictionaryEntry = currentDictionaryEntry.trim();

				if (currentDictionaryEntry.startsWith("#")
						|| StringUtil.isEmpty(currentDictionaryEntry)) {
					// Skip comments or empty lines
				} else {
					// Multiple values separated by \t
					if (currentDictionaryEntry.indexOf("\t") != -1) {
						String[] splits = currentDictionaryEntry.split("\t");
						String value = splits[0];
						dictionaryValues.add(value);
					} else {
						// Single value
						dictionaryValues.add(currentDictionaryEntry);
					}
				}
			}

			dictionary.put(currentDictionaryFile.getName(), dictionaryValues);
		}

		for (File currentDictionaryFile : dictionaryFiles) {
			HashSet<String> values = dictionary.get(currentDictionaryFile
					.getName());

			List<String> lines = new LinkedList<String>();

			for (String string : values) {
				lines.add(string);
			}

			String path = currentDictionaryFile.getAbsolutePath();

			Utils.writeLinesToFile(new File(path + "_NEW.txt"), lines,
					Utils.NEWLINE, true);
		}
	}
}
