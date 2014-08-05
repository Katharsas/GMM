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
	
	final private List<User> users = new LinkedList<User>();
	final private Set<GeneralTask> generalTasks = new HashSet<GeneralTask>();
	final private Set<TextureTask> textureTasks = new HashSet<TextureTask>();
	final private Set<ModelTask> modelTasks = new HashSet<ModelTask>();
	final private Set<Label> taskLabels = new HashSet<Label>();
	
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
		return this.<T>getDataList(clazz).copy();
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
	public synchronized <T extends Linkable> boolean addAll(Class<?> clazz, Collection<? extends T> data) {
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
