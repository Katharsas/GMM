package gmm.service;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
 
public class Spring implements ApplicationContextAware{
	
	private static ApplicationContext aContext = null;

	@SuppressWarnings("static-access")
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		this.aContext = ctx;
	}
	
	public static ApplicationContext getApplicationContext() {
		return aContext;
	}
	
	public static <T> T get(Class<T> clazz) {
		return aContext.getBean(clazz);
	}
}