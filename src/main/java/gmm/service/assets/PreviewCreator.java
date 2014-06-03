package gmm.service.assets;

import gmm.domain.AssetTask;

import java.io.IOException;
import java.nio.file.Path;

public interface PreviewCreator {

	public void createPreview(Path sourceFile, AssetTask targetTask, boolean original) throws IOException;
}
