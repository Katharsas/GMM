package gmm.domain;

import java.nio.file.Path;

public interface Asset {
	public int getSizeInBytes();
	public Path getPath();
}
