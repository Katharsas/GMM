package gmm.web;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Arrays;

import org.springframework.ui.ModelMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.domain.Label;
import gmm.domain.Priority;
import gmm.domain.Task;
import gmm.domain.TaskStatus;
import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.TaskLoader;
import gmm.service.TextureTaskImporter;
import gmm.service.TaskLoader.TaskLoaderResult;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.service.data.XMLService;
import gmm.service.data.DataConfigService;
import gmm.util.Collection;
import gmm.util.HashSet;
import gmm.util.Set;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


@Controller
@Scope("session")
@RequestMapping("admin")
public class AdminController {
	protected final Log logger = LogFactory.getLog(getClass());
	
	@Autowired TaskSession session;
	
	@Autowired DataAccess data;
	@Autowired DataConfigService config;
	@Autowired FileService fileService;
	@Autowired XMLService xmlService;
	@Autowired TextureTaskImporter textureImporter;
	@Autowired UserService users;
	
	private TaskLoader taskLoader;
	
	private final Set<String> filePaths = new HashSet<>();
	boolean areTexturePaths = true;
	
	@ModelAttribute("userList")
	public Collection<User> getUserList() {
		return users.get();
	}
	
	@ModelAttribute("task")
	public TaskForm getTaskFacade() {
		TaskForm defaultFacade = new TaskForm();
		defaultFacade.setIdName("%filename%");
		defaultFacade.setDetails("%filepath%");
		return defaultFacade;
	}
	
	@RequestMapping(method = RequestMethod.GET)
    public String send(ModelMap model) {
		filePaths.clear();
		model.addAttribute("users", data.getList(User.class));
	    model.addAttribute("taskLabels", data.getList(Label.class));
	    model.addAttribute("taskStatuses", TaskStatus.values());
	    model.addAttribute("priorities", Priority.values());
        return "admin";
    }
	
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String saveTasks(@RequestParam("name") String pathString) throws IOException {
		
		Path visible = Paths.get(config.DATA);
		Path path = visible.resolve(fileService.restrictAccess(Paths.get(pathString+".xml"), visible));
		fileService.prepareFileCreation(path);
		xmlService.serialize(data.getList(Task.class), path.toString());
		return "redirect:/admin";
	}
	
	@RequestMapping(value = "/load", method = RequestMethod.GET)
	public @ResponseBody TaskLoaderResult loadTasks(@RequestParam("dir") Path dir) {
		
		session.notifyDataChange();
		Path visible = Paths.get(config.DATA);
		dir = fileService.restrictAccess(dir, visible);
		try {
			taskLoader = new TaskLoader(visible.resolve(dir).toString());
		}
		catch(Exception e) {
			TaskLoaderResult result = new TaskLoaderResult();
			result.status = "finished";
			return result;
		}
		return taskLoader.loadNext("default",false);
	}
	
	@RequestMapping(value = "/load/next", method = RequestMethod.GET)
	public @ResponseBody TaskLoaderResult loadNextTask (
			@RequestParam("operation") String operation,
			@RequestParam("doForAll") boolean doForAll) {
		
		return taskLoader.loadNext(operation, doForAll);
	}
	
	@RequestMapping(value = {"/deleteFile"} , method = RequestMethod.POST)
	public @ResponseBody void deleteFile(@RequestParam("dir") Path dir) throws IOException {
		
		Path visible = Paths.get(config.DATA);
		dir = visible.resolve(fileService.restrictAccess(dir, visible));
		fileService.delete(dir);
	}
	
	@RequestMapping(value = {"/deleteTasks"} , method = RequestMethod.POST)
	public void deleteTasks() {
		session.notifyDataChange();
		data.removeAll(Task.class);
	}
	
	@RequestMapping(value = {"/backups"} , method = RequestMethod.POST)
	public @ResponseBody String showBackups(ModelMap model,
			@RequestParam("dir") Path dir) throws Exception {
		
		Path visible = Paths.get(config.DATA);
		Path dirPath = fileService.restrictAccess(dir, visible);
		return new FileTreeScript().html(dirPath, visible);
	}
	
	@RequestMapping(value = {"/originalAssets"} , method = RequestMethod.POST)
	public @ResponseBody String showOriginalAssets(ModelMap model,
			@RequestParam("dir") Path dir) throws Exception {
		
		Path visible = Paths.get(config.ASSETS_ORIGINAL);
		dir = fileService.restrictAccess(dir, visible);
		return new FileTreeScript().html(dir, visible);
	}
	
	@RequestMapping(value = {"/getAssetPaths"} , method = RequestMethod.GET)
	public @ResponseBody String[] getAssetPaths(ModelMap model,
			@RequestParam("dir") Path dir,
			@RequestParam("textures") boolean textures) {
		
		Path visible = Paths.get(config.ASSETS_ORIGINAL);
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
	
	@RequestMapping(value = {"/importAssets"} , method = RequestMethod.POST)
	public @ResponseBody void importAssets (ModelMap model,
			@RequestParam("textures") boolean textures,
			Principal principal) throws IOException {
		
		session.notifyDataChange();
		if(textures) {
			textureImporter.importTasks(config.ASSETS_ORIGINAL, filePaths, null, users.get(principal));
		}
	}
	
	@RequestMapping(value = {"/users/{idLink}"}, method = RequestMethod.POST)
	public @ResponseBody void editUser(
			@PathVariable("idLink") String idLink,
			@RequestParam(value="name", required=false) String name,
			@RequestParam(value="role", required=false) String role) {
		if(idLink.equals("new")) {
			User user = new User(name);
			data.add(user);
		}
		else {
			User user = users.getByIdLink(idLink);
			if(name != null) user.setName(name);
			if(role != null) user.setRole(role);
		}
	}
	
	@RequestMapping(value = "/users/reset/{idLink}")
	public @ResponseBody String resetPassword(
			@PathVariable("idLink") String idLink) {
		User user = users.getByIdLink(idLink);
		String password = users.generatePassword();
		user.setPasswordHash(users.encode(password));
		return password;
	}
}
