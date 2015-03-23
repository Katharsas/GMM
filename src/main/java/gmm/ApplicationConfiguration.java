package gmm;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import gmm.util.ElFunctions;
import gmm.web.binding.PathEditor;

import java.beans.PropertyEditor;
import java.io.IOException;
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
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

/**
 * Contains all non-web.xml, non-security settings.
 * 
 * @author Jan Mothes
 */
@Configuration
@ComponentScan(basePackages = {"gmm", "com.technologicaloddity"})
@EnableWebMvc
public class ApplicationConfiguration extends WebMvcConfigurerAdapter {
	
	private static Resource configFile = new ClassPathResource("config.properties");

	/**
	 * ----------------------------- Basic Config -----------------------------
	 */
	
	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping() {
		RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
		mapping.setUseSuffixPatternMatch(false);
		mapping.setUseTrailingSlashMatch(false);
		return mapping;
	}
	
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
	 * ViewResolver resolve the String return value from controller methods into templates (jsp or ftl).
	 */
	@Bean
	public ViewResolver jspViewResolver() {
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setOrder(255);
		resolver.setViewClass(JstlView.class);
		resolver.setPrefix("/WEB-INF/jsp/");
		resolver.setSuffix(".jsp");
		return resolver;
	}
	@Bean
	public ViewResolver ftlViewResolver() {
	    FreeMarkerViewResolver resolver = new FreeMarkerViewResolver();
	    resolver.setOrder(0);
	    resolver.setCache(true);
//	    resolver.setPrefix("");
	    resolver.setSuffix(".ftl");
	    return resolver;
	}
	/**
	 * FreeMarker (ftl) configuration
	 */
	@Bean
	public FreeMarkerConfigurer freemarkerConfig() throws IOException, TemplateException {
		FreeMarkerConfigurer result = new FreeMarkerConfigurer();

		// template path
		result.setTemplateLoaderPath("/WEB-INF/ftl/");

		// static access
		BeansWrapper wrapper = new BeansWrapper();
		TemplateHashModel statics = wrapper.getStaticModels();
		Map<String, Object> shared = new HashMap<>();
		for (Class<?> clazz : ElFunctions.staticClasses) {
			shared.put(clazz.getSimpleName(), statics.get(clazz.getName()));
		}
		result.setFreemarkerVariables(shared);

		return result;
	}
	
	
	/**
	 * Handle Multipart File Upload
	 */
	@Bean
	public MultipartResolver multipartResolver() {
		return new CommonsMultipartResolver();
	}
	
//	@Bean
//	public HandlerExceptionResolver handlerExceptionResolver() {
//		HandlerExceptionResolver result = new ExceptionHandlerExceptionResolver();
//		result.
//		return result;
//	}
	
//	@Bean
//	public ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver() {
//		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
//		resolver.
//		return resolver;
//	}
	
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
