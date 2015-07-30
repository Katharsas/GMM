package gmm.web;

import org.springframework.security.access.AccessDeniedException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Blocks AccessDeniedExceptions which are thrown by custom @ExceptionHandler
 * 
 * @see {@link gmm.web.ControllerSettings#handleExceptions(Exception, javax.servlet.http.HttpServletRequest)}
 * @author Jan Mothes
 */
public class LogAccessDeniedFilter extends Filter<ILoggingEvent> {
	
	private static String loggerName =
			"org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver";
	private static String messagePart = "Failed to invoke @ExceptionHandler method";
	private static String exceptionClass = AccessDeniedException.class.getName();
	
	@Override
	public FilterReply decide(ILoggingEvent event) {
		if (!event.getLevel().equals(Level.ERROR)) return FilterReply.NEUTRAL;
		if (event.getLoggerName().equals(loggerName)) {
			if(event.getMessage().contains(messagePart)
					&& event.getThrowableProxy() != null) {
				if(event.getThrowableProxy().getClassName().equals(exceptionClass)) {
					return FilterReply.DENY;
				}
			}
		}
		return FilterReply.NEUTRAL;
	}
}
