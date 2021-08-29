package gmm.service.ajax.operations;

import java.util.Map;
import java.util.function.Function;

import gmm.domain.UniqueObject;
import gmm.service.ajax.BundledMessageResponses;
import gmm.util.TypedString;

/**
 * Defines of a set of {@link Conflict}s that can occur on loading an element of type T.<br>
 * To load an element, call {@link #onLoad(UniqueObject)}, which returns one of those conflicts or
 * {@link #NO_CONFLICT}. If no conflict occurred, the {@link #onDefault(Object)} method must be
 * called.<br>
 * <br>
 * Each conflict is expected to be resolved by executing a corresponding conflict operation.
 * All available conflict operations can be retrieved with the {@link #getAllOperations()} method.
 * Usually, the client will select a conflict operation by sending the corresponding operation name.
 * 
 * @author Jan Mothes
 * @param <T> See {@link BundledMessageResponses}
 */
public abstract class ConflictChecker<T, O extends TypedString> {
	
	/**
	 * An operation which can be executed to resolve a conflict.
	 */
	public static interface Operation<T> {
		public String execute(Conflict<T> conflict, T element);
	}
	
	/**
	 * A conflict with unique name and detailed description, which can be resolved by specific
	 * operations.
	 */
	public static abstract class Conflict<T> {
		public String name;
		public abstract String getName();
		public abstract String getDetails(T element);
		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			if (obj instanceof Conflict) {
				final Conflict<?> other = (Conflict<?>) obj;
				return getName().equals(other.getName());
			}
			return false;
		}
		@SafeVarargs
		public final boolean isOneOf(Conflict<T>... others) {
			for(final Conflict<T> other : others) {
				if (this.equals(other)) return true;
			}
			return false;
		}
	}
	

	
	public final Conflict<T> NO_CONFLICT = new Conflict<T>() {
		@Override
		public String getName() {
			return "NO_CONFLICT";
		}
		@Override
		public String getDetails(T element) {
			throw new UnsupportedOperationException();
		}
	};
	
	private final Function<String, O> newOperation;
	public final O DEFAULT_OPERATION;
	
	public ConflictChecker(Function<String, O> newOperation) {
		this.newOperation = newOperation;
		DEFAULT_OPERATION = newOperation.apply("default");
	}
	
	public O parseOpFromString(String operationString) {
		O op = newOperation.apply(operationString);
		if (!DEFAULT_OPERATION.equals(op) && !getAllOperations().containsKey(op)) {
			throw new IllegalArgumentException("The given operation '" + operationString + "' is not available for this conflict checker!");
		}
		return op;
	}
	
	/**
	 * Get an operations used to resolve conflicts.
	 * @return A map which maps all operation names which the client can send to operations.
	 */
	public abstract Map<O, Operation<T>> getAllOperations();
	
	/**
	 * @return {@link #NO_CONFLICT} if no conflict occurred, otherwise the conflict which occurred.
	 */
	public abstract Conflict<T> onLoad(T element);
	
	/**
	 * Executed when no conflict occurred on the last element processed.
	 * @return MessageResponse message
	 */
	public abstract String onDefault(T element);
	
	protected final void assertConflict(boolean valid) {
		if (!valid) {
			throw new IllegalArgumentException("The conflict cannot be resolved by this operation!");
		}
	}
}