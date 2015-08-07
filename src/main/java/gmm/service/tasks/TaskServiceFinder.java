package gmm.service.tasks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import gmm.domain.task.Asset;
import gmm.domain.task.AssetTask;
import gmm.domain.task.Task;
import gmm.web.forms.TaskForm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TaskServiceFinder {
	
	@Autowired private List<TaskFormService<?>> taskServices;
	
	private TaskFormService<?> currentService = null;
	private Class<?> currentType = null;
	
	public <E extends Asset, T extends AssetTask<E>> void register(AssetTaskService<E,T> creator) {
		taskServices.add(creator);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Task> TaskFormService<T> getService(Class<T> type) {
		if (!type.equals(currentType)) {
			currentType = type;
			for (TaskFormService<?> service : taskServices) {
				if (service.getTaskType().equals(type)) {
					currentService = service;
				}
			}
			if (currentService == null) throw new IllegalStateException("No service registered for task of type "+type.getName());
		}
		return (TaskFormService<T>) currentService;
	}
	
	public <T extends Task> T create(Class<T> type, TaskForm form) throws Exception {
		final T task = getService(type).create(form);
		return task;
	}
	
	public <T extends Task, E extends T> void edit(T task, TaskForm form) throws IOException {
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) task.getClass();
		final TaskFormService<T> taskService = getService(clazz);
		taskService.edit(task, form);
	}
	
	public <T extends Task> TaskForm prepareForm(T task) {
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) task.getClass();
		final TaskFormService<T> taskService = getService(clazz);
		return taskService.prepareForm(task);
	}
	
	public <T extends Task> TaskFormService<T> getTaskService(Class<T> type) {
		return getService(type);
	}
	
	public <E extends Asset, T extends AssetTask<E>> AssetTaskService<E, T> getAssetService(Class<T> type) {
		@SuppressWarnings("unchecked")
		AssetTaskService<E, T> taskService = (AssetTaskService<E, T>) getService(type);
		return taskService;
	}
	
	public <E extends Asset, T extends AssetTask<E>> void addFile(T task, MultipartFile file) throws IOException {
		@SuppressWarnings("unchecked")
		AssetTaskService<E, T> taskService = (AssetTaskService<E, T>) getAssetService(task.getClass());
		taskService.addFile(file, task);
	}
	
	public <E extends Asset, T extends AssetTask<E>> void deleteFile(T task, Path relativeFile, boolean isAsset) throws IOException {
		@SuppressWarnings("unchecked")
		AssetTaskService<E, T> taskService = (AssetTaskService<E, T>) getAssetService(task.getClass());
		taskService.deleteFile(task, relativeFile, isAsset);
	}
}
