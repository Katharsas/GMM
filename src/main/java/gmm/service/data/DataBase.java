package gmm.service.data;

//import org.springframework.context.ApplicationContext;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.provisioning.InMemoryUserDetailsManager;
//import org.springframework.security.provisioning.JdbcUserDetailsManager;
//import org.springframework.security.provisioning.UserDetailsManager;
//import org.springframework.web.context.ContextLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.*;
import gmm.util.LinkedList;
import gmm.util.List;
import gmm.util.HashSet;
import gmm.util.Set;
import gmm.util.Collection;

@Service
public class DataBase implements DataAccess {

	@Autowired
	XMLSerializerService xmlService;

	final private List<User> users = new LinkedList<User>();
	final private List<GeneralTask> generalTasks = new LinkedList<GeneralTask>();
//	final private List<FileTask> generalFileTasks = new LinkedList<FileTask>();
	final private List<TextureTask> textureTasks = new LinkedList<TextureTask>();
	final private List<ModelTask> modelTasks = new LinkedList<ModelTask>();
	
	final private Set<Label> taskLabels = new HashSet<Label>();
//	final private List<ModelSite> modelSites = new LinkedList<ModelSite>();
	
	public DataBase(){
		//add somehow users, usually from xml
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
		
		testFill();
	}
	
	private void testFill(){
		
		//DO NOT CHANGE ANYMORE!
//		modelSites.add(new ModelSite("OldCamp"));
//		modelSites.add(new ModelSite("NewCamp"));
//		modelSites.add(new ModelSite("Surface"));
//		modelSites.add(new ModelSite("OldMine"));
		//DO NOT CHANGE ANYMORE!
	}
	
	@Override
	public synchronized <T> Collection<T> getList(Class<T> clazz) {
		return this.<T>getDataList(clazz).clone();
	}

	@Override
	public synchronized <T> boolean addData(T data) {
		return getDataList(data.getClass()).add(data);
	}
	
	@Override
	public synchronized <T> boolean addAllData(Class<T> clazz, Collection<? extends T> data) {
		Collection<T> collection = getDataList(clazz);
		boolean buffer = true;
		for(T t : data) {
			buffer = collection.add(t) && buffer;
		}
		return buffer;
	}

	@Override
	public synchronized <T> boolean removeData(T data) {
		return getDataList(data.getClass()).remove(data);
	}
	
	@Override
	public synchronized void removeAllData(Class<?> clazz) {
		getDataList(clazz).clear();
	}
	
	@Override
	public synchronized void saveData(Class<?> clazz) {
		xmlService.serialize(getDataList(clazz), clazz.getSimpleName()+"List");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> void loadData(Class<T> clazz) {
		removeAllData(clazz);
		List<? extends T> data = (List<? extends T>) xmlService.deserialize(clazz.getSimpleName()+"List");
		addAllData(clazz, data);
	}
	
	@SuppressWarnings("unchecked")
	private <T> Collection<T> getDataList(Class<?> clazz) {
		try {
			switch(clazz.getSimpleName()) {
				case "User":
					return (List<T>) users;
				case "GeneralTask":
					return (List<T>) generalTasks;
				case "TextureTask":
					return (List<T>) textureTasks;
				case "ModelTask":
					return (List<T>) modelTasks;
				case "Label":
					return (Set<T>) taskLabels;
				default:
					throw new UnsupportedOperationException();
			}
		} catch (UnsupportedOperationException e) {
			System.err.println("\nDatabase Error: Request for class type: "+clazz.getSimpleName()+" not implemented!\n");
			throw new UnsupportedOperationException();
		}
	}
}
