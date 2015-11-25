package gmm.web.controller;



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.web.FtlRenderer;
import gmm.web.FtlRenderer.TaskRenderResult;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("public")
@Controller
public class PublicController {
	
	@Autowired private LinkSession session;
	@Autowired private FtlRenderer ftlTaskRenderer;
	
	@ModelAttribute("comment")
	public CommentForm getCommentForm() {return new CommentForm();}
	
	/**
	 * Serves task data to client ajax code.
	 */
	@RequestMapping(value = "/linkedTasks/renderTaskData", method = RequestMethod.POST)
	@ResponseBody
	public List<TaskRenderResult> renderTasks(
			@RequestParam(value="idLinks[]", required=false) java.util.List<String> idLinks,
			ModelMap model, 
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		if(idLinks == null) return new LinkedList<>(TaskRenderResult.class);
		List<Task> tasks = new LinkedList<>(Task.class);
		for(Task task : session.getTaskLinks()) {
			boolean contains = idLinks.remove(task.getIdLink());
			if (contains) tasks.add(task);
		}
		request.setAttribute("commentForm", getCommentForm());
		return ftlTaskRenderer.renderTasks(tasks, model, request, response);
	}
	
	/**
	 * Get list of the ids of the linked tasks.
	 */
	@RequestMapping(value = "/linkedTasks/currentTaskIds", method = RequestMethod.GET)
	@ResponseBody
	public List<String> getCurrentTaskIds() throws Exception {
		List<String> taskIds = new LinkedList<>(String.class);
		for(Task task : session.getTaskLinks()) {
			taskIds.add(task.getIdLink());
		}
		return taskIds;
	}
	
	/**
	 * Stores ids and waits for client ajax code to do more stuff.
	 */
	@RequestMapping(value = "/link/{ids}/{key}", method = RequestMethod.GET)
	public String showTasks(ModelMap model, 
			@PathVariable String ids,
			@PathVariable String key) {
		session.setTaskLinks(ids, key);
		return "links";
	}
	
	/**
	 * Needed so you can link to the same page but with Login interdiction.
	 */
	@PreAuthorize("hasRole('ROLE_USER')")
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String login(ModelMap model) {
		return "links";
	}
}
