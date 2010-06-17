package org.apache.nutch.kairos.maxent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.util.LogUtil;

/**
 * 
 * @author Markus Haense
 */
public class MaximumEntropyFeatureExtractor {

	/**
	 * Logger
	 */
	public static final Log LOG = LogFactory
			.getLog(MaximumEntropyFeatureExtractor.class);

	/**
	 * The URI standard, RFC 2396, <http://www.ietf.org/rfc/rfc2396.txt> The URL
	 * standard, RFC 1738, <http://www.ietf.org/rfc/rfc1738.txt>
	 * 
	 * Split the host or path of the URL
	 */
	private final String SPLIT = " ;:@&=$-_.+!*'|,^\\`<>#%()/";

	/**
	 * Contain the feature lines for the Maximum Entropy Classifier
	 */
	private StringBuilder _features = new StringBuilder();

	/**
	 * Prefix for the host segments
	 */
	private final String HOST_PREFIX = "H:";

	/**
	 * Prefix for the host segments length
	 */
	private final String HOST_LENGTH_PREFIX = "|H|";

	/**
	 * Prefix for the path segments
	 */
	private final String PATH_PREFIX = "P:";

	/**
	 * Prefix for the path segments length
	 */
	private final String PATH_LENGTH_PREFIX = "|P|";

	/**
	 * Prefix for enumerations like 1st, 2nd, 3rd etc.
	 */
	private final String ENUMERATION_PREFIX = "NUM:";

	/**
	 * Tags the host part of the URL
	 * 
	 * @param host
	 * @return
	 */
	private String taggingHost(String host) {
		StringBuilder finalTaggedHost = new StringBuilder();

		// Lowercase host name
		host = host.toLowerCase();

		// Remove www from host name
		// host = host.replace("www", "");

		// Split the host name
		StringTokenizer st = new StringTokenizer(host, SPLIT, false);

		ArrayList<String> hostList = new ArrayList<String>();

		int num = 0;

		while (st.hasMoreElements()) {
			// Get the current host item
			String currentHostItem = st.nextElement().toString();

			// If the current host item is not empty
			if (currentHostItem.length() > 0) {
				// Append the current host item
				hostList.add(currentHostItem);

				finalTaggedHost.append(HOST_PREFIX + num + ":"
						+ currentHostItem + " ");

				finalTaggedHost.append(PATH_PREFIX + num + ":"
						+ newFeatures(currentHostItem) + " ");

				ArrayList<String> taggedDateFeatureList = findDate(currentHostItem);
				taggedDateFeatureList.removeAll(Arrays.asList(currentHostItem));
				taggedDateFeatureList.add(currentHostItem);

				for (int x = 0; x < taggedDateFeatureList.size(); x++) {
					String currentTaggedFeatureStr = taggedDateFeatureList
							.get(x);

					if (currentTaggedFeatureStr.length() > 0) {
						finalTaggedHost.append(HOST_PREFIX + num + ":"
								+ currentTaggedFeatureStr + " ");
					}
				}

				// Tagging numeration e.g. 1st, 2nd, 3rd, 4th ...
				String taggedNumerationStr = taggingNumeration(currentHostItem);

				if (taggedNumerationStr.length() > 0) {
					finalTaggedHost.append(HOST_PREFIX + num + ":"
							+ taggedNumerationStr + " ");
				}

				// Length of the current host item
				if (currentHostItem.length() > 0) {
					finalTaggedHost.append(HOST_LENGTH_PREFIX + num + ":"
							+ currentHostItem.length() + " ");
				}

				num++;
			}
		}

		// Create B-Gram of the host segments
		for (int x = 0; x < hostList.size() - 1; x++) {
			for (int y = (x + 1); y < hostList.size(); y++) {
				finalTaggedHost.append(HOST_PREFIX + hostList.get(x) + "|"
						+ hostList.get(y) + " ");
			}
		}

		if (num != 0) {
			finalTaggedHost.append(HOST_LENGTH_PREFIX + "ALL:" + num + " ");
		}

		return finalTaggedHost.toString().trim();
	}

	/**
	 * Tags the path part of the URL
	 * 
	 * @param host
	 * @return
	 */
	private String taggingPath(String path) {
		StringBuilder finalTaggedPath = new StringBuilder();

		StringTokenizer st = new StringTokenizer(path, SPLIT, false);

		ArrayList<String> pathList = new ArrayList<String>();

		int num = 0;
		while (st.hasMoreElements()) {
			String currentPathItem = st.nextElement().toString();

			if (currentPathItem.length() > 0) {
				currentPathItem = currentPathItem.trim();

				pathList.add(currentPathItem);

				// If the current path item is not empty
				if (currentPathItem.length() > 0) {
					finalTaggedPath.append(PATH_PREFIX + num + ":"
							+ currentPathItem + " ");

					finalTaggedPath.append(PATH_PREFIX + num + ":"
							+ newFeatures(currentPathItem) + " ");

					// Find date in current path item
					ArrayList<String> taggedDateFeatureList = findDate(currentPathItem);
					taggedDateFeatureList.removeAll(Arrays
							.asList(currentPathItem));
					taggedDateFeatureList.add(currentPathItem);

					if (taggedDateFeatureList.size() > 0) {
						for (int x = 0; x < taggedDateFeatureList.size(); x++) {
							String currentTaggedFeatureStr = taggedDateFeatureList
									.get(x);

							if (currentTaggedFeatureStr.length() > 0) {
								finalTaggedPath.append(PATH_PREFIX + num + ":"
										+ currentTaggedFeatureStr + " ");
							}
						}
					}

					String taggedNumerationStr = taggingNumeration(currentPathItem);

					if (taggedNumerationStr.length() > 0) {
						finalTaggedPath.append(PATH_PREFIX + num + ":"
								+ taggingNumeration(currentPathItem) + " ");
					}

					if (currentPathItem.length() > 0) {
						finalTaggedPath.append(PATH_LENGTH_PREFIX + num + ":"
								+ currentPathItem.length() + " ");
					}

					num++;
				}
			}
		}

		if (num != 0) {
			finalTaggedPath.append(PATH_LENGTH_PREFIX + "ALL:" + num + " ");
		}

		// Create B-Gram of the path segments
		for (int x = 0; x < pathList.size() - 1; x++) {
			for (int y = (x + 1); y < pathList.size(); y++) {
				finalTaggedPath.append(PATH_PREFIX + pathList.get(x) + "|"
						+ pathList.get(y) + " ");
			}
		}

		return finalTaggedPath.toString().trim();
	}

	/**
	 * Finding enumerations e.g. 1st, 2nd, 3rd, 4th
	 * 
	 * @param input
	 * @return
	 */
	private String taggingNumeration(String input) {
		// Regular expression to find enumerations
		String numerationRegex = "[0-9](st|nd|rd|th)";

		Pattern p = Pattern.compile(numerationRegex);
		Matcher m = p.matcher(input);

		// While we find enumerations
		while (m.find()) {
			// Get the enumeration
			String enumeration = input.substring(m.start() + 1, m.end());

			LOG.info("Found enumeration: " + input);

			// Tag the input string
			input = ENUMERATION_PREFIX + input.substring(0, m.start() + 1)
					+ enumeration;

			return input;
		}

		// No enumeration exists
		return "";
	}

	/**
	 * Try to identify dates in the URL
	 * 
	 * @param inputStr
	 * @return
	 */
	private ArrayList<String> findDate(String inputStr) {

		// Final tagged date feature list
		ArrayList<String> taggedDateFeatureList = new ArrayList<String>();

		// taggedDateFeatureList.add(inputStr);
		// System.out.println("Processing feature: " + inputStr);

		// Possessive regular expression to find years in input
		String yearRegex = "^[a-z]*+(19|20)?+[0-9]{1}+\\d{1}+$";

		Pattern pattern = Pattern.compile(yearRegex);
		Matcher matcher = pattern.matcher(inputStr);

		String currentYear = "";

		if (matcher.find() == true) {
			LOG.info("Found year [" + inputStr + "]");

			// Replace all occurrences of pattern in input
			// Extract only the year in the input
			pattern = Pattern.compile("[a-z]*");
			matcher = pattern.matcher(inputStr);
			String yearStr = matcher.replaceAll("");

			currentYear = yearStr;

			LOG.info("Year: [" + inputStr + "] ==> [" + yearStr + "]");

			// Replace all occurrences of pattern in input
			// Extract only the string without the year
			pattern = Pattern.compile("[0-9]*");
			matcher = pattern.matcher(inputStr);

			// Prefix of the year
			String prefixYearStr = matcher.replaceAll("");

			LOG.info("Prefix year [" + prefixYearStr + "] => [" + inputStr
					+ "]");

			// The replaced year string from format YY to format YYYY
			String replacedYearStr = "";

			// Replace only dates with format YY
			if (yearStr.length() == 2) {
				// Indicates if we already replaced a year
				boolean found = false;

				// Replace year from format 08 to 2008
				yearRegex = "^[0]\\d{1}$";
				pattern = Pattern.compile(yearRegex);
				matcher = pattern.matcher(yearStr);

				// If we find a date format
				if (matcher.find() == true) {
					replacedYearStr = "20" + yearStr;
					// System.out.println("replaced 1: " + replacedYearStr);
					found = true;
				}

				// Replace year from the format 98 to 1998
				yearRegex = "^[1-9]\\d{1}$";
				pattern = Pattern.compile(yearRegex);
				matcher = pattern.matcher(yearStr);

				if (matcher.find() == true && found == false) {
					replacedYearStr = "19" + yearStr;
					// System.out.println("replaced 2: " + replacedYearStr);
				}

				if (replacedYearStr.length() > 0) {
					currentYear = replacedYearStr;
				}
			}

			if (!isNumeric(inputStr)) {
				taggedDateFeatureList.add(prefixYearStr);

				taggedDateFeatureList.add(yearStr);

				taggedDateFeatureList.add(replacedYearStr);

				taggedDateFeatureList.add(prefixYearStr + yearStr);

				taggedDateFeatureList.add(prefixYearStr + replacedYearStr);

				taggedDateFeatureList.add(prefixYearStr + "<year>");

				taggedDateFeatureList.add("<year>");

				// Add the prefix length before a year
				if (prefixYearStr.length() > 0) {
					taggedDateFeatureList.add(Integer.toString(prefixYearStr
							.length())
							+ "<year>");
				}

			} else {
				// Input is numeric
				taggedDateFeatureList.add(inputStr);

				taggedDateFeatureList.add(replacedYearStr);

				taggedDateFeatureList.add("<year>");
			}

			LOG.info("Replace year from format (YY) to format (YYYY) ["
					+ yearStr + "] => [" + replacedYearStr + "]   [" + inputStr
					+ "]");

			// Tag current year
			int year = Calendar.getInstance().get(Calendar.YEAR);

			// Allow current year + 2 years in future
			// likely to be a conference website
			if (currentYear.equals(Integer.toString(year))
					|| currentYear.equals(Integer.toString(year++))
					|| currentYear.equals(Integer.toString(year++))) {

				LOG.info("Found current year OR +2 years in future ["
						+ inputStr + "]");

				taggedDateFeatureList.add(prefixYearStr + "<currentYear>");

				taggedDateFeatureList.add("<currentYear>");
			}

			// System.out.println("Final: " + inputStr + "\n");
		}

		return taggedDateFeatureList;
	}

	/**
	 * Checks if the input string is numeric
	 * 
	 * @param input
	 * @return
	 */
	private boolean isNumeric(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public String newFeatures(String blub) {
		StringBuilder newFeatures = new StringBuilder();
		if (blub != null && blub.length() > 0) {
			char[] characters = blub.toCharArray();

			/*
			 * for (int i = 0; i < characters.length; i++) { if (i == 3) {
			 * break; }
			 * 
			 * String prefix = "";
			 * 
			 * for (int j = 0; j <= i; j++) { prefix += characters[j]; }
			 * 
			 * newFeatures.append(prefix + " "); }
			 * 
			 * if (characters.length > 3) { for (int i = characters.length - 3;
			 * i <= characters.length - 1; i++) {
			 * 
			 * if (i < 0) { break; }
			 * 
			 * String suffix = "";
			 * 
			 * for (int j = i; j <= characters.length - 1; j++) { suffix +=
			 * characters[j]; }
			 * 
			 * newFeatures.append(suffix + " "); } } else { newFeatures.append(
			 * " "); }
			 */

			for (char c : characters) {
				if (Character.isDigit(c)) {
					newFeatures.append("0");
				}

				if (Character.isUpperCase(c) && Character.isLetter(c)) {
					newFeatures.append("A");
				}

				if (Character.isLowerCase(c) && Character.isLetter(c)) {
					newFeatures.append("a");
				}

				if (!Character.isLetter(c) && !Character.isDigit(c)) {
					newFeatures.append(".");
				}
			}

			newFeatures.append(" ");
		}

		return newFeatures.toString().trim();
	}

	public String createMaximumEntropyFeatureLineForURL(String urlStr) {
		StringBuilder predicates = new StringBuilder();

		// Create URL object
		URL url = null;

		try {
			// decode the domain
			urlStr = java.net.URLDecoder.decode(urlStr, "UTF-8");
			url = new URL(urlStr);

			if (url != null) {
				String path = url.getPath();

				predicates.append(taggingHost(url.getHost()).trim() + " ");
				predicates.append(taggingPath(path).trim());
			}
		} catch (MalformedURLException e) {
			LOG.info("Malformed url [" + urlStr + "] " + e.getMessage());
		} catch (UnsupportedEncodingException e) {
			LOG.error("UnsupportedEncodingException: " + e.getMessage());
		}

		return predicates.toString();
	}

	public StringBuilder read(File file, String classStr) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				// Create URL object
				URL url = null;

				if (line.length() > 0) {
					try {
						// decode the domain
						line = java.net.URLDecoder.decode(line, "UTF-8");
						url = new URL(line);

						if (url != null) {
							// _features.append("url: " + url.toString() +
							// "\n");
							System.out.println(url.toString());

							String path = url.getPath();


							String[] segmentedURL = Utils.segmentURL(url
									.toString());
							if (segmentedURL != null) {
								_features.append(taggingHost(url.getHost())
										+ " ");
								_features.append(taggingPath(path) + " ");

								for (int x = 0; x < segmentedURL.length - 1; x++) {
									for (int y = (x + 1); y < segmentedURL.length; y++) {
										_features
												.append(newFeatures(segmentedURL[x])
														+ "|"
														+ newFeatures(segmentedURL[y])
														+ " ");
									}
								}

								StringBuilder newFeatures = new StringBuilder();

								for (String currentSegment : segmentedURL) {
									if (currentSegment != null
											&& currentSegment.length() > 0) {
										newFeatures
												.append(newFeatures(currentSegment)
														+ " ");
									}
								}

								// _features.append(newFeatures + " ");

								_features.append(classStr + "\n");
							}
						}
					} catch (MalformedURLException e) {
						e.printStackTrace(LogUtil.getErrorStream(LOG));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace(LogUtil.getErrorStream(LOG));
					}
				}
			}

			// Close the buffered reader
			br.close();
		} catch (IOException e) {
			e.printStackTrace(LogUtil.getErrorStream(LOG));
			return new StringBuilder();
		}

		return _features;
	}

	public void write(File file, StringBuilder content) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(content.toString() + System.getProperty("line.separator"));
			bw.close();

			_features = new StringBuilder();
		} catch (IOException e) {
			e.printStackTrace(LogUtil.getErrorStream(LOG));
		}
	}

	public static void main(String[] args) {
		MaximumEntropyFeatureExtractor featureExtractor = new MaximumEntropyFeatureExtractor();

		// Read in positive
		featureExtractor.write(new File("maxent/+conference/+conference.txt"),
				featureExtractor.read(new File("maxent/conferenceURLs.txt"),
						"+conference"));

		// Read in negative
		featureExtractor.write(new File("maxent/-conference/-conference.txt"),
				featureExtractor.read(new File(
						"maxent/negativeConferencesURLs.txt"), "-conference"));

		List<String> positiveURLs = Utils.readFile(new File(
				"maxent/+conference/+conference.txt"));
		List<String> negativeURLs = Utils.readFile(new File(
				"maxent/-conference/-conference.txt"));

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					"maxent/trainingData.txt")));

			boolean positive = false;
			boolean negative = false;

			for (int i = 0;;) {

				if (i < positiveURLs.size()) {
					bw.write(positiveURLs.get(i)
							+ System.getProperty("line.separator"));
				} else {
					positive = true;
				}

				if (i < negativeURLs.size()) {
					bw.write(negativeURLs.get(i)
							+ System.getProperty("line.separator"));
				} else {
					negative = true;
				}

				bw.flush();

				if (positive == true && negative == true) {
					break;
				}

				i++;
			}

			bw.close();
		} catch (IOException e) {
			e.printStackTrace(LogUtil.getErrorStream(LOG));
		}
	}
}