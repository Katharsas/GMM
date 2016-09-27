package gmm.service.data;

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
import gmm.service.data.backup.BackupAccessService;
import gmm.service.data.backup.TaskBackupLoader;

@Service
public class DataBaseInit implements ApplicationListener<ContextRefreshedEvent> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final BackupAccessService backups;
	private final TaskBackupLoader backupLoader;
	private final PasswordEncoder encoder;
	private final DataAccess data;
	
	private boolean initialized = false;
	
	@Autowired
	public DataBaseInit(BackupAccessService backups, TaskBackupLoader backupLoader,
			PasswordEncoder encoder, DataAccess data) {
		this.backups = backups;
		this.backupLoader = backupLoader;
		this.encoder = encoder;
		this.data = data;
	}
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (!initialized) {
			initUsers();
			initTasks();
			initCombinedData(data.getCombinedData());
		}
		initialized = true;
	}
	
	@Value("${autoload.tasks}")
	private boolean autoloadTasks;
	
	protected void initTasks() {
		if (autoloadTasks) {
			final Collection<Task> tasks = backups.getLatestTaskBackup();
			if (tasks != null) {
				loadTasks(tasks);
				logger.info("Autoloaded latest task backup file.");
			} else {
				logger.info("Mising backup files caused tasks not to be autoloaded.");
			}
		} else {
			logger.info("Configuration caused tasks to not be autoloaded.");
		}
	}
	
	private void loadTasks(Collection<Task> tasks) {
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
			final Collection<User> users = backups.getLatestUserBackup();
			if (users != null) {
				for (final User user : users) {
					UniqueObject.updateCounter(user);
				}
				data.addAll(users);
				logger.info("Autoloaded latest user backup file.");
			} else {
				logger.info("Mising backup files caused users not to be autoloaded.");
			}
		} else {
			logger.info("Configuration caused users not to be autoloaded.");
		}
		if (createDefaultUser && data.getList(User.class).size() == 0) {
			data.add(createDefaultUser());
		} else {
			logger.info("Configuration caused no default user to be created.");
		}
	}
	
	private User createDefaultUser() {
		final User defaultUser = new User(defaultUserName);
		defaultUser.setPasswordHash(encoder.encode(defaultUserPW));
		defaultUser.setRole(User.ROLE_ADMIN);
		defaultUser.enable(true);
		logger.info("\n"
				+	"##########################################################" + "\n\n"
				+	"  Created default user: " + "\n"
				+	"  Username: " + defaultUser.getName() + "\n"
				+	"  Password: " + defaultUserPW + "\n\n"
				+	"##########################################################");
		return defaultUser;
	}
	
	protected void initCombinedData(CombinedData target) {
		final CombinedData combined = backups.getLatestCombinedDataBackup();
		if (combined != null) {
			target.setCustomAdminBanner(combined.getCustomAdminBanner());
			target.setCustomAdminBannerActive(combined.isCustomAdminBannerActive());
		}
	}
}
