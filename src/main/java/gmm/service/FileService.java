package gmm.service;

import gmm.util.LinkedList;
import gmm.util.List;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.springframework.stereotype.Service;

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
				throw new IllegalArgumentException();
			}
		}
		catch(Exception e) {
			throw new IllegalArgumentException("Wrong path input! Path is not valid! Try to make the path relative.");
		}
		return dir;
	}
	
	public List<String> getFilePaths(String dir, String[] fileExtensions) {
		List<String> filePaths = new LinkedList<>();
		File root = new File(dir);
		if (root.exists()) {
			List<File> files = getFilesRecursive(root, new FileExtensionFilter(fileExtensions));
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
			if(filter.accept(dir, dir.getName())) list.add(dir);
		}
		else if(dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				if(f.isDirectory()) list.addAll(getFilesRecursive(f, filter));
				else if(filter.accept(f, f.getName())) list.add(f);
			}
		}
		return list;
	}
	
	public boolean deleteFile(String dir) {
		File file = new File(dir);
		return file.delete();
	}
		
	public class FileExtensionFilter implements FilenameFilter {
		private String[] extensions;
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
}
