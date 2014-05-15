package gmm.domain;

//import java.io.IOException;

//import gmm.service.AssetService;
import gmm.util.HashSet;
import gmm.util.Set;

public class TextureTask extends AssetTask {
	
	final public Set<ModelTask> models = new HashSet<ModelTask>();
	
	public TextureTask(String idName, User author) {
		super(idName, author);
	}
}
