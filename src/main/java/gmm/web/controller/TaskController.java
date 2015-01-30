package gmm.web.controller;

/** Other*/

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
/** Annotations */

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
/** java */



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/* project */




import gmm.collections.List;
import gmm.domain.Comment;
import gmm.domain.Label;
import gmm.domain.Task;
import gmm.domain.TaskType;
import gmm.domain.UniqueObject;
import gmm.domain.User;
import gmm.service.TaskFilterService;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.service.tasks.TaskServiceFinder;
import gmm.web.AjaxResponseException;
import gmm.web.TaskRenderer;
import gmm.web.TaskRenderer.TaskRenderResult;
import gmm.web.forms.CommentForm;
import gmm.web.forms.FilterForm;
import gmm.web.forms.SearchForm;
import gmm.web.forms.SortForm;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;

/**
 * This controller is responsible for most task-CRUD operations requested by "tasks" page.
 * 
 * The session state and flow is managed by the TaskSession object.
 * @see {@link gmm.web.sessions.TaskSession}
 * 
 * @author Jan Mothes
 * 
 */
@RequestMapping(value={"tasks", "/"})
@SessionAttributes({"search", "sort", "generalFilter"})
@PreAuthorize("hasRole('ROLE_USER')")
@Controller
public class TaskController {
	
	@Autowired private TaskServiceFinder taskCreator;
	@Autowired private TaskSession session;
	@Autowired private DataAccess data;
	@Autowired private TaskFilterService filter;
	@Autowired private UserService users;
	@Autowired private TaskRenderer ftlTaskRenderer;

	@ModelAttribute("task")
	public TaskForm getTaskForm() {return new TaskForm();}
	
	@ModelAttribute("search")
	public SearchForm getSearchForm() {return new SearchForm();}
	
	@ModelAttribute("comment")
	public CommentForm getCommentForm() {return new CommentForm();}
	
	@ModelAttribute("sort")
	public SortForm getSortForm() {return session.getSortForm();}
	
	@ModelAttribute("generalFilter")
	public FilterForm getGeneralFilter() {return session.getFilterForm();}

	/**
	 * For FTL rendering
	 */
	private void populateRequest(HttpServletRequest request) {
		request.setAttribute("task", getTaskForm());
		request.setAttribute("search", getSearchForm());
		request.setAttribute("comment", getCommentForm());
		request.setAttribute("sort", getSortForm());
		request.setAttribute("generalFilter", getGeneralFilter());
	}
	
	/**
	 * Filter
	 * -----------------------------------------------------------------
	 * Filter is always applied to all tasks of a kind
	 * @param filterForm - object containing all filter information
	 */
	@RequestMapping(value="/submitFilter", method = RequestMethod.POST)
	public String handleFilter(
		 		@ModelAttribute("generalFilter") FilterForm filterForm) {

		session.updateFilter(filterForm);
		return "redirect:/tasks";
	}
	
	
	/**
	 * Search
	 * -----------------------------------------------------------------
	 * Search is always applied the tasks found by the last filter operation.
	 * @param searchForm - object containing all search information
	 */
	@RequestMapping(value="/submitSearch", method = RequestMethod.POST)
	public String handleTasksSearch(
		 		@ModelAttribute("search") SearchForm searchForm) {
		
		session.updateSearch(searchForm);
		return "redirect:/tasks";
	}
	
	
	/**
	 * Sort
	 * -----------------------------------------------------------------
	 * Sort is always applied on currently shown tasks.
	 * @param searchForm - object containing all search information
	 */
	@RequestMapping(value="/submitSort", method = RequestMethod.POST)
	public String handleSorting(
		 		@ModelAttribute("sort") SortForm sortForm) {
		
		session.updateSort(sortForm);
		return "redirect:/tasks";
	}
	
	
	/**
	 * Delete Task
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the task which will be deleted
	 */
	@RequestMapping(value="/deleteTask/{idLink}", method = RequestMethod.POST)
	@ResponseBody
	public void handleTasksDelete(
				@PathVariable String idLink) throws AjaxResponseException {
		try {
			data.remove(UniqueObject.getFromIdLink(session.getTasks(), idLink));
			session.remove(UniqueObject.getFromIdLink(session.getTasks(), idLink));
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
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
		Task task = UniqueObject.getFromIdLink(session.getTasks(), taskIdLink);
		Comment comment = UniqueObject.getFromIdLink(task.getComments(), commentIdLink);
		if(comment.getAuthor().getId() == session.getUser().getId()) {
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
	public String handleTasksComment(
				@PathVariable String idLink,
				@ModelAttribute("comment") CommentForm form) {
		Comment comment = new Comment(session.getUser(), form.getText());
		UniqueObject.getFromIdLink(session.getTasks(), idLink).getComments().add(comment);
		
		return "redirect:/tasks";
	}

	
	/**
	 * Create / Edit Task
	 * -----------------------------------------------------------------
	 * If the task is new, it will be added to session if its types matches the current tab.
	 * @param form - object containing all task information
	 * @param idLink - id of task to be edited or null/"" if the task should be added as new task
	 */
	@RequestMapping(value="/submitTask", method = RequestMethod.POST)
	public String handleTasksCreateEdit(
			@ModelAttribute("task") TaskForm form,
			@RequestParam(value="edit", defaultValue="") String idLink) throws Exception {
		
		Task task;
		boolean isNew = !validateId(idLink);
		Class<? extends Task> type = form.getType().toClass();

		if(isNew) {
			task = taskCreator.create(type, form);
			task = type.cast(task);
			data.add(task);
			if(session.getCurrentTaskType().toClass().equals(type)) {
				session.add(task);
			}
		}
		else {
			task = UniqueObject.getFromIdLink(session.getTasks(), idLink);
			taskCreator.edit(task, form);
		}
		return "redirect:/tasks";
	}
	
	/**
	 * Default Handler
	 * -----------------------------------------------------------------
	 * Used internally by other controller methods, which only modify the session object.
	 * They then call this method which sends all necessary data to the lient.
	 * 
	 * TODO: put tab change into extra method receiving ajax request
	 * @param tab - determines which tab will be selected on client page.
	 * @param edit - Task ID to be made editable in task form
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String send(
			ModelMap model,
			HttpServletRequest request,
			HttpServletResponse response,
			@ModelAttribute("task") TaskForm form,
			@RequestParam(value="tab", required=false) String tab,
			@RequestParam(value="edit", defaultValue="") String edit) {
		
		if(tab != null) {
			TaskType type = TaskType.fromTab(tab);
			session.updateTab(type);
			form.setType(type);
		}
		
		if (validateId(edit)) {
			Task task = UniqueObject.getFromIdLink(session.getTasks(), edit);
			form = taskCreator.prepareForm(task);
			model.addAttribute("label", task.getLabel());
			model.addAttribute("task", form);
		}
		
	    model.addAttribute("taskList", session.getTasks());
	    model.addAttribute("users", data.getList(User.class));
	    model.addAttribute("taskLabels", data.getList(Label.class));
	    model.addAttribute("tab", session.getCurrentTaskType().getTab());
	    model.addAttribute("edit", edit);
	    
	    return "tasks";
	}
	
	private boolean validateId(String idLink){
		return idLink != null && idLink.matches(".*[0-9]+");
	}
	
	@RequestMapping(value = "/render", method = RequestMethod.GET)
	@ResponseBody
	public List<TaskRenderResult> taskMap(
			ModelMap model,
			HttpServletRequest request,
			HttpServletResponse response) throws AjaxResponseException {
		try {
			populateRequest(request);
			return ftlTaskRenderer.renderTasks(session.getTasks(), model, request, response);
		} catch(Exception e) {
			throw new AjaxResponseException(e);
		}
	}
}
