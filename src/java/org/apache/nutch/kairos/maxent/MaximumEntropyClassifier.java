package org.apache.nutch.kairos.maxent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import opennlp.maxent.BasicEventStream;
import opennlp.maxent.Event;
import opennlp.maxent.EventStream;
import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.MaxentModel;
import opennlp.maxent.OnePassRealValueDataIndexer;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.RealBasicEventStream;
import opennlp.maxent.io.GISModelWriter;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.util.LogUtil;

/**
 * Conference URL Maximum Entropy Classifier
 * 
 * @author Markus Haense
 */
public class MaximumEntropyClassifier {
	/**
	 * Logger
	 */
	public static final Log LOG = LogFactory
			.getLog(MaximumEntropyClassifier.class);

	/**
	 * Instance of the Singleton.
	 */
	private static MaximumEntropyClassifier _instance;

	/**
	 * Lock object to ensure we get an Singelton object when we work in a
	 * multithreaded environment.
	 */
	private static final Object _classLock = MaximumEntropyClassifier.class;

	/**
	 * The model name. The model name is used as a file name when we save the
	 * model to file on disk.
	 */
	private static final String _modelFileName = "maxent/maximumEntropyConferenceURLModel.txt";

	private static MaxentModel _maxentModel;

	// some parameters if you want to play around with the smoothing option
	// for model training. This can improve model accuracy, though training
	// will potentially take longer and use more memory. Model size will also
	// be larger. Initial testing indicates improvements for models built on
	// small data sets and few outcomes, but performance degradation for those
	// with large data sets and lots of outcomes.
	public static boolean USE_SMOOTHING = true;
	public static double SMOOTHING_OBSERVATION = 0.1;

	private MaximumEntropyClassifier() {
		// Singleton
	}

	/**
	 * Singleton
	 * 
	 * @return
	 * @throws Exception
	 */
	public static MaximumEntropyClassifier getInstance() throws Exception {
		synchronized (_classLock) {
			if (_instance == null) {
				LOG.info("Read in Maximum Entropy Conference Classifier Model");

				// Read in Maximum Entropy Conference Classifier Model
				_maxentModel = new SuffixSensitiveGISModelReader(new File(
						_modelFileName)).getModel();

				_instance = new MaximumEntropyClassifier();
			}

			// Return the singleton
			return _instance;
		}
	}

	public double predict(String predicates) {
		// Create the contexts
		String[] contexts = predicates.split(" ");

		double[] ocs = _maxentModel.eval(contexts);

		LOG.info("ocs " + ocs[0] + " " + ocs[1]);

		LOG.info("For predicates: " + predicates + " "
				+ _maxentModel.getAllOutcomes(ocs));

		return ocs[0];
	}

	public void evaluate() {
		try {
			// a (TP) b(FN) positive reference = a + b
			// c (FP) d(TN) negative reference = a + d
			// positive response = a + c
			// negative response = b+d
			// total = a + b + c + d

			// accuracy() = correct() / total()
			// precision() = truePositive() / positiveResponse()
			// recall() = truePositive() / positiveReference()
			// rejectionRecall() = trueNegative() / negativeReference()
			// rejectionPrecision() = trueNegative() / negativeResponse()

			// Contains each line of the training file
			//List<String> trainingDataList = Utils.readFile(new File(
			//		"maxent/trainingData.txt"));
			
			List<String> trainingDataListPositive = Utils.readFile(new File(
			"maxent/+conference/+conference.txt"));
			
			List<String> trainingDataListNegative = Utils.readFile(new File(
			"maxent/-conference/-conference.txt"));

			int trainingDataSize = trainingDataListPositive.size() + trainingDataListNegative.size();
			
			System.out.println("\t=> Training file has "
					+ (trainingDataSize) + " lines\n");

			long seed = 0;
			int minimum = 2;

			double averageA = 0, averageB = 0, averageC = 0, averageD = 0;

			
			List<String> wrongClassification = new LinkedList<String>();
			
			// 10x cross-validation
			int cv = 10;
			for (int i = 0; i < cv; i++) {
				// Every run gets a new, but defined seed value
				seed = new Random().nextInt(10) + minimum;

				// Random number generator using a single long seed
				Random randomGenerator = new Random(seed);

				// Randomly permute the specified list using the specified
				// source of randomness. All permutations occur with equal
				// likelihood assuming that the source of randomness is fair.
				// Randomize the data
				//Collections.shuffle(trainingDataList, randomGenerator);
				Collections.shuffle(trainingDataListPositive, randomGenerator);
				Collections.shuffle(trainingDataListNegative, randomGenerator);
				
				System.out.println("### run " + (i + 1) + " (of " + cv
						+ " cross-validations) ###");

				// Creating training
				File trainFile = File.createTempFile("trainFile", ".test"
						+ (i + 1), new File("tmp/"));

				// Create test file
				File testFile = File.createTempFile("testFile", ".train"
						+ (i + 1), new File("tmp/"));

				BufferedWriter bwTrain = new BufferedWriter(new FileWriter(
						trainFile));

				BufferedWriter bwTest = new BufferedWriter(new FileWriter(
						testFile));

				int testingLines = 0;
				int trainingLines = 0;

				// Create training and testing file
				for (int j = 0; j < trainingDataListPositive.size(); j++) {
					if (j % cv == i) {
						// Test
						bwTest.write(trainingDataListPositive.get(j) + Utils.NEWLINE); 
						bwTest.write(trainingDataListNegative.get(j) + Utils.NEWLINE); 
						
						testingLines++;
					} else {
						// Train
						bwTrain.write(trainingDataListPositive.get(j) + Utils.NEWLINE); 
						bwTrain.write(trainingDataListNegative.get(j) + Utils.NEWLINE); 
						
						trainingLines++;
					}
				}

				// Close
				bwTrain.close();
				bwTest.close();

				// Training file
				FileReader trainingData = new FileReader(trainFile);

				// Testing file
				FileReader testingData = new FileReader(testFile);

				// Train the model
				GISModel model = GIS.trainModel(new BasicEventStream(
						new PlainTextByLineDataStream(trainingData)), true);

				// Read in the testing data
				EventStream testDataEventStream = new BasicEventStream(
						new PlainTextByLineDataStream(testingData));

				double a = 0, b = 0, c = 0, d = 0;

				while (testDataEventStream.hasNext()) {
					Event currentEvent = testDataEventStream.nextEvent();

					// Model classified as
					String guess = model.getBestOutcome(model.eval(currentEvent
							.getContext()));

					//double[] ocs = model.eval(currentEvent.getContext());

					// Get the label from the training data
					String ans = currentEvent.getOutcome();

					if (ans.equals("+conference") && guess.equals(ans)) {
						a++;
					} else if (ans.equals("+conference") && !guess.equals(ans)) {
						b++;
					}

					if (ans.equals("-conference") && guess.equals(ans)) {
						d++;
					} else if (ans.equals("-conference") && !guess.equals(ans)) {
						wrongClassification.add("Guess: " + guess + "\n("
								+ currentEvent.toString() + ")\n");
						c++;
					}
				}

				// Delete temp file when program exits.
				trainFile.deleteOnExit();
				testFile.deleteOnExit();

				double accuracy = (a + d) / (a + b + c + d);
				double precision = a / (a + b);
				double recall = a / (a + c);
				double f1Measure = (2 * precision * recall)
						/ (precision + recall);

				// Add to average
				averageA += a;
				averageB += b;
				averageC += c;
				averageD += d;

				System.out.println();
				System.out.println("=== EVALUATION RESULTS ==="
						+ Utils.NEWLINE);

				System.out.println("=== Confusion Matrix ===");
				System.out
						.println("\t\t\t+conference is correct\t\t-conference is correct\t<-- classified as");
				System.out.println("assigned +conference |\t" + a + "\t\t\t\t"
						+ b);
				System.out.println("assigned -conference |\t" + c + "\t\t\t\t"
						+ d);
				System.out.println();
				System.out.println();
				System.out.println("Total lines:\t" + (int) (a + b + c + d));
				System.out.println("Total Positive:\t" + (int) (a + d));
				System.out.println("Total Negative:\t" + (int) (b + c));
				System.out.println();
				System.out.println("True Positive:\t" + (int) a
						+ "\tTrue Negative:\t" + (int) d
						+ "\nFalse Positive:\t" + (int) c
						+ "\tFalse Negative:\t" + (int) b + "\n");
				System.out.println("Accuracy:\t" + accuracy);
				System.out.println("Precision:\t" + precision);
				System.out.println("Recall:\t\t" + recall);
				System.out.println("F1 measure:\t" + f1Measure);
				System.out.println();
				System.out
						.println("=======================================================================");
				System.out.println();
			}

			
			System.out.println();
			System.out.println("=== Confusion Matrix ===");
			System.out
					.println("\t\t\t+conference is correct\t\t-conference is correct\t<-- classified as");
			System.out.println("assigned +conference |\t" + averageA + "\t\t\t\t"
					+ averageB);
			System.out.println("assigned -conference |\t" + averageC + "\t\t\t\t"
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

			
			/*
			for (String string : wrongClassification) {
				System.out.println(string);
			}
			*/
			
			System.out.println();
			System.out.println();
			System.out.println("=== 10x cross-validation ===");
			System.out.println();
			System.out.println("Avg. Accuracy:\t\t" + accuracy);
			System.out.println("Avg. Precision:\t\t" + precision);
			System.out.println("Avg. Recall:\t\t" + recall);
			System.out.println("Avg. F1 measure:\t" + f1Measure);
			System.out.println();
			System.out
					.println("=======================================================================");
			
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void training() {
		boolean real = false;

		try {
			// The training data
			FileReader trainingData = new FileReader(new File(
					"maxent/trainingData.txt"));

			EventStream es;

			if (!real) {
				es = new BasicEventStream(new PlainTextByLineDataStream(
						trainingData));
			} else {
				es = new RealBasicEventStream(new PlainTextByLineDataStream(
						trainingData));
			}

			GIS.SMOOTHING_OBSERVATION = SMOOTHING_OBSERVATION;

			GISModel model;

			if (!real) {
				model = GIS.trainModel(es, USE_SMOOTHING);
			} else {
				model = GIS.trainModel(100, new OnePassRealValueDataIndexer(es,
						0), USE_SMOOTHING);
			}

			// The model file
			File modelOutputFile = new File(
					"maxent/maximumEntropyConferenceURLModel.txt");

			// Write the model to file
			GISModelWriter writer = new SuffixSensitiveGISModelWriter(model,
					modelOutputFile);
			writer.persist();
		} catch (Exception e) {
			e.printStackTrace(LogUtil.getErrorStream(LOG)); 
		}
	}

	public static void main(String[] args) throws Exception {
		// New Conditional Random Field
		MaximumEntropyClassifier maxent = MaximumEntropyClassifier
				.getInstance();

		// Train and create model
		System.out.println("Train and create the maximum entropy model");
		maxent.training();

		System.out.println();
		System.out
				.println("Do you like to evaluate the trained maximum entropy model now? (y/n)");

		Scanner in = new Scanner(System.in);

		// Get the answer from the command line
		String answer = in.nextLine();

		// Lowercase the answer
		answer = answer.trim().toLowerCase();

		// If the answer is yes
		if (answer.length() >= 0 && answer.charAt(0) == 'y') {
			// Evaluate the model
			maxent.evaluate();
		}
	}
}