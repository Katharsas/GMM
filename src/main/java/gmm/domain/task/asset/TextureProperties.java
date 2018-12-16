package gmm.domain.task.asset;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class TextureProperties extends AssetProperties {
	
	@XStreamAsAttribute
	private final int height, width;
	
	TextureProperties() {
		this(-1, -1);
	}
	
	public TextureProperties(int height, int width) {
		this.height = height;
		this.width = width;
	}

	@Deprecated
	@Override
	public void assertAttributes() {
		super.assertAttributes();
		synchronized(this) {
			if (height < 0 || width < 0) {
				throw new IllegalStateException(assertAttributesException);
			}
		}
	}
	
//	public synchronized void setDimensions(int height, int width) {
//		this.height = height;
//		this.width = width;
//	}
	
	public synchronized int getHeight() {
		return height;
	}
	public synchronized int getWidth() {
		return width;
	}
}
