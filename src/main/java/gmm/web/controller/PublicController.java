package gmm.web.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.collections.ArrayList;
import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.service.data.DataAccess;
import gmm.service.data.DataChangeEvent.ClientDataChangeEvent;
import gmm.web.ControllerArgs;
import gmm.web.ControllerSettings;
import gmm.web.sessions.BasicSession;
import gmm.web.sessions.BasicSession.TaskDataResult;
import gmm.web.sessions.LinkSession;
import gmm.web.sessions.tasklist.TaskListEvent;

@RequestMapping("public")
@Controller
public class PublicController {
	
	private final BasicSession session;
	private final LinkSession linkSession;
	private final DataAccess data;
	
	@Autowired
	public PublicController(BasicSession session, LinkSession linkSession, DataAccess data) {
		this.session = session;
		this.linkSession = linkSession;
		this.data = data;
	}
	
	/**
	 * Serves task data to client ajax code.
	 */
	@PreAuthorize("hasRole('ROLE_USER')")
	@RequestMapping(value = "/linkedTasks/renderTaskDataAny", method = RequestMethod.POST)
	@ResponseBody
	public List<TaskDataResult> renderTasksAny(
			@RequestParam(value="idLinks[]", required=false) java.util.List<String> idLinks,
			ModelMap model, 
			HttpServletRequest request,
			HttpServletResponse response) {
		
		if(idLinks == null) {
			return new ArrayList<>(TaskDataResult.class, 0);
		} else {
			ControllerArgs args = new ControllerArgs(model, request, response);
			return session.renderTasks(idLinks, data.getList(Task.class), args);
		}
	}
	
	/**
	 * Serves task data to client ajax code.
	 */
	@RequestMapping(value = "/linkedTasks/renderTaskData", method = RequestMethod.POST)
	@ResponseBody
	public List<TaskDataResult> renderTasksLinked(
			@RequestParam(value="idLinks[]", required=false) java.util.List<String> idLinks,
			ModelMap model, 
			HttpServletRequest request,
			HttpServletResponse response) {
		
		if(idLinks == null) {
			return new ArrayList<>(TaskDataResult.class, 0);
		} else {
			ControllerArgs args = new ControllerArgs(model, request, response);
			return session.renderTasks(idLinks, linkSession.getLinkedTasks(), args);
		}
	}
	
	/**
	 * Get data change events.
	 */
	@RequestMapping(value = "/linkedTasks/taskDataEvents", method = GET)
	@ResponseBody
	public List<ClientDataChangeEvent> syncTaskData() {
		return linkSession.retrieveTaskDataEvents();
	}
	
	/**
	 * Get list of the ids of the linked tasks.
	 */
	@RequestMapping(value = "/linkedTasks/taskListEvents", method = RequestMethod.GET)
	@ResponseBody
	public List<TaskListEvent> syncTaskListState() {
		return linkSession.retrieveEvents();
	}
	
	/**
	 * Stores ids, returns page. Client ajax will then request tasks from {@link #renderTasks(List, ModelMap, HttpServletRequest, HttpServletResponse)}
	 */
	@RequestMapping(value = "/link/{ids}/{key}", method = RequestMethod.GET)
	public String showTasks(ModelMap model,
			@PathVariable String ids,
			@PathVariable String key) {
		try {
			linkSession.initTaskLinks(ids, key);
		} catch (IllegalArgumentException e) {
			throw new ControllerSettings.NotFoundException();
		}
		return "links";
	}
	
	/**
	 * Needed so you can link to the same page but with Login interdiction.
	 */
	@PreAuthorize("hasRole('ROLE_USER')")
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String login(ModelMap model) {
		return "links";
	}
}
