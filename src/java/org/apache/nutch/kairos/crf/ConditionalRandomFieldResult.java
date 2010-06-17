package org.apache.nutch.kairos.crf;

import java.util.List;

/**
 * 
 * A Conditional Random Field result contains of Conditional Random Field lines.
 * A Conditional Random Field line denotes a line from the "crf_test" command
 * from the Conditional Random Field package CRF++
 * (http://crfpp.sourceforge.net/).
 * 
 * @author Markus Haense
 */
public class ConditionalRandomFieldResult {

	/**
	 * Conditional Output Probability (confidence measure for the entire output)
	 */
	private double _conditionalOutputProbability;

	/**
	 * The Conditional Random Field lines
	 */
	private List<ConditionalRandomFieldResultLine> _conditionalRandomFieldResultLines;

	/**
	 * Constructor
	 * 
	 * @param conditionalOutputProbability
	 * @param conditionalRandomFieldResultLines
	 */
	public ConditionalRandomFieldResult(
			double conditionalOutputProbability,
			List<ConditionalRandomFieldResultLine> conditionalRandomFieldResultLines) {

		// Set the conditional output probability
		this._conditionalOutputProbability = conditionalOutputProbability;

		// Set the Conditional Random Field lines
		this._conditionalRandomFieldResultLines = conditionalRandomFieldResultLines;
	}

	/**
	 * @return the Conditional Random Field result lines
	 */
	public List<ConditionalRandomFieldResultLine> getConditionalRandomFieldResultLines() {
		// Get the Conditional Random Field result lines
		return this._conditionalRandomFieldResultLines;
	}

	/**
	 * @return the conditional output probability (confidence measure for the
	 *         entire output)
	 */
	public double getConditionalOutputProbability() {
		// Get the conditional output probability
		return this._conditionalOutputProbability;
	}
}
