package gmm.web.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.List;
import gmm.domain.Notification;
import gmm.domain.TaskNotification;
import gmm.domain.UniqueObject;
import gmm.domain.task.Task;
import gmm.service.NotificationService;
import gmm.service.data.DataAccess;
import gmm.service.users.CurrentUser;

/**
 * @author Jan Mothes
 */
@RequestMapping(value={"notifics"})
@PreAuthorize("hasRole('ROLE_USER')")
@ResponseBody
@Controller
public class NotificationController {

	public static class NotificationListResponse {
		public final List<Notification> notifications;
		public final Map<String, Boolean> idLinkToExists;
		public NotificationListResponse(List<Notification> notifications, Map<String, Boolean> idLinkToExists) {
			super();
			this.notifications = notifications;
			this.idLinkToExists = idLinkToExists;
		}
	}
	
	@Autowired CurrentUser user;
	@Autowired DataAccess data;
	@Autowired NotificationService notifics;
	
	
	private Map<String, Boolean> doTasksExist(List<String> idLinks) {
		final Map<String, Boolean> result = new HashMap<>();
		final Collection<Task> tasks = data.getList(Task.class);
		for (String idLink : idLinks) {
			boolean exists = UniqueObject.getFromIdLink(tasks, idLink) != null;
			result.put(idLink, exists);
		}
		return result;
	}
	
	private NotificationListResponse createListResponse(List<Notification> notifics) {
		List<String> idLinks = new ArrayList<>(String.class);
		for (Notification notific : notifics) {
			if (TaskNotification.class.isAssignableFrom(notific.getClass())) {
				TaskNotification taskNotific = (TaskNotification) notific;
				idLinks.add(taskNotific.getTaskIdLink());
			}
		}
		Map<String, Boolean> idLinkToExists = doTasksExist(idLinks);
		return new NotificationListResponse(notifics, idLinkToExists);
	}
	
	@RequestMapping(value="/has", method = GET)
	public int hasNewNotifications() {
		return user.get().getNewNotifications().size();
	}
	
	@RequestMapping(value="/new", method = POST)
	public NotificationListResponse getNewNotifications() {
		return createListResponse(notifics.getNew(user.get()));
	}
	
	@RequestMapping(value="/old", method = POST)
	public NotificationListResponse getOldNotifications() {
		return createListResponse(notifics.getOld(user.get()));
	}
	
	@RequestMapping(value="/markRead", method = POST)
	public void markNewNotificationsAsOld() {
		notifics.moveNewToOld(user.get());
	}
	
	@RequestMapping(value="/clearRead", method = POST)
	public void clearOldNotifications() {
		notifics.clearOld(user.get());
	}
}
