package gmm.service.data.xstream;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.SerializableConverter;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.HashSet;
import gmm.collections.LinkedList;

/**
 * Can read legacy format (produced by {@link ReflectionConverter} originally) and version "2" format.
 * New format uses {@link CollectionConverter} which allows easy specialized item conversion by providing
 * {@link CollectionConverterSupplier} as constructor arg.
 * 
 * @author Jan Mothes
 */
public class GmmCollectionConverter implements Converter {

	private final Mapper mapper;
	private final ReflectionProvider reflectionProvider;
	private final CollectionConverterSupplier createCollectionConverter;
	
	@FunctionalInterface
	public static interface CollectionConverterSupplier {
		public CollectionConverter create(Mapper mapper, Class<?> standardCollectionImpl);
	}
	
	public GmmCollectionConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
		this.mapper = mapper;
		this.reflectionProvider = reflectionProvider;
		createCollectionConverter = (thatMapper, clazz) -> new CollectionConverter(thatMapper, clazz);
	}
	
	/**
	 * @param createCollectionConverter use custom collection/item converter, only works since version "2" of collection
	 * serializing.
	 */
	public GmmCollectionConverter(Mapper mapper, ReflectionProvider reflectionProvider,
			CollectionConverterSupplier createCollectionConverter) {
		this.mapper = mapper;
		this.reflectionProvider = reflectionProvider;
		this.createCollectionConverter = createCollectionConverter;
	}
	
	@Override
	public boolean canConvert(Class type) {
		return type.equals(HashSet.class)
				|| type.equals(ArrayList.class)
				|| type.equals(LinkedList.class);
	}
	
	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		@SuppressWarnings("rawtypes")
		final Collection collection = (Collection) source;
		final String classAttributeName = mapper.aliasForSystemAttribute("resolves-to");
		if (classAttributeName != null) {
			writer.addAttribute(classAttributeName, mapper.serializedClass(collection.getClass()));
		}
		writer.addAttribute("version", "2");
		writer.addAttribute("genericType", collection.getGenericType().getName());
		final Class<?> baseClass = baseClass(collection.getClass().getSimpleName());
		
		final Converter baseConverter = createCollectionConverter.create(mapper, baseClass);
		context.convertAnother(buildBaseCollection(baseClass, collection), baseConverter);
//		baseConverter.marshal(buildBaseCollection(baseClass, collection), writer, context);
		
		// TODO remove argument baseConverter
	}
	
	@SuppressWarnings({ "rawtypes" })
	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		final String version = reader.getAttribute("version");
//		final String clazz = reader.getAttribute("class") != null ?
//				reader.getAttribute("class") : reader.getNodeName();
		
		// TODO use HierarchicalStreams.readClassType
//		final Class<?> collectionClass = forName(HierarchicalStreams.readClassAttribute(reader, mapper));
//		final Class<?> collectionClass = forName(clazz);
		final Class<?> collectionClass = HierarchicalStreams.readClassType(reader, mapper);
		final Class<?> baseClass = baseClass(collectionClass.getSimpleName());
		final java.util.Collection baseCollection;
		final Class<?> genericType;
		
		if (version != null && version.equals("2")) {
			genericType = forName(reader.getAttribute("genericType"));
			if (genericType == null) {
				throw new ConversionException("Expected to find attribute 'genericType' in collection node!");
			}
//			reader.moveDown();
			
//			try {
//				final Converter baseConverter = new CollectionConverter(mapper, baseClass);
//				baseCollection = (java.util.Collection) baseConverter.unmarshal(reader, context);
//				return buildGmmCollection(collectionClass, baseCollection, genericType);
				// TODO use context.convertAnother
				baseCollection = (java.util.Collection) context.convertAnother(newInstance(baseClass), baseClass);
				return buildGmmCollection(collectionClass, baseCollection, genericType);
				
//			} finally {
//				reader.moveUp();
//			}
		} else {
			// TODO use convert another
			final Converter converter = new SerializableConverter(mapper, reflectionProvider);
			return converter.unmarshal(reader, context);
		}
	}
	
	@SuppressWarnings("rawtypes")
	private java.util.Collection newInstance(Class<?> baseClass) {
		try {
			return (java.util.Collection) baseClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ConversionException(e);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private java.util.Collection buildBaseCollection(Class<?> baseClass, Collection gmmCollection) {
		try {
			final java.util.Collection result = (java.util.Collection) baseClass.newInstance();
			result.addAll(gmmCollection);
			return result;
		} catch (SecurityException | InstantiationException | IllegalAccessException e) {
			throw new ConversionException(e);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Collection buildGmmCollection(Class<?> clazz, java.util.Collection baseCollection, Class<?> genericType) {
		try {
			final Constructor constructor = clazz.getConstructor(Class.class);
			final Collection result = (Collection) constructor.newInstance(genericType);
			result.addAll(baseCollection);
			return result;
		} catch (NoSuchMethodException | SecurityException | InstantiationException
				| IllegalAccessException | InvocationTargetException e) {
			throw new ConversionException(e);
		}
	}
	
	private Class<?> forName(String qualifiedName) {
		try {
			return Class.forName(qualifiedName);
		} catch (final ClassNotFoundException e) {
			throw new ConversionException(e);
		}
	}
	
	private Class<?> baseClass(String simpleName) {
		try {
			return Class.forName("java.util." + simpleName);
		} catch (final ClassNotFoundException e) {
			throw new ConversionException(e);
		}
	}
}
