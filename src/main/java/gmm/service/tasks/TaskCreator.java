package gmm.service.tasks;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import gmm.collections.HashSet;
import gmm.collections.Set;
import gmm.domain.AssetTask;
import gmm.domain.GeneralTask;
import gmm.domain.Label;
import gmm.domain.ModelTask;
import gmm.domain.Task;
import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.service.FileService;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.web.forms.TaskForm;
import gmm.web.sessions.TaskSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskCreator {
	
	@Autowired TaskSession session;
	@Autowired UserService users;
	@Autowired DataAccess data;
	@Autowired TexturePreviewCreator creator;
	@Autowired DataConfigService config;
	@Autowired FileService fileService;
	
	public <T extends Task> T createTask(Class<T> type, TaskForm form) throws IOException {
		T task;
		if(type.equals(GeneralTask.class)) {
			task = type.cast(new GeneralTask(session.getUser()));
		}
		else if(type.equals(ModelTask.class)) {
			task = type.cast(new ModelTask(session.getUser()));
		}
		else if(type.equals(TextureTask.class)) {
			task = type.cast(new TextureTask(session.getUser()));
		}
		else {
			throw new IllegalArgumentException("Task with type \""+type.getName()+"\" not supported!");
		}
		editTask(task, form);
		if (task instanceof TextureTask) {
			AssetTask assetTask = (AssetTask) task;
			editAssetTask(assetTask, form);
		}
		return task;
	}
	
	private <T extends AssetTask> void editAssetTask(T task, TaskForm form) throws IOException {
		Path assetPath = Paths.get(form.getAssetPath());
		//check if path is below valid dirs
		assetPath = fileService.restrictAccess(assetPath, config.ASSETS_ORIGINAL);
		//check conflict with existing files
		Path newAssetPathConflict = config.ASSETS_NEW.resolve(assetPath);
		if(newAssetPathConflict.toFile().exists()) {
			throw new IllegalArgumentException("Asset path \""+assetPath+"\" is invalid. Path points to an existing file or directory!");
		}
		//substitute wildcards
		task.setName(task.getName().replace("%filename%", assetPath.getFileName().toString()));
		task.setDetails(task.getDetails().replace("%filename%", assetPath.getFileName().toString()));
		task.setOriginalAsset(assetPath);
		task.setNewAssetFolder(assetPath);
		creator.createPreview(config.ASSETS_ORIGINAL.resolve(assetPath), task, true);
	}
	
	public <T extends Task> void editTask(T task, TaskForm form) {
		task.setName(form.getIdName());
		task.setPriority(form.getPriority());
		task.setTaskStatus(form.getStatus());
		task.setDetails(form.getDetails());
		task.setLabel(form.getLabel());
		User assigned = form.getAssigned().equals("") ? null : users.get(form.getAssigned());
		task.setAssigned(assigned);
		String label = form.getLabel();
		if(!label.equals("")) {
			data.add(new Label(label));
		}
	}
	
	public <T extends Task> TaskForm prepareForm(T task) {
		TaskForm form = new TaskForm();
		form.setIdName(task.getName());
		form.setDetails(task.getDetails());
		form.setLabel(task.getLabel());
		form.setPriority(task.getPriority());
		form.setStatus(task.getTaskStatus());
		form.setAssigned(task.getAssigned());
		return form;
	}
	
	public <T extends AssetTask> void importTasks(Iterable<String> assetPaths, TaskForm form, Class<T> type) throws IOException {
		Set<T> result = new HashSet<>();
		for(String path : assetPaths) {
			form.setAssetPath(path);
			result.add(createTask(type, form));
		}
		data.addAll(type, result);
	}
}
