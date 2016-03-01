package gmm.web.controller;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
import gmm.service.ajax.MessageResponse;
import gmm.service.data.DataAccess;
import gmm.service.data.backup.ManualBackupService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.web.FtlRenderer;
import gmm.web.FtlRenderer.RequestData;
import gmm.web.FtlRenderer.TaskRenderResult;
import gmm.web.forms.CommentForm;
import gmm.web.forms.FilterForm;
import gmm.web.forms.LoadForm;
import gmm.web.forms.SearchForm;
import gmm.web.forms.SortForm;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;
import gmm.web.sessions.WorkbenchSession;

/**
 * This controller is responsible for most task-CRUD operations requested by "tasks" page.
 * 
 * The session state and flow is managed by the TaskSession object.
 * @see {@link gmm.web.sessions.WorkbenchSession}
 * 
 * @author Jan Mothes
 * 
 */
@RequestMapping(value={"tasks"})
@PreAuthorize("hasRole('ROLE_USER')")
@Controller
public class TaskController {
	
	@Autowired private TaskServiceFinder taskCreator;
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
		modelSuppliers.put("workbench-searchForm", SearchForm::new);
		modelSuppliers.put("workbench-sortForm", workbench::getSortForm);
		modelSuppliers.put("workbench-generalFilterForm", workbench::getFilterForm);
		modelSuppliers.put("workbench-loadForm", ()->workbench.getUser().getLoadForm());
		
		templates.put("all_taskForm.ftl", new String[]{"taskForm"});
		templates.put("workbench_filters.ftl", new String[]{"workbench-generalFilterForm"});
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
	 * @param template - The filename of the template without extension.
	 * @param requestData - string will be added to this model, so you can insert the template in
	 * 		jsp code just like any other model attribute by the given template name
	 */
	private void insertTemplate(String template, RequestData requestData) {
		final String fileName = template + ".ftl";
		// populate request
		for(final String form : templates.get(fileName)) {
			requestData.request.setAttribute(form, modelSuppliers.get(form).get());
		}
		// render and insert into model
		final String result = ftlRenderer.renderTemplate(fileName, requestData);
		requestData.model.addAttribute(template, result);
		// cleanup
		for(final String form : templates.get(fileName)) {
			requestData.request.removeAttribute(form);
		}
	}
	
	/**
	 * Workbench Admin Tab <br>
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value = "workbench/admin/save", method = RequestMethod.POST)
	@ResponseBody
	public void saveTasksInWorkbench(@RequestParam("name") String pathString) throws IOException {
		manualBackups.saveTasksToXml(workbench.getTasks(), pathString);
	}
	
	@RequestMapping(value = "workbench/admin/delete", method = RequestMethod.POST)
	@ResponseBody
	public void deleteTasksInWorkbench() {
		data.removeAll(workbench.getTasks());
	}
	
	/**
	 * Load Settings <br>
	 * -----------------------------------------------------------------
	 * Changes settings for task loading and default workbench loading on login
	 * @param loadForm - object containing all task loading settings
	 */
	@RequestMapping(value="/submitLoad", method = RequestMethod.POST)
	@ResponseBody
	public void handleLoad(@ModelAttribute("workbench-loadForm") LoadForm loadForm) {
		workbench.updateLoad(loadForm);
	}
	
	/**
	 * Filter <br>
	 * -----------------------------------------------------------------
	 * Filter is always applied to all tasks of a kind
	 * @param filterForm - object containing all filter information
	 */
	@RequestMapping(value="/submitFilter", method = RequestMethod.POST)
	@ResponseBody
	public void handleFilter(@ModelAttribute("workbench-generalFilterForm") FilterForm filterForm) {
		workbench.updateFilter(filterForm);
	}
	
	
	/**
	 * Search <br>
	 * -----------------------------------------------------------------
	 * Search is always applied the tasks found by the last filter operation.
	 * @param searchForm - object containing all search information
	 */
	@RequestMapping(value="/submitSearch", method = RequestMethod.POST)
	@ResponseBody
	public void handleTasksSearch(@ModelAttribute("workbench-searchForm") SearchForm searchForm) {
		workbench.updateSearch(searchForm);
	}
	
	
	/**
	 * Sort <br>
	 * -----------------------------------------------------------------
	 * Sort is always applied on currently shown tasks.
	 * @param searchForm - object containing all search information
	 */
	@RequestMapping(value="/submitSort", method = RequestMethod.POST)
	@ResponseBody
	public void handleSorting(@ModelAttribute("workbench-sortForm") SortForm sortForm) {
		workbench.updateSort(sortForm);
	}
	
	
	/**
	 * Delete Task <br>
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the task which will be deleted
	 */
	@RequestMapping(value="/deleteTask/{idLink}", method = RequestMethod.POST)
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
	@RequestMapping(value="/editComment/{taskIdLink}/{commentIdLink}", method = RequestMethod.POST)
	public String editComment(
				@PathVariable String taskIdLink,
				@PathVariable String commentIdLink,
				@RequestParam("editedComment") String edited) {
		
		final Task task = UniqueObject.getFromIdLink(workbench.getTasks(), taskIdLink);
		final Comment comment = UniqueObject.getFromIdLink(task.getComments(), commentIdLink);
		if(comment.getAuthor().getId() == workbench.getUser().getId()) {
			comment.setText(edited);
		}
		return "redirect:/tasks";
	}
	
	
	/**
	 * Create Comment <br>
	 * -----------------------------------------------------------------
	 * @param principal - user sending the request
	 * @param idLink - identifies the task to which the comment will be added
	 * @param form - object containing all comment information
	 */
	@RequestMapping(value="/submitComment/{idLink}", method = RequestMethod.POST)
	@ResponseBody
	public void handleTasksComment(
				@PathVariable String idLink,
				@ModelAttribute("commentForm") CommentForm form) {
		
		final Comment comment = new Comment(workbench.getUser(), form.getText());
		UniqueObject.getFromIdLink(workbench.getTasks(), idLink).getComments().add(comment);
	}
	
	/**
	 * Edit task <br>
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value="/editTask/announce", method = RequestMethod.POST)
	@ResponseBody
	public void editTask(
			@RequestParam("idLink") String idLink) {
		
		final Task task = UniqueObject.getFromIdLink(workbench.getTasks(), idLink);
		if(task == null) {
			throw new IllegalArgumentException("Cannot edit task: idLink does not exist!");
		}
		taskSession.setupTaskFormNewEdit(task);
	}
	
	@RequestMapping(value="/editTask/submit", method = RequestMethod.POST)
	@ResponseBody
	public void editTask(
			@ModelAttribute("taskForm") TaskForm form) {
		
		taskSession.executeEdit(form);
	}
	
	/**
	 * Create new task <br>
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value="/createTask", method = RequestMethod.POST)
	@ResponseBody
	public List<MessageResponse> createTask(
			@ModelAttribute("taskForm") TaskForm form) {
		
		return taskSession.firstTaskCheck(form);
	}
	
	@RequestMapping(value="/createTask/next", method = RequestMethod.POST)
	@ResponseBody
	public List<MessageResponse> createTaskNext(
			@RequestParam("operation") String operation) {
		
		return taskSession.getNextTaskCheck(operation);
	}
	
	/**
	 * TaskForm <br>
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value = "/resetTaskForm", method = RequestMethod.POST)
	@ResponseBody
	public void resetTaskForm() {
		taskSession.setupTaskFormNewTask();
	}
	
	@RequestMapping(value = "/saveTaskForm", method = RequestMethod.POST)
	@ResponseBody
	public void saveTaskForm(
			@ModelAttribute("taskForm") TaskForm form) {
		
		taskSession.updateTaskForm(form);
	}
	
	@RequestMapping(value = "/renderTaskForm", method = RequestMethod.GET,  produces="text/plain")
	@ResponseBody
	public String renderTaskForm(
			ModelMap model,
			HttpServletRequest request,
			HttpServletResponse response) {
		
		final RequestData requestData = new RequestData(model, request, response);
		model.addAttribute("users", data.getList(User.class));
	    model.addAttribute("taskLabels", data.getList(Label.class));
		insertTemplate("all_taskForm", requestData);
		return (String) model.get("all_taskForm");
	}
	
	/**
	 * Default Handler <br>
	 * -----------------------------------------------------------------
	 * Used internally by other controller methods, which only modify the session object.
	 * They then call this method which sends all necessary data to the lient.
	 * 
	 * @param edit - Task ID to be made editable in task form
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String send(
			ModelMap model,
			HttpServletRequest request,
			HttpServletResponse response) {
		
		taskSession.cleanUp();
		
		final RequestData requestData = new RequestData(model, request, response);
	    insertTemplate("workbench_filters", requestData);
	    return "tasks";
	}
	
	@RequestMapping(value = "/selected", method = RequestMethod.GET)
	@ResponseBody
	public boolean[] getSelected() {
		return workbench.getSelectedTaskTypes();
	}
	
	
	@RequestMapping(value = "/load", method = RequestMethod.POST)
	@ResponseBody
	public void loadTasks(@RequestParam("type") TaskType type) {
		workbench.loadTasks(type);
	}
	
	/**
	 * Workbench tasks <br>
	 * -----------------------------------------------------------------
	 */
	
	/**
	 * Get task data for specified ids, must be visible in workbench currently.
	 */
	@RequestMapping(value = "/workbench/renderTaskData", method = RequestMethod.POST)
	@ResponseBody
	public List<TaskRenderResult> renderSelectedTasks(
			@RequestParam(value="idLinks[]", required=false) java.util.List<String> idLinks,
			ModelMap model,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		if(idLinks == null) return new LinkedList<>(TaskRenderResult.class);
		final List<Task> tasks = new LinkedList<>(Task.class);
		for(final Task task : workbench.getTasks()) {
			final boolean contains = idLinks.remove(task.getIdLink());
			if (contains) tasks.add(task);
		}
		return ftlRenderer.renderTasks(tasks,
				new RequestData(model, request, response));
	}
	
	/**
	 * Get list of the ids of the tasks currently visible in workbench.
	 */
	@RequestMapping(value = "/workbench/currentTaskIds", method = RequestMethod.GET)
	@ResponseBody
	public List<String> getCurrentTaskIds() throws Exception {
		final List<String> taskIds = new LinkedList<>(String.class);
		for(final Task task : workbench.getTasks()) {
			taskIds.add(task.getIdLink());
		}
		return taskIds;
	}
}
