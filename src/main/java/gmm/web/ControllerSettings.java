package gmm.web;

import gmm.web.binding.PathEditor;

import java.nio.file.Path;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ControllerSettings {

	@InitBinder
	public void initBinder(WebDataBinder binder) {
	    binder.registerCustomEditor(Path.class, new PathEditor());
	}
	
	@ExceptionHandler(AjaxResponseException.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public ExceptionWrapper handleExceptions(AjaxResponseException ex, HttpServletRequest request)
	{
	    ExceptionWrapper exceptionVO = new ExceptionWrapper(ex.getCause());
	    ex.printStackTrace();
	    return exceptionVO;
	}
	
	public static class ExceptionWrapper {
		private final String message;
		private final String stackTrace;
		public ExceptionWrapper(Throwable e) {
			message = e.getLocalizedMessage();
			stackTrace = ExceptionUtils.getStackTrace(e);
		}
		public String getMessage() {
			return message;
		}
		public String getStackTrace() {
			return stackTrace;
		}
	}
}
