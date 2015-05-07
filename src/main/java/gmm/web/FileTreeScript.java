package gmm.web;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;


public class FileTreeScript {
	
	/**
	 * jQuery File Tree JSP Connector
	 * Version 1.0
	 * Copyright 2008 Joshua Gould
	 * 21 April 2008
	 * 
	 * MODIFIED FOR PRIVATE PROJECT, NOT ORIGINAL VERSION
	 */	
	public String[] html(Path relDir, Path root) {
		
		Path dirPath = root.resolve(relDir);
		File dir = dirPath.toFile();
		String result = "";
	    if (dir.exists()) {
	    	//filter out hidden files
			File[] files = dir.listFiles(new FilenameFilter() {
			    public boolean accept(File dir, String name) {
					return name.charAt(0) != '.';
			    }
			});
			//sort files
			Arrays.sort(files, new Comparator<Object>() {
				@Override
				public int compare(Object o1, Object o2) {
					return String.CASE_INSENSITIVE_ORDER.compare(o1.toString(), o2.toString());
				}
			});
			result += ("<ul class=\"jqueryFileTree\" style=\"display: none;\">");
			// All dirs
			for (File file : files) {
			    if (file.isDirectory()) {
					result += ("<li class=\"directory collapsed\"><a href=\"#\" rel=\"" + 
							root.relativize(file.toPath()) + "/\">"+ file.getName() + "</a></li>");
			    }
			}
			// All files
			for (File file : files) {
			    if (!file.isDirectory()) {
					int dotIndex = file.getName().lastIndexOf('.');
					String ext = dotIndex > 0 ? file.getName().substring(dotIndex + 1) : "";
					result +=("<li class=\"file ext_" + ext + "\"><a href=\"#\" rel=\"" + 
						root.relativize(file.toPath()) + "\">" + file.getName() + "</a></li>");
			    	}
			}
			result +=("</ul>");
	    }
	    return new String[] {result};
	}
}
