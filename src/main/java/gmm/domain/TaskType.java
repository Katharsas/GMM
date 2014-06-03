package gmm.domain;

public enum TaskType {
	GENERAL, TEXTURE, MODEL;
	
	
	private final String name = getTypeKey() + "." + this.name().toLowerCase();
	public String getNameKey() {return name;}
	public String getTypeKey() {return "tasks.type";}
}
