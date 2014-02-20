package gmm.domain;

public enum Priority {
	ULTRA, HIGH, MID, LOW;
	
	String messageKey;
	private Priority() {
		messageKey = "priority."+this.name().toLowerCase();
	}
	public String getMessageKey() {
		return messageKey;
	}
}
