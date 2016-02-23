package gmm.web;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.service.Spring;
import gmm.service.UserService;
import gmm.web.forms.CommentForm;

/**
 * Renders html content from FreeMarker templates (.ftl files).
 * 
 * @author Jan Mothes
 */
@Service
public class FtlRenderer {
	
	@Autowired private UserService users;
	
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
	
	public static class RequestData {
		public final ModelMap model;
		public final HttpServletRequest request;
		public final HttpServletResponse response;
		
		public RequestData(ModelMap model,
				HttpServletRequest request,
				HttpServletResponse response) {
			this.model = model;
			this.request = request;
			this.response = response;
		}
	}
	
	/**
	 * Request must already include all needed forms.
	 * Renders basically any template to a String.
	 */
	public String renderTemplate(String fileName, RequestData requestData) {
		
		populateModel(requestData);
		StringWriter out = new StringWriter();
		StringBuffer buffer = out.getBuffer();
		renderTemplate(fileName, requestData.model, out);
		return buffer.toString();
	}
	
	/**
	 * Request must already include all needed forms.
	 * Renders task to 2 html strings: taskheader and taskbody
	 * @return WrapperObject for task html
	 */
	public TaskRenderResult renderTask(Task task, RequestData requestData) {
		
		populateModel(requestData);
		return renderSingleTask(task, requestData.model);
	}
	
	/**
	 * Request must already include all needed forms.
	 * Renders tasks to a list with task html for JSON auto-convertion.
	 * @see {@link #renderTask(Task, ModelMap, HttpServletRequest, HttpServletResponse)}
	 */
	public List<TaskRenderResult> renderTasks(List<? extends Task> tasks, RequestData requestData) {
		
		populateModel(requestData);
		List<TaskRenderResult> renderedTasks = new LinkedList<>(TaskRenderResult.class);
		
		for(Task task : tasks) {
			TaskRenderResult result = renderSingleTask(task, requestData.model);
			renderedTasks.add(result);
		}
		return renderedTasks;
	}
	
	private TaskRenderResult renderSingleTask(Task task, ModelMap model) {
		model.put("task", task);
		
		StringWriter outH = new StringWriter();
		StringBuffer bufferH = outH.getBuffer();
		renderTemplate("taskheader.ftl", model, outH);
		
		StringWriter outB = new StringWriter();
		StringBuffer bufferB = outB.getBuffer();
		renderTemplate("taskbody.ftl", model, outB);
		
		return new TaskRenderResult(task, bufferH.toString(), bufferB.toString());
	}
	
	private void populateModel(RequestData requestData) {
		// model
		ModelMap model = requestData.model;
		boolean isUserLoggedIn = users.isUserLoggedIn();
		model.addAttribute("isUserLoggedIn", isUserLoggedIn);
	    if (isUserLoggedIn) {
	    	model.addAttribute("principal", users.getLoggedInUser());
	    }
	    RequestContext context = new RequestContext(
				requestData.request,
				requestData.response,
				Spring.getServletContext(), null);
		model.put("request", requestData.request);
		model.put("springMacroRequestContext", context);
		// forms that tasks bind to
		requestData.request.setAttribute("commentForm", new CommentForm());
	}
	
	private void renderTemplate(String fileName, ModelMap model, StringWriter target) {
		try {
			config.getTemplate(fileName).process(model, target);
		} catch (TemplateException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(
					"Couldn't retrieve Freemarker template file '" + fileName + "'!", e);
		}
	}
}
