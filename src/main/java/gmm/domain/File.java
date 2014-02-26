package gmm.domain;

import java.net.URI;

@Deprecated
@SuppressWarnings("serial")
public class File extends java.io.File {
	
	//Variables----------------------------------------
	private String description="";
	private String details="";

	//Methods------------------------------------------
	public File(URI uri) {
		super(uri);
	}
	public File(String parent, String child) {
		super(parent, child);
	}
	public File(String pathname) {
		super(pathname);
	}
	public File(java.io.File parent, String child) {
		super(parent, child);
	}
	
	//Setters, Getters----------------------------------------
	public void setDescription(String description) {
		if (description==null) throw new NullPointerException();
		this.description = description;
	}
	public void setDetails(String details) {
		if (details==null) throw new NullPointerException();
		this.details = details;
	}
	
	public String getDescription() {
		return description;
	}
	public String getDetails() {
		return details;
	}
}
