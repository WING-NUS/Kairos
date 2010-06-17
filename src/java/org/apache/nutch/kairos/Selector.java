package org.apache.nutch.kairos;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.nutch.util.LogUtil;
import org.apache.nutch.util.StringUtil;

/**
 * This class selects conference URLs from the WikiCFP index for scheduled
 * crawling
 * 
 * Markus Haense
 */
public class Selector {

	/**
	 * Logger
	 */
	public static final Log LOG = LogFactory.getLog(Selector.class);

	/**
	 * Date format string
	 */
	public static final String FORMATSTRING = "MMM dd, yyyy";

	/**
	 * Validate whether the argument string can be parsed into a legal date.<br />
	 * 
	 * Does check for formating errors and illegal data (so an invalid month or
	 * day number is detected).
	 * 
	 * @param formatStr
	 *            date format string to be used to validate against
	 * @return true if a correct date and conforms to the restrictions
	 */
	private static boolean validateDate(String dateStr, String formatStr) {
		if (formatStr == null) {
			return false;
		} else {
			SimpleDateFormat df = new SimpleDateFormat(formatStr);
			Date testDate = null;

			try {
				testDate = df.parse(dateStr);
			} catch (ParseException e) {
				// Invalid date format
				return false;
			}

			// Initialize the calendar to midnight to prevent
			// the current day from being rejected
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);

			// Date in future?
			if (cal.getTime().before(testDate)) {
				return false;
			} else {
				// Get the difference in days
				// of the current date and todays date
				int days = Selector.getDifferenceOfDatesInDays(testDate);

				// Allow only dates in a range of 1 week (7 days)
				if (days <= 7) {
					return true;
				} else {
					return false;
				}
			}
		}
	}

	/**
	 * Get the difference in days of the passed date and the todays date
	 * 
	 * @param date
	 * @return
	 */
	private static int getDifferenceOfDatesInDays(Date date) {
		// Convert Date object to Calendar object
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		// Passed date
		GregorianCalendar past = new GregorianCalendar();
		past.setTime(cal.getTime());

		// Today
		GregorianCalendar today = new GregorianCalendar();

		// Get the difference
		long difference = today.getTimeInMillis() - past.getTimeInMillis();

		// Return difference in days
		return (int) (difference / (1000 * 60 * 60 * 24));
	}

	/**
	 * Select conference URLs for crawling
	 */
	public static void selectScheduledConferenceURLsForCrawling() {
		// Index directory path
		String indexDirectoryPath = "crawl.dirs/crawl.wikicfp/index";

		// If the index directory exists
		if (new File(indexDirectoryPath).exists()) {
			// Index reader object
			IndexReader ir = null;

			// Open the index
			try {
				ir = IndexReader.open(indexDirectoryPath);
			} catch (CorruptIndexException e) {
				e.printStackTrace(LogUtil.getErrorStream(LOG));
			} catch (IOException e) {
				e.printStackTrace(LogUtil.getErrorStream(LOG));
			}

			if (ir != null) {
				// Contains the conference metadata
				// from the WikiCFP index
				List<String> conferenceMetadata = new LinkedList<String>();

				// For each document in the index
				for (int x = 0; x < ir.maxDoc(); x++) {
					// Current document from the index
					Document doc = null;

					try {
						doc = ir.document(x);
					} catch (CorruptIndexException e) {
						e.printStackTrace(LogUtil.getErrorStream(LOG));
					} catch (IOException e) {
						e.printStackTrace(LogUtil.getErrorStream(LOG));
					}

					if (doc != null) {
						// Get the when value
						String whenValue = doc.get("When");

						// Get the conference link value
						String conferenceLink = doc.get("Link");

						if (!StringUtil.isEmpty(whenValue)
								&& !StringUtil.isEmpty(conferenceLink)
								&& !whenValue.equals("N/A")) {

							// LOG.info(whenValue + " => " + conferenceLink);
							// Split the date on "-"
							// e.g. Mar 14, 2008 - Mar 16, 2008
							String splittedDate[] = whenValue.split("-");

							if (splittedDate.length == 2) {
								// Begin Date
								String beginDateOfConference = splittedDate[0]
										.trim();

								// End Date
								String endDateOfConference = splittedDate[1]
										.trim();

								// If we have a valid date
								// in the range of 1 week (7 days)
								if (Selector.validateDate(endDateOfConference,
										FORMATSTRING)) {
									LOG.info("Scheduled for crawling ["
											+ endDateOfConference + "] => ["
											+ conferenceLink + "]");
									
									System.out.println("Scheduled for crawling ["
											+ endDateOfConference + "] => ["
											+ conferenceLink + "]");

									List<String> line = new LinkedList<String>();

									// Add the conference URL to the conference
									// metadata
									line.add(conferenceLink);

									// Get the conference name
									String name = doc.get("Name");

									// Get the conference title
									String title = doc.get("Title");

									// Get the conference place
									String where = doc.get("Where");

									if (!StringUtil.isEmpty(name)) {
										line.add("c_name=" + name);
									}

									if (!StringUtil.isEmpty(title)) {
										line.add("c_title=" + title);
									}

									if (!StringUtil
											.isEmpty(beginDateOfConference)) {
										line.add("c_begin_date="
												+ beginDateOfConference);
									}

									if (!StringUtil
											.isEmpty(endDateOfConference)) {
										line.add("c_end_date="
												+ endDateOfConference);
									}

									if (!StringUtil.isEmpty(where)) {
										line.add("c_place=" + where);
									}

									if (line.size() > 0) {
										conferenceMetadata.add(Utils
												.listToString(line, "\t"));
									}
								}
							}
						}
					}
				}

				if (conferenceMetadata.size() > 0) {
					Utils.writeLinesToFile(new File(
							"urls/conference/selectedURLsToFetch.txt"),
							conferenceMetadata, Utils.NEWLINE, false);
				}
			}
		} else {
			System.err.println("WikiCFP index does not exists");
			LOG.error("WikiCFP index does not exists");

			// Delete the old seed URL file
			Utils.writeLinesToFile(new File(
					"urls/conference/selectedURLsToFetch.txt"),
					new LinkedList<String>(), Utils.NEWLINE, false);
		}
	}

	public static void main(String[] args) {
		Selector.selectScheduledConferenceURLsForCrawling();
	}
}
