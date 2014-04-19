package gmm.web;

import java.util.Arrays;

import org.springframework.ui.ModelMap;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.domain.GeneralTask;
import gmm.domain.Label;
import gmm.domain.Priority;
import gmm.domain.Task;
import gmm.domain.TaskStatus;
import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.TaskLoader;
import gmm.service.TaskLoader.TaskLoaderResult;
import gmm.service.data.DataAccess;
import gmm.service.data.XMLService;

import gmm.service.data.DataConfigService;
import gmm.service.forms.TaskFacade;
import gmm.util.HashSet;
import gmm.util.Set;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


@Controller
@RequestMapping("admin")
public class AdminController {
	protected final Log logger = LogFactory.getLog(getClass());
	
	@Autowired
	DataAccess data;
	@Autowired
	DataConfigService config;
	@Autowired
	FileService fileService;
	@Autowired
	XMLService xmlService;
	
	private TaskLoader taskLoader;
	
	private Set<String> filePaths = new HashSet<>();
	boolean areTexturePaths = true;
	
	@ModelAttribute("task")
	public TaskFacade getTaskFacade() {
		TaskFacade defaultFacade = new TaskFacade();
		defaultFacade.setIdName("%filename%");
		defaultFacade.setDetails("%filepath%");
		return defaultFacade;
	}
	
	@RequestMapping(method = RequestMethod.GET)
    public String send(ModelMap model) {
		filePaths = new HashSet<>();
		model.addAttribute("users", data.getList(User.class));
	    model.addAttribute("taskLabels", data.getList(Label.class));
	    model.addAttribute("taskStatuses", TaskStatus.values());
	    model.addAttribute("priorities", Priority.values());
        return "admin";
    }
	
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String saveTasks(@RequestParam("name") String name) {
		String path = fileService.restrictAccess(config.DATA+name, config.DATA);
		xmlService.serialize(data.getList(Task.class), path+".xml");
		return "redirect:/admin";
	}
	
	@RequestMapping(value = "/load", method = RequestMethod.GET)
	public @ResponseBody TaskLoaderResult loadTasks(@RequestParam("dir") String dir) {
		String path = fileService.restrictAccess(dir, config.DATA);
		try {
			taskLoader = new TaskLoader(path);
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
	public @ResponseBody void deleteFile(@RequestParam("dir") String dir) {
		fileService.deleteFile(fileService.restrictAccess(dir, config.DATA));
	}
	
	@RequestMapping(value = {"/deleteTasks"} , method = RequestMethod.POST)
	public void deleteTasks() {
		data.removeAll(GeneralTask.class);
	}
	
	@RequestMapping(value = {"/backups"} , method = RequestMethod.POST)
	public String showBackups(ModelMap model,
			@RequestParam("dir") String dir) throws Exception {
		
		dir = fileService.restrictAccess(dir, config.DATA);
		model.addAttribute("dir", dir);
		return "jqueryFileTree";
	}
	
	@RequestMapping(value = {"/originalAssets"} , method = RequestMethod.POST)
	public String showOriginalAssets(ModelMap model,
			@RequestParam("dir") String dir) throws Exception {
		
		dir = fileService.restrictAccess(dir, config.ASSETS_ORIGINAL);
		model.addAttribute("dir", dir);
		return "jqueryFileTree";
	}
	
	@RequestMapping(value = {"/getAssetPaths"} , method = RequestMethod.GET)
	public @ResponseBody String[] getAssetPaths(ModelMap model,
			@RequestParam("dir") String dir,
			@RequestParam("textures") boolean textures) {
		
		dir = fileService.restrictAccess(dir, config.ASSETS_ORIGINAL);
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
		this.filePaths.addAll(fileService.getFilePaths(dir, extensions));
		String[] result = this.filePaths.toArray(new String[filePaths.size()]);
		Arrays.sort(result, String.CASE_INSENSITIVE_ORDER);
		return result;
	}
	
	@RequestMapping(value = {"/importAssets"} , method = RequestMethod.POST)
	public @ResponseBody String importAssets (ModelMap model,
			@RequestParam("textures") boolean textures) {
		return null;
	}
}
