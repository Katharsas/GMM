package gmm.domain.task.asset;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class TextureProperties extends AssetProperties {
	
	@XStreamAsAttribute
	private final int height, width;
	
	TextureProperties() {
		this.height = -1;
		this.width = -1;
	}
	
	public TextureProperties(int height, int width) {
		if (height <= 0 || width <= 0) {
			throw new IllegalArgumentException("Height and width must be positive!");
		}
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
