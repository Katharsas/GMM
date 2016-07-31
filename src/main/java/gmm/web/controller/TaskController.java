package gmm.web.controller;


import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.Comment;
import gmm.domain.Label;
import gmm.domain.UniqueObject;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.ajax.ConflictAnswer;
import gmm.service.ajax.MessageResponse;
import gmm.service.data.DataAccess;
import gmm.service.users.CurrentUser;
import gmm.web.ControllerArgs;
import gmm.web.FtlRenderer;
import gmm.web.FtlRenderer.TaskRenderResult;
import gmm.web.FtlTemplateService;
import gmm.web.forms.CommentForm;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;
import gmm.web.sessions.tasklist.PinnedSession;
import gmm.web.sessions.tasklist.TaskListEvent;
import gmm.web.sessions.tasklist.WorkbenchSession;

/**
 * Main task page controller.<br>
 * This controller is responsible for most task-CRUD operations requested by "tasks" page:<br>
 * Creating, editing or deleting tasks or task comments. State for these operations is managed by
 * session object {@link #taskSession}.
 * 
 * @see {@link WorkbenchController}
 * 
 * @author Jan Mothes
 */
@RequestMapping(value={"tasks"})
@PreAuthorize("hasRole('ROLE_USER')")
@Controller
public class TaskController {
	
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final TaskSession taskSession;
	private final WorkbenchSession workbench;
	private final PinnedSession pinned;
	private final DataAccess data;
	private final FtlRenderer ftlRenderer;
	private final FtlTemplateService ftlTemplates;
	private final CurrentUser user;
	
	@Autowired
	public TaskController(TaskSession taskSession, WorkbenchSession workbench, PinnedSession pinned,
			DataAccess data, FtlRenderer ftlRenderer, CurrentUser user, FtlTemplateService templates) {
		this.taskSession = taskSession;
		this.workbench = workbench;
		this.pinned = pinned;
		this.data = data;
		this.ftlRenderer = ftlRenderer;
		this.ftlTemplates = templates;
		this.user = user;
		
		// taskForm template dependencies
		templates.registerVariable("users", ()->data.getList(User.class));
		templates.registerVariable("taskLabels", ()->data.getList(Label.class));
		templates.registerVariable("taskForm", taskSession::getTaskForm);
		templates.registerForm("taskForm", taskSession::getTaskForm);
		
		templates.registerFtl("all_taskForm", "users", "taskLabels", "taskForm");
	}
	
	/**
	 * Delete Task <br>
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the task which will be deleted
	 */
	@RequestMapping(value="/deleteTask/{idLink}", method = POST)
	@ResponseBody
	public void handleTasksDelete(@PathVariable String idLink) {
		data.remove(UniqueObject.getFromIdLink(workbench.getTasks(), idLink));
	}
	
	
	/**
	 * Edit Comment <br>
	 * @param taskIdLink - identifies the task which contains the comment
	 * @param commentIdLink - identifies the comment to be edited
	 * @param edited - the edited text of the comment
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value="/editComment/{taskIdLink}/{commentIdLink}", method = POST)
	@ResponseBody
	public void editComment(
				@PathVariable String taskIdLink,
				@PathVariable String commentIdLink,
				@RequestParam("editedComment") String edited) {
		
		final Task task = UniqueObject.getFromIdLink(workbench.getTasks(), taskIdLink);
		final Comment comment = UniqueObject.getFromIdLink(task.getComments(), commentIdLink);
		if(comment.getAuthor().getId() == user.get().getId()) {
			comment.setText(edited);
		}
		//TODO: Tasks immutable
		data.edit(task);
	}
	
	/**
	 * Create Comment <br>
	 * -----------------------------------------------------------------
	 * @param principal - user sending the request
	 * @param idLink - identifies the task to which the comment will be added
	 * @param form - object containing all comment information
	 */
	@RequestMapping(value="/submitComment/{idLink}", method = POST)
	@ResponseBody
	public void handleTasksComment(
				@PathVariable String idLink,
				@ModelAttribute("commentForm") CommentForm form) {
		
		final Task task = UniqueObject.getFromIdLink(workbench.getTasks(), idLink);
		final Comment comment = new Comment(user.get(), form.getText());
		task.getComments().add(comment);
		//TODO: Tasks immutable
		data.edit(task);
	}
	
	/**
	 * Edit task <br>
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value="/editTask/announce", method = POST)
	@ResponseBody
	public void editTask(
			@RequestParam("idLink") String idLink) {
		
		final Task task = UniqueObject.getFromIdLink(workbench.getTasks(), idLink);
		if(task == null) {
			throw new IllegalArgumentException("Cannot edit task: idLink does not exist!");
		}
		taskSession.setupTaskFormNewEdit(task);
	}
	
	@RequestMapping(value="/editTask/submit", method = POST)
	@ResponseBody
	public void editTask(
			@ModelAttribute("taskForm") TaskForm form) {
		
		taskSession.executeEdit(form);
	}
	
	/**
	 * Create new task <br>
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value="/createTask", method = POST)
	@ResponseBody
	public List<MessageResponse> createTask(
			@ModelAttribute("taskForm") TaskForm form) {
		
		return taskSession.firstTaskCheck(form);
	}
	
	@RequestMapping(value="/createTask/next", method = POST)
	@ResponseBody
	public List<MessageResponse> createTaskNext(
			@RequestParam("operation") String operation) {
		
		return taskSession.getNextTaskCheck(new ConflictAnswer(operation, false));
	}
	
	/**
	 * TaskForm <br>
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value = "/resetTaskForm", method = POST)
	@ResponseBody
	public void resetTaskForm() {
		taskSession.setupTaskFormNewTask();
	}
	
	@RequestMapping(value = "/saveTaskForm", method = POST)
	@ResponseBody
	public void saveTaskForm(
			@ModelAttribute("taskForm") TaskForm form) {
		
		taskSession.updateTaskForm(form);
	}
	
	@RequestMapping(value = "/renderTaskForm", method = GET)
	@ResponseBody
	public TaskFormResult renderTaskForm(
			ModelMap model,
			HttpServletRequest request,
			HttpServletResponse response) {
		
		final ControllerArgs requestData = new ControllerArgs(model, request, response);
		final String taskFormHtml = ftlTemplates.insertFtl("all_taskForm", requestData);
		return new TaskFormResult(taskFormHtml, taskSession.getEditedIdLink());
	}
	
	public static class TaskFormResult {
		public final String editedTaskIdLink;
		public final String taskFormHtml;
		public TaskFormResult(String taskFormHtml, String editedTaskIdLink) {
			this.editedTaskIdLink = editedTaskIdLink;
			this.taskFormHtml = taskFormHtml;
		}
	}
	
	/**
	 * Default Handler <br>
	 * -----------------------------------------------------------------
	 * CLeansup leftover state from previous page and returns new tasks.jsp page.
	 */
	@RequestMapping(method = GET)
	public String send(ModelMap model) {
		taskSession.cleanUp();
		workbench.createInitEvent();
		
		// TODO remove when converted to ftl to remove dependency on Workbench
		model.addAttribute("workbench-sortForm", workbench.getSortForm());
		model.addAttribute("workbench-loadForm", user.get().getLoadForm());
	    return "tasks";
	}
	
	/**
	 * Task Lists <br>
	 * -----------------------------------------------------------------
	 */
	
	/**
	 * Get task data for specified ids, must be visible in workbench or pinned currently.
	 */
	@RequestMapping(value = "/renderTaskData", method = POST)
	@ResponseBody
	public List<TaskRenderResult> renderSelectedTasks(
			@RequestParam(value="idLinks[]", required=false) java.util.List<String> idLinks,
			ModelMap model,
			HttpServletRequest request,
			HttpServletResponse response) {
		
		if(idLinks == null) return new LinkedList<>(TaskRenderResult.class);
		final List<Task> tasks = new LinkedList<>(Task.class);
		for(final Task task : workbench.getTasks()) {
			final boolean contains = idLinks.remove(task.getIdLink());
			if (contains) tasks.add(task);
		}
		return ftlRenderer.renderTasks(tasks,
				new ControllerArgs(model, request, response));
	}
	
	/**
	 * Pinned Tasks
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value = "/pinned/taskListEvents", method = GET)
	@ResponseBody
	public List<TaskListEvent> syncPinned() {
		return pinned.retrieveEvents();
	}
	
	@RequestMapping(value = "/pinned/pin", method = POST)
	@ResponseBody
	public void pin(
			@RequestParam("idLink") String idLink) {
		final Task task = UniqueObject.getFromIdLink(workbench.getTasks(), idLink);
		pinned.pin(task);
	}
	
	@RequestMapping(value = "/pinned/unpin", method = POST)
	@ResponseBody
	public void unpin(
			@RequestParam("idLink") String idLink) {
		final Task task = UniqueObject.getFromIdLink(pinned.getTasks(), idLink);
		pinned.unpin(task);
	}
}
