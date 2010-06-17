package org.apache.nutch.wikicfp;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.net.URLNormalizer;
import org.apache.nutch.util.URLUtil;

/**
 * Converts WikiCFP URLs to a normal form.
 * 
 * @author Markus Haense
 * 
 */
public class WikicfpURLNormalizer implements URLNormalizer {
	/**
	 * Logger
	 */
	public static final Log LOG = LogFactory.getLog(WikicfpURLNormalizer.class);

	/**
	 * Configuration
	 */
	private Configuration conf;

	/**
	 * Normalize URL to a normal form
	 */
	public String normalize(String urlString, String scope)
			throws MalformedURLException {

		String oldUrlString = urlString;

		if (URLUtil.getHost(urlString).equals("www.wikicfp.com")) {

			URL url = Utils.convertStringToURLObject(urlString, false);

			if (url != null) {
				// Get the query
				String query = url.getQuery();

				// If we have a query
				if (query != null) {
					// Parse the query parameters
					String[] params = query.split("&");

					// If we have parameters
					if (params.length > 0) {
						Map<String, String> map = new HashMap<String, String>();

						// Put the query parameter in a hash map
						for (String param : params) {
							String name = param.split("=")[0].trim();
							String value = param.split("=")[1].trim();
							map.put(name, value);
						}

						// Get the protocol
						String protocol = url.getProtocol() + "://";

						// Get the authority
						String authority = url.getAuthority();

						// Get the URL path
						String urlPath = url.getPath();

						// Event
						if (url
								.toString()
								.startsWith(
										"http://www.wikicfp.com/cfp/servlet/event.showcfp?")) {
							urlString = protocol + authority + urlPath
									+ "?eventid=" + map.get("eventid");

							// Conference
						} else if (url.toString().startsWith(
								"http://www.wikicfp.com/cfp/call?conference=")) {

							// Normalize
							String conference = map.get("conference");

							if (conference == null) {
								conference = "";
							} else {
								conference = "?conference="
										+ conference.replaceAll(" ", "%20")
												.toLowerCase();
							}

							String page = map.get("page");

							// If the page query parameter is missing
							if (page == null) {
								// Add the page query parameter
								page = "&page=1";
							} else {
								page = "&page=" + page;
							}

							// Final URL
							urlString = protocol + authority + urlPath
									+ conference + page;
						} else {
							// Ignore all other WikiCFP URLs
							urlString = null;
						}
					}
				}
			} else {
				// urlString is an invalid URL
				urlString = null;
			}
		} else {
			// If we have another host than WikiCFP
			urlString = null;
		}

		LOG.info("URL NORMALIZE: [" + oldUrlString + "] ==> [" + urlString
				+ "]");

		return urlString;
	}

	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	public Configuration getConf() {
		return this.conf;
	}
}