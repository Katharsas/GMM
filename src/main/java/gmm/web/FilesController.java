package gmm.web;


/** Controller class & ModelAndView */
import org.springframework.web.servlet.ModelAndView;

/** Annotations */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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
public class FilesController {
	protected final Log logger = LogFactory.getLog(getClass());
	
	
	@RequestMapping(value = {"/files.htm"} , method = RequestMethod.GET)
    public ModelAndView handleNotifiactionsRequest(HttpServletRequest request, HttpServletResponse response)
    		throws ServletException, IOException {

		String now = (new Date()).toString();
		logger.info("Returning notifications tab view");
		
		Map<String, Object> myModel = new HashMap<String, Object>();
        myModel.put("now", now);

        return new ModelAndView("files", "model", myModel);
    }
}
