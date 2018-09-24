package gmm.domain.task.asset;

import java.util.Collection;

import gmm.domain.UniqueObject;

/**
 * Simple String wrapper for type-safe case-insensitive asset name uniquely identifying an asset.
 */
public class AssetKey implements Comparable<AssetKey> {
	
	/**
	 * @param c - collection of tasks
	 * @param idLink - identifier for a task
	 * @return The asset key of the task in the given collection with the the given idLink
	 * @throws NullPointerException if the given collection does not contain a task with the given idLink
	 */
	public static <U extends AssetTask<?>> AssetKey getFromIdLink(Collection<U> c, String idLink) {
		return UniqueObject.getFromIdLink(c, idLink).getAssetName().getKey();
	}
	
	protected final String assetNameKey;
	
	protected AssetKey(String key) {
		this.assetNameKey = key;
	}
	@Override
	public int hashCode() {
		return assetNameKey.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof AssetKey)) return false;
		final AssetKey other = (AssetKey) obj;
		return assetNameKey.equals(other.assetNameKey);
	}
	
	/**
	 * @return - The unique asset name key (same letters as actual name but normalized case).
	 */
	@Override
	public String toString() {
		return assetNameKey;
	}
	@Override
	public int compareTo(AssetKey o) {
		return assetNameKey.compareTo(o.assetNameKey);
	}
}