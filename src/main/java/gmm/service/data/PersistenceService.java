package gmm.service.data;

import java.nio.file.Path;

import gmm.collections.Collection;

/**
 * Uses serialization and deserialization of objects for persistence.
 * 
 * @author Jan Mothes
 */
public interface PersistenceService {

	/**
	 * Serialize and persist an {@link Object} or a {@link Collection} of objects.
	 * @param object - Object or collection to be persisted.
	 * @param path - Path of the file to create and save serialized data into.
	 */
	public void serialize(Object object, Path path);
	
	/**
	 * Deserialize object of type T from a file.
	 * @param path - Path of the file that contains the serialized data.
	 * @param clazz - Type of the object that was serialized into the file.
	 */
	public <T> T deserialize(Path path, Class<T> clazz);

	/**
	 * Deserialize a collection of objects of type T from a file.
	 * Similar to {@link #deserialize(Path, Class)}, but returns type-safe parameterized collection.
	 * Does not check if the collection actually only contains objects of this type!
	 * @param path - Path of the file that contains the serialized data.
	 * @param clazz - Generic type of the collection.
	 */
	public default <T> Collection<T> deserializeAll(Path path, Class<T> clazz) {
		return deserialize(path, Collection.getClassGeneric(clazz));
	}
}
