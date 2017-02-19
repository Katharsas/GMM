package gmm.domain.task.asset;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import gmm.collections.Set;

public class ModelProperties extends AssetProperties {

	@XStreamAsAttribute
	private int polyCount = -1;
	
	private Set<String> textureNames;

	public ModelProperties(AssetGroupType groupType) {
		super(groupType);
	}
	
	@Override
	public void assertAttributes() {
		super.assertAttributes();
		if (polyCount < 0 || textureNames == null) {
			throw new IllegalStateException(assertAttributesException);
		}
	}
	
	public int getPolyCount() {
		return polyCount;
	}

	public void setPolyCount(int polyCount) {
		this.polyCount = polyCount;
	}
	
	public Set<String> getTextureNames() {
		return textureNames;
	}

	public void setTextureNames(Set<String> textureNames) {
		this.textureNames = textureNames;
	}
}
