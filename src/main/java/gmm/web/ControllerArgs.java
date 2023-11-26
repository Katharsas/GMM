package gmm.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Simple wrapper for controller method arguments: ModelMap, Request, Response.<br>
 * <br>
 * Helps to keep method signature clean and readable even when all of the the wrapped objects need
 * to be injected into controller methods (e.g. to be passed to subsystems).
 * 
 * @author Jan Mothes
 */
public class ControllerArgs {
	public final ModelMap model;
	public final HttpServletRequest request;
	public final HttpServletResponse response;
	
	public ControllerArgs(ModelMap model, HttpServletRequest request, HttpServletResponse response) {
		this.model = model;
		this.request = request;
		this.response = response;
	}
	
	public RequestMethod getRequestMethod() {
		return RequestMethod.valueOf(request.getMethod().toUpperCase());
	}
}