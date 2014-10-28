package gmm.service.ajax.operations;

import java.util.Map;

import gmm.domain.UniqueObject;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.MessageResponse;

/**
 * All methods except {@link #onLoad(UniqueObject)} return the message which will be saved
 * in {@link MessageResponse#message} when creating the appropriate message.
 * 
 * This message will then be shown to the user.
 * 
 * @author Jan Mothes
 * @param <T> See {@link BundledMessageResponses}
 */
public abstract class MessageResponseOperations<T> {
	
	public static interface Operation<T> {
		public String execute(T element) throws Exception;
	}
	public static interface Conflict<T> {
		public String getStatus();
		public String getMessage(T element);
	}
	public final static Conflict<?> NO_CONFLICT = new Conflict<Object>() {
		@Override public String getStatus() {return null;}
		@Override public final String getMessage(Object element) {return null;}
	};
	@SuppressWarnings({ "unchecked" })
	public final static <E> Conflict<E> cast(Conflict<?> conflict) {
		return (Conflict<E>) conflict;
	}
	
	/**
	 * @return a map which maps all operation names which the client can send to operations.
	 */
	public abstract Map<String, Operation<T>> getOperations();
	
	/**
	 * @return {@link #NO_CONFLICT} if no conflict occured, otherwise the conflict which occured.
	 */
	public abstract Conflict<T> onLoad(T element);
	
	/**
	 * Executed when no conflict occured on the last element processed.
	 * @return MessageResponse message
	 */
	public abstract String onDefault(T element) throws Exception;
}