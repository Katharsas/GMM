package gmm.web;


/** Controller class & ModelAndView */
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.beans.factory.annotation.Autowired;
/** Annotations */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import gmm.domain.GeneralTask;
import gmm.domain.Task;
import gmm.service.data.DataAccess;






import gmm.service.data.DataConfigService;





/** javax.servlets */
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;











/** Logging */
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;













import java.io.File;
/** java */
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

/* project */


@Controller
@RequestMapping("admin")
public class AdminController {
	protected final Log logger = LogFactory.getLog(getClass());
	@Autowired
	DataAccess data;
	@Autowired
	DataConfigService config;
	
	@RequestMapping(method = RequestMethod.GET)
    public String send(ModelMap model)
    		throws ServletException, IOException {

		String now = (new Date()).toString();
		logger.info("Returning admin tab view");
		
        model.addAttribute("now", now);
        
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
		
		try {dir = java.net.URLDecoder.decode(dir, "UTF-8");}
		catch (UnsupportedEncodingException e1) {e1.printStackTrace();}	
		
		//Base path restricts dir path access to base path or below.
		//If the dir variable does not point below the base directory,
		//it will be treated as relative path below the base directory
		String base = config.ASSETS_ORIGINAL;
		try {
			String baseCanonical = new File(base).getCanonicalPath();
			String dirCanonical = new File(dir).getCanonicalPath();
			if (!dirCanonical.startsWith(baseCanonical)) {
				dir = base+dir;
				dirCanonical = new File(dir).getCanonicalPath();
			}
			if (!dirCanonical.startsWith(baseCanonical)) {
				throw new IllegalArgumentException();
			}
		}
		catch(Exception e) {
			throw new IllegalArgumentException("Wrong path input! Path is not a valid relative path!");
		}
		model.addAttribute("dir", dir);
		
		return "jqueryFileTree";
	}
}
