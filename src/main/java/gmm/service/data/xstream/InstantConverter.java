package gmm.service.data.xstream;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author Jan Mothes
 */
public class InstantConverter implements Converter {

	public static class XmlFormatException extends XStreamException {
		private static final long serialVersionUID = 22857905423146148L;
		public XmlFormatException(String message) {
			super(message);
		}
		public XmlFormatException(String message, Throwable cause) {
	        super(message, cause);
	    }
	}
	
	private static final String maxVersion = "2";
	
	private static DateTimeFormatter utilDateFormatter =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.[SSS][SS][S] z");
	
	@Override
	public boolean canConvert(Class type) {
		return Instant.class.isAssignableFrom(type);
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		final Instant obj = (Instant) source;
		writer.addAttribute("iso-8601", obj.toString());
		writer.addAttribute("version", maxVersion);
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		String version = null;
		for (final String attrName : (Iterable<String>) () -> reader.getAttributeNames()) {
			if (attrName.equals("version")) {
				version = reader.getAttribute("version");
				break;
			}
		}
		if (version == null) {
			return unmarshal_v1(reader);
		} else if (version.equals("2")) {
			return unmarshal_v2(reader);
		} else {
			throw new  XmlFormatException("Deserializing time from XML failed: "
					+ "Cannot read XML that was created by a newer version of this software! "
					+ "Version was '" + version + "', this software supports versions up to '" + maxVersion +"'.");
		}
	}
	
	public Object unmarshal_v1(HierarchicalStreamReader reader) {
		final String rootName = reader.getNodeName();
		final String utilDateString = reader.getValue();
		if (!(utilDateString == null || utilDateString.trim().isEmpty())) {
			try {
				return Instant.from(utilDateFormatter.parse(utilDateString));
			} catch(final DateTimeParseException e) {
				throw new XmlFormatException("Deserializing time from XML node '" + rootName + "' failed: "
						+ "Failed to parse text containing formatted dateTime.", e);
			}
		}
		Instant result = null;
		while(reader.hasMoreChildren()) {
			reader.moveDown();
			if (reader.getNodeName().equals("iMillis")) {
				try {
					result = Instant.ofEpochMilli(Long.parseLong(reader.getValue()));
				} catch(NumberFormatException | DateTimeException e) {
					throw new XmlFormatException("Deserializing time from XML node '" + rootName + "' failed: "
							+ "Failed to parse node 'iMillis' containing epoch dateTime.", e);
				}
			}
			reader.moveUp();
		}
		if (result != null) {
			return result;
		}
		throw new XmlFormatException("Deserializing time from XML node '" + rootName + "' failed: "
				+ "Could not find node 'iMillis' containing epoch time.");
	}
	
	public Object unmarshal_v2(HierarchicalStreamReader reader) {
		final String isoTime = reader.getAttribute("iso-8601");
		return Instant.parse(isoTime);
	}
}
