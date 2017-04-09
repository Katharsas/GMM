package gmm.domain.task.asset;

import java.nio.file.Path;
import java.util.function.Function;

import gmm.service.data.DataConfigService;

public enum FileType {

	ASSET(config -> config.subAssets()),
	WIP(config -> config.subOther());
	
	Function<DataConfigService, Path> subPathGetter;
	
	private FileType(Function<DataConfigService, Path> subPathGetter) {
		this.subPathGetter = subPathGetter;
	}
	
	public boolean isAsset() {
		return this.equals(ASSET);
	}
	
	public Path getSubPath(DataConfigService config) {
		return subPathGetter.apply(config);
	}
}
