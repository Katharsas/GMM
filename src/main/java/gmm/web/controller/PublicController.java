package gmm.web.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.service.data.DataChangeEvent.ClientDataChangeEvent;
import gmm.web.ControllerArgs;
import gmm.web.FtlRenderer;
import gmm.web.FtlRenderer.TaskRenderResult;
import gmm.web.FtlTemplateService;
import gmm.web.controller.TaskController.TaskDataResult;
import gmm.web.sessions.LinkSession;
import gmm.web.sessions.tasklist.TaskListEvent;

@RequestMapping("public")
@Controller
public class PublicController {
	
	private final LinkSession session;
	private final FtlRenderer ftlRenderer;
	
	@Autowired
	public PublicController(LinkSession session, FtlRenderer ftlRenderer,
			FtlTemplateService templates) {
		
		this.session = session;
		this.ftlRenderer = ftlRenderer;
	}
	
	/**
	 * Serves task data to client ajax code.
	 */
	@RequestMapping(value = "/linkedTasks/renderTaskData", method = RequestMethod.POST)
	@ResponseBody
	public List<TaskDataResult> renderTasks(
			@RequestParam(value="idLinks[]", required=false) java.util.List<String> idLinks,
			ModelMap model, 
			HttpServletRequest request,
			HttpServletResponse response) {
		
		if(idLinks == null) return new ArrayList<>(TaskDataResult.class, 0);
		final List<Task> tasks = new LinkedList<>(Task.class);
		for(final Task task : session.getLinkedTasks()) {
			final boolean contains = idLinks.remove(task.getIdLink());
			if (contains) tasks.add(task);
		}
		final ControllerArgs requestData = new ControllerArgs(model, request, response);
		final Map<Task, TaskRenderResult> renders = ftlRenderer.renderTasks(tasks, requestData);
		
		final List<TaskDataResult> results = new ArrayList<>(TaskDataResult.class, idLinks.size());
		for (final Entry<Task, TaskRenderResult> entry : renders.entrySet()) {
			final TaskDataResult data = new TaskDataResult();
			final Task task = entry.getKey();
			data.idLink = task.getIdLink();
			data.render = entry.getValue();
			results.add(data);
		}
		return results;
	}
	
	/**
	 * Get data change events.
	 */
	@RequestMapping(value = "/linkedTasks/taskDataEvents", method = GET)
	@ResponseBody
	public List<ClientDataChangeEvent> syncTaskData() {
		return session.retrieveTaskDataEvents();
	}
	
	/**
	 * Get list of the ids of the linked tasks.
	 */
	@RequestMapping(value = "/linkedTasks/taskListEvents", method = RequestMethod.GET)
	@ResponseBody
	public List<TaskListEvent> syncTaskListState() {
		return session.retrieveEvents();
	}
	
	/**
	 * Stores ids, returns page. Client ajax will then request tasks from {@link #renderTasks(List, ModelMap, HttpServletRequest, HttpServletResponse)}
	 */
	@RequestMapping(value = "/link/{ids}/{key}", method = RequestMethod.GET)
	public String showTasks(ModelMap model, 
			@PathVariable String ids,
			@PathVariable String key) {
		session.setTaskLinks(ids, key);
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
