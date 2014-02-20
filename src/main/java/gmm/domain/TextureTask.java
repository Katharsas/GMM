package gmm.domain;

import gmm.util.LinkedList;
import gmm.util.List;

public class TextureTask extends FileTask {

	final private List<ModelTask> models = new LinkedList<ModelTask>();
	
	public TextureTask(String idName, User author, File oldFile) {
		super(idName, author, oldFile);
	}
	
	public List<ModelTask> getModels() {
		return models;
	}
}
