package gmm.domain;

import java.nio.file.Path;

public class Model extends Asset {

	public Model(Path relative, AssetTask<Model> owner) {
		super(relative, owner);
	}
}
