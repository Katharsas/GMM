package gmm.service.data;

import java.time.Instant;
import java.util.Objects;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.domain.Linkable;
import gmm.domain.User;

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
	
	public DataChangeEvent(DataChangeType type, User source, Linkable changed) {
		this(type, source, new ArrayList<Linkable>(Linkable.class, changed));
	}
	
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getChanged(Class<T> clazz) {
		return (Collection<T>) changed;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getChangedSingle(Class<T> clazz) {
		if (!isSingleItem) {
			throw new UnsupportedOperationException("Change was not on single item!");
		} else {
			return (T) changed.iterator().next();
		}
	}
}
