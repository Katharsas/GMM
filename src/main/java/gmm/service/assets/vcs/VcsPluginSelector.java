package gmm.service.assets.vcs;

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
		String[] value();
	}
	
	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		
		final String[] identifiers = (String[]) metadata.getAnnotationAttributes(
				ConditionalOnConfigSelector.class.getName()).get("value");
		
		String selector = context.getEnvironment().getProperty("vcs.selector");
		if (selector == null) selector = "";
		for (final String identifier : identifiers) {
			if (identifier.equals(selector)) return true;
		}
		return false;
	}
}