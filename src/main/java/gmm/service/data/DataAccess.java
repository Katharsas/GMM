package gmm.service.data;

import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.Linkable;
import gmm.domain.User;
import gmm.service.assets.AssetTaskUpdater;
import gmm.service.assets.NewAssetLockService;

/**
 * DB interface, provides methods to change data, provides Observer pattern to notify on changes.
 * <br><br>
 * Methods that change data must NOT be called by main/startup thread! Otherwise {@link AssetTaskUpdater}
 * cannot schedule a reopen for {@link NewAssetLockService} lock, causing the main thread to deadlock itself.
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
	public <T extends Linkable> void editAllBy(Collection<T> data, User source);
	@Deprecated public boolean hasIds(long[] id);
	public CombinedData getCombinedData();
	
	
	/**
	 * Observer/Callback interface.
	 */
	@FunctionalInterface
	public static interface DataChangeCallback<T extends Linkable> {
		/**
		 * Called when a data change occurs.
		 * @param event - Event object which holds information about the changes.
		 */
		public void onEvent(DataChangeEvent<? extends T> event);
	}
	
	/**
	 * Register simple callback that gets called when data changes. Code inside callback is not allowed to
	 * call any methods from {@link DataAccess}. Unregister with {@link #unregister(DataChangeCallback)}.
	 */
	public <T extends Linkable> void registerForUpdates(DataChangeCallback<T> onUpdate, Class<T> clazz);
	
	/**
	 * When data changes, post processor callbacks are called before simple callbacks (see {@link #registerForUpdates(DataChangeCallback)}).
	 * Code inside callback is allowed to make any additional data changes and the corresponding additional
	 * change events will be seen by simple callbacks (but not by other post processor callbacks).
	 * Unregister with {@link #unregister(DataChangeCallback)}.
	 */
	public <T extends Linkable> void registerPostProcessor(DataChangeCallback<T> onUpdate, Class<T> clazz);
	
	/**
	 * Any registered {@link DataChangeCallback} should be unregistered when not needed anymore.
	 * @see {@link #registerForUpdates(DataChangeCallback, Class)}<br>{@link #registerPostProcessor(DataChangeCallback, Class)}.
	 */
	public void unregister(DataChangeCallback<?> onUpdate);
}
