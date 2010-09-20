package org.apache.nutch.kairos.plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.util.LogUtil;

/**
 * Negative Word Dictionary
 * 
 * @author Markus Haense
 */
public class NegativeDictionarySingleton {
	
	/**
	 * Logger
	 */
	private static final Log LOG = LogFactory
			.getLog(NegativeDictionarySingleton.class.getName());
	
	private Set<String> _negativeWordsDictionarySet = new HashSet<String>();

	private static NegativeDictionarySingleton _instance;

	@Override
	public String toString() {
		Enumeration<String> enm;
		StringBuffer result;

		result = new StringBuffer();
		enm = elements();
		while (enm.hasMoreElements()) {
			result.append(enm.nextElement().toString());
			if (enm.hasMoreElements()) {
				result.append(",");
			}
		}

		return result.toString();
	}

	public static synchronized NegativeDictionarySingleton getInstance() {
		if (_instance == null) {
			_instance = new NegativeDictionarySingleton();
		}

		return NegativeDictionarySingleton._instance;
	}

	private NegativeDictionarySingleton() {
		_negativeWordsDictionarySet = new HashSet<String>();
		
		try {
			read("/kairos/dictionaries/negative.txt");
		} catch (Exception e) {
			// Failure
			e.printStackTrace(LogUtil.getErrorStream(LOG));
		}
	}

	public void clear() {
		_negativeWordsDictionarySet.clear();
	}

	public void add(String word) {
		word = word.trim();
		if (!StringUtils.isEmpty(word)) {
			_negativeWordsDictionarySet.add(word.toLowerCase());
		}
	}

	public boolean remove(String word) {
		return _negativeWordsDictionarySet.remove(word);
	}

	public Enumeration<String> elements() {
		Iterator<String> iter;
		Vector<String> list;

		iter = _negativeWordsDictionarySet.iterator();
		list = new Vector<String>();

		while (iter.hasNext()) {
			list.add(iter.next());
		}

		// Sort list
		Collections.sort(list);

		return list.elements();
	}

	public void read(String filename) throws Exception {
		read(new File(filename));
	}

	public void read(File file) throws Exception {
		if (file.exists()) {
			read(new BufferedReader(new FileReader(file)));
		}
	}

	public void read(BufferedReader reader) throws Exception {
		String line;

		clear();

		while ((line = reader.readLine()) != null) {
			line = line.trim();
			// comment?
			if (line.startsWith("#")) {
				continue;
			}
			add(line);
		}

		reader.close();
	}

	public void write(String filename) throws Exception {
		write(new File(filename));
	}

	public void write(File file) throws Exception {
		write(new BufferedWriter(new FileWriter(file)));
	}

	public void write(BufferedWriter writer) throws Exception {
		Enumeration<String> enm;

		// header
		writer.write("# generated " + new Date() + " ("
				+ _negativeWordsDictionarySet.size() + " stopwords)");
		writer.newLine();

		enm = elements();

		while (enm.hasMoreElements()) {
			writer.write(enm.nextElement().toString());
			writer.newLine();
		}

		writer.flush();
		writer.close();
	}

	public boolean isNegativeWord(String str) {
		str = str.trim();
		return _negativeWordsDictionarySet.contains(str.toLowerCase());
	}
}