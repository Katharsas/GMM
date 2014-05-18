package gmm.service.data;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.GeneralTask;
import gmm.domain.Label;
import gmm.domain.Linkable;
import gmm.domain.ModelTask;
import gmm.domain.Task;
import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.service.UserService;
import gmm.util.LinkedList;
import gmm.util.List;
import gmm.util.HashSet;
import gmm.util.Set;
import gmm.util.Collection;

@Service
public class DataBase implements DataAccess {

	@Autowired UserService userService;
	
	final private List<User> users = new LinkedList<User>();
	final private Set<GeneralTask> generalTasks = new HashSet<GeneralTask>();
	final private Set<TextureTask> textureTasks = new HashSet<TextureTask>();
	final private Set<ModelTask> modelTasks = new HashSet<ModelTask>();
	
	final private Set<Label> taskLabels = new HashSet<Label>();
//	final private List<ModelSite> modelSites = new LinkedList<ModelSite>();
	
	public DataBase(){
		//TODO remove, load users from xml.
		users.add(new User("Fracer"));
		users.add(new User("Rolf"));
		users.add(new User("Kellendil"));
		users.add(new User("ThielHater"));
		users.add(new User("EvilTwin"));
		users.add(new User("BlackBat"));
		users.add(new User("Gnox"));
		users.add(new User("AmProsius"));
		users.add(new User("anselm"));
		users.add(new User("NaikJoy"));
	}
	
	@PostConstruct
	private void init() {
		User defaultUser = new User("admin");
		defaultUser.setPasswordHash(userService.encode("admin"));
		users.add(defaultUser);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public synchronized <T extends Linkable> Collection<T> getList(Class<?> clazz) {
		if(clazz.equals(Task.class)) {
			Collection<T> allTasks = new HashSet<>();
			allTasks.addAll((Collection<T>) generalTasks);
			allTasks.addAll((Collection<T>) textureTasks);
			allTasks.addAll((Collection<T>) modelTasks);
			return allTasks;
		}
		return this.<T>getDataList(clazz).clone();
	}

	@Override
	public synchronized <T extends Linkable> boolean add(T data) {
		return getDataList(data.getClass()).add(data);
	}
	
	@Override
	public synchronized <T extends Linkable> boolean addAll(Class<?> clazz, Collection<? extends T> data) {
		Collection<T> collection = getDataList(clazz);
		return collection.addAll(data);
	}

	@Override
	public synchronized <T extends Linkable> boolean remove(T data) {
		return getDataList(data.getClass()).remove(data);
	}
	
	@Override
	public synchronized void removeAll(Class<?> clazz) {
		if(clazz.equals(Task.class)) {
			generalTasks.clear();
			textureTasks.clear();
			modelTasks.clear();
		}
		else {
			getDataList(clazz).clear();
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Linkable> Collection<T> getDataList(Class<?> clazz) {
		try {
			switch(clazz.getSimpleName()) {
				case "User":
					return (Collection<T>) users;
				case "GeneralTask":
					return (Collection<T>) generalTasks;
				case "TextureTask":
					return (Collection<T>) textureTasks;
				case "ModelTask":
					return (Collection<T>) modelTasks;
				case "Label":
					return (Collection<T>) taskLabels;
				default:
					throw new UnsupportedOperationException();
			}
		} catch (UnsupportedOperationException e) {
			System.err.println("\nDatabase Error: Request for class type: "+clazz.getSimpleName()+" not implemented!\n");
			throw new UnsupportedOperationException();
		}
	}
}
