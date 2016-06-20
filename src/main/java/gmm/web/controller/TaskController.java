package gmm.web.controller;


import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
import gmm.domain.task.TaskType;
import gmm.service.ajax.ConflictAnswer;
import gmm.service.ajax.MessageResponse;
import gmm.service.data.DataAccess;
import gmm.service.data.backup.ManualBackupService;
import gmm.web.ControllerArgs;
import gmm.web.FtlRenderer;
import gmm.web.FtlRenderer.TaskRenderResult;
import gmm.web.forms.CommentForm;
import gmm.web.forms.FilterForm;
import gmm.web.forms.LoadForm;
import gmm.web.forms.SearchForm;
import gmm.web.forms.SortForm;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;
import gmm.web.sessions.tasklist.TaskListEvent;
import gmm.web.sessions.tasklist.WorkbenchSession;

/**
 * This controller is responsible for most task-CRUD operations requested by "tasks" page.
 * 
 * The session state and flow is managed by the TaskSession object.
 * @see {@link gmm.web.sessions.tasklist.WorkbenchSession}
 * 
 * @author Jan Mothes
 * 
 */
@RequestMapping(value={"tasks"})
@PreAuthorize("hasRole('ROLE_USER')")
@Controller
public class TaskController {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired private TaskSession taskSession;
	@Autowired private WorkbenchSession workbench;
	@Autowired private DataAccess data;
	@Autowired private FtlRenderer ftlRenderer;
	@Autowired private ManualBackupService manualBackups;

	/**
	 * ModelAttributes that are used by EITHER jsp OR ftl must be in here.
	 * Any form-receiving method needs this to get a form instance for filling request data in.
	 */
	private final HashMap<String, Supplier<?>> modelSuppliers = new HashMap<>();
	
	/**
	 * Includes all ftl templates and the form bindings they need to access.
	 * Key is filename of template, value is keys from modelSuppliers.
	 */
	private final HashMap<String, String[]> templates = new HashMap<>();;
	
	@PostConstruct
	private void init() {
		modelSuppliers.put("taskForm", taskSession::getTaskForm);
		modelSuppliers.put("commentForm", CommentForm::new);
		modelSuppliers.put("workbench-searchForm", workbench::getSearchForm);
		modelSuppliers.put("workbench-sortForm", workbench::getSortForm);
		modelSuppliers.put("workbench-generalFilterForm", workbench::getFilterForm);
		modelSuppliers.put("workbench-searchForm", workbench::getSearchForm);
		modelSuppliers.put("workbench-loadForm", ()->workbench.getUser().getLoadForm());
		
		templates.put("all_taskForm", new String[]{"taskForm"});
		templates.put("workbench_filters", new String[]{"workbench-generalFilterForm"});
		templates.put("workbench_search", new String[]{"workbench-searchForm"});
	}
	
	@ModelAttribute
	public void populateModel(Model model) {
		for(final Entry<String, Supplier<?>> entry : modelSuppliers.entrySet()) {
			model.addAttribute(entry.getKey(), entry.getValue().get());
		}
	}
	
	/**
	 * Renders the given Freemaker template to a string and inserts that into into given model to
	 * provide access to the rendered html in jsp files.
	 * @param templateFile - The filename of the template without extension.
	 * @param requestData - string will be added to this model, so you can insert the template in
	 * 		jsp code just like any other model attribute by the given template name
	 */
	private String insertTemplate(String templateFile, ControllerArgs requestData) {
		// populate request
		for(final String form : templates.get(templateFile)) {
			requestData.request.setAttribute(form, modelSuppliers.get(form).get());
		}
		// render and insert into model
		final String result = ftlRenderer.renderTemplate(templateFile, requestData);
		requestData.model.addAttribute(templateFile, result);
		// cleanup
		for(final String form : templates.get(templateFile)) {
			requestData.request.removeAttribute(form);
		}
		return result;
	}
	
	/**
	 * Workbench Admin Tab <br>
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value = "workbench/admin/save", method = POST)
	@ResponseBody
	public void saveTasksInWorkbench(@RequestParam("name") String pathString) throws IOException {
		manualBackups.saveTasksToXml(workbench.getTasks(), pathString);
	}
	
	@RequestMapping(value = "workbench/admin/delete", method = POST)
	@ResponseBody
	public void deleteTasksInWorkbench() {
		data.removeAll(workbench.getTasks());
	}
	
	/**
	 * Load  <br>
	 * -----------------------------------------------------------------
	 * @param type - type whose corresponding button was clicked by user
	 */
	@RequestMapping(value = "/loadType", method = POST)
	@ResponseBody
	public void loadTasks(@RequestParam("type") TaskType type) {
		workbench.loadTasks(type);
	}
	
	/**
	 * Changes settings for task loading and default workbench loading on login
	 * @param loadForm - object containing all task loading settings
	 */
	@RequestMapping(value="/submitLoadOptions", method = POST)
	@ResponseBody
	public void handleLoad(@ModelAttribute("workbench-loadForm") LoadForm loadForm) {
		workbench.updateLoad(loadForm);
	}
	
	/**
	 * Returns what types should be visible based on what method / buttons the user clicked.
	 * @return True for the selected/active task types, false for the others. Array element
	 * 		positions correspond to {@link TaskType#values()}.
	 */
	@RequestMapping(value = "/selected", method = GET)
	@ResponseBody
	public boolean[] getSelected() {
		return workbench.getSelectedTaskTypes();
	}
	
	/**
	 * Filter <br>
	 * -----------------------------------------------------------------
	 * @param filterForm - object containing all filter information
	 * @param reset - true if user clicked the reset filter button (discard filterForm)
	 */
	@RequestMapping(value="/filter", method = { GET, POST }, produces = "application/json")
	@ResponseBody
	public Map<String, String> handleFilter(
			ControllerArgs args,
			@ModelAttribute("workbench-generalFilterForm") FilterForm filterForm,
			@RequestParam(value = "reset", required = false, defaultValue = "false") boolean reset) {
		
		if(args.getRequestMethod().equals(POST)) {
			workbench.updateFilter(reset ? new FilterForm() : filterForm);
		}
		
		final Map<String, String> answer = new HashMap<>();
		answer.put("isInDefaultState", "" + workbench.getFilterForm().isInDefaultState());
		if (reset || args.getRequestMethod().equals(GET)) {
			answer.put("html", insertTemplate("workbench_filters", args));
		}
		return answer;
	}
	
	
	/**
	 * Search <br>
	 * -----------------------------------------------------------------
	 * Search is always applied the tasks found by the last filter operation.
	 * @param searchForm - object containing all search information
	 */
	@RequestMapping(value="/search", method = { GET, POST }, produces = "application/json")
	@ResponseBody
	public Map<String, String> handleTasksSearch(
			ControllerArgs args,
			@ModelAttribute("workbench-searchForm") SearchForm searchForm,
			@RequestParam(value = "reset", required = false, defaultValue = "false") boolean reset) {
		
		if(args.getRequestMethod().equals(POST)) {
			workbench.updateSearch(reset ? new SearchForm() : searchForm);
		}
		
		final Map<String, String> answer = new HashMap<>();
		answer.put("isInDefaultState", "" + workbench.getSearchForm().isInDefaultState());
		if (reset || args.getRequestMethod().equals(GET)) {
			answer.put("html", insertTemplate("workbench_search", args));
		}
		return answer;
	}
	
	
	/**
	 * Sort <br>
	 * -----------------------------------------------------------------
	 * Sort is always applied on currently shown tasks.
	 * @param searchForm - object containing all search information
	 */
	@RequestMapping(value="/submitSort", method = POST)
	@ResponseBody
	public void handleSorting(@ModelAttribute("workbench-sortForm") SortForm sortForm) {
		workbench.updateSort(sortForm);
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
		if(comment.getAuthor().getId() == workbench.getUser().getId()) {
			comment.setText(edited);
		}
		//TODO: Tasks imumtable
		data.remove(task);
		data.add(task);
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
		final Comment comment = new Comment(workbench.getUser(), form.getText());
		task.getComments().add(comment);
		//TODO: Tasks immutable
		data.remove(task);
		data.add(task);
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
		model.addAttribute("users", data.getList(User.class));
	    model.addAttribute("taskLabels", data.getList(Label.class));
		insertTemplate("all_taskForm", requestData);
		final String taskFormHtml = (String) model.get("all_taskForm");
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
	 * Used internally by other controller methods, which only modify the session object.
	 * They then call this method which sends all necessary data to the lient.
	 * 
	 * @param edit - Task ID to be made editable in task form
	 */
	@RequestMapping(method = GET)
	public String send(
			ModelMap model,
			HttpServletRequest request,
			HttpServletResponse response) {
		
		taskSession.cleanUp();
		workbench.createInitEvent();
		final ControllerArgs requestData = new ControllerArgs(model, request, response);
	    insertTemplate("workbench_filters", requestData);
	    return "tasks";
	}
	
	/**
	 * Workbench tasks <br>
	 * -----------------------------------------------------------------
	 */
	
	/**
	 * Get task data for specified ids, must be visible in workbench currently.
	 */
	@RequestMapping(value = "/workbench/renderTaskData", method = POST)
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
	
	@RequestMapping(value = "/workbench/taskListEvents", method = GET)
	@ResponseBody
	public List<TaskListEvent> syncTaskListState() {
		final List<TaskListEvent> events = workbench.retrieveEvents();
		if (logger.isDebugEnabled()) {
			logger.debug(workbench.getUser() + " retrieved events: "
					+ Arrays.toString(events.toArray()));
		}
		return events;
	}
}
