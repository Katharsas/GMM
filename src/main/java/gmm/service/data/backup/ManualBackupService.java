package gmm.service.data.backup;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.task.Task;
import gmm.service.FileService;
import gmm.service.data.DataConfigService;
import gmm.service.data.xstream.XMLService;

@Service
public class ManualBackupService {
	
	@Autowired private DataConfigService config;
	@Autowired private XMLService xmlService;
	@Autowired private FileService fileService;
	
	public void saveTasksToXml(Collection<? extends Task> tasks, String pathString) {
		final Path visible = config.TASKS;
		final Path path = visible.resolve(fileService.restrictAccess(Paths.get(pathString+".xml"), visible));
		xmlService.serialize(tasks, path);
	}
}
