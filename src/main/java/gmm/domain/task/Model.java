package gmm.domain.task;

import java.nio.file.Path;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class Model extends Asset {

	@XStreamAsAttribute
	private int polyCount;
	
	public Model(Path relative) {
		super(relative);
	}
	
	public int getPolyCount() {
		return polyCount;
	}

	public void setPolyCount(int polyCount) {
		this.polyCount = polyCount;
	}
}
