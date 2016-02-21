package gmm.web.controller;


import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
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

	@ModelAttribute("taskForm")
	public TaskForm getTaskForm() {return new TaskForm();}
	
	@ModelAttribute("commentForm")
	public CommentForm getCommentForm() {return new CommentForm();}
	
	@ModelAttribute("workbench-searchForm")
	public SearchForm getSearchForm() {return new SearchForm();}
	
	@ModelAttribute("workbench-sortForm")
	public SortForm getSortForm() {return workbench.getSortForm();}
	
	@ModelAttribute("workbench-generalFilterForm")
	public FilterForm getGeneralFilter() {return workbench.getFilterForm();}
	
	@ModelAttribute("workbench-loadForm")
	public LoadForm getWorkbenchLoadForm() {return workbench.getUser().getLoadForm();}

	/**
	 * For FTL rendering
	 */
	private void populateRequest(HttpServletRequest request) {
		request.setAttribute("taskForm", getTaskForm());
		request.setAttribute("commentForm", getCommentForm());
		request.setAttribute("workbench-sortForm", getSortForm());
		request.setAttribute("workbench-generalFilterForm", getGeneralFilter());
		request.setAttribute("workbench-searchForm", getSearchForm());
		request.setAttribute("workbench-loadForm", getWorkbenchLoadForm());
	}
	
	/*
	 * Workbench Admin Tab
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
	 * Load Settings
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
	 * Filter
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
	 * Search
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
	 * Sort
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
	 * Delete Task
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the task which will be deleted
	 */
	@RequestMapping(value="/deleteTask/{idLink}", method = RequestMethod.POST)
	@ResponseBody
	public void handleTasksDelete(@PathVariable String idLink) {
		data.remove(UniqueObject.getFromIdLink(workbench.getTasks(), idLink));
	}
	
	
	/**
	 * Edit Comment
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
		
		Task task = UniqueObject.getFromIdLink(workbench.getTasks(), taskIdLink);
		Comment comment = UniqueObject.getFromIdLink(task.getComments(), commentIdLink);
		if(comment.getAuthor().getId() == workbench.getUser().getId()) {
			comment.setText(edited);
		}
		return "redirect:/tasks";
	}
	
	
	/**
	 * Create Comment
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
		
		Comment comment = new Comment(workbench.getUser(), form.getText());
		UniqueObject.getFromIdLink(workbench.getTasks(), idLink).getComments().add(comment);
	}
	
	/**
	 * Edit task <br>
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value="/createTask", method = RequestMethod.POST)
	@ResponseBody
	public void editTask(
			@ModelAttribute("taskForm") TaskForm form,
			@RequestParam("idLink") String idLink) {
		
		Task task = UniqueObject.getFromIdLink(workbench.getTasks(), idLink);
		if(task == null) {
			throw new IllegalArgumentException("Cannot edit task: idLink does not exist!");
		}
		taskCreator.edit(task, form);
	}
	
	/**
	 * Create new task <br>
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value="/createTask", method = RequestMethod.POST)
	@ResponseBody
	public MessageResponse createTask(
			@ModelAttribute("taskForm") TaskForm form) {
		
		Class<? extends Task> type = form.getType().toClass();
		Task task = taskCreator.create(type, form);
		return taskSession.firstTaskCheck(task, form);
	}
	
	@RequestMapping(value="/createTask/next", method = RequestMethod.POST)
	@ResponseBody
	public MessageResponse createTaskNext(
			@RequestParam("operation") String operation) {
		
		return taskSession.getNextTaskCheck(operation);
	}
	
	/**
	 * Default Handler
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
			HttpServletResponse response,
			@ModelAttribute("taskForm") TaskForm form
//			@RequestParam(value="edit", defaultValue="") String edit
			) throws Exception {
		
		//TODO make edit form freemaker-ajax thing
		
//		if (validateId(edit)) {
//			Task task = UniqueObject.getFromIdLink(session.getTasks(), edit);
//			form = taskCreator.prepareForm(task);
//			model.addAttribute("label", task.getLabel());
//			model.addAttribute("taskForm", form);
//		}
		
	    model.addAttribute("taskList", workbench.getTasks());
	    model.addAttribute("users", data.getList(User.class));
	    model.addAttribute("taskLabels", data.getList(Label.class));
//	    model.addAttribute("edit", edit);
	    
	    populateRequest(request);
	    String filters = ftlRenderer.renderTemplate(model, "workbench_filters.ftl", request, response);
	    model.addAttribute("workbench_filters", filters);
	    
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
		List<Task> tasks = new LinkedList<>(Task.class);
		for(Task task : workbench.getTasks()) {
			boolean contains = idLinks.remove(task.getIdLink());
			if (contains) tasks.add(task);
		}
		populateRequest(request);
		return ftlRenderer.renderTasks(tasks, model, request, response);
	}
	
	/**
	 * Get list of the ids of the tasks currently visible in workbench.
	 */
	@RequestMapping(value = "/workbench/currentTaskIds", method = RequestMethod.GET)
	@ResponseBody
	public List<String> getCurrentTaskIds() throws Exception {
		List<String> taskIds = new LinkedList<>(String.class);
		for(Task task : workbench.getTasks()) {
			taskIds.add(task.getIdLink());
		}
		return taskIds;
	}
}
