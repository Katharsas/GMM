package gmm.domain.task.asset;

import java.nio.file.Path;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import gmm.collections.Set;

public class Model extends Asset {

	@XStreamAsAttribute
	private int polyCount = -1;
	
	private Set<String> textureNames;

	public Model(Path relative, AssetGroupType groupType) {
		super(relative, groupType);
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
