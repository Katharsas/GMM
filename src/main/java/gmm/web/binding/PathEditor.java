package gmm.web.binding;

import java.beans.PropertyEditorSupport;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathEditor extends PropertyEditorSupport{
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		Path p = Paths.get(text);
		this.setValue(p);
	}
	@Override
	public String getAsText() {
		Path p = (Path) this.getValue();
		return p.toString();
	}
}
