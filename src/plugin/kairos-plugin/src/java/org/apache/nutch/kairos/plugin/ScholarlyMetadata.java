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

		if (!StringUtil.isEmpty(this._author)) {
			sb.append(this._author);
		}

		if (!StringUtil.isEmpty(this._title)) {
			sb.append(Utils.NEWLINE);
			sb.append(this._title);
		}

		if (!StringUtil.isEmpty(this._affiliation)) {
			sb.append(Utils.NEWLINE);
			sb.append(this._affiliation);
		}

		if (!StringUtil.isEmpty(this._conferenceName)) {
			sb.append(Utils.NEWLINE);
			sb.append(this._conferenceName);
		}

		if (!StringUtil.isEmpty(this._conferenceTitle)) {
			sb.append(Utils.NEWLINE);
			sb.append(this._conferenceTitle);
		}

		if (!StringUtil.isEmpty(this._conferenceBeginDate)) {
			sb.append(Utils.NEWLINE);
			sb.append(this._conferenceBeginDate);
		}

		if (!StringUtil.isEmpty(this._conferenceEndDate)) {
			sb.append(Utils.NEWLINE);
			sb.append(this._conferenceEndDate);
		}

		if (!StringUtil.isEmpty(this._conferencePlace)) {
			sb.append(Utils.NEWLINE);
			sb.append(this._conferencePlace);
		}

		if (!StringUtil.isEmpty(this._conferenceURL)) {
			sb.append(Utils.NEWLINE);
			sb.append(this._conferenceURL);
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

		clone.setAuthor(this._author);
		clone.setTitle(this._title);
		clone.setAffiliation(this._affiliation);
		clone.setConferenceBeginDate(this._conferenceBeginDate);
		clone.setConferenceEndDate(this._conferenceEndDate);
		clone.setConferenceName(this._conferenceName);
		clone.setConferencePlace(this._conferencePlace);
		clone.setConferenceTitle(this._title);
		clone.setConferenceURL(this._conferenceURL);

		return clone;
	}

	public SolrInputDocument getSolrInputDocument() {
		SolrInputDocument solrDocument = new SolrInputDocument();

		if (!StringUtil.isEmpty(this._author)) {
			solrDocument.addField("p_author", this._author);
		}

		if (!StringUtil.isEmpty(this._title)) {
			solrDocument.addField("p_title", this._title);
		}

		if (!StringUtil.isEmpty(this._affiliation)) {
			solrDocument.addField("p_affiliation", this._affiliation);
		}

		if (!StringUtil.isEmpty(this._conferenceName)) {
			solrDocument.addField("c_name", this._conferenceName);
		}

		if (!StringUtil.isEmpty(this._conferenceTitle)) {
			solrDocument.addField("c_title", this._conferenceTitle);
		}

		if (!StringUtil.isEmpty(this._conferenceBeginDate)) {
			solrDocument.addField("c_begin_date", this._conferenceBeginDate);
		}

		if (!StringUtil.isEmpty(this._conferenceEndDate)) {
			solrDocument.addField("c_end_date", this._conferenceEndDate);
		}

		if (!StringUtil.isEmpty(this._conferencePlace)) {
			solrDocument.addField("c_place", this._conferencePlace);
		}

		if (!StringUtil.isEmpty(this._conferenceURL)) {
			solrDocument.addField("c_url", this._conferenceURL);
		}

		if (solrDocument.getFieldNames().size() > 0) {
			// solrDocument.addField("id", "hash");
			solrDocument.addField("date_created", "");
			solrDocument.addField("content_type", this._contentType);
			solrDocument.addField("kairos_version", "1.0");
		}

		return solrDocument;
	}

	public String getAuthor() {
		return this._author;
	}

	public String getTitle() {
		return this._title;
	}

	public String getAffiliation() {
		return this._affiliation;
	}

	public String getConferenceName() {
		return this._conferenceName;
	}

	public String getConferenceTitle() {
		return this._conferenceTitle;
	}

	public String getConferenceBeginDate() {
		return this._conferenceBeginDate;
	}

	public String getConferenceEndDate() {
		return this._conferenceEndDate;
	}

	public String getConferencePlace() {
		return this._conferencePlace;
	}

	public String getConferenceURL() {
		return this._conferenceURL;
	}

	public void setAuthor(String author) {
		if (!StringUtil.isEmpty(author)) {
			this._author = author.trim();
		}
	}

	public void setTitle(String title) {
		if (!StringUtil.isEmpty(title)) {
			this._title = title.trim();
		}
	}

	public void setAffiliation(String affiliation) {
		if (!StringUtil.isEmpty(affiliation)) {
			this._affiliation = affiliation.trim();
		}
	}

	public void setConferenceName(String name) {
		if (!StringUtil.isEmpty(name)) {
			this._conferenceName = name.trim();
		}
	}

	public void setConferenceTitle(String title) {
		if (!StringUtil.isEmpty(title)) {
			this._conferenceTitle = title.trim();
		}
	}

	public void setConferenceBeginDate(String beginDate) {
		if (!StringUtil.isEmpty(beginDate)) {
			this._conferenceBeginDate = beginDate.trim();
		}
	}

	public void setConferenceEndDate(String endDate) {
		if (!StringUtil.isEmpty(endDate)) {
			this._conferenceEndDate = endDate.trim();
		}
	}

	public void setConferencePlace(String place) {
		if (!StringUtil.isEmpty(place)) {
			this._conferencePlace = place.trim();
		}
	}

	public void setConferenceURL(String url) {
		if (!StringUtil.isEmpty(url)) {
			this._conferenceURL = url.trim();
		}
	}
	
	public boolean hasTitle() {
		return !StringUtil.isEmpty(this._title);
	}
}
