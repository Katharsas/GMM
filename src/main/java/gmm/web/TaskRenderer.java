package gmm.web;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import freemarker.template.Configuration;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.Task;
import gmm.service.Spring;
import gmm.service.UserService;

/**
 * @author Jan Mothes
 */
@Service
public class TaskRenderer {
	
	@Autowired private UserService users;
	
	private Configuration config;
	
	@Autowired
	public TaskRenderer(FreeMarkerConfigurer ftlConfig) throws IOException {
		config = ftlConfig.getConfiguration();
	}
	
	public static class TaskRenderResult {
		public String idLink;
		public String header;
		public String body;
		public TaskRenderResult(Task task, String header, String body) {
			this.idLink = task.getIdLink();
			this.header = header;
			this.body = body;
		}
		@Override
		public String toString() {
			return "TaskRenderResult\n  IDLink: "+idLink+"\n---------------------HEADER------------"
					+ "---------\n"+header+"\n---------------------BODY---------------------\n"+body;
		}
	}
	
	/**
	 * Request must already include all needed forms.
	 */
	public TaskRenderResult renderTask(Task task, ModelMap model,
			HttpServletRequest request, HttpServletResponse response, boolean isEditable) throws Exception {
		
		populateModel(model, request, response, isEditable);
		return renderSingleTask(task, model);
	}
	
	/**
	 * Request must already include all needed forms.
	 */
	public List<TaskRenderResult> renderTasks(List<? extends Task> tasks, ModelMap model,
			HttpServletRequest request, HttpServletResponse response, boolean isEditable) throws Exception {
		
		populateModel(model, request, response, isEditable);
		List<TaskRenderResult> renderedTasks = new LinkedList<>();
		
		for(Task task : tasks) {
			TaskRenderResult result = renderSingleTask(task, model);
			renderedTasks.add(result);
		}
		return renderedTasks;
	}
	
	private TaskRenderResult renderSingleTask(Task task, ModelMap model) throws Exception {
		model.put("task", task);
		
		StringWriter outH = new StringWriter();
		StringBuffer bufferH = outH.getBuffer();
		config.getTemplate("taskheader.ftl").process(model, outH);
		
		StringWriter outB = new StringWriter();
		StringBuffer bufferB = outB.getBuffer();
		config.getTemplate("taskbody.ftl").process(model, outB);
		
		return new TaskRenderResult(task, bufferH.toString(), bufferB.toString());
	}
	
	private void populateModel(ModelMap model, HttpServletRequest request,
			HttpServletResponse response, boolean isEditable) throws IOException {
		
		boolean isUserLoggedIn = users.isUserLoggedIn();
		boolean isTaskEditable = isUserLoggedIn && isEditable;
		
		model.addAttribute("isUserLoggedIn", isUserLoggedIn);
		model.addAttribute("isTaskEditable", isTaskEditable);
	    if (isUserLoggedIn) {
	    	model.addAttribute("principal", users.getLoggedInUser());
	    }
		model.put("request", request);
		model.put("springMacroRequestContext",
				new RequestContext(request, response, Spring.getServletContext(), null));
	}
}
