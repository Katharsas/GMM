package gmm.domain;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

public abstract class Asset {
	
	@XStreamAsAttribute
	private final Path fileName;
	
	@XStreamOmitField
	private Path absolutePath;
	
	public Asset(Path fileName) {
		Objects.requireNonNull(fileName);
		this.fileName = fileName;
	}
	
	protected void setAbsolute(Path absolutePath) {
		this.absolutePath = absolutePath;
	}
	
	protected Path getAbsolute() {
		return absolutePath;
	}
	
	private long getSize() {
		return absolutePath.toFile().length();
	}
	
	public String getSizeInKB() {
		DecimalFormat d = new DecimalFormat("########0");
		return d.format(((Long)getSize()).doubleValue()/1000);
	}
	
	public String getSizeInMB() {
		DecimalFormat d = new DecimalFormat("########0,00");
		return d.format(((Long)getSize()).doubleValue()/1000000);
	}
	
	public String getFileName() {
		return fileName.getFileName().toString();
	}
}
