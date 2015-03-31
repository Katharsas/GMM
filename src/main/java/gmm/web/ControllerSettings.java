package gmm.web;

import gmm.service.UserService;
import gmm.service.data.CombinedData;
import gmm.service.data.DataAccess;
import gmm.web.binding.PathEditor;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ControllerSettings {

	@Autowired private DataAccess data;
	@Autowired private UserService users;
	
	@ModelAttribute
	public CombinedData getCombinedData() {
		return data.getCombinedData();
	}
	
	@ModelAttribute
	public String getNewLine() {
		return "\n";
	}
	
	@InitBinder
	public void initBinder(WebDataBinder binder) {
	    binder.registerCustomEditor(Path.class, new PathEditor());
	}
	
	/**
	 * Intercepts exceptions and returns JSON or HTML error based on request accept header.
	 * 
	 * http://stackoverflow.com/questions/29157818/spring-restful-exceptionhandler-has-too-high-priority-order
	 * 
	 * @see http://www.newmediacampaigns.com/blog/browser-rest-http-accept-headers
	 * @see http://stackoverflow.com/questions/12977368/how-to-change-the-content-type-in-exception-handler
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleExceptions(Exception ex, HttpServletRequest request) throws Exception {
		
		if (ex instanceof AccessDeniedException) {
			System.err.println("##########################################################");
			System.err.println("  AccessDeniedException below can be ignored! ");
			System.err.println("  User will automatically be redirected to login!");
			System.err.println("##########################################################");
			throw ex;
		}
		ex.printStackTrace();
		HttpHeaders headers = new HttpHeaders();
		if(users.isUserLoggedIn()) {
			//sort MIME types from accept header field
			String accept = request.getHeader("accept");
			if(accept != null) {
				String[] mimes = accept.split(",");
				//sort most quality first
				Arrays.sort(mimes, new MimeQualityComparator());
				//if json, return as json
				String firstMime = mimes[0].split(";")[0];
				if (firstMime.equals("application/json")) {
					ExceptionWrapper exceptionVO = new ExceptionWrapper(ex);
					headers.setContentType(MediaType.APPLICATION_JSON);
					return new ResponseEntity<ExceptionWrapper>(exceptionVO, headers, HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}
			//if not json, return as html
			headers.setContentType(MediaType.TEXT_HTML);
			String error = "<h1>Internal Server Error 500</h1><br>";
			error += "Please copy the following text and send it to your server admin:<br><br>";
			error += "<pre>" + ExceptionUtils.getStackTrace(ex) + "</pre>";
			return new ResponseEntity<String>(error, headers, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<String>("Internal Server Error 500: Login for more information.", headers, HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	private static class MimeQualityComparator implements Comparator<String> {
		@Override
		public int compare(String m1, String m2) {
			//split of quality factor
			double m1Quality = getQualityofMime(m1);
			double m2Quality = getQualityofMime(m2);
			return Double.compare(m1Quality, m2Quality) * -1;
		}
	}
	
	/**
	 * Extract mime quality from one of the mime;quality Strings from the accept
	 * header of a HTTP request, according to HTTP spec.
	 */
	private static Double getQualityofMime(String mimeAndQuality) {
		//split of quality factor
		String[] mime = mimeAndQuality.split(";");
		if (mime.length <= 1) {
			return 1.0;
		} else {
			String quality = mime[1].split("=")[1];
			return Double.parseDouble(quality);
		}
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
