package gmm.domain;

import gmm.collections.HashSet;
import gmm.collections.Set;

public class TextureTask extends AssetTask<Texture> {
	
	final public Set<ModelTask> models = new HashSet<ModelTask>();
	
	public TextureTask(User author) {
		super(author);
	}
}
