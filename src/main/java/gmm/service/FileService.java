package gmm.service;

import gmm.util.LinkedList;
import gmm.util.List;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

import org.springframework.stereotype.Service;

/**
 * Service for all kind of write/read File operations.
 * @author Jan
 */
@Service
public class FileService {
	
	/**
	 * Restricts dir path access to public directory or below.
	 * If the dir variable does not point below the public directory,
	 * it will be treated as relative path below the public directory
	 */
	public String restrictAccess(String dir, String publicDir){
		try {dir = java.net.URLDecoder.decode(dir, "UTF-8");}
		catch (UnsupportedEncodingException e1) {e1.printStackTrace();}	
		
		try {
			String baseCanonical = new File(publicDir).getCanonicalPath();
			String dirCanonical = new File(dir).getCanonicalPath();
			if (!dirCanonical.startsWith(baseCanonical)) {
				dir = publicDir+dir;
				dirCanonical = new File(dir).getCanonicalPath();
			}
			if (!dirCanonical.startsWith(baseCanonical)) {
				throw new IllegalArgumentException("Wrong path input! Path is not valid! Try to make the path relative.");
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return dir;
	}
	
	/**
	 * Returns the file paths of all files inside the given directory recursivly.
	 * Thi includes files inside directories inside the given directory.
	 * 
	 * @param fileExtensions - Filters the files by file extension.
	 */
	public List<String> getFilePaths(String dir, String[] fileExtensions) {
		List<String> filePaths = new LinkedList<>();
		File root = new File(dir);
		if (root.exists()) {
			List<File> files = getFilesRecursive(root, fileExtensions == null ? 
					null :new FileExtensionFilter(fileExtensions));
			for (File f : files) {
				try { filePaths.add(f.getCanonicalPath());}
				catch (IOException e) { e.printStackTrace();}
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
	 * @author Jan
	 */
	public class FileExtensionFilter implements FilenameFilter {
		private String[] extensions;
		/**
		 * @param extensions - An array with file extensions like "txt","jpg".
		 */
		public FileExtensionFilter(String[] extensions) {
			this.extensions = extensions;
		}
		@Override
		public boolean accept(File dir, String name) {
			for(String ext : this.extensions) {
				if(name.endsWith("."+ext)) return name.charAt(0) != '.';
			}
			return false;
		}
	}
	
	/**
	 * @see {@link gmm.service.FileService#delete(File)}
	 */
	public void delete(String file) throws IOException {
		delete(new File(file));
	}
	/**
	 * Deletes the given file and all empty parent directories.
	 * So if a file is the only file in a folder, the folder will be deleted too.
	 */
	public synchronized void delete(File file) throws IOException {
		File parent = file.getParentFile();
		Files.delete(file.toPath());
		if(parent.list().length == 0) {
			delete(parent);
		}
	}
	
	/**
	 * @see {@link gmm.service.FileService#prepareFileCreation(File)}
	 */
	public void prepareFileCreation(String path) throws IOException {
		prepareFileCreation(new File(path));
	}
	/**
	 * Creates necessary parent directories for this file.
	 */
	public void prepareFileCreation(File file) throws IOException {
		File parent = file.getParentFile();
		if(!parent.exists()) {
			createDirectory(parent);
		}
	}
	
	/**
	 * Creates a directory and any necessary parent directories.
	 */
	private synchronized void createDirectory(File file) throws IOException {
		if(!file.exists()) {
			File parent = file.getParentFile();
			if(!parent.exists()) {
				createDirectory(parent);
			}
			Files.createDirectory(file.toPath());
		}
	}
	
	/**
	 * Creates a directory and any necessary parent directories.
	 */
	public synchronized void createFile(String path, byte[] data) throws IOException {
		prepareFileCreation(path);
		Files.write(new File(path).toPath(), data);
	}
}
