package gmm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.List;
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
	
	private final static int MAX_NOTIFICATIONS = 1000;
	
	private final UserService userService;
	
	@Autowired
	public NotificationService(DataAccess data, UserService userService) {
		this.userService = userService;
		data.registerForUpdates(this);
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
						final TaskNotification notification = new TaskNotification(message, idLink);
						final List<Notification> target;
						if (!user.equals(event.source)) {
							target = user.getNewNotifications();
						} else {
							target = user.getOldNotifications();
						}
						synchronized (user) {
							addNotification(notification, target);
						}
					}
				}
			}
		}
		if (User.class.isAssignableFrom(event.changed.getGenericType())) {
			if (event.type == DataChangeType.ADDED && event.isSingleItem && event.source != User.SYSTEM) {
				final User newUser = (User) event.changed.iterator().next();
				final Notification welcome = new Notification("Welcome to the GMM! Any questions to 'Kellendil' from 'forum.worldofplayers.de'.");
				synchronized (newUser) {
					newUser.getNewNotifications().add(welcome);
				}
			}
		}
	}
	
	public List<Notification> getNewAndMoveToOld(User user) {
		List<Notification> result;
		synchronized (user) {
			result = user.getNewNotifications().copy();
			for (final Notification item : result) {
				addNotification(item, user.getOldNotifications());
			}
			user.getNewNotifications().clear();
		}
		return result;
	}
	
	public List<Notification> getOld(User user) {
		synchronized (user) {
			return user.getOldNotifications().copy();
		}
	}
	
	public void clearOld(User user) {
		synchronized (user) {
			user.getOldNotifications().clear();
		}
	}
	
	private void addNotification(Notification item, List<Notification> target) {
		target.add(item);
		if (target.size() > MAX_NOTIFICATIONS) {
			target.remove(0);
		}
	}
}
