package gmm.web;

/** Controller class & ModelAndView */
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import gmm.util.*;
import gmm.web.forms.CommentForm;
import gmm.web.forms.FilterForm;
import gmm.web.forms.SearchForm;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;

/**
 * Controller class which handles all GET & POST requests with root "tasks".
 * This class is responsible for most CRUD operations on tasks.
 * 
 * The inpersistent logic (session flow) is managed by the TaskSession object.
 * @see {@link gmm.web.sessions.TaskSession}
 * 
 * @author Jan Mothes aka Kellendil
 */
@RequestMapping("tasks")
@SessionAttributes({"search","generalFilter"})
@Scope("session")
@Controller
public class TaskController {
	
	@Autowired TaskSession session;
	
	@Autowired DataAccess data;
	@Autowired TaskFilterService filter;
	@Autowired UserService users;
	@Autowired AssetService assetService;
	@Autowired DataConfigService config;
	@Autowired FileService fileService;

	@ModelAttribute("task")
	public TaskForm getTaskFacade() {return new TaskForm();}
	@ModelAttribute("comment")
	public CommentForm getCommentFacade() {return new CommentForm();}
	@ModelAttribute("search")
	public SearchForm getSearchFacade() {return new SearchForm();}
	@ModelAttribute("generalFilter")
	public FilterForm getGeneralFilter() {return new FilterForm();}
	
	
	private void setHeaderCaching(HttpServletResponse response) {
//		Calendar date = new GregorianCalendar(3000, 1, 1);
//		SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyy hh:mm:ss z", Locale.US);
//		
		response.setHeader("Cache-Control", "Public");
//		response.setHeader("Max-Age", "2629000");
//		response.setHeader("Pragma", "");
//		response.setHeader("Expires", formatter.format(date.getTime()));
	}
	
	/**
	 * Texture Preview Image
	 * -----------------------------------------------------------------
	 * @param small - true for small preview, false for full size
	 * @param version - "original" for original texture preview, newest for the most current new texture
	 * @param idLink - identifies the corresponding task
	 */
	@RequestMapping(value="/preview", method = RequestMethod.GET, produces="image/png")
	public @ResponseBody byte[] sendPreview(
			HttpServletResponse response,
			@RequestParam(value="small", defaultValue="false") boolean small,
			@RequestParam(value="ver") String version,
			@RequestParam(value="id") String idLink) throws IOException {

		setHeaderCaching(response);
		//TODO enable reasonable caching
		TextureTask task = UniqueObject.<TextureTask>getFromId(data.<TextureTask>getList(TextureTask.class), idLink);
		return assetService.getPreview(task.getNewAssetFolderPath(),small,version);
	}
	
	/**
	 * Files
	 * -----------------------------------------------------------------
	 * This method is the serverside counterpart to the JQuery plugin used to display all files of
	 * a task as a file tree (jqueryFileTree.js). The server side logic mostly resides in the class
	 * {@link gmm.web.FileTreeScript}, which originally was a jsp scriptlet converted to a java class.
	 * 
	 * This method is called whenever the user expands a folder in the tree (recursion).
	 * 
	 * @param idLink - identifies the corresponding task
	 * @param subDir - "assets" if the files are assets
	 * @param dir - relative path to the requested directory/file
	 */
	@RequestMapping(value = {"/files/{subDir}/{idLink}"} , method = RequestMethod.POST)
	@ResponseBody
	public String showAssetFiles(
			@PathVariable String idLink,
			@PathVariable String subDir,
			@RequestParam("dir") Path dir) {
		
		TextureTask task = (TextureTask) UniqueObject.getFromId(session.getTasks(), idLink);
		subDir = subDir.equals("assets") ? config.NEW_TEX_ASSETS : config.NEW_TEX_OTHER;
		
		Path visible = Paths.get(task.getNewAssetFolderPath()).resolve(subDir);
		dir = fileService.restrictAccess(dir, visible);
		return new FileTreeScript().html(dir, visible);
	}
	
	/**
	 * Upload File
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 */
	@RequestMapping(value = {"/upload/{idLink}"} , method = RequestMethod.POST)
	@ResponseBody
	public String handleUpload(
			HttpServletRequest request,
			@PathVariable String idLink) throws IOException {
		
		MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		MultiValueMap<String, MultipartFile> map = multipartRequest.getMultiFileMap();
		MultipartFile file = (MultipartFile) map.getFirst("myFile");
		
		TextureTask task = (TextureTask) UniqueObject.getFromId(session.getTasks(), idLink);
		assetService.addTextureFile(file, task);
		
		return file.isEmpty()? "Upload failed!" : "Upload successfull!";
	}
	
	/**
	 * Download File
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 * @param subDir - "asset" if the file is an asset
	 * @param dir - relative path to the downloaded file
	 */
	@RequestMapping(value = {"/download/{idLink}/{subDir}/{dir}/"},
			method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public FileSystemResource handleDownload(
			HttpServletResponse response,
			@PathVariable String idLink,
			@PathVariable String subDir,
			@PathVariable Path dir) throws IOException {
		
		TextureTask task = (TextureTask) UniqueObject.getFromId(session.getTasks(), idLink);
		subDir = subDir.equals("asset") ? config.NEW_TEX_ASSETS : config.NEW_TEX_OTHER;
		
		Path filePath = Paths.get(task.getNewAssetFolderPath()).resolve(subDir).resolve(dir);
		response.setHeader("Content-Disposition", "attachment; filename=\""+filePath.getFileName()+"\"");
		return new FileSystemResource(filePath.toFile());
	}
	
	/**
	 * Delete File
	 * -----------------------------------------------------------------
	 * @param idLink - identifies the corresponding task
	 * @param asset - true if file is an asset
	 * @param dir - relative path to the deleted file
	 */
	@RequestMapping(value = {"/deleteFile/{idLink}"} , method = RequestMethod.POST)
	@ResponseBody
	public void handleDeleteFile(
			@PathVariable String idLink,
			@RequestParam("asset") Boolean asset,
			@RequestParam("dir") Path dir) throws IOException {
		
		TextureTask task = (TextureTask) UniqueObject.getFromId(session.getTasks(), idLink);
		assetService.deleteTextureFile(dir, asset, task);
	}
	
	/**
	 * Filter
	 * -----------------------------------------------------------------
	 * Filter is always applied to all tasks of a kind
	 * @param filterForm - object containing all filter information
	 * @param tab - see send method
	 */
	@RequestMapping(value="/submitFilter", method = RequestMethod.POST)
	public String handleFilter(
				Principal principal,
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
		if (isNew) data.add(task);
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
	    model.addAttribute("taskStatuses", TaskStatus.values());
	    model.addAttribute("priorities", Priority.values());
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
