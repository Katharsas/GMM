package gmm.service.tasks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.domain.task.asset.AssetKey;
import gmm.domain.task.asset.AssetProperties;
import gmm.domain.task.asset.AssetTask;
import gmm.service.FileService.FileExtensionFilter;
import gmm.util.Util;
import gmm.web.forms.TaskForm;

@Service
public class TaskServiceFinder {
	
	private final List<TaskFormService<?>> taskServices;
	private final List<AssetTaskService<?>> assetTaskServices;
	
	final private Map<Class<? extends Task>, TaskFormService<?>> classesToServices = new HashMap<>();
	
	final private Map<String, AssetTaskService<?>> extensionToServices = new CaseInsensitiveMap<>();
	
	final private FileExtensionFilter combinedExtensionFilter;
	
	@Autowired
	public TaskServiceFinder(
			List<TaskFormService<?>> taskServices, List<AssetTaskService<?>> assetTaskServices) {
		this.taskServices = taskServices;
		this.assetTaskServices = assetTaskServices;
		
		initMaps();
		String[] combinedExtension = new String[] {};
		for (final AssetTaskService<?> service : assetTaskServices) {
			combinedExtension = ArrayUtils.addAll(combinedExtension, service.getExtensions());
		}
		combinedExtensionFilter = new FileExtensionFilter(combinedExtension);
	}
	
	private void initMaps() {
		for(final TaskFormService<?> service : taskServices) {
			classesToServices.put(service.getTaskType().toClass(), service);
			if (service instanceof AssetTaskService<?>) {
				final AssetTaskService<?> assetTaskService = (AssetTaskService<?>) service;
				for (final String extension : assetTaskService.getExtensions()) {
					extensionToServices.put(extension, assetTaskService);
				}
			}
		}
	}
	
	private <T extends Task> TaskFormService<T> getService(Class<? extends T> type) {
		@SuppressWarnings("unchecked")
		final TaskFormService<T> result = (TaskFormService<T>) classesToServices.get(type);
		if (result == null) {
			throw new IllegalStateException("No service registered for task of type "+type.getName());
		}
		return result;
	}
	
	public Task create(TaskForm form, User user) {
		final Task task = getService(form.getType().toClass()).create(form, user);
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
	
	public <E extends AssetProperties> AssetTaskService<E> getAssetService(Class<? extends AssetTask<E>> type) {
		@SuppressWarnings("unchecked")
		final AssetTaskService<E> taskService = (AssetTaskService<E>) getService(type);
		return taskService;
	}
	
	/**
	 * @return A service whose {@link AssetTaskService#getExtensionFilter()} method will return a 
	 * filter that will accept the given assetName, or null if assetName does not match a supported extension.
	 */
	public AssetTaskService<?> tryGetAssetService(AssetKey assetName) {
		Objects.requireNonNull(assetName);
		final String extension = FileExtensionFilter.getExtension(assetName.toString());
		if (extension == null) {
			throw new IllegalArgumentException("Asset name '" + assetName + "' does not have an extension!");
		}
		return getAssetService(extension);
	}
	
	/**
	 * @return A service whose {@link AssetTaskService#getExtensionFilter()} method will return a 
	 * filter that will accept the given assetName.
	 */
	public AssetTaskService<?> getAssetService(AssetKey assetName) {
		final AssetTaskService<?> service = tryGetAssetService(assetName);
		if (service == null) {
			throw new IllegalStateException("No asset service registered for asset name '" + assetName + "'!");
		}
		return service;
	}
	
	/**
	 * @return A service whose {@link AssetTaskService#getExtensionFilter()} method will return a
	 * filter that will accept a fileName for which {@link FileExtensionFilter#getExtension(String)}
	 * produced the given argument, or null if no service matches the extension.
	 */
	public AssetTaskService<?> getAssetService(String extension) {
		Objects.requireNonNull(extension);
		return extensionToServices.get(extension);
	}
	
	public List<? extends AssetTaskService<?>> getAssetTaskServices() {
		return assetTaskServices;
	}
	
	public FileExtensionFilter getCombinedExtensionFilter() {
		return combinedExtensionFilter;
	}
	
//	public <E extends AssetProperties> void addFile(AssetTask<E> task, MultipartFile file) {
//		final AssetTaskService<E> taskService = getAssetService(Util.classOf(task));
//		taskService.addFile(file, task);
//	}
//	
//	public <E extends AssetProperties> void deleteFile(AssetTask<E> task, Path relativeFile, boolean isAsset) {
//		final AssetTaskService<E> taskService = getAssetService(Util.classOf(task));
//		taskService.deleteFile(task, relativeFile, isAsset);
//	}
}
