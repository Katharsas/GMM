package gmm.service.data;

import java.time.Instant;
import java.util.Objects;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.List;
import gmm.domain.Linkable;
import gmm.domain.User;
import gmm.domain.User.UserId;
import gmm.util.Util;

/**
 * Holds information about a data change on a uniform, same-type data-set.
 * Also knows which user initiated the change. Non-user-changes are represented by
 * either {@link User#NULL}, {@link User#SYSTEM} or {@link User#UNKNOWN}.
 * 
 * @author Jan Mothes
 */
public class DataChangeEvent {

	public final DataChangeType type;
	public final Instant created;
	public final User source;
	
	public final boolean isSingleItem;
	public final Collection<? extends Linkable> changed;
	
	public DataChangeEvent(DataChangeType type, User source, Collection<? extends Linkable> changed) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(source);
		Objects.requireNonNull(changed);
		if (changed.size() == 0) {
			throw new IllegalArgumentException("Don't emit stupid events!");
		}
		this.type = type;
		this.source = source;
		this.changed = changed;
		this.isSingleItem = changed.size() == 1;
		created = Instant.now();
	}
	
	public <T extends Linkable> DataChangeEvent(DataChangeType type, User source, T changed) {
		this(type, source, new ArrayList<>(Util.classOf(changed), changed));
	}
	
	/**
	 * Convenience function to cast {@link DataChangeEvent#changed} generics to a caller-known type.
	 */
	public <T> Collection<? extends T> getChanged(Class<T> clazz) {
		return Util.castBound(changed, clazz);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getChangedSingle(Class<T> clazz) {
		if (!isSingleItem) {
			throw new UnsupportedOperationException("Change was not on single item!");
		} else {
			return (T) changed.iterator().next();
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
