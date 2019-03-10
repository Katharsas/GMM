package gmm.domain.task.asset;

import java.text.DecimalFormat;
import java.util.Optional;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Represents those properties of the asset that may change when an asset content is actually
 * edited (as opposed to simply moved to another folder). Treat non-transient fields as immutable. 
 * 
 * @author Jan Mothes
 */
public abstract class AssetProperties {
	
	protected final static String assertAttributesException =
			"This asset's attributes are not fully populated!";
	
	@XStreamAsAttribute
	private long sizeInBytes = -1;
	@XStreamAsAttribute
	private long lastModified = -1;
	@XStreamAsAttribute
	private byte[] sha1;
	
	private boolean isBuilt = false;
	
	private void checkMutability() {
		if (isBuilt) {
			throw new IllegalStateException("This field is immutable.");
		}
	}
	
	/**
	 * Poor man's builder pattern.
	 */
	public void build() {
		isBuilt = true;
	}

	public void setSizeInBytes(long sizeInBytes) {
		checkMutability();
		if (sizeInBytes <= 0) {
			throw new IllegalArgumentException("Size must be positive!");
		}
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
	
	public void setLastModified(long lastModified) {
		checkMutability();
		if (lastModified <= 0) {
			throw new IllegalArgumentException("LastModified must be positive!");
		}
		this.lastModified = lastModified;
	}
	
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * Optional because GMM versions < 0.6.3 did not save sha1 on preview creation
	 */
	public Optional<byte[]> getSha1() {
		return Optional.ofNullable(sha1);
	}
	
	public void setSha1(byte[] sha1) {
		checkMutability();
		this.sha1 = sha1;
	}
}
