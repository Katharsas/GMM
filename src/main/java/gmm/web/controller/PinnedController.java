package gmm.web.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.collections.List;
import gmm.domain.UniqueObject;
import gmm.domain.task.Task;
import gmm.service.data.DataAccess;
import gmm.service.users.CurrentUser;
import gmm.web.sessions.tasklist.PinnedSession;
import gmm.web.sessions.tasklist.TaskListEvent;

@RequestMapping(value = "tasks/pinned")
@ResponseBody
@PreAuthorize("hasRole('ROLE_USER')")
@Controller
public class PinnedController {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final PinnedSession pinned;
	private final DataAccess data;
	private final CurrentUser user;
	
	@Autowired
	public PinnedController(PinnedSession pinned, DataAccess data, CurrentUser user) {
		this.pinned = pinned;
		this.data = data;
		this.user = user;
	}

	/**
	 * Pinned Tasks
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value = "pin", method = POST)
	@ResponseBody
	public void pin(
			@RequestParam("idLink") String idLink) {
		final Task task = UniqueObject.getFromIdLink(data.getList(Task.class), idLink);
		pinned.pin(task);
	}
	
	@RequestMapping(value = "unpin", method = POST)
	@ResponseBody
	public void unpin(
			@RequestParam("idLink") String idLink) {
		final Task task = UniqueObject.getFromIdLink(data.getList(Task.class), idLink);
		pinned.unpin(task);
	}
	
	@RequestMapping(value = "taskListEvents", method = GET)
	@ResponseBody
	public List<TaskListEvent> taskListEvents() {
		final List<TaskListEvent> events = pinned.retrieveEvents();
		if (logger.isDebugEnabled()) {
			logger.debug(user.get() + " retrieved events: " + Arrays.toString(events.toArray()));
		}
		return events;
	}
	
	@RequestMapping(value = "init", method = POST)
	public void provideInit() {
		pinned.createInitEvent();
	}
}
