package gmm.service.data;

import java.time.Instant;
import java.util.Objects;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.List;
import gmm.domain.Linkable;
import gmm.domain.User;
import gmm.domain.User.UserId;
import gmm.domain.task.Task;
import gmm.util.Util;

/**
 * Holds information about a data change on a uniform, same-type data-set.
 * Also knows which user initiated the change. Non-user-changes are represented by
 * either {@link User#NULL}, {@link User#SYSTEM} or {@link User#UNKNOWN}.
 * 
 * @author Jan Mothes
 */
public class DataChangeEvent<T extends Linkable> {

	public final DataChangeType type;
	public final Instant created;
	public final User source;
	
	public final boolean isSingleItem;
	public final Collection<T> changed;
	
	/**
	 * @param changed - The generic type of the collection must be one of:
	 *  <br> - {@link Task}
	 *  <br> - {@link User}
	 *  <br> or any subclass of those classes.
	 */
	public DataChangeEvent(DataChangeType type, User source, Collection<T> changed) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(source);
		Objects.requireNonNull(changed);
		if (changed.size() == 0) {
			throw new IllegalArgumentException("Don't emit stupid events!");
		}
		if (!Task.class.isAssignableFrom(changed.getGenericType())
				&& !User.class.isAssignableFrom(changed.getGenericType())) {
			throw new IllegalArgumentException("Only User/Task (sub-)types are supported.");
		}
		this.type = type;
		this.source = source;
		this.changed = changed;
		this.isSingleItem = changed.size() == 1;
		created = Instant.now();
	}
	
	public DataChangeEvent(DataChangeType type, User source, T changed) {
		this(type, source, new ArrayList<T>(Util.classOf(changed), changed));
	}
	
	public T getChangedSingle() {
		if (!isSingleItem) {
			throw new UnsupportedOperationException("Change was not on single item!");
		} else {
			return changed.iterator().next();
		}
	}
	
	public ClientDataChangeEvent toClientEvent() {
		final List<String> changedIds = new ArrayList<>(String.class, changed.size());
		for(final Linkable linkable : changed) {
			changedIds.add(linkable.getIdLink());
		}
		return new ClientDataChangeEvent(source.getUserId(), type.name(), changedIds);
	}
	
	/**
	 * Similar to the event class itself, but only contains data fields, no references.
	 * Stripped of sensitive data and can be automatically converted to JSON for client.
	 */
	public static class ClientDataChangeEvent {
		public final UserId source;
		public final String eventType;
		public final List<String> changedIds;
		protected ClientDataChangeEvent(UserId source, String eventType, List<String> changedIds) {
			this.source = source;
			this.eventType = eventType;
			this.changedIds = changedIds;
		}
	}
}
