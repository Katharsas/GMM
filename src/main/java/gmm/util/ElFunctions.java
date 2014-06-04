package gmm.util;
import org.apache.commons.lang3.StringEscapeUtils;

public class ElFunctions {
	
	public static String escape(String input) {
		return StringEscapeUtils.escapeHtml4(StringEscapeUtils.escapeEcmaScript(input));
	}
}
