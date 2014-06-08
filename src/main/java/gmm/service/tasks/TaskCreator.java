package gmm.service.tasks;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import gmm.domain.AssetTask;
import gmm.domain.GeneralTask;
import gmm.domain.Label;
import gmm.domain.ModelTask;
import gmm.domain.Task;
import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.service.UserService;
import gmm.service.data.DataAccess;
import gmm.service.data.DataConfigService;
import gmm.util.HashSet;
import gmm.util.Set;
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
		task = editTask(task, form);
		if (task instanceof TextureTask) {
			AssetTask assetTask = (AssetTask) task;
			Path assetPath = Paths.get(form.getAssetPath());
			Path original = Paths.get(config.ASSETS_ORIGINAL);
			Path absolute = original.resolve(assetPath);
			if(absolute.toFile().isDirectory()) {
				throw new IllegalArgumentException("Asset path \""+assetPath+"\" is invalid. Path points to an existing directory!");
			}
			task.setName(task.getName().replace("%filename%", assetPath.getFileName().toString()));
			task.setDetails(task.getDetails().replace("%filename%", assetPath.getFileName().toString()));
			assetTask.setOriginalAsset(assetPath);
			assetTask.setNewAssetFolder(assetPath);
			creator.createPreview(absolute, assetTask, true);
		}
		return task;
	}
	
	public <T extends Task> T editTask(T task, TaskForm form) {
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
		return task;
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
