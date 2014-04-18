package gmm.service;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
 
public class Spring implements ApplicationContextAware{
	private static ApplicationContext ctx = null;
	public static ApplicationContext getApplicationContext() {
		return ctx;
	}
	@SuppressWarnings("static-access")
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		this.ctx = ctx;
	}
	public static <T> T get(Class<T> clazz) {
		return ctx.getBean(clazz);
	}
}