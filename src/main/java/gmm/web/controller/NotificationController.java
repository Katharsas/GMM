package gmm.web.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.collections.List;
import gmm.domain.Notification;
import gmm.service.NotificationService;
import gmm.service.users.UserService;

/**
 * Not the most thread-safe/robust design, but should to the job.
 * @author Jan Mothes
 */
@RequestMapping(value={"notifics"})
@PreAuthorize("hasRole('ROLE_USER')")
@Controller
public class NotificationController {

	@Autowired UserService users;
	@Autowired NotificationService notifics;
	
	@RequestMapping(value="/has", method = GET)
	@ResponseBody
	public int hasNewNotifications() {
		return users.getLoggedInUser().getNewNotifications().size();
	}
	
	/**
	 * Side-effect: New notifications become old notifications since the client hopefully presented them.
	 */
	@RequestMapping(value="/new", method = POST)
	@ResponseBody
	public List<Notification> getNewNotifications() {
		return notifics.getNewAndMoveToOld(users.getLoggedInUser());
	}
	
	@RequestMapping(value="/old", method = POST)
	@ResponseBody
	public List<Notification> getOldNotifications() {
		return notifics.getOld(users.getLoggedInUser());
	}
	
	@RequestMapping(value="/clear", method = POST)
	@ResponseBody
	public void deleteOldNotifications() {
		notifics.clearOld(users.getLoggedInUser());
	}
}
