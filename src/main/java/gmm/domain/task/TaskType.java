package gmm.domain.task;


public enum TaskType {
	GENERAL(GeneralTask.class, "general"), 
	TEXTURE(TextureTask.class, "textures"),
	MODEL(ModelTask.class, "models");
	
	private Class<? extends Task> type;
	private String tabName;
	
	private TaskType(Class<? extends Task> type, String tabName) {
		this.type = type;
		this.tabName = tabName;
	}
	public Class<? extends Task> toClass() {
		return type;
	}
	public String getTab() {
		return tabName;
	}
	public static TaskType fromTab(String tab) {
		TaskType[] types = TaskType.values();
		for(int i = 0; i < types.length; i++) {
			if(types[i].getTab().equals(tab)) {
				return types[i];
			}
		}
		throw new IllegalArgumentException("Invalid tab value: "+tab);
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
