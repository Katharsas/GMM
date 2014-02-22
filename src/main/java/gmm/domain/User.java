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
	private boolean isAdmin=false;
	@XStreamAsAttribute
	private String email="";
	final private List<Notification> oldNotifications = new LinkedList<Notification>();;
	final private List<Notification> newNotifications = new LinkedList<Notification>();
	
	//Options
	public boolean sentNotificationsToMail = false;
	public int daysToSaveNotifiations = 30;
	public int maximumSavedNotifications = 100;
	
	//Methods--------------------------------------------
	/**
	 * @param idName - Identificator (name) of the user.
	 * @param passwordHash - The users password hash.
	 */
	public User(String name, String passwordHash) {
		super(name);
		setPasswordHash(passwordHash);
	}
	
	//Setters, Getters---------------------------------------
	public void setPasswordHash(String passwordHash) {
		Objects.requireNonNull(passwordHash);
		this.passwordHash = passwordHash;
	}
	public String getPasswordHash() {
		return passwordHash;
	}

	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	public boolean isAdmin() {
		return isAdmin;
	}

	public void setEmail(String email) {
		Objects.requireNonNull(email);
		this.email = email;
	}
	
	public String getEmail() {
		return email;
	}
	
	public List<Notification> getOldNotifications() {
		return oldNotifications;
	}
	public List<Notification> getNewNotifications() {
		return newNotifications;
	}
}
