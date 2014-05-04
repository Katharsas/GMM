package gmm.web;

/** Controller class & ModelAndView */
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
/** Annotations */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Logging */
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


import java.io.File;
import java.io.IOException;
/** java */
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;


/* project */
import gmm.domain.*;
import gmm.service.AssetService;
import gmm.service.FileService;
import gmm.service.TaskFilterService;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.forms.CommentFacade;
import gmm.service.forms.GeneralFilterFacade;
import gmm.service.forms.SearchFacade;
import gmm.service.forms.TaskFacade;
import gmm.util.*;

/**
 * Controller class which handles all GET & POST requests with root "tasks".
 * This class is responsible for most CRUD operations on tasks and task comments.
 * 
 * 
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
	
	private Collection<? extends Task> filteredTasks;
	private Collection<? extends Task> tasks;
	
	@Autowired
	DataAccess data;
	@Autowired
	TaskFilterService filter;
	@Autowired
	UserService users;
	@Autowired
	AssetService assetService;
	@Autowired
	DataConfigService config;
	@Autowired
	FileService fileService;

	@ModelAttribute("task")
	public TaskFacade getTaskFacade() {return new TaskFacade();}
	@ModelAttribute("comment")
	public CommentFacade getCommentFacade() {return new CommentFacade();}
	@ModelAttribute("search")
	public SearchFacade getSearchFacade() {return new SearchFacade();}
	@ModelAttribute("generalFilter")
	public GeneralFilterFacade getGeneralFilter() {return new GeneralFilterFacade();}
	
	
	public void setHeaderCaching(HttpServletResponse response) {
		Calendar date = new GregorianCalendar(3000, 1, 1);
		SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss z", Locale.US);
		
		response.setHeader("Cache-Control", "Public");
		response.setHeader("Max-Age", "2629000");
		response.setHeader("Pragma", "");
		response.setHeader("Expires", formatter.format(date.getTime()));
	}
	
	
	@RequestMapping(value="/preview", method = RequestMethod.GET, produces="image/png")
	public @ResponseBody byte[] sendPreview(
			HttpServletResponse response,
			@RequestParam(value="small", defaultValue="false") boolean small,
			@RequestParam(value="ver") String version,
			@RequestParam(value="id") String id) throws IOException {

//		setHeaderCaching(response);
		//TODO enable reasonable caching
		TextureTask task = UniqueObject.<TextureTask>getFromId(data.<TextureTask>getList(TextureTask.class), id);
		return assetService.getPreview(task.getNewAssetFolderPath(),small,version);
	}
	
	@RequestMapping(value = {"/files/{subDir}/{idLink}"} , method = RequestMethod.POST)
	public String showAssetFiles(ModelMap model,
			@PathVariable String idLink,
			@PathVariable String subDir,
			@RequestParam("dir") String dir,
			@RequestParam("tab") String tab) {
		
		TextureTask task = (TextureTask) UniqueObject.getFromId(getTaskList(tab), idLink);
		subDir = subDir.equals("assets") ? config.NEW_TEX_ASSETS : config.NEW_TEX_OTHER;
		String base = task.getNewAssetFolderPath()+"/"+subDir+"/";
		
		dir = fileService.restrictAccess(dir, base);
		model.addAttribute("dir", dir);
		return "jqueryFileTree";
	}
	
	@RequestMapping(value = {"/upload/{idLink}"} , method = RequestMethod.POST)
	public @ResponseBody String handleUpload(HttpServletRequest request,
			@PathVariable String idLink,
			@RequestParam("tab") String tab) throws IOException {
		
		MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		MultiValueMap<String, MultipartFile> map = multipartRequest.getMultiFileMap();
		MultipartFile file = (MultipartFile) map.getFirst("myFile");
		
		TextureTask task = (TextureTask) UniqueObject.getFromId(getTaskList(tab), idLink);
		assetService.addTextureFile(file, task);
		
		return file.isEmpty()? "Upload failed!" : "Upload successfull!";
	}
	
	@RequestMapping(value = {"/deleteFile/{idLink}"} , method = RequestMethod.POST)
	public @ResponseBody void deleteFile(
			@PathVariable String idLink,
			@RequestParam("tab") String tab,
			@RequestParam("dir") String dir) throws IOException {
		
		TextureTask task = (TextureTask) UniqueObject.getFromId(getTaskList(tab), idLink);
		fileService.delete(fileService.restrictAccess(dir, task.getNewAssetFolderPath()));
	}
	
	
	@RequestMapping(value="/submitFilter", method = RequestMethod.POST)
	public String handleFilter(
				Principal principal,
		 		@ModelAttribute("generalFilter") GeneralFilterFacade generalFacade,
		 		@RequestParam(value="tab", defaultValue="") String tab,
		 		@RequestParam(value="edit", defaultValue="") String edit) {
		if (!validateTab(tab)) return "redirect:/tasks/reset?tab=general";
		filteredTasks = filter.filter(getTaskList(tab), generalFacade, users.get(principal));
		tasks = filteredTasks;
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
		
		tasks = filter.search(filteredTasks, facade);
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
	 		data.remove(UniqueObject.getFromId(tasks, delete));
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
			Comment comment = new Comment(users.get(principal), facade.getText());
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
		User user = users.get(principal);
		Task task;
		if (validateId(edit)) {
			task = UniqueObject.getFromId(getTaskList(tab), edit);
			task.setName(facade.getIdName());
		}
		else {
			task = new GeneralTask(facade.getIdName(), user);
		}
		task.setPriority(facade.getPriority());
		task.setTaskStatus(facade.getStatus());
		task.setDetails(facade.getDetails());
		task.setLabel(facade.getLabel());
		User assigned = facade.getAssigned().equals("") ? null : users.get(facade.getAssigned());
		task.setAssigned(assigned);
		String label = facade.getLabel();
		if(!label.equals("")) {
			data.add(new Label(label));
		}
		facade.setDefaultState();
		if (validateId(edit)) {
			return "redirect:/tasks?tab="+tab;
		}
		else {
			data.add(task);
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
		filteredTasks = getTaskList(tab);
		tasks = filteredTasks;
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
			Task task = UniqueObject.getFromId(getTaskList(tab), edit);
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
		return idNumber.matches(".*[0-9]+");
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
}
