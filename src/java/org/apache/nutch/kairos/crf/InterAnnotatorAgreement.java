package org.apache.nutch.kairos.crf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Calculates the accuracy of the Inter Annotator Agreement
 * 
 * The YAML files from the annotators are evaluated against the annotated
 * repository
 * 
 * @author Markus Haense
 */
public class InterAnnotatorAgreement {

	/**
	 * Reads in a file
	 * 
	 * @param file
	 * @return
	 */
	public static List<String> readFile(File file) {
		try {
			List<String> lines = new LinkedList<String>();

			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				// Skip comments
				if (!line.startsWith("#")) {
					// Omit leading and trailing whitespace
					line = line.trim();

					// Process spans
					if (line.startsWith("-")) {
						// Split the current line in the YAML file
						String[] split = line.split(" ");

						// If we have a span and a label
						if (split.length == 2) {

							// Get the label
							String label = split[1];
							label = label.trim();

							if (label.length() > 0) {
								lines.add(label);
							}
						}
					}
				}
			}

			// Close the buffered reader
			br.close();

			return lines;
		} catch (IOException e) {
			return new LinkedList<String>();
		}
	}

	public static void main(String[] args) {
		// Path to the Inter Annotator directory
		File annotatorDir = new File(
				"kairos/annotation/interAnnotatorAgreement/");

		// Path to the repository directory
		File repositoryDir = new File(
				"kairos/annotation/annotated_repository/");

		// Filename filter for YAML files
		FilenameFilter yamlFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".yaml");
			}
		};

		// Do not return any files that start with `.'.
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.startsWith(".");
			}
		};

		// Get the inter annotator direcotries
		String[] annotatorDirectories = annotatorDir.list(filter);

		double averageAccuracy = 0;

		for (String currentAnnotatorDirectoryPath : annotatorDirectories) {

			File currentAnnotatorDirectory = new File(annotatorDir + "/"
					+ currentAnnotatorDirectoryPath);

			// Get the annotated YAML files
			File[] yamlFiles = currentAnnotatorDirectory.listFiles(yamlFilter);

			// Inter Annotator Agreement
			int correct = 0;
			int incorrect = 0;
			int total = 0;

			for (File currentYAMLFile : yamlFiles) {

				File repositoryYamlFile = new File(repositoryDir + "/"
						+ currentYAMLFile.getName());

				List<String> currentAnnotatorYamlFile = readFile(currentYAMLFile);
				List<String> currentRepositoryYamlFile = readFile(repositoryYamlFile);

				if (currentAnnotatorYamlFile.size() == currentRepositoryYamlFile
						.size()) {

					for (int i = 0; i < currentAnnotatorYamlFile.size(); i++) {
						String labelInAnnotatorYamlFile = currentAnnotatorYamlFile
								.get(i);

						String labelInRepositoryYamlFile = currentRepositoryYamlFile
								.get(i);

						if (labelInAnnotatorYamlFile
								.equals(labelInRepositoryYamlFile)) {
							correct++;
						} else {
							incorrect++;
							System.out.println();
							System.out.println("INCORRECT: ["
									+ labelInAnnotatorYamlFile + "] ["
									+ labelInRepositoryYamlFile + "] "
									+ System.getProperty("line.separator")
									+ "\t" + currentYAMLFile.getAbsolutePath()
									+ System.getProperty("line.separator")
									+ "\tLINE: " + i);
						}

						total++;
					}
				} else {
					System.err.println("YAML FILES ARE NOT IDENTICAL:"
							+ System.getProperty("line.separator") + "\t["
							+ currentYAMLFile.getAbsolutePath() + "]"
							+ System.getProperty("line.separator") + "\t["
							+ repositoryYamlFile.getAbsolutePath() + "]");
				}
			}

			// Calculate the accuracy
			double accuracy = (double) correct / total;

			averageAccuracy += accuracy;

			System.out.println();
			System.out.println("=== Inter Annotator Agreement ===");
			System.out.println();
			System.out.println(currentAnnotatorDirectory.getAbsolutePath());
			System.out.println();
			System.out.println("Accuracy:\t" + accuracy);
			System.out.println();
			System.out.println("=================================");
			System.out.println();
			System.out.println();
			System.out.println();
		}

		// Calculate the average accuracy
		double accuracy = (double) averageAccuracy
				/ annotatorDirectories.length;

		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("=== AVERAGE Inter Annotator Agreement ===");
		System.out.println();
		System.out.println("AVERAGE Accuracy:\t" + accuracy);
		System.out.println();
		System.out.println("=========================================");
		System.out.println();
		System.out.println();

		double a = 0, b = 0, c = 0, d = 0, e = 0;
		double f = 0, g = 0, h = 0, i = 0, j = 0;
		double k = 0, l = 0, m = 0, n = 0, o = 0;
		double p = 0, q = 0, r = 0, s = 0, t = 0;
		double u = 0, v = 0, w = 0, x = 0, y = 0;

		for (String currentAnnotatorDirectoryPath : annotatorDirectories) {

			File currentAnnotatorDirectory = new File(annotatorDir + "/"
					+ currentAnnotatorDirectoryPath);

			// Get the annotated YAML files
			File[] yamlFiles = currentAnnotatorDirectory.listFiles(yamlFilter);

			for (File currentYAMLFile : yamlFiles) {

				File repositoryYamlFile = new File(repositoryDir + "/"
						+ currentYAMLFile.getName());

				List<String> currentAnnotatorYamlFile = readFile(currentYAMLFile);
				List<String> currentRepositoryYamlFile = readFile(repositoryYamlFile);

				if (currentAnnotatorYamlFile.size() == currentRepositoryYamlFile
						.size()) {

					for (int z = 0; z < currentAnnotatorYamlFile.size(); z++) {
						String guess = currentAnnotatorYamlFile.get(z);
						String answer = currentRepositoryYamlFile.get(z);

						if (answer.equals("title") && guess.equals(answer)) {
							a++;
						} else if (answer.equals("title")
								&& guess.equals("author")) {
							b++;
						} else if (answer.equals("title")
								&& guess.equals("authorWithAffiliation")) {
							c++;
						} else if (answer.equals("title")
								&& guess.equals("affiliation")) {
							d++;
						} else if (answer.equals("title") && guess.equals("na")) {
							e++;
						}

						if (answer.equals("author") && guess.equals(answer)) {
							g++;
						} else if (answer.equals("author")
								&& guess.equals("title")) {
							f++;
						} else if (answer.equals("author")
								&& guess.equals("authorWithAffiliation")) {
							h++;
						} else if (answer.equals("author")
								&& guess.equals("affiliation")) {
							i++;
						} else if (answer.equals("author")
								&& guess.equals("na")) {
							j++;
						}

						if (answer.equals("authorWithAffiliation")
								&& guess.equals(answer)) {
							m++;
						} else if (answer.equals("authorWithAffiliation")
								&& guess.equals("title")) {
							k++;
						} else if (answer.equals("authorWithAffiliation")
								&& guess.equals("author")) {
							l++;
						} else if (answer.equals("authorWithAffiliation")
								&& guess.equals("affiliation")) {
							n++;
						} else if (answer.equals("authorWithAffiliation")
								&& guess.equals("na")) {
							o++;
						}

						if (answer.equals("affiliation")
								&& guess.equals(answer)) {
							s++;
						} else if (answer.equals("affiliation")
								&& guess.equals("title")) {
							p++;
						} else if (answer.equals("affiliation")
								&& guess.equals("author")) {
							q++;
						} else if (answer.equals("affiliation")
								&& guess.equals("authorWithAffiliation")) {
							r++;
						} else if (answer.equals("affiliation")
								&& guess.equals("na")) {
							t++;
						}

						if (answer.equals("na") && guess.equals(answer)) {
							y++;
						} else if (answer.equals("na") && guess.equals("title")) {
							u++;
						} else if (answer.equals("na")
								&& guess.equals("author")) {
							v++;
						} else if (answer.equals("na")
								&& guess.equals("authorWithAffiliation")) {
							w++;
						} else if (answer.equals("na")
								&& guess.equals("affiliation")) {
							x++;
						}
					}
				}
			}
		}

		double total = a + b + c + d + e + f + g + h + i + j + k + l + m + n
				+ o + p + q + r + s + t + u + v + w + x + y;

		double agreement = a + g + m + s + y;

		double p0 = agreement / total;

		double pc1 = (((a + f + k + p + u) / total) * ((a + b + c + d + e) / total));
		double pc2 = (((b + g + l + q + v) / total) * ((f + g + h + i + j) / total));
		double pc3 = (((c + h + m + r + w) / total) * ((k + l + m + n + o) / total));
		double pc4 = (((d + i + n + s + x) / total) * ((p + q + r + s + t) / total));
		double pc5 = (((e + j + o + t + y) / total) * ((u + v + w + x + y) / total));

		double pc = pc1 + pc2 + pc3 + pc4 + pc5;

		double kappa = (p0 - pc) / (1 - pc);

		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("=== KAPPA STATISTIC ===");
		System.out.println();

		System.out.println(a + "\t" + b + "\t" + c + "\t" + d + "\t" + e
				+ "\t\t| " + (a + b + c + d + e));
		System.out.println(f + "\t" + g + "\t" + h + "\t" + i + "\t" + j
				+ "\t\t| " + (f + g + h + i + j));
		System.out.println(k + "\t" + l + "\t" + m + "\t" + n + "\t" + o
				+ "\t\t| " + (k + l + m + n + o));
		System.out.println(p + "\t" + q + "\t" + r + "\t" + s + "\t" + t
				+ "\t\t| " + (p + q + r + s + t));
		System.out.println(u + "\t" + v + "\t" + w + "\t" + x + "\t" + y
				+ "\t\t| " + (u + v + w + x + y));
		System.out
				.println("================================================================");
		System.out.println((a + f + k + p + u) + "\t" + (b + g + l + q + v)
				+ "\t" + (c + h + m + r + w) + "\t" + (d + i + n + s + x)
				+ "\t" + (e + j + o + t + y) + "\t\t| " + total);

		System.out.println();
		System.out.println();

		if (kappa < 0) {
			System.out.println("No agreement: " + kappa);
		} else if (kappa <= 0.20) {
			System.out.println("Slight agreement: " + kappa);
		} else if (kappa <= 0.40) {
			System.out.println("Fair agreement: " + kappa);
		} else if (kappa <= 0.60) {
			System.out.println("Moderate agreement: " + kappa);
		} else if (kappa <= 0.80) {
			System.out.println("Substantial agreement: " + kappa);
		} else if (kappa >= 0.81) {
			System.out.println("Almost perfect agreement: " + kappa);
		}

		System.out.println();

		System.out.println("Kappa:\t" + kappa);
		System.out.println();
		System.out.println("=========================================");
		System.out.println();
		System.out.println();

		int titleLines = 0;
		int authorLines = 0;
		int authorWithAffilitionLines = 0;
		int affiliationLines = 0;
		int naLines = 0;
		
		// Get the annotated YAML files
		File[] yamlRepositoryFiles = repositoryDir.listFiles(yamlFilter);

		for (File currentYAMLFile : yamlRepositoryFiles) {

			List<String> currentAnnotatorYamlFile = readFile(currentYAMLFile);

			for (int z = 0; z < currentAnnotatorYamlFile.size(); z++) {
				String labelInAnnotatorYamlFile = currentAnnotatorYamlFile
						.get(z);
				
				if(labelInAnnotatorYamlFile.equals("title")) {
					titleLines++;
				}
				
				if(labelInAnnotatorYamlFile.equals("author")) {
					authorLines++;
				}
				
				if(labelInAnnotatorYamlFile.equals("authorWithAffiliation")) {
					authorWithAffilitionLines++;
				}
				
				if(labelInAnnotatorYamlFile.equals("affiliation")) {
					affiliationLines++;
				}
				
				if(labelInAnnotatorYamlFile.equals("na")) {
					naLines++;
				}
			}
		}
		
		System.out.println("Corpus contains:");
		System.out.println("Title lines: " + titleLines);
		System.out.println("Author lines: " + authorLines);
		System.out.println("Author with affiliation: " + authorWithAffilitionLines);
		System.out.println("Affiliation lines: " + affiliationLines);
		System.out.println("N/A lines: " + naLines);
	}
}