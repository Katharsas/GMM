package gmm.util;

public class StringUtil {
	public static boolean IGNORE_CASE = false;
	public static boolean ALWAYS_CONTAINS_EMPTY = true;
	
	public static boolean contains(String arg0, String arg1){
		if(!ALWAYS_CONTAINS_EMPTY && arg1.equals("")) return false;
		if (IGNORE_CASE)return org.apache.commons.lang3.StringUtils.containsIgnoreCase(arg0, arg1);
		return arg0.contains(arg1);
	}
	
	public static boolean equals(String arg0, String arg1){
		if(!ALWAYS_CONTAINS_EMPTY && arg1.equals("")) return false;
		if (IGNORE_CASE)return org.apache.commons.lang3.StringUtils.equalsIgnoreCase(arg0, arg1);
		return arg0.equals(arg1);
	}
}
