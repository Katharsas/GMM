package gmm.web.controller;

import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
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
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.FileService;
import gmm.service.ajax.BundledMessageResponses;
import gmm.service.ajax.MessageResponse;
import gmm.service.ajax.operations.TaskLoaderOperations;
import gmm.service.data.BackupService;
import gmm.service.data.DataAccess;
import gmm.service.data.ManualBackupService;
import gmm.service.data.XMLService;
import gmm.service.data.DataConfigService;
import gmm.service.tasks.ModelTaskService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.service.tasks.TextureTaskService;
import gmm.web.FileTreeScript;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.AdminSession;
import gmm.web.sessions.TaskSession;


@Controller
@Scope("session")
@RequestMapping("admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")

public class AdminController {

	@Autowired private TaskSession taskSession;
	@Autowired private AdminSession session;
	
	@Autowired private DataAccess data;
	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	@Autowired private XMLService xmlService;
	@Autowired private TaskServiceFinder taskCreator;
	@Autowired private BackupService backups;
	@Autowired private ManualBackupService manualBackups;
	
	@ModelAttribute("taskForm")
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
		session.clearImportPaths();
		model.addAttribute("users", data.getList(User.class));
	    model.addAttribute("taskLabels", data.getList(Label.class));
	    return "admin";
    }
	
	/**
	 * Banner code <br/>
	 * -----------------------------------------------------------------
	 */
	
	@RequestMapping(value = {"/changeBannerMessage"} , method = RequestMethod.POST)
	public @ResponseBody void setBannerMessage(
			@RequestParam("message") String message) {
		data.getCombinedData().setCustomAdminBanner(message);
	}
	
	@RequestMapping(value = {"/activateBanner"} , method = RequestMethod.GET)
	public String activateBannerMessage() {
		data.getCombinedData().setCustomAdminBannerActive(true);
		return "redirect:/admin";
	}
	
	@RequestMapping(value = {"/deactivateBanner"} , method = RequestMethod.GET)
	public String deactivateBannerMessage() {
		data.getCombinedData().setCustomAdminBannerActive(false);
		return "redirect:/admin";
	}
	
	/**
	 * Delete all tasks <br/>
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = {"/deleteTasks"} , method = RequestMethod.POST)
	public @ResponseBody void deleteTasks() throws Exception {
		
		backups.triggerTaskBackup();
		taskSession.notifyDataChange();
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
	public @ResponseBody String[] showBackups(@RequestParam("dir") Path dir) {
		
		Path visible = config.TASKS;
		Path dirPath = fileService.restrictAccess(dir, visible);
		return new FileTreeScript().html(dirPath, visible);
	}
	
	/**
	 * Save all tasks to file.
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public void saveTasks(@RequestParam("name") String pathString) throws IOException {
		manualBackups.saveTasksToXml(data.getList(Task.class), pathString);
	}
	
	/**
	 * Delete task save file.
	 */
	@RequestMapping(value = {"/deleteFile"} , method = RequestMethod.POST)
	public @ResponseBody void deleteFile(@RequestParam("dir") Path dir) throws Exception {	
		
		Path visible = config.TASKS;
		dir = visible.resolve(fileService.restrictAccess(dir, visible));
		fileService.delete(dir);
	}
	
	/**
	 * Task Loading <br/>
	 * -----------------------------------------------------------------<br/>
	 */
	
	/**
	 * Start message conversation .<br/>
	 * @see {@link #loadNextTask(String, boolean)}
	 */
	@RequestMapping(value = "/load", method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> loadTasks(
			@RequestParam(value = "dir", required = false) Path dir) throws Exception {
		
		backups.triggerTaskBackup();
		taskSession.notifyDataChange();
		
		Iterator<? extends Task> i;
		// Load tasks from file
		if (!(dir == null)) {
			Path visible = config.TASKS;
			dir = fileService.restrictAccess(dir, visible);
			i = xmlService.deserialize(visible.resolve(dir), Task.class).iterator();
		}
		// Load tasks from asset import
		else {
			i = session.getImportedTasks();
		}
		session.taskLoader = new BundledMessageResponses<>(i, new TaskLoaderOperations());
		return session.taskLoader.loadFirstBundle();
	}
	
	/**
	 * Next message conversation. <br/>
	 * Tasks will be loaded according to received user operations. <br/>
	 */
	@RequestMapping(value = "/load/next", method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> loadNextTask (
			@RequestParam("operation") String operation,
			@RequestParam("doForAll") boolean doForAll) throws Exception {
		
		return session.taskLoader.loadNextBundle(operation, doForAll);
	}
	
	/**
	 * Asset Import <br/>
	 * -----------------------------------------------------------------<br/>
	 */
	
	/**
	 * Show original asset folder file tree.
	 */
	@RequestMapping(value = {"/originalAssets"} , method = RequestMethod.POST)
	public @ResponseBody String[] showOriginalAssets(@RequestParam("dir") Path dir) {
		
		Path visible = config.ASSETS_ORIGINAL;
		dir = fileService.restrictAccess(dir, visible);
		return new FileTreeScript().html(dir, visible);
	}
	
	/**
	 * Add to selection <br/>
	 * When user adds assets to import selection, those asset paths are saved. <br/>
	 * If user switches type of assets, selection must be cleared.
	 */
	@RequestMapping(value = {"/getAssetPaths"} , method = RequestMethod.GET)
	public @ResponseBody List<String> getAssetPaths(
			@RequestParam("dir") Path dir,
			@RequestParam("textures") boolean textures) {
		
		Path visible = config.ASSETS_ORIGINAL;
		dir = fileService.restrictAccess(dir, visible);
		FilenameFilter filter = textures ?
				TextureTaskService.extensions : ModelTaskService.extensions;
		List<Path> paths = fileService.getFilePaths(visible.resolve(dir), filter);
		session.addImportPaths(fileService.getRelativeNames(paths, visible), textures);
		return session.getImportPaths();
	}
	
	/**
	 * Clear selection when user hits cancel <br/>
	 * @see {@link #getAssetPaths(Path, boolean)}
	 */
	@RequestMapping(value = {"/import/cancel"} , method = RequestMethod.POST)
	public @ResponseBody void cancelAssetImport() {
		
		session.clearImportPaths();
	}
	
	/**
	 * Start message conversation .<br/>
	 * @see {@link #importNextAsset(String, boolean)}
	 */
	@RequestMapping(value = {"/importAssets"} , method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> importAssets (
			@RequestParam("textures") boolean textures,
			@ModelAttribute("taskForm") TaskForm form) throws Exception {
		
		backups.triggerTaskBackup();
		taskSession.notifyDataChange();
		session.assetImporter = new BundledMessageResponses<>(
				session.getImportPaths().iterator(), session.getAssetImportOperations(form));
		return session.assetImporter.loadFirstBundle();
	}
	
	/**
	 * Next message conversation. <br/>
	 * Assets will be imported according to received user operations. <br/>
	 */
	@RequestMapping(value = {"/importAssets/next"} , method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> importNextAsset(
			@RequestParam("operation") String operation,
			@RequestParam("doForAll") boolean doForAll) throws Exception {
		
		return session.assetImporter.loadNextBundle(operation, doForAll);
	}
}
