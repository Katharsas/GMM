package gmm.service.tasks;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.domain.task.asset.Asset;
import gmm.domain.task.asset.AssetTask;
import gmm.util.Util;
import gmm.web.forms.TaskForm;

@Service
public class TaskServiceFinder {
	
	@Autowired private List<TaskFormService<?>> taskServices;
	
	final private Map<Class<? extends Task>, TaskFormService<?>> classesToServices = new HashMap<>();
	
	@PostConstruct
	private void init() {
		for(final TaskFormService<?> service : taskServices) {
			classesToServices.put(service.getTaskType(), service);
		}
	}
	
	private <T extends Task> TaskFormService<T> getService(Class<? extends T> type) {
		@SuppressWarnings("unchecked")
		final
		TaskFormService<T> result = (TaskFormService<T>) classesToServices.get(type);
		if (result == null) {
			throw new IllegalStateException("No service registered for task of type "+type.getName());
		}
		return result;
	}
	
	public <T extends Task> T create(Class<T> type, TaskForm form, User user) {
		final T task = getService(type).create(form, user);
		return task;
	}
	
	public <T extends Task, E extends T> void edit(T task, TaskForm form) {
		final TaskFormService<T> taskService = getService(Util.classOf(task));
		taskService.edit(task, form);
	}
	
	public <T extends Task> TaskForm prepareForm(T task) {
		final TaskFormService<T> taskService = getService(Util.getClass(task));
		return taskService.prepareForm(task);
	}
	
	public <E extends Asset> AssetTaskService<E> getAssetService(Class<AssetTask<E>> type) {
		final AssetTaskService<E> taskService = (AssetTaskService<E>) getService(type);
		return taskService;
	}
	
	public <E extends Asset> void addFile(AssetTask<E> task, MultipartFile file) {
		final AssetTaskService<E> taskService = getAssetService(Util.classOf(task));
		taskService.addFile(file, task);
	}
	
	public <E extends Asset> void deleteFile(AssetTask<E> task, Path relativeFile, boolean isAsset) {
		final AssetTaskService<E> taskService = getAssetService(Util.classOf(task));
		taskService.deleteFile(task, relativeFile, isAsset);
	}
}
