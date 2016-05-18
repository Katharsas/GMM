package gmm.service.data.xstream;

import org.springframework.stereotype.Service;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import gmm.collections.Collection;
import gmm.domain.UniqueObject;

@Service
abstract class IdReferenceConverter implements Converter {

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		final UniqueObject obj = (UniqueObject) source;
		addAttributes(writer, obj);
	}
	
	/**
	 * Overwrite to add custom attributes. Always call super!
	 */
	protected void addAttributes(HierarchicalStreamWriter writer, UniqueObject source) {
		writer.addAttribute("id", source.getIdLink());
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		final String id = reader.getAttribute("id");
        return UniqueObject.getFromIdLink(getUniqueObjects(), id);
	}
	
	abstract Collection<? extends UniqueObject> getUniqueObjects();
}
