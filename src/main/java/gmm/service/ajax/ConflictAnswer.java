package gmm.service.ajax;

import gmm.util.TypedString;

public class ConflictAnswer<O extends TypedString> {
	
	public final O operation;
	public final boolean doForAllFlag;
	
	public ConflictAnswer(O operation, boolean doForAllFlag) {
		this.operation = operation;
		this.doForAllFlag = doForAllFlag;
	}
}