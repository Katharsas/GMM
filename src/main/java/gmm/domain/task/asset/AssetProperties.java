package gmm.domain.task.asset;

import java.text.DecimalFormat;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public abstract class AssetProperties {
	
//	@XStreamAsAttribute
//	private final Path subPath;
	@XStreamAsAttribute
	private final AssetGroupType groupType;
	@XStreamAsAttribute
	private long sizeInBytes = -1;
	
	protected final static String assertAttributesException =
			"This asset's attributes are not fully populated!";
	
	public AssetProperties(AssetGroupType groupType) {
//		Objects.requireNonNull(subPath);
		Objects.requireNonNull(groupType);
//		this.subPath = subPath;
		this.groupType = groupType;
	}
	
	public void assertAttributes() {
		if(sizeInBytes < 0) {
			throw new IllegalStateException(assertAttributesException);
		}
	}

	public void setFileSize(long sizeInBytes) {
		this.sizeInBytes = sizeInBytes;
	}

	public String getSizeInKB() {
		final DecimalFormat d = new DecimalFormat("########0");
		return d.format(((Long)sizeInBytes).doubleValue()/1000);
	}
	
	public String getSizeInMB() {
		final DecimalFormat d = new DecimalFormat("########0,00");
		return d.format(((Long)sizeInBytes).doubleValue()/1000000);
	}
	
//	public String getFileName() {
//		return subPath.getFileName().toString();
//	}

	public AssetGroupType getGroupType() {
		return groupType;
	}
}
