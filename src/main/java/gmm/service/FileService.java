package gmm.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.util.StringUtil;

/**
 * Service for all kind of write/read File operations.
 * @author Jan
 */
@Service
public class FileService {
	
	/**
	 * Restricts dir path access to visisble directory or below.
	 * 
	 * @param dir - Relative or absolute path that needs to be restricted.
	 * @param visible - Relative or absolute path that represents the restriction.
	 * @return Path relative to visible directory that points below the visible directory.
	 */
	public Path restrictAccess(Path dir, Path visible) {
		if (!isChild(dir, visible)) {
			//If dir is relative, check for back-paths
			if (!isChild(visible.resolve(dir), visible)) {
				throw new IllegalArgumentException("Path restriction error: Path could not be resolved.");
			}
			return dir;
		}
		else {
			//If dir is absolute, make it relative
			return visible.relativize(dir);
		}
	}
	
	private boolean isChild(Path child, Path parent) {
		return normalize(child).startsWith(normalize(parent));
	}
	
	public Collection<String> getRelativeNames(Collection<Path> paths, Path visible) {
		final List<String> relPaths = new LinkedList<>(String.class);
		for (final Path path : paths) {
			relPaths.add(visible.relativize(path).toString());
		}
		return relPaths;
	}
	
	/**
	 * Returns the file paths of all files inside the given directory recursivly.
	 * This includes files inside directories inside the given directory.
	 * 
	 * @param fileExtensions - Filters the files by file extension.
	 * @throws IOException 
	 */
	public List<Path> getFilesRecursive(Path path, PathFilter filter) {
		final List<Path> filePaths = new LinkedList<>(Path.class);
		if (path.toFile().exists()) {
			try(Stream<Path> stream = Files.walk(path)) {
				stream
					.filter(Files::isRegularFile)
					.filter(filter)
					.collect(Collectors.toCollection(()-> filePaths));
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return filePaths;
	}
	
	public static interface PathFilter extends Predicate<Path> {}
	
	/**
	 * A Filter that only accepts extensions specified on construction.
	 * Does not accept hidden files (unix) or directories.
	 * @author Jan
	 */
	public static class FileExtensionFilter implements PathFilter {
		private final static StringUtil strings = new StringUtil().ignoreCase();
		private final String[] extensions;
		/**
		 * @param extensions - An array with file extensions like "txt","jpg".
		 */
		public FileExtensionFilter(String[] extensions) {
			this.extensions = extensions;
		}
		@Override
		public boolean test(Path entry) {
			return test(entry.getFileName().toString());
		}
		public boolean test(String name) {
			for(final String ext : this.extensions) {
				if(strings.endsWith(name, "." + ext)) return name.charAt(0) != '.';
			}
			return false;
		}
	}
	
	/**
	 * Deletes the given file or directory and all subdirectories with all their files.
	 * @param path - must not be null and corresponding file/folder must exist
	 */
	public synchronized void delete(Path path) {
		try {
			FileUtils.forceDelete(path.toFile());
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not recursivly delete file or directory " + path, e);
		}
	}
	
	/**
	 * Creates a directory and any necessary parent directories.
	 */
	public synchronized void createDirectory(Path path) {
		try {
			FileUtils.forceMkdir(path.toFile());
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not create directory " + path, e);
		}
	}
	
	/**
	 * Creates a directory and any necessary parent directories.
	 */
	public synchronized void createFile(Path path, byte[] data) {
		createDirectory(path.getParent());
		try {
			Files.write(path, data);
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not write data to file at " + path.toString(), e);
		}
	}
	
	/**
	 * Wrapper for {@link java.nio.file.Path#normalize()} because of OpenJDK bug:
	 * <a href="https://bugs.openjdk.java.net/browse/JDK-8037945">https://bugs.openjdk.java.net/browse/JDK-8037945</a>
	 * 
	 * @see {@link java.nio.file.Path#normalize()}
	 */
	private Path normalize(Path path) {
		if (path.toString().equals("")) {
			return path;
		}
		else {
			return path.normalize();
		}
	}
}
