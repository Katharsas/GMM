package gmm.domain.task.asset;

import java.text.DecimalFormat;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public abstract class AssetProperties {
	
	@XStreamAsAttribute
	private final String filename;
	@XStreamAsAttribute
	private final AssetGroupType groupType;
	@XStreamAsAttribute
	private long sizeInBytes = -1;
	@XStreamAsAttribute
	private long lastModified = -1;
	
	protected final static String assertAttributesException =
			"This asset's attributes are not fully populated!";
	
	public AssetProperties(String filename, AssetGroupType groupType) {
		Objects.requireNonNull(filename);
		Objects.requireNonNull(groupType);
		this.filename = filename;
		this.groupType = groupType;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(filename, groupType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		final AssetProperties other = (AssetProperties) obj;
		return filename.equals(other.filename) && groupType == other.groupType;
	}
	
	public void assertAttributes() {
		if(sizeInBytes < 0 || lastModified < 0) {
			throw new IllegalStateException(assertAttributesException);
		}
	}

	public void setSizeInBytes(long sizeInBytes) {
		this.sizeInBytes = sizeInBytes;
	}
	
	public long getSizeInBytes() {
		return sizeInBytes;
	}

	public String getSizeInKB() {
		final DecimalFormat d = new DecimalFormat("########0");
		return d.format(((Long)sizeInBytes).doubleValue()/1000);
	}
	
	public String getSizeInMB() {
		final DecimalFormat d = new DecimalFormat("########0,00");
		return d.format(((Long)sizeInBytes).doubleValue()/1000000);
	}
	
	public String getFilename() {
		return filename;
	}

	public AssetGroupType getGroupType() {
		return groupType;
	}
	
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}
	
	public long getLastModified() {
		return lastModified;
	}
}
