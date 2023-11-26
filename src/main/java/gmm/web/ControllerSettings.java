package gmm.web;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

import gmm.service.data.DataAccess;
import gmm.service.users.CurrentUser;
import gmm.web.binding.PathEditor;

@ControllerAdvice
public class ControllerSettings {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * HTTP Exceptions will not be logged as thoroughly as usual exceptions when thrown.
	 */
	private static abstract class HttpException extends RuntimeException {
		private static final long serialVersionUID = 8252605336944237356L;
		public HttpException() {}
		public HttpException(String message) {
			super(message);
		}
	}
	
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public static class NotFoundException extends HttpException {
		private static final long serialVersionUID = 8672281667419693687L;
		public NotFoundException() {}
		public NotFoundException(String message) {
			super(message);
		}
	}
	
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public static class InternalServerError extends HttpException {
		private static final long serialVersionUID = 4262187988791948941L;
		public InternalServerError() {}
		public InternalServerError(String message) {
			super(message);
		}
	}

	@Value("${admin.email:}")
	private String emailAddress;
	
//	@Autowired private MailSender mailSender;
	
	private final DataAccess data;
	private final CurrentUser user;
	
	@Autowired
	public ControllerSettings(DataAccess data, CurrentUser user) {
		this.data = data;
		this.user = user;
	}
	
	@ModelAttribute("customAdminBanner")
	public String getCustomAdminBanner() {
		return data.getCombinedData().getCustomAdminBanner();
	}
	@ModelAttribute("isCustomAdminBannerActive")
	public boolean getIsCustomAdminBannerActive() {
		return data.getCombinedData().isCustomAdminBannerActive();
	}
	@ModelAttribute("newLine")
	public String getNewLine() {
		return "\n";
	}
	
	@ModelAttribute
	public void populateModel(Model model) {
		final boolean isUserLoggedIn = user.isLoggedIn();
		model.addAttribute("isUserLoggedIn", isUserLoggedIn);
		if (isUserLoggedIn) {
			model.addAttribute("principal", user.get());
		}
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
	 * @see http://stackoverflow.com/questions/27654206/session-timeout-leads-to-access-denied-in-spring-mvc-when-csrf-integration-with
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleExceptions(Exception ex, HttpServletRequest request) throws Exception {
		
		if (ex instanceof AccessDeniedException) {			
			// Spring itself will catch, log and redirect, we don't need to handle this.
			// Spring logging this exception is blocked with logger-filter to hold log clean.
			throw ex;
		} else if (ex instanceof NotFoundException) {
			throw ex; // Spring will return 404
		}
		if (ex instanceof HttpException) {
			logger.warn("Controller returned '" + ex.getClass().getSimpleName() + "': " + ex.getMessage());
		} else {
			logger.error("Controller threw exception:", ex);
		}
//		if(!emailAddress.isEmpty()) {
//			SimpleMailMessage msg = new SimpleMailMessage();
//			msg.setTo(emailAddress);
//			msg.setSubject("GMM Exception occured");
//			msg.setText(ExceptionUtils.getStackTrace(ex));
//			mailSender.send(msg);
//		}
		
		final HttpHeaders headers = new HttpHeaders();
		Object answer; // String if HTML, any object if JSON
		if(jsonHasPriority(request.getHeader("accept"))) {
			logger.info("Returning exception to client as json object");
			headers.setContentType(MediaType.APPLICATION_JSON);
			answer = errorJson(ex, user.isLoggedIn());
		} else {
			logger.info("Returning exception to client as html page");
			headers.setContentType(MediaType.TEXT_HTML);
			answer = errorHtml(ex, user.isLoggedIn());
		}
		final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		return new ResponseEntity<>(answer, headers, status);
	}
	
	private String notLoggedInErrorHtml() {
		return "Login for detailed error descriptions.";
	}
	
	private String errorHtml(Exception e, boolean isUserLoggedIn) {
		String error = "<h1>Internal Server Error 500</h1><br>";
		if (isUserLoggedIn) {
			error += "Please copy the following text and send it to your server admin:<br><br>";
			error += "<pre>" + ExceptionUtils.getStackTrace(e) + "</pre>";
		} else {
			error += notLoggedInErrorHtml();
		}
		return error;
	}
	
	private ExceptionWrapper errorJson(Exception e, boolean isUserLoggedIn) {
		if (isUserLoggedIn) {
			return new ExceptionWrapper(e);
		} else {
			return new ExceptionWrapper(notLoggedInErrorHtml());
		}
	}
	
	/**
	 * @param acceptString - HTTP accept header field, format according to HTTP spec:
	 * 		"mime1;quality1,mime2;quality2,mime3,mime4,..." (quality is optional)
	 * @return true only if json is the MIME type with highest quality of all specified MIME types.
	 */
	private boolean jsonHasPriority(String acceptString) {
		if (acceptString != null) {
			final String[] mimes = acceptString.split(",");
			Arrays.sort(mimes, new MimeQualityComparator());
			final String firstMime = mimes[0].split(";")[0];
			return firstMime.equals("application/json");
		}
		return false;
	}
	
	private static class MimeQualityComparator implements Comparator<String> {
		@Override
		public int compare(String mime1, String mime2) {
			final double m1Quality = getQualityofMime(mime1);
			final double m2Quality = getQualityofMime(mime2);
			return Double.compare(m1Quality, m2Quality) * -1;
		}
	}
	
	/**
	 * @param mimeAndQuality - "mime;quality" pair from the accept header of a HTTP request,
	 * 		according to HTTP spec (missing mimeQuality means quality = 1).
	 * @return quality of this pair according to HTTP spec.
	 */
	private static Double getQualityofMime(String mimeAndQuality) {
		//split off quality factor
		final String[] mime = mimeAndQuality.split(";");
		if (mime.length <= 1) {
			return 1.0;
		} else {
			final String quality = mime[1].split("=")[1];
			return Double.parseDouble(quality);
		}
	}
	
	public static class ExceptionWrapper {
		private final String message;
		private final String stackTrace;
		public ExceptionWrapper(String customMessage) {
			message = customMessage;
			stackTrace = null;
		}
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
