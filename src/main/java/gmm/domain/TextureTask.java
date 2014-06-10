package gmm.domain;

//import java.io.IOException;

//import gmm.service.AssetService;
import gmm.collections.HashSet;
import gmm.collections.Set;

public class TextureTask extends AssetTask {
	
	final public Set<ModelTask> models = new HashSet<ModelTask>();
	
	public TextureTask(User author) {
		super(author);
	}
}
