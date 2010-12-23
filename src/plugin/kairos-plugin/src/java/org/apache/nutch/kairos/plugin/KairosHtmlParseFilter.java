package org.apache.nutch.kairos.plugin;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.kairos.crf.CRFContext;
import org.apache.nutch.kairos.crf.ConditionalRandomFieldResult;
import org.apache.nutch.kairos.crf.ConditionalRandomFieldResultLine;
import org.apache.nutch.kairos.crf.ConditionalRandomFieldSingleton;
import org.apache.nutch.kairos.crf.Segment;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.util.LogUtil;
import org.w3c.dom.DocumentFragment;

/**
 * This is the source code for the HTML Parser extension. It tries to identify
 * conference paper metadata such as author(s), title(s), with a Conditional
 * Random Field (CRF) model. It create segments from an HTML web page. Segments
 * are list, paragraph or table entries.
 * 
 * @author Markus Haense
 */
public class KairosHtmlParseFilter implements HtmlParseFilter {

	/**
	 * Logger
	 */
	private static final Log LOG = LogFactory
			.getLog(KairosHtmlParseFilter.class.getName());

	/**
	 * Configuration
	 */
	private Configuration conf;

	private static NegativeDictionarySingleton negativeDictionary = NegativeDictionarySingleton
			.getInstance();

	static {
		try {
			negativeDictionary.read("kairos/dictionaries/negative.txt");
		} catch (Exception e) {
			// Error
			e.printStackTrace(LogUtil.getErrorStream(LOG));
		}
	}

	/**
	 * Scan the HTML document for paper metadata
	 */
	public ParseResult filter(Content content, ParseResult parseResult,
			HTMLMetaTags metaTags, DocumentFragment doc) {

		Metadata metadata = content.getMetadata();
		metadata.set("Content-Type", "text/html");

		// Get the content base URL
		String contentUrl = content.getUrl();

		// Contains the metadata
		StringBuilder indexText = new StringBuilder();

		// Parse Data
		ParseData parseData = parseResult.get(contentUrl).getData();

		// New HTML Segment Creator
		HTMLSegmentCreator htmlSegmentCreator = new HTMLSegmentCreator();

		// List of HTML segments
		List<Segment> segmentList = htmlSegmentCreator.createHTMLSegments(doc);

		// LOG.info("SEGMENT SIZE [" + segmentList.size() + "]");

		// New Conditional Random Field
		ConditionalRandomFieldSingleton crf = ConditionalRandomFieldSingleton
				.getInstance();

		// For each segment in the segment list
		for (int j = 0; j < segmentList.size(); j++) {
			// Get the current segment
			Segment currentSegment = segmentList.get(j);

			// Get the context for CRF
			List<CRFContext> context = crf.generateCRFContext(currentSegment);

			// Predict label for feature lines
			List<ConditionalRandomFieldResult> conditionalRandomFieldResults = crf
					.testing(context, false);

			if (conditionalRandomFieldResults.size() != 0) {
				// For each result from the Conditional Random Field
				for (int x = 0; x < conditionalRandomFieldResults.size(); x++) {
					// Get the conditional output probability
					// for the current result
					double currentConditionalOutputProbability = conditionalRandomFieldResults
							.get(x).getConditionalOutputProbability();

					// If the conditional output probability
					// is higher than 85%
					if (currentConditionalOutputProbability >= 0.75) {
						// Get the Conditional Random Field Testing Lines
						List<ConditionalRandomFieldResultLine> conditionalRandomFieldTestingLines = conditionalRandomFieldResults
								.get(x).getConditionalRandomFieldResultLines();

						// Contains the title words / tokens
						List<String> title = new LinkedList<String>();

						// Contains the author words / tokens
						List<String> author = new LinkedList<String>();

						// Contains the affiliation words / tokens
						List<String> affiliation = new LinkedList<String>();

						// For each CRF testing line
						for (int y = 0; y < conditionalRandomFieldTestingLines
								.size(); y++) {
							// Get the current tag
							ConditionalRandomFieldResultLine currentTag = conditionalRandomFieldTestingLines
									.get(y);

							// Get the guessed label from CRF
							String guess = currentTag.guessedLabelFromCRF;

							// double marginalProbability =
							// currentTag.marginalProbability;

							String word = currentTag.word;

							boolean isNegativeWord = negativeDictionary
									.isNegativeWord(word);

						        //System.err.println(word + " " + guess + " " + currentTag.tokens[5] + " " + currentTag.tokens[20] + " " + isNegativeWord);
							if (isNegativeWord == true) {
								continue;
							}

							// If the guessed label equals title + some
							// heuristics
							if (guess.equals("title")) {
								// Add the word / token to the title
								title.add(word);
							}

							// If the guessed label equals author + some
							// heuristics
							if (guess.equals("author")) {
                                                          if (
                                                              // Thang v110101: add separators for author fields
                                                              word.equals(",") || word.equals("and") || 
                                                                (
                                                                 currentTag.tokens[5].equals("1")
								&& !currentTag.tokens[20].equals("1")
                                                                )
                                                             ) {
									// Add the word / token to the author
                                                                  author.add(word);
								}

								if (currentTag.tokens[20].equals("1")) {
									affiliation.add(word);
								}
							}
						}


                                                // Thang v110101: filter spurious authors, titles
                                                if(author.size() > 0){
                                                  author = filterAuthorArray(author);
                                                } 
                                                if(title.size() > 0){
                                                  title = filterTitleArray(title);
                                                }

                                                // Thang v110101: (mean, stddev) of title token count (without punctuation) = (7.91568836712914, 3.02241260769559), of function word in title = (2.02774813233725, 1.38724291087029)
                                                // Statistics derived from 935 headers in ParsCit database
                                                int[] counts = countTitleToken(title);
                                                int titleCount = counts[0];
                                                int funcCount = counts[1];
 
                                                // If we have a title and an author
						if (titleCount >= 5 && titleCount <= 17 && funcCount <= 5 && author.size() >= 2) { // Thang: titleCount within (mean - stddev, mean + 3 stddev), funcWord <= mean + 3 stddev
							// Author
							indexText.append(Utils.listToString(author, " "));
							indexText.append(Utils.NEWLINE);

							// Title
							indexText.append(Utils.listToString(title, " "));

							if (affiliation.size() > 0) {
								indexText.append(Utils.NEWLINE);

								// Affiliation
								indexText.append(Utils.listToString(
										affiliation, " "));
							}

							// Empty line
							indexText.append(Utils.NEWLINE);
							indexText.append(Utils.NEWLINE);

							/*
							 * LOG.info("ADDING METADATA: PROB: " +
							 * currentConditionalOutputProbability +
							 * " [segment:" + j + "| result:" + x + " size:" +
							 * currentSegment.getSegmentTextSize() + "] : (" +
							 * indexText.toString());
							 */
                                                }
					} else {
                                                /*
						 * LOG.info("IGNORE METADATA: PROB: " +
						 * currentConditionalOutputProbability + " [segment:" +
						 * j + "| result:" + x + " size:" +
						 * currentSegment.getSegmentTextSize() + "] : (" +
						 * currentSegment.getSegmentTextConcatenated() +
						 * ")  URL: " + contentUrl);
						 */
					}
				}
			}
		}

		return ParseResult.createParseResult(content.getUrl(), new ParseImpl(
				indexText.toString(), parseData));
	}

        // Thang v110101: function word list for filtering purpose
        public final static String[] funcArray = {"a", "about", "above", "after", "again", "ago", "all", "almost", "along", "already", "also", "although", "always", "am", "among", "an", "and", "another", "any", "anybody", "anything", "anywhere", "are", "aren't", "around", "as", "at", "back", "else", "be", "been", "before", "being", "below", "beneath", "beside", "between", "beyond", "billion", "billionth", "both", "each", "but", "by", "can", "can't", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "done", "don't", "down", "during", "eight", "eighteen", "eighteenth", "eighth", "eightieth", "eighty", "either", "eleven", "eleventh", "enough", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "except", "far", "few", "fewer", "fifteen", "fifteenth", "fifth", "fiftieth", "fifty", "first", "five", "for", "fortieth", "forty", "four", "fourteen", "fourteenth", "fourth", "hundred", "from", "get", "gets", "getting", "got", "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "hence", "her", "here", "hers", "herself", "he's", "him", "himself", "his", "hither", "how", "however", "near", "hundredth", "i", "i'd", "if", "i'll", "i'm", "in", "into", "is", "i've", "isn't", "it", "its", "it's", "itself", "last", "less", "many", "me", "may", "might", "million", "millionth", "mine", "more", "most", "much", "must", "mustn't", "my", "myself", "near", "nearby", "nearly", "neither", "never", "next", "nine", "nineteen", "nineteenth", "ninetieth", "ninety", "ninth", "no", "nobody", "none", "noone", "nothing", "nor", "not", "now", "nowhere", "of", "off", "often", "on", "or", "once", "one", "only", "other", "others", "ought", "oughtn't", "our", "ours", "ourselves", "out", "over", "quite", "rather", "round", "second", "seven", "seventeen", "seventeenth", "seventh", "seventieth", "seventy", "shall", "shan't", "she'd", "she", "she'll", "she's", "should", "shouldn't", "since", "six", "sixteen", "sixteenth", "sixth", "sixtieth", "sixty", "so", "some", "somebody", "someone", "something", "sometimes", "somewhere", "soon", "still", "such", "ten", "tenth", "than", "that", "that", "that's", "the", "their", "theirs", "them", "themselves", "these", "then", "thence", "there", "therefore", "they", "they'd", "they'll", "they're", "third", "thirteen", "thirteenth", "thirtieth", "thirty", "this", "thither", "those", "though", "thousand", "thousandth", "three", "thrice", "through", "thus", "till", "to", "towards", "today", "tomorrow", "too", "twelfth", "twelve", "twentieth", "twenty", "twice", "two", "under", "underneath", "unless", "until", "up", "us", "very", "when", "was", "wasn't", "we", "we'd", "we'll", "were", "we're", "weren't", "we've", "what", "whence", "where", "whereas", "which", "while", "whither", "who", "whom", "whose", "why", "will", "with", "within", "without", "won't", "would", "wouldn't", "yes", "yesterday", "yet", "you", "your", "you'd", "you'll", "you're", "yours", "yourself", "yourselves", "you've"};
        public final static Map<String, Boolean> funcMap = new HashMap<String, Boolean>()
        {
          {
            for(String func : funcArray){
              put(func, true);
            }
          }
        };

        /**
         * Thang v110101: filter author array by removing function words (except "and"), remove leading and trailing commas, then remove consecutive "," or "and"
         **/
        private List<String> filterAuthorArray(List<String> author){
          List<String> newAuthor = new LinkedList<String>();

          String prevToken = "";
          for(String word : author){
            String lcWord = word.toLowerCase();
          
            // add only if not a function word except from "and")
            if(lcWord.equals("and") || !funcMap.containsKey(lcWord)){
              // canonicalize "and" -> ","
              if(lcWord.equals("and")){
                word = ",";
              }
             
              // add only if no consecutive ","
              if(!word.equals(",") || !prevToken.equals(",")){
                newAuthor.add(word);
                prevToken = word;
              }
            }
          }

          // remove leading, trailing ","
          removeCommas(newAuthor);

          return newAuthor;
        }

        /**
         * Thang v110101: filter title array by removing non-word tokens, leading and trailing commas
         **/
        private List<String> filterTitleArray(List<String> title){
          int titleSize = title.size();

          while(titleSize > 0 && 
                title.get(0).length() > 1 // to prevent from removing punctuation such as ","
                && (
                title.get(0).matches("[^a-zA-Z]+") // no alphabetical at all
                || title.get(0).matches(".*[^a-zA-Z]+[a-zA-Z]+[^a-zA-Z]+.*") // multiple non-alphabetical character 
                )){
            title.remove(0);
            titleSize--;
          }
         
          // removing leading and trailing commas
          removeCommas(title);

          // check if all tokens are lowercase, remove
          boolean isAllLowercase = true;
          for(String token : title){
            if(token.matches("[A-Z].*")){
              isAllLowercase = false;
            }
          }
          if(isAllLowercase){
            title.clear();
          }

          return title;
        }

        /**
         * Thang v110101: count # title tokens (without punctuation) && the number of function word 
         **/
        private int[] countTitleToken(List<String> title) {
          String titleStr = Utils.listToString(title, " ");
          titleStr = titleStr.replaceAll("\\p{Punct}", "");
          String[] tokens = titleStr.split("\\s+");
          int titleCount = tokens.length;

          int funcWordCount = 0;
          for (String token : tokens){
            if(funcMap.containsKey(token.toLowerCase())){
              funcWordCount++;
            }
          }

          return new int[]{titleCount, funcWordCount};
        }

         /**
         * Thang v110101: remove leading and trailing commas 
         **/
        private int removeCommas(List<String> tokens) {
          int tokenSize = tokens.size();
          while(tokenSize > 0 && tokens.get(0).equals(",")){
            tokens.remove(0);
            tokenSize--;
          }

          while(tokenSize > 0 && tokens.get(tokenSize -1).equals(",")){
            tokens.remove(tokenSize-1);
            tokenSize--;
          }

          return tokenSize;
        }

	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	public Configuration getConf() {
		return this.conf;
	}
}
