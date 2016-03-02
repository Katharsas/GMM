package gmm.service.tasks;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import gmm.domain.task.Task;
import gmm.domain.task.asset.Asset;
import gmm.domain.task.asset.AssetTask;
import gmm.util.Util;
import gmm.web.forms.TaskForm;

@Service
public class TaskServiceFinder {
	
	@Autowired private List<TaskFormService<?>> taskServices;
	
	private TaskFormService<?> currentService = null;
	private Class<?> currentType = null;
	
	public <E extends Asset, T extends AssetTask<E>> void register(AssetTaskService<E> creator) {
		taskServices.add(creator);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Task> TaskFormService<T> getService(Class<? extends T> type) {
		if (!type.equals(currentType)) {
			currentType = type;
			for (final TaskFormService<?> service : taskServices) {
				if (service.getTaskType().equals(type)) {
					currentService = service;
				}
			}
			if (currentService == null) throw new IllegalStateException("No service registered for task of type "+type.getName());
		}
		return (TaskFormService<T>) currentService;
	}
	
	public <T extends Task> T create(Class<T> type, TaskForm form) {
		final T task = getService(type).create(form);
		return task;
	}
	
	public <T extends Task, E extends T> void edit(T task, TaskForm form) {
		final Class<T> clazz = Util.classOf(task);
		final TaskFormService<T> taskService = getService(clazz);
		taskService.edit(task, form);
	}
	
	public <T extends Task> TaskForm prepareForm(T task) {
		final Class<? extends T> clazz =  Util.getClass(task);
		final TaskFormService<T> taskService = getService(clazz);
		return taskService.prepareForm(task);
	}
	
	public <T extends Task> TaskFormService<T> getTaskService(Class<T> type) {
		return getService(type);
	}
	
	public <E extends Asset> AssetTaskService<E> getAssetService(Class<? extends AssetTask<E>> type) {
		@SuppressWarnings("unchecked")
		final AssetTaskService<E> taskService = (AssetTaskService<E>) getService(type);
		return taskService;
	}
	
	public <E extends Asset> void addFile(AssetTask<E> task, MultipartFile file) {
		final AssetTaskService<E> taskService = getAssetService(Util.getClass(task));
		taskService.addFile(file, task);
	}
	
	public <E extends Asset> void deleteFile(AssetTask<E> task, Path relativeFile, boolean isAsset) {
		final AssetTaskService<E> taskService = getAssetService(Util.getClass(task));
		taskService.deleteFile(task, relativeFile, isAsset);
	}
}
