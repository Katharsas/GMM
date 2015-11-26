package gmm.service.data;

import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.Linkable;
import gmm.domain.task.Task;

@Service
public interface DataAccess {

	public <T extends Linkable> Collection<T> getList(Class<T> clazz);
	public <T extends Linkable> boolean add(T data);
	public <T extends Linkable> boolean remove(T data);
	public <T extends Linkable> boolean addAll(Collection<T> data);
	public <T extends Linkable> void removeAll(Class<T> clazz);
	public <T extends Linkable> void removeAll(Collection<T> data);
	public boolean hasIds(long[] id);
	public CombinedData getCombinedData();
	
	public static interface TaskUpdateCallback {
		public <T extends Task> void onAdd(T task);
		public <T extends Task> void onAddAll(Collection<T> tasks);
		public <T extends Task> void onRemove(T task);
		public <T extends Task> void onRemoveAll(Collection<T> tasks);
	}
	
	/**
	 * Register to get method calls on task data changes.
	 * Unregistering is unnecessary. All implementations of DataAccess must
	 * use weak references to reference the callback objects.
	 * 
	 * Important: DOES NOT HOLD A (STRONG) REFERENCE TO onUpdate object.
	 * => Caller must hold reference until it can be destroyed.
	 */
	public void registerForUpdates(TaskUpdateCallback onUpdate);
}
