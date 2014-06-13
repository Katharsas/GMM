package gmm.domain;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class Texture extends Asset {
	
	@XStreamAsAttribute
	private int height;
	@XStreamAsAttribute
	private int width;
	
	public Texture(Path base, Path relative) throws IOException {
		super(base, relative);
		BufferedImage image = ImageIO.read(absolute.toFile());
		height = image.getHeight();
		width = image.getWidth();
	}
	
	public int getHeight() {
		return height;
	}
	public int getWidth() {
		return width;
	}
}
