package gmm.domain.task;

public enum TaskStatus {
	TODO, INPROGRESS, INREVIEW, DONE;
	
	
	private final String name = getTypeKey() + "." + this.name().toLowerCase();
	public String getNameKey() {return name;}
	public String getTypeKey() {return "tasks.status";}
}
