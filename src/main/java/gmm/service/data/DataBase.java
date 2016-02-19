package gmm.service.data;

import java.util.Collections;
import java.util.WeakHashMap;

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
import gmm.domain.task.GeneralTask;
import gmm.domain.task.Task;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.TextureTask;
import gmm.service.UserService;
import gmm.util.Util;

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
	
	final private List<User> users = new LinkedList<>(User.class);
	final private Set<GeneralTask> generalTasks = new HashSet<>(GeneralTask.class);
	final private Set<TextureTask> textureTasks = new HashSet<>(TextureTask.class);
	final private Set<ModelTask> modelTasks = new HashSet<>(ModelTask.class);
	final private Set<Label> taskLabels = new HashSet<>(Label.class);
	final private CombinedData combined;
	
	final private java.util.Set<TaskUpdateCallback> weakCallbacks =
			Collections.newSetFromMap(new WeakHashMap<TaskUpdateCallback, Boolean>());
	
	/**
	 * DEAR GOD THIS IS UGLY.
	 */
	final private TaskUpdateCallback callbacks = new TaskUpdateCallback() {
		@Override public <T extends Task> void onRemoveAll(Collection<T> tasks) {
			for(final TaskUpdateCallback c : weakCallbacks) {c.onRemoveAll(tasks);}
		}
		@Override public <T extends Task> void onRemove(T task) {
			for(final TaskUpdateCallback c : weakCallbacks) {c.onRemove(task);}
		}
		@Override public <T extends Task> void onAddAll(Collection<T> tasks) {
			for(final TaskUpdateCallback c : weakCallbacks) {c.onAddAll(tasks);}
		}
		@Override public <T extends Task> void onAdd(T task) {
			for(final TaskUpdateCallback c : weakCallbacks) {c.onAdd(task);}
		}
	};
	
	@Autowired
	private DataBase(CombinedData combined) {
		this.combined = combined;
	}
	
	@PostConstruct
	private void init() {
		if (createDefaultUser) {
			final User defaultUser = new User(defaultUserName);
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
	
	@Override
	public synchronized <T extends Linkable> Collection<T> getList(Class<T> clazz) {
		final Collection<T> data;
		// if abstract
		if (clazz.isAssignableFrom(AssetTask.class)) {
			data = new HashSet<>(clazz);
			data.addAll(Util.castBound(textureTasks, clazz));
			data.addAll(Util.castBound(modelTasks, clazz));
			if (clazz.isAssignableFrom(Task.class)) {
				data.addAll(Util.castBound(generalTasks, clazz));
				if (!clazz.equals(Task.class)) {
					final String errorMessage =
							"A list for type '"+clazz.getSimpleName()+"' cannot be composed!";
					logger.error(errorMessage);
					throw new IllegalArgumentException(errorMessage);
				}
			}
		} else {
			data = getDataList(clazz).copy();
		}
		return data;
	}

	@Override
	public synchronized <T extends Linkable> boolean add(T data) {
		final boolean result = getDataList(Util.classOf(data)).add(data);
		if(data instanceof Task) {
			final Task task = (Task) data;
			taskLabels.add(new Label(task.getLabel()));
			callbacks.onAdd(task);
		}
		return result;
	}
	
	@Override
	public synchronized <T extends Linkable> boolean addAll(Collection<T> data) {
		final Collection<T> collection = getDataList(data.getGenericType());
		final boolean result =  collection.addAll(data);
		if(Task.class.isAssignableFrom(data.getGenericType())) {
			final Collection<? extends Task> tasks = Util.castBound(data, Task.class);
			for (final Task task : tasks) {
				taskLabels.add(new Label(task.getLabel()));
			}
			callbacks.onAddAll(tasks);
		}
		return result;
	}
	
	@Override
	public synchronized <T extends Linkable> void removeAll(Collection<T> data) {
		final Multimap<Class<? extends Linkable>, T> clazzToData = HashMultimap.create();
		final Collection<Task> tasks  = new HashSet<>(Task.class);
		for(final T item : data) {
			final Class<? extends Linkable> clazz = item.getClass();
			if (Task.class.isAssignableFrom(clazz)) {
				tasks.add((Task) item);
			}
			clazzToData.put(clazz, item);
		}
		for(final Class<? extends Linkable> clazz : clazzToData.keySet()) {
			getDataList(clazz).removeAll(clazzToData.get(clazz));
		}
		callbacks.onRemoveAll(tasks);
	}

	@Override
	public synchronized <T extends Linkable> boolean remove(T data) {
		final boolean removed = getDataList(data.getClass()).remove(data);
		if (removed && data instanceof Task) {
			final Task task = (Task) data;
			callbacks.onRemove(task);
		}
		return removed;
	}
	
	@Override
	public synchronized <T extends Linkable> void removeAll(Class<T> clazz) {
		if(Task.class.isAssignableFrom(clazz)) {
			final Collection<? extends Task> tasks = Util.castBound(getList(clazz), Task.class);
			callbacks.onRemoveAll(tasks);
		}
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
		//TODO call callbacks!!!
	}
	
	private <T extends Linkable> Collection<T> getDataList(Class<T> clazz) {
		final Collection<?> data;
		if (clazz.equals(User.class))
			data = users;
		else if (clazz.equals(GeneralTask.class))
			data = generalTasks;
		else if (clazz.equals(TextureTask.class))
			data = textureTasks;
		else if (clazz.equals(ModelTask.class))
			data = modelTasks;
		else if (clazz.equals(Label.class))
			data = taskLabels;
		else {
			final String errorMessage =
					"A list of type '"+clazz.getSimpleName()+"' does not exist!";
			logger.error(errorMessage);
			throw new IllegalArgumentException(errorMessage);
		}
		return Util.cast(data, clazz);
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
		for(final UniqueObject u : c) {
			for (final long id : ids) {
				if(u.getId() == id) return true;
			}
		}
		return false;
	}

	@Override
	public void registerForUpdates(TaskUpdateCallback onUpdate) {
		weakCallbacks.add(onUpdate);
		//TODO delete when tested with more than 20 sessions
		if (weakCallbacks.size() > 20)
			logger.error("Memory leak: Callback objects not getting garbage collected!");
	}
}
