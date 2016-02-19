package gmm.domain.task.asset;

import java.nio.file.Path;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class Model extends Asset {

	@XStreamAsAttribute
	private int polyCount = -1;
	
	public Model(Path relative, AssetGroupType groupType) {
		super(relative, groupType);
	}
	
	@Override
	public void assertAttributes() {
		super.assertAttributes();
		if (polyCount < 0) {
			throw new IllegalStateException(assertAttributesException);
		}
	}
	
	public int getPolyCount() {
		return polyCount;
	}

	public void setPolyCount(int polyCount) {
		this.polyCount = polyCount;
	}
}
