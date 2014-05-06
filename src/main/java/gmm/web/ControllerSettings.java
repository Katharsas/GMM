package gmm.web;

import gmm.web.binding.PathEditor;

import java.nio.file.Path;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
public class ControllerSettings {

	@InitBinder
	public void initBinder(WebDataBinder binder) {
	    binder.registerCustomEditor(Path.class, new PathEditor());
	}
}
