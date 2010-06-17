package org.apache.nutch.kairos.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.util.LogUtil;

/**
 * @author Markus Haense
 */
public class XMLWriterSingleton {

	/**
	 * Log writer
	 */
	public static final Log LOG = LogFactory.getLog(XMLWriterSingleton.class);

	/**
	 * Instance of the Singleton.
	 */
	private static XMLWriterSingleton _instance;

	/**
	 * Lock object to ensure we get an Singelton object when we work in a
	 * multithreaded environment.
	 */
	private static final Object _classLock = XMLWriterSingleton.class;

	/**
	 * Path to the output file
	 */
	private static String _XMLFileOutputPath;

	private XMLWriterSingleton() {
		// Singleton
	}

	/**
	 * Singleton
	 */
	public static XMLWriterSingleton getInstance() {
		synchronized (_classLock) {
			if (_instance == null) {
				try {
					_XMLFileOutputPath = "kairos/located_metadata/metadata"
							+ Utils.getDateTime() + ".xml";

					Utils.fileExists(new File(_XMLFileOutputPath), true);

					String ENCODING = "ISO-8859-1";

					PrintWriter out = new PrintWriter(new FileOutputStream(
							_XMLFileOutputPath));
					out.println("<?xml version=\"1.0\" encoding=\"" + ENCODING
							+ "\"?>");

					out.close();

				} catch (IOException e) {
					e.printStackTrace(LogUtil.getErrorStream(LOG));
				}

				_instance = new XMLWriterSingleton();
			}

			// Return the singleton
			return _instance;
		}
	}

	public synchronized boolean write(String line) {
		try {
			// Print writer for single XML meta data file
			PrintWriter out = new PrintWriter(new FileOutputStream(
					_XMLFileOutputPath, true));

			out.println(line);

			// Close the print writer
			out.close();

			// Success
			return true;
		} catch (IOException e) {
			// Error
			e.printStackTrace(LogUtil.getErrorStream(LOG));
			return false;
		}
	}
}