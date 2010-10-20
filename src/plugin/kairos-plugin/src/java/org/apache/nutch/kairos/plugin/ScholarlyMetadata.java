package org.apache.nutch.kairos.plugin;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.nutch.kairos.Utils;
import org.apache.nutch.util.StringUtil;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

/**
 * The scholarly metadata that correspond to individual papers
 * 
 * @author Markus Haense
 */
public class ScholarlyMetadata implements Cloneable {

	/**
	 * Contains the scholarly title
	 */
	private String _title = "";

	/**
	 * Contains the scholarly author(s)
	 */
	private String _author = "";

	/**
	 * Contains the affiliation(s) from the author(s)
	 */
	private String _affiliation = "";

	/**
	 * The conference name
	 */
	private String _conferenceName = "";

	/**
	 * The conference title
	 */
	private String _conferenceTitle = "";

	/**
	 * The conference / event begin date
	 */
	private String _conferenceBeginDate = "";

	/**
	 * The conference / event end date
	 */
	private String _conferenceEndDate = "";

	/**
	 * The conference place
	 */
	private String _conferencePlace = "";

	/**
	 * The conference URL
	 */
	private String _conferenceURL = "";

	private String _contentType = "";

	private String _url;

	public ScholarlyMetadata(String contentType) {
		if (!StringUtil.isEmpty(contentType)) {
			this._contentType = contentType;
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		if (!StringUtil.isEmpty(_author)) {
			sb.append(_author);
		}

		if (!StringUtil.isEmpty(_title)) {
			sb.append(Utils.NEWLINE);
			sb.append(_title);
		}

		if (!StringUtil.isEmpty(_affiliation)) {
			sb.append(Utils.NEWLINE);
			sb.append(_affiliation);
		}

		if (!StringUtil.isEmpty(_conferenceName)) {
			sb.append(Utils.NEWLINE);
			sb.append(_conferenceName);
		}

		if (!StringUtil.isEmpty(_conferenceTitle)) {
			sb.append(Utils.NEWLINE);
			sb.append(_conferenceTitle);
		}

		if (!StringUtil.isEmpty(_conferenceBeginDate)) {
			sb.append(Utils.NEWLINE);
			sb.append(_conferenceBeginDate);
		}

		if (!StringUtil.isEmpty(_conferenceEndDate)) {
			sb.append(Utils.NEWLINE);
			sb.append(_conferenceEndDate);
		}

		if (!StringUtil.isEmpty(_conferencePlace)) {
			sb.append(Utils.NEWLINE);
			sb.append(_conferencePlace);
		}

		if (!StringUtil.isEmpty(_conferenceURL)) {
			sb.append(Utils.NEWLINE);
			sb.append(_conferenceURL);
		}

		if (!StringUtil.isEmpty(_url)) {
			sb.append(Utils.NEWLINE);
			sb.append(_url);
		}

		if (sb.length() > 0) {
			sb.append(Utils.NEWLINE);
		}

		return sb.toString();
	}

	public String toXML() {
		StringBuilder sb = new StringBuilder();

		SolrInputDocument solrDocument = getSolrInputDocument();

		if (solrDocument != null) {
			if (solrDocument.getFieldNames().size() > 0) {
				sb.append("<add>" + Utils.NEWLINE + "<doc>" + Utils.NEWLINE);

				for (SolrInputField field : solrDocument) {
					String name = StringEscapeUtils.escapeXml(field.getName())
							.trim();
					String value = StringEscapeUtils.escapeXml(
							field.getValue().toString()).trim();

					if (!StringUtil.isEmpty(name) && !StringUtil.isEmpty(value)) {
						sb.append("<field ");
						sb.append("name=\"" + name + "\">");
						sb.append(value);
						sb.append("</field>" + Utils.NEWLINE);
					}
				}

				sb.append("</doc>" + Utils.NEWLINE + "</add>" + Utils.NEWLINE);
			}
		}

		return sb.toString();
	}

	@Override
	public Object clone() {
		ScholarlyMetadata clone = new ScholarlyMetadata(this._contentType);

		clone.setAuthor(_author);
		clone.setTitle(_title);
		clone.setAffiliation(_affiliation);
		clone.setConferenceBeginDate(_conferenceBeginDate);
		clone.setConferenceEndDate(_conferenceEndDate);
		clone.setConferenceName(_conferenceName);
		clone.setConferencePlace(_conferencePlace);
		clone.setConferenceTitle(_title);
		clone.setConferenceURL(_conferenceURL);
		clone.setURL(_url);

		return clone;
	}

	public SolrInputDocument getSolrInputDocument() {
		SolrInputDocument solrDocument = new SolrInputDocument();

		if (!StringUtil.isEmpty(_author)) {
			solrDocument.addField("p_author", _author);
		}

		if (!StringUtil.isEmpty(_title)) {
			solrDocument.addField("p_title", _title);
		}

		if (!StringUtil.isEmpty(_affiliation)) {
			solrDocument.addField("p_affiliation", _affiliation);
		}

		if (!StringUtil.isEmpty(_conferenceName)) {
			solrDocument.addField("c_name", _conferenceName);
		}

		if (!StringUtil.isEmpty(_conferenceTitle)) {
			solrDocument.addField("c_title", _conferenceTitle);
		}

		if (!StringUtil.isEmpty(_conferenceBeginDate)) {
			solrDocument.addField("c_begin_date", _conferenceBeginDate);
		}

		if (!StringUtil.isEmpty(_conferenceEndDate)) {
			solrDocument.addField("c_end_date", _conferenceEndDate);
		}

		if (!StringUtil.isEmpty(_conferencePlace)) {
			solrDocument.addField("c_place", _conferencePlace);
		}

		if (!StringUtil.isEmpty(_conferenceURL)) {
			solrDocument.addField("c_url", _conferenceURL);
		}

		if (!StringUtil.isEmpty(_url) && !_url.equals(_conferenceURL)) {
			solrDocument.addField("url", _url);
		}

		if (solrDocument.getFieldNames().size() > 0) {
			// solrDocument.addField("id", "hash");
			solrDocument.addField("date_created", "");
			solrDocument.addField("content_type", _contentType);
			solrDocument.addField("kairos_version", "1.0");
		}

		return solrDocument;
	}

	public String getAuthor() {
		return _author;
	}

	public String getTitle() {
		return _title;
	}

	public String getAffiliation() {
		return _affiliation;
	}

	public String getConferenceName() {
		return _conferenceName;
	}

	public String getConferenceTitle() {
		return _conferenceTitle;
	}

	public String getConferenceBeginDate() {
		return _conferenceBeginDate;
	}

	public String getConferenceEndDate() {
		return _conferenceEndDate;
	}

	public String getConferencePlace() {
		return _conferencePlace;
	}

	public String getConferenceURL() {
		return _conferenceURL;
	}

	public void setURL(String url) {
		_url = url;
	}

	public String getURL() {
		return _url;
	}

	public void setAuthor(String author) {
		if (!StringUtil.isEmpty(author)) {
			_author = author.trim();
		}
	}

	public void setTitle(String title) {
		if (!StringUtil.isEmpty(title)) {
			_title = title.trim();
		}
	}

	public void setAffiliation(String affiliation) {
		if (!StringUtil.isEmpty(affiliation)) {
			_affiliation = affiliation.trim();
		}
	}

	public void setConferenceName(String name) {
		if (!StringUtil.isEmpty(name)) {
			_conferenceName = name.trim();
		}
	}

	public void setConferenceTitle(String title) {
		if (!StringUtil.isEmpty(title)) {
			_conferenceTitle = title.trim();
		}
	}

	public void setConferenceBeginDate(String beginDate) {
		if (!StringUtil.isEmpty(beginDate)) {
			_conferenceBeginDate = beginDate.trim();
		}
	}

	public void setConferenceEndDate(String endDate) {
		if (!StringUtil.isEmpty(endDate)) {
			_conferenceEndDate = endDate.trim();
		}
	}

	public void setConferencePlace(String place) {
		if (!StringUtil.isEmpty(place)) {
			_conferencePlace = place.trim();
		}
	}

	public void setConferenceURL(String url) {
		if (!StringUtil.isEmpty(url)) {
			_conferenceURL = url.trim();
		}
	}

	public boolean hasTitle() {
		return !StringUtil.isEmpty(_title);
	}
}
