package gmm.domain.task.asset;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class TextureProperties extends AssetProperties {
	
	@XStreamAsAttribute
	private int height = -1, width = -1;
	
	public TextureProperties(String filename, AssetGroupType groupType) {
		super(filename, groupType);
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
