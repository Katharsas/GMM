package gmm.domain;

public enum TaskStatus {
	TODO, INPROGRESS, INREVIEW, DONE;
	
	String messageKey;
	private TaskStatus() {
		messageKey = "taskStatus."+this.name().toLowerCase();
	}
	public String getMessageKey() {
		return messageKey;
	}
}
