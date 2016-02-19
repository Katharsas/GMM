package gmm.domain.task.asset;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

public abstract class Asset {
	
	@XStreamAsAttribute
	private final Path fileName;
	@XStreamAsAttribute
	private final AssetGroupType groupType;
	
	@XStreamOmitField
	private Path absolutePath;
	
	protected final static String assertAttributesException =
			"This asset's attributes are not fully populated!";
	
	public Asset(Path fileName, AssetGroupType groupType) {
		Objects.requireNonNull(fileName);
		Objects.requireNonNull(groupType);
		this.fileName = fileName;
		this.groupType = groupType;
	}
	
	public void assertAttributes() {}
	
	protected void setAbsolute(Path absolutePath) {
		this.absolutePath = absolutePath;
	}
	
	public Path getAbsolute() {
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

	public AssetGroupType getGroupType() {
		return groupType;
	}
}
