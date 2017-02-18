package gmm.service.data.vcs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class VcsPluginSelector implements Condition {
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Conditional(VcsPluginSelector.class)
	public static @interface ConditionalOnConfigSelector {
		String value();
	}
	
	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		
		final String identifier = (String) metadata.getAnnotationAttributes(
				ConditionalOnConfigSelector.class.getName()).get("value");
		
		final String selector = context.getEnvironment().getProperty("vcs.selector");
		return selector != null && identifier.equals(selector);
	}
}