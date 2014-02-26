package gmm.domain;

public class AssetTask extends Task {

	private String originalAssetFileName;
	private String originalAssetDirectory;
	private String newAssetDirectory;
	
	public AssetTask(String name, User author, String originalAssetPath) {
		super(name, author);
		System.out.println(originalAssetPath);
	}

}
