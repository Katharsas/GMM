package gmm.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.jimfs.VPath;

import gmm.collections.ArrayList;

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
public class FileTreeScript {
	
	public String[] html(Path relDir, Path root) {
		return html(relDir, root, Optional.empty());
	}
	
	public String[] html(VPath relDir, VPath root, Optional<VPath> relFull) {
		return html(relDir.get(), root.get(), relFull.map(VPath::get));
	}
	
	/**
	 * @param relDir - the folder relative to root, whose contents are returned
	 * @param relFull - can be specified to not show folders other than those that are in the path of relFull.
	 * 		Example:
	 * 			root = /myPrivateRoot/
	 * 			relFull = /myPrivateRoot/stuff
	 * 		In this case, folders in myPrivateRoot other than stuff are not returned, but all folders in stuff are.
	 */
	public String[] html(Path relDir, Path root, Optional<Path> relFull) {
		
		final Path dirPath = root.resolve(relDir);// /2D
		String result = "";
	    if (Files.exists(dirPath)) {
	    	
	    	final Optional<Path> absFull = relFull.map(path -> root.resolve(path));// /
	    	final boolean isInPrivateSection =
	    			absFull.isPresent() && absFull.get().getParent().startsWith(dirPath); // false
	    	
	    	final List<Path> paths;
	    	if (isInPrivateSection) {
	    		// get child of relDir as specified in relFull
//	    		final Path childName = relDir.relativize(relFull.get()).getName(0);
	    		final int childIndex = dirPath.getNameCount();
	    		final Path childName = absFull.get().getName(childIndex);
	    		paths = new ArrayList<>(Path.class, dirPath.resolve(childName));
	    	} else {
	    		paths = readFileNames(dirPath);
	    	}
	    	final String lis = renderPaths(paths, root);
			result += ("<ul class='jqueryFileTree' style='display: none;'>" + lis + "</ul>");
	    }
	    return new String[] {result};
	}
	
	/**
	 * @param dirPath - absolute path
	 * @return - files & folders inside dirPath as absolute paths (filtered & sorted)
	 */
    private List<Path> readFileNames(Path dirPath) {
    	try (Stream<Path> stream = Files.list(dirPath)) {
			return stream
				.filter(path -> path.getFileName().toString().charAt(0) != '.')// filter hidden files
				.sorted(Comparator.comparing((Path path) -> Files.isDirectory(path))
								.thenComparing((Path path) -> path.toString(), String.CASE_INSENSITIVE_ORDER))
				.collect(Collectors.toList());
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
    }
    
    /**
     * @param paths - absolute paths
     */
    private String renderPaths(List<Path> paths, Path root) {
    	String result = "";
    	for (final Path path : paths) {
    		final String relative = root.relativize(path).toString();
    		final String fileName = path.getFileName().toString();
		    if (Files.isDirectory(path)) {
				result += "<li class='directory collapsed'>"
							+ "<a href='#' rel='" + relative + "/'>"+ fileName + "</a>"
						+ "</li>";
		    }
		    if (Files.isRegularFile(path)) {
				final int dotIndex = fileName.lastIndexOf('.');
				final String ext = dotIndex > 0 ? fileName.substring(dotIndex + 1) : "";
				result += "<li class='file ext_" + ext + "'>"
							+ "<a href='#' rel='" + relative + "'>" + fileName + "</a>"
						+ "</li>";
		    }
		}
    	return result;
    }
}
