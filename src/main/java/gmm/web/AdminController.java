package gmm.web;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Arrays;

import org.springframework.ui.ModelMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.domain.Label;
import gmm.domain.Task;
import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.TaskLoader;
import gmm.service.TaskLoader.TaskLoaderResult;
import gmm.service.UserService;
import gmm.service.assets.AssetImporter;
import gmm.service.data.DataAccess;
import gmm.service.data.XMLService;
import gmm.service.data.DataConfigService;
import gmm.util.Collection;
import gmm.util.HashSet;
import gmm.util.Set;
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
	@Autowired AssetImporter importer;
	@Autowired UserService users;
	@Autowired PasswordEncoder encoder;
	
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
        return "admin";
    }
	
	@RequestMapping(value = {"/import/cancel"} , method = RequestMethod.POST)
	public @ResponseBody void cancelAssetImport() {
		filePaths.clear();
	}
	
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String saveTasks(@RequestParam("name") String pathString) throws IOException {
		
		Path visible = Paths.get(config.DATA);
		Path path = visible.resolve(fileService.restrictAccess(Paths.get(pathString+".xml"), visible));
		fileService.prepareFileCreation(path);
		xmlService.serialize(data.getList(Task.class), path);
		return "redirect:/admin";
	}
	
	@RequestMapping(value = "/load", method = RequestMethod.GET)
	public @ResponseBody TaskLoaderResult loadTasks(@RequestParam("dir") Path dir) {
		
		session.notifyDataChange();
		Path visible = Paths.get(config.DATA);
		dir = fileService.restrictAccess(dir, visible);
		try {
			taskLoader = new TaskLoader(visible.resolve(dir));
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
			importer.importTasks(filePaths, null, TextureTask.class);
		}
	}
	
	@RequestMapping(value = {"/users/edit/{idLink}"}, method = RequestMethod.POST)
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
	
	@RequestMapping(value = {"/users/admin/{idLink}"}, method = RequestMethod.POST)
	public @ResponseBody void switchAdmin(
			@PathVariable("idLink") String idLink) {
		User user = users.getByIdLink(idLink);
		user.setRole(user.getRole().equals(User.ROLE_ADMIN) ? 
				User.ROLE_USER : User.ROLE_ADMIN);
	}
	
	@RequestMapping(value = "/users/reset/{idLink}")
	public @ResponseBody String resetPassword(
			@PathVariable("idLink") String idLink) {
		User user = users.getByIdLink(idLink);
		String password = users.generatePassword();
		user.setPasswordHash(encoder.encode(password));
		return password;
	}
	
	@RequestMapping(value = "/users/save", method = RequestMethod.POST)
	public @ResponseBody void saveUsers() throws IOException {
		Path path = Paths.get(config.DATA_USERS).resolve("users.xml");
		fileService.prepareFileCreation(path);
		xmlService.serialize(users.get(), path);
	}
	
	@RequestMapping(value = "/users/load", method = RequestMethod.POST)
	public @ResponseBody void loadUsers() throws IOException {
		
		Path path = Paths.get(config.DATA_USERS).resolve("users.xml");
		data.removeAll(User.class);
		Collection<? extends User> loadedUsers =  xmlService.deserialize(path, User.class);
		for(User user : loadedUsers) {
			user.makeUnique();
		}
		data.addAll(User.class, loadedUsers);
	}
	
	@RequestMapping(value = {"/users/switch/{idLink}"}, method = RequestMethod.POST)
	public @ResponseBody void switchUser(
			@PathVariable("idLink") String idLink) {
		
		User user = users.getByIdLink(idLink);
		user.enable(!user.isEnabled());
	}
}
