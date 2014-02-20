package gmm.domain;

import java.util.LinkedList;
import java.util.List;

public class FileTask extends Task {

	//Variables------------------------------------------
	private File oldFile;
	private List<File> newFiles = new LinkedList<File>();
	
	//Methods--------------------------------------------
	public FileTask(String idName, User author, File oldFile) {
		super(idName, author);
		this.oldFile = oldFile;
	}
	
	//Setters, Getters---------------------------------
	public void setOldFile(File oldFile) {
		if (oldFile==null) throw new NullPointerException();
		this.oldFile = oldFile;
	}
	public File getOldFile() {
		return oldFile;
	}
	
	public List<File> getNewFiles() {
		return newFiles;
	}
}
