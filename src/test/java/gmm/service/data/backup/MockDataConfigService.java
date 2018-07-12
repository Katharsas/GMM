package gmm.service.data.backup;

import java.nio.file.Paths;

import org.springframework.mock.web.MockServletContext;

import gmm.service.FileService;
import gmm.service.data.PathConfig;

public class MockDataConfigService extends PathConfig {
	
	public MockDataConfigService() {
		super(new FileService(), new MockServletContext());
		updateWorkspace(Paths.get("WEB-INF/TestWorkspace"));
	}
}
