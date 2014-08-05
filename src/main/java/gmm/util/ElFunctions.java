package gmm.util;
import gmm.domain.TaskPriority;
import gmm.domain.TaskStatus;
import gmm.domain.TaskType;
import gmm.service.sort.TaskSortAttribute;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.objenesis.instantiator.ObjectInstantiator;


public class ElFunctions {
	
	
	public final static Objenesis objenesis = new ObjenesisStd();
	
	public static String escape(String input) {
		return StringEscapeUtils.escapeHtml4(StringEscapeUtils.escapeEcmaScript(input));
	}
	
	public static Enum<?>[] values(String clazz) {
		if (clazz.equals(TaskStatus.class.getSimpleName())) {
			return TaskStatus.values();
		}
		else if (clazz.equals(TaskPriority.class.getSimpleName())) {
			return TaskPriority.values();
		}
		else if (clazz.equals(TaskType.class.getSimpleName())) {
			return TaskPriority.values();
		}
		else if (clazz.equals(TaskSortAttribute.class.getSimpleName())) {
			return TaskSortAttribute.values();
		}
		else {
			throw new IllegalArgumentException("Argument String \""+clazz+"\" can not be matched with enum class name.");
		}
	}
	
	/**
	 * This method is probably the most evil thing i have written in my live.
	 * I beg forgiveness.
	 */
	public static <T> T instanceOfClass(String clazz) throws ClassNotFoundException {
		@SuppressWarnings("unchecked")
		Class<T> resolvedClass = (Class<T>) getClassFromSimpleString(clazz);
		ObjectInstantiator<T> instantiator = objenesis.getInstantiatorOf(resolvedClass);
		return resolvedClass.cast(instantiator.newInstance());
	}
	
	private static Class<?> getClassFromSimpleString(String clazz) throws ClassNotFoundException {
		final Package[] packages = Package.getPackages();
		Class<?> result = null;
		for (final Package p : packages) {
	        final String pack = p.getName();
	        final String tentative = pack + "." + clazz;
	        try {
	            result = Class.forName(tentative);
	        } catch (final ClassNotFoundException e) {
	            continue;
	        }
	        break;
	    }
        if(result == null) {
        	throw new ClassNotFoundException("Cannot find class for String \""+clazz+"\".");
        } else {
            return result;
        }
	}
}
