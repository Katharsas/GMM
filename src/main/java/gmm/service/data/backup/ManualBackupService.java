package gmm.service.data.backup;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.task.Task;
import gmm.service.FileService;
import gmm.service.data.PathConfig;
import gmm.service.data.xstream.XMLService;

@Service
public class ManualBackupService {
	
	private final FileService fileService;
	private final PathConfig config;
	private final XMLService xmlService;
	
	@Autowired
	public ManualBackupService(
			FileService fileService, PathConfig config, XMLService xmlService) {
		this.fileService = fileService;
		this.config = config;
		this.xmlService = xmlService;
	}
	
	public void saveTasksToXml(Collection<? extends Task> tasks, String pathString) {
		final Path visible = config.dbTasks();
		final Path path = visible.resolve(fileService.restrictAccess(Paths.get(pathString+".xml"), visible));
		xmlService.serialize(tasks, path);
	}
}
