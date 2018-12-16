package gmm.service.data.xstream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;

import gmm.collections.Collection;
import gmm.service.FileService;
import gmm.service.data.PersistenceService;
import gmm.service.users.UserProvider;

@Service
public class XMLService implements PersistenceService {

	final private FileService fileService;
	final private XStream xstream;

	final private String xmlEncoding = "UTF-8";
	final private String xmlEncodingHeader = "<?xml version=\"1.0\" encoding=\"" + xmlEncoding + "\" ?>\n";

	@Autowired
	public XMLService(FileService fileService, UserProvider getUsers) {

		this.fileService = fileService;
		xstream = new XStream(new PureJavaReflectionProvider());
		xstream.setMode(XStream.NO_REFERENCES);
		// classes
		final Class<?>[] supportedClasses = {
				// Tasks
				gmm.domain.task.Task.class,
				gmm.domain.task.GeneralTask.class,
				gmm.domain.task.asset.AssetTask.class,
				gmm.domain.task.asset.ModelTask.class,
				gmm.domain.task.asset.TextureTask.class,
				gmm.domain.task.asset.AssetProperties.class,
				gmm.domain.task.asset.TextureProperties.class,
				gmm.domain.task.asset.ModelProperties.class,
				// asset info
				gmm.service.assets.AssetInfo.class,
				gmm.service.assets.OriginalAssetFileInfo.class,
				gmm.service.assets.NewAssetFolderInfo.class,
				// Other
				gmm.domain.Comment.class,
				gmm.domain.User.class,
				gmm.service.data.CombinedData.class };
		// aliases
		xstream.processAnnotations(supportedClasses);
		for (final Class<?> clazz : supportedClasses) {
			xstream.alias(clazz.getSimpleName(), clazz);
		}
		xstream.registerConverter(new PathConverter());
		xstream.registerConverter(new AssetNameConverter());
//		xstream.registerConverter(new AssetNameObjectConverter());
		xstream.registerConverter(new GmmCollectionConverter(xstream.getMapper(), xstream.getReflectionProvider()));
		final UserReferenceConverter userConverter = new UserReferenceConverter(getUsers);
		final InstantConverter instantConverter = new InstantConverter();
		// the following fields will reference user by id:
		xstream.registerLocalConverter(gmm.domain.task.Task.class, "author", userConverter);
		xstream.registerLocalConverter(gmm.domain.task.Task.class, "assigned", userConverter);
		xstream.registerLocalConverter(gmm.domain.Comment.class, "author", userConverter);
		// compatibility for old backups
		xstream.registerLocalConverter(gmm.domain.UniqueObject.class, "created", instantConverter);
		xstream.registerLocalConverter(
				gmm.domain.task.asset.AssetTask.class, "newestAssetLastUpdate", instantConverter);
		xstream.omitField(gmm.domain.task.asset.TextureTask.class, "models");
		xstream.omitField(gmm.domain.task.asset.ModelTask.class, "textures");
//		final Converter textureNamesConverter = new GmmCollectionConverter(
//				xstream.getMapper(),
//				xstream.getReflectionProvider(),
//				(mapper, clazz) -> new TextureNamesConverter(mapper, clazz));
//		xstream.registerLocalConverter(gmm.domain.task.asset.ModelProperties.class, "textureNames", textureNamesConverter);
				
	}

	@Override
	public synchronized void serialize(Object object, Path path) {
		final String xml = xmlEncodingHeader + xstream.toXML(object);
		byte[] bytes;
		try {
			bytes = xml.getBytes(xmlEncoding);
		} catch (final UnsupportedEncodingException e) {
			throw new UncheckedIOException(e);
		}
		fileService.createFile(path, bytes);
	}

	@Override
	public synchronized <T> T deserialize(Path path, Class<T> clazz) {
		if (!path.toFile().exists()) {
			throw new UncheckedIOException(new IOException(
					"File with serialized data at \"" + path + "\" does not exist!" + " Cannot deserialize."));
		}
		@SuppressWarnings("unchecked")
		final T result = (T) xstream.fromXML(path.toFile());
		return result;
	}

	@Override
	public synchronized <T> Collection<T> deserializeAll(Path path, Class<T> clazz) {
		return deserialize(path, Collection.getClassGeneric(clazz));
	}
}
