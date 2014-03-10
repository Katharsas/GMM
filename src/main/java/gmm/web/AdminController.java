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
import java.util.Map;
import java.util.HashMap;

/* project */


@Controller
public class AdminController {
	protected final Log logger = LogFactory.getLog(getClass());
	@Autowired
	DataAccess data;
	@Autowired
	DataConfigService config;
	
	@RequestMapping(value = {"/admin.htm"} , method = RequestMethod.GET)
    public ModelAndView send(HttpServletRequest request, HttpServletResponse response)
    		throws ServletException, IOException {

		String now = (new Date()).toString();
		logger.info("Returning admin tab view");
		
		Map<String, Object> myModel = new HashMap<String, Object>();
        myModel.put("now", now);

        return new ModelAndView("admin", "model", myModel);
    }
	
	@RequestMapping(value = {"/admin.htm/saveTasks.htm"} , method = RequestMethod.GET)
	public ModelAndView saveTasks() {
		data.saveData(GeneralTask.class);
		return new ModelAndView("redirect:/admin.htm");
	}
	
	@RequestMapping(value = {"/admin.htm/loadTasks.htm"} , method = RequestMethod.GET)
	public ModelAndView loadTasks() {
		data.loadData(GeneralTask.class);
		return new ModelAndView("redirect:/admin.htm");
	}
	
	@RequestMapping(value = {"/admin.htm/test.htm"} , method = RequestMethod.POST)
	public String testFileTree(ModelMap model,
			@RequestParam("dir") String dir) throws Exception {
		
		try {dir = java.net.URLDecoder.decode(dir, "UTF-8");}
		catch (UnsupportedEncodingException e1) {e1.printStackTrace();}	
		
		//Base path restricts path access to base path or below.
		//If the dir variable does not point below the base directory,
		//it will be treated as relative path below the base directory
		String base = config.PROJECT_ORIGINAL_FILES;
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
