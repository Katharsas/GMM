package gmm.service.converters;

import gmm.collections.Collection;
import gmm.domain.NamedObject;
import gmm.domain.UniqueObject;

import org.springframework.stereotype.Service;





import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@Service
abstract class NamedReferenceConverter implements Converter{

	@SuppressWarnings("rawtypes")
	@Override
	public boolean canConvert(Class clazz) {
		return false;
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		NamedObject named = (NamedObject) source;
		writer.addAttribute("id", named.getIdLink());
		writer.addAttribute("name", named.getName());
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		String id = reader.getAttribute("id");
        return UniqueObject.getFromIdLink(getNamedObjects(), id);
	}
	
	abstract Collection<? extends NamedObject> getNamedObjects();
}
