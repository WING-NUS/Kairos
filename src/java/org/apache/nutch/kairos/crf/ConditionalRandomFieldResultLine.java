package org.apache.nutch.kairos.crf;

/**
 * A Conditional Random Field line denotes a line from the "crf_test" command
 * from the Conditional Random Field package CRF++
 * (http://crfpp.sourceforge.net/).
 * 
 * @author Markus Haense
 */
public class ConditionalRandomFieldResultLine {

	/**
	 * Label in the training data
	 */
	public String labelInTrainingData;

	/**
	 * Guessed label from the "crf_test" command from the Conditional Random
	 * Field package CRF++
	 */
	public String guessedLabelFromCRF;

	/**
	 * Marginal Probability from the "crf_test" command from the Conditional
	 * Random Field package CRF++
	 */
	public double marginalProbability;

	/**
	 * The word / token
	 */
	public String word;

	public String[] tokens;
	
	/**
	 * Constructor
	 * 
	 * @param word
	 * @param label
	 * @param guessedLabelFromCRF
	 * @param marginalProbability
	 */
	public ConditionalRandomFieldResultLine(String word, String label,
			String guessedLabelFromCRF, double marginalProbability, String[] tokens) {
		// Set the word / token
		this.word = word;
		
		// Set the label from the training data
		this.labelInTrainingData = label;
		
		// Set the guessed label from CRF++
		this.guessedLabelFromCRF = guessedLabelFromCRF;
		
		// Set the marginal probability
		this.marginalProbability = marginalProbability;
		
		// Set the tokens
		this.tokens = tokens;
	}
}