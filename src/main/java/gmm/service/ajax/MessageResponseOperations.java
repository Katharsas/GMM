package gmm.service.ajax;

import java.util.HashMap;
import java.util.Map;

import gmm.domain.UniqueObject;

/**
 * All methods except {@link #onLoad(UniqueObject)} return the message which will be saved
 * in {@link MessageResponse#message} when creating the appropriate message.
 * 
 * This message will then be shown to the user.
 * 
 * @author Jan Mothes
 * @param <T> See {@link BundledMessageResponses}
 */
public abstract class MessageResponseOperations<T extends UniqueObject> {
	
	protected final Map<String, Operation<T>> map = new HashMap<>();
	
	public static interface Operation<T> {
		public String execute(T element);
	}
	
	/**
	 * @return MessageResponse message
	 */
	protected String doOperation(String operationType, T element) {
		return map.get(operationType).execute(element);
	}
	
	/**
	 * @return true if no conflict occured
	 */
	public abstract boolean onLoad(T element);
	
	/**
	 * @return MessageResponse message
	 */
	public abstract String onDefault(T element);
	
	/**
	 * @return MessageResponse message
	 */
	public abstract  String onConflict(T element);

}