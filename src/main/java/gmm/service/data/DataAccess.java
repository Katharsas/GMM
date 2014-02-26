package gmm.service.data;

import gmm.domain.Label;
import gmm.domain.ModelTask;
import gmm.domain.Task;
import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.util.List;
import gmm.util.Set;

public interface DataAccess {

	public <T> List<T> getList(Class<T> clazz);
	public <T> boolean addData(T data);
	public <T> boolean removeData(T data);
	public <T> boolean addAllData(List<T> data);
	public <T> void removeAllData(Class<T> clazz);
	public void saveData(Class<?> clazz);
	public <T> void loadData(Class<T> clazz);
	
	public List<User> getUsers();
	public List<Task> getGeneralTasks();
	public List<TextureTask> getTextureTasks();
	public List<ModelTask> getModelTasks();
	public Set<Label> getLabels();
}
