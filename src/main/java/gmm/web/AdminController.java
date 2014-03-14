package gmm.web;


/** Controller class & ModelAndView */
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

import org.springframework.beans.factory.annotation.Autowired;
/** Annotations */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import gmm.domain.Task;
import gmm.service.data.DataAccess;

/** javax.servlets */
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Logging */
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;



/** java */
import java.io.IOException;
import java.util.Date;

/* project */


@Controller
@RequestMapping("admin")
public class AdminController {
	protected final Log logger = LogFactory.getLog(getClass());
	@Autowired
	DataAccess data;
	
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
		data.saveData(Task.class);
		return "redirect:/admin";
	}
	
	@RequestMapping(value = "/loadTasks", method = RequestMethod.GET)
	public String loadTasks() {
		data.loadData(Task.class);
		return "redirect:/admin";
	}
	
	
	@RequestMapping(value = "/treeplugin", method = RequestMethod.POST)
	public String loadDataTreePlugin(ModelMap model) {
		model.addAttribute("path", "/Users/Jan/");
		return "jqueryFileTree";
	}
}
