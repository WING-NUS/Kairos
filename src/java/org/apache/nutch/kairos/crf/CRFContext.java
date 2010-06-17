package org.apache.nutch.kairos.crf;

import java.util.LinkedList;
import java.util.List;

import org.apache.nutch.util.StringUtil;

/**
 * @author Markus Haense
 */
public class CRFContext {

	private List<String> contextLines = new LinkedList<String>();

	public boolean addContext(List<String> list) {
		if (list.size() < 0) {
			return false;
		} else {
			return contextLines.addAll(list);
		}
	}

	public boolean addContext(String str) {
		str = str.trim();

		if (StringUtil.isEmpty(str)) {
			return false;
		} else {
			return contextLines.add(str);
		}
	}

	public int size() {
		return contextLines.size();
	}

	public List<String> getContext() {
		return contextLines;
	}

}