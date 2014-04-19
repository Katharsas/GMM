package gmm.service.data;

import org.springframework.stereotype.Service;

import gmm.domain.GeneralTask;
import gmm.domain.Label;
import gmm.domain.Linkable;
import gmm.domain.ModelTask;
import gmm.domain.Task;
import gmm.domain.TextureTask;
import gmm.domain.User;
import gmm.util.LinkedList;
import gmm.util.List;
import gmm.util.HashSet;
import gmm.util.Set;
import gmm.util.Collection;

@Service
public class DataBase implements DataAccess {

	final private List<User> users = new LinkedList<User>();
	final private Set<GeneralTask> generalTasks = new HashSet<GeneralTask>();
	final private Set<TextureTask> textureTasks = new HashSet<TextureTask>();
	final private Set<ModelTask> modelTasks = new HashSet<ModelTask>();
	
	final private Set<Label> taskLabels = new HashSet<Label>();
//	final private List<ModelSite> modelSites = new LinkedList<ModelSite>();
	
	public DataBase(){
		//add somehow users, usually from xml
		users.add(new User("Fracer","123456"));
		users.add(new User("Rolf", "123456"));
		users.add(new User("Kellendil", "123456"));
		users.add(new User("ThielHater", "123456"));
		users.add(new User("EvilTwin", "123456"));
		users.add(new User("BlackBat", "123456"));
		users.add(new User("Gnox", "123456"));
		users.add(new User("AmProsius", "123456"));
		users.add(new User("anselm", "123456"));
		users.add(new User("NaikJoy", "123456"));
		
		//add users to userDetailsManager bean to provide login/session functionality
		//not working, users need to be configured in xml too
//		ApplicationContext context = ContextLoader.getCurrentWebApplicationContext();
//		InMemoryUserDetailsManager manager = (InMemoryUserDetailsManager) context.getBean(UserDetailsManager.class);
//		List<SimpleGrantedAuthority> authorities = new LinkedList<SimpleGrantedAuthority>();
//		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
//		for(User u : users){
//			manager.createUser(new org.springframework.security.core.userdetails.User(
//					u.getName(), u.getPasswordHash(), authorities));
//		}
//		
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
		getDataList(clazz).clear();
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
