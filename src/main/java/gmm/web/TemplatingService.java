package gmm.web;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import gmm.web.forms.Form;

@Service
@Scope("prototype")
public class TemplatingService {
	
	private final FtlRenderer ftlRenderer;
	
	@Autowired
	public TemplatingService(FtlRenderer ftlRenderer) {
		this.ftlRenderer = ftlRenderer;
	}
	
	/**
	 * Forms that are used by EITHER jsp OR ftl must be in here.
	 * Any form-receiving method needs this to get a form instance for filling request data in.
	 */
	private final HashMap<String, Supplier<Form>> modelSuppliers = new HashMap<>();
	
	/**
	 * Includes all ftl templates and the form bindings they need to access.
	 * Key is filename of template, value is keys from modelSuppliers.
	 */
	private final HashMap<String, String[]> ftlTemplates = new HashMap<>();
	
	
	public void registerForm(String name, Supplier<Form> formCreator) {
		modelSuppliers.put(name, formCreator);
	}
	
	public void registerFtl(String fileName, String[] formsNeeded) {
		ftlTemplates.put(fileName, formsNeeded);
	}
	
	public void populateModelWithForms(Model model) {
		for(final Entry<String, Supplier<Form>> entry : modelSuppliers.entrySet()) {
			model.addAttribute(entry.getKey(), entry.getValue().get());
		}
	}
	
	/**
	 * Renders the given Freemaker template to a string and inserts that into into given model to
	 * provide access to the rendered html in jsp files.
	 * @param templateFile - The filename of the template without extension.
	 * @param requestData - string will be added to this model, so you can insert the template in
	 * 		jsp code just like any other model attribute by the given template name
	 */
	public String insert(String templateFile, ControllerArgs requestData) {
		// populate request
		for(final String form : ftlTemplates.get(templateFile)) {
			requestData.request.setAttribute(form, modelSuppliers.get(form).get());
		}
		// render and insert into model
		final String result = ftlRenderer.renderTemplate(templateFile, requestData);
		requestData.model.addAttribute(templateFile, result);
		// cleanup
		for(final String form : ftlTemplates.get(templateFile)) {
			requestData.request.removeAttribute(form);
		}
		return result;
	}
}
