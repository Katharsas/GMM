package gmm.domain;

import java.util.Collection;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.domain.task.Task;
import gmm.web.forms.LoadForm;

public class User extends NamedObject {
	
	//Constants
	public final static User NULL = new User("EMPTY");
	public final static String ROLE_ADMIN = "ROLE_ADMIN";
	public final static String ROLE_USER = "ROLE_USER";
	public final static String ROLE_GUEST = "ROLE_GUEST";
	
	//Variables-------------------------------------------
	//Domain - Set by constructor
	private String passwordHash;
	//Domain - Default
	@XStreamAsAttribute
	private String role = ROLE_USER;
	private boolean enabled = false;
	@XStreamAsAttribute
	private String email="";
	
	private final List<Task> pinnedTasks = new LinkedList<>(Task.class);
	
	private final List<Notification> oldNotifications = new LinkedList<>(Notification.class);
	private final List<Notification> newNotifications = new LinkedList<>(Notification.class);
	
	//Options & Settings
	private LoadForm loadForm = new LoadForm();
	
	//Methods--------------------------------------------
	/**
	 * @param idName - Identificator (name) of the user.
	 * @param passwordHash - The users password hash.
	 */
	public User(String name) {
		super(name);
	}
	
	/**
	 * Returns the first user from a collection who has the same name as this user.
	 * Ignores case of letters in name.
	 */
	public static User getFromName(Collection<? extends User> c, String name) {
		Objects.requireNonNull(c);
		for(final User user : c) {
			if(user.getName().equalsIgnoreCase(name)) return user;
		}
		return null;
	}
	
	@Override
	public String toString() {
		return getIdLink();
	}
	
	public String toStringDebug() {
		return "[Name: " + getName() + " Id:" + getId() + "]";
	}
	
	//Setters, Getters---------------------------------------
	public void setPasswordHash(String passwordHash) {
		Objects.requireNonNull(passwordHash);
		this.passwordHash = passwordHash;
	}
	public String getPasswordHash() {
		return passwordHash;
	}

	public void setRole(String role) {
		if(role.equals(ROLE_GUEST) || role.equals(ROLE_USER) || role.equals(ROLE_ADMIN)) {
			this.role = role;
		}
		else {
			throw new IllegalArgumentException("Role parameter is not valid.");
		}
	}
	
	public String getRole() {
		return role;
	}

	public void setEmail(String email) {
		Objects.requireNonNull(email);
		this.email = email;
	}
	
	public String getEmail() {
		return email;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void enable(boolean enabled) {
		this.enabled = enabled;
	}
	
	public List<Notification> getOldNotifications() {
		return oldNotifications;
	}
	public List<Notification> getNewNotifications() {
		return newNotifications;
	}
	public LoadForm getLoadForm() {
		return loadForm;
	}
	public void setLoadForm(LoadForm loadForm) {
		Objects.requireNonNull(loadForm);
		this.loadForm = loadForm;
	}
	
	public List<Task> getPinnedTasks() {
		return pinnedTasks;
	}
}
