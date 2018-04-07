package gmm.web.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gmm.collections.List;
import gmm.domain.task.TaskType;
import gmm.service.data.DataAccess;
import gmm.service.data.backup.ManualBackupService;
import gmm.service.users.CurrentUser;
import gmm.web.ControllerArgs;
import gmm.web.FtlTemplateService;
import gmm.web.forms.FilterForm;
import gmm.web.forms.LoadForm;
import gmm.web.forms.SearchForm;
import gmm.web.forms.SortForm;
import gmm.web.sessions.tasklist.TaskListEvent;
import gmm.web.sessions.tasklist.WorkbenchSession;

@RequestMapping(value = "workbench")
@ResponseBody
@PreAuthorize("hasRole('ROLE_USER')")
@Controller
public class WorkbenchController {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final WorkbenchSession workbench;
	private final DataAccess data;
	private final ManualBackupService manualBackups;
	private final FtlTemplateService templates;
	
	private final CurrentUser user;
	
	// names used to reference a form template
	private final String filterFormName = "workbench-generalFilterForm";
	private final String searchFormName = "workbench-searchForm";
	
	// template file name (without file ending)
	private final String filterFormTemplate = "workbench_filters";
	private final String searchFormTemplate = "workbench_search";
	
	@Autowired
	public WorkbenchController(WorkbenchSession workbench, DataAccess data,
			ManualBackupService manualBackups, CurrentUser user, FtlTemplateService templates) {
		
		this.workbench = workbench;
		this.data = data;
		this.manualBackups = manualBackups;
		this.templates = templates;
		
		this.user = user;
		
		templates.registerForm(filterFormName, workbench::getFilterForm);
		templates.registerForm(searchFormName, workbench::getSearchForm);
		
		templates.registerFtl(filterFormTemplate, filterFormName);
		templates.registerFtl(searchFormTemplate, searchFormName);
	}
	
	/**
	 * Workbench Admin Tab <br>
	 * -----------------------------------------------------------------
	 */
	
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@RequestMapping(value = "saveVisible", method = POST)
	public void saveVisible(@RequestParam("name") String pathString) throws IOException {
		manualBackups.saveTasksToXml(workbench.getTasks(), pathString);
	}
	
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@RequestMapping(value = "deleteVisible", method = POST)
	@ResponseBody
	public void deleteVisible() {
		data.removeAll(workbench.getTasks());
	}
	
	/**
	 * Workbench Load  <br>
	 * -----------------------------------------------------------------
	 * @param type - type whose corresponding button was clicked by user
	 */
	@RequestMapping(value = "loadType", method = POST)
	public void loadType(@RequestParam("type") TaskType type) {
		workbench.loadTasks(type);
	}
	
	/**
	 * Changes settings for task loading and default workbench loading on login
	 * @param loadForm - object containing all task loading settings
	 */
	@RequestMapping(value = "loadOptions", method = POST)
	public void loadOptions(@ModelAttribute("workbench-loadForm") LoadForm loadForm) {
		workbench.updateLoad(loadForm);
	}
	
	/**
	 * Returns what types should be visible based on what method / buttons the user clicked.
	 * @return True for the selected/active task types, false for the others. Array element
	 * 		positions correspond to {@link TaskType#values()}.
	 */
	@RequestMapping(value = "selected", method = GET)
	public boolean[] selected() {
		return workbench.getSelectedTaskTypes();
	}
	
	/**
	 * Filter <br>
	 * -----------------------------------------------------------------
	 * @param filterForm - object containing all filter information
	 * @param reset - true if user clicked the reset filter button (discard filterForm)
	 */
	@RequestMapping(value = "filter", method = { GET, POST }, produces = "application/json")
	public Map<String, String> filter(
			ControllerArgs args,
			@ModelAttribute(filterFormName) FilterForm filterForm,
			@RequestParam(value = "reset", required = false, defaultValue = "false") boolean reset) {
		
		if(args.getRequestMethod().equals(POST)) {
			workbench.updateFilter(reset ? new FilterForm() : filterForm);
		}
		
		final Map<String, String> answer = new HashMap<>();
		answer.put("isInDefaultState", "" + workbench.getFilterForm().isInDefaultState());
		if (reset || args.getRequestMethod().equals(GET)) {
			answer.put("html", templates.insertFtl(filterFormTemplate, args));
		}
		return answer;
	}
	
	/**
	 * Workbench Search <br>
	 * -----------------------------------------------------------------
	 * Search is always applied the tasks found by the last filter operation.
	 * @param searchForm - object containing all search information
	 */
	@RequestMapping(value = "search",  method = { GET, POST }, produces = "application/json")
	public Map<String, String> search(
			ControllerArgs args,
			@ModelAttribute(searchFormName) SearchForm searchForm,
			@RequestParam(value = "reset", required = false, defaultValue = "false") boolean reset) {
		
		if(args.getRequestMethod().equals(POST)) {
			workbench.updateSearch(reset ? new SearchForm() : searchForm);
		}
		
		final Map<String, String> answer = new HashMap<>();
		answer.put("isInDefaultState", "" + workbench.getSearchForm().isInDefaultState());
		if (reset || args.getRequestMethod().equals(GET)) {
			answer.put("html", templates.insertFtl(searchFormTemplate, args));
		}
		return answer;
	}
	
	/**
	 * Workbench Sort <br>
	 * -----------------------------------------------------------------
	 * Sort is always applied on currently shown tasks.
	 * @param sortForm - object containing all sort information
	 */
	@RequestMapping(value = "sort", method = POST)
	public void sort(@ModelAttribute("workbench-sortForm") SortForm sortForm) {
		workbench.updateSort(sortForm);
	}
	
	/**
	 * Workbench Events <br>
	 * -----------------------------------------------------------------
	 * Sync workbench taskList by exchanging taskList events.
	 */
	@RequestMapping(value = "taskListEvents", method = GET)
	public List<TaskListEvent> taskListEvents() {
		final List<TaskListEvent> events = workbench.retrieveEvents();
		if (logger.isDebugEnabled()) {
			logger.debug(user.get() + " retrieved events: " + Arrays.toString(events.toArray()));
		}
		return events;
	}
}
