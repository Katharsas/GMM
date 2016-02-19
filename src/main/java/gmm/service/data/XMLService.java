package gmm.service.data;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.Comment;
import gmm.domain.User;
import gmm.domain.task.GeneralTask;
import gmm.domain.task.Task;
import gmm.domain.task.asset.Asset;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.Model;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.Texture;
import gmm.domain.task.asset.TextureTask;
import gmm.service.FileService;
import gmm.service.converters.PathConverter;

import com.thoughtworks.xstream.XStream;


@Service
public class XMLService {
	
	@Autowired DataConfigService dataConfig;
	@Autowired FileService fileService;
	
	final private XStream xstream;
	
	public XMLService() {
		xstream = new XStream();
		xstream.autodetectAnnotations(true);
		xstream.setMode(XStream.NO_REFERENCES);
		
		Class<?>[] clazzes = {
							Task.class,
							GeneralTask.class,
							AssetTask.class,
							ModelTask.class,
							TextureTask.class,
							User.class,
							Comment.class,
							Asset.class,
							Texture.class,
							Model.class};
		for (Class<?> c : clazzes) {
			xstream.alias(c.getSimpleName(), c);
		}
		xstream.registerConverter(new PathConverter());
	}
	
	public synchronized void serialize(Collection<?> objects, Path path) throws IOException {
		String xml= xstream.toXML(objects);
		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + xml;
		byte[] bytes = xml.getBytes("UTF-8");
    	fileService.createFile(path, bytes);
	}

	@SuppressWarnings("unchecked")
	public synchronized <T> Collection<T> deserialize(Path path, Class<T> clazz) throws IOException {
		if(!path.toFile().exists()) {
			throw new IOException("File with serialized data at \""+path+"\" does not exist! Cannot deserialize.");
		}
		return (Collection<T>) xstream.fromXML(path.toFile());
	}
}
