package gmm.web.controller;



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import gmm.collections.List;
import gmm.domain.Task;
import gmm.service.data.DataAccess;
import gmm.web.AjaxResponseException;
import gmm.web.TaskRenderer;
import gmm.web.TaskRenderer.TaskRenderResult;
import gmm.web.forms.CommentForm;
import gmm.web.sessions.LinkSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("public")
@Controller
public class PublicController {
	
	@Autowired private LinkSession session;
	@Autowired private DataAccess data;
	@Autowired private TaskRenderer ftlTaskRenderer;
	
	@ModelAttribute("comment")
	public CommentForm getCommentForm() {return new CommentForm();}
	
	@RequestMapping(value = "/linkTasks/render", method = RequestMethod.GET)
	@ResponseBody
	public List<TaskRenderResult> renderTasks(
			ModelMap model, 
			HttpServletRequest request,
			HttpServletResponse response) throws AjaxResponseException {
		try {
			request.setAttribute("comment", getCommentForm());
			List<Task> tasks = session.getTaskLinks();
			return ftlTaskRenderer.renderTasks(tasks, model, request, response);
		} catch(Exception e) {
			throw new AjaxResponseException(e);
		}
	}
	
	@RequestMapping(value = "/linkTasks/{ids}", method = RequestMethod.GET)
	public String showTasks(ModelMap model, 
			@PathVariable String ids) {
		session.setTaskLinks(ids);
		return "links";
	}
	
	@PreAuthorize("hasRole('ROLE_USER')")
	@RequestMapping(value = "/login/linkTasks/{ids}", method = RequestMethod.GET)
	public String login(ModelMap model, 
			@PathVariable String ids) {
		return "redirect:/public/linkTasks/"+ids;
	}
}
