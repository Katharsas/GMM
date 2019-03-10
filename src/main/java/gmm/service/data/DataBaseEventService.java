package gmm.service.data;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.LinkedHashSet;
import gmm.collections.Set;
import gmm.domain.Linkable;
import gmm.service.data.DataAccess.DataChangeCallback;
import gmm.util.Util;

public class DataBaseEventService {

	private class OnChangeHandler<T extends Linkable> {
		private final DataChangeCallback<T> callback;
		private final Class<T> classFilter;
		
		public OnChangeHandler(DataChangeCallback<T> callback, Class<T> classFilter) {
			super();
			this.callback = callback;
			this.classFilter = classFilter;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (obj instanceof DataChangeCallback) return callback.equals(obj);
			if (obj instanceof OnChangeHandler) return callback.equals(((OnChangeHandler<?>)obj).callback);
			return false;
		}
		
		@Override
		public int hashCode() {
			return callback.hashCode();
		}
	}
	
	final private Set<OnChangeHandler<?>> consumerCallbacks = new LinkedHashSet<>(Util.castClass(OnChangeHandler.class));
	final private Set<OnChangeHandler<?>> postProcessorCallbacks = new LinkedHashSet<>(Util.castClass(OnChangeHandler.class));
	
	private boolean isEventConsumersActive = false;
	private boolean isPostProcessingActive = false;
	private Collection<DataChangeEvent<?>> postProcessingEvents = null;
	
	public <T extends Linkable> void registerPostProcessor(DataChangeCallback<T> onUpdate, Class<T> clazz) {
		synchronized(postProcessorCallbacks) {
			postProcessorCallbacks.add(new OnChangeHandler<>(onUpdate, clazz));
		}
	}
	
	public <T extends Linkable> void registerForUpdates(DataChangeCallback<T> onUpdate, Class<T> clazz) {
		synchronized (consumerCallbacks) {
			consumerCallbacks.add(new OnChangeHandler<>(onUpdate, clazz));
		}
	}
	
	// we cheat a little with hashCode and equals of OnChangeHandler to be able to do this
	@SuppressWarnings("unlikely-arg-type")
	public void unregister(DataChangeCallback<?> onUpdate) {
		synchronized(consumerCallbacks) {
			consumerCallbacks.remove(onUpdate);
		}
		synchronized (postProcessorCallbacks) {
			postProcessorCallbacks.remove(onUpdate);
		}
	}

	/**
	 * All events are guaranteed to contain collections of concrete types only.
	 */
	protected <T extends Linkable> void fireEvent(DataChangeEvent<T> event) {
		if (isEventConsumersActive) {
			throw new UnsupportedOperationException("Changing data as simple DataChangeEvent consumer is"
					+ " not supported! Try using a post processor.");
		}
		
		final Collection<T> changed = event.changed;
		final Class<T> changedType = changed.getGenericType();
		// we fire any callback that has registered on this type or a supertype
		
		if (isPostProcessingActive) {
			// If a post-processor-callback changes data (causing this method to be called recursively),
			// add new events to the list was created  below when we started iterating post-processors.
			// Note: Additional events caused by one post-processor will not be seen by others.
			
			if (event.type == DataChangeType.REMOVED || event.type == DataChangeType.ADDED) {
				// to allow this, we would need to make sure that multiple events per element are handled, 
				// for example if an  element is (EDITED +) REMOVED, we only emit REMOVED, similar for
				// ADDED (+ EDITED) and (ADDED +) REMOVED
				throw new UnsupportedOperationException("Adding and removing data is not supported as post"
						+ " processor DataChangeEvent consumer.");
			}
			
			// merge new event with existing events if possible, otherwise add it
			Collection<DataChangeEvent<?>> additionalEvents = new ArrayList<>(DataChangeEvent.getGenericClass());
			for (final DataChangeEvent<?> existingEvent : postProcessingEvents) {
				final Set<? extends Linkable> existing = existingEvent.changed;
				final Class<? extends Linkable> existingType = existing.getGenericType();
				if (existingType.equals(changedType) && existingEvent.type == event.type) {
					Util.cast(existing, changedType).addAll(changed);
				} else {
					additionalEvents.add(event);
				}
			}
			postProcessingEvents.addAll(additionalEvents);
		} else {
			// post processor listeners
			isPostProcessingActive = true;
			postProcessingEvents = new ArrayList<>(DataChangeEvent.getGenericClass(), event);
			synchronized(postProcessorCallbacks) {
				for (final OnChangeHandler<?> handler : postProcessorCallbacks) {
					checkFireEvent(handler, event);
				}
			}
			isPostProcessingActive = false;
			// simple listeners
			isEventConsumersActive = true;
			synchronized (consumerCallbacks) {
				for (final DataChangeEvent<?> processedEvent : postProcessingEvents) {
					for (final OnChangeHandler<?> handler : consumerCallbacks) {
						checkFireEvent(handler, processedEvent);
					}
				}
			}
			postProcessingEvents = null;
			isEventConsumersActive = false;
		}
	}
	
	/*
	 * Fire event handler if event's type matches event handler's type filter.
	 */
	private <T extends Linkable> void checkFireEvent(OnChangeHandler<T> handler, DataChangeEvent<?> event) {
		if (handler.classFilter.isAssignableFrom(event.changed.getGenericType())) {
			final DataChangeEvent<? extends T> castEvent = Util.castBound(event, handler.classFilter);
			handler.callback.onEvent(castEvent);
		}
	}
}
