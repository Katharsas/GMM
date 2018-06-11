package gmm.service.users;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import gmm.util.StringUtil;

@Service
public class UserService extends UserProvider implements DataChangeCallback<Task> {

	private final DataAccess data;
	private final SecureRandom random;
	private final PasswordEncoder encoder;
	
	@Autowired
	public UserService(DataAccess data, PasswordEncoder encoder) {
		super(() -> data.<User>getList(User.class));
		this.data = data;
		data.registerForUpdates(this, Task.class);
		this.encoder = encoder;
		random = new SecureRandom();
	}
	
	/**
	 * @return true if a user with this name does not yet exist.
	 */
	public boolean isFreeUserName(String name) {
		final StringUtil util = StringUtil.ignoreCase();
		if (util.equals(name, User.NULL.getName())
				|| util.equals(name, User.SYSTEM.getName())
				|| util.equals(name, User.UNKNOWN.getName())) {
			return false;
		}
		for (final User user : get()) {
			if (util.equals(name, user.getName())) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Clean up pinned tasks on task deletion.
	 */
	@Override
	public void onEvent(DataChangeEvent<Task> event) {
		if (event.type == DataChangeType.REMOVED) {
			final Multimap<Long, User> pinnedTasks = MultimapBuilder.hashKeys().arrayListValues().build();
			for (final User user : get()) {
				for (final long taskId : user.getPinnedTaskIds()) {
					pinnedTasks.put(taskId, user);
				}
			}
			for (final Task task : event.changed) {
				for (final User user : pinnedTasks.get(task.getId())) {
					user.getPinnedTaskIds().remove(task.getId());
				}
			}
		}
	}
	
	/**
	 * @return Clear text (not encoded) password.
	 */
	public String generatePassword() {
		//toString(32) encodes 5 bits/char, so BigInteger range bits should be a multiple of 5
		return new BigInteger(50, random).toString(32);
	}
	
	/**
	 * @return Encoded password (hash).
	 */
	public String encodePassword(String clearTextPassword) {
		return encoder.encode(clearTextPassword);
	}
	
	/**
	 * @return True, if the given password matches the given User's password.
	 */
	public boolean matchesPassword(String clearTextPassword, User user) {
		return encoder.matches(clearTextPassword, user.getPasswordHash());
	}
	
	public void add(User user) {
		data.add(user);
	}
	
	public void addAll(Collection<User> users) {
		data.addAll(users);
	}
}
