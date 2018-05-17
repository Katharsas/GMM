package gmm;

import java.beans.PropertyEditor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.Version;
import gmm.util.ElFunctions;
import gmm.web.ControllerArgsResolver;
import gmm.web.binding.PathEditor;

/**
 * Contains all non-web.xml, non-security settings.
 * 
 * @author Jan Mothes
 */
@Configuration
@ComponentScan(basePackages = {"gmm", "com.technologicaloddity"})
@EnableWebMvc
@EnableScheduling
@Import({ WebSocketConfiguration.class })
@PropertySource("classpath:default.properties")
@PropertySource(value = "file:./config.properties", ignoreResourceNotFound = true)
public class ApplicationConfiguration implements WebMvcConfigurer {

	@Autowired
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @PostConstruct
    public void init() {
        requestMappingHandlerAdapter.setIgnoreDefaultModelOnRedirect(true);
    }
	
	/**
	 * ----------------------------- Basic Config -----------------------------
	 */
	
	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping() {
		final RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
		mapping.setUseSuffixPatternMatch(false);
		mapping.setUseTrailingSlashMatch(false);
		return mapping;
	}
	
	/**
	 * Provides property-files for fmt:message keys from jsps
	 */
	@Bean
	public MessageSource messageSource() {
		final ResourceBundleMessageSource source = new ResourceBundleMessageSource();
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
	 * Enable Spring to wrap some controller method parameters into RequestData object.
	 */
	@Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new ControllerArgsResolver());
    }
	
	/**
	 * ViewResolver resolve the String return value from controller methods into templates (jsp or ftl).
	 */
	@Bean
	public ViewResolver jspViewResolver() {
		final InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setOrder(255);
		resolver.setViewClass(JstlView.class);
		resolver.setPrefix("/WEB-INF/jsp/");
		resolver.setSuffix(".jsp");
		return resolver;
	}
	@Bean
	public ViewResolver ftlViewResolver() {
	    final FreeMarkerViewResolver resolver = new FreeMarkerViewResolver();
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
		final FreeMarkerConfigurer result = new FreeMarkerConfigurer();

		// template path
		result.setTemplateLoaderPath("/WEB-INF/ftl/");
		result.setDefaultEncoding("UTF-8");

		// static access
		final Version version = freemarker.template.Configuration.getVersion();
		final BeansWrapper wrapper = new BeansWrapper(version);
		final TemplateHashModel statics = wrapper.getStaticModels();
		final Map<String, Object> shared = new HashMap<>();
		for (final Class<?> clazz : ElFunctions.staticClasses) {
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
	
	/**
	 * Configure Jackson (enable Path to Json/String conversion)
	 */
	@Bean
	public MappingJackson2HttpMessageConverter customJackson2HttpMessageConverter(MappingJackson2HttpMessageConverter converter) {
		
	    final ObjectMapper objectMapper = new ObjectMapper();
	    
	    final SimpleModule sm = new SimpleModule();
    	sm.addSerializer(Path.class, new StdSerializer<Path>(Path.class) {
			private static final long serialVersionUID = 8963132152002562810L;
			@Override
			public void serialize(Path value, JsonGenerator gen, SerializerProvider serializers)
					throws IOException, JsonProcessingException {
				gen.writeString(value.toString());
			}
		});
    	objectMapper.registerModule(sm);
	    
	    converter.setObjectMapper(objectMapper);
	    return converter;
	}
	
	/**
	 * We cannot add a new jackson converter (because it will be ignoredif one exists already), so we need to either use
	 * other method {@link #configureMessageConverters(List)} which will mean that we loose all default converters OR we
	 * need to find the existing jackson converter and change it (which is done below).
	 */
	@Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		
		MappingJackson2HttpMessageConverter originalJacksonConverter = null;
		for (final HttpMessageConverter<?> converter : messageConverters) {
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				originalJacksonConverter = (MappingJackson2HttpMessageConverter) converter;
			}
		}
		Objects.requireNonNull(originalJacksonConverter);
		customJackson2HttpMessageConverter(originalJacksonConverter);
    }
	
	/**
	 * Quickfix for Spring @Scheduled bug, see:
	 * https://stackoverflow.com/questions/49343692/websocketconfigurer-and-scheduled-are-not-work-well-in-an-application
	 */
	@Bean
	public TaskScheduler taskScheduler() {
	    final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
	    taskScheduler.setPoolSize(2);
	    taskScheduler.initialize();
	    return taskScheduler;
	}
	
	/**
	 * ----------------------------- Custom Beans -----------------------------
	 */
	
//	@Bean
//	public MailSender mailSender() {
//		return new JavaMailSenderImpl();
//	}
	
	@Bean
	public gmm.service.Spring applicationContextProvider() {
		return new gmm.service.Spring();
	}
	
	/**
	 * Exposes environment properties to @Value annotations.
	 * Environment properties are configured using @PropertySource on @Configuration classes.
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	
	/**
	 * Expose to <spring:eval expression="@config.getProperty(...)"  in jsps
	 */
	@Bean
	public PropertiesFactoryBean config() {
		final PropertiesFactoryBean factory = new PropertiesFactoryBean();
		factory.setLocation(new ClassPathResource("meta.properties"));
		return factory;
	}
	
	/**
	 * @Value Path converter
	 */
	@Bean
	public static CustomEditorConfigurer editorConfigurer() {
		final CustomEditorConfigurer conf = new CustomEditorConfigurer();
		final Map<Class<?>, Class<? extends PropertyEditor>>  map = new HashMap<>();
		
		map.put(Path.class, PathEditor.class);
		
		conf.setCustomEditors(map);
		return conf;
	}
}
