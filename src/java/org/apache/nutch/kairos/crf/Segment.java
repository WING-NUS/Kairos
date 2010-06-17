package org.apache.nutch.kairos.crf;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * 
 * @author Markus Haense
 */
public class Segment {

	/**
	 * The HTML segment path
	 */
	private String _domPath = "none";

	/**
	 * The previous HTML Tag
	 */

	/**
	 * If the current text segment is in an A tag and has a href attribute
	 */
	private String _url = "none";

	private List<SegmentText> _segmentText = new LinkedList<SegmentText>();

	private boolean newLine = false;
	private boolean br = false;

	/**
	 * Get the URL if the segment is in an A tag
	 * 
	 * @return
	 */
	public String getURL() {
		return this._url;
	}

	public void setURL(String url) {
		this._url = url;
	}

	/**
	 * Set the DOM Path for the segment
	 * 
	 * @param domPath
	 */
	public void setSegmentDOMPath(String domPath) {
		this._domPath = domPath;
	}

	public void addLineBreak() {
		if (br == false) {
			br = true;
		} else {
			if (newLine == false) {
				addSegmentText("\\\\");
				newLine = true;
			}
		}
	}

	public void addHorizontalLine() {
		if (newLine == false) {
			addSegmentText("\\\\");
			newLine = true;
		}
	}

	public boolean addSegmentText(String text) {
		text = StringEscapeUtils.escapeHtml(text);

		// &apos; is not valid HTML -> but we take care of it ;)
		text = text.replaceAll("&apos;", "'");

		text = text.replaceAll("&nbsp;", " ");

		text = StringEscapeUtils.unescapeHtml(text);

		// Remove more than one space between words
		text = text.replaceAll("\\s+", " ");

		// Omit leading and trailing whitespace
		text = text.trim();

		boolean success = false;

		// If we have characters
		if (text.length() > 0 && text != null) {
			// Add the text to the segment text list
			success = _segmentText.add(new SegmentText(text, _domPath));

			newLine = false;
			br = false;
		} else {
			success = false;
		}

		return success;
	}

	/**
	 * Get the segment text
	 * 
	 * @param text
	 * @return
	 */
	public String getSegmentText(String text) {

		for (int i = 0; i < _segmentText.size(); i++) {
			String currentSegmentText = _segmentText.get(i).getText();

			if (currentSegmentText.equals(text)) {
				return currentSegmentText;
			}
		}

		return "";
	}

	public String getSegmentText(int i) {
		return _segmentText.get(i).getText();
	}

	public String getStylisticFeatures(int i) {
		return _segmentText.get(i).getStylisticFeatures();
	}

	public void setSegmentTextLabel(int i, String label) {
		_segmentText.get(i).setGuessedLabelFromCRF(label);
	}

	public String getSegmentTextLabel(int i) {
		return _segmentText.get(i).getGuessedLabelFromCRF();
	}

	public int getSegmentTextSize() {
		return _segmentText.size();
	}

	public String getSegmentTextConcatenated() {
		StringBuilder text = new StringBuilder();

		for (int i = 0; i < _segmentText.size(); i++) {
			String currentSegmentText = _segmentText.get(i).getText();

			text.append(currentSegmentText + " ");
		}

		return text.toString().trim();
	}
}