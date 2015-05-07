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
	public static TaskType fromClass(Class<? extends Task> clazz) {
		TaskType primaryResult = null;
		TaskType secondaryResult = null;
		for(TaskType type : TaskType.values()) {
			if(type.toClass().isAssignableFrom(clazz)) {
				if(type.toClass().equals(clazz)) primaryResult = type;
				else secondaryResult = type;
			}
		}
		//this check is separate to not return super type instead if equal type.
		if (primaryResult == null) {
			if (secondaryResult == null) {
				throw new IllegalArgumentException("Invalid type value: "+clazz.getName());
			} else return secondaryResult;
		} else return primaryResult;
	}
	
	private final String name = getTypeKey() + "." + this.name().toLowerCase();
	public String getNameKey() {return name;}
	public String getTypeKey() {return "tasks.type";}
}