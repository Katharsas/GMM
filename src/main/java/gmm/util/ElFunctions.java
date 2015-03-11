package gmm.util;
import java.util.HashMap;
import java.util.Map;

import gmm.domain.task.TaskPriority;
import gmm.domain.task.TaskStatus;
import gmm.domain.task.TaskType;
import gmm.service.sort.TaskSortAttribute;
import gmm.web.forms.WorkbenchLoadForm;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.objenesis.instantiator.ObjectInstantiator;


public class ElFunctions {
	
	public final static Objenesis objenesis = new ObjenesisStd();
	public final static Map<String, Class<?>> classNameToEnum = new HashMap<>();
	static {
		registerEnum(TaskStatus.class);
		registerEnum(TaskPriority.class);
		registerEnum(TaskSortAttribute.class);
		registerEnum(TaskType.class);
		registerEnum(WorkbenchLoadForm.LoadOperation.class);
	}
	
	public static String escape(String input) {
		return StringEscapeUtils.escapeHtml4(StringEscapeUtils.escapeEcmaScript(input));
	}
	
	/**
	 * Returns enum values for registered enum classes.<br>
	 * Necessary because JSP cannot call stuff on classes, only objects, and sometimes, you just
	 * don't have a damn object or don't want to use one. Also its bad practice to call class stuff
	 * on objects.
	 */
	public static Enum<?>[] values(String clazz) {
		if (!classNameToEnum.containsKey(clazz)) {
			throw new UnsupportedOperationException("Enum class '"+clazz+"' not registered at ELFunctions!");
		}
		Class<?> enumClass = classNameToEnum.get(clazz);
		return (Enum<?>[]) enumClass.getEnumConstants();
	}
	
	private static void registerEnum(Class<?> enumClass) {
		classNameToEnum.put(enumClass.getSimpleName(), enumClass);
	}
	
	/**
	 * <b>Don't copy paste this stuff, cuz its evil.</b><br>
	 * Creates ANY object from simple class name. With any i mean ANY, no restrictions.<br>
	 * Used to call class methods on that object in JSP, because classes can't be used in JSP.<br>
	 * Use carefully, because this is probably slow as fuck.
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
