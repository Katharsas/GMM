package gmm.service.data.backup;

import java.nio.file.Paths;

import org.springframework.mock.web.MockServletContext;

import gmm.service.FileService;
import gmm.service.data.DataConfigService;

public class MockDataConfigService extends DataConfigService {
	
	public MockDataConfigService() {
		super(new FileService(), new MockServletContext());
		updateWorkspace(Paths.get("WEB-INF/TestWorkspace"));
	}
}
