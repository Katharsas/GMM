package gmm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.collections.List;
import gmm.domain.Linkable;
import gmm.domain.Notification;
import gmm.domain.TaskNotification;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.data.DataAccess;
import gmm.service.data.DataChangeEvent;
import gmm.service.data.DataChangeType;
import gmm.service.users.UserService;
import gmm.web.WebSocketEventSender;
import gmm.web.WebSocketEventSender.WebSocketEvent;

@Service
public class NotificationService {

	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final static int MAX_NOTIFICATIONS = 1000;
	
	private final UserService userService;
	private final WebSocketEventSender eventSender;
	
	@Autowired
	public NotificationService(DataAccess data, UserService userService, WebSocketEventSender eventSender) {
		this.userService = userService;
		this.eventSender = eventSender;
		data.registerForUpdates(this::onEvent, Linkable.class);
	}
	
	private void onEvent(DataChangeEvent<? extends Linkable> event) {
		if (event.source.isNormalUser() || event.source == User.UNKNOWN) {
			if (Task.class.isAssignableFrom(event.changed.getGenericType())) {
				for (final Linkable linkable : event.changed) {
					final Task task = (Task) linkable;
					for (final User user : userService.get()) {
						final TaskNotification notification = new TaskNotification(task, event.type, event.source);
						synchronized (user) {
							if (!user.equals(event.source)) {
								addNotification(notification, user.getNewNotifications());
								eventSender.unicastEvent(user, WebSocketEvent.NotificationChangeEvent);
							}
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
	
	public List<Notification> getNew(User user) {
		synchronized (user) {
			return user.getNewNotifications().copy();
		}
	}
	
	public List<Notification> getOld(User user) {
		synchronized (user) {
			return user.getOldNotifications().copy();
		}
	}
	
	public void moveNewToOld(User user) {
		synchronized (user) {
			user.getOldNotifications().addAll(user.getNewNotifications());
			user.getNewNotifications().clear();
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
