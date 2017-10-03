package gmm.service.data;

import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.Linkable;
import gmm.domain.User;

/**
 * DB interface, provides methods to change data, provides Observer pattern to notify on changes.
 * 
 * @author Jan Mothes
 */
@Service
public interface DataAccess {

	public <T extends Linkable> Collection<T> getList(Class<T> clazz);
	public <T extends Linkable> void add(T data);
	public <T extends Linkable> void addAll(Collection<T> data);
	public <T extends Linkable> void remove(T data);
	public <T extends Linkable> void removeAll(Collection<T> data);
	public <T extends Linkable> void removeAll(Class<T> clazz);
	public <T extends Linkable> void edit(T data);
	public <T extends Linkable> void editBy(T data, User source);
	@Deprecated public boolean hasIds(long[] id);
	public CombinedData getCombinedData();
	
	
	/**
	 * Observer/Callback interface.
	 */
	@FunctionalInterface
	public static interface DataChangeCallback {
		/**
		 * Called when a data change occurs.
		 * @param event - Event object which holds information about the changes.
		 */
		public void onEvent(DataChangeEvent event);
	}
	
	/**
	 * Register to get method calls on task data changes. Unregistering is unnecessary. All
	 * implementations of this interface must use weak references to reference the callbacks.<br>
	 * <br>
	 * Important: DOES NOT HOLD A (STRONG) REFERENCE to onUpdate object.<br>
	 * => Caller must ensure there is a reference so that the object does not get GCed.
	 */
	public void registerForUpdates(DataChangeCallback onUpdate);
	
	/**
	 * Similar to {@link #registerForUpdates(DataChangeCallback)}, but allows to post process data
	 * after a change before update listeners are notified.
	 */
	public void registerPostProcessor(DataChangeCallback onUpdate);
}
