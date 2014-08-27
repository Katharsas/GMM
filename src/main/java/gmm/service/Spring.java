package gmm.service;

import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.ServletContextAware;
 
public class Spring implements ApplicationContextAware, ServletContextAware{
	
	private static ApplicationContext aContext = null;
	private static ServletContext sContext = null;

	@SuppressWarnings("static-access")
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		this.aContext = ctx;
	}
	
	@SuppressWarnings("static-access")
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.sContext = servletContext;
		
	}
	
	public static ApplicationContext getApplicationContext() {
		return aContext;
	}
	
	public static ServletContext getServletContext() {
		return sContext;
	}
	
	public static <T> T get(Class<T> clazz) {
		return aContext.getBean(clazz);
	}


}