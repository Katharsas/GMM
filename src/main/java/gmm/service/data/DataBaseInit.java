package gmm.service.data;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.UniqueObject;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.ajax.AutoResponseBundleHandler;
import gmm.service.ajax.ConflictAnswer;
import gmm.service.data.backup.BackupService;
import gmm.service.data.backup.TaskBackupLoader;
import gmm.service.data.xstream.XMLService;
import gmm.service.users.UserService;

@Service
public class DataBaseInit implements ApplicationListener<ContextRefreshedEvent>{

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired private XMLService xmlService;
	@Autowired private BackupService backups;
	@Autowired private TaskBackupLoader backupLoader;
	
	@Autowired private PasswordEncoder encoder;
	@Autowired private UserService users;
	@Autowired private CombinedData combined;
	
	private boolean initialized = false;
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (!initialized) {
			initUsers();
			initTasks();
			initCombinedData();
			initialized = true;
		}
	}
	
	@Value("${autoload.tasks}")
	private boolean autoloadTasks;
	
	protected void initTasks() {
		if (autoloadTasks) {
			final Path latestBackup = backups.getLatestTaskBackup();
			if (latestBackup != null) {
				loadTasks(latestBackup);
				logger.info("Autoloaded latest task backup file.");
			} else {
				logger.info("Mising backup files caused tasks not to be autoloaded.");
			}
		} else {
			logger.info("Configuration caused tasks to not be autoloaded.");
		}
	}
	
	private void loadTasks(Path latestBackup) {
		final Collection<Task> tasks = xmlService.deserializeAll(latestBackup, Task.class);
		backupLoader.prepareLoadTasks(tasks);
		
		final AutoResponseBundleHandler<Task> autoLoader = new AutoResponseBundleHandler<>();
		
		autoLoader.processResponses(backupLoader.getBundledMessageResponses(),  (conflict) -> {
			if (conflict.getStatus().equals("folderConflict")) {
				logger.info("Aquiring data on automatic task import caused by: " + conflict);
				return new ConflictAnswer("aquireData", true);
			} else {
				logger.error("Conflict occured on automatic task import: " + conflict);
				return new ConflictAnswer("skip", true);
			}
		});
		autoLoader.processResponses(backupLoader.getBundledMessageResponses(), (conflict) -> {
			logger.error("Conflict occured on automatic task import: " + conflict);
			return new ConflictAnswer("skip", true);
		});
	}
	
	@Value("${autoload.users}")
	private boolean autoloadUsers;
	
	@Value("${default.user}")
	private boolean createDefaultUser;
	
	@Value("${default.username}")
	private String defaultUserName;
	
	@Value("${default.password}")
	private String defaultUserPW;
	
	protected void initUsers() {
		if(autoloadUsers) {
			final Path latestBackup = backups.getLatestUserBackup();
			if (latestBackup != null) {
				final Collection<User> users = xmlService.deserializeAll(latestBackup, User.class);
				for (User user : users) {
					UniqueObject.updateCounter(user);
				}
				this.users.addAll(users);
				logger.info("Autoloaded latest user backup file.");
			} else {
				logger.info("Mising backup files caused users not to be autoloaded.");
			}
		} else {
			logger.info("Configuration caused users not to be autoloaded.");
		}
		if (createDefaultUser && users.get().size() == 0) {
			addDefaultUser();
		} else {
			logger.info("Configuration caused no default user to be created.");
		}
	}
	
	private void addDefaultUser() {
		final User defaultUser = new User(defaultUserName);
		defaultUser.setPasswordHash(encoder.encode(defaultUserPW));
		defaultUser.setRole(User.ROLE_ADMIN);
		defaultUser.enable(true);
		users.add(defaultUser);
		
		logger.info("\n"
				+	"##########################################################" + "\n\n"
				+	"  Created default user: " + "\n"
				+	"  Username: " + defaultUser.getName() + "\n"
				+	"  Password: " + defaultUserPW + "\n\n"
				+	"##########################################################");
	}
	
	protected void initCombinedData() {
		final Path latestBackup = backups.getLatestCombinedDataBackup();
		if (latestBackup != null) {
			CombinedData combinedBackup = xmlService.deserialize(latestBackup, CombinedData.class);
			combined.setCustomAdminBannerActive(combinedBackup.isCustomAdminBannerActive());
			combined.setCustomAdminBanner(combinedBackup.getCustomAdminBanner());
		}
	}
}
