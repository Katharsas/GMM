package gmm.domain;

import java.nio.file.Path;

import gmm.collections.HashSet;
import gmm.collections.Set;

public class TextureTask extends AssetTask<Texture> {
	
	final public Set<ModelTask> models = new HashSet<ModelTask>();
	
	public TextureTask(User author, Path assetPath) throws Exception {
		super(author, assetPath);
	}
}
