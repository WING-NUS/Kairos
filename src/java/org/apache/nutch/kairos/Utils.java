package org.apache.nutch.kairos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.util.LogUtil;
import org.apache.nutch.util.URLUtil;
import org.w3c.dom.Node;

public class Utils {

	public static final String NEWLINE = System.getProperty("line.separator");

	/**
	 * Logger
	 */
	public static final Log LOG = LogFactory.getLog(Utils.class.getName());

	/**
	 * The URI standard, RFC 2396, <http://www.ietf.org/rfc/rfc2396.txt>. The
	 * URL standard, RFC 1738, <http://www.ietf.org/rfc/rfc1738.txt>
	 */
	private static final String SPLIT = " ;:@&=$-_.+!*'|,^\\`<>#%()/";

	public static String getCleanTextFromHTML(String text) {
		text = StringEscapeUtils.escapeHtml(text);

		// &apos; is not valid HTML -> but we take care of it ;)
		text = text.replaceAll("&apos;", "'");

		text = text.replaceAll("&nbsp;", " ");

		text = StringEscapeUtils.unescapeHtml(text);

		// Remove more than one space between words
		text = text.replaceAll("\\s+", " ");

		// Omit leading and trailing whitespace
		text = text.trim();

		return text;
	}

	/**
	 * Convert an array of strings to one string. Put the 'separator' string
	 * between each element.
	 */
	public static String arrayToString(String[] array, String separator) {
		StringBuffer result = new StringBuffer();

		if (array != null && array.length > 0 && separator != null) {

			result.append(array[0]);

			for (int i = 1; i < array.length; i++) {
				if (array[i].length() > 0) {
					result.append(separator);
					result.append(array[i]);
				}
			}
		}

		return result.toString();
	}

	/**
	 * Convert an list of strings to one string. Put the 'separator' string
	 * between each element.
	 */
	public static String listToString(List<String> list, String separator) {
		StringBuffer result = new StringBuffer();

		if (list != null && list.size() > 0 && separator != null) {

			result.append(list.get(0));

			for (int i = 1; i < list.size(); i++) {
				if (list.get(i).length() > 0) {
					result.append(separator);
					result.append(list.get(i));
				}
			}
		}

		return result.toString();
	}

	/**
	 * Segment a URL
	 * 
	 * @param url
	 * @return
	 */
	public static String[] segmentURL(String url) {
		if (url != null) {
			LOG.info("(segmentURL) Tokenize URL [" + url + "]");

			URL urlObject = Utils.convertStringToURLObject(url, false);

			if (urlObject != null) {
				url = URLUtil.getPage(urlObject.toString());

				if (url != null && url.length() > 0) {
					// Remove www
					url = url.replace("www", "");

					// Remove http
					url = url.replace("http", "");

					// Tokenize the url
					StringTokenizer st = new StringTokenizer(url, SPLIT, false);

					List<String> newURL = new ArrayList<String>();

					while (st.hasMoreElements()) {
						String currentItem = st.nextElement().toString().trim();

						if (!currentItem.equals("html")
								|| !currentItem.equals("htm")
								|| !currentItem.equals("default")
								|| !currentItem.equals("index")
								|| !currentItem.equals("pdf")
								|| !currentItem.equals("aspx")
								|| !currentItem.equals("php")) {

							newURL.add(currentItem);
						}
					}

					String str[] = new String[newURL.size()];

					return newURL.toArray(str);
				} else {
					LOG.info("(segmentURL) URL is empty");
					return null;
				}
			} else {
				LOG.info("(segmentURL) URL Object is null");
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Checks if the input string is numeric
	 * 
	 * @param input
	 * @return
	 */
	public static boolean isNumeric(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static URL convertStringToURLObject(String url, boolean decode) {
		if (url != null) {
			try {
				if (decode == true) {
					// decode the domain
					url = decodeURL(url);
				}

				if (url.length() > 0) {
					if (!url.startsWith("http://")) {
						url = "http://" + url;
					}

					if (url.equals("http://")) {
						return null;
					} else {
						// WikiCFP errors
						url = url.replaceAll("http:////", "http://");
						url = url.replaceAll("http:///", "http://");
						url = url.replaceAll("http://URL: ", "");

						// Create URL object
						URL urlObject = new URL(url);

						return urlObject;
					}
				} else {
					return null;
				}
			} catch (MalformedURLException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	public static String decodeURL(String url) {
		try {
			return java.net.URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	public static String checkForRedirectionAndReturnForwardHostURL(String url) {
		return checkForRedirectionAndReturnForwardHostURL(convertStringToURLObject(
				url, false));
	}

	public static String checkForRedirectionAndReturnForwardHostURL(URL url) {
		if (url != null) {
			try {
				String oldUrl = url.toString();
				HttpURLConnection.setFollowRedirects(false);
				URLConnection connection = url.openConnection();
				if (connection instanceof HttpURLConnection) {
					HttpURLConnection httpConnection = (HttpURLConnection) connection;
					httpConnection.setRequestMethod("HEAD");
					httpConnection.connect();
					int response = httpConnection.getResponseCode();
					System.out.println("Response: " + response);
					String location = httpConnection.getHeaderField("Location");
					if (location != null) {

						if (!location.startsWith("http://")) {
							location = url.toString() + location;
						}

						System.out.println("Location: " + location);
						boolean tempRedirect = response == 307 ? true : false;

						location = URLUtil.chooseRepr(oldUrl, location,
								tempRedirect);
						System.out.println("NEW Location: " + location);
						return location;
					} else {
						return url.toString();
					}
				} else {
					return "";
				}
			} catch (IOException e) {
				return "";
			}
		} else {
			LOG
					.error("(checkForRedirectionAndReturnForwardHostURL) URL is null");
			return "";
		}
	}

	public static List<String> readFile(File file) {
		try {
			List<String> lines = new LinkedList<String>();

			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				if (line.length() > 0) {
					lines.add(line.trim());
				}
			}

			// Close the buffered reader
			br.close();

			return lines;
		} catch (IOException e) {
			LOG.error("(readFile) IOException " + e.getMessage());
			return new LinkedList<String>();
		}
	}

	public static boolean writeLinesToFile(File file, List<String> lines,
			String seperator, boolean append) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, append));

			for (String line : lines) {
				bw.write(line + seperator);
				bw.flush();
			}

			bw.close();

			return true;
		} catch (IOException e) {
			e.printStackTrace(LogUtil.getErrorStream(LOG));
			return false;
		}
	}

	public static String getDateTime() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		df.setTimeZone(TimeZone.getTimeZone("PST"));
		return df.format(new Date());
	}

	/**
	 * Prints out the DOM tree
	 * 
	 * @param node
	 * @param sIndent
	 */
	public void printDOMTree(Node node, String sIndent) {
		System.out.println(sIndent + node.getClass().getName() + ": "
				+ node.getNodeName() + " = " + node.getNodeValue());
		Node child = node.getFirstChild();
		while (child != null) {
			printDOMTree(child, sIndent + "    ");
			child = child.getNextSibling();
		}
	}

	public static boolean fileExists(File file, boolean delete) {

		boolean fileExists = file.exists();

		if (fileExists == true && delete == true) {
			file.delete();
		}

		return fileExists;
	}

	public static void main(String[] args) {

	}
}
