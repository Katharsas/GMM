package gmm.service.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Config {
	
	private final PathConfig pathConfig;
	
	@Value("${autoload.tasks}") private boolean autoloadTasks;
	@Value("${autoload.users}") private boolean autoloadUsers;
	
	@Value("${default.user}") private boolean createDefaultUser;
	@Value("${default.username}") private String defaultUserName;
	@Value("${default.password}") private String defaultUserPW;
	
	@Value("${accountcreation.token}") String accountCreationToken;
	
	@Value("${previews.threads}") private int previewThreadCount;
	
	@Autowired
	public Config(PathConfig pathConfig) {
		this.pathConfig = pathConfig;
	}
	
	public PathConfig getPathConfig() {
		return pathConfig;
	}

	public boolean autoloadTasks() {
		return autoloadTasks;
	}

	public boolean autoloadUsers() {
		return autoloadUsers;
	}

	public boolean createDefaultUser() {
		return createDefaultUser;
	}

	public String getDefaultUserName() {
		return defaultUserName;
	}

	public String getDefaultUserPW() {
		return defaultUserPW;
	}

	public String getAccountCreationToken() {
		return accountCreationToken;
	}

	public int getPreviewThreadCount() {
		return previewThreadCount;
	}
}
