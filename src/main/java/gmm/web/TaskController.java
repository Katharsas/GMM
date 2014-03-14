package gmm.web;

/** Controller class & ModelAndView */
import org.springframework.ui.ModelMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
/** Annotations */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RequestMethod;


import org.springframework.web.bind.annotation.SessionAttributes;

import org.springframework.web.bind.support.SessionStatus;


/** javax.servlets */
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;


import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

/** Logging */
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


/** java */
import java.security.Principal;

/* project */
import gmm.domain.*;
import gmm.service.data.DataAccess;
import gmm.service.data.DataBaseFilter;
import gmm.service.data.DataFilter;
import gmm.service.forms.CommentFacade;
import gmm.service.forms.GeneralFilterFacade;
import gmm.service.forms.SearchFacade;
import gmm.service.forms.TaskFacade;
import gmm.util.*;

/**
 * Controller class which handles all GET & POST requests with root "tasks".
 * This class is responsible for most CRUD operations on tasks and task comments.
 * 
 * If provides following functionality:
 * - Sending tasks sorted by type, filtered by filter otions and filtered by user search simultaneously
 * - Creating new Tasks of any type
 * - Creating new Comments on an existing task
 * - Editing an existing Task
 * - Deleting an existing Task
 * 
 * TODO:
 * - specific filtering
 * - creating/editing/deleting ModelTask and TextureTask
 * - Editing any Comments created by the same user
 * - Deleting any Comments created by the same user
 * 
 * Nice to have:
 * - Highlight text caught by search filter
 * 
 * 
 * @author Jan Mothes aka Kellendil
 */
@RequestMapping("tasks")
@SessionAttributes({"task","search","generalFilter"})
@Scope("session")
@Controller
public class TaskController {
	private final Log logger = LogFactory.getLog(getClass());
	private Collection<? extends Task> tasks;
	
	@Autowired
	DataAccess data;
	Collection<User> users;
	
	@PostConstruct
	private void init() {
		users = data.getList(User.class);
	}

	@ModelAttribute("task")
	public TaskFacade getTaskFacade() {return new TaskFacade();}
	@ModelAttribute("comment")
	public CommentFacade getCommentFacade() {return new CommentFacade();}
	@ModelAttribute("search")
	public SearchFacade getSearchFacade() {return new SearchFacade();}
	@ModelAttribute("generalFilter")
	public GeneralFilterFacade getGeneralFilter() {return new GeneralFilterFacade();}
	
	
	@RequestMapping(value="/submitFilter", method = RequestMethod.POST)
	public String handleFilter(
				Principal principal,
		 		@ModelAttribute("generalFilter") GeneralFilterFacade generalFacade,
		 		@RequestParam(value="tab", defaultValue="") String tab,
		 		@RequestParam(value="edit", defaultValue="") String edit) {
		if (!validateTab(tab)) return "redirect:/tasks/reset?tab=general";
		tasks = getTaskList(tab);
		User user = getUser(principal);
		DataFilter<Task> filter = new DataBaseFilter<Task>();
		filter.setOnlyFilterEqual(true);
		if (generalFacade.isCreatedByMe()) {
			filter.filterField(tasks, "getAuthor", user);
			tasks = filter.getFilteredElements();
			filter.clear();
		}
		if (generalFacade.isAssignedToMe()) {
			filter.filterField(tasks, "getAssigned", user);
			tasks = filter.getFilteredElements();
			filter.clear();
		}
		for(int i = 0; i<Priority.values().length; i++) {
			if (!generalFacade.getPriority()[i]) {
				tasks = filter.filterField(tasks, "getPriority", Priority.values()[i]);
			}
		}
		for(int i = 0; i<TaskStatus.values().length; i++) {
			if (!generalFacade.getTaskStatus()[i]) {
				tasks = filter.filterField(tasks, "getTaskStatus", TaskStatus.values()[i]);
			}
		}
		return "redirect:/tasks?tab="+tab+"&edit="+edit;
	}
	
	
	/**
	 * Controller method for handling a search POST request.
	 * Passes all task edit information to avoid interrupting current editing process.
	 * Applies the search to the latest sent task list.
	 * @param tab - see send method
	 * @param edit - see send method
	 * @param taskFacade - object containing task information about task currently being edited.
	 * @param facade - object containing all search information
	 */
	@RequestMapping(value="/submitSearch", method = RequestMethod.POST)
	public String handleTasksSearch(
		 		@ModelAttribute("search") SearchFacade facade,
		 		@RequestParam(value="tab", defaultValue="") String tab,
		 		@RequestParam(value="edit", defaultValue="") String edit) {
		DataFilter<Task> filter = new DataBaseFilter<Task>();
		String[] getters = new String[]{
				"getName",
				"getAuthor",
				"getDetails",
				"getLabel",
				"getAssigned"};
		if(facade.isEasySearch()) {
			filter.filterOr(tasks, getters, ListUtil.inflateToArray(facade.getEasy(), getters.length));
		}
		else {
			String[] filters = new String[]{
					facade.getName(),
					facade.getAuthor(),
					facade.getDetails(),
					facade.getLabel(),
					facade.getAssigned()};
			filter.filterAnd(tasks, getters, filters);
		}
		tasks = filter.getFilteredElements();
		return "redirect:/tasks?tab="+tab+"&edit="+edit;
	}
	
	/**
	 * Controller method for handling delete GET request.
	 * Deletes task by id and sends remaining tasks.
	 * If the currently edited task is the deleted task, the edit form will be cleared.
	 * @param tab - see send method
	 * @param delete - id of task which will be deleted
	 * @return No edit/facade, because edit id could be the id of the deleted task.
	 */
	@RequestMapping(value="/deleteTask", method = RequestMethod.GET)
	public String handleTasksDelete(
				SessionStatus status,
				@RequestParam(value="tab", defaultValue="") String tab,
				@RequestParam(value="tab", defaultValue="") String edit,
				@RequestParam(value="delete", defaultValue="") String delete) {
		boolean resetFacade = false;
		if (validateId(delete)){
			if(validateId(edit)&&edit.equals(delete)){
				edit="";
				resetFacade = true;
			}
	 		data.removeData(UniqueObject.getFromId(tasks, delete));
	 		tasks.remove(UniqueObject.getFromId(tasks, delete));
	 	}
		return "redirect:/tasks?tab="+tab+"&edit="+edit+"&resetFacade="+resetFacade;
	}

	/**
	 * Controller method for handling add comment POST request.
	 * Passes all task edit information to avoid interrupting current editing process.
	 * @param principal - User sending the request
	 * @param facade - object containing all comment information
	 * @param tab - see send method
	 * @param editComment - id of task to which the comment will be added
	 * @param edit - id of task currently being edited
	 */
	@RequestMapping(value="/submitComment", method = RequestMethod.POST)
	public String handleTasksComment(
				Principal principal,
				@ModelAttribute("comment") CommentFacade facade,
				@RequestParam(value="tab", defaultValue="") String tab,
				@RequestParam(value="editComment", defaultValue="") String editComment,
				@RequestParam(value="edit", defaultValue="") String edit) {
		if (validateId(editComment)) {
			Comment comment = new Comment(getUser(principal), facade.getText());
			UniqueObject.getFromId(tasks, editComment).getComments().add(comment);
		}
		return "redirect:/tasks?tab="+tab+"&edit="+edit;
	}

	/**
	 * Controller method for handling create/edit task POST request.
	 * If a task is edited, the current task list is sent again.
	 * If a new task is added, the currents tab default view is sent,
	 * because its unknown whether the new task should be added to current task list or not.
	 * @param principal - User sending the request
	 * @param facade - object containing all task information
	 * @param tab - see send method
	 * @param edit - id of task to be edited or null/"" if the task should be added as new task
	 */
	@RequestMapping(value="/submitTask", method = RequestMethod.POST)
	public String handleTasksCreateEdit(
			HttpSession session,
			SessionStatus status,
			Principal principal,
			@ModelAttribute("task") TaskFacade facade,
			@RequestParam(value="tab", defaultValue="") String tab,
			@RequestParam(value="edit", defaultValue="") String edit) {
		User user = getUser(principal);
		GeneralTask task;
		if (validateId(edit)) {
			task = UniqueObject.getFromId(data.<GeneralTask>getList(GeneralTask.class), edit);
			task.setName(facade.getIdName());
		}
		else {
			task = new GeneralTask(facade.getIdName(), user);
		}
		task.setPriority(facade.getPriority());
		task.setTaskStatus(facade.getStatus());
		task.setDetails(facade.getDetails());
		task.setLabel(facade.getLabel());
		User assigned = facade.getAssigned().equals("") ? null : User.getFromName(users, facade.getAssigned());
		task.setAssigned(assigned);
		String label = facade.getLabel();
		if(!label.equals("")) {
			data.addData(new Label(label));
		}
		facade.setDefaultState();
		if (validateId(edit)) {
			return "redirect:/tasks?tab="+tab;
		}
		else {
			data.addData(task);
			return "redirect:/tasks/reset?tab="+tab;
		}
		
	}

	/**
	 * Standard controller method for handling GET requests.
	 * Refreshes global task list depending on tab parameter and sends this list to the user.
	 * If no tab is given, sets default tab "general".
	 * Does not interrupt edit process.
	 * @param taskFacade - object containing all information about task currently being edited
	 * @param tab - type of task list which will be retrieved from DataBase and sent to client
	 * @param edit - see send method
	 */
	@RequestMapping(value="/reset", method = RequestMethod.GET)
	public String handleTasks(
			@RequestParam(value="tab", defaultValue="") String tab,
			@RequestParam(value="edit", defaultValue="") String edit) {
		if (!validateTab(tab)) {return "redirect:/tasks/reset?tab=general";}
		tasks = getTaskList(tab);
		return "redirect:/tasks?tab="+tab+"&edit="+edit;
	}
	
	/**
	 * Used internally by other controller methods.
	 * Other controller methods put their data into the global map "model" and Task Objects into global list "tasks".
	 * They then call this method to send everything to the client.
	 * There is no other method sending a real ModelAndView map to the client.
	 * @param tab - determines which tab will be selected on client page.
	 * @param edit - Task ID to be made editable in task form
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String send(HttpSession session, ModelMap model,
			@RequestParam(value="tab", defaultValue="") String tab,
			@RequestParam(value="edit", defaultValue="") String edit,
			@RequestParam(value="resetFacade", defaultValue="false") boolean resetFacade) {
		if(tasks==null||!validateTab(tab)) return "redirect:/tasks/reset";
		
		TaskFacade facade = (TaskFacade) session.getAttribute("task");
		if (resetFacade) {
			facade.setDefaultState();
		}
		else if (validateId(edit)) {
			Task task = UniqueObject.getFromId(getTaskList("general"), edit);
			facade.setIdName(task.getName());
			facade.setDetails(task.getDetails());
			facade.setStatus(task.getTaskStatus());
			facade.setPriority(task.getPriority());
			facade.setAssigned(task.getAssigned());
			model.addAttribute("label", task.getLabel());
		}
	    model.addAttribute("taskList", tasks);
	    model.addAttribute("users", data.getList(User.class));
	    model.addAttribute("taskLabels", data.getList(Label.class));
	    model.addAttribute("taskStatuses", TaskStatus.values());
	    model.addAttribute("priorities", Priority.values());
	    model.addAttribute("tab", tab);
	    model.addAttribute("edit", edit);
	    return "tasks";
	}
	
	private boolean validateTab(String tab){
		return tab.equals("general") || tab.equals("textures") || tab.equals("models");
	}
	
	private boolean validateId(String idNumber){
		return idNumber.matches("[0-9]+");
	}
	
	private Collection<? extends Task> getTaskList (String tab) {
		switch(tab) {
		case "textures":
			return data.<TextureTask>getList(TextureTask.class);
		case "models":
			return data.<ModelTask>getList(ModelTask.class);
		case "general":
			return data.<GeneralTask>getList(GeneralTask.class);
		default:
			System.err.println("TaskController Error: Wrong tab name!");
			throw new UnsupportedOperationException();
		}
	}
	
	private User getUser(Principal principal) {
		return (principal == null) ?
				User.NULL : 
				User.getFromName(users, principal.getName());
	}
}
