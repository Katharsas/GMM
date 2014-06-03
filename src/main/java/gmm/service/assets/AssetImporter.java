package gmm.service.assets;

import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.util.HashSet;
import gmm.util.Set;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Does the import of Texture and Mesh Tasks form asset paths.
 * 
 * 
 * @author Jan
 */
@Service
public class AssetImporter {
	
	@Autowired
	TaskSession session;
	@Autowired
	DataConfigService config;
	@Autowired
	TextureService assetService;
	@Autowired
	FileService fileService;
	@Autowired
	DataAccess data;
	@Autowired
	TexturePreviewCreator creator;
	
	
	public void importTasks(Iterable<String> assetPaths, TaskForm formm, Class<?> taskClazz) throws IOException {
		Set<TextureTask> result = new HashSet<>();
		for(String path : assetPaths) {
			result.add(importTask(Paths.get(config.ASSETS_ORIGINAL).resolve(path), session.getUser()));
		}
		data.addAll(TextureTask.class, result);
	}
	
	public TextureTask importTask(Path path, User user) throws IOException {
		path = path.toAbsolutePath();
		
		TextureTask result;
		
		//get relative path and new folder path
		Path base = Paths.get(config.ASSETS_ORIGINAL).toAbsolutePath();
		Path relPath = base.relativize(path.toAbsolutePath());
		Path newFolder = Paths.get(config.ASSETS_NEW).resolve(relPath).toAbsolutePath();
		
		//create Task
		result = new TextureTask(""+path.getFileName(), user);
		result.setAssetFolderPaths(""+path, ""+newFolder);
		
		//create previews
		creator.createPreview(path, newFolder, true);
		return result;
	}
}
