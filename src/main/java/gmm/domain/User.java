package gmm.domain;

import java.util.Collection;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import gmm.collections.LinkedList;
import gmm.collections.List;
import gmm.web.forms.LoadForm;

/**
 * Since instead of a proper DB, serialization is used to create backups of user files and task
 * files separately, users cannot have persistent Task references directly. Save ids instead and
 * search for tasks in DataBase when actual Task data is needed.
 * 
 * @author Jan Mothes
 */
public class User extends NamedObject {
	
	public static class UserNameOccupiedException extends IllegalArgumentException {
		private static final long serialVersionUID = -8126984959992853457L;
		private final String userName;
		public UserNameOccupiedException(String userName, String message) {
			super(message);
			Objects.requireNonNull(userName);
			this.userName = userName;
		}
		public String getUserName() {
			return userName;
		}
	}
	
	public static class UserId implements Linkable {
		private final String idLink;
		private final String name;
		public UserId(String idLink, String name) {
			Objects.requireNonNull(idLink);
			Objects.requireNonNull(name);
			this.idLink = idLink;
			this.name = name;
		}
		@Override
		public String getIdLink() {
			return idLink;
		}
		public String getName() {
			return name;
		}
	}
	
	//Constants-------------------------------------------
	private static class PredefinedUser extends User {
		public PredefinedUser(String name) {super(name);}
		@Override
		protected void validateUserName(String name) {}
	}
	
	/** Represents the absence of a user. */
	public final static User NULL = new PredefinedUser("EMPTY");
	/** Represents a normal user thats is simply unknown at the moment. */
	public final static User UNKNOWN = new PredefinedUser("UNKNOWN");
	/** Represents the system/application itself as user. */
	public final static User SYSTEM = new PredefinedUser("SYSTEM");
	
	public final static String ROLE_ADMIN = "ROLE_ADMIN";
	public final static String ROLE_USER = "ROLE_USER";
	public final static String ROLE_GUEST = "ROLE_GUEST";
	
	{
		if (SYSTEM != null) {
			SYSTEM.setRole(ROLE_ADMIN);
		}
	}
	
	//Variables-------------------------------------------
	//Domain - Set by constructor
	private String passwordHash;
	//Domain - Default
	@XStreamAsAttribute
	private String role = ROLE_USER;
	private boolean enabled = false;
	@XStreamAsAttribute
	private String email="";
	
	private final List<Long> pinnedTaskIds = new LinkedList<>(Long.class);
	
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
	 * @return null if a user with the given name does not exist in the given collection.
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
	
	@Override
	public void setName(String name) {
		super.setName(name);
		validateUserName(name);
	}
	
	protected void validateUserName(String name) {
		if(name.equalsIgnoreCase(NULL.getName())
				|| name.equalsIgnoreCase(UNKNOWN.getName())
				|| name.equalsIgnoreCase(SYSTEM.getName())) {
			throw new UserNameOccupiedException(name, "User name '" + name + "' is not available!");
		}
	}
	
	/**
	 * @return True if this is not NULL, SYSTEM or UNKNOWN user.
	 */
	public boolean isNormalUser() {
		return this != NULL && this != SYSTEM && this != UNKNOWN;
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
	
	public List<Long> getPinnedTaskIds() {
		return pinnedTaskIds;
	}
	
	public UserId getUserId() {
		return new UserId(getIdLink(), getName());
	}
}
