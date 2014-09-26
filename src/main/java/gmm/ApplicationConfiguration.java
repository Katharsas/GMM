package gmm;

import gmm.web.binding.PathEditor;

import java.beans.PropertyEditor;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

@Configuration
@ComponentScan(basePackages = {"gmm", "com.technologicaloddity"})
@ImportResource({"classpath:applicationContext-security.xml"})
@EnableWebMvc
public class ApplicationConfiguration extends WebMvcConfigurerAdapter {
	
	private static Resource configFile = new ClassPathResource("config.properties");

	/**
	 * ----------------------------- Basic Config -----------------------------
	 */
	
	/**
	 * Provides property-files for fmt:message keys from jsps
	 */
	@Bean
	public MessageSource messageSource() {
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("i18n.messages");
		return source;
	}
	
	/**
	 * Map links starting with res (used for css/js files) to appropriate folder
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
	    registry.addResourceHandler("/res/**").addResourceLocations("/resources/");
	}
	
	/**
	 * Resolves the name given from controller to a file by using prefix and suffix
	 */
	@Bean
	public ViewResolver viewResolver() {
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setViewClass(JstlView.class);
		resolver.setPrefix("/WEB-INF/jsp/");
		resolver.setSuffix(".jsp");
		return resolver;
	}
	
	/**
	 * Handle Multipart File Upload
	 */
	@Bean
	public MultipartResolver multipartRsolver() {
		return new CommonsMultipartResolver();
	}
	
	/**
	 * ----------------------------- Custom Beans -----------------------------
	 */
	
	@Bean
	public gmm.service.Spring applicationContextProvider() {
		return new gmm.service.Spring();
	}
	
	/**
	 * Exposes property file with meta information to @Value annotation.
	 */
	@Bean
	public static PropertyPlaceholderConfigurer propertyConfigurer() {
		PropertyPlaceholderConfigurer conf =  new PropertyPlaceholderConfigurer();
		conf.setLocation(configFile);
		return conf;
	}
	
	/**
	 * Expose to <spring:eval expression="@config.getProperty(...)"  in jsps
	 */
	@Bean
	public PropertiesFactoryBean config() {
		PropertiesFactoryBean factory = new PropertiesFactoryBean();
		factory.setLocation(configFile);
		return factory;
	}
	
	/**
	 * @Value Path converter
	 */
	@Bean
	public static CustomEditorConfigurer editorConfigurer() {
		CustomEditorConfigurer conf = new CustomEditorConfigurer();
		Map<Class<?>, Class<? extends PropertyEditor>>  map = new HashMap<>();
		
		map.put(Path.class, PathEditor.class);
		
		conf.setCustomEditors(map);
		return conf;
	}
}
