package gmm.service.data;

import static gmm.service.data.DataChangeType.ADDED;
import static gmm.service.data.DataChangeType.EDITED;
import static gmm.service.data.DataChangeType.REMOVED;

import java.util.Arrays;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import gmm.service.users.UserProvider;
import gmm.util.Util;

@Service
public class DataBase implements DataAccess {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	final private Supplier<User> executingUser;
	final private CombinedData combined;
	
	// ###################
	// OBSERVER PATTERN
	// ###################
	
	final private java.util.Set<DataChangeCallback> weakPostProcessorCallbacks =
			Collections.newSetFromMap(new WeakHashMap<DataChangeCallback, Boolean>());
	
	final private java.util.Set<DataChangeCallback> weakConsumerCallbacks =
			Collections.newSetFromMap(new WeakHashMap<DataChangeCallback, Boolean>());
	
	final private DataChangeCallback callbacks = event -> {
		for(final DataChangeCallback c : weakPostProcessorCallbacks) {
			c.onEvent(event);
		}
		for(final DataChangeCallback c : weakConsumerCallbacks) {
			c.onEvent(event);
		}
	};
	
	// ###################
	// DATA
	// ###################
	
	/** Contains all types (concrete & compound) which can be mapped to collections of data.
	 */
	final private Set<Class<?>> listTypes = new HashSet<>(null, new Class<?>[]{});
	
	/** Map concrete types to collections of objects of respective type.
	 */
	final private CollectionTypeMap concretes = new CollectionTypeMap();
	
	/** Maps supertypes (=compounds) to collection views of objects of concrete types from {@link #concretes}.
	 */
	final private CollectionTypeMap compounds = new CollectionTypeMap();
	
	// ###################
	// INIT
	// ###################
	
	private DataBase() {
		
		// Initialize lists for concrete types
		initConcreteLists(new Class<?>[] {
			User.class,
			GeneralTask.class,
			TextureTask.class,
			ModelTask.class,
			Label.class
		});
		// Initialize AssetTask compound view
		initCompoundList(AssetTask.class, Util.createArray(new Class<?>[]
				{ TextureTask.class, ModelTask.class }
		));
		// Initialize GeneralTask compound view
		initCompoundList(Task.class, Util.createArray(new Class<?>[]
				{ GeneralTask.class, AssetTask.class }
		));
		
		// must be set early since used by all data-adding methods (initialization)
		executingUser = () -> UserProvider.getExecutingUser(getList(User.class));
		
		combined = new CombinedData();
	}
	
	@SuppressWarnings("unchecked")
	private <T> void initConcreteLists(Class<? extends T>[] types) {
		for (final Class<?> type : types) {
			// even though ArrayLists are used,
			// public methods ensure elements cannot be added twice
			final ArrayList<?> list = new ArrayList<>(type);
			concretes.putSafe((Class<Object>)list.getGenericType(), (Collection<Object>)list);// fuck u generics
		}
		listTypes.addAll(Arrays.asList(types));
	}
	
	/**
	 * Initializes a live combined list view of the lists from given {@code includedTypes} and maps
	 * them to given type in {@link DataBase#compounds}.
	 */
	private <T> void initCompoundList(Class<T> type, Class<T>[] includedTypes) {
		final Collection<T>[] included = Util.createArray(new Collection<?>[includedTypes.length]);
		for (int i = 0; i < included.length; i++) {
			Collection<T> list = concretes.getSafe(includedTypes[i]);
			if (list == null) {
				list = compounds.getSafe(includedTypes[i]);
				if (list == null) {
					throw new IllegalArgumentException("Cannot build compound list:"
							+ " Included type '" + includedTypes[i] + "' unknown!");
				}
			}
			included[i] = Util.cast(list, includedTypes[i]);
		}
		compounds.putSafe(type, new JoinedCollectionView<>(new ArrayList<>(type), included));
		listTypes.add(type);
	}
	
	// ###################
	// METHODS
	// ###################
	
	/**
	 * Get concrete or compound element collection.
	 * 
	 * @param clazz - Any concrete type or compound type.
	 * @return Live collection if concrete type was given, live collection view if compound was
	 * 		given. Compound collections do not support add/remove operations and may have worse
	 * 		performance for other operations!
	 */
	private <T> Collection<T> concreteOrCompound(Class<T> clazz) {
		// concrete list type
		final Collection<T> result = concretes.getSafe(clazz);
		if (result != null) {
			return result;
		} else {
			// compound type
			final Collection<T> multi = compounds.getSafe(clazz);
			if (multi != null) {
				return multi;
			} else {
				throw new IllegalArgumentException(
						"A list for type '"+clazz.getSimpleName()+"' cannot be found or composed!");
			}
		}
	}
	
	/**
	 * Get/modify concrete element collection.
	 * 
	 * @param clazz - Any concrete type, compound types not allowed! All returned collections
	 * 		support add/remove operations.
	 * @return Live collection.
	 */
	private <T> Collection<T> concreteOnly(Class<T> clazz) {
		final Collection<?> result = concretes.getSafe(clazz);
		if (result != null) {
			return Util.cast(result, clazz);
		} else {
			throw new IllegalArgumentException(
					"A list for type '"+clazz.getSimpleName()+"' cannot be found!");
		}
	}
	
	@Override
	public synchronized <T extends Linkable> Collection<T> getList(Class<T> clazz) {
		return concreteOrCompound(clazz).copy();
	}

	@Override
	public synchronized <T extends Linkable> void add(T data) {
		logger.debug("Adding element of type " + data.getClass().getSimpleName());
		final Collection<T> collection = concreteOnly(Util.classOf(data));
		if (collection.contains(data)) {
			throw new IllegalArgumentException("Element cannot be added because it already exists!");
		}
		collection.add(data);
		final Collection<Label> taskLabels = concreteOnly(Label.class);
		if(data instanceof Task) {
			final Task task = (Task) data;
			taskLabels.add(new Label(task.getLabel()));
		}
		callbacks.onEvent(new DataChangeEvent(ADDED, executingUser.get(), data));
	}
	
	@Override
	public synchronized <T extends Linkable> void addAll(Collection<T> data) {
		logger.debug("Adding multiple elements of type " + data.getGenericType().getSimpleName());
		if (data.size() >= 1) {
			// TODO split per type if data type is compound (see removeAll)
			final Collection<T> collection = concreteOnly(data.getGenericType());
			if(!Collections.disjoint(data, collection)) {
				throw new IllegalArgumentException("Elements cannot be added because at least one of them already exist!");
			}
			collection.addAll(data);
			if(Task.class.isAssignableFrom(data.getGenericType())) {
				final Collection<? extends Task> tasks = Util.castBound(data, Task.class);
				final Collection<Label> taskLabels = concreteOnly(Label.class);
				for (final Task task : tasks) {
					taskLabels.add(new Label(task.getLabel()));
				}
			}
			callbacks.onEvent(new DataChangeEvent(ADDED, executingUser.get(), data));
		}
	}

	@Override
	public synchronized <T extends Linkable> void remove(T data) {
		logger.debug("Removing element of type " + data.getClass().getSimpleName());
		final Collection<T> collection = concreteOnly(Util.classOf(data));
		if(!collection.contains(data)){
			throw new IllegalArgumentException("Element cannot be removed because it does not exist!");
		}
		collection.remove(data);
		callbacks.onEvent(new DataChangeEvent(REMOVED, executingUser.get(), data));
	}
	
	@Override
	public synchronized <T extends Linkable> void removeAll(Collection<T> data) {	
		logger.debug("Removing multiple elements of type " + data.getGenericType().getSimpleName());
		if (data.size() >= 1) {
			// TODO split per type only if data type is compound
			final Multimap<Class<T>, T> clazzToData = ArrayList.getMultiMap(data.getGenericType());
			for(final T item : data) {
				clazzToData.put(Util.classOf(item), item);
			}
			for(final Class<T> clazz : clazzToData.keySet()) {
				if(!concreteOnly(clazz).containsAll(clazzToData.get(clazz))) {
					throw new IllegalArgumentException("Elements cannot be removed because at least one of them does not exist!");
				}
			}
			for(final Class<T> clazz : clazzToData.keySet()) {
				final java.util.Collection<T> part = clazzToData.get(clazz);
				concreteOnly(clazz).removeAll(part);
				final ArrayList<T> wrapped = new ArrayList<>(clazz, part);
				callbacks.onEvent(new DataChangeEvent(REMOVED, executingUser.get(), wrapped));
			}
		}
	}
	
	@Override
	public synchronized <T extends Linkable> void removeAll(Class<T> clazz) {
		logger.debug("Removing all elements of type " + clazz.getSimpleName());
		final Collection<T> removed = getList(clazz);
		if (removed.size() >= 1) {
			for (final T t : removed) {
				logger.debug(t.toString());
			}
			concreteOrCompound(clazz).clear();
			callbacks.onEvent(new DataChangeEvent(REMOVED, executingUser.get(), removed));
		}
	}
	
	@Override
	public <T extends Linkable> void edit(T data) {
		edit(data, executingUser.get());
	}
	
	@Override
	public <T extends Linkable> void editBy(T data, User cause) {
		if (cause == User.UNKNOWN) {
			if (executingUser.get().isNormalUser()) {
				throw new IllegalStateException("Source user can be identified!");
			}
		} else if (cause != User.SYSTEM && cause != User.NULL) {
			throw new IllegalArgumentException("Cannot change source to arbitrary normal user!");
		}
		edit(data, cause);
	}
	
	private <T extends Linkable> void edit(T data, User cause) {
		logger.debug("Replacing element of type " + data.getClass().getSimpleName());
		final Collection<T> collection = concreteOnly(Util.classOf(data));
		if(!collection.contains(data)){
			throw new IllegalArgumentException("Element cannot be replaced because it does not exist!");
		}
		collection.remove(data);
		collection.add(data);
		callbacks.onEvent(new DataChangeEvent(EDITED, cause, data));
	}

	@Override
	public CombinedData getCombinedData() {
		return combined;
	}
	
	@Override
	public boolean hasIds(long[] ids) {
		return exists(concreteOrCompound(User.class), ids) == true ||
				exists(concreteOrCompound(Task.class), ids) == true;
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
		weakConsumerCallbacks.add(onUpdate);
		//TODO delete when tested with more than 10 sessions
		logger.debug("Callback object count: '" + weakConsumerCallbacks.size() + "'");
		if (weakConsumerCallbacks.size() > 20)
			logger.error("Memory leak: Callback objects not getting garbage collected!");
	}

	@Override
	public void registerPostProcessor(DataChangeCallback onUpdate) {
		weakPostProcessorCallbacks.add(onUpdate);
	}

}
