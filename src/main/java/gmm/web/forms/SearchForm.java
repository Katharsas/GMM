package gmm.web.forms;

public class SearchForm implements Form {
	private boolean isEasySearch;
	private String easy;
	private String name;
	private String author;
	private String details;
	private String label;
	private String assigned;
	private String path;
	
	public SearchForm() {
		setDefaultState();
	}
	public void setDefaultState() {
		isEasySearch=true;
		easy="";
		name="";
		author="";
		details="";
		label="";
		assigned="";
		path="";
	}
	public boolean isInDefaultState() {
		boolean isEmpty;
		if (isEasySearch) {
			isEmpty = easy.isEmpty();
		} else {
			isEmpty = name.isEmpty() && author.isEmpty() && details.isEmpty() && label.isEmpty()
					&& assigned.isEmpty() && path.isEmpty();
		}
		return isEmpty;
	}

	//Setters, Getters-------------------------------------------
	public boolean isEasySearch() {
		return isEasySearch;
	}
	public void setEasySearch(boolean isEasySearch) {
		this.isEasySearch = isEasySearch;
	}
	public String getEasy() {
		return easy;
	}
	public void setEasy(String easy) {
		this.easy = easy;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getAssigned() {
		return assigned;
	}
	public void setAssigned(String assigned) {
		this.assigned = assigned;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
}
