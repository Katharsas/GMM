package gmm.service.tasks;

import java.io.IOException;
import java.util.List;

import gmm.collections.HashSet;
import gmm.collections.Set;
import gmm.domain.Asset;
import gmm.domain.AssetTask;
import gmm.domain.Task;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.web.forms.TaskForm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceFinder {
	
	@Autowired private UserService users;
	@Autowired private DataAccess data;
	@Autowired private List<TaskService<?>> taskServices;
	
	private TaskService<?> currentService = null;
	private Class<?> currentType = null;
	
	public <E extends Asset, T extends AssetTask<E>> void register(AssetTaskService<E,T> creator) {
		taskServices.add(creator);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Task> TaskService<T> getService(Class<T> type) {
		if (!type.equals(currentType)) {
			currentType = type;
			for (TaskService<?> service : taskServices) {
				if (service.getTaskType().equals(type)) {
					currentService = service;
				}
			}
			if (currentService == null) throw new IllegalStateException("No service registered for task of type "+type.getName());
		}
		return (TaskService<T>) currentService;
	}
	
	public <T extends Task> T create(Class<T> type, TaskForm form) throws IOException {
		final T task = getService(type).create(form);
		edit(task, form);
		return task;
	}
	
	public <T extends Task, E extends T> void edit(T task, TaskForm form) {
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) task.getClass();
		final TaskService<T> taskService = getService(clazz);
		taskService.edit(task, form);
	}
	
	public <T extends Task> TaskForm prepareForm(T task) {
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) task.getClass();
		final TaskService<T> taskService = getService(clazz);
		return taskService.prepareForm(task);
	}
	
	public <T extends AssetTask<?>> void importTasks(Iterable<String> assetPaths, TaskForm form, Class<T> type) throws IOException {
		Set<T> result = new HashSet<>();
		for(String path : assetPaths) {
			form.setAssetPath(path);
			//TODO only createTask (inlcuding preview generation), if no conflict (messageOperations)
			//TODO move createTask call to messageOperations
			result.add(this.<T>create(type, form));
		}
		data.addAll(type, result);
	}
	
	public <T extends Task> TaskService<T> getTaskService(Class<T> type) {
		return getService(type);
	}
}
