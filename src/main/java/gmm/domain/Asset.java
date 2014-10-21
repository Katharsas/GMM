package gmm.domain;

import gmm.service.Spring;
import gmm.service.data.DataConfigService;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public abstract class Asset {
	
	DataConfigService config = Spring.get(DataConfigService.class);
	
	@XStreamAsAttribute
	private final Path fileName;
	private final AssetTask<?> owner;
	private final boolean isOriginal;
	
	public Asset(Path newFileName, AssetTask<?> owner) {
		Objects.requireNonNull(owner);
		this.isOriginal = newFileName == null;
		this.fileName = isOriginal ? owner.getAssetPath() : newFileName;
		Objects.requireNonNull(this.fileName);
		this.owner = owner;
	}
	
	protected Path getAbsolute() {
		//TODO security to here
		//TODO move all this to assettask. remove knowlege about original
		Path path = isOriginal ? config.ASSETS_ORIGINAL : config.ASSETS_NEW;
		path = path.resolve(owner.getAssetPath());
		if (!isOriginal) path = path.resolve(config.SUB_ASSETS).resolve(fileName);
		return path;
	}
	
	private long getSize() {
		return getAbsolute().toFile().length();
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
		return fileName;
	}
}
