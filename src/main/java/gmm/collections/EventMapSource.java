package gmm.collections;

import java.util.Map;
import java.util.function.BiConsumer;

public interface EventMapSource<K, V> {
	public Map<K,V> getLiveView();
	public void register(BiConsumer<K,V> onPut, BiConsumer<K,V> onRemove);
}
