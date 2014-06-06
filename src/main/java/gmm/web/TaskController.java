package gmm.web;

/** Controller class & ModelAndView */
import org.springframework.ui.ModelMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
/** Annotations */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;



/** java */
import java.security.Principal;




import gmm.domain.Comment;
import gmm.domain.GeneralTask;
import gmm.domain.Label;
import gmm.domain.Task;
import gmm.domain.UniqueObject;
import gmm.domain.User;
/* project */
import gmm.service.TaskFilterService;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.web.forms.CommentForm;
import gmm.web.forms.FilterForm;
import gmm.web.forms.SearchForm;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;

/**
 * Controller class which handles all GET & POST requests with root "tasks".
 * This class is responsible for most CRUD operations on tasks.
 * 
 * The session state and flow is managed by the TaskSession object.
 * @see {@link gmm.web.sessions.TaskSession}
 * 
 * @author Jan Mothes aka Kellendil
 */
@Controller
@RequestMapping("tasks")
@SessionAttributes({"search","generalFilter"})
@PreAuthorize("hasRole('ROLE_USER')")

public class TaskController {
	
	@Autowired TaskSession session;
	@Autowired DataAccess data;
	@Autowired TaskFilterService filter;
	@Autowired UserService users;

	@ModelAttribute("task")
	public TaskForm getTaskFacade() {return new TaskForm();}
	
	@ModelAttribute("comment")
	public CommentForm getCommentFacade() {return new CommentForm();}
	
	@ModelAttribute("search")
	public SearchForm getSearchFacade() {return new SearchForm();}
	
	@ModelAttribute("generalFilter")
	public FilterForm getGeneralFilter() {return session.getFilterForm();}

	
	/**
	 * Filter
	 * -----------------------------------------------------------------
	 * Filter is always applied to all tasks of a kind
	 * @param filterForm - object containing all filter information
	 * @param tab - see send method
	 */
	@RequestMapping(value="/submitFilter", method = RequestMethod.POST)
	public String handleFilter(
		 		@ModelAttribute("generalFilter") FilterForm filterForm) {

		session.updateFilter(filterForm);
		return "redirect:/tasks?tab="+session.getTab();
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
		return "redirect:/tasks?tab="+session.getTab();
	}
	
	
	/**
	 * Delete Task
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the task which will be deleted
	 */
	@RequestMapping(value="/deleteTask/{idLink}", method = RequestMethod.GET)
	public String handleTasksDelete(
				@PathVariable String idLink) {
		
 		data.remove(UniqueObject.getFromId(session.getTasks(), idLink));
 		session.remove(UniqueObject.getFromId(session.getTasks(), idLink));
 		
		return "redirect:/tasks?tab="+session.getTab();
	}

	
	/**
	 * Create Comment
	 * -----------------------------------------------------------------
	 * @param principal - user sending the request
	 * @param idLink - identifies the task to which the comment will be added
	 * @param form - object containing all comment information
	 * @param tab - see send method
	 */
	@RequestMapping(value="/submitComment/{idLink}", method = RequestMethod.POST)
	public String handleTasksComment(
				Principal principal,
				@PathVariable String idLink,
				@ModelAttribute("comment") CommentForm form) {
		
		Comment comment = new Comment(users.get(principal), form.getText());
		UniqueObject.getFromId(session.getTasks(), idLink).getComments().add(comment);
		
		return "redirect:/tasks?tab="+session.getTab();
	}

	
	/**
	 * Create / Edit Task
	 * -----------------------------------------------------------------
	 * If a task is edited, the current task list is sent again.
	 * If a new task is added, the currents tab default view is sent,
	 * because its unknown whether the new task should be added to current task list or not.
	 * TODO: Make selection a mask that cna be applied multiple times.
	 * TODO: Save this mask to session when changed, an apply it to the new element.
	 * 
	 * @param principal - User sending the request
	 * @param form - object containing all task information
	 * @param tab - see send method
	 * @param idLink - id of task to be edited or null/"" if the task should be added as new task
	 */
	@RequestMapping(value="/submitTask", method = RequestMethod.POST)
	public String handleTasksCreateEdit(
			SessionStatus status,
			Principal principal,
			@ModelAttribute("task") TaskForm form,
			@RequestParam(value="edit", defaultValue="") String idLink) {
		User user = users.get(principal);
		Task task;
		boolean isNew = !validateId(idLink);

		if(isNew) {
			task = new GeneralTask(form.getIdName(), user);
		}
		else {
			task = UniqueObject.getFromId(session.getTasks(), idLink);
			task.setName(form.getIdName());
		}
		task.setPriority(form.getPriority());
		task.setTaskStatus(form.getStatus());
		task.setDetails(form.getDetails());
		task.setLabel(form.getLabel());
		User assigned = form.getAssigned().equals("") ? null : users.get(form.getAssigned());
		task.setAssigned(assigned);
		String label = form.getLabel();
		if(!label.equals("")) {
			data.add(new Label(label));
		}
		if (isNew) {
			data.add(task);
			session.add(task);
		}
		return "redirect:/tasks?tab="+session.getTab();
	}

	
	/**
	 * Redirects to default mapping. Does nothing else.
	 * @deprecated Use default controller mapping/handler instead.
	 */
	@RequestMapping(value="/reset", method = RequestMethod.GET)
	@Deprecated
	public String handleTasks(@RequestParam("tab") String tab) {
		return "redirect:/tasks?tab="+tab;
	}
	
	
	/**
	 * Default Handler
	 * -----------------------------------------------------------------
	 * Used internally by other controller methods, which only modify the session object.
	 * They then call this method which sends all necessary data to the lient.
	 * 
	 * @param tab - determines which tab will be selected on client page.
	 * @param edit - Task ID to be made editable in task form
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String send(
			ModelMap model,
			@ModelAttribute("task") TaskForm form,
			@RequestParam(value="tab", defaultValue="") String tab,
			@RequestParam(value="edit", defaultValue="") String edit) {
		
		if(!validateTab(tab)) {
			return "redirect:/tasks?tab="+session.getTab();
		}
		
		if (validateId(edit)) {
			Task task = UniqueObject.getFromId(session.getTasks(), edit);
			form.setIdName(task.getName());
			form.setDetails(task.getDetails());
			form.setStatus(task.getTaskStatus());
			form.setPriority(task.getPriority());
			form.setAssigned(task.getAssigned());
			model.addAttribute("label", task.getLabel());
		}
		session.updateTab(tab);
		
	    model.addAttribute("taskList", session.getTasks());
	    model.addAttribute("users", data.getList(User.class));
	    model.addAttribute("taskLabels", data.getList(Label.class));
	    model.addAttribute("tab", tab);
	    model.addAttribute("edit", edit);
	    return "tasks";
	}
	
	
	private boolean validateTab(String tab){
		return tab.equals("general") || tab.equals("textures") || tab.equals("models");
	}
	
	
	private boolean validateId(String idLink){
		return idLink != null && idLink.matches(".*[0-9]+");
	}
}
