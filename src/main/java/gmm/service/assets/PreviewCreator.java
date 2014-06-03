package gmm.service.assets;

import java.io.IOException;
import java.nio.file.Path;

public interface PreviewCreator {

	public void createPreview(Path sourceFile, Path targetFolder, boolean original) throws IOException;
}
