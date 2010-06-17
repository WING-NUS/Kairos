package org.apache.nutch.kairos.plugin;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

import org.apache.axis.encoding.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.net.protocols.Response;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.parse.ParseStatus;
import org.apache.nutch.parse.Parser;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.util.LogUtil;
import org.apache.nutch.util.StringUtil;
import org.pdfbox.exceptions.CryptographyException;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.encryption.BadSecurityHandlerException;
import org.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import org.pdfbox.util.PDFTextStripper;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import forecite.ForeCiteLocator;
import forecite.ForeCitePortType;

/**
 * DELETE!!!!!!!!!!!!!!!
 * 
 * 
 * @author Markus Haense
 */
public class ParsCitPdfParser implements Parser {

	/**
	 * Logger
	 */
	private static final Log LOG = LogFactory.getLog(ParsCitPdfParser.class
			.getName());

	/**
	 * Confidence for ParsCit output
	 */
	private static final double CONFIDENCE = 0.85;

	/**
	 * Configuration
	 */
	private Configuration conf;

	public static List<String> getElementsByTagName(Document doc, String name) {
		if (name != null && name.length() > 0) {
			List<String> dataElementList = new LinkedList<String>();

			NodeList nodeList = doc.getElementsByTagName(name);

			for (int i = 0; i < nodeList.getLength(); i++) {
				Element element = (Element) nodeList.item(i);

				double confidence = element == null ? 0 : Double
						.parseDouble(element.getAttribute("confidence"));

				if (confidence >= CONFIDENCE) {
					String data = getCharacterDataFromElement(element);
					data = data.trim();

					if ((!StringUtil.isEmpty(data))) {
						dataElementList.add(data);
					}
				}
			}

			return dataElementList;
		} else {
			return new LinkedList<String>();
		}
	}

	public ParseResult getParse(Content content) {
		// in memory representation of pdf file
		PDDocument pdf = null;

		String text = null;
		String title = null;
		Metadata metadata = new Metadata();

		// Remove all other metadata
		for (String currentName : metadata.names()) {
			metadata.remove(currentName);
		}

		// No metadata found
		metadata.set("METADATA", "0");

		try {
			byte[] raw = content.getContent();

			String contentLength = content.getMetadata().get(
					Response.CONTENT_LENGTH);

			if (contentLength != null
					&& raw.length != Integer.parseInt(contentLength)) {
				return new ParseStatus(
						ParseStatus.FAILED,
						ParseStatus.FAILED_TRUNCATED,
						"Content truncated at "
								+ raw.length
								+ " bytes. Parser can't handle incomplete pdf file.")
						.getEmptyParseResult(content.getUrl(), getConf());
			}

			PDFParser parser = new PDFParser(new ByteArrayInputStream(raw));
			parser.parse();

			pdf = parser.getPDDocument();

			if (pdf.isEncrypted()) {
				// Just try using the default password and move on
				pdf.openProtection(new StandardDecryptionMaterial(""));
			}

			// Collect text
			PDFTextStripper stripper = new PDFTextStripper();
			text = stripper.getText(pdf);
		} catch (CryptographyException e) {
			return new ParseStatus(ParseStatus.FAILED,
					"Error decrypting document. " + e).getEmptyParseResult(
					content.getUrl(), getConf());
		} catch (BadSecurityHandlerException e) {
			return new ParseStatus(ParseStatus.FAILED,
					"Error decrypting document. " + e).getEmptyParseResult(
					content.getUrl(), getConf());
		} catch (Exception e) { // run time exception
			if (LOG.isWarnEnabled()) {
				LOG.warn("General exception in PDF parser: " + e.getMessage());
				e.printStackTrace(LogUtil.getWarnStream(LOG));
			}
			return new ParseStatus(ParseStatus.FAILED,
					"Can't be handled as pdf document. " + e)
					.getEmptyParseResult(content.getUrl(), getConf());
		} finally {
			try {
				if (pdf != null)
					pdf.close();
			} catch (IOException e) {
				// nothing to do
			}
		}

		if (text == null) {
			text = "";
		} else {
			try {
				Properties properties = new Properties();
				properties.load(new FileInputStream(new File(
						"kairos.properties")));

				String parscitLocalPath = properties
						.getProperty("parscitLocalPath");

				String parscitRemoteURL = properties
						.getProperty("parscitRemoteURL");

				// Create test temporary file.
				File tempFile = File.createTempFile("ParsCitPdfParser", ".txt",
						new File(parscitLocalPath));

				// Write the text to a temporary file on disk
				BufferedWriter out = new BufferedWriter(
						new FileWriter(tempFile));
				out.write(text);
				out.flush();
				out.close();

				// ForeCite service
				ForeCiteLocator service = new ForeCiteLocator();
				ForeCitePortType port;

				try {
					port = service.getForeCitePort();

					try {
						// Get the XML records
						String xmlRecords = port
								.extract_header(parscitRemoteURL
										+ tempFile.getName());

						xmlRecords = new String(Base64.decode(xmlRecords));

						tempFile.delete();

						if (!StringUtil.isEmpty(xmlRecords)) {

							DocumentBuilderFactory dbf = DocumentBuilderFactory
									.newInstance();
							DocumentBuilder db;

							try {
								db = dbf.newDocumentBuilder();
								InputSource is = new InputSource();
								is.setCharacterStream(new StringReader(
										xmlRecords.toString()));

								Document doc;
								try {
									doc = db.parse(is);

									List<String> titleList = getElementsByTagName(
											doc, "title");
									List<String> authorList = getElementsByTagName(
											doc, "author");
									List<String> affiliationList = getElementsByTagName(
											doc, "affiliation");

									// Get the title in a nice readable form ;)
									title = Utils.arrayToString(titleList
											.toArray(new String[titleList
													.size()]), ", ");

									// Get the author(s) in a nice readable form
									// ^_^
									String authors = Utils
											.arrayToString(
													authorList
															.toArray(new String[authorList
																	.size()]),
													", ");

									if (!StringUtil.isEmpty(title)
											&& !StringUtil.isEmpty(authors)) {
										// We found metadata in the PDF
										metadata.set("METADATA", "1");

										// Set the metadata
										metadata.set(title, authors);
									}

									// Save the extracted metadata to file on
									// disk
									Utils
											.writeLinesToFile(
													new File(
															"parsePaperMetadata/extracted_authors.txt"),
													authorList,
													System
															.getProperty("line.separator"),
													true);

									Utils
											.writeLinesToFile(
													new File(
															"parsePaperMetadata/extracted_affiliations.txt"),
													affiliationList,
													System
															.getProperty("line.separator"),
													true);

								} catch (SAXException e) {
									e.printStackTrace(LogUtil
											.getErrorStream(LOG));
								} catch (IOException e) {
									e.printStackTrace(LogUtil
											.getErrorStream(LOG));
								}
							} catch (ParserConfigurationException e) {
								e.printStackTrace(LogUtil.getErrorStream(LOG));
							}
						} else {
							LOG.info("No result for: " + content.getBaseUrl());
						}
					} catch (RemoteException e) {
						e.printStackTrace(LogUtil.getErrorStream(LOG));
					}
				} catch (ServiceException e) {
					e.printStackTrace(LogUtil.getErrorStream(LOG));
				}
			} catch (IOException e) {
				e.printStackTrace(LogUtil.getWarnStream(LOG));
			}
		}

		if (title == null) {
			title = "";
		}

		ParseData parseData = new ParseData(ParseStatus.STATUS_SUCCESS, title,
				new Outlink[0], metadata, metadata);

		return ParseResult.createParseResult(content.getUrl(), new ParseImpl(
				text, parseData));
	}

	public static String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();

		if (child instanceof CharacterData) {
			CharacterData cd = (CharacterData) child;

			return cd.getData();
		}

		return "?";
	}

	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	public Configuration getConf() {
		return this.conf;
	}
}