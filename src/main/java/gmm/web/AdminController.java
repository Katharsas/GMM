package gmm.web;

import java.util.Arrays;

import org.springframework.ui.ModelMap;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.domain.GeneralTask;
import gmm.service.AssetFileService;
import gmm.service.data.DataAccess;

import gmm.service.data.DataConfigService;
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
	AssetFileService assetFileService;
	
	private Set<String> filePaths = new HashSet<>();
	boolean areTexturePaths = true;
	
	@RequestMapping(method = RequestMethod.GET)
    public String send(ModelMap model) {
        return "admin";
    }
	
	@RequestMapping(value = "/saveTasks", method = RequestMethod.GET)
	public String saveTasks() {
		data.saveData(GeneralTask.class);
		return "redirect:/admin";
	}
	
	@RequestMapping(value = "/loadTasks", method = RequestMethod.GET)
	public String loadTasks() {
		data.loadData(GeneralTask.class);
		return "redirect:/admin";
	}
	
	@RequestMapping(value = {"/deleteTasks"} , method = RequestMethod.GET)
	public String deleteTasks() {
		data.removeAllData(GeneralTask.class);
		return "redirect:/admin";
	}
	
	@RequestMapping(value = {"/import"} , method = RequestMethod.POST)
	public String testFileTree(ModelMap model,
			@RequestParam("dir") String dir) throws Exception {
		
		dir = assetFileService.restrictAccess(dir, config.ASSETS_ORIGINAL);
		model.addAttribute("dir", dir);
		
		return "jqueryFileTree";
	}
	
	@RequestMapping(value = {"/getAssetPaths"} , method = RequestMethod.GET)
	public @ResponseBody String[] getAssetPaths(ModelMap model,
			@RequestParam("dir") String dir,
			@RequestParam("textures") boolean textures) {
		
		dir = assetFileService.restrictAccess(dir, config.ASSETS_ORIGINAL);
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
		this.filePaths.addAll(assetFileService.getFilePaths(dir, extensions));
		String[] result = this.filePaths.toArray(new String[filePaths.size()]);
		Arrays.sort(result, String.CASE_INSENSITIVE_ORDER);
		return result;
	}
}
