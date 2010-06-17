package org.apache.nutch.kairos.crf;

public class SegmentText {

	private String _text = "";
	private String _guessedLabelFromCRF = "";

	private String _domPath = "";

	public SegmentText(String text) {
		this._text = text;
	}

	public SegmentText(String text, String domPath) {
		this._text = text;
		this._domPath = domPath.toLowerCase();
	}

	/**
	 * Return the label if the segment is already labeled with CRF
	 * 
	 * @return
	 */
	public String getGuessedLabelFromCRF() {
		return this._guessedLabelFromCRF;
	}

	/**
	 * Set the guessed label from CRF++
	 * 
	 * @param guessedLabelFromCRF
	 */
	public void setGuessedLabelFromCRF(String guessedLabelFromCRF) {
		if (guessedLabelFromCRF != null) {
			this._guessedLabelFromCRF = guessedLabelFromCRF;
		} else {
			this._guessedLabelFromCRF = "other";
		}
	}

	public String getText() {
		return this._text;
	}

	public String getStylisticFeatures() {
		// Switch feature ON or OFF
		String ON = "1";
		String OFF = "0";

		// Stylistic features
		/*
		String inB = "inB" + OFF;
		String inTH = "inTH" + OFF;
		String inTD = "inTD" + OFF;
		String inI = "inI" + OFF;
		String inStrong = "inStrong" + OFF;
		String inFont = "inFont" + OFF;
		String inEM = "inEM" + OFF;
		String inA = "inA" + OFF;
		*/
		
		String inB = OFF;
		String inTH = OFF;
		String inTD =  OFF;
		String inI = OFF;
		String inStrong = OFF;
		String inFont =  OFF;
		String inEM =  OFF;
		String inA =  OFF;

		for (String currentDOMPathItem : _domPath.split("-")) {
			if (currentDOMPathItem.equals("b")) {
				//inB = "inB" + ON;
				
				inB = ON;
			}

			if (currentDOMPathItem.equals("th")) {
				//inTH = "inTH" + ON;
				inTH =  ON;
			}

			if (currentDOMPathItem.equals("td")) {
				//inTD = "inTD" + ON;
				inTD = ON;
			}

			if (currentDOMPathItem.equals("i")) {
				//inI = "inI" + ON;
				inI =  ON;
			}

			if (currentDOMPathItem.equals("strong")) {
				//inStrong = "inStrong" + ON;
				inStrong =  ON;
			}

			if (currentDOMPathItem.equals("font")) {
				//inFont = "inFont" + ON;
				inFont =  ON;
			}

			if (currentDOMPathItem.equals("em")) {
				//inEM = "inEM" + ON;
				inEM =  ON;
			}

			if (currentDOMPathItem.equals("a")) {
				//inA = "inA" + ON;
				inA = ON;
			}
		}

		StringBuilder featureLine = new StringBuilder();

		featureLine.append(inB + " ");
		featureLine.append(inTH + " ");
		featureLine.append(inTD + " ");
		featureLine.append(inI + " ");
		featureLine.append(inStrong + " ");
		featureLine.append(inFont + " ");
		featureLine.append(inEM + " ");
		featureLine.append(inA);

		return featureLine.toString().trim();
	}
}