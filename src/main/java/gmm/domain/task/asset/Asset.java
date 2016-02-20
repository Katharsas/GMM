package gmm.domain.task.asset;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public abstract class Asset {
	
	@XStreamAsAttribute
	private final Path fileName;
	@XStreamAsAttribute
	private final AssetGroupType groupType;
	@XStreamAsAttribute
	private long fileSize = -1;
	
	protected final static String assertAttributesException =
			"This asset's attributes are not fully populated!";
	
	public Asset(Path fileName, AssetGroupType groupType) {
		Objects.requireNonNull(fileName);
		Objects.requireNonNull(groupType);
		this.fileName = fileName;
		this.groupType = groupType;
	}
	
	public void assertAttributes() {
		if(fileSize < 0) {
			throw new IllegalStateException(assertAttributesException);
		}
	}

	public void setFileSize(Path absolutePath) {
		this.fileSize = absolutePath.toFile().length();
	}

	public String getSizeInKB() {
		DecimalFormat d = new DecimalFormat("########0");
		return d.format(((Long)fileSize).doubleValue()/1000);
	}
	
	public String getSizeInMB() {
		DecimalFormat d = new DecimalFormat("########0,00");
		return d.format(((Long)fileSize).doubleValue()/1000000);
	}
	
	public String getFileName() {
		return fileName.getFileName().toString();
	}

	public AssetGroupType getGroupType() {
		return groupType;
	}
}
