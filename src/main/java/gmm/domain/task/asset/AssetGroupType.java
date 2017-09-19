package gmm.domain.task.asset;

/**
 * Whether an asset is from the original game (included for comparison) or a new version created to replace the old one.
 * 
 * @author Jan Mothes
 */
public enum AssetGroupType {
	ORIGINAL("original"),
	NEW("newest");
	
	public static AssetGroupType get(boolean isOriginal) {
		return isOriginal ? ORIGINAL : NEW;
	}
	public static AssetGroupType get(String previewFileName) {
		for(final AssetGroupType type : AssetGroupType.values()) {
			if (type.getPreviewFileName().equals(previewFileName)) {
				return type;
			}
		}
		throw new IllegalArgumentException();
	}
	
	private final String previewFileName;
	private AssetGroupType(String previewFileName) {
		this.previewFileName = previewFileName;
	}
	public String getPreviewFileName() {
		return previewFileName;
	}
	public boolean isOriginal() {
		return this.equals(ORIGINAL);
	}
}
