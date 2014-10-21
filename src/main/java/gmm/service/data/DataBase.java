package gmm.service.data;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import gmm.collections.Collection;
import gmm.collections.HashSet;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.collections.Set;
import gmm.domain.AssetTask;
import gmm.domain.GeneralTask;
import gmm.domain.Label;
import gmm.domain.Linkable;
import gmm.domain.ModelTask;
import gmm.domain.Task;
import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.service.UserService;

@Service
public class DataBase implements DataAccess {

	@Autowired UserService userService;
	@Autowired PasswordEncoder encoder;
	
	final private List<User> users = new LinkedList<>();
	final private Set<GeneralTask> generalTasks = new HashSet<>();
	final private Set<TextureTask> textureTasks = new HashSet<>();
	final private Set<ModelTask> modelTasks = new HashSet<>();
	final private Set<Label> taskLabels = new HashSet<>();
	
	@PostConstruct
	private void init() {
		User defaultUser = new User("admin");
		defaultUser.setPasswordHash(encoder.encode("admin"));
		defaultUser.setRole(User.ROLE_ADMIN);
		defaultUser.enable(true);
		users.add(defaultUser);
		
		System.out.println("##########################################################\n");
		System.out.println("  Created default user: ");
		System.out.println("  Username: " + defaultUser.getName());
		System.out.println("  Password: " + "admin");
		System.out.println("\n##########################################################");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T extends Linkable> Collection<T> getList(Class<T> clazz) {
		if(clazz.equals(Task.class)) {
			Collection<T> allTasks = new HashSet<>();
			allTasks.addAll((Collection<T>)generalTasks);
			allTasks.addAll((Collection<T>) textureTasks);
			allTasks.addAll((Collection<T>) modelTasks);
			return allTasks;
		}
		else if(clazz.equals(AssetTask.class)) {
			Collection<T> assetTasks = new HashSet<>();
			assetTasks.addAll((Collection<T>) textureTasks);
			assetTasks.addAll((Collection<T>) modelTasks);
			return assetTasks;
		}
		else {
			return (Collection<T>) this.<T>getDataList(clazz).copy();
		}
	}

	@Override
	public synchronized <T extends Linkable> boolean add(T data) {
		boolean result = getDataList(data.getClass()).add(data);
		if(data instanceof Task) {
			taskLabels.add(new Label(((Task)data).getLabel()));
		}
		return result;
	}
	
	@Override
	public synchronized <T extends Linkable> boolean addAll(Class<T> clazz, Collection<? extends T> data) {
		Collection<T> collection = getDataList(clazz);
		boolean result =  collection.addAll(data);
		
		if(Task.class.isAssignableFrom(clazz)) {
			for (T task : data) {
				taskLabels.add(new Label(((Task)task).getLabel()));
			}
		}
		return result;
	}

	@Override
	public synchronized <T extends Linkable> boolean remove(T data) {
		return getDataList(data.getClass()).remove(data);
	}
	
	@Override
	public synchronized <T extends Linkable> void removeAll(Class<T> clazz) {
		if(clazz.equals(Task.class)) {
			generalTasks.clear();
			textureTasks.clear();
			modelTasks.clear();
		}
		else if (clazz.equals(AssetTask.class)) {
			textureTasks.clear();
			modelTasks.clear();
		}
		else {
			getDataList(clazz).clear();
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Linkable> Collection<T> getDataList(Class<? extends T> clazz) {
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
