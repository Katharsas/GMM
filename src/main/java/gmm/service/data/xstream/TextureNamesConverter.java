package gmm.service.data.xstream;

import java.util.Collection;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;

import gmm.domain.task.asset.AssetName;

/**
 * Example for how to customize item conversion (see {@link GmmCollectionConverter}).
 * Can unmarshal either {@link String} or {@link AssetName} objects from collection.
 */
public class TextureNamesConverter extends CollectionConverter {

	public TextureNamesConverter(Mapper mapper, Class<?> clazz) {
		super(mapper, clazz);
	}
	
	@Override
	public boolean canConvert(Class type) {
		return super.canConvert(type) || type.equals(gmm.collections.HashSet.class);
	}
	
	@Override
	protected void addCurrentElementToCollection(HierarchicalStreamReader reader, UnmarshallingContext context,
			Collection collection, Collection target) {
		Object item = readItem(reader, context, collection);
		if (Object.class.equals(String.class)) {
			item = new AssetName((String)item);
		}
        target.add(item);
	}

}
