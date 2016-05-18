package gmm.service.data.xstream;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

public class PathConverter extends AbstractSingleValueConverter{
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean canConvert(Class clazz) {
		return Path.class.isAssignableFrom(clazz);
	}

	@Override
	public Object fromString(String str) {
		return Paths.get(str);
	}
}
