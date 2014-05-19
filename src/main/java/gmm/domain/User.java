package gmm.domain;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class User extends NamedObject {
	
	//Variables-------------------------------------------
	//Domain - Set by constructor
	private String passwordHash;
	//Domain - Default
	@XStreamAsAttribute
	private String role = ROLE_USER;
	private boolean enabled = false;
	@XStreamAsAttribute
	private String email="";
	
	final private List<Notification> oldNotifications = new LinkedList<Notification>();
	final private List<Notification> newNotifications = new LinkedList<Notification>();
	
	//Options
//	public boolean sentNotificationsToMail = false;
//	public int daysToSaveNotifiations = 30;
//	public int maximumSavedNotifications = 100;
	
	//Constants
	public final static User NULL = new User("EMPTY");
	public final static String ROLE_ADMIN = "ROLE_ADMIN";
	public final static String ROLE_USER = "ROLE_USER";
	public final static String ROLE_GUEST = "ROLE_GUEST";
	
	//Methods--------------------------------------------
	/**
	 * @param idName - Identificator (name) of the user.
	 * @param passwordHash - The users password hash.
	 */
	public User(String name) {
		super(name);
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
}
