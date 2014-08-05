package gmm.service.tasks;

import java.io.IOException;

import gmm.collections.HashSet;
import gmm.collections.Set;
import gmm.domain.AssetTask;
import gmm.domain.GeneralTask;
import gmm.domain.ModelTask;
import gmm.domain.Task;
import gmm.domain.TextureTask;
import gmm.domain.User;
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
	@Autowired DataConfigService config;
	
	@Autowired TextureAssetCreator textureCreator;
	@Autowired ModelAssetCreator modelCreator;
	
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
			TextureTask assetTask = (TextureTask) task;
			textureCreator.editAssetTask(assetTask, form);
		}
		if (task instanceof ModelTask) {
			ModelTask assetTask = (ModelTask) task;
			modelCreator.editAssetTask(assetTask, form);
		}
		return task;
	}
	
	public <T extends Task> void editTask(T task, TaskForm form) {
		task.setName(form.getIdName());
		task.setPriority(form.getPriority());
		task.setTaskStatus(form.getStatus());
		task.setDetails(form.getDetails());
		task.setLabel(form.getLabel());
		User assigned = form.getAssigned().equals("") ? null : users.get(form.getAssigned());
		task.setAssigned(assigned);
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
	
	public <T extends AssetTask<?>> void importTasks(Iterable<String> assetPaths, TaskForm form, Class<T> type) throws IOException {
		Set<T> result = new HashSet<>();
		for(String path : assetPaths) {
			form.setOriginalAssetPath(path);
			result.add(createTask(type, form));
		}
		data.addAll(type, result);
	}
}
