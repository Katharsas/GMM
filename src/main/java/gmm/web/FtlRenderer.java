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
import gmm.domain.Label;
import gmm.domain.task.Task;
import gmm.service.Spring;
import gmm.service.UserService;
import gmm.service.data.DataAccess;

/**
 * Renders html content from FreeMarker templates (.ftl files).
 * 
 * @author Jan Mothes
 */
@Service
public class FtlRenderer {
	
	@Autowired private UserService users;
	@Autowired private DataAccess data;
	
	private Configuration config;
	
	@Autowired
	public FtlRenderer(FreeMarkerConfigurer ftlConfig) throws IOException {
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
	 * Renders basically any template to a String.
	 */
	public String renderTemplate(ModelMap model, String template,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		populateModel(model, request, response);
		StringWriter out = new StringWriter();
		StringBuffer buffer = out.getBuffer();
		config.getTemplate(template).process(model, out);
		return buffer.toString();
	}
	
	/**
	 * Request must already include all needed forms.
	 * Renders task to 2 html strings: taskheader and taskbody
	 * @return WrapperObject for task html
	 */
	public TaskRenderResult renderTask(Task task, ModelMap model,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		populateModel(model, request, response);
		return renderSingleTask(task, model);
	}
	
	/**
	 * Request must already include all needed forms.
	 * Renders tasks to a list with task html for JSON auto-convertion.
	 * @see {@link #renderTask(Task, ModelMap, HttpServletRequest, HttpServletResponse)}
	 */
	public List<TaskRenderResult> renderTasks(List<? extends Task> tasks, ModelMap model,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		populateModel(model, request, response);
		List<TaskRenderResult> renderedTasks = new LinkedList<>(TaskRenderResult.class);
		
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
			HttpServletResponse response) throws IOException {
		
		boolean isUserLoggedIn = users.isUserLoggedIn();
		model.addAttribute("isUserLoggedIn", isUserLoggedIn);
	    if (isUserLoggedIn) {
	    	model.addAttribute("principal", users.getLoggedInUser());
	    	model.addAttribute("users", users.get());
			model.addAttribute("taskLabels", data.getList(Label.class));
	    }
		model.put("request", request);
		model.put("springMacroRequestContext",
				new RequestContext(request, response, Spring.getServletContext(), null));
	}
}
