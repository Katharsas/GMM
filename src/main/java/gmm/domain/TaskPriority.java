package gmm.domain;

public enum TaskPriority {
	ULTRA, HIGH, MID, LOW;
	
	private final String name = getTypeKey() + "." + this.name().toLowerCase();
	public String getNameKey() {return name;}
	public String getTypeKey() {return "tasks.priority";}
}
