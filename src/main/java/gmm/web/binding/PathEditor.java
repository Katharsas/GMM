package gmm.web.binding;

import java.beans.PropertyEditorSupport;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathEditor extends PropertyEditorSupport{
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		try {text = java.net.URLDecoder.decode(text, "UTF-8");}
		catch (UnsupportedEncodingException e) {e.printStackTrace();}
		
		Path p = Paths.get(text);
		this.setValue(p);
	}
	@Override
	public String getAsText() {
		Path p = (Path) this.getValue();
		return p.toString();
	}
}
