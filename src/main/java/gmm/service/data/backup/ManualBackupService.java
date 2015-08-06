package gmm.service.data.backup;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.task.Task;
import gmm.service.FileService;
import gmm.service.data.DataConfigService;
import gmm.service.data.XMLService;

@Service
public class ManualBackupService {
	
	@Autowired private DataConfigService config;
	@Autowired private XMLService xmlService;
	@Autowired private FileService fileService;
	
	public void saveTasksToXml(Collection<? extends Task> tasks, String pathString) throws IOException {
		Path visible = config.TASKS;
		Path path = visible.resolve(fileService.restrictAccess(Paths.get(pathString+".xml"), visible));
		fileService.prepareFileCreation(path);
		xmlService.serialize(tasks, path);
	}
}
