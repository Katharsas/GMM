package gmm.web.controller;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;
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

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.collections.List;
import gmm.domain.Label;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.domain.task.asset.AssetGroupType;
import gmm.service.FileService;
import gmm.service.FileService.PathFilter;
import gmm.service.ajax.ConflictAnswer;
import gmm.service.ajax.MessageResponse;
import gmm.service.data.DataAccess;
import gmm.service.data.PathConfig;
import gmm.service.data.backup.BackupExecutorService;
import gmm.service.data.backup.ManualBackupService;
import gmm.service.data.xstream.XMLService;
import gmm.service.tasks.TaskServiceFinder;
import gmm.web.ControllerArgs;
import gmm.web.FileTreeScript;
import gmm.web.FtlTemplateService;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.AdminSession;


@Controller
@Scope("session")
@RequestMapping("admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")

public class AdminController {

	@Autowired private AdminSession session;
	
	@Autowired private DataAccess data;
	@Autowired private PathConfig config;
	@Autowired private FileService fileService;
	@Autowired private XMLService xmlService;
	@Autowired private BackupExecutorService backups;
	@Autowired private ManualBackupService manualBackups;
	@Autowired private FtlTemplateService templates;
	@Autowired private TaskServiceFinder serviceFinder;
	
	public AdminController() {
	}
	
	@PostConstruct
	private void init() {
		// taskForm template dependencies
		templates.registerVariable("users", ()->data.getList(User.class));
		templates.registerVariable("taskLabels", ()->data.getList(Label.class));
		templates.registerVariable("taskForm", this::createNewTaskForm);
		templates.registerForm("taskForm", this::createNewTaskForm);
		
		templates.registerFtl("all_taskForm", "users", "taskLabels", "taskForm");
	}
	
	private TaskForm createNewTaskForm() {
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
		session.getNewAssetsWithoutTasksVfs().update();
		
		final ControllerArgs requestData = new ControllerArgs(model, request, response);
		templates.insertFtl("all_taskForm", requestData);
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
		backups.triggerTaskBackup(true);
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
		
		final Path visible = config.dbTasks();
		final Path dirPath = fileService.restrictAccess(dir, visible);
		return new FileTreeScript().html(dirPath, visible);
	}
	
	/**
	 * Save all tasks to file.
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public void saveTasks(
			@RequestParam(value = "name", required = false) String pathString) {
		if (pathString != null) {
			manualBackups.saveTasksToXml(data.getList(Task.class), pathString);
		} else {
			backups.triggerTaskBackup(true);
		}
	}
	
	/**
	 * Delete task save file.
	 */
	@RequestMapping(value = {"/deleteFile"} , method = RequestMethod.POST)
	public @ResponseBody void deleteFile(@RequestParam("dir") Path dir) {	
		
		final Path visible = config.dbTasks();
		final Path dirAbsolute = visible.resolve(fileService.restrictAccess(dir, visible));
		fileService.delete(dirAbsolute);
	}
	
	/**
	 * Task Loading <br>
	 * -----------------------------------------------------------------<br>
	 */
	
	/**
	 * AssetPath conflict checking start.
	 * Start message conversation for asset tasks. <br>
	 * @see {@link #loadNextTask(String, boolean)}
	 */
	@RequestMapping(value = "/load/assetPaths", method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> loadAssetTasks(
			@RequestParam("dir") Path dir) {
		
		backups.triggerTaskBackup(true);
		final Path visible = config.dbTasks();
		final Path dirRelative = fileService.restrictAccess(dir, visible);
		final Collection<Task> tasks =
				xmlService.deserializeAll(visible.resolve(dirRelative), Task.class);
		
		session.prepareLoadTasks(tasks);
		return session.firstAssetPathCheckBundle();
	}
	
	/**
	 * Next message conversation. <br>
	 * Tasks will be loaded according to received user operations. <br>
	 */
	@RequestMapping(value = "/load/assetPaths/next", method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> loadNextAssetTask (
			@RequestParam("operation") String operation,
			@RequestParam("doForAll") boolean doForAll) {
		
		return loadNextTask(operation, doForAll);
	}
	
	/**
	 * TaskId conflict checking start.
	 * Start message conversation for general tasks. <br>
	 * @see {@link #loadNextTask(String, boolean)}
	 */
	@RequestMapping(value = "/load/tasks", method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> loadTasks() {
		
		return session.firstTaskIdCheckBundle();
	}
	
	/**
	 * Next message conversation. <br>
	 * Tasks will be loaded according to received user operations. <br>
	 */
	@RequestMapping(value = "/load/tasks/next", method = RequestMethod.POST)
	public @ResponseBody List<MessageResponse> loadNextTask (
			@RequestParam("operation") String operation,
			@RequestParam("doForAll") boolean doForAll) {
		
		return session.nextCheckBundle(new ConflictAnswer(operation, doForAll));
	}
	
	/**
	 * Original Asset Import <br>
	 * -----------------------------------------------------------------<br/>
	 */
	
	/**
	 * Show original asset folder file tree.
	 */
	@RequestMapping(value = {"/originalAssets"} , method = RequestMethod.POST)
	public @ResponseBody String[] showOriginalAssets(@RequestParam("dir") Path dir) {
		
		final Path visible = config.assetsOriginal();
		final Path dirRelative = fileService.restrictAccess(dir, visible);
		return new FileTreeScript().html(dirRelative, visible);
	}
	
	/**
	 * Show new asset folder file tree.
	 */
	@RequestMapping(value = {"/newAssets"} , method = RequestMethod.POST)
	public @ResponseBody String[] showNewAssets(@RequestParam("dir") Path dir) {
		
		final Path vfsDir = session.getNewAssetsWithoutTasksVfs().convertRelativePathToVfs(dir);
		final Path visible = session.getNewAssetsWithoutTasksVfs().getVirtualRootAssetsNew();
		final Path dirRelative = fileService.restrictAccess(vfsDir, visible);
		return new FileTreeScript().html(dirRelative, visible);
	}
	
	/**
	 * Add to selection <br/>
	 * When user adds assets to import selection, those asset paths are saved. <br/>
	 * If user switches type of assets, selection must be cleared.
	 */
	@RequestMapping(value = {"/getAssetPaths"} , method = RequestMethod.GET)
	public @ResponseBody List<Path> getAssetPaths(
			@RequestParam("dir") Path dir,
			@RequestParam("isOriginal") boolean isOriginal) {
		
		final AssetGroupType type = AssetGroupType.get(isOriginal);
		final Path visible;
		final Path dirRelative;
		if (type.isOriginal()) {
			visible = config.assetsOriginal();
			dirRelative = fileService.restrictAccess(dir, visible);
		} else {
			visible = session.getNewAssetsWithoutTasksVfs().getVirtualRootAssetsNew();
			final Path dirVfs = session.getNewAssetsWithoutTasksVfs().convertRelativePathToVfs(dir);
			dirRelative = fileService.restrictAccess(dirVfs, visible);
		}
		final PathFilter filter = serviceFinder.getCombinedExtensionFilter();
		final Collection<Path> foundAbsPaths =
				fileService.getFilesRecursive(visible.resolve(dirRelative), filter);
		final Collection<Path> foundRelativePaths;
		if (type.isOriginal()) {
			foundRelativePaths = fileService.getRelativeNames(foundAbsPaths, visible);
		} else {
			foundRelativePaths = new ArrayList<>(Path.class, foundAbsPaths.size());
			for (final Path path : foundAbsPaths) {
				// remove root slash and treat as relative
				foundRelativePaths.add(Paths.get(path.toString().substring(1)));
			}
		}
		session.addImportPaths(foundRelativePaths, type);
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
		
		backups.triggerTaskBackup(true);
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
		
		return session.nextImportCheckBundle(new ConflictAnswer(operation, doForAll));
	}
}
