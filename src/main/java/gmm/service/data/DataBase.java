package gmm.service.data;

import static gmm.service.data.DataChangeType.ADDED;
import static gmm.service.data.DataChangeType.EDITED;
import static gmm.service.data.DataChangeType.REMOVED;

import java.util.Arrays;
import java.util.Collections;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.CollectionTypeMap;
import gmm.collections.HashSet;
import gmm.collections.JoinedCollectionView;
import gmm.collections.Set;
import gmm.domain.Label;
import gmm.domain.Linkable;
import gmm.domain.UniqueObject;
import gmm.domain.User;
import gmm.domain.task.GeneralTask;
import gmm.domain.task.Task;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.TextureTask;
import gmm.service.users.UserService;
import gmm.util.Util;

@Service
public class DataBase implements DataAccess {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	final private UserService users;
	final private CombinedData combined;
	
	final private Set<Class<?>> listTypes = new HashSet<>(null, new Class<?>[]{});
	final private CollectionTypeMap lists = new CollectionTypeMap();
	final private CollectionTypeMap compounds = new CollectionTypeMap();
	
	final private java.util.Set<DataChangeCallback> weakCallbacks =
			Collections.newSetFromMap(new WeakHashMap<DataChangeCallback, Boolean>());

	final private DataChangeCallback callbacks = event -> {
		for(final DataChangeCallback c : weakCallbacks) {
			c.onEvent(event);
		}
	};
	
	@Autowired
	private DataBase(CombinedData combined, UserService users) {
		this.users = users;
		this.combined = combined;
		
		initDirectLists(new Class<?>[] {
			User.class,
			GeneralTask.class,
			TextureTask.class,
			ModelTask.class,
			Label.class
		});
		initCompoundList(AssetTask.class, Util.createArray(new Class<?>[]
				{ TextureTask.class, ModelTask.class }
		));
		initCompoundList(Task.class, Util.createArray(new Class<?>[]
				{ GeneralTask.class, AssetTask.class }
		));
	}
	
	public void initDirectLists(Class<?>[] types) {
		for (Class<?> type : types) {
			// even though ArrayLists are used,
			// public methods ensure elements cannot be added twice
			lists.put(type, new ArrayList<>(type));
		}
		listTypes.addAll(Arrays.asList(types));
	}
	
	public <T> void initCompoundList(Class<T> type, Class<T>[] includedTypes) {
		Collection<T>[] included = Util.createArray(new Collection<?>[includedTypes.length]);
		for (int i = 0; i < included.length; i++) {
			Collection<T> list = lists.get(includedTypes[i]);
			if (list == null) {
				list = compounds.get(includedTypes[i]);
			}
			included[i] = Util.cast(list, includedTypes[i]);
		}
		compounds.put(type, new JoinedCollectionView<>(new ArrayList<>(type), included));
		listTypes.add(type);
	}
	
	/**
	 * Returns a live collection view of elements of or extending the given type.
	 * 
	 * @param clazz - Any list type or compound type. Compound collections do not support
	 * 		add/remove operations and may have worse performance for other operations!
	 */
	private <T> Collection<T> directOrCompound(Class<T> clazz) {
		// direct list type
		Collection<T> result = lists.get(clazz);
		if (result != null) {
			return result;
		} else {
			// compound type
			Collection<T> multi = compounds.get(clazz);
			if (multi != null) {
				return multi;
			} else {
				throw new IllegalArgumentException(
						"A list for type '"+clazz.getSimpleName()+"' cannot be found or composed!");
			}
		}
	}
	
	/**
	 * Returns a live collection view of elements of or extending the given type.
	 * 
	 * @param clazz -Any list type, compound types not allowed! All returned collections support
	 * 		add/remove operations.
	 */
	private <T> Collection<T> directOnly(Class<T> clazz) {
		Collection<?> result = lists.get(clazz);
		if (result != null) {
			return Util.cast(result, clazz);
		} else {
			throw new IllegalArgumentException(
					"A list for type '"+clazz.getSimpleName()+"' cannot be found!");
		}
	}
	
	@Override
	public synchronized <T extends Linkable> Collection<T> getList(Class<T> clazz) {
		return directOrCompound(clazz).copy();
	}

	@Override
	public synchronized <T extends Linkable> void add(T data) {
		final Collection<T> collection = directOnly(Util.classOf(data));
		if (collection.contains(data)) {
			throw new IllegalArgumentException("Element cannot be added because it already exists!");
		}
		collection.add(data);
		Collection<Label> taskLabels = directOnly(Label.class);
		if(data instanceof Task) {
			final Task task = (Task) data;
			taskLabels.add(new Label(task.getLabel()));
		}
		callbacks.onEvent(new DataChangeEvent(ADDED, users.getLoggedInUser(), data));
	}
	
	@Override
	public synchronized <T extends Linkable> void addAll(Collection<T> data) {
		// TODO split per type if data type is compound (see removeAll)
		final Collection<T> collection = directOnly(data.getGenericType());
		if(!Collections.disjoint(data, collection)) {
			throw new IllegalArgumentException("Elements cannot be added because at least one of them already exists!");
		}
		collection.addAll(data);
		if(Task.class.isAssignableFrom(data.getGenericType())) {
			final Collection<? extends Task> tasks = Util.castBound(data, Task.class);
			Collection<Label> taskLabels = directOnly(Label.class);
			for (final Task task : tasks) {
				taskLabels.add(new Label(task.getLabel()));
			}
		}
		callbacks.onEvent(new DataChangeEvent(ADDED, users.getExecutingUser(), data));
	}

	@Override
	public synchronized <T extends Linkable> void remove(T data) {
		final Collection<T> collection = directOnly(Util.classOf(data));
		if(!collection.contains(data)){
			throw new IllegalArgumentException("Element cannot be removed because it does not exists!");
		}
		collection.remove(data);
		callbacks.onEvent(new DataChangeEvent(REMOVED, users.getExecutingUser(), data));
	}
	
	@Override
	public synchronized <T extends Linkable> void removeAll(Collection<T> data) {	
		// TODO split per type only if data type is compound
		final Multimap<Class<?>, T> clazzToData = ArrayList.getMultiMap(data.getGenericType());
		for(final T item : data) {
			clazzToData.put(Util.classOf(item), item);
		}
		for(final Class<?> clazz : clazzToData.keySet()) {
			if(!directOnly(clazz).containsAll(clazzToData.get(clazz))) {
				throw new IllegalArgumentException("Elements cannot be removed because at least one of them does not exists!");
			}
		}
		for(final Class<?> clazz : clazzToData.keySet()) {
			Collection<T> part = (Collection<T>) clazzToData.get(clazz);
			directOnly(clazz).removeAll(part);
			callbacks.onEvent(new DataChangeEvent(REMOVED, users.getExecutingUser(), part));
		}
	}
	
	@Override
	public synchronized <T extends Linkable> void removeAll(Class<T> clazz) {
		final Collection<T> removed = getList(clazz);
		directOrCompound(clazz).clear();
		callbacks.onEvent(new DataChangeEvent(REMOVED, users.getExecutingUser(), removed));
	}
	
	@Override
	public <T extends Linkable> void edit(T data) {
		final Collection<T> collection = directOnly(Util.classOf(data));
		if(!collection.contains(data)){
			throw new IllegalArgumentException("Element cannot be edited because it does not exists!");
		}
		collection.remove(data);
		collection.add(data);
		callbacks.onEvent(new DataChangeEvent(EDITED, users.getExecutingUser(), data));
	}

	@Override
	public CombinedData getCombinedData() {
		return combined;
	}
	
	@Override
	public boolean hasIds(long[] ids) {
		return exists(directOrCompound(User.class), ids) == true ||
				exists(directOrCompound(Task.class), ids) == true;
	}
	
	private boolean exists(Collection<? extends UniqueObject> c, long[] ids) {
		for(final UniqueObject u : c) {
			for (final long id : ids) {
				if(u.getId() == id) return true;
			}
		}
		return false;
	}

	@Override
	public void registerForUpdates(DataChangeCallback onUpdate) {
		weakCallbacks.add(onUpdate);
		//TODO delete when tested with more than 10 sessions
		if (weakCallbacks.size() > 20)
			logger.error("Memory leak: Callback objects not getting garbage collected!");
	}

}
