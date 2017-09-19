package gmm.domain.task.asset;

import java.nio.file.Path;
import java.util.Objects;

import gmm.util.StringUtil;

/**
 * Simple immutable asset filename wrapper which also knows about the corresponding case-insensitive (lower case) but
 * otherwise equivalent String that serves as unique key for asset names (used to hide case-insensitivity from classes
 * that need to handle asset name as key, allows them to use usual case-sensitive compare operations).
 * 
 * Ensures that the asset name string is not a "hidden" path (no slashes allowed etc.).
 */
public class AssetName implements Comparable<AssetName> {
	
	private static final char[] ILLEGAL_CHARACTERS =
		{ '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };
	
	private final String assetNameLowered;
	private final String assetNameOriginal;
	
	public AssetName(String assetName) {
		Objects.requireNonNull(assetName);
		assertValidAssetName(assetName);
		this.assetNameLowered = assetName.toLowerCase();
		assetNameOriginal = assetName;
	}
	
	public AssetName(Path path) {
		Objects.requireNonNull(path);
		final String assetName = path.getFileName().toString();
		assertValidAssetName(assetName);
		this.assetNameLowered = assetName.toLowerCase();
		assetNameOriginal = assetName;
	}
	
	private void assertValidAssetName(String assetName) {
		for (final char c : assetName.toCharArray()) {
			for (final char illegal : ILLEGAL_CHARACTERS) {
				if (c == illegal) {
					throw new IllegalArgumentException("Character '" + c + "' not allowed in asset name!");
				}
			}
		}
	}

	@Override
	public int hashCode() {
		return assetNameLowered.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		return ((AssetName) obj).assetNameLowered.equals(assetNameLowered);
	}
	
	@Override
	public int compareTo(AssetName o) {
		return assetNameLowered.compareTo(o.assetNameLowered);
	}
	
	@Override
	public String toString() {
		return assetNameOriginal;
	}
	
	public void assertPathMatch(Path path) {
		Objects.requireNonNull(path);
		final StringUtil stringUtils = StringUtil.ignoreCase();
		if (!stringUtils.equals(path.getFileName().toString(), assetNameOriginal)) {
			throw new IllegalArgumentException("Asset file/folder '" + path.toString() + "' does"
					+ " not match this asset name '" + assetNameOriginal + "'!");
		}
	}
	
	/**
	 * @return - The actual name of the asset file (in original case).
	 */
	public String get() {
		return assetNameOriginal;
	}
	
	/**
	 * @return - The unique asset name key (same letters as actual name but normalized case).
	 */
	public String getKey() {
		return assetNameLowered;
	}
}
