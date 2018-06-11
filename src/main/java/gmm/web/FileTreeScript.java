package gmm.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Thread-safe.
 */
public class FileTreeScript {
	
	/**
	 * <pre>
	 * jQuery File Tree JSP Connector
	 * Version 1.0
	 * Copyright 2008 Joshua Gould
	 * 21 April 2008
	 * 
	 * MODIFIED FOR PRIVATE PROJECT, NOT ORIGINAL VERSION
	 * 
	 * Original License from https://github.com/jak/jQuery-File-Tree:
	 * "This plugin is dual-licensed under the GNU General Public License and the MIT License and is copyright 2008 A 
	 * Beautiful Site, LLC."
	 * </pre>
	 * 
	 * Thread-safe.
	 */	
	public String[] html(Path relDir, Path root) {
		
		final Path dirPath = root.resolve(relDir);
		String result = "";
	    if (Files.exists(dirPath)) {
	    	
	    	List<Path> paths;
			try (Stream<Path> stream = Files.list(dirPath)) {
				paths = stream
					.filter(path -> path.getFileName().toString().charAt(0) != '.')// filter hidden files
					.sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.toString(), o2.toString()))
					.collect(Collectors.toList());
				
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		
			result += ("<ul class=\"jqueryFileTree\" style=\"display: none;\">");
			// All dirs
			for (final Path path : paths) {
			    if (Files.isDirectory(path)) {
					result += ("<li class=\"directory collapsed\"><a href=\"#\" rel=\"" + 
							root.relativize(path) + "/\">"+ path.getFileName() + "</a></li>");
			    }
			}
			// All files
			for (final Path path : paths) {
			    if (Files.isRegularFile(path)) {
			    	final String fileName = path.getFileName().toString();
					final int dotIndex = fileName.lastIndexOf('.');
					final String ext = dotIndex > 0 ? fileName.substring(dotIndex + 1) : "";
					result +=("<li class=\"file ext_" + ext + "\"><a href=\"#\" rel=\"" + 
						root.relativize(path) + "\">" + fileName + "</a></li>");
			    	}
			}
			result +=("</ul>");
	    }
	    return new String[] {result};
	}
}
