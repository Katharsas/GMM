package gmm.web;

import java.util.HashMap;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import gmm.web.forms.Form;

/**
 * This class allows controllers to manage ftl template dependencies in a more abstract way.
 * This includes form binding as well as variables access using the model. Single dependencies
 * (forms or variables) can be bound to multiple templates.
 * 
 * Forms will be added to request attributes, model variables to model attributes.
 * 
 * @author Jan Mothes
 */
@Service
@Scope("prototype")
public class FtlTemplateService {
	
	private final FtlRenderer ftlRenderer;
	
	@Autowired
	public FtlTemplateService(FtlRenderer ftlRenderer) {
		this.ftlRenderer = ftlRenderer;
	}
	
	private final HashMap<String, Supplier<Object>> modelSuppliers = new HashMap<>();
	private final HashMap<String, Supplier<Form>> formSuppliers = new HashMap<>();
	
	private final HashMap<String, String[]> ftlToAny = new HashMap<>();
	
	/**
	 * Any model variable that is needed inside registered ftl template must be registered here.
	 * @see {@link #registerFtl(String, String...)}
	 */
	public void registerVariable(String name, Supplier<Object> supplier) {
		modelSuppliers.put(name, supplier);
	}
	
	/**
	 * A form that a registered ftl template binds to (using spring.ftl) must be registered here.
	 * @see {@link #registerFtl(String, String...)}
	 */
	public void registerForm(String name, Supplier<Form> supplier) {
		formSuppliers.put(name, supplier);
	}
	
	/**
	 * @param fileName - The filename of the template without extension.
	 * @param dependencies - Any forms or variables that may have been registered. If
	 */
	public void registerFtl(String fileName, String... dependencies) {
		ftlToAny.put(fileName, dependencies);
	}
	
	/**
	 * Renders the given Freemaker template to a string and inserts that into into given model to
	 * provide access to the rendered html in jsp files.
	 * @param templateFile - The filename of the template without extension.
	 * @param requestData - String will be added to this model, so you can insert the template in
	 * 		jsp code just like any other model attribute by the given template name.
	 * @return The rendered template as string.
	 */
	public String insertFtl(String templateFile, ControllerArgs requestData) {
		for (final String dependency : ftlToAny.get(templateFile)) {
			// populate model
			final Supplier<Object> var = modelSuppliers.get(dependency);
			if (var != null) {
				requestData.model.addAttribute(dependency, var.get());
			}
			// populate request
			final Supplier<Form> form = formSuppliers.get(dependency);
			if (form != null) {
				requestData.request.setAttribute(dependency, form.get());
			}
			if (var == null && form == null) {
				throw new IllegalStateException("Dependency '" + dependency + "' could not be found"
						+ " for ftl template '" + templateFile + "' ! ");
			}
		}
		// render and insert into model
		final String result = ftlRenderer.renderTemplate(templateFile, requestData);
		requestData.model.addAttribute(templateFile, result);
		// cleanup request attributes
		for(final String any : ftlToAny.get(templateFile)) {
			requestData.request.removeAttribute(any);
		}
		return result;
	}
}
