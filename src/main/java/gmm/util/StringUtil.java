package gmm.util;

/**
 * No null input allowed.
 * 
 * @author Jan Mothes
 */
public class StringUtil {
	public boolean IGNORE_CASE = false;
	public boolean ALWAYS_CONTAINS_EMPTY = true;
	
	public StringUtil ignoreCase() {
		this.IGNORE_CASE = true;
		return this;
	}
	
	public boolean contains(String str, String searchStr){
		if(!ALWAYS_CONTAINS_EMPTY && searchStr.equals("")) return false;
		if (IGNORE_CASE)return org.apache.commons.lang3.StringUtils.containsIgnoreCase(str, searchStr);
		return str.contains(searchStr);
	}
	
	public boolean equals(String str1, String str2){
		if (IGNORE_CASE)return org.apache.commons.lang3.StringUtils.equalsIgnoreCase(str1, str2);
		return str1.equals(str2);
	}
	
	public boolean endsWith (String str, String suffix){
		if (IGNORE_CASE) return org.apache.commons.lang3.StringUtils.endsWithIgnoreCase(str, suffix);
		return str.endsWith(suffix);
	}
	
	public boolean startsWith (String str, String prefix){
		if (IGNORE_CASE) return org.apache.commons.lang3.StringUtils.startsWithIgnoreCase(str, prefix);
		return str.startsWith(prefix);
	}
}
