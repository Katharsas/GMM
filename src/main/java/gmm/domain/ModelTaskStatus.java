package gmm.domain;

public enum ModelTaskStatus {
	MESH, TEXTURE, ANIMATION, SPACER;
	
	
	private final String name = getTypeKey() + "." + this.name().toLowerCase();
	public String getNameKey() {return name;}
	public String getTypeKey() {return "tasks.model.status";}
}
