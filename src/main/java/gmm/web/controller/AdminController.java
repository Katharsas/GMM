package gmm.web.controller;

import java.nio.file.Path;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.collections.Collection;
import gmm.collections.List;
import gmm.domain.Label;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.FileService;
import gmm.service.FileService.PathFilter;
import gmm.service.ajax.MessageResponse;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.service.data.XMLService;
import gmm.service.data.backup.BackupService;
import gmm.service.data.backup.ManualBackupService;
import gmm.service.tasks.ModelTaskService;
import gmm.service.tasks.TextureTaskService;
import gmm.web.FileTreeScript;
import gmm.web.FtlRenderer;
import gmm.web.ControllerArgs;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.AdminSession;


@Controller
@Scope("session")
@RequestMapping("admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")

public class AdminController {

	@Autowired private AdminSession session;
	
	@Autowired private DataAccess data;
	@Autowired private DataConfigService config;
	@Autowired private FileService fileService;
	@Autowired private XMLService xmlService;
	@Autowired private BackupService backups;
	@Autowired private ManualBackupService manualBackups;
	@Autowired private FtlRenderer ftlRenderer;
	
	@ModelAttribute("taskForm")
	public TaskForm getTaskFacade() {
		final TaskForm defaultFacade = new TaskForm();
		defaultFacade.setName("%filename%");
		return defaultFacade;
	}
	
	/**
	 * Default Handler <br>
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String send(
			ModelMap model,
			HttpServletRequest request,
			HttpServletResponse response) {
		
		session.clearImportPaths();
		model.addAttribute("users", data.getList(User.class));
		model.addAttribute("taskLabels", data.getList(Label.class));
		
		model.addAttribute("taskForm", getTaskFacade());
		
		final ControllerArgs requestData = new ControllerArgs(model, request, response);
		request.setAttribute("taskForm", getTaskFacade());
		final String taskFormHtml = ftlRenderer.renderTemplate("all_taskForm.ftl", requestData);
		model.addAttribute("all_taskForm", taskFormHtml);
		
		return "admin";
	}
	
	/**
	 * Banner code <br>
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
	 * Delete all tasks <br>
	 * -----------------------------------------------------------------
	 */
	@RequestMapping(value = {"/deleteTasks"} , method = RequestMethod.POST)
	public @ResponseBody void deleteTasks() {
		backups.triggerTaskBackup();
		data.removeAll(Task.class);
	}
	
	/**
	 * Task save file operations <br>
	 * -----------------------------------------------------------------<br/>
	 */
	
	/**
	 * Show task save files.
	 */
	@RequestMapping(value = {"/backups"} , method = RequestMethod.POST)
	public @ResponseBody String[] showBackups(@RequestParam("dir") Path dir) {
		
		final Path visible = config.TASKS;
		final Path dirPath = fileService.restrictAccess(dir, visible);
		return new FileTreeScript().html(dirPath, visible);
	}
	
	/**
	 * Save all tasks to file.
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public void saveTasks(@RequestParam("name") String pathString) {
		manualBackups.saveTasksToXml(data.getList(Task.class), pathString);
	}
	
	/**
	 * Delete task save file.
	 */
	@RequestMapping(value = {"/deleteFile"} , method = RequestMethod.POST)
	public @ResponseBody void deleteFile(@RequestParam("dir") Path dir) {	
		
		final Path visible = config.TASKS;
		final Path dirAbsolute = visible.resolve(fileService.restrictAccess(dir, visible));
		fileService.delete(dirAbsolute);
	}
	
	/**
	 * Task Loading <br>
	 * -----------------------------------------------------------------<br>
	 */
	
	/**
	 * Load tasks from file.
	 * Start message conversation for general tasks. <br>
	 * @see {@link #loadNextTask(String, boolean)}
	 */
	@RequestMapping(value = "/load/general", method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> loadTasks(
			@RequestParam("dir") Path dir) {
		
		backups.triggerTaskBackup();
		final Path visible = config.TASKS;
		final Path dirRelative = fileService.restrictAccess(dir, visible);
		final Collection<Task> tasks =
				xmlService.deserialize(visible.resolve(dirRelative), Task.class);
		
		session.prepareLoadTasks(tasks);
		return session.firstLoadGeneralCheckBundle();
	}
	
	/**
	 * Next message conversation. <br>
	 * Tasks will be loaded according to received user operations. <br>
	 */
	@RequestMapping(value = "/load/general/next", method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> loadNextTask (
			@RequestParam("operation") String operation,
			@RequestParam("doForAll") boolean doForAll) {
		
		return session.nextLoadCheckBundle(operation, doForAll);
	}
	
	/**
	 * Continue loading tasks from file.
	 * Start message conversation for asset tasks. <br>
	 * @see {@link #loadNextTask(String, boolean)}
	 */
	@RequestMapping(value = "/load/asset", method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> loadAssetTasks() {
		
		return session.firstLoadAssetCheckBundle();
	}
	
	/**
	 * Next message conversation. <br>
	 * Tasks will be loaded according to received user operations. <br>
	 */
	@RequestMapping(value = "/load/asset/next", method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> loadNextAssetTask (
			@RequestParam("operation") String operation,
			@RequestParam("doForAll") boolean doForAll) {
		
		return loadNextTask(operation, doForAll);
	}
	
	/**
	 * Asset Import <br>
	 * -----------------------------------------------------------------<br/>
	 */
	
	/**
	 * Show original asset folder file tree.
	 */
	@RequestMapping(value = {"/originalAssets"} , method = RequestMethod.POST)
	public @ResponseBody String[] showOriginalAssets(@RequestParam("dir") Path dir) {
		
		final Path visible = config.ASSETS_ORIGINAL;
		final Path dirRelative = fileService.restrictAccess(dir, visible);
		return new FileTreeScript().html(dirRelative, visible);
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
		
		final Path visible = config.ASSETS_ORIGINAL;
		final Path dirRelative = fileService.restrictAccess(dir, visible);
		final PathFilter filter = textures ?
				TextureTaskService.extensions : ModelTaskService.extensions;
		final List<Path> paths = fileService.getFilesRecursive(visible.resolve(dirRelative), filter);
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
			@ModelAttribute("taskForm") TaskForm form) {
		
		backups.triggerTaskBackup();
		return session.firstImportCheckBundle(form);
	}
	
	/**
	 * Next message conversation. <br/>
	 * Assets will be imported according to received user operations. <br/>
	 */
	@RequestMapping(value = {"/importAssets/next"} , method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> importNextAsset(
			@RequestParam("operation") String operation,
			@RequestParam("doForAll") boolean doForAll) {
		
		return session.nextImportCheckBundle(operation, doForAll);
	}
}
