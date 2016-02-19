package gmm.domain.task.asset;

import java.nio.file.Path;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class Texture extends Asset {
	
	@XStreamAsAttribute
	private int height = -1, width = -1;
	
	public Texture(Path relative, AssetGroupType groupType) {
		super(relative, groupType);
	}
	
	@Override
	public void assertAttributes() {
		super.assertAttributes();
		if (height < 0 || width < 0) {
			throw new IllegalStateException(assertAttributesException);
		}
	}
	
	public void setDimensions(int height, int width) {
		this.height = height;
		this.width = width;
	}
	
	public int getHeight() {
		return height;
	}
	public int getWidth() {
		return width;
	}
}
