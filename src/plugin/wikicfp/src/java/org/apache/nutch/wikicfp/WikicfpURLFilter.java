package org.apache.nutch.wikicfp;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.net.URLFilter;

/**
 * Filters URLs.
 * 
 * @author Markus Haense
 */
public class WikicfpURLFilter implements URLFilter {

	/**
	 * Logger
	 */
	private static final Log LOG = LogFactory.getLog(WikicfpURLFilter.class);

	/**
	 * Configuration
	 */
	private Configuration conf;

	/**
	 * URL Filter
	 */
	public String filter(String urlString) {
		URL url = Utils.convertStringToURLObject(urlString, false);

		if (url != null) {
			// Get the host
			String host = url.getHost();

			// Get the URL path
			String urlPath = url.getPath();

			// If the host is WikiCFP
			if (host.equals("www.wikicfp.com")) {
				// Get the path from the URL
				String file = urlPath.substring(urlPath.lastIndexOf("/") + 1,
						urlPath.length());

				// Allow only events and conference URLs
				if (urlPath.equals("/") || urlPath.equals("")
						|| file.equals("event.showcfp") || file.equals("call")) {

					LOG.info("ALLOW [" + url.toString() + "]");
					return url.toString();
				} else {
					LOG.info("DENY [" + url.toString() + "]");
					return null;
				}
			} else {
				// If we have another host than WikiCFP
				LOG.info("NO WIKICFP HOST");
				return null;
			}
		} else {
			// urlString is an invalid URL
			LOG.info("URL STRING IS INVALID");
			return null;
		}
	}

	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	public Configuration getConf() {
		return this.conf;
	}
}