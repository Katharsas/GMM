package gmm.web;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.service.Spring;
import gmm.web.forms.CommentForm;

/**
 * Renders html content from FreeMarker templates (.ftl files).
 * 
 * @author Jan Mothes
 */
@Service
public class FtlRenderer {
	
	private final Configuration config;
	
	@Autowired
	public FtlRenderer(FreeMarkerConfigurer ftlConfig) {
		this.config = ftlConfig.getConfiguration();
	}
	
	public static class TaskRenderResult {
		public final String header;
		public final String body;
		public TaskRenderResult(Task task, String header, String body) {
			this.header = header;
			this.body = body;
		}
		@Override
		public String toString() {
			return "TaskRenderResult" + "\n---------------------HEADER------------"
					+ "---------\n"+header+"\n---------------------BODY---------------------\n"+body;
		}
	}
	
	/**
	 * Request must already include all needed forms.
	 * Renders basically any template to a String.
	 * 
	 * @param fileName - Name of template file without file extension.
	 */
	public String renderTemplate(String fileName, ControllerArgs requestData) {
		
		populateModel(requestData);
		final StringWriter out = new StringWriter();
		final StringBuffer buffer = out.getBuffer();
		renderTemplate(fileName, requestData.model, out);
		return buffer.toString();
	}
	
	/**
	 * Request must already include all needed forms.
	 * Renders tasks to a list with task html for JSON auto-convertion.
	 * @see {@link #renderTask(Task, ModelMap, HttpServletRequest, HttpServletResponse)}
	 */
	public Map<Task, TaskRenderResult> renderTasks(List<? extends Task> tasks, ControllerArgs requestData) {
		
		requestData.request.setAttribute("commentForm", new CommentForm());
		populateModel(requestData);
		final Map<Task, TaskRenderResult> renderedTasks = new HashMap<>();
		
		for(final Task task : tasks) {
			final TaskRenderResult result = renderSingleTask(task, requestData.model);
			renderedTasks.put(task, result);
		}
		return renderedTasks;
	}
	
	private TaskRenderResult renderSingleTask(Task task, ModelMap model) {
		model.put("task", task);
		
		final StringWriter outH = new StringWriter();
		final StringBuffer bufferH = outH.getBuffer();
		renderTemplate("taskheader", model, outH);
		
		final StringWriter outB = new StringWriter();
		final StringBuffer bufferB = outB.getBuffer();
		renderTemplate("taskbody", model, outB);
		
		return new TaskRenderResult(task, bufferH.toString(), bufferB.toString());
	}
	
	private void populateModel(ControllerArgs requestData) {
		final ModelMap model = requestData.model;
	    final RequestContext context = new RequestContext(
				requestData.request,
				requestData.response,
				Spring.getServletContext(), null);
		model.put("request", requestData.request);
		model.put("springMacroRequestContext", context);
	}
	
	/**
	 * @param fileName - Name of template file without file extension.
	 */
	private void renderTemplate(String fileName, ModelMap model, StringWriter target) {
		try {
			final String fileExtension = ".html.ftl";
			config.getTemplate(fileName + fileExtension).process(model, target);
		} catch (final TemplateException e) {
			throw new RuntimeException(e);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					"Couldn't retrieve Freemarker template file '" + fileName + "'!", e);
		}
	}
}
