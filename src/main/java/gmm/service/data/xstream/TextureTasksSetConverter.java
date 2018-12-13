package gmm.service.data.xstream;

import java.util.HashSet;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;

import gmm.domain.task.asset.AssetName;

public class TextureTasksSetConverter extends CollectionConverter {

public TextureTasksSetConverter(Mapper mapper, Class type) {
		super(mapper, type);
	}

	@Override
	public boolean canConvert(Class type) {
		return HashSet.class.isAssignableFrom(type);
	}

//	public TextureTasksSetConverter(Class type, Mapper mapper, String itemName, Class itemType) {
//		super(type, mapper, itemName, itemType);
//	}
	
//	@Override
//	protected void addCurrentElementToCollection(HierarchicalStreamReader reader, UnmarshallingContext context,
//			Collection collection, Collection target) {
//		 final Object item = readItem(reader, context, collection);
//	     target.add(item);
//	}
	
	@Override
	protected Object readItem(HierarchicalStreamReader reader, UnmarshallingContext context, Object current) {
		final Class itemType = AssetName.class;
		return context.convertAnother(current, itemType);
	}
}
