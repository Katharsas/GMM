package gmm.service.ajax;

public class ConflictAnswer {
	
	public final String operation;
	public final boolean doForAllFlag;
	
	public ConflictAnswer(String operation, boolean doForAllFlag) {
		this.operation = operation;
		this.doForAllFlag = doForAllFlag;
	}
}