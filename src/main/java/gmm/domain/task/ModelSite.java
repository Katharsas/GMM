package gmm.domain.task;

import gmm.domain.NamedObject;

public class ModelSite extends NamedObject{
	
	public ModelSite(String site) {
		super(site);
	}
	
	public String getSite() {
		return getName();
	}
}
