package gmm.domain;

public class ModelSite extends NamedObject{
	
	public ModelSite(String site) {
		super(site);
	}
	
	public String getSite() {
		return getName();
	}
}
