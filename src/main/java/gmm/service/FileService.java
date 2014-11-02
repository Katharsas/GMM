package gmm.service;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.util.StringUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

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
		List<String> relPaths = new LinkedList<>();
		for (Path path : paths) {
			relPaths.add(visible.relativize(path).toString());
		}
		return relPaths;
	}
	
	/**
	 * Returns the file paths of all files inside the given directory recursivly.
	 * This includes files inside directories inside the given directory.
	 * 
	 * @param fileExtensions - Filters the files by file extension.
	 */
	public List<Path> getFilePaths(Path path, FilenameFilter filter) {
		List<Path> filePaths = new LinkedList<>();
		File root = path.toFile();
		if (root.exists()) {
			List<File> files = getFilesRecursive(root, filter);
			for (File f : files) {
				filePaths.add(f.toPath());
			}
		}
		return filePaths;
	}
	
	private List<File> getFilesRecursive(File dir, FilenameFilter filter) {
		List<File> list = new LinkedList<>();
		if(dir.isFile()) {
			if(filter == null || filter.accept(dir, dir.getName())) list.add(dir);
		}
		else if(dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				if(f.isDirectory()) list.addAll(getFilesRecursive(f, filter));
				else if(filter == null || filter.accept(f, f.getName())) list.add(f);
			}
		}
		return list;
	}
	

	/**
	 * A Filter that only accepts extensions specified on contruction.
	 * Does not accept hidden files (unix).
	 * @author Jan
	 */
	public static class FileExtensionFilter implements FilenameFilter {
		private final static StringUtil strings = new StringUtil().ignoreCase();
		private final String[] extensions;
		/**
		 * @param extensions - An array with file extensions like "txt","jpg".
		 */
		public FileExtensionFilter(String[] extensions) {
			this.extensions = extensions;
		}
		@Override
		public boolean accept(File _, String name) {
			for(String ext : this.extensions) {
				if(strings.endsWith(name, "." + ext)) return name.charAt(0) != '.';
			}
			return false;
		}
	}
	
	/**
	 * Deletes the given file or directory and all empty parent directories.
	 * So if a file is the only file in a folder, the folder will be deleted too.
	 */
	public synchronized void delete(Path path) throws IOException {
		Path parent = path.getParent();
		if (path.toFile().isDirectory()) clearDirectory(path);
		Files.delete(path);
		if(parent.toFile().list().length == 0) {
			delete(parent);
		}
	}
	
	/**
	 * Deletes everything in this directory recursively.
	 */
	private void clearDirectory(Path path) throws IOException {
		if (path.toFile().isDirectory()) {
			for (File f : path.toFile().listFiles()) {
				if (f.isDirectory()) clearDirectory(f.toPath());
				Files.delete(f.toPath());
			}
		}
	}
	
	/**
	 * Creates necessary parent directories for this file.
	 */
	public void prepareFileCreation(Path path) throws IOException {
		Path parent = path.getParent();
		try {
			if(!parent.toFile().exists()) {
				createDirectory(parent);
			}
		} catch (Exception e) {
			throw new IOException("Could not prepare directories for " + path, e);
		}
	}
	
	/**
	 * Creates a directory and any necessary parent directories.
	 */
	public synchronized void createDirectory(Path dir) throws IOException {
		if(!dir.toFile().exists()) {
			Path parent = dir.getParent();
			if(!parent.toFile().exists()) {
				createDirectory(parent);
			}
			try {
				Files.createDirectory(dir);
			} catch (IOException e) {
				throw new IOException("Could not create directory at " + dir, e);
			}
		}
	}
	
	/**
	 * Creates a directory and any necessary parent directories.
	 */
	public synchronized void createFile(Path path, byte[] data) throws IOException {
		prepareFileCreation(path);
		try {
			Files.write(path, data);
		} catch (IOException e) {
			throw new IOException("Could not write data to file at " + path.toString(), e);
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
