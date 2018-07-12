package gmm.domain.task.asset;

import java.nio.file.Path;
import java.util.function.Function;

import gmm.service.data.PathConfig;

public enum FileType {

	ASSET(config -> config.subAssets()),
	WIP(config -> config.subOther());
	
	Function<PathConfig, Path> subPathGetter;
	
	private FileType(Function<PathConfig, Path> subPathGetter) {
		this.subPathGetter = subPathGetter;
	}
	
	public boolean isAsset() {
		return this.equals(ASSET);
	}
	
	public Path getSubPath(PathConfig config) {
		return subPathGetter.apply(config);
	}
}
