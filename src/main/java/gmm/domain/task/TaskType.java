package gmm.domain.task;


public enum TaskType {
	GENERAL(GeneralTask.class), 
	TEXTURE(TextureTask.class),
	MODEL(ModelTask.class);
	
	private Class<? extends Task> type;
	
	private TaskType(Class<? extends Task> type) {
		this.type = type;
	}
	public Class<? extends Task> toClass() {
		return type;
	}
	public static TaskType fromClass(Class<? extends Task> type) {
		TaskType[] types = TaskType.values();
		for(int i = 0; i < types.length; i++) {
			if(types[i].toClass().equals(type)) {
				return types[i];
			}
		}
		throw new IllegalArgumentException("Invalid type value: "+type.getName());
	}
	
	private final String name = getTypeKey() + "." + this.name().toLowerCase();
	public String getNameKey() {return name;}
	public String getTypeKey() {return "tasks.type";}
}
