package gmm.domain;

import java.util.LinkedList;
import java.util.List;

@Deprecated
public class FileTask extends Task {

	//Variables------------------------------------------
	private MyFile oldFile;
	private List<MyFile> newFiles = new LinkedList<MyFile>();
	
	//Methods--------------------------------------------
	public FileTask(String idName, User author, MyFile oldFile) {
		super(idName, author);
		this.oldFile = oldFile;
	}
	
	//Setters, Getters---------------------------------
	public void setOldFile(MyFile oldFile) {
		if (oldFile==null) throw new NullPointerException();
		this.oldFile = oldFile;
	}
	public MyFile getOldFile() {
		return oldFile;
	}
	
	public List<MyFile> getNewFiles() {
		return newFiles;
	}
}
