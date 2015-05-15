package gmm.service.data;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import gmm.collections.Collection;
import gmm.collections.HashSet;
import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.collections.Set;
import gmm.domain.Label;
import gmm.domain.Linkable;
import gmm.domain.UniqueObject;
import gmm.domain.User;
import gmm.domain.task.AssetTask;
import gmm.domain.task.GeneralTask;
import gmm.domain.task.ModelTask;
import gmm.domain.task.Task;
import gmm.domain.task.TextureTask;
import gmm.service.UserService;

@Service
public class DataBase implements DataAccess {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Value("${default.user}")
	private boolean createDefaultUser;
	
	@Value("${default.username}")
	private String defaultUserName;
	
	@Value("${default.password}")
	private String defaultUserPW;
	
	@Autowired UserService userService;
	@Autowired PasswordEncoder encoder;
	
	final private List<User> users = new LinkedList<>();
	final private Set<GeneralTask> generalTasks = new HashSet<>();
	final private Set<TextureTask> textureTasks = new HashSet<>();
	final private Set<ModelTask> modelTasks = new HashSet<>();
	final private Set<Label> taskLabels = new HashSet<>();
	final private CombinedData combined;
	
	@Autowired
	private DataBase(CombinedData combined) {
		this.combined = combined;
	}
	
	@PostConstruct
	private void init() {
		if (createDefaultUser) {
			User defaultUser = new User(defaultUserName);
			defaultUser.setPasswordHash(encoder.encode(defaultUserPW));
			defaultUser.setRole(User.ROLE_ADMIN);
			defaultUser.enable(true);
			users.add(defaultUser);
			
			logger.info("\n"
					+	"##########################################################" + "\n\n"
					+	"  Created default user: " + "\n"
					+	"  Username: " + defaultUser.getName() + "\n"
					+	"  Password: " + defaultUserPW + "\n\n"
					+	"##########################################################");
		}
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
	public synchronized <T extends Linkable> void removeAll(Collection<T> data) {
		Multimap<Class<? extends Linkable>, T> clazzToData = HashMultimap.create();
		for(T item : data) {
			clazzToData.put(item.getClass(), item);
		}
		for(Class<? extends Linkable> clazz : clazzToData.keySet()) {
			getDataList(clazz).removeAll(clazzToData.get(clazz));
		}
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

	@Override
	public CombinedData getCombinedData() {
		return combined;
	}
	
	@Override
	public boolean hasIds(long[] ids) {
		return exists(users, ids) == true ||
				exists(generalTasks, ids) == true ||
				exists(textureTasks, ids) == true ||
				exists(modelTasks, ids) == true;

	}
	
	private boolean exists(Collection<? extends UniqueObject> c, long[] ids) {
		for(UniqueObject u : c) {
			for (long id : ids) {
				if(u.getId() == id) return true;
			}
		}
		return false;
	}
}
