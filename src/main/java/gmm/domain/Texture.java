package gmm.domain;

import java.nio.file.Path;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class Texture extends Asset {
	
	@XStreamAsAttribute
	int height, width;
	
	public Texture(Path relative) {
		super(relative);
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
