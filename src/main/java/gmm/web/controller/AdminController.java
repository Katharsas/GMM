package gmm.web.controller;

import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

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

import gmm.collections.List;
import gmm.domain.Label;
import gmm.domain.Task;
import gmm.domain.Texture;
import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.MessageResponse;
import gmm.service.ajax.operations.AssetImportOperations;
import gmm.service.ajax.operations.TaskLoaderOperations;
import gmm.service.data.DataAccess;
import gmm.service.data.XMLService;
import gmm.service.data.DataConfigService;
import gmm.service.tasks.ModelTaskService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.service.tasks.TextureTaskService;
import gmm.web.AjaxResponseException;
import gmm.web.FileTreeScript;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.AssetImportSession;
import gmm.web.sessions.TaskSession;


@Controller
@Scope("session")
@RequestMapping("admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")

public class AdminController {

	@Autowired TaskSession session;
	@Autowired AssetImportSession toImport;
	
	@Autowired DataAccess data;
	@Autowired DataConfigService config;
	@Autowired FileService fileService;
	@Autowired XMLService xmlService;
	@Autowired TaskServiceFinder taskCreator;
	
	private BundledMessageResponses<Task> taskLoader;
	private BundledMessageResponses<String> assetImporter;
	
	@ModelAttribute("task")
	public TaskForm getTaskFacade() {
		TaskForm defaultFacade = new TaskForm();
		defaultFacade.setName("%filename%");
		return defaultFacade;
	}
	
	/**
	 * Default Handler <br/>
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(method = RequestMethod.GET)
    public String send(ModelMap model) {
		toImport.clear();
		model.addAttribute("users", data.getList(User.class));
	    model.addAttribute("taskLabels", data.getList(Label.class));
        return "admin";
    }
	
	/**
	 * Delete all tasks <br/>
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = {"/deleteTasks"} , method = RequestMethod.POST)
	public @ResponseBody void deleteTasks() {
		session.notifyDataChange();
		data.removeAll(Task.class);
	}
	
	/**
	 * Task save file operations <br/>
	 * -----------------------------------------------------------------<br/>
	 */
	
	/**
	 * Show task save files.
	 */
	@RequestMapping(value = {"/backups"} , method = RequestMethod.POST)
	public @ResponseBody String showBackups(@RequestParam("dir") Path dir) throws AjaxResponseException {
		try {
		
			Path visible = config.TASKS;
			Path dirPath = fileService.restrictAccess(dir, visible);
			return new FileTreeScript().html(dirPath, visible);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Save all tasks to file.
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String saveTasks(@RequestParam("name") String pathString) throws IOException
	{
		Path visible = config.TASKS;
		Path path = visible.resolve(fileService.restrictAccess(Paths.get(pathString+".xml"), visible));
		fileService.prepareFileCreation(path);
		xmlService.serialize(data.getList(Task.class), path);
		return "redirect:/admin";
	}
	
	/**
	 * Delete task save file.
	 */
	@RequestMapping(value = {"/deleteFile"} , method = RequestMethod.POST)
	public @ResponseBody void deleteFile(@RequestParam("dir") Path dir) throws AjaxResponseException {	
		try {
			Path visible = config.TASKS;
			dir = visible.resolve(fileService.restrictAccess(dir, visible));
			fileService.delete(dir);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Task Loading <br/>
	 * -----------------------------------------------------------------<br/>
	 */
	
	/**
	 * Start message conversation .<br/>
	 * @see {@link #loadNextTask(String, boolean)}
	 */
	@RequestMapping(value = "/load", method = RequestMethod.GET)
	public @ResponseBody List<MessageResponse> loadTasks(@RequestParam("dir") Path dir) throws AjaxResponseException {
		try {
			session.notifyDataChange();
			Path visible = config.TASKS;
			dir = fileService.restrictAccess(dir, visible);
			
			Iterator<Task> i = xmlService.deserialize(visible.resolve(dir), Task.class).iterator();
			
			taskLoader = new BundledMessageResponses<Task>(i, new TaskLoaderOperations());
			return taskLoader.loadFirstBundle();
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Next message conversation. <br/>
	 * Tasks will be loaded according to received user operations. <br/>
	 */
	@RequestMapping(value = "/load/next", method = RequestMethod.GET)
	public @ResponseBody List<MessageResponse> loadNextTask (
			@RequestParam("operation") String operation,
			@RequestParam("doForAll") boolean doForAll) throws AjaxResponseException {
		try {
			return taskLoader.loadNextBundle(operation, doForAll);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Asset Import <br/>
	 * -----------------------------------------------------------------<br/>
	 */
	
	/**
	 * Show original asset folder file tree.
	 */
	@RequestMapping(value = {"/originalAssets"} , method = RequestMethod.POST)
	public @ResponseBody String showOriginalAssets(@RequestParam("dir") Path dir) throws AjaxResponseException {
		try {
			Path visible = config.ASSETS_ORIGINAL;
			dir = fileService.restrictAccess(dir, visible);
			return new FileTreeScript().html(dir, visible);
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Add to selection <br/>
	 * When user adds assets to import selection, those asset paths are saved. <br/>
	 * If user switches type of assets, selection must be cleared.
	 */
	@RequestMapping(value = {"/getAssetPaths"} , method = RequestMethod.GET)
	public @ResponseBody List<String> getAssetPaths(
			@RequestParam("dir") Path dir,
			@RequestParam("textures") boolean textures) throws AjaxResponseException {
		try {
			Path visible = config.ASSETS_ORIGINAL;
			dir = fileService.restrictAccess(dir, visible);
			FilenameFilter filter = textures ?
					TextureTaskService.extensions : ModelTaskService.extensions;
			List<Path> paths = fileService.getFilePaths(visible.resolve(dir), filter);
			toImport.addPaths(fileService.getRelativeNames(paths, visible), textures);
			return toImport.get();
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Clear selection when user hits cancel <br/>
	 * @see {@link #getAssetPaths(Path, boolean)}
	 */
	@RequestMapping(value = {"/import/cancel"} , method = RequestMethod.POST)
	public @ResponseBody void cancelAssetImport() throws AjaxResponseException {
		try {
			toImport.clear();
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Start message conversation .<br/>
	 * @see {@link #importNextAsset(String, boolean)}
	 */
	@RequestMapping(value = {"/importAssets"} , method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> importAssets (
			@RequestParam("textures") boolean textures,
			@ModelAttribute("task") TaskForm form) throws AjaxResponseException {
		try {
			session.notifyDataChange();
			assetImporter = new BundledMessageResponses<String>(toImport.get().iterator(),
					new AssetImportOperations<Texture,TextureTask>(form, TextureTask.class));
			return assetImporter.loadFirstBundle();
		}
		catch (Exception e) {throw new AjaxResponseException(e);}
	}
	
	/**
	 * Next message conversation. <br/>
	 * Assets will be imported according to received user operations. <br/>
	 */
	@RequestMapping(value = {"/importAssets/next"} , method = RequestMethod.GET)
	public @ResponseBody List<MessageResponse> importNextAsset(
			@RequestParam("operation") String operation,
			@RequestParam("doForAll") boolean doForAll) throws AjaxResponseException {
		try {
			return assetImporter.loadNextBundle(operation, doForAll);
		} catch (Exception e) {throw new AjaxResponseException(e);}
	}
}
