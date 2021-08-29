package gmm.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public class EventMap<K, V> implements Map<K, V>, EventMapSource<K,V> {
	
	private final Map<K, V> baseMap;
	
	private final java.util.List<BiConsumer<K,V>> onPutListeners = new CopyOnWriteArrayList<>();
	private final java.util.List<BiConsumer<K,V>> onRemoveListeners = new CopyOnWriteArrayList<>();
	
	public EventMap(Map<K, V> baseMap) {
		this.baseMap = baseMap;
	}
	
	@Override
	public Map<K, V> getLiveView() {
		return Collections.unmodifiableMap(baseMap);
	}
	
	@Override
	public void register(BiConsumer<K, V> onPut, BiConsumer<K, V> onRemove) {
		if (onPut != null) {
			onPutListeners.add(onPut);
		}
		if (onRemove != null) {
			onRemoveListeners.add(onRemove);
		}
	}
	
	@Override
	public int size() {
		return baseMap.size();
	}

	@Override
	public boolean isEmpty() {
		return baseMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return baseMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return baseMap.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return baseMap.get(key);
	}

	@Override
	public V put(K key, V value) {
		final V result = baseMap.put(key, value);
		for (final BiConsumer<K, V> onPut : onPutListeners) {
			onPut.accept(key, value);
		}
		return result;
	}

	@Override
	public V remove(Object key) {
		final V result = baseMap.remove(key);
		if (result != null) {
			@SuppressWarnings("unchecked")
			final K castKey = (K) key;
			for (final BiConsumer<K, V> onRemove : onRemoveListeners) {
				onRemove.accept(castKey, result);
			}
		}
		return result;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		baseMap.putAll(m);
		for (final Entry<? extends K, ? extends V> entry : m.entrySet()) {
			for (final BiConsumer<K, V> onPut : onPutListeners) {
				onPut.accept(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public void clear() {
		for (final Entry<? extends K, ? extends V> entry : this.entrySet()) {
			for (final BiConsumer<K, V> onRemove : onRemoveListeners) {
				onRemove.accept(entry.getKey(), entry.getValue());
			}
		}
		baseMap.clear();
	}

	@Override
	public Set<K> keySet() {
		return baseMap.keySet();
	}

	@Override
	public Collection<V> values() {
		return baseMap.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return baseMap.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		return baseMap.equals(o);
	}

	@Override
	public int hashCode() {
		return baseMap.hashCode();
	}
}
