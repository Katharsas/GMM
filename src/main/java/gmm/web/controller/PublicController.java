package gmm.web.controller;


import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import gmm.collections.Collection;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.Task;
import gmm.domain.UniqueObject;
import gmm.service.data.DataAccess;
import gmm.web.AjaxResponseException;
import gmm.web.TaskRenderer;
import gmm.web.TaskRenderer.TaskRenderResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("public")
@Controller
public class PublicController {
	
	@Autowired private DataAccess data;
	@Autowired private TaskRenderer ftlTaskRenderer;
	
	@RequestMapping(value = "/linkTasks/render/{ids}", method = RequestMethod.GET)
	@ResponseBody
	public List<TaskRenderResult> renderTasks(
			ModelMap model, 
			@PathVariable String ids,
			HttpServletRequest request,
			HttpServletResponse response) throws AjaxResponseException {
		try {
			String[] idArray = ids.split(",");
			Collection<Task> allTasks = data.getList(Task.class);
			LinkedList<Task> tasks = new LinkedList<>();
			for (String id : Arrays.asList(idArray)) {
				Task task = UniqueObject.getFromId(allTasks, Long.parseLong(id));
				if (task != null) tasks.add(task);
			}
			return ftlTaskRenderer.renderTasks(tasks, model, request, response);
		} catch(Exception e) {
			throw new AjaxResponseException(e);
		}
	}
	
	@RequestMapping(value = "/linkTasks/{ids}", method = RequestMethod.GET)
	public String showTasks(ModelMap model, 
			@PathVariable String ids) {
		return "links";
	}
}
