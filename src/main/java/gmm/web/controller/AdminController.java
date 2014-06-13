package gmm.web.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.springframework.ui.ModelMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.collections.HashSet;
import gmm.collections.Set;
import gmm.domain.Label;
import gmm.domain.Task;
import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.TaskLoader;
import gmm.service.TaskLoader.TaskLoaderResult;
import gmm.service.data.DataAccess;
import gmm.service.data.XMLService;
import gmm.service.data.DataConfigService;
import gmm.service.tasks.TaskCreator;
import gmm.web.AjaxResponseException;
import gmm.web.FileTreeScript;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;


@Controller
@Scope("session")
@RequestMapping("admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")

public class AdminController {

	@Autowired TaskSession session;
	
	@Autowired DataAccess data;
	@Autowired DataConfigService config;
	@Autowired FileService fileService;
	@Autowired XMLService xmlService;
	@Autowired TaskCreator taskCreator;
	
	private TaskLoader taskLoader;
	
	private final Set<String> filePaths = new HashSet<>();
	boolean areTexturePaths = true;
	

	
	@ModelAttribute("task")
	public TaskForm getTaskFacade() {
		TaskForm defaultFacade = new TaskForm();
		defaultFacade.setIdName("%filename%");
		return defaultFacade;
	}
	
	/**
	 * Default Handler
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(method = RequestMethod.GET)
    public String send(ModelMap model) {
		filePaths.clear();
		model.addAttribute("users", data.getList(User.class));
	    model.addAttribute("taskLabels", data.getList(Label.class));
        return "admin";
    }
	
	@RequestMapping(value = {"/import/cancel"} , method = RequestMethod.POST)
	public @ResponseBody void cancelAssetImport() throws AjaxResponseException {
		try {
			filePaths.clear();
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String saveTasks(@RequestParam("name") String pathString) throws IOException
	{
		Path visible = config.TASKS;
		Path path = visible.resolve(fileService.restrictAccess(Paths.get(pathString+".xml"), visible));
		fileService.prepareFileCreation(path);
		xmlService.serialize(data.getList(Task.class), path);
		return "redirect:/admin";
	}
	
	@RequestMapping(value = "/load", method = RequestMethod.GET)
	public @ResponseBody TaskLoaderResult loadTasks(@RequestParam("dir") Path dir) throws AjaxResponseException {
		try {
			session.notifyDataChange();
			Path visible = config.TASKS;
			dir = fileService.restrictAccess(dir, visible);
			taskLoader = new TaskLoader(visible.resolve(dir));
			return taskLoader.loadNext("default", false);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	@RequestMapping(value = "/load/next", method = RequestMethod.GET)
	public @ResponseBody TaskLoaderResult loadNextTask (
			@RequestParam("operation") String operation,
			@RequestParam("doForAll") boolean doForAll) throws AjaxResponseException {
		try {
			return taskLoader.loadNext(operation, doForAll);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	@RequestMapping(value = {"/deleteFile"} , method = RequestMethod.POST)
	public @ResponseBody void deleteFile(@RequestParam("dir") Path dir) throws AjaxResponseException {	
		try {
			Path visible = config.TASKS;
			dir = visible.resolve(fileService.restrictAccess(dir, visible));
			fileService.delete(dir);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	@RequestMapping(value = {"/deleteTasks"} , method = RequestMethod.POST)
	public @ResponseBody void deleteTasks() {
		session.notifyDataChange();
		data.removeAll(Task.class);
	}
	
	@RequestMapping(value = {"/backups"} , method = RequestMethod.POST)
	public @ResponseBody String showBackups(@RequestParam("dir") Path dir) throws AjaxResponseException {
		try {
		
			Path visible = config.TASKS;
			Path dirPath = fileService.restrictAccess(dir, visible);
			return new FileTreeScript().html(dirPath, visible);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	@RequestMapping(value = {"/originalAssets"} , method = RequestMethod.POST)
	public @ResponseBody String showOriginalAssets(@RequestParam("dir") Path dir) throws AjaxResponseException {
		try {
			Path visible = config.ASSETS_ORIGINAL;
			dir = fileService.restrictAccess(dir, visible);
			return new FileTreeScript().html(dir, visible);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	@RequestMapping(value = {"/getAssetPaths"} , method = RequestMethod.GET)
	public @ResponseBody String[] getAssetPaths(
			@RequestParam("dir") Path dir,
			@RequestParam("textures") boolean textures) throws AjaxResponseException {
		try {
			Path visible = config.ASSETS_ORIGINAL;
			dir = fileService.restrictAccess(dir, visible);
			if(this.areTexturePaths!=textures) {
				this.filePaths.clear();
				this.areTexturePaths = textures;
			}
			String[] extensions;
			if(textures) {
				extensions = new String[]{"tga","TGA"};
			}
			else {
				extensions = new String[]{"3ds","3DS"};
			}
			this.filePaths.addAll(fileService.getRelativeNames(
					fileService.getFilePaths(visible.resolve(dir), extensions), visible));
			String[] result = this.filePaths.toArray(new String[filePaths.size()]);
			Arrays.sort(result, String.CASE_INSENSITIVE_ORDER);
			return result;
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	@RequestMapping(value = {"/importAssets"} , method = RequestMethod.POST)
	public @ResponseBody void importAssets (
			@RequestParam("textures") boolean textures,
			@ModelAttribute("task") TaskForm form) throws AjaxResponseException {
		try {
			session.notifyDataChange();
			taskCreator.importTasks(filePaths, form, TextureTask.class);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
}
