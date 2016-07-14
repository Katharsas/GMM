package gmm.service.data;

import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.Linkable;

@Service
public interface DataAccess {

	public <T extends Linkable> Collection<T> getList(Class<T> clazz);
	public <T extends Linkable> void add(T data);
	public <T extends Linkable> void addAll(Collection<T> data);
	public <T extends Linkable> void remove(T data);
	public <T extends Linkable> void removeAll(Collection<T> data);
	public <T extends Linkable> void removeAll(Class<T> clazz);
	public <T extends Linkable> void edit(T data);
	@Deprecated public boolean hasIds(long[] id);
	public CombinedData getCombinedData();
	
	@FunctionalInterface
	public static interface DataChangeCallback {
		/**
		 * Called when a data change occurs.
		 * @param event - Event object which holds information about the changes.
		 */
		public void onEvent(DataChangeEvent event);
	}
	
	/**
	 * Register to get method calls on task data changes.
	 * Unregistering is unnecessary. All implementations of DataAccess must
	 * use weak references to reference the callback objects.
	 * 
	 * Important: DOES NOT HOLD A (STRONG) REFERENCE TO onUpdate object.
	 * => Caller must hold reference until it can be destroyed.
	 */
	public void registerForUpdates(DataChangeCallback onUpdate);
}
