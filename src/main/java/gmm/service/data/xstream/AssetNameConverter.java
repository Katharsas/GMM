package gmm.service.data.xstream;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

import gmm.domain.task.asset.AssetName;

public class AssetNameConverter extends AbstractSingleValueConverter {

	@Override
	public boolean canConvert(Class clazz) {
		return AssetName.class.isAssignableFrom(clazz);
	}

	@Override
	public Object fromString(String str) {
		return new AssetName(str);
	}
	
	@Override
	public String toString(Object obj) {
		if (obj == null) return null;
		return ((AssetName)obj).get();
	}
}
