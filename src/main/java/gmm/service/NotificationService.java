package gmm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.Notification;
import gmm.domain.TaskNotification;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.data.DataAccess;
import gmm.service.data.DataAccess.DataChangeCallback;
import gmm.service.data.DataChangeEvent;
import gmm.service.data.DataChangeType;
import gmm.service.users.UserService;

@Service
public class NotificationService implements DataChangeCallback {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private UserService userService;
	
	@Autowired
	public NotificationService(DataAccess data, UserService userService) {
		this.userService = userService;
//		data.registerForUpdates(this);// TODO enable
	}
	
	@Override
	public void onEvent(DataChangeEvent event) {
		if (event.source.isNormalUser() || event.source == User.UNKNOWN) {
			if (Task.class.isAssignableFrom(event.changed.getGenericType())) {
				for (final Task task : event.getChanged(Task.class)) {
					final String message = "Task '" + task.getName() + "' was '" + event.type.name()+ "' by '" + event.source.getName() + "'.";
					final String idLink = event.type == DataChangeType.REMOVED ? null : task.getIdLink();
					logger.debug(message);
					
					for (final User user : userService.get()) {
						if (!user.equals(event.source)) {
							final TaskNotification notification = new TaskNotification(message, idLink);
							user.getNewNotifications().add(notification);
						}
					}
				}
			}
		}
		if (User.class.isAssignableFrom(event.changed.getGenericType())) {
			if (event.isSingleItem && event.source != User.SYSTEM) {
				final User newUser = (User) event.changed.iterator().next();
				final Notification welcome = new Notification("Welcome to the GMM! Any questions to 'Kellendil' from 'forum.worldofplayers.de'.");
				newUser.getNewNotifications().add(welcome);
			}
		}
	}
}
