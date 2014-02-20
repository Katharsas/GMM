package gmm.web;


/** Controller class & ModelAndView */
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
import java.util.Map;
import java.util.HashMap;

/* project */


@Controller
public class AdminController {
	protected final Log logger = LogFactory.getLog(getClass());
	@Autowired
	DataAccess data;
	
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
		data.saveData(Task.class);
		return new ModelAndView("redirect:/admin.htm");
	}
	
	@RequestMapping(value = {"/admin.htm/loadTasks.htm"} , method = RequestMethod.GET)
	public ModelAndView loadTasks() {
		data.loadData(Task.class);
		return new ModelAndView("redirect:/admin.htm");
	}
}
