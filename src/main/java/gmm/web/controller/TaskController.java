package gmm.web.controller;

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









import com.technologicaloddity.capturejsp.util.SwallowingJspRenderer;

import java.io.IOException;
/** java */







import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import gmm.domain.Comment;
import gmm.domain.Label;
import gmm.domain.Task;
import gmm.domain.TaskType;
import gmm.domain.UniqueObject;
import gmm.domain.User;
/* project */
import gmm.service.TaskFilterService;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.service.tasks.TaskCreator;
import gmm.web.forms.CommentForm;
import gmm.web.forms.FilterForm;
import gmm.web.forms.SearchForm;
import gmm.web.forms.SortForm;
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
@RequestMapping(value={"tasks", "/"})
@SessionAttributes({"search", "sort", "generalFilter"})
@PreAuthorize("hasRole('ROLE_USER')")

public class TaskController {
	
	@Autowired TaskCreator taskCreator;
	@Autowired TaskSession session;
	@Autowired DataAccess data;
	@Autowired TaskFilterService filter;
	@Autowired UserService users;
//	@Autowired SwallowingJspRenderer jspRenderer;

	@ModelAttribute("task")
	public TaskForm getTaskFacade() {return new TaskForm();}
	
	@ModelAttribute("comment")
	public CommentForm getCommentFacade() {return new CommentForm();}
	
	@ModelAttribute("sort")
	public SortForm getSortFacade() {return session.getSortForm();}
	
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
	 * Sort
	 * -----------------------------------------------------------------
	 * Sort is always applied on currently shown tasks.
	 * @param searchForm - object containing all search information
	 */
	@RequestMapping(value="/submitSort", method = RequestMethod.POST)
	public String handleSorting(
		 		@ModelAttribute("sort") SortForm sortForm) {
		
		session.updateSort(sortForm);
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
				@ModelAttribute("editedComment") String edited) {
		Task task = UniqueObject.getFromId(session.getTasks(), taskIdLink);
		Comment comment = UniqueObject.getFromId(task.getComments(), commentIdLink);
		if(comment.getAuthor().getId() == session.getUser().getId()) {
			comment.setText(edited);
		}
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
				@PathVariable String idLink,
				@ModelAttribute("comment") CommentForm form) {
		Comment comment = new Comment(session.getUser(), form.getText());
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
	 * @throws IOException 
	 */
	@RequestMapping(value="/submitTask", method = RequestMethod.POST)
	public String handleTasksCreateEdit(
			@ModelAttribute("task") TaskForm form,
			@RequestParam(value="edit", defaultValue="") String idLink) throws IOException {
		
		Task task;
		boolean isNew = !validateId(idLink);
		Class<? extends Task> type = form.getType().toClass();

		if(isNew) {
			task = taskCreator.createTask(type, form);
			task = type.cast(task);
			data.add(task);
			if(session.getCurrentTaskType().toClass().equals(type)) {
				session.add(task);
			}
		}
		else {
			task = UniqueObject.getFromId(session.getTasks(), idLink);
			taskCreator.editTask(task, form);
		}
		return "redirect:/tasks?tab="+session.getTab();
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
			HttpServletRequest request,
			HttpServletResponse response,
			@ModelAttribute("task") TaskForm form,
			@RequestParam(value="tab", defaultValue="") String tab,
			@RequestParam(value="edit", defaultValue="") String edit) {
		
		if(tab.equals("")) {return "redirect:/tasks?tab="+session.getTab();}
		
		if (validateId(edit)) {
			Task task = UniqueObject.getFromId(session.getTasks(), edit);
			form = taskCreator.prepareForm(task);
			model.addAttribute("label", task.getLabel());
			model.addAttribute("task", form);
		}
		session.updateTab(TaskType.fromTab(tab));
		
	    model.addAttribute("taskList", session.getTasks());
	    model.addAttribute("users", data.getList(User.class));
	    model.addAttribute("taskLabels", data.getList(Label.class));
	    model.addAttribute("tab", tab);
	    model.addAttribute("edit", edit);
	    
	    try {
//	    	
//	    	StringWriter sout = new StringWriter();
//	    	StringBuffer buffer = sout.getBuffer();
//
//	    	HttpServletResponse realResponse = response;
//	    	HttpServletResponse fakeResponse = new SwallowingHttpServletResponse(realResponse, sout, realResponse.getCharacterEncoding());
//
//	    	HttpServletRequest realRequest = request;
//	    	realRequest.setAttribute(WebContext.ATTRIBUTE_DWR, Boolean.TRUE);
//
//	    	Spring.getServletContext().getRequestDispatcher("/WEB-INF/jsp/tasks.jsp").forward(realRequest, fakeResponse);
//
//	    	String jspOutput = buffer.toString();
//	    	
	    	
	    	
	    	
//			String jspOutput = jspRenderer.render("tasks", model, request, response);
//	    	
//	    	
//			System.out.println("========================= JSP Render Start ========================");
//		    System.out.println(jspOutput);
//		    System.out.println("=========================  JSP Render End  ========================");
		} catch (Exception e) {
			e.printStackTrace();
		}
	    
	    return "tasks";
	}
	
	private boolean validateId(String idLink){
		return idLink != null && idLink.matches(".*[0-9]+");
	}
}
