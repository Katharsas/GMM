package gmm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
@ImportResource({"classpath:applicationContext.xml"})
public class I18nConfiguration {
	
	/**
	 * Provides property-files for fmt:message keys from jsps
	 */
	@Bean(name = "messageSource")
	public ResourceBundleMessageSource messageSource() {
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("messages");
		return source;
	}
}
