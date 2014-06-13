package gmm.domain;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

public abstract class Asset {
	
	@XStreamAsAttribute
	protected Path relative;
	@XStreamOmitField
	protected Path absolute;
	
	public Asset(Path base, Path relative) {
		Objects.requireNonNull(relative);
		this.relative = relative;
		setBase(base);
	}
	
	public void setBase(Path base) {
		Objects.requireNonNull(base);
		absolute = base.resolve(relative);
		if(!absolute.toFile().isFile()) throw new IllegalArgumentException("Asset file does not exist!");
	}
	
	private long getSize() {
		return absolute.toFile().length();
	}
	
	public String getSizeInKB() {
		DecimalFormat d = new DecimalFormat("########0");
		return d.format(((Long)getSize()).doubleValue()/1000);
	}
	
	public String getSizeInMB() {
		DecimalFormat d = new DecimalFormat("########0,00");
		return d.format(((Long)getSize()).doubleValue()/1000000);
	}
	
	public Path getPath() {
		return relative;
	}
}
