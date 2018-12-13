package gmm.service.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.domain.UniqueObject;
import gmm.domain.User;
import gmm.domain.User.UserNameOccupiedException;
import gmm.domain.task.Task;
import gmm.service.ajax.AutoResponseBundleHandler;
import gmm.service.ajax.ConflictAnswer;
import gmm.service.data.backup.BackupAccessService;
import gmm.service.data.backup.TaskBackupLoader;
import gmm.service.users.UserService;

@Service
public class DataBaseInit implements ApplicationListener<ContextRefreshedEvent> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final BackupAccessService backups;
	private final TaskBackupLoader backupLoader;
	private final DataAccess data;
	private final UserService userService;
	private final DataBaseInitNotifier notifier;
	private final Config config;
	
	private boolean initStarted = false;
	
	@Autowired
	public DataBaseInit(BackupAccessService backups, TaskBackupLoader backupLoader,
			DataAccess data, UserService userService, DataBaseInitNotifier notifier,
			Config config) {
		this.backups = backups;
		this.backupLoader = backupLoader;
		this.data = data;
		this.userService = userService;
		this.notifier = notifier;
		this.config = config;
	}
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (!initStarted) {
			new Thread(() -> {
				try {
					initUsers();
					initTasks();
					initCombinedData(data.getCombinedData());
					notifier.setInitDone();
				} catch (final Exception e) {
					logger.error(e.getMessage(), e);
				}
			}).start();
		}
		initStarted = true;
	}
	
	private void initTasks() {
		if (config.autoloadTasks()) {
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
	
	private void initUsers() {
		if(config.autoloadUsers()) {
			final Collection<User> users = backups.getLatestUserBackup();
			if (users != null) {
				final Collection<User> validated = users.copy();
				for (final User user : users) {
					UniqueObject.updateCounter(user);
					if (!userService.isFreeUserName(user.getName())) {
						logger.error("Could not load user '" + user + "'!", new UserNameOccupiedException(user.getName()));
						validated.remove(user);
					}
				}
				data.addAll(validated);
				logger.info("Autoloaded latest user backup file.");
			} else {
				logger.info("Mising backup files caused users not to be autoloaded.");
			}
		} else {
			logger.info("Configuration caused users not to be autoloaded.");
		}
		if (config.createDefaultUser() && data.getList(User.class).size() == 0) {
			data.add(createDefaultUser());
		} else {
			logger.info("\n"
					+	"##########################################################" + "\n\n"
					+	"  Did not create a default user. " + "\n"
					+	"  (Enable default.user & disable autoload to change) " + "\n\n"
					+	"##########################################################");
		}
	}
	
	private User createDefaultUser() {
		final String defaultUserName = config.getDefaultUserName();
		final String defaultUserPW = config.getDefaultUserPW();
		if (!userService.isFreeUserName(defaultUserName)) {
			throw new UserNameOccupiedException(defaultUserName);
		}
		final User defaultUser = new User(defaultUserName);
		defaultUser.setPasswordHash(userService.encodePassword(defaultUserPW));
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
	
	private void initCombinedData(CombinedData target) {
		final CombinedData combined = backups.getLatestCombinedDataBackup();
		if (combined != null) {
			target.setCustomAdminBanner(combined.getCustomAdminBanner());
			target.setCustomAdminBannerActive(combined.isCustomAdminBannerActive());
			logger.info("Autoloaded latest combined data.");
		} else {
			logger.info("Mising backup files caused combined data not to be autoloaded.");
		}
	}
}
