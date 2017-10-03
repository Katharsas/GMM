package gmm.service.users;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import gmm.collections.Collection;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.data.DataAccess;
import gmm.service.data.DataAccess.DataChangeCallback;
import gmm.service.data.DataChangeEvent;
import gmm.service.data.DataChangeType;

@Service
public class UserService extends UserProvider implements DataChangeCallback {

	private final DataAccess data;
	private final SecureRandom random;
	
	@Autowired
	public UserService(DataAccess data) {
		super(() -> data.<User>getList(User.class));
		this.data = data;
		data.registerForUpdates(this);
		random = new SecureRandom();
	}
	
	/**
	 * Clean up pinned tasks on task deletion.
	 */
	@Override
	public void onEvent(DataChangeEvent event) {
		if (event.type == DataChangeType.REMOVED) {
			if (Task.class.isAssignableFrom(event.changed.getGenericType())) {
				final Multimap<Long, User> pinnedTasks = MultimapBuilder.hashKeys().arrayListValues().build();
				for (final User user : get()) {
					for (final long taskId : user.getPinnedTaskIds()) {
						pinnedTasks.put(taskId, user);
					}
				}
				for (final Task task : event.getChanged(Task.class)) {
					for (final User user : pinnedTasks.get(task.getId())) {
						user.getPinnedTaskIds().remove(task.getId());
					}
				}
			}
		}
	}
	
	public String generatePassword() {
		//toString(32) encodes 5 bits/char, so BigInteger range bits should be a multiple of 5
		return new BigInteger(50, random).toString(32);
	}
	
	public void add(User user) {
		data.add(user);
	}
	
	public void addAll(Collection<User> users) {
		data.addAll(users);
	}
}
