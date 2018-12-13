package gmm.service.data.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import gmm.domain.task.asset.AssetName;
import gmm.service.data.xstream.InstantConverter.XmlFormatException;

public class AssetNameObjectConverter implements Converter {

	@Override
	public boolean canConvert(Class type) {
		return String.class.isAssignableFrom(type);
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		final AssetName obj = (AssetName) source;
		writer.setValue(obj.get());
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		System.out.println("Reading");
		final String assetnameString = reader.getValue();
		if (!(assetnameString == null || assetnameString.trim().isEmpty())) {
			System.out.println(assetnameString);
			return new AssetName(assetnameString);
		} else {
			while (reader.hasMoreChildren()) {
				reader.moveDown();
				final String nodeName = reader.getNodeName();
				if (nodeName.equals("assetNameOriginal") || nodeName.equals("string")) {
					System.out.println(nodeName);
					System.out.println("\n" + reader.getValue() + "\n");
					return new AssetName(reader.getValue());
				}
				reader.moveUp();
			}
			throw new XmlFormatException("Could not parse AssetName from xml!");
		}
	}
}
