package org.apache.nutch.kairos.crf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import opennlp.maxent.MaxentModel;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.postag.DefaultPOSContextGenerator;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.TokenizerME;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.util.LogUtil;
import org.apache.nutch.util.StringUtil;
import org.chasen.crfpp.Tagger;

/**
 * This class is a wrapper for the CRF++ package
 * (http://crfpp.sourceforge.net/).
 * 
 * @author Markus Haense
 */
public class ConditionalRandomFieldSingleton {

	/**
	 * Loads the CRF++ library for the Java wrapper which comes along with the
	 * CRF++ package
	 */
	static {
		try {
			File dir = new File(".");

			try {
				System.load(dir.getCanonicalPath()
						+ "/kairos/CRF++-0.54/java/libCRFPP.so");
			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace(LogUtil
						.getErrorStream(ConditionalRandomFieldSingleton.LOG));
			}
		} catch (UnsatisfiedLinkError e) {
			System.err
					.println("Cannot load the CRFPP native code.\nMake sure your LD_LIBRARY_PATH contains \'.\'\n"
							+ e);

			e.printStackTrace(LogUtil
					.getErrorStream(ConditionalRandomFieldSingleton.LOG));
			System.err.println("ConditionalRandomFieldSingleton EXIT!!!");
			System.exit(1);
		}
	}

	/**
	 * Logger
	 */
	private static final Log LOG = LogFactory
			.getLog(ConditionalRandomFieldSingleton.class.getName());

	/**
	 * Instance of the Singleton.
	 */
	private static ConditionalRandomFieldSingleton _instance;

	/**
	 * Lock object to ensure we get an Singelton object when we work in a
	 * multithreaded environment.
	 */
	private static final Object _classLock = ConditionalRandomFieldSingleton.class;

	/**
	 * Path to the template, model and training files
	 */
	private static String _pathToFiles = "kairos/";

	/**
	 * Model Name
	 */
	private File _modelFile = new File(_pathToFiles + "CrfppPaperMetadataModel");

	/**
	 * Template File
	 */
	private File _templateFile = new File(_pathToFiles + "template.txt");

	/**
	 * Training File
	 */
	private File _trainFile = new File(_pathToFiles + "train.data.txt");

	/**
	 * Path to the open-nlp-tools English token model
	 */
	private static String _englishTokenModel = _pathToFiles
			+ "open-nlp-tools-models/tokenize/EnglishTok.bin.gz";

	/**
	 * Path to the open-nlp-tools Part-Of-Speech tag model
	 */
	private static String _tagDictionary = _pathToFiles
			+ "open-nlp-tools-models/postag/tag.bin.gz";

	/**
	 * open-nlp-tool part-of-speech tagger
	 */
	private static POSTaggerME _postagger;

	/**
	 * open-nlp-tool tokenizer for converting raw text into separated tokens.
	 */
	private static MaxentModel _maxentModel;

	/**
	 * Path to the crf_test & crf_train command from the Conditional Random
	 * Field package CRF++
	 */
	private static String pathToCRF = "kairos/CRF++-0.53/bin/";

	/**
	 * Constructor
	 */
	private ConditionalRandomFieldSingleton() {
		// private
	}

	/**
	 * Singleton
	 * 
	 * @return
	 * @throws Exception
	 */
	public static ConditionalRandomFieldSingleton getInstance() {
		synchronized (_classLock) {
			if (_instance == null) {
				// Initialize the tokenizer
				try {
					_maxentModel = new SuffixSensitiveGISModelReader(new File(
							_englishTokenModel)).getModel();
				} catch (IOException e) {
					e.printStackTrace(LogUtil.getErrorStream(LOG));
				}

				// Initialize the Part-Of-Speach tagger
				try {
					_postagger = new POSTaggerME(
							new SuffixSensitiveGISModelReader(new File(
									_tagDictionary)).getModel(),
							new DefaultPOSContextGenerator(new Dictionary()));
				} catch (IOException e) {
					// Failure
					e.printStackTrace(LogUtil.getErrorStream(LOG));
				}

				_instance = new ConditionalRandomFieldSingleton();
			}

			// Return the singleton
			return _instance;
		}
	}

	/**
	 * Trains the CRF model and saves it to file on disk.
	 * 
	 * @return true if we trained a CRF successfully
	 */
	public boolean training() {
		try {
			LOG.info("CRF++ (training)");
			LOG.info("Template: " + _templateFile.getAbsolutePath());
			LOG.info("Training file: " + _trainFile.getAbsolutePath());
			LOG.info("Model file: " + _modelFile.getAbsolutePath());
			
			// Execute the CRF learn command
			Process process = Runtime.getRuntime().exec(
					pathToCRF + "crf_learn " + _templateFile.getAbsolutePath()
							+ " " + _trainFile.getAbsolutePath() + " "
							+ _modelFile.getAbsolutePath() + " -p2");

			// Read the output
			BufferedReader br = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			// Contains the actual line of the output
			String line = null;

			// For each line
			while ((line = br.readLine()) != null) {
				// Print out the line
				System.out.println(line);
				LOG.info(line);
			}

			// Close the Buffered Reader
			br.close();

			// Success
			return true;
		} catch (IOException e) {
			// Failure
			e.printStackTrace(LogUtil.getErrorStream(LOG));
			return false;
		}
	}

	/**
	 * Predict the labels for the feature lines
	 * 
	 * @return a Conditional Random Field result, A Conditional Random Field
	 *         contains of Conditional Random Field lines. A Conditional Random
	 *         Field line denotes a line from the "crf_test" command from the
	 *         Conditional Random Field package CRF++
	 *         (http://crfpp.sourceforge.net/).
	 */
	public List<ConditionalRandomFieldResult> testing(List<CRFContext> context,
			boolean crossValidation) {
		// If we have context
		if (context.size() > 0) {
			Tagger tagger = null;

			try {
				tagger = new Tagger("-m " + _modelFile.getCanonicalPath()
						+ " -v 1");
				// clear internal context
				tagger.clear();
			} catch (IOException e) {
				// Failure
				e.printStackTrace(LogUtil.getErrorStream(LOG));
			}

			if (tagger != null) {
				// Contains the Conditional Random Field results
				List<ConditionalRandomFieldResult> results = new LinkedList<ConditionalRandomFieldResult>();

				for (CRFContext currentContext : context) {
					for (String currentContextLine : currentContext
							.getContext()) {
						// Add context
						tagger.add(currentContextLine);
					}

					// parse and change internal stated as 'parsed'
					if (!tagger.parse()) {
						return new LinkedList<ConditionalRandomFieldResult>();
					}

					// Contains the Conditional Random Field lines
					List<ConditionalRandomFieldResultLine> conditionalRandomFieldResultLines = new LinkedList<ConditionalRandomFieldResultLine>();

					for (int i = 0; i < tagger.size(); i++) {
						List<String> tokenList = new LinkedList<String>();

						// Add the tokens
						for (int j = 0; j < tagger.xsize(); j++) {
							tokenList.add(tagger.x(i, j));
						}

						// Get the original word
						String word = tagger.x(i, 0);

						// True answer tag
						// String answerLabel = crossValidation == true ? tagger
						// .x(i, tagger.xsize()) : "";

						// TODO -> If we gonna change the template file
						// to deactivate certion features then the tag size
						// xsize is wrong
						String answerLabel = crossValidation == true ? tagger
								.x(i, 21) : "";

						// Guessed answer tag from CRF
						String guessedLabelFromCRF = tagger.y2(i);

						// Marginal probability
						double marginalProbability = tagger.prob(i, tagger
								.result(i));

						String[] tokenArray = new String[tokenList.size()];
						tokenList.toArray(tokenArray);

						// New Conditional Random Field result line
						conditionalRandomFieldResultLines
								.add(new ConditionalRandomFieldResultLine(word,
										answerLabel, guessedLabelFromCRF,
										marginalProbability, tokenArray));
					}

					if (conditionalRandomFieldResultLines.size() > 0) {
						// Add the CRF result to the results
						results.add(new ConditionalRandomFieldResult(tagger
								.prob(), conditionalRandomFieldResultLines));
					}

					// Clear internal context
					tagger.clear();
					//tagger.delete();
				}
				
				tagger.clear();
				tagger.delete();
				
				// Return context
				return results;
			} else {
				// No context
				return new LinkedList<ConditionalRandomFieldResult>();
			}
		} else {
			// No context
			return new LinkedList<ConditionalRandomFieldResult>();
		}
	}

	/**
	 * Evaluate the Conditional Random Field (CRF) model
	 */
	public void evaluate() {
		// Contains the randomized training / testing data
		List<CRFContext> randData = new LinkedList<CRFContext>();

		try {
			// Contains the current context lines
			// from each context from the training data
			CRFContext context = new CRFContext();

			// Reading in the training data file
			BufferedReader br = new BufferedReader(new FileReader(_trainFile));

			// Contains the current line in the training data
			String line;

			// Read the training data file till EOF
			while ((line = br.readLine()) != null) {
				// Omit leading and trailing whitespace
				line = line.trim();

				// If there is a empty line and we have context
				if (StringUtil.isEmpty(line) && context.size() != 0) {
					// Add the current feature line to the random data list
					randData.add(context);

					// Re-initialize the current feature line
					context = new CRFContext();
				} else {
					// Add the current context line
					context.addContext(line);
				}
			}

			// Close the buffered reader for the training file
			br.close();
		} catch (IOException e) {
			e.printStackTrace(LogUtil.getErrorStream(LOG));
		}

		// Seed
		long seed = 0;
		int minimum = 5;

		int totalCorrect = 0;
		int totalIncorrect = 0;

		double averageA = 0, averageB = 0, averageC = 0, averageD = 0;

		// 10x cross-validation
		int cv = 10;
		for (int i = 0; i < cv; i++) {
			System.out.println("### run " + (i + 1) + " (of " + cv
					+ " cross-validations) ###");

			// Every run gets a new, but defined seed value
			seed = new Random().nextInt(20) + minimum;

			// Random number generator using a single long seed
			Random randomGenerator = new Random(seed);

			// Randomly permute the specified list using the specified source of
			// randomness. All permutations occur with equal likelihood assuming
			// that the source of randomness is fair.
			// Randomize the data
			Collections.shuffle(randData, randomGenerator);

			// Generate the folds
			try {
				// Training file
				File trainFile = File.createTempFile("CRF_TrainFile", ".train"
						+ (i + 1), new File("tmp/"));

				// Model file
				File modelFile = File.createTempFile("CRF_ModelFile", ".model"
						+ (i + 1), new File("tmp/"));

				// Write the training folds
				BufferedWriter bwTrain = new BufferedWriter(new FileWriter(
						trainFile));

				// Contains the context for testing
				List<CRFContext> testingContext = new LinkedList<CRFContext>();

				int testingLines = 0;
				int trainingLines = 0;

				// Create training and testing file
				for (int j = 0; j < randData.size(); j++) {
					if (j % cv == i) {
						// Test
						testingContext.add(randData.get(j));
						testingLines++;
					} else {
						// Train
						String context = Utils.listToString(randData.get(j)
								.getContext(), Utils.NEWLINE);
						// Write context to file on disk
						bwTrain.write(context);

						// Insert a new line after a context
						bwTrain.write(Utils.NEWLINE + Utils.NEWLINE);

						trainingLines++;
					}
				}

				// Close
				bwTrain.close();

				// Overwrite the model file
				_modelFile = modelFile;

				// Overwrite the training file
				_trainFile = trainFile;

				System.out.println("Testing lines: " + testingLines
						+ "\tTraining lines: " + trainingLines);
				System.out.println("Train " + _trainFile.getAbsolutePath());
				System.out.println("Model " + _modelFile.getAbsolutePath());

				// ## TRAIN ##
				// Train the model
				training();

				// ## TEST ##
				List<ConditionalRandomFieldResult> results = testing(
						testingContext, true);

				System.out.println("EVAL RESULT PREDICT " + results.size());

				// If we have results
				if (results != null) {
					double correct = 0, total = 0, incorrect = 0;

					double a = 0, b = 0, c = 0, d = 0;

					for (int x = 0; x < results.size(); x++) {
						boolean authorLineIsCorrect = true;
						boolean titleLineIsCorrect = true;

						List<ConditionalRandomFieldResultLine> conditionalRandomFieldTestingLines = results
								.get(x).getConditionalRandomFieldResultLines();

						System.out.println("-- RESULT --");
						for (int y = 0; y < conditionalRandomFieldTestingLines
								.size(); y++) {

							ConditionalRandomFieldResultLine currentTag = conditionalRandomFieldTestingLines
									.get(y);

							String guess = currentTag.guessedLabelFromCRF;
							String answer = currentTag.labelInTrainingData;

							if (answer.equals("title") && guess.equals(answer)) {
								System.out.println(currentTag.word);
								a++;
								totalCorrect++;
								correct++;
							} else if (answer.equals("title")
									&& !guess.equals(answer)) {
								System.out.println("Incorrect: "
										+ currentTag.word + "   TITLE GUESS: "
										+ guess + "   ANSWER: " + answer);
								b++;
								totalIncorrect++;
								incorrect++;
								titleLineIsCorrect = false;
							}

							if (answer.equals("author") && guess.equals(answer)) {
								System.out.println(currentTag.word);
								d++;
								totalCorrect++;
								correct++;
							} else if (answer.equals("author")
									&& !guess.equals(answer)) {
								System.out.println("Incorrect: "
										+ currentTag.word + "  AUTHOR GUESS: "
										+ guess + "   ANSWER: " + answer);
								c++;
								totalIncorrect++;
								incorrect++;
								authorLineIsCorrect = false;
							}
						}

						System.out.println("-----------------");
						System.out.println();

						if (authorLineIsCorrect == true) {
							averageA++;
						} else {
							averageB++;
						}

						if (titleLineIsCorrect == true) {
							averageD++;
						} else {
							averageC++;
						}

						total++;
					}

					double accuracy = (a + d) / (a + b + c + d);
					double precision = a / (a + b);
					double recall = a / (a + c);
					double f1Measure = (2 * precision * recall)
							/ (precision + recall);

					System.out.println();
					System.out.println("=== EVALUATION RESULTS ==="
							+ System.getProperty("line.separator"));

					System.out.println("=== Confusion Matrix ===");
					System.out
							.println("\t\t\ttitle is correct\t\tauthor is correct\t<-- classified as");
					System.out.println("assigned title  |\t" + a + "\t\t\t\t"
							+ b);
					System.out.println("assigned author |\t" + c + "\t\t\t\t"
							+ d);
					System.out.println();
					System.out.println();
					System.out
							.println("Total lines:\t" + (int) (a + b + c + d));
					System.out.println("Total Positive:\t" + (int) (a + d));
					System.out.println("Total Negative:\t" + (int) (b + c));
					System.out.println();
					System.out.println("True Positive:\t" + (int) a
							+ "\tTrue Negative:\t" + (int) d
							+ "\nFalse Positive:\t" + (int) c
							+ "\tFalse Negative:\t" + (int) b);
					System.out.println();
					System.out.println("Total Correct:\t\t" + (int) correct);
					System.out.println("Total Incorrect:\t" + (int) incorrect);
					System.out.println();
					System.out.println("Accuracy:\t" + accuracy);
					System.out.println("Precision:\t" + precision);
					System.out.println("Recall:\t\t" + recall);
					System.out.println("F1 measure:\t" + f1Measure);
					System.out.println();
					System.out
							.println("=======================================================================");
					System.out.println();
				}

				// Delete temporary file when program exits.
				trainFile.deleteOnExit();
				modelFile.deleteOnExit();
			} catch (IOException e) {
				LOG.error("CRF EVAL: IOException");
				e.printStackTrace();
			}
		}

		System.out.println();
		System.out.println("=== Confusion Matrix AVERAGE ===");
		System.out
				.println("\t\t\ttitle is correct\t\tauthor is correct\t<-- classified as");
		System.out.println("assigned +title |\t" + averageA + "\t\t\t\t"
				+ averageB);
		System.out.println("assigned author |\t" + averageC + "\t\t\t\t"
				+ averageD);
		System.out.println();

		// Calculate the average
		averageA = averageA / cv;
		averageB = averageB / cv;
		averageC = averageC / cv;
		averageD = averageD / cv;

		double accuracy = (averageA + averageD)
				/ (averageA + averageB + averageC + averageD);
		double precision = averageA / (averageA + averageB);
		double recall = averageA / (averageA + averageC);
		double f1Measure = (2 * precision * recall) / (precision + recall);

		System.out.println();
		System.out.println();
		System.out.println("=== 10x cross-validation ===");
		System.out.println();
		System.out.println("DATA SIZE:\t\t" + randData.size());
		System.out.println("Total Correct:\t\t" + (int) totalCorrect);
		System.out.println("Total Incorrect:\t" + (int) totalIncorrect);
		System.out.println();
		System.out.println("Avg. Accuracy:\t\t" + accuracy);
		System.out.println("Avg. Precision:\t\t" + precision);
		System.out.println("Avg. Recall:\t\t" + recall);
		System.out.println("Avg. F1 measure:\t" + f1Measure);
		System.out.println();
		System.out
				.println("=======================================================================");
	}

	private CRFContext generateCRFContextLine(String str,
			String stylisticFeatures) {
		return generateCRFContextLine(str, stylisticFeatures, "");
	}

	private CRFContext generateCRFContextLine(String str,
			String stylisticFeatures, String annotationLabel) {
		// Switch feature ON or OFF
		String ON = "1";
		String OFF = "0";

		TokenizerME tokenizer = new TokenizerME(_maxentModel);

		// Tokenize the line string
		String[] tokens = tokenizer.tokenize(str);

		if (tokens != null && tokens.length > 0) {
			// Get the Part-Of-Speech tags for the tokens
			String[] tags = _postagger.tag(tokens);

			CRFContext context = new CRFContext();

			// For each token
			for (int i = 0; i < tokens.length; i++) {
				// Current token
				String currentToken = tokens[i];

				// Feature1: as-is
				String feature1 = currentToken;

				// Feature2: lowercased
				String feature2 = currentToken.toLowerCase();

				// Feature3: uppercased
				String feature3 = currentToken.toUpperCase();

				// Feature3: word contains number
				boolean wordIsNumber = true;

				for (char currentCharacter : currentToken.toCharArray()) {
					if (Character.isLetter(currentCharacter)) {
						wordIsNumber = false;
						break;
					}
				}

				/*
				String feature4 = wordIsNumber == true ? "wordIsNumber" + ON
						: "wordIsNumber" + OFF;
				*/
				
				String feature4 = wordIsNumber == true ? ON
						:  OFF;

				// Word appears in author dictionary
				//String feature5 = "author" + OFF;
				String feature5 = OFF;
				
				// Author feature
				try {
					// Author dictionary
					AuthorDictionarySingleton authorDictionary = AuthorDictionarySingleton
							.getInstance();

					// If the author dictionary contains
					// the current token
					if (authorDictionary != null
							&& authorDictionary.contains(currentToken)) {
						//feature5 = "author" + ON;
						feature5 =  ON;
					} else {
						//feature5 = "author" + OFF;
						feature5 = OFF;
					}
				} catch (Exception e) {
					// Error
					e.printStackTrace(LogUtil.getErrorStream(LOG));
				}

				// Feature5: Part-Of-Speech Tag
				String feature6 = "none";
				if (tags != null) {
					feature6 = tags[i];
				}

				// Feature6: Initial Caps?
				// Is current token upper case
				/*
				String feature7 = Character.isUpperCase(currentToken.charAt(0)) ? "initialCaps"
						+ ON
						: "initialCaps" + OFF;
						*/
				
				String feature7 = Character.isUpperCase(currentToken.charAt(0)) ? ON
						:  OFF;

				// Feature7: All Caps are uppercase?
				boolean allCaps = true;

				for (char currentCharacter : currentToken.toCharArray()) {
					if (!Character.isLetter(currentCharacter)
							|| Character.isLowerCase(currentCharacter)) {
						allCaps = false;
						break;
					}
				}

				/*
				String feature8 = allCaps == true ? "allCaps" + ON : "allCaps"
						+ OFF;
						*/

				String feature8 = allCaps == true ? ON : OFF;
				
				// Feature8: All Caps are uppercase?
				boolean onlyPunctiation = true;

				for (char currentCharacter : currentToken.toCharArray()) {
					if (Character.isLetter(currentCharacter)) {
						onlyPunctiation = false;
						break;
					}
				}

				/*
				String feature9 = onlyPunctiation == true ? "onlyPunctiation"
						+ ON : "onlyPunctiation" + OFF;
						*/

				String feature9 = onlyPunctiation == true ? ON :  OFF;
				
				// Word appears in title dictionary
				//String feature10 = "title" + OFF;
				
				String feature10 =  OFF;

				// Title feature
				try {
					// Title dictionary
					TitleDictionarySingleton titleDictionary = TitleDictionarySingleton
							.getInstance();

					// If the title dictionary
					// contains the current token
					if (titleDictionary != null
							&& titleDictionary.contains(currentToken)) {
						//feature10 = "title" + ON;
						feature10 = ON;
					} else {
						//feature10 = "title" + OFF;
						feature10 = OFF;
					}
				} catch (Exception e) {
					// Error
					e.printStackTrace(LogUtil.getErrorStream(LOG));
				}

				// Word appears in author dictionary
				//String feature11 = "affiliation" + OFF;
				String feature11 = OFF;
				
				
				// Affiliation feature
				try {
					// Affiliation dictionary
					AffiliationDictionarySingleton affiliationDictionary = AffiliationDictionarySingleton
							.getInstance();

					// If the affiliation dictionary
					// contains the current token
					if (affiliationDictionary != null
							&& affiliationDictionary.contains(currentToken)) {
						//feature11 = "affiliation" + ON;
						feature11 =  ON;
					} else {
						//feature11 = "affiliation" + OFF;
						feature11 = OFF;
					}
				} catch (Exception e) {
					// Error
					e.printStackTrace(LogUtil.getErrorStream(LOG));
				}

				StringBuilder featureLine = new StringBuilder();

				// Feature1: word as-is
				featureLine.append(feature1 + " ");

				// Feature2: word lowercased
				featureLine.append(feature2 + " ");

				// Feature2: word uppercased
				featureLine.append(feature3 + " ");

				// Feature3: token size
				//featureLine.append("tokenTextLength" + tokens.length + " ");
				featureLine.append(tokens.length + " ");

				// Feature: word contains number
				featureLine.append(feature4 + " ");

				// Feature4: word appears in author dictionary
				featureLine.append(feature5 + " ");

				// Feature5: Part-Of-Speech Tag
				featureLine.append(feature6 + " ");

				// Feature6: Initial Caps
				featureLine.append(feature7 + " ");

				// Feature7: All Uppercase
				featureLine.append(feature8 + " ");

				// Feature9: Only punctuation
				featureLine.append(feature9 + " ");

				// Token length
				//featureLine.append("lineTextLength" + str.length() + " ");
				featureLine.append(str.length() + " ");

				// Title diction
				featureLine.append(feature10 + " ");

				// Add stylistic features
				featureLine.append(stylisticFeatures + " ");

				// Affiliation
				featureLine.append(feature11);

				// Annotation label
				if (!StringUtil.isEmpty(annotationLabel)) {
					featureLine.append(" " + annotationLabel);
				}

				// Add context
				context.addContext(featureLine.toString());
			}

			return context;
		} else {
			return new CRFContext();
		}
	}

	public List<CRFContext> generateCRFContextFromAnnotation(Segment segment) {
		// If we have a segment without text
		if (segment.getSegmentTextSize() < 0) {
			// Discard it
			System.err.println("DISCARD SEGMENT BECAUSE IT DOES NOT CONTAIN ANY TEXT");
			LOG.info("DISCARD SEGMENT BECAUSE IT DOES NOT CONTAIN ANY TEXT");
			return new LinkedList<CRFContext>();
		} else {
			// Contains the CRF context
			List<CRFContext> finalContext = new LinkedList<CRFContext>();

			CRFContext currentContext = new CRFContext();

			// Flags for the labels
			boolean authorLabel = false;
			boolean titleLabel = false;
			String lastLabel = "";

			// For each segment text
			for (int j = 0; j < segment.getSegmentTextSize(); j++) {
				// Get the annotated label
				String currentSegmentTextLabel = segment.getSegmentTextLabel(j);

				if (!StringUtil.isEmpty(currentSegmentTextLabel)) {
					// Get the original text
					String currentSegmentText = segment.getSegmentText(j);

					// Get the previous sibling tag
					String stylisticFeatures = segment.getStylisticFeatures(j);

					// Add current context
					currentContext.addContext(generateCRFContextLine(
							currentSegmentText, stylisticFeatures,
							currentSegmentTextLabel).getContext());

					if (currentSegmentTextLabel.equals("author")
							|| currentSegmentTextLabel
									.equals("authorWithAffiliation")) {
						authorLabel = true;
					}

					if (currentSegmentTextLabel.equals("title")) {
						titleLabel = true;
					}

					// Split blocks with more labels
					if (!lastLabel.equals(currentSegmentTextLabel)
							&& authorLabel == true && titleLabel == true) {
						// Add the current context
						finalContext.add(currentContext);

						// New current context
						currentContext = new CRFContext();

						authorLabel = false;
						titleLabel = false;
						lastLabel = "";
					}

					// If we got two HTML break lines tags
					// or a HTML horizontal line tag.
					// The / character comes from the
					// HTMLSegmentCreator class / parseDOMTree method
					if (currentSegmentText.equals("\\\\")) {
						if (authorLabel == true && titleLabel == true) {
							// Add the current context
							finalContext.add(currentContext);

							// New current context
							currentContext = new CRFContext();

							authorLabel = false;
							titleLabel = false;
							lastLabel = "";
						}
					}
				}
			}

			// Return the context
			return finalContext;
		}
	}

	/**
	 * SYNCHONIZED because the Open-NLP tools are not thread safe -> took me a
	 * long time to figure it out :/
	 * 
	 * @param segment
	 * @param inAnnotationMode
	 * @return
	 */
	public List<CRFContext> generateCRFContext(Segment segment) {
		// If we have a segment without text
		if (segment.getSegmentTextSize() < 0) {
			// Discard it
			LOG.info("DISCARD SEGMENT BECAUSE IT DOES NOT CONTAIN ANY TEXT");
			return new LinkedList<CRFContext>();
		} else {
			// Contains the CRF context
			List<CRFContext> finalContext = new LinkedList<CRFContext>();

			CRFContext currentContext = new CRFContext();

			// For each segment text
			for (int j = 0; j < segment.getSegmentTextSize(); j++) {
				// Get the original text
				String currentSegmentText = segment.getSegmentText(j);

				// Get the previous sibling tag
				String stylisticFeatures = segment.getStylisticFeatures(j);

				// If we got two HTML break lines tags
				// or a HTML horizontal line tag.
				// The / character comes from the
				// HTMLSegmentCreator class / parseDOMTree method
				if (currentSegmentText.equals("\\\\")) {
					// Add the current context
					finalContext.add(currentContext);

					// New current context
					currentContext = new CRFContext();
				} else {
					// Add current context
					currentContext
							.addContext(generateCRFContextLine(
									currentSegmentText, stylisticFeatures)
									.getContext());
				}
			}

			// Add the CRF context for the current segment
			finalContext.add(currentContext);

			// Return the context
			return finalContext;
		}
	}

	public static void main(String[] args) {
		// New Conditional Random Field
		ConditionalRandomFieldSingleton crf = ConditionalRandomFieldSingleton
				.getInstance();

		// Train and create model
		System.out
				.println("Train and create the conditional random field model");
		crf.training();

		System.out.println();
		System.out
				.println("Do you like to evaluate the trained conditional random field now? (y/n)");

		Scanner in = new Scanner(System.in);

		// Get the answer from the command line
		String answer = in.nextLine();

		// Lowercase the answer
		answer = answer.trim().toLowerCase();

		// If the answer is yes
		if (answer.length() >= 0 && answer.charAt(0) == 'y') {
			// Evaluate the model
			crf.evaluate();
		}
	}
}