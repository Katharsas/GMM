package gmm.service.data.backup;

import java.nio.file.Paths;

import org.springframework.mock.web.MockServletContext;

import gmm.service.FileService;
import gmm.service.data.PathConfig;

public class MockDataConfigService extends PathConfig {
	
	public MockDataConfigService() {
		super(new FileService(), new MockServletContext());
		assetPreviews = Paths.get("previews");
		assetPreviews = Paths.get("assetsNew");
		assetsOriginal = Paths.get("assetsOriginal");
		dbTasks = Paths.get("db/tasks");
		dbUsers = Paths.get("db/users");
		dbOther = Paths.get("db/other");
		updateWorkspace(Paths.get("WEB-INF/dataTesting"));
		
	}
}
