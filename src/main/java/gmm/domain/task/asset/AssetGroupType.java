package gmm.domain.task.asset;

public enum AssetGroupType {
	ORIGINAL("original"),
	NEW("newest");
	
	public static AssetGroupType get(boolean isOriginal) {
		return isOriginal ? ORIGINAL : NEW;
	}
	
	private final  String previewFileName;
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
