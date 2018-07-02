package gmm.service.data;

import static gmm.service.data.DataChangeType.ADDED;
import static gmm.service.data.DataChangeType.EDITED;
import static gmm.service.data.DataChangeType.REMOVED;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
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
	
	@SuppressWarnings("rawtypes")
	final private Map<DataChangeCallback, Class> weakPostProcessorCallbacks = new WeakHashMap<>();
	@SuppressWarnings("rawtypes")
	final private Map<DataChangeCallback, Class> weakConsumerCallbacks = new WeakHashMap<>();
	
	
	// ###################
	// DATA TYPES
	// ###################
	
	/** Contains all types (concrete & compound) which can be mapped to collections of data.
	 */
	private final Set<Class<? extends Linkable>> listTypes = new HashSet<>(null, Util.createArray(new Class<?>[]{}));
	
	/** Map compound types to all concrete types they include.
	 */
	private final Multimap<Class<? extends Linkable>, Class<? extends Linkable>> compoundTypesToConcreteTypes = ArrayListMultimap.create();
	
	// ###################
	// DATA
	// ###################
	
	/** Map concrete types to collections of objects of respective type.
	 */
	final private CollectionTypeMap<Linkable> concretes = new CollectionTypeMap<>();
	
	/** Maps supertypes (=compounds) to collection views of objects of concrete types from {@link #concretes}.
	 */
	final private CollectionTypeMap<Linkable> compounds = new CollectionTypeMap<>();
	
	// ###################
	// INIT
	// ###################
	
	private DataBase() {
		
		// Initialize lists for concrete types
		initConcreteLists(Util.createArray(new Class<?>[] {
			User.class,
			GeneralTask.class,
			TextureTask.class,
			ModelTask.class,
			Label.class
		}));
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
	private void initConcreteLists(Class<? extends Linkable>[] types) {
		for (final Class<? extends Linkable> type : types) {
			// even though ArrayLists are used,
			// public methods ensure elements cannot be added twice
			final ArrayList<? extends Linkable> list = new ArrayList<>(type);
			concretes.putSafe((Class<Linkable>) type, (ArrayList<Linkable>) list);// fuck u generics
		}
		listTypes.addAll(Arrays.asList(types));
	}
	
	/**
	 * Initializes a live combined list view of the lists from given {@code includedTypes} and maps
	 * them to given type in {@link DataBase#compounds}.
	 * 
	 * @param includedTypes - Included types can be concrete or compound. If compound, they must already be initialized.
	 */
	private <T extends Linkable> void initCompoundList(Class<T> type, Class<? extends T>[] includedTypes) {
		final Collection<? extends T>[] included = Util.createArray(new Collection<?>[includedTypes.length]);
		for (int i = 0; i < included.length; i++) {
			Collection<? extends T> list = concretes.getSafe(includedTypes[i]);
			if (list == null) {
				list = compounds.getSafe(includedTypes[i]);
				if (list == null) {
					throw new IllegalArgumentException("Cannot build compound list:"
							+ " Included type '" + includedTypes[i] + "' unknown!");
				}
				compoundTypesToConcreteTypes.putAll(type, compoundTypesToConcreteTypes.get(includedTypes[i]));
			} else {
				compoundTypesToConcreteTypes.put(type, includedTypes[i]);
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
	private synchronized <T extends Linkable> Collection<T> concreteOrCompound(Class<T> clazz) {
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
	private synchronized <T extends Linkable> Collection<T> concreteOnly(Class<T> clazz) {
		final Collection<T> result = concretes.getSafe(clazz);
		if (result != null) {
			return result;
		} else {
			throw new IllegalArgumentException(
					"A list for type '" + clazz.getSimpleName() + "' cannot be found!");
		}
	}
	
	@Override
	public synchronized <T extends Linkable> Collection<T> getList(Class<T> clazz) {
		return concreteOrCompound(clazz).copy();
	}

	@Override
	public <T extends Linkable> void add(T data) {
		logger.debug("Adding element of type " + data.getClass().getSimpleName());
		synchronized(this) {
			final Collection<T> collection = concreteOnly(Util.classOf(data));
			if (collection.contains(data)) {
				throw new IllegalArgumentException("Element cannot be added because it already exists!");
			}
			collection.add(data);
			final Collection<Label> taskLabels = concreteOnly(Label.class);
			if(data instanceof Task) {
				final Task task = (Task) data;
				final Label taskLabel = new Label(task.getLabel());
				if (!taskLabels.contains(taskLabel)) {
					taskLabels.add(taskLabel);
				}
			}
		}
		fireEvent(new DataChangeEvent<>(ADDED, executingUser.get(), data));
	}
	
	@Override
	public <T extends Linkable> void addAll(Collection<T> data) {
		logger.debug("Adding multiple elements of type " + data.getGenericType().getSimpleName());
		final Collection<DataChangeEvent<?>> events = new ArrayList<>(DataChangeEvent.getGenericClass());
		synchronized(this) {
			this.<T>mapToConcreteGroups(data, concreteData -> {
				final Class<? extends Linkable> concreteType = concreteData.getGenericType();
				@SuppressWarnings("unchecked")
				final Collection<Linkable> concreteExisting = (Collection<Linkable>) concreteOnly(concreteType);
				if(!Collections.disjoint(concreteData, concreteExisting)) {
					throw new IllegalArgumentException("Elements cannot be added because at least one of them already exist!");
				}
				if (Task.class.isAssignableFrom(concreteType)) {
					for (final Linkable element : concreteData) {
						final Task task = (Task) element;
						final Label taskLabel = new Label(task.getLabel());
						if (!concreteOnly(Label.class).contains(taskLabel)) {
							concreteOnly(Label.class).add(taskLabel);
						}
					}
				}
				concreteExisting.addAll(concreteData);
				events.add(new DataChangeEvent<>(ADDED, executingUser.get(), concreteData));
			});
		}
		fireEvents(events);
	}

	@Override
	public <T extends Linkable> void remove(T data) {
		logger.debug("Removing element of type " + data.getClass().getSimpleName());
		synchronized(this) {
			final Collection<T> collection = concreteOnly(Util.classOf(data));
			if(!collection.contains(data)){
				throw new IllegalArgumentException("Element cannot be removed because it does not exist!");
			}
			collection.remove(data);
		}
		fireEvent(new DataChangeEvent<>(REMOVED, executingUser.get(), data));
	}
	
	/**
	 * Groups items per concrete class. Creates event for each group.
	 */
	@Override
	public <T extends Linkable> void removeAll(Collection<T> data) {	
		logger.debug("Removing multiple elements of type " + data.getGenericType().getSimpleName());
		final Collection<DataChangeEvent<?>> events = new ArrayList<>(DataChangeEvent.getGenericClass());
		synchronized(this) {
			mapToConcreteGroups(data, concreteData -> {
				final Class<? extends Linkable> concreteType = concreteData.getGenericType();
				if(!concreteOnly(concreteType).containsAll(data)) {
					throw new IllegalArgumentException("Elements cannot be removed because at least one of them does not exist!");
				}
				concreteOnly(concreteType).removeAll(concreteData);
				events.add(new DataChangeEvent<>(REMOVED, executingUser.get(), concreteData));
			});
		}
		fireEvents(events);
	}
	
	/**
	 * Splits a collection of compound/concrete type into multiple collections, each of concrete type.
	 * 
	 * @param data - A collection with concrete or compound generic type.
	 * @param action - An action that will be supplied with collections of concrete generic type only, called once for
	 * 		each such collection so that each item from data will be supplied once. Simply passes data if data is concrete.
	 */
	private <T extends Linkable> void mapToConcreteGroups(Collection<T> data, Consumer<Collection<? extends T>> action) {
		if (data.size() >= 1) {
			final Class<T> genericType = data.getGenericType();
			final java.util.Set<Class<? extends Linkable>> concreteTypes = concretes.keySet();
			// If data is concrete, pass it, else create mapping
			if (concreteTypes.contains(genericType)) {
				action.accept(data);
			} else {
				final Multimap<Class<T>, T> clazzToData = ArrayListMultimap.create();
				for(final T item : data) {
					clazzToData.put(Util.classOf(item), item);
				}
				for(final Class<T> clazz : clazzToData.keySet()) {
					final ArrayList<? extends T> concreteData = new ArrayList<>(clazz, clazzToData.get(clazz));
					action.accept(concreteData);
				}
			}
		}
	}
	
	@Override
	public <T extends Linkable> void removeAll(Class<T> clazz) {
		logger.debug("Removing all elements of type " + clazz.getSimpleName());
		final Collection<DataChangeEvent<?>> events = new ArrayList<>(DataChangeEvent.getGenericClass());
		synchronized(this) {
			if (concretes.containsKey(clazz)) {
				clearConcrete(clazz, events);
			} else {
				for (final Class<? extends Linkable> concreteClass : compoundTypesToConcreteTypes.get(clazz)) {
					clearConcrete(concreteClass, events);
				}
			}
		}
		fireEvents(events);
	}
	
	private <T extends Linkable> void clearConcrete(Class<T> concreteType, Collection<DataChangeEvent<?>> events) {
		final Collection<T> toRemove = concreteOnly(concreteType);
		if (toRemove.size() >= 1) {
			final Collection<T> removed = toRemove.copy();
			toRemove.clear();
			events.add(new DataChangeEvent<T>(REMOVED, executingUser.get(), removed));
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
		synchronized(this) {
			final Collection<T> collection = concreteOnly(Util.classOf(data));
			if(!collection.contains(data)){
				throw new IllegalArgumentException("Element cannot be replaced because it does not exist!");
			}
			collection.remove(data);
			collection.add(data);
		}
		fireEvent(new DataChangeEvent<T>(EDITED, cause, data));
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
	public <T extends Linkable> void registerPostProcessor(DataChangeCallback<T> onUpdate, Class<T> clazz) {
		synchronized(weakPostProcessorCallbacks) {
			weakPostProcessorCallbacks.put(onUpdate, clazz);
		}
	}
	
	@Override
	public <T extends Linkable> void registerForUpdates(DataChangeCallback<T> onUpdate, Class<T> clazz) {
		synchronized (weakConsumerCallbacks) {
			weakConsumerCallbacks.put(onUpdate, clazz);
		}
	}
	
	private void fireEvents(Collection<DataChangeEvent<?>> events) {
		for (final DataChangeEvent<?> event : events) {
			fireEvent(event);
		}
	}

	/**
	 * All events are guaranteed to contain collections of concrete types only.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void fireEvent(DataChangeEvent<?> event) {
		final Class<? extends Linkable> concreteType = event.changed.getGenericType();
		// we fire any callback that has registered on this type or a supertype

		final int maxCallbacks = weakPostProcessorCallbacks.size() + weakConsumerCallbacks.size();
		final Collection<DataChangeCallback> callbacks = new ArrayList<>(DataChangeCallback.class, maxCallbacks);
		
		synchronized(weakPostProcessorCallbacks) {
			for (final Map.Entry<DataChangeCallback, Class> entry : weakPostProcessorCallbacks.entrySet()) {
				if (entry.getValue().isAssignableFrom(concreteType)) {
					callbacks.add(entry.getKey());
				}
			}
		}
		synchronized (weakConsumerCallbacks) {
			for (final Map.Entry<DataChangeCallback, Class> entry : weakConsumerCallbacks.entrySet()) {
				if (entry.getValue().isAssignableFrom(concreteType)) {
					callbacks.add(entry.getKey());
				}
			}
		}
		
		for (final DataChangeCallback callback : callbacks) {
			callback.onEvent(event);
		}
	}
}
