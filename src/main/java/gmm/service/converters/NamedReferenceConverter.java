package gmm.service.converters;

import org.springframework.stereotype.Service;

import gmm.domain.NamedObject;
import gmm.util.List;

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
	public void marshal(Object value, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		NamedObject named = (NamedObject) value;
		writer.addAttribute("idName", named.getIdName());
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		String idName = reader.getAttribute("idName");
        return NamedObject.getFromName(getNamedObjects(), idName);
	}
	
	abstract List<? extends NamedObject> getNamedObjects();
}
